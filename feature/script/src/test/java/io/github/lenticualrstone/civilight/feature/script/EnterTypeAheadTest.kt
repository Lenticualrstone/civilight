package io.github.lenticualrstone.civilight.feature.script

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.lenticualrstone.civilight.domain.script.Runs
import io.github.lenticualrstone.civilight.feature.script.editor.FieldChange
import io.github.lenticualrstone.civilight.feature.script.editor.FieldDiff
import io.github.lenticualrstone.civilight.feature.script.editor.FieldOwner
import io.github.lenticualrstone.civilight.feature.script.editor.FieldSync
import io.github.lenticualrstone.civilight.feature.script.editor.Judgement
import io.github.lenticualrstone.civilight.feature.script.editor.RunEdit
import io.github.lenticualrstone.civilight.feature.script.editor.TypedAhead
import io.github.lenticualrstone.civilight.feature.script.state.ScriptOps
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.plainText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/**
 * QA 2026-11-23 #5: Enter after typing made a new block that held the previous block's text, a line
 * break and the new text, while the old block kept its text.
 *
 * [Pipeline] replays the view model's field protocol on the real pieces ([FieldDiff], [FieldSync],
 * [RunEdit], [ScriptOps]) with the operation queue made explicit, so the interleaving of
 * keystrokes and the Enter's operation can be driven step by step.
 */
class EnterTypeAheadTest {
    private val s = FieldDiff.SENTINEL

    /**
     * The view model's field protocol; [drain] runs the queued operations (the store update of Enter).
     * [noop] makes Enter's operation change nothing (a block that takes no Enter).
     */
    private class Pipeline(typed: String, private val useOwners: Boolean = true, private val noop: Boolean = false) {
        var doc: ProjectDoc = Fx.example
        var caret: Key = K.BLOCK_043
        var runs: List<TextRun> = listOf(TextRun.Plain(typed))
        val sync = FieldSync()
        private val queue = ArrayDeque<() -> Unit>()
        private val ctx = Fx.ctx()
        var created: Key? = null

        /** What the caret block's field holds (the view model's `fieldValue`). */
        var field: TextFieldValue = TextFieldValue(FieldDiff.fieldText(typed), TextRange(typed.length + 1))

        init {
            doc = ScriptOps.setRuns(doc, caret, runs, ctx)
        }

        fun owner(key: Key) = if (useOwners) FieldOwner("block:$key") else null

        /** Raw text of a block as stored. */
        fun stored(key: Key): String = Fx.block(doc, key).runs.plainText

        /** One change of a field: [from] is the block whose field emitted it. */
        fun change(from: Key, value: TextFieldValue) {
            val oldRaw = runs.plainText
            val judged = sync.judge(owner(from), value.text, value.selection.end)
            if (judged is Judgement.Ahead) {
                if (!sync.advanced) field = value
                judged.edits.forEach { edit -> queue += { replay(edit) } }
                return
            }
            when (val c = FieldDiff.classify(oldRaw, value.text, value.selection.end, parenMode = false)) {
                is FieldChange.Enter -> {
                    if (owner(from) != null) {
                        field = value
                        sync.rejectEnter(owner(from), oldRaw, c.at)
                    } else {
                        field = field.copy(composition = null)
                    }
                    enter(c.at)
                }
                is FieldChange.Text -> {
                    runs = RunEdit.replace(runs, c.start, c.end, c.inserted)
                    field = value
                    write()
                }
                else -> Unit
            }
        }

        private fun write() {
            doc = ScriptOps.setRuns(doc, caret, runs, ctx)
        }

        private fun seed(raw: String, caretRaw: Int) {
            field = TextFieldValue(FieldDiff.fieldText(raw), TextRange(FieldDiff.fieldOffset(caretRaw)))
        }

        private fun enter(at: Int) {
            queue += {
                val before = owner(caret)
                try {
                    write()
                    if (!noop) {
                        val o = ScriptOps.enter(doc, caret, at, ctx)!!
                        doc = o.doc
                        caret = o.caret!!.blockKey
                        created = o.created
                        runs = Fx.block(doc, caret).runs
                        seed(runs.plainText, 0)
                        sync.caretIn(owner(caret))
                    }
                } finally {
                    val stayed = owner(caret) == before
                    sync.landed(stayed)
                    if (stayed) seed(runs.plainText, at.coerceIn(0, Runs.length(runs)))
                }
            }
        }

        private fun replay(edit: TypedAhead) {
            if (!sync.advanced) return
            when (edit) {
                is TypedAhead.Enter -> enter(edit.offset.coerceIn(0, Runs.length(runs)))
                is TypedAhead.Edit -> {
                    val start = edit.offset.coerceIn(0, Runs.length(runs))
                    val end = (start + edit.length).coerceIn(start, Runs.length(runs))
                    runs = RunEdit.replace(runs, start, end, edit.text)
                    seed(runs.plainText, start + edit.text.length)
                    write()
                }
            }
        }

        fun drain() {
            while (queue.isNotEmpty()) queue.removeFirst()()
        }
    }

    private fun field(raw: String, caretRaw: Int = raw.length) = TextFieldValue(s + raw, TextRange(caretRaw + 1))

    @Test
    fun `a field that emits after the Enter operation ran does not hand its buffer to the new block`() {
        val p = Pipeline("Line one.")
        p.change(K.BLOCK_043, field("Line one.\n"))
        p.drain()
        val b = p.caret
        assertTrue(b != K.BLOCK_043)
        // The old field still holds `Line one.\n`; the next key comes from that buffer.
        p.change(K.BLOCK_043, field("Line one.\nL"))
        p.drain()
        p.change(K.BLOCK_043, field("Line one.\nLine two."))
        p.drain()
        assertEquals("Line one.", p.stored(K.BLOCK_043))
        assertEquals("Line two.", p.stored(b))
        // The field shown is the new block's, not the stale one's.
        assertEquals("Line two.", p.field.text.removePrefix(s.toString()))
    }

    @Test
    fun `keys typed before the Enter operation ran go to the new block`() {
        val p = Pipeline("Line one.")
        p.change(K.BLOCK_043, field("Line one.\n"))
        // The operation is still queued: the field still holds the cut.
        p.change(K.BLOCK_043, field("Line one.\nL"))
        p.change(K.BLOCK_043, field("Line one.\nLi"))
        p.drain()
        assertEquals("Line one.", p.stored(K.BLOCK_043))
        assertEquals("Li", p.stored(p.caret))
    }

    @Test
    fun `a burst of keys with no recomposition between them is typed once`() {
        val p = Pipeline("Line one.")
        p.change(K.BLOCK_043, field("Line one.\n"))
        p.change(K.BLOCK_043, field("Line one.\nx"))
        p.change(K.BLOCK_043, field("Line one.\nxy"))
        p.change(K.BLOCK_043, field("Line one.\nxyz"))
        p.drain()
        assertEquals("Line one.", p.stored(K.BLOCK_043))
        assertEquals("xyz", p.stored(p.caret))
    }

    @Test
    fun `the field keeps the cut while the Enter is in flight instead of taking its text back`() {
        val p = Pipeline("Line one.")
        p.change(K.BLOCK_043, field("Line one.\n"))
        assertEquals(s + "Line one.\n", p.field.text)
        p.change(K.BLOCK_043, field("Line one.\nL"))
        assertEquals(s + "Line one.\nL", p.field.text)
        p.drain()
        // The new block took the caret: its own field is the one shown.
        assertEquals(s + "L", p.field.text)
    }

    @Test
    fun `Enter in the middle of a line moves the typed-ahead text in front of what was cut off`() {
        val p = Pipeline("Line one.Rest")
        p.change(K.BLOCK_043, field("Line one.\nRest", caretRaw = 10))
        p.change(K.BLOCK_043, field("Line one.\nxRest", caretRaw = 11))
        p.drain()
        assertEquals("Line one.", p.stored(K.BLOCK_043))
        assertEquals("xRest", p.stored(p.caret))
    }

    @Test
    fun `two Enters in a row make two blocks and nothing else changes`() {
        val p = Pipeline("Line one.")
        p.change(K.BLOCK_043, field("Line one.\n"))
        p.change(K.BLOCK_043, field("Line one.\n\n"))
        p.drain()
        val scene = p.doc.manuscript.liveBlocksOf(K.SCENE_1_7_3).map { it.key }
        val i = scene.indexOf(K.BLOCK_043)
        assertEquals("Line one.", p.stored(scene[i]))
        assertEquals("", p.stored(scene[i + 1]))
        assertEquals("", p.stored(scene[i + 2]))
        assertTrue(scene.none { p.stored(it) == "\n" })
    }

    @Test
    fun `text typed after two Enters goes to the last block`() {
        val p = Pipeline("Line one.")
        p.change(K.BLOCK_043, field("Line one.\n"))
        p.change(K.BLOCK_043, field("Line one.\n\n"))
        p.change(K.BLOCK_043, field("Line one.\n\nz"))
        p.change(K.BLOCK_043, field("Line one.\n\nzz"))
        p.drain()
        val scene = p.doc.manuscript.liveBlocksOf(K.SCENE_1_7_3).map { it.key }
        val i = scene.indexOf(K.BLOCK_043)
        assertEquals("Line one.", p.stored(scene[i]))
        assertEquals("", p.stored(scene[i + 1]))
        assertEquals("zz", p.stored(scene[i + 2]))
    }

    @Test
    fun `the same buffer reported again changes nothing`() {
        val p = Pipeline("Line one.")
        p.change(K.BLOCK_043, field("Line one.\n"))
        p.change(K.BLOCK_043, field("Line one.\n"))
        p.drain()
        val scene = p.doc.manuscript.liveBlocksOf(K.SCENE_1_7_3).map { it.key }
        assertEquals("", p.stored(scene[scene.indexOf(K.BLOCK_043) + 1]))
        assertEquals(K.BLOCK_043, scene.first { it == K.BLOCK_043 })
        assertEquals(p.caret, scene[scene.indexOf(K.BLOCK_043) + 1])
    }

    @Test
    fun `an Enter that changed nothing gives the field its old text back and drops what was typed after it`() {
        val p = Pipeline("Line one.", noop = true)
        p.change(K.BLOCK_043, field("Line one.\n"))
        p.change(K.BLOCK_043, field("Line one.\nL"))
        p.drain()
        assertEquals("Line one.", p.stored(K.BLOCK_043))
        assertEquals(K.BLOCK_043, p.caret)
        assertEquals(s + "Line one.", p.field.text)
        // The record ended: the next change is an ordinary edit of the same block.
        p.change(K.BLOCK_043, field("Line one.!"))
        assertEquals("Line one.!", p.stored(K.BLOCK_043))
    }

    @Test
    fun `without the emitting field the stale buffer is the bug itself`() {
        // The old protocol: every change is diffed against the block that holds the caret.
        val p = Pipeline("Line one.", useOwners = false)
        p.change(K.BLOCK_043, field("Line one.\n"))
        p.drain()
        p.change(K.BLOCK_043, field("Line one.\nL"))
        p.drain()
        assertEquals("Line one.", p.stored(K.BLOCK_043))
        assertEquals("Line one.\nL", p.stored(p.caret))
    }

    @Test
    fun `a later edit of the old block is a normal edit again once the caret came back to it`() {
        val p = Pipeline("Line one.")
        p.change(K.BLOCK_043, field("Line one.\n"))
        p.drain()
        p.sync.caretIn(FieldOwner("block:${K.BLOCK_043}"))
        p.caret = K.BLOCK_043
        p.runs = Fx.block(p.doc, K.BLOCK_043).runs
        p.change(K.BLOCK_043, field("Line one.!"))
        assertEquals("Line one.!", p.stored(K.BLOCK_043))
    }

    @Test
    fun `an edit of the old part ends the record and is a normal edit`() {
        val sync = FieldSync()
        val owner = FieldOwner("block:a")
        sync.rejectEnter(owner, "Line one.", 9)
        assertEquals(Judgement.Normal, sync.judge(owner, s + "Lines one.", FieldDiff.fieldOffset(5)))
        // Ended: a later change that would have matched is not typed-ahead any more.
        assertEquals(Judgement.Normal, sync.judge(owner, s + "Line one.\nL", FieldDiff.fieldOffset(11)))
    }

    @Test
    fun `changes of other fields are never read as typed-ahead`() {
        val sync = FieldSync()
        sync.rejectEnter(FieldOwner("block:a"), "Line one.", 9)
        assertEquals(Judgement.Normal, sync.judge(FieldOwner("block:b"), s + "Line one.\nL", FieldDiff.fieldOffset(11)))
        assertEquals(Judgement.Normal, sync.judge(null, s + "Line one.\nL", FieldDiff.fieldOffset(11)))
    }

    // --- The IME composing path ---

    @Test
    fun `a composition that only changes is no edit`() {
        val composing = TextFieldValue(s + "Line one.", TextRange(10), composition = TextRange(6, 10))
        assertEquals(FieldChange.None, FieldDiff.classify("Line one.", composing.text, composing.selection.end, parenMode = false))
        val done = composing.copy(composition = null)
        assertEquals(FieldChange.None, FieldDiff.classify("Line one.", done.text, done.selection.end, parenMode = false))
    }

    @Test
    fun `Enter that arrives while a word is composing is still an Enter`() {
        // Latin keyboard: the word `one.` is the composing region; Enter commits it and adds the break.
        val value = TextFieldValue(s + "Line one.\n", TextRange(11), composition = null)
        assertEquals(FieldChange.Enter(9), FieldDiff.classify("Line one.", value.text, value.selection.end, parenMode = false))
        // Hangul: the last syllable is still composing when the break comes in.
        val hangul = TextFieldValue(s + "하르트\n", TextRange(5), composition = TextRange(3, 4))
        assertEquals(FieldChange.Enter(3), FieldDiff.classify("하르트", hangul.text, hangul.selection.end, parenMode = false))
    }

    @Test
    fun `the field is given back without the composition the Enter ended`() {
        val before = TextFieldValue(s + "Line one.", TextRange(10), composition = TextRange(6, 10))
        val reverted = before.copy(composition = null)
        assertEquals(before.text, reverted.text)
        assertEquals(before.selection, reverted.selection)
        assertFalse(before == reverted)
        assertEquals(null, reverted.composition)
    }

    @Test
    fun `typed-ahead during a composing session lands in the new block too`() {
        val p = Pipeline("Line one.")
        p.change(K.BLOCK_043, TextFieldValue(s + "Line one.\n", TextRange(11)))
        p.change(K.BLOCK_043, TextFieldValue(s + "Line one.\n라", TextRange(12), composition = TextRange(11, 12)))
        p.change(K.BLOCK_043, TextFieldValue(s + "Line one.\n라인", TextRange(13), composition = TextRange(11, 13)))
        // In flight the field keeps the composition, so the keyboard goes on composing the syllable.
        assertEquals(TextRange(11, 13), p.field.composition)
        p.drain()
        assertEquals("Line one.", p.stored(K.BLOCK_043))
        assertEquals("라인", p.stored(p.caret))
    }
}
