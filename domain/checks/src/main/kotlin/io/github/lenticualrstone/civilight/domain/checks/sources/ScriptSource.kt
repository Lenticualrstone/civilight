package io.github.lenticualrstone.civilight.domain.checks.sources

import io.github.lenticualrstone.civilight.domain.cast.CastHiding
import io.github.lenticualrstone.civilight.domain.checks.CheckCopy
import io.github.lenticualrstone.civilight.domain.checks.CheckGroup
import io.github.lenticualrstone.civilight.domain.checks.CheckRegistry
import io.github.lenticualrstone.civilight.domain.checks.CheckResult
import io.github.lenticualrstone.civilight.domain.checks.FixAction
import io.github.lenticualrstone.civilight.domain.checks.FixKind
import io.github.lenticualrstone.civilight.domain.script.Anchors
import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.domain.script.Records
import io.github.lenticualrstone.civilight.domain.script.ScriptChecks
import io.github.lenticualrstone.civilight.domain.script.VoiceIds
import io.github.lenticualrstone.civilight.domain.story.checks.Fingerprints
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.AnchorState
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.ChoiceOption

/** Rows 4, 6, 7 and 8, computed from script data (SCR-62). */
internal object ScriptSource {
    /** `▲ 화자 없음` per non-empty dialogue block without a speaker (SCR-62 copy). */
    fun speakerMissing(scope: CheckScope): List<CheckResult> = ScriptChecks.speakerMissing(scope.doc).map { item ->
        val blockKey = item.subjectKeys.single()
        CheckResult(
            checkType = CheckType.SPEAKER_MISSING,
            subjectKeys = item.subjectKeys,
            fingerprint = Fingerprints.of(CheckType.SPEAKER_MISSING, blockKey),
            title = CheckCopy.SPEAKER_MISSING,
            location = item.details.getOrNull(2),
            body = item.details.getOrNull(0),
            detail = item.details.getOrNull(1),
            actions = listOf(
                FixAction(CheckCopy.PICK_SPEAKER, FixKind.PICK_SPEAKER, blockKey),
                FixAction(CheckCopy.TO_NARRATION, FixKind.TO_NARRATION, blockKey),
            ),
            storyOrder = scope.blockOrder(blockKey),
            lockedHidden = scope.hiding.block(blockKey),
        )
    }

    /**
     * `◇ 형식과 다른 ID` per fixed ID (block or spoken option) whose speaker or episode part no longer
     * matches the format; grouped as `◇ 형식과 다른 ID n`. Acknowledged ones are kept (they are
     * listed under `맞다고 표시한 것`), with `domain:script`'s fingerprint.
     */
    fun voiceFormatDiffers(scope: CheckScope): List<CheckResult> {
        val doc = scope.doc
        val group = CheckGroup(CheckType.VOICE_FORMAT_DIFFERS, CheckCopy.VOICE_FORMAT)
        val out = ArrayList<CheckResult>()
        for (b in doc.manuscript.liveBlocks) {
            if (VoiceIds.differsFromFormat(doc, b)) out += voiceFormatItem(scope, b, null, group)
            for (o in b.options) if (VoiceIds.differsFromFormat(doc, b, o)) out += voiceFormatItem(scope, b, o, group)
        }
        return out
    }

    private fun voiceFormatItem(scope: CheckScope, block: Block, option: ChoiceOption?, group: CheckGroup): CheckResult {
        val doc = scope.doc
        val fixed = (option?.voice ?: block.voice)?.fixedId
        val auto = if (option == null) VoiceIds.autoId(doc, block) else VoiceIds.autoIdOfOption(doc, block, option)
        val number = if (option == null) Labels.number(block.number)
        else Labels.optionNumber(block.number, (VoiceIds.optionIndex(block, option.key) ?: 0))
        return CheckResult(
            checkType = CheckType.VOICE_FORMAT_DIFFERS,
            subjectKeys = listOfNotNull(block.key, option?.key),
            fingerprint = ScriptChecks.voiceFormatFingerprint(doc, block.key, option?.key)!!,
            title = CheckCopy.VOICE_FORMAT,
            location = CheckCopy.join(Labels.sceneLocation(doc, block.sceneKey), number),
            body = "${fixed.orEmpty()} → ${auto ?: "—"}",
            detail = CheckCopy.VOICE_FORMAT_DETAIL,
            actions = listOf(
                FixAction(CheckCopy.NEW_VOICE_ID, FixKind.NEW_VOICE_ID, block.key, option?.key),
                FixAction(CheckRegistry.ackLabel(CheckType.VOICE_FORMAT_DIFFERS)!!, FixKind.ACKNOWLEDGE, block.key, option?.key),
            ),
            storyOrder = scope.blockOrder(block.key),
            lockedHidden = scope.hiding.block(block.key) || (option?.speakerKey ?: block.speakerKey)?.let(scope.hiding::character) == true,
            group = group,
        )
    }

    /** `▲ 보이스 ID 겹침 n` (`[PROPOSED]` in SCR-62): one row over every shared ID. */
    fun voiceDuplicates(scope: CheckScope): List<CheckResult> = ScriptChecks.voiceDuplicates(scope.doc).map { item ->
        val first = item.subjectKeys.first()
        val block = scope.doc.index.block(first)
        CheckResult(
            checkType = CheckType.VOICE_DUPLICATE,
            subjectKeys = item.subjectKeys,
            fingerprint = Fingerprints.of(CheckType.VOICE_DUPLICATE, item.subjectKeys, item.details),
            title = item.title,
            location = block?.let { Labels.sceneLocation(scope.doc, it.sceneKey) },
            body = item.details.joinToString(CheckCopy.SEP),
            actions = listOf(FixAction(CheckCopy.GO_TO_SCRIPT, FixKind.GO_TO_SCRIPT, first)),
            storyOrder = scope.blockOrder(first),
            lockedHidden = item.subjectKeys.all(scope.hiding::block),
        )
    }

    /**
     * `◇ 근거 지워진 변화` per live record flagged `근거 지워짐` (open) or kept with `이대로 두기`
     * (acknowledged while its keep matches `Anchors.fingerprintOf`); grouped as `◇ 근거 지워진 변화 n`.
     */
    fun anchorDeleted(scope: CheckScope): List<CheckResult> {
        val doc = scope.doc
        val group = CheckGroup(CheckType.CHANGE_ANCHOR_DELETED, CheckCopy.ANCHOR_DELETED)
        return doc.changes.records
            .filter { doc.liveness.isLive(it) && it.anchorState.known in FLAGGED }
            .map { record ->
                CheckResult(
                    checkType = CheckType.CHANGE_ANCHOR_DELETED,
                    subjectKeys = listOf(record.key),
                    fingerprint = Anchors.fingerprintOf(record),
                    title = CheckCopy.ANCHOR_DELETED,
                    location = anchorLocation(scope, record.anchor),
                    body = CheckCopy.join(recordLine(scope, record), CheckCopy.anchorGone(originalNumber(scope, record))),
                    actions = listOf(
                        FixAction(CheckCopy.RELINK_RECORD, FixKind.RELINK_RECORD, record.key),
                        FixAction(CheckCopy.KEEP, FixKind.ACKNOWLEDGE, record.key),
                        FixAction(CheckCopy.TRASH_RECORD, FixKind.TRASH_RECORD, record.key),
                    ),
                    storyOrder = doc.manuscript.rankOf(record.anchor).value.takeUnless { it == Long.MAX_VALUE },
                    lockedHidden = scope.hiding.locked && (CastHiding.isAboutHidden(doc, record) || anchorHidden(scope, record.anchor)),
                    group = group,
                )
            }
    }

    private val FLAGGED = setOf(AnchorState.ORPHANED, AnchorState.KEPT)

    /** `◆ 관계 리엔 → 하르트` / `◇ 첫 공개 오리지늄 등불`. */
    private fun recordLine(scope: CheckScope, record: ChangeRecord): String {
        val subject = Records.subject(scope.doc, record)
        return if (record.target is ChangeTarget.TermReveal) "◇ 첫 공개 $subject" else "◆ ${Records.kindLabel(scope.doc, record)} $subject"
    }

    private fun originalNumber(scope: CheckScope, record: ChangeRecord): String? {
        val original = record.originalAnchor as? Anchor.AtBlock ?: return null
        return scope.doc.index.block(original.blockKey)?.let { Labels.number(it.number) }
    }

    /** Where the record sits now: `1-7 씬 3 · #044`, a unit label, or null when unplaced. */
    private fun anchorLocation(scope: CheckScope, anchor: Anchor): String? = when (anchor) {
        is Anchor.AtBlock -> scope.doc.index.block(anchor.blockKey)?.let {
            CheckCopy.join(Labels.sceneLocation(scope.doc, it.sceneKey), Labels.number(it.number))
        }
        is Anchor.UnitStart -> scope.codes.unitLabel(anchor.unitKey)
        is Anchor.UnitEnd -> scope.codes.unitLabel(anchor.unitKey)
        else -> null
    }

    private fun anchorHidden(scope: CheckScope, anchor: Anchor): Boolean = when (anchor) {
        is Anchor.AtBlock -> scope.hiding.block(anchor.blockKey)
        is Anchor.UnitStart -> scope.hiding.scene(anchor.unitKey)
        is Anchor.UnitEnd -> scope.hiding.scene(anchor.unitKey)
        else -> false
    }
}
