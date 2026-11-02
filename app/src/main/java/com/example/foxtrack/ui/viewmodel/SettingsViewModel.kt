package com.example.foxtrack.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foxtrack.R
import com.example.foxtrack.data.AppContainer
import com.example.foxtrack.domain.ErrorCode
import com.example.foxtrack.domain.asAppException
import com.example.foxtrack.ui.util.ToastEvent
import com.example.foxtrack.ui.util.ToastKind
import com.example.foxtrack.ui.util.UiText
import com.example.foxtrack.ui.util.prepareAvatarImage
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

data class SettingsUiState(
    val avatarBusy: Boolean = false,
    val usernameSaving: Boolean = false,
    val usernameError: ErrorCode? = null,
    val emailSaving: Boolean = false,
    val emailErrors: Map<String, ErrorCode> = emptyMap(),
    val passwordSaving: Boolean = false,
    val passwordErrors: Map<String, ErrorCode> = emptyMap(),
)

sealed interface SettingsEvent {
    data class Toast(val event: ToastEvent) : SettingsEvent
    data object EmailSaved : SettingsEvent
    data object PasswordSaved : SettingsEvent
}

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    private val eventChannel = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = eventChannel.receiveAsFlow()

    private suspend fun toast(kind: ToastKind, text: UiText) {
        eventChannel.send(SettingsEvent.Toast(ToastEvent(kind, text)))
    }

    fun clearUsernameError() {
        if (mutableState.value.usernameError != null) mutableState.update { it.copy(usernameError = null) }
    }

    fun clearEmailError(name: String) {
        mutableState.update { current ->
            if (name in current.emailErrors) current.copy(emailErrors = current.emailErrors - name) else current
        }
    }

    fun clearPasswordError(name: String) {
        mutableState.update { current ->
            if (name in current.passwordErrors) current.copy(passwordErrors = current.passwordErrors - name) else current
        }
    }

    fun saveUsername(username: String) {
        val userId = container.session.userIdOrNull() ?: return
        if (mutableState.value.usernameSaving) return
        mutableState.update { it.copy(usernameSaving = true, usernameError = null) }
        viewModelScope.launch {
            try {
                val user = container.auth.updateUsername(userId, username)
                container.session.userUpdated(user)
                toast(ToastKind.Success, uiText(R.string.settings_toast_profile_saved))
                mutableState.update { it.copy(usernameSaving = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val failure = error.asAppException()
                mutableState.update {
                    it.copy(usernameSaving = false, usernameError = failure.fields["username"] ?: failure.code)
                }
            }
        }
    }

    fun setAvatar(uri: Uri) {
        val userId = container.session.userIdOrNull() ?: return
        if (mutableState.value.avatarBusy) return
        mutableState.update { it.copy(avatarBusy = true) }
        viewModelScope.launch {
            try {
                val encoded = prepareAvatarImage(container.appContext, uri)
                val user = container.auth.updateAvatar(userId, encoded)
                container.session.userUpdated(user)
                toast(ToastKind.Success, uiText(R.string.settings_toast_profile_saved))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                toast(ToastKind.Error, error.asAppException().code.toUiText())
            } finally {
                mutableState.update { it.copy(avatarBusy = false) }
            }
        }
    }

    fun removeAvatar() {
        val userId = container.session.userIdOrNull() ?: return
        if (mutableState.value.avatarBusy) return
        mutableState.update { it.copy(avatarBusy = true) }
        viewModelScope.launch {
            try {
                val user = container.auth.updateAvatar(userId, null)
                container.session.userUpdated(user)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                toast(ToastKind.Error, error.asAppException().code.toUiText())
            } finally {
                mutableState.update { it.copy(avatarBusy = false) }
            }
        }
    }

    fun saveEmail(email: String, currentPassword: String) {
        val userId = container.session.userIdOrNull() ?: return
        if (mutableState.value.emailSaving) return
        mutableState.update { it.copy(emailSaving = true, emailErrors = emptyMap()) }
        viewModelScope.launch {
            try {
                val user = container.auth.updateEmail(userId, email, currentPassword)
                container.session.userUpdated(user)
                mutableState.update { it.copy(emailSaving = false) }
                eventChannel.send(SettingsEvent.EmailSaved)
                toast(ToastKind.Success, uiText(R.string.settings_toast_email_saved))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val failure = error.asAppException()
                val errors = failure.fields.ifEmpty { mapOf("currentPassword" to failure.code) }
                mutableState.update { it.copy(emailSaving = false, emailErrors = errors) }
            }
        }
    }

    fun savePassword(currentPassword: String, newPassword: String) {
        val userId = container.session.userIdOrNull() ?: return
        if (mutableState.value.passwordSaving) return
        mutableState.update { it.copy(passwordSaving = true, passwordErrors = emptyMap()) }
        viewModelScope.launch {
            try {
                container.auth.updatePassword(userId, currentPassword, newPassword)
                mutableState.update { it.copy(passwordSaving = false) }
                eventChannel.send(SettingsEvent.PasswordSaved)
                toast(ToastKind.Success, uiText(R.string.settings_toast_password_saved))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val failure = error.asAppException()
                if (failure.fields.isEmpty()) {
                    mutableState.update { it.copy(passwordSaving = false) }
                    toast(ToastKind.Error, failure.code.toUiText())
                } else {
                    mutableState.update { it.copy(passwordSaving = false, passwordErrors = failure.fields) }
                }
            }
        }
    }

    fun saveApiKey(value: String) {
        container.settings.setApiKey(value)
        viewModelScope.launch { toast(ToastKind.Success, uiText(R.string.settings_toast_api_key_saved)) }
    }
}
