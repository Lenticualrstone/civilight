package io.github.lenticualrstone.civilight.feature.cast.common

import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.ui.lens.LensRules

/**
 * The lens every cast screen reads values at (CST-10): the project's stored lens, read as `최신`
 * when it names a unit that is no longer live (the chip does the same).
 */
val ProjectDoc.castLens: LensPoint get() = LensRules.effective(this, app.view.lens.point)
