package io.github.lenticualrstone.civilight.feature.script.ui

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.tokenShadow
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.glass.OnGlass
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.feature.script.input.ReadAloudNav
import io.github.lenticualrstone.civilight.feature.script.state.BlockRow
import io.github.lenticualrstone.civilight.feature.script.state.HeadRow
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRow
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.plainText
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.sin

/**
 * (0.7.3) State holder for Sonnet-Create inline manuscript rehearsal:
 * Reveals actual manuscript rows one-by-one right on the live script canvas,
 * with pulsing `[Speaker • • •]` typing indicator + TTS-synced typewriter reveal.
 */
internal class InlineRehearsalState {
    var active by mutableStateOf(false)
    var count by mutableIntStateOf(0)
    var paused by mutableStateOf(false)
    var speed by mutableFloatStateOf(1f)
    var voiceEnabled by mutableStateOf(false)
    var spokenChars by mutableStateOf<Int?>(null)
    var restartToken by mutableIntStateOf(0)

    fun start() {
        active = true
        count = 0
        paused = false
        spokenChars = null
        restartToken++
    }

    fun stop() {
        active = false
        paused = false
        spokenChars = null
    }
}

@Composable
internal fun rememberInlineRehearsal(): InlineRehearsalState = remember { InlineRehearsalState() }

/**
 * Drives the inline rehearsal playback loop (timing + optional Android TTS with character-level progress).
 */
@Composable
internal fun InlineRehearsalEngine(
    state: InlineRehearsalState,
    rows: List<ScriptRow>,
    onScrollToLatest: (Int) -> Unit,
) {
    val context = LocalContext.current
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var ttsDoneToken by remember { mutableIntStateOf(0) }

    DisposableEffect(state.active, state.voiceEnabled) {
        if (!state.active || !state.voiceEnabled) {
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
                            if (state.active) state.spokenChars = end
                        }
                        override fun onDone(utteranceId: String?) {
                            if (state.active) state.spokenChars = Int.MAX_VALUE
                            ttsDoneToken++
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            if (state.active) state.spokenChars = Int.MAX_VALUE
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

    LaunchedEffect(state.paused) {
        if (state.paused) ttsEngine?.stop()
    }

    LaunchedEffect(state.active, state.restartToken, rows.size) {
        if (!state.active) return@LaunchedEffect
        var idx = 0
        state.count = 0
        while (idx < rows.size && state.active) {
            while (state.paused && state.active) delay(100L)
            if (!state.active) break
            val row = rows[idx]
            // Skip invisible Scene 1 spacer immediately
            if (row is HeadRow && row.ordinal == 1 && !row.hiddenTag) {
                idx++
                state.count = idx
                continue
            }
            if (state.voiceEnabled && ttsReady && row is BlockRow && row.kind == BlockKind.DIALOGUE) {
                val text = row.runs.plainText.trim()
                if (text.isNotEmpty()) {
                    delay((300f / state.speed).toLong().coerceAtLeast(120L))
                    if (!state.active) break
                    state.spokenChars = 0
                    state.count = idx + 1
                    onScrollToLatest(idx)
                    val engine = ttsEngine
                    if (engine != null) {
                        val before = ttsDoneToken
                        engine.stop()
                        engine.setSpeechRate(state.speed)
                        engine.setPitch(ReadAloudNav.speakerPitch(row.speaker?.text))
                        engine.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), "rehearsal-$idx")
                        while (ttsDoneToken == before && state.active && !state.paused && state.voiceEnabled) {
                            delay(60L)
                        }
                    }
                    state.spokenChars = null
                    idx++
                    continue
                }
            }
            // Non-TTS timing mirrors Sonnet-Create's natural conversation rhythm
            val waitSec = when (row) {
                is HeadRow -> 1.35f
                is BlockRow -> {
                    val len = row.runs.plainText.length
                    val base = if (row.kind == BlockKind.DIALOGUE) 0.55f else 0.40f
                    val perChar = if (row.kind == BlockKind.DIALOGUE) 0.032f else 0.018f
                    minOf(3.2f, base + len * perChar)
                }
                else -> 0.25f
            }
            delay(((waitSec * 1000f) / state.speed).toLong().coerceAtLeast(180L))
            while (state.paused && state.active) delay(100L)
            if (!state.active) break
            state.spokenChars = null
            state.count = idx + 1
            onScrollToLatest(idx)
            idx++
        }
    }
}

/**
 * Floating bottom controls + next-speaker typing indicator for inline manuscript rehearsal.
 */
@Composable
internal fun InlineRehearsalOverlay(
    state: InlineRehearsalState,
    rows: List<ScriptRow>,
    onExitZen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val navPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val finished = state.count >= rows.size
    val nextRow = rows.getOrNull(state.count)
    val nextSpeaker = (nextRow as? BlockRow)
        ?.takeIf { it.kind == BlockKind.DIALOGUE }
        ?.speaker?.text

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = navPad + 16.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!state.paused && !finished) {
            InlineTypingBubble(speaker = nextSpeaker)
        }

        val pillShape = RoundedCornerShape(24.dp)
        Box(
            Modifier
                .tokenShadow(pillShape, CivilightTheme.elevation.popover(colors))
                .clip(pillShape),
        ) {
            GlassBackground(
                surface = GlassSurface.POPOVER,
                modifier = Modifier.matchParentSize(),
                shape = pillShape,
                fill = colors.glass.copy(alpha = if (colors.isDark) 0.76f else 0.86f),
            )
            OnGlass {
                Row(
                    Modifier
                        .insetOutline(colors.edge.copy(alpha = if (colors.isDark) 0.30f else 0.16f), 1.dp, pillShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CivilightText(
                        "${state.count.coerceAtMost(rows.size)}/${rows.size}",
                        SonnetScriptStyle.monoChipStyle(11f),
                        color = colors.ink2,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    RehearsalActionChip(
                        icon = IconId.Volume,
                        label = "음성",
                        active = state.voiceEnabled,
                        onClick = { state.voiceEnabled = !state.voiceEnabled },
                    )
                    RehearsalActionChip(
                        icon = null,
                        label = ReadAloudNav.speedLabel(state.speed),
                        active = false,
                        onClick = { state.speed = ReadAloudNav.nextSpeed(state.speed) },
                    )
                    if (finished) {
                        RehearsalActionChip(
                            icon = IconId.Play,
                            label = "다시",
                            active = true,
                            onClick = { state.start() },
                        )
                    } else {
                        RehearsalActionChip(
                            icon = if (state.paused) IconId.Play else IconId.Stop,
                            label = if (state.paused) "재생" else "일시정지",
                            active = !state.paused,
                            onClick = { state.paused = !state.paused },
                        )
                    }
                    RehearsalActionChip(
                        icon = IconId.X,
                        label = "종료",
                        active = false,
                        onClick = {
                            state.stop()
                            onExitZen()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RehearsalActionChip(
    icon: IconId?,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val shape = CircleShape
    Row(
        Modifier
            .clip(shape)
            .background(if (active) colors.hl else colors.tint)
            .pressable(onClick = onClick, hitTarget = HitTarget.CHIP, shape = shape)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            CivilightIcon(icon, contentDescription = null, size = 14.dp, tint = if (active) colors.acc else colors.ink2)
        }
        CivilightText(
            label,
            CivilightTheme.type.chip.copy(fontSize = 12.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium),
            color = if (active) colors.acc else colors.ink,
            maxLines = 1,
        )
    }
}

@Composable
private fun InlineTypingBubble(speaker: String?) {
    val colors = CivilightTheme.colors
    val motion = LocalMotion.current
    val clockMs = remember { mutableLongStateOf(0L) }
    if (motion.loops) {
        LaunchedEffect(Unit) {
            val start = withInfiniteAnimationFrameMillis { it }
            while (true) {
                withInfiniteAnimationFrameMillis { now -> clockMs.longValue = now - start }
            }
        }
    }
    val shape = CircleShape
    Row(
        Modifier
            .clip(shape)
            .background(colors.tint)
            .insetOutline(colors.line, 1.dp, shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!speaker.isNullOrBlank()) {
            CivilightText(
                speaker,
                CivilightTheme.type.chip.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                color = colors.ink2,
                maxLines = 1,
            )
        }
        val t = clockMs.longValue / 160f
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { i ->
                val a = if (motion.loops) 0.32f + 0.68f * ((sin(t - i * 0.9f) + 1f) * 0.5f) else 0.7f
                Box(Modifier.size(6.dp).alpha(a).clip(CircleShape).background(colors.acc))
            }
        }
    }
}

/**
 * (0.7.3) Safe Scene Divider long-press menu:
 * Default delete removes ONLY the scene boundary (`SceneActions.merge`, preserving every block below),
 * while full scene+blocks management is clearly separated.
 */
@Composable
internal fun SceneDividerMenuOverlay(
    title: String,
    canRemoveDivider: Boolean,
    onEditWhere: () -> Unit,
    onRemoveDividerOnly: () -> Unit,
    onOpenFullSceneMenu: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(20.dp)
    LocalBackHandler.current(true) { onDismiss() }
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.shade)
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .padding(horizontal = 24.dp)
                .widthIn(min = 260.dp, max = 320.dp)
                .tokenShadow(shape, CivilightTheme.elevation.popover(colors))
                .clip(shape)
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            GlassBackground(
                surface = GlassSurface.POPOVER,
                modifier = Modifier.matchParentSize(),
                shape = shape,
                fill = colors.raise.copy(alpha = if (colors.isDark) 0.92f else 0.96f),
            )
            OnGlass {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .insetOutline(colors.edge.copy(alpha = if (colors.isDark) 0.30f else 0.16f), 1.dp, shape)
                        .padding(vertical = 10.dp),
                ) {
                    CivilightText(
                        title.ifBlank { "장면 구분선" },
                        SonnetScriptStyle.monoChipStyle(11f),
                        color = colors.ink3,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                    SceneDividerRow(IconId.Pin, "장면 제목 · 장소 · 때", null, colors.ink) {
                        onDismiss()
                        onEditWhere()
                    }
                    if (canRemoveDivider) {
                        SceneDividerRow(IconId.Merge, "구분선만 지우기", "아래 블록은 앞 장면에 그대로 유지", colors.acc) {
                            onDismiss()
                            onRemoveDividerOnly()
                        }
                    }
                    Box(Modifier.padding(vertical = 4.dp).fillMaxWidth().height(1.dp).background(colors.line))
                    SceneDividerRow(IconId.Trash, "이 씬과 아래 글 모두 관리", "씬 전체 삭제 · 숨김 · 이동", colors.warn) {
                        onDismiss()
                        onOpenFullSceneMenu()
                    }
                }
            }
        }
    }
}

@Composable
private fun SceneDividerRow(
    icon: IconId,
    label: String,
    sub: String?,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .pressable(onClick = onClick, hitTarget = HitTarget.BUTTON, scaleOnPress = false)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CivilightIcon(icon, contentDescription = null, size = 18.dp, tint = tint)
        Column(Modifier.weight(1f)) {
            CivilightText(label, type.itemTitleCompact, color = tint, maxLines = 1)
            if (sub != null) CivilightText(sub, type.small, color = colors.ink3, maxLines = 1)
        }
    }
}
