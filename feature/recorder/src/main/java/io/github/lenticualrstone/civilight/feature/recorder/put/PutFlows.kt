package io.github.lenticualrstone.civilight.feature.recorder.put

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.put.CardChoice
import io.github.lenticualrstone.civilight.domain.recorder.put.CardInsert
import io.github.lenticualrstone.civilight.domain.recorder.put.CardTab
import io.github.lenticualrstone.civilight.domain.recorder.put.DestinationOption
import io.github.lenticualrstone.civilight.domain.recorder.put.DestinationStep
import io.github.lenticualrstone.civilight.domain.recorder.put.Destinations
import io.github.lenticualrstone.civilight.domain.recorder.put.PageInsert
import io.github.lenticualrstone.civilight.domain.recorder.put.PositionRow
import io.github.lenticualrstone.civilight.domain.recorder.put.PositionSheets
import io.github.lenticualrstone.civilight.domain.recorder.put.PutKind
import io.github.lenticualrstone.civilight.feature.recorder.RecorderUiCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.ui.picker.PickerModels

/**
 * The `원고에 넣기` sheet as drawn (LRC-33): while choosing, the title `원고에 넣기` and the project
 * menu; once a project is chosen, `원고에 넣기 · <project> ▾` and its rows (facts only, none
 * preselected). While the biometric prompt runs ([DestinationStep.AwaitingAuth]) no row shows.
 */
data class PutSheetView(
    val title: String,
    val step: DestinationStep,
    val projects: List<DestinationOption>,
    val rows: List<PositionRow>,
) {
    val chosenProject: Key? get() = (step as? DestinationStep.Chosen)?.projectKey
}

/** A card offered in `카드에 넣기` step 2: the card and its kind word (`인물`, `장소` …). */
data class CardRow(val choice: CardChoice, val kindWord: String)

/**
 * `카드에 넣기` (LRC-35): project → card (인물 · 장소 · 용어 · 세력, search by name / 초성, locked
 * hidden ones left out) → tab (character `개요 · 말투 · 비밀 · <긴 글>`, world cards `설명 · <긴 글>`).
 * [card] is the chosen card (null while the list shows); a tab row inserts at once.
 */
data class PutCardView(
    val title: String,
    val step: DestinationStep,
    val projects: List<DestinationOption>,
    val cards: List<CardRow>,
    val card: CardRow?,
    val tabs: List<CardTab>,
) {
    val chosenProject: Key? get() = (step as? DestinationStep.Chosen)?.projectKey
}

/** `페이지로 만들기` (LRC-36): project, then `이름` prefilled with the record's first sentence (`ED`). */
data class PutPageView(
    val title: String,
    val step: DestinationStep,
    val projects: List<DestinationOption>,
    val prefill: String,
) {
    val chosenProject: Key? get() = (step as? DestinationStep.Chosen)?.projectKey

    /** The page can be made: a name typed, or the prefill when the field is left blank. */
    fun canMake(name: String): Boolean = step is DestinationStep.Chosen && (name.isNotBlank() || prefill.isNotBlank())
}

/**
 * A row of `메모 칸에 넣기` step 2 (LRC-37): a chapter, episode or scene in story order, indented by
 * level. [locked] = a hidden scene shown masked (`1-7 씬 4 · 숨긴 씬`) while the project's items are
 * locked: tapping it asks for the prompt first, it never writes into hidden content unseen.
 */
data class MemoFieldRow(val unitKey: Key, val title: String, val depth: Int, val locked: Boolean)

/** `메모 칸에 넣기` (LRC-37): project, then the structure tree (장 · 에피소드 · 씬); a row inserts at once. */
data class PutMemoFieldView(
    val title: String,
    val step: DestinationStep,
    val projects: List<DestinationOption>,
    val units: List<MemoFieldRow>,
) {
    val chosenProject: Key? get() = (step as? DestinationStep.Chosen)?.projectKey
}

/** `인물로 만들기` (LRC-38): project, then `이름` (required, empty at start `[PROPOSED]`). */
data class PutCharacterView(
    val title: String,
    val step: DestinationStep,
    val projects: List<DestinationOption>,
) {
    val chosenProject: Key? get() = (step as? DestinationStep.Chosen)?.projectKey

    /** The character can be made: a project chosen and a name typed (no prefill to fall back on). */
    fun canMake(name: String): Boolean = step is DestinationStep.Chosen && name.isNotBlank()
}

/** The project step and rows of the 넣기 sheets (LRC-32 protected destinations, LRC-33 … LRC-38). */
object PutFlows {
    /** The sheet's words for [kind]: the option's own title (`원고에 넣기`, `카드에 넣기`, `페이지로 만들기`, `메모 칸에 넣기`, `인물로 만들기`). */
    fun kindTitle(kind: PutKind): String = when (kind) {
        PutKind.MANUSCRIPT -> RecorderCopy.PUT_MANUSCRIPT
        PutKind.CARD -> RecorderCopy.PUT_CARD
        PutKind.PAGE -> RecorderCopy.MAKE_PAGE
        PutKind.MEMO_FIELD -> RecorderCopy.PUT_MEMO_FIELD
        PutKind.CHARACTER -> RecorderCopy.MAKE_CHARACTER
    }

    /**
     * The title line: `<kind> · <project> ▾` once a project is chosen (the `▾` reopens the project
     * menu), the kind's words alone while choosing. `원고에 넣기` is rule 23 §4's line; the other
     * kinds follow it ([PROPOSED], no board draws their sheets).
     */
    fun title(kind: PutKind, env: RecorderEnv, step: DestinationStep): String {
        val chosen = (step as? DestinationStep.Chosen)?.projectKey ?: return kindTitle(kind)
        val project = env.project(chosen) ?: return kindTitle(kind)
        return "${kindTitle(kind)}${RecorderCopy.SEP}${Destinations.label(project, env)}${RecorderCopy.DROPDOWN}"
    }

    /**
     * The step a sheet opens with: [requested] (a restored sheet) or the record's only linked
     * project; a protected one goes to the prompt first ([DestinationStep.AwaitingAuth]); with no
     * single offered project the menu opens.
     */
    fun initialStep(library: RecorderLibrary, recordKey: Key, env: RecorderEnv, kind: PutKind, requested: Key? = null): DestinationStep {
        val project = requested ?: PositionSheets.initialProject(library, recordKey, env) ?: return DestinationStep.ChoosingProject
        return Destinations.choose(env, project, kind)
    }

    /**
     * The step from saveable state: [projectKey] = the destination being chosen (null = the menu is
     * open), [authedKey] = the project whose prompt already passed in this sheet. A protected project
     * not yet passed waits for the prompt; one that is no longer offered sends the user to the menu.
     */
    fun step(env: RecorderEnv, projectKey: Key?, authedKey: Key?, kind: PutKind): DestinationStep {
        val key = projectKey ?: return DestinationStep.ChoosingProject
        val chosen = Destinations.choose(env, key, kind)
        return if (chosen is DestinationStep.AwaitingAuth && authedKey == key) DestinationStep.Chosen(key) else chosen
    }

    /** The destination a sheet opens with: [requested] (the sheet key's), else the record's only linked project. */
    fun initialProject(library: RecorderLibrary, recordKey: Key, env: RecorderEnv, requested: Key? = null): Key? =
        requested ?: PositionSheets.initialProject(library, recordKey, env)

    fun view(library: RecorderLibrary, recordKey: Key, env: RecorderEnv, step: DestinationStep): PutSheetView = when (step) {
        DestinationStep.ChoosingProject ->
            PutSheetView(RecorderCopy.PUT_MANUSCRIPT, step, Destinations.options(env, PutKind.MANUSCRIPT), emptyList())
        is DestinationStep.AwaitingAuth -> PutSheetView(RecorderCopy.PUT_MANUSCRIPT, step, emptyList(), emptyList())
        is DestinationStep.Chosen -> {
            val sheet = PositionSheets.of(library, recordKey, step.projectKey, env)
            PutSheetView(sheet.title, step, emptyList(), sheet.rows)
        }
    }

    /** `카드에 넣기` as drawn now: the menu, the card list for [query], or the chosen card's tabs. */
    fun cardView(env: RecorderEnv, step: DestinationStep, query: String, card: TargetRef?): PutCardView {
        val title = title(PutKind.CARD, env, step)
        val projectKey = (step as? DestinationStep.Chosen)?.projectKey
            ?: return PutCardView(title, step, if (step is DestinationStep.ChoosingProject) Destinations.options(env, PutKind.CARD) else emptyList(), emptyList(), null, emptyList())
        val doc = env.project(projectKey)?.doc
        // The chosen card, while it is still offered (a card hidden or deleted meanwhile drops back to the list).
        val chosen = card?.takeIf { it.projectKey == projectKey }
            ?.let { c -> CardInsert.cards(env, projectKey).firstOrNull { it.target == c } }
        if (chosen != null && doc != null) {
            return PutCardView(title, step, emptyList(), emptyList(), CardRow(chosen, kindWord(chosen.target.kind)), CardInsert.tabs(doc, chosen.target))
        }
        val cards = CardInsert.cards(env, projectKey, query).map { CardRow(it, kindWord(it.target.kind)) }
        return PutCardView(title, step, emptyList(), cards, null, emptyList())
    }

    /** `페이지로 만들기` as drawn now: the menu, or the name step with its prefill. */
    fun pageView(library: RecorderLibrary, recordKey: Key, env: RecorderEnv, step: DestinationStep): PutPageView {
        val prefill = library.record(recordKey)?.let(PageInsert::prefillName).orEmpty()
        val projects = if (step is DestinationStep.ChoosingProject) Destinations.options(env, PutKind.PAGE) else emptyList()
        return PutPageView(title(PutKind.PAGE, env, step), step, projects, prefill)
    }

    /**
     * `메모 칸에 넣기` as drawn now: the menu, or the chosen project's live units in story order
     * (`core:ui` `PickerModels.units`, the 단위 고르기 rows). [hiddenOpen] = the project's items are
     * open (hidden scenes show their titles and take the line at once).
     */
    fun memoFieldView(env: RecorderEnv, step: DestinationStep, hiddenOpen: Boolean): PutMemoFieldView {
        val title = title(PutKind.MEMO_FIELD, env, step)
        val projectKey = (step as? DestinationStep.Chosen)?.projectKey
            ?: return PutMemoFieldView(title, step, if (step is DestinationStep.ChoosingProject) Destinations.options(env, PutKind.MEMO_FIELD) else emptyList(), emptyList())
        val doc = env.project(projectKey)?.doc ?: return PutMemoFieldView(title, step, emptyList(), emptyList())
        val rows = PickerModels.units(doc, hiddenOpen = hiddenOpen).items.map { item ->
            val unit = doc.index.unit(item.key)
            MemoFieldRow(item.key, item.title, item.depth, locked = !hiddenOpen && unit?.hidden == true)
        }
        return PutMemoFieldView(title, step, emptyList(), rows)
    }

    /** `인물로 만들기` as drawn now: the menu, or the name step. */
    fun characterView(env: RecorderEnv, step: DestinationStep): PutCharacterView =
        PutCharacterView(title(PutKind.CHARACTER, env, step), step, if (step is DestinationStep.ChoosingProject) Destinations.options(env, PutKind.CHARACTER) else emptyList())

    fun kindWord(kind: RecorderTargetKind): String = when (kind) {
        RecorderTargetKind.CHARACTER -> RecorderUiCopy.KIND_CHARACTER
        RecorderTargetKind.PLACE -> RecorderUiCopy.KIND_PLACE
        RecorderTargetKind.TERM -> RecorderUiCopy.KIND_TERM
        RecorderTargetKind.FACTION -> RecorderUiCopy.KIND_FACTION
        RecorderTargetKind.PAGE -> RecorderUiCopy.KIND_PAGE
        RecorderTargetKind.PROJECT -> RecorderUiCopy.KIND_PROJECT
        RecorderTargetKind.UNIT -> ""
    }
}
