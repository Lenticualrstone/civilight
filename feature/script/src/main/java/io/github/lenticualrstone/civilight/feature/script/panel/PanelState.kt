package io.github.lenticualrstone.civilight.feature.script.panel

import io.github.lenticualrstone.civilight.domain.script.SceneTimeSource
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.ScriptLayers
import io.github.lenticualrstone.civilight.model.script.StagePosition
import java.time.LocalDate
import java.time.ZoneId

/**
 * What the screen asks the panel to show (SCR-31): the tab, the scene under the header (the `시간`
 * tab's scene when no block is selected) and whether a panel or panel sheet is on screen at all,
 * so nothing is computed while it is closed.
 */
data class PanelRequest(val tab: PanelTab, val headerScene: Key?, val visible: Boolean)

/** One built tab of the panel (only the shown tab is computed). */
data class PanelContent(
    val tab: PanelTab,
    val selected: Key?,
    val time: TimeTabModel? = null,
    val ref: RefTabModel? = null,
    val props: PropsModel? = null,
    val mods: ModsTabModel? = null,
)

object PanelBuild {
    /** The shown tab's model for the selected block (selection following, SCR-31). */
    fun of(
        doc: ProjectDoc,
        tab: PanelTab,
        selected: Key?,
        headerScene: Key?,
        time: SceneTimeSource,
        layers: ScriptLayers,
        unlocked: Boolean,
        readOnly: Boolean,
        zone: ZoneId,
        today: LocalDate = LocalDate.now(zone),
        recorderLine: (sceneKey: Key) -> String? = { null },
    ): PanelContent = when (tab) {
        PanelTab.TIME -> PanelContent(tab, selected, time = TimeTab.of(doc, selected, headerScene, time, layers, unlocked, readOnly))
        PanelTab.REF -> PanelContent(tab, selected, ref = RefTab.of(doc, selected, unlocked))
        PanelTab.PROPS -> PanelContent(tab, selected, props = PropsModel.of(doc, selected, readOnly, unlocked, zone))
        PanelTab.MODS -> PanelContent(tab, selected, mods = ModsTab.of(doc, selected, headerScene, today, zone, readOnly, unlocked, recorderLine))
    }
}

/** What the panel's controls call (the side panel and the cover sheet share it). */
class PanelActions(
    val changeKind: (blockKey: Key) -> Unit,
    val changeSpeaker: (blockKey: Key) -> Unit,
    val restoreSpeaker: (characterKey: Key) -> Unit,
    /** `이대로 두기` on a deleted speaker's line (SCR-34, rule 20). */
    val keepSpeaker: (characterKey: Key, blockKey: Key) -> Unit,
    val setStage: (blockKey: Key, StagePosition) -> Unit,
    val setManualVoice: (blockKey: Key, String) -> Unit,
    val showTab: (PanelTab) -> Unit,
    val openSceneWhere: (sceneKey: Key) -> Unit,
    val openTravelChecks: () -> Unit,
    val selectBlock: (blockKey: Key) -> Unit,
    val editRecord: (RecordEdit) -> Unit,
    val showInMap: (characterKey: Key) -> Unit,
    /** `다른 블록에 잇기` (SCR-43) and the point line's `블록에 잇기` (SCR-42): the candidate sheet. */
    val relink: (recordKey: Key) -> Unit = {},
    /** `이대로 두기` on an orphaned record (SCR-43). */
    val keepRecord: (recordKey: Key) -> Unit = {},
    /** `기록 지우기` (SCR-43): the record goes to the trash. */
    val trashRecord: (recordKey: Key) -> Unit = {},
    /** `되살리기` on the trash note (SCR-39): the deleted original block comes back and its records re-attach. */
    val restoreBlock: (blockKey: Key) -> Unit = {},
    /** `◆ 변화 기록` (SCR-41) for the selected block. */
    val recordChange: (blockKey: Key) -> Unit = {},
    /** `씬 메모` typed in place (SCR-35): written after a pause, one undo step per pause. */
    val editMemo: (sceneKey: Key, text: String) -> Unit = { _, _ -> },
    /** `레코더 · 정리 안 됨 n ›` (LRC-24): the inbox for this scene. */
    val openRecorderInbox: (sceneKey: Key) -> Unit = {},
    /** The `점검` card (SCR-35 `[PROPOSED]` tap → check center). */
    val openChecks: () -> Unit = {},
    /** `블록 고르기` from `속성` (SCR-36 visible entry, `ED`): select mode with this block checked. */
    val selectMode: ((blockKey: Key) -> Unit)? = null,
    /** `출처 · 레코더 …` of a recorder memo (SCR-59): the record in the recorder. */
    val openRecord: (recordKey: Key) -> Unit = {},
    /** `되살리기` on a `대상 지워짐` record (SCR-57): the deleted item comes back. */
    val restoreSubject: (kind: io.github.lenticualrstone.civilight.model.text.LinkTargetKind?, itemKey: Key) -> Unit = { _, _ -> },
    /** `이대로 두기` on a `대상 지워짐` record (SCR-57). */
    val keepSubject: (itemKey: Key, recordKey: Key) -> Unit = { _, _ -> },
)
