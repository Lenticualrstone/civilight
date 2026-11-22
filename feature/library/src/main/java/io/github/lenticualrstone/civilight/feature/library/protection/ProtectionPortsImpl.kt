package io.github.lenticualrstone.civilight.feature.library.protection

import io.github.lenticualrstone.civilight.feature.library.common.ProjectWriter
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.store.app.AppStateStore
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.security.AuthCopy
import io.github.lenticualrstone.civilight.ui.security.AuthRequest
import io.github.lenticualrstone.civilight.ui.security.AuthResult
import io.github.lenticualrstone.civilight.ui.security.BiometricGate

/** [ProtectionPorts] over the shared stores and the system prompt. */
internal class AppProtectionPorts(
    private val writer: ProjectWriter,
    private val biometric: BiometricGate,
    private val appState: AppStateStore,
) : ProtectionPorts {
    override suspend fun write(projectKey: Key, label: String, transform: (Project) -> Project): EditResult =
        writer.edit(projectKey, label) { doc, stamp ->
            val next = transform(doc.project)
            if (next == doc.project) doc else doc.copy(project = next.copy(updatedAt = stamp.at, updatedBy = stamp.by))
        }

    override suspend fun authenticate(): Boolean =
        biometric.authenticate(AuthRequest(AuthCopy.HIDDEN_PROJECTS)) == AuthResult.SUCCESS

    override suspend fun forgetRecents(projectKey: Key) {
        appState.ensureLoaded()
        appState.removeRecentsOf(projectKey)
    }
}
