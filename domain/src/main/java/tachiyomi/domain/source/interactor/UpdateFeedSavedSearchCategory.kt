package tachiyomi.domain.source.interactor

import tachiyomi.domain.source.repository.FeedSavedSearchCategoryRepository

class UpdateFeedSavedSearchCategory(
    private val repository: FeedSavedSearchCategoryRepository,
) {

    suspend fun await(id: Long, name: String) {
        repository.updatePartial(id, name)
    }
}
