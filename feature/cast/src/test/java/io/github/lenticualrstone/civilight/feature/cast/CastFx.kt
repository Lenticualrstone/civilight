package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.feature.cast.common.toAction
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.UlidGenerator
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import java.util.Random
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Fixtures over the example project 「잿빛 새벽의 등불」 (docs/status/fixture.md). */
object CastFx {
    fun doc(): ProjectDoc = ExampleProject.build()

    /** 2026-11-22 09:00 KST with deterministic keys. */
    fun ctx(seed: Long = 11): EditContext {
        val generator = UlidGenerator(clock = { ExampleClock.NOW }, random = Random(seed))
        return EditContext(newKey = generator::next, now = ExampleClock.NOW, device = "test-device", zone = ExampleClock.ZONE)
    }

    /** The same clock and keys as a `domain:actions` context. */
    fun action(seed: Long = 11): ActionContext = ctx(seed).toAction()

    /** The document with its stored lens set to [lens]. */
    fun ProjectDoc.atLens(lens: LensPoint): ProjectDoc = copy(app = app.copy(view = app.view.copy(lens = app.view.lens.copy(point = lens))))

    /** The fixture with 노아 live (fixture.md conflict #22 recipe: clear his mark, drop `TRASH_NOAH`). */
    fun noahLive(doc: ProjectDoc = doc()): ProjectDoc = doc.copy(
        cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.NOAH) it.copy(deletedAt = null, trashEntryKey = null) else it }),
        trash = doc.trash.copy(entries = doc.trash.entries.filter { it.key != K.TRASH_NOAH }),
    )

    /** Adds a character at the end of the cast. */
    fun addCharacter(doc: ProjectDoc, key: Key, name: String, recordNo: Int, fields: Map<Key, FieldValue> = emptyMap(), oneLiner: String? = null): ProjectDoc {
        val last = doc.cast.characters.maxOf { it.order }
        val c = Character(key = key, recordNo = recordNo, name = name, order = OrderKeys.after(last), fields = fields, oneLiner = oneLiner)
        return doc.copy(cast = doc.cast.copy(characters = doc.cast.characters + c))
    }

    /** A fresh ULID-shaped test key. */
    fun key(family: String, n: Int): Key = "01JZ0000" + family + n.toString().padStart(14, '0')
}
