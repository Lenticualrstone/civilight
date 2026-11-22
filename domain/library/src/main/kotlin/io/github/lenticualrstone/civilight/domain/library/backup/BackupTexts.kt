package io.github.lenticualrstone.civilight.domain.library.backup

import io.github.lenticualrstone.civilight.domain.library.common.TimeLabels
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.BackupKind
import io.github.lenticualrstone.civilight.model.app.BackupRun
import io.github.lenticualrstone.civilight.model.app.BackupState
import java.time.ZoneId

/** One run in 백업에서 되살리기 (LIB-59, B-08). */
data class RestoreRunRow(val title: String, val sub: String, val button: String) {
    companion object {
        fun of(run: BackupRun, zone: ZoneId): RestoreRunRow {
            val projects = run.entries.filter { it.kind == BackupKind.PROJECT }
            val hidden = projects.count { it.hidden }
            val size = TimeLabels.size(run.totalBytes)
            val sub = if (hidden > 0) "프로젝트 ${projects.size} · 숨김 $hidden 포함 · $size" else "프로젝트 ${projects.size} · $size"
            return RestoreRunRow(
                title = TimeLabels.monthDayTime(run.startedAt, zone),
                sub = sub,
                button = "${TimeLabels.monthDay(run.startedAt, zone)} 백업 되살리기",
            )
        }
    }
}

/** Failure reasons; the login reason is drawn; the others follow its pattern. */
enum class BackupFailure(val code: String, val label: String) {
    LOGIN_EXPIRED("loginExpired", "Google 로그인 만료"),
    NO_SPACE("noSpace", "Drive 저장 공간 부족"),
    NETWORK("network", "네트워크 연결 없음"),
    ;

    companion object {
        fun of(code: String?): BackupFailure? = entries.firstOrNull { it.code == code }
    }
}

/** The 설정 › 보관 failure banner (LIB-61). */
data class BackupFailureBanner(val title: String, val sub: String, val buttons: List<String>, val driveRow: String?) {
    companion object {
        /**
         * Null when the current streak has no failure. `<n>일째` is 1-based: calendar days from the
         * first failed run of the streak to today, plus one (`ED`).
         */
        fun of(state: BackupState, now: Timestamp, zone: ZoneId): BackupFailureBanner? {
            val start = state.failureStreakStartedAt ?: return null
            val day = TimeLabels.daysBetween(start, now, zone) + 1
            val reason = BackupFailure.of(state.lastFailureReason)?.label ?: (state.lastFailureReason ?: "이유 알 수 없음")
            return BackupFailureBanner(
                title = "백업 실패 · ${day}일째",
                sub = "$reason · 원고는 기기에 안전",
                buttons = listOf("다시 로그인", "지금 다시 시도"),
                driveRow = state.lastSuccessAt?.let { "마지막 성공 ${TimeLabels.monthDayTime(it, zone)}" },
            )
        }
    }
}

/** 되살리는 중 (LIB-60) step lines. */
data class RestoreProgress(val doneBytes: Long, val totalBytes: Long, val projects: Int, val images: Int, val audio: Int, val hiddenProjects: Int) {
    val title: String get() = "되살리는 중"
    val bytesLine: String get() = "${mb(doneBytes)} / ${mb(totalBytes)} MB"
    val steps: List<Pair<String, String>>
        get() = buildList {
            add("원고 · 인물 · 세계" to "${projects}개 프로젝트")
            add("에셋 · 음성 기록" to "이미지 $images · 음성 $audio")
            if (hiddenProjects > 0) add("숨긴 프로젝트 $hiddenProjects" to "지문 확인 후 풀림")
        }
    val note: String get() = "앱을 닫아도 계속 · 끝나면 알림"

    private fun mb(bytes: Long): String = Math.round(bytes / (1024.0 * 1024.0)).toString()
}
