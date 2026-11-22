package io.github.lenticualrstone.civilight.feature.library.onboarding

/**
 * Copy of 첫 실행 설정, the walkthrough and 예시 둘러보기 (LIB-15 … LIB-18; boards `OnbWelcome`,
 * `OnbLook`, `OnbSafe`, `OnbVoice`, `Walk1` … `Walk3`, `OnbExample`).
 */
object OnboardingCopy {
    // OnbWelcome
    const val WORDMARK = "CIVILIGHT"
    const val WELCOME_TITLE = "문명의 빛"
    const val WELCOME_SMALL = "설정 3단계 + 예시 · 모두 나중에 바꾸기 가능"
    const val START = "시작하기"
    const val RESTORE_FROM_BACKUP = "백업에서 되살리기"

    // Shared page parts
    const val SKIP = "건너뛰기"
    const val NEXT = "다음"
    const val LATER = "나중에"
    const val PREVIOUS = "이전"
    const val SETUP_PREFIX = "처음 설정"

    // OnbLook
    const val LOOK_TITLE = "화면"
    const val THEME_SYSTEM = "기기 설정"
    const val THEME_LIGHT = "라이트"
    const val THEME_DARK = "다크"
    const val TEXT_SIZE = "글자 크기"
    const val SIZE_GLYPH = "가"

    // OnbSafe
    const val SAFE_TITLE = "백업과 잠금"
    const val DRIVE_TITLE = "Google Drive 자동 백업"
    const val DRIVE_SUB = "하루 한 번 · 와이파이 · 원본은 기기에"
    const val CONNECT = "연결"
    const val APP_LOCK_TITLE = "앱 잠금"
    const val APP_LOCK_SUB = "열 때 지문"
    const val SAFE_NOTE = "나중에 연결: 설정 › 보관"

    /**
     * `연결` cannot sign in yet: Google sign-in and Drive authorization arrive with LIB-57
     * (needsExternal). Shown as a toast; no source copy.
     */
    const val TODO_COPY_LIB15_DRIVE_NOT_READY = "TODO_COPY_LIB15_DRIVE_NOT_READY"

    // OnbVoice
    const val VOICE_TITLE = "라이프 레코더"
    const val RECORDER_CARD_TITLE = "라이프 레코더"
    const val RECORDER_CARD_SUB = "프로젝트 밖의 따로 된 기록함"
    const val RECORDER_FLOW = "말로 기록 → 글로 변환 → 원고 · 카드에 넣기"
    const val MIC_TITLE = "마이크 허용"
    const val MIC_SUB = "녹음할 때만 사용"
    const val WIDGET_TITLE = "홈 화면 위젯 추가"
    const val WIDGET_SUB = "앱을 열지 않고 바로 녹음"

    /** The launcher cannot pin widgets (or the app has none): no source copy. */
    const val TODO_COPY_LIB15_WIDGET_UNSUPPORTED = "TODO_COPY_LIB15_WIDGET_UNSUPPORTED"

    // Walkthrough
    const val WALK_1_TITLE = "밀어서 탭 고르기"
    const val WALK_1_SUB = "블록 왼쪽으로 → 시간 · 참고 · 속성"
    const val WALK_2_TITLE = "시점 렌즈"
    const val WALK_2_SUB = "고른 시점까지만 보기 · 앱 전체"
    const val WALK_3_TITLE = "떠오르면 말로"
    const val WALK_3_SUB = "명령 칸 마이크 · 라이프 레코더"
    const val SEE_EXAMPLE = "예시 둘러보기"

    /** Replay from 도움말 (LIB-21): the last page ends the replay (`ED`, the tours' `마치기`). */
    const val FINISH_REPLAY = "마치기"
    const val WALK_BANNER = "예시 · 읽기 전용 · 온보딩 후 삭제"
    const val TAB_TIME = "시간"
    const val TAB_REFERENCE = "참고"
    const val TAB_PROPERTIES = "속성"
    const val LENS_LATER = "시점 · 1-7"
    const val LENS_EARLIER = "시점 · 1-2"
    const val ROW_AGE = "나이"
    const val ROW_FACTION = "소속"
    const val ROW_WITH_HART = "하르트와"
    const val LISTENING = "듣는 중"

    // OnbExample
    const val EXAMPLE_BANNER = "예시 프로젝트 · 읽기 전용"
    const val EXAMPLE_BANNER_SUB = "온보딩 후 삭제"
    const val EXPLORE = "둘러볼 곳"
    const val EXPLORE_SCRIPT = "스크립트"
    const val EXPLORE_SCRIPT_SUB = "대사 · 나레이션 · 연출 지시 · 변화 표식"
    const val EXPLORE_CAST = "인물"
    const val EXPLORE_CAST_SUB = "시점에 따라 바뀌는 필드와 관계"
    const val EXPLORE_WORLD = "세계"
    const val EXPLORE_WORLD_SUB = "용어 공개 시점과 점검"
    const val EPISODE_DONE = "완료"
    const val EPISODE_UNDECIDED = "미정"
    const val UNTITLED = "—"
    const val FINISH_ONBOARDING = "온보딩 끝내기"

    /** `처음 설정 · 1/4`. */
    fun setupLine(index: Int, of: Int = 4): String = "$SETUP_PREFIX · $index/$of"

    /** `스크립트 · 1-7 씬 3`. */
    fun exploreLine(area: String, what: String): String = "$area · $what"
}

/**
 * Story content shown on onboarding pages (the example manuscript and the demo cards), quoted from
 * the boards as written. It is content, not interface copy, so the copy lint (no 해요체, terse
 * fragments) does not apply and this object is not in `LibraryCopyLintTest` (DS-62 `content_` rule).
 */
object OnboardingSample {
    const val WELCOME_LINE_1 = "세계를 만들고, 인물을 세우고,"
    const val WELCOME_LINE_2 = "대사를 쓰는 곳."
    const val PREVIEW_LINE = "“멈추지 마. 저건 등불이 아니야.”"
    const val HART_UNKNOWN = "???"
    const val HART = "하르트"
    const val HART_LINE = "멈추지 마. 저건 등불이 아니야."
    const val RIEN = "리엔"
    const val AGE = "17세"
    const val FACTION_LATER = "등불 결사 ◆"
    const val FACTION_EARLIER = "없음"
    const val HART_LATER = "경계"
    const val HART_EARLIER = "만나기 전"
    const val SPOKEN = "2장 첫 장면은 비 오는 상층 관문에서"

    /** The blurred manuscript behind `OnbWelcome` (speaker, line); null speaker = narration. */
    val MANUSCRIPT: List<Pair<String?, String>> = listOf(
        "세라" to "불은 꺼졌어. 기록도 거기서 끝나.",
        "오웬" to "기록 따위로 배가 부르진 않지.",
        null to "▸ 배경  하층 거리(새벽)   ♪ 잔불",
        "리엔" to "(작게) 저기… 불빛이 보여요. 누가 아직 남아 있는 걸까요?",
        "??? 하르트" to "멈추지 마. 저건 등불이 아니야.",
        null to "바람이 잿더미를 쓸고 지나갔다. 꺼진 줄 알았던 오리지늄 등불이 한 번 깜박였다.",
        "리엔" to "(숨을 삼키며) 하르트 씨, 방금…",
        null to "⌁ 작가 메모  여기서 등불이 두 번째로 깜박이게 할지",
        "하르트" to "뒤돌아보지 말고 따라와.",
        "리엔" to "하지만 저 불빛은 결사의 신호였어요.",
    )
}
