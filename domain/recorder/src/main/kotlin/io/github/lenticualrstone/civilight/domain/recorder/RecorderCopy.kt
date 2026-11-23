package io.github.lenticualrstone.civilight.domain.recorder

/**
 * UI copy of the Life Recorder (70-life-recorder.md), verbatim. Strings no source gives are not
 * invented: they are `TODO_COPY_LRC_<n>` ids that the UI maps to a string resource (§0 Markers).
 */
object RecorderCopy {
    const val APP_NAME = "라이프 레코더"
    const val LIBRARY_LINE = "라이브러리 · 프로젝트 밖"
    const val SEP = " · "

    // Capture screens.
    const val LISTENING = "듣는 중"
    const val FINALISING = "받아쓰는 중 · 문장 나누기"
    const val CANCEL = "취소"
    const val TO_TEXT = "글로 쓰기"
    const val STOP_AND_SAVE = "멈추고 저장"
    const val PAUSE = "일시정지"
    const val CONTINUE_SPEAKING = "이어서 말하기"
    const val KEEP_IN_INBOX = "기록함에 두기"
    const val PUT_HERE = "여기 넣기"
    const val AUDIO_KEPT_NOTE = "원본 음성 함께 보관 · 글은 나중에 고치기 가능"
    const val LINK_PREFIX = "연결 · "
    const val ADD_LINK = "+ 연결"
    const val AT_MARK = "@"

    // Text memo.
    const val TEXT_SHEET_TITLE = "라이프 레코더 · 글"
    const val CLOSE = "닫기"
    const val SAVE = "저장"
    const val AT_HINT = "@로 인물 · 용어 · 씬 연결"
    const val TO_VOICE = "말로 기록으로 바꾸기"

    // Hide row (LRC-42).
    const val HIDE = "숨기기"
    const val HIDE_SUB = "지문 · PIN으로 열림"

    // Kinds and states.
    const val VOICE = "음성"
    const val TEXT = "글"
    const val UNSORTED = "정리 안 됨"
    const val PUT = "넣음"
    const val DONE = "끝냄"

    // Inbox.
    const val ALL = "전체"
    const val PROJECT_FILTER = "프로젝트 ▾"
    const val ALL_PROJECTS = "모든 프로젝트"
    const val NO_LINK = "연결 없음"
    const val DROPDOWN = " ▾"
    const val CLEAR = " ×"
    const val RECORD_BY_VOICE = "말로 기록"
    const val BY_TEXT_SHORT = "글로"
    const val EMPTY_TITLE = "기록 없음"
    const val EMPTY_SUB = "명령 칸 마이크 · 홈 위젯 · 빠른 설정"

    /**
     * (0.2.0) LRC-50 · S-4: the label of a link into a project whose `라이프 레코더 연결 가리기` is on, in place of
     * the rating badge. `TODO_COPY_LIB_MASKED_NAME` with its interim text (LIB-92).
     */
    const val MASKED_NAME = "이름 가림"
    const val HIDDEN_RECORDS = "숨긴 기록"
    const val HIDDEN_GROUP_SUB = "내용 · 날짜 · 길이 가림 · 검색 제외"
    const val OPEN_WITH_FINGERPRINT = "지문으로 보기"
    const val OPENED = "열림"
    const val LOCKS_ON_LEAVE = "앱을 떠나면 잠김"
    const val RELOCK = "다시 잠그기"
    const val UNHIDE = "숨김 풀기"
    const val BIOMETRIC_TITLE = "숨긴 기록 보기"

    // 넣기.
    const val PUT_MANUSCRIPT = "원고에 넣기"
    const val PUT_CARD = "카드에 넣기"
    const val MAKE_PAGE = "페이지로 만들기"
    const val PUT_MEMO_FIELD = "메모 칸에 넣기"
    const val MAKE_CHARACTER = "인물로 만들기"
    const val MARK_DONE = "끝냄으로 표시"
    const val UNMARK_DONE = "다시 정리 안 됨"
    const val DELETE = "지우기"
    const val PUT_MANUSCRIPT_SUB = "위치 고르기 · 연결된 씬 · 마지막으로 쓰던 곳"
    const val PUT_CARD_SUB = "고른 탭 끝에 작가 메모"
    const val DELETE_SUB = "휴지통 · 30일 뒤 삭제"
    const val LINKED_SCENE = "연결된 씬"
    const val SCENE_END = "맨 끝"
    const val LAST_WRITING = "마지막으로 쓰던 곳"
    const val AFTER = "뒤"
    const val PICK_SCENE = "씬 고르기 ›"
    const val ALREADY_PUT = "이미 넣음"
    const val AUTHOR_MEMO = "작가 메모"
    const val MEMO_MARK = "⌁"
    const val PUT_DONE = "넣음"
    const val UNDO = "되돌리기"
    const val GO_SEE = "가 보기"
    const val PLACEMENTS = "넣은 곳"
    const val EDITED_AFTER = "◇ 넣은 뒤 고침"
    const val PLACEMENTS_GONE = "◇ 넣은 곳 지워짐"
    const val SCENE_MEMO = "씬 메모"
    const val RECORDER_MARK = "레코더"
    const val RECORD_PURGED = "지워진 기록"
    const val NAME = "이름"

    // Card tabs.
    const val TAB_OVERVIEW = "개요"
    const val TAB_SPEECH = "말투"
    const val TAB_SECRET = "비밀"
    const val TAB_DESCRIPTION = "설명"

    // Link labels (LRC-22, LRC-23).
    const val HIDDEN_PROJECT = "숨긴 프로젝트"
    const val HIDDEN_ITEM = "숨긴 항목"
    const val IN_TRASH = "휴지통"
    const val DELETED_SCENE = "지워진 씬"
    const val PURGED = "영구 삭제"
    const val EXAMPLE = "예시 · 끝나면 연결 없어짐"
    const val DIAMOND = "◇"
    const val DELETED_PREFIX = "지운"
    const val RELINK_CHIP = "칩 다시 고르기"
    const val REMOVE_CHIP = "칩 빼기"
    const val KEEP_AS_IS = "이대로 두기"
    const val LINK_OTHER = "다른 항목에 잇기"
    const val TO_PLAIN_TEXT = "글로 풀기"
    const val RESTORE = "되살리기"
    const val IN_TEXT_GROUP = "글에 나온 이름"

    // Trash, export, hooks.
    const val PURGE = "영구 삭제"
    const val DAYS_LEFT = "일 남음"
    const val EXPORT_RECORDS = "레코더 기록"
    const val EXPORT_AUDIO = "원본 음성 포함"
    const val EXCLUDED = "제외"
    const val TARGET_LINE = "레코더 · 정리 안 됨"
    const val RECORDS = "기록"
    const val START_MATERIAL = "라이프 레코더 기록 연결"
    const val CHECKLIST_TAIL = "정리하기"
    const val CHECKLIST_LINKED = "연결된 기록"
    const val INBOX = "기록함"

    // Missing copy (§6 Q21): ids for string resources, never shown as Korean text by this module.
    const val TODO_QS_TILE = "TODO_COPY_LRC_01"
    const val TODO_RESUME = "TODO_COPY_LRC_02"
    const val TODO_CANCELLED = "TODO_COPY_LRC_03"
    const val TODO_NOTIFICATION = "TODO_COPY_LRC_04"
    const val TODO_NO_ENGINE = "TODO_COPY_LRC_05"
    const val TODO_AUDIO_NOT_KEPT = "TODO_COPY_LRC_06"
    const val TODO_PICKER_TITLE = "TODO_COPY_LRC_07"
    const val TODO_GONE_UNIT = "TODO_COPY_LRC_08"
    const val TODO_MENU_ARIA = "TODO_COPY_LRC_09"
    const val TODO_OTHER_YEAR = "TODO_COPY_LRC_10"
    const val TODO_DONE_TOAST = "TODO_COPY_LRC_11"
    const val TODO_FILTERED_EMPTY = "TODO_COPY_LRC_12"
    const val TODO_CARD_TOAST = "TODO_COPY_LRC_13"
    const val TODO_PAGE_BUTTON = "TODO_COPY_LRC_14"
    const val TODO_PAGE_TOAST = "TODO_COPY_LRC_15"
    const val TODO_MEMO_TOAST = "TODO_COPY_LRC_16"
    const val TODO_CHARACTER_TOAST = "TODO_COPY_LRC_17"
    const val TODO_RELOCK_NOTE = "TODO_COPY_LRC_18"
    const val TODO_DELETED_TOAST = "TODO_COPY_LRC_19"
    const val TODO_EXPORT_HIDDEN_INCLUDED = "TODO_COPY_LRC_20"
}

/**
 * A toast (LIB-40 host). [text] is the message, or a `TODO_COPY_LRC_<n>` id when no source gives
 * it ([isTodo]); [actions] are the action labels in order.
 */
data class Toast(val text: String, val actions: List<String>) {
    val isTodo: Boolean get() = text.startsWith("TODO_COPY_")
}
