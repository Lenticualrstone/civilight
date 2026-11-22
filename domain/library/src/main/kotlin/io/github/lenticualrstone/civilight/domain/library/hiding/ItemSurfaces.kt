package io.github.lenticualrstone.civilight.domain.library.hiding

import io.github.lenticualrstone.civilight.domain.library.common.UnitCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.EntityRef
import io.github.lenticualrstone.civilight.model.cast.Relation
import io.github.lenticualrstone.civilight.model.cast.RelationEndKind
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.gallery.GalleryItem
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.model.map.MapRegion
import io.github.lenticualrstone.civilight.model.map.Pin
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun

/** The one locked row that stands for hidden items in a list (`숨긴 인물 1` · `지문 · PIN으로 열림` · `지문으로 보기`). */
data class LockedItemsRow(val kind: HideableKind, val count: Int) {
    val title: String get() = "숨긴 ${kind.word} $count"
    val sub: String get() = "지문 · PIN으로 열림"
    val action: String get() = "지문으로 보기"

    /** One-line form used at the end of pickers: `숨긴 인물 1 · 지문으로 보기`. */
    val inline: String get() = "$title · $action"
}

/** A list after masking: visible items (with `숨김` tags while open) plus the locked row. */
data class MaskedList<T>(val items: List<T>, val taggedHidden: Set<Key>, val lockedRow: LockedItemsRow?)

/** A scene in 스토리 구조 (I-02). */
data class StructureSceneLabel(val text: String, val title: String?, val hiddenTag: Boolean)

/** A scene in the script: its blocks, or one locked line (I-03). */
sealed interface ScriptSceneView {
    data class Blocks(val blocks: List<Block>) : ScriptSceneView

    data class LockedLine(val blockCount: Int) : ScriptSceneView {
        val text: String get() = "숨긴 씬 · 블록 $blockCount · 지문으로 보기"
    }
}

/** ◇ `숨긴 인물이 보이는 씬에 나옴 · 씬 3` (I-16): reported, never fixed automatically. */
data class HiddenSpeakerFinding(val characterKey: Key, val sceneKey: Key, val sceneLabel: String) {
    val text: String get() = "숨긴 인물이 보이는 씬에 나옴 · $sceneLabel"
    val actions: List<String> get() = listOf("씬도 숨기기", "그대로")
}

/**
 * The item-hiding masking contract (LIB-36, R19 §6), `itemSurface(surface, locked)` per surface.
 * Modules render; this decides what is shown. [locked] = the project's hidden items are locked.
 * App-drawn names and links are masked; text the author typed stays as typed.
 */
class ItemSurfaces(private val doc: ProjectDoc) {
    val hidden = HiddenItems(doc)
    private val index = doc.index
    private val liveness = doc.liveness
    private val codes = UnitCodes(doc)

    /** Generic list: hidden items collapse into one locked row while locked; tagged `숨김` while open. */
    fun <T> list(items: List<T>, kind: HideableKind, keyOf: (T) -> Key, locked: Boolean): MaskedList<T> {
        val hiddenKeys = hidden.of(kind)
        val (hiddenItems, visible) = items.partition { keyOf(it) in hiddenKeys }
        return if (locked) {
            MaskedList(visible, emptySet(), if (hiddenItems.isEmpty()) null else LockedItemsRow(kind, hiddenItems.size))
        } else {
            MaskedList(items, hiddenItems.map(keyOf).toSet(), null)
        }
    }

    /** I-01: live characters in manual order. */
    fun castList(locked: Boolean): MaskedList<Character> =
        list(doc.cast.characters.filter { it.deletedAt == null }.sortedBy { it.order }, HideableKind.CHARACTER, { it.key }, locked)

    /** I-02: `1-7 씬 4 · 숨긴 씬` with the title masked, or the title and a `숨김` tag. */
    fun structureScene(sceneKey: Key, locked: Boolean): StructureSceneLabel {
        val scene = requireNotNull(index.unit(sceneKey)) { "no scene $sceneKey" }
        val label = codes.sceneLabel(sceneKey).orEmpty()
        val isHidden = hidden.isHidden(HideableKind.SCENE, sceneKey)
        return when {
            isHidden && locked -> StructureSceneLabel("$label · 숨긴 씬", null, false)
            else -> StructureSceneLabel(label, scene.title, isHidden)
        }
    }

    /** I-03: a hidden scene shows one line instead of its blocks while locked. */
    fun scriptScene(sceneKey: Key, locked: Boolean): ScriptSceneView {
        val blocks = doc.manuscript.liveBlocksOf(sceneKey)
        return if (locked && hidden.isHidden(HideableKind.SCENE, sceneKey)) ScriptSceneView.LockedLine(blocks.size) else ScriptSceneView.Blocks(blocks)
    }

    /** I-04: the speaker cell; the line text itself is never changed. */
    fun speakerLabel(characterKey: Key, locked: Boolean): String? {
        val c = index.character(characterKey) ?: return null
        return if (locked && c.key in hidden.characters) HIDDEN_CHARACTER else c.name
    }

    /** I-05, I-13: `@` links to hidden items show `숨긴 항목`; typed text is unchanged. */
    fun displayRuns(runs: List<TextRun>, locked: Boolean): List<TextRun> {
        if (!locked) return runs
        return runs.map { run ->
            if (run is TextRun.Link && hidden.isLinkTargetHidden(run.target)) run.copy(text = HIDDEN_ITEM) else run
        }
    }

    /** I-13: a recorder link to an item of this project. */
    fun recorderTargetLabel(name: String, isHidden: Boolean, locked: Boolean): String = if (isHidden && locked) HIDDEN_ITEM else name

    /** I-06, I-17: speaker picker / `@` candidates; locked hidden characters only via the last row. */
    fun speakerPicker(locked: Boolean, query: String = ""): MaskedList<Character> {
        val candidates = doc.cast.characters
            .filter { it.deletedAt == null && (query.isEmpty() || it.name.contains(query)) }
            .sortedBy { it.order }
        return list(candidates, HideableKind.CHARACTER, { it.key }, locked)
    }

    /**
     * I-07: what search may return. Card results exclude hidden items while locked; the text of
     * visible scenes stays searchable (a typed name in a visible scene is still found). Footer
     * `숨긴 항목 제외` while something is excluded.
     */
    fun searchScope(locked: Boolean): ItemSearchScope {
        val cards = buildSet {
            doc.cast.characters.filter { it.deletedAt == null }.forEach { add(it.key) }
            doc.world.places.filter { it.deletedAt == null }.forEach { add(it.key) }
            doc.world.terms.filter { it.deletedAt == null }.forEach { add(it.key) }
            doc.world.factions.filter { it.deletedAt == null }.forEach { add(it.key) }
            doc.pages.values.filter { it.deletedAt == null }.forEach { add(it.key) }
        }
        val scenes = doc.manuscript.liveScenes.map { it.key }
        val liveGallery = doc.galleryItems.filter { liveness.isLive(it) }
        if (!locked) return ItemSearchScope(cards, scenes.toSet(), null, liveGallery.mapTo(HashSet()) { it.key })
        val hiddenCards = hidden.characters + hidden.places + hidden.terms + hidden.factions + hidden.pages
        val visibleScenes = scenes.filterNot { it in hidden.scenes }.toSet()
        val visibleGallery = liveGallery.filter { isGalleryItemVisible(it, locked = true) }.mapTo(HashSet()) { it.key }
        val excluded = hiddenCards.isNotEmpty() || hidden.scenes.isNotEmpty() || visibleGallery.size < liveGallery.size
        return ItemSearchScope(cards - hiddenCards, visibleScenes, if (excluded) FOOTER_HIDDEN_ITEMS else null, visibleGallery)
    }

    /** I-08: other characters' relations; relations with a hidden character collapse into one row. */
    fun relationsOf(characterKey: Key, locked: Boolean): MaskedRelations {
        val me = EntityRef(RelationEndKind.CHARACTER, characterKey)
        val relations = doc.cast.relations.filter { liveness.isLive(it) && it.involves(me) }
        if (!locked) return MaskedRelations(relations, 0)
        val (hiddenOnes, visible) = relations.partition { r -> r.otherEnd(me)?.let(hidden::isEndHidden) == true }
        return MaskedRelations(visible, hiddenOnes.size)
    }

    /** I-09: relationship map nodes drawn (hidden characters not drawn while locked). */
    fun mapNodes(characterKeys: List<Key>, locked: Boolean): List<Key> =
        if (locked) characterKeys.filterNot { it in hidden.characters } else characterKeys

    /** I-10: `◆` records of hidden items are grouped while locked. */
    fun lensRecords(records: List<ChangeRecord>, locked: Boolean): MaskedRecords {
        if (!locked) return MaskedRecords(records, 0)
        val (hiddenOnes, visible) = records.partition(hidden::isRecordHidden)
        return MaskedRecords(visible, hiddenOnes.size)
    }

    /** I-12: a character's route is drawn only when the character is visible. */
    fun hasRoute(characterKey: Key, locked: Boolean): Boolean = !(locked && characterKey in hidden.characters)

    /** I-12: a stretch through a hidden scene is drawn dashed with this label while locked. */
    fun routeStretchLabel(sceneKey: Key, locked: Boolean): String? =
        if (locked && sceneKey in hidden.scenes) HIDDEN_SCENE else null

    // --- Gallery (55 GAL-19, GAL-10) ---

    /**
     * True when [item] may appear on a surface: live, not a hidden image while [locked], and not
     * owned by a hidden character while [locked].
     */
    fun isGalleryItemVisible(item: GalleryItem, locked: Boolean): Boolean {
        if (!liveness.isLive(item)) return false
        if (!locked) return true
        if (item.key in hidden.galleryImages) return false
        return !(item.ownerKind.known == GalleryOwnerKind.CHARACTER && item.ownerKey in hidden.characters)
    }

    /**
     * One gallery in `직접 순서`: live items; while [locked] hidden images collapse into the locked
     * tile or row (`숨긴 이미지 <H>`), while open they are tagged `숨김` (GAL-19).
     */
    fun gallery(ownerKey: Key, locked: Boolean): MaskedList<GalleryItem> =
        list(index.galleryItemsOf(ownerKey).filter { liveness.isLive(it) }, HideableKind.GALLERY_IMAGE, { it.key }, locked)

    /**
     * The project gallery groups (GAL-10): the project's own items first, then every visible
     * character with items in cast order. While [locked], hidden characters with items collapse into
     * one row `숨긴 인물 <H>` (no image count).
     */
    fun projectGallery(locked: Boolean): ProjectGalleryGroups {
        val own = gallery(doc.key, locked)
        val characters = doc.cast.characters.filter { it.deletedAt == null }.sortedBy { it.order }
        val groups = ArrayList<CharacterGalleryGroup>()
        var hiddenWithItems = 0
        for (c in characters) {
            val items = gallery(c.key, locked)
            if (items.items.isEmpty() && items.lockedRow == null) continue
            if (locked && c.key in hidden.characters) {
                hiddenWithItems++
                continue
            }
            groups += CharacterGalleryGroup(c, items)
        }
        return ProjectGalleryGroups(own, groups, if (hiddenWithItems > 0) LockedItemsRow(HideableKind.CHARACTER, hiddenWithItems) else null)
    }

    /** I-18: pins of hidden places are not drawn while locked. */
    fun visiblePins(map: WorldMap, locked: Boolean): List<Pin> =
        if (locked) map.pins.filterNot { it.placeKey in hidden.places } else map.pins

    /** I-18: regions of hidden places or factions are not drawn while locked. */
    fun visibleRegions(map: WorldMap, locked: Boolean): List<MapRegion> = if (!locked) map.regions else map.regions.filterNot { r ->
        r.placeKey?.let { it in hidden.places } == true || r.factionKey?.let { it in hidden.factions } == true
    }

    /**
     * I-16: a hidden character speaking in a visible live scene. The app never hides the scene; the
     * check offers `씬도 숨기기` · `그대로`.
     */
    fun hiddenSpeakersInVisibleScenes(): List<HiddenSpeakerFinding> = doc.manuscript.liveScenes
        .filterNot { it.key in hidden.scenes }
        .flatMap { scene ->
            doc.manuscript.liveBlocksOf(scene.key)
                .flatMap { b -> listOfNotNull(b.speakerKey.takeIf { b.kind.known == BlockKind.DIALOGUE }) + b.options.mapNotNull { it.speakerKey } }
                .filter { it in hidden.characters }
                .distinct()
                .map { HiddenSpeakerFinding(it, scene.key, codes.sceneOrdinalLabel(scene.key).orEmpty()) }
        }

    companion object {
        const val HIDDEN_CHARACTER = "숨긴 인물"
        const val HIDDEN_ITEM = "숨긴 항목"
        const val HIDDEN_SCENE = "숨긴 씬"
        const val HIDDEN_TAG = "숨김"
        const val FOOTER_HIDDEN_ITEMS = "숨긴 항목 제외"

        /** I-09: `12명 중 5명 · 숨김 1`. */
        fun relationMapSummary(total: Int, shown: Int, hiddenCount: Int): String =
            if (hiddenCount > 0) "${total}명 중 ${shown}명 · 숨김 $hiddenCount" else "${total}명 중 ${shown}명"

        /** I-11: checks are still computed; their results collapse into this row while locked. */
        fun lockedChecksRow(count: Int): String = "▲ 숨긴 항목 관련 $count · 지문으로 보기"
    }
}

/** Search scope after item masking (I-07); [galleryItemKeys] = gallery images search may return (GAL-04). */
data class ItemSearchScope(
    val cardKeys: Set<Key>,
    val sceneKeys: Set<Key>,
    val footer: String?,
    val galleryItemKeys: Set<Key> = emptySet(),
)

/** One character's group in the project gallery (GAL-10): header `<name> · <N>`. */
data class CharacterGalleryGroup(val character: Character, val items: MaskedList<GalleryItem>)

/** The project gallery after masking (GAL-10): own items, character groups, the hidden characters' row. */
data class ProjectGalleryGroups(
    val project: MaskedList<GalleryItem>,
    val characters: List<CharacterGalleryGroup>,
    val hiddenCharacters: LockedItemsRow?,
)

/** Relations after masking (I-08). */
data class MaskedRelations(val visible: List<Relation>, val hiddenCount: Int) {
    /** `숨긴 인물과의 관계 1`, or null. */
    val lockedRow: String? get() = if (hiddenCount > 0) "숨긴 인물과의 관계 $hiddenCount" else null
}

/** Lens `◆` records after masking (I-10). */
data class MaskedRecords(val visible: List<ChangeRecord>, val hiddenCount: Int) {
    /** `숨긴 항목 변화 2`, or null. */
    val lockedRow: String? get() = if (hiddenCount > 0) "숨긴 항목 변화 $hiddenCount" else null
}
