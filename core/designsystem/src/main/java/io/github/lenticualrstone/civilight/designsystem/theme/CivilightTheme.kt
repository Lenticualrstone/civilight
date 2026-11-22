package io.github.lenticualrstone.civilight.designsystem.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBudget
import io.github.lenticualrstone.civilight.designsystem.glass.GlassMode
import io.github.lenticualrstone.civilight.designsystem.glass.LocalGlassBudget

val LocalCivilightColors = staticCompositionLocalOf { CivilightColors.dark }
val LocalCivilightType = staticCompositionLocalOf { CivilightType.create() }
val LocalCivilightSpacing = staticCompositionLocalOf { CivilightSpacing() }
val LocalCivilightShapes = staticCompositionLocalOf { CivilightShapes() }
val LocalCivilightSizes = staticCompositionLocalOf { CivilightSizes() }
val LocalCivilightBlur = staticCompositionLocalOf { CivilightBlur() }
val LocalCivilightElevation = staticCompositionLocalOf { CivilightElevation() }
val LocalTextScale = staticCompositionLocalOf { TextScale.Default }

/** Resolved glass mode (CNV-46 decides it; the app passes it to [CivilightTheme]). */
val LocalGlassMode = staticCompositionLocalOf { GlassMode.BLUR }

/** Whether motion is reduced (CNV-46 decides it). Components then fade 120 ms instead of moving. */
val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * System back hook for in-composition overlays (sheets, side panels). The design system has no
 * activity dependency; the app provides one (e.g. activity-compose `BackHandler`). Default: no-op.
 */
val LocalBackHandler = staticCompositionLocalOf<@Composable (enabled: Boolean, onBack: () -> Unit) -> Unit> {
    { _, _ -> }
}

/**
 * Root theme (DS-01). Provides every token through composition locals and bridges Material 3
 * (no dynamic colour). [applyToWindow] also sets the window background to `bg` and the system-bar icon
 * appearance; edge-to-edge itself is enabled by the activity.
 */
@Composable
fun CivilightTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    textScale: TextScale = TextScale.Default,
    glassMode: GlassMode = GlassMode.resolve(Build.VERSION.SDK_INT, reduceTransparency = false),
    reducedMotion: Boolean = false,
    applyToWindow: Boolean = true,
    content: @Composable () -> Unit,
) {
    val resolved = resolveTheme(themeMode, isSystemInDarkTheme())
    val colors = colorsFor(resolved)
    val type = remember(textScale) { CivilightType.create(textScale) }
    val budget = remember { GlassBudget() }
    if (applyToWindow) SystemBarsAppearance(colors)
    CompositionLocalProvider(
        LocalCivilightColors provides colors,
        LocalCivilightType provides type,
        LocalTextScale provides textScale,
        LocalGlassMode provides glassMode,
        LocalGlassBudget provides budget,
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(colorScheme = materialBridge(colors), typography = materialTypography(type)) {
            CompositionLocalProvider(
                LocalContentColor provides colors.ink,
                LocalTextSelectionColors provides TextSelectionColors(
                    handleColor = colors.acc,
                    backgroundColor = colors.acc.copy(alpha = 0.3f),
                ),
                content = content,
            )
        }
    }
}

/** Token accessors, as `CivilightTheme.colors.ink`. */
object CivilightTheme {
    val colors: CivilightColors @Composable @ReadOnlyComposable get() = LocalCivilightColors.current
    val type: CivilightType @Composable @ReadOnlyComposable get() = LocalCivilightType.current
    val spacing: CivilightSpacing @Composable @ReadOnlyComposable get() = LocalCivilightSpacing.current
    val shapes: CivilightShapes @Composable @ReadOnlyComposable get() = LocalCivilightShapes.current
    val sizes: CivilightSizes @Composable @ReadOnlyComposable get() = LocalCivilightSizes.current
    val blur: CivilightBlur @Composable @ReadOnlyComposable get() = LocalCivilightBlur.current
    val elevation: CivilightElevation @Composable @ReadOnlyComposable get() = LocalCivilightElevation.current
    val motion: CivilightMotion get() = CivilightMotion
    val glassMode: GlassMode @Composable @ReadOnlyComposable get() = LocalGlassMode.current
    val reducedMotion: Boolean @Composable @ReadOnlyComposable get() = LocalReducedMotion.current
    val textScale: TextScale @Composable @ReadOnlyComposable get() = LocalTextScale.current
}

/** DS-01 Material 3 bridge (proposed mapping). */
fun materialBridge(c: CivilightColors): ColorScheme {
    val base = if (c.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = c.pri,
        onPrimary = c.onpri,
        secondary = c.acc,
        tertiary = c.acc,
        background = c.bg,
        onBackground = c.ink,
        surface = c.bg,
        onSurface = c.ink,
        surfaceContainer = c.raise,
        onSurfaceVariant = c.ink2,
        outline = c.edge,
        outlineVariant = c.line,
        error = c.r18,
        onError = c.onr18,
        scrim = c.shade,
    )
}

private fun materialTypography(t: CivilightType): Typography = Typography(
    displayLarge = t.hero,
    displayMedium = t.display,
    displaySmall = t.displayM,
    headlineLarge = t.pageTitle,
    headlineMedium = t.stateTitle,
    headlineSmall = t.dialogTitle,
    titleLarge = t.title,
    titleMedium = t.heading,
    titleSmall = t.itemTitle,
    bodyLarge = t.body,
    bodyMedium = t.command,
    bodySmall = t.small,
    labelLarge = t.button,
    labelMedium = t.label,
    labelSmall = t.micro,
)

@Composable
private fun SystemBarsAppearance(colors: CivilightColors) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        window.setBackgroundDrawable(ColorDrawable(colors.bg.toArgb()))
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !colors.isDark
        controller.isAppearanceLightNavigationBars = !colors.isDark
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
