package io.github.lenticualrstone.civilight.feature.recorder.put

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SearchField
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.put.DestinationStep
import io.github.lenticualrstone.civilight.domain.recorder.put.PutKind
import io.github.lenticualrstone.civilight.feature.recorder.PutSheet
import io.github.lenticualrstone.civilight.feature.recorder.RecorderUiCopy
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureProtection
import io.github.lenticualrstone.civilight.feature.recorder.capture.RecorderServices
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHost
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow

/**
 * `카드에 넣기` sheet (LRC-35; not drawn). Steps: the project (LRC-32 protected destinations), the
 * card (search by name or 초성, locked hidden cards left out; each row names its kind), then the
 * card's tabs: tapping a tab appends a `⌁ 작가 메모` paragraph at its end at once (toast
 * TODO_COPY_LRC_13 · `되돌리기`). The chosen card is drawn as a selected row; tapping it goes back to
 * the list. The step, query and card are saveable.
 */
@Composable
fun PutCardContent(sheet: PutSheet, scope: SheetScope, services: RecorderServices, deps: FeatureDeps, host: SheetHost) {
    val library = rememberLibrary(services)
    val env = rememberEnv(services, deps) ?: return
    val step = rememberProjectStep(sheet, PutKind.CARD, library, env, deps)
    var query by rememberSaveable(sheet, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var cardKind by rememberSaveable(sheet) { mutableStateOf<String?>(null) }
    var cardKey by rememberSaveable(sheet) { mutableStateOf<String?>(null) }
    val current = step.current
    val chosenProject = (current as? DestinationStep.Chosen)?.projectKey
    val card = chosenProject?.let { p ->
        val kind = cardKind?.let { k -> runCatching { RecorderTargetKind.valueOf(k) }.getOrNull() }
        val key = cardKey
        if (kind != null && key != null) TargetRef(p, kind, key) else null
    }
    val view = remember(env, current, query.text, card) { PutFlows.cardView(env, current, query.text, card) }
    ProtectWindow(CaptureProtection.of(env, listOfNotNull(view.chosenProject) + view.projects.map { it.projectKey }))
    val colors = CivilightTheme.colors
    with(scope) {
        PutTitle(view.title, ProjectStep(current) { key ->
            cardKind = null
            cardKey = null
            if (key == null) step.reopenMenu() else step.choose(key)
        })
        when (current) {
            DestinationStep.ChoosingProject -> SheetScrollBody(Modifier.padding(top = 8.dp)) { ProjectRows(view.projects, step) }
            is DestinationStep.AwaitingAuth -> Unit
            is DestinationStep.Chosen -> {
                val chosen = view.card
                if (chosen == null) {
                    SearchField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.padding(top = 10.dp),
                        placeholder = RecorderUiCopy.TODO_CARD_SEARCH,
                    )
                    SheetScrollBody(Modifier.padding(top = 6.dp)) {
                        for (row in view.cards) {
                            ListRow(
                                title = row.choice.name,
                                subtitle = row.kindWord,
                                compact = true,
                                onClick = {
                                    cardKind = row.choice.target.kind.name
                                    cardKey = row.choice.target.key
                                },
                            )
                        }
                    }
                } else {
                    SheetScrollBody(Modifier.padding(top = 8.dp)) {
                        ListRow(
                            title = chosen.choice.name,
                            subtitle = chosen.kindWord,
                            compact = true,
                            selected = true,
                            onClick = {
                                cardKind = null
                                cardKey = null
                            },
                            trailing = { CivilightIcon(IconId.Check, contentDescription = null, size = 18.dp, tint = colors.acc) },
                        )
                        for (tab in view.tabs) {
                            ListRow(
                                title = tab.label,
                                compact = true,
                                onClick = {
                                    insertAndToast(services, deps, host) { putInCard(sheet.recordKey, current.projectKey, chosen.choice.target, tab.id) }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
