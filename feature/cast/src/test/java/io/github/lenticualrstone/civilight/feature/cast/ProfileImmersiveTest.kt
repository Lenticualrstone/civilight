package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.domain.cast.field.FieldRow
import io.github.lenticualrstone.civilight.feature.cast.profile.ModularTabCopy
import io.github.lenticualrstone.civilight.feature.cast.profile.ProfileHeadLine
import io.github.lenticualrstone.civilight.feature.cast.profile.ProfileTabSet
import io.github.lenticualrstone.civilight.feature.cast.profile.RecordMenuRow
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.ProfileTabKind
import io.github.lenticualrstone.civilight.model.app.ProfileTabPreset
import io.github.lenticualrstone.civilight.ui.profile.ProfileTabSetup
import io.github.lenticualrstone.civilight.ui.profile.ProfileTabSetupCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The immersive record's pure parts: tab set, the shared tab setup sheet, the `⋯` order, subtitle and copy. */
class ProfileImmersiveTest {
    private val game = ProfileTabPreset.GAME_VN.defaultTabs()

    // ---- tabs shown ----

    @Test
    fun `game text tab follows the game tools switch, whatever the preset`() {
        assertTrue(ProfileTabSet.visible(game, gameTools = true).any { it.kind == ProfileTabKind.GAME_TEXT })
        assertFalse(ProfileTabSet.visible(game, gameTools = false).any { it.kind == ProfileTabKind.GAME_TEXT })
    }

    @Test
    fun `disabled tabs drop and an empty set falls back to the standard preset`() {
        val off = game.map { it.copy(enabled = it.kind == ProfileTabKind.GAME_TEXT) }
        val shown = ProfileTabSet.visible(off, gameTools = false)
        assertEquals(ProfileTabPreset.STANDARD.defaultTabs().map { it.id }, shown.map { it.id })
    }

    // ---- the one tab setup sheet ----

    @Test
    fun `active tabs are the custom list, else the preset's`() {
        val preset = AppSettings(profileTabPreset = ProfileTabPreset.MINIMAL)
        assertEquals(ProfileTabPreset.MINIMAL.defaultTabs(), ProfileTabSetup.active(preset))
        assertTrue(ProfileTabSetup.presetSelected(preset, ProfileTabPreset.MINIMAL))
        val custom = preset.copy(customProfileTabs = game)
        assertEquals(game, ProfileTabSetup.active(custom))
        assertFalse(ProfileTabSetup.presetSelected(custom, ProfileTabPreset.MINIMAL))
    }

    @Test
    fun `order moves up and down and stops at the ends`() {
        val up = ProfileTabSetup.move(game, 1, -1)!!
        assertEquals(listOf(game[1], game[0]), up.take(2))
        val down = ProfileTabSetup.move(game, 0, 1)!!
        assertEquals(listOf(game[1], game[0]), down.take(2))
        assertNull(ProfileTabSetup.move(game, 0, -1))
        assertNull(ProfileTabSetup.move(game, game.lastIndex, 1))
    }

    @Test
    fun `the last tab on cannot be turned off`() {
        val one = game.mapIndexed { i, t -> t.copy(enabled = i == 0) }
        assertNull(ProfileTabSetup.toggle(one, one[0].id, false))
        assertTrue(ProfileTabSetup.toggle(one, one[1].id, true)!!.count { it.enabled } == 2)
    }

    @Test
    fun `a custom tab is added at the end with its trimmed name, never blank`() {
        val next = ProfileTabSetup.addCustom(game, "  소속 ", "custom_1")!!
        assertEquals("소속", next.last().label)
        assertEquals(ProfileTabKind.CUSTOM, next.last().kind)
        assertNull(ProfileTabSetup.addCustom(game, "   ", "custom_2"))
    }

    // ---- record menu ----

    @Test
    fun `delete is the last record menu row and the only destructive one`() {
        assertEquals(RecordMenuRow.DELETE, RecordMenuRow.order.last())
        assertEquals(listOf(RecordMenuRow.DELETE), RecordMenuRow.order.filter { it.destructive })
        assertEquals(CastUiCopy.BTN_DELETE, RecordMenuRow.label(RecordMenuRow.DELETE, hidden = false))
        assertEquals(CastUiCopy.BTN_UNHIDE, RecordMenuRow.label(RecordMenuRow.HIDE, hidden = true))
    }

    // ---- subtitle ----

    @Test
    fun `subtitle is the one-liner, else the first head values`() {
        val rows = listOf(FieldRow("직업", "서고 관리인"), FieldRow("빈칸", " "), FieldRow("나이", "28세"), FieldRow("키", "170"))
        assertEquals("고요한 사서", ProfileHeadLine.subtitle(" 고요한 사서 ", rows))
        assertEquals("서고 관리인 · 28세", ProfileHeadLine.subtitle(null, rows))
        assertEquals("서고 관리인 · 28세", ProfileHeadLine.subtitle("  ", rows))
        assertNull(ProfileHeadLine.subtitle(null, emptyList()))
    }

    // ---- copy ----

    @Test
    fun `profile copy passes the lint and has no sentence endings or old words`() {
        val texts = ModularTabCopy.all + ProfileTabSetupCopy.all +
            listOf(ModularTabCopy.traceTitle(3, 2), ModularTabCopy.count(4), ModularTabCopy.more(5), ModularTabCopy.backTo("여담"))
        val failures = texts.mapNotNull { t -> CopyLint.lint(t).takeIf { it.isNotEmpty() }?.let { "$t: $it" } }
        assertEquals(emptyList<String>(), failures)
        for (t in texts) {
            assertFalse(t, t.endsWith("다") || t.endsWith("요") || t.endsWith("."))
            assertFalse(t, "추가" in t || "선택" in t)
        }
        assertEquals("← 여담", ModularTabCopy.backTo("여담"))
    }
}
