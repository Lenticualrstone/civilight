package io.github.lenticualrstone.civilight.designsystem.component

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
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

/**
 * A full-window dialog with the `shade` scrim drawn by us (the platform dim is turned off). Back and the
 * scrim call [onDismissRequest].
 */
@Composable
fun ScrimDialog(
    onDismissRequest: () -> Unit,
    contentAlignment: Alignment = Alignment.Center,
    dismissOnScrim: Boolean = true,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window?.setDimAmount(0f)
        }
        Box(Modifier.fillMaxSize(), contentAlignment = contentAlignment) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(CivilightTheme.colors.shade)
                    .clearAndSetSemantics { }
                    .then(
                        if (dismissOnScrim) {
                            Modifier.clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismissRequest)
                        } else Modifier,
                    ),
            )
            content()
        }
    }
}

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
                CivilightButton(cancelLabel, onDismissRequest, Modifier.weight(1f), variant = ButtonVariant.GHOST)
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
 * DS-25 bottom floating dialog (`ErrBio`, system-auth style): 12 dp sides, 16 dp above the navigation
 * inset, radius 28, padding 26 · 22 · 18, `raise`, `line` inset, shadow. Centred column, 8 dp gaps:
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
) {
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    ScrimDialog(onDismissRequest, contentAlignment = Alignment.BottomCenter) {
        FloatingDialogCard(
            title = title,
            onCancel = onDismissRequest,
            primaryLabel = primaryLabel,
            onPrimary = onPrimary,
            modifier = modifier.padding(start = 12.dp, end = 12.dp, bottom = 16.dp + nav),
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
