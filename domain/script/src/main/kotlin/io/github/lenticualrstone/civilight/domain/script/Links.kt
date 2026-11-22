package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun

/** How an `@` link in block text is drawn (SCR-55, rules 19 and 20). */
object Links {
    sealed interface State {
        /** Normal link look (`ink` text, 1 px `ink3` underline). */
        data object Live : State

        /** Locked hidden target: the text is drawn as `숨긴 인물` / `숨긴 항목`, emphasis removed. */
        data class Hidden(val text: String) : State

        /** Target in the trash: dotted underline + `◇`; the card reads `지운 인물 · 노아`. */
        data class Deleted(val card: String) : State {
            val buttons: List<String> get() = listOf("다른 항목에 잇기", "글로 풀기", "되살리기", "이대로 두기")
        }

        /** Target permanently deleted: chip `영구 삭제 · 노아`; `되살리기` is gone. */
        data class Purged(val chip: String) : State {
            val buttons: List<String> get() = listOf("다른 항목에 잇기", "글로 풀기", "이대로 두기")
        }
    }

    private fun kindWord(kind: LinkTargetKind?): String = when (kind) {
        LinkTargetKind.CHARACTER -> "인물"
        LinkTargetKind.TERM -> "용어"
        LinkTargetKind.PLACE -> "장소"
        LinkTargetKind.FACTION -> "세력"
        else -> "항목"
    }

    /** State of a link run; [unlocked] = the hidden gate is open. */
    fun state(doc: ProjectDoc, link: TextRun.Link, unlocked: Boolean = false): State {
        val key = link.target.key
        val kind = link.target.kind.known
        val row: Pair<String, Pair<Boolean, Boolean>>? = when (kind) {
            LinkTargetKind.CHARACTER -> doc.index.character(key)?.let { it.name to (it.hidden to (it.deletedAt != null)) }
            LinkTargetKind.TERM -> doc.index.term(key)?.let { it.name to (it.hidden to (it.deletedAt != null)) }
            LinkTargetKind.PLACE -> doc.index.place(key)?.let { it.name to (it.hidden to (it.deletedAt != null)) }
            else -> null
        }
        val tag = doc.index.nameTag(key)
        val hidden = row?.second?.first ?: tag?.hidden ?: false
        if (hidden && !unlocked) return State.Hidden(if (kind == LinkTargetKind.CHARACTER) "숨긴 인물" else "숨긴 항목")
        return when {
            row == null && tag != null -> State.Purged("영구 삭제 · ${tag.name}")
            row == null -> State.Purged("영구 삭제 · ${link.text}")
            row.second.second -> State.Deleted("지운 ${kindWord(kind)} · ${row.first}")
            else -> State.Live
        }
    }

    /** `글로 풀기`: the link run becomes plain text with the same shown text. */
    fun unlink(doc: ProjectDoc, blockKey: Key, linkKey: Key, ctx: EditContext): ProjectDoc {
        val block = doc.index.block(blockKey) ?: return doc
        fun flatten(runs: List<TextRun>) = Runs.normalize(runs.map { if (it is TextRun.Link && it.linkKey == linkKey) TextRun.Plain(it.text) else it })
        return doc.replaceBlock(
            block.copy(
                runs = flatten(block.runs),
                options = block.options.map { it.copy(runs = flatten(it.runs)) },
                updatedAt = ctx.now, updatedBy = ctx.device,
            ),
        )
    }

    /** `다른 항목에 잇기`: the same link (key and text) pointing at another item of a script kind. */
    fun retarget(doc: ProjectDoc, blockKey: Key, linkKey: Key, kind: LinkTargetKind, targetKey: Key, ctx: EditContext): ProjectDoc {
        if (kind !in LinkTargetKind.SCRIPT) return doc
        val block = doc.index.block(blockKey) ?: return doc
        fun swap(runs: List<TextRun>) = runs.map {
            if (it is TextRun.Link && it.linkKey == linkKey) it.copy(target = io.github.lenticualrstone.civilight.model.text.LinkTarget(kind, targetKey)) else it
        }
        return doc.replaceBlock(
            block.copy(runs = swap(block.runs), options = block.options.map { it.copy(runs = swap(it.runs)) }, updatedAt = ctx.now, updatedBy = ctx.device),
        )
    }
}
