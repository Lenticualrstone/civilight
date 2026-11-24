package io.github.lenticualrstone.civilight.feature.library.shelf.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.Unhide
import io.github.lenticualrstone.civilight.designsystem.theme.unhide
import io.github.lenticualrstone.civilight.feature.library.shelf.CoverArt
import io.github.lenticualrstone.civilight.feature.library.shelf.ProjectCardUi
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCopy
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import kotlin.math.max

/*
 * The faces the boards draw on their boxes: spines, covers, manuscript sheets, index cards, corridor bays.
 * They are plain composables recorded into a layer ([FaceSource]) and replayed in the face's projected quad.
 * Sizes are the boards' px as dp, and type is set in dp too (a face is a picture of a book, so the system
 * font scale does not reflow it). The lock item never draws a project's name or cover (LIB-89, V-04): only
 * the lock mark, in the boards' lock tokens.
 */

/** Board text: [px] is the boards' font size (dp), whatever the system font scale. */
@Composable
@ReadOnlyComposable
internal fun boardText(px: Float, line: Float, weight: FontWeight, color: Color, spacingEm: Float = 0f): TextStyle {
    val d = LocalDensity.current
    fun u(v: Float): TextUnit = (v / d.fontScale).sp
    return CivilightTheme.type.body.merge(
        TextStyle(fontSize = u(px), lineHeight = u(line), fontWeight = weight, color = color, letterSpacing = spacingEm.em),
    )
}

@Composable
private fun T(text: String, style: TextStyle, modifier: Modifier = Modifier, maxLines: Int = 1) {
    BasicText(text, modifier, style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

/** The 형식 word of a project (the boards' `ckd` chip: 장편 · 단편); null while it loads. */
internal fun formWord(card: ProjectCardUi): String? = when (card.row.format) {
    ProjectFormat.SERIAL -> ShelfViewCopy.FORMAT_SERIAL
    ProjectFormat.SHORT -> ShelfViewCopy.FORMAT_SHORT
    ProjectFormat.SETTING_ONLY -> ShelfViewCopy.FORMAT_SETTING
    null -> null
}

private fun Modifier.unhideOf(card: ProjectCardUi?, unhide: Unhide?): Modifier =
    if (card?.hiddenOpen == true && unhide != null) unhide(unhide) else this

// ------------------------------------------------------------------ board A (책장 · 표지 회전)

/** Board A tokens (`XT` + the shared night-manuscript tokens), dark and light. */
internal class LookA(
    val wallA: Color, val wallB: Color, val glow: Color, val back: Color, val plankT: Color, val plankF: Color, val plankE: Color,
    val dimc: Color, val sheen: Color, val lockc: Color, val lockink: Color, val paperE: Color, val shadow: Color,
    val rfA: Color, val rfB: Color, val page: Color, val pageln: Color, val line: Color, val edge: Color, val bg: Color,
) {
    companion object {
        val dark = LookA(
            Color(0xFF15171C), Color(0xFF0B0C0E), Color(226, 176, 104).copy(alpha = .16f), Color(0xFF121419), Color(0xFF25282E), Color(0xFF1B1D22),
            Color.White.copy(alpha = .07f), Color(6, 7, 9).copy(alpha = .62f), Color.White.copy(alpha = .26f), Color(0xFF262930), Color(0xFF9A9DA6),
            Color(0xFFCDC6B8), Color.Black.copy(alpha = .55f), Color(11, 12, 14).copy(alpha = .5f), Color(0xFF0B0C0E), Color(0xFF14161A),
            Color.White.copy(alpha = .10f), Color.White.copy(alpha = .09f), Color.White.copy(alpha = .38f), Color(0xFF0B0C0E),
        )
        val light = LookA(
            Color(0xFFEAE7E0), Color(0xFFF6F5F1), Color(200, 145, 63).copy(alpha = .18f), Color(0xFFE2DFD6), Color(0xFFD6D1C5), Color(0xFFC6C0B2),
            Color.White.copy(alpha = .7f), Color(120, 116, 108).copy(alpha = .35f), Color.White.copy(alpha = .38f), Color(0xFFD2CEC5), Color(0xFF55585F),
            Color(0xFFEAE4D7), Color(40, 34, 24).copy(alpha = .24f), Color(246, 245, 241).copy(alpha = .5f), Color(0xFFF6F5F1), Color(0xFFFBFAF7),
            Color(22, 23, 26).copy(alpha = .10f), Color(22, 23, 26).copy(alpha = .10f), Color(22, 23, 26).copy(alpha = .5f), Color(0xFFF6F5F1),
        )

        @Composable
        @ReadOnlyComposable
        fun current(): LookA = if (CivilightTheme.colors.isDark) dark else light
    }
}

private val SpineInk = Color(0xFFF3EEE6)
private val CoverInk = Color(0xFFF6F2EA)

/**
 * Board A spine (`.sp`, [t] × [h]): the cloth with its turned-edge shading, the double band, the title running
 * down upright (13 px bold, `vertical-rl` · `upright`), the 형식 chip; the lock spine and the ruled new spine.
 */
@Composable
internal fun SpineFace(item: ShelfCardItem, tone: Tone, t: Float, h: Float, unhide: Unhide?) {
    val look = LookA.current()
    when (item) {
        is ShelfCardItem.Project -> Box(
            Modifier
                .size(t.dp, h.dp)
                .unhideOf(item.card, unhide)
                .drawBehind {
                    drawRect(tone.cloth)
                    drawRect(
                        Brush.horizontalGradient(
                            0f to Color.Black.copy(alpha = .34f), .26f to Color.White.copy(alpha = .12f),
                            .52f to Color.White.copy(alpha = 0f), 1f to Color.Black.copy(alpha = .3f),
                        ),
                    )
                    val px = density
                    // `.bd`: 3 px band, its `0 7px 0 -1px` shadow a second, narrower band under it.
                    drawRect(tone.band, Offset(0f, 14f * px), Size(size.width, 3f * px))
                    drawRect(tone.band, Offset(1f * px, 20f * px), Size(size.width - 2f * px, 3f * px))
                    drawRect(Color.White.copy(alpha = .05f), style = Stroke(1f * px))
                },
        ) {
            Column(Modifier.fillMaxSize().padding(top = 14.dp + 3.dp + 18.dp, bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val form = formWord(item.card)
                val chipH = form?.let { it.length * 11f + 8f + 2f + 8f } ?: 0f
                val room = h - 14f - 3f - 18f - 12f - chipH
                Upright(item.card.row.title, max(1, (room / (13f + 0.78f)).toInt()), boardText(13f, 13f, FontWeight.Bold, SpineInk), 13.78f, Modifier.weight(1f, fill = false))
                if (form != null) {
                    Box(
                        Modifier
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .insetOutline(SpineInk.copy(alpha = .5f), 1.dp, RoundedCornerShape(3.dp))
                            .padding(horizontal = 1.dp, vertical = 4.dp),
                    ) { Upright(form, form.length, boardText(9f, 11f, FontWeight.SemiBold, SpineInk), 11f) }
                }
            }
        }
        is ShelfCardItem.Lock -> Box(
            Modifier
                .size(t.dp, h.dp)
                .drawBehind {
                    drawRect(look.lockc)
                    drawRect(Brush.horizontalGradient(0f to Color.Black.copy(alpha = .18f), .3f to Color.White.copy(alpha = .08f), 1f to Color.Black.copy(alpha = .16f)))
                },
            contentAlignment = Alignment.Center,
        ) { CivilightIcon(IconId.Lock, contentDescription = null, size = 16.dp, tint = look.lockink) }
        ShelfCardItem.NewProject -> Box(
            Modifier
                .size(t.dp, h.dp)
                .clip(RoundedCornerShape(4.dp))
                .drawBehind {
                    var y = 7f * density
                    while (y < size.height) {
                        drawRect(look.line, Offset(0f, y), Size(size.width, density))
                        y += 8f * density
                    }
                }
                .insetOutline(look.edge, 1.5.dp, RoundedCornerShape(4.dp)),
        ) {
            Column(Modifier.fillMaxSize().padding(top = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CivilightIcon(IconId.Plus, contentDescription = null, size = 16.dp, tint = CivilightTheme.colors.ink3)
                Upright(ShelfCopy.NEW_PROJECT, 6, boardText(13f, 13f, FontWeight.SemiBold, CivilightTheme.colors.ink3), 13.78f, Modifier.padding(top = 12.dp))
            }
        }
    }
}

/** `writing-mode: vertical-rl; text-orientation: upright`: one glyph under another, [pitch] dp apart. */
@Composable
private fun Upright(text: String, maxGlyphs: Int, style: TextStyle, pitch: Float, modifier: Modifier = Modifier) {
    val fit = SpineText.fit(text, maxGlyphs)
    // A long name first tightens its pitch (to 78 %), then ends in `︙` at the spine's foot (never cut mid-glyph).
    val k = fit.scale
    val shown = if (k < 1f) style.merge(TextStyle(fontSize = style.fontSize * k, lineHeight = style.lineHeight * k)) else style
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        for (g in fit.glyphs) {
            // A space keeps its place as a half step, as `writing-mode: vertical-rl` sets it.
            if (g == ' ') Box(Modifier.height((pitch * k * SpineText.SPACE).dp)) else Box(Modifier.height((pitch * k).dp), contentAlignment = Alignment.Center) { BasicText(g.toString(), style = shown) }
        }
    }
}

/** Upright spine text (pure, JVM-tested): the glyphs drawn and their size. */
internal object SpineText {
    /** A space takes half a step. */
    const val SPACE = 0.5f

    /** The smallest pitch a long name tightens to before it is ellipsized. */
    const val MIN_SCALE = 0.78f

    const val ELLIPSIS = '︙'

    class Fit(val glyphs: List<Char>, val scale: Float)

    /** Steps a run of glyphs takes (a space is half). */
    fun steps(glyphs: List<Char>): Float = glyphs.sumOf { if (it == ' ') SPACE.toDouble() else 1.0 }.toFloat()

    /** [text] (runs of spaces collapsed) in [room] full steps: tightened down to [MIN_SCALE], then cut at a glyph with `︙`. */
    fun fit(text: String, room: Int): Fit {
        val all = text.trim().replace(Regex("\\s+"), " ").toList()
        if (room <= 0 || all.isEmpty()) return Fit(emptyList(), 1f)
        val need = steps(all)
        if (need <= room) return Fit(all, 1f)
        if (need * MIN_SCALE <= room) return Fit(all, room / need)
        val cap = room / MIN_SCALE
        val out = ArrayList<Char>()
        var used = 0f
        for (c in all) {
            val step = if (c == ' ') SPACE else 1f
            if (used + step + 1f > cap) break
            out += c
            used += step
        }
        while (out.isNotEmpty() && out.last() == ' ') out.removeAt(out.size - 1)
        out += ELLIPSIS
        return Fit(out, MIN_SCALE)
    }
}

/**
 * Board A cover (`.cvr`): the project's cover art, the bottom shade, the 형식 chip, the title and meta, and the
 * 7 px spine shading on the left. [big] is the cover-flow size (title 21, chip at 14). Lock: the lock tokens
 * and the mark only. New: the outlined place.
 */
@Composable
internal fun BookCoverFace(item: ShelfCardItem, big: Boolean, unhide: Unhide?, radius: Dp = if (big) 6.dp else 2.dp) {
    val look = LookA.current()
    val shape = if (big) RoundedCornerShape(radius) else RoundedCornerShape(2.dp, 5.dp, 5.dp, 2.dp)
    when (item) {
        is ShelfCardItem.Project -> Box(Modifier.fillMaxSize().clip(shape).unhideOf(item.card, unhide)) {
            val px = with(LocalDensity.current) { 220.dp.roundToPx() }
            CoverArt(item.card.cover, px)
            Box(Modifier.fillMaxSize().drawBehind {
                val top = size.height * .48f
                drawRect(Brush.verticalGradient(listOf(Color(8, 9, 12, 0), Color(8, 9, 12).copy(alpha = .78f)), startY = top, endY = size.height), Offset(0f, top), Size(size.width, size.height - top))
                drawRect(
                    Brush.horizontalGradient(0f to Color.Black.copy(alpha = .35f), .5f to Color.White.copy(alpha = .12f), 1f to Color.Transparent, endX = 7f * density),
                    size = Size(7f * density, size.height),
                )
            })
            formWord(item.card)?.let { form ->
                val chip = RoundedCornerShape(5.dp)
                Box(
                    Modifier
                        .padding(start = if (big) 14.dp else 12.dp, top = if (big) 14.dp else 12.dp)
                        .height(18.dp)
                        .clip(chip)
                        .background(Color(8, 9, 12).copy(alpha = .55f))
                        .insetOutline(CoverInk.copy(alpha = .6f), 1.dp, chip)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) { T(form, boardText(10f, 18f, FontWeight.SemiBold, CoverInk)) }
            }
            Column(Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 10.dp, bottom = 12.dp)) {
                T(item.card.row.title, if (big) boardText(21f, 27f, FontWeight.ExtraBold, CoverInk, -.01f) else boardText(17f, 22f, FontWeight.ExtraBold, CoverInk, -.01f), maxLines = 2)
                T(item.card.row.meta, if (big) boardText(12f, 17f, FontWeight.Medium, CoverInk.copy(alpha = .82f)) else boardText(11f, 15f, FontWeight.Medium, CoverInk.copy(alpha = .82f)), Modifier.padding(top = 3.dp))
            }
        }
        is ShelfCardItem.Lock -> Box(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .drawBehind {
                    drawRect(look.lockc)
                    drawRect(Brush.linearGradient(listOf(Color.White.copy(alpha = .06f), Color.Black.copy(alpha = .12f)), start = Offset(size.width * .33f, 0f), end = Offset(size.width * .67f, size.height)))
                },
            contentAlignment = Alignment.Center,
        ) { CivilightIcon(IconId.Lock, contentDescription = null, size = if (big) 30.dp else 26.dp, tint = look.lockink) }
        ShelfCardItem.NewProject -> {
            val s = RoundedCornerShape(6.dp)
            Column(
                Modifier.fillMaxSize().clip(s).background(look.bg).insetOutline(look.edge, 1.5.dp, s),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CivilightIcon(IconId.Plus, contentDescription = null, size = 26.dp, tint = CivilightTheme.colors.ink2)
                T(ShelfCopy.NEW_PROJECT, boardText(13f, 18f, FontWeight.SemiBold, CivilightTheme.colors.ink2))
            }
        }
    }
}

// ------------------------------------------------------------------ board B (원고 더미 · 서고 회랑)

/** Board B tokens (`TOK`), dark and light. */
internal class LookB(
    val wall: Color, val wall2: Color, val desk1: Color, val desk2: Color, val deskln: Color, val lamp: Color,
    val paper: Color, val paper2: Color, val paper3: Color, val pink: Color, val pink2: Color, val drop: Color, val clip: Color, val clip2: Color,
    val hw1: Color, val hw2: Color, val board: Color, val gap: Color, val sp: List<Color>,
    val fl1: Color, val fl2: Color, val ceil: Color, val beam: Color, val shelf: Color, val shelf2: Color, val brass: Color, val brassink: Color,
    val far: Color, val glow: Color, val wsh: Color, val cone: Color, val bulb: Color, val edge: Color, val tint: Color, val ink: Color, val ink2: Color, val bg: Color,
) {
    companion object {
        // (0.2.3) `wall` = the theme's `bg` (was 16110E / E6DFD2): the stage behind the desk no longer reads as a
        // coloured box now that it runs on under the bars.
        val dark = LookB(
            Color(0xFF0B0C0E), Color(0xFF0B0C0E), Color(0xFF3B2A1F), Color(0xFF1B130E), Color(255, 226, 190).copy(alpha = .05f), Color(255, 196, 124).copy(alpha = .34f),
            Color(0xFFECE5D6), Color(0xFFD5CBB8), Color(0xFFB9AE99), Color(0xFF22201C), Color(0xFF6A6358), Color.Black.copy(alpha = .62f), Color(0xFF1C1D20), Color(0xFF6A6E77),
            Color(0xFF241A14), Color(0xFF120D0A), Color(0xFF3E2C20), Color(0xFF0A0807),
            listOf(Color(0xFF4A3426), Color(0xFF2E3A4D), Color(0xFF5C4330), Color(0xFF3A2820), Color(0xFF2C3529)),
            Color(0xFF1C1511), Color(0xFF0E0B09), Color(0xFF0F0C0A), Color(0xFF251C15), Color(0xFF4A3527), Color(0xFF22180F), Color(0xFFC9A46A), Color(0xFF24190E),
            Color(0xFF1C1612), Color(255, 186, 108).copy(alpha = .55f), Color.Black.copy(alpha = .32f), Color(255, 204, 146).copy(alpha = .20f), Color(0xFFFFE2B8),
            Color.White.copy(alpha = .38f), Color.White.copy(alpha = .07f), Color(0xFFECECEF), Color(0xFFB4B6BD), Color(0xFF0B0C0E),
        )
        val light = LookB(
            Color(0xFFF6F5F1), Color(0xFFF6F5F1), Color(0xFFD2BA9C), Color(0xFFA98B6C), Color(80, 50, 20).copy(alpha = .07f), Color(255, 250, 236).copy(alpha = .62f),
            Color(0xFFFFFDF7), Color(0xFFE8E1D3), Color(0xFFCFC6B4), Color(0xFF1C1B18), Color(0xFF5D574D), Color(70, 46, 24).copy(alpha = .30f), Color(0xFF2A2B2E), Color(0xFF8D9097),
            Color(0xFFD6C8B3), Color(0xFFB8A68C), Color(0xFF8E6E50), Color(0xFF5A4836),
            listOf(Color(0xFFA47A5C), Color(0xFF6E819A), Color(0xFFC29874), Color(0xFF8C6852), Color(0xFF7F8D74)),
            Color(0xFFD8CBB6), Color(0xFFB5A389), Color(0xFFEEE7DB), Color(0xFFC6B399), Color(0xFFA9845F), Color(0xFF7A5B40), Color(0xFFD9BC84), Color(0xFF24190E),
            Color(0xFFFBF6EC), Color(255, 206, 140).copy(alpha = .70f), Color(74, 50, 26).copy(alpha = .16f), Color(255, 232, 190).copy(alpha = .42f), Color(0xFFFFF4DC),
            Color(22, 23, 26).copy(alpha = .5f), Color(22, 23, 26).copy(alpha = .05f), Color(0xFF16171A), Color(0xFF4A4C52), Color(0xFFF6F5F1),
        )

        @Composable
        @ReadOnlyComposable
        fun current(): LookB = if (CivilightTheme.colors.isDark) dark else light
    }
}

/**
 * Board B manuscript sheet (`.ms .pg`, 176 × 236): paper with its five stacked edges and drop shadow, the
 * binder clip, the cover window (14 … 162 × 26 … 122), the title, a rule, the meta and two short rules. The
 * lock sheet is plain paper with the lock disc; the new sheet is the dashed place.
 */
@Composable
internal fun SheetFace(item: ShelfCardItem, unhide: Unhide?) {
    val look = LookB.current()
    val shape = RoundedCornerShape(3.dp)
    val paperBg = if (item is ShelfCardItem.Lock) look.paper2 else look.paper
    if (item is ShelfCardItem.NewProject) {
        Column(
            Modifier.fillMaxSize().drawBehind {
                drawRoundRect(look.edge, cornerRadius = CornerRadius(8f * density), style = Stroke(2f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * density, 4f * density))))
            },
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(look.tint), contentAlignment = Alignment.Center) {
                CivilightIcon(IconId.Plus, contentDescription = null, size = 20.dp, tint = look.ink2)
            }
            T(ShelfCopy.NEW_PROJECT, boardText(14f, 18f, FontWeight.SemiBold, look.ink2))
        }
        return
    }
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                // `box-shadow: 1px 1px 0 paper2, 2px 2px 0 paper, 3px 3px 0 paper3, 4px 4px 0 paper, 5px 5px 0 paper2`.
                val r = CornerRadius(3f * density)
                val edges = arrayOf(look.paper2, look.paper, look.paper3, look.paper, look.paper2)
                for (k in 5 downTo 1) drawRoundRect(edges[k - 1], Offset(k * density, k * density), size, r)
            }
            .clip(shape)
            .background(paperBg)
            .unhideOf((item as? ShelfCardItem.Project)?.card, unhide),
    ) {
        when (item) {
            is ShelfCardItem.Project -> {
                Box(Modifier.offset(14.dp, 26.dp).size(148.dp, 96.dp).clip(RoundedCornerShape(2.dp))) {
                    CoverArt(item.card.cover, with(LocalDensity.current) { 160.dp.roundToPx() })
                }
                T(item.card.row.title, boardText(17f, 22f, FontWeight.ExtraBold, look.pink, -.02f), Modifier.offset(14.dp, 132.dp).width(150.dp), maxLines = 2)
                Rule(look.pink, 176f, 14f)
                T(item.card.row.meta, boardText(11f, 15f, FontWeight.Medium, look.pink2), Modifier.offset(14.dp, 182.dp).width(150.dp))
                Rule(look.pink, 206f, 60f)
                Rule(look.pink, 216f, 90f)
            }
            is ShelfCardItem.Lock -> Box(
                Modifier.align(Alignment.Center).size(56.dp).clip(CircleShape).background(look.paper).insetOutline(Color.Black.copy(alpha = .08f), 1.dp, CircleShape),
                contentAlignment = Alignment.Center,
            ) { CivilightIcon(IconId.Lock, contentDescription = null, size = 24.dp, tint = look.pink2) }
            ShelfCardItem.NewProject -> Unit
        }
        // `.clip`: the binder clip over the top edge.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-4).dp)
                .size(46.dp, 16.dp)
                .clip(RoundedCornerShape(3.dp, 3.dp, 6.dp, 6.dp))
                .background(Brush.verticalGradient(listOf(look.clip2, look.clip))),
        )
    }
}

@Composable
private fun Rule(color: Color, top: Float, right: Float) {
    Box(Modifier.fillMaxWidth().padding(start = 14.dp, end = right.dp).offset(y = top.dp).height(1.dp).background(color.copy(alpha = .12f)))
}

// ------------------------------------------------------------------ board C (색인 카드 서랍 · 별자리)

/** Board C tokens (`TOK`), dark and light. */
internal class LookC(
    val card: Color, val cardb: Color, val rule: Color, val rtop: Color, val cline: Color,
    val dw1: Color, val dw2: Color, val dwf: Color, val dwf2: Color, val dwin: Color, val brass: Color,
    val sheen: Color, val shadow: Color, val sky1: Color, val sky2: Color, val star: Color, val cst: Color,
    val page: Color, val ink: Color, val ink2: Color, val ink3: Color, val edge: Color, val raise: Color, val line: Color, val acc: Color, val bg: Color, val tint: Color,
) {
    companion object {
        val dark = LookC(
            Color(0xFF1F2228), Color(0xFF191B20), Color(143, 184, 230).copy(alpha = .12f), Color(201, 138, 98).copy(alpha = .6f), Color.White.copy(alpha = .08f),
            Color(0xFF1C1E23), Color(0xFF111316), Color(0xFF25282F), Color(0xFF1A1C21), Color(0xFF0D0E11), Color(0xFF8C7A5C),
            Color.White.copy(alpha = .12f), Color.Black.copy(alpha = .55f), Color(0xFF151B28), Color(0xFF0B0C0E), Color(236, 236, 239).copy(alpha = .7f), Color(143, 184, 230).copy(alpha = .30f),
            Color(0xFFD8D2C4), Color(0xFFECECEF), Color(0xFFB4B6BD), Color(0xFF8E9099), Color.White.copy(alpha = .38f), Color(0xFF16181C), Color.White.copy(alpha = .09f), Color(0xFF8FB8E6), Color(0xFF0B0C0E), Color.White.copy(alpha = .07f),
        )
        val light = LookC(
            Color(0xFFFFFDF8), Color(0xFFF1EDE4), Color(31, 85, 144).copy(alpha = .13f), Color(179, 38, 30).copy(alpha = .45f), Color(22, 23, 26).copy(alpha = .10f),
            Color(0xFFE2DED4), Color(0xFFD2CDC1), Color(0xFFDDD8CC), Color(0xFFCEC8BB), Color(0xFFB9B3A6), Color(0xFF9A8257),
            Color.White.copy(alpha = .32f), Color(40, 36, 28).copy(alpha = .22f), Color(0xFFE9EDF4), Color(0xFFF6F5F1), Color(22, 23, 26).copy(alpha = .42f), Color(31, 85, 144).copy(alpha = .30f),
            Color(0xFFF3EFE4), Color(0xFF16171A), Color(0xFF4A4C52), Color(0xFF62646B), Color(22, 23, 26).copy(alpha = .5f), Color(0xFFECEAE4), Color(22, 23, 26).copy(alpha = .10f), Color(0xFF1F5590), Color(0xFFF6F5F1), Color(22, 23, 26).copy(alpha = .05f),
        )

        @Composable
        @ReadOnlyComposable
        fun current(): LookC = if (CivilightTheme.colors.isDark) dark else light
    }
}

/** Board C: the tab's x on the card for the item's place in the drawer (`TABX` cycle 14 · 100 · 186). */
internal fun tabX(index: Int): Float = floatArrayOf(14f, 100f, 186f)[index.mod(3)]

/**
 * Board C index card (`.card .fc`, 318 × 290, with its 27 px tab above: the face is 318 × 315, the card
 * starting 25 px down): ruled lines from 89 every 22, the red top rule at 87, the title, meta, the 66 × 92
 * cover. The lock card shows the lock only (tab too); the new card the dashed place.
 */
@Composable
internal fun IndexCardFace(item: ShelfCardItem, index: Int, unhide: Unhide?, tone: Tone? = null) {
    val look = LookC.current()
    val tabW = if (item is ShelfCardItem.Project) 118f else 44f
    Box(Modifier.fillMaxSize()) {
        // The tab.
        val tabShape = RoundedCornerShape(7.dp, 7.dp, 0.dp, 0.dp)
        Row(
            Modifier
                .offset(x = tabX(index).dp)
                .size(tabW.dp, 27.dp)
                .clip(tabShape)
                .background(look.card)
                .drawBehind {
                    drawRect(look.edge, size = Size(size.width, density))
                    drawRect(look.cline, size = Size(density, size.height))
                    drawRect(look.cline, Offset(size.width - density, 0f), Size(density, size.height))
                }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (item is ShelfCardItem.Project) Arrangement.spacedBy(6.dp) else Arrangement.Center,
        ) {
            when (item) {
                is ShelfCardItem.Project -> {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(tone?.band ?: look.rtop))
                    T(item.card.row.title, boardText(12f, 16f, FontWeight.SemiBold, look.ink))
                }
                is ShelfCardItem.Lock -> CivilightIcon(IconId.Lock, contentDescription = null, size = 14.dp, tint = look.ink2)
                ShelfCardItem.NewProject -> CivilightIcon(IconId.Plus, contentDescription = null, size = 14.dp, tint = look.ink2)
            }
        }
        // The card.
        val shape = RoundedCornerShape(6.dp)
        Box(
            Modifier
                .offset(y = 25.dp)
                .size(318.dp, 290.dp)
                .clip(shape)
                .background(look.card)
                .drawBehind {
                    val d = density
                    var y = 89f * d + 21f * d
                    while (y < size.height) {
                        drawRect(look.rule, Offset(0f, y), Size(size.width, d))
                        y += 22f * d
                    }
                    drawRect(look.rtop, Offset(0f, 87f * d), Size(size.width, 1.5f * d))
                    drawRect(look.cline, style = Stroke(d))
                    drawRect(look.edge, size = Size(size.width, d))
                }
                .unhideOf((item as? ShelfCardItem.Project)?.card, unhide),
        ) {
            when (item) {
                is ShelfCardItem.Project -> {
                    Column(Modifier.padding(start = 18.dp, end = 16.dp, top = 36.dp)) {
                        T(item.card.row.title, boardText(21f, 28f, FontWeight.Bold, look.ink, -.01f))
                        T(item.card.row.meta, boardText(12f, 17f, FontWeight.Medium, look.ink2), Modifier.padding(top = 2.dp))
                    }
                    Box(Modifier.offset(18.dp, 100.dp).size(66.dp, 92.dp).clip(RoundedCornerShape(4.dp))) {
                        CoverArt(item.card.cover, with(LocalDensity.current) { 80.dp.roundToPx() })
                    }
                    formWord(item.card)?.let { form ->
                        Row(Modifier.offset(100.dp, 88.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            T(ShelfViewCopy.FORM_KEY, boardText(12f, 22f, FontWeight.Normal, look.ink3), Modifier.width(28.dp))
                            T(form, boardText(12f, 22f, FontWeight.Normal, look.ink2))
                        }
                    }
                }
                is ShelfCardItem.Lock -> Box(Modifier.fillMaxWidth().padding(top = 36.dp).height(44.dp), contentAlignment = Alignment.Center) {
                    CivilightIcon(IconId.Lock, contentDescription = null, size = 30.dp, tint = look.ink2)
                }
                ShelfCardItem.NewProject -> Row(
                    Modifier
                        .offset(18.dp, 100.dp)
                        .size(282.dp, 92.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .insetOutline(look.edge, 1.5.dp, RoundedCornerShape(6.dp)),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CivilightIcon(IconId.Plus, contentDescription = null, size = 18.dp, tint = look.ink2)
                    T(ShelfCopy.NEW_PROJECT, boardText(15f, 20f, FontWeight.SemiBold, look.ink2))
                }
            }
        }
    }
}

/** Board C star book cover (`.bk3 .fr`, 110 × 156): cover art, the 3 px binding shade, the title at the foot. */
@Composable
internal fun StarCoverFace(item: ShelfCardItem.Project, unhide: Unhide?) {
    val shape = RoundedCornerShape(3.dp, 6.dp, 6.dp, 3.dp)
    Box(Modifier.fillMaxSize().clip(shape).unhideOf(item.card, unhide)) {
        CoverArt(item.card.cover, with(LocalDensity.current) { 120.dp.roundToPx() })
        Box(Modifier.fillMaxSize().drawBehind {
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(8, 9, 12).copy(alpha = .6f)), startY = size.height * .55f), Offset(0f, size.height * .55f), Size(size.width, size.height * .45f))
            drawRect(Color.Black.copy(alpha = .18f), size = Size(3f * density, size.height))
        })
        T(item.card.row.title, boardText(13f, 17f, FontWeight.Bold, CoverInk, -.01f), Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 10.dp, bottom = 12.dp), maxLines = 2)
    }
}

/** Board C lock star (`.lks .f`, 72 × 72): a raised disc in its halo, the lock. */
@Composable
internal fun LockStarFace() {
    val look = LookC.current()
    Box(
        Modifier.fillMaxSize().drawBehind {
            val r = size.minDimension / 2f
            drawCircle(Brush.radialGradient(listOf(look.cst, Color.Transparent), radius = r * 0.7f / 0.7f), r)
            drawCircle(look.raise, r * .52f)
            drawCircle(look.line, r, style = Stroke(density))
        },
        contentAlignment = Alignment.Center,
    ) { CivilightIcon(IconId.Lock, contentDescription = null, size = 22.dp, tint = look.ink) }
}

/** Board C new-project slot (`.nslot`, 64 circle, 1.5 px edge, + ; `sel`: 2 px accent). */
@Composable
internal fun NewSlotFace(selected: Boolean) {
    val look = LookC.current()
    Box(
        Modifier.fillMaxSize().drawBehind {
            drawCircle(if (selected) look.acc else look.edge, size.minDimension / 2f - density, style = Stroke(if (selected) 2f * density else 1.5f * density))
        },
        contentAlignment = Alignment.Center,
    ) { CivilightIcon(IconId.Plus, contentDescription = null, size = 20.dp, tint = if (selected) look.ink else look.ink2) }
}

/** A canvas-only face helper for boards that draw their faces themselves. */
@Composable
internal fun PaintFace(modifier: Modifier = Modifier, block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit) {
    Canvas(modifier.fillMaxSize(), onDraw = block)
}
