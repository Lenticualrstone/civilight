package io.github.lenticualrstone.civilight.model.marks

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind

/** `열림 · 거둠 · 끝냄` (SCR-77, the home `할 일 · 복선` filters). */
enum class MarkState { OPEN, PAID_OFF, DONE }

/** One row of the `할 일 · 복선` list: a foreshadowing or a memo to-do. */
sealed interface MarkItem {
    val key: Key

    data class Plant(val foreshadow: Foreshadow) : MarkItem {
        override val key: Key get() = foreshadow.key
    }

    /** A memo block with `할 일` on; [key] is the block key. */
    data class Todo(val block: Block) : MarkItem {
        override val key: Key get() = block.key
    }
}

/** What an anchor of a mark sits on now (rule 12 §2). */
data class ResolvedMark(
    /** The live block the mark is drawn on, or null when no live block follows (unplaced). */
    val blockKey: Key?,
    /** True while the stored block is not live: drawn dotted on [blockKey] (`근거 지워짐`). */
    val moved: Boolean,
)

/** Which anchor of a foreshadowing a block carries. */
enum class MarkRole { PLANT, MENTION, PAYOFF }

/** One mark shown in a block's `속성` rows. */
data class BlockMark(val foreshadow: Foreshadow, val role: MarkRole, val moved: Boolean)

/**
 * Pure reads of N-41 marks (SCR-77). Deleted rows are never counted: a trashed foreshadowing, and a
 * to-do whose memo block is not live (it goes to the trash with its memo). Hidden locked items are
 * the caller's filter (rule 19).
 */
object Marks {
    /** Live foreshadowings, then live memo to-dos in manuscript order. */
    fun items(doc: ProjectDoc): List<MarkItem> {
        val plants = doc.story.foreshadows.filter { it.deletedAt == null }.map { MarkItem.Plant(it) }
        val todos = doc.manuscript.liveBlocks
            .filter { it.kind.known == BlockKind.MEMO && it.todo != null }
            .map { MarkItem.Todo(it) }
        return plants + todos
    }

    fun state(item: MarkItem): MarkState = when (item) {
        is MarkItem.Plant -> if (item.foreshadow.payoff != null) MarkState.PAID_OFF else MarkState.OPEN
        is MarkItem.Todo -> if (item.block.todo?.done == true) MarkState.DONE else MarkState.OPEN
    }

    /** Items in [state]. */
    fun filter(doc: ProjectDoc, state: MarkState): List<MarkItem> = items(doc).filter { state(it) == state }

    /** Count per state (every state present, 0 when none). */
    fun counts(doc: ProjectDoc): Map<MarkState, Int> {
        val all = items(doc).groupingBy(::state).eachCount()
        return MarkState.entries.associateWith { all[it] ?: 0 }
    }

    /**
     * Where [anchor] is drawn: its own block while live; otherwise the first live block after its
     * old manuscript position (the §5.5 relocation the `◆` records use), flagged [ResolvedMark.moved];
     * null when nothing live follows or the block is gone.
     */
    fun resolve(doc: ProjectDoc, anchor: MarkAnchor): ResolvedMark {
        val m = doc.manuscript
        if (m.isLiveBlock(anchor.blockKey)) return ResolvedMark(anchor.blockKey, moved = false)
        val target = m.relocationTarget(Anchor.AtBlock(anchor.blockKey))
        return ResolvedMark((target as? Anchor.AtBlock)?.blockKey, moved = true)
    }

    /** The block a row of the list opens: the plant's resolved block, or the to-do's memo block. */
    fun jumpTarget(doc: ProjectDoc, item: MarkItem): Key? = when (item) {
        is MarkItem.Plant -> resolve(doc, item.foreshadow.plant).blockKey
        is MarkItem.Todo -> item.block.key
    }

    /** Marks drawn on [blockKey] (live foreshadowings only), plant first. */
    fun on(doc: ProjectDoc, blockKey: Key): List<BlockMark> {
        val out = ArrayList<BlockMark>()
        for (f in doc.story.foreshadows) {
            if (f.deletedAt != null) continue
            fun add(anchor: MarkAnchor, role: MarkRole) {
                val r = resolve(doc, anchor)
                if (r.blockKey == blockKey) out += BlockMark(f, role, r.moved)
            }
            add(f.plant, MarkRole.PLANT)
            f.mentions.forEach { add(it, MarkRole.MENTION) }
            f.payoff?.let { add(it, MarkRole.PAYOFF) }
        }
        return out
    }

    /** True when any live foreshadowing anchors on [blockKey] itself (an empty block with a mark is not empty, SCR-04). */
    fun carriesMark(doc: ProjectDoc, blockKey: Key): Boolean =
        doc.story.foreshadows.any { f -> f.deletedAt == null && f.anchors.any { it.blockKey == blockKey } }

    /**
     * Anchors on [fromBlockKey] moved to [toBlockKey]: for a block removed outright (a `합치기` merge
     * removes the later block rather than trashing it), so its marks join the kept block.
     */
    fun moveAnchors(doc: ProjectDoc, fromBlockKey: Key, toBlockKey: Key): ProjectDoc =
        rewriteAnchors(doc) { if (it.blockKey == fromBlockKey) it.copy(blockKey = toBlockKey) else it }

    /**
     * Before blocks are purged for good: anchors on them are pinned to where they are drawn now
     * ([resolve]), so a purge never leaves a mark without a block. Anchors with no live block after
     * them keep their key (drawn unplaced).
     */
    fun settleBeforePurge(doc: ProjectDoc, purgedBlockKeys: Set<Key>): ProjectDoc {
        if (purgedBlockKeys.isEmpty() || doc.story.foreshadows.isEmpty()) return doc
        return rewriteAnchors(doc) { a ->
            if (a.blockKey !in purgedBlockKeys) a
            else resolve(doc, a).blockKey?.let { a.copy(blockKey = it) } ?: a
        }
    }

    private fun rewriteAnchors(doc: ProjectDoc, f: (MarkAnchor) -> MarkAnchor): ProjectDoc {
        val next = doc.story.foreshadows.map { fs ->
            fs.copy(plant = f(fs.plant), mentions = fs.mentions.map(f), payoff = fs.payoff?.let(f))
        }
        return if (next == doc.story.foreshadows) doc else doc.copy(story = doc.story.copy(foreshadows = next))
    }
}
