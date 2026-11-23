@file:OptIn(ExperimentalSharedTransitionApi::class)

package io.github.lenticualrstone.civilight.feature.cast.list

import io.github.lenticualrstone.civilight.designsystem.component.createdHighlight
import io.github.lenticualrstone.civilight.feature.cast.common.castLens
import io.github.lenticualrstone.civilight.feature.cast.common.CastViewModeRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.Saver
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardMode
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardState
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.designsystem.component.IconOnlyButton
import io.github.lenticualrstone.civilight.designsystem.component.ListCreateRow
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.CardBackModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.feature.cast.create.CastCreate
import io.github.lenticualrstone.civilight.feature.cast.create.CastCreateCard
import io.github.lenticualrstone.civilight.feature.cast.create.CreateCardCodec
import io.github.lenticualrstone.civilight.model.app.CardStyle
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.Avatar
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CommandBar
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.NoResults
import io.github.lenticualrstone.civilight.designsystem.component.SearchField
import io.github.lenticualrstone.civilight.designsystem.component.SearchHit
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.RemovableChip
import io.github.lenticualrstone.civilight.designsystem.component.ViewToggle
import io.github.lenticualrstone.civilight.feature.cast.image.CharacterImages
import io.github.lenticualrstone.civilight.feature.cast.image.ImagePlace
import io.github.lenticualrstone.civilight.feature.cast.image.SlotImage
import io.github.lenticualrstone.civilight.feature.cast.image.rememberSlotPainter
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.navigation.ImagePickSheet
import io.github.lenticualrstone.civilight.designsystem.card.CardViewMotion
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.Unhide
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.designsystem.theme.rememberUnhide
import io.github.lenticualrstone.civilight.designsystem.theme.unhide
import io.github.lenticualrstone.civilight.feature.cast.gallery.CastGallery
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.Gallery
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.list.CastRow
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.HiddenMark
import io.github.lenticualrstone.civilight.feature.cast.common.editContext
import io.github.lenticualrstone.civilight.navigation.CastList
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.navigation.RecorderInbox
import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.navigation.RelationMap
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.lens.lensChipLabel
import io.github.lenticualrstone.civilight.ui.nav.LocalPaneRole
import io.github.lenticualrstone.civilight.ui.nav.PaneRole
import io.github.lenticualrstone.civilight.ui.nav.openDetail
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import io.github.lenticualrstone.civilight.model.app.ViewMode
import kotlinx.coroutines.launch

/** Saves an open create card with the screen (kind, mode and typed values). */
private val CreateCardSaver: Saver<CreateCardState?, String> = Saver(
    save = { state -> state?.let { CreateCardCodec.encode(it) } ?: "" },
    restore = { text -> CreateCardCodec.decode(text) },
)

/** 인물 목록 (CST-01, CST-02; boards `Cast`, `EmptyCast`, `TwoCast`, `ColCast`). */
@Composable
fun CastListScreen(route: CastList, deps: FeatureDeps) {
    val vm = entryViewModel { CastListViewModel(route.projectKey, deps.projects, deps::editContext) }
    val load by vm.state.collectAsState()
    val ready = load as? ProjectLoad.Ready ?: return
    val doc = ready.doc
    val gate = rememberHiddenGate(route.projectKey)
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var typed by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    // U-16 (CST-67): the view follows `카드 보기` and the head toggle; a style without a renderer shows the list.
    // The 0.1.0 `목록 | 카드` toggle (CST-04) is superseded and `viewByScreen["cast"]` is no longer read.
    val settings by deps.settings.collectAsState()
    val style = CastCardViews.shown(settings)
    val cards = false
    var filtersJson by rememberSaveable { mutableStateOf("") }
    // U-15 (CST-66): the create card over the list; the new row is highlighted for a moment after `만들기`.
    var creating by rememberSaveable(stateSaver = CreateCardSaver) { mutableStateOf<CreateCardState?>(null) }
    var highlighted by remember { mutableStateOf<Key?>(null) }
    var creatingBusy by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val filters = remember(filtersJson) { FilterStateCodec.decode(filtersJson) }
    val ui = CastListUi(selectedTag, typed.text, searching, cards, filters)
    val state = remember(doc, ui, gate.open, ready.readOnly) { CastListModel.build(doc, ui, gate.open, ready.readOnly) }
    val view = state.view
    val sheets = rememberSheetHost()
    val paneRole = LocalPaneRole.current
    val top = deps.navigator.backStack.lastOrNull()
    val selectedKey = if (paneRole == PaneRole.LIST) (top as? CharacterProfile)?.characterKey else null
    val open: (String) -> Unit = { key -> deps.navigator.openDetail(CharacterProfile(route.projectKey, key)) }
    // U-16 (CST-67): a 3D view draws the narrowed characters; the CST-01 empty state and a typed search (with its
    // field hits and `숨긴 항목 제외`) are drawn by the list.
    val renderer = CastCardViews.renderers[style]?.takeIf { !style.isList }
    val typing = searching && typed.text.isNotBlank()
    val threeD = renderer != null && view.empty == null && !typing

    CenterColumn {
        CivilightScreen(
            title = view.title,
            smallLine = view.projectName,
            back = IconAction(CastUiCopy.BACK) { deps.navigator.pop() },
            lens = lensChipLabel(doc),
            onLens = { sheets.open(LensSheet(route.projectKey)) },
            actions = {
                // The toggle is left out while the list stands in for the view (empty cast, typed search): it would change nothing.
                if (CastCardViews.showsToggle(settings) && view.empty == null && !typing) {
                    CastViewToggle(settings.castCardStyle, style) { mode -> deps.appScope.launch { deps.stores.settings.setCastToggle(mode) } }
                }
                if (state.canFilter) {
                    CivilightIconButton(IconId.Filter, CastUiCopy.FILTER_BUTTON, { sheets.open(CastFilterSheet(route.projectKey)) }, tint = if (state.filters.active) CivilightTheme.colors.acc else CivilightTheme.colors.ink2)
                }
                // CST-66: the head `+` is gone; creating is the `＋ 새 인물` row, or in a 3D view this head button (D31).
                if (threeD && !state.readOnly) {
                    CivilightIconButton(IconButtonSpec.LIST_CREATE, { creating = CastCreate.openCharacter(CreateCardMode.LIST) })
                }
            },
            commandBar = {
                // The search field and the command bar swap in place (P3).
                ContentSwap(searching, label = "castBar") { typing ->
                    if (typing) {
                        SearchBar(typed, { typed = it }, view.searchHint) {
                            searching = false
                            typed = TextFieldValue("")
                        }
                    } else {
                        // S-8: no mic here; the recorder opens only from home.
                        CommandBar(hint = view.searchHint, onClick = { searching = true })
                    }
                }
            },
            sheets = sheets,
            localSheets = { key ->
                when (key) {
                    is CastFilterSheet -> {
                        { CastFilterContent(CastFilterModel.sheet(doc), state.filters) { filtersJson = FilterStateCodec.encode(it) } }
                    }
                    else -> null
                }
            },
        ) { padding ->
            val inner = LocalWindowLayout.current.isInner
            val galleryCounts = remember(doc, gate.open) { CastGallery.counts(doc, doc.cast.characters.filter { it.deletedAt == null }.map { it.key }, gate.open) }
            val hiddenKeys = remember(doc, gate.open) { if (gate.open) doc.cast.characters.filter { it.hidden }.mapTo(HashSet()) { it.key } else emptySet() }
            if (threeD) {
                val shownCards = remember(doc, state) {
                    state.view.rows.distinctBy { it.key }.mapNotNull { r ->
                        doc.index.character(r.key)?.let { io.github.lenticualrstone.civilight.domain.cast.list.CastList.card(doc, it, doc.castLens) }
                    }
                }
                // The stage's input and actions are the same instances while nothing it draws changed (no rebuilt
                // lambdas, no `CardBackModel.of` rerun on a flipped card).
                val stageImageOf = remember(doc) { { key: String -> CharacterImages.image(doc, key, ImagePlace.CARD) } }
                val stageBackOf = remember(doc) { { key: String -> CardBackModel.of(doc, key, doc.castLens) } }
                // The gate's `unlock` is a new lambda each pass; the actions call the latest one.
                val unlockNow by rememberUpdatedState(gate.unlock)
                val openNow by rememberUpdatedState(open)
                val stageActions = remember(state.readOnly) {
                    CastCardViewActions(open = { openNow(it) }, unlock = { unlockNow() }, create = if (state.readOnly) null else ({ creating = CastCreate.openCharacter(CreateCardMode.LIST) }))
                }
                CastCardViewHost(
                    state = state,
                    noCards = shownCards.isEmpty() && state.view.lockRow == null,
                    padding = padding,
                    onTag = { selectedTag = it },
                    onMap = { deps.navigator.push(RelationMap(route.projectKey)) },
                    onFilters = { filtersJson = FilterStateCodec.encode(it) },
                ) {
                    val stageInput = remember(shownCards, state.view.lockRow, state.readOnly, hiddenKeys, stageImageOf, stageBackOf, settings.viewTilt, highlighted) {
                        CastCardViewInput(
                            cards = shownCards,
                            lock = state.view.lockRow,
                            readOnly = state.readOnly,
                            hiddenKeys = hiddenKeys,
                            imageOf = stageImageOf,
                            backOf = stageBackOf,
                            viewTilt = settings.viewTilt,
                            focusKey = highlighted,
                        )
                    }
                    renderer.Draw(stageInput, stageActions)
                }
                return@CivilightScreen
            }
            CastListBody(
                state = state,
                cards = cards,
                inner = inner,
                galleryCounts = galleryCounts,
                hiddenKeys = hiddenKeys,
                revealed = gate.open,
                onGallery = { key -> deps.navigator.push(Gallery.ofCharacter(route.projectKey, key)) },
                padding = padding,
                selectedKey = selectedKey,
                onTag = { selectedTag = it },
                onRow = open,
                onUnlock = gate.unlock,
                onAdd = { creating = CastCreate.openCharacter(CreateCardMode.LIST) },
                showCreate = CastListModel.showsCreate(state),
                highlighted = highlighted,
                listState = listState,
                onRecorder = { deps.navigator.push(RecorderInbox(projectKey = route.projectKey)) },
                onMap = { deps.navigator.push(RelationMap(route.projectKey)) },
                onFilters = { filtersJson = FilterStateCodec.encode(it) },
                imageOf = { key -> CharacterImages.image(doc, key, ImagePlace.AVATAR) },
                cardImageOf = { key -> CharacterImages.image(doc, key, ImagePlace.CARD) },
                onPickImage = if (state.readOnly) null else ({ key ->
                    sheets.open(ImagePickSheet(route.projectKey, ImageOwnerKind.CHARACTER, key, ImageSlot.CHARACTER_PORTRAIT))
                }),
            )
        }
        creating?.takeIf { !state.readOnly }?.let { card ->
            val liveTags = remember(doc) { CharacterEdits.tags(doc) }
            val finish: (Boolean) -> Unit = { openAfter ->
                val made = card
                // The card closes only once the write applied; otherwise the typed values stay.
                creatingBusy = true
                vm.createFromCard(made, onDone = { creatingBusy = false }) { key ->
                    creating = null
                    if (openAfter) {
                        open(key)
                    } else {
                        // Return the chip filter to `전체` when it would hide the new row (§8.3 Q46).
                        val next = CastCreate.afterCreate(ui, CastCreate.tagKeys(made))
                        selectedTag = next.selectedTag
                        highlighted = key
                    }
                }
            }
            CastCreateCard(
                state = card,
                onChange = { creating = it },
                onCancel = { creating = null },
                onCreate = { finish(false) },
                onCreateAndOpen = { finish(true) },
                canCreate = !creatingBusy && CastCreate.canCreate(card),
                tags = liveTags,
            )
        }
    }
    // After `만들기`: scroll just enough to show the new row when it is out of view, then highlight it (DS-72).
    LaunchedEffect(highlighted, state) {
        val key = highlighted ?: return@LaunchedEffect
        val keys = CastListModel.itemKeys(state, CastListModel.showsCreate(state))
        val index = CastListModel.firstRowIndex(keys, state, key)
        if (index < 0) {
            // CST-66: a search or field filter that hides the new row is cleared, then this runs again.
            val next = CastCreate.revealCreated(doc, ui, key, gate.open, ready.readOnly)
            if (next == ui) return@LaunchedEffect
            selectedTag = next.selectedTag
            if (next.query != ui.query) typed = TextFieldValue(next.query)
            searching = next.searching
            if (next.filters != ui.filters) filtersJson = FilterStateCodec.encode(next.filters)
            return@LaunchedEffect
        }
        // A 3D view brings the new card to the front itself (`focusKey`); the list's scroll state is not attached then.
        if (!threeD && listState.layoutInfo.visibleItemsInfo.none { it.index == index }) listState.animateScrollToItem(index)
        delay(CivilightMotion.createdRowHighlightMs.toLong())
        highlighted = null
    }
}

/** The head toggle (DS-39, CST-67): `목록` and the chosen 3D view; drawn only when that view has a renderer. */
@Composable
private fun CastViewToggle(chosen: CardStyle, shown: CardStyle, onSelect: (ViewMode) -> Unit) {
    val colors = CivilightTheme.colors
    IconOnlyButton(IconId.List, CastUiCopy.CARD_STYLES.last(), { onSelect(ViewMode.LIST) }, tint = if (shown.isList) colors.ink else colors.ink3)
    IconOnlyButton(IconId.Grid, CastUiCopy.CARD_STYLES[chosen.ordinal], { onSelect(ViewMode.CARD) }, tint = if (shown.isList) colors.ink3 else colors.ink)
}

/**
 * The frame of a 3D view (CST-67): `목록 | 관계도`, the tag chips and the active field filters stay above the
 * view, as over the list; the view fills the rest. Field filters and the tag chip narrow the cards.
 */
@Composable
private fun CastCardViewHost(
    state: CastListState,
    /** No row became a card and there is no lock card: the stage would draw nothing. */
    noCards: Boolean,
    padding: PaddingValues,
    onTag: (String?) -> Unit,
    onMap: () -> Unit,
    onFilters: (FieldFilterState) -> Unit,
    content: @Composable () -> Unit,
) {
    val side = Modifier.padding(horizontal = CAST_SIDE)
    Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())) {
        CastViewModeRow(0, { if (it == 1) onMap() }, side.padding(bottom = 12.dp))
        ChipFlow(side) {
            for (chip in state.view.chips) FilterChip(chip.label, chip.selected, { onTag(chip.tagKey) })
        }
        if (state.activeFilters.isNotEmpty()) {
            ChipFlow(side.padding(top = 8.dp)) {
                for (f in state.activeFilters) RemovableChip(f.label, { onFilters(f.without) }, CastUiCopy.BTN_REMOVE)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp)) {
            if (noCards || (state.view.rows.isEmpty() && state.view.lockRow == null)) {
                NoResults(CastUiCopy.NO_RESULTS, side.padding(top = 16.dp))
            } else {
                content()
            }
        }
    }
}

@Composable
private fun SearchBar(value: TextFieldValue, onChange: (TextFieldValue) -> Unit, hint: String, onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    SearchField(value, onChange, Modifier.focusRequester(focus), placeholder = hint, onClose = onClose, closeLabel = CastUiCopy.BTN_CLOSE)
    LaunchedEffect(Unit) { focus.requestFocus() }
}

/**
 * The list body. `목록 ↔ 카드` is one switch (CNV-45): each portrait grows in place into its card, list text
 * leaves first and card text follows, and under reduced motion the body crossfades. Every lazy item
 * moves with the shared item motion (P1); hidden characters that just became visible unblur (P5).
 */
@Composable
private fun CastListBody(
    state: CastListState,
    cards: Boolean,
    inner: Boolean,
    galleryCounts: Map<Key, Int>,
    hiddenKeys: Set<Key>,
    revealed: Boolean,
    onGallery: (Key) -> Unit,
    padding: PaddingValues,
    selectedKey: String?,
    onTag: (String?) -> Unit,
    onRow: (String) -> Unit,
    onUnlock: () -> Unit,
    onAdd: () -> Unit,
    onRecorder: () -> Unit,
    onMap: () -> Unit,
    onFilters: (FieldFilterState) -> Unit,
    imageOf: (String) -> SlotImage?,
    cardImageOf: (String) -> SlotImage?,
    onPickImage: ((String) -> Unit)?,
    showCreate: Boolean,
    highlighted: Key?,
    listState: LazyListState,
) {
    val motion = CivilightTheme.motion
    val unhide = rememberUnhide(revealed)
    SharedTransitionLayout {
        val layout = this
        AnimatedContent(cards, transitionSpec = { CardViewMotion.transform(motion) }, label = "castView") { showCards ->
            val shared = CastShared.of(layout, this, motion)
            CastLazyBody(
                state, showCards, inner, shared, galleryCounts, hiddenKeys, unhide, onGallery, padding, selectedKey, onTag, onRow, onUnlock, onAdd,
                onRecorder, onMap, onFilters, imageOf, cardImageOf, onPickImage, showCreate, highlighted, listState,
            )
        }
    }
}

@Composable
private fun CastLazyBody(
    state: CastListState,
    showCards: Boolean,
    inner: Boolean,
    shared: CastShared,
    galleryCounts: Map<Key, Int>,
    hiddenKeys: Set<Key>,
    unhide: Unhide,
    onGallery: (Key) -> Unit,
    padding: PaddingValues,
    selectedKey: String?,
    onTag: (String?) -> Unit,
    onRow: (String) -> Unit,
    onUnlock: () -> Unit,
    onAdd: () -> Unit,
    onRecorder: () -> Unit,
    onMap: () -> Unit,
    onFilters: (FieldFilterState) -> Unit,
    imageOf: (String) -> SlotImage?,
    cardImageOf: (String) -> SlotImage?,
    onPickImage: ((String) -> Unit)?,
    showCreate: Boolean,
    highlighted: Key?,
    listState: LazyListState,
) {
    val view = state.view
    val revealedLook: (Key) -> Modifier = { key -> if (key in hiddenKeys) Modifier.unhide(unhide) else Modifier }
    // No horizontal content padding: the card rows run to the screen edge (QA 7), everything else keeps 24 dp.
    val side = Modifier.padding(horizontal = CAST_SIDE)
    // Item order and keys follow `CastListModel.itemKeys` (used to scroll a new row into view).
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 16.dp),
    ) {
        if (view.empty == null) {
            item(key = CastListModel.MODE_KEY) {
                // `목록 | 관계도` (CST-01; not on `EmptyCast`): `관계도` is the map (CST-33), a separate route for now.
                CastViewModeRow(0, { if (it == 1) onMap() }, civilightItem().then(side).padding(bottom = 12.dp))
            }
        }
        item(key = CastListModel.CHIPS_KEY) {
            ChipFlow(civilightItem().then(side)) {
                for (chip in view.chips) FilterChip(chip.label, chip.selected, { onTag(chip.tagKey) })
            }
        }
        if (state.activeFilters.isNotEmpty()) {
            item(key = CastListModel.ACTIVE_KEY) {
                ChipFlow(civilightItem().then(side).padding(top = 8.dp)) {
                    for (f in state.activeFilters) RemovableChip(f.label, { onFilters(f.without) }, CastUiCopy.BTN_REMOVE)
                }
            }
        }
        // U-15.1 (CST-66): `＋ 새 인물` under the chips, with 0 or many characters and while a query is typed.
        if (showCreate) {
            item(key = CastListModel.CREATE_KEY) { ListCreateRow(CreateKind.CHARACTER, onAdd, civilightItem().then(side).padding(top = 8.dp)) }
        }
        view.empty?.let { empty ->
            item(key = CastListModel.EMPTY_KEY) {
                EmptyBlock(
                    title = empty.title,
                    subtitle = empty.sub,
                    primaryLabel = empty.primary.takeUnless { state.readOnly },
                    onPrimary = onAdd.takeUnless { state.readOnly },
                    secondaryLabel = empty.secondary,
                    onSecondary = onRecorder,
                    modifier = civilightItem().then(side),
                )
            }
        }
        if (state.noResults) item(key = CastListModel.NONE_KEY) { NoResults(CastUiCopy.NO_RESULTS, civilightItem().then(side)) }
        view.lockRow?.let { lock ->
            item(key = CastListModel.LOCK_KEY) {
                Box(civilightItem().then(side).padding(top = 16.dp)) { LockRow(lock.title, lock.action, onUnlock, subtitle = lock.sub) }
            }
        }
        if (state.fieldHits.isNotEmpty()) {
            item(key = CastListModel.HITS_HEAD_KEY) { SectionLabel(CastUiCopy.ADD_FIELD_ROW, civilightItem().then(side).padding(top = 20.dp, bottom = 6.dp)) }
            val hitKeys = CastListModel.hitKeys(state.fieldHits)
            items(state.fieldHits.size, key = { hitKeys[it] }) { i ->
                val hit = state.fieldHits[i]
                SearchHit(hit.location, hit.sentence, listOf(hit.match), onClick = { onRow(hit.characterKey) }, modifier = civilightItem().then(side))
            }
        }
        if (state.hiddenExcluded) {
            item(key = CastListModel.HIDDEN_EXCLUDED_KEY) { CivilightText(CastUiCopy.HIDDEN_EXCLUDED, CivilightTheme.type.small, civilightItem().then(side).padding(top = 12.dp), color = CivilightTheme.colors.ink3) }
        }
        if (showCards) {
            state.cardGroups.forEachIndexed { i, group ->
                item(key = CastListModel.cardsKey(group.tagKey)) {
                    CastCardRow(i, group, inner, cardImageOf, galleryCounts, onRow, onGallery, onPickImage, civilightItem(), shared, revealedLook)
                }
            }
        } else {
            for (group in view.groups) {
                item(key = CastListModel.sectionKey(group.tagKey)) { SectionLabel(group.title, civilightItem().then(side).padding(top = 20.dp, bottom = 6.dp)) }
                items(group.rows, key = { CastListModel.rowKey(group.tagKey, it.key) }) { row ->
                    CastRowView(
                        row,
                        imageOf(row.key),
                        selected = row.key == selectedKey,
                        onClick = { onRow(row.key) },
                        portraitShare = shared.portrait(group.tagKey, row.key, radiusDp = 20f),
                        // DS-72: the row just made (every copy of it, CST-68) is highlighted for a moment, not selected.
                        modifier = civilightItem().then(side).then(revealedLook(row.key)).createdHighlight(row.key == highlighted),
                    )
                }
            }
        }
    }
}

@Composable
private fun CastRowView(row: CastRow, image: SlotImage?, selected: Boolean, onClick: () -> Unit, portraitShare: Modifier, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val portrait = rememberSlotPainter(image)
    ListRow(
        title = row.name,
        modifier = modifier,
        subtitle = row.subtitle.ifEmpty { null },
        leading = { Box(portraitShare) { Avatar(row.name, size = 40.dp, image = portrait) } },
        trailing = {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (row.hiddenTag) HiddenMark()
                row.firstAppearance?.let { CivilightText(it, CivilightTheme.type.small, color = colors.ink3, maxLines = 1) }
            }
        },
        onClick = onClick,
        selected = selected,
    )
}
