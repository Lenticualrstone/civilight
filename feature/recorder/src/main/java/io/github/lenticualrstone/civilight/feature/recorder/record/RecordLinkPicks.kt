package io.github.lenticualrstone.civilight.feature.recorder.record

import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.feature.recorder.RecordLinkSheet
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind

/** What a pick in the record's link picker does (LRC-20, LRC-23). */
sealed interface LinkPickMode {
    /** `+ 연결`: one more link of origin `직접`. */
    data object Add : LinkPickMode

    /** A chip tap or `칩 다시 고르기`: these links go, the first one's place takes the pick. */
    data class Replace(val linkKeys: List<Key>) : LinkPickMode

    /** `다른 항목에 잇기` on an `@` chip: the runs to [from] point at the pick. */
    data class Relink(val from: TargetRef) : LinkPickMode
}

/** The picker sheet key ↔ its mode (the key holds only strings, so it saves with the sheet stack). */
object RecordLinkPicks {
    fun mode(sheet: RecordLinkSheet): LinkPickMode {
        val kind = sheet.relinkKind?.let { k -> runCatching { RecorderTargetKind.valueOf(k) }.getOrNull() }
        val project = sheet.relinkProjectKey
        val key = sheet.relinkKey
        return when {
            kind != null && project != null && key != null -> LinkPickMode.Relink(TargetRef(project, kind, key))
            sheet.replaceLinkKeys.isNotEmpty() -> LinkPickMode.Replace(sheet.replaceLinkKeys)
            else -> LinkPickMode.Add
        }
    }

    /** The picker a deleted card's button asks for. */
    fun sheetFor(recordKey: Key, outcome: CardOutcome.OpenPicker): RecordLinkSheet {
        val from = outcome.relinkFrom
        return if (from != null) {
            RecordLinkSheet(recordKey, relinkProjectKey = from.projectKey, relinkKind = from.kind.name, relinkKey = from.key)
        } else {
            RecordLinkSheet(recordKey, replaceLinkKeys = outcome.replaceLinkKeys)
        }
    }

    suspend fun apply(c: RecordController, mode: LinkPickMode, target: TargetRef) = when (mode) {
        LinkPickMode.Add -> c.addLink(target)
        is LinkPickMode.Replace -> c.replaceLinks(mode.linkKeys, target)
        is LinkPickMode.Relink -> c.relinkRuns(mode.from, target)
    }
}
