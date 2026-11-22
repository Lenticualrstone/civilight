package io.github.lenticualrstone.civilight.feature.recorder

import io.github.lenticualrstone.civilight.domain.recorder.inbox.MenuAction
import io.github.lenticualrstone.civilight.domain.recorder.put.DestinationStep
import io.github.lenticualrstone.civilight.domain.recorder.put.PutKind
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordState
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.feature.recorder.inbox.MenuOutcome
import io.github.lenticualrstone.civilight.feature.recorder.put.PutFlows
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.PlacementKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** `⋯ › 메모 칸에 넣기` (LRC-37) and `⋯ › 인물로 만들기` (LRC-38) from the 기록함, over fakes. */
class PutMemoAndCharacterTest {
    private val r1 = ExampleKeys.RECORDER_R1
    private val r3 = ExampleKeys.RECORDER_R3
    private val c2 = ExampleKeys.CHAPTER_2

    @Test
    fun LRC_37_memoFieldSheetStepsProjectThenStructureTree() = runTest {
        val f = InboxFixture(this)
        val env = f.env.env
        // R3 has no link: the project menu first.
        val menu = PutFlows.memoFieldView(env, PutFlows.initialStep(f.recorder.library, r3, env, PutKind.MEMO_FIELD), hiddenOpen = false)
        assertEquals("메모 칸에 넣기", menu.title)
        assertEquals(listOf("잿빛 새벽의 등불"), menu.projects.map { it.label })
        assertTrue(menu.units.isEmpty())

        val tree = PutFlows.memoFieldView(env, DestinationStep.Chosen(Fx.P1), hiddenOpen = false)
        assertEquals("메모 칸에 넣기 · 잿빛 새벽의 등불 ▾", tree.title)
        // 장 · 에피소드 · 씬 in story order, indented by level.
        val keys = tree.units.map { it.unitKey }
        assertEquals(ExampleKeys.CHAPTER_1, keys.first())
        assertTrue(keys.indexOf(ExampleKeys.EPISODE_1_7) < keys.indexOf(Fx.S))
        assertTrue(keys.indexOf(Fx.S) < keys.indexOf(c2))
        val chapter = tree.units.first { it.unitKey == ExampleKeys.CHAPTER_1 }
        val scene = tree.units.first { it.unitKey == Fx.S }
        assertTrue(scene.depth > chapter.depth)
        assertTrue(scene.title.startsWith("1-7 씬 3"))
        assertTrue(tree.units.none { it.locked } || tree.units.filter { it.locked }.all { it.title.endsWith("숨긴 씬") })
    }

    @Test
    fun LRC_37_aHiddenSceneIsMaskedAndLockedUntilTheItemsOpen() = runTest {
        val f = InboxFixture(this)
        val doc = f.projects.doc.let { d ->
            d.copy(story = d.story.copy(units = d.story.units.map { if (it.key == Fx.S) it.copy(hidden = true) else it }))
        }
        val env = Fx.env(doc)
        val locked = PutFlows.memoFieldView(env, DestinationStep.Chosen(Fx.P1), hiddenOpen = false).units.single { it.unitKey == Fx.S }
        assertTrue(locked.locked)
        assertEquals("1-7 씬 3 · 숨긴 씬", locked.title)
        val open = PutFlows.memoFieldView(env, DestinationStep.Chosen(Fx.P1), hiddenOpen = true).units.single { it.unitKey == Fx.S }
        assertFalse(open.locked)
        assertFalse(open.title.contains("숨긴 씬"))
    }

    @Test
    fun LRC_T22_memoFieldIntoChapter2ThenUndo() = runTest {
        val f = InboxFixture(this)
        f.view(this)
        val before = f.projects.doc.index.unit(c2)!!.synopsis
        val done = f.writer.putInMemoField(r3, Fx.P1, c2)!!
        assertEquals("TODO_COPY_LRC_16", done.toast.text)
        assertEquals(listOf("메모 칸에 넣기"), f.projects.labels)
        val after = f.projects.doc.index.unit(c2)!!.synopsis!!
        assertTrue(after.endsWith(ExampleRecorder.R3_TEXT.replace(Regex("""\s+"""), " ").trim()))
        assertFalse(after.contains('\n'))
        val placement = f.recorder.library.placementsOf(r3).single()
        assertEquals(PlacementKind.MEMO_FIELD.open(), placement.kind)
        assertEquals(c2, placement.targetKey)
        val link = f.recorder.library.linksOf(r3).single()
        assertEquals(RecorderTargetKind.UNIT.open(), link.targetKind)
        assertEquals(c2, link.targetKey)
        assertEquals(LinkOrigin.INSERT.open(), link.origin)
        assertEquals(RecordState.PUT, f.view(this).rows.single { it.row.recordKey == r3 }.row.state)

        f.writer.undoPut(done)
        assertEquals(before, f.projects.doc.index.unit(c2)!!.synopsis)
        assertTrue(f.recorder.library.placementsOf(r3).isEmpty())
        assertTrue(f.recorder.library.linksOf(r3).isEmpty())
        assertEquals(RecordState.UNSORTED, f.view(this).rows.single { it.row.recordKey == r3 }.row.state)
    }

    @Test
    fun LRC_37_aSceneGetsANewLineInItsSceneMemo() = runTest {
        val f = InboxFixture(this)
        val before = f.projects.doc.index.unit(Fx.S)!!.memo
        val done = f.writer.putInMemoField(r3, Fx.P1, Fx.S)!!
        val memo = f.projects.doc.index.unit(Fx.S)!!.memo!!
        if (before.isNullOrBlank()) assertFalse(memo.contains('\n')) else assertTrue(memo.startsWith(before.trimEnd() + "\n"))
        assertEquals("1-7 씬 3 · 씬 메모", done.insertion.placement.lastLabel)
    }

    @Test
    fun LRC_T27_makeCharacterThenUndo() = runTest {
        val f = InboxFixture(this)
        f.view(this)
        val view = PutFlows.characterView(f.env.env, DestinationStep.Chosen(Fx.P1))
        assertEquals("인물로 만들기 · 잿빛 새벽의 등불 ▾", view.title)
        // 이름 is required: empty at start, blank refused.
        assertFalse(view.canMake(""))
        assertFalse(view.canMake("  "))
        assertTrue(view.canMake("시장 꼬마"))
        assertFalse(PutFlows.characterView(f.env.env, DestinationStep.ChoosingProject).canMake("시장 꼬마"))

        val done = f.writer.makeCharacter(r3, Fx.P1, "시장 꼬마")!!
        assertEquals("TODO_COPY_LRC_17", done.toast.text)
        assertEquals(listOf("인물로 만들기"), f.projects.labels)
        val character = f.projects.doc.index.character(done.insertion.placement.targetKey)!!
        assertEquals("시장 꼬마", character.name)
        assertEquals("2장 첫 장면은 비 오는 상층 관문에서 시작.", character.oneLiner)
        val overview = character.fields.values.single().payload as FieldPayload.Rich
        assertEquals(ExampleRecorder.R3_TEXT, Runs.text(overview.doc.last().runs))
        assertEquals(PlacementKind.CHARACTER.open(), done.insertion.placement.kind)
        val link = f.recorder.library.linksOf(r3).single()
        assertEquals(character.key, link.targetKey)
        assertEquals(LinkOrigin.INSERT.open(), link.origin)
        assertEquals(RecordState.PUT, f.view(this).rows.single { it.row.recordKey == r3 }.row.state)

        f.writer.undoPut(done)
        assertNull(f.projects.doc.index.character(character.key))
        assertTrue(f.recorder.library.linksOf(r3).isEmpty())
        assertEquals(RecordState.UNSORTED, f.view(this).rows.single { it.row.recordKey == r3 }.row.state)
    }

    @Test
    fun LRC_38_aBlankNameOrAReadOnlyProjectMakesNothing() = runTest {
        val f = InboxFixture(this)
        assertNull(f.writer.makeCharacter(r3, Fx.P1, "   "))
        f.projects.readOnly = true
        assertNull(f.writer.makeCharacter(r3, Fx.P1, "시장 꼬마"))
        assertNull(f.writer.putInMemoField(r3, Fx.P1, c2))
        assertTrue(f.recorder.library.placementsOf(r3).isEmpty())
    }

    @Test
    fun LRC_T26_aProtectedDestinationWaitsInTheMemoFieldAndCharacterSheets() = runTest {
        val f = InboxFixture(this)
        val r18 = f.projects.doc.let { it.copy(project = it.project.copy(rating = Rating.R18.open())) }
        val env = Fx.env(r18)
        for (kind in listOf(PutKind.MEMO_FIELD, PutKind.CHARACTER)) {
            assertEquals(DestinationStep.AwaitingAuth(Fx.P1), PutFlows.initialStep(f.recorder.library, r1, env, kind))
        }
        assertTrue(PutFlows.memoFieldView(env, DestinationStep.AwaitingAuth(Fx.P1), hiddenOpen = false).units.isEmpty())
        assertFalse(PutFlows.characterView(env, DestinationStep.AwaitingAuth(Fx.P1)).canMake("시장 꼬마"))
    }

    @Test
    fun LRC_29_memoFieldAndCharacterOpenTheirSheetsFromTheMenu() = runTest {
        val f = InboxFixture(this)
        f.view(this)
        val menu = f.controller.menu(r1)!!
        assertTrue(menu.options.single { it.option.action == MenuAction.PUT_MEMO_FIELD }.enabled)
        assertTrue(menu.options.single { it.option.action == MenuAction.MAKE_CHARACTER }.enabled)
        assertEquals(MenuOutcome.OpenPut(r1, PutKind.MEMO_FIELD), f.controller.onMenu(r1, MenuAction.PUT_MEMO_FIELD))
        assertEquals(MenuOutcome.OpenPut(r1, PutKind.CHARACTER), f.controller.onMenu(r1, MenuAction.MAKE_CHARACTER))
    }
}
