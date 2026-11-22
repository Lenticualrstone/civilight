package io.github.lenticualrstone.civilight.domain.story

import io.github.lenticualrstone.civilight.domain.story.Fx.S9
import io.github.lenticualrstone.civilight.domain.story.Fx.unit
import io.github.lenticualrstone.civilight.domain.story.Fx.withBlock
import io.github.lenticualrstone.civilight.domain.story.scene.SceneTrash
import io.github.lenticualrstone.civilight.domain.story.structure.SceneStatus
import io.github.lenticualrstone.civilight.domain.story.structure.StoryOverview
import io.github.lenticualrstone.civilight.domain.story.structure.StructureEdit
import io.github.lenticualrstone.civilight.domain.story.text.BlockText
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.app.DayBaseline
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Structure labels, statuses and counts (20-story-time §4.14 STR-01 … STR-05; STO-01, STO-06). */
class StructureTest {
    private val doc = Fx.doc()
    private val view = StoryOverview(doc)

    @Test
    fun str01_chapterHeadersAndProgress() {
        val c1 = view.chapterRow(doc.unit(K.CHAPTER_1))
        assertEquals("Ⅰ 1장 · 등불지기", c1.header)
        assertEquals("7/10", c1.progress)
        assertFalse(c1.planned)
        assertNull(c1.synopsis)
        val c2 = view.chapterRow(doc.unit(K.CHAPTER_2))
        assertEquals("Ⅱ 2장 · 잿불", c2.header)
        assertEquals("0/8 · 계획", c2.progress)
        assertTrue(c2.planned)
        assertEquals("상층으로 올라간 리엔이 꺼진 빛의 값을 알게 된다.", c2.synopsis)
        assertEquals("단계 이름: 장 › 에피소드 › 씬", view.levelLine)
    }

    /** STR-02's Given: 1-7 씬 1 `새벽 순찰` with 1,820자 and 씬 2 with 2,104자 (one narration block each). */
    private fun str02Doc(): io.github.lenticualrstone.civilight.model.doc.ProjectDoc {
        val sized = mapOf(K.SCENE_1_7_1 to 1820, K.SCENE_1_7_2 to 2104)
        val keep = sized.keys.associateWith { k -> doc.manuscript.liveBlocksOf(k).first().key }
        val titled = doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == K.SCENE_1_7_1) it.copy(title = "새벽 순찰") else it }))
        return titled.copy(
            episodes = titled.episodes.mapValues { (_, f) ->
                f.copy(
                    blocks = f.blocks.mapNotNull { b ->
                        val n = sized[b.sceneKey] ?: return@mapNotNull b
                        if (b.key != keep[b.sceneKey]) null else b.copy(kind = io.github.lenticualrstone.civilight.model.script.BlockKind.NARRATION.let { io.github.lenticualrstone.civilight.model.OpenEnum.of(it) }, runs = listOf(TextRun.Plain("가".repeat(n))), cues = emptyList())
                    },
                )
            },
        )
    }

    @Test
    fun str02_expandedEpisodeRows() {
        val d = str02Doc()
        val v = StoryOverview(d)
        val rows = v.sceneRows(d.unit(K.EPISODE_1_7))
        assertEquals("1일째 새벽 | 씬 1 · 새벽 순찰 | 1,820자", rows[0].let { "${it.time} | ${it.label} | ${it.meta}" })
        assertEquals("1일째 저녁", rows[1].time)
        assertTrue(rows[1].label.startsWith("씬 2 · "))
        assertEquals("2,104자", rows[1].meta)
        assertEquals("2일째 새벽 | 씬 3 · 꺼진 등불 | 쓰는 중", rows[2].let { "${it.time} | ${it.label} | ${it.meta}" })
        assertEquals("2일째 아침 | 씬 4 · 미정 | —", rows[3].let { "${it.time} | ${it.label} | ${it.meta}" })
        assertTrue(rows[2].current)
        assertEquals(SceneStatus.UNDECIDED, rows[3].status)
        val total = v.charCount(d.unit(K.EPISODE_1_7))
        assertEquals("씬 4개 · ${StoryOverview.chars(total)}", v.episodeRow(d.unit(K.EPISODE_1_7), expanded = true).meta)
        assertEquals("씬 4개", v.episodeRow(d.unit(K.EPISODE_1_7)).meta)
        assertEquals("5,210자", StoryOverview.chars(5210))
    }

    @Test
    fun unwrittenEpisodeKeepsItsTitle() {
        val d = doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == K.EPISODE_1_9) it.copy(title = "이름") else it }))
        val row = StoryOverview(d).episodeRow(d.unit(K.EPISODE_1_9))
        assertEquals("이름", row.title)
        assertEquals("—", row.meta)
        assertTrue(row.unwritten)
    }

    @Test
    fun str03_renameLevelKeepsCodes() {
        val renamed = StructureEdit.renameLevel(doc, 1, "화")
        assertEquals("단계 이름: 장 › 화 › 씬", StoryOverview(renamed).levelLine)
        assertEquals("1-7", UnitCodes(renamed).unitLabel(K.EPISODE_1_7))
        assertEquals(doc.story, renamed.story)
        assertEquals(doc, StructureEdit.renameLevel(doc, 1, "  "))
        assertFalse(StructureEdit.canChangeLevelCount(doc))
    }

    @Test
    fun str04_trashedSceneIsNotCounted() {
        val d = SceneTrash.delete(doc, S9, Fx.ctx())
        val v = StoryOverview(d)
        assertEquals("씬 3개", v.episodeRow(d.unit(K.EPISODE_1_7)).meta)
        assertEquals("씬 3", UnitCodes(d).sceneLabel(K.SCENE_1_7_4))
        assertEquals(listOf("씬 1", "씬 2", "씬 3"), v.sceneRows(d.unit(K.EPISODE_1_7)).map { it.label.substringBefore(" ·") })
    }

    @Test
    fun str05_homeRows() {
        assertEquals("1-7 | 잿빛 새벽의 등불 | 쓰는 중 · 씬 3", view.homeRow(doc.unit(K.EPISODE_1_7)).line)
        assertEquals("1-8 | — | 미정", view.homeRow(doc.unit(K.EPISODE_1_8)).line)
        assertEquals("1-5 | 갈림길 | 완료", view.homeRow(doc.unit(K.EPISODE_1_5)).line)
        // The last edit elsewhere: the highest-numbered scene with content.
        val elsewhere = StoryOverview(doc, lastEditedBlockKey = null)
        assertEquals("쓰는 중 · 씬 3", elsewhere.episodeStatus(doc.unit(K.EPISODE_1_7)))
    }

    @Test
    fun lens09_episode18StaysUndecided() {
        val d = doc.withBlock(K.SCENE_1_9_1, "01JZ000000TEST0000000000B1", "관문 앞은 조용했다.")
        val v = StoryOverview(d)
        val row = v.episodeRow(d.unit(K.EPISODE_1_8))
        assertTrue(row.unwritten)
        assertEquals("미정", row.title)
        assertEquals("—", row.meta)
        assertEquals("8/10", v.progress(d.unit(K.CHAPTER_1)))
    }

    @Test
    fun homeStatsAndResume() {
        assertEquals("7/10" to "1장 에피소드", view.chapterStat())
        assertEquals(11, view.characterCount)
        assertEquals(K.CHAPTER_1, view.currentChapter?.key)
        val (where, quote) = view.resume()!!
        assertEquals("1-7 씬 3 · 꺼진 등불", where)
        assertEquals("“바람이 잿더미를 쓸고 지나갔다. 꺼진 줄 알았던 오리지늄 등불이 한 번 깜박였다.”", quote)
        val empty = doc.copy(episodes = doc.episodes.mapValues { (_, f) -> f.copy(blocks = emptyList()) })
        assertNull(StoryOverview(empty).resume())
    }

    @Test
    fun hiddenSceneRows() {
        val d = doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == K.SCENE_1_7_4) it.copy(hidden = true, title = "문 앞") else it }))
        assertEquals("씬 4 · 숨긴 씬", StoryOverview(d).sceneRow(d.unit(K.SCENE_1_7_4)).label)
        val open = StoryOverview(d, locked = false).sceneRow(d.unit(K.SCENE_1_7_4))
        assertEquals("씬 4 · 문 앞", open.label)
        assertTrue(open.hiddenTag)
    }

    @Test
    fun chk02_writtenRange() {
        assertEquals("1-1 ~ 1-7", view.writtenRange())
    }

    @Test
    fun charCountRules() {
        val b = doc.index.block(K.BLOCK_045)!!
        // `(숨을 삼키며)` + `하르트 씨, 방금…`: paren text counted without the brackets, spaces included.
        assertEquals("숨을 삼키며".length + "하르트 씨, 방금…".length, BlockText.charCount(b))
        assertEquals(0, BlockText.charCount(doc.index.block(K.BLOCK_046)!!))
        assertEquals("(숨을 삼키며) 하르트 씨, 방금…", BlockText.gameText(b))
        assertEquals(2, BlockText.graphemes("👩‍👩‍👧가"))
    }

    @Test
    fun todayChars() {
        val today = LocalDate.of(2026, 11, 22)
        val withBaseline = doc.copy(
            app = doc.app.copy(
                activity = doc.app.activity.copy(dayBaselines = listOf(DayBaseline(today, K.BLOCK_045, 3))),
            ),
        )
        val len = BlockText.charCount(doc.index.block(K.BLOCK_045)!!)
        assertEquals(len - 3, StructureEdit.todayChars(withBaseline, today))
        assertEquals(0, StructureEdit.todayChars(doc, today))
        val created = doc.withBlock(K.SCENE_1_7_3, "01JZ000000TEST0000000000B2", "", kind = BlockKind.NARRATION, runs = listOf(TextRun.Plain("새 줄")))
        val withCreated = created.copy(
            episodes = created.episodes.mapValues { (_, f) ->
                f.copy(blocks = f.blocks.map { if (it.key == "01JZ000000TEST0000000000B2") it.copy(createdAt = 1_795_305_600_000L) else it })
            },
        )
        assertEquals(3, StructureEdit.todayChars(withCreated, today))
    }
}
