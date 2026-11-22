package io.github.lenticualrstone.civilight.feature.library.rating

import io.github.lenticualrstone.civilight.domain.library.rating.Protection
import io.github.lenticualrstone.civilight.domain.library.rating.RatedProject
import io.github.lenticualrstone.civilight.domain.library.rating.RatingDefaultsChanges
import io.github.lenticualrstone.civilight.domain.library.rating.RatingDefaultsTable
import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.domain.library.rating.isOn
import io.github.lenticualrstone.civilight.domain.library.rating.with
import io.github.lenticualrstone.civilight.feature.library.protection.ProtectionCopy
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.project.Rating

/** One default switch of 설정 · 수위 등급. */
data class DefaultSwitchUi(val rating: Rating, val protection: Protection, val title: String, val sub: String?, val on: Boolean)

/** A rating group: badge + gloss, its switches (none for ALL: `라벨만`), and the `같이 바꾸기` tag. */
data class RatingGroupUi(val rating: Rating, val gloss: String, val switches: List<DefaultSwitchUi>, val tag: String?)

data class RatingSettingsUi(val groups: List<RatingGroupUi>, val exportCoverRating: Boolean)

/** One project of the `같이 바꾸기` list: what would change. */
data class ApplyRowUi(val project: RatedProject, val changes: String, val after: Protections)

/**
 * 설정 · 수위 등급 (LIB-23, board `RatingSettings`), pure. A default change affects only new projects
 * and later rating changes; existing projects of that rating whose editable protections differ from
 * the group's defaults are offered in `이미 있는 <rating> 프로젝트 <N> · 같이 바꾸기` (R19-10).
 */
object RatingSettingsState {
    private val GROUPS = listOf(Rating.R18, Rating.R15, Rating.ALL)

    fun build(defaults: RatingDefaults, projects: List<RatedProject>, hiddenGroupOpen: Boolean): RatingSettingsUi =
        RatingSettingsUi(
            groups = GROUPS.map { rating ->
                RatingGroupUi(
                    rating = rating,
                    gloss = RatingLabels.gloss(rating),
                    switches = switches(rating, defaults),
                    tag = RatingDefaultsChanges.tag(rating, defaults, differing(rating, defaults, projects, hiddenGroupOpen)),
                )
            },
            exportCoverRating = defaults.exportCoverRating,
        )

    private fun switches(rating: Rating, defaults: RatingDefaults): List<DefaultSwitchUi> {
        val values = RatingDefaultsTable.defaultsFor(rating, defaults)
        return Protection.entries.filter { RatingDefaultsTable.isEditable(rating, it) }.map { p ->
            val (title, sub) = when (p) {
                Protection.HIDDEN -> RatingCopy.R18_HIDE to RatingCopy.R18_HIDE_SUB
                Protection.CAPTURE_BLOCK -> RatingCopy.CAPTURE to RatingCopy.R18_CAPTURE_SUB.takeIf { rating == Rating.R18 }
                Protection.NAME_MASK -> RatingCopy.NAME_MASK to RatingCopy.R18_NAME_MASK_SUB
                Protection.RECORDER_LINK_MASK -> RatingCopy.RECORDER_MASK to RatingCopy.R18_RECORDER_MASK_SUB
            }
            DefaultSwitchUi(rating, p, title, sub, values.isOn(p))
        }
    }

    /**
     * The changed values of [rating]'s group (LIB-23 `같이 바꾸기` sets "the changed value"): the
     * editable protections whose default now differs from the factory default. The settings keep
     * only which groups changed, so this is derived (`ED`); a value turned off and on again is not
     * a change.
     */
    fun changedProtections(rating: Rating, defaults: RatingDefaults): List<Protection> {
        val now = RatingDefaultsTable.defaultsFor(rating, defaults)
        val factory = RatingDefaultsTable.defaultsFor(rating, RatingDefaults())
        return Protection.entries.filter { RatingDefaultsTable.isEditable(rating, it) && now.isOn(it) != factory.isOn(it) }
    }

    /** [before] with the changed values of [rating]'s group set to the group's default; others kept. */
    fun withDefaults(rating: Rating, defaults: RatingDefaults, before: Protections): Protections {
        val values = RatingDefaultsTable.defaultsFor(rating, defaults)
        return changedProtections(rating, defaults).fold(before) { acc, p -> acc.with(p, values.isOn(p)) }
    }

    /**
     * Projects of [rating] (hidden ones only while the group is open, `ED`) whose value of a changed
     * protection differs from the group's current default; empty while the group is unchanged.
     */
    fun differing(rating: Rating, defaults: RatingDefaults, projects: List<RatedProject>, hiddenGroupOpen: Boolean): List<RatedProject> {
        if (!defaults.changed(rating)) return emptyList()
        return RatingDefaultsChanges.existing(rating, projects, hiddenGroupOpen)
            .filter { withDefaults(rating, defaults, it.protections) != it.protections }
    }

    /** `이미 있는 R-18 프로젝트 2`: the list's title, the tag's own words without `같이 바꾸기`. */
    fun applyTitle(rating: Rating, count: Int): String =
        "${RatingCopy.EXISTING_PREFIX} ${RatingLabels.badge(rating)} ${RatingCopy.PROJECT} $count"

    /** Rows of the `같이 바꾸기` list: each project with the protections that would change. */
    fun applyRows(rating: Rating, defaults: RatingDefaults, projects: List<RatedProject>, hiddenGroupOpen: Boolean): List<ApplyRowUi> =
        differing(rating, defaults, projects, hiddenGroupOpen).map { project ->
            val after = withDefaults(rating, defaults, project.protections)
            val changed = Protection.entries.filter { project.protections.isOn(it) != after.isOn(it) }
            ApplyRowUi(project, changed.joinToString(" · ", transform = ::protectionName), after)
        }

    fun protectionName(p: Protection): String = when (p) {
        Protection.HIDDEN -> ProtectionCopy.HIDE
        Protection.CAPTURE_BLOCK -> ProtectionCopy.CAPTURE_BLOCK
        Protection.NAME_MASK -> ProtectionCopy.NAME_MASK
        Protection.RECORDER_LINK_MASK -> ProtectionCopy.RECORDER_LINK_MASK
    }
}
