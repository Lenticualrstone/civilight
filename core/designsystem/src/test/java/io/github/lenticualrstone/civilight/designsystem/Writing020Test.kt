package io.github.lenticualrstone.civilight.designsystem

import androidx.compose.ui.graphics.vector.PathParser
import io.github.lenticualrstone.civilight.designsystem.component.CreateButton
import io.github.lenticualrstone.civilight.designsystem.component.CreateCard
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardCopy
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardMode
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardState
import io.github.lenticualrstone.civilight.designsystem.component.CreateFieldType
import io.github.lenticualrstone.civilight.designsystem.component.CreateFields
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.designsystem.component.InputContext
import io.github.lenticualrstone.civilight.designsystem.component.InputKind
import io.github.lenticualrstone.civilight.designsystem.component.InputMark
import io.github.lenticualrstone.civilight.designsystem.component.InputPlace
import io.github.lenticualrstone.civilight.designsystem.component.InputSpeaker
import io.github.lenticualrstone.civilight.designsystem.component.ScriptInput
import io.github.lenticualrstone.civilight.designsystem.component.ScriptInputCopy
import io.github.lenticualrstone.civilight.designsystem.component.ScriptInputEvent as E
import io.github.lenticualrstone.civilight.designsystem.component.ScriptInputRules
import io.github.lenticualrstone.civilight.designsystem.component.ScriptInputState
import io.github.lenticualrstone.civilight.designsystem.component.SendSlot
import io.github.lenticualrstone.civilight.designsystem.component.SwipeReveal
import io.github.lenticualrstone.civilight.designsystem.component.SwipeSide
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonCopy
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonLint
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtons
import io.github.lenticualrstone.civilight.designsystem.icon.IconClass
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.designsystem.theme.MotionLint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** 0.2.0 writing components: DS-70 … DS-72 (T-DS-46 … T-DS-50), the U-14 input bar reducer, SCR-30 swipe. */
class Writing020Test {

    // ---- T-DS-47 · icon set (DS-71)

    /** The r2 set (`icons_set.py`, 2026-11-24), copied here as DS-71 says. */
    private val r2 = linkedMapOf(
        "send" to "M12 19V6M6.5 11.5L12 6l5.5 5.5",
        "check" to "M5 12.5l4.5 4.5L19 7.5",
        "search" to "M10.5 17a6.5 6.5 0 1 0 0-13 6.5 6.5 0 0 0 0 13zM20 20l-4.5-4.5",
        "layers" to "M12 4l9 5-9 5-9-5zM3 14l9 5 9-5",
        "close" to "M6.5 6.5l11 11M17.5 6.5l-11 11",
        "split" to "M12 4v16M4 12h5M6.5 9.5L4 12l2.5 2.5M20 12h-5M17.5 9.5L20 12l-2.5 2.5",
        "merge" to "M12 4v16M3 12h5M5.5 9.5L8 12l-2.5 2.5M21 12h-5M18.5 9.5L16 12l2.5 2.5",
        "next_scene" to "M4 6h12M4 10h12M4 14h7M17 13v7M13.5 16.5h7",
        "edit" to "M4 20h4L19 9l-4-4L4 16zM13.5 6.5l4 4",
        "insert_after" to "M5 6h14M5 11h14M12 14v6M9 17h6",
        "kind" to "M4 7h10M4 12h16M4 17h7M17 4l3 3-3 3",
        "scene_cut" to "M4 5h16M4 19h16M4 12h3M10.5 12h3M17 12h3",
        "trash" to "M5 7h14M10 7V5h4v2M7 7l1 12h8l1-12",
        "note_ref" to "M5 5h14v10h-8l-4 3.5V15H5z",
        "props" to "M4 7h16M4 12h16M4 17h10",
        "copy" to "M8 8h11v11H8zM5 16V5h11",
        "cut" to "M6 7a2.5 2.5 0 1 0 0 .01M6 17a2.5 2.5 0 1 0 0 .01M8 8.5L19 18M8 15.5L19 6",
        "move" to "M12 4v16M8 8l4-4 4 4M8 16l4 4 4-4",
        "undo" to "M9 14L4 9l5-5M4 9h10a6 6 0 0 1 0 12h-3",
        "plus" to "M12 5v14M5 12h14",
        "list" to "M4 7h16M4 12h16M4 17h16",
        "graph" to "M8.5 6.5a2.5 2.5 0 1 1-5 0 2.5 2.5 0 0 1 5 0zM20.5 9a2.5 2.5 0 1 1-5 0 2.5 2.5 0 0 1 5 0z" +
            "M12.5 18a2.5 2.5 0 1 1-5 0 2.5 2.5 0 0 1 5 0zM8.4 7.1l7.2 1.4M6.6 8.9l2.6 6.8M16.4 10.8l-4.6 5.4",
        "back" to "M15 5l-7 7 7 7",
        "prev" to "M15 5l-7 7 7 7",
        "next" to "M9 5l7 7-7 7",
        "up" to "M5 15l7-7 7 7",
        "down" to "M5 9l7 7 7-7",
        "to_top" to "M5 4.5h14M12 20V9M7 14l5-5 5 5",
        "to_bottom" to "M5 19.5h14M12 4v11M7 10l5 5 5-5",
        "select_box" to "M5 5h14v14H5zM8.5 12l2.5 2.5 4.5-5",
        "more" to "M5 12h.01M12 12h.01M19 12h.01",
        "caption" to "M5 6h14M5 11h14M5 16h9",
        "expand" to "M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5",
        "image" to "M4 5h16v14H4zM4 16l5-5 4 4 3-3 4 4",
        "file" to "M6 3h8l4 4v14H6zM14 3v4h4",
        "rotate" to "M4 12a8 8 0 1 0 3-6.2M4 4v4h4",
        "mirror" to "M12 3v18M8 7l-4 5 4 5M16 7l4 5-4 5",
        "reset" to "M4 4v6h6M4.5 15a8 8 0 1 0 2-8.5L4 9",
        "lock" to "M7 11V8a5 5 0 0 1 10 0v3M6 11h12v9H6zM12 15v2",
        "fingerprint" to "M7 18c1-2 1.5-4 1.5-6a3.5 3.5 0 0 1 7 0c0 1.2-.1 2.3-.3 3.4M12 12c0 3-.6 5.6-1.8 8" +
            "M4.5 14c.3-.8.5-1.6.5-2.5a7 7 0 0 1 14 0c0 2-.2 3.9-.7 5.7M14.8 19.5c.3-.8.5-1.6.6-2.5",
        "mic" to "M12 4a3 3 0 0 1 3 3v5a3 3 0 0 1-6 0V7a3 3 0 0 1 3-3zM6 11a6 6 0 0 0 12 0M12 17v3",
        "write" to "M5 6h14M12 6v12M9 18h6",
        "pause" to "M9 6v12M15 6v12",
        "dice" to "M4 4h16v16H4zM9 9h.01M15 15h.01M15 9h.01M9 15h.01",
        "flip" to "M4 12a8 8 0 0 1 14-5.3M20 12a8 8 0 0 1-14 5.3M18 3v4h-4M6 21v-4h4",
        "redeal" to "M4 12a8 8 0 1 0 2.4-5.7M4 4.5v4h4",
        "open_out" to "M14 5h5v5M19 5l-8 8M10 6H6a1 1 0 0 0-1 1v11a1 1 0 0 0 1 1h11a1 1 0 0 0 1-1v-4",
        "fan" to "M12 20L5 7M12 20l7-13M12 20V5",
        "gather" to "M5 9l7-4 7 4-7 4zM5 13l7 4 7-4M5 17l7 4 7-4",
        "lift" to "M4 17h16M4 20.5h16M7 6.5h10v6H7zM9.5 4L12 2l2.5 2",
        "drop" to "M4 17h16M4 20.5h16M7 8h10v5.5H7zM9.5 3L12 5l2.5-2",
    )

    @Test
    fun `T-DS-47 icon set r2`() {
        assertEquals(51, r2.size)
        assertEquals(r2.keys.toList(), IconId.r2Keys)
        r2.forEach { (key, path) ->
            val id = IconId.forKey(key)
            assertNotNull(key, id)
            assertEquals(key, path, id!!.pathData)
        }
        assertEquals(IconId.Flip, IconId.forKey("mirror"))
        assertEquals(IconId.CardFlip, IconId.forKey("flip"))
        assertEquals(IconId.MoveBlock, IconId.forKey("move"))
        assertEquals(IconId.X, IconId.forKey("close"))
        assertEquals(IconId.forKey("back")!!.pathData, IconId.forKey("prev")!!.pathData)
        assertEquals("M5 12.5l4.5 4.5L19 7.5", IconId.Check.pathData)
        assertNull(IconId.forKey("nope"))
        // The map pan tool keeps its own shape.
        assertTrue(IconId.Move.pathData != IconId.MoveBlock.pathData)
        IconId.entries.forEach { assertTrue(it.name, PathParser().parsePathString(it.pathData).toNodes().isNotEmpty()) }
    }

    // ---- T-DS-46 · icon classes (DS-70)

    private val aRows = mapOf(
        "A1" to listOf("보내기"), "A3" to listOf("찾기 · 명령"), "A4" to listOf("연출 · 메모 표시"), "A5" to listOf("찾기 닫기"),
        "A6" to listOf("닫기"), "A7" to listOf("새 프로젝트"), "A8" to listOf("이전", "다음"), "A9" to listOf("고치기"),
        "A10" to listOf("보기", "고치기"), "A11" to listOf("되돌리기"), "A12" to listOf("되돌리기"), "A13" to listOf("서재로"),
        "A14" to listOf("이전 카드", "다음 카드"), "A15" to listOf("닫기"),
    )

    @Test
    fun `T-DS-46 icon classes`() {
        aRows.forEach { (row, names) ->
            val buttons = IconButtons.row(row)
            assertEquals(row, names, buttons.map { IconButtons.nameOf(it) })
            buttons.forEach {
                assertEquals(it.name, IconClass.A, IconButtons.classOf(it))
                assertTrue(it.name, IconButtons.nameOf(it).isNotBlank())
                assertNotNull("${it.name} draws an r2 icon", it.icon)
                assertNull("a draws no text", it.text)
            }
        }
        assertEquals(IconClass.C, IconButtons.classOf(IconButtonSpec.EDIT_SEND))
        assertEquals("고치기", IconButtonSpec.EDIT_SEND.text)
        assertNull(IconButtonSpec.EDIT_SEND.icon)
        // 15 a-rows less A2 (kept as text); every b-row B1 … B33 but B26 (terrain palette, no r2 key).
        assertEquals(14, IconButtonSpec.entries.filter { it.cls == IconClass.A }.map { it.row }.distinct().size)
        assertEquals((1..33).map { "B$it" } - "B26", IconButtonSpec.entries.filter { it.cls == IconClass.B }.map { it.row }.distinct().sortedBy { it.drop(1).toInt() })
        assertEquals("시점 더하기", IconButtons.nameOf(IconButtonSpec.ADD_POINT))
        assertEquals("시점", IconButtonSpec.ADD_POINT.text)
        assertEquals("여러 장 고르기", IconButtons.nameOf(IconButtonSpec.MULTI_SELECT))
        assertEquals("고르기", IconButtonSpec.MULTI_SELECT.text)
        assertEquals("다음 씬 만들기", IconButtons.nameOf(IconButtonSpec.NEXT_SCENE))
        assertEquals("항목 넣기", IconButtons.nameOf(IconButtonSpec.ADD_FIELD))
        assertEquals(
            listOf("고치기", "이 뒤에", "종류", "씬 나누기", "지우기"),
            IconButtons.row("B3").map { IconButtons.nameOf(it) },
        )
        assertEquals(listOf("edit", "insert_after", "kind", "scene_cut", "trash"), IconButtons.row("B3").map { it.iconKey })
        assertEquals("합치기 · 꺼짐 · 종류 다름", IconButtons.offName("합치기", "종류 다름"))
        assertEquals("나누기 · 꺼짐", IconButtons.offName("나누기"))
        IconButtonSpec.entries.filter { it.cls == IconClass.B }.forEach {
            assertTrue(it.name, it.text!!.isNotBlank() && it.accessibilityName.isNotBlank())
            if (it != IconButtonSpec.SWIPE_TIME) assertNotNull(it.name, it.icon)
        }
        assertTrue(IconButtonSpec.CAPTION_TOGGLE.toggle && IconButtonSpec.CARD_FLIP.toggle)
    }

    @Test
    fun `T-DS-48 touch targets`() {
        IconButtonSpec.entries.filter { it.cls == IconClass.A }.forEach {
            assertTrue(IconButtons.TOUCH_DP >= 44)
            assertEquals(20, IconButtons.iconSizeDp(it.cls))
        }
        assertTrue(IconButtons.iconSizeDp(IconClass.B)!! in IconButtons.ICON_WITH_TEXT_DP)
        assertEquals(16..18, IconButtons.ICON_WITH_TEXT_DP)
        assertNull(IconButtons.iconSizeDp(IconClass.C))
    }

    @Test
    fun `icon-only lint flags blank and missing names`() {
        val src = """
            fun f() {
                CivilightIconButton(IconId.Back, "뒤로", onClick = {})
                CivilightIconButton(IconId.More, "", {})
                CivilightIconButton(icon = IconId.X, onClick = {})
                IconOnlyButton(IconId.Send, contentDescription = " ", onClick = {})
                GlassRoundButton(IconId.Back, contentDescription = label, onClick = {})
                CivilightIconButton(IconButtonSpec.SEND, {})
                // CivilightIconButton(IconId.X, "", {}) in a comment is not a call
                val s = "CivilightIconButton(IconId.X, \"\", {})"
            }
            @Composable fun CivilightIconButton(icon: IconId, contentDescription: String, onClick: () -> Unit) {}
        """.trimIndent()
        val issues = IconButtonLint.lint("X.kt", src)
        assertEquals(listOf(3, 4, 5), issues.map { it.line })
    }

    @Test
    fun `every icon-only call in the repository has a name`() {
        // Unit tests run in the module directory: lint the whole repository's sources (features included).
        val root = File("../..")
        check(File(root, "settings.gradle.kts").exists()) { "not the repository root: ${root.absolutePath}" }
        val issues = listOf("app", "core", "domain", "feature").flatMap { IconButtonLint.lintDir(File(root, it)) }
            .filterNot { it.file.contains("/test/") }
        assertEquals("", IconButtonLint.report(issues))
    }

    // ---- T-DS-49 / T-DS-50 · create card (DS-72)

    @Test
    fun `T-DS-49 create card buttons`() {
        assertEquals(listOf("취소", "만들고 열기", "만들기"), CreateCard.buttons(CreateCardMode.LIST).map { it.label })
        assertEquals(listOf(CreateButton.CANCEL, CreateButton.CREATE), CreateCard.buttons(CreateCardMode.PICKER))
        assertFalse(CreateCard.canCreate("  "))
        assertFalse(CreateCard.canCreate(""))
        assertTrue(CreateCard.canCreate("세라"))
    }

    @Test
    fun `T-DS-50 create card gap`() {
        val fit = CreateCard.fit(windowHeight = 844f, statusTop = 32f, systemBottom = 24f, keyboardTop = 520f, cardHeight = 600f, titleHeight = 48f, buttonsHeight = 76f)
        assertTrue(fit.bottom <= 512f)
        val top = 32f + 8f
        assertEquals(top, fit.top, 0.01f)
        assertEquals(512f - top - 48f - 76f, fit.fieldArea, 0.01f)
        // A short card is centred in the band; without the keyboard the band ends at systemBottom + 16.
        val small = CreateCard.fit(844f, 32f, 24f, null, 300f, 48f, 76f)
        assertEquals(40f + (804f - 40f - 300f) / 2f, small.top, 0.01f)
        assertEquals(small.top + 300f, small.bottom, 0.01f)
        assertEquals(176f, small.fieldArea, 0.01f)
    }

    @Test
    fun `U-15 field sets per kind`() {
        fun labels(k: CreateKind) = k.fields.map { it.label }
        assertEquals(listOf("이미지", "이름", "표시 이름", "한 줄 소개", "태그"), labels(CreateKind.CHARACTER))
        assertEquals(listOf("이미지", "이름", "상위 지역", "한 줄 설명"), labels(CreateKind.PLACE))
        assertEquals(listOf("이름", "다른 이름", "한 줄 설명"), labels(CreateKind.TERM))
        assertEquals(listOf("이미지", "이름", "한 줄 설명"), labels(CreateKind.FACTION))
        assertEquals(listOf("이름", "때", "관련 인물"), labels(CreateKind.EVENT))
        assertEquals(listOf("종류", "이름", "파일"), labels(CreateKind.ASSET))
        assertEquals(listOf("이름", "색"), labels(CreateKind.TAG))
        assertEquals(listOf("이름", "양쪽에서 부르는 이름"), labels(CreateKind.RELATION_KIND))
        assertEquals(listOf("이름"), labels(CreateKind.PAGE))
        assertEquals(listOf("이름"), labels(CreateKind.CANVAS))
        // Only the name is required, and every kind has one.
        CreateKind.entries.forEach { k ->
            assertEquals(k.name, listOf(CreateFields.NAME), k.fields.filter { it.required }.map { it.key })
            assertEquals(k.name, k.fields.map { it.key }.distinct().size, k.fields.size)
        }
        assertEquals(CreateFieldType.COLOR, CreateKind.TAG.fields.last().type)
    }

    @Test
    fun `U-15 create rows and picker prefill`() {
        assertEquals("새 인물", CreateCard.rowLabel(CreateKind.CHARACTER))
        assertEquals("새 장소", CreateCard.rowLabel(CreateKind.PLACE, "하층", matched = true))
        assertEquals("‘세라’ 새 인물", CreateCard.rowLabel(CreateKind.CHARACTER, " 세라 ", matched = false))
        assertEquals("새 관계 종류", CreateCard.title(CreateKind.RELATION_KIND))
        val picker = CreateCardState.open(CreateKind.CHARACTER, CreateCardMode.PICKER, "세라")
        assertEquals("세라", picker.name)
        assertTrue(picker.canCreate)
        assertFalse(CreateCard.dismissOnScrim(picker))
        val empty = CreateCardState.open(CreateKind.TERM, CreateCardMode.LIST)
        assertFalse(empty.canCreate)
        assertTrue(CreateCard.dismissOnScrim(empty))
        assertFalse(empty.with(CreateFields.NAME, "   ").canCreate)
        assertTrue(empty.with(CreateFields.NAME, "등불").canCreate)
        assertFalse(CreateCard.dismissOnScrim(empty.with("aliases", "등")))
    }

    // ---- U-14 input bar reducer (SCR-70, SCR-16, SCR-72, SCR-73)

    private val rien = InputSpeaker("c1", "리엔")
    private fun s0() = ScriptInputState(speaker = rien, sceneNumber = 3)
    private fun ScriptInputState.typed(t: String) = ScriptInput.reduce(this, E.TextChanged(t, t.length, t.length))
    private fun ScriptInputState.on(e: E) = ScriptInput.reduce(this, e)

    @Test
    fun `U-14_7 Enter adds a line break and sends nothing`() {
        val s = s0().typed("첫 줄").on(E.EnterPressed)
        assertEquals("첫 줄\n", s.text)
        assertEquals(4, s.selectionEnd)
        assertEquals(InputMark.None, s.mark)
        // Mid-text with a selection: replaced by the break.
        val mid = ScriptInput.reduce(s0(), E.TextChanged("가나다", 1, 2)).on(E.EnterPressed)
        assertEquals("가\n다", mid.text)
        assertEquals(2, mid.selectionStart)
    }

    @Test
    fun `U-14_2 chips keep their values after three sends`() {
        var s = s0().on(E.KindPicked(InputKind.DIALOGUE))
        repeat(3) { i ->
            s = s.typed("대사 $i")
            assertTrue(ScriptInputRules.canSend(s))
            s = s.on(E.Sent)
            assertEquals(rien, s.speaker)
            assertEquals(InputKind.DIALOGUE, s.kind)
            assertEquals("", s.text)
        }
        assertFalse(ScriptInputRules.speakerDimmed(s))
        listOf(InputKind.NARRATION, InputKind.DIRECTION, InputKind.MEMO, InputKind.CHOICE, InputKind.SYSTEM).forEach {
            assertTrue(it.name, ScriptInputRules.speakerDimmed(s.on(E.KindPicked(it))))
            assertEquals(rien, s.on(E.KindPicked(it)).speaker)
        }
    }

    @Test
    fun `U-14_5 the insert-after mark is used once`() {
        var s = s0().on(E.InsertAfter("044"))
        assertEquals(InputPlace.After("044"), ScriptInputRules.place(s))
        assertFalse(ScriptInputRules.showsNextScene(s))
        s = s.typed("끼워 넣기").on(E.Sent)
        assertEquals(InputMark.None, s.mark)
        assertEquals(InputPlace.SceneEnd(3), ScriptInputRules.place(s))
        assertTrue(ScriptInputRules.showsNextScene(s))
        // ✕ clears without sending, and the draft stays.
        val closed = s0().typed("남김").on(E.InsertAfter("010")).on(E.CloseMark)
        assertEquals(InputMark.None, closed.mark)
        assertEquals("남김", closed.text)
        assertEquals(InputMark.None, s0().on(E.InsertAfter("010")).on(E.AfterTargetGone).mark)
    }

    @Test
    fun `U-14_11 choice needs two non-blank lines`() {
        val c = s0().on(E.KindPicked(InputKind.CHOICE))
        assertFalse(ScriptInputRules.canSend(c.typed("따라간다")))
        assertFalse(ScriptInputRules.canSend(c.typed("따라간다\n   \n")))
        assertTrue(ScriptInputRules.canSend(c.typed("따라간다\n돌아선다")))
        assertEquals(listOf("따라간다", "돌아선다"), ScriptInputRules.choiceOptions(" 따라간다 \n\n돌아선다\n"))
        // 연출: text or (switch on) a cue.
        val d = s0().on(E.KindPicked(InputKind.DIRECTION))
        assertFalse(ScriptInputRules.canSend(d))
        assertTrue(ScriptInputRules.canSend(d, InputContext(hasCues = true)))
        assertTrue(ScriptInputRules.canSend(d.typed("배경 하층 거리, 잔불 BGM")))
        // Blank, read-only and no scene are off.
        assertFalse(ScriptInputRules.canSend(s0().typed(" \n ")))
        assertFalse(ScriptInputRules.canSend(s0().typed("글"), InputContext(readOnly = true)))
        val noScene = ScriptInputState(speaker = rien).typed("글")
        assertFalse(ScriptInputRules.canSend(noScene))
        assertEquals(InputPlace.NoScene, ScriptInputRules.place(noScene))
        // A 화자 없음 dialogue send is allowed (SCR-62 marks it later).
        assertTrue(ScriptInputRules.canSend(ScriptInputState(sceneNumber = 1).typed("누구?")))
    }

    @Test
    fun `U-14_8 edit sets the draft aside and restores it`() {
        val draft = s0().typed("쓰던 글").on(E.InsertAfter("010"))
        var s = draft.on(E.StartEdit("044", "멈춰. 거기서 더 오면 쏜다.", InputSpeaker("c2", "하르트"), InputKind.DIALOGUE))
        assertEquals(InputPlace.Editing("044"), ScriptInputRules.place(s))
        assertEquals(SendSlot.EDIT_TEXT, ScriptInputRules.sendSlot(s))
        assertEquals("하르트", s.speaker!!.label)
        assertEquals(s.text.length, s.selectionEnd)
        assertFalse(ScriptInputRules.showsNextScene(s))
        // The right swipe's 이 뒤에 is not available while editing.
        assertEquals(s, s.on(E.InsertAfter("050")))
        // Chips change only the edit; ✕ discards and brings the draft back whole.
        s = s.on(E.KindPicked(InputKind.NARRATION)).typed("바뀐 글")
        val back = s.on(E.CloseMark)
        assertEquals("쓰던 글", back.text)
        assertEquals(rien, back.speaker)
        assertEquals(InputKind.DIALOGUE, back.kind)
        assertEquals(InputMark.After("010"), back.mark)
        assertNull(back.setAside)
        assertEquals(SendSlot.ICON, ScriptInputRules.sendSlot(back))
        // A 고치기 send restores the same way.
        assertEquals("쓰던 글", s.on(E.Sent).text)
        // An emptied edit cannot be sent.
        assertFalse(ScriptInputRules.canSend(s.typed("  ")))
        assertTrue(ScriptInputRules.canSend(s.typed("고친 글")))
    }

    @Test
    fun `U-14_9 split needs text on both sides of the caret`() {
        val e = s0().on(E.StartEdit("044", "앞 문장. 뒤 문장.", rien, InputKind.DIALOGUE))
        assertFalse("caret at the end", ScriptInputRules.canSplit(e))
        val mid = e.on(E.TextChanged(e.text, 5, 5))
        assertTrue(ScriptInputRules.canSplit(mid))
        assertEquals("앞 문장." to "뒤 문장.", ScriptInputRules.splitParts(mid))
        assertFalse(ScriptInputRules.canSplit(mid.on(E.KindPicked(InputKind.CHOICE))))
        assertFalse("not editing", ScriptInputRules.canSplit(s0().typed("앞 뒤").on(E.TextChanged("앞 뒤", 1, 1))))
    }

    @Test
    fun `edit target gone keeps or clears the text`() {
        val e = s0().typed("쓰던 글").on(E.StartEdit("044", "원래 글", rien, InputKind.DIALOGUE)).typed("고치던 글")
        val removed = e.on(E.EditTargetGone(locked = false))
        assertEquals("고치던 글", removed.text)
        assertEquals(InputMark.None, removed.mark)
        assertNull(removed.setAside)
        val locked = e.on(E.EditTargetGone(locked = true))
        assertEquals("쓰던 글", locked.text)
        assertEquals(InputMark.None, locked.mark)
    }

    @Test
    fun `U-14_10 tool row and tools`() {
        val s = s0()
        assertFalse(ScriptInputRules.showsToolRow(s))
        assertFalse(ScriptInputRules.showsToolRow(s.on(E.Ime(true))))
        val typing = s.on(E.Ime(true)).on(E.Focus(true))
        assertTrue(ScriptInputRules.showsToolRow(typing))
        assertFalse(ScriptInputRules.showsToolRow(typing, readOnly = true))
        assertFalse(ScriptInputRules.showsToolRow(typing.on(E.Ime(false))))
        assertTrue(ScriptInputRules.parenEnabled(typing))
        assertFalse(ScriptInputRules.parenEnabled(typing.on(E.KindPicked(InputKind.NARRATION))))
        assertFalse(ScriptInputRules.changeEnabled(typing, InputContext()))
        assertTrue(ScriptInputRules.changeEnabled(typing, InputContext(hasTargetBlock = true)))
        assertTrue(ScriptInputRules.changeEnabled(typing.on(E.StartEdit("1", "a", null, InputKind.MEMO)), InputContext()))
    }

    @Test
    fun `U-14_16 field grows to six lines then scrolls`() {
        assertEquals(1, ScriptInputRules.visibleLines(0))
        assertEquals(6, ScriptInputRules.visibleLines(6))
        assertEquals(6, ScriptInputRules.visibleLines(8))
        assertFalse(ScriptInputRules.scrollsInside(6))
        assertTrue(ScriptInputRules.scrollsInside(7))
    }

    @Test
    fun `U-14_21 the place label flashes only on a scene change`() {
        var s = ScriptInputState().on(E.SceneInView(3))
        assertEquals(0, s.sceneFlash)
        s = s.on(E.SceneInView(3))
        assertEquals(0, s.sceneFlash)
        s = s.on(E.SceneInView(4))
        assertEquals(1, s.sceneFlash)
        assertEquals(InputPlace.SceneEnd(4), ScriptInputRules.place(s))
        s = s.on(E.SceneInView(null)).on(E.SceneInView(2))
        assertEquals(1, s.sceneFlash)
    }

    // ---- SCR-30 swipe row

    @Test
    fun `U-14_3 a swipe only reveals and settles past half`() {
        val ref = 192f
        val edit = 300f
        assertEquals(SwipeSide.CLOSED, SwipeReveal.settle(-90f, ref, edit))
        assertEquals(SwipeSide.REFERENCE, SwipeReveal.settle(-97f, ref, edit))
        assertEquals(SwipeSide.CLOSED, SwipeReveal.settle(149f, ref, edit))
        assertEquals(SwipeSide.EDIT, SwipeReveal.settle(151f, ref, edit))
        // Right swipe off (고치기 active, read-only): it never opens and the drag is held at 0.
        assertEquals(SwipeSide.CLOSED, SwipeReveal.settle(299f, ref, edit, editEnabled = false))
        assertEquals(0f, SwipeReveal.clamp(250f, ref, edit, editEnabled = false), 0f)
        assertEquals(-ref, SwipeReveal.clamp(-500f, ref, edit), 0f)
        assertEquals(edit, SwipeReveal.clamp(500f, ref, edit), 0f)
        assertEquals(-ref, SwipeReveal.restOffset(SwipeSide.REFERENCE, ref, edit), 0f)
        assertEquals(edit, SwipeReveal.restOffset(SwipeSide.EDIT, ref, edit), 0f)
        assertEquals(SwipeSide.CLOSED, SwipeReveal.afterTap())
        // Only the layer on the open side exists, so the hidden one cannot take taps (the two overlap on cover).
        assertTrue(SwipeReveal.showsReference(-ref) && !SwipeReveal.showsEdit(-ref))
        assertTrue(SwipeReveal.showsEdit(edit) && !SwipeReveal.showsReference(edit))
        assertFalse(SwipeReveal.showsReference(0f) || SwipeReveal.showsEdit(0f))
        assertFalse(SwipeReveal.showsEdit(edit, editEnabled = false))
        assertFalse(SwipeReveal.showsReference(-ref, referenceEnabled = false))
        assertEquals(192f, SwipeReveal.REFERENCE_WIDTH_DP, 0f)
        assertEquals(300f, SwipeReveal.EDIT_WIDTH_DP, 0f)
    }

    // ---- copy and motion

    @Test
    fun `0_2_0 component copy passes CopyLint`() {
        (IconButtonCopy.all + ScriptInputCopy.all + CreateCardCopy.all + io.github.lenticualrstone.civilight.designsystem.component.TagColorCopy.all + InputKind.entries.map { it.label })
            .forEach { assertTrue("$it: ${CopyLint.lint(it)}", CopyLint.lint(it).isEmpty()) }
        IconButtonSpec.entries.forEach {
            listOfNotNull(it.text, it.accessibilityName).forEach { v -> assertTrue("$v: ${CopyLint.lint(v, isButton = true)}", CopyLint.lint(v, isButton = true).isEmpty()) }
        }
        assertEquals("씬 3 끝에", ScriptInputCopy.sceneEnd(3))
        assertEquals("#044 뒤에", ScriptInputCopy.after("044"))
        assertEquals("#044 고치기", ScriptInputCopy.editing("044"))
    }

    @Test
    fun `design system sources pass MotionLint`() {
        assertEquals("", MotionLint.report(MotionLint.lintModule(File("."), allowEasingDefinitions = true)))
    }
}
