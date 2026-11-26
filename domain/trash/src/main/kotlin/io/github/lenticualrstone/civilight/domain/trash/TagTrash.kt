package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.cast.Tag
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind

/** Result of [TagTrash.restore]. */
sealed interface TagRestore {
    data class Restored(val doc: ProjectDoc, val tag: Tag) : TagRestore

    /** A live tag already has this name; `되살리기` stays disabled (CST-75, nothing renamed by guess). */
    data class NameTaken(val name: String) : TagRestore

    data object NotFound : TagRestore
}

/**
 * (0.2.0, N-44, CST-75) Tags in the trash. A trashed tag keeps its row in `project.json › tags` with
 * `deletedAt`; characters, terms (2026-11-26) and gallery images keep its key, so a restore shows it on them again. A
 * purge (`영구 삭제` or the 30-day purge, through [PurgeOps]) removes the key everywhere.
 */
object TagTrash {
    /** Live tags in order (the only tags any surface shows). */
    fun liveTags(doc: ProjectDoc): List<Tag> =
        doc.project.tags.filter { it.isLive }.sortedWith(compareBy<Tag>({ it.order }, { it.key }))

    /** True when no **live** tag other than [except] has [name] (NFC, trimmed). */
    fun nameFree(doc: ProjectDoc, name: String, except: Key? = null): Boolean {
        val n = name.trim().nfc()
        return n.isNotEmpty() && doc.project.tags.none { it.isLive && it.key != except && it.name.nfc() == n }
    }

    /** `쓰는 인물 n`: live characters holding [tagKey] (the caller leaves out locked hidden ones by [counted]). */
    fun usingCharacters(doc: ProjectDoc, tagKey: Key, counted: (io.github.lenticualrstone.civilight.model.cast.Character) -> Boolean = { true }): Int =
        doc.cast.characters.count { it.deletedAt == null && tagKey in it.tagKeys && counted(it) }

    /**
     * (2026-11-26) `용어 n`: live terms holding [tagKey] (the caller leaves out locked hidden ones by [counted]).
     * Only a live tag of scope term or all can be on a term.
     */
    fun usingTerms(doc: ProjectDoc, tagKey: Key, counted: (io.github.lenticualrstone.civilight.model.world.Term) -> Boolean = { true }): Int =
        doc.world.terms.count { it.deletedAt == null && tagKey in it.tagKeys && counted(it) }

    /** `이미지 n`: live gallery images holding [tagKey]. */
    fun usingImages(doc: ProjectDoc, tagKey: Key): Int =
        doc.galleryItems.count { it.deletedAt == null && tagKey in it.tagKeys }

    /** Moves a live tag to the trash with one entry of kind `tag`; null when it is not a live tag. */
    fun trash(doc: ProjectDoc, tagKey: Key, ctx: TrashContext): Pair<ProjectDoc, TrashEntry>? {
        val tag = doc.project.tags.firstOrNull { it.key == tagKey && it.isLive } ?: return null
        val entry = TrashEntry(
            key = ctx.newKey(),
            kind = TrashKind.TAG.open(),
            projectKey = doc.key,
            targetKeys = listOf(tagKey),
            deletedAt = ctx.now,
            purgeOn = TrashEntry.purgeDateFor(ctx.today),
        )
        val marked = tag.copy(deletedAt = ctx.now, trashEntryKey = entry.key, updatedAt = ctx.now, updatedBy = ctx.deviceId)
        val next = doc.copy(
            project = doc.project.copy(tags = doc.project.tags.map { if (it.key == tagKey) marked else it }),
            trash = doc.trash.copy(entries = doc.trash.entries + entry),
        )
        return next to entry
    }

    /** `되살리기` of a tag entry; refused while a live tag has the same name. */
    fun restore(doc: ProjectDoc, entryKey: Key, ctx: TrashContext): TagRestore {
        val entry = doc.index.trashEntry(entryKey)?.takeIf { it.kind.known == TrashKind.TAG } ?: return TagRestore.NotFound
        val tag = doc.project.tags.firstOrNull { it.key in entry.targetKeys && !it.isLive } ?: return TagRestore.NotFound
        if (!nameFree(doc, tag.name, except = tag.key)) return TagRestore.NameTaken(tag.name)
        val back = tag.copy(deletedAt = null, trashEntryKey = null, updatedAt = ctx.now, updatedBy = ctx.deviceId)
        val next = doc.copy(
            project = doc.project.copy(tags = doc.project.tags.map { if (it.key == tag.key) back else it }),
            trash = doc.trash.copy(entries = doc.trash.entries.filter { it.key != entryKey }),
        )
        return TagRestore.Restored(next, back)
    }

    /** False while a live tag has the trashed tag's name (`되살리기` disabled, `TODO_COPY_CAST_TAG_NAME_TAKEN`). */
    fun canRestore(doc: ProjectDoc, entryKey: Key): Boolean {
        val entry = doc.index.trashEntry(entryKey)?.takeIf { it.kind.known == TrashKind.TAG } ?: return false
        val tag = doc.project.tags.firstOrNull { it.key in entry.targetKeys && !it.isLive } ?: return false
        return nameFree(doc, tag.name, except = tag.key)
    }

    /** Removes [tagKey] from the project, every character, every term (2026-11-26) and every gallery image (live and trashed rows). */
    fun removeEverywhere(doc: ProjectDoc, tagKey: Key, ctx: TrashContext): ProjectDoc = doc.copy(
        project = doc.project.copy(tags = doc.project.tags.filterNot { it.key == tagKey }),
        world = if (doc.world.terms.none { tagKey in it.tagKeys }) doc.world else doc.world.copy(
            terms = doc.world.terms.map {
                if (tagKey in it.tagKeys) it.copy(tagKeys = it.tagKeys - tagKey, updatedAt = ctx.now, updatedBy = ctx.deviceId) else it
            },
        ),
        cast = doc.cast.copy(
            characters = doc.cast.characters.map {
                if (tagKey in it.tagKeys) it.copy(tagKeys = it.tagKeys - tagKey, updatedAt = ctx.now, updatedBy = ctx.deviceId) else it
            },
        ),
        gallery = doc.gallery.mapValues { (_, file) ->
            if (file.items.none { tagKey in it.tagKeys }) {
                file
            } else {
                file.copy(items = file.items.map { if (tagKey in it.tagKeys) it.copy(tagKeys = it.tagKeys - tagKey, updatedAt = ctx.now, updatedBy = ctx.deviceId) else it })
            }
        },
    )
}
