package io.github.lenticualrstone.civilight.domain.library

import io.github.lenticualrstone.civilight.domain.library.common.TimeLabels
import io.github.lenticualrstone.civilight.domain.library.format.ProjectFileOptions
import io.github.lenticualrstone.civilight.domain.library.trash.NameTags
import io.github.lenticualrstone.civilight.domain.library.trash.TrashDays
import io.github.lenticualrstone.civilight.domain.library.trash.TrashFilter
import io.github.lenticualrstone.civilight.domain.library.trash.TrashGrouping
import io.github.lenticualrstone.civilight.domain.library.trash.TrashRows
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.model.script.VoiceMode
import io.github.lenticualrstone.civilight.model.script.VoiceState
import io.github.lenticualrstone.civilight.model.trash.NameTagKind
import io.github.lenticualrstone.civilight.model.trash.NameTagState
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import io.github.lenticualrstone.civilight.model.trash.TrashReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 60-library §4.8 trash cases T-01 … T-15, against the host side kept in this module (rows, filters,
 * countdown, grouping key, name-tag labels). Delete, restore and purge themselves are domain:trash's
 * (its `LibraryTrashTest` runs the same ids against those operations).
 */
class TrashTest {
    private val rows = TrashRows(Fixtures.NOW, Fixtures.ZONE)
    private val doc = Fixtures.example()

    private fun entry(
        key: Key,
        kind: TrashKind,
        targets: List<Key>,
        deletedAt: Long,
        projectKey: Key? = K.PROJECT,
        groupScene: Key? = null,
        reason: TrashReason = TrashReason.DELETED,
        mergedInto: Key? = null,
        hidden: Boolean = false,
    ) = TrashEntry(
        key = key, kind = kind.open(), projectKey = projectKey, targetKeys = targets,
        groupSceneKey = groupScene, groupDate = groupScene?.let { TimeLabels.localDate(deletedAt, Fixtures.ZONE) },
        reason = reason.open(), mergedIntoKey = mergedInto, hiddenAtDelete = hidden, deletedAt = deletedAt,
        purgeOn = TrashEntry.purgeDateFor(TimeLabels.localDate(deletedAt, Fixtures.ZONE)),
    )

    /** [source] with [blocks] trashed under [entry]. */
    private fun withTrashedBlocks(source: ProjectDoc, blocks: List<Key>, entry: TrashEntry): ProjectDoc = source.copy(
        episodes = source.episodes.mapValues { (_, file) ->
            file.copy(blocks = file.blocks.map { if (it.key in blocks) it.copy(deletedAt = entry.deletedAt, trashEntryKey = entry.key) else it })
        },
        trash = source.trash.copy(entries = source.trash.entries + entry),
    )

    @Test
    fun `T-01 project deleted yesterday`() {
        val glass = Fixtures.projectWith("유리 정원", Rating.ALL, format = ProjectFormat.SHORT, scenes = 6, characters = 4)
        val e = entry("E1", TrashKind.PROJECT, listOf(glass.key), Fixtures.at("2026-11-21T01:00:00Z"), projectKey = glass.key)
        val row = rows.row(e, glass)
        assertEquals("유리 정원", row.title)
        assertEquals("프로젝트 · 씬 6개 · 인물 4명", row.sub)
        assertEquals("29일 남음", row.daysLeft)
        assertEquals(listOf("되살리기", "영구 삭제"), row.tags)
        assertFalse(row.restoreNeedsAuth)
    }

    @Test
    fun `T-02 four blocks of one scene deleted today`() {
        val blocks = listOf(K.BLOCK_036, K.BLOCK_037, K.BLOCK_038, K.BLOCK_039)
        val e = entry("E2", TrashKind.BLOCK_GROUP, blocks, Fixtures.NOW, groupScene = K.SCENE_1_7_3)
        val row = rows.row(e, withTrashedBlocks(doc, blocks, e))
        assertEquals("1-7 씬 3 · 블록 4개", row.title)
        assertEquals("블록 · 잿빛 새벽의 등불 · 오늘 지움", row.sub)
        assertEquals("30일 남음", row.daysLeft)
    }

    @Test
    fun `T-03 a scene's blocks deleted today and yesterday make two rows`() {
        val yesterday = Fixtures.at("2026-11-20T23:00:00Z")
        val older = entry("E3a", TrashKind.BLOCK_GROUP, listOf(K.BLOCK_040, K.BLOCK_041), yesterday, groupScene = K.SCENE_1_7_3)
        val today = entry("E3b", TrashKind.BLOCK_GROUP, listOf(K.BLOCK_036, K.BLOCK_037), Fixtures.at("2026-11-21T22:00:00Z"), groupScene = K.SCENE_1_7_3)
        val entries = listOf(older, today)
        // Grouping key: same scene and same local day joins; another day or scene is a new row.
        assertSame(today, TrashGrouping.groupFor(entries, K.SCENE_1_7_3, Fixtures.NOW, Fixtures.ZONE))
        assertSame(older, TrashGrouping.groupFor(entries, K.SCENE_1_7_3, Fixtures.at("2026-11-21T10:00:00Z"), Fixtures.ZONE))
        assertNull(TrashGrouping.groupFor(entries, K.SCENE_1_7_2, Fixtures.NOW, Fixtures.ZONE))

        val trashed = withTrashedBlocks(withTrashedBlocks(doc, older.targetKeys, older), today.targetKeys, today)
        val shown = rows.filter(entries, TrashFilter.BLOCK).map { rows.row(it, trashed) }
        assertEquals(listOf("1-7 씬 3 · 블록 2개", "1-7 씬 3 · 블록 2개"), shown.map { it.title })
        assertEquals(listOf("블록 · 잿빛 새벽의 등불 · 오늘 지움", "블록 · 잿빛 새벽의 등불 · 어제 지움"), shown.map { it.sub })
    }

    @Test
    fun `T-04 an empty block never enters the trash`() {
        val empty = doc.index.block(K.BLOCK_1_8_001)!!
        assertFalse(TrashGrouping.needsEntry(empty))
        assertTrue(TrashGrouping.needsEntry(doc.index.block(K.BLOCK_043)!!))
    }

    @Test
    fun `T-05 scene in the trash`() {
        val e = doc.index.trashEntry(K.TRASH_SCENE_1_6_5)!!
        val row = rows.row(e, doc)
        assertEquals("1-6 씬 5 · 시장의 밤", row.title)
        assertEquals("씬 · 잿빛 새벽의 등불", row.sub)
        assertEquals("12일 남음", row.daysLeft)
        assertEquals(listOf(e), rows.filter(doc.trash.entries, TrashFilter.SCENE))
    }

    @Test
    fun `T-06 character in the trash with reference counts`() {
        val row = rows.row(doc.index.trashEntry(K.TRASH_NOAH)!!, doc)
        assertEquals("노아", row.title)
        assertEquals("인물 · 잿빛 새벽의 등불 · 대사 14 · 관계 1", row.sub)
        assertEquals("3일 남음", row.daysLeft)
    }

    @Test
    fun `T-07 recorder record in the trash`() {
        val e = ExampleRecorder.trashEntries().single()
        val record = ExampleRecorder.library().records.single { it.key == K.RECORDER_TRASHED }
        val row = rows.recorderRow(e, record)
        assertEquals("음성 0:12", row.title)
        assertEquals("라이프 레코더", row.sub)
        assertEquals("1일 남음", row.daysLeft)
        assertEquals(listOf(e), rows.filter(ExampleRecorder.trashEntries(), TrashFilter.RECORDER))
    }

    @Test
    fun `T-08 an entry deleted 2026-10-23 is purged on 2026-11-22`() {
        val e = entry("E8", TrashKind.CHARACTER, listOf(K.GORDON), Fixtures.at("2026-10-23T01:00:00Z"))
        assertEquals(0L, TrashDays.daysLeft(e.deletedAt, Fixtures.NOW, Fixtures.ZONE))
        assertTrue(TrashDays.isPurgeDue(e, Fixtures.NOW, Fixtures.ZONE))
        assertEquals(java.time.LocalDate.of(2026, 11, 22), e.purgeOn)
        val dayLater = entry("E8b", TrashKind.CHARACTER, listOf(K.GORDON), Fixtures.at("2026-10-24T01:00:00Z"))
        assertFalse(TrashDays.isPurgeDue(dayLater, Fixtures.NOW, Fixtures.ZONE))
        // The day boundary is local: 2026-10-23 23:30 KST is still the 23rd.
        assertEquals(0L, TrashDays.daysLeft(Fixtures.at("2026-10-23T14:30:00Z"), Fixtures.NOW, Fixtures.ZONE))
    }

    @Test
    fun `T-09 filter 세계 lists the trashed term and place`() {
        val at = Fixtures.at("2026-11-21T01:00:00Z")
        val term = entry("E9a", TrashKind.TERM, listOf(K.TERM_ORIGINIUM_LANTERN), at)
        val place = entry("E9b", TrashKind.PLACE, listOf(K.PLACE_ASH_MARKET), at.plus(1))
        val entries = doc.trash.entries + term + place
        val world = rows.filter(entries, TrashFilter.WORLD)
        assertEquals(setOf(term, place), world.toSet())
        assertEquals(setOf("오리지늄 등불", "잿더미 시장"), world.map { rows.row(it, doc).title }.toSet())
        assertTrue(rows.filter(entries, TrashFilter.CHARACTER).none { it == term || it == place })
        // Newest deletion first.
        assertEquals(listOf(place, term), world)
    }

    @Test
    fun `T-10 filter chips`() {
        assertEquals(listOf("전체", "프로젝트", "씬", "블록", "인물", "세계", "변화", "레코더"), TrashFilter.entries.map { it.label })
        assertEquals(listOf("전체 4", "프로젝트", "씬", "블록", "인물", "세계", "변화", "레코더"), TrashFilter.entries.map { it.chip(4) })
    }

    @Test
    fun `T-11 a merged character shows 합침 → its target`() {
        val live = Fixtures.withNoahLive()
        val kidKey = Fixtures.keys(111)()
        val e = entry("E11", TrashKind.CHARACTER, listOf(kidKey), Fixtures.NOW, reason = TrashReason.MERGED, mergedInto = K.NOAH)
        val kid = Character(
            key = kidKey, recordNo = 99, name = "시장 꼬마", order = OrderKeys.between(null, null), deletedAt = Fixtures.NOW, trashEntryKey = e.key,
        )
        val merged = live.copy(cast = live.cast.copy(characters = live.cast.characters + kid), trash = live.trash.copy(entries = live.trash.entries + e))
        val row = rows.row(e, merged)
        assertEquals(TrashKind.CHARACTER, e.kind.known)
        assertEquals(TrashReason.MERGED, e.reason.known)
        assertEquals("시장 꼬마", row.title)
        assertTrue(row.sub, row.sub.contains("합침 → 노아"))
        assertEquals(listOf(e), rows.filter(merged.trash.entries, TrashFilter.CHARACTER).filter { it == e })
    }

    @Test
    fun `T-12 a name tag keeps 노아 after permanent delete and fixed IDs stay`() {
        val noah = doc.index.character(K.NOAH)!!
        val tag = NameTags.forCharacter(noah, Fixtures.NOW)
        assertEquals(K.NOAH, tag.key)
        assertEquals("노아", tag.name)
        assertEquals(NameTagKind.CHARACTER, tag.kind.known)
        assertEquals("NOA", tag.voiceCode)
        assertFalse(tag.hidden)
        assertEquals(NameTagState.PURGED, tag.state.known)
        assertEquals("영구 삭제 · 노아", NameTags.referenceLabel(tag))
        // The file keeps 노아's fixed voice IDs on his lines and a name tag for the references.
        val pkg = Fixtures.open(Fixtures.export(doc))
        assertEquals("NOA_0106_007", pkg.doc.index.block(K.BLOCK_1_6_007)!!.voice!!.fixedId)
        assertEquals("노아", pkg.doc.index.nameTag(K.NOAH)!!.name)
    }

    @Test
    fun `T-13 a hidden character's name tag masks references`() {
        val hidden = doc.index.character(K.NOAH)!!.copy(hidden = true)
        val tag = NameTags.forCharacter(hidden, Fixtures.NOW)
        assertTrue(tag.hidden)
        assertEquals("영구 삭제 · 숨긴 항목", NameTags.referenceLabel(tag))
    }

    @Test
    fun `T-14 a block of a trashed scene offers two restore choices`() {
        val inTrashedScene = doc.index.blocksOfScene(K.SCENE_1_6_5).first()
        assertEquals(listOf("씬도 되살리기", "다른 씬 끝에"), TrashGrouping.restoreChoices(doc, inTrashedScene.key))
        assertEquals(emptyList<String>(), TrashGrouping.restoreChoices(doc, K.BLOCK_043))
    }

    @Test
    fun `T-15 a trashed block keeps its number and fixed voice ID in the file`() {
        val e = entry("E15", TrashKind.BLOCK_GROUP, listOf(K.BLOCK_043), Fixtures.NOW, groupScene = K.SCENE_1_7_3)
        val fixed = VoiceState(mode = VoiceMode.FIXED.open(), fixedId = "HRT_0107_043", fixedAt = Fixtures.at("2026-11-20T01:00:00Z"))
        val withVoice = doc.copy(
            episodes = doc.episodes.mapValues { (_, f) -> f.copy(blocks = f.blocks.map { if (it.key == K.BLOCK_043) it.copy(voice = fixed) else it }) },
        )
        val trashed = withTrashedBlocks(withVoice, listOf(K.BLOCK_043), e)
        val pkg = Fixtures.open(Fixtures.export(trashed, options = ProjectFileOptions(includeTrash = true)))
        val block = pkg.doc.index.block(K.BLOCK_043)!!
        assertEquals(43, block.number)
        assertEquals(VoiceMode.FIXED, block.voice!!.mode.known)
        assertEquals("HRT_0107_043", block.voice!!.fixedId)
        assertEquals(e.key, block.trashEntryKey)
        assertNotNull(pkg.doc.index.trashEntry(e.key))
        // The records anchored at #043 are still there to re-attach on restore.
        assertEquals(doc.changes.records.map { it.key }.toSet(), pkg.doc.changes.records.map { it.key }.toSet())
    }

    @Test
    fun `hidden items in the trash are masked while locked`() {
        val hiddenNoah = doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.NOAH) it.copy(hidden = true) else it }))
        val e = doc.index.trashEntry(K.TRASH_NOAH)!!.copy(hiddenAtDelete = true)
        val locked = rows.row(e, hiddenNoah, itemsLocked = true)
        assertEquals("숨긴 인물 · 3일 남음", locked.titleWithDays)
        assertEquals("인물 · 잿빛 새벽의 등불 · 되살리기는 지문 확인", locked.sub)
        assertTrue(locked.restoreNeedsAuth)
        val open = rows.row(e, hiddenNoah, itemsLocked = false)
        assertEquals("노아", open.title)
        assertTrue(open.restoreNeedsAuth)
        // An item of a hidden project shows the project as `숨긴 프로젝트` and asks before restoring.
        val inHiddenProject = rows.row(doc.index.trashEntry(K.TRASH_SCENE_1_6_5)!!, doc, projectLocked = true)
        assertEquals("씬 · 숨긴 프로젝트", inHiddenProject.sub)
        assertTrue(inHiddenProject.restoreNeedsAuth)
    }
}
