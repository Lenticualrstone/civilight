package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.version.EpisodeVersion
import io.github.lenticualrstone.civilight.model.version.VersionBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** (0.2.0 QA, T-U14-25) A kept block moved since into a locked hidden scene of another episode shows no text. */
class VersionDiffMovedTest {
    private val ctx = testCtx()

    private fun versionOf(doc: io.github.lenticualrstone.civilight.model.doc.ProjectDoc): EpisodeVersion {
        val blocks = doc.manuscript.liveScenes.filter { doc.numberingUnitOfScene(it.key) == F1.E17 }
            .flatMap { doc.manuscript.liveBlocksOf(it.key) }
            .map { VersionBlock(it.key, it.sceneKey, it.number, it.kind, it.speakerKey, it.runs) }
        return EpisodeVersion(key = "01JZ000000VERS000000000099", name = "v", createdAt = 0L, blocks = blocks)
    }

    private fun hide(doc: io.github.lenticualrstone.civilight.model.doc.ProjectDoc, scene: String) =
        doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == scene) it.copy(hidden = true) else it }))

    @Test fun aBlockMovedIntoALockedHiddenSceneOfAnotherEpisodeIsNotStruck() {
        val doc = F1.build()
        val v = versionOf(doc)
        val moved = BlockMove.move(doc, listOf(F1.b(50)), BlockPosition.SceneEnd(F1.S18_1), ctx).doc
        assertEquals(F1.S18_1, moved.index.block(F1.b(50))!!.sceneKey)
        // Visible there: the compare shows it as gone from this episode, with its text.
        val open = VersionDiff.rows(v, moved, F1.E17, unlocked = false)
        assertTrue(open.any { it is VersionDiff.Row.Deleted && it.blockKey == F1.b(50) })
        // Hidden and locked there: no row carries it, struck or not; it counts under that scene's hidden row.
        val locked = hide(moved, F1.S18_1)
        val rows = VersionDiff.rows(v, locked, F1.E17, unlocked = false)
        assertTrue(rows.none { it is VersionDiff.Row.Deleted && it.blockKey == F1.b(50) })
        assertEquals(1, rows.filterIsInstance<VersionDiff.Row.HiddenScene>().single { it.sceneKey == F1.S18_1 }.count)
        // Unlocked, the row is back.
        assertTrue(VersionDiff.rows(v, locked, F1.E17, unlocked = true).any { it is VersionDiff.Row.Deleted && it.blockKey == F1.b(50) })
    }
}
