package io.github.lenticualrstone.civilight.feature.script.input

import io.github.lenticualrstone.civilight.designsystem.component.InputKind
import io.github.lenticualrstone.civilight.designsystem.component.InputMark
import io.github.lenticualrstone.civilight.designsystem.component.InputSpeaker
import io.github.lenticualrstone.civilight.designsystem.component.ScriptInputState
import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.domain.script.Links
import io.github.lenticualrstone.civilight.domain.script.Runs
import io.github.lenticualrstone.civilight.domain.script.ScriptWrite
import io.github.lenticualrstone.civilight.domain.script.WriteDraft
import io.github.lenticualrstone.civilight.feature.script.editor.LinkLook
import io.github.lenticualrstone.civilight.feature.script.editor.RunEdit
import io.github.lenticualrstone.civilight.feature.script.editor.SpanKind
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.DirectionCue
import io.github.lenticualrstone.civilight.model.text.TextRun
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * (0.2.0, U-14, SCR-70) The input bar's draft, kept as runs (never rebuilt from plain text, so a link keeps
 * its key and a paren stays a paren through `고치기`). The field's text is the runs' raw text (a paren run
 * without its parentheses, one `\n` per break); the parentheses are drawn by [DraftTransformation].
 */
@Serializable
data class Draft(
    val runs: List<TextRun> = emptyList(),
    /** Caret / selection in raw offsets. */
    val selStart: Int = 0,
    val selEnd: Int = 0,
    /** Draft cue chips (kept, drawn only with `게임 제작 도구` on). */
    val cues: List<DirectionCue> = emptyList(),
    val speakerKey: Key? = null,
    val kind: BlockKind = BlockKind.DIALOGUE,
    val mark: DraftMark = DraftMark.None,
    /** The draft set aside when `고치기` started (SCR-72); it comes back whole when it ends. */
    val aside: Draft? = null,
    /** Typing goes into a paren run at the caret (`( )` or a typed `(`), until `)` or the caret moves. */
    val parenOpen: Boolean = false,
    /** An empty `()` drawn at this raw offset until its first letter (SCR-16 `( )`). */
    val pendingParenAt: Int? = null,
) {
    val text: String get() = rawText(runs)
    val editing: Boolean get() = mark is DraftMark.Edit
    val caret: Int get() = selEnd
}

/** The mark above the input: none, `이 뒤에` a block, or `고치기` a block. */
@Serializable
sealed interface DraftMark {
    @Serializable
    data object None : DraftMark

    @Serializable
    data class After(val blockKey: Key) : DraftMark

    @Serializable
    data class Edit(val blockKey: Key) : DraftMark
}

/** Raw text of runs: shown text, one `\n` per break. */
fun rawText(runs: List<TextRun>): String = runs.joinToString("") { it.shownText }

/** What a text change asks of the screen. */
enum class DraftEffect { NONE, OPEN_LINK_PICKER }

/** The pure reducer of the draft (JVM tested). */
object Drafts {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    fun encode(d: Draft): String = json.encodeToString(Draft.serializer(), d)

    fun decode(s: String?): Draft? = s?.let { runCatching { json.decodeFromString(Draft.serializer(), it) }.getOrNull() }

    /**
     * The field reported [text] with the selection [selStart, selEnd] (typing, IME, paste, Enter). One range
     * replace on the runs ([RunEdit]); a typed `@` opens the link picker, a typed `(` opens a paren and `)`
     * closes it (both letters are dropped; the parentheses are drawn).
     */
    fun textChanged(d: Draft, text: String, selStart: Int, selEnd: Int): Pair<Draft, DraftEffect> {
        val old = d.text
        if (text == old) {
            // Only the caret moved: a paren stays open only while the caret stays where typing went.
            val moved = selEnd != d.selEnd
            return d.copy(
                selStart = selStart.coerceIn(0, text.length), selEnd = selEnd.coerceIn(0, text.length),
                parenOpen = d.parenOpen && !moved, pendingParenAt = d.pendingParenAt.takeIf { !moved },
            ) to DraftEffect.NONE
        }
        var prefix = 0
        val max = minOf(old.length, text.length)
        while (prefix < max && old[prefix] == text[prefix]) prefix++
        var suffix = 0
        while (suffix < max - prefix && old[old.length - 1 - suffix] == text[text.length - 1 - suffix]) suffix++
        val removedEnd = old.length - suffix
        var inserted = text.substring(prefix, text.length - suffix)
        var effect = DraftEffect.NONE
        var parenOpen = d.parenOpen
        var pending = d.pendingParenAt
        // A single typed control letter (never inside a paste).
        if (inserted == "@" && prefix == removedEnd) {
            return d.copy(selStart = prefix, selEnd = prefix) to DraftEffect.OPEN_LINK_PICKER
        }
        if (inserted == "(" && prefix == removedEnd && d.kind == BlockKind.DIALOGUE) {
            return d.copy(selStart = prefix, selEnd = prefix, parenOpen = true, pendingParenAt = prefix) to DraftEffect.NONE
        }
        // (0.4.0) Handle both direct `)` and IME-committed trailing `)` (e.g. "감)") cleanly without leaving `(text))`.
        if ((inserted == ")" || (inserted.endsWith(")") && inserted.length <= 4)) && (parenOpen || pending != null) && d.kind == BlockKind.DIALOGUE) {
            val beforeClose = inserted.dropLast(1)
            val runs = if (beforeClose.isNotEmpty() || prefix != removedEnd) {
                Runs.normalize(RunEdit.replace(d.runs, prefix, removedEnd, beforeClose, parenMode = true))
            } else d.runs
            val caret = (prefix + beforeClose.length).coerceIn(0, rawText(runs).length)
            return d.copy(
                runs = runs,
                selStart = caret,
                selEnd = caret,
                parenOpen = false,
                pendingParenAt = null,
            ) to DraftEffect.NONE
        }
        if (inserted.isEmpty() && pending != null) {
            pending = null
            parenOpen = false
        }
        val parenMode = parenOpen && d.kind == BlockKind.DIALOGUE
        val runs = Runs.normalize(RunEdit.replace(d.runs, prefix, removedEnd, inserted, parenMode = parenMode))
        if (inserted.isNotEmpty()) pending = null
        if (inserted.contains('\n')) parenOpen = false
        // If all text inside a paren run was deleted, exit paren mode so subsequent typing is normal.
        if (inserted.isEmpty() && parenOpen && runs.none { it is TextRun.Paren }) {
            parenOpen = false
        }
        val caretShift = rawText(runs).length - text.length
        return d.copy(
            runs = runs,
            selStart = (selStart + caretShift).coerceIn(0, rawText(runs).length),
            selEnd = (selEnd + caretShift).coerceIn(0, rawText(runs).length),
            parenOpen = parenOpen, pendingParenAt = pending,
        ) to effect
    }

    /** (0.4.0) The tool row `( )` (SCR-16, `대사` only): toggles open/close cleanly. */
    fun openParen(d: Draft): Draft {
        if (d.kind != BlockKind.DIALOGUE) return d
        return if (d.parenOpen || d.pendingParenAt != null) {
            d.copy(parenOpen = false, pendingParenAt = null)
        } else {
            d.copy(parenOpen = true, pendingParenAt = d.selEnd, selStart = d.selEnd)
        }
    }

    /** A smart-punctuation result: the field's [text] and caret, and how to take it back ([undo]). */
    data class Smart(val text: String, val selStart: Int, val selEnd: Int, val undo: SmartUndo?)

    /**
     * The last substitution: an immediate backspace that turns [result] into [backspaced] restores [literal]
     * (the typed `...` / `--`) with the caret at [literalCaret].
     */
    data class SmartUndo(val backspaced: String, val literal: String, val literalCaret: Int)

    private val SMART_PAIRS = listOf("..." to "…", "--" to "──")

    /**
     * (0.5, review item 7) Optional smart scenario punctuation (`...` → `…`, `--` → `──`) on the just-typed span
     * only: [old] is the draft's text before, [text] the field's report. A match must overlap the inserted span
     * (up to two letters of context before it complete a pattern), so text already in the draft (loaded by
     * `고치기`, pasted earlier, restored by an undo) is never rewritten. The caret moves only by replacements
     * that end at or before it. [undo] is the previous substitution: when [text] is exactly its backspace,
     * the typed literal comes back and is not substituted again.
     */
    fun smartPunctuation(old: String, text: String, selStart: Int, selEnd: Int, undo: SmartUndo?): Smart {
        if (undo != null && text == undo.backspaced) {
            return Smart(undo.literal, undo.literalCaret, undo.literalCaret, null)
        }
        if (text == old) return Smart(text, selStart, selEnd, undo)
        var prefix = 0
        val max = minOf(old.length, text.length)
        while (prefix < max && old[prefix] == text[prefix]) prefix++
        var suffix = 0
        while (suffix < max - prefix && old[old.length - 1 - suffix] == text[text.length - 1 - suffix]) suffix++
        val insEnd = text.length - suffix
        if (insEnd <= prefix) return Smart(text, selStart, selEnd, null)
        val from = (prefix - 2).coerceAtLeast(0)
        val out = StringBuilder(text.length)
        out.append(text, 0, from)
        var s = selStart
        var e = selEnd
        var last: Pair<Int, Int>? = null // (start in out, replacement length) of the last substitution
        var lastLiteral = ""
        var i = from
        while (i < text.length) {
            val hit = if (i < insEnd) {
                SMART_PAIRS.firstOrNull { (lit, _) -> text.startsWith(lit, i) && i + lit.length > prefix && i + lit.length <= insEnd }
            } else null
            if (hit == null) {
                out.append(text[i]); i++; continue
            }
            val (lit, rep) = hit
            val end = i + lit.length
            val shift = rep.length - lit.length
            if (end <= selStart) s += shift
            if (end <= selEnd) e += shift
            last = out.length to rep.length
            lastLiteral = lit
            out.append(rep)
            i = end
        }
        val result = out.toString()
        if (last == null) return Smart(text, selStart, selEnd, null)
        val caret = s.coerceIn(0, result.length)
        val (at, len) = last
        // Only a substitution that ends at a collapsed caret can be taken back by the next backspace.
        val nextUndo = if (s == e && at + len == caret) {
            SmartUndo(
                backspaced = result.removeRange(caret - 1, caret),
                literal = result.substring(0, at) + lastLiteral + result.substring(at + len),
                literalCaret = at + lastLiteral.length,
            )
        } else null
        return Smart(result, caret, e.coerceIn(0, result.length), nextUndo)
    }

    /** A picked `@` item: a link run at the caret, the caret after it (SCR-55). */
    fun insertLink(d: Draft, link: TextRun.Link): Draft {
        val at = d.selEnd.coerceIn(0, d.text.length)
        val start = minOf(d.selStart, d.selEnd).coerceIn(0, d.text.length)
        val cleared = if (start != at) RunEdit.replace(d.runs, start, at, "") else d.runs
        val runs = Runs.normalize(Runs.insertRun(cleared, start, link))
        val caret = start + link.text.length
        return d.copy(runs = runs, selStart = caret, selEnd = caret, parenOpen = false, pendingParenAt = null)
    }

    fun pickSpeaker(d: Draft, key: Key?): Draft = d.copy(speakerKey = key)

    /** A cue chip added at the end of the draft's cues (SCR-08, switch on). */
    fun addCue(d: Draft, cue: DirectionCue): Draft = d.copy(cues = d.cues + cue)

    /** A cue chip's `✕` (SCR-08): it leaves the draft; the block keeps it until the send. */
    fun removeCue(d: Draft, cueKey: Key): Draft = d.copy(cues = d.cues.filterNot { it.key == cueKey })

    fun pickKind(d: Draft, kind: BlockKind): Draft = d.copy(kind = kind, parenOpen = false, pendingParenAt = null)

    /** Right swipe `이 뒤에` (SCR-73): the draft is kept; not during `고치기`. */
    fun insertAfter(d: Draft, blockKey: Key): Draft = if (d.editing) d else d.copy(mark = DraftMark.After(blockKey))

    /**
     * Right swipe `고치기` (SCR-72): the draft is set aside whole (a second `고치기` keeps the first aside); the
     * field takes the block's runs exactly, the chips its speaker and kind, the cue chips its cues.
     */
    fun startEdit(d: Draft, block: Block): Draft {
        val aside = d.aside ?: d.copy(aside = null, parenOpen = false, pendingParenAt = null)
        val runs = ScriptWrite.draftRunsOf(block)
        val end = rawText(runs).length
        return Draft(
            runs = runs, selStart = end, selEnd = end, cues = block.cues,
            speakerKey = if (block.kind.known == BlockKind.DIALOGUE) block.speakerKey else aside.speakerKey,
            kind = block.kind.known ?: BlockKind.NARRATION,
            mark = DraftMark.Edit(block.key), aside = aside,
        )
    }

    /** `✕` (SCR-72, SCR-73): clears `이 뒤에`, or ends `고치기` and discards its edits. */
    fun close(d: Draft): Draft = when (d.mark) {
        DraftMark.None -> d
        is DraftMark.After -> d.copy(mark = DraftMark.None)
        is DraftMark.Edit -> restore(d)
    }

    /** A send, `고치기` send, `나누기` or `합치기` went through. The chips keep their values (U-14.2). */
    fun sent(d: Draft): Draft = when (d.mark) {
        is DraftMark.Edit -> restore(d)
        else -> d.copy(runs = emptyList(), cues = emptyList(), selStart = 0, selEnd = 0, mark = DraftMark.None, parenOpen = false, pendingParenAt = null)
    }

    /**
     * The `고치기` block is gone. [locked]: it became hidden and locked, so its text leaves the field and the
     * set-aside draft comes back; else nothing typed is lost (SCR-72): with a set-aside draft that holds text,
     * the field gets the set-aside text, a line break and the edited text, under the set-aside chips and mark;
     * with none (or a blank one), the edited text stays with its chips.
     */
    fun editTargetGone(d: Draft, locked: Boolean): Draft {
        if (!d.editing) return d
        if (locked) return restore(d)
        val aside = d.aside
        if (aside == null || (aside.runs.isEmpty() && aside.cues.isEmpty())) {
            return d.copy(mark = aside?.mark ?: DraftMark.None, aside = null)
        }
        val runs = when {
            d.runs.isEmpty() -> aside.runs
            aside.runs.isEmpty() -> d.runs
            else -> Runs.normalize(aside.runs + TextRun.Break + d.runs)
        }
        val end = rawText(runs).length
        return aside.copy(
            runs = runs, selStart = end, selEnd = end,
            cues = aside.cues + d.cues.filter { c -> aside.cues.none { it.key == c.key } },
            aside = null, parenOpen = false, pendingParenAt = null,
        )
    }

    /** The `이 뒤에` block is gone, moved or locked: the mark clears silently (SCR-73). */
    fun afterTargetGone(d: Draft): Draft = if (d.mark is DraftMark.After) d.copy(mark = DraftMark.None) else d

    private fun restore(d: Draft): Draft {
        val a = d.aside ?: return d.copy(runs = emptyList(), cues = emptyList(), selStart = 0, selEnd = 0, mark = DraftMark.None)
        return a.copy(aside = null)
    }

    /**
     * Follows the document: a `고치기` or `이 뒤에` block that is no longer live (or sits in a locked hidden scene,
     * or in another episode) ends its mark (SCR-72, SCR-73, T-U14-25); a `고치기` block whose speaker is hidden or
     * that links a hidden item, while locked, ends it the same way (its text leaves the field, SCR-55); a chip
     * speaker that went to the trash or became hidden and locked resets to `화자 없음` (SCR-18).
     */
    fun reconcile(d: Draft, doc: ProjectDoc, unitKey: Key?, unlocked: Boolean): Draft {
        fun state(key: Key): Gone? {
            val block = doc.index.block(key)
            if (block == null || !doc.manuscript.isLiveBlock(key)) return Gone.REMOVED
            val scene = doc.index.unit(block.sceneKey)
            if (scene?.hidden == true && !unlocked) return Gone.LOCKED
            val speaker = block.speakerKey?.let { doc.index.character(it) }
            if (speaker?.hidden == true && !unlocked) return Gone.LOCKED
            if (unitKey != null && doc.numberingUnitOfSceneOrNull(block.sceneKey) != unitKey) return Gone.REMOVED
            return null
        }
        // Hidden text first: a hidden link in the field (added while unlocked) or in the block wins over REMOVED,
        // so the join of a removed block never keeps it.
        fun editState(key: Key): Gone? =
            if (Runs.hidesLink(doc, d.runs, unlocked) || doc.index.block(key)?.let { Runs.blockHidesLink(doc, it, unlocked) } == true) Gone.LOCKED
            else state(key)
        var out = d
        when (val m = d.mark) {
            is DraftMark.Edit -> editState(m.blockKey)?.let { out = editTargetGone(out, it == Gone.LOCKED) }
            is DraftMark.After -> state(m.blockKey)?.let { out = afterTargetGone(out) }
            DraftMark.None -> Unit
        }
        val aside = out.aside
        if (aside?.mark is DraftMark.After && state((aside.mark as DraftMark.After).blockKey) != null) {
            out = out.copy(aside = afterTargetGone(aside))
        }
        return scrub(out, doc, unlocked)
    }

    /**
     * Rule 19 / T-U14-25 on every draft, whatever its mark, and on the set-aside one: while locked, a link to a
     * hidden item leaves the draft (its text never stays in the field, the saved state or `view.json`; it is
     * not turned into plain text, which a send would write into the manuscript), and a chip speaker that went to
     * the trash or is hidden resets to `화자 없음` (SCR-18).
     */
    fun scrub(d: Draft, doc: ProjectDoc, unlocked: Boolean): Draft {
        var out = d
        if (Runs.hidesLink(doc, d.runs, unlocked)) {
            val runs = Runs.normalize(d.runs.filterNot { it is TextRun.Link && Links.state(doc, it, unlocked) is Links.State.Hidden })
            val len = rawText(runs).length
            out = out.copy(
                runs = runs, selStart = d.selStart.coerceIn(0, len), selEnd = d.selEnd.coerceIn(0, len),
                parenOpen = false, pendingParenAt = null,
            )
        }
        out.speakerKey?.let { key ->
            val c = doc.index.character(key)
            if (c == null || c.deletedAt != null || (c.hidden && !unlocked)) out = out.copy(speakerKey = null)
        }
        val aside = out.aside
        if (aside != null) {
            val clean = scrub(aside, doc, unlocked)
            if (clean != aside) out = out.copy(aside = clean)
        }
        return out
    }

    /**
     * What may be kept outside the process (`view.json`, the screen's saved state): the draft as a locked gate
     * leaves it, since the gate is shut after process death. A `고치기` of a hidden block gives back the set-aside
     * draft; hidden links and a hidden chip speaker are gone. [unitKey] null: no episode check.
     */
    fun forStorage(d: Draft, doc: ProjectDoc): Draft = reconcile(d, doc, null, unlocked = false)

    private enum class Gone { REMOVED, LOCKED }

    /** The domain draft; [gameTools] off hands null cues, so stored cues stay untouched (scope §8). */
    fun toWrite(d: Draft, gameTools: Boolean): WriteDraft =
        WriteDraft(d.kind, d.runs, d.speakerKey.takeIf { d.kind == BlockKind.DIALOGUE }, d.cues.takeIf { gameTools })

    /**
     * (Double-tap guard) A queued send, `나누기` or `합치기` of [captured] still applies only while the live draft
     * is the same one: same mark, runs, kind, speaker and cues. A second tap queued before the first ran sees
     * the field already cleared (or the set-aside draft back) and does nothing.
     */
    fun stillPending(captured: Draft, live: Draft): Boolean =
        live.mark == captured.mark && live.runs == captured.runs && live.kind == captured.kind &&
            live.speakerKey == captured.speakerKey && live.cues == captured.cues

    /** `나누기`: the runs before and after the caret. */
    fun splitParts(d: Draft): Pair<List<TextRun>, List<TextRun>> = Runs.splitAt(d.runs, d.selEnd.coerceIn(0, d.text.length))

    /** `나누기` is on (SCR-72, T-U14-10): during `고치기`, not `선택지`, non-blank text on both sides of the caret. */
    fun canSplit(d: Draft): Boolean {
        if (!d.editing || d.kind == BlockKind.CHOICE) return false
        val at = d.selEnd.coerceIn(0, d.text.length)
        return d.text.substring(0, at).isNotBlank() && d.text.substring(at).isNotBlank()
    }

    fun inputKind(kind: BlockKind): InputKind = when (kind) {
        BlockKind.DIALOGUE -> InputKind.DIALOGUE
        BlockKind.NARRATION -> InputKind.NARRATION
        BlockKind.DIRECTION -> InputKind.DIRECTION
        BlockKind.CHOICE -> InputKind.CHOICE
        BlockKind.SYSTEM -> InputKind.SYSTEM
        BlockKind.MEMO -> InputKind.MEMO
    }

    fun blockKind(kind: InputKind): BlockKind = when (kind) {
        InputKind.DIALOGUE -> BlockKind.DIALOGUE
        InputKind.NARRATION -> BlockKind.NARRATION
        InputKind.DIRECTION -> BlockKind.DIRECTION
        InputKind.CHOICE -> BlockKind.CHOICE
        InputKind.SYSTEM -> BlockKind.SYSTEM
        InputKind.MEMO -> BlockKind.MEMO
    }

    /** The bar's view state: [sceneNumber] of the scene in view, the label of the chip speaker. */
    fun inputState(d: Draft, doc: ProjectDoc, sceneNumber: Int?, sceneFlash: Int, ime: Boolean, focused: Boolean): ScriptInputState {
        fun number(key: Key): String = doc.index.block(key)?.let { Labels.number(it.number).removePrefix("#") } ?: ""
        val speaker = d.speakerKey?.let { key -> Labels.speakerName(doc, key)?.let { InputSpeaker(key, it) } }
        val mark = when (val m = d.mark) {
            DraftMark.None -> InputMark.None
            is DraftMark.After -> InputMark.After(number(m.blockKey))
            is DraftMark.Edit -> InputMark.Editing(number(m.blockKey))
        }
        val len = d.text.length
        return ScriptInputState(
            text = d.text,
            selectionStart = d.selStart.coerceIn(0, len),
            selectionEnd = d.selEnd.coerceIn(0, len),
            speaker = speaker,
            kind = inputKind(d.kind),
            mark = mark,
            sceneNumber = sceneNumber,
            sceneFlash = sceneFlash,
            imeVisible = ime,
            focused = focused,
        )
    }
}

private fun ProjectDoc.numberingUnitOfSceneOrNull(sceneKey: Key): Key? = index.numberingUnitKeyOfScene(sceneKey)

/** How the input field draws a link run of the draft (SCR-55): live, masked when locked hidden, deleted. */
object DraftLooks {
    fun of(doc: ProjectDoc, link: TextRun.Link, unlocked: Boolean): LinkLook =
        when (val state = Links.state(doc, link, unlocked)) {
            Links.State.Live -> LinkLook(SpanKind.LINK)
            is Links.State.Hidden -> LinkLook(SpanKind.HIDDEN_LINK, state.text)
            is Links.State.Deleted, is Links.State.Purged ->
                LinkLook(SpanKind.DELETED_LINK)
        }
}
