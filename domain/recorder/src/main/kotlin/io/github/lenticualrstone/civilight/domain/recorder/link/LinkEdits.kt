package io.github.lenticualrstone.civilight.domain.recorder.link

import io.github.lenticualrstone.civilight.domain.recorder.RecorderEditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.text.TextRun

/**
 * Editing the links (`연결`) of one record (LRC-18): a link is a fact, never moves content; a
 * target is linked at most once; `@` links leave only with their text.
 */
object LinkEdits {
    /** Links in chip order: creation order ([PROPOSED] LRC-21), key as tie-break. */
    fun ordered(links: List<RecorderLink>): List<RecorderLink> =
        links.sortedWith(compareBy<RecorderLink> { it.createdAt }.thenBy { it.key })

    fun newLink(recordKey: Key, target: TargetRef, origin: LinkOrigin, ctx: RecorderEditContext): RecorderLink = RecorderLink(
        key = ctx.newKey(), recordKey = recordKey, projectKey = target.projectKey, targetKind = target.kind.open(),
        targetKey = target.key, origin = origin.open(), createdAt = ctx.now, updatedAt = ctx.now, updatedBy = ctx.device,
    )

    /**
     * Adds a link to [target]; a second add of the same target is a no-op. [links] are the links of
     * one record (or one draft), whatever record key they carry yet.
     */
    fun add(links: List<RecorderLink>, recordKey: Key, target: TargetRef, origin: LinkOrigin, ctx: RecorderEditContext): List<RecorderLink> =
        if (links.any { it.pointsAt(target) }) links
        else links + newLink(recordKey, target, origin, ctx)

    /**
     * Chip × (no confirmation, [PROPOSED]). An `@` link, or any link whose target still has an `@`
     * run in [runs], cannot be removed this way: it leaves only by editing the text.
     */
    fun remove(links: List<RecorderLink>, linkKey: Key, runs: List<TextRun> = emptyList()): List<RecorderLink> {
        val link = links.firstOrNull { it.key == linkKey } ?: return links
        if (isAtForm(link, runs)) return links
        return links.filter { it.key != linkKey }
    }

    /**
     * Chip tap → picker → another target ([PROPOSED] replace = remove + add in one step). The new
     * link keeps the old chip's place in the order and counts as the user's own choice (`직접`).
     */
    fun replace(
        links: List<RecorderLink>,
        linkKey: Key,
        target: TargetRef,
        runs: List<TextRun>,
        ctx: RecorderEditContext,
    ): List<RecorderLink> {
        val old = links.firstOrNull { it.key == linkKey } ?: return links
        if (isAtForm(old, runs)) return links
        val rest = links.filter { it.key != linkKey }
        if (rest.any { it.pointsAt(target) }) return rest
        return rest + newLink(old.recordKey, target, LinkOrigin.MANUAL, ctx).copy(createdAt = old.createdAt)
    }

    /** Targets of the `@` runs in [runs]. */
    fun atTargets(runs: List<TextRun>): Set<TargetRef> =
        runs.filterIsInstance<TextRun.Link>().mapNotNull { it.recorderTarget }.toSet()

    /**
     * The chip of [link] shows the `@` form: the link came from an `@`, or an `@` run in the text
     * points at the same target ([PROPOSED] LRC-18 duplicates).
     */
    fun isAtForm(link: RecorderLink, runs: List<TextRun>): Boolean =
        link.origin.known == LinkOrigin.AT || link.target?.let { it in atTargets(runs) } == true

    /**
     * Reconciles links with the `@` runs of [runs] (LRC-17): every run target is linked (an `@`
     * link when it was not linked yet); `@` links whose run is gone are removed. Links of other
     * origins are never removed here.
     */
    fun syncAt(links: List<RecorderLink>, recordKey: Key, runs: List<TextRun>, ctx: RecorderEditContext): List<RecorderLink> {
        val targets = atTargets(runs)
        var out = links.filter { it.origin.known != LinkOrigin.AT || it.target in targets }
        for (target in targets) out = add(out, recordKey, target, LinkOrigin.AT, ctx)
        return out
    }

    // --- Library variants ---

    fun replaceLinksOf(library: RecorderLibrary, recordKey: Key, links: List<RecorderLink>): RecorderLibrary =
        library.copy(links = library.links.filter { it.recordKey != recordKey } + links.map { it.copy(recordKey = recordKey) })

    fun add(library: RecorderLibrary, recordKey: Key, target: TargetRef, origin: LinkOrigin, ctx: RecorderEditContext): RecorderLibrary =
        replaceLinksOf(library, recordKey, add(library.linksOf(recordKey), recordKey, target, origin, ctx))

    fun remove(library: RecorderLibrary, recordKey: Key, linkKey: Key): RecorderLibrary {
        val runs = library.record(recordKey)?.runs.orEmpty()
        return replaceLinksOf(library, recordKey, remove(library.linksOf(recordKey), linkKey, runs))
    }

    fun replace(library: RecorderLibrary, recordKey: Key, linkKey: Key, target: TargetRef, ctx: RecorderEditContext): RecorderLibrary {
        val runs = library.record(recordKey)?.runs.orEmpty()
        return replaceLinksOf(library, recordKey, replace(library.linksOf(recordKey), linkKey, target, runs, ctx))
    }
}
