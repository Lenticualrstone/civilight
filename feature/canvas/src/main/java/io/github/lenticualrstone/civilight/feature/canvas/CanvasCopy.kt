package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind
import kotlin.math.roundToInt

/**
 * UI copy of `:feature:canvas` (DECISIONS §3). Every value comes from `docs/spec/50-canvas-pages.md`
 * or the boards `Canvas`, `CanvasC`, `CanvasCLabel`; copy no source gives is a `TODO_COPY_CANVAS_*`
 * constant listed under Requests in `docs/status/feature-canvas.md`. Keys starting `BTN_` are buttons.
 * `CanvasCopyLintTest` runs the design-system lint over every value.
 */
object CanvasCopy {
    const val SEP = " · "

    // --- Header (CNV-02) ---
    /** Small mono line above the canvas name. */
    const val SMALL_LINE = "캔버스"
    const val BACK = "뒤로"
    const val READ_ONLY = "읽기 전용"

    /**
     * Name of a canvas made because the project had none (CNV-02 [PROPOSED]); no source names it, so
     * the area word is used instead of inventing a name (Requests).
     */
    const val NEW_CANVAS_NAME = "캔버스"

    // --- Tool bar (CNV-03) ---
    const val TOOL_NODE = "노드"
    const val TOOL_CHARACTER_INNER = "인물 카드"
    const val TOOL_CHARACTER_COVER = "인물"
    const val TOOL_MEMO = "메모"
    const val TOOL_LINE = "선"
    const val TOOL_GROUP = "묶음"

    /** Cover hint 92 dp above the bottom (`CanvasC`). */
    const val HINT = "두 손가락 이동 · 확대 · 노드는 끌어서 이동"

    // --- Zoom column (CNV-01) ---
    const val ZOOM_IN = "확대"
    const val TODO_COPY_CANVAS_ZOOM_OUT = "TODO_COPY_CANVAS_ZOOM_OUT"

    /** `82%`. */
    fun zoomPercent(zoom: Float): String = "${(zoom * 100).roundToInt()}%"

    // --- Node kinds (CNV-04 … CNV-06) ---
    const val KIND_CHARACTER = "인물"
    const val KIND_FACTION = "세력"
    const val KIND_EVENT = "사건"
    const val KIND_TERM = "용어"
    const val KIND_MEMO = "메모"
    const val KIND_GROUP = "묶음"

    /** The kind line of a node card. */
    fun kindWord(kind: CanvasNodeKind?): String = when (kind) {
        CanvasNodeKind.CHARACTER -> KIND_CHARACTER
        CanvasNodeKind.FACTION -> KIND_FACTION
        CanvasNodeKind.EVENT -> KIND_EVENT
        CanvasNodeKind.TERM -> KIND_TERM
        CanvasNodeKind.MEMO -> KIND_MEMO
        CanvasNodeKind.GROUP -> KIND_GROUP
        null -> KIND_MEMO
    }

    /** `노드` places one of these (Q-CV-02 default). */
    val NODE_TOOL_KINDS = listOf(KIND_FACTION, KIND_EVENT, KIND_TERM)

    // --- Pickers used to place item nodes (CNV-04) ---
    const val PICK_SUFFIX = " 고르기"

    /** `인물 고르기`, `세력 고르기`, `사건 고르기`, `용어 고르기`. */
    fun pickTitle(kindWord: String): String = "$kindWord$PICK_SUFFIX"

    const val SEARCH_PLACEHOLDER = "초성 검색"

    // --- Hidden and deleted references (CNV-11, rule 19 §6, rule 20 §2) ---
    const val HIDDEN_CHARACTER = "숨긴 인물"
    const val HIDDEN_ITEM = "숨긴 항목"
    const val DELETED_PREFIX = "지운 "

    /** `지운 사건 · 대정전`. */
    fun deleted(kindWord: String, name: String): String = "$DELETED_PREFIX$kindWord$SEP$name"

    // --- Line label (CNV-08, boards `CanvasCLabel`, `Canvas`) ---
    const val LINE_LABEL_TITLE = "선 라벨"
    const val BTN_CLOSE = "닫기"
    const val WHAT_TO_RECORD = "무엇으로 적을지"
    const val SLOT_BETWEEN = "사이"
    const val MIND_SUFFIX = "의 마음"

    /** `리엔의 마음`. */
    fun mindOf(name: String): String = "$name$MIND_SUFFIX"

    const val KIND = "종류"
    const val BTN_NEW_KIND = "+ 새 종류"
    const val START_POINT = "시작 시점"
    const val BTN_KEEP_LINE = "그냥 선으로 두기"
    const val BTN_MAKE_RELATION = "관계로 만들기"

    /** `리엔 · 하르트`. */
    fun pair(a: String, b: String): String = "$a$SEP$b"

    // --- Node edit (CNV-10; contents not drawn, Q-CV-03) ---
    const val BTN_OPEN_CHARACTER = "인물 기록"
    const val BTN_OPEN_TERM = "용어 카드"
    const val BTN_OPEN_FACTION = "세력 카드"
    const val BTN_OPEN_EVENT = "사건 상세"
    const val TODO_COPY_CANVAS_REMOVE_NODE = "TODO_COPY_CANVAS_REMOVE_NODE"
    const val TODO_COPY_CANVAS_REMOVE_LINE = "TODO_COPY_CANVAS_REMOVE_LINE"

    // --- Empty state (Q-EM-01: a read-only project without a canvas) ---
    const val TODO_COPY_CANVAS_NONE = "TODO_COPY_CANVAS_NONE"

    /** Undo-step names (shown in undo toasts). */
    const val UNDO_ADD_NODE = TOOL_NODE
    const val UNDO_MOVE = "노드는 끌어서 이동"
    const val UNDO_MEMO = KIND_MEMO
    const val UNDO_GROUP = KIND_GROUP
    const val UNDO_LINE = TOOL_LINE
    const val UNDO_NEW_CANVAS = SMALL_LINE
    const val UNDO_REMOVE = TOOL_NODE
}
