package io.github.lenticualrstone.civilight.feature.map

import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonLint
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec
import io.github.lenticualrstone.civilight.designsystem.icon.IconClass
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.feature.map.common.MapCreation
import io.github.lenticualrstone.civilight.feature.map.common.MapNav
import io.github.lenticualrstone.civilight.feature.map.common.MapOffers
import io.github.lenticualrstone.civilight.feature.map.common.MapScreenViewModel
import io.github.lenticualrstone.civilight.feature.map.edit.EditTool
import io.github.lenticualrstone.civilight.feature.map.view.MapViewState
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.MapBaseKind
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.navigation.MapView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** WLD-T85 (state model): 0.2.0 maps are one image plus pins; 0.1.0 maps stay listed and open view-only (D11). */
class Map020Test {
    private val doc = MapFx.doc()

    /** The example's maps plus one image map. */
    private fun withImageMap(): Pair<ProjectDoc, String> {
        val made = MapCreation.imageMap(doc, MapFx.blob(), MapFx.ctx())!!
        return made.doc to made.mapKey
    }

    @Test
    fun `WLD-T85 only image maps are editable`() {
        val (d, image) = withImageMap()
        assertTrue(MapOffers.editable(d.maps.getValue(image)))
        for (key in listOf(K.MAP_WORLD, K.MAP_LOWER_DISTRICT, K.MAP_ASH_MARKET)) {
            val map = d.maps.getValue(key)
            assertFalse(map.name, MapOffers.canEdit(map))
            assertFalse(map.name, MapOffers.canReplaceBase(map))
            assertFalse(map.name, MapOffers.canMovePin(map))
        }
        val sketch = d.maps.getValue(image).copy(baseKind = MapBaseKind.SKETCH.open())
        assertFalse(MapOffers.canEdit(sketch))
        assertFalse(MapOffers.canReplaceBase(sketch))
    }

    @Test
    fun `WLD-T85 0_1_0 maps open view-only in 지도 보기, the image map editable, none with a scale bar`() {
        val (d, image) = withImageMap()
        for (key in listOf(K.MAP_WORLD, K.MAP_LOWER_DISTRICT, K.MAP_ASH_MARKET)) {
            assertEquals(MapView(PROJECT, key), MapNav.routeOf(d, PROJECT, key))
            val ui = MapViewState.build(d, key, LensPoint.Latest, false, null, null, null)!!
            assertFalse(ui.canEdit)
            assertFalse(ui.canReplaceBase)
            assertFalse(ui.canMovePin)
            assertNull(ui.styleChip)
            assertNull(ui.scale)
            assertTrue(ui.scene.routes.isEmpty())
        }
        val ui = MapViewState.build(d, image, LensPoint.Latest, false, null, null, null)!!
        assertTrue(ui.canEdit)
        assertTrue(ui.canReplaceBase)
        assertTrue(ui.canMovePin)
        assertEquals(listOf("이동", "핀", "영역", "라벨", "레이어"), EditTool.BASE.map { it.label })
    }

    @Test
    fun `D11 a 0_1_0 map opened view-only still draws its base and overlays`() {
        val node = doc.maps.getValue(K.MAP_ASH_MARKET)
        assertEquals(MapBaseKind.NODE, node.baseKind.known)
        val nodeUi = MapViewState.build(doc, K.MAP_ASH_MARKET, LensPoint.Latest, false, null, null, null)!!
        assertEquals(node.nodes.size, nodeUi.scene.nodes.size)
        assertTrue(nodeUi.scene.nodes.isNotEmpty())
        assertEquals(node.nodeLinks.size, nodeUi.scene.links.size)
        for (key in listOf(K.MAP_WORLD, K.MAP_LOWER_DISTRICT)) {
            val map = doc.maps.getValue(key)
            assertEquals(MapBaseKind.GENERATED, map.baseKind.known)
            val ui = MapViewState.build(doc, key, LensPoint.Latest, false, null, null, null)!!
            assertNotNull(map.name, ui.base)
            assertTrue(map.name, ui.scene.pins.isNotEmpty() || ui.scene.regions.isNotEmpty() || ui.scene.labels.isNotEmpty())
        }
    }

    @Test
    fun `WLD-T85 every 0_1_0 map stays listed and keeps its data`() {
        val list = io.github.lenticualrstone.civilight.feature.map.list.MapListState.build(doc, LensPoint.Latest, false)
        assertEquals(3, list.cards.size)
        val lower = doc.maps.getValue(K.MAP_LOWER_DISTRICT)
        assertTrue(lower.routes.isNotEmpty())
        assertNotNull(lower.scale)
    }

    @Test
    fun `WLD-75 a new image map takes the smallest free 지도 N`() {
        val (d, image) = withImageMap()
        assertEquals("지도 1", d.maps.getValue(image).name)
        val second = MapCreation.imageMap(d, MapFx.blob(hash = "b".repeat(64)), MapFx.ctx(8))!!
        assertEquals("지도 2", second.doc.maps.getValue(second.mapKey).name)
        assertTrue(MapOffers.NEW_MAP_IMAGE_ONLY)
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

    private companion object {
        const val PROJECT = "P"
    }
}
