package io.github.lenticualrstone.civilight.feature.cast.list

import androidx.compose.runtime.Composable
import io.github.lenticualrstone.civilight.domain.cast.list.CastCard
import io.github.lenticualrstone.civilight.domain.cast.list.LockRow
import io.github.lenticualrstone.civilight.feature.cast.image.SlotImage
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.CardBack
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.CardStage
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.CardStyle
import io.github.lenticualrstone.civilight.model.app.ShownView

/** What a 3D card view draws (CST-67): the narrowed characters in list order, each once. */
data class CastCardViewInput(
    val cards: List<CastCard>,
    /** `숨긴 인물 <n>` lock card at the end while hidden characters are locked; null when none. */
    val lock: LockRow?,
    val readOnly: Boolean,
    /** Unlocked hidden characters among [cards] (the `숨김` mark); empty while they are locked. */
    val hiddenKeys: Set<Key> = emptySet(),
    /** The card portrait of a character (4:5 crop), or null for the first letter. */
    val imageOf: (Key) -> SlotImage? = { null },
    /** The back of a flipped card (부채꼴 덱), built only for the card that turns. */
    val backOf: (Key) -> CardBack? = { null },
    /** `AppSettings.viewTilt`; the views also stop the tilt while motion is reduced (`Tilt.follows`). */
    val viewTilt: Boolean = true,
    /** A card to bring to the front (the character just made with `만들기`). */
    val focusKey: Key? = null,
)

/** The callbacks a 3D view needs: `열기`, the lock card's `지문으로 보기`, and the head `＋ 새 인물`. */
data class CastCardViewActions(
    val open: (Key) -> Unit,
    val unlock: () -> Unit,
    val create: (() -> Unit)?,
)

/**
 * One 3D renderer of the character card list (U-16, CST-67): the six styles
 * (`부채꼴 덱 · 회전 목마 · 겹친 더미 · 타로 펼침 · 기울어진 벽 · 롤로덱스`) are [StageRenderer]s.
 */
interface CastCardRenderer {
    @Composable
    fun Draw(input: CastCardViewInput, actions: CastCardViewActions)
}

/** A style drawn by the shared card stage (`list/cards3d`). */
internal class StageRenderer(private val style: CardStyle) : CastCardRenderer {
    @Composable
    override fun Draw(input: CastCardViewInput, actions: CastCardViewActions) = CardStage(style, input, actions)
}

/**
 * (0.2.0, U-16) Which view the character list shows, from `AppSettings.castCardStyle` and
 * `castToggle` (`ShownView.cast`). A style with no renderer draws the list for that session (CST-67
 * "Failure": never a blank screen), and the head toggle is drawn only when the chosen style has a
 * renderer, so no visible control does nothing. Wave 3 registers all six 3D styles.
 */
object CastCardViews {
    /** Registered renderers by style: every 3D style (U-16). */
    val renderers: Map<CardStyle, CastCardRenderer> =
        CardStyle.entries.filterNot { it.isList }.associateWith { StageRenderer(it) }

    /** The style drawn: the shown view of the settings when it has a renderer, else [CardStyle.LIST]. */
    fun shown(settings: AppSettings, available: Map<CardStyle, CastCardRenderer> = renderers): CardStyle {
        val style = ShownView.cast(settings)
        return if (style.isList || style in available) style else CardStyle.LIST
    }

    /** The head toggle (`목록` · the chosen 3D view) is drawn only when the chosen style can be drawn. */
    fun showsToggle(settings: AppSettings, available: Map<CardStyle, CastCardRenderer> = renderers): Boolean =
        ShownView.showsToggle(settings.castCardStyle) && settings.castCardStyle in available
}
