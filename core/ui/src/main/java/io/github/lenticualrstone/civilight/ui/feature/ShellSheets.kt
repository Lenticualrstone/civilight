package io.github.lenticualrstone.civilight.ui.feature

import androidx.compose.runtime.remember
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.navigation.RichLinkPickerSheet
import io.github.lenticualrstone.civilight.ui.richtext.RichLinkPickerContent
import io.github.lenticualrstone.civilight.ui.lens.LensSheetFor
import io.github.lenticualrstone.civilight.ui.project.LocalProjectStore
import io.github.lenticualrstone.civilight.ui.scaffold.PlaceholderSheet

/** Registers the sheets the shell draws itself ([LensSheet], [RichLinkPickerSheet]). Called once by the app. */
fun SheetRegistry.registerShellSheets() {
    register<RichLinkPickerSheet> { sheet -> RichLinkPickerContent(sheet, this) }
    register<LensSheet> { sheet ->
        val deps = LocalFeatureDeps.current
        val store = LocalProjectStore.current?.takeIf { it.key == sheet.projectKey } ?: deps.projects.peek(sheet.projectKey)
        if (store == null) {
            PlaceholderSheet(sheet, this)
        } else {
            val controller = remember(store) { deps.lens(store) }
            LensSheetFor(store, controller)
        }
    }
}
