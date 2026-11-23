package io.github.lenticualrstone.civilight.feature.library.newproject

import io.github.lenticualrstone.civilight.domain.library.newproject.NewProjectDraft
import io.github.lenticualrstone.civilight.domain.library.newproject.NewProjectFlow
import io.github.lenticualrstone.civilight.domain.library.rating.NewProjectProtections
import io.github.lenticualrstone.civilight.domain.library.rating.R18DefaultsBlock
import io.github.lenticualrstone.civilight.domain.cast.field.FieldBundles
import io.github.lenticualrstone.civilight.domain.cast.field.FieldTemplates
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.FieldTemplate
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.project.FieldBundle
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.project.StoryStructure
import io.github.lenticualrstone.civilight.model.project.StructurePreset
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.navigation.NewProjectStep
import kotlinx.serialization.Serializable

/**
 * What the author typed and picked in the four steps (LIB-09 … LIB-12). Saved as JSON with each step
 * entry so it survives process death; nothing is created until `만들기`.
 */
@Serializable
data class NewProjectForm(
    val name: String = "",
    val oneLiner: String = "",
    val format: ProjectFormat = ProjectFormat.SERIAL,
    /** Step-1 default `ALL` (§6 Q11 `ED`). */
    val rating: Rating = Rating.ALL,
    val structurePreset: StructurePreset = StructurePreset.CHAPTER_EPISODE_SCENE,
    /** `직접 정하기` level names; seeded from the default levels the first time it is picked. */
    val customLevels: List<String> = emptyList(),
    val timeMode: WorldTimeMode = WorldTimeMode.YEAR_ONLY,
    val startYear: String = "",
    val protagonist: String = "",
    val firstSceneTitle: String = "",
    val linkedRecordKeys: List<Key> = emptyList(),
    val bundle: FieldBundle = FieldBundle.BASIC,
    /** The project whose field set `다른 프로젝트에서` copies. */
    val bundleSourceKey: Key? = null,
    /** The 인물 템플릿 picked at step 4 when [bundle] is `TEMPLATE` (CST-61); the key only, never its name. */
    val bundleTemplateKey: Key? = null,
    /** Step-4 switch as left by the author; null = untouched (follows the rating's default). */
    val step4Hide: Boolean? = null,
    /** True once the author changed the step-2 structure by hand; until then it follows the 형식 ([NewProjectRules.withFormat]). */
    val structureTouched: Boolean = false,
)

/** The rules of the four steps, pure (state in, state out). */
object NewProjectRules {
    const val STEPS = 4

    fun index(step: NewProjectStep): Int = step.ordinal

    /** `새 프로젝트 · k/4`. */
    fun smallLine(step: NewProjectStep): String = "${NewProjectCopy.SMALL} · ${step.ordinal + 1}/$STEPS"

    fun title(step: NewProjectStep): String = when (step) {
        NewProjectStep.NAME -> NewProjectCopy.TITLE_NAME
        NewProjectStep.STRUCTURE -> NewProjectCopy.TITLE_STRUCTURE
        NewProjectStep.TIME -> NewProjectCopy.TITLE_TIME
        NewProjectStep.START -> NewProjectCopy.TITLE_START
    }

    fun next(step: NewProjectStep): NewProjectStep? = NewProjectStep.entries.getOrNull(step.ordinal + 1)

    fun previous(step: NewProjectStep): NewProjectStep? = NewProjectStep.entries.getOrNull(step.ordinal - 1)

    /** Level names the form stands for now. */
    fun levels(form: NewProjectForm): List<String> = toDraft(form).levels

    /** `다음` / `만들기` enabled: a name (P-06); with `직접 정하기`, 1–4 named levels. */
    fun canProceed(form: NewProjectForm, step: NewProjectStep): Boolean = when (step) {
        NewProjectStep.NAME -> NewProjectFlow.canProceedFromName(form.name)
        NewProjectStep.STRUCTURE -> structureValid(form)
        NewProjectStep.TIME, NewProjectStep.START ->
            NewProjectFlow.canProceedFromName(form.name) && structureValid(form)
    }

    private fun structureValid(form: NewProjectForm): Boolean =
        form.structurePreset != StructurePreset.CUSTOM || NewProjectFlow.levelsValid(form.customLevels)

    /**
     * Step-1 `형식`: while the structure is untouched it moves to the new format's default
     * ([NewProjectFlow.defaultStructure]: 단편 → one level `씬`); a structure picked by hand is kept.
     */
    fun withFormat(form: NewProjectForm, format: ProjectFormat): NewProjectForm {
        if (form.structureTouched) return form.copy(format = format)
        val (preset, levels) = NewProjectFlow.defaultStructure(format)
        return form.copy(
            format = format,
            structurePreset = preset,
            customLevels = if (preset == StructurePreset.CUSTOM) levels else emptyList(),
        )
    }

    fun withPreset(form: NewProjectForm, preset: StructurePreset): NewProjectForm = form.copy(
        structurePreset = preset,
        customLevels = if (preset == StructurePreset.CUSTOM && form.customLevels.isEmpty()) StoryStructure.DEFAULT_LEVELS else form.customLevels,
        // Re-tapping the card already selected is not a change: the structure keeps following the 형식.
        structureTouched = form.structureTouched || preset != form.structurePreset,
    )

    fun setLevel(form: NewProjectForm, index: Int, name: String): NewProjectForm =
        form.copy(customLevels = form.customLevels.mapIndexed { i, old -> if (i == index) name else old }, structureTouched = true)

    fun canAddLevel(form: NewProjectForm): Boolean = form.customLevels.size < NewProjectFlow.MAX_LEVELS

    fun canRemoveLevel(form: NewProjectForm): Boolean = form.customLevels.size > 1

    /** A new level goes below the others (`ED`), empty until named. */
    fun addLevel(form: NewProjectForm): NewProjectForm =
        if (canAddLevel(form)) form.copy(customLevels = form.customLevels + "", structureTouched = true) else form

    fun removeLevel(form: NewProjectForm, index: Int): NewProjectForm =
        if (canRemoveLevel(form)) form.copy(customLevels = form.customLevels.filterIndexed { i, _ -> i != index }, structureTouched = true) else form

    /** Digits only, at most 6 (`이야기가 시작되는 해`, an integer). */
    fun withYear(form: NewProjectForm, text: String): NewProjectForm =
        form.copy(startYear = text.filter(Char::isDigit).take(6))

    fun toggleRecord(form: NewProjectForm, recordKey: Key): NewProjectForm = form.copy(
        linkedRecordKeys = if (recordKey in form.linkedRecordKeys) form.linkedRecordKeys - recordKey else form.linkedRecordKeys + recordKey,
    )

    /** The step-4 switch: the author's choice, else the rating's hide default (R19-11 … 13). */
    fun hideSwitch(form: NewProjectForm, defaults: RatingDefaults): Boolean =
        form.step4Hide ?: NewProjectProtections.of(form.rating, defaults, null).step4ToggleInitial

    /** The tinted block under the R-18 cell (LIB-09), only while R-18 is picked. */
    fun r18Block(form: NewProjectForm, defaults: RatingDefaults, recoverySet: Boolean): R18DefaultsBlock? =
        if (form.rating == Rating.R18) R18DefaultsBlock.of(defaults, recoverySet) else null

    /**
     * What makes the step-2 `미리 보기` swap (crossfade + height) instead of updating in place: the picked
     * structure and the tree's shape (depths). Typing a level name keeps the key, so the text just changes.
     */
    fun previewSwapKey(form: NewProjectForm): Any = previewSwapKey(form.structurePreset, preview(form))

    /** [previewSwapKey] from a preset (null when unknown) and the preview lines. */
    fun previewSwapKey(preset: StructurePreset?, lines: List<Pair<Int, String>>): Any = preset to lines.map { it.first }

    /** Step-2 `미리 보기` lines (depth, text); empty while a custom level is unnamed. */
    fun preview(form: NewProjectForm): List<Pair<Int, String>> {
        val levels = levels(form)
        return if (NewProjectFlow.levelsValid(levels)) NewProjectFlow.previewTree(levels) else emptyList()
    }

    /**
     * The step-4 `인물 항목` row (CST-61, TC-F-39, TC-F-42): a built-in bundle's row, or for a template
     * `인물 항목 | 인물 10 · 장소 1 | 내 기본 ›`; a hidden template while the group is locked reads
     * `인물 항목 | 숨긴 템플릿 ›` with no counts; a template that is gone reads as `기본`.
     */
    fun bundleRow(form: NewProjectForm, templates: List<FieldTemplate>, hiddenUnlocked: Boolean): Triple<String, String, String> {
        if (form.bundle != FieldBundle.TEMPLATE) return FieldBundles.newProjectRow(form.bundle)
        val t = templates.firstOrNull { it.key == form.bundleTemplateKey && it.deletedAt == null }
            ?: return FieldBundles.newProjectRow(FieldBundles.DEFAULT)
        if (t.hidden && !hiddenUnlocked) return Triple(FieldBundles.ROW_TITLE, "", "${FieldTemplates.HIDDEN_NAME} ›")
        return Triple(FieldBundles.ROW_TITLE, FieldTemplates.kindCounts(t), "${t.name} ›")
    }

    /** The domain draft for `만들기`; record links are made by the recorder path, so none here. */
    fun toDraft(form: NewProjectForm): NewProjectDraft = NewProjectDraft(
        name = form.name,
        oneLiner = form.oneLiner.ifBlank { null },
        format = form.format,
        rating = form.rating,
        structurePreset = form.structurePreset,
        customLevels = form.customLevels,
        timeMode = form.timeMode,
        startYear = if (form.timeMode == WorldTimeMode.NONE) null else form.startYear.toIntOrNull(),
        protagonist = form.protagonist,
        firstSceneTitle = form.firstSceneTitle,
        linkedRecordKeys = emptyList(),
        bundle = form.bundle,
        step4Hide = form.step4Hide,
    )
}
