package com.example.foxtrack.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foxtrack.R
import com.example.foxtrack.data.AppContainer
import com.example.foxtrack.domain.ErrorCode
import com.example.foxtrack.domain.SearchGroup
import com.example.foxtrack.domain.Validation
import com.example.foxtrack.domain.asAppException
import com.example.foxtrack.domain.mergeGroups
import com.example.foxtrack.ui.util.ToastEvent
import com.example.foxtrack.ui.util.ToastKind
import com.example.foxtrack.ui.util.toUiText
import com.example.foxtrack.ui.util.uiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SearchPhase { Idle, Loading, Loaded, Error }

data class AddUiState(
    val phase: SearchPhase = SearchPhase.Idle,
    val query: String = "",
    val results: List<SearchGroup> = emptyList(),
    val hasMore: Boolean = false,
    val page: Int = 1,
    val error: ErrorCode? = null,
    val loadingMore: Boolean = false,
    val owned: Map<String, Long> = emptyMap(),
)

class AddGameViewModel(private val container: AppContainer) : ViewModel() {
    var queryInput by mutableStateOf("")
        private set
    var searchError by mutableStateOf<ErrorCode?>(null)
        private set
    var dialogGroup by mutableStateOf<SearchGroup?>(null)
        private set
    var checked by mutableStateOf<Set<Long>>(emptySet())
        private set
    var submitting by mutableStateOf(false)
        private set
    var dialogError by mutableStateOf<ErrorCode?>(null)
        private set

    private val mutableState = MutableStateFlow(AddUiState())
    val state: StateFlow<AddUiState> = mutableState.asStateFlow()

    private val toastChannel = Channel<ToastEvent>(Channel.BUFFERED)
    val toasts: Flow<ToastEvent> = toastChannel.receiveAsFlow()

    private var searchJob: Job? = null

    fun onQueryInput(value: String) {
        queryInput = value
        if (searchError != null) searchError = null
    }

    fun submitSearch() {
        val trimmed = queryInput.trim()
        if (trimmed.isEmpty()) {
            searchJob?.cancel()
            searchError = null
            mutableState.value = AddUiState()
            return
        }
        val problem = Validation.checkSearch(trimmed)
        if (problem != null) {
            searchError = problem
            return
        }
        searchError = null
        runSearch(trimmed)
    }

    fun clearSearch() {
        searchJob?.cancel()
        queryInput = ""
        searchError = null
        mutableState.value = AddUiState()
    }

    fun retry() {
        val query = mutableState.value.query
        if (query.isNotEmpty()) runSearch(query)
    }

    private fun runSearch(query: String) {
        searchJob?.cancel()
        mutableState.value = AddUiState(phase = SearchPhase.Loading, query = query)
        searchJob = viewModelScope.launch {
            val userId = container.session.userIdOrNull() ?: return@launch
            try {
                val page = container.search.search(userId, query, 1)
                mutableState.value = AddUiState(
                    phase = SearchPhase.Loaded,
                    query = query,
                    results = page.results,
                    hasMore = page.hasMore,
                    page = 1,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                mutableState.value = AddUiState(
                    phase = SearchPhase.Error,
                    query = query,
                    error = error.asAppException().code,
                )
            }
        }
    }

    fun loadMore() {
        val current = mutableState.value
        if (current.loadingMore || !current.hasMore || current.phase != SearchPhase.Loaded) return
        val userId = container.session.userIdOrNull() ?: return
        mutableState.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            try {
                val next = current.page + 1
                val page = container.search.search(userId, current.query, next)
                mutableState.update {
                    it.copy(
                        results = mergeGroups(it.results, page.results),
                        hasMore = page.hasMore,
                        page = next,
                        loadingMore = false,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                mutableState.update { it.copy(loadingMore = false) }
                toastChannel.send(ToastEvent(ToastKind.Error, error.asAppException().code.toUiText()))
            }
        }
    }

    fun openDialog(group: SearchGroup) {
        dialogGroup = group
        checked = emptySet()
        dialogError = null
        submitting = false
    }

    fun closeDialog() {
        if (!submitting) dialogGroup = null
    }

    fun toggle(gameId: Long) {
        checked = if (gameId in checked) checked - gameId else checked + gameId
        dialogError = null
    }

    fun confirm() {
        val group = dialogGroup ?: return
        if (submitting) return
        if (checked.isEmpty()) {
            dialogError = ErrorCode.PLATFORM_REQUIRED
            return
        }
        val userId = container.session.userIdOrNull() ?: return
        submitting = true
        dialogError = null
        viewModelScope.launch {
            try {
                val game = container.library.add(
                    userId = userId,
                    gameIds = group.platforms.map { it.gameId },
                    selectedGameIds = checked.toList(),
                )
                dialogGroup = null
                submitting = false
                mutableState.update { it.copy(owned = it.owned + (group.key to game.id)) }
                toastChannel.send(
                    ToastEvent(
                        kind = ToastKind.Success,
                        text = uiText(
                            R.string.add_toast_success,
                            game.title,
                            game.selectedPlatforms.joinToString(", "),
                        ),
                        actionLabel = uiText(R.string.add_open),
                        actionKey = "open:${game.id}",
                    ),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                dialogError = error.asAppException().code
                submitting = false
            }
        }
    }
}
