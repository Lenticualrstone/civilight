package io.github.lenticualrstone.civilight.feature.recorder.put

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.ShownCopy
import io.github.lenticualrstone.civilight.domain.recorder.put.DestinationStep
import io.github.lenticualrstone.civilight.domain.recorder.put.PutKind
import io.github.lenticualrstone.civilight.feature.recorder.PutSheet
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureProtection
import io.github.lenticualrstone.civilight.feature.recorder.capture.RecorderServices
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHost
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow

/**
 * `페이지로 만들기` sheet (LRC-36; not drawn): the project (LRC-32), then `이름` prefilled with the
 * start of the record's first sentence (`ED`, editable; left blank it falls back to the prefill) and
 * the confirm button (TODO_COPY_LRC_14). The page holds the copied text; toast TODO_COPY_LRC_15 ·
 * `되돌리기`. The typed name is saveable.
 */
@Composable
fun PutPageContent(sheet: PutSheet, scope: SheetScope, services: RecorderServices, deps: FeatureDeps, host: SheetHost) {
    val library = rememberLibrary(services)
    val env = rememberEnv(services, deps) ?: return
    val step = rememberProjectStep(sheet, PutKind.PAGE, library, env, deps)
    val current = step.current
    val view = remember(library, env, current, sheet) { PutFlows.pageView(library, sheet.recordKey, env, current) }
    var name by rememberSaveable(sheet) { mutableStateOf(view.prefill) }
    ProtectWindow(CaptureProtection.of(env, listOfNotNull(view.chosenProject) + view.projects.map { it.projectKey }))
    with(scope) {
        PutTitle(view.title, step)
        when (current) {
            DestinationStep.ChoosingProject -> SheetScrollBody(Modifier.padding(top = 8.dp)) { ProjectRows(view.projects, step) }
            is DestinationStep.AwaitingAuth -> Unit
            is DestinationStep.Chosen -> {
                CivilightTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.padding(top = 12.dp),
                    label = RecorderCopy.NAME,
                    placeholder = view.prefill.takeIf { it.isNotEmpty() },
                )
                SheetActions(Modifier.padding(top = 16.dp)) {
                    CivilightButton(
                        ShownCopy.PAGE_BUTTON,
                        { insertAndToast(services, deps, host) { makePage(sheet.recordKey, current.projectKey, name) } },
                        Modifier.weight(1f),
                        enabled = view.canMake(name),
                        variant = ButtonVariant.PRIMARY,
                    )
                }
            }
        }
    }
}
