package io.github.lenticualrstone.civilight.feature.library.onboarding

import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.designsystem.component.BottomGlassBar
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.GlassScaffold
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.ReadOnlyBanner
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.foundation.readingMeasure
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.navigation.ExampleTour
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.project.ProjectViewModel
import io.github.lenticualrstone.civilight.ui.scaffold.ToastOverlay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * How other areas end onboarding from the example (script lane, `OnbExampleScript`
 * `온보딩 끝내기`): send `true` under [FINISH_REQUEST] through `deps.results`, then
 * `navigator.popToType<ExampleTour>()`. 예시 둘러보기 consumes it and runs LIB-18.
 */
object ExampleTourContract {
    const val FINISH_REQUEST = "library.onboarding.finish"
}

internal class ExampleTourViewModel(projectKey: io.github.lenticualrstone.civilight.model.Key, services: LibraryServices) :
    ProjectViewModel(projectKey, services.deps.projects) {
    val ui: StateFlow<ExampleTourUi?> = state
        .map { load -> (load as? ProjectLoad.Ready)?.let { ExampleTourState.build(it.doc) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

/**
 * 예시 둘러보기 · 4/4 (LIB-17, board `OnbExample`) over the read-only example: the floating glass
 * banner (4 step bars, lock, `예시 프로젝트 · 읽기 전용`, `온보딩 후 삭제`), the title and one-liner,
 * `둘러볼 곳` (script, 리엔, 오리지늄 등불), the chapter's episodes around 1-7, and `온보딩 끝내기`.
 */
@Composable
internal fun ExampleTourScreen(route: ExampleTour, services: LibraryServices) {
    val deps = services.deps
    val actions = services.onboarding
    val vm = entryViewModel { ExampleTourViewModel(route.projectKey, services) }
    val ui by vm.ui.collectAsState()
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    LaunchedEffect(route) { actions.recordResumeId(OnboardingFlow.EXAMPLE) }
    LaunchedEffect(route) {
        deps.results.results(ExampleTourContract.FINISH_REQUEST).collect {
            if (deps.results.consume(ExampleTourContract.FINISH_REQUEST) == true) actions.finish()
        }
    }
    GlassScaffold(
        topBar = {
            Box(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 16.dp), contentAlignment = Alignment.TopCenter) {
                ReadOnlyBanner(
                    OnboardingCopy.EXAMPLE_BANNER,
                    Modifier.readingMeasure(),
                    subtitle = OnboardingCopy.EXAMPLE_BANNER_SUB,
                    stepCount = OnboardingFlow.SETUP_STEPS,
                    currentStep = OnboardingFlow.SETUP_STEPS - 1,
                )
            }
        },
        bottomBar = {
            BottomGlassBar(barHeight = 120.dp) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CivilightButton(OnboardingCopy.FINISH_ONBOARDING, actions::finish, Modifier.readingMeasure().fillMaxWidth(), variant = ButtonVariant.PRIMARY_LARGE)
                }
            }
        },
        overlay = { padding -> ToastOverlay(bottom = padding.calculateBottomPadding()) },
    ) { padding ->
      // Loading → content (audit row 7).
      ContentSwap(ui != null, Modifier.fillMaxSize(), label = "exampleTour") { ready ->
        val current = ui
        if (!ready || current == null) return@ContentSwap
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .readingMeasure()
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding() + 40.dp, bottom = padding.calculateBottomPadding() + 16.dp),
            ) {
                CivilightText(current.title, type.displayM, color = colors.ink)
                current.oneLiner?.let { CivilightText(it, type.leadCompact, Modifier.padding(top = 10.dp), color = colors.ink2) }
                if (current.explore.isNotEmpty()) {
                    SectionLabel(OnboardingCopy.EXPLORE, Modifier.padding(top = 24.dp, bottom = 6.dp))
                    current.explore.forEach { row ->
                        ListRow(
                            title = row.title,
                            subtitle = row.sub,
                            onClick = {
                                if (row.isScript) actions.recordResumeId(OnboardingFlow.EXAMPLE_SCRIPT)
                                deps.navigator.push(row.route)
                            },
                            trailing = { CivilightIcon(IconId.Next, contentDescription = null, size = 16.dp, tint = colors.ink3) },
                        )
                    }
                }
                current.chapterHeader?.let { header ->
                    SectionLabel(header, Modifier.padding(top = 22.dp, bottom = 6.dp))
                    current.episodes.forEach { e -> EpisodeLine(e) }
                }
            }
        }
      }
    }
}

/** `1-7 | 잿빛 새벽의 등불 | 씬 4개` (mono code, title in `ink2`, small status). */
@Composable
private fun EpisodeLine(row: TourEpisodeRow) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        Modifier.fillMaxWidth().topHairline(colors.line).padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CivilightText(row.code, type.number, Modifier.width(32.dp), color = colors.ink3, maxLines = 1)
        CivilightText(row.title, type.leadCompact, Modifier.weight(1f), color = colors.ink2, maxLines = 1)
        CivilightText(row.status, type.small, color = colors.ink3, maxLines = 1)
    }
}
