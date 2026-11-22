package io.github.lenticualrstone.civilight.domain.checks.sources

import io.github.lenticualrstone.civilight.domain.cast.voice.VoiceLines
import io.github.lenticualrstone.civilight.domain.checks.CheckCopy
import io.github.lenticualrstone.civilight.domain.checks.CheckRegistry
import io.github.lenticualrstone.civilight.domain.checks.CheckResult
import io.github.lenticualrstone.civilight.domain.checks.FixAction
import io.github.lenticualrstone.civilight.domain.checks.FixKind
import io.github.lenticualrstone.civilight.domain.story.checks.Fingerprints
import io.github.lenticualrstone.civilight.domain.story.scene.Appearance
import io.github.lenticualrstone.civilight.model.checks.CheckType

/** Row 5 (`▲ 보이스 비어 있음`, CST-14) and row 19 (`◇ 숨긴 인물이 보이는 씬에 나옴`, R19 §6). */
internal object CastSource {
    /** One row per live character with an empty voice line, in cast order. */
    fun voiceEmpty(scope: CheckScope): List<CheckResult> = scope.doc.cast.characters
        .filter { it.deletedAt == null }
        .mapNotNull { c ->
            val check = VoiceLines.emptyCheck(scope.doc, c.key) ?: return@mapNotNull null
            CheckResult(
                checkType = CheckType.VOICE_EMPTY,
                subjectKeys = listOf(c.key),
                fingerprint = Fingerprints.of(CheckType.VOICE_EMPTY, c.key),
                title = check.label,
                location = check.location,
                body = check.body,
                detail = check.detail,
                actions = listOf(FixAction(check.action, FixKind.WRITE_VOICE, check.firstEmptyLineKey)),
                lockedHidden = scope.hiding.character(c.key),
            )
        }

    /**
     * A live hidden character who appears (speaks or is silent cast, `ED`) in a live scene that is
     * not hidden. The app never hides the scene itself; `씬도 숨기기` is the author's choice.
     */
    fun hiddenInVisibleScenes(scope: CheckScope): List<CheckResult> {
        val doc = scope.doc
        val hidden = doc.cast.characters.filter { it.deletedAt == null && it.hidden }
        if (hidden.isEmpty()) return emptyList()
        val keys = hidden.map { it.key }.toSet()
        return doc.manuscript.liveScenes
            .filterNot { scope.hiding.unitHidden(it.key) }
            .flatMap { scene ->
                Appearance.of(doc, scene.key).all.filter { it in keys }.map { characterKey ->
                    CheckResult(
                        checkType = CheckType.HIDDEN_CHARACTER_IN_VISIBLE_SCENE,
                        subjectKeys = listOf(scene.key, characterKey),
                        fingerprint = Fingerprints.of(CheckType.HIDDEN_CHARACTER_IN_VISIBLE_SCENE, scene.key, characterKey),
                        title = CheckCopy.join(CheckCopy.HIDDEN_CHARACTER, scope.codes.sceneLabel(scene)),
                        location = scope.codes.sceneRef(scene),
                        body = doc.index.character(characterKey)?.name,
                        actions = listOf(
                            FixAction(CheckCopy.HIDE_SCENE, FixKind.HIDE_SCENE, scene.key),
                            FixAction(CheckRegistry.ackLabel(CheckType.HIDDEN_CHARACTER_IN_VISIBLE_SCENE)!!, FixKind.ACKNOWLEDGE, characterKey),
                        ),
                        storyOrder = scope.sceneOrder(scene.key),
                        lockedHidden = scope.hiding.locked,
                    )
                }
            }
    }
}
