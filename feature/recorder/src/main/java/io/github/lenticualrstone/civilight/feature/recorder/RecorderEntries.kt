@file:JvmName("RecorderEntries")

package io.github.lenticualrstone.civilight.feature.recorder

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.feature.recorder.capture.RecordingRoute
import io.github.lenticualrstone.civilight.feature.recorder.capture.RecoveryHook
import io.github.lenticualrstone.civilight.feature.recorder.inbox.InboxScreen
import io.github.lenticualrstone.civilight.feature.recorder.memo.SharedLinkPicker
import io.github.lenticualrstone.civilight.feature.recorder.memo.SharedTextMemo
import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.navigation.RecorderInbox
import io.github.lenticualrstone.civilight.navigation.RecorderSettings
import io.github.lenticualrstone.civilight.feature.recorder.settings.RecorderSettingsContent
import io.github.lenticualrstone.civilight.navigation.RecorderRecordDetail
import io.github.lenticualrstone.civilight.feature.recorder.record.RecordDetailScreen
import io.github.lenticualrstone.civilight.navigation.RecorderLinkPickerSheet
import io.github.lenticualrstone.civilight.navigation.TextMemoSheet
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps

/**
 * Routes, shared sheets and the start-up hook of the Life Recorder (shell.md §1):
 * - `Recording` (녹음 중 · 정리 중 · 마이크 꺼짐 · 글 메모), `RecorderInbox` (기록함) and
 *   `RecorderRecordDetail` (기록 화면), their sheets feature-local, and `RecorderSettings`
 *   (설정 · 라이프 레코더, LRC-44);
 * - the shared `TextMemoSheet` and `RecorderLinkPickerSheet`;
 * - `recorder.resume` (LRC-09 recovery), registered once per process.
 *
 * Only registrations here: the capture hub is made on first use (screen, sheet or hook).
 */
fun EntryProviderScope<NavKey>.recorderEntries(deps: FeatureDeps) {
    entry<Recording> { route -> RecordingRoute(route, deps) }
    entry<RecorderInbox> { route -> InboxScreen(route, deps) }
    entry<RecorderRecordDetail> { route -> RecordDetailScreen(route, deps) }
    entry<RecorderSettings> { RecorderSettingsContent(deps) }
    deps.sheets.register<TextMemoSheet> { sheet -> SharedTextMemo(sheet, this, deps) }
    deps.sheets.register<RecorderLinkPickerSheet> { _ -> SharedLinkPicker(this, deps) }
    if (!deps.startup.isRegistered(RecoveryHook.ID)) deps.startup.register(RecoveryHook.ID, RecoveryHook)
}
