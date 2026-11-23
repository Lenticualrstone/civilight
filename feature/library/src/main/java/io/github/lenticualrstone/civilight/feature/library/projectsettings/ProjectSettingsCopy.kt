package io.github.lenticualrstone.civilight.feature.library.projectsettings

/** Copy of 프로젝트 설정 (LIB-69 container, LIB-25 section; board `ProjSettings`, CNV-20 row map). */
object ProjectSettingsCopy {
    const val TITLE = "프로젝트 설정"

    /** U-08 LIB-82: first section `표지` with one row (`[PROPOSED]`, 60 §7.2). */
    const val SECTION_COVER = "표지"
    const val COVER = "표지 이미지"
    const val COVER_SUB = "서재 카드 · 목록 썸네일 · 프로젝트 홈"
    const val SECTION_STRUCTURE = "구조"
    const val SECTION_CAST = "인물"
    /** (0.2.0, LIB-92) Was `등급 · 숨기기`. */
    const val SECTION_PROTECTION = "숨기기"
    const val SECTION_SCRIPT = "스크립트 추가 칸"

    /** (0.2.0, WLD-73) The world group of 프로젝트 설정, holding `용어 후보`. */
    const val SECTION_WORLD = "세계"
    const val TERM_CANDIDATES = "용어 후보"

    const val CHANGE = "바꾸기"

    // 구조
    const val LEVELS = "단계 이름"
    const val WORLD_TIME = "세계 시간"
    const val TIME_YEAR = "연도만"
    const val TIME_CALENDAR = "세계 달력"
    const val TIME_NONE = "시간 없이"
    const val START = "시작"
    const val NOTATION = "때 표기"
    const val NOTATION_EPISODE = "에피소드 기준"
    const val NOTATION_STORY_START = "이야기 시작부터"
    const val NOTATION_NOTE = "연표는 기준 사건도"
    const val TRAVEL = "이동 수단"
    const val DAY_BANDS = "하루 · 시간대"
    const val DAWN_DAY = "하루는 새벽 4시부터"
    const val MIDNIGHT = "자정"

    // 인물
    const val RELATION_KINDS = "관계 종류"

    /** CST-12 `TagSettings` title (cast lane); the row is not on the `ProjSettings` board. */
    const val TAGS = "태그"

    // 스크립트 추가 칸
    /** (0.2.0, LIB-91) First row of `스크립트 추가 칸`. */
    const val GAME_TOOLS = "게임 제작 도구"

    /**
     * LIB-91 row sub: copy id `TODO_COPY_LIB_GAME_TOOLS_SUB` (to confirm), interim from the words of the user's
     * decision row `게임 제작 기능` (spec 60 §8.9).
     */
    const val GAME_TOOLS_SUB = "보이스 ID · 에셋 · 연출 큐 · 녹음 대본 CSV · 엔진 JSON"

    const val STAGE = "무대 위치"
    const val STAGE_SUB = "왼쪽 · 가운데 · 오른쪽"
    const val VOICE_ID = "보이스 ID"
    const val VOICE_ID_SUB = "대사마다 녹음용 ID"
    const val VOICE_FORMAT = "ID 형식"
    const val VOICE_FORMAT_PARTS = "인물 · 에피소드 · 블록 · 내보내면 고정"
    const val TRANSITION = "전환 문구"
    const val TRANSITION_DEFAULT = "새 씬 기본"
    const val TRANSITION_AUTO = "자동"
    const val TRANSITION_DIRECT = "직접"
    const val TRANSITION_NONE = "없음"
    const val TEMPLATE = "틀"
    const val TEMPLATE_PLACE_TIME = "장소 · 시각"
    const val TEMPLATE_PLACE_ONLY = "장소만"
}
