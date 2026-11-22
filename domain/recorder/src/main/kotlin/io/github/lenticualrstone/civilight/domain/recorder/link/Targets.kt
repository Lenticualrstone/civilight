package io.github.lenticualrstone.civilight.domain.recorder.link

import io.github.lenticualrstone.civilight.domain.recorder.ProjectSnapshot
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind

/** Where a link target stands (§3 resolve, without the hiding case). */
enum class TargetState { LIVE, TRASHED, PURGED }

/**
 * A resolved link target. [project] is null when the project is permanently deleted. [label] is
 * the current label while the row exists; [hidden] is rule-19 item hiding (for a project link: the
 * project's own hiding); [isScene] is known only while the unit row exists.
 */
data class ResolvedTarget(
    val ref: TargetRef,
    val project: ProjectSnapshot?,
    val state: TargetState,
    val hidden: Boolean,
    val label: String?,
    val isScene: Boolean? = null,
)

/** Resolution of recorder link targets against the projects of the 서재. */
object Targets {
    fun resolve(env: RecorderEnv, ref: TargetRef): ResolvedTarget {
        val project = env.project(ref.projectKey)
            ?: return ResolvedTarget(ref, null, TargetState.PURGED, hidden = false, label = null)
        val doc = project.doc
        val liveness = doc.liveness
        val index = doc.index
        fun state(exists: Boolean, live: Boolean): TargetState = when {
            !exists -> TargetState.PURGED
            !live || project.trashed -> TargetState.TRASHED
            else -> TargetState.LIVE
        }
        return when (ref.kind) {
            RecorderTargetKind.PROJECT -> ResolvedTarget(
                ref, project, if (project.trashed) TargetState.TRASHED else TargetState.LIVE, project.hidden, project.name,
            )
            RecorderTargetKind.UNIT -> {
                val unit = index.unit(ref.key)
                val labels = UnitLabels(doc)
                ResolvedTarget(
                    ref, project, state(unit != null, unit != null && liveness.isLive(unit)),
                    hidden = unit?.hidden == true, label = unit?.let(labels::label), isScene = unit?.let(labels::isScene),
                )
            }
            RecorderTargetKind.CHARACTER -> {
                val row = index.character(ref.key)
                ResolvedTarget(ref, project, state(row != null, liveness.isCharacterLive(ref.key)), row?.hidden == true, row?.name)
            }
            RecorderTargetKind.PLACE -> {
                val row = index.place(ref.key)
                ResolvedTarget(ref, project, state(row != null, liveness.isPlaceLive(ref.key)), row?.hidden == true, row?.name)
            }
            RecorderTargetKind.TERM -> {
                val row = index.term(ref.key)
                ResolvedTarget(ref, project, state(row != null, liveness.isTermLive(ref.key)), row?.hidden == true, row?.name)
            }
            RecorderTargetKind.FACTION -> {
                val row = index.faction(ref.key)
                ResolvedTarget(ref, project, state(row != null, liveness.isFactionLive(ref.key)), row?.hidden == true, row?.name)
            }
            RecorderTargetKind.PAGE -> {
                val row = doc.pages[ref.key]
                ResolvedTarget(ref, project, state(row != null, liveness.isPageLive(ref.key)), row?.hidden == true, row?.name)
            }
        }
    }

    /** A live target that may be offered or chosen now: project live and open, item live and not locked hidden. */
    fun isOfferable(env: RecorderEnv, ref: TargetRef): Boolean {
        val resolved = resolve(env, ref)
        val project = resolved.project ?: return false
        if (resolved.state != TargetState.LIVE) return false
        if (env.isLockedHidden(project)) return false
        if (!ref.isProject && resolved.hidden && !env.itemsOpen(ref.projectKey)) return false
        return true
    }
}
