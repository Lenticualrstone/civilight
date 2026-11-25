package io.github.lenticualrstone.civilight.feature.script.panel

import io.github.lenticualrstone.civilight.domain.cast.DisplayNames
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.domain.script.Manuscript
import io.github.lenticualrstone.civilight.domain.script.Panels
import io.github.lenticualrstone.civilight.domain.script.VoiceIds
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.feature.script.state.ScriptOps
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.VoiceIdFormatMode
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.ScriptPanelTab
import io.github.lenticualrstone.civilight.model.script.StagePosition
import java.time.ZoneId

/** Tabs of the side panel and the cover panel sheet (SCR-31): `시간 · 참고 · 속성 · 모듈`. */
enum class PanelTab(val label: String, val stored: ScriptPanelTab) {
    TIME(ScriptCopy.TAB_TIME, ScriptPanelTab.TIME),
    REF(ScriptCopy.TAB_REF, ScriptPanelTab.REF),
    PROPS(ScriptCopy.TAB_PROPS, ScriptPanelTab.PROPS),
    MODS(ScriptCopy.TAB_MODS, ScriptPanelTab.MODULES),
    ;

    companion object {
        /** Tabs with content, in the drawn order `시간 · 참고 · 속성 · 모듈` (SCR-31, SCR-35). */
        val BUILT: List<PanelTab> = listOf(TIME, REF, PROPS, MODS)

        /** The swipe row's buttons (SCR-30), also drawn in the command row: `◆ 시간 · 참고 · 속성`. */
        val SWIPE: List<PanelTab> = listOf(TIME, REF, PROPS)

        /** The remembered tab of `_app/view.json` (SCR-31 `[PROPOSED]`), a built one. */
        fun fromStored(stored: ScriptPanelTab): PanelTab = entries.firstOrNull { it.stored == stored }?.takeIf { it in BUILT } ?: TIME
    }
}

/** A `속성` field: label above a field box with a value, an optional right note and `바꾸기` (board `WideProps`). */
data class PropField(
    val label: String,
    val value: String,
    val avatar: String? = null,
    val avatarKey: Key? = null,
    val canChange: Boolean = false,
    /** Right side note: `1-9에서 공개`, `자동`, `고정 · 11월 20일 내보냄`. */
    val note: String? = null,
    /** The inner panel's wording of [note] when it differs (`1-9에서 이름 공개`, board `WideProps`). */
    val noteWide: String? = null,
    /** Mono value (voice IDs). */
    val mono: Boolean = false,
    /** Dotted look of a deleted speaker (rule 20). */
    val dotted: Boolean = false,
)

/**
 * A deleted speaker's buttons (SCR-34, SCR-57, rule 20): `화자 바꾸기`, `되살리기` while the
 * character is in the trash, and `이대로 두기` until this line was kept.
 */
data class DeletedSpeaker(val characterKey: Key, val canRestore: Boolean, val canKeep: Boolean = false)

/** `보이스 ID` of one spoken choice option (SCR-23, SCR-09): `#006-1` and its field. */
data class OptionVoice(val number: String, val field: VoiceField)

/** `무대 위치` (SCR-25): `왼쪽 | 가운데 | 오른쪽`, nothing chosen until the writer picks one. */
data class StageField(val selected: StagePosition?, val enabled: Boolean) {
    companion object {
        val OPTIONS: List<Pair<StagePosition, String>> = listOf(
            StagePosition.LEFT to ScriptCopy.STAGE_LEFT,
            StagePosition.CENTER to ScriptCopy.STAGE_CENTER,
            StagePosition.RIGHT to ScriptCopy.STAGE_RIGHT,
        )
    }
}

/** `보이스 ID` (SCR-23): the ID and its state; with the format `직접` the field takes typing. */
data class VoiceField(val id: String?, val state: String?, val editable: Boolean)

/** The `속성` tab of the selected block (SCR-34). */
data class PropsModel(
    /** `고른 블록 · #043`, or `고른 블록 없음` without a selected block. */
    val title: String,
    val blockKey: Key?,
    val kind: PropField?,
    val speaker: PropField?,
    val deletedSpeaker: DeletedSpeaker? = null,
    val displayName: PropField? = null,
    /** The character whose record holds the `표시 이름` history: tapping the field opens it (SCR-34 `[PROPOSED]`). */
    val displayNameOwner: Key? = null,
    val stage: StageField? = null,
    val voice: VoiceField? = null,
    /** Spoken options of a choice block with a voice ID (SCR-23 table: options get `_1`, `_2`). */
    val optionVoices: List<OptionVoice> = emptyList(),
    /** `변화 연결`: `◆ 관계 변화 · 리엔 → 하르트`, one line per record on the block. */
    val changes: List<String> = emptyList(),
    /** Memo from the recorder: `출처 · 레코더 11월 22일 08:41` (SCR-59). */
    val source: PropField? = null,
    /** The record behind [source]; tapping the field opens it in the recorder (SCR-59). */
    val sourceRecordKey: Key? = null,
    val readOnly: Boolean = false,
    /** (0.2.0, N-43) `이 블록만 · 표시 이름` of a dialogue block ("" = none); null on other kinds. */
    val blockDisplayName: String? = null,
    /** (0.2.0, N-41) `복선 · {name} · {열림|거둠}` lines of marks on this block. */
    val marks: List<String> = emptyList(),
    /** `복선 심기` (every kind but `작가 메모`). */
    val canPlant: Boolean = false,
    /** `할 일` of a memo block: null on other kinds; [todoDone] = `끝냄`. */
    val todo: Boolean? = null,
    val todoDone: Boolean = false,
) {
    companion object {
        fun of(doc: ProjectDoc, selected: Key?, readOnly: Boolean, unlocked: Boolean = false, zone: ZoneId = ZoneId.systemDefault()): PropsModel {
            val block = selected?.let(doc.index::block)?.takeIf { doc.manuscript.isLiveBlock(it.key) }
                ?: return PropsModel(ScriptCopy.NO_SELECTED_BLOCK, null, null, null)
            val kind = block.kind.known
            // (0.2.0, SCR-13) Through the kind menu; a conversion its check blocks is dimmed there.
            val canChangeKind = !readOnly && kind != null
            val dialogue = kind == BlockKind.DIALOGUE
            var speaker: PropField? = null
            var deleted: DeletedSpeaker? = null
            var displayName: PropField? = null
            if (dialogue) {
                val label = Manuscript.speakerLabel(doc, block, unlocked)
                when (label) {
                    is Manuscript.SpeakerLabel.Deleted -> {
                        val field = Panels.deletedSpeakerField(doc, block)
                        speaker = PropField(ScriptCopy.SPEAKER_FIELD, field?.text ?: label.text, canChange = false, dotted = true)
                        deleted = block.speakerKey?.let {
                            DeletedSpeaker(
                                it,
                                canRestore = !readOnly && !label.purged,
                                canKeep = !readOnly && !SpeakerKeep.isKept(doc, it, block.key),
                            )
                        }
                    }
                    Manuscript.SpeakerLabel.Hidden ->
                        speaker = PropField(ScriptCopy.SPEAKER_FIELD, Manuscript.SpeakerLabel.Hidden.TEXT, canChange = !readOnly)
                    is Manuscript.SpeakerLabel.Named -> {
                        speaker = PropField(ScriptCopy.SPEAKER_FIELD, label.name, avatar = label.name, avatarKey = block.speakerKey, canChange = !readOnly)
                        val key = block.speakerKey!!
                        val shown = Manuscript.displayNameAt(doc, key, block.key) ?: label.name
                        val note = DisplayNames.propertiesValue(doc, key, ReadPoint.BeforeBlock(block.key)).second
                            ?.takeIf { shown != label.name }
                        displayName = PropField(
                            ScriptCopy.DISPLAY_NAME, shown, note = note,
                            noteWide = note?.removeSuffix(ScriptCopy.REVEAL_SUFFIX)?.let(ScriptCopy::nameRevealWide),
                        )
                    }
                    else -> speaker = PropField(ScriptCopy.SPEAKER_FIELD, ScriptCopy.NO_SPEAKER, canChange = !readOnly)
                }
            }
            val settings = doc.project.script
            // (0.2.0, G-2, SCR-79) Stage and voice rows only with `게임 제작 도구` on; choice options never show an ID (N-23).
            val game = io.github.lenticualrstone.civilight.model.game.GameTools.on(doc)
            val stage = if (dialogue && game && settings.stagePositionEnabled) StageField(block.stage?.known, enabled = !readOnly) else null
            val voice = if (dialogue && game && settings.voiceIdEnabled) voiceField(doc, block, readOnly, zone) else null
            val optionVoices = emptyList<OptionVoice>()
            val changes = PanelRecords.onBlock(doc, block.key).map { PanelRecords.changeLink(doc, it) }
            val source = block.recorderSource?.takeIf { kind == BlockKind.MEMO }?.let {
                PropField(ScriptCopy.SOURCE_FIELD, ScriptCopy.recorderSource(Labels.monthDayTime(it.recordedAt, zone)))
            }
            return PropsModel(
                title = Panels.selectedBlockTitle(block),
                blockKey = block.key,
                kind = PropField(ScriptCopy.KIND_FIELD, Labels.kindLabel(kind), canChange = canChangeKind),
                speaker = speaker,
                deletedSpeaker = deleted,
                displayName = displayName,
                displayNameOwner = block.speakerKey.takeIf { displayName != null },
                stage = stage,
                voice = voice,
                optionVoices = optionVoices,
                changes = changes,
                source = source,
                sourceRecordKey = block.recorderSource?.recordKey.takeIf { source != null },
                readOnly = readOnly,
                blockDisplayName = if (dialogue) block.displayNameOverride.orEmpty() else null,
                // Rule 19: a foreshadowing planted in a locked hidden scene is never named here (as in the pick sheet).
                marks = io.github.lenticualrstone.civilight.model.marks.Marks.on(doc, block.key)
                    .filter { m -> unlocked || !plantHidden(doc, m.foreshadow) }
                    .map { m ->
                    io.github.lenticualrstone.civilight.feature.script.input.WriteCopy.markLine(
                        m.foreshadow.name,
                        if (m.foreshadow.payoff == null) io.github.lenticualrstone.civilight.feature.script.input.WriteCopy.OPEN
                        else io.github.lenticualrstone.civilight.feature.script.input.WriteCopy.PAID_OFF,
                    )
                },
                canPlant = !readOnly && kind != null && kind != BlockKind.MEMO,
                todo = if (kind == BlockKind.MEMO) block.todo != null else null,
                todoDone = block.todo?.done == true,
            )
        }

        /**
         * The foreshadowing's plant sits in a hidden scene (or under a hidden episode): the block it is drawn on
         * now, else the stored one.
         */
        internal fun plantHidden(doc: ProjectDoc, f: io.github.lenticualrstone.civilight.model.marks.Foreshadow): Boolean {
            val marks = io.github.lenticualrstone.civilight.model.marks.Marks
            val blockKey = marks.resolve(doc, f.plant).blockKey ?: f.plant.blockKey
            val sceneKey = (doc.index.block(blockKey) ?: doc.index.block(f.plant.blockKey))?.sceneKey ?: return false
            val unit = doc.index.unit(sceneKey) ?: return false
            return unit.hidden || doc.index.ancestors(unit).any { it.hidden }
        }

        /** Options with an ID (a speaker and a voice code, or a fixed ID), in option order. */
        private fun optionVoices(doc: ProjectDoc, block: io.github.lenticualrstone.civilight.model.script.Block, zone: ZoneId): List<OptionVoice> =
            io.github.lenticualrstone.civilight.domain.script.BlockOps.orderedOptions(block).mapIndexedNotNull { i, option ->
                val id = VoiceIds.currentIdOfOption(doc, block, option) ?: return@mapIndexedNotNull null
                val state = if (VoiceIds.isFixed(option.voice)) ScriptCopy.voiceFixed(option.voice?.fixedAt?.let { Labels.monthDay(it, zone) }) else ScriptCopy.VOICE_AUTO
                OptionVoice(Labels.optionNumber(block.number, i + 1), VoiceField(id, state, editable = false))
            }

        private fun voiceField(doc: ProjectDoc, block: io.github.lenticualrstone.civilight.model.script.Block, readOnly: Boolean, zone: ZoneId): VoiceField? {
            val fixed = VoiceIds.isFixed(block.voice)
            val manual = doc.project.script.voiceIdFormat.mode.known == VoiceIdFormatMode.MANUAL
            val id = VoiceIds.currentId(doc, block)
            if (fixed) {
                val date = block.voice?.fixedAt?.let { Labels.monthDay(it, zone) }
                return VoiceField(id, ScriptCopy.voiceFixed(date), editable = false)
            }
            if (manual) return VoiceField(id, null, editable = !readOnly)
            return id?.let { VoiceField(it, ScriptCopy.VOICE_AUTO, editable = false) }
        }
    }
}
