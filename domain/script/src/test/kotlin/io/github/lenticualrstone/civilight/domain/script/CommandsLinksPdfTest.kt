package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.plainText
import io.github.lenticualrstone.civilight.model.trash.NameTag
import io.github.lenticualrstone.civilight.model.trash.NameTagKind
import io.github.lenticualrstone.civilight.model.trash.NameTagState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Block-add sheet and commands (SCR-12/68), link states (SCR-55) and PDF content (SCR-65). */
class CommandsLinksPdfTest {
    private val ctx = testCtx()
    private val doc = F1.build()
    private fun b(n: Int) = F1.b(n)

    @Test
    fun `block add sheet rows and slash shortcuts`() {
        assertEquals(listOf("/대", "/나", "/연", "/선", "/씬", "/시", "/메"), ScriptCommands.ADD_ROWS.map { it.shortcut })
        assertEquals("화자 · 대사 · (괄호 지문)", ScriptCommands.ADD_ROWS.first().description)
        assertEquals("지금 블록 아래 · 새 대사 화자: 리엔(직전)", ScriptCommands.addSheetSubline(doc, BlockPosition.After(b(46))))
        assertEquals("지금 블록 아래", ScriptCommands.addSheetSubline(doc, BlockPosition.SceneStart(F1.S1)))
        assertEquals(ScriptCommands.Slash.AddBlock(BlockKind.SYSTEM), ScriptCommands.parseSlash("/시"))
        assertEquals(ScriptCommands.Slash.NewScene, ScriptCommands.parseSlash("/씬"))
        assertEquals(ScriptCommands.Slash.OpenSheet, ScriptCommands.parseSlash("/"))
        assertNull(ScriptCommands.parseSlash("/없음"))
        assertEquals(6, ScriptCommands.KIND_ROWS.size)
        assertTrue(ScriptCommands.commands(doc, F1.E17).contains("번호 다시 매기기 · 1-7"))
    }

    @Test
    fun `jumps by block number, scene ordinal and title`() {
        assertEquals(ScriptCommands.Jump.ToBlock(b(43)), ScriptCommands.jump(doc, F1.E17, "#043"))
        assertEquals(ScriptCommands.Jump.ToScene(F1.S3), ScriptCommands.jump(doc, F1.E17, "씬 3"))
        assertEquals(ScriptCommands.Jump.ToScene(F1.S3), ScriptCommands.jump(doc, F1.E17, "꺼진 등불"))
        assertNull(ScriptCommands.jump(doc, F1.E17, "#999"))
        val deleted = BlockTrash.delete(doc, listOf(b(43)), ctx).doc
        assertNull(ScriptCommands.jump(deleted, F1.E17, "#043"))
    }

    private fun noahLink(doc: ProjectDoc) = doc.block(K.block(1, 6, 6)).runs.filterIsInstance<TextRun.Link>().single()

    @Test
    fun `link states for live, hidden, deleted and purged targets`() {
        val l44 = doc.block(b(44)).runs.filterIsInstance<TextRun.Link>().single()
        assertEquals(Links.State.Live, Links.state(doc, l44))
        val deleted = Links.state(doc, noahLink(doc)) as Links.State.Deleted
        assertEquals("지운 인물 · 노아", deleted.card)
        assertEquals(listOf("다른 항목에 잇기", "글로 풀기", "되살리기", "이대로 두기"), deleted.buttons)
        val hiddenTerm = doc.copy(world = doc.world.copy(terms = doc.world.terms.map { if (it.key == K.TERM_ORIGINIUM_LANTERN) it.copy(hidden = true) else it }))
        assertEquals(Links.State.Hidden("숨긴 항목"), Links.state(hiddenTerm, l44))
        assertEquals(Links.State.Live, Links.state(hiddenTerm, l44, unlocked = true))
        val purged = doc.copy(
            cast = doc.cast.copy(characters = doc.cast.characters.filterNot { it.key == K.NOAH }),
            tombstones = doc.tombstones.copy(
                nameTags = doc.tombstones.nameTags + NameTag(K.NOAH, NameTagKind.CHARACTER.open(), "노아", "NOA", state = NameTagState.PURGED.open(), purgedAt = ExampleClock.NOW),
            ),
        )
        val p = Links.state(purged, noahLink(purged)) as Links.State.Purged
        assertEquals("영구 삭제 · 노아", p.chip)
        assertTrue("되살리기" !in p.buttons)
    }

    @Test
    fun `unlink keeps the text and retarget keeps the key`() {
        val link = noahLink(doc)
        val block = K.block(1, 6, 6)
        val text = doc.block(block).runs.plainText
        val plain = Links.unlink(doc, block, link.linkKey, ctx)
        assertEquals(text, plain.block(block).runs.plainText)
        assertTrue(plain.block(block).runs.none { it is TextRun.Link })
        val moved = Links.retarget(doc, block, link.linkKey, LinkTargetKind.CHARACTER, K.OWEN, ctx)
        val after = moved.block(block).runs.filterIsInstance<TextRun.Link>().single()
        assertEquals(link.linkKey, after.linkKey)
        assertEquals(K.OWEN, after.target.key)
        assertEquals(doc, Links.retarget(doc, block, link.linkKey, LinkTargetKind.FACTION, K.FACTION_LANTERN_SOCIETY, ctx))
    }

    @Test
    fun `pdf prints heads as titles and follows the layer choice`() {
        val times = StoryTimes.source(doc)
        val with = ScriptPdfContent.lines(doc, ExportScope.pdf(listOf(F1.S3, F1.S4)), times, includeDirectionAndParens = true)
        assertEquals(ScriptPdfContent.Line.Title("씬 3 · 결사의 문 · 2일째 새벽 05:40"), with.first())
        assertTrue(with.contains(ScriptPdfContent.Line.Title("씬 4 · 결사의 문 · 2일째 아침 06:10")))
        assertTrue(with.none { it is ScriptPdfContent.Line.Title && it.text.contains("전환 문구") })
        assertTrue(with.any { it is ScriptPdfContent.Line.Body && it.kind == BlockKind.DIRECTION })
        assertTrue(with.contains(ScriptPdfContent.Line.Body("리엔", "(작게) 저기… 불빛이 보여요. 누가 아직 남아 있는 걸까요?", BlockKind.DIALOGUE)))
        val without = ScriptPdfContent.lines(doc, ExportScope.pdf(listOf(F1.S3)), times, includeDirectionAndParens = false)
        assertTrue(without.none { it is ScriptPdfContent.Line.Body && it.kind == BlockKind.DIRECTION })
        assertTrue(without.contains(ScriptPdfContent.Line.Body("리엔", "저기… 불빛이 보여요. 누가 아직 남아 있는 걸까요?", BlockKind.DIALOGUE)))
        val hidden = doc.updateUnit(F1.S4) { it.copy(hidden = true) }
        val left = ScriptPdfContent.lines(hidden, ExportScope.pdf(listOf(F1.E17)), StoryTimes.source(hidden), includeDirectionAndParens = true)
        assertTrue(left.none { it is ScriptPdfContent.Line.Title && it.text.startsWith("씬 4 · 결사의 문 · 2일째 아침") })
    }
}
