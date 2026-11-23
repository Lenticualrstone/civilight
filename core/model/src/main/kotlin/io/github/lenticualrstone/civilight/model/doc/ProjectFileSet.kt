package io.github.lenticualrstone.civilight.model.doc

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.ProjectAppState
import io.github.lenticualrstone.civilight.model.app.UnknownFile
import io.github.lenticualrstone.civilight.model.canvas.Canvas
import io.github.lenticualrstone.civilight.model.canvas.Page
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
import io.github.lenticualrstone.civilight.model.version.EpisodeVersionsFile
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement

/**
 * The project file units at their paths (§7.2, §8.2). [live] holds the normal files (live rows only,
 * C-14); [trash] the `trash/` mirror (non-live rows, §7.9) or null when not written.
 *
 * This is the structural split only. Export rules (name tags for trashed references, blob
 * selection, hidden content, encryption, manifest) belong to the exporter.
 */
data class ProjectFileSet(
    val live: FileUnits,
    val trash: TrashMirror?,
) {
    /** Every file as JSON, by path (live files, then the trash mirror). */
    fun encode(): Map<String, JsonElement> = live.encode("") + (trash?.encode().orEmpty())

    /** Reassembles the in-memory project (trashed rows rejoin their live files). */
    fun toDoc(
        app: ProjectAppState = ProjectAppState(),
        unknownFiles: Map<String, UnknownFile> = emptyMap(),
    ): ProjectDoc {
        val t = trash?.units
        return ProjectDoc(
            project = live.project.copy(fieldDefs = union(live.project.fieldDefs, t?.project?.fieldDefs) { it.key }),
            story = live.storyIndex.copy(
                units = union(live.storyIndex.units, t?.storyIndex?.units) { it.key },
                discardedVoiceIds = live.storyIndex.discardedVoiceIds + t?.storyIndex?.discardedVoiceIds.orEmpty(),
                foreshadows = union(live.storyIndex.foreshadows, t?.storyIndex?.foreshadows) { it.key },
            ),
            episodes = mergeEpisodes(live.episodes, t?.episodes.orEmpty()),
            cast = CastFile(
                characters = union(live.cast.characters, t?.cast?.characters) { it.key },
                relations = union(live.cast.relations, t?.cast?.relations) { it.key },
                voiceLines = union(live.cast.voiceLines, t?.cast?.voiceLines) { it.key },
                archiveDocs = union(live.cast.archiveDocs, t?.cast?.archiveDocs) { it.key },
                ext = live.cast.ext,
            ),
            world = WorldFile(
                places = union(live.world.places, t?.world?.places) { it.key },
                terms = union(live.world.terms, t?.world?.terms) { it.key },
                factions = union(live.world.factions, t?.world?.factions) { it.key },
                events = union(live.world.events, t?.world?.events) { it.key },
                ext = live.world.ext,
            ),
            maps = mergeMaps(live.maps, t?.maps.orEmpty()),
            changes = live.changes.copy(records = union(live.changes.records, t?.changes?.records) { it.key }),
            checks = live.checks,
            tombstones = live.tombstones,
            deletions = live.deletions,
            canvases = t?.canvases.orEmpty() + live.canvases,
            pages = t?.pages.orEmpty() + live.pages,
            assets = live.assets.copy(
                blobs = union(live.assets.blobs, t?.assets?.blobs) { it.hash },
                imageUses = union(live.assets.imageUses, t?.assets?.imageUses) { it.key },
                scriptAssets = union(live.assets.scriptAssets, t?.assets?.scriptAssets) { it.key },
            ),
            trash = trash?.index ?: TrashIndexFile(),
            app = app,
            unknownFiles = unknownFiles,
            gallery = mergeGallery(live.gallery, t?.gallery.orEmpty()),
            versions = live.versions,
        )
    }

    companion object {
        /**
         * Splits [doc] into live files and the trash mirror by derived liveness (§5.1). Episode
         * files are written for every numbering unit with a live scene (possibly empty) and for any
         * group that still holds discarded voice IDs.
         */
        fun split(doc: ProjectDoc): ProjectFileSet {
            val l = doc.liveness
            val ix = doc.index

            val (liveUnits, deadUnits) = doc.story.units.partition { l.isLive(it) }
            val (liveMarks, deadMarks) = doc.story.foreshadows.partition { it.deletedAt == null }

            val liveNumberingUnits = liveUnits.filter { ix.isScene(it) }.mapNotNull { ix.numberingUnitKeyOfScene(it.key) }.toSet()
            val liveEpisodes = LinkedHashMap<Key, EpisodeFile>()
            val deadEpisodes = LinkedHashMap<Key, EpisodeFile>()
            for ((groupKey, file) in doc.episodes) {
                val (liveBlocks, deadBlocks) = file.blocks.partition { l.isLive(it) }
                if (groupKey in liveNumberingUnits || liveBlocks.isNotEmpty() || file.discardedVoiceIds.isNotEmpty() || file.ext != null) {
                    liveEpisodes[groupKey] = file.copy(blocks = liveBlocks)
                }
                if (deadBlocks.isNotEmpty()) deadEpisodes[groupKey] = EpisodeFile(unitKey = file.unitKey, blocks = deadBlocks)
            }
            for (unitKey in liveNumberingUnits) liveEpisodes.getOrPut(unitKey) { EpisodeFile(unitKey = unitKey) }

            val (liveChars, deadChars) = doc.cast.characters.partition { it.deletedAt == null }
            val (liveRels, deadRels) = doc.cast.relations.partition { l.isLive(it) }
            val (liveLines, deadLines) = doc.cast.voiceLines.partition { l.isCharacterLive(it.characterKey) }
            val (liveDocs, deadDocs) = doc.cast.archiveDocs.partition { l.isCharacterLive(it.characterKey) }
            val (livePlaces, deadPlaces) = doc.world.places.partition { it.deletedAt == null }
            val (liveTerms, deadTerms) = doc.world.terms.partition { it.deletedAt == null }
            val (liveFactions, deadFactions) = doc.world.factions.partition { it.deletedAt == null }
            val (liveEvents, deadEvents) = doc.world.events.partition { it.deletedAt == null }
            val (liveRecords, deadRecords) = doc.changes.records.partition { l.isLive(it) }
            val (liveUses, deadUses) = doc.assets.imageUses.partition { l.isLive(it) }

            val liveMaps = LinkedHashMap<Key, WorldMap>()
            val deadMaps = LinkedHashMap<Key, WorldMap>()
            for ((key, map) in doc.maps) {
                if (map.deletedAt != null) {
                    deadMaps[key] = map
                    continue
                }
                val (livePins, deadPins) = map.pins.partition { l.isPlaceLive(it.placeKey) }
                liveMaps[key] = map.copy(pins = livePins)
                if (deadPins.isNotEmpty()) deadMaps[key] = map.shell().copy(pins = deadPins)
            }

            val liveGallery = LinkedHashMap<Key, GalleryFile>()
            val deadGallery = LinkedHashMap<Key, GalleryFile>()
            for ((owner, file) in doc.gallery) {
                val (liveItems, deadItems) = file.items.partition { l.isLive(it) }
                if (liveItems.isNotEmpty() || file.ext != null) liveGallery[owner] = file.copy(items = liveItems)
                if (deadItems.isNotEmpty()) deadGallery[owner] = file.copy(items = deadItems, ext = null)
            }

            val (liveDefs, deadDefs) = doc.project.fieldDefs.partition { it.deletedAt == null }
            val live = FileUnits(
                project = doc.project.copy(fieldDefs = liveDefs),
                storyIndex = doc.story.copy(units = liveUnits, foreshadows = liveMarks),
                episodes = liveEpisodes,
                cast = doc.cast.copy(characters = liveChars, relations = liveRels, voiceLines = liveLines, archiveDocs = liveDocs),
                world = doc.world.copy(places = livePlaces, terms = liveTerms, factions = liveFactions, events = liveEvents),
                changes = doc.changes.copy(records = liveRecords),
                maps = liveMaps,
                canvases = doc.canvases.filterValues { it.deletedAt == null },
                pages = doc.pages.filterValues { it.deletedAt == null },
                assets = doc.assets.copy(imageUses = liveUses),
                checks = doc.checks,
                tombstones = doc.tombstones,
                deletions = doc.deletions,
                gallery = liveGallery,
                versions = doc.versions,
            )
            val dead = FileUnits(
                project = doc.project.copy(fieldDefs = deadDefs, tags = emptyList(), relationKinds = emptyList(), travelModes = emptyList()),
                storyIndex = StoryIndexFile(units = deadUnits, foreshadows = deadMarks),
                episodes = deadEpisodes,
                cast = CastFile(characters = deadChars, relations = deadRels, voiceLines = deadLines, archiveDocs = deadDocs),
                world = WorldFile(places = deadPlaces, terms = deadTerms, factions = deadFactions, events = deadEvents),
                changes = ChangesFile(records = deadRecords),
                maps = deadMaps,
                canvases = doc.canvases.filterValues { it.deletedAt != null },
                pages = doc.pages.filterValues { it.deletedAt != null },
                assets = AssetsIndexFile(imageUses = deadUses),
                checks = ChecksFile(),
                tombstones = TombstonesFile(),
                deletions = DeletionsFile(),
                gallery = deadGallery,
            )
            return ProjectFileSet(live, TrashMirror(doc.trash, dead))
        }

        /**
         * Reads file units from JSON by path (as produced by [encode]). `project.json` and
         * `story/index.json` are required and throw when missing or unreadable. Any other file
         * that cannot be read is skipped and reported in [DecodeResult.failedPaths], so one damaged
         * file loses only its own content (§7.1, LIB-55). Paths outside the known layout are
         * returned in [DecodeResult.unknownPaths]. In the trash mirror, `trash/project.json` holds
         * only trashed field definitions.
         */
        fun decode(files: Map<String, JsonElement>): DecodeResult {
            val known = mutableSetOf<String>()
            val failed = LinkedHashMap<String, String>()

            fun <T> read(path: String, serializer: KSerializer<T>, required: Boolean = false): T? {
                val element = files[path] ?: return null
                known += path
                return try {
                    CivilightJson.decodeElement(serializer, element)
                } catch (e: IllegalArgumentException) {
                    // SerializationException and invalid values (e.g. a clock out of range).
                    if (required) throw e
                    failed[path] = e.message ?: e.toString()
                    null
                }
            }

            fun units(prefix: String, required: Boolean, fallbackProject: Project?): FileUnits? {
                val anything = files.keys.any { it.startsWith(prefix) && (prefix.isNotEmpty() || !it.startsWith(PackagePaths.TRASH_DIR)) }
                if (!required && !anything) return null
                val project = read(prefix + PackagePaths.PROJECT, Project.serializer(), required)
                val storyIndex = read(prefix + PackagePaths.STORY_INDEX, StoryIndexFile.serializer(), required)
                if (required) {
                    requireNotNull(project) { "project.json missing" }
                    requireNotNull(storyIndex) { "story/index.json missing" }
                }
                val episodes = LinkedHashMap<Key, EpisodeFile>()
                val maps = LinkedHashMap<Key, WorldMap>()
                val canvases = LinkedHashMap<Key, Canvas>()
                val pages = LinkedHashMap<Key, Page>()
                val gallery = LinkedHashMap<Key, GalleryFile>()
                val versions = LinkedHashMap<Key, EpisodeVersionsFile>()
                for (path in files.keys) {
                    if (!path.startsWith(prefix)) continue
                    val rel = path.removePrefix(prefix)
                    if (prefix.isEmpty() && rel.startsWith(PackagePaths.TRASH_DIR)) continue
                    when {
                        rel == PackagePaths.STORY_INDEX -> Unit
                        rel.startsWith("story/") && rel.endsWith(".json") && rel.count { it == '/' } == 1 ->
                            read(path, EpisodeFile.serializer())?.let { episodes[rel.removePrefix("story/").removeSuffix(".json")] = it }
                        rel.startsWith("maps/") && rel.endsWith(".json") ->
                            read(path, WorldMap.serializer())?.let { maps[it.key] = it }
                        rel.startsWith("canvas/") && rel.endsWith(".json") ->
                            read(path, Canvas.serializer())?.let { canvases[it.key] = it }
                        rel.startsWith("pages/") && rel.endsWith(".json") ->
                            read(path, Page.serializer())?.let { pages[it.key] = it }
                        PackagePaths.isGalleryUnit(rel) ->
                            read(path, GalleryFile.serializer())?.let { file ->
                                // Items of another owner than the file's own are dropped (a damaged or edited file).
                                gallery[file.ownerKey] = file.copy(items = file.items.filter { it.ownerKey == file.ownerKey })
                            }
                        prefix.isEmpty() && PackagePaths.isVersionsUnit(rel) ->
                            read(path, EpisodeVersionsFile.serializer())?.let { versions[PackagePaths.versionsUnitOf(rel)] = it }
                    }
                }
                val base = fallbackProject ?: project!!
                return FileUnits(
                    project = if (required) project!! else base.copy(fieldDefs = project?.fieldDefs.orEmpty()),
                    storyIndex = storyIndex ?: StoryIndexFile(),
                    episodes = episodes,
                    cast = read(prefix + PackagePaths.CAST, CastFile.serializer()) ?: CastFile(),
                    world = read(prefix + PackagePaths.WORLD, WorldFile.serializer()) ?: WorldFile(),
                    changes = read(prefix + PackagePaths.CHANGES, ChangesFile.serializer()) ?: ChangesFile(),
                    maps = maps,
                    canvases = canvases,
                    pages = pages,
                    assets = read(prefix + PackagePaths.ASSETS_INDEX, AssetsIndexFile.serializer()) ?: AssetsIndexFile(),
                    checks = read(prefix + PackagePaths.CHECKS, ChecksFile.serializer()) ?: ChecksFile(),
                    tombstones = read(prefix + PackagePaths.TOMBSTONES, TombstonesFile.serializer()) ?: TombstonesFile(),
                    deletions = read(prefix + PackagePaths.DELETIONS, DeletionsFile.serializer()) ?: DeletionsFile(),
                    gallery = gallery,
                    versions = versions,
                )
            }

            val live = units("", required = true, fallbackProject = null)!!
            val trashIndex = read(PackagePaths.TRASH_INDEX, TrashIndexFile.serializer())
            val trashUnits = units(PackagePaths.TRASH_DIR, required = false, fallbackProject = live.project.copy(
                fieldDefs = emptyList(), tags = emptyList(), relationKinds = emptyList(), travelModes = emptyList(),
            ))
            val trash = if (trashIndex == null && trashUnits == null) null
            else TrashMirror(trashIndex ?: TrashIndexFile(), trashUnits ?: FileUnits.empty(live.project.copy(fieldDefs = emptyList())))
            return DecodeResult(ProjectFileSet(live, trash), files.keys.filter { it !in known }, failed)
        }

        private fun <T> union(primary: List<T>, secondary: List<T>?, key: (T) -> Any): List<T> {
            if (secondary.isNullOrEmpty()) return primary
            val seen = primary.mapTo(HashSet(), key)
            return primary + secondary.filter { key(it) !in seen }
        }

        private fun mergeEpisodes(live: Map<Key, EpisodeFile>, trash: Map<Key, EpisodeFile>): Map<Key, EpisodeFile> {
            val out = LinkedHashMap(live)
            for ((key, dead) in trash) {
                val current = out[key]
                out[key] = current?.copy(blocks = union(current.blocks, dead.blocks) { it.key }) ?: dead
            }
            return out
        }

        private fun mergeMaps(live: Map<Key, WorldMap>, trash: Map<Key, WorldMap>): Map<Key, WorldMap> {
            val out = LinkedHashMap(live)
            for ((key, dead) in trash) {
                val current = out[key]
                out[key] = current?.copy(pins = union(current.pins, dead.pins) { it.key }) ?: dead
            }
            return out
        }

        private fun mergeGallery(live: Map<Key, GalleryFile>, trash: Map<Key, GalleryFile>): Map<Key, GalleryFile> {
            val out = LinkedHashMap(live)
            for ((key, dead) in trash) {
                val current = out[key]
                out[key] = current?.copy(items = union(current.items, dead.items) { it.key }) ?: dead
            }
            return out
        }

        private fun WorldMap.shell(): WorldMap = copy(
            layers = emptyList(), pins = emptyList(), regions = emptyList(), routes = emptyList(), labels = emptyList(),
            stamps = emptyList(), drawnLines = emptyList(), nodes = emptyList(), nodeLinks = emptyList(),
            hex = null, poly = null, generation = null,
        )
    }

    /**
     * [unknownPaths]: files outside the known layout (keep them as `UnknownFile`s, §8.3).
     * [failedPaths]: known files that could not be read, with the reason.
     */
    data class DecodeResult(
        val files: ProjectFileSet,
        val unknownPaths: List<String>,
        val failedPaths: Map<String, String> = emptyMap(),
    )
}

/** One set of the project's file units (the normal files, or the `trash/` mirror's content). */
data class FileUnits(
    val project: Project,
    val storyIndex: StoryIndexFile,
    val episodes: Map<Key, EpisodeFile>,
    val cast: CastFile,
    val world: WorldFile,
    val changes: ChangesFile,
    val maps: Map<Key, WorldMap>,
    val canvases: Map<Key, Canvas>,
    val pages: Map<Key, Page>,
    val assets: AssetsIndexFile,
    val checks: ChecksFile,
    val tombstones: TombstonesFile,
    val deletions: DeletionsFile,
    /** Gallery files by owner key (`gallery/<ownerKey>.json`, 55 §3.3); only owners with items. */
    val gallery: Map<Key, GalleryFile> = emptyMap(),
    /** (0.2.0) `versions/<unitKey>.json` by unit key; only in the normal files, never the trash mirror. */
    val versions: Map<Key, EpisodeVersionsFile> = emptyMap(),
) {
    /**
     * JSON of each unit by path under [prefix]. With a non-empty prefix (the trash mirror) the
     * checks, tombstones and deletions files are not written, empty units are skipped, and
     * `project.json` is written only to hold trashed field definitions.
     */
    fun encode(prefix: String): Map<String, JsonElement> {
        val mirror = prefix.isNotEmpty()
        val out = LinkedHashMap<String, JsonElement>()
        if (!mirror || project.fieldDefs.isNotEmpty()) out[prefix + PackagePaths.PROJECT] = CivilightJson.encodeElement(Project.serializer(), project)
        if (!mirror || storyIndex.units.isNotEmpty() || storyIndex.foreshadows.isNotEmpty()) out[prefix + PackagePaths.STORY_INDEX] = CivilightJson.encodeElement(StoryIndexFile.serializer(), storyIndex)
        for ((key, file) in episodes) out[prefix + PackagePaths.episode(key)] = CivilightJson.encodeElement(EpisodeFile.serializer(), file)
        if (!mirror || cast != CastFile()) out[prefix + PackagePaths.CAST] = CivilightJson.encodeElement(CastFile.serializer(), cast)
        if (!mirror || world != WorldFile()) out[prefix + PackagePaths.WORLD] = CivilightJson.encodeElement(WorldFile.serializer(), world)
        if (!mirror || changes != ChangesFile()) out[prefix + PackagePaths.CHANGES] = CivilightJson.encodeElement(ChangesFile.serializer(), changes)
        for ((key, map) in maps) out[prefix + PackagePaths.map(key)] = CivilightJson.encodeElement(WorldMap.serializer(), map)
        for ((key, canvas) in canvases) out[prefix + PackagePaths.canvas(key)] = CivilightJson.encodeElement(Canvas.serializer(), canvas)
        for ((key, page) in pages) out[prefix + PackagePaths.page(key)] = CivilightJson.encodeElement(Page.serializer(), page)
        if (!mirror || assets != AssetsIndexFile()) out[prefix + PackagePaths.ASSETS_INDEX] = CivilightJson.encodeElement(AssetsIndexFile.serializer(), assets)
        for ((key, file) in gallery) out[prefix + PackagePaths.gallery(key)] = CivilightJson.encodeElement(GalleryFile.serializer(), file)
        if (!mirror) for ((key, file) in versions) out[PackagePaths.versions(key)] = CivilightJson.encodeElement(EpisodeVersionsFile.serializer(), file)
        if (!mirror) {
            out[PackagePaths.CHECKS] = CivilightJson.encodeElement(ChecksFile.serializer(), checks)
            out[PackagePaths.TOMBSTONES] = CivilightJson.encodeElement(TombstonesFile.serializer(), tombstones)
            out[PackagePaths.DELETIONS] = CivilightJson.encodeElement(DeletionsFile.serializer(), deletions)
        }
        return out
    }

    companion object {
        fun empty(project: Project) = FileUnits(
            project, StoryIndexFile(), emptyMap(), CastFile(), WorldFile(), ChangesFile(), emptyMap(), emptyMap(),
            emptyMap(), AssetsIndexFile(), ChecksFile(), TombstonesFile(), DeletionsFile(),
        )
    }
}

/** The `trash/` mirror (§7.9): `trash/index.json` plus non-live rows in the normal shapes. */
data class TrashMirror(val index: TrashIndexFile, val units: FileUnits) {
    fun encode(): Map<String, JsonElement> =
        mapOf(PackagePaths.TRASH_INDEX to CivilightJson.encodeElement(TrashIndexFile.serializer(), index)) +
            units.encode(PackagePaths.TRASH_DIR)
}

/** Shortcut for [ProjectFileSet.split]. */
fun ProjectDoc.toFileSet(): ProjectFileSet = ProjectFileSet.split(this)
