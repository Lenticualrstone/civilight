package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.EpisodeFile
import io.github.lenticualrstone.civilight.model.file.StoryIndexFile
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.project.StoryStructure
import io.github.lenticualrstone.civilight.model.project.StructurePreset
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.text.TextRun
import java.time.LocalDate
import java.time.ZoneId

/** Test access to the example project 「잿빛 새벽의 등불」 and small hand-built hierarchies. */
object Fx {
    val zone: ZoneId = ZoneId.of("Asia/Seoul")

    /** 2026-11-22, the fixture's day (20-story-time §0 clock). */
    val today: LocalDate = LocalDate.of(2026, 11, 22)

    fun doc(): ProjectDoc = ExampleProject.build()

    /** Deterministic action context: keys `01JZ000000TEST` + counter, 2026-11-22 09:00 KST. */
    fun ctx(): ActionContext {
        var n = 0
        return ActionContext(now = 1_795_305_600_000L, zone = zone, device = "test", newKey = { "01JZ000000TEST" + (++n).toString().padStart(12, '0') })
    }

    fun ProjectDoc.unit(key: Key): StoryUnit = index.unit(key)!!

    fun ProjectDoc.withUnit(key: Key, change: (StoryUnit) -> StoryUnit): ProjectDoc =
        copy(story = story.copy(units = story.units.map { if (it.key == key) change(it) else it }))

    /** [this] with one narration block appended to [sceneKey] (e.g. LENS-09: content in a 1-9 scene). */
    fun ProjectDoc.withBlock(sceneKey: Key, blockKey: Key, text: String, number: Int = 900): ProjectDoc {
        val unitKey = index.numberingUnitKeyOfScene(sceneKey)!!
        val last = index.blocksOfScene(sceneKey).lastOrNull()?.order
        val block = Block(
            key = blockKey, sceneKey = sceneKey, order = OrderKeys.after(last), number = number,
            kind = BlockKind.NARRATION.open(), runs = listOf(TextRun.Plain(text)),
        )
        val file = episodes[unitKey] ?: EpisodeFile(unitKey = unitKey)
        return copy(episodes = episodes + (unitKey to file.copy(blocks = file.blocks + block)))
    }

    /** A node of a [mini] tree: a unit with children; a scene with [text] gets one narration block. */
    data class Node(val key: Key, val title: String? = null, val children: List<Node> = emptyList(), val text: String? = null)

    fun node(key: Key, title: String? = null, text: String? = null, vararg children: Node) = Node(key, title, children.toList(), text)

    /** A project with [levels] (top → scene) and the units of [roots]. */
    fun mini(levels: List<String>, vararg roots: Node): ProjectDoc {
        val projectKey = "01JZ000000MINIPRJT0000000001"
        val units = ArrayList<StoryUnit>()
        val blocks = ArrayList<Block>()
        fun add(nodes: List<Node>, parent: Key?, level: Int) {
            val orders = OrderKeys.evenlySpaced(nodes.size)
            nodes.forEachIndexed { i, n ->
                units += StoryUnit(key = n.key, parentKey = parent, level = level, order = orders[i], title = n.title)
                if (n.text != null) {
                    blocks += Block(
                        key = n.key + "B",
                        sceneKey = n.key,
                        order = "V",
                        number = blocks.size + 1,
                        kind = BlockKind.NARRATION.open(),
                        runs = listOf(TextRun.Plain(n.text)),
                    )
                }
                add(n.children, n.key, level + 1)
            }
        }
        add(roots.toList(), null, 0)
        val preset = if (levels == StoryStructure.DEFAULT_LEVELS) StructurePreset.CHAPTER_EPISODE_SCENE else StructurePreset.CUSTOM
        val doc = ProjectDoc(
            project = Project(key = projectKey, name = "작은 이야기", structure = StoryStructure(preset = preset.open(), levels = levels)),
            story = StoryIndexFile(units = units),
            episodes = mapOf(projectKey to EpisodeFile(unitKey = projectKey, blocks = blocks)),
        )
        return doc.regroupBlocks()
    }
}
