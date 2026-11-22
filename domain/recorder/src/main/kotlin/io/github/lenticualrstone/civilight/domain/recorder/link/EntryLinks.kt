package io.github.lenticualrstone.civilight.domain.recorder.link

import io.github.lenticualrstone.civilight.domain.recorder.RecorderEditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.recorder.EntryContext
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.ProjectFilter
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.recorder.RecorderSettings
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind

/** Entry contexts (`보던 곳`) and the one pre-filled link they give (LRC-19, rule 23 §2 table). */
object EntryLinks {
    /** Entries with no 보던 곳: only `+ 연결`. */
    private val NO_PLACE = setOf(
        EntrySurface.LIBRARY, EntrySurface.HOME_WIDGET, EntrySurface.QS_TILE, EntrySurface.LOCK_SCREEN, EntrySurface.COVER_WIDGET,
    )

    /**
     * The pre-filled target of a new capture, or null: script → that scene; card → that card;
     * page → that page; project home · 스토리 구조 · 연표 · 지도 and other in-project screens → that
     * project; inbox → its project filter ([EntryContext.projectKey], see [inbox]); 서재, widgets,
     * tile, lock screen → none. Setting `보던 곳 연결` off → none.
     */
    fun preset(entry: EntryContext, settings: RecorderSettings): TargetRef? {
        if (!settings.presetLink) return null
        if (entry.onLockScreen || entry.surface in NO_PLACE) return null
        val projectKey = entry.projectKey ?: return null
        val kind = entry.targetKind
        val key = entry.targetKey
        return if (kind != null && key != null && entry.surface != EntrySurface.INBOX) TargetRef(projectKey, kind, key)
        else TargetRef.project(projectKey)
    }

    /** The pending links a capture starts with: the preset as one `보던 곳` link, or none. */
    fun initialLinks(recordKey: Key, entry: EntryContext, settings: RecorderSettings, ctx: RecorderEditContext): List<RecorderLink> =
        preset(entry, settings)?.let { listOf(LinkEdits.newLink(recordKey, it, LinkOrigin.ENTRY, ctx)) }.orEmpty()

    // --- Entry builders for the callers ---

    fun script(projectKey: Key, sceneKey: Key, caretBlockKey: Key?, isExample: Boolean = false) = EntryContext(
        surface = EntrySurface.SCRIPT, projectKey = projectKey, targetKind = RecorderTargetKind.UNIT, targetKey = sceneKey,
        caretBlockKey = caretBlockKey, isExample = isExample,
    )

    fun card(surface: EntrySurface, projectKey: Key, kind: RecorderTargetKind, key: Key) =
        EntryContext(surface = surface, projectKey = projectKey, targetKind = kind, targetKey = key)

    fun page(projectKey: Key, pageKey: Key) =
        EntryContext(surface = EntrySurface.PAGE, projectKey = projectKey, targetKind = RecorderTargetKind.PAGE, targetKey = pageKey)

    /** Project home · 스토리 구조 · 연표 · 지도 · other screens inside a project. */
    fun project(surface: EntrySurface, projectKey: Key, isExample: Boolean = false) =
        EntryContext(surface = surface, projectKey = projectKey, isExample = isExample)

    /** 서재, widgets, tile, cover widget; lock screen when [onLockScreen]. */
    fun outside(surface: EntrySurface, onLockScreen: Boolean = surface == EntrySurface.LOCK_SCREEN) =
        EntryContext(surface = surface, onLockScreen = onLockScreen)

    /** The inbox's `말로 기록` / `글로 쓰기`: its project filter when one is set. */
    fun inbox(filter: ProjectFilter) =
        EntryContext(surface = EntrySurface.INBOX, projectKey = (filter as? ProjectFilter.Project)?.projectKey)
}
