package io.github.lenticualrstone.civilight.domain.library.export

import io.github.lenticualrstone.civilight.domain.library.common.UnitCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.app.ExportOptions
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.DiscardedVoiceId
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/** Range and hidden scenes of an export (LIB-41 `범위`, `숨긴 씬`). */
class ExportScope(private val doc: ProjectDoc, rangeKeys: List<Key>) {
    private val index = doc.index

    /** Live scenes in manuscript order under the range units (the whole manuscript when the range is empty). */
    val scenes: List<StoryUnit> = doc.manuscript.liveScenes.filter { scene ->
        rangeKeys.isEmpty() || scene.key in rangeKeys || index.ancestors(scene).any { it.key in rangeKeys }
    }

    /** Hidden scenes in range. */
    val hiddenScenes: List<StoryUnit> = scenes.filter { it.hidden }

    fun written(includeHidden: Boolean): List<StoryUnit> = if (includeHidden) scenes else scenes.filterNot { it.hidden }
}

/** Builds the [ExportPlan] of a PDF, CSV or engine JSON export. */
object ExportPlanner {
    fun plan(doc: ProjectDoc, options: ExportOptions): ExportPlan {
        require(options.format != ExportFormat.PROJECT_FILE) { "use ProjectPackageWriter for .civilight" }
        val index = doc.index
        val codes = UnitCodes(doc)
        val scope = ExportScope(doc, options.rangeKeys)
        val written = scope.written(options.includeHiddenScenes)
        val leftOut = scope.hiddenScenes.filterNot { it in written }.map { it.key }

        val scenes = written.map { scene ->
            PlannedScene(
                sceneKey = scene.key,
                label = codes.sceneLabel(scene.key).orEmpty(),
                title = scene.title,
                hidden = scene.hidden,
                blocks = doc.manuscript.liveBlocksOf(scene.key),
                place = scene.place,
                time = scene.time,
                flashback = scene.time is SceneTime.Flashback,
                transition = when (scene.transitionModeOrNone) {
                    TransitionMode.NONE -> null
                    TransitionMode.DIRECT -> PlannedTransition(TransitionMode.DIRECT, scene.transitionRuns.orEmpty())
                    TransitionMode.AUTO -> PlannedTransition(TransitionMode.AUTO, null)
                },
            )
        }

        val speakerKeys = scenes.flatMap { s -> s.blocks.flatMap { WrittenLine.of(it) } }.mapNotNull { it.speakerKey }.distinct()
        val characters = speakerKeys.mapNotNull { key ->
            val c = index.character(key)
            if (c != null) {
                PlannedCharacter(
                    key, c.name, c.displayNameBase, if (c.hidden) CharacterProfile.NAME_ONLY else CharacterProfile.FULL,
                    hidden = c.hidden, deleted = c.deletedAt != null,
                )
            } else {
                index.nameTag(key)?.let { tag ->
                    PlannedCharacter(key, tag.name, null, CharacterProfile.NAME_ONLY, hidden = tag.hidden, deleted = true)
                }
            }
        }

        val fieldDefKeys = if (options.format == ExportFormat.ENGINE_JSON) {
            doc.project.fieldDefs
                .filter { it.entityKind.known == EntityKind.CHARACTER && it.deletedAt == null && !it.definitionHidden }
                .filter { options.includeSecretFields || !it.isSecret }
                .sortedBy { it.order }
                .map { it.key }
        } else {
            emptyList()
        }

        val discarded = if (options.format == ExportFormat.CSV) discardedInRange(doc, written.map { it.key }.toSet()) else emptyList()

        val warnings = buildList {
            if (options.format == ExportFormat.ENGINE_JSON && leftOut.isNotEmpty()) {
                val choices = choicesInto(doc, written.map { it.key }.toSet(), leftOut.toSet())
                if (choices.isNotEmpty()) add(ExportWarning.ChoiceIntoLeftOutScene(choices))
            }
        }

        return ExportPlan(
            format = options.format,
            projectKey = doc.key,
            projectName = doc.project.name,
            scenes = scenes,
            leftOutScenes = leftOut,
            characters = characters,
            fieldDefKeys = fieldDefKeys,
            discardedVoiceIds = discarded,
            layers = if (options.format == ExportFormat.PDF) options.layers else emptySet(),
            fileName = options.fileName,
            warnings = warnings,
        )
    }

    /**
     * Choice blocks of written scenes whose rejoin (the next block in manuscript order, LIB-45)
     * lies in a left-out scene.
     */
    fun choicesInto(doc: ProjectDoc, writtenScenes: Set<Key>, leftOut: Set<Key>): List<Key> {
        val blocks = doc.manuscript.liveBlocks
        return blocks.withIndex()
            .filter { (i, b) -> b.kind.known == BlockKind.CHOICE && b.sceneKey in writtenScenes && blocks.getOrNull(i + 1)?.sceneKey in leftOut }
            .map { it.value.key }
    }

    /**
     * Discarded voice IDs whose block belongs to a written scene. For a purged block (no row left)
     * the numbering unit of its episode file decides (`ED`).
     */
    private fun discardedInRange(doc: ProjectDoc, writtenScenes: Set<Key>): List<DiscardedVoiceId> {
        val index = doc.index
        val writtenUnits = writtenScenes.mapNotNull { index.numberingUnitKeyOfScene(it) }.toSet()
        val withUnit = doc.episodes.flatMap { (unitKey, file) -> file.discardedVoiceIds.map { it to unitKey } } +
            doc.story.discardedVoiceIds.map { it to null }
        return withUnit.filter { (id, unitKey) ->
            val block = index.block(id.blockKey)
            if (block != null) block.sceneKey in writtenScenes else unitKey != null && unitKey in writtenUnits
        }.map { it.first }.distinctBy { it.id }.sortedBy { it.discardedAt }
    }
}
