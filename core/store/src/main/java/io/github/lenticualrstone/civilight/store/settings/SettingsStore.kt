package io.github.lenticualrstone.civilight.store.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.lenticualrstone.civilight.model.app.AnimationPref
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.BlockSpacing
import io.github.lenticualrstone.civilight.model.app.CardStyle
import io.github.lenticualrstone.civilight.model.app.DragResistance
import io.github.lenticualrstone.civilight.model.app.LibraryStyle
import io.github.lenticualrstone.civilight.model.app.LargeMapExport
import io.github.lenticualrstone.civilight.model.app.MapStylePref
import io.github.lenticualrstone.civilight.model.app.MiniRailProjectItem
import io.github.lenticualrstone.civilight.model.app.MiniRailScriptItem
import io.github.lenticualrstone.civilight.model.app.MiniRailSide
import io.github.lenticualrstone.civilight.model.app.PaneSplit
import io.github.lenticualrstone.civilight.model.app.ProfileShape
import io.github.lenticualrstone.civilight.model.app.ProfileTabConfig
import io.github.lenticualrstone.civilight.model.app.ProfileTabPreset
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.app.RelockPolicy
import io.github.lenticualrstone.civilight.model.app.SceneNotationPref
import io.github.lenticualrstone.civilight.model.app.TextMapEngine
import io.github.lenticualrstone.civilight.model.app.ThemeMode
import io.github.lenticualrstone.civilight.model.app.UnfoldedLayout
import io.github.lenticualrstone.civilight.model.app.ViewMode
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.recorder.RecorderSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * The one [AppSettings] record (class S, §2.13, LIB settings, CNV-46, STO-42, WLD-67, LRC-44) in
 * DataStore. Stored as one JSON value ([AppSettingsCodec]); a missing or unreadable value reads as
 * the defaults, and values a newer version added are ignored.
 */
class SettingsStore(private val dataStore: DataStore<Preferences>) {

    /** The settings; emits on every change. */
    val settings: Flow<AppSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { AppSettingsCodec.decode(it[KEY]) }
        .distinctUntilChanged()

    suspend fun current(): AppSettings = settings.first()

    /** Changes the settings in one write; returns the new value. */
    suspend fun update(transform: (AppSettings) -> AppSettings): AppSettings {
        var result = AppSettings()
        dataStore.edit { prefs ->
            val next = transform(AppSettingsCodec.decode(prefs[KEY]))
            prefs[KEY] = AppSettingsCodec.encode(next)
            result = next
        }
        return result
    }

    // Typed setters, one per settings row.
    suspend fun setTheme(value: ThemeMode) = update { it.copy(theme = value) }
    suspend fun setTextSizeStep(value: Int) = update { it.copy(textSizeStep = value) }
    suspend fun setUnfoldedLayout(value: UnfoldedLayout) = update { it.copy(unfoldedLayout = value) }
    suspend fun setAnimation(value: AnimationPref) = update { it.copy(animation = value) }
    suspend fun setReduceTransparency(value: Boolean) = update { it.copy(reduceTransparency = value) }
    suspend fun setRecorderGlow(value: Boolean) = update { it.copy(recorderGlow = value) }
    suspend fun setHoloDefaultForNewImages(value: Boolean) = update { it.copy(holoDefaultForNewImages = value) }
    suspend fun setViewMode(screen: String, value: ViewMode) = update { it.copy(viewByScreen = it.viewByScreen + (screen to value)) }
    suspend fun setAppLock(value: Boolean) = update { it.copy(appLock = value) }
    suspend fun setRelockPolicy(value: RelockPolicy) = update { it.copy(relockPolicy = value) }
    suspend fun setHiddenInSearchWhileOpen(value: Boolean) = update { it.copy(hiddenInSearchWhileOpen = value) }
    suspend fun setHiddenOutOfWidgetsRecents(value: Boolean) = update { it.copy(hiddenOutOfWidgetsRecents = value) }
    suspend fun setBackupHidden(value: Boolean) = update { it.copy(backupHidden = value) }
    suspend fun setDriveBackup(enabled: Boolean, accountId: String?) = update { it.copy(driveBackup = enabled, driveAccountId = accountId) }
    suspend fun setFirstUseTips(value: Boolean) = update { it.copy(firstUseTips = value) }
    suspend fun setDefaultSceneNotation(value: SceneNotationPref) = update { it.copy(defaultSceneNotation = value) }
    suspend fun setCheckTravel(value: Boolean) = update { it.copy(checkTravel = value) }
    suspend fun setCheckNoPlace(value: Boolean) = update { it.copy(checkNoPlace = value) }
    suspend fun setDefaultMapStyle(value: MapStylePref) = update { it.copy(defaultMapStyle = value) }
    suspend fun setTextMapEngine(value: TextMapEngine) = update { it.copy(textMapEngine = value) }
    suspend fun setLargeMapExport(value: LargeMapExport) = update { it.copy(largeMapExport = value) }
    suspend fun updateRecorder(transform: (RecorderSettings) -> RecorderSettings) = update { it.copy(recorder = transform(it.recorder)) }
    suspend fun updateRatingDefaults(transform: (RatingDefaults) -> RatingDefaults) = update { it.copy(ratingDefaults = transform(it.ratingDefaults)) }

    /** Gallery viewer `설명` toggle (55 GAL-09). */
    suspend fun setGalleryTextOn(value: Boolean) = update { it.copy(galleryTextOn = value) }

    /**
     * (0.2.0, LIB-94) `서재 보기`. Picking a 3D style also sets the 서재 head toggle to that view
     * (`CARD`) so the choice shows on return; picking `목록` leaves the toggle as stored.
     */
    suspend fun setLibraryStyle(value: LibraryStyle) = update {
        it.copy(libraryStyle = value, libraryToggle = if (value.isList) it.libraryToggle else ViewMode.CARD)
    }

    /** (0.2.0, LIB-94, CST-67) Character `카드 보기`; same toggle rule as [setLibraryStyle]. */
    suspend fun setCastCardStyle(value: CardStyle) = update {
        it.copy(castCardStyle = value, castToggle = if (value.isList) it.castToggle else ViewMode.CARD)
    }

    /** (0.2.0, LIB-93) 서재 head toggle position (`LIST` or `CARD`), remembered across restarts. */
    suspend fun setLibraryToggle(value: ViewMode) = update { it.copy(libraryToggle = value) }

    /** (0.2.0, CST-67) Character list head toggle position. */
    suspend fun setCastToggle(value: ViewMode) = update { it.copy(castToggle = value) }

    /** (0.2.0, LIB-94) Tilt follow switch. */
    suspend fun setViewTilt(value: Boolean) = update { it.copy(viewTilt = value) }

    /** CNV-47: the inner-screen pane ratio of [module] ([PaneSplit.Modules]); [PaneSplit.DEFAULT] clears it. */
    suspend fun setPaneSplit(module: String, value: PaneSplit) = update { it.withPaneSplit(module, value) }

    // (0.4.0) New interaction, script, and mini-rail customization setters
    suspend fun setDragResistance(value: DragResistance) = update { it.copy(dragResistance = value) }
    suspend fun setProfileShape(value: ProfileShape) = update { it.copy(profileShape = value) }
    suspend fun setBlockSpacing(value: BlockSpacing) = update { it.copy(blockSpacing = value) }
    suspend fun setLongPressWiggle(value: Boolean) = update { it.copy(longPressWiggle = value) }
    suspend fun setTypewriterScroll(value: Boolean) = update { it.copy(typewriterScroll = value) }
    suspend fun setSmartPunctuation(value: Boolean) = update { it.copy(smartPunctuation = value) }
    suspend fun setSmartSpeakerSwap(value: Boolean) = update { it.copy(smartSpeakerSwap = value) }
    suspend fun setLifeRecorderEnabled(value: Boolean) = update { it.copy(lifeRecorderEnabled = value) }
    suspend fun setMiniRailScript(value: List<MiniRailScriptItem>) = update { it.copy(miniRailScript = value) }
    suspend fun setMiniRailProject(value: List<MiniRailProjectItem>) = update { it.copy(miniRailProject = value) }

    // (0.5.0) Portrait ambient blur, mini-rail side, and modular character profile tab presets
    suspend fun setPortraitAmbientBlur(value: Boolean) = update { it.copy(portraitAmbientBlur = value) }
    suspend fun setMiniRailSide(value: MiniRailSide) = update { it.copy(miniRailSide = value) }
    suspend fun setProfileTabPreset(value: ProfileTabPreset) = update {
        it.copy(profileTabPreset = value, customProfileTabs = emptyList())
    }
    suspend fun setCustomProfileTabs(value: List<ProfileTabConfig>) = update { it.copy(customProfileTabs = value) }

    // (0.7.0, Labs) Script ambient glow backdrop
    suspend fun setScriptAmbientGlow(value: Boolean) = update { it.copy(scriptAmbientGlow = value) }

    companion object {
        val KEY = stringPreferencesKey("appSettings")
    }
}

/** JSON form of [AppSettings] in the preference value (lowerCamelCase enums, §1.5). */
object AppSettingsCodec {
    fun encode(settings: AppSettings): String = CivilightJson.encode(AppSettings.serializer(), settings)

    /** The settings in [text]; defaults when null or unreadable. */
    fun decode(text: String?): AppSettings {
        if (text.isNullOrBlank()) return AppSettings()
        return try {
            CivilightJson.decode(AppSettings.serializer(), text)
        } catch (_: IllegalArgumentException) {
            AppSettings()
        }
    }
}
