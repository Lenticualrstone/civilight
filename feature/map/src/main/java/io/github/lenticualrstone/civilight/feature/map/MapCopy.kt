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

    // --- Missing copy (value = name; listed under Requests) ---
    /** Toast when an image or photo cannot be read or stored. Proposed: `이미지 가져오기 · 안 됨`. */
    const val TODO_COPY_MAP_IMPORT_FAILED = "TODO_COPY_MAP_IMPORT_FAILED"

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

    /** `영역 3` default region name (as `지도 N`). */
    fun defaultRegionName(n: Int): String = "$REGION $n"

    /** `핀 옮기기 · 결사의 문`. */
    fun movingPin(placeName: String): String = "$BTN_MOVE_PIN$SEP$placeName"

    /** Style names `색면 · 먹선 · 색 · 양피지`. */
    val STYLE_NAMES = listOf("색면", "먹선", "색", "양피지")
}
