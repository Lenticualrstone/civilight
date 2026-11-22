package io.github.lenticualrstone.civilight.domain.cast.refs

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.ItemRefs
import io.github.lenticualrstone.civilight.domain.cast.ResolvedItem
import io.github.lenticualrstone.civilight.domain.cast.label
import io.github.lenticualrstone.civilight.domain.cast.nameTagKind
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationEdits
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.trash.DeletionLogEntry
import io.github.lenticualrstone.civilight.model.trash.NameTag
import io.github.lenticualrstone.civilight.model.trash.NameTagState
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashReason
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Check-center row of a deleted item, one per item (CST-42). */
data class DeletedItemRow(
    val key: Key,
    /** `◇ 지운 인물 · 노아`, or `영구 삭제 · 노아` after permanent delete. */
    val label: String,
    val location: String,
    /** `3일 남음`; null after permanent delete. */
    val daysLeft: String?,
    /** `대사 14 · 씬 등장 2 · @ 3 · 레코더 1`. */
    val counts: String,
    /** One bulk button per reference kind, then `되살리기` (not after purge) and `이대로 두기`. */
    val actions: List<String>,
)

/** A trash row of a character (CST-48). */
data class TrashRow(val name: String, val daysLeft: String, val sub: String, val chips: List<String>)

/** `화자 한꺼번에 바꾸기` (CST-42): which lines changed, for one-step undo. */
data class SpeakerChange(val from: Key, val to: Key, val blockKeys: List<Key>, val optionKeys: List<Pair<Key, Key>>) {
    val count: Int get() = blockKeys.size + optionKeys.size
}

/** References of an item at delete time, kept so a restore can report what re-linked (CST-43). */
@Serializable
data class ReferenceSnapshot(
    val speakerBlocks: List<Key> = emptyList(),
    /** `blockKey:optionKey`. */
    val speakerOptions: List<String> = emptyList(),
    val appearanceScenes: List<Key> = emptyList(),
    val linkKeys: List<Key> = emptyList(),
)

/** Result of a restore: what re-linked and what the author had changed meanwhile. */
data class RestoreReport(val name: String, val relinkedLines: Int, val relinkedLinks: Int, val relinkedAppearances: Int, val keptLines: Int, val keptLinks: Int, val keptAppearances: Int) {
    /** `노아 되살림 · 다시 이어짐 · 대사 11 · @ 3 · 씬 등장 2 · 그대로 둔 것 · 대사 3`. */
    val toast: String
        get() {
            val relinked = parts(relinkedLines, relinkedLinks, relinkedAppearances)
            val kept = parts(keptLines, keptLinks, keptAppearances)
            return CastCopy.joinAll(
                listOf("$name 되살림") +
                    (if (relinked.isNotEmpty()) listOf("다시 이어짐") + relinked else emptyList()) +
                    (if (kept.isNotEmpty()) listOf("그대로 둔 것") + kept else emptyList()),
            )
        }

    private fun parts(lines: Int, links: Int, appearances: Int) = listOfNotNull(
        lines.takeIf { it > 0 }?.let { "대사 $it" },
        links.takeIf { it > 0 }?.let { "@ $it" },
        appearances.takeIf { it > 0 }?.let { "씬 등장 $it" },
    )
}

/** `◇ 같은 이름 2 · 노아` with `합치기` (CST-46): exact NFC equality only, never similarity. */
data class SameNameNotice(val name: String, val keys: List<Key>) {
    val label: String get() = "${CastCopy.SOFT_MARK} 같은 이름 ${keys.size} · $name"
    val action: String get() = "합치기"
}

/**
 * Cast's side of rule 20 for characters (CST-41 … CST-48): check and trash rows, bulk speaker
 * change, restore report, purge of owned rows with a name tag, same-name notice. Marking and
 * clearing trash marks is the trash domain's job.
 */
object DeletedItems {
    const val TRASH_SNAPSHOT_KEY = "castRefs"

    fun daysLeft(entry: TrashEntry, today: LocalDate): Long = ChronoUnit.DAYS.between(today, entry.purgeOn).coerceAtLeast(0)

    /** Check row for a trashed or permanently deleted item. */
    fun checkRow(
        doc: ProjectDoc,
        kind: EntityKind,
        key: Key,
        today: LocalDate,
        recorderLinks: List<RecorderLink> = emptyList(),
        hiddenUnlocked: Boolean = false,
    ): DeletedItemRow? {
        val resolved = ItemRefs.resolve(doc, kind, key, hiddenUnlocked)
        val refs = References.scan(doc, kind, key, recorderLinks).filter { it.live }
        val counts = DeleteImpact.countsLine(
            refs.count { it.kind == RefKind.SPEAKER }, refs.count { it.kind == RefKind.APPEARANCE },
            refs.count { it.kind == RefKind.LINK }, refs.count { it.kind == RefKind.RECORDER },
            refs.count { it.kind == RefKind.FIELD_LINK }, refs.count { it.kind == RefKind.SCENE_PLACE },
        )
        val bulk = listOfNotNull(
            "화자 한꺼번에 바꾸기".takeIf { refs.any { it.kind == RefKind.SPEAKER } },
            "장소 한꺼번에 바꾸기".takeIf { refs.any { it.kind == RefKind.SCENE_PLACE } },
            "@ 한꺼번에 바꾸기".takeIf { refs.any { it.kind == RefKind.LINK } },
            "연결 한꺼번에 바꾸기".takeIf { refs.any { it.kind == RefKind.FIELD_LINK } },
        )
        return when (resolved) {
            is ResolvedItem.Trashed -> {
                val entry = trashEntryOf(doc, kind, key)
                DeletedItemRow(
                    key = key,
                    label = "${CastCopy.SOFT_MARK} ${resolved.wideLabel}",
                    location = "휴지통",
                    daysLeft = entry?.let { "${daysLeft(it, today)}일 남음" },
                    counts = counts,
                    actions = bulk + listOf("되살리기", "이대로 두기"),
                )
            }
            is ResolvedItem.Tagged -> DeletedItemRow(
                key = key, label = resolved.wideLabel, location = "휴지통", daysLeft = null, counts = counts,
                actions = bulk + listOf("이대로 두기"),
            )
            is ResolvedItem.HiddenLocked -> {
                // Hidden items: counts only, under the hidden group.
                DeletedItemRow(key, "${CastCopy.SOFT_MARK} ${resolved.wideLabel}", "휴지통", null, counts, listOf("지문으로 보기"))
            }
            else -> null
        }
    }

    fun trashEntryOf(doc: ProjectDoc, kind: EntityKind, key: Key): TrashEntry? {
        val entryKey = when (kind) {
            EntityKind.CHARACTER -> doc.index.character(key)?.trashEntryKey
            EntityKind.PLACE -> doc.index.place(key)?.trashEntryKey
            EntityKind.TERM -> doc.index.term(key)?.trashEntryKey
            EntityKind.FACTION -> doc.index.faction(key)?.trashEntryKey
        }
        return entryKey?.let { doc.index.trashEntry(it) }
    }

    /**
     * Trash row of a character: `노아` · `3일 남음` · `인물 · 잿빛 새벽의 등불 · 대사 14 · 관계 1`;
     * hidden while locked `숨긴 인물` · `인물 · <프로젝트> · 되살리기는 지문 확인`; merged adds
     * `합침 → 노아`.
     */
    fun trashRow(doc: ProjectDoc, characterKey: Key, today: LocalDate, hiddenUnlocked: Boolean = false): TrashRow? {
        val c = doc.index.character(characterKey) ?: return null
        val entry = doc.index.trashEntry(c.trashEntryKey ?: return null) ?: return null
        val days = "${daysLeft(entry, today)}일 남음"
        val project = doc.project.name
        if (c.hidden && !hiddenUnlocked) {
            return TrashRow(CastCopy.HIDDEN_CHARACTER, days, CastCopy.join("인물", project, "되살리기는 지문 확인"), listOf("되살리기", "영구 삭제"))
        }
        val lines = References.scan(doc, EntityKind.CHARACTER, characterKey).count { it.live && it.kind == RefKind.SPEAKER }
        val relations = doc.cast.relations.count { r ->
            val other = when (characterKey) {
                r.endA.key -> r.endB
                r.endB.key -> r.endA
                else -> return@count false
            }
            doc.liveness.isEndLive(other)
        }
        val merged = if (entry.reason.known == TrashReason.MERGED) {
            entry.mergedIntoKey?.let { doc.index.character(it)?.name }?.let { "합침 → $it" }
        } else {
            null
        }
        val sub = CastCopy.joinAll(
            listOf("인물", project, lines.takeIf { it > 0 }?.let { "대사 $it" }, relations.takeIf { it > 0 }?.let { "관계 $it" }, merged),
        )
        return TrashRow(c.name, days, sub, listOf("되살리기", "영구 삭제"))
    }

    // --- Bulk speaker change ---

    /** Preview line `대사 14 · 노아 → 오웬`. */
    fun speakerPreview(doc: ProjectDoc, from: Key, to: Key): String {
        val count = References.scan(doc, EntityKind.CHARACTER, from).count { it.live && it.kind == RefKind.SPEAKER }
        val fromName = doc.index.character(from)?.name ?: doc.index.nameTag(from)?.name.orEmpty()
        return "대사 $count · $fromName → ${doc.index.character(to)?.name.orEmpty()}"
    }

    /**
     * `바꾸기`: live lines (or only [onlyBlocks]) spoken by [from] get [to] as speaker. Returns the
     * change for a one-step undo ([undoSpeakerChange]).
     */
    fun changeSpeaker(doc: ProjectDoc, from: Key, to: Key, ctx: EditContext, onlyBlocks: Set<Key>? = null): Pair<ProjectDoc, SpeakerChange> {
        val blocks = mutableListOf<Key>()
        val options = mutableListOf<Pair<Key, Key>>()
        val episodes = doc.episodes.mapValues { (_, file) ->
            file.copy(blocks = file.blocks.map { b ->
                if (!doc.liveness.isLive(b) || (onlyBlocks != null && b.key !in onlyBlocks)) return@map b
                var changed = b
                if (b.speakerKey == from) {
                    changed = changed.copy(speakerKey = to)
                    blocks += b.key
                }
                if (b.options.any { it.speakerKey == from }) {
                    changed = changed.copy(options = changed.options.map { o ->
                        if (o.speakerKey == from) {
                            options += b.key to o.key
                            o.copy(speakerKey = to)
                        } else {
                            o
                        }
                    })
                }
                if (changed !== b) changed.copy(updatedAt = ctx.now, updatedBy = ctx.by) else b
            })
        }
        return doc.copy(episodes = episodes) to SpeakerChange(from, to, blocks, options)
    }

    /** One-step undo of [changeSpeaker]. */
    fun undoSpeakerChange(doc: ProjectDoc, change: SpeakerChange): ProjectDoc {
        val blocks = change.blockKeys.toSet()
        val options = change.optionKeys.toSet()
        return doc.copy(episodes = doc.episodes.mapValues { (_, file) ->
            file.copy(blocks = file.blocks.map { b ->
                var next = if (b.key in blocks && b.speakerKey == change.to) b.copy(speakerKey = change.from) else b
                if (b.options.any { (b.key to it.key) in options }) {
                    next = next.copy(options = next.options.map { o ->
                        if ((b.key to o.key) in options && o.speakerKey == change.to) o.copy(speakerKey = change.from) else o
                    })
                }
                next
            })
        })
    }

    // --- Restore report ---

    /** References of a live character just before it is deleted. */
    fun snapshot(doc: ProjectDoc, characterKey: Key): ReferenceSnapshot {
        val refs = References.scan(doc, EntityKind.CHARACTER, characterKey).filter { it.live }
        return ReferenceSnapshot(
            speakerBlocks = refs.filter { it.kind == RefKind.SPEAKER && it.field == "speakerKey" }.map { it.ownerKey },
            speakerOptions = refs.filter { it.kind == RefKind.SPEAKER && it.field.startsWith("options:") }
                .map { "${it.ownerKey}:${it.field.removePrefix("options:")}" },
            appearanceScenes = refs.filter { it.kind == RefKind.APPEARANCE }.map { it.ownerKey },
            linkKeys = refs.filter { it.kind == RefKind.LINK }.map { it.field.removePrefix("link:") },
        )
    }

    /** The snapshot stored with a trash entry (`ext["castRefs"]`), or null. */
    fun snapshotOf(entry: TrashEntry): ReferenceSnapshot? = entry.ext?.get(TRASH_SNAPSHOT_KEY)?.let {
        io.github.lenticualrstone.civilight.model.json.CivilightJson.decodeElement(ReferenceSnapshot.serializer(), it)
    }

    /** The entry with [snapshot] stored in its extension slot. */
    fun withSnapshot(entry: TrashEntry, snapshot: ReferenceSnapshot): TrashEntry {
        val element = io.github.lenticualrstone.civilight.model.json.CivilightJson.encodeElement(ReferenceSnapshot.serializer(), snapshot)
        return entry.copy(ext = JsonObject((entry.ext ?: JsonObject(emptyMap())) + (TRASH_SNAPSHOT_KEY to element)))
    }

    /**
     * After a restore: snapshot references that point at the character again (and are live) re-link;
     * the ones the author changed meanwhile stay as changed (`그대로 둔 것`).
     */
    fun restoreReport(doc: ProjectDoc, characterKey: Key, snapshot: ReferenceSnapshot): RestoreReport {
        val now = References.scan(doc, EntityKind.CHARACTER, characterKey).filter { it.live }
        val nowBlocks = now.filter { it.kind == RefKind.SPEAKER && it.field == "speakerKey" }.mapTo(HashSet()) { it.ownerKey }
        val nowOptions = now.filter { it.kind == RefKind.SPEAKER && it.field.startsWith("options:") }
            .mapTo(HashSet()) { "${it.ownerKey}:${it.field.removePrefix("options:")}" }
        val nowScenes = now.filter { it.kind == RefKind.APPEARANCE }.mapTo(HashSet()) { it.ownerKey }
        val nowLinks = now.filter { it.kind == RefKind.LINK }.mapTo(HashSet()) { it.field.removePrefix("link:") }
        val lineHits = snapshot.speakerBlocks.count { it in nowBlocks } + snapshot.speakerOptions.count { it in nowOptions }
        val lineTotal = snapshot.speakerBlocks.size + snapshot.speakerOptions.size
        val linkHits = snapshot.linkKeys.count { it in nowLinks }
        val sceneHits = snapshot.appearanceScenes.count { it in nowScenes }
        return RestoreReport(
            name = doc.index.character(characterKey)?.name.orEmpty(),
            relinkedLines = lineHits,
            relinkedLinks = linkHits,
            relinkedAppearances = sceneHits,
            keptLines = lineTotal - lineHits,
            keptLinks = snapshot.linkKeys.size - linkHits,
            keptAppearances = snapshot.appearanceScenes.size - sceneHits,
        )
    }

    // --- Permanent delete ---

    /** The name tag a purged character leaves: name, kind, voice code, hidden mark (CST-44). */
    fun nameTag(character: Character, ctx: EditContext): NameTag = NameTag(
        key = character.key,
        kind = EntityKind.CHARACTER.nameTagKind.open(),
        name = character.name,
        voiceCode = character.voiceCode,
        hidden = character.hidden,
        state = NameTagState.PURGED.open(),
        purgedAt = ctx.now,
    )

    /**
     * Cast's part of a purge: the character row and everything it owns (relations and their
     * records, records about it, voice lines, archive documents, its images) are removed; a name
     * tag remains; references keep the key and resolve to `영구 삭제 · 노아`.
     */
    fun purgeCharacter(doc: ProjectDoc, characterKey: Key, ctx: EditContext): ProjectDoc {
        val c = doc.index.character(characterKey) ?: return doc
        var next = doc
        for (r in doc.cast.relations.filter { it.endA.key == characterKey || it.endB.key == characterKey }) {
            next = RelationEdits.removeRelation(next, r.key)
        }
        val records = next.changes.records.filterNot { r ->
            when (val t = r.target) {
                is ChangeTarget.Field -> t.entityKind.known == EntityKind.CHARACTER && t.entityKey == characterKey
                is ChangeTarget.DisplayName -> t.characterKey == characterKey
                else -> false
            }
        }
        next = next.copy(
            cast = next.cast.copy(
                characters = next.cast.characters.filterNot { it.key == characterKey },
                voiceLines = next.cast.voiceLines.filterNot { it.characterKey == characterKey },
                archiveDocs = next.cast.archiveDocs.filterNot { it.characterKey == characterKey },
            ),
            changes = next.changes.copy(records = records),
            assets = next.assets.copy(imageUses = next.assets.imageUses.filterNot {
                it.ownerKind.known == ImageOwnerKind.CHARACTER && it.ownerKey == characterKey
            }),
            tombstones = next.tombstones.copy(nameTags = next.tombstones.nameTags.filterNot { it.key == characterKey } + nameTag(c, ctx)),
            trash = next.trash.copy(entries = next.trash.entries.filterNot { it.key == c.trashEntryKey }),
            deletions = next.deletions.copy(entries = next.deletions.entries + DeletionLogEntry(characterKey, "character", ctx.now)),
        )
        return next
    }

    // --- Same name ---

    /** Live items of a kind sharing exactly the same (NFC) name. */
    fun sameNames(doc: ProjectDoc, kind: EntityKind = EntityKind.CHARACTER): List<SameNameNotice> {
        val items: List<Pair<Key, String>> = when (kind) {
            EntityKind.CHARACTER -> doc.cast.characters.filter { it.deletedAt == null }.map { it.key to it.name }
            EntityKind.PLACE -> doc.world.places.filter { it.deletedAt == null }.map { it.key to it.name }
            EntityKind.TERM -> doc.world.terms.filter { it.deletedAt == null }.map { it.key to it.name }
            EntityKind.FACTION -> doc.world.factions.filter { it.deletedAt == null }.map { it.key to it.name }
        }
        return items.groupBy { it.second.nfc() }.filter { it.value.size > 1 }.map { (name, list) -> SameNameNotice(name, list.map { it.first }) }
    }

    /** `지운 인물 · 노아` label words for an item kind (wide places). */
    fun deletedWord(kind: EntityKind): String = "지운 ${kind.label}"
}
