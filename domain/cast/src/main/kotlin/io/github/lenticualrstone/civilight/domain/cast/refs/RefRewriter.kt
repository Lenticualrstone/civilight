package io.github.lenticualrstone.civilight.domain.cast.refs

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.DocBlock
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.TextRun

/** Rewrites key references in place (merge and its undo). Text is never changed. */
object RefRewriter {
    private fun runs(list: List<TextRun>, f: (TextRun.Link) -> TextRun.Link): List<TextRun> {
        if (list.none { it is TextRun.Link }) return list
        return list.map { if (it is TextRun.Link) f(it) else it }
    }

    private fun doc(d: RichDoc, f: (TextRun.Link) -> TextRun.Link): RichDoc =
        if (d.none { b -> b.runs.any { it is TextRun.Link } }) d else d.map { b: DocBlock -> b.copy(runs = runs(b.runs, f)) }

    private fun fields(map: Map<Key, FieldValue>, f: (TextRun.Link) -> TextRun.Link): Map<Key, FieldValue> = map.mapValues { (_, v) ->
        val p = v.payload as? FieldPayload.Rich ?: return@mapValues v
        v.copy(payload = FieldPayload.Rich(doc(p.doc, f)))
    }

    /** Applies [f] to every link run of every in-project text surface. */
    fun mapLinks(d: ProjectDoc, f: (TextRun.Link) -> TextRun.Link): ProjectDoc = d.copy(
        episodes = d.episodes.mapValues { (_, file) ->
            file.copy(blocks = file.blocks.map { b ->
                b.copy(runs = runs(b.runs, f), options = b.options.map { o -> o.copy(runs = runs(o.runs, f)) })
            })
        },
        story = d.story.copy(units = d.story.units.map { u -> u.transitionRuns?.let { u.copy(transitionRuns = runs(it, f)) } ?: u }),
        cast = d.cast.copy(
            characters = d.cast.characters.map { c ->
                c.copy(speechBody = doc(c.speechBody, f), secretBody = doc(c.secretBody, f), fields = fields(c.fields, f))
            },
            voiceLines = d.cast.voiceLines.map { it.copy(text = doc(it.text, f)) },
            archiveDocs = d.cast.archiveDocs.map { it.copy(body = doc(it.body, f)) },
        ),
        world = d.world.copy(
            terms = d.world.terms.map { it.copy(description = doc(it.description, f), fields = fields(it.fields, f)) },
            places = d.world.places.map { it.copy(description = doc(it.description, f), fields = fields(it.fields, f)) },
            factions = d.world.factions.map { it.copy(description = doc(it.description, f), fields = fields(it.fields, f)) },
            events = d.world.events.map { it.copy(description = doc(it.description, f)) },
        ),
        pages = d.pages.mapValues { (_, p) -> p.copy(blocks = doc(p.blocks, f)) },
    )

    /** Points the link runs [linkKeys] at [to] (kind and text unchanged). */
    fun retargetLinks(d: ProjectDoc, linkKeys: Set<Key>, to: Key): ProjectDoc =
        if (linkKeys.isEmpty()) d else mapLinks(d) { l -> if (l.linkKey in linkKeys) l.copy(target = l.target.copy(key = to)) else l }

    /** Sets the speaker of a block (or of one of its options) from [from] to [to]. */
    fun setSpeaker(d: ProjectDoc, blockKey: Key, optionKey: Key?, from: Key, to: Key): ProjectDoc = d.copy(
        episodes = d.episodes.mapValues { (_, file) ->
            file.copy(blocks = file.blocks.map { b ->
                when {
                    b.key != blockKey -> b
                    optionKey == null -> if (b.speakerKey == from) b.copy(speakerKey = to) else b
                    else -> b.copy(options = b.options.map { o -> if (o.key == optionKey && o.speakerKey == from) o.copy(speakerKey = to) else o })
                }
            })
        },
    )

    /** Replaces [from] by [to] in a scene's silent cast, or removes [from] when [to] is already there. Returns how. */
    fun moveSilentCast(d: ProjectDoc, sceneKey: Key, from: Key, to: Key): Pair<ProjectDoc, String?> {
        val scene = d.index.unit(sceneKey) ?: return d to null
        val index = scene.silentCast.indexOf(from)
        if (index < 0) return d to null
        val (cast, how) = if (to in scene.silentCast) scene.silentCast.filterIndexed { i, _ -> i != index } to "silentCast:removed@$index"
        else scene.silentCast.map { if (it == from) to else it } to "silentCast"
        return d.copy(story = d.story.copy(units = d.story.units.map { if (it.key == sceneKey) it.copy(silentCast = cast) else it })) to how
    }

    /** Undo of [moveSilentCast]. */
    fun undoSilentCast(d: ProjectDoc, sceneKey: Key, field: String, from: Key, to: Key): ProjectDoc {
        val scene = d.index.unit(sceneKey) ?: return d
        val cast = if (field.startsWith("silentCast:removed@")) {
            val i = field.substringAfter("@").toIntOrNull() ?: scene.silentCast.size
            scene.silentCast.toMutableList().apply { add(i.coerceIn(0, size), from) }
        } else {
            scene.silentCast.map { if (it == to) from else it }
        }
        return d.copy(story = d.story.copy(units = d.story.units.map { if (it.key == sceneKey) it.copy(silentCast = cast) else it }))
    }

    /** Points a canvas item node at [to]. */
    fun setCanvasNodeRef(d: ProjectDoc, nodeKey: Key, to: Key): ProjectDoc = d.copy(
        canvases = d.canvases.mapValues { (_, c) ->
            if (c.nodes.none { it.key == nodeKey }) c else c.copy(nodes = c.nodes.map { if (it.key == nodeKey) it.copy(refKey = to) else it })
        },
    )
}
