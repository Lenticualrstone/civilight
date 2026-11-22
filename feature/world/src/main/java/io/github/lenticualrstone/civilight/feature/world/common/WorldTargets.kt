package io.github.lenticualrstone.civilight.feature.world.common

import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.domain.world.term.DescribedKind
import io.github.lenticualrstone.civilight.domain.world.term.OutsideSpot
import io.github.lenticualrstone.civilight.domain.world.term.ScriptSpot
import io.github.lenticualrstone.civilight.domain.world.term.TextSpot
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.EventDetail
import io.github.lenticualrstone.civilight.navigation.Navigator
import io.github.lenticualrstone.civilight.navigation.PageDoc
import io.github.lenticualrstone.civilight.navigation.ProfileTab
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.ui.nav.openDetail
import io.github.lenticualrstone.civilight.navigation.FactionCard as FactionCardRoute
import io.github.lenticualrstone.civilight.navigation.PlaceCard as PlaceCardRoute
import io.github.lenticualrstone.civilight.navigation.TermCard as TermCardRoute

/** Where rows of world screens lead (WLD-08 `쓰인 곳` rows, card links). */
object WorldTargets {
    /** The card route of a world item. */
    fun card(projectKey: Key, kind: WorldKind, key: Key): Route = when (kind) {
        WorldKind.TERM -> TermCardRoute(projectKey, key)
        WorldKind.PLACE -> PlaceCardRoute(projectKey, key)
        WorldKind.FACTION -> FactionCardRoute(projectKey, key)
    }

    /**
     * The screen that shows [spot]: the manuscript at the block (or the scene for `전환 문구`), the
     * page, the character's tab, or the card whose text holds it (`[PROPOSED]` WLD-08, as the check
     * center's `원고로 가기`).
     */
    fun of(projectKey: Key, spot: TextSpot): Route = when (spot) {
        is ScriptSpot.BlockText -> Script(projectKey, blockKey = spot.blockKey)
        is ScriptSpot.OptionText -> Script(projectKey, blockKey = spot.blockKey)
        is ScriptSpot.TransitionText -> Script(projectKey, sceneKey = spot.sceneKey)
        is OutsideSpot.MemoBlock -> Script(projectKey, blockKey = spot.blockKey)
        is OutsideSpot.PageText -> PageDoc(projectKey, spot.pageKey)
        is OutsideSpot.CharacterBody ->
            CharacterProfile(projectKey, spot.characterKey, if (spot.secret) ProfileTab.SECRET else ProfileTab.OVERVIEW)
        is OutsideSpot.VoiceLineText -> CharacterProfile(projectKey, spot.characterKey, ProfileTab.GAME_TEXT)
        is OutsideSpot.ArchiveText -> CharacterProfile(projectKey, spot.characterKey, ProfileTab.GAME_TEXT)
        is OutsideSpot.Description -> when (spot.kind) {
            DescribedKind.TERM -> TermCardRoute(projectKey, spot.key)
            DescribedKind.PLACE -> PlaceCardRoute(projectKey, spot.key)
            DescribedKind.FACTION -> FactionCardRoute(projectKey, spot.key)
            DescribedKind.EVENT -> EventDetail(projectKey, spot.key)
        }
        is OutsideSpot.FieldText -> when (spot.entityKind) {
            EntityKind.CHARACTER -> CharacterProfile(projectKey, spot.entityKey)
            EntityKind.PLACE -> PlaceCardRoute(projectKey, spot.entityKey)
            EntityKind.TERM -> TermCardRoute(projectKey, spot.entityKey)
            EntityKind.FACTION -> FactionCardRoute(projectKey, spot.entityKey)
        }
    }

    /** True for routes shown in the detail pane next to the world list (CNV-33). */
    fun isWorldCard(route: Route): Boolean = route is TermCardRoute || route is PlaceCardRoute || route is FactionCardRoute

    /**
     * True when [spot] must not be shown while hidden items are locked: text of a hidden scene, of a
     * hidden character, or of a hidden term, place or faction (rule 19 §6).
     */
    fun isLocked(doc: ProjectDoc, spot: TextSpot, hiddenOpen: Boolean): Boolean {
        if (hiddenOpen) return false
        val sceneKey = when (spot) {
            is ScriptSpot.BlockText -> doc.index.block(spot.blockKey)?.sceneKey
            is ScriptSpot.OptionText -> doc.index.block(spot.blockKey)?.sceneKey
            is ScriptSpot.TransitionText -> spot.sceneKey
            is OutsideSpot.MemoBlock -> doc.index.block(spot.blockKey)?.sceneKey
            else -> null
        }
        if (sceneKey != null) {
            val scene = doc.index.unit(sceneKey) ?: return false
            return scene.hidden || doc.index.ancestors(scene).any { it.hidden }
        }
        return when (spot) {
            is OutsideSpot.CharacterBody -> doc.index.character(spot.characterKey)?.hidden == true
            is OutsideSpot.VoiceLineText -> doc.index.character(spot.characterKey)?.hidden == true
            is OutsideSpot.ArchiveText -> doc.index.character(spot.characterKey)?.hidden == true
            is OutsideSpot.Description -> when (spot.kind) {
                DescribedKind.TERM -> doc.index.term(spot.key)?.hidden == true
                DescribedKind.PLACE -> doc.index.place(spot.key)?.hidden == true
                DescribedKind.FACTION -> doc.index.faction(spot.key)?.hidden == true
                DescribedKind.EVENT -> false
            }
            is OutsideSpot.FieldText -> when (spot.entityKind) {
                EntityKind.CHARACTER -> doc.index.character(spot.entityKey)?.hidden == true
                EntityKind.PLACE -> doc.index.place(spot.entityKey)?.hidden == true
                EntityKind.TERM -> doc.index.term(spot.entityKey)?.hidden == true
                EntityKind.FACTION -> doc.index.faction(spot.entityKey)?.hidden == true
            }
            else -> false
        }
    }
}

/** Opens [route]: world cards beside the world list (CNV-33 `openDetail`), anything else on top. */
fun Navigator.openFromWorld(route: Route) {
    if (WorldTargets.isWorldCard(route)) openDetail(route) else push(route)
}
