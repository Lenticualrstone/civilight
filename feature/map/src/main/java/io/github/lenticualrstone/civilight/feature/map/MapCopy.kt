package io.github.lenticualrstone.civilight.feature.map

/**
 * Every UI string of feature:map (DECISIONS §3). Terse fragments joined with ` · `, no 해요체, no em
 * dash; `BTN_` constants are button labels (CopyLint button rules). Copy is from `40-world.md` and
 * the boards `MapNew`, `Map`, `MapC`, `MapSoft`, `MapEdit`, `MapRouteC`, `MapDetail`, `SceneWhere`.
 * Missing copy is a `TODO_COPY_MAP_…` constant whose value is its own name (listed under Requests).
 */
object MapCopy {
    const val SEP = " · "

    // --- Common ---
    const val BACK = "뒤로"
    const val BTN_CLOSE = "닫기"
    const val BTN_DONE = "완료"
    const val BTN_CANCEL = "취소"
    const val BTN_UNDO = "되돌리기"
    const val READ_ONLY = "읽기 전용"
    const val NONE = "없음"
    const val UNLOCK = "지문으로 보기"
    const val WORLD = "세계"

    // --- WLD-36 지도 목록 · 새 지도 (board MapNew) ---
    const val LIST_TITLE = "지도"
    const val NEW_MAP = "새 지도"
    const val PARENT = "상위"
    const val TO_CHILD = "→ 하위 지도"
    const val NEW_GENERATED = "지형 만들기"
    const val NEW_GENERATED_SUB = "종류 · 땅 비율 · 그림 → 후보 3"
    const val NEW_IMAGE = "이미지 가져오기"
    const val NEW_IMAGE_SUB = "갤러리 · 파일 · 큰 이미지도 그대로"
    const val NEW_SKETCH = "스케치 촬영"
    const val NEW_SKETCH_SUB = "종이에 그린 지도 → 펴기 · 덧그리기"
    const val NEW_NODE = "노드 지도"
    const val NEW_NODE_SUB = "바탕 없이 장소와 연결만"
    const val BTN_GALLERY = "갤러리"
    const val BTN_FILE = "파일"
    const val BTN_SHOOT = "촬영"

    // --- WLD-39 스케치 · 펴기 ---
    const val SKETCH_TITLE = "스케치 촬영"
    const val STRAIGHTEN = "펴기"
    const val BTN_STRAIGHTEN = "펴기"
    const val TRACE = "덧그리기"

    // --- WLD-37 지도 보기 (boards Map, MapC, MapSoft, MapSoftC) ---
    const val VIEW_SMALL = "세계 · 지도"
    const val ALL_PINS = "전체 핀"
    const val BTN_REPLACE_BASE = "지도 이미지 바꾸기"
    const val BTN_EDIT = "편집"
    const val PLACE = "장소"
    const val APPEARANCE = "등장"
    const val REGION_OF_PLACE = "지역"
    const val BACKGROUND_ASSETS = "배경 에셋"
    const val BTN_MOVE_PIN = "핀 옮기기"
    const val BTN_OPEN_PLACE_CARD = "장소 카드 열기"
    const val BTN_PLACE_CARD = "장소 카드"
    const val CHILD_MAP = "하위 지도"

    // --- WLD-40 편집 (boards MapEdit, MapRouteC, TourMap) ---
    const val EDIT_SUFFIX = "편집"
    const val TOOLBAR = "지도 도구"
    const val TOOL_MOVE = "이동"
    const val TOOL_PIN = "핀"
    const val TOOL_REGION = "영역"
    const val TOOL_ROUTE = "경로"
    const val TOOL_LABEL = "라벨"
    const val TOOL_LAYER = "레이어"
    const val TAB_PROPS = "속성"
    const val TAB_LAYERS = "레이어"
    const val TAB_LIST = "목록"
    const val ROUTE_DRAWING = "경로 그리기"

    // --- WLD-41 핀 ---
    const val PIN = "핀"
    const val PIN_PICK_TITLE = "장소 고르기"
    const val NEW_PLACE = "새 장소"
    const val PLACE_NAME = "이름"
    const val BTN_CREATE = "만들기"
    const val REVEAL_POINT = "보이는 시점"
    const val LAYER = "레이어"

    // --- WLD-42 영역 (boards MapEdit, MMapRegion) ---
    const val REGION = "영역"
    const val NEW_REGION_DRAWING = "새 영역 · 그리는 중"
    const val REGION_NAME = "이름"
    const val KIND = "종류"
    const val LINK = "연결"
    const val FACTION = "세력"
    const val COLOR = "색"
    const val BTN_VERTEX_UNDO = "되돌리기"
    const val BTN_VERTEX_CLOSE = "닫기"
    val REGION_KINDS = listOf("세력 영역", "지역", "기타")

    // --- WLD-43 경로 (boards MapRouteC, MMapRoute) ---
    const val ROUTE = "경로"
    const val CUSTOM_MODE = "직접"

    // --- WLD-44 라벨 ---
    const val LABEL = "라벨"
    const val BTN_PLACE_LABEL = "놓기"
    const val CURVE_LABEL = "곡선 라벨"

    // --- WLD-52 지도에서 고르기 ---
    const val PICK_TITLE = "지도에서 고르기"
    const val PICK_PLACE = "장소"
    const val PICK_REGION = "지역"
    const val BTN_PICK = "고르기"

    // --- Undo step labels (also the toast text): `<what> · <subject>` from source words ---
    const val STEP_NEW_MAP = "새 지도"

    // --- 지도 메뉴 `⋯` (WLD-46, WLD-49, WLD-51 entries) ---
    const val MORE = "지도 메뉴"

    // --- WLD-45 레이어 ---
    const val NAME = "이름"

    // --- WLD-46 축척 맞추기 (board MapScale) ---
    const val SCALE_TITLE = "축척"
    const val SCALE_SHEET = "축척 맞추기"
    const val SCALE_HINT = "아는 거리 하나 → 지도 전체"
    const val SCALE_A = "A"
    const val SCALE_B = "B"
    const val DAY_TRAVEL = "하루 이동"
    const val BTN_SAVE_SCALE = "축척 저장"
    /** Unit chips `일 · km · 리 · 직접` (ScaleUnit order DAY, KM, LI, CUSTOM). */
    val SCALE_UNITS = listOf("일", "km", "리", "직접")

    // --- WLD-47 이동 수단 (rule 18 §3, boards ProjSettings, MapRouteC, MapScale) ---
    const val TRAVEL_MODES = "이동 수단"
    /** `하루` + `시간` of a mode's moving hours (rule 18 detail `도보 15 km · 하루 8시간`). */
    const val PER_DAY = "하루"
    const val UNIT_KM = "km"
    const val UNIT_HOURS = "시간"

    // --- WLD-48 하위 지도 ---
    const val BTN_CHILD_CREATE = "만들기"
    const val BTN_CHILD_PICK = "고르기"

    // --- WLD-49 시점별 공개 (boards MapReveal, MMapReveal) ---
    const val REVEAL_TITLE = "시점별 공개"
    const val REVEAL_VISIBLE_ONLY = "보이는 것만"
    const val REVEAL_DIM = "숨은 것 흐리게"

    // --- WLD-50 노드 지도 (board MapNode) ---
    const val NODE_TITLE = "노드 지도"
    const val NODE = "노드"
    const val TOOL_NODE = "노드"
    const val TOOL_LINK = "연결"
    const val NODE_SEARCH = "장소 · 노드 찾기"
    const val RECORDER = "라이프 레코더"

    // --- WLD-51 지도 내보내기 (board SettingsMore, `큰 지도 내보내기 | 4K · 8K`) ---
    const val EXPORT = "지도 내보내기"
    const val EXPORT_SIZE = "큰 지도 내보내기"
    val EXPORT_SIZES = listOf("4K", "8K")

    // --- WLD-53 … 55 지형 만들기 (boards MapFormat, MapGen, MapGenWide) ---
    const val FORMAT_TITLE = "양식"
    const val FORMAT_SMALL = "새 지도 · 지형 만들기 1/2"
    const val GEN_STEP_2 = "지형 만들기 2/2"
    const val GEN_TITLE = "지형 만들기"
    const val FORMAT_HEX_SUB = "칸마다 지형 · 전략 지도"
    const val FORMAT_POLY_SUB = "지방 · 국경 나누기"
    const val FORMAT_DETAIL_SUB = "색면 · 산 · 강 · 길 · 마을"
    const val FORMAT_OUTLINE_SUB = "해안선 · 등고선만"
    const val FORMAT_NODE_SUB = "장소와 연결만"
    const val FORMAT_TEXT = "글로 설명"
    const val FORMAT_TEXT_SUB = "문장 → 조건 → 지형"
    const val FORMAT_TEXT_SAMPLE = "“북쪽은 눈 덮인 산맥, 남쪽 해안에 항구…”"
    const val BTN_PREVIOUS = "이전"
    const val BTN_BY_STEPS = "단계별로 만들기"
    const val BTN_START_MAP = "이 지도로 시작"
    const val BTN_NEW_CANDIDATES = "새 후보"
    const val GEN_BASE = "기본"
    const val GEN_STEPS = "단계 · 고정 · 다시"
    const val CONTOURS = "등고선"
    const val LOCK = "고정"
    const val REROLL = "다시"
    const val LAKE = "호수"
    const val LAKE_SUB = "막힌 곳에 호수"

    // --- Map menu rows (WLD-36 rename, WLD-56 … 62 entries) ---
    const val MAP_NAME = "이름"

    // --- WLD-56 단계 다시 만들기 (boards MapReroll, MMapReroll) ---
    const val REROLL_TITLE = "단계 다시 만들기"
    const val BTN_REROLL = "다시 만들기"
    const val CHECK_POSITIONS = "위치 확인"
    const val BTN_KEEP_SPOT = "원래 자리"
    const val BTN_NEW_SPOT = "지금 자리"

    // --- WLD-57 그림 (boards MapSoft, MapFormat, SettingsMore) ---
    const val STYLE = "그림"

    // --- WLD-58 육각 · 칸 칠하기 (boards MapHex, MMapHexFill) ---
    const val HEX_FORMAT = "육각"
    const val HEX_TITLE = "칸 칠하기"
    const val HEX_TOOL_ONE = "한 칸"
    const val HEX_TOOL_LINE = "줄"
    const val HEX_TOOL_FILL = "채우기"
    const val HEX_TOOL_PICK = "고르기"
    const val HEX_TOOL_UNDO = "되돌리기"
    const val HEX_HINT = "칸 탭 → 고른 지형 · 표식(산 · 숲)은 지형 따라 자동"
    const val BTN_CREATE_PLACE = "장소 만들기"
    const val CELL_SIZE = "칸 크기"
    const val ORIENTATION = "방향"
    val ORIENTATIONS = listOf("뾰족", "평평")
    const val COORDS = "좌표 표시"

    // --- WLD-59 다각형 · 지역 나누기 (board MapPoly) ---
    const val POLY_CRUMB = "지방"
    const val POLY_TITLE = "다각형 · 지역 나누기"
    const val TAB_PROVINCES = "지역"
    const val POLY_HINT = "이름 · 색 · 연결 세력 → 지역 탭"
    const val BTN_NEW_PROVINCE = "새 지역"
    const val POLY_TOOL_PICK = "고르기"
    const val POLY_TOOL_ASSIGN = "칸 넣기"
    const val POLY_TOOL_MERGE = "합치기"
    const val POLY_TOOL_SPLIT = "나누기"
    const val POLY_TOOL_LAND = "땅 · 바다"
    const val BTN_SPLIT = "나누기"
    const val PROVINCE = "지역"
    const val FACTION_LINK = "연결 세력"

    // --- WLD-60 상세 지형 · 편집 (boards MapDetail, MMapStamp) ---
    const val DETAIL_TITLE = "상세 지형 · 편집"
    const val TOOL_STAMP = "도장"
    const val TOOL_RIVER = "강"
    const val TOOL_ROAD = "길"
    const val TAB_STAMPS = "도장"
    const val TAB_LEGEND = "범례"
    const val STAMP_HINT = "탭 → 놓기 · 모서리 → 크기 · 위 손잡이 → 회전"
    const val STAMP_SIZE = "크기"
    val STAMP_SIZES = listOf("1", "2", "3")
    const val PLACE_LINK = "장소 연결"
    const val STAMP = "도장"

    // --- WLD-61 글로 설명 (boards MapText, MapTextResult, MMapText) ---
    const val TEXT_SMALL = "새 지도 · 글로 설명"
    const val TEXT_TITLE = "지형 만들기"
    const val READ_ALONG = "함께 읽기"
    val ENGINES = listOf("기기 안", "클라우드")
    const val BTN_CONDITION = "조건"
    const val BTN_MAKE_FROM_CONDITIONS = "이 조건으로 만들기"
    const val TEXT_RESULT_TITLE = "글로 설명 · 결과"
    const val BTN_POSITION = "위치"
    const val BTN_FIX = "고치기"
    const val PICK_POSITION = "위치 고르기"
    const val COND_WHAT = "무엇"
    const val COND_WHERE = "어디"
    const val COND_TRAITS = "특징"
    const val COND_PLACE = "연결 장소"
    const val CONDITION = "조건"
    const val BTN_REMOVE = "지우기"
    const val SOURCE_PLACES = "장소"
    const val SOURCE_FACTIONS = "세력"
    const val SOURCE_PAGE = "페이지"

    // --- WLD-62 인물 동선 (board MapTrack) ---
    const val TRACK_TITLE = "인물 동선"
    const val TAB_TRACK = "동선"
    const val TAB_TRACK_PLACES = "장소"

    // --- WLD-65 세력 영역의 시점별 모양 (board TimelineMap) ---
    const val SHAPE = "모양"
    const val COMPARE = "비교"

    // --- Missing copy (value = name; listed under Requests) ---

    /** A stored map from another generator version cannot be re-rolled (WLD-53: never changed silently). */
    const val TODO_COPY_MAP_REROLL_OTHER_VERSION = "TODO_COPY_MAP_REROLL_OTHER_VERSION"

    /** The play button of the 인물 동선 scrubber (WLD-62: `can play`, no label drawn). */
    const val TODO_COPY_MAP_TRACK_PLAY = "TODO_COPY_MAP_TRACK_PLAY"

    /** Shown when no reader can read the text (no on-device model, cloud off; WLD-61). */
    const val TODO_COPY_MAP_TEXT_UNREADABLE = "TODO_COPY_MAP_TEXT_UNREADABLE"

    /** 인물 동선 with no stop on this map, or no character chosen (WLD-62). */
    const val TODO_COPY_MAP_TRACK_EMPTY = "TODO_COPY_MAP_TRACK_EMPTY"
    /** Toast when an image or photo cannot be read or stored. Proposed: `이미지 가져오기 · 안 됨`. */
    const val TODO_COPY_MAP_IMPORT_FAILED = "TODO_COPY_MAP_IMPORT_FAILED"

    /** Accessibility labels of the layer order buttons (WLD-45 order is `[PROPOSED]`, no copy). */
    const val TODO_COPY_MAP_LAYER_UP = "TODO_COPY_MAP_LAYER_UP"
    const val TODO_COPY_MAP_LAYER_DOWN = "TODO_COPY_MAP_LAYER_DOWN"

    /** Field label of a node's sub line (`세라 · 1-3`); the board shows the line, not its name. */
    const val TODO_COPY_MAP_NODE_SUB = "TODO_COPY_MAP_NODE_SUB"

    /** Toast when a map image cannot be made or shared (WLD-51). */
    const val TODO_COPY_MAP_EXPORT_FAILED = "TODO_COPY_MAP_EXPORT_FAILED"

    // --- Formatted ---

    /** `잿빛 새벽의 등불 · 세계`. */
    fun listSmall(projectName: String): String = "$projectName$SEP$WORLD"

    /** `세계 · 지도 · 색면`, or `세계 · 지도`. */
    fun viewSmall(styleName: String?): String = if (styleName == null) VIEW_SMALL else "$VIEW_SMALL$SEP$styleName"

    /** `하층 구역 지도 · 편집`. */
    fun editTitle(mapName: String): String = "$mapName$SEP$EDIT_SUFFIX"

    /** `120%`. */
    fun zoomLabel(percent: Int): String = "$percent%"

    /** `세계 지도 · 핀 ‘상층’`. */
    fun parentTag(mapName: String, placeName: String): String = "$mapName$SEP$PIN ‘$placeName’"

    /** `1-7부터`. */
    fun revealFrom(code: String): String = "${code}부터"


    /** `세력 · 상층 경비대`. */
    fun factionLink(name: String): String = "$FACTION$SEP$name"

    /** `지점 3 · 12 km` route draft bar (km only with a scale). */
    fun routeDraftBar(points: Int, km: String?): String = if (km == null) "지점 $points" else "지점 $points$SEP$km km"

    /** Undo label with its subject: `핀 · 결사의 문`, `핀 옮기기 · 결사의 문`, `영역 · 상층 관문 구역`. */
    fun step(label: String, subject: String?): String = if (subject.isNullOrBlank()) label else "$label$SEP$subject"

    /** `지도 3`: the default name a new map gets (`MapEdits.newMap`, WLD-36). */
    fun defaultMapName(n: Int): String = "$LIST_TITLE $n"

    /** `영역 3` default region name (as `지도 N`). */
    fun defaultRegionName(n: Int): String = "$REGION $n"

    /** `핀 옮기기 · 결사의 문`. */
    fun movingPin(placeName: String): String = "$BTN_MOVE_PIN$SEP$placeName"

    /** Style names `색면 · 먹선 · 색 · 양피지`. */
    val STYLE_NAMES = listOf("색면", "먹선", "색", "양피지")

    /** `도보 15 km` (the `하루 이동` value of the scale sheet). */
    fun dayTravel(modeName: String, km: String): String = "$modeName $km $UNIT_KM"

    /** `1-5 시점 · 보이는 것만`. */
    fun revealMode(code: String): String = "$code 시점$SEP$REVEAL_VISIBLE_ONLY"

    /** `상층 관문 · 1-7` (a pin not yet revealed). */
    fun pendingTag(name: String, code: String): String = "$name$SEP$code"

    /** `하층 구역 › 결사 본부` (breadcrumb text). */
    fun crumbs(parts: List<String>): String = parts.joinToString(" › ")

    /** `연결 · 시장 입구` while choosing the second node of a connection. */
    fun linking(fromName: String): String = "$TOOL_LINK$SEP$fromName"

    /** `시드 0031`. */
    fun seed(label: String): String = "시드 $label"

    /** `상세 지형 그림`. */
    fun styleSection(formatName: String): String = "$formatName 그림"

    /** `강 다시 만들기` (a step's menu row; the sheet title comes from `StepReroll.sheet`). */
    fun rerollStep(stepName: String): String = "$stepName $BTN_REROLL"

    /** `세계 지도 · 육각`. */
    fun hexSmall(mapName: String): String = "$mapName$SEP$HEX_FORMAT"

    /** `3칸마다` (`좌표 표시`). */
    fun coordEvery(n: Int): String = "${n}칸마다"

    /** `칸 37` (a province row). */
    fun cells(n: Int): String = "칸 $n"

    /** `칸 1 → 하층 구역` (the floating `칸 넣기` chip). */
    fun assigned(n: Int, provinceName: String): String = "칸 $n → $provinceName"

    /** `지역 3`: the label of a province without a name (as `지도 N`, `영역 N`). */
    fun defaultProvinceName(n: Int): String = "$PROVINCE $n"

    /** `24 km ›` value of `칸 크기`. */
    fun km(value: String): String = "$value $UNIT_KM"

    /** `다른 후보 3`. */
    fun otherCandidates(n: Int): String = "다른 후보 $n"

    /** `장소 6` (a `함께 읽기` source chip). */
    fun sourceCount(label: String, n: Int): String = "$label $n"

    /** `페이지 ‘하층 구역의 빛’`. */
    fun sourcePage(title: String): String = "$SOURCE_PAGE ‘$title’"

    /** `상세 지형 · 색면 · 지형 만들기 2/2` (cover small line). */
    fun genSmall(formatName: String, styleName: String): String = "$formatName$SEP$styleName$SEP$GEN_STEP_2"
}
