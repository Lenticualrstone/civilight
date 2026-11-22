package io.github.lenticualrstone.civilight.feature.recorder.link

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.picker.CandidateGroup
import io.github.lenticualrstone.civilight.domain.recorder.picker.LinkCandidate
import io.github.lenticualrstone.civilight.domain.recorder.picker.LinkPicker
import io.github.lenticualrstone.civilight.domain.recorder.picker.PickerMode
import io.github.lenticualrstone.civilight.feature.recorder.RecorderUiCopy
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind

/** One row of the link picker: [title] (`하르트`, `1-7 씬 3`), [sub] (`잿빛 새벽의 등불 · 인물`), [linked] drawn checked. */
data class LinkPickerRow(val candidate: LinkCandidate, val title: String, val sub: String, val linked: Boolean)

/** A titled group of rows; only `글에 나온 이름` has a title (rule 23 §5). */
data class LinkPickerSection(val title: String?, val rows: List<LinkPickerRow>)

/** Rows of the link picker (LRC-20) and of the memo's `@` list (LRC-17) from `domain:recorder` `LinkPicker`. */
object LinkPickerRows {
    /** The `+ 연결` / chip picker: `글에 나온 이름` first, then the ranked rest in one list. */
    fun sections(env: RecorderEnv, query: String, links: List<RecorderLink>, recordText: String?): List<LinkPickerSection> {
        val candidates = LinkPicker.search(env, query, links, PickerMode.PICKER, recordText)
        val (inText, rest) = candidates.partition { it.group == CandidateGroup.IN_TEXT }
        return listOfNotNull(
            inText.takeIf { it.isNotEmpty() }?.let { LinkPickerSection(RecorderCopy.IN_TEXT_GROUP, it.map(::row)) },
            rest.takeIf { it.isNotEmpty() }?.let { LinkPickerSection(null, it.map(::row)) },
        )
    }

    /**
     * A candidate as a row. A project row prints the project's label as the chips do (`R-18` when
     * `라이프 레코더 연결 가리기` is on, LRC-22); a unit's label already names its level (`1-7 씬 3`),
     * so its sub line is the project only; items add their kind word.
     */
    fun row(candidate: LinkCandidate): LinkPickerRow {
        val kind = candidate.target.kind
        val title = if (kind == RecorderTargetKind.PROJECT) candidate.projectLabel else candidate.label
        val sub = when (kind) {
            RecorderTargetKind.PROJECT -> RecorderUiCopy.KIND_PROJECT
            RecorderTargetKind.UNIT -> candidate.projectLabel
            else -> "${candidate.projectLabel}${RecorderCopy.SEP}${kindWord(kind)}"
        }
        return LinkPickerRow(candidate, title, sub, candidate.linked)
    }

    fun kindWord(kind: RecorderTargetKind): String = when (kind) {
        RecorderTargetKind.PROJECT -> RecorderUiCopy.KIND_PROJECT
        RecorderTargetKind.CHARACTER -> RecorderUiCopy.KIND_CHARACTER
        RecorderTargetKind.PLACE -> RecorderUiCopy.KIND_PLACE
        RecorderTargetKind.TERM -> RecorderUiCopy.KIND_TERM
        RecorderTargetKind.FACTION -> RecorderUiCopy.KIND_FACTION
        RecorderTargetKind.PAGE -> RecorderUiCopy.KIND_PAGE
        RecorderTargetKind.UNIT -> ""
    }
}
