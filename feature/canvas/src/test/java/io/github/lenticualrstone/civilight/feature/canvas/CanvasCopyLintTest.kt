package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.designsystem.theme.MotionLint
import io.github.lenticualrstone.civilight.feature.canvas.board.CanvasTool
import io.github.lenticualrstone.civilight.feature.canvas.card.CardCopy
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryUiCopy
import io.github.lenticualrstone.civilight.feature.canvas.holo.HoloCopy
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageCopy
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageSlots
import io.github.lenticualrstone.civilight.feature.canvas.page.PageCopy
import io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.Modifier

/** DECISIONS §3: every value of the canvas copy object passes the design-system copy lint. */
class CanvasCopyLintTest {
    /** Every copy object of this module. */
    private val objects = listOf(CanvasCopy::class.java, PageCopy::class.java, CardCopy::class.java, ImageCopy::class.java, HoloCopy::class.java, GalleryUiCopy::class.java, io.github.lenticualrstone.civilight.feature.canvas.image.GalleryCropChips::class.java)

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
        add(CanvasCopy.ZOOM_OUT)
        add(CanvasCopy.BTN_REMOVE_NODE)
        add(CanvasCopy.BTN_REMOVE_LINE)
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
        // U-08 cover mode (CV-32).
        add(ImageCopy.tab(ImageCopy.TAB_GALLERY, 3))
        add(ImageCopy.coverNow("사진"))
        add(ImageCopy.coverNow("잿빛 새벽"))
        add(ImageCopy.pickSubtitle("유리 정원", ImageSlots.spec(io.github.lenticualrstone.civilight.model.asset.ImageSlot.PROJECT_COVER).chips))
        add(ImageCopy.cropHeader("유리 정원", ImageCopy.SHORT_PROJECT_COVER))
        add(ImageCopy.readout(2160, 2880, 3024, 4032))
        add(ImageCopy.zoomValue(1.4f))
        add(GalleryUiCopy.countLine("리엔", 8))
        add(GalleryUiCopy.chip("삽화", 4))
        add(GalleryUiCopy.addSubtitle("리엔"))
        add(GalleryUiCopy.addButton(3))
        add(GalleryUiCopy.ownerPosition("리엔", 3, 8, asIs = true))
        add(GalleryUiCopy.facts(3024, 4032, "4.2 MB", GalleryUiCopy.PHOTO))
        add(GalleryUiCopy.hiddenShownIn(GalleryUiCopy.SHOWN_IN_PORTRAIT))
        add(GalleryUiCopy.orderLine("3/8", GalleryUiCopy.ONLY_IN_ALL))
        add(GalleryUiCopy.orderSection(128))
        add(GalleryUiCopy.pickedBar("등불 앞의 리엔", "3/8"))
        add(GalleryUiCopy.movingBar("등불 앞의 리엔"))
        add(GalleryUiCopy.pickedCount(3))
        add(GalleryUiCopy.imageCount(8, 2))
        add(GalleryUiCopy.imageCount(8, 0))
        add(GalleryUiCopy.readout(41, 128))
        add(GalleryUiCopy.position(3, 8))
        add(GalleryUiCopy.deletedScene("1-6 씬 1"))
        add(GalleryUiCopy.unlockedBar("숨긴 이미지 2"))
        add(GalleryUiCopy.ownerGallery("리엔"))
        add(GalleryUiCopy.ownerLink("리엔"))
        add(GalleryUiCopy.originalLine(1200, 1600, "4.2 MB"))
        add(GalleryUiCopy.originalLine(1200, 1600, null))
        add(GalleryUiCopy.addedOn("11-20"))
        add(GalleryUiCopy.offerSub(GalleryUiCopy.NOW_PORTRAIT, GalleryUiCopy.PHOTO))
        HoloCopy.SAMPLE_STATS.forEach { add(it.first); add(it.second) }
        ImageSlots.all.forEach { add(it.name); add(it.usedIn); add(it.holoNote); add(it.pickTitle); addAll(it.chips) }
    }

    @Test
    fun `every copy value passes the lint`() {
        val found = values()
        assertTrue("the copy object exposes its constants", found.size > 20)
        val failures = found.flatMap { (name, texts) ->
            texts.mapNotNull { text ->
                val issues = CopyLint.lint(text, isButton = name.startsWith("BTN_"))
                if (issues.isEmpty()) null else "$name = $text: $issues"
            }
        } + formatted().mapNotNull { text -> CopyLint.lint(text).takeIf { it.isNotEmpty() }?.let { "$text: $it" } }
        assertEquals(emptyList<String>(), failures)
    }

    @Test
    fun `no placeholder constant is left in the copy objects`() {
        val todo = values().filterKeys { it.startsWith("TODO_COPY_") }
        assertEquals(emptyList<String>(), todo.keys.toList())
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

    /** Motion audit §3.2: no literal durations, default `animateItem()`, bare `Crossfade` or own curves. */
    @Test
    fun `motion lint`() {
        val issues = MotionLint.lintModule(File("."))
        assertEquals(MotionLint.report(issues), 0, issues.size)
    }

    /** Hard rule: no placeholder copy may remain where it can reach the screen (the shell has the same guard). */
    @Test
    fun `no TODO_COPY string is left in main sources`() {
        val left = File("src/main").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { f -> f.readLines().any { it.contains("\"TODO_COPY") && !it.trimStart().startsWith("*") && !it.trimStart().startsWith("//") } }
            .map { it.name }
            .toList()
        assertEquals(emptyList<String>(), left)
    }
}
