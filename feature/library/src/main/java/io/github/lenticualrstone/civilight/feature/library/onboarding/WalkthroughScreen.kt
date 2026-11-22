package io.github.lenticualrstone.civilight.feature.library.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.navigation.Walkthrough

/**
 * 워크스루 3장 (LIB-16, boards `Walk1` … `Walk3`): 3-bar progress and `건너뛰기`, the live demo card
 * (420 high, radius 24) with `예시 · 읽기 전용 · 온보딩 후 삭제` on its lower edge, mono `k/3`, title,
 * sub, then `다음` (page 1) or `이전` · `다음` / `예시 둘러보기`. `건너뛰기` and `예시 둘러보기` lead to
 * 예시 둘러보기 (LIB-17). As a replay from 도움말 (LIB-21) it ends back where it was opened.
 */
@Composable
internal fun WalkthroughScreen(route: Walkthrough, services: LibraryServices) {
    val actions = services.onboarding
    val state by actions.state.collectAsState()
    val page = route.page.coerceIn(1, OnboardingFlow.WALK_PAGES)
    val replay = OnboardingFlow.isReplay(state)
    LaunchedEffect(route) { actions.recordResume(route) }
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type

    fun end(target: OnbTarget) {
        if (target !is OnbTarget.Go || target.route !is Walkthrough) actions.markWalkthroughSeen()
        actions.follow(target)
    }

    OnbFrame(
        top = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WalkDots(page)
                Box(Modifier.weight(1f))
                TextAction(OnboardingCopy.SKIP, { end(OnboardingFlow.walkSkip(replay, actions.exampleKey())) })
            }
        },
        buttons = {
            OnboardingFlow.walkPrevious(page)?.let { previous ->
                CivilightButton(OnboardingCopy.PREVIOUS, { actions.back(previous) }, Modifier.weight(1f), variant = ButtonVariant.GHOST)
            }
            CivilightButton(
                OnboardingFlow.walkPrimaryLabel(page, replay),
                { end(OnboardingFlow.walkNext(page, replay, actions.exampleKey())) },
                Modifier.weight(2f),
                variant = ButtonVariant.PRIMARY_LARGE,
            )
        },
    ) {
        Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            WalkDemo(page, Modifier.padding(horizontal = 0.dp))
            if (OnboardingFlow.walkShowsBanner(replay)) {
                val shape = RoundedCornerShape(10.dp)
                Box(Modifier.align(Alignment.BottomCenter).offset(y = (-30).dp)) {
                    CivilightText(
                        OnboardingCopy.WALK_BANNER,
                        type.micro,
                        Modifier.clip(shape).background(colors.raise).insetOutline(colors.line, 1.dp, shape).padding(horizontal = 10.dp, vertical = 4.dp),
                        color = colors.ink2,
                        maxLines = 1,
                    )
                }
            }
        }
        Column(Modifier.padding(top = 30.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CivilightText("$page/${OnboardingFlow.WALK_PAGES}", type.number.merge(TextStyle(fontSize = 11.sp)), color = colors.ink3)
            CivilightText(titleOf(page), type.pageTitle, color = colors.ink)
            CivilightText(subOf(page), type.command, color = colors.ink3)
        }
    }
}

private fun titleOf(page: Int): String = when (page) {
    1 -> OnboardingCopy.WALK_1_TITLE
    2 -> OnboardingCopy.WALK_2_TITLE
    else -> OnboardingCopy.WALK_3_TITLE
}

private fun subOf(page: Int): String = when (page) {
    1 -> OnboardingCopy.WALK_1_SUB
    2 -> OnboardingCopy.WALK_2_SUB
    else -> OnboardingCopy.WALK_3_SUB
}
