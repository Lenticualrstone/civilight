package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.animation.core.Animatable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.tokenShadow
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LayoutMetrics
import io.github.lenticualrstone.civilight.designsystem.theme.OverlayGaps
import androidx.compose.ui.platform.LocalConfiguration

/**
 * A full-window dialog with the `shade` scrim drawn by us (the platform dim and window animations are turned
 * off). It opens with one progress on `enter`: the scrim fades in and the content rises 12 dp (S8; reduced:
 * fade only). Back and the scrim fade it out on `exit` first, then call [onDismissRequest]; so does a
 * `취소` in [ConfirmDialog]. Removing the dialog from composition is an instant close.
 */
@Composable
fun ScrimDialog(
    onDismissRequest: () -> Unit,
    contentAlignment: Alignment = Alignment.Center,
    dismissOnScrim: Boolean = true,
    content: @Composable () -> Unit,
) {
    val motion = LocalMotion.current
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val onDismiss by rememberUpdatedState(onDismissRequest)
    val closing = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { progress.animateTo(1f, motion.spec(CivilightMotion.enter)) }
    val animatedDismiss: () -> Unit = {
        if (!closing.value) {
            closing.value = true
            scope.launch {
                progress.animateTo(0f, motion.spec(CivilightMotion.exit))
                onDismiss()
                // Self-healing: a caller that keeps the dialog (a no-op or guarded dismiss) must not be left
                // with an invisible window that eats every touch, so fade back in if still composed.
                withFrameNanos { }
                withFrameNanos { }
                closing.value = false
                progress.animateTo(1f, motion.spec(CivilightMotion.enter))
            }
        }
    }
    Dialog(
        onDismissRequest = animatedDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window?.setDimAmount(0f)
            window?.setWindowAnimations(0)
        }
        val rise = with(LocalDensity.current) { DIALOG_RISE.toPx() }
        Box(Modifier.fillMaxSize(), contentAlignment = contentAlignment) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = progress.value }
                    .background(CivilightTheme.colors.shade)
                    .clearAndSetSemantics { }
                    .then(
                        if (dismissOnScrim) {
                            Modifier.clickable(remember { MutableInteractionSource() }, indication = null, onClick = animatedDismiss)
                        } else Modifier,
                    ),
            )
            // DS-66: a centred dialog sits in the band `[statusTop + 24, H − systemBottom − 24]`; a bottom dialog
            // places itself ([FloatingDialog]).
            val band = if (contentAlignment == Alignment.Center) {
                Modifier.padding(top = statusTop() + OverlayGaps.dialogBand, bottom = rememberSystemBottom() + OverlayGaps.dialogBand)
            } else Modifier
            Box(
                band.graphicsLayer {
                    val p = progress.value
                    alpha = p
                    translationY = if (motion.reduced) 0f else (1f - p) * rise
                },
                contentAlignment = contentAlignment,
            ) {
                CompositionLocalProvider(LocalDialogDismiss provides animatedDismiss, content = content)
            }
        }
    }
}

/** Window width from which [FloatingDialog] takes the inner-screen placement when not told. */
private const val INNER_MIN_WIDTH_DP = 600

/** How far a dialog's content rises while it opens (S8). */
private val DIALOG_RISE = 12.dp

/** The animated dismiss of the enclosing [ScrimDialog] (null outside one). */
val LocalDialogDismiss = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * DS-25 centred dialog (`ConfirmDelete`): 20 dp from the sides (max 420 dp), radius 26, padding 22, `raise`,
 * 1 dp `line` inset, `shadow-float`. Title in `dialogTitle`, then [facts] rows (`label` | `value`),
 * optional [body] (e.g. a `small` label + confirm-name field), then `취소` (ghost) and the confirm button,
 * 8 dp apart, flex 1 each. [hardToUndo] confirms use the `danger` button; every other confirm is primary.
 * The confirm label is a verb, never `확인` alone.
 */
@Composable
fun ConfirmDialog(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    facts: List<Pair<String, String>> = emptyList(),
    cancelLabel: String = "취소",
    hardToUndo: Boolean = false,
    confirmEnabled: Boolean = true,
    body: (@Composable ColumnScope.() -> Unit)? = null,
) {
    ScrimDialog(onDismissRequest) {
        DialogCard(title, modifier) {
            facts.forEach { (label, value) -> FieldTableRow(label = label, value = value) }
            body?.invoke(this)
            ButtonRow(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                val cancel = LocalDialogDismiss.current ?: onDismissRequest
                CivilightButton(cancelLabel, cancel, Modifier.weight(1f), variant = ButtonVariant.GHOST)
                CivilightButton(confirmLabel, onConfirm, Modifier.weight(1f), variant = confirmButtonVariant(hardToUndo), enabled = confirmEnabled)
            }
        }
    }
}

/** The card of [ConfirmDialog], usable inside other hosts. */
@Composable
fun DialogCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .widthIn(max = LayoutMetrics.dialogMaxWidth)
            .fillMaxWidth()
            .tokenShadow(shape, CivilightTheme.elevation.float(colors.isDark))
            .clip(shape)
            .background(colors.raise)
            .insetOutline(colors.line, 1.dp, shape)
            .padding(22.dp)
            .semantics { paneTitle = title },
    ) {
        CivilightText(title, CivilightTheme.type.dialogTitle, color = colors.ink)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

/**
 * DS-25 bottom floating dialog (`ErrBio`, system-auth style): 12 dp sides on cover (centred, max 420 on the
 * inner screen, [inner] defaults to a window 600 dp or wider), `systemBottom + 16` (inner 24; 8 over the
 * keyboard) above the window bottom (DS-66), radius 28, padding 26 · 22 · 18, `raise`, `line` inset, shadow. Centred column, 8 dp gaps:
 * `small` [header], title 19 / 26 700, `small` 13 sp [subtitle], [icon] 48 `ink3`, buttons ghost + primary.
 */
@Composable
fun FloatingDialog(
    title: String,
    onDismissRequest: () -> Unit,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    header: String? = null,
    subtitle: String? = null,
    icon: IconId? = null,
    cancelLabel: String = "취소",
    inner: Boolean? = null,
) {
    val wide = inner ?: (LocalConfiguration.current.screenWidthDp >= INNER_MIN_WIDTH_DP)
    // The activity's own bottom inset, in case the dialog window reports none.
    val outside = systemBottomWithoutKeyboard()
    ScrimDialog(onDismissRequest, contentAlignment = Alignment.BottomCenter) {
        // DS-66: `systemBottom + edge` (16 cover, 24 inner) above the window bottom, `+ 8` over the keyboard.
        val y = OverlayGaps.floatingDialogY(wide, maxOf(outside, rememberSystemBottom()).value, keyboardUp())
        FloatingDialogCard(
            title = title,
            onCancel = onDismissRequest,
            primaryLabel = primaryLabel,
            onPrimary = onPrimary,
            modifier = modifier
                .then(
                    if (wide) Modifier.widthIn(max = OverlayGaps.floatingDialogMaxWidthInner) else Modifier.padding(horizontal = OverlayGaps.sideCompact),
                )
                .padding(bottom = y.dp),
            header = header,
            subtitle = subtitle,
            icon = icon,
            cancelLabel = cancelLabel,
        )
    }
}

/** The card of [FloatingDialog], usable inside other hosts. */
@Composable
fun FloatingDialogCard(
    title: String,
    onCancel: () -> Unit,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    header: String? = null,
    subtitle: String? = null,
    icon: IconId? = null,
    cancelLabel: String = "취소",
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .tokenShadow(shape, CivilightTheme.elevation.float(colors.isDark))
            .clip(shape)
            .background(colors.raise)
            .insetOutline(colors.line, 1.dp, shape)
            .padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 18.dp)
            .semantics { paneTitle = title },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (header != null) CivilightText(header, type.small, color = colors.ink3)
        CivilightText(
            title,
            type.headerTitle.merge(TextStyle(fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold)),
            color = colors.ink,
            textAlign = TextAlign.Center,
        )
        if (subtitle != null) {
            CivilightText(subtitle, type.small.merge(TextStyle(fontSize = 13.sp)), color = colors.ink3, textAlign = TextAlign.Center)
        }
        if (icon != null) CivilightIcon(icon, contentDescription = null, size = 48.dp, tint = colors.ink3)
        ButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            CivilightButton(cancelLabel, onCancel, Modifier.weight(1f), variant = ButtonVariant.GHOST)
            CivilightButton(primaryLabel, onPrimary, Modifier.weight(1f), variant = ButtonVariant.PRIMARY)
        }
    }
}
