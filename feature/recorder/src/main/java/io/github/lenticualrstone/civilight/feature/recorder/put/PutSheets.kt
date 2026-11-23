package io.github.lenticualrstone.civilight.feature.recorder.put

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.ShownCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.put.DestinationOption
import io.github.lenticualrstone.civilight.domain.recorder.put.DestinationStep
import io.github.lenticualrstone.civilight.domain.recorder.put.Destinations
import io.github.lenticualrstone.civilight.domain.recorder.put.ManuscriptSpot
import io.github.lenticualrstone.civilight.domain.recorder.put.PositionRow
import io.github.lenticualrstone.civilight.domain.recorder.put.PutKind
import io.github.lenticualrstone.civilight.feature.recorder.PutScenePickSheet
import io.github.lenticualrstone.civilight.feature.recorder.PutSheet
import io.github.lenticualrstone.civilight.feature.recorder.RecorderUiCopy
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureProtection
import io.github.lenticualrstone.civilight.feature.recorder.capture.RecorderServices
import io.github.lenticualrstone.civilight.feature.recorder.record.PutDone
import io.github.lenticualrstone.civilight.feature.recorder.record.RecordWriter
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.picker.UnitPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHost
import io.github.lenticualrstone.civilight.ui.security.AuthCopy
import io.github.lenticualrstone.civilight.ui.security.AuthRequest
import io.github.lenticualrstone.civilight.ui.security.AuthResult
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import kotlinx.coroutines.launch

/** The env for a 넣기 sheet, reloaded when the unlock state changes (a destination prompt can open the hidden group). */
@Composable
internal fun rememberEnv(services: RecorderServices, deps: FeatureDeps): RecorderEnv? {
    val unlock by deps.unlock.state.collectAsState()
    var env by remember { mutableStateOf<RecorderEnv?>(null) }
    LaunchedEffect(unlock) { env = services.envSource.load() }
    return env
}

/** The library as it changes, for a sheet. */
@Composable
internal fun rememberLibrary(services: RecorderServices): RecorderLibrary {
    val flow = remember(services) { services.recorder.observe() }
    val library by flow.collectAsState(initial = services.recorder.current())
    return library
}

/**
 * Runs one 넣기 in the app scope, then closes the sheets and shows its toast with `되돌리기` (one
 * action: the toast host has one, §6 Q4). A refused insert (read-only project, the spot or card
 * gone) leaves the sheet open.
 */
fun insertAndToast(services: RecorderServices, deps: FeatureDeps, host: SheetHost, put: suspend RecordWriter.() -> PutDone?) {
    deps.appScope.launch {
        val done = services.writer.put() ?: return@launch
        host.closeAll()
        deps.toasts.show(ShownCopy.of(done.toast), RecorderCopy.UNDO) { deps.appScope.launch { services.writer.undoPut(done) } }
    }
}

/** `원고에 넣기` at a spot (LRC-33). */
fun insertAndToast(services: RecorderServices, deps: FeatureDeps, host: SheetHost, recordKey: Key, projectKey: Key, spot: ManuscriptSpot) =
    insertAndToast(services, deps, host) { putInManuscript(recordKey, projectKey, spot) }

/**
 * The project step every 넣기 sheet starts with (LRC-32): the chosen project and the project whose
 * prompt passed are saveable (a stacked sheet and process death keep them; a passed prompt is not
 * asked again). Choosing a locked hidden or R-18 project runs the system prompt; cancel or failure
 * leaves the choice unmade and the menu open.
 */
class ProjectStep internal constructor(
    val current: DestinationStep,
    private val setProject: (Key?) -> Unit,
) {
    /** A row of the project menu chosen. */
    fun choose(projectKey: Key) = setProject(projectKey)

    /** The title's `▾`: back to the project menu. */
    fun reopenMenu() = setProject(null)
}

@Composable
internal fun rememberProjectStep(sheet: PutSheet, kind: PutKind, library: RecorderLibrary, env: RecorderEnv, deps: FeatureDeps): ProjectStep {
    var projectKey by rememberSaveable(sheet) { mutableStateOf(PutFlows.initialProject(library, sheet.recordKey, env, sheet.projectKey)) }
    var authedKey by rememberSaveable(sheet) { mutableStateOf<String?>(null) }
    val current = PutFlows.step(env, projectKey, authedKey, kind)
    LaunchedEffect(current) {
        val awaiting = current as? DestinationStep.AwaitingAuth ?: return@LaunchedEffect
        val hiddenLocked = env.project(awaiting.projectKey)?.let(env::isLockedHidden) == true
        val title = if (hiddenLocked) AuthCopy.HIDDEN_PROJECTS else RecorderUiCopy.PROTECTED_PROMPT
        val ok = deps.biometric.authenticate(AuthRequest(title)) == AuthResult.SUCCESS
        // The destination's rows read its structure, so the hidden group opens with the prompt (Defaults used).
        if (ok && hiddenLocked) deps.unlock.unlockHiddenGroup()
        when (Destinations.authResult(awaiting, ok)) {
            is DestinationStep.Chosen -> authedKey = awaiting.projectKey
            else -> projectKey = null
        }
    }
    return ProjectStep(current) { projectKey = it }
}

/** The title line of a 넣기 sheet: `<kind> · <project> ▾` (tap = the project menu again) and `닫기`. */
@Composable
internal fun SheetScope.PutTitle(title: String, step: ProjectStep) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val switchable = step.current is DestinationStep.Chosen
        CivilightText(
            title, CivilightTheme.type.title,
            Modifier
                .weight(1f)
                .then(if (switchable) Modifier.pressable(PressAction.Click({ step.reopenMenu() }), scaleOnPress = false) else Modifier),
            color = CivilightTheme.colors.ink, maxLines = 2,
        )
        TextAction(RecorderCopy.CLOSE, { this@PutTitle.close() })
    }
}

/** The project menu rows (`숨긴 프로젝트` while locked; choosing a protected one runs the prompt). */
@Composable
internal fun ColumnScope.ProjectRows(projects: List<DestinationOption>, step: ProjectStep) {
    for (option in projects) ListRow(title = option.label, compact = true, onClick = { step.choose(option.projectKey) })
}

/** A [PutSheet] of any of the five kinds; an unknown kind (a key saved by another build) closes. */
@Composable
fun PutSheetContent(sheet: PutSheet, scope: SheetScope, services: RecorderServices, deps: FeatureDeps, host: SheetHost) {
    when (runCatching { PutKind.valueOf(sheet.kind) }.getOrNull()) {
        PutKind.MANUSCRIPT -> PutManuscriptContent(sheet, scope, services, deps, host)
        PutKind.CARD -> PutCardContent(sheet, scope, services, deps, host)
        PutKind.PAGE -> PutPageContent(sheet, scope, services, deps, host)
        PutKind.MEMO_FIELD -> PutMemoFieldContent(sheet, scope, services, deps, host)
        PutKind.CHARACTER -> PutCharacterContent(sheet, scope, services, deps, host)
        null -> LaunchedEffect(sheet) { scope.close() }
    }
}

/**
 * `원고에 넣기` position sheet (LRC-33; not drawn, copy from rule 23 §4). The title line
 * `원고에 넣기 · <project> ▾` switches the destination; choosing a locked hidden or R-18 project runs
 * the system prompt first and cancel leaves the choice unmade (LRC-32). Rows are facts, none
 * preselected; tapping `연결된 씬 · …` or `마지막으로 쓰던 곳 · …` inserts at once, `씬 고르기 ›` lists
 * the scenes, `이미 넣음 · …` opens the script at that block.
 */
@Composable
fun PutManuscriptContent(sheet: PutSheet, scope: SheetScope, services: RecorderServices, deps: FeatureDeps, host: SheetHost) {
    val library = rememberLibrary(services)
    val env = rememberEnv(services, deps) ?: return
    val step = rememberProjectStep(sheet, PutKind.MANUSCRIPT, library, env, deps)
    val current = step.current
    val view = remember(library, env, current, sheet) { PutFlows.view(library, sheet.recordKey, env, current) }
    ProtectWindow(CaptureProtection.of(env, listOfNotNull(view.chosenProject) + view.projects.map { it.projectKey }))
    with(scope) {
        PutTitle(view.title, step)
        SheetScrollBody(Modifier.padding(top = 8.dp)) {
            when (current) {
                DestinationStep.ChoosingProject -> ProjectRows(view.projects, step)
                is DestinationStep.AwaitingAuth -> Unit
                is DestinationStep.Chosen -> for (row in view.rows) {
                    ListRow(
                        title = row.label,
                        compact = true,
                        onClick = {
                            val spot = row.spot
                            when {
                                spot != null -> insertAndToast(services, deps, host, sheet.recordKey, current.projectKey, spot)
                                row is PositionRow.PickScene -> scope.open(PutScenePickSheet(sheet.recordKey, current.projectKey))
                                row is PositionRow.AlreadyPut -> {
                                    host.closeAll()
                                    deps.navigator.push(Script(current.projectKey, blockKey = row.blockKey))
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

/** `씬 고르기 ›` (LRC-33 `[PROPOSED]`): the project's scenes; choosing one puts the memo at its end. */
@Composable
fun PutScenePickContent(sheet: PutScenePickSheet, scope: SheetScope, services: RecorderServices, deps: FeatureDeps, host: SheetHost) {
    val env = rememberEnv(services, deps) ?: return
    val doc = env.project(sheet.projectKey)?.doc
    if (doc == null) {
        LaunchedEffect(sheet) { scope.close() }
        return
    }
    val levels = remember(doc) { doc.index.allScenesInOrder.map { it.level }.toSet() }
    ProtectWindow(CaptureProtection.of(env, listOf(sheet.projectKey)))
    with(scope) {
        UnitPickerSheet(
            doc = doc,
            gate = rememberHiddenGate(sheet.projectKey),
            title = RecorderUiCopy.PICK_SCENE_TITLE,
            onPick = { sceneKey -> insertAndToast(services, deps, host, sheet.recordKey, sheet.projectKey, ManuscriptSpot.SceneEnd(sceneKey)) },
            onClose = scope::close,
            levels = levels,
        )
    }
}
