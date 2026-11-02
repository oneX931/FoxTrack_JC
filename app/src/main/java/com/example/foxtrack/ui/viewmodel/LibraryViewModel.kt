package com.example.foxtrack.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foxtrack.data.AppContainer
import com.example.foxtrack.domain.ErrorCode
import com.example.foxtrack.domain.GameStatus
import com.example.foxtrack.domain.LibraryListing
import com.example.foxtrack.domain.asAppException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryFilters(
    val query: String = "",
    val status: GameStatus? = null,
    val platform: String? = null,
) {
    val active: Boolean get() = query.isNotBlank() || status != null || platform != null
}

data class LibraryUiState(
    val loading: Boolean = true,
    val listing: LibraryListing? = null,
    val error: ErrorCode? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(private val container: AppContainer) : ViewModel() {
    var queryInput by mutableStateOf("")
        private set

    private val filtersState = MutableStateFlow(LibraryFilters())
    val filters: StateFlow<LibraryFilters> = filtersState.asStateFlow()

    private val reloadTick = MutableStateFlow(0)
    private val mutableState = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(filtersState, container.library.version, reloadTick) { current, _, _ -> current }
                .collectLatest { load(it) }
        }
    }

    private suspend fun load(current: LibraryFilters) {
        val userId = container.session.userIdOrNull() ?: return
        mutableState.update { it.copy(loading = it.listing == null, error = null) }
        try {
            val listing = container.library.list(userId, current.query, current.status, current.platform)
            mutableState.value = LibraryUiState(loading = false, listing = listing, error = null)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            mutableState.update { it.copy(loading = false, error = error.asAppException().code) }
        }
    }

    fun onQueryInput(value: String) {
        queryInput = value
    }

    fun submitSearch() {
        filtersState.update { it.copy(query = queryInput.trim()) }
    }

    fun clearSearch() {
        queryInput = ""
        filtersState.update { it.copy(query = "") }
    }

    fun selectStatus(status: GameStatus?) {
        filtersState.update { it.copy(status = status) }
    }

    fun selectPlatform(platform: String?) {
        filtersState.update { it.copy(platform = platform) }
    }

    fun resetFilters() {
        queryInput = ""
        filtersState.value = LibraryFilters()
    }

    fun retry() {
        reloadTick.update { it + 1 }
    }
}
