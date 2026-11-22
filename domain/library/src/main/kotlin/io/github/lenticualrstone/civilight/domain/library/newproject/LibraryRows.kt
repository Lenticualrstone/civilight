package io.github.lenticualrstone.civilight.domain.library.newproject

import io.github.lenticualrstone.civilight.domain.library.common.TimeLabels
import io.github.lenticualrstone.civilight.domain.library.common.UnitCodes
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.ExampleState
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.app.OnboardingState
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import java.time.ZoneId

/** The meta line of a 서재 row (LIB-01). */
object RowMeta {
    /**
     * 연재 장편 `<n>장 <e>/<E> · 인물 <c> · <time>`; 단편 `단편 · 씬 <s>개 · <time>`; 설정만
     * `설정만 · 인물 <c> · <time>`. `<n>` = chapter of the furthest-written episode (the episode
     * holding the 집필 선), `<e>` = its number in the chapter, `<E>` = live episodes of the chapter.
     */
    fun of(doc: ProjectDoc, modifiedAt: Timestamp, now: Timestamp, zone: ZoneId): String {
        val time = TimeLabels.relative(modifiedAt, now, zone)
        val characters = doc.cast.characters.count { it.deletedAt == null }
        return when (doc.project.format.known) {
            ProjectFormat.SHORT -> "단편 · 씬 ${doc.manuscript.liveScenes.size}개 · $time"
            ProjectFormat.SETTING_ONLY -> "설정만 · 인물 $characters · $time"
            else -> listOfNotNull(position(doc), "인물 $characters", time).joinToString(" · ")
        }
    }

    /** `1장 7/10`, or null without a written scene or without an episode level. */
    fun position(doc: ProjectDoc): String? {
        val structure = doc.project.structure
        val episodeLevel = structure.episodeLevel ?: return null
        val sceneKey = doc.manuscript.writingLineSceneKey ?: return null
        val scene = doc.index.unit(sceneKey) ?: return null
        val episode = doc.index.ancestors(scene).firstOrNull { it.level == episodeLevel } ?: return null
        val top = doc.index.ancestors(scene).firstOrNull { it.level == 0 } ?: episode
        val codes = UnitCodes(doc)
        val chapterNumber = codes.ordinal(top) ?: return null
        val siblings = doc.index.children(episode.parentKey).filter { doc.liveness.isLive(it) }
        val e = codes.ordinal(episode) ?: return null
        return "$chapterNumber${structure.levels.first()} $e/${siblings.size}"
    }
}

/** `온보딩 끝내기` (LIB-18). */
data class OnboardingFinish(
    val state: OnboardingState,
    /** The library without the example (permanently deleted: no trash entry). */
    val library: List<LibraryEntry>,
    /** The recorder library without links into the example (LRC-23). */
    val recorder: RecorderLibrary,
    val purgedExample: LibraryEntry?,
)

/** The empty 서재 (LIB-18, LIB-70 `OnbEmpty`). */
data class EmptyLibrary(val showDeletedNotice: Boolean) {
    val section: String get() = "프로젝트 · 0"
    val title: String get() = "프로젝트 없음"
    val sub: String get() = "프로젝트 = 원고 · 인물 · 세계 · 페이지 한 묶음"
    val buttons: List<String> get() = listOf("새 프로젝트", "백업에서 되살리기")
    val notice: String? get() = if (showDeletedNotice) "예시 프로젝트 삭제됨" else null
}

object Onboarding {
    fun finish(state: OnboardingState, library: List<LibraryEntry>, recorder: RecorderLibrary): OnboardingFinish {
        val example = library.firstOrNull { it.isExample }
        val keptRecorder = if (example == null) recorder else recorder.copy(
            links = recorder.links.filterNot { it.projectKey == example.key },
            placements = recorder.placements.filterNot { it.projectKey == example.key },
        )
        return OnboardingFinish(
            state = state.copy(firstRunCompleted = true, exampleState = ExampleState.DELETED, resumePage = null),
            library = library.filterNot { it.isExample },
            recorder = keptRecorder,
            purgedExample = example,
        )
    }

    /** The empty 서재 when no project is left; `예시 프로젝트 삭제됨` until the first project exists (`ED`). */
    fun emptyLibrary(state: OnboardingState, library: List<LibraryEntry>): EmptyLibrary? {
        if (library.any { it.deletedAt == null && !it.isExample }) return null
        return EmptyLibrary(showDeletedNotice = state.exampleState == ExampleState.DELETED && !state.emptyLibraryNoticeShown)
    }
}

/** Read-only enforcement (LIB-17, P-08): every write to a read-only project is rejected. */
object ReadOnly {
    sealed interface Write<out T> {
        data class Done<T>(val value: T) : Write<T>

        data object Rejected : Write<Nothing>
    }

    fun canWrite(entry: LibraryEntry): Boolean = !entry.readOnly && !entry.isExample

    /** Runs [edit] only for a writable project. */
    fun <T> guard(entry: LibraryEntry, edit: () -> T): Write<T> = if (canWrite(entry)) Write.Done(edit()) else Write.Rejected
}
