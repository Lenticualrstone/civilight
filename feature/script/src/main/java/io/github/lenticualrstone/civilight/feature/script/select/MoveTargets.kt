package io.github.lenticualrstone.civilight.feature.script.select

import io.github.lenticualrstone.civilight.domain.script.BlockPosition
import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.feature.script.records.BlockLine
import io.github.lenticualrstone.civilight.feature.script.records.BlockPreview
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * `옮기기` (SCR-37, Q-17 `[PROPOSED]` destination picker): first a scene from the scene list
 * (chapters › episodes › scenes, as `1-7 씬 3 · 꺼진 등불`), then a position inside it: `맨 앞`, or
 * below one of its blocks (`#044 아래`, the words of T-22-08). The moved blocks are not offered.
 */
object MoveTargets {
    /** A scene row: `1-7 씬 3` + its title (`미정` without one); [current] = the scene the first moved block is in. */
    data class SceneRow(val sceneKey: Key, val location: String, val title: String, val current: Boolean)

    /** A position row: `맨 앞`, or `#044 아래` over the block's line. */
    data class PositionRow(val position: BlockPosition, val label: String, val line: BlockLine?)

    /**
     * Every live scene in manuscript order; locked hidden scenes are left out. Scenes of 1-level
     * projects read `씬 3` as `Labels.sceneLocation` gives them.
     */
    fun scenes(doc: ProjectDoc, moving: Collection<Key>, unlocked: Boolean): List<SceneRow> {
        val from = moving.firstNotNullOfOrNull { doc.index.block(it)?.sceneKey }
        return doc.manuscript.liveScenes.filter { unlocked || !it.hidden }.map { s ->
            SceneRow(s.key, Labels.sceneLocation(doc, s.key), Labels.titleOrUndecided(s), s.key == from)
        }
    }

    /** Positions inside [sceneKey]: `맨 앞`, then below each live block that is not moving. */
    fun positions(doc: ProjectDoc, sceneKey: Key, moving: Collection<Key>, unlocked: Boolean): List<PositionRow> {
        val set = moving.toSet()
        val first = PositionRow(BlockPosition.SceneStart(sceneKey), MoveCopy.FIRST, null)
        return listOf(first) + doc.manuscript.liveBlocksOf(sceneKey).filter { it.key !in set }.map { b ->
            PositionRow(BlockPosition.After(b.key), MoveCopy.below(Labels.number(b.number)), BlockPreview.of(doc, b, unlocked))
        }
    }
}

/** Copy of select mode and the move picker (SCR-36 … SCR-40); linted with the screen's copy. */
object MoveCopy {
    /** Header title and right `모두` (board `RSSelect`); `완료` is `ScriptCopy.DONE`. */
    const val TITLE = "블록 고르기"
    const val ALL = "모두"

    /** Checkbox and grip labels (board `RSSelect` `aria-label`). */
    const val CHECK = "고르기"
    const val GRIP = "끌어서 옮기기"

    /** Move picker (not drawn): title = the bar's `옮기기`; `맨 앞` and `#044 아래` (T-12-15, T-22-08). */
    const val MOVE = "옮기기"
    const val FIRST = "맨 앞"
    fun below(number: String): String = "$number 아래"

    /** The clipboard strip over the command row: `블록 3개 · 잘라내기` / `블록 3개 · 복사` (spec words). */
    const val CUT = "잘라내기"
    const val COPY = "복사"
    fun clipboard(count: Int, cut: Boolean): String = "블록 ${count}개 · ${if (cut) CUT else COPY}"

    /** Paste: the spec names no word for it (SCR-40 "Pasting"). */
    const val PASTE = "붙이기"

    /** Back from the position list to the scene list (DS `‹` icon label, as the shell's `뒤로`). */
    const val BACK = "뒤로"

    val all: List<String> = listOf(TITLE, ALL, CHECK, GRIP, MOVE, FIRST, below("#044"), CUT, COPY, clipboard(3, true), clipboard(2, false), PASTE, BACK)
    val buttons: List<String> = listOf(ALL, MOVE, CUT, COPY, PASTE)
}
