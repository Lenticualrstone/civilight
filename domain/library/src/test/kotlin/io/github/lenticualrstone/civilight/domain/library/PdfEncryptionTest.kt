package io.github.lenticualrstone.civilight.domain.library

import io.github.lenticualrstone.civilight.domain.library.export.PdfEncryption
import io.github.lenticualrstone.civilight.domain.library.export.PdfEncryptionException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.security.SecureRandom
import java.util.zip.Deflater
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * `PDF 암호` (LIB-43 `ED`: AES-256, Standard security handler R6). The test re-derives the file key
 * from the password the way a reader does (Algorithm 2.A) and decrypts the page stream and the info
 * strings, and checks every xref offset.
 */
class PdfEncryptionTest {
    /** A minimal `PdfDocument`-shaped file: catalog, pages, one page with a Flate content stream, info. */
    private fun samplePdf(): ByteArray {
        val content = "BT /F1 24 Tf 72 720 Td (Hello Civilight) Tj ET".toByteArray()
        val deflater = Deflater().apply { setInput(content); finish() }
        val buf = ByteArray(1024)
        val packed = buf.copyOf(deflater.deflate(buf))
        val parts = ArrayList<ByteArray>()
        val offsets = ArrayList<Int>()
        var size = 0
        fun addBytes(b: ByteArray) {
            parts += b
            size += b.size
        }
        fun obj(text: String) {
            offsets += size
            addBytes(text.toByteArray(Charsets.ISO_8859_1))
        }
        addBytes("%PDF-1.4\n%âãÏÓ\n".toByteArray(Charsets.ISO_8859_1))
        obj("1 0 obj\n<</Type /Catalog /Pages 2 0 R>>\nendobj\n")
        obj("2 0 obj\n<</Type /Pages /Count 1 /Kids [3 0 R]>>\nendobj\n")
        obj("3 0 obj\n<</Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources <</Font <</F1 5 0 R>>>> /Contents 4 0 R>>\nendobj\n")
        offsets += size
        addBytes("4 0 obj\n<</Filter /FlateDecode /Length ${packed.size}>> stream\n".toByteArray(Charsets.ISO_8859_1))
        addBytes(packed)
        addBytes("\nendstream\nendobj\n".toByteArray(Charsets.ISO_8859_1))
        obj("5 0 obj\n<</Type /Font /Subtype /Type1 /BaseFont /Helvetica>>\nendobj\n")
        obj("6 0 obj\n<</Producer (Skia/PDF m120) /Title (\\(test\\) a\\\\b)>>\nendobj\n")
        val xref = size
        val sb = StringBuilder("xref\n0 7\n0000000000 65535 f \n")
        offsets.forEach { sb.append(it.toString().padStart(10, '0')).append(" 00000 n \n") }
        sb.append("trailer\n<</Size 7 /Root 1 0 R /Info 6 0 R>>\nstartxref\n$xref\n%%EOF\n")
        addBytes(sb.toString().toByteArray(Charsets.ISO_8859_1))
        val out = ByteArray(size)
        var at = 0
        parts.forEach { it.copyInto(out, at); at += it.size }
        return out
    }

    private fun ByteArray.text() = toString(Charsets.ISO_8859_1)

    private fun hexOf(pdf: String, key: String): ByteArray {
        val m = Regex("$key <([0-9a-f]+)>").find(pdf) ?: error("no $key")
        val h = m.groupValues[1]
        return ByteArray(h.length / 2) { h.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }

    /** Algorithm 2.A for the user password: validate `U`, unwrap `UE`. */
    private fun fileKey(pdf: String, password: String): ByteArray? {
        val u = hexOf(pdf, "/U")
        val ue = hexOf(pdf, "/UE")
        val pw = password.toByteArray(Charsets.UTF_8)
        val check = hashOf(pw, u.copyOfRange(32, 40))
        if (!check.contentEquals(u.copyOfRange(0, 32))) return null
        val kek = hashOf(pw, u.copyOfRange(40, 48))
        val c = Cipher.getInstance("AES/CBC/NoPadding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(kek, "AES"), IvParameterSpec(ByteArray(16)))
        return c.doFinal(ue)
    }

    private fun hashOf(pw: ByteArray, salt: ByteArray): ByteArray {
        return PdfEncryption.hash(pw, salt, ByteArray(0))
    }

    private fun aesOpen(key: ByteArray, data: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(data.copyOfRange(0, 16)))
        return c.doFinal(data, 16, data.size - 16)
    }

    @Test
    fun `the password opens the file key and the page stream and info strings decrypt`() {
        val plain = samplePdf()
        val locked = PdfEncryption.encrypt(plain, "등불지기1234".toCharArray(), SecureRandom())
        val text = locked.text()
        assertTrue(text.startsWith("%PDF-1.7"))
        assertTrue(text.contains("/Filter /Standard /V 5 /R 6"))
        assertTrue(text.contains("/Encrypt 7 0 R"))
        assertFalse("no plain producer string left", text.contains("Skia/PDF"))

        val key = fileKey(text, "등불지기1234")!!
        assertEquals(32, key.size)
        assertEquals(null, fileKey(text, "틀린암호"))

        // The page stream: between `stream\n` and `\nendstream` of object 4.
        val start = text.indexOf("4 0 obj")
        val dataStart = text.indexOf("stream\n", start) + "stream\n".length
        val dataEnd = text.indexOf("\nendstream", dataStart)
        val packed = aesOpen(key, locked.copyOfRange(dataStart, dataEnd))
        val inflater = java.util.zip.Inflater().apply { setInput(packed) }
        val out = ByteArray(1024)
        val n = inflater.inflate(out)
        assertEquals("BT /F1 24 Tf 72 720 Td (Hello Civilight) Tj ET", String(out, 0, n, Charsets.ISO_8859_1))
        val length = Regex("/Length (\\d+)").findAll(text.substring(start, dataStart)).last().groupValues[1].toInt()
        assertEquals(dataEnd - dataStart, length)

        val title = Regex("/Title <([0-9a-f]+)>").find(text)!!.groupValues[1]
        val titleBytes = ByteArray(title.length / 2) { title.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        assertEquals("(test) a\\b", aesOpen(key, titleBytes).toString(Charsets.ISO_8859_1))
    }

    @Test
    fun `every xref offset points at its object`() {
        val locked = PdfEncryption.encrypt(samplePdf(), "pw".toCharArray()).text()
        val xref = locked.substring(locked.lastIndexOf("startxref") + 10).trim().lines().first().toInt()
        val lines = locked.substring(xref).lines()
        assertEquals("xref", lines[0])
        val count = lines[1].split(' ')[1].toInt()
        for (n in 1 until count) {
            val offset = lines[2 + n].substring(0, 10).toInt()
            assertTrue(locked.startsWith("$n 0 obj", offset))
        }
        // Debug copy for a check with a real reader (PDFKit): build/pdf-encryption/sample.pdf, password `pw`.
        File("build/pdf-encryption").mkdirs()
        File("build/pdf-encryption/sample.pdf").writeBytes(locked.toByteArray(Charsets.ISO_8859_1))
    }

    @Test
    fun `two runs differ and an encrypted or broken file is refused`() {
        val plain = samplePdf()
        val a = PdfEncryption.encrypt(plain, "pw".toCharArray())
        assertNotEquals(a.text(), PdfEncryption.encrypt(plain, "pw".toCharArray()).text())
        try {
            PdfEncryption.encrypt(a, "pw".toCharArray())
            fail("already encrypted")
        } catch (_: PdfEncryptionException) {
        }
        try {
            PdfEncryption.encrypt("hello".toByteArray(), "pw".toCharArray())
            fail("not a pdf")
        } catch (_: PdfEncryptionException) {
        }
        assertArrayEquals(plain, samplePdf())
    }
}
