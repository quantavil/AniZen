package tachiyomi.data.source

import tachiyomi.domain.source.model.FeedSavedSearchCategory

object FeedSavedSearchCategoryMapper {
    fun map(
        id: Long,
        name: String,
        order: Long,
    ): FeedSavedSearchCategory {
        return FeedSavedSearchCategory(
            id = id,
            name = name,
            order = order,
        )
    }
}
