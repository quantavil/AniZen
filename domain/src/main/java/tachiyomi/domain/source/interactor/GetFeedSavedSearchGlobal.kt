package tachiyomi.domain.source.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.repository.FeedSavedSearchRepository

class GetFeedSavedSearchGlobal(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    suspend fun await(categoryId: Long = 1): List<FeedSavedSearch> {
        return feedSavedSearchRepository.getGlobal(categoryId)
    }

    fun subscribe(categoryId: Long = 1): Flow<List<FeedSavedSearch>> {
        return feedSavedSearchRepository.getGlobalAsFlow(categoryId)
    }
}
