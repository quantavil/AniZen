package tachiyomi.data.source

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.source.model.FeedSavedSearchCategory
import tachiyomi.domain.source.repository.FeedSavedSearchCategoryRepository

class FeedSavedSearchCategoryRepositoryImpl(
    private val handler: DatabaseHandler,
) : FeedSavedSearchCategoryRepository {

    override fun subscribe(): Flow<List<FeedSavedSearchCategory>> {
        return handler.subscribeToList { feed_categoriesQueries.selectAll(FeedSavedSearchCategoryMapper::map) }
    }

    override suspend fun getAll(): List<FeedSavedSearchCategory> {
        return handler.awaitList { feed_categoriesQueries.selectAll(FeedSavedSearchCategoryMapper::map) }
    }

    override suspend fun getGlobal(): FeedSavedSearchCategory? {
        return handler.awaitOneOrNull {
            feed_categoriesQueries.selectById(1, FeedSavedSearchCategoryMapper::map)
        }
    }

    override suspend fun insert(category: FeedSavedSearchCategory) {
        handler.await {
            feed_categoriesQueries.insert(category.name)
        }
    }

    override suspend fun updatePartial(id: Long, name: String) {
        handler.await {
            feed_categoriesQueries.update(name, id)
        }
    }

    override suspend fun delete(id: Long) {
        handler.await {
            feed_categoriesQueries.delete(id)
        }
    }
}
