package io.github.lenticualrstone.civilight.feature.script.input

import io.github.lenticualrstone.civilight.domain.script.KindChange
import io.github.lenticualrstone.civilight.domain.script.MergeCheck

/**
 * (0.2.0) Copy of the U-14 writing flow, 찾기 · 바꾸기, 낭독, versions and marks (SCR-12, 13, 28, 60, 68, 70,
 * 72, 75 … 78). Spec `TODO_COPY_SCR_…` ids carry the spec's suggested words; they are listed under
 * "Copy to confirm" in the status file.
 */
object WriteCopy {
    // Kind menu (SCR-12) sublines; the names are the DS chip's.
    const val SUB_NARRATION = "화자 없음"
    const val SUB_DIRECTION = "글 한 줄"
    const val SUB_CHOICE = "화자 없음 · 2개 이상"
    const val SUB_SYSTEM = "화면 문구"
    const val SUB_MEMO = "게임 출력 제외"
    const val OFF = "꺼짐"

    /** `TODO_COPY_SCR_KIND_HAS_CUES` (suggestion). */
    const val KIND_HAS_CUES = "큐 있음"

    /**
     * `TODO_COPY_SCR_KIND_KEEPS_DATA` (suggestion): why a kind is off with `게임 제작 도구` off (G-2 names no cue).
     */
    const val KIND_KEEPS_DATA = "저장된 내용 있음"

    /** `TODO_COPY_SCR_KIND_CHOICE_LINES` (suggestion). */
    const val KIND_CHOICE_LINES = "2줄 이상"

    // 합치기 reasons (SCR-72); `TODO_COPY_SCR_MERGE_SCENE_START` suggestion `씬 처음`.
    const val MERGE_SCENE_START = "씬 처음"
    const val MERGE_KIND = "종류 다름"
    const val MERGE_SPEAKER = "화자 다름"

    /** The block being edited (SCR-04). */
    const val EDITING_TAG = "고치는 중"

    // New-block tags (SCR-04): after a send, the back block of a 나누기, the merged block.
    const val FRESH_SENT = "새로"
    const val FRESH_SPLIT = "나눔"
    const val FRESH_MERGED = "합침"

    // Cue chips in the input (SCR-08, switch on only).
    const val CUE_REMOVE = "큐 빼기"
    const val CUE_ADD = "큐 넣기"
    const val CUE_ASSET_SUB = "에셋 고르기"
    const val CUE_EFFECT_SUB = "페이드 · 암전"

    // Header (SCR-01, SCR-60).
    const val READ_ONLY_LINE = "읽기 전용 · 편집 불가"

    // Undo labels.
    const val UNDO_SEND = "보내기"
    const val UNDO_EDIT = "고치기"
    const val UNDO_NEXT_SCENE = "다음 씬"
    const val UNDO_REPLACE = "바꾸기"
    const val UNDO_VERSION_TEXT = "이 판 글로"
    const val UNDO_NAME = "표시 이름"
    const val UNDO_MARK = "복선"
    const val UNDO_TODO = "할 일"

    // 찾기 (SCR-68) and 바꾸기 (SCR-75).
    const val FIND_HINT = "명령 · 블록 · 인물 · 씬 이동"
    const val FIND_FIELD = "찾기"
    const val REPLACE = "바꾸기"
    /** `TODO_COPY_SCR_REPLACE_FIND` (N-28 sketch word). */
    const val REPLACE_FIND = "찾을 말"
    /** `TODO_COPY_SCR_REPLACE_WITH` (N-28 sketch word). */
    const val REPLACE_WITH = "바꿀 말"
    const val CHECK_ALL = "모두 고르기"
    const val PARTICLE_CHECK = "◇ 조사 확인"
    const val NO_HITS = "찾은 곳 없음"
    const val COMMANDS = "명령"
    const val READ_ALOUD_CMD = "낭독"
    const val VERSIONS_CMD = "지난 판"
    const val KEEP_VERSION_CMD = "판 남기기"

    fun replaceButton(k: Int): String = "$REPLACE · $k"

    /** `TODO_COPY_SCR_REPLACED` (suggestion `{k}곳 바꿈`). */
    fun replaced(k: Int): String = "${k}곳 바꿈"

    fun blocksSection(n: Int): String = "이 에피소드 · 블록 $n"

    /** `TODO_COPY_SCR_REPLACE_SCOPE` (suggestion): replace mode searches the whole project (D19). */
    fun projectBlocksSection(n: Int): String = "프로젝트 전체 · 블록 $n"

    /** A command row in 찾기: `› 낭독`. */
    fun commandRow(command: String): String = "› $command"

    /** A hit's scene in its location line: `씬 3`. */
    fun sceneLabel(n: Int): String = "씬 $n"

    /** A version row's subtitle: the number of blocks it holds. */
    fun versionBlocks(n: Int): String = "블록 $n"

    // 낭독 (SCR-28).
    const val PREV = "이전"
    const val FROM_START = "처음부터"
    const val CLOSE = "닫기"
    const val NEXT = "다음"

    // Versions (SCR-76).
    const val VERSIONS_TITLE = "지난 판"
    const val KEEP_VERSION = "판 남기기"
    const val VERSION_NAME = "이름"
    const val SAVE = "남기기"
    const val TO_THIS_VERSION = "이 판 글로"
    const val NO_VERSIONS = "남긴 판 없음"
    const val EXPORTED = "내보낸 판"

    fun hiddenSceneRow(n: Int): String = "숨긴 씬 · 블록 $n · 지문으로 보기"

    // 속성 rows (SCR-77, SCR-78).
    const val BLOCK_DISPLAY_NAME = "이 블록만 · 표시 이름"
    const val PLANT = "복선 심기"
    const val MENTION_OR_PAYOFF = "다시 언급 · 거두기"
    const val MENTION = "다시 언급"
    const val PAYOFF = "거두기"
    const val TODO = "할 일"
    const val MARK_NAME = "이름"
    const val PAYOFF_AT = "거둘 곳"
    const val PAYOFF_NONE = "정하지 않음"

    /** `TODO_COPY_SCR_PLANT_CONFIRM` (suggestion). */
    const val PLANT_CONFIRM = "심기"
    const val OPEN = "열림"
    const val PAID_OFF = "거둠"
    const val DONE = "끝냄"
    const val NO_MARKS = "복선 없음"

    fun markLine(name: String, state: String): String = "복선 · $name · $state"

    fun kindReason(reason: KindChange.Blocked): String = when (reason) {
        KindChange.Blocked.HAS_CUES -> KIND_HAS_CUES
        KindChange.Blocked.CHOICE_LINES -> KIND_CHOICE_LINES
        KindChange.Blocked.UNKNOWN -> OFF
    }

    fun mergeReason(reason: MergeCheck.Reason): String = when (reason) {
        MergeCheck.Reason.SCENE_START -> MERGE_SCENE_START
        MergeCheck.Reason.KIND -> MERGE_KIND
        MergeCheck.Reason.SPEAKER -> MERGE_SPEAKER
    }

    val all: List<String> = listOf(
        SUB_NARRATION, SUB_DIRECTION, SUB_CHOICE, SUB_SYSTEM, SUB_MEMO, OFF, KIND_HAS_CUES, KIND_CHOICE_LINES,
        MERGE_SCENE_START, MERGE_KIND, MERGE_SPEAKER, EDITING_TAG, CUE_REMOVE, CUE_ADD, CUE_ASSET_SUB, CUE_EFFECT_SUB, FRESH_SENT, FRESH_SPLIT, FRESH_MERGED, READ_ONLY_LINE, UNDO_SEND, UNDO_EDIT, UNDO_NEXT_SCENE, UNDO_REPLACE,
        UNDO_VERSION_TEXT, UNDO_NAME, UNDO_MARK, UNDO_TODO, FIND_HINT, FIND_FIELD, REPLACE_FIND, REPLACE_WITH, CHECK_ALL,
        PARTICLE_CHECK, NO_HITS, COMMANDS, READ_ALOUD_CMD, VERSIONS_CMD, KEEP_VERSION_CMD, replaced(3), blocksSection(12),
        VERSIONS_TITLE, VERSION_NAME, NO_VERSIONS, EXPORTED, hiddenSceneRow(12), BLOCK_DISPLAY_NAME, MARK_NAME, PAYOFF_AT,
        PAYOFF_NONE, OPEN, PAID_OFF, DONE, NO_MARKS, markLine("등불", OPEN), TODO,
        KIND_KEEPS_DATA, projectBlocksSection(12), commandRow(READ_ALOUD_CMD), sceneLabel(3), versionBlocks(12),
    )

    val buttons: List<String> = listOf(
        REPLACE, replaceButton(12), PREV, FROM_START, CLOSE, NEXT, KEEP_VERSION, SAVE, TO_THIS_VERSION, PLANT,
        MENTION_OR_PAYOFF, MENTION, PAYOFF, PLANT_CONFIRM,
    )
}
