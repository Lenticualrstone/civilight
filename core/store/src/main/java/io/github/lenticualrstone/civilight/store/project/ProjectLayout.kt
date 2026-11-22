package io.github.lenticualrstone.civilight.store.project

import io.github.lenticualrstone.civilight.model.app.ProjectActivityFile
import io.github.lenticualrstone.civilight.model.app.ProjectViewFile
import io.github.lenticualrstone.civilight.model.canvas.Canvas
import io.github.lenticualrstone.civilight.model.canvas.Page
import io.github.lenticualrstone.civilight.model.doc.FileUnits
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.ProjectFileSet
import io.github.lenticualrstone.civilight.model.file.AssetsIndexFile
import io.github.lenticualrstone.civilight.model.file.CastFile
import io.github.lenticualrstone.civilight.model.file.ChangesFile
import io.github.lenticualrstone.civilight.model.file.ChecksFile
import io.github.lenticualrstone.civilight.model.file.DeletionsFile
import io.github.lenticualrstone.civilight.model.file.EpisodeFile
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.file.StoryIndexFile
import io.github.lenticualrstone.civilight.model.file.TombstonesFile
import io.github.lenticualrstone.civilight.model.file.TrashIndexFile
import io.github.lenticualrstone.civilight.model.file.WorldFile
import io.github.lenticualrstone.civilight.model.gallery.GalleryFile
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.store.io.AtomicFiles
import io.github.lenticualrstone.civilight.store.io.StoreDirectory
import io.github.lenticualrstone.civilight.store.version.StoreVersionFile
import kotlinx.serialization.KSerializer

/**
 * The files of one project directory (§8.2, §9): the §7.2 file units split by liveness
 * ([ProjectFileSet.split]: live rows in the normal files, non-live rows in the `trash/` mirror),
 * plus the app-only `_app/` files. Unknown files of an imported package sit at their own paths.
 */
object ProjectLayout {
    const val APP_DIR = "_app/"
    const val STORE_META = "_app/store.json"
    const val DAMAGED_DIR = "_app/damaged/"

    /** A file unit: the value it holds (compared to decide whether to write) and its encoder. */
    internal class UnitFile(val value: Any, private val encoder: () -> ByteArray) {
        fun encode(): ByteArray = encoder()
    }

    /** Every file unit of [doc] by path, in write order. Encoding happens only on demand. */
    internal fun units(doc: ProjectDoc, storeVersion: Int): LinkedHashMap<String, UnitFile> {
        val set = ProjectFileSet.split(doc)
        val out = LinkedHashMap<String, UnitFile>()
        addUnits(out, "", set.live)
        val trash = set.trash
        if (trash != null) {
            out[PackagePaths.TRASH_INDEX] = unit(trash.index, TrashIndexFile.serializer())
            addUnits(out, PackagePaths.TRASH_DIR, trash.units)
        } else {
            out[PackagePaths.TRASH_INDEX] = unit(doc.trash, TrashIndexFile.serializer())
        }
        out[PackagePaths.APP_ACTIVITY] = unit(doc.app.activity, ProjectActivityFile.serializer())
        out[PackagePaths.APP_VIEW] = unit(doc.app.view, ProjectViewFile.serializer())
        out[STORE_META] = unit(StoreVersionFile(storeVersion), StoreVersionFile.serializer())
        return out
    }

    /** Mirrors `FileUnits.encode(prefix)` of the model: which units exist at which path. */
    private fun addUnits(out: MutableMap<String, UnitFile>, prefix: String, u: FileUnits) {
        val mirror = prefix.isNotEmpty()
        if (!mirror || u.project.fieldDefs.isNotEmpty()) out[prefix + PackagePaths.PROJECT] = unit(u.project, Project.serializer())
        if (!mirror || u.storyIndex.units.isNotEmpty()) out[prefix + PackagePaths.STORY_INDEX] = unit(u.storyIndex, StoryIndexFile.serializer())
        for ((key, file) in u.episodes) out[prefix + PackagePaths.episode(key)] = unit(file, EpisodeFile.serializer())
        if (!mirror || u.cast != CastFile()) out[prefix + PackagePaths.CAST] = unit(u.cast, CastFile.serializer())
        if (!mirror || u.world != WorldFile()) out[prefix + PackagePaths.WORLD] = unit(u.world, WorldFile.serializer())
        if (!mirror || u.changes != ChangesFile()) out[prefix + PackagePaths.CHANGES] = unit(u.changes, ChangesFile.serializer())
        for ((key, map) in u.maps) out[prefix + PackagePaths.map(key)] = unit(map, WorldMap.serializer())
        for ((key, canvas) in u.canvases) out[prefix + PackagePaths.canvas(key)] = unit(canvas, Canvas.serializer())
        for ((key, page) in u.pages) out[prefix + PackagePaths.page(key)] = unit(page, Page.serializer())
        if (!mirror || u.assets != AssetsIndexFile()) out[prefix + PackagePaths.ASSETS_INDEX] = unit(u.assets, AssetsIndexFile.serializer())
        // One unit per gallery owner (55 §3.3, §12.5): a caption keystroke rewrites only that owner's file.
        for ((key, file) in u.gallery) out[prefix + PackagePaths.gallery(key)] = unit(file, GalleryFile.serializer())
        if (!mirror) {
            out[PackagePaths.CHECKS] = unit(u.checks, ChecksFile.serializer())
            out[PackagePaths.TOMBSTONES] = unit(u.tombstones, TombstonesFile.serializer())
            out[PackagePaths.DELETIONS] = unit(u.deletions, DeletionsFile.serializer())
        }
    }

    private fun <T : Any> unit(value: T, serializer: KSerializer<T>) =
        UnitFile(value) { CivilightJson.encode(serializer, value).toByteArray(Charsets.UTF_8) }

    /**
     * True for a path the §7 layout reads (normal files and the `trash/` mirror). A file at such a
     * path that cannot be read is damaged, never unknown.
     */
    fun isLayoutPath(path: String): Boolean {
        if (path == PackagePaths.TRASH_INDEX) return true
        val rel = path.removePrefix(PackagePaths.TRASH_DIR)
        if (rel in FIXED) return true
        if (!rel.endsWith(".json")) return false
        return (rel.startsWith("story/") && rel.count { it == '/' } == 1) ||
            rel.startsWith("maps/") || rel.startsWith("canvas/") || rel.startsWith("pages/") || PackagePaths.isGalleryUnit(rel)
    }

    /** True when an unknown file may be kept at [path] (not a layout path, not store-internal). */
    fun isFreeForUnknown(path: String): Boolean {
        if (path.isEmpty() || path.startsWith("/") || path.startsWith(APP_DIR)) return false
        if (path == StoreDirectory.JOURNAL || path.endsWith(AtomicFiles.TEMP_SUFFIX)) return false
        if (path.split('/').any { it.isEmpty() || it == "." || it == ".." }) return false
        return !isLayoutPath(path)
    }

    /**
     * The value a decoded file at [path] holds in [set] (as read from disk), or null when the path
     * does not map back to one value (then the file is rewritten or removed on the next save).
     */
    internal fun decodedValue(path: String, set: ProjectFileSet): Any? {
        if (path == PackagePaths.TRASH_INDEX) return set.trash?.index
        val inTrash = path.startsWith(PackagePaths.TRASH_DIR)
        val u = if (inTrash) set.trash?.units ?: return null else set.live
        val rel = path.removePrefix(PackagePaths.TRASH_DIR.takeIf { inTrash } ?: "")
        fun fileKey(dir: String) = rel.removePrefix(dir).removeSuffix(".json")
        return when {
            rel == PackagePaths.PROJECT -> u.project
            rel == PackagePaths.STORY_INDEX -> u.storyIndex
            rel == PackagePaths.CAST -> u.cast
            rel == PackagePaths.WORLD -> u.world
            rel == PackagePaths.CHANGES -> u.changes
            rel == PackagePaths.ASSETS_INDEX -> u.assets
            rel == PackagePaths.CHECKS -> if (inTrash) null else u.checks
            rel == PackagePaths.TOMBSTONES -> if (inTrash) null else u.tombstones
            rel == PackagePaths.DELETIONS -> if (inTrash) null else u.deletions
            rel.startsWith("story/") -> u.episodes[fileKey("story/")]
            rel.startsWith("maps/") -> u.maps[fileKey("maps/")]
            rel.startsWith("canvas/") -> u.canvases[fileKey("canvas/")]
            rel.startsWith("pages/") -> u.pages[fileKey("pages/")]
            PackagePaths.isGalleryUnit(rel) -> u.gallery[PackagePaths.galleryOwnerOf(rel)]
            else -> null
        }
    }

    private val FIXED = setOf(
        PackagePaths.PROJECT, PackagePaths.STORY_INDEX, PackagePaths.CAST, PackagePaths.WORLD, PackagePaths.CHANGES,
        PackagePaths.ASSETS_INDEX, PackagePaths.CHECKS, PackagePaths.TOMBSTONES, PackagePaths.DELETIONS,
    )
}
