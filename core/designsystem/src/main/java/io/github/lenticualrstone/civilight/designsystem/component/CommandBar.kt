package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/**
 * DS-29 command bar, always inside the bottom glass bar: height 52, radius 14, `tint`, 1 dp `edge` inset,
 * gap 10, padding 0 × 14; leading `/` (`number` 13 sp `ink2`), one-line [hint] in the `command` role with
 * ellipsis, trailing mic (32 dp, touch 44, `라이프 레코더`). With [typedText] the hint is replaced by the
 * typed text in `ink` with a caret; with [onClose] the right end shows [closeLabel] instead of the mic.
 * Behaviour and hints belong to STO / LIB.
 */
@Composable
fun CommandBar(
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMic: (() -> Unit)? = null,
    micLabel: String = "라이프 레코더",
    typedText: String? = null,
    onClose: (() -> Unit)? = null,
    closeLabel: String = "닫기",
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressable(onClick = onClick, shape = shape, scaleOnPress = false, onClickLabel = hint)
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(colors.tint)
            .insetOutline(colors.edge, 1.dp, shape)
            .padding(start = 14.dp, end = if (onMic != null || onClose != null) 10.dp else 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivilightText("/", type.number.merge(TextStyle(fontSize = 13.sp)), color = colors.ink2, maxLines = 1)
        Box(Modifier.weight(1f)) {
            if (typedText != null) {
                DecoratedText(
                    AnnotatedString(typedText),
                    type.command,
                    color = colors.ink,
                    caretOffset = typedText.length,
                    caretHeight = 18.dp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                CivilightText(hint, type.command, color = colors.ink3, maxLines = 1)
            }
        }
        when {
            onClose != null -> TextAction(closeLabel, onClose, style = type.small)
            onMic != null -> MicButton(onClick = onMic, contentDescription = micLabel)
        }
    }
}
