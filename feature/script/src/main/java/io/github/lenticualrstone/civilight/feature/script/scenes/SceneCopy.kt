package io.github.lenticualrstone.civilight.feature.script.scenes

/**
 * Copy of the split and merge sheets (SCR-50, SCR-51; boards `RSSplit`, `RSMerge`) and of the
 * `/씬` routing (SCR-49) that the domain previews do not produce. DECISIONS §3.
 */
object SceneCopy {
    const val SPLIT_TITLE = "씬 나누기"
    const val MERGE_TITLE = "씬 합치기"
    const val FIELD_PLACE = "장소"
    const val FIELD_TIME = "때"
    const val FIELD_TITLE = "제목"
    const val CHANGE = "바꾸기 ›"
    const val WRITE = "쓰기 ›"
    const val CLEAR = "비우기"
    const val UNTITLED = "미정"
    const val NO_PLACE = "장소 없음"
    const val NO_TIME = "때 없음"
    const val SAME_TIME = "같은 때"
    const val FLASHBACK = "회상"
    const val HIDDEN = "숨김"
    const val CHANGES = "바뀌는 것"
    const val SPLIT = "나누기"
    const val MERGE = "합치기"
    const val KEEP_AS_SYSTEM = "시스템 텍스트로 남기기"
    const val DISCARD = "지우기"
    const val PICK_PLACE = "장소 고르기"
    const val READ_ONLY = "읽기 전용"

    /** The excerpt's speaker while the speaker is a locked hidden character (rule 19). */
    const val HIDDEN_SPEAKER = "숨긴 인물"

    /** A record subject that names a locked hidden item (rule 19). */
    const val HIDDEN_ITEM = "숨긴 항목"

    /** The new scene's part card: `새 씬 4`. */
    fun newScene(ordinal: Int): String = "새 씬 $ordinal"

    /** Every value above, for the lint test. */
    val all: List<String> = listOf(
        SPLIT_TITLE, MERGE_TITLE, FIELD_PLACE, FIELD_TIME, FIELD_TITLE, CHANGE, WRITE, CLEAR, UNTITLED, NO_PLACE, NO_TIME,
        SAME_TIME, FLASHBACK, HIDDEN, CHANGES, SPLIT, MERGE, KEEP_AS_SYSTEM, DISCARD, PICK_PLACE, READ_ONLY, HIDDEN_SPEAKER,
        HIDDEN_ITEM, newScene(4),
    )

    /** Buttons (linted with the button rules). */
    val buttons: List<String> = listOf(CHANGE, WRITE, CLEAR, SPLIT, MERGE, KEEP_AS_SYSTEM, DISCARD)
}
