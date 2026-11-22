package io.github.lenticualrstone.civilight.feature.recorder.capture

import io.github.lenticualrstone.civilight.model.recorder.EntryContext
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.navigation.Recording

/**
 * The `Recording` route of core:navigation carries the entry context (보던 곳, LRC-19) as plain
 * strings, because other lanes push it from their command bars. This parser is tolerant: it
 * accepts the `EntrySurface` names in any case (`script`, `SCRIPT`, `projectHome`,
 * `project_home`) plus the aliases below, and infers the surface from the other arguments when
 * [Recording.surface] is the default `commandBar` or unknown.
 *
 * Accepted [Recording.targetKind]: `project`, `scene` · `episode` · `chapter` · `unit` (all one
 * structure unit), `character`, `place`, `term`, `faction`, `page`.
 */
object EntryRoutes {
    private val SURFACE_ALIASES: Map<String, EntrySurface> = mapOf(
        "widget" to EntrySurface.HOME_WIDGET,
        "home" to EntrySurface.PROJECT_HOME,
        "tile" to EntrySurface.QS_TILE,
        "qs" to EntrySurface.QS_TILE,
        "lock" to EntrySurface.LOCK_SCREEN,
        "cover" to EntrySurface.COVER_WIDGET,
        "cast" to EntrySurface.CHARACTER,
        "profile" to EntrySurface.CHARACTER,
        "storystructure" to EntrySurface.STORY,
        "structure" to EntrySurface.STORY,
        "world" to EntrySurface.OTHER_IN_PROJECT,
        "checkcenter" to EntrySurface.OTHER_IN_PROJECT,
        "canvas" to EntrySurface.OTHER_IN_PROJECT,
        "settings" to EntrySurface.OTHER_IN_PROJECT,
        "onboarding" to EntrySurface.LIBRARY,
        "seojae" to EntrySurface.LIBRARY,
    )

    private val INFERRED = setOf("", "commandbar", "command", "mic", "resume")

    /** A route [targetKind] string as a link target kind, or null when unknown. */
    fun targetKind(value: String?): RecorderTargetKind? = when (normalize(value.orEmpty())) {
        "project" -> RecorderTargetKind.PROJECT
        "scene", "episode", "chapter", "unit", "act" -> RecorderTargetKind.UNIT
        "character" -> RecorderTargetKind.CHARACTER
        "place" -> RecorderTargetKind.PLACE
        "term" -> RecorderTargetKind.TERM
        "faction" -> RecorderTargetKind.FACTION
        "page" -> RecorderTargetKind.PAGE
        else -> null
    }

    /**
     * The entry context of [route]. Only the lock-screen surface (`lock`, LRC-05) is on the lock
     * screen; it never pre-fills a link (rule 23 §2 table).
     */
    fun entryOf(route: Recording): EntryContext {
        val kind = targetKind(route.targetKind)
        val key = route.targetKey?.takeIf { kind != null }
        val projectKey = route.projectKey
        val named = surfaceNamed(route.surface)
        val surface = named ?: infer(projectKey, kind, route.caretBlockKey)
        return when {
            surface == EntrySurface.LOCK_SCREEN -> EntryContext(surface = surface, onLockScreen = true)
            // No project: nothing to pre-fill, whatever the surface says.
            projectKey == null -> EntryContext(surface = if (surface.needsProject) EntrySurface.LIBRARY else surface)
            else -> EntryContext(
                surface = surface,
                projectKey = projectKey,
                targetKind = kind.takeIf { key != null && it != RecorderTargetKind.PROJECT },
                targetKey = key.takeIf { kind != RecorderTargetKind.PROJECT },
                caretBlockKey = route.caretBlockKey.takeIf { surface == EntrySurface.SCRIPT },
            )
        }
    }

    /** The route that opens the capture again for [entry] (undo of `취소`, the running recording). */
    fun routeOf(entry: EntryContext?, startTyped: Boolean = false): Recording = Recording(
        surface = surfaceName(entry?.surface),
        projectKey = entry?.projectKey,
        targetKind = entry?.targetKind?.let(::kindName),
        targetKey = entry?.targetKey,
        caretBlockKey = entry?.caretBlockKey,
        startTyped = startTyped,
    )

    private fun surfaceNamed(value: String): EntrySurface? {
        val n = normalize(value)
        if (n in INFERRED) return null
        return EntrySurface.entries.firstOrNull { normalize(it.name) == n } ?: SURFACE_ALIASES[n]
    }

    private fun infer(projectKey: String?, kind: RecorderTargetKind?, caretBlockKey: String?): EntrySurface = when {
        projectKey == null -> EntrySurface.LIBRARY
        caretBlockKey != null -> EntrySurface.SCRIPT
        kind == RecorderTargetKind.UNIT -> EntrySurface.SCRIPT
        kind == RecorderTargetKind.CHARACTER -> EntrySurface.CHARACTER
        kind == RecorderTargetKind.PLACE -> EntrySurface.PLACE
        kind == RecorderTargetKind.TERM -> EntrySurface.TERM
        kind == RecorderTargetKind.FACTION -> EntrySurface.FACTION
        kind == RecorderTargetKind.PAGE -> EntrySurface.PAGE
        else -> EntrySurface.OTHER_IN_PROJECT
    }

    private val EntrySurface.needsProject: Boolean
        get() = this !in setOf(
            EntrySurface.LIBRARY, EntrySurface.HOME_WIDGET, EntrySurface.QS_TILE, EntrySurface.LOCK_SCREEN,
            EntrySurface.COVER_WIDGET, EntrySurface.INBOX,
        )

    private fun surfaceName(surface: EntrySurface?): String =
        surface?.name?.lowercase()?.split('_')?.mapIndexed { i, w -> if (i == 0) w else w.replaceFirstChar(Char::uppercase) }?.joinToString("")
            ?: "commandBar"

    private fun kindName(kind: RecorderTargetKind): String = kind.name.lowercase()

    private fun normalize(value: String): String = value.filter { it.isLetterOrDigit() }.lowercase()
}
