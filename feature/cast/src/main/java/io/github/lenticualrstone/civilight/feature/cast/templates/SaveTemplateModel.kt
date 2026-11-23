package io.github.lenticualrstone.civilight.feature.cast.templates

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettings
import io.github.lenticualrstone.civilight.domain.cast.field.FieldTemplates
import io.github.lenticualrstone.civilight.domain.cast.field.FieldTypes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.FieldTemplate
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** One checkbox row of `담을 항목`: `<name>` with `<종류> · <detail>`, `숨김` for a hidden definition. */
data class SaveRow(val key: Key, val name: String, val sub: String, val hidden: Boolean, val checked: Boolean)

/** One kind group of `담을 항목` (`인물 · 11`). */
data class SaveGroup(val kind: EntityKind, val label: String, val rows: List<SaveRow>)

/** `새 템플릿` or `있는 템플릿 바꾸기`. */
enum class SaveTarget { NEW, REPLACE }

/** Everything the `템플릿으로 저장` sheet shows (CST-57, S31). */
data class SaveTemplateView(
    val groups: List<SaveGroup>,
    val picksTitle: String,
    /** `◇ 나이 · 출생 없이 · 직접 넣기` under each `계산` row whose source is unchecked. */
    val calcNotices: Map<Key, String>,
    /** `이 인물만 항목 1 · 빠짐`, or null. */
    val scopedLine: String?,
    /** `◇ 같은 이름 템플릿 있음` (visible templates only), or null. */
    val sameName: String?,
    /** `◇ 잠가도 보임`: a hidden project saving a template that is not hidden. */
    val visibleNotice: String?,
    val canSave: Boolean,
)

/** Pure model of `템플릿으로 저장` (CST-57). Saving never writes to the project. */
object SaveTemplateModel {
    fun initialPicks(doc: ProjectDoc): Set<Key> = FieldTemplates.defaultPicks(doc)

    /** The `숨긴 템플릿` switch starts on in a hidden project (TC-F-41), off elsewhere. */
    fun initialHidden(doc: ProjectDoc): Boolean = doc.project.protections.hidden

    fun rowSub(kindLabel: String?, detail: String?): String = CastCopy.join(kindLabel, detail)

    fun groups(doc: ProjectDoc, picks: Set<Key>): List<SaveGroup> = FieldTemplates.KINDS.mapNotNull { kind ->
        val defs = FieldTemplates.candidates(doc, kind)
        if (defs.isEmpty()) return@mapNotNull null
        SaveGroup(
            kind,
            TemplateCopy.kindCount(kind, defs.size),
            defs.map { d ->
                val type = d.type.known?.let(FieldTypes::label) ?: d.type.raw
                SaveRow(d.key, d.name, rowSub(type, FieldSettings.detail(doc, d)), d.definitionHidden, d.key in picks)
            },
        )
    }

    fun view(
        doc: ProjectDoc,
        picks: Set<Key>,
        name: String,
        hidden: Boolean,
        visibleTemplates: List<FieldTemplate>,
        target: SaveTarget = SaveTarget.NEW,
        replaceKey: Key? = null,
    ): SaveTemplateView {
        val groups = groups(doc, picks)
        val checked = groups.sumOf { g -> g.rows.count { it.checked } }
        val scoped = FieldTemplates.scopedCount(doc)
        val except = if (target == SaveTarget.REPLACE) replaceKey else null
        return SaveTemplateView(
            groups = groups,
            picksTitle = TemplateCopy.picks(checked),
            calcNotices = FieldTemplates.calcWithoutSource(doc, picks).associate { it.calcKey to TemplateCopy.calcWithoutSource(it.calcName, it.sourceName) },
            scopedLine = if (scoped > 0) TemplateCopy.scopedLeftOut(scoped) else null,
            sameName = if (FieldTemplates.hasSameName(visibleTemplates, name, except)) TemplateCopy.SAME_NAME else null,
            visibleNotice = if (doc.project.protections.hidden && !hidden) TemplateCopy.VISIBLE_NOTICE else null,
            canSave = name.isNotBlank() && checked > 0 && (target == SaveTarget.NEW || visibleTemplates.any { it.key == replaceKey }),
        )
    }

    /** Toggles one row. */
    fun toggle(picks: Set<Key>, key: Key, on: Boolean): Set<Key> = if (on) picks + key else picks - key

    /**
     * The template `저장` writes (the project is untouched): a new one after the last template, or
     * [replace]'s field list replaced by the checked definitions (keys kept by source key or origin).
     */
    fun save(
        doc: ProjectDoc,
        picks: Set<Key>,
        name: String,
        hidden: Boolean,
        all: List<FieldTemplate>,
        ctx: EditContext,
        replace: FieldTemplate? = null,
    ): FieldTemplate = if (replace != null) {
        FieldTemplates.replaceFields(replace, doc, picks, ctx, name = name, hidden = hidden)
    } else {
        FieldTemplates.fromProject(doc, picks, name, hidden, ctx, after = all.maxOfOrNull { it.order })
    }
}
