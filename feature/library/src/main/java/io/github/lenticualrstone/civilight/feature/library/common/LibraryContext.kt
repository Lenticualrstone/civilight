package io.github.lenticualrstone.civilight.feature.library.common

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.library.masking.HiddenAccess
import io.github.lenticualrstone.civilight.model.app.UnlockSession
import io.github.lenticualrstone.civilight.store.StoreContainer
import io.github.lenticualrstone.civilight.ui.security.UnlockState

/** One [ActionContext] from the injected clock and device id (DECISIONS §1: one context per action). */
internal fun StoreContainer.actionContext(): ActionContext =
    ActionContext(now = clock.now(), zone = clock.zone, device = deviceId.value)

/** The shell's unlock state as the model's [UnlockSession] (rule modules take this form). */
internal fun UnlockState.toSession(): UnlockSession = UnlockSession(
    hiddenGroupUnlockedAt = hiddenGroupUnlockedAt,
    itemsUnlocked = itemsUnlockedAt,
    recordsUnlockedAt = recordsUnlockedAt,
)

/**
 * The shell's unlock state as `domain:library` [HiddenAccess], with the projects kept open in this
 * session ("지금은 열린 채", `UnlockController.keepOpen`) so every surface reads the same set.
 */
internal fun UnlockState.toAccess(): HiddenAccess = HiddenAccess(session = toSession(), keptOpen = keptOpen)
