package io.github.lenticualrstone.civilight.feature.library.settings

/**
 * Copy of 설정 (LIB-63, board `Settings`), 설정 · 숨기기 (LIB-64, board `HideSettings`) and
 * 화면 · 글자 크기 (LIB-65, boards `Settings`, `OnbLook`).
 */
object SettingsCopy {
    const val SMALL = "Civilight"
    const val TITLE = "설정"

    // 화면
    const val SECTION_SCREEN = "화면"
    const val THEME_SYSTEM = "기기 설정"
    const val THEME_LIGHT = "라이트"
    const val THEME_DARK = "다크"
    const val TEXT_SIZE = "글자 크기"
    const val TEXT_SIZE_DEFAULT = "보통"
    const val LINE_HEIGHT = "행간 1.7"
    const val BODY = "본문"
    const val LAYOUT = "펼친 화면 배치"
    const val LAYOUT_SUB = "서재 · 인물 · 세계 · 연표 …"
    const val LAYOUT_LIST_DETAIL = "목록 + 상세"
    const val LAYOUT_CENTER = "가운데 한 칸"
    const val MOTION = "움직임 · 투명도"
    const val MOTION_SUB = "애니메이션 줄이기 · 투명도 줄이기"
    const val MOTION_FOLLOW = "기기 설정"
    const val MOTION_ALWAYS = "항상 줄이기"
    const val MOTION_NEVER = "줄이지 않음"
    const val MORE = "지도 · 이미지 · 연표"
    const val MORE_SUB = "색면 · 홀로 · 생성 방식 · 이동 거리"

    // 보관
    const val SECTION_KEEP = "보관"
    const val DRIVE = "Google Drive 자동 백업"
    const val LAST_BACKUP = "마지막 백업"
    const val RECOVERY = "복구 암호"
    const val RECOVERY_PURPOSE = "숨긴 프로젝트 백업용"
    const val RECOVERY_SET = "정해 둠"
    const val RECOVERY_CHANGE = "바꾸기"
    const val RECOVERY_DEFINE = "정하기"

    // 보안
    const val SECTION_SECURITY = "보안"
    const val APP_LOCK = "앱 잠금"
    const val APP_LOCK_SUB = "지문으로 열기"
    const val HIDING = "숨기기"
    const val HIDDEN_PROJECTS = "숨긴 프로젝트"
    const val RELOCK_WHEN = "다시 잠그는 때"

    // Rows the board does not draw (§6 Q27 `ED`): those screens' own titles.
    const val TRASH = "휴지통"
    const val RECORDER = "라이프 레코더"
    const val HELP = "도움말 · 안내 다시 보기"

    // 설정 · 숨기기 (LIB-64)
    const val HIDING_INTRO = "프로젝트 단위 기본 · 앱 잠금과 별도"
    const val RELOCK_ON_LEAVE = "앱을 떠날 때"
    const val RELOCK_ON_LEAVE_SUB = "다른 앱 · 화면 꺼짐"
    const val RELOCK_SCREEN_OFF = "화면이 꺼질 때만"
    const val RELOCK_SCREEN_OFF_SUB = "다른 앱 다녀와도 유지"
    const val RELOCK_FIVE_MINUTES = "연 지 5분 뒤"
    const val SECTION_MASKED = "가리는 곳"
    const val MASK_SEARCH = "검색 · 명령 결과"
    const val MASK_SEARCH_SUB = "열려 있을 때만 결과에 포함"
    const val MASK_WIDGETS = "위젯 · 최근 항목 · 알림"
    const val MASK_WIDGETS_SUB = "위젯 · 최근 항목 · 검색에서 빠짐 · 알림은 ‘숨긴 프로젝트’"
    const val SECTION_AUTH = "인증"
    const val AUTH_BIOMETRIC = "지문"
    const val AUTH_BIOMETRIC_SUB = "실패하면 기기 PIN"
    const val AUTH_IN_USE = "사용 중"
    const val BACKUP_HIDDEN = "숨긴 것도 백업"
    const val BACKUP_HIDDEN_SUB = "Google Drive · 복구 암호로 잠금"
    const val RATING_RULES = "수위 등급별 동작"
    const val RATING_RULES_SUB = "R-18 · 숨기기 · 캡처 가림 · 이름 가림"

    // 화면 · 글자 크기 (LIB-65)
    const val TEXT_SIZE_SMALL = "설정 › 화면"
    const val SIZE_GLYPH = "가"

    /** The live preview line under the slider (board `OnbLook`). */
    const val PREVIEW_LINE = "“멈추지 마. 저건 등불이 아니야.”"

    /** Toast when 앱 잠금 is turned on without a device lock (§6 Q19; copy not in sources). */
    const val TODO_COPY_LIB35_NO_DEVICE_LOCK = "TODO_COPY_LIB35_NO_DEVICE_LOCK"
}
