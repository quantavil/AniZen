package tachiyomi.domain.source.interactor

import tachiyomi.domain.source.model.FeedSavedSearchCategory
import tachiyomi.domain.source.repository.FeedSavedSearchCategoryRepository

class InsertFeedSavedSearchCategory(
    private val repository: FeedSavedSearchCategoryRepository,
) {

    suspend fun await(name: String) {
        repository.insert(FeedSavedSearchCategory(0, name, 0))
    }
}
