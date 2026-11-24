package io.github.lenticualrstone.civilight.feature.library.settings

import io.github.lenticualrstone.civilight.navigation.HelpReplay
import io.github.lenticualrstone.civilight.navigation.RecorderSettings
import io.github.lenticualrstone.civilight.navigation.RecoveryPassword
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.SettingsBackup
import io.github.lenticualrstone.civilight.navigation.SettingsHiding
import io.github.lenticualrstone.civilight.navigation.SettingsMore
import io.github.lenticualrstone.civilight.navigation.SettingsMotion
import io.github.lenticualrstone.civilight.navigation.SettingsTextSize
import io.github.lenticualrstone.civilight.navigation.TemplateList
import io.github.lenticualrstone.civilight.navigation.Trash

/** Every row of 설정 that opens a screen (CNV-18 row map). The theme and 앱 잠금 act in place. */
enum class SettingsRootRow { TEXT_SIZE, MOTION, MORE, DRIVE, RECOVERY, HIDING, TRASH, RECORDER, HELP, TEMPLATES }

/**
 * CNV-18: where each 설정 row leads. All but `라이프 레코더` (feature:recorder `RecorderSettings`) are
 * this lane's screens; `휴지통`, `라이프 레코더` and `도움말 · 안내 다시 보기` are the rows the board does
 * not draw (§6 Q27, Q-ST-01 `ED`); the Drive row opens 설정 › 보관 (Q27 `ED`).
 */
object SettingsRoutes {
    fun routeOf(row: SettingsRootRow): Route = when (row) {
        SettingsRootRow.TEXT_SIZE -> SettingsTextSize
        SettingsRootRow.MOTION -> SettingsMotion
        SettingsRootRow.MORE -> SettingsMore
        SettingsRootRow.DRIVE -> SettingsBackup
        SettingsRootRow.RECOVERY -> RecoveryPassword
        SettingsRootRow.HIDING -> SettingsHiding
        SettingsRootRow.TRASH -> Trash()
        SettingsRootRow.RECORDER -> RecorderSettings
        SettingsRootRow.HELP -> HelpReplay
        // CST-58: feature:cast's app-level template list.
        SettingsRootRow.TEMPLATES -> TemplateList()
    }
}
