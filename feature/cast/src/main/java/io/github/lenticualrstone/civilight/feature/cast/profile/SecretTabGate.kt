package io.github.lenticualrstone.civilight.feature.cast.profile

import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.navigation.ProfileTab

/**
 * (0.2.0, CST-74) The record's tabs: `개요 · 말투 · 관계 · 게임 텍스트 · 비밀`, or `개요 · 말투 · 관계 · 비밀`
 * while `게임 제작 도구` is off (its data kept). Tabs are always addressed by [ProfileTab], never by index,
 * so the 4-tab row cannot open the wrong tab.
 */
object ProfileTabs {
    fun shown(gameTools: Boolean): List<ProfileTab> =
        if (gameTools) ProfileTab.entries else ProfileTab.entries.filter { it != ProfileTab.GAME_TEXT }

    fun label(tab: ProfileTab): String = CastUiCopy.TABS[tab.ordinal]

    fun labels(gameTools: Boolean): List<String> = shown(gameTools).map(::label)

    /** A remembered `게임 텍스트` while the switch is off opens `개요`. */
    fun allowed(tab: ProfileTab, gameTools: Boolean): ProfileTab = if (tab in shown(gameTools)) tab else ProfileTab.OVERVIEW
}

/** `LOCKED → PROMPTING → OPEN` (CST-73). */
enum class SecretGate { LOCKED, PROMPTING, OPEN }

/**
 * The selected tab and the 비밀 gate of one open record. [tab] is what the user selected; while the gate
 * is not [SecretGate.OPEN] for a locked 비밀 tab, [shown] is [previous] (the tab before) and the secret
 * content is never composed.
 */
data class TabGateState(
    val tab: ProfileTab = ProfileTab.OVERVIEW,
    val previous: ProfileTab = ProfileTab.OVERVIEW,
    val gate: SecretGate = SecretGate.LOCKED,
) {
    /** The tab drawn. */
    fun shown(locked: Boolean): ProfileTab = if (tab == ProfileTab.SECRET && locked && gate != SecretGate.OPEN) previous else tab

    /** The device lock prompt is due. */
    fun prompting(locked: Boolean): Boolean = locked && tab == ProfileTab.SECRET && gate == SecretGate.PROMPTING
}

/**
 * (0.2.0, N-43, CST-73) The pure gate of the 비밀 tab: while `이 탭 잠그기` is on, **every** opening asks
 * for the fingerprint or the screen lock. An opening is selecting the tab, opening a record whose
 * remembered tab is 비밀, or the app coming back to the foreground with the tab shown. Leaving the tab,
 * leaving the record or the app going to the background locks it again; a configuration change keeps it
 * (the state lives in the record's view model). Cancel or failure keeps the previous tab (`개요` when the
 * record opened on 비밀).
 */
object SecretTabGate {
    /** A record opened on [tab] (its remembered tab), [gameTools] applied. */
    fun open(tab: ProfileTab, gameTools: Boolean, locked: Boolean): TabGateState {
        val t = ProfileTabs.allowed(tab, gameTools)
        return if (t == ProfileTab.SECRET && locked) TabGateState(t, ProfileTab.OVERVIEW, SecretGate.PROMPTING) else TabGateState(t)
    }

    /** Selecting [target]. */
    fun select(state: TabGateState, target: ProfileTab, locked: Boolean): TabGateState = when {
        // 비밀 selected but relocked (the app came back, the lock turned on again by undo or redo): ask now.
        target == state.tab && target == ProfileTab.SECRET && locked && state.gate == SecretGate.LOCKED -> state.copy(gate = SecretGate.PROMPTING)
        target == state.tab -> state
        target == ProfileTab.SECRET && locked -> TabGateState(target, state.shown(locked), SecretGate.PROMPTING)
        target == ProfileTab.SECRET -> TabGateState(target, state.shown(locked), SecretGate.OPEN)
        // Leaving the 비밀 tab locks it again.
        else -> TabGateState(target, state.shown(locked), SecretGate.LOCKED)
    }

    /** The prompt succeeded. */
    fun confirm(state: TabGateState): TabGateState =
        if (state.gate == SecretGate.PROMPTING) state.copy(gate = SecretGate.OPEN) else state

    /** The prompt was cancelled or failed: back to the previous tab. */
    fun cancel(state: TabGateState): TabGateState =
        if (state.gate == SecretGate.PROMPTING) TabGateState(state.previous, state.previous, SecretGate.LOCKED) else state

    /**
     * The app went to the background. [ownPrompt] is true while a device-lock prompt that the cast itself
     * started is on screen (turning `이 탭 잠그기` off, the merge comparison): the screen-lock fallback
     * of that prompt stops the activity, and that is not leaving the tab, so the tab stays open and no
     * second prompt follows when it returns.
     */
    fun background(state: TabGateState, ownPrompt: Boolean = false): TabGateState =
        if (state.gate == SecretGate.OPEN && !ownPrompt) state.copy(gate = SecretGate.LOCKED) else state

    /** The app came back with the 비밀 tab selected: ask again. */
    fun foreground(state: TabGateState, locked: Boolean): TabGateState =
        if (locked && state.tab == ProfileTab.SECRET && state.gate == SecretGate.LOCKED) state.copy(gate = SecretGate.PROMPTING) else state

    /** `이 탭 잠그기` turned on from inside the open tab: asks nothing and keeps the tab open. */
    fun lockedFromInside(state: TabGateState): TabGateState =
        if (state.tab == ProfileTab.SECRET) state.copy(gate = SecretGate.OPEN) else state

    /** `게임 제작 도구` switched off while `게임 텍스트` was selected. */
    fun gameToolsChanged(state: TabGateState, gameTools: Boolean): TabGateState =
        if (state.tab in ProfileTabs.shown(gameTools)) state else TabGateState(ProfileTab.OVERVIEW)
}
