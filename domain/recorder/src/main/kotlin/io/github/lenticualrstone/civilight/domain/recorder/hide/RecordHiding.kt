package io.github.lenticualrstone.civilight.domain.recorder.hide

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEditContext
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink

/** The `숨기기` switch row of `정리 중` and the text sheet (LRC-42). [on] is the switch. */
data class HideRow(val on: Boolean) {
    val title: String get() = RecorderCopy.HIDE
    val sub: String get() = RecorderCopy.HIDE_SUB
}

/** Hiding records (LRC-40 … LRC-42). Hiding needs no authentication ([PROPOSED]); opening does. */
object RecordHiding {
    fun setHidden(library: RecorderLibrary, recordKey: Key, hidden: Boolean, ctx: RecorderEditContext): RecorderLibrary =
        library.copy(records = library.records.map {
            if (it.key == recordKey && it.hidden != hidden) it.copy(hidden = hidden, updatedAt = ctx.now, updatedBy = ctx.device) else it
        })

    /** `⋯ › 숨기기`: at once; the row leaves the list and the locked group grows. */
    fun hide(library: RecorderLibrary, recordKey: Key, ctx: RecorderEditContext) = setHidden(library, recordKey, true, ctx)

    /** `숨김 풀기` (only while hidden records are open). */
    fun unhide(library: RecorderLibrary, recordKey: Key, ctx: RecorderEditContext) = setHidden(library, recordKey, false, ctx)

    /**
     * True when a link (explicit, or implied through a sub-link) points into a hidden project or an
     * R-18 project: the condition for the `숨기기` row.
     */
    fun needsHideRow(links: List<RecorderLink>, env: RecorderEnv): Boolean = links.map { it.projectKey }.distinct().any { key ->
        val project = env.project(key)
        project != null && (project.hidden || project.rating == Rating.R18)
    }

    /**
     * The switch after the links changed: it appears **on** the first time the condition holds,
     * keeps the user's value while it holds, and disappears (null) when it no longer holds — then
     * the record is not hidden ([PROPOSED] LRC-42).
     */
    fun hideSwitch(current: Boolean?, links: List<RecorderLink>, env: RecorderEnv): Boolean? = when {
        !needsHideRow(links, env) -> null
        current == null -> true
        else -> current
    }

    fun row(switch: Boolean?): HideRow? = switch?.let(::HideRow)

    /** `숨긴 기록 <n>`: all hidden records not in the trash, regardless of filters ([PROPOSED]). */
    fun hiddenCount(library: RecorderLibrary): Int = library.records.count { it.hidden && it.deletedAt == null }
}
