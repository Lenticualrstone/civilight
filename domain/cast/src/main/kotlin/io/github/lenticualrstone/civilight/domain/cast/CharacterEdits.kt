package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.cast.Tag
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.text.DocBlock
import io.github.lenticualrstone.civilight.model.text.DocBlockKind
import io.github.lenticualrstone.civilight.model.text.RecorderSource
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.TextRun

/** Where `카드에 넣기` puts a recorder memo in a character (CST-49). */
sealed interface CharacterTab {
    data object Speech : CharacterTab
    data object Secret : CharacterTab

    /** A `긴 글` field (a numbered section of `개요`). */
    data class LongField(val defKey: Key) : CharacterTab
}

/** Creating characters and editing their fixed fields (CST-05, CST-11 … CST-13, CST-39, CST-49). */
object CharacterEdits {
    const val CREATE_TITLE = "인물 추가"
    const val CREATE_ACTION = "넣기"

    /** Live characters in list order. */
    fun live(doc: ProjectDoc): List<Character> =
        doc.cast.characters.filter { it.deletedAt == null }.sortedWith(compareBy<Character>({ it.order }, { it.key }))

    /**
     * A new character: new key, the next record number (never reused), empty fields, no relations;
     * placed after the last character. Only the name is required.
     */
    fun create(doc: ProjectDoc, name: String, ctx: EditContext, oneLiner: String? = null): Pair<ProjectDoc, Character> {
        val n = name.trim().nfc()
        require(n.isNotEmpty()) { "character name is empty" }
        val counters = doc.project.counters
        val character = Character(
            key = ctx.newKey(),
            recordNo = counters.nextCharacterRecordNo,
            name = n,
            oneLiner = oneLiner?.trim()?.nfc()?.takeIf { it.isNotEmpty() },
            order = OrderKeys.after(doc.cast.characters.maxOfOrNull { it.order }),
            updatedAt = ctx.now,
            updatedBy = ctx.device,
        )
        val project = doc.project.copy(counters = counters.copy(nextCharacterRecordNo = counters.nextCharacterRecordNo + 1))
        return doc.copy(project = project, cast = doc.cast.copy(characters = doc.cast.characters + character)) to character
    }

    fun update(doc: ProjectDoc, key: Key, ctx: EditContext, change: (Character) -> Character): ProjectDoc = doc.copy(
        cast = doc.cast.copy(characters = doc.cast.characters.map {
            if (it.key == key) change(it).copy(updatedAt = ctx.now, updatedBy = ctx.device) else it
        }),
    )

    fun rename(doc: ProjectDoc, key: Key, name: String, ctx: EditContext): ProjectDoc {
        val n = name.trim().nfc()
        require(n.isNotEmpty()) { "character name is empty" }
        return update(doc, key, ctx) { it.copy(name = n) }
    }

    fun setOneLiner(doc: ProjectDoc, key: Key, text: String?, ctx: EditContext): ProjectDoc =
        update(doc, key, ctx) { it.copy(oneLiner = text?.trim()?.nfc()?.takeIf { s -> s.isNotEmpty() }) }

    fun setVoiceCode(doc: ProjectDoc, key: Key, code: String?, ctx: EditContext): ProjectDoc =
        update(doc, key, ctx) { it.copy(voiceCode = code?.trim()?.takeIf { s -> s.isNotEmpty() }) }

    /** `숨기기` / `숨김 풀기` (CST-39). */
    fun setHidden(doc: ProjectDoc, key: Key, hidden: Boolean, ctx: EditContext): ProjectDoc = update(doc, key, ctx) { it.copy(hidden = hidden) }

    fun setSpeech(doc: ProjectDoc, key: Key, body: RichDoc, ctx: EditContext): ProjectDoc = update(doc, key, ctx) { it.copy(speechBody = body) }

    fun setSecret(doc: ProjectDoc, key: Key, body: RichDoc, ctx: EditContext): ProjectDoc = update(doc, key, ctx) { it.copy(secretBody = body) }

    // --- Tags (CST-12) ---

    /** Ordered tags of the character. */
    fun setTags(doc: ProjectDoc, key: Key, tagKeys: List<Key>, ctx: EditContext): ProjectDoc =
        update(doc, key, ctx) { it.copy(tagKeys = tagKeys.distinct()) }

    /** Adds a project tag (names are unique per project: an existing name returns that tag). */
    fun addTag(doc: ProjectDoc, name: String, ctx: EditContext): Pair<ProjectDoc, Tag> {
        val n = name.trim().nfc()
        require(n.isNotEmpty()) { "tag name is empty" }
        doc.project.tags.firstOrNull { it.name.nfc() == n }?.let { return doc to it }
        val tag = Tag(ctx.newKey(), n, OrderKeys.after(doc.project.tags.maxOfOrNull { it.order }), ctx.now, ctx.device)
        return doc.copy(project = doc.project.copy(tags = doc.project.tags + tag)) to tag
    }

    /** Project tags in order. */
    fun tags(doc: ProjectDoc): List<Tag> = doc.project.tags.sortedWith(compareBy<Tag>({ it.order }, { it.key }))

    // --- Recorder (CST-49) ---

    /** First sentence of a text (`한 줄 소개` of `인물로 만들기`). */
    fun firstSentence(text: String): String {
        val t = text.trim()
        val end = Regex("""[.?!。…](?=\s|$)""").find(t)?.range?.last
        return (if (end != null) t.substring(0, end + 1) else t.lineSequence().first()).trim()
    }

    /** A `⌁ 작가 메모` paragraph with its source mark. */
    fun memoParagraph(text: String, source: RecorderSource, ctx: EditContext, after: RichDoc): DocBlock = DocBlock(
        key = ctx.newKey(),
        order = OrderKeys.after(after.maxOfOrNull { it.order }),
        kind = DocBlockKind.MEMO.open(),
        runs = listOf(TextRun.Plain(text)),
        recorderSource = source,
    )

    /**
     * `카드에 넣기` into a character tab: the text is copied to the end as a `⌁` paragraph; the
     * record stays in the library.
     */
    fun appendMemo(doc: ProjectDoc, key: Key, tab: CharacterTab, text: String, source: RecorderSource, ctx: EditContext): ProjectDoc =
        when (tab) {
            CharacterTab.Speech -> update(doc, key, ctx) { it.copy(speechBody = it.speechBody + memoParagraph(text, source, ctx, it.speechBody)) }
            CharacterTab.Secret -> update(doc, key, ctx) { it.copy(secretBody = it.secretBody + memoParagraph(text, source, ctx, it.secretBody)) }
            is CharacterTab.LongField -> update(doc, key, ctx) { c ->
                val current = (c.fields[tab.defKey]?.payload as? FieldPayload.Rich)?.doc.orEmpty()
                val next = current + memoParagraph(text, source, ctx, current)
                c.copy(fields = c.fields + (tab.defKey to FieldValue.of(FieldPayload.Rich(next))))
            }
        }

    /**
     * `⋯ › 인물로 만들기`: a new character whose `한 줄 소개` is the record's first sentence, with the
     * text as a `⌁` paragraph at the end of `개요` (the first `긴 글` field; [PROPOSED], the model
     * has no separate 개요 body).
     */
    fun createFromRecord(doc: ProjectDoc, name: String, recordText: String, source: RecorderSource, ctx: EditContext): Pair<ProjectDoc, Character> {
        val (created, character) = create(doc, name, ctx, oneLiner = firstSentence(recordText))
        val overviewField = created.index.fieldDefs(io.github.lenticualrstone.civilight.model.cast.EntityKind.CHARACTER)
            .firstOrNull { it.deletedAt == null && it.type.known == FieldType.LONG_TEXT && it.scopeCharacterKey == null && !it.isSecret }
            ?: return created to character
        val withMemo = appendMemo(created, character.key, CharacterTab.LongField(overviewField.key), recordText.trim(), source, ctx)
        return withMemo to withMemo.index.character(character.key)!!
    }
}
