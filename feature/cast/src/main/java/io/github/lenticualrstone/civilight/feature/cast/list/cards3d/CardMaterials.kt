package io.github.lenticualrstone.civilight.feature.cast.list.cards3d

import androidx.compose.ui.graphics.Color
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonCopy

/** Copy of the 3D card views that the shared copy objects do not hold yet (U-16 cards boards). */
internal object Cards3dCopy {
    /** Kind chip of a character card on the 기울어진 벽 and the 타로 back (board `kd`, `bcat`). */
    const val KIND = "인물"

    /** A face-down 타로 card's name in the panel (board `· · ·`). */
    const val FACE_DOWN = "· · ·"

    /** The panel line under a face-down 타로 card. */
    const val FACE_DOWN_HINT = "카드 누르기 · 뒤집기"

    /** Under the pulled-out wall card (board `fhint`). */
    const val WALL_HINT = "밀어서 넘기기 · 눌러서 열기"

    /** Over the 롤로덱스 buttons (board `rhint`). */
    const val ROLODEX_HINT = "아래로 끌면 다음 · 위로 끌면 이전"

    /** Over the 겹친 더미 buttons (board `cnt .hint`). */
    const val PILE_HINT = "밀어서 넘기기 · 누르면 열기"

    /** 타로: deal the next (or the previous) spread of five (QA 0.2.1: every character can be reached). */
    const val NEXT_SPREAD = "다음 펼침"
    const val PREV_SPREAD = "이전 펼침"

    /** 타로 panel: the focus card among all the cards (`3 / 7`), and which spread is dealt (`펼침 1 / 2`). */
    fun position(index: Int, total: Int): String = "${index + 1} / $total"
    fun spread(hand: Int, hands: Int): String = "펼침 ${hand + 1} / $hands"

    /** TalkBack's name of a card's long press: open the character, or (the lock card) the tap's unlock path. */
    const val LONG_PRESS_OPEN = IconButtonCopy.OPEN
    const val LONG_PRESS_UNLOCK = IconButtonCopy.SHOW_WITH_FINGERPRINT

    /** `03 / 11`: two digits, as the pile counter and number chip read. */
    fun twoDigits(n: Int): String = n.toString().padStart(2, '0')
}

/**
 * The materials of the cards boards that are not theme tokens (board CSS `--back`, `--sheen`, `--cshadow`,
 * `--table1` …), dark and light. Colours exactly as the boards write them.
 */
internal class CardMaterial(
    /** Back of a flipped card (cards-a `--back`). */
    val back: Color,
    /** Stripe of the patterned back (cards-a `--backline`). */
    val backLine: Color,
    /** Light band (cards-a `--sheen`). */
    val sheen: Color,
    /** Floor light under the ring (cards-a `--pool`). */
    val pool: Color,
    /** Card drop shadow, wide then tight (cards-a `--cshadow`). */
    val shadowWide: Color,
    val shadowTight: Color,
    /** cards-b `--cshadow` (pile, 타로). */
    val shadowB: Color,
    /** cards-c `--shadow` (wall, 롤로덱스). */
    val shadowC: Color,
    /** 타로 table and its line (cards-b `--table1/2`, `--tline`). */
    val table1: Color,
    val table2: Color,
    val tableLine: Color,
    /** 타로 face-down back (cards-b `--back1/2`, `--bline`, `--bmark`) and face-up card (`--card`). */
    val tarotBack1: Color,
    val tarotBack2: Color,
    val tarotLine: Color,
    val tarotMark: Color,
    val tarotCard: Color,
    /** 롤로덱스 paper, rule and tab (cards-c `--paper`, `--rule`, `--tab`). */
    val paper: Color,
    val rule: Color,
    val tab: Color,
    /** Wall plate back (cards-c `--wallb`) and the scrim over the wall (`--scrim`). */
    val wallBack: Color,
    val scrim: Color,
) {
    companion object {
        private fun rgba(r: Int, g: Int, b: Int, a: Float) = Color(r, g, b, (a * 255).toInt())
        private fun hex(v: Long) = Color(0xFF000000 or v)

        val dark = CardMaterial(
            back = hex(0x15171B),
            backLine = rgba(255, 255, 255, 0.05f),
            sheen = rgba(255, 255, 255, 0.30f),
            pool = rgba(143, 184, 230, 0.16f),
            shadowWide = rgba(0, 0, 0, 0.55f),
            shadowTight = rgba(0, 0, 0, 0.4f),
            shadowB = rgba(0, 0, 0, 0.6f),
            shadowC = rgba(0, 0, 0, 0.55f),
            table1 = hex(0x1A2230),
            table2 = hex(0x0E1218),
            tableLine = rgba(143, 184, 230, 0.12f),
            tarotBack1 = hex(0x22304A),
            tarotBack2 = hex(0x0D1422),
            tarotLine = rgba(217, 199, 163, 0.30f),
            tarotMark = hex(0xD9C7A3),
            tarotCard = hex(0x1A1C21),
            paper = hex(0x1D2026),
            rule = rgba(143, 184, 230, 0.10f),
            tab = hex(0x262A31),
            wallBack = hex(0x121418),
            scrim = rgba(5, 6, 8, 0.8f),
        )

        val light = CardMaterial(
            back = hex(0xFBFAF7),
            backLine = rgba(22, 23, 26, 0.045f),
            sheen = rgba(255, 255, 255, 0.34f),
            pool = rgba(31, 85, 144, 0.12f),
            shadowWide = rgba(30, 30, 28, 0.24f),
            shadowTight = rgba(30, 30, 28, 0.14f),
            shadowB = rgba(40, 36, 30, 0.30f),
            shadowC = rgba(40, 36, 28, 0.22f),
            table1 = hex(0xE8E4DA),
            table2 = hex(0xD3CDBF),
            tableLine = rgba(22, 23, 26, 0.10f),
            tarotBack1 = hex(0x1C3F69),
            tarotBack2 = hex(0x031C35),
            tarotLine = rgba(235, 199, 170, 0.36f),
            tarotMark = hex(0xEBC7AA),
            tarotCard = hex(0xFBFAF7),
            paper = hex(0xFFFDF8),
            rule = rgba(31, 85, 144, 0.10f),
            tab = hex(0xE8E5DC),
            wallBack = hex(0xECEAE4),
            scrim = rgba(246, 245, 241, 0.86f),
        )

        fun of(dark: Boolean): CardMaterial = if (dark) this.dark else light

        /** Text and chips over card art: fixed, both themes (board `#F4F4F6`, `rgba(10,11,14,…)`). */
        val onArt = hex(0xF4F4F6)
        val artChip = rgba(10, 11, 14, 0.42f)
        val artChipEdge = rgba(255, 255, 255, 0.18f)
        val artEdge = rgba(255, 255, 255, 0.16f)
        val artEdgeTop = rgba(255, 255, 255, 0.22f)
        val scrimInk = rgba(6, 7, 10, 0.86f)
        val scrimInkMid = rgba(6, 7, 10, 0.5f)
        val shadeInk = rgba(8, 9, 12, 0.9f)
        val glyph = rgba(255, 255, 255, 0.12f)
    }
}
