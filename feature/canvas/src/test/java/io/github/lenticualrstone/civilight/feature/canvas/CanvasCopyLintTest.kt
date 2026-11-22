package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.feature.canvas.board.CanvasTool
import io.github.lenticualrstone.civilight.feature.canvas.card.CardCopy
import io.github.lenticualrstone.civilight.feature.canvas.holo.HoloCopy
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageCopy
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageSlots
import io.github.lenticualrstone.civilight.feature.canvas.page.PageCopy
import io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/** DECISIONS §3: every value of the canvas copy object passes the design-system copy lint. */
class CanvasCopyLintTest {
    /** Every copy object of this module. */
    private val objects = listOf(CanvasCopy::class.java, PageCopy::class.java, CardCopy::class.java, ImageCopy::class.java, HoloCopy::class.java)

    private fun values(): Map<String, List<String>> = objects.flatMap { type ->
        type.declaredFields
            .filter { Modifier.isStatic(it.modifiers) && it.name != "INSTANCE" }
            .map { field ->
                field.isAccessible = true
                val value = field.get(null)
                field.name to when (value) {
                    is String -> listOf(value)
                    is List<*> -> value.filterIsInstance<String>()
                    else -> emptyList()
                }
            }
    }.groupBy({ it.first }, { it.second }).mapValues { (_, lists) -> lists.flatten() }

    /** Formatted copy, with sample arguments from the example project. */
    private fun formatted(): List<String> = buildList {
        add(CanvasCopy.zoomPercent(0.82f))
        CanvasNodeKind.entries.forEach { add(CanvasCopy.kindWord(it)) }
        CanvasCopy.NODE_TOOL_KINDS.forEach { add(CanvasCopy.pickTitle(it)) }
        add(CanvasCopy.deleted(CanvasCopy.KIND_CHARACTER, "노아"))
        add(CanvasCopy.mindOf("리엔"))
        add(CanvasCopy.pair("리엔", "하르트"))
        CanvasTool.entries.forEach {
            add(it.label(cover = true))
            add(it.label(cover = false))
        }
        add(PageCopy.sectionLine("세계관 메모"))
        add(PageCopy.meta("11월 20일", 4))
        add(PageCopy.hiddenPages(1))
        add(PageCopy.listSection(2))
        add(PageCopy.rowSub("설정 노트", "7월 2일"))
        add(CardCopy.hiddenProjects(2))
        CardCopy.statusChip("쓰는 중", true)?.let(::add)
        add(ImageCopy.tab(ImageCopy.TAB_PRESETS, 8))
        add(ImageCopy.tab(ImageCopy.TAB_MINE, 3))
        add(ImageCopy.pickSubtitle("유리 정원", ImageSlots.spec(io.github.lenticualrstone.civilight.model.asset.ImageSlot.PROJECT_COVER).chips))
        add(ImageCopy.cropHeader("유리 정원", ImageCopy.SHORT_PROJECT_COVER))
        add(ImageCopy.readout(2160, 2880, 3024, 4032))
        add(ImageCopy.zoomValue(1.4f))
        HoloCopy.SAMPLE_STATS.forEach { add(it.first); add(it.second) }
        ImageSlots.all.forEach { add(it.name); add(it.usedIn); add(it.holoNote); add(it.pickTitle); addAll(it.chips) }
    }

    @Test
    fun `every copy value passes the lint`() {
        val found = values()
        assertTrue("the copy object exposes its constants", found.size > 20)
        val failures = found.flatMap { (name, texts) ->
            texts.filterNot { it.startsWith("TODO_COPY_") }.mapNotNull { text ->
                val issues = CopyLint.lint(text, isButton = name.startsWith("BTN_"))
                if (issues.isEmpty()) null else "$name = $text: $issues"
            }
        } + formatted().mapNotNull { text -> CopyLint.lint(text).takeIf { it.isNotEmpty() }?.let { "$text: $it" } }
        assertEquals(emptyList<String>(), failures)
    }

    @Test
    fun `missing copy is marked as such`() {
        val todo = values().filterKeys { it.startsWith("TODO_COPY_") }
        assertTrue(todo.isNotEmpty())
        todo.forEach { (name, texts) -> assertTrue("$name holds its own name", texts.all { it == name }) }
    }

    @Test
    fun `formatted copy reads as the boards`() {
        assertEquals("82%", CanvasCopy.zoomPercent(0.82f))
        assertEquals("100%", CanvasCopy.zoomPercent(1f))
        assertEquals("리엔의 마음", CanvasCopy.mindOf("리엔"))
        assertEquals("리엔 · 하르트", CanvasCopy.pair("리엔", "하르트"))
        assertEquals("지운 인물 · 노아", CanvasCopy.deleted("인물", "노아"))
        assertEquals("세력 고르기", CanvasCopy.pickTitle(CanvasCopy.KIND_FACTION))
        assertEquals(listOf("노드", "인물", "메모", "선", "묶음"), CanvasTool.entries.map { it.label(cover = true) })
        assertEquals(listOf("노드", "인물 카드", "메모", "선", "묶음"), CanvasTool.entries.map { it.label(cover = false) })
        assertEquals("두 손가락 이동 · 확대 · 노드는 끌어서 이동", CanvasCopy.HINT)
        assertEquals("기본 이미지 8", ImageCopy.tab(ImageCopy.TAB_PRESETS, 8))
        assertEquals(
            "유리 정원 · 카드 3:4 · 목록 1:1",
            ImageCopy.pickSubtitle("유리 정원", ImageSlots.spec(io.github.lenticualrstone.civilight.model.asset.ImageSlot.PROJECT_COVER).chips),
        )
        assertEquals("유리 정원 · 표지", ImageCopy.cropHeader("유리 정원", ImageCopy.SHORT_PROJECT_COVER))
    }
}
