package io.github.lenticualrstone.civilight.feature.world.term

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTabs
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.ImmersiveDetailScaffold
import io.github.lenticualrstone.civilight.designsystem.component.holoImage
import io.github.lenticualrstone.civilight.designsystem.component.ImmersiveMath
import io.github.lenticualrstone.civilight.designsystem.component.LensChip
import io.github.lenticualrstone.civilight.designsystem.component.ReportBottomChrome
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.component.rememberImmersiveDetailState
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import androidx.compose.foundation.layout.statusBars
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.domain.world.term.BulkMode
import io.github.lenticualrstone.civilight.domain.world.term.RevealTarget
import io.github.lenticualrstone.civilight.domain.world.term.TermCandidate
import io.github.lenticualrstone.civilight.domain.world.term.TermChip
import io.github.lenticualrstone.civilight.domain.world.term.TermFlow
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.CardAccess
import io.github.lenticualrstone.civilight.feature.world.common.CardBodySwap
import io.github.lenticualrstone.civilight.feature.world.common.CardImageContent
import io.github.lenticualrstone.civilight.feature.world.common.CardImageUi
import io.github.lenticualrstone.civilight.feature.world.common.CardImages
import io.github.lenticualrstone.civilight.feature.world.common.DescriptionSheet
import io.github.lenticualrstone.civilight.feature.world.common.FieldSection
import io.github.lenticualrstone.civilight.feature.world.common.FixedRow
import io.github.lenticualrstone.civilight.feature.world.common.LensBanner
import io.github.lenticualrstone.civilight.feature.world.common.LockedCard
import io.github.lenticualrstone.civilight.feature.world.common.MergePickSheet
import io.github.lenticualrstone.civilight.feature.world.common.NoticeLine
import io.github.lenticualrstone.civilight.feature.world.common.ParagraphUi
import io.github.lenticualrstone.civilight.feature.world.common.WorldCommandBar
import io.github.lenticualrstone.civilight.feature.world.common.WorldEnv
import io.github.lenticualrstone.civilight.feature.world.common.WorldGoneCard
import io.github.lenticualrstone.civilight.feature.world.common.WorldTargets
import io.github.lenticualrstone.civilight.feature.world.common.cardPadding
import io.github.lenticualrstone.civilight.feature.world.common.itemSheet
import io.github.lenticualrstone.civilight.feature.world.common.lensBannerSlot
import io.github.lenticualrstone.civilight.feature.world.common.openFromWorld
import io.github.lenticualrstone.civilight.feature.world.common.stageOf
import io.github.lenticualrstone.civilight.feature.world.common.worldBack
import io.github.lenticualrstone.civilight.feature.world.place.WorldHeaderLens
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.navigation.AddFieldSheet
import io.github.lenticualrstone.civilight.navigation.CheckCenter
import io.github.lenticualrstone.civilight.navigation.FieldValueEditSheet
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.lens.lensChipLabel
import io.github.lenticualrstone.civilight.ui.nav.LocalPaneRole
import io.github.lenticualrstone.civilight.ui.nav.PaneRole
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.CommandScope
import io.github.lenticualrstone.civilight.ui.scaffold.LocalCommandBarHost
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.lenticualrstone.civilight.navigation.TermCard as TermCardRoute

/** What the open card's tab bodies call back (kept in one place so the three tabs stay small). */
private class TermActions(
    val onTags: () -> Unit,
    val onShowEmpty: () -> Unit,
    val onReveal: () -> Unit,
    val onRelated: () -> Unit,
    val onField: (String) -> Unit,
    val onAddField: () -> Unit,
    val onAliases: () -> Unit,
    val onDescription: () -> Unit,
    val onMerge: () -> Unit,
    val onChip: (TermChip) -> Unit,
    val onSearchOn: () -> Unit,
    val onOpen: (Route) -> Unit,
    val onLink: (TermCandidate) -> Unit,
    val onReject: (TermCandidate) -> Unit,
    val onWarning: () -> Unit,
    val onIntended: (Key) -> Unit,
    val onBulk: (BulkMode) -> Unit,
)

/**
 * 용어 카드 (WLD-08) on the immersive detail frame (boards `Immersive` kind `term`, `ImmContent`, `ImmTermWhy`):
 * the reference image (else a colour field) behind everything, the name as the title that docks into the head,
 * `처음 나옴 1-7 씬 3` as the subtitle, `⋯` and the lens chip in the head, and the tabs `개요 · 쓰인 곳 · 연결` on
 * the bottom glass. A locked or trashed term keeps the plain card frame (no image, no tabs, nothing named).
 *
 * Command bar (DS-65): in a `목록 + 상세` pane it goes to the frame's one bar; alone in the window the tabs take
 * the bottom (the project search stays on the list and the head).
 */
@Composable
fun TermCardScreen(route: TermCardRoute, deps: FeatureDeps) {
    val vm = entryViewModel(key = "term:${route.termKey}") { TermCardViewModel(route.projectKey, route.termKey, WorldEnv.of(deps)) }
    val ui by vm.ui.collectAsState()
    val doc by vm.doc.collectAsState()
    val readOnly by vm.readOnly.collectAsState()
    val sheets = rememberSheetHost()
    val gate = rememberHiddenGate(route.projectKey)
    val scope = rememberCoroutineScope()
    var showEmpty by rememberSaveable(route.termKey) { mutableStateOf(false) }
    // (0.2.0, WLD-73) Opened from a `용어 후보 · N` row: open on `쓰인 곳` and scroll to the `후보` group once.
    var toCandidates by rememberSaveable(route.termKey) { mutableStateOf(TermCardScroll.take(route.projectKey, route.termKey)) }
    var tab by rememberSaveable(route.termKey) { mutableStateOf(if (toCandidates) TermTab.USES else TermTab.OVERVIEW) }
    val inPane = LocalCommandBarHost.current != null && CommandScope.of(LocalPaneRole.current) != null
    val inDetailPane = inPane && LocalPaneRole.current == PaneRole.DETAIL
    val card = ui
    val open = card?.access == CardAccess.Open && card.head != null

    fun choose(target: RevealTarget) {
        scope.launch {
            when (val outcome = vm.setReveal(target)) {
                is EditOutcome.Done -> {
                    sheets.closeAll()
                    outcome.toast?.let { deps.toasts.show(it) { vm.undoLast() } }
                }
                is EditOutcome.Refused -> deps.toasts.show(outcome.reason, actionLabel = null)
                EditOutcome.ReadOnly -> Unit
            }
        }
    }

    val actions = TermActions(
        onTags = { sheets.open(TermTagSheet(route.termKey)) },
        onShowEmpty = { showEmpty = true },
        onReveal = { sheets.open(RevealSheet(route.termKey)) },
        onRelated = { sheets.open(RelatedSheet(route.termKey)) },
        onField = { def -> sheets.open(FieldValueEditSheet(route.projectKey, EntityKind.TERM, route.termKey, def)) },
        onAddField = { sheets.open(AddFieldSheet(route.projectKey, EntityKind.TERM, route.termKey)) },
        onAliases = { sheets.open(AliasSheet(route.termKey)) },
        onDescription = { sheets.open(DescriptionSheet(WorldKind.TERM.name, route.termKey)) },
        onMerge = { sheets.open(MergePickSheet(WorldKind.TERM.name, route.termKey)) },
        onChip = { chip ->
            when (chip) {
                is TermChip.SuggestReveal -> choose(chip.suggestion.target)
                is TermChip.TurnOffSearch -> vm.setTextSearch(false)
            }
        },
        onSearchOn = { vm.setTextSearch(true) },
        onOpen = { deps.navigator.openFromWorld(it) },
        onLink = { candidate -> scope.launch { if (vm.linkCandidate(candidate)) deps.toasts.show(WorldCopy.LINKED_TOAST) { vm.undoLast() } } },
        onReject = { candidate -> scope.launch { if (vm.rejectCandidate(candidate)) deps.toasts.show(WorldCopy.REJECTED_TOAST) { vm.undoLast() } } },
        onWarning = { deps.navigator.push(CheckCenter(route.projectKey, CheckFilter.TERM.name)) },
        onIntended = { linkKey -> scope.launch { if (vm.markIntended(linkKey)) deps.toasts.show(WorldCopy.INTENDED_TOAST) { vm.undoLast() } } },
        onBulk = { mode -> sheets.open(BulkCandidatesSheet(route.termKey, mode)) },
    )
    val back = worldBack(route, deps)

    CenterColumn {
        CivilightScreen(
            title = card?.title.orEmpty(),
            smallLine = if (open) null else WorldCopy.SMALL_TERM,
            back = back,
            lens = if (open) null else doc?.let(::lensChipLabel),
            onLens = { sheets.open(LensSheet(route.projectKey)) },
            banner = if (open) null else lensBannerSlot(doc?.let { LensBanner.of(it, readOnly) }, vm::backToLatest),
            // The immersive frame draws its own head.
            topBar = if (open) ({}) else null,
            commandBar = if (!open || inPane) ({
                WorldCommandBar(deps, route.projectKey, WorldCopy.SEARCH_CARD, EntrySurface.TERM, RecorderTargetKind.TERM, route.termKey)
            }) else null,
            sheets = sheets,
            localSheets = { key ->
                revealSheet(key) ?: bulkSheet(key) ?: termSheet(key, doc, readOnly, gate, vm) ?: itemSheet(key, doc, vm, readOnly, gate)
            },
        ) { padding ->
            CardBodySwap(card, { stageOf(it?.access) }) { shown ->
                val c = shown
                val d = doc
                if (c != null && c.access == CardAccess.Open && c.head != null && d != null) {
                    if (!inPane) ReportBottomChrome(ImmersiveMath.BAR_ROW_DP.dp)
                    val scroll = rememberSaveable(route.termKey, saver = ScrollState.Saver) { ScrollState(0) }
                    val immersive = rememberImmersiveDetailState(scroll)
                    val lens = lensChipLabel(d)
                    val banner = lensBannerSlot(LensBanner.of(d, readOnly), vm::backToLatest)
                    // Where the `후보` group sits (root y) and where the frame starts, for the one-shot scroll.
                    var frameTop by remember { mutableFloatStateOf(Float.NaN) }
                    var candidatesTop by remember { mutableFloatStateOf(Float.NaN) }
                    val density = LocalDensity.current
                    val statusPx = androidx.compose.foundation.layout.WindowInsets.statusBars.getTop(density).toFloat()
                    if (toCandidates) {
                        LaunchedEffect(route.termKey) {
                            val (y, top) = snapshotFlow { candidatesTop to frameTop }.first { !it.first.isNaN() && !it.second.isNaN() }
                            // The group title lands under the docked head (status bar + 56 dp row) with a little air.
                            val headPx = statusPx + with(density) { (ImmersiveMath.HEADER_ROW_DP + 16f).dp.toPx() }
                            scroll.scrollTo((scroll.value + (y - top) - headPx).toInt().coerceAtLeast(0))
                            toCandidates = false
                        }
                        // No `후보` group (the candidates went away): the request ends at the tab.
                        LaunchedEffect(c.groups.none { it.id == TermCardScroll.CANDIDATES }) {
                            if (c.groups.none { it.id == TermCardScroll.CANDIDATES }) toCandidates = false
                        }
                    }
                    val goBack: () -> Unit = back?.onClick ?: { deps.navigator.pop() }
                    val motion = CivilightTheme.motion
                    ImmersiveDetailScaffold(
                        title = c.head.name,
                        subtitle = c.subtitle,
                        // DS-70: the `목록 + 상세` detail pane has no back (the list is beside it).
                        onBack = if (inDetailPane) null else goBack,
                        modifier = Modifier
                            .onGloballyPositioned { frameTop = it.positionInRoot().y }
                            .then(if (inPane) Modifier.padding(bottom = padding.calculateBottomPadding()) else Modifier),
                        bottomInset = if (inPane) 0.dp else null,
                        hero = { TermHero(CardImages.of(d, WorldKind.TERM, route.termKey, c.head.name), c.head.name) },
                        actions = { WorldHeaderLens(lens) { sheets.open(LensSheet(route.projectKey)) } },
                        bottomBar = {
                            CivilightTabs(
                                tabs = TermTab.entries.map { it.label },
                                selectedIndex = tab.ordinal,
                                onSelect = { tab = TermTab.entries[it] },
                            )
                        },
                        state = immersive,
                        onMore = { sheets.open(TermMenuSheet(route.termKey)) },
                    ) {
                        banner?.let { Box(Modifier.fillMaxWidth().padding(bottom = 12.dp)) { it() } }
                        AnimatedContent(tab, transitionSpec = { motion.swap(clip = false) }, label = "termTab") { t ->
                            when (t) {
                                TermTab.OVERVIEW -> OverviewTab(c, readOnly, showEmpty, actions)
                                TermTab.USES -> UsesTab(c, readOnly, actions) { candidatesTop = it }
                                TermTab.LINKS -> LinksTab(c, readOnly, actions)
                            }
                        }
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = cardPadding(padding)) {
                        when (val access = c?.access) {
                            is CardAccess.Locked -> item(key = "locked") { Box(civilightItem()) { LockedCard(access.text, gate.unlock) } }
                            is CardAccess.Gone -> item(key = "gone") { Box(civilightItem()) { WorldGoneCard(access, d, vm, WorldKind.TERM, route.termKey, readOnly, gate) } }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

/**
 * The hero: the reference image's 16:9 crop decoded for the pane and drawn top-aligned to fill (holo kept),
 * a preset as its art, else a colour field from the theme tokens (never a stand-in picture).
 */
@Composable
private fun BoxScope.TermHero(image: CardImageUi, name: String) {
    val colors = CivilightTheme.colors
    val field = remember(colors) { Brush.verticalGradient(listOf(colors.chip, colors.raise, colors.bg)) }
    Box(Modifier.fillMaxSize().background(field))
    when (image) {
        is CardImageUi.Initial -> Unit
        is CardImageUi.Preset -> CardImageContent(image, name, targetWidth = 1280)
        is CardImageUi.Picked -> BoxWithConstraints(Modifier.fillMaxSize()) {
            val deps = LocalFeatureDeps.current
            // Filling the pane height with a 16:9 crop needs about height × 16 / 9 px of width.
            val target = (constraints.maxHeight * 16 / 9).coerceIn(640, 2560)
            val bitmap by produceState<ImageBitmap?>(null, image, target) {
                value = withContext(Dispatchers.IO) {
                    deps.stores.blobs.fileOf(image.hash)?.let { runCatching { CardImages.decode(it, image.crop, target) }.getOrNull() }?.asImageBitmap()
                }
            }
            androidx.compose.animation.Crossfade(bitmap, Modifier.fillMaxSize(), animationSpec = CivilightTheme.motion.spec(io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion.standard), label = "termHero") { shown ->
                if (shown != null) {
                    Image(
                        shown,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize().holoImage(image.holo),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                    )
                }
            }
        }
    }
}

/** `개요`: tags, `설명`, aliases, `첫 공개`, `세부 항목` (board `ImmContent` term branch, top to bottom). */
@Composable
private fun OverviewTab(card: TermCardUi, readOnly: Boolean, showEmpty: Boolean, a: TermActions) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val head = card.head ?: return
    Column(Modifier.fillMaxWidth()) {
        if (card.tags.isNotEmpty() || card.canAddTag) {
            ChipFlow(gap = 8.dp) {
                card.tags.forEach { tag -> PlainChip(tag.name, onClick = if (card.canAddTag) a.onTags else null) }
                if (card.canAddTag) AddChip(WorldCopy.ADD_TAG, a.onTags)
            }
        }
        FoldRevealValue(card.sameName) { text ->
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                CivilightText(text, type.small, color = colors.ink3)
                if (!readOnly) TextAction(WorldCopy.MENU_MERGE, a.onMerge, color = colors.acc, style = type.small)
            }
        }
        SectionLabel(
            WorldCopy.SEC_DESCRIPTION,
            Modifier.padding(top = 30.dp, bottom = 10.dp),
            trailing = if (readOnly || card.descriptionLocked) null else WorldCopy.EDIT_DESCRIPTION,
            onTrailing = a.onDescription,
        )
        Description(head.description)
        // Aliases as chips; tapping opens the alias editor (WLD-16), `+ 별칭` adds.
        if (card.aliasChips.isNotEmpty() || !readOnly) {
            ChipFlow(Modifier.padding(top = 12.dp), gap = 8.dp) {
                card.aliasChips.forEach { PlainChip(it, onClick = if (readOnly) null else a.onAliases) }
                if (!readOnly) AddChip(WorldCopy.ADD_ALIAS, a.onAliases)
            }
            card.aliases?.note?.let { CivilightText(it, type.small, Modifier.padding(top = 6.dp), color = colors.acc) }
        }
        Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
            card.reveal?.let { FixedRow(it, a.onReveal) }
        }
        SectionLabel(WorldCopy.counted(WorldCopy.SEC_FIELDS, card.fields.filled.size), Modifier.padding(top = 30.dp, bottom = 10.dp))
        FieldSection(
            section = card.fields,
            showEmpty = showEmpty,
            readOnly = readOnly,
            onShowEmpty = a.onShowEmpty,
            onEdit = { a.onField(it.fieldDefKey) },
            onAdd = a.onAddField,
        )
    }
}

/** `설명`: the first paragraph as the large serif lead (19 / 33), the rest 16 / 28 in `ink2`; `@` links keep their shape. */
@Composable
private fun Description(paragraphs: List<ParagraphUi>) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    if (paragraphs.isEmpty()) {
        CivilightText(WorldCopy.EMPTY_VALUE, type.lead, color = colors.ink3)
        return
    }
    paragraphs.forEachIndexed { i, p ->
        DecoratedText(
            text = AnnotatedString(p.text),
            style = if (i == 0) type.lead.merge(TextStyle(fontSize = 19.sp, lineHeight = 33.sp)) else type.lead.merge(TextStyle(lineHeight = 28.sp)),
            modifier = Modifier.padding(top = if (i == 0) 0.dp else 12.dp),
            color = if (i == 0) colors.ink else colors.ink2,
            decorations = p.decorations,
        )
    }
}

/**
 * `쓰인 곳`: notices and chips (WLD-25, WLD-18), `쓰인 흐름`, `작중에서` (game-output links), the `후보` group with
 * `모두 잇기 · 모두 아님`, then `원고 밖 · 메모`. [onCandidatesAt] gets the root y of the `후보` group (WLD-73 scroll).
 */
@Composable
private fun UsesTab(card: TermCardUi, readOnly: Boolean, a: TermActions, onCandidatesAt: (Float) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(Modifier.fillMaxWidth()) {
        if (card.notices.isNotEmpty() || card.searchOff || card.chips.isNotEmpty()) {
            TermNotices(card, readOnly, a.onChip, a.onSearchOn)
        }
        card.flow.title?.let { title ->
            SectionLabel(title, Modifier.padding(top = 18.dp, bottom = 10.dp))
            FlowStripView(card.flow)
            FlowLegendView(Modifier.padding(top = 10.dp))
        }
        card.groups.forEach { group ->
            val candidates = group.id == TermCardScroll.CANDIDATES
            val title = if (group.id == "usage") WorldCopy.counted(WorldCopy.SEC_QUOTES, group.rows.size) else group.title
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp, bottom = 10.dp)
                    .then(if (candidates) Modifier.onGloballyPositioned { onCandidatesAt(it.positionInRoot().y) } else Modifier),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel(title, Modifier.weight(1f))
                // WLD-23: `모두 잇기 · 모두 아님` over the 후보 group; each word opens the sheet in its mode.
                if (candidates && card.bulkActions.size == 2 && !readOnly) {
                    TextAction(card.bulkActions[0], { a.onBulk(BulkMode.LINK_ALL) }, color = colors.acc, style = type.small)
                    CivilightText(WorldCopy.SEP, type.small, color = colors.ink3)
                    TextAction(card.bulkActions[1], { a.onBulk(BulkMode.REJECT_ALL) }, color = colors.acc, style = type.small)
                }
            }
            group.rows.forEach { row ->
                val onOpen = row.target?.let { t -> { a.onOpen(t) } }
                if (group.id == "outside") {
                    UsageRowView(row = row, onOpen = onOpen, readOnly = readOnly, onWarning = a.onWarning)
                } else {
                    QuoteRowView(
                        row = row,
                        onOpen = onOpen,
                        readOnly = readOnly,
                        onLink = { row.candidate?.let(a.onLink) },
                        onReject = { row.candidate?.let(a.onReject) },
                        onWarning = a.onWarning,
                        onIntended = { row.linkKey?.let(a.onIntended) },
                    )
                }
            }
        }
    }
}

/** `연결`: `같은 씬에 나온 것`, `관련` (tap → the `관련` sheet), characters linking to the term. */
@Composable
private fun LinksTab(card: TermCardUi, readOnly: Boolean, a: TermActions) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(Modifier.fillMaxWidth()) {
        if (card.neighbors.isNotEmpty()) {
            SectionLabel(WorldCopy.SEC_NEIGHBORS, Modifier.padding(top = 4.dp, bottom = 10.dp))
            ChipFlow(gap = 8.dp) { card.neighbors.forEach { KindChip(it, a.onOpen) } }
        }
        card.related?.let { related ->
            Column(Modifier.fillMaxWidth().padding(top = if (card.neighbors.isEmpty()) 4.dp else 30.dp)) { FixedRow(related, a.onRelated) }
        }
        if (card.linkingCharacters.isNotEmpty()) {
            SectionLabel(WorldCopy.LINKED_CHARACTERS, Modifier.padding(top = 30.dp, bottom = 10.dp))
            ChipFlow(gap = 8.dp) { card.linkingCharacters.forEach { KindChip(it, a.onOpen) } }
        }
        if (card.neighbors.isEmpty() && card.linkingCharacters.isEmpty() && card.related == null) {
            CivilightText(WorldCopy.NO_LINKS, type.small, Modifier.padding(top = 12.dp), color = colors.ink3)
        }
    }
}

/** A 32 dp chip on `chip` (board `S.chip`); [onClick] null = not tappable. */
@Composable
private fun PlainChip(text: String, onClick: (() -> Unit)?) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .then(if (onClick != null) Modifier.pressable(PressAction.Click(onClick), role = Role.Button, hitTarget = HitTarget.CHIP, shape = shape) else Modifier)
            .heightIn(min = 32.dp)
            .clip(shape)
            .background(colors.chip)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { CivilightText(text, CivilightTheme.type.chip, color = colors.ink2, maxLines = 1) }
}

/** `+ 태그` / `+ 별칭`: an outlined 32 dp chip (board `X.addChip`). */
@Composable
private fun AddChip(text: String, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .pressable(PressAction.Click(onClick), role = Role.Button, hitTarget = HitTarget.CHIP, shape = shape)
            .heightIn(min = 32.dp)
            .border(1.dp, colors.line, shape)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { CivilightText(text, CivilightTheme.type.chip, color = colors.ink2, maxLines = 1) }
}

/** `인물 하윤`: the kind word in `ink3` 11, then the name; opens the item. */
@Composable
private fun KindChip(chip: TermLinkChipUi, onOpen: (Route) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .then(chip.target?.let { t -> Modifier.pressable(PressAction.Click(onClick = { onOpen(t) }), role = Role.Button, hitTarget = HitTarget.CHIP, shape = shape) } ?: Modifier)
            .heightIn(min = 32.dp)
            .clip(shape)
            .background(colors.chip)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CivilightText(chip.kind, type.micro, color = colors.ink3, maxLines = 1)
        CivilightText(chip.name, type.chip, color = colors.ink2, maxLines = 1)
    }
}

@Composable
private fun TermNotices(card: TermCardUi, readOnly: Boolean, onChip: (TermChip) -> Unit, onSearchOn: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(Modifier.fillMaxWidth()) {
        card.notices.forEach { NoticeLine(it) }
        FoldReveal(card.searchOff) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CivilightText("${WorldCopy.NOTICE_SEARCH_OFF}${WorldCopy.SEP}", type.small, color = colors.ink3)
                TextAction(WorldCopy.BTN_SEARCH_ON, onSearchOn, color = colors.acc, style = type.small)
            }
        }
        FoldRevealValue(card.chips.takeIf { it.isNotEmpty() }) { chips ->
            ChipFlow(Modifier.padding(top = 8.dp)) {
                chips.forEach { chip -> FilterChip(label = chip.label, selected = false, onClick = { onChip(chip) }, enabled = !readOnly) }
            }
        }
    }
}
