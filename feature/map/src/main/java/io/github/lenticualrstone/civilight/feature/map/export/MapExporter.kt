package io.github.lenticualrstone.civilight.feature.map.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.designsystem.theme.ResolvedTheme
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBase
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScene
import io.github.lenticualrstone.civilight.model.app.LargeMapExport
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import kotlinx.coroutines.launch

/** `지도 내보내기` from a map screen (WLD-51): [exporting] is true while the image is made. */
class MapExporter internal constructor(private val run: (MapBase, MapScene, String, LargeMapExport) -> Unit) {
    var exporting by mutableStateOf(false)
        internal set

    /** Draws [base] with [scene] (what the screen shows at the lens) and opens the system share sheet. */
    fun export(base: MapBase, scene: MapScene, mapName: String, size: LargeMapExport) = run(base, scene, mapName, size)
}

/** One exporter per screen; a failure shows `MapCopy.EXPORT_FAILED`. */
@Composable
fun rememberMapExporter(deps: FeatureDeps): MapExporter {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = CivilightTheme.colors
    val art = FixedColors.mapArt(if (colors.isDark) ResolvedTheme.DARK else ResolvedTheme.LIGHT)
    val exportColors = ExportColors.of(colors, art)
    val holder = remember { arrayOfNulls<MapExporter>(1) }
    val colorsNow = remember { arrayOf(exportColors) }
    colorsNow[0] = exportColors
    return holder[0] ?: MapExporter { base, scene, name, size ->
        val exporter = holder[0] ?: return@MapExporter
        if (exporter.exporting) return@MapExporter
        exporter.exporting = true
        scope.launch {
            try {
                val bitmap = MapExport.render(context, deps.stores.blobs, base, scene, colorsNow[0], ExportPlan.longEdge(size))
                val shared = bitmap != null && MapExport.share(context, bitmap, ExportPlan.fileName(name, size))
                if (!shared) deps.toasts.show(MapCopy.EXPORT_FAILED, actionLabel = null)
            } finally {
                exporter.exporting = false
            }
        }
    }.also { holder[0] = it }
}
