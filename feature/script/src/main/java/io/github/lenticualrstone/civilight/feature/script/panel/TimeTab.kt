package io.github.lenticualrstone.civilight.feature.script.panel

import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.domain.script.Panels
import io.github.lenticualrstone.civilight.domain.script.Records
import io.github.lenticualrstone.civilight.domain.script.SceneText
import io.github.lenticualrstone.civilight.domain.script.SceneTimeSource
import io.github.lenticualrstone.civilight.domain.story.scene.DayChip
import io.github.lenticualrstone.civilight.domain.story.scene.SceneWhere
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.TimeLabels
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.CueKind
import io.github.lenticualrstone.civilight.model.script.ScriptLayers
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.TimePrecision

/**
 * `이 씬 · 장소와 때` (SCR-32): the transition state word, `1-7 · 2일째 · 새벽` with ` · 결사의 문`,
 * `직전 씬 +1일 · 정확 · 이야기 약 13일째`, and a `▲ 하르트 이동 시간 · 점검 센터` line per open
 * travel warning of the scene.
 */
data class SceneTimeModel(
    val sceneKey: Key,
    val transitionState: String,
    val headline: String,
    val place: String?,
    val detail: String?,
    val travel: List<String>,
)

/**
 * One row of `이 씬 안` (SCR-32): a background cue (`05:40 배경` · `하층 거리`), a block (`◆ #043` ·
 * `관계`, `#042` · `변화 없음`), or a point-seat line (`◆ 1-7부터 · 근거 블록 없음`, SCR-42) with no
 * block. [right] is the cover form; [rightWide] the inner one (`용어 첫 공개 · 오리지늄 등불`).
 */
data class InSceneRow(
    val blockKey: Key?,
    val left: String,
    val right: String,
    val rightWide: String = right,
    val glyph: String? = null,
    val selected: Boolean = false,
    val point: Boolean = false,
    /** A point-seat line's record: the row shows `블록에 잇기` (SCR-42). */
    val recordKey: Key? = null,
)

/** The `시간` tab (SCR-32): the selected block's records, the scene's place and time, and the scene's rows. */
data class TimeTabModel(
    /** `고른 블록 · #043`; null without a selected block (SCR-31 `[PROPOSED]`: scene content only). */
    val blockTitle: String?,
    /** Inner form `고른 블록 · #043 하르트`. */
    val blockTitleWide: String?,
    /** Empty with a [blockTitle] reads `변화 없음`. */
    val cards: List<RecordCard>,
    val scene: SceneTimeModel?,
    val inScene: List<InSceneRow>,
    /** Notes under the cards for deleted original blocks in the trash (SCR-43, SCR-39), one per block. */
    val trashNotes: List<TrashNote> = emptyList(),
    /** `◆ 변화 기록` at the end of the block section (SCR-41 `[PROPOSED]`): a live block in a writable project. */
    val canRecord: Boolean = false,
    /** Buttons that change the project (orphan fixes, `블록에 잇기`, `되살리기`) show only when it is writable. */
    val editable: Boolean = false,
    /** The selected block (the target of `◆ 변화 기록`). */
    val blockKey: Key? = null,
)

object TimeTab {
    fun of(
        doc: ProjectDoc,
        selected: Key?,
        headerScene: Key?,
        time: SceneTimeSource,
        layers: ScriptLayers = ScriptLayers(),
        unlocked: Boolean = false,
        readOnly: Boolean = false,
    ): TimeTabModel {
        val block = selected?.let(doc.index::block)?.takeIf { doc.manuscript.isLiveBlock(it.key) }
        val sceneKey = block?.sceneKey ?: headerScene?.takeIf { doc.manuscript.isLiveUnit(it) }
        val scene = sceneKey?.let(doc.index::unit)?.takeIf { !it.hidden || unlocked }
        val title = block?.let(Panels::selectedBlockTitle)
        val speaker = block?.takeIf { it.kind.known == BlockKind.DIALOGUE }?.speakerKey
            ?.takeIf { key -> doc.index.character(key)?.hidden != true || unlocked }
            ?.let { Labels.speakerName(doc, it) }
        val cards = block?.let { b -> PanelRecords.onBlock(doc, b.key).map { PanelRecords.card(doc, it, b.key) } }.orEmpty()
        return TimeTabModel(
            blockTitle = title,
            blockTitleWide = title?.let { ScriptCopy.selectedBlockWide(it, speaker) },
            cards = cards,
            scene = scene?.let { sceneModel(doc, it.key) },
            inScene = scene?.let { inScene(doc, it.key, block?.key, time, layers) }.orEmpty(),
            trashNotes = cards.mapNotNull { it.trashNote }.distinct(),
            canRecord = block != null && !readOnly,
            editable = !readOnly,
            blockKey = block?.key,
        )
    }

    fun sceneModel(doc: ProjectDoc, sceneKey: Key): SceneTimeModel {
        val scene = doc.index.unit(sceneKey)!!
        val sheet = SceneWhere.sheet(doc, sceneKey)
        val labels = TimeLabels(SceneTimes(doc))
        val result = if (sheet.flashback) labels.flashbackResult(sceneKey) else labels.resultLine(sceneKey)
        val episode = Labels.episodeCodeOfScene(doc, sceneKey)
        val headline = result?.first ?: listOf(episode, SceneText.headTime(null)).filter { it.isNotEmpty() }.joinToString(" · ")
        val base = if (sheet.flashback) null else baseText(scene.time, sheet.dayChip, sheet.baseLine?.let { if (it.isDirectlyPrevious) ScriptCopy.PREVIOUS_SCENE else it.text })
        val precision = sheet.precision?.takeIf { !sheet.flashback }?.let {
            if (it == TimePrecision.APPROX) ScriptCopy.PRECISION_APPROX else ScriptCopy.PRECISION_EXACT
        }
        val detail = listOfNotNull(base, precision, result?.second?.takeIf { it.isNotEmpty() }).joinToString(" · ").ifEmpty { null }
        return SceneTimeModel(
            sceneKey = sceneKey,
            transitionState = SceneText.transitionState(scene),
            headline = headline,
            place = SceneText.placeName(doc, scene.place),
            detail = detail,
            travel = sheet.travel.map { it.warning.characterName }.distinct().map(ScriptCopy::travelLine),
        )
    }

    /** `직전 씬 +1일`, `1-2 씬 1 기준 같은 날`, `직접`; null without 때. */
    fun baseText(time: SceneTime?, chip: DayChip?, base: String?): String? {
        if (chip == DayChip.DIRECT || time is SceneTime.Direct) return ScriptCopy.DIRECT_TIME
        val rel = time as? SceneTime.Relative ?: return null
        val interval = when (chip) {
            DayChip.SAME_DAY -> ScriptCopy.SAME_DAY
            DayChip.PLUS_ONE, DayChip.PLUS_TWO -> chip.word
            else -> if (rel.dayMin == rel.dayMax) "+${rel.dayMin}일" else "+${rel.dayMin}~${rel.dayMax}일"
        }
        return listOfNotNull(base, interval).joinToString(" ")
    }

    /** Rows of `이 씬 안` in manuscript order; the selected block's row is highlighted. */
    fun inScene(doc: ProjectDoc, sceneKey: Key, selected: Key?, time: SceneTimeSource, layers: ScriptLayers): List<InSceneRow> {
        val blocks = doc.manuscript.liveBlocksOf(sceneKey)
        val t = time.timeOf(sceneKey)
        var clock: String? = (t?.clock?.toString() ?: t?.band?.let(Labels::bandWord))
        val rows = mutableListOf<InSceneRow>()
        for (b in blocks) {
            if (b.kind.known == BlockKind.DIRECTION) {
                for (cue in b.cues.filter { it.kind.known == CueKind.BACKGROUND }) {
                    val name = cue.assetKey?.let { doc.index.scriptAsset(it)?.name }?.let(::placePart) ?: continue
                    val left = listOfNotNull(clock, ScriptCopy.BACKGROUND).joinToString(" ")
                    clock = null
                    rows += InSceneRow(b.key, left, name, selected = b.key == selected)
                }
                if (PanelRecords.onBlock(doc, b.key).isEmpty()) continue
            }
            rows += blockRow(doc, b.key, Labels.number(b.number), selected)
        }
        val points = Records.pointLines(doc, sceneKey, layers)
        val start = points.filter { !it.atEnd }.map { InSceneRow(null, it.text, "", point = true, recordKey = it.recordKey) }
        val end = points.filter { it.atEnd }.map { InSceneRow(null, it.text, "", point = true, recordKey = it.recordKey) }
        return start + rows + end
    }

    private fun blockRow(doc: ProjectDoc, blockKey: Key, number: String, selected: Key?): InSceneRow {
        val records = PanelRecords.onBlock(doc, blockKey)
        val changes = records.filterNot(PanelRecords::isReveal)
        val reveals = records.filter(PanelRecords::isReveal)
        val glyph = when {
            changes.isNotEmpty() -> "◆"
            reveals.isNotEmpty() -> "◇"
            else -> null
        }
        val kinds = changes.map { Records.kindLabel(doc, it) }.distinct()
        val right = (kinds + if (reveals.isNotEmpty()) listOf(ScriptCopy.REVEAL_ROW) else emptyList()).joinToString(" · ").ifEmpty { ScriptCopy.NO_CHANGE }
        val terms = reveals.map { Records.subject(doc, it) }
        val wide = if (terms.isEmpty()) right else "$right · ${terms.joinToString(" · ")}"
        return InSceneRow(blockKey, listOfNotNull(glyph, number).joinToString(" "), right, wide, glyph, selected = blockKey == selected)
    }

    /** `하층 거리(새벽)` → `하층 거리` (the band word stays with the time, board `CSheetTime`). */
    fun placePart(assetName: String): String = assetName.replace(Regex("""\s*\([^)]*\)\s*$"""), "").ifEmpty { assetName }
}
