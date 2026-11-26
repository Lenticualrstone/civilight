package io.github.lenticualrstone.civilight.domain.world.item

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.common.StoryCodes
import io.github.lenticualrstone.civilight.domain.world.map.MapViews
import io.github.lenticualrstone.civilight.domain.world.map.PinStatus
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.history
import io.github.lenticualrstone.civilight.model.doc.recordAt
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.text.docText
import io.github.lenticualrstone.civilight.model.world.Faction
import io.github.lenticualrstone.civilight.model.world.Place

/** A list row of the place or faction list (WLD-03, WLD-04). */
data class WorldListRow(val key: Key, val name: String, val fragment: String, val right: String)

/** A titled section of a list (`지역` group of places). */
data class WorldListSection(val title: String?, val rows: List<WorldListRow>)

/** A list of places or factions with the locked row `숨긴 장소 N` (WLD-13). */
data class WorldList(val sections: List<WorldListSection>, val lockRow: String?, val emptyLine: String?)

/** A field row of a card: `상태 | 대정전 이후` with `◆` when the value has history. */
data class FieldRow(val fieldDefKey: Key, val label: String, val value: String, val changed: Boolean)

/** The place card (WLD-09): summary rows and the `지도` tab. */
data class PlaceCard(
    val key: Key,
    /** `장소 · 하층 구역` (the parent place) or `장소`. */
    val eyebrow: String,
    val name: String,
    val description: String,
    /** `등장 | 1-2 · 1-7 외 4`. */
    val appearance: String,
    /** `지역 | 결사의 문`, or null. */
    val parentName: String?,
    /** `배경 에셋 | 2`. */
    val backgroundAssets: Int,
    val fields: List<FieldRow>,
    /** `빈 항목 N · 보기`, or null when every field has a value. */
    val emptyLine: String?,
    val tabs: List<String>,
    /** Maps that hold a pin of the place (`◇ 지도 없음` when that map is in the trash). */
    val pins: List<PinStatus>,
)

/** A character tied to a faction by a `연결` field: `리엔 · 소속`. */
data class LinkedCharacter(val characterKey: Key, val label: String)

/** The faction card (WLD-10, `[PROPOSED]` layout). */
data class FactionCard(
    val key: Key,
    val name: String,
    val description: String,
    val fields: List<FieldRow>,
    val emptyLine: String?,
    /** `이 항목으로 잇는 인물`. */
    val linkedCharacters: List<LinkedCharacter>,
    /** Regions and provinces with `연결 세력` = this faction: `상층 경비대 · 세계 지도`. */
    val areas: List<String>,
    /** Events at which a `◆` of this faction changes (event keys). */
    val changeEvents: List<Key>,
    /** (2026-11-26) `거점` (fixed field) and `소속 인물` by the project's `묶음 기준` ([Affiliations.factionMembers]). */
    val affiliation: FactionMembers? = null,
)

/**
 * Place and faction lists and cards (WLD-03, WLD-04, WLD-09, WLD-10). Values follow the lens;
 * hidden items collapse into one locked row while locked.
 */
class WorldCards(private val doc: ProjectDoc, private val hiddenUnlocked: Boolean = false) {
    private val codes = StoryCodes(doc)

    // --- Places ---

    private fun livePlaces(): List<Place> = doc.world.places.filter { it.deletedAt == null }.sortedWith(compareBy({ it.order }, { it.key }))

    /** Episodes whose live scenes use the place (or a region tied to the place), in manuscript order. */
    fun appearanceEpisodes(placeKey: Key): List<String> {
        val regions = doc.maps.values.filter { doc.liveness.isMapLive(it.key) }
            .flatMap { m -> m.regions.filter { it.placeKey == placeKey }.map { it.key } }.toSet()
        return doc.manuscript.liveScenes.filter { s ->
            val ref = s.place ?: return@filter false
            when (ref.kind.known) {
                PlaceRefKind.PLACE -> ref.key == placeKey
                PlaceRefKind.REGION -> ref.key in regions
                null -> false
            }
        }.mapNotNull { codes.episodeCode(it.key) }.distinct()
    }

    /** `1-2 · 1-7 외 4` (the first two, then the rest counted), `1-3`, or `없음`. */
    fun appearanceLabel(placeKey: Key): String {
        val episodes = appearanceEpisodes(placeKey)
        if (episodes.isEmpty()) return "없음"
        val head = episodes.take(2).joinToString(Copy.SEP)
        return if (episodes.size > 2) "$head 외 ${episodes.size - 2}" else head
    }

    /** The place list (`[PROPOSED]`): ungrouped places first, then one section per `지역` parent. */
    fun placeList(): WorldList {
        val live = livePlaces()
        val locked = live.filter { it.hidden && !hiddenUnlocked }
        val visible = live - locked.toSet()
        fun row(p: Place) = WorldListRow(p.key, p.name, p.description.docText.lineSequence().firstOrNull().orEmpty(), "등장 ${appearanceLabel(p.key)}")
        val visibleKeys = visible.map { it.key }.toSet()
        val (grouped, top) = visible.partition { it.parentPlaceKey != null && it.parentPlaceKey in visibleKeys }
        val sections = ArrayList<WorldListSection>()
        if (top.isNotEmpty()) sections += WorldListSection(null, top.map(::row))
        for ((parent, children) in grouped.groupBy { it.parentPlaceKey!! }) {
            sections += WorldListSection(doc.index.place(parent)?.name, children.map(::row))
        }
        return WorldList(
            sections = sections,
            lockRow = if (locked.isEmpty()) null else "숨긴 장소 ${locked.size}",
            emptyLine = if (live.isEmpty()) "장소 없음" else null,
        )
    }

    fun placeCard(placeKey: Key, lens: LensPoint = LensPoint.Latest): PlaceCard? {
        val place = doc.index.place(placeKey) ?: return null
        val parent = place.parentPlaceKey?.let { doc.index.place(it) }?.takeIf { it.deletedAt == null }
        val (fields, empty) = fieldRows(EntityKind.PLACE, placeKey, place.fields, lens)
        return PlaceCard(
            key = placeKey,
            eyebrow = parent?.let { "장소${Copy.SEP}${it.name}" } ?: "장소",
            name = place.name,
            description = place.description.docText,
            appearance = appearanceLabel(placeKey),
            parentName = parent?.name,
            backgroundAssets = doc.assets.scriptAssets.count { it.placeKey == placeKey },
            fields = fields,
            emptyLine = if (empty > 0) "빈 항목 $empty${Copy.SEP}보기" else null,
            tabs = listOf("개요", "이곳의 시간", "지도"),
            pins = MapViews.pinsOf(doc, placeKey),
        )
    }

    // --- Factions ---

    private fun liveFactions(): List<Faction> = doc.world.factions.filter { it.deletedAt == null }.sortedWith(compareBy({ it.order }, { it.key }))

    /**
     * Live characters whose `연결` field (at the lens) points to [key], with the field name. (CST-73) A secret
     * field of a character whose 비밀 tab is locked is left out: the link shows only inside the confirmed tab.
     */
    fun linkedCharacters(key: Key, lens: LensPoint = LensPoint.Latest): List<LinkedCharacter> {
        val defs = doc.index.fieldDefs(EntityKind.CHARACTER).filter { it.deletedAt == null }
        return doc.cast.characters.filter { it.deletedAt == null && (!it.hidden || hiddenUnlocked) }
            .sortedWith(compareBy({ it.order }, { it.key }))
            .flatMap { c ->
                defs.filterNot { it.isSecret && c.secretTabLocked }.mapNotNull { def ->
                    val value = valueAt(EntityKind.CHARACTER, c.key, def.key, c.fields[def.key], lens)
                    val link = value?.payload as? FieldPayload.Link ?: return@mapNotNull null
                    if (key in link.targetKeys) LinkedCharacter(c.key, "${c.name}${Copy.SEP}${def.name}") else null
                }
            }
    }

    /** The faction list (`[PROPOSED]`): right column `인물 N`. */
    fun factionList(): WorldList {
        val live = liveFactions()
        val locked = live.filter { it.hidden && !hiddenUnlocked }
        val rows = (live - locked.toSet()).map { f ->
            WorldListRow(f.key, f.name, f.description.docText.lineSequence().firstOrNull().orEmpty(), "인물 ${linkedCharacters(f.key).size}")
        }
        return WorldList(
            sections = if (rows.isEmpty()) emptyList() else listOf(WorldListSection(null, rows)),
            lockRow = if (locked.isEmpty()) null else "숨긴 세력 ${locked.size}",
            emptyLine = if (live.isEmpty()) "세력 없음" else null,
        )
    }

    fun factionCard(factionKey: Key, lens: LensPoint = LensPoint.Latest): FactionCard? {
        val faction = doc.index.faction(factionKey) ?: return null
        val (fields, empty) = fieldRows(EntityKind.FACTION, factionKey, faction.fields, lens)
        val maps = MapViews.liveMaps(doc)
        val areas = maps.flatMap { m ->
            m.regions.filter { it.factionKey == factionKey }.map { "${it.name}${Copy.SEP}${m.name}" } +
                m.poly?.provinces.orEmpty().filter { it.factionKey == factionKey }.map { "${it.name}${Copy.SEP}${m.name}" }
        }
        val events = doc.changes.records
            .filter { doc.liveness.isLive(it) }
            .filter { (it.target as? ChangeTarget.Field)?.let { t -> t.entityKind.known == EntityKind.FACTION && t.entityKey == factionKey } == true }
            .mapNotNull { (it.anchor as? Anchor.WorldEvent)?.eventKey }
            .distinct()
        return FactionCard(
            key = factionKey,
            name = faction.name,
            description = faction.description.docText,
            fields = fields,
            emptyLine = if (empty > 0) "빈 항목 $empty${Copy.SEP}보기" else null,
            linkedCharacters = linkedCharacters(factionKey, lens),
            areas = areas,
            changeEvents = events,
            affiliation = Affiliations(doc, hiddenUnlocked).factionMembers(factionKey, lens),
        )
    }

    // --- Fields ---

    /** A field value at the lens: the live `◆` record in force, else [base] (also used by [Affiliations]). */
    internal fun valueAt(kind: EntityKind, entityKey: Key, defKey: Key, base: FieldValue?, lens: LensPoint): FieldValue? {
        val record = doc.recordAt(ChangeTarget.Field(kind, entityKey, defKey), lens) ?: return base
        return record.valueAs(FieldValue.serializer())
    }

    /** Non-empty project field rows at the lens, and the number of empty ones. */
    private fun fieldRows(kind: EntityKind, entityKey: Key, base: Map<Key, FieldValue>, lens: LensPoint): Pair<List<FieldRow>, Int> {
        val defs = doc.index.fieldDefs(kind).filter { it.deletedAt == null }
        var empty = 0
        val rows = defs.mapNotNull { def ->
            val value = valueAt(kind, entityKey, def.key, base[def.key], lens)
            val text = FieldText.of(doc, value)
            if (text.isNullOrBlank()) {
                empty++
                null
            } else {
                val changed = doc.history(ChangeTarget.Field(kind, entityKey, def.key)).isNotEmpty()
                FieldRow(def.key, def.name, text, changed)
            }
        }
        return rows to empty
    }
}
