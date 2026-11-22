package io.github.lenticualrstone.civilight.feature.recorder

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.SheetKey
import kotlinx.serialization.Serializable

// Feature-local sheets of the 기록함 (saved with the inbox screen, shell.md §5). The shared ones
// (`TextMemoSheet`, `RecorderLinkPickerSheet`) live in core:navigation.

/** The record `⋯` menu (LRC-29). */
@Serializable
data class RecordMenuSheet(val recordKey: Key) : SheetKey

/**
 * A 넣기 sheet (LRC-32): the project step, then the kind's own rows. [kind] is a `domain:recorder`
 * `PutKind` name: `MANUSCRIPT` (`원고에 넣기`, LRC-33), `CARD` (LRC-35) and `PAGE` (LRC-36) run;
 * LRC-37 / 38 add theirs. [projectKey] = the chosen
 * destination, null while the project menu is open.
 */
@Serializable
data class PutSheet(val recordKey: Key, val kind: String, val projectKey: Key? = null) : SheetKey

/** `씬 고르기 ›` of `원고에 넣기`: the scenes of [projectKey]; a pick inserts at its end. */
@Serializable
data class PutScenePickSheet(val recordKey: Key, val projectKey: Key) : SheetKey

/** The `프로젝트 ▾` menu of the inbox filter row (LRC-27). */
@Serializable
data object ProjectFilterSheet : SheetKey

/**
 * The link picker over a saved record (LRC-20): `+ 연결` on an inbox row or the record screen adds a
 * link. From a deleted card's sheet (LRC-23) it instead replaces [replaceLinkKeys] (`칩 다시 고르기`),
 * or re-points the `@` runs of the target [relinkProjectKey] · [relinkKind] · [relinkKey]
 * (`다른 항목에 잇기`; kind = `RecorderTargetKind` name).
 */
@Serializable
data class RecordLinkSheet(
    val recordKey: Key,
    val replaceLinkKeys: List<Key> = emptyList(),
    val relinkProjectKey: Key? = null,
    val relinkKind: String? = null,
    val relinkKey: Key? = null,
) : SheetKey

/** The card of a deleted target's chip on the record screen (LRC-23): `지운 인물 · 노아` and its buttons. */
@Serializable
data class DeletedTargetSheet(val recordKey: Key, val linkKey: Key) : SheetKey
