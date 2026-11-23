package io.github.lenticualrstone.civilight.feature.library.shelf

import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.icon.vector
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.library.newproject.ProjectMenu
import io.github.lenticualrstone.civilight.domain.library.newproject.ProjectMenuAction
import io.github.lenticualrstone.civilight.domain.library.newproject.ProjectMenus
import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import io.github.lenticualrstone.civilight.ui.security.WindowProtection
import kotlinx.serialization.Serializable

/** 프로젝트 ⋯ (LIB-02, board `LibMenu`). */
@Serializable
data class ShelfMenuSheet(val projectKey: Key) : SheetKey

/** `이름 바꾸기` (LIB-02). */
@Serializable
data class RenameSheet(val projectKey: Key) : SheetKey

internal fun iconOf(action: ProjectMenuAction): ImageVector = when (action) {
    ProjectMenuAction.OPEN -> IconId.Book.vector
    ProjectMenuAction.RENAME -> IconId.Pencil.vector
    ProjectMenuAction.RATING -> IconId.Shield.vector
    ProjectMenuAction.HIDE -> IconId.EyeOff.vector
    ProjectMenuAction.UNHIDE -> IconId.Unlock.vector
    ProjectMenuAction.BACKUP_EXPORT -> IconId.Download.vector
    ProjectMenuAction.DELETE -> IconId.Trash.vector
    ProjectMenuAction.COVER -> IconId.Image.vector
}

/**
 * The ⋯ sheet: project name (`title`) + `닫기`, `<format> · 씬 <s>개 · 인물 <c> · 페이지 <p>`,
 * then the rows of [ProjectMenus] (the example shows only `열기`). Content of a protected project, so
 * the window is protected while it is open (LIB-26).
 */
@Composable
internal fun SheetScope.ProjectMenuContent(
    doc: ProjectDoc,
    isExample: Boolean,
    fileExists: (String) -> Boolean = { true },
    onAction: (ProjectMenuAction) -> Unit,
) {
    val scope = this
    val menu: ProjectMenu = ProjectMenus.of(doc, isExample, fileExists)
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    ProtectWindow(WindowProtection.forProject(doc.project.protections))
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        CivilightText(menu.title, type.title, Modifier.weight(1f), color = colors.ink, maxLines = 1)
        // DS-70 A6: the sheet head `닫기` is the `close` icon with its name.
        CivilightIconButton(IconButtonSpec.CLOSE, scope::close)
    }
    // (0.2.0, LIB-92) The sub line without a rating badge.
    BadgeLine(RatingLabels.effective(doc.project.rating), menu.sub, Modifier.padding(top = 2.dp))
    SheetScrollBody(Modifier.padding(top = 10.dp)) {
        menu.rows.forEach { row ->
            ListRow(
                title = row.title,
                subtitle = row.sub,
                leading = { CivilightIcon(iconOf(row.action), contentDescription = null, tint = colors.ink2) },
                onClick = { onAction(row.action) },
            )
        }
    }
}

/** `이름 바꾸기`: field `이름` (LIB-09 label); `바꾸기` stays off while the name is blank after trimming. */
@Composable
internal fun SheetScope.RenameContent(currentName: String, onRename: (String) -> Unit) {
    val scope = this
    var text by rememberSaveable { mutableStateOf(currentName) }
    val clean = ProjectMenus.rename(text)
    SheetHeader(title = ShelfCopy.RENAME_TITLE, onClose = scope::close, closeLabel = ShelfCopy.CLOSE)
    CivilightTextField(value = text, onValueChange = { text = it }, label = ShelfCopy.NAME_LABEL, modifier = Modifier.padding(top = 12.dp))
    SheetActions {
        CivilightButton(ShelfCopy.CANCEL, scope::close, Modifier.weight(1f), variant = ButtonVariant.GHOST)
        CivilightButton(
            ShelfCopy.RENAME_CONFIRM,
            onClick = {
                if (clean != null) {
                    onRename(clean)
                    scope.close()
                }
            },
            modifier = Modifier.weight(1f),
            enabled = clean != null && clean != currentName,
        )
    }
}
