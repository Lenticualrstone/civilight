package io.github.lenticualrstone.civilight.feature.story.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.story.common.StoryActionsViewModel
import io.github.lenticualrstone.civilight.feature.story.common.storyServices
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.BulkPlaceTime
import io.github.lenticualrstone.civilight.navigation.NewUnitSheet
import io.github.lenticualrstone.civilight.navigation.SceneMergeSheet
import io.github.lenticualrstone.civilight.navigation.SceneWhereSheet
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.navigation.StructureLevels
import io.github.lenticualrstone.civilight.navigation.UnitMenuSheet
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import kotlinx.serialization.Serializable

/** Which row set a unit ⋯ shows: the scene-head one while the script is on top (SCR-47), else the 스토리 list one. */
fun menuSurfaceFor(current: NavKey?): MenuSurface = if (current is Script) MenuSurface.SCENE_HEAD else MenuSurface.STORY_LIST

/** The structure ⋯ on the 스토리 head (feature-local: `UnitMenuSheet` always names a unit). */
@Serializable
data class StructureMenuSheet(val projectKey: Key) : SheetKey

/** The story sheets' view model in the hosting entry (any project screen of any lane). */
@Composable
internal fun storyActions(projectKey: Key, deps: FeatureDeps): StoryActionsViewModel =
    entryViewModel(key = "story.actions") { StoryActionsViewModel(projectKey, deps.storyServices()) }

/**
 * `UnitMenuSheet` content (shared, owner STORY): the ⋯ of a scene, an episode or a unit above. Opened
 * from the 스토리 list and from the script's scene head; self-contained (its own view model, the
 * hosting project's store).
 */
@Composable
fun SheetScope.UnitMenuContent(sheet: UnitMenuSheet, deps: FeatureDeps) {
    val host = this
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val actions = storyActions(sheet.projectKey, deps)
    val surface = menuSurfaceFor(deps.navigator.backStack.lastOrNull())
    val model = remember(doc, sheet.unitKey, surface, store.readOnly) { UnitMenuModel.build(doc, sheet.unitKey, surface, store.readOnly) }
    if (model == null) {
        SheetHeader(title = "", onClose = ::close)
        return
    }
    SheetHeader(title = model.title, onClose = ::close)
    SheetScrollBody {
        model.rows.forEach { row ->
            MenuRowView(row) {
                val key = sheet.unitKey
                val p = sheet.projectKey
                when (row.action) {
                    MenuAction.PLACE_TIME, MenuAction.TRANSITION -> host.replaceWith(SceneWhereSheet(p, key))
                    MenuAction.MERGE_PREVIOUS -> host.replaceWith(SceneMergeSheet(p, key))
                    MenuAction.SPLIT_HERE -> {
                        host.close()
                        deps.navigator.push(Script(p, sceneKey = key))
                    }
                    MenuAction.DELETE_SCENE -> {
                        actions.deleteScene(key)
                        host.close()
                    }
                    MenuAction.NEW_SCENE_BEFORE -> {
                        val parent = doc.index.unit(key)?.parentKey
                        if (parent != null) host.replaceWith(NewUnitSheet(p, parentKey = parent, beforeKey = key)) else host.close()
                    }
                    MenuAction.RENUMBER -> host.replaceWith(RenumberSheet(p, key))
                    MenuAction.NEW_UNIT_AFTER -> {
                        actions.newUnitAfter(key, row.label)
                        host.close()
                    }
                    MenuAction.MARK_DONE, MenuAction.UNMARK_DONE -> {
                        actions.setDone(key, row.action == MenuAction.MARK_DONE, row.label)
                        host.close()
                    }
                    MenuAction.BULK, MenuAction.LEVEL_NAMES, MenuAction.NEW_TOP_UNIT -> host.close()
                }
            }
        }
    }
}

/** The structure ⋯ content (STO-07). */
@Composable
fun SheetScope.StructureMenuContent(sheet: StructureMenuSheet, deps: FeatureDeps) {
    val host = this
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val actions = storyActions(sheet.projectKey, deps)
    val model = remember(doc, store.readOnly) { StructureMenuModel.build(doc, store.readOnly) }
    SheetHeader(title = model.title, onClose = ::close)
    SheetScrollBody {
        model.rows.forEach { row ->
            MenuRowView(row) {
                host.close()
                when (row.action) {
                    MenuAction.BULK -> deps.navigator.push(BulkPlaceTime(sheet.projectKey))
                    MenuAction.LEVEL_NAMES -> deps.navigator.push(StructureLevels(sheet.projectKey))
                    MenuAction.NEW_TOP_UNIT -> actions.newTopUnit(row.label)
                    else -> Unit
                }
            }
        }
    }
}

/** Closes this sheet and opens [next] in its place. */
private fun SheetScope.replaceWith(next: SheetKey) {
    close()
    open(next)
}

/** A ⋯ row as `RSHeadMenu` draws it: min 52, `line` top, label 15 / 21 medium, value `small`. */
@Composable
private fun MenuRowView(row: MenuRow, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .pressable(onClick = onClick, enabled = row.enabled, scaleOnPress = false)
            .alpha(if (row.enabled) 1f else DISABLED_ALPHA)
            .heightIn(min = 52.dp)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        CivilightText(row.label, type.itemTitleCompact.merge(TextStyle(fontWeight = FontWeight.Medium)), color = colors.ink, maxLines = 1)
        row.value?.let { CivilightText(it, type.small, color = colors.ink3, maxLines = 1) }
    }
}

private const val DISABLED_ALPHA = 0.38f
