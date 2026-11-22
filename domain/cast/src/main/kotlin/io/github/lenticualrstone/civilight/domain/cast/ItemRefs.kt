package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.trash.NameTagKind
import io.github.lenticualrstone.civilight.model.trash.NameTagState

/** Display word of an item kind: `인물 · 장소 · 용어 · 세력`. */
val EntityKind.label: String
    get() = when (this) {
        EntityKind.CHARACTER -> "인물"
        EntityKind.PLACE -> "장소"
        EntityKind.TERM -> "용어"
        EntityKind.FACTION -> "세력"
    }

/** `연결` target kind as an item kind. */
val FieldLinkTargetKind.entityKind: EntityKind
    get() = when (this) {
        FieldLinkTargetKind.FACTION -> EntityKind.FACTION
        FieldLinkTargetKind.PLACE -> EntityKind.PLACE
        FieldLinkTargetKind.TERM -> EntityKind.TERM
    }

/** `세력 · 장소 · 용어`. */
val FieldLinkTargetKind.label: String get() = entityKind.label

internal val EntityKind.nameTagKind: NameTagKind
    get() = when (this) {
        EntityKind.CHARACTER -> NameTagKind.CHARACTER
        EntityKind.PLACE -> NameTagKind.PLACE
        EntityKind.TERM -> NameTagKind.TERM
        EntityKind.FACTION -> NameTagKind.FACTION
    }

/**
 * A key reference resolved at read time (§3 of the domain model): hiding first, then live, then
 * trashed, then a name tag, then missing. Plain text is never a reference.
 */
sealed interface ResolvedItem {
    val key: Key
    val kind: EntityKind

    /** Text for narrow places (speaker field, `@` chip, 등장 line). */
    val narrowLabel: String

    /** Text for wide places (panels, cards, check rows): `지운 인물 · 노아`. */
    val wideLabel: String

    /** Drawn with a dotted underline and `◇`. */
    val dotted: Boolean get() = this is Trashed || this is Tagged || this is Missing

    /** The target is hidden and the hiding gate is locked (`숨긴 인물` / `숨긴 항목`). */
    data class HiddenLocked(override val key: Key, override val kind: EntityKind) : ResolvedItem {
        override val narrowLabel: String get() = hiddenWord(kind)
        override val wideLabel: String get() = hiddenWord(kind)
    }

    data class Live(override val key: Key, override val kind: EntityKind, val name: String, val hidden: Boolean) : ResolvedItem {
        override val narrowLabel: String get() = name
        override val wideLabel: String get() = name
    }

    /** In the trash: `지운 인물 · 노아`; fix paths include `되살리기`. */
    data class Trashed(override val key: Key, override val kind: EntityKind, val name: String) : ResolvedItem {
        override val narrowLabel: String get() = name
        override val wideLabel: String get() = "지운 ${kind.label} · $name"
    }

    /**
     * Only a name tag is left: `영구 삭제 · 노아` (purged) or `지운 인물 · 노아` without `되살리기`
     * (not exported). A hidden item's tag reads `영구 삭제 · 숨긴 항목` while locked.
     */
    data class Tagged(
        override val key: Key,
        override val kind: EntityKind,
        val name: String,
        val purged: Boolean,
        val maskedHidden: Boolean,
        val voiceCode: String?,
    ) : ResolvedItem {
        private val shownName: String get() = if (maskedHidden) CastCopy.HIDDEN_ITEM else name
        override val narrowLabel: String get() = if (purged) "영구 삭제 · $shownName" else shownName
        override val wideLabel: String get() = if (purged) "영구 삭제 · $shownName" else "지운 ${kind.label} · $shownName"
    }

    /** Neither a row nor a tag (copy not in the sources; `TODO_COPY_DM_01`). */
    data class Missing(override val key: Key, override val kind: EntityKind) : ResolvedItem {
        override val narrowLabel: String get() = "지운 ${kind.label}"
        override val wideLabel: String get() = "지운 ${kind.label}"
    }

    companion object {
        fun hiddenWord(kind: EntityKind): String = if (kind == EntityKind.CHARACTER) CastCopy.HIDDEN_CHARACTER else CastCopy.HIDDEN_ITEM
    }
}

/** Reference resolution for characters, places, terms and factions. */
object ItemRefs {
    /**
     * Resolves [key] of [kind]. [hiddenUnlocked] is the hiding gate (rule 19): while locked, a
     * hidden target is never named.
     */
    fun resolve(doc: ProjectDoc, kind: EntityKind, key: Key, hiddenUnlocked: Boolean = false): ResolvedItem {
        val row: Triple<String, Boolean, Boolean>? = when (kind) {
            EntityKind.CHARACTER -> doc.index.character(key)?.let { Triple(it.name, it.hidden, it.deletedAt == null) }
            EntityKind.PLACE -> doc.index.place(key)?.let { Triple(it.name, it.hidden, it.deletedAt == null) }
            EntityKind.TERM -> doc.index.term(key)?.let { Triple(it.name, it.hidden, it.deletedAt == null) }
            EntityKind.FACTION -> doc.index.faction(key)?.let { Triple(it.name, it.hidden, it.deletedAt == null) }
        }
        if (row != null) {
            val (name, hidden, live) = row
            if (hidden && !hiddenUnlocked) return ResolvedItem.HiddenLocked(key, kind)
            return if (live) ResolvedItem.Live(key, kind, name, hidden) else ResolvedItem.Trashed(key, kind, name)
        }
        val tag = doc.index.nameTag(key)
        if (tag != null && tag.kind.known == kind.nameTagKind) {
            return ResolvedItem.Tagged(
                key = key, kind = kind, name = tag.name,
                purged = tag.state.known != NameTagState.NOT_EXPORTED,
                maskedHidden = tag.hidden && !hiddenUnlocked,
                voiceCode = tag.voiceCode,
            )
        }
        return ResolvedItem.Missing(key, kind)
    }

    fun character(doc: ProjectDoc, key: Key, hiddenUnlocked: Boolean = false): ResolvedItem =
        resolve(doc, EntityKind.CHARACTER, key, hiddenUnlocked)

    /** Live and not hidden-locked: may be listed, picked and searched. */
    fun isVisible(doc: ProjectDoc, kind: EntityKind, key: Key, hiddenUnlocked: Boolean): Boolean =
        resolve(doc, kind, key, hiddenUnlocked) is ResolvedItem.Live
}
