package io.github.lenticualrstone.civilight.feature.library.newproject

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.domain.cast.field.FieldBundles
import io.github.lenticualrstone.civilight.domain.library.masking.ProjectProfile
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.project.FieldBundle
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import io.github.lenticualrstone.civilight.ui.security.WindowProtection
import kotlinx.serialization.Serializable

/** `인물 항목` bundle picker of step 4 (LIB-12). */
@Serializable
data object BundlePickerSheet : SheetKey

/**
 * `기본` (default, never guessed from 형식), `게임 프로필`, `비어 있음`, `다른 프로젝트에서` (other
 * projects; hidden ones only while the group is open, P-05; copies the field set only). Each built-in
 * bundle's sub lists its field names.
 */
@Composable
internal fun SheetScope.BundlePickerContent(
    form: NewProjectForm,
    sources: List<ProjectProfile>,
    onPick: (FieldBundle, Key?) -> Unit,
) {
    val scope = this
    var showSources by rememberSaveable { mutableStateOf(form.bundle == FieldBundle.FROM_PROJECT) }
    SheetHeader(title = FieldBundles.ROW_TITLE, onClose = scope::close, closeLabel = NewProjectCopy.CLOSE)
    SheetScrollBody(Modifier.padding(top = 8.dp)) {
        FieldBundles.PICKER.forEach { bundle ->
            val names = FieldBundles.fields(bundle).joinToString(" · ") { it.field.name }
            RadioRow(
                title = FieldBundles.label(bundle),
                selected = if (bundle == FieldBundle.FROM_PROJECT) showSources else form.bundle == bundle && !showSources,
                onClick = {
                    if (bundle == FieldBundle.FROM_PROJECT) {
                        showSources = true
                    } else {
                        onPick(bundle, null)
                        scope.close()
                    }
                },
                subtitle = names.ifEmpty { null },
                enabled = bundle != FieldBundle.FROM_PROJECT || sources.isNotEmpty(),
            )
        }
        if (showSources) {
            sources.forEach { project ->
                if (project.hidden) ProtectWindow(WindowProtection.forProject(project.protections))
                RadioRow(
                    title = project.name,
                    selected = form.bundle == FieldBundle.FROM_PROJECT && form.bundleSourceKey == project.key,
                    onClick = {
                        onPick(FieldBundle.FROM_PROJECT, project.key)
                        scope.close()
                    },
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
    }
}
