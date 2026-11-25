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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTabs
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.designsystem.theme.rememberLastNonNull
import io.github.lenticualrstone.civilight.feature.cast.gallery.CastGallery
import io.github.lenticualrstone.civilight.feature.cast.gallery.GallerySection
import io.github.lenticualrstone.civilight.navigation.Gallery
import io.github.lenticualrstone.civilight.navigation.GalleryAddSheet
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.field.Profile
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationViews
import io.github.lenticualrstone.civilight.domain.cast.voice.ArchiveDocs
import io.github.lenticualrstone.civilight.domain.cast.voice.VoiceLines
import io.github.lenticualrstone.civilight.domain.story.search.CommandScreen
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.CastViewModel
import io.github.lenticualrstone.civilight.feature.cast.common.RecordFrame
import io.github.lenticualrstone.civilight.feature.cast.common.castLens
import io.github.lenticualrstone.civilight.feature.cast.common.editContext
import io.github.lenticualrstone.civilight.feature.cast.common.recordTopInset
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
import io.github.lenticualrstone.civilight.navigation.ProjectSearch
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
import io.github.lenticualrstone.civilight.designsystem.component.CommandBar as CommandBarView
import io.github.lenticualrstone.civilight.domain.story.search.CommandBar as CommandHints

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
    val modularTabs = remember(appSettings) {
        (appSettings.customProfileTabs.ifEmpty { appSettings.profileTabPreset.defaultTabs() }).filter { it.enabled }
            .ifEmpty { io.github.lenticualrstone.civilight.model.app.ProfileTabPreset.STANDARD.defaultTabs() }
    }
    var selectedModularId by rememberSaveable(c) { mutableStateOf(modularTabs.first().id) }
    val activeModularTab = modularTabs.firstOrNull { it.id == selectedModularId } ?: modularTabs.first()
    var speechOverride by remember(c) { mutableStateOf(false) }
    var secretOverride by remember(c) { mutableStateOf(false) }
    val gate = rememberHiddenGate(p)
    val context = LocalContext.current
    // CST-74: 4 tabs while `게임 제작 도구` is off. CST-73: the 비밀 tab behind the device lock while locked.
    val gameTools = GameTools.on(doc)
    val storedLocked = doc.index.character(c)?.secretTabLocked == true
    // CST-73: the lock turned off by anything but the confirmed `이 탭 잠그기` switch asks for the device lock first
    // (since 0.2.0 the lock is written without an undo step, so undo and redo cannot; this guards any other writer); the tab is treated as locked while that prompt is on screen, and a cancel locks it again.
    var ownUnlock by remember(c) { mutableStateOf(false) }
    var unlockPending by remember(c) { mutableStateOf(false) }
    val secretLocked = storedLocked || unlockPending
    vm.ensureTabs { SecretTabGate.open(route.tab, gameTools, secretLocked) }
    val tabState = vm.tabs.collectAsState().value ?: SecretTabGate.open(route.tab, gameTools, secretLocked)
    LaunchedEffect(gameTools) { vm.updateTabs { SecretTabGate.gameToolsChanged(it, gameTools) } }
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
    var showEmpty by rememberSaveable { mutableStateOf(false) }
    var headerExpanded by rememberSaveable { mutableStateOf(false) }
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
    val list = rememberLazyListState()
    val collapsed by remember { derivedStateOf { list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > 160 } }
    val inDetailPane = LocalPaneRole.current == PaneRole.DETAIL
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
    CenterColumn {
        RecordFrame(
            back = if (inDetailPane) null else IconAction(CastUiCopy.BACK) { deps.navigator.pop() },
            lens = lensChipLabel(doc),
            onLens = { sheets.open(LensSheet(p)) },
            add = if (editable && gameTools && ui.tab == ProfileTab.GAME_TEXT) IconAction(CastUiCopy.NEW_VOICE_LINE) { sheets.open(VoiceLineSheet(p, c)) } else null,
            menu = if (editable) IconAction(CastUiCopy.MENU) { sheets.open(RecordMenuSheet(p, c)) } else null,
            collapsed = collapsed,
            collapsedTitle = state.name,
            collapsedSmallLine = state.header.title.substringBefore(CastCopy.SEP),
            // LIB-19 cast tour: the lens and the `관계` tab are the holes of `시점 렌즈` and `관계` (CastTourSteps);
            // the lens backs up `값이 바뀌는 때` when the header has no field row.
            lensModifier = Modifier.tourTarget(CastTourSteps.LENS).tourTarget(CastTourSteps.TIMED_VALUE, fallback = true),
            tabs = {
                ModularProfileTabBar(
                    tabs = modularTabs,
                    selectedId = activeModularTab.id,
                    onSelect = { picked ->
                        speechOverride = false
                        secretOverride = false
                        selectedModularId = picked.id
                        val mapped = when (picked.kind) {
                            io.github.lenticualrstone.civilight.model.app.ProfileTabKind.OVERVIEW -> ProfileTab.OVERVIEW
                            io.github.lenticualrstone.civilight.model.app.ProfileTabKind.RELATIONS -> ProfileTab.RELATIONS
                            io.github.lenticualrstone.civilight.model.app.ProfileTabKind.SPEECH -> ProfileTab.SPEECH
                            io.github.lenticualrstone.civilight.model.app.ProfileTabKind.GAME_TEXT -> ProfileTab.GAME_TEXT
                            io.github.lenticualrstone.civilight.model.app.ProfileTabKind.SECRET -> ProfileTab.SECRET
                            else -> ProfileTab.OVERVIEW
                        }
                        vm.updateTabs { SecretTabGate.select(it, mapped, secretLocked) }
                    },
                    onManageTemplates = { sheets.open(ProfileTabTemplatesSheetKey) },
                    modifier = Modifier.weight(1f),
                )
            },
            commandBar = {
                CommandBarView(
                    hint = CommandHints.placeholder(CommandScreen.PROFILE),
                    onClick = { deps.navigator.push(ProjectSearch(p)) },
                    // S-8: the recorder opens only from home; no mic on this bar.
                )
            },
            sheets = sheets,
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
                            ProfileTabTemplateSheetContent(
                                settings = appSettings,
                                onSelectPreset = { preset ->
                                    deps.appScope.launch { deps.stores.settings.setProfileTabPreset(preset) }
                                },
                                onUpdateTabs = { next ->
                                    deps.appScope.launch { deps.stores.settings.setCustomProfileTabs(next) }
                                },
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
        ) { padding ->
            val bgArt = remember(doc, c, wide) { CharacterImages.background(doc, c, inner = wide) }
            val cardPortrait = remember(doc, c) { CharacterImages.image(doc, c, ImagePlace.CARD) }
            // (0.5.2) Match night-manuscript-r3 Profile hero: full-bleed FadingHeaderArt (22dp edge blur fade)
            // for background art OR non-holo portrait; only use tilting HoloCard when holo is explicitly enabled.
            val art = bgArt ?: cardPortrait?.takeIf { !it.holo }
            val portrait = if (bgArt == null && cardPortrait?.holo == true) cardPortrait else null
            val artSlotField = if (bgArt != null) FixedField.BACKGROUND else FixedField.PORTRAIT
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
            // The art's top edge: the content starts under the floating buttons, or at the art (no inset).
            val topPad by animateDpAsState(if (art == null) recordTopInset() else 0.dp, motion.move(CivilightMotion.standard), label = "recordTop")
            LazyColumn(
                state = list,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = PROFILE_SIDE, end = PROFILE_SIDE, top = topPad, bottom = padding.calculateBottomPadding() + 24.dp),
            ) {
                item(key = "art") {
                    FoldRevealValue(art) { image ->
                        androidx.compose.foundation.layout.Box(
                            Modifier.then(
                                if (editable) Modifier.pressable(onClick = { actions.onFixed(artSlotField) }, scaleOnPress = false)
                                else Modifier,
                            ),
                        ) { RecordArt(image, inner = wide, sidePadding = PROFILE_SIDE) }
                    }
                }
                item(key = "portrait") {
                    FoldRevealValue(portrait) { image ->
                        androidx.compose.foundation.layout.Box(
                            Modifier.then(
                                if (editable) Modifier.pressable(onClick = { actions.onFixed(FixedField.PORTRAIT) }, scaleOnPress = false)
                                else Modifier,
                            ),
                        ) { PortraitHero(image) }
                    }
                }
                item(key = "head") { RecordHead(state, editable, actions) }
                item(key = "table") { HeaderTable(doc, state, fullState, ui.headerExpanded, actions) { headerExpanded = true } }
                // One item for the tab body: the tabs swap their content in place (row 3).
                item(key = "tab") {
                    // No clip: the gallery row runs past the list's side padding and must not be cut while the tabs swap.
                    val tabKey = when {
                        secretOverride -> "secret_override"
                        speechOverride -> "speech_override"
                        else -> activeModularTab.id
                    }
                    AnimatedContent(tabKey, transitionSpec = { motion.swap(clip = false) }, label = "recordTab") { currentKey ->
                        when {
                            currentKey == "speech_override" || activeModularTab.kind == io.github.lenticualrstone.civilight.model.app.ProfileTabKind.SPEECH -> {
                                androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth()) {
                                    if (currentKey == "speech_override") {
                                        TextAction(
                                            "← ${activeModularTab.label}으로 돌아가기",
                                            onClick = { speechOverride = false },
                                            color = CivilightTheme.colors.acc,
                                            style = CivilightTheme.type.small,
                                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                                        )
                                    }
                                    TabBody(doc, c, state, fullState, ui.copy(tab = ProfileTab.SPEECH), gate.open, gallery, wide, actions, recorderLink, secret)
                                }
                            }
                            currentKey == "secret_override" || activeModularTab.kind == io.github.lenticualrstone.civilight.model.app.ProfileTabKind.SECRET -> {
                                if (SecretTabGate.composesBody(ProfileTab.SECRET, tabState, secretLocked)) {
                                    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth()) {
                                        if (currentKey == "secret_override") {
                                            TextAction(
                                                "← ${activeModularTab.label}으로 돌아가기",
                                                onClick = { secretOverride = false },
                                                color = CivilightTheme.colors.acc,
                                                style = CivilightTheme.type.small,
                                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                                            )
                                        }
                                        TabBody(doc, c, state, fullState, ui.copy(tab = ProfileTab.SECRET), gate.open, gallery, wide, actions, recorderLink, secret)
                                    }
                                }
                            }
                            activeModularTab.kind == io.github.lenticualrstone.civilight.model.app.ProfileTabKind.OVERVIEW -> {
                                TabBody(doc, c, state, fullState, ui.copy(tab = ProfileTab.OVERVIEW), gate.open, gallery, wide, actions, recorderLink, secret)
                            }
                            activeModularTab.kind == io.github.lenticualrstone.civilight.model.app.ProfileTabKind.RELATIONS -> {
                                TabBody(doc, c, state, fullState, ui.copy(tab = ProfileTab.RELATIONS), gate.open, gallery, wide, actions, recorderLink, secret)
                            }
                            activeModularTab.kind == io.github.lenticualrstone.civilight.model.app.ProfileTabKind.GAME_TEXT -> {
                                TabBody(doc, c, state, fullState, ui.copy(tab = ProfileTab.GAME_TEXT), gate.open, gallery, wide, actions, recorderLink, secret)
                            }
                            activeModularTab.kind == io.github.lenticualrstone.civilight.model.app.ProfileTabKind.APPEARANCES -> {
                                AppearancesTraceTab(
                                    doc = doc,
                                    characterKey = c,
                                    hiddenUnlocked = gate.open,
                                    onJumpToBlock = { blockKey ->
                                        deps.navigator.push(io.github.lenticualrstone.civilight.navigation.Script(p, blockKey = blockKey))
                                    },
                                )
                            }
                            else -> {
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
                }
            }
        }
    }
}

/** `인물 기록 007 · 최신 1-7`, the name (40 / 48) and the one-liner; tapping them edits them. */
@Composable
private fun RecordHead(state: ProfileState, editable: Boolean, actions: RecordActions) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(state.header.title, Modifier.weight(1f, fill = false))
            FoldReveal(state.hidden, horizontal = true) { HiddenMark() }
        }
        CivilightText(
            state.name,
            type.display,
            Modifier
                .padding(top = 8.dp)
                .then(if (editable) Modifier.pressable(onClick = { actions.onFixed(FixedField.NAME) }, scaleOnPress = false, onClickLabel = CastUiCopy.NAME) else Modifier),
            color = colors.ink,
        )
        state.oneLiner?.let { line ->
            CivilightText(
                line,
                type.lead,
                Modifier
                    .padding(top = 10.dp)
                    .then(if (editable) Modifier.pressable(onClick = { actions.onFixed(FixedField.ONE_LINER) }, scaleOnPress = false, onClickLabel = CastUiCopy.ONE_LINER) else Modifier),
                color = colors.ink2,
            )
        }
    }
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
        ProfileTab.RELATIONS -> RelationsTab(state, actions)
        ProfileTab.SPEECH -> {
            val c = doc.index.character(characterKey) ?: return
            val lens = doc.castLens
            val chips = remember(doc, hiddenUnlocked) { RelationViews.addressChips(doc, characterKey, lens, hiddenUnlocked) }
            val hidden = RelationViews.hiddenPartnerCount(doc, characterKey, hiddenUnlocked).takeIf { it > 0 }?.let(RelationViews::hiddenRow)
            SpeechTab(c.speechBody.docText, chips, hidden, actions, summary = c.speechSummary, onSummary = secret.summaryEdit)
        }
        ProfileTab.GAME_TEXT -> {
            val progress = VoiceLines.progress(doc, characterKey)
            val archive = ArchiveDocs.rows(doc, characterKey)
            GameTextTab(progress.sectionTitle, progress.fraction, VoiceLines.ORDER_LABEL, VoiceLines.rows(doc, characterKey), ArchiveDocs.sectionTitle(doc, characterKey), archive, actions)
        }
        ProfileTab.SECRET -> {
            val secretView = remember(doc) { Profile.secretTab(doc, characterKey, doc.castLens) }
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

