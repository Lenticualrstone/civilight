package io.github.lenticualrstone.civilight.feature.map

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/** DECISIONS §3: every value of [MapCopy] passes the design-system copy lint. */
class MapCopyLintTest {
    private fun values(): Map<String, List<String>> = MapCopy::class.java.declaredFields
        .filter { Modifier.isStatic(it.modifiers) && it.name != "INSTANCE" }
        .associate { field ->
            field.isAccessible = true
            field.name to when (val value = field.get(null)) {
                is String -> listOf(value)
                is List<*> -> value.filterIsInstance<String>()
                else -> emptyList()
            }
        }

    private fun formatted(): List<String> = listOf(
        MapCopy.listSmall("잿빛 새벽의 등불"),
        MapCopy.viewSmall("색면"),
        MapCopy.viewSmall(null),
        MapCopy.editTitle("하층 구역 지도"),
        MapCopy.zoomLabel(120),
        MapCopy.parentTag("세계 지도", "상층"),
        MapCopy.revealFrom("1-7"),
        MapCopy.factionLink("상층 경비대"),
        MapCopy.routeDraftBar(3, "12"),
        MapCopy.routeDraftBar(1, null),
        MapCopy.step(MapCopy.PIN, "결사의 문"),
        MapCopy.defaultRegionName(3),
        MapCopy.movingPin("결사의 문"),
    )

    @Test
    fun `every copy value passes the lint`() {
        val found = values()
        assertTrue("the copy object exposes its constants", found.size > 40)
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
        values().filterKeys { it.startsWith("TODO_COPY_") }.forEach { (name, texts) -> assertEquals(listOf(name), texts) }
    }

    @Test
    fun `formatted copy reads as the specs and boards`() {
        assertEquals("잿빛 새벽의 등불 · 세계", MapCopy.listSmall("잿빛 새벽의 등불"))
        assertEquals("세계 · 지도 · 색면", MapCopy.viewSmall("색면"))
        assertEquals("하층 구역 지도 · 편집", MapCopy.editTitle("하층 구역 지도"))
        assertEquals("120%", MapCopy.zoomLabel(120))
        assertEquals("세계 지도 · 핀 ‘상층’", MapCopy.parentTag("세계 지도", "상층"))
        assertEquals("1-7부터", MapCopy.revealFrom("1-7"))
        assertEquals("세력 · 상층 경비대", MapCopy.factionLink("상층 경비대"))
        assertEquals("지점 3 · 12 km", MapCopy.routeDraftBar(3, "12"))
        assertEquals(listOf("세력 영역", "지역", "기타"), MapCopy.REGION_KINDS)
        assertEquals("종류 · 땅 비율 · 그림 → 후보 3", MapCopy.NEW_GENERATED_SUB)
    }
}
