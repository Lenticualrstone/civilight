package io.github.lenticualrstone.civilight.feature.library.onboarding

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * The device-side choices of `OnbVoice` (LIB-15): microphone permission and the 라이프 레코더 home
 * widget. The widget belongs to feature:recorder; it is found through the app's own installed
 * widget providers, so this module needs no reference to it.
 */
object DeviceChoices {
    const val MIC_PERMISSION = Manifest.permission.RECORD_AUDIO

    fun micGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, MIC_PERMISSION) == PackageManager.PERMISSION_GRANTED

    /** True when one of this app's widgets is on a home screen. */
    fun widgetPlaced(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context) ?: return false
        return manager.getInstalledProvidersForPackage(context.packageName, null)
            .any { manager.getAppWidgetIds(it.provider).isNotEmpty() }
    }

    /**
     * Asks the launcher to pin the app's widget (`홈 화면 위젯 추가`). False when the launcher cannot
     * pin widgets or the app has none.
     */
    fun requestWidget(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context) ?: return false
        if (!manager.isRequestPinAppWidgetSupported) return false
        val provider = manager.getInstalledProvidersForPackage(context.packageName, null).firstOrNull() ?: return false
        return manager.requestPinAppWidget(provider.provider, null, null)
    }
}
