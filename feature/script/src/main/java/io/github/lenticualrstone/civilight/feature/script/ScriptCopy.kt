package io.github.lenticualrstone.civilight.feature.script

/**
 * UI copy of the script screen that the domain module does not already produce (DECISIONS §3).
 * Every value is linted by `ScriptCopyLintTest`. Strings the domain builds (`ScriptCommands`,
 * `SceneText`, `Toasts`, `Manuscript`) are used as they come and linted there too.
 */
object ScriptCopy {
    // --- Header (SCR-01, SCR-02, SCR-14) ---

    /** `완료`: leaves editing, keeps the block selected (SCR-14). */
    const val DONE = "완료"

    /** Accessibility label of the layers icon (header and command row, SCR-01). */
    const val LAYERS_LABEL = "연출 · 메모 표시"

    /** The one row of the position-label menu (SCR-02). */
    const val SET_LENS_TO_SCENE = "렌즈를 이 씬으로 맞추기"

    // --- Bottom bar (SCR-01, SCR-15) ---

    /** Primary pill of the command row (SCR-15, Q-03 default: a soft line break). */
    const val LINE_BREAK = "줄바꿈"

    /** Life Recorder mic (owned by the recorder area). */
    const val MIC_LABEL = "라이프 레코더"

    // --- Manuscript states (SCR-01, SCR-14, SCR-18) ---

    /** Small label after the speaker of the block being typed (SCR-14). */
    const val TYPING = "입력 중"

    /** Small label after an inherited speaker until the first character (SCR-18). */
    const val INHERITED_SPEAKER = "직전 화자"

    /** Empty episode (SCR-01 `[PROPOSED]`). */
    const val NO_SCENES = "씬 없음"

    /** Button of the empty episode (SCR-01 `[PROPOSED]`). */
    const val NEW_SCENE = "새 씬"

    /** Speaker-slot words of a choice block (SCR-09, board `Blocks`). */
    const val CHOICE_LABEL = "선택지"
    const val CHOICE_REJOIN = "다음 블록에서 합류"

    /** Speaker-slot words of a system-text block (SCR-10, board `Blocks`). */
    const val SYSTEM_LABEL = "시스템"
    const val SYSTEM_SUB = "화면 문구"

    /** Speaker pill without a speaker (vocabulary: never `미지정`). */
    const val NO_SPEAKER = "화자 없음"

    /** Picker sub-line prefix when the display name differs at the block: `표시 이름 ???` (board `Picker`). */
    const val DISPLAY_NAME = "표시 이름"

    /** Scene-head `⋯` label: `씬 4 메뉴` (SCR-46). */
    const val SCENE_MENU_SUFFIX = "메뉴"

    fun sceneMenuLabel(ordinal: Int): String = "씬 $ordinal $SCENE_MENU_SUFFIX"

    /** `블록 추가` subline without an insert position (the first part of `지금 블록 아래 · 새 대사 화자: 리엔(직전)`). */
    const val ADD_BELOW = "지금 블록 아래"

    // --- Keyboard toolbar (SCR-16) ---

    /** Kind tool: `대사 ▾` (current kind + the sheet chevron). */
    const val KIND_CHEVRON = " ▾"

    const val PAREN_TOOL = "( )"
    const val PAREN_LABEL = "괄호 지문"
    const val LINK_TOOL = "@"
    const val CHANGE_TOOL = "◆"
    const val CHANGE_LABEL = "변화 기록"
    const val UNDO_LABEL = "되돌리기"

    /** Link picker title (SCR-55 names none). */
    const val LINK_PICKER_TITLE = "TODO_COPY_SCRIPT_LINK_PICKER_TITLE"

    // --- Undo step names (shown by undo history; spec words where one exists) ---

    const val UNDO_ADD_BLOCK = "블록 추가"
    const val UNDO_SPLIT = "나누기"
    const val UNDO_MERGE = "합치기"
    const val UNDO_DELETE = "지우기"
    const val UNDO_SPEAKER = "화자"
    const val UNDO_RESTORE = "되살리기"
    const val UNDO_LINE_BREAK = LINE_BREAK
    const val UNDO_TYPING = "TODO_COPY_SCRIPT_UNDO_TYPING"

    /** `닫기` (the command field while typing; the DS sheet header's default label). */
    const val CLOSE = "닫기"

    /** Every value above, for the lint test. */
    val all: List<String> = listOf(
        DONE, LAYERS_LABEL, SET_LENS_TO_SCENE, LINE_BREAK, MIC_LABEL, TYPING, INHERITED_SPEAKER, NO_SCENES,
        NEW_SCENE, CHOICE_LABEL, CHOICE_REJOIN, SYSTEM_LABEL, SYSTEM_SUB, NO_SPEAKER, DISPLAY_NAME, sceneMenuLabel(4), KIND_CHEVRON.trim(),
        PAREN_TOOL, PAREN_LABEL, LINK_TOOL, CHANGE_TOOL, CHANGE_LABEL, UNDO_LABEL, LINK_PICKER_TITLE,
        UNDO_ADD_BLOCK, UNDO_SPLIT, UNDO_MERGE, UNDO_DELETE, UNDO_SPEAKER, UNDO_RESTORE, ADD_BELOW, UNDO_LINE_BREAK, UNDO_TYPING, CLOSE,
    )

    /** Buttons (linted with the button rules). */
    val buttons: List<String> = listOf(DONE, LINE_BREAK, NEW_SCENE, SET_LENS_TO_SCENE, CLOSE)
}
