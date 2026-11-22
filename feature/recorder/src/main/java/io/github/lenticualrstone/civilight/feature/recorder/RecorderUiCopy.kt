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
}
