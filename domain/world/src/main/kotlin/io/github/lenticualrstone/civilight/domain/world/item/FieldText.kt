package io.github.lenticualrstone.civilight.domain.world.item

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.cast.FieldValueState
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.docText

/** Short display text of a field value on world cards and history rows (`빛 있음`, `??`). */
object FieldText {
    fun of(doc: ProjectDoc, value: FieldValue?): String? {
        if (value == null) return null
        return when (value.state.known) {
            FieldValueState.UNKNOWN -> "??"
            FieldValueState.FALLBACK_TEXT -> value.fallbackText
            FieldValueState.VALUE -> when (val p = value.payload) {
                is FieldPayload.Texts -> p.values.joinToString(Copy.SEP)
                is FieldPayload.Rich -> p.doc.docText.lineSequence().firstOrNull()
                is FieldPayload.Number -> p.value
                is FieldPayload.Choice -> {
                    val labels = p.optionKeys.mapNotNull { key ->
                        doc.project.fieldDefs.firstNotNullOfOrNull { d -> d.options?.firstOrNull { it.key == key }?.label }
                    }
                    (labels + listOfNotNull(p.freeText)).joinToString(Copy.SEP)
                }
                is FieldPayload.Date -> listOfNotNull(p.year.toString(), p.month?.toString(), p.day?.toString()).joinToString(".")
                is FieldPayload.Anniversary -> "${p.month}/${p.day}"
                is FieldPayload.Link -> p.targetKeys.mapNotNull { k ->
                    doc.index.place(k)?.name ?: doc.index.faction(k)?.name ?: doc.index.term(k)?.name
                }.joinToString(Copy.SEP)
                is FieldPayload.Unknown, null -> null
            }
            null -> null
        }
    }
}
