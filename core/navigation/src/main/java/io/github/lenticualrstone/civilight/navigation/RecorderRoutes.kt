package io.github.lenticualrstone.civilight.navigation

import io.github.lenticualrstone.civilight.model.Key
import kotlinx.serialization.Serializable

/**
 * 기록함 (LRC-25 … LRC-30, LRC-40/41); inner `TwoLr` list pane. [filter] = selected chip id.
 *
 * The opening context (`domain:recorder` `InboxOpening`, LRC-27 presets):
 * - nothing set = `FromLibrary` (no filter);
 * - [projectKey] only = `FromProject` (this project + `정리 안 됨`: LIB-13 checklist, empty cast
 *   `라이프 레코더에서`);
 * - [projectKey] + [targetKind] + [targetKey] = `FromTarget` (`레코더 · 정리 안 됨 n ›` on a scene,
 *   card or profile, CST-49). [targetKind] uses the strings of [Recording.targetKind].
 *
 * Not a [ProjectRoute]: the inbox spans projects and is never wrapped in a project frame.
 */
@Serializable
data class RecorderInbox(
    val filter: String? = null,
    val projectKey: Key? = null,
    val targetKind: String? = null,
    val targetKey: Key? = null,
) : RecorderRoute, ListPaneRoute

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
