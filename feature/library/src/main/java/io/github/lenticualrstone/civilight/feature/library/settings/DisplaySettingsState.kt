package io.github.lenticualrstone.civilight.feature.library.settings

import io.github.lenticualrstone.civilight.model.app.AnimationPref
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.LargeMapExport
import io.github.lenticualrstone.civilight.model.app.MapStylePref
import io.github.lenticualrstone.civilight.model.app.SceneNotationPref
import io.github.lenticualrstone.civilight.model.app.TextMapEngine

/** One `애니메이션` radio (LIB-67). */
data class MotionOption(val pref: AnimationPref, val title: String, val sub: String?, val selected: Boolean)

/** 설정 · 움직임 · 투명도 (LIB-67, board `ReduceSettings`). */
data class MotionSettingsUi(
    val animation: List<MotionOption>,
    val reduceTransparency: SettingsSwitchRow,
    /** `라이프 레코더 빛`: the same value as 설정 · 라이프 레코더 `녹음 중 빛 띠` (Q-ST-04, `ED`). */
    val recorderGlow: SettingsSwitchRow,
)

/** The value rows of 설정 · 지도 · 이미지 · 연표 that open a picker (`… ›`). */
enum class MoreValue { MAP_STYLE, TERRAIN_ENGINE, LARGE_EXPORT, NOTATION }

/** A `title · sub · value ›` row of [MoreValue]. */
data class MoreValueRow(val value: MoreValue, val title: String, val sub: String, val current: String)

/** One choice in a value picker; [id] is the stored enum's name. */
data class ValueOption(val id: String, val title: String, val sub: String?, val selected: Boolean)

/** 설정 · 지도 · 이미지 · 연표 (LIB-68, CNV-19, WLD-67; board `SettingsMore`). */
data class MoreSettingsUi(
    val holo: SettingsSwitchRow,
    val presets: SettingsLinkRow,
    val mapStyle: MoreValueRow,
    val terrain: MoreValueRow,
    val largeExport: MoreValueRow,
    val notation: MoreValueRow,
    val checkTravel: SettingsSwitchRow,
    val checkNoPlace: SettingsSwitchRow,
) {
    /**
     * The 지도 section's rows as drawn in 0.2.0: `새 지도 그림` and `지형 만들기 · 글로 설명` are hidden with the
     * generated and sketch maps they set (scope S-3), so no visible row stores a value nothing reads.
     * Their stored values are kept.
     */
    val mapRows: List<MoreValueRow>
        get() = listOf(mapStyle, terrain, largeExport).filter { it.value !in DisplaySettingsState.HIDDEN_MAP_VALUES }
}

/**
 * Pure builders and the one reducer of the three display screens of 설정 (LIB-66 … LIB-68). Every
 * value is an [AppSettings] field; what it does belongs to its owner (CNV-33 layout, CNV-46 motion
 * and glass, CNV-30 holo, the map area, STO-17 and the travel check).
 */
object DisplaySettingsState {
    /** (0.2.0, S-3) Value rows not drawn: generated and sketch maps are hidden ([MoreSettingsUi.mapRows]). */
    val HIDDEN_MAP_VALUES: Set<MoreValue> = setOf(MoreValue.MAP_STYLE, MoreValue.TERRAIN_ENGINE)

    fun motion(settings: AppSettings): MotionSettingsUi = MotionSettingsUi(
        animation = listOf(
            MotionOption(AnimationPref.FOLLOW_SYSTEM, MotionSettingsCopy.FOLLOW, MotionSettingsCopy.FOLLOW_SUB, settings.animation == AnimationPref.FOLLOW_SYSTEM),
            MotionOption(AnimationPref.ALWAYS_REDUCE, MotionSettingsCopy.ALWAYS, MotionSettingsCopy.ALWAYS_SUB, settings.animation == AnimationPref.ALWAYS_REDUCE),
            MotionOption(AnimationPref.NEVER_REDUCE, MotionSettingsCopy.NEVER, null, settings.animation == AnimationPref.NEVER_REDUCE),
        ),
        reduceTransparency = SettingsSwitchRow(MotionSettingsCopy.REDUCE_TRANSPARENCY, MotionSettingsCopy.REDUCE_TRANSPARENCY_SUB, settings.reduceTransparency),
        recorderGlow = SettingsSwitchRow(MotionSettingsCopy.RECORDER_GLOW, MotionSettingsCopy.RECORDER_GLOW_SUB, settings.recorderGlow),
    )

    fun more(settings: AppSettings): MoreSettingsUi = MoreSettingsUi(
        holo = SettingsSwitchRow(MoreSettingsCopy.HOLO, MoreSettingsCopy.HOLO_SUB, settings.holoDefaultForNewImages),
        presets = SettingsLinkRow(MoreSettingsCopy.PRESETS, MoreSettingsCopy.PRESETS_SUB, MoreSettingsCopy.PRESETS_VALUE),
        mapStyle = MoreValueRow(MoreValue.MAP_STYLE, MoreSettingsCopy.MAP_STYLE, MoreSettingsCopy.MAP_STYLE_SUB, mapStyleLabel(settings.defaultMapStyle)),
        terrain = MoreValueRow(MoreValue.TERRAIN_ENGINE, MoreSettingsCopy.TERRAIN, MoreSettingsCopy.TERRAIN_SUB, engineLabel(settings.textMapEngine)),
        largeExport = MoreValueRow(MoreValue.LARGE_EXPORT, MoreSettingsCopy.LARGE_EXPORT, MoreSettingsCopy.LARGE_EXPORT_SUB, exportLabel(settings.largeMapExport)),
        notation = MoreValueRow(MoreValue.NOTATION, MoreSettingsCopy.NOTATION, MoreSettingsCopy.NOTATION_SUB, notationLabel(settings.defaultSceneNotation)),
        checkTravel = SettingsSwitchRow(MoreSettingsCopy.CHECK_TRAVEL, MoreSettingsCopy.CHECK_TRAVEL_SUB, settings.checkTravel),
        checkNoPlace = SettingsSwitchRow(MoreSettingsCopy.CHECK_NO_PLACE, MoreSettingsCopy.CHECK_NO_PLACE_SUB, settings.checkNoPlace),
    )

    /** The picker of a value row: every choice, in the order the row's sub lists them. */
    fun options(value: MoreValue, settings: AppSettings): List<ValueOption> = when (value) {
        MoreValue.MAP_STYLE -> MapStylePref.entries.map { ValueOption(it.name, mapStyleLabel(it), null, it == settings.defaultMapStyle) }
        MoreValue.TERRAIN_ENGINE -> TextMapEngine.entries.map {
            ValueOption(it.name, engineLabel(it), if (it == TextMapEngine.CLOUD) MoreSettingsCopy.ENGINE_CLOUD_SUB else null, it == settings.textMapEngine)
        }
        MoreValue.LARGE_EXPORT -> LargeMapExport.entries.map { ValueOption(it.name, exportLabel(it), null, it == settings.largeMapExport) }
        MoreValue.NOTATION -> SceneNotationPref.entries.map { ValueOption(it.name, notationLabel(it), null, it == settings.defaultSceneNotation) }
    }

    /** The row's title, which is also its picker's title. */
    fun title(value: MoreValue): String = when (value) {
        MoreValue.MAP_STYLE -> MoreSettingsCopy.MAP_STYLE
        MoreValue.TERRAIN_ENGINE -> MoreSettingsCopy.TERRAIN
        MoreValue.LARGE_EXPORT -> MoreSettingsCopy.LARGE_EXPORT
        MoreValue.NOTATION -> MoreSettingsCopy.NOTATION
    }

    /** Picking [optionId] for [value]; an unknown id changes nothing. */
    fun choose(settings: AppSettings, value: MoreValue, optionId: String): AppSettings = when (value) {
        MoreValue.MAP_STYLE -> MapStylePref.entries.firstOrNull { it.name == optionId }?.let { settings.copy(defaultMapStyle = it) }
        MoreValue.TERRAIN_ENGINE -> TextMapEngine.entries.firstOrNull { it.name == optionId }?.let { settings.copy(textMapEngine = it) }
        MoreValue.LARGE_EXPORT -> LargeMapExport.entries.firstOrNull { it.name == optionId }?.let { settings.copy(largeMapExport = it) }
        MoreValue.NOTATION -> SceneNotationPref.entries.firstOrNull { it.name == optionId }?.let { settings.copy(defaultSceneNotation = it) }
    } ?: settings

    fun mapStyleLabel(style: MapStylePref): String = when (style) {
        MapStylePref.SOFT -> MoreSettingsCopy.STYLE_SOFT
        MapStylePref.INK -> MoreSettingsCopy.STYLE_INK
        MapStylePref.COLOR -> MoreSettingsCopy.STYLE_COLOR
        MapStylePref.PARCHMENT -> MoreSettingsCopy.STYLE_PARCHMENT
    }

    fun engineLabel(engine: TextMapEngine): String = when (engine) {
        TextMapEngine.ON_DEVICE -> MoreSettingsCopy.ENGINE_DEVICE
        TextMapEngine.CLOUD -> MoreSettingsCopy.ENGINE_CLOUD
    }

    fun exportLabel(export: LargeMapExport): String = when (export) {
        LargeMapExport.K4 -> MoreSettingsCopy.EXPORT_4K
        LargeMapExport.K8 -> MoreSettingsCopy.EXPORT_8K
    }

    fun notationLabel(notation: SceneNotationPref): String = when (notation) {
        SceneNotationPref.EPISODE -> MoreSettingsCopy.NOTATION_EPISODE
        SceneNotationPref.STORY_START -> MoreSettingsCopy.NOTATION_STORY_START
    }
}
