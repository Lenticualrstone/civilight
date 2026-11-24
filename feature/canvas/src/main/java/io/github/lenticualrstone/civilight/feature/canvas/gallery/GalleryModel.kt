package io.github.lenticualrstone.civilight.feature.canvas.gallery

import io.github.lenticualrstone.civilight.domain.library.hiding.HideableKind
import io.github.lenticualrstone.civilight.domain.library.hiding.ItemSurfaces
import io.github.lenticualrstone.civilight.domain.library.hiding.LockedItemsRow
import io.github.lenticualrstone.civilight.domain.library.hiding.MaskedList
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.ViewMode
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.gallery.GalleryItem
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.designsystem.card.CardSize
import io.github.lenticualrstone.civilight.ui.gallery.GalleryCardRules

/** The three gallery views (GAL-04): `목록` · `격자` · `카드`. */
enum class GalleryViewKind { LIST, GRID, CARD }

/**
 * The remembered view per gallery screen (GAL-04, T-GAL-37): `AppSettings.viewByScreen` keys
 * `characterGallery` and `projectGallery` (not per owner). Default `카드` (§6 Q2), unlike the other screens'
 * `목록`, so this does not reuse `ViewChoice`.
 */
object GalleryViewChoice {
    fun screenOf(kind: GalleryOwnerKind): String =
        if (kind == GalleryOwnerKind.PROJECT) AppSettings.GALLERY_PROJECT else AppSettings.GALLERY_CHARACTER

    fun of(viewByScreen: Map<String, ViewMode>, kind: GalleryOwnerKind): GalleryViewKind = when (viewByScreen[screenOf(kind)]) {
        ViewMode.LIST -> GalleryViewKind.LIST
        ViewMode.GRID -> GalleryViewKind.GRID
        ViewMode.CARD, null -> GalleryViewKind.CARD
    }

    fun modeOf(view: GalleryViewKind): ViewMode = when (view) {
        GalleryViewKind.LIST -> ViewMode.LIST
        GalleryViewKind.GRID -> ViewMode.GRID
        GalleryViewKind.CARD -> ViewMode.CARD
    }
}

/** Sort modes of the order line (GAL-04): `직접 순서` (stored), `넣은 날` (newest first), `씬 순서`. */
enum class GallerySort(val label: String) {
    MANUAL(GalleryUiCopy.SORT_MANUAL),
    ADDED(GalleryUiCopy.SORT_ADDED),
    SCENE(GalleryUiCopy.SORT_SCENE),
}

/**
 * One filter chip (one choice, GAL-04, GAL-10). Kept in saved UI state by [id] (`all`, `project`, `characters`,
 * `tag:<key>`).
 */
sealed interface GalleryFilter {
    val id: String

    data object All : GalleryFilter {
        override val id: String get() = "all"
    }

    /** Project gallery only: the project's own items. */
    data object Project : GalleryFilter {
        override val id: String get() = "project"
    }

    /** Project gallery only: every visible character's items. */
    data object Characters : GalleryFilter {
        override val id: String get() = "characters"
    }

    data class Tag(val tagKey: Key) : GalleryFilter {
        override val id: String get() = "tag:$tagKey"
    }

    companion object {
        fun fromId(id: String?): GalleryFilter = when {
            id == null || id == All.id -> All
            id == Project.id -> Project
            id == Characters.id -> Characters
            id.startsWith("tag:") -> Tag(id.removePrefix("tag:"))
            else -> All
        }
    }
}

/** Who may see this gallery right now (drawn before anything else, so a locked owner shows no name). */
enum class GalleryAccess {
    OPEN,

    /** A hidden character while locked (GAL-19): not reachable. */
    OWNER_LOCKED,

    /** The owner is in the trash or gone. */
    OWNER_GONE,
}

/** A filter chip: `전체 8`, `삽화 4`. */
data class GalleryChip(val filter: GalleryFilter, val label: String, val count: Int) {
    val text: String get() = GalleryUiCopy.chip(label, count)
}

/** One visible item with everything a card, tile or row draws. [position] is 1-based among the visible items. */
data class GalleryEntry(
    val item: GalleryItem,
    val position: Int,
    /** `숨김` chip (unlocked hidden item, GAL-19). */
    val hiddenTag: Boolean,
    /** `초상 · 홀로` (GAL-07), null when nothing applies. */
    val chip: String?,
    /** Card and list meta line: the scene code, else the tags (GAL-05, GAL-07). */
    val meta: String?,
) {
    val key: Key get() = item.key
    val title: String? get() = GalleryCardRules.shownTitle(item.title)
}

/**
 * A run of items with one header: the whole gallery, or one owner's group in the project gallery (GAL-10).
 * [header] is null for an ungrouped gallery. [ownerLink] = the character whose gallery `<name> 갤러리 ›` opens.
 */
data class GallerySection(
    val id: String,
    val header: String?,
    val ownerLink: Key?,
    val ownerName: String?,
    val entries: List<GalleryEntry>,
)

/** The whole gallery screen after masking, filter and sort (GAL-04 … GAL-10, GAL-19, GAL-25). */
data class GalleryView(
    val access: GalleryAccess,
    val ownerKind: GalleryOwnerKind,
    val ownerKey: Key,
    val ownerName: String,
    val filter: GalleryFilter,
    val sort: GallerySort,
    val chips: List<GalleryChip>,
    val sections: List<GallerySection>,
    /** Hidden items while locked: one locked tile, row or card at the end (`숨긴 이미지 2`). */
    val lockedRow: LockedItemsRow?,
    /** Hidden characters while locked (project gallery): one `LockRow` after the groups, no image count. */
    val hiddenCharacters: LockedItemsRow?,
    /** Hidden items shown because they are unlocked (`숨긴 이미지 2 · 열림`); 0 while locked. */
    val unlockedHidden: Int,
    /** Live items of this owner in any state, before the filter: 0 = the empty gallery (GAL-25). */
    val ownerTotal: Int,
) {
    val entries: List<GalleryEntry> by lazy { sections.flatMap { it.entries } }
    val keys: List<Key> by lazy { entries.map { it.key } }
    val count: Int get() = entries.size
    val grouped: Boolean get() = sections.any { it.header != null }
    val hiddenLocked: Int get() = lockedRow?.count ?: 0

    fun entry(key: Key?): GalleryEntry? = key?.let { k -> entries.firstOrNull { it.key == k } }

    fun position(key: Key?): String? = entry(key)?.let { GalleryUiCopy.position(it.position, count) }

    /** No item at all (GAL-25 empty gallery): no live visible item and no locked row. */
    val isEmpty: Boolean get() = ownerTotal == 0 && hiddenCharacters == null

    /** A filter that matches nothing while the gallery has items (GAL-25 `이 태그의 이미지 없음`). */
    val filteredEmpty: Boolean get() = !isEmpty && entries.isEmpty() && filter != GalleryFilter.All

    /** `직접 순서` with `전체` and no filter is the only state where order buttons work (GAL-15). */
    val orderEditable: Boolean get() = sort == GallerySort.MANUAL && filter == GalleryFilter.All

    /** `8장 · 숨김 2` (목록 · 격자 order-line slot, GAL-04). */
    val countLine: String get() = GalleryUiCopy.imageCount(count, hiddenLocked)

    /** The unlocked bar (`숨긴 이미지 2 · 열림 · 앱을 떠나면 잠김`), null when nothing hidden is open. */
    val unlockedBar: String? get() = if (unlockedHidden > 0) GalleryUiCopy.unlockedBar(LockedItemsRow(HideableKind.GALLERY_IMAGE, unlockedHidden).title) else null
}

/**
 * Builds [GalleryView] from the document (55 GAL-04 … GAL-10, GAL-19). Masking comes only from
 * `ItemSurfaces.gallery` / `projectGallery`, so the gallery shows exactly what the hiding contract allows.
 * [locked] = the project's hidden items are locked.
 */
object GalleryRows {
    /** More cards than this show the `41 · 128` readout instead of pager marks (GAL-04, GAL-25). */
    const val MAX_PAGER_MARKS = 12

    fun build(
        doc: ProjectDoc,
        ownerKind: GalleryOwnerKind,
        ownerKey: Key,
        locked: Boolean,
        filter: GalleryFilter,
        sort: GallerySort,
    ): GalleryView {
        val surfaces = ItemSurfaces(doc)
        val access = access(doc, surfaces, ownerKind, ownerKey, locked)
        if (access != GalleryAccess.OPEN) {
            return GalleryView(access, ownerKind, ownerKey, "", GalleryFilter.All, sort, emptyList(), emptyList(), null, null, 0, 0)
        }
        val ownerName = ownerName(doc, ownerKind, ownerKey)
        val labels = EntryLabels(doc, locked)
        // Live tags only (N-44): a trashed tag's key stays on images but shows nowhere until restored.
        val liveTags = doc.project.tags.filter { it.isLive }
        val tagNames = liveTags.associate { it.key to it.name }
        val tagOrder = liveTags.sortedBy { it.order }.map { it.key }

        // Groups after masking: one for a character gallery; project + characters for the project gallery.
        data class Group(val id: String, val ownerKind: GalleryOwnerKind, val ownerKey: Key, val name: String, val list: MaskedList<GalleryItem>)
        val groups: List<Group>
        var hiddenCharacters: LockedItemsRow? = null
        if (ownerKind == GalleryOwnerKind.PROJECT) {
            val p = surfaces.projectGallery(locked)
            groups = listOf(Group("project", GalleryOwnerKind.PROJECT, ownerKey, ownerName, p.project)) +
                p.characters.map { Group("c-${it.character.key}", GalleryOwnerKind.CHARACTER, it.character.key, it.character.name, it.items) }
            hiddenCharacters = p.hiddenCharacters
        } else {
            groups = listOf(Group("own", ownerKind, ownerKey, ownerName, surfaces.gallery(ownerKey, locked)))
        }

        val allVisible = groups.flatMap { it.list.items }
        val chips = chips(ownerKind, filter, groups.map { it.ownerKind to it.list.items }, allVisible, tagOrder, tagNames)
        val shownFilter = if (chips.any { it.filter == filter }) filter else GalleryFilter.All
        val grouped = ownerKind == GalleryOwnerKind.PROJECT && (shownFilter == GalleryFilter.All || shownFilter == GalleryFilter.Characters)

        var position = 0
        // GAL-10: a filter that is not grouped (`프로젝트`, a tag) shows one header-less run over every owner, sorted
        // as a whole, never several header-less rows.
        if (ownerKind == GalleryOwnerKind.PROJECT && !grouped) {
            val inFilter = groups.flatMap { g -> g.list.items.filter { matches(shownFilter, g.ownerKind, it) } }
            val taggedHidden = groups.flatMapTo(HashSet()) { it.list.taggedHidden }
            val entries = sortItems(inFilter, sort, labels).map { item ->
                position++
                GalleryEntry(item, position, item.key in taggedHidden, labels.chip(item), labels.meta(item, tagNames))
            }
            return finish(access, ownerKind, ownerKey, ownerName, shownFilter, sort, chips, listOf(GallerySection("own", null, null, ownerName, entries)), groups.map { it.list }, hiddenCharacters)
        }
        val sections = groups.mapNotNull { g ->
            val inFilter = g.list.items.filter { matches(shownFilter, g.ownerKind, it) }
            if (inFilter.isEmpty() && grouped) return@mapNotNull null
            if (inFilter.isEmpty() && ownerKind == GalleryOwnerKind.PROJECT) return@mapNotNull null
            val sorted = sortItems(inFilter, sort, labels)
            val entries = sorted.map { item ->
                position++
                GalleryEntry(item, position, item.key in g.list.taggedHidden, labels.chip(item), labels.meta(item, tagNames))
            }
            GallerySection(
                id = g.id,
                header = if (grouped) GalleryUiCopy.countLine(g.name, entries.size) else null,
                ownerLink = if (grouped && g.ownerKind == GalleryOwnerKind.CHARACTER) g.ownerKey else null,
                ownerName = g.name,
                entries = entries,
            )
        }.ifEmpty { listOf(GallerySection("own", null, null, ownerName, emptyList())) }
        return finish(access, ownerKind, ownerKey, ownerName, shownFilter, sort, chips, sections, groups.map { it.list }, hiddenCharacters)
    }

    private fun finish(
        access: GalleryAccess,
        ownerKind: GalleryOwnerKind,
        ownerKey: Key,
        ownerName: String,
        shownFilter: GalleryFilter,
        sort: GallerySort,
        chips: List<GalleryChip>,
        sections: List<GallerySection>,
        lists: List<MaskedList<GalleryItem>>,
        hiddenCharacters: LockedItemsRow?,
    ): GalleryView {
        val lockedCount = lists.sumOf { it.lockedRow?.count ?: 0 }
        val lockedRow = lists.firstNotNullOfOrNull { it.lockedRow }?.copy(count = lockedCount)
        val unlocked = lists.sumOf { it.taggedHidden.size }
        val total = lists.sumOf { it.items.size + (it.lockedRow?.count ?: 0) }
        return GalleryView(
            access = access,
            ownerKind = ownerKind,
            ownerKey = ownerKey,
            ownerName = ownerName,
            filter = shownFilter,
            sort = sort,
            chips = chips,
            sections = sections,
            lockedRow = lockedRow?.takeIf { lockedCount > 0 },
            hiddenCharacters = hiddenCharacters,
            unlockedHidden = unlocked,
            ownerTotal = total,
        )
    }

    fun access(doc: ProjectDoc, surfaces: ItemSurfaces, kind: GalleryOwnerKind, key: Key, locked: Boolean): GalleryAccess = when (kind) {
        GalleryOwnerKind.PROJECT -> GalleryAccess.OPEN
        GalleryOwnerKind.CHARACTER -> when {
            !doc.liveness.isCharacterLive(key) -> GalleryAccess.OWNER_GONE
            locked && key in surfaces.hidden.characters -> GalleryAccess.OWNER_LOCKED
            else -> GalleryAccess.OPEN
        }
    }

    fun ownerName(doc: ProjectDoc, kind: GalleryOwnerKind, key: Key): String = when (kind) {
        GalleryOwnerKind.PROJECT -> doc.project.name
        GalleryOwnerKind.CHARACTER -> doc.index.character(key)?.name.orEmpty()
    }

    /** `전체 <N>` (· `프로젝트 <N>` · `인물 <N>`) then one chip per used tag in project tag order. A chosen tag with 0 stays (`<tag> 0`, GAL-25). */
    private fun chips(
        ownerKind: GalleryOwnerKind,
        current: GalleryFilter,
        groups: List<Pair<GalleryOwnerKind, List<GalleryItem>>>,
        all: List<GalleryItem>,
        tagOrder: List<Key>,
        tagNames: Map<Key, String>,
    ): List<GalleryChip> = buildList {
        add(GalleryChip(GalleryFilter.All, GalleryUiCopy.FILTER_ALL, all.size))
        if (ownerKind == GalleryOwnerKind.PROJECT) {
            add(GalleryChip(GalleryFilter.Project, GalleryUiCopy.FILTER_PROJECT, groups.filter { it.first == GalleryOwnerKind.PROJECT }.sumOf { it.second.size }))
            add(GalleryChip(GalleryFilter.Characters, GalleryUiCopy.FILTER_CHARACTERS, groups.filter { it.first == GalleryOwnerKind.CHARACTER }.sumOf { it.second.size }))
        }
        val counts = HashMap<Key, Int>()
        all.forEach { item -> item.tagKeys.distinct().forEach { counts[it] = (counts[it] ?: 0) + 1 } }
        val chosenTag = (current as? GalleryFilter.Tag)?.tagKey
        for (tag in tagOrder) {
            val n = counts[tag] ?: 0
            if (n > 0 || (tag == chosenTag && tag in tagNames)) add(GalleryChip(GalleryFilter.Tag(tag), tagNames[tag].orEmpty(), n))
        }
    }

    private fun matches(filter: GalleryFilter, ownerKind: GalleryOwnerKind, item: GalleryItem): Boolean = when (filter) {
        GalleryFilter.All -> true
        GalleryFilter.Project -> ownerKind == GalleryOwnerKind.PROJECT
        GalleryFilter.Characters -> ownerKind == GalleryOwnerKind.CHARACTER
        is GalleryFilter.Tag -> filter.tagKey in item.tagKeys
    }

    /** Sorting never changes storage (T-GAL-12): `직접 순서` = as masked, `넣은 날` newest first, `씬 순서` by manuscript. */
    fun sortItems(items: List<GalleryItem>, sort: GallerySort, labels: EntryLabels): List<GalleryItem> = when (sort) {
        GallerySort.MANUAL -> items
        GallerySort.ADDED -> items.withIndex().sortedWith(compareByDescending<IndexedValue<GalleryItem>> { it.value.createdAt }.thenBy { it.index }).map { it.value }
        GallerySort.SCENE -> {
            val (linked, unlinked) = items.partition { labels.scenePosition(it.sceneKey) != null }
            linked.withIndex().sortedWith(compareBy<IndexedValue<GalleryItem>> { labels.scenePosition(it.value.sceneKey) }.thenBy { it.index }).map { it.value } + unlinked
        }
    }
}

/**
 * Per-item labels shared by the rows and the panel: main-image chips (derived from `ImageUse`, GAL-14), the
 * scene link state (GAL-08), the meta line and scene positions for `씬 순서`.
 */
class EntryLabels(private val doc: ProjectDoc, private val locked: Boolean) {
    private val codes = UnitCodes(doc)
    private val hiddenScenes: Set<Key> by lazy { ItemSurfaces(doc).hidden.scenes }
    private val scenePositions: Map<Key, Int> by lazy { doc.index.allScenesInOrder.withIndex().associate { (i, s) -> s.key to i } }
    private val uses by lazy { doc.assets.imageUses.filter { doc.liveness.isLive(it) } }

    /** Where a linked scene sits in the manuscript, or null when it counts as unlinked (not live, or hidden while locked). */
    fun scenePosition(sceneKey: Key?): Int? = when (sceneState(sceneKey)) {
        SceneState.LIVE -> scenePositions[sceneKey]
        else -> null
    }

    enum class SceneState { NONE, LIVE, HIDDEN, DELETED }

    fun sceneState(sceneKey: Key?): SceneState = when {
        sceneKey == null -> SceneState.NONE
        !doc.liveness.isUnitLive(sceneKey) -> SceneState.DELETED
        locked && sceneKey in hiddenScenes -> SceneState.HIDDEN
        else -> SceneState.LIVE
    }

    /** `1-7 씬 3` of a live scene, or the code it would have (trashed), or the purged name tag. */
    fun sceneCode(sceneKey: Key): String? = doc.index.unit(sceneKey)?.let { codes.sceneRef(it) } ?: doc.index.nameTag(sceneKey)?.name

    /** `초상 · 홀로`: the slots whose `ImageUse.source` equals this item's source, then holo (GAL-07, GAL-14). */
    fun chip(item: GalleryItem): String? {
        if (item.source.isEmpty()) return GalleryCardRules.chip(false, false, false, item.holo)
        val mine = uses.filter { it.ownerKey == item.ownerKey && it.source == item.source }
        fun has(slot: ImageSlot) = mine.any { it.slot.known == slot }
        return when (item.ownerKind.known) {
            GalleryOwnerKind.PROJECT -> GalleryCardRules.chip(false, false, has(ImageSlot.PROJECT_COVER), item.holo)
            else -> GalleryCardRules.chip(has(ImageSlot.CHARACTER_PORTRAIT), has(ImageSlot.CHARACTER_BACKGROUND), false, item.holo)
        }
    }

    /** Card and list meta line: the live scene's code, else the tags joined with ` · `; null when neither. */
    fun meta(item: GalleryItem, tagNames: Map<Key, String>): String? {
        if (sceneState(item.sceneKey) == SceneState.LIVE) item.sceneKey?.let(::sceneCode)?.let { return it }
        return item.tagKeys.mapNotNull { tagNames[it] }.joinToString(GalleryUiCopy.SEP).ifEmpty { null }
    }
}

/**
 * Card sizes of the `카드` rows (GAL-07, GAL-10): an ungrouped gallery uses the 222 × 296 card (inner 236 × 315);
 * grouped, the project row uses 162 × 216 and character rows 132 × 176 (GAL-10 [PROPOSED]).
 */
object GalleryCardSizes {
    val PROJECT_GROUP = CardSize(162f, 216f)
    val CHARACTER_GROUP = CardSize(132f, 176f)

    fun of(section: GallerySection, grouped: Boolean, inner: Boolean): CardSize = when {
        !grouped -> GalleryCardRules.size(inner)
        section.ownerLink == null -> PROJECT_GROUP
        else -> CHARACTER_GROUP
    }
}
