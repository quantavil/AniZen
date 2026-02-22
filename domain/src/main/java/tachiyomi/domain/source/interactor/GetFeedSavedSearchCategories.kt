package tachiyomi.domain.source.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.model.FeedSavedSearchCategory
import tachiyomi.domain.source.repository.FeedSavedSearchCategoryRepository

class GetFeedSavedSearchCategories(
    private val repository: FeedSavedSearchCategoryRepository,
) {

    fun subscribe(): Flow<List<FeedSavedSearchCategory>> {
        return repository.subscribe()
    }

    suspend fun await(): List<FeedSavedSearchCategory> {
        return repository.getAll()
    }
}
