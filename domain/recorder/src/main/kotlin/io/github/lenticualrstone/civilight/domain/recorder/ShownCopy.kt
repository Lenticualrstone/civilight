package io.github.lenticualrstone.civilight.domain.recorder

/**
 * What the screens show for the `TODO_COPY_LRC_<n>` ids of [RecorderCopy] (spec §6 Q21: no source gives
 * the copy). The ids stay what the domain returns, so the rules and their tests keep pinning them; every
 * UI exit (view text, toast, accessibility label, notification) maps them through [of] and never lets a
 * raw id reach the screen. Each value is the board or spec copy where there is one, otherwise built from
 * existing copy (`… 지움`, `… 넣음` patterns, the library's re-lock notes). All picks are `ED`: to confirm.
 */
object ShownCopy {
    /** 녹음 중 (notification title line, `TODO_COPY_LRC_04`). */
    const val RECORDING = "녹음 중"

    /** Resume label of the pause button while paused (`TODO_COPY_LRC_02`): the mic icon's words. */
    const val RESUME = RecorderCopy.CONTINUE_SPEAKING

    const val CANCELLED = "녹음 취소"
    const val NO_ENGINE = "받아쓰기 쓸 수 없음 · 음성만 저장"

    /** The second half of [RecorderCopy.AUDIO_KEPT_NOTE] (audio keeping off or the microphone not opened). */
    const val AUDIO_NOT_KEPT = "글은 나중에 고치기 가능"
    const val PICKER_TITLE = "연결 고르기"
    const val GONE_UNIT = "지워진 항목"
    const val MENU = "메뉴"
    const val DONE_TOAST = "끝냄"
    const val FILTERED_EMPTY = "결과 없음"
    const val CARD_TOAST = "카드에 넣음"
    const val PAGE_BUTTON = RecorderCopy.MAKE_PAGE
    const val PAGE_TOAST = "페이지로 만듦"
    const val MEMO_TOAST = "메모 칸에 넣음"
    const val CHARACTER_TOAST = "인물로 만듦"
    const val DELETED_TOAST = "기록 지움"
    const val EXPORT_HIDDEN_INCLUDED = "숨긴 기록 포함"

    /** The open group's right text for the re-lock policies other than `앱을 떠나면 잠김` (the library's pattern). */
    const val RELOCK_SCREEN_OFF = "화면이 꺼지면 잠김"
    const val RELOCK_FIVE_MINUTES = "5분 뒤 잠김"

    /** `TODO_COPY_LRC_18` has no policy of its own: [Inbox] picks it for any policy but `앱을 떠날 때`. */
    const val RELOCK_OTHER = RELOCK_SCREEN_OFF

    private val byId: Map<String, String> = mapOf(
        RecorderCopy.TODO_QS_TILE to RecorderCopy.APP_NAME,
        RecorderCopy.TODO_RESUME to RESUME,
        RecorderCopy.TODO_CANCELLED to CANCELLED,
        RecorderCopy.TODO_NOTIFICATION to RECORDING,
        RecorderCopy.TODO_NO_ENGINE to NO_ENGINE,
        RecorderCopy.TODO_AUDIO_NOT_KEPT to AUDIO_NOT_KEPT,
        RecorderCopy.TODO_PICKER_TITLE to PICKER_TITLE,
        RecorderCopy.TODO_GONE_UNIT to GONE_UNIT,
        RecorderCopy.TODO_MENU_ARIA to MENU,
        // 10 is the date format (`yyyy년 M월 d일`, see RecorderTimeLabels), never drawn as a string.
        RecorderCopy.TODO_OTHER_YEAR to "yyyy년 M월 d일",
        RecorderCopy.TODO_DONE_TOAST to DONE_TOAST,
        RecorderCopy.TODO_FILTERED_EMPTY to FILTERED_EMPTY,
        RecorderCopy.TODO_CARD_TOAST to CARD_TOAST,
        RecorderCopy.TODO_PAGE_BUTTON to PAGE_BUTTON,
        RecorderCopy.TODO_PAGE_TOAST to PAGE_TOAST,
        RecorderCopy.TODO_MEMO_TOAST to MEMO_TOAST,
        RecorderCopy.TODO_CHARACTER_TOAST to CHARACTER_TOAST,
        RecorderCopy.TODO_RELOCK_NOTE to RELOCK_OTHER,
        RecorderCopy.TODO_DELETED_TOAST to DELETED_TOAST,
        RecorderCopy.TODO_EXPORT_HIDDEN_INCLUDED to EXPORT_HIDDEN_INCLUDED,
    )

    /** Every id this object resolves (the copy test walks them against [RecorderCopy]). */
    val ids: Set<String> get() = byId.keys

    /** [text] itself, or its copy when [text] is a `TODO_COPY_LRC_<n>` id. */
    fun of(text: String): String = byId[text] ?: text

    /** [text] of [toast] as shown. */
    fun of(toast: Toast): String = of(toast.text)

    /** The open group's right text for a re-lock policy name (`ON_LEAVE`, `SCREEN_OFF_ONLY`, `FIVE_MINUTES`). */
    fun relockNote(policy: String): String = when (policy) {
        "SCREEN_OFF_ONLY" -> RELOCK_SCREEN_OFF
        "FIVE_MINUTES" -> RELOCK_FIVE_MINUTES
        else -> RecorderCopy.LOCKS_ON_LEAVE
    }
}
