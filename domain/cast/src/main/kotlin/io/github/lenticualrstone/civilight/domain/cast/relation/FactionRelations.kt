package io.github.lenticualrstone.civilight.domain.cast.relation

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityRef
import io.github.lenticualrstone.civilight.model.cast.Relation
import io.github.lenticualrstone.civilight.model.cast.RelationEndKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * Faction-to-faction relations (CST-36, P2; rule 13 §8): the same shape as character relations
 * (`사이` + each side's stance) with faction ends. Storage only; screens are decided later. One
 * relation per pair. Deleting a faction takes its relations to the trash with it (rule 20; derived
 * liveness in `domain:trash`). The relationship map, the `관계` tab and the speaker panel read
 * character ends only, so these never show there.
 */
object FactionRelations {
    fun factionRef(key: Key): EntityRef = EntityRef(RelationEndKind.FACTION, key)

    /** The relation of two factions, live or not. */
    fun between(doc: ProjectDoc, a: Key, b: Key): Relation? = doc.index.relationBetween(factionRef(a), factionRef(b))

    /** The pair's relation, or a new empty one; both ends must be factions and differ. */
    fun openOrCreate(doc: ProjectDoc, a: Key, b: Key, ctx: EditContext): Pair<ProjectDoc, Relation> {
        require(a != b) { "a relation needs two factions" }
        require(doc.index.faction(a) != null && doc.index.faction(b) != null) { "both ends must be factions" }
        between(doc, a, b)?.let { return doc to it }
        val relation = Relation(key = ctx.newKey(), endA = factionRef(a), endB = factionRef(b), updatedAt = ctx.now, updatedBy = ctx.device)
        return doc.copy(cast = doc.cast.copy(relations = doc.cast.relations + relation)) to relation
    }

    /** Live relations with [factionKey] as an end. */
    fun of(doc: ProjectDoc, factionKey: Key): List<Relation> =
        doc.index.relationsOf(factionRef(factionKey)).filter { doc.liveness.isLive(it) }
}
