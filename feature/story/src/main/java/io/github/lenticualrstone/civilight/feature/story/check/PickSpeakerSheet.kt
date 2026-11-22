package io.github.lenticualrstone.civilight.feature.story.check

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.script.BlockOps
import io.github.lenticualrstone.civilight.domain.story.scene.Appearance
import io.github.lenticualrstone.civilight.domain.checks.CheckCopy
import io.github.lenticualrstone.civilight.feature.story.menu.storyActions
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.picker.CharacterPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import kotlinx.serialization.Serializable
import io.github.lenticualrstone.civilight.domain.script.EditContext as ScriptEditContext

/** `화자 고르기` from a `▲ 화자 없음` row (feature-local): the shared 화자 picker, then the block's speaker. */
@Serializable
data class PickSpeakerSheet(val projectKey: Key, val blockKey: Key) : SheetKey

@Composable
fun SheetScope.PickSpeakerContent(sheet: PickSpeakerSheet, deps: FeatureDeps) {
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val actions = storyActions(sheet.projectKey, deps)
    val sceneKey = doc.index.block(sheet.blockKey)?.sceneKey
    CharacterPickerSheet(
        doc = doc,
        gate = rememberHiddenGate(sheet.projectKey),
        onPick = { character ->
            actions.apply(CheckCopy.PICK_SPEAKER) { d, ctx -> SpeakerFix.set(d, sheet.blockKey, character, ctx) }
            close()
        },
        onClose = ::close,
        sceneCast = sceneKey?.let { Appearance.of(doc, it).speakers }.orEmpty(),
    )
}

/** The speaker change through `domain:script` (DECISIONS §1: block edits). */
object SpeakerFix {
    fun set(doc: ProjectDoc, blockKey: Key, characterKey: Key, ctx: ActionContext): ProjectDoc {
        if (doc.index.block(blockKey) == null) return doc
        return BlockOps.changeSpeaker(doc, blockKey, characterKey, ScriptEditContext(now = ctx.now, zone = ctx.zone, device = ctx.device, newKey = ctx.newKey))
    }
}
