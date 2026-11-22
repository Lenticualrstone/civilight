package io.github.lenticualrstone.civilight.domain.library

import io.github.lenticualrstone.civilight.domain.library.crypto.EncryptedFileSheet
import io.github.lenticualrstone.civilight.domain.library.crypto.EncryptedFiles
import io.github.lenticualrstone.civilight.domain.library.crypto.EncryptedOpen
import io.github.lenticualrstone.civilight.domain.library.crypto.PackageCrypto
import io.github.lenticualrstone.civilight.domain.library.crypto.RecoveryKeys
import io.github.lenticualrstone.civilight.domain.library.format.FormatVersion
import io.github.lenticualrstone.civilight.domain.library.format.PackageOpen
import io.github.lenticualrstone.civilight.domain.library.format.PackageWriter
import io.github.lenticualrstone.civilight.domain.library.format.ProjectFileOptions
import io.github.lenticualrstone.civilight.domain.library.format.ProjectPackageReader
import io.github.lenticualrstone.civilight.domain.library.format.ProjectPackageWriter
import io.github.lenticualrstone.civilight.domain.library.format.Sha256
import io.github.lenticualrstone.civilight.domain.library.format.UpgradeChain
import io.github.lenticualrstone.civilight.domain.library.format.UpgradeStep
import io.github.lenticualrstone.civilight.domain.library.format.ZipCodec
import io.github.lenticualrstone.civilight.domain.library.importing.OpenDecision
import io.github.lenticualrstone.civilight.domain.library.importing.OpenDecisions
import io.github.lenticualrstone.civilight.domain.library.importing.OpenFileSheets
import io.github.lenticualrstone.civilight.domain.library.importing.ProjectImports
import io.github.lenticualrstone.civilight.domain.library.importing.SameProjectChoice
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.FileManifest
import io.github.lenticualrstone.civilight.model.file.ManifestFeatures
import io.github.lenticualrstone.civilight.model.file.ManifestProject
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import io.github.lenticualrstone.civilight.model.trash.TrashReason
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom
import java.text.Normalizer
import java.time.LocalDate

/** 60-library §4.6 `.civilight` format cases F-01 … F-19. */
class FormatTest {
    private val doc = Fixtures.example()
    private val k = ExampleKeys.PROJECT
    private val fastIterations = 1_000

    private fun manifestOf(bytes: ByteArray): FileManifest =
        CivilightJson.decode(FileManifest.serializer(), ZipCodec.read(bytes)[PackagePaths.MANIFEST]!!.toString(Charsets.UTF_8))

    /** A package whose manifest says [formatVersion] / [minReader] / [features] around the example's files. */
    private fun packageWith(
        formatVersion: String,
        minReader: String,
        features: ManifestFeatures,
        source: ProjectDoc = doc,
    ): ByteArray {
        val content = ProjectPackageWriter.content(source, writtenBy = "Civilight test", writtenAt = Fixtures.NOW)
        val manifest = content.manifest.copy(formatVersion = formatVersion, minReaderVersion = minReader, features = features)
        return PackageWriter.zip(PackagePaths.MIME_PROJECT, manifest, content.files)
    }

    private fun library(vararg docs: ProjectDoc): List<LibraryEntry> = docs.map { Fixtures.entry(it) }

    @Test
    fun `F-01 mimetype first and stored, every other file hashed`() {
        val bytes = Fixtures.export(doc, Fixtures.recorder())
        val zip = ZipCodec.read(bytes)
        val first = zip.items.first()
        assertEquals(PackagePaths.MIMETYPE, first.path)
        assertTrue(first.stored)
        assertEquals(PackagePaths.MIME_PROJECT, first.bytes.toString(Charsets.US_ASCII))
        assertTrue(zip.items.drop(1).none { it.stored })

        val manifest = manifestOf(bytes)
        val others = zip.paths.filter { it != PackagePaths.MIMETYPE && it != PackagePaths.MANIFEST }
        assertEquals(others.toSet(), manifest.files.keys)
        for (path in others) {
            val hash = manifest.files.getValue(path)
            assertTrue(hash.startsWith("sha256-"))
            assertEquals(Sha256.label(zip[path]!!), hash)
        }
        assertEquals(FileManifest.FORMAT_PROJECT, manifest.format)
        assertEquals("1.0", manifest.formatVersion)
        assertEquals("1.0", manifest.minReaderVersion)
        assertEquals(listOf("story.blocks", "changes", "time.base-interval"), manifest.features.required)
        assertEquals(ManifestProject(k, "잿빛 새벽의 등불", Rating.R15.open()), manifest.project)
        assertFalse(manifest.encrypted)
        // Every file unit of §7.2 that the example fills is there.
        for (path in listOf(PackagePaths.PROJECT, PackagePaths.STORY_INDEX, PackagePaths.CAST, PackagePaths.WORLD, PackagePaths.CHANGES, PackagePaths.ASSETS_INDEX, PackagePaths.RECORDER)) {
            assertTrue(path, path in zip)
        }
        assertTrue(PackagePaths.episode(ExampleKeys.EPISODE_1_7) in zip)
    }

    @Test
    fun `F-02 a decomposed name is read as NFC`() {
        val nfd = Normalizer.normalize("잿빛 새벽의 등불", Normalizer.Form.NFD)
        assertNotEquals("잿빛 새벽의 등불", nfd)
        // Hand-built package: project.json carries NFD bytes (a writer that did not normalise).
        val content = ProjectPackageWriter.content(doc, writtenBy = "Civilight test", writtenAt = Fixtures.NOW)
        val projectJson = content.files.getValue(PackagePaths.PROJECT).toString(Charsets.UTF_8)
            .replace("잿빛 새벽의 등불", nfd)
        assertTrue(projectJson.contains(nfd))
        val files = LinkedHashMap(content.files)
        files[PackagePaths.PROJECT] = projectJson.toByteArray(Charsets.UTF_8)
        val pkg = Fixtures.open(PackageWriter.zip(PackagePaths.MIME_PROJECT, content.manifest, files))
        assertEquals("잿빛 새벽의 등불", pkg.doc.project.name)
        assertTrue(pkg.doc.project.name.contains("잿빛"))

        // Write side: an NFD name typed into the project is stored NFC.
        val typed = doc.copy(project = doc.project.copy(name = nfd))
        val written = ZipCodec.read(Fixtures.export(typed))[PackagePaths.PROJECT]!!.toString(Charsets.UTF_8)
        assertTrue(written.contains("잿빛 새벽의 등불"))
        assertFalse(written.contains(nfd))
        assertEquals("잿빛 새벽의 등불", manifestOf(Fixtures.export(typed)).project!!.title)
    }

    @Test
    fun `F-03 a 1_0 file opens in a 1_2 app through every upgrade step in order`() {
        val bytes = Fixtures.export(doc)
        val original = bytes.copyOf()
        val seen = ArrayList<String>()
        fun step(from: String, to: String, mark: String) = UpgradeStep(FormatVersion.parse(from), FormatVersion.parse(to)) { files ->
            seen += "$from→$to"
            val project = files.getValue(PackagePaths.PROJECT).jsonObject
            val name = project.getValue("name").jsonPrimitive.content
            files + (PackagePaths.PROJECT to JsonObject(project + ("name" to JsonPrimitive(name + mark))))
        }
        // Steps listed out of order: the chain still runs them by version.
        val chain = UpgradeChain(FormatVersion(1, 2), listOf(step("1.1", "1.2", "B"), step("1.0", "1.1", "A")))
        val result = ProjectPackageReader.open(bytes, chain)
        assertTrue(result is PackageOpen.Ready)
        val pkg = (result as PackageOpen.Ready).pkg
        assertEquals(listOf("1.0→1.1", "1.1→1.2"), seen)
        assertEquals(listOf("1.0→1.1", "1.1→1.2"), pkg.appliedUpgrades)
        assertEquals("잿빛 새벽의 등불AB", pkg.doc.project.name)
        assertArrayEquals(original, bytes)
        // The current app has no steps yet: a 1.0 file opens unchanged.
        assertEquals(emptyList<String>(), Fixtures.open(bytes).appliedUpgrades)
    }

    @Test
    fun `F-04 a file needing a newer reader is refused with the newer-file sheet`() {
        val features = ManifestFeatures(required = listOf("story.blocks", "changes", "time.base-interval", "maps.gen.v2"))
        val bytes = packageWith("1.4", "1.4", features)
        val appMax = FormatVersion(1, 2)
        val decision = OpenDecisions.decide(manifestOf(bytes), library(), appMax)
        assertTrue(decision is OpenDecision.RefuseNewer)
        val sheet = (decision as OpenDecision.RefuseNewer).sheet
        assertEquals(listOf("이 파일" to "양식 1.4", "지금 앱" to "1.2까지", "필요한 기능" to "지도 생성 2판"), sheet.rows)
        assertEquals("새 버전 앱에서 만든 파일", sheet.heading)
        assertEquals("가져오지 않음 · 파일은 그대로 · 업데이트 뒤 다시 열기", sheet.note)
        assertEquals(listOf("닫기", "Civilight 업데이트"), sheet.buttons)
        assertEquals("잿빛 새벽의 등불", sheet.title)
        assertEquals("R-15", sheet.ratingBadge)
        assertEquals("11월 22일", sheet.summary(1_258_291, Fixtures.ZONE).substringAfter(" · "))
        // The reader refuses too: nothing is decoded or imported.
        val read = ProjectPackageReader.open(bytes, UpgradeChain(appMax, emptyList()))
        assertTrue(read is PackageOpen.Newer)
        assertTrue((read as PackageOpen.Newer).check.versionTooNew)
    }

    @Test
    fun `F-05 an unknown optional feature never blocks`() {
        val features = ManifestFeatures(optional = listOf("maps.gen.v2"))
        val bytes = packageWith("1.3", "1.0", features)
        val appMax = FormatVersion(1, 2)
        assertTrue(OpenDecisions.decide(manifestOf(bytes), library(), appMax) is OpenDecision.Import)
        val read = ProjectPackageReader.open(bytes, UpgradeChain(appMax, emptyList()))
        assertTrue(read is PackageOpen.Ready)
        assertEquals(listOf("maps.gen.v2"), (read as PackageOpen.Ready).pkg.skippedOptionalFeatures)
        // The known content reads exactly as from a current file.
        assertEquals(Fixtures.open(Fixtures.export(doc)).doc, read.pkg.doc)
        assertEquals(emptyList<String>(), read.pkg.appliedUpgrades)
    }

    @Test
    fun `F-06 an unknown required feature refuses even with an old minReaderVersion`() {
        val features = ManifestFeatures(required = listOf("story.blocks.v3", "changes", "time.base-interval"))
        val bytes = packageWith("1.0", "1.0", features)
        val decision = OpenDecisions.decide(manifestOf(bytes), library())
        assertTrue(decision is OpenDecision.RefuseNewer)
        assertEquals("필요한 기능" to "story.blocks.v3", (decision as OpenDecision.RefuseNewer).sheet.rows.last())
        val read = ProjectPackageReader.open(bytes)
        assertTrue(read is PackageOpen.Newer)
        assertFalse((read as PackageOpen.Newer).check.versionTooNew)
        assertEquals(listOf("story.blocks.v3"), read.check.unknownRequired)
    }

    @Test
    fun `F-07 a damaged episode file is left out and everything else imported`() {
        val content = ProjectPackageWriter.content(doc, writtenBy = "Civilight test", writtenAt = Fixtures.NOW)
        // Hashes of the clean files, then the 1-7 bytes are altered (PackageWriter would rehash them).
        val manifest = PackageWriter.withHashes(content.manifest, content.files)
        val path = PackagePaths.episode(ExampleKeys.EPISODE_1_7)
        val files = LinkedHashMap(content.files)
        files[path] = files.getValue(path).copyOf().also { it[it.size / 2] = (it[it.size / 2] + 1).toByte() }
        val entries = LinkedHashMap<String, ByteArray>()
        entries[PackagePaths.MANIFEST] = PackageWriter.jsonBytes(CivilightJson.encodeElement(FileManifest.serializer(), manifest))
        entries.putAll(files)
        val pkg = Fixtures.open(ZipCodec.write(PackagePaths.MIME_PROJECT, entries, Fixtures.NOW))

        assertEquals(listOf(path), pkg.failed.map { it.path })
        assertEquals("열지 못한 부분 · 1-7", pkg.failedMessage)
        assertNull(pkg.doc.index.block(ExampleKeys.BLOCK_043))
        // The rest is there: the story index (1-7 scenes and their counter), other episodes, cast, world.
        assertNotNull(pkg.doc.index.unit(ExampleKeys.SCENE_1_7_3))
        assertEquals(doc.index.unit(ExampleKeys.EPISODE_1_7)!!.nextBlockNumber, pkg.doc.index.unit(ExampleKeys.EPISODE_1_7)!!.nextBlockNumber)
        assertNotNull(pkg.doc.index.block(ExampleKeys.BLOCK_B61))
        assertNotNull(pkg.doc.index.character(ExampleKeys.RIEN))
        assertNotNull(pkg.doc.index.term(ExampleKeys.TERM_ORIGINIUM_LANTERN))
        assertEquals("열지 못한 부분 · 1-7", OpenFileSheets.of(pkg, OpenDecision.Import(null), 0, Fixtures.NOW, Fixtures.ZONE).failedNote)
    }

    @Test
    fun `F-07 a damaged project json imports nothing`() {
        val content = ProjectPackageWriter.content(doc, writtenBy = "Civilight test", writtenAt = Fixtures.NOW)
        val manifest = PackageWriter.withHashes(content.manifest, content.files)
        val files = LinkedHashMap(content.files)
        files[PackagePaths.PROJECT] = "{}".toByteArray()
        val entries = LinkedHashMap<String, ByteArray>()
        entries[PackagePaths.MANIFEST] = PackageWriter.jsonBytes(CivilightJson.encodeElement(FileManifest.serializer(), manifest))
        entries.putAll(files)
        val result = ProjectPackageReader.open(ZipCodec.write(PackagePaths.MIME_PROJECT, entries, Fixtures.NOW))
        assertTrue(result is PackageOpen.Damaged)
        assertEquals(PackagePaths.PROJECT, (result as PackageOpen.Damaged).path)
    }

    @Test
    fun `F-08 unknown ext and unknown files survive import and re-export`() {
        val ext = buildJsonObject {
            put("labsColor", "#ff8800")
            put("nested", buildJsonObject { put("x", 1) })
        }
        val withExt = doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == ExampleKeys.RIEN) it.copy(ext = ext) else it }))
        val content = ProjectPackageWriter.content(withExt, writtenBy = "Civilight test", writtenAt = Fixtures.NOW)
        val labs = """{"experiment":"x","values":[1,2,3]}""".toByteArray()
        val files = LinkedHashMap(content.files)
        files["labs/x.json"] = labs
        files["labs/raw.bin"] = byteArrayOf(1, 2, 3, 4)
        val first = Fixtures.open(PackageWriter.zip(PackagePaths.MIME_PROJECT, content.manifest, files))
        assertEquals(ext, first.doc.index.character(ExampleKeys.RIEN)!!.ext)
        assertEquals(setOf("labs/x.json", "labs/raw.bin"), first.doc.unknownFiles.keys)

        val again = Fixtures.export(first.doc)
        val zip = ZipCodec.read(again)
        assertArrayEquals(labs, zip["labs/x.json"])
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), zip["labs/raw.bin"])
        assertEquals(Sha256.label(labs), manifestOf(again).files["labs/x.json"])
        val second = Fixtures.open(again)
        assertEquals(ext, second.doc.index.character(ExampleKeys.RIEN)!!.ext)
        assertEquals(first.doc, second.doc)
    }

    @Test
    fun `F-09 same project key asks, 새 프로젝트로 by default`() {
        val pkg = Fixtures.open(Fixtures.export(doc))
        val decision = OpenDecisions.decide(pkg, library(doc))
        assertTrue(decision is OpenDecision.AskSameProject)
        val ask = decision as OpenDecision.AskSameProject
        assertEquals(k, ask.existing.key)
        assertEquals(SameProjectChoice.AS_NEW_PROJECT, ask.choices.default)
        assertEquals("새 프로젝트로", ask.choices.title(SameProjectChoice.AS_NEW_PROJECT))
        assertEquals("‘잿빛 새벽의 등불 (2)’로 추가", ask.choices.sub(SameProjectChoice.AS_NEW_PROJECT))
        assertEquals("덮어쓰기", ask.choices.title(SameProjectChoice.OVERWRITE))
        assertEquals("지금 것은 휴지통 · 30일", ask.choices.sub(SameProjectChoice.OVERWRITE))
        assertEquals(listOf("취소", "열기"), ask.choices.buttons)
        // The manifest-only decision agrees.
        assertEquals(decision, OpenDecisions.decide(pkg.manifest, library(doc)))

        val sheet = OpenFileSheets.of(pkg, decision, existingSceneCount = 34, now = Fixtures.NOW, zone = Fixtures.ZONE)
        assertEquals("같은 프로젝트가 이미 있음", sheet.sameProject!!.heading)
        assertEquals("서재 · 씬 34 · 오늘 08:12 수정", sheet.sameProject!!.existingSub)
        // A different name with the same key is still the same project; the same name with another key is not.
        val renamed = Fixtures.entry(doc).copy(name = "다른 이름")
        assertTrue(OpenDecisions.decide(pkg, listOf(renamed)) is OpenDecision.AskSameProject)
        val sameNameOtherKey = Fixtures.entry(doc).copy(key = Fixtures.keys(3)())
        assertEquals(OpenDecision.Import(null), OpenDecisions.decide(pkg, listOf(sameNameOtherKey)))
    }

    @Test
    fun `F-09 no same key imports directly and the sheet sums up the file`() {
        val bytes = Fixtures.export(doc)
        val pkg = Fixtures.open(bytes)
        val decision = OpenDecisions.decide(pkg, emptyList())
        assertEquals(OpenDecision.Import(null), decision)
        val sheet = OpenFileSheets.of(pkg, decision, 0, Fixtures.NOW, Fixtures.ZONE)
        assertNull(sheet.sameProject)
        assertEquals("잿빛 새벽의 등불", sheet.title)
        assertEquals("R-15", sheet.ratingBadge)
        assertEquals("Civilight · 파일에서 열기", sheet.header)
        assertTrue(sheet.summary, sheet.summary.startsWith("씬 34 · 인물 11 · "))
        assertTrue(sheet.summary, sheet.summary.endsWith(" · 11월 22일"))
        assertEquals("씬 31 · 인물 11 · 1.2 MB · 11월 22일", OpenFileSheets.summary(
            doc.copy(story = doc.story.copy(units = doc.story.units.filterNot { it.parentKey == ExampleKeys.EPISODE_2_3 || it.parentKey == ExampleKeys.EPISODE_2_5 })),
            1_258_291, Fixtures.NOW, Fixtures.ZONE,
        ))
    }

    @Test
    fun `F-10 새 프로젝트로 gives a new key and keeps item keys`() {
        val pkg = Fixtures.open(Fixtures.export(doc, Fixtures.recorder()))
        val plan = ProjectImports.asNewProject(pkg, library(doc), Fixtures.recorder(), Fixtures.NOW, Fixtures.keys(11))
        assertNotEquals(k, plan.doc.key)
        assertEquals(plan.doc.key, plan.entry.key)
        assertEquals("잿빛 새벽의 등불 (2)", plan.doc.project.name)
        assertEquals("잿빛 새벽의 등불 (2)", plan.entry.name)
        assertEquals(pkg.doc.cast.characters.map { it.key }, plan.doc.cast.characters.map { it.key })
        assertEquals(pkg.doc.allBlocks.map { it.key }, plan.doc.allBlocks.map { it.key })
        assertEquals(pkg.doc.story.units.map { it.key }, plan.doc.story.units.map { it.key })
        assertNull(plan.replaced)
        // Nothing refers to the old key inside the new project.
        val encoded = io.github.lenticualrstone.civilight.model.doc.ProjectFileSet.split(plan.doc).encode()
        assertTrue(encoded.none { (path, e) -> path.contains(k) || e.toString().contains(k) })
        // The original project is untouched (pure functions).
        assertEquals(Fixtures.example(), doc)
    }

    @Test
    fun `F-11 the next free number is used`() {
        val pkg = Fixtures.open(Fixtures.export(doc))
        val two = Fixtures.otherProject("잿빛 새벽의 등불 (2)", Rating.R15)
        val decision = OpenDecisions.decide(pkg, library(doc, two)) as OpenDecision.AskSameProject
        assertEquals("‘잿빛 새벽의 등불 (3)’로 추가", decision.choices.sub(SameProjectChoice.AS_NEW_PROJECT))
        val plan = ProjectImports.asNewProject(pkg, library(doc, two), RecorderLibrary(), Fixtures.NOW, Fixtures.keys(12))
        assertEquals("잿빛 새벽의 등불 (3)", plan.doc.project.name)
    }

    @Test
    fun `F-12 덮어쓰기 moves the old project to the trash and keeps the key`() {
        val pkg = Fixtures.open(Fixtures.export(doc))
        val existing = Fixtures.entry(doc)
        val plan = ProjectImports.overwrite(pkg, existing, RecorderLibrary(), Fixtures.NOW, Fixtures.ZONE, Fixtures.keys(13))
        assertEquals(k, plan.doc.key)
        val replaced = plan.replaced!!
        assertEquals(k, replaced.oldKey)
        assertNotEquals(k, replaced.newKey)
        val entry = replaced.trashEntry!!
        assertEquals(TrashKind.PROJECT, entry.kind.known)
        assertEquals(TrashReason.REPLACED_BY_OVERWRITE, entry.reason.known)
        assertEquals(LocalDate.of(2026, 12, 22), entry.purgeOn)
        assertEquals(replaced.newKey, entry.projectKey)
        assertEquals(listOf(replaced.newKey), entry.targetKeys)
        assertEquals(Fixtures.NOW, entry.deletedAt)
        // The re-keyed old copy keeps its items.
        val oldCopy = io.github.lenticualrstone.civilight.domain.library.importing.ProjectRekey.rekey(doc, replaced.newKey)
        assertEquals(replaced.newKey, oldCopy.key)
        assertEquals(doc.allBlocks.map { it.key }, oldCopy.allBlocks.map { it.key })
    }

    @Test
    fun `F-12 a same-key project only in the trash is re-keyed without asking`() {
        val pkg = Fixtures.open(Fixtures.export(doc))
        val trashed = Fixtures.entry(doc, deletedAt = Fixtures.at("2026-11-20T00:00:00Z"))
        val decision = OpenDecisions.decide(pkg, listOf(trashed))
        assertEquals(OpenDecision.Import(trashed), decision)
        val plan = ProjectImports.direct(pkg, RecorderLibrary(), Fixtures.NOW, Fixtures.keys(14), trashedSameKey = trashed)
        assertEquals(k, plan.doc.key)
        assertEquals(k, plan.replaced!!.oldKey)
        assertNotEquals(k, plan.replaced!!.newKey)
        assertNull(plan.replaced!!.trashEntry)
    }

    @Test
    fun `F-13 an export-locked file asks for 암호 and hides its manifest`() {
        val bytes = PackageCrypto.encryptWithPassword(Fixtures.export(doc), "등불지기".toCharArray(), fastIterations, SecureRandom(), Fixtures.NOW)
        assertNull(ZipCodec.read(bytes)[PackagePaths.MANIFEST])
        val opened = ProjectPackageReader.open(bytes)
        assertTrue(opened is PackageOpen.Encrypted)
        val sheet = EncryptedFileSheet.of((opened as PackageOpen.Encrypted).header, "civilight-내보내기-1122.civilight")
        assertEquals("암호", sheet.field.label)
        assertEquals("내보낼 때 정한 암호", sheet.field.hint)
        assertEquals("암호 파일", sheet.title)
        assertEquals("이름 · 등급 · 내용 · 열면 보임", sheet.note)
        assertEquals("열면 ‘숨긴 프로젝트’ 묶음으로 · 지문으로 다시 잠금", sheet.afterNote)
        assertEquals(listOf("취소", "열기"), sheet.buttons)
        // Nothing about the project is readable outside.
        val outside = ZipCodec.read(bytes).items.joinToString("") { it.bytes.toString(Charsets.UTF_8) }
        assertFalse(outside.contains("잿빛"))
        assertFalse(outside.contains(k))
    }

    @Test
    fun `F-14 a recovery-locked backup asks for 복구 암호`() {
        val key = RecoveryKeys.create("등불지기의 복구 암호".toCharArray(), fastIterations)
        val bytes = PackageCrypto.encryptWithRecoveryKey(Fixtures.export(doc), key, SecureRandom(), Fixtures.NOW)
        val header = (ProjectPackageReader.open(bytes) as PackageOpen.Encrypted).header
        val sheet = EncryptedFileSheet.of(header, "backup.civilight")
        assertEquals("복구 암호", sheet.field.label)
        assertNull(sheet.field.hint)
    }

    @Test
    fun `F-15 a decrypted file lands hidden on this device`() {
        val black = Fixtures.projectWith("검은 서약 (가제)", Rating.R18, Protections(true, true, true, true), characters = 5)
        for (source in listOf(black, doc)) {
            val bytes = PackageCrypto.encryptWithPassword(Fixtures.export(source), "pw".toCharArray(), fastIterations, SecureRandom(), Fixtures.NOW)
            val opened = EncryptedFiles.open(bytes, "pw".toCharArray())
            val pkg = ((opened as EncryptedOpen.Opened).result as PackageOpen.Ready).pkg
            assertTrue(pkg.decrypted)
            assertEquals(io.github.lenticualrstone.civilight.model.file.PackageLockType.EXPORT, pkg.lockType!!.known)
            val plan = ProjectImports.direct(pkg, RecorderLibrary(), Fixtures.NOW, Fixtures.keys(15))
            assertTrue(plan.doc.project.protections.hidden)
            assertTrue(plan.entry.hidden)
        }
        // A hidden project's 복구 암호 backup stays hidden (its own flag).
        val key = RecoveryKeys.create("복구 암호 1234".toCharArray(), fastIterations)
        val backup = PackageCrypto.encryptWithRecoveryKey(Fixtures.export(black), key, SecureRandom(), Fixtures.NOW)
        val restored = ((EncryptedFiles.open(backup, "복구 암호 1234".toCharArray()) as EncryptedOpen.Opened).result as PackageOpen.Ready).pkg
        assertTrue(ProjectImports.direct(restored, RecorderLibrary(), Fixtures.NOW, Fixtures.keys(17)).doc.project.protections.hidden)
        // An unencrypted file keeps its own flags.
        val plain = ProjectImports.direct(Fixtures.open(Fixtures.export(doc)), RecorderLibrary(), Fixtures.NOW, Fixtures.keys(16))
        assertFalse(plain.doc.project.protections.hidden)
    }

    @Test
    fun `F-16 a hidden project is exported without its cover`() {
        val preview = Fixtures.randomBytes(64)
        val hidden = Fixtures.withProtections(doc, Rating.R15, Protections(hidden = true))
        assertFalse(PackagePaths.PREVIEW in ZipCodec.read(Fixtures.export(hidden, options = ProjectFileOptions(preview = preview))))
        val visible = ZipCodec.read(Fixtures.export(doc, options = ProjectFileOptions(preview = preview)))
        assertArrayEquals(preview, visible[PackagePaths.PREVIEW])
    }

    @Test
    fun `F-17 the encrypted container holds exactly three entries`() {
        val bytes = PackageCrypto.encryptWithPassword(Fixtures.export(doc), "pw".toCharArray(), fastIterations, SecureRandom(), Fixtures.NOW)
        val zip = ZipCodec.read(bytes)
        assertEquals(listOf(PackagePaths.MIMETYPE, PackagePaths.ENCRYPTION, PackagePaths.PAYLOAD), zip.paths)
        assertTrue(zip.items.first().stored)
        assertEquals(PackagePaths.MIME_PROJECT, zip[PackagePaths.MIMETYPE]!!.toString(Charsets.US_ASCII))
        val header = CivilightJson.json.parseToJsonElement(zip[PackagePaths.ENCRYPTION]!!.toString(Charsets.UTF_8)).jsonObject
        assertEquals(setOf("format", "lockType", "kdf", "iterations", "salt", "cipher", "nonce"), header.keys)
        assertEquals("civilight-encrypted", header.getValue("format").jsonPrimitive.content)
        assertEquals("export", header.getValue("lockType").jsonPrimitive.content)
        assertEquals("PBKDF2-HMAC-SHA256", header.getValue("kdf").jsonPrimitive.content)
        assertEquals("AES-256-GCM", header.getValue("cipher").jsonPrimitive.content)
    }

    @Test
    fun `F-18 a record already in the library gains a link and is not duplicated`() {
        val library = Fixtures.recorder()
        val pkg = Fixtures.open(Fixtures.export(doc, library))
        val plan = ProjectImports.asNewProject(pkg, library(doc), library, Fixtures.NOW, Fixtures.keys(18))
        val merged = plan.recorder
        assertEquals(library.records.map { it.key }, merged.records.map { it.key })
        assertEquals(1, merged.records.count { it.key == ExampleKeys.RECORDER_R1 })
        assertEquals(setOf(k, plan.doc.key), merged.linksOf(ExampleKeys.RECORDER_R1).map { it.projectKey }.toSet())
        // Every link into 잿빛 now also exists for 잿빛 (2), with its own key.
        val oldLinks = library.links.filter { it.projectKey == k && it.recordKey == ExampleKeys.RECORDER_R1 }
        val newLinks = merged.links.filter { it.projectKey == plan.doc.key && it.recordKey == ExampleKeys.RECORDER_R1 }
        assertEquals(oldLinks.map { it.targetKind to (if (it.targetKey == k) plan.doc.key else it.targetKey) }.toSet(), newLinks.map { it.targetKind to it.targetKey }.toSet())
        assertTrue(newLinks.none { n -> library.links.any { it.key == n.key } })
        // Local text kept.
        assertEquals(library.records.first { it.key == ExampleKeys.RECORDER_R1 }, merged.records.first { it.key == ExampleKeys.RECORDER_R1 })
    }

    @Test
    fun `F-19 one record linked from two restored projects merges into one with two links`() {
        val glass = Fixtures.projectWith("유리 정원", Rating.ALL, format = io.github.lenticualrstone.civilight.model.project.ProjectFormat.SHORT, scenes = 6, characters = 4)
        val base = Fixtures.recorder()
        val record = base.records.first { it.key == ExampleKeys.RECORDER_R1 }
        val shared = base.copy(
            links = base.links + RecorderLink(
                key = Fixtures.keys(19)(), recordKey = record.key, projectKey = glass.key,
                targetKind = RecorderTargetKind.PROJECT.open(), targetKey = glass.key, origin = LinkOrigin.MANUAL.open(),
                createdAt = Fixtures.NOW,
            ),
        )
        val ash = Fixtures.open(Fixtures.export(doc, shared))
        val glassPkg = Fixtures.open(Fixtures.export(glass, shared))
        val first = ProjectImports.direct(ash, RecorderLibrary(), Fixtures.NOW, Fixtures.keys(20))
        val second = ProjectImports.direct(glassPkg, first.recorder, Fixtures.NOW, Fixtures.keys(21))
        val restored = second.recorder
        assertEquals(1, restored.records.count { it.key == ExampleKeys.RECORDER_R1 })
        assertEquals(setOf(k, glass.key), restored.linksOf(ExampleKeys.RECORDER_R1).map { it.projectKey }.toSet())
        assertTrue(restored.linksOf(ExampleKeys.RECORDER_R1).any { it.projectKey == glass.key && it.targetKey == glass.key })
    }

    @Test
    fun `manifest and project json disagreeing on the key - the content decides`() {
        val content = ProjectPackageWriter.content(doc, writtenBy = "Civilight test", writtenAt = Fixtures.NOW)
        val forged: Key = Fixtures.keys(30)()
        val manifest = content.manifest.copy(project = content.manifest.project!!.copy(key = forged, title = "다른 이름"))
        val pkg = Fixtures.open(PackageWriter.zip(PackagePaths.MIME_PROJECT, manifest, content.files))
        assertEquals(k, pkg.manifest.project!!.key)
        assertEquals("잿빛 새벽의 등불", pkg.manifest.project!!.title)
        // So the same-project check sees the key the import uses.
        assertTrue(OpenDecisions.decide(pkg, library(doc)) is OpenDecision.AskSameProject)
        assertTrue(OpenDecisions.decide(pkg.manifest, library(doc)) is OpenDecision.AskSameProject)
    }

    @Test
    fun `not a package`() {
        assertTrue(ProjectPackageReader.open(byteArrayOf(1, 2, 3)) is PackageOpen.NotAPackage)
        val wrongMime = ZipCodec.write("application/zip", mapOf("project.json" to "{}".toByteArray()), 0L)
        assertTrue(ProjectPackageReader.open(wrongMime) is PackageOpen.NotAPackage)
        val recorderFile = ZipCodec.write(PackagePaths.MIME_RECORDER, mapOf("records.json" to "{}".toByteArray()), 0L)
        assertTrue(ProjectPackageReader.open(recorderFile) is PackageOpen.NotAPackage)
    }

    @Test
    fun `features follow the content`() {
        val plain = manifestOf(Fixtures.export(doc))
        assertTrue("fields.custom" in plain.features.optional)
        assertTrue("recorder" !in plain.features.optional)
        assertTrue("trash" !in plain.features.optional)
        val full = manifestOf(Fixtures.export(doc, Fixtures.recorder(), ProjectFileOptions(includeTrash = true)))
        assertTrue("recorder" in full.features.optional)
        assertTrue("trash" in full.features.optional)
        assertEquals(full.features.optional.distinct(), full.features.optional)
    }

    @Test
    fun `without trash no non-live row is written`() {
        val zip = ZipCodec.read(Fixtures.export(doc, Fixtures.recorder()))
        assertTrue(zip.paths.none { it.startsWith("trash/") })
        val cast = zip[PackagePaths.CAST]!!.toString(Charsets.UTF_8)
        assertFalse(cast.contains(ExampleKeys.NOAH + "\",\"recordNo"))
        val tree: JsonElement = CivilightJson.json.parseToJsonElement(cast)
        val keys = tree.jsonObject.getValue("characters").let { it as kotlinx.serialization.json.JsonArray }.map { it.jsonObject.getValue("key").jsonPrimitive.content }
        assertFalse(ExampleKeys.NOAH in keys)
    }
}
