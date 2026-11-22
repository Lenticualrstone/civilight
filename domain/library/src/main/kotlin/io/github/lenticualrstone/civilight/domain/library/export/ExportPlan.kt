package io.github.lenticualrstone.civilight.domain.library.export

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.DiscardedVoiceId
import io.github.lenticualrstone.civilight.model.script.VoiceState
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.text.TextRun

/**
 * What a reading/engine export writes (LIB-42…46). The library area decides **what** goes into the
 * file (range, hidden scenes, speakers' profile depth, secret fields, CSV row states, voice-ID
 * fixation); the script area serialises the plan into PDF, CSV and engine JSON (domain:library must
 * not depend on domain:script). Recorder records never go into these outputs (E-13).
 */
data class ExportPlan(
    val format: ExportFormat,
    val projectKey: Key,
    val projectName: String,
    /** Written scenes in manuscript order. */
    val scenes: List<PlannedScene>,
    /** Hidden scenes in range left out by `빼기`. */
    val leftOutScenes: List<Key>,
    /** Speakers of written lines (JSON `characters[]`, CSV `화자` names). */
    val characters: List<PlannedCharacter>,
    /** JSON: character field definitions to export (secret ones only with `비밀 항목 포함`). */
    val fieldDefKeys: List<Key>,
    /** CSV `버림` rows: discarded voice IDs of the range (never rows left out by `빼기`). */
    val discardedVoiceIds: List<DiscardedVoiceId>,
    /** PDF display layers (ids of [ExportLayer]). */
    val layers: Set<String>,
    val fileName: String,
    val warnings: List<ExportWarning>,
) {
    /** CSV and JSON fix the voice IDs they write; PDF and `.civilight` never do (LIB-46). */
    val fixesVoiceIds: Boolean get() = format == ExportFormat.CSV || format == ExportFormat.ENGINE_JSON

    /** Dialogue lines written, including spoken choice options, in manuscript order. */
    val writtenLines: List<WrittenLine> by lazy {
        scenes.flatMap { scene ->
            scene.blocks.flatMap { block -> WrittenLine.of(block) }
        }
    }

    /** Recorder records written (always none for these formats). */
    val recorderRecordKeys: List<Key> get() = emptyList()
}

/** One written scene. The scene head is not a block (R22 §6); it is derived from these fields. */
data class PlannedScene(
    val sceneKey: Key,
    /** `1-7 씬 4`. */
    val label: String,
    val title: String?,
    val hidden: Boolean,
    /** Live blocks in order (memo blocks included; the serialiser applies `export:false` and layers). */
    val blocks: List<Block>,
    val place: PlaceRef?,
    /** Stored time; the story area resolves it (base + interval) for the engine (LIB-45). */
    val time: SceneTime,
    val flashback: Boolean,
    val transition: PlannedTransition?,
)

/** A scene's 전환 문구: direct runs, or auto (text made by the script area from its template). */
data class PlannedTransition(val mode: TransitionMode, val runs: List<TextRun>?)

/** How much of a speaker the engine JSON carries (LIB-45, CF §8, R19 §5). */
enum class CharacterProfile {
    /** Fields and history. */
    FULL,

    /** Hidden character: id · 이름 · 표시 이름 only. */
    NAME_ONLY,
}

/** A speaker of written lines. Trashed and purged speakers keep their name (R20 §2). */
data class PlannedCharacter(
    val key: Key,
    val name: String,
    val displayNameBase: String?,
    val profile: CharacterProfile,
    val hidden: Boolean,
    /** In the trash, or purged (name from the name tag). */
    val deleted: Boolean,
)

/** One dialogue line of a dialogue block, or a spoken choice option ([optionKey] set). */
data class WrittenLine(
    val sceneKey: Key,
    val blockKey: Key,
    val optionKey: Key?,
    val speakerKey: Key?,
    val runs: List<TextRun>,
    val voice: VoiceState?,
) {
    companion object {
        fun of(block: Block): List<WrittenLine> = buildList {
            if (block.kind.known == io.github.lenticualrstone.civilight.model.script.BlockKind.DIALOGUE) {
                add(WrittenLine(block.sceneKey, block.key, null, block.speakerKey, block.runs, block.voice))
            }
            if (block.kind.known == io.github.lenticualrstone.civilight.model.script.BlockKind.CHOICE) {
                block.options.sortedBy { it.order }.filter { it.speakerKey != null || it.voice != null }.forEach { o ->
                    add(WrittenLine(block.sceneKey, block.key, o.key, o.speakerKey, o.runs, o.voice))
                }
            }
        }
    }
}

/** A warning shown in the sheet before creating. */
sealed interface ExportWarning {
    val text: String

    /** ▲ `숨긴 씬으로 가는 선택지 <n>` (JSON with `빼기`, E-06). */
    data class ChoiceIntoLeftOutScene(val choiceBlockKeys: List<Key>) : ExportWarning {
        override val text: String get() = "▲ 숨긴 씬으로 가는 선택지 ${choiceBlockKeys.size}"
    }
}
