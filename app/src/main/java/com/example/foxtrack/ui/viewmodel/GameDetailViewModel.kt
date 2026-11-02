package com.example.foxtrack.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foxtrack.R
import com.example.foxtrack.data.AppContainer
import com.example.foxtrack.domain.AppException
import com.example.foxtrack.domain.ErrorCode
import com.example.foxtrack.domain.GameDetail
import com.example.foxtrack.domain.GameStatus
import com.example.foxtrack.domain.Validation
import com.example.foxtrack.domain.asAppException
import com.example.foxtrack.ui.util.ToastEvent
import com.example.foxtrack.ui.util.ToastKind
import com.example.foxtrack.ui.util.UiText
import com.example.foxtrack.ui.util.labelRes
import com.example.foxtrack.ui.util.toUiText
import com.example.foxtrack.ui.util.uiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val loading: Boolean = true,
    val game: GameDetail? = null,
    val error: ErrorCode? = null,
    val savingField: String? = null,
    val savingReview: Boolean = false,
    val savingPlatforms: Boolean = false,
    val deleting: Boolean = false,
)

sealed interface DetailEvent {
    data class Toast(val event: ToastEvent) : DetailEvent
    data class Deleted(val title: String) : DetailEvent
}

class GameDetailViewModel(
    private val container: AppContainer,
    private val gameId: Long,
) : ViewModel() {
    var reviewDraft by mutableStateOf("")
        private set
    var platformDraft by mutableStateOf<Set<String>>(emptySet())
        private set
    var confirmingDelete by mutableStateOf(false)
        private set

    private val mutableState = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = mutableState.asStateFlow()

    private val eventChannel = Channel<DetailEvent>(Channel.BUFFERED)
    val events: Flow<DetailEvent> = eventChannel.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        val userId = container.session.userIdOrNull() ?: return
        mutableState.value = DetailUiState(loading = true)
        viewModelScope.launch {
            try {
                val game = container.library.get(userId, gameId)
                reviewDraft = game.review.orEmpty()
                platformDraft = game.selectedPlatforms.toSet()
                mutableState.value = DetailUiState(loading = false, game = game)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                mutableState.value = DetailUiState(loading = false, error = error.asAppException().code)
            }
        }
    }

    private fun applyGame(updated: GameDetail) {
        val previous = mutableState.value.game
        if (previous == null || previous.review != updated.review) reviewDraft = updated.review.orEmpty()
        if (previous == null || previous.selectedPlatforms != updated.selectedPlatforms) {
            platformDraft = updated.selectedPlatforms.toSet()
        }
        mutableState.update { it.copy(game = updated) }
    }

    private suspend fun toast(kind: ToastKind, text: UiText) {
        eventChannel.send(DetailEvent.Toast(ToastEvent(kind, text)))
    }

    private fun patch(
        field: String,
        request: suspend (Long) -> GameDetail,
        message: (GameDetail) -> UiText,
    ) {
        val userId = container.session.userIdOrNull() ?: return
        if (mutableState.value.savingField != null) return
        mutableState.update { it.copy(savingField = field) }
        viewModelScope.launch {
            try {
                val updated = request(userId)
                applyGame(updated)
                toast(ToastKind.Success, message(updated))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                toast(ToastKind.Error, error.asAppException().code.toUiText())
            } finally {
                mutableState.update { it.copy(savingField = null) }
            }
        }
    }

    fun setStatus(status: GameStatus) {
        val current = mutableState.value.game ?: return
        if (current.status == status) return
        patch(
            field = "status",
            request = { userId -> container.library.updateStatus(userId, gameId, status) },
            message = { updated -> uiText(R.string.detail_toast_status, UiText.Res(updated.status.labelRes())) },
        )
    }

    fun setRating(rating: Int?) {
        patch(
            field = "rating",
            request = { userId -> container.library.updateRating(userId, gameId, rating) },
            message = { updated ->
                val value = updated.rating
                if (value != null) uiText(R.string.detail_toast_rating_set, value) else uiText(R.string.detail_toast_rating_cleared)
            },
        )
    }

    fun onReviewChange(value: String) {
        reviewDraft = value
    }

    val reviewError: ErrorCode?
        get() = Validation.checkReview(reviewDraft)

    val reviewDirty: Boolean
        get() = reviewDraft.trim() != (mutableState.value.game?.review ?: "")

    val platformsDirty: Boolean
        get() {
            val current = mutableState.value.game?.selectedPlatforms ?: return false
            return platformDraft != current.toSet()
        }

    fun saveReview() {
        if (reviewError != null || !reviewDirty) return
        val userId = container.session.userIdOrNull() ?: return
        mutableState.update { it.copy(savingReview = true) }
        viewModelScope.launch {
            try {
                val updated = container.library.updateReview(userId, gameId, reviewDraft.trim())
                applyGame(updated)
                toast(
                    ToastKind.Success,
                    uiText(if (updated.review != null) R.string.detail_toast_review_saved else R.string.detail_toast_review_cleared),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                toast(ToastKind.Error, error.asAppException().code.toUiText())
            } finally {
                mutableState.update { it.copy(savingReview = false) }
            }
        }
    }

    fun togglePlatform(name: String) {
        platformDraft = if (name in platformDraft) platformDraft - name else platformDraft + name
    }

    fun savePlatforms() {
        if (platformDraft.isEmpty() || !platformsDirty) return
        val userId = container.session.userIdOrNull() ?: return
        val selected = mutableState.value.game?.platforms?.filter { it in platformDraft } ?: return
        mutableState.update { it.copy(savingPlatforms = true) }
        viewModelScope.launch {
            try {
                val updated = container.library.updatePlatforms(userId, gameId, selected)
                applyGame(updated)
                toast(ToastKind.Success, uiText(R.string.detail_toast_platforms, updated.selectedPlatforms.joinToString(", ")))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                toast(ToastKind.Error, error.asAppException().code.toUiText())
            } finally {
                mutableState.update { it.copy(savingPlatforms = false) }
            }
        }
    }

    fun requestDelete() {
        confirmingDelete = true
    }

    fun dismissDelete() {
        if (!mutableState.value.deleting) confirmingDelete = false
    }

    fun confirmDelete() {
        val game = mutableState.value.game ?: return
        val userId = container.session.userIdOrNull() ?: return
        if (mutableState.value.deleting) return
        mutableState.update { it.copy(deleting = true) }
        viewModelScope.launch {
            try {
                container.library.remove(userId, gameId)
                eventChannel.send(DetailEvent.Deleted(game.title))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                toast(ToastKind.Error, error.asAppException().code.toUiText())
                confirmingDelete = false
                mutableState.update { it.copy(deleting = false) }
            }
        }
    }
}
