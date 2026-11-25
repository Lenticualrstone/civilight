package io.github.lenticualrstone.civilight.model.sample

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.cast.FieldValueState
import io.github.lenticualrstone.civilight.model.cast.RelationCategory
import io.github.lenticualrstone.civilight.model.cast.RelationEndKind
import io.github.lenticualrstone.civilight.model.cast.SlotValue
import io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.AnchorState
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RegionShapeValue
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.PointRef
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.recorder.PlacementKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.CueKind
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.world.ItemRefKind
import kotlinx.serialization.builtins.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

class ExampleProjectInvariantsTest {
    private val doc: ProjectDoc = ExampleProject.build()

    @Test
    fun buildIsDeterministic() {
        assertEquals(doc, ExampleProject.build())
        assertEquals(ExampleRecorder.library(), ExampleRecorder.library())
    }

    @Test
    fun ratingAndProjectSettings() {
        assertEquals(Rating.R15, doc.project.rating.known)
        assertEquals("잿빛 새벽의 등불", doc.project.name)
        assertEquals(K.PROJECT, doc.project.key)
        assertEquals(listOf("장", "에피소드", "씬"), doc.project.structure.levels)
        assertEquals(1200, doc.project.worldTime.storyStartYear)
        val p = doc.project.protections
        assertTrue(!p.hidden && !p.captureBlock && !p.nameMask && !p.recorderLinkMask)
        assertEquals(listOf("도보", "말", "배"), doc.project.travelModes.map { it.name })
        assertEquals(listOf(K.TRAVEL_WALK, K.TRAVEL_HORSE, K.TRAVEL_SHIP), doc.project.travelModes.map { it.key })
        assertEquals(listOf(15.0 to 8.0, 40.0 to 8.0, 30.0 to 24.0), doc.project.travelModes.map { it.kmPerDay to it.movingHoursPerDay })
    }

    @Test
    fun everyKeyIsAValidUlidAndUnique() {
        val keys = ExampleWalk.identityKeys(doc)
        val invalid = keys.filterNot(Keys::isValid)
        assertTrue("invalid keys: $invalid", invalid.isEmpty())
        val duplicates = keys.groupingBy { it }.eachCount().filterValues { it > 1 }
        assertTrue("duplicate keys: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun namedConstantsMatchTheirFamilies() {
        assertEquals(K.scene(1, 7, 3), K.SCENE_1_7_3)
        assertEquals(K.scene(1, 10, 3), K.SCENE_1_10_3)
        assertEquals(K.episode(1, 10), K.EPISODE_1_10)
        assertEquals(K.episode(2, 5), K.EPISODE_2_5)
        assertEquals(K.chapter(2), K.CHAPTER_2)
        assertEquals(K.block(1, 7, 43), K.BLOCK_043)
        assertEquals(K.block(1, 2, 3), K.BLOCK_B12)
        assertEquals(K.block(1, 8, 1), K.BLOCK_1_8_001)
        assertEquals(K.character(7), K.RIEN)
        assertEquals(K.fieldOption(11, 6), K.OPTION_SPECIALTY_CLERIC)
        assertEquals(K.relationKind(22), K.KIND_INDIFFERENCE)
        assertEquals(K.recorderRecord(0), K.RECORDER_R0)
    }

    @Test
    fun orderKeysAreValid() {
        val orders = buildList {
            doc.project.travelModes.forEach { add(it.order) }
            doc.project.tags.forEach { add(it.order) }
            doc.project.relationKinds.forEach { add(it.order) }
            doc.project.fieldDefs.forEach { d -> add(d.order); d.options.orEmpty().forEach { add(it.order) } }
            doc.units.forEach { add(it.order) }
            doc.allBlocks.forEach { b -> add(b.order); b.options.forEach { add(it.order) } }
            doc.cast.characters.forEach { add(it.order) }
            doc.cast.voiceLines.forEach { add(it.order) }
            doc.cast.archiveDocs.forEach { add(it.order) }
            doc.world.terms.forEach { add(it.order) }
            doc.world.places.forEach { add(it.order) }
            doc.world.factions.forEach { add(it.order) }
            doc.world.events.forEach { add(it.order) }
            doc.maps.values.forEach { m -> add(m.order); m.layers.forEach { add(it.order) } }
            doc.canvases.values.forEach { c -> add(c.order); c.nodes.forEach { add(it.z) } }
            doc.pages.values.forEach { add(it.order) }
            ExampleWalk.docs(doc).forEach { (_, rich) -> rich.forEach { add(it.order) } }
        }
        val invalid = orders.filterNot(OrderKeys::isValid)
        assertTrue("invalid order keys: $invalid", invalid.isEmpty())
        // Siblings never share an order key.
        val siblings = doc.units.groupBy { it.parentKey }.values + doc.allBlocks.groupBy { it.sceneKey }.values
        for (group in siblings) {
            val dupes = group.groupingBy { (it as io.github.lenticualrstone.civilight.model.Ordered).order }.eachCount().filterValues { it > 1 }
            assertTrue("shared order keys among siblings: $dupes", dupes.isEmpty())
        }
    }

    @Test
    fun treeLevelsAndNumberingUnits() {
        val structure = doc.project.structure
        for (u in doc.units) {
            val parent = u.parentKey?.let(doc.index::unit)
            if (u.level == 0) assertNull(u.parentKey) else assertEquals("level of ${u.key}", u.level - 1, parent!!.level)
            assertTrue((u.nextBlockNumber != null) == (u.level == structure.episodeLevel))
        }
        for (scene in doc.units.filter { doc.index.isScene(it) }) {
            val unit = doc.index.numberingUnitKeyOfScene(scene.key)
            assertEquals("numbering unit of ${scene.key}", scene.parentKey, unit)
            if (doc.liveness.isLive(scene)) assertNotNull("episode file of $unit", doc.episodes[unit])
        }
        for ((groupKey, file) in doc.episodes) {
            assertEquals(groupKey, file.unitKey)
            for (b in file.blocks) assertEquals("file of ${b.key}", doc.index.numberingUnitKeyOfScene(b.sceneKey), groupKey)
        }
        assertEquals(doc, doc.regroupBlocks())
    }

    @Test
    fun blockNumbersAreUniquePerNumberingUnitAndBelowTheCounter() {
        val purged = doc.deletions.entries.map { it.key }.toSet()
        for ((unitKey, file) in doc.episodes) {
            val counter = doc.index.unit(unitKey)!!.nextBlockNumber!!
            val numbers = file.blocks.map { it.number }
            assertEquals("duplicate numbers in $unitKey", numbers.size, numbers.toSet().size)
            assertTrue("numbers ≥ 1 and < $counter in $unitKey", numbers.all { it in 1 until counter })
            for (b in file.blocks) assertEquals(b.key, K.block(b.key.substring(14, 16).toInt(), b.key.substring(16, 18).toInt(), b.number))
            for (d in file.discardedVoiceIds) assertTrue("discarded block ${d.blockKey} is purged", d.blockKey in purged)
        }
        // Purged numbers are never reused by a live or trashed block.
        assertTrue(doc.allBlocks.none { it.key in purged })
    }

    @Test
    fun exactlyOneLiveTermRevealPerTerm() {
        for (term in doc.world.terms) {
            val reveals = doc.changes.records.filter {
                (it.target as? ChangeTarget.TermReveal)?.termKey == term.key && doc.liveness.isLive(it)
            }
            assertEquals("live reveals of ${term.name}", 1, reveals.size)
            assertNull(reveals.single().value)
        }
    }

    @Test
    fun trashMarksMatchTrashEntries() {
        val marked: Map<Key, Pair<Long, Key?>> = buildMap {
            doc.units.filter { it.deletedAt != null }.forEach { put(it.key, it.deletedAt!! to it.trashEntryKey) }
            doc.allBlocks.filter { it.deletedAt != null }.forEach { put(it.key, it.deletedAt!! to it.trashEntryKey) }
            doc.cast.characters.filter { it.deletedAt != null }.forEach { put(it.key, it.deletedAt!! to it.trashEntryKey) }
            doc.changes.records.filter { it.deletedAt != null }.forEach { put(it.key, it.deletedAt!! to it.trashEntryKey) }
            doc.project.fieldDefs.filter { it.deletedAt != null }.forEach { put(it.key, it.deletedAt!! to it.trashEntryKey) }
            doc.world.terms.filter { it.deletedAt != null }.forEach { put(it.key, it.deletedAt!! to it.trashEntryKey) }
            doc.world.places.filter { it.deletedAt != null }.forEach { put(it.key, it.deletedAt!! to it.trashEntryKey) }
            doc.world.factions.filter { it.deletedAt != null }.forEach { put(it.key, it.deletedAt!! to it.trashEntryKey) }
            doc.world.events.filter { it.deletedAt != null }.forEach { put(it.key, it.deletedAt!! to it.trashEntryKey) }
            doc.maps.values.filter { it.deletedAt != null }.forEach { put(it.key, it.deletedAt!! to it.trashEntryKey) }
            doc.pages.values.filter { it.deletedAt != null }.forEach { put(it.key, it.deletedAt!! to it.trashEntryKey) }
            doc.canvases.values.filter { it.deletedAt != null }.forEach { put(it.key, it.deletedAt!! to it.trashEntryKey) }
        }
        val listed = doc.trash.entries.flatMap { e -> e.targetKeys.map { it to e } }.toMap()
        assertEquals(marked.keys, listed.keys)
        for ((key, mark) in marked) {
            val entry = listed.getValue(key)
            assertEquals(entry.key, mark.second)
            assertEquals(entry.deletedAt, mark.first)
            assertEquals(entry.projectKey, K.PROJECT)
            assertEquals(entry.deletedAt.let(ExampleClock::localDate).plusDays(30), entry.purgeOn)
        }
        for (e in ExampleRecorder.trashEntries()) {
            val record = ExampleRecorder.records().single { it.key in e.targetKeys }
            assertEquals(e.key, record.trashEntryKey)
            assertEquals(e.deletedAt, record.deletedAt)
        }
    }

    @Test
    fun allReferencesResolve() {
        val ix = doc.index
        val problems = mutableListOf<String>()
        fun check(ok: Boolean, what: () -> String) {
            if (!ok) problems += what()
        }
        val unitKeys = doc.units.map { it.key }.toSet()
        val sceneKeys = doc.units.filter { ix.isScene(it) }.map { it.key }.toSet()
        val blockKeys = doc.allBlocks.map { it.key }.toSet()
        val characterKeys = doc.cast.characters.map { it.key }.toSet()
        val assetKeys = doc.assets.scriptAssets.map { it.key }.toSet()

        fun placeRef(ref: PlaceRef?, where: String) {
            if (ref == null) return
            when (ref.kind.known) {
                PlaceRefKind.PLACE -> check(ix.place(ref.key) != null) { "$where: place ${ref.key}" }
                PlaceRefKind.REGION -> check(ix.region(ref.key) != null) { "$where: region ${ref.key}" }
                null -> problems += "$where: unknown place kind"
            }
        }

        fun point(ref: PointRef?, where: String) {
            if (ref == null) return
            check(ref.kind.known != null && ref.unitKey in unitKeys) { "$where: point ${ref.unitKey}" }
        }

        // Story.
        for (u in doc.units) {
            placeRef(u.place, "scene ${u.key}")
            u.silentCast.forEach { check(it in characterKeys) { "silent cast $it in ${u.key}" } }
            placeRef(u.transitionSnapshot?.place, "snapshot ${u.key}")
            when (val t = u.time) {
                is SceneTime.Relative -> {
                    check(t.baseKey in sceneKeys && t.baseKey != u.key) { "base ${t.baseKey} of ${u.key}" }
                    check(t.dayMin <= t.dayMax) { "interval of ${u.key}" }
                }
                is SceneTime.Flashback -> check(t.eventKey?.let(ix::event) != null) { "flashback event of ${u.key}" }
                else -> Unit
            }
            if (u.time !is SceneTime.None) check(ix.isScene(u)) { "time on non-scene ${u.key}" }
        }
        // Script.
        for (b in doc.allBlocks) {
            check(b.sceneKey in sceneKeys) { "scene of block ${b.key}" }
            when (b.kind.known) {
                BlockKind.DIALOGUE -> check(b.speakerKey == null || b.speakerKey in characterKeys) { "speaker of ${b.key}" }
                BlockKind.DIRECTION -> {
                    check(b.cues.isNotEmpty()) { "empty direction ${b.key}" }
                    b.cues.forEach { c ->
                        if (c.kind.known == CueKind.SCREEN_EFFECT) check(c.effect != null) { "effect of ${c.key}" }
                        else check(c.assetKey in assetKeys) { "asset of cue ${c.key}" }
                    }
                }
                BlockKind.CHOICE -> {
                    check(b.options.size >= 2) { "choice ${b.key} has < 2 options" }
                    b.options.forEach { o -> check(o.speakerKey == null || o.speakerKey in characterKeys) { "option speaker ${o.key}" } }
                }
                else -> check(b.speakerKey == null && b.cues.isEmpty() && b.options.isEmpty()) { "kind fields of ${b.key}" }
            }
            if (b.voice != null) check(b.kind.known == BlockKind.DIALOGUE && b.speakerKey != null) { "voice on ${b.key}" }
        }
        // Link runs, per surface.
        for ((surface, link) in ExampleWalk.links(doc)) {
            val kind = link.target.kind.known
            val allowed = when (surface.kind) {
                ExampleWalk.Kind.SCRIPT -> LinkTargetKind.SCRIPT
                ExampleWalk.Kind.DOCUMENT -> LinkTargetKind.DOCUMENT
                ExampleWalk.Kind.RECORDER -> LinkTargetKind.RECORDER
            }
            check(kind in allowed) { "${surface.name}: link kind $kind not allowed" }
            val key = link.target.key
            val resolves = when (kind) {
                LinkTargetKind.CHARACTER -> ix.character(key) != null
                LinkTargetKind.TERM -> ix.term(key) != null
                LinkTargetKind.PLACE -> ix.place(key) != null
                LinkTargetKind.FACTION -> ix.faction(key) != null
                LinkTargetKind.PAGE -> doc.pages[key] != null
                LinkTargetKind.UNIT -> key in unitKeys
                LinkTargetKind.PROJECT -> key == K.PROJECT
                LinkTargetKind.EVENT -> doc.world.events.any { it.key == key }
                null -> false
            }
            check(resolves) { "${surface.name}: link ${link.linkKey} → $key" }
            check((link.target.projectKey != null) == (surface.kind == ExampleWalk.Kind.RECORDER)) { "projectKey of ${link.linkKey}" }
        }
        // Fields.
        fun fields(kind: EntityKind, owner: Key, values: Map<Key, FieldValue>) {
            for ((defKey, value) in values) {
                val def: FieldDef? = ix.fieldDef(defKey)
                check(def != null && def.entityKind.known == kind) { "field $defKey on $owner" }
                if (def == null) continue
                val state = value.state.known
                check((value.payload != null) == (state == FieldValueState.VALUE)) { "payload state of $defKey on $owner" }
                val optionKeys = def.options.orEmpty().map { it.key }.toSet()
                when (val p = value.payload) {
                    is FieldPayload.Choice -> {
                        check(def.type.known == FieldType.CHOICE || def.type.known == FieldType.MULTI_CHOICE) { "choice on $defKey" }
                        check(p.optionKeys.all { it in optionKeys }) { "option of $defKey on $owner" }
                        if (def.type.known == FieldType.CHOICE) check(p.optionKeys.size == 1) { "single choice $defKey" }
                    }
                    is FieldPayload.Link -> {
                        check(def.type.known == FieldType.LINK) { "link on $defKey" }
                        p.targetKeys.forEach { t ->
                            check(def.linkTargetKind?.known == FieldLinkTargetKind.PLACE && ix.place(t) != null) { "link target $t" }
                        }
                    }
                    is FieldPayload.Texts -> check(def.type.known == FieldType.SHORT_TEXT) { "texts on $defKey" }
                    is FieldPayload.Number -> check(def.type.known == FieldType.NUMBER || def.type.known == FieldType.CALC) { "number on $defKey" }
                    is FieldPayload.Date -> check(def.type.known == FieldType.DATE) { "date on $defKey" }
                    is FieldPayload.Anniversary -> check(def.type.known == FieldType.ANNIVERSARY) { "anniversary on $defKey" }
                    is FieldPayload.Rich -> check(def.type.known == FieldType.LONG_TEXT) { "rich on $defKey" }
                    is FieldPayload.Unknown -> problems += "unknown payload on $defKey"
                    null -> Unit
                }
            }
        }
        for (def in doc.project.fieldDefs) {
            def.calc?.let { c -> check(ix.fieldDef(c.sourceFieldDefKey) != null) { "calc source of ${def.key}" } }
            if (def.inFilter) check(def.type.known in setOf(FieldType.CHOICE, FieldType.MULTI_CHOICE, FieldType.LINK)) { "filter on ${def.name}" }
        }
        // Cast.
        for (c in doc.cast.characters) {
            c.tagKeys.forEach { check(ix.tag(it) != null) { "tag $it of ${c.name}" } }
            fields(EntityKind.CHARACTER, c.key, c.fields)
            check(c.voiceCode != null) { "voice code of ${c.name}" }
        }
        for (r in doc.cast.relations) {
            listOf(r.endA, r.endB).forEach { e -> check(e.kind.known == RelationEndKind.CHARACTER && e.key in characterKeys) { "end ${e.key} of ${r.key}" } }
            fun slot(v: SlotValue?, category: RelationCategory) {
                v?.kindKey?.let { k -> check(ix.relationKind(k)?.category?.known == category) { "kind $k in ${r.key}" } }
            }
            slot(r.between, RelationCategory.BETWEEN)
            slot(r.mindA, RelationCategory.MIND)
            slot(r.mindB, RelationCategory.MIND)
        }
        assertEquals("one relation per pair", doc.cast.relations.size, doc.cast.relations.map { it.pairId }.toSet().size)
        doc.cast.voiceLines.forEach { check(it.characterKey in characterKeys) { "voice line ${it.key}" } }
        doc.cast.archiveDocs.forEach { a -> check(a.characterKey in characterKeys) { "archive ${a.key}" }; point(a.unlockPoint, "archive ${a.key}") }
        // World.
        for (t in doc.world.terms) {
            fields(EntityKind.TERM, t.key, t.fields)
            t.related.forEach { r ->
                val ok = when (r.kind.known) {
                    ItemRefKind.TERM -> ix.term(r.key) != null
                    ItemRefKind.PLACE -> ix.place(r.key) != null
                    ItemRefKind.FACTION -> ix.faction(r.key) != null
                    null -> false
                }
                check(ok) { "related ${r.key} of ${t.name}" }
            }
        }
        for (p in doc.world.places) {
            fields(EntityKind.PLACE, p.key, p.fields)
            p.parentPlaceKey?.let { check(ix.place(it) != null && it != p.key) { "parent of ${p.name}" } }
        }
        doc.world.factions.forEach { fields(EntityKind.FACTION, it.key, it.fields) }
        for (e in doc.world.events) {
            e.sceneKey?.let { check(it in sceneKeys) { "scene of ${e.title}" } }
            placeRef(e.place, "event ${e.title}")
            e.relatedTermKeys.forEach { check(ix.term(it) != null) { "term $it of ${e.title}" } }
            point(e.revealPoint, "event ${e.title}")
        }
        // Maps.
        for (m in doc.maps.values) {
            m.parent?.let { p -> check(doc.maps[p.mapKey]?.pins?.any { it.key == p.pinKey } == true) { "parent of ${m.name}" } }
            val layers = m.layers.map { it.key }.toSet()
            val pins = m.pins.map { it.key }.toSet()
            m.pins.forEach { p -> check(ix.place(p.placeKey) != null) { "pin place ${p.key}" }; point(p.revealPoint, "pin ${p.key}") }
            check(m.pins.map { it.placeKey }.toSet().size == m.pins.size) { "one pin per place on ${m.name}" }
            m.regions.forEach { r ->
                r.factionKey?.let { check(ix.faction(it) != null) { "faction of region ${r.key}" } }
                r.placeKey?.let { check(ix.place(it) != null) { "place of region ${r.key}" } }
                r.layerKey?.let { check(it in layers) { "layer of region ${r.key}" } }
                check(r.vertices.size >= 3) { "vertices of ${r.key}" }
                point(r.revealPoint, "region ${r.key}")
            }
            m.routes.forEach { r ->
                r.points.forEach { p -> if (p.pinKey != null) check(p.pinKey in pins) { "route pin ${p.pinKey}" } else check(p.x != null && p.y != null) { "route point" } }
                r.layerKey?.let { check(it in layers) { "layer of route ${r.key}" } }
                r.shownModeKey?.let { k -> check(doc.project.travelModes.any { it.key == k }) { "mode of route ${r.key}" } }
            }
            m.stamps.forEach { s -> s.placeKey?.let { check(ix.place(it) != null) { "stamp place ${s.key}" } } }
            val nodes = m.nodes.map { it.key }.toSet()
            m.nodes.forEach { n -> n.placeKey?.let { check(ix.place(it) != null) { "node place ${n.key}" } } }
            m.nodeLinks.forEach { l -> check(l.aNodeKey in nodes && l.bNodeKey in nodes) { "node link ${l.key}" } }
            m.scale?.travelModeKey?.let { k -> check(doc.project.travelModes.any { it.key == k }) { "scale mode of ${m.name}" } }
        }
        // Change records.
        for (r in doc.changes.records) {
            when (val t = r.target) {
                is ChangeTarget.Field -> {
                    val kind = t.entityKind.known!!
                    check(ix.nameOf(kind, t.entityKey) != null) { "target entity of ${r.key}" }
                    check(ix.fieldDef(t.fieldDefKey)?.entityKind?.known == kind) { "target field of ${r.key}" }
                    check(r.valueAs(FieldValue.serializer())?.state?.known == FieldValueState.VALUE) { "value of ${r.key}" }
                }
                is ChangeTarget.DisplayName -> check(ix.character(t.characterKey) != null) { "display name target of ${r.key}" }
                is ChangeTarget.RelationSlot -> {
                    val relation = ix.relation(t.relationKey)
                    check(relation != null) { "relation of ${r.key}" }
                    when (t.slot.known) {
                        RelationSlotName.BETWEEN -> check(r.valueAs(SlotValue.serializer())?.kindKey?.let(ix::relationKind)?.category?.known == RelationCategory.BETWEEN) { "between value of ${r.key}" }
                        RelationSlotName.MIND_A, RelationSlotName.MIND_B ->
                            check(r.valueAs(SlotValue.serializer())?.kindKey?.let(ix::relationKind)?.category?.known == RelationCategory.MIND) { "mind value of ${r.key}" }
                        RelationSlotName.ADDRESS_A, RelationSlotName.ADDRESS_B -> check(r.valueAs(String.serializer()) != null) { "address value of ${r.key}" }
                        null -> problems += "slot of ${r.key}"
                    }
                }
                is ChangeTarget.RegionShape -> {
                    check(doc.maps[t.mapKey]?.regions?.any { it.key == t.regionKey } == true) { "region of ${r.key}" }
                    check((r.valueAs(RegionShapeValue.serializer())?.vertices?.size ?: 0) >= 3) { "shape of ${r.key}" }
                }
                is ChangeTarget.TermReveal -> check(ix.term(t.termKey) != null) { "term of ${r.key}" }
                is ChangeTarget.Unknown -> problems += "unknown target ${r.key}"
            }
            when (val a = r.anchor) {
                is Anchor.AtBlock -> check(a.blockKey in blockKeys) { "anchor block of ${r.key}" }
                is Anchor.UnitStart -> check(a.unitKey in unitKeys) { "anchor unit of ${r.key}" }
                is Anchor.UnitEnd -> check(a.unitKey in unitKeys) { "anchor unit of ${r.key}" }
                is Anchor.WorldEvent -> check(ix.event(a.eventKey) != null) { "anchor event of ${r.key}" }
                else -> problems += "anchor of ${r.key}: $a"
            }
            check(r.anchorState.known == AnchorState.NORMAL && r.originalAnchor == null) { "anchor state of ${r.key}" }
        }
        // Canvas, pages, assets, checks.
        for (c in doc.canvases.values) {
            val nodes = c.nodes.associateBy { it.key }
            for (n in c.nodes) {
                val ok = when (n.kind.known) {
                    CanvasNodeKind.CHARACTER -> n.refKey in characterKeys
                    CanvasNodeKind.FACTION -> n.refKey?.let(ix::faction) != null
                    CanvasNodeKind.EVENT -> n.refKey?.let(ix::event) != null
                    CanvasNodeKind.TERM -> n.refKey?.let(ix::term) != null
                    CanvasNodeKind.MEMO, CanvasNodeKind.GROUP -> n.refKey == null && n.text != null
                    null -> false
                }
                check(ok) { "canvas node ${n.key}" }
            }
            for (e in c.edges) {
                val from = nodes[e.fromNodeKey]
                val to = nodes[e.toNodeKey]
                check(from != null && to != null) { "edge ${e.key}" }
                e.relationKey?.let { rk ->
                    val rel = ix.relation(rk)
                    check(rel != null && from?.kind?.known == CanvasNodeKind.CHARACTER && to?.kind?.known == CanvasNodeKind.CHARACTER) { "relation edge ${e.key}" }
                    check(rel != null && setOf(rel.endA.key, rel.endB.key) == setOf(from?.refKey, to?.refKey)) { "relation ends of ${e.key}" }
                }
            }
        }
        val recordKeys = ExampleRecorder.records().map { it.key }.toSet()
        for (p in doc.pages.values) {
            p.sourceRecord?.let { check(it.recordKey in recordKeys) { "source of ${p.name}" } }
            p.blocks.forEach { b -> b.recorderSource?.let { check(it.recordKey in recordKeys) { "paragraph source in ${p.name}" } } }
        }
        doc.assets.imageUses.forEach { check(it.ownerKey == K.PROJECT && it.isPreset) { "image use ${it.key}" } }
        doc.assets.scriptAssets.forEach { a -> a.placeKey?.let { check(ix.place(it) != null) { "asset place ${a.key}" } } }
        for (a in doc.checks.acknowledgements) {
            a.subjectKeys.forEach { k -> check(k in characterKeys || k in unitKeys || k in blockKeys) { "ack subject $k" } }
        }
        // Recorder slice.
        for (l in ExampleRecorder.links()) {
            check(l.recordKey in recordKeys && l.projectKey == K.PROJECT) { "recorder link ${l.key}" }
            val ok = when (l.targetKind.known) {
                RecorderTargetKind.PROJECT -> l.targetKey == K.PROJECT
                RecorderTargetKind.UNIT -> l.targetKey in unitKeys
                RecorderTargetKind.CHARACTER -> l.targetKey in characterKeys
                RecorderTargetKind.PLACE -> ix.place(l.targetKey) != null
                RecorderTargetKind.TERM -> ix.term(l.targetKey) != null
                RecorderTargetKind.FACTION -> ix.faction(l.targetKey) != null
                RecorderTargetKind.PAGE -> doc.pages[l.targetKey] != null
                null -> false
            }
            check(ok) { "recorder link target ${l.targetKey}" }
        }
        for (p in ExampleRecorder.placements()) {
            check(p.recordKey in recordKeys && p.kind.known == PlacementKind.PAGE && doc.pages[p.targetKey]?.sourceRecord?.recordKey == p.recordKey) { "placement ${p.key}" }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}
