package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.feature.script.ui.scriptPickerCreator
import io.github.lenticualrstone.civilight.feature.script.ui.LINK_CREATE_KINDS
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.GlassScaffold
import io.github.lenticualrstone.civilight.designsystem.component.InputContext
import io.github.lenticualrstone.civilight.designsystem.component.ScriptInputActions
import io.github.lenticualrstone.civilight.designsystem.component.ScriptInputBar
import io.github.lenticualrstone.civilight.designsystem.component.ScriptInputEvent
import io.github.lenticualrstone.civilight.designsystem.component.ScriptInputReadOnlyRow
import io.github.lenticualrstone.civilight.designsystem.component.SwipeSide
import io.github.lenticualrstone.civilight.feature.script.ui.BlockMenu
import io.github.lenticualrstone.civilight.feature.script.ui.BlockMenuEntry
import io.github.lenticualrstone.civilight.feature.script.ui.BlockMenuItem
import io.github.lenticualrstone.civilight.feature.script.ui.BlockMenuOpen
import io.github.lenticualrstone.civilight.feature.script.ui.BlockMenuOverlay
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.component.ToastHost
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler
import io.github.lenticualrstone.civilight.designsystem.theme.Motion
import io.github.lenticualrstone.civilight.designsystem.theme.rememberLastNonNull
import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.domain.script.MergeCheck
import io.github.lenticualrstone.civilight.domain.script.ReadAloud
import io.github.lenticualrstone.civilight.domain.script.ScriptCommands
import io.github.lenticualrstone.civilight.feature.script.assets.AssetForm
import io.github.lenticualrstone.civilight.feature.script.assets.AssetFormSheet
import io.github.lenticualrstone.civilight.feature.script.assets.AssetFormSheetKey
import io.github.lenticualrstone.civilight.feature.script.assets.AssetsCopy
import io.github.lenticualrstone.civilight.feature.script.change.ChangeActions
import io.github.lenticualrstone.civilight.feature.script.change.ChangePickSheet
import io.github.lenticualrstone.civilight.feature.script.change.ChangePickSheetKey
import io.github.lenticualrstone.civilight.feature.script.change.ChangeSheet
import io.github.lenticualrstone.civilight.feature.script.change.ChangeSheetKey
import io.github.lenticualrstone.civilight.feature.script.fold.FoldRules
import io.github.lenticualrstone.civilight.feature.script.fold.PanelHandover
import io.github.lenticualrstone.civilight.feature.script.fold.PanelScrollMemory
import io.github.lenticualrstone.civilight.feature.script.input.ChipSpeakerSheetKey
import io.github.lenticualrstone.civilight.feature.script.input.DraftEffect
import io.github.lenticualrstone.civilight.feature.script.input.DraftLinkSheetKey
import io.github.lenticualrstone.civilight.feature.script.input.DraftMark
import io.github.lenticualrstone.civilight.feature.script.input.DraftTransformation
import io.github.lenticualrstone.civilight.feature.script.input.Drafts
import io.github.lenticualrstone.civilight.feature.script.input.FindActions
import io.github.lenticualrstone.civilight.feature.script.input.FindSheet
import io.github.lenticualrstone.civilight.feature.script.input.FindSheetKey
import io.github.lenticualrstone.civilight.feature.script.input.KindMenu
import io.github.lenticualrstone.civilight.feature.script.input.KindMenuSheet
import io.github.lenticualrstone.civilight.feature.script.input.KindMenuSheetKey
import io.github.lenticualrstone.civilight.feature.script.input.MarkPickSheet
import io.github.lenticualrstone.civilight.feature.script.input.MarkPickSheetKey
import io.github.lenticualrstone.civilight.feature.script.input.PlantSheet
import io.github.lenticualrstone.civilight.feature.script.input.PlantSheetKey
import io.github.lenticualrstone.civilight.feature.script.input.DraftCueChips
import io.github.lenticualrstone.civilight.feature.script.input.DraftCueSheet
import io.github.lenticualrstone.civilight.feature.script.input.DraftCueSheetKey
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import io.github.lenticualrstone.civilight.feature.script.input.ReadAloudPlayer
import io.github.lenticualrstone.civilight.feature.script.input.SceneInView
import io.github.lenticualrstone.civilight.feature.script.input.BottomAnchor
import io.github.lenticualrstone.civilight.feature.script.input.WriteCopy
import io.github.lenticualrstone.civilight.feature.script.layers.LayersModel
import io.github.lenticualrstone.civilight.feature.script.layers.LayersSheet
import io.github.lenticualrstone.civilight.feature.script.layers.LayersSheetKey
import io.github.lenticualrstone.civilight.feature.script.layout.ScriptLayout
import io.github.lenticualrstone.civilight.feature.script.layout.panelToggleEnter
import io.github.lenticualrstone.civilight.feature.script.layout.panelToggleExit
import io.github.lenticualrstone.civilight.ui.window.PanelRail
import io.github.lenticualrstone.civilight.ui.window.PanelSeamButton
import io.github.lenticualrstone.civilight.ui.window.PanelSide
import io.github.lenticualrstone.civilight.ui.window.rememberPanelCollapseState
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import io.github.lenticualrstone.civilight.feature.script.layout.TabletopLayout
import io.github.lenticualrstone.civilight.feature.script.layout.foldInFrameDp
import io.github.lenticualrstone.civilight.feature.script.layout.rememberTabletopFoldPx
import io.github.lenticualrstone.civilight.feature.script.links.BlockTap
import io.github.lenticualrstone.civilight.feature.script.links.LinkCardActions
import io.github.lenticualrstone.civilight.feature.script.links.LinkCardSheet
import io.github.lenticualrstone.civilight.feature.script.links.LinkCardSheetKey
import io.github.lenticualrstone.civilight.feature.script.motion.SwipeButtonSpec
import io.github.lenticualrstone.civilight.feature.script.motion.barSwap
import io.github.lenticualrstone.civilight.feature.script.panel.PanelActions
import io.github.lenticualrstone.civilight.feature.script.panel.PanelContent
import io.github.lenticualrstone.civilight.feature.script.panel.PanelSheet
import io.github.lenticualrstone.civilight.feature.script.panel.PanelSheetKey
import io.github.lenticualrstone.civilight.feature.script.panel.PanelTab
import io.github.lenticualrstone.civilight.feature.script.panel.PanelTabBody
import io.github.lenticualrstone.civilight.feature.script.panel.ScriptSidePanel
import io.github.lenticualrstone.civilight.feature.script.panel.TabletopPanel
import io.github.lenticualrstone.civilight.feature.script.records.Relink
import io.github.lenticualrstone.civilight.feature.script.records.RelinkSheet
import io.github.lenticualrstone.civilight.feature.script.records.RelinkSheetKey
import io.github.lenticualrstone.civilight.feature.script.readonly.ReadOnlyFrame
import io.github.lenticualrstone.civilight.feature.script.select.MoveSheet
import io.github.lenticualrstone.civilight.feature.script.select.MoveSheetKey
import io.github.lenticualrstone.civilight.feature.script.select.SelectBottomBar
import io.github.lenticualrstone.civilight.feature.script.select.SelectList
import io.github.lenticualrstone.civilight.feature.script.select.SelectRowModel
import io.github.lenticualrstone.civilight.feature.script.select.SelectRows
import io.github.lenticualrstone.civilight.feature.script.select.SelectRules
import io.github.lenticualrstone.civilight.feature.script.select.SelectTopBar
import io.github.lenticualrstone.civilight.feature.script.state.BlockRow
import io.github.lenticualrstone.civilight.feature.script.state.CommandAction
import io.github.lenticualrstone.civilight.feature.script.state.CommandRouting
import io.github.lenticualrstone.civilight.feature.script.state.HeadRow
import io.github.lenticualrstone.civilight.feature.script.state.HeaderModel
import io.github.lenticualrstone.civilight.feature.script.state.HiddenSceneRow
import io.github.lenticualrstone.civilight.feature.script.state.ScriptOps
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRow
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRowKeys
import io.github.lenticualrstone.civilight.feature.script.ui.BlockLook
import io.github.lenticualrstone.civilight.feature.script.ui.KindSheet
import io.github.lenticualrstone.civilight.feature.script.ui.KindSheetKey
import io.github.lenticualrstone.civilight.feature.script.ui.LinkSheet
import io.github.lenticualrstone.civilight.feature.script.ui.LinkSheetKey
import io.github.lenticualrstone.civilight.feature.script.ui.Manuscript
import io.github.lenticualrstone.civilight.feature.script.ui.ManuscriptActions
import io.github.lenticualrstone.civilight.feature.script.ui.PositionSheet
import io.github.lenticualrstone.civilight.feature.script.ui.PositionSheetKey
import io.github.lenticualrstone.civilight.feature.script.ui.ScriptTopBar
import io.github.lenticualrstone.civilight.feature.script.ui.SpeakerSheet
import io.github.lenticualrstone.civilight.feature.script.ui.SpeakerSheetKey
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.PaneSplit
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.game.GameTools
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.ScriptPreview
import io.github.lenticualrstone.civilight.navigation.CheckCenter
import io.github.lenticualrstone.civilight.navigation.NewUnitSheet
import io.github.lenticualrstone.civilight.navigation.ProjectSearch
import io.github.lenticualrstone.civilight.navigation.RecorderRecordDetail
import io.github.lenticualrstone.civilight.navigation.RenumberSheet
import io.github.lenticualrstone.civilight.navigation.SceneWhereSheet
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.ScriptAssets
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.navigation.UnitMenuSheet
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.picker.CharacterPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.LinkPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.scaffold.LocalToasts
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHost
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHostLayout
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.tour.TourHold
import io.github.lenticualrstone.civilight.ui.tour.TourStepAbsent
import io.github.lenticualrstone.civilight.ui.tour.tourTarget
import io.github.lenticualrstone.civilight.ui.window.FoldPosture
import io.github.lenticualrstone.civilight.ui.window.PaneSplitHandle
import io.github.lenticualrstone.civilight.ui.window.rememberPaneSplitState
import kotlinx.coroutines.launch

/**
 * The script screen (0.2.0: SCR-01, SCR-14, SCR-70 … SCR-74): one episode's manuscript under the glass header
 * (`이 씬 · 1-7` · `찾기` · `표시`), always in the reading state; text is written only in the input bar at the
 * bottom (or, in a read-only project, the `예시 프로젝트` row). A block tap selects; swipes reveal buttons.
 * Built on `GlassScaffold`; the toast host is drawn here the same way `CivilightScreen` does.
 */
@Composable
fun ScriptScreen(route: Script, deps: FeatureDeps) {
    val vm = entryViewModel { ScriptViewModel(route, deps) }
    val content by vm.content.collectAsState()
    val sheets = rememberSheetHost()
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.flushNow() }
    // SCR-70: the draft survives process death through the saved state (the process memory covers the rest).
    var savedDraft by rememberSaveable { mutableStateOf<String?>(null) }
    val draft by vm.draft.collectAsState()
    // Read once, before the effect below overwrites it with the live draft.
    val restoredDraft = remember { savedDraft }
    LaunchedEffect(Unit) { vm.restoreSavedDraft(restoredDraft) }
    // The project's draft is shared by its script screens: the one shown (or resumed) checks it against its episode.
    LaunchedEffect(vm) { vm.claimDraft() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.claimDraft() }
    // Hidden text never goes into the saved state (Drafts.forStorage); before the document loads the last one stays.
    LaunchedEffect(draft, content?.doc) { vm.savableDraft(draft)?.let { savedDraft = it } }
    val c = content
    val panelScroll = remember(c != null) { PanelScrollMemory(c?.doc?.app?.view?.script?.panel?.scroll.orEmpty()) }
    val gate = rememberHiddenGate(route.projectKey)
    var readAloudAt by rememberSaveable { mutableStateOf<Int?>(null) }
    val panelActions = { open: (SheetKey) -> Unit, leave: () -> Unit -> scriptPanelActions(route, deps, vm, gate.open, open, leave) }
    val startReadAloud: () -> Unit = {
        val doc = vm.content.value?.doc
        val unit = vm.content.value?.unitKey
        if (doc != null && unit != null) {
            val steps = ReadAloud.steps(doc, unit, GameTools.on(doc), gate.open)
            val from = vm.editor.value.selected ?: vm.sceneInViewKey?.let { s -> doc.manuscript.liveBlocksOf(s).firstOrNull()?.key }
            readAloudAt = ReadAloud.indexAt(steps, doc, from).takeIf { it >= 0 }
        }
    }
    SheetHostLayout(sheets, localSheets = { key -> scriptSheet(key, route, deps, vm, c, gate.open, gate.unlock, panelActions, panelScroll, startReadAloud) }) {
        if (c == null) {
            TourHold()
            Box(Modifier.fillMaxSize().background(CivilightTheme.colors.bg))
        } else {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                ScriptFrame(route, deps, vm, c, sheets, maxWidth.value, maxHeight.value, panelScroll, startReadAloud)
            }
            // SCR-28: 낭독 over the whole screen; `닫기` and back return with the step's block selected.
            val at = readAloudAt
            if (at != null) {
                val steps = remember(c.doc, c.unitKey, gate.open) { ReadAloud.steps(c.doc, c.unitKey, GameTools.on(c.doc), gate.open) }
                fun close(block: Key?) {
                    readAloudAt = null
                    block?.let { vm.selectBlock(it); vm.aboveInput.tryEmit(ScriptRowKeys.block(it)) }
                }
                LocalBackHandler.current(true) { close(steps.getOrNull(at)?.blockKey) }
                ReadAloudPlayer(c.doc, steps, at.coerceIn(0, (steps.size - 1).coerceAtLeast(0)), onIndex = { readAloudAt = it }, onClose = ::close)
            }
        }
    }
}

/** Content of the script's own sheets (shell.md §5: feature-local keys). */
private fun scriptSheet(
    key: SheetKey,
    route: Script,
    deps: FeatureDeps,
    vm: ScriptViewModel,
    c: ScriptContent?,
    unlocked: Boolean,
    unlock: () -> Unit,
    panelActions: (open: (SheetKey) -> Unit, leave: () -> Unit) -> PanelActions,
    panelScroll: PanelScrollMemory,
    startReadAloud: () -> Unit,
): (@Composable SheetScope.() -> Unit)? {
    val doc = c?.doc ?: return null
    return when (key) {
        is LayersSheetKey -> {
            {
                val view = doc.app.view.script
                val readAloud = remember(doc, c.unitKey, unlocked) { LayersModel.readAloudEnabled(doc, c.unitKey, unlocked) }
                LayersSheet(view.layers, view.preview, onLayer = vm::setLayer, onPreview = { p ->
                    if (p == ScriptPreview.READ_ALOUD) {
                        close()
                        startReadAloud()
                    } else vm.setPreview(p)
                }, readAloudEnabled = readAloud, onSelectBlocks = if (c.readOnly) null else ({
                    // As the `속성` entry (ScriptPanelWiring): the current rows, not a stale capture.
                    vm.content.value?.rows?.let(vm::enterSelect)
                }))
            }
        }
        is PanelSheetKey -> {
            {
                val tab by vm.panelTab.collectAsState()
                val panel by vm.panel.collectAsState()
                val actions = panelActions(::open, ::close)
                PanelSheet(tab, vm::setPanelTab, scrollOf = panelScroll::state) { shown ->
                    panel?.takeIf { it.tab == shown }?.let { PanelBody(it, wide = false, actions = actions, c = c, vm = vm) }
                }
            }
        }
        is KindMenuSheetKey -> {
            {
                val draft by vm.draft.collectAsState()
                val block = key.blockKey?.let(doc.index::block)
                val editBlock = (draft.mark as? DraftMark.Edit)?.blockKey?.let(doc.index::block)
                val current = block?.kind?.known ?: draft.kind
                KindMenuSheet(KindMenu.rows(current, block, editBlock.takeIf { block == null }, GameTools.on(doc))) { kind ->
                    if (key.blockKey != null) vm.convertKind(key.blockKey, kind) else vm.pickChipKind(kind)
                }
            }
        }
        is ChipSpeakerSheetKey -> {
            {
                val draft by vm.draft.collectAsState()
                val gate = rememberHiddenGate(route.projectKey)
                CharacterPickerSheet(
                    doc = doc,
                    gate = gate,
                    onPick = { picked ->
                        close()
                        vm.pickChipSpeaker(picked)
                    },
                    onClose = ::close,
                    sceneCast = ScriptOps.sceneCast(doc, key.sceneKey),
                    recent = doc.app.view.script.recentSpeakers,
                    currentKey = draft.speakerKey,
                    creator = scriptPickerCreator(CreateKind.CHARACTER),
                )
            }
        }
        is DraftLinkSheetKey -> {
            {
                LinkPickerSheet(
                    doc = doc,
                    gate = rememberHiddenGate(key.projectKey),
                    title = ScriptCopy.LINK_PICKER_TITLE,
                    onPick = { kind, target, text ->
                        close()
                        vm.insertDraftLink(kind, target, text)
                    },
                    onClose = ::close,
                    creator = scriptPickerCreator(*LINK_CREATE_KINDS),
                )
            }
        }
        is FindSheetKey -> {
            {
                val game = GameTools.on(doc)
                val commands = ScriptCommands.commands(doc, c.unitKey).filterNot { it.startsWith("/") }
                    .filterNot { !game && it == AssetsCopy.TITLE }
                    .filterNot { c.readOnly && it == io.github.lenticualrstone.civilight.feature.script.select.MoveCopy.TITLE } +
                    // SCR-28: `낭독` is offered only when the episode has a step (as the `표시` chip).
                    listOfNotNull(WriteCopy.READ_ALOUD_CMD.takeIf { LayersModel.readAloudEnabled(doc, c.unitKey, unlocked) }, WriteCopy.VERSIONS_CMD)
                FindSheet(
                    key.replace, key.query, doc, c.unitKey, unlocked, c.readOnly, commands,
                    FindActions(
                        jumpTo = { block ->
                            vm.selectBlock(block)
                            vm.aboveInput.tryEmit(ScriptRowKeys.block(block))
                        },
                        command = { text -> vm.commandRequests.tryEmit(text) },
                        replace = vm::replaceHits,
                    ),
                )
            }
        }
        is DraftCueSheetKey -> {
            {
                DraftCueSheet(key, doc, onPick = vm::addDraftCue, onNewAsset = { kind ->
                    AssetForm.kindOf(kind)?.let { open(AssetFormSheetKey(route.projectKey, kind = it)) }
                })
            }
        }
        is PlantSheetKey -> {
            { PlantSheet(key, doc) { name, payoff -> vm.plantMark(key.blockKey, name, payoff) } }
        }
        is MarkPickSheetKey -> {
            {
                MarkPickSheet(
                    key, doc, unlocked,
                    onMention = { f -> vm.mentionMark(f, key.blockKey) },
                    onPayoff = { f -> vm.payoffMark(f, key.blockKey) },
                )
            }
        }
        is SpeakerSheetKey -> {
            {
                SpeakerSheet(
                    key = key,
                    doc = doc,
                    sceneCast = ScriptOps.sceneCast(doc, key.sceneKey),
                    onPick = { picked -> vm.pickSpeaker(key.target, picked) },
                    onRestoreDeleted = if (vm.readOnly) null else vm::restoreSpeaker,
                )
            }
        }
        is LinkSheetKey -> {
            { LinkSheet(key, doc) { kind, target, text -> vm.insertLink(key.blockKey, key.rawOffset, kind, target, text, key.optionKey) } }
        }
        is KindSheetKey -> {
            {
                val block = doc.index.block(key.blockKey)
                KindSheet(
                    subline = block?.let { Labels.number(it.number) }.orEmpty(),
                    choices = ScriptOps.kindChoices(doc, key.blockKey),
                    readOnly = vm.readOnly,
                    onPick = { kind -> vm.convertKind(key.blockKey, kind) },
                )
            }
        }
        is PositionSheetKey -> {
            { PositionSheet(key) { scene -> vm.setLensToScene(scene) } }
        }
        is ChangeSheetKey -> {
            {
                val draft by vm.changeDraft.collectAsState()
                LaunchedEffect(key) { vm.changeDraftOf(key.blockKey) }
                ChangeSheet(
                    key, doc, draft?.takeIf { it.blockKey == key.blockKey },
                    ChangeActions(edit = vm::editChange, record = vm::recordChange, addKind = vm::addRelationKind),
                )
            }
        }
        is ChangePickSheetKey -> {
            {
                val scene = doc.index.block(key.blockKey)?.sceneKey
                ChangePickSheet(key, doc, ScriptOps.sceneCast(doc, scene), vm::editChange)
            }
        }
        is MoveSheetKey -> {
            {
                val open = rememberHiddenGate(key.projectKey).open
                MoveSheet(key, doc, open) { position -> vm.moveBlocks(key.blockKeys, position) }
            }
        }
        is LinkCardSheetKey -> {
            {
                val gate = rememberHiddenGate(key.projectKey)
                LinkCardSheet(
                    key, doc, gate, vm.readOnly,
                    LinkCardActions(
                        relink = { kind, target -> vm.relinkLink(key.blockKey, key.linkKey, kind, target) },
                        unlink = { vm.unlinkLink(key.blockKey, key.linkKey) },
                        restore = { kind, target -> vm.restoreLinkTarget(kind, target, authenticated = gate.open) },
                        keep = { target -> vm.keepLink(target, key.blockKey) },
                    ),
                )
            }
        }
        is AssetFormSheetKey -> {
            { AssetFormSheet(key) }
        }
        is RelinkSheetKey -> {
            {
                val open = rememberHiddenGate(key.projectKey).open
                RelinkSheet(Relink.of(doc, key.recordKey, open), vm.readOnly) { block -> vm.relinkRecord(key.recordKey, block) }
            }
        }
        is io.github.lenticualrstone.civilight.feature.script.synergy.MentionPreviewSheetKey -> {
            {
                io.github.lenticualrstone.civilight.feature.script.synergy.MentionPreviewSheetContent(
                    doc = doc,
                    hiddenUnlocked = rememberHiddenGate(key.projectKey).open,
                    targetKindName = key.targetKind,
                    targetKey = key.targetKey,
                    onOpenDetail = {
                        when (io.github.lenticualrstone.civilight.model.text.LinkTargetKind.entries.firstOrNull { it.name == key.targetKind }) {
                            io.github.lenticualrstone.civilight.model.text.LinkTargetKind.CHARACTER ->
                                deps.navigator.push(io.github.lenticualrstone.civilight.navigation.CharacterProfile(route.projectKey, key.targetKey))
                            io.github.lenticualrstone.civilight.model.text.LinkTargetKind.TERM ->
                                deps.navigator.push(io.github.lenticualrstone.civilight.navigation.TermCard(route.projectKey, key.targetKey))
                            io.github.lenticualrstone.civilight.model.text.LinkTargetKind.PLACE ->
                                deps.navigator.push(io.github.lenticualrstone.civilight.navigation.PlaceCard(route.projectKey, key.targetKey))
                            io.github.lenticualrstone.civilight.model.text.LinkTargetKind.FACTION ->
                                deps.navigator.push(io.github.lenticualrstone.civilight.navigation.FactionCard(route.projectKey, key.targetKey))
                            io.github.lenticualrstone.civilight.model.text.LinkTargetKind.EVENT ->
                                deps.navigator.push(io.github.lenticualrstone.civilight.navigation.EventDetail(route.projectKey, key.targetKey))
                            else -> Unit
                        }
                    },
                    onOpenRefPanel = {
                        vm.setPanelTab(PanelTab.REF)
                    },
                    onClose = { close() },
                )
            }
        }
        is io.github.lenticualrstone.civilight.feature.script.synergy.ScriptOutlineSheetKey -> {
            {
                val open = rememberHiddenGate(key.projectKey).open
                io.github.lenticualrstone.civilight.feature.script.synergy.ScriptOutlineSheetContent(
                    doc = doc,
                    unitKey = key.unitKey,
                    hiddenUnlocked = open,
                    lensScene = vm.sceneInViewKey,
                    // Rules-time 4: the position sheet's `렌즈를 이 씬으로 맞추기`, reached from here.
                    onSetLens = { scene, _ ->
                        open(PositionSheetKey(route.projectKey, scene, Labels.sceneLocation(doc, scene)))
                    },
                    // Scroll within this screen (as `/씬 3`), no new Script route; blank scenes land on their head.
                    // The scene's first drawn block sits above the input bar (its head shows above it); blank → the head.
                    onJumpScene = { row ->
                        vm.aboveInput.tryEmit(c.rows.firstOrNull { it is BlockRow && it.sceneKey == row.sceneKey }?.key ?: row.jumpRowKey)
                    },
                    // The scene head's `⋯` menu (STO-07 · SCR-47): `지우기` and the rest.
                    onSceneMenu = { scene -> open(UnitMenuSheet(route.projectKey, scene)) },
                    onClose = { close() },
                )
            }
        }
        is io.github.lenticualrstone.civilight.feature.script.synergy.SpeakerFocusSheetKey -> {
            {
                io.github.lenticualrstone.civilight.feature.script.synergy.SpeakerFocusSheetContent(
                    doc = doc,
                    hiddenUnlocked = rememberHiddenGate(key.projectKey).open,
                    unitKey = key.unitKey,
                    focusedSpeakerKey = null,
                    onSelectSpeaker = { speakerKey ->
                        if (speakerKey != null) vm.pickChipSpeaker(speakerKey)
                    },
                    onClose = { close() },
                )
            }
        }
        else -> null
    }
}

/**
 * A panel tab's body; the `참고` tab starts with the selected block's term candidates while the project
 * setting `용어 후보` is on (SCR-33, S-5): `용어 후보 · 등불 · 잇기 · 아님`.
 */
@Composable
private fun PanelBody(content: PanelContent, wide: Boolean, actions: PanelActions, c: ScriptContent, vm: ScriptViewModel) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (content.tab == PanelTab.REF && c.doc.project.termCandidates) {
            val row = content.selected?.let { key -> c.rows.firstOrNull { it.key == ScriptRowKeys.block(key) } as? BlockRow }
            val raw = row?.runs?.joinToString("") { it.shownText }.orEmpty()
            row?.termCandidates?.forEach { span ->
                val word = raw.substring(span.start.coerceIn(0, raw.length), span.end.coerceIn(0, raw.length))
                Row(Modifier.fillMaxWidth().background(colors.tint).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CivilightText("${io.github.lenticualrstone.civilight.feature.script.terms.TermCopy.LAYER} · ", type.small, color = colors.ink3, maxLines = 1)
                    CivilightText(word, type.itemTitle, Modifier.weight(1f), color = colors.ink, maxLines = 1)
                    if (!c.readOnly) {
                        TextAction(io.github.lenticualrstone.civilight.feature.script.links.LinkCopy.LINK, { vm.linkCandidate(span.candidate) })
                        TextAction(io.github.lenticualrstone.civilight.feature.script.links.LinkCopy.REJECT, { vm.rejectCandidate(span.candidate) })
                    }
                }
            }
        }
        PanelTabBody(content, wide = wide, actions = actions)
    }
}

@Composable
private fun ScriptFrame(
    route: Script,
    deps: FeatureDeps,
    vm: ScriptViewModel,
    c: ScriptContent,
    sheets: SheetHost,
    widthDp: Float,
    heightDp: Float,
    panelScroll: PanelScrollMemory,
    startReadAloud: () -> Unit,
) {
    val doc = c.doc
    val editor by vm.editor.collectAsState()
    val draft by vm.draft.collectAsState()
    val fresh by vm.fresh.collectAsState()
    ScriptTourSteps.absent(c.readOnly).forEach { step -> key(step) { TourStepAbsent(step) } }
    // r3 (decision 0.2.3 · r3): the writer closes and reopens the panel with the shared seam button and rail; the
    // choice is stored per screen (`AppSettings.panelClosed`, module `scriptPanel`) by the shell's collapse state.
    val panelState = rememberPanelCollapseState(PaneSplit.Modules.SCRIPT_PANEL)
    val panelOpen = !panelState.closed
    fun setPanelByWriter(open: Boolean) = panelState.setClosed(!open)
    val panelTab by vm.panelTab.collectAsState()
    val panel by vm.panel.collectAsState()
    val select by vm.select.collectAsState()
    val selecting = select.active
    val selectListState = rememberLazyListState()
    var frameTopPx by remember { mutableFloatStateOf(0f) }
    val foldPx = rememberTabletopFoldPx()
    val density = LocalDensity.current
    val tabletop = foldPx?.let { TabletopLayout.of(FoldPosture.TABLETOP, heightDp, foldInFrameDp(it, frameTopPx, density.density)) }
    val split = tabletop?.takeIf { !selecting }
    val upperHeight by animateFloatAsState(
        split?.upper ?: heightDp,
        CivilightTheme.motion.move(if (split != null) CivilightMotion.foldOpen else CivilightMotion.foldClose),
        label = "tabletopUpper",
    )
    val upperSettled = split == null && kotlin.math.abs(upperHeight - heightDp) < 0.5f
    val motion = CivilightTheme.motion
    // Select mode and tabletop put the panel out of the way for a while: no panel and no rail (the writer's choice stays).
    val panelHidden = selecting || tabletop != null
    val baseLayout = ScriptLayout.of(widthDp, panelOpen, height = heightDp, hidden = panelHidden)
    val paneSplit = rememberPaneSplitState(PaneSplit.Modules.SCRIPT_PANEL, widthDp, ScriptLayout.of(widthDp, panelOpen = true, height = heightDp).areaWidth)
    val targetLayout = if (baseLayout.panel && paneSplit.resizable) ScriptLayout.of(widthDp, true, area = paneSplit.leftDp, height = heightDp) else baseLayout
    var panelArrived by remember { mutableStateOf(targetLayout.panel) }
    LaunchedEffect(targetLayout.panel, motion) {
        if (!targetLayout.panel) {
            panelArrived = false
        } else if (!panelArrived) {
            kotlinx.coroutines.delay(motion.delayMs(CivilightMotion.foldOpen.durationMs))
            panelArrived = true
        }
    }
    // The last layout without the panel (beside the rail, or full width) and the width it was for.
    var noPanel by remember { mutableStateOf<Pair<Float, ScriptLayout>?>(null) }
    SideEffect { if (!targetLayout.panel) noPanel = widthDp to targetLayout }
    val beforePanel = ScriptLayout.beforePanel(noPanel?.second, noPanel?.first, widthDp, heightDp)
    // Until the panel has arrived the manuscript keeps the width it had (SCR-66, one re-wrap).
    val layout = if (targetLayout.panel && !panelArrived && !motion.reduced) targetLayout.keepingAreaOf(beforePanel) else targetLayout
    val preview = doc.app.view.script.preview
    val previewOnly = !LayersModel.isEditable(preview)
    // SCR-30: one row open at a time, on one side; scrolling closes it.
    var revealedRow by remember { mutableStateOf<String?>(null) }
    var revealedSide by remember { mutableStateOf(SwipeSide.CLOSED) }
    // (0.2.4) The block whose long-press menu is open (one at a time).
    var blockMenu by remember { mutableStateOf<BlockMenuOpen?>(null) }
    val wide = layout.wide
    val canUndo by vm.canUndo.collectAsState()
    val entered by vm.entered.collectAsState()
    val target by vm.target.collectAsState()
    val toasts = LocalToasts.current
    val toast by toasts.current.collectAsState()
    val gate = rememberHiddenGate(route.projectKey)
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val scriptSettings by deps.settings.collectAsState()
    val rows = c.rows
    val game = GameTools.on(doc)
    // (0.5.0) Typewriter scroll: keep the selected/edited block anchored near 38% from the top of the viewport.
    LaunchedEffect(editor.selected, scriptSettings.typewriterScroll) {
        if (!scriptSettings.typewriterScroll) return@LaunchedEffect
        val key = editor.selected ?: return@LaunchedEffect
        val idx = rows.indexOfFirst { it is BlockRow && it.blockKey == key }
        if (idx >= 0) {
            val vh = listState.layoutInfo.viewportSize.height
            val targetOffset = if (vh > 0) -(vh * 0.36f).toInt() else 0
            listState.animateScrollToItem(idx, targetOffset)
        }
    }
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            revealedRow = null
            revealedSide = SwipeSide.CLOSED
        }
    }

    // --- The scene in view (SCR-71): the item directly above the input bar ---
    var bottomPadPx by remember { mutableIntStateOf(0) }
    val sceneInView by remember(rows) {
        derivedStateOf {
            val info = listState.layoutInfo
            val items = info.visibleItemsInfo.mapNotNull { item ->
                when (val row = rows.getOrNull(item.index)) {
                    is HeadRow -> SceneInView.Item(row.sceneKey, SceneInView.Kind.HEAD, item.offset.toFloat(), (item.offset + item.size).toFloat())
                    is HiddenSceneRow -> SceneInView.Item(row.sceneKey, SceneInView.Kind.HIDDEN_ROW, item.offset.toFloat(), (item.offset + item.size).toFloat())
                    is BlockRow -> SceneInView.Item(row.sceneKey, SceneInView.Kind.BLOCK, item.offset.toFloat(), (item.offset + item.size).toFloat())
                    else -> null
                }
            }
            val inputTop = (info.viewportEndOffset - bottomPadPx).toFloat()
            SceneInView.target(items, inputTop) ?: rows.firstOrNull { it !is HiddenSceneRow }?.sceneKey
        }
    }
    // SCR-71 bottom anchoring: the manuscript moves with the bar's top edge (snap, no animation).
    LaunchedEffect(listState) {
        var last: Int? = null
        snapshotFlow { listState.layoutInfo.viewportEndOffset - bottomPadPx }
            .collect { top ->
                if (top <= 0) return@collect
                val d = BottomAnchor.delta(last, top, listState.isScrollInProgress)
                last = top
                if (d != 0f) listState.scrollBy(d)
            }
    }
    val sceneKey = sceneInView?.takeIf { doc.manuscript.isLiveUnit(it) }
    vm.sceneInViewKey = sceneKey
    val sceneNumber = sceneKey?.let { Labels.sceneOrdinal(doc, it) }
    var flash by remember { mutableIntStateOf(0) }
    var lastScene by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(sceneNumber) {
        if (lastScene != null && sceneNumber != null && lastScene != sceneNumber) flash++
        lastScene = sceneNumber
    }
    LaunchedEffect(vm) { vm.placeFlashes.collect { flash++ } }
    val selectRows = if (selecting) SelectRows.of(doc, rows, select, doc.app.view.script.layers, gate.open) else emptyList()
    val selectScene by remember(rows, selecting) { derivedStateOf { if (selecting) sceneUnderSelectBar(selectListState, selectRows) else null } }
    val header = HeaderModel.of(doc, c.unitKey, c.time, editor.copy(selected = null, virtualScene = null), if (selecting) selectScene else sceneKey, false, true)
    val headerScene = header.sceneKey
    val coverPanelOpen = sheets.top is PanelSheetKey
    LaunchedEffect(layout.panel, coverPanelOpen, headerScene, split != null) {
        vm.setPanelRequest(headerScene, layout.panel || coverPanelOpen || split != null)
    }

    // --- Open at the target once (SCR-01 entry: the block directly above the input bar) ---
    var openedAtTarget by rememberSaveable { mutableStateOf(false) }
    var pendingAbove by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(target, rows.isNotEmpty()) {
        val t = target ?: return@LaunchedEffect
        if (openedAtTarget || rows.isEmpty()) return@LaunchedEffect
        val key = t.blockKey?.let(ScriptRowKeys::block)
            ?: t.sceneKey?.let { s -> doc.manuscript.liveBlocksOf(s).lastOrNull()?.key?.let(ScriptRowKeys::block) ?: ScriptRowKeys.head(s) }
        if (key != null) pendingAbove = key
        openedAtTarget = true
    }
    var pendingScroll by remember { mutableStateOf<String?>(null) }
    val restoredEntry = remember { openedAtTarget }
    LaunchedEffect(rows.isNotEmpty()) {
        if (rows.isEmpty() || vm.foldRestoreChecked) return@LaunchedEffect
        vm.foldRestoreChecked = true
        val point = doc.app.view.script.restore
        if (restoredEntry && point != null && FoldRules.shouldRestore(point, c.unitKey)) {
            vm.restoreTo(point)?.let { pendingScroll = it }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        val top = listState.layoutInfo.visibleItemsInfo.firstOrNull { ScriptRowKeys.blockKeyOf(it.key.toString()) != null }?.key?.toString()
        vm.saveFoldState(c.unitKey, top, panelScroll.offsets())
    }
    PanelHandover(
        sideLayout = tabletop != null || ScriptLayout.of(widthDp, panelOpen = true, height = heightDp).panel,
        sheetOpen = sheets.top is PanelSheetKey,
        panelOpen = panelOpen,
        editing = false,
        selecting = selecting,
    ) { handover ->
        if (handover.closeSheet && sheets.top is PanelSheetKey) sheets.close()
        // Unfold with the cover panel sheet open: the sheet becomes the side panel. Otherwise the stored choice stands.
        if (handover.panelOpen && panelState.closed) panelState.setClosed(false)
        if (handover.openSheet) sheets.open(PanelSheetKey(route.projectKey))
    }
    LaunchedEffect(vm) { vm.scrollTo.collect { key -> pendingScroll = key } }
    LaunchedEffect(vm) { vm.scrollAboveInput.collect { key -> pendingAbove = key } }
    LaunchedEffect(vm) { vm.sheetRequests.collect { key -> sheets.open(key) } }
    val newUnitOpen = sheets.top is NewUnitSheet
    LaunchedEffect(newUnitOpen) { if (newUnitOpen) vm.followNewScene(c.unitKey) else vm.stopFollowingNewScene() }
    LaunchedEffect(pendingScroll, rows) {
        val key = pendingScroll ?: return@LaunchedEffect
        if (rows.none { it.key == key }) return@LaunchedEffect
        showAboveInput(listState, rows, key, motion, bottomPadPx, with(density) { ABOVE_GAP.toPx() })
        pendingScroll = null
    }
    LaunchedEffect(pendingAbove, rows) {
        val key = pendingAbove ?: return@LaunchedEffect
        if (rows.none { it.key == key }) return@LaunchedEffect
        showAboveInput(listState, rows, key, motion, bottomPadPx, with(density) { ABOVE_GAP.toPx() })
        pendingAbove = null
    }

    // --- Actions ---
    fun openPanel(blockKey: Key, tab: PanelTab) {
        revealedRow = null
        revealedSide = SwipeSide.CLOSED
        vm.openPanelAt(blockKey, tab)
        when {
            split != null -> Unit
            ScriptLayout.of(widthDp, panelOpen = true, height = heightDp).panel -> if (!panelOpen) setPanelByWriter(true)
            else -> sheets.open(PanelSheetKey(route.projectKey))
        }
    }
    fun openFind(replace: Boolean = false) {
        keyboard?.hide()
        sheets.open(FindSheetKey(route.projectKey, replace = replace))
    }
    fun openLayers() {
        keyboard?.hide()
        sheets.open(LayersSheetKey(route.projectKey))
    }
    fun startSelect() {
        focusManager.clearFocus()
        keyboard?.hide()
        vm.enterSelect(rows)
    }
    fun runCommand(text: String) {
        when (text) {
            WriteCopy.READ_ALOUD_CMD -> return startReadAloud()
            WriteCopy.VERSIONS_CMD -> return sheets.open(io.github.lenticualrstone.civilight.navigation.EpisodeVersionsSheet(route.projectKey, c.unitKey))
        }
        when (val action = CommandRouting.route(doc, c.unitKey, text)) {
            is CommandAction.Jump -> {
                action.blockKey?.let(vm::selectBlock)
                pendingAbove = action.rowKey
            }
            CommandAction.SetLens -> headerScene?.let(vm::setLensToScene)
            is CommandAction.Renumber -> sheets.open(RenumberSheet(route.projectKey, action.unitKey))
            CommandAction.SelectMode -> startSelect()
            CommandAction.OpenCheckCenter -> deps.navigator.push(CheckCenter(route.projectKey))
            CommandAction.OpenAssets -> if (game) deps.navigator.push(ScriptAssets(route.projectKey))
            is CommandAction.Search -> deps.navigator.push(ProjectSearch(route.projectKey, action.query))
            else -> Unit
        }
    }
    LaunchedEffect(vm) { vm.commandRequests.collect { runCommand(it) } }
    LocalBackHandler.current(selecting && sheets.top == null) { vm.leaveSelect() }
    // Back with an `이 뒤에` mark clears it (the draft stays). Back never ends 고치기: that would drop the edit.
    LocalBackHandler.current(draft.mark is DraftMark.After && sheets.top == null && !selecting) { vm.closeMark() }

    // --- The input bar (SCR-70) ---
    var imeUp by remember { mutableStateOf(false) }
    var fieldFocused by remember { mutableStateOf(false) }
    val inputState = Drafts.inputState(draft, doc, sceneNumber, flash, imeUp, fieldFocused)
    val editKey = (draft.mark as? DraftMark.Edit)?.blockKey
    val mergeReason = editKey?.let { MergeCheck.reason(doc, it, draft.kind, draft.speakerKey) }?.let(WriteCopy::mergeReason)
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val runLayout = remember(draft.runs, draft.pendingParenAt, draft.parenOpen, draft.selEnd, doc, gate.open) {
        io.github.lenticualrstone.civilight.feature.script.editor.RunLayout.of(
            draft.runs, showParens = true, pendingParenAt = draft.pendingParenAt,
            openParenEndAt = draft.selEnd.takeIf { draft.parenOpen && draft.pendingParenAt == null },
            // SCR-55: a link to a locked hidden item is drawn as its mask (the draft drops it on lock as well).
            linkLook = { link -> io.github.lenticualrstone.civilight.feature.script.input.DraftLooks.of(doc, link, gate.open) },
        )
    }
    val transformation = remember(runLayout, colors, type) { DraftTransformation(runLayout, colors, type) }
    val inputActions = ScriptInputActions(
        onEvent = { e ->
            when (e) {
                is ScriptInputEvent.TextChanged -> {
                    if (vm.onDraftText(e.text, e.selectionStart, e.selectionEnd, smartPunctuation = scriptSettings.smartPunctuation) == DraftEffect.OPEN_LINK_PICKER) {
                        sheets.open(DraftLinkSheetKey(route.projectKey))
                    }
                }
                ScriptInputEvent.EnterPressed -> {
                    val t = draft.text
                    val at = draft.selEnd.coerceIn(0, t.length)
                    vm.onDraftText(t.substring(0, at) + "\n" + t.substring(at), at + 1, at + 1, smartPunctuation = scriptSettings.smartPunctuation)
                }
                is ScriptInputEvent.Ime -> imeUp = e.visible
                is ScriptInputEvent.Focus -> fieldFocused = e.focused
                ScriptInputEvent.CloseMark -> vm.closeMark()
                else -> Unit
            }
        },
        onSend = {
            val sentSpeaker = if (draft.kind == BlockKind.DIALOGUE && draft.mark == null) draft.speakerKey else null
            vm.sendDraft(sceneKey)
            revealedRow = null
            // (0.5.0, Labs) Smart speaker swap: after sending a dialogue block, pre-select the previous partner speaker.
            if (scriptSettings.smartSpeakerSwap && sentSpeaker != null) {
                val partner = rows.asReversed()
                    .filterIsInstance<BlockRow>()
                    .firstOrNull { it.kind == BlockKind.DIALOGUE && it.speaker?.characterKey != null && it.speaker.characterKey != sentSpeaker }
                    ?.speaker?.characterKey
                if (partner != null) vm.pickChipSpeaker(partner)
            }
        },
        onSpeakerChip = { if (draft.kind == BlockKind.DIALOGUE) sheets.open(ChipSpeakerSheetKey(route.projectKey, sceneKey)) },
        onKindChip = { sheets.open(KindMenuSheetKey(route.projectKey, editKey = editKey)) },
        onNextScene = { vm.nextScene(sceneKey) },
        onSplit = vm::splitDraft,
        onMerge = vm::mergeDraft,
        onParen = vm::openDraftParen,
        onLink = { sheets.open(DraftLinkSheetKey(route.projectKey)) },
        onChange = { (editKey ?: editor.selected)?.let { b -> vm.changeSheetFor(b)?.let(sheets::open) } },
        onUndo = vm::undoStep,
    )
    val bottomContent: @Composable () -> Unit = {
        val mode = when {
            selecting -> BottomMode.SELECT
            c.readOnly -> BottomMode.READ_ONLY
            previewOnly -> BottomMode.NONE
            else -> BottomMode.INPUT
        }
        AnimatedContent(mode, transitionSpec = { motion.barSwap() }, contentAlignment = Alignment.BottomStart, label = "scriptBottomBar") { m ->
            when (m) {
                BottomMode.SELECT -> {
                    val checked = SelectRules.ordered(select, rows)
                    SelectBottomBar(
                        bar = SelectRows.bar(doc, select),
                        onMove = { sheets.open(MoveSheetKey(route.projectKey, checked)) },
                        onDelete = { vm.deleteChecked(checked) },
                        onCut = { vm.clipChecked(checked, cut = true) },
                        onCopy = { vm.clipChecked(checked, cut = false) },
                        onNewScene = null,
                    )
                }
                // SCR-60: `예시 프로젝트` with `예시 홈으로` · `온보딩 끝내기` in place of the input bar.
                BottomMode.READ_ONLY -> ScriptInputReadOnlyRow(
                    onSecondary = { ReadOnlyFrame.goHome(deps, route.projectKey) },
                    onPrimary = { ReadOnlyFrame.finish(deps, route.projectKey) },
                )
                BottomMode.INPUT -> ScriptInputBar(
                    state = inputState,
                    actions = inputActions,
                    modifier = Modifier.tourTarget(ScriptTourSteps.COMMAND),
                    context = InputContext(
                        readOnly = false,
                        hasCues = game && draft.kind == BlockKind.DIRECTION && draft.cues.isNotEmpty(),
                        hasTargetBlock = editor.selected != null,
                        canUndo = canUndo,
                    ),
                    mergeOffReason = mergeReason,
                    // SCR-74 [DEFAULT]: `다음 씬` is off while it would only pile onto an empty scene.
                    nextSceneEnabled = NextScene.emptyAfter(doc, c.unitKey, route.projectKey, sceneKey, vm.lastNextScene) == null,
                    speakerMenuOpen = sheets.top is ChipSpeakerSheetKey,
                    kindMenuOpen = sheets.top is KindMenuSheetKey,
                    visualTransformation = transformation,
                    // SCR-08: cue chips only with `게임 제작 도구` on, under `연출`.
                    cues = if (game && draft.kind == BlockKind.DIRECTION) {
                        {
                            DraftCueChips(doc, draft.cues, onRemove = vm::removeDraftCue, onAdd = {
                                sheets.open(DraftCueSheetKey(route.projectKey, c.unitKey))
                            })
                        }
                    } else null,
                )
                // 대사만 (SCR-27) is a reading view: no input bar (nothing in it could act).
                BottomMode.NONE -> Spacer(Modifier.fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }

    val railOnLeft = scriptSettings.miniRailSide == io.github.lenticualrstone.civilight.model.app.MiniRailSide.LEFT
    val panelSide = if (railOnLeft) PanelSide.START else PanelSide.END
    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { frameTopPx = it.positionInWindow().y },
    ) {
        Box(
            Modifier
                .align(if (railOnLeft) Alignment.TopEnd else Alignment.TopStart)
                .then(if (upperSettled) Modifier.fillMaxHeight() else Modifier.height(upperHeight.dp))
                .width(layout.areaWidth.dp),
        ) {
            GlassScaffold(
                topBar = {
                    AnimatedContent(selecting, transitionSpec = { motion.barSwap() }, contentAlignment = Alignment.TopStart, label = "scriptTopBar") { sel ->
                        if (sel) {
                            SelectTopBar(header.line, onAll = { vm.checkAll(rows, headerScene) }, onDone = vm::leaveSelect)
                        } else {
                            ScriptTopBar(
                                title = doc.project.name,
                                // SCR-60: `읽기 전용 · 편집 불가` replaces the position line.
                                line = if (c.readOnly) WriteCopy.READ_ONLY_LINE else header.line,
                                positionLabel = header.positionLabel,
                                wide = wide,
                                onPositionLabel = {
                                    sheets.open(io.github.lenticualrstone.civilight.feature.script.synergy.ScriptOutlineSheetKey(route.projectKey, c.unitKey))
                                },
                                onLayers = ::openLayers,
                                onFind = { openFind() },
                                back = if (io.github.lenticualrstone.civilight.ui.nav.LocalPaneRole.current == io.github.lenticualrstone.civilight.ui.nav.PaneRole.DETAIL) null
                                else io.github.lenticualrstone.civilight.designsystem.component.IconAction("뒤로") { deps.navigator.pop() },
                            )
                        }
                    }
                },
                bottomBar = { AnimatedVisibility(split == null, enter = motion.enter(CivilightMotion.foldOpen), exit = motion.exit(CivilightMotion.foldClose)) { bottomContent() } },
                overlay = { padding ->
                    Box(
                        Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding() + TOAST_GAP),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        ToastHost(message = toast?.text, actionLabel = toast?.actionLabel, onAction = toast?.onAction?.let { { toasts.act() } })
                    }
                },
            ) { padding ->
                with(density) { bottomPadPx = padding.calculateBottomPadding().roundToPx() }
                Box(Modifier.fillMaxSize()) {
                    if (selecting) {
                        SelectList(
                            rows = selectRows,
                            listState = selectListState,
                            padding = padding,
                            layout = layout,
                            onToggle = { key -> vm.toggleChecked(key, rows) },
                            onDrop = vm::moveBlocks,
                        )
                    } else Manuscript(
                        rows = rows,
                        editor = editor,
                        showParens = doc.app.view.script.layers.paren,
                        readOnly = c.readOnly,
                        entered = entered,
                        listState = listState,
                        padding = padding,
                        layout = layout,
                        actions = ManuscriptActions(
                            onTapBlock = { row, offset -> vm.tapBlock(row.blockKey, row.isText, offset) },
                            onTapOption = { row, option -> option.optionKey?.let { vm.tapOption(row.blockKey, it) } },
                            onSpeaker = { row -> vm.tapBlock(row.blockKey, row.isText, null) },
                            onTapEmptyScene = { scene -> vm.tapEmptyScene(scene) },
                            onOpenHead = { scene -> sheets.open(SceneWhereSheet(route.projectKey, scene)) },
                            onHeadMenu = { scene ->
                                keyboard?.hide()
                                sheets.open(UnitMenuSheet(route.projectKey, scene))
                            },
                            onUnlockHidden = gate.unlock,
                            onNewScene = if (c.readOnly) null else ({ vm.nextScene(null) }),
                            emptySpeaker = { null },
                            onEntered = vm::enteredShown,
                            editableBlock = { _, _ -> },
                            editableEmpty = { _, _ -> },
                            onPanel = { row, tab -> openPanel(row.blockKey, tab) },
                            onReveal = { key -> revealedRow = key; if (key == null) revealedSide = SwipeSide.CLOSED },
                            onTapTarget = { row, tap ->
                                when (tap) {
                                    // SCR-59: the record opens in the recorder (audio plays there).
                                    is BlockTap.Source -> {
                                        deps.navigator.push(RecorderRecordDetail(tap.recordKey))
                                        true
                                    }
                                    // (0.5.0) Short tap on an `@` mention opens Obsidian-style compact preview sheet
                                    // (`자세히 열기` / `참고 패널`) without breaking reading flow or long-press.
                                    is BlockTap.Link -> {
                                        val link = tap.link
                                        // Rules 19 and 20: preview only a LIVE target; a locked hidden one asks
                                        // for the unlock; trashed, name-tagged or missing ones get the link card.
                                        when (val act = io.github.lenticualrstone.civilight.feature.script.synergy.MentionTap.decide(doc, link.target, gate.open)) {
                                            is io.github.lenticualrstone.civilight.feature.script.synergy.MentionTap.Action.Preview -> sheets.open(
                                                io.github.lenticualrstone.civilight.feature.script.synergy.MentionPreviewSheetKey(
                                                    projectKey = route.projectKey,
                                                    targetKind = act.kind.name,
                                                    targetKey = link.target.key,
                                                ),
                                            )
                                            io.github.lenticualrstone.civilight.feature.script.synergy.MentionTap.Action.Unlock -> gate.unlock()
                                            io.github.lenticualrstone.civilight.feature.script.synergy.MentionTap.Action.Card -> sheets.open(
                                                io.github.lenticualrstone.civilight.feature.script.links.LinkCardSheetKey(
                                                    projectKey = route.projectKey,
                                                    blockKey = row.blockKey,
                                                    linkKey = link.linkKey,
                                                ),
                                            )
                                        }
                                        true
                                    }
                                    else -> false
                                }
                            },
                            onMarginWarning = null,
                            look = { row ->
                                when {
                                    row.blockKey == editKey -> BlockLook(tag = WriteCopy.EDITING_TAG, numbered = true)
                                    (draft.mark as? DraftMark.After)?.blockKey == row.blockKey -> BlockLook(numbered = true, afterTarget = true)
                                    fresh[row.blockKey] != null -> BlockLook(tag = fresh[row.blockKey], numbered = true)
                                    else -> BlockLook()
                                }
                            },
                            editButtons = { row ->
                                val run = blockActions(row, vm, sheets, route, doc, c.unitKey) { tab -> openPanel(row.blockKey, tab) }
                                editButtons(BlockMenu.items(row, off = c.readOnly || previewOnly || draft.editing), run, colors.warn)
                            },
                            menu = { row ->
                                val run = blockActions(row, vm, sheets, route, doc, c.unitKey) { tab -> openPanel(row.blockKey, tab) }
                                BlockMenu.items(row, off = c.readOnly || previewOnly || draft.editing).map { item ->
                                    BlockMenuEntry(item, run(item), accent = item == BlockMenuItem.TIME && row.hasChange)
                                }
                            },
                            onLongPress = { row, bounds ->
                                keyboard?.hide()
                                vm.clearFresh()
                                blockMenu = BlockMenuOpen(row.key, row.blockKey, bounds)
                            },
                            onLiftedBounds = { key, bounds ->
                                blockMenu?.let { if (it.rowKey == key && it.bounds != bounds) blockMenu = it.copy(bounds = bounds) }
                            },
                            onSwipe = { key, side ->
                                revealedRow = key
                                revealedSide = side
                                if (key != null) vm.clearFresh()
                            },
                        ),
                        headMenuOpen = (sheets.top as? UnitMenuSheet)?.unitKey,
                        previewOnly = previewOnly,
                        revealedRow = revealedRow,
                        revealedSide = revealedSide,
                        liftedRow = blockMenu?.liftedRow,
                        blockSpacing = scriptSettings.blockSpacing,
                    )
                }
            }
        }
        val panelWidth = rememberLastNonNull(targetLayout.panelWidth.takeIf { targetLayout.panel }) ?: ScriptLayout.panelWidthFor(widthDp, heightDp)
        val seamShown = targetLayout.panel || targetLayout.rail
        if (seamShown) {
            val railActions = scriptSettings.miniRailScript.mapNotNull { item ->
                when (item) {
                    io.github.lenticualrstone.civilight.model.app.MiniRailScriptItem.TIME ->
                        io.github.lenticualrstone.civilight.ui.window.MiniRailAction(
                            icon = IconId.Clock,
                            label = "시간",
                            active = panelTab == PanelTab.TIME,
                            onClick = { vm.setPanelTab(PanelTab.TIME); panelState.setClosed(false) },
                        )
                    io.github.lenticualrstone.civilight.model.app.MiniRailScriptItem.REF ->
                        io.github.lenticualrstone.civilight.ui.window.MiniRailAction(
                            icon = IconId.Book,
                            label = "참고",
                            active = panelTab == PanelTab.REF,
                            onClick = { vm.setPanelTab(PanelTab.REF); panelState.setClosed(false) },
                        )
                    io.github.lenticualrstone.civilight.model.app.MiniRailScriptItem.PROPS ->
                        io.github.lenticualrstone.civilight.ui.window.MiniRailAction(
                            icon = IconId.Sliders,
                            label = "속성",
                            active = panelTab == PanelTab.PROPS,
                            onClick = { vm.setPanelTab(PanelTab.PROPS); panelState.setClosed(false) },
                        )
                    io.github.lenticualrstone.civilight.model.app.MiniRailScriptItem.MODS ->
                        io.github.lenticualrstone.civilight.ui.window.MiniRailAction(
                            icon = IconId.Grid,
                            label = "모듈",
                            active = panelTab == PanelTab.MODS,
                            onClick = { vm.setPanelTab(PanelTab.MODS); panelState.setClosed(false) },
                        )
                    io.github.lenticualrstone.civilight.model.app.MiniRailScriptItem.OUTLINE ->
                        io.github.lenticualrstone.civilight.ui.window.MiniRailAction(
                            icon = IconId.Outline,
                            label = "씬 목차",
                            onClick = {
                                sheets.open(io.github.lenticualrstone.civilight.feature.script.synergy.ScriptOutlineSheetKey(route.projectKey, c.unitKey))
                            },
                        )
                    io.github.lenticualrstone.civilight.model.app.MiniRailScriptItem.SPEAKER_FOCUS ->
                        io.github.lenticualrstone.civilight.ui.window.MiniRailAction(
                            icon = IconId.User,
                            label = "화자 포커스",
                            onClick = {
                                sheets.open(io.github.lenticualrstone.civilight.feature.script.synergy.SpeakerFocusSheetKey(route.projectKey, c.unitKey))
                            },
                        )
                }
            }
            PanelRail(
                panelState,
                panelSide,
                Modifier.align(if (railOnLeft) Alignment.TopStart else Alignment.TopEnd),
                topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                actions = railActions,
            )
        }
        AnimatedVisibility(
            visible = targetLayout.panel,
            modifier = Modifier.align(if (railOnLeft) Alignment.TopStart else Alignment.TopEnd),
            enter = if (ScriptLayout.slidesFromRail(beforePanel)) motion.panelToggleEnter() else motion.panelEnter(),
            exit = if (ScriptLayout.slidesFromRail(targetLayout)) motion.panelToggleExit() else motion.panelExit(),
        ) {
            val actions = scriptPanelActions(route, deps, vm, gate.open, open = { sheets.open(it) }, leave = {})
            ScriptSidePanel(
                tab = panelTab,
                onSelectTab = vm::setPanelTab,
                scrollOf = panelScroll::state,
                width = panelWidth.dp,
            ) { tab ->
                panel?.takeIf { it.tab == tab }?.let { PanelBody(it, wide = true, actions = actions, c = c, vm = vm) }
            }
        }
        if (seamShown) {
            PanelSeamButton(
                panelState,
                panelSide,
                panelDp = { panelWidth },
                modifier = Modifier.align(if (railOnLeft) Alignment.CenterStart else Alignment.CenterEnd).fillMaxWidth(),
            )
        }
        val lastSplit = rememberLastNonNull(split)
        AnimatedVisibility(
            visible = split != null,
            enter = motion.enter(CivilightMotion.foldOpen, rise = TABLETOP_RISE),
            exit = motion.exit(CivilightMotion.foldClose),
        ) {
            if (lastSplit != null) {
                val actions = scriptPanelActions(route, deps, vm, gate.open, open = { sheets.open(it) }, leave = {})
                TabletopPanel(
                    tab = panelTab,
                    onSelectTab = vm::setPanelTab,
                    scrollOf = panelScroll::state,
                    modifier = Modifier.padding(top = (lastSplit.upper + lastSplit.gap).dp).height(lastSplit.lower.dp).fillMaxWidth(),
                    commandArea = bottomContent,
                ) { tab ->
                    panel?.takeIf { it.tab == tab }?.let { PanelBody(it, wide = true, actions = actions, c = c, vm = vm) }
                }
            }
        }
        // (0.2.4) A block's long-press menu over the whole screen; it closes when its block leaves the manuscript or
        // the screen changes mode (select, 고치기 starting elsewhere, a sheet on top).
        blockMenu?.let { open ->
            val row = rows.firstOrNull { it.key == open.rowKey } as? BlockRow
            val gone = row == null || selecting || sheets.top != null
            LaunchedEffect(gone) { if (gone) blockMenu = null }
            if (row != null && !selecting && sheets.top == null) {
                val run = blockActions(row, vm, sheets, route, doc, c.unitKey) { tab -> openPanel(row.blockKey, tab) }
                val entries = BlockMenu.items(row, off = c.readOnly || previewOnly || draft.editing).map { item ->
                    BlockMenuEntry(item, run(item), accent = item == BlockMenuItem.TIME && row.hasChange)
                }
                key(open.rowKey) {
                    BlockMenuOverlay(
                        open,
                        entries,
                        onClosing = { blockMenu?.let { if (it.rowKey == open.rowKey) blockMenu = it.copy(closing = true) } },
                        onDismiss = { if (blockMenu?.rowKey == open.rowKey) blockMenu = null },
                    )
                }
            }
        }
    }
}

/**
 * What each block action does (SCR-30 swipe, 0.2.4 long-press menu): the swipe's edit side and the menu run the
 * same code. `이 블록만 · 표시 이름` opens 속성, where that field is.
 */
private fun blockActions(
    row: BlockRow,
    vm: ScriptViewModel,
    sheets: SheetHost,
    route: Script,
    doc: ProjectDoc,
    unitKey: Key,
    openPanel: (PanelTab) -> Unit,
): (BlockMenuItem) -> () -> Unit = { item ->
    val key = row.blockKey
    when (item) {
        BlockMenuItem.EDIT -> ({ vm.swipeEdit(key) })
        BlockMenuItem.INSERT_AFTER -> ({ vm.swipeAfter(key) })
        BlockMenuItem.KIND -> ({ sheets.open(KindMenuSheetKey(route.projectKey, blockKey = key)) })
        // SCR-49: not the last block → `씬 나누기` below it; the last → `다음 씬 만들기` after its scene.
        BlockMenuItem.SCENE_CUT -> ({ sheets.open(ScriptOps.newSceneSheet(doc, route.projectKey, key, row.sceneKey, unitKey)) })
        BlockMenuItem.PLANT -> ({ if (!vm.readOnly) sheets.open(io.github.lenticualrstone.civilight.feature.script.input.PlantSheetKey(route.projectKey, key)) })
        BlockMenuItem.MENTION_OR_PAYOFF -> ({ if (!vm.readOnly) sheets.open(io.github.lenticualrstone.civilight.feature.script.input.MarkPickSheetKey(route.projectKey, key)) })
        BlockMenuItem.DISPLAY_NAME, BlockMenuItem.PROPS -> ({ openPanel(PanelTab.PROPS) })
        BlockMenuItem.TIME -> ({ openPanel(PanelTab.TIME) })
        BlockMenuItem.REF -> ({ openPanel(PanelTab.REF) })
        BlockMenuItem.DELETE -> ({ vm.swipeDelete(key) })
    }
}

/**
 * The edit side's five buttons (SCR-30; 0.2.4: a left swipe, at the right end); none in read-only projects,
 * `대사만` or during `고치기` ([BlockMenu.items]). `지우기` is drawn in `warn`.
 */
private fun editButtons(items: List<BlockMenuItem>, run: (BlockMenuItem) -> () -> Unit, warn: androidx.compose.ui.graphics.Color): List<SwipeButtonSpec> {
    return BlockMenu.swipeEdit(items).map { item ->
        val icon = item.icon ?: IconId.Edit
        SwipeButtonSpec(item.label, run(item), labelColor = if (item.warning) warn else null) {
            SwipeIcon(icon, warning = item.warning)
        }
    }
}

@Composable
private fun SwipeIcon(icon: IconId, warning: Boolean = false) {
    CivilightIcon(icon, null, size = 18.dp, tint = if (warning) CivilightTheme.colors.warn else CivilightTheme.colors.ink2)
}

/** Select mode: the scene of the first select row under the top bar. */
private fun sceneUnderSelectBar(listState: LazyListState, rows: List<SelectRowModel>): Key? {
    val first = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.offset + it.size > 0 } ?: return null
    for (i in first.index downTo 0) {
        when (val r = rows.getOrNull(i)) {
            is SelectRowModel.Head -> return r.sceneKey
            is SelectRowModel.Block -> return r.sceneKey
            null -> Unit
        }
    }
    return null
}

/**
 * SCR-71: scrolls so [key]'s bottom sits [gapPx] above the input bar (a block taller than the band shows its
 * top). Motion through `CivilightMotion` (`enter`, decelerate); reduced motion snaps.
 */
private suspend fun showAboveInput(listState: LazyListState, rows: List<ScriptRow>, key: String, motion: Motion, bottomPadPx: Int, gapPx: Float) {
    val index = rows.indexOfFirst { it.key == key }
    if (index < 0) return
    if (listState.layoutInfo.visibleItemsInfo.none { it.index == index }) listState.scrollToItem(index)
    val info = listState.layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val bandTop = info.beforeContentPadding.toFloat()
    val bandBottom = (info.viewportEndOffset - bottomPadPx).toFloat() - gapPx
    val delta = if (item.size > bandBottom - bandTop) item.offset - bandTop else (item.offset + item.size) - bandBottom
    if (kotlin.math.abs(delta) < 1f) return
    if (motion.reduced) listState.scrollBy(delta) else listState.animateScrollBy(delta, motion.spec(CivilightMotion.enter))
}

/** What the script's bottom bar shows (motion audit, row 1). */
private enum class BottomMode { INPUT, READ_ONLY, SELECT, NONE }

private const val HANDLE_HALF = 22f

/** CNV-47 grip under the seam tab: half the tab's 72 dp touch area + half the grip's 44 dp + 8 dp. */
private val SPLIT_GRIP_DROP = 66.dp

private val TABLETOP_RISE = 24.dp

private val TOAST_GAP = 6.dp

/** SCR-71: a new block's bottom sits 12 dp above the input bar. */
private val ABOVE_GAP = 12.dp
