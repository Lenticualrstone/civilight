package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.version.EpisodeVersion
import io.github.lenticualrstone.civilight.model.version.VersionBlock

/**
 * (0.2.0, N-29, SCR-76) The compare rows of a version against the live episode. Blocks are matched by key in
 * current manuscript order; a removed block follows the nearest earlier matched block of the version. Text of
 * a locked hidden scene never appears: such a scene is one row.
 */
object VersionDiff {
    enum class SegKind { SAME, INSERTED, DELETED }

    data class Seg(val text: String, val kind: SegKind)

    sealed interface Row {
        data class Same(val blockKey: Key, val number: Int, val text: String) : Row

        /** [segments] of the current text: inserted or changed characters boxed, deleted spans struck. */
        data class Changed(val blockKey: Key, val number: Int, val segments: List<Seg>, val versionBlock: VersionBlock) : Row

        data class Deleted(val blockKey: Key, val number: Int, val text: String) : Row

        data class Added(val blockKey: Key, val number: Int, val text: String) : Row

        /** `숨긴 씬 · 블록 {count} · 지문으로 보기`. */
        data class HiddenScene(val sceneKey: Key, val count: Int) : Row
    }

    fun rows(version: EpisodeVersion, doc: ProjectDoc, unitKey: Key, unlocked: Boolean = false): List<Row> {
        val scenes = doc.manuscript.liveScenes.filter { doc.numberingUnitOfScene(it.key) == unitKey }
        val current = scenes.flatMap { doc.manuscript.liveBlocksOf(it.key) }
        val byKey = version.blocks.associateBy { it.key }
        val currentKeys = current.map { it.key }.toSet()
        fun lockedScene(key: Key?): Boolean = key != null && !unlocked && doc.index.unit(key)?.hidden == true
        // Removed blocks: after the nearest earlier version block that is still live (or first).
        val removedAfter = linkedMapOf<Key?, MutableList<VersionBlock>>()
        var lastMatched: Key? = null
        for (vb in version.blocks) {
            if (vb.key in currentKeys) lastMatched = vb.key else removedAfter.getOrPut(lastMatched) { mutableListOf() } += vb
        }
        val ordered = mutableListOf<Pair<VersionBlock?, Block?>>()
        removedAfter[null]?.forEach { ordered += it to null }
        for (b in current) {
            ordered += byKey[b.key] to b
            removedAfter[b.key]?.forEach { ordered += it to null }
        }
        val out = mutableListOf<Row>()
        val hiddenCounts = linkedMapOf<Key, Int>()
        val hiddenRowAt = mutableMapOf<Key, Int>()
        for ((vb, b) in ordered) {
            // A version block no longer in this episode is checked where it sits now too (moved into a locked
            // hidden scene of another episode, live or in the trash), not only where it was kept (T-U14-25).
            val nowAt = b?.sceneKey ?: vb?.let { doc.index.block(it.key)?.sceneKey }
            val hiddenKey = listOfNotNull(nowAt, vb?.sceneKey).firstOrNull { lockedScene(it) }
            if (hiddenKey != null) {
                hiddenCounts[hiddenKey] = (hiddenCounts[hiddenKey] ?: 0) + 1
                if (hiddenKey !in hiddenRowAt) {
                    hiddenRowAt[hiddenKey] = out.size
                    out += Row.HiddenScene(hiddenKey, 0)
                }
                continue
            }
            // SCR-55: links to locked hidden items read `숨긴 인물` / `숨긴 항목`; Same vs Changed is decided on the real text.
            out += when {
                b == null -> Row.Deleted(vb!!.key, vb.number, textOf(vb, doc, unlocked))
                vb == null -> Row.Added(b.key, b.number, textOf(b, doc, unlocked))
                else -> {
                    if (textOf(vb) == textOf(b)) Row.Same(b.key, b.number, textOf(b, doc, unlocked))
                    else Row.Changed(b.key, b.number, diff(textOf(vb, doc, unlocked), textOf(b, doc, unlocked)), vb)
                }
            }
        }
        for ((key, at) in hiddenRowAt) out[at] = Row.HiddenScene(key, hiddenCounts[key] ?: 0)
        return out
    }

    /** The compared text: runs joined (break = line break); a choice's option lines joined by line breaks. */
    fun textOf(b: Block): String = if (b.kind.known == BlockKind.CHOICE) {
        BlockOps.orderedOptions(b).joinToString("\n") { join(it.runs) }
    } else join(b.runs)

    fun textOf(v: VersionBlock): String = if (v.kind.known == BlockKind.CHOICE) {
        v.options.joinToString("\n") { join(it.runs) }
    } else join(v.runs)

    /** [textOf] as shown: links to locked hidden items masked ([unlocked] false, SCR-55). */
    fun textOf(b: Block, doc: ProjectDoc, unlocked: Boolean): String = if (b.kind.known == BlockKind.CHOICE) {
        BlockOps.orderedOptions(b).joinToString("\n") { join(Runs.masked(doc, it.runs, unlocked)) }
    } else join(Runs.masked(doc, b.runs, unlocked))

    fun textOf(v: VersionBlock, doc: ProjectDoc, unlocked: Boolean): String = if (v.kind.known == BlockKind.CHOICE) {
        v.options.joinToString("\n") { join(Runs.masked(doc, it.runs, unlocked)) }
    } else join(Runs.masked(doc, v.runs, unlocked))

    private fun join(runs: List<TextRun>): String = runs.joinToString("") { r ->
        when (r) {
            is TextRun.Paren -> "(${r.text})"
            else -> r.shownText
        }
    }

    /** Character diff (LCS over code points): the current text with inserted spans and deleted spans in place. */
    fun diff(old: String, now: String): List<Seg> {
        val a = old.codePoints().toArray()
        val b = now.codePoints().toArray()
        if (a.size.toLong() * b.size > MAX_CELLS) {
            return listOfNotNull(Seg(old, SegKind.DELETED).takeIf { old.isNotEmpty() }, Seg(now, SegKind.INSERTED).takeIf { now.isNotEmpty() })
        }
        val dp = Array(a.size + 1) { IntArray(b.size + 1) }
        for (i in a.indices.reversed()) for (j in b.indices.reversed()) {
            dp[i][j] = if (a[i] == b[j]) dp[i + 1][j + 1] + 1 else maxOf(dp[i + 1][j], dp[i][j + 1])
        }
        val segs = mutableListOf<Seg>()
        fun push(cp: Int, kind: SegKind) {
            val s = String(Character.toChars(cp))
            val last = segs.lastOrNull()
            if (last != null && last.kind == kind) segs[segs.lastIndex] = last.copy(text = last.text + s) else segs += Seg(s, kind)
        }
        var i = 0
        var j = 0
        while (i < a.size && j < b.size) {
            when {
                a[i] == b[j] -> { push(b[j], SegKind.SAME); i++; j++ }
                dp[i + 1][j] >= dp[i][j + 1] -> { push(a[i], SegKind.DELETED); i++ }
                else -> { push(b[j], SegKind.INSERTED); j++ }
            }
        }
        while (i < a.size) { push(a[i], SegKind.DELETED); i++ }
        while (j < b.size) { push(b[j], SegKind.INSERTED); j++ }
        return segs
    }

    private const val MAX_CELLS = 4_000_000L
}
