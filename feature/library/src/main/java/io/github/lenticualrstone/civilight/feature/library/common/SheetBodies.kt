package io.github.lenticualrstone.civilight.feature.library.common

import androidx.compose.runtime.Composable
import io.github.lenticualrstone.civilight.ui.feature.SheetScope

/** The content of one local sheet, as `CivilightScreen(localSheets = …)` takes it. */
typealias SheetBody = @Composable SheetScope.() -> Unit

/** Types a local sheet's content lambda (keeps `localSheets` branches readable). */
fun sheetBody(content: @Composable SheetScope.() -> Unit): SheetBody = content
