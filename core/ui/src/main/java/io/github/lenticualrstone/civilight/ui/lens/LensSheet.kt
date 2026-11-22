package io.github.lenticualrstone.civilight.ui.lens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.LensChip
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.component.selectedRowBackground
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.store.project.ProjectStore
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.labels.UnitLabels

/**
 * The lens chip of a lens screen's top bar (STO-13): `최신 · 1-7` or `시점 · 1-5` with the `acc`
 * outline. Tapping opens the lens sheet ([io.github.lenticualrstone.civilight.navigation.LensSheet]).
 */
@Composable
fun ProjectLensChip(store: ProjectStore, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val doc by store.doc.collectAsState()
    val label = remember(doc) { LensRules.chipLabel(doc, doc.app.view.lens.point) }
    LensChip(label.text, label.state, onClick, modifier)
}

/** The lens chip's label for [doc] (for screens that build their own top bar). */
fun lensChipLabel(doc: ProjectDoc): LensChipLabel = LensRules.chipLabel(doc, doc.app.view.lens.point)

/**
 * 시점 렌즈 sheet content (STO-13, board `Lens`): rows `최신` and episode points, `보는 중` on the
 * viewed one, `더 자세히` neighbour chips and `씬 단위로 고르기`, then `최신으로 돌아가기` ·
 * `이 시점으로 보기`. Tapping a row only highlights it; [onApply] receives the chosen point.
 */
@Composable
fun ColumnScope.LensSheetContent(
    doc: ProjectDoc,
    viewed: LensPoint,
    onApply: (LensPoint) -> Unit,
    onClose: () -> Unit,
) {
    val labels = remember(doc) { UnitLabels(doc) }
    val rowsModel = remember(doc, labels) { LensRows(doc, labels) }
    // The highlighted point is saved as a unit key ("" = 최신).
    var highlightedKey by rememberSaveable { mutableStateOf((LensRules.effective(doc, viewed) as? LensPoint.UnitEnd)?.unitKey.orEmpty()) }
    var byScene by rememberSaveable { mutableStateOf(false) }
    val highlighted: LensPoint = if (highlightedKey.isEmpty()) LensPoint.Latest else LensPoint.UnitEnd(highlightedKey)
    val rows = remember(doc, viewed) { rowsModel.rows(viewed) }
    val viewedEffective = LensRules.effective(doc, viewed)
    val highlightedUnit = (highlighted as? LensPoint.UnitEnd)?.unitKey?.let(doc.index::unit)
    val highlightedEpisode = highlightedUnit?.let { u -> rowsModel.episodes.firstOrNull { it.key == u.key } ?: labels.episodeOf(u) }

    SheetHeader(title = LensCopy.SHEET_TITLE, onClose = onClose, subtitle = LensCopy.SHEET_SUB)
    SheetScrollBody(Modifier.padding(top = 12.dp)) {
        for (row in rows) {
            LensRowItem(row, highlighted = row.point == highlighted, viewing = row.point == viewedEffective) {
                highlightedKey = (row.point as? LensPoint.UnitEnd)?.unitKey.orEmpty()
            }
        }
        // S18d: the neighbour chips and the scene rows fold open and closed; the episode shown while folding
        // away is the last one highlighted.
        FoldRevealValue(highlightedEpisode) { episode ->
            Column {
                SectionLabel(LensCopy.MORE, Modifier.padding(top = 16.dp))
                ChipFlow(Modifier.padding(top = 8.dp)) {
                    for (neighbour in rowsModel.neighbours(episode.key)) {
                        FilterChip(
                            label = labels.code(neighbour) ?: LensCopy.UNTITLED,
                            selected = neighbour.key == highlightedKey,
                            onClick = { highlightedKey = neighbour.key },
                        )
                    }
                    FilterChip(label = LensCopy.BY_SCENE, selected = byScene, onClick = { byScene = !byScene })
                }
                FoldReveal(byScene) {
                    Column(Modifier.padding(top = 8.dp)) {
                        for (row in rowsModel.sceneRows(episode.key)) {
                            LensRowItem(row, highlighted = row.point == highlighted, viewing = row.point == viewedEffective) {
                                highlightedKey = (row.point as? LensPoint.UnitEnd)?.unitKey.orEmpty()
                            }
                        }
                    }
                }
            }
        }
    }
    SheetActions {
        CivilightButton(LensCopy.BACK_TO_LATEST, onClick = { onApply(LensPoint.Latest) }, Modifier.weight(1f), variant = ButtonVariant.GHOST)
        CivilightButton(LensCopy.APPLY, onClick = { onApply(highlighted) }, Modifier.weight(1f))
    }
}

@Composable
private fun LensRowItem(row: LensRow, highlighted: Boolean, viewing: Boolean, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(
        Modifier
            .fillMaxWidth()
            .selectedRowBackground(highlighted, colors.sel, colors.acc)
            .pressable(onClick = onClick, role = Role.RadioButton, scaleOnPress = false)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CivilightText(row.code, type.number, color = if (row.notWritten) colors.ink3 else colors.ink, maxLines = 1)
            Spacer(Modifier.weight(1f))
            if (viewing) CivilightText(LensCopy.VIEWING, type.small, color = colors.acc, maxLines = 1)
        }
        CivilightText(row.sub, type.small, color = if (row.notWritten) colors.ink3 else colors.ink2, maxLines = 1)
    }
}

/** The shell's registration of [io.github.lenticualrstone.civilight.navigation.LensSheet]. */
@Composable
fun SheetScope.LensSheetFor(store: ProjectStore, controller: LensController) {
    val doc by store.doc.collectAsState()
    LensSheetContent(
        doc = doc,
        viewed = doc.app.view.lens.point,
        onApply = { point ->
            controller.set(point)
            close()
        },
        onClose = ::close,
    )
}

/** `최신으로` on the lens edit banner (STO-15) and similar text actions. */
@Composable
fun LatestAction(controller: LensController, label: String = LATEST_ACTION) = TextAction(label, { controller.backToLatest() })

/** STO-15 banner action copy. */
const val LATEST_ACTION = "최신으로"
