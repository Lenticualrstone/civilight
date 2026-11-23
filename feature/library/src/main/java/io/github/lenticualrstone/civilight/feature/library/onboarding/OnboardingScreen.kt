package io.github.lenticualrstone.civilight.feature.library.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.ChoiceCard
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightSwitch
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ThemeSwatch
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.readingMeasure
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ThemeMode
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsViewModel
import io.github.lenticualrstone.civilight.feature.library.settings.TextSizeControl
import io.github.lenticualrstone.civilight.navigation.Onboarding
import io.github.lenticualrstone.civilight.navigation.OnboardingPage
import io.github.lenticualrstone.civilight.navigation.RestoreBackup
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import kotlinx.coroutines.launch

/** 첫 실행 설정 (LIB-15): 환영 and the three setup pages, one route per page. */
@Composable
internal fun OnboardingScreen(route: Onboarding, services: LibraryServices) {
    val actions = services.onboarding
    LaunchedEffect(route) { actions.recordResume(route) }
    val page = route.page
    val next = { actions.go(OnboardingFlow.next(page)) }
    val skip = { actions.go(OnboardingFlow.skip()) }
    when (page) {
        OnboardingPage.WELCOME -> WelcomePage(onStart = next, onRestore = { services.deps.navigator.push(RestoreBackup) })
        OnboardingPage.LOOK -> LookPage(services, onNext = next, onSkip = skip)
        OnboardingPage.SAFE -> SafePage(services, onNext = next, onSkip = skip)
        OnboardingPage.VOICE -> VoicePage(onNext = next, onSkip = skip)
    }
}

/**
 * `OnbWelcome`: the sample manuscript blurred behind glass (masked from transparent at the top),
 * mono `CIVILIGHT`, `문명의 빛`, the two content lines, the small line and the two buttons.
 */
@Composable
private fun WelcomePage(onStart: () -> Unit, onRestore: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Box(Modifier.fillMaxSize().background(colors.bg)) {
        BlurredManuscript(Modifier.fillMaxSize().padding(top = 120.dp))
        // The glass wash: transparent at the top, full by 45 % of the height (board mask).
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to colors.bg.copy(alpha = 0f), 0.45f to colors.bg.copy(alpha = 0.78f), 1f to colors.bg.copy(alpha = 0.92f)),
            ),
        )
        Column(
            Modifier.align(Alignment.BottomCenter).readingMeasure().fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 26.dp),
        ) {
            Column(Modifier.padding(horizontal = 12.dp)) {
                CivilightText(OnboardingCopy.WORDMARK, type.wordmarkWelcome, color = colors.ink3)
                CivilightText(OnboardingCopy.WELCOME_TITLE, type.hero, Modifier.padding(top = 10.dp), color = colors.ink)
                CivilightText(OnboardingSample.WELCOME_LINE_1, type.lead, Modifier.padding(top = 12.dp), color = colors.ink2)
                CivilightText(OnboardingSample.WELCOME_LINE_2, type.lead, color = colors.ink2)
                CivilightText(OnboardingCopy.WELCOME_SMALL, type.small, Modifier.padding(top = 18.dp), color = colors.ink3)
            }
            Column(Modifier.padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CivilightButton(OnboardingCopy.START, onStart, Modifier.fillMaxWidth(), variant = ButtonVariant.PRIMARY_LARGE)
                CivilightButton(OnboardingCopy.RESTORE_FROM_BACKUP, onRestore, Modifier.fillMaxWidth(), variant = ButtonVariant.GHOST)
            }
        }
    }
}

/** The example manuscript as decoration (opacity .55, 1.5 dp blur; no semantics). */
@Composable
private fun BlurredManuscript(modifier: Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(modifier.alpha(0.55f).blur(1.5.dp), verticalArrangement = Arrangement.spacedBy(26.dp)) {
        OnboardingSample.MANUSCRIPT.forEach { (who, line) ->
            Column(Modifier.padding(horizontal = 24.dp)) {
                if (who != null) CivilightText(who, type.speaker, color = colors.ink2)
                CivilightText(line, if (who == null) type.dir else type.line, color = if (who == null) colors.ink3 else colors.ink)
            }
        }
    }
}

/** `OnbLook` (화면 · 1/4): three theme swatches (applied live) and the text-size slider with its preview line. */
@Composable
private fun LookPage(services: LibraryServices, onNext: () -> Unit, onSkip: () -> Unit) {
    val vm = entryViewModel { SettingsViewModel(services) }
    val ui by vm.textSize.collectAsState()
    val type = CivilightTheme.type
    val colors = CivilightTheme.colors
    OnbFrame(
        top = { SetupTopRow(current = 0, onSkip = onSkip) },
        buttons = { CivilightButton(OnboardingCopy.NEXT, onNext, Modifier.weight(1f), variant = ButtonVariant.PRIMARY_LARGE) },
    ) {
        SetupHeading(1, OnboardingCopy.LOOK_TITLE)
        Row(Modifier.padding(top = 22.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                ThemeMode.SYSTEM to OnboardingCopy.THEME_SYSTEM,
                ThemeMode.LIGHT to OnboardingCopy.THEME_LIGHT,
                ThemeMode.DARK to OnboardingCopy.THEME_DARK,
            ).forEachIndexed { index, (mode, label) ->
                ThemeSwatch(mode, label, selected = ui.themeIndex == index, onClick = { vm.setTheme(index) }, modifier = Modifier.weight(1f), height = 120.dp)
            }
        }
        CivilightText(OnboardingCopy.TEXT_SIZE, type.sec, Modifier.padding(top = 26.dp, bottom = 10.dp), color = colors.ink3)
        TextSizeControl(ui, vm::setTextSlider, showSizeLabel = false)
    }
}

/**
 * `OnbSafe` (백업과 잠금 · 2/4): `Google Drive 자동 백업` with `연결` (sign-in is a stub until
 * LIB-57) and `앱 잠금` · `열 때 지문` with the same rule as 설정 (LIB-35: on needs a device lock,
 * off asks first). Default off until chosen (§6 Q13).
 */
@Composable
private fun SafePage(services: LibraryServices, onNext: () -> Unit, onSkip: () -> Unit) {
    val vm = entryViewModel { SettingsViewModel(services) }
    val settings by services.deps.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    OnbFrame(
        top = { SetupTopRow(current = 1, onSkip = onSkip) },
        buttons = { LaterNextButtons(onLater = onNext, onNext = onNext) },
    ) {
        SetupHeading(2, OnboardingCopy.SAFE_TITLE)
        Column(Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SafeCard(IconId.Drive, OnboardingCopy.DRIVE_TITLE, OnboardingCopy.DRIVE_SUB) {
                CivilightButton(
                    OnboardingCopy.CONNECT,
                    onClick = {
                        scope.launch {
                            when (services.backups.connect()) {
                                is DriveConnect.Connected -> Unit
                                DriveConnect.Cancelled -> Unit
                                DriveConnect.Unavailable -> services.deps.toasts.show(OnboardingCopy.DRIVE_NOT_READY, actionLabel = null)
                            }
                        }
                    },
                    variant = ButtonVariant.COMPACT_PRIMARY,
                    enabled = !settings.driveBackup,
                )
            }
            SafeCard(IconId.Lock, OnboardingCopy.APP_LOCK_TITLE, OnboardingCopy.APP_LOCK_SUB) {
                CivilightSwitch(settings.appLock, onCheckedChange = vm::setAppLock)
            }
        }
        CivilightText(OnboardingCopy.SAFE_NOTE, type.small, Modifier.padding(top = 8.dp), color = colors.ink3)
    }
}

/** One `OnbSafe` card: 40 dp icon tile, title (700 16 / 22), small sub, trailing control. */
@Composable
private fun SafeCard(icon: IconId, title: String, sub: String, trailing: @Composable () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().insetOutline(colors.line, 1.dp, shape).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.tint), contentAlignment = Alignment.Center) {
            CivilightIcon(icon, contentDescription = null, size = 18.dp, tint = colors.ink)
        }
        Column(Modifier.weight(1f)) {
            CivilightText(title, type.itemTitle, color = colors.ink)
            CivilightText(sub, type.small, color = colors.ink3)
        }
        trailing()
    }
}

/**
 * `OnbVoice` (라이프 레코더 · 3/4): the recorder card, then the two choices. `마이크 허용` shows its
 * reason (`녹음할 때만 사용`) on the card before the system prompt opens; it reads as chosen once the
 * device says so. (0.2.0 S-8) The `홈 화면 위젯 추가` choice is gone with the widget.
 */
@Composable
private fun VoicePage(onNext: () -> Unit, onSkip: () -> Unit) {
    val context = LocalContext.current
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    var micGranted by remember { mutableStateOf(DeviceChoices.micGranted(context)) }
    val micPrompt = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> micGranted = granted }
    OnbFrame(
        top = { SetupTopRow(current = 2, onSkip = onSkip) },
        buttons = { LaterNextButtons(onLater = onNext, onNext = onNext) },
    ) {
        SetupHeading(3, OnboardingCopy.VOICE_TITLE)
        val shape = RoundedCornerShape(18.dp)
        Column(Modifier.padding(top = 22.dp).fillMaxWidth().insetOutline(colors.line, 1.dp, shape).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).background(colors.pri), contentAlignment = Alignment.Center) {
                    CivilightIcon(IconId.Mic, contentDescription = null, size = 18.dp, tint = colors.onpri)
                }
                Column {
                    CivilightText(OnboardingCopy.RECORDER_CARD_TITLE, type.itemTitle, color = colors.ink)
                    CivilightText(OnboardingCopy.RECORDER_CARD_SUB, type.small, color = colors.ink3)
                }
            }
            CivilightText(OnboardingCopy.RECORDER_FLOW, type.leadCompact, Modifier.padding(top = 12.dp), color = colors.ink2)
        }
        Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceCard(
                OnboardingCopy.MIC_TITLE,
                selected = micGranted,
                onClick = { if (!micGranted) micPrompt.launch(DeviceChoices.MIC_PERMISSION) },
                subtitle = OnboardingCopy.MIC_SUB,
            )
            // (0.2.0 S-8, LIB-95) No `홈 화면 위젯 추가`: the recorder widget is not declared any more.
        }
    }
}

