package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.UlidGenerator
import io.github.lenticualrstone.civilight.model.canvas.Canvas
import io.github.lenticualrstone.civilight.model.canvas.CanvasEdge
import io.github.lenticualrstone.civilight.model.canvas.CanvasNode
import io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import java.util.Random
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/**
 * Fixtures over the example project 「잿빛 새벽의 등불」 (docs/status/fixture.md): **S0** is the example as
 * built; **S0-noRel** is S0 minus the 리엔 · 하르트 relation and its records (spec §4).
 */
object CanvasFx {
    const val CANVAS = K.CANVAS_CHAPTER_1
    const val EDGE_RIEN_HART = K.CANVAS_EDGE_RIEN_HART
    val EDGE_BLACKOUT_LANTERN: Key = K.canvasEdge(2)
    val NODE_FACTION: Key = K.canvasNode(1)
    val NODE_RIEN: Key = K.canvasNode(2)
    val NODE_HART: Key = K.canvasNode(3)
    val NODE_BLACKOUT: Key = K.canvasNode(4)
    val NODE_LANTERN: Key = K.canvasNode(5)
    val NODE_MEMO: Key = K.canvasNode(6)

    val LATEST: LensPoint = LensPoint.Latest
    val L1_6 = LensPoint.UnitEnd(K.EPISODE_1_6)
    val L1_7 = LensPoint.UnitEnd(K.EPISODE_1_7)
    val L1_8 = LensPoint.UnitEnd(K.EPISODE_1_8)
    val L1_9 = LensPoint.UnitEnd(K.EPISODE_1_9)

    fun s0(): ProjectDoc = ExampleProject.build()

    /** S0 without the 리엔 · 하르트 relation, its records, and the canvas line's relation key. */
    fun s0NoRel(): ProjectDoc {
        val d = s0()
        val canvas = d.canvases.getValue(CANVAS)
        return d.copy(
            cast = d.cast.copy(relations = d.cast.relations.filterNot { it.key == K.REL_RIEN_HART }),
            changes = d.changes.copy(records = d.changes.records.filterNot { (it.target as? ChangeTarget.RelationSlot)?.relationKey == K.REL_RIEN_HART }),
            canvases = d.canvases + (CANVAS to canvas.copy(edges = canvas.edges.map { if (it.key == EDGE_RIEN_HART) it.copy(relationKey = null) else it })),
        )
    }

    /** 2026-11-22 09:00 KST, deterministic keys. */
    fun ctx(seed: Long = 5): EditContext {
        val generator = UlidGenerator(clock = { ExampleClock.NOW }, random = Random(seed))
        return EditContext(newKey = generator::next, now = ExampleClock.NOW, device = "test-device", zone = ExampleClock.ZONE)
    }

    fun canvas(doc: ProjectDoc): Canvas = doc.canvases.getValue(CANVAS)

    fun edge(doc: ProjectDoc, key: Key): CanvasEdge = canvas(doc).edges.single { it.key == key }

    /** Adds a node (and optionally a line from [lineFrom] with [relationKey]) to `1장 구상`. */
    fun withNode(
        doc: ProjectDoc,
        nodeKey: Key,
        kind: CanvasNodeKind,
        refKey: Key?,
        x: Float,
        y: Float,
        text: String? = null,
    ): ProjectDoc {
        val c = canvas(doc)
        val node = CanvasNode(key = nodeKey, kind = kind.open(), refKey = refKey, text = text, x = x, y = y, z = "z$nodeKey")
        return doc.copy(canvases = doc.canvases + (CANVAS to c.copy(nodes = c.nodes + node)))
    }

    fun withLine(doc: ProjectDoc, edgeKey: Key, from: Key, to: Key, relationKey: Key? = null, label: String? = null): ProjectDoc {
        val c = canvas(doc)
        val edge = CanvasEdge(key = edgeKey, fromNodeKey = from, toNodeKey = to, relationKey = relationKey, labelText = label)
        return doc.copy(canvases = doc.canvases + (CANVAS to c.copy(edges = c.edges + edge)))
    }
}
