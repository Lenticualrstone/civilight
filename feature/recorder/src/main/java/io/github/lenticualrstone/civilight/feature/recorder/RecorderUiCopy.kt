package io.github.lenticualrstone.civilight.feature.recorder

/**
 * UI copy of this feature that `domain:recorder` `RecorderCopy` does not hold (that object keeps
 * the rule-23 copy and the `TODO_COPY_LRC_<n>` ids; screens use both). Verbatim from the spec and
 * the boards (`ErrMic`, `OnbVoice`, `LrRecord`, `LrText`). Missing copy stays a `TODO_COPY_` id.
 */
object RecorderUiCopy {
    // 마이크 꺼짐 (LRC-06, `ErrMic`).
    const val MIC_OFF_TITLE = "마이크 꺼짐"
    const val MIC_OFF_SUB = "시스템 설정에서 허용 필요 · 녹음할 때만 사용"
    const val OPEN_SYSTEM_SETTINGS = "시스템 설정 열기"

    // Reason line before the system prompt (LRC-06, the `OnbVoice` choice reused).
    const val MIC_ALLOW = "마이크 허용"
    const val MIC_ALLOW_SUB = "녹음할 때만 사용"

    // Kind words of the link picker's sub line (LRC-18 link units).
    const val KIND_PROJECT = "프로젝트"
    const val KIND_CHARACTER = "인물"
    const val KIND_PLACE = "장소"
    const val KIND_TERM = "용어"
    const val KIND_FACTION = "세력"
    const val KIND_PAGE = "페이지"

    // 홈 화면 위젯 (LRC-02, board `LrEntry`).
    const val WIDGET_TITLE = "떠오른 생각"
    const val WIDGET_SUB = "눌러서 바로 녹음"

    // 기록함 (LRC-25 … LRC-27).
    const val BACK = "뒤로"

    /** Label of the header icon that opens the text search over the records (§6 Q11 `ED`); no source names it. */
    const val TODO_SEARCH_ICON = "TODO_COPY_LRC_SEARCH_ICON"

    /** Placeholder of that search field (no source gives one). */
    const val TODO_SEARCH_PLACEHOLDER = "TODO_COPY_LRC_SEARCH_FIELD"

    // 넣기 sheets (LRC-32, LRC-33).
    /** Title of the scene list behind `씬 고르기 ›` (the row's words without the chevron). */
    const val PICK_SCENE_TITLE = "씬 고르기"

    /** Title of the biometric prompt when an R-18 project is chosen as a 넣기 destination (no source gives it). */
    const val TODO_R18_PROMPT = "TODO_COPY_LRC_R18_PROMPT"

    /** Placeholder of the card search in `카드에 넣기` (LRC-35; the sheet is not drawn). */
    const val TODO_CARD_SEARCH = "TODO_COPY_LRC_CARD_SEARCH"

    /** Confirm button of `인물로 만들기` (LRC-38; the sheet is not drawn and no source names the button). */
    const val TODO_CHARACTER_BUTTON = "TODO_COPY_LRC_CHARACTER_BUTTON"

    // 원본 음성 (LRC-14): labels of the round play button (the board draws only the glyph).
    const val TODO_PLAY = "TODO_COPY_LRC_PLAY"
    const val TODO_PAUSE = "TODO_COPY_LRC_PAUSE"

    /** 빠른 설정 타일 label: TODO_COPY_LRC_01, `ED` the app-wide name until decided (LRC-03). */
    const val TILE_LABEL = "라이프 레코더"
}
