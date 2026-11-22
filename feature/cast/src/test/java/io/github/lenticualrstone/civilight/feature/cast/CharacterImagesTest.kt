package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.feature.cast.image.CharacterImages
import io.github.lenticualrstone.civilight.feature.cast.image.ImagePlace
import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.serialName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Image slots and crops per place (CST-13, CST-06 inner art). */
class CharacterImagesTest {
    private fun withImage(doc: ProjectDoc, slot: ImageSlot, source: String, crops: Map<String, Crop> = emptyMap(), holo: Boolean = false): ProjectDoc =
        doc.copy(assets = doc.assets.copy(imageUses = doc.assets.imageUses + ImageUse("img-$slot", ImageOwnerKind.CHARACTER.open(), K.RIEN, slot.open(), source, crops, holo)))

    @Test
    fun `no image reads 이미지 고르기 on both slots`() {
        val rows = CharacterImages.slotRows(CastFx.doc(), K.RIEN)
        assertEquals(listOf("인물 초상" to "이미지 고르기", "인물 배경" to "이미지 고르기"), rows.map { it.label to it.value })
        assertNull(CharacterImages.image(CastFx.doc(), K.RIEN, ImagePlace.AVATAR))
    }

    @Test
    fun `a preset portrait shows by name with the holo flag, a blob reads 내 이미지`() {
        val doc = withImage(CastFx.doc(), ImageSlot.CHARACTER_PORTRAIT, "preset:lantern", holo = true)
        assertEquals("등불", CharacterImages.slotRows(doc, K.RIEN).first().value)
        val card = CharacterImages.image(doc, K.RIEN, ImagePlace.CARD)!!
        assertEquals("lantern", card.presetId)
        assertTrue(card.holo)
        assertEquals(1, CharacterImages.presetIndex("lantern"))
        val blob = withImage(CastFx.doc(), ImageSlot.CHARACTER_PORTRAIT, "abc123")
        assertEquals("내 이미지", CharacterImages.slotRows(blob, K.RIEN).first().value)
    }

    @Test
    fun `each place uses its own crop - avatar 1x1, card 4x5, background 9x16 on the cover and 4x3 inside`() {
        val square = Crop(0, 0, 100, 100)
        val wide = Crop(0, 0, 400, 300)
        val tall = Crop(0, 0, 90, 160)
        val doc = withImage(
            withImage(CastFx.doc(), ImageSlot.CHARACTER_PORTRAIT, "abc", mapOf(Ratio.R1X1.serialName to square)),
            ImageSlot.CHARACTER_BACKGROUND, "def", mapOf(Ratio.R4X3.serialName to wide, Ratio.R9X16.serialName to tall), holo = true,
        )
        assertEquals(square, CharacterImages.image(doc, K.RIEN, ImagePlace.AVATAR)!!.crop)
        assertNull(CharacterImages.image(doc, K.RIEN, ImagePlace.CARD)!!.crop)
        assertEquals(tall, CharacterImages.background(doc, K.RIEN, inner = false)!!.crop)
        assertEquals(wide, CharacterImages.background(doc, K.RIEN, inner = true)!!.crop)
        // The background has no holo ("글자 뒤").
        assertFalse(CharacterImages.background(doc, K.RIEN, inner = true)!!.holo)
        assertEquals(0.75f, CharacterImages.aspect(Ratio.R3X4))
    }
}
