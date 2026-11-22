package io.github.lenticualrstone.civilight

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

/** Creates the [ServiceLocator] and feeds screen-off events to the re-lock rules (LIB-33). */
class CivilightApp : Application() {
    lateinit var locator: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        locator = ServiceLocator(this)
        ContextCompat.registerReceiver(
            this,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == Intent.ACTION_SCREEN_OFF) locator.unlock.onScreenOff()
                }
            },
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }
}

/** The process's [ServiceLocator]. */
val Context.locator: ServiceLocator get() = (applicationContext as CivilightApp).locator
