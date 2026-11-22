package io.github.lenticualrstone.civilight.domain.story

import io.github.lenticualrstone.civilight.domain.story.Fx.S10
import io.github.lenticualrstone.civilight.domain.story.Fx.S11
import io.github.lenticualrstone.civilight.domain.story.Fx.S5
import io.github.lenticualrstone.civilight.domain.story.Fx.S8
import io.github.lenticualrstone.civilight.domain.story.Fx.S9
import io.github.lenticualrstone.civilight.domain.story.Fx.clock
import io.github.lenticualrstone.civilight.domain.story.Fx.rel
import io.github.lenticualrstone.civilight.domain.story.Fx.unit
import io.github.lenticualrstone.civilight.domain.story.Fx.withTime
import io.github.lenticualrstone.civilight.domain.story.Fx.withUnit
import io.github.lenticualrstone.civilight.domain.story.time.CascadeMode
import io.github.lenticualrstone.civilight.domain.story.time.Durations
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.Span
import io.github.lenticualrstone.civilight.domain.story.travel.Distance
import io.github.lenticualrstone.civilight.domain.story.travel.Distances
import io.github.lenticualrstone.civilight.domain.story.travel.ModeSource
import io.github.lenticualrstone.civilight.domain.story.travel.TravelCheck
import io.github.lenticualrstone.civilight.domain.story.travel.TravelModes
import io.github.lenticualrstone.civilight.domain.story.travel.TravelVerdict
import io.github.lenticualrstone.civilight.domain.story.units.PlaceNames
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.MapScale
import io.github.lenticualrstone.civilight.model.map.ScaleUnit
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.SceneTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 20-story-time §4.8 TRV-01 … TRV-16. */
class TravelTest {
    private val doc = Fx.doc()
    private val ctx = Fx.ctx()
    private val walk get() = doc.project.travelModes.single { it.name == "도보" }
    private val horse get() = doc.project.travelModes.single { it.name == "말" }
    private val ship get() = doc.project.travelModes.single { it.name == "배" }

    private fun hart(d: ProjectDoc) = TravelCheck(d).warningFor(ExampleKeys.HART, S8, S9)

    @Test
    fun trv01_requiredTimes() {
        assertEquals(3136, TravelModes.requiredMinutes(38.0, walk))
        assertEquals("2일 4시간", Durations.required(3136))
        assertEquals(456, TravelModes.requiredMinutes(38.0, horse))
        assertEquals("약 7시간 40분", Durations.required(456))
        assertEquals(1824, TravelModes.requiredMinutes(38.0, ship))
        assertEquals("1일 6시간", Durations.required(1824))
        // The fixture route measures 38 km.
        val d = Distances(doc).between(doc.unit(S8).place!!, doc.unit(S9).place!!)
        assertEquals(Distance.Route(38.0, ExampleKeys.MAP_LOWER_DISTRICT), d)
    }

    @Test
    fun trv02_fullDaysOnly() {
        assertEquals(2880, TravelModes.requiredMinutes(30.0, walk))
        assertEquals("2일", Durations.required(2880))
        // Float noise just below a multiple of k must not drop a whole day.
        assertEquals(2880, TravelModes.requiredMinutes(29.9999999999, walk))
    }

    @Test
    fun trv03_gapS8toS9() {
        val gap = SceneTimes(doc).minutesBetween(S8, S9)!!
        assertEquals(Span(580, 760), gap)
        assertEquals("9시간 40분 – 12시간 40분", Durations.gapRange(gap))
    }

    @Test
    fun trv04_shortageForHart() {
        val w = hart(doc)!!
        assertEquals(TravelVerdict.SHORTAGE, w.verdict)
        assertEquals("▲ 이동 시간 부족", w.headline)
        assertEquals("하르트", w.characterName)
        assertEquals("도보 2일 4시간", w.needText)
        assertEquals("9시간 40분 – 12시간 40분", w.haveText)
        assertEquals("경로 38 km · 도보 15 km · 하루 8시간 · 1일째 저녁(17–20) → 2일째 05:40", w.detail)
        assertEquals("1-7 씬 2 상층 관문 → 씬 3 결사의 문", w.routeLine)
        assertEquals(760.0 / 3136, w.haveFraction, 1e-9)
        // The only ▲ of the example project.
        assertEquals(listOf(w), TravelCheck(doc).warnings)
    }

    @Test
    fun trv05_timeChip() {
        val w = hart(doc)!!
        assertEquals("씬 3 · +2일로", w.timeChip.label)
        val preview = TravelCheck.timeChipPreview(doc, w, ctx)
        assertEquals(rel(S8, 3, clock = clock(5, 40)), preview.withFollowing.unit(S9).time)
        val after = preview.result(CascadeMode.WITH_FOLLOWING)
        assertEquals(3460, SceneTimes(after).minutesBetween(S8, S9)!!.min)
        assertNull(hart(after))
        val oneDay = doc.withTime(S9, rel(S8, 2, clock = clock(5, 40)))
        assertEquals(2020, SceneTimes(oneDay).minutesBetween(S8, S9)!!.min)
        assertTrue(hart(oneDay) != null)
    }

    @Test
    fun trv06_modeChips() {
        val w = hart(doc)!!
        assertEquals(listOf("말 · 약 7시간 40분"), w.modeChips.map { it.label })
        val after = TravelCheck.applyModeChip(doc, w, w.modeChips.single(), ctx)
        assertNull(hart(after))
        assertEquals(ModeSource.OVERRIDE, TravelModes.forPair(after, ExampleKeys.HART, S8, S9)!!.source)
        assertEquals("도보", TravelModes.forPair(after, ExampleKeys.SERA, S9, S10)!!.mode.name)
        assertEquals("도보", TravelModes.forPair(after, ExampleKeys.HART, S9, S10)!!.mode.name)
    }

    @Test
    fun trv07_fingerprintTracksInputs() {
        val fp = hart(doc)!!.fingerprint
        assertEquals(fp, hart(Fx.doc())!!.fingerprint)
        val placeChanged = doc.withUnit(S9) { it.copy(place = PlaceRef(PlaceRefKind.PLACE, ExampleKeys.PLACE_ARCHIVE_ROOM)) }
        hart(placeChanged)?.let { assertNotEquals(fp, it.fingerprint) }
        val bandChanged = doc.withTime(S8, rel(Fx.S7, 0, Band.NIGHT))
        assertNotEquals(fp, hart(bandChanged)!!.fingerprint)
        val longer = withRouteKm(doc, 40.0)
        assertNotEquals(fp, hart(longer)!!.fingerprint)
        val boat = TravelModes.setOverride(doc, ExampleKeys.HART, S8, S9, ExampleKeys.TRAVEL_SHIP, ctx)
        assertNotEquals(fp, hart(boat)!!.fingerprint)
    }

    /** The 하층 구역 지도 scale set so the 결사의 문 → 상층 관문 route measures [km]. */
    private fun withRouteKm(d: ProjectDoc, km: Double): ProjectDoc {
        val map = d.maps.getValue(ExampleKeys.MAP_LOWER_DISTRICT)
        val scale = map.scale!!
        val units = 0.316667
        val newScale = MapScale(scale.a, scale.b, km * 0.25 / units, ScaleUnit.KM.open())
        return d.copy(maps = d.maps + (map.key to map.copy(scale = newScale)))
    }

    @Test
    fun trv08_routeFits() {
        val short = withRouteKm(doc, 4.0)
        val d = Distances(short).between(doc.unit(S8).place!!, doc.unit(S9).place!!) as Distance.Route
        assertEquals(4.0, d.km, 1e-9)
        assertEquals(128, TravelModes.requiredMinutes(4.0, walk))
        assertNull(hart(short))
    }

    @Test
    fun trv09_sameTimeTwoPlaces() {
        val mini = MiniProject.build(12.0, rel(MiniProject.W, 1, Band.MORNING), rel(MiniProject.X, 0, Band.MORNING))
        val check = TravelCheck(mini)
        val w = check.warnings.single()
        assertEquals(TravelVerdict.SAME_TIME, w.verdict)
        assertEquals(384, w.requiredMinutes)
        assertEquals(Span(0, 240), w.gap)
        assertEquals("▲ 같은 때 두 장소", w.headline)
        assertEquals("같은 때 두 장소 · 세라 | 1-1 씬 2 기록실 · 씬 3 잿더미 시장 · 둘 다 2일째 아침", w.compactLine)
    }

    @Test
    fun trv10_simultaneous() {
        val mini = MiniProject.build(12.0, rel(MiniProject.W, 1, clock = clock(6, 10)), rel(MiniProject.X, 0, clock = clock(6, 10)))
        val w = TravelCheck(mini).warnings.single()
        assertEquals(TravelVerdict.SIMULTANEOUS, w.verdict)
        assertEquals("▲ 동시에 두 장소", w.headline)
    }

    @Test
    fun trv11_straightLine() {
        val x = rel(MiniProject.W, 0, clock = clock(6, 0))
        val y = rel(MiniProject.X, 0, clock = clock(11, 0))
        val far = MiniProject.build(20.0, x, y, route = false)
        val w = TravelCheck(far).warnings.single()
        assertEquals(TravelVerdict.SHORTAGE, w.verdict)
        assertEquals(1600, w.requiredMinutes)
        assertEquals(Span.point(300), w.gap)
        assertEquals("직선으로도 부족 · 경로 없음", w.straightNote)
        assertTrue(w.detail.startsWith("직선 20 km · "))
        val near = MiniProject.build(2.0, x, y, route = false)
        assertTrue(TravelCheck(near).warnings.isEmpty())
        assertEquals(64, TravelModes.requiredMinutes(2.0, walk))
    }

    @Test
    fun trv12_noScale() {
        val x = rel(MiniProject.W, 0, clock = clock(6, 0))
        val y = rel(MiniProject.X, 0, clock = clock(7, 0))
        for (mini in listOf(MiniProject.build(20.0, x, y, scaled = false), MiniProject.build(20.0, x, y, nodeMap = true))) {
            val check = TravelCheck(mini)
            assertTrue(check.warnings.isEmpty())
            val item = check.noScale.single()
            assertEquals("◇ 축척 없음 · 점검 못 함", item.title)
            assertEquals("축척 맞추기", item.action)
        }
    }

    @Test
    fun trv13_samePlaceNoCheck() {
        val mini = MiniProject.build(20.0, rel(MiniProject.W, 0, clock = clock(6, 0)), rel(MiniProject.X, 0, clock = clock(6, 0)), samePlace = true)
        val check = TravelCheck(mini)
        assertTrue(check.warnings.isEmpty())
        assertTrue(check.noScale.isEmpty())
    }

    @Test
    fun trv14_flashbackAndNoPlaceAreSkipped() {
        val check = TravelCheck(doc)
        assertTrue(check.tracks.values.none { S5 in it })
        val noPlace = doc.withUnit(S11) { it.copy(place = null) }
        val c2 = TravelCheck(noPlace)
        assertTrue(c2.tracks.values.none { S11 in it })
        val item = c2.noPlace!!
        assertTrue(S11 in item.sceneKeys)
        assertEquals("◇ 장소 없음 · 씬 ${item.sceneKeys.size}개", item.title)
        assertTrue(item.detail.contains("1-8 씬 1") && item.detail.endsWith(" · 동선 · 점검에서 빠짐"))
    }

    @Test
    fun trv15_deletedPlaceSkipped() {
        val trashed = doc.copy(world = doc.world.copy(places = doc.world.places.map {
            if (it.key == ExampleKeys.PLACE_UPPER_GATE) it.copy(deletedAt = 1L, trashEntryKey = "01JZ000000TRSH000000000099") else it
        }))
        assertNull(hart(trashed))
        assertTrue(TravelCheck(trashed).warnings.isEmpty())
        assertEquals("지운 장소 · 상층 관문", PlaceNames(trashed).label(trashed.unit(S8).place)!!.wide)
    }

    @Test
    fun trv16_modePrecedence() {
        val defKey = "01JZ000000FDEF000000000099"
        val def = FieldDef(key = defKey, entityKind = EntityKind.CHARACTER.open(), name = TravelModes.CHARACTER_FIELD_NAME, type = FieldType.SHORT_TEXT.open(), order = OrderKeys.after(doc.project.fieldDefs.maxOf { it.order }))
        val withField = doc.copy(
            project = doc.project.copy(fieldDefs = doc.project.fieldDefs + def),
            cast = doc.cast.copy(characters = doc.cast.characters.map {
                if (it.key == ExampleKeys.HART) it.copy(fields = it.fields + (defKey to FieldValue.of(FieldPayload.Texts(listOf("말"))))) else it
            }),
        )
        assertEquals(ModeSource.CHARACTER_FIELD, TravelModes.forPair(withField, ExampleKeys.HART, S8, S9)!!.source)
        assertNull(hart(withField))
        val overridden = TravelModes.setOverride(withField, ExampleKeys.HART, S8, S9, ExampleKeys.TRAVEL_WALK, ctx)
        assertTrue(hart(overridden) != null)
        assertEquals(ModeSource.PROJECT_DEFAULT, TravelModes.forPair(doc, ExampleKeys.HART, S8, S9)!!.source)
        assertEquals("도보", TravelModes.forPair(doc, ExampleKeys.HART, S8, S9)!!.mode.name)
    }

    @Test
    fun chk06_settingsSwitchRowsOff() {
        val off = TravelCheck(doc, io.github.lenticualrstone.civilight.domain.story.travel.TravelSettings(checkTravel = false, checkNoPlace = false))
        assertTrue(off.warnings.isEmpty())
        assertNull(off.noPlace)
        assertFalse(TravelCheck(doc).warnings.isEmpty())
        assertTrue(TravelCheck(doc).noPlace != null)
    }
}
