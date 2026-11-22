package io.github.lenticualrstone.civilight.domain.story.travel

import io.github.lenticualrstone.civilight.domain.story.scene.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.Position
import io.github.lenticualrstone.civilight.model.position.Rank
import io.github.lenticualrstone.civilight.model.position.recordAt
import io.github.lenticualrstone.civilight.model.project.TravelMode
import io.github.lenticualrstone.civilight.model.story.TravelSegmentOverride
import kotlin.math.floor
import kotlin.math.roundToInt

/** Where the mode of a pair came from (STO-28 precedence). */
enum class ModeSource { OVERRIDE, CHARACTER_FIELD, PROJECT_DEFAULT }

data class ModeChoice(val mode: TravelMode, val source: ModeSource)

/** Travel modes (WLD-47) as used by the travel check (STO-28). */
object TravelModes {
    /** Name of the optional character field that holds a default mode (`기본 이동 수단`). */
    const val CHARACTER_FIELD_NAME = "기본 이동 수단"

    fun modes(doc: ProjectDoc): List<TravelMode> = doc.project.travelModes.sortedWith(compareBy<TravelMode> { it.order }.thenBy { it.key })

    /** The project default: `defaultTravelModeKey`, else `도보`, else the first mode. */
    fun default(doc: ProjectDoc): TravelMode? {
        val all = modes(doc)
        return doc.project.defaultTravelModeKey?.let { k -> all.firstOrNull { it.key == k } }
            ?: all.firstOrNull { it.name == "도보" }
            ?: all.firstOrNull()
    }

    /**
     * Mode for one character's pair (STO-28): the per-segment override → the character's
     * `기본 이동 수단` (value in force at the later scene's start, `ED`) → the project default. The
     * app never picks a faster mode by itself.
     */
    fun forPair(doc: ProjectDoc, characterKey: Key, fromSceneKey: Key, toSceneKey: Key): ModeChoice? {
        val all = modes(doc)
        doc.checks.travelOverrides.lastOrNull { it.characterKey == characterKey && it.fromSceneKey == fromSceneKey && it.toSceneKey == toSceneKey }
            ?.let { o -> all.firstOrNull { it.key == o.modeKey } }
            ?.let { return ModeChoice(it, ModeSource.OVERRIDE) }
        characterMode(doc, characterKey, toSceneKey)?.let { return ModeChoice(it, ModeSource.CHARACTER_FIELD) }
        return default(doc)?.let { ModeChoice(it, ModeSource.PROJECT_DEFAULT) }
    }

    private fun characterMode(doc: ProjectDoc, characterKey: Key, atSceneKey: Key): TravelMode? {
        val def = doc.project.fieldDefs.firstOrNull {
            it.entityKind.known == EntityKind.CHARACTER && it.name == CHARACTER_FIELD_NAME && it.deletedAt == null
        } ?: return null
        val target = ChangeTarget.Field(EntityKind.CHARACTER, characterKey, def.key)
        val rank = doc.manuscript.rankOf(Position.Start(atSceneKey)) ?: Rank.UNPLACED
        val record = doc.manuscript.recordAt(doc.changes.records, target, rank) { doc.liveness.isLive(it) }
        val value = if (record != null) record.valueAs(FieldValue.serializer()) else doc.index.character(characterKey)?.fields?.get(def.key)
        val name = when (val p = value?.payload) {
            is FieldPayload.Texts -> p.values.firstOrNull()
            is FieldPayload.Choice -> p.optionKeys.firstOrNull()?.let { k -> def.options?.firstOrNull { it.key == k }?.label } ?: p.freeText
            else -> null
        }?.trim() ?: return null
        return modes(doc).firstOrNull { it.name == name }
    }

    /**
     * Required minutes (STO-29, must equal WLD-66 `requiredMinutes`): `full = floor(d / k)`,
     * `rest = d − full·k`, `required = full·24 h + rest / k · h`, rounded to whole minutes.
     */
    fun requiredMinutes(distanceKm: Double, mode: TravelMode): Int {
        val k = mode.kmPerDay
        if (k <= 0.0) return Int.MAX_VALUE
        val full = floor(distanceKm / k + 1e-9)
        val rest = (distanceKm - full * k).coerceAtLeast(0.0)
        return (full * 24 * 60 + rest / k * mode.movingHoursPerDay * 60).roundToInt()
    }

    /** `도보 15 km · 하루 8시간`. */
    fun detail(mode: TravelMode): String = "${mode.name} ${number(mode.kmPerDay)} km · 하루 ${number(mode.movingHoursPerDay)}시간"

    /** `15` or `12.5`. */
    fun number(v: Double): String = if (v == floor(v)) v.toLong().toString() else String.format(java.util.Locale.ROOT, "%.1f", v)

    /**
     * The mode chip: stores (or replaces) the override for (character, from, to) immediately; one
     * undo step (the previous doc).
     */
    fun setOverride(doc: ProjectDoc, characterKey: Key, fromSceneKey: Key, toSceneKey: Key, modeKey: Key, ctx: EditContext): ProjectDoc {
        val kept = doc.checks.travelOverrides.filterNot {
            it.characterKey == characterKey && it.fromSceneKey == fromSceneKey && it.toSceneKey == toSceneKey
        }
        val row = TravelSegmentOverride(ctx.newKey(), characterKey, fromSceneKey, toSceneKey, modeKey, ctx.now, ctx.device)
        return doc.copy(checks = doc.checks.copy(travelOverrides = kept + row))
    }
}
