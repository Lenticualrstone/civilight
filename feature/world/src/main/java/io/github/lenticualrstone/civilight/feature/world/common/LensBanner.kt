package io.github.lenticualrstone.civilight.feature.world.common

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.LensEditBanner
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.ui.labels.UnitLabels
import io.github.lenticualrstone.civilight.ui.lens.LensRules

/** The past-lens banner of a card: `1-5 시점 값 고치는 중` · `고치면 1-5부터 적용` · `최신으로`. */
data class LensBannerUi(val title: String, val subtitle: String)

/**
 * WLD-12: a world card viewed at a past lens edits values at that point (board `LensEdit`, the
 * cast spec's banner). Shown only when the project can be edited and the lens is not 최신.
 */
object LensBanner {
    fun of(doc: ProjectDoc, readOnly: Boolean): LensBannerUi? {
        if (readOnly) return null
        val point = LensRules.effective(doc, doc.app.view.lens.point) as? LensPoint.UnitEnd ?: return null
        val unit = doc.index.unit(point.unitKey) ?: return null
        val code = UnitLabels(doc).reference(unit) ?: return null
        return LensBannerUi(WorldCopy.lensEditTitle(code), WorldCopy.lensEditSub(code))
    }
}

/** The banner slot of `CivilightScreen`, or null when there is no banner. */
fun lensBannerSlot(banner: LensBannerUi?, onLatest: () -> Unit): (@Composable () -> Unit)? = banner?.let { b ->
    {
        LensEditBanner(
            title = b.title,
            subtitle = b.subtitle,
            actionLabel = WorldCopy.BTN_TO_LATEST,
            onAction = onLatest,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
