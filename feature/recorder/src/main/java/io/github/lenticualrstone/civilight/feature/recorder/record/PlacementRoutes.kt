package io.github.lenticualrstone.civilight.feature.recorder.record

import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.put.CardDocs
import io.github.lenticualrstone.civilight.domain.recorder.state.PlacementStatus
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordStates
import io.github.lenticualrstone.civilight.model.recorder.Placement
import io.github.lenticualrstone.civilight.model.recorder.PlacementKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.FactionCard
import io.github.lenticualrstone.civilight.navigation.PageDoc
import io.github.lenticualrstone.civilight.navigation.PlaceCard
import io.github.lenticualrstone.civilight.navigation.ProfileTab
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.StoryStructure
import io.github.lenticualrstone.civilight.navigation.TermCard

/**
 * Where a `넣은 곳` row of the record screen goes (LRC-28): a block → the script at that block; a card
 * paragraph → the card (a character on its tab: 말투 · 비밀 as their own tabs, 개요 and `긴 글`
 * fields on 개요); a page → the page; a scene memo → the script at that scene; a chapter or episode
 * line → 스토리 구조 at that unit; a new character → the profile. A copy in the trash or gone has no
 * destination (the row is drawn, not tappable: the project trash is reached from its own screens).
 */
object PlacementRoutes {
    fun routeOf(placement: Placement, env: RecorderEnv): Route? {
        if (RecordStates.placementStatus(placement, env) != PlacementStatus.ACTIVE) return null
        val project = env.project(placement.projectKey) ?: return null
        val doc = project.doc
        val p = placement.projectKey
        return when (placement.kind.known) {
            PlacementKind.SCRIPT_BLOCK -> Script(p, blockKey = placement.targetKey)
            PlacementKind.CARD_PARAGRAPH -> {
                val key = placement.containerKey ?: return null
                when (CardDocs.kindOf(doc, key)) {
                    RecorderTargetKind.CHARACTER -> CharacterProfile(p, key, profileTab(placement.tabId))
                    RecorderTargetKind.PLACE -> PlaceCard(p, key)
                    RecorderTargetKind.TERM -> TermCard(p, key)
                    RecorderTargetKind.FACTION -> FactionCard(p, key)
                    else -> null
                }
            }
            PlacementKind.PAGE -> PageDoc(p, placement.targetKey)
            PlacementKind.MEMO_FIELD -> {
                val unit = doc.index.unit(placement.targetKey) ?: return null
                val isScene = doc.index.allScenesInOrder.any { it.key == unit.key }
                if (isScene) Script(p, sceneKey = unit.key) else StoryStructure(p, focusUnitKey = unit.key)
            }
            PlacementKind.CHARACTER -> CharacterProfile(p, placement.targetKey)
            null -> null
        }
    }

    private fun profileTab(tabId: String?): ProfileTab = when (tabId) {
        CardDocs.SPEECH -> ProfileTab.SPEECH
        CardDocs.SECRET -> ProfileTab.SECRET
        else -> ProfileTab.OVERVIEW
    }
}
