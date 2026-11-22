package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.LensState
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.position.PointRef
import io.github.lenticualrstone.civilight.model.position.PointRefKind
import io.github.lenticualrstone.civilight.model.story.SceneTime

/**
 * Moves every in-project reference to a scene that a merge removes (B) onto the scene that stays
 * (A), so nothing is left pointing at a missing unit (which would read as unplaced):
 * - record anchors: `unitStart(B)` → B's first former block (else `unitEnd(A)`), `unitEnd(B)` →
 *   `unitEnd(A)`; the same for a remembered original anchor;
 * - reveal and unlock points (map overlays, timeline events, archive documents):
 *   `unitStart(B)` → `unitStart(A)` and `unitEnd(B)` → `unitEnd(A)`, which give the same visibility
 *   at every lens point that exists after the merge;
 * - the lens and the script's saved lens; story events on B; travel overrides (dropped when both
 *   ends become A); acknowledgements about B (dropped, they are re-evaluated);
 * - `rebasedFromKey = B` on any scene becomes A.
 * Recorder links to B live outside the project (recorder library) and are not touched here.
 */
internal object SceneRefs {
    fun retarget(doc: ProjectDoc, bKey: Key, aKey: Key, bFirstBlock: Key?): ProjectDoc {
        fun anchor(a: Anchor?): Anchor? = when (a) {
            is Anchor.UnitStart -> if (a.unitKey == bKey) bFirstBlock?.let { Anchor.AtBlock(it) } ?: Anchor.UnitEnd(aKey) else a
            is Anchor.UnitEnd -> if (a.unitKey == bKey) Anchor.UnitEnd(aKey) else a
            else -> a
        }
        fun point(p: PointRef?): PointRef? =
            if (p == null || p.unitKey != bKey) p
            else when (p.kind.known) {
                PointRefKind.UNIT_START -> PointRef.unitStart(aKey)
                PointRefKind.UNIT_END -> PointRef.unitEnd(aKey)
                null -> p.copy(unitKey = aKey)
            }
        fun lens(p: LensPoint?): LensPoint? = if (p is LensPoint.UnitEnd && p.unitKey == bKey) LensPoint.UnitEnd(aKey) else p

        val records = doc.changes.records.map { r ->
            val a = anchor(r.anchor)!!
            val o = anchor(r.originalAnchor)
            if (a == r.anchor && o == r.originalAnchor) r else r.copy(anchor = a, originalAnchor = o)
        }
        val events = doc.world.events.map { e ->
            e.copy(sceneKey = if (e.sceneKey == bKey) aKey else e.sceneKey, revealPoint = point(e.revealPoint))
        }
        val archive = doc.cast.archiveDocs.map { it.copy(unlockPoint = point(it.unlockPoint)) }
        val maps = doc.maps.mapValues { (_, m) -> m.withPoints(::point) }
        val viewLens = doc.app.view.lens
        val newLens = LensState(lens(viewLens.point) ?: LensPoint.Latest, lens(viewLens.savedForScript))
        val overrides = doc.checks.travelOverrides.map { o ->
            o.copy(
                fromSceneKey = if (o.fromSceneKey == bKey) aKey else o.fromSceneKey,
                toSceneKey = if (o.toSceneKey == bKey) aKey else o.toSceneKey,
            )
        }.filter { it.fromSceneKey != it.toSceneKey }
        val acks = doc.checks.acknowledgements.filterNot { bKey in it.subjectKeys }
        val units = doc.story.units.map { u ->
            val t = u.time as? SceneTime.Relative
            if (t?.rebasedFromKey == bKey) u.copy(time = t.copy(rebasedFromKey = aKey)) else u
        }
        return doc.copy(
            story = doc.story.copy(units = units),
            changes = doc.changes.copy(records = records),
            world = doc.world.copy(events = events),
            cast = doc.cast.copy(archiveDocs = archive),
            maps = maps,
            app = doc.app.copy(view = doc.app.view.copy(lens = newLens)),
            checks = doc.checks.copy(acknowledgements = acks, travelOverrides = overrides),
        )
    }

    private fun WorldMap.withPoints(point: (PointRef?) -> PointRef?): WorldMap = copy(
        layers = layers.map { it.copy(revealPoint = point(it.revealPoint)) },
        pins = pins.map { it.copy(revealPoint = point(it.revealPoint)) },
        regions = regions.map { it.copy(revealPoint = point(it.revealPoint)) },
        routes = routes.map { it.copy(revealPoint = point(it.revealPoint)) },
        labels = labels.map { it.copy(revealPoint = point(it.revealPoint)) },
        stamps = stamps.map { it.copy(revealPoint = point(it.revealPoint)) },
        nodes = nodes.map { it.copy(revealPoint = point(it.revealPoint)) },
        nodeLinks = nodeLinks.map { it.copy(revealPoint = point(it.revealPoint)) },
    )
}
