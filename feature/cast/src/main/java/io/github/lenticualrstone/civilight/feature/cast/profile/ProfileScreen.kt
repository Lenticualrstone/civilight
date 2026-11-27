package io.github.lenticualrstone.civilight.feature.cast.profile

import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import io.github.lenticualrstone.civilight.ui.project.EditResult
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.lenticualrstone.civilight.domain.actions.CharacterFieldActions
import io.github.lenticualrstone.civilight.domain.cast.Appearances
import io.github.lenticualrstone.civilight.feature.cast.common.toAction
import io.github.lenticualrstone.civilight.feature.cast.common.CastAuth
import io.github.lenticualrstone.civilight.model.game.GameTools
import io.github.lenticualrstone.civilight.ui.security.AuthAvailability
import io.github.lenticualrstone.civilight.ui.security.AuthRequest
import io.github.lenticualrstone.civilight.ui.security.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import io.github.lenticualrstone.civilight.designsystem.component.ImmersiveDetailScaffold
import io.github.lenticualrstone.civilight.designsystem.component.ImmersiveHeaderButton
import io.github.lenticualrstone.civilight.designsystem.component.ImmersiveMath
import io.github.lenticualrstone.civilight.designsystem.component.LensChip
import io.github.lenticualrstone.civilight.designsystem.component.ReportBottomChrome
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ToastHost
import io.github.lenticualrstone.civilight.designsystem.component.rememberImmersiveDetailState
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.domain.cast.DisplayNames
import io.github.lenticualrstone.civilight.domain.cast.field.FieldRow
import io.github.lenticualrstone.civilight.model.app.ProfileTabKind
import io.github.lenticualrstone.civilight.ui.lens.LensChipLabel
import io.github.lenticualrstone.civilight.ui.profile.ProfileTabSetup
import io.github.lenticualrstone.civilight.ui.profile.ProfileTabSetupContent
import io.github.lenticualrstone.civilight.ui.scaffold.CommandScope
import io.github.lenticualrstone.civilight.ui.scaffold.LocalCommandBarHost
import io.github.lenticualrstone.civilight.ui.scaffold.LocalToasts
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHostLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.designsystem.theme.rememberLastNonNull
import io.github.lenticualrstone.civilight.feature.cast.gallery.CastGallery
import io.github.lenticualrstone.civilight.feature.cast.gallery.GallerySection
import io.github.lenticualrstone.civilight.navigation.Gallery
import io.github.lenticualrstone.civilight.navigation.GalleryAddSheet
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.field.Profile
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationViews
import io.github.lenticualrstone.civilight.domain.cast.voice.ArchiveDocs
import io.github.lenticualrstone.civilight.domain.cast.voice.VoiceLines
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.CastViewModel
import io.github.lenticualrstone.civilight.feature.cast.common.castLens
import io.github.lenticualrstone.civilight.feature.cast.common.editContext
import io.github.lenticualrstone.civilight.feature.cast.field.DisplayNameEditSheet
import io.github.lenticualrstone.civilight.feature.cast.field.DisplayNameHistoryContent
import io.github.lenticualrstone.civilight.feature.cast.field.DisplayNameHistorySheet
import io.github.lenticualrstone.civilight.feature.cast.field.DisplayNameSheetContent
import io.github.lenticualrstone.civilight.feature.cast.trash.rememberItemTrash
import io.github.lenticualrstone.civilight.feature.cast.relation.RelationSlotContent
import io.github.lenticualrstone.civilight.feature.cast.relation.RelationSlotSheet
import io.github.lenticualrstone.civilight.domain.cast.relation.SheetSlot
import io.github.lenticualrstone.civilight.feature.cast.gametext.VoiceLineContent
import io.github.lenticualrstone.civilight.feature.cast.gametext.VoiceLineSheet
import io.github.lenticualrstone.civilight.navigation.ArchiveDocEdit
import io.github.lenticualrstone.civilight.navigation.ImagePickSheet
import io.github.lenticualrstone.civilight.feature.cast.image.CharacterImages
import io.github.lenticualrstone.civilight.feature.cast.image.ImagePlace
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.text.docText
import io.github.lenticualrstone.civilight.navigation.AddFieldSheet
import io.github.lenticualrstone.civilight.navigation.DeleteImpactSheet
import io.github.lenticualrstone.civilight.navigation.MergeItemsSheet
import io.github.lenticualrstone.civilight.navigation.RecorderInbox
import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.feature.cast.common.CastWrites
import io.github.lenticualrstone.civilight.feature.cast.common.HiddenMark
import io.github.lenticualrstone.civilight.feature.cast.common.Step
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.FieldHistorySheet
import io.github.lenticualrstone.civilight.navigation.FieldValueEditSheet
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.navigation.ProfileTab
import io.github.lenticualrstone.civilight.ui.tour.tourTarget
import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.navigation.RelationEditSheet
import io.github.lenticualrstone.civilight.navigation.RelationMap
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.lens.lensChipLabel
import io.github.lenticualrstone.civilight.ui.nav.LocalPaneRole
import io.github.lenticualrstone.civilight.ui.nav.PaneRole
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout

/** The character record (CST-07 … CST-12, CST-28); its sheets carry the edits. */
class ProfileViewModel(
    projectKey: Key,
    private val characterKey: Key,
    sessions: ProjectSessions,
    contextOf: () -> io.github.lenticualrstone.civilight.domain.cast.EditContext,
) : CastViewModel(projectKey, sessions, contextOf) {
    /**
     * The selected tab and the 비밀 gate (CST-73). Held here, not in saved UI state: a configuration change
     * (fold, unfold) keeps a confirmed tab open, and nothing outlives the record.
     */
    private val tabState = MutableStateFlow<TabGateState?>(null)
    val tabs: StateFlow<TabGateState?> = tabState

    fun ensureTabs(initial: () -> TabGateState) {
        if (tabState.value == null) set(initial())
    }

    /** Synchronous, so a relock from the lifecycle observer reaches the secret sheets before the next frame. */
    fun updateTabs(change: (TabGateState) -> TabGateState) {
        tabState.value?.let { set(change(it)) }
    }

    private fun set(state: TabGateState) {
        tabState.value = state
        CastAuth.publishTab(characterKey, state)
    }

    override fun onCleared() {
        CastAuth.publishTab(characterKey, null)
        super.onCleared()
    }
}

/** 인물 기록 (`Profile`, `RTEditTwo`, `RTPlanned`, `Relations`; inner in the `TwoCast` right pane or `ColCast`). */
@Composable
fun ProfileScreen(route: CharacterProfile, deps: FeatureDeps) {
    // Keyed by character: the inner pane swaps records under one entry, and the 비밀 gate must never carry over.
    val vm = entryViewModel(key = "profile:${route.characterKey}") { ProfileViewModel(route.projectKey, route.characterKey, deps.projects, deps::editContext) }
    val load by vm.state.collectAsState()
    val ready = load as? ProjectLoad.Ready ?: return
    val doc = ready.doc
    val p = route.projectKey
    val c = route.characterKey
    val appSettings by deps.settings.collectAsState()
    var speechOverride by remember(c) { mutableStateOf(false) }
    var secretOverride by remember(c) { mutableStateOf(false) }
    // The modular tab before the 비밀 tab was picked: a cancelled prompt goes back to it (no empty body).
    var beforeSecretId by rememberSaveable(c) { mutableStateOf<String?>(null) }
    val gate = rememberHiddenGate(p)
    val context = LocalContext.current
    // CST-74: 4 tabs while `게임 제작 도구` is off. CST-73: the 비밀 tab behind the device lock while locked.
    val gameTools = GameTools.on(doc)
    // CST-74: `게임 텍스트` only while `게임 제작 도구` is on, whatever the preset says.
    val modularTabs = remember(appSettings, gameTools) {
        ProfileTabSet.visible(ProfileTabSetup.active(appSettings), gameTools)
    }
    var selectedModularId by rememberSaveable(c) { mutableStateOf(modularTabs.first().id) }
    val activeModularTab = modularTabs.firstOrNull { it.id == selectedModularId } ?: modularTabs.first()
    val storedLocked = doc.index.character(c)?.secretTabLocked == true
    // CST-73: the lock turned off by anything but the confirmed `이 탭 잠그기` switch asks for the device lock first
    // (since 0.2.0 the lock is written without an undo step, so undo and redo cannot; this guards any other writer); the tab is treated as locked while that prompt is on screen, and a cancel locks it again.
    var ownUnlock by remember(c) { mutableStateOf(false) }
    var unlockPending by remember(c) { mutableStateOf(false) }
    val secretLocked = storedLocked || unlockPending
    vm.ensureTabs { SecretTabGate.open(route.tab, gameTools, secretLocked) }
    val tabState = vm.tabs.collectAsState().value ?: SecretTabGate.open(route.tab, gameTools, secretLocked)
    LaunchedEffect(gameTools) { vm.updateTabs { SecretTabGate.gameToolsChanged(it, gameTools) } }
    // N-43: a cancelled or failed prompt (or a relock that moved the gate off 비밀) drops the 비밀 selection.
    LaunchedEffect(SecretTabGate.secretSelected(tabState)) {
        if (SecretTabGate.secretSelected(tabState)) return@LaunchedEffect
        secretOverride = false
        if (activeModularTab.kind == io.github.lenticualrstone.civilight.model.app.ProfileTabKind.SECRET) {
            selectedModularId = beforeSecretId?.takeIf { id -> modularTabs.any { it.id == id && it.kind != io.github.lenticualrstone.civilight.model.app.ProfileTabKind.SECRET } }
                ?: modularTabs.firstOrNull { it.kind != io.github.lenticualrstone.civilight.model.app.ProfileTabKind.SECRET }?.id
                ?: selectedModularId
        }
    }
    val shownTabs = ProfileTabs.shown(gameTools)
    val shownTab = tabState.shown(secretLocked)
    val prompting = tabState.prompting(secretLocked)
    LaunchedEffect(prompting) {
        if (!prompting) return@LaunchedEffect
        val result = deps.biometric.authenticate(AuthRequest(ProfileTabs.label(ProfileTab.SECRET)))
        vm.updateTabs { SecretTabGate.answered(it, result) }
        if (SecretTabGate.needsDeviceLock(result)) {
            deps.toasts.show(CastUiCopy.SECRET_NO_DEVICE_LOCK, CastUiCopy.SECRET_NO_DEVICE_LOCK_ACTION) {
                runCatching { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
    }
    var lastStored by remember(c) { mutableStateOf(storedLocked) }
    LaunchedEffect(storedLocked) {
        val was = lastStored
        lastStored = storedLocked
        if (!was || storedLocked) return@LaunchedEffect
        if (ownUnlock) {
            ownUnlock = false
            return@LaunchedEffect
        }
        if (ready.readOnly) return@LaunchedEffect
        unlockPending = true
        vm.updateTabs { SecretTabGate.background(it) }
        if (!CastAuth.confirm(deps, AuthRequest(ProfileTabs.label(ProfileTab.SECRET)))) {
            vm.actWithoutUndo(then = { unlockPending = false }) { d, ctx -> CharacterFieldActions.setSecretTabLocked(d, c, true, ctx.toAction()) }
        } else {
            unlockPending = false
        }
    }
    // CST-73: leaving the record (a screen pushed over it, another record in the pane, the record popped) locks the
    // tab again and coming back asks again. The record only re-parented (fold, unfold: the activity handles those
    // configuration changes itself, so nothing is recreated) is still the top of the back stack and keeps it.
    val lockedNow by rememberUpdatedState(secretLocked)
    DisposableEffect(vm, c) {
        vm.updateTabs { SecretTabGate.foreground(it, lockedNow) }
        onDispose {
            if (deps.navigator.backStack.lastOrNull() != route) {
                vm.updateTabs { SecretTabGate.background(it, ownPrompt = CastAuth.inFlight) }
                // Unpublished while left: a sheet opened from another screen asks on its own.
                CastAuth.publishTab(c, null)
            }
        }
    }
    // Re-read when the app comes back: a screen lock set or removed in the system settings meanwhile.
    var canAuthenticate by remember { mutableStateOf(deps.biometric.availability() != AuthAvailability.NONE) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, vm, secretLocked) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> vm.updateTabs { SecretTabGate.background(it, ownPrompt = CastAuth.inFlight) }
                Lifecycle.Event.ON_START -> {
                    canAuthenticate = deps.biometric.availability() != AuthAvailability.NONE
                    vm.updateTabs { SecretTabGate.foreground(it, secretLocked) }
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    var showEmpty by rememberSaveable(c) { mutableStateOf(false) }
    var headerExpanded by rememberSaveable(c) { mutableStateOf(false) }
    val wide = LocalWindowLayout.current.isInner
    val cover = !wide
    val ui = ProfileUi(shownTab, showEmpty, headerExpanded)
    val built = remember(doc, ui, cover, gate.open, ready.readOnly) { ProfileModel.build(doc, c, ui, cover, gate.open, ready.readOnly) }
    // Every row `보기` or `항목 더 보기` can bring in, so those rows fold in and out instead of jumping (P2).
    val full = remember(doc, ui, cover, gate.open, ready.readOnly) { ProfileModel.build(doc, c, ui.copy(showEmpty = true, headerExpanded = true), cover, gate.open, ready.readOnly) }
    // A record that leaves because it was trashed or merged stays drawn while the screen pops (row 13); one that
    // is hidden and locked again goes at once, so protected content never lingers (D5).
    val lastState = rememberLastNonNull(built)
    val relocked = built == null && !gate.open && doc.index.character(c)?.let { it.hidden && it.deletedAt == null } == true
    val state = if (relocked) null else lastState
    if (state == null) {
        LaunchedEffect(c) { deps.navigator.pop() }
        return
    }
    val fullState = rememberLastNonNull(full) ?: state
    val leaving = built == null
    if (leaving) LaunchedEffect(c) { deps.navigator.pop() }
    val motion = CivilightTheme.motion
    val sheets = rememberSheetHost()
    val editable = !state.readOnly && !leaving
    val trash = rememberItemTrash(p)
    val writes = remember(deps, p) { CastWrites(deps, p) }
    val scope = rememberCoroutineScope()
    val recorderLink = rememberRecorderLink(p, c, doc)
    // CST-73: the merge sheet itself asks for the device lock before it shows a locked character's values.
    val openMerge: () -> Unit = { sheets.open(MergeItemsSheet(p, EntityKind.CHARACTER, c, c)) }
    val actions = RecordActions(
        editable = editable,
        onField = { sheets.open(FieldValueEditSheet(p, EntityKind.CHARACTER, c, it)) },
        onFieldHistory = { sheets.open(FieldHistorySheet(p, EntityKind.CHARACTER, c, it)) },
        onFixed = { field ->
            sheets.open(
                when (field) {
                    FixedField.NAME -> NameEditSheet(p, c)
                    FixedField.DISPLAY_NAME -> DisplayNameEditSheet(p, c)
                    FixedField.ONE_LINER -> OneLinerEditSheet(p, c)
                    FixedField.TAGS -> TagsEditSheet(p, c)
                    // CST-13: picking, presets, crops and holo are the image area's sheet.
                    FixedField.PORTRAIT -> ImagePickSheet(p, ImageOwnerKind.CHARACTER, c, ImageSlot.CHARACTER_PORTRAIT)
                    FixedField.BACKGROUND -> ImagePickSheet(p, ImageOwnerKind.CHARACTER, c, ImageSlot.CHARACTER_BACKGROUND)
                },
            )
        },
        onDisplayNameHistory = { sheets.open(DisplayNameHistorySheet(p, c)) },
        onToggleEmpty = { showEmpty = !showEmpty },
        onAddField = { sheets.open(AddFieldSheet(p, EntityKind.CHARACTER, c)) },
        onMap = { deps.navigator.push(RelationMap(p, c)) },
        onAddRelation = { sheets.open(RelationEditSheet(p, endAKey = c)) },
        onRelation = { partner -> sheets.open(RelationEditSheet(p, endAKey = c, endBKey = partner)) },
        onRestorePartner = { partner -> trash.restore(EntityKind.CHARACTER, partner) },
        onKeepPartner = { partner, relationKey -> trash.keep(partner, relationKey) },
        onEditSpeech = { sheets.open(BodyEditSheet(p, c, BodyPart.SPEECH)) },
        onAddress = { partner -> sheets.open(RelationSlotSheet(p, c, partner, SheetSlot.ADDRESS)) },
        onAddAddress = { sheets.open(RelationSlotSheet(p, c, null, SheetSlot.ADDRESS)) },
        onEditSecret = { sheets.open(BodyEditSheet(p, c, BodyPart.SECRET)) },
        onVoiceLine = { line -> sheets.open(VoiceLineSheet(p, c, line)) },
        onToggleVoice = { line, label -> vm.act(label) { d, ctx -> VoiceLines.toggleStatus(d, line, ctx) } },
        onArchive = { key -> deps.navigator.push(ArchiveDocEdit(p, c, key)) },
        onDeletedPartner = { partner -> sheets.open(DeleteImpactSheet(p, EntityKind.CHARACTER, partner)) },
        onRecorder = { deps.navigator.push(RecorderInbox(projectKey = p, targetKind = RecorderLinkModel.TARGET_KIND, targetKey = c)) },
        onSameNameMerge = { openMerge() },
        onGalleryAdd = { sheets.open(GalleryAddSheet(p, GalleryOwnerKind.CHARACTER, c)) },
        onGalleryAll = { deps.navigator.push(Gallery.ofCharacter(p, c)) },
        // The gallery screen opens in its remembered view (card, grid or list) with this item picked (GAL-08).
        onGalleryCard = { item -> deps.navigator.push(Gallery.ofCharacter(p, c, pickKey = item)) },
    )
    val gallery = remember(doc, c, gate.open) { CastGallery.section(doc, c, gate.open) }
    val secret = SecretTabExtras(
        summaryEdit = if (editable) ({ sheets.open(SpeechSummarySheet(p, c)) }) else null,
        locked = secretLocked,
        // CST-73: shown when the device can authenticate, or when the lock is already on (never hidden while set).
        showLock = canAuthenticate || secretLocked,
        lockEnabled = editable && canAuthenticate,
        onLock = { on ->
            if (on) {
                vm.updateTabs(SecretTabGate::lockedFromInside)
                ownUnlock = false
                vm.actWithoutUndo { d, ctx -> CharacterFieldActions.setSecretTabLocked(d, c, true, ctx.toAction()) }
            } else {
                // Turning it off asks for the device lock first; a cancel leaves it on.
                scope.launch {
                    if (CastAuth.confirm(deps, AuthRequest(ProfileTabs.label(ProfileTab.SECRET)))) {
                        ownUnlock = true
                        vm.actWithoutUndo(then = { if (it != EditResult.APPLIED) ownUnlock = false }) { d, ctx ->
                            CharacterFieldActions.setSecretTabLocked(d, c, false, ctx.toAction())
                        }
                    }
                }
            }
        },
    )
    // Title: the display name at the lens (`???` while it is overridden, CST-11); subtitle: the one-line head info.
    val titleCard = remember(doc, c) { DisplayNames.card(doc, c, doc.castLens) }
    val title = titleCard.first
    // The title opens `이름` (or `표시 이름` while it reads `???`) only on an editable, unmasked record.
    val titleField = RecordHeroTaps.titleField(editable, hidden = state.hidden, hiddenUnlocked = gate.open, overridden = titleCard.second != null)
    // The subtitle line (the one-liner, else head values) opens `한 줄 소개` on the same terms.
    val subtitleField = RecordHeroTaps.subtitleField(editable, hidden = state.hidden, hiddenUnlocked = gate.open)
    val subtitle = remember(state) { ProfileHeadLine.subtitle(state.oneLiner, state.header.rows) }
    // Saved per record: the inner pane swaps records under one entry, and each opens at its own scroll.
    val scroll = rememberSaveable(c, saver = ScrollState.Saver) { ScrollState(0) }
    val immersive = rememberImmersiveDetailState(scroll)
    val lens = lensChipLabel(doc)
    val toasts = LocalToasts.current
    val toast by toasts.current.collectAsState()
    // Two-pane inner screen: the shared command bar sits under the pane; the record stops above it.
    val paneHost = if (CommandScope.of(LocalPaneRole.current) != null) LocalCommandBarHost.current else null
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // The pane's padding below already holds the frame bar and the navigation inset: the scaffold adds none.
    val bottomInset = if (paneHost != null && paneHost.panePadding > 0.dp) 0.dp else null
    // `목록 + 상세`: the record in the detail pane has no back (the list is beside it).
    val inDetailPane = LocalPaneRole.current == PaneRole.DETAIL
    CenterColumn {
        SheetHostLayout(
            sheets,
            localSheets = { key ->
                when (key) {
                    is NameEditSheet -> { { NameSheetContent(key) } }
                    is OneLinerEditSheet -> { { OneLinerSheetContent(key) } }
                    is TagsEditSheet -> { { TagsSheetContent(key) } }
                    is SpeechSummarySheet -> { { SpeechSummaryContent(key) } }
                    is DisplayNameEditSheet -> { { DisplayNameSheetContent(key) } }
                    is DisplayNameHistorySheet -> { { DisplayNameHistoryContent(key) } }
                    is VoiceLineSheet -> { { VoiceLineContent(key) } }
                    is BodyEditSheet -> { { BodyEditContent(key) } }
                    is RelationSlotSheet -> { { RelationSlotContent(key) } }
                    ProfileTabTemplatesSheetKey -> {
                        {
                            ProfileTabSetupContent(
                                settings = appSettings,
                                onSelectPreset = { preset -> deps.appScope.launch { deps.stores.settings.setProfileTabPreset(preset) } },
                                onUpdateTabs = { next -> deps.appScope.launch { deps.stores.settings.setCustomProfileTabs(next) } },
                                onClose = { close() },
                            )
                        }
                    }
                    is RecordMenuSheet -> {
                        {
                            RecordMenuContent(
                                state.name,
                                hidden = state.hidden,
                                // CST-39: hiding closes the record while hidden items are locked; the list then shows the lock row.
                                onHide = {
                                    close()
                                    val hide = !state.hidden
                                    writes.launch(if (hide) CastUiCopy.BTN_HIDE else CastUiCopy.BTN_UNHIDE, { d, ctx -> Step(CharacterEdits.setHidden(d, c, hide, ctx.edit), Unit) })
                                },
                                onDelete = { close(); trash.requestDelete(EntityKind.CHARACTER, c, sheets::open) },
                                onMerge = { close(); openMerge() },
                            )
                        }
                    }
                    else -> null
                }
            },
        ) {
            // Sized from the pane (BoxWithConstraints), never from the window: the record works in the inner right pane.
            val frameBottomInset = paneHost?.panePadding?.takeIf { it > 0.dp }
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val paneWide = ImmersiveMath.isWide(maxWidth.value)
                val bgArt = remember(doc, c, paneWide) { CharacterImages.background(doc, c, inner = paneWide) }
                val cardPortrait = remember(doc, c) { CharacterImages.image(doc, c, ImagePlace.CARD) }
                // Hero: the back layer (background, else the portrait while `초상 앰비언트 블러` is on) and the portrait
                // card (holo on: the tilting holo card) are independent; both draw when both apply.
                val hero = RecordHeroLayout.layers(bgArt, cardPortrait, portraitBack = appSettings.portraitAmbientBlur)
                val hasArt = hero.back != null || hero.card != null
                val artSlotField = RecordHeroTaps.artField(hasBackground = bgArt != null)
                val artTaps = RecordHeroTaps.artTaps(editable)
                val hasSubtitle = !subtitle.isNullOrEmpty()
                val effectiveBottom = frameBottomInset ?: bottomInset
                if (paneHost == null) ReportBottomChrome(ImmersiveMath.BAR_ROW_DP.dp)
                ImmersiveDetailScaffold(
                    title = title,
                    subtitle = subtitle,
                    onBack = if (inDetailPane) null else ({ deps.navigator.pop() }),
                    backLabel = CastUiCopy.BACK,
                    bottomInset = effectiveBottom,
                    hasHeroArt = hasArt,
                    onHeroClick = if (artTaps) ({ actions.onFixed(artSlotField) }) else null,
                    heroLabel = RecordHeroTaps.label(artSlotField),
                    onTitleClick = titleField?.let { f -> { actions.onFixed(f) } },
                    titleClickLabel = titleField?.let(RecordHeroTaps::label),
                    onSubtitleClick = subtitleField?.let { f -> { actions.onFixed(f) } },
                    subtitleClickLabel = subtitleField?.let(RecordHeroTaps::label),
                    heroTargets = if (artTaps) ({
                        RecordHeroTargets(hero, hasSubtitle, effectiveBottom) { actions.onFixed(RecordHeroTaps.cardField) }
                    }) else null,
                    hero = { RecordImmersiveHero(hero, hasSubtitle = hasSubtitle, navInset = effectiveBottom) },
                    actions = {
                        // Board deviation: the board's header has only back and `⋯`; the lens stays on screen here
                        // (values follow it), as the record's floating header had it.
                        RecordHeaderLens(lens) { sheets.open(LensSheet(p)) }
                        if (editable && gameTools && activeModularTab.kind == ProfileTabKind.GAME_TEXT && !speechOverride && !secretOverride) {
                            ImmersiveHeaderButton(IconId.Plus, ModularTabCopy.NEW_VOICE_LINE, { sheets.open(VoiceLineSheet(p, c)) })
                        }
                    },
                    bottomBar = {
                        ModularProfileTabBar(
                            tabs = modularTabs,
                            selectedId = activeModularTab.id,
                            onSelect = { picked ->
                                speechOverride = false
                                secretOverride = false
                                if (picked.kind == ProfileTabKind.SECRET && activeModularTab.kind != picked.kind) {
                                    beforeSecretId = activeModularTab.id
                                }
                                selectedModularId = picked.id
                                val mapped = ProfileTabs.gateTab(picked.kind)
                                vm.updateTabs { SecretTabGate.select(it, mapped, secretLocked) }
                            },
                            onManageTemplates = { sheets.open(ProfileTabTemplatesSheetKey) },
                        )
                    },
                    state = immersive,
                    onMore = if (editable) ({ sheets.open(RecordMenuSheet(p, c)) }) else null,
                ) {
                    RecordTopLine(state)
                    HeaderTable(doc, state, fullState, ui.headerExpanded, actions) { headerExpanded = true }
                    // One body for the tabs: they swap their content in place (row 3).
                    val tabKey = when {
                        secretOverride -> "secret_override"
                        speechOverride -> "speech_override"
                        else -> activeModularTab.id
                    }
                    // No clip: the gallery row runs past the side padding and must not be cut while the tabs swap.
                    AnimatedContent(tabKey, transitionSpec = { motion.swap(clip = false) }, label = "recordTab") { currentKey ->
                        when {
                            currentKey == "speech_override" || activeModularTab.kind == ProfileTabKind.SPEECH -> {
                                Column(Modifier.fillMaxWidth()) {
                                    if (currentKey == "speech_override") {
                                        TextAction(
                                            ModularTabCopy.backTo(activeModularTab.label),
                                            onClick = { speechOverride = false },
                                            color = CivilightTheme.colors.acc,
                                            style = CivilightTheme.type.small,
                                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                                        )
                                    }
                                    TabBody(doc, c, state, fullState, ui.copy(tab = ProfileTab.SPEECH), gate.open, gallery, wide, actions, recorderLink, secret)
                                }
                            }
                            currentKey == "secret_override" || activeModularTab.kind == ProfileTabKind.SECRET -> {
                                if (SecretTabGate.composesBody(ProfileTab.SECRET, tabState, secretLocked)) {
                                    Column(Modifier.fillMaxWidth()) {
                                        if (currentKey == "secret_override") {
                                            TextAction(
                                                ModularTabCopy.backTo(activeModularTab.label),
                                                onClick = {
                                                    // N-43: leaving the 비밀 view locks it again; the next opening asks.
                                                    secretOverride = false
                                                    vm.updateTabs { SecretTabGate.leave(it, ProfileTabs.gateTab(activeModularTab.kind), secretLocked) }
                                                },
                                                color = CivilightTheme.colors.acc,
                                                style = CivilightTheme.type.small,
                                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                                            )
                                        }
                                        TabBody(doc, c, state, fullState, ui.copy(tab = ProfileTab.SECRET), gate.open, gallery, wide, actions, recorderLink, secret)
                                    }
                                }
                            }
                            activeModularTab.kind == ProfileTabKind.OVERVIEW ->
                                TabBody(doc, c, state, fullState, ui.copy(tab = ProfileTab.OVERVIEW), gate.open, gallery, wide, actions, recorderLink, secret)
                            activeModularTab.kind == ProfileTabKind.RELATIONS ->
                                TabBody(doc, c, state, fullState, ui.copy(tab = ProfileTab.RELATIONS), gate.open, gallery, wide, actions, recorderLink, secret)
                            activeModularTab.kind == ProfileTabKind.GAME_TEXT ->
                                TabBody(doc, c, state, fullState, ui.copy(tab = ProfileTab.GAME_TEXT), gate.open, gallery, wide, actions, recorderLink, secret)
                            activeModularTab.kind == ProfileTabKind.APPEARANCES ->
                                AppearancesTraceTab(
                                    doc = doc,
                                    characterKey = c,
                                    hiddenUnlocked = gate.open,
                                    onJumpToBlock = { blockKey -> deps.navigator.push(io.github.lenticualrstone.civilight.navigation.Script(p, blockKey = blockKey)) },
                                )
                            else ->
                                ModularSectionTab(
                                    doc = doc,
                                    tab = activeModularTab,
                                    state = state,
                                    actions = actions,
                                    speechSummary = doc.index.character(c)?.speechSummary,
                                    onEditSpeechSummary = secret.summaryEdit,
                                    onOpenSpeech = {
                                        secretOverride = false
                                        speechOverride = true
                                    },
                                    onOpenSecret = {
                                        speechOverride = false
                                        secretOverride = true
                                        vm.updateTabs { SecretTabGate.select(it, ProfileTab.SECRET, secretLocked) }
                                    },
                                )
                        }
                    }
                }
                // Toasts (e.g. `기기 잠금 필요 · 설정`, N-43) sit above the tab bar.
                Box(
                    Modifier.fillMaxSize().padding(bottom = (bottomInset ?: navBottom) + ImmersiveMath.BAR_ROW_DP.dp + 6.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    ToastHost(message = toast?.text, actionLabel = toast?.actionLabel, onAction = toast?.onAction?.let { { toasts.act() } })
                }
            }
        }
    }
}

/** The top line of the panel: `인물 기록 007 · 최신 1-7` and the `숨김` tag. Name and one-liner are the title and subtitle. */
@Composable
private fun RecordTopLine(state: ProfileState) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(state.header.title, Modifier.weight(1f, fill = false))
        FoldReveal(state.hidden, horizontal = true) { HiddenMark() }
    }
}

/**
 * The lens in the immersive header: the shared [LensChip] on a 44 dp chip pill (glass, or its scrim inside the
 * header glass), the cast tour's `시점 렌즈` hole and the `값이 바뀌는 때` fallback.
 */
@Composable
private fun RecordHeaderLens(lens: LensChipLabel, onLens: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier.heightIn(min = 44.dp).tourTarget(CastTourSteps.LENS).tourTarget(CastTourSteps.TIMED_VALUE, fallback = true),
        contentAlignment = Alignment.Center,
    ) {
        GlassBackground(GlassSurface.CHIP, Modifier.matchParentSize(), shape = shape)
        LensChip(lens.text, lens.state, onLens, Modifier.padding(horizontal = 6.dp), small = true)
    }
}

/** The immersive subtitle: the one-liner, else the first head field values (`서고 관리인 · 28세`). */
internal object ProfileHeadLine {
    const val MAX_FIELDS = 2

    fun subtitle(oneLiner: String?, rows: List<FieldRow>): String? =
        oneLiner?.trim()?.takeIf { it.isNotEmpty() }
            ?: rows.map { it.value.trim() }.filter { it.isNotEmpty() }.take(MAX_FIELDS).joinToString(CastCopy.SEP).takeIf { it.isNotEmpty() }
}

/**
 * The field table (CST-07): `머리` fields, `첫 등장`, `보이스`; `항목 더 보기` on the cover; `예정` footnote. [full] has
 * every header row, so `항목 더 보기` folds the rest in instead of jumping (P2).
 */
@Composable
private fun HeaderTable(doc: ProjectDoc, state: ProfileState, full: ProfileState, expanded: Boolean, actions: RecordActions, onMore: () -> Unit) {
    val colors = CivilightTheme.colors
    val shown = remember(state) { state.header.rows.mapTo(HashSet()) { it.label } }
    // LIB-19 cast tour: the `값이 바뀌는 때` hole (CastTourSteps.timedValueRow).
    val timed = remember(state) { CastTourSteps.timedValueRow(state.header.rows) }
    Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        for (row in full.header.rows) {
            key(row.label) {
                FoldReveal(expanded || row.label in shown) {
                    FieldRowItem(doc, row, actions, if (row.label == timed) Modifier.tourTarget(CastTourSteps.TIMED_VALUE) else Modifier)
                }
            }
        }
        FoldRevealValue(state.header.moreLabel) { label -> TextAction(label, onMore, color = colors.ink2, style = CivilightTheme.type.small) }
        FoldRevealValue(state.header.plannedFootnote) { note -> CivilightText(note, CivilightTheme.type.small, Modifier.padding(top = 14.dp), color = colors.ink3) }
    }
}

@Composable
private fun TabBody(
    doc: ProjectDoc,
    characterKey: Key,
    state: ProfileState,
    full: ProfileState,
    ui: ProfileUi,
    hiddenUnlocked: Boolean,
    gallery: GallerySection,
    inner: Boolean,
    actions: RecordActions,
    recorderLink: String?,
    secret: SecretTabExtras,
) {
    when (ui.tab) {
        ProfileTab.OVERVIEW -> {
            // CST-71: appearance bars and `마지막 등장`, drawn by the tab directly under the field table.
            val bars = remember(doc, characterKey, hiddenUnlocked) { Appearances.of(doc, characterKey, hiddenUnlocked) }
            OverviewTab(doc, state, full, ui.showEmpty, gallery, inner, actions, recorderLink) { AppearanceSection(bars) }
        }
        ProfileTab.RELATIONS -> RelationsTab(state, actions, doc = doc)
        ProfileTab.SPEECH -> {
            val c = doc.index.character(characterKey) ?: return
            val lens = doc.castLens
            val chips = remember(doc, characterKey, hiddenUnlocked) { RelationViews.addressChips(doc, characterKey, lens, hiddenUnlocked) }
            val hidden = RelationViews.hiddenPartnerCount(doc, characterKey, hiddenUnlocked).takeIf { it > 0 }?.let(RelationViews::hiddenRow)
            SpeechTab(c.speechBody.docText, chips, hidden, actions, summary = c.speechSummary, onSummary = secret.summaryEdit)
        }
        ProfileTab.GAME_TEXT -> {
            val progress = VoiceLines.progress(doc, characterKey)
            val archive = ArchiveDocs.rows(doc, characterKey)
            GameTextTab(progress.sectionTitle, progress.fraction, VoiceLines.ORDER_LABEL, VoiceLines.rows(doc, characterKey), ArchiveDocs.sectionTitle(doc, characterKey), archive, actions)
        }
        ProfileTab.SECRET -> {
            val secretView = remember(doc, characterKey) { Profile.secretTab(doc, characterKey, doc.castLens) }
            SecretTab(doc, secretView.rows, secretView.body.docText, actions) {
                if (secret.showLock) SecretLockRow(secret.locked, secret.lockEnabled, secret.onLock)
            }
        }
    }
}

/** What the `말투` and `비밀` tabs add in 0.2.0 (CST-72, CST-73). */
private data class SecretTabExtras(
    val summaryEdit: (() -> Unit)?,
    val locked: Boolean,
    val showLock: Boolean,
    val lockEnabled: Boolean,
    val onLock: (Boolean) -> Unit,
)

@kotlinx.serialization.Serializable
private data object ProfileTabTemplatesSheetKey : io.github.lenticualrstone.civilight.navigation.SheetKey

