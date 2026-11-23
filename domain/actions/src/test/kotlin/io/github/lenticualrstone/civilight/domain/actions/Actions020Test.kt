package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.library.export.ExportRunOutcome
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.ItemKind
import io.github.lenticualrstone.civilight.domain.trash.PurgeOps
import io.github.lenticualrstone.civilight.domain.trash.RestoreOps
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.domain.trash.TagRestore
import io.github.lenticualrstone.civilight.domain.trash.TagTrash
import io.github.lenticualrstone.civilight.domain.trash.TrashListing
import io.github.lenticualrstone.civilight.domain.trash.TrashFilter
import io.github.lenticualrstone.civilight.domain.trash.TrashOps
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.app.ExportOptions
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.marks.MarkState
import io.github.lenticualrstone.civilight.model.marks.Marks
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import io.github.lenticualrstone.civilight.model.version.VersionOrigin
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 0.2.0 write paths (DECISIONS §1 rows marked 0.2.0). */
class Actions020Test {
    private val ctx = Fx.ctx()

    private fun ProjectDoc.tagKeyOf(name: String) = project.tags.first { it.name == name }.key

    // ---- N-44 tags ----

    @Test
    fun `a trashed tag leaves its holders and comes back on restore`() {
        val doc = Fx.doc()
        val tag = doc.project.tags.first { t -> doc.cast.characters.count { t.key in it.tagKeys } >= 2 }
        val holders = doc.cast.characters.filter { tag.key in it.tagKeys }.map { it.key }
        assertEquals(holders.count { k -> doc.index.character(k)!!.deletedAt == null }, TagActions.usage(doc, tag.key).characters)

        val (trashed, entry) = TagActions.trash(doc, tag.key, ctx)!!
        assertEquals(TrashKind.TAG, entry.kind.known)
        assertFalse(tag.key in TagActions.liveTags(trashed).map { it.key })
        // Characters keep the key (nothing else changes).
        assertEquals(holders, trashed.cast.characters.filter { tag.key in it.tagKeys }.map { it.key })
        assertEquals(doc.cast, trashed.cast)
        assertTrue(TrashListing.projectItemRows(trashed, ctx.trash).any { it.entryKey == entry.key && it.title == tag.name })
        assertTrue(TrashFilter.CHARACTER.matches(entry.kind))

        val restored = (TagActions.restore(trashed, entry.key, ctx) as TagRestore.Restored).doc
        assertTrue(tag.key in TagActions.liveTags(restored).map { it.key })
        assertTrue(restored.trash.entries.none { it.key == entry.key })
        // The trash screen's generic restore handles a tag row too.
        assertTrue(RestoreOps.restore(trashed, entry.key, ctx.trash) is RestoreOutcome.Restored)
    }

    @Test
    fun `restore is refused while a live tag has the name, and a purge removes the key everywhere`() {
        val doc = Fx.doc()
        val tag = doc.project.tags.first()
        val (trashed, entry) = TagActions.trash(doc, tag.key, ctx)!!
        val (taken, _) = TagActions.create(trashed, tag.name, "amber", ctx)!!
        assertFalse(TagTrash.canRestore(taken, entry.key))
        assertEquals(TagRestore.NameTaken(tag.name), TagActions.restore(taken, entry.key, ctx))
        assertNull("names are unique among live tags", TagActions.create(taken, " ${tag.name} ", null, ctx))

        val purged = TagActions.purge(trashed, entry.key, ctx).doc
        assertTrue(purged.project.tags.none { it.key == tag.key })
        assertTrue(purged.cast.characters.none { tag.key in it.tagKeys })
        assertTrue(purged.galleryItems.none { tag.key in it.tagKeys })

        // The 30-day purge (PurgeActions.runDue → PurgeOps.purgeDue) does the same.
        val later = Fx.ctxAt("2026-12-23T09:00")
        val due = PurgeOps.purgeDue(trashed, later.trash).doc
        assertTrue(due.project.tags.none { it.key == tag.key })
        assertTrue(due.trash.entries.none { it.key == entry.key })
    }

    @Test
    fun `tag create and colour`() {
        val (doc, tag) = TagActions.create(Fx.doc(), "조연", "sky", ctx)!!
        assertEquals("sky", tag.color)
        val cleared = TagActions.setColor(doc, tag.key, null, ctx)
        assertNull(cleared.project.tags.first { it.key == tag.key }.color)
    }

    // ---- N-41 marks ----

    @Test
    fun `plant, mention, pay off, filter and follow a deleted block`() {
        val doc = Fx.doc()
        val (planted, f) = MarkActions.plant(doc, K.BLOCK_043, "등불의 정체", K.EPISODE_1_7, ctx)!!
        assertNull("not on a memo", MarkActions.plant(planted, doc.manuscript.liveBlocks.firstOrNull { it.kind.known == BlockKind.MEMO }?.key ?: "none", "x", null, ctx))
        val mentioned = MarkActions.mention(planted, f.key, K.BLOCK_044, ctx)
        assertEquals(mapOf(MarkState.OPEN to 1, MarkState.PAID_OFF to 0, MarkState.DONE to 0), Marks.counts(mentioned))
        assertEquals(listOf(K.BLOCK_043, K.BLOCK_044), mentioned.story.foreshadows.single().anchors.map { it.blockKey })
        val paid = MarkActions.payoff(mentioned, f.key, K.BLOCK_045, ctx)
        assertEquals(1, Marks.counts(paid)[MarkState.PAID_OFF])

        // Rule 12 §1: deleting the plant's block moves the mark to the next block, dotted; undo re-attaches.
        val deleted = TrashOps.delete(paid, ItemId(ItemKind.BLOCK, K.BLOCK_043), ctx.trash)
        val resolved = Marks.resolve(deleted.doc, deleted.doc.story.foreshadows.single().plant)
        assertEquals(K.BLOCK_044, resolved.blockKey)
        assertTrue(resolved.moved)
        val undone = TrashOps.undo(deleted.doc, deleted)
        assertEquals(K.BLOCK_043, Marks.resolve(undone, undone.story.foreshadows.single().plant).blockKey)
    }

    @Test
    fun `memo to-do states`() {
        val doc = Fx.doc()
        val memo = doc.manuscript.liveBlocks.first { it.kind.known == BlockKind.MEMO }
        val on = BlockFieldActions.setTodo(doc, memo.key, true, ctx)
        assertEquals(1, Marks.counts(on)[MarkState.OPEN])
        val done = BlockFieldActions.setTodoDone(on, memo.key, true, ctx)
        assertEquals(1, Marks.counts(done)[MarkState.DONE])
        assertEquals(on.index.block(memo.key)!!.number, done.index.block(memo.key)!!.number)
        assertTrue("a dialogue block has no to-do", BlockFieldActions.setTodo(doc, K.BLOCK_043, true, ctx) === doc)
    }

    @Test
    fun `purges pin marks and drop versions, tag restore undoes, a marked empty block is kept, a merge moves marks`() {
        val doc = Fx.doc()
        val (planted, f) = MarkActions.plant(doc, K.BLOCK_043, "등불", null, ctx)!!
        // Purging the plant's block pins the anchor to where it was drawn.
        val del = TrashOps.delete(planted, ItemId(ItemKind.BLOCK, K.BLOCK_043), ctx.trash)
        val purged = PurgeOps.purge(del.doc, del.entries.single().key, ctx.trash).doc
        assertNull(purged.index.block(K.BLOCK_043))
        val anchor = purged.story.foreshadows.single { it.key == f.key }.plant
        assertEquals(K.BLOCK_044, anchor.blockKey)
        assertFalse(Marks.resolve(purged, anchor).moved)

        // Purging an episode deletes its versions file.
        val (versioned, _) = VersionActions.capture(doc, K.EPISODE_1_7, null, VersionOrigin.MANUAL, ctx)
        val epDel = TrashOps.delete(versioned, ItemId(ItemKind.UNIT, K.EPISODE_1_7), ctx.trash)
        assertTrue("kept while in the trash", K.EPISODE_1_7 in epDel.doc.versions)
        val epPurged = PurgeOps.purge(epDel.doc, epDel.entries.single().key, ctx.trash).doc
        assertFalse(K.EPISODE_1_7 in epPurged.versions)

        // A tag restore is one undoable change.
        val tag = doc.project.tags.first()
        val (trashed, entry) = TagActions.trash(doc, tag.key, ctx)!!
        val restored = RestoreOps.restore(trashed, entry.key, ctx.trash) as RestoreOutcome.Restored
        assertEquals(trashed, restored.change.revert(restored.doc))

        // An empty block carrying a mark goes to the trash instead of being removed.
        val emptied = Fx.editBlock(planted, K.BLOCK_043) { it.copy(runs = emptyList()) }
        val emptyDel = TrashOps.delete(emptied, ItemId(ItemKind.BLOCK, K.BLOCK_043), ctx.trash)
        assertTrue(emptyDel.hardDeleted.isEmpty())
        assertNotNull(emptyDel.doc.index.block(K.BLOCK_043))

        // A merge (합치기) removes the later block; its marks join the kept one.
        val (onLater, g) = MarkActions.plant(doc, K.BLOCK_044, "잔불", null, ctx)!!
        val merged = io.github.lenticualrstone.civilight.domain.script.BlockOps.backspace(
            onLater, K.BLOCK_044, atStart = true,
            io.github.lenticualrstone.civilight.domain.script.EditContext(ctx.now, ctx.zone, ctx.device, ctx.newKey),
        )
        assertTrue(merged is io.github.lenticualrstone.civilight.domain.script.BlockOps.BackspaceResult.Merged)
        assertEquals(K.BLOCK_043, merged.doc.story.foreshadows.single { it.key == g.key }.plant.blockKey)
    }

    // ---- N-43 ----

    @Test
    fun `per-block display name shows in the manuscript label and the engine JSON, voice ID unchanged`() {
        val doc = Fx.doc().let { it.copy(project = it.project.copy(script = it.project.script.copy(voiceIdEnabled = true))) }
        val block = doc.index.block(K.BLOCK_043)!!
        val named = BlockFieldActions.setDisplayNameOverride(doc, K.BLOCK_043, "  경비병 ", ctx)
        val after = named.index.block(K.BLOCK_043)!!
        assertEquals("경비병", after.displayNameOverride)
        assertEquals(block.copy(displayNameOverride = "경비병", updatedAt = after.updatedAt, updatedBy = after.updatedBy), after)
        assertEquals("경비병 (하르트)", BlockFieldActions.speakerLabel(after.displayNameOverride, "하르트"))
        assertNull(BlockFieldActions.setDisplayNameOverride(named, K.BLOCK_043, " ", ctx).index.block(K.BLOCK_043)!!.displayNameOverride)

        val opts = ExportOptions(format = ExportFormat.ENGINE_JSON, rangeKeys = listOf(K.EPISODE_1_7), fileName = "x")
        val plain = ExportActions.run(doc, opts, authenticated = true, ctx) as ExportRunOutcome.Done
        val out = ExportActions.run(named, opts, authenticated = true, ctx) as ExportRunOutcome.Done
        fun line(o: ExportRunOutcome.Done) = Json.parseToJsonElement(o.file.bytes.toString(Charsets.UTF_8)).jsonObject["scenes"]!!.jsonArray
            .flatMap { it.jsonObject["blocks"]!!.jsonArray }.map { it.jsonObject }.first { it["id"]!!.jsonPrimitive.content == K.BLOCK_043 }
        assertEquals("경비병", line(out)["displayName"]!!.jsonPrimitive.content)
        assertEquals(line(plain)["voice"], line(out)["voice"])
    }

    @Test
    fun `speech summary is one line and the secret tab lock is stored`() {
        val doc = Fx.doc()
        assertEquals("반말 · 짧게 끊음", CharacterFieldActions.normalizeSummary("  반말 ·\n 짧게 끊음 "))
        assertNull(CharacterFieldActions.normalizeSummary(" \n "))
        val d = CharacterFieldActions.setSpeechSummary(doc, K.RIEN, "반말\n짧게", ctx)
        assertEquals("반말 짧게", d.index.character(K.RIEN)!!.speechSummary)
        val locked = CharacterFieldActions.setSecretTabLocked(d, K.RIEN, true, ctx)
        assertTrue(locked.index.character(K.RIEN)!!.secretTabLocked)
    }

    // ---- N-23 ----

    @Test
    fun `choices are not in the recording CSV and no option ID is fixed`() {
        val doc = Fx.doc().let { it.copy(project = it.project.copy(script = it.project.script.copy(voiceIdEnabled = true))) }
        val choice = doc.allBlocks.first { b -> b.kind.known == BlockKind.CHOICE && b.options.any { it.speakerKey != null } }
        val out = ExportActions.run(doc, ExportOptions(format = ExportFormat.CSV, fileName = "x"), authenticated = true, ctx) as ExportRunOutcome.Done
        val text = out.file.bytes.toString(Charsets.UTF_8)
        assertFalse(choice.key in text)
        assertEquals(choice.options, out.doc.index.block(choice.key)!!.options)
    }

    // ---- N-29 versions ----

    @Test
    fun `a version round-trips and restores one block's text only`() {
        val doc = Fx.doc()
        val (captured, v) = VersionActions.capture(doc, K.EPISODE_1_7, null, VersionOrigin.MANUAL, ctx)
        assertEquals("11월 22일 · 초고", v.name)
        val sceneKeys = VersionActions.scenesOf(doc, K.EPISODE_1_7).map { it.key }.toSet()
        assertEquals(doc.manuscript.liveBlocks.filter { it.sceneKey in sceneKeys }.map { it.key }, v.blocks.map { it.key })

        val original = captured.index.block(K.BLOCK_043)!!
        val edited = Fx.editBlock(captured, K.BLOCK_043) { it.copy(runs = listOf(TextRun.Plain("바뀐 글"))) }
        val trashed = TrashOps.delete(edited, ItemId(ItemKind.BLOCK, K.BLOCK_044), ctx.trash).doc
        val back = VersionActions.restoreBlockText(trashed, K.EPISODE_1_7, v.key, K.BLOCK_043, ctx)!!
        val b = back.index.block(K.BLOCK_043)!!
        assertEquals(original.runs, b.runs)
        assertEquals(original.copy(updatedAt = b.updatedAt, updatedBy = b.updatedBy), b)
        assertEquals(trashed.changes, back.changes)
        assertFalse("a trashed block offers no 이 판 글로", VersionActions.canRestore(trashed, v.blocks.first { it.key == K.BLOCK_044 }))
        assertNull(VersionActions.restoreBlockText(trashed, K.EPISODE_1_7, v.key, K.BLOCK_044, ctx))
    }

    @Test
    fun `an export adds one version per exported episode`() {
        val doc = Fx.doc()
        val range = listOf(K.EPISODE_1_1, K.episode(1, 2), K.episode(1, 3))
        val out = ExportActions.run(doc, ExportOptions(format = ExportFormat.CSV, rangeKeys = range, fileName = "x"), authenticated = true, ctx) as ExportRunOutcome.Done
        val written = out.plan.scenes.mapNotNull { doc.index.numberingUnitKeyOfScene(it.sceneKey) }.toSet()
        assertEquals(written, out.doc.versions.keys)
        for (unit in written) {
            val versions = VersionActions.versionsOf(out.doc, unit)
            assertEquals(1, versions.size)
            assertEquals(VersionOrigin.EXPORT, versions.single().origin.known)
            assertEquals("11월 22일 · 내보낸 판", versions.single().name)
        }
    }

    // ---- N-58 canvases ----

    @Test
    fun `create, switch, delete and restore a canvas`() {
        val doc = Fx.doc()
        val first = CanvasActions.openTarget(doc)
        assertNotNull(first)
        val (two, c) = CanvasActions.create(doc, "인물 지도", ctx)!!
        assertEquals(CanvasActions.live(doc).size + 1, CanvasActions.live(two).size)
        val opened = two.copy(app = CanvasActions.opened(two.app, c.key))
        assertEquals(c.key, CanvasActions.openTarget(opened))
        val deleted = CanvasActions.delete(opened, c.key, ctx)
        assertEquals("a gone canvas falls back to the first live one", first, CanvasActions.openTarget(deleted.doc))
        val entry = deleted.entries.single()
        val restored = CanvasActions.restore(deleted.doc, entry.key, ctx) as RestoreOutcome.Restored
        assertEquals(c.key, CanvasActions.openTarget(restored.doc))
        assertEquals("새 이름", CanvasActions.rename(restored.doc, c.key, " 새 이름 ", ctx).canvases.getValue(c.key).name)
    }

    // ---- settings ----

    @Test
    fun `project switches write true or false`() {
        val doc = Fx.doc()
        val off = ProjectSettingActions.setGameTools(doc, false, ctx)
        assertEquals(false, off.project.gameTools)
        assertEquals("hiding never deletes", doc.allBlocks, off.allBlocks)
        assertTrue(ProjectSettingActions.setGameTools(off, false, ctx) === off)
        assertFalse(ProjectSettingActions.setTermCandidates(doc, false, ctx).project.termCandidates)
    }
}
