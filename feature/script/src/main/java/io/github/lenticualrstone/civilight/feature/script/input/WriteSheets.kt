package io.github.lenticualrstone.civilight.feature.script.input

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxMark
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.SearchField
import io.github.lenticualrstone.civilight.designsystem.component.SearchHit
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.component.selectedRowBackground
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.actions.VersionActions
import io.github.lenticualrstone.civilight.domain.script.KindChange
import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.domain.script.Manuscript
import io.github.lenticualrstone.civilight.domain.script.Particles
import io.github.lenticualrstone.civilight.domain.script.Replace
import io.github.lenticualrstone.civilight.domain.script.VersionDiff
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.marks.Marks
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import kotlinx.serialization.Serializable

// (0.2.0) Feature-local sheets of the writing flow (shell.md §5: keys are saved with the screen).

/** The kind menu (SCR-12): from the chip ([blockKey] null) or the swipe `종류` / `속성` (converts at once). */
@Serializable
data class KindMenuSheetKey(val projectKey: Key, val blockKey: Key? = null, val editKey: Key? = null) : SheetKey

/** The speaker picker for the speaker chip (SCR-18, SCR-19). */
@Serializable
data class ChipSpeakerSheetKey(val projectKey: Key, val sceneKey: Key? = null) : SheetKey

/** `@` for the draft (SCR-16, SCR-55). */
@Serializable
data class DraftLinkSheetKey(val projectKey: Key) : SheetKey

/** `찾기` (SCR-68) and its `바꾸기` mode (SCR-75). */
@Serializable
data class FindSheetKey(val projectKey: Key, val replace: Boolean = false, val query: String = "") : SheetKey

/** `지난 판` with `판 남기기` (SCR-76). */
@Serializable
data class VersionsSheetKey(val projectKey: Key, val unitKey: Key) : SheetKey

/** One version compared with the episode (SCR-76). */
@Serializable
data class VersionCompareSheetKey(val projectKey: Key, val unitKey: Key, val versionKey: Key) : SheetKey

/** `복선 심기` on a block (SCR-77). */
@Serializable
data class PlantSheetKey(val projectKey: Key, val blockKey: Key) : SheetKey

/** `다시 언급 · 거두기` on a block (SCR-77). */
@Serializable
data class MarkPickSheetKey(val projectKey: Key, val blockKey: Key) : SheetKey

/** One row of the kind menu: the kind, its subline (or the reason it is off) and whether it is the current one. */
data class KindMenuRow(val kind: BlockKind, val name: String, val sub: String?, val current: Boolean, val enabled: Boolean)

object KindMenu {
    private val SUBS = mapOf(
        BlockKind.DIALOGUE to null,
        BlockKind.NARRATION to WriteCopy.SUB_NARRATION,
        BlockKind.DIRECTION to WriteCopy.SUB_DIRECTION,
        BlockKind.CHOICE to WriteCopy.SUB_CHOICE,
        BlockKind.SYSTEM to WriteCopy.SUB_SYSTEM,
        BlockKind.MEMO to WriteCopy.SUB_MEMO,
    )

    /**
     * Rows in menu order. [block] (swipe or `속성`) dims what [KindChange.check] blocks; [editBlock] (the chip
     * in `고치기`) dims only `큐 있음`, since the send rule checks choice lines there.
     */
    fun rows(
        current: BlockKind,
        block: io.github.lenticualrstone.civilight.model.script.Block?,
        editBlock: io.github.lenticualrstone.civilight.model.script.Block?,
        gameTools: Boolean = true,
    ): List<KindMenuRow> =
        SUBS.map { (kind, sub) ->
            val blocked = when {
                block != null -> KindChange.check(block, kind)
                editBlock != null -> KindChange.check(editBlock, kind)?.takeIf { it == KindChange.Blocked.HAS_CUES }
                else -> null
            }
            // G-2: with `게임 제작 도구` off, cues are named nowhere; the block still keeps its kind (scope §8 rule 1).
            val reason = blocked?.let { if (!gameTools && it == KindChange.Blocked.HAS_CUES) WriteCopy.KIND_KEEPS_DATA else WriteCopy.kindReason(it) }
            KindMenuRow(kind, Drafts.inputKind(kind).label, reason ?: sub, kind == current, blocked == null)
        }
}

@Composable
fun SheetScope.KindMenuSheet(rows: List<KindMenuRow>, onPick: (BlockKind) -> Unit) {
    val sheet = this
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    SheetHeader(title = io.github.lenticualrstone.civilight.designsystem.component.ScriptInputCopy.KIND, onClose = ::close)
    SheetScrollBody(Modifier.padding(top = 8.dp)) {
        rows.forEach { row ->
            val name = if (row.enabled) row.name else "${row.name} · ${WriteCopy.OFF} · ${row.sub.orEmpty()}"
            Row(
                Modifier
                    .fillMaxWidth()
                    .topHairline(colors.line)
                    .selectedRowBackground(row.current, colors.sel, colors.acc)
                    .pressable(PressAction.Click({
                        sheet.close()
                        if (!row.current) onPick(row.kind)
                    }), enabled = row.enabled, role = Role.Button, scaleOnPress = false)
                    .heightIn(min = 44.dp)
                    .padding(horizontal = 14.dp, vertical = 11.dp)
                    .semantics { contentDescription = name },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CivilightText(row.name, type.itemTitle, color = if (row.enabled) colors.ink else colors.ink3, maxLines = 1)
                if (row.sub != null) CivilightText(row.sub, type.small, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
                else Row(Modifier.weight(1f)) {}
                // The chosen row: the DS check icon (decorative; the row reads as selected by its fill).
                if (row.current) io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon(
                    io.github.lenticualrstone.civilight.designsystem.icon.IconId.Check, null, size = 16.dp, tint = colors.acc,
                )
            }
        }
    }
}

/** A hit's location line: `1-7 씬 3 · #044 하르트` (a locked hidden speaker reads `숨긴 인물`). */
fun hitLabel(doc: ProjectDoc, hit: Replace.Hit, unlocked: Boolean): String {
    val block = doc.index.block(hit.blockKey) ?: return ""
    val episode = Labels.episodeCodeOfScene(doc, block.sceneKey)
    val scene = Labels.sceneOrdinal(doc, block.sceneKey)?.let(WriteCopy::sceneLabel).orEmpty()
    val who = when (val l = Manuscript.speakerLabel(doc, block, unlocked)) {
        is Manuscript.SpeakerLabel.Named -> l.name
        is Manuscript.SpeakerLabel.Deleted -> l.text
        Manuscript.SpeakerLabel.Hidden -> Manuscript.SpeakerLabel.Hidden.TEXT
        Manuscript.SpeakerLabel.Missing -> Manuscript.SpeakerLabel.Missing.TEXT
        null -> Labels.kindLabel(block.kind.known)
    }
    return "$episode $scene · ${Labels.number(block.number)} $who".replace("  ", " ")
}

/** What the find sheet does on a tap. */
class FindActions(
    val jumpTo: (blockKey: Key) -> Unit,
    val command: (text: String) -> Unit,
    val replace: (hits: List<Replace.Hit>, query: String, with: String) -> Unit,
)

/**
 * `찾기` (SCR-68): the query field, this episode's block hits (whole sentence, match boxed) and the matching
 * commands; `바꾸기` switches to replace mode (SCR-75): `찾을 말` · `바꿀 말`, one check per hit (empty at first),
 * `모두 고르기` and `바꾸기 · k`; a hit before a particle whose 받침 would change carries `◇ 조사 확인`.
 */
@Composable
fun SheetScope.FindSheet(
    key: FindSheetKey,
    doc: ProjectDoc,
    unitKey: Key,
    unlocked: Boolean,
    readOnly: Boolean,
    commands: List<String>,
    actions: FindActions,
) {
    val sheet = this
    var replaceMode by rememberSaveable { mutableStateOf(key.replace && !readOnly) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(key.query, TextRange(key.query.length))) }
    var with by rememberSaveable { mutableStateOf("") }
    // Replace mode keeps its list after a `바꾸기` ([frozen], remapped to the new offsets); checks are by index in it.
    var checked by remember { mutableStateOf(setOf<Int>()) }
    var done by remember { mutableStateOf(setOf<Int>()) }
    var frozen by remember { mutableStateOf<List<Replace.Hit>?>(null) }
    val q = query.text
    // `되돌리기` (or any change) that puts the kept list out of step with the document drops it: the list is found again.
    androidx.compose.runtime.LaunchedEffect(doc) {
        val kept = frozen ?: return@LaunchedEffect
        if (kept.withIndex().any { (i, h) -> i !in done && !Replace.matches(doc, h, q) }) {
            frozen = null; done = emptySet(); checked = emptySet()
        }
    }
    val allHits = remember(doc, q, unlocked) { Replace.find(doc, q, unlocked) }
    val hits = if (replaceMode) frozen ?: allHits else allHits.filter { doc.index.numberingUnitKeyOfScene(doc.index.block(it.blockKey)?.sceneKey ?: "") == unitKey }
    Row(verticalAlignment = Alignment.CenterVertically) {
        SearchField(
            value = query,
            onValueChange = {
                query = it
                checked = emptySet(); done = emptySet(); frozen = null
            },
            modifier = Modifier.weight(1f),
            placeholder = if (replaceMode) WriteCopy.REPLACE_FIND else WriteCopy.FIND_FIELD,
            onClose = sheet::close,
            closeLabel = WriteCopy.CLOSE,
        )
        if (!readOnly && !replaceMode) TextAction(WriteCopy.REPLACE, { replaceMode = true })
    }
    if (!replaceMode) CivilightText(WriteCopy.FIND_HINT, CivilightTheme.type.small, Modifier.padding(top = 6.dp), color = CivilightTheme.colors.ink3)
    if (replaceMode) {
        CivilightTextField(with, { with = it }, Modifier.padding(top = 8.dp), label = WriteCopy.REPLACE_WITH)
    }
    SheetScrollBody(Modifier.padding(top = 8.dp)) {
        if (!replaceMode) {
            val matching = commands.filter { q.isBlank() || it.contains(q) || io.github.lenticualrstone.civilight.ui.search.KoreanSearch.matches(it, q) }
            if (matching.isNotEmpty()) {
                SectionLabel(WriteCopy.COMMANDS, Modifier.padding(top = 8.dp))
                matching.forEach { c ->
                    ListRow(title = WriteCopy.commandRow(c), compact = true, onClick = {
                        sheet.close()
                        actions.command(c)
                    })
                }
            }
        }
        if (q.isNotEmpty()) {
            // D19: replace mode covers the whole project; find lists this episode.
            SectionLabel(if (replaceMode) WriteCopy.projectBlocksSection(hits.size) else WriteCopy.blocksSection(hits.size), Modifier.padding(top = 12.dp))
            if (hits.isEmpty()) CivilightText(WriteCopy.NO_HITS, CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
            hits.forEachIndexed { index, hit ->
                val range = hit.sentenceStart until (hit.sentenceStart + (hit.end - hit.start)).coerceAtMost(hit.sentence.length)
                val particle = replaceMode && Particles.needsCheck(q, with, hit.following)
                if (replaceMode) {
                    Row(verticalAlignment = Alignment.Top) {
                        val isDone = index in done
                        CheckboxMark(
                            checked = index in checked || isDone,
                            modifier = Modifier.padding(top = 12.dp),
                            onCheckedChange = if (isDone) null else ({ checked = if (index in checked) checked - index else checked + index }),
                        )
                        SearchHit(
                            location = hitLabel(doc, hit, unlocked),
                            sentence = hit.sentence,
                            matches = listOf(range),
                            modifier = Modifier.weight(1f),
                            trailingWarning = WriteCopy.PARTICLE_CHECK.takeIf { particle },
                            animate = false,
                        )
                    }
                } else {
                    SearchHit(
                        location = hitLabel(doc, hit, unlocked),
                        sentence = hit.sentence,
                        matches = listOf(range),
                        animate = false,
                        onClick = {
                            sheet.close()
                            actions.jumpTo(hit.blockKey)
                        },
                    )
                }
            }
        }
    }
    if (replaceMode) {
        val k = checked.size
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextAction(WriteCopy.CHECK_ALL, { checked = hits.indices.filter { it !in done }.toSet() })
            Row(Modifier.weight(1f)) {}
            CivilightButton(
                WriteCopy.replaceButton(k),
                onClick = {
                    val picked = hits.filterIndexed { i, _ -> i in checked }
                    // Later hits in the same runs move by the length change, so the next 바꾸기 hits the right places.
                    frozen = Replace.remap(hits, checked, q, with)
                    done = done + checked
                    checked = emptySet()
                    actions.replace(picked, q, with)
                },
                enabled = k > 0 && q.isNotEmpty(),
            )
        }
    }
}

/** `지난 판` (SCR-76): `판 남기기` with its default name, then the versions, newest first. */
@Composable
fun SheetScope.VersionsSheet(
    key: VersionsSheetKey,
    doc: ProjectDoc,
    readOnly: Boolean,
    defaultName: String,
    onKeep: (String) -> Unit,
) {
    val sheet = this
    var name by rememberSaveable { mutableStateOf(defaultName) }
    SheetHeader(title = WriteCopy.VERSIONS_TITLE, onClose = ::close, subtitle = Labels.unitCode(doc, key.unitKey))
    if (!readOnly) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CivilightTextField(name, { name = it }, Modifier.weight(1f), label = WriteCopy.VERSION_NAME)
            CivilightButton(WriteCopy.KEEP_VERSION, onClick = { onKeep(name); name = defaultName }, variant = ButtonVariant.COMPACT_PRIMARY)
        }
    }
    SheetScrollBody(Modifier.padding(top = 8.dp)) {
        val versions = VersionActions.versionsOf(doc, key.unitKey).reversed()
        if (versions.isEmpty()) CivilightText(WriteCopy.NO_VERSIONS, CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
        versions.forEach { v ->
            ListRow(title = v.name, subtitle = WriteCopy.versionBlocks(v.blocks.size), compact = true, onClick = {
                sheet.open(VersionCompareSheetKey(key.projectKey, key.unitKey, v.key))
            })
        }
    }
}

/**
 * The compare screen (SCR-76): one column; changed characters boxed, deleted spans struck in `ink3`; `이 판 글로`
 * per changed block. A locked hidden scene is one row with no text of it.
 */
@Composable
fun SheetScope.VersionCompareSheet(
    key: VersionCompareSheetKey,
    doc: ProjectDoc,
    unlocked: Boolean,
    readOnly: Boolean,
    onUnlock: () -> Unit,
    onRestore: (blockKey: Key) -> Unit,
) {
    val sheet = this
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val version = VersionActions.versionsOf(doc, key.unitKey).firstOrNull { it.key == key.versionKey }
    SheetHeader(title = version?.name.orEmpty(), onClose = ::close, subtitle = Labels.unitCode(doc, key.unitKey))
    version ?: return
    val rows = remember(doc, version, unlocked) { VersionDiff.rows(version, doc, key.unitKey, unlocked) }
    val boxed = SpanStyle(background = colors.sel, color = colors.ink)
    val struck = SpanStyle(color = colors.ink3, textDecoration = TextDecoration.LineThrough)
    SheetScrollBody(Modifier.padding(top = 8.dp)) {
        rows.forEach { row ->
            Column(Modifier.fillMaxWidth().topHairline(colors.line).padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                when (row) {
                    is VersionDiff.Row.Same -> {
                        CivilightText(Labels.number(row.number), type.number, color = colors.ink3, maxLines = 1)
                        CivilightText(row.text, type.body, color = colors.ink2)
                    }
                    is VersionDiff.Row.Changed -> {
                        CivilightText(Labels.number(row.number), type.number, color = colors.ink3, maxLines = 1)
                        val text = AnnotatedString.Builder().apply {
                            row.segments.forEach { s ->
                                when (s.kind) {
                                    VersionDiff.SegKind.SAME -> append(s.text)
                                    VersionDiff.SegKind.INSERTED -> { pushStyle(boxed); append(s.text); pop() }
                                    VersionDiff.SegKind.DELETED -> { pushStyle(struck); append(s.text); pop() }
                                }
                            }
                        }.toAnnotatedString()
                        RichText(text, type.body, colors.ink)
                        if (!readOnly && VersionActions.canRestore(doc, row.versionBlock)) {
                            TextAction(WriteCopy.TO_THIS_VERSION, { onRestore(row.blockKey) })
                        }
                    }
                    is VersionDiff.Row.Deleted -> {
                        CivilightText(Labels.number(row.number), type.number, color = colors.ink3, maxLines = 1)
                        RichText(AnnotatedString(row.text, listOf(AnnotatedString.Range(struck, 0, row.text.length))), type.body, colors.ink3)
                    }
                    is VersionDiff.Row.Added -> {
                        CivilightText(Labels.number(row.number), type.number, color = colors.ink3, maxLines = 1)
                        RichText(AnnotatedString(row.text, listOf(AnnotatedString.Range(boxed, 0, row.text.length))), type.body, colors.ink)
                    }
                    is VersionDiff.Row.HiddenScene -> ListRow(title = WriteCopy.hiddenSceneRow(row.count), compact = true, onClick = onUnlock)
                }
            }
        }
    }
}

/** `복선 심기` (SCR-77): `이름` (required) and an optional `거둘 곳` episode. */
@Composable
fun SheetScope.PlantSheet(key: PlantSheetKey, doc: ProjectDoc, onPlant: (name: String, payoff: Key?) -> Unit) {
    val sheet = this
    var name by rememberSaveable { mutableStateOf("") }
    var payoff by rememberSaveable { mutableStateOf<String?>(null) }
    val block = doc.index.block(key.blockKey)
    SheetHeader(title = WriteCopy.PLANT, onClose = ::close, subtitle = block?.let { Labels.number(it.number) })
    CivilightTextField(name, { name = it }, Modifier.padding(top = 8.dp), label = WriteCopy.MARK_NAME)
    SectionLabel(WriteCopy.PAYOFF_AT, Modifier.padding(top = 12.dp))
    SheetScrollBody {
        val level = doc.project.structure.episodeLevel
        val episodes = if (level == null) emptyList() else doc.units.filter { it.level == level && doc.manuscript.isLiveUnit(it.key) }
            .sortedBy { Labels.ordinalPath(doc, it.key).joinToString(".") { n -> n.toString().padStart(4, '0') } }
        RadioRow(WriteCopy.PAYOFF_NONE, payoff == null, { payoff = null })
        episodes.forEach { e -> RadioRow(Labels.unitCode(doc, e.key), payoff == e.key, { payoff = e.key }) }
    }
    CivilightButton(
        WriteCopy.PLANT_CONFIRM,
        onClick = {
            sheet.close()
            onPlant(name.trim(), payoff)
        },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        enabled = name.isNotBlank(),
    )
}

/** `다시 언급 · 거두기` (SCR-77): the project's foreshadowings, open ones first; each with the two actions. */
@Composable
fun SheetScope.MarkPickSheet(
    key: MarkPickSheetKey,
    doc: ProjectDoc,
    unlocked: Boolean,
    onMention: (Key) -> Unit,
    onPayoff: (Key) -> Unit,
) {
    val sheet = this
    SheetHeader(title = WriteCopy.MENTION_OR_PAYOFF, onClose = ::close)
    SheetScrollBody(Modifier.padding(top = 8.dp)) {
        val items = Marks.items(doc).filterIsInstance<io.github.lenticualrstone.civilight.model.marks.MarkItem.Plant>()
            .filter { visible(doc, it.foreshadow.plant.blockKey, unlocked) }
            .sortedBy { if (it.foreshadow.payoff == null) 0 else 1 }
        if (items.isEmpty()) CivilightText(WriteCopy.NO_MARKS, CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
        items.forEach { item ->
            val f = item.foreshadow
            ListRow(
                title = f.name,
                subtitle = if (f.payoff == null) WriteCopy.OPEN else WriteCopy.PAID_OFF,
                compact = true,
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextAction(WriteCopy.MENTION, { sheet.close(); onMention(f.key) })
                        TextAction(WriteCopy.PAYOFF, { sheet.close(); onPayoff(f.key) })
                    }
                },
            )
        }
    }
}

/** A mark whose plant sits in a locked hidden scene is left out (rule 19). */
fun visible(doc: ProjectDoc, blockKey: Key, unlocked: Boolean): Boolean {
    if (unlocked) return true
    val scene = doc.index.block(blockKey)?.sceneKey?.let { doc.index.unit(it) } ?: return true
    return !scene.hidden
}

@Composable
private fun RichText(text: AnnotatedString, style: androidx.compose.ui.text.TextStyle, color: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.text.BasicText(text, style = style.merge(androidx.compose.ui.text.TextStyle(color = color)))
}
