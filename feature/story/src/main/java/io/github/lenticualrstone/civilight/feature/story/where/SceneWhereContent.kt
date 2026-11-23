package io.github.lenticualrstone.civilight.feature.story.where

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.rememberLastNonNull
import io.github.lenticualrstone.civilight.domain.story.time.CascadeMode
import io.github.lenticualrstone.civilight.domain.story.travel.TravelSettings
import io.github.lenticualrstone.civilight.feature.story.check.TravelCard
import io.github.lenticualrstone.civilight.feature.story.menu.storyActions
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.navigation.PickOnMap
import io.github.lenticualrstone.civilight.navigation.SceneWhereSheet
import io.github.lenticualrstone.civilight.navigation.Timeline
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.picker.CharacterPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.EventPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.PlacePickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore

/**
 * `SceneWhereSheet` content (shared, owner STORY; STO-20 … STO-26, STO-11 rows, STO-27 and STO-30 rows). Opened
 * from the script's scene head and `시간` tab, the structure ⋯ and the bulk table. Every control
 * applies at once (one undo step each); a change that moves dependent scenes swaps the body to the
 * cascade preview first, and nothing changes until `바꾸기` (STO-23). The place list picker also
 * swaps in place, so the sheet keeps its state.
 */
@Composable
fun SheetScope.SceneWhereContent(sheet: SceneWhereSheet, deps: FeatureDeps) {
    val host = this
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val settings by deps.settings.collectAsState()
    val actions = storyActions(sheet.projectKey, deps)
    val travel = TravelSettings(checkTravel = settings.checkTravel, checkNoPlace = settings.checkNoPlace)
    val gate = rememberHiddenGate(sheet.projectKey)
    val locked = !gate.open
    val ui = remember(doc, sheet.sceneKey, store.readOnly, travel, locked) { WhereModel.build(doc, sheet.sceneKey, store.readOnly, travel, locked) }

    var pendingScene by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingTime by rememberSaveable { mutableStateOf<String?>(null) }
    var picking by rememberSaveable { mutableStateOf<String?>(null) }
    val mapRequest = rememberSaveable(sheet.sceneKey) { "story.where.${sheet.sceneKey}" }

    val currentDoc by rememberUpdatedState(doc)
    LaunchedEffect(mapRequest) {
        deps.results.results(mapRequest).collect {
            (deps.results.consume(mapRequest) as? PlaceRef)?.let { ref ->
                actions.apply(WhereCopy.UNDO_LABEL) { d, ctx -> WhereModel.setPlace(d, sheet.sceneKey, ref, ctx) }
            }
        }
    }

    if (ui == null) {
        SheetHeader(title = "", onClose = host::close)
        return
    }

    fun saveTime(sceneKey: String, time: SceneTime?) {
        if (store.readOnly) return
        when (val step = WhereModel.step(currentDoc, sceneKey, time)) {
            TimeStep.NoChange -> Unit
            is TimeStep.Direct -> actions.apply(WhereCopy.UNDO_LABEL) { d, ctx -> WhereModel.setTime(d, sceneKey, step.time, CascadeMode.WITH_FOLLOWING, ctx) }
            is TimeStep.Preview -> {
                pendingScene = sceneKey
                pendingTime = WhereModel.encode(step.time)
            }
        }
    }

    val cascadeScene = pendingScene
    val cascadeTime = WhereModel.decode(pendingTime)
    val cascadeStep = if (cascadeScene != null && cascadeTime != null) remember(doc, cascadeScene, cascadeTime) { WhereModel.step(doc, cascadeScene, cascadeTime) } else null
    val preview = cascadeStep as? TimeStep.Preview
    if (cascadeStep != null && preview == null) {
        // The project changed under the preview (nothing depends on the scene any more): back to the form.
        LaunchedEffect(cascadeStep) {
            if (cascadeStep is TimeStep.Direct && cascadeScene != null && cascadeTime != null) saveTime(cascadeScene, cascadeTime)
            pendingScene = null
            pendingTime = null
        }
    }
    val lastPreview = rememberLastNonNull(preview)

    // The form, the cascade preview and the pickers swap in place on the body kind (P3); never on the data,
    // so an edit inside the form does not cross-fade the sheet.
    ContentSwap(WhereBody.of(preview != null, picking), Modifier.fillMaxWidth()) { body ->
        Column(Modifier.fillMaxWidth()) {
            when (body) {
                WhereBody.CASCADE -> lastPreview?.let { shown ->
                    CascadeBody(shown.preview, store.readOnly, onCancel = { pendingScene = null; pendingTime = null }) { mode ->
                        val scene = cascadeScene
                        val time = cascadeTime
                        if (scene != null && time != null) actions.apply(WhereCopy.UNDO_LABEL) { d, ctx -> WhereModel.setTime(d, scene, time, mode, ctx) }
                        pendingScene = null
                        pendingTime = null
                    }
                }
                WhereBody.PICK_PLACES, WhereBody.PICK_REGIONS -> PlacePickerSheet(
                    doc = doc,
                    gate = gate,
                    title = WhereCopy.PICK_PLACE,
                    onPick = { ref ->
                        actions.apply(WhereCopy.UNDO_LABEL) { d, ctx -> WhereModel.setPlace(d, sheet.sceneKey, ref, ctx) }
                        picking = null
                    },
                    onClose = { picking = null },
                    selected = ui.place,
                    includeRegions = body == WhereBody.PICK_REGIONS,
                    creator = io.github.lenticualrstone.civilight.feature.story.common.storyPickerCreator(io.github.lenticualrstone.civilight.designsystem.component.CreateKind.PLACE),
                )
                WhereBody.PICK_EVENT -> EventPickerSheet(
                    doc = doc,
                    title = WhereCopy.ANCHOR_EVENT,
                    onPick = { eventKey ->
                        saveTime(sheet.sceneKey, FlashbackModel.next(currentDoc, sheet.sceneKey, FlashbackAction.Event(eventKey)))
                        picking = null
                    },
                    onClose = { picking = null },
                    selectedKey = ui.flashback?.eventKey,
                )
                WhereBody.PICK_CAST -> CharacterPickerSheet(
                    doc = doc,
                    gate = gate,
                    onPick = { key ->
                        actions.apply(WhereCopy.CAST) { d, ctx -> CastModel.add(d, sheet.sceneKey, key, ctx) }
                        picking = null
                    },
                    onClose = { picking = null },
                    title = WhereCopy.CAST,
                    sceneCast = ui.cast.inScene,
                    creator = io.github.lenticualrstone.civilight.feature.story.common.storyPickerCreator(io.github.lenticualrstone.civilight.designsystem.component.CreateKind.CHARACTER),
                )
                WhereBody.FORM -> {
                    SheetHeader(title = ui.sheet.title, onClose = host::close)
                    SheetScrollBody(Modifier.padding(bottom = 12.dp)) {
                        WherePlaceSection(
                            ui,
                            onPickList = { regions -> picking = if (regions) KEY_REGIONS else KEY_PLACES },
                            onPickMap = { deps.navigator.push(PickOnMap(sheet.projectKey, mapRequest, sheet.sceneKey)) },
                            onRestore = {
                                if (PlaceFix.needsAuth(currentDoc, sheet.sceneKey) && !gate.open) {
                                    gate.unlock()
                                } else {
                                    actions.restoreWithToast(WhereCopy.RESTORE) { d, ctx -> PlaceFix.restoreStep(d, sheet.sceneKey, ctx, gate.open) }
                                }
                            },
                            onKeep = { actions.apply(WhereCopy.KEEP) { d, ctx -> PlaceFix.keep(d, sheet.sceneKey, ctx) } },
                        )
                        WhereFlashbackRow(ui) { on -> saveTime(sheet.sceneKey, WhereModel.flashbackTime(on)) }
                        // 회상 form and 때 section swap on the kind of the scene (P3).
                        ContentSwap(ui.flashback != null, Modifier.fillMaxWidth()) { flashback ->
                            if (flashback) {
                                WhereFlashbackForm(
                                    ui,
                                    onAction = { action -> saveTime(sheet.sceneKey, FlashbackModel.next(currentDoc, sheet.sceneKey, action)) },
                                    onPickEvent = { picking = KEY_EVENT },
                                    onTimeline = { deps.navigator.push(Timeline(sheet.projectKey)) },
                                )
                            } else {
                                WhereTimeSection(
                                    ui,
                                    onAction = { action -> saveTime(sheet.sceneKey, WhenEdit.next(currentDoc, sheet.sceneKey, action)) },
                                    onRebase = { actions.apply(WhereCopy.UNDO_LABEL) { d, ctx -> WhereModel.rebaseToPrevious(d, sheet.sceneKey, ctx) } },
                                    onTimeline = { deps.navigator.push(Timeline(sheet.projectKey)) },
                                )
                            }
                        }
                        WhereCastRow(
                            ui,
                            onAdd = { picking = KEY_CAST },
                            onRemove = { key -> actions.apply(WhereCopy.CAST) { d, ctx -> CastModel.remove(d, sheet.sceneKey, key, ctx) } },
                            onRestore = { key ->
                                if (CastModel.needsAuth(currentDoc, key) && !gate.open) {
                                    gate.unlock()
                                } else {
                                    actions.restoreWithToast(WhereCopy.RESTORE) { d, ctx -> CastModel.restoreStep(d, key, ctx, gate.open) }
                                }
                            },
                            onKeep = { key -> actions.apply(WhereCopy.KEEP) { d, ctx -> CastModel.keep(d, sheet.sceneKey, key, ctx) } },
                        )
                        WhereTransitionSection(
                            ui,
                            onMode = { mode -> actions.apply(WhereCopy.TRANSITION) { d, ctx -> WhereModel.setTransitionMode(d, sheet.sceneKey, mode, ctx) } },
                            onDirectText = { text ->
                                actions.applyMerged(WhereCopy.TRANSITION, "story.transition.${sheet.sceneKey}") { d, ctx -> WhereModel.writeDirect(d, sheet.sceneKey, text, ctx) }
                            },
                        )
                        ui.sheet.travel.forEach { block ->
                            val title = block.title.split(" | ")
                            val modes = block.warning.modeChips.size
                            TravelCard(
                                title = title.first(),
                                tag = title.getOrNull(1),
                                routeLine = null,
                                warning = block.warning,
                                detail = block.warning.detail,
                                chips = if (store.readOnly) emptyList() else block.chips,
                                onChip = { i ->
                                    when {
                                        i == 0 -> saveTime(block.warning.timeChip.sceneKey, WhereModel.delayedTime(currentDoc, block.warning.timeChip.sceneKey, block.warning.timeChip.days))
                                        i <= modes -> {
                                            val modeKey = block.warning.modeChips[i - 1].modeKey
                                            actions.apply(WhereCopy.UNDO_LABEL) { d, ctx -> WhereModel.applyModeChip(d, block.warning, modeKey, ctx) }
                                        }
                                        else -> actions.apply(WhereCopy.MATCH) { d, ctx -> WhereModel.acknowledgeTravel(d, block.warning, ctx) }
                                    }
                                },
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

internal const val KEY_PLACES = "places"
internal const val KEY_REGIONS = "regions"
internal const val KEY_EVENT = "event"
internal const val KEY_CAST = "cast"
