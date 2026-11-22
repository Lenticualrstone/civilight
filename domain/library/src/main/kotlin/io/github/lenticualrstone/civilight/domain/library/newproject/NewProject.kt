package io.github.lenticualrstone.civilight.domain.library.newproject

import io.github.lenticualrstone.civilight.domain.library.masking.HiddenAccess
import io.github.lenticualrstone.civilight.domain.library.masking.ProjectProfile
import io.github.lenticualrstone.civilight.domain.library.rating.NewProjectProtections
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.EpisodeFile
import io.github.lenticualrstone.civilight.model.file.StoryIndexFile
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.FieldBundle
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.project.ProjectCounters
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.project.StoryStructure
import io.github.lenticualrstone.civilight.model.project.StructurePreset
import io.github.lenticualrstone.civilight.model.project.TravelMode
import io.github.lenticualrstone.civilight.model.project.WorldTime
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/** The four steps' values (LIB-09…12); nothing exists until `만들기`. */
data class NewProjectDraft(
    val name: String = "",
    val oneLiner: String? = null,
    val format: ProjectFormat = ProjectFormat.SERIAL,
    /** Step-1 default `ALL` (`ED`, §6 Q11). */
    val rating: Rating = Rating.ALL,
    val structurePreset: StructurePreset = StructurePreset.CHAPTER_EPISODE_SCENE,
    /** Level names for `직접 정하기` (1–4). */
    val customLevels: List<String> = emptyList(),
    val timeMode: WorldTimeMode = WorldTimeMode.YEAR_ONLY,
    val startYear: Int? = null,
    val protagonist: String = "",
    val firstSceneTitle: String = "",
    /** 기록함 records checked under `라이프 레코더 기록 연결`. */
    val linkedRecordKeys: List<Key> = emptyList(),
    /** Always `기본` until the author picks another (never guessed from 형식, P-04). */
    val bundle: FieldBundle = FieldBundle.BASIC,
    /** Step-4 `이 프로젝트 숨기기` as left by the author; null = untouched (initial from the rating). */
    val step4Hide: Boolean? = null,
) {
    val levels: List<String>
        get() = when (structurePreset) {
            StructurePreset.CHAPTER_EPISODE_SCENE -> StoryStructure.DEFAULT_LEVELS
            StructurePreset.ACT_SCENE -> StoryStructure.ACT_SCENE_LEVELS
            StructurePreset.CUSTOM -> customLevels.map { it.trim().nfc() }
        }
}

/** Result of `만들기` (LIB-12). */
class CreatedProject(
    val doc: ProjectDoc,
    val entry: LibraryEntry,
    /** `연결` rows (origin `startMaterial`) of the checked records; their text is not inserted anywhere. */
    val recorderLinks: List<RecorderLink>,
    val protections: NewProjectProtections,
) {
    /** A project created hidden stays open for this session until the next re-lock. */
    val keepOpenUntilRelock: Boolean get() = protections.protections.hidden
}

/** The new-project flow (LIB-09…13). */
object NewProjectFlow {
    const val MAX_LEVELS = 4

    /** Step 1 `다음`: enabled when the name is not blank after trimming (P-06). */
    fun canProceedFromName(name: String): Boolean = name.isNotBlank()

    /** Step 2 with `직접 정하기`: 1–4 non-blank level names. */
    fun levelsValid(levels: List<String>): Boolean = levels.size in 1..MAX_LEVELS && levels.all { it.isNotBlank() }

    /** Step-2 `미리 보기`: 3 levels `장 1` › `에피소드 1-1` › `씬 1`, `씬 2`, `에피소드 1-2`; 2 levels `<L1> 1` › `<L2> 1`, `<L2> 2`. */
    fun previewTree(levels: List<String>): List<Pair<Int, String>> = when (levels.size) {
        0 -> emptyList()
        1 -> listOf(0 to "${levels[0]} 1", 0 to "${levels[0]} 2")
        2 -> listOf(0 to "${levels[0]} 1", 1 to "${levels[1]} 1", 1 to "${levels[1]} 2")
        else -> {
            val top = levels.size - 1
            buildList {
                add(0 to "${levels[0]} 1")
                for (l in 1 until top) add(l to "${levels[l]} ${List(l + 1) { 1 }.joinToString("-")}")
                add(top to "${levels[top]} 1")
                add(top to "${levels[top]} 2")
                add(top - 1 to "${levels[top - 1]} ${List(top - 1) { 1 }.joinToString("-")}-2")
            }
        }
    }

    /** Step-4 record list: 기록함 records in state 정리 안 됨, newest first (`ED` filter). */
    fun startRecordCandidates(recorder: RecorderLibrary): List<RecorderRecord> =
        recorder.records.filter { RecordStates.isUnsorted(recorder, it) }.sortedByDescending { it.createdAt }

    /** `다른 프로젝트에서` sources: other projects; hidden ones only while the group is open (P-05). */
    fun bundleSources(projects: List<ProjectProfile>, access: HiddenAccess): List<ProjectProfile> =
        projects.filter { !it.trashed && !it.isExample && (!it.hidden || access.groupOpen) }

    /** Bundle labels (LIB-12). */
    fun bundleLabel(bundle: FieldBundle): String = when (bundle) {
        FieldBundle.BASIC -> "기본"
        FieldBundle.GAME_PROFILE -> "게임 프로필"
        FieldBundle.EMPTY -> "비어 있음"
        FieldBundle.FROM_PROJECT -> "다른 프로젝트에서"
    }

    /**
     * `만들기`: keys, structure with the first unit of every level when the format has a manuscript
     * (설정만 creates no scene, `ED`), time mode, field bundle ([fieldDefsFor] is the cast area's bundle
     * definition), travel modes, protections from the rating, the protagonist and the record links.
     * The first scene's title is the typed one, or none (shown as `씬 1`, a derived label).
     */
    fun create(
        draft: NewProjectDraft,
        defaults: RatingDefaults,
        now: Timestamp,
        device: String,
        newKey: () -> Key,
        fieldDefsFor: (FieldBundle) -> List<FieldDef> = { emptyList() },
    ): CreatedProject {
        require(canProceedFromName(draft.name)) { "name required" }
        val levels = draft.levels
        require(levelsValid(levels)) { "1–4 level names required" }
        val projectKey = newKey()
        val protections = NewProjectProtections.of(draft.rating, defaults, draft.step4Hide)
        val structure = StoryStructure(preset = draft.structurePreset.open(), levels = levels)

        val units = ArrayList<StoryUnit>()
        val episodes = LinkedHashMap<Key, EpisodeFile>()
        if (draft.format != ProjectFormat.SETTING_ONLY) {
            var parent: Key? = null
            for (level in levels.indices) {
                val key = newKey()
                val isScene = level == structure.sceneLevel
                val isNumbering = level == structure.episodeLevel
                units += StoryUnit(
                    key = key, parentKey = parent, level = level, order = OrderKeys.between(null, null),
                    title = if (isScene) draft.firstSceneTitle.trim().nfc().ifEmpty { null } else null,
                    nextBlockNumber = if (isNumbering) 1 else null,
                    updatedAt = now, updatedBy = device,
                )
                if (isNumbering) episodes[key] = EpisodeFile(unitKey = key)
                parent = key
            }
            if (structure.episodeLevel == null) episodes[projectKey] = EpisodeFile(unitKey = projectKey)
        }

        val protagonist = draft.protagonist.trim().nfc().takeIf { it.isNotEmpty() }?.let {
            Character(key = newKey(), recordNo = 1, name = it, order = OrderKeys.between(null, null), updatedAt = now, updatedBy = device)
        }
        val project = Project(
            key = projectKey,
            name = draft.name.trim().nfc(),
            oneLiner = draft.oneLiner?.trim()?.nfc()?.ifEmpty { null },
            format = draft.format.open(),
            rating = draft.rating.open(),
            protections = protections.protections,
            structure = structure,
            worldTime = WorldTime(
                mode = draft.timeMode.open(),
                storyStartYear = if (draft.timeMode == WorldTimeMode.NONE) null else draft.startYear,
            ),
            travelModes = TravelMode.defaults(newKey, now, device),
            fieldBundleAtCreation = draft.bundle.open(),
            counters = ProjectCounters(
                nextCharacterRecordNo = if (protagonist != null) 2 else 1,
                nextBlockNumber = if (structure.episodeLevel == null && draft.format != ProjectFormat.SETTING_ONLY) 1 else null,
            ),
            fieldDefs = fieldDefsFor(draft.bundle),
            createdAt = now,
            updatedAt = now,
            updatedBy = device,
        )
        val doc = ProjectDoc(
            project = project,
            story = StoryIndexFile(units = units),
            episodes = episodes,
            cast = io.github.lenticualrstone.civilight.model.file.CastFile(characters = listOfNotNull(protagonist)),
        )
        val links = draft.linkedRecordKeys.distinct().map { recordKey ->
            RecorderLink(
                key = newKey(), recordKey = recordKey, projectKey = projectKey, targetKind = RecorderTargetKind.PROJECT.open(),
                targetKey = projectKey, origin = LinkOrigin.START_MATERIAL.open(), createdAt = now, updatedAt = now, updatedBy = device,
            )
        }
        val entry = LibraryEntry(
            key = projectKey, name = project.name, rating = project.rating, hidden = project.protections.hidden,
            createdAt = now, updatedAt = now, lastOpenedAt = now,
        )
        return CreatedProject(doc, entry, links, protections)
    }
}

/** Recorder record states (derived, LRC §3.3): 정리 안 됨 · 넣음 · 끝냄. */
object RecordStates {
    fun isDone(record: RecorderRecord): Boolean = record.doneMarked

    fun isPlaced(recorder: RecorderLibrary, record: RecorderRecord): Boolean = recorder.placementsOf(record.key).any { it.purgedAt == null }

    fun isUnsorted(recorder: RecorderLibrary, record: RecorderRecord): Boolean =
        record.deletedAt == null && !isDone(record) && !isPlaced(recorder, record)
}
