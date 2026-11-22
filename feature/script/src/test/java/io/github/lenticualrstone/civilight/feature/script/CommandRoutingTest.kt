package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.feature.script.state.CommandAction
import io.github.lenticualrstone.civilight.feature.script.state.CommandRouting
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRowKeys
import io.github.lenticualrstone.civilight.model.script.BlockKind
import org.junit.Assert.assertEquals
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** SCR-12 slash shortcuts and SCR-68 jumps typed into the command field. */
class CommandRoutingTest {
    private fun route(text: String) = CommandRouting.route(Fx.example, K.EPISODE_1_7, text)

    @Test
    fun `SCR-12 slash alone opens the sheet and shortcuts add blocks`() {
        assertEquals(CommandAction.OpenAddSheet, route("/"))
        assertEquals(CommandAction.AddBlock(BlockKind.DIALOGUE), route("/대"))
        assertEquals(CommandAction.AddBlock(BlockKind.NARRATION), route("/나"))
        assertEquals(CommandAction.AddBlock(BlockKind.DIRECTION), route("/연"))
        assertEquals(CommandAction.AddBlock(BlockKind.CHOICE), route("/선"))
        assertEquals(CommandAction.AddBlock(BlockKind.SYSTEM), route("/시"))
        assertEquals(CommandAction.AddBlock(BlockKind.MEMO), route(" /메 "))
        assertEquals(CommandAction.NewScene, route("/씬"))
    }

    @Test
    fun `SCR-68 block numbers and scenes of this episode jump`() {
        assertEquals(CommandAction.Jump(ScriptRowKeys.block(K.BLOCK_043), K.BLOCK_043), route("#043"))
        assertEquals(CommandAction.Jump(ScriptRowKeys.head(K.SCENE_1_7_3), null), route("씬 3"))
        assertEquals(CommandAction.Jump(ScriptRowKeys.head(K.SCENE_1_7_3), null), route("꺼진 등불"))
    }

    @Test
    fun `the lens command, search and blank input`() {
        assertEquals(CommandAction.SetLens, route("렌즈를 이 씬으로 맞추기"))
        assertEquals(CommandAction.Search("하르트"), route("하르트"))
        assertEquals(CommandAction.Search("#999"), route("#999"))
        assertEquals(CommandAction.Nothing, route("  "))
    }
}
