package io.github.lenticualrstone.civilight.feature.story.check

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.CharacterActions
import io.github.lenticualrstone.civilight.domain.trash.Messages
import io.github.lenticualrstone.civilight.domain.trash.RestoreOps
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.domain.trash.TrashGates
import io.github.lenticualrstone.civilight.domain.checks.CheckAcks
import io.github.lenticualrstone.civilight.domain.checks.CheckInputs
import io.github.lenticualrstone.civilight.domain.checks.CheckResult
import io.github.lenticualrstone.civilight.domain.checks.FixAction
import io.github.lenticualrstone.civilight.domain.checks.FixKind
import io.github.lenticualrstone.civilight.domain.checks.WritingChecks
import io.github.lenticualrstone.civilight.domain.script.Anchors
import io.github.lenticualrstone.civilight.domain.script.BlockOps
import io.github.lenticualrstone.civilight.domain.script.VoiceIds
import io.github.lenticualrstone.civilight.domain.story.checks.Fingerprints
import io.github.lenticualrstone.civilight.domain.story.structure.UnitSettingsEdit
import io.github.lenticualrstone.civilight.domain.story.transition.Transitions
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.RevealEdit
import io.github.lenticualrstone.civilight.domain.world.term.TermAccess
import io.github.lenticualrstone.civilight.domain.world.term.TermCandidate
import io.github.lenticualrstone.civilight.domain.world.term.TermEdits
import io.github.lenticualrstone.civilight.domain.world.term.TermQueries
import io.github.lenticualrstone.civilight.feature.story.common.RestoreStep
import io.github.lenticualrstone.civilight.feature.story.where.WhereModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.ProfileTab
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.SceneWhereSheet
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.navigation.TermCard
import io.github.lenticualrstone.civilight.feature.story.where.CascadeSheet
import io.github.lenticualrstone.civilight.domain.world.common.EditContext as WorldEditContext
import io.github.lenticualrstone.civilight.domain.script.EditContext as ScriptEditContext

/** What one row button does in 점검 센터. */
sealed interface FixEffect {
    /** Open a screen; [detail] = the list | detail pair's right pane on inner screens (`TwoCheck`). */
    data class Navigate(val route: Route, val detail: Boolean) : FixEffect

    /** One undo step named [label], recomputed on the doc it is applied to. */
    data class Edit(val label: String, val transform: (ProjectDoc, ActionContext) -> ProjectDoc) : FixEffect

    data class OpenSheet(val sheet: SheetKey) : FixEffect

    /**
     * `되살리기` as one undo step whose toast carries `되돌리기` (as the sheet's): [step] returns what came
     * back, or null when nothing did on the doc it is applied to.
     */
    data class Restore(val label: String, val step: (ProjectDoc, ActionContext) -> RestoreStep?) : FixEffect

    /** `화자 고르기`: the character picker, then the speaker of [blockKey]. */
    data class PickSpeaker(val blockKey: Key) : FixEffect

    /**
     * `한꺼번에 ›` (row 13): 장소 · 때 한꺼번에 with [sceneKeys] already chosen. The route carries no scenes,
     * so the screen hands them over through `deps.results` (`BulkModel.preselectKey`) before pushing.
     */
    data class OpenBulk(val sceneKeys: List<Key>) : FixEffect

    /** `지문으로 보기`: the biometric prompt opens the project's hidden items. */
    data object Unlock : FixEffect

    /** No effect this module can run: the button is not drawn (listed under Requests). */
    data object Unavailable : FixEffect
}

/**
 * Routes every [FixKind] of the STO-40 registry to its owner's function or screen (one exhaustive
 * `when`, no default). Edits go through the owning module's write path (DECISIONS §1): `맞음` ·
 * `이대로 두기` · `되돌리기` through `domain:checks` [CheckAcks], blocks and records through
 * `domain:script`, terms through `domain:world`, time and transitions through `domain:story`.
 */
object CheckFixes {
    fun effect(doc: ProjectDoc, projectKey: Key, result: CheckResult, action: FixAction, inputs: CheckInputs, members: List<CheckResult> = listOf(result)): FixEffect {
        val target = action.target
        return when (action.kind) {
            FixKind.GO_TO_SCRIPT -> target?.let { FixEffect.Navigate(scriptAt(doc, projectKey, it), detail = true) } ?: FixEffect.Unavailable
            FixKind.MOVE_REVEAL -> target?.let { FixEffect.Navigate(TermCard(projectKey, it), detail = true) } ?: FixEffect.Unavailable
            FixKind.ACKNOWLEDGE -> FixEffect.Edit(action.label) { d, ctx -> acknowledge(d, inputs, result, ctx) }
            FixKind.REVERT -> target?.let { ack -> FixEffect.Edit(action.label) { d, ctx -> CheckAcks.revert(d, ack, ctx.edit) } } ?: FixEffect.Unavailable
            FixKind.LINK_CANDIDATE -> FixEffect.Edit(action.label) { d, ctx -> candidate(d, result)?.let { TermEdits.link(d, it, ctx.world)?.doc } ?: d }
            FixKind.REJECT_CANDIDATE -> FixEffect.Edit(action.label) { d, ctx -> candidate(d, result)?.let { TermEdits.reject(d, it, ctx.world) } ?: d }
            FixKind.SET_REVEAL -> target?.let { term -> FixEffect.Edit(action.label) { d, ctx -> setSuggestedReveal(d, term, ctx) } } ?: FixEffect.Unavailable
            FixKind.PICK_SPEAKER -> target?.let { FixEffect.PickSpeaker(it) } ?: FixEffect.Unavailable
            FixKind.TO_NARRATION -> target?.let { block ->
                FixEffect.Edit(action.label) { d, ctx -> if (d.index.block(block) != null) BlockOps.changeKind(d, block, BlockKind.NARRATION, ctx.script) else d }
            } ?: FixEffect.Unavailable
            FixKind.WRITE_VOICE -> result.subjectKeys.firstOrNull()?.let { FixEffect.Navigate(CharacterProfile(projectKey, it, ProfileTab.GAME_TEXT), detail = true) } ?: FixEffect.Unavailable
            FixKind.NEW_VOICE_ID -> target?.let { block ->
                FixEffect.Edit(action.label) { d, ctx -> if (d.index.block(block) != null) VoiceIds.replaceWithNew(d, block, action.option, ctx.script) else d }
            } ?: FixEffect.Unavailable
            FixKind.TRASH_RECORD -> target?.let { record -> FixEffect.Edit(action.label) { d, ctx -> trashRecords(d, listOf(record), ctx) } } ?: FixEffect.Unavailable
            FixKind.TRASH_ALL_RECORDS -> FixEffect.Edit(action.label) { d, ctx -> trashRecords(d, members.mapNotNull { it.subjectKeys.singleOrNull() }, ctx) }
            FixKind.DELAY_SCENE -> {
                val scene = target
                val days = action.days
                val delayed = if (scene == null || days == null) null else WhereModel.delayedTime(doc, scene, days)
                if (delayed == null) FixEffect.Unavailable else FixEffect.OpenSheet(CascadeSheet(projectKey, scene!!, WhereModel.encode(delayed)))
            }
            FixKind.USE_MODE -> {
                val warning = result.travel
                val mode = action.modeKey
                if (warning == null || mode == null) FixEffect.Unavailable else FixEffect.Edit(action.label) { d, ctx -> WhereModel.applyModeChip(d, warning, mode, ctx) }
            }
            FixKind.OPEN_BULK -> FixEffect.OpenBulk(result.subjectKeys.filter { WhereModel.isLiveScene(doc, it) })
            // S-3 hides the map's distance tools ("hidden means hidden everywhere"), so `축척 고치기` is not
            // drawn; restore `Navigate(MapScale(projectKey, it), detail = false)` only if the map lane keeps
            // MapScale reachable under S-3 (Requests).
            FixKind.FIX_SCALE -> FixEffect.Unavailable
            FixKind.TO_AUTO -> target?.let { scene -> FixEffect.Edit(action.label) { d, ctx -> if (WhereModel.isLiveScene(d, scene)) Transitions.toAuto(d, scene, ctx.edit) else d } } ?: FixEffect.Unavailable
            FixKind.EDIT_TRANSITION, FixKind.WRITE_DIRECT -> target?.let { FixEffect.OpenSheet(SceneWhereSheet(projectKey, it)) } ?: FixEffect.Unavailable
            FixKind.HIDE_SCENE -> target?.let { scene -> FixEffect.Edit(action.label) { d, ctx -> UnitSettingsEdit.setHidden(d, scene, true, ctx.edit) } } ?: FixEffect.Unavailable
            FixKind.UNLOCK -> FixEffect.Unlock
            FixKind.RESTORE -> target?.let { item -> restoreEffect(doc, item, action.label, inputs.hiddenUnlocked) } ?: FixEffect.Unavailable
            // Another lane's UI with no route or shared sheet yet (Requests): `다른 블록에 잇기` needs a
            // block picker, `화자 한꺼번에 바꾸기` cast's bulk replace.
            FixKind.RELINK_RECORD, FixKind.BULK_REPLACE -> FixEffect.Unavailable
        }
    }

    /**
     * `되살리기` on a `◇ 지운 …` row (row 9, R20): a character through `domain:actions`
     * `CharacterActions.restore` (DECISIONS §1), any other item with its own trash entry through
     * `domain:trash` `RestoreOps.restore`. A hidden one asks `지문으로 보기` first (rule 19 gate).
     */
    fun restoreEffect(doc: ProjectDoc, itemKey: Key, label: String, hiddenUnlocked: Boolean): FixEffect {
        val character = doc.index.character(itemKey)
        val entryKey = character?.trashEntryKey ?: doc.index.place(itemKey)?.trashEntryKey ?: doc.index.term(itemKey)?.trashEntryKey
            ?: doc.index.faction(itemKey)?.trashEntryKey ?: return FixEffect.Unavailable
        val entry = doc.index.trashEntry(entryKey) ?: return FixEffect.Unavailable
        if (TrashGates.needsAuth(doc, entry) && !hiddenUnlocked) return FixEffect.Unlock
        return FixEffect.Restore(label) { d, ctx -> restoreStep(d, itemKey, ctx, hiddenUnlocked) }
    }

    /** The restore itself, recomputed on [doc]: the character's own toast, else the trash toast. */
    fun restoreStep(doc: ProjectDoc, itemKey: Key, ctx: ActionContext, hiddenUnlocked: Boolean): RestoreStep? {
        if (doc.index.character(itemKey) != null) {
            val r = CharacterActions.restore(doc, itemKey, ctx, hiddenUnlocked)
            val restored = r.outcome as? RestoreOutcome.Restored ?: return null
            return RestoreStep(restored.doc, restored.change, r.toast)
        }
        val entryKey = doc.index.place(itemKey)?.trashEntryKey ?: doc.index.term(itemKey)?.trashEntryKey
            ?: doc.index.faction(itemKey)?.trashEntryKey ?: return null
        val restored = RestoreOps.restore(doc, entryKey, ctx.trash, hiddenUnlocked) as? RestoreOutcome.Restored ?: return null
        return RestoreStep(restored.doc, restored.change, Messages.restoreToast(restored.report))
    }

    /** The buttons drawn for a row: only those with an effect here. */
    fun shown(
        doc: ProjectDoc,
        projectKey: Key,
        result: CheckResult,
        actions: List<FixAction>,
        inputs: CheckInputs,
        members: List<CheckResult> = listOf(result),
        readOnly: Boolean = false,
    ): List<Pair<FixAction, FixEffect>> =
        actions.map { it to effect(doc, projectKey, result, it, inputs, members) }
            .filter { (_, e) -> e != FixEffect.Unavailable && !(readOnly && e.edits) }

    /** Effects that change the project (not drawn in a read-only project such as the example, LIB-17). */
    private val FixEffect.edits: Boolean
        get() = this is FixEffect.Edit || this is FixEffect.Restore || this is FixEffect.OpenSheet || this is FixEffect.PickSpeaker

    /** `Script` at a block (`#043`, selected) or at a scene head, whichever [key] is. */
    fun scriptAt(doc: ProjectDoc, projectKey: Key, key: Key): Script =
        if (doc.index.block(key) != null) Script(projectKey, blockKey = key) else Script(projectKey, sceneKey = key)

    /** `맞음` · `그대로` · `이대로 두기` on the item as it fires now (the fingerprint of the current doc). */
    fun acknowledge(doc: ProjectDoc, inputs: CheckInputs, result: CheckResult, ctx: ActionContext): ProjectDoc {
        val now = current(doc, inputs, result) ?: return doc
        return CheckAcks.acknowledge(doc, now, ctx.edit)
    }

    /** The same item (type and subjects) in [doc], or null once it no longer fires (or its kind is gated, S-2). */
    fun current(doc: ProjectDoc, inputs: CheckInputs, result: CheckResult): CheckResult? =
        WritingChecks.check(doc, inputs, result.checkType).firstOrNull { it.subjectKeys == result.subjectKeys }

    /** The `◇ 공개 전 후보` candidate a row stands for: same term, spot and fingerprint. */
    fun candidate(doc: ProjectDoc, result: CheckResult): TermCandidate? {
        if (result.checkType != CheckType.TERM_CANDIDATE_BEFORE_REVEAL) return null
        val index = CandidateIndex.build(doc, TermAccess(hiddenUnlocked = true))
        return index.all().firstOrNull { c ->
            c.termKey == result.subjectKeys.firstOrNull() &&
                Fingerprints.of(CheckType.TERM_CANDIDATE_BEFORE_REVEAL, result.subjectKeys, c.start, c.end, c.matchedText) == result.fingerprint
        }
    }

    /** `<위치>을 첫 공개로` (WLD-18): the reveal at the term's first link, as `domain:world` suggests it. */
    fun setSuggestedReveal(doc: ProjectDoc, termKey: Key, ctx: ActionContext): ProjectDoc {
        val queries = TermQueries(doc, CandidateIndex.build(doc, TermAccess(hiddenUnlocked = true)), TermAccess(hiddenUnlocked = true))
        val target = queries.revealSuggestion(termKey)?.suggestion?.target ?: return doc
        return (TermEdits.setReveal(doc, termKey, target, ctx.world) as? RevealEdit.Applied)?.doc ?: doc
    }

    private fun trashRecords(doc: ProjectDoc, records: List<Key>, ctx: ActionContext): ProjectDoc =
        records.fold(doc) { d, key -> if (d.index.record(key) != null) Anchors.trashRecord(d, key, ctx.script) else d }

    private val ActionContext.script: ScriptEditContext get() = ScriptEditContext(now = now, zone = zone, device = device, newKey = newKey)

    private val ActionContext.world: WorldEditContext get() = WorldEditContext(newKey = newKey, now = { now }, device = device)
}
