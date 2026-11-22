package io.github.lenticualrstone.civilight.feature.map.gen

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor
import io.github.lenticualrstone.civilight.feature.map.common.FoldSwitch
import io.github.lenticualrstone.civilight.feature.map.common.MapMotion
import io.github.lenticualrstone.civilight.feature.map.common.appear
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.BottomGlassBar
import io.github.lenticualrstone.civilight.designsystem.component.ButtonRow
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightSwitch
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.GeneratorStepRow
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.StepChip
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.common.GlassCard
import io.github.lenticualrstone.civilight.model.map.GeneratedMapKind
import io.github.lenticualrstone.civilight.model.map.LandRatio
import io.github.lenticualrstone.civilight.model.map.MapStyle
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import kotlinx.serialization.Serializable

/** `후보 기록 N`: every candidate so far, choosable again, with `새 후보` (feature-local). */
@Serializable
data object CandidateHistorySheet : SheetKey

/**
 * `지형 만들기` 2/2 (WLD-54, WLD-55; boards `MapGen` cover, `MapGenWide` inner).
 * - Cover: the preview on top; step tabs `해안선 · 산 · 강 · 숲 · 기후 · 마을 · 길` (lock mark on
 *   locked steps); the step card `강 · 시드 0031` with its chips and `강만 다시 · 강 고정`; below it
 *   `기본` and the style chips; bottom `후보 기록 9 · 이 지도로 시작`.
 * - Inner: the preview fills the left; a side panel with `기본`, `단계 · 고정 · 다시` (seed, `고정`,
 *   `다시` per step; the chosen step's chips under it) and the style section; the candidate strip
 *   with `새 후보` at the bottom left; `이 지도로 시작` in the header.
 * `다시` re-seeds that step only, `고정` keeps it; every candidate stays in the history.
 */
@Composable
internal fun StepsPage(
    state: GenFlowState,
    vm: MapGenerateViewModel,
    back: IconAction,
    readOnly: Boolean,
    defaultName: String?,
    set: (GenFlowState) -> Unit,
    onStart: () -> Unit,
) {
    val ui = GenFlow.ui(state)
    val inner = LocalWindowLayout.current.isInner
    val sheets = rememberSheetHost()
    val newCandidates = { set(GenFlow.newCandidates(state, ::nextSeed, vm.editContext().now())) }
    CivilightScreen(
        title = if (inner) MapCopy.GEN_TITLE else defaultName ?: MapCopy.GEN_TITLE,
        smallLine = if (inner) ui.header else ui.coverSmall,
        back = back,
        actions = {
            if (inner) CivilightButton(MapCopy.BTN_START_MAP, onStart, variant = ButtonVariant.COMPACT_PRIMARY, enabled = !readOnly)
        },
        bottomBar = if (inner) null else ({
            BottomGlassBar(barHeight = 92.dp) {
                ButtonRow(Modifier.fillMaxWidth()) {
                    CivilightButton(ui.historyLabel, { sheets.open(CandidateHistorySheet) }, Modifier.weight(1f), variant = ButtonVariant.GHOST)
                    CivilightButton(MapCopy.BTN_START_MAP, onStart, Modifier.weight(2f), enabled = !readOnly)
                }
            }
        }),
        sheets = sheets,
        localSheets = { key ->
            if (key != CandidateHistorySheet) null else {
                {
                    HistoryContent(state, vm, onChoose = { set(GenFlow.chooseCandidate(state, it)); close() }, onNew = newCandidates, onClose = ::close)
                }
            }
        },
    ) { padding ->
        FoldSwitch(inner) { isInner ->
        if (isInner) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().padding(end = 260.dp)) { WorldPreview(vm, state.input(), state.format, token = CivilightMotion.mapReroll) }
                CandidateStrip(
                    state, vm, onChoose = { set(GenFlow.chooseCandidate(state, it)) }, onNew = newCandidates,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = padding.calculateBottomPadding() + 16.dp),
                )
                GlassCard(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = padding.calculateTopPadding() + 8.dp, end = 16.dp, bottom = 16.dp)
                        .width(236.dp)
                        .fillMaxHeight(),
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BaseSection(ui, state, set)
                        SectionLabel(MapCopy.GEN_STEPS, Modifier.padding(top = 8.dp))
                        ui.steps.forEach { row ->
                            GeneratorStepRow(
                                title = row.name,
                                locked = row.locked,
                                onToggleLock = { set(GenFlow.lock(state, row.kind, it)) },
                                onReroll = { set(GenFlow.reroll(state, row.kind, nextSeed())) },
                                seed = row.seed,
                                selected = row.focused,
                                onClick = { set(GenFlow.focus(state, row.kind)) },
                            )
                            // The focused step's chips fold open under it and fold away when focus moves on.
                            FoldRevealValue(ui.questions.takeIf { row.focused && it.isNotEmpty() }) { questions ->
                                Column(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                                        .background(CivilightTheme.colors.sel).padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) { Questions(questions, state, set, stacked = true) }
                            }
                        }
                        StyleSection(ui, state, set)
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding() - 10.dp)) {
                Box(Modifier.fillMaxWidth().height(380.dp)) { WorldPreview(vm, state.input(), state.format, token = CivilightMotion.mapReroll) }
                Column(
                    Modifier.fillMaxWidth().weight(1f).background(CivilightTheme.colors.bg).verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = padding.calculateBottomPadding() + 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ui.steps.forEach { row ->
                            StepChip(
                                row.name, row.focused, { set(GenFlow.focus(state, row.kind)) }, Modifier.weight(1f),
                                icon = if (row.locked) IconId.StepLock else IconId.StepOpen,
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        CivilightText(ui.cardName, CivilightTheme.type.itemTitle, Modifier.weight(1f))
                        CivilightText(MapCopy.seed(ui.cardSeed), CivilightTheme.type.number, color = CivilightTheme.colors.ink3)
                    }
                    Questions(ui.questions, state, set, stacked = false)
                    ButtonRow(Modifier.fillMaxWidth()) {
                        CivilightButton(ui.rerollLabel, { set(GenFlow.reroll(state, state.focus, nextSeed())) }, Modifier.weight(1f), variant = ButtonVariant.GHOST, icon = IconId.Dice, enabled = !ui.focusedLocked)
                        CivilightButton(ui.lockLabel, { set(GenFlow.lock(state, state.focus, !ui.focusedLocked)) }, Modifier.weight(1f), variant = ButtonVariant.GHOST, icon = if (ui.focusedLocked) IconId.StepLock else IconId.StepOpen)
                    }
                    BaseSection(ui, state, set)
                    StyleSection(ui, state, set)
                }
            }
        }
        }
    }
}

/** `기본`: `대륙 · 두 대륙 · 섬 무리 · 한 지역` and `바다 많음 · 반반 · 땅 많음`. */
@Composable
private fun BaseSection(ui: GenUi, state: GenFlowState, set: (GenFlowState) -> Unit) {
    SectionLabel(MapCopy.GEN_BASE)
    ChipFlow { ui.kinds.forEachIndexed { i, k -> FilterChip(k, i == ui.kindIndex, { set(GenFlow.kind(state, GeneratedMapKind.entries[i])) }) } }
    ChipFlow { ui.ratios.forEachIndexed { i, r -> FilterChip(r, i == ui.ratioIndex, { set(GenFlow.ratio(state, LandRatio.entries[i])) }) } }
}

/** `상세 지형 그림`: `색면 · 먹선 · 색 · 양피지` and `등고선` (the drawing of styles is WLD-57). */
@Composable
private fun StyleSection(ui: GenUi, state: GenFlowState, set: (GenFlowState) -> Unit) {
    SectionLabel(ui.styleSection, Modifier.padding(top = 8.dp))
    ChipFlow { ui.styles.forEachIndexed { i, s -> FilterChip(s, i == ui.styleIndex, { set(GenFlow.style(state, MapStyle.entries[i])) }) } }
    Row(verticalAlignment = Alignment.CenterVertically) {
        CivilightText(MapCopy.CONTOURS, CivilightTheme.type.small, Modifier.weight(1f), color = CivilightTheme.colors.ink2)
        CivilightSwitch(state.contours, { set(GenFlow.contours(state, it)) })
    }
}

/** The chips of the focused step (`개수 · 적게 · 보통 · 많이`); `호수` is a switch `막힌 곳에 호수`. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Questions(questions: List<QuestionUi>, state: GenFlowState, set: (GenFlowState) -> Unit, stacked: Boolean) {
    questions.forEach { q ->
        val content: @Composable () -> Unit = {
            if (q.toggle) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CivilightSwitch(q.selected == q.options.first(), { on -> set(GenFlow.choose(state, state.focus, q.key, if (on) q.options[0] else q.options[1])) })
                    CivilightText(MapCopy.LAKE_SUB, CivilightTheme.type.small, color = CivilightTheme.colors.ink2)
                }
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    q.options.forEach { o -> FilterChip(o, o == q.selected, { set(GenFlow.choose(state, state.focus, q.key, o)) }) }
                }
            }
        }
        if (stacked) {
            CivilightText(q.label, CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
            content()
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CivilightText(q.label, CivilightTheme.type.small, Modifier.width(56.dp), color = CivilightTheme.colors.ink3)
                Box(Modifier.weight(1f)) { content() }
            }
        }
    }
}

/** The inner strip: the last batch of candidates (92 × 64; the chosen one ringed) and `새 후보`. */
@Composable
private fun CandidateStrip(state: GenFlowState, vm: MapGenerateViewModel, onChoose: (Int) -> Unit, onNew: () -> Unit, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        // A new batch appears one tile after another (the keys are the history indices, so a new tile is a new element).
        state.batch.forEachIndexed { n, i -> key(i) { Candidate(state, vm, i, Modifier.size(92.dp, 64.dp).appear(delayMs = MapMotion.stagger(n)), onChoose) } }
        CivilightButton(MapCopy.BTN_NEW_CANDIDATES, onNew, variant = ButtonVariant.COMPACT_GHOST, icon = IconId.Dice)
    }
}

@Composable
private fun Candidate(state: GenFlowState, vm: MapGenerateViewModel, index: Int, modifier: Modifier, onChoose: (Int) -> Unit) {
    val input = GenFlow.candidateInput(state, index) ?: return
    val shape = RoundedCornerShape(10.dp)
    val colors = CivilightTheme.colors
    // The ring colour crosses over when the chosen candidate changes; its width follows.
    val ring by animatedColor(if (state.chosen == index) colors.acc else colors.line, label = "candidateRing")
    val ringWidth by animateDpAsState(if (state.chosen == index) 2.dp else 1.dp, CivilightTheme.motion.move(CivilightMotion.standard), label = "candidateRingWidth")
    Box(
        modifier
            .pressable(onClick = { onChoose(index) }, role = Role.RadioButton, onClickLabel = "${index + 1}")
            .clip(shape)
            .insetOutline(ring, ringWidth, shape),
    ) { WorldPreview(vm, input, state.format, token = CivilightMotion.mapReroll) }
}

/** `후보 기록 N`: all candidates in a grid, newest last, and `새 후보`. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.HistoryContent(state: GenFlowState, vm: MapGenerateViewModel, onChoose: (Int) -> Unit, onNew: () -> Unit, onClose: () -> Unit) {
    SheetHeader(GenFlow.ui(state).historyLabel, onClose = onClose)
    SheetScrollBody {
        CivilightButton(MapCopy.BTN_NEW_CANDIDATES, onNew, Modifier.padding(vertical = 10.dp), variant = ButtonVariant.COMPACT_GHOST, icon = IconId.Dice)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // The tiles span the sheet width to both margins (QA 7), the leftover going between the columns.
            val columns = GridFill.columns(maxWidth.value, 150f)
            val gap = GridFill.gap(maxWidth.value, 150f, columns).dp
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GridFill.rows(state.history.indices.toList(), columns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        row.forEach { i -> Candidate(state, vm, i, Modifier.size(150.dp, 104.dp), onChoose) }
                    }
                }
            }
        }
    }
}
