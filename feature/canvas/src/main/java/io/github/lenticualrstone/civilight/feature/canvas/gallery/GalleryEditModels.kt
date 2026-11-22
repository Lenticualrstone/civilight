package io.github.lenticualrstone.civilight.feature.canvas.gallery

import io.github.lenticualrstone.civilight.domain.actions.GalleryActions
import io.github.lenticualrstone.civilight.domain.actions.GalleryOwner
import io.github.lenticualrstone.civilight.domain.actions.GallerySourceMark
import io.github.lenticualrstone.civilight.domain.library.hiding.HiddenItems
import io.github.lenticualrstone.civilight.domain.library.hiding.ItemSurfaces
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageOwner
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageOwners
import io.github.lenticualrstone.civilight.feature.canvas.image.PresetImage
import io.github.lenticualrstone.civilight.feature.canvas.image.PresetImages
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.gallery.GalleryItem
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind

// ---------------------------------------------------------------- 내 이미지 while locked (GAL-19, §3.4)

/**
 * `내 이미지` with hidden sources masked (55 §3.4 last row, T-GAL-20): while [locked], an original that only
 * hidden things use (hidden gallery images, images of hidden characters, places, terms, factions or pages) is
 * left out, so the picker never shows a hidden image. An original nothing uses yet stays listed.
 */
object MyImages {
    fun visible(doc: ProjectDoc, locked: Boolean): List<Blob> {
        val all = ImageOwners.myImages(doc)
        if (!locked) return all
        val masked = maskedHashes(doc)
        return all.filter { it.hash !in masked }
    }

    /** Hashes held only by hidden rows (while locked). */
    fun maskedHashes(doc: ProjectDoc): Set<String> {
        val hidden = HiddenItems(doc)
        val visibleHolders = HashSet<String>()
        val hiddenHolders = HashSet<String>()
        for (item in doc.galleryItems) {
            val hash = item.blobHash ?: continue
            val isHidden = item.hidden || (item.ownerKind.known == GalleryOwnerKind.CHARACTER && item.ownerKey in hidden.characters)
            (if (isHidden) hiddenHolders else visibleHolders) += hash
        }
        for (use in doc.assets.imageUses) {
            val hash = use.blobHash ?: continue
            val ownerHidden = when (use.ownerKind.known) {
                ImageOwnerKind.CHARACTER -> use.ownerKey in hidden.characters
                ImageOwnerKind.PLACE -> use.ownerKey in hidden.places
                ImageOwnerKind.TERM -> use.ownerKey in hidden.terms
                ImageOwnerKind.FACTION -> use.ownerKey in hidden.factions
                ImageOwnerKind.PAGE -> use.ownerKey in hidden.pages
                else -> false
            }
            (if (ownerHidden) hiddenHolders else visibleHolders) += hash
        }
        return hiddenHolders - visibleHolders
    }
}

// ---------------------------------------------------------------- 이미지 넣기 (GAL-11)

/** One tile of `갤러리에 넣기`: [caption] is the original's size (`4.2 MB`) or the preset name. */
data class AddTile(
    val source: String,
    val caption: String,
    val preset: PresetImage?,
    val blob: Blob?,
    /** `이미 있음` · `휴지통에 있음`: dimmed and not choosable. */
    val mark: GallerySourceMark,
    /** 1-based badge = the order it will be added in; null when not chosen. */
    val number: Int?,
) {
    val choosable: Boolean get() = mark == GallerySourceMark.NONE
}

data class AddModel(
    val title: String,
    val subtitle: String,
    val tabs: List<String>,
    val presets: List<AddTile>,
    val mine: List<AddTile>,
    val chosen: List<String>,
) {
    val count: Int get() = chosen.size
    val button: String get() = GalleryUiCopy.addButton(count)
    val canAdd: Boolean get() = count > 0
}

/**
 * The add sheet (GAL-11, T-GAL-09, T-GAL-20, T-GAL-21): tabs `기본 이미지 8` · `내 이미지 <N>`, tiles toggled
 * in a numbered order, marks from `GalleryActions.sourceMark` (a source only a hidden item holds while locked
 * is never marked), `내 이미지` masked while locked.
 */
object GalleryAddModel {
    const val TAB_PRESETS = 0
    const val TAB_MINE = 1

    fun build(
        doc: ProjectDoc,
        owner: GalleryOwner,
        locked: Boolean,
        chosen: List<String>,
        fileExists: (String) -> Boolean = { true },
    ): AddModel {
        fun mark(source: String) = GalleryActions.sourceMark(doc, owner, source, locked)
        val live = chosen.filter { mark(it) == GallerySourceMark.NONE }
        fun number(source: String) = live.indexOf(source).takeIf { it >= 0 }?.plus(1)
        val presets = PresetImages.all.map { AddTile(it.source, it.name, it, null, mark(it.source), number(it.source)) }
        val mine = MyImages.visible(doc, locked).filter { fileExists(it.hash) }.map { b ->
            AddTile(b.hash, GalleryFacts.size(b.bytes), null, b, mark(b.hash), number(b.hash))
        }
        val ownerName = GalleryRows.ownerName(doc, owner.kind, owner.key)
        return AddModel(
            title = GalleryUiCopy.ADD_TITLE,
            subtitle = GalleryUiCopy.addSubtitle(ownerName),
            tabs = listOf(GalleryUiCopy.chip(GalleryUiCopy.PRESET, presets.size), GalleryUiCopy.chip(io.github.lenticualrstone.civilight.feature.canvas.image.ImageCopy.TAB_MINE, mine.size)),
            presets = presets,
            mine = mine,
            chosen = live,
        )
    }

    /** Tap on a tile: a marked tile does nothing; else it joins the end of the order, or leaves it. */
    fun toggle(chosen: List<String>, tile: AddTile): List<String> = when {
        !tile.choosable -> chosen
        tile.source in chosen -> chosen - tile.source
        else -> chosen + tile.source
    }

    /** The sources of `넣기 · <k>` in badge order, with the blob row for originals. */
    fun sources(doc: ProjectDoc, chosen: List<String>): List<io.github.lenticualrstone.civilight.domain.actions.GalleryAddSource> =
        chosen.map { s -> io.github.lenticualrstone.civilight.domain.actions.GalleryAddSource(s, doc.index.blob(s)) }
}

// ---------------------------------------------------------------- 메뉴 (GAL-14 … GAL-19)

/** The three main-image targets (GAL-14). */
enum class MainTarget(
    val slot: ImageSlot,
    val label: String,
    val sub: String,
    val now: String,
    val shownIn: String,
) {
    PORTRAIT(ImageSlot.CHARACTER_PORTRAIT, GalleryUiCopy.TO_PORTRAIT, GalleryUiCopy.PORTRAIT_SUB, GalleryUiCopy.NOW_PORTRAIT, GalleryUiCopy.SHOWN_IN_PORTRAIT),
    BACKGROUND(ImageSlot.CHARACTER_BACKGROUND, GalleryUiCopy.TO_BACKGROUND, GalleryUiCopy.BACKGROUND_SUB, GalleryUiCopy.NOW_BACKGROUND, GalleryUiCopy.SHOWN_IN_BACKGROUND),
    COVER(ImageSlot.PROJECT_COVER, GalleryUiCopy.TO_COVER, GalleryUiCopy.COVER_SUB, GalleryUiCopy.NOW_COVER, GalleryUiCopy.SHOWN_IN_COVER),
    ;

    /** The image slot this target writes. */
    fun owner(item: GalleryItem): ImageOwner = ImageOwner(
        if (this == COVER) ImageOwnerKind.PROJECT else ImageOwnerKind.CHARACTER,
        item.ownerKey,
        slot,
    )

    companion object {
        fun of(kind: GalleryOwnerKind): List<MainTarget> =
            if (kind == GalleryOwnerKind.PROJECT) listOf(COVER) else listOf(PORTRAIT, BACKGROUND)
    }
}

/** A main-image row: [current] = the slot already shows this source (`지금 초상` instead of the chevron). */
data class MainRow(val target: MainTarget, val sub: String, val current: Boolean)

/** What `초상으로` · `배경으로` · `표지로` does (GAL-14). Nothing is written before the crop editor's `적용`. */
sealed interface MainPlan {
    /** A preset goes in at once (presets are not cropped, Q-IM-03); one undo step. */
    data class PlacePreset(val owner: ImageOwner, val presetId: String, val holo: Boolean) : MainPlan

    /** An original opens the crop editor for the slot's ratios; `취소` there cancels the whole action (T-GAL-08). */
    data class Crop(val owner: ImageOwner, val blobHash: String, val holo: Boolean, val note: String?) : MainPlan
}

/** The `순서` row (GAL-15, T-GAL-24): text and which buttons work. */
data class OrderRowModel(
    val text: String,
    val editable: Boolean,
    val canFirst: Boolean,
    val canForward: Boolean,
    val canBackward: Boolean,
    val canLast: Boolean,
)

/** The `⋯` menu of one item (GAL-14, board `GalMore`). */
data class MenuModel(
    val itemKey: Key,
    val title: String,
    val untitled: Boolean,
    val small: String,
    val main: List<MainRow>,
    val cropEnabled: Boolean,
    val cropSub: String,
    val order: OrderRowModel,
    val holo: Boolean,
    val hidden: Boolean,
)

object GalleryMenus {
    /**
     * The menu of [itemKey] as the gallery screen shows it: [view] gives the position `<i>/<N>` and whether
     * order moves are allowed (`직접 순서` with `전체` only). Null when the item is not visible.
     */
    fun menu(doc: ProjectDoc, view: GalleryView, itemKey: Key, locked: Boolean): MenuModel? {
        val entry = view.entry(itemKey) ?: return null
        val item = entry.item
        val owner = GalleryOwner.of(item)
        val position = GalleryUiCopy.position(entry.position, view.count)
        val ownerName = GalleryRows.ownerName(doc, owner.kind, owner.key)
        return MenuModel(
            itemKey = item.key,
            title = entry.title ?: GalleryUiCopy.UNTITLED,
            untitled = entry.title == null,
            small = GalleryUiCopy.ownerPosition(ownerName, entry.position, view.count),
            main = mainRows(doc, item),
            cropEnabled = !item.isPreset,
            cropSub = if (item.isPreset) GalleryUiCopy.CROP_PRESET_SUB else GalleryUiCopy.CROP_SUB,
            order = orderRow(doc, view, item, position, locked),
            holo = item.holo,
            hidden = item.hidden,
        )
    }

    /** `초상으로` · `배경으로` (character) or `표지로` (project), with `지금 …` derived from `ImageUse` (T-GAL-07). */
    fun mainRows(doc: ProjectDoc, item: GalleryItem): List<MainRow> {
        val kind = item.ownerKind.known ?: GalleryOwnerKind.PROJECT
        return MainTarget.of(kind).map { t ->
            val owner = t.owner(item)
            val current = doc.assets.imageUses.any { owner.matches(it) && doc.liveness.isLive(it) && it.source == item.source && item.source.isNotEmpty() }
            MainRow(t, if (item.hidden) GalleryUiCopy.hiddenShownIn(t.shownIn) else t.sub, current)
        }
    }

    /** The plan of a main-image row (T-GAL-07, T-GAL-08, T-GAL-23). Hidden items carry the note for the crop editor. */
    fun mainPlan(item: GalleryItem, target: MainTarget): MainPlan {
        val owner = target.owner(item)
        val presetId = item.presetId
        return if (presetId != null) {
            MainPlan.PlacePreset(owner, presetId, item.holo)
        } else {
            MainPlan.Crop(owner, item.source, item.holo, if (item.hidden) GalleryUiCopy.hiddenShownIn(target.shownIn) else null)
        }
    }

    /**
     * The `순서` row (T-GAL-11, T-GAL-24): moves only in `직접 순서` with `전체`; otherwise disabled with the
     * reason (`넣은 날` · `씬 순서` · `전체에서만`). Ends are judged among the owner's visible items.
     */
    fun orderRow(doc: ProjectDoc, view: GalleryView, item: GalleryItem, position: String, locked: Boolean): OrderRowModel {
        val editable = view.orderEditable
        val why = when {
            view.sort == GallerySort.ADDED -> GalleryUiCopy.SORT_ADDED
            view.sort == GallerySort.SCENE -> GalleryUiCopy.SORT_SCENE
            view.filter != GalleryFilter.All -> GalleryUiCopy.ONLY_IN_ALL
            else -> GalleryUiCopy.SORT_MANUAL
        }
        val own = GalleryActions.visibleItems(doc, GalleryOwner.of(item), locked)
        val i = own.indexOfFirst { it.key == item.key }
        val first = i == 0
        val last = i == own.lastIndex
        return OrderRowModel(
            text = GalleryUiCopy.orderLine(position, why),
            editable = editable,
            canFirst = editable && i > 0 && !first,
            canForward = editable && i > 0,
            canBackward = editable && i >= 0 && !last,
            canLast = editable && i >= 0 && !last,
        )
    }
}

// ---------------------------------------------------------------- 이미지 정보 (GAL-13)

/** The info sheet's head (board `GalInfo`). */
data class InfoHead(val facts: String, val small: String)

object GalleryInfo {
    /** `3024 × 4032 · 4.2 MB · 사진` (preset `기본 이미지`, no size), `리엔 · 갤러리 3/8 · 원본 그대로`. */
    fun head(doc: ProjectDoc, item: GalleryItem, view: GalleryView?): InfoHead {
        val facts = if (item.isPreset) {
            GalleryUiCopy.PRESET
        } else {
            val blob = item.blobHash?.let(doc.index::blob)
            val source = when (blob?.source?.known) {
                BlobSource.GALLERY -> GalleryUiCopy.PHOTO
                BlobSource.FILE -> GalleryUiCopy.SOURCE_FILE
                else -> null
            }
            GalleryUiCopy.facts(blob?.width, blob?.height, blob?.bytes?.let(GalleryFacts::size), source)
        }
        val owner = GalleryOwner.of(item)
        val ownerName = GalleryRows.ownerName(doc, owner.kind, owner.key)
        val entry = view?.entry(item.key)
        val small = if (entry != null) GalleryUiCopy.ownerPosition(ownerName, entry.position, view.count, asIs = true)
        else listOf(GalleryUiCopy.ownerGallery(ownerName), GalleryUiCopy.AS_IS).joinToString(GalleryUiCopy.SEP)
        return InfoHead(facts, small)
    }

    /** Toggles [tagKey] on an item's tags (keeps the order the tags were added in). */
    fun toggleTag(tagKeys: List<Key>, tagKey: Key): List<Key> = if (tagKey in tagKeys) tagKeys - tagKey else tagKeys + tagKey
}

// ---------------------------------------------------------------- 순서 바꾸기 (GAL-15)

/** One row of `순서 바꾸기`: the owner's visible items in stored order. */
data class OrderScreenRow(val item: GalleryItem, val position: Int, val title: String?) {
    val key: Key get() = item.key
}

/** The order screen's own state: the picked row and the item being moved (`옮기기`). Saveable by keys. */
data class OrderState(val pickedKey: Key? = null, val movingKey: Key? = null) {
    val moving: Boolean get() = movingKey != null
}

object GalleryOrder {
    /** Rows of the owner of [ownerKey] in `직접 순서` (hidden items while locked left out, T-GAL-24). */
    fun rows(doc: ProjectDoc, ownerKey: Key, locked: Boolean): List<OrderScreenRow> =
        ItemSurfaces(doc).gallery(ownerKey, locked).items.mapIndexed { i, item ->
            OrderScreenRow(item, i + 1, item.title.trim().ifEmpty { null })
        }

    /** Tapping a row picks it; tapping the picked row again unpicks it. No effect while moving. */
    fun tap(state: OrderState, key: Key): OrderState = when {
        state.moving -> state
        state.pickedKey == key -> state.copy(pickedKey = null)
        else -> state.copy(pickedKey = key)
    }

    /** `옮기기`: the picked row becomes the moving one. */
    fun startMove(state: OrderState): OrderState = state.pickedKey?.let { state.copy(movingKey = it) } ?: state

    fun cancel(state: OrderState): OrderState = state.copy(movingKey = null)

    /** After `이 뒤로` (or `맨 앞으로`) the mode ends and the moved item stays picked. */
    fun placed(state: OrderState): OrderState = OrderState(pickedKey = state.movingKey ?: state.pickedKey, movingKey = null)

    /** Drops keys that are no longer rows (deleted, relocked). */
    fun reconcile(state: OrderState, rows: List<OrderScreenRow>): OrderState {
        val keys = rows.mapTo(HashSet()) { it.key }
        val picked = state.pickedKey?.takeIf { it in keys }
        val moving = state.movingKey?.takeIf { it in keys }
        return if (picked == state.pickedKey && moving == state.movingKey) state else OrderState(picked, moving)
    }
}

// ---------------------------------------------------------------- 여러 장 고르기 (GAL-18)

/** The `여러 장 고르기` mode: chosen keys in tap order. Leaving it never changes the picked item. */
data class MultiPick(val active: Boolean = false, val keys: List<Key> = emptyList()) {
    val count: Int get() = keys.size
    val title: String get() = GalleryUiCopy.pickedCount(count)

    fun toggle(key: Key): MultiPick = if (!active) this else copy(keys = if (key in keys) keys - key else keys + key)

    fun reconcile(visible: Collection<Key>): MultiPick = keys.filter { it in visible }.let { if (it == keys) this else copy(keys = it) }

    companion object {
        val OFF = MultiPick()
        fun start(): MultiPick = MultiPick(active = true)
    }
}

// ---------------------------------------------------------------- 다른 곳에도 넣기 (GAL-16)

/** A gallery the item can be copied into: the project or a visible character; [mark] for a single item. */
data class CopyTarget(val owner: GalleryOwner, val name: String, val sub: String?, val mark: GallerySourceMark) {
    val choosable: Boolean get() = mark == GallerySourceMark.NONE
}

object GalleryCopyTargets {
    /**
     * The project, then live characters in cast order (hidden ones while locked left out). With one item, an
     * owner that already holds its source in a visible item reads `이미 있음` (T-GAL-22); a hidden holder while
     * locked is not marked (GAL-16).
     */
    fun targets(doc: ProjectDoc, itemKeys: List<Key>, locked: Boolean, query: String = ""): List<CopyTarget> {
        val single = itemKeys.singleOrNull()?.let(doc.index::galleryItem)
        val surfaces = ItemSurfaces(doc)
        fun mark(owner: GalleryOwner) = single?.let { GalleryActions.sourceMark(doc, owner, it.source, locked) } ?: GallerySourceMark.NONE
        val project = GalleryOwner.project(doc)
        val rows = ArrayList<CopyTarget>()
        rows += CopyTarget(project, doc.project.name, GalleryUiCopy.PROJECT_WORD, mark(project))
        doc.characters
            .filter { doc.liveness.isCharacterLive(it.key) && !(locked && it.key in surfaces.hidden.characters) }
            .sortedWith(compareBy({ it.order }, { it.key }))
            .forEach { c ->
                val owner = GalleryOwner.character(c.key)
                rows += CopyTarget(owner, c.name, null, mark(owner))
            }
        val q = query.trim()
        return if (q.isEmpty()) rows else rows.filter { io.github.lenticualrstone.civilight.ui.search.KoreanSearch.matches(it.name, q) }
    }
}
