package io.github.lenticualrstone.civilight.feature.script.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap

/**
 * A sheet that swaps its body for a picker step (`장소 고르기`, `다른 항목에 잇기`) and back: the two
 * crossfade and the sheet follows the new height (motion audit P3). The body keeps the column's
 * `weight(1f, fill = false)` the sheet gave its scroll area, so a tall step still scrolls.
 */
@Composable
fun ColumnScope.SheetStep(
    picking: Boolean,
    picker: @Composable () -> Unit,
    body: @Composable () -> Unit,
) {
    ContentSwap(picking, Modifier.weight(1f, fill = false), label = "sheetStep") { step ->
        // `AnimatedContent` stacks its children like a Box: the sheet's header, scroll body and actions need their column.
        Column(Modifier.fillMaxWidth()) {
            if (step) picker() else body()
        }
    }
}
