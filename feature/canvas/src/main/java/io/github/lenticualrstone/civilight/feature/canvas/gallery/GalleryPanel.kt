package io.github.lenticualrstone.civilight.feature.canvas.gallery

import io.github.lenticualrstone.civilight.domain.library.hiding.HiddenItems
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.TimeLabels
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.gallery.GalleryItem
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.ordered

/** A piece of the description: [link] pieces are drawn in `acc` (an `@` link whose target the viewer may see). */
data class CaptionSpan(val text: String, val link: Boolean)

/** The scene line of the panel (GAL-08 item 2). Only [LIVE] is tappable (opens the script at the scene head). */
data class SceneLine(val text: String, val state: EntryLabels.SceneState, val sceneKey: Key?) {
    val tappable: Boolean get() = state == EntryLabels.SceneState.LIVE
    val dotted: Boolean get() = state == EntryLabels.SceneState.DELETED
}

/**
 * The 설명 판 of the picked item (GAL-08), also the inner right pane (GAL-23) and the viewer text (GAL-09).
 * [clampLines] is per view: `카드` 3, `격자` 2, `목록` and full surfaces none (null).
 */
data class PanelModel(
    val key: Key,
    val title: String,
    val untitled: Boolean,
    /** `3/8` among the visible items in the current order and filter. */
    val position: String,
    val scene: SceneLine?,
    val tags: List<String>,
    /** The caption as spans; null = `설명 없음`. */
    val caption: List<CaptionSpan>?,
    /** Neither title nor description: the first button is the primary `설명 쓰기`. */
    val writeFirst: Boolean,
    val hiddenTag: Boolean,
) {
    /** No link and no tags: `연결 없음`. */
    val noLink: Boolean get() = scene == null && tags.isEmpty()

    val captionText: String? get() = caption?.joinToString("") { it.text }

    /** `보기` · `고치기` · `⋯`, or `설명 쓰기` · `보기` · `⋯` (GAL-08 item 4). */
    val buttons: List<PanelButton>
        get() = if (writeFirst) listOf(PanelButton.WRITE, PanelButton.VIEW, PanelButton.MENU) else listOf(PanelButton.VIEW, PanelButton.EDIT, PanelButton.MENU)

    companion object {
        /** Lines before `더 보기` (GAL-08 item 3; inner `카드` keeps 3, GAL-23). */
        fun clampLines(view: GalleryViewKind, fullSurface: Boolean = false): Int? = when {
            fullSurface -> null
            view == GalleryViewKind.CARD -> 3
            view == GalleryViewKind.GRID -> 2
            else -> null
        }
    }
}

enum class PanelButton(val label: String) {
    VIEW(GalleryUiCopy.BTN_VIEW),
    EDIT(GalleryUiCopy.BTN_EDIT),
    WRITE(GalleryUiCopy.BTN_WRITE),
    MENU(GalleryUiCopy.MENU),
}

/** Builds [PanelModel] (pure; JVM-tested with fixture G0). */
class GalleryPanels(private val doc: ProjectDoc, private val locked: Boolean) {
    private val labels = EntryLabels(doc, locked)
    private val times by lazy { TimeLabels(SceneTimes(doc)) }
    private val hidden by lazy { HiddenItems(doc) }
    private val tagNames by lazy { doc.project.tags.associate { it.key to it.name } }

    fun panel(view: GalleryView, key: Key?): PanelModel? {
        val entry = view.entry(key) ?: return null
        return panel(entry.item, entry.position, view.count, entry.hiddenTag)
    }

    fun panel(item: GalleryItem, position: Int, count: Int, hiddenTag: Boolean = false): PanelModel {
        val title = item.title.takeIf { it.isNotBlank() }
        val caption = item.caption?.let(::spans)?.takeIf { spans -> spans.any { it.text.isNotBlank() } }
        return PanelModel(
            key = item.key,
            title = title ?: GalleryUiCopy.UNTITLED,
            untitled = title == null,
            position = GalleryUiCopy.position(position, count),
            scene = sceneLine(item.sceneKey),
            tags = item.tagKeys.mapNotNull { tagNames[it] },
            caption = caption,
            writeFirst = title == null && caption == null,
            hiddenTag = hiddenTag,
        )
    }

    /** `1-7 씬 3 · 2일째 새벽` (live), `숨긴 씬` (hidden while locked), `지운 씬 · 1-6 씬 1` (trashed or purged). */
    fun sceneLine(sceneKey: Key?): SceneLine? = when (val state = labels.sceneState(sceneKey)) {
        EntryLabels.SceneState.NONE -> null
        EntryLabels.SceneState.HIDDEN -> SceneLine(GalleryUiCopy.HIDDEN_SCENE, state, null)
        EntryLabels.SceneState.DELETED -> SceneLine(GalleryUiCopy.deletedScene(sceneKey?.let(labels::sceneCode)), state, null)
        EntryLabels.SceneState.LIVE -> {
            val key = sceneKey!!
            val code = labels.sceneCode(key).orEmpty()
            val time = times.short(key)?.takeIf { it.isNotBlank() }
            SceneLine(listOfNotNull(code.ifEmpty { null }, time).joinToString(GalleryUiCopy.SEP), state, key)
        }
    }

    /** Paragraphs joined by a newline; link runs keep their typed text and are drawn as links only when visible. */
    fun spans(caption: RichDoc): List<CaptionSpan> {
        val out = ArrayList<CaptionSpan>()
        caption.ordered().forEachIndexed { i, block ->
            if (i > 0) out += CaptionSpan("\n", false)
            block.runs.forEach { run ->
                when (run) {
                    is TextRun.Link -> out += CaptionSpan(run.text, linkShown(run))
                    else -> run.shownText.takeIf { it.isNotEmpty() }?.let { out += CaptionSpan(it, false) }
                }
            }
        }
        return out
    }

    private fun linkShown(run: TextRun.Link): Boolean {
        val key = run.target.key
        val kind = run.target.kind.known ?: return false
        val live = when (kind) {
            LinkTargetKind.CHARACTER -> doc.liveness.isCharacterLive(key)
            LinkTargetKind.UNIT -> doc.liveness.isUnitLive(key)
            LinkTargetKind.TERM -> doc.liveness.isTermLive(key)
            LinkTargetKind.PLACE -> doc.liveness.isPlaceLive(key)
            LinkTargetKind.FACTION -> doc.liveness.isFactionLive(key)
            LinkTargetKind.PAGE -> doc.liveness.isPageLive(key)
            LinkTargetKind.PROJECT -> true
        }
        if (!live) return false
        if (!locked) return true
        return when (kind) {
            LinkTargetKind.CHARACTER -> key !in hidden.characters
            LinkTargetKind.TERM -> key !in hidden.terms
            LinkTargetKind.PLACE -> key !in hidden.places
            LinkTargetKind.FACTION -> key !in hidden.factions
            LinkTargetKind.PAGE -> key !in hidden.pages
            LinkTargetKind.UNIT -> key !in hidden.scenes
            LinkTargetKind.PROJECT -> true
        }
    }
}
