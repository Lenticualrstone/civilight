package io.github.lenticualrstone.civilight.domain.world.common

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.TextRun

/** Rewrites text runs across a project (link retargeting, unlinking) and updates single blocks or units. */
object DocRuns {
    private fun RichDoc.mapRuns(f: (List<TextRun>) -> List<TextRun>): RichDoc =
        map { p -> f(p.runs).let { if (it == p.runs) p else p.copy(runs = it) } }

    private fun Map<Key, FieldValue>.mapRuns(f: (List<TextRun>) -> List<TextRun>): Map<Key, FieldValue> =
        mapValues { (_, value) ->
            val rich = value.payload as? FieldPayload.Rich ?: return@mapValues value
            val next = rich.doc.mapRuns(f)
            if (next == rich.doc) value else value.copy(payload = FieldPayload.Rich(next))
        }

    /** Applies [f] to the runs of every text container of [doc], live and trashed. */
    fun mapAll(doc: ProjectDoc, f: (List<TextRun>) -> List<TextRun>): ProjectDoc = doc.copy(
        episodes = doc.episodes.mapValues { (_, file) ->
            file.copy(blocks = file.blocks.map { b -> mapBlock(b, f) })
        },
        story = doc.story.copy(
            units = doc.story.units.map { u -> u.transitionRuns?.let(f)?.takeIf { it != u.transitionRuns }?.let { u.copy(transitionRuns = it) } ?: u },
        ),
        pages = doc.pages.mapValues { (_, p) -> p.copy(blocks = p.blocks.mapRuns(f)) },
        cast = doc.cast.copy(
            characters = doc.cast.characters.map { c ->
                c.copy(speechBody = c.speechBody.mapRuns(f), secretBody = c.secretBody.mapRuns(f), fields = c.fields.mapRuns(f))
            },
            voiceLines = doc.cast.voiceLines.map { it.copy(text = it.text.mapRuns(f)) },
            archiveDocs = doc.cast.archiveDocs.map { it.copy(body = it.body.mapRuns(f)) },
        ),
        world = doc.world.copy(
            terms = doc.world.terms.map { it.copy(description = it.description.mapRuns(f), fields = it.fields.mapRuns(f)) },
            places = doc.world.places.map { it.copy(description = it.description.mapRuns(f), fields = it.fields.mapRuns(f)) },
            factions = doc.world.factions.map { it.copy(description = it.description.mapRuns(f), fields = it.fields.mapRuns(f)) },
            events = doc.world.events.map { it.copy(description = it.description.mapRuns(f)) },
        ),
    )

    private fun mapBlock(b: Block, f: (List<TextRun>) -> List<TextRun>): Block {
        val runs = f(b.runs)
        val options = b.options.map { o -> f(o.runs).let { if (it == o.runs) o else o.copy(runs = it) } }
        return if (runs == b.runs && options == b.options) b else b.copy(runs = runs, options = options)
    }

    /** Replaces one block (found by key in its episode file). Null when the block is unknown. */
    fun updateBlock(doc: ProjectDoc, blockKey: Key, f: (Block) -> Block): ProjectDoc? {
        val group = doc.index.episodeGroupOfBlock(blockKey) ?: return null
        val file = doc.episodes[group] ?: return null
        val blocks = file.blocks.map { if (it.key == blockKey) f(it) else it }
        return doc.copy(episodes = doc.episodes + (group to file.copy(blocks = blocks)))
    }

    /** Replaces one story unit. Null when the unit is unknown. */
    fun updateUnit(doc: ProjectDoc, unitKey: Key, f: (StoryUnit) -> StoryUnit): ProjectDoc? {
        if (doc.index.unit(unitKey) == null) return null
        return doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == unitKey) f(it) else it }))
    }
}
