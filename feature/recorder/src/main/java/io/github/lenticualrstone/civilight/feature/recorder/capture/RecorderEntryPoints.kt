package io.github.lenticualrstone.civilight.feature.recorder.capture

/**
 * (0.2.0) LRC-48 · S-8: where the recorder can be opened. Inside the app a recording starts only from the
 * 프로젝트 홈 command-bar mic and the 기록함's own `말로 기록` · `글로 쓰기`; every other command bar draws no
 * mic. Outside the app nothing is offered: the home-screen and cover widgets, the quick-settings tile and
 * lock-screen recording are not declared (feature:recorder manifest) and their settings row is hidden.
 *
 * Each screen's bar decides its own mic; since the 0.2.0 integration only `feature:story` HomeScreen passes
 * `onMic`. `Recorder020Test.LRC_0_2_01_onlyHomePassesOnMic` scans every feature's sources for it.
 */
object RecorderEntryPoints {
    /** Screens with a command bar (LRC-01's list) plus the 기록함. */
    enum class Screen { HOME, SCRIPT, CAST, WORLD, LIBRARY, CANVAS, PAGE, GALLERY, CHECKS, MAP, TIMELINE, STRUCTURE, ASSETS, TRACK, INBOX }

    /** Outside surfaces of 0.1.0 (LRC-02 … LRC-05). */
    enum class Outside { HOME_WIDGET, QS_TILE, COVER_WIDGET, LOCK_SCREEN }

    /** Whether [screen]'s command bar draws the recorder mic (LRC-0.2-01): only 프로젝트 홈. */
    fun commandBarMic(screen: Screen): Boolean = screen == Screen.HOME

    /** Whether [screen] can start a capture at all: the home mic and the 기록함's buttons. */
    fun opensRecorder(screen: Screen): Boolean = screen == Screen.HOME || screen == Screen.INBOX

    /** Whether an outside surface is offered: none in 0.2.0 (N-73 `안 함`). */
    fun offered(surface: Outside): Boolean = false
}
