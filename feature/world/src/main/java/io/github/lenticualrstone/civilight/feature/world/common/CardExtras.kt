package io.github.lenticualrstone.civilight.feature.world.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.ShardArt
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.ImagePickSheet
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.picker.HiddenGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** What a card's 16:9 image slot shows (WLD-07, board `ImageSlots` row `장소 · 용어 · 세력 · 16:9`). */
sealed interface CardImageUi {
    /** No image: the initial-letter card and `이미지 고르기`. */
    data class Initial(val letter: String) : CardImageUi

    /** A preset image (`preset:<id>`); the presets themselves are the image area's (CNV). */
    data class Preset(val id: String) : CardImageUi

    /** A picked image: the blob and its 16:9 crop (null = centre crop of the original). */
    data class Picked(val hash: String, val crop: Crop?, val holo: Boolean) : CardImageUi
}

/** Pure part of the image slot: the item's `CARD_REFERENCE` image use (WLD-07). */
object CardImages {
    fun ownerKind(kind: WorldKind): ImageOwnerKind = when (kind) {
        WorldKind.PLACE -> ImageOwnerKind.PLACE
        WorldKind.TERM -> ImageOwnerKind.TERM
        WorldKind.FACTION -> ImageOwnerKind.FACTION
    }

    fun of(doc: ProjectDoc, kind: WorldKind, key: Key, name: String): CardImageUi {
        val use = doc.index.imageUse(ownerKind(kind), key, ImageSlot.CARD_REFERENCE)
            ?: return CardImageUi.Initial(name.trim().take(1))
        use.presetId?.let { return CardImageUi.Preset(it) }
        return CardImageUi.Picked(use.blobHash!!, use.cropFor(Ratio.R16X9), use.holo)
    }

    /** The pick sheet of the slot (the image area registers it: canvas `ImagePickSheet`). */
    fun pickSheet(projectKey: Key, kind: WorldKind, key: Key) = ImagePickSheet(projectKey, ownerKind(kind), key, ImageSlot.CARD_REFERENCE)

    /**
     * Decodes [file] for a slot about [targetWidth] px wide and applies [crop] (source pixels after
     * rotation, then the mirror). Runs off the main thread; null when the file cannot be read.
     */
    fun decode(file: File, crop: Crop?, targetWidth: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val cropWidth = crop?.w ?: bounds.outWidth
        var sample = 1
        while (cropWidth / (sample * 2) >= targetWidth) sample *= 2
        val raw = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        if (crop == null) return raw
        val matrix = Matrix().apply {
            if (crop.rotation % 360 != 0) postRotate(crop.rotation.toFloat())
            if (crop.flipH) postScale(-1f, 1f)
        }
        val turned = if (matrix.isIdentity) raw else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
        val x = (crop.x / sample).coerceIn(0, turned.width - 1)
        val y = (crop.y / sample).coerceIn(0, turned.height - 1)
        val w = (crop.w / sample).coerceIn(1, turned.width - x)
        val h = (crop.h / sample).coerceIn(1, turned.height - y)
        return Bitmap.createBitmap(turned, x, y, w, h)
    }
}

/**
 * The card's 16:9 image slot (WLD-07): the picked image (decoded off the main thread), a preset, or
 * the initial-letter card; [label] (`참고 이미지` on the term card) over the image; `이미지 고르기`
 * under it opens the image area's `ImagePickSheet` (picking, crop and holo are that area's).
 */
@Composable
fun CardImageSlot(
    doc: ProjectDoc,
    kind: WorldKind,
    key: Key,
    name: String,
    label: String?,
    readOnly: Boolean,
    onPick: () -> Unit,
) {
    val image = CardImages.of(doc, kind, key, name)
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(18.dp)
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(shape).background(colors.raise)) {
            CardImageContent(image, name, targetWidth = 1280)
            if (label != null) {
                CivilightText(
                    text = label,
                    style = type.micro,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        if (!readOnly) {
            TextAction(WorldCopy.PICK_IMAGE, onPick, Modifier.align(Alignment.End), color = colors.acc, style = type.small)
        }
    }
}

/**
 * What a 16:9 image slot draws (card, card row tile): the initial-letter card, a preset, or the picked image
 * decoded off the main thread; the picked image fades in on `standard` (and a changed crop or image
 * crossfades). [modifier] goes on the image layer only (the card row's parallax), not on the letter.
 */
@Composable
fun CardImageContent(image: CardImageUi, name: String, targetWidth: Int, modifier: Modifier = Modifier) {
    val deps = LocalFeatureDeps.current
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val motion = CivilightTheme.motion
    Box(Modifier.fillMaxSize()) {
        when (image) {
            is CardImageUi.Initial -> CivilightText(image.letter, type.hero, Modifier.align(Alignment.Center), color = colors.ink3)
            is CardImageUi.Preset -> ShardArt(listOf(colors.tint, colors.raise, colors.edge), modifier)
            is CardImageUi.Picked -> {
                val bitmap by produceState<ImageBitmap?>(null, image) {
                    value = withContext(Dispatchers.IO) {
                        deps.stores.blobs.fileOf(image.hash)?.let { runCatching { CardImages.decode(it, image.crop, targetWidth) }.getOrNull() }?.asImageBitmap()
                    }
                }
                Crossfade(targetState = bitmap, modifier = modifier.fillMaxSize(), animationSpec = motion.spec(CivilightMotion.standard), label = "cardImage") { shown ->
                    if (shown != null) Image(shown, contentDescription = name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
        }
    }
}

/**
 * The card of a trashed item: its dotted label (`지운 장소 · 잿더미 시장`) and, while its trash entry
 * exists, `되살리기` (WLD-14). A hidden item asks for `지문으로 보기` first.
 */
@Composable
fun WorldGoneCard(access: CardAccess.Gone, doc: ProjectDoc?, vm: WorldViewModel, kind: WorldKind, key: Key, readOnly: Boolean, gate: HiddenGate) {
    val deps = LocalFeatureDeps.current
    val scope = rememberCoroutineScope()
    val item = ItemLifecycle.itemId(kind, key)
    Column {
        if (access.text.isNotEmpty()) ListRow(title = access.text, titleDotted = access.dotted, modifier = Modifier.padding(top = 12.dp))
        FoldReveal(!readOnly && doc != null && ItemLifecycle.restorable(doc, item)) {
            CivilightButton(
                text = WorldCopy.BTN_RESTORE,
                variant = ButtonVariant.GHOST,
                modifier = Modifier.padding(top = 12.dp),
                onClick = {
                    scope.launch {
                        when (val r = vm.restoreItem(item, authenticated = gate.open)) {
                            is Restored.Done -> deps.toasts.show(r.toast.message, actionLabel = r.toast.action) { vm.undoLast() }
                            Restored.NeedsAuth -> gate.unlock()
                            Restored.NotFound -> Unit
                        }
                    }
                },
            )
        }
    }
}

/** `◇ 같은 이름 2 · 노아` with `합치기` (WLD-15 entry): only when another live item has the same name. */
@Composable
fun SameNameLine(doc: ProjectDoc?, kind: WorldKind, key: Key, readOnly: Boolean, onMerge: () -> Unit) {
    val text = doc?.let { ItemLifecycle.sameName(it, kind, key) }
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    FoldRevealValue(text) { line ->
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CivilightText(line, type.small, color = colors.ink3)
            if (!readOnly) TextAction(WorldCopy.MENU_MERGE, onMerge, color = colors.acc, style = type.small)
        }
    }
}
