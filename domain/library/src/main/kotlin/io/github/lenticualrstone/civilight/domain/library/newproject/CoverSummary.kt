package io.github.lenticualrstone.civilight.domain.library.newproject

import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.asset.Presets
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * The project cover in words (U-08 LIB-81 · LIB-82 · LIB-84). The cover is the one `ImageUse`
 * `(project, <project key>, projectCover)` (LIB-80, C-10); there is no project column.
 *
 * The source word is the preset's name (`등불`), `사진` for a blob picked with the system photo picker
 * (`source = gallery`), and `파일` for every other blob. It never names a gallery item, so a hidden
 * item's title cannot leak (LIB-84, LIB-89).
 */
object CoverSummary {
    const val TITLE = "표지 이미지"
    const val WORD_PHOTO = "사진"
    const val WORD_FILE = "파일"
    const val NOW = "지금"
    const val NONE = "없음"
    const val NONE_SUB = "없음 · 첫 글자 카드"

    /** The cover row of [doc], or null without one. */
    fun use(doc: ProjectDoc): ImageUse? = doc.index.imageUse(ImageOwnerKind.PROJECT, doc.key, ImageSlot.PROJECT_COVER)

    /**
     * The source word of the cover that is drawn, or null when the initial-letter card is drawn: no row,
     * an unknown preset, or an original whose file is not on this device ([fileExists], LIB-88).
     */
    fun word(doc: ProjectDoc, fileExists: (String) -> Boolean = { true }): String? {
        val use = use(doc) ?: return null
        return wordOf(doc, use, fileExists)
    }

    /** [word] for a given [use] of [doc]. */
    fun wordOf(doc: ProjectDoc, use: ImageUse, fileExists: (String) -> Boolean = { true }): String? {
        use.presetId?.let { return Presets.nameOf(it) }
        val hash = use.blobHash ?: return null
        if (!fileExists(hash)) return null
        val source = doc.assets.blobs.firstOrNull { it.hash == hash }?.source?.known
        return if (source == BlobSource.GALLERY) WORD_PHOTO else WORD_FILE
    }

    /** ⋯ row sub (LIB-81): `지금 <word>`, or `없음 · 첫 글자 카드`. */
    fun menuSub(doc: ProjectDoc, fileExists: (String) -> Boolean = { true }): String =
        word(doc, fileExists)?.let { "$NOW $it" } ?: NONE_SUB

    /** 프로젝트 설정 row value (LIB-82): `<word>`, or `없음`; the row draws its own `›`. */
    fun settingsValue(doc: ProjectDoc, fileExists: (String) -> Boolean = { true }): String = word(doc, fileExists) ?: NONE
}
