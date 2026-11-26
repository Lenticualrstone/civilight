package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.cast.Tag
import io.github.lenticualrstone.civilight.model.cast.TagScope
import io.github.lenticualrstone.civilight.model.cast.TagScopes
import io.github.lenticualrstone.civilight.model.cast.fits
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
/**
 * Tag settings row counts: `인물 0 · 이미지 1` (55 §3.4). (2026-11-26) Terms holding the tag add
 * `용어 n` after `인물` when any; the line of a tag no term holds is unchanged.
 */
data class TagUsage(val characters: Int, val images: Int, val terms: Int = 0) {
    val line: String get() = if (terms == 0) "인물 $characters · 이미지 $images" else "인물 $characters · 용어 $terms · 이미지 $images"
}

object CharacterEdits {
    const val CREATE_TITLE = "인물 추가"
    const val CREATE_ACTION = "넣기"

    /** Live characters in list order. */
    fun live(doc: ProjectDoc): List<Character> =
        doc.cast.characters.filter { it.deletedAt == null }.sortedWith(compareBy<Character>({ it.order }, { it.key }))

    /**
     * A new character: new key, the next record number (never reused), empty fields, no relations;
     * placed after the last character. Only the name is required.
     *
     * (0.2.0, CST-66) The create card also passes [displayNameBase] (stored as the base display name,
     * with no change record) and [tagKeys] (only live tags are kept). One edit, one undo step.
     */
    fun create(
        doc: ProjectDoc,
        name: String,
        ctx: EditContext,
        oneLiner: String? = null,
        displayNameBase: String? = null,
        tagKeys: List<Key> = emptyList(),
    ): Pair<ProjectDoc, Character> {
        val n = name.trim().nfc()
        require(n.isNotEmpty()) { "character name is empty" }
        val counters = doc.project.counters
        val live = tags(doc).mapTo(HashSet()) { it.key } // (2026-11-26) character-scope or all only
        val character = Character(
            key = ctx.newKey(),
            recordNo = counters.nextCharacterRecordNo,
            name = n,
            displayNameBase = displayNameBase?.trim()?.nfc()?.takeIf { it.isNotEmpty() },
            oneLiner = oneLiner?.trim()?.nfc()?.takeIf { it.isNotEmpty() },
            tagKeys = tagKeys.distinct().filter { it in live },
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

    /**
     * Ordered tags of the character. (2026-11-26) A live tag a character may not carry (scope term, place or
     * faction) is dropped; trashed tags' keys stay (N-44), as do keys of tags not in the project. A live tag
     * whose scope is unknown (newer file) is never attached here, and one already held stays untouched.
     */
    fun setTags(doc: ProjectDoc, key: Key, tagKeys: List<Key>, ctx: EditContext): ProjectDoc =
        update(doc, key, ctx) { c -> c.copy(tagKeys = TagScopes.settleInPickOrder(doc, TagScope.CHARACTER, tagKeys, c.tagKeys, keepMissing = true)) }

    /**
     * Adds a project tag (names are unique among live tags: an existing live name returns that tag).
     * (0.2.0, N-44) A trashed tag of the same name is not reused; it stays in the trash. New tags of the
     * create card go through `domain:actions` `TagActions.create` (DECISIONS §1).
     *
     * (2026-11-26) Names are unique across every scope, so the returned tag may be one a character may not
     * carry (a `용어 전용` tag of that name): callers check `tag.fits(TagScope.CHARACTER)` before attaching.
     * A new tag here is `인물` scope.
     */
    fun addTag(doc: ProjectDoc, name: String, ctx: EditContext): Pair<ProjectDoc, Tag> {
        val n = name.trim().nfc()
        require(n.isNotEmpty()) { "tag name is empty" }
        TagScopes.liveNamed(doc, n)?.let { return doc to it }
        val tag = Tag(ctx.newKey(), n, OrderKeys.after(doc.project.tags.maxOfOrNull { it.order }), ctx.now, ctx.device)
        return doc.copy(project = doc.project.copy(tags = doc.project.tags + tag)) to tag
    }

    /**
     * Project tags in order. (0.2.0, N-44, CST-75) Live tags only: a trashed tag appears nowhere (no chip,
     * section, field chip, picker chip or tag search hit) until it is restored.
     *
     * (2026-11-26) Only tags a character may carry (scope `인물` or `모든 항목`; every older tag is `인물`):
     * this is what every character list, chip, section and picker shows. Name checks use
     * `TagScopes.live` / `TagScopes.liveNamed` (all scopes).
     */
    fun tags(doc: ProjectDoc): List<Tag> = TagScopes.tagsFor(doc, TagScope.CHARACTER)

    /** (0.2.0, N-44) The live tag of [tagKey], or null when it is gone or in the trash. */
    fun liveTag(doc: ProjectDoc, tagKey: Key): Tag? = doc.index.tag(tagKey)?.takeIf { it.isLive }

    /** (0.2.0, N-44) Names of the character's live tags, in the character's order. */
    fun liveTagNames(doc: ProjectDoc, character: Character): List<String> = character.tagKeys.mapNotNull { liveTag(doc, it)?.name }

    /** Renames a tag (names stay unique: an existing other tag with the name refuses with null). Keys stay, so nothing else changes. */
    fun renameTag(doc: ProjectDoc, tagKey: Key, name: String, ctx: EditContext): ProjectDoc? {
        val n = name.trim().nfc()
        require(n.isNotEmpty()) { "tag name is empty" }
        if (TagScopes.liveNamed(doc, n, except = tagKey) != null) return null
        return doc.copy(
            project = doc.project.copy(
                tags = doc.project.tags.map { if (it.key == tagKey) it.copy(name = n, updatedAt = ctx.now, updatedBy = ctx.device) else it },
            ),
        )
    }

    /**
     * Removes a tag from the project and, in the same edit, from every character, every term (2026-11-26)
     * and every gallery image that holds it (55 §3.4, T-GAL-31); live and trashed rows alike.
     */
    fun removeTag(doc: ProjectDoc, tagKey: Key, ctx: EditContext): ProjectDoc = doc.copy(
        project = doc.project.copy(tags = doc.project.tags.filterNot { it.key == tagKey }),
        world = if (doc.world.terms.none { tagKey in it.tagKeys }) doc.world else doc.world.copy(
            terms = doc.world.terms.map {
                if (tagKey in it.tagKeys) it.copy(tagKeys = it.tagKeys - tagKey, updatedAt = ctx.now, updatedBy = ctx.device) else it
            },
        ),
        cast = doc.cast.copy(
            characters = doc.cast.characters.map {
                if (tagKey in it.tagKeys) it.copy(tagKeys = it.tagKeys - tagKey, updatedAt = ctx.now, updatedBy = ctx.device) else it
            },
        ),
        gallery = doc.gallery.mapValues { (_, file) ->
            if (file.items.none { tagKey in it.tagKeys }) {
                file
            } else {
                file.copy(items = file.items.map { if (tagKey in it.tagKeys) it.copy(tagKeys = it.tagKeys - tagKey, updatedAt = ctx.now, updatedBy = ctx.device) else it })
            }
        },
    )

    /** How many live characters, live terms (2026-11-26) and live gallery images hold [tagKey] (tag settings row, T-GAL-31). */
    fun tagUsage(doc: ProjectDoc, tagKey: Key): TagUsage = TagUsage(
        characters = doc.cast.characters.count { it.deletedAt == null && tagKey in it.tagKeys },
        images = doc.galleryItems.count { doc.liveness.isLive(it) && tagKey in it.tagKeys },
        terms = doc.world.terms.count { it.deletedAt == null && tagKey in it.tagKeys },
    )

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
