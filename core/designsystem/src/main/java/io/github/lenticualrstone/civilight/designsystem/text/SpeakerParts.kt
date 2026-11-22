package io.github.lenticualrstone.civilight.designsystem.text

import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole
import io.github.lenticualrstone.civilight.designsystem.theme.TypeRole

/** A marker shown on a speaker line, solid or dotted. */
data class SpeakerMarker(val kind: MarkerKind, val line: MarkerLine = MarkerLine.SOLID)

/** Underline of a speaker-line part. */
enum class PartUnderline { NONE, DOTTED }

/**
 * One piece of the speaker line (DS-42): text in a role and colour, or a [marker]. [alignEnd] parts sit at
 * the far right of the line.
 */
data class SpeakerPart(
    val text: String,
    val role: TypeRole,
    val color: ColorRole,
    val alignEnd: Boolean = false,
    val underline: PartUnderline = PartUnderline.NONE,
    val marker: SpeakerMarker? = null,
)

/**
 * DS-42 / T-DS-19 / T-DS-13. The display name first (`ink` when the block is selected or typed, else
 * `ink2`); when it differs from the real name, the real name follows in `ink3` together with the block
 * number when selected (`하르트 · #043`). A speaker that is deleted gets a dotted underline and a
 * following `◇` (`노아 ◇`). Markers go to the far right. No expression tags.
 */
fun speakerLabelParts(
    displayName: String,
    realName: String?,
    selected: Boolean,
    blockNo: String?,
    markers: List<SpeakerMarker>,
    deleted: Boolean = false,
    typing: Boolean = false,
): List<SpeakerPart> {
    val parts = mutableListOf<SpeakerPart>()
    val nameColor = if (selected || typing) ColorRole.INK else ColorRole.INK2
    parts += SpeakerPart(
        text = displayName,
        role = TypeRole.SPEAKER_NAME,
        color = nameColor,
        underline = if (deleted) PartUnderline.DOTTED else PartUnderline.NONE,
    )
    if (deleted) {
        parts += SpeakerPart(MarkerKind.NOTICE.symbol.toString(), TypeRole.SPEAKER, ColorRole.INK3, marker = SpeakerMarker(MarkerKind.NOTICE))
    }
    val showReal = realName != null && realName != displayName
    val number = if (selected && !blockNo.isNullOrEmpty()) blockNo else null
    val rest = listOfNotNull(if (showReal) realName else null, number)
    if (rest.isNotEmpty()) {
        val text = if (showReal) rest.joinToString(" · ") else "· ${rest.single()}"
        parts += SpeakerPart(text, TypeRole.SPEAKER, ColorRole.INK3)
    }
    markers.forEach { m ->
        val color = when (m.kind) {
            MarkerKind.CHANGE -> ColorRole.ACC
            MarkerKind.WARNING -> ColorRole.WARN
            else -> ColorRole.INK3
        }
        parts += SpeakerPart(m.kind.symbol.toString(), TypeRole.SPEAKER, color, alignEnd = true, marker = m)
    }
    return parts
}
