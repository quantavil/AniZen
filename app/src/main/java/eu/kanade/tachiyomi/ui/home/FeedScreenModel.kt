package eu.kanade.tachiyomi.ui.home

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.anime.interactor.NetworkToLocalAnime
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.toDomainAnime
import tachiyomi.domain.source.interactor.GetFeedSavedSearchCategories
import tachiyomi.domain.source.interactor.GetFeedSavedSearchGlobal
import tachiyomi.domain.source.interactor.GetSavedSearchGlobalFeed
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.model.FeedSavedSearchCategory
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class FeedScreenModel(
    private val sourceManager: SourceManager = Injekt.get(),
    private val getFeedSavedSearchGlobal: GetFeedSavedSearchGlobal = Injekt.get(),
    private val getSavedSearchGlobalFeed: GetSavedSearchGlobalFeed = Injekt.get(),
    private val networkToLocalAnime: NetworkToLocalAnime = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    private val getFeedSavedSearchCategories: GetFeedSavedSearchCategories = Injekt.get(),
) : StateScreenModel<FeedScreenModel.State>(State()) {

    init {
        screenModelScope.launchIO {
            val categories = getFeedSavedSearchCategories.await()
            mutableState.update { it.copy(categories = categories.toImmutableList()) }

            getFeedSavedSearchCategories.subscribe()
                .onEach { updatedCategories ->
                    mutableState.update { it.copy(categories = updatedCategories.toImmutableList()) }
                }
                .launchIn(screenModelScope)
        }

        screenModelScope.launchIO {
            state.map { it.selectedCategoryId }
                .distinctUntilChanged()
                .flatMapLatest { categoryId ->
                    combine(
                        getFeedSavedSearchGlobal.subscribe(categoryId),
                        sourceManager.isInitialized,
                        ::Pair
                    )
                }
                .collectLatest { (feedSavedSearches, isInitialized) ->
                    if (!isInitialized) {
                        mutableState.update { it.copy(isLoading = true) }
                        return@collectLatest
                    }

                    if (feedSavedSearches.isEmpty()) {
                        mutableState.update { it.copy(isLoading = false, items = persistentListOf()) }
                        return@collectLatest
                    }

                    mutableState.update { it.copy(isLoading = true) }

                    // Fetch saved searches for the current category
                    val savedSearches = getSavedSearchGlobalFeed.await(state.value.selectedCategoryId)
                    
                    val feedItems = coroutineScope {
                        feedSavedSearches.map { feed ->
                            async {
                                val source = sourceManager.get(feed.source) as? AnimeCatalogueSource
                                if (source != null) {
                                    val results = try {
                                        when (FeedSavedSearch.Type.from(feed.type)) {
                                            FeedSavedSearch.Type.Latest -> {
                                                try {
                                                    source.getLatestUpdates(1).animes
                                                } catch (e: Exception) {
                                                    source.getPopularAnime(1).animes
                                                }
                                            }
                                            FeedSavedSearch.Type.Popular -> source.getPopularAnime(1).animes
                                            FeedSavedSearch.Type.SavedSearch -> {
                                                val savedSearch = savedSearches.find { it.id == feed.savedSearch }
                                                if (savedSearch != null) {
                                                    val filters = source.getFilterList()
                                                    source.getSearchAnime(1, savedSearch.query ?: "", filters).animes
                                                } else {
                                                    emptyList()
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        emptyList()
                                    }

                                    val animeList = results.map {
                                        async {
                                            val domainAnime = it.toDomainAnime(source.id)
                                            networkToLocalAnime.await(domainAnime)
                                        }
                                    }.awaitAll()

                                    FeedItem(
                                        feed = feed,
                                        source = source,
                                        savedSearch = savedSearches.find { it.id == feed.savedSearch },
                                        animeList = animeList.distinctBy { it.id }.toImmutableList(),
                                    )
                                } else {
                                    null
                                }
                            }
                        }.awaitAll().filterNotNull()
                    }

                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            items = feedItems.toImmutableList(),
                        )
                    }
                }
        }
    }

    fun onCategorySelected(categoryId: Long) {
        mutableState.update { it.copy(selectedCategoryId = categoryId) }
    }

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val items: ImmutableList<FeedItem> = persistentListOf(),
        val categories: ImmutableList<FeedSavedSearchCategory> = persistentListOf(),
        val selectedCategoryId: Long = 1,
    )

    @Immutable
    data class FeedItem(
        val feed: FeedSavedSearch,
        val source: AnimeCatalogueSource,
        val savedSearch: SavedSearch?,
        val animeList: ImmutableList<Anime>,
    )
}