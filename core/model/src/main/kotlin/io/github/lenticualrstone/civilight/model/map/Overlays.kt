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
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.PointRef
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** A map overlay row: keyed, optionally on a layer and revealed from a story point (WLD-49). */
interface MapOverlay : Entity {
    val revealPoint: PointRef?
}

@Serializable
data class MapLayer(
    override val key: Key,
    override val order: OrderKey,
    val name: String,
    @EncodeDefault val visible: Boolean = true,
    override val revealPoint: PointRef? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : MapOverlay, Ordered

/** A pin of a place; unique per (map, place). */
@Serializable
data class Pin(
    override val key: Key,
    val placeKey: Key,
    val x: Float,
    val y: Float,
    val layerKey: Key? = null,
    override val revealPoint: PointRef? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : MapOverlay

/** An area (`영역`); its key is project-unique so a scene can name it as `PlaceRef(region)`. */
@Serializable
data class MapRegion(
    override val key: Key,
    val name: String,
    /** Free kind text (WLD Q-17). */
    @EncodeDefault val kind: String = "",
    val factionKey: Key? = null,
    val placeKey: Key? = null,
    /** Token name or hex colour. */
    val color: String,
    val layerKey: Key? = null,
    override val revealPoint: PointRef? = null,
    /** Base shape (≥ 3 points); history via `regionShape` change records. */
    val vertices: List<Point>,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : MapOverlay

/** A route through free points and pins. */
@Serializable
data class Route(
    override val key: Key,
    val points: List<RoutePoint>,
    val layerKey: Key? = null,
    override val revealPoint: PointRef? = null,
    val shownModeKey: Key? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : MapOverlay

/** A route point: `{x, y}` (free) or `{pinKey}` (follows the pin). */
@Serializable
data class RoutePoint(
    val x: Float? = null,
    val y: Float? = null,
    val pinKey: Key? = null,
    override val ext: JsonObject? = null,
) : HasExt {
    val isPin: Boolean get() = pinKey != null

    companion object {
        fun at(x: Float, y: Float) = RoutePoint(x = x, y = y)
        fun pin(pinKey: Key) = RoutePoint(pinKey = pinKey)
    }
}

@Serializable
data class MapLabel(
    override val key: Key,
    val text: String,
    val x: Float,
    val y: Float,
    val curve: List<Point>? = null,
    val layerKey: Key? = null,
    override val revealPoint: PointRef? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : MapOverlay

@Serializable
data class Stamp(
    override val key: Key,
    val kind: OpenEnum<StampKind>,
    val x: Float,
    val y: Float,
    /** 1, 2 or 3. */
    @EncodeDefault val size: Int = 2,
    /** Degrees, a multiple of 15. */
    @EncodeDefault val rotation: Int = 0,
    val placeKey: Key? = null,
    val layerKey: Key? = null,
    override val revealPoint: PointRef? = null,
    /** Re-roll guard. */
    @EncodeDefault val attachedLayer: OpenEnum<AttachedLayer> = AttachedLayer.NONE.open(),
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : MapOverlay

/** 산 · 언덕 · 숲 · 도시 · 마을 · 성 · 폐허 · 탑 · 다리 · 항구 · 동굴 · 표지석. */
enum class StampKind { MOUNTAIN, HILL, FOREST, CITY, TOWN, CASTLE, RUIN, TOWER, BRIDGE, PORT, CAVE, MARKER }

enum class AttachedLayer { RIVER, ROAD, NONE }

/** A river or road of a detailed-terrain map. */
@Serializable
data class DrawnLine(
    override val key: Key,
    val kind: OpenEnum<DrawnLineKind>,
    val points: List<Point>,
    val origin: OpenEnum<LineOrigin>,
    @EncodeDefault val editedByAuthor: Boolean = false,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity

enum class DrawnLineKind { RIVER, ROAD }

enum class LineOrigin { GENERATED, AUTHOR }

@Serializable
data class NodeMapNode(
    override val key: Key,
    val name: String,
    val sub: String? = null,
    val placeKey: Key? = null,
    val x: Float,
    val y: Float,
    override val revealPoint: PointRef? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : MapOverlay

@Serializable
data class NodeMapLink(
    override val key: Key,
    val aNodeKey: Key,
    val bNodeKey: Key,
    val label: String? = null,
    override val revealPoint: PointRef? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : MapOverlay
