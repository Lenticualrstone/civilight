package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.ui.graphics.graphicsLayer
import io.github.lenticualrstone.civilight.designsystem.foundation.animatedInsetOutline
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.designsystem.theme.animatedAlpha
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/** DS-35 field outline: unfocused 1 dp `edge`, focused 1.5 dp `acc`, error 1.5 dp `r18` (proposed). */
data class FieldOutline(val color: Color, val width: Dp)

fun fieldOutline(focused: Boolean, error: Boolean, colors: CivilightColors): FieldOutline = when {
    error -> FieldOutline(colors.r18, 1.5.dp)
    focused -> FieldOutline(colors.acc, 1.5.dp)
    else -> FieldOutline(colors.edge, 1.dp)
}

/**
 * DS-35 text field: optional [label] above (`label` role `ink2`, 6 dp gap), a 46 dp `tint` field with radius
 * 12 and padding 0 × 12, value in the `field` role, `acc` caret, optional [unit] suffix (`small` `ink3`, never
 * converted), optional [hint] or [error] line below. [placeholder] is a format hint only.
 */
@Composable
fun CivilightTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    unit: String? = null,
    hint: String? = null,
    error: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textStyle: TextStyle = CivilightTheme.type.field,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val outline = fieldOutline(focused, error != null, colors)
    val outlineColor = animatedColor(outline.color, label = "fieldOutline")
    val outlineWidth = animatedAlpha(outline.width.value, label = "fieldOutlineWidth")
    val enabledAlpha = animatedAlpha(if (enabled) 1f else DISABLED_ALPHA, label = "enabled")
    val shape = RoundedCornerShape(12.dp)
    // U-02: a multi-line field keeps its caret line in view while it grows.
    val caret = rememberCaretFollow(value, focused, enabled = !singleLine && enabled) {
        if (visualTransformation === VisualTransformation.None) it.selection.end
        else visualTransformation.filter(AnnotatedString(it.text)).offsetMapping.originalToTransformed(it.selection.end)
    }
    Column(modifier.graphicsLayer { alpha = enabledAlpha.value }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) CivilightText(label, type.label, color = colors.ink2)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            textStyle = textStyle.merge(TextStyle(color = colors.ink)),
            cursorBrush = SolidColor(colors.acc),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = source,
            modifier = Modifier.fillMaxWidth(),
            onTextLayout = caret::onTextLayout,
            decorationBox = { inner ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                        .clip(shape)
                        .background(colors.tint)
                        .animatedInsetOutline(outlineColor, { outlineWidth.value.dp }, shape)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
                        if (value.text.isEmpty() && placeholder != null) {
                            CivilightText(placeholder, textStyle, color = colors.ink3, maxLines = 1)
                        }
                        Box(Modifier.caretFollow(caret)) { inner() }
                    }
                    if (unit != null) CivilightText(unit, type.small, color = colors.ink3, maxLines = 1)
                }
            },
        )
        // Error and hint lines fold in and out (S6); the text colour crosses over between hint and error.
        FoldRevealValue(error ?: hint, label = "fieldLine") { line ->
            val lineColor by animatedColor(if (error != null) colors.r18 else colors.ink3, label = "fieldLineColor")
            CivilightText(line, type.small, color = lineColor)
        }
    }
}

/** String convenience overload of [CivilightTextField]. */
@Composable
fun CivilightTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    unit: String? = null,
    hint: String? = null,
    error: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textStyle: TextStyle = CivilightTheme.type.field,
) {
    var held by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    val current = if (held.text == value) held else held.copy(text = value, selection = TextRange(value.length))
    CivilightTextField(
        value = current,
        onValueChange = {
            held = it
            if (it.text != value) onValueChange(it.text)
        },
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        unit = unit,
        hint = hint,
        error = error,
        singleLine = singleLine,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        textStyle = textStyle,
    )
}

/** DS-35 password field (`복구 암호`): dots `•` in 18 / 20 with 0.18 em tracking and `tnum`. */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    hint: String? = null,
    error: String? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    CivilightTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        hint = hint,
        error = error,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        keyboardActions = keyboardActions,
        visualTransformation = PasswordVisualTransformation('•'),
        textStyle = CivilightTheme.type.number.merge(TextStyle(fontSize = 18.sp, lineHeight = 20.sp, letterSpacing = 0.18.em)),
    )
}

/**
 * DS-35 search field: the command-bar shape (52 tall, radius 14, `tint`, 1 dp `edge`) with `Search` 18 at
 * the left, typed text in `ink` with the `acc` caret, and a right-aligned `small` 11 sp [rightHint]
 * (`초성 검색`), or a [closeLabel] text button (`닫기`) when [onClose] is set.
 */
@Composable
fun SearchField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    rightHint: String? = null,
    onClose: (() -> Unit)? = null,
    closeLabel: String = "닫기",
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(14.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = type.command.merge(TextStyle(color = colors.ink, fontSize = 15.sp)),
        cursorBrush = SolidColor(colors.acc),
        keyboardOptions = KeyboardOptions.Default,
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(shape)
                    .background(colors.tint)
                    .insetOutline(colors.edge, 1.dp, shape)
                    .padding(start = 14.dp, end = if (onClose != null) 4.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CivilightIcon(IconId.Search, contentDescription = null, size = 18.dp, tint = colors.ink3)
                Box(Modifier.weight(1f)) {
                    if (value.text.isEmpty() && placeholder != null) {
                        CivilightText(placeholder, type.command, color = colors.ink3, maxLines = 1)
                    }
                    inner()
                }
                when {
                    onClose != null -> TextAction(closeLabel, onClose)
                    rightHint != null -> CivilightText(rightHint, type.small.merge(TextStyle(fontSize = 11.sp)), color = colors.ink3, maxLines = 1)
                }
            }
        },
    )
}
