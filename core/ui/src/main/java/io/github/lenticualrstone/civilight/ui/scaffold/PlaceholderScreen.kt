package io.github.lenticualrstone.civilight.ui.scaffold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.owner
import io.github.lenticualrstone.civilight.navigation.routeName

/**
 * Stand-in for a route whose feature has not registered it yet (development only): the route's
 * name, the module that must register it and its arguments. Not product UI.
 */
@Composable
fun PlaceholderScreen(route: NavKey, onBack: (() -> Unit)?) {
    val owner = (route as? Route)?.owner?.gradlePath
    CivilightScreen(
        title = route.routeName,
        smallLine = owner,
        back = onBack?.let { IconAction(BACK_LABEL, it) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 22.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CivilightText(route.toString(), CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
        }
    }
}

/** Accessibility label of the back icon. The boards draw only `‹`; `뒤로` is the label (`ED`). */
const val BACK_LABEL = "뒤로"
