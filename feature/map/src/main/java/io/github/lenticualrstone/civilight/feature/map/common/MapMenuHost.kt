package io.github.lenticualrstone.civilight.feature.map.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonRow
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.world.gen.RerollMode
import io.github.lenticualrstone.civilight.domain.world.gen.StampCheck
import io.github.lenticualrstone.civilight.domain.world.gen.StepChoices
import io.github.lenticualrstone.civilight.domain.world.gen.StepReroll
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.edit.NameContent
import io.github.lenticualrstone.civilight.feature.map.gen.nextSeed
import io.github.lenticualrstone.civilight.feature.map.view.MapMenuAction
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.GenerationStepKind
import io.github.lenticualrstone.civilight.model.map.MapFormat
import io.github.lenticualrstone.civilight.model.map.MapStyle
import io.github.lenticualrstone.civilight.navigation.CharacterTrack
import io.github.lenticualrstone.civilight.navigation.DetailStamps
import io.github.lenticualrstone.civilight.navigation.HexPaint
import io.github.lenticualrstone.civilight.navigation.PolygonRegions
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHost
import kotlinx.serialization.Serializable

/** `이름` of the map (WLD-36). */
@Serializable
data object MapNameSheet : SheetKey

/** `그림` of a generated map: `색면 · 먹선 · 색 · 양피지` and `등고선` (WLD-57). */
@Serializable
data object MapStyleSheet : SheetKey

/** `단계 다시 만들기`: one row per step (WLD-56). */
@Serializable
data object RerollStepsSheet : SheetKey

/** The guard `강 다시 만들기` of one step ([step] = `GenerationStepKind` name). */
@Serializable
data class RerollGuardSheet(val step: String) : SheetKey

/** A `위치 확인` row, saved with the sheet (the re-roll result itself is not stored). */
@Serializable
data class StampCheckRow(val stampKey: Key, val label: String, val wasX: Float, val wasY: Float, val nearX: Float? = null, val nearY: Float? = null) {
    companion object {
        fun of(c: StampCheck) = StampCheckRow(c.stampKey, c.label, c.was.x, c.was.y, c.nearest?.x, c.nearest?.y)
    }
}

/** `위치 확인` after `모두 새로`: the stamps that sat on the old layer. */
@Serializable
data class StampCheckSheet(val rows: List<StampCheckRow>) : SheetKey

/**
 * The map menu's actions and sheets, shared by 지도 보기 and the editors: rename, the format editor
 * routes, `단계 다시 만들기` with its guard sheet and `위치 확인`, `그림`, and `인물 동선`.
 */
class MapMenuHost(
    private val projectKey: Key,
    private val mapKey: Key,
    private val deps: FeatureDeps,
    private val vm: MapScreenViewModel,
    private val sheets: SheetHost,
) {
    fun onRow(action: MapMenuAction, doc: ProjectDoc?) {
        val map = doc?.maps?.get(mapKey)
        when (action) {
            MapMenuAction.RENAME -> sheets.open(MapNameSheet)
            MapMenuAction.FORMAT_EDIT -> when (map?.format?.known) {
                MapFormat.HEX -> deps.navigator.push(HexPaint(projectKey, mapKey))
                MapFormat.POLY -> deps.navigator.push(PolygonRegions(projectKey, mapKey))
                MapFormat.DETAIL -> deps.navigator.push(DetailStamps(projectKey, mapKey))
                else -> Unit
            }
            MapMenuAction.REROLL -> sheets.open(RerollStepsSheet)
            MapMenuAction.TRACK -> deps.navigator.push(CharacterTrack(projectKey, mapKey = mapKey))
            MapMenuAction.DELETE -> vm.deleteMap(mapKey) { deps.navigator.pop() }
        }
    }

    private fun reroll(step: GenerationStepKind, mode: RerollMode) {
        vm.reroll(mapKey, step, mode, nextSeed()) { checks ->
            if (checks.isNotEmpty()) sheets.open(StampCheckSheet(checks.map(StampCheckRow::of)))
        }
    }

    /** The content of this host's sheets, or null for other keys. */
    fun content(key: SheetKey, doc: ProjectDoc?, readOnly: Boolean): (@Composable SheetScope.() -> Unit)? {
        val map = doc?.maps?.get(mapKey) ?: return null
        return when (key) {
            MapNameSheet -> { { NameContent(map.name, onDone = { name -> close(); vm.renameMap(mapKey, name) }, onClose = ::close) } }
            MapStyleSheet -> { { StyleContent(map.style?.known ?: MapStyle.SOFT, map.contours, map.format?.known == MapFormat.DETAIL, readOnly, { s, c -> vm.setStyle(mapKey, s, c) }, ::close) } }
            RerollStepsSheet -> {
                {
                    val sheet = this
                    SheetHeader(MapCopy.REROLL_TITLE, onClose = ::close)
                    SheetScrollBody {
                        StepChoices.ORDER.forEach { step ->
                            ListRow(MapCopy.rerollStep(StepChoices.stepName(step)), onClick = if (readOnly) null else ({
                                if (StepReroll.sheet(map, step) == null) {
                                    sheet.close()
                                    reroll(step, RerollMode.KEEP_EDITED)
                                } else {
                                    sheet.open(RerollGuardSheet(step.name))
                                }
                            }))
                        }
                    }
                }
            }
            is RerollGuardSheet -> {
                val step = runCatching { GenerationStepKind.valueOf(key.step) }.getOrNull() ?: return null
                val guard = StepReroll.sheet(map, step) ?: return null
                {
                    val sheet = this
                    var mode by rememberSaveable { mutableIntStateOf(guard.options.indexOfFirst { it.first == guard.defaultMode }.coerceAtLeast(0)) }
                    SheetHeader(guard.title, onClose = ::close)
                    SheetScrollBody {
                        CivilightText(guard.line, CivilightTheme.type.label, Modifier.padding(top = 4.dp, bottom = 8.dp))
                        guard.options.forEachIndexed { i, (_, label) ->
                            RadioRow(label.substringBefore(MapCopy.SEP), mode == i, { mode = i }, subtitle = label.substringAfter(MapCopy.SEP, "").ifEmpty { null })
                        }
                        CivilightText(guard.note, CivilightTheme.type.small, Modifier.padding(top = 8.dp), color = CivilightTheme.colors.ink3)
                    }
                    SheetActions {
                        CivilightButton(guard.buttons[0], sheet::close, Modifier.weight(1f), variant = ButtonVariant.GHOST)
                        CivilightButton(guard.buttons[1], {
                            sheet.close()
                            reroll(step, guard.options[mode].first)
                        }, Modifier.weight(1f), enabled = !readOnly)
                    }
                }
            }
            is StampCheckSheet -> {
                {
                    val sheet = this
                    var done by rememberSaveable { mutableStateOf(listOf<String>()) }
                    fun finish(stampKey: String) {
                        done = done + stampKey
                        if (key.rows.all { it.stampKey in done }) sheet.close()
                    }
                    SheetHeader(MapCopy.CHECK_POSITIONS, onClose = ::close)
                    SheetScrollBody {
                        key.rows.filter { it.stampKey !in done }.forEach { row ->
                            Column(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                CivilightText(row.label, CivilightTheme.type.itemTitle, maxLines = 1)
                                ButtonRow {
                                    CivilightButton(MapCopy.BTN_KEEP_SPOT, { finish(row.stampKey) }, Modifier.weight(1f), variant = ButtonVariant.GHOST)
                                    CivilightButton(MapCopy.BTN_NEW_SPOT, {
                                        val x = row.nearX
                                        val y = row.nearY
                                        if (x != null && y != null) vm.moveStampTo(mapKey, row.stampKey, Point(x, y))
                                        finish(row.stampKey)
                                    }, Modifier.weight(1f), enabled = row.nearX != null && !readOnly)
                                }
                            }
                        }
                    }
                }
            }
            else -> null
        }
    }
}

@Composable
fun rememberMapMenuHost(projectKey: Key, mapKey: Key, deps: FeatureDeps, vm: MapScreenViewModel, sheets: SheetHost): MapMenuHost =
    remember(projectKey, mapKey, vm, sheets) { MapMenuHost(projectKey, mapKey, deps, vm, sheets) }

/** `그림`: the four styles and, on detailed terrain, `등고선`. Each choice is one step. */
@Composable
private fun SheetScope.StyleContent(style: MapStyle, contours: Boolean, canContour: Boolean, readOnly: Boolean, onSet: (MapStyle, Boolean) -> Unit, onClose: () -> Unit) {
    SheetHeader(MapCopy.STYLE, onClose = onClose)
    SheetScrollBody {
        MapStyle.entries.forEach { s ->
            RadioRow(MapCopy.STYLE_NAMES[s.ordinal], s == style, { if (!readOnly) onSet(s, contours) }, enabled = !readOnly)
        }
        FoldReveal(canContour) { SwitchRow(MapCopy.CONTOURS, contours, { onSet(style, it) }, enabled = !readOnly) }
    }
}
