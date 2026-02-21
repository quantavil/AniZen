package tachiyomi.domain.source.interactor

import tachiyomi.domain.source.repository.FeedSavedSearchRepository

class CountFeedSavedSearchGlobal(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    suspend fun await(categoryId: Long = 1): Long {
        return feedSavedSearchRepository.countGlobal(categoryId)
    }
}
