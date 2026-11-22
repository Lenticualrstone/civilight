package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/** Which scenes an export covers and how hidden content and voice IDs are handled (SCR-63/64). */
data class ExportScope(
    /** Units chosen in the export sheet (chapters, episodes or scenes); empty = the whole project. */
    val unitKeys: List<Key> = emptyList(),
    /** `숨긴 씬 {n} · 넣기 · 빼기`: JSON and CSV default `넣기` (after one biometric confirmation). */
    val includeHiddenScenes: Boolean = true,
    /** SCR Q-10 default: parentheticals are written as `(…)` in the text. */
    val includeParens: Boolean = true,
) {
    /** Live scenes in range, manuscript order. */
    fun scenes(doc: ProjectDoc): List<StoryUnit> = doc.manuscript.liveScenes.filter { s ->
        (unitKeys.isEmpty() || s.key in unitKeys || doc.index.ancestors(s).any { it.key in unitKeys }) &&
            (includeHiddenScenes || !s.hidden)
    }

    /** Hidden scenes in range (`숨긴 씬 {n}` in the export sheet). */
    fun hiddenScenes(doc: ProjectDoc): List<StoryUnit> = copy(includeHiddenScenes = true).scenes(doc).filter { it.hidden }

    /** Sheet row `숨긴 씬 1 · 1-7 씬 4`. */
    fun hiddenRow(doc: ProjectDoc): String? {
        val hidden = hiddenScenes(doc)
        if (hidden.isEmpty()) return null
        return "숨긴 씬 ${hidden.size} · " + hidden.joinToString(" · ") { Labels.sceneLocation(doc, it.key) }
    }

    /** The preselected chip of the hidden-scene row: `넣기` or `빼기`. */
    val hiddenChoice: String get() = if (includeHiddenScenes) "넣기" else "빼기"

    companion object {
        /** Engine JSON: hidden scenes `넣기` by default (one biometric confirmation, export area). */
        fun json(unitKeys: List<Key> = emptyList()) = ExportScope(unitKeys, includeHiddenScenes = true)

        /** Dialogue CSV: hidden scenes `넣기` by default. */
        fun csv(unitKeys: List<Key> = emptyList()) = ExportScope(unitKeys, includeHiddenScenes = true)

        /** Script PDF (SCR-65): hidden scenes `빼기` by default. */
        fun pdf(unitKeys: List<Key> = emptyList()) = ExportScope(unitKeys, includeHiddenScenes = false)
    }
}

/** Voice-ID fixing shared by the JSON and CSV exports (LIB-46): only what is written gets fixed. */
internal object ExportVoices {
    fun enabled(doc: ProjectDoc): Boolean = doc.project.script.voiceIdEnabled

    /**
     * Fixes the voice IDs of the written dialogue blocks and spoken options at [at] and records
     * `lastExport`. Manual-format IDs are stored as written.
     */
    fun fix(doc: ProjectDoc, written: List<Block>, at: Timestamp): ProjectDoc {
        if (!enabled(doc)) return doc
        val updates = written.mapNotNull { b ->
            var changed = false
            var voice = b.voice
            if (b.kind.known == BlockKind.DIALOGUE) {
                VoiceIds.currentId(doc, b)?.let { id ->
                    voice = VoiceIds.exported(b.voice, id, VoiceIds.spokenText(b), b.speakerKey, at)
                    changed = true
                }
            }
            val options = b.options.map { o ->
                val id = VoiceIds.currentIdOfOption(doc, b, o) ?: return@map o
                changed = true
                o.copy(voice = VoiceIds.exported(o.voice, id, VoiceIds.spokenText(b, o), o.speakerKey, at))
            }
            if (!changed) null else b.key to b.copy(voice = voice, options = options)
        }.toMap()
        return doc.replaceBlocks(updates)
    }
}
