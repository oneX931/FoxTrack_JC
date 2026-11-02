package com.example.foxtrack.domain

object Validation {
    const val REVIEW_MAX_LENGTH = 1000
    const val USERNAME_MIN_LENGTH = 3
    const val USERNAME_MAX_LENGTH = 30
    const val PASSWORD_MIN_LENGTH = 8
    const val PASSWORD_MAX_LENGTH = 128

    private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")
    private val usernamePattern = Regex("^[\\p{L}\\p{N}._-]+$")

    fun countCharacters(value: String): Int = value.codePointCount(0, value.length)

    fun normalizeEmail(email: String): String = email.trim().lowercase()

    fun checkEmail(email: String): ErrorCode? {
        val value = email.trim()
        return when {
            value.isEmpty() -> ErrorCode.EMAIL_REQUIRED
            value.length > 254 || !emailPattern.matches(value) -> ErrorCode.EMAIL_INVALID
            else -> null
        }
    }

    fun checkUsername(username: String): ErrorCode? {
        val name = username.trim()
        return when {
            name.isEmpty() -> ErrorCode.USERNAME_REQUIRED
            name.length < USERNAME_MIN_LENGTH -> ErrorCode.USERNAME_MIN
            name.length > USERNAME_MAX_LENGTH -> ErrorCode.USERNAME_MAX
            !usernamePattern.matches(name) -> ErrorCode.USERNAME_CHARS
            else -> null
        }
    }

    fun checkNewPassword(password: String): ErrorCode? = when {
        password.isEmpty() -> ErrorCode.PASSWORD_REQUIRED
        password.length < PASSWORD_MIN_LENGTH -> ErrorCode.PASSWORD_MIN
        password.length > PASSWORD_MAX_LENGTH -> ErrorCode.PASSWORD_MAX
        else -> null
    }

    fun checkExistingPassword(password: String): ErrorCode? = when {
        password.isEmpty() -> ErrorCode.PASSWORD_REQUIRED
        password.length > PASSWORD_MAX_LENGTH -> ErrorCode.PASSWORD_MAX
        else -> null
    }

    fun validateRegister(username: String, email: String, password: String): Map<String, ErrorCode> {
        val errors = LinkedHashMap<String, ErrorCode>()
        checkUsername(username)?.let { errors["username"] = it }
        checkEmail(email)?.let { errors["email"] = it }
        checkNewPassword(password)?.let { errors["password"] = it }
        return errors
    }

    fun validateLogin(email: String, password: String): Map<String, ErrorCode> {
        val errors = LinkedHashMap<String, ErrorCode>()
        checkEmail(email)?.let { errors["email"] = it }
        checkExistingPassword(password)?.let { errors["password"] = it }
        return errors
    }

    fun checkSearch(query: String): ErrorCode? {
        val value = query.trim()
        return when {
            value.length < 2 -> ErrorCode.SEARCH_MIN
            value.length > 100 -> ErrorCode.SEARCH_MAX
            else -> null
        }
    }

    fun checkReview(value: String): ErrorCode? =
        if (countCharacters(value.trim()) > REVIEW_MAX_LENGTH) ErrorCode.REVIEW_MAX else null
}
