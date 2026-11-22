package io.github.lenticualrstone.civilight.feature.library.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.icon.ICON_STROKE_UNITS

/**
 * Outline icons the `LibMenu` board draws that DS-12 lacks (requested for core:designsystem). Built
 * exactly like DS-12 icons: 24-unit grid, stroke 1.6, round caps and joins.
 */
internal object LibraryIcons {
    /** `이름 바꾸기` (pencil). */
    val Pencil: ImageVector by lazy { build("Pencil", "M4 20h4l10-10-4-4L4 16z") }

    /** `수위 등급` (shield). */
    val Shield: ImageVector by lazy { build("Shield", "M12 3l8 4v5c0 4.5-3.4 8.2-8 9-4.6-.8-8-4.5-8-9V7z") }

    /** `백업 · 내보내기` (download tray). */
    val Download: ImageVector by lazy { build("Download", "M12 4v11M7 10l5 5 5-5M5 20h14") }

    private fun build(name: String, d: String): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathParser().parsePathString(d).toNodes(),
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = ICON_STROKE_UNITS,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()
}
