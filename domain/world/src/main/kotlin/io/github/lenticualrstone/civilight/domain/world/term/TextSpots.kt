package io.github.lenticualrstone.civilight.domain.world.term

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.Rank
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.ordered

/**
 * A place where text runs live. [ScriptSpot]s are game-output manuscript text (scanned for
 * candidates, links there are usage and may be `▲`); every other spot is `원고 밖`.
 */
sealed interface TextSpot

/** Game-output manuscript text (WLD-19 rows with `Candidates searched: yes`). */
sealed interface ScriptSpot : TextSpot {
    /** The block or scene key a `TermRejection` stores as `containerKey`. */
    val containerKey: Key

    /** A dialogue, narration or system block. */
    data class BlockText(val blockKey: Key) : ScriptSpot {
        override val containerKey: Key get() = blockKey
    }

    /** One option line of a choice block (tag `선택지`). */
    data class OptionText(val blockKey: Key, val optionKey: Key) : ScriptSpot {
        override val containerKey: Key get() = blockKey
    }

    /** A scene's direct transition text (`전환 문구`, WLD Q-05 default: game output). */
    data class TransitionText(val sceneKey: Key) : ScriptSpot {
        override val containerKey: Key get() = sceneKey
    }
}

/** Text outside the game output (group `원고 밖 · 메모`). */
sealed interface OutsideSpot : TextSpot {
    /** An author memo block in the manuscript. */
    data class MemoBlock(val blockKey: Key) : OutsideSpot

    /** A paragraph of a page. */
    data class PageText(val pageKey: Key, val paragraphKey: Key) : OutsideSpot

    /** A paragraph of a character's speech or secret body. */
    data class CharacterBody(val characterKey: Key, val secret: Boolean, val paragraphKey: Key) : OutsideSpot

    /** A voice line (game text). */
    data class VoiceLineText(val characterKey: Key, val voiceLineKey: Key, val paragraphKey: Key) : OutsideSpot

    /** An archive document (game text). */
    data class ArchiveText(val characterKey: Key, val archiveKey: Key, val paragraphKey: Key) : OutsideSpot

    /** A paragraph of a term, place, faction or event description. */
    data class Description(val kind: DescribedKind, val key: Key, val paragraphKey: Key) : OutsideSpot

    /** A paragraph of a long-text field value. */
    data class FieldText(val entityKind: EntityKind, val entityKey: Key, val fieldDefKey: Key, val paragraphKey: Key) : OutsideSpot
}

enum class DescribedKind { TERM, PLACE, FACTION, EVENT }

/** A spot with its runs and, for manuscript text, its rank. */
data class SpotText(val spot: TextSpot, val runs: List<TextRun>, val rank: Rank?)

/** One stretch of plain text inside a spot; [offset] is its start in the spot's text. */
data class Segment(val text: String, val offset: Int)

/** Walks the project's text spots. Only live containers are returned. */
object TextSpots {
    /**
     * The text of a spot in run coordinates: the concatenation of every run's shown text (a
     * `괄호 지문` run without its parentheses, a break as `\n`). Candidate offsets use this.
     */
    fun textOf(runs: List<TextRun>): String = runs.joinToString("") { it.shownText }

    /**
     * Plain stretches of [runs]: consecutive plain runs form one segment; link, `괄호 지문`, break and
     * unknown runs split segments, so the position next to them is a word boundary (Q-04 f).
     */
    fun segments(runs: List<TextRun>): List<Segment> {
        val out = ArrayList<Segment>()
        val current = StringBuilder()
        var start = 0
        var cursor = 0
        for (run in runs) {
            val shown = run.shownText
            if (run is TextRun.Plain) {
                if (current.isEmpty()) start = cursor
                current.append(shown)
            } else if (current.isNotEmpty()) {
                out += Segment(current.toString(), start)
                current.clear()
            }
            cursor += shown.length
        }
        if (current.isNotEmpty()) out += Segment(current.toString(), start)
        return out
    }

    /** Game-output manuscript spots in manuscript order (a scene's transition text before its blocks). */
    fun script(doc: ProjectDoc): List<SpotText> {
        val out = ArrayList<SpotText>()
        val manuscript = doc.manuscript
        for (scene in manuscript.liveScenes) {
            transition(doc, scene)?.let { out += it }
            for (block in manuscript.liveBlocksOf(scene.key)) out += scriptSpotsOf(doc, block)
        }
        return out
    }

    private fun transition(doc: ProjectDoc, scene: StoryUnit): SpotText? {
        val runs = scene.transitionRuns ?: return null
        if (scene.transitionMode?.known != TransitionMode.DIRECT) return null
        return SpotText(ScriptSpot.TransitionText(scene.key), runs, doc.manuscript.transitionRankOf(scene.key))
    }

    /** The game-output spots of one block (none for memo, direction or unknown kinds). */
    fun scriptSpotsOf(doc: ProjectDoc, block: Block): List<SpotText> {
        val rank = doc.manuscript.blockRankOf(block.key)
        return when (block.kind.known) {
            BlockKind.DIALOGUE, BlockKind.NARRATION, BlockKind.SYSTEM -> listOf(SpotText(ScriptSpot.BlockText(block.key), block.runs, rank))
            BlockKind.CHOICE -> block.options.sortedWith(compareBy({ it.order }, { it.key }))
                .map { SpotText(ScriptSpot.OptionText(block.key, it.key), it.runs, rank) }
            else -> emptyList()
        }
    }

    /** The runs of a script spot in [doc], or null when the container no longer exists. */
    fun runsOf(doc: ProjectDoc, spot: ScriptSpot): List<TextRun>? = when (spot) {
        is ScriptSpot.BlockText -> doc.index.block(spot.blockKey)?.runs
        is ScriptSpot.OptionText -> doc.index.block(spot.blockKey)?.options?.firstOrNull { it.key == spot.optionKey }?.runs
        is ScriptSpot.TransitionText -> doc.index.unit(spot.sceneKey)?.transitionRuns
    }

    /** True when the container of [spot] is live (block and scene live; transition shown). */
    fun isLive(doc: ProjectDoc, spot: ScriptSpot): Boolean = when (spot) {
        is ScriptSpot.BlockText -> doc.manuscript.isLiveBlock(spot.blockKey)
        is ScriptSpot.OptionText -> doc.manuscript.isLiveBlock(spot.blockKey) &&
            doc.index.block(spot.blockKey)?.options?.any { it.key == spot.optionKey } == true
        is ScriptSpot.TransitionText -> doc.manuscript.isLiveUnit(spot.sceneKey) &&
            doc.index.unit(spot.sceneKey)?.transitionMode?.known == TransitionMode.DIRECT
    }

    /** True when the container of [spot] still exists (live or in the trash). */
    fun exists(doc: ProjectDoc, spot: ScriptSpot): Boolean = runsOf(doc, spot) != null

    /** Rank of a script spot (its block's or its transition's). */
    fun rankOf(doc: ProjectDoc, spot: ScriptSpot): Rank? = when (spot) {
        is ScriptSpot.BlockText -> doc.manuscript.blockRankOf(spot.blockKey)
        is ScriptSpot.OptionText -> doc.manuscript.blockRankOf(spot.blockKey)
        is ScriptSpot.TransitionText -> doc.manuscript.transitionRankOf(spot.sceneKey)
    }

    /** Every live text outside the game output: memo blocks, pages, characters, world descriptions, long-text fields. */
    fun outside(doc: ProjectDoc): List<SpotText> {
        val out = ArrayList<SpotText>()
        val manuscript = doc.manuscript
        for (block in manuscript.liveBlocks) {
            if (block.kind.known == BlockKind.MEMO) out += SpotText(OutsideSpot.MemoBlock(block.key), block.runs, manuscript.blockRankOf(block.key))
        }
        fun paragraphs(doc: RichDoc, make: (Key) -> TextSpot) = doc.ordered().map { SpotText(make(it.key), it.runs, null) }
        for (page in doc.pages.values.filter { it.deletedAt == null }.sortedWith(compareBy({ it.order }, { it.key }))) {
            out += paragraphs(page.blocks) { OutsideSpot.PageText(page.key, it) }
        }
        val liveCharacters = doc.cast.characters.filter { it.deletedAt == null }.sortedWith(compareBy({ it.order }, { it.key }))
        for (c in liveCharacters) {
            out += paragraphs(c.speechBody) { OutsideSpot.CharacterBody(c.key, secret = false, paragraphKey = it) }
            out += paragraphs(c.secretBody) { OutsideSpot.CharacterBody(c.key, secret = true, paragraphKey = it) }
            for (line in doc.index.voiceLines(c.key)) out += paragraphs(line.text) { OutsideSpot.VoiceLineText(c.key, line.key, it) }
            for (archive in doc.index.archiveDocs(c.key)) out += paragraphs(archive.body) { OutsideSpot.ArchiveText(c.key, archive.key, it) }
            out += fieldTexts(doc, EntityKind.CHARACTER, c.key, c.fields)
        }
        for (t in doc.world.terms.filter { it.deletedAt == null }) {
            out += paragraphs(t.description) { OutsideSpot.Description(DescribedKind.TERM, t.key, it) }
            out += fieldTexts(doc, EntityKind.TERM, t.key, t.fields)
        }
        for (p in doc.world.places.filter { it.deletedAt == null }) {
            out += paragraphs(p.description) { OutsideSpot.Description(DescribedKind.PLACE, p.key, it) }
            out += fieldTexts(doc, EntityKind.PLACE, p.key, p.fields)
        }
        for (f in doc.world.factions.filter { it.deletedAt == null }) {
            out += paragraphs(f.description) { OutsideSpot.Description(DescribedKind.FACTION, f.key, it) }
            out += fieldTexts(doc, EntityKind.FACTION, f.key, f.fields)
        }
        for (e in doc.world.events.filter { it.deletedAt == null }) {
            out += paragraphs(e.description) { OutsideSpot.Description(DescribedKind.EVENT, e.key, it) }
        }
        return out
    }

    private fun fieldTexts(
        doc: ProjectDoc,
        kind: EntityKind,
        entityKey: Key,
        fields: Map<Key, io.github.lenticualrstone.civilight.model.cast.FieldValue>,
    ): List<SpotText> = fields.entries
        .filter { doc.liveness.isFieldDefLive(it.key) }
        .sortedBy { it.key }
        .flatMap { (defKey, value) ->
            val rich = value.payload as? FieldPayload.Rich ?: return@flatMap emptyList()
            rich.doc.ordered().map { SpotText(OutsideSpot.FieldText(kind, entityKey, defKey, it.key), it.runs, null) }
        }
}
