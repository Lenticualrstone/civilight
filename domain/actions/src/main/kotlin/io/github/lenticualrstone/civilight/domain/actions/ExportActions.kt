package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.cast.export.CharacterExport
import io.github.lenticualrstone.civilight.domain.cast.field.FieldDefinitions
import io.github.lenticualrstone.civilight.domain.library.export.CharacterProfile
import io.github.lenticualrstone.civilight.domain.library.export.CsvRowPlan
import io.github.lenticualrstone.civilight.domain.library.export.CsvRows
import io.github.lenticualrstone.civilight.domain.library.export.ExportPlan
import io.github.lenticualrstone.civilight.domain.library.export.ExportRun
import io.github.lenticualrstone.civilight.domain.library.export.ExportDefaults
import io.github.lenticualrstone.civilight.domain.library.export.ExportRunOutcome
import io.github.lenticualrstone.civilight.domain.library.export.ExportStart
import io.github.lenticualrstone.civilight.domain.library.export.ExportedFile
import io.github.lenticualrstone.civilight.domain.library.export.VoiceFixation
import io.github.lenticualrstone.civilight.domain.library.export.ExportSerializer
import io.github.lenticualrstone.civilight.domain.library.export.WrittenLine
import io.github.lenticualrstone.civilight.domain.script.DialogueCsvExport
import io.github.lenticualrstone.civilight.domain.script.ExportScope
import io.github.lenticualrstone.civilight.domain.script.GameJsonExport
import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.domain.script.Runs
import io.github.lenticualrstone.civilight.domain.script.SceneText
import io.github.lenticualrstone.civilight.domain.script.StoryTimes
import io.github.lenticualrstone.civilight.domain.script.VoiceIds
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.app.ExportOptions
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Reading/engine exports (DECISIONS §1): `domain:library` decides **what** is written once
 * ([ExportRun] / `ExportPlanner`: range, hidden scenes `넣기 · 빼기`, speakers' profile depth, secret
 * fields, CSV row states, discarded IDs, the auth gate and voice-ID fixation); this object maps that
 * plan onto `domain:script`'s serialisers for the 대사 표 CSV ([DialogueCsvExport] cells and quoting)
 * and the engine JSON ([GameJsonExport.build]). The script's own `export(...)` functions are not
 * called: they would fix voice IDs on their own. PDF is drawn by `feature:library` from the same plan.
 */
object ExportActions {
    private val PRETTY = Json { prettyPrint = true }

    /**
     * `<형식> 만들기` for CSV and engine JSON: [ExportRun.run] with this module's serialiser and the
     * script's voice IDs. PDF and `.civilight` are refused here (feature:library, `ProjectFileExport`).
     */
    fun run(doc: ProjectDoc, options: ExportOptions, authenticated: Boolean, now: Timestamp): ExportRunOutcome =
        run(doc, options, authenticated, ActionContext(now))

    /**
     * [ExportRun.run]'s steps (gate, plan, serialiser, fixation of exactly the lines written) with
     * the 0.2.0 rules: choice options are never written as spoken lines and get no voice ID (N-23),
     * and the returned document carries one automatic `내보낸 판` per exported numbering unit (D21).
     */
    fun run(doc: ProjectDoc, options: ExportOptions, authenticated: Boolean, ctx: ActionContext): ExportRunOutcome {
        val serializer = requireNotNull(serializerFor(options.format)) { "no serialiser here for ${options.format}" }
        val plan = when (val start = ExportStart.of(doc, options, authenticated)) {
            ExportStart.Cancelled -> return ExportRunOutcome.Cancelled
            is ExportStart.Write -> withoutChoiceVoices(start.plan)
        }
        val bytes = serializer.write(doc, plan)
        val fixed = VoiceFixation.apply(doc, plan, ctx.now, voiceIdOf(doc))
        val versioned = VersionActions.captureForExport(fixed, plan, ctx)
        val file = ExportedFile(ExportRun.fileName(options.fileName, options.format), ExportDefaults.mimeType(options.format), bytes)
        return ExportRunOutcome.Done(file, versioned, plan)
    }

    /**
     * (0.2.0, N-23) [plan] with choice options carrying no speaker and no voice, so no option is a
     * written line (no CSV row, no voice-ID fixation). Stored 0.1.0 option speakers and IDs stay in
     * the document; only the plan forgets them.
     */
    fun withoutChoiceVoices(plan: ExportPlan): ExportPlan = plan.copy(
        scenes = plan.scenes.map { scene ->
            scene.copy(
                blocks = scene.blocks.map { b ->
                    if (b.kind.known == BlockKind.CHOICE && b.options.any { it.speakerKey != null || it.voice != null }) {
                        b.copy(options = b.options.map { it.copy(speakerKey = null, voice = null) })
                    } else {
                        b
                    }
                },
            )
        },
    )

    /** The serialiser of [format], or null for PDF (feature:library) and `.civilight`. */
    fun serializerFor(format: ExportFormat): ExportSerializer? = when (format) {
        ExportFormat.CSV -> CSV
        ExportFormat.ENGINE_JSON -> ENGINE_JSON
        ExportFormat.PDF, ExportFormat.PROJECT_FILE -> null
    }

    /** 대사 표 CSV (UTF-8 with BOM, CRLF, RFC 4180 quoting as [DialogueCsvExport.toCsv]). */
    val CSV: ExportSerializer = ExportSerializer { doc, plan -> DialogueCsvExport.toCsv(csvRows(doc, plan)).toByteArray(Charsets.UTF_8) }

    /** Engine JSON (`civilight-engine`, LIB-45), pretty-printed UTF-8. */
    val ENGINE_JSON: ExportSerializer = ExportSerializer { doc, plan ->
        PRETTY.encodeToString(JsonObject.serializer(), engineJson(doc, plan)).toByteArray(Charsets.UTF_8)
    }

    /**
     * The voice ID each written line is written with: the script's current ID (fixed, else manual,
     * else automatic). The same function goes to [ExportRun] for fixation, so the file and the
     * fixed IDs agree.
     */
    fun voiceIdOf(doc: ProjectDoc): (WrittenLine) -> String? = { line ->
        val block = doc.index.block(line.blockKey)
        when {
            block == null -> null
            line.optionKey == null -> VoiceIds.currentId(doc, block)
            else -> block.options.firstOrNull { it.key == line.optionKey }?.let { VoiceIds.currentIdOfOption(doc, block, it) }
        }
    }

    /**
     * CSV rows from the plan ([CsvRows.of]: order, `새 · 바뀜 · 버림` states and the discarded IDs in
     * range are the library's), with the script's cell text: export text with `(…)` (SCR Q-10),
     * speaker names (trashed and purged speakers keep their name), the transition phrase text and ID.
     * With voice IDs off the voice column stays empty.
     */
    fun csvRows(doc: ProjectDoc, plan: ExportPlan): List<DialogueCsvExport.Row> {
        val voices = doc.project.script.voiceIdEnabled
        val time = StoryTimes.source(doc)
        return CsvRows.of(plan, voiceIdOf(doc)).mapNotNull { row ->
            when (row) {
                is CsvRowPlan.Line -> DialogueCsvExport.Row(
                    speaker = row.speakerName ?: row.line.speakerKey?.let { Labels.speakerName(doc, it) }.orEmpty(),
                    line = Runs.exportText(row.line.runs, includeParens = true),
                    voiceId = if (voices) row.voiceId.orEmpty() else "",
                    id = row.line.blockKey,
                    state = row.state?.label.orEmpty(),
                )
                is CsvRowPlan.Transition -> {
                    val scene = doc.index.unit(row.sceneKey) ?: return@mapNotNull null
                    val text = SceneText.transitionText(doc, scene, time.timeOf(row.sceneKey)) ?: return@mapNotNull null
                    DialogueCsvExport.Row(row.speakerCell, text, "", SceneText.transitionId(row.sceneKey), "")
                }
                is CsvRowPlan.Discarded -> {
                    val block = doc.index.block(row.discarded.blockKey)
                    val option = row.discarded.optionKey?.let { k -> block?.options?.firstOrNull { it.key == k } }
                    val speaker = (option?.speakerKey ?: block?.speakerKey)?.let { Labels.speakerName(doc, it) }.orEmpty()
                    val runs = option?.runs ?: block?.runs
                    DialogueCsvExport.Row(
                        speaker = speaker,
                        line = runs?.let { Runs.exportText(it, includeParens = true) }.orEmpty(),
                        voiceId = row.discarded.id,
                        id = row.discarded.blockKey,
                        state = row.state.label,
                    )
                }
            }
        }
    }

    /**
     * The engine JSON of the plan: [GameJsonExport.build] over exactly the planned scenes (hidden
     * ones only when the plan writes them), with `characters[]` from the plan: every speaker of a
     * written line, `id · name · displayName` for hidden and deleted speakers (R19 §5, R20 §2), and
     * for the others the planned character fields ([ExportPlan.fieldDefKeys], secret ones only with
     * `비밀 항목 포함`) as `domain:cast` [CharacterExport] writes them.
     */
    fun engineJson(doc: ProjectDoc, plan: ExportPlan): JsonObject {
        val built = if (plan.scenes.isEmpty()) {
            // An empty unit list means "the whole project" to the script scope; an empty plan writes nothing.
            buildJsonObject {
                put("format", GameJsonExport.FORMAT)
                put("version", GameJsonExport.VERSION)
                putJsonArray("assets") {}
                putJsonArray("characters") {}
                putJsonArray("scenes") {}
            }
        } else {
            GameJsonExport.build(doc, scope(plan), StoryTimes.source(doc))
        }
        return JsonObject(built + ("characters" to characters(doc, plan, built["characters"] as? JsonArray)))
    }

    /** The script scope that writes exactly the plan's scenes (they are already filtered for `빼기`). */
    fun scope(plan: ExportPlan): ExportScope =
        ExportScope(unitKeys = plan.scenes.map { it.sceneKey }, includeHiddenScenes = true, includeParens = true)

    private fun characters(doc: ProjectDoc, plan: ExportPlan, scriptEntries: JsonArray?): JsonArray {
        val byId = scriptEntries.orEmpty().mapNotNull { it as? JsonObject }.associateBy { it["id"]?.jsonPrimitive?.content }
        val defs = plan.fieldDefKeys.mapNotNull(doc.index::fieldDef)
        return buildJsonArray {
            for (pc in plan.characters) {
                val script = byId[pc.key]
                val entry = buildJsonObject {
                    put("id", pc.key)
                    put("name", pc.name)
                    put("displayName", script?.get("displayName") ?: kotlinx.serialization.json.JsonPrimitive(pc.displayNameBase ?: pc.name))
                    val character = doc.index.character(pc.key)
                    if (pc.profile == CharacterProfile.FULL && character != null && !character.hidden) {
                        val visible = FieldDefinitions.visibleFor(doc, EntityKind.CHARACTER, pc.key).map { it.key }.toSet()
                        put(
                            "fields",
                            buildJsonObject {
                                for (def in defs.filter { it.key in visible }) {
                                    CharacterExport.field(doc, pc.key, def)?.let { put(CharacterExport.exportKey(def), it) }
                                }
                            },
                        )
                    }
                }
                add(entry)
            }
        }
    }
}
