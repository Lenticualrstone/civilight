package io.github.lenticualrstone.civilight.feature.script.records

import io.github.lenticualrstone.civilight.domain.script.Anchors
import io.github.lenticualrstone.civilight.domain.script.Records
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.AnchorState
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** One candidate of `다른 블록에 잇기` / `블록에 잇기`: `하르트 #043` over the block's text. */
data class RelinkRow(val blockKey: Key, val line: BlockLine)

/** The candidate sheet (SCR-43, SCR-42): title, the record it re-anchors, the rows. */
data class RelinkModel(val title: String, val subject: String, val rows: List<RelinkRow>)

object Relink {
    /**
     * Candidates of [recordKey] (SCR-43, Q-21 default from `domain:script` `Anchors.relinkCandidates`):
     * the block right before the current anchor first, then the scene's other blocks in order; for a
     * point-seat record the blocks of its unit's first scene. Blocks of a locked hidden scene are left out.
     */
    fun of(doc: ProjectDoc, recordKey: Key, unlocked: Boolean): RelinkModel? {
        val record = doc.index.record(recordKey)?.takeIf { it.deletedAt == null } ?: return null
        val point = record.anchor !is Anchor.AtBlock
        val rows = Anchors.relinkCandidates(doc, recordKey)
            .filter { unlocked || doc.index.unit(it.sceneKey)?.hidden != true }
            .map { RelinkRow(it.key, BlockPreview.of(doc, it, unlocked)) }
        return RelinkModel(
            title = if (point) ScriptCopy.LINK_TO_BLOCK else ScriptCopy.RELINK,
            subject = Records.shortTitle(doc, record),
            rows = rows,
        )
    }

    /** The orphan card's buttons show while the record is flagged `근거 지워짐` (not after `이대로 두기`). */
    fun isOrphan(doc: ProjectDoc, recordKey: Key): Boolean =
        doc.index.record(recordKey)?.anchorState?.known == AnchorState.ORPHANED
}
