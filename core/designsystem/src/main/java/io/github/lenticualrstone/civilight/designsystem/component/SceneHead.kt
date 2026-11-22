package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/**
 * DS-43 scene head (not a block): 1 dp `line` top border, margins 8 top / 24 left / 12 right, padding-top
 * 10; title in `sceneHead` (`씬 4 · 결사의 문 · 2일째 아침 06:10`), [subtitle] in `number` 11 / 16 `ink3`
 * (`전환 문구 · 결사의 문 앞 · 06:10 · 직접`), and a `⋯` icon button (44, shifted −6 dp up) labelled
 * [menu] (label `씬 4 메뉴`). Selected (menu open): `sel` fill, `⋯` in `ink`, no bar.
 * Without [menu] the older geometry applies (margins 0 × 24, padding-top 12).
 */
@Composable
fun SceneHead(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    menu: IconAction? = null,
    selected: Boolean = false,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (menu != null) 8.dp else 0.dp, start = 24.dp, end = if (menu != null) 12.dp else 24.dp)
            .then(if (selected) Modifier.background(colors.sel) else Modifier)
            .topHairline(colors.line)
            .padding(top = if (menu != null) 10.dp else 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            CivilightText(title, type.sceneHead, color = colors.ink)
            if (subtitle != null) {
                CivilightText(subtitle, type.number.merge(TextStyle(fontSize = 11.sp, lineHeight = 16.sp)), color = colors.ink3)
            }
        }
        if (menu != null) {
            CivilightIconButton(
                IconId.More,
                menu.label,
                menu.onClick,
                Modifier.offset(y = (-6).dp),
                tint = if (selected) colors.ink else colors.ink2,
            )
        }
    }
}
