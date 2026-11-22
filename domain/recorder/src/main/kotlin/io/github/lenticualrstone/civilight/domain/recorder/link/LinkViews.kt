package io.github.lenticualrstone.civilight.domain.recorder.link

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun

/** How a link renders (§3.1 `LinkDisplay`). */
enum class LinkDisplay { NORMAL, MASK_RATING, HIDDEN_PROJECT, HIDDEN_ITEM, TRASHED, DELETED_ITEM_TRASH, GONE, EXAMPLE }

/**
 * One link as shown: [label] is the printed text (before any `연결 · ` prefix or ` @` mark).
 * [plain] = drawn as plain text, not a chip (a purged scene, project, chapter, episode or page).
 */
data class LinkView(
    val link: RecorderLink,
    val display: LinkDisplay,
    val label: String,
    val dotted: Boolean,
    val diamond: Boolean,
    val plain: Boolean,
    val atForm: Boolean,
)

/** Link labels and the §3.3 evaluation order (hiding wins over deletion). */
object LinkViews {
    /** Badge text of a rating (`R-18`), as LIB-28's `recorderLinkLabel` prints it. */
    fun ratingBadge(rating: Rating): String = when (rating) {
        Rating.ALL -> "ALL"
        Rating.R15 -> "R-15"
        Rating.R18 -> "R-18"
    }

    /**
     * The view of [link], or null when it is not shown at all because [record] is hidden and
     * hidden records are locked (step 1). [runs] decide the `@` form.
     */
    fun view(link: RecorderLink, env: RecorderEnv, runs: List<TextRun>, record: RecorderRecord? = null): LinkView? {
        if (record != null && record.hidden && !env.recordsOpen) return null
        val atForm = LinkEdits.isAtForm(link, runs)
        fun v(display: LinkDisplay, label: String, dotted: Boolean = false, diamond: Boolean = false, plain: Boolean = false) =
            LinkView(link, display, label, dotted, diamond, plain, atForm)

        val target = link.target ?: return v(LinkDisplay.GONE, link.lastLabel.orEmpty(), plain = true)
        val project = env.project(link.projectKey)
        // (2) hidden project, locked; also when it is trashed or purged.
        if (project != null && env.isLockedHidden(project)) return v(LinkDisplay.HIDDEN_PROJECT, RecorderCopy.HIDDEN_PROJECT)
        if (project == null && link.lastLabelHidden && target.isProject) return v(LinkDisplay.HIDDEN_PROJECT, RecorderCopy.HIDDEN_PROJECT)
        // (3) 라이프 레코더 연결 가리기: the rating only, also while the project is open.
        if (project != null && project.linkMask) return v(LinkDisplay.MASK_RATING, ratingBadge(project.rating))
        val resolved = Targets.resolve(env, target)
        val purged = link.purgedAt != null || resolved.state == TargetState.PURGED
        // (4) hidden item, items locked; also in the trash; after purge never the name.
        if (!target.isProject && !purged && resolved.hidden && !env.itemsOpen(target.projectKey)) {
            return v(LinkDisplay.HIDDEN_ITEM, RecorderCopy.HIDDEN_ITEM)
        }
        if (purged && link.lastLabelHidden) {
            return v(LinkDisplay.GONE, "${RecorderCopy.PURGED}${RecorderCopy.SEP}${RecorderCopy.HIDDEN_ITEM}")
        }
        // (5) purged.
        if (purged) return goneView(link, target, env, ::v)
        val label = resolved.label.orEmpty()
        // (6) in the trash.
        if (resolved.state == TargetState.TRASHED) {
            return if (target.kind.isCard) v(LinkDisplay.DELETED_ITEM_TRASH, label, dotted = true, diamond = true)
            else v(LinkDisplay.TRASHED, "$label${RecorderCopy.SEP}${RecorderCopy.IN_TRASH}", dotted = true)
        }
        // (7) the onboarding example.
        if (project?.isExample == true) return v(LinkDisplay.EXAMPLE, RecorderCopy.EXAMPLE)
        return v(LinkDisplay.NORMAL, label)
    }

    private fun goneView(
        link: RecorderLink,
        target: TargetRef,
        env: RecorderEnv,
        v: (LinkDisplay, String, Boolean, Boolean, Boolean) -> LinkView,
    ): LinkView {
        val last = link.lastLabel.orEmpty()
        return when (target.kind) {
            RecorderTargetKind.CHARACTER, RecorderTargetKind.PLACE, RecorderTargetKind.TERM, RecorderTargetKind.FACTION ->
                v(LinkDisplay.GONE, "${RecorderCopy.PURGED}${RecorderCopy.SEP}$last", false, false, false)
            RecorderTargetKind.UNIT -> {
                val sceneWord = env.project(link.projectKey)?.doc?.project?.structure?.levels?.lastOrNull() ?: "씬"
                val scene = sceneLabelIn(last, sceneWord)
                if (scene != null) v(LinkDisplay.GONE, "${RecorderCopy.DELETED_SCENE}${RecorderCopy.SEP}$scene", false, false, true)
                else v(LinkDisplay.GONE, last, false, false, true)
            }
            RecorderTargetKind.PROJECT, RecorderTargetKind.PAGE -> v(LinkDisplay.GONE, last, false, false, true)
        }
    }

    /**
     * The scene label at the start of a stored last label (`1-7 씬 3` of `1-7 씬 3 · 꺼진 등불`, which
     * is what `domain:trash` writes for a purged unit), or null when the label is not a scene's.
     */
    fun sceneLabelIn(lastLabel: String, sceneWord: String): String? =
        Regex("""^(?:\S+ )?${Regex.escape(sceneWord)} \d+""").find(lastLabel)?.value

    /** The last label to store for [target] when it is purged (the label shown today). */
    fun lastLabelFor(env: RecorderEnv, target: TargetRef): String? = Targets.resolve(env, target).label

    /** Projects whose links collapse into one masked chip. */
    internal fun isMasked(view: LinkView): Boolean =
        view.display == LinkDisplay.MASK_RATING || view.display == LinkDisplay.HIDDEN_PROJECT

    internal fun projectKeyOf(view: LinkView): Key = view.link.projectKey
}
