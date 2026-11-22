@file:OptIn(ExperimentalSharedTransitionApi::class)

package io.github.lenticualrstone.civilight.feature.canvas.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.size
import io.github.lenticualrstone.civilight.feature.canvas.card.CardViewMotion
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import io.github.lenticualrstone.civilight.designsystem.component.ViewToggle
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.feature.canvas.card.CardKind
import io.github.lenticualrstone.civilight.feature.canvas.card.RecentFileCard
import io.github.lenticualrstone.civilight.feature.canvas.card.SnapCardRow
import io.github.lenticualrstone.civilight.feature.canvas.card.ViewChoice
import io.github.lenticualrstone.civilight.feature.canvas.common.canvasEditContext
import io.github.lenticualrstone.civilight.feature.canvas.image.SlotImage
import io.github.lenticualrstone.civilight.model.app.ViewMode
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.navigation.PageDoc
import io.github.lenticualrstone.civilight.navigation.PageList
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import kotlinx.coroutines.launch

/**
 * 페이지 목록 (CNV-14; no board, Q-PG-05): the project's pages in their order, `목록 | 카드` in the
 * header (CNV-21, remembered per screen), hidden pages folded into one locked row while locked (rule 19
 * §6), a `+` that makes a page and opens it for editing. The card view is one snapping row of page cards
 * (the 최근 연 파일 card, CNV-22 … CNV-24).
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
    val open = { key: io.github.lenticualrstone.civilight.model.Key -> deps.navigator.push(PageDoc(route.projectKey, key)) }

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
                if (view != null && !view.readOnly) {
                    CivilightIconButton(IconId.Plus, PageCopy.TODO_COPY_CANVAS_NEW_PAGE, {
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
            if (view == null) return@CenterColumn
            if (view.isEmpty) {
                Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                    EmptyBlock(title = PageCopy.TODO_COPY_CANVAS_PAGE_LIST_EMPTY)
                }
                return@CenterColumn
            }
            val side = if (inner) 32.dp else 20.dp
            val reduced = CivilightTheme.reducedMotion
            // CNV-45 `목록 ↔ 카드`: thumbnails grow in place into the cards (shared images); list text
            // leaves first, card text comes in after; reduced motion crossfades.
            SharedTransitionLayout {
                AnimatedContent(cards, transitionSpec = { CardViewMotion.transform(reduced) }, label = "pageListView") { showCards ->
                    val visibility = this
                    val plan = CardViewMotion.plan(reduced)
                    @Composable
                    fun Modifier.sharedImage(key: Any): Modifier =
                        if (!plan.sharedImages) this
                        else this.sharedElement(
                            rememberSharedContentState(key),
                            animatedVisibilityScope = visibility,
                            boundsTransform = BoundsTransform { _, _ -> CardViewMotion.growSpec() },
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
                                ) { row, shift ->
                                    RecentFileCard(
                                        kindLine = row.kindLine,
                                        title = row.name,
                                        time = row.date,
                                        inner = inner,
                                        onClick = { open(row.key) },
                                        imageModifier = Modifier.sharedImage("img-${row.key}"),
                                    ) {
                                        SlotImage(row.headerImage, Ratio.R21X9, row.name, onPick = null, shiftDp = shift, compactPlaceholder = true)
                                    }
                                }
                            }
                        } else {
                            item(key = "section") { SectionLabel(view.section, Modifier.padding(horizontal = side).padding(bottom = 4.dp)) }
                            items(view.rows, key = { it.key }) { row ->
                                ListRow(
                                    title = row.name,
                                    subtitle = row.sub,
                                    onClick = { open(row.key) },
                                    leading = {
                                        Box(
                                            Modifier
                                                .sharedImage("img-${row.key}")
                                                .size(CardViewMotion.THUMB_DP.dp)
                                                .clip(RoundedCornerShape(CardViewMotion.THUMB_RADIUS_DP.dp)),
                                        ) {
                                            SlotImage(row.headerImage, Ratio.R21X9, row.name, onPick = null, compactPlaceholder = true)
                                        }
                                    },
                                    trailing = if (row.hiddenTag) ({ HiddenMark() }) else null,
                                    modifier = Modifier.padding(horizontal = side),
                                )
                            }
                        }
                        view.lockedRow?.let { title ->
                            item(key = "locked") {
                                LockRow(title = title, actionLabel = PageCopy.BTN_UNLOCK, onAction = gate.unlock, modifier = Modifier.fillMaxWidth().padding(horizontal = side, vertical = 12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
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
