package io.github.lenticualrstone.civilight.ui.richtext

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKey
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.text.DocBlock
import io.github.lenticualrstone.civilight.model.text.DocBlockKind
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.isBlankText
import io.github.lenticualrstone.civilight.model.text.ordered

/**
 * Editing a [RichDoc] as one plain text without losing what it holds (shared editor for `긴 글`
 * values, 말투 / 비밀 prose, archive documents, world text and pages).
 *
 * The editor's text is the paragraphs joined by `\n`; each run shows its `shownText` (a `괄호 지문`
 * without its parentheses, a `Break` as `\n` inside its paragraph). An edit is the stretch between
 * the longest common prefix and suffix of the old and new text:
 * - runs outside it are kept as they are, so `@` links keep their `linkKey` and paragraphs keep their
 *   `key`, `kind`, `recorderSource` and `ext` (recorder `카드에 넣기` targets paragraph keys);
 * - an edit wholly inside one link or `괄호 지문` edits that run's text (a link emptied by it goes);
 * - otherwise a link, break or unknown run the edit touches is replaced as a whole, plain and
 *   `괄호 지문` runs are cut and keep their kind;
 * - inserted text is plain, or `괄호 지문` in paren mode; a new line starts a new paragraph, which
 *   takes the kind of the paragraph it was split from (a `⌁` memo stays a memo, without its source);
 *   a line typed on its own is a plain paragraph.
 *
 * No NFC here: the field's text and offsets must stay as typed; `CivilightJson` normalises on write.
 */
object RichDocEdit {
    /** The editor's text of [doc]: paragraphs (in order) joined by new lines. */
    fun textOf(doc: RichDoc): String = doc.ordered().joinToString("\n") { b -> b.runs.joinToString("") { it.shownText } }

    /** [doc] with its text replaced by [newText]; [parenMode] makes inserted text `괄호 지문`. */
    fun apply(doc: RichDoc, newText: String, newKey: () -> Key, parenMode: Boolean = false): RichDoc {
        val ordered = doc.ordered()
        val old = textOf(ordered)
        if (newText == old) return doc
        if (newText.isEmpty()) return emptyList()

        val atoms = atomsOf(ordered)
        var prefix = commonPrefix(old, newText)
        var suffix = commonSuffix(old, newText, prefix)
        val middleRaw = newText.substring(prefix, newText.length - suffix)
        val endRaw = old.length - suffix

        editInside(ordered, atoms, prefix, endRaw, middleRaw, parenMode)?.let { return it }

        // Widen the edited span to whole runs except inside plain and paren text.
        var end = endRaw
        var pos = 0
        for (atom in atoms) {
            val len = atom.text.length
            if (!atom.splittable && prefix > pos && prefix < pos + len) prefix = pos
            if (!atom.splittable && end > pos && end < pos + len) end = pos + len
            pos += len
        }
        suffix = old.length - end
        val middle = newText.substring(prefix, newText.length - suffix)

        val out = ArrayList<Atom>()
        pos = 0
        for (atom in atoms) {
            val from = pos
            val to = pos + atom.text.length
            pos = to
            when {
                // A zero-length run (an unknown run without text) at the edit point is kept by the
                // loop after the edit only, so it is never doubled.
                to < prefix || (to == prefix && from < to) -> out += atom
                from < prefix -> out += atom.slice(0, prefix - from)
            }
        }
        middle.split('\n').forEachIndexed { i, piece ->
            if (i > 0) out += Atom(null, NEW_PARAGRAPH, "\n", separator = true)
            if (piece.isNotEmpty()) out += Atom(if (parenMode) TextRun.Paren(piece) else TextRun.Plain(piece), NEW_PARAGRAPH, piece)
        }
        pos = 0
        for (atom in atoms) {
            val from = pos
            val to = pos + atom.text.length
            pos = to
            when {
                from >= end -> out += atom
                to > end -> out += atom.slice(end - from, atom.text.length)
            }
        }
        return rebuild(ordered, out, newKey)
    }

    /**
     * `@` 연결: [doc] with [link] inserted at [offset] of [textOf] (clamped). An offset inside another
     * link or an unknown run moves to its end, so those runs are never cut; plain and paren text split.
     * Returns the new document and the offset right after the inserted link (the caret).
     */
    fun insertLink(doc: RichDoc, offset: Int, link: TextRun.Link, newKey: () -> Key): Pair<RichDoc, Int> {
        val ordered = doc.ordered()
        val atoms = atomsOf(ordered)
        val total = atoms.sumOf { it.text.length }
        var at = offset.coerceIn(0, total)
        var pos = 0
        for (atom in atoms) {
            val len = atom.text.length
            if (!atom.splittable && !atom.separator && at > pos && at < pos + len) at = pos + len
            pos += len
        }
        val out = ArrayList<Atom>()
        var inserted = false
        pos = 0
        for (atom in atoms) {
            val len = atom.text.length
            val from = pos
            pos += len
            if (!inserted && at == from) {
                // At a separator the link ends the previous paragraph; elsewhere it goes before this atom.
                val paragraph = if (atom.separator) out.lastOrNull()?.paragraph ?: 0 else atom.paragraph
                out += Atom(link, paragraph, link.text)
                inserted = true
            } else if (!inserted && at > from && at < from + len) {
                out += atom.slice(0, at - from)
                out += Atom(link, atom.paragraph, link.text)
                out += atom.slice(at - from, len)
                inserted = true
                continue
            }
            out += atom
        }
        if (!inserted) out += Atom(link, atoms.lastOrNull()?.paragraph ?: if (ordered.isEmpty()) NEW_PARAGRAPH else 0, link.text)
        return rebuild(ordered, out, newKey) to (at + link.text.length)
    }

    /** What is saved: trailing empty paragraphs dropped; a document with no visible text is empty. */
    fun trimmed(doc: RichDoc): RichDoc {
        val ordered = doc.ordered()
        if (ordered.all { it.runs.isBlankText() }) return emptyList()
        var last = ordered.lastIndex
        while (last > 0 && ordered[last].runs.isEmpty() && ordered[last].recorderSource == null) last--
        return ordered.subList(0, last + 1)
    }

    /** The paragraph that holds text [offset] (a separator belongs to the paragraph it starts). */
    fun paragraphAt(doc: RichDoc, offset: Int): DocBlock? {
        val ordered = doc.ordered()
        if (ordered.isEmpty()) return null
        var pos = 0
        for ((i, block) in ordered.withIndex()) {
            val len = block.runs.sumOf { it.shownText.length }
            if (offset <= pos + len) return block
            pos += len + 1
            if (i == ordered.lastIndex) return block
        }
        return ordered.last()
    }

    /** Where every run and paragraph sits in [textOf], for drawing (links, parens, memo lines). */
    fun layout(doc: RichDoc): TextLayout {
        val runs = ArrayList<RunSpan>()
        val paragraphs = ArrayList<ParagraphSpan>()
        var pos = 0
        doc.ordered().forEachIndexed { i, block ->
            if (i > 0) pos += 1
            val start = pos
            for (run in block.runs) {
                val len = run.shownText.length
                runs += RunSpan(pos, pos + len, run)
                pos += len
            }
            paragraphs += ParagraphSpan(start, pos, block)
        }
        return TextLayout(runs, paragraphs)
    }

    // --- in-place edit of one link or paren run ---

    private fun editInside(doc: RichDoc, atoms: List<Atom>, start: Int, end: Int, middle: String, parenMode: Boolean): RichDoc? {
        if ('\n' in middle) return null
        var pos = 0
        for ((i, atom) in atoms.withIndex()) {
            val len = atom.text.length
            val from = pos
            pos += len
            val run = atom.run
            if (run !is TextRun.Link && run !is TextRun.Paren) continue
            // A pure insertion goes inside only strictly inside the run; at its edges it is new text
            // (paren mode typing at a paren's end continues that paren).
            val inside = if (start == end) {
                start > from && start < from + len || (parenMode && run is TextRun.Paren && start == from + len)
            } else {
                start >= from && end <= from + len && !(run is TextRun.Link && start == from && end == from + len)
            }
            if (!inside) continue
            val text = atom.text.substring(0, start - from) + middle + atom.text.substring(end - from)
            val edited: TextRun? = when (run) {
                is TextRun.Link -> text.takeIf { it.isNotEmpty() }?.let { run.copy(text = it) }
                is TextRun.Paren -> run.copy(text = text)
            }
            val replaced = atoms.toMutableList()
            if (edited == null) replaced.removeAt(i) else replaced[i] = atom.copy(run = edited, text = edited.shownText)
            return rebuild(doc, replaced, newKey = { error("no new paragraph in an in-run edit") })
        }
        return null
    }

    // --- atoms ---

    private const val NEW_PARAGRAPH = -1

    /** One run (or a paragraph separator) with the index of the paragraph it came from. */
    private data class Atom(val run: TextRun?, val paragraph: Int, val text: String, val separator: Boolean = false) {
        val splittable: Boolean get() = run is TextRun.Plain || run is TextRun.Paren

        fun slice(from: Int, to: Int): Atom {
            val piece = text.substring(from, to)
            val cut: TextRun = when (run) {
                is TextRun.Plain -> run.copy(text = piece)
                is TextRun.Paren -> run.copy(text = piece)
                else -> error("only plain and paren runs are cut")
            }
            return copy(run = cut, text = piece)
        }
    }

    private fun atomsOf(doc: RichDoc): List<Atom> = buildList {
        doc.forEachIndexed { i, block ->
            if (i > 0) add(Atom(null, i, "\n", separator = true))
            block.runs.forEach { add(Atom(it, i, it.shownText)) }
        }
    }

    private fun commonPrefix(a: String, b: String): Int {
        val max = minOf(a.length, b.length)
        var i = 0
        while (i < max && a[i] == b[i]) i++
        return i
    }

    private fun commonSuffix(a: String, b: String, prefix: Int): Int {
        val max = minOf(a.length, b.length) - prefix
        var i = 0
        while (i < max && a[a.length - 1 - i] == b[b.length - 1 - i]) i++
        return i
    }

    private fun rebuild(old: RichDoc, atoms: List<Atom>, newKey: () -> Key): RichDoc {
        val groups = ArrayList<MutableList<Atom>>().apply { add(ArrayList()) }
        for (atom in atoms) {
            if (atom.separator) groups += arrayListOf(atom) else groups.last() += atom
        }
        val used = HashSet<Int>()
        val origins: List<Int?> = groups.mapIndexed { i, group ->
            group.firstOrNull { it.paragraph != NEW_PARAGRAPH && !it.separator }?.paragraph
                ?: group.firstOrNull { it.separator && it.paragraph != NEW_PARAGRAPH }?.paragraph
                ?: if (i == 0 && old.isNotEmpty()) 0 else null
        }
        val sources: List<DocBlock?> = origins.map { origin -> if (origin != null && used.add(origin)) old[origin] else null }
        val orders = orderKeys(sources.map { it?.order })
        return groups.mapIndexed { i, group ->
            val runs = normalize(group.mapNotNull { it.run })
            val source = sources[i]
            // A paragraph split off another takes its kind (not its key or recorder source); a new line
            // typed on its own is a plain paragraph.
            source?.copy(order = orders[i], runs = runs)
                ?: DocBlock(newKey(), orders[i], (origins[i]?.let { old[it].kind } ?: DocBlockKind.PARAGRAPH.open()), runs)
        }
    }

    /** Keeps increasing original order keys and fills the gaps; rewrites all when they are out of order. */
    private fun orderKeys(kept: List<OrderKey?>): List<OrderKey> {
        val present = kept.filterNotNull()
        if (present.zipWithNext().any { (a, b) -> a >= b }) return OrderKeys.evenlySpaced(kept.size)
        val out = ArrayList<OrderKey>(kept.size)
        var i = 0
        while (i < kept.size) {
            val k = kept[i]
            if (k != null) {
                out += k
                i++
                continue
            }
            var j = i
            while (j < kept.size && kept[j] == null) j++
            out += OrderKeys.between(out.lastOrNull(), kept.getOrNull(j), j - i)
            i = j
        }
        return out
    }

    /** Merges neighbouring plain runs and neighbouring paren runs (no ext) and drops empty ones. */
    fun normalize(runs: List<TextRun>): List<TextRun> {
        val out = ArrayList<TextRun>(runs.size)
        for (run in runs) {
            when (run) {
                is TextRun.Plain -> {
                    if (run.text.isEmpty()) continue
                    val last = out.lastOrNull()
                    if (last is TextRun.Plain && last.ext == null && run.ext == null) out[out.lastIndex] = last.copy(text = last.text + run.text) else out += run
                }
                is TextRun.Paren -> {
                    if (run.text.isEmpty()) continue
                    val last = out.lastOrNull()
                    if (last is TextRun.Paren && last.ext == null && run.ext == null) out[out.lastIndex] = last.copy(text = last.text + run.text) else out += run
                }
                is TextRun.Link -> if (run.text.isNotEmpty()) out += run
                is TextRun.Break, is TextRun.Unknown -> out += run
            }
        }
        return out
    }
}

/** A run's place in the editor text. */
data class RunSpan(val start: Int, val end: Int, val run: TextRun)

/** A paragraph's place in the editor text (without its separator). */
data class ParagraphSpan(val start: Int, val end: Int, val block: DocBlock)

data class TextLayout(val runs: List<RunSpan>, val paragraphs: List<ParagraphSpan>)
