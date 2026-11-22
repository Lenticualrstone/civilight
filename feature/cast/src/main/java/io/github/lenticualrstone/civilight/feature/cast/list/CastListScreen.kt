@file:OptIn(ExperimentalSharedTransitionApi::class)

package io.github.lenticualrstone.civilight.feature.cast.list

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
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
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

/** The `viewByScreen` id of the cast list's `목록 | 카드` choice. */
private const val CARD_SCREEN = "cast"

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
    // CST-04: `목록 | 카드` is remembered per screen as a local preference (`AppSettings.viewByScreen`).
    val settings by deps.settings.collectAsState()
    val cards = settings.viewByScreen[CARD_SCREEN] == ViewMode.CARD
    val setCards: (Boolean) -> Unit = { on -> deps.appScope.launch { deps.stores.settings.setViewMode(CARD_SCREEN, if (on) ViewMode.CARD else ViewMode.LIST) } }
    var filtersJson by rememberSaveable { mutableStateOf("") }
    val filters = remember(filtersJson) { FilterStateCodec.decode(filtersJson) }
    val ui = CastListUi(selectedTag, typed.text, searching, cards, filters)
    val state = remember(doc, ui, gate.open, ready.readOnly) { CastListModel.build(doc, ui, gate.open, ready.readOnly) }
    val view = state.view
    val sheets = rememberSheetHost()
    val paneRole = LocalPaneRole.current
    val top = deps.navigator.backStack.lastOrNull()
    val selectedKey = if (paneRole == PaneRole.LIST) (top as? CharacterProfile)?.characterKey else null
    val open: (String) -> Unit = { key -> deps.navigator.openDetail(CharacterProfile(route.projectKey, key)) }

    CenterColumn {
        CivilightScreen(
            title = view.title,
            smallLine = view.projectName,
            back = IconAction(CastUiCopy.BACK) { deps.navigator.pop() },
            lens = lensChipLabel(doc),
            onLens = { sheets.open(LensSheet(route.projectKey)) },
            actions = {
                ViewToggle(cards, setCards)
                if (state.canFilter) {
                    CivilightIconButton(IconId.Filter, CastFilterModel.TITLE, { sheets.open(CastFilterSheet(route.projectKey)) }, tint = if (state.filters.active) CivilightTheme.colors.acc else CivilightTheme.colors.ink2)
                }
                if (!state.readOnly) {
                    CivilightIconButton(IconId.Plus, CastUiCopy.NEW_CHARACTER, { sheets.open(NewCharacterSheet(route.projectKey)) }, tint = CivilightTheme.colors.ink)
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
                        CommandBar(
                            hint = view.searchHint,
                            onClick = { searching = true },
                            onMic = { deps.navigator.push(Recording(projectKey = route.projectKey)) },
                            micLabel = CastUiCopy.MIC,
                        )
                    }
                }
            },
            sheets = sheets,
            localSheets = { key ->
                when (key) {
                    is NewCharacterSheet -> {
                        { NewCharacterSheetContent { name -> vm.create(name) { created -> close(); open(created) } } }
                    }
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
                onAdd = { sheets.open(NewCharacterSheet(route.projectKey)) },
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
) {
    val motion = CivilightTheme.motion
    val unhide = rememberUnhide(revealed)
    SharedTransitionLayout {
        val layout = this
        AnimatedContent(cards, transitionSpec = { CardViewMotion.transform(motion) }, label = "castView") { showCards ->
            val shared = CastShared.of(layout, this, motion)
            CastLazyBody(state, showCards, inner, shared, galleryCounts, hiddenKeys, unhide, onGallery, padding, selectedKey, onTag, onRow, onUnlock, onAdd, onRecorder, onMap, onFilters, imageOf, cardImageOf, onPickImage)
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
) {
    val view = state.view
    val revealedLook: (Key) -> Modifier = { key -> if (key in hiddenKeys) Modifier.unhide(unhide) else Modifier }
    // No horizontal content padding: the card rows run to the screen edge (QA 7), everything else keeps 24 dp.
    val side = Modifier.padding(horizontal = CAST_SIDE)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 16.dp),
    ) {
        if (view.empty == null) {
            item(key = "mode") {
                // `목록 | 관계도` (CST-01; not on `EmptyCast`): `관계도` is the map (CST-33), a separate route for now.
                SegmentedControl(CastUiCopy.VIEW_MODES, 0, { if (it == 1) onMap() }, civilightItem().then(side).padding(bottom = 12.dp))
            }
        }
        item(key = "chips") {
            ChipFlow(civilightItem().then(side)) {
                for (chip in view.chips) FilterChip(chip.label, chip.selected, { onTag(chip.tagKey) })
            }
        }
        if (state.activeFilters.isNotEmpty()) {
            item(key = "active") {
                ChipFlow(civilightItem().then(side).padding(top = 8.dp)) {
                    for (f in state.activeFilters) RemovableChip(f.label, { onFilters(f.without) }, CastUiCopy.BTN_REMOVE)
                }
            }
        }
        view.empty?.let { empty ->
            item(key = "empty") {
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
        if (state.noResults) item(key = "none") { NoResults(CastUiCopy.NO_RESULTS, civilightItem().then(side)) }
        view.lockRow?.let { lock ->
            item(key = "lock") {
                Box(civilightItem().then(side).padding(top = 16.dp)) { LockRow(lock.title, lock.action, onUnlock, subtitle = lock.sub) }
            }
        }
        if (state.fieldHits.isNotEmpty()) {
            item(key = "hits-head") { SectionLabel(CastUiCopy.ADD_FIELD_ROW, civilightItem().then(side).padding(top = 20.dp, bottom = 6.dp)) }
            val hitKeys = CastListModel.hitKeys(state.fieldHits)
            items(state.fieldHits.size, key = { hitKeys[it] }) { i ->
                val hit = state.fieldHits[i]
                SearchHit(hit.location, hit.sentence, listOf(hit.match), onClick = { onRow(hit.characterKey) }, modifier = civilightItem().then(side))
            }
        }
        if (state.hiddenExcluded) {
            item(key = "hidden-excluded") { CivilightText(CastUiCopy.HIDDEN_EXCLUDED, CivilightTheme.type.small, civilightItem().then(side).padding(top = 12.dp), color = CivilightTheme.colors.ink3) }
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
                        modifier = civilightItem().then(side).then(revealedLook(row.key)),
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
