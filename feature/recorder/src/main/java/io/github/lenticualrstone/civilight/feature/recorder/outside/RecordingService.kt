package io.github.lenticualrstone.civilight.feature.recorder.outside

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import io.github.lenticualrstone.civilight.designsystem.R as DsR
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureHub
import io.github.lenticualrstone.civilight.feature.recorder.capture.LeaveRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps a recording alive while the app is in the background (LRC-08, LRC-09): a foreground service
 * of type `microphone` with the ongoing notification ([RecordingNotices]). It holds no state of its
 * own; the process-wide [CaptureHub] is the recording. It is started with `startService` while the
 * recording screen is visible and promotes itself; if the platform refuses (no permission, API
 * limits), it stops quietly and the recording goes on while the app is open (never a crash of the
 * capture). Tapping the notification reopens `녹음 중`. Removing the task from recents saves the take as
 * `정리 안 됨` and releases the microphone ([onTaskRemoved], LRC-49); the service stops when the hub goes idle.
 *
 * Not run on a device yet (APK builds are on hold).
 */
class RecordingService : Service() {
    private var inForeground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra(EXTRA_TITLE)
        val text = intent?.getStringExtra(EXTRA_TEXT)
        val channel = intent?.getStringExtra(EXTRA_CHANNEL)
        val route = intent?.getStringExtra(OutsideEntries.EXTRA_ROUTE)
        if (title == null || text == null || channel == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (inForeground) {
            // Already promoted: only the text changed (pause or resume). Updating the posted notification
            // never re-promotes the microphone service from the background, so it can never stop the take.
            runCatching { getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification(title, text, channel, route)) }
            return START_NOT_STICKY
        }
        try {
            val type = if (Build.VERSION.SDK_INT >= 30) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(title, text, channel, route), type)
            inForeground = true
        } catch (e: Exception) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    /**
     * The task was swiped away from recents (LRC-49, D32): the take is saved and the microphone released
     * ([LeaveRules.onTaskRemoved]). The service is not stopped here: [KeepAlive] stops it once the hub's
     * [CaptureHub.leaving] save has finished, so the process lives until the record is written.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        KeepAlive.hub?.let(LeaveRules::onTaskRemoved)
        super.onTaskRemoved(rootIntent)
    }

    private fun notification(title: String, text: String, channelName: String, route: String?): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW))
        val tap = route?.let { r ->
            packageManager.getLaunchIntentForPackage(packageName)?.component?.let { component ->
                android.app.PendingIntent.getActivity(
                    this, OutsideEntries.REQUEST_NOTIFICATION,
                    Intent(Intent.ACTION_MAIN).setComponent(component).putExtra(OutsideEntries.EXTRA_ROUTE, r)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(DsR.drawable.civilight_mark)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .apply { if (tap != null) setContentIntent(tap) }
            .build()
    }

    companion object {
        const val CHANNEL_ID = "recorder.recording"
        const val NOTIFICATION_ID = 7101
        private const val EXTRA_TITLE = "recorder.title"
        private const val EXTRA_TEXT = "recorder.text"
        private const val EXTRA_CHANNEL = "recorder.channel"

        fun start(context: Context, notice: RecordingNotice, route: String) {
            val intent = Intent(context, RecordingService::class.java)
                .putExtra(EXTRA_TITLE, notice.title)
                .putExtra(EXTRA_TEXT, notice.text)
                .putExtra(EXTRA_CHANNEL, notice.channelName)
                .putExtra(OutsideEntries.EXTRA_ROUTE, route)
            runCatching { context.startService(intent) }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, RecordingService::class.java)) }
        }
    }
}

/**
 * Follows the hub: the service runs exactly while a take is recording or paused. Attached once per
 * process from the first visible capture screen (a start from the background is not allowed, and
 * every start of a take happens on that screen); stopping is allowed any time.
 */
object KeepAlive {
    private var attached = false

    /** The hub the service follows (set on attach); the service saves through it when the task is removed. */
    @Volatile
    var hub: CaptureHub? = null
        private set

    fun attach(context: Context, hub: CaptureHub, scope: CoroutineScope) {
        synchronized(this) {
            if (attached) return
            attached = true
            this.hub = hub
        }
        val app = context.applicationContext
        scope.launch {
            hub.state.map { state -> RecordingNotices.of(state)?.let { it to state.entry } }
                // The notice text follows pause and resume (`녹음 중` · `일시정지`), so a change of notice re-sends it.
                .distinctUntilChanged { a, b -> a?.first == b?.first }
                .collect { pair ->
                    if (pair == null) {
                        // A leave (in-app, the activity finishing or the task removed) is written before the service goes.
                        hub.leaving?.join()
                        RecordingService.stop(app)
                    } else {
                        val route = io.github.lenticualrstone.civilight.navigation.RouteCodec.encode(OutsideEntries.resume(pair.second))
                        RecordingService.start(app, pair.first, route)
                    }
                }
        }
    }
}
