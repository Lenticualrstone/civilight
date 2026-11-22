package io.github.lenticualrstone.civilight.feature.world.common

import io.github.lenticualrstone.civilight.domain.world.item.RefLabel
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.domain.world.item.WorldRefs
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.world.ItemRef
import io.github.lenticualrstone.civilight.model.world.ItemRefKind

/** Whether a card can show its item (rule 19 hiding, rule 20 trash). */
sealed interface CardAccess {
    /** The item is live and visible. */
    data object Open : CardAccess

    /** Hidden while locked: one lock row `숨긴 용어 1 · 지문으로 보기`, title `숨긴 항목`. */
    data class Locked(val text: String) : CardAccess

    /** Trashed (`지운 용어 · 오리지늄 등불`, dotted), purged (`영구 삭제 · …`) or unknown (empty). */
    data class Gone(val text: String, val dotted: Boolean) : CardAccess

    companion object {
        /** The access of [kind] [key] for a card route. */
        fun of(doc: ProjectDoc, kind: WorldKind, key: Key, hiddenOpen: Boolean): CardAccess =
            when (val label = WorldRefs.label(doc, kind, key, hiddenUnlocked = hiddenOpen)) {
                is RefLabel.Live -> Open
                is RefLabel.Hidden -> Locked(WorldCopy.hiddenRow(kind, 1))
                is RefLabel.Deleted -> Gone(label.text, dotted = true)
                is RefLabel.Purged -> Gone(label.text, dotted = true)
                is RefLabel.Missing -> Gone(label.text, dotted = false)
            }
    }
}

/** Eyebrow, name and description of a card (`용어` · `오리지늄 등불` · 설명). */
data class CardHeadUi(val eyebrow: String, val name: String, val description: List<ParagraphUi>)

/**
 * A fixed or derived field row (`첫 공개 | 1-7 씬 3 · #044`); editable rows open their editor. [note]
 * is the small line after the value (`별칭 | 오리지늄 · 빛돌 | 후보 2 새로 찾음`, board `RTLinkAll`).
 */
data class FixedRowUi(val label: String, val value: String, val editable: Boolean, val note: String? = null)

/** Item references as cards print them: live names, `숨긴 항목`, `지운 용어 · …`, joined with ` · `. */
object ItemRefsText {
    fun of(doc: ProjectDoc, refs: List<ItemRef>, hiddenOpen: Boolean): String = refs.mapNotNull { ref ->
        val kind = kindOf(ref.kind.known ?: return@mapNotNull null)
        WorldRefs.label(doc, kind, ref.key, hiddenUnlocked = hiddenOpen).text.takeIf { it.isNotEmpty() }
    }.joinToString(WorldCopy.SEP)

    fun kindOf(kind: ItemRefKind): WorldKind = when (kind) {
        ItemRefKind.TERM -> WorldKind.TERM
        ItemRefKind.PLACE -> WorldKind.PLACE
        ItemRefKind.FACTION -> WorldKind.FACTION
    }
}
