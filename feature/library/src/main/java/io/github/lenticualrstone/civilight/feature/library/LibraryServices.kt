package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.feature.library.common.NoBackupYet
import io.github.lenticualrstone.civilight.feature.library.common.ProjectWriter
import io.github.lenticualrstone.civilight.feature.library.common.ProtectionFacts
import io.github.lenticualrstone.civilight.feature.library.common.SessionProjectWriter
import io.github.lenticualrstone.civilight.feature.library.newproject.NewProjectDraftHolder
import io.github.lenticualrstone.civilight.feature.library.protection.AppProtectionPorts
import io.github.lenticualrstone.civilight.feature.library.protection.ProtectionActions
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps

/**
 * What every screen of this module shares, made once per activity in [libraryEntries] (in-memory
 * objects only; no I/O at creation): the shell's [deps], the one write path for projects that are
 * not the current screen's, the protection rules, the facts of later batches and the 새 프로젝트
 * draft that its four step entries share.
 */
class LibraryServices(val deps: FeatureDeps) {
    val writer: ProjectWriter = SessionProjectWriter(deps.projects)
    val protection = ProtectionActions(AppProtectionPorts(writer, deps.biometric, deps.stores.appState))
    val facts: ProtectionFacts = NoBackupYet
    val draft = NewProjectDraftHolder()
}
