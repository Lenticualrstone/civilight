package io.github.lenticualrstone.civilight.feature.cast.profile

import io.github.lenticualrstone.civilight.domain.cast.Appearances
import io.github.lenticualrstone.civilight.domain.trash.HidingLocks
import io.github.lenticualrstone.civilight.domain.trash.RefState
import io.github.lenticualrstone.civilight.domain.trash.Resolution
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun

/** One script appearance entry extracted automatically from the manuscript for `작중 행적`. */
internal data class ScriptAppearanceEntry(
    val sceneKey: Key,
    val sceneTitle: String,
    val blockKey: Key,
    val isDialogue: Boolean,
    val excerpt: String,
)

/** One scene card of `작중 행적`: the first [AppearanceTrace.PER_SCENE] rows and how many more there are. */
internal data class AppearanceSceneGroup(
    val sceneKey: Key,
    val sceneTitle: String,
    val entries: List<ScriptAppearanceEntry>,
) {
    fun visible(expanded: Boolean): List<ScriptAppearanceEntry> = if (expanded) entries else entries.take(AppearanceTrace.PER_SCENE)

    /** Rows behind the `더 보기 · n` row (0 = no row). */
    fun more(expanded: Boolean): Int = if (expanded) 0 else (entries.size - AppearanceTrace.PER_SCENE).coerceAtLeast(0)
}

/**
 * (0.5.0) `작중 행적`: the blocks that make a character appear, by the same rule as the appearance bars
 * ([Appearances]): live scenes in manuscript order, hidden scenes (or scenes under a hidden episode or
 * chapter) only while hidden items are open, speaker or `@` in game-output text. Rule 19: `@` links to
 * locked hidden items read `숨긴 인물` / `숨긴 항목` in the excerpts.
 */
internal object AppearanceTrace {
    const val PER_SCENE = 8
    private const val EXCERPT = 96

    fun scan(doc: ProjectDoc, characterKey: Key, hiddenUnlocked: Boolean): List<ScriptAppearanceEntry> {
        val scenes = Appearances.scenesByCharacter(doc, hiddenUnlocked)[characterKey].orEmpty()
        val out = ArrayList<ScriptAppearanceEntry>()
        for (scene in scenes) {
            val title = scene.title?.takeIf { it.isNotBlank() } ?: "씬"
            for (block in doc.manuscript.liveBlocksOf(scene.key)) {
                if (characterKey !in Appearances.charactersOf(block)) continue
                val isSpeaker = block.speakerKey == characterKey || block.options.any { it.speakerKey == characterKey }
                val plain = excerptOf(doc, block, characterKey, hiddenUnlocked)
                if (plain.isBlank()) continue
                out += ScriptAppearanceEntry(
                    sceneKey = scene.key,
                    sceneTitle = title,
                    blockKey = block.key,
                    isDialogue = isSpeaker && block.kind.known == BlockKind.DIALOGUE,
                    excerpt = if (plain.length > EXCERPT) plain.take(EXCERPT) + "…" else plain,
                )
            }
        }
        return out
    }

    fun group(entries: List<ScriptAppearanceEntry>): List<AppearanceSceneGroup> =
        entries.groupBy { it.sceneKey }.map { (key, list) -> AppearanceSceneGroup(key, list.first().sceneTitle, list) }

    /** The block text; a choice block uses the option lines that make the character appear (else all). */
    private fun excerptOf(doc: ProjectDoc, block: Block, characterKey: Key, unlocked: Boolean): String {
        val main = flat(doc, block.runs, unlocked)
        if (main.isNotBlank() || block.options.isEmpty()) return main
        val own = block.options.filter { o ->
            o.speakerKey == characterKey || o.runs.any { it is TextRun.Link && it.target.key == characterKey }
        }.ifEmpty { block.options }
        return own.joinToString(" · ") { flat(doc, it.runs, unlocked) }.trim()
    }

    private fun flat(doc: ProjectDoc, runs: List<TextRun>, unlocked: Boolean): String = runs.joinToString("") { run ->
        when (run) {
            is TextRun.Plain -> run.text
            is TextRun.Paren -> "(${run.text})"
            is TextRun.Link -> Resolution.resolve(doc, run.target, HidingLocks(itemsLocked = !unlocked))
                ?.takeIf { it.state == RefState.HIDDEN_LOCKED }?.label ?: run.text
            TextRun.Break -> " "
            else -> ""
        }
    }.trim()
}
