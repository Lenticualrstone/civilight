package io.github.lenticualrstone.civilight.feature.recorder.put

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.recorder.put.DestinationStep
import io.github.lenticualrstone.civilight.domain.recorder.put.PutKind
import io.github.lenticualrstone.civilight.feature.recorder.PutSheet
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureProtection
import io.github.lenticualrstone.civilight.feature.recorder.capture.RecorderServices
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHost
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow

/**
 * `⋯ › 메모 칸에 넣기` sheet (LRC-37; not drawn): the project (LRC-32 protected destinations), then the
 * project's structure (장 · 에피소드 · 씬, indented as 단위 고르기). Tapping a scene appends the record
 * text as one line at the end of its `씬 메모`; a chapter or episode gets it at the end of its
 * description line. Toast TODO_COPY_LRC_16 · `되돌리기`. A hidden scene shown masked asks for the
 * prompt (`rememberHiddenGate`) and inserts only once its title shows.
 */
@Composable
fun PutMemoFieldContent(sheet: PutSheet, scope: SheetScope, services: RecorderServices, deps: FeatureDeps, host: SheetHost) {
    val library = rememberLibrary(services)
    val env = rememberEnv(services, deps) ?: return
    val step = rememberProjectStep(sheet, PutKind.MEMO_FIELD, library, env, deps)
    val current = step.current
    val chosen = (current as? DestinationStep.Chosen)?.projectKey
    val unlock by deps.unlock.state.collectAsState()
    val hiddenOpen = chosen?.let { unlock.itemsOpen(it) } == true
    val gate = chosen?.let { rememberHiddenGate(it) }
    val view = remember(env, current, hiddenOpen) { PutFlows.memoFieldView(env, current, hiddenOpen) }
    ProtectWindow(CaptureProtection.of(env, listOfNotNull(view.chosenProject) + view.projects.map { it.projectKey }))
    val colors = CivilightTheme.colors
    with(scope) {
        PutTitle(view.title, step)
        SheetScrollBody(Modifier.padding(top = 8.dp)) {
            when (current) {
                DestinationStep.ChoosingProject -> ProjectRows(view.projects, step)
                is DestinationStep.AwaitingAuth -> Unit
                is DestinationStep.Chosen -> for (row in view.units) {
                    ListRow(
                        title = row.title,
                        compact = true,
                        modifier = Modifier.padding(start = (row.depth * 16).dp),
                        onClick = {
                            if (row.locked) {
                                gate?.unlock()
                            } else {
                                insertAndToast(services, deps, host) { putInMemoField(sheet.recordKey, current.projectKey, row.unitKey) }
                            }
                        },
                        trailing = if (row.locked) {
                            { CivilightIcon(IconId.Lock, contentDescription = null, size = 16.dp, tint = colors.ink3) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}
