package io.github.lenticualrstone.civilight.feature.story.check

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.component.travelBarFraction
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.ValueSwap
import io.github.lenticualrstone.civilight.domain.checks.FixAction
import io.github.lenticualrstone.civilight.domain.story.checks.Marker
import io.github.lenticualrstone.civilight.domain.story.travel.TravelWarning
import io.github.lenticualrstone.civilight.feature.story.common.MiniMap
import io.github.lenticualrstone.civilight.feature.story.common.MiniMapView

/**
 * The STO-30 travel card (board `TravelCheck`; also the block of the `장소와 때` sheet): a `tint` card
 * (radius 18, 1 dp `edge`, padding 14) with `▲ <kind>` in `warn` and [tag] in `small`, the [routeLine] in
 * the serif, the optional mini [map] (318 × 150 on the board), the `필요` / `있음` [TravelBar] on one hour
 * scale, the [detail] line and the fix [chips] as compact ghost buttons (`씬 3 · +2일로` · `말 · 약 7시간
 * 40분` · `맞음`). [onChip] gets the chip's index.
 */
@Composable
fun TravelCard(
    title: String,
    tag: String?,
    routeLine: String?,
    warning: TravelWarning,
    detail: String?,
    chips: List<String>,
    onChip: (Int) -> Unit,
    modifier: Modifier = Modifier,
    map: MiniMap? = null,
    location: String? = null,
    onLocation: (() -> Unit)? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.tint)
            .insetOutline(colors.edge, 1.dp, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            CivilightText(title, type.label.merge(TextStyle(fontWeight = FontWeight.Bold)), color = colors.warn, maxLines = 1)
            tag?.let { CivilightText(it, type.small, Modifier.weight(1f, fill = false), color = colors.ink3, maxLines = 1) }
            LocationAction(location, onLocation, Modifier.weight(1f))
        }
        routeLine?.let { CivilightText(it, type.body, color = colors.ink) }
        map?.let { MiniMapView(it, Modifier.padding(top = 6.dp).fillMaxWidth().height(150.dp)) }
        AnimatedTravelBar(
            requiredValue = warning.needText,
            availableValue = warning.haveText,
            requiredMinutes = warning.requiredMinutes.toLong(),
            availableMinutes = warning.gap.max.toLong().coerceAtLeast(0L),
            modifier = Modifier.padding(top = 8.dp),
        )
        detail?.let { CivilightText(it, type.small, Modifier.padding(top = 4.dp), color = colors.ink3) }
        if (chips.isNotEmpty()) {
            ChipFlow(Modifier.padding(top = 8.dp)) {
                chips.forEachIndexed { i, label -> CivilightButton(label, { onChip(i) }, variant = ButtonVariant.COMPACT_GHOST) }
            }
        }
    }
}

/**
 * DS `TravelBar` (rows `필요` · `있음` on one scale) with the `있음` fill animated on `standard` when the figures
 * change (motion audit, feature:story 11). The DS bar draws its fill at once; this copy goes when the DS bar
 * animates (Request in `docs/status/feature-story.md`). Reduced: the fill snaps (`motion.move`).
 */
@Composable
private fun AnimatedTravelBar(
    requiredValue: String,
    availableValue: String,
    requiredMinutes: Long,
    availableMinutes: Long,
    modifier: Modifier = Modifier,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val fraction by animateFloatAsState(
        travelBarFraction(availableMinutes, requiredMinutes).coerceIn(0f, 1f),
        LocalMotion.current.move(CivilightMotion.standard),
        label = "travelFill",
    )
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple(TravelCopy.REQUIRED, requiredValue, 1f to colors.warn),
            Triple(TravelCopy.AVAILABLE, availableValue, fraction to colors.acc),
        ).forEach { (label, value, bar) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CivilightText(label, type.small, Modifier.width(44.dp), color = colors.ink3, maxLines = 1)
                Box(Modifier.weight(1f).height(8.dp)) {
                    Box(Modifier.fillMaxWidth(bar.first).height(8.dp).clip(RoundedCornerShape(4.dp)).background(bar.second))
                }
                ValueSwap(value, type.small, Modifier.widthIn(min = 64.dp), color = colors.ink)
            }
        }
    }
}

/**
 * The compact travel row (`TravelCheck` list form): the marker glyph (`▲` `warn`, `◇` `ink3`), the bold
 * serif title and one small line; [trailing] is drawn at the right (`한꺼번에 ›`) and the other [buttons]
 * under the line (first `ink`, then `ink2`, last `ink3`).
 */
@Composable
fun TravelCompactRow(
    look: TravelLook.Compact,
    buttons: List<FixAction>,
    onButton: (FixAction) -> Unit,
    selected: Boolean = false,
    location: String? = null,
    onLocation: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val trailing = if (look.trailingAction) buttons.firstOrNull() else null
    val below = if (trailing != null) buttons.drop(1) else buttons
    Column(
        modifier.fillMaxWidth().topHairline(colors.line).selectedRow(selected).padding(top = 12.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            CivilightText(
                look.marker.glyph,
                type.label.merge(TextStyle(fontWeight = FontWeight.Bold)),
                color = if (look.marker == Marker.WARNING) colors.warn else colors.ink3,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                CivilightText(look.title, type.itemTitleCompact, color = colors.ink)
                CivilightText(look.line, type.small, color = colors.ink3)
            }
            trailing?.let { TextAction(it.label, { onButton(it) }, Modifier.widthIn(min = 44.dp), color = colors.ink2, style = type.small) }
            if (trailing == null && location != null) LocationAction(location, onLocation)
        }
        if (below.isNotEmpty()) ButtonsRow(below, onButton)
    }
}

/** The row's location at the right (CNV-17 anatomy): a text action opening the script there, else plain `ink3`. */
@Composable
private fun LocationAction(location: String?, onLocation: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(modifier, horizontalArrangement = Arrangement.End) {
        when {
            location == null -> Unit
            onLocation != null -> TextAction(location, onLocation, color = colors.ink2, style = type.small)
            else -> CivilightText(location, type.small, color = colors.ink3, maxLines = 1)
        }
    }
}
