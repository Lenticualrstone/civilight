package io.github.lenticualrstone.civilight.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.lenticualrstone.civilight.ui.gallery.GalleryCardRules
import io.github.lenticualrstone.civilight.ui.richtext.RichDocEdit
import io.github.lenticualrstone.civilight.ui.richtext.RichTextEditorState
import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryRingAndCaptionBurstTest {
    @Test
    fun `selection ring hugs the drawn card, not the rule size - GAL-07 group rows`() {
        // A group row shrinks the card to 162 x 216 dp; at density 1 px = dp.
        val g = 2f
        val r = GalleryCardRules.RADIUS_DP
        val gap = GalleryCardRules.ringGap(162f, 216f, g, r)
        assertEquals(-1f, gap.left, 0f)
        assertEquals(-1f, gap.top, 0f)
        assertEquals(164f, gap.width, 0f)
        assertEquals(218f, gap.height, 0f)
        assertEquals(21f, gap.corner, 0f)
        val acc = GalleryCardRules.ringAcc(162f, 216f, g, r)
        assertEquals(-3f, acc.left, 0f)
        assertEquals(-3f, acc.top, 0f)
        assertEquals(168f, acc.width, 0f)
        assertEquals(222f, acc.height, 0f)
        assertEquals(23f, acc.corner, 0f)
        // The outer edge of the acc stroke sits 4 dp outside the card on every side.
        assertEquals(162f + 8f, acc.width + g, 0f)
        // Full-size card (non-group gallery) is unchanged.
        val full = GalleryCardRules.ringAcc(222f, 296f, g, r)
        assertEquals(228f, full.width, 0f)
        assertEquals(302f, full.height, 0f)
    }

    @Test
    fun `caption burst - every typed character stays in field, doc and result`() {
        var n = 0
        val newKey = { "burst-${n++}" }
        val state = RichTextEditorState.of(emptyList(), newKey)
        val typed = "abcdefghijklmnopqrstuvwxyz0123456789"
        for (ch in typed) {
            val next = state.field.text + ch
            state.onFieldChange(TextFieldValue(next, TextRange(next.length)))
        }
        assertEquals(typed, state.field.text)
        assertEquals(typed, RichDocEdit.textOf(state.doc))
        assertEquals(typed, RichDocEdit.textOf(state.result))
    }

    @Test
    fun `caption burst with IME composition ranges keeps the tail`() {
        var n = 0
        val newKey = { "ime-${n++}" }
        val state = RichTextEditorState.of(emptyList(), newKey)
        val words = listOf("harbor.", " still lit", " 항구")
        var committed = ""
        for (word in words) {
            for (i in 1..word.length) {
                val text = committed + word.substring(0, i)
                state.onFieldChange(TextFieldValue(text, TextRange(text.length), TextRange(committed.length, text.length)))
            }
            committed += word
            state.onFieldChange(TextFieldValue(committed, TextRange(committed.length), composition = null))
        }
        assertEquals("harbor. still lit 항구", state.field.text)
        assertEquals("harbor. still lit 항구", RichDocEdit.textOf(state.result))
    }

    @Test
    fun `caption burst survives the saver round trip with caret and paren mode`() {
        var n = 0
        val newKey = { "save-${n++}" }
        val state = RichTextEditorState.of(emptyList(), newKey)
        val typed = "abcdefghijklmnopqrstuvwxyz0123456789"
        for (ch in typed) {
            val next = state.field.text + ch
            state.onFieldChange(TextFieldValue(next, TextRange(next.length), TextRange(0, next.length)))
        }
        state.toggleParen()
        val saver = RichTextEditorState.saver(newKey)
        val scope = object : SaverScope {
            override fun canBeSaved(value: Any): Boolean = true
        }
        val saved = with(saver) { scope.save(state) }!!
        val restored = saver.restore(saved)!!
        assertEquals(typed, restored.field.text)
        assertEquals(TextRange(typed.length), restored.field.selection)
        assertEquals(typed, RichDocEdit.textOf(restored.result))
        assertEquals(true, restored.parenMode)
        assertEquals(state.original, restored.original)
    }
}
