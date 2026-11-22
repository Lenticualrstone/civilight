package io.github.lenticualrstone.civilight.feature.cast.common

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.trash.DocChange
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** The compound-action context (`domain:actions`) of a cast edit context: same clock, zone, device and key source. */
fun EditContext.toAction(): ActionContext = ActionContext(now = now, zone = zone, device = device, newKey = newKey)

/** The action context of an action made now. */
fun FeatureDeps.actionContext(): ActionContext = editContext().toAction()

/** A document change with what the screen shows afterwards (a toast, a report). */
data class Step<out R>(val doc: ProjectDoc, val value: R)

/**
 * Writes that must outlive the screen that starts them: a record that leaves after its own delete,
 * a toast's `되돌리기` pressed after the record is gone. They run on the app scope with their own
 * project session (as feature:story's scene delete does), never on a view model's scope.
 */
class CastWrites(private val deps: FeatureDeps, private val projectKey: Key) {
    /**
     * One undo step named [label]. [compute] returns null to change nothing. [then] runs after the
     * write on the main thread with the step's value. Writes with the same [mergeKey] in quick
     * succession form one undo step (typing). A read-only project changes nothing.
     */
    fun <R> launch(
        label: String,
        compute: (ProjectDoc, ActionContext) -> Step<R>?,
        mergeKey: String? = null,
        then: (R) -> Unit = {},
    ): Job = deps.appScope.launch {
        val store = deps.projects.acquire(projectKey)
        try {
            if (store.readOnly) return@launch
            val ctx = deps.actionContext()
            var out: Step<R>? = null
            store.update(label, mergeKey) { doc -> compute(doc, ctx)?.also { out = it }?.doc ?: doc }
            out?.let { then(it.value) }
        } finally {
            deps.projects.release(projectKey)
        }
    }

    /** `되돌리기` of exactly [change] (row-level revert), as its own undo step named [label]. */
    fun revert(label: String, change: DocChange): Job = launch(label, { doc, _ -> Step(change.revert(doc), Unit) })
}
