package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.checks.AckKind
import io.github.lenticualrstone.civilight.model.checks.Acknowledgement
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun

/** Result of a fix path; undo with [DocChange.revert]. */
data class FixResult(val doc: ProjectDoc, val count: Int, val change: DocChange)

/** Fix paths for references to deleted items (CST-41, CST-42) and the same-name notice (CST-46). */
object Fixes {
    /**
     * `화자 한꺼번에 바꾸기` (CST-42, TC-20-04): the speaker [from] becomes [to] on live dialogue
     * lines and spoken options of live scenes ([blockKeys] limits it to some lines). Lines inside
     * trashed scenes are not touched (they are judged when the scene returns).
     */
    fun replaceSpeaker(doc: ProjectDoc, from: Key, to: Key, ctx: TrashContext, blockKeys: Set<Key>? = null): FixResult {
        val live = doc.manuscript.liveBlocks.map { it.key }.toSet()
        var count = 0
        val next = doc.copy(
            episodes = doc.episodes.mapValues { (_, file) ->
                file.copy(
                    blocks = file.blocks.map { b ->
                        if (b.key !in live || (blockKeys != null && b.key !in blockKeys)) return@map b
                        var changed = false
                        val speaker = if (b.kind.known == BlockKind.DIALOGUE && b.speakerKey == from) {
                            changed = true; count++; to
                        } else {
                            b.speakerKey
                        }
                        val options = b.options.map { o -> if (o.speakerKey == from) o.copy(speakerKey = to).also { changed = true; count++ } else o }
                        if (changed) b.copy(speakerKey = speaker, options = options, updatedAt = ctx.now, updatedBy = ctx.deviceId) else b
                    },
                )
            },
        )
        return FixResult(next, count, DocChange(doc, next))
    }

    /** Preview line of the bulk change: `대사 14 · 노아 → 오웬`. */
    fun replaceSpeakerPreview(count: Int, fromName: String, toName: String): String =
        TrashCopy.join("대사 $count", "$fromName → $toName")

    /**
     * `글로 풀기` (TC-20-22): the link run [linkKey] becomes plain text with the same shown text,
     * wherever it is. The choice is kept when the item is restored.
     */
    fun unlink(doc: ProjectDoc, linkKey: Key, ctx: TrashContext): FixResult {
        val (next, owners) = doc.rewriteLinks(
            { link -> if (link.linkKey == linkKey) TextRun.Plain(link.text) else link },
            ctx.now, ctx.deviceId,
        )
        return FixResult(next, owners.size, DocChange(doc, next))
    }

    /**
     * `이대로 두기` on a deleted item (CST-42, TC-20-28): stores `Acknowledgement(deleted.item, keep,
     * [itemKey])` (or `[itemKey, referencingKey]` for one reference, e.g. a `대상 지워짐` record). The
     * dotted marks stay; the row moves to `맞다고 표시한 것`.
     */
    fun keep(doc: ProjectDoc, itemKey: Key, ctx: TrashContext, referencingKey: Key? = null): FixResult {
        val subjects = listOfNotNull(itemKey, referencingKey)
        if (doc.checks.acknowledgements.any { it.checkType == CheckType.DELETED_ITEM && it.subjectKeys == subjects }) {
            return FixResult(doc, 0, DocChange(doc, doc))
        }
        val ack = Acknowledgement(
            key = ctx.newKey(),
            checkType = CheckType.DELETED_ITEM,
            subjectKeys = subjects,
            fingerprint = deletedItemFingerprint(subjects),
            kind = AckKind.KEEP.open(),
            createdAt = ctx.now,
            updatedAt = ctx.now,
            updatedBy = ctx.deviceId,
        )
        val next = doc.copy(checks = doc.checks.copy(acknowledgements = doc.checks.acknowledgements + ack))
        return FixResult(next, 1, DocChange(doc, next))
    }

    /** True when the item's deleted-item row was acknowledged with `이대로 두기`. */
    fun isKept(doc: ProjectDoc, itemKey: Key): Boolean =
        doc.checks.acknowledgements.any { it.checkType == CheckType.DELETED_ITEM && it.subjectKeys == listOf(itemKey) }

    /**
     * Fingerprint of a `deleted.item` acknowledgement (`ED`): the subjects themselves, since the
     * ack is about the item and stays valid while it is in the trash and after it is purged.
     */
    fun deletedItemFingerprint(subjects: List<Key>): String = "${CheckType.DELETED_ITEM}:${subjects.joinToString(",")}"

    /** Live items of the same kind whose name is exactly [item]'s name (NFC equality, never similarity; CST-46). */
    fun sameNameCount(doc: ProjectDoc, item: ItemId): Int {
        val name = doc.itemName(item)?.nfc() ?: return 0
        val names = when (item.kind) {
            ItemKind.CHARACTER -> doc.cast.characters.filter { it.deletedAt == null }.map { it.name }
            ItemKind.PLACE -> doc.world.places.filter { it.deletedAt == null }.map { it.name }
            ItemKind.TERM -> doc.world.terms.filter { it.deletedAt == null }.map { it.name }
            ItemKind.FACTION -> doc.world.factions.filter { it.deletedAt == null }.map { it.name }
            else -> return 0
        }
        return names.count { it.nfc() == name }
    }

    /** `◇ 같은 이름 2 · 노아` when another live item has the same name; with the action `합치기`. */
    fun sameNameNotice(doc: ProjectDoc, item: ItemId): String? {
        val count = sameNameCount(doc, item)
        if (count < 2) return null
        return "◇ 같은 이름 $count${TrashCopy.SEP}${doc.itemName(item)}"
    }

    /**
     * The speaker picker's top row for a block whose speaker is in the trash (TC-20-16, SCR-57):
     * `지운 인물 · 노아 · 되살리기`; null otherwise (deleted items are never listed in the picker).
     */
    fun deletedSpeakerPickerRow(doc: ProjectDoc, blockKey: Key): String? {
        val block = doc.index.block(blockKey) ?: return null
        val speaker = block.speakerKey ?: return null
        val character = doc.index.character(speaker) ?: return null
        if (character.deletedAt == null) return null
        return TrashCopy.join(TrashCopy.deletedLabel(ItemKind.CHARACTER, character.name), TrashCopy.RESTORE)
    }
}
