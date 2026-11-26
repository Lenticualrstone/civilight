package io.github.lenticualrstone.civilight.feature.canvas.image

/**
 * Copy of the image area (CNV-25 … CNV-31): boards `ImageSlots`, `ImagePick`, `ImageCrop`, `MHolo` and
 * the spec 50 tables. `BTN_` keys are buttons. Copy no board gives is the nearest existing wording
 * (listed under Requests).
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

    const val SLOT_GALLERY = "갤러리"
    const val USED_GALLERY = "갤러리 카드 · 목록 썸네일 · 보기는 원본"

    const val USED_PROJECT_COVER = "서재 카드 · 목록 썸네일 · 프로젝트 홈"
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
    /** The system photo picker button: `사진`, not `갤러리`, which is the gallery feature's word (55 §6 Q1 default). */
    const val BTN_PHOTO = "사진"
    const val BTN_FILE = "파일"
    const val BTN_PICK = "고르기"
    const val BTN_CLOSE = "닫기"

    // Cover mode of the picker (U-08 LIB-84 … LIB-87, 60 §7.2; `[PROPOSED]` rows there).
    const val TAB_GALLERY = "갤러리"
    const val COVER_NOW = "지금 표지"
    const val COVER_NONE = "표지 없음 · 첫 글자 카드"
    const val BTN_CROP = "자르기"
    const val BTN_CLEAR = "빼기"
    const val GALLERY_EMPTY = "이미지 없음"
    const val GALLERY_UNTITLED = "제목 없음"
    const val CHIP_COVER = "표지"
    const val CHIP_HOLO = "홀로"
    const val CHIP_HIDDEN = "숨김"
    const val TOAST_COVER_CLEARED = "표지 뺌"
    const val UNDO = "되돌리기"

    /** `숨긴 이미지 · 표지에서는 보임` above the crop controls when a hidden gallery item becomes the cover (GAL-14). */
    const val HIDDEN_SHOWN_IN_COVER = "숨긴 이미지 · 표지에서는 보임"

    /** `지금 표지 · 사진`: the picker's current line (LIB-84); the word never names a gallery item. */
    fun coverNow(word: String): String = "$COVER_NOW · $word"

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

    // Crop previews of the other ratios (CST-13; no board yet): `아바타 1:1 · 자동` follows the first ratio.
    const val CROP_AUTO = "자동"
    const val CROP_OWN = "직접"

    /** `아바타 1:1 · 자동`: a preview's label. */
    fun cropPreview(chip: String, auto: Boolean): String = "$chip · ${if (auto) CROP_AUTO else CROP_OWN}"

    // Copy no board gives (Q-EM-01, Q-IM-05): the nearest existing words, listed under Requests.
    const val MY_IMAGES_EMPTY = "내 이미지 없음"
    const val IMAGE_IMPORT_FAILED = "이미지 가져오기 실패"
    const val IMAGE_UNREADABLE = "이미지 열 수 없음"

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
