package io.github.lenticualrstone.civilight.domain.library.export

import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.Normalizer
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/** The PDF could not be rewritten (a structure this writer does not handle); nothing is written. */
class PdfEncryptionException(message: String) : Exception(message)

/**
 * `PDF 암호` of the 대본 PDF (LIB-43 `ED`: standard PDF encryption, AES-256): rewrites a plain PDF as
 * Standard security handler V5 / R6 (ISO 32000-2 §7.6.4, `AESV3`). Every string and stream of every
 * object is encrypted with a random 256-bit file key (AES-256-CBC, random IV, PKCS#5 padding); the key
 * is wrapped for the user [password] (`U`, `UE`) and for a random owner password (`O`, `OE`), with the
 * permissions sealed in `Perms`. The cross-reference table and trailer are written fresh.
 *
 * Input: a classic PDF with a cross-reference table and no object streams (what Android's
 * `PdfDocument` writes). Anything else is refused with [PdfEncryptionException] rather than written
 * unprotected.
 */
object PdfEncryption {
    /** All permissions; the password protects reading (`ED`). */
    const val PERMISSIONS: Int = -4

    fun encrypt(pdf: ByteArray, password: CharArray, random: SecureRandom = SecureRandom()): ByteArray {
        require(password.isNotEmpty()) { "password required" }
        val parsed = PdfFile.parse(pdf)
        if (parsed.trailer.entry("/Encrypt") != null) throw PdfEncryptionException("already encrypted")

        val fileKey = random.bytes(32)
        val user = passwordBytes(password)
        val owner = random.bytes(32)
        val keys = Keys.make(user, owner, fileKey, random)
        user.fill(0)

        val out = ByteArrayOutputStream()
        out.write("%PDF-1.7\n%".toByteArray(Charsets.ISO_8859_1))
        out.write(byteArrayOf(0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), '\n'.code.toByte()))
        val offsets = HashMap<Int, Pair<Int, Int>>() // number → (offset, generation)
        for (obj in parsed.objects) {
            offsets[obj.number] = out.size() to obj.generation
            val seal = Sealer(fileKey, random)
            if (obj.stream != null) {
                val data = seal.bytes(obj.stream)
                val dict = (obj.value as? PdfVal.Dict ?: throw PdfEncryptionException("stream without a dictionary"))
                val type = dict.entry("/Type")
                if (type is PdfVal.Name && (type.raw == "/XRef" || type.raw == "/ObjStm")) throw PdfEncryptionException("object or xref streams")
                val sealed = PdfVal.Dict(dict.entries.filter { it.first.raw != "/Length" }.map { it.first to seal.value(it.second) } + (PdfVal.Name("/Length") to PdfVal.Raw(data.size.toString())))
                out.write("${obj.number} ${obj.generation} obj\n".latin1())
                out.write(sealed.serialize().latin1())
                out.write("\nstream\n".latin1())
                out.write(data)
                out.write("\nendstream\nendobj\n".latin1())
            } else {
                out.write("${obj.number} ${obj.generation} obj\n".latin1())
                out.write(seal.value(obj.value).serialize().latin1())
                out.write("\nendobj\n".latin1())
            }
        }
        val encryptNumber = (parsed.objects.maxOfOrNull { it.number } ?: 0) + 1
        offsets[encryptNumber] = out.size() to 0
        out.write("$encryptNumber 0 obj\n${keys.dictionary()}\nendobj\n".latin1())

        val size = encryptNumber + 1
        val xref = out.size()
        val table = StringBuilder("xref\n0 $size\n0000000000 65535 f \n")
        for (n in 1 until size) {
            val at = offsets[n]
            if (at == null) table.append("0000000000 00000 f \n") else table.append(at.first.toString().padStart(10, '0')).append(' ').append(at.second.toString().padStart(5, '0')).append(" n \n")
        }
        out.write(table.toString().latin1())
        val id = (parsed.trailer.entry("/ID") as? PdfVal.Arr)?.items?.firstOrNull() as? PdfVal.Str
        val idHex = hex(id?.bytes ?: random.bytes(16))
        val keep = parsed.trailer.entries.filter { it.first.raw in setOf("/Root", "/Info") }
        val trailer = PdfVal.Dict(
            listOf(PdfVal.Name("/Size") to PdfVal.Raw(size.toString())) + keep +
                listOf(
                    PdfVal.Name("/ID") to PdfVal.Raw("[<$idHex><$idHex>]"),
                    PdfVal.Name("/Encrypt") to PdfVal.Raw("$encryptNumber 0 R"),
                ),
        )
        out.write("trailer\n${trailer.serialize()}\nstartxref\n$xref\n%%EOF\n".latin1())
        fileKey.fill(0)
        return out.toByteArray()
    }

    /** SASLprep is approximated by NFKC (the same for Hangul syllables); UTF-8, at most 127 bytes. */
    internal fun passwordBytes(password: CharArray): ByteArray {
        val text = Normalizer.normalize(String(password), Normalizer.Form.NFKC)
        val bytes = text.toByteArray(Charsets.UTF_8)
        return if (bytes.size > 127) bytes.copyOf(127) else bytes
    }

    /** Algorithm 2.B (R6 hash). */
    internal fun hash(password: ByteArray, salt: ByteArray, userKey: ByteArray): ByteArray {
        var k = MessageDigest.getInstance("SHA-256").digest(password + salt + userKey)
        var round = 0
        while (true) {
            val block = password + k + userKey
            val k1 = ByteArray(block.size * 64).also { for (i in 0 until 64) block.copyInto(it, i * block.size) }
            val aes = Cipher.getInstance("AES/CBC/NoPadding")
            aes.init(Cipher.ENCRYPT_MODE, SecretKeySpec(k.copyOfRange(0, 16), "AES"), IvParameterSpec(k.copyOfRange(16, 32)))
            val e = aes.doFinal(k1)
            val mod = e.copyOfRange(0, 16).sumOf { it.toInt() and 0xFF } % 3
            k = MessageDigest.getInstance(
                when (mod) {
                    0 -> "SHA-256"
                    1 -> "SHA-384"
                    else -> "SHA-512"
                },
            ).digest(e)
            round++
            if (round >= 64 && (e.last().toInt() and 0xFF) <= round - 32) break
        }
        return k.copyOf(32)
    }

    internal fun aesCbcNoPad(key: ByteArray, data: ByteArray, mode: Int = Cipher.ENCRYPT_MODE): ByteArray {
        val c = Cipher.getInstance("AES/CBC/NoPadding")
        c.init(mode, SecretKeySpec(key, "AES"), IvParameterSpec(ByteArray(16)))
        return c.doFinal(data)
    }

    /** `U`, `UE`, `O`, `OE`, `Perms` for one file key. */
    internal class Keys(val u: ByteArray, val ue: ByteArray, val o: ByteArray, val oe: ByteArray, val perms: ByteArray) {
        fun dictionary(): String =
            "<< /Filter /Standard /V 5 /R 6 /Length 256 /CF << /StdCF << /AuthEvent /DocOpen /CFM /AESV3 /Length 32 >> >> " +
                "/StmF /StdCF /StrF /StdCF /O <${hex(o)}> /U <${hex(u)}> /OE <${hex(oe)}> /UE <${hex(ue)}> /Perms <${hex(perms)}> " +
                "/P $PERMISSIONS /EncryptMetadata true >>"

        companion object {
            fun make(user: ByteArray, owner: ByteArray, fileKey: ByteArray, random: SecureRandom): Keys {
                val uValidation = random.bytes(8)
                val uKeySalt = random.bytes(8)
                val u = hash(user, uValidation, ByteArray(0)) + uValidation + uKeySalt
                val ue = aesCbcNoPad(hash(user, uKeySalt, ByteArray(0)), fileKey)
                val oValidation = random.bytes(8)
                val oKeySalt = random.bytes(8)
                val o = hash(owner, oValidation, u) + oValidation + oKeySalt
                val oe = aesCbcNoPad(hash(owner, oKeySalt, u), fileKey)
                val p = PERMISSIONS
                val plain = byteArrayOf(
                    p.toByte(), (p shr 8).toByte(), (p shr 16).toByte(), (p shr 24).toByte(),
                    -1, -1, -1, -1, 'T'.code.toByte(), 'a'.code.toByte(), 'd'.code.toByte(), 'b'.code.toByte(),
                ) + random.bytes(4)
                val ecb = Cipher.getInstance("AES/ECB/NoPadding")
                ecb.init(Cipher.ENCRYPT_MODE, SecretKeySpec(fileKey, "AES"))
                return Keys(u, ue, o, oe, ecb.doFinal(plain))
            }
        }
    }

    /** Encrypts strings and stream data with the file key (AESV3: no per-object key). */
    private class Sealer(private val key: ByteArray, private val random: SecureRandom) {
        fun bytes(data: ByteArray): ByteArray {
            val iv = random.bytes(16)
            val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
            c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            return iv + c.doFinal(data)
        }

        fun value(v: PdfVal): PdfVal = when (v) {
            is PdfVal.Str -> PdfVal.Str(bytes(v.bytes))
            is PdfVal.Arr -> PdfVal.Arr(v.items.map(::value))
            is PdfVal.Dict -> PdfVal.Dict(v.entries.map { it.first to value(it.second) })
            else -> v
        }
    }

    private fun SecureRandom.bytes(n: Int) = ByteArray(n).also { nextBytes(it) }

    private fun String.latin1() = toByteArray(Charsets.ISO_8859_1)

    internal fun hex(bytes: ByteArray): String = bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
}

/** A parsed PDF value; [serialize] writes it back (strings as hex). */
internal sealed interface PdfVal {
    fun serialize(): String

    /** A name, number, keyword or reference kept as written. */
    data class Raw(val raw: String) : PdfVal {
        override fun serialize() = raw
    }

    data class Name(val raw: String) : PdfVal {
        override fun serialize() = raw
    }

    class Str(val bytes: ByteArray) : PdfVal {
        override fun serialize() = "<" + PdfEncryption.hex(bytes) + ">"
    }

    data class Arr(val items: List<PdfVal>) : PdfVal {
        override fun serialize() = items.joinToString(" ", "[", "]") { it.serialize() }
    }

    data class Dict(val entries: List<Pair<Name, PdfVal>>) : PdfVal {
        fun entry(key: String): PdfVal? = entries.firstOrNull { it.first.raw == key }?.second

        override fun serialize() = entries.joinToString(" ", "<< ", " >>") { "${it.first.raw} ${it.second.serialize()}" }
    }
}

/** One indirect object; [stream] holds the stream's raw (still filtered) bytes. */
internal class PdfObject(val number: Int, val generation: Int, val value: PdfVal, val stream: ByteArray?)

/** A classic PDF: its objects in file order and the trailer dictionary. */
internal class PdfFile(val objects: List<PdfObject>, val trailer: PdfVal.Dict) {
    companion object {
        fun parse(bytes: ByteArray): PdfFile = PdfParser(bytes).file()
    }
}

/** Just enough of ISO 32000 §7.3 to read and rewrite a `PdfDocument` file. */
private class PdfParser(private val b: ByteArray) {
    private var pos = 0

    fun file(): PdfFile {
        if (!startsWith("%PDF-")) throw PdfEncryptionException("not a PDF")
        val objects = LinkedHashMap<Int, PdfObject>()
        var trailer: PdfVal.Dict? = null
        while (true) {
            skipWs()
            if (pos >= b.size) break
            when {
                startsWith("xref") -> {
                    val at = indexOf("trailer", pos) ?: throw PdfEncryptionException("no trailer")
                    pos = at + "trailer".length
                    val dict = value() as? PdfVal.Dict ?: throw PdfEncryptionException("bad trailer")
                    if (dict.entry("/Prev") != null) throw PdfEncryptionException("incremental updates")
                    trailer = dict
                }
                startsWith("startxref") -> {
                    pos += "startxref".length
                    skipWs()
                    token()
                }
                isDigit(b[pos]) -> {
                    val obj = indirect()
                    objects[obj.number] = obj
                }
                else -> throw PdfEncryptionException("unexpected content at $pos")
            }
        }
        return PdfFile(objects.values.toList(), trailer ?: throw PdfEncryptionException("no trailer"))
    }

    private fun indirect(): PdfObject {
        val number = token().toIntOrNull() ?: throw PdfEncryptionException("bad object number at $pos")
        skipWs()
        val generation = token().toIntOrNull() ?: throw PdfEncryptionException("bad generation at $pos")
        skipWs()
        if (token() != "obj") throw PdfEncryptionException("obj expected at $pos")
        val value = value()
        skipWs()
        var stream: ByteArray? = null
        if (startsWith("stream")) {
            pos += "stream".length
            if (pos < b.size && b[pos] == '\r'.code.toByte()) pos++
            if (pos < b.size && b[pos] == '\n'.code.toByte()) pos++
            val start = pos
            val declared = ((value as? PdfVal.Dict)?.entry("/Length") as? PdfVal.Raw)?.raw?.toIntOrNull()
            val end = if (declared != null && start + declared <= b.size && endstreamAt(start + declared)) {
                start + declared
            } else {
                val at = indexOf("endstream", start) ?: throw PdfEncryptionException("no endstream")
                var e = at
                if (e > start && b[e - 1] == '\n'.code.toByte()) e--
                if (e > start && b[e - 1] == '\r'.code.toByte()) e--
                e
            }
            stream = b.copyOfRange(start, end)
            pos = (indexOf("endstream", end) ?: throw PdfEncryptionException("no endstream")) + "endstream".length
            skipWs()
        }
        if (token() != "endobj") throw PdfEncryptionException("endobj expected at $pos")
        return PdfObject(number, generation, value, stream)
    }

    private fun endstreamAt(at: Int): Boolean {
        var p = at
        while (p < b.size && isWs(b[p])) p++
        return matches(p, "endstream")
    }

    fun value(): PdfVal {
        skipWs()
        if (pos >= b.size) throw PdfEncryptionException("unexpected end")
        val c = b[pos].toInt().toChar()
        return when {
            c == '/' -> PdfVal.Name(name())
            c == '(' -> PdfVal.Str(literal())
            c == '<' && pos + 1 < b.size && b[pos + 1] == '<'.code.toByte() -> dict()
            c == '<' -> PdfVal.Str(hexString())
            c == '[' -> array()
            else -> {
                val t = token()
                if (t.isEmpty()) throw PdfEncryptionException("bad token at $pos")
                if (t.all { it.isDigit() }) reference(t) ?: PdfVal.Raw(t) else PdfVal.Raw(t)
            }
        }
    }

    private fun reference(first: String): PdfVal? {
        val save = pos
        skipWs()
        val second = token()
        if (second.isNotEmpty() && second.all { it.isDigit() }) {
            skipWs()
            if (pos < b.size && b[pos] == 'R'.code.toByte() && (pos + 1 >= b.size || isDelimOrWs(b[pos + 1]))) {
                pos++
                return PdfVal.Raw("$first $second R")
            }
        }
        pos = save
        return null
    }

    private fun dict(): PdfVal.Dict {
        pos += 2
        val entries = ArrayList<Pair<PdfVal.Name, PdfVal>>()
        while (true) {
            skipWs()
            if (startsWith(">>")) {
                pos += 2
                return PdfVal.Dict(entries)
            }
            val key = value() as? PdfVal.Name ?: throw PdfEncryptionException("dictionary key expected at $pos")
            entries += key to value()
        }
    }

    private fun array(): PdfVal.Arr {
        pos++
        val items = ArrayList<PdfVal>()
        while (true) {
            skipWs()
            if (pos >= b.size) throw PdfEncryptionException("unclosed array")
            if (b[pos] == ']'.code.toByte()) {
                pos++
                return PdfVal.Arr(items)
            }
            items += value()
        }
    }

    private fun name(): String {
        val start = pos
        pos++
        while (pos < b.size && !isDelimOrWs(b[pos])) pos++
        return String(b, start, pos - start, Charsets.ISO_8859_1)
    }

    private fun literal(): ByteArray {
        pos++
        val out = ByteArrayOutputStream()
        var depth = 1
        while (pos < b.size) {
            val c = b[pos++].toInt() and 0xFF
            when (c) {
                '\\'.code -> {
                    if (pos >= b.size) break
                    val e = b[pos++].toInt() and 0xFF
                    when (e) {
                        'n'.code -> out.write(10)
                        'r'.code -> out.write(13)
                        't'.code -> out.write(9)
                        'b'.code -> out.write(8)
                        'f'.code -> out.write(12)
                        '\r'.code -> if (pos < b.size && b[pos] == '\n'.code.toByte()) pos++
                        '\n'.code -> Unit
                        in '0'.code..'7'.code -> {
                            var v = e - '0'.code
                            repeat(2) {
                                if (pos < b.size && (b[pos].toInt() and 0xFF) in '0'.code..'7'.code) v = v * 8 + (b[pos++] - '0'.code.toByte())
                            }
                            out.write(v and 0xFF)
                        }
                        else -> out.write(e)
                    }
                }
                '('.code -> {
                    depth++
                    out.write(c)
                }
                ')'.code -> {
                    depth--
                    if (depth == 0) return out.toByteArray()
                    out.write(c)
                }
                else -> out.write(c)
            }
        }
        throw PdfEncryptionException("unclosed string")
    }

    private fun hexString(): ByteArray {
        pos++
        val digits = StringBuilder()
        while (pos < b.size && b[pos] != '>'.code.toByte()) {
            val ch = b[pos++].toInt().toChar()
            if (!ch.isWhitespace()) digits.append(ch)
        }
        pos++
        if (digits.length % 2 == 1) digits.append('0')
        return ByteArray(digits.length / 2) { digits.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }

    private fun token(): String {
        val start = pos
        while (pos < b.size && !isDelimOrWs(b[pos])) pos++
        return String(b, start, pos - start, Charsets.ISO_8859_1)
    }

    private fun skipWs() {
        while (pos < b.size) {
            if (isWs(b[pos])) {
                pos++
            } else if (b[pos] == '%'.code.toByte()) {
                while (pos < b.size && b[pos] != '\n'.code.toByte() && b[pos] != '\r'.code.toByte()) pos++
            } else {
                return
            }
        }
    }

    private fun startsWith(s: String) = matches(pos, s)

    private fun matches(at: Int, s: String): Boolean {
        if (at + s.length > b.size) return false
        for (i in s.indices) if (b[at + i] != s[i].code.toByte()) return false
        return true
    }

    private fun indexOf(s: String, from: Int): Int? {
        var i = from
        while (i + s.length <= b.size) {
            if (matches(i, s)) return i
            i++
        }
        return null
    }

    private fun isDigit(x: Byte) = x in '0'.code.toByte()..'9'.code.toByte()

    private fun isWs(x: Byte) = x == 0.toByte() || x == 9.toByte() || x == 10.toByte() || x == 12.toByte() || x == 13.toByte() || x == 32.toByte()

    private fun isDelimOrWs(x: Byte) = isWs(x) || "()<>[]{}/%".indexOf(x.toInt().toChar()) >= 0
}
