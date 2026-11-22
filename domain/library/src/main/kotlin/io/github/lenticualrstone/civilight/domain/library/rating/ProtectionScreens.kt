package io.github.lenticualrstone.civilight.domain.library.rating

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.project.Rating

/**
 * Protections of a new project (LIB-09, LIB-12, R19 §2 table row 새 프로젝트): every value from the
 * chosen rating's defaults; the step-4 hide switch starts at the hide default and, if the author
 * turns it off, that is final while the other three stay as derived.
 */
data class NewProjectProtections(
    val protections: Protections,
    /** Initial value of the step-4 switch `이 프로젝트 숨기기`. */
    val step4ToggleInitial: Boolean,
    /** Hide was a default and the author turned it off: the summary says `숨김 · 끔`. */
    val hideTurnedOff: Boolean,
) {
    /** `숨김 · 끔` for the LIB-13 summary line, or null. */
    val summaryMarker: String? get() = if (hideTurnedOff) "숨김 · 끔" else null

    companion object {
        /** [step4Hide] = the step-4 switch as left by the author, null when untouched. */
        fun of(rating: Rating, defaults: RatingDefaults, step4Hide: Boolean?): NewProjectProtections {
            val derived = RatingDefaultsTable.defaultsFor(rating, defaults)
            val hidden = step4Hide ?: derived.hidden
            return NewProjectProtections(derived.copy(hidden = hidden), derived.hidden, derived.hidden && !hidden)
        }
    }
}

/** One switch row of 프로젝트 설정 › 등급 · 숨기기 (LIB-25). */
data class ProtectionSettingRow(
    val protection: Protection,
    val title: String,
    val sub: String,
    val on: Boolean,
    /** `<rating> 기본과 다름` (drawn in `acc`) when the value differs from the rating's default. */
    val differsMarker: String?,
) {
    /** The sub with the marker appended (` · ` joined). */
    val fullSub: String get() = if (differsMarker == null) sub else "$sub · $differsMarker"
}

/** 프로젝트 설정 › 등급 · 숨기기 (LIB-25). */
data class ProjectProtectionSettings(val rating: Rating, val rows: List<ProtectionSettingRow>) {
    val ratingRowTitle: String get() = "수위 등급"
    val ratingRowSub: String get() = "ALL · R-15 · R-18"
    val ratingRowValue: String get() = "${RatingLabels.badge(rating)} ›"

    companion object {
        fun of(rating: Rating, protections: Protections, defaults: RatingDefaults): ProjectProtectionSettings {
            val def = RatingDefaultsTable.defaultsFor(rating, defaults)
            val rows = Protection.entries.map { p ->
                val (title, sub) = when (p) {
                    Protection.HIDDEN -> "이 프로젝트 숨기기" to "지문 · PIN으로 열림"
                    Protection.CAPTURE_BLOCK -> "화면 캡처 가리기" to "스크린샷 · 최근 앱 미리 보기"
                    Protection.NAME_MASK -> "이름 가리기" to "위젯 · 알림 · 최근 항목"
                    Protection.RECORDER_LINK_MASK -> "라이프 레코더 연결 가리기" to "기록의 연결에 등급만 표시"
                }
                val on = protections.isOn(p)
                ProtectionSettingRow(p, title, sub, on, if (on != def.isOn(p)) "${RatingLabels.badge(rating)} 기본과 다름" else null)
            }
            return ProjectProtectionSettings(rating, rows)
        }
    }
}

/**
 * Switching one protection in 프로젝트 설정 (LIB-25): never changes the rating; turning hiding off
 * needs authentication first, turning it on is immediate (LIB-31).
 */
data class ProtectionSwitch(val protection: Protection, val before: Protections, val on: Boolean) {
    val requiresAuth: Boolean get() = protection == Protection.HIDDEN && before.hidden && !on
    val after: Protections get() = before.with(protection, on)
}

/** The tinted R-18 block under the step-1 picker (LIB-09). */
data class R18DefaultsBlock(val header: String, val lines: List<String>, val warning: String?) {
    companion object {
        const val HEADER = "R-18 기본 동작 · 설정에서 바꾸기"
        const val TODO_COPY_LIB09_RECORDER_LINE = "TODO_COPY_LIB09_RECORDER_LINE"

        /** One line per R-18 default that is on; ◇ `자동 백업에서 빠짐` when hiding is a default and no 복구 암호 exists. */
        fun of(defaults: RatingDefaults, recoverySet: Boolean): R18DefaultsBlock {
            val lines = buildList {
                if (defaults.r18Hide) add("프로젝트 숨기기 켬")
                if (defaults.r18Capture) add("화면 캡처 · 최근 앱 미리 보기 가림")
                if (defaults.r18NameMask) add("위젯 · 알림에 이름 대신 ‘R-18 프로젝트’")
                if (defaults.r18RecorderMask) add(TODO_COPY_LIB09_RECORDER_LINE)
            }
            return R18DefaultsBlock(HEADER, lines, if (defaults.r18Hide && !recoverySet) "자동 백업에서 빠짐" else null)
        }
    }
}

/** A project as listed for `이미 있는 … 같이 바꾸기` (LIB-23). */
data class RatedProject(val key: Key, val name: String, val rating: Rating, val protections: Protections)

/**
 * Changing a default in 설정 · 수위 등급 (LIB-23, R19-10): only new projects and future rating
 * changes use it; existing projects keep their values until the author applies the change to
 * checked ones.
 */
object RatingDefaultsChanges {
    /**
     * Projects listed under a changed group: same rating; hidden projects only while the hidden
     * group is open (`ED`).
     */
    fun existing(rating: Rating, projects: List<RatedProject>, hiddenGroupOpen: Boolean): List<RatedProject> =
        projects.filter { it.rating == rating && (hiddenGroupOpen || !it.protections.hidden) }

    /** `이미 있는 R-18 프로젝트 2 · 같이 바꾸기`, or null when the group is unchanged or has no project. */
    fun tag(rating: Rating, defaults: RatingDefaults, listed: List<RatedProject>): String? {
        if (!defaults.changed(rating) || listed.isEmpty()) return null
        return "이미 있는 ${RatingLabels.badge(rating)} 프로젝트 ${listed.size} · 같이 바꾸기"
    }

    /**
     * Sets [protection] to the group's current default on the checked projects. Hiding going off
     * goes through LIB-31 (authentication): those projects are returned in [ApplyToExisting.needsAuth]
     * and left unchanged.
     */
    fun applyToChecked(
        rating: Rating,
        protection: Protection,
        defaults: RatingDefaults,
        checked: List<RatedProject>,
    ): ApplyToExisting {
        val value = RatingDefaultsTable.defaultsFor(rating, defaults).isOn(protection)
        val (needsAuth, direct) = checked.partition { protection == Protection.HIDDEN && it.protections.hidden && !value }
        return ApplyToExisting(direct.map { it.copy(protections = it.protections.with(protection, value)) }, needsAuth)
    }

    data class ApplyToExisting(val updated: List<RatedProject>, val needsAuth: List<RatedProject>)
}
