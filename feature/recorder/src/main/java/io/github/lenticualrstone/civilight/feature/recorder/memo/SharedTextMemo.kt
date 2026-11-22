package io.github.lenticualrstone.civilight.feature.recorder.memo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureHubs
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureProtection
import io.github.lenticualrstone.civilight.feature.recorder.capture.EntryRoutes
import io.github.lenticualrstone.civilight.feature.recorder.capture.memoActions
import io.github.lenticualrstone.civilight.feature.recorder.link.LinkPickerContent
import io.github.lenticualrstone.civilight.model.recorder.EntryContext
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface
import io.github.lenticualrstone.civilight.navigation.RecorderLinkPickerSheet
import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.navigation.TextMemoSheet
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow

/**
 * The entry context of the shared `TextMemoSheet` (LRC-19): the card, scene or page named by
 * `targetKind` / `targetKey` (same strings as `Recording`), else the project on screen, else none.
 */
fun entryOf(sheet: TextMemoSheet): EntryContext =
    if (sheet.projectKey == null) EntryContext(surface = EntrySurface.LIBRARY)
    else EntryRoutes.entryOf(Recording(projectKey = sheet.projectKey, targetKind = sheet.targetKind, targetKey = sheet.targetKey))

/**
 * The shared text memo sheet (owner RECORDER, opened by other areas over their own screens). Same
 * content and rules as the memo of the `Recording` route; `+ 연결` stacks [RecorderLinkPickerSheet];
 * the mic hands the memo to the `Recording` route (LRC-16), which asks for the permission and
 * records. The draft is saved as it is typed, so a scrim tap or back keeps it (LRC-15).
 */
@Composable
fun SharedTextMemo(sheet: TextMemoSheet, scope: SheetScope, deps: FeatureDeps) {
    val hub = remember(deps) { CaptureHubs.of(deps) }
    val state by hub.state.collectAsState()
    val context = LocalContext.current
    val entry = remember(sheet) { entryOf(sheet) }
    LaunchedEffect(sheet) {
        val now = hub.state.value
        if (!hub.showMemo(entry)) {
            // A recording (or `정리 중`) is running: show it instead of a second capture (LRC-01).
            scope.close()
            deps.navigator.push(EntryRoutes.routeOf(now.session?.entry ?: entry))
        }
    }
    val actions = memoActions(
        hub = hub,
        context = context,
        onOpenPicker = { request ->
            hub.openPicker(request)
            scope.open(RecorderLinkPickerSheet())
        },
        onToVoice = {
            hub.requestVoiceFromMemo()
            scope.close()
            deps.navigator.push(EntryRoutes.routeOf(state.memo?.draft?.entry ?: entry))
        },
    ).let { base ->
        MemoActions(
            onTextChange = base.onTextChange, onCaret = base.onCaret, onPickAt = base.onPickAt, onOpenPicker = base.onOpenPicker,
            onRemoveLinks = base.onRemoveLinks, onHide = base.onHide, onToVoice = base.onToVoice,
            onSave = { hub.launch { if (memoSave()) scope.close() } },
            onClose = {
                hub.launch { memoClose() }
                scope.close()
            },
        )
    }
    // Dismissed by the host (scrim, drag, back) rather than `닫기` / `저장`: the next opening starts fresh.
    DisposableEffect(hub) { onDispose { hub.memoSheetGone() } }
    ProtectWindow(CaptureProtection.of(state.env, CaptureProtection.projectsShown(state)))
    with(scope) { TextMemoSheetContent(state, actions) }
}

/** The link picker stacked over the shared memo. */
@Composable
fun SharedLinkPicker(scope: SheetScope, deps: FeatureDeps) {
    val hub = remember(deps) { CaptureHubs.of(deps) }
    with(scope) {
        LinkPickerContent(hub, onClose = hub::closePicker)
    }
    // A pick or `닫기` ends the request; the sheet closes with it (also when restored without one).
    val state by hub.state.collectAsState()
    LaunchedEffect(state.picker) {
        if (state.picker == null) scope.close()
    }
    // Dismissed by the host (scrim, back): the request ends too, so the memo below is itself again.
    DisposableEffect(hub) { onDispose { hub.closePicker() } }
}
