@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.map

import io.github.lenticualrstone.civilight.model.Entity
import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.OrderKey
import io.github.lenticualrstone.civilight.model.Ordered
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.Trashable
import io.github.lenticualrstone.civilight.model.open
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * One map with its overlays and generation record: the file unit `maps/<mapKey>.json` (§2.7, §7.6).
 * All coordinates are in [0, 1] of the base (x right, y down). A pin's child map is derived from
 * [parent] of the child.
 */
@Serializable
data class WorldMap(
    override val key: Key,
    override val order: OrderKey,
    val name: String,
    /** The only nesting link (the child names its parent pin). */
    val parent: MapParent? = null,
    val baseKind: OpenEnum<MapBaseKind>,
    val baseImageHash: String? = null,
    val baseSize: MapSize? = null,
    val sketch: MapSketch? = null,
    val format: OpenEnum<MapFormat>? = null,
    val style: OpenEnum<MapStyle>? = null,
    @EncodeDefault val contours: Boolean = false,
    val scale: MapScale? = null,
    /** `숨은 것 흐리게`. */
    @EncodeDefault val revealViewDim: Boolean = false,
    @EncodeDefault val layers: List<MapLayer> = emptyList(),
    @EncodeDefault val pins: List<Pin> = emptyList(),
    @EncodeDefault val regions: List<MapRegion> = emptyList(),
    @EncodeDefault val routes: List<Route> = emptyList(),
    @EncodeDefault val labels: List<MapLabel> = emptyList(),
    @EncodeDefault val stamps: List<Stamp> = emptyList(),
    @EncodeDefault val drawnLines: List<DrawnLine> = emptyList(),
    val hex: HexGrid? = null,
    val poly: PolyMap? = null,
    @EncodeDefault val nodes: List<NodeMapNode> = emptyList(),
    @EncodeDefault val nodeLinks: List<NodeMapLink> = emptyList(),
    val generation: GeneratedMapRecord? = null,
    override val deletedAt: Timestamp? = null,
    override val trashEntryKey: Key? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity, Ordered, Trashable

@Serializable
data class MapParent(
    val mapKey: Key,
    val pinKey: Key,
    override val ext: JsonObject? = null,
) : HasExt

enum class MapBaseKind { GENERATED, IMAGE, SKETCH, NODE }

enum class MapFormat { HEX, POLY, DETAIL, OUTLINE, NODE }

/** `색면 · 먹선 · 색 · 양피지`. */
enum class MapStyle { SOFT, INK, COLOR, PARCHMENT }

/** Pixel size of the base image. */
@Serializable
data class MapSize(
    val w: Int,
    val h: Int,
    override val ext: JsonObject? = null,
) : HasExt

/** A photographed sketch and its 4 corner points. */
@Serializable
data class MapSketch(
    val photoHash: String,
    val corners: List<Point>,
    override val ext: JsonObject? = null,
) : HasExt

/** `축척 맞추기` (WLD-46): two points and the distance between them. */
@Serializable
data class MapScale(
    val a: Point,
    val b: Point,
    val value: Double,
    val unit: OpenEnum<ScaleUnit>,
    /** For unit `day`: the travel mode a day refers to. */
    val travelModeKey: Key? = null,
    override val ext: JsonObject? = null,
) : HasExt

enum class ScaleUnit { DAY, KM, LI, CUSTOM }
