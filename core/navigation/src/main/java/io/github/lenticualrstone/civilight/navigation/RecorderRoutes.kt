package io.github.lenticualrstone.civilight.navigation

import io.github.lenticualrstone.civilight.model.Key
import kotlinx.serialization.Serializable

/** 기록함 (LRC-25 … LRC-30, LRC-40/41); inner `TwoLr` list pane. [filter] = selected chip id. */
@Serializable
data class RecorderInbox(val filter: String? = null) : RecorderRoute, ListPaneRoute

/** 기록 화면 (LRC-28), inner `TwoLr` detail pane. */
@Serializable
data class RecorderRecordDetail(val recordKey: Key) : RecorderRoute, DetailPaneRoute

/**
 * 녹음 중 (LRC-07) → 정리 중 (LRC-13), incl. 마이크 꺼짐 (LRC-06); full-window glass over the
 * previous screen. The entry point context is flattened into simple values (LRC-01 … LRC-05):
 * [surface] names where it started (`commandBar`, `inbox`, `widget`, `tile`, `onboarding` …).
 */
@Serializable
data class Recording(
    val surface: String = "commandBar",
    val projectKey: Key? = null,
    val targetKind: String? = null,
    val targetKey: Key? = null,
    val caretBlockKey: Key? = null,
    val startTyped: Boolean = false,
) : RecorderRoute, OverlayRoute

/** 설정 · 라이프 레코더 (LRC-44, `LrSettings`). */
@Serializable
data object RecorderSettings : RecorderRoute
