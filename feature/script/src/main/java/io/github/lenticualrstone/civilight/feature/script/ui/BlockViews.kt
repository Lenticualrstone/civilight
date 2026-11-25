package io.github.lenticualrstone.civilight.feature.script.ui

import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.SceneHead
import io.github.lenticualrstone.civilight.designsystem.component.selectedRowBackground
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.text.DecorationRange
import io.github.lenticualrstone.civilight.designsystem.text.InlineDecoration
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.feature.script.links.BlockTap
import io.github.lenticualrstone.civilight.feature.script.links.LinkFixes
import io.github.lenticualrstone.civilight.feature.script.terms.TermCopy
import io.github.lenticualrstone.civilight.domain.world.term.TermCandidate
import androidx.compose.foundation.layout.width
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.lenticualrstone.civilight.feature.script.editor.LinkLook
import io.github.lenticualrstone.civilight.feature.script.editor.RunLayout
import io.github.lenticualrstone.civilight.feature.script.editor.SpanKind
import io.github.lenticualrstone.civilight.feature.script.state.BlockRow
import io.github.lenticualrstone.civilight.feature.script.state.EmptySceneRow
import io.github.lenticualrstone.civilight.feature.script.state.HeadRow
import io.github.lenticualrstone.civilight.feature.script.state.HiddenSceneRow
import io.github.lenticualrstone.civilight.feature.script.state.MarkModel
import io.github.lenticualrstone.civilight.feature.script.state.OptionLine
import io.github.lenticualrstone.civilight.feature.script.state.SlotModel
import io.github.lenticualrstone.civilight.feature.script.state.SpeakerLook
import io.github.lenticualrstone.civilight.feature.script.state.SpeakerModel
import io.github.lenticualrstone.civilight.model.script.BlockKind

/** DS decoration of a display span. */
fun SpanKind.decoration(): InlineDecoration = when (this) {
    SpanKind.PAREN -> InlineDecoration.PAREN
    SpanKind.LINK -> InlineDecoration.LINK
    SpanKind.HIDDEN_LINK -> InlineDecoration.HIDDEN_LINK
    SpanKind.DELETED_LINK -> InlineDecoration.DELETED_REF
}

/** The run layout a block row draws (parens per the `괄호 지문` layer). */
fun BlockRow.layout(showParens: Boolean, editing: Boolean = false): RunLayout =
    RunLayout.of(runs, showParens = showParens || editing, linkLook = { linkLooks[it.linkKey] ?: LinkLook(SpanKind.LINK) })

/** Text style of a text block by kind (DS-42; system text mono 13 / 22, memo `dir`). */
@Composable
fun blockTextStyle(kind: BlockKind?): Pair<TextStyle, androidx.compose.ui.graphics.Color> {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    return when (kind) {
        BlockKind.DIALOGUE -> type.line to colors.ink
        BlockKind.SYSTEM -> type.number.merge(TextStyle(fontSize = 13.sp, lineHeight = 22.sp)) to colors.ink2
        BlockKind.MEMO -> type.dir to colors.ink3
        else -> type.body to colors.ink2
    }
}

/**
 * A block as the manuscript draws it when the caret is not in it (SCR-04 … SCR-11): typeset on `bg`
 * with no card; selected = `sel` fill + 2 dp `acc` bar and 12 dp padding. Tapping the text gives the
 * raw offset under the finger ([onTapText]); the speaker line opens the picker ([onSpeaker]).
 */
@Composable
fun BlockView(
    row: BlockRow,
    selected: Boolean,
    showParens: Boolean,
    inheritedHint: Boolean,
    onTapText: (rawOffset: Int?) -> Unit,
    onSpeaker: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onTapOption: ((OptionLine) -> Unit)? = null,
    /** A tap on a link, a candidate or a recorder source (SCR-55, 56, 59); true = handled. */
    onTapTarget: ((BlockTap) -> Boolean)? = null,
    /** The candidate whose small window is open in this block (drawn boxed). */
    openCandidate: TermCandidate? = null,
    /** The small window anchored under the text (SCR-56). */
    popover: (@Composable () -> Unit)? = null,
    /** The margin `▲` (SCR-56): opens the check center's term items. */
    onMarginWarning: (() -> Unit)? = null,
    /** (0.2.0, SCR-04, SCR-21) `고치는 중`, new, `이 뒤에` target and open-swipe looks. */
    look: BlockLook = BlockLook(),
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val numbered = selected || look.numbered
    val deps = io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps.current
    val store = io.github.lenticualrstone.civilight.ui.project.LocalProjectStore.current
    val doc = store?.doc?.collectAsState()?.value
    val settings = deps?.settings?.collectAsState()?.value
    val profileShape = settings?.profileShape ?: io.github.lenticualrstone.civilight.model.app.ProfileShape.SQUARE
    val inner = io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout.current.isInner
    val safeWidth = if (inner) 64.dp else 56.dp
    val portraitSize = if (inner) 48.dp else 44.dp

    val afterLine = if (look.afterTarget) Modifier.drawBehind {
        val y = size.height + 6.dp.toPx()
        drawLine(colors.acc, Offset(8.dp.toPx(), y), Offset(size.width, y), strokeWidth = 2.dp.toPx())
        drawCircle(colors.acc, radius = 4.dp.toPx(), center = Offset(8.dp.toPx(), y))
    } else Modifier
    Box(modifier.then(afterLine)) {
    BlockFrame(selected || look.tag != null, Modifier.pressable(PressAction.Click({ onTapText(null) }), role = Role.Button, scaleOnPress = false)) {
        AddressHints(row.addressHints)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // (0.4.0) Unified left safe area across all 6 block kinds:
            // - DIALOGUE: 1:1 square/circle character portrait + name (tapping portrait opens character card)
            // - Other kinds: visual distinction badge only (no tap action; preserves swipe & long-press)
            Column(
                modifier = Modifier.width(safeWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (row.kind == BlockKind.DIALOGUE) {
                    val speaker = row.speaker
                    val charKey = speaker?.characterKey
                    val openCard: (() -> Unit)? = if (charKey != null && doc != null && deps != null) {
                        { deps.navigator.push(io.github.lenticualrstone.civilight.navigation.CharacterProfile(doc.project.key, charKey)) }
                    } else null
                    io.github.lenticualrstone.civilight.ui.image.CharacterPortraitTile(
                        doc = doc,
                        characterKey = charKey,
                        displayName = speaker?.text.orEmpty(),
                        shape = profileShape,
                        size = portraitSize,
                        onClick = openCard,
                    )
                    if (speaker != null) {
                        CivilightText(
                            speaker.text,
                            CivilightTheme.type.micro,
                            color = if (charKey != null) colors.ink2 else colors.ink3,
                            maxLines = 2,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    val (badgeIcon, badgeLabel, badgeTint) = when (row.kind) {
                        BlockKind.NARRATION -> Triple(io.github.lenticualrstone.civilight.designsystem.icon.IconId.Text, "지문", colors.ink3)
                        BlockKind.DIRECTION -> Triple(io.github.lenticualrstone.civilight.designsystem.icon.IconId.Spark, "연출", colors.acc)
                        BlockKind.CHOICE -> Triple(io.github.lenticualrstone.civilight.designsystem.icon.IconId.Split, "선택지", colors.acc)
                        BlockKind.SYSTEM -> Triple(io.github.lenticualrstone.civilight.designsystem.icon.IconId.Exclaim, "시스템", colors.ink2)
                        BlockKind.MEMO -> Triple(io.github.lenticualrstone.civilight.designsystem.icon.IconId.Paper, "메모", colors.warn)
                        else -> Triple(io.github.lenticualrstone.civilight.designsystem.icon.IconId.File, "블록", colors.ink3)
                    }
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.tint),
                        contentAlignment = Alignment.Center,
                    ) {
                        io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon(
                            badgeIcon,
                            contentDescription = null,
                            size = 15.dp,
                            tint = badgeTint,
                        )
                    }
                    CivilightText(
                        badgeLabel,
                        CivilightTheme.type.micro,
                        color = badgeTint,
                        maxLines = 1,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }

            // Right content column: metadata line + block body + inline popover
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (row.kind == BlockKind.DIALOGUE) {
                    val speaker = row.speaker
                    if (speaker != null && (numbered || look.tag != null || row.speakerMarks.isNotEmpty() || speaker.realName != null)) {
                        ScriptSpeakerLine(
                            speaker, numbered, row.number.takeIf { numbered }, row.speakerMarks,
                            extraLabel = look.tag,
                            onClick = onSpeaker,
                        )
                    }
                } else {
                    val slot = row.slot(numbered)
                    if (slot != null) {
                        SlotLine(slot.label, listOfNotNull(slot.sub, look.tag).joinToString(" · ").ifEmpty { null }, slot.number)
                    } else if (look.tag != null) {
                        SlotLine(io.github.lenticualrstone.civilight.domain.script.Labels.kindLabel(row.kind), look.tag, row.number)
                    }
                }
                when (row.kind) {
                    BlockKind.DIRECTION -> CivilightText(row.text, type.dir, color = colors.ink3)
                    BlockKind.CHOICE -> row.options.forEach { option ->
                        OptionView(option, selected, onTap = onTapOption?.let { tap -> { tap(option) } })
                    }
                    null -> CivilightText(row.text, type.body, color = colors.ink2)
                    else -> Box {
                        RunsText(row, showParens, onTapText, onTapTarget, openCandidate)
                        popover?.invoke()
                    }
                }
            }
        }
    }
    if (row.preReveal) MarginWarning(selected, onMarginWarning, Modifier.align(Alignment.TopStart))
    }
}

/**
 * The amber `▲` in the line's margin (SCR-56): a link here is used before its term's first reveal.
 * Drawn whatever the layers; a tap opens the check center's `용어 공개` items.
 */
@Composable
private fun MarginWarning(selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier) {
    val tap = onClick?.let { Modifier.pressable(PressAction.Click(it), role = Role.Button, hitTarget = HitTarget.SMALL, scaleOnPress = false) } ?: Modifier
    Box(
        modifier
            .padding(top = if (selected) 12.dp else 0.dp)
            .width(24.dp)
            .heightIn(min = 24.dp)
            .semantics { contentDescription = TermCopy.MARGIN }
            .then(tap),
        contentAlignment = Alignment.Center,
    ) {
        CivilightText("▲", CivilightTheme.type.small, color = CivilightTheme.colors.warn, maxLines = 1)
    }
}

/**
 * The vertical padding of a block's frame while the manuscript animates it (SCR-67): the row keeps
 * one state across the reading look and the field, so selecting and typing grow it instead of
 * cutting. Null = the frame decides (`12 dp` selected).
 */
val LocalBlockPad = staticCompositionLocalOf<State<Dp>?> { null }

/** (0.2.0) How a block reads beyond selection: [tag] `고치는 중 · 새로 · 나눔 · 합침`, [numbered], [afterTarget]. */
data class BlockLook(val tag: String? = null, val numbered: Boolean = false, val afterTarget: Boolean = false)

/** Shared block frame: column gap 6, side padding 24 (the caller's column), selection look. */
@Composable
fun BlockFrame(selected: Boolean, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = CivilightTheme.colors
    val animated = LocalBlockPad.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .selectedRowBackground(selected, colors.sel, colors.acc, outset = 0.dp)
            .padding(horizontal = 24.dp)
            .padding(vertical = animated?.value ?: if (selected) 12.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/** One option line of a choice block (board `Blocks`): mono index (`#045-1` while selected) + serif 16 / 26 text. */
@Composable
fun OptionView(option: OptionLine, selected: Boolean, onTap: (() -> Unit)?, text: @Composable (() -> Unit)? = null) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    // Lines keep the board's 16 / 26 rhythm; the touch area grows to 44 dp without moving them (DS-11).
    val tap = onTap?.let {
        Modifier.pressable(PressAction.Click(it), role = Role.Button, hitTarget = HitTarget.SMALL, scaleOnPress = false)
    } ?: Modifier
    Row(
        tap.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        CivilightText(if (selected) option.number else option.index, optionIndexStyle(), color = colors.ink3, maxLines = 1)
        if (text != null) {
            Box(Modifier.weight(1f)) { text() }
        } else {
            CivilightText(option.text, optionTextStyle(), Modifier.weight(1f), color = colors.ink)
        }
    }
}

/** Option index: mono 12. */
@Composable
fun optionIndexStyle(): TextStyle = CivilightTheme.type.number.merge(TextStyle(fontSize = 12.sp))

/** Option text: serif 16 / 26. */
@Composable
fun optionTextStyle(): TextStyle = CivilightTheme.type.body.merge(TextStyle(lineHeight = 26.sp))

/** A [SlotModel] line. */
@Composable
fun SlotLine(slot: SlotModel) = SlotLine(slot.label, slot.sub, slot.number)

/** `선택지 · 다음 블록에서 합류` / `시스템 · 화면 문구` speaker slot (boards `Blocks`). */
@Composable
fun SlotLine(label: String, sub: String?, number: String?) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        CivilightText(label, type.speakerName, color = colors.ink2, maxLines = 1)
        if (sub != null) CivilightText(sub, type.speaker, color = colors.ink3, maxLines = 1)
        if (number != null) CivilightText("· $number", type.speaker, color = colors.ink3, maxLines = 1)
    }
}

/** `호칭 힌트` (SCR-29): `리엔을 ‘꼬마’` in `small` `ink3` above the speaker line. */
@Composable
fun AddressHints(hints: List<String>) {
    if (hints.isEmpty()) return
    CivilightText(hints.joinToString(" · "), CivilightTheme.type.small, color = CivilightTheme.colors.ink3, maxLines = 1)
}

/** Text runs with their decorations, the memo prefix, and the end-of-line marks; taps give a raw offset. */
@Composable
private fun RunsText(
    row: BlockRow,
    showParens: Boolean,
    onTapText: (Int?) -> Unit,
    onTapTarget: ((BlockTap) -> Boolean)?,
    openCandidate: TermCandidate?,
) {
    val (style, color) = blockTextStyle(row.kind)
    val layout = remember(row, showParens) { row.layout(showParens) }
    val prefix = row.memoPrefix?.let { "$it  " }.orEmpty()
    val suffix = row.textMarks.joinToString("") { " ${it.glyph}" }
    val text = prefix + layout.text + suffix
    val decorations = remember(layout, prefix, row.termCandidates, openCandidate) {
        layout.spans.map { DecorationRange(it.start + prefix.length, it.end + prefix.length, it.kind.decoration()) } +
            // SCR-56: candidate spans (raw offsets) drawn dotted; the tapped one boxed.
            row.termCandidates.map { c ->
                val kind = if (c.candidate == openCandidate) InlineDecoration.CANDIDATE_SELECTED else InlineDecoration.CANDIDATE
                DecorationRange(layout.toDisplay(c.start) + prefix.length, layout.toDisplay(c.end) + prefix.length, kind)
            }
    }
    val dotted = remember(row.textMarks, text) { dottedIndices(text.length - suffix.length, row.textMarks) }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    DecoratedText(
        AnnotatedString(text),
        style,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 20.dp)
            .pointerInput(layout, prefix, row.termCandidates, row.recorderRecordKey, onTapTarget) {
                detectTapGestures { position ->
                    val result = textLayout
                    val at = result?.getOffsetForPosition(position)
                    // The character under the finger (links, candidates, the recorder source).
                    val char = if (result == null || at == null) null else listOf(at, at - 1).firstOrNull { i ->
                        i in 0 until (text.length - suffix.length) && result.getBoundingBox(i).contains(position)
                    }
                    if (char != null && onTapTarget != null) {
                        val inPrefix = char < prefix.length
                        val raw = if (inPrefix) null else layout.toRaw(char - prefix.length)
                        val hit = LinkFixes.tapAt(row, raw, inPrefix)
                        if (hit != null && onTapTarget(hit)) return@detectTapGestures
                    }
                    onTapText(at?.let { layout.toRaw((it - prefix.length).coerceAtLeast(0)) })
                }
            },
        color = color,
        decorations = decorations,
        dottedMarkers = dotted,
        onTextLayout = { textLayout = it },
    )
}

/** Indices of dotted marks appended as ` ◆` / ` ◇` after [start]. */
private fun dottedIndices(start: Int, marks: List<MarkModel>): Set<Int> =
    marks.mapIndexedNotNull { i, m -> if (m.dotted) start + i * 2 + 1 else null }.toSet()

/**
 * Scene head (SCR-46, DS-43): head text and the `전환 문구` line; tap → `씬 · 장소와 때`; `⋯`
 * (`씬 4 메뉴`) → the scene menu (SCR-47) when [onMenu] is given; `sel` while that menu is open.
 */
@Composable
fun HeadView(row: HeadRow, selected: Boolean, onOpen: () -> Unit, onMenu: (() -> Unit)?) {
    // DS-08: wraps between 어절 only (emulator QA: `때 없 / 음` in the 1:2 split's 280 dp column).
    val title = remember(row.title) { HeadLines.unbroken(row.title) }
    val subtitle = HeadLines.subtitle(row)
    val subtitleShown = remember(subtitle) { subtitle?.let(HeadLines::unbroken) }
    SceneHead(
        title = title,
        modifier = Modifier.pressable(PressAction.Click(onOpen), role = Role.Button, scaleOnPress = false),
        // SCR-58 `[PROPOSED]`: an unlocked hidden scene's head carries a small `숨김` tag.
        subtitle = subtitleShown,
        menu = onMenu?.let { IconAction(ScriptCopy.sceneMenuLabel(row.ordinal), it) },
        selected = selected,
    )
}

/** A locked hidden scene: one row; tapping starts the unlock (rule 19). */
@Composable
fun HiddenSceneView(row: HiddenSceneRow, onUnlock: () -> Unit) {
    val colors = CivilightTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(PressAction.Click(onUnlock), role = Role.Button, scaleOnPress = false)
            .heightIn(min = 44.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivilightText(row.text, CivilightTheme.type.small, color = colors.ink3, maxLines = 1)
    }
}

/** The virtual first line of an empty scene: speaker line (inherited or `화자 없음`) and an empty line. */
@Composable
fun EmptySceneView(row: EmptySceneRow, speaker: SpeakerModel?, onTap: () -> Unit) {
    BlockFrame(false, Modifier.pressable(PressAction.Click(onTap), role = Role.Button, scaleOnPress = false)) {
        ScriptSpeakerLine(
            speaker ?: SpeakerModel(ScriptCopy.NO_SPEAKER, null, SpeakerLook.UNSET, null),
            selected = false,
            number = null,
            marks = emptyList(),
            extraLabel = ScriptCopy.INHERITED_SPEAKER.takeIf { speaker != null && speaker == row.speaker },
        )
        Spacer(Modifier.height(CivilightTheme.type.line.lineHeight.value.dp))
    }
}

/** The scene head's second line (SCR-46, SCR-58): the transition line and, for a hidden scene, `숨김`. */
object HeadLines {
    fun subtitle(row: HeadRow): String? =
        listOfNotNull(row.transitionLine, io.github.lenticualrstone.civilight.feature.script.scenes.SceneCopy.HIDDEN.takeIf { row.hiddenTag })
            .joinToString(" · ").ifEmpty { null }

    /** U+2060 WORD JOINER: no line break on either side of it (UAX #14 class WJ), drawn as nothing. */
    const val WORD_JOINER: Char = '\u2060'

    /** U+00A0 NO-BREAK SPACE: keeps a ` · ` separator on the line of the word before it. */
    const val NO_BREAK_SPACE: Char = '\u00A0'

    /** Words longer than this stay breakable, so a very long name still wraps instead of running off. */
    const val MAX_JOINED: Int = 16

    /**
     * Display form of a head line that breaks only between 어절 (DS-08), whatever the platform's
     * Korean line breaking does (the phrase word-break is API 33+ and locale-bound): the letters of each
     * word up to [MAX_JOINED] letters are joined with [WORD_JOINER], and the space before a `·`
     * becomes [NO_BREAK_SPACE] so no line starts with `·`. Display only: rows, tests and search keep
     * the plain text.
     */
    fun unbroken(text: String): String {
        val out = StringBuilder(text.length * 2)
        val words = text.split(' ')
        words.forEachIndexed { i, word ->
            if (i > 0) out.append(if (word == "·") NO_BREAK_SPACE else ' ')
            val letters = graphemes(word)
            if (letters.size > MAX_JOINED) out.append(word) else out.append(letters.joinToString(WORD_JOINER.toString()))
        }
        return out.toString()
    }

    /** The user-perceived letters of [word] (a joiner never splits a combining mark or an emoji sequence). */
    private fun graphemes(word: String): List<String> {
        if (word.isEmpty()) return emptyList()
        val it = java.text.BreakIterator.getCharacterInstance()
        it.setText(word)
        val out = ArrayList<String>()
        var start = it.first()
        var end = it.next()
        while (end != java.text.BreakIterator.DONE) {
            out += word.substring(start, end)
            start = end
            end = it.next()
        }
        return out
    }
}
