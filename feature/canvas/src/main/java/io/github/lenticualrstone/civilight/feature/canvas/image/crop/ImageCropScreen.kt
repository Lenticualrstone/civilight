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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
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
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
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
    var played by rememberSaveable { mutableStateOf(false) }
    var holoTurnedOn by rememberSaveable { mutableStateOf(false) }
    var session by rememberSaveable(stateSaver = SessionSaver) { mutableStateOf<CropSession?>(null) }
    val knownSize = ready?.doc?.index?.blob(route.blobHash)?.let { b -> if (b.width != null && b.height != null) PixelSize(b.width!!, b.height!!) else null }
    val loaded by produceState<OriginalLoad?>(null, route.blobHash, ready != null) {
        if (ready == null) return@produceState
        value = OriginalLoad(withContext(Dispatchers.IO) {
            val file = deps.stores.blobs.fileOf(route.blobHash) ?: return@withContext null
            val size = knownSize ?: BitmapFactory.Options().apply { inJustDecodeBounds = true }.let { o ->
                BitmapFactory.decodeFile(file.path, o)
                if (o.outWidth > 0 && o.outHeight > 0) PixelSize(o.outWidth, o.outHeight) else null
            } ?: return@withContext null
            runCatching { SlotImages.decode(file, null, STAGE_SIDE) }.getOrNull()?.let { Original(size, it.asImageBitmap()) }
        })
    }
    val original = loaded?.original
    LaunchedEffect(ready != null, original) {
        val doc = ready?.doc ?: return@LaunchedEffect
        val o = original ?: return@LaunchedEffect
        if (session == null) {
            val existing = ImageEdits.use(doc, owner)
            val holo = handOff?.holo ?: HoloRules.initial(route.slot, existing, settings.holoDefaultForNewImages)
            session = CropSession.start(route.slot, o.size, existing, route.blobHash, holo)
        }
    }
    val ownerName = ready?.doc?.let { ImageOwners.name(it, route.ownerKind, route.ownerKey) }.orEmpty()
    val inner = LocalWindowLayout.current.isInner
    val apply: () -> Unit = {
        val s = session
        if (s != null) {
            vm.apply(owner, route.blobHash, s) { r ->
                if (r == EditResult.READ_ONLY) deps.toasts.show(ImageCopy.READ_ONLY, actionLabel = null) else deps.navigator.pop()
            }
        }
    }

    ProvideHoloTilt {
        CivilightScreen(
            title = ImageCopy.CROP_TITLE,
            topBar = { CropTopBar(ImageCopy.cropHeader(ownerName, spec.shortName), onCancel = { deps.navigator.pop() }, onApply = apply, canApply = session != null) },
            bottomBar = {
                session?.let { s ->
                    CropControls(
                        session = s,
                        inner = inner,
                        onChange = { session = it },
                        onHolo = { on ->
                            holoTurnedOn = on
                            session = s.holo(on)
                        },
                    )
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().background(CivilightTheme.colors.bg).padding(padding)) {
                val o = original
                val s = session
                when {
                    ready == null || loaded == null -> Unit
                    o == null -> CivilightText(
                        ImageCopy.TODO_COPY_CANVAS_IMAGE_UNREADABLE,
                        CivilightTheme.type.small,
                        Modifier.align(Alignment.Center).padding(24.dp),
                        color = CivilightTheme.colors.ink3,
                    )
                    s == null -> Unit
                    else -> {
                        // Captured once: the growth plays on the first show only, never after a restore.
                        val origin = remember { if (played) null else handOff?.tileBounds }
                        LaunchedEffect(Unit) { played = true }
                        CropStage(
                            bitmap = o.bitmap,
                            session = s,
                            origin = origin,
                            holoSweep = holoTurnedOn,
                            onGesture = { dx, dy, zoom ->
                                var next = s.pan(dx, dy)
                                if (zoom != 1f) next = next.zoom(next.edit.zoom * zoom)
                                session = next
                            },
                        )
                    }
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
private fun CropControls(session: CropSession, inner: Boolean, onChange: (CropSession) -> Unit, onHolo: (Boolean) -> Unit) {
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
            CropRatioRow(
                labels = session.ratios.map { ImageSlots.chip(session.slot, it) },
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
