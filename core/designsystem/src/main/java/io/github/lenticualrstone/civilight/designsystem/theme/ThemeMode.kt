package io.github.lenticualrstone.civilight.designsystem.theme

/** The theme setting (`기기 설정` · `라이트` · `다크`); the stored value is owned by LIB-65. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** The theme actually drawn. */
enum class ResolvedTheme { LIGHT, DARK }

/** DS-01: `isDark = theme == DARK || (theme == SYSTEM && systemIsDark)`. */
fun resolveTheme(mode: ThemeMode, systemDark: Boolean): ResolvedTheme = when (mode) {
    ThemeMode.DARK -> ResolvedTheme.DARK
    ThemeMode.LIGHT -> ResolvedTheme.LIGHT
    ThemeMode.SYSTEM -> if (systemDark) ResolvedTheme.DARK else ResolvedTheme.LIGHT
}
