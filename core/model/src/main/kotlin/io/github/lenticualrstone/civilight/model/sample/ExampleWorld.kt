package io.github.lenticualrstone.civilight.model.sample

import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.file.WorldFile
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.PointRef
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.DEVICE
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.LAST_EDIT
import io.github.lenticualrstone.civilight.model.sample.ExampleText.link
import io.github.lenticualrstone.civilight.model.sample.ExampleText.t
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.world.EventKind
import io.github.lenticualrstone.civilight.model.world.Faction
import io.github.lenticualrstone.civilight.model.world.ItemRef
import io.github.lenticualrstone.civilight.model.world.ItemRefKind
import io.github.lenticualrstone.civilight.model.world.Place
import io.github.lenticualrstone.civilight.model.world.Term
import io.github.lenticualrstone.civilight.model.world.TimelineEvent
import io.github.lenticualrstone.civilight.model.world.WorldDate
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** `world.json` of the example: 5 terms, 7 places, 2 factions, 3 events. Nothing is hidden or trashed. */
internal object ExampleWorld {
    private fun text(value: String) = FieldValue.of(FieldPayload.Texts(listOf(value)))

    fun terms(): List<Term> {
        val orders = OrderKeys.evenlySpaced(5)
        fun term(n: Int, name: String, aliases: List<String>, description: String, related: List<ItemRef> = emptyList()) = Term(
            key = K.term(n), order = orders[n - 1], name = name, aliases = aliases,
            description = doc(300 + 10 * (n - 1)) { paragraph(description) }, related = related,
            updatedAt = LAST_EDIT, updatedBy = DEVICE,
        )
        return listOf(
            term(
                1, "오리지늄 등불", listOf("오리지늄", "빛돌"),
                "타지 않고 빛만 남기는 돌을 넣은 등. 하층 구역에서 빛은 곧 값이다.",
                related = listOf(ItemRef(ItemRefKind.FACTION, K.FACTION_LANTERN_SOCIETY), ItemRef(ItemRefKind.TERM, K.TERM_BLACKOUT)),
            ),
            term(2, "대정전", emptyList(), "1195년 하층 거리의 등이 한꺼번에 꺼진 밤."),
            term(
                3, "등불 결사", emptyList(), "꺼진 등을 세고 남은 등을 지키는 사람들의 모임.",
                related = listOf(ItemRef(ItemRefKind.FACTION, K.FACTION_LANTERN_SOCIETY)),
            ),
            term(4, "잿불의 서약", emptyList(), "등불지기장이 되는 날 하는 맹세."),
            term(
                5, "상층 송전망", emptyList(), "상층만 밝히는 전깃줄의 그물.",
                related = listOf(ItemRef(ItemRefKind.PLACE, K.PLACE_UPPER_CITY)),
            ),
        )
    }

    fun places(): List<Place> {
        val orders = OrderKeys.evenlySpaced(7)
        fun place(n: Int, name: String, description: String) = Place(
            key = K.place(n), order = orders[n - 1], name = name,
            description = doc(400 + 10 * (n - 1)) { paragraph(description) },
            updatedAt = LAST_EDIT, updatedBy = DEVICE,
        )
        return listOf(
            place(1, "하층 거리", "등이 줄지어 걸린 하층의 큰길. 대정전이 시작된 곳.")
                .copy(fields = mapOf(K.FIELD_PLACE_STATE to text("빛 있음"))),
            place(2, "결사의 문", "등불 결사의 본부. 문 위의 등 하나는 꺼지지 않는다."),
            place(3, "잿더미 시장", "해가 지면 깨어나는 시장. 꺼진 돌도 여기서는 값이 있다."),
            place(4, "상층 관문", "상층과 하층을 잇는 유일한 문.").copy(parentPlaceKey = K.PLACE_UPPER_CITY),
            place(5, "송전탑 폐허", "상층 송전망의 끝. 지금은 아무것도 흐르지 않는다."),
            place(6, "기록실", "결사의 기록이 쌓인 방. 대정전 이전의 지도가 남아 있다.")
                .copy(parentPlaceKey = K.PLACE_SOCIETY_GATE),
            place(7, "상층", "등불 도시의 위쪽 절반. 하층 구역을 내려다본다."),
        )
    }

    fun factions(): List<Faction> {
        val orders = OrderKeys.evenlySpaced(2)
        return listOf(
            Faction(
                key = K.FACTION_LANTERN_SOCIETY, order = orders[0], name = "등불 결사",
                description = doc(500) {
                    paragraph(
                        t("대정전 뒤 하층의 등을 지키려고 모인 사람들. 수장은 "),
                        link(K.link(11), LinkTargetKind.CHARACTER, K.MARTHA, "마르타"),
                        t("."),
                    )
                },
                fields = mapOf(K.FIELD_FACTION_LEADER to text("마르타")),
                updatedAt = LAST_EDIT, updatedBy = DEVICE,
            ),
            Faction(
                key = K.FACTION_UPPER_GUARD, order = orders[1], name = "상층 경비대",
                description = doc(510) { paragraph("상층 관문을 지키는 경비대. 하층으로는 내려오지 않는다.") },
                fields = mapOf(K.FIELD_FACTION_LEADER to text("카일")),
                updatedAt = LAST_EDIT, updatedBy = DEVICE,
            ),
        )
    }

    fun events(): List<TimelineEvent> {
        val orders = OrderKeys.evenlySpaced(3)
        return listOf(
            TimelineEvent(
                key = K.EVENT_BLACKOUT, order = orders[0], kind = EventKind.WORLD.open(), title = "대정전",
                description = doc(600) { paragraph("하층 거리의 등이 한꺼번에 꺼졌다. 빛이 꺼진 첫 거리.") },
                worldDate = WorldDate(1195), place = PlaceRef(PlaceRefKind.PLACE, K.PLACE_LOWER_STREET),
                relatedTermKeys = listOf(K.TERM_ORIGINIUM_LANTERN, K.TERM_BLACKOUT),
                updatedAt = LAST_EDIT, updatedBy = DEVICE,
            ),
            TimelineEvent(
                key = K.EVENT_GRID_ACCIDENT, order = orders[1], kind = EventKind.WORLD.open(), title = "상층 송전망 사고",
                description = doc(610) { paragraph("송전탑 하나가 무너져 상층의 절반이 하룻밤 어두웠다.") },
                worldDate = WorldDate(1201), place = PlaceRef(PlaceRefKind.PLACE, K.PLACE_PYLON_RUINS),
                relatedTermKeys = listOf(K.TERM_UPPER_GRID),
                revealPoint = PointRef.unitStart(K.CHAPTER_2),
                updatedAt = LAST_EDIT, updatedBy = DEVICE,
            ),
            TimelineEvent(
                key = K.EVENT_JOINS_SOCIETY, order = orders[2], kind = EventKind.STORY.open(), title = "리엔, 결사 합류",
                description = doc(620) { paragraph("세라의 안내로 리엔이 등불 결사의 견습이 된다.") },
                sceneKey = K.SCENE_1_3_2, place = PlaceRef(PlaceRefKind.PLACE, K.PLACE_SOCIETY_GATE),
                updatedAt = LAST_EDIT, updatedBy = DEVICE,
            ),
        )
    }

    fun worldFile(): WorldFile = WorldFile(places = places(), terms = terms(), factions = factions(), events = events())
}
