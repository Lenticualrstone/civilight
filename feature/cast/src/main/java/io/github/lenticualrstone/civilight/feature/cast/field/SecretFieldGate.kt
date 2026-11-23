package io.github.lenticualrstone.civilight.feature.cast.field

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.CastAuth
import io.github.lenticualrstone.civilight.feature.cast.common.SheetGate
import io.github.lenticualrstone.civilight.feature.cast.profile.SecretGate
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.navigation.ProfileTab
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import io.github.lenticualrstone.civilight.ui.security.AuthRequest

/**
 * (0.2.0, CST-73) Wraps the shared value and history sheets of a field: a secret field of a character whose
 * 비밀 tab is locked is drawn only after the device lock ([CastAuth.needsFieldAuth]); a cancel closes the
 * sheet. Nothing of the sheet is composed before the confirmation.
 */
@Composable
fun SheetScope.SecretFieldGate(kind: EntityKind, entityKey: Key, defKey: Key, content: @Composable SheetScope.() -> Unit) {
    val doc by currentProjectStore.doc.collectAsState()
    SecretSheetGate(entityKey, CastAuth.needsFieldAuth(doc, kind, entityKey, defKey, openSecretOf = null), content)
}

/**
 * (CST-73) The gate of every sheet that shows secret text of [characterKey] ([needsLock]: that text and a locked
 * 비밀 tab). It observes the open tab ([CastAuth.openSecretFlow]) and the app's background, so a relock hides
 * the sheet at once: a sheet opened from the open tab closes, one that asked on its own asks again. The
 * confirmation is plain `remember`: a restored sheet asks again.
 */
@Composable
fun SheetScope.SecretSheetGate(characterKey: Key, needsLock: Boolean, content: @Composable SheetScope.() -> Unit) {
    val deps = LocalFeatureDeps.current
    val tabs by CastAuth.secretTabs.collectAsState()
    val tab = tabs[characterKey]
    // The sheet's own confirmation is cleared when the app goes to the background.
    val stops = rememberStopCount()
    var authed by remember(characterKey, stops) { mutableStateOf(false) }
    var viaTab by remember(characterKey) { mutableStateOf(false) }
    if (needsLock && tab == SecretGate.OPEN && !viaTab) SideEffect { viaTab = true }
    when (CastAuth.sheetGate(needsLock, tab, authed, viaTab)) {
        SheetGate.SHOW -> content()
        SheetGate.WAIT -> Unit
        SheetGate.CLOSE -> LaunchedEffect(Unit) { close() }
        SheetGate.PROMPT -> LaunchedEffect(characterKey, stops) {
            if (CastAuth.confirm(deps, AuthRequest(CastUiCopy.TABS[ProfileTab.SECRET.ordinal]))) authed = true else close()
        }
    }
}

/**
 * How often the app went to the background while this sheet was shown (a stop during the cast's own prompt,
 * whose screen-lock fallback stops the activity, does not count). A confirmation keyed on it is cleared by the
 * background; it is plain `remember`, so a restored sheet asks again.
 */
@Composable
fun rememberStopCount(): Int {
    var stops by remember { mutableStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !CastAuth.inFlight) stops++
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return stops
}
