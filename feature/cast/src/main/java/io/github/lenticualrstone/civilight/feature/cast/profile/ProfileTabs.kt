package io.github.lenticualrstone.civilight.feature.cast.profile

import io.github.lenticualrstone.civilight.designsystem.component.HoloSurface
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton

import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import io.github.lenticualrstone.civilight.designsystem.card.CardKind
import io.github.lenticualrstone.civilight.designsystem.card.CardMetrics
import io.github.lenticualrstone.civilight.designsystem.card.SnapCardRow
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.feature.cast.common.bleed
import io.github.lenticualrstone.civilight.feature.cast.gallery.GallerySection
import io.github.lenticualrstone.civilight.feature.cast.image.rememberSlotPainter
import io.github.lenticualrstone.civilight.ui.gallery.GalleryCard
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.AddChip
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.designsystem.theme.ValueSwap
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor
import io.github.lenticualrstone.civilight.designsystem.component.Avatar
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationDrafts
import io.github.lenticualrstone.civilight.designsystem.component.AvatarSize
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.ProgressBar
import io.github.lenticualrstone.civilight.designsystem.component.RowDivider
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.text.DecorationRange
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.text.InlineDecoration
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.field.FieldRow
import io.github.lenticualrstone.civilight.domain.cast.relation.DeletedPartnerRow
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationBlock
import io.github.lenticualrstone.civilight.domain.cast.relation.StepStatus
import io.github.lenticualrstone.civilight.domain.cast.voice.VoiceLineRow
import io.github.lenticualrstone.civilight.domain.cast.voice.ArchiveRow
import io.github.lenticualrstone.civilight.domain.cast.relation.AddressChip
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import androidx.compose.ui.semantics.Role
import io.github.lenticualrstone.civilight.domain.cast.voice.VoiceLineState
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.docText
import androidx.compose.ui.text.AnnotatedString

/** What the record's rows open (sheets of the record screen). */
class RecordActions(
    val editable: Boolean,
    val onField: (defKey: Key) -> Unit,
    val onFieldHistory: (defKey: Key) -> Unit,
    val onFixed: (FixedField) -> Unit,
    val onDisplayNameHistory: () -> Unit,
    val onToggleEmpty: () -> Unit,
    val onAddField: () -> Unit,
    val onMap: () -> Unit,
    val onAddRelation: () -> Unit,
    /** A partner block: the pair's relation sheet (CST-29). */
    val onRelation: (partnerKey: Key) -> Unit,
    /** `되살리기` on a deleted partner's dotted row (CST-43). */
    val onRestorePartner: (partnerKey: Key) -> Unit,
    /** `이대로 두기` on a deleted partner's dotted row (CST-41). */
    val onKeepPartner: (partnerKey: Key, relationKey: Key) -> Unit,
    /** The `말투` prose (CST-15). */
    val onEditSpeech: () -> Unit = {},
    /** A 호칭 chip: that relation's 호칭 slot (CST-15). */
    val onAddress: (partnerKey: Key) -> Unit = {},
    /** A voice line's sheet (CST-16). */
    val onVoiceLine: (lineKey: Key) -> Unit = {},
    /** The state label of a voice line: `쓰는 중 ↔ 녹음 대본 완료` (CST-16). */
    val onToggleVoice: (lineKey: Key, label: String) -> Unit = { _, _ -> },
    /** An archive document's editor; null = a new one (CST-17). */
    val onArchive: (docKey: Key?) -> Unit = {},
    /** `+ 호칭` on `말투`: partner picker, then that pair's 호칭 slot (CST-31). */
    val onAddAddress: () -> Unit = {},
    /** The `비밀` prose (CST-18). */
    val onEditSecret: () -> Unit = {},
    /** A deleted partner's dotted name: that item's check row (CST-42 bulk changes, CST-43). */
    val onDeletedPartner: (partnerKey: Key) -> Unit = {},
    /** `레코더 · 정리 안 됨 n ›`: the recorder inbox for this character (CST-49). */
    val onRecorder: () -> Unit = {},
    /** `합치기` on the `◇ 같은 이름` row (CST-46): the merge sheet's picker. */
    val onSameNameMerge: () -> Unit = {},
    /** `넣기` of the `갤러리 · N` section (GAL-01): the add sheet for this character. */
    val onGalleryAdd: () -> Unit = {},
    /** `전체 보기 ›` (GAL-01): the character's gallery screen. */
    val onGalleryAll: () -> Unit = {},
    /** A small gallery card (GAL-01): the gallery screen, in its remembered view, with that item picked. */
    val onGalleryCard: (itemKey: Key) -> Unit = {},
)

/** A field row of the header table or the `항목` list with its planned row. */
@Composable
fun FieldRowItem(doc: ProjectDoc, row: FieldRow, actions: RecordActions, modifier: Modifier = Modifier) {
    val def = row.defKey?.let(doc.index::fieldDef)
    val numeric = def == null || def.type.known == FieldType.NUMBER || def.type.known == FieldType.CALC
    val key = row.defKey
    RecordFieldRow(
        label = row.label,
        value = row.value,
        modifier = modifier,
        since = row.since,
        hasHistory = row.hasHistory,
        numeric = numeric,
        mark = row.characterOnly,
        onClick = key?.let { { actions.onField(it) } },
        onHistory = key?.let { { actions.onFieldHistory(it) } },
    )
    row.planned?.let { (point, value) -> PlannedRow(point, value) }
}

/**
 * `개요` (CST-08): fixed rows and the `항목` list, `빈 항목 N · 보기 | + 항목`, the gallery section (GAL-01),
 * then the `긴 글` sections. [full] is the same record with every row `보기` can bring in, so a row that
 * `보기` adds or removes folds in and out instead of jumping (P2); [state] says which rows show now.
 */
@Composable
fun OverviewTab(
    doc: ProjectDoc,
    state: ProfileState,
    full: ProfileState,
    showEmpty: Boolean,
    gallery: GallerySection,
    inner: Boolean,
    actions: RecordActions,
    recorderLink: String? = null,
    appearance: @Composable () -> Unit = {},
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shownFixed = remember(state) { state.fixedRows.mapTo(HashSet()) { it.field } }
    val shownItems = remember(state) { state.overview.items.mapTo(HashSet()) { it.defKey } }
    Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        // CST-46: `◇ 같은 이름 2 · 노아` with `합치기` (exact same name only).
        FoldRevealValue(state.sameName) { notice ->
            ListRow(
                notice,
                compact = true,
                divider = RowDivider.DASHED,
                trailing = if (actions.editable) ({ TextAction(CastUiCopy.BTN_MERGE, actions.onSameNameMerge, color = colors.acc, style = type.label) }) else null,
            )
        }
        for (row in full.fixedRows) {
            key(row.field) {
                FoldReveal(showEmpty || row.field in shownFixed) {
                    Column {
                        RecordFieldRow(
                            label = row.label,
                            value = row.value,
                            since = row.since,
                            hasHistory = row.hasHistory,
                            onClick = { actions.onFixed(row.field) },
                            onHistory = if (row.field == FixedField.DISPLAY_NAME) actions.onDisplayNameHistory else null,
                        )
                        row.planned?.let { (point, value) -> PlannedRow(point, value) }
                    }
                }
            }
        }
        // CST-08 1a, CST-71: appearance bars and `마지막 등장` directly under the field table, before the `항목` list.
        appearance()
        for (row in full.overview.items) {
            key(row.defKey) { FoldReveal(showEmpty || row.defKey in shownItems) { FieldRowItem(doc, row, actions) } }
        }
        for (row in full.emptyLongRows) {
            key(row.defKey) { FoldReveal(showEmpty) { FieldRowItem(doc, row, actions) } }
        }
        Row(Modifier.fillMaxWidth().topHairline(colors.line), verticalAlignment = Alignment.CenterVertically) {
            TextAction(state.emptyLabel, actions.onToggleEmpty, color = if (showEmpty) colors.acc else colors.ink3, style = type.small, modifier = Modifier.padding(start = 0.dp))
            Spacer(Modifier.weight(1f))
            // Icon list `＋ 항목` (class b): icon `plus` + `항목`, read as `항목 넣기`.
            if (actions.editable) CivilightIconButton(IconButtonSpec.ADD_FIELD, actions.onAddField)
        }
        GallerySectionView(gallery, inner, actions)
        // CST-49: `레코더 · 정리 안 됨 1 ›` right of the first section header (or on its own line without one).
        FoldRevealValue(recorderLink.takeIf { state.overview.sections.isEmpty() }) { link ->
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.End) {
                TextAction(link, actions.onRecorder, color = colors.acc, style = type.small)
            }
        }
        state.overview.sections.forEachIndexed { index, section ->
            val link = recorderLink.takeIf { index == 0 }
            SectionLabel("${section.numeral} ${section.title}", Modifier.padding(top = 22.dp), trailing = link, onTrailing = link?.let { actions.onRecorder })
            CivilightText(
                section.body.docText,
                type.body,
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .pressable(onClick = { actions.onField(section.defKey) }, scaleOnPress = false, onClickLabel = section.title),
                color = colors.ink,
            )
        }
    }
}

/**
 * `갤러리 · 8` (GAL-01; board `GalProfile`): `넣기` and `전체 보기 ›` in the header, then one snapping row of small
 * gallery cards (132 × 176, pitch 146, at most 12; the shared `GalleryCard` without `⋯`). The row runs to the
 * screen edge so the third card peeks. An empty gallery shows one outlined card instead and no `전체 보기 ›`.
 * Tapping a card opens the gallery screen with that item picked; the screen reads its own remembered view.
 */
@Composable
private fun GallerySectionView(section: GallerySection, inner: Boolean, actions: RecordActions) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(Modifier.fillMaxWidth().padding(top = 22.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(section.header, Modifier.weight(1f))
            if (actions.editable) {
                Row(
                    Modifier.pressable(onClick = actions.onGalleryAdd, scaleOnPress = false, hitTarget = HitTarget.BUTTON, onClickLabel = CastUiCopy.BTN_INSERT).heightIn(min = 44.dp).padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CivilightIcon(IconId.Plus, contentDescription = null, size = 16.dp, tint = colors.ink2)
                    CivilightText(CastUiCopy.BTN_INSERT, type.label, color = colors.ink2, maxLines = 1)
                }
            }
            if (section.showAll) TextAction(CastUiCopy.BTN_GALLERY_ALL, actions.onGalleryAll, color = colors.acc, style = type.label)
        }
        if (section.empty) {
            if (actions.editable) EmptyGalleryCard(actions.onGalleryAdd)
        } else {
            val inset = CardMetrics.inset(inner)
            Box(Modifier.fillMaxWidth().bleed(PROFILE_SIDE).padding(start = (PROFILE_SIDE.value - inset).coerceAtLeast(0f).dp)) {
                SnapCardRow(
                    items = section.cards,
                    key = { it.itemKey },
                    kind = CardKind.PORTRAIT_SMALL,
                    inner = inner,
                    showPager = false,
                ) { card, imageModifier ->
                    val painter = rememberSlotPainter(card.image)
                    GalleryCard(
                        title = card.title,
                        meta = card.meta,
                        chip = card.chip,
                        inner = inner,
                        onClick = { actions.onGalleryCard(card.itemKey) },
                        modifier = Modifier.size(GALLERY_CARD_W, GALLERY_CARD_H),
                        small = true,
                    ) {
                        if (painter != null) {
                            // The chip's `홀로` always comes with the foil itself (CNV-30).
                            HoloSurface(on = card.image.holo, modifier = Modifier.matchParentSize()) {
                                Image(painter, contentDescription = null, modifier = imageModifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                        } else {
                            Box(Modifier.fillMaxSize().background(colors.tint))
                        }
                    }
                }
            }
        }
    }
}

/** The small cards of the section: 132 × 176 (3:4), pitch 146 on the cover. */
private val GALLERY_CARD_W = 132.dp
private val GALLERY_CARD_H = 176.dp

/** The profile list's side padding; the gallery row runs past it. */
internal val PROFILE_SIDE = 24.dp

/** One outlined card (1.5 dp `edge`, `+` and `이미지 넣기`) for an empty gallery (GAL-25 look). */
@Composable
private fun EmptyGalleryCard(onAdd: () -> Unit) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .padding(top = 6.dp)
            .size(GALLERY_CARD_W, GALLERY_CARD_H)
            .clip(shape)
            .insetOutline(colors.edge, 1.5.dp, shape)
            .pressable(onClick = onAdd, scaleOnPress = false, shape = shape, onClickLabel = CastUiCopy.GALLERY_EMPTY_ADD),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CivilightIcon(IconId.Plus, contentDescription = null, size = 28.dp, tint = colors.ink2)
        CivilightText(CastUiCopy.GALLERY_EMPTY_ADD, CivilightTheme.type.small, Modifier.padding(top = 8.dp), color = colors.ink2, maxLines = 1)
    }
}

/** `관계` (CST-28; `Relations`): one block per partner from the viewer's side, dotted and hidden rows, `+ 관계`. */
@Composable
fun RelationsTab(state: ProfileState, actions: RecordActions, doc: ProjectDoc? = null) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val tab = state.relations
    Column(Modifier.fillMaxWidth().padding(top = 22.dp)) {
        SectionLabel(tab.header, trailing = tab.mapLink, onTrailing = actions.onMap, trailingAccent = false)
        Spacer(Modifier.padding(top = 6.dp))
        for (block in tab.blocks) RelationBlockView(block, doc = doc, onClick = if (actions.editable) ({ actions.onRelation(block.partnerKey) }) else null)
        for (row in tab.deletedRows) {
            val relationKey = state.deletedPartnerRelations[row.partnerKey]
            val fixes = actions.editable && relationKey != null && row.partnerKey !in state.keptPartners
            DeletedPartnerView(
                row,
                onOpen = { actions.onDeletedPartner(row.partnerKey) },
                onRestore = if (fixes) ({ actions.onRestorePartner(row.partnerKey) }) else null,
                onKeep = if (fixes) ({ actions.onKeepPartner(row.partnerKey, relationKey) }) else null,
            )
        }
        tab.hiddenRow?.let { text ->
            ListRow(text, compact = true, divider = RowDivider.DASHED)
        }
        if (actions.editable) {
            Row(Modifier.fillMaxWidth().topHairline(colors.line)) {
                TextAction(tab.addLabel, actions.onAddRelation, color = colors.ink2, style = type.label)
            }
        }
    }
}

@Composable
private fun RelationBlockView(block: RelationBlock, doc: ProjectDoc?, onClick: (() -> Unit)?) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val portrait = io.github.lenticualrstone.civilight.ui.image.rememberCharacterAvatarPainter(doc, block.partnerKey)
    Column(
        Modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .then(if (onClick != null) Modifier.pressable(onClick = onClick, scaleOnPress = false, onClickLabel = block.title) else Modifier)
            .padding(vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(block.partnerName, size = AvatarSize.List, image = portrait)
            Column(Modifier.weight(1f)) {
                CivilightText(block.title, type.itemTitle, color = colors.ink, maxLines = 1)
                ValueSwap(block.sub, type.small, color = colors.ink3, maxLines = 2)
            }
            if (block.hasRecords) CivilightText(io.github.lenticualrstone.civilight.domain.cast.CastCopy.CHANGE_MARK, type.label, color = colors.acc)
        }
        if (block.steps.isNotEmpty()) {
            Column(Modifier.padding(start = 48.dp, top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val stepStyle = type.small.merge(TextStyle(fontSize = 13.sp, lineHeight = 19.sp))
                for (step in block.steps) {
                    val ink = if (step.status == StepStatus.IN_EFFECT) colors.ink else colors.ink3
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ValueSwap(step.point, type.number.merge(TextStyle(fontSize = 11.sp)), Modifier.width(36.dp), color = ink, maxLines = 1)
                        val body = step.text.removePrefix("${step.point} · ").removePrefix(step.point)
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ValueSwap(body, stepStyle, Modifier.weight(1f, fill = false), color = ink, maxLines = Int.MAX_VALUE)
                            step.statusLabel?.let { CivilightText(CastUiCopy.stepStatus(it), stepStyle, color = colors.acc, maxLines = 1) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dotted row of a partner in the trash: `노아 ◇ 지운 인물 · 관계 1` with `되살리기 · 이대로 두기`
 * (CST-41, CST-43). After `이대로 두기` the dotted row stays without its buttons.
 */
@Composable
private fun DeletedPartnerView(row: DeletedPartnerRow, onOpen: () -> Unit, onRestore: (() -> Unit)?, onKeep: (() -> Unit)?) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(Modifier.fillMaxWidth().topHairline(colors.line, dashed = true)) {
        DeletedPartnerLine(row, Modifier.pressable(onClick = onOpen, scaleOnPress = false, onClickLabel = row.label))
        if (onRestore != null || onKeep != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                onRestore?.let { TextAction(row.actions.getOrElse(0) { CastUiCopy.BTN_RESTORE }, it, color = colors.ink2, style = type.label) }
                onKeep?.let { TextAction(row.actions.getOrElse(1) { CastUiCopy.BTN_KEEP }, it, color = colors.ink2, style = type.label) }
            }
        }
    }
}

@Composable
private fun DeletedPartnerLine(row: DeletedPartnerRow, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DecoratedText(
            AnnotatedString(row.name),
            type.itemTitleCompact,
            color = colors.ink3,
            decorations = listOf(DecorationRange(0, row.name.length, InlineDecoration.DELETED_REF)),
            maxLines = 1,
        )
        CivilightText(row.label, type.small, color = colors.ink3, maxLines = 1)
    }
}

/** A tag (`Tag` style): 30 tall, radius 15, `edge` outline; tappable when [onClick] is set. */
@Composable
private fun TagLabel(text: String, onClick: (() -> Unit)? = null) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(15.dp)
    CivilightText(
        text,
        CivilightTheme.type.chip,
        Modifier
            .then(if (onClick != null) Modifier.pressable(onClick = onClick, hitTarget = HitTarget.CHIP, shape = shape, onClickLabel = text) else Modifier)
            .heightIn(min = 30.dp)
            .insetOutline(colors.edge, 1.dp, shape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = colors.ink2,
        maxLines = 1,
    )
}

/**
 * `말투` (CST-15): the prose (tap to edit with the shared rich editor), then the 호칭 chips read from
 * the relations at the lens; tapping a chip edits that relation's 호칭 slot.
 */
@Composable
fun SpeechTab(
    body: String,
    chips: List<AddressChip>,
    hiddenRow: String?,
    actions: RecordActions,
    summary: String? = null,
    onSummary: (() -> Unit)? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(Modifier.fillMaxWidth().padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // CST-72: the one-line summary first, then the prose and the 호칭 chips.
        SpeechSummaryRow(summary, onSummary)
        SectionLabel(CastUiCopy.SPEECH)
        val edit = if (actions.editable) Modifier.pressable(onClick = actions.onEditSpeech, scaleOnPress = false, onClickLabel = CastUiCopy.SPEECH) else Modifier
        if (body.isNotBlank()) {
            CivilightText(body, type.body, Modifier.fillMaxWidth().then(edit), color = colors.ink)
        } else if (actions.editable) {
            CivilightText(CastUiCopy.EMPTY_VALUE, type.body, Modifier.fillMaxWidth().heightIn(min = 44.dp).then(edit), color = colors.ink3)
        }
        if (chips.isNotEmpty() || actions.editable) {
            ChipFlow {
                chips.forEach { chip -> TagLabel(chip.label, if (actions.editable) ({ actions.onAddress(chip.partnerKey) }) else null) }
                if (actions.editable) AddChip(RelationDrafts.ADDRESS, actions.onAddAddress)
            }
        }
        hiddenRow?.let { CivilightText(it, type.small, color = colors.ink3) }
    }
}

/**
 * `게임 텍스트` (`Voice`; CST-16, CST-17): `보이스 · 18 / 24` with the flat progress bar, voice lines
 * in game order (tap a line to edit it, tap its state to switch `쓰는 중 ↔ 녹음 대본 완료`), then the
 * archive documents (tap to open the editor; `+` adds one).
 */
@Composable
fun GameTextTab(
    progressTitle: String,
    progress: Float,
    orderLabel: String,
    lines: List<VoiceLineRow>,
    archiveTitle: String,
    archive: List<ArchiveRow>,
    actions: RecordActions,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(Modifier.fillMaxWidth().padding(top = 22.dp)) {
        SectionLabel(progressTitle, trailing = orderLabel, trailingAccent = false)
        ProgressBar(progress, Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp))
        for (line in lines) key(line.key) { VoiceLineRowView(line, actions) }
        SectionLabel(
            archiveTitle,
            Modifier.padding(top = 22.dp, bottom = 6.dp),
            trailing = if (actions.editable) CastUiCopy.ADD_MARK else null,
            onTrailing = if (actions.editable) ({ actions.onArchive(null) }) else null,
            trailingAccent = false,
        )
        for (row in archive) ListRow(row.title, subtitle = row.sub, compact = true, onClick = { actions.onArchive(row.key) })
    }
}

/**
 * `비밀` (CST-18, S8 not drawn): the secret fields (shown only here; tap to edit, `◆` opens the
 * history), then the prose under `비밀` (shared rich editor; `⌁` memo paragraphs kept). An empty body
 * reads a tappable `비어 있음`, as on `말투`.
 */
@Composable
fun SecretTab(doc: ProjectDoc, rows: List<FieldRow>, body: String, actions: RecordActions, head: @Composable () -> Unit = {}) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        // CST-73: `이 탭 잠그기` at the tab head.
        head()
        for (row in rows) FieldRowItem(doc, row, actions)
        SectionLabel(CastUiCopy.TABS[4], Modifier.padding(top = 22.dp, bottom = 10.dp))
        val edit = if (actions.editable) Modifier.pressable(onClick = actions.onEditSecret, scaleOnPress = false, onClickLabel = CastUiCopy.TABS[4]) else Modifier
        if (body.isNotBlank()) {
            CivilightText(body, type.body, Modifier.fillMaxWidth().then(edit), color = colors.ink)
        } else if (actions.editable) {
            CivilightText(CastUiCopy.EMPTY_VALUE, type.body, Modifier.fillMaxWidth().heightIn(min = 44.dp).then(edit), color = colors.ink3)
        }
    }
}

/** One voice line: number, title, its state word (tap to switch) and the text. The state colour fades (P4). */
@Composable
private fun VoiceLineRowView(line: VoiceLineRow, actions: RecordActions) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val target = when (line.state) {
        VoiceLineState.DONE -> colors.ink2
        VoiceLineState.WRITING -> colors.acc
        VoiceLineState.EMPTY -> colors.ink3
    }
    val stateColor by animatedColor(target, label = "voiceState")
    val textColor by animatedColor(if (line.state == VoiceLineState.EMPTY) colors.ink3 else colors.ink, label = "voiceText")
    Column(
        Modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .then(if (actions.editable) Modifier.pressable(onClick = { actions.onVoiceLine(line.key) }, scaleOnPress = false, onClickLabel = line.title) else Modifier)
            .padding(vertical = 12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            CivilightText(line.number, type.number.merge(TextStyle(fontSize = 11.sp)), color = colors.ink3)
            CivilightText(line.title, type.label, Modifier.weight(1f), color = colors.ink, maxLines = 1)
            val toggles = actions.editable && line.state != VoiceLineState.EMPTY
            ValueSwap(
                line.state.label,
                type.small,
                if (toggles) {
                    Modifier.pressable(onClick = { actions.onToggleVoice(line.key, line.state.label) }, role = Role.Button, hitTarget = HitTarget.CHIP, scaleOnPress = false, onClickLabel = line.state.label)
                } else {
                    Modifier
                },
                color = stateColor,
                maxLines = 1,
                contentAlignment = Alignment.CenterEnd,
            )
        }
        CivilightText(line.text, type.body, Modifier.padding(top = 4.dp), color = textColor)
    }
}
