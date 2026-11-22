package io.github.lenticualrstone.civilight.navigation

import io.github.lenticualrstone.civilight.model.Key
import kotlinx.serialization.Serializable

/**
 * 스크립트 (SCR-01 … SCR-69). No lens (R11 §3, STO-16): entering stores the lens, leaving applies
 * `렌즈를 이 씬으로 맞추기` if it was used. Opens at [blockKey] (caret at its end when
 * [caretAtEnd], `이어 쓰기`), else at the head of [sceneKey], else at the last writing position.
 * The read-only example shows the `예시 프로젝트 · 읽기 전용` banner (SCR-60).
 */
@Serializable
data class Script(
    override val projectKey: Key,
    val sceneKey: Key? = null,
    val blockKey: Key? = null,
    val caretAtEnd: Boolean = false,
) : ScriptRoute, ProjectRoute

/** 에셋 list (SCR-61). */
@Serializable
data class ScriptAssets(override val projectKey: Key) : ScriptRoute, ProjectRoute

/** 프로젝트 설정 › 스크립트 추가 칸: `무대 위치 · 보이스 ID · ID 형식` (SCR-24, SCR-25). */
@Serializable
data class ScriptFieldSettings(override val projectKey: Key) : ScriptRoute, ProjectRoute

/** 프로젝트 설정 › 전환 문구 (SCR-48 project default). */
@Serializable
data class TransitionTextSettings(override val projectKey: Key) : ScriptRoute, ProjectRoute
