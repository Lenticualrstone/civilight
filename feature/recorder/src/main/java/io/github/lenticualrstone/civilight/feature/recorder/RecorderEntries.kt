@file:JvmName("RecorderEntries")

package io.github.lenticualrstone.civilight.feature.recorder

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.feature.recorder.capture.RecordingRoute
import io.github.lenticualrstone.civilight.feature.recorder.memo.SharedLinkPicker
import io.github.lenticualrstone.civilight.feature.recorder.memo.SharedTextMemo
import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.navigation.TextMemoSheet
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps

/**
 * Routes and shared sheets of the Life Recorder (shell.md §1). Batch 1 registers the capture
 * (`Recording`: 녹음 중 · 정리 중 · 마이크 꺼짐 · 글 메모) and the shared `TextMemoSheet`;
 * `RecorderInbox`, `RecorderRecordDetail` and `RecorderSettings` keep the shell placeholder until
 * their batches land. Only registrations here: the capture hub is made on first use in composition.
 */
fun EntryProviderScope<NavKey>.recorderEntries(deps: FeatureDeps) {
    entry<Recording> { route -> RecordingRoute(route, deps) }
    deps.sheets.register<TextMemoSheet> { sheet -> SharedTextMemo(sheet, this, deps) }
    deps.sheets.register<RecorderLinkPickerSheet> { _ -> SharedLinkPicker(this, deps) }
}
