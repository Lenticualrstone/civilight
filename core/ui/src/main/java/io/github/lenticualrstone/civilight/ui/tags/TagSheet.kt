package io.github.lenticualrstone.civilight.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxMark
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.RadioMark
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.model.cast.fits
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import kotlinx.coroutines.launch

/** Copy of the shared tag sheet (board `ImmTermTagSheet`). Labels and fragments only. */
object TagSheetCopy {
    const val SEP = " · "
    const val TITLE = "태그"
    const val NEW_TAG = "＋ 새 태그"
    const val NAME = "이름"
    const val SCOPE = "범위"
    const val DEFAULT = "기본"
    const val EVERYWHERE = "모든 항목"
    const val EVERYWHERE_SUB = "인물 · 장소 · 용어 · 세력"
    const val NOTE = "모든 항목으로 만든 태그는 나중에 범위를 줄일 수 없음 · 넓히기만 가능"
    const val CANCEL = "취소"
    const val CREATE = "만들기"

    /** The scope word of a `모든 항목` tag in the list. */
    const val SCOPE_ALL = "전체"
    const val USE_EXISTING = "이 태그 쓰기"
    const val WIDEN = "모든 항목으로 넓히기"
    const val NONE = "태그 없음"
    const val READ_ONLY = "읽기 전용"

    /** `용어에서 쓸 수 있는 것`. */
    fun subtitle(kindWord: String): String = "${kindWord}에서 쓸 수 있는 것"

    /** `용어 전용`. */
    fun ownScope(kindWord: String): String = "$kindWord 전용"

    /** `이미 있음 · 광물` (the name is taken by a live tag). */
    fun taken(name: String): String = "이미 있음$SEP$name"

    val all: List<String> = listOf(TITLE, NEW_TAG, NAME, SCOPE, DEFAULT, EVERYWHERE, EVERYWHERE_SUB, NOTE, CANCEL, CREATE, SCOPE_ALL, USE_EXISTING, WIDEN, NONE, READ_ONLY)
}

/** One tag of the list: [everywhere] = scope `모든 항목` (shown `전체`), else the kind's own scope. */
data class TagSheetRow(val key: String, val name: String, val checked: Boolean, val everywhere: Boolean)

/** What `만들기` did; the sheet maps the domain result to this. */
sealed interface TagSheetCreate {
    /** Made and put on the item; the form closes. */
    data object Created : TagSheetCreate

    /** Nothing to make (blank name). */
    data object Blank : TagSheetCreate

    /** A live tag already has the name; [fits] = this kind may carry it (`이 태그 쓰기`), else only widening. */
    data class Taken(val key: String, val name: String, val fits: Boolean) : TagSheetCreate

    /** The project could not be written (read-only). */
    data object Failed : TagSheetCreate
}

/** Pure state of the new-tag form (testable without Compose). */
data class TagForm(val open: Boolean = false, val name: String = "", val everywhere: Boolean = false, val taken: TagSheetCreate.Taken? = null) {
    /** `만들기` is enabled for a non-blank name. */
    val canCreate: Boolean get() = name.isNotBlank()

    fun opened(): TagForm = TagForm(open = true)

    fun named(next: String): TagForm = copy(name = next, taken = null)

    fun scoped(everywhere: Boolean): TagForm = copy(everywhere = everywhere)

    /** After `만들기`: closes on success, keeps the form with the taken tag otherwise. */
    fun after(result: TagSheetCreate): TagForm = when (result) {
        TagSheetCreate.Created -> TagForm()
        is TagSheetCreate.Taken -> copy(taken = result)
        TagSheetCreate.Blank, TagSheetCreate.Failed -> this
    }

    /** The visible action next to `이미 있음 · <name>`, or null when there is none for this kind. */
    fun takenAction(canWiden: Boolean): String? = when {
        taken == null -> null
        taken.fits -> TagSheetCopy.USE_EXISTING
        canWiden -> TagSheetCopy.WIDEN
        else -> null
    }
}

/**
 * The shared tag sheet (board `ImmTermTagSheet`): title `태그` with `<kind>에서 쓸 수 있는 것`; a checkable row per
 * tag the kind may carry (checkbox = many) with its scope word (`<kind>` or a `전체` pill); `＋ 새 태그` opens the
 * form: name, scope radios (`<kind> 전용` default · `모든 항목`), the one-way note, `취소 · 만들기`. A taken name shows
 * `이미 있음 · <name>` with a visible `이 태그 쓰기` (or `모든 항목으로 넓히기` when this kind may not carry it and
 * [onWiden] is given). Used by term cards; written so character and gallery tag UI can reuse it.
 */
@Composable
fun SheetScope.TagSheet(
    kindWord: String,
    rows: List<TagSheetRow>,
    readOnly: Boolean,
    onToggle: (String) -> Unit,
    onCreate: suspend (name: String, everywhere: Boolean) -> TagSheetCreate,
    onUseExisting: (String) -> Unit,
    onWiden: ((String) -> Unit)? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val scope = rememberCoroutineScope()
    var open by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var everywhere by rememberSaveable { mutableStateOf(false) }
    var taken by androidx.compose.runtime.remember { mutableStateOf<TagSheetCreate.Taken?>(null) }
    fun form() = TagForm(open, name, everywhere, taken)
    fun set(f: TagForm) {
        open = f.open; name = f.name; everywhere = f.everywhere; taken = f.taken
    }

    SheetHeader(title = TagSheetCopy.TITLE, onClose = ::close, subtitle = TagSheetCopy.subtitle(kindWord))
    SheetScrollBody(Modifier.padding(top = 8.dp)) {
        if (rows.isEmpty()) {
            CivilightText(TagSheetCopy.NONE, type.small, Modifier.padding(vertical = 12.dp), color = colors.ink3)
        }
        rows.forEach { row -> TagRow(row, kindWord, enabled = !readOnly) { onToggle(row.key) } }
        if (readOnly) {
            CivilightText(TagSheetCopy.READ_ONLY, type.small, Modifier.padding(top = 12.dp), color = colors.ink3)
            return@SheetScrollBody
        }
        Row(
            Modifier
                .fillMaxWidth()
                .topHairline(colors.line)
                .pressable(PressAction.Click(onClick = { set(if (open) TagForm() else form().opened()) }), role = Role.Button, scaleOnPress = false)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CivilightText(TagSheetCopy.NEW_TAG, type.label, color = colors.acc)
        }
        FoldReveal(open) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.tint)
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CivilightTextField(
                    value = name,
                    onValueChange = { set(form().named(it)) },
                    label = TagSheetCopy.NAME,
                    modifier = Modifier.fillMaxWidth(),
                )
                FoldRevealValue(taken) { t ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CivilightText(TagSheetCopy.taken(t.name), type.small, Modifier.weight(1f, fill = false), color = colors.warn)
                        when (form().takenAction(onWiden != null)) {
                            TagSheetCopy.USE_EXISTING -> TextAction(TagSheetCopy.USE_EXISTING, { onUseExisting(t.key); set(TagForm()) }, color = colors.acc, style = type.small)
                            TagSheetCopy.WIDEN -> TextAction(TagSheetCopy.WIDEN, { onWiden?.invoke(t.key); set(TagForm()) }, color = colors.acc, style = type.small)
                            else -> Unit
                        }
                    }
                }
                CivilightText(TagSheetCopy.SCOPE, type.sec, color = colors.ink3)
                // Radios = one choice; the kind's own scope is the default.
                ScopeRadio(TagSheetCopy.ownScope(kindWord), TagSheetCopy.DEFAULT, selected = !everywhere) { set(form().scoped(false)) }
                ScopeRadio(TagSheetCopy.EVERYWHERE, TagSheetCopy.EVERYWHERE_SUB, selected = everywhere) { set(form().scoped(true)) }
                CivilightText(TagSheetCopy.NOTE, type.small, color = colors.ink3)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    CivilightButton(TagSheetCopy.CANCEL, { set(TagForm()) }, variant = ButtonVariant.GHOST)
                    CivilightButton(
                        TagSheetCopy.CREATE,
                        {
                            val f = form()
                            scope.launch { set(f.after(onCreate(f.name, f.everywhere))) }
                        },
                        enabled = form().canCreate,
                    )
                }
            }
        }
    }
}

/** A tag row: 22 dp checkbox, name, the scope word (`용어` in `ink3`, `전체` as an accent pill); min 48. */
@Composable
private fun TagRow(row: TagSheetRow, kindWord: String, enabled: Boolean, onToggle: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .pressable(PressAction.Select(row.checked, onToggle), enabled = enabled, role = Role.Checkbox, scaleOnPress = false)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CheckboxMark(row.checked, enabled = enabled)
        CivilightText(row.name, type.label, Modifier.weight(1f), color = colors.ink, maxLines = 1)
        TagScopeMark(if (row.everywhere) TagSheetCopy.SCOPE_ALL else kindWord, everywhere = row.everywhere)
    }
}

/**
 * The scope word of a tag row (board `ImmTermTagSheet`): a kind word (`인물` · `용어`) in `ink3`, or `전체` as an
 * accent pill for a `모든 항목` tag. Shared by the tag sheet and tag settings.
 */
@Composable
fun TagScopeMark(word: String, everywhere: Boolean, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    if (everywhere) {
        Box(
            modifier
                .clip(RoundedCornerShape(11.dp))
                .background(colors.acc.copy(alpha = 0.14f))
                .heightIn(min = 22.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) { CivilightText(word, type.micro, color = colors.acc) }
    } else {
        CivilightText(word, type.small, modifier, color = colors.ink3, maxLines = 1)
    }
}

/**
 * The same-name line under a tag name field: `이미 있음 · 광물` in `warn`, then the visible action ([action] is
 * `이 태그 쓰기` only when this kind may carry the tag; null = the line alone). Shown with [FoldRevealValue].
 */
@Composable
fun TagTakenLine(taken: TagSheetCreate.Taken?, action: String?, onAction: (TagSheetCreate.Taken) -> Unit, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    FoldRevealValue(taken) { t ->
        Row(modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CivilightText(TagSheetCopy.taken(t.name), type.small, Modifier.weight(1f, fill = false), color = colors.warn)
            if (action != null) TextAction(action, { onAction(t) }, color = colors.acc, style = type.small)
        }
    }
}

/**
 * (2026-11-26) The same-name state of a typed tag name, derived from the document at render time (not from a
 * write): the live tag of that name (any scope; names are unique across scopes) with [TagSheetCreate.Taken.fits]
 * = an item of [kind] may carry it. Null when the name is blank or free.
 */
object TagNameCheck {
    fun taken(
        doc: io.github.lenticualrstone.civilight.model.doc.ProjectDoc,
        name: String,
        kind: io.github.lenticualrstone.civilight.model.cast.TagScope,
        except: String? = null,
    ): TagSheetCreate.Taken? {
        val tag = io.github.lenticualrstone.civilight.model.cast.TagScopes.liveNamed(doc, name, except) ?: return null
        return TagSheetCreate.Taken(tag.key, tag.name, tag.fits(kind))
    }

    /** The visible action of a picker or `+ 태그` line: `이 태그 쓰기` only when the tag fits; no widening there. */
    fun action(taken: TagSheetCreate.Taken?): String? = TagForm(taken = taken).takenAction(canWiden = false)
}

/** A scope radio row (min 44): mark, label, the trailing note in `ink3`. Radios = one choice (also tag settings). */
@Composable
fun ScopeRadio(label: String, trailing: String, selected: Boolean, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(PressAction.Select(selected, onClick), role = Role.RadioButton, scaleOnPress = false)
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioMark(selected, Modifier.size(20.dp))
        CivilightText(label, type.label, Modifier.weight(1f), color = if (selected) colors.ink else colors.ink2)
        CivilightText(trailing, type.small, color = colors.ink3, maxLines = 1)
    }
}
