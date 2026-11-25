package io.github.lenticualrstone.civilight.feature.library.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.cast.field.FieldTemplates
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.model.app.AnimationPref
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.BlockSpacing
import io.github.lenticualrstone.civilight.model.app.DragResistance
import io.github.lenticualrstone.civilight.model.app.MiniRailProjectItem
import io.github.lenticualrstone.civilight.model.app.MiniRailScriptItem
import io.github.lenticualrstone.civilight.model.app.ProfileShape
import io.github.lenticualrstone.civilight.model.app.RelockPolicy
import io.github.lenticualrstone.civilight.model.app.ThemeMode
import io.github.lenticualrstone.civilight.ui.security.AuthRequest
import io.github.lenticualrstone.civilight.ui.security.AuthResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The app-settings screens (LIB-63 … LIB-68). Reads [AppSettings] through the shell's settings flow
 * and writes through the settings store's typed setters; values apply live (the shell bridges them
 * into the theme).
 */
internal class SettingsViewModel(private val services: LibraryServices) : ViewModel() {
    private val deps = services.deps
    private val settingsStore = deps.stores.settings

    val root: StateFlow<SettingsUi> = combine(
        deps.settings, deps.stores.library.state, services.backups.book.state, deps.stores.fieldTemplates.state, deps.unlock.state,
    ) { settings, library, book, templates, unlock ->
        val hidden = library.live.count { it.hidden && !it.isExample }
        // CST-62: only templates the viewer may see are counted.
        val visibleTemplates = FieldTemplates.visible(templates.templates, unlock.hiddenGroupOpen).size
        SettingsState.root(
            settings, hidden, book.recovery != null, lastBackupAt = book.state.lastSuccessAt, now = deps.clock.now(), zone = deps.clock.zone,
            visibleTemplates = visibleTemplates,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState.root(deps.settings.value, 0, services.facts.recoverySet(), null, deps.clock.now(), deps.clock.zone))

    init {
        services.backups.load()
        viewModelScope.launch { deps.stores.fieldTemplates.ensureLoaded() }
    }

    val hiding: StateFlow<HidingSettingsUi> = deps.settings
        .map { SettingsState.hiding(it, services.deviceSecure()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState.hiding(deps.settings.value, services.deviceSecure()))

    val textSize: StateFlow<TextSizeUi> = deps.settings
        .map(SettingsState::textSize)
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState.textSize(deps.settings.value))

    val motion: StateFlow<MotionSettingsUi> = deps.settings
        .map(DisplaySettingsState::motion)
        .stateIn(viewModelScope, SharingStarted.Eagerly, DisplaySettingsState.motion(deps.settings.value))

    val more: StateFlow<MoreSettingsUi> = deps.settings
        .map(DisplaySettingsState::more)
        .stateIn(viewModelScope, SharingStarted.Eagerly, DisplaySettingsState.more(deps.settings.value))

    /** The choices of a 지도 · 이미지 · 연표 value picker, as stored now. */
    fun options(value: MoreValue): List<ValueOption> = DisplaySettingsState.options(value, deps.settings.value)

    /**
     * (0.2.0, LIB-94) A `서재 보기` or `카드 보기` pick applies at once. The store's setter also turns that
     * screen's head toggle to the 3D view (a `목록` pick leaves it), so the choice is visible on return.
     * Runs on the app scope: the pick pops this screen at once, which cancels [viewModelScope] and would
     * drop a write still waiting for the settings store.
     */
    fun chooseView(kind: ViewKind, id: String) {
        deps.appScope.launch {
            when (kind) {
                ViewKind.LIBRARY -> ViewStyles.libraryStyleOf(id)?.let { settingsStore.setLibraryStyle(it) }
                ViewKind.CARD -> ViewStyles.cardStyleOf(id)?.let { settingsStore.setCastCardStyle(it) }
            }
        }
    }

    /** (0.2.0, LIB-94) `TODO_COPY_LIB_VIEW_TILT`. */
    fun setViewTilt(on: Boolean) {
        viewModelScope.launch { settingsStore.setViewTilt(on) }
    }

    fun setAnimation(pref: AnimationPref) {
        viewModelScope.launch { settingsStore.setAnimation(pref) }
    }

    fun setReduceTransparency(on: Boolean) {
        viewModelScope.launch { settingsStore.setReduceTransparency(on) }
    }

    /** `라이프 레코더 빛`: the same `AppSettings.recorderGlow` the recorder's `녹음 중 빛 띠` writes. */
    fun setRecorderGlow(on: Boolean) {
        viewModelScope.launch { settingsStore.setRecorderGlow(on) }
    }

    fun setHoloDefault(on: Boolean) {
        viewModelScope.launch { settingsStore.setHoloDefaultForNewImages(on) }
    }

    fun choose(value: MoreValue, optionId: String) {
        viewModelScope.launch { settingsStore.update { DisplaySettingsState.choose(it, value, optionId) } }
    }

    fun setCheckTravel(on: Boolean) {
        viewModelScope.launch { settingsStore.setCheckTravel(on) }
    }

    fun setCheckNoPlace(on: Boolean) {
        viewModelScope.launch { settingsStore.setCheckNoPlace(on) }
    }

    fun setTheme(index: Int) {
        val mode: ThemeMode = SettingsState.THEMES.getOrNull(index) ?: return
        viewModelScope.launch { settingsStore.setTheme(mode) }
    }

    fun setTextSlider(index: Int) {
        viewModelScope.launch { settingsStore.setTextSizeStep(SettingsState.stepOfSlider(index)) }
    }

    /**
     * `앱 잠금` (LIB-35): on needs a secure lock screen (§6 Q19 `ED`, a toast otherwise); off asks
     * once first (`ED`) and a cancelled prompt changes nothing, silently (LIB-37).
     */
    fun setAppLock(wanted: Boolean) {
        when (AppLockRules.change(deps.settings.value.appLock, wanted, services.deviceSecure())) {
            AppLockChange.NONE -> Unit
            AppLockChange.REFUSED_NO_DEVICE_LOCK ->
                deps.toasts.show(SettingsCopy.NO_DEVICE_LOCK, actionLabel = null)
            AppLockChange.TURN_ON -> viewModelScope.launch { settingsStore.setAppLock(true) }
            AppLockChange.TURN_OFF_AFTER_AUTH -> viewModelScope.launch {
                if (deps.biometric.authenticate(AuthRequest(SettingsCopy.APP_LOCK)) == AuthResult.SUCCESS) {
                    settingsStore.setAppLock(false)
                    deps.unlock.appLockDisabled()
                }
            }
        }
    }

    fun setRelock(policy: RelockPolicy) {
        viewModelScope.launch { settingsStore.setRelockPolicy(policy) }
    }

    fun setSearchResults(on: Boolean) {
        viewModelScope.launch { settingsStore.setHiddenInSearchWhileOpen(on) }
    }

    val settings: StateFlow<AppSettings> = deps.settings

    fun setBackupHidden(on: Boolean) {
        viewModelScope.launch { settingsStore.setBackupHidden(on) }
    }

    fun setDragResistance(value: DragResistance) {
        viewModelScope.launch { settingsStore.setDragResistance(value) }
    }

    fun setProfileShape(value: ProfileShape) {
        viewModelScope.launch { settingsStore.setProfileShape(value) }
    }

    fun setBlockSpacing(value: BlockSpacing) {
        viewModelScope.launch { settingsStore.setBlockSpacing(value) }
    }

    fun setLongPressWiggle(on: Boolean) {
        viewModelScope.launch { settingsStore.setLongPressWiggle(on) }
    }

    fun setTypewriterScroll(on: Boolean) {
        viewModelScope.launch { settingsStore.setTypewriterScroll(on) }
    }

    fun setSmartPunctuation(on: Boolean) {
        viewModelScope.launch { settingsStore.setSmartPunctuation(on) }
    }

    fun setSmartSpeakerSwap(on: Boolean) {
        viewModelScope.launch { settingsStore.setSmartSpeakerSwap(on) }
    }

    fun setLifeRecorderEnabled(on: Boolean) {
        viewModelScope.launch { settingsStore.setLifeRecorderEnabled(on) }
    }

    fun toggleMiniRailScript(item: MiniRailScriptItem) {
        viewModelScope.launch {
            val cur = deps.settings.value.miniRailScript
            val next = if (item in cur) {
                if (cur.size <= 1) cur else cur - item
            } else {
                MiniRailScriptItem.entries.filter { it in cur || it == item }
            }
            settingsStore.setMiniRailScript(next)
        }
    }

    fun toggleMiniRailProject(item: MiniRailProjectItem) {
        viewModelScope.launch {
            val cur = deps.settings.value.miniRailProject
            val next = if (item in cur) {
                if (cur.size <= 1) cur else cur - item
            } else {
                MiniRailProjectItem.entries.filter { it in cur || it == item }
            }
            settingsStore.setMiniRailProject(next)
        }
    }

    fun setPortraitAmbientBlur(on: Boolean) {
        viewModelScope.launch { settingsStore.setPortraitAmbientBlur(on) }
    }

    fun setMiniRailSide(side: io.github.lenticualrstone.civilight.model.app.MiniRailSide) {
        viewModelScope.launch { settingsStore.setMiniRailSide(side) }
    }

    fun setProfileTabPreset(preset: io.github.lenticualrstone.civilight.model.app.ProfileTabPreset) {
        viewModelScope.launch { settingsStore.setProfileTabPreset(preset) }
    }

    fun setCustomProfileTabs(tabs: List<io.github.lenticualrstone.civilight.model.app.ProfileTabConfig>) {
        viewModelScope.launch { settingsStore.setCustomProfileTabs(tabs) }
    }
}
