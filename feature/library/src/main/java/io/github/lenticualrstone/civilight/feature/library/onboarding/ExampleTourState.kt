package io.github.lenticualrstone.civilight.feature.library.onboarding

import io.github.lenticualrstone.civilight.domain.story.structure.SceneStatus
import io.github.lenticualrstone.civilight.domain.story.structure.StoryOverview
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.TermCard

/** One `둘러볼 곳` row: `스크립트 · 1-7 씬 3` with its sub, and where it leads. */
data class ExploreRow(val title: String, val sub: String, val route: Route, val isScript: Boolean)

/** `1-6 | 잿더미 시장 | 완료`; an untitled episode reads `—`. */
data class TourEpisodeRow(val key: Key, val code: String, val title: String, val status: String)

/** 예시 둘러보기 · 4/4 (LIB-17, board `OnbExample`). */
data class ExampleTourUi(
    val title: String,
    val oneLiner: String?,
    val explore: List<ExploreRow>,
    /** `Ⅰ 1장 · 등불지기`, or null when the scene has no chapter. */
    val chapterHeader: String?,
    val episodes: List<TourEpisodeRow>,
)

/**
 * Builds 예시 둘러보기 from the example's document: the three places to look at (the scene of the
 * 집필 선, 리엔, 오리지늄 등불; a row whose item is gone is left out) and the episode around that
 * scene with its neighbours. Episode status: `완료` when marked done, `미정` with nothing written,
 * else `씬 <n>개` (board `OnbExample`).
 */
object ExampleTourState {
    fun build(
        doc: ProjectDoc,
        sceneKey: Key = ExampleKeys.SCENE_1_7_3,
        characterKey: Key = ExampleKeys.RIEN,
        termKey: Key = ExampleKeys.TERM_ORIGINIUM_LANTERN,
    ): ExampleTourUi {
        val overview = StoryOverview(doc)
        val codes = overview.codes
        val p = doc.key
        val scene = doc.index.unit(sceneKey)?.takeIf { doc.liveness.isUnitLive(it.key) }
        val character = doc.cast.characters.firstOrNull { it.key == characterKey && it.deletedAt == null }
        val term = doc.world.terms.firstOrNull { it.key == termKey && it.deletedAt == null }
        val explore = listOfNotNull(
            scene?.let {
                ExploreRow(
                    OnboardingCopy.exploreLine(OnboardingCopy.EXPLORE_SCRIPT, codes.sceneRef(it)),
                    OnboardingCopy.EXPLORE_SCRIPT_SUB, Script(p, sceneKey = it.key), isScript = true,
                )
            },
            character?.let {
                ExploreRow(OnboardingCopy.exploreLine(OnboardingCopy.EXPLORE_CAST, it.name), OnboardingCopy.EXPLORE_CAST_SUB, CharacterProfile(p, it.key), isScript = false)
            },
            term?.let {
                ExploreRow(OnboardingCopy.exploreLine(OnboardingCopy.EXPLORE_WORLD, it.name), OnboardingCopy.EXPLORE_WORLD_SUB, TermCard(p, it.key), isScript = false)
            },
        )
        val episode = scene?.let(codes::episodeOf)
        val chapter = scene?.let(codes::chapterOf)
        val episodes = if (episode == null || chapter == null) emptyList() else {
            val siblings = overview.episodesOf(chapter)
            val at = siblings.indexOfFirst { it.key == episode.key }
            siblings.subList((at - 1).coerceAtLeast(0), (at + 2).coerceAtMost(siblings.size)).map { e ->
                val word = overview.episodeStatus(e)
                val status = when (word) {
                    SceneStatus.DONE.word -> OnboardingCopy.EPISODE_DONE
                    SceneStatus.UNDECIDED.word -> OnboardingCopy.EPISODE_UNDECIDED
                    else -> overview.episodeRow(e).meta
                }
                TourEpisodeRow(e.key, codes.unitLabel(e), e.title ?: OnboardingCopy.UNTITLED, status)
            }
        }
        return ExampleTourUi(
            title = doc.project.name,
            oneLiner = doc.project.oneLiner?.takeIf { it.isNotBlank() },
            explore = explore,
            chapterHeader = chapter?.let(codes::chapterHeader),
            episodes = episodes,
        )
    }
}
