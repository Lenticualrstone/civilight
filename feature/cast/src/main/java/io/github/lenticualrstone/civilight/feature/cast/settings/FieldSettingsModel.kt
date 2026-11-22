package io.github.lenticualrstone.civilight.feature.cast.settings

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.field.FieldDefinitions
import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettings
import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettingsScreen
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** The kinds behind the `인물 · 장소 · 용어 · 세력` tabs, in tab order. */
val FIELD_KINDS: List<EntityKind> = listOf(EntityKind.CHARACTER, EntityKind.PLACE, EntityKind.TERM, EntityKind.FACTION)

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
}
