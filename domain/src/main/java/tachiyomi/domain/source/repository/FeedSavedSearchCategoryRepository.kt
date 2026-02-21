package tachiyomi.domain.source.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.model.FeedSavedSearchCategory

interface FeedSavedSearchCategoryRepository {
    fun subscribe(): Flow<List<FeedSavedSearchCategory>>
    suspend fun getAll(): List<FeedSavedSearchCategory>
    suspend fun getGlobal(): FeedSavedSearchCategory?
    suspend fun insert(category: FeedSavedSearchCategory)
    suspend fun updatePartial(id: Long, name: String)
    suspend fun delete(id: Long)
}
