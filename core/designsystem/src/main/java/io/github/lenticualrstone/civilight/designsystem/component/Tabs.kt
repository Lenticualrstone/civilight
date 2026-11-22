package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
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
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
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
 * slides with `Standard`. One line only; the row scrolls horizontally when it does not fit.
 */
@Composable
fun CivilightTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    gap: Dp = TabGap.Default,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val reduced = CivilightTheme.reducedMotion
    val bounds = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val x = remember { Animatable(0f) }
    val w = remember { Animatable(0f) }
    val target = bounds[selectedIndex]
    LaunchedEffect(target) {
        val t = target ?: return@LaunchedEffect
        if (w.value == 0f || reduced) {
            x.snapTo(t.first)
            w.snapTo(t.second)
        } else {
            val spec = CivilightMotion.spec<Float>(CivilightMotion.standard, reduced = false)
            launchBoth({ x.animateTo(t.first, spec) }, { w.animateTo(t.second, spec) })
        }
    }
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .selectableGroup()
            .drawBehind {
                if (w.value > 0f) {
                    val h = 2.dp.toPx()
                    drawRect(colors.acc, Offset(x.value, size.height - h), Size(w.value, h))
                }
            },
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { i, label ->
            val style = tabStyle(i == selectedIndex, colors)
            Box(
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .onPlaced { bounds[i] = it.positionInParent().x to it.size.width.toFloat() }
                    .pressable(PressAction.Select(i == selectedIndex) { onSelect(i) }, role = Role.Tab, scaleOnPress = false),
                contentAlignment = Alignment.Center,
            ) {
                CivilightText(label, type.tab, color = style.text, maxLines = 1)
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
