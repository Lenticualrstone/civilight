package io.github.lenticualrstone.civilight.feature.recorder.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.inbox.MenuAction
import io.github.lenticualrstone.civilight.feature.recorder.record.RecordMenuSource
import io.github.lenticualrstone.civilight.feature.recorder.record.RecordMenus
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.feature.recorder.PutSheet
import io.github.lenticualrstone.civilight.feature.recorder.RecordLinkSheet
import io.github.lenticualrstone.civilight.feature.recorder.RecordMenuSheet
import io.github.lenticualrstone.civilight.feature.recorder.RecorderUiCopy
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureProtection
import io.github.lenticualrstone.civilight.feature.recorder.link.LinkPickerBody
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHost
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import kotlinx.coroutines.launch

/** The icon of each `⋯` option (DS-12; the board draws the four it shows with `ink2` 20 px glyphs). */
private fun iconOf(action: MenuAction): IconId = when (action) {
    MenuAction.PUT_MANUSCRIPT -> IconId.Paper
    MenuAction.PUT_CARD -> IconId.Book
    MenuAction.MAKE_PAGE -> IconId.File
    MenuAction.PUT_MEMO_FIELD -> IconId.Pencil
    MenuAction.MAKE_CHARACTER -> IconId.Plus
    MenuAction.MARK_DONE -> IconId.Check
    MenuAction.UNMARK_DONE -> IconId.Undo
    MenuAction.HIDE -> IconId.EyeOff
    MenuAction.UNHIDE -> IconId.Unlock
    MenuAction.DELETE -> IconId.Trash
}

/**
 * The record `⋯` sheet (LRC-29, board `LrHide`): the preview card (`음성 0:24 · 오늘 08:41` and an
 * excerpt), then the options in the table's order with their subs. Nothing is preselected.
 * A 넣기 option without text or destination is drawn disabled ([MenuOptionView.enabled]).
 */
@Composable
fun RecordMenuContent(sheet: RecordMenuSheet, scope: SheetScope, source: RecordMenuSource, refresh: Any?, deps: FeatureDeps, host: SheetHost) {
    val menu = remember(refresh, sheet) { source.menu(sheet.recordKey) }
    if (menu == null) {
        // The record went (deleted elsewhere, hidden and locked): nothing to act on.
        LaunchedEffect(sheet) { scope.close() }
        return
    }
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    with(scope) {
        Column(
            Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.tint)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            CivilightText(menu.preview.meta, type.small, color = colors.ink3, maxLines = 1)
            CivilightText(
                menu.preview.excerpt,
                type.line.merge(TextStyle(fontSize = 15.sp, lineHeight = 24.sp)),
                Modifier.padding(top = 4.dp), color = colors.ink, maxLines = 2,
            )
        }
        SheetScrollBody(Modifier.padding(top = 10.dp)) {
            for (item in menu.options) {
                val action = item.option.action
                ListRow(
                    title = item.option.title,
                    subtitle = item.option.sub,
                    modifier = if (item.enabled) Modifier else Modifier.alpha(DISABLED_ALPHA),
                    leading = { CivilightIcon(iconOf(action), contentDescription = null, size = 20.dp, tint = colors.ink2) },
                    onClick = if (!item.enabled) null else ({
                        val put = RecordMenus.putKindOf(action)
                        if (put != null) {
                            host.replace(PutSheet(sheet.recordKey, put.name))
                        } else {
                            scope.close()
                            deps.appScope.launch {
                                when (val outcome = source.onMenu(sheet.recordKey, action)) {
                                    is MenuOutcome.ShowToast -> deps.toasts.show(outcome.toast.text, RecorderCopy.UNDO) {
                                        deps.appScope.launch { outcome.undo() }
                                    }
                                    is MenuOutcome.OpenPut, MenuOutcome.Done -> Unit
                                }
                            }
                        }
                    }),
                )
            }
        }
    }
}

/**
 * The `프로젝트 ▾` menu (LRC-27): `모든 프로젝트` · each project in 서재 order · `연결 없음`; locked
 * hidden projects are not listed, masked ones read their rating. The chosen entry is checked.
 */
@Composable
fun ProjectFilterContent(scope: SheetScope, c: InboxController) {
    val env by c.env.collectAsState()
    val items = remember(env, c.view.collectAsState().value?.filter) { c.projectMenu() }
    env?.let { e -> ProtectWindow(CaptureProtection.of(e, items.mapNotNull { (it.filter as? io.github.lenticualrstone.civilight.model.recorder.ProjectFilter.Project)?.projectKey })) }
    with(scope) {
        SheetHeader(title = RecorderUiCopy.KIND_PROJECT, onClose = scope::close)
        SheetScrollBody(Modifier.padding(top = 8.dp)) {
            for (item in items) {
                ListRow(
                    title = item.label,
                    compact = true,
                    onClick = {
                        c.setProject(item.filter)
                        scope.close()
                    },
                    trailing = if (item.selected) ({ CivilightIcon(IconId.Check, contentDescription = null, size = 18.dp, tint = CivilightTheme.colors.acc) }) else null,
                )
            }
        }
    }
}

/** `+ 연결` on a row (LRC-20 over a saved record): the shared picker body; a pick adds a link of origin `직접`. */
@Composable
fun RecordLinkContent(sheet: RecordLinkSheet, scope: SheetScope, library: RecorderLibrary, env: RecorderEnv?, deps: FeatureDeps, onPick: suspend (io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef) -> Unit) {
    val e = env ?: return
    val record = library.record(sheet.recordKey)
    if (record == null) {
        LaunchedEffect(sheet) { scope.close() }
        return
    }
    with(scope) {
        LinkPickerBody(
            env = e,
            links = library.linksOf(record.key),
            recordText = Runs.text(record.runs),
            onPick = { candidate ->
                deps.appScope.launch { onPick(candidate.target) }
                scope.close()
            },
            onClose = scope::close,
        )
    }
}
