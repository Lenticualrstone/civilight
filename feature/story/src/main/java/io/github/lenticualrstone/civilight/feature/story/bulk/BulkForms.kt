package io.github.lenticualrstone.civilight.feature.story.bulk

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.story.bulk.BandChoice
import io.github.lenticualrstone.civilight.domain.story.bulk.BulkPlace
import io.github.lenticualrstone.civilight.domain.story.bulk.BulkWhen
import io.github.lenticualrstone.civilight.domain.story.bulk.PlaceOption
import io.github.lenticualrstone.civilight.domain.story.bulk.Suggestions
import io.github.lenticualrstone.civilight.domain.story.bulk.WhenMode
import io.github.lenticualrstone.civilight.domain.story.bulk.WhenPlan
import io.github.lenticualrstone.civilight.domain.story.units.PlaceNames
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.TimePrecision

/**
 * `장소 정하기` (STO-32, `BulkPlace`): the list, its footer and the one-step edits. Every edit is
 * recomputed on the doc it applies to: scenes that are gone by then are skipped.
 */
object BulkPlaceForm {
    /** Places (or regions) with their scene counts in the chapter, most used first. */
    fun options(doc: ProjectDoc, scope: List<Key>, regions: Boolean, locked: Boolean = false): List<PlaceOption> =
        BulkPlace.options(doc, scope, if (regions) PlaceRefKind.REGION else PlaceRefKind.PLACE).filter { BulkModel.visible(doc, it.ref, locked) }

    /** `씬 4개 → 잿더미 시장` (`모두 같은 장소`), or the names in scene order (`씬마다 차례로`). */
    fun footer(doc: ProjectDoc, count: Int, refs: List<PlaceRef>): String? {
        if (refs.isEmpty()) return null
        val names = PlaceNames(doc)
        return BulkCopy.placeResult(count, refs.mapNotNull(names::name))
    }

    /** `모두 같은 장소`: every live chosen scene gets [place]; one undo step. */
    fun applySame(doc: ProjectDoc, selected: List<Key>, place: PlaceRef, ctx: ActionContext): ProjectDoc {
        val live = live(doc, selected)
        return if (live.isEmpty() || !placeExists(doc, place)) doc else BulkPlace.applySame(doc, live, place, ctx.edit)
    }

    /** `씬마다 차례로`: each scene its own place; one undo step. */
    fun applyEach(doc: ProjectDoc, choices: Map<Key, PlaceRef>, ctx: ActionContext): ProjectDoc {
        val live = choices.filterKeys { it in live(doc, choices.keys.toList()) }.filterValues { placeExists(doc, it) }
        return if (live.isEmpty()) doc else BulkPlace.applyEach(doc, live, ctx.edit)
    }

    private fun placeExists(doc: ProjectDoc, ref: PlaceRef): Boolean = PlaceNames(doc).name(ref) != null && !PlaceNames(doc).isDeleted(ref)

    fun live(doc: ProjectDoc, keys: List<Key>): List<Key> {
        val live = doc.manuscript.liveScenes.map { it.key }.toSet()
        return keys.filter { it in live }
    }
}

/**
 * The `때 정하기` form (STO-33, `BulkWhen`) as saveable values: [mode], the `씬 사이` chip ([dayChip] 0 · 1 · 2)
 * or `씬마다 고르기` ([perScene] with a chip per scene), the `시간대` choice ([band]: `KEEP` · `CLEAR` · a band
 * name) and the precision (default `대략`).
 */
data class BulkWhenForm(
    val mode: WhenMode = WhenMode.CHAIN,
    val dayChip: Int = 1,
    val perScene: Boolean = false,
    val perSceneChips: Map<Key, Int> = emptyMap(),
    val band: String = KEEP,
    val precision: TimePrecision = TimePrecision.APPROX,
) {
    val bandChoice: BandChoice
        get() = when (band) {
            KEEP -> BandChoice.Keep
            CLEAR -> BandChoice.Clear
            else -> Band.entries.firstOrNull { it.name == band }?.let(BandChoice::Set) ?: BandChoice.Keep
        }

    /** The preview and result on [doc] (nothing is stored before the caller takes [WhenPlan.result]). */
    fun plan(doc: ProjectDoc, selected: List<Key>, ctx: ActionContext? = null): WhenPlan =
        BulkWhen.plan(
            doc = doc,
            selected = BulkPlaceForm.live(doc, selected),
            mode = mode,
            dayChip = dayChip,
            band = bandChoice,
            precision = precision,
            perScene = if (perScene) perSceneChips else emptyMap(),
            ctx = ctx?.edit,
        )

    /** The chip a scene gets under `씬마다 고르기` (the shared chip until it is changed). */
    fun chipOf(sceneKey: Key): Int = perSceneChips[sceneKey] ?: dayChip

    fun encode(): String = listOf(mode.name, dayChip, perScene, perSceneChips.entries.joinToString(",") { "${it.key}=${it.value}" }, band, precision.name).joinToString(";")

    companion object {
        const val KEEP = "KEEP"
        const val CLEAR = "CLEAR"

        /** The three `씬 사이` chips: `같은 날 · +1일 · +2일`. */
        val DAY_CHIPS: List<Int> = listOf(0, 1, 2)

        fun decode(s: String?): BulkWhenForm {
            val p = s?.split(";") ?: return BulkWhenForm()
            if (p.size != 6) return BulkWhenForm()
            return BulkWhenForm(
                mode = WhenMode.entries.firstOrNull { it.name == p[0] } ?: WhenMode.CHAIN,
                dayChip = p[1].toIntOrNull()?.coerceIn(0, 2) ?: 1,
                perScene = p[2] == "true",
                perSceneChips = p[3].split(",").filter { "=" in it }.mapNotNull { e ->
                    val (k, v) = e.split("=", limit = 2)
                    v.toIntOrNull()?.let { k to it }
                }.toMap(),
                band = p[4],
                precision = TimePrecision.entries.firstOrNull { it.name == p[5] } ?: TimePrecision.APPROX,
            )
        }
    }
}

/** `제안 받기` (STO-34, `BulkSuggest`): `넣기` · `모두 넣기`, each recomputed on the doc it is applied to (SUG-05). */
object BulkSuggestForm {
    /** `넣기`: the scene's proposal as it reads now; nothing when it no longer has one. */
    fun put(doc: ProjectDoc, sceneKey: Key, ctx: ActionContext): ProjectDoc {
        val suggestion = Suggestions(doc).suggestionFor(sceneKey) ?: return doc
        return Suggestions.apply(doc, suggestion, ctx.edit)
    }

    /** `제안 n개 모두 넣기`: every proposal of [sceneKeys], one undo step. */
    fun putAll(doc: ProjectDoc, sceneKeys: List<Key>, ctx: ActionContext): ProjectDoc {
        val suggestions = Suggestions(doc)
        val all = sceneKeys.mapNotNull(suggestions::suggestionFor)
        return if (all.isEmpty()) doc else Suggestions.applyAll(doc, all, ctx.edit)
    }
}
