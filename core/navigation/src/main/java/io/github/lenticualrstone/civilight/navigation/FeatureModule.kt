package io.github.lenticualrstone.civilight.navigation

import io.github.lenticualrstone.civilight.model.Key

/** The module that registers a route's entry or a shared sheet's content. */
enum class FeatureModule(val gradlePath: String) {
    LIBRARY(":feature:library"),
    STORY(":feature:story"),
    SCRIPT(":feature:script"),
    CAST(":feature:cast"),
    WORLD(":feature:world"),
    MAP(":feature:map"),
    CANVAS(":feature:canvas"),
    RECORDER(":feature:recorder"),

    /** core:ui and app. */
    SHELL(":app"),
}

/** Owner of every route; exhaustive over the sealed areas, so a new area cannot be forgotten. */
val Route.owner: FeatureModule
    get() = when (this) {
        is LibraryRoute -> FeatureModule.LIBRARY
        is StoryRoute -> FeatureModule.STORY
        is ScriptRoute -> FeatureModule.SCRIPT
        is CastRoute -> FeatureModule.CAST
        is WorldRoute -> FeatureModule.WORLD
        is MapRoute -> FeatureModule.MAP
        is CanvasRoute -> FeatureModule.CANVAS
        is RecorderRoute -> FeatureModule.RECORDER
        is ShellRoute -> FeatureModule.SHELL
    }

/** The project a route shows, or null for app-level screens. */
val Route.projectKeyOrNull: Key?
    get() = (this as? ProjectRoute)?.projectKey

/** Whether the screen shows the lens chip (STO-13). */
val Route.hasLens: Boolean get() = this is LensScreen

/** A short stable name for logs and placeholder screens (`Script`, `CastList` …). */
val Any.routeName: String get() = this::class.simpleName ?: this::class.java.name.substringAfterLast('.')
