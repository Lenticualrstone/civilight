package io.github.lenticualrstone.civilight.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.R

/**
 * Pretendard v1.3.9 (OFL 1.1), the only shipped family (DS-05). Weight 800 roles request
 * [FontWeight.ExtraBold]; until `pretendard_extrabold.otf` is bundled the font matcher falls back to Bold 700.
 */
val Pretendard: FontFamily = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
)

/** Every type role of DS-06 (plus the size variants DS-06 lists inside a row). */
enum class TypeRole {
    HERO, DISPLAY, DISPLAY_M, PAGE_TITLE, STATE_TITLE, TITLE, EMPTY_TITLE, DIALOG_TITLE, HEADER_TITLE,
    HEADER_TITLE_INNER, HEADING, ITEM_TITLE, ITEM_TITLE_COMPACT, SCENE_HEAD, LINE, BODY, LEAD, LEAD_COMPACT, FIELD,
    RECORDER_LIVE, STAT, COMMAND, LABEL, TAB, BUTTON, BUTTON_LARGE, CHIP, SPEAKER, SPEAKER_NAME, SMALL, PAREN,
    MICRO, SEC, NUMBER, NUMBER_HEADER, DIR, BADGE, WORDMARK, WORDMARK_WELCOME,
}

/** The default text colour of each role (DS-06 "Colour" column). */
fun defaultColorRole(role: TypeRole): ColorRole = when (role) {
    TypeRole.BODY, TypeRole.LEAD, TypeRole.LEAD_COMPACT, TypeRole.LABEL, TypeRole.CHIP, TypeRole.MICRO,
    TypeRole.SPEAKER_NAME -> ColorRole.INK2
    TypeRole.COMMAND, TypeRole.TAB, TypeRole.SPEAKER, TypeRole.SMALL, TypeRole.PAREN, TypeRole.SEC,
    TypeRole.NUMBER, TypeRole.NUMBER_HEADER, TypeRole.DIR, TypeRole.WORDMARK, TypeRole.WORDMARK_WELCOME -> ColorRole.INK3
    TypeRole.BUTTON, TypeRole.BUTTON_LARGE -> ColorRole.ONPRI
    TypeRole.BADGE -> ColorRole.INK2
    else -> ColorRole.INK
}

/** DS-06 type roles. Colours are not part of the styles; components pick them from [CivilightColors]. */
@Immutable
data class CivilightType(
    val hero: TextStyle,
    val display: TextStyle,
    val displayM: TextStyle,
    val pageTitle: TextStyle,
    val stateTitle: TextStyle,
    val title: TextStyle,
    val emptyTitle: TextStyle,
    val dialogTitle: TextStyle,
    val headerTitle: TextStyle,
    val headerTitleInner: TextStyle,
    val heading: TextStyle,
    val itemTitle: TextStyle,
    val itemTitleCompact: TextStyle,
    val sceneHead: TextStyle,
    val line: TextStyle,
    val body: TextStyle,
    val lead: TextStyle,
    val leadCompact: TextStyle,
    val field: TextStyle,
    val recorderLive: TextStyle,
    val stat: TextStyle,
    val command: TextStyle,
    val label: TextStyle,
    val tab: TextStyle,
    val button: TextStyle,
    val buttonLarge: TextStyle,
    val chip: TextStyle,
    val speaker: TextStyle,
    val speakerName: TextStyle,
    val small: TextStyle,
    val paren: TextStyle,
    val micro: TextStyle,
    val sec: TextStyle,
    val number: TextStyle,
    val numberHeader: TextStyle,
    val dir: TextStyle,
    val badge: TextStyle,
    val wordmark: TextStyle,
    val wordmarkWelcome: TextStyle,
) {
    fun of(role: TypeRole): TextStyle = when (role) {
        TypeRole.HERO -> hero
        TypeRole.DISPLAY -> display
        TypeRole.DISPLAY_M -> displayM
        TypeRole.PAGE_TITLE -> pageTitle
        TypeRole.STATE_TITLE -> stateTitle
        TypeRole.TITLE -> title
        TypeRole.EMPTY_TITLE -> emptyTitle
        TypeRole.DIALOG_TITLE -> dialogTitle
        TypeRole.HEADER_TITLE -> headerTitle
        TypeRole.HEADER_TITLE_INNER -> headerTitleInner
        TypeRole.HEADING -> heading
        TypeRole.ITEM_TITLE -> itemTitle
        TypeRole.ITEM_TITLE_COMPACT -> itemTitleCompact
        TypeRole.SCENE_HEAD -> sceneHead
        TypeRole.LINE -> line
        TypeRole.BODY -> body
        TypeRole.LEAD -> lead
        TypeRole.LEAD_COMPACT -> leadCompact
        TypeRole.FIELD -> field
        TypeRole.RECORDER_LIVE -> recorderLive
        TypeRole.STAT -> stat
        TypeRole.COMMAND -> command
        TypeRole.LABEL -> label
        TypeRole.TAB -> tab
        TypeRole.BUTTON -> button
        TypeRole.BUTTON_LARGE -> buttonLarge
        TypeRole.CHIP -> chip
        TypeRole.SPEAKER -> speaker
        TypeRole.SPEAKER_NAME -> speakerName
        TypeRole.SMALL -> small
        TypeRole.PAREN -> paren
        TypeRole.MICRO -> micro
        TypeRole.SEC -> sec
        TypeRole.NUMBER -> number
        TypeRole.NUMBER_HEADER -> numberHeader
        TypeRole.DIR -> dir
        TypeRole.BADGE -> badge
        TypeRole.WORDMARK -> wordmark
        TypeRole.WORDMARK_WELCOME -> wordmarkWelcome
    }

    companion object {
        /** Tabular figures for the 숫자 group (DS-06). */
        const val TABULAR: String = "tnum"

        /** Builds every role; manuscript roles follow [scale] (DS-07). */
        fun create(scale: TextScale = TextScale.Default, family: FontFamily = Pretendard): CivilightType {
            fun s(
                size: Float,
                lh: Float,
                weight: FontWeight,
                tracking: TextUnit = 0.em,
                tnum: Boolean = false,
            ) = TextStyle(
                fontFamily = family,
                fontSize = size.sp,
                lineHeight = lh.sp,
                fontWeight = weight,
                letterSpacing = tracking,
                fontFeatureSettings = if (tnum) TABULAR else null,
                fontSynthesis = FontSynthesis.None,
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
                lineBreak = KoreanLineBreak,
            )
            val w800 = FontWeight.ExtraBold
            val line = scale.line
            val body = scale.body
            val paren = scale.paren
            val field = scale.field
            val live = scale.recorderLive
            return CivilightType(
                hero = s(44f, 54f, w800),
                display = s(40f, 48f, w800, (-0.01).em),
                displayM = s(36f, 46f, w800, (-0.01).em),
                pageTitle = s(28f, 38f, w800),
                stateTitle = s(26f, 34f, w800),
                title = s(22f, 28f, w800),
                emptyTitle = s(22f, 30f, w800),
                dialogTitle = s(21f, 28f, w800),
                headerTitle = s(19f, 26f, FontWeight.Bold),
                headerTitleInner = s(21f, 28f, FontWeight.Bold),
                heading = s(17f, 24f, FontWeight.Bold),
                itemTitle = s(16f, 22f, FontWeight.Bold),
                itemTitleCompact = s(15f, 22f, FontWeight.Bold),
                sceneHead = s(15f, 22f, FontWeight.Bold),
                line = s(line.sizeSp, line.lineHeightSp, FontWeight.Normal),
                body = s(body.sizeSp, body.lineHeightSp, FontWeight.Normal),
                lead = s(16f, 27f, FontWeight.Normal),
                leadCompact = s(15f, 25f, FontWeight.Normal),
                field = s(field.sizeSp, field.lineHeightSp, FontWeight.Normal),
                recorderLive = s(live.sizeSp, live.lineHeightSp, FontWeight.Normal),
                stat = s(20f, 26f, FontWeight.Normal, tnum = true),
                command = s(14f, 20f, FontWeight.Normal),
                label = s(13f, 18f, FontWeight.Medium),
                tab = s(13f, 44f, FontWeight.Medium),
                button = s(13f, 16f, FontWeight.SemiBold),
                buttonLarge = s(15f, 16f, FontWeight.SemiBold),
                chip = s(12f, 16f, FontWeight.Medium),
                speaker = s(12f, 16f, FontWeight.Medium, 0.04.em),
                speakerName = s(12f, 16f, FontWeight.SemiBold, 0.04.em),
                small = s(12f, 17f, FontWeight.Normal),
                paren = s(paren.sizeSp, paren.lineHeightSp, FontWeight.Normal),
                micro = s(11f, 14f, FontWeight.Medium),
                sec = s(11f, 14f, FontWeight.Medium, 0.08.em, tnum = true),
                number = s(12f, 16f, FontWeight.Normal, tnum = true),
                numberHeader = s(11f, 15f, FontWeight.Normal, tnum = true),
                dir = s(12f, 16f, FontWeight.Normal, tnum = true),
                badge = s(10f, 18f, FontWeight.SemiBold, tnum = true),
                wordmark = s(11f, 16f, FontWeight.Normal, 0.3.em, tnum = true),
                wordmarkWelcome = s(12f, 16f, FontWeight.Normal, 0.12.em, tnum = true),
            )
        }

        /**
         * DS-08 Korean line breaking: between 어절 (`WordBreak.Phrase`). Compose applies the phrase
         * word-break only on API 33+; on API 29–32 the platform default applies.
         */
        val KoreanLineBreak: LineBreak = LineBreak(
            strategy = LineBreak.Strategy.HighQuality,
            strictness = LineBreak.Strictness.Strict,
            wordBreak = LineBreak.WordBreak.Phrase,
        )
    }
}
