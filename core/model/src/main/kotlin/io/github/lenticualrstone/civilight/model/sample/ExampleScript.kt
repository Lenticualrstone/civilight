package io.github.lenticualrstone.civilight.model.sample

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.DEVICE
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.VOICE_EXPORT
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.at
import io.github.lenticualrstone.civilight.model.sample.ExampleText.link
import io.github.lenticualrstone.civilight.model.sample.ExampleText.paren
import io.github.lenticualrstone.civilight.model.sample.ExampleText.t
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.ChoiceOption
import io.github.lenticualrstone.civilight.model.script.CueKind
import io.github.lenticualrstone.civilight.model.script.DirectionCue
import io.github.lenticualrstone.civilight.model.script.DiscardReason
import io.github.lenticualrstone.civilight.model.script.DiscardedVoiceId
import io.github.lenticualrstone.civilight.model.script.TrashPlacement
import io.github.lenticualrstone.civilight.model.script.VoiceExport
import io.github.lenticualrstone.civilight.model.script.VoiceMode
import io.github.lenticualrstone.civilight.model.script.VoiceState
import io.github.lenticualrstone.civilight.model.file.EpisodeFile
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.plainText
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/**
 * The manuscript: blocks of every episode file. Numbers run per episode in manuscript order; gaps
 * are numbers that were issued and later purged (1-2 #007, 1-7 #049 … #057, 1-8 #002 … #060).
 */
internal object ExampleScript {
    /** Voice codes (`HRT`); the speaker part of voice IDs. */
    val voiceCodes: Map<Key, String> = mapOf(
        K.MARTHA to "MRT", K.SERA to "SRA", K.OWEN to "OWN", K.HART to "HRT", K.KAIL to "KYL", K.IRENA to "IRN",
        K.RIEN to "RIE", K.NOAH to "NOA", K.DOR to "DOR", K.TEO to "TEO", K.ELIO to "ELI", K.GORDON to "GDN",
    )

    /** The default pattern `{code}_{chapter2}{episode2}_{number3}`. */
    fun voiceId(speaker: Key, c: Int, e: Int, number: Int): String =
        "${voiceCodes.getValue(speaker)}_${two(c)}${two(e)}_${number.toString().padStart(3, '0')}"

    private fun two(n: Int) = n.toString().padStart(2, '0')

    /** Episodes 1-1 … 1-3 were exported to CSV on [VOICE_EXPORT]: their dialogue voice IDs are fixed. */
    private val exportedEpisodes = setOf(1 to 1, 1 to 2, 1 to 3)

    val BLOCKS_1_5_DELETED_AT: Long = at("2026-11-15T03:00:00Z")

    // --- Line DSL ---------------------------------------------------------------------------------

    private sealed interface Line

    private data class Say(val speaker: Key?, val runs: List<TextRun>, val voice: VoiceState? = null) : Line
    private data class Narrate(val runs: List<TextRun>) : Line
    private data class Direct(val cues: List<DirectionCue>) : Line
    private data class Choose(val options: List<ChoiceOption>) : Line
    private data class System(val runs: List<TextRun>) : Line
    private data class Memo(val runs: List<TextRun>) : Line

    /** Lines deleted together into the trash (one block group). */
    private data class Trashed(val line: Line) : Line

    /** A number that was issued and purged. */
    private data object Skip : Line

    private fun say(speaker: Key?, text: String) = Say(speaker, listOf(t(text)))
    private fun say(speaker: Key?, vararg runs: TextRun) = Say(speaker, runs.toList())
    private fun n(text: String) = Narrate(listOf(t(text)))
    private fun n(vararg runs: TextRun) = Narrate(runs.toList())

    private fun cue(n: Int, kind: CueKind, asset: Key? = null, effect: String? = null) =
        DirectionCue(key = K.directionCue(n), kind = kind.open(), assetKey = asset, effect = effect)

    private class SceneLines(val s: Int, val lines: List<Line>)

    private fun scene(s: Int, vararg lines: Line) = SceneLines(s, lines.toList())

    // --- The manuscript ---------------------------------------------------------------------------

    private val episodes: Map<Pair<Int, Int>, List<SceneLines>> = linkedMapOf(
        (1 to 1) to listOf(
            scene(
                1,
                n(
                    link(K.link(4), LinkTargetKind.TERM, K.TERM_LANTERN_SOCIETY, "등불 결사"),
                    t("의 등불지기는 해가 뜨기 전에 거리를 돈다. 꺼진 등을 세고, 남은 등을 지킨다."),
                ),
                say(K.SERA, "오늘은 네가 앞에 서. 꺼진 등이 보이면 숫자만 세고 손대지 마."),
                say(K.TEO, "숫자만요? 고칠 수 있는 것도 있잖아요."),
                say(K.SERA, "고치는 건 결사의 문 안에서 해. 거리에서는 기록만."),
            ),
            scene(
                2,
                n("해가 지면 잿더미 시장은 다시 깨어난다. 천막 아래로 녹슨 등이 줄지어 걸린다."),
                say(K.GORDON, "오웬, 오늘 밤엔 경비대가 내려온다는 소문이야."),
                say(K.OWEN, "소문은 늘 내려오지. 사람은 안 내려오고."),
            ),
            scene(
                3,
                n("결사의 문은 밤새 닫혀 있다. 문 위의 등 하나만 꺼지지 않는다."),
                say(K.MARTHA, "테오, 오늘 센 숫자를 말해 봐."),
                say(K.TEO, "꺼진 등 열한 개요. 어제보다 둘 많아요."),
                say(K.MARTHA, paren("잠시 말이 없다가"), t("기록해 둬. 세라에게도 알리고.")),
            ),
        ),
        (1 to 2) to listOf(
            scene(
                1,
                n("새벽 순찰에는 꼬마 하나가 따라붙었다. 이름은 리엔이라고 했다."),
                say(K.SERA, "따라오지 말랬지. 여긴 아이가 돌 거리가 아니야."),
                // b12: all plain, a cut-off candidate of 오리지늄 등불.
                say(K.RIEN, "등불 속 그 돌, 오리지… 뭐라고 했죠?"),
                say(K.SERA, "몰라도 돼. 불빛만 세."),
            ),
            scene(
                2,
                n("고아원 지붕 위에서 보면 하층 거리의 등이 한눈에 들어온다."),
                say(K.RIEN, "하나, 둘… 오늘은 열한 개가 꺼졌어."),
                Skip, // #007: 리엔's line, exported (RIE_0102_007), deleted and purged on 2026-11-14.
                n("리엔은 꺼진 등의 자리를 손가락으로 하나씩 짚었다."),
            ),
            scene(
                3,
                say(K.GORDON, "꼬마, 그 손에 쥔 건 뭐냐."),
                say(K.RIEN, "주웠어요. 아직 따뜻해요."),
                say(K.OWEN, paren("웃으며"), t("주운 건 주인이 없는 거지. 얼마에 팔래?")),
            ),
        ),
        (1 to 3) to listOf(
            scene(
                1,
                say(K.TEO, "등불 결사에 들어오고 싶다고? 나이부터 말해."),
                say(K.RIEN, "열일곱이요. 숫자는 누구보다 잘 세요."),
                n("테오는 웃지 않았다. 대신 결사의 문으로 가는 길을 알려 주었다."),
            ),
            scene(
                2,
                n("결사의 문이 아침에 열리는 일은 드물었다."),
                say(K.MARTHA, "세라가 데려온 아이라면 받아 주지. 대신 첫 해는 견습이다."),
                say(K.RIEN, "견습이면 등을 만져도 돼요?"),
                // b32: alias candidate 빛돌.
                say(K.SERA, "빛돌 하나면 한 달은 버텨."),
                say(K.MARTHA, "등은 기록부터 배운 다음이야."),
            ),
            scene(
                3,
                n("결사의 안쪽 방에는 꺼진 등들이 줄지어 놓여 있었다."),
                say(K.MARTHA, "여기 있는 등은 모두 누군가의 거리였다. 하나씩 이름을 붙여 둬."),
                say(K.RIEN, "이름을요? 등에도요?"),
            ),
        ),
        (1 to 4) to listOf(
            scene(
                1,
                Direct(listOf(cue(1, CueKind.BACKGROUND, K.ASSET_BG_SOCIETY_GATE_MORNING))),
                say(K.RIEN, "세라 씨, 이 기록은 왜 한 해가 통째로 비어 있어요?"),
                say(K.SERA, "그해엔 적을 게 없었어. 불이 다 꺼졌으니까."),
            ),
            scene(
                2,
                // b41: `◇ 첫 공개` of 대정전.
                n(
                    t("1195년, "),
                    link(K.link(5), LinkTargetKind.TERM, K.TERM_BLACKOUT, "대정전"),
                    t("의 밤. 하층 거리의 등이 한꺼번에 꺼졌다."),
                ),
                // b42: link l42 before the reveal on #044 (`▲ 공개 전 사용`).
                say(
                    K.SERA,
                    link(K.LINK_L42, LinkTargetKind.TERM, K.TERM_ORIGINIUM_LANTERN, "오리지늄"),
                    t("은 타지 않아. 빛만 남지."),
                ),
                n("어린 세라의 손에는 아직 따뜻한 돌 하나가 쥐여 있었다."),
            ),
            scene(
                3,
                n("기록실에는 불이 꺼지기 전의 지도가 남아 있었다."),
                say(K.SERA, "이 지도에서 빛이 있던 곳을 전부 표시해. 지금 거리와 맞춰 볼 거야."),
                say(K.RIEN, "그럼 꺼진 등도 다시 셀 수 있겠네요."),
            ),
        ),
        (1 to 5) to listOf(
            scene(
                1,
                n("안개가 낮게 깔린 새벽, 순찰대는 하층 구역의 경계를 따라 걸었다."),
                say(K.TEO, "세라 씨, 저쪽 골목은 등이 하나도 안 남았어요."),
                say(K.SERA, "그럼 지도에 검게 칠해. 오늘은 거기까지만 가."),
            ),
            scene(
                2,
                n("거리 끝에서 길이 두 갈래로 나뉘었다."),
                say(K.RIEN, "왼쪽은 시장, 오른쪽은 결사예요. 어느 쪽으로 가요?"),
                Choose(
                    listOf(
                        ChoiceOption(K.choiceOption(1), OrderKeys.evenlySpaced(2)[0], listOf(t("시장 쪽으로 간다")), speakerKey = K.RIEN),
                        ChoiceOption(K.choiceOption(2), OrderKeys.evenlySpaced(2)[1], listOf(t("결사로 돌아간다"))),
                    ),
                ),
                say(K.SERA, "어느 쪽이든 해가 뜨기 전에 돌아와."),
                Trashed(n("리엔은 오래 망설였다.")),
                Trashed(n("결국 리엔은 시장 쪽을 골랐다.")),
            ),
            scene(
                3,
                n("도르의 작업대 위에는 뚜껑이 열린 등이 셋 있었다."),
                say(K.DOR, "심지는 멀쩡해. 문제는 안에 든 돌이야. 빛이 바랬어."),
                say(K.RIEN, "돌도 늙어요?"),
                say(K.DOR, "늙지. 사람보다 천천히."),
            ),
            scene(
                4,
                System(listOf(t("기록부에 새 쪽이 추가되었다."), TextRun.Break, t("꺼진 등 · 열일곱"))),
                n("저녁이 되자 세라는 기록부의 마지막 장을 덮었다."),
                say(K.SERA, "리엔, 내일은 시장에 가 봐. 오웬이라는 남자가 꺼진 돌을 사들인대."),
                say(K.RIEN, "사들여요? 꺼진 걸요?"),
            ),
        ),
        (1 to 6) to listOf(
            scene(
                1,
                n("낮의 잿더미 시장은 밤보다 조용했다. 대신 눈이 많았다."),
                say(K.RIEN, "꺼진 돌을 산다는 사람이 당신이에요?"),
                // b61: alias candidate 빛돌.
                say(K.OWEN, "하층에서 빛돌을 판다고?"),
                say(K.OWEN, paren("목소리를 낮추며"), t("여기선 그런 말 크게 하지 마.")),
            ),
            scene(
                2,
                Direct(listOf(cue(2, CueKind.BACKGROUND, K.ASSET_BG_ASH_MARKET_NIGHT))),
                n(
                    link(K.link(6), LinkTargetKind.CHARACTER, K.NOAH, "노아"),
                    t("가 장부를 품에 안고 천막 사이를 뛰어왔다."),
                ),
                Say(
                    K.NOAH, listOf(t("오웬 아저씨! 상층 사람들이 또 돌 값을 물어봤어요.")),
                    voice = fixedVoice(K.NOAH, 1, 6, 7, "오웬 아저씨! 상층 사람들이 또 돌 값을 물어봤어요.", at("2026-11-12T03:00:00Z")),
                ),
                say(K.OWEN, "뭐라고 했어?"),
                say(K.NOAH, "모른다고 했죠. 진짜 모르니까."),
                say(K.NOAH, "근데 그 사람들, 등 하나에 은화 세 닢을 준대요."),
                say(K.OWEN, "세 닢이면 거짓말이야. 다섯 닢은 받아야지."),
                say(K.NOAH, "다섯 닢이요? 그럼 나도 하나 팔래요."),
                say(K.NOAH, "아, 농담이에요. 아저씨 눈 무서워요."),
                say(K.NOAH, "장부는 여기 두고 갈게요. 고든 아저씨가 찾던데요."),
            ),
            scene(
                3,
                say(K.GORDON, "도르, 그 등 고쳐지면 나한테 먼저 보여 줘."),
                say(K.DOR, "고쳐지면 결사로 간다. 시장엔 안 와."),
                n("고든은 대답 대신 천막 끈을 세게 당겼다."),
            ),
            scene(
                4,
                n("오웬의 장부에는 이름 대신 숫자만 적혀 있었다."),
                say(K.NOAH, "아저씨, 이 숫자는 누구예요?"),
                say(K.OWEN, "몰라도 되는 사람."),
                say(K.NOAH, "그럼 이건요? 이것도 몰라도 돼요?"),
                say(K.OWEN, "그건 너다."),
                say(K.NOAH, "나요? 나도 숫자예요?"),
                say(K.NOAH, "몇 번인데요?"),
                say(K.OWEN, paren("장부를 덮으며"), t("백 번째. 그러니까 오래 살아라.")),
                say(K.NOAH, "백 번째면 좋은 거예요?"),
                say(K.NOAH, "알았어요. 오래 살게요."),
                say(K.NOAH, "대신 내일 사탕 사 줘요."),
                say(K.NOAH, "약속이에요!"),
            ),
            // In the trash (scene deleted 2026-11-04); its blocks are not live by containment.
            scene(
                5,
                say(K.NOAH, "오늘은 등이 하나도 안 꺼졌어요."),
                n("노아가 웃었다."),
                say(K.OWEN, "그럼 오늘은 장사 접자."),
            ),
        ),
        (1 to 7) to listOf(
            scene(
                1,
                Direct(listOf(cue(3, CueKind.SCREEN_EFFECT, effect = "fade"), cue(4, CueKind.SFX, K.ASSET_SFX_WIND))),
                n("새벽 네 시, 하층 거리는 아직 잿빛이었다."),
                n("리엔은 견습 표식을 목에 걸고 혼자 순찰을 나섰다."),
                say(K.RIEN, paren("작게"), t("하나, 둘, 셋… 오늘도 꺼진 등이 늘었어.")),
                n("골목 끝, 오래전에 꺼진 등 하나가 바람에 흔들렸다."),
                say(K.RIEN, "저 등은 세라 씨 기록에도 없던 거야."),
                n("리엔이 다가가자 유리 안쪽에서 무언가가 아주 잠깐 빛났다."),
                Direct(listOf(cue(5, CueKind.CG, K.ASSET_CG_FLICKER))),
                say(K.RIEN, "…방금 켜졌어?"),
                n("다시 보았을 때 등은 어두웠다. 하지만 유리는 따뜻했다."),
                say(K.RIEN, "착각이 아니야. 손이 아직 따뜻해."),
                n("멀리서 첫 종이 울렸다. 상층 관문이 열리는 시각이었다."),
                say(K.RIEN, "기록해야 해. 시각, 자리, 그리고 빛."),
                n("리엔은 기록부를 펼쳐 날짜 옆에 작은 점을 찍었다."),
                say(K.RIEN, "깜박임 하나. 하층 거리, 동쪽 끝."),
                say(K.RIEN, "마르타 씨는 믿어 줄까."),
                n("그 뒤로 등은 다시 깜박이지 않았다."),
            ),
            scene(
                2,
                n("저녁의 상층 관문은 붉은 등으로 줄지어 밝혀져 있었다."),
                n("관문 앞에 외투 깃을 세운 남자가 서 있었다."),
                say(K.KAIL, "통행증."),
                say(K.HART, "없어."),
                say(K.KAIL, "그럼 돌아가. 오늘 밤엔 아무도 안 내려간다."),
                say(K.HART, "내려가려는 게 아니야. 올라온 거지."),
                say(K.IRENA, "대장님, 이 사람 이름이 명부에 없습니다."),
                say(K.KAIL, "이름이 뭐지?"),
                say(K.HART, paren("잠시 등불을 올려다보며"), t("이름은 아래에 두고 왔어.")),
                // #027: no speaker (`▲ 화자 없음`).
                say(null, "대장님, 하층에서 또 등이 꺼졌다는 보고입니다."),
                say(K.KAIL, "꺼지는 건 늘 하층이지. 기록만 해 둬."),
                n("남자의 시선이 관문 너머 아래쪽 거리로 향했다."),
                say(K.HART, "꺼진 게 아니라 누가 끈 거라면?"),
                say(K.IRENA, "무슨 뜻이죠?"),
                say(K.HART, "새벽이 되면 알게 될 거야."),
                n("카일이 손짓하자 경비 둘이 남자의 양옆에 섰다."),
                say(K.KAIL, "새벽까지 여기 있어. 그때 다시 묻겠다."),
                n("하지만 새벽이 오기 전에 남자는 사라지고 없었다."),
            ),
            scene(
                3,
                say(K.SERA, "불은 꺼졌어. 기록도 거기서 끝나."),
                say(K.OWEN, "기록 따위로 배가 부르진 않지."),
                n("결사의 문 앞, 새벽 다섯 시 사십 분. 잿빛 하늘 아래 사람들이 모여 있었다."),
                n("밤새 꺼진 등은 모두 열일곱 개였다."),
                n("리엔은 기록부를 품에 안고 문 그림자 쪽으로 물러섰다."),
                Direct(listOf(cue(6, CueKind.BACKGROUND, K.ASSET_BG_LOWER_STREET_DAWN), cue(7, CueKind.BGM, K.ASSET_BGM_EMBERS))),
                say(K.RIEN, paren("작게"), t("저기… 불빛이 보여요. 누가 아직 남아 있는 걸까요?")),
                say(K.HART, "멈추지 마. 저건 등불이 아니야."),
                n(
                    t("바람이 잿더미를 쓸고 지나갔다. 꺼진 줄 알았던 "),
                    link(K.LINK_L44, LinkTargetKind.TERM, K.TERM_ORIGINIUM_LANTERN, "오리지늄 등불"),
                    t("이 한 번 깜박였다."),
                ),
                say(K.RIEN, paren("숨을 삼키며"), t("하르트 씨, 방금…")),
                Memo(listOf(t("여기서 등불이 두 번째로 깜박이게 할지"))),
                say(K.HART, "뒤돌아보지 말고 따라와."),
                say(K.RIEN, "하지만 저 불빛은 결사의 신호였어요."),
            ),
        ),
        (1 to 8) to listOf(
            // #001: created and left empty; numbers #002 … #060 were issued earlier and purged.
            scene(1, Narrate(emptyList())),
        ),
    )

    /** Episode creation times; block `createdAt` = base + its index in minutes. */
    private val episodeWrittenFrom: Map<Pair<Int, Int>, Long> = mapOf(
        (1 to 1) to at("2026-04-01T11:00:00Z"),
        (1 to 2) to at("2026-04-12T11:00:00Z"),
        (1 to 3) to at("2026-04-26T11:00:00Z"),
        (1 to 4) to at("2026-05-10T11:00:00Z"),
        (1 to 5) to at("2026-06-01T11:00:00Z"),
        (1 to 6) to at("2026-07-01T11:00:00Z"),
        (1 to 7) to at("2026-11-15T11:00:00Z"),
        (1 to 8) to at("2026-11-21T23:00:00Z"),
    )

    private fun fixedVoice(speaker: Key, c: Int, e: Int, number: Int, text: String, exportedAt: Long) = VoiceState(
        mode = VoiceMode.FIXED.open(),
        fixedId = voiceId(speaker, c, e, number),
        fixedAt = exportedAt,
        lastExport = VoiceExport(at = exportedAt, textHash = ExampleText.sha256(text), speakerKey = speaker),
    )

    /** One episode file per numbering unit that has a live scene (§4.1). */
    fun episodeFiles(): Map<Key, EpisodeFile> {
        val out = LinkedHashMap<Key, EpisodeFile>()
        for ((ce, scenes) in episodes) out[K.episode(ce.first, ce.second)] = buildEpisode(ce.first, ce.second, scenes)
        // Episodes with scenes but no blocks still get their (empty) file.
        for ((c, e) in listOf(1 to 9, 1 to 10, 2 to 3, 2 to 5)) out[K.episode(c, e)] = EpisodeFile(unitKey = K.episode(c, e))
        return out
    }

    private fun buildEpisode(c: Int, e: Int, scenes: List<SceneLines>): EpisodeFile {
        val live = mutableListOf<Block>()
        val dead = mutableListOf<Block>()
        var number = 0
        var index = 0
        val base = episodeWrittenFrom.getValue(c to e)
        for (scene in scenes) {
            val sceneKey = K.scene(c, e, scene.s)
            val sceneIsTrashed = sceneKey == K.SCENE_1_6_5
            val counted = scene.lines.filter { it != Skip }
            val orders = OrderKeys.evenlySpaced(counted.size)
            var position = 0
            var lastLiveKey: Key? = null
            for (line in scene.lines) {
                number++
                if (line == Skip) continue
                val trashed = line is Trashed
                val block = blockOf(
                    c, e, number, sceneKey, orders[position], (line as? Trashed)?.line ?: line, base + index * 60_000L,
                )
                val placed = if (trashed) {
                    block.copy(
                        deletedAt = BLOCKS_1_5_DELETED_AT, trashEntryKey = K.TRASH_BLOCKS_1_5_2,
                        trashPlacement = TrashPlacement(prevKey = lastLiveKey, nextKey = null, ordinal = position),
                        updatedAt = BLOCKS_1_5_DELETED_AT,
                    )
                } else {
                    block
                }
                if (trashed || sceneIsTrashed) dead += placed else live += placed
                if (!trashed) lastLiveKey = placed.key
                position++
                index++
            }
        }
        val discarded = if (c == 1 && e == 2) {
            listOf(
                DiscardedVoiceId(
                    id = voiceId(K.RIEN, 1, 2, 7), blockKey = K.block(1, 2, 7),
                    discardedAt = at("2026-11-14T02:00:00Z"), reason = DiscardReason.DELETED.open(),
                ),
            )
        } else {
            emptyList()
        }
        // Non-live blocks last: the order a split + reassemble round trip produces.
        return EpisodeFile(unitKey = K.episode(c, e), blocks = live + dead, discardedVoiceIds = discarded)
    }

    private fun blockOf(c: Int, e: Int, number: Int, sceneKey: Key, order: String, line: Line, createdAt: Long): Block {
        val common = Block(
            key = K.block(c, e, number), sceneKey = sceneKey, order = order, number = number,
            kind = BlockKind.NARRATION.open(), createdAt = createdAt, updatedAt = createdAt, updatedBy = DEVICE,
        )
        return when (line) {
            is Say -> common.copy(
                kind = BlockKind.DIALOGUE.open(), speakerKey = line.speaker, runs = line.runs,
                voice = line.voice ?: if (line.speaker != null && (c to e) in exportedEpisodes) {
                    fixedVoice(line.speaker, c, e, number, line.runs.plainText, VOICE_EXPORT)
                } else {
                    null
                },
            )
            is Narrate -> common.copy(runs = line.runs)
            is Direct -> common.copy(kind = BlockKind.DIRECTION.open(), cues = line.cues)
            is Choose -> common.copy(kind = BlockKind.CHOICE.open(), options = line.options)
            is System -> common.copy(kind = BlockKind.SYSTEM.open(), runs = line.runs)
            is Memo -> common.copy(kind = BlockKind.MEMO.open(), runs = line.runs)
            is Trashed, Skip -> error("not a block line: $line")
        }
    }
}
