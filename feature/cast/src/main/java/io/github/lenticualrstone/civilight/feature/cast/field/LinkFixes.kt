package io.github.lenticualrstone.civilight.feature.cast.field

import io.github.lenticualrstone.civilight.domain.cast.ItemRefs
import io.github.lenticualrstone.civilight.domain.cast.ResolvedItem
import io.github.lenticualrstone.civilight.feature.cast.trash.ItemTrash
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * A dotted `연결` value in the value sheet (CST-41 table, CST-47, TC-20-13): `지운 세력 · 상층 경비대`
 * with `되살리기` (while in the trash) and `이대로 두기` (until chosen). `다른 항목 고르기` and `비우기`
 * are the sheet's own add chip and `빼기`. After a permanent delete the label reads
 * `영구 삭제 · 상층 경비대` and `되살리기` is gone (CST-44).
 */
data class DottedLinkRow(val key: Key, val label: String, val canRestore: Boolean, val kept: Boolean)

object LinkFixes {
    /**
     * Rows for the dotted values among [keys] (items of [kind]) held by [holderKey]'s field. A locked
     * hidden target is not dotted (it reads `숨긴 항목`) and gets no row.
     */
    fun rows(doc: ProjectDoc, kind: EntityKind, keys: List<Key>, holderKey: Key, hiddenUnlocked: Boolean): List<DottedLinkRow> =
        keys.distinct().mapNotNull { key ->
            val resolved = ItemRefs.resolve(doc, kind, key, hiddenUnlocked)
            if (!resolved.dotted) return@mapNotNull null
            DottedLinkRow(
                key = key,
                label = resolved.wideLabel,
                canRestore = resolved is ResolvedItem.Trashed,
                kept = ItemTrash.isKept(doc, key, holderKey),
            )
        }
}
