package io.github.lenticualrstone.civilight.domain.story.scene

import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.TimeLabels
import io.github.lenticualrstone.civilight.domain.story.time.sameTimeAs
import io.github.lenticualrstone.civilight.domain.story.units.Particles
import io.github.lenticualrstone.civilight.domain.story.units.PlaceNames
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/** Where a silent-cast member of the split scene goes (`씬 3 · 새 씬 4 · 둘 다`, default `둘 다`). */
enum class SilentCastChoice { FIRST, NEW, BOTH }

/**
 * Preview of `씬 나누기` (STO-09 values for SCR-50). Nothing changes before [result] is taken.
 *
 * @property placeLine `장소 · 결사의 문 · 씬 3과 같음` (null when A has no place).
 * @property timeLine `때 · 씬 3 기준 · 같은 때`; `때 · 회상` for a flashback; null without 때.
 * @property changeRows `순번 · 씬 4 · 미정 → 씬 5`, then `씬 5 때 · 기준 씬 3 · 2일째 아침 06:10 그대로`.
 */
data class SplitPreview(
    val title: String,
    val sub: String,
    val newSceneKey: Key,
    val firstPart: String,
    val newPart: String,
    val placeLine: String?,
    val timeLine: String?,
    val titleLine: String,
    val silentCast: List<Key>,
    val flashbackRow: Boolean,
    val hiddenRow: Boolean,
    val changeRows: List<String>,
    val result: ProjectDoc,
)

/** `씬 나누기`: A → A + new B below the split block. Split never changes another scene's absolute 때. */
object SceneSplit {
    /**
     * @param afterBlockKey the last block that stays in A; later blocks move to B with their keys,
     *   numbers, voice IDs and records unchanged.
     * @param silentCast choice per silent-cast member of A (missing = [SilentCastChoice.BOTH]).
     * @param keepFlashback B stays 회상 when A is (toggle shown on); off → B has no 때.
     * @param keepHidden B stays hidden when A is (toggle shown on).
     */
    fun plan(
        doc: ProjectDoc,
        sceneKey: Key,
        afterBlockKey: Key,
        ctx: EditContext,
        silentCast: Map<Key, SilentCastChoice> = emptyMap(),
        keepFlashback: Boolean = true,
        keepHidden: Boolean = true,
    ): SplitPreview {
        val a = requireNotNull(doc.index.unit(sceneKey)) { "unknown scene $sceneKey" }
        val times = SceneTimes(doc)
        val codes = UnitCodes(doc)
        val labels = TimeLabels(times, codes)
        val all = doc.index.blocksOfScene(sceneKey)
        val split = all.indexOfFirst { it.key == afterBlockKey }
        require(split >= 0) { "$afterBlockKey is not in $sceneKey" }
        val stay = all.take(split + 1)
        val move = all.drop(split + 1)

        val bTime: SceneTime = when (val t = a.time) {
            is SceneTime.Flashback -> if (keepFlashback) t else SceneTime.None
            is SceneTime.Relative, is SceneTime.Direct -> sameTimeAs(times, sceneKey)
            else -> SceneTime.None
        }
        val choiceOf = { k: Key -> silentCast[k] ?: SilentCastChoice.BOTH }
        val newKey = ctx.newKey()
        val b = StoryUnit(
            key = newKey,
            parentKey = a.parentKey,
            level = a.level,
            order = NewScene.orderAfter(doc, a.parentKey, sceneKey),
            place = a.place,
            time = bTime,
            hidden = a.hidden && keepHidden,
            silentCast = a.silentCast.filter { choiceOf(it) != SilentCastChoice.FIRST },
            transitionMode = TransitionMode.NONE.open(),
            updatedAt = ctx.now,
            updatedBy = ctx.device,
        )
        val newA = a.copy(silentCast = a.silentCast.filter { choiceOf(it) != SilentCastChoice.NEW }, updatedAt = ctx.now, updatedBy = ctx.device)
        val movedKeys = move.map { it.key }.toSet()
        val units = doc.story.units.map { if (it.key == sceneKey) newA else it } + b
        val episodes = doc.episodes.mapValues { (_, f) ->
            f.copy(blocks = f.blocks.map { blk -> if (blk.key in movedKeys) blk.copy(sceneKey = newKey, updatedAt = ctx.now, updatedBy = ctx.device) else blk })
        }
        val result = doc.copy(story = doc.story.copy(units = units), episodes = episodes)

        val n = codes.ordinal(a)
        val sceneWord = codes.sceneLabel(a).substringBeforeLast(' ')
        val liveStay = stay.count { it.deletedAt == null }
        val liveMove = move.count { it.deletedAt == null }
        val later = doc.index.children(a.parentKey).filter { doc.liveness.isUnitLive(it.key) && it.order > a.order }
        val newCodes = UnitCodes(result)
        val rows = later.map { "순번 · ${codes.sceneLabel(it)} · ${it.title ?: "미정"} → ${newCodes.sceneLabel(it)}" } +
            times.directDependents(sceneKey).map { d ->
                "${newCodes.sceneRefRelativeTo(result.index.unit(d.key)!!, newA)} 때 · 기준 ${codes.sceneLabel(a)} · ${labels.head(d.key)} 그대로"
            }
        val placeName = PlaceNames(doc).name(a.place)
        val number = doc.index.blocksOfScene(sceneKey).firstOrNull { it.key == afterBlockKey }?.number
        return SplitPreview(
            title = "씬 나누기",
            sub = "${codes.sceneRef(a)} · #${number.toString().padStart(3, '0')} 아래",
            newSceneKey = newKey,
            firstPart = "${codes.sceneLabel(a)} · 블록 ${liveStay}개",
            newPart = "새 $sceneWord ${n + 1} · 블록 ${liveMove}개",
            placeLine = placeName?.let { "장소 · $it · $sceneWord ${Particles.gwa(n)} 같음" },
            timeLine = when (bTime) {
                is SceneTime.Relative -> "때 · ${codes.sceneLabel(a)} 기준 · 같은 때"
                is SceneTime.Flashback -> "때 · 회상"
                else -> null
            },
            titleLine = "제목 · 미정",
            silentCast = a.silentCast,
            flashbackRow = a.time is SceneTime.Flashback,
            hiddenRow = a.hidden,
            changeRows = rows,
            result = result,
        )
    }
}
