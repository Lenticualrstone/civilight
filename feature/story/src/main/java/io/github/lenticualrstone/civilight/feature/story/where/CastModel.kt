package io.github.lenticualrstone.civilight.feature.story.where

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.CharacterActions
import io.github.lenticualrstone.civilight.domain.actions.CharacterRestoreResult
import io.github.lenticualrstone.civilight.domain.story.scene.Appearance
import io.github.lenticualrstone.civilight.domain.story.scene.AppearanceEdit
import io.github.lenticualrstone.civilight.domain.trash.Fixes
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.feature.story.common.RestoreStep
import io.github.lenticualrstone.civilight.domain.trash.TrashGates
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** How one 등장 member is drawn (STO-26 with R19 / R20 rendering, STO-11). */
enum class MemberLook {
    /** The name. */
    LIVE,

    /** Locked hidden character: `숨긴 인물`, never the name (hiding wins over deletion). */
    HIDDEN,

    /** Deleted character: dotted name; a silent one gets `빼기 · 되살리기 · 이대로 두기`. */
    DELETED,

    /** Deleted and `이대로 두기` was chosen for this scene: dotted name, no buttons. */
    DELETED_KEPT,
}

data class CastMember(val key: Key, val name: String, val look: MemberLook) {
    val dotted: Boolean get() = look == MemberLook.DELETED || look == MemberLook.DELETED_KEPT
}

/**
 * The `등장 | 리엔 · 하르트 | (대사) | + 세라` row (STO-26): [speakers] follow the blocks (a fact, never
 * edited here), [silent] were added by hand and can be removed; `+` adds one through the speaker picker.
 */
data class CastRow(val speakers: List<CastMember>, val silent: List<CastMember>) {
    /** Live and hidden members (speakers first) for the picker's `이 씬 등장` section. */
    val inScene: List<Key> get() = (speakers + silent).filter { it.look == MemberLook.LIVE || it.look == MemberLook.HIDDEN }.map { it.key }
}

/** The 등장 row as data and its edits (STO-26, STO-11). Every edit is a no-op on a gone scene. */
object CastModel {
    fun row(doc: ProjectDoc, sceneKey: Key, locked: Boolean): CastRow {
        val a = Appearance.of(doc, sceneKey)
        // A key with no character row at all (purged without a name tag) is left out.
        fun member(key: Key): CastMember? {
            val c = doc.index.character(key) ?: return null
            val look = when {
                c.hidden && locked -> MemberLook.HIDDEN
                c.deletedAt != null -> if (isKept(doc, key, sceneKey)) MemberLook.DELETED_KEPT else MemberLook.DELETED
                else -> MemberLook.LIVE
            }
            val name = if (look == MemberLook.HIDDEN) WhereCopy.HIDDEN_CHARACTER else c.name
            return CastMember(key, name, look)
        }
        return CastRow(a.speakers.mapNotNull(::member), a.silent.mapNotNull(::member))
    }

    /** `+` → a picked character joins the silent cast. */
    fun add(doc: ProjectDoc, sceneKey: Key, characterKey: Key, ctx: ActionContext): ProjectDoc =
        AppearanceEdit.addSilent(doc, sceneKey, characterKey, ctx.edit)

    /** `빼기` (live or deleted silent member). */
    fun remove(doc: ProjectDoc, sceneKey: Key, characterKey: Key, ctx: ActionContext): ProjectDoc =
        AppearanceEdit.removeSilent(doc, sceneKey, characterKey, ctx.edit)

    /**
     * `이대로 두기` on a deleted silent member (R20 §2): the `deleted.item` acknowledgement for this one
     * reference (`[character, scene]`, trash `Fixes.keep`); the dotted name stays.
     */
    fun keep(doc: ProjectDoc, sceneKey: Key, characterKey: Key, ctx: ActionContext): ProjectDoc {
        val c = doc.index.character(characterKey) ?: return doc
        if (c.deletedAt == null || characterKey !in (doc.index.unit(sceneKey)?.silentCast ?: return doc)) return doc
        return Fixes.keep(doc, characterKey, ctx.trash, referencingKey = sceneKey).doc
    }

    /** `되살리기` on a deleted member: through `domain:actions` `CharacterActions.restore` (DECISIONS §1). */
    fun restore(doc: ProjectDoc, characterKey: Key, ctx: ActionContext, authenticated: Boolean): CharacterRestoreResult =
        CharacterActions.restore(doc, characterKey, ctx, authenticated)

    /** [restore] as a [RestoreStep] (doc, change, toast `노아 되살림 · 다시 이어짐 · …`), or null when nothing came back. */
    fun restoreStep(doc: ProjectDoc, characterKey: Key, ctx: ActionContext, authenticated: Boolean): RestoreStep? {
        val r = restore(doc, characterKey, ctx, authenticated)
        val restored = r.outcome as? RestoreOutcome.Restored ?: return null
        return RestoreStep(restored.doc, restored.change, r.toast)
    }

    /** `되살리기` needs `지문으로 보기` first (rule 19 gate on the character's trash entry). */
    fun needsAuth(doc: ProjectDoc, characterKey: Key): Boolean {
        val entry = doc.index.character(characterKey)?.trashEntryKey?.let(doc.index::trashEntry) ?: return false
        return TrashGates.needsAuth(doc, entry)
    }

    /** Kept for the whole item (점검 센터 `이대로 두기`) or for this scene only. */
    fun isKept(doc: ProjectDoc, characterKey: Key, sceneKey: Key): Boolean =
        doc.checks.acknowledgements.any {
            it.checkType == CheckType.DELETED_ITEM && (it.subjectKeys == listOf(characterKey) || it.subjectKeys == listOf(characterKey, sceneKey))
        }
}
