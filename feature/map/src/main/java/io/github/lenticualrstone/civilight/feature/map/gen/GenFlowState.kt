package io.github.lenticualrstone.civilight.feature.map.gen

import io.github.lenticualrstone.civilight.domain.world.gen.ChoiceQuestion
import io.github.lenticualrstone.civilight.domain.world.gen.GenInput
import io.github.lenticualrstone.civilight.domain.world.gen.GenSession
import io.github.lenticualrstone.civilight.domain.world.gen.StepChoices
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.model.map.GeneratedMapKind
import io.github.lenticualrstone.civilight.model.map.GenerationHistoryEntry
import io.github.lenticualrstone.civilight.model.map.GenerationStep
import io.github.lenticualrstone.civilight.model.map.GenerationStepKind
import io.github.lenticualrstone.civilight.model.map.LandRatio
import io.github.lenticualrstone.civilight.model.map.MapFormat
import io.github.lenticualrstone.civilight.model.map.MapStyle
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The `지형 만들기` flow before `이 지도로 시작` (WLD-54, WLD-55), saveable as JSON: the page (1 =
 * `양식`, 2 = steps), the generator session's settings (domain `GenSession` is not serializable, so
 * its parts are kept here and rebuilt), the focused step, the last batch of candidates and the
 * chosen one (indices into the history), and the `등고선` switch.
 */
@Serializable
data class GenFlowState(
    val page: Int = 1,
    val kind: GeneratedMapKind = GeneratedMapKind.CONTINENT,
    val landRatio: LandRatio = LandRatio.HALF,
    val format: MapFormat = MapFormat.DETAIL,
    val style: MapStyle = MapStyle.SOFT,
    val contours: Boolean = false,
    val steps: List<GenerationStep> = emptyList(),
    val history: List<GenerationHistoryEntry> = emptyList(),
    /**
     * `종류` and `땅 비율` each history entry was made with (parallel to [history]; a history entry
     * stores only the steps), so a candidate chosen again is the same world after the chips changed.
     */
    val historyKinds: List<GeneratedMapKind> = emptyList(),
    val historyRatios: List<LandRatio> = emptyList(),
    val focus: GenerationStepKind = GenerationStepKind.COAST,
    /** History indices of the last `새 후보` batch (the inner candidate strip). */
    val batch: List<Int> = emptyList(),
    /** The history entry now shown, or null when the settings were changed since. */
    val chosen: Int? = null,
    /** True when the flow started on page 1 (`‹` then goes back to `양식`). */
    val fromFormats: Boolean = true,
) {
    fun session(): GenSession = GenSession(kind, landRatio, format, style, steps, history = history)

    fun input(): GenInput = session().input()

    fun encode(): String = JSON.encodeToString(serializer(), this)

    companion object {
        private val JSON = Json { ignoreUnknownKeys = true }

        fun decode(text: String?): GenFlowState? = text?.let { runCatching { JSON.decodeFromString(serializer(), it) }.getOrNull() }

        /** A first state with one fresh seed per step; [format] set when the flow starts at page 2. */
        fun start(nextSeed: () -> Int, format: MapFormat? = null): GenFlowState {
            val session = GenSession.start(nextSeed)
            return GenFlowState(
                page = if (format != null) 2 else 1,
                format = format ?: session.format,
                steps = session.steps,
                fromFormats = format == null,
            )
        }

        /** `MapGenerate.format` (a `MapFormat` name, any case), or null. */
        fun formatOf(id: String?): MapFormat? = id?.let { s -> MapFormat.entries.firstOrNull { it.name.equals(s.trim(), ignoreCase = true) } }
    }
}

/** The format cards of `양식` (board `MapFormat`), in board order; `글로 설명` is WLD-61's. */
enum class FormatCard(val format: MapFormat?, val sub: String) {
    HEX(MapFormat.HEX, MapCopy.FORMAT_HEX_SUB),
    POLY(MapFormat.POLY, MapCopy.FORMAT_POLY_SUB),
    DETAIL(MapFormat.DETAIL, MapCopy.FORMAT_DETAIL_SUB),
    OUTLINE(MapFormat.OUTLINE, MapCopy.FORMAT_OUTLINE_SUB),
    NODE(MapFormat.NODE, MapCopy.FORMAT_NODE_SUB),
    TEXT(null, MapCopy.FORMAT_TEXT_SUB),
    ;

    /** `육각 · 다각형 · 상세 지형 · 윤곽 · 노드` (the generator's format names) or `글로 설명`. */
    val title: String get() = format?.let(StepChoices::formatName) ?: MapCopy.FORMAT_TEXT

    companion object {
        /** The cards the picker draws: `글로 설명` only while it is offered (MapOffers.OFFERS_TEXT_TERRAIN). */
        val shown: List<FormatCard>
            get() = entries.filter { it.format != null || io.github.lenticualrstone.civilight.feature.map.common.MapOffers.OFFERS_TEXT_TERRAIN }
    }
}

/** A step row: `강 · 0031`, locked or not, focused or not. */
data class StepRowUi(val kind: GenerationStepKind, val name: String, val seed: String, val locked: Boolean, val focused: Boolean)

/** One chip question of the focused step; [toggle] questions (`호수`) draw as a switch. */
data class QuestionUi(val key: String, val label: String, val options: List<String>, val selected: String, val toggle: Boolean)

/** What the two pages draw. */
data class GenUi(
    /** `같은 세계 · 시드 4827 · 양식만 다름 · 시작 전까지 바꾸기 가능`. */
    val formatNote: String,
    /** `상세 지형 · 색면 · 시드 4827 · 해안선 고정` (inner small line). */
    val header: String,
    /** `상세 지형 · 색면 · 지형 만들기 2/2` (cover small line). */
    val coverSmall: String,
    val kinds: List<String>,
    val kindIndex: Int,
    val ratios: List<String>,
    val ratioIndex: Int,
    val styles: List<String>,
    val styleIndex: Int,
    /** `상세 지형 그림`. */
    val styleSection: String,
    val steps: List<StepRowUi>,
    /** `강 · 시드 0031` and `강만 다시 · 강 고정` of the focused step (cover card). */
    val cardTitle: String,
    val cardName: String,
    val cardSeed: String,
    val rerollLabel: String,
    val lockLabel: String,
    val focusedLocked: Boolean,
    val questions: List<QuestionUi>,
    /** `후보 기록 9`. */
    val historyLabel: String,
)

/** The reducer and readings of the flow; pure (seeds and time come in as arguments). */
object GenFlow {
    private fun of(state: GenFlowState, s: GenSession): GenFlowState = state.copy(steps = s.steps, history = s.history)

    fun pickFormat(state: GenFlowState, format: MapFormat): GenFlowState = state.copy(format = format)

    fun toSteps(state: GenFlowState): GenFlowState = state.copy(page = 2)

    fun toFormats(state: GenFlowState): GenFlowState = state.copy(page = 1)

    fun kind(state: GenFlowState, kind: GeneratedMapKind): GenFlowState = state.copy(kind = kind, chosen = null)

    fun ratio(state: GenFlowState, ratio: LandRatio): GenFlowState = state.copy(landRatio = ratio, chosen = null)

    fun style(state: GenFlowState, style: MapStyle): GenFlowState = state.copy(style = style)

    fun contours(state: GenFlowState, on: Boolean): GenFlowState = state.copy(contours = on)

    fun focus(state: GenFlowState, kind: GenerationStepKind): GenFlowState = state.copy(focus = kind)

    fun lock(state: GenFlowState, kind: GenerationStepKind, locked: Boolean): GenFlowState = of(state, state.session().lock(kind, locked))

    fun choose(state: GenFlowState, kind: GenerationStepKind, key: String, value: String): GenFlowState =
        of(state, state.session().choose(kind, key, value)).copy(chosen = null)

    /** `다시` on one step: a new seed for that step only; a locked step never changes (WLD-55). */
    fun reroll(state: GenFlowState, kind: GenerationStepKind, seed: Int): GenFlowState =
        if (state.session().step(kind).locked) state else of(state, state.session().reroll(kind, seed)).copy(chosen = null)

    /** `새 후보`: 3 candidates with new seeds for unlocked steps, kept in the history; the first is shown. */
    fun newCandidates(state: GenFlowState, nextSeed: () -> Int, now: Long, count: Int = 3): GenFlowState {
        val (withHistory, candidates) = state.session().newCandidates(count, nextSeed, now)
        val first = state.history.size
        val batch = (first until first + candidates.size).toList()
        return state.copy(
            steps = candidates.first().steps,
            history = withHistory.history,
            historyKinds = kindsOf(state) + List(candidates.size) { state.kind },
            historyRatios = ratiosOf(state) + List(candidates.size) { state.landRatio },
            batch = batch,
            chosen = first,
        )
    }

    /** [GenFlowState.historyKinds] padded to the history (entries from before it was kept use the current chips). */
    private fun kindsOf(state: GenFlowState): List<GeneratedMapKind> =
        List(state.history.size) { i -> state.historyKinds.getOrElse(i) { state.kind } }

    private fun ratiosOf(state: GenFlowState): List<LandRatio> =
        List(state.history.size) { i -> state.historyRatios.getOrElse(i) { state.landRatio } }

    /** Picks a candidate of the history again with its own `종류` and `땅 비율` (same version → same world). */
    fun chooseCandidate(state: GenFlowState, index: Int): GenFlowState {
        val s = state.session().chooseFromHistory(index) ?: return state
        return state.copy(
            steps = s.steps,
            kind = state.historyKinds.getOrElse(index) { state.kind },
            landRatio = state.historyRatios.getOrElse(index) { state.landRatio },
            chosen = index,
        )
    }

    /** The generator input of history entry [index] (thumbnails), with the chips it was made with. */
    fun candidateInput(state: GenFlowState, index: Int): GenInput? =
        state.history.getOrNull(index)?.let {
            GenInput(state.historyKinds.getOrElse(index) { state.kind }, state.historyRatios.getOrElse(index) { state.landRatio }, it.steps)
        }

    fun ui(state: GenFlowState): GenUi {
        val s = state.session()
        val focusStep = s.step(state.focus)
        val (cardTitle, buttons) = s.stepCard(state.focus)
        val formatName = StepChoices.formatName(state.format)
        val styleName = StepChoices.styleName(state.style)
        return GenUi(
            formatNote = s.formatNote,
            header = s.header,
            coverSmall = MapCopy.genSmall(formatName, styleName),
            kinds = GeneratedMapKind.entries.map(StepChoices::kindName),
            kindIndex = state.kind.ordinal,
            ratios = LandRatio.entries.map(StepChoices::landRatioName),
            ratioIndex = state.landRatio.ordinal,
            styles = MapStyle.entries.map(StepChoices::styleName),
            styleIndex = state.style.ordinal,
            styleSection = MapCopy.styleSection(formatName),
            steps = StepChoices.ORDER.map { k ->
                val step = s.step(k)
                StepRowUi(k, StepChoices.stepName(k), StepChoices.seedLabel(step.seed), step.locked, k == state.focus)
            },
            cardTitle = cardTitle,
            cardName = StepChoices.stepName(state.focus),
            cardSeed = StepChoices.seedLabel(focusStep.seed),
            rerollLabel = buttons[0],
            lockLabel = buttons[1],
            focusedLocked = focusStep.locked,
            questions = StepChoices.questions(state.focus).map { q -> question(q, focusStep.choices[q.key] ?: q.default) },
            historyLabel = s.historyLabel,
        )
    }

    private fun question(q: ChoiceQuestion, selected: String): QuestionUi {
        val toggle = q.options == listOf(StepChoices.ON, StepChoices.OFF)
        return QuestionUi(q.key, if (toggle) MapCopy.LAKE else q.label, q.options, selected, toggle)
    }
}
