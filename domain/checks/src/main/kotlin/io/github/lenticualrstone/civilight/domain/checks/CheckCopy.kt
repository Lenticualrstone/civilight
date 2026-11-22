package io.github.lenticualrstone.civilight.domain.checks

/** UI copy of 점검 센터 (STO-39 … STO-41, CNV-17), verbatim from the specs. */
object CheckCopy {
    const val SEP = " · "

    const val SCREEN_TITLE = "점검 센터"
    const val INTRO = "기록과 원고가 어긋난 곳"
    const val SCANNING = "점검 중"

    // Kind titles (marker + kind) of the STO-40 registry.
    const val TERM_BEFORE_REVEAL = "▲ 공개 전 사용"
    const val TERM_CANDIDATE = "◇ 공개 전 후보"
    const val TERM_NO_REVEAL = "◇ 첫 공개 없음"
    const val SPEAKER_MISSING = "▲ 화자 없음"
    const val VOICE_EMPTY = "▲ 보이스 비어 있음"
    const val VOICE_FORMAT = "◇ 형식과 다른 ID"
    const val VOICE_FORMAT_DETAIL = "보이스 ID가 화자 · 에피소드와 다름"
    const val ANCHOR_DELETED = "◇ 근거 지워진 변화"
    const val NO_SCALE = "◇ 축척 없음 · 점검 못 함"
    const val CLOCK_BAND = "◇ 시각과 시간대가 다름"
    const val ORDER = "◇ 순서와 때가 다름"
    const val HIDDEN_CHARACTER = "◇ 숨긴 인물이 보이는 씬에 나옴"

    // Buttons.
    const val GO_TO_SCRIPT = "원고로 가기"
    const val MOVE_REVEAL = "공개 시점 옮기기"
    const val MATCH = "맞음"
    const val AS_IS = "그대로"
    const val KEEP = "이대로 두기"
    const val LINK = "잇기"
    const val REJECT = "아님"
    const val PICK_SPEAKER = "화자 고르기"
    const val TO_NARRATION = "나레이션으로 바꾸기"
    const val WRITE_VOICE = "보이스 쓰기"
    const val NEW_VOICE_ID = "새 ID로"
    const val RELINK_RECORD = "다른 블록에 잇기"
    const val TRASH_RECORD = "기록 지우기"
    const val TRASH_ALL_RECORDS = "모두 지우기"
    const val RESTORE = "되살리기"
    const val OPEN_BULK = "한꺼번에 ›"
    const val FIX_SCALE = "축척 맞추기"
    const val TO_AUTO = "자동으로"
    const val EDIT_TRANSITION = "문구 고치기"
    const val WRITE_DIRECT = "직접 쓰기"
    const val HIDE_SCENE = "씬도 숨기기"
    const val UNLOCK = "지문으로 보기"
    const val REVERT = "되돌리기"

    /** `근거 #043 지워짐`; `근거 지워짐` when the original block is unknown. */
    fun anchorGone(blockNumber: String?): String = if (blockNumber == null) "근거 지워짐" else "근거 $blockNumber 지워짐"

    /** `용어 · 오리지늄 등불` (location of a term-level row, `ED`). */
    fun termLocation(name: String): String = "용어$SEP$name"

    /** `쓰인 곳 3`. */
    fun usedIn(n: Int): String = "쓰인 곳 $n"

    /** `▲ 숨긴 항목 관련 2 · 지문으로 보기`. */
    fun hiddenGroup(n: Int): String = "▲ 숨긴 항목 관련 $n$SEP$UNLOCK"

    fun join(vararg parts: String?): String = parts.filterNot { it.isNullOrEmpty() }.joinToString(SEP)
}
