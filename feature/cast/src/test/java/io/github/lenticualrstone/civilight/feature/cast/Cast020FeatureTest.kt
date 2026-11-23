package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.designsystem.component.CreateCardMode
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardState
import io.github.lenticualrstone.civilight.designsystem.component.CreateFieldType
import io.github.lenticualrstone.civilight.designsystem.component.CreateFields
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.domain.actions.TagActions
import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.feature.cast.create.CastCreate
import io.github.lenticualrstone.civilight.feature.cast.create.CreateCardCodec
import io.github.lenticualrstone.civilight.feature.cast.list.CastCardViews
import io.github.lenticualrstone.civilight.feature.cast.list.CastFilterModel
import io.github.lenticualrstone.civilight.feature.cast.list.CastListModel
import io.github.lenticualrstone.civilight.feature.cast.list.CastListUi
import io.github.lenticualrstone.civilight.feature.cast.list.FieldFilterState
import io.github.lenticualrstone.civilight.feature.cast.profile.FixedFieldEdits
import io.github.lenticualrstone.civilight.feature.cast.profile.ProfileTabs
import io.github.lenticualrstone.civilight.feature.cast.profile.SecretGate
import io.github.lenticualrstone.civilight.feature.cast.profile.SecretTabGate
import io.github.lenticualrstone.civilight.feature.cast.profile.SpeechSummaryModel
import io.github.lenticualrstone.civilight.feature.cast.profile.TagPick
import io.github.lenticualrstone.civilight.feature.cast.settings.FieldSettingsModel
import io.github.lenticualrstone.civilight.feature.cast.settings.HeaderFixed
import io.github.lenticualrstone.civilight.feature.cast.settings.TagDeleteModel
import io.github.lenticualrstone.civilight.feature.cast.trash.ItemTrash
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.CardStyle
import io.github.lenticualrstone.civilight.model.app.ViewMode
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.ProfileTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** 0.2.0 cast screens' pure parts: U-15 create, N-43, N-44, N-47 sort, G-2 tabs, U-16 view hook. */
class Cast020FeatureTest {
    private val doc = CastFx.doc()

    private fun gameTools(doc: ProjectDoc, on: Boolean) = doc.copy(project = doc.project.copy(gameTools = on))

    // --- U-15 · CST-66 ---

    @Test
    fun `TC-U15-02 만들기 is off while the name is blank, the name trims, tags and display name are kept`() {
        val blank = CastCreate.openCharacter(CreateCardMode.LIST).with(CreateFields.NAME, "   ")
        assertFalse(CastCreate.canCreate(blank))
        assertNull(CastCreate.createCharacter(doc, blank, CastFx.ctx()))
        val card = CastCreate.toggleTag(
            CastCreate.openCharacter(CreateCardMode.LIST).with(CreateFields.NAME, " 엘라 ").with(CreateFields.displayName.key, "???"),
            K.TAG_LEAD,
        )
        val (next, key) = CastCreate.createCharacter(doc, card, CastFx.ctx())!!
        val c = next.index.character(key)!!
        assertEquals("엘라", c.name)
        assertEquals("???", c.displayNameBase)
        assertEquals(listOf(K.TAG_LEAD), c.tagKeys)
        assertEquals(doc.cast.characters.size + 1, next.cast.characters.size)
    }

    @Test
    fun `TC-U15-03 a picker card opens with an unmatched query as the name and the result is the picked value`() {
        assertNull(CastCreate.pickerQuery("리엔", matched = true))
        assertNull(CastCreate.pickerQuery("  ", matched = false))
        val name = CastCreate.pickerQuery(" 세라2 ", matched = false)
        assertEquals("세라2", name)
        val card = CastCreate.openCharacter(CreateCardMode.PICKER, name)
        assertEquals("세라2", card.name)
        val (next, picked) = CastCreate.createCharacter(doc, card, CastFx.ctx())!!
        assertEquals("세라2", next.index.character(picked)!!.name)
    }

    @Test
    fun `the card draws only fields with a working control (no 이미지, no 색 yet)`() {
        assertEquals(listOf("이름", "표시 이름", "한 줄 소개", "태그"), CastCreate.fields(CreateKind.CHARACTER).map { it.label })
        assertEquals(listOf(CreateFieldType.IMAGE), CastCreate.hiddenFields(CreateKind.CHARACTER).map { it.type })
        assertEquals(listOf("이름"), CastCreate.fields(CreateKind.TAG).map { it.label })
    }

    @Test
    fun `after 만들기 a tag filter that would hide the new row returns to 전체, otherwise it stays`() {
        val ui = CastListUi(selectedTag = K.TAG_UPPER)
        assertNull(CastCreate.afterCreate(ui, listOf(K.TAG_LEAD)).selectedTag)
        assertEquals(K.TAG_UPPER, CastCreate.afterCreate(ui, listOf(K.TAG_UPPER)).selectedTag)
    }

    @Test
    fun `the create row sits under the chips and above the first section, and the new row can be found`() {
        val state = CastListModel.build(doc, CastListUi(), hiddenUnlocked = false, readOnly = false)
        val keys = CastListModel.itemKeys(state, CastListModel.showsCreate(state))
        assertEquals(keys.indexOf(CastListModel.CHIPS_KEY) + 1, keys.indexOf(CastListModel.CREATE_KEY))
        assertTrue(keys.indexOf(CastListModel.CREATE_KEY) < keys.indexOfFirst { it.startsWith("sec-") })
        assertEquals(keys.size, keys.toSet().size)
        assertTrue(CastListModel.firstRowIndex(keys, state, K.RIEN) > keys.indexOf(CastListModel.CREATE_KEY))
        val readOnly = CastListModel.build(doc, CastListUi(), hiddenUnlocked = false, readOnly = true)
        assertFalse("U-15.10", CastListModel.showsCreate(readOnly))
    }

    @Test
    fun `an open card survives with its screen`() {
        val card = CastCreate.toggleTag(CreateCardState.open(CreateKind.CHARACTER, CreateCardMode.PICKER, "세라"), K.TAG_LEAD)
        assertEquals(card, CreateCardCodec.decode(CreateCardCodec.encode(card)))
        assertNull(CreateCardCodec.decode(""))
    }

    // --- CST-75 tags ---

    @Test
    fun `TC-U15-09 a tag card refuses a live name, creates through TagActions and checks it on the character in one write`() {
        val taken = CreateCardState.open(CreateKind.TAG, CreateCardMode.PICKER, doc.index.tag(K.TAG_LEAD)!!.name)
        assertFalse(CastCreate.canCreateTag(doc, taken))
        val card = CreateCardState.open(CreateKind.TAG, CreateCardMode.PICKER, "조연")
        val (next, tagKey) = CastCreate.createTagOn(doc, K.RIEN, card, CastFx.action())!!
        assertTrue(tagKey in next.index.character(K.RIEN)!!.tagKeys)
        assertEquals("조연", CharacterEdits.liveTag(next, tagKey)!!.name)
    }

    @Test
    fun `TC-N44-05 saving the tag sheet keeps the key of a trashed tag, so a restore re-attaches it`() {
        val held = doc.index.character(K.RIEN)!!.tagKeys
        val (trashed, entry) = TagActions.trash(doc, K.TAG_LEAD, CastFx.action())!!
        val picks = FixedFieldEdits.picks(trashed, K.RIEN)
        assertTrue(picks.none { it.key == K.TAG_LEAD })
        val saved = FixedFieldEdits.apply(trashed, K.RIEN, picks + TagPick(null, "새 이름"), CastFx.ctx())
        val keys = saved.index.character(K.RIEN)!!.tagKeys
        if (K.TAG_LEAD in held) assertTrue(K.TAG_LEAD in keys)
        val restored = TagActions.restore(saved, entry.key, CastFx.action())
        assertTrue(restored is io.github.lenticualrstone.civilight.domain.trash.TagRestore.Restored)
    }

    @Test
    fun `the tag delete confirm counts users like the settings row and shows images only when held`() {
        val confirm = TagDeleteModel.confirm(doc, K.TAG_LEAD, hiddenUnlocked = false)!!
        assertEquals("‘${doc.index.tag(K.TAG_LEAD)!!.name}’ 지우기", confirm.title)
        assertTrue(confirm.lines.first().startsWith("쓰는 인물 "))
        assertTrue(confirm.lines.drop(1).all { it.startsWith("이미지 ") })
        val (trashed, _) = TagActions.trash(doc, K.TAG_LEAD, CastFx.action())!!
        assertNull("a trashed tag has no confirm", TagDeleteModel.confirm(trashed, K.TAG_LEAD, false))
    }

    // --- N-47 sort ---

    @Test
    fun `TC-N47-05 오래 안 나옴 keeps the tag sections, is always offered and shows as an active sort`() {
        val state = FieldFilterState().sortAbsent()
        assertTrue(state.absent)
        assertNull(state.sortDefKey)
        assertFalse(CastFilterModel.sheet(doc).isEmpty)
        val plain = CastListModel.build(doc, CastListUi(), false, false)
        val sorted = CastListModel.build(doc, CastListUi(filters = state), false, false)
        assertEquals(plain.view.groups.map { it.tagKey }, sorted.view.groups.map { it.tagKey })
        assertEquals(plain.view.groups.map { g -> g.rows.map { it.key }.toSet() }, sorted.view.groups.map { g -> g.rows.map { it.key }.toSet() })
        assertEquals(CastUiCopy.SORT_ABSENT, CastFilterModel.activeFilters(doc, state).single().label)
        assertFalse(CastFilterModel.activeFilters(doc, state).single().without.active)
    }

    // --- G-2 · CST-74 ---

    @Test
    fun `TC-G-02 four tabs while game tools are off, a remembered 게임 텍스트 opens 개요`() {
        assertEquals(listOf("개요", "말투", "관계", "비밀"), ProfileTabs.labels(false))
        assertEquals(listOf("개요", "말투", "관계", "게임 텍스트", "비밀"), ProfileTabs.labels(true))
        assertEquals(ProfileTab.OVERVIEW, SecretTabGate.open(ProfileTab.GAME_TEXT, gameTools = false, locked = false).tab)
        // Index 3 of the 4-tab row is 비밀, never 게임 텍스트.
        assertEquals(ProfileTab.SECRET, ProfileTabs.shown(false)[3])
        assertEquals(ProfileTab.OVERVIEW, SecretTabGate.gameToolsChanged(SecretTabGate.open(ProfileTab.GAME_TEXT, true, false), false).tab)
    }

    @Test
    fun `the 보이스 header switch and the impact row follow the switch`() {
        assertFalse(HeaderFixed.VOICE in FieldSettingsModel.headerSwitches(gameTools(doc, false)))
        assertTrue(HeaderFixed.VOICE in FieldSettingsModel.headerSwitches(gameTools(doc, true)))
        val withVoices = doc.cast.voiceLines.firstOrNull()?.characterKey ?: return
        val impact = ItemTrash.impact(doc, EntityKind.CHARACTER, withVoices, null) ?: return
        val on = ItemTrash.sheet(EntityKind.CHARACTER, impact, gameTools = true).owned.map { it.label }
        val off = ItemTrash.sheet(EntityKind.CHARACTER, impact, gameTools = false).owned.map { it.label }
        assertTrue(on.any { it.startsWith(ItemTrash.GAME_TEXT_ROW) })
        assertTrue(off.none { it.startsWith(ItemTrash.GAME_TEXT_ROW) })
    }

    // --- N-43 · CST-72, CST-73 ---

    @Test
    fun `TC-N43-02 the summary sheet saves only a changed one-line value`() {
        assertFalse(SpeechSummaryModel.canSave("  반말 ", "반말"))
        assertTrue(SpeechSummaryModel.canSave("반말 · 짧게 끊음", null))
        assertTrue(SpeechSummaryModel.canSave("", "반말"))
        assertFalse(SpeechSummaryModel.canSave(" ", null))
    }

    @Test
    fun `TC-N43-04 every opening of a locked 비밀 tab prompts, cancel keeps the previous tab, leaving locks again`() {
        var s = SecretTabGate.open(ProfileTab.OVERVIEW, gameTools = true, locked = true)
        s = SecretTabGate.select(s, ProfileTab.RELATIONS, locked = true)
        s = SecretTabGate.select(s, ProfileTab.SECRET, locked = true)
        assertTrue(s.prompting(locked = true))
        assertEquals("not composed before confirmation", ProfileTab.RELATIONS, s.shown(locked = true))
        val cancelled = SecretTabGate.cancel(s)
        assertEquals(ProfileTab.RELATIONS, cancelled.shown(true))
        assertFalse(cancelled.prompting(true))
        s = SecretTabGate.confirm(s)
        assertEquals(ProfileTab.SECRET, s.shown(true))
        // Background locks; foreground asks again.
        s = SecretTabGate.background(s)
        assertEquals(ProfileTab.RELATIONS, s.shown(true))
        s = SecretTabGate.foreground(s, locked = true)
        assertTrue(s.prompting(true))
        s = SecretTabGate.confirm(s)
        // Leaving the tab locks it; coming back prompts again.
        s = SecretTabGate.select(s, ProfileTab.SPEECH, true)
        assertEquals(SecretGate.LOCKED, s.gate)
        assertTrue(SecretTabGate.select(s, ProfileTab.SECRET, true).prompting(true))
    }

    @Test
    fun `TC-N43-05 a record opened on a locked 비밀 tab shows 개요 until confirmed, an unlocked tab never prompts`() {
        val opened = SecretTabGate.open(ProfileTab.SECRET, gameTools = false, locked = true)
        assertTrue(opened.prompting(true))
        assertEquals(ProfileTab.OVERVIEW, opened.shown(true))
        assertEquals(ProfileTab.OVERVIEW, SecretTabGate.cancel(opened).shown(true))
        val free = SecretTabGate.select(SecretTabGate.open(ProfileTab.OVERVIEW, true, false), ProfileTab.SECRET, false)
        assertFalse(free.prompting(false))
        assertEquals(ProfileTab.SECRET, free.shown(false))
        // Turning the lock on from inside the open tab asks nothing and keeps it open.
        assertEquals(ProfileTab.SECRET, SecretTabGate.lockedFromInside(free).shown(true))
    }

    // --- U-16 hook ---

    @Test
    fun `TC-U16-01 with no 3D renderer the list is shown and no head toggle is drawn`() {
        val defaults = AppSettings()
        assertEquals(CardStyle.FAN_DECK, defaults.castCardStyle)
        assertEquals(CardStyle.LIST, CastCardViews.shown(defaults))
        assertFalse(CastCardViews.showsToggle(defaults))
        val listChosen = defaults.copy(castCardStyle = CardStyle.LIST)
        assertFalse(CastCardViews.showsToggle(listChosen))
        // With a renderer for the chosen style the toggle shows and the stored toggle decides.
        val fake = mapOf(CardStyle.FAN_DECK to object : io.github.lenticualrstone.civilight.feature.cast.list.CastCardRenderer {
            @androidx.compose.runtime.Composable
            override fun Draw(
                input: io.github.lenticualrstone.civilight.feature.cast.list.CastCardViewInput,
                actions: io.github.lenticualrstone.civilight.feature.cast.list.CastCardViewActions,
            ) = Unit
        })
        assertTrue(CastCardViews.showsToggle(defaults, fake))
        assertEquals(CardStyle.FAN_DECK, CastCardViews.shown(defaults, fake))
        assertEquals(CardStyle.LIST, CastCardViews.shown(defaults.copy(castToggle = ViewMode.LIST), fake))
        assertNotNull(CastUiCopy.CARD_STYLES.getOrNull(CardStyle.ROLODEX.ordinal))
        assertEquals(CardStyle.entries.size, CastUiCopy.CARD_STYLES.size)
    }
}

/** 0.2.0 wiring checks: G-2 surfaces with the switch off, the CST-73 gates, icon names. */
class Cast020SweepTest {
    private val base = CastFx.doc()
    private fun withTools(on: Boolean) = base.copy(project = base.project.copy(gameTools = on))
    private val gameWords = listOf("보이스", "게임 텍스트")

    private fun assertNoGameWords(where: String, text: String) {
        for (w in gameWords) assertFalse("$where shows `$w` with the switch off: $text", text.contains(w))
    }

    @Test
    fun `TC-G-03 with the switch off no cast model shows a voice or game-text surface`() {
        val doc = withTools(false)
        val live = doc.cast.characters.filter { it.deletedAt == null }
        for (c in live) {
            for (tab in ProfileTabs.shown(false)) {
                val ui = io.github.lenticualrstone.civilight.feature.cast.profile.ProfileUi(tab, showEmpty = true, headerExpanded = true)
                for (cover in listOf(true, false)) {
                    val state = io.github.lenticualrstone.civilight.feature.cast.profile.ProfileModel.build(doc, c.key, ui, cover, true, false) ?: continue
                    assertNoGameWords("profile ${c.name} $tab", state.header.toString() + state.fixedRows + state.overview)
                }
            }
            val impact = ItemTrash.impact(doc, EntityKind.CHARACTER, c.key, null) ?: continue
            assertNoGameWords("impact ${c.name}", ItemTrash.sheet(EntityKind.CHARACTER, impact, gameTools = false).owned.joinToString { it.label })
        }
        val settings = FieldSettingsModel.screen(doc, EntityKind.CHARACTER)
        assertNoGameWords("field settings", settings.fixedTitle + settings.fixedFields)
        assertFalse(HeaderFixed.VOICE in FieldSettingsModel.headerSwitches(doc))
        assertEquals(listOf("개요", "말투", "관계", "비밀"), ProfileTabs.labels(false))
    }

    @Test
    fun `TC-G-04 the merge sheet's 보이스 ID row shows only with the switch on`() {
        // Give the merged character one fixed voice ID so the row exists with the switch on.
        val merged = base.cast.characters.first { c -> c.deletedAt == null && c.key != K.RIEN && base.allBlocks.any { it.speakerKey == c.key && it.deletedAt == null } }
        val block = base.allBlocks.first { it.speakerKey == merged.key && it.deletedAt == null }
        val fixed = io.github.lenticualrstone.civilight.model.script.VoiceState(
            mode = io.github.lenticualrstone.civilight.model.OpenEnum.of(io.github.lenticualrstone.civilight.model.script.VoiceMode.FIXED),
            fixedId = "R_001",
        )
        val voiced = base.copy(episodes = base.episodes.mapValues { (_, f) -> f.copy(blocks = f.blocks.map { if (it.key == block.key) it.copy(voice = fixed) else it }) })
        fun rows(on: Boolean) = io.github.lenticualrstone.civilight.feature.cast.merge.MergeModel.view(
            voiced.copy(project = voiced.project.copy(gameTools = on)), EntityKind.CHARACTER, merged.key, K.RIEN, emptySet(),
        )!!.infoRows.joinToString { it.toString() }
        assertTrue(rows(true).contains("보이스 ID"))
        assertFalse(rows(false).contains("보이스 ID"))
    }

    @Test
    fun `TC-N43-06 the merge comparison of a locked 비밀 tab needs the device lock, for either side`() {
        val other = base.cast.characters.first { it.deletedAt == null && it.key != K.RIEN }.key
        val m = io.github.lenticualrstone.civilight.feature.cast.merge.MergeModel
        assertFalse(m.needsSecretAuth(base, EntityKind.CHARACTER, other, K.RIEN))
        val locked = base.copy(cast = base.cast.copy(characters = base.cast.characters.map { if (it.key == K.RIEN) it.copy(secretTabLocked = true) else it }))
        assertTrue(m.needsSecretAuth(locked, EntityKind.CHARACTER, other, K.RIEN))
        assertTrue(m.needsSecretAuth(locked, EntityKind.CHARACTER, K.RIEN, other))
        assertFalse(m.needsSecretAuth(locked, EntityKind.PLACE, K.RIEN, other))
    }

    @Test
    fun `TC-N43-07 a device-lock prompt the cast starts does not relock the open tab or prompt twice`() {
        var s = SecretTabGate.select(SecretTabGate.open(ProfileTab.OVERVIEW, true, true), ProfileTab.SECRET, true)
        s = SecretTabGate.confirm(s)
        val during = SecretTabGate.background(s, ownPrompt = true)
        assertEquals(SecretGate.OPEN, during.gate)
        assertFalse(SecretTabGate.foreground(during, locked = true).prompting(true))
        // A real trip to the background still locks it.
        assertEquals(SecretGate.LOCKED, SecretTabGate.background(s).gate)
    }

    @Test
    fun `TC-N43-09 a secret field sheet of a locked character asks first, unless its tab is open`() {
        val def = base.project.fieldDefs.first { it.entityKind.known == EntityKind.CHARACTER && it.deletedAt == null }
        val secretDoc = base.copy(
            project = base.project.copy(fieldDefs = base.project.fieldDefs.map { if (it.key == def.key) it.copy(isSecret = true) else it }),
            cast = base.cast.copy(characters = base.cast.characters.map { if (it.key == K.RIEN) it.copy(secretTabLocked = true) else it }),
        )
        val auth = io.github.lenticualrstone.civilight.feature.cast.common.CastAuth
        assertTrue(auth.needsFieldAuth(secretDoc, EntityKind.CHARACTER, K.RIEN, def.key, openSecretOf = null))
        assertFalse(auth.needsFieldAuth(secretDoc, EntityKind.CHARACTER, K.RIEN, def.key, openSecretOf = K.RIEN))
        assertFalse("not locked", auth.needsFieldAuth(base.copy(project = secretDoc.project), EntityKind.CHARACTER, K.RIEN, def.key, null))
        assertFalse("not secret", auth.needsFieldAuth(secretDoc.copy(project = base.project), EntityKind.CHARACTER, K.RIEN, def.key, null))
    }

    @Test
    fun `TC-N43-08 list search never returns secret field values`() {
        val doc = base
        val secret = io.github.lenticualrstone.civilight.domain.cast.field.FieldDefinitions.live(doc, EntityKind.CHARACTER).filter { it.isSecret }.map { it.name }.toSet()
        for (q in listOf("ㄹ", "리", "결사", "비밀", "a")) {
            val hits = CastListModel.fieldHits(doc, q, hiddenUnlocked = true)
            assertTrue(hits.none { h -> secret.any { h.sentence.startsWith("$it ") } })
        }
    }

    @Test
    fun `TC-U15-10 the settings kind card takes the section's category, a filled other side makes a pair`() {
        val between = io.github.lenticualrstone.civilight.model.cast.RelationCategory.BETWEEN
        val mind = io.github.lenticualrstone.civilight.model.cast.RelationCategory.MIND
        val card = CreateCardState.open(CreateKind.RELATION_KIND, CreateCardMode.LIST)
        assertNull(CastCreate.createKind(base, between, card.with(CreateFields.NAME, "  "), CastFx.ctx()))
        val (pairDoc, pairKey) = CastCreate.createKind(base, between, card.with(CreateFields.NAME, "사형").with(CreateFields.bothSides.key, "사제"), CastFx.ctx())!!
        val pair = pairDoc.project.relationKinds.single { it.key == pairKey }
        assertEquals("사제", pair.pairName)
        assertEquals(io.github.lenticualrstone.civilight.model.cast.RelationSymmetry.PAIR, pair.symmetry?.known)
        val (mindDoc, mindKey) = CastCreate.createKind(base, mind, card.with(CreateFields.NAME, "흠모").with(CreateFields.bothSides.key, "무시"), CastFx.ctx())!!
        val m = mindDoc.project.relationKinds.single { it.key == mindKey }
        assertEquals(mind, m.category.known)
        assertNull(m.pairName)
        // An existing kind of the section (the example's 마음 `동경`) is refused, not made twice (CST-76).
        assertNull(CastCreate.createKind(base, mind, card.with(CreateFields.NAME, "동경"), CastFx.ctx()))
        assertEquals(listOf(CreateFields.NAME), CastCreate.kindFields(between = false).map { it.key })
    }

    @Test
    fun `ICON every icon-only button in feature cast carries a name, and the list names follow the icon list`() {
        val issues = io.github.lenticualrstone.civilight.designsystem.icon.IconButtonLint.lintDir(java.io.File("src/main"))
        assertEquals(io.github.lenticualrstone.civilight.designsystem.icon.IconButtonLint.report(issues), 0, issues.size)
        assertEquals("거르기", CastUiCopy.FILTER_BUTTON)
        assertEquals("메뉴", CastUiCopy.MENU)
        assertEquals("뒤로", CastUiCopy.BACK)
        assertEquals("항목 넣기", io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec.ADD_FIELD.accessibilityName)
        assertEquals(listOf("목록", "관계도"), listOf(io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec.CAST_LIST.accessibilityName, io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec.CAST_GRAPH.accessibilityName))
        assertEquals("시점 추가", io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec.ADD_POINT.accessibilityName)
    }

    // --- review fixes (CST-73, CST-76, CST-66, merge) ---

    @Test
    fun `CST-73 a secret sheet opened from the open tab is not shown after the background and closes on cancel`() {
        val auth = io.github.lenticualrstone.civilight.feature.cast.common.CastAuth
        val c = K.RIEN
        try {
            var s = SecretTabGate.confirm(SecretTabGate.select(SecretTabGate.open(ProfileTab.OVERVIEW, true, true), ProfileTab.SECRET, true))
            auth.publishTab(c, s)
            assertEquals(io.github.lenticualrstone.civilight.feature.cast.common.SheetGate.SHOW, auth.sheetGate(needsLock = true, tab = auth.secretTabs.value[c], authed = false, viaTab = false))
            // Background: the record relocks synchronously; the sheet that came from the tab draws nothing.
            s = SecretTabGate.background(s)
            auth.publishTab(c, s)
            assertEquals(io.github.lenticualrstone.civilight.feature.cast.common.SheetGate.WAIT, auth.sheetGate(true, auth.secretTabs.value[c], authed = false, viaTab = true))
            s = SecretTabGate.foreground(s, locked = true)
            auth.publishTab(c, s)
            assertEquals(io.github.lenticualrstone.civilight.feature.cast.common.SheetGate.WAIT, auth.sheetGate(true, auth.secretTabs.value[c], authed = false, viaTab = true))
            // Cancelled: the tab is left, the sheet closes (never shown, never its own second prompt).
            s = SecretTabGate.cancel(s)
            auth.publishTab(c, s)
            assertNull(auth.secretTabs.value[c])
            assertEquals(io.github.lenticualrstone.civilight.feature.cast.common.SheetGate.CLOSE, auth.sheetGate(true, auth.secretTabs.value[c], authed = false, viaTab = true))
            // A sheet opened elsewhere never waits on a relocked record in the back stack: it asks on its own.
            assertEquals(io.github.lenticualrstone.civilight.feature.cast.common.SheetGate.PROMPT, auth.sheetGate(true, SecretGate.LOCKED, authed = false, viaTab = false))
            assertEquals(io.github.lenticualrstone.civilight.feature.cast.common.SheetGate.PROMPT, auth.sheetGate(true, SecretGate.PROMPTING, authed = false, viaTab = false))
            // A sheet opened elsewhere asks on its own; an unlocked tab never gates.
            assertEquals(io.github.lenticualrstone.civilight.feature.cast.common.SheetGate.PROMPT, auth.sheetGate(true, null, authed = false, viaTab = false))
            assertEquals(io.github.lenticualrstone.civilight.feature.cast.common.SheetGate.SHOW, auth.sheetGate(false, null, authed = false, viaTab = true))
            // Confirmed again: the waiting sheet comes back.
            assertEquals(io.github.lenticualrstone.civilight.feature.cast.common.SheetGate.SHOW, auth.sheetGate(true, SecretGate.OPEN, authed = false, viaTab = true))
        } finally {
            auth.publishTab(c, null)
        }
    }

    @Test
    fun `CST-73 tapping a relocked selected 비밀 tab asks again`() {
        var s = SecretTabGate.select(SecretTabGate.open(ProfileTab.OVERVIEW, true, false), ProfileTab.SECRET, false)
        s = SecretTabGate.background(s)
        assertEquals(SecretGate.LOCKED, s.gate)
        assertEquals(ProfileTab.OVERVIEW, s.shown(locked = true))
        val tapped = SecretTabGate.select(s, ProfileTab.SECRET, locked = true)
        assertTrue(tapped.prompting(true))
        assertEquals(ProfileTab.SECRET, SecretTabGate.confirm(tapped).shown(true))
        // Not locked, or already asking: unchanged.
        assertEquals(s, SecretTabGate.select(s, ProfileTab.SECRET, locked = false))
        assertEquals(tapped, SecretTabGate.select(tapped, ProfileTab.SECRET, locked = true))
    }

    @Test
    fun `CST-76 the settings kind card refuses a name the section already has`() {
        val between = io.github.lenticualrstone.civilight.model.cast.RelationCategory.BETWEEN
        val mind = io.github.lenticualrstone.civilight.model.cast.RelationCategory.MIND
        val card = CreateCardState.open(CreateKind.RELATION_KIND, CreateCardMode.LIST).with(CreateFields.NAME, "단짝지기")
        val (once, _) = CastCreate.createKind(base, between, card, CastFx.ctx())!!
        assertFalse(CastCreate.canCreateKind(once, between, card))
        assertFalse(CastCreate.canCreateKind(once, between, card.with(CreateFields.NAME, " 단짝지기 ")))
        assertNull(CastCreate.createKind(once, between, card, CastFx.ctx()))
        assertEquals(1, io.github.lenticualrstone.civilight.domain.cast.relation.RelationKinds.of(once, between).count { it.name == "단짝지기" })
        assertTrue("other section", CastCreate.canCreateKind(once, mind, card))
    }

    @Test
    fun `CST-66 after 만들기 a search or field filter that hides the new row is cleared, one that matches is kept`() {
        val card = CastCreate.openCharacter(CreateCardMode.LIST).with(CreateFields.NAME, "새인물")
        val (next, key) = CastCreate.createCharacter(base, card, CastFx.ctx())!!
        val hiding = CastListUi(query = "zzzz-none", searching = true)
        val revealed = CastCreate.revealCreated(next, hiding, key, hiddenUnlocked = false, readOnly = false)
        assertEquals("", revealed.query)
        val s = CastListModel.build(next, revealed, false, false)
        assertTrue(CastListModel.firstRowIndex(CastListModel.itemKeys(s, CastListModel.showsCreate(s)), s, key) >= 0)
        val matching = CastListUi(query = "새인물", searching = true)
        assertEquals(matching, CastCreate.revealCreated(next, matching, key, false, false))
        assertEquals("not in the doc yet", hiding, CastCreate.revealCreated(base, hiding, key, false, false))
    }

    @Test
    fun `merge 태그 row compares live tags only, like the value it prints`() {
        val slot = io.github.lenticualrstone.civilight.domain.cast.merge.MergeSlot.Tags
        val preview = io.github.lenticualrstone.civilight.domain.cast.merge.MergePlanner
        val tagged = base.copy(cast = base.cast.copy(characters = base.cast.characters.map { if (it.key == K.RIEN) it.copy(tagKeys = listOf(K.TAG_LEAD)) else it }))
        val (trashed, _) = TagActions.trash(tagged, K.TAG_LEAD, CastFx.action())!!
        val c = trashed.index.character(K.RIEN)!!
        assertNull(preview.value(c, slot, trashed))
        assertEquals("", preview.text(trashed, c, slot))
        assertNotNull(preview.value(tagged.index.character(K.RIEN)!!, slot, tagged))
    }
}
