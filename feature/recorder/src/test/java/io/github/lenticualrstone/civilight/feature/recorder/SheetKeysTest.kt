package io.github.lenticualrstone.civilight.feature.recorder

import io.github.lenticualrstone.civilight.domain.recorder.put.PutKind
import io.github.lenticualrstone.civilight.navigation.RecorderLinkPickerSheet
import io.github.lenticualrstone.civilight.navigation.RouteCodec
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.navigation.TextMemoSheet
import org.junit.Assert.assertEquals
import org.junit.Test

/** The inbox's sheet stack is saved with `RouteCodec` (shell.md §5): every key must come back as it was. */
class SheetKeysTest {
    @Test
    fun everySheetKeyOfThisModuleRoundTrips() {
        val keys: List<SheetKey> = listOf(
            RecordMenuSheet(Fx.S),
            PutSheet(ExampleKeysR1, PutKind.MANUSCRIPT.name),
            PutSheet(ExampleKeysR1, PutKind.MANUSCRIPT.name, projectKey = Fx.P1),
            PutScenePickSheet(ExampleKeysR1, Fx.P1),
            ProjectFilterSheet,
            RecordLinkSheet(ExampleKeysR1),
            RecordLinkSheet(ExampleKeysR1, replaceLinkKeys = listOf(Fx.S, Fx.P1)),
            RecordLinkSheet(ExampleKeysR1, relinkProjectKey = Fx.P1, relinkKind = "CHARACTER", relinkKey = Fx.S),
            DeletedTargetSheet(ExampleKeysR1, Fx.S),
            PutSheet(ExampleKeysR1, PutKind.CARD.name, projectKey = Fx.P1),
            PutSheet(ExampleKeysR1, PutKind.PAGE.name),
            TextMemoSheet(Fx.P1, "character", Fx.S),
            RecorderLinkPickerSheet(),
        )
        assertEquals(keys, RouteCodec.decodeAll(RouteCodec.encodeAll(keys)))
        for (key in keys) assertEquals(key, RouteCodec.decode(RouteCodec.encode(key)))
    }

    private companion object {
        val ExampleKeysR1 = io.github.lenticualrstone.civilight.model.sample.ExampleKeys.RECORDER_R1
    }
}
