package eu.kanade.tachiyomi.ui.home

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.source.interactor.DeleteFeedSavedSearchById
import tachiyomi.domain.source.interactor.GetFeedSavedSearchGlobal
import tachiyomi.domain.source.interactor.GetSavedSearchGlobalFeed
import tachiyomi.domain.source.interactor.ReorderFeed
import tachiyomi.domain.source.interactor.UpdateFeedSavedSearch
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.model.FeedSavedSearchUpdate
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

import tachiyomi.domain.source.interactor.DeleteFeedSavedSearchCategory
import tachiyomi.domain.source.interactor.GetFeedSavedSearchCategories
import tachiyomi.domain.source.interactor.InsertFeedSavedSearchCategory
import tachiyomi.domain.source.interactor.UpdateFeedSavedSearchCategory
import tachiyomi.domain.source.model.FeedSavedSearchCategory

class FeedManageScreenModel(
    private val sourceManager: SourceManager = Injekt.get(),
    private val getFeedSavedSearchGlobal: GetFeedSavedSearchGlobal = Injekt.get(),
    private val getSavedSearchGlobalFeed: GetSavedSearchGlobalFeed = Injekt.get(),
    private val reorderFeed: ReorderFeed = Injekt.get(),
    private val deleteFeedSavedSearchById: DeleteFeedSavedSearchById = Injekt.get(),
    private val updateFeedSavedSearch: UpdateFeedSavedSearch = Injekt.get(),
    private val getSavedSearchBySourceId: tachiyomi.domain.source.interactor.GetSavedSearchBySourceId = Injekt.get(),
    private val insertFeedSavedSearch: tachiyomi.domain.source.interactor.InsertFeedSavedSearch = Injekt.get(),
    private val getFeedSavedSearchCategories: GetFeedSavedSearchCategories = Injekt.get(),
    private val insertFeedSavedSearchCategory: InsertFeedSavedSearchCategory = Injekt.get(),
    private val updateFeedSavedSearchCategory: UpdateFeedSavedSearchCategory = Injekt.get(),
    private val deleteFeedSavedSearchCategory: DeleteFeedSavedSearchCategory = Injekt.get(),
) : StateScreenModel<FeedManageScreenModel.State>(State()) {

    init {
        screenModelScope.launchIO {
            getFeedSavedSearchCategories.subscribe().collect { categories ->
                mutableState.update { it.copy(categories = categories.toImmutableList()) }
                if (state.value.selectedCategoryId == -1L && categories.isNotEmpty()) {
                     mutableState.update { it.copy(selectedCategoryId = categories.first().id) }
                }
                getFeed()
            }
        }
    }

    fun selectCategory(categoryId: Long) {
        mutableState.update { it.copy(selectedCategoryId = categoryId) }
        getFeed()
    }

    fun createCategory(name: String) {
        screenModelScope.launchIO {
            insertFeedSavedSearchCategory.await(name)
        }
    }

    fun deleteCategory(categoryId: Long) {
        screenModelScope.launchIO {
            deleteFeedSavedSearchCategory.await(categoryId)
            if (state.value.selectedCategoryId == categoryId) {
                mutableState.update { it.copy(selectedCategoryId = 1) } // Default to Global
            }
        }
    }

    fun renameCategory(categoryId: Long, name: String) {
        screenModelScope.launchIO {
            updateFeedSavedSearchCategory.await(categoryId, name)
        }
    }

    fun getFeed() {
        val categoryId = state.value.selectedCategoryId
        if (categoryId == -1L) return

        screenModelScope.launchIO {
            val feedSavedSearches = getFeedSavedSearchGlobal.await(categoryId)
            val savedSearches = getSavedSearchGlobalFeed.await(categoryId)
            
            val items = feedSavedSearches.map { feed ->
                val source = sourceManager.get(feed.source)
                val savedSearch = savedSearches.find { it.id == feed.savedSearch }
                
                FeedItem(
                    feed = feed,
                    title = source?.name ?: "Unknown",
                    subtitle = savedSearch?.name ?: FeedSavedSearch.Type.from(feed.type).name,
                    source = source,
                )
            }

            mutableState.update {
                it.copy(
                    items = items.toImmutableList(),
                )
            }
        }
    }

    suspend fun getSourceSavedSearches(sourceId: Long): List<SavedSearch> {
        return getSavedSearchBySourceId.await(sourceId)
    }

    fun updateFeed(feed: FeedSavedSearch, type: FeedSavedSearch.Type, savedSearchId: Long?) {
        screenModelScope.launchIO {
            updateFeedSavedSearch.await(
                FeedSavedSearchUpdate(
                    id = feed.id,
                    searchType = type.value.toLong(),
                    savedSearch = savedSearchId,
                    deleteSavedSearch = savedSearchId == null,
                )
            )
            getFeed()
        }
    }

    fun duplicate(feed: FeedSavedSearch) {
        screenModelScope.launchIO {
            val currentFeed = getFeedSavedSearchGlobal.await(feed.category)
            val nextOrder = (currentFeed.maxOfOrNull { it.feedOrder } ?: -1) + 1
            insertFeedSavedSearch.await(
                feed.copy(
                    id = -1,
                    feedOrder = nextOrder
                )
            )
            getFeed()
        }
    }

    fun moveUp(feed: FeedSavedSearch) {
        screenModelScope.launchIO {
            reorderFeed.moveUp(feed)
            getFeed()
        }
    }

    fun moveDown(feed: FeedSavedSearch) {
        screenModelScope.launchIO {
            reorderFeed.moveDown(feed)
            getFeed()
        }
    }

    fun delete(feed: FeedSavedSearch) {
        screenModelScope.launchIO {
            deleteFeedSavedSearchById.await(feed.id)
            getFeed()
        }
    }

    @Immutable
    data class State(
        val items: ImmutableList<FeedItem> = persistentListOf(),
        val categories: ImmutableList<FeedSavedSearchCategory> = persistentListOf(),
        val selectedCategoryId: Long = -1L,
    )

    @Immutable
    data class FeedItem(
        val feed: FeedSavedSearch,
        val title: String,
        val subtitle: String,
        val source: eu.kanade.tachiyomi.source.Source?,
    )
}