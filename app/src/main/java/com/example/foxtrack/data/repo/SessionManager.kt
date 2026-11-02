package com.example.foxtrack.data.repo

import com.example.foxtrack.domain.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val user: User) : SessionState
}

class SessionManager(private val auth: AuthRepository) {
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = mutableState.asStateFlow()

    suspend fun restore() {
        if (mutableState.value !is SessionState.Loading) return
        val user = try {
            auth.restoreSession()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
        if (mutableState.value is SessionState.Loading) {
            mutableState.value = if (user != null) SessionState.SignedIn(user) else SessionState.SignedOut
        }
    }

    fun signedIn(user: User) {
        mutableState.value = SessionState.SignedIn(user)
    }

    fun userUpdated(user: User) {
        if (mutableState.value is SessionState.SignedIn) mutableState.value = SessionState.SignedIn(user)
    }

    fun signOut() {
        auth.logout()
        mutableState.value = SessionState.SignedOut
    }

    fun userIdOrNull(): Long? = (mutableState.value as? SessionState.SignedIn)?.user?.id
}
