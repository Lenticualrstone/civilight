package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.domain.cast.field.Profile
import io.github.lenticualrstone.civilight.domain.cast.list.CastList
import io.github.lenticualrstone.civilight.domain.cast.list.Hangul
import io.github.lenticualrstone.civilight.domain.cast.voice.ArchiveDocs
import io.github.lenticualrstone.civilight.domain.cast.voice.VoiceLineState
import io.github.lenticualrstone.civilight.domain.cast.voice.VoiceLines
import io.github.lenticualrstone.civilight.model.cast.EntityRef
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.RelationEndKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.text.DocBlock
import io.github.lenticualrstone.civilight.model.text.DocBlockKind
import io.github.lenticualrstone.civilight.model.text.RecorderSource
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.docText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Character list, card view, game text and character edits (CST-01 … CST-05, CST-14, CST-16, CST-17, CST-49). */
class CastViewsTest {
    private val doc = Fx.doc()
    private val ctx = Fx.ctx()

    @Test
    fun `list groups by first tag with chips in tag order`() {
        val view = CastList.build(doc, Fx.LATEST)
        assertEquals("잿빛 새벽의 등불", view.projectName)
        assertEquals("인물", view.title)
        assertEquals("최신 · 1-7", view.lensChip)
        assertEquals(listOf("목록", "관계도"), view.modes)
        assertEquals(listOf("전체 11", "주연", "등불 결사", "하층 구역", "상층"), view.chips.map { it.label })
        assertTrue(view.chips.first().selected)
        assertEquals(listOf("주연", "등불 결사", "하층 구역", "상층"), view.groups.map { it.title })
        assertEquals(listOf("리엔", "하르트", "세라"), view.groups.first().rows.map { it.name })
        assertEquals(listOf("마르타", "테오"), view.groups[1].rows.map { it.name })
        assertEquals("이름 · 초성 · 태그로 찾기", view.searchHint)
        assertNull(view.lockRow)
        assertNull(view.empty)
    }

    @Test
    fun `tag chip filters to that tag`() {
        val view = CastList.build(doc, Fx.LATEST, selectedTag = K.TAG_LANTERN_SOCIETY)
        assertEquals(listOf("리엔", "세라", "마르타", "테오"), view.rows.map { it.name })
        assertTrue(view.chips.single { it.tagKey == K.TAG_LANTERN_SOCIETY }.selected)
    }

    @Test
    fun `chips skip tags no shown character holds`() {
        // A tag only gallery images use (the tag list is shared, 55 §3.4) has no chip.
        val (galleryOnly, harbor) = CharacterEdits.addTag(doc, "harbor", ctx)
        assertEquals(listOf("전체 11", "주연", "등불 결사", "하층 구역", "상층"), CastList.chips(galleryOnly, null).map { it.label })
        // Held only by a locked hidden character: no chip while locked, a chip once unlocked.
        val hidden = CharacterEdits.setHidden(CharacterEdits.setTags(galleryOnly, K.OWEN, listOf(harbor.key), ctx), K.OWEN, true, ctx)
        assertFalse(CastList.chips(hidden, null, hiddenUnlocked = false).any { it.tagKey == harbor.key })
        assertTrue(CastList.chips(hidden, null, hiddenUnlocked = true).any { it.tagKey == harbor.key })
        assertEquals(listOf(harbor.key), CastList.chipTags(hidden, hiddenUnlocked = true).filter { it.name == "harbor" }.map { it.key })
    }

    @Test
    fun `search by name, 초성 and tag`() {
        // 초성 match anywhere in the name: 리엔, and 엘리오 (`리오`).
        assertEquals(listOf("리엔", "엘리오"), CastList.build(doc, Fx.LATEST, query = "ㄹㅇ").rows.map { it.name })
        assertEquals(listOf("리엔"), CastList.build(doc, Fx.LATEST, query = "ㄹㅇ", selectedTag = K.TAG_LEAD).rows.map { it.name })
        assertEquals(listOf("오웬", "도르", "고든"), CastList.build(doc, Fx.LATEST, query = "하층").rows.map { it.name })
        val none = CastList.build(doc, Fx.LATEST, query = "없는 이름")
        assertTrue(none.noResults)
        assertTrue(Hangul.matches("리엔", "ㄹ엔"))
        assertEquals("ㄹㅇ", Hangul.initials("리엔"))
        assertFalse(Hangul.matches("리엔", "ㅎ"))
    }

    @Test
    fun `empty project shows EmptyCast`() {
        val empty = ProjectDoc(project = Project(key = Fx.key("PRJT", 3), name = "빈 프로젝트"))
        val view = CastList.build(empty, Fx.LATEST)
        val e = requireNotNull(view.empty)
        assertEquals("전체 0", e.chip)
        assertEquals("인물 없음", e.title)
        assertEquals("이름만 적어도 시작 · 나머지는 나중에", e.sub)
        assertEquals("인물 추가" to "라이프 레코더에서", e.primary to e.secondary)
        assertEquals("최신 · 전체", view.lensChip)
    }

    @Test
    fun `아직 등장 전 is appended before the first appearance`() {
        assertTrue(Profile.listSubtitle(doc, K.RIEN, Fx.lens(K.EPISODE_1_1)).endsWith("아직 등장 전"))
        assertFalse(Profile.listSubtitle(doc, K.RIEN, Fx.LATEST).contains("아직 등장 전"))
        assertEquals("1-2", FirstAppearance.label(doc, K.RIEN))
        assertEquals("1-1", FirstAppearance.label(doc, K.MARTHA))
        assertNull(FirstAppearance.label(Fx.addCharacter(doc, Fx.key("CHAR", 30), "무명", 13), Fx.key("CHAR", 30)))
    }

    @Test
    fun `card view rows and display name cards`() {
        val groups = CastList.cards(doc, Fx.LATEST)
        assertEquals(listOf("주연 · 3", "등불 결사 · 2", "하층 구역 · 3", "상층 · 3"), groups.map { it.header })
        val hart = groups.first().cards.single { it.key == K.HART }
        assertEquals("???", hart.title)
        assertEquals("하르트 · 이름 공개 1-9", hart.subtitle)
        val rien = groups.first().cards.single { it.key == K.RIEN }
        assertEquals("리엔", rien.title)
        assertEquals("17세 · 등불 결사 · 막내 등불지기", rien.subtitle)
        assertFalse(rien.holo)
    }

    @Test
    fun `voice lines and progress`() {
        val p = VoiceLines.progress(doc, K.RIEN)
        assertEquals("3 / 5", p.text)
        assertEquals("보이스 · 3 / 5", p.sectionTitle)
        val rows = VoiceLines.rows(doc, K.RIEN)
        assertEquals(listOf("01", "02", "03", "04", "05"), rows.map { it.number })
        assertEquals(
            listOf(VoiceLineState.DONE, VoiceLineState.DONE, VoiceLineState.WRITING, VoiceLineState.EMPTY, VoiceLineState.EMPTY),
            rows.map { it.state },
        )
        assertEquals(listOf("녹음 대본 완료", "녹음 대본 완료", "쓰는 중", "비어 있음", "비어 있음"), rows.map { it.state.label })
        assertEquals("—", rows[3].text)
        val check = requireNotNull(VoiceLines.emptyCheck(doc, K.RIEN))
        assertEquals("▲ 보이스 비어 있음", check.label)
        assertEquals("인물 · 리엔", check.location)
        assertEquals("리엔 · 신뢰 상승 후", check.body)
        assertEquals("보이스 2/5 비어 있음", check.detail)
        assertEquals("보이스 쓰기", check.action)
        assertEquals(K.VOICE_RIEN_AFTER_TRUST, check.firstEmptyLineKey)
        assertNull(VoiceLines.emptyCheck(doc, K.HART))
        // Header row `보이스 3 / 5` after `첫 등장`.
        val header = Profile.header(doc, K.RIEN, Fx.LATEST, cover = false)
        assertEquals(listOf("나이", "소속", "신장", "종족", "첫 등장", "보이스"), header.rows.map { it.label })
        assertEquals("3 / 5", header.rows.last().value)
    }

    @Test
    fun `voice line edits`() {
        val (added, line) = VoiceLines.add(doc, K.RIEN, "작별", ctx)
        assertEquals("06", VoiceLines.rows(added, K.RIEN).last().number)
        assertEquals("3 / 6", VoiceLines.progress(added, K.RIEN).text)
        // An empty line stays `비어 있음` when its state is tapped.
        assertEquals(VoiceLineState.EMPTY, VoiceLines.state(VoiceLines.toggleStatus(added, line.key, ctx).index.voiceLines(K.RIEN).last()))
        val written = VoiceLines.setText(added, line.key, listOf(DocBlock(Fx.key("DOCB", 9), "V", runs = listOf(TextRun.Plain("또 봐요.")))), ctx)
        assertEquals(VoiceLineState.WRITING, VoiceLines.state(written.index.voiceLines(K.RIEN).last()))
        val done = VoiceLines.toggleStatus(written, line.key, ctx)
        assertEquals(VoiceLineState.DONE, VoiceLines.state(done.index.voiceLines(K.RIEN).last()))
        assertEquals("4 / 6", VoiceLines.progress(done, K.RIEN).text)
    }

    @Test
    fun `archive documents`() {
        assertEquals("아카이브 문서 · 2", ArchiveDocs.sectionTitle(doc, K.RIEN))
        val rows = ArchiveDocs.rows(doc, K.RIEN)
        assertEquals("기록 1 · 등불지기의 일지", rows[0].title)
        assertEquals("67자 · 1-7 이후 열림", rows[0].sub)
        assertEquals("기록 2 · 대정전 그날", rows[1].title)
        assertEquals("비어 있음 · 2장 이후 열림", rows[1].sub)
        assertEquals("1,120", ArchiveDocs.formatCount(1120))
        val journal = doc.index.archiveDocs(K.RIEN).first()
        assertTrue(ArchiveDocs.isOpenAt(doc, journal, Fx.L1_7))
        assertFalse(ArchiveDocs.isOpenAt(doc, journal, Fx.L1_6))
    }

    @Test
    fun `create a character`() {
        val (created, c) = CharacterEdits.create(doc, "  카렌 ", ctx)
        assertEquals("카렌", c.name)
        assertEquals(13, c.recordNo)
        assertEquals(14, created.project.counters.nextCharacterRecordNo)
        assertTrue(c.fields.isEmpty())
        assertTrue(created.cast.relations.none { it.involves(EntityRef(RelationEndKind.CHARACTER, c.key)) })
        assertEquals("카렌", CharacterEdits.live(created).last().name)
        // Numbers are never reused.
        assertEquals(14, CharacterEdits.create(created, "도윤", Fx.ctx(seed = 9)).second.recordNo)
        assertEquals("인물 추가", CharacterEdits.CREATE_TITLE)
    }

    @Test
    fun `recorder memo into a character and 인물로 만들기`() {
        val source = RecorderSource(K.RECORDER_R1, ctx.now)
        val withMemo = CharacterEdits.appendMemo(doc, K.RIEN, CharacterTab.Speech, "말끝을 흐린다", source, ctx)
        val memo = withMemo.index.character(K.RIEN)!!.speechBody.last()
        assertEquals(DocBlockKind.MEMO, memo.kind.known)
        assertEquals(source, memo.recorderSource)
        assertEquals(doc.index.character(K.RIEN)!!.speechBody, withMemo.index.character(K.RIEN)!!.speechBody.dropLast(1))

        assertEquals("시장에서 본 아이.", CharacterEdits.firstSentence("시장에서 본 아이. 이름은 모른다."))
        val (made, c) = CharacterEdits.createFromRecord(doc, "꼬마", "시장에서 본 아이. 이름은 모른다.", source, ctx)
        assertEquals("시장에서 본 아이.", c.oneLiner)
        val personality = made.index.character(c.key)!!.fields[K.FIELD_PERSONALITY]?.payload as FieldPayload.Rich
        assertEquals("시장에서 본 아이. 이름은 모른다.", personality.doc.docText)
        assertNotNull(personality.doc.single().recorderSource)
    }

    @Test
    fun `tags are unique per project`() {
        val (same, tag) = CharacterEdits.addTag(doc, "주연", ctx)
        assertEquals(K.TAG_LEAD, tag.key)
        assertEquals(doc.project.tags, same.project.tags)
        val (more, added) = CharacterEdits.addTag(doc, "외부인", ctx)
        assertEquals(listOf("주연", "등불 결사", "하층 구역", "상층", "외부인"), CharacterEdits.tags(more).map { it.name })
        assertEquals(listOf(K.TAG_LEAD, added.key), CharacterEdits.setTags(more, K.RIEN, listOf(K.TAG_LEAD, added.key, K.TAG_LEAD), ctx).index.character(K.RIEN)!!.tagKeys)
    }
}
