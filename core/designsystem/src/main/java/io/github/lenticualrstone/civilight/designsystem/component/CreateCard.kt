package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.tokenShadow
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.OverlayGaps

/*
 * DS-72 / U-15: the shared create card. Pure rules (field sets, buttons, labels, layout) are JVM tested; the
 * overlay and the create rows are stateless composables. Each feature supplies its write and the content of
 * fields that are not plain text (image, picks, colour, file, time).
 */

/** Where the card was opened: a list's create row (`취소 · 만들고 열기 · 만들기`) or a picker (`취소 · 만들기`). */
enum class CreateCardMode { LIST, PICKER }

/** The card's buttons (c-class text). */
enum class CreateButton(val label: String) {
    CANCEL(CreateCardCopy.CANCEL),
    CREATE_AND_OPEN(CreateCardCopy.CREATE_AND_OPEN),
    CREATE(CreateCardCopy.CREATE),
}

/** How a field is edited. [TEXT] is drawn by the card; every other type is a slot the feature fills. */
enum class CreateFieldType { TEXT, IMAGE, PICK, MULTI_PICK, COLOR, FILE, TIME }

/** One field of a create card: a stable [key], its visible [label] and [type]. Only the name is [required]. */
data class CreateField(val key: String, val label: String, val type: CreateFieldType, val required: Boolean = false)

/** The kinds that can be created in place (U-15.4), with their field sets (U-15.5, U-15.7, U-15.9). */
enum class CreateKind(val word: String, val fields: List<CreateField>) {
    CHARACTER(
        CreateCardCopy.WORD_CHARACTER,
        listOf(CreateFields.image, CreateFields.name, CreateFields.displayName, CreateFields.oneLineIntro, CreateFields.tags),
    ),
    PLACE(CreateCardCopy.WORD_PLACE, listOf(CreateFields.image, CreateFields.name, CreateFields.parentRegion, CreateFields.oneLineNote)),
    TERM(CreateCardCopy.WORD_TERM, listOf(CreateFields.name, CreateFields.aliases, CreateFields.oneLineNote)),
    FACTION(CreateCardCopy.WORD_FACTION, listOf(CreateFields.image, CreateFields.name, CreateFields.oneLineNote)),
    EVENT(CreateCardCopy.WORD_EVENT, listOf(CreateFields.name, CreateFields.time, CreateFields.people)),
    ASSET(CreateCardCopy.WORD_ASSET, listOf(CreateFields.assetKind, CreateFields.name, CreateFields.file)),
    TAG(CreateCardCopy.WORD_TAG, listOf(CreateFields.name, CreateFields.color)),
    RELATION_KIND(CreateCardCopy.WORD_RELATION_KIND, listOf(CreateFields.name, CreateFields.bothSides)),
    PAGE(CreateCardCopy.WORD_PAGE, listOf(CreateFields.name)),
    CANVAS(CreateCardCopy.WORD_CANVAS, listOf(CreateFields.name)),
}

/** The field definitions shared by the kinds. */
object CreateFields {
    const val NAME = "name"

    val name = CreateField(NAME, CreateCardCopy.FIELD_NAME, CreateFieldType.TEXT, required = true)
    val image = CreateField("image", CreateCardCopy.FIELD_IMAGE, CreateFieldType.IMAGE)
    val displayName = CreateField("displayName", CreateCardCopy.FIELD_DISPLAY_NAME, CreateFieldType.TEXT)
    val oneLineIntro = CreateField("oneLine", CreateCardCopy.FIELD_ONE_LINE_INTRO, CreateFieldType.TEXT)
    val oneLineNote = CreateField("oneLine", CreateCardCopy.FIELD_ONE_LINE_NOTE, CreateFieldType.TEXT)
    val tags = CreateField("tags", CreateCardCopy.FIELD_TAGS, CreateFieldType.MULTI_PICK)
    val parentRegion = CreateField("parent", CreateCardCopy.FIELD_PARENT_REGION, CreateFieldType.PICK)
    val aliases = CreateField("aliases", CreateCardCopy.FIELD_ALIASES, CreateFieldType.TEXT)
    val time = CreateField("time", CreateCardCopy.FIELD_TIME, CreateFieldType.TIME)
    val people = CreateField("people", CreateCardCopy.FIELD_PEOPLE, CreateFieldType.MULTI_PICK)
    val assetKind = CreateField("assetKind", CreateCardCopy.FIELD_ASSET_KIND, CreateFieldType.PICK)
    val file = CreateField("file", CreateCardCopy.FIELD_FILE, CreateFieldType.FILE)
    val color = CreateField("color", CreateCardCopy.FIELD_COLOR, CreateFieldType.COLOR)
    val bothSides = CreateField("bothSides", CreateCardCopy.FIELD_BOTH_SIDES, CreateFieldType.TEXT)
}

/**
 * The card's typed values: [values] by field key (text fields; slot fields may store a display value or a key).
 * Only the name decides whether it can be created.
 */
data class CreateCardState(val kind: CreateKind, val mode: CreateCardMode, val values: Map<String, String> = emptyMap()) {
    val name: String get() = values[CreateFields.NAME].orEmpty()

    val canCreate: Boolean get() = CreateCard.canCreate(name)

    /** Anything typed or picked (a scrim tap then does nothing, DS-72). */
    val hasContent: Boolean get() = values.values.any { it.isNotBlank() }

    fun with(key: String, value: String): CreateCardState = copy(values = values + (key to value))

    companion object {
        /** A card opened from a picker with [query]: the name holds the query (U-15.3). */
        fun open(kind: CreateKind, mode: CreateCardMode, query: String? = null): CreateCardState =
            CreateCardState(kind, mode, if (query.isNullOrBlank()) emptyMap() else mapOf(CreateFields.NAME to query.trim()))
    }
}

/** Where the card sits (dp, from the window top) and how tall its field area is (T-DS-50). */
data class CreateCardFit(val top: Float, val bottom: Float, val fieldArea: Float)

/** DS-72 pure rules. */
object CreateCard {
    /** Cover card side margin and inner max width. */
    const val SIDE_DP: Float = 16f
    const val MAX_WIDTH_INNER_DP: Float = 420f
    const val PADDING_DP: Float = 20f

    /** Bottom gap without the keyboard: `systemBottom + 16`. */
    const val BOTTOM_GAP_DP: Float = 16f

    fun buttons(mode: CreateCardMode): List<CreateButton> = when (mode) {
        CreateCardMode.LIST -> listOf(CreateButton.CANCEL, CreateButton.CREATE_AND_OPEN, CreateButton.CREATE)
        CreateCardMode.PICKER -> listOf(CreateButton.CANCEL, CreateButton.CREATE)
    }

    /** `만들기` and `만들고 열기` are on only while the name is non-blank after trim. */
    fun canCreate(name: String): Boolean = name.isNotBlank()

    /** `새 인물`. */
    fun title(kind: CreateKind): String = CreateCardCopy.newOf(kind.word)

    /**
     * The create row's label: `새 인물` on a list or a picker; in a picker whose [query] matches nothing,
     * `‘세라’ 새 인물` (U-15.3).
     */
    fun rowLabel(kind: CreateKind, query: String? = null, matched: Boolean = true): String =
        if (!matched && !query.isNullOrBlank()) CreateCardCopy.newNamed(query.trim(), kind.word) else title(kind)

    /** A scrim tap closes the card only while nothing has been typed. */
    fun dismissOnScrim(state: CreateCardState): Boolean = !state.hasContent

    /**
     * T-DS-50: the card is centred in `[statusTop + 8, limit]`, where limit is `keyboardTop − 8` with the
     * keyboard up or `windowHeight − systemBottom − 16` without it. When [cardHeight] does not fit, the card
     * takes the whole band and the field area shrinks to `band − title − buttons` (it scrolls).
     */
    fun fit(
        windowHeight: Float,
        statusTop: Float,
        systemBottom: Float,
        keyboardTop: Float?,
        cardHeight: Float,
        titleHeight: Float,
        buttonsHeight: Float,
    ): CreateCardFit {
        val top = statusTop + OverlayGaps.top.value
        val limit = keyboardTop?.let { it - OverlayGaps.aboveKeyboard.value } ?: (windowHeight - systemBottom - BOTTOM_GAP_DP)
        val band = (limit - top).coerceAtLeast(0f)
        val height = cardHeight.coerceAtMost(band)
        val cardTop = top + (band - height) / 2f
        val fields = (height - titleHeight - buttonsHeight).coerceAtLeast(0f)
        return CreateCardFit(cardTop, cardTop + height, fields)
    }
}

/** Copy of the create card and rows (DS-72, U-15; titles beyond `새 인물 · 장소 · 용어 · 세력` are Q-DS-30). */
object CreateCardCopy {
    const val CANCEL = "취소"
    const val CREATE_AND_OPEN = "만들고 열기"
    const val CREATE = "만들기"
    const val NEW = "새"
    const val WORD_CHARACTER = "인물"
    const val WORD_PLACE = "장소"
    const val WORD_TERM = "용어"
    const val WORD_FACTION = "세력"
    const val WORD_EVENT = "사건"
    const val WORD_ASSET = "에셋"
    const val WORD_TAG = "태그"
    const val WORD_RELATION_KIND = "관계 종류"
    const val WORD_PAGE = "페이지"
    const val WORD_CANVAS = "캔버스"
    const val FIELD_NAME = "이름"
    const val FIELD_IMAGE = "이미지"
    const val FIELD_DISPLAY_NAME = "표시 이름"
    const val FIELD_ONE_LINE_INTRO = "한 줄 소개"
    const val FIELD_ONE_LINE_NOTE = "한 줄 설명"
    const val FIELD_TAGS = "태그"
    const val FIELD_PARENT_REGION = "상위 지역"
    const val FIELD_ALIASES = "다른 이름"
    const val FIELD_TIME = "때"
    const val FIELD_PEOPLE = "관련 인물"
    const val FIELD_ASSET_KIND = "종류"
    const val FIELD_FILE = "파일"
    const val FIELD_COLOR = "색"
    const val FIELD_BOTH_SIDES = "양쪽에서 부르는 이름"

    fun newOf(word: String): String = "$NEW $word"

    /** `‘세라’ 새 인물`. */
    fun newNamed(query: String, word: String): String = "‘$query’ ${newOf(word)}"

    val all: List<String> = listOf(
        CANCEL, CREATE_AND_OPEN, CREATE, NEW, WORD_CHARACTER, WORD_PLACE, WORD_TERM, WORD_FACTION, WORD_EVENT, WORD_ASSET,
        WORD_TAG, WORD_RELATION_KIND, WORD_PAGE, WORD_CANVAS, FIELD_NAME, FIELD_IMAGE, FIELD_DISPLAY_NAME,
        FIELD_ONE_LINE_INTRO, FIELD_ONE_LINE_NOTE, FIELD_TAGS, FIELD_PARENT_REGION, FIELD_ALIASES, FIELD_TIME,
        FIELD_PEOPLE, FIELD_ASSET_KIND, FIELD_FILE, FIELD_COLOR, FIELD_BOTH_SIDES,
    ) + CreateKind.entries.map { CreateCard.title(it) } + newNamed("세라", WORD_CHARACTER)
}

/**
 * DS-72 create card overlay: a centred card over the `shade` scrim, `raise`, radius 20, 1 dp `line` inset,
 * float shadow, padding 20; cover width = window − 2 × 16, inner max 420. Title pinned on top, [fields]
 * scroll in between, buttons pinned below. Its bottom stays 8 above the keyboard (or `systemBottom + 16`).
 * `취소` and back discard; a scrim tap closes only while [dismissOnScrim]. In [CreateCardMode.PICKER] only
 * `취소 · 만들기` show. Motion: the dialog host's enter / exit.
 */
@Composable
fun CreateCardOverlay(
    title: String,
    mode: CreateCardMode,
    canCreate: Boolean,
    onCancel: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
    onCreateAndOpen: () -> Unit = {},
    dismissOnScrim: Boolean = true,
    inner: Boolean? = null,
    fields: @Composable ColumnScope.() -> Unit,
) {
    val wide = inner ?: (LocalConfiguration.current.screenWidthDp >= INNER_WIDTH_DP)
    // The activity's own insets, in case the dialog window reports none (as FloatingDialog does).
    val outsideBottom = systemBottomWithoutKeyboard()
    val outsideTop = statusTop()
    val outsideKeyboard = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    ScrimDialog(onDismissRequest = onCancel, contentAlignment = Alignment.TopCenter, dismissOnScrim = dismissOnScrim) {
        val keyboard = maxOf(outsideKeyboard, WindowInsets.ime.asPaddingValues().calculateBottomPadding())
        val bottom = if (keyboard > 0.dp) {
            keyboard + OverlayGaps.aboveKeyboard
        } else {
            maxOf(outsideBottom, systemBottomWithoutKeyboard()) + CreateCard.BOTTOM_GAP_DP.dp
        }
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = maxOf(outsideTop, statusTop()) + OverlayGaps.top, bottom = bottom),
            contentAlignment = Alignment.Center,
        ) {
            CreateCardBody(title, mode, canCreate, onCancel, onCreate, onCreateAndOpen, modifier, wide, fields)
        }
    }
}

/** The card itself, usable in previews and other hosts. */
@Composable
fun CreateCardBody(
    title: String,
    mode: CreateCardMode,
    canCreate: Boolean,
    onCancel: () -> Unit,
    onCreate: () -> Unit,
    onCreateAndOpen: () -> Unit,
    modifier: Modifier = Modifier,
    inner: Boolean = false,
    fields: @Composable ColumnScope.() -> Unit,
) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(20.dp)
    val cancel = LocalDialogDismiss.current ?: onCancel
    Column(
        modifier
            .then(if (inner) Modifier.widthIn(max = CreateCard.MAX_WIDTH_INNER_DP.dp) else Modifier.padding(horizontal = CreateCard.SIDE_DP.dp))
            .fillMaxWidth()
            .tokenShadow(shape, CivilightTheme.elevation.float(colors.isDark))
            .clip(shape)
            .background(colors.raise)
            .insetOutline(colors.line, 1.dp, shape)
            .padding(CreateCard.PADDING_DP.dp)
            .semantics { paneTitle = title },
    ) {
        CivilightText(title, CivilightTheme.type.dialogTitle, color = colors.ink)
        Column(
            Modifier
                .weight(1f, fill = false)
                .padding(top = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = fields,
        )
        ButtonRow(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            CreateCard.buttons(mode).forEach { b ->
                when (b) {
                    CreateButton.CANCEL -> CivilightButton(b.label, cancel, Modifier.weight(1f), variant = ButtonVariant.GHOST)
                    CreateButton.CREATE_AND_OPEN ->
                        CivilightButton(b.label, onCreateAndOpen, Modifier.weight(1f), variant = ButtonVariant.GHOST, enabled = canCreate)
                    CreateButton.CREATE -> CivilightButton(b.label, onCreate, Modifier.weight(1f), variant = ButtonVariant.PRIMARY, enabled = canCreate)
                }
            }
        }
    }
}

/**
 * The card's fields for [state]: text fields are drawn here with a visible label above (the name takes focus
 * on open, caret at its end, so a picker's query is ready to edit); every other type goes to [slot].
 */
@Composable
fun ColumnScope.CreateCardFields(
    state: CreateCardState,
    onValueChange: (key: String, value: String) -> Unit,
    slot: @Composable (CreateField) -> Unit = {},
    focusName: Boolean = true,
) {
    val focus = remember { FocusRequester() }
    state.kind.fields.forEach { field ->
        when (field.type) {
            CreateFieldType.TEXT -> CivilightTextField(
                value = state.values[field.key].orEmpty(),
                onValueChange = { onValueChange(field.key, it) },
                label = field.label,
                modifier = if (field.key == CreateFields.NAME) Modifier.focusRequester(focus) else Modifier,
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CivilightText(field.label, CivilightTheme.type.label, color = CivilightTheme.colors.ink2)
                slot(field)
            }
        }
    }
    if (focusName) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

/**
 * U-15.1 / U-15.3 create row (DS-72, class b): 48 tall, icon `plus` 18 + [label] in `acc`, pressed per DS-68,
 * no chevron. Accessibility name = the label. Not drawn in read-only projects (the caller leaves it out).
 */
@Composable
fun CreateRow(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .pressable(onClick = onClick, hitTarget = HitTarget.BUTTON, scaleOnPress = false)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp)
            .semantics(mergeDescendants = true) { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CivilightIcon(IconId.Plus, contentDescription = null, size = 18.dp, tint = colors.acc)
        CivilightText(label, CivilightTheme.type.itemTitle, color = colors.acc, maxLines = 1)
    }
}

/** List top create row for [kind] (`＋ 새 인물`). */
@Composable
fun ListCreateRow(kind: CreateKind, onClick: () -> Unit, modifier: Modifier = Modifier) {
    CreateRow(CreateCard.rowLabel(kind), onClick, modifier)
}

/** Picker top create row: `＋ 새 인물`, or `＋ ‘세라’ 새 인물` when [query] matches nothing ([matched] false). */
@Composable
fun PickerCreateRow(kind: CreateKind, query: String?, matched: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    CreateRow(CreateCard.rowLabel(kind, query, matched), onClick, modifier)
}

private const val INNER_WIDTH_DP = 600
