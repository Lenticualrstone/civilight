package io.github.lenticualrstone.civilight.domain.recorder.hooks

import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.link.recorderTarget
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.text.TextRun

/**
 * Rule 21 does not apply to Life Recorder text (LRC-17, rule 21 §5 table `라이프 레코더 · 아니오`):
 * records are never scanned for `후보` or `▲ 공개 전 사용`. Only `@` links count as uses of a term.
 */
object RecorderTermUse {
    /** Recorder text is never a source of candidates or pre-reveal warnings. */
    const val SCANNED_FOR_CANDIDATES = false

    /** Records (not in the trash) whose text holds an `@` link to [term]; plain words never count. */
    fun recordsUsing(library: RecorderLibrary, term: TargetRef): List<Key> = library.records
        .filter { r -> r.deletedAt == null && r.runs.any { it is TextRun.Link && it.recorderTarget == term } }
        .map { it.key }
}
