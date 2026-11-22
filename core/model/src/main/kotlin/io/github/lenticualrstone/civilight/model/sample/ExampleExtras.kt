package io.github.lenticualrstone.civilight.model.sample

import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.app.ProjectActivity
import io.github.lenticualrstone.civilight.model.app.ProjectActivityFile
import io.github.lenticualrstone.civilight.model.app.ProjectAppState
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.canvas.Canvas
import io.github.lenticualrstone.civilight.model.canvas.CanvasEdge
import io.github.lenticualrstone.civilight.model.canvas.CanvasNode
import io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind
import io.github.lenticualrstone.civilight.model.canvas.Page
import io.github.lenticualrstone.civilight.model.checks.AckKind
import io.github.lenticualrstone.civilight.model.checks.Acknowledgement
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.file.AssetsIndexFile
import io.github.lenticualrstone.civilight.model.file.ChecksFile
import io.github.lenticualrstone.civilight.model.file.DeletionsFile
import io.github.lenticualrstone.civilight.model.file.TrashIndexFile
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.DEVICE
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.LAST_EDIT
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.at
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.localDate
import io.github.lenticualrstone.civilight.model.sample.ExampleText.link
import io.github.lenticualrstone.civilight.model.sample.ExampleText.t
import io.github.lenticualrstone.civilight.model.script.ScriptAsset
import io.github.lenticualrstone.civilight.model.script.ScriptAssetKind
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.RecorderSource
import io.github.lenticualrstone.civilight.model.text.plainText
import io.github.lenticualrstone.civilight.model.trash.DeletionLogEntry
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Canvas, pages, assets, checks, trash rows, deletion log and app-only state of the example. */
internal object ExampleExtras {
    fun canvases(): List<Canvas> {
        val z = OrderKeys.evenlySpaced(6)
        fun node(n: Int, kind: CanvasNodeKind, ref: String?, x: Float, y: Float, text: String? = null) = CanvasNode(
            key = K.canvasNode(n), kind = kind.open(), refKey = ref, text = text, x = x, y = y, z = z[n - 1],
            updatedAt = LAST_EDIT, updatedBy = DEVICE,
        )
        return listOf(
            Canvas(
                key = K.CANVAS_CHAPTER_1, order = OrderKeys.FIRST, name = "1장 구상", createdAt = at("2026-03-03T11:00:00Z"),
                nodes = listOf(
                    node(1, CanvasNodeKind.FACTION, K.FACTION_LANTERN_SOCIETY, 80f, 60f),
                    node(2, CanvasNodeKind.CHARACTER, K.RIEN, 240f, 160f),
                    node(3, CanvasNodeKind.CHARACTER, K.HART, 420f, 160f),
                    node(4, CanvasNodeKind.EVENT, K.EVENT_BLACKOUT, 80f, 300f),
                    node(5, CanvasNodeKind.TERM, K.TERM_ORIGINIUM_LANTERN, 260f, 320f),
                    node(6, CanvasNodeKind.MEMO, null, 460f, 320f, text = "2장 떡밥?"),
                ),
                edges = listOf(
                    CanvasEdge(
                        key = K.CANVAS_EDGE_RIEN_HART, fromNodeKey = K.canvasNode(2), toNodeKey = K.canvasNode(3),
                        relationKey = K.REL_RIEN_HART, updatedAt = LAST_EDIT, updatedBy = DEVICE,
                    ),
                    CanvasEdge(
                        key = K.canvasEdge(2), fromNodeKey = K.canvasNode(4), toNodeKey = K.canvasNode(5),
                        labelText = "그날 꺼진 빛", updatedAt = LAST_EDIT, updatedBy = DEVICE,
                    ),
                ),
                updatedAt = LAST_EDIT, updatedBy = DEVICE,
            ),
        )
    }

    fun pages(): List<Page> {
        val orders = OrderKeys.evenlySpaced(2)
        val r0 = ExampleRecorder.records().first { it.key == K.RECORDER_R0 }
        val source = RecorderSource(recordKey = r0.key, recordedAt = r0.createdAt)
        return listOf(
            Page(
                key = K.PAGE_LOWER_LIGHT, order = orders[0], name = "하층 구역의 빛", sectionLabel = "설정 노트",
                blocks = doc(700) {
                    paragraph(
                        t("대정전 이후 하층 구역에서 빛은 곧 권력이다. "),
                        link(K.LINK_LPG, LinkTargetKind.TERM, K.TERM_ORIGINIUM_LANTERN, "오리지늄 등불"),
                        t("을 가진 쪽이 거리를 가진다."),
                    )
                    paragraph(
                        t("시장 아이들 중 "),
                        link(K.link(7), LinkTargetKind.CHARACTER, K.NOAH, "노아"),
                        t("만 빛돌 시세를 외운다."),
                    )
                    memo("2장에서 값이 두 배로 뛰는 장면 넣기. 노아 시점으로?")
                },
                createdAt = at("2026-03-01T06:00:00Z"), updatedAt = at("2026-07-02T12:00:00Z"), updatedBy = DEVICE,
            ),
            Page(
                key = K.PAGE_SOCIETY_RULES, order = orders[1], name = "등불 결사 규약", sectionLabel = "세력",
                blocks = doc(720) {
                    memo(r0.runs.plainText, source)
                    paragraph("셋째 조항은 오웬과 거래를 끊은 날 생겼다.")
                },
                sourceRecord = source,
                createdAt = at("2026-11-06T12:00:00Z"), updatedAt = at("2026-11-06T12:00:00Z"), updatedBy = DEVICE,
            ),
        )
    }

    fun assets(): AssetsIndexFile = AssetsIndexFile(
        imageUses = listOf(
            ImageUse(
                key = K.IMAGE_PROJECT_COVER, ownerKind = ImageOwnerKind.PROJECT.open(), ownerKey = K.PROJECT,
                slot = ImageSlot.PROJECT_COVER.open(), source = ImageUse.PRESET_PREFIX + "ash-dawn",
                updatedAt = at("2026-03-01T00:00:00Z"), updatedBy = DEVICE,
            ),
        ),
        scriptAssets = listOf(
            asset(K.ASSET_BG_LOWER_STREET_DAWN, ScriptAssetKind.BACKGROUND, "하층 거리(새벽)", "bg_lower_street_dawn", place = K.PLACE_LOWER_STREET),
            asset(K.ASSET_BG_SOCIETY_GATE_MORNING, ScriptAssetKind.BACKGROUND, "결사의 문(아침)", "bg_society_gate_morning", place = K.PLACE_SOCIETY_GATE),
            asset(K.ASSET_BG_ASH_MARKET_NIGHT, ScriptAssetKind.BACKGROUND, "잿더미 시장(밤)", "bg_ash_market_night", place = K.PLACE_ASH_MARKET),
            asset(K.ASSET_BGM_EMBERS, ScriptAssetKind.BGM, "잔불", "bgm_embers", durationMs = 154_000, loop = true),
            asset(K.ASSET_SFX_WIND, ScriptAssetKind.SFX, "바람", "sfx_wind", durationMs = 4_200),
            asset(K.ASSET_CG_FLICKER, ScriptAssetKind.CG, "깜박이는 등불", "cg_flicker"),
            asset(K.ASSET_BGM_UPPER_BELL, ScriptAssetKind.BGM, "상층의 종", "bgm_upper_bell", durationMs = 98_000, loop = true),
        ),
    )

    private fun asset(
        key: String,
        kind: ScriptAssetKind,
        name: String,
        fileName: String,
        durationMs: Long? = null,
        loop: Boolean = false,
        place: String? = null,
    ) = ScriptAsset(
        key = key, kind = kind.open(), name = name, fileName = fileName, durationMs = durationMs, loop = loop,
        placeKey = place, updatedAt = at("2026-11-15T11:00:00Z"), updatedBy = DEVICE,
    )

    /**
     * Two acknowledgements (`맞다고 표시한 것 2`). Fingerprints are the real values of the domain:story
     * Fingerprints scheme (computed by domain:checks), so both stay acknowledged (STO-41).
     */
    fun checks(): ChecksFile = ChecksFile(
        acknowledgements = listOf(
            Acknowledgement(
                key = K.ACK_TRAVEL_HART, checkType = CheckType.TRAVEL_SHORTAGE,
                subjectKeys = listOf(K.HART, K.SCENE_1_7_2, K.SCENE_1_7_3),
                fingerprint = "sha256-1f4bd89577870ca8310efdafc13f2015eb611d792c8d1bfe4310cc4f5e192f26",
                kind = AckKind.MATCH.open(), createdAt = at("2026-11-21T13:00:00Z"),
                updatedAt = at("2026-11-21T13:00:00Z"), updatedBy = DEVICE,
            ),
            Acknowledgement(
                key = K.ACK_CLOCK_BAND, checkType = CheckType.TIME_CLOCK_BAND_MISMATCH,
                subjectKeys = listOf(K.SCENE_1_5_1),
                fingerprint = "sha256-abbd7af894cac54d25b10f6cd6059928464eec05286a01f9383afba6a3f65d11",
                kind = AckKind.MATCH.open(), createdAt = at("2026-06-02T12:00:00Z"),
                updatedAt = at("2026-06-02T12:00:00Z"), updatedBy = DEVICE,
            ),
        ),
    )

    fun trash(): TrashIndexFile {
        fun entry(key: String, kind: TrashKind, targets: List<String>, deletedAt: Long, groupScene: String? = null) = TrashEntry(
            key = key, kind = kind.open(), projectKey = K.PROJECT, targetKeys = targets, groupSceneKey = groupScene,
            groupDate = if (groupScene != null) localDate(deletedAt) else null,
            deletedAt = deletedAt, purgeOn = TrashEntry.purgeDateFor(localDate(deletedAt)),
        )
        return TrashIndexFile(
            listOf(
                entry(K.TRASH_NOAH, TrashKind.CHARACTER, listOf(K.NOAH), ExampleCast.NOAH_DELETED_AT),
                entry(K.TRASH_SCENE_1_6_5, TrashKind.SCENE, listOf(K.SCENE_1_6_5), ExampleStory.SCENE_1_6_5_DELETED_AT),
                entry(
                    K.TRASH_BLOCKS_1_5_2, TrashKind.BLOCK_GROUP, listOf(K.block(1, 5, 8), K.block(1, 5, 9)),
                    ExampleScript.BLOCKS_1_5_DELETED_AT, groupScene = K.SCENE_1_5_2,
                ),
                entry(K.TRASH_RECORD, TrashKind.CHANGE, listOf(K.RECORD_TRASHED), ExampleChanges.TRASHED_RECORD_DELETED_AT),
            ),
        )
    }

    /** Purged in the last 30 days: 1-2 #007 (2026-11-14) and 1-7 #049 … #057 (2026-11-19). Keys are the purged items' keys. */
    fun deletions(): DeletionsFile = DeletionsFile(
        listOf(DeletionLogEntry(key = K.block(1, 2, 7), kind = "block", deletedAt = at("2026-11-14T02:00:00Z"))) +
            (49..57).map { n -> DeletionLogEntry(key = K.block(1, 7, n), kind = "block", deletedAt = at("2026-11-19T02:00:00Z")) },
    )

    /** `_app/…`: the last writing position is the end of #044 (LRC, SCR T-23-02). */
    fun app(blockTextLength: Int): ProjectAppState = ProjectAppState(
        activity = ProjectActivityFile(
            activity = ProjectActivity(
                lastEditedBlockKey = K.BLOCK_044, lastEditedOffset = blockTextLength,
                lastEditedAt = LAST_EDIT, lastOpenedAt = LAST_EDIT, lastRoute = "script",
            ),
        ),
    )
}
