package io.github.lenticualrstone.civilight.feature.map.common

import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.map.MapEdit
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.MapBaseKind
import io.github.lenticualrstone.civilight.model.map.MapParent
import io.github.lenticualrstone.civilight.model.map.MapSize
import io.github.lenticualrstone.civilight.model.map.MapSketch

/** A doc with a new map and that map's key. */
data class Created(val doc: ProjectDoc, val mapKey: Key)

/**
 * New maps and base swaps (WLD-36, WLD-38, WLD-39, WLD-50) as pure edits over `MapEdits`. Each one
 * also records the base image's [Blob] row in the project's asset index, so `.civilight` export
 * packs the file (the package writer writes only catalogued blobs).
 */
object MapCreation {
    fun withBlob(doc: ProjectDoc, blob: Blob): ProjectDoc =
        if (doc.assets.blobs.any { it.hash == blob.hash }) doc else doc.copy(assets = doc.assets.copy(blobs = doc.assets.blobs + blob))

    private fun sizeOf(blob: Blob): MapSize? {
        val w = blob.width
        val h = blob.height
        return if (w != null && h != null && w > 0 && h > 0) MapSize(w, h) else null
    }

    /** `이미지 가져오기`: an IMAGE map on [blob], kept uncropped at full resolution. */
    fun imageMap(doc: ProjectDoc, blob: Blob, ctx: EditContext, parent: MapParent? = null): Created? {
        val made = MapEdits.newMap(doc, MapBaseKind.IMAGE, ctx, parent = parent) as? MapEdit.Applied ?: return null
        val based = MapEdits.replaceBase(made.doc, made.value.key, blob.hash, sizeOf(blob), ctx) ?: return null
        return Created(withBlob(based, blob), made.value.key)
    }

    /** `노드 지도`: a map without a base. */
    fun nodeMap(doc: ProjectDoc, ctx: EditContext, parent: MapParent? = null): Created? {
        val made = MapEdits.newMap(doc, MapBaseKind.NODE, ctx, parent = parent) as? MapEdit.Applied ?: return null
        return Created(made.doc, made.value.key)
    }

    /**
     * `스케치 촬영 → 펴기`: a SKETCH map whose base is the straightened image; the original photo and
     * its four corners (0–1 of the photo, TL TR BR BL) are kept.
     */
    fun sketchMap(doc: ProjectDoc, photo: Blob, straightened: Blob, corners: List<Point>, ctx: EditContext, parent: MapParent? = null): Created? {
        val made = MapEdits.newMap(doc, MapBaseKind.SKETCH, ctx, parent = parent) as? MapEdit.Applied ?: return null
        val key = made.value.key
        val based = MapEdits.replaceBase(made.doc, key, straightened.hash, sizeOf(straightened), ctx) ?: return null
        val sketched = MapEdits.updateMap(based, key, ctx) { it.copy(sketch = MapSketch(photo.hash, corners)) } ?: return null
        return Created(withBlob(withBlob(sketched, photo), straightened), key)
    }

    /** `지도 이미지 바꾸기`: a new base; every overlay keeps its 0–1 coordinates. */
    fun replaceBase(doc: ProjectDoc, mapKey: Key, blob: Blob, ctx: EditContext): ProjectDoc? =
        MapEdits.replaceBase(doc, mapKey, blob.hash, sizeOf(blob), ctx)?.let { withBlob(it, blob) }
}
