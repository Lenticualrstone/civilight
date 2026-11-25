package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.trash.NameTagState

/** What a key reference can point at. */
enum class RefTargetKind { CHARACTER, PLACE, TERM, FACTION, PAGE, UNIT, EVENT, MAP, REGION }

/** The five cases of §3, checked in this order. */
enum class RefState {
    /** Hidden and its gate locked: never the name. Hiding wins over deletion. */
    HIDDEN_LOCKED,
    LIVE,

    /** In the trash: dotted + `◇`, `지운 <kind> · <name>`, fix paths incl. `되살리기`. */
    TRASHED,

    /** Only a name tag left: `영구 삭제 · <name>` (purged) or `지운 <kind> · <name>` without `되살리기` (not exported). */
    TAGGED,

    /** Neither row nor tag: dotted, copy `TODO_COPY_DM_01`; never crashes, never `화자 없음`. */
    MISSING,
}

/** Rule-19 gates relevant to one project. */
data class HidingLocks(
    /** The project's hidden-item gate is closed (LIB-36). */
    val itemsLocked: Boolean = true,
    /** The project is hidden and its group gate is closed (references from outside, e.g. the recorder). */
    val projectLocked: Boolean = false,
)

/**
 * A resolved reference. [name] is the real name when it may be shown; [label] is the wide-place
 * label (`지운 인물 · 노아`, `영구 삭제 · 노아`, `숨긴 인물` …), null for [RefState.MISSING] (no copy
 * yet); narrow places show [name] with a dotted underline and `◇` when [dotted].
 */
data class ResolvedRef(
    val kind: RefTargetKind,
    val key: Key,
    val state: RefState,
    val name: String?,
    val label: String?,
    val dotted: Boolean,
    /** `되살리기` is offered (only for trashed rows). */
    val canRestore: Boolean,
    /** Speaker part of voice IDs, from the row or the name tag (TC-20-21, rule 20 §2). */
    val voiceCode: String?,
    /** Name for exports (JSON/CSV keep the speaker name of deleted and hidden speakers). */
    val exportName: String?,
)

/** Reference resolution with deleted and hidden states (§3, rules 19 and 20 as data). */
object Resolution {
    fun resolve(doc: ProjectDoc, kind: RefTargetKind, key: Key, locks: HidingLocks = HidingLocks()): ResolvedRef {
        val row = rowInfo(doc, kind, key)
        val tag = if (kind.hasNameTags) doc.index.nameTag(key) else null
        val itemKind = kind.itemKind
        val exportName = row?.name ?: tag?.name
        val voiceCode = row?.voiceCode ?: tag?.voiceCode
        fun result(state: RefState, name: String?, label: String?, dotted: Boolean, canRestore: Boolean = false) =
            ResolvedRef(kind, key, state, name, label, dotted, canRestore, voiceCode, exportName)

        // 1. Hiding wins (checked first, also for trashed and name-tagged targets).
        if (locks.projectLocked && doc.project.protections.hidden) {
            return result(RefState.HIDDEN_LOCKED, null, TrashCopy.HIDDEN_PROJECT, dotted = false)
        }
        if (locks.itemsLocked) {
            if (row != null && row.hidden) return result(RefState.HIDDEN_LOCKED, null, hiddenLabel(kind), dotted = false)
            if (row == null && tag != null && tag.hidden) {
                val label = if (tag.state.known == NameTagState.PURGED) TrashCopy.purgedHiddenLabel else hiddenLabel(kind)
                return result(RefState.HIDDEN_LOCKED, null, label, dotted = true)
            }
        }
        // 2. Live.
        if (row != null && row.live) return result(RefState.LIVE, row.name, row.name, dotted = false)
        // 3. In the trash.
        if (row != null) {
            return result(RefState.TRASHED, row.name, TrashCopy.deletedLabel(itemKind, row.name), dotted = true, canRestore = true)
        }
        // 4. Name tag.
        if (tag != null) {
            val label = when (tag.state.known) {
                NameTagState.NOT_EXPORTED -> TrashCopy.deletedLabel(itemKind, tag.name)
                else -> TrashCopy.purgedLabel(tag.name)
            }
            return result(RefState.TAGGED, tag.name, label, dotted = true)
        }
        // 5. Missing.
        return result(RefState.MISSING, null, null, dotted = true)
    }

    /** A link run's target. */
    fun resolve(doc: ProjectDoc, target: LinkTarget, locks: HidingLocks = HidingLocks()): ResolvedRef? {
        val kind = when (target.kind.known) {
            LinkTargetKind.CHARACTER -> RefTargetKind.CHARACTER
            LinkTargetKind.TERM -> RefTargetKind.TERM
            LinkTargetKind.PLACE -> RefTargetKind.PLACE
            LinkTargetKind.FACTION -> RefTargetKind.FACTION
            LinkTargetKind.PAGE -> RefTargetKind.PAGE
            LinkTargetKind.UNIT -> RefTargetKind.UNIT
            LinkTargetKind.EVENT -> RefTargetKind.EVENT
            LinkTargetKind.PROJECT, null -> return null
        }
        return resolve(doc, kind, target.key, locks)
    }

    /** A scene place (`장소` or `지역`). */
    fun resolve(doc: ProjectDoc, place: PlaceRef, locks: HidingLocks = HidingLocks()): ResolvedRef? = when (place.kind.known) {
        PlaceRefKind.PLACE -> resolve(doc, RefTargetKind.PLACE, place.key, locks)
        PlaceRefKind.REGION -> resolve(doc, RefTargetKind.REGION, place.key, locks)
        null -> null
    }

    /**
     * The speaker part of voice IDs for a dialogue speaker: from the row, else the name tag; never
     * cleared or swapped for another code while the speaker is in the trash or purged (TC-20-21).
     */
    fun voiceCodeOf(doc: ProjectDoc, characterKey: Key): String? =
        doc.index.character(characterKey)?.voiceCode ?: doc.index.nameTag(characterKey)?.voiceCode

    private data class RowInfo(val name: String, val live: Boolean, val hidden: Boolean, val voiceCode: String?)

    private fun rowInfo(doc: ProjectDoc, kind: RefTargetKind, key: Key): RowInfo? {
        val live = doc.liveness
        return when (kind) {
            RefTargetKind.CHARACTER -> doc.index.character(key)?.let { RowInfo(it.name, live.isCharacterLive(key), it.hidden, it.voiceCode) }
            RefTargetKind.PLACE -> doc.index.place(key)?.let { RowInfo(it.name, live.isPlaceLive(key), it.hidden, null) }
            RefTargetKind.TERM -> doc.index.term(key)?.let { RowInfo(it.name, live.isTermLive(key), it.hidden, null) }
            RefTargetKind.FACTION -> doc.index.faction(key)?.let { RowInfo(it.name, live.isFactionLive(key), it.hidden, null) }
            RefTargetKind.PAGE -> doc.pages[key]?.let { RowInfo(it.name, live.isPageLive(key), it.hidden, null) }
            RefTargetKind.UNIT -> doc.index.unit(key)?.let {
                RowInfo(UnitCodes(doc).codeAndTitle(key).orEmpty(), live.isUnitLive(key), it.hidden, null)
            }
            RefTargetKind.EVENT -> doc.index.event(key)?.let { RowInfo(it.title, live.isEventLive(key), false, null) }
            RefTargetKind.MAP -> doc.maps[key]?.let { RowInfo(it.name, live.isMapLive(key), false, null) }
            RefTargetKind.REGION -> doc.index.region(key)?.let { (map, region) -> RowInfo(region.name, live.isMapLive(map.key), false, null) }
        }
    }

    private fun hiddenLabel(kind: RefTargetKind): String =
        if (kind == RefTargetKind.CHARACTER) TrashCopy.hiddenKind(ItemKind.CHARACTER) else TrashCopy.HIDDEN_ITEM

    private val RefTargetKind.hasNameTags: Boolean
        get() = this == RefTargetKind.CHARACTER || this == RefTargetKind.PLACE || this == RefTargetKind.TERM ||
            this == RefTargetKind.FACTION || this == RefTargetKind.PAGE || this == RefTargetKind.UNIT

    private val RefTargetKind.itemKind: ItemKind
        get() = when (this) {
            RefTargetKind.CHARACTER -> ItemKind.CHARACTER
            RefTargetKind.PLACE, RefTargetKind.REGION -> ItemKind.PLACE
            RefTargetKind.TERM -> ItemKind.TERM
            RefTargetKind.FACTION -> ItemKind.FACTION
            RefTargetKind.PAGE -> ItemKind.PAGE
            RefTargetKind.UNIT -> ItemKind.UNIT
            RefTargetKind.EVENT -> ItemKind.EVENT
            RefTargetKind.MAP -> ItemKind.MAP
        }
}
