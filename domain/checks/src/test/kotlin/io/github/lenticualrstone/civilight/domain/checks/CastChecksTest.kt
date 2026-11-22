package io.github.lenticualrstone.civilight.domain.checks

import io.github.lenticualrstone.civilight.domain.checks.Fx.withCharacter
import io.github.lenticualrstone.civilight.domain.checks.Fx.withNoahLive
import io.github.lenticualrstone.civilight.domain.checks.Fx.withUnit
import io.github.lenticualrstone.civilight.domain.checks.Fx.withoutEmptyVoiceLines
import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.model.checks.AckKind
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Row 5 (`▲ 보이스 비어 있음`) and row 19 (`◇ 숨긴 인물이 보이는 씬에 나옴`, rule 19 §6). */
class CastChecksTest {
    private val doc = Fx.doc()

    @Test
    fun voiceEmptyGoesWhenEveryLineHasText() {
        assertEquals(1, Fx.run(doc).ofType(CheckType.VOICE_EMPTY).size)
        assertTrue(Fx.run(doc.withoutEmptyVoiceLines()).ofType(CheckType.VOICE_EMPTY).isEmpty())
        // Not ackable: the button is `보이스 쓰기` only.
        val item = Fx.run(doc).ofType(CheckType.VOICE_EMPTY).single()
        assertFalse(item.ackable)
        assertEquals(CheckFilter.VOICE, item.filter)
    }

    /** CST TC-19-05 / LIB I-16: 노아 hidden and appearing in visible scenes (unlocked). */
    @Test
    fun tc1905_hiddenCharacterInVisibleScene() {
        val hidden = doc.withNoahLive().withCharacter(K.NOAH) { it.copy(hidden = true) }
        val report = Fx.run(hidden, Fx.inputs(hiddenUnlocked = true))
        val rows = report.ofType(CheckType.HIDDEN_CHARACTER_IN_VISIBLE_SCENE)
        assertEquals(listOf("1-2 씬 2", "1-5 씬 3", "1-6 씬 2", "1-6 씬 4"), rows.map { it.location })
        assertEquals("◇ 숨긴 인물이 보이는 씬에 나옴 · 씬 2", rows[2].title)
        assertEquals(listOf("씬도 숨기기", "그대로"), rows[2].actions.map { it.label })
        assertEquals(listOf(K.SCENE_1_6_2, K.NOAH), rows[2].subjectKeys)
        assertEquals(CheckFilter.SPEAKER, rows[2].filter)
        // The app never hides the scene itself.
        assertFalse(hidden.index.unit(K.SCENE_1_6_2)!!.hidden)

        // `그대로` is a match on scene + character.
        val acked = CheckAcks.acknowledge(hidden, rows[2], Fx.ctx())
        assertEquals(AckKind.MATCH, acked.checks.acknowledgements.last().kind.known)
        assertEquals(3, Fx.run(acked, Fx.inputs(hiddenUnlocked = true)).open.count { it.checkType == CheckType.HIDDEN_CHARACTER_IN_VISIBLE_SCENE })

        // `씬도 숨기기` (done by the author) removes the row of that scene.
        val sceneHidden = hidden.withUnit(K.SCENE_1_6_2) { it.copy(hidden = true) }
        assertEquals(3, Fx.run(sceneHidden, Fx.inputs(hiddenUnlocked = true)).ofType(CheckType.HIDDEN_CHARACTER_IN_VISIBLE_SCENE).size)
    }

    /** CST TC-19-09 / CNV T-CNV-27: results about a locked hidden character collapse into one row. */
    @Test
    fun tc1909_lockedHiddenCharacterRowsCollapse() {
        val hidden = doc.withNoahLive().withCharacter(K.NOAH) { it.copy(hidden = true) }
        val locked = Fx.run(hidden)
        val aboutNoah = locked.open.filter { K.NOAH in it.subjectKeys }
        assertEquals(4, aboutNoah.size)
        assertTrue(aboutNoah.all { it.lockedHidden })
        val rows = CheckCenterView.rows(locked)
        assertEquals("▲ 숨긴 항목 관련 4 · 지문으로 보기", (rows.first() as CheckListRow.HiddenGroup).text)
        assertEquals(1, rows.count { it is CheckListRow.HiddenGroup })
        // Still computed and counted (Q-CK-03 default).
        assertEquals(locked.open.size, locked.count(CheckFilter.ALL))

        val unlocked = CheckCenterView.rows(Fx.run(hidden, Fx.inputs(hiddenUnlocked = true)))
        assertTrue(unlocked.none { it is CheckListRow.HiddenGroup })
        assertEquals(4, unlocked.filterIsInstance<CheckListRow.Item>().count { K.NOAH in it.result.subjectKeys })
    }

    @Test
    fun hiddenSpeakerVoiceRowCollapsesWhileLocked() {
        val hidden = doc.withCharacter(K.RIEN) { it.copy(hidden = true) }
        val voice = Fx.run(hidden).ofType(CheckType.VOICE_EMPTY).single()
        assertTrue(voice.lockedHidden)
        assertFalse(Fx.run(hidden, Fx.inputs(hiddenUnlocked = true)).ofType(CheckType.VOICE_EMPTY).single().lockedHidden)
    }
}
