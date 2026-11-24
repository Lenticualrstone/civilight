package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.feature.canvas.gallery.EntryLabels
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryFilter
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryRows
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GallerySort
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryStyle
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryStyleChoice
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryStyleCopy
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryStyleGroup
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryStyleModels
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryStylePlans
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryTone
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryViewKind
import io.github.lenticualrstone.civilight.feature.canvas.gallery.SlotFacts
import io.github.lenticualrstone.civilight.feature.canvas.gallery.StylePages
import io.github.lenticualrstone.civilight.feature.canvas.gallery.StylePickRules
import io.github.lenticualrstone.civilight.feature.canvas.gallery.StyleSlot
import io.github.lenticualrstone.civilight.feature.canvas.gallery.paneRows
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.ViewMode
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/** 0.2.3 r3 gallery views (`galviews`) and the view picker (`viewpick`): registry, stored choice, plans, masking. */
class GalleryStylesTest {
    private val zone = ZoneId.of("Asia/Seoul")

    // ---------------------------------------------------------------- registry and stored choice

    @Test
    fun `registry has the seven board views then the three old ones, with stable ids`() {
        assertEquals(listOf("film", "mood", "pola", "exhibit", "tone", "just", "line", "list", "grid", "card"), GalleryStyle.ORDER.map { it.id })
        assertEquals(listOf("필름", "무드보드", "빨랫줄", "전시실", "색 묶음", "태그 줄", "씬 순서", "목록", "격자", "카드 진열"), GalleryStyle.ORDER.map { it.label })
        assertEquals(7, GalleryStyle.entries.count { it.group == GalleryStyleGroup.NEW && it.isNew })
        assertEquals(GalleryStyle.entries.size, GalleryStyle.entries.map { it.id }.toSet().size)
        assertNull(GalleryStyle.ofId("fan"))
        assertNull(GalleryStyle.ofId(null))
        assertEquals(GalleryViewKind.CARD, GalleryStyle.FILM.kind)
        assertEquals(GalleryViewKind.GRID, GalleryStyle.GRID.kind)
    }

    @Test
    fun `stored choice keeps the old key readable and stores the r3 view by id`() {
        val kind = GalleryOwnerKind.CHARACTER
        assertEquals(GalleryStyle.CARD, GalleryStyleChoice.of(AppSettings(), kind))
        assertEquals(GalleryStyle.LIST, GalleryStyleChoice.of(AppSettings(viewByScreen = mapOf(AppSettings.GALLERY_CHARACTER to ViewMode.LIST)), kind))

        val start = AppSettings(viewByScreen = mapOf("library" to ViewMode.LIST, AppSettings.GALLERY_PROJECT to ViewMode.GRID))
        val film = GalleryStyleChoice.write(start, kind, GalleryStyle.FILM)
        assertEquals(GalleryStyle.FILM, GalleryStyleChoice.of(film, kind))
        assertEquals(mapOf(AppSettings.GALLERY_CHARACTER to "film"), film.galleryStyle)
        // An older build reads `카드` from the old key.
        assertEquals(ViewMode.CARD, film.viewByScreen[AppSettings.GALLERY_CHARACTER])
        // Other screens untouched.
        assertEquals(ViewMode.LIST, film.viewByScreen["library"])
        assertEquals(GalleryStyle.GRID, GalleryStyleChoice.of(film, GalleryOwnerKind.PROJECT))

        val mood = GalleryStyleChoice.write(film, kind, GalleryStyle.MOOD)
        assertEquals(1, mood.galleryStyle.size)
        assertEquals(GalleryStyle.MOOD, GalleryStyleChoice.of(mood, kind))

        val list = GalleryStyleChoice.write(mood, kind, GalleryStyle.LIST)
        assertTrue(list.galleryStyle.isEmpty())
        assertEquals(GalleryStyle.LIST, GalleryStyleChoice.of(list, kind))

        // An id from a newer build that this one does not know reads the old key.
        val unknown = AppSettings(viewByScreen = mapOf(AppSettings.GALLERY_CHARACTER to ViewMode.GRID), galleryStyle = mapOf(AppSettings.GALLERY_CHARACTER to "orbit"))
        assertEquals(GalleryStyle.GRID, GalleryStyleChoice.of(unknown, kind))
    }

    // ---------------------------------------------------------------- plans over any count

    private fun facts(n: Int, lock: Boolean): List<SlotFacts> =
        List(n) { i ->
            SlotFacts(
                ratio = listOf(0.75f, 1.5f, 1f, 0.8f, 1.6f)[i % 5],
                tagGroup = listOf("삽화", null, "설정화", "소품", "삽화")[i % 5],
                tagOrder = listOf(0, null, 1, 2, 0)[i % 5],
                scenePos = if (i % 3 == 0) 100 - i else null,
            )
        } + if (lock) listOf(SlotFacts(GalleryStylePlans.LOCK_RATIO, lock = true)) else emptyList()

    private fun assertCovers(name: String, size: Int, order: List<Int>) {
        assertEquals("$name covers every place once", (0 until size).toList(), order.sorted())
    }

    @Test
    fun `every view's plan covers 0, 1, 2, 9 and 30 places, with and without the lock bundle`() {
        for (n in listOf(0, 1, 2, 9, 30)) for (lock in listOf(false, true)) {
            val f = facts(n, lock)
            val size = f.size
            val plans = mapOf(
                "film" to GalleryStylePlans.film(f),
                "mood" to GalleryStylePlans.mood(f),
                "line" to GalleryStylePlans.clothesline(f),
                "exhibit cover" to GalleryStylePlans.exhibit(f, inner = false),
                "exhibit inner" to GalleryStylePlans.exhibit(f, inner = true),
                "tags" to GalleryStylePlans.tagStylePages(GalleryStylePlans.tagRows(f)),
                "tone" to GalleryStylePlans.tone(f.map { if (it.lock) GalleryTone.LOCK else GalleryTone.entries[(it.ratio * 10).toInt() % 3] }).first,
            )
            plans.forEach { (name, p) ->
                assertCovers("$name n=$n lock=$lock", size, p.order)
                p.pages.forEach { assertTrue("$name page not empty", it.isNotEmpty()) }
                if (size > 0) p.order.forEach { i -> assertTrue(p.pageOf(i) in p.pages.indices) }
            }
            assertTrue(GalleryStylePlans.mood(f).pages.all { it.size <= GalleryStylePlans.MOOD_PAGE })
            assertTrue(GalleryStylePlans.clothesline(f).pages.all { it.size <= GalleryStylePlans.LINE_PAGE })
            assertTrue(GalleryStylePlans.exhibit(f, true).pages.all { it.size <= GalleryStylePlans.EXHIBIT_WALL })
            assertTrue(GalleryStylePlans.tagPages(GalleryStylePlans.tagRows(f)).all { it.size <= GalleryStylePlans.TAG_ROWS_PAGE })
            assertTrue(GalleryStylePlans.tagRows(f).all { it.slots.size in 1..GalleryStylePlans.TAG_ROW_MAX })
            val scene = GalleryStylePlans.scene(f)
            assertCovers("scene n=$n lock=$lock", size, scene.order)
            assertTrue(scene.placed.pages.all { it.size <= GalleryStylePlans.SCENE_ROWS_PAGE })
            if (lock) assertEquals(size - 1, scene.lane.last())
            // ◀ ▶ never leave the order and stop at the ends.
            val order = GalleryStylePlans.mood(f).order
            if (order.isNotEmpty()) {
                assertEquals(order.first(), GalleryStylePlans.step(order, order.first(), -1))
                assertEquals(order.last(), GalleryStylePlans.step(order, order.last(), 1))
                assertFalse(GalleryStylePlans.hasPrev(order, order.first()))
                assertFalse(GalleryStylePlans.hasNext(order, order.last()))
            }
            assertEquals(if (size == 0) -1 else 0, GalleryStylePlans.selection(size, -1, -1))
        }
    }

    @Test
    fun `scene order places live scenes in manuscript order and the rest in the lane, locks last`() {
        val f = listOf(
            SlotFacts(1f, scenePos = 7),
            SlotFacts(1f),
            SlotFacts(1f, scenePos = 2),
            SlotFacts(1f, scenePos = null),
            SlotFacts(0.7f, lock = true),
        )
        val p = GalleryStylePlans.scene(f)
        assertEquals(listOf(listOf(2, 0)), p.placed.pages)
        assertEquals(listOf(1, 3, 4), p.lane)
        assertEquals(listOf(2, 0, 1, 3, 4), p.order)
        assertEquals("1-3" to "씬 2", GalleryStylePlans.splitCode("1-3 씬 2"))
        assertEquals("프롤로그" to "", GalleryStylePlans.splitCode("프롤로그"))
    }

    @Test
    fun `tag rows group by first tag, balance rows, share a row for small groups, untagged and locks last`() {
        assertEquals(listOf(listOf(0, 1), listOf(2, 3)), GalleryStylePlans.balanced(listOf(0, 1, 2, 3), 3))
        assertEquals(listOf(listOf(0, 1, 2), listOf(3, 4)), GalleryStylePlans.balanced(listOf(0, 1, 2, 3, 4), 3))
        // The board: 삽화 4 (two rows), 설정화 · 소품 (one row), 태그 없음 · 숨김.
        val f = listOf(
            SlotFacts(0.75f, tagGroup = "삽화", tagOrder = 0),
            SlotFacts(0.75f, tagGroup = "설정화", tagOrder = 1),
            SlotFacts(0.8f, tagGroup = "삽화", tagOrder = 0),
            SlotFacts(1.5f),
            SlotFacts(1.33f, tagGroup = "삽화", tagOrder = 0),
            SlotFacts(1f, tagGroup = "소품", tagOrder = 2),
            SlotFacts(1.6f, tagGroup = "삽화", tagOrder = 0),
            SlotFacts(0.75f, tagGroup = "설정화", tagOrder = 1),
            SlotFacts(0.7f, lock = true),
        )
        val rows = GalleryStylePlans.tagRows(f)
        assertEquals(listOf("삽화 4", null, "설정화 · 소품", "태그 없음 · 숨김"), rows.map { it.label })
        assertEquals(listOf(listOf(0, 2), listOf(4, 6), listOf(1, 7, 5), listOf(3, 8)), rows.map { it.slots })
        val w = GalleryStylePlans.rowWeights(rows, f)
        assertTrue(w.all { it > 0f })
        // A row of wide pictures is lower than a row of tall ones (justified rows).
        assertTrue(w[1] < w[0])
    }

    @Test
    fun `colour groups follow the picture's main colour`() {
        assertEquals(GalleryTone.WARM, GalleryStylePlans.presetTone("lantern"))
        assertEquals(GalleryTone.WARM, GalleryStylePlans.presetTone("red-citadel"))
        assertEquals(GalleryTone.COOL, GalleryStylePlans.presetTone("deep-sea"))
        assertEquals(GalleryTone.COOL, GalleryStylePlans.presetTone("nebula"))
        assertEquals(GalleryTone.GREY, GalleryStylePlans.presetTone("ash-dawn"))
        assertEquals(GalleryTone.GREY, GalleryStylePlans.classify(8, 8, 9))
        assertEquals(GalleryTone.GREY, GalleryStylePlans.presetTone("unknown"))
        val (pages, of) = GalleryStylePlans.tone(List(14) { GalleryTone.WARM } + GalleryTone.COOL + GalleryTone.LOCK)
        assertEquals(listOf(6, 6, 2, 1, 1), pages.pages.map { it.size })
        assertEquals(listOf(GalleryTone.WARM, GalleryTone.WARM, GalleryTone.WARM, GalleryTone.COOL, GalleryTone.LOCK), of)
        assertEquals(listOf(1, 2), GalleryStylePlans.toneRows(3))
        assertEquals(listOf(1, 1), GalleryStylePlans.toneRows(2))
        assertEquals(listOf(2, 2, 2), GalleryStylePlans.toneRows(6))
        assertEquals(emptyList<Int>(), GalleryStylePlans.toneRows(0))
    }

    @Test
    fun `counts, ratios and geometry`() {
        val f = listOf(SlotFacts(1f), SlotFacts(1f), SlotFacts(0.7f, lock = true, lockCount = 2), SlotFacts(0.7f, lock = true))
        val order = listOf(1, 0, 2, 3)
        assertEquals("1 / 2", GalleryStylePlans.count(f, order, 1))
        assertEquals("2 / 2", GalleryStylePlans.count(f, order, 0))
        assertEquals("숨김 2", GalleryStylePlans.count(f, order, 2))
        // The hidden characters' bundle has no count.
        assertEquals("숨김", GalleryStylePlans.count(f, order, 3))
        assertEquals("", GalleryStylePlans.count(f, order, 9))
        assertEquals("3:4", GalleryStylePlans.ratioText(2400, 3200))
        assertEquals("16:10", GalleryStylePlans.ratioText(3200, 2000))
        assertEquals("4:3", GalleryStylePlans.ratioText(2660, 2000))
        assertEquals("1.42:1", GalleryStylePlans.ratioText(1420, 1000))
        assertEquals(0.75f, GalleryStylePlans.ratioOf(0, 0, preset = false))
        assertEquals(3.5f, GalleryStylePlans.ratioOf(9000, 100, preset = false))
        assertEquals(100f to 75f, GalleryStylePlans.fit(100f, 200f, 4f / 3f))
        assertEquals(0f to 0f, GalleryStylePlans.fit(0f, 200f, 1f))
        (GalleryStylePlans.MOOD_COVER + GalleryStylePlans.MOOD_INNER).forEach { r ->
            assertTrue(r.x >= 0f && r.y >= 0f && r.x + r.w <= 1.01f && r.y + r.h <= 1.01f)
        }
        assertEquals(9, GalleryStylePlans.MOOD_COVER.size)
        assertEquals(24f, GalleryStylePlans.polaroidPictureH(40f, 7f, 28f))
    }

    @Test
    fun `picker rules wrap and only apply another view`() {
        assertEquals(9, StylePickRules.wrap(-1, 10))
        assertEquals(0, StylePickRules.wrap(10, 10))
        assertFalse(StylePickRules.canApply(GalleryStyle.FILM, GalleryStyle.FILM))
        assertTrue(StylePickRules.canApply(GalleryStyle.MOOD, GalleryStyle.FILM))
        assertEquals(1, StylePickRules.swipeStep(-80f, 56f))
        assertEquals(-1, StylePickRules.swipeStep(80f, 56f))
        assertEquals(0, StylePickRules.swipeStep(20f, 56f))
    }

    // ---------------------------------------------------------------- masking on fixture G0

    private fun model(locked: Boolean) = ExampleGallery.build().let { doc ->
        val v = GalleryRows.build(doc, GalleryOwnerKind.CHARACTER, ExampleKeys.RIEN, locked, GalleryFilter.All, GallerySort.MANUAL)
        Triple(doc, v, GalleryStyleModels.build(doc, v, locked, zone))
    }

    @Test
    fun `locked gallery shows one lock bundle with no key, title or picture of a hidden image`() {
        val (doc, v, m) = model(locked = true)
        assertEquals(v.count, m.itemCount)
        val lock = m.slots.last()
        assertTrue(lock is StyleSlot.Locked)
        assertEquals(v.hiddenLocked, (lock as StyleSlot.Locked).count)
        assertTrue(v.hiddenLocked > 0)
        val hidden = doc.galleryItems.filter { it.hidden }.map { it.key }.toSet()
        assertTrue(hidden.isNotEmpty())
        m.slots.filterIsInstance<StyleSlot.Item>().forEach { assertFalse(it.entry.key in hidden) }
        assertFalse(m.slots.any { it.id in hidden })
        // Picking the bundle is screen state: the selection rule never yields a key for it.
        assertEquals(m.lockIndex, GalleryStylePlans.selection(m.slots.size, 0, m.lockIndexOf(StyleSlot.LOCK_ID)))
        assertEquals(-1, m.lockIndexOf(StyleSlot.LOCK_OWNERS_ID))
        assertNull(m.item(m.lockIndex))
        assertEquals("숨긴 이미지", lock.label)
        assertEquals(GalleryStyleCopy.hiddenCount(v.hiddenLocked), GalleryStylePlans.count(m.facts, m.facts.indices.toList(), m.lockIndex))
    }

    @Test
    fun `two lock bundles are told apart and the characters' one says so`() {
        val owners = StyleSlot.Locked(0, owners = true)
        val images = StyleSlot.Locked(3, owners = false)
        assertEquals("숨긴 인물", owners.label)
        assertEquals("숨긴 이미지", images.label)
        assertFalse(owners.id == images.id)
        assertEquals(0, owners.facts.lockCount)
        assertEquals(3, images.facts.lockCount)
    }

    @Test
    fun `taps pick then open, toggle in multi, moves never toggle the multi set`() {
        val (_, _, m) = model(locked = true)
        val picks = ArrayList<Int>()
        val moves = ArrayList<Int>()
        val opens = ArrayList<Int>()
        fun state(sel: Int, multi: Set<String>?, interactive: Boolean = true) = io.github.lenticualrstone.civilight.feature.canvas.gallery.StyleBodyState(
            GalleryStyle.MOOD, m, inner = false, sel = sel, interactive = interactive,
            onPick = { picks += it }, onOpen = { opens += it }, multi = multi, onMove = { moves += it },
        )
        state(0, null).tap(1)
        assertEquals(listOf(1), picks)
        state(1, null).tap(1)
        assertEquals(listOf(1), opens)
        // 여러 장 고르기: a tap toggles (onPick), the lock bundle does nothing, ◀ ▶ only move.
        state(0, emptySet()).tap(2)
        state(0, emptySet()).tap(m.lockIndex)
        assertEquals(listOf(1, 2), picks)
        state(0, emptySet()).go(3)
        assertEquals(listOf(3), moves)
        assertEquals(listOf(1, 2), picks)
        // A preview reacts to nothing.
        state(0, null, interactive = false).tap(1)
        state(0, null, interactive = false).go(1)
        assertEquals(listOf(1, 2), picks)
        assertEquals(listOf(3), moves)
        // Out of range moves are ignored.
        state(0, null).go(m.slots.size)
        assertEquals(listOf(3), moves)
    }

    @Test
    fun `the stored view survives the settings codec`() {
        val settings = GalleryStyleChoice.write(AppSettings(), GalleryOwnerKind.PROJECT, GalleryStyle.EXHIBIT)
        val back = io.github.lenticualrstone.civilight.store.settings.AppSettingsCodec.decode(io.github.lenticualrstone.civilight.store.settings.AppSettingsCodec.encode(settings))
        assertEquals(GalleryStyle.EXHIBIT, GalleryStyleChoice.of(back, GalleryOwnerKind.PROJECT))
        assertEquals(GalleryStyle.CARD, GalleryStyleChoice.of(back, GalleryOwnerKind.CHARACTER))
        assertEquals(ViewMode.LIST, back.viewByScreen["library"])
    }

    @Test
    fun `unlocked gallery has no bundle and every image`() {
        val (_, v, m) = model(locked = false)
        assertEquals(-1, m.lockIndex)
        assertEquals(v.count, m.slots.size)
    }

    @Test
    fun `scene codes and times only for live visible scenes, pane rows read 없음 where empty`() {
        val (doc, _, m) = model(locked = true)
        val labels = EntryLabels(doc, true)
        m.slots.filterIsInstance<StyleSlot.Item>().forEach { s ->
            val live = labels.sceneState(s.entry.item.sceneKey) == EntryLabels.SceneState.LIVE
            if (!live) {
                assertNull(s.info.sceneCode)
                assertNull(s.info.sceneTime)
                assertNull(s.info.scenePos)
            }
            val rows = paneRows(s.info)
            assertEquals(listOf("태그", "씬", "때", "종류", "비율", "크기", "넣은 날", "쓰인 곳"), rows.map { it.first })
            rows.forEach { assertTrue(it.second.isNotBlank()) }
        }
        val untitled = m.slots.filterIsInstance<StyleSlot.Item>().filter { it.info.untitled }
        untitled.forEach { assertEquals("제목 없음", it.info.title) }
        assertTrue(StylePages(emptyList()).order.isEmpty())
    }

    // ---------------------------------------------------------------- 0.2.3 review fixes

    @Test
    fun `scene order page never points past the placed pages`() {
        // A lane pick keeps the last page; the placed list then shrank to one page.
        assertEquals(0, GalleryStylePlans.clampPage(1, 0))
        assertEquals(0, GalleryStylePlans.clampPage(3, -1))
        assertEquals(1, GalleryStylePlans.clampPage(1, 2))
        assertEquals(0, GalleryStylePlans.clampPage(-1, 2))
    }

    @Test
    fun `the first slot fallback is not an explicit pick`() {
        assertFalse(GalleryStylePlans.isExplicit(5, -1, -1))
        assertEquals(0, GalleryStylePlans.selection(5, -1, -1))
        assertTrue(GalleryStylePlans.isExplicit(5, 0, -1))
        assertTrue(GalleryStylePlans.isExplicit(5, -1, 4))
        assertFalse(GalleryStylePlans.isExplicit(5, 7, -1))
        assertFalse(GalleryStylePlans.isExplicit(0, 0, -1))
    }
}
