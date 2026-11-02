package com.example.foxtrack.ui.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.foxtrack.R
import com.example.foxtrack.domain.ErrorCode
import com.example.foxtrack.domain.GameStatus
import com.example.foxtrack.domain.Validation

sealed interface UiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Raw(val value: String) : UiText

    fun resolve(context: Context): String = when (this) {
        is Raw -> value
        is Res -> {
            if (args.isEmpty()) {
                context.getString(id)
            } else {
                val resolved = args.map { if (it is UiText) it.resolve(context) else it }
                context.getString(id, *resolved.toTypedArray())
            }
        }
    }
}

fun uiText(@StringRes id: Int, vararg args: Any): UiText = UiText.Res(id, args.toList())

@Composable
fun UiText.asString(): String = resolve(LocalContext.current)

enum class ToastKind { Success, Error, Info }

data class ToastEvent(
    val kind: ToastKind,
    val text: UiText,
    val actionLabel: UiText? = null,
    val actionKey: String? = null,
)

@StringRes
fun GameStatus.labelRes(): Int = when (this) {
    GameStatus.WISH -> R.string.status_wish
    GameStatus.PLAYING -> R.string.status_playing
    GameStatus.DONE -> R.string.status_done
}

fun ErrorCode.toUiText(): UiText = when (this) {
    ErrorCode.NETWORK -> uiText(R.string.api_error_network)
    ErrorCode.HTTP_ERROR -> uiText(R.string.api_error_http_error)
    ErrorCode.VALIDATION_ERROR -> uiText(R.string.api_error_validation_error)
    ErrorCode.NOT_FOUND -> uiText(R.string.api_error_not_found)
    ErrorCode.EMAIL_TAKEN -> uiText(R.string.api_error_email_taken)
    ErrorCode.USERNAME_TAKEN -> uiText(R.string.api_error_username_taken)
    ErrorCode.WRONG_PASSWORD -> uiText(R.string.api_error_wrong_password)
    ErrorCode.INVALID_CREDENTIALS -> uiText(R.string.api_error_invalid_credentials)
    ErrorCode.GAME_ALREADY_IN_LIBRARY -> uiText(R.string.api_error_game_already_in_library)
    ErrorCode.GAME_NOT_FOUND -> uiText(R.string.api_error_game_not_found)
    ErrorCode.TGDB_UNAVAILABLE -> uiText(R.string.api_error_tgdb_unavailable)
    ErrorCode.TGDB_NOT_CONFIGURED -> uiText(R.string.api_error_tgdb_not_configured)
    ErrorCode.TGDB_QUOTA -> uiText(R.string.api_error_tgdb_quota)
    ErrorCode.TGDB_AUTH -> uiText(R.string.api_error_tgdb_auth)
    ErrorCode.AVATAR_UNREADABLE -> uiText(R.string.api_error_avatar_unreadable)
    ErrorCode.EMAIL_REQUIRED -> uiText(R.string.validation_email_required)
    ErrorCode.EMAIL_INVALID -> uiText(R.string.validation_email_invalid)
    ErrorCode.USERNAME_REQUIRED -> uiText(R.string.validation_username_required)
    ErrorCode.USERNAME_MIN -> uiText(R.string.validation_username_min)
    ErrorCode.USERNAME_MAX -> uiText(R.string.validation_username_max)
    ErrorCode.USERNAME_CHARS -> uiText(R.string.validation_username_chars)
    ErrorCode.PASSWORD_REQUIRED -> uiText(R.string.validation_password_required)
    ErrorCode.PASSWORD_MIN -> uiText(R.string.validation_password_min)
    ErrorCode.PASSWORD_MAX -> uiText(R.string.validation_password_max)
    ErrorCode.SEARCH_MIN -> uiText(R.string.validation_search_min)
    ErrorCode.SEARCH_MAX -> uiText(R.string.validation_search_max)
    ErrorCode.REVIEW_MAX -> uiText(R.string.validation_review_max, Validation.REVIEW_MAX_LENGTH)
    ErrorCode.RATING_NOT_COMPLETED -> uiText(R.string.api_error_validation_error)
    ErrorCode.REVIEW_NOT_COMPLETED -> uiText(R.string.api_error_validation_error)
    ErrorCode.PLATFORM_UNKNOWN -> uiText(R.string.api_error_platform_unknown)
    ErrorCode.PLATFORM_REQUIRED -> uiText(R.string.add_dialog_select_at_least_one)
    ErrorCode.RATING_RANGE -> uiText(R.string.api_error_rating_range)
}

@Composable
fun ErrorCode.asString(): String = toUiText().asString()
