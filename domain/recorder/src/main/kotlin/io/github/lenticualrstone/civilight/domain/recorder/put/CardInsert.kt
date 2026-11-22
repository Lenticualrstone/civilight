package io.github.lenticualrstone.civilight.domain.recorder.put

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEditContext
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.Toast
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.link.Targets
import io.github.lenticualrstone.civilight.domain.recorder.link.isCard
import io.github.lenticualrstone.civilight.domain.recorder.text.Hangul
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.PlacementKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.text.DocBlock
import io.github.lenticualrstone.civilight.model.text.DocBlockKind
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.RichDoc

/** A card offered by `카드에 넣기` step 2. */
data class CardChoice(val target: TargetRef, val name: String)

/**
 * `카드에 넣기` (LRC-35, CST-49): a `⌁ 작가 메모` paragraph with the copied text appended at the end
 * of the chosen tab or field, with its source mark. There is no fixed "card memo" field.
 */
object CardInsert {
    /**
     * Step 2: the live cards (인물 · 장소 · 용어 · 세력) of the project, locked hidden ones left out,
     * filtered by name or 초성.
     */
    fun cards(env: RecorderEnv, projectKey: Key, query: String = ""): List<CardChoice> {
        val doc = env.project(projectKey)?.doc ?: return emptyList()
        val q = Hangul.fold(query)
        fun visible(kind: RecorderTargetKind, key: Key) = Targets.isOfferable(env, TargetRef(projectKey, kind, key))
        fun matches(name: String) = q.isEmpty() || Hangul.fold(name).contains(q) ||
            (Hangul.isInitialsQuery(q) && Hangul.initials(name).contains(q.replace(" ", "")))
        val all = doc.cast.characters.sortedWith(compareBy({ it.order }, { it.key })).map { Triple(RecorderTargetKind.CHARACTER, it.key, it.name) } +
            doc.world.places.sortedWith(compareBy({ it.order }, { it.key })).map { Triple(RecorderTargetKind.PLACE, it.key, it.name) } +
            doc.world.terms.sortedWith(compareBy({ it.order }, { it.key })).map { Triple(RecorderTargetKind.TERM, it.key, it.name) } +
            doc.world.factions.sortedWith(compareBy({ it.order }, { it.key })).map { Triple(RecorderTargetKind.FACTION, it.key, it.name) }
        return all.filter { (kind, key, name) -> visible(kind, key) && matches(name) }
            .map { (kind, key, name) -> CardChoice(TargetRef(projectKey, kind, key), name) }
    }

    /** Step 3: the tabs of the chosen card. */
    fun tabs(doc: ProjectDoc, card: TargetRef): List<CardTab> = CardDocs.tabs(doc, card.kind, card.key)

    fun memoParagraph(record: RecorderRecord, doc: ProjectDoc, after: RichDoc, ctx: RecorderEditContext): DocBlock = DocBlock(
        key = ctx.newKey(),
        order = OrderKeys.after(after.maxOfOrNull { it.order }),
        kind = DocBlockKind.MEMO.open(),
        runs = Put.copyRuns(record.runs, doc.key, LinkTargetKind.DOCUMENT, ctx),
        recorderSource = Put.sourceOf(record),
    )

    fun insert(doc: ProjectDoc, library: RecorderLibrary, recordKey: Key, card: TargetRef, tabId: String, ctx: RecorderEditContext): Insertion {
        val record = Put.source(library, recordKey)
        Put.requireText(record)
        if (!card.kind.isCard || card.projectKey != doc.key) throw InsertionException("not a card of this project")
        if (!CardDocs.isLive(doc, card.kind, card.key)) throw InsertionException("card ${card.key} is not live")
        val tab = CardDocs.tabs(doc, card.kind, card.key).firstOrNull { it.id == tabId }
            ?: throw InsertionException("card ${card.key} has no tab $tabId")
        val fieldKey = if (tabId == CardDocs.OVERVIEW) CardDocs.overviewField(doc, card.key)?.key else tabId
        val hadValue = fieldKey != null && fieldValueExists(doc, card, fieldKey)

        var paragraph: DocBlock? = null
        val nextDoc = CardDocs.updateTab(doc, card.kind, card.key, tabId, ctx.now, ctx.device) { current ->
            val p = memoParagraph(record, doc, current, ctx)
            paragraph = p
            current + p
        } ?: throw InsertionException("card ${card.key} has no tab $tabId")
        val created = paragraph!!

        val name = CardDocs.name(doc, card.kind, card.key).orEmpty()
        val label = "$name${RecorderCopy.SEP}${tab.label}"
        val (nextLibrary, placement, addedLink) = Put.commit(
            library, record, doc.key, PlacementKind.CARD_PARAGRAPH, created.key, card.key, tabId, label, card, ctx,
        )
        val isField = fieldKey != null && fieldKey != CardDocs.SPEECH && fieldKey != CardDocs.SECRET && fieldKey != CardDocs.DESCRIPTION
        return Insertion(
            doc = nextDoc,
            library = nextLibrary,
            placement = placement,
            toast = Toast(RecorderCopy.TODO_CARD_TOAST, listOf(RecorderCopy.UNDO)),
            undo = Put.undoToken(
                record, placement, addedLink,
                CreatedContent.CardParagraph(card.key, created.key, if (isField && !hadValue) fieldKey else null),
            ),
        )
    }

    private fun fieldValueExists(doc: ProjectDoc, card: TargetRef, fieldKey: Key): Boolean {
        val fields = when (card.kind) {
            RecorderTargetKind.CHARACTER -> doc.index.character(card.key)?.fields
            RecorderTargetKind.PLACE -> doc.index.place(card.key)?.fields
            RecorderTargetKind.TERM -> doc.index.term(card.key)?.fields
            RecorderTargetKind.FACTION -> doc.index.faction(card.key)?.fields
            else -> null
        } ?: return false
        return fields.containsKey(fieldKey)
    }
}
