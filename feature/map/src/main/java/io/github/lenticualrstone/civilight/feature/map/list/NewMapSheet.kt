package io.github.lenticualrstone.civilight.feature.map.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonRow
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.selectedRowBackground
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.SheetKey
import kotlinx.serialization.Serializable

/**
 * `새 지도 · 닫기` (WLD-36, board `MapNew`). Feature-local; a pin's `하위 지도 · 만들기` (WLD-48)
 * opens it with [parentMapKey] and [parentPinKey], which shows the `상위` line.
 */
@Serializable
data class NewMapSheet(val parentMapKey: Key? = null, val parentPinKey: Key? = null) : SheetKey

/** The four bases of a new map, in board order. */
enum class NewMapOption(val title: String, val sub: String, val icon: IconId) {
    GENERATED(MapCopy.NEW_GENERATED, MapCopy.NEW_GENERATED_SUB, IconId.Dice),
    IMAGE(MapCopy.NEW_IMAGE, MapCopy.NEW_IMAGE_SUB, IconId.Image),
    SKETCH(MapCopy.NEW_SKETCH, MapCopy.NEW_SKETCH_SUB, IconId.Camera),
    NODE(MapCopy.NEW_NODE, MapCopy.NEW_NODE_SUB, IconId.Node),
}

/** What the sheet asks the screen to do. */
sealed interface NewMapAction {
    data object Generate : NewMapAction
    data object ImageFromGallery : NewMapAction
    data object ImageFromFile : NewMapAction
    data object SketchShoot : NewMapAction
    data object SketchFromGallery : NewMapAction
    data object Node : NewMapAction
}

/**
 * The sheet body: one row per base. `지형 만들기` and `노드 지도` act at once; `이미지 가져오기` and
 * `스케치 촬영` open their source buttons (`갤러리 · 파일`, `촬영 · 갤러리`) under the row.
 */
@Composable
fun ColumnScope.NewMapSheetContent(
    parent: ParentUi?,
    readOnly: Boolean,
    onAction: (NewMapAction) -> Unit,
    onClose: () -> Unit,
) {
    var open by rememberSaveable { mutableStateOf<NewMapOption?>(null) }
    SheetHeader(MapCopy.NEW_MAP, onClose = onClose)
    SheetScrollBody {
        if (parent != null) {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CivilightText(MapCopy.PARENT, CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
                io.github.lenticualrstone.civilight.designsystem.component.MapValueChip(parent.tag)
                CivilightText(MapCopy.TO_CHILD, CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
            }
        }
        Column(Modifier.padding(top = 10.dp)) {
            NewMapOption.entries.forEach { option ->
                OptionRow(
                    option = option,
                    selected = open == option,
                    enabled = !readOnly,
                    onClick = {
                        when (option) {
                            NewMapOption.GENERATED -> onAction(NewMapAction.Generate)
                            NewMapOption.NODE -> onAction(NewMapAction.Node)
                            else -> open = if (open == option) null else option
                        }
                    },
                )
                if (open == option && !readOnly) {
                    ButtonRow(Modifier.padding(start = 54.dp, bottom = 12.dp)) {
                        when (option) {
                            NewMapOption.IMAGE -> {
                                CivilightButton(MapCopy.BTN_GALLERY, { onAction(NewMapAction.ImageFromGallery) }, variant = ButtonVariant.COMPACT_PRIMARY, icon = IconId.Image)
                                CivilightButton(MapCopy.BTN_FILE, { onAction(NewMapAction.ImageFromFile) }, variant = ButtonVariant.COMPACT_GHOST, icon = IconId.File)
                            }
                            NewMapOption.SKETCH -> {
                                CivilightButton(MapCopy.BTN_SHOOT, { onAction(NewMapAction.SketchShoot) }, variant = ButtonVariant.COMPACT_PRIMARY, icon = IconId.Camera)
                                CivilightButton(MapCopy.BTN_GALLERY, { onAction(NewMapAction.SketchFromGallery) }, variant = ButtonVariant.COMPACT_GHOST, icon = IconId.Image)
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
        if (readOnly) {
            CivilightText(MapCopy.READ_ONLY, CivilightTheme.type.small, Modifier.padding(vertical = 12.dp), color = CivilightTheme.colors.ink3)
        }
    }
}

@Composable
private fun OptionRow(option: NewMapOption, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .selectedRowBackground(selected, colors.sel, colors.acc)
            .selectable(selected = selected, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.tint), contentAlignment = Alignment.Center) {
            CivilightIcon(option.icon, null, size = 20.dp, tint = if (selected) colors.acc else colors.ink2)
        }
        Column(Modifier.weight(1f)) {
            CivilightText(option.title, type.itemTitle, color = if (enabled) colors.ink else colors.ink3, maxLines = 1)
            CivilightText(option.sub, type.small, color = colors.ink3, maxLines = 1)
        }
    }
}
