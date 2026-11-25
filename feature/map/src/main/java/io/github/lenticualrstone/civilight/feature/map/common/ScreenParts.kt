package io.github.lenticualrstone.civilight.feature.map.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.glass.OnGlass
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps

/** `‹` of a map screen: pops. */
fun mapBack(deps: FeatureDeps): IconAction = IconAction(MapCopy.BACK) { deps.navigator.pop() }

/**
 * A glass card floating over the map: the cover place sheet, the inner side card, the vertex bar
 * and the pick confirm bar are all this one element (WLD-37 fold rule: sheet ↔ side card).
 */
@Composable
fun GlassCard(modifier: Modifier = Modifier, radius: Int = 20, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(radius.dp)
    Box(modifier.blockMapTaps().insetOutline(CivilightTheme.colors.line, 1.dp, shape)) {
        GlassBackground(GlassSurface.PANEL, Modifier.matchParentSize(), shape = shape)
        OnGlass {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

/**
 * Keeps taps on a floating card (or the glass bar strip) from reaching the map below it. Buttons
 * inside still get their own clicks first.
 */
fun Modifier.blockMapTaps(): Modifier = pointerInput(Unit) {
    detectTapGestures { }
}

/** The strip under the glass top bar: taps there belong to the bar, never to the map. */
@Composable
fun BoxScope.TopBarTapGuard(height: Dp) {
    Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(height).blockMapTaps())
}

/** A card header: title (`하층 거리`) and `닫기`. */
@Composable
fun CardHeader(title: String, onClose: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        CivilightText(title, CivilightTheme.type.itemTitle, Modifier.weight(1f), maxLines = 1)
        if (onClose != null) TextAction(MapCopy.BTN_CLOSE, onClose)
    }
}

/** A `key | value` line of a card (`.row`): key `ink3`, value `ink`; tappable when [onClick] is set. */
@Composable
fun CardRow(key: String, value: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CivilightText(key, type.label, color = colors.ink3, maxLines = 1)
        if (onClick == null) {
            CivilightText(value, type.label, Modifier.weight(1f), color = colors.ink, maxLines = 1)
        } else {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                TextAction("$value ›", onClick, color = colors.ink)
            }
        }
    }
}
