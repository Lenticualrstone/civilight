package io.github.lenticualrstone.civilight.feature.recorder.inbox

import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec
import io.github.lenticualrstone.civilight.domain.recorder.inbox.EmptyInbox

/**
 * (0.2.0) `EmptyLr` as drawn (LRC-30, LRC-48, icon list B24):
 * - no sub line: the 0.1.0 sub names the command-bar mics, the widget and the tile, which 0.2.0 does not
 *   offer; its new copy is `TODO_COPY_LRC_EMPTY_SUB` and nothing is drawn until it is given;
 * - two b buttons, icon + text: `말로 기록` (mic) and `글로 쓰기` (write; `글로` becomes `글로 쓰기`).
 */
data class EmptyInboxView(
    val chip: String,
    val title: String,
    val sub: String?,
    val voice: IconButtonSpec,
    val text: IconButtonSpec,
) {
    companion object {
        fun of(empty: EmptyInbox): EmptyInboxView = EmptyInboxView(
            chip = empty.chip,
            title = empty.title,
            sub = null,
            voice = IconButtonSpec.INBOX_RECORD_VOICE,
            text = IconButtonSpec.INBOX_RECORD_TEXT,
        )
    }
}
