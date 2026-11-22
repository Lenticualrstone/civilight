package io.github.lenticualrstone.civilight.feature.world.common

import io.github.lenticualrstone.civilight.domain.world.item.FieldText
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.history
import io.github.lenticualrstone.civilight.model.doc.recordAt
import io.github.lenticualrstone.civilight.model.position.LensPoint

/** A project-field row of a world card (WLD-06): `상태 | 대정전 이후 ◆`; [value] null when empty. */
data class FieldRowUi(val fieldDefKey: Key, val label: String, val value: String?, val changed: Boolean)

/**
 * The project fields of one card: [filled] rows show in view; [empty] rows only after `보기`
 * ([emptyLine] = `빈 항목 N · 보기`, null when nothing is empty).
 */
data class FieldSectionUi(val filled: List<FieldRowUi>, val empty: List<FieldRowUi>) {
    val emptyLine: String? get() = if (empty.isEmpty()) null else WorldCopy.emptyFields(empty.size)
}

/**
 * Project fields of places, terms and factions through the shared field mechanism (인물 항목 rule 5):
 * every live, shown definition of the kind in field order, valued at the lens (`◆` records win
 * over the base value; `예정` records are not applied at 최신).
 */
object FieldRows {
    fun build(doc: ProjectDoc, kind: EntityKind, entityKey: Key, base: Map<Key, FieldValue>, lens: LensPoint): FieldSectionUi {
        val defs = doc.index.fieldDefs(kind).filter { it.deletedAt == null && !it.definitionHidden }
        val rows = defs.map { def ->
            val target = ChangeTarget.Field(kind, entityKey, def.key)
            val record = doc.recordAt(target, lens)
            val value = if (record != null) record.valueAs(FieldValue.serializer()) else base[def.key]
            val text = FieldText.of(doc, value)?.takeIf { it.isNotBlank() }
            FieldRowUi(def.key, def.name, text, changed = doc.history(target).isNotEmpty())
        }
        return FieldSectionUi(filled = rows.filter { it.value != null }, empty = rows.filter { it.value == null })
    }
}
