package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.trash.PurgeOps
import io.github.lenticualrstone.civilight.domain.trash.PurgeResult
import io.github.lenticualrstone.civilight.domain.trash.TagRestore
import io.github.lenticualrstone.civilight.domain.trash.TagTrash
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.cast.Tag
import io.github.lenticualrstone.civilight.model.cast.TagScope
import io.github.lenticualrstone.civilight.model.cast.TagScopes
import io.github.lenticualrstone.civilight.model.cast.fits
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.trash.TrashCopy
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.trash.TrashEntry

/** `지우기` confirm counts (CST-75): `쓰는 인물 n` and, when any, `이미지 n`; (2026-11-26) `용어 n`. */
data class TagUsage(val characters: Int, val images: Int, val terms: Int = 0)

/**
 * (2026-11-26) The impact sheet of a tag `지우기`: counts per kind. [parts] has one fragment per kind the
 * tag may be on (`인물 3` for an `인물` tag, `용어 2` for a `용어 전용` tag, both for `모든 항목`), then
 * `이미지 n` when any image holds it. [line] = `인물 3 · 용어 2`.
 */
data class TagImpact(val tagKey: Key, val name: String, val scope: TagScope?, val characters: Int, val terms: Int, val images: Int) {
    val parts: List<String>
        get() = buildList {
            if (scope == TagScope.CHARACTER || scope == TagScope.ALL || characters > 0) add("${CastCopy.tagKindWord(TagScope.CHARACTER)} $characters")
            if (scope == TagScope.TERM || scope == TagScope.ALL || terms > 0) add("${CastCopy.tagKindWord(TagScope.TERM)} $terms")
            if (images > 0) add("${TrashCopy.kindWord(io.github.lenticualrstone.civilight.domain.trash.ItemKind.GALLERY_ITEM)} $images")
        }
    val line: String get() = parts.joinToString(CastCopy.SEP)
}

/** (2026-11-26) Result of [TagActions.createFor]. */
sealed interface TagCreate {
    data class Created(val doc: ProjectDoc, val tag: Tag) : TagCreate

    /**
     * A live tag already has the name (any scope; names are unique across scopes). [fits] = the card's kind
     * may carry it, so the UI can offer `이 태그 쓰기`; otherwise only `모든 항목으로 넓히기` ([TagActions.widen]).
     */
    data class NameTaken(val existing: Tag, val fits: Boolean) : TagCreate

    data object Blank : TagCreate
}

/** (2026-11-26) Result of [TagActions.widen]. */
sealed interface TagWiden {
    data class Widened(val doc: ProjectDoc, val tag: Tag) : TagWiden

    /** Already that scope; nothing written. */
    data class Unchanged(val tag: Tag) : TagWiden

    /** Narrowing (or moving to another kind, or an unknown scope) is never allowed: an item could be left holding a tag it may not carry. */
    data class Refused(val from: TagScope?, val to: TagScope) : TagWiden

    data object NotFound : TagWiden
}

/**
 * (0.2.0, U-15, N-44, CST-75) Tags: create with a colour, recolour, delete to the trash, restore
 * (re-attaches on every character and image that kept the key) and purge. Every tag delete in the
 * app goes through [trash]; the permanent removal is only [purge] (and the 30-day purge, which runs
 * the same `domain:trash` code through `PurgeActions.runDue`).
 */
object TagActions {
    /** Live tags in order, every scope (name checks, tag settings). Pickers and chips use [tagsFor]. */
    fun liveTags(doc: ProjectDoc): List<Tag> = TagTrash.liveTags(doc)

    /** (2026-11-26) `tagsFor(kind)`: live tags an item of [kind] may carry (scope == [kind] or `모든 항목`), in tag order. */
    fun tagsFor(doc: ProjectDoc, kind: TagScope): List<Tag> = TagScopes.tagsFor(doc, kind)

    /** True when [name] is free among **live** tags of every scope (CST-75 `canName`). */
    fun canName(doc: ProjectDoc, name: String, except: Key? = null): Boolean = TagTrash.nameFree(doc, name, except)

    /** `만들기` of the create card (`이름 · 색`) of a character surface; null when the name is blank or a live tag has it. */
    fun create(doc: ProjectDoc, name: String, color: String?, ctx: ActionContext): Pair<ProjectDoc, Tag>? =
        (createFor(doc, name, color, TagScope.CHARACTER, ctx) as? TagCreate.Created)?.let { it.doc to it.tag }

    /**
     * (2026-11-26) `만들기` of the new-tag card on a card of [kind] (`인물` · `용어` · `장소` · `세력`). The
     * scope is [kind] (`용어 전용` from a term card) unless [everywhere] is chosen (`모든 항목`, never by
     * default). Names are unique among all live tags regardless of scope; a taken name returns the tag.
     */
    fun createFor(doc: ProjectDoc, name: String, color: String?, kind: TagScope, ctx: ActionContext, everywhere: Boolean = false): TagCreate {
        require(kind != TagScope.ALL) { "kind is the card's kind; pass everywhere = true for 모든 항목" }
        val n = name.trim().nfc()
        if (n.isEmpty()) return TagCreate.Blank
        TagScopes.liveNamed(doc, n)?.let { return TagCreate.NameTaken(it, it.fits(kind)) }
        val tag = Tag(
            key = ctx.newKey(),
            name = n,
            order = OrderKeys.after(doc.project.tags.maxOfOrNull { it.order }),
            updatedAt = ctx.now,
            updatedBy = ctx.device,
            color = color,
            scope = (if (everywhere) TagScope.ALL else kind).open(),
        )
        return TagCreate.Created(doc.copy(project = doc.project.copy(tags = doc.project.tags + tag)), tag)
    }

    /**
     * (2026-11-26) Widens a live tag's scope: a kind's own scope → `모든 항목`. Anything else (narrowing,
     * one kind to another, an unknown scope from a newer file) is [TagWiden.Refused].
     */
    fun widen(doc: ProjectDoc, tagKey: Key, to: TagScope, ctx: ActionContext): TagWiden {
        val tag = doc.project.tags.firstOrNull { it.key == tagKey && it.isLive } ?: return TagWiden.NotFound
        val from = tag.scope.known
        if (from == to) return TagWiden.Unchanged(tag)
        if (from == null || to != TagScope.ALL) return TagWiden.Refused(from, to)
        val next = tag.copy(scope = TagScope.ALL.open(), updatedAt = ctx.now, updatedBy = ctx.device)
        return TagWiden.Widened(doc.copy(project = doc.project.copy(tags = doc.project.tags.map { if (it.key == tagKey) next else it })), next)
    }

    /** (2026-11-26) True when [tagKey]'s scope may change to [to] (only own kind → `모든 항목`). */
    fun canWiden(doc: ProjectDoc, tagKey: Key, to: TagScope = TagScope.ALL): Boolean {
        val from = doc.project.tags.firstOrNull { it.key == tagKey && it.isLive }?.scope?.known ?: return false
        return from != to && to == TagScope.ALL
    }

    /** Sets or clears ([color] null) a live tag's colour. */
    fun setColor(doc: ProjectDoc, tagKey: Key, color: String?, ctx: ActionContext): ProjectDoc {
        val tag = doc.project.tags.firstOrNull { it.key == tagKey && it.isLive } ?: return doc
        if (tag.color == color) return doc
        val next = tag.copy(color = color, updatedAt = ctx.now, updatedBy = ctx.device)
        return doc.copy(project = doc.project.copy(tags = doc.project.tags.map { if (it.key == tagKey) next else it }))
    }

    /** Counts for the `지우기` confirm; [counted] leaves out locked hidden characters, [countedTerm] locked hidden terms. */
    fun usage(
        doc: ProjectDoc,
        tagKey: Key,
        counted: (io.github.lenticualrstone.civilight.model.cast.Character) -> Boolean = { true },
        countedTerm: (io.github.lenticualrstone.civilight.model.world.Term) -> Boolean = { true },
    ): TagUsage = TagUsage(TagTrash.usingCharacters(doc, tagKey, counted), TagTrash.usingImages(doc, tagKey), TagTrash.usingTerms(doc, tagKey, countedTerm))

    /**
     * (2026-11-26, N-44 extended) The impact sheet of `지우기`: `인물 3 · 용어 2` (+ `이미지 n`). Null when
     * [tagKey] is not a live tag. [counted] / [countedTerm] leave out locked hidden items.
     */
    fun impact(
        doc: ProjectDoc,
        tagKey: Key,
        counted: (io.github.lenticualrstone.civilight.model.cast.Character) -> Boolean = { true },
        countedTerm: (io.github.lenticualrstone.civilight.model.world.Term) -> Boolean = { true },
    ): TagImpact? {
        val tag = doc.project.tags.firstOrNull { it.key == tagKey && it.isLive } ?: return null
        val u = usage(doc, tagKey, counted, countedTerm)
        return TagImpact(tag.key, tag.name, tag.scope.known, u.characters, u.terms, u.images)
    }

    /**
     * `휴지통으로`: only the tag goes to the trash; characters, terms and images keep the key, so the tag shows
     * nowhere (detached) until restored. Null when not a live tag.
     */
    fun trash(doc: ProjectDoc, tagKey: Key, ctx: ActionContext): Pair<ProjectDoc, TrashEntry>? = TagTrash.trash(doc, tagKey, ctx.trash)

    /** `되살리기` of a tag row: every holder (characters, terms, images) shows the tag again; refused while a live tag has its name. */
    fun restore(doc: ProjectDoc, entryKey: Key, ctx: ActionContext): TagRestore = TagTrash.restore(doc, entryKey, ctx.trash)

    /** `영구 삭제` of a tag row: the key leaves every character, term and image. */
    fun purge(doc: ProjectDoc, entryKey: Key, ctx: ActionContext): PurgeResult = PurgeOps.purge(doc, entryKey, ctx.trash)
}
