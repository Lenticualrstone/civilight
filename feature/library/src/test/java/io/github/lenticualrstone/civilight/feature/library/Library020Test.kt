package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.library.export.ExportPlanner
import io.github.lenticualrstone.civilight.feature.library.export.ExportFormRules
import io.github.lenticualrstone.civilight.feature.library.export.ExportReady
import io.github.lenticualrstone.civilight.feature.library.export.ExportRunner
import io.github.lenticualrstone.civilight.feature.library.projectsettings.ProjectSettingsCopy
import io.github.lenticualrstone.civilight.feature.library.projectsettings.ProjectSettingsRows
import io.github.lenticualrstone.civilight.feature.library.projectsettings.ProjectSettingsState
import io.github.lenticualrstone.civilight.feature.library.projectsettings.ScriptSettingsEdit
import io.github.lenticualrstone.civilight.feature.library.projectsettings.ScriptSwitch
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsCopy
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsState
import io.github.lenticualrstone.civilight.feature.library.settings.CardViewMirror
import io.github.lenticualrstone.civilight.feature.library.settings.ViewKind
import io.github.lenticualrstone.civilight.feature.library.settings.ViewStyles
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfUi
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfViewRenderers
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfViews
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.CardStyle
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.app.LibraryStyle
import io.github.lenticualrstone.civilight.model.app.ViewMode
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.version.VersionOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 0.2.0 rules of spec 60 §8.3 on the feature side (LIB-91, LIB-93, LIB-94, LIB-97, WLD-73, N-29). */
class Library020Test {
    private fun gameTools(doc: ProjectDoc, on: Boolean) = doc.copy(project = doc.project.copy(gameTools = on))

    @Test
    fun `G-08 스크립트 추가 칸 follows 게임 제작 도구`() {
        val off = ProjectSettingsRows.script(gameTools(LibFx.ashen(), false))
        assertEquals(listOf(ProjectSettingsCopy.GAME_TOOLS), off.switches.map { it.title })
        assertEquals(listOf(ProjectSettingsCopy.TRANSITION), off.links.map { it.title })
        assertFalse(off.switches.single().on)
        val on = ProjectSettingsRows.script(gameTools(LibFx.ashen(), true))
        assertEquals(listOf("게임 제작 도구", "무대 위치", "보이스 ID"), on.switches.map { it.title })
        assertEquals(listOf("ID 형식", "전환 문구"), on.links.map { it.title })
        assertTrue(on.switches.first().on)
    }

    @Test
    fun `G-01 LIB-91 the switch writes only the switch, and turning it off deletes nothing`() {
        val ashen = gameTools(LibFx.ashen(), true)
        val ctx = LibFx.ctx()
        val off = ScriptSettingsEdit.set(ashen, ScriptSwitch.GAME_TOOLS, false, ctx)
        assertEquals(false, off.project.gameTools)
        // Game data is untouched: blocks (voice IDs, cues), assets and the script's own switches.
        assertEquals(ashen.episodes, off.episodes)
        assertEquals(ashen.assets, off.assets)
        assertEquals(ashen.project.script, off.project.script)
        assertEquals(true, ScriptSettingsEdit.set(off, ScriptSwitch.GAME_TOOLS, true, ctx).project.gameTools)
        // Same value: the same document.
        assertTrue(ScriptSettingsEdit.set(off, ScriptSwitch.GAME_TOOLS, false, ctx) === off)
        // A new project has it off.
        assertEquals(false, LibFx.created(io.github.lenticualrstone.civilight.feature.library.newproject.NewProjectForm(name = "새 글")).project.gameTools)
        assertEquals(ProjectSettingsCopy.GAME_TOOLS, ScriptSettingsEdit.label(ScriptSwitch.GAME_TOOLS))
    }

    @Test
    fun `WLD-73 용어 후보 switch in section 세계, default on`() {
        val ashen = LibFx.ashen()
        val ui = ProjectSettingsState.build(ashen, readOnly = false)
        assertEquals(listOf(ProjectSettingsCopy.TERM_CANDIDATES), ui.world.map { it.title })
        assertTrue(ui.world.single().on)
        assertNull("no sub until TODO_COPY_WORLD_CANDIDATES_SUB has copy", ui.world.single().sub)
        val off = ScriptSettingsEdit.set(ashen, ScriptSwitch.TERM_CANDIDATES, false, LibFx.ctx())
        assertFalse(off.project.termCandidates)
        assertFalse(ProjectSettingsRows.world(off).single().on)
        // Stored `잇기 · 아님` decisions are kept.
        assertEquals(ashen.world, off.world)
    }

    @Test
    fun `V-01 V-02 V-03 V-08 the 서재 view and its head toggle`() {
        val all = LibraryStyle.entries.toSet()
        // V-01: BOOKSHELF with no stored toggle shows the 3D view and draws the toggle (once it has a renderer).
        val v1 = ShelfViews.of(AppSettings(), available = all)
        assertEquals(LibraryStyle.BOOKSHELF, v1.shown)
        assertEquals(LibraryStyle.BOOKSHELF, v1.rendered)
        assertEquals(SettingsCopy.LIB_BOOKSHELF, v1.toggle!!.styleLabel)
        assertEquals(SettingsCopy.VIEW_LIST, v1.toggle!!.listLabel)
        assertTrue(v1.toggle!!.styleOn)
        // V-02: the toggle on `목록` is remembered in `libraryToggle`.
        val v2 = ShelfViews.of(AppSettings(libraryToggle = ViewMode.LIST), available = all)
        assertEquals(LibraryStyle.LIST, v2.shown)
        assertNull(v2.rendered)
        assertFalse(v2.toggle!!.styleOn)
        // V-03: style `목록` draws no toggle.
        assertNull(ShelfViews.of(AppSettings(libraryStyle = LibraryStyle.LIST), available = all).toggle)
        // V-08: the 0.1.0 `viewByScreen["library"]` is not read.
        val v8 = ShelfViews.of(AppSettings(viewByScreen = mapOf("library" to ViewMode.LIST)), available = all)
        assertEquals(LibraryStyle.BOOKSHELF, v8.shown)
    }

    @Test
    fun `LIB-93 without a renderer every style draws the list and no toggle is drawn`() {
        assertEquals(emptySet<LibraryStyle>(), ShelfViewRenderers.available)
        for (style in LibraryStyle.entries) {
            val view = ShelfViews.of(AppSettings(libraryStyle = style))
            assertNull("$style draws the list", view.rendered)
            assertNull("$style: no control that does nothing", view.toggle)
        }
        // V-05: with 0 projects no 3D view is drawn even when one exists.
        val v = ShelfViews.of(AppSettings(), available = setOf(LibraryStyle.BOOKSHELF))
        assertFalse(ShelfViews.drawsView(v, ShelfUi(loaded = true, empty = io.github.lenticualrstone.civilight.domain.library.newproject.EmptyLibrary(showDeletedNotice = false))))
        assertTrue(ShelfViews.drawsView(v, ShelfUi(loaded = true)))
    }

    @Test
    fun `LIB-94 설정 rows and the two pickers`() {
        val libAll = LibraryStyle.entries.toSet()
        val cardAll = CardStyle.entries.toSet()
        val ui = SettingsState.root(
            AppSettings(libraryStyle = LibraryStyle.COVER_SPIN, castCardStyle = CardStyle.ROLODEX, viewTilt = false),
            0, false, null, LibFx.NOW, LibFx.ZONE, libraryViews = libAll, cardViews = cardAll,
        )
        assertEquals("서재 보기", ui.libraryView!!.title)
        assertEquals("표지 회전", ui.libraryView!!.value)
        assertNull(ui.libraryView!!.sub)
        assertEquals("카드 보기", ui.cardView!!.title)
        assertEquals("롤로덱스", ui.cardView!!.value)
        assertEquals(SettingsCopy.VIEW_TILT, ui.tilt!!.title)
        assertFalse(ui.tilt!!.on)
        assertTrue(SettingsState.root(AppSettings(), 0, false, null, LibFx.NOW, LibFx.ZONE, libraryViews = libAll, cardViews = cardAll).tilt!!.on)

        val lib = ViewStyles.options(ViewKind.LIBRARY, AppSettings(), library = libAll)
        assertEquals(listOf("3D 책장", "표지 회전", "원고 더미", "서고 회랑", "색인 카드 서랍", "별자리", "목록"), lib.map { it.title })
        assertEquals(listOf(true, false, false, false, false, false, false), lib.map { it.selected })
        val cards = ViewStyles.options(ViewKind.CARD, AppSettings(), cards = cardAll)
        assertEquals(listOf("부채꼴 덱", "회전 목마", "겹친 더미", "타로 펼침", "기울어진 벽", "롤로덱스", "목록"), cards.map { it.title })
        assertEquals(LibraryStyle.ARCHIVE_HALL, ViewStyles.libraryStyleOf(LibraryStyle.ARCHIVE_HALL.name))
        assertEquals(CardStyle.TAROT, ViewStyles.cardStyleOf(CardStyle.TAROT.name))
        assertNull(ViewStyles.libraryStyleOf("unknown"))
    }

    @Test
    fun `LIB-94 no view rows while no 3D view has a renderer`() {
        // Tripwire with the ShelfViews one: wave 3 flips both renderer sets (and CardViewMirror with CastCardViews).
        assertEquals(emptySet<CardStyle>(), CardViewMirror.available)
        val ui = SettingsState.root(AppSettings(libraryStyle = LibraryStyle.BOOKSHELF, castCardStyle = CardStyle.FAN_DECK), 0, false, null, LibFx.NOW, LibFx.ZONE)
        assertNull("서재 보기 does nothing without a renderer", ui.libraryView)
        assertNull("카드 보기 does nothing without a renderer", ui.cardView)
        assertNull("tilt does nothing without a renderer", ui.tilt)

        // One kind drawable: only its row, plus tilt; the picker offers 목록 and the drawable styles only.
        val some = SettingsState.root(AppSettings(libraryStyle = LibraryStyle.ARCHIVE_HALL), 0, false, null, LibFx.NOW, LibFx.ZONE, libraryViews = setOf(LibraryStyle.BOOKSHELF), cardViews = emptySet())
        assertEquals("목록", some.libraryView!!.value)
        assertNull(some.cardView)
        assertNotNull(some.tilt)
        val lib = ViewStyles.options(ViewKind.LIBRARY, AppSettings(libraryStyle = LibraryStyle.ARCHIVE_HALL), library = setOf(LibraryStyle.BOOKSHELF))
        assertEquals(listOf("3D 책장", "목록"), lib.map { it.title })
        assertEquals(listOf(false, true), lib.map { it.selected })
        assertEquals(listOf("목록"), ViewStyles.options(ViewKind.CARD, AppSettings(), cards = emptySet()).map { it.title })
    }

    @Test
    fun `LIB-91 a remembered CSV draft falls back while 게임 제작 도구 is off`() {
        val on = gameTools(LibFx.ashen(), true)
        val off = gameTools(LibFx.ashen(), false)
        val csv = ExportFormRules.selectFormat(ExportFormRules.initial(on, LibFx.NOW, LibFx.ZONE), on, ExportFormat.CSV, LibFx.NOW, LibFx.ZONE)
        assertEquals(ExportFormat.CSV, csv.format)
        assertEquals(ExportFormat.CSV, ExportFormRules.visible(csv, on, LibFx.NOW, LibFx.ZONE).format)
        assertEquals(ExportFormat.PDF, ExportFormRules.visible(csv, off, LibFx.NOW, LibFx.ZONE).format)
        assertEquals(ExportFormat.PDF, ExportFormRules.initial(off, LibFx.NOW, LibFx.ZONE, ExportFormat.ENGINE_JSON).format)
        // `.civilight` stays as it is.
        val file = ExportFormRules.initial(off, LibFx.NOW, LibFx.ZONE, ExportFormat.PROJECT_FILE)
        assertEquals(ExportFormat.PROJECT_FILE, file.format)
        assertTrue(file.options.includeVersions)
        assertFalse(ExportFormRules.setIncludeVersions(file, false).options.includeVersions)
    }

    @Test
    fun `N-29 D21 a saved export adds one 내보낸 판 per exported episode, a civilight none`() {
        val doc = LibFx.ashen()
        val options = ExportFormRules.runOptions(ExportFormRules.initial(doc, LibFx.NOW, LibFx.ZONE))
        val plan = ExportPlanner.plan(doc, options)
        val units = plan.scenes.mapNotNull { doc.index.numberingUnitKeyOfScene(it.sceneKey) }.distinct()
        assertTrue(units.isNotEmpty())
        val ctx = ActionContext(LibFx.NOW, LibFx.ZONE)
        // PDF: no voice-ID fixation plan, only the version plan.
        val pdf = ExportRunner.afterSave(doc, ExportReady("a.pdf", "application/pdf", ByteArray(0), null, versionPlan = plan), ctx)
        assertNotNull(pdf)
        val added = units.sumOf { pdf!!.versions[it]?.versions.orEmpty().size } - units.sumOf { doc.versions[it]?.versions.orEmpty().size }
        assertEquals(units.size, added)
        assertTrue(units.all { u -> pdf!!.versions[u]!!.versions.last().origin.known == VersionOrigin.EXPORT })
        assertTrue(units.all { u -> pdf!!.versions[u]!!.versions.last().name.endsWith("내보낸 판") })
        // Blocks are untouched (◆, numbers and voice IDs are not touched).
        assertEquals(doc.episodes, pdf!!.episodes)
        // `.civilight`: nothing to write back.
        assertNull(ExportRunner.afterSave(doc, ExportReady("a.civilight", "application/zip", ByteArray(0), null), ctx))
    }

    @Test
    fun `N-29 an undo step gets the exported version as exported, never its own text`() {
        val doc = LibFx.ashen()
        val options = ExportFormRules.runOptions(ExportFormRules.initial(doc, LibFx.NOW, LibFx.ZONE))
        val plan = ExportPlanner.plan(doc, options)
        val units = plan.scenes.mapNotNull { doc.index.numberingUnitKeyOfScene(it.sceneKey) }.distinct()
        val ctx = ActionContext(LibFx.NOW, LibFx.ZONE)
        val after = ExportRunner.afterSave(doc, ExportReady("a.pdf", "application/pdf", ByteArray(0), null, versionPlan = plan), ctx)!!
        // An older undo step: one exported block had other text then.
        val blockKey = after.versions[units.first()]!!.versions.last().blocks.first().key
        val older = doc.copy(
            episodes = doc.episodes.mapValues { (_, f) ->
                f.copy(blocks = f.blocks.map { if (it.key == blockKey) it.copy(runs = listOf(io.github.lenticualrstone.civilight.model.text.TextRun.Plain("옛 글"))) else it })
            },
        )
        val carried = ExportRunner.carryAfterSave(older, doc, after)
        units.forEach { u -> assertEquals(after.versions[u]!!.versions.last(), carried.versions[u]!!.versions.last()) }
        // The step keeps its own text; only the new version rows came over.
        assertEquals(older.episodes, carried.episodes)
        // Carrying twice adds nothing more.
        assertEquals(carried, ExportRunner.carryAfterSave(carried, doc, after))
    }
}
