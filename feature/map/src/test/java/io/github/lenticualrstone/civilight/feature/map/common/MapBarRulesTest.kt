package io.github.lenticualrstone.civilight.feature.map.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Which floating bar each map screen shows (motion audit row 1): a change of kind animates, anything else updates in place. */
class MapBarRulesTest {
    @Test
    fun `map view shows the move bar first, then the cover place sheet, else the tools`() {
        assertEquals(MapBars.MOVE, MapBars.view(moving = true, place = true, inner = false))
        assertEquals(MapBars.PLACE, MapBars.view(moving = false, place = true, inner = false))
        assertEquals(MapBars.TOOLS, MapBars.view(moving = false, place = false, inner = false))
    }

    @Test
    fun `the inner map view keeps the tools under a selected place, which sits in the side card`() {
        assertEquals(MapBars.TOOLS, MapBars.view(moving = false, place = true, inner = true))
        assertEquals(MapBars.MOVE, MapBars.view(moving = true, place = true, inner = true))
    }

    @Test
    fun `the editor bar order is move, shape choice, layers, properties, stamps`() {
        assertEquals(MapBars.MOVE, MapBars.edit(true, true, true, true, true, inner = false))
        assertEquals(MapBars.SHAPE, MapBars.edit(false, true, true, true, true, inner = false))
        assertEquals(MapBars.LAYERS, MapBars.edit(false, false, true, true, true, inner = false))
        assertEquals(MapBars.PROPS, MapBars.edit(false, false, false, true, true, inner = false))
        assertEquals(MapBars.STAMPS, MapBars.edit(false, false, false, false, true, inner = false))
        assertNull(MapBars.edit(false, false, false, false, false, inner = false))
    }

    @Test
    fun `on the inner editor only the two step bars sit above the tool bar`() {
        assertEquals(MapBars.MOVE, MapBars.edit(true, false, true, true, true, inner = true))
        assertEquals(MapBars.SHAPE, MapBars.edit(false, true, true, true, true, inner = true))
        assertNull(MapBars.edit(false, false, true, true, true, inner = true))
    }

    @Test
    fun `the node map bar is the step, then on the cover the layers or the properties`() {
        assertEquals(MapBars.STEP, MapBars.node(step = true, layerTool = true, props = true, inner = false))
        assertEquals(MapBars.LAYERS, MapBars.node(false, true, true, inner = false))
        assertEquals(MapBars.PROPS, MapBars.node(false, false, true, inner = false))
        assertNull(MapBars.node(false, true, true, inner = true))
        assertNull(MapBars.node(false, false, false, inner = false))
    }

    @Test
    fun `stagger steps are the shared 60 ms`() {
        assertEquals(0, MapMotion.stagger(0))
        assertEquals(120, MapMotion.stagger(2))
        assertEquals(0, MapMotion.stagger(-3))
    }
}
