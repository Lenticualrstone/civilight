package io.github.lenticualrstone.civilight.feature.canvas.holo

/**
 * Copy of the holo experiment screens (CNV-31, boards `Holo`, `HoloKit`, `HoloMoment`). `SAMPLE_` values
 * are the boards' sample content (the experiment shows the example project's words, Q-IM-11).
 */
object HoloCopy {
    const val BACK = "뒤로"
    const val SMALL = "실험"
    const val TITLE = "홀로 실험"

    /** Tabs: the three board titles. */
    val TABS: List<String> = listOf("은빛 홀로 카드", "홀로 조각", "1장 완결")

    // Board `Holo`.
    const val CARD_LINE = "실험 · 은빛 홀로그램 카드"
    const val CARD_HINT = "기울이면 은빛이 흐름"
    const val INPUT_GYRO = "입력 · 자이로"
    const val INPUT_DRAG = "입력 · 끌기"
    const val INPUT_STILL = "애니메이션 줄이기 · 정지"
    const val INPUT_RULE = "기기 자이로 → 없으면 끌어서 기울이기 → 손을 떼면 천천히 흔들림"
    const val SAMPLE_RECORD_NO = "인물 기록 007"
    const val SAMPLE_NAME = "리엔"
    const val SAMPLE_SUB = "등불 결사 · 막내 등불지기"

    // Board `HoloKit`.
    const val KIT_LINE = "실험 · 홀로 조각"
    const val SAMPLE_PROJECT = "잿빛 새벽의 등불"
    const val KIT_SUB = "은빛 바탕 · 옅은 무지갯빛 · 같은 기울기 값"
    const val KIT_TITLE = "제목"
    const val KIT_MARKS = "표식"
    const val KIT_BADGE = "배지"
    const val KIT_CHIP = "칩"
    const val KIT_BUTTON = "주 버튼"
    const val KIT_PROGRESS = "진행"
    const val KIT_TOGGLE = "토글"
    const val KIT_BORDER = "카드 테두리"
    const val KIT_NOTE = "은빛 모든 단계 대비 4.5:1 이상"
    const val SAMPLE_DONE = "1장 완결"
    const val SAMPLE_MARKS = "◆ ◇"
    const val SAMPLE_BADGE = "LEGEND"
    const val SAMPLE_CHIP = "주연 · 리엔"
    const val SAMPLE_BUTTON = "쓰기 시작"
    const val SAMPLE_PROGRESS = "1장 7/10"
    const val SAMPLE_BORDER_TITLE = "하르트 · 정체 공개"
    const val SAMPLE_BORDER_SUB = "1-9 · 표시 이름 ??? → 하르트"

    // Board `HoloMoment`.
    const val SAMPLE_MOMENT_SUB = "등불지기 · 1-1 ~ 1-10"
    val SAMPLE_STATS: List<Pair<String, String>> = listOf("48,210" to "글자", "31" to "씬", "12" to "인물")
    const val MOST_SPOKEN = "가장 많이 말한 인물"
    const val SAMPLE_MOST_SPOKEN = "리엔 · 214줄"
    const val BTN_SAVE_CARD = "카드로 저장"
    const val BTN_NEXT_CHAPTER = "2장 시작"
}
