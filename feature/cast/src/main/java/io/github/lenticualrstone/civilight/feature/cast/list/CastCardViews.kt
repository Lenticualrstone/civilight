package io.github.lenticualrstone.civilight.feature.cast.list

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import io.github.lenticualrstone.civilight.domain.cast.list.CastCard
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.CardStyle
import io.github.lenticualrstone.civilight.model.app.ShownView

/** What a 3D card view draws (CST-67): the narrowed characters in list order, each once. */
data class CastCardViewInput(
    val cards: List<CastCard>,
    /** `숨긴 인물 <n>` lock card at the end while hidden characters are locked; null when none. */
    val lockedCount: Int?,
    val readOnly: Boolean,
    val padding: PaddingValues,
)

/** The callbacks a 3D view needs: `열기`, the lock card's `지문으로 보기`, and the head `＋ 새 인물`. */
data class CastCardViewActions(
    val open: (Key) -> Unit,
    val unlock: () -> Unit,
    val create: (() -> Unit)?,
)

/**
 * One 3D renderer of the character card list (U-16, CST-67). Wave 3 adds the six styles
 * (`부채꼴 덱 · 회전 목마 · 겹친 더미 · 타로 펼침 · 기울어진 벽 · 롤로덱스`) as implementations of this.
 */
interface CastCardRenderer {
    @Composable
    fun Draw(input: CastCardViewInput, actions: CastCardViewActions)
}

/**
 * (0.2.0, U-16) Which view the character list shows, from `AppSettings.castCardStyle` and
 * `castToggle` (`ShownView.cast`). A style with no renderer draws the list for that session (CST-67
 * "Failure": never a blank screen), and the head toggle is drawn only when the chosen style has a
 * renderer, so no visible control does nothing. In 0.2.0 wave 2 no renderer is registered yet: the
 * list is shown and no toggle is drawn.
 */
object CastCardViews {
    /** Registered renderers by style. Empty until the 3D views land. */
    val renderers: Map<CardStyle, CastCardRenderer> = emptyMap()

    /** The style drawn: the shown view of the settings when it has a renderer, else [CardStyle.LIST]. */
    fun shown(settings: AppSettings, available: Map<CardStyle, CastCardRenderer> = renderers): CardStyle {
        val style = ShownView.cast(settings)
        return if (style.isList || style in available) style else CardStyle.LIST
    }

    /** The head toggle (`목록` · the chosen 3D view) is drawn only when the chosen style can be drawn. */
    fun showsToggle(settings: AppSettings, available: Map<CardStyle, CastCardRenderer> = renderers): Boolean =
        ShownView.showsToggle(settings.castCardStyle) && settings.castCardStyle in available
}
