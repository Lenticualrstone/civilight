package io.github.lenticualrstone.civilight.navigation

import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.model.Key
import kotlinx.serialization.Serializable

/**
 * Every full screen of the app is one [Route] (a Navigation3 [NavKey]). Routes are small
 * `@Serializable` values holding keys and simple choices only (no model objects), so the back stack
 * survives process death and fold-close (CNV-35).
 *
 * Each route belongs to exactly one area interface ([LibraryRoute], [StoryRoute], …); that area's
 * feature module registers the entry. Sheets are not routes: see [SheetKey].
 */
@Serializable
sealed interface Route : NavKey

/** A screen inside one project. The shell uses [projectKey] for loading, locks and capture blocking. */
sealed interface ProjectRoute : Route {
    val projectKey: Key
}

/** Screens of feature:library (LIB): 서재, onboarding, new project, settings, trash, files. */
sealed interface LibraryRoute : Route

/** Screens of feature:story (STO): project home, story structure, check centre, search, bulk. */
sealed interface StoryRoute : Route

/** Screens of feature:script (SCR). */
sealed interface ScriptRoute : Route

/** Screens of feature:cast (CST). */
sealed interface CastRoute : Route

/** Screens of feature:world (WLD terms, places, factions, timeline). */
sealed interface WorldRoute : Route

/** Screens of feature:map (WLD maps). */
sealed interface MapRoute : Route

/** Screens of feature:canvas (CNV canvas, pages, images). */
sealed interface CanvasRoute : Route

/** Screens of feature:recorder (LRC). */
sealed interface RecorderRoute : Route

/** Screens the shell (core:ui + app) draws itself. */
sealed interface ShellRoute : Route

/**
 * Screens whose content follows the 시점 렌즈 and show the lens chip (STO-13). Never the script,
 * the story structure or the check centre (R11 §3).
 */
interface LensScreen

/**
 * The list half of a list module (CNV-33). On an inner screen in `목록 + 상세` layout, a
 * [DetailPaneRoute] pushed on top of it is drawn beside it.
 */
interface ListPaneRoute

/** The detail half of a list module (CNV-33); drawn right of the [ListPaneRoute] below it when wide. */
interface DetailPaneRoute

/**
 * A full-window glass screen drawn over the blurred previous screen (search STO-37, recording
 * LRC-07). The previous screen stays composed underneath.
 */
interface OverlayRoute
