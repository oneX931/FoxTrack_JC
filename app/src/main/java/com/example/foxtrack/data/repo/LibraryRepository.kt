package com.example.foxtrack.data.repo

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import com.example.foxtrack.data.db.FoxDatabase
import com.example.foxtrack.data.remote.TgdbClient
import com.example.foxtrack.domain.AppException
import com.example.foxtrack.domain.ErrorCode
import com.example.foxtrack.domain.GameDetail
import com.example.foxtrack.domain.GameStatus
import com.example.foxtrack.domain.LibraryCard
import com.example.foxtrack.domain.LibraryListing
import com.example.foxtrack.domain.Validation
import com.example.foxtrack.domain.buildGroup
import com.example.foxtrack.domain.cleanReleaseDate
import com.example.foxtrack.domain.compareReleasesThenId
import com.example.foxtrack.domain.localeCompare
import com.example.foxtrack.domain.titleKey
import java.util.EnumMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException

class LibraryRepository(
    private val database: FoxDatabase,
    private val tgdb: TgdbClient,
) {
    private val versionState = MutableStateFlow(0)
    val version: StateFlow<Int> = versionState.asStateFlow()

    private class Row(
        val id: Long,
        val title: String,
        val titleKey: String,
        val coverUrl: String?,
        val selectedPlatforms: List<String>,
        val status: GameStatus,
        val rating: Int?,
    )

    suspend fun list(userId: Long, query: String, status: GameStatus?, platform: String?): LibraryListing =
        withContext(Dispatchers.IO) {
            val rows = database.readableDatabase.rawQuery(
                """
                SELECT id, title, title_key, cover_url, selected_platforms, status, rating
                  FROM user_games
                 WHERE user_id = ?
                 ORDER BY added_at DESC, id DESC
                """.trimIndent(),
                arrayOf(userId.toString()),
            ).use { cursor ->
                val list = mutableListOf<Row>()
                while (cursor.moveToNext()) {
                    list += Row(
                        id = cursor.getLong(0),
                        title = cursor.getString(1),
                        titleKey = cursor.getString(2),
                        coverUrl = cursor.nullableString(3),
                        selectedPlatforms = parseList(cursor.getString(4)),
                        status = GameStatus.fromDb(cursor.getString(5)),
                        rating = if (cursor.isNull(6)) null else cursor.getInt(6),
                    )
                }
                list
            }

            val counts = EnumMap<GameStatus, Int>(GameStatus::class.java)
            GameStatus.entries.forEach { counts[it] = 0 }
            for (row in rows) counts[row.status] = (counts[row.status] ?: 0) + 1

            val queryKey = if (query.isBlank()) "" else titleKey(query)
            val filtered = rows.filter { row ->
                (queryKey.isEmpty() || row.titleKey.contains(queryKey)) &&
                    (status == null || row.status == status) &&
                    (platform.isNullOrEmpty() || platform in row.selectedPlatforms)
            }

            LibraryListing(
                items = filtered.map {
                    LibraryCard(
                        id = it.id,
                        title = it.title,
                        coverUrl = it.coverUrl,
                        selectedPlatforms = it.selectedPlatforms,
                        status = it.status,
                        rating = if (it.status == GameStatus.DONE) it.rating else null,
                    )
                },
                total = filtered.size,
                platforms = rows.flatMap { it.selectedPlatforms }.distinct()
                    .sortedWith { a, b -> a.compareTo(b, ignoreCase = true) },
                counts = counts,
                allCount = rows.size,
            )
        }

    suspend fun get(userId: Long, id: Long): GameDetail = withContext(Dispatchers.IO) {
        readDetail(database.readableDatabase, userId, id) ?: throw AppException(ErrorCode.NOT_FOUND)
    }

    suspend fun add(userId: Long, gameIds: List<Long>, selectedGameIds: List<Long>): GameDetail {
        val ids = gameIds.distinct()
        val selectedIds = selectedGameIds.distinct()
        if (ids.isEmpty() || selectedIds.isEmpty() || !ids.containsAll(selectedIds)) {
            throw AppException(ErrorCode.VALIDATION_ERROR, mapOf("selectedGameIds" to ErrorCode.PLATFORM_REQUIRED))
        }

        val known = withContext(Dispatchers.IO) { findByTgdbIds(userId, ids) }
        if (known != null) throw AppException(ErrorCode.GAME_ALREADY_IN_LIBRARY, existingId = known)

        val games = tgdb.getGames(ids)
        val selected = games.filter { it.id in selectedIds }
        if (selected.size != selectedIds.size) throw AppException(ErrorCode.GAME_NOT_FOUND)

        val key = titleKey(selected.first().title)
        val related = games.filter { titleKey(it.title) == key }
        val duplicate = withContext(Dispatchers.IO) { findByTitleKey(userId, key) }
        if (duplicate != null) throw AppException(ErrorCode.GAME_ALREADY_IN_LIBRARY, existingId = duplicate)

        val group = buildGroup(key, related)
        val primary = selected.sortedWith { a, b -> compareReleasesThenId(a, b) }.first()
        val ordered = related.sortedWith { a, b -> compareReleasesThenId(a, b) }
        val genres = primary.genres.ifEmpty { ordered.firstOrNull { it.genres.isNotEmpty() }?.genres ?: emptyList() }
        val selectedPlatforms = selected.map { it.platformName }.distinct().sortedWith { a, b -> localeCompare(a, b) }

        val values = ContentValues().apply {
            put("user_id", userId)
            put("thegamesdb_id", primary.id)
            put("title_key", key)
            put("title", group.title)
            put("cover_url", primary.cover?.medium ?: ordered.firstOrNull { it.cover != null }?.cover?.medium)
            put("description", primary.overview ?: ordered.firstOrNull { it.overview != null }?.overview)
            put("release_date", primary.releaseDate ?: ordered.firstOrNull { it.releaseDate != null }?.releaseDate)
            put("genres", JSONArray(genres).toString())
            put("platforms", JSONArray(group.platforms.map { it.name }).toString())
            put("selected_platforms", JSONArray(selectedPlatforms).toString())
        }

        val detail = withContext(Dispatchers.IO) {
            val db = database.writableDatabase
            val id = try {
                db.insertOrThrow("user_games", null, values)
            } catch (_: SQLiteConstraintException) {
                throw AppException(ErrorCode.GAME_ALREADY_IN_LIBRARY, existingId = findByTitleKey(userId, key))
            }
            readDetail(db, userId, id) ?: throw AppException(ErrorCode.NOT_FOUND)
        }
        versionState.update { it + 1 }
        return detail
    }

    suspend fun updateStatus(userId: Long, id: Long, status: GameStatus): GameDetail =
        mutate(userId, id) { db, _ ->
            val values = ContentValues().apply { put("status", status.dbValue) }
            db.update("user_games", values, "id = ? AND user_id = ?", arrayOf(id.toString(), userId.toString()))
        }

    suspend fun updatePlatforms(userId: Long, id: Long, selectedPlatforms: List<String>): GameDetail =
        mutate(userId, id) { db, current ->
            if (selectedPlatforms.isEmpty()) {
                throw AppException(ErrorCode.VALIDATION_ERROR, mapOf("selectedPlatforms" to ErrorCode.PLATFORM_REQUIRED))
            }
            if (selectedPlatforms.any { it !in current.platforms }) {
                throw AppException(ErrorCode.VALIDATION_ERROR, mapOf("selectedPlatforms" to ErrorCode.PLATFORM_UNKNOWN))
            }
            val unique = selectedPlatforms.distinct().sortedWith { a, b -> localeCompare(a, b) }
            val values = ContentValues().apply { put("selected_platforms", JSONArray(unique).toString()) }
            db.update("user_games", values, "id = ? AND user_id = ?", arrayOf(id.toString(), userId.toString()))
        }

    suspend fun updateRating(userId: Long, id: Long, rating: Int?): GameDetail =
        mutate(userId, id) { db, current ->
            if (rating != null && rating !in 1..10) {
                throw AppException(ErrorCode.VALIDATION_ERROR, mapOf("rating" to ErrorCode.RATING_RANGE))
            }
            if (rating != null && current.status != GameStatus.DONE) {
                throw AppException(ErrorCode.VALIDATION_ERROR, mapOf("rating" to ErrorCode.RATING_NOT_COMPLETED))
            }
            val values = ContentValues()
            if (rating == null) values.putNull("rating") else values.put("rating", rating)
            db.update("user_games", values, "id = ? AND user_id = ?", arrayOf(id.toString(), userId.toString()))
        }

    suspend fun updateReview(userId: Long, id: Long, review: String?): GameDetail =
        mutate(userId, id) { db, current ->
            val text = review?.trim().orEmpty()
            if (Validation.countCharacters(text) > Validation.REVIEW_MAX_LENGTH) {
                throw AppException(ErrorCode.VALIDATION_ERROR, mapOf("review" to ErrorCode.REVIEW_MAX))
            }
            if (text.isNotEmpty() && current.status != GameStatus.DONE) {
                throw AppException(ErrorCode.VALIDATION_ERROR, mapOf("review" to ErrorCode.REVIEW_NOT_COMPLETED))
            }
            val values = ContentValues()
            if (text.isEmpty()) values.putNull("review") else values.put("review", text)
            db.update("user_games", values, "id = ? AND user_id = ?", arrayOf(id.toString(), userId.toString()))
        }

    suspend fun remove(userId: Long, id: Long) {
        withContext(Dispatchers.IO) {
            val removed = database.writableDatabase.delete(
                "user_games",
                "id = ? AND user_id = ?",
                arrayOf(id.toString(), userId.toString()),
            )
            if (removed == 0) throw AppException(ErrorCode.NOT_FOUND)
        }
        versionState.update { it + 1 }
    }

    private suspend fun mutate(
        userId: Long,
        id: Long,
        change: (SQLiteDatabase, GameDetail) -> Unit,
    ): GameDetail {
        val detail = withContext(Dispatchers.IO) {
            val db = database.writableDatabase
            val current = readDetail(db, userId, id) ?: throw AppException(ErrorCode.NOT_FOUND)
            change(db, current)
            readDetail(db, userId, id) ?: throw AppException(ErrorCode.NOT_FOUND)
        }
        versionState.update { it + 1 }
        return detail
    }

    private fun readDetail(db: SQLiteDatabase, userId: Long, id: Long): GameDetail? =
        db.rawQuery(
            """
            SELECT id, thegamesdb_id, title, cover_url, description, release_date, genres, platforms,
                   selected_platforms, status, rating, review, added_at
              FROM user_games
             WHERE id = ? AND user_id = ?
            """.trimIndent(),
            arrayOf(id.toString(), userId.toString()),
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                null
            } else {
                val status = GameStatus.fromDb(cursor.getString(9))
                val completed = status == GameStatus.DONE
                GameDetail(
                    id = cursor.getLong(0),
                    tgdbId = cursor.getLong(1),
                    title = cursor.getString(2),
                    coverUrl = cursor.nullableString(3),
                    description = cursor.nullableString(4),
                    releaseDate = cleanReleaseDate(cursor.nullableString(5)),
                    genres = parseList(cursor.getString(6)),
                    platforms = parseList(cursor.getString(7)),
                    selectedPlatforms = parseList(cursor.getString(8)),
                    status = status,
                    rating = if (completed && !cursor.isNull(10)) cursor.getInt(10) else null,
                    review = if (completed) cursor.nullableString(11) else null,
                    addedAt = cursor.getString(12),
                )
            }
        }

    private fun findByTgdbIds(userId: Long, ids: List<Long>): Long? {
        val placeholders = ids.joinToString(", ") { "?" }
        val args = arrayOf(userId.toString()) + ids.map { it.toString() }
        return database.readableDatabase.rawQuery(
            "SELECT id FROM user_games WHERE user_id = ? AND thegamesdb_id IN ($placeholders)",
            args,
        ).use { if (it.moveToFirst()) it.getLong(0) else null }
    }

    private fun findByTitleKey(userId: Long, key: String): Long? =
        database.readableDatabase.rawQuery(
            "SELECT id FROM user_games WHERE user_id = ? AND title_key = ?",
            arrayOf(userId.toString(), key),
        ).use { if (it.moveToFirst()) it.getLong(0) else null }

    private fun Cursor.nullableString(index: Int): String? = if (isNull(index)) null else getString(index)

    private fun parseList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return try {
            val array = JSONArray(value)
            (0 until array.length()).map { array.getString(it) }
        } catch (_: JSONException) {
            emptyList()
        }
    }
}
