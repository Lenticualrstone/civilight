package io.github.lenticualrstone.civilight.feature.map

import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.model.UlidGenerator
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleProject

/** The example project 「잿빛 새벽의 등불」 (docs/status/fixture.md) and deterministic edit contexts. */
object MapFx {
    val K = ExampleKeys

    fun doc(): ProjectDoc = ExampleProject.build()

    fun ctx(seed: Long = 7): EditContext {
        val gen = UlidGenerator(clock = { ExampleClock.NOW }, random = java.util.Random(seed))
        return EditContext(newKey = gen::next, now = { ExampleClock.NOW }, device = "test-device")
    }

    fun blob(hash: String = "a".repeat(64), w: Int? = 6000, h: Int? = 4000, source: BlobSource = BlobSource.GALLERY): Blob =
        Blob(hash = hash, fileExt = "jpg", mime = "image/jpeg", bytes = 1000, width = w, height = h, source = source.open(), importedAt = 0)
}
