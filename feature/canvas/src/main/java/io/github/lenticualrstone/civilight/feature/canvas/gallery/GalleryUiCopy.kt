package io.github.lenticualrstone.civilight.feature.canvas.gallery

/**
 * Copy of the gallery screen and viewer (55 GAL-04 … GAL-09, GAL-23, GAL-25), verbatim from the spec. The card's
 * own words (`메뉴`, `제목 없음`, `초상` …) are core:ui `GalleryCardCopy`; the locked row is domain:library
 * `LockedItemsRow`. Named `GalleryUiCopy` because domain:actions already has a `GalleryCopy` (toasts).
 */
object GalleryUiCopy {
    const val SEP = " · "

    // Header (GAL-04)
    const val TITLE = "갤러리"
    const val BACK = "뒤로"
    const val ADD = "이미지 넣기"

    // Filter chips and order line (GAL-04, GAL-10)
    const val FILTER_ALL = "전체"
    const val FILTER_PROJECT = "프로젝트"
    const val FILTER_CHARACTERS = "인물"
    const val SORT_A11Y = "정렬"
    const val SORT_MANUAL = "직접 순서"
    const val SORT_ADDED = "넣은 날"
    const val SORT_SCENE = "씬 순서"
    const val HIDDEN_COUNT = "숨김"
    const val UNIT_IMAGES = "장"

    // Pick and panel (GAL-08)
    const val HINT_CARD = "카드 누르기 · 설명 보기"
    const val HINT_INNER = "이미지 누르기 · 설명 보기"
    const val UNTITLED = "제목 없음"
    const val NO_LINK = "연결 없음"
    const val NO_DESCRIPTION = "설명 없음"
    const val HIDDEN_SCENE = "숨긴 씬"
    const val DELETED_SCENE = "지운 씬"
    const val BTN_MORE = "더 보기"
    const val BTN_LESS = "접기"
    const val BTN_VIEW = "보기"
    const val BTN_EDIT = "고치기"
    const val BTN_WRITE = "설명 쓰기"
    const val MENU = "메뉴"
    const val BTN_BACK_TO_PICK = "고른 카드로"
    const val PICKED_IMAGE = "고른 이미지"
    const val HIDDEN_TAG = "숨김"

    // Unlocked hidden items (GAL-19, board GalUnlocked)
    const val UNLOCKED_OPEN = "열림"
    const val UNLOCKED_LEAVE = "앱을 떠나면 잠김"
    const val BTN_RELOCK = "다시 잠그기"

    // Viewer (GAL-09)
    const val BTN_CLOSE = "닫기"
    const val BTN_PREV = "이전"
    const val BTN_TEXT = "설명"
    const val BTN_NEXT = "다음"
    const val ORIGINAL = "원본"
    const val PRESET = "기본 이미지"
    const val ADDED_ON = "넣은 날"

    // Empty and filtered-empty (GAL-25)
    const val EMPTY_LINE = "기본 이미지 8 · 사진 · 파일 · 원본 그대로"
    const val OFFER_PORTRAIT = "초상 이미지도 넣기"
    const val OFFER_COVER = "표지 이미지도 넣기"
    const val NOW_PORTRAIT = "지금 초상"
    const val NOW_COVER = "지금 표지"
    const val PHOTO = "사진"
    const val ONE_ORIGINAL = "원본 하나"
    const val BTN_ADD = "넣기"
    const val TAG_EMPTY = "이 태그의 이미지 없음"
    const val BTN_SHOW_ALL = "전체 보기"

    // Project gallery groups (GAL-10)
    const val GALLERY_LINK = "갤러리 ›"

    // Read-only example and owner not reachable
    const val READ_ONLY = "읽기 전용"
    const val GONE = "갤러리 없음"
    const val BTN_UNLOCK = "지문으로 보기"

    // Add (GAL-11, board GalAdd)
    const val ADD_TITLE = "갤러리에 넣기"
    const val ADD_MANY = "여러 장"
    const val ADD_IN_ORDER = "고른 순서대로"
    const val ADD_NOTE = "원본 그대로 · 같은 원본은 한 번만 저장"
    const val BTN_PHOTO = "사진"
    const val BTN_FILE = "파일"
    const val SOURCE_FILE = "파일"

    // Info sheet (GAL-13, board GalInfo)
    const val INFO_TITLE = "이미지 정보"
    const val FIELD_TITLE = "제목"
    const val FIELD_DESCRIPTION = "설명"
    const val LINK_HINT = "@ 인물 · 용어 · 장소 잇기"
    const val LINK_PICKER_TITLE = "항목 고르기"
    const val FIELD_TAGS = "태그"
    const val FIELD_NAME = "이름"
    const val FIELD_LINK = "연결"
    const val SCENE = "씬"
    const val BTN_UNLINK = "빼기"
    const val BTN_CHANGE = "바꾸기"
    const val BTN_PICK_SCENE = "씬 고르기"
    const val AS_IS = "원본 그대로"

    // Menu (GAL-14, board GalMore)
    const val TO_PORTRAIT = "초상으로"
    const val TO_BACKGROUND = "배경으로"
    const val TO_COVER = "표지로"
    const val PORTRAIT_SUB = "인물 카드 · 아바타 · 카드 4:5 · 아바타 1:1"
    const val BACKGROUND_SUB = "프로필 위 · 커버 9:16 · 펼친 화면 4:3"
    const val COVER_SUB = "서재 카드 · 목록 썸네일 · 카드 3:4 · 목록 1:1"
    const val NOW_BACKGROUND = "지금 배경"
    const val HIDDEN_IMAGE = "숨긴 이미지"
    const val SHOWN_IN_PORTRAIT = "초상에서는 보임"
    const val SHOWN_IN_BACKGROUND = "배경에서는 보임"
    const val SHOWN_IN_COVER = "표지에서는 보임"
    const val BTN_CROP = "자르기"
    const val CROP_SUB = "갤러리 카드 3:4 · 목록 1:1 · 원본 그대로"
    const val CROP_PRESET_SUB = "기본 이미지 · 자르기 없음"
    const val BTN_COPY_TO = "다른 곳에도 넣기"
    const val COPY_TO_SUB = "원본 하나 · 프로젝트 · 다른 인물"
    const val ORDER = "순서"
    const val ONLY_IN_ALL = "전체에서만"
    const val BTN_FIRST = "맨 앞"
    const val BTN_FORWARD = "앞으로"
    const val BTN_BACKWARD = "뒤로"
    const val BTN_LAST = "맨 뒤"
    const val BTN_REORDER = "순서 바꾸기"
    const val REORDER_SUB = "전체 순서 · 옮기기"
    const val HOLO = "홀로그램 효과"
    const val HOLO_SUB = "은빛 · 기울이면 흐름"
    const val BTN_HIDE = "숨기기"
    const val BTN_UNHIDE = "숨김 풀기"
    const val HIDE_SUB = "지문 · PIN으로 열림"
    const val BTN_DELETE = "지우기"
    const val DELETE_SUB = "휴지통 · 30일 뒤 삭제"

    // Order screen (GAL-15, boards GalOrder, GalOrderMove)
    const val BTN_MOVE = "옮기기"
    const val MOVING = "옮기는 중"
    const val BTN_CANCEL = "취소"
    const val TO_FIRST = "맨 앞으로"
    const val BTN_AFTER_THIS = "이 뒤로"
    const val DRAG_A11Y = "끌어서 옮기기"

    // Several at once (GAL-18)
    const val BTN_SELECT_MANY = "여러 장 고르기"
    const val PICKED_SUFFIX = "장 고름"
    const val BTN_TAGS = "태그"
    const val PROJECT_WORD = "프로젝트"

    /** `갤러리 · 8` (section) or `<owner> · 8` (group header). */
    fun countLine(name: String, n: Int): String = "$name$SEP$n"

    /** `전체 8` · `삽화 4`: a chip with its count. */
    fun chip(label: String, n: Int): String = "$label $n"

    /** `8장 · 숨김 2` (`· 숨김 H` only while hidden items exist and are locked). */
    fun imageCount(n: Int, hiddenLocked: Int): String =
        if (hiddenLocked > 0) "$n$UNIT_IMAGES$SEP$HIDDEN_COUNT $hiddenLocked" else "$n$UNIT_IMAGES"

    /** `41 · 128`: first visible card and the count (more than 12 cards). */
    fun readout(firstVisible: Int, n: Int): String = "$firstVisible$SEP$n"

    /** `3/8`. */
    fun position(i: Int, n: Int): String = "$i/$n"

    /** `지운 씬 · 1-6 씬 1`, or `지운 씬` when no code is known. */
    fun deletedScene(code: String?): String = if (code.isNullOrBlank()) DELETED_SCENE else "$DELETED_SCENE$SEP$code"

    /** `숨긴 이미지 2 · 열림 · 앱을 떠나면 잠김`. */
    fun unlockedBar(hiddenTitle: String): String = "$hiddenTitle$SEP$UNLOCKED_OPEN$SEP$UNLOCKED_LEAVE"

    /** `리엔 · 갤러리`. */
    fun ownerGallery(owner: String): String = "$owner$SEP$TITLE"

    /** `리엔 갤러리 ›`. */
    fun ownerLink(owner: String): String = "$owner $GALLERY_LINK"

    /** `원본 1200 × 1600 · 4.2 MB`, preset `<W> × <H> · 기본 이미지`. */
    fun originalLine(w: Int, h: Int, size: String?): String =
        if (size == null) "$w × $h$SEP$PRESET" else "$ORIGINAL $w × $h$SEP$size"

    /** `넣은 날 11-20`. */
    fun addedOn(mmdd: String): String = "$ADDED_ON $mmdd"

    /** `리엔 · 여러 장 · 고른 순서대로`. */
    fun addSubtitle(owner: String): String = "$owner$SEP$ADD_MANY$SEP$ADD_IN_ORDER"

    /** `넣기 · 3`. */
    fun addButton(k: Int): String = "$BTN_ADD$SEP$k"

    /** `리엔 · 갤러리 3/8` (menu) and `리엔 · 갤러리 3/8 · 원본 그대로` (info head). */
    fun ownerPosition(owner: String, i: Int, n: Int, asIs: Boolean = false): String =
        "$owner$SEP$TITLE ${position(i, n)}" + if (asIs) "$SEP$AS_IS" else ""

    /** `3024 × 4032 · 4.2 MB · 사진`; parts that are unknown are left out. */
    fun facts(w: Int?, h: Int?, size: String?, source: String?): String =
        listOfNotNull(if (w != null && h != null) "$w × $h" else null, size, source).joinToString(SEP)

    /** `숨긴 이미지 · 초상에서는 보임`. */
    fun hiddenShownIn(shownIn: String): String = "$HIDDEN_IMAGE$SEP$shownIn"

    /** `3/8 · 직접 순서`, `3/8 · 넣은 날`, `3/8 · 전체에서만`. */
    fun orderLine(position: String, why: String): String = "$position$SEP$why"

    /** `직접 순서 · 8`. */
    fun orderSection(n: Int): String = "$SORT_MANUAL$SEP$n"

    /** `고른 이미지 · 등불 앞의 리엔 · 3/8`. */
    fun pickedBar(title: String, position: String): String = "$PICKED_IMAGE$SEP$title$SEP$position"

    /** `옮기는 중 · 등불 앞의 리엔`. */
    fun movingBar(title: String): String = "$MOVING$SEP$title"

    /** `3장 고름`. */
    fun pickedCount(k: Int): String = "$k$PICKED_SUFFIX"

    /** `지금 초상 · 사진 · 원본 하나`. */
    fun offerSub(now: String, sourceName: String): String = "$now$SEP$sourceName$SEP$ONE_ORIGINAL"
}
