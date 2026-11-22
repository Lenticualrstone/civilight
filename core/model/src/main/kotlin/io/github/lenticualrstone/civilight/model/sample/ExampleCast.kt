package io.github.lenticualrstone.civilight.model.sample

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.cast.ArchiveDoc
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.EntityRef
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.cast.PairHolder
import io.github.lenticualrstone.civilight.model.cast.Relation
import io.github.lenticualrstone.civilight.model.cast.RelationEndKind
import io.github.lenticualrstone.civilight.model.cast.SlotValue
import io.github.lenticualrstone.civilight.model.cast.VoiceLine
import io.github.lenticualrstone.civilight.model.cast.VoiceLineStatus
import io.github.lenticualrstone.civilight.model.file.CastFile
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.PointRef
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.DEVICE
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.LAST_EDIT
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.at
import io.github.lenticualrstone.civilight.model.sample.ExampleText.link
import io.github.lenticualrstone.civilight.model.sample.ExampleText.t
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** `cast.json` of the example: 12 characters (노아 in the trash), 5 relations, voice lines, archive documents. */
internal object ExampleCast {
    /** 노아 went to the trash on 2026-10-26 (`3일 남음` on 2026-11-22). */
    val NOAH_DELETED_AT: Long = at("2026-10-26T01:00:00Z")

    // --- Field value helpers ----------------------------------------------------------------------

    private fun text(value: String) = FieldValue.of(FieldPayload.Texts(listOf(value)))
    private fun number(value: String) = FieldValue.of(FieldPayload.Number(value))
    private fun choice(vararg options: Key) = FieldValue.of(FieldPayload.Choice(options.toList()))
    private fun year(value: Int) = FieldValue.of(FieldPayload.Date(value))
    private fun anniversary(month: Int, day: Int) = FieldValue.of(FieldPayload.Anniversary(month, day))
    private fun linkTo(vararg targets: Key) = FieldValue.of(FieldPayload.Link(targets.toList()))
    private fun rich(doc: RichDoc) = FieldValue.of(FieldPayload.Rich(doc))

    private val FEMALE = choice(K.OPTION_GENDER_FEMALE)
    private val MALE = choice(K.OPTION_GENDER_MALE)
    private val HUMAN = choice(K.OPTION_SPECIES_HUMAN)
    private val BEASTFOLK = choice(K.OPTION_SPECIES_BEASTFOLK)

    // --- Characters -------------------------------------------------------------------------------

    /** Manual order of the cast list (노아 among them, though trashed). */
    private val castOrder = listOf(
        K.RIEN, K.HART, K.SERA, K.MARTHA, K.OWEN, K.NOAH, K.KAIL, K.IRENA, K.DOR, K.TEO, K.ELIO, K.GORDON,
    )

    private fun character(
        key: Key,
        recordNo: Int,
        name: String,
        oneLiner: String,
        tags: List<Key>,
        fields: Map<Key, FieldValue>,
        displayNameBase: String? = null,
        speechBody: RichDoc = emptyList(),
        secretBody: RichDoc = emptyList(),
    ): Character {
        val orders = OrderKeys.evenlySpaced(castOrder.size)
        return Character(
            key = key, recordNo = recordNo, name = name, displayNameBase = displayNameBase, oneLiner = oneLiner,
            tagKeys = tags, speechBody = speechBody, secretBody = secretBody,
            voiceCode = ExampleScript.voiceCodes.getValue(key), order = orders[castOrder.indexOf(key)], fields = fields,
            updatedAt = LAST_EDIT, updatedBy = DEVICE,
        )
    }

    fun characters(): List<Character> {
        val rien = character(
            K.RIEN, 7, "리엔", "막내 등불지기", listOf(K.TAG_LEAD, K.TAG_LANTERN_SOCIETY),
            mapOf(
                K.FIELD_BIRTH to year(1183),
                K.FIELD_BIRTHDAY to anniversary(3, 14),
                K.FIELD_HEIGHT to number("154"),
                K.FIELD_GENDER to FEMALE,
                K.FIELD_SPECIES to HUMAN,
                K.FIELD_PERSONALITY to rich(doc(120) { paragraph("겁이 많지만 한 번 정한 일에서는 물러서지 않는다.") }),
                K.FIELD_CODENAME to text("작은 등"),
                K.FIELD_ORIGIN to linkTo(K.PLACE_LOWER_STREET),
                K.FIELD_SPECIALTY to choice(K.OPTION_SPECIALTY_LAMP_REPAIR, K.OPTION_SPECIALTY_PATHFINDING),
            ),
            speechBody = doc(100) {
                heading("말투")
                paragraph("존댓말. 낯선 사람에게도 ‘씨’를 붙인다.")
                paragraph("숫자를 셀 때만 혼잣말이 빨라진다.")
            },
            secretBody = doc(110) {
                paragraph("대정전 날 부모를 잃었다. 그날 주운 돌을 아직 지니고 있다.")
            },
        )
        val hart = character(
            K.HART, 4, "하르트", "등불을 싫어하는 남자", listOf(K.TAG_LEAD),
            mapOf(
                K.FIELD_BIRTH to FieldValue.UNKNOWN,
                K.FIELD_AGE to FieldValue.UNKNOWN,
                K.FIELD_HEIGHT to number("183"),
                K.FIELD_GENDER to MALE,
                K.FIELD_SPECIES to HUMAN,
                K.FIELD_PERSONALITY to rich(doc(150) { paragraph("말이 짧다. 등불 앞에서는 늘 등을 돌린다.") }),
                K.FIELD_SPECIALTY to choice(K.OPTION_SPECIALTY_SWORD),
            ),
            displayNameBase = "???",
            speechBody = doc(130) { paragraph("반말. 문장을 끝까지 말하지 않는다.") },
            secretBody = doc(140) {
                paragraph(
                    t("대정전 때 누군가를 잃었다. "),
                    link(K.link(10), LinkTargetKind.TERM, K.TERM_UPPER_GRID, "상층 송전망"),
                    t("과 관련이 있다."),
                )
            },
        )
        val sera = character(
            K.SERA, 2, "세라", "결사의 기록관", listOf(K.TAG_LEAD, K.TAG_LANTERN_SOCIETY),
            mapOf(
                K.FIELD_BIRTH to year(1179),
                K.FIELD_AFFILIATION to text("등불 결사"),
                K.FIELD_HEIGHT to number("168"),
                K.FIELD_GENDER to FEMALE,
                K.FIELD_SPECIES to HUMAN,
                K.FIELD_CODENAME to text("잉크"),
                K.FIELD_ORIGIN to linkTo(K.PLACE_LOWER_STREET),
                K.FIELD_SPECIALTY to choice(K.OPTION_SPECIALTY_RECORDS, K.OPTION_SPECIALTY_PATHFINDING),
            ),
            speechBody = doc(160) { paragraph("짧은 명령형. 아이에게는 조금 부드럽다.") },
        )
        val martha = character(
            K.MARTHA, 1, "마르타", "등불 결사의 결사장", listOf(K.TAG_LANTERN_SOCIETY),
            mapOf(
                K.FIELD_BIRTH to year(1158),
                K.FIELD_AFFILIATION to text("상층 경비대"),
                K.FIELD_HEIGHT to number("171"),
                K.FIELD_GENDER to FEMALE,
                K.FIELD_SPECIES to HUMAN,
                K.FIELD_PERSONALITY to rich(doc(200) { paragraph("말이 적고 기록을 믿는다.") }),
                K.FIELD_ORIGIN to linkTo(K.PLACE_UPPER_CITY),
                K.FIELD_SPECIALTY to choice(K.OPTION_SPECIALTY_CLERIC, K.OPTION_SPECIALTY_RECORDS),
            ),
        )
        val owen = character(
            K.OWEN, 3, "오웬", "잿더미 시장의 밀수상", listOf(K.TAG_LOWER_DISTRICT),
            mapOf(
                K.FIELD_BIRTH to year(1171),
                K.FIELD_AFFILIATION to text("없음"),
                K.FIELD_HEIGHT to number("176"),
                K.FIELD_GENDER to MALE,
                K.FIELD_SPECIES to BEASTFOLK,
                K.FIELD_SPECIALTY to choice(K.OPTION_SPECIALTY_BARTER),
            ),
            secretBody = doc(180) {
                paragraph(
                    link(K.link(8), LinkTargetKind.CHARACTER, K.NOAH, "노아"),
                    t("를 몰래 돌보고 있다. 장부의 백 번째 줄이 노아다."),
                )
            },
        )
        val kail = character(
            K.KAIL, 5, "카일", "상층 경비대 대장", listOf(K.TAG_UPPER),
            mapOf(
                K.FIELD_BIRTH to year(1170),
                K.FIELD_AFFILIATION to text("상층 경비대"),
                K.FIELD_HEIGHT to number("185"),
                K.FIELD_GENDER to MALE,
                K.FIELD_SPECIES to HUMAN,
                K.FIELD_SPECIALTY to choice(K.OPTION_SPECIALTY_SWORD),
            ),
        )
        val irena = character(
            K.IRENA, 6, "이레나", "상층 관문 명부 담당", listOf(K.TAG_UPPER),
            mapOf(
                K.FIELD_BIRTH to year(1185),
                K.FIELD_AFFILIATION to text("상층 경비대"),
                K.FIELD_GENDER to FEMALE,
                K.FIELD_SPECIES to HUMAN,
            ),
        )
        val dor = character(
            K.DOR, 9, "도르", "하층 거리의 등 수리공", listOf(K.TAG_LOWER_DISTRICT),
            mapOf(
                K.FIELD_BIRTH to year(1165),
                K.FIELD_GENDER to MALE,
                K.FIELD_SPECIES to BEASTFOLK,
                K.FIELD_SPECIALTY to choice(K.OPTION_SPECIALTY_LAMP_REPAIR),
            ),
        )
        val teo = character(
            K.TEO, 10, "테오", "등불 결사 순찰대원", listOf(K.TAG_LANTERN_SOCIETY),
            mapOf(
                K.FIELD_BIRTH to year(1186),
                K.FIELD_AFFILIATION to text("등불 결사"),
                K.FIELD_GENDER to MALE,
                K.FIELD_SPECIES to HUMAN,
                K.FIELD_SPECIALTY to choice(K.OPTION_SPECIALTY_PATHFINDING),
            ),
        )
        val elio = character(
            K.ELIO, 11, "엘리오", "상층 귀족가의 막내", listOf(K.TAG_UPPER),
            mapOf(
                K.FIELD_BIRTH to year(1182),
                K.FIELD_AFFILIATION to text("상층"),
                K.FIELD_GENDER to choice(K.OPTION_GENDER_UNSAID),
                K.FIELD_SPECIES to choice(K.OPTION_SPECIES_FAE),
            ),
        )
        val gordon = character(
            K.GORDON, 12, "고든", "잿더미 시장의 천막 주인", listOf(K.TAG_LOWER_DISTRICT),
            mapOf(
                K.FIELD_BIRTH to year(1160),
                K.FIELD_AFFILIATION to text("잿더미 시장"),
                K.FIELD_GENDER to MALE,
                K.FIELD_SPECIES to BEASTFOLK,
                K.FIELD_SPECIALTY to choice(K.OPTION_SPECIALTY_BARTER),
            ),
        )
        val noah = character(
            K.NOAH, 8, "노아", "잿더미 시장의 심부름꾼", listOf(K.TAG_LOWER_DISTRICT),
            mapOf(
                K.FIELD_AGE to number("11"),
                K.FIELD_GENDER to MALE,
                K.FIELD_SPECIES to HUMAN,
            ),
        ).copy(deletedAt = NOAH_DELETED_AT, trashEntryKey = K.TRASH_NOAH, updatedAt = NOAH_DELETED_AT)
        // Non-live rows last (round-trip order).
        return listOf(rien, hart, sera, martha, owen, kail, irena, dor, teo, elio, gordon, noah)
    }

    // --- Relations --------------------------------------------------------------------------------

    private fun ref(key: Key) = EntityRef(RelationEndKind.CHARACTER, key)

    private fun relation(key: Key, a: Key, b: Key) =
        Relation(key = key, endA = ref(a), endB = ref(b), updatedAt = LAST_EDIT, updatedBy = DEVICE)

    fun relations(): List<Relation> = listOf(
        relation(K.REL_RIEN_HART, K.RIEN, K.HART),
        relation(K.REL_RIEN_SERA, K.RIEN, K.SERA).copy(
            between = SlotValue(kindKey = K.KIND_SENIOR_JUNIOR, pairFirstHolder = PairHolder.B.open()),
            mindA = SlotValue(kindKey = K.KIND_RELIANCE, description = "의지하는 선배"),
            addressA = "세라 씨",
        ),
        relation(K.REL_RIEN_OWEN, K.RIEN, K.OWEN).copy(between = SlotValue(kindKey = K.KIND_TRADE_PARTNER)),
        relation(K.REL_SERA_HART, K.SERA, K.HART).copy(between = SlotValue(kindKey = K.KIND_COLLEAGUE)),
        // Not live while 노아 is in the trash (last, round-trip order).
        relation(K.REL_OWEN_NOAH, K.OWEN, K.NOAH).copy(addressB = "아저씨"),
    )

    // --- Voice lines and archive documents ----------------------------------------------------------

    private fun voiceLine(n: Int, character: Key, order: String, title: String, text: String?, done: Boolean) = VoiceLine(
        key = K.voiceLine(n), characterKey = character, order = order, title = title,
        text = text?.let { doc(210 + n) { paragraph(it) } }.orEmpty(),
        status = (if (done) VoiceLineStatus.DONE else VoiceLineStatus.WRITING).open(),
        updatedAt = LAST_EDIT, updatedBy = DEVICE,
    )

    fun voiceLines(): List<VoiceLine> {
        val rien = OrderKeys.evenlySpaced(5)
        return listOf(
            voiceLine(1, K.RIEN, rien[0], "임명", "견습 등불지기 리엔, 오늘부터 거리를 셉니다.", done = true),
            voiceLine(2, K.RIEN, rien[1], "아침 인사", "좋은 아침이에요. 꺼진 등은 몇 개였어요?", done = true),
            voiceLine(3, K.RIEN, rien[2], "등불을 켤 때", "하나 켤게요. 다시는 안 꺼지게.", done = false),
            voiceLine(4, K.RIEN, rien[3], "신뢰 상승 후", null, done = false),
            voiceLine(5, K.RIEN, rien[4], "전투 시작", null, done = false),
            voiceLine(6, K.HART, OrderKeys.FIRST, "첫 만남", "멈추지 마.", done = true),
        )
    }

    fun archiveDocs(): List<ArchiveDoc> {
        val rien = OrderKeys.evenlySpaced(2)
        return listOf(
            ArchiveDoc(
                key = K.ARCHIVE_RIEN_JOURNAL, characterKey = K.RIEN, order = rien[0], title = "등불지기의 일지",
                body = doc(240) {
                    paragraph("꺼진 등 열일곱. 그중 하나가 깜박였다. 아무도 믿지 않겠지만 적어 둔다.")
                    paragraph("하르트라는 사람은 불빛을 보고도 멈추지 않았다.")
                },
                unlockPoint = PointRef.unitEnd(K.EPISODE_1_7), updatedAt = LAST_EDIT, updatedBy = DEVICE,
            ),
            ArchiveDoc(
                key = K.ARCHIVE_RIEN_BLACKOUT, characterKey = K.RIEN, order = rien[1], title = "대정전 그날",
                unlockPoint = PointRef.unitEnd(K.CHAPTER_2), updatedAt = LAST_EDIT, updatedBy = DEVICE,
            ),
            ArchiveDoc(
                key = K.ARCHIVE_HART_NOTEBOOK, characterKey = K.HART, order = OrderKeys.FIRST, title = "하르트의 수첩",
                body = doc(250) { paragraph("아래로 내려간다. 이름은 두고 간다.") },
                unlockPoint = PointRef.unitEnd(K.EPISODE_1_9), updatedAt = LAST_EDIT, updatedBy = DEVICE,
            ),
        )
    }

    fun castFile(): CastFile = CastFile(
        characters = characters(),
        relations = relations(),
        voiceLines = voiceLines(),
        archiveDocs = archiveDocs(),
    )
}
