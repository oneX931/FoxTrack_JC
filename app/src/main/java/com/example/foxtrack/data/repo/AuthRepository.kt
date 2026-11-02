package com.example.foxtrack.data.repo

import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import com.example.foxtrack.data.db.FoxDatabase
import com.example.foxtrack.data.local.SettingsStore
import com.example.foxtrack.data.security.PasswordHasher
import com.example.foxtrack.domain.AppException
import com.example.foxtrack.domain.ErrorCode
import com.example.foxtrack.domain.User
import com.example.foxtrack.domain.Validation
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val database: FoxDatabase,
    private val settings: SettingsStore,
) {
    private class UserRow(val user: User, val passwordHash: String)

    suspend fun restoreSession(): User? = withContext(Dispatchers.IO) {
        val id = settings.sessionUserId() ?: return@withContext null
        val user = findById(database.readableDatabase, id)?.user
        if (user == null) settings.clearSession()
        user
    }

    suspend fun register(username: String, email: String, password: String): User {
        val errors = Validation.validateRegister(username, email, password)
        if (errors.isNotEmpty()) throw AppException(ErrorCode.VALIDATION_ERROR, errors)

        val name = username.trim()
        val normalizedEmail = Validation.normalizeEmail(email)
        val usernameKey = usernameKey(name)
        val hash = withContext(Dispatchers.Default) { PasswordHasher.hash(password) }

        return withContext(Dispatchers.IO) {
            val db = database.writableDatabase
            if (findByEmail(db, normalizedEmail) != null) {
                throw AppException(ErrorCode.EMAIL_TAKEN, mapOf("email" to ErrorCode.EMAIL_TAKEN))
            }
            if (findIdByUsernameKey(db, usernameKey) != null) {
                throw AppException(ErrorCode.USERNAME_TAKEN, mapOf("username" to ErrorCode.USERNAME_TAKEN))
            }
            val values = ContentValues().apply {
                put("username", name)
                put("username_key", usernameKey)
                put("email", normalizedEmail)
                put("password_hash", hash)
            }
            val id = try {
                db.insertOrThrow("users", null, values)
            } catch (error: SQLiteConstraintException) {
                throw uniqueViolation(error)
            }
            settings.saveSession(id)
            User(id = id, username = name, email = normalizedEmail, avatar = null)
        }
    }

    suspend fun login(email: String, password: String): User {
        val errors = Validation.validateLogin(email, password)
        if (errors.isNotEmpty()) throw AppException(ErrorCode.VALIDATION_ERROR, errors)

        val row = withContext(Dispatchers.IO) { findByEmail(database.readableDatabase, Validation.normalizeEmail(email)) }
        val valid = withContext(Dispatchers.Default) {
            if (row != null) {
                PasswordHasher.verify(password, row.passwordHash)
            } else {
                PasswordHasher.spendVerificationTime(password)
                false
            }
        }
        if (!valid || row == null) throw AppException(ErrorCode.INVALID_CREDENTIALS)
        settings.saveSession(row.user.id)
        return row.user
    }

    fun logout() {
        settings.clearSession()
    }

    suspend fun updateUsername(userId: Long, username: String): User {
        Validation.checkUsername(username)?.let {
            throw AppException(ErrorCode.VALIDATION_ERROR, mapOf("username" to it))
        }
        val name = username.trim()
        val key = usernameKey(name)
        return withContext(Dispatchers.IO) {
            val db = database.writableDatabase
            val existing = findIdByUsernameKey(db, key)
            if (existing != null && existing != userId) {
                throw AppException(ErrorCode.USERNAME_TAKEN, mapOf("username" to ErrorCode.USERNAME_TAKEN))
            }
            val values = ContentValues().apply {
                put("username", name)
                put("username_key", key)
            }
            try {
                db.update("users", values, "id = ?", arrayOf(userId.toString()))
            } catch (error: SQLiteConstraintException) {
                throw uniqueViolation(error)
            }
            requireUser(db, userId)
        }
    }

    suspend fun updateAvatar(userId: Long, avatar: String?): User = withContext(Dispatchers.IO) {
        val db = database.writableDatabase
        val values = ContentValues()
        values.put("avatar", avatar)
        db.update("users", values, "id = ?", arrayOf(userId.toString()))
        requireUser(db, userId)
    }

    suspend fun updateEmail(userId: Long, email: String, currentPassword: String): User {
        val emailError = Validation.checkEmail(email)
        if (emailError != null) throw AppException(ErrorCode.VALIDATION_ERROR, mapOf("email" to emailError))
        if (currentPassword.isEmpty()) {
            throw AppException(
                ErrorCode.VALIDATION_ERROR,
                mapOf("currentPassword" to ErrorCode.PASSWORD_REQUIRED),
            )
        }
        requireCurrentPassword(userId, currentPassword)

        val normalized = Validation.normalizeEmail(email)
        return withContext(Dispatchers.IO) {
            val db = database.writableDatabase
            val existing = findByEmail(db, normalized)
            if (existing != null && existing.user.id != userId) {
                throw AppException(ErrorCode.EMAIL_TAKEN, mapOf("email" to ErrorCode.EMAIL_TAKEN))
            }
            val values = ContentValues().apply { put("email", normalized) }
            try {
                db.update("users", values, "id = ?", arrayOf(userId.toString()))
            } catch (error: SQLiteConstraintException) {
                throw uniqueViolation(error)
            }
            requireUser(db, userId)
        }
    }

    suspend fun updatePassword(userId: Long, currentPassword: String, newPassword: String) {
        val errors = LinkedHashMap<String, ErrorCode>()
        Validation.checkExistingPassword(currentPassword)?.let { errors["currentPassword"] = it }
        Validation.checkNewPassword(newPassword)?.let { errors["newPassword"] = it }
        if (errors.isNotEmpty()) throw AppException(ErrorCode.VALIDATION_ERROR, errors)

        requireCurrentPassword(userId, currentPassword)
        val hash = withContext(Dispatchers.Default) { PasswordHasher.hash(newPassword) }
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply { put("password_hash", hash) }
            database.writableDatabase.update("users", values, "id = ?", arrayOf(userId.toString()))
        }
    }

    private suspend fun requireCurrentPassword(userId: Long, currentPassword: String) {
        val row = withContext(Dispatchers.IO) { findById(database.readableDatabase, userId) }
            ?: throw AppException(ErrorCode.NOT_FOUND)
        val valid = withContext(Dispatchers.Default) { PasswordHasher.verify(currentPassword, row.passwordHash) }
        if (!valid) {
            throw AppException(ErrorCode.WRONG_PASSWORD, mapOf("currentPassword" to ErrorCode.WRONG_PASSWORD))
        }
    }

    private fun usernameKey(username: String): String =
        Normalizer.normalize(username, Normalizer.Form.NFKC).lowercase(Locale.ROOT)

    private fun uniqueViolation(error: SQLiteConstraintException): AppException {
        val message = error.message.orEmpty()
        return if (message.contains("users.username_key")) {
            AppException(ErrorCode.USERNAME_TAKEN, mapOf("username" to ErrorCode.USERNAME_TAKEN))
        } else {
            AppException(ErrorCode.EMAIL_TAKEN, mapOf("email" to ErrorCode.EMAIL_TAKEN))
        }
    }

    private fun requireUser(db: SQLiteDatabase, id: Long): User =
        findById(db, id)?.user ?: throw AppException(ErrorCode.NOT_FOUND)

    private fun findById(db: SQLiteDatabase, id: Long): UserRow? =
        queryUser(db, "WHERE id = ?", id.toString())

    private fun findByEmail(db: SQLiteDatabase, email: String): UserRow? =
        queryUser(db, "WHERE email = ?", email)

    private fun queryUser(db: SQLiteDatabase, where: String, arg: String): UserRow? =
        db.rawQuery(
            "SELECT id, username, email, avatar, password_hash FROM users $where",
            arrayOf(arg),
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                null
            } else {
                UserRow(
                    user = User(
                        id = cursor.getLong(0),
                        username = cursor.getString(1),
                        email = cursor.getString(2),
                        avatar = if (cursor.isNull(3)) null else cursor.getString(3),
                    ),
                    passwordHash = cursor.getString(4),
                )
            }
        }

    private fun findIdByUsernameKey(db: SQLiteDatabase, key: String): Long? =
        db.rawQuery("SELECT id FROM users WHERE username_key = ?", arrayOf(key)).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
}
