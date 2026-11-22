package io.github.lenticualrstone.civilight.feature.recorder.outside

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.github.lenticualrstone.civilight.feature.recorder.capture.EntryRoutes
import io.github.lenticualrstone.civilight.model.recorder.EntryContext
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface
import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.navigation.RouteCodec

/**
 * Entry points outside the app's screens (LRC-02 … LRC-05, LRC-09): the route each surface opens
 * and the intent that opens it. None of them pre-fills a link (rule 23 §2 table).
 *
 * **needsExternal stub:** the spec's isolated surfaces are not built. The home and cover widgets
 * should record in a translucent, task-isolated activity "without opening the app" (LRC-02 `ED`),
 * and the lock screen needs a show-when-locked activity (LRC-05). Until those exist, every surface
 * opens the app's own `Recording` overlay through the launcher activity (shell.md §4 "From outside
 * the app"); on the lock screen the system asks for the device unlock first.
 */
object OutsideEntries {
    /**
     * Copy of the app's `MainActivity.EXTRA_ROUTE` (the extra holding a route as `RouteCodec` text).
     * Requested as a shared constant in core:navigation; this module cannot see the app.
     */
    const val EXTRA_ROUTE = "io.github.lenticualrstone.civilight.ROUTE"

    const val SURFACE_WIDGET = "widget"
    const val SURFACE_TILE = "tile"
    const val SURFACE_COVER = "cover"
    const val SURFACE_LOCK = "lock"

    /** The `Recording` route a surface opens; its entry context has no pre-filled link. */
    fun route(surface: EntrySurface): Recording = Recording(
        surface = when (surface) {
            EntrySurface.HOME_WIDGET -> SURFACE_WIDGET
            EntrySurface.QS_TILE -> SURFACE_TILE
            EntrySurface.COVER_WIDGET -> SURFACE_COVER
            EntrySurface.LOCK_SCREEN -> SURFACE_LOCK
            else -> SURFACE_WIDGET
        },
    )

    /** The route that shows the running capture again (the ongoing notification, LRC-09). */
    fun resume(entry: EntryContext?): Recording = EntryRoutes.routeOf(entry)

    /** An explicit intent for the app's launcher activity carrying [route]; null when the app has none. */
    fun intent(context: Context, route: Recording): Intent? {
        val component = context.packageManager.getLaunchIntentForPackage(context.packageName)?.component ?: return null
        return Intent(Intent.ACTION_MAIN)
            .setComponent(component)
            .putExtra(EXTRA_ROUTE, RouteCodec.encode(route))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    fun pendingIntent(context: Context, route: Recording, requestCode: Int): PendingIntent? {
        val intent = intent(context, route) ?: return null
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    const val REQUEST_WIDGET = 7001
    const val REQUEST_TILE = 7002
    const val REQUEST_NOTIFICATION = 7003
}
