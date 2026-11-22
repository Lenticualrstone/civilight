package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.Motion
import io.github.lenticualrstone.civilight.designsystem.theme.rememberLastNonNull

/**
 * DS-27 toast: radius 16, padding 6 · 6 · 6 · 16, `ink` fill, message 14 / 20 500 in `bg` (up to two lines),
 * one action button with `bg` fill and `ink` text (`Undo` 16 + `되돌리기`), gap 10.
 * Messages and timing are LIB-40's.
 */
@Composable
fun CivilightToast(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = "되돌리기",
    onAction: (() -> Unit)? = null,
    actionIcon: IconId? = IconId.Undo,
) {
    val colors = CivilightTheme.colors
    val style = toastStyle(colors)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(style.fill)
            .padding(start = 16.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivilightText(
            message,
            CivilightTheme.type.command.merge(TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)),
            Modifier.weight(1f),
            color = style.message,
            maxLines = 2,
        )
        if (actionLabel != null && onAction != null) {
            CivilightButton(actionLabel, onAction, variant = ButtonVariant.ON_TOAST, icon = actionIcon)
        }
    }
}

/**
 * Shows [message] as a toast while non-null: rises 8 dp and fades in with `Enter`, fades out with `Exit`
 * (reduced motion: 120 ms fades). A new toast while one shows replaces it (LIB-40): a new [id] crossfades the
 * card ([Motion.swap], S10) and its size follows on `standard`; the same id with new text updates in place.
 * [id] defaults to the message. Place it 16 dp from the sides and 6 dp above the bottom glass bar (e.g. in
 * [GlassScaffold]'s overlay with its bottom padding).
 */
@Composable
fun ToastHost(
    message: String?,
    modifier: Modifier = Modifier,
    actionLabel: String? = "되돌리기",
    onAction: (() -> Unit)? = null,
    id: Any? = message,
) {
    val motion = LocalMotion.current
    val shown = rememberLastNonNull(message?.let { ShownToast(id ?: it, it, actionLabel, onAction) })
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = motion.enter(CivilightMotion.enter, rise = 8.dp),
        exit = motion.exit(CivilightMotion.exit),
        label = "toast",
    ) {
        if (shown != null) {
            AnimatedContent(
                targetState = shown,
                transitionSpec = { motion.swap(enter = CivilightMotion.enter) },
                contentKey = { it.id },
                label = "toastSwap",
            ) { t ->
                CivilightToast(
                    t.message,
                    Modifier.animateContentSize(motion.move(CivilightMotion.standard)),
                    actionLabel = t.actionLabel,
                    onAction = t.onAction,
                )
            }
        }
    }
}

private data class ShownToast(val id: Any, val message: String, val actionLabel: String?, val onAction: (() -> Unit)?)
