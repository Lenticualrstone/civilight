package io.github.lenticualrstone.civilight.feature.map.list

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.common.Created
import io.github.lenticualrstone.civilight.feature.map.common.MapCreation
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.common.MapScreenViewModel
import io.github.lenticualrstone.civilight.feature.map.sketch.SketchResult
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.MapParent
import io.github.lenticualrstone.civilight.store.blob.import
import io.github.lenticualrstone.civilight.ui.project.EditResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** 지도 목록 · 새 지도 (WLD-36, WLD-38, WLD-39, WLD-50 creation). */
class MapListViewModel(projectKey: Key, env: MapEnv) : MapScreenViewModel(projectKey, env) {
    val ui: StateFlow<MapListUi?> = combine(doc, hiddenOpen) { d, open ->
        d?.let { MapListState.build(it, lensOf(it), open) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Applies a map creation as one undo step; returns the new map's key, or null (read-only, refused). */
    private suspend fun create(build: (ProjectDoc) -> Created?): Key? {
        var key: Key? = null
        val result = update(MapCopy.STEP_NEW_MAP) { d -> build(d)?.also { key = it.mapKey }?.doc ?: d }
        if (result == EditResult.READ_ONLY) {
            env.toasts.show(MapCopy.READ_ONLY, actionLabel = null)
            return null
        }
        return key
    }

    suspend fun createNodeMap(parent: MapParent?): Key? {
        val ctx = editContext()
        return create { MapCreation.nodeMap(it, ctx, parent) }
    }

    /** `이미지 가져오기` from the gallery or the file picker: stored once by content hash, uncropped. */
    suspend fun importImage(resolver: ContentResolver, uri: Uri, source: BlobSource, parent: MapParent?): Key? {
        if (readOnly.value) {
            env.toasts.show(MapCopy.READ_ONLY, actionLabel = null)
            return null
        }
        val blob = try {
            env.blobs.import(resolver, uri, source)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            env.toasts.show(MapCopy.TODO_COPY_MAP_IMPORT_FAILED, actionLabel = null)
            return null
        }
        if (!blob.mime.startsWith("image/")) {
            env.toasts.show(MapCopy.TODO_COPY_MAP_IMPORT_FAILED, actionLabel = null)
            return null
        }
        val ctx = editContext()
        return create { MapCreation.imageMap(it, blob, ctx, parent) }
    }

    /** `스케치 촬영 → 펴기`: the photo and the straightened image become a SKETCH map. */
    suspend fun createSketchMap(result: SketchResult, parent: MapParent?): Key? {
        val ctx = editContext()
        return create { MapCreation.sketchMap(it, result.photo, result.straightened, result.corners, ctx, parent) }
    }
}
