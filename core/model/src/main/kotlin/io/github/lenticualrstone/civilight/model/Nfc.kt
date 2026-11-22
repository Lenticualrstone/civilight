package io.github.lenticualrstone.civilight.model

import java.text.Normalizer

/** NFC normalisation (§1.4): all text is NFC on write and on read. */
object Nfc {
    fun normalize(text: String): String =
        if (Normalizer.isNormalized(text, Normalizer.Form.NFC)) text
        else Normalizer.normalize(text, Normalizer.Form.NFC)
}

/** This string in NFC. */
fun String.nfc(): String = Nfc.normalize(this)
