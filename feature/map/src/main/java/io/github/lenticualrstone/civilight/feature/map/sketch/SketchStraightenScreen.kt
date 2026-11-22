package io.github.lenticualrstone.civilight.feature.map.sketch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.common.MapScreenViewModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.map.MapParent
import io.github.lenticualrstone.civilight.navigation.MapEdit
import io.github.lenticualrstone.civilight.navigation.SketchStraighten
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** `펴기` as a back-stack entry (WLD-39): makes the SKETCH map, then opens it in `편집` for `덧그리기`. */
class SketchStraightenViewModel(projectKey: Key, env: MapEnv) : MapScreenViewModel(projectKey, env)

@Composable
fun SketchStraightenScreen(route: SketchStraighten, deps: FeatureDeps) {
    val vm = entryViewModel { SketchStraightenViewModel(route.projectKey, MapEnv.of(deps)) }
    val scope = rememberCoroutineScope()
    var photo by remember(route.photoHash) { mutableStateOf<Blob?>(null) }
    var working by remember { mutableStateOf(false) }
    LaunchedEffect(route.photoHash) {
        photo = runCatching { SketchImages.blobOf(deps.stores.blobs, route.photoHash) }
            .onFailure { if (it is CancellationException) throw it }
            .getOrNull()
        if (photo == null) {
            deps.toasts.show(MapCopy.TODO_COPY_MAP_IMPORT_FAILED, actionLabel = null)
            deps.navigator.pop()
        }
    }
    val parent = route.parentMapKey?.let { m -> route.parentPinKey?.let { MapParent(m, it) } }
    val current = photo ?: return
    StraightenScreen(
        photo = current,
        blobs = deps.stores.blobs,
        working = working,
        onStraighten = { corners ->
            working = true
            scope.launch {
                try {
                    val result = runCatching { SketchImages.straightenToBlob(deps.stores.blobs, current, corners) }
                        .onFailure { if (it is CancellationException) throw it }
                        .getOrNull()
                    if (result == null) {
                        deps.toasts.show(MapCopy.TODO_COPY_MAP_IMPORT_FAILED, actionLabel = null)
                        return@launch
                    }
                    val key = vm.createSketchMap(result, parent)
                    if (key != null) deps.navigator.replace(MapEdit(route.projectKey, key))
                } finally {
                    working = false
                }
            }
        },
        onCancel = { deps.navigator.pop() },
    )
}
