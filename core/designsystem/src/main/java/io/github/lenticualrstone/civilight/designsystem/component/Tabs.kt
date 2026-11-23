package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** DS-37 fixed tab sets (copy from the spec). */
object TabSets {
    /** Character record tabs (5, fixed). */
    val characterRecord: List<String> = listOf("개요", "말투", "관계", "게임 텍스트", "비밀")

    /** Script panel / sheet tabs. */
    val scriptPanel: List<String> = listOf("시간", "참고", "속성", "모듈")
}

/** DS-37 tab gaps: 18 default, 16 panels, 14 cover sheets, 22 settings kind tabs. */
object TabGap {
    val Default: Dp = 18.dp
    val Panel: Dp = 16.dp
    val Sheet: Dp = 14.dp
    val Settings: Dp = 22.dp
}

/**
 * DS-37 text tabs: `tab` role, selected `ink` with a 2 dp `acc` underline at the bottom of its 44 dp box that
 * slides with `Standard`; label colours cross over (S6). Reduced motion: the underline does not move, it
 * crossfades from the old tab to the new one. One line only; the row scrolls horizontally when it does not fit.
 * [tabModifier] adds to one tab's box by index (a tour marks a tab as its hole, LIB-19).
 */
@Composable
fun CivilightTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    gap: Dp = TabGap.Default,
    tabModifier: (Int) -> Modifier = { Modifier },
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val indicator = rememberSlidingIndicator(selectedIndex)
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .selectableGroup()
            .drawBehind {
                val h = 2.dp.toPx()
                with(indicator) {
                    drawIndicator { x, w, a -> drawRect(colors.acc, Offset(x, size.height - h), Size(w, h), alpha = a) }
                }
            },
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { i, label ->
            val style = tabStyle(i == selectedIndex, colors)
            val text by animatedColor(style.text, label = "tabText")
            Box(
                modifier = tabModifier(i)
                    .heightIn(min = 44.dp)
                    .onPlaced { indicator.bounds[i] = it.positionInParent().x to it.size.width.toFloat() }
                    .pressable(PressAction.Select(i == selectedIndex) { onSelect(i) }, role = Role.Tab, scaleOnPress = false),
                contentAlignment = Alignment.Center,
            ) {
                CivilightText(label, type.tab, color = text, maxLines = 1)
            }
        }
    }
}

internal suspend fun launchBoth(a: suspend () -> Unit, b: suspend () -> Unit) {
    coroutineScope {
        launch { a() }
        launch { b() }
    }
}
