package io.github.lenticualrstone.civilight.feature.story.home

/** Copy of 프로젝트 홈 (STO-01, board `Home`). */
object HomeCopy {
    /** The head names the project only (QA U-06); the back button is icon only (DS-70 A13, `서재로`). */

    /** Accessibility name of the head's back button (DS-70 A13: back leads to the 서재). */
    const val BACK_TO_LIBRARY = "서재로"

    const val STAT_TODAY = "오늘 쓴 글자"
    const val STAT_CAST = "인물"

    const val RESUME = "이어 쓰기"
    const val WORK = "작업"

    const val SHORTCUT_STORY = "스토리"
    const val SHORTCUT_MARKS = "할 일 · 복선"
    const val SHORTCUT_CAST = "인물"
    const val SHORTCUT_WORLD = "세계"
    const val SHORTCUT_TIMELINE = "연표"
    const val SHORTCUT_CANVAS = "캔버스"
    const val SHORTCUT_GALLERY = "갤러리"
    const val SHORTCUT_PAGES = "페이지"
    const val SHORTCUT_CHECKS = "점검 센터"
    const val SHORTCUT_SETTINGS = "설정"

    /** 만든 직후 (board `NewReady`, LIB-13 block): `바로 쓰기` + `쓰기`, `목록 닫기`; the head keeps the name (QA U-06). */
    const val QUICK_WRITE = "바로 쓰기"
    const val WRITE = "쓰기"
    const val DISMISS = "목록 닫기"

    /** The head cover's accessibility name (U-08 LIB-83, `[PROPOSED]`): a cover shown, or the initial-letter card. */
    const val COVER_CHANGE = "표지 이미지 · 바꾸기"
    const val COVER_PICK = "표지 이미지 · 고르기"

    /** The immersive head's meta line: `연재 장편 · 에피소드 10개 · 최신 1-7 씬 3`. */
    const val META_SEPARATOR = " · "
    const val META_LATEST = "최신"
    fun metaCount(levelName: String, n: Int): String = "$levelName ${n}개"
    fun metaLatest(sceneRef: String): String = "$META_LATEST $sceneRef"

    /** Untitled episode in compact rows (STO-03). */
    const val NO_TITLE = "—"

    /** (0.2.0, N-41) The `할 일 · 복선` sheet: title, the three filters, the row kinds, `거둘 곳 · 2-5`. */
    const val MARKS_TITLE = "할 일 · 복선"
    const val MARK_OPEN = "열림"
    const val MARK_PAID_OFF = "거둠"
    const val MARK_DONE = "끝냄"
    const val MARK_PLANT = "복선"
    const val MARK_TODO = "할 일"
    const val MARK_PAYOFF_AT = "거둘 곳"
}
