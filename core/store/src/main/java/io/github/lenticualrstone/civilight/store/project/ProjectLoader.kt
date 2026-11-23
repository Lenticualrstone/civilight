package io.github.lenticualrstone.civilight.store.project

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.LensOnOpen
import io.github.lenticualrstone.civilight.model.app.ProjectActivityFile
import io.github.lenticualrstone.civilight.model.app.ProjectAppState
import io.github.lenticualrstone.civilight.model.app.ProjectViewFile
import io.github.lenticualrstone.civilight.model.app.UnknownFile
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.ProjectFileSet
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.game.GameToolsUpgrade
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.store.AppClock
import io.github.lenticualrstone.civilight.store.ProjectLoadException
import io.github.lenticualrstone.civilight.store.io.AtomicFiles
import io.github.lenticualrstone.civilight.store.io.StoreDirectory
import io.github.lenticualrstone.civilight.store.version.StoreUpgradeChain
import io.github.lenticualrstone.civilight.store.version.StoreVersionFile
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64

/** What happened while a project was read. */
data class LoadReport(
    /** Files that could not be read (path → reason). Their bytes were moved to [damagedDir], never overwritten. */
    val damaged: Map<String, String> = emptyMap(),
    /** Where damaged files were set aside (relative to the project directory), or null. */
    val damagedDir: String? = null,
    /** The store version the files were upgraded from, or null when they were current. */
    val upgradedFrom: Int? = null,
    /** True when an interrupted save was completed from its journal. */
    val recoveredTransaction: Boolean = false,
    /**
     * (0.2.0, G-3) The `게임 제작 도구` value decided by this load for a project that had none (a
     * 0.1.0 project), or null when the stored value was already there.
     */
    val gameToolsDecided: Boolean? = null,
)

/** What is on disk right after a load or a save: path → value (or [Stale]) and the unknown files. */
internal data class WrittenState(
    val units: Map<String, Any> = emptyMap(),
    val unknown: Map<String, UnknownFile> = emptyMap(),
)

/** A file on disk whose content does not map back to one value of the document: always rewritten or removed. */
internal object Stale

internal class LoadedProject(val doc: ProjectDoc, val written: WrittenState, val report: LoadReport)

/**
 * Reads one project directory (§8.2) into a [ProjectDoc].
 *
 * Order: finish or drop an interrupted save; refuse a newer store version; upgrade older files on
 * their raw JSON; then decode. A file at a layout path that cannot be read is damaged: it is moved
 * to `_app/damaged/<time>/` (when [mutate]) and reported, so a later save never replaces it with an
 * empty default. A missing or unreadable `project.json` or `story/index.json` fails the load
 * ([ProjectLoadException]). Any other file becomes an [UnknownFile] and is written back unchanged.
 */
internal object ProjectLoader {
    private val APP_FILES = setOf(PackagePaths.APP_ACTIVITY, PackagePaths.APP_VIEW)

    fun load(
        key: Key,
        dir: StoreDirectory,
        chain: StoreUpgradeChain,
        clock: AppClock,
        mutate: Boolean,
    ): LoadedProject {
        if (!dir.root.isDirectory) throw ProjectLoadException(key, "directory missing")
        val recovered = dir.recover()

        val version = dir.readText(ProjectLayout.STORE_META)
            ?.let { runCatching { CivilightJson.decode(StoreVersionFile.serializer(), it).storeVersion }.getOrNull() }
            ?: 1
        val upgrade = chain.needsUpgrade(version)

        val damaged = LinkedHashMap<String, String>()
        var json = LinkedHashMap<String, JsonElement>()
        val raw = LinkedHashMap<String, ByteArray>()
        for (path in dir.listFiles(skipPrefixes = setOf(ProjectLayout.APP_DIR))) {
            val bytes = dir.read(path) ?: continue
            val element = if (path.endsWith(".json")) parse(bytes) else null
            when {
                element != null -> {
                    json[path] = element
                    raw[path] = bytes
                }
                ProjectLayout.isLayoutPath(path) -> damaged[path] = "not JSON"
                else -> raw[path] = bytes
            }
        }
        val appJson = LinkedHashMap<String, JsonElement>()
        for (path in APP_FILES) {
            val bytes = dir.read(path) ?: continue
            val element = parse(bytes)
            if (element != null) appJson[path] = element else damaged[path] = "not JSON"
        }

        if (upgrade) {
            val before = json + appJson
            val after = chain.upgrade(version, before)
            if (mutate) {
                val writes = LinkedHashMap<String, ByteArray>()
                for ((path, element) in after) {
                    if (before[path] != element) writes[path] = CivilightJson.json.encodeToString(JsonElement.serializer(), element).toByteArray()
                }
                writes[ProjectLayout.STORE_META] = CivilightJson.encode(StoreVersionFile.serializer(), StoreVersionFile(chain.current)).toByteArray()
                dir.commit(writes, before.keys.filter { it !in after })
            }
            json = LinkedHashMap(after.filterKeys { it !in APP_FILES })
            appJson.clear()
            appJson.putAll(after.filterKeys { it in APP_FILES })
        }

        val decoded = try {
            ProjectFileSet.decode(json)
        } catch (e: IllegalArgumentException) {
            throw ProjectLoadException(key, e.message ?: "unreadable", e)
        }
        damaged.putAll(decoded.failedPaths)
        val set = decoded.files
        if (set.live.project.key != key) throw ProjectLoadException(key, "project.json holds key ${set.live.project.key}")

        val unknownPaths = decoded.unknownPaths.toSet() + raw.keys.filter { it !in json }
        val unknown = LinkedHashMap<String, UnknownFile>()
        for (path in unknownPaths) {
            val element = json[path]
            val bytes = if (upgrade && element != null) {
                CivilightJson.json.encodeToString(JsonElement.serializer(), element).toByteArray()
            } else {
                raw[path] ?: continue
            }
            unknown[path] = unknownFile(path, bytes)
        }

        val activity = decodeApp(PackagePaths.APP_ACTIVITY, appJson, ProjectActivityFile.serializer(), damaged)
        val view = decodeApp(PackagePaths.APP_VIEW, appJson, ProjectViewFile.serializer(), damaged)
        // (0.2.0, D9) The project opens at 최신; the stored lens is kept and written back unchanged.
        val loadedDoc = set.toDoc(LensOnOpen.open(ProjectAppState(activity ?: ProjectActivityFile(), view ?: ProjectViewFile())), unknown)
        // (0.2.0, G-3) A project without a stored `gameTools` (written by 0.1.0) gets it decided once
        // from its game data (D6). With [mutate] the new project.json is written at once (no undo step).
        val doc = GameToolsUpgrade.resolve(loadedDoc)
        val decided = if (doc !== loadedDoc) doc.project.gameTools else null

        val written = LinkedHashMap<String, Any>()
        for (path in json.keys) {
            if (path in damaged || path in unknownPaths) continue
            written[path] = ProjectLayout.decodedValue(path, set) ?: Stale
        }
        activity?.let { written[PackagePaths.APP_ACTIVITY] = it }
        view?.let { written[PackagePaths.APP_VIEW] = it }
        if (upgrade || dir.exists(ProjectLayout.STORE_META)) written[ProjectLayout.STORE_META] = StoreVersionFile(chain.current)
        if (decided != null && mutate) {
            val unit = ProjectLayout.units(doc, chain.current).getValue(PackagePaths.PROJECT)
            dir.commit(mapOf(PackagePaths.PROJECT to unit.encode()), emptyList())
            written[PackagePaths.PROJECT] = unit.value
        }

        val damagedDir = if (mutate && damaged.isNotEmpty()) quarantine(dir, damaged.keys, clock) else null
        return LoadedProject(
            doc = doc,
            written = WrittenState(written, unknown),
            report = LoadReport(damaged, damagedDir, version.takeIf { upgrade }, recovered, decided),
        )
    }

    private fun parse(bytes: ByteArray): JsonElement? =
        runCatching { CivilightJson.json.parseToJsonElement(bytes.toString(Charsets.UTF_8)) }.getOrNull()

    private fun <T> decodeApp(path: String, json: Map<String, JsonElement>, serializer: KSerializer<T>, damaged: MutableMap<String, String>): T? {
        val element = json[path] ?: return null
        return try {
            CivilightJson.decodeElement(serializer, element)
        } catch (e: IllegalArgumentException) {
            damaged[path] = e.message ?: "unreadable"
            null
        }
    }

    private fun quarantine(dir: StoreDirectory, paths: Collection<String>, clock: AppClock): String {
        val stamp = STAMP.format(Instant.ofEpochMilli(clock.now()))
        var base = "${ProjectLayout.DAMAGED_DIR}$stamp"
        var n = 1
        while (dir.file(base).exists()) base = "${ProjectLayout.DAMAGED_DIR}$stamp-${n++}"
        for (path in paths) {
            val from = dir.file(path)
            if (from.isFile) AtomicFiles.moveAtomically(from, dir.file("$base/$path"))
        }
        return "$base/"
    }

    fun unknownFile(path: String, bytes: ByteArray): UnknownFile =
        UnknownFile(path = path, bytesBase64 = Base64.getEncoder().encodeToString(bytes), sha256 = sha256Hex(bytes))

    fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private val STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC)
}
