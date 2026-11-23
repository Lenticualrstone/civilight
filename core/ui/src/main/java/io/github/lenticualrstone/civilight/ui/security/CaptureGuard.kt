package io.github.lenticualrstone.civilight.ui.security

import android.app.Activity
import android.app.ActivityManager
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.lenticualrstone.civilight.model.project.Protections

/** Name shown on the system recents card (LIB-29). */
enum class TaskLabel(val text: String) {
    /** Hidden-project content on screen: the plain app name, never the project. */
    APP_NAME("Civilight"),

    /** Content of a name-masked project on screen. */
    // (0.2.0 S-4, R-03) No rating word: the same interim copy as `RatingLabels.MASKED_PROJECT` (TODO_COPY_LIB_MASKED_NAME).
    MASKED_PROJECT("이름 가림"),
}

/**
 * What the window must hide while some content is on screen (LIB-26, LIB-29).
 * [secure] = `FLAG_SECURE` (screenshots, recording, recents preview black);
 * [hideRecentsPreview] = only the recents preview is masked;
 * [taskLabel] = recents card name override.
 */
data class WindowProtection(
    val secure: Boolean = false,
    val hideRecentsPreview: Boolean = false,
    val taskLabel: TaskLabel? = null,
) {
    val isNone: Boolean get() = !secure && !hideRecentsPreview && taskLabel == null

    /** The strongest of both (a hidden project's `Civilight` label wins over `이름 가림`). */
    operator fun plus(other: WindowProtection) = WindowProtection(
        secure = secure || other.secure,
        hideRecentsPreview = hideRecentsPreview || other.hideRecentsPreview || secure || other.secure,
        taskLabel = listOfNotNull(taskLabel, other.taskLabel).minByOrNull { it.ordinal },
    )

    companion object {
        val None = WindowProtection()

        /**
         * Protection while content of a project is on screen: capture block → `FLAG_SECURE`;
         * hidden → recents preview masked and the card named `Civilight`; name mask → `이름 가림`.
         */
        fun forProject(p: Protections): WindowProtection = WindowProtection(
            secure = p.captureBlock,
            hideRecentsPreview = p.hidden || p.captureBlock,
            taskLabel = when {
                p.hidden -> TaskLabel.APP_NAME
                p.nameMask -> TaskLabel.MASKED_PROJECT
                else -> null
            },
        )
    }
}

/**
 * Reference-counted protection requests of everything on screen. Screens and rows register while
 * composed ([ProtectWindow]); the merged value is handed to [applier] synchronously, so the flag is
 * set in the same frame the protected content first appears, and a leaving screen never clears the
 * flag of the screen entering over it (both are composed during a transition).
 */
class CaptureGuard(private val applier: (WindowProtection) -> Unit = {}) {
    private val requests = LinkedHashMap<Any, WindowProtection>()

    var current: WindowProtection = WindowProtection.None
        private set

    fun set(token: Any, protection: WindowProtection) {
        if (protection.isNone) requests.remove(token) else requests[token] = protection
        publish()
    }

    fun remove(token: Any) {
        if (requests.remove(token) != null) publish()
    }

    /** Extra protection that is not tied to composition (e.g. a lock screen showing mark only). */
    fun hold(token: Any, protection: WindowProtection) = set(token, protection)

    private fun publish() {
        val merged = requests.values.fold(WindowProtection.None) { a, b -> a + b }
        if (merged != current) {
            current = merged
            applier(merged)
        }
    }
}

val LocalCaptureGuard = staticCompositionLocalOf { CaptureGuard() }

/**
 * Keeps [protection] in force while this composable is on screen (LIB-26): use it on any row, card
 * or screen that shows content of a protected project, also outside the project (서재 hidden group,
 * search results, 기록함 rows, recents cards, the export sheet). Masked-only labels need none.
 */
@Composable
fun ProtectWindow(protection: WindowProtection) {
    val guard = LocalCaptureGuard.current
    val token = remember { Any() }
    DisposableEffect(guard, protection) {
        guard.set(token, protection)
        onDispose { guard.remove(token) }
    }
}

/** Applies [WindowProtection] to an activity window (FLAG_SECURE, recents screenshot, task label). */
class ActivityWindowProtector(private val activity: Activity) : (WindowProtection) -> Unit {
    override fun invoke(p: WindowProtection) {
        val recentsOnly = p.hideRecentsPreview && !p.secure
        // API 33+ can hide only the recents preview; before that FLAG_SECURE is the fallback (LIB-29 ED).
        val secure = p.secure || (recentsOnly && Build.VERSION.SDK_INT < 33)
        if (secure) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        if (Build.VERSION.SDK_INT >= 33) activity.setRecentsScreenshotEnabled(!p.hideRecentsPreview)
        val label = p.taskLabel?.text ?: activity.applicationInfo.loadLabel(activity.packageManager).toString()
        if (Build.VERSION.SDK_INT >= 33) {
            activity.setTaskDescription(ActivityManager.TaskDescription.Builder().setLabel(label).build())
        } else {
            @Suppress("DEPRECATION")
            activity.setTaskDescription(ActivityManager.TaskDescription(label))
        }
    }
}
