@file:OptIn(ExperimentalSharedTransitionApi::class)

package io.github.lenticualrstone.civilight.feature.canvas.gallery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.card.CardViewMotion
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler
import io.github.lenticualrstone.civilight.feature.canvas.image.CropSession
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageCopy
import io.github.lenticualrstone.civilight.feature.canvas.image.crop.CropEditor
import androidx.compose.foundation.layout.navigationBarsPadding
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.component.ViewToggle
import io.github.lenticualrstone.civilight.designsystem.component.ViewToggleMode
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.designsystem.theme.rememberLastNonNull
import io.github.lenticualrstone.civilight.domain.actions.GalleryOwner
import io.github.lenticualrstone.civilight.domain.library.hiding.HideableKind
import io.github.lenticualrstone.civilight.domain.library.hiding.LockedItemsRow
import io.github.lenticualrstone.civilight.feature.canvas.common.canvasEditContext
import io.github.lenticualrstone.civilight.feature.canvas.image.PresetImages
import io.github.lenticualrstone.civilight.feature.canvas.image.SlotImage
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.PaneSplit
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.gallery.GalleryItem
import io.github.lenticualrstone.civilight.designsystem.component.BottomGlassBar
import io.github.lenticualrstone.civilight.designsystem.component.CommandBar
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.navigation.Gallery
import io.github.lenticualrstone.civilight.navigation.GalleryAddSheet
import io.github.lenticualrstone.civilight.navigation.GalleryInfoSheet
import io.github.lenticualrstone.civilight.navigation.GalleryMenuSheet
import io.github.lenticualrstone.civilight.navigation.ProjectSearch
import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHostLayout
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import io.github.lenticualrstone.civilight.ui.window.SplitPanes
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** `직접 순서 ⌄`: the sort modes only (GAL-04), a feature-local sheet. */
@Serializable
data class GallerySortSheet(val projectKey: Key, val ownerKey: Key) : SheetKey

/** What the gallery body shows (one crossfade between them). */
private enum class GalleryBody { LOADING, LOCKED, GONE, EMPTY, ITEMS }

/** The in-screen modes over the gallery (no routes of their own; back leaves them): `보기`, `자르기`, `순서 바꾸기`. */
private sealed interface GalleryMode {
    data object Main : GalleryMode
    data class Viewer(val key: Key) : GalleryMode
    data class Crop(val key: Key) : GalleryMode
    data class Order(val key: Key) : GalleryMode
}

/**
 * 갤러리 (55 GAL-04 … GAL-09, GAL-23 … GAL-25; route `Gallery`): header with the three-cell view toggle and `+`,
 * filter chips, the order line, then `목록` · `격자` · `카드`. Picking is only an explicit tap (or arriving with
 * an item named); the picked item's text shows in the 설명 판 under its row, in the expanded row, or in the inner
 * right pane. `보기` opens the full-screen viewer inside this entry (no route of its own; back closes it).
 *
 * The pick, the panel's `더 보기` and the scroll survive fold and process death (saveable state, written through to
 * `_app/view.json` for "returning restores the last pick"). The route's `pickKey` is applied once per entry.
 */
@Composable
fun GalleryScreen(route: Gallery, deps: FeatureDeps) {
    val vm = entryViewModel(key = "gallery-${route.ownerKey}") { GalleryViewModel(route.projectKey, deps.projects, deps::canvasEditContext, deps.stores.blobs) }
    val load by vm.state.collectAsState()
    val settings by deps.settings.collectAsState()
    val gate = rememberHiddenGate(route.projectKey)
    val locked = !gate.open
    val window = LocalWindowLayout.current
    val inner = window.isInner
    val scope = rememberCoroutineScope()
    val sheets = rememberSheetHost()
    val motion = CivilightTheme.motion
    val ready = load as? ProjectLoad.Ready
    val ownerKind = route.ownerKind

    var filterId by rememberSaveable { mutableStateOf(GalleryFilter.All.id) }
    var sortName by rememberSaveable { mutableStateOf(GallerySort.MANUAL.name) }
    var pickedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var seeded by rememberSaveable { mutableStateOf(false) }
    var routePickUsed by rememberSaveable { mutableStateOf(false) }
    var viewerKey by rememberSaveable { mutableStateOf<String?>(null) }
    var viewerIndex by rememberSaveable { mutableIntStateOf(0) }
    var cropKey by rememberSaveable { mutableStateOf<String?>(null) }
    var orderKey by rememberSaveable { mutableStateOf<String?>(null) }
    /** An item to pick once it is in the view (after an add, GAL-11; `되돌리기` putting the old pick back). */
    var arriveKey by rememberSaveable { mutableStateOf<String?>(null) }
    var multiActive by rememberSaveable { mutableStateOf(false) }
    var multiKeys by rememberSaveable { mutableStateOf(listOf<String>()) }
    var scrollRequest by remember { mutableIntStateOf(0) }
    /** The request that arrival made: it jumps (the rows may not be laid out yet) instead of animating. */
    var arrivalRequest by remember { mutableIntStateOf(-1) }
    val multi = MultiPick(multiActive, multiKeys)
    fun setMulti(next: MultiPick) {
        multiActive = next.active
        multiKeys = next.keys
    }

    val viewKind = GalleryViewChoice.of(settings.viewByScreen, ownerKind)
    val sort = GallerySort.entries.firstOrNull { it.name == sortName } ?: GallerySort.MANUAL
    val view = remember(ready?.doc, locked, filterId, sort) {
        ready?.let { GalleryRows.build(it.doc, ownerKind, route.ownerKey, locked, GalleryFilter.fromId(filterId), sort) }
    }
    val pick = PickState(pickedKey, expanded)
    val layout = GalleryLayout.of(inner, window.twoPanes, viewKind)

    fun setPick(next: PickState) {
        if (next == PickState(pickedKey, expanded)) return
        pickedKey = next.pickedKey
        expanded = next.expanded
        vm.saveState(route.ownerKey) { GalleryPick.save(it, next) }
    }

    // Seed once from `_app` (returning restores the last pick), apply the route's item once, and drop a pick
    // that is no longer visible (filter, relock, delete). Only with a loaded document.
    LaunchedEffect(view) {
        val v = view ?: return@LaunchedEffect
        val doc = ready?.doc ?: return@LaunchedEffect
        if (v.access != GalleryAccess.OPEN) return@LaunchedEffect
        var next = PickState(pickedKey, expanded)
        if (!seeded) {
            seeded = true
            next = PickState.of(doc.app.view.galleryOf(route.ownerKey))
        }
        if (!routePickUsed) {
            routePickUsed = true
            val arrived = GalleryPick.arrive(next, route.pickKey, v.keys)
            // Also when the route item already was the saved pick: the rows still start at 0 (scroll restore is
            // skipped when arriving with an item), so the picked card would sit off screen.
            if (GalleryReveal.scrollsOnArrival(route.pickKey, v.keys)) {
                scrollRequest++
                arrivalRequest = scrollRequest
            }
            next = arrived
        }
        setPick(GalleryPick.reconcile(next, v.keys))
        setMulti(multi.reconcile(v.keys))
        // A filter that hides the chosen tag entirely falls back to what the view shows.
        if (v.filter.id != filterId && v.chips.none { it.filter.id == filterId }) filterId = v.filter.id
    }

    // An item named after an add (or the old pick on `되돌리기`) is picked once the view holds it.
    LaunchedEffect(view, arriveKey) {
        val v = view ?: return@LaunchedEffect
        val key = arriveKey ?: return@LaunchedEffect
        if (key !in v.keys) return@LaunchedEffect
        arriveKey = null
        setPick(GalleryPick.arrive(PickState(pickedKey, expanded), key, v.keys))
        scrollRequest++
    }

    // The project gallery adds to the project's own items (GAL-10).
    val addKey = GalleryAddSheet(route.projectKey, ownerKind, route.ownerKey)
    val canAdd = ready != null && !ready.readOnly && deps.sheets.contentFor(addKey) != null
    val canMenu = deps.sheets.contentFor(GalleryMenuSheet(route.projectKey, "")) != null
    val canEdit = ready != null && !ready.readOnly && deps.sheets.contentFor(GalleryInfoSheet(route.projectKey, "")) != null
    val handlers = GalleryHandlers(
        tap = { key ->
            // An explicit tap wins over a pick still waiting from an add.
            arriveKey = null
            if (multi.active) setMulti(multi.toggle(key)) else setPick(GalleryPick.tap(PickState(pickedKey, expanded), key))
        },
        view = { key -> viewerKey = key },
        toggleMore = { setPick(GalleryPick.toggleExpanded(PickState(pickedKey, expanded))) },
        openScene = { key -> deps.navigator.push(Script(route.projectKey, sceneKey = key)) },
        unlock = gate.unlock,
        openOwner = { key -> deps.navigator.push(Gallery.ofCharacter(route.projectKey, key)) },
        backToPick = { scrollRequest++ },
        menu = if (canMenu && !multi.active) ({ key ->
            setPick(GalleryPick.menu(PickState(pickedKey, expanded), key))
            sheets.open(GalleryMenuSheet(route.projectKey, key))
        }) else null,
        edit = if (canEdit) ({ key, focus -> sheets.open(GalleryInfoSheet(route.projectKey, key, focusDescription = focus)) }) else null,
    )

    val listState = rememberLazyListState()
    // The first card row's scroll is saved and restored through `_app`; grouped rows keep theirs in memory.
    val mainRow = rememberLazyListState()

    val host = GalleryHost(
        view = view,
        openViewer = { key -> viewerKey = key },
        openCrop = { key -> cropKey = key },
        openOrder = { key -> orderKey = key },
        pick = { key -> arriveKey = key },
        currentPick = { pickedKey },
        restorePick = { key -> if (key == null) setPick(PickState()) else arriveKey = key },
    )
    val mode: GalleryMode = when {
        cropKey != null -> GalleryMode.Crop(cropKey!!)
        orderKey != null -> GalleryMode.Order(orderKey!!)
        viewerKey != null -> GalleryMode.Viewer(viewerKey!!)
        else -> GalleryMode.Main
    }

    // The sheet host wraps both the screen and the viewer, so `⋯` and `고치기` open over `보기` too.
    androidx.compose.runtime.CompositionLocalProvider(LocalGalleryHost provides host) {
    SheetHostLayout(sheets, localSheets = { key ->
        if (key is GallerySortSheet) ({ SortSheetContent(sort, { s -> sortName = s.name }) { close() } }) else null
    }) {
    SharedTransitionLayout {
        AnimatedContent(
            targetState = mode,
            contentKey = { it::class },
            transitionSpec = { motion.enter(CivilightMotion.enterLong) togetherWith motion.exit() },
            label = "galleryViewer",
        ) { m ->
            val vk = (m as? GalleryMode.Viewer)?.key
            val visibility = this
            val plan = CardViewMotion.plan(motion.reduced)
            val sharedImage: @Composable (Key) -> Modifier = { key ->
                if (!plan.sharedImages) Modifier
                else Modifier.sharedElement(
                    rememberSharedContentState("gallery-image-$key"),
                    animatedVisibilityScope = visibility,
                    boundsTransform = BoundsTransform { _, _ -> CardViewMotion.growSpec(motion) },
                )
            }
            val v = view
            val doc = ready?.doc
            if (m is GalleryMode.Crop && doc != null) {
                GalleryCropMode(doc, m.key, deps, vm, locked) { cropKey = null }
            } else if (m is GalleryMode.Order && doc != null) {
                val item = doc.index.galleryItem(m.key)
                if (item == null) {
                    LaunchedEffect(Unit) { orderKey = null }
                } else {
                    val owner = GalleryOwner.of(item)
                    GalleryOrderScreen(
                        doc = doc,
                        ownerKey = owner.key,
                        ownerName = GalleryRows.ownerName(doc, owner.kind, owner.key),
                        locked = locked,
                        startKey = m.key,
                        readOnly = ready.readOnly,
                        onMove = { key, move -> vm.move(key, move, locked) },
                        onMoveAfter = { key, after -> vm.moveAfter(key, after, locked) },
                        onReadOnly = { deps.toasts.show(GalleryUiCopy.READ_ONLY, actionLabel = null) },
                        onClose = { last ->
                            orderKey = null
                            // The last picked row comes back picked (in `전체` · `직접 순서` it is visible).
                            last?.let { arriveKey = it }
                        },
                    )
                }
            } else if (vk != null && v != null && doc != null) {
                val keys = v.keys
                val at = keys.indexOf(vk)
                if (at >= 0) SideEffect { viewerIndex = at }
                // The shown item left the list (deleted, hidden by a relock): the nearest one, or close.
                val current = ViewerNav.settle(keys, vk, if (at >= 0) at else viewerIndex)
                if (current != vk) LaunchedEffect(current) { viewerKey = current }
                if (current == null) {
                    Box(Modifier.fillMaxSize())
                } else {
                    val nav = ViewerNav(keys, current)
                    val entry = v.entry(current)!!
                    val panels = remember(doc, locked) { GalleryPanels(doc, locked) }
                    val shown = remember(entry, v.count) {
                        ViewerItem(entry.item, panels.panel(entry.item, entry.position, v.count, entry.hiddenTag), facts(doc, entry.item, deps))
                    }
                    GalleryViewer(
                        nav = nav,
                        shown = shown,
                        ownerName = GalleryRows.ownerName(doc, GalleryOwner.of(entry.item).kind, entry.item.ownerKey),
                        textOn = settings.galleryTextOn,
                        inner = inner,
                        sharedImage = sharedImage,
                        onMove = { viewerKey = it },
                        onText = { on -> scope.launch { deps.stores.settings.setGalleryTextOn(on) } },
                        onClose = {
                            // Closing returns with the last viewed item picked (GAL-09).
                            setPick(GalleryPick.arrive(PickState(pickedKey, expanded), current, keys))
                            viewerKey = null
                            scrollRequest++
                        },
                        onEdit = handlers.edit?.let { e -> { k: Key -> e(k, false) } },
                        onMenu = handlers.menu,
                        openScene = handlers.openScene,
                    )
                }
            } else {
                GalleryFrame(
                    route = route,
                    deps = deps,
                    vm = vm,
                    view = view,
                    ready = ready,
                    viewKind = viewKind,
                    layout = layout,
                    pick = pick,
                    locked = locked,
                    sort = sort,
                    handlers = handlers,
                    sharedImage = sharedImage,
                    listState = listState,
                    mainRow = mainRow,
                    scrollRequest = scrollRequest,
                    scrollInstant = scrollRequest == arrivalRequest,
                    sheets = sheets,
                    canAdd = canAdd,
                    onAdd = { sheets.open(addKey) },
                    onFilter = { f ->
                        filterId = f.id
                        arriveKey = null
                    },
                    onView = { mode -> scope.launch { deps.stores.settings.setViewMode(GalleryViewChoice.screenOf(ownerKind), GalleryViewChoice.modeOf(mode)) } },
                    onRelock = { deps.unlock.relockNow() },
                    multi = multi,
                    onMulti = ::setMulti,
                )
            }
        }
    }
    }
    }

    // Persist the scroll of the main row or list when it settles (never per frame), after the seed.
    LaunchedEffect(viewKind, seeded) {
        if (!seeded) return@LaunchedEffect
        val state = if (viewKind == GalleryViewKind.CARD) mainRow else listState
        snapshotFlow { state.isScrollInProgress }.drop(1).filter { !it }.collect {
            vm.saveState(route.ownerKey) { it.copy(scrollIndex = state.firstVisibleItemIndex, scrollOffset = state.firstVisibleItemScrollOffset) }
        }
    }
    // Restore the saved scroll once, when the first view arrives.
    var scrollRestored by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(view != null) {
        val doc = ready?.doc ?: return@LaunchedEffect
        if (scrollRestored) return@LaunchedEffect
        scrollRestored = true
        if (route.pickKey != null) return@LaunchedEffect
        val saved = doc.app.view.galleryOf(route.ownerKey)
        val state = if (viewKind == GalleryViewKind.CARD) mainRow else listState
        if (saved.scrollIndex > 0 || saved.scrollOffset > 0) state.scrollToItem(saved.scrollIndex, saved.scrollOffset)
    }
}

/** The screen around the bodies: header, chips, order line, unlocked bar, and the empty / locked states. */
@Composable
private fun GalleryFrame(
    route: Gallery,
    deps: FeatureDeps,
    vm: GalleryViewModel,
    view: GalleryView?,
    ready: ProjectLoad.Ready?,
    viewKind: GalleryViewKind,
    layout: GalleryLayout,
    pick: PickState,
    locked: Boolean,
    sort: GallerySort,
    handlers: GalleryHandlers,
    sharedImage: @Composable (Key) -> Modifier,
    listState: LazyListState,
    mainRow: LazyListState,
    scrollRequest: Int,
    scrollInstant: Boolean,
    sheets: io.github.lenticualrstone.civilight.ui.scaffold.SheetHost,
    canAdd: Boolean,
    onAdd: () -> Unit,
    onFilter: (GalleryFilter) -> Unit,
    onView: (GalleryViewKind) -> Unit,
    onRelock: () -> Unit,
    multi: MultiPick,
    onMulti: (MultiPick) -> Unit,
) {
    val open = view?.access == GalleryAccess.OPEN
    val editable = ready != null && !ready.readOnly
    val batch = GalleryBatch.rememberActions(route.projectKey)
    LocalBackHandler.current(multi.active) { onMulti(MultiPick.OFF) }
    CivilightScreen(
        title = GalleryUiCopy.TITLE,
        smallLine = view?.takeIf { open }?.ownerName,
        back = io.github.lenticualrstone.civilight.designsystem.component.IconAction(GalleryUiCopy.BACK) { deps.navigator.pop() },
        actions = {
            if (open) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    ViewToggle(
                        mode = when (viewKind) {
                            GalleryViewKind.LIST -> ViewToggleMode.LIST
                            GalleryViewKind.GRID -> ViewToggleMode.GRID
                            GalleryViewKind.CARD -> ViewToggleMode.CARD
                        },
                        onChange = { m ->
                            onView(
                                when (m) {
                                    ViewToggleMode.LIST -> GalleryViewKind.LIST
                                    ViewToggleMode.GRID -> GalleryViewKind.GRID
                                    ViewToggleMode.CARD -> GalleryViewKind.CARD
                                },
                            )
                        },
                    )
                    if (canAdd) CivilightIconButton(IconId.Plus, GalleryUiCopy.ADD, onAdd)
                }
            }
        },
        bottomBar = {
            // GAL-04 search: the command bar (`갤러리에서 찾기 · 제목 · 설명 · 태그`) opens the project search over
            // this gallery, which then searches only gallery images. `여러 장 고르기` replaces it with its bar.
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                FoldReveal((view == null || open) && !multi.active, label = "galleryFind") {
                    BottomGlassBar {
                        CommandBar(
                            hint = if (route.ownerKind == GalleryOwnerKind.PROJECT) GalleryUiCopy.FIND_ALL else GalleryUiCopy.FIND,
                            onClick = { deps.navigator.push(ProjectSearch(route.projectKey)) },
                            onMic = { deps.navigator.push(Recording(projectKey = route.projectKey)) },
                        )
                    }
                }
                FoldRevealValue(multi.takeIf { it.active && open }, label = "multiBar") { mp ->
                    MultiBar(
                        multi = mp,
                        editable = editable,
                        hiddenOnly = view?.let { v -> mp.keys.isNotEmpty() && mp.keys.all { k -> v.entry(k)?.item?.hidden == true } } == true,
                        onClose = { onMulti(MultiPick.OFF) },
                        onTags = { sheets.open(GalleryTagsSheet(route.projectKey, mp.keys)) },
                        onHide = { hidden -> if (hidden) batch.unhide(mp.keys) else batch.hide(mp.keys) },
                        onDelete = {
                            batch.delete(mp.keys)
                            onMulti(MultiPick.OFF)
                        },
                        onCopy = { sheets.open(GalleryCopySheet(route.projectKey, mp.keys)) },
                    )
                }
            }
        },
    ) { padding ->
        val body = when {
            view == null -> GalleryBody.LOADING
            view.access == GalleryAccess.OWNER_LOCKED -> GalleryBody.LOCKED
            view.access == GalleryAccess.OWNER_GONE -> GalleryBody.GONE
            view.isEmpty -> GalleryBody.EMPTY
            else -> GalleryBody.ITEMS
        }
        val shown = rememberLastNonNull(view)
        ContentSwap(body, Modifier.fillMaxSize(), label = "galleryBody") { b ->
            when (b) {
                GalleryBody.LOADING -> Box(Modifier.fillMaxSize())
                GalleryBody.LOCKED -> Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                    val row = LockedItemsRow(HideableKind.CHARACTER, 1)
                    LockRow(title = row.title, subtitle = row.sub, actionLabel = row.action, onAction = handlers.unlock)
                }
                GalleryBody.GONE -> Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                    EmptyBlock(title = GalleryUiCopy.GONE)
                }
                GalleryBody.EMPTY -> shown?.let { v -> ready?.let { EmptyGallery(deps, vm, v, it, locked, canAdd, onAdd, padding) } }
                GalleryBody.ITEMS -> shown?.let { v ->
                    val doc = ready?.doc ?: return@let
                    ItemsBody(route, doc, v, viewKind, layout, pick, locked, handlers, sharedImage, listState, mainRow, scrollRequest, scrollInstant, padding, sheets, onFilter, onRelock, multi, if (editable && handlers.menu != null || multi.active) onMulti else null)
                }
            }
        }
    }
}

@Composable
private fun ItemsBody(
    route: Gallery,
    doc: ProjectDoc,
    v: GalleryView,
    viewKind: GalleryViewKind,
    layout: GalleryLayout,
    pick: PickState,
    locked: Boolean,
    handlers: GalleryHandlers,
    sharedImage: @Composable (Key) -> Modifier,
    listState: LazyListState,
    mainRow: LazyListState,
    scrollRequest: Int,
    scrollInstant: Boolean,
    padding: PaddingValues,
    sheets: io.github.lenticualrstone.civilight.ui.scaffold.SheetHost,
    onFilter: (GalleryFilter) -> Unit,
    onRelock: () -> Unit,
    multi: MultiPick,
    onMulti: ((MultiPick) -> Unit)?,
) {
    val motion = CivilightTheme.motion
    val panels = remember(doc, locked) { GalleryPanels(doc, locked) }
    val panel = remember(v, pick.pickedKey, panels) { panels.panel(v, pick.pickedKey) }
    val rowStates = v.sections.mapIndexed { i, section -> section.id to if (i == 0) mainRow else key(section.id) { rememberLazyListState() } }.toMap()
    val state = GalleryBodyState(
        view = v, viewKind = viewKind, layout = layout, pick = pick, panel = panel, inner = layout != GalleryLayout.COVER, handlers = handlers,
        sharedImage = sharedImage,
        captionOf = { e -> e.item.caption?.let(panels::spans)?.takeIf { spans -> spans.any { it.text.isNotBlank() } } },
        multi = multi,
    )
    // `고른 카드로`, arriving with an item and closing the viewer bring the picked item into view (GAL-01, GAL-08).
    // Arriving jumps: the rows may not be laid out yet, and an animated scroll needs a laid-out row.
    LaunchedEffect(scrollRequest) {
        if (scrollRequest == 0) return@LaunchedEffect
        val key = pick.pickedKey ?: return@LaunchedEffect
        val section = v.sections.firstOrNull { s -> s.entries.any { it.key == key } } ?: return@LaunchedEffect
        val jump = motion.reduced || scrollInstant
        when (viewKind) {
            GalleryViewKind.CARD -> {
                val row = rowStates[section.id] ?: return@LaunchedEffect
                val index = section.entries.indexOfFirst { it.key == key }
                if (jump || row.layoutInfo.visibleItemsInfo.isEmpty()) row.scrollToItem(index) else row.animateScrollToItem(index)
            }
            GalleryViewKind.GRID, GalleryViewKind.LIST -> {
                // Chips, order line and the bar come first; the row's index is found by its key.
                val at = GalleryColumn.indexOfItem(key, v, viewKind, layout) ?: return@LaunchedEffect
                if (listState.layoutInfo.visibleItemsInfo.none { it.index == at }) {
                    if (jump || listState.layoutInfo.visibleItemsInfo.isEmpty()) listState.scrollToItem(at) else listState.animateScrollToItem(at)
                }
            }
        }
    }
    // A picked row (`목록`) or the panel under a picked tile line (`격자`) opens in place; when it would end below
    // the bars, the column follows it up while it unfolds (never past the row's own top). In inner list + detail the
    // picked row (or tile line) itself is lifted out from under the bottom bar the same way. A drag stops it.
    val currentView by rememberUpdatedState(v)
    LaunchedEffect(pick.pickedKey, viewKind, layout) {
        val key = pick.pickedKey ?: return@LaunchedEffect
        if (!GalleryReveal.applies(viewKind, layout)) return@LaunchedEffect
        val withPanelLine = GalleryPlacement.panel(viewKind, layout) == PanelPlace.UNDER_ROW
        coroutineScope {
            val follow = launch {
                snapshotFlow {
                    val info = listState.layoutInfo
                    if (listState.isScrollInProgress) return@snapshotFlow null
                    val at = GalleryColumn.indexOfItem(key, currentView, viewKind, layout) ?: return@snapshotFlow null
                    val first = info.visibleItemsInfo.firstOrNull { it.index == at } ?: return@snapshotFlow null
                    val end = info.viewportEndOffset - info.afterContentPadding
                    if (!withPanelLine) return@snapshotFlow RevealSpan(first.offset, first.offset + first.size, end)
                    val next = info.visibleItemsInfo.firstOrNull { it.index == at + 1 }
                    when {
                        // The panel line is laid out: the tile line and the panel together.
                        (next?.key as? String)?.endsWith(GRID_PANEL_KEY_SUFFIX) == true -> RevealSpan(first.offset, next.offset + next.size, end)
                        // Still the layout from before the pick (another line follows): wait for the next one.
                        next != null -> null
                        // The panel line lies past the end: bring the tile line's end in, then measure the panel.
                        else -> RevealSpan(first.offset, maxOf(first.offset + first.size, end) + 1, end)
                    }
                }.filterNotNull().distinctUntilChanged().collect { span ->
                    val d = GalleryReveal.delta(span.top, span.bottom, 0, span.end)
                    if (d > 0) listState.scrollBy(d.toFloat())
                }
            }
            listState.interactionSource.interactions.first { it is DragInteraction.Start }
            follow.cancel()
        }
    }
    // The bottom bar folds away (multi-pick, inner list + detail without it): then the gesture bar still needs room.
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val clear = PaddingValues(top = padding.calculateTopPadding(), bottom = maxOf(padding.calculateBottomPadding(), navBottom))
    val column = @Composable { withPanel: GalleryBodyState, modifier: Modifier ->
        LazyColumn(
            modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = clear.calculateTopPadding() + 8.dp, bottom = clear.calculateBottomPadding() + 24.dp),
        ) {
            item(key = "chips") { ChipsRow(v, withPanel.side, onFilter, civilightItem()) }
            item(key = "orderLine") {
                OrderLine(withPanel, sort = v.sort, sheets = sheets, route = route, rowState = rowStates[v.sections.first().id], onMulti = onMulti?.takeIf { !multi.active }?.let { set -> { set(MultiPick.start()) } }, modifier = civilightItem())
            }
            v.unlockedBar?.let { bar -> item(key = "unlocked") { UnlockedBar(bar, withPanel.side, onRelock, civilightItem()) } }
            if (v.filteredEmpty) {
                item(key = "filteredEmpty") { FilteredEmpty(withPanel.side) { onFilter(GalleryFilter.All) } }
            } else {
                when (viewKind) {
                    GalleryViewKind.CARD -> cardRows(withPanel, { id -> rowStates.getValue(id) }) { _, _ -> handlers.backToPick() }
                    GalleryViewKind.GRID -> gridRows(withPanel)
                    GalleryViewKind.LIST -> listRows(withPanel)
                }
            }
        }
    }
    when (layout) {
        GalleryLayout.INNER_SPLIT -> SplitPanes(
            module = PaneSplit.Modules.GALLERY,
            // One shared image per key: the right pane's image is the one that grows into `보기`.
            left = { column(GalleryBodyState(v, viewKind, layout, pick, panel, state.inner, handlers, { Modifier }, state.captionOf, multi), Modifier) },
            right = { GalleryDetailPane(state, clear) },
        )
        GalleryLayout.INNER_COLUMN -> CenterColumn { column(state, Modifier) }
        else -> column(state, Modifier)
    }
}

/** The column key of the grid's panel line ends so (`<section>-panel`, see [gridRows]). */
private const val GRID_PANEL_KEY_SUFFIX = "-panel"

/** A picked item's span in the column and the end of the clear area (item offsets: 0 = under the top bar). */
private data class RevealSpan(val top: Int, val bottom: Int, val end: Int)

/** `전체 8` · `삽화 4` …: one choice, scrolls when long. */
@Composable
private fun ChipsRow(v: GalleryView, side: androidx.compose.ui.unit.Dp, onFilter: (GalleryFilter) -> Unit, modifier: Modifier) {
    LazyRow(modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = side), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(v.chips, key = { it.filter.id }) { chip ->
            FilterChip(chip.text, selected = chip.filter == v.filter, onClick = { onFilter(chip.filter) }, modifier = civilightItem())
        }
    }
}

/** `직접 순서 ⌄` (sort sheet) · `여러 장 고르기` · right slot: pager, readout, count or the hint (GAL-04). */
@Composable
private fun OrderLine(
    s: GalleryBodyState,
    sort: GallerySort,
    sheets: io.github.lenticualrstone.civilight.ui.scaffold.SheetHost,
    route: Gallery,
    rowState: LazyListState?,
    onMulti: (() -> Unit)?,
    modifier: Modifier,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(modifier.fillMaxWidth().padding(horizontal = s.side).padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .heightIn(min = 44.dp)
                .semantics { contentDescription = GalleryUiCopy.SORT_A11Y }
                .pressable(PressAction.Click({ sheets.open(GallerySortSheet(route.projectKey, route.ownerKey)) }), role = Role.Button),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CivilightText(sort.label, type.label, color = colors.ink2, maxLines = 1)
            CivilightIcon(IconId.Next, null, Modifier.rotate(90f), size = 14.dp, tint = colors.ink3)
        }
        // `여러 장 고르기` (GAL-18), in every view; shown only where the menu actions exist.
        FoldReveal(onMulti != null, horizontal = true, label = "multiEntry") {
            TextAction(GalleryUiCopy.BTN_SELECT_MANY, { onMulti?.invoke() }, Modifier.padding(start = 12.dp))
        }
        Box(Modifier.weight(1f))
        when (val slot = GalleryPlacement.orderSlot(s.view, s.viewKind, s.hint)) {
            is OrderSlot.Pager, is OrderSlot.Readout -> rowState?.let { RowPosition(s.view.count, it) }
            is OrderSlot.Count -> CivilightText(slot.text, type.number, color = colors.ink3, maxLines = 1)
            is OrderSlot.Hint -> CivilightText(slot.text, type.small, color = colors.ink3, maxLines = 1)
            OrderSlot.None -> Unit
        }
    }
}

/** `숨긴 이미지 2 · 열림 · 앱을 떠나면 잠김` with `다시 잠그기` (GAL-19, board `GalUnlocked`). */
@Composable
private fun UnlockedBar(text: String, side: androidx.compose.ui.unit.Dp, onRelock: () -> Unit, modifier: Modifier) {
    val colors = CivilightTheme.colors
    Row(
        modifier.fillMaxWidth().padding(horizontal = side, vertical = 6.dp).clip(RoundedCornerShape(12.dp)).background(colors.tint).padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivilightIcon(IconId.Unlock, null, size = 16.dp, tint = colors.ink2)
        CivilightText(text, CivilightTheme.type.small, Modifier.weight(1f).padding(start = 8.dp), color = colors.ink2, maxLines = 2)
        TextAction(GalleryUiCopy.BTN_RELOCK, onRelock, color = colors.acc)
    }
}

/** `이 태그의 이미지 없음` with `전체 보기` (GAL-25 [PROPOSED]). */
@Composable
private fun FilteredEmpty(side: androidx.compose.ui.unit.Dp, onAll: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = side, vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallLine(GalleryUiCopy.TAG_EMPTY)
        TextAction(GalleryUiCopy.BTN_SHOW_ALL, onAll, color = CivilightTheme.colors.acc)
    }
}

/** The sort modes (radio), applied at once. */
@Composable
private fun SortSheetContent(current: GallerySort, onSort: (GallerySort) -> Unit, close: () -> Unit) {
    SheetHeader(title = GalleryUiCopy.SORT_A11Y, onClose = close, closeLabel = GalleryUiCopy.BTN_CLOSE)
    GallerySort.entries.forEach { s ->
        RadioRow(title = s.label, selected = s == current, onClick = {
            onSort(s)
            close()
        })
    }
}

/**
 * The empty gallery (GAL-25): `갤러리 · 0`, one outlined card with `+` and `이미지 넣기`, the small line, and the
 * offer to add the current portrait (or cover) as an item.
 */
@Composable
private fun EmptyGallery(
    deps: FeatureDeps,
    vm: GalleryViewModel,
    v: GalleryView,
    ready: ProjectLoad.Ready,
    locked: Boolean,
    canAdd: Boolean,
    onAdd: () -> Unit,
    padding: PaddingValues,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val inner = LocalWindowLayout.current.isInner
    val side = if (inner) 32.dp else 20.dp
    val size = io.github.lenticualrstone.civilight.ui.gallery.GalleryCardRules.size(false)
    val shape = RoundedCornerShape(20.dp)
    val main = remember(ready.doc, v.ownerKind, v.ownerKey) { mainImage(ready.doc, v.ownerKind, v.ownerKey) }
    Column(
        Modifier.fillMaxSize().padding(top = padding.calculateTopPadding() + 12.dp, bottom = padding.calculateBottomPadding() + 24.dp).padding(horizontal = side),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CivilightText(GalleryUiCopy.countLine(GalleryUiCopy.TITLE, 0), type.sec, Modifier.fillMaxWidth(), color = colors.ink3)
        val cardModifier = Modifier
            .size(size.width.dp, size.height.dp)
            .insetOutline(colors.edge, 1.5.dp, shape)
            .then(if (canAdd) Modifier.pressable(PressAction.Click(onAdd), role = Role.Button, shape = shape) else Modifier)
        Column(cardModifier, verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
            CivilightIcon(IconId.Plus, null, size = 28.dp, tint = colors.ink2)
            CivilightText(GalleryUiCopy.ADD, type.label, color = colors.ink2, maxLines = 1)
        }
        SmallLine(GalleryUiCopy.EMPTY_LINE)
        FoldRevealValue(main?.takeIf { !ready.readOnly }, label = "mainOffer") { use ->
            val project = v.ownerKind == GalleryOwnerKind.PROJECT
            val sourceName = use.presetId?.let { PresetImages.byId(it)?.name } ?: GalleryUiCopy.PHOTO
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.raise).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))) {
                    SlotImage(use, Ratio.R1X1, v.ownerName, onPick = null, compactPlaceholder = true)
                }
                Column(Modifier.weight(1f)) {
                    CivilightText(if (project) GalleryUiCopy.OFFER_COVER else GalleryUiCopy.OFFER_PORTRAIT, type.label, color = colors.ink, maxLines = 1)
                    SmallLine(GalleryUiCopy.offerSub(if (project) GalleryUiCopy.NOW_COVER else GalleryUiCopy.NOW_PORTRAIT, sourceName))
                }
                GalleryPrimaryButton(GalleryUiCopy.BTN_ADD, {
                    val owner = if (project) GalleryOwner.project(ready.doc) else GalleryOwner.character(v.ownerKey)
                    vm.addSource(owner, use.source, locked, deps.settings.value.holoDefaultForNewImages) { r, _, toast ->
                        when {
                            r == EditResult.READ_ONLY -> deps.toasts.show(GalleryUiCopy.READ_ONLY, actionLabel = null)
                            toast != null -> deps.toasts.show(toast.message, actionLabel = toast.action) { vm.undoLast() }
                        }
                    }
                })
            }
        }
    }
}

/** The owner's current main image (`초상` for a character, `표지` for the project), if any. */
private fun mainImage(doc: ProjectDoc, kind: GalleryOwnerKind, key: Key): ImageUse? {
    val slot = if (kind == GalleryOwnerKind.PROJECT) ImageSlot.PROJECT_COVER else ImageSlot.CHARACTER_PORTRAIT
    return doc.assets.imageUses.firstOrNull { it.slot.known == slot && (kind == GalleryOwnerKind.PROJECT || it.ownerKey == key) && it.source.isNotEmpty() }
}

/** The viewer's facts column (inner, GAL-09): `원본 <W> × <H> · <size>` or `기본 이미지`, then `넣은 날 <MM-DD>`. */
private fun facts(doc: ProjectDoc, item: GalleryItem, deps: FeatureDeps): List<String> = buildList {
    val blob = item.blobHash?.let { h -> doc.assets.blobs.firstOrNull { it.hash == h } }
    when {
        item.isPreset -> add(GalleryUiCopy.PRESET)
        blob != null -> {
            val w = blob.width
            val h = blob.height
            add(if (w != null && h != null) GalleryUiCopy.originalLine(w, h, GalleryFacts.size(blob.bytes)) else GalleryFacts.size(blob.bytes))
        }
    }
    if (item.createdAt > 0L) add(GalleryUiCopy.addedOn(GalleryFacts.monthDay(item.createdAt, deps.clock.zone)))
}

/**
 * `자르기` of a gallery item (GAL-12, board `GalCrop`): the CNV-28 editor with the chips `갤러리 카드 3:4` · `목록 1:1`,
 * header `<owner> · 갤러리`, holo row. Crops change only card faces, tiles and list thumbnails; `보기` keeps the whole
 * original. `적용` writes the edited ratios and holo as one undo step; `취소` and back write nothing.
 */
@Composable
private fun GalleryCropMode(doc: ProjectDoc, itemKey: Key, deps: FeatureDeps, vm: GalleryViewModel, locked: Boolean, onDone: () -> Unit) {
    val item = doc.index.galleryItem(itemKey)
    val hash = item?.blobHash
    if (item == null || hash == null || !doc.liveness.isLive(item)) {
        LaunchedEffect(Unit) { onDone() }
        return
    }
    val owner = GalleryOwner.of(item)
    CropEditor(
        doc = doc,
        blobHash = hash,
        stageSlot = CropSession.GALLERY_STAND_IN,
        header = GalleryUiCopy.ownerGallery(GalleryRows.ownerName(doc, owner.kind, owner.key)),
        handOff = null,
        note = null,
        deps = deps,
        start = { _, size -> CropSession.startGallery(size, item) },
        onCancel = onDone,
        onApply = { session ->
            vm.applyCrop(item.key, session, ImageCopy.BTN_APPLY) { r ->
                if (r == EditResult.READ_ONLY) deps.toasts.show(GalleryUiCopy.READ_ONLY, actionLabel = null) else onDone()
            }
        },
        handleBack = true,
    )
}

/**
 * The `여러 장 고르기` bar (GAL-18): `<k>장 고름` with `닫기`, then `태그` · `숨기기` · `지우기` · `다른 곳에도 넣기`.
 * Each action is one undo step; leaving the mode never changes the picked item.
 */
@Composable
private fun MultiBar(
    multi: MultiPick,
    editable: Boolean,
    hiddenOnly: Boolean,
    onClose: () -> Unit,
    onTags: () -> Unit,
    onHide: (alreadyHidden: Boolean) -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val some = editable && multi.count > 0
    Column(
        Modifier.fillMaxWidth().background(colors.raise).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            io.github.lenticualrstone.civilight.designsystem.theme.ValueSwap(multi.title, CivilightTheme.type.label, Modifier.weight(1f), color = colors.ink)
            TextAction(GalleryUiCopy.BTN_CLOSE, onClose)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CivilightButton(GalleryUiCopy.BTN_TAGS, onTags, Modifier.weight(1f), variant = ButtonVariant.COMPACT_GHOST, enabled = some)
            CivilightButton(if (hiddenOnly) GalleryUiCopy.BTN_UNHIDE else GalleryUiCopy.BTN_HIDE, { onHide(hiddenOnly) }, Modifier.weight(1f), variant = ButtonVariant.COMPACT_GHOST, enabled = some)
            CivilightButton(GalleryUiCopy.BTN_DELETE, onDelete, Modifier.weight(1f), variant = ButtonVariant.COMPACT_GHOST, enabled = some)
            CivilightButton(GalleryUiCopy.BTN_COPY_TO, onCopy, Modifier.weight(1.6f), variant = ButtonVariant.COMPACT_GHOST, enabled = some)
        }
    }
}
