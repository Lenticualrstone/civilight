@file:JvmName("CanvasEntries")

package io.github.lenticualrstone.civilight.feature.canvas

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.feature.canvas.board.CanvasBoardScreen
import io.github.lenticualrstone.civilight.feature.canvas.label.LineLabelSheetContent
import io.github.lenticualrstone.civilight.feature.canvas.label.LineStartSheet
import io.github.lenticualrstone.civilight.feature.canvas.label.LineStartSheetContent
import io.github.lenticualrstone.civilight.navigation.CanvasBoard
import io.github.lenticualrstone.civilight.navigation.LineLabelSheet
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps

/**
 * Entries of `:feature:canvas` (shell.md §1). Routes: the `canvas` row of shell.md §3; shared sheets:
 * those owned by CANVAS in shell.md §5. Only registrations happen here.
 *
 * Registered so far (batch 1): route `CanvasBoard`; shared sheet `LineLabelSheet`; the feature-local
 * `LineStartSheet` (stacked over `LineLabelSheet`, so it must work over any area's screen). Not yet
 * (later batches; the shell shows its placeholder): `PageList`, `PageDoc`, `ImageCrop`, `ImageSpecs`,
 * `PresetImages`, `HoloExperiment`, and the shared `ImagePickSheet`.
 */
fun EntryProviderScope<NavKey>.canvasEntries(deps: FeatureDeps) {
    entry<CanvasBoard> { route -> CanvasBoardScreen(route, deps) }

    deps.sheets.register<LineLabelSheet> { sheet -> LineLabelSheetContent(sheet) }
    deps.sheets.register<LineStartSheet> { sheet -> LineStartSheetContent(sheet) }
}
