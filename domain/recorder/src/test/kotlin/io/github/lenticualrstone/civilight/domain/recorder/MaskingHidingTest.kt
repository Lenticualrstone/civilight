package io.github.lenticualrstone.civilight.domain.recorder

import io.github.lenticualrstone.civilight.domain.recorder.Fx.withRecord
import io.github.lenticualrstone.civilight.domain.recorder.capture.Recording
import io.github.lenticualrstone.civilight.domain.recorder.hooks.RecorderHooks
import io.github.lenticualrstone.civilight.domain.recorder.inbox.CaptureGuard
import io.github.lenticualrstone.civilight.domain.recorder.inbox.Inbox
import io.github.lenticualrstone.civilight.domain.recorder.inbox.RecordScreens
import io.github.lenticualrstone.civilight.domain.recorder.inbox.RecordSearch
import io.github.lenticualrstone.civilight.domain.recorder.link.Chips
import io.github.lenticualrstone.civilight.domain.recorder.link.EntryLinks
import io.github.lenticualrstone.civilight.domain.recorder.link.LinkDisplay
import io.github.lenticualrstone.civilight.domain.recorder.link.LinkEdits
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.model.app.UnlockSession
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.recorder.InboxFilterState
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderSettings
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/** LRC §4.5 · Masking, hiding, capture. */
class MaskingHidingTest {
    private val p3Scene = TargetRef(Fx.P3, RecorderTargetKind.UNIT, Fx.P3_SCENE)

    private fun onlyR1(): RecorderLibrary = Fx.openLibrary().let { lib ->
        RecorderLibrary(listOf(lib.record(Fx.R1)!!), lib.linksOf(Fx.R1), emptyList())
    }

    private fun raisedToR18(doc: ProjectDoc) = doc.copy(
        project = doc.project.copy(rating = Rating.R18.open(), protections = doc.project.protections.copy(recorderLinkMask = true)),
    )

    @Test
    fun LRC_T29_maskAppliesAtOnce() {
        val library = onlyR1()
        assertEquals(1, RecorderHooks.linkedRecordCount(library, Fx.P1))
        val before = Fx.env()
        assertEquals(listOf("1-7 씬 3", "하르트"), Inbox.row(library, library.record(Fx.R1)!!, before, Fx.NOW).linkTags.map { it.text })

        val raised = Fx.env(p1 = raisedToR18(Fx.p1Doc()))
        val tags = Inbox.row(library, library.record(Fx.R1)!!, raised, Fx.NOW).linkTags
        assertEquals(listOf("이름 가림"), tags.map { it.text })
        assertEquals(2, tags.single().links.size)
        assertEquals(listOf("연결 · 이름 가림"), RecordScreens.of(library, Fx.R1, raised, Fx.NOW)!!.chips.map { it.text })

        // The same while the project is open.
        val open = Fx.env(p1 = raisedToR18(Fx.p1Doc()), unlock = UnlockSession(hiddenGroupUnlockedAt = Fx.NOW, itemsUnlocked = mapOf(Fx.P1 to Fx.NOW)))
        assertEquals(listOf("이름 가림"), Inbox.row(library, library.record(Fx.R1)!!, open, Fx.NOW).linkTags.map { it.text })
    }

    private fun libraryLinkedTo(target: TargetRef): RecorderLibrary =
        LinkEdits.add(Fx.openLibrary(), Fx.R3, target, LinkOrigin.MANUAL, Fx.ctx())

    private fun r3Tags(library: RecorderLibrary, env: RecorderEnv) = Inbox.row(library, library.record(Fx.R3)!!, env, Fx.NOW).linkTags.map { it.text }

    @Test
    @Ignore("spec conflict: P3 has 라이프 레코더 연결 가리기 on (§4.1), so after unlocking the §3.3 order gives `R-18`, not the scene label")
    fun LRC_T30_maskingTable() {
        val library = libraryLinkedTo(p3Scene)
        assertEquals(listOf("숨긴 프로젝트"), r3Tags(library, Fx.env()))
        assertEquals(listOf("1-1 씬 1"), r3Tags(library, Fx.env(unlock = UnlockSession(hiddenGroupUnlockedAt = Fx.NOW))))
    }

    @Test
    fun LRC_T30_fixture_maskingTable() {
        val library = libraryLinkedTo(p3Scene)
        assertEquals(listOf("숨긴 프로젝트"), r3Tags(library, Fx.env()))
        assertEquals(listOf("이름 가림"), r3Tags(library, Fx.env(unlock = UnlockSession(hiddenGroupUnlockedAt = Fx.NOW))))
        // A hidden project without the rating mask shows the scene label once the group is open.
        val unmasked = Fx.p3Doc().let { it.copy(project = it.project.copy(protections = Protections(hidden = true))) }
        assertEquals(listOf("숨긴 프로젝트"), r3Tags(library, Fx.env(p3 = unmasked)))
        assertEquals(listOf("1-1 씬 1"), r3Tags(library, Fx.env(p3 = unmasked, unlock = UnlockSession(hiddenGroupUnlockedAt = Fx.NOW))))

        val noahHidden = Fx.withNoahLive(Fx.p1Doc(), hidden = true)
        val toNoah = libraryLinkedTo(Fx.noah)
        assertEquals(listOf("숨긴 항목"), r3Tags(toNoah, Fx.env(p1 = noahHidden)))
        assertEquals(listOf("노아"), r3Tags(toNoah, Fx.env(p1 = noahHidden, unlock = UnlockSession(itemsUnlocked = mapOf(Fx.P1 to Fx.NOW)))))

        val hiddenRecord = toNoah.withRecord(Fx.R3) { it.copy(hidden = true) }
        val screen = Inbox.screen(hiddenRecord, InboxFilterState(), Fx.env(p1 = noahHidden), Fx.NOW)
        assertTrue(screen.rows.none { it.recordKey == Fx.R3 })
        assertTrue(Chips.inboxTags(hiddenRecord.record(Fx.R3)!!, hiddenRecord.linksOf(Fx.R3), Fx.env(p1 = noahHidden)).isEmpty())
        assertNull(RecordScreens.of(hiddenRecord, Fx.R3, Fx.env(p1 = noahHidden), Fx.NOW))
    }

    @Test
    fun LRC_T31_hideRowDefault() {
        val settings = RecorderSettings()
        val ctx = Fx.ctx()
        val openGroup = Fx.env(unlock = UnlockSession(hiddenGroupUnlockedAt = Fx.NOW))
        val fromP3 = EntryLinks.script(Fx.P3, Fx.P3_SCENE, null)
        fun processed(env: RecorderEnv, entry: io.github.lenticualrstone.civilight.model.recorder.EntryContext) =
            Recording.stopAndSave(Recording.final(Recording.start("01JZ000000TSTK000000000800", entry, settings, env, "a", ctx), "생각"), RecorderLibrary(), ctx)

        val (s, lib) = processed(openGroup, fromP3)
        val screen = Recording.processingScreen(s, openGroup, finalising = false, audioKept = true)
        assertTrue(screen.hideRow!!.on)
        assertEquals("숨기기", screen.hideRow.title)
        assertTrue(Recording.keepInInbox(s, lib, ctx).record(s.recordKey)!!.hidden)
        assertFalse(Recording.keepInInbox(Recording.setHideSwitch(s, false), lib, ctx).record(s.recordKey)!!.hidden)

        val (p1Only, _) = processed(openGroup, EntryLinks.project(io.github.lenticualrstone.civilight.model.recorder.EntrySurface.PROJECT_HOME, Fx.P1))
        assertNull(Recording.processingScreen(p1Only, openGroup, finalising = false, audioKept = true).hideRow)

        val openR18 = Fx.p2Doc().let { it.copy(project = it.project.copy(rating = Rating.R18.open())) }
        val r18Env = Fx.env(p2 = openR18)
        val (toR18, _) = processed(r18Env, EntryLinks.project(io.github.lenticualrstone.civilight.model.recorder.EntrySurface.PROJECT_HOME, Fx.P2))
        assertTrue(Recording.processingScreen(toR18, r18Env, finalising = false, audioKept = true).hideRow!!.on)
    }

    @Test
    fun hideRowFollowsTheLinksLive() {
        val env = Fx.env()
        val ctx = Fx.ctx()
        var s = Recording.start("01JZ000000TSTK000000000801", EntryLinks.project(io.github.lenticualrstone.civilight.model.recorder.EntrySurface.PROJECT_HOME, Fx.P1), RecorderSettings(), env, "a", ctx)
        assertNull(s.hideSwitch)
        s = Recording.addLink(s, p3Scene, env, ctx)
        assertEquals(true, s.hideSwitch)
        s = Recording.setHideSwitch(s, false)
        s = Recording.addLink(s, Fx.hart, env, ctx)
        assertEquals(false, s.hideSwitch)
        val p3Link = s.pendingLinks.first { it.projectKey == Fx.P3 }
        s = Recording.removeLink(s, p3Link.key, env)
        assertNull(s.hideSwitch)
    }

    @Test
    fun LRC_T32_captureGuard() {
        val blocked = Fx.p1Doc().let { it.copy(project = it.project.copy(protections = Protections(captureBlock = true))) }
        val env = Fx.env(p1 = blocked)
        val library = Fx.openLibrary()
        assertEquals(setOf(Fx.P1), CaptureGuard.projectsFor(library, listOf(Fx.R1), env))
        assertTrue(CaptureGuard.isSecure(library, listOf(Fx.R1, Fx.R3), env))
        assertFalse(CaptureGuard.isSecure(library, listOf(Fx.R3, Fx.R4), env))
        assertFalse(CaptureGuard.isSecure(library, emptyList(), env))
        // A hidden record while locked shows nothing.
        assertFalse(CaptureGuard.isSecure(library.withRecord(Fx.R1) { it.copy(hidden = true) }, listOf(Fx.R1), env))
    }

    @Test
    fun LRC_T33_searchExclusion() {
        val library = Fx.openLibrary().withRecord(Fx.R4) { it.copy(hidden = true) }
        assertTrue(RecordSearch.search(library, "마지막 등불", Fx.env()).isEmpty())
        val unlocked = Fx.env(unlock = UnlockSession(recordsUnlockedAt = Fx.NOW))
        assertEquals(listOf(Fx.R4), RecordSearch.search(library, "마지막 등불", unlocked).map { it.key })
        assertEquals(listOf(Fx.R4), RecordSearch.search(Fx.openLibrary(), "마지막 등불", Fx.env()).map { it.key })
    }

    @Test
    fun recordsOfALockedHiddenProjectAreNotSearched() {
        val library = LinkEdits.add(Fx.openLibrary(), Fx.R4, p3Scene, LinkOrigin.MANUAL, Fx.ctx())
        assertTrue(RecordSearch.search(library, "마지막 등불", Fx.env()).isEmpty())
        assertEquals(1, RecordSearch.search(library, "마지막 등불", Fx.env(unlock = UnlockSession(hiddenGroupUnlockedAt = Fx.NOW))).size)
    }

    @Test
    fun maskedProjectsInTheProjectMenu() {
        val menu = Inbox.projectMenu(io.github.lenticualrstone.civilight.model.recorder.ProjectFilter.All, Fx.env())
        assertEquals(listOf("모든 프로젝트", "잿빛 새벽의 등불", "유리 정원", "연결 없음"), menu.map { it.label })
        val open = Inbox.projectMenu(io.github.lenticualrstone.civilight.model.recorder.ProjectFilter.All, Fx.env(unlock = UnlockSession(hiddenGroupUnlockedAt = Fx.NOW)))
        assertEquals(listOf("모든 프로젝트", "잿빛 새벽의 등불", "유리 정원", "이름 가림", "연결 없음"), open.map { it.label })
    }

    @Test
    fun masksCollapseByProjectAndKeepOrder() {
        val library = LinkEdits.add(LinkEdits.add(Fx.openLibrary(), Fx.R3, p3Scene, LinkOrigin.MANUAL, Fx.ctx(start = 1)),
            Fx.R3, TargetRef(Fx.P3, RecorderTargetKind.CHARACTER, Fx.P3_SERA), LinkOrigin.MANUAL, Fx.ctx(now = Fx.NOW + 1, start = 50))
        val chips = Chips.captureRow(library.linksOf(Fx.R3), emptyList(), Fx.env())
        assertEquals(listOf("연결 · 숨긴 프로젝트"), chips.map { it.text })
        assertEquals(LinkDisplay.HIDDEN_PROJECT, chips.single().display)
        assertEquals(2, chips.single().links.size)
    }
}
