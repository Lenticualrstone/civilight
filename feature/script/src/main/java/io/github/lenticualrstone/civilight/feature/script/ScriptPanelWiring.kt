package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.feature.script.panel.PanelActions
import io.github.lenticualrstone.civilight.feature.script.panel.RecordEdit
import io.github.lenticualrstone.civilight.feature.script.records.RelinkSheetKey
import io.github.lenticualrstone.civilight.feature.script.ui.KindSheetKey
import io.github.lenticualrstone.civilight.feature.script.ui.SpeakerSheetKey
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.CheckCenter
import io.github.lenticualrstone.civilight.navigation.CheckCenterFilter
import io.github.lenticualrstone.civilight.navigation.FieldValueEditSheet
import io.github.lenticualrstone.civilight.navigation.RelationEditSheet
import io.github.lenticualrstone.civilight.navigation.RecorderInbox
import io.github.lenticualrstone.civilight.navigation.RelationMap
import io.github.lenticualrstone.civilight.navigation.SceneWhereSheet
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.navigation.RecorderRecordDetail
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps

/**
 * The panel's controls wired to the screen (SCR-32 … SCR-34). [open] opens a sheet over the panel
 * (the screen's host for the side panel, the sheet scope for the cover sheet); [leave] runs before a
 * route is pushed (the cover sheet closes first). [authenticated]: the hidden-item gate is open.
 */
internal fun scriptPanelActions(
    route: Script,
    deps: FeatureDeps,
    vm: ScriptViewModel,
    authenticated: Boolean,
    open: (SheetKey) -> Unit,
    leave: () -> Unit,
): PanelActions {
    val p = route.projectKey
    fun push(to: io.github.lenticualrstone.civilight.navigation.Route) {
        leave()
        deps.navigator.push(to)
    }
    return PanelActions(
        changeKind = { block -> if (!vm.readOnly) open(KindSheetKey(p, block)) },
        changeSpeaker = { block -> if (!vm.readOnly) vm.speakerTargetOf(block)?.let { open(SpeakerSheetKey.of(p, it)) } },
        restoreSpeaker = { character -> vm.restoreSpeaker(character, authenticated) },
        keepSpeaker = vm::keepSpeaker,
        setStage = vm::setStage,
        setManualVoice = vm::setManualVoice,
        showTab = vm::setPanelTab,
        openSceneWhere = { scene -> open(SceneWhereSheet(p, scene)) },
        openTravelChecks = { push(CheckCenter(p, CheckCenterFilter.TRAVEL)) },
        selectBlock = vm::selectAndShow,
        editRecord = { edit ->
            when (edit) {
                is RecordEdit.Relation -> open(RelationEditSheet(p, relationKey = edit.relationKey))
                is RecordEdit.Field -> open(FieldValueEditSheet(p, edit.kind, edit.entityKey, edit.fieldDefKey))
                is RecordEdit.Profile -> push(CharacterProfile(p, edit.characterKey))
            }
        },
        showInMap = { character -> push(RelationMap(p, character)) },
        relink = { record -> if (!vm.readOnly) open(RelinkSheetKey(p, record)) },
        keepRecord = vm::keepRecord,
        trashRecord = vm::trashRecord,
        restoreBlock = { block -> vm.restoreDeletedBlock(block, authenticated) },
        recordChange = { block -> vm.changeSheetFor(block)?.let(open) },
        editMemo = vm::editSceneMemo,
        openRecorderInbox = { scene -> push(RecorderInbox(projectKey = p, targetKind = RECORDER_TARGET_SCENE, targetKey = scene)) },
        openChecks = { push(CheckCenter(p)) },
        openRecord = { record -> push(RecorderRecordDetail(record)) },
        restoreSubject = { kind, item -> vm.restoreLinkTarget(kind, item, authenticated) },
        keepSubject = vm::keepSubject,
        selectMode = { block ->
            leave()
            vm.selectBlock(block)
            vm.content.value?.rows?.let(vm::enterSelect)
        },
    )
}

/** `RecorderInbox.targetKind` / `Recording.targetKind` of a scene (the recorder lane's route contract). */
internal const val RECORDER_TARGET_SCENE = "scene"
