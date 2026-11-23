package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.trash.PurgeOps
import io.github.lenticualrstone.civilight.domain.trash.PurgeResult
import io.github.lenticualrstone.civilight.domain.trash.TagRestore
import io.github.lenticualrstone.civilight.domain.trash.TagTrash
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.cast.Tag
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.trash.TrashEntry

/** `지우기` confirm counts (CST-75): `쓰는 인물 n` and, when any, `이미지 n`. */
data class TagUsage(val characters: Int, val images: Int)

/**
 * (0.2.0, U-15, N-44, CST-75) Tags: create with a colour, recolour, delete to the trash, restore
 * (re-attaches on every character and image that kept the key) and purge. Every tag delete in the
 * app goes through [trash]; the permanent removal is only [purge] (and the 30-day purge, which runs
 * the same `domain:trash` code through `PurgeActions.runDue`).
 */
object TagActions {
    /** Live tags in order: the only tags chips, sections, pickers and filters show. */
    fun liveTags(doc: ProjectDoc): List<Tag> = TagTrash.liveTags(doc)

    /** True when [name] is free among **live** tags (CST-75 `canName`). */
    fun canName(doc: ProjectDoc, name: String, except: Key? = null): Boolean = TagTrash.nameFree(doc, name, except)

    /** `만들기` of the create card (`이름 · 색`); null when the name is blank or a live tag has it. */
    fun create(doc: ProjectDoc, name: String, color: String?, ctx: ActionContext): Pair<ProjectDoc, Tag>? {
        if (!canName(doc, name)) return null
        val tag = Tag(
            key = ctx.newKey(),
            name = name.trim().nfc(),
            order = OrderKeys.after(doc.project.tags.maxOfOrNull { it.order }),
            updatedAt = ctx.now,
            updatedBy = ctx.device,
            color = color,
        )
        return doc.copy(project = doc.project.copy(tags = doc.project.tags + tag)) to tag
    }

    /** Sets or clears ([color] null) a live tag's colour. */
    fun setColor(doc: ProjectDoc, tagKey: Key, color: String?, ctx: ActionContext): ProjectDoc {
        val tag = doc.project.tags.firstOrNull { it.key == tagKey && it.isLive } ?: return doc
        if (tag.color == color) return doc
        val next = tag.copy(color = color, updatedAt = ctx.now, updatedBy = ctx.device)
        return doc.copy(project = doc.project.copy(tags = doc.project.tags.map { if (it.key == tagKey) next else it }))
    }

    /** Counts for the `지우기` confirm; [counted] leaves out locked hidden characters. */
    fun usage(doc: ProjectDoc, tagKey: Key, counted: (io.github.lenticualrstone.civilight.model.cast.Character) -> Boolean = { true }): TagUsage =
        TagUsage(TagTrash.usingCharacters(doc, tagKey, counted), TagTrash.usingImages(doc, tagKey))

    /** `휴지통으로`: only the tag goes to the trash; characters and images keep the key. Null when not a live tag. */
    fun trash(doc: ProjectDoc, tagKey: Key, ctx: ActionContext): Pair<ProjectDoc, TrashEntry>? = TagTrash.trash(doc, tagKey, ctx.trash)

    /** `되살리기` of a tag row: every holder shows the tag again; refused while a live tag has its name. */
    fun restore(doc: ProjectDoc, entryKey: Key, ctx: ActionContext): TagRestore = TagTrash.restore(doc, entryKey, ctx.trash)

    /** `영구 삭제` of a tag row: the key leaves every character and image. */
    fun purge(doc: ProjectDoc, entryKey: Key, ctx: ActionContext): PurgeResult = PurgeOps.purge(doc, entryKey, ctx.trash)
}
