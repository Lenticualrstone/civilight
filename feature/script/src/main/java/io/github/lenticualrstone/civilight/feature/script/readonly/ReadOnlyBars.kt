package io.github.lenticualrstone.civilight.feature.script.readonly

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ReadOnlyBanner
import io.github.lenticualrstone.civilight.designsystem.foundation.readingMeasure
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.glass.OnGlass
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.ExampleTour
import io.github.lenticualrstone.civilight.navigation.popToType
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps

/** Copy of the read-only example script (SCR-60, board `OnbExampleScript`). */
object ReadOnlyCopy {
    const val BANNER = "예시 프로젝트 · 읽기 전용"
    const val BANNER_SUB = "온보딩 후 삭제"
    const val NO_EDIT = "읽기 전용 · 편집 불가"
    const val EXAMPLE_HOME = "예시 홈으로"
    const val FINISH = "온보딩 끝내기"

    val all: List<String> = listOf(BANNER, BANNER_SUB, NO_EDIT, EXAMPLE_HOME, FINISH)
    val buttons: List<String> = listOf(EXAMPLE_HOME, FINISH)
}

/**
 * The read-only example's frame (SCR-60): a read-only project (only the onboarding example is one,
 * LIB-17) shows the banner header and `예시 홈으로 · 온보딩 끝내기` in place of the command field.
 * Both actions belong to the onboarding area (feature:library `ExampleTourContract`): `온보딩 끝내기`
 * sends `true` under [FINISH_REQUEST], then the stack returns to `예시 둘러보기`, which runs LIB-18.
 */
object ReadOnlyFrame {
    /** feature:library `ExampleTourContract.FINISH_REQUEST` (this module cannot import that lane). */
    const val FINISH_REQUEST = "library.onboarding.finish"

    /** The onboarding's step bars: four, at the last (예시 둘러보기 · 4/4). */
    const val STEPS = 4
    const val CURRENT_STEP = STEPS - 1

    /** Where `예시 홈으로` goes: back to the tour when it is under the script, else the tour is pushed. */
    sealed interface Home {
        data object PopToTour : Home

        data class PushTour(val route: ExampleTour) : Home
    }

    fun home(backStack: List<NavKey>, projectKey: Key): Home =
        if (backStack.any { it is ExampleTour && it.projectKey == projectKey }) Home.PopToTour else Home.PushTour(ExampleTour(projectKey))

    /** `예시 홈으로`. */
    fun goHome(deps: FeatureDeps, projectKey: Key) {
        when (val home = home(deps.navigator.backStack, projectKey)) {
            Home.PopToTour -> deps.navigator.popToType<ExampleTour>()
            is Home.PushTour -> deps.navigator.push(home.route)
        }
    }

    /** `온보딩 끝내기`: the request for the tour, then the tour (which consumes it). */
    fun finish(deps: FeatureDeps, projectKey: Key) {
        deps.results.send(FINISH_REQUEST, true)
        goHome(deps, projectKey)
    }
}

/** Header of the read-only example (130 dp): the floating banner and the position line under it. */
@Composable
fun ReadOnlyTopBar(line: String) {
    val inset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(Modifier.fillMaxWidth().heightIn(min = 130.dp + inset)) {
        GlassBackground(GlassSurface.TOP_BAR, Modifier.matchParentSize())
        OnGlass {
            Column(Modifier.fillMaxWidth().padding(top = inset + 16.dp, start = 12.dp, end = 12.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ReadOnlyBanner(
                    ReadOnlyCopy.BANNER,
                    Modifier.readingMeasure(),
                    subtitle = ReadOnlyCopy.BANNER_SUB,
                    stepCount = ReadOnlyFrame.STEPS,
                    currentStep = ReadOnlyFrame.CURRENT_STEP,
                )
                CivilightText(
                    line,
                    CivilightTheme.type.numberHeader,
                    Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp),
                    color = CivilightTheme.colors.ink3,
                    maxLines = 1,
                )
            }
        }
    }
}

/** In place of the command field: `읽기 전용 · 편집 불가` over `예시 홈으로` (ghost) · `온보딩 끝내기`. */
@Composable
fun ReadOnlyFooter(onHome: () -> Unit, onFinish: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CivilightText(
            ReadOnlyCopy.NO_EDIT,
            CivilightTheme.type.small,
            Modifier.fillMaxWidth(),
            color = CivilightTheme.colors.ink3,
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CivilightButton(ReadOnlyCopy.EXAMPLE_HOME, onHome, Modifier.weight(1f), variant = ButtonVariant.GHOST)
            CivilightButton(ReadOnlyCopy.FINISH, onFinish, Modifier.weight(1f))
        }
    }
}
