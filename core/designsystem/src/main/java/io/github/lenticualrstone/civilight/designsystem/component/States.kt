package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import kotlinx.coroutines.delay

/**
 * DS-41 lock row: padding 14 × 16, radius 16, `tint` fill, 1 dp `edge` inset, gap 12; optional `Lock` 18
 * `ink`; title `itemTitle` (`숨긴 프로젝트 2`) + `small` sub-line; trailing compact primary button 40 tall,
 * 12 sp (`지문으로 보기`). Locked names are never rendered behind it.
 */
@Composable
fun LockRow(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showIcon: Boolean = true,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.tint)
            .insetOutline(colors.edge, 1.dp, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showIcon) CivilightIcon(IconId.Lock, contentDescription = null, size = 18.dp, tint = colors.ink)
        Column(Modifier.weight(1f)) {
            CivilightText(title, type.itemTitle, color = colors.ink, maxLines = 1)
            if (subtitle != null) CivilightText(subtitle, type.small, color = colors.ink3)
        }
        CivilightButton(actionLabel, onAction, variant = ButtonVariant.COMPACT_PRIMARY)
    }
}

/**
 * DS-47 empty block: 18 dp top margin, padding 26 × 20, radius 20, 1 dp `edge` inset; title 800 22 / 30
 * (`인물 없음`); `small` 13 sp sub-line; buttons 16 dp below: primary flex 1 + optional ghost flex 1.
 */
@Composable
fun EmptyBlock(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .padding(top = 18.dp)
            .fillMaxWidth()
            .insetOutline(colors.edge, 1.dp, shape)
            .padding(horizontal = 20.dp, vertical = 26.dp),
    ) {
        CivilightText(title, type.emptyTitle, color = colors.ink)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            CivilightText(subtitle, type.small.merge(TextStyle(fontSize = 13.sp)), color = colors.ink3)
        }
        if (primaryLabel != null && onPrimary != null) {
            // Flex 1 + flex 1 (DS-47) while both labels fit; stacked in a narrow pane so no label is cut (CNV-47).
            FittingButtonRow(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                CivilightButton(primaryLabel, onPrimary)
                if (secondaryLabel != null && onSecondary != null) {
                    CivilightButton(secondaryLabel, onSecondary, variant = ButtonVariant.GHOST)
                }
            }
        }
    }
}

/**
 * DS-47 no results (search): no box; title 800 22 / 30 (`결과 없음`) 30 dp below the chips, `small` 13 sp
 * [context]; then [similar] (a `sec` head and rows with search boxes) and an optional ghost action.
 */
@Composable
fun NoResults(
    title: String,
    modifier: Modifier = Modifier,
    context: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    similar: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(modifier.fillMaxWidth().padding(top = 30.dp)) {
        CivilightText(title, type.emptyTitle, color = colors.ink)
        if (context != null) CivilightText(context, type.small.merge(TextStyle(fontSize = 13.sp)), color = colors.ink3)
        if (similar != null) {
            Spacer(Modifier.height(24.dp))
            similar()
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            CivilightButton(actionLabel, onAction, variant = ButtonVariant.GHOST)
        }
    }
}

/**
 * DS-47 inline error banner (`ErrBackup`): padding 14 × 16, radius 16, `tint`, 1.5 dp inset and `Warn` 18 in
 * the error colour (`r18` as drawn, Q-DS-05), title 700 15 / 22, `small` sub-line, [actions] 10 dp below.
 */
@Composable
fun ErrorBanner(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    errorColor: Color = CivilightTheme.colors.r18,
    actions: (@Composable () -> Unit)? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.tint)
            .insetOutline(errorColor, 1.5.dp, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CivilightIcon(IconId.Warn, contentDescription = null, size = 18.dp, tint = errorColor)
            Column(Modifier.weight(1f)) {
                CivilightText(title, type.itemTitleCompact.merge(TextStyle(fontWeight = FontWeight.Bold)), color = colors.ink)
                if (subtitle != null) CivilightText(subtitle, type.small, color = colors.ink3)
            }
        }
        actions?.invoke()
    }
}

/**
 * DS-47 full-screen error (`ErrMic`): glass full screen, [icon] 28 in the error colour, `stateTitle`,
 * `small` 13 sp sub-line, buttons column: `primaryLarge` + ghost.
 */
@Composable
fun FullScreenError(
    icon: IconId,
    title: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    errorColor: Color = CivilightTheme.colors.r18,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Box(modifier.fillMaxSize()) {
        GlassBackground(GlassSurface.SHEET, Modifier.matchParentSize())
        Column(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CivilightIcon(icon, contentDescription = null, size = 28.dp, tint = errorColor)
            CivilightText(title, type.stateTitle, color = colors.ink, textAlign = TextAlign.Center)
            if (subtitle != null) {
                CivilightText(subtitle, type.small.merge(TextStyle(fontSize = 13.sp)), color = colors.ink3, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(14.dp))
            CivilightButton(primaryLabel, onPrimary, Modifier.fillMaxWidth(), variant = ButtonVariant.PRIMARY_LARGE)
            if (secondaryLabel != null && onSecondary != null) {
                CivilightButton(secondaryLabel, onSecondary, Modifier.fillMaxWidth(), variant = ButtonVariant.GHOST)
            }
        }
    }
}

/**
 * DS-47 loading rule: nothing for work under 1 s; after [delayMs] the [content] (a progress bar or the work
 * drawing itself) fades in on `standard` and out on `exit` (S11; 120 ms fades when reduced). The 1 s wait is
 * a perceived-time rule, not motion, so it does not follow the animator scale. No spinners or rings.
 */
@Composable
fun DelayedProgress(working: Boolean, modifier: Modifier = Modifier, delayMs: Long = 1000L, content: @Composable () -> Unit) {
    val motion = LocalMotion.current
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(working) {
        show = false
        if (working) {
            delay(delayMs)
            show = true
        }
    }
    AnimatedVisibility(
        visible = working && show,
        modifier = modifier,
        enter = fadeIn(motion.spec(CivilightMotion.standard)),
        exit = fadeOut(motion.spec(CivilightMotion.exit)),
    ) { content() }
}
