package io.github.lenticualrstone.civilight.feature.cast.settings

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.field.FieldDefinitions
import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettings
import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettingsScreen
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** The kinds behind the `인물 · 장소 · 용어 · 세력` tabs, in tab order. */
val FIELD_KINDS: List<EntityKind> = listOf(EntityKind.CHARACTER, EntityKind.PLACE, EntityKind.TERM, EntityKind.FACTION)

/** The computed fixed fields a record header can hide (CST-14): `첫 등장` and `보이스`. */
enum class HeaderFixed(val label: String) { FIRST_APPEARANCE(CastUiCopy.FIRST_APPEARANCE), VOICE(CastUiCopy.VOICE) }

/** Pure model of `프로젝트 설정 › 인물 항목` (CST-19) and its grip reorder. */
object FieldSettingsModel {
    fun screen(doc: ProjectDoc, kind: EntityKind): FieldSettingsScreen = FieldSettings.screen(doc, kind)

    /** The row order after dragging [from] to [to] (indices in [keys]); out-of-range targets are clamped. */
    fun reorder(keys: List<Key>, from: Int, to: Int): List<Key> {
        if (from !in keys.indices) return keys
        val target = to.coerceIn(0, keys.lastIndex)
        if (target == from) return keys
        return keys.toMutableList().apply { add(target, removeAt(from)) }
    }

    /** The order and the dragged row's remaining offset after [dragStep]. */
    data class DragStep(val order: List<Key>, val offsetPx: Float)

    /**
     * One step of a live drag: when the dragged row [key] (at [offsetPx] from its resting place in [order])
     * has crossed into another slot, [order] is reordered at once and the offset is reduced by the rows
     * crossed, so the row stays under the finger while its neighbours move aside (the list animates
     * their placement). Nothing changes while the row is still within its own slot.
     */
    fun dragStep(order: List<Key>, key: Key, offsetPx: Float, rowHeightPx: Float): DragStep {
        val from = order.indexOf(key)
        if (from < 0 || rowHeightPx <= 0f) return DragStep(order, offsetPx)
        val to = dropIndex(from, offsetPx, rowHeightPx, order.size)
        if (to == from) return DragStep(order, offsetPx)
        return DragStep(reorder(order, from, to), offsetPx - (to - from) * rowHeightPx)
    }

    /** The drop index of a drag of [offsetPx] from [from], rows [rowHeightPx] tall. */
    fun dropIndex(from: Int, offsetPx: Float, rowHeightPx: Float, count: Int): Int {
        if (rowHeightPx <= 0f || count == 0) return from
        val steps = Math.round(offsetPx / rowHeightPx)
        return (from + steps).coerceIn(0, count - 1)
    }

    /** Writes a reorder: [defKey] gets an order key between its new neighbours (`끌어서 순서`). */
    fun move(doc: ProjectDoc, kind: EntityKind, defKey: Key, from: Int, to: Int, ctx: EditContext): ProjectDoc {
        val keys = screen(doc, kind).rows.map { it.key }
        if (keys.getOrNull(from) != defKey) return doc
        val next = reorder(keys, from, to)
        val at = next.indexOf(defKey)
        if (next == keys) return doc
        return FieldDefinitions.move(doc, defKey, afterKey = next.getOrNull(at - 1), beforeKey = next.getOrNull(at + 1), ctx = ctx)
    }

    /** Whether the record header shows [which] (CST-14; on by default). */
    fun showsInHeader(doc: ProjectDoc, which: HeaderFixed): Boolean = when (which) {
        HeaderFixed.FIRST_APPEARANCE -> doc.project.cast.showFirstAppearanceInHeader
        HeaderFixed.VOICE -> doc.project.cast.showVoiceProgressInHeader
    }

    /**
     * The switch of a computed fixed field in the `고정 항목` box (CST-14 [PROPOSED control]): it can
     * be hidden from the record header, never deleted.
     */
    fun setShowInHeader(doc: ProjectDoc, which: HeaderFixed, show: Boolean, ctx: EditContext): ProjectDoc {
        if (showsInHeader(doc, which) == show) return doc
        val cast = when (which) {
            HeaderFixed.FIRST_APPEARANCE -> doc.project.cast.copy(showFirstAppearanceInHeader = show)
            HeaderFixed.VOICE -> doc.project.cast.copy(showVoiceProgressInHeader = show)
        }
        return doc.copy(project = doc.project.copy(cast = cast, updatedAt = ctx.now, updatedBy = ctx.device))
    }
}
