package com.example.foxtrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foxtrack.R
import com.example.foxtrack.data.AppContainer
import com.example.foxtrack.domain.ErrorCode
import com.example.foxtrack.domain.SearchGroup
import com.example.foxtrack.ui.components.Attribution
import com.example.foxtrack.ui.components.CoverImage
import com.example.foxtrack.ui.components.ErrorBlock
import com.example.foxtrack.ui.components.FoxButton
import com.example.foxtrack.ui.components.FoxButtonStyle
import com.example.foxtrack.ui.components.FoxIcon
import com.example.foxtrack.ui.components.PlatformCheckRow
import com.example.foxtrack.ui.components.PlatformOverflowRow
import com.example.foxtrack.ui.components.SearchField
import com.example.foxtrack.ui.components.SkeletonList
import com.example.foxtrack.ui.components.StateBlock
import com.example.foxtrack.ui.components.ToastEffect
import com.example.foxtrack.ui.theme.FoxTheme
import com.example.foxtrack.ui.theme.GeistMonoFamily
import com.example.foxtrack.ui.theme.pixelFamilyFor
import com.example.foxtrack.ui.util.asString
import com.example.foxtrack.ui.viewmodel.AddGameViewModel
import com.example.foxtrack.ui.viewmodel.FoxViewModelFactory
import com.example.foxtrack.ui.viewmodel.SearchPhase

@Composable
fun AddGameScreen(
    container: AppContainer,
    onOpenGame: (Long) -> Unit,
) {
    val vm: AddGameViewModel = viewModel(factory = FoxViewModelFactory { AddGameViewModel(container) })
    val state by vm.state.collectAsStateWithLifecycle()

    ToastEffect(events = vm.toasts) { key ->
        if (key.startsWith("open:")) key.removePrefix("open:").toLongOrNull()?.let(onOpenGame)
    }

    val title = stringResource(R.string.add_title)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = pixelFamilyFor(title),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.add_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SearchField(
                value = vm.queryInput,
                onValueChange = vm::onQueryInput,
                placeholder = stringResource(R.string.add_search_placeholder),
                submitDescription = stringResource(R.string.add_search_submit),
                clearDescription = stringResource(R.string.library_search_clear),
                onSubmit = vm::submitSearch,
                onClear = vm::clearSearch,
                error = vm.searchError?.asString(),
            )
        }

        when (state.phase) {
            SearchPhase.Idle -> item {
                StateBlock(
                    icon = R.drawable.ic_gamepad,
                    title = stringResource(R.string.add_idle_title),
                    description = stringResource(R.string.add_idle_description, stringResource(R.string.add_search_submit)),
                )
            }

            SearchPhase.Loading -> item { SkeletonList(count = 4) }

            SearchPhase.Error -> item {
                ErrorBlock(
                    code = state.error ?: ErrorCode.HTTP_ERROR,
                    onRetry = vm::retry,
                    title = stringResource(R.string.add_error_title),
                )
            }

            SearchPhase.Loaded -> {
                if (state.results.isEmpty()) {
                    item {
                        StateBlock(
                            icon = R.drawable.ic_search_x,
                            title = stringResource(R.string.add_no_results_title),
                            description = stringResource(R.string.add_no_results_description),
                        )
                    }
                } else {
                    items(items = state.results, key = { it.key }) { group ->
                        SearchResultCard(
                            group = group,
                            ownedId = state.owned[group.key] ?: group.libraryGameId,
                            onAdd = { vm.openDialog(group) },
                            onOpen = onOpenGame,
                        )
                    }
                    if (state.hasMore) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                FoxButton(
                                    text = stringResource(R.string.add_load_more),
                                    onClick = vm::loadMore,
                                    style = FoxButtonStyle.Secondary,
                                    loading = state.loadingMore,
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Attribution() }
    }

    val group = vm.dialogGroup
    if (group != null) {
        AddGameDialog(
            group = group,
            checked = vm.checked,
            submitting = vm.submitting,
            error = vm.dialogError,
            onToggle = vm::toggle,
            onConfirm = vm::confirm,
            onDismiss = vm::closeDialog,
        )
    }
}

@Composable
private fun SearchResultCard(
    group: SearchGroup,
    ownedId: Long?,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    val colors = FoxTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CoverImage(
                url = group.cover,
                modifier = Modifier
                    .width(76.dp)
                    .aspectRatio(3f / 4f)
                    .clip(MaterialTheme.shapes.small),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = group.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val year = group.year
                    if (year != null) {
                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = GeistMonoFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (ownedId != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FoxIcon(id = R.drawable.ic_circle_check, size = 18.dp, tint = colors.done)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.add_owned),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.done,
                            fontWeight = FontWeight.Medium,
                        )
                        TextButton(onClick = { onOpen(ownedId) }) {
                            Text(text = stringResource(R.string.add_open), color = colors.accentInk)
                        }
                    }
                } else {
                    PlatformOverflowRow(platforms = group.platforms, modifier = Modifier.fillMaxWidth())
                    FoxButton(
                        text = stringResource(R.string.add_dialog_confirm),
                        onClick = onAdd,
                        style = FoxButtonStyle.Secondary,
                        compact = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddGameDialog(
    group: SearchGroup,
    checked: Set<Long>,
    submitting: Boolean,
    error: ErrorCode?,
    onToggle: (Long) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = FoxTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.large,
        title = { Text(text = stringResource(R.string.add_dialog_title), fontWeight = FontWeight.SemiBold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    CoverImage(
                        url = group.cover,
                        modifier = Modifier
                            .width(48.dp)
                            .aspectRatio(3f / 4f)
                            .clip(MaterialTheme.shapes.extraSmall),
                        iconSize = 20.dp,
                    )
                    Text(
                        text = stringResource(R.string.add_dialog_subtitle, group.title),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                group.platforms.forEach { platform ->
                    PlatformCheckRow(
                        name = platform.name,
                        year = platform.year,
                        checked = platform.gameId in checked,
                        onToggle = { onToggle(platform.gameId) },
                        enabled = !submitting,
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (error != null) {
                    Text(
                        text = error.asString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.danger,
                    )
                } else if (checked.isEmpty()) {
                    Text(
                        text = stringResource(R.string.add_dialog_select_at_least_one),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            val label = stringResource(R.string.add_dialog_confirm)
            FoxButton(
                text = if (checked.isNotEmpty()) "$label (${checked.size})" else label,
                onClick = onConfirm,
                loading = submitting,
                enabled = checked.isNotEmpty(),
                compact = true,
            )
        },
        dismissButton = {
            FoxButton(
                text = stringResource(R.string.add_dialog_cancel),
                onClick = onDismiss,
                style = FoxButtonStyle.Ghost,
                enabled = !submitting,
                compact = true,
            )
        },
    )
}
