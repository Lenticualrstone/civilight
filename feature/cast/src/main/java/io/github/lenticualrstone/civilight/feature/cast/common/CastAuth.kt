package io.github.lenticualrstone.civilight.feature.cast.common

import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.security.AuthRequest
import io.github.lenticualrstone.civilight.ui.security.AuthResult
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import io.github.lenticualrstone.civilight.feature.cast.profile.SecretGate
import io.github.lenticualrstone.civilight.feature.cast.profile.TabGateState
import io.github.lenticualrstone.civilight.navigation.ProfileTab
import java.util.concurrent.atomic.AtomicInteger

/**
 * (0.2.0, CST-73) The device-lock prompts the cast starts on its own (`이 탭 잠그기` off, the merge
 * comparison of a character whose 비밀 tab is locked). It is the same system prompt the hidden items use
 * (`FeatureDeps.biometric`, biometric with the screen-lock fallback). While one is on screen, [inFlight]
 * is true, so the record's lifecycle observer does not read the fallback's activity switch as the app
 * going to the background ([io.github.lenticualrstone.civilight.feature.cast.profile.SecretTabGate.background]).
 */
object CastAuth {
    private val running = AtomicInteger(0)

    val inFlight: Boolean get() = running.get() > 0

    /**
     * The 비밀 tab of every open record whose selected tab is 비밀, by character, with its gate. The record's
     * view model publishes it synchronously on every change (also from the lifecycle observer), so a relock
     * reaches the secret sheets before the next frame is drawn.
     */
    private val tabs = MutableStateFlow<Map<Key, SecretGate>>(emptyMap())
    val secretTabs: StateFlow<Map<Key, SecretGate>> = tabs.asStateFlow()

    /** The record of [characterKey] now has [state]; null when the record went away. */
    fun publishTab(characterKey: Key, state: TabGateState?) {
        tabs.update { m -> if (state == null || state.tab != ProfileTab.SECRET) m - characterKey else m + (characterKey to state.gate) }
    }

    /** The character whose 비밀 tab is open and confirmed, when it is [characterKey]. */
    fun openSecretOf(characterKey: Key): Key? = characterKey.takeIf { tabs.value[it] == SecretGate.OPEN }

    /**
     * (CST-73) A value or history sheet of a secret field of a character whose 비밀 tab is locked asks for the
     * device lock first, unless that tab is open and confirmed ([openSecretOf]). The sheets are shared, so
     * another area (the script's reference panel) can open them too.
     */
    fun needsFieldAuth(doc: ProjectDoc, kind: EntityKind, entityKey: Key, defKey: Key, openSecretOf: Key? = openSecretOf(entityKey)): Boolean =
        kind == EntityKind.CHARACTER &&
            doc.index.character(entityKey)?.secretTabLocked == true &&
            doc.index.fieldDef(defKey)?.isSecret == true &&
            openSecretOf != entityKey

    /**
     * What a sheet that shows secret text of a character whose 비밀 tab is locked does now (CST-73).
     * [needsLock]: the sheet shows secret text and the tab is locked. [tab]: the gate of that character's record
     * while its 비밀 tab is selected ([secretTabs]), null otherwise. [authed]: the sheet's own prompt succeeded
     * since the app last went to the background. [viaTab]: the sheet was shown through that open tab (a sheet
     * opened elsewhere never waits on a record in the back stack: it asks on its own). While the tab
     * is open the sheet shows; while it is relocked or asking again (background, foreground) the sheet draws
     * nothing and waits; when the tab is left (cancel, another tab, the record left) a sheet that depended on
     * it closes, so nothing is drawn behind the record's prompt and nothing stays after a cancel.
     */
    fun sheetGate(needsLock: Boolean, tab: SecretGate?, authed: Boolean, viaTab: Boolean): SheetGate = when {
        !needsLock -> SheetGate.SHOW
        tab == SecretGate.OPEN -> SheetGate.SHOW
        viaTab && tab != null -> SheetGate.WAIT
        viaTab -> SheetGate.CLOSE
        authed -> SheetGate.SHOW
        else -> SheetGate.PROMPT
    }

    /** True when the user confirmed with the fingerprint or the screen lock. */
    suspend fun confirm(deps: FeatureDeps, request: AuthRequest): Boolean {
        running.incrementAndGet()
        return try {
            deps.biometric.authenticate(request) == AuthResult.SUCCESS
        } finally {
            running.decrementAndGet()
        }
    }
}

/** See [CastAuth.sheetGate]. */
enum class SheetGate { SHOW, WAIT, PROMPT, CLOSE }
