package io.github.lenticualrstone.civilight.ui.richtext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.lenticualrstone.civilight.navigation.RichLinkPickerSheet
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.picker.LinkPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.PickKind
import io.github.lenticualrstone.civilight.ui.picker.PickerModels
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.LocalProjectStore
import io.github.lenticualrstone.civilight.ui.scaffold.PlaceholderSheet

/** The link kinds a [RichLinkPickerSheet] offers (unknown names are skipped; none = 인물 · 용어 · 장소). */
fun RichLinkPickerSheet.pickKinds(): Set<PickKind> =
    kinds.mapNotNull { name -> PickKind.entries.firstOrNull { it.name == name } }.toSet().ifEmpty { PickerModels.SCRIPT_LINK_KINDS }

/**
 * Content of [RichLinkPickerSheet] (registered by the shell): the shared `@` picker over the
 * project's people, terms, places (and factions when asked), with the hidden row. A pick is sent as
 * a [LinkPick] under the sheet's request key and the sheet closes.
 */
@Composable
fun RichLinkPickerContent(sheet: RichLinkPickerSheet, scope: SheetScope) {
    val deps = LocalFeatureDeps.current
    val store = LocalProjectStore.current?.takeIf { it.key == sheet.projectKey } ?: deps.projects.peek(sheet.projectKey)
    if (store == null) {
        PlaceholderSheet(sheet, scope)
        return
    }
    val doc by store.doc.collectAsState()
    val gate = rememberHiddenGate(sheet.projectKey)
    scope.LinkPickerSheet(
        doc = doc,
        gate = gate,
        title = sheet.title,
        onPick = { kind, key, text ->
            deps.results.send(sheet.requestKey, LinkPick(kind, key, text))
            scope.close()
        },
        onClose = scope::close,
        kinds = sheet.pickKinds(),
    )
}
