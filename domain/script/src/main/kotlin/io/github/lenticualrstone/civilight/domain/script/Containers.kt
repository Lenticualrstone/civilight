package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.checks.Acknowledgement
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.world.RejectionContainerKind
import io.github.lenticualrstone.civilight.model.world.TermRejection

/**
 * Rows keyed on block keys that follow block edits (§5.5 last bullet): `아님` decisions
 * ([TermRejection]) and acknowledgements. Occurrence indexes are re-counted with plain substring
 * counts of the matched text (`ED`; the world area owns candidate matching).
 */
internal object Containers {
    fun occurrences(text: String, needle: String): Int {
        if (needle.isEmpty()) return 0
        var count = 0
        var i = text.indexOf(needle)
        while (i >= 0) {
            count++
            i = text.indexOf(needle, i + needle.length)
        }
        return count
    }

    private fun ProjectDoc.mapRejections(transform: (TermRejection) -> TermRejection): ProjectDoc =
        copy(world = world.copy(terms = world.terms.map { t -> t.copy(rejections = t.rejections.map(transform)) }))

    private fun TermRejection.inBlock(key: Key): Boolean =
        containerKind.known == RejectionContainerKind.BLOCK && containerKey == key

    /** Split: rejections of matches now in the back part move to the new block. */
    fun splitRejections(doc: ProjectDoc, fromKey: Key, toKey: Key, frontText: String): ProjectDoc = doc.mapRejections { r ->
        if (!r.inBlock(fromKey) || r.optionKey != null) return@mapRejections r
        val inFront = occurrences(frontText, r.matchedText)
        if (r.occurrence < inFront) r else r.copy(containerKey = toKey, occurrence = r.occurrence - inFront)
    }

    /** Merge: rejections and acknowledgements of the later block follow it into the merged block. */
    fun mergeContainers(doc: ProjectDoc, laterKey: Key, intoKey: Key, previousText: String): ProjectDoc {
        val withRejections = doc.mapRejections { r ->
            if (!r.inBlock(laterKey)) r else r.copy(containerKey = intoKey, occurrence = r.occurrence + occurrences(previousText, r.matchedText))
        }
        return withRejections.copy(
            checks = withRejections.checks.copy(
                acknowledgements = withRejections.checks.acknowledgements.map { a ->
                    if (laterKey !in a.subjectKeys) a else a.copy(subjectKeys = a.subjectKeys.map { if (it == laterKey) intoKey else it }.distinct())
                },
            ),
        )
    }

    /**
     * Acknowledgements about blocks that stop being live are deleted (`맞음` marks go with the
     * block); `deleted.item` acks are exempt (§2.9). Returns the doc and the removed acks (for undo).
     */
    fun dropAcksOf(doc: ProjectDoc, blockKeys: Set<Key>): Pair<ProjectDoc, List<Acknowledgement>> {
        if (blockKeys.isEmpty()) return doc to emptyList()
        val (removed, kept) = doc.checks.acknowledgements.partition { a ->
            a.checkType != CheckType.DELETED_ITEM && a.subjectKeys.any { it in blockKeys }
        }
        return doc.copy(checks = doc.checks.copy(acknowledgements = kept)) to removed
    }
}
