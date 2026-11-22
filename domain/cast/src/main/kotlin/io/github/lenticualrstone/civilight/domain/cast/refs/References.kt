package io.github.lenticualrstone.civilight.domain.cast.refs

import io.github.lenticualrstone.civilight.domain.cast.entityKind
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.world.ItemRefKind

/** Kinds of references to an item that stay in place when it is deleted (rule 20 §2). */
enum class RefKind {
    /** Dialogue speaker (block or spoken choice option). */
    SPEAKER,

    /** Scene `등장` (silent cast). */
    APPEARANCE,

    /** `@` link run. */
    LINK,

    /** Recorder `연결` chip (outside the project file). */
    RECORDER,

    /** A `연결` field value (base or record). */
    FIELD_LINK,

    /** A term's `관련`. */
    TERM_RELATED,

    /** A canvas item node. */
    CANVAS_NODE,

    /** A scene's place. */
    SCENE_PLACE,
}

/**
 * One reference: where it sits ([ownerKind], [ownerKey], [field]) and whether its container is live.
 * The location triple is what a merge records as a moved reference (`MergeRecord.movedRefs`):
 * - speaker: `block` · block key · `speakerKey` or `options:<optionKey>`;
 * - appearance: `scene` · scene key · `silentCast`;
 * - link: the container kind · container key · `link:<linkKey>`;
 * - field link: entity kind · entity key · `fields:<fieldDefKey>`, or `record` · record key · `value`;
 * - term related: `term` · term key · `related`; canvas node: `canvasNode` · node key · `refKey`;
 * - scene place: `scene` · scene key · `place`; recorder: `recorderLink` · link key · `targetKey`.
 */
data class ItemReference(
    val kind: RefKind,
    val ownerKind: String,
    val ownerKey: Key,
    val field: String,
    /** Scene of a script reference (for `1-6 씬 2 · 씬 4`). */
    val sceneKey: Key? = null,
    /** False inside a trashed scene, block, page … : not counted, not checked (CST-41). */
    val live: Boolean = true,
)

/** Finds every key reference to a character, place, term or faction (§3 reference surfaces). */
object References {
    private fun EntityKind.linkKind(): LinkTargetKind = when (this) {
        EntityKind.CHARACTER -> LinkTargetKind.CHARACTER
        EntityKind.PLACE -> LinkTargetKind.PLACE
        EntityKind.TERM -> LinkTargetKind.TERM
        EntityKind.FACTION -> LinkTargetKind.FACTION
    }

    private fun EntityKind.recorderKind(): RecorderTargetKind = when (this) {
        EntityKind.CHARACTER -> RecorderTargetKind.CHARACTER
        EntityKind.PLACE -> RecorderTargetKind.PLACE
        EntityKind.TERM -> RecorderTargetKind.TERM
        EntityKind.FACTION -> RecorderTargetKind.FACTION
    }

    /** Link runs of a run list pointing at the item. */
    private fun linksIn(runs: List<TextRun>, kind: LinkTargetKind, key: Key): List<TextRun.Link> =
        runs.filterIsInstance<TextRun.Link>().filter { it.target.kind.known == kind && it.target.key == key && it.target.projectKey == null }

    private fun linksInDoc(doc: RichDoc, kind: LinkTargetKind, key: Key): List<TextRun.Link> = doc.flatMap { linksIn(it.runs, kind, key) }

    /**
     * Every reference to [key] of [kind] in the project, plus recorder chips from [recorderLinks]
     * (the recorder library lives outside the project; pass the links of this project).
     */
    fun scan(doc: ProjectDoc, kind: EntityKind, key: Key, recorderLinks: List<RecorderLink> = emptyList()): List<ItemReference> {
        val out = mutableListOf<ItemReference>()
        val lk = kind.linkKind()
        val live = doc.liveness

        // Script: speakers, links in blocks and options.
        for (block in doc.allBlocks) {
            val blockLive = live.isLive(block)
            if (kind == EntityKind.CHARACTER) {
                if (block.speakerKey == key) out += ItemReference(RefKind.SPEAKER, "block", block.key, "speakerKey", block.sceneKey, blockLive)
                for (o in block.options) if (o.speakerKey == key) {
                    out += ItemReference(RefKind.SPEAKER, "block", block.key, "options:${o.key}", block.sceneKey, blockLive)
                }
            }
            for (l in linksIn(block.runs, lk, key)) out += ItemReference(RefKind.LINK, "block", block.key, "link:${l.linkKey}", block.sceneKey, blockLive)
            for (o in block.options) for (l in linksIn(o.runs, lk, key)) {
                out += ItemReference(RefKind.LINK, "block", block.key, "link:${l.linkKey}", block.sceneKey, blockLive)
            }
        }
        // Scenes: silent cast, place, transition links.
        for (unit in doc.units) {
            val unitLive = live.isLive(unit)
            if (kind == EntityKind.CHARACTER && key in unit.silentCast) {
                out += ItemReference(RefKind.APPEARANCE, "scene", unit.key, "silentCast", unit.key, unitLive)
            }
            if (kind == EntityKind.PLACE && unit.place?.kind?.known == PlaceRefKind.PLACE && unit.place?.key == key) {
                out += ItemReference(RefKind.SCENE_PLACE, "scene", unit.key, "place", unit.key, unitLive)
            }
            for (l in linksIn(unit.transitionRuns.orEmpty(), lk, key)) {
                out += ItemReference(RefKind.LINK, "scene", unit.key, "link:${l.linkKey}", unit.key, unitLive)
            }
        }
        // Documents of characters (bodies, long-text fields), voice lines, archives.
        for (c in doc.cast.characters) {
            val ownerLive = c.deletedAt == null && c.key != key
            for (l in linksInDoc(c.speechBody, lk, key) + linksInDoc(c.secretBody, lk, key)) {
                out += ItemReference(RefKind.LINK, "character", c.key, "link:${l.linkKey}", live = ownerLive)
            }
            for ((_, v) in c.fields) (v.payload as? FieldPayload.Rich)?.let { rich ->
                for (l in linksInDoc(rich.doc, lk, key)) out += ItemReference(RefKind.LINK, "character", c.key, "link:${l.linkKey}", live = ownerLive)
            }
        }
        for (v in doc.cast.voiceLines) for (l in linksInDoc(v.text, lk, key)) {
            out += ItemReference(RefKind.LINK, "voiceLine", v.key, "link:${l.linkKey}", live = live.isCharacterLive(v.characterKey) && v.characterKey != key)
        }
        for (a in doc.cast.archiveDocs) for (l in linksInDoc(a.body, lk, key)) {
            out += ItemReference(RefKind.LINK, "archiveDoc", a.key, "link:${l.linkKey}", live = live.isCharacterLive(a.characterKey) && a.characterKey != key)
        }
        // World documents and term relations.
        for (t in doc.world.terms) {
            for (l in linksInDoc(t.description, lk, key)) out += ItemReference(RefKind.LINK, "term", t.key, "link:${l.linkKey}", live = t.deletedAt == null && t.key != key)
            val relatedKind = when (kind) {
                EntityKind.TERM -> ItemRefKind.TERM
                EntityKind.PLACE -> ItemRefKind.PLACE
                EntityKind.FACTION -> ItemRefKind.FACTION
                EntityKind.CHARACTER -> null
            }
            if (relatedKind != null && t.related.any { it.kind.known == relatedKind && it.key == key }) {
                out += ItemReference(RefKind.TERM_RELATED, "term", t.key, "related", live = t.deletedAt == null && t.key != key)
            }
        }
        for (p in doc.world.places) for (l in linksInDoc(p.description, lk, key)) {
            out += ItemReference(RefKind.LINK, "place", p.key, "link:${l.linkKey}", live = p.deletedAt == null && p.key != key)
        }
        for (f in doc.world.factions) for (l in linksInDoc(f.description, lk, key)) {
            out += ItemReference(RefKind.LINK, "faction", f.key, "link:${l.linkKey}", live = f.deletedAt == null && f.key != key)
        }
        for (e in doc.world.events) for (l in linksInDoc(e.description, lk, key)) {
            out += ItemReference(RefKind.LINK, "event", e.key, "link:${l.linkKey}", live = e.deletedAt == null)
        }
        for (page in doc.pages.values) for (l in linksInDoc(page.blocks, lk, key)) {
            out += ItemReference(RefKind.LINK, "page", page.key, "link:${l.linkKey}", live = page.deletedAt == null)
        }
        // `연결` field values: base values of all kinds and values of field records.
        if (kind != EntityKind.CHARACTER) {
            val linkDefs = doc.project.fieldDefs.filter { d ->
                d.type.known == FieldType.LINK && d.linkTargetKind?.known?.entityKind == kind
            }.associateBy { it.key }
            fun holds(v: FieldValue?) = (v?.payload as? FieldPayload.Link)?.targetKeys?.contains(key) == true
            fun entities(): List<Triple<String, Key, Pair<Boolean, Map<Key, FieldValue>>>> =
                doc.cast.characters.map { Triple("character", it.key, (it.deletedAt == null) to it.fields) } +
                    doc.world.places.map { Triple("place", it.key, (it.deletedAt == null) to it.fields) } +
                    doc.world.terms.map { Triple("term", it.key, (it.deletedAt == null) to it.fields) } +
                    doc.world.factions.map { Triple("faction", it.key, (it.deletedAt == null) to it.fields) }
            for ((ownerKind, ownerKey, pair) in entities()) {
                val (ownerLive, fields) = pair
                for ((defKey, v) in fields) if (defKey in linkDefs && holds(v)) {
                    out += ItemReference(RefKind.FIELD_LINK, ownerKind, ownerKey, "fields:$defKey", live = ownerLive && live.isFieldDefLive(defKey))
                }
            }
            for (r in doc.changes.records) {
                val t = r.target as? ChangeTarget.Field ?: continue
                if (t.fieldDefKey !in linkDefs) continue
                if (holds(r.valueAs(FieldValue.serializer()))) out += ItemReference(RefKind.FIELD_LINK, "record", r.key, "value", live = live.isLive(r))
            }
        }
        // Canvas item nodes.
        val nodeKind = when (kind) {
            EntityKind.CHARACTER -> CanvasNodeKind.CHARACTER
            EntityKind.FACTION -> CanvasNodeKind.FACTION
            EntityKind.TERM -> CanvasNodeKind.TERM
            EntityKind.PLACE -> null
        }
        if (nodeKind != null) for (canvas in doc.canvases.values) for (n in canvas.nodes) {
            if (n.kind.known == nodeKind && n.refKey == key) {
                out += ItemReference(RefKind.CANVAS_NODE, "canvasNode", n.key, "refKey", live = canvas.deletedAt == null)
            }
        }
        // Recorder chips.
        val rk = kind.recorderKind()
        for (link in recorderLinks) {
            if (link.projectKey == doc.project.key && link.targetKind.known == rk && link.targetKey == key) {
                out += ItemReference(RefKind.RECORDER, "recorderLink", link.key, "targetKey", live = link.purgedAt == null)
            }
        }
        return out
    }
}
