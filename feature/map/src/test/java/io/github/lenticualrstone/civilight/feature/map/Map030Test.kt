package io.github.lenticualrstone.civilight.feature.map

import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonLint
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec
import io.github.lenticualrstone.civilight.designsystem.icon.IconClass
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.feature.map.common.MapCreation
import io.github.lenticualrstone.civilight.feature.map.common.MapNav
import io.github.lenticualrstone.civilight.feature.map.common.MapOffers
import io.github.lenticualrstone.civilight.feature.map.common.MapScreenViewModel
import io.github.lenticualrstone.civilight.feature.map.common.RouteLines
import io.github.lenticualrstone.civilight.feature.map.edit.EditState
import io.github.lenticualrstone.civilight.feature.map.edit.EditTool
import io.github.lenticualrstone.civilight.feature.map.edit.EditorState
import io.github.lenticualrstone.civilight.feature.map.edit.MapTourSteps
import io.github.lenticualrstone.civilight.feature.map.list.NewMapOption
import io.github.lenticualrstone.civilight.feature.map.node.NodeEditorState
import io.github.lenticualrstone.civilight.feature.map.node.NodeMapState
import io.github.lenticualrstone.civilight.feature.map.view.MapViewState
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.map.MapBaseKind
import io.github.lenticualrstone.civilight.model.map.MapFormat
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.domain.library.onboarding.ModuleTours
import io.github.lenticualrstone.civilight.domain.library.onboarding.TourModule
import io.github.lenticualrstone.civilight.navigation.MapView
import io.github.lenticualrstone.civilight.navigation.NodeMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** (0.3.0) Map making is back: every base kind is made and edited; 0.1.x / 0.2.x maps load and edit as they are. */
class Map030Test {
    private val doc = MapFx.doc()

    private fun withImageMap(): Pair<ProjectDoc, String> {
        val made = MapCreation.imageMap(doc, MapFx.blob(), MapFx.ctx())!!
        return made.doc to made.mapKey
    }

    @Test
    fun `gates per base kind`() {
        val (d, image) = withImageMap()
        val img = d.maps.getValue(image)
        val generated = d.maps.getValue(K.MAP_LOWER_DISTRICT)
        val sketch = img.copy(baseKind = MapBaseKind.SKETCH.open())
        val node = d.maps.getValue(K.MAP_ASH_MARKET)
        assertEquals(MapBaseKind.GENERATED, generated.baseKind.known)
        assertEquals(MapBaseKind.NODE, node.baseKind.known)
        for (m in listOf(img, generated, sketch)) {
            assertTrue(m.name, MapOffers.canEdit(m))
            assertTrue(m.name, MapOffers.canMovePin(m))
            assertTrue(m.name, MapOffers.canDropPin(m))
        }
        assertFalse("a node map is edited in the node editor", MapOffers.canEdit(node))
        assertTrue(MapOffers.canReplaceBase(img))
        assertTrue(MapOffers.canReplaceBase(sketch))
        assertFalse(MapOffers.canReplaceBase(generated))
        assertFalse(MapOffers.canReplaceBase(node))
        // (0.3.0) Distances are back: scale on every map but a node map, 인물 동선 and the style chip.
        for (m in listOf(img, generated, sketch)) assertTrue(m.name, MapOffers.showsScale(m))
        assertFalse(MapOffers.showsScale(node))
        assertFalse(MapOffers.showsStyleChip(node))
        assertTrue(MapOffers.OFFERS_TRACK)
        assertTrue(MapOffers.OFFERS_TEXT_TERRAIN)
        assertTrue(MapOffers.DRAWS_ROUTES)
        assertTrue(MapOffers.OFFERS_GENERATION)
    }

    @Test
    fun `node maps open in the node editor, every other map in 지도 보기 with 편집`() {
        assertEquals(NodeMap(PROJECT, K.MAP_ASH_MARKET), MapNav.routeOf(doc, PROJECT, K.MAP_ASH_MARKET))
        for (key in listOf(K.MAP_WORLD, K.MAP_LOWER_DISTRICT)) {
            assertEquals(MapView(PROJECT, key), MapNav.routeOf(doc, PROJECT, key))
            val ui = MapViewState.build(doc, key, LensPoint.Latest, false, null, null, null)!!
            assertTrue(ui.canEdit)
            assertTrue(ui.canMovePin)
            assertFalse("generated maps have no picture to replace", ui.canReplaceBase)
        }
        val lower = MapViewState.build(doc, K.MAP_LOWER_DISTRICT, LensPoint.Latest, false, null, null, null)!!
        assertTrue("routes are drawn", lower.scene.routes.isNotEmpty())
        assertEquals("the 0.1.x scale is shown again", "1일 · 15 km", lower.scale)
    }

    @Test
    fun `the editor's bar is 이동 · 핀 · 영역 · 경로 · 라벨 · 레이어, and the tour points at 경로`() {
        assertEquals(listOf("이동", "핀", "영역", "경로", "라벨", "레이어"), EditTool.BASE.map { it.label })
        assertEquals(MapTourSteps.ROUTE, MapTourSteps.stepOfTool(EditTool.ROUTE))
        assertNull(MapTourSteps.stepOfTool(EditTool.PIN))
        val titles = ModuleTours.steps(TourModule.MAP).map { it.title }
        assertEquals(MapCopy.TOOL_ROUTE, titles[MapTourSteps.ROUTE])
        assertEquals(MapCopy.REVEAL_POINT, titles[MapTourSteps.REVEAL])
        assertTrue(TourModule.MAP in ModuleTours.offered)
    }

    @Test
    fun `the new-map sheet offers all four bases in board order`() {
        assertEquals(
            listOf("지형 만들기", "이미지 가져오기", "스케치 촬영", "노드 지도"),
            NewMapOption.entries.map { it.title },
        )
    }

    @Test
    fun `route sheet with a scale reads distance, duration and modes, without one 축척 없음 and 축척 맞추기`() {
        val lower = doc.maps.getValue(K.MAP_LOWER_DISTRICT)
        assertNotNull("the 0.1.x map keeps its scale", lower.scale)
        val route = lower.routes.first { it.key == K.ROUTE_GATE_TO_UPPER_GATE }
        val sheet = RouteLines.sheet(doc, lower, route, lower.pins.mapTo(HashSet()) { it.key })
        // Board `MapRouteC`: `결사의 문 → 상층 관문`, `38 km · 지점 3`, `2일 4시간`, `도보 15 km · 8시간 · 말 · 배 · 직접`.
        assertEquals("결사의 문 → 상층 관문", sheet.title)
        assertEquals("38 km · 지점 3", sheet.detail)
        assertEquals("2일 4시간", sheet.duration)
        assertNull(sheet.action)
        assertEquals("도보 15 km · 8시간", sheet.modeChips.first())
        assertEquals("직접", sheet.modeChips.last())
        assertEquals("결사의 문 → 상층 관문 · 도보 2일 4시간 · 38 km", sheet.mapLabel)
        assertEquals("지점 3 · 12 km", RouteLines.draftBar(lower, 3, "12"))
        // No scale: a defined row, never a blank stand-in.
        val noScale = lower.copy(scale = null)
        val d2 = doc.copy(maps = doc.maps + (noScale.key to noScale))
        val s2 = RouteLines.sheet(d2, noScale, route, noScale.pins.mapTo(HashSet()) { it.key })
        assertEquals("축척 없음", s2.detail)
        assertNull(s2.duration)
        assertEquals("축척 맞추기", s2.action)
        assertEquals(s2.title, s2.mapLabel)
        assertTrue(s2.modeChips.isNotEmpty())
        // A masked end reads `지점`, in the title and in the map chip.
        val masked = RouteLines.sheet(doc, lower, route, emptySet())
        assertEquals("지점 → 지점", masked.title)
        assertTrue(masked.mapLabel.startsWith("지점 → 지점"))
        assertFalse(masked.mapLabel.contains("결사의 문"))
    }

    @Test
    fun `0_1_x maps of every base kind and format load and open editable`() {
        val text = javaClass.classLoader!!.getResource("v010/map.json")!!.readText()
        val formats = listOf(null) + MapFormat.entries
        for (kind in MapBaseKind.entries) for (format in formats) {
            var t = text.replace("\"baseKind\": \"generated\"", "\"baseKind\": \"${kind.wire()}\"")
            t = if (format == null) t.replace("\"format\": \"hex\",", "") else t.replace("\"format\": \"hex\"", "\"format\": \"${format.wire()}\"")
            val map = CivilightJson.decode(WorldMap.serializer(), t)
            assertEquals(kind, map.baseKind.known)
            // The stored map writes back the same (nothing dropped).
            val again = CivilightJson.decode(WorldMap.serializer(), CivilightJson.encode(WorldMap.serializer(), map))
            assertEquals(CivilightJson.encodeElement(WorldMap.serializer(), map), CivilightJson.encodeElement(WorldMap.serializer(), again))
            val d = doc.copy(maps = doc.maps + (map.key to map))
            val label = "$kind/$format"
            if (io.github.lenticualrstone.civilight.domain.world.map.MapScales.isNodeMap(map)) {
                assertEquals(label, NodeMap(PROJECT, map.key), MapNav.routeOf(d, PROJECT, map.key))
                assertNotNull(label, NodeMapState.build(d, map.key, LensPoint.Latest, false, NodeEditorState()))
            } else {
                assertEquals(label, MapView(PROJECT, map.key), MapNav.routeOf(d, PROJECT, map.key))
                val view = MapViewState.build(d, map.key, LensPoint.Latest, false, null, null, null)
                assertNotNull(label, view)
                assertTrue(label, view!!.canEdit)
                assertNotNull(label, EditState.build(d, map.key, LensPoint.Latest, false, EditorState()))
            }
        }
    }

    @Test
    fun `the maps of a real 0_1_0 store open in their editors`() {
        val names = listOf("01JZ000000MAPS000000000001", "01JZ000000MAPS000000000002", "01JZ000000MAPS000000000003")
        for (name in names) {
            val text = javaClass.classLoader!!.getResource("v010/store-rich-maps/$name.json")!!.readText()
            val map = CivilightJson.decode(WorldMap.serializer(), text)
            val d = doc.copy(maps = doc.maps + (map.key to map))
            if (io.github.lenticualrstone.civilight.domain.world.map.MapScales.isNodeMap(map)) {
                assertEquals(name, NodeMap(PROJECT, map.key), MapNav.routeOf(d, PROJECT, map.key))
                assertNotNull(name, NodeMapState.build(d, map.key, LensPoint.Latest, false, NodeEditorState()))
            } else {
                val view = MapViewState.build(d, map.key, LensPoint.Latest, false, null, null, null)!!
                assertTrue(name, view.canEdit)
                val edit = EditState.build(d, map.key, LensPoint.Latest, false, EditorState())
                assertNotNull(name, edit)
                // Stored routes are drawn again.
                if (map.routes.any { it.points.size >= 2 }) assertTrue(name, view.scene.routes.isNotEmpty())
            }
        }
    }

    @Test
    fun `WLD-75 a new image map takes the smallest free 지도 N`() {
        val (d, image) = withImageMap()
        assertEquals("지도 1", d.maps.getValue(image).name)
        val second = MapCreation.imageMap(d, MapFx.blob(hash = "b".repeat(64)), MapFx.ctx(8))!!
        assertEquals("지도 2", second.doc.maps.getValue(second.mapKey).name)
    }

    @Test
    fun `S-1 map screens read at 최신 whatever lens the world head picked`() {
        val past = doc.copy(app = doc.app.copy(view = doc.app.view.copy(lens = doc.app.view.lens.copy(point = LensPoint.UnitEnd(K.EPISODE_1_5)))))
        assertEquals(LensPoint.Latest, MapScreenViewModel.lensOf(past))
    }

    @Test
    fun `WLD-76 the place panel's 배경 에셋 follows the game tools switch`() {
        assertTrue(MapViewState.build(doc, K.MAP_LOWER_DISTRICT, LensPoint.Latest, false, null, null, null)!!.showsAssets)
        val off = doc.copy(project = doc.project.copy(gameTools = false))
        assertFalse(MapViewState.build(off, K.MAP_LOWER_DISTRICT, LensPoint.Latest, false, null, null, null)!!.showsAssets)
    }

    @Test
    fun `ICON the editor's undo is class a with its name, and every icon-only button is named`() {
        assertEquals(IconClass.A, IconButtonSpec.MAP_UNDO.cls)
        assertEquals("되돌리기", IconButtonSpec.MAP_UNDO.accessibilityName)
        val root = listOf(File("src/main"), File("feature/map/src/main")).first { it.exists() }
        assertEquals("", IconButtonLint.report(IconButtonLint.lintDir(root)))
    }

    private fun MapBaseKind.wire(): String = name.lowercase()
    private fun MapFormat.wire(): String = name.lowercase()

    private companion object {
        const val PROJECT = "P"
    }
}
