package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.cast.voice.ArchiveDocs
import io.github.lenticualrstone.civilight.domain.cast.voice.VoiceLineState
import io.github.lenticualrstone.civilight.domain.cast.voice.VoiceLines
import io.github.lenticualrstone.civilight.feature.cast.gametext.GameTextModel
import io.github.lenticualrstone.civilight.feature.cast.settings.FieldSettingsModel
import io.github.lenticualrstone.civilight.feature.cast.settings.HeaderFixed
import io.github.lenticualrstone.civilight.model.text.DocBlock
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.domain.cast.field.Profile
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** `게임 텍스트` edits (CST-16, CST-17) and the computed header fields' switches (CST-14). */
class GameTextModelTest {
    private val doc = CastFx.doc()

    private fun text(s: String) = listOf(DocBlock(key = "b1", order = "a0", runs = listOf(TextRun.Plain(s))))

    @Test
    fun `CST-16 + adds a line at the end of the game order with its text`() {
        val before = VoiceLines.rows(doc, K.RIEN)
        val (next, key) = GameTextModel.addLine(doc, K.RIEN, "작별", text("또 만나요."), CastFx.ctx())!!
        val rows = VoiceLines.rows(next, K.RIEN)
        assertEquals(before.size + 1, rows.size)
        assertEquals(key, rows.last().key)
        assertEquals("작별", rows.last().title)
        assertEquals(VoiceLineState.WRITING, rows.last().state)
        assertNull(GameTextModel.addLine(doc, K.RIEN, " ", emptyList(), CastFx.ctx()))
    }

    @Test
    fun `CST-16 saving a line keeps its state, an emptied line reads 비어 있음`() {
        val line = VoiceLines.rows(doc, K.RIEN).first { it.state != VoiceLineState.EMPTY }
        val emptied = GameTextModel.saveLine(doc, line.key, line.title, emptyList(), CastFx.ctx())
        assertEquals(VoiceLineState.EMPTY, VoiceLines.rows(emptied, K.RIEN).single { it.key == line.key }.state)
        assertEquals(VoiceLines.progress(doc, K.RIEN).filled - 1, VoiceLines.progress(emptied, K.RIEN).filled)
    }

    @Test
    fun `CST-17 a new archive document is made on its first save only`() {
        assertNull(GameTextModel.saveArchive(doc, K.RIEN, null, "", emptyList(), CastFx.ctx()))
        val before = ArchiveDocs.docs(doc, K.RIEN).size
        val (made, key) = GameTextModel.saveArchive(doc, K.RIEN, null, "새 기록", text("첫 줄"), CastFx.ctx())!!
        assertEquals(before + 1, ArchiveDocs.docs(made, K.RIEN).size)
        val view = GameTextModel.archiveView(made, K.RIEN, key)
        assertEquals("새 기록", view.title)
        assertEquals("3자", view.count) // spaces count, line breaks do not (CST-17 [PROPOSED])
        assertNull(view.unlock)
        // Saving again updates the same document.
        val (again, sameKey) = GameTextModel.saveArchive(made, K.RIEN, key, "새 기록", text("첫 줄이다"), CastFx.ctx())!!
        assertEquals(key, sameKey)
        assertEquals(before + 1, ArchiveDocs.docs(again, K.RIEN).size)
    }

    @Test
    fun `CST-17 the unlock point reads X 이후 열림`() {
        val withUnlock = GameTextModel.setUnlock(doc, K.RIEN, K.ARCHIVE_RIEN_JOURNAL, K.EPISODE_1_7, CastFx.ctx())
        assertEquals("1-7 이후 열림", GameTextModel.archiveView(withUnlock, K.RIEN, K.ARCHIVE_RIEN_JOURNAL).unlock)
        assertEquals("비어 있음", GameTextModel.archiveView(doc, K.RIEN, K.ARCHIVE_RIEN_BLACKOUT).count)
    }

    @Test
    fun `CST-14 첫 등장 and 보이스 can leave the record header, never the record`() {
        val header = { d: io.github.lenticualrstone.civilight.model.doc.ProjectDoc -> Profile.header(d, K.RIEN, LensPoint.Latest, cover = false).rows.map { it.label } }
        assertTrue("첫 등장" in header(doc))
        assertTrue(FieldSettingsModel.showsInHeader(doc, HeaderFixed.FIRST_APPEARANCE))
        val hidden = FieldSettingsModel.setShowInHeader(doc, HeaderFixed.FIRST_APPEARANCE, false, CastFx.ctx())
        assertFalse("첫 등장" in header(hidden))
        assertTrue("보이스" in header(hidden))
        assertEquals(hidden, FieldSettingsModel.setShowInHeader(hidden, HeaderFixed.FIRST_APPEARANCE, false, CastFx.ctx()))
    }
}
