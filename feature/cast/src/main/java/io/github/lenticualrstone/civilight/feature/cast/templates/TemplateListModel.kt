package io.github.lenticualrstone.civilight.feature.cast.templates

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.field.FieldTemplates
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.FieldTemplate
import io.github.lenticualrstone.civilight.model.app.TemplateField
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.FieldBundle
import java.time.Instant
import java.time.ZoneId

/** A template row: name, per-kind counts, a lock mark while it is hidden (and open). */
data class TemplateRowView(val key: Key, val title: String, val sub: String, val lockMark: Boolean)

/** A `지운 템플릿` row: name, `11월 23일 지움 · 30일 뒤 삭제`. */
data class TemplateTrashRowView(val key: Key, val title: String, val sub: String)

/** `설정 › 인물 템플릿` (CST-58, S32). Nothing here names a hidden template while the group is locked. */
data class TemplateListView(
    /** `템플릿 · 3` (visible live templates only). */
    val sectionTitle: String,
    val rows: List<TemplateRowView>,
    /** `숨긴 템플릿 · 지문으로 보기`, with no count. */
    val lockRow: Boolean,
    /** `지운 템플릿 · 2`, or null when there is none to show. */
    val trashRow: String?,
    /** The `템플릿 없음` card. */
    val empty: Boolean,
)

/** Where `템플릿 만들기` starts from (CST-58). */
enum class TemplateStart { EMPTY, BASIC, GAME_PROFILE, FROM_TEMPLATE, FROM_PROJECT }

/** Pure models of the template list, its trash and `템플릿 만들기`. */
object TemplateListModel {
    fun view(all: List<FieldTemplate>, hiddenUnlocked: Boolean): TemplateListView {
        val visible = FieldTemplates.visible(all, hiddenUnlocked)
        val lock = FieldTemplates.showsLockRow(all, hiddenUnlocked)
        val trashed = FieldTemplates.trashed(all, hiddenUnlocked).size
        return TemplateListView(
            sectionTitle = TemplateCopy.list(visible.size),
            rows = visible.map { TemplateRowView(it.key, it.name, FieldTemplates.kindCounts(it), it.hidden) },
            lockRow = lock,
            trashRow = if (trashed > 0) TemplateCopy.trashRow(trashed) else null,
            empty = visible.isEmpty() && !lock,
        )
    }

    fun trashRows(all: List<FieldTemplate>, hiddenUnlocked: Boolean, zone: ZoneId): List<TemplateTrashRowView> =
        FieldTemplates.trashed(all, hiddenUnlocked).map { t ->
            val date = Instant.ofEpochMilli(t.deletedAt ?: 0L).atZone(zone).toLocalDate()
            TemplateTrashRowView(t.key, t.name, TemplateCopy.trashSub(date.monthValue, date.dayOfMonth))
        }

    /** The radios of `시작`, in order. */
    val STARTS: List<TemplateStart> = TemplateStart.entries

    fun startLabel(start: TemplateStart): String = when (start) {
        TemplateStart.EMPTY -> TemplateCopy.START_EMPTY
        TemplateStart.BASIC -> io.github.lenticualrstone.civilight.domain.cast.field.FieldBundles.label(FieldBundle.BASIC)
        TemplateStart.GAME_PROFILE -> io.github.lenticualrstone.civilight.domain.cast.field.FieldBundles.label(FieldBundle.GAME_PROFILE)
        TemplateStart.FROM_TEMPLATE -> TemplateCopy.FROM_TEMPLATE
        TemplateStart.FROM_PROJECT -> TemplateCopy.FROM_PROJECT
    }

    /** `만들기` is enabled with a name and, for the expanding sources, a picked source. */
    fun canMake(name: String, start: TemplateStart, sourceKey: Key?): Boolean =
        name.isNotBlank() && (start !in setOf(TemplateStart.FROM_TEMPLATE, TemplateStart.FROM_PROJECT) || sourceKey != null)

    /** The new template's fields for [start]; [fromTemplate] and [fromProject] are the picked sources. */
    fun startFields(start: TemplateStart, ctx: EditContext, fromTemplate: FieldTemplate?, fromProject: ProjectDoc?): List<TemplateField> = when (start) {
        TemplateStart.EMPTY -> emptyList()
        TemplateStart.BASIC -> FieldTemplates.fieldsFromBundle(FieldBundle.BASIC, ctx)
        TemplateStart.GAME_PROFILE -> FieldTemplates.fieldsFromBundle(FieldBundle.GAME_PROFILE, ctx)
        TemplateStart.FROM_TEMPLATE -> fromTemplate?.let { FieldTemplates.copyFields(it, ctx) }.orEmpty()
        TemplateStart.FROM_PROJECT -> fromProject?.let { FieldTemplates.fromProject(it, FieldTemplates.defaultPicks(it), "x", false, ctx).fields }.orEmpty()
    }

    /** `템플릿 만들기`: a new template after the last one. */
    fun make(
        name: String,
        hidden: Boolean,
        start: TemplateStart,
        all: List<FieldTemplate>,
        ctx: EditContext,
        fromTemplate: FieldTemplate? = null,
        fromProject: ProjectDoc? = null,
    ): FieldTemplate = FieldTemplates.create(
        name, hidden, ctx, after = all.maxOfOrNull { it.order },
        fields = startFields(start, ctx, fromTemplate, fromProject),
    )
}
