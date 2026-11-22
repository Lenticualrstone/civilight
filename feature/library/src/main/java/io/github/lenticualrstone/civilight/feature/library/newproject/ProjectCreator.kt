package io.github.lenticualrstone.civilight.feature.library.newproject

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.cast.field.FieldBundles
import io.github.lenticualrstone.civilight.domain.library.newproject.CreatedProject
import io.github.lenticualrstone.civilight.domain.library.newproject.NewProjectFlow
import io.github.lenticualrstone.civilight.domain.recorder.hooks.RecorderHooks
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.app.SceneNotationPref
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.FieldBundle
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.ProjectHome
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.WorldCalendar

/** Where `만들기` writes: the 서재 (a complete new project) and the recorder library (links). */
interface CreatorPorts {
    suspend fun addProject(doc: ProjectDoc)

    suspend fun updateRecorder(transform: (RecorderLibrary) -> RecorderLibrary)

    /**
     * Whether the device has a secure lock screen. Without one a hidden project could never be
     * opened again (§6 Q19 `ED`: hiding needs a device credential), so the project is made visible.
     */
    fun deviceSecure(): Boolean = true

    /** "지금은 열린 채" (R19 §2): a project created hidden stays open until the next re-lock. */
    fun keepOpen(projectKey: Key) = Unit
}

/** What happened on `만들기`, for the navigation that follows. */
data class CreateResult(
    val projectKey: Key,
    /** Created hidden: it joins the hidden group and stays open until the next re-lock (R19 §2). */
    val hidden: Boolean,
    /** `세계 달력 만들기` was picked: the calendar editor opens after creation (Spec conflicts). */
    val opensCalendar: Boolean,
    /** Hiding was asked for but the device has no secure lock screen: created visible (§6 Q19). */
    val hidingRefused: Boolean = false,
)

/**
 * `만들기` (LIB-12): the project document from `domain:library` [NewProjectFlow] (units, numbering,
 * time, protections from the rating, protagonist), the field set from `domain:cast` [FieldBundles]
 * (`기본` · `게임 프로필` · `비어 있음` · `다른 프로젝트에서`, definitions only), then the record links
 * through `domain:recorder` [RecorderHooks.linkRecords] (origin `시작 재료`, text not inserted).
 */
object ProjectCreator {
    /**
     * The complete document, nothing written yet. [bundleSource] = the `다른 프로젝트에서` project;
     * [notation] = 설정 › 지도 · 이미지 · 연표 › `때 표기 기본` (STO-17, LIB-68), the project's `때 표기`.
     */
    fun build(
        form: NewProjectForm,
        defaults: RatingDefaults,
        ctx: ActionContext,
        bundleSource: ProjectDoc?,
        notation: SceneNotationPref = SceneNotationPref.EPISODE,
    ): CreatedProject {
        val created = NewProjectFlow.create(NewProjectRules.toDraft(form), defaults, ctx.now, ctx.device, ctx.newKey)
        val withFields = when (form.bundle) {
            FieldBundle.FROM_PROJECT -> bundleSource?.let { FieldBundles.applyFromProject(created.doc, it, ctx.edit).doc } ?: created.doc
            else -> FieldBundles.apply(created.doc, form.bundle, ctx.edit).doc
        }
        return CreatedProject(withNotation(withFields, notation), created.entry, emptyList(), created.protections)
    }

    private fun withNotation(doc: ProjectDoc, notation: SceneNotationPref): ProjectDoc {
        val sceneTime = doc.project.sceneTime
        if (sceneTime.notation.known == notation.notation) return doc
        return doc.copy(project = doc.project.copy(sceneTime = sceneTime.copy(notation = notation.notation.open())))
    }

    suspend fun create(
        ports: CreatorPorts,
        form: NewProjectForm,
        defaults: RatingDefaults,
        ctx: ActionContext,
        bundleSource: ProjectDoc?,
        notation: SceneNotationPref = SceneNotationPref.EPISODE,
    ): CreateResult {
        val built = build(form, defaults, ctx, bundleSource, notation)
        val wantsHidden = built.doc.project.protections.hidden
        val refused = wantsHidden && !ports.deviceSecure()
        val doc = if (refused) built.doc.copy(project = built.doc.project.copy(protections = built.doc.project.protections.copy(hidden = false))) else built.doc
        val key = doc.key
        ports.addProject(doc)
        val hidden = doc.project.protections.hidden
        if (hidden) ports.keepOpen(key)
        val records = form.linkedRecordKeys.distinct()
        if (records.isNotEmpty()) ports.updateRecorder { RecorderHooks.linkRecords(it, records, key, ctx.recorder) }
        return CreateResult(key, hidden, form.timeMode == WorldTimeMode.CALENDAR, hidingRefused = refused)
    }

    /**
     * The back stack after `만들기`: 서재 › the new project's 프로젝트 홈 (its `만든 직후` state, board
     * `NewReady`: `‹ 서재 · 새 프로젝트`), plus the world calendar when `세계 달력 만들기` was picked.
     * Every `NewProject` step leaves the stack, so back from the home goes to 서재.
     */
    fun stackAfter(result: CreateResult): List<Route> =
        listOfNotNull(Library, ProjectHome(result.projectKey), if (result.opensCalendar) WorldCalendar(result.projectKey) else null)
}
