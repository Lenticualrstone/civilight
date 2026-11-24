package io.github.lenticualrstone.civilight.feature.story.bulk

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.domain.story.bulk.BulkFilter
import io.github.lenticualrstone.civilight.feature.story.common.storyServices
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.navigation.BulkPlaceTime
import io.github.lenticualrstone.civilight.navigation.SceneWhereSheet
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.LocalSheets
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout

/**
 * 장소 · 때 한꺼번에 (STO-31 … STO-35). Cover = `BulkList` (progress, filter chips, episode groups with
 * checkboxes, footer with `장소 정하기` · `때 정하기` · `제안 받기 n`); inner = `BulkWide` (table `씬 | 장소 |
 * 때 | 제안` and the `장소별 씬 수` map). The two sheets are local; `제안 받기` is a page of this entry.
 * `한꺼번에 ›` (점검 센터) hands its scenes over through `deps.results` ([BulkModel.preselectKey]).
 */
@Composable
fun BulkScreen(route: BulkPlaceTime, deps: FeatureDeps) {
    val p = route.projectKey
    val vm = entryViewModel { BulkViewModel(p, deps.storyServices()) }
    val load by vm.state.collectAsState()
    val ready = load as? ProjectLoad.Ready
    val doc = ready?.doc
    val readOnly = ready?.readOnly ?: true
    val gate = rememberHiddenGate(p)
    val locked = !gate.open
    var preselected by rememberSaveable { mutableStateOf(listOf<String>()) }
    var selected by rememberSaveable { mutableStateOf(listOf<String>()) }
    var chapter by rememberSaveable { mutableStateOf<String?>(null) }
    var chapterChosen by rememberSaveable { mutableStateOf(false) }
    // `한꺼번에 ›` hands its scenes over here, also when this screen is already on the stack.
    LaunchedEffect(p) {
        val key = BulkModel.preselectKey(p)
        deps.results.results(key).collect {
            BulkModel.preselection(deps.results.consume(key))?.let { keys ->
                preselected = keys
                selected = keys
                chapterChosen = false
            }
        }
    }
    var filterName by rememberSaveable { mutableStateOf(BulkFilter.ALL.name) }
    var suggesting by rememberSaveable { mutableStateOf(false) }
    var handled by rememberSaveable { mutableStateOf(listOf<String>()) }
    LaunchedEffect(doc != null, chapterChosen) {
        if (doc != null && !chapterChosen) {
            chapter = BulkModel.chapterKey(doc, preselected)
            chapterChosen = true
        }
    }
    val filter = BulkFilter.entries.firstOrNull { it.name == filterName } ?: BulkFilter.ALL
    val ui = remember(doc, chapter, filter, selected, locked) { doc?.let { BulkModel.build(it, chapter, filter, selected, locked) } }
    val sheets = rememberSheetHost()
    val wide = LocalWindowLayout.current.isInner
    val localSheets: LocalSheets = { key ->
        when (key) {
            is BulkPlaceSheet -> { { BulkPlaceContent(key, vm, deps, locked) } }
            is BulkWhenSheet -> { { BulkWhenContent(key, vm) } }
            else -> null
        }
    }

    // 0.2.3: through the shell's layered back, so a layer opened later closes first.
    io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler.current(suggesting) { suggesting = false }
    val showSuggest = suggesting && doc != null && ui != null
    val actions = BulkActions(
        onToggle = { key -> selected = BulkModel.toggle(selected, key) },
        onSelectAll = { ui?.let { selected = BulkModel.selectAll(it, selected) } },
        onFilter = { filterName = it.name },
        onPlace = { ui?.let { sheets.open(BulkPlaceSheet(p, it.selected, chapter)) } },
        onTime = { ui?.let { sheets.open(BulkWhenSheet(p, it.selected)) } },
        onSuggest = {
            handled = emptyList()
            suggesting = true
        },
        onScene = { key -> sheets.open(SceneWhereSheet(p, key)) },
        onPin = { placeKey -> ui?.takeIf { it.canApply(readOnly) }?.let { vm.placeSame(it.selected, PlaceRef(PlaceRefKind.PLACE, placeKey)) } },
        locked = locked,
    )
    // `제안 받기` is a page of this entry (no route), so it moves like a route: the list pushes it in and
    // back pops it, with the shell's push / pop shape (`motion.push`; fades only when reduced).
    val motion = CivilightTheme.motion
    AnimatedContent(showSuggest, Modifier.fillMaxSize(), transitionSpec = { motion.push(forward = targetState) }, label = "bulkPage") { suggestPage ->
        if (suggestPage && doc != null && ui != null) {
            BulkSuggestPage(
                doc = doc,
                selected = ui.selected,
                handled = handled,
                readOnly = readOnly,
                locked = locked,
                sheets = sheets,
                localSheets = localSheets,
                onBack = { suggesting = false },
                onPut = { key ->
                    vm.put(key)
                    handled = handled + key
                },
                onFix = { key -> sheets.open(SceneWhereSheet(p, key)) },
                onSkip = { key -> handled = handled + key },
                onPutAll = { keys ->
                    vm.putAll(keys)
                    handled = handled + keys
                    suggesting = false
                },
            )
        } else {
            CivilightScreen(
                title = ui?.title ?: BulkCopy.TITLE,
                smallLine = if (wide) ui?.wideHead else ui?.head,
                back = IconAction(BulkCopy.BACK) { deps.navigator.pop() },
                actions = {
                    if (ui != null) {
                        TextAction(BulkCopy.SELECT_ALL, actions.onSelectAll)
                        if (wide) {
                            val empty = ui.filter == BulkFilter.EMPTY
                            FilterChip(ui.emptyChip.label, empty, { actions.onFilter(if (empty) BulkFilter.ALL else BulkFilter.EMPTY) })
                        }
                    }
                },
                bottomBar = { ui?.let { BulkFooter(it, readOnly, wide, actions) } },
                sheets = sheets,
                localSheets = localSheets,
            ) { padding ->
                // Loading -> list swaps on the kind (loaded, wide), never on the data (motion audit, feature:story 7).
                ContentSwap(BulkBody.of(loaded = ui != null && doc != null, wide = wide), Modifier.fillMaxSize()) { body ->
                    if (body == BulkBody.LOADING || ui == null || doc == null) {
                        Box(Modifier.fillMaxSize().background(CivilightTheme.colors.bg))
                    } else if (body == BulkBody.WIDE) {
                        BulkWide(ui, doc, padding, readOnly, actions)
                    } else {
                        BulkCover(ui, padding, actions)
                    }
                }
            }
        }
    }
}

/** What the list, the table and the footer do. */
class BulkActions(
    val onToggle: (String) -> Unit,
    val onSelectAll: () -> Unit,
    val onFilter: (BulkFilter) -> Unit,
    val onPlace: () -> Unit,
    val onTime: () -> Unit,
    val onSuggest: () -> Unit,
    /** `+ 장소` / `+ 때` (and a filled cell): the scene's `장소와 때` sheet (STO-20). */
    val onScene: (String) -> Unit,
    /** `고른 씬 → 핀 탭`: the chosen scenes get that place (one undo step). */
    val onPin: (String) -> Unit,
    /** The project's hidden items are locked: hidden places stay off the map. */
    val locked: Boolean = false,
)
