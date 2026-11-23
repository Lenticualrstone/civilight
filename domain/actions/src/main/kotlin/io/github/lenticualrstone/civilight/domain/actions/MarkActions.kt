package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.marks.Foreshadow
import io.github.lenticualrstone.civilight.model.marks.MarkAnchor
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.script.BlockKind

/**
 * (0.2.0, N-41, SCR-77) Foreshadowing writes: `복선 심기`, `다시 언급`, `거두기`. Each is one undo step
 * for the caller. Reads (list, filters `열림 · 거둠 · 끝냄`, counts, where a mark is drawn after a
 * block delete) are `core:model` `Marks`; memo `할 일` is [BlockFieldActions.setTodo].
 */
object MarkActions {
    /**
     * `복선 심기` on a live block of any kind except `작가 메모`. [name] is required (one line);
     * [payoffUnitKey] is the optional `거둘 곳` episode. Null when refused.
     */
    fun plant(doc: ProjectDoc, blockKey: Key, name: String, payoffUnitKey: Key?, ctx: ActionContext): Pair<ProjectDoc, Foreshadow>? {
        val block = doc.index.block(blockKey)?.takeIf { doc.liveness.isLive(it) } ?: return null
        if (block.kind.known == BlockKind.MEMO) return null
        val n = oneLine(name) ?: return null
        val f = Foreshadow(
            key = ctx.newKey(),
            name = n,
            plant = MarkAnchor(blockKey),
            payoffUnitKey = payoffUnitKey,
            createdAt = ctx.now,
            updatedAt = ctx.now,
            updatedBy = ctx.device,
        )
        return doc.copy(story = doc.story.copy(foreshadows = doc.story.foreshadows + f)) to f
    }

    /** `다시 언급`: adds [blockKey] as a mention (once). */
    fun mention(doc: ProjectDoc, foreshadowKey: Key, blockKey: Key, ctx: ActionContext): ProjectDoc =
        update(doc, foreshadowKey, blockKey, ctx) { f ->
            if (f.mentions.any { it.blockKey == blockKey }) f else f.copy(mentions = f.mentions + MarkAnchor(blockKey))
        }

    /** `거두기`: [blockKey] becomes the payoff and the item is `거둠`. */
    fun payoff(doc: ProjectDoc, foreshadowKey: Key, blockKey: Key, ctx: ActionContext): ProjectDoc =
        update(doc, foreshadowKey, blockKey, ctx) { it.copy(payoff = MarkAnchor(blockKey)) }

    /** Name and `거둘 곳` of an item (the sheet's `저장`). */
    fun edit(doc: ProjectDoc, foreshadowKey: Key, name: String, payoffUnitKey: Key?, ctx: ActionContext): ProjectDoc {
        val n = oneLine(name) ?: return doc
        return map(doc, foreshadowKey, ctx) { it.copy(name = n, payoffUnitKey = payoffUnitKey) }
    }

    private fun update(doc: ProjectDoc, key: Key, blockKey: Key, ctx: ActionContext, change: (Foreshadow) -> Foreshadow): ProjectDoc {
        if (doc.index.block(blockKey)?.let { doc.liveness.isLive(it) } != true) return doc
        return map(doc, key, ctx, change)
    }

    private fun map(doc: ProjectDoc, key: Key, ctx: ActionContext, change: (Foreshadow) -> Foreshadow): ProjectDoc {
        var changed = false
        val next = doc.story.foreshadows.map { f ->
            if (f.key != key || f.deletedAt != null) return@map f
            val n = change(f)
            if (n == f) f else n.copy(updatedAt = ctx.now, updatedBy = ctx.device).also { changed = true }
        }
        return if (changed) doc.copy(story = doc.story.copy(foreshadows = next)) else doc
    }

    private fun oneLine(name: String): String? = name.nfc().replace(Regex("\\s*[\\r\\n]+\\s*"), " ").trim().ifEmpty { null }
}
