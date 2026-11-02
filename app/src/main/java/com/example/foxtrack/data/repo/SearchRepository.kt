package com.example.foxtrack.data.repo

import com.example.foxtrack.data.db.FoxDatabase
import com.example.foxtrack.data.remote.TgdbClient
import com.example.foxtrack.domain.AppException
import com.example.foxtrack.domain.ErrorCode
import com.example.foxtrack.domain.SearchPage
import com.example.foxtrack.domain.Validation
import com.example.foxtrack.domain.groupGames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchRepository(
    private val database: FoxDatabase,
    private val tgdb: TgdbClient,
) {
    suspend fun search(userId: Long, query: String, page: Int): SearchPage {
        val text = query.trim()
        Validation.checkSearch(text)?.let {
            throw AppException(ErrorCode.VALIDATION_ERROR, mapOf("q" to it))
        }
        val result = tgdb.searchGames(text, page)
        val owned = withContext(Dispatchers.IO) { ownedTitles(userId) }
        return SearchPage(
            query = text,
            page = page,
            hasMore = result.hasMore,
            results = groupGames(result.games).map { it.copy(libraryGameId = owned[it.key]) },
        )
    }

    private fun ownedTitles(userId: Long): Map<String, Long> =
        database.readableDatabase.rawQuery(
            "SELECT id, title_key FROM user_games WHERE user_id = ?",
            arrayOf(userId.toString()),
        ).use { cursor ->
            val map = HashMap<String, Long>()
            while (cursor.moveToNext()) map[cursor.getString(1)] = cursor.getLong(0)
            map
        }
}
