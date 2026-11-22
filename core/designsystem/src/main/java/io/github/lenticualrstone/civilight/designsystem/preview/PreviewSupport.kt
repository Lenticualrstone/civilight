package io.github.lenticualrstone.civilight.designsystem.preview

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.glass.GlassMode
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ThemeMode

/** Dark and light previews of one composable (DS-64: both themes are equal citizens). */
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL, widthDp = 390, backgroundColor = 0xFF0B0C0E, showBackground = true)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL, widthDp = 390, backgroundColor = 0xFFF6F5F1, showBackground = true)
annotation class CivilightPreviews

/** Theme + `bg` + padding for previews; the theme follows the preview's night mode. */
@Composable
internal fun PreviewSurface(
    glassMode: GlassMode = GlassMode.SCRIM,
    padding: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    CivilightTheme(themeMode = ThemeMode.SYSTEM, glassMode = glassMode, applyToWindow = false) {
        Column(
            Modifier
                .background(CivilightTheme.colors.bg)
                .padding(if (padding) 16.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}
