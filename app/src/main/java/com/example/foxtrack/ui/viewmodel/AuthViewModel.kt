package com.example.foxtrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foxtrack.data.AppContainer
import com.example.foxtrack.domain.ErrorCode
import com.example.foxtrack.domain.asAppException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val submitting: Boolean = false,
    val fieldErrors: Map<String, ErrorCode> = emptyMap(),
    val formError: ErrorCode? = null,
)

class AuthViewModel(private val container: AppContainer) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = mutableState.asStateFlow()

    fun resetErrors() {
        mutableState.update { it.copy(fieldErrors = emptyMap(), formError = null) }
    }

    fun clearFieldError(name: String) {
        mutableState.update { current ->
            if (name in current.fieldErrors) current.copy(fieldErrors = current.fieldErrors - name) else current
        }
    }

    fun submit(register: Boolean, username: String, email: String, password: String) {
        if (mutableState.value.submitting) return
        mutableState.value = AuthUiState(submitting = true)
        viewModelScope.launch {
            try {
                val user = if (register) {
                    container.auth.register(username, email, password)
                } else {
                    container.auth.login(email, password)
                }
                mutableState.value = AuthUiState()
                container.session.signedIn(user)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val failure = error.asAppException()
                mutableState.value = if (failure.fields.isNotEmpty()) {
                    AuthUiState(fieldErrors = failure.fields)
                } else {
                    AuthUiState(formError = failure.code)
                }
            }
        }
    }
}
