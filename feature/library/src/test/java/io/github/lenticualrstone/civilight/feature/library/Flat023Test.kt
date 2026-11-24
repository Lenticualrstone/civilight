package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.designsystem.theme.ColorPalettes
import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole
import io.github.lenticualrstone.civilight.domain.library.masking.HiddenGroupRow
import io.github.lenticualrstone.civilight.feature.library.shelf.ProjectCardUi
import io.github.lenticualrstone.civilight.feature.library.shelf.ProjectRowUi
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfState
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfViewRenderers
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfViews
import io.github.lenticualrstone.civilight.feature.library.shelf.flat.CellState
import io.github.lenticualrstone.civilight.feature.library.shelf.flat.FillRows
import io.github.lenticualrstone.civilight.feature.library.shelf.flat.FlatViewCopy
import io.github.lenticualrstone.civilight.feature.library.shelf.flat.FlatViews
import io.github.lenticualrstone.civilight.feature.library.shelf.flat.PaperGrid
import io.github.lenticualrstone.civilight.feature.library.shelf.flat.PickGroup
import io.github.lenticualrstone.civilight.feature.library.shelf.flat.ProgressCells
import io.github.lenticualrstone.civilight.feature.library.shelf.flat.RowFacts
import io.github.lenticualrstone.civilight.feature.library.shelf.flat.RowFactsCache
import io.github.lenticualrstone.civilight.feature.library.shelf.flat.TableOrder
import io.github.lenticualrstone.civilight.feature.library.shelf.flat.TableSort
import io.github.lenticualrstone.civilight.feature.library.shelf.flat.TimelineLayout
import io.github.lenticualrstone.civilight.feature.library.shelf.flat.ViewPick
import io.github.lenticualrstone.civilight.feature.library.shelf.flat.WallLayout
import io.github.lenticualrstone.civilight.feature.library.shelf.views.LookB
import io.github.lenticualrstone.civilight.feature.library.shelf.views.ShelfScene
import io.github.lenticualrstone.civilight.feature.library.shelf.views.StarsGeo
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.LibraryStyle
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.ui.image.CoverUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** 0.2.3 lane library: the r3 flat views, the view picker and the full-screen scene (JVM, no device). */
class Flat023Test {
    private fun row(title: String, facts: RowFacts? = null, ageDays: Float = 0f, edited: String = "오늘 08:12") = ProjectRowUi(
        key = Keys.newKey(),
        title = title,
        rating = Rating.ALL,
        meta = "",
        writing = false,
        protections = Protections(),
        readOnly = false,
        facts = facts,
        edited = edited,
        ageDays = ageDays,
    )

    private fun item(r: ProjectRowUi) = ShelfCardItem.Project(ProjectCardUi(r, null, CoverUi(null, null, null, r.title, holo = false), hiddenOpen = false))

    private val lock = ShelfCardItem.Lock(HiddenGroupRow(2))

    // ------------------------------------------------------------ enum and registry

    @Test
    fun `the seven flat styles are appended and old stored values still read`() {
        val names = LibraryStyle.entries.map { it.name }
        assertEquals(listOf("BOOKSHELF", "COVER_SPIN", "MANUSCRIPT_STACK", "ARCHIVE_HALL", "INDEX_DRAWER", "CONSTELLATION", "LIST"), names.take(7))
        assertEquals(FlatViews.styles, LibraryStyle.entries.drop(7))
        // 0.2.2 settings with a 3D style read as before; a new value round-trips; an unknown one falls back.
        val old = CivilightJson.decode(AppSettings.serializer(), """{"libraryStyle":"archiveHall"}""")
        assertEquals(LibraryStyle.ARCHIVE_HALL, old.libraryStyle)
        val json = CivilightJson.encode(AppSettings.serializer(), AppSettings(libraryStyle = LibraryStyle.COVER_WALL))
        assertEquals(LibraryStyle.COVER_WALL, CivilightJson.decode(AppSettings.serializer(), json).libraryStyle)
        assertEquals(LibraryStyle.BOOKSHELF, CivilightJson.decode(AppSettings.serializer(), """{"libraryStyle":"someFutureView"}""").libraryStyle)
    }

    @Test
    fun `every flat style has a real renderer, never the 3D scene`() {
        for (s in FlatViews.styles) {
            assertTrue("$s registered", s in ShelfViewRenderers.available)
            assertFalse("$s is not a 3D style", s in ShelfScene.styles)
            val view = ShelfViews.of(AppSettings(libraryStyle = s))
            assertEquals(s, view.rendered)
            assertNotNull(view.toggle)
        }
        assertEquals(6, ShelfScene.styles.size)
    }

    // ------------------------------------------------------------ facts (masked, cached)

    @Test
    fun `row facts read characters, cast, cells and the last place`() {
        val doc = LibFx.ashen()
        val f = RowFacts.of(doc)
        assertTrue(f.chars > 0)
        assertTrue(f.cast > 0)
        val cells = f.progress!!
        assertTrue(cells.total > 0)
        assertEquals(cells.flat.size, cells.total)
        assertTrue(cells.done <= cells.total)
        assertNotNull(f.place)
        // 설정만 has no cells.
        assertNull(RowFacts.of(LibFx.oath()).progress)
    }

    @Test
    fun `a hidden scene never shows its title in the last place`() {
        val doc = LibFx.ashen()
        val blockKey = doc.app.activity.activity.lastEditedBlockKey!!
        val sceneKey = doc.index.block(blockKey)!!.sceneKey
        val scene = doc.index.unit(sceneKey)!!
        val shown = RowFacts.of(doc).place!!
        scene.title?.let { assertTrue("visible title is shown", shown.contains(it)) }
        val hidden = doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == sceneKey) it.copy(hidden = true) else it }))
        val masked = RowFacts.of(hidden).place!!
        scene.title?.let { assertFalse("hidden title is not shown", masked.contains(it)) }
    }

    @Test
    fun `a name-masked project keeps scene titles off the 서재`() {
        val doc = LibFx.ashen()
        val masked = doc.copy(project = doc.project.copy(protections = doc.project.protections.copy(nameMask = true)))
        val sceneKey = doc.index.block(doc.app.activity.activity.lastEditedBlockKey!!)!!.sceneKey
        val title = doc.index.unit(sceneKey)!!.title
        if (title != null) assertFalse(RowFacts.of(masked).place!!.contains(title))
        assertEquals("정렬 · 글자", FlatViewCopy.sortBy(FlatViewCopy.CHARS))
    }

    @Test
    fun `facts are computed once per document instance`() {
        val cache = RowFactsCache()
        val doc = LibFx.glass()
        val a = cache.of(doc)
        cache.sweep()
        assertSame(a, cache.of(doc))
    }

    @Test
    fun `the lock adds nothing to totals and counts`() {
        val items = listOf(item(row("가", RowFacts(100, 1, null, null))), item(row("나", RowFacts(23, 0, null, null))), lock)
        assertEquals(123, FlatViews.totalChars(items))
        assertEquals("프로젝트 2 · 잠김", FlatViews.countLine(items))
        assertEquals("프로젝트 1", FlatViews.countLine(items.take(1)))
    }

    @Test
    fun `ages count calendar days and a later edit reads younger`() {
        val now = LibFx.at("2026-11-24T10:00")
        val morning = ShelfState.ageDays(LibFx.at("2026-11-24T08:00"), now, LibFx.ZONE)
        val later = ShelfState.ageDays(LibFx.at("2026-11-24T09:30"), now, LibFx.ZONE)
        val yesterday = ShelfState.ageDays(LibFx.at("2026-11-23T23:50"), now, LibFx.ZONE)
        assertEquals(0, morning.toInt())
        assertTrue(later < morning)
        assertEquals(1, yesterday.toInt())
    }

    // ------------------------------------------------------------ layouts

    @Test
    fun `timeline segments and no overlap from one project to many`() {
        assertEquals(0, TimelineLayout.segmentOf(0))
        assertEquals(1, TimelineLayout.segmentOf(1))
        assertEquals(2, TimelineLayout.segmentOf(6))
        assertEquals(3, TimelineLayout.segmentOf(7))
        assertEquals(5, TimelineLayout.segmentOf(400))
        for (n in listOf(1, 7, 30)) {
            // Worst case: everything edited today.
            val (xs, w) = TimelineLayout.place(List(n) { .1f }, 451f, 54f, 56f)
            assertEquals(n, xs.size)
            assertTrue(w >= 451f)
            for (side in 0..1) {
                val s = xs.filterIndexed { i, _ -> i % 2 == side }
                s.zipWithNext().forEach { (a, b) -> assertTrue("n=$n gap ${b - a}", b - a >= 60f - .01f) }
            }
            xs.forEach { x -> assertTrue(x <= w - 56f) }
        }
        // Recent on the left.
        val (xs, _) = TimelineLayout.place(listOf(.1f, 40f), 451f, 54f, 56f)
        assertTrue(xs[0] < xs[1])
        // Old projects stay on the old side (pushed back left from the right bound, not to the start).
        val (old, w) = TimelineLayout.place(List(3) { 60f }, 451f, 54f, 56f)
        val segW = (w - 56f - 12f) / TimelineLayout.SEGMENTS.size
        old.forEach { x -> assertTrue("x=$x is in 이전", x >= 12f + segW * 4f) }
        // Mixed ages keep their order on each side and never overlap.
        val ages = listOf(.2f, .5f, 1.4f, 3f, 9f, 20f, 45f, 70f, 80f)
        val (mx, _) = TimelineLayout.place(ages, 451f, 54f, 56f)
        for (side in 0..1) mx.filterIndexed { i, _ -> i % 2 == side }.zipWithNext().forEach { (a, b) -> assertTrue(b - a >= 60f - .01f) }
    }

    @Test
    fun `wall fills the height or keeps the smallest tile`() {
        val cols = WallLayout.columns(8, 2)
        assertEquals(listOf(listOf(0, 2, 4, 6), listOf(1, 3, 5, 7)), cols)
        val w = cols.map { c -> c.map { WallLayout.WEIGHTS[it % 7] } }
        val unit = WallLayout.unit(w, 800f, 8f)
        assertTrue(w.any { c -> kotlin.math.abs(c.sum() * unit + 8f * (c.size - 1) - 800f) < .5f })
        val many = WallLayout.columns(40, 2).map { c -> c.map { WallLayout.WEIGHTS[it % 7] } }
        val u2 = WallLayout.unit(many, 600f, 8f)
        assertTrue(many.flatten().all { it * u2 >= WallLayout.MIN_TILE - .01f })
    }

    @Test
    fun `rows fill the room down to their smallest size`() {
        assertEquals(100f, FillRows.height(5, 524f, 6f, 44f), .01f)
        assertEquals(44f, FillRows.height(40, 524f, 6f, 44f), .01f)
    }

    @Test
    fun `table sorts by column and pins the lock last`() {
        val a = item(row("가", RowFacts(10, 0, null, null)))
        val b = item(row("다", RowFacts(30, 0, null, null)))
        val c = item(row("나", RowFacts(20, 0, null, null)))
        val items = listOf(a, b, lock, c)
        assertEquals(listOf(a, b, c, lock), TableOrder.order(items, TableSort.EDITED, false))
        assertEquals(listOf(a, c, b, lock), TableOrder.order(items, TableSort.NAME, false))
        assertEquals(listOf(b, c, a, lock), TableOrder.order(items, TableSort.CHARS, true))
        assertEquals(TableSort.CHARS to true, TableOrder.toggle(TableSort.EDITED, false, TableSort.CHARS))
        assertEquals(TableSort.CHARS to false, TableOrder.toggle(TableSort.CHARS, true, TableSort.CHARS))
        assertEquals(TableSort.NAME to false, TableOrder.toggle(TableSort.EDITED, false, TableSort.NAME))
    }

    @Test
    fun `원고지 puts one glyph a cell`() {
        assertEquals(listOf("잿빛 새벽의 ", "등불"), PaperGrid.nameRows("잿빛 새벽의 등불", 7))
        assertEquals(listOf("가나다라마바사아", "자차카타파하가…"), PaperGrid.nameRows("가나다라마바사아자차카타파하가나다", 8))
        assertEquals(listOf("4", "8", ",", "2", "1", "0", "자"), PaperGrid.countGlyphs(48210, 9))
        assertNull(PaperGrid.countGlyphs(1_234_567, 5))
        val cells = ProgressCells("에피소드", listOf(List(6) { CellState.DONE } + CellState.CURRENT + List(13) { CellState.PLANNED }))
        val rowCells = PaperGrid.progressRow(cells, 9)
        assertEquals(9, rowCells.size)
        assertEquals(3, rowCells.count { it == CellState.DONE })
        assertEquals(CellState.CURRENT, rowCells[3])
        assertEquals("에피소드 6 / 20", cells.line)
        assertEquals(30, cells.percent)
    }

    // ------------------------------------------------------------ picker

    @Test
    fun `the picker lists new, 3D and 목록 in groups, and applies only a change`() {
        val e = ViewPick.entries()
        assertEquals(FlatViews.styles + ShelfScene.styles + LibraryStyle.LIST, e.map { it.style })
        assertEquals(listOf(PickGroup.NEW, PickGroup.THREE_D, PickGroup.BASIC), e.map { it.group }.distinct())
        assertEquals(e.indexOfFirst { it.style == LibraryStyle.POSTER }, ViewPick.currentIndex(e, LibraryStyle.POSTER))
        // A stored style that draws nothing reads as 목록 (what the 서재 then shows).
        val few = ViewPick.entries(available = setOf(LibraryStyle.TABLE))
        assertEquals(listOf(LibraryStyle.TABLE, LibraryStyle.LIST), few.map { it.style })
        assertEquals(1, ViewPick.currentIndex(few, LibraryStyle.BOOKSHELF))
        assertFalse(ViewPick.canApply(3, 3))
        assertTrue(ViewPick.canApply(2, 3))
        assertEquals(0, ViewPick.wrap(14, 14))
        assertEquals(13, ViewPick.wrap(-1, 14))
        assertEquals(-1, ViewPick.distance(13, 0, 14))
        assertEquals(1, ViewPick.afterSwipe(0, -80f, 14))
        assertEquals(13, ViewPick.afterSwipe(0, 80f, 14))
        assertEquals(0, ViewPick.afterSwipe(0, 30f, 14))
        e.forEach { assertTrue(it.glyph.startsWith("M")) }
    }

    @Test
    fun `flat and picker copy pass the lint`() {
        val values = FlatViewCopy::class.java.declaredFields
            .filter { java.lang.reflect.Modifier.isStatic(it.modifiers) && it.type == String::class.java }
            .map { it.isAccessible = true; it.get(null) as String }
        val built = FlatViewCopy.SEGMENTS + ViewPick.entries().flatMap { listOf(it.label, it.frag) } +
            listOf(FlatViewCopy.now("표지 벽"), FlatViewCopy.viewButton("표"), FlatViewCopy.sortBy(FlatViewCopy.NAME), FlatViewCopy.noProgress, FlatViewCopy.progress("씬", 3, 6), FlatViewCopy.chars(48210))
        val bad = (values + built).mapNotNull { t -> CopyLint.lint(t).takeIf { it.isNotEmpty() }?.let { "$t: $it" } }
        assertEquals(emptyList<String>(), bad)
    }

    // ------------------------------------------------------------ the 3D scene under the bars (0.2.2 smoke gaps)

    @Test
    fun `the scene runs on to the viewport foot under the bars`() {
        for ((screen, top, bottom) in listOf(Triple(751f, 112f, 76f), Triple(933f, 112f, 76f), Triple(560f, 112f, 76f))) {
            val fit = ShelfScene.stageFit(screen, top, bottom, 160f)
            val run = ShelfScene.runHeight(screen, fit)
            assertEquals("no plain band at the foot ($screen)", screen, fit.top + run, .01f)
            val k = ShelfScene.stageScale(fit.height)
            assertTrue(ShelfScene.frameH(run, k) * k >= run - .01f)
            assertTrue(ShelfScene.frameH(run, k) >= ShelfScene.STAGE_H)
        }
    }

    @Test
    fun `원고 더미 and 서고 회랑 melt into bg at every edge`() {
        assertEquals(ColorPalettes.dark.getValue(ColorRole.BG).toColor(), LookB.dark.wall)
        assertEquals(ColorPalettes.light.getValue(ColorRole.BG).toColor(), LookB.light.wall)
        assertTrue(ShelfScene.sideFade(LibraryStyle.MANUSCRIPT_STACK, 400f) > 0f)
        assertTrue(ShelfScene.sideFade(LibraryStyle.ARCHIVE_HALL, 400f) > 0f)
        assertEquals(0f, ShelfScene.sideFade(LibraryStyle.BOOKSHELF, 400f), 0f)
        assertTrue(ShelfScene.sideFade(LibraryStyle.ARCHIVE_HALL, 60f) <= 10f)
    }

    @Test
    fun `별자리 plus stays above the dock`() {
        // The slot's foot is inside the board's stage, above the band the dock overlaps.
        assertTrue(StarsGeo.newSlotBottom <= ShelfScene.STAGE_H - ShelfScene.DOCK_OVERLAP)
        // Clear of 설정만's orbit (centre 452, amp 10, book 156 × .5).
        assertTrue(StarsGeo.NY - 32f >= 452f + 10f + 156f * .5f / 2f)
    }
}
