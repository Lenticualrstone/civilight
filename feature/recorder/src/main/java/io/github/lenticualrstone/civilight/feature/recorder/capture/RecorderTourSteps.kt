package io.github.lenticualrstone.civilight.feature.recorder.capture

/**
 * The recorder tour's targets on the capture screen (LIB-19, board `TourLr`; copy in `ModuleTours`):
 * which element each step's hole goes around.
 * - `녹음 중`: `기록 남기기` = the bottom row (`글로 쓰기 · 멈추고 저장 · 일시정지`), `연결` = the chip row,
 *   `기록함` = `멈추고 저장` (the take goes to the 기록함).
 * - `정리 중`: `기록 남기기` = the bottom buttons, `연결` = the chip row, `기록함` = `기록함에 두기`.
 *
 * The tour waits ([hold]) while neither pane is drawn: the mic reason sheet, `마이크 꺼짐` (emulator QA
 * 2026-11-23: no mic permission, all three cards floated), the text memo, the link picker and the
 * start. It then runs on the first `녹음 중` with every hole in place, and is not marked seen before.
 * While a card is shown the take is paused (`CaptureHub.holdForTour`, tour QA 2026-11-23: the cards were
 * read over a live take, `00:03 → 00:40` in the record) and resumes when the tour ends (`releaseTour`).
 * The recorder has no ready-to-record stage to run the tour on instead: the mic starts the take at once (LRC-01).
 */
object RecorderTourSteps {
    /** `기록 남기기`: the pane's bottom controls. */
    const val RECORD: Int = 0

    /** `연결`: the link chip row. */
    const val LINK: Int = 1

    /** `기록함`: `멈추고 저장` (녹음 중) / `기록함에 두기` (정리 중). */
    const val INBOX: Int = 2

    const val COUNT: Int = 3

    /** Whether the tour waits: no capture pane ([CaptureSlot.Recording] / [CaptureSlot.Processing]) is up, or a sheet covers it. */
    fun hold(state: CaptureState): Boolean = hold(CaptureSlot.of(state), CaptureSheets.of(state))

    fun hold(slot: CaptureSlot, sheet: CaptureSheetKind?): Boolean =
        sheet != null || (slot !is CaptureSlot.Recording && slot !is CaptureSlot.Processing)
}
