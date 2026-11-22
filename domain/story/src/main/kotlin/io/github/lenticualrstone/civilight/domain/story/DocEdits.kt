package io.github.lenticualrstone.civilight.domain.story

import io.github.lenticualrstone.civilight.domain.story.scene.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/** Replaces units by key (stamped with [ctx] when given); units not in [changes] are kept as they are. */
internal fun ProjectDoc.withUnits(changes: Map<Key, StoryUnit>, ctx: EditContext? = null): ProjectDoc {
    if (changes.isEmpty()) return this
    val stamped = if (ctx == null) changes else changes.mapValues { (_, u) -> u.copy(updatedAt = ctx.now, updatedBy = ctx.device) }
    return copy(story = story.copy(units = story.units.map { stamped[it.key] ?: it }))
}

/** Sets the stored 때 of scenes (stamped when [ctx] is given). */
internal fun ProjectDoc.withTimes(times: Map<Key, SceneTime>, ctx: EditContext? = null): ProjectDoc =
    withUnits(times.mapNotNull { (k, t) -> index.unit(k)?.let { k to it.copy(time = t) } }.toMap(), ctx)
