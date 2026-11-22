package io.github.lenticualrstone.civilight.domain.recorder.put

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.text.RichDoc

/** A tab or field of a card that `카드에 넣기` can append to. [id] is stored as `Placement.tabId`. */
data class CardTab(val id: String, val label: String)

/**
 * The long texts of cards (CST-49, Q19): a character has `개요` (its first non-secret `긴 글` field,
 * as `domain:cast` decides: the model has no separate 개요 body), `말투`, `비밀` and every `긴 글`
 * field; a place, term or faction has `설명` (its description) and every `긴 글` field of its kind.
 */
object CardDocs {
    const val OVERVIEW = "overview"
    const val SPEECH = "speech"
    const val SECRET = "secret"
    const val DESCRIPTION = "description"

    fun entityKind(kind: RecorderTargetKind): EntityKind? = when (kind) {
        RecorderTargetKind.CHARACTER -> EntityKind.CHARACTER
        RecorderTargetKind.PLACE -> EntityKind.PLACE
        RecorderTargetKind.TERM -> EntityKind.TERM
        RecorderTargetKind.FACTION -> EntityKind.FACTION
        else -> null
    }

    /** Live `긴 글` field definitions of an entity kind in order (character-scoped ones only for that character). */
    fun longFields(doc: ProjectDoc, kind: EntityKind, entityKey: Key): List<FieldDef> = doc.index.fieldDefs(kind).filter {
        it.deletedAt == null && it.type.known == FieldType.LONG_TEXT && (it.scopeCharacterKey == null || it.scopeCharacterKey == entityKey)
    }

    /** The field `개요` appends to: the first non-secret `긴 글` field of characters. */
    fun overviewField(doc: ProjectDoc, characterKey: Key): FieldDef? =
        longFields(doc, EntityKind.CHARACTER, characterKey).firstOrNull { !it.isSecret }

    fun exists(doc: ProjectDoc, kind: RecorderTargetKind, key: Key): Boolean = name(doc, kind, key) != null

    fun name(doc: ProjectDoc, kind: RecorderTargetKind, key: Key): String? = when (kind) {
        RecorderTargetKind.CHARACTER -> doc.index.character(key)?.name
        RecorderTargetKind.PLACE -> doc.index.place(key)?.name
        RecorderTargetKind.TERM -> doc.index.term(key)?.name
        RecorderTargetKind.FACTION -> doc.index.faction(key)?.name
        else -> null
    }

    /** The tabs of a card in order. */
    fun tabs(doc: ProjectDoc, kind: RecorderTargetKind, key: Key): List<CardTab> {
        val entityKind = entityKind(kind) ?: return emptyList()
        if (!exists(doc, kind, key)) return emptyList()
        val fields = longFields(doc, entityKind, key).map { CardTab(it.key, it.name) }
        return if (kind == RecorderTargetKind.CHARACTER) {
            listOfNotNull(
                overviewField(doc, key)?.let { CardTab(OVERVIEW, RecorderCopy.TAB_OVERVIEW) },
                CardTab(SPEECH, RecorderCopy.TAB_SPEECH),
                CardTab(SECRET, RecorderCopy.TAB_SECRET),
            ) + fields
        } else {
            listOf(CardTab(DESCRIPTION, RecorderCopy.TAB_DESCRIPTION)) + fields
        }
    }

    /** Label of a stored tab id (a deleted field shows its old name while the definition row exists). */
    fun tabLabel(doc: ProjectDoc, tabId: String?): String? = when (tabId) {
        null -> null
        OVERVIEW -> RecorderCopy.TAB_OVERVIEW
        SPEECH -> RecorderCopy.TAB_SPEECH
        SECRET -> RecorderCopy.TAB_SECRET
        DESCRIPTION -> RecorderCopy.TAB_DESCRIPTION
        else -> doc.index.fieldDef(tabId)?.name
    }

    /** Every long text of a card (bodies, description, rich field values). */
    fun allDocs(doc: ProjectDoc, kind: RecorderTargetKind, key: Key): List<RichDoc> {
        fun fieldDocs(fields: Map<Key, FieldValue>): List<RichDoc> = fields.values.mapNotNull { (it.payload as? FieldPayload.Rich)?.doc }
        return when (kind) {
            RecorderTargetKind.CHARACTER -> doc.index.character(key)?.let { listOf(it.speechBody, it.secretBody) + fieldDocs(it.fields) }
            RecorderTargetKind.PLACE -> doc.index.place(key)?.let { listOf(it.description) + fieldDocs(it.fields) }
            RecorderTargetKind.TERM -> doc.index.term(key)?.let { listOf(it.description) + fieldDocs(it.fields) }
            RecorderTargetKind.FACTION -> doc.index.faction(key)?.let { listOf(it.description) + fieldDocs(it.fields) }
            else -> null
        }.orEmpty()
    }

    /** The card kind holding entity [key] (characters, places, terms, factions), or null. */
    fun kindOf(doc: ProjectDoc, key: Key): RecorderTargetKind? = when {
        doc.index.character(key) != null -> RecorderTargetKind.CHARACTER
        doc.index.place(key) != null -> RecorderTargetKind.PLACE
        doc.index.term(key) != null -> RecorderTargetKind.TERM
        doc.index.faction(key) != null -> RecorderTargetKind.FACTION
        else -> null
    }

    /** True when the card holds paragraph [paragraphKey] in any of its long texts. */
    fun holdsParagraph(doc: ProjectDoc, entityKey: Key, paragraphKey: Key): Boolean {
        val kind = kindOf(doc, entityKey) ?: return false
        return allDocs(doc, kind, entityKey).any { d -> d.any { it.key == paragraphKey } }
    }

    /** Live state of a card row. */
    fun isLive(doc: ProjectDoc, kind: RecorderTargetKind, key: Key): Boolean = when (kind) {
        RecorderTargetKind.CHARACTER -> doc.liveness.isCharacterLive(key)
        RecorderTargetKind.PLACE -> doc.liveness.isPlaceLive(key)
        RecorderTargetKind.TERM -> doc.liveness.isTermLive(key)
        RecorderTargetKind.FACTION -> doc.liveness.isFactionLive(key)
        else -> false
    }

    /** Hidden state of a card row (rule 19 item hiding). */
    fun isHidden(doc: ProjectDoc, kind: RecorderTargetKind, key: Key): Boolean = when (kind) {
        RecorderTargetKind.CHARACTER -> doc.index.character(key)?.hidden == true
        RecorderTargetKind.PLACE -> doc.index.place(key)?.hidden == true
        RecorderTargetKind.TERM -> doc.index.term(key)?.hidden == true
        RecorderTargetKind.FACTION -> doc.index.faction(key)?.hidden == true
        else -> false
    }

    /**
     * [doc] with [change] applied to the long text [tabId] of card [key]; null when the card or the
     * tab does not exist. `updatedAt`/`updatedBy` are stamped on the card.
     */
    fun updateTab(
        doc: ProjectDoc,
        kind: RecorderTargetKind,
        key: Key,
        tabId: String,
        now: Timestamp,
        device: String,
        change: (RichDoc) -> RichDoc,
    ): ProjectDoc? {
        fun richField(fields: Map<Key, FieldValue>, defKey: Key): Map<Key, FieldValue> {
            val current = (fields[defKey]?.payload as? FieldPayload.Rich)?.doc.orEmpty()
            return fields + (defKey to FieldValue.of(FieldPayload.Rich(change(current))))
        }
        val tab = if (tabId == OVERVIEW) overviewField(doc, key)?.key ?: return null else tabId
        val entityKind = entityKind(kind) ?: return null
        val isField = longFields(doc, entityKind, key).any { it.key == tab }
        return when (kind) {
            RecorderTargetKind.CHARACTER -> {
                val row = doc.index.character(key) ?: return null
                val next = when {
                    tab == SPEECH -> row.copy(speechBody = change(row.speechBody))
                    tab == SECRET -> row.copy(secretBody = change(row.secretBody))
                    isField -> row.copy(fields = richField(row.fields, tab))
                    else -> return null
                }.copy(updatedAt = now, updatedBy = device)
                doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == key) next else it }))
            }
            RecorderTargetKind.PLACE -> {
                val row = doc.index.place(key) ?: return null
                val next = when {
                    tab == DESCRIPTION -> row.copy(description = change(row.description))
                    isField -> row.copy(fields = richField(row.fields, tab))
                    else -> return null
                }.copy(updatedAt = now, updatedBy = device)
                doc.copy(world = doc.world.copy(places = doc.world.places.map { if (it.key == key) next else it }))
            }
            RecorderTargetKind.TERM -> {
                val row = doc.index.term(key) ?: return null
                val next = when {
                    tab == DESCRIPTION -> row.copy(description = change(row.description))
                    isField -> row.copy(fields = richField(row.fields, tab))
                    else -> return null
                }.copy(updatedAt = now, updatedBy = device)
                doc.copy(world = doc.world.copy(terms = doc.world.terms.map { if (it.key == key) next else it }))
            }
            RecorderTargetKind.FACTION -> {
                val row = doc.index.faction(key) ?: return null
                val next = when {
                    tab == DESCRIPTION -> row.copy(description = change(row.description))
                    isField -> row.copy(fields = richField(row.fields, tab))
                    else -> return null
                }.copy(updatedAt = now, updatedBy = device)
                doc.copy(world = doc.world.copy(factions = doc.world.factions.map { if (it.key == key) next else it }))
            }
            else -> null
        }
    }

    /**
     * [doc] with paragraph [paragraphKey] removed from every long text of card [entityKey] (undo).
     * When [dropFieldIfEmpty] names a field whose value the insertion created, that value is
     * removed again once empty (empty values are never stored).
     */
    fun removeParagraph(doc: ProjectDoc, entityKey: Key, paragraphKey: Key, dropFieldIfEmpty: Key? = null): ProjectDoc {
        fun strip(d: RichDoc): RichDoc = d.filter { it.key != paragraphKey }
        fun stripFields(fields: Map<Key, FieldValue>): Map<Key, FieldValue> = buildMap {
            for ((defKey, value) in fields) {
                val rich = value.payload as? FieldPayload.Rich
                if (rich == null || rich.doc.none { it.key == paragraphKey }) {
                    put(defKey, value)
                    continue
                }
                val rest = strip(rich.doc)
                if (rest.isEmpty() && defKey == dropFieldIfEmpty) continue
                put(defKey, value.copy(payload = FieldPayload.Rich(rest)))
            }
        }
        return doc.copy(
            cast = doc.cast.copy(characters = doc.cast.characters.map {
                if (it.key == entityKey) it.copy(speechBody = strip(it.speechBody), secretBody = strip(it.secretBody), fields = stripFields(it.fields)) else it
            }),
            world = doc.world.copy(
                places = doc.world.places.map { if (it.key == entityKey) it.copy(description = strip(it.description), fields = stripFields(it.fields)) else it },
                terms = doc.world.terms.map { if (it.key == entityKey) it.copy(description = strip(it.description), fields = stripFields(it.fields)) else it },
                factions = doc.world.factions.map { if (it.key == entityKey) it.copy(description = strip(it.description), fields = stripFields(it.fields)) else it },
            ),
        )
    }
}
