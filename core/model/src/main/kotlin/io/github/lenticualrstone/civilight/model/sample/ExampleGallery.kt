package io.github.lenticualrstone.civilight.model.sample

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.cast.Tag
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.gallery.GalleryFile
import io.github.lenticualrstone.civilight.model.gallery.GalleryItem
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.DEVICE
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.at
import java.security.MessageDigest
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/**
 * Fixture G0 of `55-gallery.md` §4, opt-in: [addTo] puts 리엔's gallery (ten items, two of them
 * hidden), the three gallery tags, the blob rows and 리엔's portrait (same source as item 3) on top
 * of the example project. [ExampleProject.build] itself stays unchanged, so tests and the format
 * regression files that assert on it are not affected.
 *
 * Visible numbering 1…8 skips H1 and H2. Blob bytes are tiny stand-ins ([bytesOf]) whose hashes are
 * real SHA-256 values, so package tests can check `gallery/files/<hash>.<ext>`.
 */
object ExampleGallery {
    private const val TIME = K.TIME

    fun item(n: Int): Key = TIME + "GXMG" + n.toString().padStart(12, '0')

    /** Items 1…8 in visible order, H1 and H2 the hidden ones. */
    val ITEM_1 = item(1)
    val ITEM_2 = item(2)
    val ITEM_3 = item(3)
    val ITEM_4 = item(4)
    val ITEM_H1 = item(91)
    val ITEM_5 = item(5)
    val ITEM_6 = item(6)
    val ITEM_7 = item(7)
    val ITEM_8 = item(8)
    val ITEM_H2 = item(92)

    /** Stored order of 리엔's gallery. */
    val STORED: List<Key> = listOf(ITEM_1, ITEM_2, ITEM_3, ITEM_4, ITEM_H1, ITEM_5, ITEM_6, ITEM_7, ITEM_8, ITEM_H2)

    val TAG_ILLUSTRATION = K.tag(5)
    val TAG_SETTING = K.tag(6)
    val TAG_PROP = K.tag(7)

    const val PORTRAIT_KEY = TIME + "PCTR000000000002"

    /** The sentence T-GAL-19 searches for. */
    const val SOOT_SENTENCE = "손등의 그을음은 마지막까지 불을 지킨 흔적."

    /** Stand-in bytes of item [n]'s original (91, 92 = H1, H2). */
    fun bytesOf(n: Int): ByteArray = "g0-original-$n".toByteArray(Charsets.UTF_8)

    fun hashOf(n: Int): String = MessageDigest.getInstance("SHA-256").digest(bytesOf(n)).joinToString("") { "%02x".format(it) }

    /** Blob bytes by hash for package tests (`BlobReader.of`). */
    val blobBytes: Map<String, ByteArray> get() = NUMBERS.associate { hashOf(it) to bytesOf(it) }

    private val NUMBERS = listOf(1, 2, 3, 4, 91, 5, 6, 7, 8, 92)

    fun blob(n: Int): Blob = Blob(
        hash = hashOf(n), fileExt = "webp", mime = "image/webp", bytes = bytesOf(n).size.toLong(),
        width = 1200, height = 1600, source = BlobSource.GALLERY.open(), importedAt = at("2026-11-20T09:00:00Z"),
    )

    /** The example project with G0 on top. */
    fun addTo(doc: ProjectDoc): ProjectDoc {
        val lastTag = doc.project.tags.maxOfOrNull { it.order }
        val tagOrders = OrderKeys.between(lastTag, null, 3)
        val tags = listOf(
            Tag(TAG_ILLUSTRATION, "삽화", tagOrders[0], at("2026-11-20T09:00:00Z"), DEVICE),
            Tag(TAG_SETTING, "설정화", tagOrders[1], at("2026-11-20T09:00:00Z"), DEVICE),
            Tag(TAG_PROP, "소품", tagOrders[2], at("2026-11-20T09:00:00Z"), DEVICE),
        )
        val orders = OrderKeys.evenlySpaced(NUMBERS.size)
        fun g(i: Int, n: Int, title: String, tag: Key?, scene: Key? = null, holo: Boolean = false, hidden: Boolean = false, caption: String? = null) =
            GalleryItem(
                key = item(n), ownerKind = GalleryOwnerKind.CHARACTER.open(), ownerKey = K.RIEN, source = hashOf(n), order = orders[i],
                title = title, caption = caption?.let { text -> doc(9000 + n) { text.split("\n").forEach { paragraph(it) } } },
                tagKeys = listOfNotNull(tag), sceneKey = scene, holo = holo, hidden = hidden,
                createdAt = at("2026-11-20T09:00:00Z").plus(i * 60_000L), updatedAt = at("2026-11-20T09:00:00Z"), updatedBy = DEVICE,
            )
        val items = listOf(
            g(0, 1, "결사에 들어온 날", TAG_ILLUSTRATION, K.SCENE_1_3_2),
            g(1, 2, "외출복 설정", TAG_SETTING),
            g(
                2, 3, "등불 앞의 리엔", TAG_ILLUSTRATION, K.SCENE_1_7_3, holo = true,
                caption = "정전 뒤 첫 밤, 리엔은 등불을 들고 계단 끝에 섰다.\n" +
                    "바람이 불어도 불꽃은 꺼지지 않았다. 세라는 그 모습을 오래 기억했다. " +
                    SOOT_SENTENCE + " 그날부터 하층 구역은 그녀를 등불지기라 불렀다.",
            ),
            g(3, 4, "", null),
            g(4, 91, "숨긴 스케치", TAG_ILLUSTRATION, hidden = true),
            g(5, 5, "대정전의 밤", TAG_ILLUSTRATION, K.SCENE_1_4_2),
            g(6, 6, "등불 고리", TAG_PROP),
            g(7, 7, "하층 구역 계단", TAG_ILLUSTRATION, K.SCENE_1_6_1),
            g(8, 8, "겨울 옷 시안", TAG_SETTING),
            g(9, 92, "숨긴 메모", TAG_SETTING, hidden = true),
        )
        val portrait = ImageUse(
            key = PORTRAIT_KEY, ownerKind = ImageOwnerKind.CHARACTER.open(), ownerKey = K.RIEN,
            slot = ImageSlot.CHARACTER_PORTRAIT.open(), source = hashOf(3), holo = true,
            updatedAt = at("2026-11-20T09:00:00Z"), updatedBy = DEVICE,
        )
        return doc.copy(
            project = doc.project.copy(tags = doc.project.tags + tags),
            assets = doc.assets.copy(
                blobs = doc.assets.blobs + NUMBERS.map(::blob),
                imageUses = doc.assets.imageUses.filterNot { it.key == PORTRAIT_KEY } + portrait,
            ),
            gallery = doc.gallery + (K.RIEN to GalleryFile(GalleryOwnerKind.CHARACTER.open(), K.RIEN, items)),
        )
    }

    /** The example project with G0 ([ExampleProject.build] + [addTo]). */
    fun build(): ProjectDoc = addTo(ExampleProject.build())
}
