package io.github.lenticualrstone.civilight.feature.script.links

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.CharacterActions
import io.github.lenticualrstone.civilight.domain.script.EditContext
import io.github.lenticualrstone.civilight.domain.script.Links
import io.github.lenticualrstone.civilight.domain.trash.Fixes
import io.github.lenticualrstone.civilight.domain.trash.Messages
import io.github.lenticualrstone.civilight.domain.trash.RestoreOps
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.domain.trash.TrashContext
import io.github.lenticualrstone.civilight.feature.script.state.BlockRow
import io.github.lenticualrstone.civilight.feature.script.terms.TermMarksModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.FactionCard
import io.github.lenticualrstone.civilight.navigation.PlaceCard
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.TermCard
import io.github.lenticualrstone.civilight.domain.world.term.TermCandidate

/** What a tap on a block's text hit (SCR-55, SCR-56, SCR-59), when it is not plain text. */
sealed interface BlockTap {
    /** An `@` link run. */
    data class Link(val link: TextRun.Link) : BlockTap

    /** A `용어 후보` span (layer on). */
    data class Candidate(val candidate: TermCandidate) : BlockTap

    /** The `⌁ 작가 메모 · 레코더 …` prefix of a memo the recorder put in. */
    data class Source(val recordKey: Key) : BlockTap
}

/** The deleted-link card (SCR-55, rule 20): `지운 인물 · 노아` or `영구 삭제 · 노아`, and its buttons. */
data class LinkCard(val title: String, val buttons: List<String>, val canRestore: Boolean, val targetKind: LinkTargetKind?, val targetKey: Key)

/**
 * `@` links in block text as the reader taps them (SCR-55). A live link opens its item's card in
 * its area; a locked hidden link asks for the unlock; a deleted one opens the card with `다른 항목에
 * 잇기 · 글로 풀기 · 되살리기 · 이대로 두기` (`되살리기` gone after permanent deletion). The block edits
 * are `domain:script` `Links.retarget` / `unlink`; `이대로 두기` is `domain:trash` `Fixes.keep`
 * (acknowledgement `[item, block]`); `되살리기` is `domain:actions` `CharacterActions.restore` for a
 * character and `domain:trash` `RestoreOps.restore` for the other kinds.
 */
object LinkFixes {
    /** A typed `@` (one character inserted, nothing replaced) opens the link picker (SCR-55). */
    fun opensPicker(inserted: String, start: Int, end: Int): Boolean = inserted == "@" && start == end

    /** What the character at raw offset [raw] (or the memo prefix, [inPrefix]) of [row] is. */
    fun tapAt(row: BlockRow, raw: Int?, inPrefix: Boolean): BlockTap? {
        if (inPrefix) return row.recorderRecordKey?.let(BlockTap::Source)
        raw ?: return null
        TermMarksModel.hit(row.termCandidates, raw)?.let { return BlockTap.Candidate(it) }
        return linkAt(row.runs, raw)?.let(BlockTap::Link)
    }

    /** The link run holding the character at raw offset [raw]. */
    fun linkAt(runs: List<TextRun>, raw: Int): TextRun.Link? {
        var at = 0
        for (run in runs) {
            val length = run.shownText.length
            if (run is TextRun.Link && raw >= at && raw < at + length) return run
            at += length
        }
        return null
    }

    /** The card a live target opens in its area, or null (hidden, deleted, a kind with no card here). */
    fun liveRoute(projectKey: Key, link: TextRun.Link): Route? {
        val key = link.target.key
        return when (link.target.kind.known) {
            LinkTargetKind.CHARACTER -> CharacterProfile(projectKey, key)
            LinkTargetKind.TERM -> TermCard(projectKey, key)
            LinkTargetKind.PLACE -> PlaceCard(projectKey, key)
            LinkTargetKind.FACTION -> FactionCard(projectKey, key)
            else -> null
        }
    }

    /** The card of a deleted or purged target in [blockKey]'s text; null when the link is live, hidden or gone. */
    fun card(doc: ProjectDoc, blockKey: Key, linkKey: Key, unlocked: Boolean): LinkCard? {
        val link = find(doc, blockKey, linkKey) ?: return null
        val kind = link.target.kind.known
        return when (val state = Links.state(doc, link, unlocked)) {
            is Links.State.Deleted -> LinkCard(state.card, state.buttons, canRestore = true, kind, link.target.key)
            is Links.State.Purged -> LinkCard(state.chip, state.buttons, canRestore = false, kind, link.target.key)
            else -> null
        }
    }

    fun find(doc: ProjectDoc, blockKey: Key, linkKey: Key): TextRun.Link? {
        val block = doc.index.block(blockKey) ?: return null
        return (block.runs + block.options.flatMap { it.runs }).filterIsInstance<TextRun.Link>().firstOrNull { it.linkKey == linkKey }
    }

    /** `다른 항목에 잇기`: the same link (key, text) to another character, term or place. */
    fun relink(doc: ProjectDoc, blockKey: Key, linkKey: Key, kind: LinkTargetKind, targetKey: Key, ctx: EditContext): ProjectDoc =
        Links.retarget(doc, blockKey, linkKey, kind, targetKey, ctx)

    /** `글로 풀기`: the text stays, the link goes. */
    fun unlink(doc: ProjectDoc, blockKey: Key, linkKey: Key, ctx: EditContext): ProjectDoc = Links.unlink(doc, blockKey, linkKey, ctx)

    /** `이대로 두기`: this reference moves to `맞다고 표시한 것`; the dotted look stays. */
    fun keep(doc: ProjectDoc, targetKey: Key, blockKey: Key, ctx: TrashContext): ProjectDoc =
        Fixes.keep(doc, targetKey, ctx, referencingKey = blockKey).doc

    /** The reference was kept (by this block or for the whole item). */
    fun isKept(doc: ProjectDoc, targetKey: Key, blockKey: Key): Boolean =
        Fixes.isKept(doc, targetKey) || doc.checks.acknowledgements.any { it.subjectKeys == listOf(targetKey, blockKey) }

    /** The project after `되살리기` and its toast, or null when nothing was restored (not found, needs authentication). */
    fun restore(doc: ProjectDoc, kind: LinkTargetKind?, targetKey: Key, ctx: ActionContext, authenticated: Boolean): Pair<ProjectDoc, String?>? {
        if (kind == LinkTargetKind.CHARACTER) {
            val result = CharacterActions.restore(doc, targetKey, ctx, authenticated)
            return (result.outcome as? RestoreOutcome.Restored)?.let { it.doc to result.toast?.message }
        }
        val entryKey = when (kind) {
            LinkTargetKind.TERM -> doc.index.term(targetKey)?.trashEntryKey
            LinkTargetKind.PLACE -> doc.index.place(targetKey)?.trashEntryKey
            LinkTargetKind.FACTION -> doc.index.faction(targetKey)?.trashEntryKey
            else -> null
        } ?: return null
        val outcome = RestoreOps.restore(doc, entryKey, ctx.trash, authenticated) as? RestoreOutcome.Restored ?: return null
        return outcome.doc to Messages.restoreToast(outcome.report).message
    }
}
