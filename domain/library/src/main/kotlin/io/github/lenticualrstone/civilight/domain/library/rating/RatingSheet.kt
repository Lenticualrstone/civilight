package io.github.lenticualrstone.civilight.domain.library.rating

import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.project.Rating

enum class RatingDirection { RAISE, LOWER }

/** One checkbox row of the `바뀌는 것` sheet. [rightLabel] is the grey target-default label (lowering only). */
data class RatingSheetRow(val protection: Protection, val title: String, val sub: String, val rightLabel: String?)

enum class RatingSheetExtra { NO_RECOVERY_PASSWORD, UNENCRYPTED_BACKUPS }

/** A ◇ row shown only while its condition holds. */
data class RatingSheetExtraRow(val kind: RatingSheetExtra, val title: String, val sub: String, val action: String)

/**
 * The rating-change `바뀌는 것` sheet (LIB-24, R19 §2), built fresh every time from the current
 * values: nothing is applied silently and nothing is released automatically. Raising pre-checks
 * `current ∨ target default` (§6 Q7); lowering pre-checks the current values, so a protection goes
 * off only when the author unchecks it.
 */
class RatingSheet internal constructor(
    val projectName: String,
    val from: Rating,
    val to: Rating,
    val current: Protections,
    val rows: List<RatingSheetRow>,
    val initialChecks: Set<Protection>,
    private val recoverySet: Boolean,
    private val unencryptedBackups: Int,
) {
    val direction: RatingDirection = if (to > from) RatingDirection.RAISE else RatingDirection.LOWER

    /** `<project name> · 수위 등급`. */
    val header: String get() = "$projectName · 수위 등급"

    /** `R-15 → R-18`, followed by [titleSuffix]. */
    val title: String get() = "${RatingLabels.badge(from)} → ${RatingLabels.badge(to)}"
    val titleSuffix: String get() = "바뀌는 것"

    val smallLine: String
        get() = when (direction) {
            RatingDirection.RAISE -> "체크 · ${RatingLabels.badge(to)} 기본값 · 설정 › 수위 등급"
            RatingDirection.LOWER -> "체크 · 지금 켜진 보호 · 체크를 풀어야 꺼짐"
        }

    val buttons: List<String> get() = listOf("취소", "바꾸기")

    /** ◇ rows for the current checks. */
    fun extraRows(checked: Set<Protection> = initialChecks): List<RatingSheetExtraRow> = buildList {
        val hides = Protection.HIDDEN in checked
        if (hides && !recoverySet) {
            add(RatingSheetExtraRow(RatingSheetExtra.NO_RECOVERY_PASSWORD, "복구 암호 없음", "숨기면 자동 백업에서 빠짐", "정하기 ›"))
        }
        if (hides && unencryptedBackups > 0) {
            add(
                RatingSheetExtraRow(
                    RatingSheetExtra.UNENCRYPTED_BACKUPS, "이전 백업 $unencryptedBackups · 암호 없음", "30일 백업 목록에 그대로", "지우기 ›",
                ),
            )
        }
    }

    /**
     * Note line: raising with `프로젝트 숨기기` newly checked → the group note; lowering → `내려도 저절로
     * 풀리는 것 없음`; otherwise none (`ED`).
     */
    fun note(checked: Set<Protection> = initialChecks): String? = when {
        direction == RatingDirection.LOWER -> "내려도 저절로 풀리는 것 없음"
        Protection.HIDDEN in checked && !current.hidden -> "바꾸면 ‘숨긴 프로젝트’ 묶음으로 · 지금은 열린 채"
        else -> null
    }

    /** `바꾸기` with [checked]: the change to confirm (authentication first when hiding goes off). */
    fun apply(checked: Set<Protection> = initialChecks): RatingChange =
        RatingChange(from, to, current, current.withExactly(checked))
}

/** The result of `바꾸기` before authentication (LIB-24 apply). */
data class RatingChange(val from: Rating, val to: Rating, val before: Protections, val after: Protections) {
    /** `프로젝트 숨기기` on → off needs a fresh authentication first (LIB-32 prompt). */
    val requiresAuth: Boolean get() = before.hidden && !after.hidden

    /** off → on: the project joins the hidden group but stays open until the next re-lock (LIB-33). */
    val joinsHiddenGroup: Boolean get() = !before.hidden && after.hidden

    /**
     * Applies the change. With [requiresAuth], [authenticated] false (cancel or failure) changes
     * nothing and the sheet stays open.
     */
    fun complete(authenticated: Boolean = false): RatingChangeOutcome =
        if (requiresAuth && !authenticated) RatingChangeOutcome.Unchanged(from, before)
        else RatingChangeOutcome.Applied(to, after, keepOpenUntilRelock = joinsHiddenGroup)
}

sealed interface RatingChangeOutcome {
    data class Applied(val rating: Rating, val protections: Protections, val keepOpenUntilRelock: Boolean) : RatingChangeOutcome

    data class Unchanged(val rating: Rating, val protections: Protections) : RatingChangeOutcome
}

object RatingSheets {
    /**
     * The sheet for changing [from] → [to], or null when the rating does not change (no sheet, no
     * change). [linkedRecords] = recorder records linked to the project (`기록 <n> 바로 적용`);
     * [unencryptedBackups] = unencrypted Drive versions of this project.
     */
    fun build(
        projectName: String,
        from: Rating,
        to: Rating,
        current: Protections,
        defaults: RatingDefaults,
        recoverySet: Boolean,
        unencryptedBackups: Int,
        linkedRecords: Int,
    ): RatingSheet? {
        if (from == to) return null
        val raising = to > from
        val target = RatingDefaultsTable.defaultsFor(to, defaults)
        val checks = if (raising) current.onSet + target.onSet else current.onSet
        val rows = Protection.entries.map { p ->
            RatingSheetRow(
                protection = p,
                title = title(p),
                sub = if (raising) raiseSub(p, linkedRecords) else lowerSub(p),
                rightLabel = if (raising) null else defaultLabel(to, target.isOn(p)),
            )
        }
        return RatingSheet(projectName, from, to, current, rows, checks, recoverySet, unencryptedBackups)
    }

    private fun title(p: Protection) = when (p) {
        Protection.HIDDEN -> "프로젝트 숨기기"
        Protection.CAPTURE_BLOCK -> "화면 캡처 가리기"
        Protection.NAME_MASK -> "이름 가리기"
        Protection.RECORDER_LINK_MASK -> "레코더 연결 가리기"
    }

    private fun raiseSub(p: Protection, linkedRecords: Int) = when (p) {
        Protection.HIDDEN -> "지문 · PIN으로 열림"
        Protection.CAPTURE_BLOCK -> "스크린샷 · 녹화 · 최근 앱"
        Protection.NAME_MASK -> "위젯 · 알림 · 최근 항목 ‘R-18 프로젝트’"
        Protection.RECORDER_LINK_MASK -> "연결에 등급만 · 기록 $linkedRecords 바로 적용"
    }

    private fun lowerSub(p: Protection) = when (p) {
        Protection.HIDDEN -> "풀면 바꿀 때 지문 확인"
        Protection.CAPTURE_BLOCK -> "스크린샷 · 녹화 · 최근 앱"
        Protection.NAME_MASK -> "위젯 · 알림 · 최근 항목"
        Protection.RECORDER_LINK_MASK -> "연결에 등급만"
    }

    /** `R-15 기본 · 끔`; the "on" wording is not in the sources. */
    private fun defaultLabel(to: Rating, on: Boolean) =
        if (on) TODO_COPY_LIB24_DEFAULT_ON else "${RatingLabels.badge(to)} 기본 · 끔"

    const val TODO_COPY_LIB24_DEFAULT_ON = "TODO_COPY_LIB24_DEFAULT_ON"
}
