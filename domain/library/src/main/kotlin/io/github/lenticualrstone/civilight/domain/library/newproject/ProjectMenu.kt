package io.github.lenticualrstone.civilight.domain.library.newproject

import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc

/** What the ⋯ sheet and the delete dialog count (live rows only). */
data class ProjectCounts(val scenes: Int, val characters: Int, val pages: Int) {
    companion object {
        fun of(doc: ProjectDoc) = ProjectCounts(
            scenes = doc.manuscript.liveScenes.size,
            characters = doc.cast.characters.count { it.deletedAt == null },
            pages = doc.pages.values.count { it.deletedAt == null },
        )
    }
}

/** The actions of the project ⋯ sheet (LIB-02); the rows' order is [ProjectMenus.of]'s. [COVER]: `표지 이미지` (U-08 LIB-81). */
enum class ProjectMenuAction { OPEN, RENAME, RATING, HIDE, UNHIDE, BACKUP_EXPORT, DELETE, COVER }

data class ProjectMenuRow(val action: ProjectMenuAction, val title: String, val sub: String?)

/**
 * The `LibMenu` sheet (LIB-02). (0.2.0, LIB-92) [ratingBadge] is still filled from the stored value but
 * no surface draws it.
 */
data class ProjectMenu(val title: String, val ratingBadge: String, val sub: String, val rows: List<ProjectMenuRow>) {
    val close: String get() = "닫기"
}

object ProjectMenus {
    /**
     * Header sub `<format> · 씬 <s>개 · 인물 <c> · 페이지 <p>`. The example project shows only `열기`
     * (`ED`, read-only); a hidden project (reachable only while the group is open) offers `숨김 풀기`
     * instead of `프로젝트 숨기기`. `표지 이미지` follows `이름 바꾸기` with `지금 <source word>` or
     * `없음 · 첫 글자 카드` (U-08 LIB-81); [fileExists] says whether a cover original is on this device.
     * (0.2.0, LIB-92) Rows: `열기 · 이름 바꾸기 · 표지 이미지 · 프로젝트 숨기기 (숨김 풀기) · 백업 · 내보내기 · 지우기`.
     */
    fun of(doc: ProjectDoc, isExample: Boolean = false, fileExists: (String) -> Boolean = { true }): ProjectMenu {
        val p = doc.project
        val c = ProjectCounts.of(doc)
        val rows = buildList {
            add(ProjectMenuRow(ProjectMenuAction.OPEN, "열기", "마지막 위치"))
            if (!isExample) {
                add(ProjectMenuRow(ProjectMenuAction.RENAME, "이름 바꾸기", null))
                add(ProjectMenuRow(ProjectMenuAction.COVER, CoverSummary.TITLE, CoverSummary.menuSub(doc, fileExists)))
                // (0.2.0, LIB-92, S-4) No `수위 등급` row: the rating is kept in the project, never shown or set.
                if (p.protections.hidden) {
                    add(ProjectMenuRow(ProjectMenuAction.UNHIDE, "숨김 풀기", null))
                } else {
                    add(ProjectMenuRow(ProjectMenuAction.HIDE, "프로젝트 숨기기", "프로젝트 전체 · 지문 · PIN으로 열림"))
                }
                add(ProjectMenuRow(ProjectMenuAction.BACKUP_EXPORT, "백업 · 내보내기", "Google Drive · 파일"))
                add(ProjectMenuRow(ProjectMenuAction.DELETE, "지우기", "휴지통 · 30일 뒤 삭제"))
            }
        }
        return ProjectMenu(
            title = p.name,
            ratingBadge = RatingLabels.badge(p.rating),
            sub = "${StartBlock.formatLabel(p.format.known)} · 씬 ${c.scenes}개 · 인물 ${c.characters} · 페이지 ${c.pages}",
            rows = rows,
        )
    }

    /** `이름 바꾸기`: trimmed and NFC-normalised; null (field error) when blank (`ED`). */
    fun rename(input: String): String? = input.trim().nfc().ifEmpty { null }
}

/** The `ConfirmDelete` dialog (LIB-03): the name-typing confirm used only for whole projects (R20 §4). */
data class ProjectDeleteDialog(val name: String, val counts: ProjectCounts) {
    val title: String get() = "‘$name’ 지우기"
    val rows: List<Pair<String, String>>
        get() = listOf("씬 · 인물 · 페이지" to "${counts.scenes} · ${counts.characters} · ${counts.pages}", "이동" to "휴지통 · 30일 보관")
    val fieldLabel: String get() = "확인: 프로젝트 이름 입력"
    val buttons: List<String> get() = listOf("취소", "휴지통으로")

    /**
     * `휴지통으로` is enabled only when the typed text equals the name after NFC normalisation;
     * leading and trailing whitespace is trimmed, inner spaces must match (`ED`).
     */
    fun canConfirm(typed: String): Boolean = typed.trim().nfc() == name.nfc()

    companion object {
        fun of(doc: ProjectDoc) = ProjectDeleteDialog(doc.project.name, ProjectCounts.of(doc))
    }
}
