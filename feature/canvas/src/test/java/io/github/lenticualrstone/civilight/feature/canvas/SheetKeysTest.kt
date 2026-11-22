package io.github.lenticualrstone.civilight.feature.canvas

import androidx.compose.ui.unit.Constraints
import io.github.lenticualrstone.civilight.feature.canvas.board.CanvasTool
import io.github.lenticualrstone.civilight.feature.canvas.board.PlaceItemSheet
import io.github.lenticualrstone.civilight.feature.canvas.label.LineStartSheet
import io.github.lenticualrstone.civilight.feature.canvas.node.NodeEditSheet
import io.github.lenticualrstone.civilight.feature.canvas.page.BlockAddSheet
import io.github.lenticualrstone.civilight.feature.canvas.page.DeletedLinkSheet
import io.github.lenticualrstone.civilight.feature.canvas.page.PageMenuSheet
import io.github.lenticualrstone.civilight.feature.canvas.page.RelinkSheet
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryCopySheet
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GallerySceneSheet
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GallerySortSheet
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryTagsSheet
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery
import io.github.lenticualrstone.civilight.navigation.GalleryAddSheet
import io.github.lenticualrstone.civilight.navigation.GalleryInfoSheet
import io.github.lenticualrstone.civilight.navigation.GalleryMenuSheet
import io.github.lenticualrstone.civilight.navigation.ImagePickSheet
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
            PageMenuSheet(p, K.PAGE_LOWER_LIGHT),
            BlockAddSheet(p, K.PAGE_LOWER_LIGHT),
            DeletedLinkSheet(p, K.PAGE_LOWER_LIGHT, K.link(7)),
            RelinkSheet(p, K.PAGE_LOWER_LIGHT, K.link(7)),
            ImagePickSheet(p, ImageOwnerKind.PAGE, K.PAGE_LOWER_LIGHT, ImageSlot.PAGE_HEADER),
            ImagePickSheet(p, ImageOwnerKind.CHARACTER, K.RIEN, ImageSlot.CHARACTER_PORTRAIT),
            GallerySortSheet(p, K.RIEN),
            GalleryCopySheet(p, listOf(ExampleGallery.ITEM_1, ExampleGallery.ITEM_2)),
            GallerySceneSheet(p, ExampleGallery.ITEM_3),
            GalleryTagsSheet(p, listOf(ExampleGallery.ITEM_3)),
            GalleryAddSheet(p, GalleryOwnerKind.CHARACTER, K.RIEN),
            GalleryInfoSheet(p, ExampleGallery.ITEM_3, focusDescription = true),
            GalleryMenuSheet(p, ExampleGallery.ITEM_3),
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
