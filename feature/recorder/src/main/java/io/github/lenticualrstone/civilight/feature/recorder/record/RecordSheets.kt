package io.github.lenticualrstone.civilight.feature.recorder.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.feature.recorder.DeletedTargetSheet
import io.github.lenticualrstone.civilight.feature.recorder.RecordLinkSheet
import io.github.lenticualrstone.civilight.feature.recorder.capture.RecorderPort
import io.github.lenticualrstone.civilight.feature.recorder.link.LinkPickerBody
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHost
import kotlinx.coroutines.launch

/**
 * The link picker over the record (LRC-20): adds a link, replaces a chip's links (`칩 다시 고르기`,
 * a chip tap) or re-points `@` runs (`다른 항목에 잇기`), as [RecordLinkPicks.mode] reads the key.
 */
@Composable
fun RecordLinkPickContent(sheet: RecordLinkSheet, scope: SheetScope, c: RecordController, recorder: RecorderPort, deps: FeatureDeps) {
    val env by c.env.collectAsState()
    val flow = remember(recorder) { recorder.observe() }
    val library by flow.collectAsState(initial = recorder.current())
    val e = env ?: return
    val record = library.record(sheet.recordKey)
    if (record == null) {
        LaunchedEffect(sheet) { scope.close() }
        return
    }
    val mode = RecordLinkPicks.mode(sheet)
    with(scope) {
        LinkPickerBody(
            env = e,
            links = library.linksOf(record.key),
            recordText = Runs.text(record.runs),
            onPick = { candidate ->
                deps.appScope.launch { RecordLinkPicks.apply(c, mode, candidate.target) }
                scope.close()
            },
            onClose = scope::close,
        )
    }
}

/**
 * The card of a deleted target's chip (LRC-23, rule 20): title `지운 인물 · 노아`, then its buttons in
 * order (`칩 다시 고르기 · 칩 빼기 · 이대로 두기`, or for an `@` chip `다른 항목에 잇기 · 글로 풀기 ·
 * 되살리기 · 이대로 두기`). `이대로 두기` closes and keeps the dotted chip. A restore shows its toast.
 */
@Composable
fun DeletedTargetContent(sheet: DeletedTargetSheet, scope: SheetScope, c: RecordController, deps: FeatureDeps, host: SheetHost) {
    val status by c.status.collectAsState()
    val card = remember(status, sheet) { c.chipOf(sheet.linkKey)?.let(c::cardOf) }
    if (card == null) {
        // Restored or relinked meanwhile: the chip is a live one again.
        LaunchedEffect(sheet) { scope.close() }
        return
    }
    with(scope) {
        SheetHeader(title = card.title, onClose = scope::close, closeLabel = RecorderCopy.CLOSE)
        Column(Modifier.fillMaxWidth().padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (action in card.actions) {
                CivilightButton(
                    action,
                    {
                        if (action == RecorderCopy.KEEP_AS_IS) {
                            scope.close()
                        } else {
                            deps.appScope.launch {
                                when (val outcome = c.onCard(sheet.linkKey, action)) {
                                    CardOutcome.Close -> host.close()
                                    is CardOutcome.OpenPicker -> host.replace(RecordLinkPicks.sheetFor(sheet.recordKey, outcome))
                                    is CardOutcome.Restored -> {
                                        host.close()
                                        if (outcome.text.isNotEmpty()) deps.toasts.show(outcome.text, actionLabel = null)
                                    }
                                }
                            }
                        }
                    },
                    Modifier.fillMaxWidth(),
                    variant = ButtonVariant.GHOST,
                )
            }
        }
    }
}
