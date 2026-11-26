package io.github.lenticualrstone.civilight.feature.world.place

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.Avatar
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightSwitch
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.ui.image.rememberCharacterAvatarPainter
import io.github.lenticualrstone.civilight.ui.reorder.ReorderList
import io.github.lenticualrstone.civilight.ui.reorder.ReorderRowScope
import io.github.lenticualrstone.civilight.ui.reorder.ReorderState

/** What the `소속` rows do; every target is resolved by the screen (live → card, deleted → rule-20 sheet). */
class AffiliationActionsUi(
    val onFaction: (FactionRowUi) -> Unit,
    val onMember: (MemberRowUi) -> Unit,
    val onLoose: (LooseRowUi) -> Unit,
    val onIncludeSubPlaces: (Boolean) -> Unit,
    val onPickBasis: () -> Unit,
    val onUnlock: () -> Unit,
    val onMoveFaction: (Key, Int) -> Unit,
    val onMoveMember: (factionKey: Key, Key, Int) -> Unit,
    val onMoveLoose: (Key, Int) -> Unit,
)

/**
 * `소속 세력 및 인물` (board `ImmContent` place branch, `ImmPlaceRules`): head `소속 세력 및 인물 · 세력 N · 인물 N`;
 * the row with the switch `하위 지역 포함` and `순서 바꾸기`; the basis line; faction rows (chevron 44, name,
 * `거점`, count) with their people; `무소속 · N` outside; `숨긴 인물 N · 지문으로 보기`. Without a `소속` basis the
 * factions come by `거점` only and `소속 기준 고르기` takes the people's place.
 */
@Composable
fun AffiliationSectionView(
    doc: ProjectDoc?,
    ui: AffiliationUi,
    expanded: Set<Key>,
    onExpand: (Key, Boolean) -> Unit,
    reorder: ReorderState,
    editable: Boolean,
    actions: AffiliationActionsUi,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
            CivilightText(ui.title, type.heading, Modifier.weight(1f, fill = false), color = colors.ink, maxLines = 1)
            CivilightText(ui.header, type.small, Modifier.padding(start = 12.dp), color = colors.ink3, maxLines = 1)
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            // The label and the switch toggle together (one switch for the screen reader).
            Row(
                Modifier
                    .pressable(PressAction.Toggle(ui.includeSubPlaces, actions.onIncludeSubPlaces), role = Role.Switch, scaleOnPress = false)
                    .heightIn(min = 44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CivilightText(Copy.Affiliation.INCLUDE_SUB_PLACES, type.label, color = colors.ink2, maxLines = 1)
                CivilightSwitch(ui.includeSubPlaces, onCheckedChange = null)
            }
            FoldReveal((ui.canReorder && editable) || reorder.ordering, horizontal = true) {
                TextAction(
                    if (reorder.ordering) Copy.Affiliation.REORDER_DONE else Copy.Affiliation.REORDER,
                    { reorder.toggle() },
                    color = if (reorder.ordering) colors.ink else colors.acc,
                )
            }
        }
        FoldRevealValue(ui.basisLine) { line -> CivilightText(line, type.small, Modifier.padding(top = 2.dp, bottom = 8.dp), color = colors.ink3) }
        ReorderList(
            state = reorder,
            list = AffiliationUi.LIST_FACTIONS,
            keys = ui.factions.map { it.key },
            labels = PlaceCopy.REORDER_LABELS,
            enabled = editable,
            onMove = actions.onMoveFaction,
        ) { key, _ ->
            val f = ui.factions.first { it.key == key }
            FactionBox(doc, f, key in expanded, onExpand, reorder, editable, actions)
        }
        FoldRevealValue(ui.pickBasis) { label ->
            Row(Modifier.fillMaxWidth().topHairline(colors.line).padding(top = 10.dp)) {
                CivilightButton(label, actions.onPickBasis, variant = ButtonVariant.GHOST)
            }
        }
        val looseTitle = ui.looseTitle
        if (looseTitle != null) {
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp).topHairline(colors.line).padding(top = 14.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CivilightText(looseTitle, type.itemTitleCompact.merge(TextStyle(fontWeight = FontWeight.SemiBold)), color = colors.ink, maxLines = 1)
                CivilightText(PlaceCopy.LOOSE_NOTE, type.small, Modifier.padding(start = 12.dp), color = colors.ink3, maxLines = 1)
            }
            ReorderList(
                state = reorder,
                list = AffiliationUi.LIST_LOOSE,
                keys = ui.loose.map { it.key },
                labels = PlaceCopy.REORDER_LABELS,
                enabled = editable,
                onMove = actions.onMoveLoose,
            ) { key, _ ->
                val m = ui.loose.first { it.key == key }
                PersonRow(
                    doc = doc,
                    key = m.key,
                    name = m.name,
                    role = m.role,
                    deleted = m.deleted,
                    trailing = { CivilightText(m.line, type.small, color = colors.ink2, maxLines = 1) },
                    onClick = if (reorder.ordering) null else ({ actions.onLoose(m) }),
                    indent = false,
                    divider = false,
                )
            }
        }
        FoldRevealValue(ui.lockLine) { line ->
            LockRow(title = line, actionLabel = WorldCopy.UNLOCK, onAction = actions.onUnlock, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun ReorderRowScope.FactionBox(
    doc: ProjectDoc?,
    f: FactionRowUi,
    open: Boolean,
    onExpand: (Key, Boolean) -> Unit,
    reorder: ReorderState,
    editable: Boolean,
    actions: AffiliationActionsUi,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val turn = animatedFraction(open, label = "factionChevron")
    Column(Modifier.fillMaxWidth().topHairline(colors.line)) {
        Row(
            press
                .fillMaxWidth()
                .then(if (!ordering) Modifier.pressable(PressAction.Click({ actions.onFaction(f) }), role = Role.Button, scaleOnPress = false) else Modifier)
                .heightIn(min = 60.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Handle()
            if (f.expandable) {
                Box(
                    Modifier
                        .offset(x = (-12).dp)
                        .size(44.dp)
                        .pressable(PressAction.Click({ onExpand(f.key, !open) }), role = Role.Button, scaleOnPress = false)
                        .semantics { contentDescription = PlaceCopy.expandLabel(f.name, open) },
                    contentAlignment = Alignment.Center,
                ) {
                    CivilightIcon(IconId.Next, contentDescription = null, tint = colors.ink2, modifier = Modifier.graphicsLayer { rotationZ = 90f * turn.value })
                }
            } else {
                Spacer(Modifier.width(32.dp))
            }
            Column(Modifier.weight(1f).offset(x = if (f.expandable) (-12).dp else 0.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                RefText(f.name, type.itemTitle.merge(TextStyle(fontWeight = FontWeight.SemiBold)), f.deleted)
                f.base?.let { RefText(it, type.small, f.baseDeleted, color = colors.ink3) }
            }
            f.count?.let { CivilightText(it, type.label, color = colors.ink2, maxLines = 1) }
        }
        FoldReveal(open && f.expandable) {
            ReorderList(
                state = reorder,
                list = AffiliationUi.membersList(f.key),
                keys = f.members.map { it.key },
                labels = PlaceCopy.REORDER_LABELS,
                enabled = editable,
                onMove = { k, to -> actions.onMoveMember(f.key, k, to) },
            ) { key, _ ->
                val m = f.members.first { it.key == key }
                PersonRow(
                    doc = doc,
                    key = m.key,
                    name = m.name,
                    role = m.role,
                    deleted = m.deleted,
                    trailing = { m.multi?.let { MultiMark(it) } },
                    onClick = if (reorder.ordering) null else ({ actions.onMember(m) }),
                    indent = true,
                    divider = true,
                )
            }
        }
    }
}

/**
 * A person row (board `mem` / `memLoose`): grip (picked row only), avatar 28, name 14 · one-liner 12, trailing;
 * members are indented 36 under their faction. The row's press area is the scope's [ReorderRowScope.press].
 */
@Composable
fun ReorderRowScope.PersonRow(
    doc: ProjectDoc?,
    key: Key,
    name: String,
    role: String?,
    deleted: Boolean,
    trailing: @Composable () -> Unit,
    onClick: (() -> Unit)?,
    indent: Boolean,
    divider: Boolean,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        press
            .fillMaxWidth()
            .then(if (divider) Modifier.topHairline(colors.line) else Modifier)
            .then(if (onClick != null) Modifier.pressable(PressAction.Click(onClick), role = Role.Button, scaleOnPress = false) else Modifier)
            .heightIn(min = 52.dp)
            .padding(start = if (indent) 36.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Handle()
        Avatar(name, size = 28.dp, image = rememberCharacterAvatarPainter(doc, key))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            RefText(name, type.itemTitleCompact, deleted)
            if (role != null) CivilightText(role, type.small, color = colors.ink3, maxLines = 1)
        }
        trailing()
    }
}

/** `2곳` (board `multi`: 11 medium `ink2` on a `tint` pill). */
@Composable
private fun MultiMark(text: String) {
    val colors = CivilightTheme.colors
    CivilightText(
        text,
        CivilightTheme.type.micro,
        Modifier.background(colors.tint, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 1.dp),
        color = colors.ink2,
        maxLines = 1,
    )
}
