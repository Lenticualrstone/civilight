package io.github.lenticualrstone.civilight.model.sample

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.cast.CalcSpec
import io.github.lenticualrstone.civilight.model.cast.DatePrecision
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldOption
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.RelationCategory
import io.github.lenticualrstone.civilight.model.cast.RelationKind
import io.github.lenticualrstone.civilight.model.cast.RelationSymmetry
import io.github.lenticualrstone.civilight.model.cast.Tag
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.BundleNames
import io.github.lenticualrstone.civilight.model.project.CastSettings
import io.github.lenticualrstone.civilight.model.project.FieldBundle
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.project.ProjectCounters
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.project.TravelMode
import io.github.lenticualrstone.civilight.model.project.WorldTime
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.DEVICE
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.LAST_EDIT
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.PROJECT_CREATED
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** `project.json` of the example: settings, field definitions, tags, relation kinds, travel modes. */
internal object ExampleSettings {
    const val NAME = "잿빛 새벽의 등불"
    const val ONE_LINER = "꺼진 등불이 깜박인 새벽, 하층 거리의 견습 등불지기가 이름 없는 남자를 만난다."

    /** `인물 기록` numbers 1 … 12 are issued. */
    const val NEXT_CHARACTER_RECORD_NO = 13

    fun project(): Project = Project(
        key = K.PROJECT,
        name = NAME,
        oneLiner = ONE_LINER,
        format = ProjectFormat.SERIAL.open(),
        rating = Rating.R15.open(),
        worldTime = WorldTime(mode = WorldTimeMode.YEAR_ONLY.open(), storyStartYear = 1200),
        cast = CastSettings(bundleName = BundleNames(character = "게임 프로필")),
        travelModes = TravelMode.defaults(
            newKey = sequenceOf(K.TRAVEL_WALK, K.TRAVEL_HORSE, K.TRAVEL_SHIP).iterator()::next,
            at = PROJECT_CREATED,
            by = DEVICE,
        ),
        fieldBundleAtCreation = FieldBundle.GAME_PROFILE.open(),
        counters = ProjectCounters(nextCharacterRecordNo = NEXT_CHARACTER_RECORD_NO),
        fieldDefs = fieldDefs(),
        tags = tags(),
        relationKinds = relationKinds(),
        createdAt = PROJECT_CREATED,
        updatedAt = LAST_EDIT,
        updatedBy = DEVICE,
    )

    private fun option(def: Int, n: Int, label: String, count: Int) =
        FieldOption(key = K.fieldOption(def, n), label = label, order = OrderKeys.evenlySpaced(count)[n - 1])

    /**
     * The `게임 프로필` bundle (CST-24) in the order of the `RCFields` board, 생일 after 나이, plus one
     * place field and one faction field.
     */
    fun fieldDefs(): List<FieldDef> {
        val characterOrders = OrderKeys.evenlySpaced(11)
        fun def(
            n: Int,
            name: String,
            type: FieldType,
            origin: String,
            inHeader: Boolean = false,
            inList: Boolean = false,
            inFilter: Boolean = false,
        ) = FieldDef(
            key = K.fieldDef(n), entityKind = EntityKind.CHARACTER.open(), name = name, type = type.open(),
            order = characterOrders[n - 1], inHeader = inHeader, inList = inList, inFilter = inFilter,
            bundleOrigin = origin, updatedAt = PROJECT_CREATED, updatedBy = DEVICE,
        )
        return listOf(
            def(1, "출생", FieldType.DATE, "basic.birth").copy(datePrecision = DatePrecision.YEAR.open()),
            def(2, "나이", FieldType.CALC, "basic.age", inHeader = true, inList = true)
                .copy(calc = CalcSpec(sourceFieldDefKey = K.FIELD_BIRTH)),
            def(3, "생일", FieldType.ANNIVERSARY, "basic.birthday"),
            def(4, "소속", FieldType.SHORT_TEXT, "basic.affiliation", inHeader = true, inList = true),
            def(5, "신장", FieldType.NUMBER, "gameProfile.height", inHeader = true)
                .copy(unit = "cm", exportName = "height"),
            def(6, "성별", FieldType.CHOICE, "gameProfile.gender", inFilter = true).copy(
                options = listOf(option(6, 1, "여", 3), option(6, 2, "남", 3), option(6, 3, "밝히지 않음", 3)),
                allowFreeText = false,
            ),
            def(7, "종족", FieldType.CHOICE, "gameProfile.species", inHeader = true, inFilter = true).copy(
                options = listOf(
                    option(7, 1, "인간", 4), option(7, 2, "수인", 4), option(7, 3, "요정", 4), option(7, 4, "자동인형", 4),
                ),
                allowFreeText = false,
            ),
            def(8, "성격", FieldType.LONG_TEXT, "basic.personality"),
            def(9, "코드네임", FieldType.SHORT_TEXT, "gameProfile.codename").copy(shortTextMultiple = false),
            def(10, "출신지", FieldType.LINK, "gameProfile.origin", inFilter = true)
                .copy(linkTargetKind = FieldLinkTargetKind.PLACE.open()),
            def(11, "특기", FieldType.MULTI_CHOICE, "gameProfile.specialty", inFilter = true).copy(
                options = listOf(
                    option(11, 1, "등불 수리", 7), option(11, 2, "길 찾기", 7), option(11, 3, "검술", 7),
                    option(11, 4, "기록", 7), option(11, 5, "흥정", 7), option(11, 6, "성직자", 7), option(11, 7, "치료", 7),
                ),
            ),
            FieldDef(
                key = K.FIELD_PLACE_STATE, entityKind = EntityKind.PLACE.open(), name = "상태",
                type = FieldType.SHORT_TEXT.open(), order = OrderKeys.FIRST, inHeader = true,
                updatedAt = PROJECT_CREATED, updatedBy = DEVICE,
            ),
            FieldDef(
                key = K.FIELD_FACTION_LEADER, entityKind = EntityKind.FACTION.open(), name = "수장",
                type = FieldType.SHORT_TEXT.open(), order = OrderKeys.FIRST, inHeader = true,
                updatedAt = PROJECT_CREATED, updatedBy = DEVICE,
            ),
        )
    }

    fun tags(): List<Tag> {
        val names = listOf("주연", "등불 결사", "하층 구역", "상층")
        val orders = OrderKeys.evenlySpaced(names.size)
        return names.mapIndexed { i, name ->
            Tag(key = K.tag(i + 1), name = name, order = orders[i], updatedAt = PROJECT_CREATED, updatedBy = DEVICE)
        }
    }

    /** Rule 13 §3 seed list: 7 symmetric and 4 pair 사이 kinds, 11 마음 kinds. */
    fun relationKinds(): List<RelationKind> {
        val symmetric = listOf("동료", "형제자매", "배우자", "연인", "친구", "거래 상대", "적")
        val pairs = listOf("부모" to "자식", "스승" to "제자", "상관" to "부하", "선배" to "후배")
        val minds = listOf("신뢰", "의지", "동경", "호감", "보호", "경계", "불신", "질투", "증오", "죄책감", "무관심")
        val betweenOrders = OrderKeys.evenlySpaced(symmetric.size + pairs.size)
        val mindOrders = OrderKeys.evenlySpaced(minds.size)
        var n = 0
        fun next(): Key = K.relationKind(++n)
        val out = mutableListOf<RelationKind>()
        symmetric.forEachIndexed { i, name ->
            out += RelationKind(
                key = next(), category = RelationCategory.BETWEEN.open(), symmetry = RelationSymmetry.SYMMETRIC.open(),
                name = name, order = betweenOrders[i], updatedAt = PROJECT_CREATED, updatedBy = DEVICE,
            )
        }
        pairs.forEachIndexed { i, (first, second) ->
            out += RelationKind(
                key = next(), category = RelationCategory.BETWEEN.open(), symmetry = RelationSymmetry.PAIR.open(),
                name = first, pairName = second, order = betweenOrders[symmetric.size + i],
                updatedAt = PROJECT_CREATED, updatedBy = DEVICE,
            )
        }
        minds.forEachIndexed { i, name ->
            out += RelationKind(
                key = next(), category = RelationCategory.MIND.open(), name = name, order = mindOrders[i],
                updatedAt = PROJECT_CREATED, updatedBy = DEVICE,
            )
        }
        return out
    }
}
