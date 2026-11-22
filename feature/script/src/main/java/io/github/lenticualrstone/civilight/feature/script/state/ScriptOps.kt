package io.github.lenticualrstone.civilight.feature.script.state

import io.github.lenticualrstone.civilight.domain.script.BlockOps
import io.github.lenticualrstone.civilight.domain.script.BlockPosition
import io.github.lenticualrstone.civilight.domain.script.EditContext
import io.github.lenticualrstone.civilight.domain.script.Runs
import io.github.lenticualrstone.civilight.domain.script.WritingCounts
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.navigation.NewUnitSheet
import io.github.lenticualrstone.civilight.navigation.SceneSplitSheet
import io.github.lenticualrstone.civilight.navigation.SheetKey

/** Where a new block goes (SCR-12). */
sealed interface InsertAt {
    /** Directly below a block. */
    data class After(val blockKey: Key) : InsertAt

    /** At the end of a scene (no current block). */
    data class SceneEnd(val sceneKey: Key) : InsertAt

    /** First in a scene (the virtual line of an empty scene). */
    data class SceneStart(val sceneKey: Key) : InsertAt

    fun toPosition(): BlockPosition = when (this) {
        is After -> BlockPosition.After(blockKey)
        is SceneEnd -> BlockPosition.SceneEnd(sceneKey)
        is SceneStart -> BlockPosition.SceneStart(sceneKey)
    }
}

/** A caret position in raw offsets of a block's runs. */
data class Caret(val blockKey: Key, val offset: Int)

/** The block whose speaker the picker sets, and the speaker it shows. */
data class SpeakerTarget(
    /** A dialogue block, or null when the pick is held for the next dialogue insert. */
    val blockKey: Key?,
    val speakerKey: Key?,
    /** The scene whose cast the picker lists first. */
    val sceneKey: Key?,
)

/**
 * The script's edit operations over a document (SCR-12, SCR-15, SCR-18, SCR-19). Each returns the
 * next document and where the caret or selection goes; the view model runs each one inside one
 * store update, so one operation is one undo step (SCR-17).
 */
object ScriptOps {
    data class Outcome(
        val doc: ProjectDoc,
        /** Caret after the operation (null: no caret). */
        val caret: Caret? = null,
        /** Block selected without a caret. */
        val select: Key? = null,
        /** Toast message (`◆ 1개 #042로`, `블록 #043 지움 · …`). */
        val toast: String? = null,
        /** The new dialogue block took the speaker above it (`직전 화자`). */
        val inherited: Boolean = false,
        /** A block this operation created (it enters from 8 dp below, SCR-12 / SCR-67). */
        val created: Key? = null,
    )

    /** Inserts a block of [kind] at [at]; a text kind gets the caret, others are selected. */
    fun insert(doc: ProjectDoc, at: InsertAt, kind: BlockKind, speakerKey: Key?, ctx: EditContext, runs: List<TextRun> = emptyList()): Outcome {
        val inserted = BlockOps.insert(doc, at.toPosition(), kind, ctx, runs = runs, speakerKey = speakerKey.takeIf { kind == BlockKind.DIALOGUE })
        return if (kind in BlockOps.TEXT_KINDS) {
            Outcome(
                inserted.doc,
                caret = Caret(inserted.blockKey, Runs.length(Runs.normalize(runs))),
                inherited = inserted.inheritedSpeaker,
                created = inserted.blockKey,
            )
        } else {
            Outcome(inserted.doc, select = inserted.blockKey, created = inserted.blockKey)
        }
    }

    /** Creates the first line of an empty scene from its virtual line, holding [runs] (the first keystroke). */
    fun materialize(doc: ProjectDoc, sceneKey: Key, speakerKey: Key?, runs: List<TextRun>, ctx: EditContext): Outcome =
        insert(doc, InsertAt.SceneStart(sceneKey), BlockKind.DIALOGUE, speakerKey, ctx, runs)

    /** Enter at raw [offset]: a new block of the same kind below, or a split (SCR-15). */
    fun enter(doc: ProjectDoc, blockKey: Key, offset: Int, ctx: EditContext): Outcome? {
        val before = WritingCounts.recordBaseline(doc, blockKey, ctx.today)
        return when (val r = BlockOps.enter(before, blockKey, offset, ctx)) {
            is BlockOps.EnterResult.NewBlock -> {
                val made = r.doc.index.block(r.blockKey)
                val inherited = made?.kind?.known == BlockKind.DIALOGUE && made.speakerKey != null
                Outcome(r.doc, caret = Caret(r.blockKey, 0), inherited = inherited, created = r.blockKey)
            }
            is BlockOps.EnterResult.Split -> Outcome(r.doc, caret = Caret(r.newBlockKey, 0), created = r.newBlockKey)
            is BlockOps.EnterResult.Nothing -> null
        }
    }

    /**
     * Backspace at the start of a block (SCR-15): merge into the previous block (toast when records
     * moved), remove an empty block (caret to the previous block's end), trash an emptied block that
     * anchors records (toast), or nothing at the first block of a scene.
     */
    fun backspaceAtStart(doc: ProjectDoc, blockKey: Key, ctx: EditContext): Outcome? {
        val previousKey = previousInScene(doc, blockKey)
        val before = previousKey?.let { WritingCounts.recordBaseline(doc, it, ctx.today) } ?: doc
        return when (val r = BlockOps.backspace(before, blockKey, atStart = true, ctx)) {
            is BlockOps.BackspaceResult.Nothing -> null
            is BlockOps.BackspaceResult.RemovedEmpty -> Outcome(r.doc, caret = r.caretBlockKey?.let { Caret(it, endOf(r.doc, it)) })
            is BlockOps.BackspaceResult.Merged -> Outcome(r.doc, caret = Caret(r.intoKey, r.caretOffset), toast = r.toast)
            is BlockOps.BackspaceResult.Deleted -> Outcome(
                r.doc,
                caret = previousKey?.takeIf { r.doc.manuscript.isLiveBlock(it) }?.let { Caret(it, endOf(r.doc, it)) },
                toast = r.result.toast,
            )
        }
    }

    /** Typing: the block's runs, with today's baseline for `오늘` counted first (STO-06). */
    fun setRuns(doc: ProjectDoc, blockKey: Key, runs: List<TextRun>, ctx: EditContext): ProjectDoc {
        val block = doc.index.block(blockKey) ?: return doc
        if (block.runs == runs) return doc
        return BlockOps.setRuns(WritingCounts.recordBaseline(doc, blockKey, ctx.today), blockKey, runs, ctx)
    }

    /** Sets a dialogue block's speaker (SCR-19). */
    fun changeSpeaker(doc: ProjectDoc, blockKey: Key, speakerKey: Key, ctx: EditContext): ProjectDoc =
        BlockOps.changeSpeaker(doc, blockKey, speakerKey, ctx)

    /** Raw length of a block's runs. */
    fun endOf(doc: ProjectDoc, blockKey: Key): Int = doc.index.block(blockKey)?.let { Runs.length(it.runs) } ?: 0

    /** The live block before [blockKey] in its scene. */
    fun previousInScene(doc: ProjectDoc, blockKey: Key): Key? {
        val block = doc.index.block(blockKey) ?: return null
        val scene = doc.manuscript.liveBlocksOf(block.sceneKey)
        val i = scene.indexOfFirst { it.key == blockKey }
        return if (i > 0) scene[i - 1].key else null
    }

    /**
     * Where the add sheet inserts (SCR-12): below the selected block, else at the end of the scene
     * under the header, else nowhere (an episode without scenes).
     */
    fun insertTarget(doc: ProjectDoc, selected: Key?, headerScene: Key?): InsertAt? {
        selected?.takeIf { doc.manuscript.isLiveBlock(it) }?.let { return InsertAt.After(it) }
        return headerScene?.takeIf { doc.manuscript.isLiveUnit(it) }?.let { InsertAt.SceneEnd(it) }
    }

    /** Where `블록 추가` inserts, for its subline (SCR-12): the virtual line, else [insertTarget]. */
    fun addPosition(doc: ProjectDoc, state: EditorState, headerScene: Key?): BlockPosition? =
        state.virtualScene?.let { BlockPosition.SceneStart(it) } ?: insertTarget(doc, state.selected, headerScene)?.toPosition()

    /** Kind of the writing position: the selected block's, `대사` on the virtual line, else null. */
    fun currentKind(doc: ProjectDoc, state: EditorState): BlockKind? = when {
        state.virtualScene != null -> BlockKind.DIALOGUE
        else -> state.selected?.let(doc.index::block)?.takeIf { doc.manuscript.isLiveBlock(it.key) }?.kind?.known
    }

    /**
     * `새 씬` / `/씬` (SCR-49 routing): `다음 씬 만들기` when the current block is the last of its
     * scene or there is none; `여기서 씬 나누기` below the current block otherwise. An episode
     * without scenes gets a new scene in [unitKey]. The sheets belong to SCR-49/50.
     */
    fun newSceneSheet(doc: ProjectDoc, projectKey: Key, current: Key?, sceneKey: Key?, unitKey: Key): SheetKey {
        val block = current?.let(doc.index::block)?.takeIf { doc.manuscript.isLiveBlock(it.key) }
        if (block != null) {
            val last = doc.manuscript.liveBlocksOf(block.sceneKey).lastOrNull()?.key
            if (last != block.key) return SceneSplitSheet(projectKey, block.key)
        }
        val scene = (block?.sceneKey ?: sceneKey)?.let(doc.index::unit)?.takeIf { doc.manuscript.isLiveUnit(it.key) }
        return if (scene != null) {
            NewUnitSheet(projectKey, parentKey = scene.parentKey, afterKey = scene.key)
        } else {
            NewUnitSheet(projectKey, parentKey = unitKey.takeIf { it != projectKey })
        }
    }

    /**
     * Whose speaker the pill, the toolbar and the picker change (SCR-01, SCR-19): the current dialogue
     * block; otherwise the next dialogue insert, showing the held pick or the speaker it would inherit.
     */
    fun speakerTarget(doc: ProjectDoc, state: EditorState): SpeakerTarget {
        val block = state.selected?.let(doc.index::block)?.takeIf { doc.manuscript.isLiveBlock(it.key) }
        if (block != null && block.kind.known == BlockKind.DIALOGUE) {
            return SpeakerTarget(block.key, block.speakerKey, block.sceneKey)
        }
        val position = when {
            block != null -> BlockPosition.After(block.key)
            state.virtualScene != null -> BlockPosition.SceneStart(state.virtualScene)
            else -> null
        }
        val inherited = position?.let { BlockOps.inheritedSpeaker(doc, it) }
        return SpeakerTarget(null, state.pendingSpeaker ?: inherited, block?.sceneKey ?: state.virtualScene)
    }

    /** Speakers of a scene (and its silent cast) for the picker's `이 씬 등장` (SCR-19). */
    fun sceneCast(doc: ProjectDoc, sceneKey: Key?): List<Key> {
        sceneKey ?: return emptyList()
        val scene = doc.index.unit(sceneKey)
        return (doc.manuscript.liveBlocksOf(sceneKey).mapNotNull { it.speakerKey } + scene?.silentCast.orEmpty()).distinct()
    }

    /** `최근`, most recent first, kept to [max]. */
    fun recentSpeakers(recent: List<Key>, picked: Key, max: Int = 10): List<Key> = (listOf(picked) + recent.filter { it != picked }).take(max)
}
