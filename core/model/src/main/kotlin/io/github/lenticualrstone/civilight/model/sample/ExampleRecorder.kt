package io.github.lenticualrstone.civilight.model.sample

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.file.RecorderFile
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.Placement
import io.github.lenticualrstone.civilight.model.recorder.PlacementKind
import io.github.lenticualrstone.civilight.model.recorder.RecordKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.recorder.TranscriptionMode
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.DEVICE
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.at
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.localDate
import io.github.lenticualrstone.civilight.model.sample.ExampleText.link
import io.github.lenticualrstone.civilight.model.sample.ExampleText.t
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/**
 * Life Recorder records that belong with the example (the recorder library lives outside projects,
 * §2.12). R1 … R4 are LRC fixture P1's records; R0 became page 등불 결사 규약; one record is linked
 * to 노아; one voice record is in the recorder trash.
 */
object ExampleRecorder {
    /** Text of LRC R1. */
    const val R1_TEXT = "하르트가 등불을 싫어하는 이유. 대정전 때 누군가를 잃었다는 설정은 어떨까. 그게 세라의 오빠였다면?"

    /** Text of LRC R3 (`음성 1:02`). */
    const val R3_TEXT = "2장 첫 장면은 비 오는 상층 관문에서 시작. 리엔이 처음으로 위를 올려다보는…"

    val TRASHED_RECORD_DELETED_AT: Long = at("2026-10-24T01:00:00Z")

    private fun record(
        key: Key,
        kind: RecordKind,
        createdAt: String,
        runs: List<io.github.lenticualrstone.civilight.model.text.TextRun>,
        durationMs: Long? = null,
        doneMarked: Boolean = false,
    ): RecorderRecord {
        val time = at(createdAt)
        return RecorderRecord(
            key = key, kind = kind.open(), createdAt = time, runs = runs, durationMs = durationMs,
            transcribedBy = if (kind == RecordKind.VOICE) TranscriptionMode.ON_DEVICE.open() else null,
            doneMarked = doneMarked, updatedAt = time, updatedBy = DEVICE,
        )
    }

    fun records(): List<RecorderRecord> = listOf(
        record(
            K.RECORDER_R0, RecordKind.TEXT, "2026-11-05T13:00:00Z",
            listOf(t("등불 결사 규약 초안. 하나, 꺼진 등은 센다. 둘, 남은 등은 지킨다. 셋, 빛은 팔지 않는다.")),
        ),
        record(K.RECORDER_R1, RecordKind.VOICE, "2026-11-21T23:41:00Z", listOf(t(R1_TEXT)), durationMs = 24_000),
        record(
            K.RECORDER_R2, RecordKind.TEXT, "2026-11-21T14:10:00Z",
            listOf(
                t("오웬은 사실 결사 출신? 탈퇴 이유가 "),
                link(K.link(9), LinkTargetKind.CHARACTER, K.SERA, "세라", projectKey = K.PROJECT),
                t("와 관련"),
            ),
        ),
        record(K.RECORDER_R3, RecordKind.VOICE, "2026-11-20T23:15:00Z", listOf(t(R3_TEXT)), durationMs = 62_000),
        record(K.RECORDER_R4, RecordKind.TEXT, "2026-11-18T01:00:00Z", listOf(t("결말: 리엔이 마지막 등불을 스스로 끈다.")), doneMarked = true),
        record(K.RECORDER_NOAH, RecordKind.TEXT, "2026-10-20T12:30:00Z", listOf(t("노아는 시장 골목에서 자란 아이. 오웬이 몰래 돌본다."))),
        record(K.RECORDER_TRASHED, RecordKind.VOICE, "2026-10-23T12:00:00Z", listOf(t("등불 소리 녹음 시험")), durationMs = 12_000)
            .copy(deletedAt = TRASHED_RECORD_DELETED_AT, trashEntryKey = K.trashEntry(5)),
    )

    fun links(): List<RecorderLink> {
        fun link(n: Int, record: Key, kind: RecorderTargetKind, target: Key, origin: LinkOrigin, createdAt: String) = RecorderLink(
            key = K.recorderLink(n), recordKey = record, projectKey = K.PROJECT, targetKind = kind.open(), targetKey = target,
            origin = origin.open(), createdAt = at(createdAt), updatedAt = at(createdAt), updatedBy = DEVICE,
        )
        return listOf(
            link(1, K.RECORDER_R0, RecorderTargetKind.PAGE, K.PAGE_SOCIETY_RULES, LinkOrigin.INSERT, "2026-11-06T12:00:00Z"),
            link(2, K.RECORDER_R1, RecorderTargetKind.UNIT, K.SCENE_1_7_3, LinkOrigin.ENTRY, "2026-11-21T23:41:00Z"),
            link(3, K.RECORDER_R1, RecorderTargetKind.CHARACTER, K.HART, LinkOrigin.MANUAL, "2026-11-21T23:42:00Z"),
            link(4, K.RECORDER_R2, RecorderTargetKind.PROJECT, K.PROJECT, LinkOrigin.ENTRY, "2026-11-21T14:10:00Z"),
            link(5, K.RECORDER_R2, RecorderTargetKind.CHARACTER, K.OWEN, LinkOrigin.MANUAL, "2026-11-21T14:11:00Z"),
            link(6, K.RECORDER_R2, RecorderTargetKind.CHARACTER, K.SERA, LinkOrigin.AT, "2026-11-21T14:11:30Z"),
            link(7, K.RECORDER_NOAH, RecorderTargetKind.CHARACTER, K.NOAH, LinkOrigin.MANUAL, "2026-10-20T12:31:00Z"),
        )
    }

    fun placements(): List<Placement> = listOf(
        Placement(
            key = K.placement(1), recordKey = K.RECORDER_R0, projectKey = K.PROJECT, kind = PlacementKind.PAGE.open(),
            targetKey = K.PAGE_SOCIETY_RULES, copiedRevision = 1, createdAt = at("2026-11-06T12:00:00Z"),
            lastLabel = "등불 결사 규약", updatedAt = at("2026-11-06T12:00:00Z"), updatedBy = DEVICE,
        ),
    )

    /** The recorder library slice of the example. */
    fun library(): RecorderLibrary = RecorderLibrary(records(), links(), placements())

    /** Recorder trash rows (outside projects: `projectKey` null). */
    fun trashEntries(): List<TrashEntry> = listOf(
        TrashEntry(
            key = K.trashEntry(5), kind = TrashKind.RECORDER.open(), targetKeys = listOf(K.RECORDER_TRASHED),
            deletedAt = TRASHED_RECORD_DELETED_AT, purgeOn = TrashEntry.purgeDateFor(localDate(TRASHED_RECORD_DELETED_AT)),
        ),
    )

    /** The example's `recorder.json`: records linked to it, not trashed, without audio (§7.10). */
    fun projectFile(): RecorderFile {
        val library = library()
        val linked = library.recordsLinkedTo(K.PROJECT).filter { it.deletedAt == null && !it.hidden }
        return RecorderFile.forProject(library, K.PROJECT, linked, includeAudio = false)
    }
}
