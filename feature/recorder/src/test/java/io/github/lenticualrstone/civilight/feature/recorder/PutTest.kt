package io.github.lenticualrstone.civilight.feature.recorder

import io.github.lenticualrstone.civilight.domain.recorder.put.DestinationStep
import io.github.lenticualrstone.civilight.domain.recorder.put.Destinations
import io.github.lenticualrstone.civilight.domain.recorder.put.ManuscriptInsert
import io.github.lenticualrstone.civilight.domain.recorder.put.ManuscriptSpot
import io.github.lenticualrstone.civilight.domain.recorder.put.PositionRow
import io.github.lenticualrstone.civilight.domain.recorder.put.PutKind
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordState
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.feature.recorder.put.PutFlows
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.script.BlockKind
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `원고에 넣기` from the 기록함 (LRC-32, LRC-33) over fakes. The example fixture's 1-7 counter gives
 * #058 where the spec's fixture gives #062 (fixture.md conflict 3), so these are the `_fixture` twins.
 */
class PutTest {
    private val r1 = ExampleKeys.RECORDER_R1
    private val r3 = ExampleKeys.RECORDER_R3

    @Test
    fun LRC_T16_positionSheetRowsAndNothingPreselected() = runTest {
        val f = InboxFixture(this)
        val env = f.env.env
        val step = PutFlows.initialStep(f.recorder.library, r1, env, PutKind.MANUSCRIPT)
        assertEquals(DestinationStep.Chosen(Fx.P1), step)
        val view = PutFlows.view(f.recorder.library, r1, env, step)
        assertEquals("원고에 넣기 · 잿빛 새벽의 등불 ▾", view.title)
        assertEquals(listOf("연결된 씬 · 1-7 씬 3 · 맨 끝", "마지막으로 쓰던 곳 · 1-7 씬 3 · #044 뒤", "씬 고르기 ›"), view.rows.map { it.label })
        assertEquals(ExampleKeys.BLOCK_044, (view.rows[1] as PositionRow.LastWriting).blockKey)
        // Nothing was inserted by opening the sheet.
        assertTrue(f.recorder.library.placementsOf(r1).isEmpty())
        assertTrue(f.projects.labels.isEmpty())
    }

    @Test
    fun LRC_33_aRecordWithoutALinkedProjectOpensTheProjectMenu() = runTest {
        val f = InboxFixture(this)
        val step = PutFlows.initialStep(f.recorder.library, r3, f.env.env, PutKind.MANUSCRIPT)
        assertEquals(DestinationStep.ChoosingProject, step)
        val view = PutFlows.view(f.recorder.library, r3, f.env.env, step)
        assertEquals("원고에 넣기", view.title)
        assertEquals(listOf("잿빛 새벽의 등불"), view.projects.map { it.label })
        assertTrue(view.rows.isEmpty())
    }

    @Test
    fun LRC_T17_fixture_insertFromTheInboxThenUndo() = runTest {
        val f = InboxFixture(this)
        f.view(this)
        val done = f.writer.putInManuscript(r1, Fx.P1, ManuscriptSpot.AfterBlock(ExampleKeys.BLOCK_044))!!
        val block = f.projects.doc.index.block(done.insertion.placement.targetKey)!!
        assertEquals(BlockKind.MEMO.open(), block.kind)
        assertEquals(58, block.number)
        assertEquals(Fx.R1_TEXT, Runs.text(block.runs))
        assertEquals("1-7 씬 3 · #058 작가 메모 넣음", done.toast.text)
        assertEquals(listOf("원고에 넣기"), f.projects.labels)
        // S was linked already: no link added; the row reads 넣음 at once (the env took the new doc).
        assertEquals(2, f.recorder.library.linksOf(r1).size)
        assertEquals(RecordState.PUT, f.view(this).rows.single { it.row.recordKey == r1 }.row.state)
        assertEquals(listOf(Fx.P1), f.env.forgotten)

        // `이미 넣음` appears from the second time.
        val again = PutFlows.view(f.recorder.library, r1, f.controller.env.value!!, DestinationStep.Chosen(Fx.P1))
        assertEquals("이미 넣음 · 1-7 씬 3 · #058", again.rows.last().label)

        f.writer.undoPut(done)
        assertNull(f.projects.doc.index.block(block.key))
        assertTrue(f.recorder.library.placementsOf(r1).isEmpty())
        assertEquals(RecordState.UNSORTED, f.view(this).rows.single { it.row.recordKey == r1 }.row.state)
        val unit = f.projects.doc.index.numberingUnitKeyOfScene(Fx.S)!!
        assertEquals(59, ManuscriptInsert.nextNumber(f.projects.doc, unit))
    }

    @Test
    fun LRC_T14_T21_insertIntoADoneRecordAddsTheSceneLinkAndUndoRestoresDone() = runTest {
        val f = InboxFixture(this)
        val r4 = ExampleKeys.RECORDER_R4
        f.view(this)
        val done = f.writer.putInManuscript(r4, Fx.P1, ManuscriptSpot.SceneEnd(Fx.S))!!
        val record = f.recorder.library.record(r4)!!
        assertEquals(false, record.doneMarked)
        val added = f.recorder.library.linksOf(r4).single()
        assertEquals(LinkOrigin.INSERT.open(), added.origin)
        assertEquals(Fx.S, added.targetKey)
        assertEquals(RecordState.PUT, f.view(this).rows.single { it.row.recordKey == r4 }.row.state)
        f.writer.undoPut(done)
        assertTrue(f.recorder.library.linksOf(r4).isEmpty())
        assertEquals(RecordState.DONE, f.view(this).rows.single { it.row.recordKey == r4 }.row.state)
    }

    @Test
    fun LRC_32_aPassedPromptIsNotAskedAgainAndCancelReopensTheMenu() = runTest {
        val f = InboxFixture(this)
        val r18 = f.projects.doc.let { it.copy(project = it.project.copy(rating = Rating.R18.open())) }
        val env = Fx.env(r18)
        assertEquals(Fx.P1, PutFlows.initialProject(f.recorder.library, r1, env))
        assertEquals(DestinationStep.AwaitingAuth(Fx.P1), PutFlows.step(env, Fx.P1, authedKey = null, kind = PutKind.MANUSCRIPT))
        assertEquals(DestinationStep.Chosen(Fx.P1), PutFlows.step(env, Fx.P1, authedKey = Fx.P1, kind = PutKind.MANUSCRIPT))
        assertEquals(DestinationStep.ChoosingProject, PutFlows.step(env, null, authedKey = Fx.P1, kind = PutKind.MANUSCRIPT))
        // An unprotected project needs no prompt; an unknown one sends the user to the menu.
        assertEquals(DestinationStep.Chosen(Fx.P1), PutFlows.step(f.env.env, Fx.P1, null, PutKind.MANUSCRIPT))
        assertEquals(DestinationStep.ChoosingProject, PutFlows.step(f.env.env, "01JZ000000TSTK000000000999", null, PutKind.MANUSCRIPT))
    }

    @Test
    fun LRC_33_aReadOnlyProjectTakesNothing() = runTest {
        val f = InboxFixture(this)
        f.projects.readOnly = true
        assertNull(f.writer.putInManuscript(r1, Fx.P1, ManuscriptSpot.SceneEnd(Fx.S)))
        assertTrue(f.recorder.library.placementsOf(r1).isEmpty())
    }

    @Test
    fun LRC_T26_protectedDestinationWaitsForThePrompt() = runTest {
        val f = InboxFixture(this)
        val r18 = f.projects.doc.let { it.copy(project = it.project.copy(rating = Rating.R18.open())) }
        val env = Fx.env(r18)
        val step = PutFlows.initialStep(f.recorder.library, r1, env, PutKind.MANUSCRIPT)
        assertEquals(DestinationStep.AwaitingAuth(Fx.P1), step)
        assertTrue(PutFlows.view(f.recorder.library, r1, env, step).rows.isEmpty())
        assertEquals(DestinationStep.ChoosingProject, Destinations.authResult(step, success = false))
        val chosen = Destinations.authResult(step, success = true)
        assertEquals(DestinationStep.Chosen(Fx.P1), chosen)
        assertNotNull(PutFlows.view(f.recorder.library, r1, env, chosen).rows.firstOrNull())
    }

    // ---- 카드에 넣기 (LRC-35) and 페이지로 만들기 (LRC-36) ----

    @Test
    fun LRC_35_cardSheetStepsProjectThenCardThenTab() = runTest {
        val f = InboxFixture(this)
        val env = f.env.env
        val step = PutFlows.initialStep(f.recorder.library, r1, env, PutKind.CARD)
        val list = PutFlows.cardView(env, step, "", null)
        assertEquals("카드에 넣기 · 잿빛 새벽의 등불 ▾", list.title)
        assertNull(list.card)
        // 노아 is in the trash: never offered; search by name and 초성.
        assertTrue(list.cards.none { it.choice.target.key == ExampleKeys.NOAH })
        assertEquals(listOf("하르트"), PutFlows.cardView(env, step, "하르", null).cards.map { it.choice.name })
        assertTrue(PutFlows.cardView(env, step, "ㅎㄹㅌ", null).cards.any { it.choice.name == "하르트" })
        val hart = list.cards.first { it.choice.target.key == ExampleKeys.HART }
        assertEquals("인물", hart.kindWord)
        val tabs = PutFlows.cardView(env, step, "", hart.choice.target)
        assertEquals(hart, tabs.card)
        assertEquals(listOf("개요", "말투", "비밀"), tabs.tabs.take(3).map { it.label })
        // A card of another project never stays chosen.
        assertNull(PutFlows.cardView(env, step, "", hart.choice.target.copy(projectKey = "01JZ000000TSTK000000000999")).card)
        // The menu when the record has no single project.
        val menu = PutFlows.cardView(env, PutFlows.initialStep(f.recorder.library, r3, env, PutKind.CARD), "", null)
        assertEquals("카드에 넣기", menu.title)
        assertEquals(listOf("잿빛 새벽의 등불"), menu.projects.map { it.label })
    }

    @Test
    fun LRC_35_cardInsertAppendsAMemoParagraphAndUndoRemovesIt() = runTest {
        val f = InboxFixture(this)
        f.view(this)
        val hart = io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef(Fx.P1, io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind.CHARACTER, ExampleKeys.HART)
        val done = f.writer.putInCard(r3, Fx.P1, hart, io.github.lenticualrstone.civilight.domain.recorder.put.CardDocs.SPEECH)!!
        assertEquals("TODO_COPY_LRC_13", done.toast.text)
        assertEquals(listOf("카드에 넣기"), f.projects.labels)
        val placement = f.recorder.library.placementsOf(r3).single()
        assertEquals("하르트 · 말투", placement.lastLabel)
        // R3 had no link: the card is linked by the insert.
        assertEquals(listOf(ExampleKeys.HART), f.recorder.library.linksOf(r3).map { it.targetKey })
        assertEquals(LinkOrigin.INSERT.open(), f.recorder.library.linksOf(r3).single().origin)
        assertEquals(RecordState.PUT, f.view(this).rows.single { it.row.recordKey == r3 }.row.state)
        f.writer.undoPut(done)
        assertTrue(f.recorder.library.placementsOf(r3).isEmpty())
        assertTrue(f.recorder.library.linksOf(r3).isEmpty())
        assertEquals(RecordState.UNSORTED, f.view(this).rows.single { it.row.recordKey == r3 }.row.state)
    }

    @Test
    fun LRC_T21_makePageWithThePrefilledNameAddsThePageLink() = runTest {
        val f = InboxFixture(this)
        f.view(this)
        val view = PutFlows.pageView(f.recorder.library, r3, f.env.env, DestinationStep.Chosen(Fx.P1))
        assertEquals("2장 첫 장면은 비 오는 상층 관문에서 시작", view.prefill)
        assertEquals("페이지로 만들기 · 잿빛 새벽의 등불 ▾", view.title)
        assertTrue(view.canMake(""))
        assertTrue(view.canMake(view.prefill))
        val done = f.writer.makePage(r3, Fx.P1, view.prefill)!!
        assertEquals("TODO_COPY_LRC_15", done.toast.text)
        val page = f.projects.doc.pages.getValue(done.insertion.placement.targetKey)
        assertEquals("2장 첫 장면은 비 오는 상층 관문에서 시작", page.name)
        val link = f.recorder.library.linksOf(r3).single()
        assertEquals(page.key, link.targetKey)
        assertEquals(LinkOrigin.INSERT.open(), link.origin)
        assertEquals(RecordState.PUT, f.view(this).rows.single { it.row.recordKey == r3 }.row.state)
        f.writer.undoPut(done)
        assertNull(f.projects.doc.pages[page.key])
        assertTrue(f.recorder.library.linksOf(r3).isEmpty())
    }

    @Test
    fun LRC_T24_atRunsStayLinksInAPageOfTheSameProject() = runTest {
        val f = InboxFixture(this)
        f.view(this)
        val done = f.writer.makePage(ExampleKeys.RECORDER_R2, Fx.P1, "")!!
        val page = f.projects.doc.pages.getValue(done.insertion.placement.targetKey)
        val runs = page.blocks.single().runs
        assertTrue(runs.any { it is io.github.lenticualrstone.civilight.model.text.TextRun.Link && it.text == "세라" })
    }

    @Test
    fun LRC_T26_aProtectedDestinationWaitsInEveryPutSheet() = runTest {
        val f = InboxFixture(this)
        val r18 = f.projects.doc.let { it.copy(project = it.project.copy(rating = Rating.R18.open())) }
        val env = Fx.env(r18)
        for (kind in listOf(PutKind.CARD, PutKind.PAGE)) {
            val step = PutFlows.initialStep(f.recorder.library, r1, env, kind)
            assertEquals(DestinationStep.AwaitingAuth(Fx.P1), step)
        }
        assertTrue(PutFlows.cardView(env, DestinationStep.AwaitingAuth(Fx.P1), "", null).cards.isEmpty())
        assertFalse(PutFlows.pageView(f.recorder.library, r1, env, DestinationStep.AwaitingAuth(Fx.P1)).canMake("이름"))
    }

    @Test
    fun LRC_32_menuAndRowTagsRunCardAndPageNow() = runTest {
        val f = InboxFixture(this)
        val row = f.view(this).rows.single { it.row.recordKey == r1 }
        assertTrue(row.actions.putManuscript)
        assertTrue(row.actions.putCard)
        assertTrue(row.actions.makePage)
        assertEquals(io.github.lenticualrstone.civilight.feature.recorder.inbox.MenuOutcome.OpenPut(r1, PutKind.CARD), f.controller.onMenu(r1, io.github.lenticualrstone.civilight.domain.recorder.inbox.MenuAction.PUT_CARD))
        assertEquals(io.github.lenticualrstone.civilight.feature.recorder.inbox.MenuOutcome.OpenPut(r1, PutKind.PAGE), f.controller.onMenu(r1, io.github.lenticualrstone.civilight.domain.recorder.inbox.MenuAction.MAKE_PAGE))
    }
}
