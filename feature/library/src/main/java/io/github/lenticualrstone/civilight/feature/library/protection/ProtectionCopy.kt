package io.github.lenticualrstone.civilight.feature.library.protection

/**
 * Undo-step labels of protection changes (shown in undo toasts, so they are UI copy). Each is the
 * name of the control that made the change, as the boards give it (`LibMenu`, `ProjSettings`).
 */
object ProtectionCopy {
    const val HIDE = "프로젝트 숨기기"
    const val UNHIDE = "숨김 풀기"
    const val RATING = "수위 등급"
    const val HIDE_SWITCH = "이 프로젝트 숨기기"
    const val CAPTURE_BLOCK = "화면 캡처 가리기"
    const val NAME_MASK = "이름 가리기"
    const val RECORDER_LINK_MASK = "라이프 레코더 연결 가리기"

    /**
     * Toast when hiding is refused on a device without a secure lock screen (§6 Q19 `ED`: hiding
     * needs a device credential; copy not in sources).
     */
    const val NO_DEVICE_LOCK = "기기 잠금 필요"
}
