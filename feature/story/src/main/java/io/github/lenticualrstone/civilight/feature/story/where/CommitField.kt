package io.github.lenticualrstone.civilight.feature.story.where

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/**
 * A short number field (clock, story day, band start) that commits its draft only on the keyboard's
 * Done or when it loses focus, never per keystroke: a half-typed `2` (from `20`) or `0:00` (from
 * `20:00`) must not be stored, nor open the cascade preview while typing. The draft follows [seed]
 * when the stored value changes.
 */
@Composable
fun CommitField(
    seed: String,
    onCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    unit: String? = null,
    placeholder: String? = null,
    textStyle: TextStyle = CivilightTheme.type.number,
) {
    var draft by rememberSaveable(seed) { mutableStateOf(seed) }
    var focused by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    fun commit() {
        if (draft != seed) onCommit(draft)
    }
    CivilightTextField(
        value = draft,
        onValueChange = { draft = it },
        modifier = modifier.onFocusChanged { state ->
            if (focused && !state.isFocused) commit()
            focused = state.isFocused
        },
        unit = unit,
        placeholder = placeholder,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            commit()
            focus.clearFocus()
        }),
        textStyle = textStyle,
    )
}
