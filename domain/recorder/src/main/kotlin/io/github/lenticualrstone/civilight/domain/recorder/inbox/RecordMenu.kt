package io.github.lenticualrstone.civilight.domain.recorder.inbox

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEditContext
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.RecorderTimeLabels
import io.github.lenticualrstone.civilight.domain.recorder.Toast
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordState
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordStates
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord

enum class MenuAction { PUT_MANUSCRIPT, PUT_CARD, MAKE_PAGE, PUT_MEMO_FIELD, MAKE_CHARACTER, MARK_DONE, UNMARK_DONE, HIDE, UNHIDE, DELETE }

/** One option of the record `⋯` sheet; [sub] is null where no source gives one. */
data class MenuOption(val title: String, val sub: String?, val action: MenuAction)

/** The sheet's preview card: `음성 0:24 · 오늘 08:41` and a one-to-two-line excerpt. */
data class MenuPreview(val meta: String, val excerpt: String)

/** The record `⋯` menu (LRC-29) and the `끝냄` marks (LRC-26). Nothing is preselected. */
object RecordMenu {
    /** Excerpt length of the preview card (`ED`). */
    const val EXCERPT_MAX = 30

    fun preview(record: RecorderRecord, env: RecorderEnv, now: Timestamp): MenuPreview {
        val text = Runs.text(record.runs).replace('\n', ' ')
        val excerpt = if (text.length <= EXCERPT_MAX) text else text.substring(0, EXCERPT_MAX).trimEnd() + "…"
        return MenuPreview("${RecorderTimeLabels.kindLabel(record)}${RecorderCopy.SEP}${RecorderTimeLabels.meta(record.createdAt, now, env.zone)}", excerpt)
    }

    fun options(library: RecorderLibrary, record: RecorderRecord, env: RecorderEnv): List<MenuOption> {
        val state = RecordStates.state(library, record, env)
        return listOfNotNull(
            MenuOption(RecorderCopy.PUT_MANUSCRIPT, RecorderCopy.PUT_MANUSCRIPT_SUB, MenuAction.PUT_MANUSCRIPT),
            MenuOption(RecorderCopy.PUT_CARD, RecorderCopy.PUT_CARD_SUB, MenuAction.PUT_CARD),
            MenuOption(RecorderCopy.MAKE_PAGE, null, MenuAction.MAKE_PAGE),
            MenuOption(RecorderCopy.PUT_MEMO_FIELD, null, MenuAction.PUT_MEMO_FIELD),
            MenuOption(RecorderCopy.MAKE_CHARACTER, null, MenuAction.MAKE_CHARACTER),
            if (state == RecordState.UNSORTED) MenuOption(RecorderCopy.MARK_DONE, null, MenuAction.MARK_DONE) else null,
            if (state == RecordState.DONE) MenuOption(RecorderCopy.UNMARK_DONE, null, MenuAction.UNMARK_DONE) else null,
            if (!record.hidden) MenuOption(RecorderCopy.HIDE, RecorderCopy.HIDE_SUB, MenuAction.HIDE) else null,
            if (record.hidden && env.recordsOpen) MenuOption(RecorderCopy.UNHIDE, null, MenuAction.UNHIDE) else null,
            MenuOption(RecorderCopy.DELETE, RecorderCopy.DELETE_SUB, MenuAction.DELETE),
        )
    }

    private fun setDone(library: RecorderLibrary, key: Key, done: Boolean, ctx: RecorderEditContext) = library.copy(
        records = library.records.map { if (it.key == key) it.copy(doneMarked = done, updatedAt = ctx.now, updatedBy = ctx.device) else it },
    )

    /** `끝냄으로 표시` (only in `정리 안 됨`): toast TODO_COPY_LRC_11 + `되돌리기`. */
    fun markDone(library: RecorderLibrary, key: Key, env: RecorderEnv, ctx: RecorderEditContext): Pair<RecorderLibrary, Toast> {
        val record = library.record(key) ?: return library to Toast(RecorderCopy.TODO_DONE_TOAST, emptyList())
        require(RecordStates.state(library, record, env) == RecordState.UNSORTED) { "mark-done is offered only for unsorted records" }
        return setDone(library, key, true, ctx) to Toast(RecorderCopy.TODO_DONE_TOAST, listOf(RecorderCopy.UNDO))
    }

    /** The toast's `되돌리기` after `끝냄으로 표시`. */
    fun undoMarkDone(library: RecorderLibrary, key: Key, ctx: RecorderEditContext) = setDone(library, key, false, ctx)

    /** `다시 정리 안 됨` (only in `끝냄`). */
    fun unmarkDone(library: RecorderLibrary, key: Key, ctx: RecorderEditContext) = setDone(library, key, false, ctx)
}
