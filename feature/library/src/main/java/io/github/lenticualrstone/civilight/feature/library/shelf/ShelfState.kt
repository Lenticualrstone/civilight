package io.github.lenticualrstone.civilight.feature.library.shelf

import io.github.lenticualrstone.civilight.domain.library.common.TimeLabels
import io.github.lenticualrstone.civilight.domain.library.lock.Relock
import io.github.lenticualrstone.civilight.domain.library.masking.HiddenAccess
import io.github.lenticualrstone.civilight.domain.library.masking.HiddenGroupRow
import io.github.lenticualrstone.civilight.domain.library.masking.LibraryListing
import io.github.lenticualrstone.civilight.domain.library.masking.ProjectProfile
import io.github.lenticualrstone.civilight.domain.library.newproject.EmptyLibrary
import io.github.lenticualrstone.civilight.domain.library.newproject.Onboarding
import io.github.lenticualrstone.civilight.domain.library.newproject.RowMeta
import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.ProjectSnapshot
import io.github.lenticualrstone.civilight.domain.recorder.hooks.RecorderHooks
import io.github.lenticualrstone.civilight.domain.story.structure.SceneStatus
import io.github.lenticualrstone.civilight.domain.story.structure.StoryOverview
import io.github.lenticualrstone.civilight.feature.library.rating.RatingPickSheet
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.app.OnboardingState
import io.github.lenticualrstone.civilight.model.app.RelockPolicy
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.navigation.ProjectSheet
import io.github.lenticualrstone.civilight.navigation.RatingChangeSheet
import io.github.lenticualrstone.civilight.navigation.SheetKey
import java.time.ZoneId

/** A 서재 entry with its document; [doc] is null while it loads or when it cannot be read. */
data class ShelfProject(val entry: LibraryEntry, val doc: ProjectDoc?)

/** One project row (LIB-01): title, rating badge + meta, `쓰는 중`, ⋯. */
data class ProjectRowUi(
    val key: Key,
    val title: String,
    val rating: Rating,
    val meta: String,
    val writing: Boolean,
    val protections: Protections,
    val readOnly: Boolean,
)

/** The open hidden group (`LibUnlocked`): `숨긴 프로젝트 <M> · 열림`, the re-lock note, its rows. */
data class OpenGroupUi(val header: String, val note: String, val rows: List<ProjectRowUi>)

/** Everything the 서재 draws (LIB-01, LIB-31 … LIB-33 parts, LIB-70 `OnbEmpty`). */
data class ShelfUi(
    val loaded: Boolean = false,
    val sectionLabel: String = "",
    val rows: List<ProjectRowUi> = emptyList(),
    val lockedGroup: HiddenGroupRow? = null,
    val openGroup: OpenGroupUi? = null,
    /** Header right: `다시 잠그기` instead of `+` while the group is open (board `LibUnlocked`, §6 Q30). */
    val headerRelock: Boolean = false,
    val empty: EmptyLibrary? = null,
    val recorderSub: String = ShelfCopy.RECORDER_NO_RECORDS,
) {
    /**
     * Whether [projectKey]'s content may be drawn over the 서재 now: a visible row, or a row of the
     * open hidden group. A sheet or dialog restored after process death or left open across a
     * re-lock must not show a hidden project's name or counts (LIB-27, R19 §3).
     */
    fun shows(projectKey: Key): Boolean =
        rows.any { it.key == projectKey } || openGroup?.rows?.any { it.key == projectKey } == true
}

/** Builds [ShelfUi] from the stores' current values; pure, so the 서재 rules run on the JVM. */
object ShelfState {
    /** The project a 서재 sheet is about, for [ShelfUi.shows]; null for sheets about no project. */
    fun projectOf(sheet: SheetKey): Key? = when (sheet) {
        is ShelfMenuSheet -> sheet.projectKey
        is RenameSheet -> sheet.projectKey
        is RatingPickSheet -> sheet.projectKey
        // The shared `바뀌는 것` sheet guards itself (it closes once, after its own write).
        is RatingChangeSheet -> null
        is ProjectSheet -> sheet.projectKey
        else -> null
    }

    /** How many sheets to close from the top: those about a project the 서재 may not show now. */
    fun sheetsToClose(ui: ShelfUi, stack: List<SheetKey>): Int {
        if (!ui.loaded) return 0
        return stack.asReversed().takeWhile { sheet -> projectOf(sheet)?.let { !ui.shows(it) } ?: false }.size
    }

    fun build(
        projects: List<ShelfProject>,
        access: HiddenAccess,
        relockPolicy: RelockPolicy,
        recorder: RecorderLibrary,
        onboarding: OnboardingState,
        now: Timestamp,
        zone: ZoneId,
        loaded: Boolean = true,
    ): ShelfUi {
        val live = projects.filter { it.entry.deletedAt == null }.sortedByDescending { it.entry.updatedAt }
        val byKey = live.associateBy { it.entry.key }
        val listing = LibraryListing.of(live.map(::profileOf), access)
        fun row(profile: ProjectProfile) = rowOf(byKey.getValue(profile.key), now, zone)
        val openGroup = listing.openGroup.takeIf { it.isNotEmpty() }?.let { hidden ->
            OpenGroupUi(
                header = "${HiddenGroupRow(hidden.size).title} · ${ShelfCopy.GROUP_OPEN}",
                note = Relock.openGroupNote(relockPolicy),
                rows = hidden.map(::row),
            )
        }
        return ShelfUi(
            loaded = loaded,
            sectionLabel = listing.sectionLabel,
            rows = listing.rows.map(::row),
            lockedGroup = listing.lockedGroup,
            openGroup = openGroup,
            headerRelock = openGroup != null,
            empty = Onboarding.emptyLibrary(onboarding, live.map { it.entry }),
            recorderSub = recorderSub(recorder, live, access, zone),
        )
    }

    /** `기록 <n> · 정리 안 됨 <u>`, or `기록 0` with no records (board `OnbEmpty`). */
    fun recorderSub(recorder: RecorderLibrary, projects: List<ShelfProject>, access: HiddenAccess, zone: ZoneId): String {
        val snapshots = projects.mapNotNull { p -> p.doc?.let { ProjectSnapshot(p.entry, it) } }
        val counts = RecorderHooks.libraryCounts(recorder, RecorderEnv(snapshots, access.session, zone))
        return if (counts.records == 0) ShelfCopy.RECORDER_NO_RECORDS else counts.text
    }

    /**
     * `쓰는 중` (§6 Q12, `ED`): the project has an episode whose status is `쓰는 중` (written scenes,
     * not all marked done). Formats without an episode level (단편, 설정만) never show it.
     */
    fun isWriting(doc: ProjectDoc): Boolean {
        val overview = StoryOverview(doc)
        return overview.chapters.asSequence()
            .flatMap { overview.episodesOf(it).asSequence() }
            .any { overview.episodeStatus(it).startsWith(SceneStatus.WRITING.word) }
    }

    private fun profileOf(p: ShelfProject): ProjectProfile =
        p.doc?.let { ProjectProfile.of(it, trashed = false, isExample = p.entry.isExample) }
            ?: ProjectProfile(
                key = p.entry.key,
                name = p.entry.name,
                rating = RatingLabels.effective(p.entry.rating),
                protections = Protections(hidden = p.entry.hidden),
                isExample = p.entry.isExample,
            )

    private fun rowOf(p: ShelfProject, now: Timestamp, zone: ZoneId): ProjectRowUi {
        val doc = p.doc
        return ProjectRowUi(
            key = p.entry.key,
            title = doc?.project?.name ?: p.entry.name,
            rating = RatingLabels.effective(doc?.project?.rating ?: p.entry.rating),
            meta = doc?.let { RowMeta.of(it, p.entry.updatedAt, now, zone) } ?: TimeLabels.relative(p.entry.updatedAt, now, zone),
            writing = doc?.let(::isWriting) ?: false,
            protections = doc?.project?.protections ?: Protections(hidden = p.entry.hidden),
            readOnly = p.entry.readOnly || p.entry.isExample,
        )
    }
}
