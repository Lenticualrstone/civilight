package io.github.lenticualrstone.civilight.feature.world.common

import io.github.lenticualrstone.civilight.designsystem.text.DecorationRange
import io.github.lenticualrstone.civilight.designsystem.text.InlineDecoration
import io.github.lenticualrstone.civilight.domain.script.Runs
import io.github.lenticualrstone.civilight.domain.world.item.RefLabel
import io.github.lenticualrstone.civilight.domain.world.item.WorldRefs
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKey
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.text.DocBlock
import io.github.lenticualrstone.civilight.model.text.DocBlockKind
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.ordered
import java.text.Normalizer

/** One paragraph of card text as drawn: the text and its link decorations (DS-19). */
data class ParagraphUi(val text: String, val decorations: List<DecorationRange>)

/**
 * Card text (`설명`) for display (WLD-17): `@` links keep their written text with the link shape;
 * a link to a hidden item while locked reads `숨긴 항목` (the stored text is never altered); links to
 * trashed or purged items are dotted (rule 20). `괄호 지문` is drawn in parentheses.
 */
object CardText {
    fun paragraphs(doc: ProjectDoc, text: RichDoc, hiddenOpen: Boolean): List<ParagraphUi> =
        text.ordered().map { block -> paragraph(doc, block.runs, hiddenOpen) }.filter { it.text.isNotBlank() }

    /** True when [text] links to an item that is hidden while locked (its editor needs `지문으로 보기`). */
    fun hasLockedLinks(doc: ProjectDoc, text: RichDoc, hiddenOpen: Boolean): Boolean =
        !hiddenOpen && text.any { b -> b.runs.any { it is TextRun.Link && linkLabel(doc, it, false) is RefLabel.Hidden } }

    private fun paragraph(doc: ProjectDoc, runs: List<TextRun>, hiddenOpen: Boolean): ParagraphUi {
        val out = StringBuilder()
        val decorations = ArrayList<DecorationRange>()
        for (run in runs) {
            when (run) {
                is TextRun.Link -> {
                    val label = linkLabel(doc, run, hiddenOpen)
                    val (shown, kind) = when (label) {
                        is RefLabel.Hidden -> WorldCopy.HIDDEN_ITEM to InlineDecoration.HIDDEN_LINK
                        is RefLabel.Deleted, is RefLabel.Purged -> run.text to InlineDecoration.DELETED_REF
                        else -> run.text to InlineDecoration.LINK
                    }
                    val start = out.length
                    out.append(shown)
                    decorations += DecorationRange(start, out.length, kind)
                }
                is TextRun.Paren -> out.append('(').append(run.text).append(')')
                else -> out.append(run.shownText)
            }
        }
        return ParagraphUi(out.toString(), decorations)
    }

    private fun linkLabel(doc: ProjectDoc, run: TextRun.Link, hiddenOpen: Boolean): RefLabel? =
        WorldRefs.link(doc, run, hiddenOpen) ?: when (run.target.kind.known) {
            LinkTargetKind.CHARACTER -> doc.index.character(run.target.key)?.let { c ->
                when {
                    c.hidden && !hiddenOpen -> RefLabel.Hidden()
                    c.deletedAt != null -> RefLabel.Deleted(run.text, c.name, emptyList())
                    else -> RefLabel.Live(run.text)
                }
            }
            else -> null
        }
}

/**
 * Editing card text as plain text without losing what the text holds (WLD-17): the edited span is
 * the stretch between the longest common prefix and suffix of the old and new text. Runs outside
 * it are kept as they are, so `@` links keep their `linkKey` (and their `맞음`) and paragraphs keep
 * their keys (recorder `카드에 넣기` targets them). A link or `괄호 지문` run the edit touches
 * becomes plain text. New lines start new paragraphs.
 */
object RichTextEdit {
    /** The editor's text of [doc]: paragraphs joined by new lines. */
    fun textOf(doc: RichDoc): String = doc.ordered().joinToString("\n") { b -> b.runs.joinToString("") { it.shownText } }

    /** [doc] with its text replaced by [newText] (NFC; trailing blank lines dropped). */
    fun apply(doc: RichDoc, newText: String, newKey: () -> Key): RichDoc {
        val ordered = doc.ordered()
        val target = Normalizer.normalize(newText, Normalizer.Form.NFC).trimEnd()
        val old = textOf(ordered)
        if (target == old) return doc
        if (target.isBlank()) return emptyList()

        val atoms = atomsOf(ordered)
        var prefix = commonPrefix(old, target)
        var suffix = commonSuffix(old, target, prefix)
        // Widen the edited span to whole runs except inside plain text.
        var end = old.length - suffix
        var pos = 0
        for (atom in atoms) {
            val len = atom.text.length
            val splittable = atom.run is TextRun.Plain
            if (!splittable && prefix > pos && prefix < pos + len) prefix = pos
            if (!splittable && end > pos && end < pos + len) end = pos + len
            pos += len
        }
        suffix = old.length - end
        val middle = target.substring(prefix, target.length - suffix)

        val out = ArrayList<Atom>()
        pos = 0
        for (atom in atoms) {
            val len = atom.text.length
            val from = pos
            val to = pos + len
            pos = to
            when {
                to <= prefix -> out += atom
                from < prefix -> out += atom.slice(0, prefix - from)
            }
        }
        middle.split('\n').forEachIndexed { i, piece ->
            if (i > 0) out += Atom(null, NEW_PARAGRAPH, "\n", separator = true)
            if (piece.isNotEmpty()) out += Atom(TextRun.Plain(piece), NEW_PARAGRAPH, piece)
        }
        pos = 0
        for (atom in atoms) {
            val len = atom.text.length
            val from = pos
            val to = pos + len
            pos = to
            when {
                from >= end -> out += atom
                to > end -> out += atom.slice(end - from, len)
            }
        }
        return rebuild(ordered, out, newKey)
    }

    /**
     * `@` 연결 (WLD-17): [doc] with [link] inserted at [offset] of [textOf] (clamped). An offset
     * inside another link or `괄호 지문` moves to its end, so runs are never cut; plain text splits.
     * Returns the new text and the offset right after the inserted link (the caret).
     */
    fun insertLink(doc: RichDoc, offset: Int, link: TextRun.Link, newKey: () -> Key): Pair<RichDoc, Int> {
        val ordered = doc.ordered()
        val atoms = atomsOf(ordered)
        val total = atoms.sumOf { it.text.length }
        var at = offset.coerceIn(0, total)
        var pos = 0
        for (atom in atoms) {
            val len = atom.text.length
            if (atom.run !is TextRun.Plain && !atom.separator && at > pos && at < pos + len) at = pos + len
            pos += len
        }
        val out = ArrayList<Atom>()
        var inserted = false
        pos = 0
        for (atom in atoms) {
            val len = atom.text.length
            val from = pos
            pos += len
            if (!inserted && at == from && !atom.separator) {
                out += Atom(link, atom.paragraph, link.text)
                inserted = true
            }
            if (!inserted && at > from && at < from + len) {
                out += atom.slice(0, at - from)
                out += Atom(link, atom.paragraph, link.text)
                out += atom.slice(at - from, len)
                inserted = true
                continue
            }
            if (!inserted && at == from && atom.separator) {
                // At the end of the previous paragraph.
                out += Atom(link, (out.lastOrNull()?.paragraph ?: 0), link.text)
                inserted = true
            }
            out += atom
        }
        if (!inserted) out += Atom(link, atoms.lastOrNull()?.paragraph ?: if (ordered.isEmpty()) NEW_PARAGRAPH else 0, link.text)
        return rebuild(ordered, out, newKey) to (at + link.text.length)
    }

    // --- atoms ---

    private const val NEW_PARAGRAPH = -1

    /**
     * One run (or a paragraph separator) with the index of the paragraph it came from; a separator
     * carries the paragraph it starts.
     */
    private data class Atom(val run: TextRun?, val paragraph: Int, val text: String, val separator: Boolean = false) {
        fun slice(from: Int, to: Int): Atom {
            val plain = run as TextRun.Plain
            val piece = plain.text.substring(from, to)
            return copy(run = plain.copy(text = piece), text = piece)
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
        while (groups.size > 1 && groups.last().none { !it.separator && it.text.isNotEmpty() }) groups.removeAt(groups.lastIndex)
        val used = HashSet<Int>()
        val sources: List<DocBlock?> = groups.mapIndexed { i, group ->
            val origin = group.firstOrNull { it.paragraph != NEW_PARAGRAPH }?.paragraph
                ?: if (i == 0 && old.isNotEmpty()) 0 else null
            if (origin != null && used.add(origin)) old[origin] else null
        }
        val orders = orderKeys(sources.map { it?.order })
        return groups.mapIndexed { i, group ->
            val runs = Runs.normalize(group.mapNotNull { it.run })
            val source = sources[i]
            source?.copy(order = orders[i], runs = runs)
                ?: DocBlock(newKey(), orders[i], DocBlockKind.PARAGRAPH.open(), runs)
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
}
