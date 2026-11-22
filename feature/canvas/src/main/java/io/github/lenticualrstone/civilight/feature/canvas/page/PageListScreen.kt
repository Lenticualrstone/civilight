@file:OptIn(ExperimentalSharedTransitionApi::class)

package io.github.lenticualrstone.civilight.feature.canvas.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.card.CardKind
import io.github.lenticualrstone.civilight.designsystem.card.CardMetrics
import io.github.lenticualrstone.civilight.designsystem.card.CardViewMotion
import io.github.lenticualrstone.civilight.designsystem.card.SnapCardRow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.ViewToggle
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.Unhide
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.designsystem.theme.rememberLastNonNull
import io.github.lenticualrstone.civilight.designsystem.theme.rememberUnhide
import io.github.lenticualrstone.civilight.designsystem.theme.unhide
import io.github.lenticualrstone.civilight.feature.canvas.card.RecentFileCard
import io.github.lenticualrstone.civilight.feature.canvas.card.ViewChoice
import io.github.lenticualrstone.civilight.feature.canvas.common.canvasEditContext
import io.github.lenticualrstone.civilight.feature.canvas.image.SlotImage
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.ViewMode
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.navigation.PageDoc
import io.github.lenticualrstone.civilight.navigation.PageList
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.picker.HiddenGate
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import kotlinx.coroutines.launch

/** What the body of the page list shows (one crossfade between them, CNV-14). */
private enum class ListBody { LOADING, EMPTY, LIST }

/**
 * 페이지 목록 (CNV-14; no board, Q-PG-05): the project's pages in their order, `목록 | 카드` in the
 * header (CNV-21, remembered per screen), hidden pages folded into one locked row while locked (rule 19
 * §6), a `+` that makes a page and opens it for editing. The card view is one snapping row of page cards
 * (the 최근 연 파일 card, CNV-22 … CNV-24).
 *
 * Motion (audit §4 canvas 3, 4): rows and cards enter, leave and move with `civilightItem()`; loading,
 * empty and list crossfade; pages that appear when hidden items open are drawn with the unhide (CNV-43);
 * the `목록 ↔ 카드` switch grows thumbnails into cards (CNV-45) while the second card moves aside and the
 * image radius morphs.
 */
@Composable
fun PageListScreen(route: PageList, deps: FeatureDeps) {
    val vm = entryViewModel { PageListViewModel(route.projectKey, deps.projects, deps::canvasEditContext) }
    val load by vm.state.collectAsState()
    val settings by deps.settings.collectAsState()
    val gate = rememberHiddenGate(route.projectKey)
    val scope = rememberCoroutineScope()
    val inner = LocalWindowLayout.current.isInner
    val ready = load as? ProjectLoad.Ready
    val view = remember(ready, gate.open) { ready?.let { PageListModel.build(it.doc, gate.open, it.readOnly, deps.clock.zone) } }
    val cards = ViewChoice.showCards(settings.viewByScreen, ViewChoice.PAGES)
    val open = { key: Key -> deps.navigator.push(PageDoc(route.projectKey, key)) }
    val unhide = rememberUnhide(gate.open)

    CivilightScreen(
        title = PageCopy.SMALL_LINE,
        smallLine = ready?.doc?.project?.name,
        back = IconAction(PageCopy.BACK) { deps.navigator.pop() },
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                ViewToggle(
                    showCards = cards,
                    onChange = { show -> scope.launch { deps.stores.settings.setViewMode(ViewChoice.PAGES, if (show) ViewMode.CARD else ViewMode.LIST) } },
                )
                FoldReveal(view != null && !view.readOnly, horizontal = true) {
                    CivilightIconButton(IconId.Plus, PageCopy.NEW_PAGE, {
                        vm.create { r, key ->
                            if (r == EditResult.READ_ONLY) deps.toasts.show(PageCopy.READ_ONLY, actionLabel = null)
                            key?.takeIf { r == EditResult.APPLIED }?.let(open)
                        }
                    })
                }
            }
        },
    ) { padding ->
        CenterColumn {
            val shown = rememberLastNonNull(view)
            val body = when {
                view == null -> ListBody.LOADING
                view.isEmpty -> ListBody.EMPTY
                else -> ListBody.LIST
            }
            ContentSwap(body, Modifier.fillMaxSize(), label = "pageListBody") { b ->
                when (b) {
                    ListBody.LOADING -> Box(Modifier.fillMaxSize())
                    ListBody.EMPTY -> Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                        EmptyBlock(title = PageCopy.PAGE_LIST_EMPTY)
                    }
                    ListBody.LIST -> shown?.let { PageListBody(it, cards, inner, padding, gate, unhide, open) }
                }
            }
        }
    }
}

/**
 * The list or card view of the pages. CNV-45 `목록 ↔ 카드`: thumbnails grow in place into the cards (shared
 * images); list text leaves first, card text comes in after; reduced motion crossfades.
 */
@Composable
private fun PageListBody(
    view: PageListView,
    cards: Boolean,
    inner: Boolean,
    padding: PaddingValues,
    gate: HiddenGate,
    unhide: Unhide,
    open: (Key) -> Unit,
) {
    val motion = CivilightTheme.motion
    val side = if (inner) 32.dp else 20.dp
    val cardRadius = CardMetrics.radius(CardKind.RECENT)
    val secondKey = view.rows.getOrNull(1)?.key
    SharedTransitionLayout {
        AnimatedContent(cards, transitionSpec = { CardViewMotion.transform(motion) }, label = "pageListView") { showCards ->
            val visibility = this
            val plan = CardViewMotion.plan(motion.reduced)
            @Composable
            fun Modifier.sharedImage(key: Any): Modifier =
                if (!plan.sharedImages) this
                else this.sharedElement(
                    rememberSharedContentState(key),
                    animatedVisibilityScope = visibility,
                    boundsTransform = BoundsTransform { _, _ -> CardViewMotion.growSpec(motion) },
                )
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = padding.calculateTopPadding() + 12.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            ) {
                if (showCards) {
                    item(key = "cards") {
                        SnapCardRow(
                            items = view.rows,
                            key = { it.key },
                            kind = CardKind.RECENT,
                            inner = inner,
                            title = view.section,
                            modifier = civilightItem(),
                        ) { row, imageModifier ->
                            val nudge = if (row.key == secondKey) visibility.rememberNudge() else null
                            RecentFileCard(
                                kindLine = row.kindLine,
                                title = row.name,
                                time = row.date,
                                inner = inner,
                                onClick = { open(row.key) },
                                modifier = Modifier
                                    .graphicsLayer { translationX = (nudge?.value ?: 0f) * density }
                                    .then(if (row.hiddenTag) Modifier.unhide(unhide) else Modifier),
                                imageModifier = Modifier.sharedImage("img-${row.key}"),
                                imageRadius = visibility.imageRadius(own = cardRadius, other = CardViewMotion.THUMB_RADIUS_DP).dp,
                            ) {
                                SlotImage(row.headerImage, Ratio.R21X9, row.name, onPick = null, imageModifier = imageModifier, compactPlaceholder = true)
                            }
                        }
                    }
                } else {
                    item(key = "section") { SectionLabel(view.section, civilightItem().padding(horizontal = side).padding(bottom = 4.dp)) }
                    items(view.rows, key = { it.key }) { row ->
                        val radius = visibility.imageRadius(own = CardViewMotion.THUMB_RADIUS_DP, other = cardRadius)
                        ListRow(
                            title = row.name,
                            subtitle = row.sub,
                            onClick = { open(row.key) },
                            leading = {
                                Box(
                                    Modifier
                                        .sharedImage("img-${row.key}")
                                        .size(CardViewMotion.THUMB_DP.dp)
                                        .clip(RoundedCornerShape(radius.dp)),
                                ) {
                                    SlotImage(row.headerImage, Ratio.R21X9, row.name, onPick = null, compactPlaceholder = true)
                                }
                            },
                            trailing = if (row.hiddenTag) ({ HiddenMark() }) else null,
                            modifier = civilightItem()
                                .padding(horizontal = side)
                                .then(if (row.hiddenTag) Modifier.unhide(unhide) else Modifier),
                        )
                    }
                }
                view.lockedRow?.let { title ->
                    item(key = "locked") {
                        LockRow(title = title, actionLabel = PageCopy.BTN_UNLOCK, onAction = gate.unlock, modifier = civilightItem().fillMaxWidth().padding(horizontal = side, vertical = 12.dp))
                    }
                }
            }
        }
    }
}

/**
 * The second card starts [PageListMotion.NUDGE_DP] to the left and moves to its place on `enterLong` when the
 * card view has just been switched on ("the second card moves aside", CNV-45); none at first show or when
 * reduced.
 */
@Composable
private fun AnimatedContentScope.rememberNudge(): Animatable<Float, AnimationVector1D> {
    val motion = CivilightTheme.motion
    val entering = remember { transition.currentState == EnterExitState.PreEnter }
    val nudge = remember { Animatable(PageListMotion.nudgeDp(motion.reduced, entering)) }
    LaunchedEffect(Unit) {
        if (nudge.value != 0f) nudge.animateTo(0f, motion.move(CivilightMotion.enterLong))
    }
    return nudge
}

/**
 * The image radius while the view switches (list thumbnail 10 dp, card image 14 dp): [own] at rest, [other]
 * before entering and after leaving, moving on `enterLong` (a snap when reduced).
 */
@Composable
private fun AnimatedContentScope.imageRadius(own: Float, other: Float): Float {
    val motion = CivilightTheme.motion
    val radius by transition.animateFloat(transitionSpec = { motion.move(CivilightMotion.enterLong) }, label = "imageRadius") { state ->
        if (state == EnterExitState.Visible) own else other
    }
    return radius
}

/** Pure values of the page list's own motion (JVM-tested). */
object PageListMotion {
    /** The second card sits this far left of its place when the card view starts (dp, CNV-45). */
    const val NUDGE_DP = -24f

    fun nudgeDp(reduced: Boolean, entering: Boolean): Float = if (reduced || !entering) 0f else NUDGE_DP
}

/** The small `숨김` tag of a hidden page while hidden items are open (rule 19 §6). */
@Composable
private fun HiddenMark(modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    CivilightText(
        PageCopy.HIDDEN_TAG,
        CivilightTheme.type.small,
        modifier.clip(RoundedCornerShape(8.dp)).background(colors.tint).padding(horizontal = 6.dp, vertical = 1.dp),
        color = colors.ink2,
        maxLines = 1,
    )
}
