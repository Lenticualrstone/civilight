package io.github.lenticualrstone.civilight.feature.library.newproject

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.cast.field.FieldBundles
import io.github.lenticualrstone.civilight.domain.library.newproject.CreatedProject
import io.github.lenticualrstone.civilight.domain.library.newproject.NewProjectFlow
import io.github.lenticualrstone.civilight.domain.recorder.hooks.RecorderHooks
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.FieldBundle
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary

/** Where `만들기` writes: the 서재 (a complete new project) and the recorder library (links). */
interface CreatorPorts {
    suspend fun addProject(doc: ProjectDoc)

    suspend fun updateRecorder(transform: (RecorderLibrary) -> RecorderLibrary)
}

/** What happened on `만들기`, for the navigation that follows. */
data class CreateResult(
    val projectKey: Key,
    /** Created hidden: it joins the hidden group (LIB-12 "지금은 열린 채" needs the shell, see Requests). */
    val hidden: Boolean,
    /** `세계 달력 만들기` was picked: the calendar editor opens after creation (Spec conflicts). */
    val opensCalendar: Boolean,
)

/**
 * `만들기` (LIB-12): the project document from `domain:library` [NewProjectFlow] (units, numbering,
 * time, protections from the rating, protagonist), the field set from `domain:cast` [FieldBundles]
 * (`기본` · `게임 프로필` · `비어 있음` · `다른 프로젝트에서`, definitions only), then the record links
 * through `domain:recorder` [RecorderHooks.linkRecords] (origin `시작 재료`, text not inserted).
 */
object ProjectCreator {
    /** The complete document, nothing written yet. [bundleSource] = the `다른 프로젝트에서` project. */
    fun build(form: NewProjectForm, defaults: RatingDefaults, ctx: ActionContext, bundleSource: ProjectDoc?): CreatedProject {
        val created = NewProjectFlow.create(NewProjectRules.toDraft(form), defaults, ctx.now, ctx.device, ctx.newKey)
        val withFields = when (form.bundle) {
            FieldBundle.FROM_PROJECT -> bundleSource?.let { FieldBundles.applyFromProject(created.doc, it, ctx.edit).doc } ?: created.doc
            else -> FieldBundles.apply(created.doc, form.bundle, ctx.edit).doc
        }
        return CreatedProject(withFields, created.entry, emptyList(), created.protections)
    }

    suspend fun create(
        ports: CreatorPorts,
        form: NewProjectForm,
        defaults: RatingDefaults,
        ctx: ActionContext,
        bundleSource: ProjectDoc?,
    ): CreateResult {
        val created = build(form, defaults, ctx, bundleSource)
        val key = created.doc.key
        ports.addProject(created.doc)
        val records = form.linkedRecordKeys.distinct()
        if (records.isNotEmpty()) ports.updateRecorder { RecorderHooks.linkRecords(it, records, key, ctx.recorder) }
        return CreateResult(key, created.protections.protections.hidden, form.timeMode == WorldTimeMode.CALENDAR)
    }
}
