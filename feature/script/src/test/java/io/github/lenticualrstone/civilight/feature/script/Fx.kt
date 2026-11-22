package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.script.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import java.time.ZoneId

/** Shared fixtures: the example project 「잿빛 새벽의 등불」 and a deterministic edit context. */
object Fx {
    val zone: ZoneId = ZoneId.of("Asia/Seoul")

    val example: ProjectDoc by lazy { ExampleProject.build() }

    /** Keys `01JZ000000TEST0000000000001`… in order, so new blocks are predictable. */
    fun ctx(): EditContext {
        var n = 0
        return EditContext(now = ExampleClock.NOW, zone = zone, device = "test", newKey = { "01JZ000000TEST" + (++n).toString().padStart(12, '0') })
    }

    fun block(doc: ProjectDoc, key: Key) = doc.index.block(key) ?: error("no block $key")
}
