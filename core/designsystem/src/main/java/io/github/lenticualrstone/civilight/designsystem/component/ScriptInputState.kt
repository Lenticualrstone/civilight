package io.github.lenticualrstone.civilight.designsystem.component

/*
 * U-14 input bar (SCR-70, SCR-16, SCR-72, SCR-73): the pure state of the draft and its reducer. The design
 * system knows no model types, so the block kind is mirrored by [InputKind] and the speaker by a key + label.
 * What only the script knows (the scene in view, the merge reason, cues, undo) comes in as values.
 */

/** The kind chip's values, in the kind menu's order (SCR-12). [speakerDimmed]: the speaker chip is dimmed. */
enum class InputKind(val label: String, val speakerDimmed: Boolean) {
    DIALOGUE(ScriptInputCopy.KIND_DIALOGUE, false),
    NARRATION(ScriptInputCopy.KIND_NARRATION, true),
    DIRECTION(ScriptInputCopy.KIND_DIRECTION, true),
    CHOICE(ScriptInputCopy.KIND_CHOICE, true),
    SYSTEM(ScriptInputCopy.KIND_SYSTEM, true),
    MEMO(ScriptInputCopy.KIND_MEMO, true),
}

/** (0.7.0) Streamlined 3-mode composer tabs matching Sonnet-Create (`대사 | 지문 | 장면`). */
enum class ComposerTab(val label: String) {
    DIALOGUE(ScriptInputCopy.TAB_DIALOGUE),
    NARRATION(ScriptInputCopy.TAB_NARRATION),
    SCENE(ScriptInputCopy.TAB_SCENE),
}

/** The speaker chip's value: a character [key] and the [label] the chip shows (`리엔`). */
data class InputSpeaker(val key: String, val label: String)

/** The mark above the input: none, `#044 뒤에` (SCR-73) or `#044 고치기` (SCR-72). [number] is `044`. */
sealed interface InputMark {
    data object None : InputMark

    data class After(val number: String) : InputMark

    data class Editing(val number: String) : InputMark
}

/** The draft as it was before `고치기` started; it comes back whole when `고치기` ends (SCR-72). */
data class SetAsideDraft(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int,
    val speaker: InputSpeaker?,
    val kind: InputKind,
    val mark: InputMark,
)

/**
 * The input bar's view state. [speaker] null is `화자 없음`. [sceneNumber] is the ordinal of the scene in view
 * (`씬 3 끝에`), null when there is none (`씬 없음`). [sceneFlash] counts scene changes for the place-label
 * flash (0 = never flashed). [imeVisible] and [focused] decide the tool row.
 */
data class ScriptInputState(
    val text: String = "",
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0,
    val speaker: InputSpeaker? = null,
    val kind: InputKind = InputKind.DIALOGUE,
    val mark: InputMark = InputMark.None,
    val setAside: SetAsideDraft? = null,
    val sceneNumber: Int? = null,
    val sceneFlash: Int = 0,
    val imeVisible: Boolean = false,
    val focused: Boolean = false,
    /** (0.7.0) Additional speakers for simultaneous/chorus dialogue. */
    val coSpeakers: List<InputSpeaker> = emptyList(),
    /** (0.7.0) True when the `[장면]` composer tab is selected (sending creates a new scene). */
    val sceneMode: Boolean = false,
    /** (0.7.0) Incremented on empty send attempts to drive subtle shake feedback. */
    val shakeCount: Int = 0,
) {
    val editing: Boolean get() = mark is InputMark.Editing

    /** Active 3-mode composer tab. */
    val activeTab: ComposerTab
        get() = when {
            sceneMode && !editing -> ComposerTab.SCENE
            kind == InputKind.DIALOGUE -> ComposerTab.DIALOGUE
            else -> ComposerTab.NARRATION
        }
}

/** Things that happen to the input bar. */
sealed interface ScriptInputEvent {
    /** The field's text or caret changed (typing, IME, paste). */
    data class TextChanged(val text: String, val selectionStart: Int, val selectionEnd: Int) : ScriptInputEvent

    /** Enter (soft or hardware): a line break at the caret. It never sends (U-14.7). */
    data object EnterPressed : ScriptInputEvent

    data class SpeakerPicked(val speaker: InputSpeaker?) : ScriptInputEvent

    data class KindPicked(val kind: InputKind) : ScriptInputEvent

    /** Right swipe `이 뒤에` on block [number] (ignored during `고치기`). */
    data class InsertAfter(val number: String) : ScriptInputEvent

    /** Right swipe `고치기` on block [number]: its text, speaker and kind move into the input. */
    data class StartEdit(val number: String, val text: String, val speaker: InputSpeaker?, val kind: InputKind) : ScriptInputEvent

    /** `✕`: clears `이 뒤에`, or ends `고치기` and discards the edits. */
    data object CloseMark : ScriptInputEvent

    /** A send, `고치기` send, `나누기` or `합치기` went through (the caller wrote it). */
    data object Sent : ScriptInputEvent

    /**
     * The `고치기` block is gone. [locked] true: it became hidden and locked, so the field is cleared; false:
     * it was removed, and the typed text stays as a plain draft (SCR-72).
     */
    data class EditTargetGone(val locked: Boolean) : ScriptInputEvent

    /** The `이 뒤에` block is gone, moved or locked: the mark clears silently (SCR-73). */
    data object AfterTargetGone : ScriptInputEvent

    /** The scene in view (SCR-71), null for none. */
    data class SceneInView(val number: Int?) : ScriptInputEvent

    data class Ime(val visible: Boolean) : ScriptInputEvent

    data class Focus(val focused: Boolean) : ScriptInputEvent
}

/** What the place area shows (SCR-70 table); the bar renders it with [ScriptInputCopy]. */
sealed interface InputPlace {
    data class SceneEnd(val number: Int) : InputPlace

    data object NoScene : InputPlace

    data class After(val number: String) : InputPlace

    data class Editing(val number: String) : InputPlace
}

/** The send slot: icon `send` (a) or the text `고치기` (c, scope ICON). */
enum class SendSlot { ICON, EDIT_TEXT }

/** Values only the script knows, read by [ScriptInputRules]. */
data class InputContext(
    val readOnly: Boolean = false,
    /** `연출` with `게임 제작 도구` on: the draft has at least one cue chip. */
    val hasCues: Boolean = false,
    /** A block for `◆`: the `고치기` block or the selected block (SCR-16). */
    val hasTargetBlock: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
)

/** The pure reducer of the input bar (JVM tested). */
object ScriptInput {
    fun reduce(state: ScriptInputState, event: ScriptInputEvent): ScriptInputState = when (event) {
        is ScriptInputEvent.TextChanged -> {
            val len = event.text.length
            state.copy(
                text = event.text,
                selectionStart = event.selectionStart.coerceIn(0, len),
                selectionEnd = event.selectionEnd.coerceIn(0, len),
            )
        }
        ScriptInputEvent.EnterPressed -> {
            val a = minOf(state.selectionStart, state.selectionEnd).coerceIn(0, state.text.length)
            val b = maxOf(state.selectionStart, state.selectionEnd).coerceIn(0, state.text.length)
            val text = state.text.substring(0, a) + "\n" + state.text.substring(b)
            state.copy(text = text, selectionStart = a + 1, selectionEnd = a + 1)
        }
        is ScriptInputEvent.SpeakerPicked -> state.copy(speaker = event.speaker)
        is ScriptInputEvent.KindPicked -> state.copy(kind = event.kind)
        is ScriptInputEvent.InsertAfter -> if (state.editing) state else state.copy(mark = InputMark.After(event.number))
        is ScriptInputEvent.StartEdit -> {
            // A second 고치기 replaces the first; the draft set aside at the start stays the one to restore.
            val aside = state.setAside ?: SetAsideDraft(
                state.text, state.selectionStart, state.selectionEnd, state.speaker, state.kind, state.mark,
            )
            state.copy(
                text = event.text,
                selectionStart = event.text.length,
                selectionEnd = event.text.length,
                speaker = event.speaker,
                kind = event.kind,
                mark = InputMark.Editing(event.number),
                setAside = aside,
            )
        }
        ScriptInputEvent.CloseMark -> when (state.mark) {
            InputMark.None -> state
            is InputMark.After -> state.copy(mark = InputMark.None)
            is InputMark.Editing -> restore(state)
        }
        ScriptInputEvent.Sent -> when (state.mark) {
            is InputMark.Editing -> restore(state)
            // The chips keep their values (U-14.2); the 이 뒤에 mark is used once (U-14.5).
            else -> state.copy(text = "", selectionStart = 0, selectionEnd = 0, mark = InputMark.None)
        }
        is ScriptInputEvent.EditTargetGone -> when {
            !state.editing -> state
            event.locked -> restore(state)
            else -> state.copy(mark = InputMark.None, setAside = null)
        }
        ScriptInputEvent.AfterTargetGone -> if (state.mark is InputMark.After) state.copy(mark = InputMark.None) else state
        is ScriptInputEvent.SceneInView -> {
            val before = state.sceneNumber
            val flash = before != null && event.number != null && before != event.number
            state.copy(sceneNumber = event.number, sceneFlash = if (flash) state.sceneFlash + 1 else state.sceneFlash)
        }
        is ScriptInputEvent.Ime -> state.copy(imeVisible = event.visible)
        is ScriptInputEvent.Focus -> state.copy(focused = event.focused)
    }

    /** Ends `고치기`: the set-aside draft comes back whole, or an empty field with the chips from before. */
    private fun restore(state: ScriptInputState): ScriptInputState {
        val a = state.setAside ?: return state.copy(text = "", selectionStart = 0, selectionEnd = 0, mark = InputMark.None)
        return state.copy(
            text = a.text,
            selectionStart = a.selectionStart,
            selectionEnd = a.selectionEnd,
            speaker = a.speaker,
            kind = a.kind,
            mark = a.mark,
            setAside = null,
        )
    }
}

/** Derived rules of the input bar (SCR-70 validation, SCR-16, SCR-72). */
object ScriptInputRules {
    /** Most lines the field grows to before it scrolls inside itself (U-14.16). */
    const val MAX_VISIBLE_LINES: Int = 6

    /** Least option lines a `선택지` send needs (U-14.11). */
    const val MIN_CHOICE_LINES: Int = 2

    /** The draft's lines (split at line breaks). */
    fun lines(text: String): List<String> = text.split('\n')

    /** Non-blank lines, trimmed: a `선택지` draft's options. */
    fun choiceOptions(text: String): List<String> = lines(text).map { it.trim() }.filter { it.isNotEmpty() }

    /** The draft trimmed of leading and trailing whitespace and line breaks (SCR-70 send step 1). */
    fun normalized(text: String): String = text.trim()

    /** Whether the draft holds content for its kind (SCR-70 content rule). */
    fun hasContent(state: ScriptInputState, hasCues: Boolean = false): Boolean = when {
        state.sceneMode && !state.editing -> true
        state.kind == InputKind.CHOICE -> choiceOptions(state.text).size >= MIN_CHOICE_LINES
        state.kind == InputKind.DIRECTION -> state.text.isNotBlank() || hasCues
        else -> state.text.isNotBlank() || (state.kind == InputKind.NARRATION && hasCues)
    }

    /** Whether a place to write exists: the `고치기` block, the `이 뒤에` block or a scene in view. */
    fun hasTarget(state: ScriptInputState): Boolean = when {
        state.sceneMode && !state.editing -> true
        state.mark is InputMark.Editing || state.mark is InputMark.After -> true
        else -> state.sceneNumber != null
    }

    /** `보내기` (or the `고치기` button) is on. */
    fun canSend(state: ScriptInputState, ctx: InputContext = InputContext()): Boolean =
        !ctx.readOnly && hasTarget(state) && hasContent(state, ctx.hasCues)

    fun sendSlot(state: ScriptInputState): SendSlot = if (state.editing) SendSlot.EDIT_TEXT else SendSlot.ICON

    fun place(state: ScriptInputState): InputPlace = when (val m = state.mark) {
        is InputMark.Editing -> InputPlace.Editing(m.number)
        is InputMark.After -> InputPlace.After(m.number)
        InputMark.None -> state.sceneNumber?.let { InputPlace.SceneEnd(it) } ?: InputPlace.NoScene
    }

    /** `다음 씬` shows only with no mark (SCR-70, SCR-72). */
    fun showsNextScene(state: ScriptInputState, readOnly: Boolean = false): Boolean = !readOnly && state.mark == InputMark.None

    /** The speaker chip is dimmed for every kind but `대사` (U-14.2, U-14.20). */
    fun speakerDimmed(state: ScriptInputState): Boolean = state.kind.speakerDimmed

    /** The tool row shows only while the keyboard is up and the field has focus (U-14.10). */
    fun showsToolRow(state: ScriptInputState, readOnly: Boolean = false): Boolean = !readOnly && state.imeVisible && state.focused

    /** `( )` is on for `대사` only (SCR-16). */
    fun parenEnabled(state: ScriptInputState): Boolean = state.kind == InputKind.DIALOGUE

    /** `◆` needs a block: the `고치기` block or the selected one (SCR-16). */
    fun changeEnabled(state: ScriptInputState, ctx: InputContext): Boolean = state.editing || ctx.hasTargetBlock

    /**
     * `나누기` (SCR-72): during `고치기`, not for `선택지`, with non-blank text on both sides of the caret.
     */
    fun canSplit(state: ScriptInputState): Boolean {
        if (!state.editing || state.kind == InputKind.CHOICE) return false
        val at = state.selectionEnd.coerceIn(0, state.text.length)
        return state.text.substring(0, at).isNotBlank() && state.text.substring(at).isNotBlank()
    }

    /** The text before and after the caret for `나누기`, whitespace at the cut trimmed. */
    fun splitParts(state: ScriptInputState): Pair<String, String> {
        val at = state.selectionEnd.coerceIn(0, state.text.length)
        return state.text.substring(0, at).trim() to state.text.substring(at).trim()
    }

    /** How many lines the field draws (1 … 6); past 6 it scrolls inside (U-14.16). */
    fun visibleLines(lineCount: Int): Int = lineCount.coerceIn(1, MAX_VISIBLE_LINES)

    /** Whether the field scrolls inside itself. */
    fun scrollsInside(lineCount: Int): Boolean = lineCount > MAX_VISIBLE_LINES
}

/** Copy of the input bar and its read-only row (SCR-70, SCR-16, SCR-60). */
object ScriptInputCopy {
    const val PLACEHOLDER = "쓰기…"
    const val PLACEHOLDER_DIALOGUE = "대사 쓰기…"
    const val PLACEHOLDER_NARRATION = "지문 · 서술 쓰기… (// 메모)"
    const val PLACEHOLDER_SCENE = "새 장면 제목…"
    const val TAB_DIALOGUE = "대사"
    const val TAB_NARRATION = "지문"
    const val TAB_SCENE = "장면"
    const val TOOL_REDO_NAME = "다시 실행"
    const val LEADING_SPEAKER = "화자 고르기"
    const val LEADING_ATTACH = "첨부 · 연출"
    const val LEADING_SCENE = "장면 설정"
    const val SPEAKER = "화자"
    const val KIND = "종류"
    const val NO_SPEAKER = "화자 없음"
    const val KIND_DIALOGUE = "대사"
    const val KIND_NARRATION = "나레이션"
    const val KIND_DIRECTION = "연출"
    const val KIND_CHOICE = "선택지"
    const val KIND_SYSTEM = "시스템"
    const val KIND_MEMO = "메모"
    const val END = "끝에"
    const val AFTER = "뒤에"
    const val EDITING = "고치기"
    const val NO_SCENE = "씬 없음"
    const val TOOL_PAREN = "( )"
    const val TOOL_PAREN_NAME = "괄호 지문"
    const val TOOL_LINK = "@"
    const val TOOL_LINK_NAME = "잇기"
    const val TOOL_CHANGE = "◆"
    const val TOOL_CHANGE_NAME = "변화 기록"
    const val TOOL_UNDO_NAME = "되돌리기"
    const val READ_ONLY_LABEL = "예시 프로젝트"
    const val READ_ONLY_HOME = "예시 홈으로"
    const val READ_ONLY_FINISH = "온보딩 끝내기"

    /** `씬 3` (bold part of `씬 3 끝에`). */
    fun scene(n: Int): String = "씬 $n"

    fun sceneEnd(n: Int): String = "${scene(n)} $END"

    /** `#044`. */
    fun number(n: String): String = "#$n"

    fun after(n: String): String = "${number(n)} $AFTER"

    fun editing(n: String): String = "${number(n)} $EDITING"

    val all: List<String> = listOf(
        PLACEHOLDER, SPEAKER, KIND, NO_SPEAKER, KIND_DIALOGUE, KIND_NARRATION, KIND_DIRECTION, KIND_CHOICE, KIND_SYSTEM,
        KIND_MEMO, END, AFTER, EDITING, NO_SCENE, TOOL_PAREN, TOOL_PAREN_NAME, TOOL_LINK, TOOL_LINK_NAME, TOOL_CHANGE,
        TOOL_CHANGE_NAME, TOOL_UNDO_NAME, READ_ONLY_LABEL, READ_ONLY_HOME, READ_ONLY_FINISH,
        sceneEnd(3), after("044"), editing("044"),
    )
}
