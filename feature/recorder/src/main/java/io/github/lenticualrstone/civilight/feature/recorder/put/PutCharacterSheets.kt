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
import io.github.lenticualrstone.civilight.domain.recorder.put.DestinationStep
import io.github.lenticualrstone.civilight.domain.recorder.put.PutKind
import io.github.lenticualrstone.civilight.feature.recorder.PutSheet
import io.github.lenticualrstone.civilight.feature.recorder.RecorderUiCopy
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureProtection
import io.github.lenticualrstone.civilight.feature.recorder.capture.RecorderServices
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHost
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow

/**
 * `⋯ › 인물로 만들기` sheet (LRC-38; not drawn, `EmptyCast` names the entry): the project (LRC-32),
 * then `이름` (required, empty at start `[PROPOSED]`) and the confirm button
 * (TODO_COPY_LRC_CHARACTER_BUTTON). The cast area's rule makes the character: `한 줄 소개` = the
 * record's first sentence, a `⌁ 작가 메모` paragraph at the end of `개요`. Toast TODO_COPY_LRC_17 ·
 * `되돌리기`. The typed name is saveable.
 */
@Composable
fun PutCharacterContent(sheet: PutSheet, scope: SheetScope, services: RecorderServices, deps: FeatureDeps, host: SheetHost) {
    val library = rememberLibrary(services)
    val env = rememberEnv(services, deps) ?: return
    val step = rememberProjectStep(sheet, PutKind.CHARACTER, library, env, deps)
    val current = step.current
    val view = remember(env, current) { PutFlows.characterView(env, current) }
    var name by rememberSaveable(sheet) { mutableStateOf("") }
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
                )
                SheetActions(Modifier.padding(top = 16.dp)) {
                    CivilightButton(
                        RecorderUiCopy.TODO_CHARACTER_BUTTON,
                        { insertAndToast(services, deps, host) { makeCharacter(sheet.recordKey, current.projectKey, name) } },
                        Modifier.weight(1f),
                        enabled = view.canMake(name),
                        variant = ButtonVariant.PRIMARY,
                    )
                }
            }
        }
    }
}
