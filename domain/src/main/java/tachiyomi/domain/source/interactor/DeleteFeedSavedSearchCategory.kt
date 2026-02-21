package tachiyomi.domain.source.interactor

import tachiyomi.domain.source.repository.FeedSavedSearchCategoryRepository

class DeleteFeedSavedSearchCategory(
    private val repository: FeedSavedSearchCategoryRepository,
) {

    suspend fun await(id: Long) {
        repository.delete(id)
    }
}
