package io.github.lenticualrstone.civilight.feature.cast.settings

import io.github.lenticualrstone.civilight.domain.actions.TagActions
import io.github.lenticualrstone.civilight.domain.actions.TagImpact
import io.github.lenticualrstone.civilight.domain.actions.TagWiden
import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.TagUsage
import io.github.lenticualrstone.civilight.domain.library.hiding.ItemSurfaces
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.TagScope
import io.github.lenticualrstone.civilight.model.cast.TagScopes
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.ui.tags.TagSheetCopy

/**
 * One live project tag with its use. (2026-11-26) Every scope is listed here (the single place that manages
 * tags): [scope] is the tag's scope (null = unknown, from a newer file), [usage] counts the characters, terms
 * and images that carry it (locked hidden ones left out), and [line] is the impact line `인물 3 · 용어 2`
 * (+ `이미지 1` when any image holds it).
 */
data class TagRow(val key: Key, val name: String, val usage: TagUsage, val scope: TagScope? = TagScope.CHARACTER, val color: String? = null) {
    /** Characters that carry the tag (CST-68 counts each once). */
    val count: Int get() = usage.characters

    /** The impact of the row's counts (one fragment per kind the tag may be on). */
    val impact: TagImpact get() = TagImpact(key, name, scope, usage.characters, usage.terms, usage.images)

    /** `인물 3 · 용어 2` under the tag's name; null when there is nothing to say (unknown scope, no use). */
    val line: String? get() = impact.line.ifEmpty { null }

    /** The small scope label: `인물` · `용어` · `전체`; null for an unknown scope. */
    val scopeLabel: String? get() = scope?.let(CastCopy::tagScopeShort)

    /** `전체` is drawn as the accent pill (board `ImmTermTagSheet`). */
    val everywhere: Boolean get() = scope == TagScope.ALL

    /** `모든 항목으로 넓히기` is offered only on a kind's own scope (never on `모든 항목` or an unknown scope). */
    val canWiden: Boolean get() = scope != null && scope != TagScope.ALL
}

/** The scope picked on the settings new-tag card (radios, one choice). `인물 전용` is the default here. */
enum class TagNewScope(val kind: TagScope, val everywhere: Boolean) {
    CHARACTER(TagScope.CHARACTER, false),
    TERM(TagScope.TERM, false),
    ALL(TagScope.CHARACTER, true),
    ;

    /** `인물 전용` · `용어 전용` · `모든 항목`. */
    val label: String get() = if (everywhere) TagSheetCopy.EVERYWHERE else TagSheetCopy.ownScope(CastCopy.tagKindWord(kind))

    /** The trailing note in `ink3`: `기본` on the default, the kinds on `모든 항목`. */
    val trailing: String get() = when (this) {
        CHARACTER -> TagSheetCopy.DEFAULT
        TERM -> ""
        ALL -> TagSheetCopy.EVERYWHERE_SUB
    }

    companion object {
        val DEFAULT: TagNewScope = CHARACTER

        fun of(name: String?): TagNewScope = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/** The `모든 항목으로 넓히기` confirm: irreversible (`줄일 수 없음`). */
data class TagWidenConfirm(val key: Key, val name: String, val title: String, val lines: List<String>)

/**
 * Project tags (CST-12, CST-75; 2026-11-26 scopes): every live tag in tag order whatever its scope, names unique
 * across scopes. Rename, recolour, delete (N-44) and widen (`모든 항목으로 넓히기`) work on any row.
 */
object TagSettingsModel {
    /**
     * Counts follow the list chips and the gallery: locked hidden characters, terms and images are not
     * counted (rule 19 §6). Unlocked, a row equals `CharacterEdits.tagUsage`.
     */
    fun rows(doc: ProjectDoc, hiddenUnlocked: Boolean = false): List<TagRow> {
        val live = doc.cast.characters.filter { it.deletedAt == null && (hiddenUnlocked || !it.hidden) }
        val surfaces = ItemSurfaces(doc)
        val images = doc.galleryItems.filter { surfaces.isGalleryItemVisible(it, locked = !hiddenUnlocked) }
        val terms = doc.world.terms.filter { it.deletedAt == null && (hiddenUnlocked || !it.hidden) }
        return TagScopes.live(doc).map { tag ->
            TagRow(
                key = tag.key,
                name = tag.name,
                usage = TagUsage(live.count { tag.key in it.tagKeys }, images.count { tag.key in it.tagKeys }, terms.count { tag.key in it.tagKeys }),
                scope = tag.scope.known,
                color = tag.color,
            )
        }
    }

    /** A new name is usable when it is not blank and no other live tag of any scope has it (NFC). */
    fun canName(doc: ProjectDoc, name: String, except: Key? = null): Boolean =
        name.trim().nfc().isNotEmpty() && TagScopes.liveNamed(doc, name, except) == null

    /** The inline error of a name field: `이미 있음 · 광물` when another live tag (any scope) has the name. */
    fun nameError(doc: ProjectDoc, name: String, except: Key? = null): String? =
        TagScopes.liveNamed(doc, name, except)?.let { TagSheetCopy.taken(it.name) }

    /**
     * Renames [key] through the one write path `CharacterEdits.renameTag`; null when the name is blank,
     * taken or unchanged. Characters, terms and gallery images keep the tag (by key), so nothing else changes.
     */
    fun rename(doc: ProjectDoc, key: Key, name: String, ctx: EditContext): ProjectDoc? {
        val n = name.trim().nfc()
        val tag = CharacterEdits.liveTag(doc, key) ?: return null
        if (n.isEmpty() || tag.name == n || !canName(doc, n, except = key)) return null
        return CharacterEdits.renameTag(doc, key, n, ctx)
    }

    /** The widen confirm of [key]: `‘광물’ 모든 항목으로 넓히기`, `용어 전용 → 모든 항목`, `줄일 수 없음`. Null when it may not widen. */
    fun widenConfirm(doc: ProjectDoc, key: Key): TagWidenConfirm? {
        val tag = CharacterEdits.liveTag(doc, key) ?: return null
        val from = tag.scope.known ?: return null
        if (!TagActions.canWiden(doc, key)) return null
        return TagWidenConfirm(
            key = key,
            name = tag.name,
            title = TagSettingsCopy.widenTitle(tag.name),
            lines = listOf(TagSettingsCopy.widenFromTo(CastCopy.tagScopeLabel(from)), TagSettingsCopy.WIDEN_ONE_WAY),
        )
    }

    /**
     * (2026-11-26) The widen re-applied to a kept undo step (the widen is one-way: no undo step, and undoing
     * an earlier step never narrows the tag again). A plain row write: the tag row gets `모든 항목` whether live
     * or in the trash on that step (so a later restore brings it back wide). A step without the row, or where
     * it is already `모든 항목` or of an unknown scope, is returned as is.
     */
    fun widenOnStep(doc: ProjectDoc, key: Key, ctx: io.github.lenticualrstone.civilight.domain.actions.ActionContext): ProjectDoc {
        val tag = doc.project.tags.firstOrNull { it.key == key } ?: return doc
        val from = tag.scope.known ?: return doc
        if (from == TagScope.ALL) return doc
        val next = tag.copy(scope = TagScope.ALL.open(), updatedAt = ctx.now, updatedBy = ctx.device)
        return doc.copy(project = doc.project.copy(tags = doc.project.tags.map { if (it.key == key) next else it }))
    }

    /** One terse line for a widen that wrote nothing (`Refused` · `NotFound`); null after `Widened` or `Unchanged`. */
    fun widenNotice(result: TagWiden): String? = when (result) {
        is TagWiden.Widened, is TagWiden.Unchanged -> null
        is TagWiden.Refused -> TagSettingsCopy.WIDEN_REFUSED
        TagWiden.NotFound -> TagSettingsCopy.GONE
    }

    /** The settings card's `만들기`: the picked scope (`인물 전용` default) through `TagActions.createFor`. */
    fun create(doc: ProjectDoc, name: String, color: String?, scope: TagNewScope, ctx: io.github.lenticualrstone.civilight.domain.actions.ActionContext) =
        TagActions.createFor(doc, name, color, scope.kind, ctx, everywhere = scope.everywhere)
}

/** Copy of the tag settings screen (labels and fragments only). */
object TagSettingsCopy {
    const val SCOPE = TagSheetCopy.SCOPE
    const val NOTE = TagSheetCopy.NOTE
    const val WIDEN = TagSheetCopy.WIDEN

    /** Confirm button of the widen dialog. */
    const val WIDEN_CONFIRM = "넓히기"

    /** The irreversible line of the widen confirm. */
    const val WIDEN_ONE_WAY = "줄일 수 없음"

    /** A widen that was refused (narrowing, another kind, an unknown scope). */
    const val WIDEN_REFUSED = "범위 넓히기 안 됨"

    /** The tag left the list (trashed elsewhere) before the write. */
    const val GONE = "지운 태그"

    /** `‘광물’ 모든 항목으로 넓히기`. */
    fun widenTitle(name: String): String = "‘$name’ ${TagSheetCopy.WIDEN}"

    /** `용어 전용 → 모든 항목`. */
    fun widenFromTo(from: String): String = "$from → ${TagSheetCopy.EVERYWHERE}"

    val all: List<String> = listOf(WIDEN_CONFIRM, WIDEN_ONE_WAY, WIDEN_REFUSED, GONE)
}
