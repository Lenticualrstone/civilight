package io.github.lenticualrstone.civilight.feature.cast.list

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import kotlinx.serialization.Serializable

/** `인물 추가` (CST-05) over the list; feature-local. */
@Serializable
data class NewCharacterSheet(val projectKey: Key) : SheetKey

/** CST-05 [PROPOSED] sheet: title `인물 추가`, one `이름` field, primary `넣기`. */
@Composable
fun SheetScope.NewCharacterSheetContent(onCreate: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    SheetHeader(CastUiCopy.NEW_CHARACTER, onClose = ::close, closeLabel = CastUiCopy.BTN_CLOSE)
    CivilightTextField(
        value = name,
        onValueChange = { name = it },
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp).focusRequester(focus),
        label = CastUiCopy.NAME,
        keyboardActions = KeyboardActions(onDone = { if (NewCharacter.canCreate(name)) onCreate(name) }),
    )
    SheetActions {
        CivilightButton(CastUiCopy.BTN_INSERT, { onCreate(name) }, Modifier.weight(1f), enabled = NewCharacter.canCreate(name))
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}
