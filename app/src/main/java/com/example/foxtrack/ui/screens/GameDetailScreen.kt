package com.example.foxtrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foxtrack.R
import com.example.foxtrack.data.AppContainer
import com.example.foxtrack.domain.GameDetail
import com.example.foxtrack.domain.GameStatus
import com.example.foxtrack.domain.Validation
import com.example.foxtrack.domain.ErrorCode
import com.example.foxtrack.domain.formatAddedDate
import com.example.foxtrack.domain.formatReleaseDate
import com.example.foxtrack.domain.genreLabel
import com.example.foxtrack.domain.platformColor
import com.example.foxtrack.ui.components.ConfirmDialog
import com.example.foxtrack.ui.components.CoverImage
import com.example.foxtrack.ui.components.ErrorBlock
import com.example.foxtrack.ui.components.FoxButton
import com.example.foxtrack.ui.components.FoxButtonStyle
import com.example.foxtrack.ui.components.FoxIcon
import com.example.foxtrack.ui.components.FoxTabs
import com.example.foxtrack.ui.components.FoxTextField
import com.example.foxtrack.ui.components.LoadingBlock
import com.example.foxtrack.ui.components.LocalToaster
import com.example.foxtrack.ui.components.PlatformCheckRow
import com.example.foxtrack.ui.components.platformIconRes
import com.example.foxtrack.ui.components.RatingStars
import com.example.foxtrack.ui.components.SectionTitle
import com.example.foxtrack.ui.components.StateBlock
import com.example.foxtrack.ui.components.StatusBadge
import com.example.foxtrack.ui.components.Tag
import com.example.foxtrack.ui.theme.FoxTheme
import com.example.foxtrack.ui.theme.GeistMonoFamily
import com.example.foxtrack.ui.theme.pixelFamilyFor
import com.example.foxtrack.ui.util.ToastKind
import com.example.foxtrack.ui.util.asString
import com.example.foxtrack.ui.util.labelRes
import com.example.foxtrack.ui.util.uiText
import com.example.foxtrack.ui.viewmodel.DetailEvent
import com.example.foxtrack.ui.viewmodel.FoxViewModelFactory
import com.example.foxtrack.ui.viewmodel.GameDetailViewModel

@Composable
fun GameDetailScreen(
    container: AppContainer,
    gameId: Long,
    languageTag: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val vm: GameDetailViewModel = viewModel(
        key = "game-$gameId",
        factory = FoxViewModelFactory { GameDetailViewModel(container, gameId) },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val toaster = LocalToaster.current
    val context = LocalContext.current
    val currentOnDeleted by rememberUpdatedState(onDeleted)

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is DetailEvent.Toast -> toaster.show(
                    kind = event.event.kind,
                    message = event.event.text.resolve(context),
                )

                is DetailEvent.Deleted -> {
                    toaster.show(
                        kind = ToastKind.Success,
                        message = uiText(R.string.detail_toast_deleted, event.title).resolve(context),
                    )
                    currentOnDeleted()
                }
            }
        }
    }

    val game = state.game
    val error = state.error
    when {
        state.loading -> LoadingBlock()

        error != null -> {
            if (error == ErrorCode.NOT_FOUND) {
                StateBlock(
                    icon = R.drawable.ic_search_x,
                    title = stringResource(R.string.detail_not_found_title),
                    description = stringResource(R.string.detail_not_found_description),
                    action = {
                        FoxButton(text = stringResource(R.string.detail_back), onClick = onBack)
                    },
                )
            } else {
                ErrorBlock(code = error, onRetry = vm::load)
            }
        }

        game != null -> {
            GameDetailContent(
                game = game,
                vm = vm,
                savingField = state.savingField,
                savingReview = state.savingReview,
                savingPlatforms = state.savingPlatforms,
                languageTag = languageTag,
            )
            if (vm.confirmingDelete) {
                ConfirmDialog(
                    title = stringResource(R.string.detail_delete_confirm_title),
                    description = stringResource(R.string.detail_delete_confirm_description, game.title),
                    confirmLabel = stringResource(R.string.detail_delete_confirm_confirm),
                    cancelLabel = stringResource(R.string.detail_delete_confirm_cancel),
                    onConfirm = vm::confirmDelete,
                    onDismiss = vm::dismissDelete,
                    danger = true,
                    busy = state.deleting,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GameDetailContent(
    game: GameDetail,
    vm: GameDetailViewModel,
    savingField: String?,
    savingReview: Boolean,
    savingPlatforms: Boolean,
    languageTag: String,
) {
    val scheme = MaterialTheme.colorScheme
    val reviewDraft = vm.reviewDraft
    val reviewChars = Validation.countCharacters(reviewDraft)
    val reviewDirty = reviewDraft.trim() != (game.review ?: "")
    val reviewErrorCode = Validation.checkReview(reviewDraft)
    val platformsDirty = vm.platformDraft != game.selectedPlatforms.toSet()
    val release = formatReleaseDate(game.releaseDate, languageTag)
    val added = formatAddedDate(game.addedAt, languageTag)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CoverImage(
                url = game.coverUrl,
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .fillMaxWidth(0.7f)
                    .aspectRatio(3f / 4f)
                    .clip(MaterialTheme.shapes.medium),
                iconSize = 48.dp,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = game.title,
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = pixelFamilyFor(game.title),
                fontWeight = FontWeight.Bold,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusBadge(status = game.status)
                if (release != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FoxIcon(id = R.drawable.ic_calendar, size = 14.dp, tint = scheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = release,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = GeistMonoFamily,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (game.genres.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    game.genres.forEach { genre -> Tag(text = genreLabel(genre, languageTag)) }
                }
            }
        }

        val description = game.description
        if (!description.isNullOrBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle(text = stringResource(R.string.detail_description))
                Text(text = description, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(text = stringResource(R.string.detail_all_platforms))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                game.platforms.forEach { name ->
                    Tag(
                        text = name,
                        leading = platformIconRes(name),
                        leadingTint = Color(platformColor(name)),
                    )
                }
            }
        }

        HorizontalDivider(color = FoxTheme.colors.border)

        SectionTitle(text = stringResource(R.string.detail_progress))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.detail_status),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
            FoxTabs(
                labels = GameStatus.entries.map { stringResource(it.labelRes()) },
                selected = GameStatus.entries.indexOf(game.status),
                onSelect = { vm.setStatus(GameStatus.entries[it]) },
                enabled = savingField != "status",
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.detail_platforms),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
            game.platforms.forEach { name ->
                PlatformCheckRow(
                    name = name,
                    year = null,
                    checked = name in vm.platformDraft,
                    onToggle = { vm.togglePlatform(name) },
                    enabled = !savingPlatforms,
                )
            }
            Spacer(Modifier.height(4.dp))
            FoxButton(
                text = stringResource(R.string.detail_platforms_save),
                onClick = vm::savePlatforms,
                style = FoxButtonStyle.Secondary,
                compact = true,
                enabled = platformsDirty && vm.platformDraft.isNotEmpty(),
                loading = savingPlatforms,
            )
        }

        if (game.status == GameStatus.DONE) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.detail_rating),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )
                RatingStars(
                    value = game.rating,
                    onChange = vm::setRating,
                    enabled = savingField != "rating",
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FoxTextField(
                    value = reviewDraft,
                    onValueChange = vm::onReviewChange,
                    label = stringResource(R.string.detail_review),
                    placeholder = stringResource(R.string.detail_review_placeholder),
                    error = if (reviewDirty) reviewErrorCode?.asString() else null,
                    counter = stringResource(R.string.common_char_count, reviewChars, Validation.REVIEW_MAX_LENGTH),
                    counterOver = reviewChars > Validation.REVIEW_MAX_LENGTH,
                    singleLine = false,
                    minLines = 4,
                    imeAction = ImeAction.Default,
                )
                FoxButton(
                    text = stringResource(R.string.detail_review_save),
                    onClick = vm::saveReview,
                    enabled = reviewDirty && reviewErrorCode == null,
                    loading = savingReview,
                    icon = R.drawable.ic_save,
                )
            }
        } else {
            Row(verticalAlignment = Alignment.Top) {
                FoxIcon(id = R.drawable.ic_star, size = 16.dp, tint = scheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.detail_locked_hint, stringResource(GameStatus.DONE.labelRes())),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }

        HorizontalDivider(color = FoxTheme.colors.border)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (added != null) {
                Text(
                    text = stringResource(R.string.detail_added_at, added),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            FoxButton(
                text = stringResource(R.string.detail_delete),
                onClick = vm::requestDelete,
                style = FoxButtonStyle.Danger,
                icon = R.drawable.ic_trash,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}
