package com.example.foxtrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foxtrack.R
import com.example.foxtrack.data.AppContainer
import com.example.foxtrack.domain.GameStatus
import com.example.foxtrack.domain.LibraryCard
import com.example.foxtrack.ui.components.Attribution
import com.example.foxtrack.ui.components.CoverImage
import com.example.foxtrack.ui.components.ErrorBlock
import com.example.foxtrack.ui.components.FoxButton
import com.example.foxtrack.ui.components.FoxButtonStyle
import com.example.foxtrack.ui.components.FoxIcon
import com.example.foxtrack.ui.components.PlatformIcon
import com.example.foxtrack.ui.components.RatingChip
import com.example.foxtrack.ui.components.SearchField
import com.example.foxtrack.ui.components.SkeletonList
import com.example.foxtrack.ui.components.StateBlock
import com.example.foxtrack.ui.components.StatusBadge
import com.example.foxtrack.ui.theme.FoxTheme
import com.example.foxtrack.ui.theme.pixelFamilyFor
import com.example.foxtrack.ui.util.labelRes
import com.example.foxtrack.ui.viewmodel.FoxViewModelFactory
import com.example.foxtrack.ui.viewmodel.LibraryViewModel

@Composable
fun LibraryScreen(
    container: AppContainer,
    onOpenGame: (Long) -> Unit,
    onAddGame: () -> Unit,
) {
    val vm: LibraryViewModel = viewModel(factory = FoxViewModelFactory { LibraryViewModel(container) })
    val state by vm.state.collectAsStateWithLifecycle()
    val filters by vm.filters.collectAsStateWithLifecycle()
    val resources = LocalContext.current.resources
    val listing = state.listing
    val hasAnyGames = (listing?.allCount ?: 0) > 0 || (listing?.total ?: 0) > 0 || filters.active
    val title = stringResource(R.string.library_title)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = pixelFamilyFor(title),
                    fontWeight = FontWeight.Bold,
                )
                if (listing != null && listing.allCount > 0) {
                    Text(
                        text = resources.getQuantityString(R.plurals.library_total_count, listing.allCount, listing.allCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if ((state.loading || listing != null) && hasAnyGames) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SearchField(
                        value = vm.queryInput,
                        onValueChange = vm::onQueryInput,
                        placeholder = stringResource(R.string.library_search_placeholder),
                        submitDescription = stringResource(R.string.library_search_submit),
                        clearDescription = stringResource(R.string.library_search_clear),
                        onSubmit = vm::submitSearch,
                        onClear = vm::clearSearch,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChipItem(
                                label = stringResource(R.string.library_status_all),
                                count = listing?.allCount,
                                selected = filters.status == null,
                                onClick = { vm.selectStatus(null) },
                            )
                            GameStatus.entries.forEach { status ->
                                FilterChipItem(
                                    label = stringResource(status.labelRes()),
                                    count = listing?.counts?.get(status),
                                    selected = filters.status == status,
                                    onClick = { vm.selectStatus(status) },
                                )
                            }
                        }
                        val platforms = listing?.platforms.orEmpty()
                        if (platforms.isNotEmpty()) {
                            PlatformFilter(
                                platforms = platforms,
                                selected = filters.platform,
                                onSelect = vm::selectPlatform,
                            )
                        }
                    }
                }
            }
        }

        if (state.loading && listing == null) {
            item(span = { GridItemSpan(maxLineSpan) }) { SkeletonList(count = 6) }
        }

        val error = state.error
        if (error != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ErrorBlock(code = error, onRetry = vm::retry)
            }
        }

        if (listing != null && listing.items.isEmpty() && error == null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                if (!filters.active) {
                    StateBlock(
                        icon = R.drawable.ic_library,
                        title = stringResource(R.string.library_empty_title),
                        description = stringResource(R.string.library_empty_description),
                        action = {
                            FoxButton(
                                text = stringResource(R.string.library_empty_action),
                                onClick = onAddGame,
                                icon = R.drawable.ic_plus,
                            )
                        },
                    )
                } else {
                    StateBlock(
                        icon = R.drawable.ic_search_x,
                        title = stringResource(R.string.library_no_results_title),
                        description = stringResource(R.string.library_no_results_description),
                        action = {
                            FoxButton(
                                text = stringResource(R.string.library_no_results_reset),
                                onClick = vm::resetFilters,
                                style = FoxButtonStyle.Secondary,
                            )
                        },
                    )
                }
            }
        }

        if (listing != null) {
            items(items = listing.items, key = { it.id }) { card ->
                LibraryGameCard(card = card, onClick = { onOpenGame(card.id) })
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) { Attribution() }
    }
}

@Composable
private fun FilterChipItem(label: String, count: Int?, selected: Boolean, onClick: () -> Unit) {
    val colors = FoxTheme.colors
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (selected) colors.focus else scheme.surface,
        contentColor = if (selected) scheme.background else scheme.onSurface,
        border = BorderStroke(1.dp, if (selected) colors.focus else colors.borderStrong),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, maxLines = 1)
            if (count != null) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) scheme.background.copy(alpha = 0.75f) else scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PlatformFilter(platforms: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    val colors = FoxTheme.colors
    var expanded by remember { mutableStateOf(false) }
    val allLabel = stringResource(R.string.library_filter_platform_all)
    Box {
        Surface(
            onClick = { expanded = true },
            shape = MaterialTheme.shapes.small,
            color = colors.field,
            border = BorderStroke(1.dp, colors.borderStrong),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selected != null) {
                    PlatformIcon(name = selected, size = 14.dp)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = selected ?: allLabel,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 120.dp),
                )
                Spacer(Modifier.width(4.dp))
                FoxIcon(id = R.drawable.ic_chevron_down, size = 16.dp)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(allLabel) },
                onClick = {
                    expanded = false
                    onSelect(null)
                },
            )
            platforms.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    leadingIcon = { PlatformIcon(name = name, size = 16.dp) },
                    onClick = {
                        expanded = false
                        onSelect(name)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryGameCard(card: LibraryCard, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, FoxTheme.colors.border),
    ) {
        Column {
            CoverImage(
                url = card.coverUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f),
                iconSize = 36.dp,
            )
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlatformIcon(name = card.selectedPlatforms.firstOrNull(), size = 14.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = card.selectedPlatforms.joinToString(", "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StatusBadge(status = card.status)
                    RatingChip(rating = card.rating)
                }
            }
        }
    }
}
