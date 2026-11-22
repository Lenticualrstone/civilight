package io.github.lenticualrstone.civilight.feature.library.newproject

/** Copy of 새 프로젝트 1/4 … 4/4 (LIB-09 … LIB-12, LIB-14; boards `NewName`, `NewNameR18`, `NewStructure`, `NewTime`, `NewStart`, `NewWide`). */
object NewProjectCopy {
    const val SMALL = "새 프로젝트"

    const val TITLE_NAME = "이름과 형식"
    const val TITLE_STRUCTURE = "구조"
    const val TITLE_TIME = "시간"
    const val TITLE_START = "시작 재료"

    const val CANCEL = "취소"
    const val NEXT = "다음"
    const val PREVIOUS = "이전"
    const val CREATE = "만들기"

    // 1/4
    const val NAME = "이름"
    const val ONE_LINER = "한 줄 소개 (선택)"
    const val ONE_LINER_PLACEHOLDER = "—"
    const val FORMAT = "형식"
    const val FORMAT_SERIAL = "연재 장편"
    const val FORMAT_SERIAL_SUB = "장 · 에피소드 · 씬으로 길게"
    const val FORMAT_SHORT = "단편"
    const val FORMAT_SHORT_SUB = "씬만으로 짧게"
    const val FORMAT_SETTING = "설정만"
    const val FORMAT_SETTING_SUB = "원고 없이 세계와 인물만"
    const val RATING = "수위 등급"
    const val RATING_NOTE = "나중에 바꾸기 가능"

    // 2/4
    const val STRUCTURE_DESC = "단계 이름은 나중에 바꾸기 가능"
    const val STRUCTURE_HOW = "나누는 방법"
    const val PRESET_CHAPTER = "장 › 에피소드 › 씬"
    const val PRESET_CHAPTER_SUB = "서브컬처 게임 메인 스토리처럼"
    const val PRESET_ACT = "막 › 장면"
    const val PRESET_ACT_SUB = "연극 · 짧은 이벤트"
    const val PRESET_CUSTOM = "직접 정하기"
    const val PRESET_CUSTOM_SUB = "단계 수와 이름을 직접 (최대 4단계)"
    const val PRESET_CUSTOM_SUB_WIDE = "최대 4단계"
    const val PREVIEW = "미리 보기"
    const val PREVIEW_LIVE = "실시간 미리 보기"

    /** The `직접 정하기` editor's add and remove buttons (LIB-10: layout `ED`, copy not in sources). */
    /** While `만들기` runs (DS-47 `DelayedProgress`: shown only after a second). */
    const val WORKING = "만드는 중"

    const val ADD_LEVEL = "단계 추가"
    const val REMOVE_LEVEL = "단계 빼기"

    // 3/4
    const val TIME_YEAR = "연도만"
    const val TIME_YEAR_SUB = "예: 1200년"
    const val TIME_CALENDAR = "세계 달력 만들기"
    const val TIME_CALENDAR_SUB = "달 이름 · 한 달의 날 수 · 요일을 직접"
    const val TIME_NONE = "시간 없이"
    const val TIME_NONE_SUB = "연표 끔"
    const val START_YEAR = "이야기가 시작되는 해"
    const val START_YEAR_PLACEHOLDER = "1200"
    const val START_YEAR_HINT = "생일이 있으면 나이 자동 계산"

    // 4/4
    const val PROTAGONIST = "주인공 (선택)"
    const val FIRST_SCENE = "첫 씬 제목 (선택)"
    const val FIRST_SCENE_PLACEHOLDER = "비워 두면 ‘씬 1’"
    const val HIDE = "이 프로젝트 숨기기"
    const val HIDE_SUB = "지문 · PIN으로 열림"
    const val CLOSE = "닫기"
}
