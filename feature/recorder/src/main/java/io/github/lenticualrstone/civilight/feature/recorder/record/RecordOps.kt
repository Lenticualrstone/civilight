package io.github.lenticualrstone.civilight.feature.recorder.record

import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.inbox.MenuAction
import io.github.lenticualrstone.civilight.domain.recorder.inbox.RecordMenu
import io.github.lenticualrstone.civilight.domain.recorder.put.Destinations
import io.github.lenticualrstone.civilight.domain.recorder.put.PutKind
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.feature.recorder.inbox.MenuOptionView
import io.github.lenticualrstone.civilight.feature.recorder.inbox.MenuOutcome
import io.github.lenticualrstone.civilight.feature.recorder.inbox.MenuView
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary

/**
 * Which 넣기 can run on a record now (LRC-32, LRC-33 `[PROPOSED]`): the record has text (an empty
 * record cannot be copied anywhere) and at least one project is offered for that kind (not
 * read-only, not the example, not in the trash; for `원고에 넣기` not `설정만`). All five kinds run
 * since batch 4 (`메모 칸에 넣기`, `인물로 만들기`: LRC-37, LRC-38).
 */
object PutAvailability {
    /** The 넣기 kinds this build runs, in the row's order. */
    val BUILT: List<PutKind> = listOf(PutKind.MANUSCRIPT, PutKind.CARD, PutKind.PAGE, PutKind.MEMO_FIELD, PutKind.CHARACTER)

    /** Kinds that have at least one destination project. */
    fun offered(env: RecorderEnv): Set<PutKind> = BUILT.filter { Destinations.options(env, it).isNotEmpty() }.toSet()

    /** Kinds that can run on a record with [text]. */
    fun kinds(text: String, offered: Set<PutKind>): Set<PutKind> = if (text.isBlank()) emptySet() else offered

    fun kinds(library: RecorderLibrary, recordKey: Key, env: RecorderEnv): Set<PutKind> {
        val record = library.record(recordKey) ?: return emptySet()
        return kinds(Runs.text(record.runs), offered(env))
    }
}

/** What a screen showing records offers the shared `⋯` sheet (LRC-29): the inbox and the record screen. */
interface RecordMenuSource {
    /** The sheet for [recordKey], or null when the record is gone, in the trash, or hidden and locked. */
    fun menu(recordKey: Key): MenuView?

    suspend fun onMenu(recordKey: Key, action: MenuAction): MenuOutcome
}

/** The `⋯` sheet's content and what each option does, shared by every [RecordMenuSource]. */
object RecordMenus {
    fun menu(library: RecorderLibrary, recordKey: Key, env: RecorderEnv, now: Timestamp): MenuView? {
        val record = library.record(recordKey)?.takeIf { it.deletedAt == null && (!it.hidden || env.recordsOpen) } ?: return null
        val puts = PutAvailability.kinds(library, recordKey, env)
        val options = RecordMenu.options(library, record, env).map { MenuOptionView(it, enabled(it.action, puts)) }
        return MenuView(RecordMenu.preview(record, env, now), options)
    }

    /** The options that can run now: a 넣기 only with text and a destination (LRC-32), the others always. */
    fun enabled(action: MenuAction, puts: Set<PutKind>): Boolean = when (action) {
        MenuAction.PUT_MANUSCRIPT -> PutKind.MANUSCRIPT in puts
        MenuAction.PUT_CARD -> PutKind.CARD in puts
        MenuAction.MAKE_PAGE -> PutKind.PAGE in puts
        MenuAction.PUT_MEMO_FIELD -> PutKind.MEMO_FIELD in puts
        MenuAction.MAKE_CHARACTER -> PutKind.CHARACTER in puts
        MenuAction.MARK_DONE, MenuAction.UNMARK_DONE, MenuAction.HIDE, MenuAction.UNHIDE, MenuAction.DELETE -> true
    }

    /** The 넣기 kind an option opens, or null for the other options. */
    fun putKindOf(action: MenuAction): PutKind? = when (action) {
        MenuAction.PUT_MANUSCRIPT -> PutKind.MANUSCRIPT
        MenuAction.PUT_CARD -> PutKind.CARD
        MenuAction.MAKE_PAGE -> PutKind.PAGE
        MenuAction.PUT_MEMO_FIELD -> PutKind.MEMO_FIELD
        MenuAction.MAKE_CHARACTER -> PutKind.CHARACTER
        else -> null
    }

    /** Runs [action] on [recordKey] through [writer] (the 넣기 options only ask for their sheet). */
    suspend fun run(writer: RecordWriter, recordKey: Key, action: MenuAction, env: RecorderEnv): MenuOutcome {
        putKindOf(action)?.let { kind ->
            return if (kind in PutAvailability.BUILT) MenuOutcome.OpenPut(recordKey, kind) else MenuOutcome.Done
        }
        return when (action) {
            MenuAction.MARK_DONE -> writer.markDone(recordKey, env)?.let { MenuOutcome.ShowToast(it) { writer.undoMarkDone(recordKey) } } ?: MenuOutcome.Done
            MenuAction.UNMARK_DONE -> {
                writer.unmarkDone(recordKey, env)
                MenuOutcome.Done
            }
            MenuAction.HIDE -> {
                writer.hide(recordKey)
                MenuOutcome.Done
            }
            MenuAction.UNHIDE -> {
                writer.unhide(recordKey)
                MenuOutcome.Done
            }
            MenuAction.DELETE -> writer.delete(recordKey)?.let { done -> MenuOutcome.ShowToast(done.toast) { writer.undoDelete(done) } } ?: MenuOutcome.Done
            else -> MenuOutcome.Done
        }
    }
}
