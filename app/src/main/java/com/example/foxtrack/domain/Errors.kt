package com.example.foxtrack.domain

enum class ErrorCode {
    NETWORK,
    HTTP_ERROR,
    VALIDATION_ERROR,
    NOT_FOUND,
    EMAIL_TAKEN,
    USERNAME_TAKEN,
    WRONG_PASSWORD,
    INVALID_CREDENTIALS,
    GAME_ALREADY_IN_LIBRARY,
    GAME_NOT_FOUND,
    TGDB_UNAVAILABLE,
    TGDB_NOT_CONFIGURED,
    TGDB_QUOTA,
    TGDB_AUTH,
    AVATAR_UNREADABLE,
    EMAIL_REQUIRED,
    EMAIL_INVALID,
    USERNAME_REQUIRED,
    USERNAME_MIN,
    USERNAME_MAX,
    USERNAME_CHARS,
    PASSWORD_REQUIRED,
    PASSWORD_MIN,
    PASSWORD_MAX,
    SEARCH_MIN,
    SEARCH_MAX,
    REVIEW_MAX,
    RATING_NOT_COMPLETED,
    REVIEW_NOT_COMPLETED,
    PLATFORM_UNKNOWN,
    PLATFORM_REQUIRED,
    RATING_RANGE,
}

class AppException(
    val code: ErrorCode,
    val fields: Map<String, ErrorCode> = emptyMap(),
    val existingId: Long? = null,
) : Exception(code.name)

fun Throwable.asAppException(): AppException {
    if (this is AppException) return this
    printStackTrace()
    return AppException(ErrorCode.HTTP_ERROR)
}
