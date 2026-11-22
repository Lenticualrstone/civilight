package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.trash.NameTagKind
import io.github.lenticualrstone.civilight.model.trash.TrashKind

/** Kinds of project items that can be put in the trash (§5.2). Projects and recorder records live outside a project. */
enum class ItemKind {
    CHARACTER, PLACE, TERM, FACTION, FIELD_DEF, CHANGE, MAP, EVENT, PAGE, CANVAS,

    /** A story unit (scene, or any higher level); trash kind `scene`. */
    UNIT,

    /** A non-empty block; trash kind `blockGroup` (one entry per scene per local day). */
    BLOCK;

    val trashKind: TrashKind
        get() = when (this) {
            CHARACTER -> TrashKind.CHARACTER
            PLACE -> TrashKind.PLACE
            TERM -> TrashKind.TERM
            FACTION -> TrashKind.FACTION
            FIELD_DEF -> TrashKind.FIELD_DEF
            CHANGE -> TrashKind.CHANGE
            MAP -> TrashKind.MAP
            EVENT -> TrashKind.EVENT
            PAGE -> TrashKind.PAGE
            CANVAS -> TrashKind.CANVAS
            UNIT -> TrashKind.SCENE
            BLOCK -> TrashKind.BLOCK_GROUP
        }

    /** Kinds that leave a name tag when purged (§5.3) and that can be merged (CST-45, WLD-15). */
    val isNamedItem: Boolean get() = this == CHARACTER || this == PLACE || this == TERM || this == FACTION

    val entityKind: EntityKind?
        get() = when (this) {
            CHARACTER -> EntityKind.CHARACTER
            PLACE -> EntityKind.PLACE
            TERM -> EntityKind.TERM
            FACTION -> EntityKind.FACTION
            else -> null
        }

    val nameTagKind: NameTagKind?
        get() = when (this) {
            CHARACTER -> NameTagKind.CHARACTER
            PLACE -> NameTagKind.PLACE
            TERM -> NameTagKind.TERM
            FACTION -> NameTagKind.FACTION
            UNIT -> NameTagKind.UNIT
            PAGE -> NameTagKind.PAGE
            else -> null
        }

    val recorderTargetKind: RecorderTargetKind?
        get() = when (this) {
            CHARACTER -> RecorderTargetKind.CHARACTER
            PLACE -> RecorderTargetKind.PLACE
            TERM -> RecorderTargetKind.TERM
            FACTION -> RecorderTargetKind.FACTION
            PAGE -> RecorderTargetKind.PAGE
            UNIT -> RecorderTargetKind.UNIT
            else -> null
        }

    companion object {
        /** The item kind of a project-scoped trash kind; null for `project`, `recorder` and unknown kinds. */
        fun of(kind: OpenEnum<TrashKind>): ItemKind? = when (kind.known) {
            TrashKind.CHARACTER -> CHARACTER
            TrashKind.PLACE -> PLACE
            TrashKind.TERM -> TERM
            TrashKind.FACTION -> FACTION
            TrashKind.FIELD_DEF -> FIELD_DEF
            TrashKind.CHANGE -> CHANGE
            TrashKind.MAP -> MAP
            TrashKind.EVENT -> EVENT
            TrashKind.PAGE -> PAGE
            TrashKind.CANVAS -> CANVAS
            TrashKind.SCENE -> UNIT
            TrashKind.BLOCK_GROUP -> BLOCK
            TrashKind.PROJECT, TrashKind.RECORDER, null -> null
        }

        fun of(kind: EntityKind): ItemKind = when (kind) {
            EntityKind.CHARACTER -> CHARACTER
            EntityKind.PLACE -> PLACE
            EntityKind.TERM -> TERM
            EntityKind.FACTION -> FACTION
        }
    }
}

/** One project item by kind and key. */
data class ItemId(val kind: ItemKind, val key: Key) {
    companion object {
        fun character(key: Key) = ItemId(ItemKind.CHARACTER, key)
        fun place(key: Key) = ItemId(ItemKind.PLACE, key)
        fun term(key: Key) = ItemId(ItemKind.TERM, key)
        fun faction(key: Key) = ItemId(ItemKind.FACTION, key)
        fun fieldDef(key: Key) = ItemId(ItemKind.FIELD_DEF, key)
        fun change(key: Key) = ItemId(ItemKind.CHANGE, key)
        fun map(key: Key) = ItemId(ItemKind.MAP, key)
        fun event(key: Key) = ItemId(ItemKind.EVENT, key)
        fun page(key: Key) = ItemId(ItemKind.PAGE, key)
        fun canvas(key: Key) = ItemId(ItemKind.CANVAS, key)
        fun unit(key: Key) = ItemId(ItemKind.UNIT, key)
        fun block(key: Key) = ItemId(ItemKind.BLOCK, key)
    }
}
