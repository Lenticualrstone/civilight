package io.github.lenticualrstone.civilight.ui.picker

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.ui.labels.UnitLabels
import io.github.lenticualrstone.civilight.ui.search.KoreanSearch

/** What a picker row picks. */
enum class PickKind { CHARACTER, TERM, PLACE, REGION, FACTION, UNIT }

/**
 * One row. [insertText] is the text a link inserts (an alias when the alias matched, SCR-55);
 * [depth] indents unit rows.
 */
data class PickerItem(
    val kind: PickKind,
    val key: Key,
    val title: String,
    val subtitle: String? = null,
    val insertText: String = title,
    val depth: Int = 0,
    val avatar: Boolean = false,
)

data class PickerSection(val title: String?, val items: List<PickerItem>)

/** `지운 인물 · 노아 · 되살리기` (SCR-19, rule 20): the current speaker is a deleted character. */
data class DeletedRow(val kind: PickKind, val key: Key, val name: String)

/**
 * A picker's content. [hiddenCount] locked hidden items are left out and summed in the last row
 * `숨긴 <kind word> N · 지문으로 보기` (R19 §6, I-06, I-17).
 */
data class PickerModel(
    val sections: List<PickerSection>,
    val hiddenCount: Int = 0,
    val hiddenKindWord: String = PickerCopy.KIND_ITEM,
    val deleted: DeletedRow? = null,
) {
    val items: List<PickerItem> get() = sections.flatMap { it.items }
    val isEmpty: Boolean get() = sections.all { it.items.isEmpty() } && hiddenCount == 0 && deleted == null
}

/** Picker copy from the specs (SCR-19, SCR-55, R19 §6, R20). */
object PickerCopy {
    const val SPEAKER_TITLE = "화자"
    const val SEARCH_PLACEHOLDER = "초성 검색"
    const val ALL = "전체"
    const val IN_SCENE = "이 씬 등장"
    const val RECENT = "최근"
    const val KIND_CHARACTER = "인물"
    const val KIND_TERM = "용어"
    const val KIND_PLACE = "장소"
    const val KIND_FACTION = "세력"
    const val KIND_ITEM = "항목"
    const val HIDDEN_PREFIX = "숨긴"
    const val UNLOCK = "지문으로 보기"
    const val DELETED_CHARACTER = "지운 인물"
    const val RESTORE = "되살리기"
    const val ALIAS_OF = "의 별칭"
    const val SEP = " · "

    fun hiddenRow(kindWord: String, count: Int) = "$HIDDEN_PREFIX $kindWord $count"
}

/** Builds picker content from a document; no domain module needed (counts and names only). */
object PickerModels {
    const val RECENT_MAX = 5

    /**
     * 화자 고르기 (SCR-19, CST-37): sections `이 씬 등장` ([sceneCast] order), `최근` ([recent],
     * at most 5, not already listed), `전체` (the rest by name). [tagKey] filters by tag. Trashed
     * characters are never listed; [currentSpeakerKey] naming one gives the [DeletedRow].
     */
    fun characters(
        doc: ProjectDoc,
        query: String = "",
        tagKey: Key? = null,
        sceneCast: List<Key> = emptyList(),
        recent: List<Key> = emptyList(),
        hiddenOpen: Boolean = false,
        currentSpeakerKey: Key? = null,
        subtitleOf: (Character) -> String? = { defaultSubtitle(doc, it) },
    ): PickerModel {
        val live = doc.characters.filter { it.deletedAt == null }
        val visible = live.filter { hiddenOpen || !it.hidden }
        val hiddenCount = live.count { it.hidden && !hiddenOpen }
        val matching = visible.filter { c ->
            (tagKey == null || tagKey in c.tagKeys) &&
                (query.isBlank() || KoreanSearch.matchesAny(listOf(c.name, c.displayNameBase) + c.tagKeys.mapNotNull { doc.index.tag(it)?.name }, query))
        }
        val byKey = matching.associateBy { it.key }
        val inScene = sceneCast.distinct().mapNotNull(byKey::get)
        val listed = inScene.mapTo(HashSet()) { it.key }
        val recents = recent.distinct().mapNotNull(byKey::get).filter { it.key !in listed }.take(RECENT_MAX)
        listed += recents.map { it.key }
        val rest = matching.filter { it.key !in listed }.sortedBy { it.name }
        fun row(c: Character) = PickerItem(PickKind.CHARACTER, c.key, c.name, subtitleOf(c), avatar = true)
        val deleted = currentSpeakerKey?.let(doc.index::character)?.takeIf { it.deletedAt != null }
            ?.let { DeletedRow(PickKind.CHARACTER, it.key, it.name) }
        return PickerModel(
            sections = listOf(
                PickerSection(PickerCopy.IN_SCENE, inScene.map(::row)),
                PickerSection(PickerCopy.RECENT, recents.map(::row)),
                PickerSection(PickerCopy.ALL, rest.map(::row)),
            ).filter { it.items.isNotEmpty() },
            hiddenCount = hiddenCount,
            hiddenKindWord = PickerCopy.KIND_CHARACTER,
            deleted = deleted,
        )
    }

    /** Row sub line of a character: its tags (`주연 · 등불 결사`), else its one-liner. */
    fun defaultSubtitle(doc: ProjectDoc, c: Character): String? =
        c.tagKeys.mapNotNull { doc.index.tag(it)?.name }.takeIf { it.isNotEmpty() }?.joinToString(PickerCopy.SEP)
            ?: c.oneLiner?.takeIf { it.isNotBlank() }

    /** 용어 고르기: names and aliases; an alias hit reads `오리지늄 · 오리지늄 등불의 별칭` and inserts the alias. */
    fun terms(doc: ProjectDoc, query: String = "", hiddenOpen: Boolean = false): PickerModel {
        val live = doc.world.terms.filter { it.deletedAt == null }
        val visible = live.filter { hiddenOpen || !it.hidden }.sortedBy { it.name }
        val rows = buildList {
            for (t in visible) {
                when {
                    query.isBlank() || KoreanSearch.matches(t.name, query) -> add(PickerItem(PickKind.TERM, t.key, t.name))
                    else -> t.aliases.firstOrNull { KoreanSearch.matches(it, query) }?.let { alias ->
                        add(PickerItem(PickKind.TERM, t.key, alias, subtitle = "${t.name}${PickerCopy.ALIAS_OF}", insertText = alias))
                    }
                }
            }
        }
        return PickerModel(
            sections = listOf(PickerSection(null, rows)).filter { it.items.isNotEmpty() },
            hiddenCount = live.count { it.hidden && !hiddenOpen },
            hiddenKindWord = PickerCopy.KIND_TERM,
        )
    }

    /**
     * 장소 고르기 (WLD-52): places by name, then, when [includeRegions], map regions of live maps
     * (sub line = map name). Regions tied to a hidden place follow that place while locked.
     */
    fun places(doc: ProjectDoc, query: String = "", hiddenOpen: Boolean = false, includeRegions: Boolean = true): PickerModel {
        val live = doc.world.places.filter { it.deletedAt == null }
        val hiddenPlaces = live.filter { it.hidden }.mapTo(HashSet()) { it.key }
        val places = live.filter { hiddenOpen || !it.hidden }
            .filter { query.isBlank() || KoreanSearch.matches(it.name, query) }
            .sortedBy { it.name }
            .map { p -> PickerItem(PickKind.PLACE, p.key, p.name, subtitle = p.parentPlaceKey?.let(doc.index::place)?.name) }
        val regions = if (!includeRegions) emptyList() else doc.maps.values
            .filter { it.deletedAt == null }
            .sortedBy { it.order }
            .flatMap { map ->
                map.regions
                    .filter { r -> hiddenOpen || r.placeKey == null || r.placeKey !in hiddenPlaces }
                    .filter { query.isBlank() || KoreanSearch.matches(it.name, query) }
                    .map { r -> PickerItem(PickKind.REGION, r.key, r.name, subtitle = map.name) }
            }
        return PickerModel(
            sections = listOf(PickerSection(null, places), PickerSection(null, regions)).filter { it.items.isNotEmpty() },
            hiddenCount = live.count { it.hidden && !hiddenOpen },
            hiddenKindWord = PickerCopy.KIND_PLACE,
        )
    }

    /** The picked place or region as the kind a scene stores (`PlaceRef`). */
    fun placeRefKind(item: PickerItem): PlaceRefKind? = when (item.kind) {
        PickKind.PLACE -> PlaceRefKind.PLACE
        PickKind.REGION -> PlaceRefKind.REGION
        else -> null
    }

    /** 세력 고르기. */
    fun factions(doc: ProjectDoc, query: String = "", hiddenOpen: Boolean = false): PickerModel {
        val live = doc.world.factions.filter { it.deletedAt == null }
        val rows = live.filter { hiddenOpen || !it.hidden }
            .filter { query.isBlank() || KoreanSearch.matches(it.name, query) }
            .sortedBy { it.name }
            .map { PickerItem(PickKind.FACTION, it.key, it.name) }
        return PickerModel(
            sections = listOf(PickerSection(null, rows)).filter { it.items.isNotEmpty() },
            hiddenCount = live.count { it.hidden && !hiddenOpen },
            hiddenKindWord = PickerCopy.KIND_FACTION,
        )
    }

    /**
     * `@` 연결 (SCR-55): people, terms (with aliases) and places in one list. Locked hidden items are
     * left out and counted; the last row names their kind when only one kind is hidden.
     */
    fun links(doc: ProjectDoc, query: String = "", hiddenOpen: Boolean = false): PickerModel {
        val people = characters(doc, query, hiddenOpen = hiddenOpen)
        val terms = terms(doc, query, hiddenOpen)
        val places = places(doc, query, hiddenOpen, includeRegions = false)
        val parts = listOf(people, terms, places)
        val hiddenKinds = parts.filter { it.hiddenCount > 0 }
        return PickerModel(
            sections = listOf(
                PickerSection(PickerCopy.KIND_CHARACTER, people.items),
                PickerSection(PickerCopy.KIND_TERM, terms.items),
                PickerSection(PickerCopy.KIND_PLACE, places.items),
            ).filter { it.items.isNotEmpty() },
            hiddenCount = parts.sumOf { it.hiddenCount },
            hiddenKindWord = hiddenKinds.singleOrNull()?.hiddenKindWord ?: PickerCopy.KIND_ITEM,
        )
    }

    /**
     * 단위 고르기: live units in story order as indented rows (`Ⅰ 1장 · 등불지기`, `1-7 · 잿빛 새벽의 등불`,
     * `1-7 씬 3 · 꺼진 등불`). [levels] limits the rows to those levels (e.g. only scenes); hidden
     * scenes keep their place with the title masked while locked (`1-7 씬 4 · 숨긴 씬`, I-02).
     */
    fun units(
        doc: ProjectDoc,
        query: String = "",
        levels: Set<Int>? = null,
        hiddenOpen: Boolean = false,
        labels: UnitLabels = UnitLabels(doc),
    ): PickerModel {
        val rows = ArrayList<PickerItem>()
        fun visit(parent: Key?) {
            for (unit in doc.index.children(parent)) {
                if (!doc.liveness.isLive(unit)) continue
                if (levels == null || unit.level in levels) {
                    val title = unitTitle(unit, labels, hiddenOpen)
                    if (query.isBlank() || KoreanSearch.matches(title, query)) {
                        rows += PickerItem(PickKind.UNIT, unit.key, title, depth = if (levels == null) unit.level else 0)
                    }
                }
                visit(unit.key)
            }
        }
        visit(null)
        return PickerModel(listOf(PickerSection(null, rows)).filter { it.items.isNotEmpty() })
    }

    private fun unitTitle(unit: StoryUnit, labels: UnitLabels, hiddenOpen: Boolean): String {
        val masked = unit.hidden && !hiddenOpen && labels.isScene(unit)
        return when {
            masked -> "${labels.reference(unit)}${PickerCopy.SEP}$HIDDEN_SCENE"
            unit.level == 0 -> labels.header(unit).orEmpty()
            else -> labels.referenceWithTitle(unit).orEmpty()
        }
    }

    /** R19 §6 hidden scene label. */
    const val HIDDEN_SCENE = "숨긴 씬"
}
