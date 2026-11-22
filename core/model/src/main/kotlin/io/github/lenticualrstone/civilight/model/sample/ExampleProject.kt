package io.github.lenticualrstone.civilight.model.sample

import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.StoryIndexFile
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.text.plainText

/**
 * The canonical example project 「잿빛 새벽의 등불」 (R-15), as one [ProjectDoc].
 *
 * Fully deterministic: every key is a constant of [ExampleKeys] and every timestamp a constant of
 * [ExampleClock] (the fixtures' "now" is [ExampleClock.NOW], 2026-11-22 09:00 KST), so
 * `build() == build()`. Live and trashed rows are held together as in the working store; within
 * every list the non-live rows come last, which is the order `ProjectFileSet.split(doc).toDoc()`
 * reproduces. The recorder library slice is [ExampleRecorder]. Facts and spec reconciliation:
 * `docs/status/fixture.md`.
 */
object ExampleProject {
    const val NAME = ExampleSettings.NAME

    fun build(): ProjectDoc {
        val episodes = ExampleScript.episodeFiles()
        val lastEdited = episodes.values.flatMap { it.blocks }.first { it.key == ExampleKeys.BLOCK_044 }
        return ProjectDoc(
            project = ExampleSettings.project(),
            story = StoryIndexFile(units = ExampleStory.units()),
            episodes = episodes,
            cast = ExampleCast.castFile(),
            world = ExampleWorld.worldFile(),
            maps = ExampleMaps.maps().associateBy { it.key },
            changes = ExampleChanges.changesFile(),
            checks = ExampleExtras.checks(),
            deletions = ExampleExtras.deletions(),
            canvases = ExampleExtras.canvases().associateBy { it.key },
            pages = ExampleExtras.pages().associateBy { it.key },
            assets = ExampleExtras.assets(),
            trash = ExampleExtras.trash(),
            app = ExampleExtras.app(lastEdited.runs.plainText.length),
        )
    }

    /** The 서재 row of the bundled example (LIB-17): read-only, flagged as the example. */
    fun libraryEntry(): LibraryEntry = LibraryEntry(
        key = ExampleKeys.PROJECT,
        name = NAME,
        rating = Rating.R15.open(),
        isExample = true,
        readOnly = true,
        lastOpenedAt = ExampleClock.LAST_EDIT,
        createdAt = ExampleClock.PROJECT_CREATED,
        updatedAt = ExampleClock.LAST_EDIT,
    )
}
