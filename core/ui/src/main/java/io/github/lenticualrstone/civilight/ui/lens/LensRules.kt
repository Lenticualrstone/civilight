package io.github.lenticualrstone.civilight.ui.lens

import io.github.lenticualrstone.civilight.designsystem.component.LensChipState
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.LensState
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.position.Position
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.ui.labels.UnitLabels

/** Copy of the lens chip and sheet (STO-13). */
object LensCopy {
    const val LATEST = "최신"
    const val POINT = "시점"
    const val ALL = "전체"
    const val SHEET_TITLE = "시점 렌즈"
    const val SHEET_SUB = "앱 전체 적용 · 이 시점까지의 필드 · 관계 · 용어 공개"
    const val LATEST_SUB = "기본값 · 지금까지 쓴 모든 내용"
    const val NOT_WRITTEN = "아직 쓰지 않음"
    const val VIEWING = "보는 중"
    const val MORE = "더 자세히"
    const val BY_SCENE = "씬 단위로 고르기"
    const val BACK_TO_LATEST = "최신으로 돌아가기"
    const val APPLY = "이 시점으로 보기"
    const val UNTITLED = "미정"
}

/** What the lens chip shows. */
data class LensChipLabel(val text: String, val state: LensChipState)

/**
 * The lens rules the shell owns (STO-13, STO-16). The value itself lives in the project's
 * `_app/view.json` ([LensState]) and is written through `ProjectStore.updateApp`, so it also works
 * in the read-only example.
 */
object LensRules {

    /** Entering the script (STO-16): the lens is kept as it is; a stale `렌즈를 이 씬으로 맞추기` is dropped. */
    fun enterScript(state: LensState): LensState = state.copy(savedForScript = null)

    /** `렌즈를 이 씬으로 맞추기` inside the script: applied when the script is left. */
    fun setLensToScene(state: LensState, sceneKey: Key): LensState = state.copy(savedForScript = LensPoint.UnitEnd(sceneKey))

    /** Leaving the script restores the stored lens, or applies the scene point chosen in it. */
    fun leaveScript(state: LensState): LensState =
        state.savedForScript?.let { LensState(point = it, savedForScript = null) } ?: state

    /** A lens point naming a unit that is not live reads as 최신 for labels and rows. */
    fun effective(doc: ProjectDoc, point: LensPoint): LensPoint = when (point) {
        is LensPoint.UnitEnd -> if (doc.liveness.isUnitLive(point.unitKey)) point else LensPoint.Latest
        else -> point
    }

    /** `최신 · 1-7`, `최신 · 전체`, `시점 · 1-5`, `시점 · 1-7 씬 2` (STO-13). */
    fun chipLabel(doc: ProjectDoc, point: LensPoint, labels: UnitLabels = UnitLabels(doc)): LensChipLabel =
        when (val p = effective(doc, point)) {
            is LensPoint.UnitEnd -> {
                val unit = doc.index.unit(p.unitKey)
                val code = unit?.let(labels::reference) ?: LensCopy.ALL
                LensChipLabel("${LensCopy.POINT}${UnitLabels.SEP}$code", LensChipState.PAST)
            }
            else -> {
                val sceneKey = doc.manuscript.writingLineSceneKey
                val scene = sceneKey?.let(doc.index::unit)
                val code = scene?.let { s -> labels.episodeOf(s)?.let(labels::code) ?: labels.reference(s) } ?: LensCopy.ALL
                LensChipLabel("${LensCopy.LATEST}${UnitLabels.SEP}$code", LensChipState.LATEST)
            }
        }
}

/** One row of the lens sheet. */
data class LensRow(
    val point: LensPoint,
    /** `최신`, `1-5`, `1-7 씬 2`. */
    val code: String,
    val sub: String,
    /** Beyond the 집필 선: drawn `ink3`, still selectable (R11 §2). */
    val notWritten: Boolean,
)

/**
 * Rows of the lens sheet (STO-13 `ED`): 최신, then in manuscript order the episodes that hold a
 * change record ([marked], by default every episode a live record is anchored in), the [viewed]
 * point, and the first episode beyond the 집필 선.
 */
class LensRows(private val doc: ProjectDoc, private val labels: UnitLabels = UnitLabels(doc)) {
    private val structure = doc.project.structure
    private val pointLevel: Int = structure.episodeLevel ?: structure.sceneLevel

    /** Live units of the lens level (episodes, or scenes in a 1-level project) in manuscript order. */
    val episodes: List<StoryUnit> by lazy {
        val out = ArrayList<StoryUnit>()
        fun visit(parent: Key?) {
            for (unit in doc.index.children(parent)) {
                if (!doc.liveness.isLive(unit)) continue
                if (unit.level == pointLevel) out += unit else if (unit.level < pointLevel) visit(unit.key)
            }
        }
        visit(null)
        out
    }

    /** Episodes a live change record is anchored in. */
    val episodesWithRecords: Set<Key> by lazy {
        doc.records.filter { doc.liveness.isLive(it) }.mapNotNullTo(HashSet()) { episodeKeyOf(it.anchor) }
    }

    fun rows(viewed: LensPoint, marked: Set<Key> = episodesWithRecords): List<LensRow> {
        val viewedPoint = LensRules.effective(doc, viewed)
        val viewedKey = (viewedPoint as? LensPoint.UnitEnd)?.unitKey
        val firstUnwritten = episodes.firstOrNull(::isNotWritten)?.key
        val wanted = marked + listOfNotNull(firstUnwritten, viewedKey?.let { k -> doc.index.unit(k)?.let { episodeKeyOfUnit(it) } })
        val rows = mutableListOf(LensRow(LensPoint.Latest, LensCopy.LATEST, LensCopy.LATEST_SUB, notWritten = false))
        for (episode in episodes) {
            if (episode.key in wanted) rows += rowOf(episode)
            if (viewedKey != null && viewedKey != episode.key && doc.index.unit(viewedKey)?.let(::episodeKeyOfUnit) == episode.key) {
                doc.index.unit(viewedKey)?.let { rows += rowOf(it) }
            }
        }
        return rows
    }

    /** The episode row before and after [episodeKey] with it in the middle (`더 자세히` chips). */
    fun neighbours(episodeKey: Key): List<StoryUnit> {
        val i = episodes.indexOfFirst { it.key == episodeKey }
        if (i < 0) return emptyList()
        return episodes.subList(maxOf(0, i - 1), minOf(episodes.size, i + 2))
    }

    /** Scene rows of an episode (`씬 단위로 고르기`). */
    fun sceneRows(episodeKey: Key): List<LensRow> =
        doc.manuscript.liveScenes.filter { episodeKeyOfUnit(it) == episodeKey }.map(::rowOf)

    fun rowOf(unit: StoryUnit): LensRow {
        val notWritten = isNotWritten(unit)
        val code = labels.reference(unit) ?: LensCopy.UNTITLED
        val sub = when {
            notWritten -> LensCopy.NOT_WRITTEN
            else -> unit.title?.takeIf { it.isNotBlank() } ?: LensCopy.UNTITLED
        }
        return LensRow(LensPoint.UnitEnd(unit.key), code, sub, notWritten)
    }

    /** True when the unit starts after the 집필 선 (nothing of it is written yet). */
    fun isNotWritten(unit: StoryUnit): Boolean {
        val line = doc.manuscript.writingLineRank ?: return true
        val start = doc.manuscript.rankOf(Position.Start(unit.key)) ?: return true
        return start > line
    }

    private fun episodeKeyOf(anchor: Anchor): Key? = when (anchor) {
        is Anchor.AtBlock -> doc.index.block(anchor.blockKey)?.let { doc.index.unit(it.sceneKey) }?.let(::episodeKeyOfUnit)
        is Anchor.UnitStart -> doc.index.unit(anchor.unitKey)?.let(::episodeKeyOfUnit)
        is Anchor.UnitEnd -> doc.index.unit(anchor.unitKey)?.let(::episodeKeyOfUnit)
        else -> null
    }

    private fun episodeKeyOfUnit(unit: StoryUnit): Key? = when {
        unit.level == pointLevel -> unit.key
        unit.level > pointLevel -> doc.index.ancestors(unit).firstOrNull { it.level == pointLevel }?.key
        else -> null
    }
}
