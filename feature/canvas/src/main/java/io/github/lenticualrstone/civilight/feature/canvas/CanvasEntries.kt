@file:JvmName("CanvasEntries")

package io.github.lenticualrstone.civilight.feature.canvas

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.feature.canvas.board.CanvasBoardScreen
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryAddSheetContent
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryCopySheet
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryCopySheetContent
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryInfoSheetContent
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryMenuSheetContent
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GallerySceneSheet
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GallerySceneSheetContent
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryScreen
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryTagsSheet
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryTagsSheetContent
import io.github.lenticualrstone.civilight.feature.canvas.label.LineLabelSheetContent
import io.github.lenticualrstone.civilight.feature.canvas.label.LineStartSheet
import io.github.lenticualrstone.civilight.feature.canvas.label.LineStartSheetContent
import io.github.lenticualrstone.civilight.feature.canvas.page.PageDocScreen
import io.github.lenticualrstone.civilight.feature.canvas.page.PageListScreen
import io.github.lenticualrstone.civilight.feature.canvas.holo.HoloExperimentScreen
import io.github.lenticualrstone.civilight.feature.canvas.image.crop.ImageCropScreen
import io.github.lenticualrstone.civilight.feature.canvas.image.pick.ImagePickSheetContent
import io.github.lenticualrstone.civilight.feature.canvas.image.specs.ImageSpecsScreen
import io.github.lenticualrstone.civilight.feature.canvas.image.specs.PresetImagesScreen
import io.github.lenticualrstone.civilight.navigation.CanvasBoard
import io.github.lenticualrstone.civilight.navigation.Gallery
import io.github.lenticualrstone.civilight.navigation.GalleryAddSheet
import io.github.lenticualrstone.civilight.navigation.GalleryInfoSheet
import io.github.lenticualrstone.civilight.navigation.GalleryMenuSheet
import io.github.lenticualrstone.civilight.navigation.HoloExperiment
import io.github.lenticualrstone.civilight.navigation.ImageCrop
import io.github.lenticualrstone.civilight.navigation.ImagePickSheet
import io.github.lenticualrstone.civilight.navigation.ImageSpecs
import io.github.lenticualrstone.civilight.navigation.PresetImages
import io.github.lenticualrstone.civilight.navigation.PageDoc
import io.github.lenticualrstone.civilight.navigation.PageList
import io.github.lenticualrstone.civilight.navigation.LineLabelSheet
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps

/**
 * Entries of `:feature:canvas` (shell.md §1). Routes: the `canvas` row of shell.md §3; shared sheets:
 * those owned by CANVAS in shell.md §5. Only registrations happen here.
 *
 * Routes: `CanvasBoard` (batch 1), `PageList` and `PageDoc` (batch 2), `ImageCrop`, `ImageSpecs`,
 * `PresetImages` and `HoloExperiment` (batch 3), `Gallery` (55 GAL-04 … GAL-09, 0.1.0 wave step 2). Shared sheets: `LineLabelSheet` and `ImagePickSheet`;
 * the feature-local `LineStartSheet` is registered because it stacks over `LineLabelSheet` on any area's
 * screen. The gallery's shared sheets `GalleryAddSheet` (GAL-11), `GalleryInfoSheet` (GAL-13) and `GalleryMenuSheet`
 * (GAL-14) (0.1.0 wave step 3), and their feature-local pickers (`GalleryCopySheet`, `GallerySceneSheet`,
 * `GalleryTagsSheet`), which stack over them on any area's screen.
 */
fun EntryProviderScope<NavKey>.canvasEntries(deps: FeatureDeps) {
    entry<CanvasBoard> { route -> CanvasBoardScreen(route, deps) }
    entry<PageList> { route -> PageListScreen(route, deps) }
    entry<PageDoc> { route -> PageDocScreen(route, deps) }
    entry<ImageCrop> { route -> ImageCropScreen(route, deps) }
    entry<ImageSpecs> { ImageSpecsScreen(deps) }
    entry<PresetImages> { PresetImagesScreen(deps) }
    entry<HoloExperiment> { HoloExperimentScreen(deps) }
    entry<Gallery> { route -> GalleryScreen(route, deps) }

    deps.sheets.register<LineLabelSheet> { sheet -> LineLabelSheetContent(sheet) }
    deps.sheets.register<LineStartSheet> { sheet -> LineStartSheetContent(sheet) }
    deps.sheets.register<ImagePickSheet> { sheet -> ImagePickSheetContent(sheet) }
    deps.sheets.register<GalleryAddSheet> { sheet -> GalleryAddSheetContent(sheet) }
    deps.sheets.register<GalleryInfoSheet> { sheet -> GalleryInfoSheetContent(sheet) }
    deps.sheets.register<GalleryMenuSheet> { sheet -> GalleryMenuSheetContent(sheet) }
    deps.sheets.register<GalleryCopySheet> { sheet -> GalleryCopySheetContent(sheet) }
    deps.sheets.register<GallerySceneSheet> { sheet -> GallerySceneSheetContent(sheet) }
    deps.sheets.register<GalleryTagsSheet> { sheet -> GalleryTagsSheetContent(sheet) }
}
