package io.github.lenticualrstone.civilight.feature.canvas

import androidx.compose.ui.unit.Constraints
import io.github.lenticualrstone.civilight.feature.canvas.board.CanvasTool
import io.github.lenticualrstone.civilight.feature.canvas.board.PlaceItemSheet
import io.github.lenticualrstone.civilight.feature.canvas.label.LineStartSheet
import io.github.lenticualrstone.civilight.feature.canvas.node.NodeEditSheet
import io.github.lenticualrstone.civilight.navigation.LineLabelSheet
import io.github.lenticualrstone.civilight.navigation.RouteCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Open sheets are saved with the screen through `RouteCodec` (CNV-35); every canvas sheet key must round-trip. */
class SheetKeysTest {
    @Test
    fun `canvas sheet keys survive the saved sheet stack`() {
        val p = K.PROJECT
        val keys = listOf(
            NodeEditSheet(p, CanvasFx.CANVAS, CanvasFx.NODE_RIEN),
            PlaceItemSheet(p, CanvasFx.CANVAS, CanvasTool.NODE, 120.5f, -40f),
            PlaceItemSheet(p, CanvasFx.CANVAS, CanvasTool.CHARACTER, 0f, 0f),
            LineStartSheet(p, CanvasFx.CANVAS, CanvasFx.EDGE_RIEN_HART),
            LineLabelSheet(p, CanvasFx.CANVAS, CanvasFx.EDGE_RIEN_HART),
        )
        for (key in keys) assertEquals(key, RouteCodec.decode(RouteCodec.encode(key)))
        assertEquals(keys, RouteCodec.decodeAll(RouteCodec.encodeAll(keys)))
    }

    @Test
    fun `nodes and labels are measured without a size limit`() {
        // GraphSurface places children outside the parent's bounds; only Infinity is a valid unbounded max.
        val unbounded = Constraints(maxWidth = Constraints.Infinity, maxHeight = Constraints.Infinity)
        assertFalse(unbounded.hasBoundedWidth)
        assertFalse(unbounded.hasBoundedHeight)
    }
}
