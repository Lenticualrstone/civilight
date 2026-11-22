package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.TextRun

/** A row whose text held a rewritten link: owner kind (`block`, `page` …), owner key, field. */
internal data class RunOwner(val kind: String, val key: Key, val field: String)

/**
 * Rewrites `@` link runs on every text surface of the project, live and trashed (blocks and their
 * options, transitions, character bodies, long-text fields, voice lines, archive documents, world
 * and event descriptions, pages, long-text values of `◆` records, gallery descriptions). [transform] returns the
 * replacement run, or the same run to keep it.
 */
internal fun ProjectDoc.rewriteLinks(
    transform: (TextRun.Link) -> TextRun,
    at: Long,
    by: String,
): Pair<ProjectDoc, List<RunOwner>> {
    val owners = mutableListOf<RunOwner>()

    fun runs(list: List<TextRun>, owner: (() -> RunOwner)?): List<TextRun> {
        var changed = false
        val out = list.map { run ->
            if (run is TextRun.Link) {
                val next = transform(run)
                if (next != run) changed = true
                next
            } else {
                run
            }
        }
        if (!changed) return list
        if (owner != null) owners += owner()
        return out
    }

    fun rich(doc: RichDoc, owner: () -> RunOwner): RichDoc {
        var changed = false
        val out = doc.map { block ->
            val next = runs(block.runs, null)
            if (next !== block.runs) {
                changed = true
                block.copy(runs = next)
            } else {
                block
            }
        }
        if (!changed) return doc
        owners += owner()
        return out
    }

    fun fields(map: Map<Key, FieldValue>, ownerKind: String, ownerKey: Key): Map<Key, FieldValue> {
        var changed = false
        val out = map.mapValues { (defKey, value) ->
            val payload = value.payload as? FieldPayload.Rich ?: return@mapValues value
            val next = rich(payload.doc) { RunOwner(ownerKind, ownerKey, "fields.$defKey") }
            if (next !== payload.doc) {
                changed = true
                value.copy(payload = FieldPayload.Rich(next))
            } else {
                value
            }
        }
        return if (changed) out else map
    }

    val episodes = episodes.mapValues { (_, file) ->
        file.copy(
            blocks = file.blocks.map { block ->
                val r = runs(block.runs) { RunOwner("block", block.key, "runs") }
                val o = block.options.map { option ->
                    val next = runs(option.runs) { RunOwner("block", block.key, "options.${option.key}") }
                    if (next !== option.runs) option.copy(runs = next) else option
                }
                if (r !== block.runs || o != block.options) block.copy(runs = r, options = o, updatedAt = at, updatedBy = by) else block
            },
        )
    }
    val units = story.units.map { unit ->
        val t = unit.transitionRuns ?: return@map unit
        val next = runs(t) { RunOwner("unit", unit.key, "transitionRuns") }
        if (next !== t) unit.copy(transitionRuns = next, updatedAt = at, updatedBy = by) else unit
    }
    val characters = cast.characters.map { c ->
        val speech = rich(c.speechBody) { RunOwner("character", c.key, "speechBody") }
        val secret = rich(c.secretBody) { RunOwner("character", c.key, "secretBody") }
        val f = fields(c.fields, "character", c.key)
        if (speech !== c.speechBody || secret !== c.secretBody || f !== c.fields) {
            c.copy(speechBody = speech, secretBody = secret, fields = f, updatedAt = at, updatedBy = by)
        } else {
            c
        }
    }
    val voiceLines = cast.voiceLines.map { v ->
        val next = rich(v.text) { RunOwner("voiceLine", v.key, "text") }
        if (next !== v.text) v.copy(text = next, updatedAt = at, updatedBy = by) else v
    }
    val archiveDocs = cast.archiveDocs.map { a ->
        val next = rich(a.body) { RunOwner("archiveDoc", a.key, "body") }
        if (next !== a.body) a.copy(body = next, updatedAt = at, updatedBy = by) else a
    }
    val places = world.places.map { p ->
        val d = rich(p.description) { RunOwner("place", p.key, "description") }
        val f = fields(p.fields, "place", p.key)
        if (d !== p.description || f !== p.fields) p.copy(description = d, fields = f, updatedAt = at, updatedBy = by) else p
    }
    val terms = world.terms.map { t ->
        val d = rich(t.description) { RunOwner("term", t.key, "description") }
        val f = fields(t.fields, "term", t.key)
        if (d !== t.description || f !== t.fields) t.copy(description = d, fields = f, updatedAt = at, updatedBy = by) else t
    }
    val factions = world.factions.map { x ->
        val d = rich(x.description) { RunOwner("faction", x.key, "description") }
        val f = fields(x.fields, "faction", x.key)
        if (d !== x.description || f !== x.fields) x.copy(description = d, fields = f, updatedAt = at, updatedBy = by) else x
    }
    val events = world.events.map { e ->
        val d = rich(e.description) { RunOwner("event", e.key, "description") }
        if (d !== e.description) e.copy(description = d, updatedAt = at, updatedBy = by) else e
    }
    val pages = pages.mapValues { (_, page) ->
        val d = rich(page.blocks) { RunOwner("page", page.key, "blocks") }
        if (d !== page.blocks) page.copy(blocks = d, updatedAt = at, updatedBy = by) else page
    }
    val records = changes.records.map { record ->
        if (record.target !is ChangeTarget.Field) return@map record
        val value = record.fieldValue() ?: return@map record
        val payload = value.payload as? FieldPayload.Rich ?: return@map record
        val next = rich(payload.doc) { RunOwner("record", record.key, "value") }
        if (next === payload.doc) return@map record
        record.copy(
            value = ChangeRecord.encodeValue(FieldValue.serializer(), value.copy(payload = FieldPayload.Rich(next))),
            updatedAt = at, updatedBy = by,
        )
    }
    val gallery = gallery.mapValues { (_, file) ->
        val items = file.items.map { item ->
            val caption = item.caption ?: return@map item
            val next = rich(caption) { RunOwner("galleryItem", item.key, "caption") }
            if (next !== caption) item.copy(caption = next, updatedAt = at, updatedBy = by) else item
        }
        if (items == file.items) file else file.copy(items = items)
    }
    if (owners.isEmpty()) return this to emptyList()
    return copy(
        episodes = episodes,
        story = story.copy(units = units),
        cast = cast.copy(characters = characters, voiceLines = voiceLines, archiveDocs = archiveDocs),
        world = world.copy(places = places, terms = terms, factions = factions, events = events),
        pages = pages,
        changes = changes.copy(records = records),
        gallery = gallery,
    ) to owners.toList()
}
