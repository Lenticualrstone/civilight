package io.github.lenticualrstone.civilight.ui.lens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.lenticualrstone.civilight.designsystem.component.LensEditBanner
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.ui.labels.UnitLabels

/** Copy of the past-lens edit banner (board `LensEdit`, STO-15, CST S10, WLD-12). */
object LensEditCopy {
    const val TO_LATEST = "최신으로"

    /** `1-5 시점 값 고치는 중`. */
    fun title(code: String): String = "$code 시점 값 고치는 중"

    /** `고치면 1-5부터 적용`. */
    fun subtitle(code: String): String = "고치면 ${code}부터 적용"
}

/** What the banner says; null when no banner is due. */
data class LensEditBannerUi(val code: String, val title: String, val subtitle: String, val action: String = LensEditCopy.TO_LATEST)

/**
 * The past-lens edit banner shared by cast and world: shown on an editable project whose lens is a
 * unit end before 최신 (`1-5 시점 값 고치는 중` · `고치면 1-5부터 적용` · `최신으로`).
 */
object LensEditBanners {
    fun of(doc: ProjectDoc, readOnly: Boolean): LensEditBannerUi? {
        if (readOnly) return null
        val point = LensRules.effective(doc, doc.app.view.lens.point) as? LensPoint.UnitEnd ?: return null
        val unit = doc.index.unit(point.unitKey) ?: return null
        val code = UnitLabels(doc).reference(unit) ?: return null
        return LensEditBannerUi(code, LensEditCopy.title(code), LensEditCopy.subtitle(code))
    }
}

/**
 * The DS-28 banner for [doc]'s lens, or nothing; [onLatest] = `최신으로` (e.g. `LensController.backToLatest`).
 * It folds in and out (S8), keeping its last text while it leaves.
 */
@Composable
fun ProjectLensEditBanner(doc: ProjectDoc, readOnly: Boolean, onLatest: () -> Unit, modifier: Modifier = Modifier) {
    FoldRevealValue(LensEditBanners.of(doc, readOnly), modifier) { ui ->
        LensEditBanner(title = ui.title, actionLabel = ui.action, onAction = onLatest, subtitle = ui.subtitle)
    }
}
