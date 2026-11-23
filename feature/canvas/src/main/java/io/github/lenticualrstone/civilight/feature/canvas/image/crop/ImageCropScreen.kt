package io.github.lenticualrstone.civilight.feature.canvas.image.crop

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CropRatioRow
import io.github.lenticualrstone.civilight.designsystem.component.StepSlider
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.feature.canvas.common.canvasEditContext
import io.github.lenticualrstone.civilight.feature.canvas.image.CropHandOff
import io.github.lenticualrstone.civilight.feature.canvas.image.CropMath
import io.github.lenticualrstone.civilight.feature.canvas.image.CropSession
import io.github.lenticualrstone.civilight.feature.canvas.image.HoloRules
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageCopy
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageEdits
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageOwner
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageOwners
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageSlots
import io.github.lenticualrstone.civilight.feature.canvas.image.PixelSize
import io.github.lenticualrstone.civilight.feature.canvas.image.ProvideHoloTilt
import io.github.lenticualrstone.civilight.feature.canvas.image.SlotImages
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.ImageCrop
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Longest side the stage decodes the original to (the stored crop is always in source pixels). */
private const val STAGE_SIDE = 2048

private val SessionSaver = Saver<CropSession?, String>(save = { it?.encode() }, restore = { CropSession.decode(it) })

/** Size and stage bitmap of an original, read off the main thread. */
private data class Original(val size: PixelSize, val bitmap: ImageBitmap)

/** Loading the original: [original] is null when the file is missing or unreadable. */
private data class OriginalLoad(val original: Original?)

/**
 * `자르기` (CNV-28, board `ImageCrop`): full screen. Top bar `취소` · `<owner> · <slot>` over `자르기` ·
 * `적용`; the stage; the controls (ratio chips + `규격 고정`, `확대` slider with its value, `90° 회전 ·
 * 좌우 반전 · 처음대로`, the holo row for holo-capable slots). Each ratio chip edits its own crop. `적용`
 * writes once; `취소` and back drop the session. Inner screens keep the stage full and the controls in
 * the 560 column (Q-IM-01 default).
 */
@Composable
fun ImageCropScreen(route: ImageCrop, deps: FeatureDeps) {
    val vm = entryViewModel { ImageCropViewModel(route.projectKey, deps.projects, deps::canvasEditContext) }
    val load by vm.state.collectAsState()
    val settings by deps.settings.collectAsState()
    val ready = load as? ProjectLoad.Ready
    val owner = ImageOwner(route.ownerKind, route.ownerKey, route.slot)
    val spec = ImageSlots.spec(route.slot)
    val handOff = remember { CropHandOff.take(route.ownerKey, route.slot, route.blobHash) }
    // The hand-off lives in memory only; its note is kept with the screen so a restore still shows it (GAL-14).
    val note by rememberSaveable { mutableStateOf(handOff?.note) }
    val ownerName = ready?.doc?.let { ImageOwners.name(it, route.ownerKind, route.ownerKey) }.orEmpty()
    CropEditor(
        doc = ready?.doc,
        blobHash = route.blobHash,
        stageSlot = route.slot,
        header = ImageCopy.cropHeader(ownerName, spec.shortName),
        handOff = handOff,
        note = note,
        deps = deps,
        start = { doc, size ->
            val existing = ImageEdits.use(doc, owner)
            val holo = handOff?.holo ?: HoloRules.initial(route.slot, existing, settings.holoDefaultForNewImages)
            CropSession.start(route.slot, size, existing, route.blobHash, holo, handOff?.crops.orEmpty())
        },
        onCancel = { deps.navigator.pop() },
        onApply = { s ->
            vm.apply(owner, route.blobHash, s) { r ->
                if (r == EditResult.READ_ONLY) deps.toasts.show(ImageCopy.READ_ONLY, actionLabel = null) else deps.navigator.pop()
            }
        },
    )
}

/**
 * The crop editor itself (CNV-28), shared by the `ImageCrop` route and the gallery's in-screen crop (GAL-12):
 * loads [blobHash], starts the session with [start] once the original's size is known, and hands the session to
 * [onApply]. Nothing is written here. [handleBack] = back cancels (an in-screen editor has no route to pop).
 * [note] is shown above the controls (`숨긴 이미지 · 초상에서는 보임`, GAL-14).
 */
@Composable
internal fun CropEditor(
    doc: ProjectDoc?,
    blobHash: String,
    stageSlot: ImageSlot,
    header: String,
    handOff: CropHandOff?,
    note: String?,
    deps: FeatureDeps,
    start: (ProjectDoc, PixelSize) -> CropSession,
    onCancel: () -> Unit,
    onApply: (CropSession) -> Unit,
    handleBack: Boolean = false,
) {
    if (handleBack) LocalBackHandler.current(true, onCancel)
    var played by rememberSaveable { mutableStateOf(false) }
    /** Raised when the user switches holo on (the foil sweeps once then); never on a restore. */
    var holoSweep by rememberSaveable { mutableIntStateOf(0) }
    /** Raised for each step from the controls (turn, mirror, reset, zoom, ratio): those ease; a drag or pinch never raises it. */
    var controlStep by remember { mutableIntStateOf(0) }
    var session by rememberSaveable(stateSaver = SessionSaver) { mutableStateOf<CropSession?>(null) }
    val provisional = remember { CropSession.start(stageSlot, PixelSize(4, 3), null, blobHash, holo = false) }
    val knownSize = doc?.index?.blob(blobHash)?.let { b -> if (b.width != null && b.height != null) PixelSize(b.width!!, b.height!!) else null }
    val loaded by produceState<OriginalLoad?>(null, blobHash, doc != null) {
        if (doc == null) return@produceState
        value = OriginalLoad(withContext(Dispatchers.IO) {
            val file = deps.stores.blobs.fileOf(blobHash) ?: return@withContext null
            val size = knownSize ?: BitmapFactory.Options().apply { inJustDecodeBounds = true }.let { o ->
                BitmapFactory.decodeFile(file.path, o)
                if (o.outWidth > 0 && o.outHeight > 0) PixelSize(o.outWidth, o.outHeight) else null
            } ?: return@withContext null
            runCatching { SlotImages.decode(file, null, STAGE_SIDE) }.getOrNull()?.let { Original(size, it.asImageBitmap()) }
        })
    }
    val original = loaded?.original
    LaunchedEffect(doc != null, original) {
        val d = doc ?: return@LaunchedEffect
        val o = original ?: return@LaunchedEffect
        if (session == null) session = start(d, o.size)
    }
    val inner = LocalWindowLayout.current.isInner
    val apply: () -> Unit = { session?.let(onApply) }

    ProvideHoloTilt {
        CivilightScreen(
            title = ImageCopy.CROP_TITLE,
            topBar = { CropTopBar(header, onCancel = onCancel, onApply = apply, canApply = session != null) },
            bottomBar = {
                // The controls keep their place from the first frame (the stage does not resize when the session
                // arrives); they fade in on `enter` and take no touches until the session exists.
                val live = session != null
                val shown = session ?: provisional
                val appear by animatedFraction(live, CivilightMotion.enter, label = "cropControls")
                Box(Modifier.graphicsLayer { alpha = appear }.then(if (live) Modifier else Modifier.blockTouches())) {
                    CropControls(
                        session = shown,
                        inner = inner,
                        note = note,
                        onChange = {
                            if (live) {
                                controlStep++
                                session = it
                            }
                        },
                        onHolo = { on ->
                            if (live) {
                                session = shown.holo(on)
                                if (on) holoSweep++
                            }
                        },
                    )
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().background(CivilightTheme.colors.bg).padding(padding)) {
                val o = original
                val s = session
                if (loaded != null && o == null) {
                    CivilightText(
                        ImageCopy.IMAGE_UNREADABLE,
                        CivilightTheme.type.small,
                        Modifier.align(Alignment.Center).padding(24.dp),
                        color = CivilightTheme.colors.ink3,
                    )
                } else {
                    // Captured once: the growth plays on the first show only, never after a restore. The stage is on
                    // screen from the first frame: the picker's thumbnail grows while the original decodes.
                    val origin = remember { if (played) null else handOff?.tileBounds }
                    LaunchedEffect(Unit) { played = true }
                    CropStage(
                        bitmap = o?.bitmap,
                        session = s,
                        slot = stageSlot,
                        origin = origin,
                        tile = handOff?.tileBitmap,
                        holoSweep = holoSweep,
                        controlStep = controlStep,
                        onGesture = { dx, dy, zoom ->
                            val cur = session ?: return@CropStage
                            var next = cur.pan(dx, dy)
                            if (zoom != 1f) next = next.zoom(next.edit.zoom * zoom)
                            session = next
                        },
                    )
                }
            }
        }
    }
}

/** `bg`, 70 tall: `취소` · `<owner> · <slot>` over `자르기` (700 17 / 24) · `적용` (primary, 40). */
@Composable
private fun CropTopBar(header: String, onCancel: () -> Unit, onApply: () -> Unit, canApply: Boolean) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.bg)
            .statusBarsPadding()
            .heightIn(min = 70.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextAction(ImageCopy.BTN_CANCEL, onCancel)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            CivilightText(header, type.number.merge(TextStyle(fontSize = 11.sp, lineHeight = 14.sp)), color = colors.ink3, maxLines = 1)
            CivilightText(
                ImageCopy.CROP_TITLE,
                type.title.merge(TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)),
                color = colors.ink,
                maxLines = 1,
            )
        }
        CivilightButton(ImageCopy.BTN_APPLY, onApply, Modifier.padding(end = 6.dp), variant = ButtonVariant.COMPACT_PRIMARY, enabled = canApply)
    }
}

/** The `bg` control panel (16 × 20 padding): ratio chips, zoom, tools, holo. */
@Composable
private fun CropControls(session: CropSession, inner: Boolean, note: String?, onChange: (CropSession) -> Unit, onHolo: (Boolean) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Box(Modifier.fillMaxWidth().background(colors.bg).navigationBarsPadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .then(if (inner) Modifier.widthIn(max = 560.dp) else Modifier)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // A hidden gallery image used as a main image says what becomes visible (GAL-14).
            note?.let { CivilightText(it, type.small, color = colors.ink3) }
            CropRatioRow(
                labels = session.ratios.map(session::chip),
                selectedIndex = session.ratios.indexOf(session.current),
                onSelect = { i -> onChange(session.select(session.ratios[i])) },
                note = ImageCopy.FIXED_RATIOS,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepSlider(
                    value = CropMath.stepOfZoom(session.edit.zoom),
                    stepCount = CropMath.zoomSteps,
                    onValueChange = { onChange(session.zoomStep(it)) },
                    modifier = Modifier.weight(1f),
                    smallLabel = ImageCopy.ZOOM,
                    largeLabel = "",
                    contentDescription = ImageCopy.ZOOM,
                )
                CivilightText(ImageCopy.zoomValue(session.edit.zoom), type.number, color = colors.ink2, maxLines = 1)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CivilightButton(ImageCopy.BTN_ROTATE, { onChange(session.rotate()) }, Modifier.weight(1f), variant = ButtonVariant.GHOST, icon = IconId.Rotate)
                CivilightButton(ImageCopy.BTN_FLIP, { onChange(session.flip()) }, Modifier.weight(1f), variant = ButtonVariant.GHOST, icon = IconId.Flip)
                CivilightButton(ImageCopy.BTN_RESET, { onChange(session.reset()) }, Modifier.weight(1f), variant = ButtonVariant.GHOST, icon = IconId.Reset)
            }
            if (session.holoAllowed) {
                SwitchRow(title = ImageCopy.HOLO, checked = session.holo, onCheckedChange = onHolo, subtitle = ImageCopy.HOLO_SUB_CROP)
            }
        }
    }
}

/** Swallows every touch on a part of the screen that is drawn but not ready yet. */
private fun Modifier.blockTouches(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
    }
}
