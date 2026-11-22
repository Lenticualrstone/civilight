package io.github.lenticualrstone.civilight.feature.recorder

import io.github.lenticualrstone.civilight.navigation.SheetKey
import kotlinx.serialization.Serializable

/**
 * The link picker (LRC-20) opened from the shared `TextMemoSheet` over another area's screen. It is
 * registered in the shared registry too, because a sheet stacked from a shared sheet resolves
 * against the host screen's local sheets, which belong to another area. What it edits is held by
 * the capture hub (`PickerRequest`).
 */
@Serializable
data class RecorderLinkPickerSheet(val forMemo: Boolean = true) : SheetKey
