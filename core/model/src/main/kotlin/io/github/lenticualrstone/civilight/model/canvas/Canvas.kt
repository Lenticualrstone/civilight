@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.canvas

import io.github.lenticualrstone.civilight.model.Entity
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.OrderKey
import io.github.lenticualrstone.civilight.model.Ordered
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.Trashable
import io.github.lenticualrstone.civilight.model.text.RecorderSource
import io.github.lenticualrstone.civilight.model.text.RichDoc
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A canvas with its nodes and edges: the file unit `canvas/<key>.json` (§2.8). Viewport and last
 * opened time are app-only. Relation data never lives here.
 */
@Serializable
data class Canvas(
    override val key: Key,
    override val order: OrderKey,
    val name: String,
    @EncodeDefault val createdAt: Timestamp = 0L,
    @EncodeDefault val nodes: List<CanvasNode> = emptyList(),
    @EncodeDefault val edges: List<CanvasEdge> = emptyList(),
    override val deletedAt: Timestamp? = null,
    override val trashEntryKey: Key? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity, Ordered, Trashable

/** A node; an item node is a reference that stays (dotted) when its item is trashed (rule 20). */
@Serializable
data class CanvasNode(
    override val key: Key,
    val kind: OpenEnum<CanvasNodeKind>,
    /** Required for item kinds. */
    val refKey: Key? = null,
    /** Memo text or group title. */
    val text: String? = null,
    /** dp at zoom 1. */
    val x: Float,
    val y: Float,
    val w: Float? = null,
    val h: Float? = null,
    val groupKey: Key? = null,
    /** Stacking order. */
    val z: OrderKey,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity

enum class CanvasNodeKind {
    CHARACTER, FACTION, EVENT, TERM, MEMO, GROUP;

    val isItem: Boolean get() = this == CHARACTER || this == FACTION || this == EVENT || this == TERM
}

/** A line; [relationKey] only when both nodes are characters. */
@Serializable
data class CanvasEdge(
    override val key: Key,
    val fromNodeKey: Key,
    val toNodeKey: Key,
    val labelText: String? = null,
    val relationKey: Key? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity

/** A free document page: the file unit `pages/<key>.json` (§2.8). Header image = `ImageUse` slot `pageHeader`. */
@Serializable
data class Page(
    override val key: Key,
    override val order: OrderKey,
    val name: String,
    /** Q-PG-03. */
    val sectionLabel: String? = null,
    @EncodeDefault val blocks: RichDoc = emptyList(),
    @EncodeDefault val hidden: Boolean = false,
    /** Set when made by `페이지로 만들기` from a recorder record. */
    val sourceRecord: RecorderSource? = null,
    @EncodeDefault val createdAt: Timestamp = 0L,
    override val deletedAt: Timestamp? = null,
    override val trashEntryKey: Key? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity, Ordered, Trashable
