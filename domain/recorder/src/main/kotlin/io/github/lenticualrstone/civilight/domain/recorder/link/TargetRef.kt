package io.github.lenticualrstone.civilight.domain.recorder.link

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun

/**
 * The identity of a link target: item keys are unique only inside a project (§1.1), so a target
 * is always `(projectKey, kind, key)`. For a project link [key] equals [projectKey].
 */
data class TargetRef(val projectKey: Key, val kind: RecorderTargetKind, val key: Key) {
    val isProject: Boolean get() = kind == RecorderTargetKind.PROJECT

    /** The `@` run target for this item (recorder text keeps the project key, §1.4). */
    fun toLinkTarget(): LinkTarget = LinkTarget(kind.toLinkTargetKind(), key, projectKey)

    companion object {
        fun project(projectKey: Key) = TargetRef(projectKey, RecorderTargetKind.PROJECT, projectKey)
    }
}

/** The target of this link, or null for a target kind from a newer file. */
val RecorderLink.target: TargetRef?
    get() = targetKind.known?.let { TargetRef(projectKey, it, targetKey) }

/** True when this link points at [target]. */
fun RecorderLink.pointsAt(target: TargetRef): Boolean =
    projectKey == target.projectKey && targetKind.known == target.kind && targetKey == target.key

/** The target of an `@` run in recorder text, or null when it has no project key or an unknown kind. */
val TextRun.Link.recorderTarget: TargetRef?
    get() {
        val projectKey = target.projectKey ?: return null
        val kind = target.kind.known?.toRecorderTargetKind() ?: return null
        return TargetRef(projectKey, kind, target.key)
    }

fun RecorderTargetKind.toLinkTargetKind(): LinkTargetKind = when (this) {
    RecorderTargetKind.PROJECT -> LinkTargetKind.PROJECT
    RecorderTargetKind.UNIT -> LinkTargetKind.UNIT
    RecorderTargetKind.CHARACTER -> LinkTargetKind.CHARACTER
    RecorderTargetKind.PLACE -> LinkTargetKind.PLACE
    RecorderTargetKind.TERM -> LinkTargetKind.TERM
    RecorderTargetKind.FACTION -> LinkTargetKind.FACTION
    RecorderTargetKind.PAGE -> LinkTargetKind.PAGE
}

fun LinkTargetKind.toRecorderTargetKind(): RecorderTargetKind = when (this) {
    LinkTargetKind.PROJECT -> RecorderTargetKind.PROJECT
    LinkTargetKind.UNIT -> RecorderTargetKind.UNIT
    LinkTargetKind.CHARACTER -> RecorderTargetKind.CHARACTER
    LinkTargetKind.PLACE -> RecorderTargetKind.PLACE
    LinkTargetKind.TERM -> RecorderTargetKind.TERM
    LinkTargetKind.FACTION -> RecorderTargetKind.FACTION
    LinkTargetKind.PAGE -> RecorderTargetKind.PAGE
}

/** Card kinds (rule 20 dotted chip, `카드에 넣기`). */
val RecorderTargetKind.isCard: Boolean
    get() = this == RecorderTargetKind.CHARACTER || this == RecorderTargetKind.PLACE ||
        this == RecorderTargetKind.TERM || this == RecorderTargetKind.FACTION

/** `인물 · 장소 · 용어 · 세력` (the word in `지운 인물 · 노아`). */
val RecorderTargetKind.cardWord: String?
    get() = when (this) {
        RecorderTargetKind.CHARACTER -> "인물"
        RecorderTargetKind.PLACE -> "장소"
        RecorderTargetKind.TERM -> "용어"
        RecorderTargetKind.FACTION -> "세력"
        else -> null
    }
