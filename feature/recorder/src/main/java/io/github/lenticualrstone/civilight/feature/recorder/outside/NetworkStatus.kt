package io.github.lenticualrstone.civilight.feature.recorder.outside

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Whether the device is online now (LRC-12: `클라우드` falls back to on-device without a network).
 * Read once per take, when it starts.
 */
class AndroidNetworkStatus(context: Context) : () -> Boolean {
    private val connectivity = context.applicationContext.getSystemService(ConnectivityManager::class.java)

    override fun invoke(): Boolean {
        val cm = connectivity ?: return false
        val caps = runCatching { cm.getNetworkCapabilities(cm.activeNetwork) }.getOrNull() ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
