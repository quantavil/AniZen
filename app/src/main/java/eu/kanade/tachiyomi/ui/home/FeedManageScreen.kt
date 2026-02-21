package eu.kanade.tachiyomi.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.RadioMenuItem
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.plus

class FeedManageScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { FeedManageScreenModel() }
        val state by screenModel.state.collectAsState()

        var deleteDialogItem by remember { mutableStateOf<FeedSavedSearch?>(null) }
        var editFeedItem by remember { mutableStateOf<FeedManageScreenModel.FeedItem?>(null) }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = "Manage Feed",
                    navigateUp = { navigator.pop() },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            if (state.items.isEmpty()) {
                EmptyScreen(
                    stringRes = MR.strings.information_empty_category, // reuse for now
                    modifier = Modifier.padding(paddingValues),
                )
                return@Scaffold
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = paddingValues,
            ) {
                itemsIndexed(
                    items = state.items,
                    key = { _, item -> "feed-${item.feed.id}" },
                ) { index, item ->
                    FeedManageItem(
                        title = item.title,
                        type = if (item.feed.savedSearch != null) "Saved Search" else FeedSavedSearch.Type.from(item.feed.type).name,
                        canMoveUp = index != 0,
                        canMoveDown = index != state.items.lastIndex,
                        onMoveUp = { screenModel.moveUp(item.feed) },
                        onMoveDown = { screenModel.moveDown(item.feed) },
                        onDuplicate = { screenModel.duplicate(item.feed) },
                        onDelete = { deleteDialogItem = item.feed },
                        onClick = { editFeedItem = item },
                        modifier = Modifier.animateItem(),
                    )
                    if (index != state.items.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }

        if (deleteDialogItem != null) {
            val feed = deleteDialogItem!!
            AlertDialog(
                onDismissRequest = { deleteDialogItem = null },
                title = { Text(text = "Delete feed item?") },
                text = { Text(text = "Are you sure you want to remove this from your feed?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            screenModel.delete(feed)
                            deleteDialogItem = null
                        },
                    ) {
                        Text(text = stringResource(MR.strings.action_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteDialogItem = null }) {
                        Text(text = stringResource(MR.strings.action_cancel))
                    }
                },
            )
        }

        if (editFeedItem != null) {
            val feedItem = editFeedItem!!
            val feed = feedItem.feed
            val source = feedItem.source

            val savedSearches by produceState<List<SavedSearch>?>(initialValue = null, feed.source) {
                value = screenModel.getSourceSavedSearches(feed.source)
            }

            AlertDialog(
                onDismissRequest = { editFeedItem = null },
                title = { Text(text = stringResource(MR.strings.action_edit)) },
                text = {
                    if (savedSearches == null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            val currentType = FeedSavedSearch.Type.from(feed.type)
                            val isSavedSearch = feed.savedSearch != null

                            if (source is AnimeCatalogueSource && source.supportsLatest) {
                                RadioMenuItem(
                                    text = { Text(text = stringResource(MR.strings.latest)) },
                                    isChecked = !isSavedSearch && currentType == FeedSavedSearch.Type.Latest,
                                ) {
                                    screenModel.updateFeed(feed, FeedSavedSearch.Type.Latest, null)
                                    editFeedItem = null
                                }
                            }
                            RadioMenuItem(
                                text = { Text(text = stringResource(MR.strings.popular)) },
                                isChecked = !isSavedSearch && currentType == FeedSavedSearch.Type.Popular,
                            ) {
                                screenModel.updateFeed(feed, FeedSavedSearch.Type.Popular, null)
                                editFeedItem = null
                            }

                            if (!savedSearches.isNullOrEmpty()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Text(
                                    text = stringResource(SYMR.strings.saved_searches),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                )
                                savedSearches!!.forEach { savedSearch ->
                                    RadioMenuItem(
                                        text = { Text(text = savedSearch.name) },
                                        isChecked = feed.savedSearch == savedSearch.id,
                                    ) {
                                        screenModel.updateFeed(feed, FeedSavedSearch.Type.SavedSearch, savedSearch.id)
                                        editFeedItem = null
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { editFeedItem = null }) {
                        Text(text = stringResource(MR.strings.action_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun FeedManageItem(
    title: String,
    type: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = type,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp,
            ) {
                Icon(imageVector = Icons.Outlined.ArrowDropUp, contentDescription = null)
            }
            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown,
            ) {
                Icon(imageVector = Icons.Outlined.ArrowDropDown, contentDescription = null)
            }
            IconButton(onClick = onDuplicate) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(MR.strings.copy),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(MR.strings.action_delete),
                )
            }
        }
    }
}
