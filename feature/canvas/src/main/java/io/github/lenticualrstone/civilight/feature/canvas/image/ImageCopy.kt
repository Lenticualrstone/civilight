package io.github.lenticualrstone.civilight.feature.canvas.image

/**
 * Copy of the image area (CNV-25 … CNV-31): boards `ImageSlots`, `ImagePick`, `ImageCrop`, `MHolo` and
 * the spec 50 tables. `BTN_` keys are buttons. Missing copy is a `TODO_COPY_CANVAS_*` constant that holds
 * its own name (listed under Requests).
 */
object ImageCopy {
    const val BACK = "뒤로"
    const val READ_ONLY = "읽기 전용"

    // Slot names, where they are used, holo notes (board `ImageSlots`, CNV-25 table).
    const val SLOT_PROJECT_COVER = "프로젝트 표지"
    const val SLOT_CHARACTER_PORTRAIT = "인물 초상"
    const val SLOT_CHARACTER_BACKGROUND = "인물 배경"
    const val SLOT_CARD_REFERENCE = "장소 · 용어 · 세력"
    const val SLOT_PAGE_HEADER = "페이지 머리 이미지"

    const val USED_PROJECT_COVER = "서재 카드 · 목록 썸네일"
    const val USED_CHARACTER_PORTRAIT = "인물 카드 · 아바타"
    const val USED_CHARACTER_BACKGROUND = "프로필 위 · 아래로 흐려짐"
    const val USED_CARD_REFERENCE = "카드 참고 이미지"
    const val USED_PAGE_HEADER = "페이지 맨 위"

    const val HOLO_CHOICE = "홀로 선택"
    const val HOLO_NONE_BEHIND_TEXT = "홀로 없음 · 글자 뒤"
    const val HOLO_NONE = "홀로 없음"

    /** Short slot words of the crop header (`유리 정원 · 표지`); only `표지` is drawn (Q-IM-05). */
    const val SHORT_PROJECT_COVER = "표지"

    /** Picker title of the cover slot (board `ImagePick`); other slots use their slot name (Q-IM-05). */
    const val PICK_TITLE_COVER = "표지 이미지"

    // Ratio chips: 표지 drawn; the others follow the main-tab wording (Q-IM-05).
    const val CHIP_CARD_3_4 = "카드 3:4"
    const val CHIP_LIST_1_1 = "목록 1:1"
    const val CHIP_CARD_4_5 = "카드 4:5"
    const val CHIP_AVATAR_1_1 = "아바타 1:1"
    const val CHIP_COVER_9_16 = "커버 9:16"
    const val CHIP_INNER_4_3 = "펼친 화면 4:3"
    const val CHIP_16_9 = "16:9"
    const val CHIP_21_9 = "21:9"

    // Image specs screen (board `ImageSlots`).
    const val SPECS_SMALL = "규격"
    const val SPECS_TITLE = "이미지 · 자르기 · 홀로"
    const val SPECS_SECTION_SLOTS = "이미지가 들어가는 곳"
    const val SPECS_SECTION_RULES = "규칙"
    val SPECS_RULES: List<String> = listOf(
        "원본 그대로 보관 · 자르기는 언제든 다시",
        "한 이미지 → 규격마다 따로 자르기",
        "기본 이미지 · 앱에 포함 · 오프라인",
        "홀로 · 기본 끔 · 이미지마다 켜기",
    )

    // Presets (CNV-26) and the preset view (`설정 · 지도 · 이미지 · 연표 › 기본 이미지`, view not drawn).
    val PRESET_NAMES: List<String> = listOf("잿빛 새벽", "등불", "심해", "안개 숲", "사막 유적", "성운", "붉은 성채", "설원")
    const val PRESETS_SMALL = "설정"
    const val PRESETS_TITLE = "기본 이미지"
    const val PRESETS_SUB = "8종 · 앱에 포함 · 오프라인"

    // Picker (board `ImagePick`).
    const val TAB_PRESETS = "기본 이미지"
    const val TAB_MINE = "내 이미지"
    const val HOLO = "홀로그램 효과"
    const val HOLO_SUB = "은빛 · 기울이면 흐름"
    const val BTN_GALLERY = "갤러리"
    const val BTN_FILE = "파일"
    const val BTN_PICK = "고르기"
    const val BTN_CLOSE = "닫기"

    // Crop editor (board `ImageCrop`).
    const val BTN_CANCEL = "취소"
    const val CROP_TITLE = "자르기"
    const val BTN_APPLY = "적용"
    const val FIXED_RATIOS = "규격 고정"
    const val ZOOM = "확대"
    const val BTN_ROTATE = "90° 회전"
    const val BTN_FLIP = "좌우 반전"
    const val BTN_RESET = "처음대로"
    const val HOLO_SUB_CROP = "은빛 · 기울이면 흐름 · 애니메이션 줄이기면 고정"

    // Missing copy (Q-EM-01, Q-IM-05).
    const val TODO_COPY_CANVAS_MY_IMAGES_EMPTY = "TODO_COPY_CANVAS_MY_IMAGES_EMPTY"
    const val TODO_COPY_CANVAS_IMAGE_IMPORT_FAILED = "TODO_COPY_CANVAS_IMAGE_IMPORT_FAILED"
    const val TODO_COPY_CANVAS_IMAGE_UNREADABLE = "TODO_COPY_CANVAS_IMAGE_UNREADABLE"

    /** `기본 이미지 8`, `내 이미지 3`: tab with its count. */
    fun tab(label: String, count: Int): String = "$label $count"

    /** `유리 정원 · 카드 3:4 · 목록 1:1`: owner name and the slot's ratio chips. */
    fun pickSubtitle(ownerName: String, chips: List<String>): String = (listOf(ownerName) + chips).joinToString(" · ")

    /** `유리 정원 · 표지`: crop header line. */
    fun cropHeader(ownerName: String, slotShort: String): String = "$ownerName · $slotShort"

    /** `2160 × 2880 · 원본 3024 × 4032`. */
    fun readout(cropW: Int, cropH: Int, sourceW: Int, sourceH: Int): String = "$cropW × $cropH · 원본 $sourceW × $sourceH"

    /** `1.4×`. */
    fun zoomValue(zoom: Float): String {
        val tenths = Math.round(zoom * 10f)
        return "${tenths / 10}.${tenths % 10}×"
    }
}
