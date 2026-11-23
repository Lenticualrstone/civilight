package io.github.lenticualrstone.civilight.feature.story.home

import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.marks.MarkItem
import io.github.lenticualrstone.civilight.model.marks.MarkState
import io.github.lenticualrstone.civilight.model.marks.Marks
import io.github.lenticualrstone.civilight.model.text.plainText
import io.github.lenticualrstone.civilight.navigation.SheetKey
import kotlinx.serialization.Serializable

/** (0.2.0, N-41) The `할 일 · 복선` list sheet over home; local to home (no route exists for it yet, Requests). */
@Serializable
data class HomeMarksSheet(val projectKey: Key) : SheetKey

/** One filter chip: `열림 3`. */
data class MarkChip(val state: MarkState, val label: String, val count: Int)

/**
 * One row: [kind] `복선` or `할 일`, [title] the foreshadowing's name or the memo's first line, [sub]
 * `1-7 씬 3 · 거둘 곳 · 2-5`. [blockKey] is the block the row opens (the plant's block as drawn now, or the
 * memo); null when nothing live follows a deleted plant block, and the row is then not tappable. [moved]:
 * the plant's own block is deleted and the mark is drawn on the next block (rule 12 §2).
 */
data class MarkRow(
    val key: Key,
    val kind: String,
    val title: String,
    val sub: String,
    val blockKey: Key?,
    val moved: Boolean,
)

data class MarksUi(val filter: MarkState, val chips: List<MarkChip>, val rows: List<MarkRow>)

/**
 * The home `작업` › `할 일 · 복선` list (N-41, SCR-77) over `core:model` [Marks]: filters `열림 · 거둠 · 끝냄`
 * with counts, rows in [Marks.items] order (foreshadowings, then memo to-dos in manuscript order). Items
 * whose block sits in a hidden scene (or under a hidden unit) are left out while hidden items are locked
 * (rule 19), and the counts follow the same set; a visible plant's `거둘 곳` in a hidden unit is dropped
 * from its row. No grid view.
 */
object HomeMarks {
    fun word(state: MarkState): String = when (state) {
        MarkState.OPEN -> HomeCopy.MARK_OPEN
        MarkState.PAID_OFF -> HomeCopy.MARK_PAID_OFF
        MarkState.DONE -> HomeCopy.MARK_DONE
    }

    /** The items shown: every live item, without those in a hidden scene while [locked]. */
    fun items(doc: ProjectDoc, locked: Boolean): List<MarkItem> =
        Marks.items(doc).filterNot { locked && inHiddenScene(doc, it) }

    fun build(doc: ProjectDoc, filter: MarkState, locked: Boolean): MarksUi {
        val items = items(doc, locked)
        val counts = items.groupingBy(Marks::state).eachCount()
        val chips = MarkState.entries.map { s -> (counts[s] ?: 0).let { n -> MarkChip(s, "${word(s)} $n", n) } }
        val codes = UnitCodes(doc)
        val rows = items.filter { Marks.state(it) == filter }.map { row(doc, codes, it, locked) }
        return MarksUi(filter, chips, rows)
    }

    private fun row(doc: ProjectDoc, codes: UnitCodes, item: MarkItem, locked: Boolean): MarkRow = when (item) {
        is MarkItem.Plant -> {
            val resolved = Marks.resolve(doc, item.foreshadow.plant)
            val where = resolved.blockKey?.let { doc.index.block(it)?.sceneKey }?.let(codes::sceneRef)
            // While locked, a `거둘 곳` in a hidden unit is not named, not even by its code (rule 19).
            val payoffAt = item.foreshadow.payoffUnitKey?.takeUnless { locked && isHiddenUnit(doc, it) }
                ?.let(codes::unitLabel)?.let { "${HomeCopy.MARK_PAYOFF_AT} · $it" }
            MarkRow(
                key = item.key,
                kind = HomeCopy.MARK_PLANT,
                title = item.foreshadow.name,
                sub = listOfNotNull(where, payoffAt).joinToString(" · "),
                blockKey = resolved.blockKey,
                moved = resolved.moved,
            )
        }
        is MarkItem.Todo -> {
            val line = item.block.runs.plainText.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
            MarkRow(
                key = item.key,
                kind = HomeCopy.MARK_TODO,
                title = line ?: HomeCopy.NO_TITLE,
                sub = codes.sceneRef(item.block.sceneKey).orEmpty(),
                blockKey = item.block.key,
                moved = false,
            )
        }
    }

    /** The block an item belongs to is in a hidden scene (the plant's block as drawn now, else its own). */
    private fun inHiddenScene(doc: ProjectDoc, item: MarkItem): Boolean {
        val blockKey = when (item) {
            is MarkItem.Plant -> Marks.resolve(doc, item.foreshadow.plant).blockKey ?: item.foreshadow.plant.blockKey
            is MarkItem.Todo -> item.block.key
        }
        val sceneKey = doc.index.block(blockKey)?.sceneKey ?: return false
        return isHiddenUnit(doc, sceneKey)
    }

    /** [unitKey] is hidden, by its own flag or an ancestor's (a hidden episode hides its scenes). */
    private fun isHiddenUnit(doc: ProjectDoc, unitKey: Key): Boolean {
        val unit = doc.index.unit(unitKey) ?: return false
        return unit.hidden || doc.index.ancestors(unit).any { it.hidden }
    }
}
