package io.github.lenticualrstone.civilight.feature.library.shelf.flat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import io.github.lenticualrstone.civilight.model.project.ProjectFormat

/**
 * 편지함 (r3 `LvLibMailC` / `LvLibMailI`): each project an airmail envelope with its name, a postmark of the last
 * edit and a square form seal (장 · 단 · 설). Cover: the envelopes stack, the picked one opens to the height left
 * and its letter rises with the form, characters, last place, progress and `열기`. Inner: the envelopes share the
 * left column (the picked one taller, ringed) and the letter fills the right. The locked envelope shows only the
 * lock; its letter, `잠금 풀기`.
 */
@Composable
internal fun MailView(args: FlatArgs) {
    if (!args.wide) MailCover(args) else MailInner(args)
}

/** Board: a closed envelope 52 tall, stacked 6 over the one before. */
private const val ENV_H = 52f
private const val ENV_STEP = 46f
private const val LETTER_MIN = 220f

@Composable
private fun MailCover(args: FlatArgs) {
    BoxWithConstraints(Modifier.fillMaxSize().semantics { contentDescription = FlatViewCopy.MAILBOX_GROUP }) {
        val n = args.count
        val open = (maxHeight.value - ENV_STEP * (n - 1)).coerceAtLeast(LETTER_MIN)
        val scroll = ENV_STEP * (n - 1) + open > maxHeight.value + 1f
        Column(Modifier.fillMaxSize().then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)) {
            args.items.forEachIndexed { i, item ->
                val on = i == args.index
                val f by animateFloatAsState(if (on) 1f else 0f, flatMove(), label = "envelope")
                Envelope(
                    args, i, item,
                    Modifier
                        .offset(y = (-6 * i).dp)
                        .fillMaxWidth()
                        .height((ENV_H + (open - ENV_H) * f).dp),
                    letter = f,
                )
            }
        }
    }
}

@Composable
private fun MailInner(args: FlatArgs) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BoxWithConstraints(Modifier.width(276.dp).fillMaxHeight().semantics { contentDescription = FlatViewCopy.MAILBOX_GROUP }) {
            val n = args.count
            // Board `.mbi`: every envelope flex 1, the picked one 1.6, 8 apart; at least 52 each, then scroll.
            val unit = ((maxHeight.value - 8f * (n - 1)) / (n + .6f)).coerceAtLeast(ENV_H)
            val scroll = unit * (n + .6f) + 8f * (n - 1) > maxHeight.value + 1f
            Column(Modifier.fillMaxSize().then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                args.items.forEachIndexed { i, item ->
                    val on = i == args.index
                    val h by animateFloatAsState(if (on) unit * 1.6f else unit, flatMove(), label = "envelopeInner")
                    Envelope(args, i, item, Modifier.fillMaxWidth().height(h.dp).ring(on, CivilightTheme.colors.acc, 8.dp), letter = 0f, inner = true)
                }
            }
        }
        Letter(args, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun Envelope(args: FlatArgs, i: Int, item: ShelfCardItem, modifier: Modifier, letter: Float, inner: Boolean = false) {
    val colors = CivilightTheme.colors
    val look = FlatLook.current()
    val type = CivilightTheme.type
    val on = i == args.index
    val shape = RoundedCornerShape(8.dp)
    val row = (item as? ShelfCardItem.Project)?.card?.row
    val name = row?.let { "${it.title} · ${formWord(it.format)} · ${it.edited}" } ?: FlatViewCopy.LOCKED_PROJECT
    Box(
        modifier
            .then(if (!inner) Modifier.shadow(6.dp, shape, clip = false, ambientColor = look.shadow, spotColor = look.shadow) else Modifier)
            .lockTour(args, item)
            .unhideOf(item, args),
    ) {
        Airmail(Modifier.matchParentSize(), look)
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (inner) Modifier.fillMaxHeight() else Modifier.height(46.dp))
                .pressable(onClick = { args.tap(i) }, role = Role.Button, scaleOnPress = false)
                .semantics { contentDescription = name; stateDescription = if (on) FlatViewCopy.PICKED else "" }
                .padding(start = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (row == null) Arrangement.Center else Arrangement.spacedBy(10.dp),
        ) {
            if (row == null) {
                CivilightIcon(IconId.Lock, contentDescription = null, size = 18.dp, tint = colors.ink2)
            } else {
                CivilightText(row.title, TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold), Modifier.weight(1f), color = colors.ink, maxLines = 1)
                Postmark(row.edited)
                Seal(row.format, look)
            }
        }
        if (!inner && letter > 0.01f) {
            Box(
                Modifier
                    .padding(start = 10.dp, end = 10.dp, top = 46.dp, bottom = 8.dp)
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = letter
                        translationY = (1f - letter) * 16.dp.toPx()
                    },
            ) { LetterSlip(args, item) }
        }
    }
}

/** The airmail border: 3 dp of `s2 · env · s1 · env` stripes at 135°, the envelope inside. */
@Composable
private fun Airmail(modifier: Modifier, look: FlatLook) {
    Canvas(modifier) {
        val r = 8.dp.toPx()
        val b = 3.dp.toPx()
        clipRect {
            drawRoundRect(look.env, cornerRadius = CornerRadius(r))
            rotate(45f, pivot = Offset(size.width / 2f, size.height / 2f)) {
                val period = 24.dp.toPx()
                val span = size.width + size.height
                var x = -span
                while (x < span * 2) {
                    drawRect(look.s2, Offset(x, -span), Size(7.dp.toPx(), span * 3))
                    drawRect(look.s1, Offset(x + 12.dp.toPx(), -span), Size(7.dp.toPx(), span * 3))
                    x += period
                }
            }
        }
        drawRoundRect(look.env, Offset(b, b), Size(size.width - 2 * b, size.height - 2 * b), CornerRadius((r - b).coerceAtLeast(0f)))
    }
}

/** The postmark: the last edit on ruled lines, turned −4°. */
@Composable
private fun Postmark(text: String) {
    val colors = CivilightTheme.colors
    val line = colors.line
    Box(Modifier.rotate(-4f)) {
        Canvas(Modifier.matchParentSize()) {
            var y = 5.dp.toPx()
            while (y < size.height) {
                drawRect(line, Offset(0f, y), Size(size.width, 1.dp.toPx()))
                y += 6.dp.toPx()
            }
        }
        CivilightText(text, CivilightTheme.type.small.merge(TextStyle(fontWeight = FontWeight.SemiBold)), Modifier.padding(horizontal = 6.dp, vertical = 4.dp), color = colors.ink3, maxLines = 1)
    }
}

/** The square form seal (board `.seal`: 28 × 34, a dashed outline 2 outside). */
@Composable
internal fun Seal(format: ProjectFormat?, look: FlatLook) {
    val (bg, ink, glyph) = when (format) {
        ProjectFormat.SHORT -> Triple(look.s2, look.s2i, FlatViewCopy.SEAL_SHORT)
        ProjectFormat.SETTING_ONLY -> Triple(look.s3, look.s3i, FlatViewCopy.SEAL_SETTING)
        ProjectFormat.SERIAL, null -> Triple(look.s1, look.s1i, FlatViewCopy.SEAL_SERIAL)
    }
    val dash = CivilightTheme.colors.ink3
    Box(Modifier.size(34.dp, 40.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(dash, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx()))))
        }
        Box(Modifier.size(28.dp, 34.dp).clip(RoundedCornerShape(2.dp)).background(bg), contentAlignment = Alignment.Center) {
            CivilightText(glyph, TextStyle(fontSize = 15.sp, lineHeight = 15.sp, fontWeight = FontWeight.ExtraBold), color = ink, maxLines = 1)
        }
    }
}

/** The cover letter (board `.lt`). */
@Composable
private fun LetterSlip(args: FlatArgs, item: ShelfCardItem) {
    val look = FlatLook.current()
    val shape = RoundedCornerShape(8.dp)
    Column(
        Modifier.fillMaxSize().shadow(6.dp, shape, ambientColor = look.shadow, spotColor = look.shadow).clip(shape).background(look.letter).padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val row = (item as? ShelfCardItem.Project)?.card?.row
        if (row == null) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
                LockMark(30.dp, look.lettink2)
                CivilightButton(FlatViewCopy.UNLOCK, args.input.onUnlock, Modifier.height(44.dp), variant = ButtonVariant.PRIMARY, icon = IconId.Fingerprint)
            }
            return@Column
        }
        val small = CivilightTheme.type.small
        CivilightText("${formWord(row.format)} · ${FlatViewCopy.CHARS} ${FlatViewCopy.number(row.facts?.chars ?: 0)}", small, color = look.lettink2, maxLines = 1)
        row.facts?.place?.let { CivilightText(it, TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold), color = look.lettink, maxLines = 2) }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProgressBar(row.facts?.progress?.percent ?: 0, Modifier.weight(1f), track = Color.Black.copy(alpha = .10f), fill = look.letterAcc)
            CivilightText(progressLine(row), small.merge(TextStyle(fontWeight = FontWeight.SemiBold)), color = look.lettink2, maxLines = 1)
            LetterButton(FlatViewCopy.OPEN, look, Modifier, args.openPicked)
            MoreButton(args, item, tint = look.lettink2)
        }
    }
}

/** The inner letter (board `.let2`). */
@Composable
private fun Letter(args: FlatArgs, modifier: Modifier) {
    val look = FlatLook.current()
    val shape = RoundedCornerShape(16.dp)
    val item = args.selected
    Box(modifier.shadow(10.dp, shape, ambientColor = look.shadow, spotColor = look.shadow).clip(shape).background(look.letter)) {
        Box(Modifier.fillMaxWidth().height(6.dp).background(look.pgut))
        Column(Modifier.fillMaxSize().padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (item) {
                is ShelfCardItem.Project -> {
                    val row = item.card.row
                    Column(Modifier.weight(1f).unhideOf(item, args), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Seal(row.format, look)
                            CivilightText(formLine(row), CivilightTheme.type.small, color = look.lettink2, maxLines = 1)
                        }
                        CivilightText(row.title, TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold), color = look.lettink, maxLines = 2)
                        row.facts?.place?.let { CivilightText(it, CivilightTheme.type.small.merge(TextStyle(fontSize = 13.sp)), color = look.lettink2, maxLines = 1) }
                        Art(item, Modifier.fillMaxWidth().weight(1f), radius = 10.dp, targetWidth = 640)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ProgressBar(row.facts?.progress?.percent ?: 0, Modifier.weight(1f), track = Color.Black.copy(alpha = .10f), fill = look.letterAcc)
                            CivilightText(progressLine(row), CivilightTheme.type.small.merge(TextStyle(fontWeight = FontWeight.SemiBold)), color = look.lettink2, maxLines = 1)
                        }
                        val ruled = Color.Black.copy(alpha = .10f)
                        FactLine(FlatViewCopy.CHARS, FlatViewCopy.number(row.facts?.chars ?: 0), ink = look.lettink, ink2 = look.lettink2, line = ruled)
                        FactLine(FlatViewCopy.CAST, (row.facts?.cast ?: 0).toString(), ink = look.lettink, ink2 = look.lettink2, line = ruled)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        MoreButton(args, item, tint = look.lettink2)
                        LetterButton(FlatViewCopy.OPEN, look, Modifier, args.openPicked)
                    }
                }
                is ShelfCardItem.Lock -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
                    LockMark(40.dp, look.lettink2)
                    CivilightButton(FlatViewCopy.UNLOCK, args.input.onUnlock, Modifier.height(44.dp), variant = ButtonVariant.PRIMARY, icon = IconId.Fingerprint)
                }
                else -> Unit
            }
        }
    }
}
