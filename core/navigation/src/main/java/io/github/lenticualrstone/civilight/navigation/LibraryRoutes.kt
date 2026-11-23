package io.github.lenticualrstone.civilight.navigation

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.project.Rating
import kotlinx.serialization.Serializable

/** 서재 (LIB-01, LIB-04, LIB-06). On inner screens its detail pane is [ProjectHome]. */
@Serializable
data object Library : LibraryRoute, ListPaneRoute

/** 첫 실행 설정 pages (LIB-15): `OnbWelcome`, `OnbLook`, `OnbSafe`, `OnbVoice`. */
@Serializable
data class Onboarding(val page: OnboardingPage = OnboardingPage.WELCOME) : LibraryRoute

@Serializable
enum class OnboardingPage { WELCOME, LOOK, SAFE, VOICE }

/** 워크스루 3장 (LIB-16), [page] 1…3. */
@Serializable
data class Walkthrough(val page: Int = 1) : LibraryRoute

/** 예시 둘러보기 · 4/4 (LIB-17 `OnbExample`) over the read-only example project. */
@Serializable
data class ExampleTour(override val projectKey: Key) : LibraryRoute, ProjectRoute

/** 새 프로젝트 1/4 … 4/4 (LIB-09 … LIB-12, LIB-14). Values persist while moving between steps. */
@Serializable
data class NewProject(val step: NewProjectStep = NewProjectStep.NAME) : LibraryRoute

@Serializable
enum class NewProjectStep { NAME, STRUCTURE, TIME, START }

/** 설정 root (LIB-63, CNV-18). */
@Serializable
data object Settings : LibraryRoute

/** 화면 · 글자 크기 (LIB-65). */
@Serializable
data object SettingsTextSize : LibraryRoute

/** 설정 · 펼친 화면 배치 (LIB-66, CNV-33). */
@Serializable
data object SettingsLayout : LibraryRoute

/** 설정 · 움직임 · 투명도 (LIB-67, CNV-46). */
@Serializable
data object SettingsMotion : LibraryRoute

/** 설정 · 지도 · 이미지 · 연표 (LIB-68, CNV-19). */
@Serializable
data object SettingsMore : LibraryRoute

/** (0.2.0, U-16, LIB-94) 설정 › 화면 › 서재 보기: the 3D shelf style list. */
@Serializable
data object SettingsLibraryView : LibraryRoute

/** (0.2.0, U-16, LIB-94) 설정 › 화면 › 카드 보기: the character card style list. */
@Serializable
data object SettingsCardView : LibraryRoute

/** 설정 · 숨기기 (LIB-64). */
@Serializable
data object SettingsHiding : LibraryRoute

/** 설정 · 수위 등급 (LIB-23). */
@Serializable
data object SettingsRating : LibraryRoute

/** `이미 있는 <rating> 프로젝트 · 같이 바꾸기` list (LIB-23). */
@Serializable
data class RatingDefaultsApply(val rating: Rating) : LibraryRoute

/** 설정 › 보관 (LIB-61, LIB-62). */
@Serializable
data object SettingsBackup : LibraryRoute

/** 복구 암호 정하기 (LIB-58). */
@Serializable
data object RecoveryPassword : LibraryRoute

/** 도움말 · 안내 다시 보기 (LIB-21). */
@Serializable
data object HelpReplay : LibraryRoute

/** Licence texts shipped with the app (Pretendard OFL). */
@Serializable
data object Licenses : LibraryRoute

/** 휴지통 (LIB-38), app-wide, with the chosen filter chip. */
@Serializable
data class Trash(val filter: TrashFilter = TrashFilter.ALL) : LibraryRoute

/** Filter chips of 휴지통: `전체 · 프로젝트 · 씬 · 블록 · 인물 · 세계 · 변화 · 레코더`. */
@Serializable
enum class TrashFilter { ALL, PROJECT, SCENE, BLOCK, CHARACTER, WORLD, CHANGE, RECORDER }

/** 백업에서 되살리기 (LIB-59). */
@Serializable
data object RestoreBackup : LibraryRoute

/** 되살리는 중 (LIB-60). */
@Serializable
data object Restoring : LibraryRoute

/** 파일에서 열기 (LIB-52) incl. 새 버전 파일 (LIB-53), 암호 파일 (LIB-54), partial import (LIB-55). */
@Serializable
data class OpenFile(val uri: String) : LibraryRoute

/** 프로젝트 설정 container (LIB-69, LIB-25, CNV-20). */
@Serializable
data class ProjectSettings(override val projectKey: Key) : LibraryRoute, ProjectRoute

/** 단계 이름 editor (LIB-69 `구조`, STO-02). */
@Serializable
data class StructureLevels(override val projectKey: Key) : LibraryRoute, ProjectRoute
