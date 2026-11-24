package io.github.lenticualrstone.civilight.feature.script

import androidx.compose.ui.geometry.Rect
import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.feature.script.motion.BlockPress
import io.github.lenticualrstone.civilight.feature.script.ui.BlockMenu
import io.github.lenticualrstone.civilight.feature.script.ui.BlockMenuItem
import io.github.lenticualrstone.civilight.feature.script.ui.BlockMenuOpen
import io.github.lenticualrstone.civilight.feature.script.ui.BlockMenuItem.DELETE
import io.github.lenticualrstone.civilight.feature.script.ui.BlockMenuItem.DISPLAY_NAME
import io.github.lenticualrstone.civilight.feature.script.ui.BlockMenuItem.EDIT
import io.github.lenticualrstone.civilight.feature.script.ui.BlockMenuItem.INSERT_AFTER
import io.github.lenticualrstone.civilight.feature.script.ui.BlockMenuItem.KIND
import io.github.lenticualrstone.civilight.feature.script.ui.BlockMenuItem.MENTION_OR_PAYOFF
import io.github.lenticualrstone.civilight.feature.script.ui.BlockMenuItem.PLANT
import io.github.lenticualrstone.civilight.feature.script.ui.BlockMenuItem.PROPS
import io.github.lenticualrstone.civilight.feature.script.ui.BlockMenuItem.REF
import io.github.lenticualrstone.civilight.feature.script.ui.BlockMenuItem.SCENE_CUT
import io.github.lenticualrstone.civilight.feature.script.ui.BlockMenuItem.TIME
import io.github.lenticualrstone.civilight.model.script.BlockKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** (0.2.4) The block's long-press menu: its rows, the swipe's edit side, placement and the long-press decision. */
class BlockMenuTest {
    @Test
    fun `a dialogue block lists every action in order, 지우기 last and apart`() {
        val items = BlockMenu.items(edit = true, editLocked = false, kind = BlockKind.DIALOGUE)
        assertEquals(listOf(EDIT, INSERT_AFTER, KIND, SCENE_CUT, PLANT, MENTION_OR_PAYOFF, DISPLAY_NAME, TIME, REF, PROPS, DELETE), items)
        assertTrue(BlockMenu.dividerBefore(items, items.indexOf(DELETE)))
        assertTrue(DELETE.warning)
        assertTrue(items.filter { it != DELETE }.none { it.warning })
        // Lines between the four groups only.
        assertEquals(3, items.indices.count { BlockMenu.dividerBefore(items, it) })
        assertEquals(6f * 2 + 48f * 11 + 9f * 3, BlockMenu.heightDp(items), 0f)
    }

    @Test
    fun `kind rules - no 복선 심기 on a memo, 표시 이름 only on dialogue`() {
        val memo = BlockMenu.items(edit = true, editLocked = false, kind = BlockKind.MEMO)
        assertFalse(PLANT in memo)
        assertTrue(MENTION_OR_PAYOFF in memo)
        assertFalse(DISPLAY_NAME in memo)
        val narration = BlockMenu.items(edit = true, editLocked = false, kind = BlockKind.NARRATION)
        assertTrue(PLANT in narration)
        assertFalse(DISPLAY_NAME in narration)
    }

    @Test
    fun `same rules as the swipe - locked hidden text has no 고치기, read-only keeps only 시간 참고 속성`() {
        val locked = BlockMenu.items(edit = true, editLocked = true, kind = BlockKind.DIALOGUE)
        assertFalse(EDIT in locked)
        assertEquals(listOf(INSERT_AFTER, KIND, SCENE_CUT, DELETE), BlockMenu.swipeEdit(locked))
        val readOnly = BlockMenu.items(edit = false, editLocked = false, kind = BlockKind.DIALOGUE)
        assertEquals(listOf(TIME, REF, PROPS), readOnly)
        assertEquals(emptyList<BlockMenuItem>(), BlockMenu.swipeEdit(readOnly))
        // An unknown kind: no edit actions either (as the swipe).
        assertEquals(listOf(TIME, REF, PROPS), BlockMenu.items(edit = true, editLocked = false, kind = null))
    }

    @Test
    fun `the swipe's edit side is the five frequent actions in the menu's order`() {
        val items = BlockMenu.items(edit = true, editLocked = false, kind = BlockKind.DIALOGUE)
        assertEquals(listOf(EDIT, INSERT_AFTER, KIND, SCENE_CUT, DELETE), BlockMenu.swipeEdit(items))
        assertEquals(listOf("고치기", "이 뒤에", "종류", "씬 나누기", "지우기"), BlockMenu.swipeEdit(items).map { it.label })
    }

    @Test
    fun `every row has an icon or the change marker, and its words pass the copy lint`() {
        BlockMenuItem.entries.forEach { item ->
            assertTrue(item.name, item.icon != null || item == TIME)
            assertTrue(item.label, CopyLint.lint(item.label, isButton = true).isEmpty())
        }
        assertTrue(CopyLint.lint(ScriptCopy.BLOCK_MENU).isEmpty())
        assertTrue(BlockMenu.ROW_DP >= 48f)
    }

    @Test
    fun `placement - below the block, else above, else the larger side, else the lower part`() {
        val w = 400f
        val h = 800f
        fun place(block: Rect, menuH: Float) = BlockMenu.place(block, 248f, menuH, w, h, top = 40f, bottom = 40f, side = 12f, gap = 8f, minRoom = 160f)
        // Room below.
        val below = place(Rect(0f, 100f, 400f, 160f), 300f)
        assertEquals(168f, below.y, 0f)
        // No room below, room above.
        val above = place(Rect(0f, 600f, 400f, 700f), 300f)
        assertEquals(600f - 8f - 300f, above.y, 0f)
        // Neither fits: the larger side, scrolling inside.
        val larger = place(Rect(0f, 300f, 400f, 380f), 600f)
        assertEquals(388f, larger.y, 0f)
        assertEquals(800f - 40f - 388f, larger.maxHeight, 0f)
        // A block taller than the screen: the card sits over the lower part.
        val tall = place(Rect(0f, 0f, 400f, 800f), 600f)
        assertEquals(800f - 40f - 600f, tall.y, 0f)
        // The card lines up with the block's right edge, inside the side margin.
        assertEquals(12f, place(Rect(0f, 100f, 200f, 160f), 300f).x, 0f)
        assertEquals(400f - 12f - 248f, place(Rect(0f, 100f, 400f, 160f), 300f).x, 0f)
    }

    @Test
    fun `the scrim's hole covers the lifted block`() {
        val block = Rect(0f, 100f, 400f, 200f)
        val hole = BlockMenu.hole(block, CivilightMotion.liftScale, 6f)
        assertTrue(hole.left < block.left && hole.right > block.right && hole.top < block.top && hole.bottom > block.bottom)
        assertEquals(block.center, hole.center)
        assertTrue(CivilightMotion.liftScale > 1f)
    }

    @Test
    fun `long press - waits inside the slop, cancels on a move, a lift, a second finger or a taken finger`() {
        val slop = 18f
        assertEquals(BlockPress.Verdict.WAIT, BlockPress.verdict(3f, 4f, slop, lifted = false, takenByOther = false, pointers = 1))
        assertEquals(BlockPress.Verdict.CANCEL, BlockPress.verdict(20f, 0f, slop, lifted = false, takenByOther = false, pointers = 1))
        assertEquals(BlockPress.Verdict.CANCEL, BlockPress.verdict(0f, 19f, slop, lifted = false, takenByOther = false, pointers = 1))
        assertEquals(BlockPress.Verdict.CANCEL, BlockPress.verdict(0f, 0f, slop, lifted = true, takenByOther = false, pointers = 1))
        assertEquals(BlockPress.Verdict.CANCEL, BlockPress.verdict(0f, 0f, slop, lifted = false, takenByOther = true, pointers = 1))
        assertEquals(BlockPress.Verdict.CANCEL, BlockPress.verdict(0f, 0f, slop, lifted = false, takenByOther = false, pointers = 2))
    }

    @Test
    fun `a press that catches a fling never opens the menu`() {
        assertFalse(BlockPress.mayStart(scrolling = true))
        assertTrue(BlockPress.mayStart(scrolling = false))
    }

    @Test
    fun `the block settles as soon as the menu starts closing`() {
        val open = BlockMenuOpen("row", "b1", Rect(0f, 0f, 10f, 10f))
        assertEquals("row", open.liftedRow)
        assertEquals(null, open.copy(closing = true).liftedRow)
    }
}
