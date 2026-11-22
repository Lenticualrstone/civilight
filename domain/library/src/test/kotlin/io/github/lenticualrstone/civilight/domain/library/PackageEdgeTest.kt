package io.github.lenticualrstone.civilight.domain.library

import io.github.lenticualrstone.civilight.domain.library.crypto.DecryptResult
import io.github.lenticualrstone.civilight.domain.library.crypto.EncryptedFiles
import io.github.lenticualrstone.civilight.domain.library.crypto.EncryptedOpen
import io.github.lenticualrstone.civilight.domain.library.crypto.PackageCrypto
import io.github.lenticualrstone.civilight.domain.library.crypto.RecoveryKeys
import io.github.lenticualrstone.civilight.domain.library.format.BlobReader
import io.github.lenticualrstone.civilight.domain.library.format.FormatVersion
import io.github.lenticualrstone.civilight.domain.library.format.PackageOpen
import io.github.lenticualrstone.civilight.domain.library.format.PackageZipException
import io.github.lenticualrstone.civilight.domain.library.format.ProjectFileOptions
import io.github.lenticualrstone.civilight.domain.library.format.ProjectPackageReader
import io.github.lenticualrstone.civilight.domain.library.format.ProjectPackageWriter
import io.github.lenticualrstone.civilight.domain.library.format.RecorderPackage
import io.github.lenticualrstone.civilight.domain.library.format.RecorderPackageOpen
import io.github.lenticualrstone.civilight.domain.library.format.Sha256
import io.github.lenticualrstone.civilight.domain.library.format.UnsupportedFormatException
import io.github.lenticualrstone.civilight.domain.library.format.UpgradeChain
import io.github.lenticualrstone.civilight.domain.library.format.UpgradeStep
import io.github.lenticualrstone.civilight.domain.library.format.ZipCodec
import io.github.lenticualrstone.civilight.domain.library.importing.ProjectRekey
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.RecorderAudio
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.security.SecureRandom

/** Encryption, container, version and recorder-file edges beyond the §4 cases. */
class PackageEdgeTest {
    private val doc = Fixtures.example()
    private val fast = 1_000
    private val plain: ByteArray by lazy { Fixtures.export(doc) }

    private fun rebuild(outer: ByteArray, change: (String, ByteArray) -> ByteArray): ByteArray {
        val zip = ZipCodec.read(outer)
        val entries = LinkedHashMap<String, ByteArray>()
        for (item in zip.items.drop(1)) entries[item.path] = change(item.path, item.bytes)
        return ZipCodec.write(zip[PackagePaths.MIMETYPE]!!.toString(Charsets.US_ASCII), entries, 0L)
    }

    @Test
    fun `a wrong password fails and allows another try`() {
        val bytes = PackageCrypto.encryptWithPassword(plain, "맞는 암호".toCharArray(), fast)
        assertEquals(EncryptedOpen.WrongPassword, EncryptedFiles.open(bytes, "틀린 암호".toCharArray()))
        assertTrue(EncryptedFiles.open(bytes, "맞는 암호".toCharArray()) is EncryptedOpen.Opened)
    }

    @Test
    fun `an altered header or payload never decrypts`() {
        val pw = "pw".toCharArray()
        val bytes = PackageCrypto.encryptWithPassword(plain, pw, fast)
        val header = ZipCodec.read(bytes)[PackagePaths.ENCRYPTION]!!.toString(Charsets.UTF_8)
        // Same password, one extra space in the header: the exact header bytes are the GCM associated data.
        val relabeled = rebuild(bytes) { path, b -> if (path == PackagePaths.ENCRYPTION) header.replace("\"export\"", "\"export\" ").toByteArray() else b }
        assertEquals(DecryptResult.WrongPassword, PackageCrypto.decrypt(relabeled, pw))
        val flipped = rebuild(bytes) { path, b -> if (path == PackagePaths.PAYLOAD) b.copyOf().also { it[10] = (it[10] + 1).toByte() } else b }
        assertEquals(DecryptResult.WrongPassword, PackageCrypto.decrypt(flipped, pw))
        val fewerIterations = rebuild(bytes) { path, b -> if (path == PackagePaths.ENCRYPTION) header.replace("\"iterations\":1000", "\"iterations\":999").toByteArray() else b }
        assertEquals(DecryptResult.WrongPassword, PackageCrypto.decrypt(fewerIterations, pw))
    }

    @Test
    fun `an unknown method is reported, not guessed`() {
        val bytes = PackageCrypto.encryptWithPassword(plain, "pw".toCharArray(), fast)
        val argon = rebuild(bytes) { path, b ->
            if (path == PackagePaths.ENCRYPTION) b.toString(Charsets.UTF_8).replace("PBKDF2-HMAC-SHA256", "Argon2id").toByteArray() else b
        }
        assertEquals(DecryptResult.Unsupported("Argon2id", "AES-256-GCM"), PackageCrypto.decrypt(argon, "pw".toCharArray()))
        assertEquals(EncryptedOpen.Unsupported("Argon2id", "AES-256-GCM"), EncryptedFiles.open(argon, "pw".toCharArray()))
        assertTrue(PackageCrypto.decrypt(plain, "pw".toCharArray()) is DecryptResult.NotEncrypted)
    }

    @Test
    fun `the default header records 600000 PBKDF2 iterations and fresh salt and nonce`() {
        val a = PackageCrypto.headerOf(PackageCrypto.encryptWithPassword(byteArrayOf(1), "pw".toCharArray()))!!
        assertEquals(600_000, a.iterations)
        assertEquals("PBKDF2-HMAC-SHA256", a.kdf)
        assertEquals("AES-256-GCM", a.cipher)
        assertEquals(16, java.util.Base64.getDecoder().decode(a.salt).size)
        assertEquals(12, java.util.Base64.getDecoder().decode(a.nonce).size)
        val b = PackageCrypto.headerOf(PackageCrypto.encryptWithPassword(byteArrayOf(1), "pw".toCharArray(), fast))!!
        assertFalse(a.salt == b.salt)
        assertFalse(a.nonce == b.nonce)
        assertNull(PackageCrypto.headerOf(plain))
    }

    @Test
    fun `a recovery key opens by password on any device and by data key on this one`() {
        val pw = "등불지기의 복구 암호".toCharArray()
        val key = RecoveryKeys.create(pw, fast)
        val material = key.toMaterial(dataKeyWrappedByKeystore = "keystore-wrapped", keystoreAlias = "civilight-backup", setAt = Fixtures.NOW)
        assertEquals("PBKDF2-HMAC-SHA256", material.kdf)
        assertFalse(material.toString().contains("등불지기"))
        assertArrayEquals(key.dataKey, RecoveryKeys.unlock(material, pw))
        assertNull(RecoveryKeys.unlock(material, "틀림".toCharArray()))

        // Daily backups rebuild the key from material + Keystore data key, without typing.
        val rebuilt = RecoveryKeys.fromMaterial(material, key.dataKey)
        val bytes = PackageCrypto.encryptWithRecoveryKey(plain, rebuilt)
        val byPassword = PackageCrypto.decrypt(bytes, pw)
        assertTrue(byPassword is DecryptResult.Ok)
        assertArrayEquals(plain, (byPassword as DecryptResult.Ok).inner)
        assertTrue(PackageCrypto.decryptWithDataKey(bytes, key.dataKey) is DecryptResult.Ok)
        assertEquals(DecryptResult.WrongPassword, PackageCrypto.decrypt(bytes, "틀림".toCharArray()))
        // A changed 복구 암호 makes a new key; old backups keep the old password (§6 Q24).
        val changed = RecoveryKeys.create("새 복구 암호 12".toCharArray(), fast)
        assertEquals(DecryptResult.WrongPassword, PackageCrypto.decryptWithDataKey(bytes, changed.dataKey))
    }

    @Test
    fun `복구 암호 정하기 needs 8 characters and a matching repeat`() {
        assertTrue(RecoveryKeys.canSet("12345678".toCharArray(), "12345678".toCharArray()))
        assertFalse(RecoveryKeys.canSet("1234567".toCharArray(), "1234567".toCharArray()))
        assertFalse(RecoveryKeys.canSet("12345678".toCharArray(), "12345679".toCharArray()))
    }

    @Test
    fun `versions compare numerically and parse strictly`() {
        assertTrue(FormatVersion.parse("1.10") > FormatVersion.parse("1.9"))
        assertTrue(FormatVersion.parse("2.0") > FormatVersion.parse("1.99"))
        assertNull(FormatVersion.parseOrNull("1"))
        assertNull(FormatVersion.parseOrNull("1.a"))
        assertNull(FormatVersion.parseOrNull("-1.0"))
        assertEquals(FormatVersion(1, 0), FormatVersion.CURRENT)
        // An unreadable minReaderVersion refuses.
        val content = ProjectPackageWriter.content(doc, writtenBy = "t", writtenAt = Fixtures.NOW)
        val bad = io.github.lenticualrstone.civilight.domain.library.format.PackageWriter.zip(
            PackagePaths.MIME_PROJECT, content.manifest.copy(minReaderVersion = "eins"), content.files,
        )
        assertTrue(ProjectPackageReader.open(bad) is PackageOpen.Newer)
    }

    @Test
    fun `the chain walks minor gaps and refuses an unknown major`() {
        val seen = ArrayList<String>()
        fun step(a: String, b: String) = UpgradeStep(FormatVersion.parse(a), FormatVersion.parse(b)) { seen += "$a→$b"; it }
        val chain = UpgradeChain(FormatVersion(1, 3), listOf(step("1.0", "1.1"), step("1.2", "1.3")))
        assertEquals(listOf("1.0→1.1", "1.2→1.3"), chain.upgrade(FormatVersion(1, 0), emptyMap()).applied.map { it.toString() })
        assertEquals(listOf("1.0→1.1", "1.2→1.3"), seen)
        assertTrue(chain.upgrade(FormatVersion(1, 3), emptyMap()).applied.isEmpty())
        val toTwo = UpgradeChain(FormatVersion(2, 0), emptyList())
        try {
            toTwo.upgrade(FormatVersion(1, 0), emptyMap())
            fail("expected UnsupportedFormatException")
        } catch (e: UnsupportedFormatException) {
            assertNotNull(e.message)
        }
        try {
            UpgradeStep(FormatVersion(1, 1), FormatVersion(1, 0)) { it }
            fail("a step must move forward")
        } catch (e: IllegalArgumentException) {
            assertNotNull(e.message)
        }
    }

    @Test
    fun `unsafe or oversized containers are rejected`() {
        for (bad in listOf("../x.json", "/abs.json", "a/../../b", "a\\b")) {
            try {
                ZipCodec.write(PackagePaths.MIME_PROJECT, mapOf(bad to byteArrayOf(1)), 0L)
                fail("wrote $bad")
            } catch (e: PackageZipException) {
                assertTrue(e.message!!.contains("unsafe"))
            }
        }
        try {
            ZipCodec.read(plain, maxBytes = 1_000)
            fail("read past the size cap")
        } catch (e: PackageZipException) {
            assertTrue(e.message!!.contains("larger"))
        }
        // A hand-made zip with a traversal entry is not a package.
        val out = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(out).use { z ->
            z.putNextEntry(java.util.zip.ZipEntry("mimetype")); z.write(PackagePaths.MIME_PROJECT.toByteArray()); z.closeEntry()
            z.putNextEntry(java.util.zip.ZipEntry("../evil.json")); z.write("{}".toByteArray()); z.closeEntry()
        }
        assertTrue(ProjectPackageReader.open(out.toByteArray()) is PackageOpen.NotAPackage)
    }

    @Test
    fun `only referenced blobs are written and a bad blob is reported`() {
        val cover = Fixtures.randomBytes(200, seed = 3)
        val loose = Fixtures.randomBytes(100, seed = 4)
        fun blob(bytes: ByteArray) = Blob(Sha256.hex(bytes), "webp", "image/webp", bytes.size.toLong(), source = BlobSource.GALLERY.open(), importedAt = Fixtures.NOW)
        val use = ImageUse(
            key = Fixtures.keys(50)(), ownerKind = ImageOwnerKind.CHARACTER.open(), ownerKey = ExampleKeys.RIEN,
            slot = ImageSlot.CHARACTER_PORTRAIT.open(), source = Sha256.hex(cover),
        )
        val withImages = doc.copy(assets = doc.assets.copy(blobs = listOf(blob(cover), blob(loose)), imageUses = doc.assets.imageUses + use))
        val content = ProjectPackageWriter.content(withImages, blobs = BlobReader.of(mapOf(Sha256.hex(cover) to cover, Sha256.hex(loose) to loose)), writtenBy = "t", writtenAt = Fixtures.NOW)
        assertTrue(PackagePaths.asset(Sha256.hex(cover), "webp") in content.files)
        assertFalse(PackagePaths.asset(Sha256.hex(loose), "webp") in content.files)
        assertTrue(content.missingBlobs.isEmpty())
        val pkg = Fixtures.open(content.toZip())
        assertArrayEquals(cover, pkg.blobs[Sha256.hex(cover)])
        assertEquals(listOf(Sha256.hex(cover)), pkg.doc.assets.blobs.map { it.hash })

        val missing = ProjectPackageWriter.content(withImages, blobs = BlobReader.NONE, writtenBy = "t", writtenAt = Fixtures.NOW)
        assertEquals(listOf(Sha256.hex(cover)), missing.missingBlobs)
    }

    @Test
    fun `recorder library file round trip with hidden records and audio`() {
        val base = Fixtures.recorder()
        val audioBytes = Fixtures.randomBytes(300, seed = 9)
        val audio = RecorderAudio(hash = Sha256.hex(audioBytes), mime = "audio/mp4", bytes = audioBytes.size.toLong(), durationMs = 24_000)
        val library = base.copy(
            records = base.records.map {
                when (it.key) {
                    ExampleKeys.RECORDER_R1 -> it.copy(audio = audio)
                    ExampleKeys.RECORDER_R4 -> it.copy(hidden = true)
                    else -> it
                }
            },
        )
        val bytes = RecorderPackage.write(library, includeHidden = false, includeAudio = true, audio = { if (it == audio.hash) audioBytes else null }, writtenBy = "t", writtenAt = Fixtures.NOW)
        val zip = ZipCodec.read(bytes)
        assertEquals(PackagePaths.MIME_RECORDER, zip[PackagePaths.MIMETYPE]!!.toString(Charsets.US_ASCII))
        val read = (RecorderPackage.read(bytes) as RecorderPackageOpen.Ready).content
        assertEquals("civilight-recorder", read.manifest.format)
        val keys = read.library.records.map { it.key }
        assertFalse(ExampleKeys.RECORDER_R4 in keys)
        assertFalse(ExampleKeys.RECORDER_TRASHED in keys)
        assertTrue(ExampleKeys.RECORDER_R1 in keys)
        assertArrayEquals(audioBytes, read.audio[audio.hash])
        // Links keep their project keys in the library file.
        assertEquals(
            library.linksOf(ExampleKeys.RECORDER_R1).map { it.projectKey to it.targetKey }.toSet(),
            read.library.linksOf(ExampleKeys.RECORDER_R1).map { it.projectKey to it.targetKey }.toSet(),
        )

        // Without audio: no audio entries.
        val noAudio = RecorderPackage.write(library, includeHidden = false, includeAudio = false, audio = { audioBytes }, writtenBy = "t", writtenAt = Fixtures.NOW)
        assertTrue(ZipCodec.read(noAudio).paths.none { it.startsWith("assets/") })

        // Hidden records only inside a recovery-locked file.
        val key = RecoveryKeys.create("복구 암호 1234".toCharArray(), fast)
        val withHidden = RecorderPackage.write(library, includeHidden = true, includeAudio = false, audio = { null }, writtenBy = "t", writtenAt = Fixtures.NOW)
        val locked = PackageCrypto.encryptWithRecoveryKey(withHidden, key, SecureRandom(), Fixtures.NOW, PackagePaths.MIME_RECORDER)
        assertEquals(PackagePaths.MIME_RECORDER, ZipCodec.read(locked)[PackagePaths.MIMETYPE]!!.toString(Charsets.US_ASCII))
        assertTrue(RecorderPackage.read(locked) is RecorderPackageOpen.Encrypted)
        assertTrue(ProjectPackageReader.open(locked) is PackageOpen.NotAPackage)
        val inner = (PackageCrypto.decrypt(locked, "복구 암호 1234".toCharArray()) as DecryptResult.Ok).inner
        val unlocked = (RecorderPackage.read(inner) as RecorderPackageOpen.Ready).content
        assertTrue(ExampleKeys.RECORDER_R4 in unlocked.library.records.map { it.key })
    }

    @Test
    fun `re-keying a project moves every reference to the project key`() {
        val one = Fixtures.projectWith("한 단계", io.github.lenticualrstone.civilight.model.project.Rating.ALL, scenes = 2).let { d ->
            // A one-level project numbers on the project key (§4.1).
            d.copy(
                project = d.project.copy(structure = io.github.lenticualrstone.civilight.model.project.StoryStructure(
                    io.github.lenticualrstone.civilight.model.project.StructurePreset.CUSTOM.open(), listOf("씬"),
                )),
                story = d.story.copy(units = d.story.units.filter { it.level == 2 }.map { it.copy(parentKey = null, level = 0) }),
                episodes = mapOf(d.key to io.github.lenticualrstone.civilight.model.file.EpisodeFile(unitKey = d.key)),
            )
        }
        val newKey = Fixtures.keys(60)()
        val moved = ProjectRekey.rekey(one, newKey)
        assertEquals(newKey, moved.key)
        assertEquals(setOf(newKey), moved.episodes.keys)
        assertEquals(newKey, moved.episodes.getValue(newKey).unitKey)
        assertEquals(one.story.units.map { it.key }, moved.story.units.map { it.key })
        // The example's cover image is owned by the project key.
        val ash = ProjectRekey.rekey(doc, newKey)
        assertEquals(newKey, ash.assets.imageUses.single { it.key == ExampleKeys.IMAGE_PROJECT_COVER }.ownerKey)
        assertEquals(doc.allBlocks.map { it.key }, ash.allBlocks.map { it.key })
    }

    @Test
    fun `the trash travels only with 휴지통도 포함`() {
        val withTrash = Fixtures.open(Fixtures.export(doc, options = ProjectFileOptions(includeTrash = true)))
        assertEquals(doc.trash, withTrash.doc.trash)
        assertNotNull(withTrash.doc.index.character(ExampleKeys.NOAH))
        val without = Fixtures.open(Fixtures.export(doc))
        assertTrue(without.doc.trash.entries.isEmpty())
        assertNull(without.doc.index.character(ExampleKeys.NOAH))
    }
}
