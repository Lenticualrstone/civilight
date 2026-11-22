package io.github.lenticualrstone.civilight.feature.library.tour

/**
 * Where the tooltip card of a tour step goes (LIB-19, DS-49), in the host's pixels: below the
 * target (arrow on top) when the card fits there, else above it (arrow at the bottom), else over the
 * middle without an arrow. The arrow points at the target's centre, kept 24 dp inside the card.
 */
data class TourPlacement(val cardTop: Float, val arrow: Arrow, val arrowX: Float) {
    enum class Arrow { TOP, BOTTOM, NONE }

    companion object {
        /**
         * [targetTop] … [targetBottom] / [targetCenterX] = the target in host pixels (null = no
         * target: the card sits in the middle); [hostHeight]; [cardHeight] measured; [cardLeft] and
         * [cardWidth] = the card's horizontal span; [gap] between the hole's frame and the card.
         */
        fun of(
            targetTop: Float?,
            targetBottom: Float?,
            targetCenterX: Float?,
            hostHeight: Float,
            cardHeight: Float,
            cardLeft: Float,
            cardWidth: Float,
            gap: Float,
            edge: Float,
            arrowInset: Float,
        ): TourPlacement {
            val middle = ((hostHeight - cardHeight) / 2f).coerceAtLeast(edge)
            if (targetTop == null || targetBottom == null || targetCenterX == null) return TourPlacement(middle, Arrow.NONE, 0f)
            val arrowX = (targetCenterX - cardLeft).coerceIn(arrowInset, (cardWidth - arrowInset).coerceAtLeast(arrowInset))
            val below = targetBottom + gap
            if (below + cardHeight <= hostHeight - edge) return TourPlacement(below, Arrow.TOP, arrowX)
            val above = targetTop - gap - cardHeight
            if (above >= edge) return TourPlacement(above, Arrow.BOTTOM, arrowX)
            return TourPlacement(middle, Arrow.NONE, arrowX)
        }
    }
}
