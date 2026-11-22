package io.github.lenticualrstone.civilight.feature.map.view

import android.content.ContentResolver
import android.net.Uri
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.common.MapCreation
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.common.MapScreenViewModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.store.blob.import
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

/** 지도 보기 (WLD-37, WLD-38 base swap, WLD-41 `핀 옮기기`). UI state lives in the screen (saveable). */
class MapViewViewModel(projectKey: Key, val mapKey: Key, env: MapEnv) : MapScreenViewModel(projectKey, env) {
    /** `핀 옮기기 · 완료`: one undo step. */
    fun movePin(pinKey: Key, to: Point, onDone: () -> Unit): Job {
        val ctx = editContext()
        val name = doc.value?.let { d -> d.maps[mapKey]?.pins?.firstOrNull { it.key == pinKey }?.let { d.index.place(it.placeKey)?.name } }
        return commit(MapCopy.step(MapCopy.BTN_MOVE_PIN, name), onDone) { d -> MapEdits.movePin(d, mapKey, pinKey, to, ctx) }
    }

    /** `지도 이미지 바꾸기`: a new base image; overlays keep their 0–1 coordinates. */
    suspend fun replaceBase(resolver: ContentResolver, uri: Uri) {
        if (readOnly.value) {
            env.toasts.show(MapCopy.READ_ONLY, actionLabel = null)
            return
        }
        val blob = try {
            env.blobs.import(resolver, uri, BlobSource.GALLERY)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        if (blob == null || !blob.mime.startsWith("image/")) {
            env.toasts.show(MapCopy.TODO_COPY_MAP_IMPORT_FAILED, actionLabel = null)
            return
        }
        val ctx = editContext()
        commit(MapCopy.BTN_REPLACE_BASE) { d -> MapCreation.replaceBase(d, mapKey, blob, ctx) }
    }
}
