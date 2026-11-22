package io.github.lenticualrstone.civilight.domain.recorder.link

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun

/**
 * One chip or tag of a link row. Several masked links of one project collapse into one chip
 * ([links] then holds all of them, [PROPOSED] LRC-22). [prefixed] = the first chip of a capture
 * screen (`연결 · `). [removable] = shows ×: false for `@` chips (they leave with their text) and for
 * plain-text (purged) labels; × on a collapsed masked chip removes all its links (`ED`).
 */
data class Chip(
    val links: List<RecorderLink>,
    val display: LinkDisplay,
    val label: String,
    val prefixed: Boolean,
    val atForm: Boolean,
    val removable: Boolean,
    val dotted: Boolean,
    val diamond: Boolean,
    val plain: Boolean,
) {
    /** The printed text: `연결 · 1-7 씬 3`, `세라 @`, `R-18`. */
    val text: String
        get() = buildString {
            if (prefixed) append(RecorderCopy.LINK_PREFIX)
            append(label)
            if (atForm) append(' ').append(RecorderCopy.AT_MARK)
        }
}

/** Chip rows (LRC-21). */
object Chips {
    /**
     * Capture screens and the record screen: every link as a chip in creation order, the first
     * prefixed `연결 · `; masked links of one project collapse. The `+ 연결` chip
     * ([RecorderCopy.ADD_LINK]) follows and is not part of this list.
     */
    fun captureRow(links: List<RecorderLink>, runs: List<TextRun>, env: RecorderEnv, record: RecorderRecord? = null): List<Chip> {
        val chips = collapse(LinkEdits.ordered(links).mapNotNull { LinkViews.view(it, env, runs, record) })
        return chips.mapIndexed { i, chip -> if (i == 0) chip.copy(prefixed = true) else chip }
    }

    /**
     * Inbox rows: labels only, no ×, no prefix. A project link is omitted when the record also
     * links something inside that project ([PROPOSED] LRC-21, §6 Q24). Empty for a hidden record
     * while hidden records are locked.
     */
    fun inboxTags(record: RecorderRecord, links: List<RecorderLink>, env: RecorderEnv): List<Chip> {
        val ordered = LinkEdits.ordered(links)
        val innerProjects = ordered.filter { it.targetKind.known != RecorderTargetKind.PROJECT }.map { it.projectKey }.toSet()
        val shown = ordered.filter { !(it.targetKind.known == RecorderTargetKind.PROJECT && it.projectKey in innerProjects) }
        return collapse(shown.mapNotNull { LinkViews.view(it, env, record.runs, record) })
            .map { it.copy(removable = false, atForm = false) }
    }

    private fun collapse(views: List<LinkView>): List<Chip> {
        val out = mutableListOf<Chip>()
        val maskedIndex = HashMap<String, Int>()
        for (view in views) {
            if (LinkViews.isMasked(view)) {
                val at = maskedIndex[view.link.projectKey]
                if (at != null) {
                    val chip = out[at]
                    out[at] = chip.copy(links = chip.links + view.link, removable = chip.removable && !view.atForm)
                    continue
                }
                maskedIndex[view.link.projectKey] = out.size
                out += Chip(
                    listOf(view.link), view.display, view.label, prefixed = false, atForm = false,
                    removable = !view.atForm, dotted = false, diamond = false, plain = false,
                )
                continue
            }
            out += Chip(
                listOf(view.link), view.display, view.label, prefixed = false, atForm = view.atForm,
                removable = !view.atForm && !view.plain, dotted = view.dotted, diamond = view.diamond, plain = view.plain,
            )
        }
        return out
    }
}
