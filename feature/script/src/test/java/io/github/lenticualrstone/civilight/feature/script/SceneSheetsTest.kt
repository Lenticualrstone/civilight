package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.SceneActions
import io.github.lenticualrstone.civilight.domain.story.scene.MergeSide
import io.github.lenticualrstone.civilight.domain.story.scene.SilentCastChoice
import io.github.lenticualrstone.civilight.domain.story.scene.TransitionOnMerge
import io.github.lenticualrstone.civilight.feature.script.scenes.ChangeLine
import io.github.lenticualrstone.civilight.feature.script.scenes.MergeChoices
import io.github.lenticualrstone.civilight.feature.script.scenes.MergeModel
import io.github.lenticualrstone.civilight.feature.script.scenes.MergeSheetModel
import io.github.lenticualrstone.civilight.feature.script.scenes.PlaceMode
import io.github.lenticualrstone.civilight.feature.script.scenes.SplitChoices
import io.github.lenticualrstone.civilight.feature.script.scenes.SplitModel
import io.github.lenticualrstone.civilight.feature.script.state.EditorRules
import io.github.lenticualrstone.civilight.feature.script.state.EditorState
import io.github.lenticualrstone.civilight.feature.script.state.ScriptOps
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.navigation.NewUnitSheet
import io.github.lenticualrstone.civilight.navigation.SceneSplitSheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** SCR-49 … SCR-53 on the example project (1-7 씬 3 holds #036–#048; 씬 4 is empty, based on 씬 3, `직접` text). */
class SceneSheetsTest {
    private val doc = Fx.example

    private fun ctx(): ActionContext {
        var n = 0
        return ActionContext(now = ExampleClock.NOW, zone = Fx.zone, device = "test", newKey = { "01JZ000000SCNT" + (++n).toString().padStart(12, '0') })
    }

    private fun numbers(d: ProjectDoc, scene: String): List<Int> = d.manuscript.liveBlocksOf(scene).map { it.number }

    // --- SCR-49 routing (T-22-08) ---

    @Test
    fun `T-22-08 slash scene routes by the position and never changes anything`() {
        fun route(current: String?, checked: List<String> = emptyList(), offset: Int? = null) =
            ScriptOps.newSceneSheet(doc, K.PROJECT, current, K.SCENE_1_7_3, K.EPISODE_1_7, checked, offset)
        assertEquals(SceneSplitSheet(K.PROJECT, K.BLOCK_044), route(K.BLOCK_044))
        assertEquals(NewUnitSheet(K.PROJECT, parentKey = K.EPISODE_1_7, afterKey = K.SCENE_1_7_3), route(K.BLOCK_048))
        // Checked #042 … #044: below the last checked block, whatever holds the caret.
        assertEquals(SceneSplitSheet(K.PROJECT, K.BLOCK_044), route(K.BLOCK_036, listOf(K.BLOCK_042, K.BLOCK_043, K.BLOCK_044)))
        // The caret at the very start of the scene's first block: only `앞에 새 씬` (`ED`).
        assertEquals(NewUnitSheet(K.PROJECT, parentKey = K.EPISODE_1_7, beforeKey = K.SCENE_1_7_3), route(K.BLOCK_036, offset = 0))
        assertEquals(SceneSplitSheet(K.PROJECT, K.BLOCK_036), route(K.BLOCK_036, offset = 3))
        // An empty first (and last) block, 1-8 씬 1 #001: offset 0 is its only offset, so `다음 씬 만들기`.
        assertEquals(
            NewUnitSheet(K.PROJECT, parentKey = K.EPISODE_1_8, afterKey = K.SCENE_1_8_1),
            ScriptOps.newSceneSheet(doc, K.PROJECT, K.BLOCK_1_8_001, K.SCENE_1_8_1, K.EPISODE_1_8, caretOffset = 0),
        )
        // The empty 씬 4 (virtual line): the next scene after it.
        assertEquals(
            NewUnitSheet(K.PROJECT, parentKey = K.EPISODE_1_7, afterKey = K.SCENE_1_7_4),
            ScriptOps.newSceneSheet(doc, K.PROJECT, null, K.SCENE_1_7_4, K.EPISODE_1_7),
        )
    }

    @Test
    fun `SCR-49 a scene made by the sheet is found and takes the caret line`() {
        val known = NewSceneFollow.known(doc, K.EPISODE_1_7)
        assertEquals(setOf(K.SCENE_1_7_1, K.SCENE_1_7_2, K.SCENE_1_7_3, K.SCENE_1_7_4), known)
        // The empty 씬 4 was there before: it is not "new".
        assertNull(NewSceneFollow.created(doc, K.EPISODE_1_7, known))
        val (after, key) = SceneActions.create(doc, K.EPISODE_1_7, K.SCENE_1_7_3, ctx())
        assertEquals(key, NewSceneFollow.created(after, K.EPISODE_1_7, known))
        // Another episode's new scene is not followed.
        assertNull(NewSceneFollow.created(after, K.EPISODE_1_7, known + key))
        val editor = EditorRules.editVirtual(EditorState(), key)
        assertEquals(key, editor.virtualScene)
    }

    // --- SCR-50 split (T-22-09, T-22-10, T-22-11) ---

    @Test
    fun `T-22-09 the split sheet below 044 reads as RSSplit`() {
        val model = SplitModel.of(doc, K.BLOCK_044, SplitChoices(), unlocked = false, readOnly = false, ctx = ctx())!!
        assertEquals("씬 나누기", model.title)
        assertEquals("1-7 씬 3 · #044 아래", model.sub)
        val (a, b) = model.parts
        assertEquals(listOf("씬 3", "블록 9개", "‘세라 · 불은 꺼졌어…’ ~ ‘…한 번 깜박였다.’"), listOf(a.title, a.count, a.range))
        assertEquals(listOf("새 씬 4", "블록 4개", "‘리엔 · 하르트 씨, 방금…’ ~ ‘…결사의 신호였어요.’"), listOf(b.title, b.count, b.range))
        assertTrue(b.isNew)
        assertEquals("결사의 문", model.place.value)
        assertEquals("· 씬 3과 같음", model.place.note)
        assertEquals("씬 3 기준 · 같은 때", model.time.value)
        assertTrue(model.timeChoice)
        assertEquals("미정", model.titleLine.value)
        val silent = model.silentRows.single()
        assertEquals("말 없는 등장 · 마르타", silent.label)
        assertEquals(listOf("씬 3", "새 씬 4", "둘 다"), silent.chips)
        assertEquals(2, silent.selected)
        val changes = model.changes
        assertEquals(ChangeLine("순번", "씬 4 · 미정 → 씬 5"), changes.first())
        assertTrue(changes.toString(), ChangeLine("첫 공개", "오리지늄 등불 · 씬 3 #044 그대로") in changes)
        assertTrue(changes.toString(), ChangeLine("◆ 관계", "리엔 → 하르트 · #043 그대로") in changes)
        // Reveals before `◆` records, as the board lists them.
        assertTrue(changes.indexOfFirst { it.label == "첫 공개" } < changes.indexOfFirst { it.label == "◆ 관계" })
        assertEquals("씬 5 때", changes.last().label)
        assertTrue(changes.last().value, changes.last().value.startsWith("기준 씬 3 · ") && changes.last().value.endsWith("그대로"))
        assertFalse(model.flashbackRow)
        assertFalse(model.hiddenRow)
    }

    @Test
    fun `T-22-10 records on blocks that move name their new location`() {
        val model = SplitModel.of(doc, K.BLOCK_042, SplitChoices(), unlocked = false, readOnly = false, ctx = ctx())!!
        assertTrue(model.changes.toString(), ChangeLine("◆ 관계", "1-7 씬 3 #043 → 1-7 씬 4 #043") in model.changes)
        assertTrue(model.changes.toString(), ChangeLine("첫 공개", "1-7 씬 3 #044 → 1-7 씬 4 #044") in model.changes)
    }

    @Test
    fun `T-22-09 split applies through SceneActions, keeps numbers, and writes the sheet's choices`() {
        val after = SplitModel.apply(doc, K.BLOCK_044, SplitChoices(), ctx())
        assertEquals((36..44).toList(), numbers(after, K.SCENE_1_7_3))
        val scenes = after.manuscript.liveScenes.filter { it.parentKey == K.EPISODE_1_7 }
        assertEquals(5, scenes.size)
        val b = scenes[3]
        assertEquals((45..48).toList(), numbers(after, b.key))
        assertEquals(doc.index.unit(K.SCENE_1_7_3)!!.place, b.place)
        assertTrue(b.time is SceneTime.Relative)
        assertEquals(listOf(K.SCENE_1_7_4), scenes.drop(4).map { it.key })

        val chosen = SplitChoices(place = PlaceMode.CLEARED, sameTime = false, title = "문 앞", silentCast = mapOf(K.MARTHA to SilentCastChoice.FIRST))
        val edited = SplitModel.apply(doc, K.BLOCK_044, chosen, ctx())
        val nb = edited.manuscript.liveScenes.filter { it.parentKey == K.EPISODE_1_7 }[3]
        assertNull(nb.place)
        assertEquals(SceneTime.None, nb.time)
        assertEquals("문 앞", nb.title)
        assertTrue(nb.silentCast.isEmpty())
        // The sheet reflects the choices before `나누기`.
        val model = SplitModel.of(doc, K.BLOCK_044, chosen, unlocked = false, readOnly = false, ctx = ctx())!!
        assertEquals(listOf("장소 없음", "때 없음", "문 앞"), listOf(model.place.value, model.time.value, model.titleLine.value))
        assertEquals(0, model.silentRows.single().selected)
    }

    @Test
    fun `SCR-50 a stale or last-block split writes nothing and a read-only project cannot split`() {
        assertNull(SplitModel.of(doc, K.BLOCK_048, SplitChoices(), unlocked = false, readOnly = false, ctx = ctx()))
        assertEquals(doc, SplitModel.apply(doc, K.BLOCK_048, SplitChoices(), ctx()))
        assertEquals(doc, SplitModel.apply(doc, "no-such-block", SplitChoices(), ctx()))
        assertFalse(SplitModel.of(doc, K.BLOCK_044, SplitChoices(), unlocked = false, readOnly = true, ctx = ctx())!!.canApply)
    }

    @Test
    fun `T-22-11 a hidden scene's split shows the 숨김 row and a locked hidden speaker is masked in the range`() {
        val hidden = doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == K.SCENE_1_7_3) it.copy(hidden = true) else it }))
        assertTrue(SplitModel.of(hidden, K.BLOCK_044, SplitChoices(), unlocked = true, readOnly = false, ctx = ctx())!!.hiddenRow)
        val seraHidden = doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.SERA) it.copy(hidden = true) else it }))
        val locked = SplitModel.of(seraHidden, K.BLOCK_044, SplitChoices(), unlocked = false, readOnly = false, ctx = ctx())!!
        assertTrue(locked.parts.first().range, locked.parts.first().range.startsWith("‘숨긴 인물 · 불은 꺼졌어…’"))
        val open = SplitModel.of(seraHidden, K.BLOCK_044, SplitChoices(), unlocked = true, readOnly = false, ctx = ctx())!!
        assertTrue(open.parts.first().range.startsWith("‘세라 · "))
    }

    // --- SCR-51 merge (T-22-13, T-22-16, T-22-17) ---

    @Test
    fun `T-22-13 the merge sheet of 씬 4 reads as RSMerge`() {
        val model = MergeModel.of(doc, K.SCENE_1_7_4, MergeChoices(), readOnly = false, ctx = ctx()) as MergeSheetModel.Ready
        assertEquals("씬 합치기", model.title)
        assertEquals("1-7 씬 3 + 씬 4 → 씬 3", model.sub)
        assertEquals("결사의 문", model.place!!.value)
        assertEquals("· 씬 3 · 씬 4 같음", model.place!!.note)
        assertEquals("때 · 하나 고르기", model.timeHeader)
        assertEquals(listOf("씬 3 것", "씬 4 것"), model.timeRadios.map { it.title })
        assertEquals("2일째 새벽 05:40", model.timeRadios[0].sub)
        assertEquals("2일째 아침 06:10", model.timeRadios[1].sub)
        assertEquals(listOf(true, false), model.timeRadios.map { it.selected })
        assertEquals("고르지 않은 때 → 씬 메모 한 줄", model.timeNote)
        assertEquals("전환 문구 · 씬 4 직접", model.transitionLine)
        assertEquals("‘결사의 문 앞 · 06:10’", model.transitionText)
        assertEquals(0, model.transitionIndex)
        assertTrue(model.changes.toString(), ChangeLine("제목", "꺼진 등불 · 씬 4 제목 없음") in model.changes)
        assertTrue(ChangeLine("기록", "블록에 그대로 · 위치 이름만 씬 4 → 씬 3") in model.changes)
        // The choices are radio rows: picking 씬 4's time moves the selection.
        val second = MergeModel.of(doc, K.SCENE_1_7_4, MergeChoices(time = MergeSide.SECOND), readOnly = false, ctx = ctx()) as MergeSheetModel.Ready
        assertEquals(listOf(false, true), second.timeRadios.map { it.selected })
    }

    @Test
    fun `T-22-13 and T-22-17 merge keeps the direct text as system text or drops it`() {
        val kept = MergeModel.apply(doc, K.SCENE_1_7_4, MergeChoices(), ctx())
        assertNull(kept.index.unit(K.SCENE_1_7_4)?.takeIf { kept.manuscript.isLiveUnit(it.key) })
        val last = kept.manuscript.liveBlocksOf(K.SCENE_1_7_3).last()
        assertEquals(BlockKind.SYSTEM, last.kind.known)
        assertEquals(58, last.number)
        assertTrue(kept.index.unit(K.SCENE_1_7_3)!!.memo.orEmpty().contains("합치기 전 · 씬 4 · 결사의 문 · 2일째 아침 06:10"))
        val dropped = MergeModel.apply(doc, K.SCENE_1_7_4, MergeChoices(transition = TransitionOnMerge.DISCARD), ctx())
        assertEquals((36..48).toList(), numbers(dropped, K.SCENE_1_7_3))
    }

    @Test
    fun `T-22-16 a first scene is not offered and a mixed hidden pair is blocked`() {
        assertEquals(MergeSheetModel.NotOffered, MergeModel.of(doc, K.SCENE_1_8_1, MergeChoices(), readOnly = false, ctx = ctx()))
        assertEquals(doc, MergeModel.apply(doc, K.SCENE_1_8_1, MergeChoices(), ctx()))
        val hidden = doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == K.SCENE_1_7_4) it.copy(hidden = true) else it }))
        val blocked = MergeModel.of(hidden, K.SCENE_1_7_4, MergeChoices(), readOnly = false, ctx = ctx())
        assertEquals(MergeSheetModel.Blocked("씬 합치기", "숨긴 씬이 섞임 · 먼저 숨김 풀기"), blocked)
        assertEquals(hidden, MergeModel.apply(hidden, K.SCENE_1_7_4, MergeChoices(), ctx()))
        val readOnly = MergeModel.of(doc, K.SCENE_1_7_4, MergeChoices(), readOnly = true, ctx = ctx()) as MergeSheetModel.Ready
        assertFalse(readOnly.canApply)
    }

    // --- SCR-52 / SCR-53 (story's sheet and action; the script follows) ---

    @Test
    fun `SCR-52 deleting the caret's scene takes the caret off and keeps other scenes`() {
        val result = SceneActions.delete(doc, K.SCENE_1_7_3, ctx())
        assertNotNull(result)
        val after = result!!.doc
        val editing = EditorRules.select(EditorState(), K.BLOCK_044)
        assertNull(EditorRules.reconcile(editing, after).selected)
        assertTrue(after.manuscript.isLiveUnit(K.SCENE_1_7_4))
        assertTrue(result.toast.message, result.toast.message.startsWith("씬 3 지움"))
    }

    @Test
    fun `SCR-53 a scene made before 씬 3 is followed and the ordinals shift`() {
        val known = NewSceneFollow.known(doc, K.EPISODE_1_7)
        val (after, key) = SceneActions.create(doc, K.EPISODE_1_7, K.SCENE_1_7_2, ctx())
        assertEquals(key, NewSceneFollow.created(after, K.EPISODE_1_7, known))
        assertEquals(4, io.github.lenticualrstone.civilight.domain.script.Labels.sceneOrdinal(after, K.SCENE_1_7_3))
    }
}
