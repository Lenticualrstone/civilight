package io.github.lenticualrstone.civilight.domain.recorder.put

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEditContext
import io.github.lenticualrstone.civilight.domain.recorder.Toast
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.link.UnitLabels
import io.github.lenticualrstone.civilight.domain.recorder.state.PlacementLabels
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.EpisodeFile
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.PlacementKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind

/** Where `원고에 넣기` puts the memo block. */
sealed interface ManuscriptSpot {
    /** After the scene's last block, before the next 씬 머리. */
    data class SceneEnd(val sceneKey: Key) : ManuscriptSpot

    /** Directly after block [blockKey] (by key; the number is only shown). */
    data class AfterBlock(val blockKey: Key) : ManuscriptSpot
}

/**
 * `원고에 넣기` / `여기 넣기` (LRC-33, LRC-34, SCR-59): exactly one `⌁ 작가 메모` block holding a copy
 * of the record's current text, numbered with the episode's next number (rule 12: the counter
 * only grows), with `recorderSource` pointing back at the record. Memo blocks are never exported
 * as game text (their owner's display rules apply).
 */
object ManuscriptInsert {
    fun insert(doc: ProjectDoc, library: RecorderLibrary, recordKey: Key, spot: ManuscriptSpot, ctx: RecorderEditContext): Insertion {
        val record = Put.source(library, recordKey)
        Put.requireText(record)
        val index = doc.index
        val anchor: Block? = (spot as? ManuscriptSpot.AfterBlock)?.let {
            index.block(it.blockKey)?.takeIf(doc.liveness::isLive) ?: throw InsertionException("block ${it.blockKey} is not live")
        }
        val sceneKey = anchor?.sceneKey ?: (spot as ManuscriptSpot.SceneEnd).sceneKey
        val scene = index.unit(sceneKey)?.takeIf { index.isScene(it) && doc.liveness.isLive(it) }
            ?: throw InsertionException("scene $sceneKey is not live")
        val sceneBlocks = index.blocksOfScene(sceneKey)
        val order = if (anchor == null) {
            OrderKeys.after(sceneBlocks.lastOrNull()?.order)
        } else {
            val next = sceneBlocks.firstOrNull { it.order > anchor.order }
            OrderKeys.between(anchor.order, next?.order)
        }
        val unitKey = index.numberingUnitKeyOfScene(sceneKey) ?: throw InsertionException("no numbering unit for $sceneKey")
        val (number, withCounter) = takeNumber(doc, unitKey)

        val source = Put.sourceOf(record)
        val block = Block(
            key = ctx.newKey(), sceneKey = sceneKey, order = order, number = number, kind = BlockKind.MEMO.open(),
            runs = Put.copyRuns(record.runs, doc.key, LinkTargetKind.SCRIPT, ctx), recorderSource = source,
            createdAt = ctx.now, updatedAt = ctx.now, updatedBy = ctx.device,
        )
        val file = withCounter.episodes[unitKey] ?: EpisodeFile(unitKey = unitKey)
        val nextDoc = withCounter.copy(episodes = withCounter.episodes + (unitKey to file.copy(blocks = file.blocks + block)))

        val label = "${UnitLabels(nextDoc).sceneRef(scene)}${RecorderCopy.SEP}${PlacementLabels.number(number)}"
        val (nextLibrary, placement, addedLink) = Put.commit(
            library, record, doc.key, PlacementKind.SCRIPT_BLOCK, block.key, sceneKey, null, label,
            TargetRef(doc.key, RecorderTargetKind.UNIT, sceneKey), ctx,
        )
        return Insertion(
            doc = nextDoc,
            library = nextLibrary,
            placement = placement,
            toast = toast(label),
            undo = Put.undoToken(record, placement, addedLink, CreatedContent.ScriptBlock(block.key, unitKey)),
        )
    }

    /** `1-7 씬 3 · #062 작가 메모 넣음` · `되돌리기` · `가 보기` (rules/23.md §4, §6 Q4). */
    fun toast(label: String) = Toast(
        "$label ${RecorderCopy.AUTHOR_MEMO} ${RecorderCopy.PUT_DONE}",
        listOf(RecorderCopy.UNDO, RecorderCopy.GO_SEE),
    )

    /**
     * The next number of numbering unit [unitKey] and the doc with the counter advanced. The
     * counter is `StoryUnit.nextBlockNumber` (or `Project.counters.nextBlockNumber` for a 1-level
     * structure); a number already used in the unit is never issued again.
     */
    fun takeNumber(doc: ProjectDoc, unitKey: Key): Pair<Int, ProjectDoc> {
        val used = (doc.episodes[unitKey]?.blocks.orEmpty().maxOfOrNull { it.number } ?: 0) + 1
        return if (unitKey == doc.key) {
            val counter = doc.project.counters.nextBlockNumber ?: 1
            val number = maxOf(counter, used)
            number to doc.copy(project = doc.project.copy(counters = doc.project.counters.copy(nextBlockNumber = number + 1)))
        } else {
            val unit = doc.index.unit(unitKey) ?: throw InsertionException("no unit $unitKey")
            val number = maxOf(unit.nextBlockNumber ?: 1, used)
            number to doc.copy(
                story = doc.story.copy(units = doc.story.units.map { if (it.key == unitKey) it.copy(nextBlockNumber = number + 1) else it }),
            )
        }
    }

    /** The number the next block of [unitKey] would get (`#063` after an undone `#062`). */
    fun nextNumber(doc: ProjectDoc, unitKey: Key): Int = takeNumber(doc, unitKey).first
}
