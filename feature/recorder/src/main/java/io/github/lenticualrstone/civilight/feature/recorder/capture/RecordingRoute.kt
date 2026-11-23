package io.github.lenticualrstone.civilight.feature.recorder.capture

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.lenticualrstone.civilight.feature.recorder.outside.AndroidNetworkStatus
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.lenticualrstone.civilight.designsystem.component.CivilightSheetLayout
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.rememberLastNonNull
import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.ShownCopy
import io.github.lenticualrstone.civilight.feature.recorder.link.LinkPickerContent
import io.github.lenticualrstone.civilight.feature.recorder.memo.MemoActions
import io.github.lenticualrstone.civilight.feature.recorder.memo.TextMemoSheetContent
import io.github.lenticualrstone.civilight.feature.recorder.outside.AndroidAudioFocus
import io.github.lenticualrstone.civilight.feature.recorder.outside.DeviceUnlock
import io.github.lenticualrstone.civilight.feature.recorder.outside.KeepAlive
import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import io.github.lenticualrstone.civilight.ui.tour.TourHold
import io.github.lenticualrstone.civilight.ui.tour.tourRunning
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import io.github.lenticualrstone.civilight.feature.recorder.RecorderUiCopy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** The activity behind [this] context (the device-unlock request needs it). */
tailrec fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Whether the app may record audio now. */
fun micGranted(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

/**
 * The `Recording` overlay route (LRC-01 … LRC-20): `녹음 중`, `정리 중`, `마이크 꺼짐`, the reason
 * line, the text memo (`startTyped`) and the link picker, over the screen the user was on (the
 * shell keeps it composed underneath, blurred).
 *
 * The start is consumed once ([rememberSaveable]): a back stack restored after process death never
 * turns the mic on by itself; it closes when no capture is running. The route closes when the
 * capture goes idle. Leaving `녹음 중` inside the app (system back, another screen) saves the take as
 * `정리 안 됨` and releases the microphone at once ([CaptureHub.leave], U-13); the app going to the
 * background keeps a running take (LRC-09, the service's notification). Elsewhere back acts like the
 * screen's own close.
 */
@Composable
fun RecordingRoute(route: Recording, deps: FeatureDeps) {
    val hub = remember(deps) { CaptureHubs.of(deps) }
    val state by hub.state.collectAsState()
    val context = LocalContext.current
    var consumed by rememberSaveable { mutableStateOf(false) }
    val close = {
        if (deps.navigator.current == route) deps.navigator.pop()
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hub.launch { permissionResult(granted) }
    }

    // Platform parts that need a context: audio focus (LRC-09) and the background keep-alive service.
    LaunchedEffect(hub) {
        hub.usePlatformFocus(AndroidAudioFocus(context))
        hub.useNetwork(AndroidNetworkStatus(context))
        KeepAlive.attach(context, hub, deps.appScope)
    }

    // Lock-screen capture (LRC-05): the device unlock before recording or before the link picker.
    LaunchedEffect(state.unlock) {
        if (state.unlock == null) return@LaunchedEffect
        val activity = context.findActivity()
        if (activity == null) hub.unlockResult(false) else DeviceUnlock.request(activity, hub::unlockResult)
    }

    LaunchedEffect(Unit) {
        if (!consumed) {
            hub.begin(EntryRoutes.entryOf(route), route.startTyped, micGranted(context))
            consumed = true
        }
        hub.state.first { !it.active }
        close()
    }

    // `마이크 꺼짐`: back from the system settings with the permission granted closes the screen; recording does not start by itself (LRC-06).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && hub.state.value.stage == CaptureStage.MIC_OFF && micGranted(context)) hub.dismissMic()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // U-13: leaving the recorder saves the take (`정리 안 됨`) and releases the microphone before anything else runs.
    val leaveNow: () -> Unit = {
        hub.leave()?.let { save ->
            deps.appScope.launch {
                save.join()
                if (!save.isCancelled) deps.toasts.show(RecorderUiCopy.LEFT_SAVED, actionLabel = null)
            }
        }
    }

    BackHandler(enabled = state.stage != CaptureStage.IDLE) {
        if (state.stage == CaptureStage.RECORDING) leaveNow() else hub.launch { back() }
    }

    ProtectWindow(CaptureProtection.of(state.env, CaptureProtection.projectsShown(state)))

    // LIB-19 recorder tour: it waits until a capture pane is drawn, so every card has its hole (RecorderTourSteps).
    if (RecorderTourSteps.hold(state)) TourHold()
    // While its card is shown the take pauses, so reading the cards is not recorded; it resumes when the
    // card closes (or this screen leaves). Keyed on the stage: a take started from the memo's mic pauses too.
    val touring = tourRunning()
    DisposableEffect(touring, state.stage) {
        if (touring) hub.holdForTour()
        onDispose { if (touring) hub.releaseTour() }
    }

    // U-13: the route left while a take records or is paused (popped, another screen on top, the app closed).
    // Declared after the tour hold so it is disposed first: the take is saved before a tour release could resume it.
    // Not on a configuration change, and not when the system tears the screen down in the background (LRC-09).
    val activity = remember(context) { context.findActivity() }
    DisposableEffect(hub) {
        onDispose {
            val inApp = activity?.isChangingConfigurations != true &&
                (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) || activity?.isFinishing == true)
            if (inApp && deps.navigator.backStack.none { it is Recording }) leaveNow()
        }
    }

    val inner = LocalWindowLayout.current.isInner
    val reduced = CivilightTheme.reducedMotion
    val motion = CivilightTheme.motion
    val glowOn = deps.settings.collectAsState().value.recorderGlow

    // One sheet for the picker, the memo and the mic reason, swapped inside it (row 13): two stacked
    // layouts scaled the screen behind them twice (0.965 squared) when the picker opened over the memo.
    val sheetKind = CaptureSheets.of(state)
    val shownKind = rememberLastNonNull(sheetKind)
    val shownPicker = rememberLastNonNull(state.picker)
    CivilightSheetLayout(
        sheetVisible = sheetKind != null,
        onDismissRequest = { if (state.picker != null) hub.closePicker() else hub.launch { back() } },
        centered = inner,
        sheet = {
            AnimatedContent(
                targetState = shownKind,
                transitionSpec = { motion.swap() },
                label = "captureSheet",
            ) { kind ->
                Column {
                    when (kind) {
                        CaptureSheetKind.PICKER -> LinkPickerContent(hub, onClose = hub::closePicker, request = shownPicker)
                        CaptureSheetKind.MIC_REASON -> MicReasonContent(onAllow = { permission.launch(Manifest.permission.RECORD_AUDIO) }, onClose = hub::dismissMic)
                        CaptureSheetKind.MEMO -> TextMemoSheetContent(state, memoActions(hub, context, onOpenPicker = hub::openPicker))
                        null -> Unit
                    }
                }
            }
        },
    ) {
        val glow = GlowModes.of(state, glowOn, reduced)
        val glass = state.stage == CaptureStage.RECORDING || state.stage == CaptureStage.PROCESSING || state.stage == CaptureStage.MIC_OFF
        CaptureFrame(glow = glow, level = { hub.level.value }, glass = glass) {
            // The typed line never moves, so the stages swap by opacity only; each slot keeps its own view
            // so the pane that leaves still shows what it showed (the live state has moved on).
            val slot = CaptureSlot.of(state)
            AnimatedContent(
                targetState = slot,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = { motion.swap() },
                contentKey = { it.kind },
                label = "captureStage",
            ) { shown ->
                Column(Modifier.fillMaxSize()) {
                    when (shown) {
                        is CaptureSlot.Recording -> RecordingPane(
                            view = shown.view,
                            onCancel = {
                                hub.launch {
                                    val outcome = cancel()
                                    if (outcome is CancelOutcome.Discarded) {
                                        deps.toasts.show(ShownCopy.of(outcome.cancelled.toast), RecorderCopy.UNDO) {
                                            hub.launch {
                                                undoCancel(outcome.cancelled)
                                                deps.navigator.push(EntryRoutes.routeOf(outcome.cancelled.session.entry))
                                            }
                                        }
                                    }
                                }
                            },
                            onAddLink = { hub.openPicker(PickerRequest(PickerTarget.SESSION)) },
                            onReplaceLink = { chip -> hub.openPicker(PickerRequest(PickerTarget.SESSION, chip.links.map { it.key })) },
                            onRemoveLink = { chip -> hub.removeLinks(PickerTarget.SESSION, chip.links.map { it.key }) },
                            onToText = hub::toText,
                            onStop = { hub.launch { stopAndSave() } },
                            onPause = hub::togglePause,
                        )
                        is CaptureSlot.Processing -> ProcessingPane(
                            view = shown.view,
                            onAddLink = { hub.openPicker(PickerRequest(PickerTarget.SESSION)) },
                            onReplaceLink = { chip -> hub.openPicker(PickerRequest(PickerTarget.SESSION, chip.links.map { it.key })) },
                            onRemoveLink = { chip -> hub.removeLinks(PickerTarget.SESSION, chip.links.map { it.key }) },
                            onHide = hub::setHide,
                            onPutHere = {
                                hub.launch {
                                    val done = putHere() ?: return@launch
                                    deps.toasts.show(ShownCopy.of(done.insertion.toast), RecorderCopy.UNDO) { hub.launch { undoPut(done) } }
                                }
                            },
                            onContinue = hub::continueSpeaking,
                            onKeep = { hub.launch { keepInInbox() } },
                        )
                        CaptureSlot.MicOff -> MicOffPane(
                            onClose = hub::dismissMic,
                            onOpenSettings = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            },
                            onToText = { hub.launch { micOffToText() } },
                        )
                        CaptureSlot.None -> Unit
                    }
                }
            }
        }
    }
}

/**
 * The memo's actions. [onOpenPicker] opens the link picker where the memo is shown (the overlay
 * draws it itself; a shared sheet stacks the registered picker sheet). The memo's mic asks for the
 * permission through the reason line first (LRC-06).
 */
fun memoActions(hub: CaptureHub, context: Context, onOpenPicker: (PickerRequest) -> Unit, onToVoice: (() -> Unit)? = null) = MemoActions(
    onTextChange = hub::memoTextChanged,
    onCaret = hub::memoCaretMoved,
    onPickAt = hub::memoPickAt,
    onOpenPicker = onOpenPicker,
    onRemoveLinks = { chip -> hub.removeLinks(PickerTarget.MEMO, chip.links.map { it.key }) },
    onHide = hub::setHide,
    onToVoice = onToVoice ?: {
        val granted = micGranted(context)
        hub.launch { memoToVoice(granted) }
        Unit
    },
    onSave = { hub.launch { memoSave() } },
    onClose = { hub.launch { memoClose() } },
)
