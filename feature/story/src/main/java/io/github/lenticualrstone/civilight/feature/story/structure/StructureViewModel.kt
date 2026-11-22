package io.github.lenticualrstone.civilight.feature.story.structure

import io.github.lenticualrstone.civilight.feature.story.common.StoryServices
import io.github.lenticualrstone.civilight.feature.story.common.StoryViewModel
import io.github.lenticualrstone.civilight.model.Key

/**
 * 스토리 구조 (STO-04 … STO-07, STO-10). The list itself is [StructureModel] over the document; the
 * expanded set is screen state saved with the entry (`rememberSaveable`), so it survives process
 * death without a `SavedStateHandle`. Edits (`씬 지우기`, new units, `완료`) come from [StoryViewModel].
 */
class StructureViewModel(projectKey: Key, services: StoryServices) : StoryViewModel(projectKey, services)
