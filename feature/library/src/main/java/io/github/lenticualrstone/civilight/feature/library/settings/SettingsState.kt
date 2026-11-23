package io.github.lenticualrstone.civilight.feature.library.settings

import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.AnimationPref
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.CardStyle
import io.github.lenticualrstone.civilight.model.app.LibraryStyle
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfViewRenderers
import io.github.lenticualrstone.civilight.model.app.RelockPolicy
import io.github.lenticualrstone.civilight.model.app.ThemeMode
import io.github.lenticualrstone.civilight.model.app.UnfoldedLayout
import io.github.lenticualrstone.civilight.domain.library.common.TimeLabels
import java.time.ZoneId

/** A `title · sub · value ›` row of a settings list. */
data class SettingsLinkRow(val title: String, val sub: String?, val value: String?)

/** A switch row; [enabled] false draws it at the disabled alpha and ignores taps. */
data class SettingsSwitchRow(val title: String, val sub: String?, val on: Boolean, val enabled: Boolean = true)

/** 설정 root (LIB-63, board `Settings`). */
data class SettingsUi(
    val themeIndex: Int,
    val textSize: SettingsLinkRow,
    val layout: SettingsLinkRow,
    val motion: SettingsLinkRow,
    val more: SettingsLinkRow,
    val drive: SettingsSwitchRow,
    val recovery: SettingsLinkRow,
    val appLock: SettingsSwitchRow,
    val hiding: SettingsLinkRow,
    /**
     * (0.2.0, LIB-94) `서재 보기` · `카드 보기` · tilt switch, after `펼친 화면 배치`. Each is null (not drawn)
     * while the views it would change have no renderer ([ViewStyles.libraryRow] …).
     */
    val libraryView: SettingsLinkRow? = null,
    val cardView: SettingsLinkRow? = null,
    val tilt: SettingsSwitchRow? = null,
    /** `인물 템플릿` · `템플릿 3` (CST-58; hidden templates are never counted while locked). */
    val templates: SettingsLinkRow = SettingsLinkRow(SettingsCopy.TEMPLATES_TITLE, SettingsCopy.templatesSub(0), null),
)

/** One `다시 잠그는 때` radio. */
data class RelockOption(val policy: RelockPolicy, val title: String, val sub: String?, val selected: Boolean)

/** 설정 · 숨기기 (LIB-64, board `HideSettings`). */
data class HidingSettingsUi(
    val relock: List<RelockOption>,
    val searchResults: SettingsSwitchRow,
    val widgets: SettingsSwitchRow,
    /** `지문 · 실패하면 기기 PIN · 사용 중`; the status only while a device lock exists. */
    val biometricStatus: String?,
    val backupHidden: SettingsSwitchRow,
    /** (0.2.0, LIB-92) Kept for the model; not drawn (the row left 설정 · 숨기기). */
    val ratingRules: SettingsLinkRow,
)

/** 화면 · 글자 크기 (LIB-65): slider position 0…4 (model −2…+2) and the row texts. */
data class TextSizeUi(val themeIndex: Int, val sliderIndex: Int, val stepCount: Int, val label: String)

/**
 * Pure builders of the app-settings screens. Everything comes from [AppSettings] plus a few facts
 * (hidden project count, 복구 암호 set, last backup); writes are the view model's.
 */
object SettingsState {
    val THEMES = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
    val THEME_LABELS = listOf(SettingsCopy.THEME_SYSTEM, SettingsCopy.THEME_LIGHT, SettingsCopy.THEME_DARK)

    /** Model steps −2…+2 (`보통` = 0) and their body sizes (LIB-65 `ED` 16/17/18/20/22 sp). */
    const val MIN_STEP = -2
    const val MAX_STEP = 2
    private val BODY_SP = mapOf(-2 to 16, -1 to 17, 0 to 18, 1 to 20, 2 to 22)

    fun themeIndex(mode: ThemeMode): Int = THEMES.indexOf(mode).coerceAtLeast(0)

    fun clampStep(step: Int): Int = step.coerceIn(MIN_STEP, MAX_STEP)

    fun sliderIndexOf(step: Int): Int = clampStep(step) - MIN_STEP

    fun stepOfSlider(index: Int): Int = clampStep(index + MIN_STEP)

    /** `본문 18 · 행간 1.7`. */
    fun textSizeLabel(step: Int): String = "${SettingsCopy.BODY} ${BODY_SP.getValue(clampStep(step))} · ${SettingsCopy.LINE_HEIGHT}"

    /** Only `보통` has a name (other names: not in sources, Q-ST-02); the others show `›` alone. */
    fun textSizeValue(step: Int): String? = if (clampStep(step) == 0) SettingsCopy.TEXT_SIZE_DEFAULT else null

    fun layoutValue(layout: UnfoldedLayout): String = when (layout) {
        UnfoldedLayout.LIST_DETAIL -> SettingsCopy.LAYOUT_LIST_DETAIL
        UnfoldedLayout.CENTER_COLUMN -> SettingsCopy.LAYOUT_CENTER
    }

    fun motionValue(pref: AnimationPref): String = when (pref) {
        AnimationPref.FOLLOW_SYSTEM -> SettingsCopy.MOTION_FOLLOW
        AnimationPref.ALWAYS_REDUCE -> SettingsCopy.MOTION_ALWAYS
        AnimationPref.NEVER_REDUCE -> SettingsCopy.MOTION_NEVER
    }

    fun relockTitle(policy: RelockPolicy): String = when (policy) {
        RelockPolicy.ON_LEAVE -> SettingsCopy.RELOCK_ON_LEAVE
        RelockPolicy.SCREEN_OFF_ONLY -> SettingsCopy.RELOCK_SCREEN_OFF
        RelockPolicy.FIVE_MINUTES -> SettingsCopy.RELOCK_FIVE_MINUTES
    }

    private fun relockSub(policy: RelockPolicy): String? = when (policy) {
        RelockPolicy.ON_LEAVE -> SettingsCopy.RELOCK_ON_LEAVE_SUB
        RelockPolicy.SCREEN_OFF_ONLY -> SettingsCopy.RELOCK_SCREEN_OFF_SUB
        RelockPolicy.FIVE_MINUTES -> null
    }

    /**
     * The root. [lastBackupAt] null (no successful Drive run yet, LIB-57) leaves the Drive row
     * without a sub line (`ED`: the board only draws `마지막 백업 <time>`).
     */
    fun root(
        settings: AppSettings,
        hiddenProjects: Int,
        recoverySet: Boolean,
        lastBackupAt: Timestamp?,
        now: Timestamp,
        zone: ZoneId,
        visibleTemplates: Int = 0,
        libraryViews: Set<LibraryStyle> = ShelfViewRenderers.available,
        cardViews: Set<CardStyle> = CardViewMirror.available,
    ): SettingsUi = SettingsUi(
        themeIndex = themeIndex(settings.theme),
        textSize = SettingsLinkRow(SettingsCopy.TEXT_SIZE, textSizeLabel(settings.textSizeStep), textSizeValue(settings.textSizeStep)),
        layout = SettingsLinkRow(SettingsCopy.LAYOUT, SettingsCopy.LAYOUT_SUB, layoutValue(settings.unfoldedLayout)),
        motion = SettingsLinkRow(SettingsCopy.MOTION, SettingsCopy.MOTION_SUB, motionValue(settings.animation)),
        more = SettingsLinkRow(SettingsCopy.MORE, SettingsCopy.MORE_SUB, null),
        drive = SettingsSwitchRow(
            title = SettingsCopy.DRIVE,
            sub = lastBackupAt?.let { "${SettingsCopy.LAST_BACKUP} ${TimeLabels.relative(it, now, zone)}" },
            on = settings.driveBackup,
            // Turning it on needs a Drive account (LIB-57); the row opens 설정 › 보관 instead.
            enabled = false,
        ),
        recovery = SettingsLinkRow(
            title = SettingsCopy.RECOVERY,
            sub = if (recoverySet) "${SettingsCopy.RECOVERY_PURPOSE} · ${SettingsCopy.RECOVERY_SET}" else SettingsCopy.RECOVERY_PURPOSE,
            value = if (recoverySet) SettingsCopy.RECOVERY_CHANGE else SettingsCopy.RECOVERY_DEFINE,
        ),
        appLock = SettingsSwitchRow(SettingsCopy.APP_LOCK, SettingsCopy.APP_LOCK_SUB, settings.appLock),
        hiding = SettingsLinkRow(
            title = SettingsCopy.HIDING,
            sub = "${SettingsCopy.HIDDEN_PROJECTS} $hiddenProjects · ${SettingsCopy.RELOCK_WHEN}",
            value = relockTitle(settings.relockPolicy),
        ),
        templates = SettingsLinkRow(SettingsCopy.TEMPLATES_TITLE, SettingsCopy.templatesSub(visibleTemplates), null),
        libraryView = ViewStyles.libraryRow(settings, libraryViews),
        cardView = ViewStyles.cardRow(settings, cardViews),
        tilt = ViewStyles.tiltRow(settings, libraryViews, cardViews),
    )

    /**
     * 설정 · 숨기기. `위젯 · 최근 항목 · 알림` stays on and disabled (§6 Q28 `ED`: turning it off has no
     * defined meaning and must never reveal a hidden name). The biometric status shows only while
     * the device has a secure lock screen (its absence: copy not in sources).
     */
    fun hiding(settings: AppSettings, deviceSecure: Boolean): HidingSettingsUi = HidingSettingsUi(
        relock = RelockPolicy.entries.map { RelockOption(it, relockTitle(it), relockSub(it), it == settings.relockPolicy) },
        searchResults = SettingsSwitchRow(SettingsCopy.MASK_SEARCH, SettingsCopy.MASK_SEARCH_SUB, settings.hiddenInSearchWhileOpen),
        widgets = SettingsSwitchRow(SettingsCopy.MASK_WIDGETS, SettingsCopy.MASK_WIDGETS_SUB, on = true, enabled = false),
        biometricStatus = if (deviceSecure) SettingsCopy.AUTH_IN_USE else null,
        backupHidden = SettingsSwitchRow(SettingsCopy.BACKUP_HIDDEN, SettingsCopy.BACKUP_HIDDEN_SUB, settings.backupHidden),
        ratingRules = SettingsLinkRow(SettingsCopy.RATING_RULES, SettingsCopy.RATING_RULES_SUB, null),
    )

    fun textSize(settings: AppSettings): TextSizeUi = TextSizeUi(
        themeIndex = themeIndex(settings.theme),
        sliderIndex = sliderIndexOf(settings.textSizeStep),
        stepCount = MAX_STEP - MIN_STEP + 1,
        label = textSizeLabel(settings.textSizeStep),
    )
}

/** What 앱 잠금 does when its switch is tapped (LIB-35, §6 Q19). */
enum class AppLockChange {
    /** No change (same value). */
    NONE,

    /** Turn on: needs a secure lock screen, else refused with a toast. */
    TURN_ON,
    REFUSED_NO_DEVICE_LOCK,

    /** Turn off: one prompt first (`ED`); cancel changes nothing. */
    TURN_OFF_AFTER_AUTH,
}

object AppLockRules {
    fun change(current: Boolean, wanted: Boolean, deviceSecure: Boolean): AppLockChange = when {
        current == wanted -> AppLockChange.NONE
        wanted && !deviceSecure -> AppLockChange.REFUSED_NO_DEVICE_LOCK
        wanted -> AppLockChange.TURN_ON
        else -> AppLockChange.TURN_OFF_AFTER_AUTH
    }
}
