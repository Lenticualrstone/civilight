package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.model.UlidGenerator
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import java.util.Random

/** Fixtures over the example project 「잿빛 새벽의 등불」 (docs/status/fixture.md). */
object CastFx {
    fun doc(): ProjectDoc = ExampleProject.build()

    /** 2026-11-22 09:00 KST with deterministic keys. */
    fun ctx(seed: Long = 11): EditContext {
        val generator = UlidGenerator(clock = { ExampleClock.NOW }, random = Random(seed))
        return EditContext(newKey = generator::next, now = ExampleClock.NOW, device = "test-device", zone = ExampleClock.ZONE)
    }

    /** The document with its stored lens set to [lens]. */
    fun ProjectDoc.atLens(lens: LensPoint): ProjectDoc = copy(app = app.copy(view = app.view.copy(lens = app.view.lens.copy(point = lens))))
}
