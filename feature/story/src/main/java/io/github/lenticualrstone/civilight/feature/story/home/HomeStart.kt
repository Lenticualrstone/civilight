package io.github.lenticualrstone.civilight.feature.story.home

import io.github.lenticualrstone.civilight.domain.library.newproject.ChecklistItem
import io.github.lenticualrstone.civilight.domain.library.newproject.StartBlock
import io.github.lenticualrstone.civilight.feature.story.common.TODO_COPY_PREFIX
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.ProjectAppState
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.navigation.CastList
import io.github.lenticualrstone.civilight.navigation.RecorderInbox
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.World
import io.github.lenticualrstone.civilight.navigation.WorldTab

/** One `시작해 볼 것` row: checkbox square, title (struck through when done), sub line, chevron. */
data class HomeStartItem(val id: ChecklistItem.Id, val title: String, val sub: String, val done: Boolean, val route: Route)

/**
 * The 만든 직후 block on home (LIB-13 content, hosted here by STO-01; board `NewReady`):
 * `연재 장편 · 장 › 에피소드 › 씬 · 1200년`, `바로 쓰기` + `1장 · 에피소드 1-1 · 씬 1` + `쓰기`, and
 * `시작해 볼 것 · 1/4` with `목록 닫기`.
 */
data class HomeStart(
    val summary: String,
    /** `1장 · 에피소드 1-1 · 씬 1`; null when the project has no scene (format `설정만`). */
    val quickWrite: String?,
    val quickWriteRoute: Route,
    val header: String,
    val items: List<HomeStartItem>,
)

/**
 * When and what the block shows. The items complete by fact (`domain:library` [StartBlock]); the
 * block stays until `목록 닫기` (`startChecklist.dismissed`, app state, no undo step).
 * `ED`: never on a read-only project (the example, LIB-17), which nobody created.
 */
object HomeStartModel {
    fun build(doc: ProjectDoc, recorder: RecorderLibrary, readOnly: Boolean): HomeStart? {
        if (readOnly || doc.app.activity.startChecklist.dismissed) return null
        val block = StartBlock.of(doc, recorder)
        val p = doc.project.key
        val write = Script(p, sceneKey = firstScene(doc))
        return HomeStart(
            summary = block.summary,
            quickWrite = block.quickWrite,
            quickWriteRoute = write,
            header = block.header,
            items = block.items.map { item ->
                val route = when (item.id) {
                    ChecklistItem.Id.PROTAGONIST -> CastList(p)
                    // `FromProject` preset (core pass): this project's records + `정리 안 됨`.
                    ChecklistItem.Id.LINKED_RECORDS -> RecorderInbox(projectKey = p)
                    // feature:world opens the new-term form on arrival (core pass `createTerm`).
                    ChecklistItem.Id.TERM -> World(p, WorldTab.TERMS, createTerm = true)
                    ChecklistItem.Id.FIRST_LINE -> write
                }
                HomeStartItem(item.id, item.title, shownSub(item.sub), item.done, route)
            },
        )
    }

    /** The first scene down the first live unit of every level (the `바로 쓰기` target). */
    fun firstScene(doc: ProjectDoc): Key? {
        var parent: Key? = null
        var last: Key? = null
        while (true) {
            val next = doc.index.children(parent).filter { doc.liveness.isLive(it) }.minByOrNull { it.order } ?: break
            last = next.key.takeIf { next.level == doc.project.structure.sceneLevel }
            parent = next.key
        }
        return last
    }

    /**
     * A sub line as shown: copy the spec does not draw yet (`TODO_COPY_…` from `domain:library`, e.g.
     * `주인공 만들기` while no character exists) is left empty, never put on screen.
     */
    fun shownSub(sub: String): String = if (sub.startsWith(TODO_COPY_PREFIX)) "" else sub

    /** `목록 닫기`: dismissed for good. */
    fun dismiss(app: ProjectAppState): ProjectAppState =
        app.copy(activity = app.activity.copy(startChecklist = app.activity.startChecklist.copy(dismissed = true)))
}
