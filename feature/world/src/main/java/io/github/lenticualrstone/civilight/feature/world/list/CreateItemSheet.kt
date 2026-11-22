package io.github.lenticualrstone.civilight.feature.world.list

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.navigation.WorldTab
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** `+` on the 세계 header (WLD-11): create a term, place or faction of [tab]. Feature-local sheet. */
@Serializable
data class CreateItemSheet(val tab: WorldTab) : SheetKey

/**
 * `<종류> 만들기`: `이름` (required, NFC and trimmed by the domain), `설명` (optional), primary
 * `만들기`. [create] returns the new key (null: nothing made); the caller opens the new card.
 */
@Composable
fun SheetScope.CreateItemSheetContent(
    kind: WorldKind,
    readOnly: Boolean,
    create: suspend (name: String, description: String) -> Key?,
    onCreated: (Key) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    SheetHeader(title = WorldCopy.createLabel(kind), onClose = ::close)
    SheetScrollBody {
        CivilightTextField(
            value = name,
            onValueChange = { name = it },
            label = WorldCopy.NAME,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            enabled = !readOnly,
        )
        CivilightTextField(
            value = description,
            onValueChange = { description = it },
            label = WorldCopy.DESCRIPTION,
            singleLine = false,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            enabled = !readOnly,
        )
    }
    SheetActions {
        CivilightButton(
            text = if (readOnly) WorldCopy.READ_ONLY else WorldCopy.BTN_CREATE,
            onClick = {
                scope.launch {
                    val key = create(name, description) ?: return@launch
                    this@CreateItemSheetContent.close()
                    onCreated(key)
                }
            },
            modifier = Modifier.weight(1f),
            enabled = !readOnly && name.isNotBlank(),
        )
    }
}
