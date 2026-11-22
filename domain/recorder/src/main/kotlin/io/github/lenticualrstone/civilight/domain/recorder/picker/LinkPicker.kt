package io.github.lenticualrstone.civilight.domain.recorder.picker

import io.github.lenticualrstone.civilight.domain.recorder.ProjectSnapshot
import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.link.LinkViews
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.link.Targets
import io.github.lenticualrstone.civilight.domain.recorder.link.UnitLabels
import io.github.lenticualrstone.civilight.domain.recorder.link.target
import io.github.lenticualrstone.civilight.domain.recorder.text.Hangul
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind

/** Groups of the picker, in order (LRC-17, LRC-20). Only the first has a label. */
enum class CandidateGroup(val label: String?) {
    IN_TEXT(RecorderCopy.IN_TEXT_GROUP),
    LINKED_PROJECTS(null),
    RECENT_PROJECT(null),
    OTHERS(null),
}

/** Who asks: the `+ 연결` / chip sheet (has the `글에 나온 이름` group) or an `@` in a text memo. */
enum class PickerMode { PICKER, AT }

/**
 * One choosable target. [label] is what the row prints; [runText] is the text an `@` run gets (the
 * name, or the alias that matched); [projectLabel] is the project's (masked) label; [linked] =
 * already a link of this record (drawn checked, [PROPOSED] LRC-T07).
 */
data class LinkCandidate(
    val target: TargetRef,
    val label: String,
    val runText: String,
    val projectLabel: String,
    val group: CandidateGroup,
    val linked: Boolean,
)

/**
 * The shared link picker's content (LRC-20) and the `@` search (LRC-17). Offered: every link unit
 * (프로젝트 · 장 · 에피소드 · 씬 · 인물 · 장소 · 용어 · 세력 · 페이지, `ED` Q20) of live projects that
 * are not locked hidden, live and not locked hidden themselves. Never offered: trashed targets,
 * locked hidden projects and their contents, locked hidden items.
 */
object LinkPicker {
    private data class Entry(val target: TargetRef, val name: String, val aliases: List<String>, val listIndex: Int)

    private val KIND_ORDER = listOf(
        RecorderTargetKind.PROJECT, RecorderTargetKind.UNIT, RecorderTargetKind.CHARACTER, RecorderTargetKind.PLACE,
        RecorderTargetKind.TERM, RecorderTargetKind.FACTION, RecorderTargetKind.PAGE,
    )

    /**
     * Candidates for [query] (blank = everything, grouped). [recordText] feeds `글에 나온 이름`
     * ([PickerMode.PICKER] only): characters and terms whose name appears verbatim in the text,
     * in order of first occurrence. Then items of projects the record is already linked to, then
     * the most recently opened project ([RecorderEnv.recentProjectKey]), then the rest. Inside a
     * group: exact name, name prefix, name contains, alias, 초성; then kind and list order.
     */
    fun search(
        env: RecorderEnv,
        query: String,
        links: List<RecorderLink>,
        mode: PickerMode,
        recordText: String? = null,
    ): List<LinkCandidate> {
        val q = Hangul.fold(query)
        val linkedTargets = links.mapNotNull { it.target }.toSet()
        val linkedProjects = links.map { it.projectKey }.toSet()
        val entries = env.projects.filter { offerable(env, it) }.flatMap { entries(env, it) }
        val text = recordText?.nfc().orEmpty()

        fun groupOf(e: Entry): CandidateGroup = when {
            e.target.projectKey in linkedProjects -> CandidateGroup.LINKED_PROJECTS
            e.target.projectKey == env.recentProjectKey -> CandidateGroup.RECENT_PROJECT
            else -> CandidateGroup.OTHERS
        }

        // Same name in several projects: linked projects first, then the recent one, then 서재 order.
        val inText = if (mode == PickerMode.PICKER && text.isNotEmpty()) {
            entries.filter { (it.target.kind == RecorderTargetKind.CHARACTER || it.target.kind == RecorderTargetKind.TERM) && text.contains(it.name) }
                .filter { q.isEmpty() || match(it, q) != null }
                .sortedWith(
                    compareBy<Entry> { text.indexOf(it.name) }
                        .thenBy { groupOf(it).ordinal }
                        .thenBy { projectIndex(env, it.target.projectKey) }
                        .thenBy { KIND_ORDER.indexOf(it.target.kind) }
                        .thenBy { it.listIndex },
                )
        } else {
            emptyList()
        }
        val inTextTargets = inText.map { it.target }.toSet()

        val rest = entries.filter { it.target !in inTextTargets }
            .mapNotNull { e -> match(e, q)?.let { e to it } }
            .sortedWith(
                compareBy<Pair<Entry, Match>> { groupOf(it.first).ordinal }
                    .thenBy { it.second.rank }
                    .thenBy { projectIndex(env, it.first.target.projectKey) }
                    .thenBy { KIND_ORDER.indexOf(it.first.target.kind) }
                    .thenBy { it.first.listIndex },
            )

        return inText.map { candidate(env, it, it.name, CandidateGroup.IN_TEXT, it.target in linkedTargets) } +
            rest.map { (e, m) -> candidate(env, e, m.runText, groupOf(e), e.target in linkedTargets) }
    }

    private fun projectIndex(env: RecorderEnv, key: String): Int = env.projects.indexOfFirst { it.key == key }

    private fun offerable(env: RecorderEnv, project: ProjectSnapshot): Boolean = !project.trashed && !env.isLockedHidden(project)

    private fun candidate(env: RecorderEnv, e: Entry, runText: String, group: CandidateGroup, linked: Boolean): LinkCandidate {
        val project = env.project(e.target.projectKey)
        val projectLabel = when {
            project == null -> ""
            project.linkMask -> LinkViews.ratingBadge(project.rating)
            else -> project.name
        }
        return LinkCandidate(e.target, e.name, runText, projectLabel, group, linked)
    }

    private data class Match(val rank: Int, val runText: String)

    private fun match(e: Entry, q: String): Match? {
        if (q.isEmpty()) return Match(0, e.name)
        val name = Hangul.fold(e.name)
        when {
            name == q -> return Match(0, e.name)
            name.startsWith(q) -> return Match(1, e.name)
            name.contains(q) -> return Match(2, e.name)
        }
        e.aliases.firstOrNull { Hangul.fold(it).contains(q) }?.let { return Match(3, it) }
        if (Hangul.isInitialsQuery(q)) {
            val compact = q.replace(" ", "")
            if (Hangul.initials(e.name).replace(" ", "").contains(compact)) return Match(4, e.name)
        }
        return null
    }

    /** Every offerable target of one project with its searchable names, in list order. */
    private fun entries(env: RecorderEnv, project: ProjectSnapshot): List<Entry> {
        val doc = project.doc
        val key = project.key
        val out = mutableListOf<Entry>()
        fun add(kind: RecorderTargetKind, itemKey: String, name: String, aliases: List<String> = emptyList()) {
            val ref = TargetRef(key, kind, itemKey)
            if (kind != RecorderTargetKind.PROJECT && !Targets.isOfferable(env, ref)) return
            out += Entry(ref, name, aliases, out.size)
        }
        add(RecorderTargetKind.PROJECT, key, project.name)
        val labels = UnitLabels(doc)
        fun visit(parentKey: String?) {
            for (unit in doc.index.children(parentKey)) {
                if (!doc.liveness.isLive(unit)) continue
                val label = labels.label(unit)
                add(RecorderTargetKind.UNIT, unit.key, label)
                visit(unit.key)
            }
        }
        visit(null)
        doc.cast.characters.sortedWith(compareBy({ it.order }, { it.key })).forEach { add(RecorderTargetKind.CHARACTER, it.key, it.name) }
        doc.world.places.sortedWith(compareBy({ it.order }, { it.key })).forEach { add(RecorderTargetKind.PLACE, it.key, it.name) }
        doc.world.terms.sortedWith(compareBy({ it.order }, { it.key })).forEach { add(RecorderTargetKind.TERM, it.key, it.name, it.aliases) }
        doc.world.factions.sortedWith(compareBy({ it.order }, { it.key })).forEach { add(RecorderTargetKind.FACTION, it.key, it.name) }
        doc.pages.values.sortedWith(compareBy({ it.order }, { it.key })).forEach { add(RecorderTargetKind.PAGE, it.key, it.name) }
        return out
    }
}
