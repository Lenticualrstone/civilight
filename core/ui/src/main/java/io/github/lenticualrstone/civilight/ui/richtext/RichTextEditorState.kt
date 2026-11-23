package io.github.lenticualrstone.civilight.ui.richtext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.text.DocBlock
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.ordered
import io.github.lenticualrstone.civilight.ui.picker.PickKind
import kotlinx.serialization.builtins.ListSerializer

/** A pick from the `@` picker, sent back through `NavResults` by `RichLinkPickerSheet`. */
data class LinkPick(val kind: PickKind, val key: Key, val text: String) {
    /** The stored link kind, or null for a kind a document cannot link (regions, units, events). */
    val targetKind: LinkTargetKind?
        get() = when (kind) {
            PickKind.CHARACTER -> LinkTargetKind.CHARACTER
            PickKind.TERM -> LinkTargetKind.TERM
            PickKind.PLACE -> LinkTargetKind.PLACE
            PickKind.FACTION -> LinkTargetKind.FACTION
            else -> null
        }
}

/**
 * The editor's state: the document being edited, the field (text, caret, IME composition) and the
 * paren mode. The field is the source of truth while typing; the document follows every change
 * through [RichDocEdit.apply], so it is never re-seeded from the document (Hangul composition stays
 * intact). Read [result] to save.
 */
@Stable
class RichTextEditorState internal constructor(
    initialDoc: RichDoc,
    initialField: TextFieldValue,
    initialParen: Boolean,
    private val newKey: () -> Key,
    /** The document the editor started from (to tell whether anything changed). */
    val original: RichDoc,
) {
    var doc: RichDoc by mutableStateOf(initialDoc)
        private set

    var field: TextFieldValue by mutableStateOf(initialField)
        private set

    /** `( )` on: typed text becomes `괄호 지문`. */
    var parenMode: Boolean by mutableStateOf(initialParen)
        private set

    /** The caret where `@` was pressed; the pick is inserted there. */
    internal var linkAt: Int = initialField.selection.min

    /** The document to save: trailing empty paragraphs dropped, empty text = empty document. */
    val result: RichDoc get() = RichDocEdit.trimmed(doc)

    /** True when [result] differs from what the editor started with. */
    val changed: Boolean get() = result != RichDocEdit.trimmed(original)

    /** A field change from the text field (typing, IME composition, caret moves). */
    fun onFieldChange(value: TextFieldValue) {
        if (value.text != field.text) doc = RichDocEdit.apply(doc, value.text, newKey, parenMode)
        field = value
    }

    fun toggleParen() {
        parenMode = !parenMode
    }

    /** Remembers the caret for the `@` pick about to be made. */
    fun beginLink() {
        linkAt = field.selection.min
    }

    /** Inserts [pick] as a link run at the caret remembered by [beginLink]; unsupported kinds are ignored. */
    fun insertLink(pick: LinkPick) {
        val kind = pick.targetKind ?: return
        val link = TextRun.Link(newKey(), LinkTarget(kind, pick.key), pick.text)
        val (next, caret) = RichDocEdit.insertLink(doc, linkAt, link, newKey)
        doc = next
        field = TextFieldValue(RichDocEdit.textOf(next), TextRange(caret))
        linkAt = caret
    }

    /** Replaces everything (e.g. the value changed elsewhere and the user chose to reload). */
    fun reset(to: RichDoc) {
        doc = to.ordered()
        field = TextFieldValue(RichDocEdit.textOf(doc), TextRange(RichDocEdit.textOf(doc).length))
        parenMode = false
    }

    companion object {
        private val docSerializer = ListSerializer(DocBlock.serializer())

        /** A state over [doc] with the caret at the end. */
        fun of(doc: RichDoc, newKey: () -> Key = Keys::newKey): RichTextEditorState {
            val ordered = doc.ordered()
            val text = RichDocEdit.textOf(ordered)
            return RichTextEditorState(ordered, TextFieldValue(text, TextRange(text.length)), false, newKey, ordered)
        }

        /** Saves the document, caret and paren mode for process death (CNV-35). */
        fun saver(newKey: () -> Key): Saver<RichTextEditorState, Any> = Saver(
            save = { s ->
                listOf(
                    CivilightJson.encode(docSerializer, s.doc),
                    s.field.selection.start,
                    s.field.selection.end,
                    s.parenMode,
                    CivilightJson.encode(docSerializer, s.original),
                    s.linkAt,
                )
            },
            restore = { saved ->
                val list = saved as List<*>
                val doc = CivilightJson.decode(docSerializer, list[0] as String)
                val text = RichDocEdit.textOf(doc)
                val start = (list[1] as Int).coerceIn(0, text.length)
                val end = (list[2] as Int).coerceIn(0, text.length)
                RichTextEditorState(doc, TextFieldValue(text, TextRange(start, end)), list[3] as Boolean, newKey, CivilightJson.decode(docSerializer, list[4] as String))
                    .also { it.linkAt = (list.getOrNull(5) as? Int ?: start).coerceIn(0, text.length) }
            },
        )
    }
}

/**
 * The editor state for [initial], kept across recomposition and process death. [key] restarts it
 * (e.g. another field or item); a new [initial] (a host saving as typed hands back what was just typed)
 * and a new [newKey] lambda do not, so typing is never interrupted and the field is never re-seeded.
 */
@Composable
fun rememberRichTextEditorState(initial: RichDoc, key: Any? = null, newKey: () -> Key = Keys::newKey): RichTextEditorState {
    // One saver for the composable's life: it reads the latest key maker, so a host passing a fresh
    // lambda each frame changes neither the saver nor the state.
    val keys = rememberUpdatedState(newKey)
    val saver = remember { RichTextEditorState.saver { keys.value() } }
    return rememberSaveable(key, saver = saver) { RichTextEditorState.of(initial) { keys.value() } }
}
