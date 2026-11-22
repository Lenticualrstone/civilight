package io.github.lenticualrstone.civilight.domain.recorder.link

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEditContext
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.text.TextRun

/** The card a tap on a deleted card chip opens (LRC-23, rule 20): `지운 인물 · 노아` and its buttons. */
data class DeletedTargetCard(val title: String, val actions: List<String>)

/**
 * Links to deleted characters, places, terms and factions (LRC-23). A plain chip offers
 * `칩 다시 고르기 · 칩 빼기 · 이대로 두기`; an `@` chip mirrors its run and offers `다른 항목에 잇기 ·
 * 글로 풀기 · 되살리기 · 이대로 두기` (after purge without `되살리기`) and never `칩 빼기`.
 * `이대로 두기` stores nothing: the chip stays dotted.
 */
object DeletedTargets {
    fun card(chip: Chip, env: RecorderEnv): DeletedTargetCard? {
        if (chip.display != LinkDisplay.DELETED_ITEM_TRASH && chip.display != LinkDisplay.GONE) return null
        val link = chip.links.singleOrNull() ?: return null
        val word = link.target?.kind?.cardWord ?: return null
        val name = if (chip.display == LinkDisplay.GONE) {
            if (link.lastLabelHidden) RecorderCopy.HIDDEN_ITEM else link.lastLabel.orEmpty()
        } else {
            chip.label
        }
        val title = "${RecorderCopy.DELETED_PREFIX} $word${RecorderCopy.SEP}$name"
        val actions = when {
            !chip.atForm -> listOf(RecorderCopy.RELINK_CHIP, RecorderCopy.REMOVE_CHIP, RecorderCopy.KEEP_AS_IS)
            chip.display == LinkDisplay.GONE -> listOf(RecorderCopy.LINK_OTHER, RecorderCopy.TO_PLAIN_TEXT, RecorderCopy.KEEP_AS_IS)
            else -> listOf(RecorderCopy.LINK_OTHER, RecorderCopy.TO_PLAIN_TEXT, RecorderCopy.RESTORE, RecorderCopy.KEEP_AS_IS)
        }
        return DeletedTargetCard(title, actions)
    }

    /**
     * `글로 풀기`: every run to [target] in the record becomes plain text with the same characters,
     * and the `@` link goes with it (a non-`@` link of the same target stays). The text revision
     * does not change: the characters are the same.
     */
    fun toPlainText(library: RecorderLibrary, recordKey: Key, target: TargetRef, ctx: RecorderEditContext): RecorderLibrary {
        val record = library.record(recordKey) ?: return library
        val runs = Runs.unlink(record.runs) { it.recorderTarget == target }
        val withRecord = library.copy(records = library.records.map {
            if (it.key == recordKey) it.copy(runs = runs, updatedAt = ctx.now, updatedBy = ctx.device) else it
        })
        return LinkEdits.replaceLinksOf(withRecord, recordKey, LinkEdits.syncAt(library.linksOf(recordKey), recordKey, runs, ctx))
    }

    /**
     * `다른 항목에 잇기` on an `@` run: the runs to [from] point at [to] (new link key, same text);
     * links follow.
     */
    fun relinkRuns(library: RecorderLibrary, recordKey: Key, from: TargetRef, to: TargetRef, ctx: RecorderEditContext): RecorderLibrary {
        val record = library.record(recordKey) ?: return library
        val runs = record.runs.map { run ->
            if (run is TextRun.Link && run.recorderTarget == from) {
                Runs.linkRun(ctx.newKey(), to.kind.toLinkTargetKind(), to.key, to.projectKey, run.text)
            } else {
                run
            }
        }
        val withRecord = library.copy(records = library.records.map {
            if (it.key == recordKey) it.copy(runs = runs, updatedAt = ctx.now, updatedBy = ctx.device) else it
        })
        return LinkEdits.replaceLinksOf(withRecord, recordKey, LinkEdits.syncAt(library.linksOf(recordKey), recordKey, runs, ctx))
    }
}
