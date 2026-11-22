package io.github.lenticualrstone.civilight.feature.recorder.capture

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.GlowMode
import io.github.lenticualrstone.civilight.designsystem.component.RecorderGlowBand
import io.github.lenticualrstone.civilight.designsystem.component.RecordingMark
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.designsystem.theme.animatedAlpha
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.designsystem.theme.rememberLastNonNull
import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.ShownCopy
import io.github.lenticualrstone.civilight.domain.recorder.link.Chip
import io.github.lenticualrstone.civilight.feature.recorder.RecorderUiCopy
import io.github.lenticualrstone.civilight.feature.recorder.link.LinkChipRow

/** Readable column of the capture screens: 560 at most, centred on inner screens (LRC-07 `[PROPOSED]`). */
private val COLUMN = 560.dp

/**
 * The full-window capture surface (boards `LrRecord`, `LrProcess`, `ErrMic`): the glass fill over the
 * screen the shell already blurred underneath, the voice glow behind the bottom edge (LRC-10), and
 * the content inside the system bars in a centred column.
 */
@Composable
fun CaptureFrame(glow: GlowMode?, level: () -> Float, glass: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val motion = CivilightTheme.motion
    // The glass fill fades with the stage (row 1) and the glow band fades in and out on its own mode (row 2);
    // the band keeps its last mode while it leaves. Mode blends (record to process) are the DS band's.
    val glassFill = animatedFraction(glass, label = "captureGlass")
    val fill = CivilightTheme.colors.glass
    val shownGlow = rememberLastNonNull(glow)
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind { drawRect(fill.copy(alpha = fill.alpha * glassFill.value)) },
    ) {
        AnimatedVisibility(
            visible = glow != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = motion.enter(),
            exit = motion.exit(),
        ) {
            if (shownGlow != null) RecorderGlowBand(shownGlow, level = level)
        }
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = COLUMN)
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
            content = content,
        )
    }
}

/** Header row: `라이프 레코더`, the timer, and on the right `취소` (recording only). */
@Composable
private fun CaptureHeader(timer: String, live: Boolean?, cancel: String?, onCancel: () -> Unit) {
    val colors = CivilightTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(start = 22.dp, end = 6.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (live != null) {
            val mark = animatedAlpha(if (live) 1f else 0.4f, label = "recordingMark")
            RecordingMark(Modifier.graphicsLayer { alpha = mark.value })
        }
        CivilightText(RecorderCopy.APP_NAME, CivilightTheme.type.label.copy(fontWeight = FontWeight.SemiBold), color = colors.ink, maxLines = 1)
        CivilightText(timer, CivilightTheme.type.number.copy(fontSize = 13.sp), color = colors.ink2, maxLines = 1)
        Spacer(Modifier.weight(1f))
        if (cancel != null) TextAction(cancel, onCancel)
    }
}

/** The transcript: section label, confirmed text in `ink`, the unconfirmed tail in `ink3` and the `acc` caret (LRC-07). */
@Composable
private fun ColumnScope.Transcript(label: String?, note: String?, confirmed: String, tail: String, caret: Boolean) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val scroll = rememberScrollState()
    val motion = CivilightTheme.motion
    // Reduced motion: `move` is a snap, so the scroll jumps to the end instead of gliding.
    LaunchedEffect(confirmed, tail) { scroll.animateScrollTo(scroll.maxValue, motion.move()) }
    val caretOn = rememberCaretOn()
    Column(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(scroll)
            .padding(horizontal = 22.dp),
    ) {
        if (label != null) CivilightText(label, type.sec, color = colors.ink3)
        if (note != null) CivilightText(note, type.small, Modifier.padding(top = 8.dp), color = colors.ink3)
        val text = buildAnnotatedString {
            withStyle(SpanStyle(color = colors.ink)) { append(confirmed) }
            if (tail.isNotEmpty()) {
                if (confirmed.isNotEmpty() && !confirmed.last().isWhitespace()) append(' ')
                withStyle(SpanStyle(color = colors.ink3)) { append(tail) }
            }
            if (caret) appendInlineContent(CARET, "|")
        }
        val caretContent = mapOf(
            CARET to InlineTextContent(Placeholder(5.sp, 26.sp, PlaceholderVerticalAlign.TextCenter)) {
                Box(Modifier.padding(start = 3.dp).size(2.dp, 26.dp).graphicsLayer { alpha = if (caretOn.value) 1f else 0f }.background(colors.acc))
            },
        )
        BasicText(text, Modifier.padding(top = 12.dp), style = type.recorderLive.copy(color = colors.ink), inlineContent = caretContent)
    }
}

private const val CARET = "caret"

/** `녹음 중` (LRC-07, board `LrRecord`). */
@Composable
fun ColumnScope.RecordingPane(
    view: RecordingView,
    onCancel: () -> Unit,
    onAddLink: () -> Unit,
    onReplaceLink: (Chip) -> Unit,
    onRemoveLink: (Chip) -> Unit,
    onToText: () -> Unit,
    onStop: () -> Unit,
    onPause: () -> Unit,
) {
    val screen = view.screen
    CaptureHeader(screen.timer, live = screen.live, cancel = screen.cancel, onCancel = onCancel)
    LinkChipRow(screen.chips, onAddLink, onReplaceLink, onRemoveLink, Modifier.padding(start = 22.dp, end = 22.dp, top = 8.dp))
    Spacer(Modifier.height(28.dp))
    Transcript(screen.transcript.label, view.engineNote, screen.transcript.confirmed, screen.transcript.tail, caret = true)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (view.canToText) CivilightButton(screen.toText, onToText, variant = ButtonVariant.GHOST, icon = IconId.Text)
        CivilightButton(screen.stopAndSave, onStop, Modifier.weight(1f).heightIn(min = 56.dp), variant = ButtonVariant.PRIMARY_LARGE)
        RoundButton(
            icon = if (view.paused) IconId.Mic else IconId.Pause,
            label = ShownCopy.of(screen.pauseLabel),
            size = 56,
            onClick = onPause,
        )
    }
}

/** `정리 중` (LRC-13, board `LrProcess`). */
@Composable
fun ColumnScope.ProcessingPane(
    view: ProcessingView,
    onAddLink: () -> Unit,
    onReplaceLink: (Chip) -> Unit,
    onRemoveLink: (Chip) -> Unit,
    onHide: (Boolean) -> Unit,
    onPutHere: () -> Unit,
    onContinue: () -> Unit,
    onKeep: () -> Unit,
) {
    val screen = view.screen
    val colors = CivilightTheme.colors
    val motion = CivilightTheme.motion
    CaptureHeader(screen.duration, live = null, cancel = null, onCancel = {})
    Column(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        // Rows that come and go (the finalising label, the hide row, the note) fold; the text under them follows.
        Column(Modifier.fillMaxWidth().animateContentSize(motion.move())) {
            Spacer(Modifier.height(64.dp))
            FoldRevealValue(screen.sectionLabel) { CivilightText(it, CivilightTheme.type.sec, color = colors.ink3) }
            CivilightText(screen.text, CivilightTheme.type.recorderLive, Modifier.padding(top = 12.dp), color = colors.ink)
            LinkChipRow(screen.chips, onAddLink, onReplaceLink, onRemoveLink, Modifier.padding(top = 22.dp))
            FoldRevealValue(screen.hideRow) { row ->
                SwitchRow(title = row.title, subtitle = row.sub, checked = row.on, onCheckedChange = onHide, modifier = Modifier.padding(top = 12.dp))
            }
            CivilightText(
                ShownCopy.of(screen.note),
                CivilightTheme.type.small.copy(fontSize = 13.sp, lineHeight = 20.sp),
                Modifier.padding(top = 18.dp),
                color = colors.ink3,
            )
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 26.dp),
    ) {
        FoldRevealValue(screen.putHere) { label ->
            CivilightButton(label, onPutHere, Modifier.fillMaxWidth().padding(bottom = 10.dp), variant = ButtonVariant.GHOST)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CivilightButton(screen.buttons[0], onContinue, Modifier.weight(1f), variant = ButtonVariant.GHOST)
            CivilightButton(screen.buttons[1], onKeep, Modifier.weight(1f), variant = ButtonVariant.PRIMARY)
        }
    }
}

/** `마이크 꺼짐` (LRC-06, board `ErrMic`). */
@Composable
fun ColumnScope.MicOffPane(onClose: () -> Unit, onOpenSettings: () -> Unit, onToText: () -> Unit) {
    val colors = CivilightTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(start = 22.dp, end = 6.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivilightText(RecorderCopy.APP_NAME, CivilightTheme.type.label.copy(fontWeight = FontWeight.SemiBold), Modifier.weight(1f), color = colors.ink, maxLines = 1)
        TextAction(RecorderCopy.CLOSE, onClose)
    }
    Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 110.dp)) {
        CivilightIcon(IconId.Mic, contentDescription = null, size = 28.dp, tint = colors.r18)
        CivilightText(RecorderUiCopy.MIC_OFF_TITLE, CivilightTheme.type.stateTitle, Modifier.padding(top = 12.dp), color = colors.ink)
        CivilightText(RecorderUiCopy.MIC_OFF_SUB, CivilightTheme.type.small.copy(fontSize = 13.sp), Modifier.padding(top = 6.dp), color = colors.ink2)
        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CivilightButton(RecorderUiCopy.OPEN_SYSTEM_SETTINGS, onOpenSettings, Modifier.fillMaxWidth(), variant = ButtonVariant.PRIMARY_LARGE)
            CivilightButton(RecorderCopy.TO_TEXT, onToText, Modifier.fillMaxWidth(), variant = ButtonVariant.GHOST)
        }
    }
}

/** The reason line before the system prompt (LRC-06 `[PROPOSED]`: the `OnbVoice` choice as a small sheet). */
@Composable
fun ColumnScope.MicReasonContent(onAllow: () -> Unit, onClose: () -> Unit) {
    SheetHeader(title = RecorderCopy.APP_NAME, onClose = onClose, closeLabel = RecorderCopy.CLOSE)
    CivilightButton(
        text = RecorderUiCopy.MIC_ALLOW,
        subText = RecorderUiCopy.MIC_ALLOW_SUB,
        onClick = onAllow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        variant = ButtonVariant.TWO_LINE,
    )
}

/** A round ghost button (`일시정지` 56, the memo's mic 48) with its Korean label. */
@Composable
private fun RoundButton(icon: IconId, label: String, size: Int, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    Box(
        Modifier
            .pressable(onClick = onClick, hitTarget = HitTarget.ICON_BUTTON, shape = CircleShape)
            .size(size.dp)
            .clip(CircleShape)
            .background(colors.tint)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(icon, animationSpec = CivilightTheme.motion.spec(CivilightMotion.standard), label = "roundIcon") { shown ->
            CivilightIcon(shown, contentDescription = null, size = 20.dp, tint = colors.ink)
        }
    }
}
