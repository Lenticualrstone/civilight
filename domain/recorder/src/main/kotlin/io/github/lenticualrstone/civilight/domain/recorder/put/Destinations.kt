package io.github.lenticualrstone.civilight.domain.recorder.put

import io.github.lenticualrstone.civilight.domain.recorder.ProjectSnapshot
import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import io.github.lenticualrstone.civilight.model.project.Rating

/** Which 넣기 sheet asks for a destination project. */
enum class PutKind { MANUSCRIPT, CARD, PAGE, MEMO_FIELD, CHARACTER }

/** A destination in a 넣기 sheet's project menu; [needsAuth] = choosing it runs the biometric prompt. */
data class DestinationOption(val projectKey: Key, val label: String, val needsAuth: Boolean)

/** The project step of every 넣기 sheet. */
sealed interface DestinationStep {
    /** The project menu is open; nothing chosen. */
    data object ChoosingProject : DestinationStep

    /** The system biometric prompt runs for [projectKey]; no row is shown yet. */
    data class AwaitingAuth(val projectKey: Key) : DestinationStep

    /** [projectKey] is the destination; the sheet shows its rows. */
    data class Chosen(val projectKey: Key) : DestinationStep
}

/**
 * Protected destinations (LRC-32, rule 23 §4): choosing a locked hidden project or an R-18 project
 * runs the system biometric prompt at that moment; cancel → the choice is not made. Not offered
 * ([PROPOSED]): the read-only example, projects in the trash, and for `원고에 넣기` projects of
 * format `설정만`.
 */
object Destinations {
    fun offered(project: ProjectSnapshot, kind: PutKind): Boolean =
        !project.trashed && !project.readOnly && !project.isExample &&
            !(kind == PutKind.MANUSCRIPT && project.format == ProjectFormat.SETTING_ONLY)

    fun needsAuth(project: ProjectSnapshot, env: RecorderEnv): Boolean =
        env.isLockedHidden(project) || project.rating == Rating.R18

    /** The project's label in the menu and sheet title: `숨긴 프로젝트` while locked, else the name. */
    fun label(project: ProjectSnapshot, env: RecorderEnv): String =
        if (env.isLockedHidden(project)) RecorderCopy.HIDDEN_PROJECT else project.name

    fun options(env: RecorderEnv, kind: PutKind): List<DestinationOption> =
        env.projects.filter { offered(it, kind) }.map { DestinationOption(it.key, label(it, env), needsAuth(it, env)) }

    fun choose(env: RecorderEnv, projectKey: Key, kind: PutKind): DestinationStep {
        val project = env.project(projectKey)?.takeIf { offered(it, kind) } ?: return DestinationStep.ChoosingProject
        return if (needsAuth(project, env)) DestinationStep.AwaitingAuth(projectKey) else DestinationStep.Chosen(projectKey)
    }

    /** The prompt's result: success → chosen; cancel or failure → back to the project choice, nothing changed. */
    fun authResult(step: DestinationStep, success: Boolean): DestinationStep = when (step) {
        is DestinationStep.AwaitingAuth -> if (success) DestinationStep.Chosen(step.projectKey) else DestinationStep.ChoosingProject
        else -> step
    }
}
