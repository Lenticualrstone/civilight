package io.github.lenticualrstone.civilight.domain.checks.sources

import io.github.lenticualrstone.civilight.domain.checks.CheckCopy
import io.github.lenticualrstone.civilight.domain.checks.CheckResult
import io.github.lenticualrstone.civilight.domain.checks.FixAction
import io.github.lenticualrstone.civilight.domain.checks.FixKind
import io.github.lenticualrstone.civilight.domain.trash.DeletedItemRow
import io.github.lenticualrstone.civilight.domain.trash.DeletedItems
import io.github.lenticualrstone.civilight.domain.trash.Fixes
import io.github.lenticualrstone.civilight.domain.trash.HidingLocks
import io.github.lenticualrstone.civilight.domain.trash.TrashContext
import io.github.lenticualrstone.civilight.model.checks.CheckType

/**
 * Row 9 (`◇ 지운 인물 · 노아`, `영구 삭제 · 노아`; CST-42) from the trash area: one row per deleted
 * character · place · term · faction that live data still references. This is the only source of
 * deleted-item rows (the world and cast areas' own deleted rows would count twice).
 */
internal object TrashSource {
    fun deletedItems(scope: CheckScope): List<CheckResult> {
        val inputs = scope.inputs
        val ctx = TrashContext(now = inputs.now, zone = inputs.zone, newKey = { "" })
        val rows = DeletedItems.rows(scope.doc, ctx, inputs.recorder, HidingLocks(itemsLocked = scope.hiding.locked))
        return rows.map { row -> result(row) }
    }

    private fun result(row: DeletedItemRow): CheckResult {
        val subjects = listOf(row.item.key)
        return CheckResult(
            checkType = CheckType.DELETED_ITEM,
            subjectKeys = subjects,
            fingerprint = Fixes.deletedItemFingerprint(subjects),
            title = row.label,
            location = row.location,
            body = row.body,
            actions = row.actions.map { label -> FixAction(label, kindOf(label), row.item.key) },
            lockedHidden = row.masked,
        )
    }

    private fun kindOf(label: String): FixKind = when (label) {
        CheckCopy.RESTORE -> FixKind.RESTORE
        CheckCopy.KEEP -> FixKind.ACKNOWLEDGE
        else -> FixKind.BULK_REPLACE
    }
}
