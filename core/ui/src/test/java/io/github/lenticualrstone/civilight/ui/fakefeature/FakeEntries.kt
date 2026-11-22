@file:JvmName("FakeEntries")

package io.github.lenticualrstone.civilight.ui.fakefeature

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.navigation.SceneSplitSheet
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.picker.CharacterPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import kotlinx.serialization.Serializable

/** A feature-local sheet key, as a feature module would declare one. */
@Serializable
data class FakeSpeakerSheet(val blockKey: String) : SheetKey

/**
 * A feature written exactly as the shell.md contract says; compiled and called by
 * `FeatureContractTest` to prove the contract works from a feature's side.
 */
fun EntryProviderScope<NavKey>.fakeEntries(deps: FeatureDeps) {
    entry<Script>(metadata = mapOf(MARK to true)) { route ->
        val store = currentProjectStore
        val doc by store.doc.collectAsState()
        val sheets = rememberSheetHost()
        CivilightScreen(
            title = doc.project.name,
            sheets = sheets,
            localSheets = { key ->
                when (key) {
                    is FakeSpeakerSheet -> {
                        {
                            CharacterPickerSheet(
                                doc = doc,
                                gate = rememberHiddenGate(route.projectKey),
                                onPick = { close() },
                                onClose = ::close,
                            )
                        }
                    }
                    else -> null
                }
            },
        ) { }
    }
    deps.sheets.register<SceneSplitSheet> { sheet ->
        SheetHeader(title = sheet.blockKey, onClose = ::close)
        SheetScrollBody { }
    }
}

const val MARK = "fake.feature"
