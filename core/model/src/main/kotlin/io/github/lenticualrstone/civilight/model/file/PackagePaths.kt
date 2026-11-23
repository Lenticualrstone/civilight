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

    /** Gallery file units `gallery/<ownerKey>.json` (55 §3.3). */
    const val GALLERY_DIR = "gallery/"

    /** Originals of gallery items in a package: `gallery/files/<hash>.<ext>` (never in the working store). */
    const val GALLERY_FILES_DIR = "gallery/files/"
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
    fun gallery(ownerKey: Key) = "$GALLERY_DIR$ownerKey.json"

    /** (0.2.0, 00 §13.5) `versions/`: one file per numbering unit with versions. */
    const val VERSIONS_DIR = "versions/"

    /** (0.2.0) `versions/<unitKey>.json`. */
    fun versions(unitKey: Key) = "$VERSIONS_DIR$unitKey.json"

    /** (0.2.0) True for `versions/<key>.json` (one level, not a nested path). */
    fun isVersionsUnit(path: String): Boolean =
        path.startsWith(VERSIONS_DIR) && path.endsWith(".json") && path.count { it == '/' } == 1

    /** (0.2.0) The unit key of a `versions/<unitKey>.json` path. */
    fun versionsUnitOf(path: String): Key = path.removePrefix(VERSIONS_DIR).removeSuffix(".json")
    fun galleryOriginal(hash: String, ext: String) = "$GALLERY_FILES_DIR$hash.$ext"

    /** True for a gallery file unit path (`gallery/<ownerKey>.json`, not an original). */
    fun isGalleryUnit(path: String): Boolean =
        path.startsWith(GALLERY_DIR) && path.endsWith(".json") && path.count { it == '/' } == 1

    /** The owner key of a gallery file unit path. */
    fun galleryOwnerOf(path: String): Key = path.removePrefix(GALLERY_DIR).removeSuffix(".json")

    /** The `trash/` mirror path of a normal file path (§7.9). */
    fun trash(path: String) = TRASH_DIR + path
}
