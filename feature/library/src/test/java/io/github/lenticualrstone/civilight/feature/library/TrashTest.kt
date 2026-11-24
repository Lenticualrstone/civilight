package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.SceneActions
import io.github.lenticualrstone.civilight.domain.trash.RecorderTrashState
import io.github.lenticualrstone.civilight.domain.trash.SceneChoice
import io.github.lenticualrstone.civilight.domain.trash.TrashFilter
import io.github.lenticualrstone.civilight.feature.library.trash.TrashActions
import io.github.lenticualrstone.civilight.feature.library.trash.TrashInputs
import io.github.lenticualrstone.civilight.feature.library.trash.TrashOutcome
import io.github.lenticualrstone.civilight.feature.library.trash.TrashPorts
import io.github.lenticualrstone.civilight.feature.library.trash.TrashRowUi
import io.github.lenticualrstone.civilight.feature.library.trash.TrashSource
import io.github.lenticualrstone.civilight.feature.library.trash.TrashState
import io.github.lenticualrstone.civilight.feature.library.trash.TrashUi
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.LibraryIndexFile
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.security.UnlockState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** 휴지통 (LIB-38, LIB-39): the row list the screen draws and every write it makes, spec §4.8. */
class TrashTest {
    private val ctx = LibFx.ctx()
    private val ashen = LibFx.ashen()
    private val glass = LibFx.glass()

    /** 유리 정원 in the trash since 2026-11-21 (T-01), as `LibraryStore.moveToTrash` writes it. */
    private val glassDeletedAt = LibFx.at("2026-11-21T10:00")
    private val glassEntry = TrashEntry(
        key = "01KTESTTRASHGLASS0000000001", kind = TrashKind.PROJECT.open(), projectKey = glass.key, targetKeys = listOf(glass.key),
        deletedAt = glassDeletedAt, purgeOn = TrashEntry.purgeDateFor(LocalDate.of(2026, 11, 21)),
    )

    private fun index(hiddenGlass: Boolean = false) = LibraryIndexFile(
        projects = listOf(
            ExampleProject.libraryEntry().copy(key = glass.key, name = glass.project.name, hidden = hiddenGlass, deletedAt = glassDeletedAt, trashEntryKey = glassEntry.key),
        ),
    )

    private fun inputs(liveDocs: List<ProjectDoc> = listOf(ashen), hiddenGlass: Boolean = false) = TrashInputs(
        index = index(hiddenGlass),
        projectEntries = listOf(glassEntry.copy(hiddenAtDelete = hiddenGlass)),
        trashedDocs = mapOf(glass.key to glass),
        liveDocs = liveDocs,
        recorder = ExampleRecorder.library(),
        recorderEntries = ExampleRecorder.trashEntries(),
    )

    private fun build(unlock: UnlockState = UnlockState(), liveDocs: List<ProjectDoc> = listOf(ashen), hiddenGlass: Boolean = false): TrashUi =
        TrashState.build(inputs(liveDocs, hiddenGlass), unlock, ctx.trash)

    private fun TrashUi.byTitle(title: String): TrashRowUi = rows.first { it.row.title == title }

    @Test
    fun `T-10 chips are 전체 n then the seven kinds`() {
        val ui = build()
        assertEquals(
            listOf("전체 ${ui.rows.size}", "프로젝트", "씬", "블록", "인물", "세계", "변화", "레코더"),
            ui.chips.map { it.label },
        )
    }

    @Test
    fun `T-01 a trashed project row reads its counts and 29일 남음`() {
        val row = build().byTitle("유리 정원")
        assertEquals(TrashSource.PROJECT, row.source)
        assertEquals("29일 남음", row.row.daysLeftLabel)
        val scenes = glass.manuscript.liveScenes.size
        val cast = glass.cast.characters.count { it.deletedAt == null }
        assertEquals("프로젝트 · 씬 ${scenes}개 · 인물 ${cast}명", row.row.sub)
        assertNotNull("an unmasked project row protects the window as the project does", row.protections)
    }

    @Test
    fun `T-05 T-06 the example's scene and character rows, newest deletion first`() {
        val ui = build()
        val scene = ui.byTitle("1-6 씬 5 · 시장의 밤")
        assertEquals("12일 남음", scene.row.daysLeftLabel)
        assertEquals("씬 · 잿빛 새벽의 등불", scene.row.sub)
        val noah = ui.byTitle("노아")
        assertEquals("3일 남음", noah.row.daysLeftLabel)
        assertTrue(noah.row.sub.startsWith("인물 · 잿빛 새벽의 등불"))
        val deleted = ui.rows.map { it.row.deletedAt }
        assertEquals(deleted.sortedDescending(), deleted)
    }

    @Test
    fun `T-07 the recorder row is 음성 0 12 with 1일 남음`() {
        val record = build().rows.single { it.source == TrashSource.RECORDER }
        assertEquals("음성 0:12", record.row.title)
        assertEquals("라이프 레코더", record.row.sub)
        assertEquals("1일 남음", record.row.daysLeftLabel)
    }

    @Test
    fun `T-09 the 세계 and 인물 filters keep only their kinds`() {
        val ui = build()
        assertTrue(ui.visible(TrashFilter.CHARACTER).all { it.row.kind.known == TrashKind.CHARACTER || it.row.kind.known == TrashKind.FIELD_DEF })
        assertEquals(listOf("노아"), ui.visible(TrashFilter.CHARACTER).map { it.row.title })
        assertTrue(ui.visible(TrashFilter.WORLD).isEmpty())
        assertEquals(ui.rows.size, ui.visible(TrashFilter.ALL).size)
        assertEquals(listOf("1-5 씬 2 · 블록 2개"), ui.visible(TrashFilter.BLOCK).map { it.row.title })
    }

    @Test
    fun `S-08 a hidden project's rows are masked while locked and named while open`() {
        val hidden = ashen.copy(project = ashen.project.copy(protections = ashen.project.protections.copy(hidden = true)))
        val locked = build(liveDocs = listOf(hidden), hiddenGlass = true)
        val glassRow = locked.rows.first { it.source == TrashSource.PROJECT }
        assertEquals("숨긴 프로젝트", glassRow.row.title)
        assertTrue(glassRow.row.masked)
        assertNull(glassRow.protections)
        val items = locked.rows.filter { it.source == TrashSource.ITEM }
        assertTrue("no item row names the hidden project", items.none { "잿빛" in it.row.sub || "잿빛" in it.row.title })
        assertTrue(items.all { it.row.restoreNeedsAuth })

        val open = build(UnlockState(hiddenGroupUnlockedAt = LibFx.NOW), liveDocs = listOf(hidden), hiddenGlass = true)
        assertEquals("유리 정원", open.rows.first { it.source == TrashSource.PROJECT }.row.title)
        assertTrue(open.rows.filter { it.source == TrashSource.ITEM }.any { "잿빛 새벽의 등불" in it.row.sub })
    }

    @Test
    fun `LIB-39 비우기 leaves exactly the masked rows`() {
        val hidden = ashen.copy(project = ashen.project.copy(protections = ashen.project.protections.copy(hidden = true)))
        val ui = build(liveDocs = listOf(hidden))
        assertTrue(ui.purgeable.none { it.row.masked })
        assertEquals(ui.rows.count { !it.row.masked }, ui.purgeable.size)
        assertTrue("유리 정원 is not hidden and can go", ui.purgeable.any { it.source == TrashSource.PROJECT })
    }

    // ---- writes ----

    private class FakePorts(docs: List<ProjectDoc>, var auth: Boolean = true, val ctx: ActionContext) : TrashPorts {
        val docs = docs.associateBy { it.key }.toMutableMap()
        var recorder = RecorderTrashState(ExampleRecorder.library(), ExampleRecorder.trashEntries())
        var prompts = 0
        val toasts = mutableListOf<String>()
        val restoredProjects = mutableListOf<Key>()
        val purgedProjects = mutableListOf<Key>()
        val purgeWrites = mutableListOf<Key>()
        val labels = mutableListOf<String>()
        var blobRuns = 0

        override suspend fun authenticate(): Boolean {
            prompts++
            return auth
        }

        override suspend fun restoreProject(projectKey: Key) {
            restoredProjects += projectKey
        }

        override suspend fun purgeProject(projectKey: Key) {
            purgedProjects += projectKey
        }

        override suspend fun editProject(projectKey: Key, label: String, transform: (ProjectDoc) -> ProjectDoc): EditResult {
            labels += label
            docs[projectKey] = transform(docs.getValue(projectKey))
            return EditResult.APPLIED
        }

        override suspend fun purgeInProject(projectKey: Key, transform: (ProjectDoc) -> ProjectDoc): EditResult {
            purgeWrites += projectKey
            docs[projectKey] = transform(docs.getValue(projectKey))
            return EditResult.APPLIED
        }

        override suspend fun updateRecorder(transform: (RecorderTrashState) -> RecorderTrashState) {
            recorder = transform(recorder)
        }

        override suspend fun updateRecorderLibrary(transform: (RecorderLibrary) -> RecorderLibrary) {
            recorder = recorder.copy(library = transform(recorder.library))
        }

        override suspend fun collectBlobs() {
            blobRuns++
        }

        override fun toast(text: String) {
            toasts += text
        }

        override fun context(): ActionContext = ctx
    }

    private fun rowsOf(ports: FakePorts, unlock: UnlockState = UnlockState()): TrashUi = TrashState.build(
        TrashInputs(index(), listOf(glassEntry), mapOf(glass.key to glass), ports.docs.values.toList(), ports.recorder.library, ports.recorder.entries),
        unlock, ctx.trash,
    )

    @Test
    fun `restoring 노아 goes through the character action and shows its toast`() = runTest {
        val ports = FakePorts(listOf(ashen), ctx = ctx)
        val noah = rowsOf(ports).byTitle("노아")
        assertEquals(TrashOutcome.Done, TrashActions(ports).restore(noah))
        val after = ports.docs.getValue(ashen.key)
        assertNull(after.index.character(ExampleKeys.NOAH)!!.deletedAt)
        assertNull(after.index.trashEntry(ExampleKeys.TRASH_NOAH))
        assertEquals(0, ports.prompts)
        assertEquals(listOf("되살리기"), ports.labels)
        assertTrue(ports.toasts.single().startsWith("노아 되살림"))
    }

    @Test
    fun `restoring the trashed scene brings 1-6 씬 5 back through the scene action`() = runTest {
        val ports = FakePorts(listOf(ashen), ctx = ctx)
        val scene = rowsOf(ports).byTitle("1-6 씬 5 · 시장의 밤")
        assertEquals(TrashOutcome.Done, TrashActions(ports).restore(scene))
        val after = ports.docs.getValue(ashen.key)
        assertNull(after.index.unit(ExampleKeys.SCENE_1_6_5)!!.deletedAt)
        assertTrue(ports.toasts.single().startsWith("시장의 밤 되살림"))
    }

    @Test
    fun `a hidden row asks once and a cancelled prompt changes nothing - LIB-37`() = runTest {
        val hidden = ashen.copy(project = ashen.project.copy(protections = ashen.project.protections.copy(hidden = true)))
        val ports = FakePorts(listOf(hidden), auth = false, ctx = ctx)
        val row = rowsOf(ports).rows.first { it.source == TrashSource.ITEM && it.row.kind.known == TrashKind.CHARACTER }
        assertTrue(row.row.restoreNeedsAuth)
        assertEquals(TrashOutcome.Cancelled, TrashActions(ports).restore(row))
        assertEquals(1, ports.prompts)
        assertEquals(hidden, ports.docs.getValue(hidden.key))
        assertTrue(ports.toasts.isEmpty())

        ports.auth = true
        assertEquals(TrashOutcome.Done, TrashActions(ports).restore(row))
        assertEquals(2, ports.prompts)
        assertNull(ports.docs.getValue(hidden.key).index.character(ExampleKeys.NOAH)!!.deletedAt)
        assertTrue("a masked row's restore names nothing in a toast", ports.toasts.isEmpty())
    }

    @Test
    fun `T-14 a block whose scene is in the trash asks 씬도 되살리기 then restores both`() = runTest {
        val sceneGone = SceneActions.delete(ashen, ExampleKeys.SCENE_1_5_2, ctx).doc
        val ports = FakePorts(listOf(sceneGone), ctx = ctx)
        val actions = TrashActions(ports)
        val blocks = rowsOf(ports).rows.first { it.key == ExampleKeys.TRASH_BLOCKS_1_5_2 }
        val asked = actions.restore(blocks)
        assertTrue(asked is TrashOutcome.NeedsSceneChoice)
        asked as TrashOutcome.NeedsSceneChoice
        assertEquals(sceneGone, ports.docs.getValue(sceneGone.key))

        val again = rowsOf(ports).rows.first { it.key == ExampleKeys.TRASH_BLOCKS_1_5_2 }
        assertEquals(TrashOutcome.Done, actions.restoreBlocksWith(again, asked.blockKeys, SceneChoice.RestoreScene))
        val after = ports.docs.getValue(sceneGone.key)
        assertNull(after.index.unit(ExampleKeys.SCENE_1_5_2)!!.deletedAt)
        assertNull(after.index.trashEntry(ExampleKeys.TRASH_BLOCKS_1_5_2))
        assertTrue(asked.blockKeys.all { after.index.block(it)!!.deletedAt == null })
    }

    @Test
    fun `one block of an open group comes back on its own`() = runTest {
        val ports = FakePorts(listOf(ashen), ctx = ctx)
        val group = rowsOf(ports).rows.first { it.key == ExampleKeys.TRASH_BLOCKS_1_5_2 }
        assertEquals(2, group.row.lines.size)
        val first = group.row.lines.first()
        assertEquals(TrashOutcome.Done, TrashActions(ports).restoreBlock(group, first.blockKey))
        val after = ports.docs.getValue(ashen.key)
        assertNull(after.index.block(first.blockKey)!!.deletedAt)
        assertEquals(listOf(group.row.lines.last().blockKey), after.index.trashEntry(ExampleKeys.TRASH_BLOCKS_1_5_2)!!.targetKeys)
    }

    @Test
    fun `T-12 영구 삭제 of 노아 leaves a name tag and writes without undo`() = runTest {
        val ports = FakePorts(listOf(ashen), ctx = ctx)
        val noah = rowsOf(ports).byTitle("노아")
        assertEquals(TrashOutcome.Done, TrashActions(ports).purge(noah))
        val after = ports.docs.getValue(ashen.key)
        assertNull(after.index.character(ExampleKeys.NOAH))
        assertNull(after.index.trashEntry(ExampleKeys.TRASH_NOAH))
        assertTrue(after.tombstones.nameTags.any { it.key == ExampleKeys.NOAH && it.name == "노아" })
        assertEquals("a purge never goes through the undo path", listOf(ashen.key), ports.purgeWrites)
        assertTrue(ports.labels.isEmpty())
        assertEquals(1, ports.blobRuns)
    }

    @Test
    fun `project and recorder rows go to their own stores`() = runTest {
        val ports = FakePorts(listOf(ashen), ctx = ctx)
        val actions = TrashActions(ports)
        val ui = rowsOf(ports)
        assertEquals(TrashOutcome.Done, actions.restore(ui.byTitle("유리 정원")))
        assertEquals(listOf(glass.key), ports.restoredProjects)
        assertEquals(listOf("유리 정원 되살림"), ports.toasts)

        val record = ui.rows.single { it.source == TrashSource.RECORDER }
        assertEquals(TrashOutcome.Done, actions.restore(record))
        assertTrue(ports.recorder.entries.isEmpty())
        assertNull(ports.recorder.library.record(ExampleKeys.RECORDER_TRASHED)!!.deletedAt)
    }

    @Test
    fun `비우기 purges every open row of every source and leaves masked ones`() = runTest {
        val ports = FakePorts(listOf(ashen), ctx = ctx)
        val ui = rowsOf(ports)
        assertEquals(TrashOutcome.Done, TrashActions(ports).emptyTrash(ui.rows))
        assertEquals(listOf(glass.key), ports.purgedProjects)
        assertTrue(ports.docs.getValue(ashen.key).trash.entries.isEmpty())
        assertTrue(ports.recorder.entries.isEmpty())
        assertNull(ports.recorder.library.record(ExampleKeys.RECORDER_TRASHED))

        val hidden = ashen.copy(project = ashen.project.copy(protections = ashen.project.protections.copy(hidden = true)))
        val locked = FakePorts(listOf(hidden), ctx = ctx)
        TrashActions(locked).emptyTrash(rowsOf(locked).rows)
        assertEquals("the locked hidden project's rows stay", hidden.trash.entries, locked.docs.getValue(hidden.key).trash.entries)
        assertEquals(0, locked.prompts)
    }

    @Test
    fun `T-08 the 30-day purge removes only entries whose day has come`() = runTest {
        val later = ctx.copy(now = LibFx.at("2026-11-26T09:00"))
        val ports = FakePorts(listOf(ashen), ctx = later)
        TrashActions(ports).purgeDue(ashen)
        val after = ports.docs.getValue(ashen.key)
        assertNull("노아 (purge on 11-25) is gone", after.index.trashEntry(ExampleKeys.TRASH_NOAH))
        assertNotNull("1-6 씬 5 (12-04) stays", after.index.trashEntry(ExampleKeys.TRASH_SCENE_1_6_5))
        assertFalse(after.trash.entries.isEmpty())
    }

    @Test
    fun `an authorization ends with its flow - a dismissed scene choice asks again`() = runTest {
        val hidden = ashen.copy(project = ashen.project.copy(protections = ashen.project.protections.copy(hidden = true)))
        val sceneGone = SceneActions.delete(hidden, ExampleKeys.SCENE_1_5_2, ctx).doc
        val ports = FakePorts(listOf(sceneGone), ctx = ctx)
        val actions = TrashActions(ports)
        val blocks = rowsOf(ports).rows.first { it.key == ExampleKeys.TRASH_BLOCKS_1_5_2 }
        assertTrue(actions.restore(blocks) is TrashOutcome.NeedsSceneChoice)
        assertEquals(1, ports.prompts)
        actions.forget(blocks.key)
        assertTrue(actions.restore(blocks) is TrashOutcome.NeedsSceneChoice)
        assertEquals("asked again after the dismissed choice", 2, ports.prompts)
        actions.forgetAll()
        actions.restore(blocks)
        assertEquals("asked again after a lock change", 3, ports.prompts)
    }

    // ---- 0.2.0 tags (N-44, CST-75) ----

    private fun trashedTag(doc: ProjectDoc): Triple<ProjectDoc, io.github.lenticualrstone.civilight.model.cast.Tag, Key> {
        val tag = doc.project.tags.first { it.isLive }
        val (trashed, entry) = io.github.lenticualrstone.civilight.domain.actions.TagActions.trash(doc, tag.key, ctx)!!
        return Triple(trashed, tag, entry.key)
    }

    @Test
    fun `a trashed tag of a locked hidden project is masked and named once open`() {
        val hidden = ashen.copy(project = ashen.project.copy(protections = ashen.project.protections.copy(hidden = true)))
        val (doc, tag, entryKey) = trashedTag(hidden)
        val locked = build(liveDocs = listOf(doc)).rows.single { it.key == entryKey }
        assertTrue(locked.row.masked)
        assertEquals("숨긴 항목", locked.row.title)
        assertFalse(tag.name in locked.row.title || tag.name in locked.row.sub)
        assertFalse("잿빛" in locked.row.sub)
        assertTrue(locked.row.restoreNeedsAuth)

        val open = build(UnlockState(hiddenGroupUnlockedAt = LibFx.NOW), liveDocs = listOf(doc)).rows.single { it.key == entryKey }
        assertFalse(open.row.masked)
        assertEquals(tag.name, open.row.title)
    }

    @Test
    fun `restoring a tag whose name a live tag has says why and changes nothing`() = runTest {
        val (trashed, tag, entryKey) = trashedTag(ashen)
        val (taken, _) = io.github.lenticualrstone.civilight.domain.actions.TagActions.create(trashed, tag.name, null, ctx)!!
        val ports = FakePorts(listOf(taken), ctx = ctx)
        val row = rowsOf(ports).rows.single { it.key == entryKey }
        assertEquals("the row says why before the tap", "잿빛 새벽의 등불 · ◇ 같은 이름 태그 있음", row.row.sub)
        assertEquals(TrashOutcome.NothingDone, TrashActions(ports).restore(row))
        assertEquals(listOf("◇ 같은 이름 태그 있음"), ports.toasts)
        assertEquals(taken, ports.docs.getValue(taken.key))

        // With the name free again the same row restores the tag.
        val free = FakePorts(listOf(trashed), ctx = ctx)
        val freeRow = rowsOf(free).rows.single { it.key == entryKey }
        assertEquals("잿빛 새벽의 등불", freeRow.row.sub)
        assertEquals(TrashOutcome.Done, TrashActions(free).restore(freeRow))
        assertTrue(free.docs.getValue(ashen.key).project.tags.single { it.key == tag.key }.isLive)
    }
}
