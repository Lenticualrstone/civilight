package io.github.lenticualrstone.civilight.feature.library.rating

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.library.rating.Protection
import io.github.lenticualrstone.civilight.domain.library.rating.RatedProject
import io.github.lenticualrstone.civilight.domain.library.rating.RatingDefaultsTable
import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.common.LibraryDocs
import io.github.lenticualrstone.civilight.feature.library.protection.ProtectionOutcome
import io.github.lenticualrstone.civilight.model.project.Rating
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 설정 · 수위 등급 and its `같이 바꾸기` list (LIB-23): app settings for the defaults, the live
 * projects (not the example) as [RatedProject]s for the tags and the list.
 */
internal class RatingSettingsViewModel(private val services: LibraryServices) : ViewModel() {
    private val deps = services.deps
    private val docs = LibraryDocs(deps.stores.library, viewModelScope)

    val projects: StateFlow<List<RatedProject>> = combine(deps.stores.library.state, docs.docs) { library, docs ->
        library.live.filter { !it.isExample }.mapNotNull { entry ->
            docs[entry.key]?.let { RatedProject(entry.key, it.project.name, RatingLabels.effective(it.project.rating), it.project.protections) }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val state: StateFlow<RatingSettingsUi> = combine(deps.settings, projects, deps.unlock.state) { settings, projects, unlock ->
        RatingSettingsState.build(settings.ratingDefaults, projects, unlock.hiddenGroupOpen)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RatingSettingsState.build(deps.settings.value.ratingDefaults, emptyList(), false))

    fun setDefault(rating: Rating, protection: Protection, on: Boolean) {
        if (!RatingDefaultsTable.isEditable(rating, protection)) return
        viewModelScope.launch { deps.stores.settings.updateRatingDefaults { RatingDefaultsTable.change(it, rating, protection, on) } }
    }

    fun setExportCover(on: Boolean) {
        viewModelScope.launch { deps.stores.settings.updateRatingDefaults { it.copy(exportCoverRating = on) } }
    }

    /** `같이 바꾸기` on the checked rows: one prompt when hiding goes off anywhere; cancel changes nothing. */
    fun apply(rows: List<ApplyRowUi>, done: (ProtectionOutcome) -> Unit) {
        viewModelScope.launch {
            done(services.protection.applyDefaultsAll(rows.map { Triple(it.project.key, it.project.protections, it.after) }))
        }
    }
}
