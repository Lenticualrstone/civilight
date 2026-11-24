package io.github.lenticualrstone.civilight.feature.script.input

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.script.ReadAloud
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The player's position in its steps (pure; JVM tested). */
object ReadAloudNav {
    fun next(i: Int, size: Int): Int = if (i < size - 1) i + 1 else i

    fun prev(i: Int): Int = if (i > 0) i - 1 else i

    fun canNext(i: Int, size: Int): Boolean = i < size - 1

    fun canPrev(i: Int): Boolean = i > 0
}

/**
 * (0.2.0, N-27, SCR-28) `낭독`: one step at a time, full screen. The background is the `배경` asset image in
 * effect (switch on and an image), else solid `bg` (G-5). The dialogue box: speaker line, text, `▸` caption
 * or the choice lines. `이전 · 처음부터 · 닫기 · 다음`; a tap on the box goes to the next step. No TTS.
 */
@Composable
fun ReadAloudPlayer(
    doc: ProjectDoc,
    steps: List<ReadAloud.Step>,
    index: Int,
    onIndex: (Int) -> Unit,
    onClose: (blockKey: Key?) -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val motion = CivilightTheme.motion
    val step = steps.getOrNull(index)
    val hash = step?.background?.let { doc.index.scriptAsset(it)?.fileHash }
    // Over the whole screen: every touch the buttons and the box leave is consumed here, so nothing reaches
    // the manuscript, header or input bar drawn underneath (taps, swipes, the disabled box on the last step).
    Box(Modifier.fillMaxSize().background(colors.bg).pointerInput(Unit) { consumeAllPointers() }) {
        if (hash != null) {
            val image = rememberBlobImage(hash)
            if (image != null) Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Column(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnimatedContent(
                step,
                transitionSpec = { motion.enter(CivilightMotion.standard) togetherWith motion.exit(CivilightMotion.standard) },
                label = "readAloudStep",
            ) { s ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.raise)
                        .pressable(PressAction.Click({ onIndex(ReadAloudNav.next(index, steps.size)) }), enabled = ReadAloudNav.canNext(index, steps.size), role = Role.Button, scaleOnPress = false)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    when (s) {
                        is ReadAloud.Step.Line -> {
                            s.speaker?.let { CivilightText(it, type.speakerName.merge(TextStyle(fontSize = 13.sp)), color = colors.ink2, maxLines = 1) }
                            CivilightText(s.text, type.line.merge(TextStyle(fontSize = 17.sp)), color = colors.ink)
                        }
                        is ReadAloud.Step.Caption -> CivilightText(s.text, type.number.merge(TextStyle(fontSize = 13.sp, lineHeight = 20.sp)), color = colors.ink3)
                        is ReadAloud.Step.Choices -> s.lines.forEachIndexed { i, line ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CivilightText("${i + 1}", type.number.merge(TextStyle(fontSize = 12.sp)), color = colors.ink3, maxLines = 1)
                                CivilightText(line, type.body.merge(TextStyle(lineHeight = 26.sp)), color = colors.ink)
                            }
                        }
                        null -> Unit
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CivilightButton(WriteCopy.PREV, { onIndex(ReadAloudNav.prev(index)) }, Modifier.weight(1f), ButtonVariant.COMPACT_GHOST, enabled = ReadAloudNav.canPrev(index))
                CivilightButton(WriteCopy.FROM_START, { onIndex(0) }, Modifier.weight(1f), ButtonVariant.COMPACT_GHOST, enabled = steps.isNotEmpty())
                CivilightButton(WriteCopy.CLOSE, { onClose(step?.blockKey) }, Modifier.weight(1f), ButtonVariant.COMPACT_GHOST)
                CivilightButton(WriteCopy.NEXT, { onIndex(ReadAloudNav.next(index, steps.size)) }, Modifier.weight(1f), ButtonVariant.COMPACT_PRIMARY, enabled = ReadAloudNav.canNext(index, steps.size))
            }
        }
    }
}

/** Swallows every pointer change after the children had it (Main pass), so siblings below see none. */
private suspend fun PointerInputScope.consumeAllPointers() {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Main).changes.forEach { it.consume() }
        }
    }
}

@Composable
private fun rememberBlobImage(hash: String): ImageBitmap? {
    val deps = LocalFeatureDeps.current
    val bitmap by produceState<ImageBitmap?>(null, hash) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val file = deps.stores.blobs.fileOf(hash) ?: return@runCatching null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= MAX_PX && bounds.outHeight / (sample * 2) >= MAX_PX) sample *= 2
                BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
            }.getOrNull()
        }
    }
    return bitmap
}

private const val MAX_PX = 1280
