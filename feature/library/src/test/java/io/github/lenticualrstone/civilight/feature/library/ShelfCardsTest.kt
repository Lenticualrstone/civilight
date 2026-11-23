package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.domain.library.masking.CrossSearchScope
import io.github.lenticualrstone.civilight.domain.library.masking.HiddenAccess
import io.github.lenticualrstone.civilight.domain.library.masking.ProjectProfile
import io.github.lenticualrstone.civilight.feature.library.LibFx.NOW
import io.github.lenticualrstone.civilight.feature.library.LibFx.ZONE
import io.github.lenticualrstone.civilight.feature.library.LibFx.at
import io.github.lenticualrstone.civilight.feature.library.LibFx.shelf
import io.github.lenticualrstone.civilight.feature.library.shelf.RecentsModel
import io.github.lenticualrstone.civilight.feature.library.shelf.RecentsRecorder
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCards
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfProject
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfState
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfUi
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.OnboardingState
import io.github.lenticualrstone.civilight.model.app.RecentItem
import io.github.lenticualrstone.civilight.model.app.RecentKind
import io.github.lenticualrstone.civilight.model.app.RelockPolicy
import io.github.lenticualrstone.civilight.model.app.UnlockSession
import io.github.lenticualrstone.civilight.model.app.ViewMode
import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.PageDoc
import io.github.lenticualrstone.civilight.navigation.ProjectHome
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.TermCard
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** 서재 card view (LIB-04), 최근 연 파일 (LIB-05) and the cross-search scope (LIB-07); spec §4.2. */
class ShelfCardsTest {
    private val ashen = LibFx.ashen()
    private val glass = withCover(LibFx.glass(), holo = true)
    private val oath = LibFx.oath()

    private val projects = listOf(
        shelf(glass, at("2026-11-19T10:00")),
        shelf(oath, at("2026-11-02T10:00")),
        shelf(ashen, at("2026-11-22T08:12")),
    )

    private val locked = HiddenAccess()
    private val open = HiddenAccess(UnlockSession(hiddenGroupUnlockedAt = NOW))

    private fun withCover(doc: ProjectDoc, holo: Boolean): ProjectDoc = doc.copy(
        assets = doc.assets.copy(
            imageUses = doc.assets.imageUses + ImageUse("cover-1", ImageOwnerKind.PROJECT.open(), doc.key, ImageSlot.PROJECT_COVER.open(), "preset:ash-dawn", holo = holo),
        ),
    )

    private fun build(
        list: List<ShelfProject> = projects,
        access: HiddenAccess = locked,
        cards: Boolean = true,
        recents: List<RecentItem> = emptyList(),
    ): ShelfUi = ShelfState.build(list, access, RelockPolicy.ON_LEAVE, RecorderLibrary(), OnboardingState(firstRunCompleted = true), NOW, ZONE, showCards = cards, recentItems = recents)

    private var n = 0
    private fun recent(doc: ProjectDoc, kind: RecentKind, target: Key, at: String) =
        RecentItem(key = "RI${++n}", projectKey = doc.key, kind = kind, targetKey = target, openedAt = at(at))

    @Test
    fun `LIB-04 the card row holds visible projects then one lock card, never a hidden project's card`() {
        val ui = build()
        assertTrue(ui.showCards)
        assertEquals("프로젝트 · 2 · 숨김 1", ui.cardSectionLabel)
        assertEquals(listOf("잿빛 새벽의 등불", "유리 정원"), ui.cards.map { it.row.title })
        assertTrue(ui.cards.none { it.hiddenOpen })
        assertEquals("숨긴 프로젝트 1", ui.lockedGroup?.title)
        assertEquals("이름 · 표지 가림", ui.lockedGroup?.cardSub)
        val cover = ShelfCards.cardItems(ui, wide = false)
        assertEquals(listOf("project", "project", "lock"), cover.map { it::class.simpleName!!.lowercase().removeSuffix("card") })
        assertEquals(3, cover.size)
        val inner = ShelfCards.cardItems(ui, wide = true)
        assertEquals("the wide row adds 새 프로젝트", 4, inner.size)
        assertEquals(ShelfCardItem.NewProject, inner.last())
        assertEquals("every card has its own lazy key", inner.size, inner.map { it.key }.toSet().size)
    }

    @Test
    fun `LIB-04 the open group's projects follow as cards and the lock card leaves`() {
        val ui = build(access = open)
        assertEquals(listOf("잿빛 새벽의 등불", "유리 정원", "검은 서약 (가제)"), ui.cards.map { it.row.title })
        assertEquals(listOf(false, false, true), ui.cards.map { it.hiddenOpen })
        assertTrue(ui.cards.last().row.protections.hidden)
        assertNull(ui.lockedGroup)
        assertTrue("its sheets may show while open", ui.shows(oath.key))
    }

    @Test
    fun `LIB-04 status chip joins 쓰는 중 and 홀로, the cover follows the project's cover image`() {
        val ui = build()
        val byTitle = ui.cards.associateBy { it.row.title }
        assertEquals("쓰는 중", byTitle.getValue("잿빛 새벽의 등불").chip)
        assertEquals("홀로", byTitle.getValue("유리 정원").chip)
        assertEquals("ash-dawn", byTitle.getValue("유리 정원").cover.presetId)
        assertEquals("잿", byTitle.getValue("잿빛 새벽의 등불").cover.initial)
        assertNull(byTitle.getValue("잿빛 새벽의 등불").cover.hash)
        assertEquals("쓰는 중 · 홀로", ShelfCards.chip(writing = true, holo = true))
        assertNull(ShelfCards.chip(writing = false, holo = false))
    }

    @Test
    fun `LIB-04 the view is remembered per screen and both views carry their data`() {
        val settings = AppSettings()
        assertFalse(ShelfCards.showCards(settings))
        val cards = ShelfCards.withView(settings, cards = true)
        assertTrue(ShelfCards.showCards(cards))
        assertEquals(ViewMode.CARD, cards.viewByScreen["library"])
        assertFalse(ShelfCards.showCards(ShelfCards.withView(cards, cards = false)))
        val list = build(cards = false, recents = listOf(recent(ashen, RecentKind.CHARACTER, K.RIEN, "2026-11-21T10:00")))
        assertFalse(list.showCards)
        // Both views carry their data, so a 목록 ↔ 카드 switch can animate the outgoing one (CNV-45).
        assertEquals(listOf("잿빛 새벽의 등불", "유리 정원"), list.cards.map { it.row.title })
        assertEquals(1, list.recents.size)
        assertEquals(listOf(3, 4), listOf(ShelfCards.recentLimit(false), ShelfCards.recentLimit(true)))
        assertFalse(ShelfCards.isWide(390))
        assertTrue(ShelfCards.isWide(560))
    }

    @Test
    fun `LIB-05 recents read as the LibCards board, newest first`() {
        val items = listOf(
            recent(ashen, RecentKind.CHARACTER, K.RIEN, "2026-11-21T10:00"),
            recent(ashen, RecentKind.SCENE, K.SCENE_1_7_3, "2026-11-22T08:12"),
            recent(glass, RecentKind.SCENE, glass.manuscript.liveScenes.first().key, "2026-11-19T10:00"),
        )
        val ui = build(recents = items)
        assertEquals(listOf("씬 · 잿빛 새벽의 등불", "인물 · 잿빛 새벽의 등불", "씬 · 유리 정원"), ui.recents.map { it.kindLine })
        assertEquals("1-7 씬 3 · 꺼진 등불", ui.recents[0].title)
        assertEquals("리엔", ui.recents[1].title)
        assertEquals(listOf("오늘 08:12", "어제", "3일 전"), ui.recents.map { it.time })
        assertTrue(ui.recents.none { it.masked })
    }

    @Test
    fun `S-02 recents of a hidden project, hidden or trashed items are left out, locked or open`() {
        val hiddenRien = ashen.copy(cast = ashen.cast.copy(characters = ashen.cast.characters.map { if (it.key == K.RIEN) it.copy(hidden = true) else it }))
        val list = listOf(shelf(hiddenRien, at("2026-11-22T08:12")), shelf(oath, at("2026-11-02T10:00")))
        val items = listOf(
            recent(hiddenRien, RecentKind.CHARACTER, K.RIEN, "2026-11-22T08:00"),
            recent(hiddenRien, RecentKind.CHARACTER, K.NOAH, "2026-11-22T07:00"),
            recent(oath, RecentKind.CHARACTER, oath.cast.characters.first().key, "2026-11-22T06:00"),
            recent(hiddenRien, RecentKind.SCENE, K.SCENE_1_7_3, "2026-11-21T06:00"),
        )
        listOf(locked, open).forEach { access ->
            val ui = build(list = list, access = access, recents = items)
            assertEquals(listOf("1-7 씬 3 · 꺼진 등불"), ui.recents.map { it.title })
        }
    }

    @Test
    fun `S-02 S-12 a name-masked project reads R-18 프로젝트 without the title and does not block capture`() {
        val masked = ashen.copy(project = ashen.project.copy(protections = ashen.project.protections.copy(nameMask = true, captureBlock = true)))
        val ui = build(list = listOf(shelf(masked, at("2026-11-22T08:12"))), recents = listOf(recent(masked, RecentKind.SCENE, K.SCENE_1_7_3, "2026-11-22T08:12")))
        val card = ui.recents.single()
        assertEquals("씬 · R-18 프로젝트", card.kindLine)
        assertNull(card.title)
        assertTrue(card.masked)
        assertNull("no picture of masked content", card.imageHash)
    }

    @Test
    fun `LIB-05 what an opened screen records, and what it never records`() {
        var k = 0
        val newKey = { "RK${++k}" }
        val scene = RecentsModel.itemOf(Script(ashen.key, sceneKey = K.SCENE_1_7_3), ashen, isExample = false, now = NOW, newKey = newKey)!!
        assertEquals(RecentKind.SCENE to K.SCENE_1_7_3, scene.kind to scene.targetKey)
        val block = ashen.manuscript.liveBlocks.first { ashen.index.block(it.key)?.sceneKey == K.SCENE_1_7_3 }
        assertEquals("a block opens its scene", K.SCENE_1_7_3, RecentsModel.itemOf(Script(ashen.key, blockKey = block.key), ashen, false, NOW, newKey)?.targetKey)
        assertEquals(RecentKind.CHARACTER, RecentsModel.itemOf(CharacterProfile(ashen.key, K.RIEN), ashen, false, NOW, newKey)?.kind)
        assertEquals(RecentKind.PAGE, RecentsModel.itemOf(PageDoc(ashen.key, K.PAGE_LOWER_LIGHT), ashen, false, NOW, newKey)?.kind)
        assertEquals(RecentKind.TERM, RecentsModel.itemOf(TermCard(ashen.key, K.TERM_LANTERN_SOCIETY), ashen, false, NOW, newKey)?.kind)
        assertNull("not a recent target", RecentsModel.itemOf(ProjectHome(ashen.key), ashen, false, NOW, newKey))
        assertNull("the example", RecentsModel.itemOf(CharacterProfile(ashen.key, K.RIEN), ashen, true, NOW, newKey))
        assertNull("a trashed character", RecentsModel.itemOf(CharacterProfile(ashen.key, K.NOAH), ashen, false, NOW, newKey))
        assertNull("a hidden project", RecentsModel.itemOf(CharacterProfile(oath.key, oath.cast.characters.first().key), oath, false, NOW, newKey))
        assertEquals(CharacterProfile(ashen.key, K.RIEN), RecentsModel.routeOf(RecentItem("x", ashen.key, RecentKind.CHARACTER, K.RIEN, NOW)))
    }

    @Test
    fun `the recorder waits for the opened store, then falls back to the stored copy`() = runTest {
        val recorded = mutableListOf<RecentItem>()
        var opened: ProjectDoc? = null
        var calls = 0
        val ports = object : RecentsRecorder.Ports {
            override fun openDoc(projectKey: Key): ProjectDoc? = opened.also { calls++; if (calls == 3) opened = ashen }
            override fun storedDoc(projectKey: Key): ProjectDoc? = null
            override fun isExample(projectKey: Key): Boolean = false
            override suspend fun record(item: RecentItem) {
                recorded += item
            }
        }
        val rec = RecentsRecorder(ports, { NOW }, { "RR" }, waitTries = 5, waitMs = 1)
        assertNotNull(rec.onTop(CharacterProfile(ashen.key, K.RIEN)))
        assertEquals(K.RIEN, recorded.single().targetKey)
        assertNull(rec.onTop(ProjectHome(ashen.key)))

        val example = RecentsRecorder(object : RecentsRecorder.Ports by ports {
            override fun isExample(projectKey: Key) = true
        }, { NOW }, { "RR" }, waitTries = 1, waitMs = 1)
        assertNull(example.onTop(CharacterProfile(ashen.key, K.RIEN)))
        assertEquals(1, recorded.size)
    }

    @Test
    fun `S-03 S-09 LIB-07 cross search leaves hidden projects out while locked and follows the 숨기기 switch while open`() {
        val profiles = listOf(ashen, glass, oath).map { ProjectProfile.of(it.project) }
        val whileLocked = CrossSearchScope.of(profiles, locked, AppSettings())
        assertFalse(oath.key in whileLocked.projectKeys)
        assertEquals("숨긴 프로젝트 제외", whileLocked.footer)
        assertEquals("‘등불’ · 숨긴 프로젝트 제외", CrossSearchScope.emptySub("등불", whileLocked))
        val whileOpen = CrossSearchScope.of(profiles, open, AppSettings(hiddenInSearchWhileOpen = true))
        assertTrue(oath.key in whileOpen.projectKeys)
        assertNull(whileOpen.footer)
        val switchedOff = CrossSearchScope.of(profiles, open, AppSettings(hiddenInSearchWhileOpen = false))
        assertFalse("off: hidden content stays out even while open (§6 Q28)", oath.key in switchedOff.projectKeys)
        assertTrue(ashen.key in switchedOff.projectKeys && glass.key in switchedOff.projectKeys)
    }

    @Test
    fun `D6 every row carries a 48 dp thumbnail made from the cover's 1 to 1 crop`() {
        val ui = build()
        val byTitle = ui.rows.associateBy { it.title }
        assertEquals("ash-dawn", byTitle.getValue("유리 정원").thumb.presetId)
        assertEquals("잿", byTitle.getValue("잿빛 새벽의 등불").thumb.initial)
        assertNull(byTitle.getValue("잿빛 새벽의 등불").thumb.hash)
        val withImage = ashen.copy(
            assets = ashen.assets.copy(
                imageUses = ashen.assets.imageUses + ImageUse("cover-2", ImageOwnerKind.PROJECT.open(), ashen.key, ImageSlot.PROJECT_COVER.open(), "hash-a")
                    .withCrop(Ratio.R3X4, Crop(0, 0, 300, 400))
                    .withCrop(Ratio.R1X1, Crop(10, 10, 300, 300)),
            ),
        )
        val thumb = ShelfCards.thumb(withImage, "잿빛 새벽의 등불")
        assertEquals("hash-a", thumb.hash)
        assertEquals("the list thumbnail is the 1:1 crop", 300, thumb.crop?.h)
        assertEquals("the card cover is the 3:4 crop", 400, ShelfCards.cover(withImage, "잿빛 새벽의 등불").crop?.h)
    }
}
