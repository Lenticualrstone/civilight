package io.github.lenticualrstone.civilight.model.sample

import io.github.lenticualrstone.civilight.model.Key

/**
 * Fixed keys of the example project 「잿빛 새벽의 등불」.
 *
 * Every key is a valid ULID: the time part [TIME] plus a 4-letter family tag plus 12 digits. The
 * family functions ([scene], [block], [character] …) build the same strings as the constants, so a
 * test can write `ExampleKeys.block(1, 7, 43)` or `ExampleKeys.BLOCK_043`. Crockford base 32 has no
 * I, L, O or U, which is why some tags read `P1CE`, `P1NS`, `HREF`.
 */
@Suppress("MemberVisibilityCanBePrivate")
object ExampleKeys {
    /** Shared ULID time part (2025, before any example timestamp). */
    const val TIME = "01JZ000000"

    private fun key(tag: String, number: String): Key {
        require(tag.length == 4 && number.length == 12) { "bad key parts $tag/$number" }
        return TIME + tag + number
    }

    private fun n12(n: Int): String = n.toString().padStart(12, '0')

    // --- Families -------------------------------------------------------------------------------

    fun chapter(c: Int): Key = key("CHAP", n12(c))
    fun episode(c: Int, e: Int): Key = key("EPSD", "00000000" + two(c) + two(e))
    fun scene(c: Int, e: Int, s: Int): Key = key("SCNE", "000000" + two(c) + two(e) + two(s))

    /** Block `#n` of episode `c-e` (also used for numbers that were issued and purged). */
    fun block(c: Int, e: Int, n: Int): Key = key("BKNM", two(c) + two(e) + "0000" + n.toString().padStart(4, '0'))

    /** Character by its `인물 기록` number. */
    fun character(recordNo: Int): Key = key("CHAR", n12(recordNo))
    fun fieldDef(n: Int): Key = key("FDEF", n12(n))
    fun fieldOption(def: Int, option: Int): Key = key("FCHC", def.toString().padStart(6, '0') + option.toString().padStart(6, '0'))
    fun tag(n: Int): Key = key("TAGS", n12(n))
    fun relationKind(n: Int): Key = key("RKND", n12(n))
    fun relation(n: Int): Key = key("RSHP", n12(n))
    fun voiceLine(n: Int): Key = key("VCES", n12(n))
    fun archiveDoc(n: Int): Key = key("ARCH", n12(n))
    fun term(n: Int): Key = key("TERM", n12(n))
    fun place(n: Int): Key = key("P1CE", n12(n))
    fun faction(n: Int): Key = key("FCTN", n12(n))
    fun event(n: Int): Key = key("EVNT", n12(n))
    fun map(n: Int): Key = key("MAPS", n12(n))
    fun pin(n: Int): Key = key("P1NS", n12(n))
    fun region(n: Int): Key = key("RGNS", n12(n))
    fun route(n: Int): Key = key("RTES", n12(n))
    fun mapLayer(n: Int): Key = key("SHTS", n12(n))
    fun mapLabel(n: Int): Key = key("MTXT", n12(n))
    fun stamp(n: Int): Key = key("STMP", n12(n))
    fun drawnLine(n: Int): Key = key("DRWN", n12(n))
    fun mapNode(n: Int): Key = key("NDES", n12(n))
    fun mapNodeLink(n: Int): Key = key("NDJN", n12(n))
    fun canvas(n: Int): Key = key("CNVS", n12(n))
    fun canvasNode(n: Int): Key = key("CNND", n12(n))
    fun canvasEdge(n: Int): Key = key("CNED", n12(n))
    fun page(n: Int): Key = key("PAGE", n12(n))
    fun docBlock(n: Int): Key = key("DCBK", n12(n))
    fun record(n: Int): Key = key("CHGR", n12(n))
    fun link(n: Int): Key = key("HREF", n12(n))
    fun scriptAsset(n: Int): Key = key("ASST", n12(n))
    fun directionCue(n: Int): Key = key("DRCT", n12(n))
    fun choiceOption(n: Int): Key = key("CHPT", n12(n))
    fun imageUse(n: Int): Key = key("PCTR", n12(n))
    fun acknowledgement(n: Int): Key = key("ACKS", n12(n))
    fun trashEntry(n: Int): Key = key("TRSH", n12(n))
    fun travelMode(n: Int): Key = key("TRVM", n12(n))
    fun recorderRecord(n: Int): Key = key("RCRD", n12(n))
    fun recorderLink(n: Int): Key = key("RCJN", n12(n))
    fun placement(n: Int): Key = key("RCPM", n12(n))

    private fun two(n: Int): String = n.toString().padStart(2, '0')

    // --- Project ------------------------------------------------------------------------------

    const val PROJECT = TIME + "PRJT000000000001"

    // --- Story structure ----------------------------------------------------------------------

    const val CHAPTER_1 = TIME + "CHAP000000000001"
    const val CHAPTER_2 = TIME + "CHAP000000000002"

    const val EPISODE_1_1 = TIME + "EPSD000000000101"
    const val EPISODE_1_2 = TIME + "EPSD000000000102"
    const val EPISODE_1_3 = TIME + "EPSD000000000103"
    const val EPISODE_1_4 = TIME + "EPSD000000000104"
    const val EPISODE_1_5 = TIME + "EPSD000000000105"
    const val EPISODE_1_6 = TIME + "EPSD000000000106"
    const val EPISODE_1_7 = TIME + "EPSD000000000107"
    const val EPISODE_1_8 = TIME + "EPSD000000000108"
    const val EPISODE_1_9 = TIME + "EPSD000000000109"
    const val EPISODE_1_10 = TIME + "EPSD000000000110"
    const val EPISODE_2_3 = TIME + "EPSD000000000203"
    const val EPISODE_2_5 = TIME + "EPSD000000000205"

    /** Fixture T S1: 1-1 씬 1 `첫 순찰`. */
    const val SCENE_1_1_1 = TIME + "SCNE000000010101"
    const val SCENE_1_1_2 = TIME + "SCNE000000010102"
    const val SCENE_1_1_3 = TIME + "SCNE000000010103"

    /** Fixture T S2: 1-2 씬 1 `새벽 순찰` (holds b12). */
    const val SCENE_1_2_1 = TIME + "SCNE000000010201"
    const val SCENE_1_2_2 = TIME + "SCNE000000010202"
    const val SCENE_1_2_3 = TIME + "SCNE000000010203"

    /** Fixture T S3: 1-3 씬 1, no place, no 때. */
    const val SCENE_1_3_1 = TIME + "SCNE000000010301"

    /** Fixture T S4: 1-3 씬 2 `결사 합류` (holds b32; story event 결사 합류). */
    const val SCENE_1_3_2 = TIME + "SCNE000000010302"
    const val SCENE_1_3_3 = TIME + "SCNE000000010303"
    const val SCENE_1_4_1 = TIME + "SCNE000000010401"

    /** Fixture T S5: 1-4 씬 2 `세라의 회상`, 회상 · 대정전 그날 (holds b41, b42). */
    const val SCENE_1_4_2 = TIME + "SCNE000000010402"
    const val SCENE_1_4_3 = TIME + "SCNE000000010403"
    const val SCENE_1_5_1 = TIME + "SCNE000000010501"
    const val SCENE_1_5_2 = TIME + "SCNE000000010502"
    const val SCENE_1_5_3 = TIME + "SCNE000000010503"
    const val SCENE_1_5_4 = TIME + "SCNE000000010504"

    /** Fixture T S6: 1-6 씬 1 `잿더미 시장` (holds b61). */
    const val SCENE_1_6_1 = TIME + "SCNE000000010601"
    const val SCENE_1_6_2 = TIME + "SCNE000000010602"
    const val SCENE_1_6_3 = TIME + "SCNE000000010603"
    const val SCENE_1_6_4 = TIME + "SCNE000000010604"

    /** 1-6 씬 5 `시장의 밤`, in the trash since 2026-11-04. */
    const val SCENE_1_6_5 = TIME + "SCNE000000010605"

    /** Fixture T S7: 1-7 씬 1 `첫 번째 깜박임`. */
    const val SCENE_1_7_1 = TIME + "SCNE000000010701"

    /** Fixture T S8: 1-7 씬 2 `상층 관문`. */
    const val SCENE_1_7_2 = TIME + "SCNE000000010702"

    /** Fixture T S9: 1-7 씬 3 `꺼진 등불`, 결사의 문, 2일째 새벽 05:40; the 집필 선 is its end. */
    const val SCENE_1_7_3 = TIME + "SCNE000000010703"

    /** Fixture T S10: 1-7 씬 4 (미정), 결사의 문, 06:10, 직접 전환 문구; no blocks. */
    const val SCENE_1_7_4 = TIME + "SCNE000000010704"

    /** Fixture T S11: 1-8 씬 1 `송전탑 아래`; one empty block #001. */
    const val SCENE_1_8_1 = TIME + "SCNE000000010801"
    const val SCENE_1_8_2 = TIME + "SCNE000000010802"
    const val SCENE_1_9_1 = TIME + "SCNE000000010901"

    /** Fixture T S12: 1-9 씬 2, 직접 · 이야기 20일째 · 낮. */
    const val SCENE_1_9_2 = TIME + "SCNE000000010902"
    const val SCENE_1_10_1 = TIME + "SCNE000000011001"
    const val SCENE_1_10_2 = TIME + "SCNE000000011002"
    const val SCENE_1_10_3 = TIME + "SCNE000000011003"
    const val SCENE_2_3_1 = TIME + "SCNE000000020301"

    /** 2-3 씬 2 `경비대 검문` (planned, 하르트). */
    const val SCENE_2_3_2 = TIME + "SCNE000000020302"
    const val SCENE_2_5_1 = TIME + "SCNE000000020501"

    // --- Blocks of 1-7 씬 3 (SCR fixture F1) and named blocks of WLD fixture F --------------------

    const val BLOCK_036 = TIME + "BKNM010700000036"
    const val BLOCK_037 = TIME + "BKNM010700000037"
    const val BLOCK_038 = TIME + "BKNM010700000038"
    const val BLOCK_039 = TIME + "BKNM010700000039"
    const val BLOCK_040 = TIME + "BKNM010700000040"

    /** Direction: 배경 `하층 거리(새벽)`, BGM `잔불`. */
    const val BLOCK_041 = TIME + "BKNM010700000041"

    /** 리엔 `(작게) 저기… 불빛이 보여요. …` (WLD b7342). */
    const val BLOCK_042 = TIME + "BKNM010700000042"

    /** 하르트 `멈추지 마. 저건 등불이 아니야.` — R4, R5, R5a anchor here (WLD b7343). */
    const val BLOCK_043 = TIME + "BKNM010700000043"

    /** Narration with link l44; `◇ 첫 공개` of 오리지늄 등불 (WLD b7344). */
    const val BLOCK_044 = TIME + "BKNM010700000044"

    /** 리엔 `(숨을 삼키며) 하르트 씨, 방금…` (WLD b7345). */
    const val BLOCK_045 = TIME + "BKNM010700000045"

    /** Author memo `여기서 등불이 두 번째로 깜박이게 할지`. */
    const val BLOCK_046 = TIME + "BKNM010700000046"
    const val BLOCK_047 = TIME + "BKNM010700000047"

    /** Last block before the 집필 선. */
    const val BLOCK_048 = TIME + "BKNM010700000048"

    /** 1-7 씬 2 #027: dialogue with no speaker (`▲ 화자 없음`). */
    const val BLOCK_1_7_027 = TIME + "BKNM010700000027"

    /** 1-8 씬 1 #001: an empty narration block (no content, so the 집필 선 does not move). */
    const val BLOCK_1_8_001 = TIME + "BKNM010800000001"

    /** WLD b12: 1-2 씬 1 리엔 `등불 속 그 돌, 오리지… 뭐라고 했죠?` (plain, cut-off candidate). */
    const val BLOCK_B12 = TIME + "BKNM010200000003"

    /** WLD b32: 1-3 씬 2 세라 `빛돌 하나면 한 달은 버텨.` (alias candidate). */
    const val BLOCK_B32 = TIME + "BKNM010300000007"

    /** 1-4 씬 2 #004: narration with a link to term 대정전; its `◇ 첫 공개` (WLD-T34 b41). */
    const val BLOCK_B41 = TIME + "BKNM010400000004"

    /** WLD b42: 1-4 씬 2 세라 `[오리지늄]은 타지 않아. 빛만 남지.` (`▲ 공개 전 사용`). */
    const val BLOCK_B42 = TIME + "BKNM010400000005"

    /** WLD b61: 1-6 씬 1 오웬 `하층에서 빛돌을 판다고?` (alias candidate). */
    const val BLOCK_B61 = TIME + "BKNM010600000003"

    /** 1-1 씬 1 #001: first block of the manuscript; `◇ 첫 공개` of term 등불 결사. */
    const val BLOCK_1_1_001 = TIME + "BKNM010100000001"

    /** 1-5 씬 2 #006: the choice block (options `시장 쪽으로 간다` · `결사로 돌아간다`). */
    const val BLOCK_1_5_CHOICE = TIME + "BKNM010500000006"

    /** 1-6 씬 2 #007: 노아's first line, fixed voice ID `NOA_0106_007`; N1 anchors here. */
    const val BLOCK_1_6_007 = TIME + "BKNM010600000007"

    /** 1-6 씬 4 #025: 오웬 `(장부를 덮으며) 백 번째…`; N2 anchors here. */
    const val BLOCK_1_6_025 = TIME + "BKNM010600000025"

    // --- Characters (key encodes the 인물 기록 number) --------------------------------------------

    const val MARTHA = TIME + "CHAR000000000001"
    const val SERA = TIME + "CHAR000000000002"
    const val OWEN = TIME + "CHAR000000000003"
    const val HART = TIME + "CHAR000000000004"
    const val KAIL = TIME + "CHAR000000000005"
    const val IRENA = TIME + "CHAR000000000006"
    const val RIEN = TIME + "CHAR000000000007"

    /** In the trash since 2026-10-26 (`3일 남음` on 2026-11-22). */
    const val NOAH = TIME + "CHAR000000000008"
    const val DOR = TIME + "CHAR000000000009"
    const val TEO = TIME + "CHAR000000000010"
    const val ELIO = TIME + "CHAR000000000011"
    const val GORDON = TIME + "CHAR000000000012"

    // --- Field definitions ------------------------------------------------------------------------

    const val FIELD_BIRTH = TIME + "FDEF000000000001"
    const val FIELD_AGE = TIME + "FDEF000000000002"
    const val FIELD_BIRTHDAY = TIME + "FDEF000000000003"

    /** 소속, `짧은 글` (see fixture.md: not `연결`). */
    const val FIELD_AFFILIATION = TIME + "FDEF000000000004"

    /** 신장, number in cm, export name `height`. */
    const val FIELD_HEIGHT = TIME + "FDEF000000000005"
    const val FIELD_GENDER = TIME + "FDEF000000000006"
    const val FIELD_SPECIES = TIME + "FDEF000000000007"
    const val FIELD_PERSONALITY = TIME + "FDEF000000000008"
    const val FIELD_CODENAME = TIME + "FDEF000000000009"
    const val FIELD_ORIGIN = TIME + "FDEF000000000010"
    const val FIELD_SPECIALTY = TIME + "FDEF000000000011"

    /** Place field 상태. */
    const val FIELD_PLACE_STATE = TIME + "FDEF000000000012"

    /** Faction field 수장. */
    const val FIELD_FACTION_LEADER = TIME + "FDEF000000000013"

    const val OPTION_GENDER_FEMALE = TIME + "FCHC000006000001"
    const val OPTION_GENDER_MALE = TIME + "FCHC000006000002"
    const val OPTION_GENDER_UNSAID = TIME + "FCHC000006000003"
    const val OPTION_SPECIES_HUMAN = TIME + "FCHC000007000001"
    const val OPTION_SPECIES_BEASTFOLK = TIME + "FCHC000007000002"
    const val OPTION_SPECIES_FAE = TIME + "FCHC000007000003"
    const val OPTION_SPECIES_AUTOMATON = TIME + "FCHC000007000004"
    const val OPTION_SPECIALTY_LAMP_REPAIR = TIME + "FCHC000011000001"
    const val OPTION_SPECIALTY_PATHFINDING = TIME + "FCHC000011000002"
    const val OPTION_SPECIALTY_SWORD = TIME + "FCHC000011000003"
    const val OPTION_SPECIALTY_RECORDS = TIME + "FCHC000011000004"
    const val OPTION_SPECIALTY_BARTER = TIME + "FCHC000011000005"
    const val OPTION_SPECIALTY_CLERIC = TIME + "FCHC000011000006"
    const val OPTION_SPECIALTY_HEALING = TIME + "FCHC000011000007"

    // --- Tags ---------------------------------------------------------------------------------------

    const val TAG_LEAD = TIME + "TAGS000000000001"
    const val TAG_LANTERN_SOCIETY = TIME + "TAGS000000000002"
    const val TAG_LOWER_DISTRICT = TIME + "TAGS000000000003"
    const val TAG_UPPER = TIME + "TAGS000000000004"

    // --- Relation kinds (rule 13 seed list) --------------------------------------------------------

    const val KIND_COLLEAGUE = TIME + "RKND000000000001"
    const val KIND_SIBLING = TIME + "RKND000000000002"
    const val KIND_SPOUSE = TIME + "RKND000000000003"
    const val KIND_LOVER = TIME + "RKND000000000004"
    const val KIND_FRIEND = TIME + "RKND000000000005"
    const val KIND_TRADE_PARTNER = TIME + "RKND000000000006"
    const val KIND_ENEMY = TIME + "RKND000000000007"
    const val KIND_PARENT_CHILD = TIME + "RKND000000000008"
    const val KIND_MASTER_PUPIL = TIME + "RKND000000000009"
    const val KIND_SUPERIOR_SUBORDINATE = TIME + "RKND000000000010"
    const val KIND_SENIOR_JUNIOR = TIME + "RKND000000000011"
    const val KIND_TRUST = TIME + "RKND000000000012"
    const val KIND_RELIANCE = TIME + "RKND000000000013"
    const val KIND_ADMIRATION = TIME + "RKND000000000014"
    const val KIND_FONDNESS = TIME + "RKND000000000015"
    const val KIND_PROTECTION = TIME + "RKND000000000016"
    const val KIND_WARINESS = TIME + "RKND000000000017"
    const val KIND_DISTRUST = TIME + "RKND000000000018"
    const val KIND_JEALOUSY = TIME + "RKND000000000019"
    const val KIND_HATRED = TIME + "RKND000000000020"
    const val KIND_GUILT = TIME + "RKND000000000021"
    const val KIND_INDIFFERENCE = TIME + "RKND000000000022"

    // --- Relations ----------------------------------------------------------------------------------

    /** 리엔 (A) · 하르트 (B): empty base; R4–R7a. */
    const val REL_RIEN_HART = TIME + "RSHP000000000001"

    /** 리엔 (A) · 세라 (B): 사이 선배↔후배 (세라 holds 선배), 리엔's 마음 의지. */
    const val REL_RIEN_SERA = TIME + "RSHP000000000002"

    /** 리엔 (A) · 오웬 (B): 사이 거래 상대. */
    const val REL_RIEN_OWEN = TIME + "RSHP000000000003"

    /** 세라 (A) · 하르트 (B): 사이 동료. */
    const val REL_SERA_HART = TIME + "RSHP000000000004"

    /** 오웬 (A) · 노아 (B): not live (노아 trashed); N2. */
    const val REL_OWEN_NOAH = TIME + "RSHP000000000005"

    // --- Voice lines and archive documents ----------------------------------------------------------

    /** 리엔 `신뢰 상승 후`, empty (`▲ 보이스 비어 있음`). */
    const val VOICE_RIEN_AFTER_TRUST = TIME + "VCES000000000004"
    const val ARCHIVE_RIEN_JOURNAL = TIME + "ARCH000000000001"
    const val ARCHIVE_RIEN_BLACKOUT = TIME + "ARCH000000000002"
    const val ARCHIVE_HART_NOTEBOOK = TIME + "ARCH000000000003"

    // --- World ----------------------------------------------------------------------------------------

    /** T1 `오리지늄 등불`, aliases 오리지늄 · 빛돌, `◇` on #044. */
    const val TERM_ORIGINIUM_LANTERN = TIME + "TERM000000000001"

    /** `대정전`, `◇` on b41 (1-4 씬 2). */
    const val TERM_BLACKOUT = TIME + "TERM000000000002"

    /** `등불 결사`, `◇` on 1-1 씬 1 #001. */
    const val TERM_LANTERN_SOCIETY = TIME + "TERM000000000003"

    /** `잿불의 서약`, `◇` planned at unit 2-5 start. */
    const val TERM_EMBER_OATH = TIME + "TERM000000000004"

    /** `상층 송전망`, `◇` planned at 2장 start. */
    const val TERM_UPPER_GRID = TIME + "TERM000000000005"

    const val PLACE_LOWER_STREET = TIME + "P1CE000000000001"
    const val PLACE_SOCIETY_GATE = TIME + "P1CE000000000002"
    const val PLACE_ASH_MARKET = TIME + "P1CE000000000003"
    const val PLACE_UPPER_GATE = TIME + "P1CE000000000004"
    const val PLACE_PYLON_RUINS = TIME + "P1CE000000000005"
    const val PLACE_ARCHIVE_ROOM = TIME + "P1CE000000000006"

    /** `상층`: its pin on 세계 지도 is the parent of 하층 구역 지도 (WLD-T55). */
    const val PLACE_UPPER_CITY = TIME + "P1CE000000000007"

    const val FACTION_LANTERN_SOCIETY = TIME + "FCTN000000000001"
    const val FACTION_UPPER_GUARD = TIME + "FCTN000000000002"

    /** World event 대정전, 1195, at 하층 거리. */
    const val EVENT_BLACKOUT = TIME + "EVNT000000000001"

    /** World event 상층 송전망 사고, 1201, revealed from 2장. */
    const val EVENT_GRID_ACCIDENT = TIME + "EVNT000000000002"

    /** Story event `리엔, 결사 합류` on 1-3 씬 2 (anchor event of Fixture T). */
    const val EVENT_JOINS_SOCIETY = TIME + "EVNT000000000003"

    // --- Maps ---------------------------------------------------------------------------------------

    const val MAP_WORLD = TIME + "MAPS000000000001"

    /** 하층 구역 지도, child of 세계 지도 via [PIN_WORLD_UPPER_CITY]; scale 2일 도보 per 0.25. */
    const val MAP_LOWER_DISTRICT = TIME + "MAPS000000000002"

    /** Node map 잿더미 시장, child of 하층 구역 지도 via [PIN_ASH_MARKET]. */
    const val MAP_ASH_MARKET = TIME + "MAPS000000000003"

    const val PIN_WORLD_UPPER_CITY = TIME + "P1NS000000000001"
    const val PIN_WORLD_PYLON_RUINS = TIME + "P1NS000000000002"
    const val PIN_SOCIETY_GATE = TIME + "P1NS000000000003"
    const val PIN_LOWER_STREET = TIME + "P1NS000000000004"
    const val PIN_ASH_MARKET = TIME + "P1NS000000000005"
    const val PIN_ARCHIVE_ROOM = TIME + "P1NS000000000006"
    const val PIN_UPPER_GATE = TIME + "P1NS000000000007"

    /** Region 하층 구역 on 하층 구역 지도 (a scene `지역`, revealed from 1-1). */
    const val REGION_LOWER_DISTRICT = TIME + "RGNS000000000001"

    /** Region 상층 경비대 on 세계 지도; shape `◆` from 1-9. */
    const val REGION_UPPER_GUARD = TIME + "RGNS000000000002"

    /** 결사의 문 → 상층 관문, 38 km. */
    const val ROUTE_GATE_TO_UPPER_GATE = TIME + "RTES000000000001"

    /** 결사의 문 → 잿더미 시장. */
    const val ROUTE_GATE_TO_MARKET = TIME + "RTES000000000002"

    // --- Canvas and pages ---------------------------------------------------------------------------

    /** Canvas `1장 구상`. */
    const val CANVAS_CHAPTER_1 = TIME + "CNVS000000000001"

    /** Edge 리엔 — 하르트 backed by [REL_RIEN_HART]. */
    const val CANVAS_EDGE_RIEN_HART = TIME + "CNED000000000001"

    /** Page `하층 구역의 빛` (WLD pg1). */
    const val PAGE_LOWER_LIGHT = TIME + "PAGE000000000001"

    /** Page `등불 결사 규약`, made from recorder record [RECORDER_R0]. */
    const val PAGE_SOCIETY_RULES = TIME + "PAGE000000000002"

    // --- Link runs (link keys) ----------------------------------------------------------------------

    /** l44: #044 → 오리지늄 등불. */
    const val LINK_L44 = TIME + "HREF000000000001"

    /** l42: b42 → 오리지늄 등불 (text `오리지늄`). */
    const val LINK_L42 = TIME + "HREF000000000002"

    /** lpg: page 하층 구역의 빛 → 오리지늄 등불. */
    const val LINK_LPG = TIME + "HREF000000000003"

    // --- Change records -----------------------------------------------------------------------------

    /** R1 리엔 · 소속 `없음` at unit 1-1 start (note `하층 구역의 고아`). */
    const val R1 = TIME + "CHGR000000000001"

    /** R2 리엔 · 소속 `등불 결사` at unit 1-3 start (note `세라의 안내로 합류`). */
    const val R2 = TIME + "CHGR000000000002"

    /** R3 리엔 · 소속 `등불 결사 · 등불지기장` at unit 2-5 start (예정). */
    const val R3 = TIME + "CHGR000000000003"

    /** R4 리엔의 마음 → 하르트 `경계` (`불빛을 아는 낯선 사람`) at #043. */
    const val R4 = TIME + "CHGR000000000004"

    /** R5 하르트의 마음 → 리엔 `보호` at #043. */
    const val R5 = TIME + "CHGR000000000005"

    /** R5a 하르트's 호칭 for 리엔 `꼬마` at #043. */
    const val R5A = TIME + "CHGR000000000006"

    /** R6 사이 `동료` at unit 1-9 start (예정). */
    const val R6 = TIME + "CHGR000000000007"

    /** R7 리엔의 마음 → 하르트 `신뢰` at unit 1-9 start (예정). */
    const val R7 = TIME + "CHGR000000000008"

    /** R7a 리엔's 호칭 for 하르트 `하르트` at unit 1-9 start (예정). */
    const val R7A = TIME + "CHGR000000000009"

    /** R8 하르트 · 표시 이름 cleared (show 이름) at unit 1-9 start (예정). */
    const val R8 = TIME + "CHGR000000000010"

    /** R9 `◇ 첫 공개` 오리지늄 등불 at #044. */
    const val R9 = TIME + "CHGR000000000011"
    const val REVEAL_BLACKOUT = TIME + "CHGR000000000012"
    const val REVEAL_LANTERN_SOCIETY = TIME + "CHGR000000000013"
    const val REVEAL_EMBER_OATH = TIME + "CHGR000000000014"
    const val REVEAL_UPPER_GRID = TIME + "CHGR000000000015"

    /** W1 마르타 · 소속 `등불 결사` at world event 대정전 (base `상층 경비대`). */
    const val W1 = TIME + "CHGR000000000016"

    /** W2 하층 거리 · 상태 `대정전 이후` at world event 대정전 (base `빛 있음`). */
    const val W2 = TIME + "CHGR000000000017"

    /** Region 상층 경비대 shape S2 at unit 1-9 start (예정). */
    const val REGION_SHAPE_1_9 = TIME + "CHGR000000000018"

    /** N1 노아 · 소속 `잿더미 시장 심부름꾼` at 1-6 #007 (target in the trash). */
    const val N1 = TIME + "CHGR000000000019"

    /** N2 오웬의 마음 → 노아 `보호` at 1-6 #025 (relation not live). */
    const val N2 = TIME + "CHGR000000000020"

    /** 세라 · 코드네임 record, removed by `기록 지우기` on 2026-11-18. */
    const val RECORD_TRASHED = TIME + "CHGR000000000021"

    // --- Script assets ------------------------------------------------------------------------------

    const val ASSET_BG_LOWER_STREET_DAWN = TIME + "ASST000000000001"
    const val ASSET_BG_SOCIETY_GATE_MORNING = TIME + "ASST000000000002"
    const val ASSET_BG_ASH_MARKET_NIGHT = TIME + "ASST000000000003"
    const val ASSET_BGM_EMBERS = TIME + "ASST000000000004"
    const val ASSET_SFX_WIND = TIME + "ASST000000000005"
    const val ASSET_CG_FLICKER = TIME + "ASST000000000006"

    /** Not used by any cue (`안 쓰는 것`). */
    const val ASSET_BGM_UPPER_BELL = TIME + "ASST000000000007"

    // --- Checks, trash, travel ----------------------------------------------------------------------

    /** `맞음` on `travel.shortage` 하르트 1-7 씬 2 → 씬 3. */
    const val ACK_TRAVEL_HART = TIME + "ACKS000000000001"

    /** `맞음` on `time.clockBandMismatch` 1-5 씬 1 (새벽 + 06:20). */
    const val ACK_CLOCK_BAND = TIME + "ACKS000000000002"

    const val TRASH_NOAH = TIME + "TRSH000000000001"
    const val TRASH_SCENE_1_6_5 = TIME + "TRSH000000000002"
    const val TRASH_BLOCKS_1_5_2 = TIME + "TRSH000000000003"
    const val TRASH_RECORD = TIME + "TRSH000000000004"

    const val TRAVEL_WALK = TIME + "TRVM000000000001"
    const val TRAVEL_HORSE = TIME + "TRVM000000000002"
    const val TRAVEL_SHIP = TIME + "TRVM000000000003"

    const val IMAGE_PROJECT_COVER = TIME + "PCTR000000000001"

    // --- Recorder (library outside the project) -----------------------------------------------------

    /** Made into page 등불 결사 규약 (state 넣음). */
    const val RECORDER_R0 = TIME + "RCRD000000000000"

    /** LRC R1: voice, 2026-11-22 08:41 KST, links [1-7 씬 3 (entry), 하르트 (manual)]. */
    const val RECORDER_R1 = TIME + "RCRD000000000001"

    /** LRC R2: text with `@세라`, links [project (entry), 오웬 (manual), 세라 (at)]. */
    const val RECORDER_R2 = TIME + "RCRD000000000002"

    /** LRC R3: voice 1:02, no links. */
    const val RECORDER_R3 = TIME + "RCRD000000000003"

    /** LRC R4: text, `끝냄`. */
    const val RECORDER_R4 = TIME + "RCRD000000000004"

    /** Linked to 노아 (manual): his `레코더 칩 1`. */
    const val RECORDER_NOAH = TIME + "RCRD000000000005"

    /** Voice 0:12 in the recorder trash since 2026-10-24. */
    const val RECORDER_TRASHED = TIME + "RCRD000000000006"
}
