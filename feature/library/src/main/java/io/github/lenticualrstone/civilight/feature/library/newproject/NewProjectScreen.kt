package io.github.lenticualrstone.civilight.feature.library.newproject

import io.github.lenticualrstone.civilight.designsystem.component.DelayedProgress
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import io.github.lenticualrstone.civilight.model.project.FieldBundle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.BottomGlassBar
import io.github.lenticualrstone.civilight.designsystem.component.ButtonRow
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.GlassScaffold
import io.github.lenticualrstone.civilight.designsystem.component.StepBars
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.common.sheetBody
import io.github.lenticualrstone.civilight.navigation.NewProject
import io.github.lenticualrstone.civilight.navigation.NewProjectStep
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHostLayout
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout

/**
 * One step of 새 프로젝트 (LIB-09 … LIB-12). Each step is its own back-stack entry; the values live in
 * the shared draft, so `이전` (a pop) and `다음` (a push) keep them. Cover: full page with the 4-bar
 * progress, small `새 프로젝트 · k/4`, title, fields, glass bottom bar. Inner (`NewWide`, LIB-14):
 * form column 380 with `취소` beside the progress and a 300-wide `미리 보기` aside.
 */
@Composable
internal fun NewProjectScreen(route: NewProject, services: LibraryServices) {
    val deps = services.deps
    val vm = entryViewModel { NewProjectViewModel(services) }
    var snapshot by rememberSaveable { mutableStateOf<String?>(null) }
    remember { services.draft.restore(snapshot) }
    val form by services.draft.form.collectAsState()
    LaunchedEffect(form) { snapshot = NewProjectDraftHolder.encode(form) }
    val settings by deps.settings.collectAsState()
    val inputs by vm.inputs.collectAsState()
    val creating by vm.creating.collectAsState()
    val step = route.step
    val navigator = deps.navigator
    val sheets = rememberSheetHost()
    val inner = LocalWindowLayout.current.isInner
    fun update(transform: (NewProjectForm) -> NewProjectForm) = services.draft.update(transform)

    fun cancel() {
        services.draft.reset()
        navigator.popTo { it !is NewProject }
    }

    fun back() {
        if (step == NewProjectStep.NAME) cancel() else navigator.pop()
    }

    fun next() {
        val following = NewProjectRules.next(step)
        if (following != null) {
            navigator.push(NewProject(following))
        } else {
            vm.create(form) { stack ->
                services.draft.reset()
                navigator.resetTo(stack)
            }
        }
    }

    // Step 1's R-18 block says whether a 복구 암호 exists (batch 5: the backup book, loaded here).
    val backupBook by services.backups.book.state.collectAsState()
    LaunchedEffect(Unit) { services.backups.book.ensureLoaded() }
    val enabled = NewProjectRules.canProceed(form, step) && !creating
    val leftLabel = if (step == NewProjectStep.NAME) NewProjectCopy.CANCEL else NewProjectCopy.PREVIOUS
    val rightLabel = if (step == NewProjectStep.START) NewProjectCopy.CREATE else NewProjectCopy.NEXT
    val body: @Composable ColumnScope.() -> Unit = {
        when (step) {
            NewProjectStep.NAME -> NameStep(form, settings.ratingDefaults, backupBook.recovery != null, ::update)
            NewProjectStep.STRUCTURE -> StructureStep(form, wide = inner, showPreview = !inner, update = ::update)
            NewProjectStep.TIME -> TimeStep(form, ::update)
            NewProjectStep.START -> StartStep(
                form = form,
                hideSwitch = NewProjectRules.hideSwitch(form, settings.ratingDefaults),
                inputs = inputs,
                update = ::update,
                onPickBundle = { sheets.open(BundlePickerSheet) },
            )
        }
    }

    SheetHostLayout(
        sheets,
        localSheets = { key ->
            when (key) {
                is BundlePickerSheet -> sheetBody {
                    BundlePickerContent(
                        form, inputs.bundleSources,
                        onPick = { bundle, source -> update { it.copy(bundle = bundle, bundleSourceKey = source, bundleTemplateKey = null) } },
                        templates = inputs.visibleTemplates,
                        onPickTemplate = { key -> update { it.copy(bundle = FieldBundle.TEMPLATE, bundleSourceKey = null, bundleTemplateKey = key) } },
                    )
                }
                else -> null
            }
        },
    ) {
        if (inner) {
            WideFrame(step, form, enabled, leftLabel, rightLabel, ::back, ::next, ::cancel, creating, body)
        } else {
            CoverFrame(step, enabled, leftLabel, rightLabel, ::back, ::next, creating, body)
        }
    }
}

@Composable
private fun StepHeading(step: NewProjectStep) {
    val type = CivilightTheme.type
    CivilightText(NewProjectRules.smallLine(step), type.sec, color = CivilightTheme.colors.ink3)
    CivilightText(NewProjectRules.title(step), type.pageTitle, Modifier.padding(top = 4.dp), color = CivilightTheme.colors.ink)
}

@Composable
private fun StepButtons(enabled: Boolean, leftLabel: String, rightLabel: String, onLeft: () -> Unit, onRight: () -> Unit, modifier: Modifier = Modifier, working: Boolean = false) {
    Column(modifier.fillMaxWidth()) {
        // `만들기` can take a while: after a second a small line says so (DS-47), and the buttons stay off.
        DelayedProgress(working, Modifier.padding(bottom = 6.dp)) {
            CivilightText(NewProjectCopy.WORKING, CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
        }
        ButtonRow(Modifier.fillMaxWidth()) {
            CivilightButton(leftLabel, onLeft, Modifier.weight(1f), variant = ButtonVariant.GHOST)
            CivilightButton(rightLabel, onRight, Modifier.weight(1f), variant = ButtonVariant.PRIMARY_LARGE, enabled = enabled)
        }
    }
}

@Composable
private fun CoverFrame(
    step: NewProjectStep,
    enabled: Boolean,
    leftLabel: String,
    rightLabel: String,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    working: Boolean,
    body: @Composable ColumnScope.() -> Unit,
) {
    val colors = CivilightTheme.colors
    GlassScaffold(
        topBar = {
            Box(Modifier.fillMaxWidth().background(colors.bg).statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp)) {
                StepBars(NewProjectRules.STEPS, NewProjectRules.index(step), Modifier.fillMaxWidth())
            }
        },
        bottomBar = {
            BottomGlassBar(barHeight = 120.dp) { StepButtons(enabled, leftLabel, rightLabel, onLeft, onRight, working = working) }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding() + 18.dp, bottom = padding.calculateBottomPadding() + 16.dp),
        ) {
            StepHeading(step)
            Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = body)
        }
    }
}

@Composable
private fun WideFrame(
    step: NewProjectStep,
    form: NewProjectForm,
    enabled: Boolean,
    leftLabel: String,
    rightLabel: String,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onCancel: () -> Unit,
    working: Boolean,
    body: @Composable ColumnScope.() -> Unit,
) {
    val colors = CivilightTheme.colors
    // Board `NewWide` (800 wide): the progress row spans both columns with `취소` at its end; the
    // 380 form column sits at the start and the 300 `미리 보기` aside at the end, stretching down to
    // the buttons; `이전 · 다음` (380 wide) sit at the bottom end. Wider windows centre the 800 frame.
    // The buttons ride on the keyboard (or the navigation bar), consumption-aware.
    Box(
        Modifier.fillMaxSize().background(colors.bg).statusBarsPadding().windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = 800.dp).fillMaxWidth().fillMaxHeight().padding(start = 40.dp, end = 40.dp, top = 28.dp, bottom = 26.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StepBars(NewProjectRules.STEPS, NewProjectRules.index(step), Modifier.weight(1f))
                TextAction(NewProjectCopy.CANCEL, onCancel)
            }
            Row(Modifier.weight(1f).fillMaxWidth().padding(top = 26.dp, bottom = 22.dp)) {
                Column(Modifier.width(380.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    StepHeading(step)
                    Column(Modifier.padding(top = 22.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = body)
                }
                Spacer(Modifier.weight(1f).widthIn(min = 24.dp))
                PreviewAside(form, Modifier.width(300.dp).fillMaxHeight())
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                StepButtons(enabled, leftLabel, rightLabel, onLeft, onRight, Modifier.width(380.dp), working)
            }
        }
    }
}

/** `NewWide` aside (padding 22, radius 22, `raise`): `미리 보기`, the name, the format, the live tree, `실시간 미리 보기`. */
@Composable
private fun PreviewAside(form: NewProjectForm, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(22.dp)
    Column(modifier.clip(shape).background(colors.raise).insetOutline(colors.line, 1.dp, shape).verticalScroll(rememberScrollState()).padding(22.dp)) {
        CivilightText(NewProjectCopy.PREVIEW, type.sec, color = colors.ink3)
        if (form.name.isNotBlank()) CivilightText(form.name.trim(), type.emptyTitle, Modifier.padding(top = 10.dp), color = colors.ink)
        CivilightText(formatLabel(form.format), type.small, Modifier.padding(top = 4.dp), color = colors.ink3)
        PreviewTree(NewProjectRules.preview(form), Modifier.padding(top = 16.dp), swapKey = NewProjectRules.previewSwapKey(form))
        CivilightText(NewProjectCopy.PREVIEW_LIVE, type.small, Modifier.padding(top = 14.dp), color = colors.ink3)
    }
}
