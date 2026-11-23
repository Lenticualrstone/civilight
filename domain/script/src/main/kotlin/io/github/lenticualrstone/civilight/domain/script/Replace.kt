package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.TextRun
import java.text.Normalizer

/**
 * (0.2.0, N-28, SCR-75) Find and replace in the manuscript. Range (D19): runs of every live block, choice
 * option text and direction text lines; not cues, titles or profiles. A hit lies inside one run's text.
 */
object Replace {
    /**
     * One match. [optionKey] null = the block's own runs. [runIndex] and [start] / [end] are in that run list
     * and run text. [following] is the text right after the match in the same run (for `조사 확인`).
     */
    data class Hit(
        val blockKey: Key,
        val optionKey: Key?,
        val runIndex: Int,
        val start: Int,
        val end: Int,
        val sentence: String,
        val sentenceStart: Int,
        val following: String,
    ) {
        /** A stable id for checks in the UI. */
        val id: String get() = "$blockKey/${optionKey.orEmpty()}/$runIndex/$start"
    }

    fun nfc(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFC)

    /**
     * Hits in manuscript order; blocks of a locked hidden scene are left out ([unlocked] false). The text of a
     * link to a locked hidden item is never searched, and [Hit.sentence] shows it as `숨긴 인물` / `숨긴 항목`
     * (SCR-55): a typed hidden name neither shows nor confirms it.
     */
    fun find(doc: ProjectDoc, query: String, unlocked: Boolean = false): List<Hit> {
        val q = nfc(query)
        if (q.isEmpty()) return emptyList()
        val out = mutableListOf<Hit>()
        for (scene in doc.manuscript.liveScenes) {
            if (scene.hidden && !unlocked) continue
            for (b in doc.manuscript.liveBlocksOf(scene.key)) {
                out += hitsIn(doc, unlocked, b.key, null, b.runs, q)
                for (o in BlockOps.orderedOptions(b)) out += hitsIn(doc, unlocked, b.key, o.key, o.runs, q)
            }
        }
        return out
    }

    private fun hitsIn(doc: ProjectDoc, unlocked: Boolean, blockKey: Key, optionKey: Key?, runs: List<TextRun>, q: String): List<Hit> {
        val out = mutableListOf<Hit>()
        val shown = Runs.masked(doc, runs, unlocked)
        val whole = shown.joinToString("") { it.shownText }
        var prefix = 0
        runs.forEachIndexed { i, run ->
            val before = prefix
            prefix += shown[i].shownText.length
            if (lockedLink(doc, run, unlocked)) return@forEachIndexed
            val text = textOf(run) ?: return@forEachIndexed
            var from = 0
            while (true) {
                val at = text.indexOf(q, from)
                if (at < 0) break
                out += Hit(blockKey, optionKey, i, at, at + q.length, whole, before + at, text.substring(at + q.length))
                from = at + q.length
            }
        }
        return out
    }

    private fun lockedLink(doc: ProjectDoc, run: TextRun, unlocked: Boolean): Boolean =
        !unlocked && run is TextRun.Link && Links.state(doc, run, unlocked) is Links.State.Hidden

    private fun textOf(run: TextRun): String? = when (run) {
        is TextRun.Plain -> run.text
        is TextRun.Paren -> run.text
        is TextRun.Link -> run.text
        else -> null
    }

    /**
     * Replaces exactly [hits] with [with] (one store update = one undo step). A link keeps its target and key;
     * keys, numbers, records, voice IDs and speakers are untouched. A hit whose text no longer matches
     * [query] is skipped.
     */
    fun apply(doc: ProjectDoc, hits: Collection<Hit>, query: String, with: String, ctx: EditContext, unlocked: Boolean = false): ProjectDoc =
        applyCounted(doc, hits, query, with, ctx, unlocked).doc

    /** The document after a replace and how many places really changed (the `{k}곳 바꿈` toast). */
    data class Applied(val doc: ProjectDoc, val count: Int)

    /**
     * As [apply], counting the places that changed. A link to a locked hidden item ([unlocked] false) is never
     * rewritten.
     */
    fun applyCounted(doc: ProjectDoc, hits: Collection<Hit>, query: String, with: String, ctx: EditContext, unlocked: Boolean = false): Applied {
        val q = nfc(query)
        val w = nfc(with)
        var count = 0
        var out = doc
        for ((key, group) in hits.groupBy { it.blockKey to it.optionKey }) {
            val (blockKey, optionKey) = key
            val block = out.index.block(blockKey)?.takeIf { out.manuscript.isLiveBlock(it.key) } ?: continue
            fun edit(runs: List<TextRun>): List<TextRun> {
                val list = runs.toMutableList()
                for ((runIndex, inRun) in group.groupBy { it.runIndex }) {
                    var run = list.getOrNull(runIndex) ?: continue
                    if (lockedLink(out, run, unlocked)) continue
                    for (h in inRun.sortedByDescending { it.start }) {
                        val text = textOf(run) ?: break
                        if (h.end > text.length || text.substring(h.start, h.end) != q) continue
                        count++
                        val next = text.substring(0, h.start) + w + text.substring(h.end)
                        run = when (run) {
                            is TextRun.Plain -> run.copy(text = next)
                            is TextRun.Paren -> run.copy(text = next)
                            is TextRun.Link -> run.copy(text = next)
                            else -> run
                        }
                    }
                    list[runIndex] = run
                }
                return list
            }
            val updated = if (optionKey == null) {
                block.copy(runs = edit(block.runs))
            } else {
                block.copy(options = block.options.map { if (it.key == optionKey) it.copy(runs = edit(it.runs)) else it })
            }
            if (updated != block) out = out.replaceBlock(updated.copy(updatedAt = ctx.now, updatedBy = ctx.device))
        }
        return Applied(out, count)
    }

    /** [hit] still lies on [query] in [doc] (a screen drops its kept list when not, e.g. after `되돌리기`). */
    fun matches(doc: ProjectDoc, hit: Hit, query: String): Boolean {
        val q = nfc(query)
        val block = doc.index.block(hit.blockKey)?.takeIf { doc.manuscript.isLiveBlock(it.key) } ?: return false
        val runs = if (hit.optionKey == null) block.runs else block.options.firstOrNull { it.key == hit.optionKey }?.runs ?: return false
        val text = runs.getOrNull(hit.runIndex)?.let(::textOf) ?: return false
        return hit.end <= text.length && text.substring(hit.start, hit.end) == q
    }

    /**
     * The list [shown] after the hits at [replacedIndices] were replaced by [with]: same size and order, so a
     * screen keeps its checks by index. Later hits in the same run and sentence move by the length change, so
     * a second `바꾸기` hits the right places; a replaced hit now spans [with]; sentences show the new text.
     */
    fun remap(shown: List<Hit>, replacedIndices: Set<Int>, query: String, with: String): List<Hit> {
        val q = nfc(query)
        val w = nfc(with)
        val delta = w.length - q.length
        val replaced = shown.withIndex().filter { it.index in replacedIndices }.map { it.value }
        if (replaced.isEmpty()) return shown
        val byGroup = replaced.groupBy { it.blockKey to it.optionKey }
        return shown.mapIndexed { i, h ->
            val group = byGroup[h.blockKey to h.optionKey] ?: return@mapIndexed h
            val sentence = group.sortedByDescending { it.sentenceStart }.fold(h.sentence) { acc, r ->
                val e = r.sentenceStart + q.length
                if (e > acc.length) acc else acc.substring(0, r.sentenceStart) + w + acc.substring(e)
            }
            val sentenceStart = h.sentenceStart + delta * group.count { it.sentenceStart < h.sentenceStart }
            val sameRun = group.filter { it.runIndex == h.runIndex }
            val start = h.start + delta * sameRun.count { it.start < h.start }
            val following = sameRun.filter { it.start >= h.end }.sortedByDescending { it.start }.fold(h.following) { acc, r ->
                val at = r.start - h.end
                if (at + q.length > acc.length) acc else acc.substring(0, at) + w + acc.substring(at + q.length)
            }
            val length = if (i in replacedIndices) w.length else h.end - h.start
            h.copy(start = start, end = start + length, sentence = sentence, sentenceStart = sentenceStart, following = following)
        }
    }
}
