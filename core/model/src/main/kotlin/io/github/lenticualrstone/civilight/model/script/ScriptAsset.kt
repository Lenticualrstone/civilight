@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.script

import io.github.lenticualrstone.civilight.model.Entity
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.Timestamp
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A direction asset (`에셋`: 배경 · BGM · 효과음 · CG), in `assets/index.json › scriptAssets` (§2.3).
 * Use counts, first-use episode and `안 쓰는 것` are derived.
 */
@Serializable
data class ScriptAsset(
    override val key: Key,
    val kind: OpenEnum<ScriptAssetKind>,
    val name: String,
    /** Engine file name (`bg_lower_dawn`). */
    val fileName: String,
    /** → Blob: image for background/CG, audio for BGM/SFX. */
    val fileHash: String? = null,
    val durationMs: Long? = null,
    @EncodeDefault val loop: Boolean = false,
    /** Background ↔ place link (bulk suggestions). */
    val placeKey: Key? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity

enum class ScriptAssetKind { BACKGROUND, BGM, SFX, CG }
