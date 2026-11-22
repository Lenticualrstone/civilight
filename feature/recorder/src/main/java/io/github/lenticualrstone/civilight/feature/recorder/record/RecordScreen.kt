package io.github.lenticualrstone.civilight.feature.recorder.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.put.PutKind
import io.github.lenticualrstone.civilight.domain.recorder.state.PlacementStatus
import io.github.lenticualrstone.civilight.feature.recorder.DeletedTargetSheet
import io.github.lenticualrstone.civilight.feature.recorder.PutScenePickSheet
import io.github.lenticualrstone.civilight.feature.recorder.PutSheet
import io.github.lenticualrstone.civilight.feature.recorder.RecordLinkSheet
import io.github.lenticualrstone.civilight.feature.recorder.RecordMenuSheet
import io.github.lenticualrstone.civilight.feature.recorder.RecorderUiCopy
import io.github.lenticualrstone.civilight.feature.recorder.audio.PlayerRow
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureProtection
import io.github.lenticualrstone.civilight.feature.recorder.inbox.ActionTag
import io.github.lenticualrstone.civilight.feature.recorder.inbox.RecordMenuContent
import io.github.lenticualrstone.civilight.feature.recorder.link.LinkChipRow
import io.github.lenticualrstone.civilight.feature.recorder.link.LinkPickerRows
import io.github.lenticualrstone.civilight.feature.recorder.memo.MemoField
import io.github.lenticualrstone.civilight.feature.recorder.put.PutScenePickContent
import io.github.lenticualrstone.civilight.feature.recorder.put.PutSheetContent
import io.github.lenticualrstone.civilight.navigation.RecorderRecordDetail
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.nav.LocalPaneRole
import io.github.lenticualrstone.civilight.ui.nav.PaneRole
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.security.AuthRequest
import io.github.lenticualrstone.civilight.ui.security.AuthResult
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import kotlinx.coroutines.launch

/**
 * 기록 화면 (LRC-28; not drawn, built from the `LrProcess` parts as the spec proposes): header kind +
 * duration and date, back (not in the detail pane beside the 기록함), the visible `⋯` button; the
 * player (LRC-14); the text, editable in place with autosave and `@` (LRC-17); the link chips with
 * `+ 연결`, a deleted card's chip opening its card (LRC-23); `넣은 곳` with `◇ 넣은 뒤 고침` and
 * `◇ 넣은 곳 지워짐` (LRC-39 marks); the actions `원고에 넣기` · `카드에 넣기` · `페이지로 만들기`.
 * A hidden record while hidden records are locked asks `숨긴 기록 보기` first (LRC-41).
 * On inner screens it is the right pane of `목록 + 상세`, or the 560 column of `가운데 한 칸` (LRC-31).
 */
@Composable
fun RecordDetailScreen(route: RecorderRecordDetail, deps: FeatureDeps) {
    val vm = entryViewModel { RecordViewModel(deps, route.recordKey) }
    val c = vm.controller
    val status by c.status.collectAsState()
    val env by c.env.collectAsState()
    val sheets = rememberSheetHost()
    val inDetailPane = LocalPaneRole.current == PaneRole.DETAIL
    val back = if (inDetailPane) null else IconAction(RecorderUiCopy.BACK) { deps.navigator.pop() }
    // The prompt `숨긴 기록 보기` runs on arrival only: once asked, or once the record was shown, a re-lock leaves just the row.
    var unlockAsked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(status is RecordStatus.Ready) { if (status is RecordStatus.Ready) unlockAsked = true }

    // Back from another screen (a 넣기 target, the script): projects may have changed. Leaving: the waiting edit is written.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> c.refresh()
                Lifecycle.Event.ON_PAUSE -> vm.flushSoon()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    when (val s = status) {
        RecordStatus.Loading -> CenterColumn { CivilightScreen(title = RecorderCopy.APP_NAME, back = back) {} }
        RecordStatus.Gone -> LaunchedEffect(route) { if (deps.navigator.current == route) deps.navigator.pop() }
        RecordStatus.Locked -> LockedRecord(deps, back, askOnArrival = !unlockAsked) { unlockAsked = true }
        is RecordStatus.Ready -> {
            val view = s.view
            env?.let { ProtectWindow(CaptureProtection.of(it, view.protectedProjects)) }
            val openPut = { kind: PutKind ->
                vm.flushSoon()
                sheets.open(PutSheet(route.recordKey, kind.name))
            }
            CenterColumn {
                CivilightScreen(
                    title = view.screen.kindLabel,
                    smallLine = view.screen.meta,
                    back = back,
                    actions = {
                        CivilightIconButton(IconId.More, RecorderCopy.TODO_MENU_ARIA, onClick = {
                            vm.flushSoon()
                            sheets.open(RecordMenuSheet(route.recordKey))
                        })
                    },
                    sheets = sheets,
                    localSheets = { key ->
                        when (key) {
                            is RecordMenuSheet -> { { RecordMenuContent(key, this, c, view, deps, sheets) } }
                            is PutSheet -> { { PutSheetContent(key, this, vm.services, deps, sheets) } }
                            is PutScenePickSheet -> { { PutScenePickContent(key, this, vm.services, deps, sheets) } }
                            is RecordLinkSheet -> { { RecordLinkPickContent(key, this, c, vm.services.recorder, deps) } }
                            is DeletedTargetSheet -> { { DeletedTargetContent(key, this, c, deps, sheets) } }
                            else -> null
                        }
                    },
                ) { padding ->
                    RecordBody(
                        view = view,
                        padding = padding,
                        c = c,
                        onAddLink = { sheets.open(RecordLinkSheet(route.recordKey)) },
                        onReplaceLink = { chip -> sheets.open(RecordLinkSheet(route.recordKey, replaceLinkKeys = chip.links.map { it.key })) },
                        onRemoveLink = { chip -> deps.appScope.launch { c.removeLinks(chip.links.map { it.key }) } },
                        onCard = { chip -> chip.links.firstOrNull()?.let { sheets.open(DeletedTargetSheet(route.recordKey, it.key)) } },
                        onPlacement = { route -> deps.navigator.push(route) },
                        onPut = openPut,
                        player = vm.services.player,
                    )
                }
            }
        }
    }
}

/** The record's content (thin: everything comes from [RecordView]). */
@Composable
private fun RecordBody(
    view: RecordView,
    padding: PaddingValues,
    c: RecordController,
    onAddLink: () -> Unit,
    onReplaceLink: (io.github.lenticualrstone.civilight.domain.recorder.link.Chip) -> Unit,
    onRemoveLink: (io.github.lenticualrstone.civilight.domain.recorder.link.Chip) -> Unit,
    onCard: (io.github.lenticualrstone.civilight.domain.recorder.link.Chip) -> Unit,
    onPlacement: (io.github.lenticualrstone.civilight.navigation.Route) -> Unit,
    onPut: (PutKind) -> Unit,
    player: io.github.lenticualrstone.civilight.feature.recorder.audio.RecordPlayer,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val side = if (LocalWindowLayout.current.isInner) 28.dp else 24.dp
    val screen = view.screen
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = side, end = side, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
    ) {
        view.audio?.let { audio -> PlayerRow(screen.recordKey, audio, player, Modifier.padding(bottom = 14.dp)) }
        MemoField(
            text = view.text,
            caret = view.caret,
            reload = view.reload,
            marks = view.marks,
            onChange = c::edit,
            onCaret = c::caretMoved,
            textStyle = type.recorderLive,
            boxed = false,
        )
        if (view.atOpen && view.atCandidates.isNotEmpty()) {
            Column(Modifier.padding(top = 6.dp)) {
                for (candidate in view.atCandidates) {
                    val row = LinkPickerRows.row(candidate)
                    ListRow(title = row.title, subtitle = row.sub.takeIf { it.isNotEmpty() }, compact = true, onClick = { c.pickAt(candidate) })
                }
            }
        }
        LinkChipRow(
            chips = screen.chips,
            onAdd = onAddLink,
            onReplace = onReplaceLink,
            onRemove = onRemoveLink,
            modifier = Modifier.padding(top = 22.dp),
            hasCard = { chip -> c.cardOf(chip) != null },
            onCard = onCard,
        )
        if (view.placements.isNotEmpty() || screen.placementsGoneMark != null) {
            SectionLabel(screen.placementsLabel, Modifier.padding(top = 28.dp, bottom = 4.dp))
            for (placement in view.placements) PlacementLine(placement, onPlacement)
            screen.placementsGoneMark?.let { mark ->
                CivilightText(mark, type.small, Modifier.padding(top = 8.dp), color = colors.ink3)
            }
        }
        ChipFlow(Modifier.padding(top = 24.dp)) {
            for (label in screen.actions) {
                when (label) {
                    RecorderCopy.PUT_MANUSCRIPT -> ActionTag(label, PutKind.MANUSCRIPT in view.puts) { onPut(PutKind.MANUSCRIPT) }
                    RecorderCopy.PUT_CARD -> ActionTag(label, PutKind.CARD in view.puts) { onPut(PutKind.CARD) }
                    RecorderCopy.MAKE_PAGE -> ActionTag(label, PutKind.PAGE in view.puts) { onPut(PutKind.PAGE) }
                    else -> ActionTag(label, false) {}
                }
            }
        }
    }
}

/**
 * One `넣은 곳` row: its label (` · 휴지통` while the copy is in the trash), `◇ 넣은 뒤 고침` at the
 * right when the text changed after the copy; a copy gone for good is plain `ink3` text. A live copy
 * opens where it is (script block, card tab, page, scene memo, structure, profile).
 */
@Composable
private fun PlacementLine(placement: PlacementView, onOpen: (io.github.lenticualrstone.civilight.navigation.Route) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val row = placement.row
    val target = placement.route
    Row(
        Modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .then(if (target != null) Modifier.pressable(PressAction.Click({ onOpen(target) }), scaleOnPress = false) else Modifier)
            .heightIn(min = 44.dp)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val gone = row.status == PlacementStatus.GONE
        CivilightText(row.text, if (gone) type.small else type.itemTitleCompact, Modifier.weight(1f), color = if (gone) colors.ink3 else colors.ink, maxLines = 2)
        row.mark?.let { CivilightText(it, type.small, color = colors.ink3, maxLines = 1) }
        if (target != null) CivilightText("›", type.small, color = colors.ink2, maxLines = 1)
    }
}

/**
 * A hidden record while hidden records are locked (LRC-41): the record's content stays out; the
 * prompt `숨긴 기록 보기` runs on arrival ([askOnArrival]) and the visible `지문으로 보기` row asks
 * again. A re-lock while the record is shown leaves only the row. Cancel or failure changes nothing
 * and shows nothing (LIB-37).
 */
@Composable
private fun LockedRecord(deps: FeatureDeps, back: IconAction?, askOnArrival: Boolean, onAsked: () -> Unit) {
    val ask = {
        deps.appScope.launch {
            if (deps.biometric.authenticate(AuthRequest(RecorderCopy.BIOMETRIC_TITLE)) == AuthResult.SUCCESS) deps.unlock.unlockRecords()
        }
        Unit
    }
    LaunchedEffect(Unit) {
        if (askOnArrival) {
            onAsked()
            ask()
        }
    }
    val side = if (LocalWindowLayout.current.isInner) 28.dp else 24.dp
    CenterColumn {
        CivilightScreen(title = RecorderCopy.APP_NAME, smallLine = RecorderCopy.LIBRARY_LINE, back = back) { padding ->
            LockRow(
                title = RecorderCopy.HIDDEN_RECORDS,
                actionLabel = RecorderCopy.OPEN_WITH_FINGERPRINT,
                onAction = ask,
                subtitle = RecorderCopy.HIDDEN_GROUP_SUB,
                modifier = Modifier.padding(start = side, end = side, top = padding.calculateTopPadding() + 14.dp),
            )
        }
    }
}
