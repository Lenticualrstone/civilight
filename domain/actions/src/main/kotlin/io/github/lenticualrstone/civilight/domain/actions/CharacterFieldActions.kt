package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc

/** (0.2.0, N-43) Character fields added in 0.2.0: `말투 · 한 줄 요약` (CST-72) and `이 탭 잠그기` (CST-73). */
object CharacterFieldActions {
    /** One line: NFC, trimmed, each line break becomes one space; blank is stored as null. */
    fun normalizeSummary(value: String?): String? =
        value?.nfc()?.replace(LINE_BREAKS, " ")?.trim()?.ifEmpty { null }

    /** `한 줄 요약` `저장` (no `◆` history, U-10). */
    fun setSpeechSummary(doc: ProjectDoc, characterKey: Key, value: String?, ctx: ActionContext): ProjectDoc =
        update(doc, characterKey, ctx) { it.copy(speechSummary = normalizeSummary(value)) }

    /**
     * `이 탭 잠그기`. Turning it off asks for the device lock first; that prompt is the caller's
     * (`core:ui` `LockRules`), this only writes the value.
     */
    fun setSecretTabLocked(doc: ProjectDoc, characterKey: Key, locked: Boolean, ctx: ActionContext): ProjectDoc =
        update(doc, characterKey, ctx) { it.copy(secretTabLocked = locked) }

    private fun update(doc: ProjectDoc, key: Key, ctx: ActionContext, change: (Character) -> Character): ProjectDoc {
        var changed = false
        val characters = doc.cast.characters.map { c ->
            if (c.key != key) return@map c
            val next = change(c)
            if (next == c) c else next.copy(updatedAt = ctx.now, updatedBy = ctx.device).also { changed = true }
        }
        return if (changed) doc.copy(cast = doc.cast.copy(characters = characters)) else doc
    }

    private val LINE_BREAKS = Regex("\\s*(\\r\\n|\\r|\\n|\\u2028|\\u2029)\\s*")
}
