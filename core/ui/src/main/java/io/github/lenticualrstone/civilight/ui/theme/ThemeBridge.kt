package io.github.lenticualrstone.civilight.ui.theme

import android.content.Context
import android.os.Build
import android.provider.Settings
import io.github.lenticualrstone.civilight.designsystem.component.Rating as DsRating
import io.github.lenticualrstone.civilight.designsystem.glass.GlassMode
import io.github.lenticualrstone.civilight.designsystem.theme.TextScale
import io.github.lenticualrstone.civilight.designsystem.theme.ThemeMode as DsThemeMode
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.app.AnimationPref
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.ThemeMode
import io.github.lenticualrstone.civilight.model.project.Rating

/**
 * Maps stored values (core:model) to the design system's own enums once, here. The design system
 * has no model dependency, so it has its own `ThemeMode`, `Rating` and text steps.
 */
object ThemeBridge {
    /** Step of `보통` (18 sp) in the design system's `TextScale.StepsSp` (16 / 17 / 18 / 20 / 22). */
    const val DS_DEFAULT_STEP = 2

    fun themeMode(mode: ThemeMode): DsThemeMode = when (mode) {
        ThemeMode.SYSTEM -> DsThemeMode.SYSTEM
        ThemeMode.LIGHT -> DsThemeMode.LIGHT
        ThemeMode.DARK -> DsThemeMode.DARK
    }

    fun rating(rating: Rating): DsRating = when (rating) {
        Rating.ALL -> DsRating.ALL
        Rating.R15 -> DsRating.R15
        Rating.R18 -> DsRating.R18
    }

    /** Unknown stored ratings draw as `ALL` (labels only). */
    fun rating(rating: OpenEnum<Rating>): DsRating = rating(rating.known ?: Rating.ALL)

    /** `AppSettings.textSizeStep` counts from `보통` = 0; the design system's steps start at the smallest. */
    fun textScale(settings: AppSettings): TextScale = TextScale.forStep(settings.textSizeStep + DS_DEFAULT_STEP)

    /** CNV-46: reduced when `항상 줄이기`, or `기기 설정 따름` with the system's animations removed. */
    fun reducedMotion(pref: AnimationPref, systemAnimationsOff: Boolean): Boolean = when (pref) {
        AnimationPref.ALWAYS_REDUCE -> true
        AnimationPref.NEVER_REDUCE -> false
        AnimationPref.FOLLOW_SYSTEM -> systemAnimationsOff
    }

    /** CNV-46 glass tier: `투명도 줄이기` → opaque; else by API level. */
    fun glassMode(settings: AppSettings, apiLevel: Int = Build.VERSION.SDK_INT): GlassMode =
        GlassMode.resolve(apiLevel, settings.reduceTransparency)

    /** The system `애니메이션 삭제` setting (animator duration scale 0). */
    fun systemAnimationsOff(context: Context): Boolean =
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}
