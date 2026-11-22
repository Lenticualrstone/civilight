package io.github.lenticualrstone.civilight.model.sample

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.TextRun

/** Walks over every keyed row and every text surface of the example (test helper). */
internal object ExampleWalk {
    /** A run list and the surface it sits on. */
    data class Surface(val name: String, val runs: List<TextRun>, val kind: Kind)

    enum class Kind { SCRIPT, DOCUMENT, RECORDER }

    fun docs(doc: ProjectDoc): List<Pair<String, RichDoc>> = buildList {
        for (c in doc.cast.characters) {
            add("speech ${c.name}" to c.speechBody)
            add("secret ${c.name}" to c.secretBody)
            for ((_, v) in c.fields) (v.payload as? io.github.lenticualrstone.civilight.model.cast.FieldPayload.Rich)?.let { add("field ${c.name}" to it.doc) }
        }
        for (v in doc.cast.voiceLines) add("voice ${v.title}" to v.text)
        for (a in doc.cast.archiveDocs) add("archive ${a.title}" to a.body)
        for (t in doc.world.terms) add("term ${t.name}" to t.description)
        for (p in doc.world.places) add("place ${p.name}" to p.description)
        for (f in doc.world.factions) add("faction ${f.name}" to f.description)
        for (e in doc.world.events) add("event ${e.title}" to e.description)
        for (p in doc.pages.values) add("page ${p.name}" to p.blocks)
    }

    fun surfaces(doc: ProjectDoc): List<Surface> = buildList {
        for (b in doc.allBlocks) {
            add(Surface("block ${b.number} of ${b.sceneKey}", b.runs, Kind.SCRIPT))
            for (o in b.options) add(Surface("option ${o.key}", o.runs, Kind.SCRIPT))
        }
        for (u in doc.units) u.transitionRuns?.let { add(Surface("transition ${u.key}", it, Kind.SCRIPT)) }
        for ((name, rich) in docs(doc)) for (p in rich) add(Surface("$name ${p.key}", p.runs, Kind.DOCUMENT))
        for (r in ExampleRecorder.records()) add(Surface("recorder ${r.key}", r.runs, Kind.RECORDER))
    }

    fun links(doc: ProjectDoc): List<Pair<Surface, TextRun.Link>> =
        surfaces(doc).flatMap { s -> s.runs.filterIsInstance<TextRun.Link>().map { s to it } }

    /** Every identity key of the project and its recorder slice, with duplicates kept. */
    fun identityKeys(doc: ProjectDoc): List<Key> = buildList {
        add(doc.project.key)
        doc.project.travelModes.forEach { add(it.key) }
        doc.project.tags.forEach { add(it.key) }
        doc.project.relationKinds.forEach { add(it.key) }
        doc.project.fieldDefs.forEach { d -> add(d.key); d.options.orEmpty().forEach { add(it.key) } }
        doc.units.forEach { add(it.key) }
        doc.allBlocks.forEach { b -> add(b.key); b.cues.forEach { add(it.key) }; b.options.forEach { add(it.key) } }
        doc.cast.characters.forEach { add(it.key) }
        doc.cast.relations.forEach { add(it.key) }
        doc.cast.voiceLines.forEach { add(it.key) }
        doc.cast.archiveDocs.forEach { add(it.key) }
        doc.world.terms.forEach { t -> add(t.key); t.rejections.forEach { add(it.key) } }
        doc.world.places.forEach { add(it.key) }
        doc.world.factions.forEach { add(it.key) }
        doc.world.events.forEach { add(it.key) }
        for (m in doc.maps.values) {
            add(m.key)
            m.layers.forEach { add(it.key) }
            m.pins.forEach { add(it.key) }
            m.regions.forEach { add(it.key) }
            m.routes.forEach { add(it.key) }
            m.labels.forEach { add(it.key) }
            m.stamps.forEach { add(it.key) }
            m.drawnLines.forEach { add(it.key) }
            m.nodes.forEach { add(it.key) }
            m.nodeLinks.forEach { add(it.key) }
        }
        for (c in doc.canvases.values) {
            add(c.key)
            c.nodes.forEach { add(it.key) }
            c.edges.forEach { add(it.key) }
        }
        doc.pages.values.forEach { add(it.key) }
        docs(doc).forEach { (_, rich) -> rich.forEach { add(it.key) } }
        doc.changes.records.forEach { add(it.key) }
        doc.checks.acknowledgements.forEach { add(it.key) }
        doc.checks.travelOverrides.forEach { add(it.key) }
        doc.assets.imageUses.forEach { add(it.key) }
        doc.assets.scriptAssets.forEach { add(it.key) }
        doc.trash.entries.forEach { add(it.key) }
        links(doc).forEach { (_, l) -> add(l.linkKey) }
        ExampleRecorder.records().forEach { add(it.key) }
        ExampleRecorder.links().forEach { add(it.key) }
        ExampleRecorder.placements().forEach { add(it.key) }
        ExampleRecorder.trashEntries().forEach { add(it.key) }
    }
}
