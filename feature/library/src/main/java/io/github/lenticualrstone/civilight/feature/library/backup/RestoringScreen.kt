package io.github.lenticualrstone.civilight.feature.library.backup

import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ProgressBar
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.StepState
import io.github.lenticualrstone.civilight.designsystem.component.stepState
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsFrame

/** The lines of 되살리는 중 for one status (pure; tested). */
data class RestoringUi(
    val section: String,
    val fraction: Float,
    val bytes: String,
    val estimate: String?,
    val steps: List<Pair<String, String>>,
    val current: Int,
)

object RestoringModel {
    fun of(status: RestoreStatus): RestoringUi {
        val p = status.progress
        return RestoringUi(
            section = "${RestoreCopy.DRIVE} · ${status.dateLabel} ${RestoreCopy.BACKUP}",
            fraction = if (p.totalBytes <= 0) 0f else (p.doneBytes.toFloat() / p.totalBytes).coerceIn(0f, 1f),
            bytes = p.bytesLine,
            estimate = status.secondsLeft?.takeIf { !status.finished }?.let { "${RestoreCopy.ABOUT} ${it.coerceAtLeast(1)}${RestoreCopy.SECONDS}" },
            steps = p.steps,
            current = status.step,
        )
    }
}

/**
 * 되살리는 중 (route `Restoring`, LIB-60, board `Restoring`; head small `되살리기`): `Google Drive · <date>
 * 백업`, the title, the byte bar with `<done> / <total> MB` and `약 <n>초`, the square-marker steps and
 * `앱을 닫아도 계속 · 끝나면 알림`. Back leaves the screen; the restore goes on (app scope).
 */
@Composable
internal fun RestoringScreen(services: LibraryServices) {
    val navigator = services.deps.navigator
    val status by services.backups.restore.status.collectAsState()
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    SettingsFrame(title = "", smallLine = RestoreCopy.RESTORING_SMALL, onBack = { navigator.pop() }) {
        // Nothing running ↔ the progress swap in place (audit row 7).
        ContentSwap(status != null, label = "restoring") { running ->
            val current = status
            if (!running || current == null) {
                CivilightText(RestoreCopy.NOT_RUNNING, type.small, color = colors.ink3)
            } else {
                val ui = RestoringModel.of(current)
                Column {
                    SectionLabel(ui.section)
                    CivilightText(current.progress.title, type.pageTitle, Modifier.padding(top = 8.dp), color = colors.ink)
                    ProgressBar(ui.fraction, Modifier.padding(top = 18.dp), valueText = ui.bytes, estimateText = ui.estimate)
                    Column(Modifier.padding(top = 18.dp)) {
                        ui.steps.forEachIndexed { i, (title, sub) -> StepRow(title, sub, stepState(i, ui.current)) }
                    }
                    CivilightText(current.progress.note, type.small, Modifier.padding(top = 16.dp), color = colors.ink3)
                }
            }
        }
    }
}

/** A 20 dp radius-6 marker (done `acc` fill, current 2 dp `acc` inset, todo 1.5 dp `ink3`) and two lines. */
@Composable
private fun StepRow(title: String, sub: String, state: StepState) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(6.dp)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The marker changes fill and outline colour on `standard` (S6): todo → current → done.
        val fill by animatedFraction(state == StepState.DONE)
        val outline by animatedColor(if (state == StepState.UPCOMING) colors.ink3 else colors.acc)
        val stroke = if (state == StepState.UPCOMING) 1.5.dp else 2.dp
        Box(Modifier.size(20.dp).clip(shape).background(colors.acc.copy(alpha = colors.acc.alpha * fill)).insetOutline(outline, stroke, shape))
        Column(Modifier.weight(1f)) {
            val titleColor by animatedColor(if (state == StepState.UPCOMING) colors.ink3 else colors.ink)
            CivilightText(title, type.command, color = titleColor)
            CivilightText(sub, type.small, color = colors.ink3)
        }
    }
}
