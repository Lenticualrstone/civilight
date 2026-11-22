package io.github.lenticualrstone.civilight.feature.recorder.audio

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor
import io.github.lenticualrstone.civilight.feature.recorder.RecorderUiCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.recorder.RecorderAudio

/**
 * The player row of a kept recording (LRC-14, boards `LrInbox` · `TwoLr`): a 32 dp round `tint`
 * button (play triangle 14, pause while playing; the touch area is the DS icon-button target) and
 * the 20-bar waveform, the played part in `acc`, the rest `ink3`.
 */
@Composable
fun PlayerRow(recordKey: Key, audio: RecorderAudio, player: RecordPlayer, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val state by player.state.collectAsState()
    val playing = state.playing(recordKey)
    val bars = remember(audio.envelope) { Waveforms.bars(audio.envelopeBytes()) }
    val played = Waveforms.playedBars(state.positionOf(recordKey), audio.durationMs)
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .pressable(onClick = { player.toggle(recordKey, audio) }, hitTarget = HitTarget.ICON_BUTTON, shape = CircleShape)
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.tint)
                .semantics { contentDescription = if (playing) RecorderUiCopy.PAUSE else RecorderUiCopy.PLAY },
            contentAlignment = Alignment.Center,
        ) {
            // Play and pause cross-fade on `standard` (reduced: 120 ms).
            Crossfade(playing, animationSpec = CivilightTheme.motion.spec(CivilightMotion.standard), label = "playPause") { isPlaying ->
                if (isPlaying) {
                    CivilightIcon(IconId.Pause, contentDescription = null, size = 14.dp, tint = colors.ink)
                } else {
                    val ink = colors.ink
                    // The board's glyph `M8 5l11 7-11 7z` in a 24 box.
                    Canvas(Modifier.size(14.dp)) {
                        val u = size.width / 24f
                        val path = Path().apply {
                            moveTo(8f * u, 5f * u)
                            lineTo(19f * u, 12f * u)
                            lineTo(8f * u, 19f * u)
                            close()
                        }
                        drawPath(path, ink)
                    }
                }
            }
        }
        Row(
            Modifier.height(24.dp).clearAndSetSemantics {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            bars.forEachIndexed { i, level ->
                // The played bars change colour on `standard`; the playhead walks bar by bar.
                val barColor = animatedColor(if (i < played) colors.acc else colors.ink3, label = "playedBar")
                Box(
                    Modifier
                        .width(3.dp)
                        .height(Waveforms.heightDp(level).dp)
                        .clip(RoundedCornerShape(1.dp))
                        .drawBehind { drawRect(barColor.value) },
                )
            }
        }
    }
}
