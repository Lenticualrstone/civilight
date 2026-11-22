package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.CueKind
import io.github.lenticualrstone.civilight.model.script.DirectionCue
import io.github.lenticualrstone.civilight.model.script.ScriptAssetKind
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Game-engine JSON (SCR-63, provisional). Envelope per 00-domain-model C-27 / LIB-45
 * (`civilight-engine`, version 1); scene and block names per SCR-63. Resolved 때 comes from the
 * place & time area; the scene head is never a block. Writing the file fixes the written voice IDs.
 */
object GameJsonExport {
    const val FORMAT = "civilight-engine"
    const val VERSION = 1

    data class Output(val json: JsonObject, val doc: ProjectDoc) {
        fun text(pretty: Boolean = true): String = (if (pretty) PRETTY else Json).encodeToString(JsonObject.serializer(), json)
    }

    private val PRETTY = Json { prettyPrint = true }

    /** Builds the export and the doc with the written voice IDs fixed at [at]. */
    fun export(doc: ProjectDoc, scope: ExportScope, time: SceneTimeSource, at: Timestamp): Output {
        val json = build(doc, scope, time)
        val written = scope.scenes(doc).flatMap { doc.manuscript.liveBlocksOf(it.key) }
        return Output(json, ExportVoices.fix(doc, written, at))
    }

    /** The JSON object (no side effects). */
    fun build(doc: ProjectDoc, scope: ExportScope, time: SceneTimeSource): JsonObject {
        val scenes = scope.scenes(doc)
        val blocks = scenes.flatMap { doc.manuscript.liveBlocksOf(it.key) }
        return buildJsonObject {
            put("format", FORMAT)
            put("version", VERSION)
            putJsonArray("assets") { for (a in assetsUsed(doc, blocks)) add(a) }
            putJsonArray("characters") { for (c in characters(doc, blocks)) add(c) }
            putJsonArray("scenes") { for (s in scenes) add(scene(doc, s, time, scope)) }
        }
    }

    private fun assetsUsed(doc: ProjectDoc, blocks: List<Block>): List<JsonObject> =
        blocks.flatMap { it.cues }.mapNotNull { it.assetKey }.distinct().mapNotNull { key ->
            val a = doc.index.scriptAsset(key) ?: return@mapNotNull null
            buildJsonObject {
                put("id", a.key)
                put("kind", assetKind(a.kind.known) ?: a.kind.raw)
                put("name", a.name)
                put("file", a.fileName)
            }
        }

    private fun assetKind(kind: ScriptAssetKind?): String? = when (kind) {
        ScriptAssetKind.BACKGROUND -> "background"
        ScriptAssetKind.BGM -> "bgm"
        ScriptAssetKind.SFX -> "sfx"
        ScriptAssetKind.CG -> "cg"
        null -> null
    }

    /** Speakers that appear (id, name, display name at their first line in range); no profile data. */
    private fun characters(doc: ProjectDoc, blocks: List<Block>): List<JsonObject> {
        val first = LinkedHashMap<Key, Block>()
        for (b in blocks) {
            val speakers = listOfNotNull(b.speakerKey?.takeIf { b.kind.known == BlockKind.DIALOGUE }) + b.options.mapNotNull { it.speakerKey }
            for (s in speakers) first.putIfAbsent(s, b)
        }
        return first.map { (key, block) ->
            val name = Labels.speakerName(doc, key) ?: ""
            buildJsonObject {
                put("id", key)
                put("name", name)
                put("displayName", Manuscript.displayNameAt(doc, key, block.key) ?: name)
            }
        }
    }

    private fun scene(doc: ProjectDoc, scene: StoryUnit, time: SceneTimeSource, scope: ExportScope): JsonObject {
        val t = time.timeOf(scene.key)
        val blocks = doc.manuscript.liveBlocksOf(scene.key)
        return buildJsonObject {
            put("id", scene.key)
            put("index", Labels.sceneOrdinal(doc, scene.key))
            put("title", scene.title?.let(::JsonPrimitive) ?: JsonNull)
            put("place", place(doc, scene))
            put("time", timeOf(t))
            put("flashback", t?.flashback ?: false)
            put("transition", transition(doc, scene, t))
            putJsonArray("blocks") { for (b in blocks) add(block(doc, b, scope)) }
        }
    }

    private fun place(doc: ProjectDoc, scene: StoryUnit): JsonElement {
        val p = scene.place ?: return JsonNull
        return buildJsonObject {
            put("id", p.key)
            put("name", SceneText.placeName(doc, p))
            put("kind", if (p.kind.known == PlaceRefKind.REGION) "region" else "place")
        }
    }

    /** Resolved 때: story-day range, episode day, band, clock, precision; flashback year. */
    private fun timeOf(t: ResolvedSceneTime?): JsonElement {
        if (t == null) return JsonNull
        return buildJsonObject {
            t.storyDay?.let { putJsonObject("storyDay") { put("min", it.first); put("max", it.last) } }
            t.episodeDay?.let { put("episodeDay", it) }
            t.band?.let { put("band", Labels.bandWord(it)) }
            t.clock?.let { put("clock", it.toString()) }
            put("precision", if (t.approx || t.episodeDayApprox) "approx" else "exact")
            t.flashbackYear?.let { put("year", it) }
        }
    }

    private fun transition(doc: ProjectDoc, scene: StoryUnit, t: ResolvedSceneTime?): JsonElement {
        val text = SceneText.transitionText(doc, scene, t) ?: return JsonNull
        return buildJsonObject {
            put("id", SceneText.transitionId(scene.key))
            put("mode", if (scene.transitionModeOrNone == TransitionMode.AUTO) "auto" else "direct")
            put("text", text)
        }
    }

    private fun block(doc: ProjectDoc, b: Block, scope: ExportScope): JsonObject {
        val voices = ExportVoices.enabled(doc)
        return buildJsonObject {
            put("id", b.key)
            when (b.kind.known) {
                BlockKind.DIALOGUE -> {
                    put("type", "dialogue")
                    put("speaker", b.speakerKey?.let(::JsonPrimitive) ?: JsonNull)
                    b.speakerKey?.let { s -> put("displayName", Manuscript.displayNameAt(doc, s, b.key) ?: Labels.speakerName(doc, s)) }
                    if (doc.project.script.stagePositionEnabled) b.stage?.let { put("stage", it.raw) }
                    put("text", Runs.exportText(b.runs, scope.includeParens))
                    if (voices) VoiceIds.currentId(doc, b)?.let { put("voice", it) }
                }
                BlockKind.NARRATION -> { put("type", "narration"); put("text", Runs.exportText(b.runs, scope.includeParens)) }
                BlockKind.SYSTEM -> { put("type", "system"); put("text", Runs.exportText(b.runs, scope.includeParens)) }
                BlockKind.MEMO -> { put("type", "memo"); put("text", Runs.exportText(b.runs, scope.includeParens)); put("export", false) }
                BlockKind.DIRECTION -> {
                    put("type", "direction")
                    putJsonArray("cues") { for (c in b.cues) add(cue(c)) }
                }
                BlockKind.CHOICE -> {
                    put("type", "choice")
                    putJsonArray("options") {
                        for (o in BlockOps.orderedOptions(b)) addJsonObject {
                            put("id", o.key)
                            put("text", Runs.exportText(o.runs, scope.includeParens))
                            if (voices) VoiceIds.currentIdOfOption(doc, b, o)?.let { put("voice", it) }
                        }
                    }
                    put("rejoin", rejoinOf(doc, b)?.let(::JsonPrimitive) ?: JsonNull)
                }
                null -> { put("type", b.kind.raw); put("text", Runs.exportText(b.runs, scope.includeParens)) }
            }
        }
    }

    /** Choice rejoin: the block that now follows the choice (computed, never stored). */
    fun rejoinOf(doc: ProjectDoc, choice: Block): Key? {
        val blocks = doc.manuscript.liveBlocks
        val i = blocks.indexOfFirst { it.key == choice.key }
        return blocks.getOrNull(i + 1)?.key?.takeIf { i >= 0 }
    }

    /** Direction ops: `background · bgm.play · sfx · cg`, a screen effect by its name (`shake`, `fade`). */
    private fun cue(c: DirectionCue): JsonObject = buildJsonObject {
        when (c.kind.known) {
            CueKind.BACKGROUND -> { put("op", "background"); put("asset", c.assetKey) }
            CueKind.BGM -> { put("op", "bgm.play"); put("asset", c.assetKey) }
            CueKind.SFX -> { put("op", "sfx"); put("asset", c.assetKey) }
            CueKind.CG -> { put("op", "cg"); put("asset", c.assetKey) }
            CueKind.SCREEN_EFFECT -> put("op", c.effect ?: "effect")
            null -> { put("op", c.kind.raw); c.assetKey?.let { put("asset", it) } }
        }
    }
}
