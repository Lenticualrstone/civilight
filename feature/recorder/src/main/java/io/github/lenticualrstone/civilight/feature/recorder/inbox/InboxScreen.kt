package io.github.lenticualrstone.civilight.feature.recorder.inbox

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.lenticualrstone.civilight.designsystem.component.BottomGlassBar
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.SearchField
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.inbox.FilterAction
import io.github.lenticualrstone.civilight.domain.recorder.inbox.OpenGroup
import io.github.lenticualrstone.civilight.feature.recorder.PutSheet
import io.github.lenticualrstone.civilight.feature.recorder.PutScenePickSheet
import io.github.lenticualrstone.civilight.feature.recorder.ProjectFilterSheet
import io.github.lenticualrstone.civilight.feature.recorder.RecordLinkSheet
import io.github.lenticualrstone.civilight.feature.recorder.RecordMenuSheet
import io.github.lenticualrstone.civilight.feature.recorder.RecorderUiCopy
import io.github.lenticualrstone.civilight.feature.recorder.audio.RecordPlayer
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureProtection
import io.github.lenticualrstone.civilight.feature.recorder.capture.EntryRoutes
import io.github.lenticualrstone.civilight.feature.recorder.put.PutSheetContent
import io.github.lenticualrstone.civilight.feature.recorder.put.PutScenePickContent
import io.github.lenticualrstone.civilight.navigation.RecorderInbox
import io.github.lenticualrstone.civilight.navigation.RecorderRecordDetail
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.nav.openDetail
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.security.AuthRequest
import io.github.lenticualrstone.civilight.ui.security.AuthResult
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import io.github.lenticualrstone.civilight.ui.nav.LocalPaneRole
import io.github.lenticualrstone.civilight.ui.nav.PaneRole
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout

private const val ROW_KEY = "row:"
private const val OPEN_KEY = "open"

/**
 * 기록함 (LRC-25 … LRC-27, board `LrInbox`): glass header (`라이브러리 · 프로젝트 밖` · `라이프 레코더`,
 * back, the search icon), the filter row, the hidden-record group, the rows newest first and the
 * bottom bar `글로 쓰기` · `말로 기록`. With no record at all it shows `EmptyLr` without the bottom
 * bar. Rows open the record (`RecorderRecordDetail`, LRC-28) beside the list on inner screens
 * (`목록 + 상세`, the open record marked), or in the same 560 column (`가운데 한 칸`, LRC-31).
 */
@Composable
fun InboxScreen(route: RecorderInbox, deps: FeatureDeps) {
    val vm = entryViewModel { InboxViewModel(deps, route) }
    val c = vm.controller
    val view by c.view.collectAsState()
    val env by c.env.collectAsState()
    val libraryFlow = remember(vm) { vm.services.recorder.observe() }
    val library by libraryFlow.collectAsState(initial = vm.services.recorder.current())
    val sheets = rememberSheetHost()
    val listState = rememberLazyListState()
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }

    // Projects may have changed while another screen was on top (a 넣기, a delete, an unlock).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) c.refresh() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // Capture guard (LRC-22, LIB-26): the protections of the projects whose records are on screen now.
    val visible by remember {
        derivedStateOf {
            val keys = listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }
            keys.filter { it.startsWith(ROW_KEY) }.map { it.removePrefix(ROW_KEY) } +
                (if (OPEN_KEY in keys) view?.openRows.orEmpty().map { it.row.recordKey } else emptyList())
        }
    }
    val guarded = remember(library, visible, env) { c.protectedProjects(library, visible) }
    env?.let { ProtectWindow(CaptureProtection.of(it, guarded)) }

    val actions = InboxActions(deps, c, sheets::open, vm.services.player)

    // LRC-41 reveal: the hidden rows' text clears (blur 7 → 0, 320 ms) when the group opens on this
    // screen after its locked row was shown; `다시 잠그기` plays it backwards before locking. A group
    // already open on arrival (opened elsewhere) shows clear, and a re-lock from elsewhere is instant.
    // While the locked row shows, the progress waits at 0, so the group's first frame is the hidden look.
    val groupOpen = view?.screen?.openGroup != null
    val lockedShown = view?.screen?.lockedGroup != null
    var sawLocked by remember { mutableStateOf(false) }
    LaunchedEffect(lockedShown) { if (lockedShown) sawLocked = true }
    val revealLook = RevealMotion.look(CivilightTheme.reducedMotion)
    val reveal = remember { Animatable(1f) }
    LaunchedEffect(groupOpen, sawLocked) {
        when {
            !groupOpen -> reveal.snapTo(if (sawLocked) 0f else 1f)
            reveal.value < 1f -> reveal.animateTo(1f, tween(RevealMotion.durationMs(revealLook)))
        }
    }
    val relockScope = rememberCoroutineScope()
    fun relock() {
        relockScope.launch {
            reveal.animateTo(0f, tween(RevealMotion.durationMs(revealLook)))
            deps.unlock.relockNow()
        }
    }
    val current = view
    val layout = LocalWindowLayout.current
    val inner = layout.isInner

    // LRC-31 `목록 + 상세`: the record beside the list is marked; opening the inbox with nothing beside it selects the newest visible record (once per entry).
    val top = deps.navigator.backStack.lastOrNull()
    val selectedKey = if (LocalPaneRole.current == PaneRole.LIST) (top as? RecorderRecordDetail)?.recordKey else null
    var autoSelected by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(current?.rows?.firstOrNull()?.row?.recordKey, layout.twoPanes) {
        val rows = current?.rows.orEmpty()
        // Not used up by an empty first view (the library may load after the projects).
        if (!layout.twoPanes || rows.isEmpty() || autoSelected) return@LaunchedEffect
        val pick = InboxSelection.autoSelect(alreadyDone = false, inboxOnTop = deps.navigator.current == route, rows = rows)
        autoSelected = true
        pick?.let(actions::openRecord)
    }

    CenterColumn {
    CivilightScreen(
        title = RecorderCopy.APP_NAME,
        smallLine = RecorderCopy.LIBRARY_LINE,
        back = IconAction(RecorderUiCopy.BACK) { deps.navigator.pop() },
        actions = {
            if (current != null && !current.empty) {
                current.screen.headerAction?.let { label ->
                    CivilightButton(label, { relock() }, variant = ButtonVariant.COMPACT_GHOST, icon = IconId.Lock)
                }
                CivilightIconButton(IconId.Filter, RecorderUiCopy.TODO_SEARCH_ICON, onClick = {
                    searching = !searching
                    if (!searching) {
                        query = TextFieldValue("")
                        c.search("")
                    }
                })
            }
        },
        bottomBar = if (current != null && !current.empty) {
            {
                BottomGlassBar {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        CivilightButton(RecorderCopy.TO_TEXT, { actions.capture(current, typed = true) }, variant = ButtonVariant.GHOST, icon = IconId.Text)
                        CivilightButton(
                            RecorderCopy.RECORD_BY_VOICE, { actions.capture(current, typed = false) },
                            Modifier.weight(1f).heightIn(min = 56.dp), variant = ButtonVariant.PRIMARY_LARGE, icon = IconId.Mic,
                        )
                    }
                }
            }
        } else {
            null
        },
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                is RecordMenuSheet -> { { RecordMenuContent(key, this, c, view, deps, sheets) } }
                is PutSheet -> { { PutSheetContent(key, this, vm.services, deps, sheets) } }
                is PutScenePickSheet -> { { PutScenePickContent(key, this, vm.services, deps, sheets) } }
                is ProjectFilterSheet -> { { ProjectFilterContent(this, c) } }
                is RecordLinkSheet -> { { RecordLinkContent(key, this, library, env, deps) { target -> c.addLink(key.recordKey, target) } } }
                else -> null
            }
        },
    ) { padding ->
        if (current == null) return@CivilightScreen
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = if (inner) 28.dp else 24.dp, end = if (inner) 28.dp else 24.dp,
                top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            if (current.empty) {
                item(key = "empty") { EmptyInbox(current, actions) }
                return@LazyColumn
            }
            if (searching) {
                item(key = "search") {
                    SearchField(
                        value = query,
                        onValueChange = {
                            query = it
                            c.search(it.text)
                        },
                        modifier = Modifier.padding(bottom = 10.dp),
                        placeholder = RecorderUiCopy.TODO_SEARCH_PLACEHOLDER,
                        onClose = {
                            searching = false
                            query = TextFieldValue("")
                            c.search("")
                        },
                    )
                }
            }
            item(key = "filters") {
                ChipFlow {
                    for (chip in current.screen.filters) {
                        FilterChip(chip.label, chip.selected, onClick = {
                            if (chip.action == FilterAction.ProjectMenu) sheets.open(ProjectFilterSheet) else c.onFilter(chip.action)
                        })
                    }
                }
            }
            current.screen.lockedGroup?.let { group ->
                item(key = "locked") {
                    LockRow(
                        title = group.title, actionLabel = group.button, onAction = actions::openHidden,
                        modifier = Modifier.padding(top = 14.dp), subtitle = group.sub,
                    )
                }
            }
            current.screen.openGroup?.let { group ->
                item(key = OPEN_KEY) { OpenGroupBox(group, current.openRows, actions, Modifier.revealText(revealLook) { reveal.value }) }
            }
            if (current.filteredEmpty) {
                item(key = "filteredEmpty") {
                    CivilightText(RecorderCopy.TODO_FILTERED_EMPTY, CivilightTheme.type.small, Modifier.padding(top = 24.dp), color = CivilightTheme.colors.ink3)
                }
            }
            items(current.rows, key = { ROW_KEY + it.row.recordKey }) { row ->
                InboxRowItem(row, actions, Modifier.padding(top = if (row === current.rows.first()) 8.dp else 0.dp), selected = row.row.recordKey == selectedKey)
            }
        }
    }
    }
}

/** What a row and the screen's buttons do; writes run in the app scope so they finish after the screen leaves. */
class InboxActions(
    private val deps: FeatureDeps,
    private val c: InboxController,
    private val openSheet: (io.github.lenticualrstone.civilight.navigation.SheetKey) -> Unit,
    val player: RecordPlayer,
) {
    fun openRecord(recordKey: String) = deps.navigator.openDetail(RecorderRecordDetail(recordKey))

    fun menu(recordKey: String) = openSheet(RecordMenuSheet(recordKey))

    fun put(recordKey: String, kind: io.github.lenticualrstone.civilight.domain.recorder.put.PutKind) = openSheet(PutSheet(recordKey, kind.name))

    fun addLink(recordKey: String) = openSheet(RecordLinkSheet(recordKey))

    fun unhide(recordKey: String) {
        deps.appScope.launch { c.unhide(recordKey) }
    }

    /** `말로 기록` / `글로 쓰기`: the capture with the inbox as entry (its project filter pre-fills the link, LRC-19). */
    fun capture(view: InboxView, typed: Boolean) {
        val base = EntryRoutes.routeOf(view.captureEntry, startTyped = typed)
        deps.navigator.push(base.copy(surface = "inbox"))
    }

    /** `지문으로 보기` (LRC-41): the system prompt `숨긴 기록 보기`; cancel or failure changes nothing and shows nothing (LIB-37). */
    fun openHidden() {
        deps.appScope.launch {
            if (deps.biometric.authenticate(AuthRequest(RecorderCopy.BIOMETRIC_TITLE)) == AuthResult.SUCCESS) deps.unlock.unlockRecords()
        }
    }
}

/** `EmptyLr` (LRC-30): `전체 0`, the outlined block `기록 없음` with `말로 기록` · `글로`. */
@Composable
private fun EmptyInbox(view: InboxView, actions: InboxActions) {
    val empty = view.screen.empty ?: return
    Column {
        FilterChip(empty.chip, selected = true, onClick = {})
        EmptyBlock(
            title = empty.title,
            subtitle = empty.sub,
            primaryLabel = empty.buttons[0],
            onPrimary = { actions.capture(view, typed = false) },
            secondaryLabel = empty.buttons[1],
            onSecondary = { actions.capture(view, typed = true) },
        )
    }
}

/** The open hidden group (LRC-41, `LrUnlocked`): `sel` box, header `숨긴 기록 n · 열림` · `앱을 떠나면 잠김`, the hidden rows with `숨김 풀기`. */
@Composable
private fun OpenGroupBox(group: OpenGroup, rows: List<RowView>, actions: InboxActions, textModifier: Modifier) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .padding(top = 14.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.sel)
            .insetOutline(colors.line, 1.dp, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CivilightIcon(IconId.Unlock, contentDescription = null, size = 16.dp, tint = colors.acc)
            CivilightText(group.header, CivilightTheme.type.label, Modifier.weight(1f), color = colors.ink, maxLines = 1)
            CivilightText(group.relockNote, CivilightTheme.type.small, color = colors.ink3, maxLines = 1)
        }
        for (row in rows) Box { InboxRowItem(row, actions, textModifier = textModifier) }
    }
}
