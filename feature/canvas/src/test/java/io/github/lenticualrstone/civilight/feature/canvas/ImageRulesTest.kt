package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.feature.canvas.image.CropEdit
import io.github.lenticualrstone.civilight.feature.canvas.image.CropMath
import io.github.lenticualrstone.civilight.feature.canvas.image.CropSession
import io.github.lenticualrstone.civilight.feature.canvas.image.HoloRules
import io.github.lenticualrstone.civilight.feature.canvas.image.HoloSensorGate
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageEdits
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageOwner
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageOwners
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageSlots
import io.github.lenticualrstone.civilight.feature.canvas.image.PixelSize
import io.github.lenticualrstone.civilight.feature.canvas.image.PresetImages
import io.github.lenticualrstone.civilight.feature.canvas.image.Tilt
import io.github.lenticualrstone.civilight.feature.canvas.image.TiltSensorPort
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.ProjectFileSet
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.open
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Image slots, crops, presets, image writes and holo (CNV-25 … CNV-30; spec §4.5, §4.7). */
class ImageRulesTest {
    private val original = PixelSize(3024, 4032)
    private val cover = ImageOwner(ImageOwnerKind.PROJECT, K.PROJECT, ImageSlot.PROJECT_COVER)
    private val portrait = ImageOwner(ImageOwnerKind.CHARACTER, K.RIEN, ImageSlot.CHARACTER_PORTRAIT)
    private val background = ImageOwner(ImageOwnerKind.CHARACTER, K.RIEN, ImageSlot.CHARACTER_BACKGROUND)

    private fun blob(hash: String = "a".repeat(64), importedAt: Long = 1L) = Blob(
        hash = hash, fileExt = "jpg", mime = "image/jpeg", bytes = 1000, width = 3024, height = 4032,
        source = BlobSource.GALLERY.open(), importedAt = importedAt,
    )

    @Test
    fun `T-CNV-30 crop readout`() {
        val r = CropMath.cropReadout(original, Ratio.R3X4, CropEdit(zoom = 1.4f))
        assertEquals(2160, r.crop.w)
        assertEquals(2880, r.crop.h)
        assertEquals("2160 × 2880 · 원본 3024 × 4032", r.label)
    }

    @Test
    fun `T-CNV-31 zoom 1 is the largest frame`() {
        val square = CropMath.crop(original, Ratio.R1X1, CropEdit(zoom = 1f))
        assertEquals(3024 to 3024, square.w to square.h)
        val card = CropMath.crop(original, Ratio.R3X4, CropEdit(zoom = 1f))
        assertEquals(3024 to 4032, card.w to card.h)
        assertEquals(0 to 0, card.x to card.y)
        assertEquals(504, square.y)
    }

    @Test
    fun `T-CNV-32 slot registry`() {
        assertEquals(listOf(Ratio.R3X4, Ratio.R1X1), ImageSlots.ratios(ImageSlot.PROJECT_COVER))
        assertEquals(listOf(Ratio.R4X5, Ratio.R1X1), ImageSlots.ratios(ImageSlot.CHARACTER_PORTRAIT))
        assertEquals(listOf(Ratio.R9X16, Ratio.R4X3), ImageSlots.ratios(ImageSlot.CHARACTER_BACKGROUND))
        assertEquals(listOf(Ratio.R16X9), ImageSlots.ratios(ImageSlot.CARD_REFERENCE))
        assertEquals(listOf(Ratio.R21X9), ImageSlots.ratios(ImageSlot.PAGE_HEADER))
        assertTrue(ImageSlots.holoAllowed(ImageSlot.PROJECT_COVER))
        assertTrue(ImageSlots.holoAllowed(ImageSlot.CHARACTER_PORTRAIT))
        assertTrue(ImageSlots.holoAllowed(ImageSlot.CARD_REFERENCE))
        assertFalse(ImageSlots.holoAllowed(ImageSlot.CHARACTER_BACKGROUND))
        assertFalse(ImageSlots.holoAllowed(ImageSlot.PAGE_HEADER))
        assertEquals(listOf("카드 3:4", "목록 1:1"), ImageSlots.spec(ImageSlot.PROJECT_COVER).chips)
        assertEquals("표지 이미지", ImageSlots.spec(ImageSlot.PROJECT_COVER).pickTitle)
        assertEquals("홀로 없음 · 글자 뒤", ImageSlots.spec(ImageSlot.CHARACTER_BACKGROUND).holoNote)
    }

    @Test
    fun `T-CNV-33 separate crops per ratio`() {
        val ctx = CanvasFx.ctx()
        val b = blob()
        var doc = ImageEdits.addBlob(CanvasFx.s0(), b)
        var session = CropSession.start(ImageSlot.PROJECT_COVER, original, ImageEdits.use(doc, cover), b.hash, holo = false)
            .select(Ratio.R1X1).zoom(1.2f)
        doc = ImageEdits.placeBlob(doc, cover, b.hash, session.crops(), session.holo, ctx)
        val before = ImageEdits.use(doc, cover)!!
        val square = before.cropFor(Ratio.R1X1)
        assertNotNull(square)
        assertNotNull(before.cropFor(Ratio.R3X4))

        session = CropSession.start(ImageSlot.PROJECT_COVER, original, before, b.hash, holo = false).zoom(1.4f)
        assertEquals(setOf(Ratio.R3X4), session.touched)
        doc = ImageEdits.placeBlob(doc, cover, b.hash, session.crops(), session.holo, ctx)
        val after = ImageEdits.use(doc, cover)!!
        assertTrue(CropMath.same(square, after.cropFor(Ratio.R1X1)))
        assertEquals(2160, after.cropFor(Ratio.R3X4)!!.w)
        assertEquals(before.key, after.key)
        assertEquals(1, doc.assets.imageUses.count { cover.matches(it) })
        // The original is untouched: one blob row, same size.
        assertEquals(listOf(b), doc.assets.blobs)
    }

    @Test
    fun `T-CNV-34 holo defaults and the background never holds holo`() {
        val ctx = CanvasFx.ctx()
        val doc = CanvasFx.s0()
        assertFalse(HoloRules.initial(ImageSlot.CHARACTER_PORTRAIT, null, holoDefaultForNewImages = false))
        val off = ImageEdits.placePreset(doc, portrait, "ash-dawn", HoloRules.initial(ImageSlot.CHARACTER_PORTRAIT, null, false), ctx)
        assertFalse(ImageEdits.use(off, portrait)!!.holo)

        val p = HoloRules.initial(ImageSlot.CHARACTER_PORTRAIT, null, holoDefaultForNewImages = true)
        val bg = HoloRules.initial(ImageSlot.CHARACTER_BACKGROUND, null, holoDefaultForNewImages = true)
        var on = ImageEdits.placePreset(doc, portrait, "ash-dawn", p, ctx)
        on = ImageEdits.placePreset(on, background, "nebula", bg, ctx)
        assertTrue(ImageEdits.use(on, portrait)!!.holo)
        assertFalse(ImageEdits.use(on, background)!!.holo)
        assertFalse(HoloRules.showToggle(ImageSlot.CHARACTER_BACKGROUND))
        assertTrue(HoloRules.showToggle(ImageSlot.CHARACTER_PORTRAIT))
        // The write itself refuses holo where it is not allowed.
        val forced = ImageEdits.placePreset(doc, background, "nebula", holo = true, ctx = ctx)
        assertFalse(ImageEdits.use(forced, background)!!.holo)
        assertFalse(ImageEdits.use(ImageEdits.setHolo(forced, background, true, ctx), background)!!.holo)
        // An existing image keeps its own value.
        assertTrue(HoloRules.initial(ImageSlot.CHARACTER_PORTRAIT, ImageEdits.use(on, portrait), holoDefaultForNewImages = false))
    }

    @Test
    fun `T-CNV-35 one original used twice is stored once`() {
        val ctx = CanvasFx.ctx()
        val b = blob()
        var doc = ImageEdits.addBlob(CanvasFx.s0(), b)
        doc = ImageEdits.addBlob(doc, b)
        doc = ImageEdits.placeBlob(doc, cover, b.hash, emptyMap(), false, ctx)
        doc = ImageEdits.placeBlob(doc, portrait, b.hash, emptyMap(), false, ctx)
        val index = exportedAssets(doc)
        assertEquals(1, index.getValue("blobs").jsonArray.size)
        val sources = index.getValue("imageUses").jsonArray.map { it.jsonObject.getValue("source").jsonPrimitive.content }
        assertEquals(2, sources.count { it == b.hash })
    }

    @Test
    fun `T-CNV-36 presets are stored by name`() {
        val doc = ImageEdits.placePreset(CanvasFx.s0(), cover, "ash-dawn", false, CanvasFx.ctx())
        val index = exportedAssets(doc)
        val use = index.getValue("imageUses").jsonArray.single { it.jsonObject.getValue("slot").jsonPrimitive.content == "projectCover" }
        assertEquals("preset:ash-dawn", use.jsonObject.getValue("source").jsonPrimitive.content)
        assertTrue(index["blobs"]?.jsonArray.isNullOrEmpty())
        assertEquals("ash-dawn", PresetImages.ofSource("preset:ash-dawn")!!.id)
    }

    @Test
    fun `presets are the eight of the table, in every slot`() {
        assertEquals(
            listOf("잿빛 새벽", "등불", "심해", "안개 숲", "사막 유적", "성운", "붉은 성채", "설원"),
            PresetImages.all.map { it.name },
        )
        assertEquals(
            listOf("ash-dawn", "lantern", "deep-sea", "mist-forest", "desert-ruin", "nebula", "red-citadel", "snowfield"),
            PresetImages.all.map { it.id },
        )
        assertEquals(listOf(0, 1, 2, 3, 0, 1, 2, 3), PresetImages.all.map { it.shardSet })
        assertEquals(0x2B2F3AL, PresetImages.all[0].c0)
        assertEquals(0xF6F8FAL, PresetImages.all[7].s3)
        PresetImages.all.forEach { p -> assertEquals(3, p.shards.size) }
        assertNull(PresetImages.ofSource("a".repeat(64)))
        assertNull(PresetImages.ofSource("preset:unknown"))
    }

    @Test
    fun `crop session edits rotate, mirror and reset one ratio at a time`() {
        val s = CropSession.start(ImageSlot.PROJECT_COVER, original, null, "h", holo = true)
        assertEquals(Ratio.R3X4, s.current)
        assertTrue(s.holo)
        val turned = s.rotate()
        assertEquals(90, turned.edit.rotation)
        assertEquals("2160 × 2880 · 원본 3024 × 4032", s.zoom(1.4f).readout().label)
        // Rotated original is 4032 × 3024: the 3:4 frame is 2268 × 3024 at zoom 1.
        assertEquals(2268 to 3024, turned.readout().crop.w to turned.readout().crop.h)
        // Mirror on: the centre follows the shown (mirrored) picture through the turn.
        val off = CropEdit(zoom = 2f, cx = 0.3f, cy = 0.4f, flipH = true)
        val mirroredTurn = CropMath.rotate(original, Ratio.R1X1, off)
        assertEquals(0.4f, mirroredTurn.cx, 0.001f)
        assertEquals(0.7f, mirroredTurn.cy, 0.001f)
        val plainTurn = CropMath.rotate(original, Ratio.R1X1, off.copy(flipH = false))
        assertEquals(0.6f, plainTurn.cx, 0.001f)
        assertEquals(0.3f, plainTurn.cy, 0.001f)
        val flipped = turned.flip()
        assertTrue(flipped.edit.flipH)
        assertEquals(CropEdit(), flipped.reset().edit)
        val other = flipped.select(Ratio.R1X1)
        assertEquals(CropEdit(), other.edit)
        assertEquals(setOf(Ratio.R3X4, Ratio.R1X1), other.touched)
        // Background: holo is refused.
        assertFalse(CropSession.start(ImageSlot.CHARACTER_BACKGROUND, original, null, "h", holo = true).holo)
        // Saved state round trip (CNV-35).
        assertEquals(other, CropSession.decode(other.encode()))
    }

    @Test
    fun `panning keeps the frame inside the image`() {
        val s = CropSession.start(ImageSlot.PROJECT_COVER, original, null, "h", holo = false).zoom(2f)
        val far = s.pan(-100_000f, -100_000f)
        val crop = far.readout().crop
        assertEquals(original.w - crop.w, crop.x)
        assertEquals(original.h - crop.h, crop.y)
        val back = CropMath.editOf(original, crop)
        assertTrue(CropMath.same(crop, CropMath.crop(original, Ratio.R3X4, back)))
    }

    @Test
    fun `zoom steps run from 1 to 4 by tenths`() {
        assertEquals(31, CropMath.zoomSteps)
        assertEquals(1.4f, CropMath.zoomOfStep(4))
        assertEquals(4, CropMath.stepOfZoom(1.4f))
        assertEquals("1.4×", io.github.lenticualrstone.civilight.feature.canvas.image.ImageCopy.zoomValue(1.4f))
        assertEquals("1.0×", io.github.lenticualrstone.civilight.feature.canvas.image.ImageCopy.zoomValue(1f))
    }

    @Test
    fun `frame sits centred in the stage`() {
        val f = CropMath.frameIn(390f, 470f, 3f / 4f, 24f)
        assertEquals(422f, f.height, 0.01f)
        assertEquals(316.5f, f.width, 0.01f)
        assertEquals((390f - 316.5f) / 2, f.left, 0.01f)
    }

    @Test
    fun `my images are this project's picked originals, newest first`() {
        val older = blob("b".repeat(64), importedAt = 1)
        val newer = blob("c".repeat(64), importedAt = 5)
        val map = blob("d".repeat(64), importedAt = 9).copy(source = BlobSource.SKETCH.open())
        val audio = blob("e".repeat(64), importedAt = 9).copy(mime = "audio/mp4", source = BlobSource.RECORDER.open())
        var doc: ProjectDoc = CanvasFx.s0()
        listOf(older, newer, map, audio).forEach { doc = ImageEdits.addBlob(doc, it) }
        assertEquals(listOf(newer, older), ImageOwners.myImages(doc))
        assertEquals("잿빛 새벽의 등불", ImageOwners.name(doc, ImageOwnerKind.PROJECT, K.PROJECT))
        assertEquals("리엔", ImageOwners.name(doc, ImageOwnerKind.CHARACTER, K.RIEN))
    }

    @Test
    fun `T-CNV-55 holo sensor lifecycle`() {
        val port = FakePort()
        val gate = HoloSensorGate(port)
        gate.shown()
        assertTrue(port.registered)
        gate.hidden()
        assertFalse(port.registered)
        assertEquals(1 to 1, port.registers to port.unregisters)

        gate.reducedMotion(true)
        gate.shown()
        assertFalse(port.registered)
        assertNull(HoloRules.sweepOnTurnOn(reduced = true))
        assertEquals(480, HoloRules.sweepOnTurnOn(reduced = false)!!.durationMs)

        val noGyro = HoloSensorGate(FakePort(available = false))
        noGyro.shown()
        assertFalse(noGyro.registered)
        assertFalse(noGyro.usesSensor)
    }

    @Test
    fun `tilt follows the device, the drag and sways back`() {
        assertEquals(Tilt(1f, -0.4f), HoloRules.fromDevice(pitchDeg = -10f, rollDeg = 40f, basePitchDeg = 0f, baseRollDeg = 0f))
        assertEquals(Tilt(0.5f, 0f), HoloRules.fromDrag(Tilt.ZERO, 75f, 0f, 300f, 430f))
        val start = HoloRules.sway(Tilt(1f, 0f), 0)
        assertEquals(1f, start.x, 0.001f)
        assertTrue(kotlin.math.abs(HoloRules.sway(Tilt(1f, 0f), 2000).x) < 0.2f)
        assertEquals(Tilt.ZERO, HoloRules.sway(Tilt(1f, 0f), HoloRules.SWAY_END_MS))
        assertEquals(Tilt(-1f, 0f), HoloRules.sweepTilt(0f))
        assertEquals(Tilt(1f, 0f), HoloRules.sweepTilt(1f))
    }

    private fun exportedAssets(doc: ProjectDoc) =
        ProjectFileSet.split(doc).encode().getValue(PackagePaths.ASSETS_INDEX).jsonObject

    private class FakePort(override val available: Boolean = true) : TiltSensorPort {
        var registered = false
        var registers = 0
        var unregisters = 0

        override fun register() {
            registered = true
            registers++
        }

        override fun unregister() {
            registered = false
            unregisters++
        }
    }
}
