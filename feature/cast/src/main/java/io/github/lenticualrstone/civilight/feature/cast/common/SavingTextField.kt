package io.github.lenticualrstone.civilight.feature.cast.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

/** Typing pause after which a settings text is written (as the archive editor does). */
const val TYPING_SAVE_DELAY_MS = 600L

/**
 * A settings text field that writes as you type: [onSave] runs after a pause in typing and once more
 * when the field leaves the screen, never with an unchanged text. The field holds its own text (never
 * re-seeded from the document while typing, so Hangul composition is safe); [key] resets it.
 */
@Composable
fun SavingTextField(
    initial: String,
    key: Any,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    hint: String? = null,
    enabled: Boolean = true,
) {
    var text by rememberSaveable(key) { mutableStateOf(initial) }
    var saved by rememberSaveable(key) { mutableStateOf(initial) }
    val save by rememberUpdatedState { value: String ->
        if (enabled && value != saved) {
            saved = value
            onSave(value)
        }
    }
    LaunchedEffect(key) {
        snapshotFlow { text }.drop(1).collectLatest { t ->
            delay(TYPING_SAVE_DELAY_MS)
            save(t)
        }
    }
    val latest by rememberUpdatedState(text)
    DisposableEffect(key) { onDispose { save(latest) } }
    CivilightTextField(text, { text = it }, modifier, label = label, placeholder = placeholder, hint = hint, enabled = enabled)
}
