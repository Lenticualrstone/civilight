package io.github.lenticualrstone.civilight.feature.map.pick

import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import org.junit.Assert.assertEquals
import org.junit.Test

/** WLD-52 bulk `장소 정하기`: `PickOnMap.sceneCount` in the confirm bar (`씬 4개 → 잿더미 시장`). */
class PickCountTest {
    @Test
    fun `a bulk pick names its scene count`() {
        val doc = MapFx.doc()
        val ref = PlaceRef(PlaceRefKind.PLACE, K.PLACE_ASH_MARKET)
        val ui = PickState.build(doc, K.MAP_LOWER_DISTRICT, LensPoint.Latest, false, false, ref, null, sceneCount = 4)!!
        assertEquals("씬 4개 → 잿더미 시장", ui.bar)
    }
}
