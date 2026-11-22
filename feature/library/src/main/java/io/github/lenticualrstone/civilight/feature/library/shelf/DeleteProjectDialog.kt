package io.github.lenticualrstone.civilight.feature.library.shelf

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.ConfirmDialog
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.library.newproject.ProjectDeleteDialog
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import io.github.lenticualrstone.civilight.ui.security.WindowProtection

/**
 * `‘<name>’ 지우기` (LIB-03, board `ConfirmDelete`): counts, `이동 · 휴지통 · 30일 보관`, the name to type
 * and `휴지통으로` in `r18` (the one destructive fill), enabled only on an NFC-equal name.
 */
@Composable
internal fun DeleteProjectDialog(dialog: ProjectDeleteDialog, protections: Protections, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    var typed by rememberSaveable { mutableStateOf("") }
    val (cancel, confirm) = dialog.buttons
    ProtectWindow(WindowProtection.forProject(protections))
    ConfirmDialog(
        title = dialog.title,
        confirmLabel = confirm,
        cancelLabel = cancel,
        onConfirm = onConfirm,
        onDismissRequest = onDismiss,
        facts = dialog.rows,
        hardToUndo = true,
        confirmEnabled = dialog.canConfirm(typed),
        body = {
            CivilightText(dialog.fieldLabel, CivilightTheme.type.small, Modifier.padding(top = 14.dp), color = CivilightTheme.colors.ink3)
            CivilightTextField(value = typed, onValueChange = { typed = it }, modifier = Modifier.padding(top = 6.dp))
        },
    )
}
