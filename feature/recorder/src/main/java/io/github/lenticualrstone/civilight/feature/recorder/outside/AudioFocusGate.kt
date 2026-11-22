package io.github.lenticualrstone.civilight.feature.recorder.outside

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

/**
 * Audio focus while recording (LRC-09): a call or another app taking focus pauses the recording,
 * which stays paused until the user resumes (`[PROPOSED]` §6 Q8).
 */
interface AudioFocusGate {
    /** Asks for focus while recording; [onLost] runs on the main thread when it is taken away. */
    fun request(onLost: () -> Unit)

    fun abandon()

    /** No focus handling (tests, and until the route installs the platform gate). */
    object None : AudioFocusGate {
        override fun request(onLost: () -> Unit) = Unit

        override fun abandon() = Unit
    }
}

/** The platform's audio focus: exclusive transient focus for speech; loss (incl. transient) pauses. */
class AndroidAudioFocus(context: Context) : AudioFocusGate {
    private val manager: AudioManager? = context.applicationContext.getSystemService(AudioManager::class.java)
    private var held: AudioFocusRequest? = null

    override fun request(onLost: () -> Unit) {
        val audio = manager ?: return
        if (held != null) return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(),
            )
            .setOnAudioFocusChangeListener({ change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) onLost()
            }, Handler(Looper.getMainLooper()))
            .build()
        runCatching { audio.requestAudioFocus(request) }
        held = request
    }

    override fun abandon() {
        val request = held ?: return
        held = null
        runCatching { manager?.abandonAudioFocusRequest(request) }
    }
}
