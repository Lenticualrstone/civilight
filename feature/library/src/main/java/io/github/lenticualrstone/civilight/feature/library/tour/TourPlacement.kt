package io.github.lenticualrstone.civilight.feature.library.tour

/**
 * Where the tooltip card of a tour step goes (LIB-19, DS-49), in the host's pixels: below the
 * target (arrow on top) when the card fits there, else above it (arrow at the bottom), else over the
 * middle without an arrow. The arrow points at the target's centre, kept 24 dp inside the card.
 * On a host wider than the card (the inner screen, spec "same, anchored to target") the card also
 * moves sideways under its target by [cardShift] from the centred place, kept [edge] inside the host;
 * at cover width the card spans the host, so the shift is 0.
 */
data class TourPlacement(val cardTop: Float, val arrow: Arrow, val arrowX: Float, val cardShift: Float = 0f) {
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
            hostWidth: Float = 0f,
        ): TourPlacement {
            val middle = ((hostHeight - cardHeight) / 2f).coerceAtLeast(edge)
            if (targetTop == null || targetBottom == null || targetCenterX == null) return TourPlacement(middle, Arrow.NONE, 0f)
            // With the host's width known, the card's left is where it will stand, not where it stood (no lag while it glides).
            val left = if (hostWidth > 0f && cardWidth > 0f) cardLeftFor(targetCenterX, hostWidth, cardWidth, edge) else cardLeft
            val shift = if (hostWidth > 0f && cardWidth > 0f) left - (hostWidth - cardWidth) / 2f else 0f
            val arrowX = (targetCenterX - left).coerceIn(arrowInset, (cardWidth - arrowInset).coerceAtLeast(arrowInset))
            val below = targetBottom + gap
            if (below + cardHeight <= hostHeight - edge) return TourPlacement(below, Arrow.TOP, arrowX, shift)
            val above = targetTop - gap - cardHeight
            if (above >= edge) return TourPlacement(above, Arrow.BOTTOM, arrowX, shift)
            return TourPlacement(middle, Arrow.NONE, arrowX)
        }

        /**
         * The card's left edge under a target centred at [targetCenterX]: centred on it, kept [edge]
         * inside the host; centred in the host when the card leaves no room to move.
         */
        fun cardLeftFor(targetCenterX: Float, hostWidth: Float, cardWidth: Float, edge: Float): Float {
            val centred = (hostWidth - cardWidth) / 2f
            // Cover width: the card already spans the host less its 16 dp sides (1 px for rounding).
            if (hostWidth - cardWidth <= 2f * edge + 1f) return centred
            return (targetCenterX - cardWidth / 2f).coerceIn(edge, hostWidth - edge - cardWidth)
        }

        /**
         * Whether a marked target ([left] … [bottom], in host pixels) can take the hole: it has an area
         * and lies at least partly inside the host ([hostWidth] × [hostHeight], both measured). A target
         * scrolled away or clipped by its parent reports an empty or outside rect; its step then shows the
         * card in the middle without a hole, never a hole around nothing.
         */
        fun usableTarget(left: Float, top: Float, right: Float, bottom: Float, hostWidth: Float, hostHeight: Float): Boolean =
            hostWidth > 0f && hostHeight > 0f &&
                right - left > 0f && bottom - top > 0f &&
                right > 0f && left < hostWidth && bottom > 0f && top < hostHeight

        /**
         * The hole of a step (host pixels as `[left, top, right, bottom]`): its own target while that is
         * [usableTarget], else its fallback while that is, else null (the card sits in the middle).
         */
        fun pickTarget(own: FloatArray?, fallback: FloatArray?, hostWidth: Float, hostHeight: Float): FloatArray? =
            listOf(own, fallback).firstOrNull { it != null && usableTarget(it[0], it[1], it[2], it[3], hostWidth, hostHeight) }
    }
}
