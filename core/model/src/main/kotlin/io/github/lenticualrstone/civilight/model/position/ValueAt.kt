package io.github.lenticualrstone.civilight.model.position

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget

/**
 * `valueAt` (§4.5) and the script panel's block-level reading (§4.4).
 *
 * Every function returns the winning **record** (or records), never the value itself: `null` means
 * "no record applies, use the base value" (`Character.fields`, `displayNameBase`, relation base slots,
 * `MapRegion.vertices`, or `만나기 전` for an empty relation slot), while a record whose `value` is
 * null is a real value (`displayName` null = show `이름`).
 *
 * [isLive] decides which records count (§5.1: not trashed and target live). The default only checks
 * the record's own mark; pass `doc.liveness::isLive` to apply target liveness. Records of an unknown
 * target kind never match. Ties at equal positions go to the later `createdAt`, then the greater key.
 */
private val RECORD_TIE_BREAK: Comparator<ChangeRecord> = compareBy<ChangeRecord> { it.createdAt }.thenBy { it.key }

private fun List<ChangeRecord>.matching(target: ChangeTarget, isLive: (ChangeRecord) -> Boolean): List<ChangeRecord> {
    val id = target.targetId ?: return emptyList()
    return filter { it.target.targetId == id && isLive(it) }
}

/** The live record of [target] with the greatest `pos(anchor) ≤ at`, or null (base value). */
fun ManuscriptIndex.recordAt(
    records: List<ChangeRecord>,
    target: ChangeTarget,
    at: Rank,
    isLive: (ChangeRecord) -> Boolean = { it.deletedAt == null },
): ChangeRecord? = records.matching(target, isLive)
    .map { it to rankOf(it.anchor) }
    .filter { (_, rank) -> rank <= at }
    .maxWithOrNull(compareBy<Pair<ChangeRecord, Rank>> { it.second }.thenBy(RECORD_TIE_BREAK) { it.first })
    ?.first

/** `valueAt(target, P)` for a lens point (§4.5). */
fun ManuscriptIndex.recordAt(
    records: List<ChangeRecord>,
    target: ChangeTarget,
    lens: LensPoint,
    isLive: (ChangeRecord) -> Boolean = { it.deletedAt == null },
): ChangeRecord? = recordAt(records, target, rankOf(lens), isLive)

/**
 * Script panel state for block [blockKey]: the record in force just **before** the block
 * (`pos(anchor) < rank[B]`), or null. Null as well when the block is not in the project.
 */
fun ManuscriptIndex.recordBeforeBlock(
    records: List<ChangeRecord>,
    target: ChangeTarget,
    blockKey: Key,
    isLive: (ChangeRecord) -> Boolean = { it.deletedAt == null },
): ChangeRecord? {
    val rank = blockRankOf(blockKey) ?: return null
    return recordAt(records, target, Rank(rank.value - 1), isLive)
}

/** The `여기서` rows of block [blockKey]: live records of any target with `pos(anchor) = rank[B]`, by tie-break order. */
fun ManuscriptIndex.recordsHere(
    records: List<ChangeRecord>,
    blockKey: Key,
    isLive: (ChangeRecord) -> Boolean = { it.deletedAt == null },
): List<ChangeRecord> {
    val rank = blockRankOf(blockKey) ?: return emptyList()
    if (!isLiveBlock(blockKey)) return emptyList()
    return records.filter { isLive(it) && it.target.targetId != null && rankOf(it.anchor) == rank }
        .sortedWith(RECORD_TIE_BREAK)
}

/** Live records of [target] in position order (history list), ties by `createdAt` then key. */
fun ManuscriptIndex.history(
    records: List<ChangeRecord>,
    target: ChangeTarget,
    isLive: (ChangeRecord) -> Boolean = { it.deletedAt == null },
): List<ChangeRecord> = records.matching(target, isLive)
    .sortedWith(compareBy<ChangeRecord> { rankOf(it.anchor) }.thenBy(RECORD_TIE_BREAK) { it })

/**
 * `revealPos(term)` (§4.5): the rank of the live `termReveal` record's anchor, or null when the term
 * has no reveal (then no `▲`).
 */
fun ManuscriptIndex.revealRank(
    records: List<ChangeRecord>,
    termKey: Key,
    isLive: (ChangeRecord) -> Boolean = { it.deletedAt == null },
): Rank? = records.matching(ChangeTarget.TermReveal(termKey), isLive)
    .minWithOrNull(compareBy<ChangeRecord> { rankOf(it.anchor) }.thenBy(RECORD_TIE_BREAK) { it })
    ?.let { rankOf(it.anchor) }

/**
 * `▲ 공개 전 사용` (WLD-20): a link at [linkRank] (its block's or transition's rank) to [termKey]
 * comes before the term's reveal. Block granularity: a link inside the reveal block is after it.
 */
fun ManuscriptIndex.isBeforeReveal(
    records: List<ChangeRecord>,
    termKey: Key,
    linkRank: Rank,
    isLive: (ChangeRecord) -> Boolean = { it.deletedAt == null },
): Boolean = revealRank(records, termKey, isLive)?.let { linkRank < it } ?: false
