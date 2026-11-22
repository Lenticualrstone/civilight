@file:JvmName("CanvasEntries")

package io.github.lenticualrstone.civilight.feature.canvas

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.feature.canvas.board.CanvasBoardScreen
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
 * `PresetImages` and `HoloExperiment` (batch 3). Shared sheets: `LineLabelSheet` and `ImagePickSheet`;
 * the feature-local `LineStartSheet` is registered because it stacks over `LineLabelSheet` on any area's
 * screen.
 */
fun EntryProviderScope<NavKey>.canvasEntries(deps: FeatureDeps) {
    entry<CanvasBoard> { route -> CanvasBoardScreen(route, deps) }
    entry<PageList> { route -> PageListScreen(route, deps) }
    entry<PageDoc> { route -> PageDocScreen(route, deps) }
    entry<ImageCrop> { route -> ImageCropScreen(route, deps) }
    entry<ImageSpecs> { ImageSpecsScreen(deps) }
    entry<PresetImages> { PresetImagesScreen(deps) }
    entry<HoloExperiment> { HoloExperimentScreen(deps) }

    deps.sheets.register<LineLabelSheet> { sheet -> LineLabelSheetContent(sheet) }
    deps.sheets.register<LineStartSheet> { sheet -> LineStartSheetContent(sheet) }
    deps.sheets.register<ImagePickSheet> { sheet -> ImagePickSheetContent(sheet) }
}
