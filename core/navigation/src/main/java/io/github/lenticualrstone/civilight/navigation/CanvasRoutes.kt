package io.github.lenticualrstone.civilight.navigation

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import kotlinx.serialization.Serializable

/** 캔버스 (CNV-01 … CNV-12); [canvasKey] null = the project's first canvas. */
@Serializable
data class CanvasBoard(override val projectKey: Key, val canvasKey: Key? = null) : CanvasRoute, ProjectRoute, LensScreen

/** 페이지 목록 (CNV-14). */
@Serializable
data class PageList(override val projectKey: Key) : CanvasRoute, ProjectRoute

/** 페이지 (CNV-13, CNV-15, CNV-16). */
@Serializable
data class PageDoc(override val projectKey: Key, val pageKey: Key) : CanvasRoute, ProjectRoute

/** 자르기 (CNV-28) of [blobHash] for the image slot of an owner. */
@Serializable
data class ImageCrop(
    override val projectKey: Key,
    val ownerKind: ImageOwnerKind,
    val ownerKey: Key,
    val slot: ImageSlot,
    val blobHash: String,
) : CanvasRoute, ProjectRoute

/** 이미지 규격 (CNV-25). */
@Serializable
data object ImageSpecs : CanvasRoute

/** 기본 이미지 8종 (CNV-26, `설정 · 지도 · 이미지 · 연표 › 기본 이미지 · 보기 ›`). */
@Serializable
data object PresetImages : CanvasRoute

/** 홀로 실험 (CNV-31, P2). */
@Serializable
data object HoloExperiment : CanvasRoute
