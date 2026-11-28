package io.github.lenticualrstone.civilight.feature.script.input

import android.graphics.BitmapFactory
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.domain.script.ReadAloud
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

/** The player's position and rehearsal helpers (pure; JVM tested). */
object ReadAloudNav {
    val SPEEDS: List<Float> = listOf(0.5f, 1.0f, 1.5f, 2.0f)

    fun next(i: Int, size: Int): Int = if (i < size - 1) i + 1 else i

    fun prev(i: Int): Int = if (i > 0) i - 1 else i

    fun canNext(i: Int, size: Int): Boolean = i < size - 1

    fun canPrev(i: Int): Boolean = i > 0

    fun nextSpeed(current: Float): Float {
        val idx = SPEEDS.indexOfFirst { abs(it - current) < 0.05f }
        return SPEEDS[(idx + 1) % SPEEDS.size]
    }

    fun speedLabel(speed: Float): String = when {
        abs(speed - 0.5f) < 0.05f -> "0.5×"
        abs(speed - 1.5f) < 0.05f -> "1.5×"
        abs(speed - 2.0f) < 0.05f -> "2×"
        else -> "1×"
    }

    /** Deterministic subtle pitch casting per speaker so voices sound distinguishable in TTS rehearsal. */
    fun speakerPitch(speaker: String?): Float {
        if (speaker.isNullOrBlank()) return 1.0f
        val bucket = abs(speaker.hashCode()) % 7
        return 0.88f + bucket * 0.045f
    }
}

/**
 * (0.2.0 + 0.7.0) `낭독 · 리허설`:
 * - Manual VN mode: background asset image + speaker/text box + `이전 · 처음부터 · 닫기 · 다음`.
 * - Auto Rehearsal mode (0.7.0): auto-advance (`Play/Pause`), speed control (`0.5× · 1× · 1.5× · 2×`),
 *   optional Android `TextToSpeech` voice casting with character-synced typewriter reveal, and next-speaker typing indicator.
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
    val context = LocalContext.current
    val step = steps.getOrNull(index)
    val nextStep = steps.getOrNull(index + 1)
    val hash = step?.background?.let { doc.index.scriptAsset(it)?.fileHash }

    var autoPlay by remember { mutableStateOf(false) }
    var ttsOn by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(1.0f) }
    var revealedChars by remember(index) { mutableIntStateOf(Int.MAX_VALUE) }
    var waitingNextSpeaker by remember(index) { mutableStateOf(false) }
    var ttsDoneToken by remember { mutableIntStateOf(0) }

    // Lazy Android TextToSpeech lifecycle: initialized only when user toggles TTS on, always shut down cleanly.
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    DisposableEffect(ttsOn) {
        if (!ttsOn) {
            ttsEngine?.stop()
            ttsEngine?.shutdown()
            ttsEngine = null
            ttsReady = false
            onDispose { }
        } else {
            var engine: TextToSpeech? = null
            engine = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    engine?.language = Locale.KOREAN
                    engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                            revealedChars = end
                        }
                        override fun onDone(utteranceId: String?) {
                            revealedChars = Int.MAX_VALUE
                            ttsDoneToken++
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            revealedChars = Int.MAX_VALUE
                            ttsDoneToken++
                        }
                    })
                    ttsReady = true
                }
            }
            ttsEngine = engine
            onDispose {
                engine.stop()
                engine.shutdown()
                ttsEngine = null
                ttsReady = false
            }
        }
    }

    // Drive typewriter reveal + optional TTS speech + auto-advance pacing.
    LaunchedEffect(index, autoPlay, ttsOn, ttsReady, speed, step) {
        waitingNextSpeaker = false
        val fullText = when (step) {
            is ReadAloud.Step.Line -> step.text
            is ReadAloud.Step.Caption -> step.text
            is ReadAloud.Step.Choices -> step.lines.joinToString(" ")
            null -> ""
        }
        val speakerName = (step as? ReadAloud.Step.Line)?.speaker

        if (ttsOn && ttsReady && fullText.isNotBlank()) {
            val engine = ttsEngine
            if (engine != null) {
                revealedChars = if (autoPlay) 0 else fullText.length
                val beforeToken = ttsDoneToken
                engine.stop()
                engine.setSpeechRate(speed)
                engine.setPitch(ReadAloudNav.speakerPitch(speakerName))
                engine.speak(fullText, TextToSpeech.QUEUE_FLUSH, Bundle(), "step-$index")
                if (autoPlay) {
                    while (ttsDoneToken == beforeToken) delay(60L)
                    revealedChars = fullText.length
                    if (ReadAloudNav.canNext(index, steps.size)) {
                        waitingNextSpeaker = true
                        delay((520f / speed).toLong().coerceAtLeast(180L))
                        waitingNextSpeaker = false
                        onIndex(ReadAloudNav.next(index, steps.size))
                    } else {
                        autoPlay = false
                    }
                }
            }
            return@LaunchedEffect
        }

        if (!autoPlay) {
            revealedChars = Int.MAX_VALUE
            return@LaunchedEffect
        }

        // Auto-rehearsal without TTS: timed typewriter reveal + natural reading pause + next-speaker indicator.
        if (fullText.isNotEmpty() && !motion.reduced) {
            revealedChars = 0
            val charDelay = (38f / speed).toLong().coerceIn(12L, 90L)
            for (c in 1..fullText.length) {
                revealedChars = c
                delay(charDelay)
            }
        } else {
            revealedChars = fullText.length
        }
        val holdMs = ((650f + fullText.length * 24f) / speed).toLong().coerceIn(500L, 4200L)
        delay(holdMs)
        if (ReadAloudNav.canNext(index, steps.size)) {
            waitingNextSpeaker = true
            delay((560f / speed).toLong().coerceAtLeast(220L))
            waitingNextSpeaker = false
            onIndex(ReadAloudNav.next(index, steps.size))
        } else {
            autoPlay = false
        }
    }

    Box(Modifier.fillMaxSize().background(colors.bg).pointerInput(Unit) { consumeAllPointers() }) {
        if (hash != null) {
            val image = rememberBlobImage(hash)
            if (image != null) Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }

        // Top rehearsal control strip: progress + Auto Play + Speed + TTS toggle
        Row(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RehearsalPill(
                text = "${index + 1} / ${steps.size.coerceAtLeast(1)}",
                active = false,
                onClick = null,
            )
            Box(Modifier.weight(1f))
            RehearsalPill(
                text = if (autoPlay) "정지" else "자동 재생",
                icon = if (autoPlay) IconId.Stop else IconId.Play,
                active = autoPlay,
                onClick = { autoPlay = !autoPlay },
            )
            RehearsalPill(
                text = ReadAloudNav.speedLabel(speed),
                active = false,
                onClick = { speed = ReadAloudNav.nextSpeed(speed) },
            )
            RehearsalPill(
                text = "음성",
                icon = IconId.Volume,
                active = ttsOn,
                onClick = { ttsOn = !ttsOn },
            )
        }

        Column(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Next-speaker typing indicator pill between turns
            if (waitingNextSpeaker && nextStep is ReadAloud.Step.Line && !nextStep.speaker.isNullOrBlank()) {
                TypingIndicatorPill(speaker = nextStep.speaker!!)
            }

            AnimatedContent(
                step,
                transitionSpec = { motion.enter(CivilightMotion.standard) togetherWith motion.exit(CivilightMotion.standard) },
                label = "readAloudStep",
            ) { s ->
                val boxShape = RoundedCornerShape(18.dp)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(boxShape)
                        .background(colors.raise.copy(alpha = 0.94f))
                        .insetOutline(colors.edge, 1.dp, boxShape)
                        .pressable(
                            PressAction.Click({
                                val textLen = (s as? ReadAloud.Step.Line)?.text?.length ?: (s as? ReadAloud.Step.Caption)?.text?.length ?: 0
                                if (revealedChars < textLen) {
                                    revealedChars = textLen
                                } else {
                                    onIndex(ReadAloudNav.next(index, steps.size))
                                }
                            }),
                            enabled = ReadAloudNav.canNext(index, steps.size) || revealedChars < Int.MAX_VALUE,
                            role = Role.Button,
                            scaleOnPress = false,
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    when (s) {
                        is ReadAloud.Step.Line -> {
                            s.speaker?.let { CivilightText(it, type.speakerName.merge(TextStyle(fontSize = 13.sp)), color = colors.acc, maxLines = 1) }
                            val shown = if (revealedChars >= s.text.length) s.text else s.text.take(revealedChars.coerceAtLeast(0))
                            CivilightText(shown, type.line.merge(TextStyle(fontSize = 17.sp)), color = colors.ink)
                        }
                        is ReadAloud.Step.Caption -> {
                            val shown = if (revealedChars >= s.text.length) s.text else s.text.take(revealedChars.coerceAtLeast(0))
                            CivilightText(shown, type.number.merge(TextStyle(fontSize = 13.sp, lineHeight = 20.sp)), color = colors.ink3)
                        }
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
                CivilightButton(WriteCopy.PREV, { autoPlay = false; onIndex(ReadAloudNav.prev(index)) }, Modifier.weight(1f), ButtonVariant.COMPACT_GHOST, enabled = ReadAloudNav.canPrev(index))
                CivilightButton(WriteCopy.FROM_START, { onIndex(0) }, Modifier.weight(1f), ButtonVariant.COMPACT_GHOST, enabled = steps.isNotEmpty())
                CivilightButton(WriteCopy.CLOSE, { autoPlay = false; ttsEngine?.stop(); onClose(step?.blockKey) }, Modifier.weight(1f), ButtonVariant.COMPACT_GHOST)
                CivilightButton(WriteCopy.NEXT, { autoPlay = false; onIndex(ReadAloudNav.next(index, steps.size)) }, Modifier.weight(1f), ButtonVariant.COMPACT_PRIMARY, enabled = ReadAloudNav.canNext(index, steps.size))
            }
        }
    }
}

@Composable
private fun RehearsalPill(
    text: String,
    icon: IconId? = null,
    active: Boolean,
    onClick: (() -> Unit)?,
) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val mod = Modifier
        .clip(shape)
        .background(if (active) colors.acc.copy(alpha = 0.22f) else colors.raise.copy(alpha = 0.88f))
        .insetOutline(if (active) colors.acc else colors.edge, 1.dp, shape)
        .let { base -> if (onClick != null) base.pressable(onClick = onClick, hitTarget = HitTarget.CHIP, shape = shape) else base }
        .padding(horizontal = 10.dp, vertical = 7.dp)
    Row(
        mod,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            CivilightIcon(icon, contentDescription = null, size = 14.dp, tint = if (active) colors.acc else colors.ink2)
        }
        CivilightText(
            text,
            CivilightTheme.type.chip.copy(fontSize = 12.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium),
            color = if (active) colors.acc else colors.ink,
            maxLines = 1,
        )
    }
}

@Composable
private fun TypingIndicatorPill(speaker: String) {
    val colors = CivilightTheme.colors
    val motion = LocalMotion.current
    val timeMs = remember { mutableLongStateOf(0L) }
    if (motion.loops) {
        LaunchedEffect(Unit) {
            val start = withInfiniteAnimationFrameMillis { it }
            while (true) {
                withInfiniteAnimationFrameMillis { now -> timeMs.longValue = now - start }
            }
        }
    }
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .clip(shape)
            .background(colors.raise.copy(alpha = 0.90f))
            .insetOutline(colors.edge, 1.dp, shape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivilightText("$speaker 입력 중", CivilightTheme.type.small, color = colors.ink2, maxLines = 1)
        val t = timeMs.longValue / 180f
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(3) { idx ->
                val a = if (motion.loops) 0.35f + 0.65f * ((sin(t - idx * 0.9f) + 1f) * 0.5f) else 0.7f
                Box(Modifier.size(4.dp).alpha(a).clip(CircleShape).background(colors.acc))
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
