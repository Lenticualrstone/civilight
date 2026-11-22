package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind

/** The block-add sheet (SCR-12) and script commands in the command field (SCR-68). */
object ScriptCommands {
    /** One row of `블록 추가`: name, description, shortcut; [kind] null = `새 씬` (SCR-49 routing). */
    data class AddRow(val name: String, val description: String, val shortcut: String, val kind: BlockKind?)

    val ADD_ROWS: List<AddRow> = listOf(
        AddRow("대사", "화자 · 대사 · (괄호 지문)", "/대", BlockKind.DIALOGUE),
        AddRow("나레이션", "화자 없는 서술", "/나", BlockKind.NARRATION),
        AddRow("연출 지시", "배경 · BGM · 효과음 · CG · 화면 효과", "/연", BlockKind.DIRECTION),
        AddRow("선택지", "대사만 달라지고 다시 합류", "/선", BlockKind.CHOICE),
        AddRow("새 씬", "다음 씬 만들기 · 여기서 나누기", "/씬", null),
        AddRow("시스템 텍스트", "획득 · 안내 같은 화면 문구", "/시", BlockKind.SYSTEM),
        AddRow("작가 메모", "게임 출력 제외", "/메", BlockKind.MEMO),
    )

    const val SHEET_TITLE = "블록 추가"
    const val KIND_CHANGE_TITLE = "종류 바꾸기"
    const val READ_ONLY = "읽기 전용"
    const val HINT = "명령 또는 검색 · 인물 · 블록 · 씬 이동"

    /** Sheet subline `지금 블록 아래 · 새 대사 화자: 리엔(직전)` (no speaker: the part is left out). */
    fun addSheetSubline(doc: ProjectDoc, position: BlockPosition): String {
        val inherited = BlockOps.inheritedSpeaker(doc, position)?.let { Labels.speakerName(doc, it) }
        return listOfNotNull("지금 블록 아래", inherited?.let { "새 대사 화자: $it(직전)" }).joinToString(" · ")
    }

    /** Rows of `종류 바꾸기` (SCR-13 `[PROPOSED]`): the add list without `새 씬`. */
    val KIND_ROWS: List<AddRow> get() = ADD_ROWS.filter { it.kind != null }

    /** What a typed slash command means. */
    sealed interface Slash {
        data class AddBlock(val kind: BlockKind) : Slash
        data object NewScene : Slash
        data object OpenSheet : Slash
    }

    /** `/` alone opens the sheet; `/대 … /메` add a block; `/씬` routes (SCR-49). Null for other text. */
    fun parseSlash(text: String): Slash? {
        val t = text.trim()
        if (t == "/") return Slash.OpenSheet
        val row = ADD_ROWS.firstOrNull { it.shortcut == t } ?: return null
        return row.kind?.let { Slash.AddBlock(it) } ?: Slash.NewScene
    }

    /** Script commands offered in the command field (SCR-68). */
    fun commands(doc: ProjectDoc, numberingUnitKey: Key): List<String> {
        val code = if (numberingUnitKey == doc.project.key) "" else Labels.unitCode(doc, numberingUnitKey)
        return ADD_ROWS.map { it.shortcut } + listOf(
            "렌즈를 이 씬으로 맞추기",
            "점검 센터 열기",
            "번호 다시 매기기 · $code".trimEnd(' ', '·'),
            "블록 고르기",
            "에셋",
        )
    }

    /** Navigation target of typed text (`[PROPOSED]`). */
    sealed interface Jump {
        data class ToBlock(val blockKey: Key) : Jump
        data class ToScene(val sceneKey: Key) : Jump
    }

    private val BLOCK_NUMBER = Regex("""^#(\d+)$""")
    private val SCENE_ORDINAL = Regex("""^씬\s*(\d+)$""")

    /**
     * `#043` jumps to that live block of the current numbering unit; `씬 3` or a scene title jumps to
     * that scene head. Null when nothing matches (other results come from the search area).
     */
    fun jump(doc: ProjectDoc, numberingUnitKey: Key, text: String): Jump? {
        val t = text.trim()
        BLOCK_NUMBER.matchEntire(t)?.let { m ->
            val n = m.groupValues[1].toInt()
            return Numbering.liveBlocksOfUnit(doc, numberingUnitKey).firstOrNull { it.number == n }?.let { Jump.ToBlock(it.key) }
        }
        val scenes = doc.manuscript.liveScenes.filter { doc.numberingUnitOfScene(it.key) == numberingUnitKey }
        SCENE_ORDINAL.matchEntire(t)?.let { m ->
            return scenes.getOrNull(m.groupValues[1].toInt() - 1)?.let { Jump.ToScene(it.key) }
        }
        return scenes.firstOrNull { it.title == t }?.let { Jump.ToScene(it.key) }
    }
}
