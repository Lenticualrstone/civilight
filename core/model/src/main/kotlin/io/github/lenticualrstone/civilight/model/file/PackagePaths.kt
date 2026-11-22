package io.github.lenticualrstone.civilight.model.file

import io.github.lenticualrstone.civilight.model.Key

/** Entry paths of a `.civilight` package (§7.2), also the file layout of the working store (§8.2). */
object PackagePaths {
    const val MIMETYPE = "mimetype"
    const val MIME_PROJECT = "application/vnd.civilight.project"
    const val MIME_RECORDER = "application/vnd.civilight.recorder"

    const val MANIFEST = "manifest.json"
    const val PROJECT = "project.json"
    const val STORY_INDEX = "story/index.json"
    const val CAST = "cast.json"
    const val WORLD = "world.json"
    const val CHANGES = "changes.json"
    const val ASSETS_INDEX = "assets/index.json"
    const val RECORDER = "recorder.json"
    const val CHECKS = "checks.json"
    const val TOMBSTONES = "tombstones.json"
    const val DELETIONS = "deletions.json"
    const val PREVIEW = "preview.webp"
    const val ENCRYPTION = "encryption.json"
    const val PAYLOAD = "payload.bin"

    const val TRASH_DIR = "trash/"
    const val TRASH_INDEX = "trash/index.json"

    /** `.civilight-recorder`: all records. */
    const val RECORDER_LIBRARY = "records.json"

    /** Working store app-only files (§8.2). */
    const val APP_ACTIVITY = "_app/activity.json"
    const val APP_VIEW = "_app/view.json"
    const val APP_UNDO_DIR = "_app/undo/"

    fun episode(numberingUnitKey: Key) = "story/$numberingUnitKey.json"
    fun map(mapKey: Key) = "maps/$mapKey.json"
    fun canvas(canvasKey: Key) = "canvas/$canvasKey.json"
    fun page(pageKey: Key) = "pages/$pageKey.json"
    fun asset(hash: String, ext: String) = "assets/$hash.$ext"

    /** The `trash/` mirror path of a normal file path (§7.9). */
    fun trash(path: String) = TRASH_DIR + path
}
