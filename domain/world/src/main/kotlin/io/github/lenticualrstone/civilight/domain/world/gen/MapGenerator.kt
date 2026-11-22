package io.github.lenticualrstone.civilight.domain.world.gen

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.map.HexCell
import io.github.lenticualrstone.civilight.domain.world.map.HexMaps
import io.github.lenticualrstone.civilight.domain.world.map.MapEdit
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.domain.world.map.MapScales
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.GeneratedMapKind
import io.github.lenticualrstone.civilight.model.map.GeneratedMapRecord
import io.github.lenticualrstone.civilight.model.map.GenerationHistoryEntry
import io.github.lenticualrstone.civilight.model.map.GenerationStep
import io.github.lenticualrstone.civilight.model.map.GenerationStepKind
import io.github.lenticualrstone.civilight.model.map.LandRatio
import io.github.lenticualrstone.civilight.model.map.MapBaseKind
import io.github.lenticualrstone.civilight.model.map.MapFormat
import io.github.lenticualrstone.civilight.model.map.MapParent
import io.github.lenticualrstone.civilight.model.map.MapStyle
import io.github.lenticualrstone.civilight.model.map.ScaleUnit
import io.github.lenticualrstone.civilight.model.map.TextCondition
import io.github.lenticualrstone.civilight.model.map.TextModelInfo
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.open

/**
 * The generator flow before `이 지도로 시작` (WLD-54, WLD-55, WLD-57): base chips, format and style
 * (changeable until start; the world stays the same), five steps with seed, `고정` and `다시`, chip
 * choices, `새 후보` and the candidate history (`후보 기록 N`). Every candidate is kept as its step
 * settings and can be chosen again (same version → same world).
 */
data class GenSession(
    val kind: GeneratedMapKind = GeneratedMapKind.CONTINENT,
    val landRatio: LandRatio = LandRatio.HALF,
    val format: MapFormat = MapFormat.DETAIL,
    val style: MapStyle = MapStyle.SOFT,
    val steps: List<GenerationStep>,
    val conditions: List<TextCondition> = emptyList(),
    val textModel: TextModelInfo? = null,
    val history: List<GenerationHistoryEntry> = emptyList(),
) {
    fun input(): GenInput = GenInput(kind, landRatio, steps, conditions)

    /** The world these settings give now. */
    fun preview(): GeneratedWorld = TerrainGenerator.generate(input())

    fun step(kind: GenerationStepKind): GenerationStep = input().step(kind)

    private fun update(kind: GenerationStepKind, f: (GenerationStep) -> GenerationStep): GenSession =
        copy(steps = StepChoices.ORDER.map { k -> step(k).let { if (k == kind) f(it) else it } })

    fun lock(kind: GenerationStepKind, locked: Boolean): GenSession = update(kind) { it.copy(locked = locked) }

    fun choose(kind: GenerationStepKind, key: String, value: String): GenSession = update(kind) { it.copy(choices = it.choices + (key to value)) }

    /** `다시` on one step: a new seed for that step only; a locked step never changes. */
    fun reroll(kind: GenerationStepKind, seed: Int): GenSession = if (step(kind).locked) this else update(kind) { it.copy(seed = seed) }

    /**
     * `새 후보`: [count] candidates with new seeds for every unlocked step, each added to the
     * history. Returns the session (with the history) and the candidates.
     */
    fun newCandidates(count: Int, nextSeed: () -> Int, now: Long): Pair<GenSession, List<GenSession>> {
        val candidates = (0 until count).map { _ ->
            copy(steps = StepChoices.ORDER.map { k -> step(k).let { s -> if (s.locked) s else s.copy(seed = nextSeed()) } })
        }
        val entries = candidates.map { GenerationHistoryEntry(createdAt = now, steps = it.steps) }
        val withHistory = copy(history = history + entries)
        return withHistory to candidates.map { it.copy(history = withHistory.history) }
    }

    /** Picks a candidate of the history again. */
    fun chooseFromHistory(index: Int): GenSession? = history.getOrNull(index)?.let { copy(steps = it.steps) }

    val historyLabel: String get() = "후보 기록 ${history.size}"

    /** `상세 지형 · 색면 · 시드 4827 · 해안선 고정`. */
    val header: String
        get() {
            val locked = StepChoices.ORDER.filter { step(it).locked }.map(StepChoices::stepName)
            val parts = listOf(StepChoices.formatName(format), StepChoices.styleName(style), "시드 ${StepChoices.seedLabel(step(GenerationStepKind.COAST).seed)}") +
                (if (locked.isEmpty()) emptyList() else listOf("${locked.joinToString(Copy.SEP)} 고정"))
            return parts.joinToString(Copy.SEP)
        }

    /** Note of the format screen: `같은 세계 · 시드 4827 · 양식만 다름 · 시작 전까지 바꾸기 가능`. */
    val formatNote: String
        get() = "같은 세계${Copy.SEP}시드 ${StepChoices.seedLabel(step(GenerationStepKind.COAST).seed)}${Copy.SEP}양식만 다름${Copy.SEP}시작 전까지 바꾸기 가능"

    /** Cover step card: `강 · 시드 0031` with `강만 다시 · 강 고정`. */
    fun stepCard(kind: GenerationStepKind): Pair<String, List<String>> {
        val name = StepChoices.stepName(kind)
        return "$name${Copy.SEP}시드 ${StepChoices.seedLabel(step(kind).seed)}" to listOf("${name}만 다시", "$name 고정")
    }

    companion object {
        /** A first session with one seed per step from [nextSeed]. */
        fun start(nextSeed: () -> Int, format: MapFormat = MapFormat.DETAIL, style: MapStyle = MapStyle.SOFT): GenSession =
            GenSession(format = format, style = style, steps = StepChoices.ORDER.map { GenerationStep(it.open(), seed = nextSeed()) })
    }
}

/** `이 지도로 시작` (WLD-53): the result is fixed as data with the seeds, choices and generator version. */
object MapGenerator {
    /**
     * Creates the generated map. The stored result is the baseline; seeds are used only for `다시`.
     * HEX maps get hex cells and a scale from the cell size; POLY maps cells and provinces; DETAIL
     * maps rivers, roads and stamps; OUTLINE maps contours on. `노드` makes a base-less node map.
     */
    fun start(doc: ProjectDoc, session: GenSession, ctx: EditContext, name: String? = null, parent: MapParent? = null): MapEdit<WorldMap> {
        if (session.format == MapFormat.NODE) return MapEdits.newMap(doc, MapBaseKind.NODE, ctx, name, parent)
        val world = session.preview()
        val conditions = TextConditions.evaluate(world, session.conditions)
        var map = WorldMap(
            key = ctx.newKey(),
            order = OrderKeys.after(doc.maps.values.maxOfOrNull { it.order }),
            name = name?.trim()?.takeIf { it.isNotEmpty() } ?: "지도 ${doc.maps.size + 1}",
            parent = parent,
            baseKind = MapBaseKind.GENERATED.open(),
            format = session.format.open(),
            style = session.style.open(),
            contours = session.format == MapFormat.OUTLINE,
            generation = GeneratedMapRecord(
                generatorVersion = world.version,
                kind = session.kind.open(),
                landRatio = session.landRatio.open(),
                steps = session.steps,
                result = GenResults.encode(world),
                history = session.history,
                conditions = conditions.takeIf { it.isNotEmpty() },
                textModel = session.textModel,
            ),
            updatedAt = ctx.now(),
            updatedBy = ctx.device,
        )
        map = when (session.format) {
            MapFormat.HEX -> {
                val hex = GenFormats.hex(world)
                val a = HexMaps.center(hex, HexCell(0, 0))
                val b = HexMaps.center(hex, HexCell(0, 1))
                map.copy(hex = hex, scale = MapScales.scale(a, b, hex.cellKm, ScaleUnit.KM))
            }
            MapFormat.POLY -> map.copy(poly = GenFormats.poly(world, session.step(GenerationStepKind.COAST).seed, ctx))
            MapFormat.DETAIL -> map.copy(drawnLines = GenFormats.drawnLines(world, ctx), stamps = GenFormats.stamps(world, ctx))
            else -> map
        }
        return MapEdit.Applied(doc.copy(maps = doc.maps + (map.key to map)), map)
    }

    /** The stored world of a generated map, or null when the stored result is not complete. */
    fun storedWorld(map: WorldMap): GeneratedWorld? = map.generation?.result?.let { GenResults.decode(it).toWorld() }
}
