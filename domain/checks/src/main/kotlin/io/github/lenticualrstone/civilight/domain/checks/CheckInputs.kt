package io.github.lenticualrstone.civilight.domain.checks

import io.github.lenticualrstone.civilight.domain.story.travel.TravelSettings
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import java.time.ZoneId

/**
 * Everything a check run needs besides the project doc.
 *
 * @property travel the STO-42 switches `이동 점검` and `장소 없는 씬 알림`.
 * @property hiddenUnlocked the project's hidden items are open; while locked, rows about them
 *   collapse into `▲ 숨긴 항목 관련 N` (R19 §6). Checks are computed either way.
 * @property candidates the term candidate index (WLD-26). Null = a full scan now; an incomplete
 *   index (`CandidateIndex.pending()`) puts the center in `점검 중`.
 * @property now / [zone] the clock for `3일 남음` on deleted-item rows.
 * @property recorder the recorder library, for `레코더 N` reference counts.
 */
data class CheckInputs(
    val now: Timestamp,
    val zone: ZoneId = ZoneId.of("Asia/Seoul"),
    val travel: TravelSettings = TravelSettings(),
    val hiddenUnlocked: Boolean = false,
    val candidates: CandidateIndex? = null,
    val recorder: RecorderLibrary? = null,
) {
    companion object {
        /** Inputs with the switches of the app settings record. */
        fun of(
            settings: AppSettings,
            now: Timestamp,
            zone: ZoneId = ZoneId.of("Asia/Seoul"),
            hiddenUnlocked: Boolean = false,
            candidates: CandidateIndex? = null,
            recorder: RecorderLibrary? = null,
        ): CheckInputs = CheckInputs(
            now = now,
            zone = zone,
            travel = TravelSettings(checkTravel = settings.checkTravel, checkNoPlace = settings.checkNoPlace),
            hiddenUnlocked = hiddenUnlocked,
            candidates = candidates,
            recorder = recorder,
        )
    }
}
