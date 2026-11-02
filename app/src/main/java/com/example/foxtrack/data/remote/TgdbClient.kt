package com.example.foxtrack.data.remote

import com.example.foxtrack.data.db.FoxDatabase
import com.example.foxtrack.domain.AppException
import com.example.foxtrack.domain.Cover
import com.example.foxtrack.domain.ErrorCode
import com.example.foxtrack.domain.TgdbGame
import com.example.foxtrack.domain.cleanReleaseDate
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class TgdbSearchResult(val games: List<TgdbGame>, val hasMore: Boolean)

class TgdbClient(
    private val database: FoxDatabase,
    private val apiKey: () -> String,
    private val baseUrl: String = "https://api.thegamesdb.net/v1",
) {
    private class CachedSearch(val value: TgdbSearchResult, val expiresAt: Long)
    private class Memo(val at: Long, val map: Map<Int, String>)

    private val lock = Any()
    private val searchCache = object : LinkedHashMap<String, CachedSearch>(16, 0.75f, false) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedSearch>?): Boolean =
            size > SEARCH_CACHE_LIMIT
    }
    private val dictionaryMemo = HashMap<String, Memo>()

    suspend fun searchGames(name: String, page: Int = 1): TgdbSearchResult = coroutineScope {
        val cacheKey = "$page:${name.lowercase()}"
        val cached = synchronized(lock) { searchCache[cacheKey] }
        if (cached != null && cached.expiresAt > System.currentTimeMillis()) return@coroutineScope cached.value

        val bodyTask = async {
            request(
                "Games/ByGameName",
                mapOf("name" to name, "page" to page.toString(), "fields" to "platform", "include" to "boxart,platform"),
            )
        }
        val platformsTask = async { platforms() }
        val body = bodyTask.await()
        val platformNames = platformsTask.await()

        val result = TgdbSearchResult(
            games = normalizeGames(body, platformNames),
            hasMore = hasNextPage(body),
        )
        synchronized(lock) {
            searchCache[cacheKey] = CachedSearch(result, System.currentTimeMillis() + SEARCH_TTL_MS)
        }
        result
    }

    suspend fun getGames(ids: List<Long>): List<TgdbGame> = coroutineScope {
        val genresTask = async { genres() }
        val platformsTask = async { platforms() }
        val genres = genresTask.await() ?: throw AppException(ErrorCode.TGDB_UNAVAILABLE)
        val platformNames = platformsTask.await()

        val collected = mutableListOf<TgdbGame>()
        for (page in 1..MAX_DETAIL_PAGES) {
            val body = request(
                "Games/ByGameID",
                mapOf(
                    "id" to ids.joinToString(","),
                    "page" to page.toString(),
                    "fields" to "platform,genres,overview",
                    "include" to "boxart,platform",
                ),
            )
            collected += normalizeGames(body, platformNames)
            if (!hasNextPage(body)) break
        }
        collected.map { game -> game.copy(genres = game.genreIds.mapNotNull { genres[it] }) }
    }

    private suspend fun genres(): Map<Int, String>? = dictionary("genres", "Genres", "genres")

    private suspend fun platforms(): Map<Int, String>? = try {
        dictionary("platforms", "Platforms", "platforms")
    } catch (_: AppException) {
        null
    }

    private suspend fun dictionary(cacheName: String, endpoint: String, dataKey: String): Map<Int, String>? {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            val memo = dictionaryMemo[cacheName]
            if (memo != null && now - memo.at < DICTIONARY_TTL_MS) return memo.map
        }

        val stored = withContext(Dispatchers.IO) { readStored(cacheName) }
        if (stored != null && now - stored.second < DICTIONARY_TTL_MS) {
            val map = parseDictionary(stored.first)
            synchronized(lock) { dictionaryMemo[cacheName] = Memo(stored.second, map) }
            return map
        }

        return try {
            val body = request(endpoint)
            val container = body.optJSONObject("data")?.optJSONObject(dataKey)
            val entries = mutableListOf<Pair<Int, String>>()
            if (container != null) {
                val keys = container.keys()
                while (keys.hasNext()) {
                    val item = container.optJSONObject(keys.next()) ?: continue
                    val id = (item.opt("id") as? Number)?.toInt() ?: continue
                    val itemName = text(item, "name") ?: continue
                    entries += id to itemName
                }
            }
            if (entries.isEmpty()) throw AppException(ErrorCode.TGDB_UNAVAILABLE)
            withContext(Dispatchers.IO) { writeStored(cacheName, entries, now) }
            val map = entries.toMap()
            synchronized(lock) { dictionaryMemo[cacheName] = Memo(now, map) }
            map
        } catch (error: AppException) {
            if (stored == null) throw error
            val map = parseDictionary(stored.first)
            synchronized(lock) { dictionaryMemo[cacheName] = Memo(now - DICTIONARY_TTL_MS / 2, map) }
            map
        }
    }

    private fun readStored(name: String): Pair<String, Long>? =
        database.readableDatabase
            .rawQuery("SELECT payload, fetched_at FROM dictionary_cache WHERE name = ?", arrayOf(name))
            .use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) to cursor.getLong(1) else null }

    private fun writeStored(name: String, entries: List<Pair<Int, String>>, fetchedAt: Long) {
        val payload = JSONArray()
        for ((id, itemName) in entries) payload.put(JSONArray().put(id).put(itemName))
        database.writableDatabase.execSQL(
            "INSERT OR REPLACE INTO dictionary_cache (name, payload, fetched_at) VALUES (?, ?, ?)",
            arrayOf<Any>(name, payload.toString(), fetchedAt),
        )
    }

    private fun parseDictionary(payload: String): Map<Int, String> {
        val result = HashMap<Int, String>()
        val array = try {
            JSONArray(payload)
        } catch (_: JSONException) {
            return result
        }
        for (index in 0 until array.length()) {
            val pair = array.optJSONArray(index) ?: continue
            result[pair.optInt(0)] = pair.optString(1)
        }
        return result
    }

    private suspend fun request(endpoint: String, params: Map<String, String> = emptyMap()): JSONObject =
        withContext(Dispatchers.IO) {
            val key = apiKey().trim()
            if (key.isEmpty()) throw AppException(ErrorCode.TGDB_NOT_CONFIGURED)

            val query = (params + ("apikey" to key)).entries
                .filter { it.value.isNotEmpty() }
                .joinToString("&") { "${encode(it.key)}=${encode(it.value)}" }
            val url = URL("${baseUrl.trimEnd('/')}/$endpoint?$query")

            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("User-Agent", "FoxTrack/1.0")

                val status: Int
                val text: String
                try {
                    status = connection.responseCode
                    val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                    text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                } catch (_: IOException) {
                    throw AppException(ErrorCode.NETWORK)
                }

                val body = try {
                    JSONObject(text)
                } catch (_: JSONException) {
                    null
                }
                val code = body?.optInt("code", status) ?: status
                if (status in 200..299 && code == 200 && body != null) return@withContext body

                val quota = (body?.optLong("allowance_refresh_timer", 0L) ?: 0L) > 0L
                when {
                    code == 429 || status == 429 || quota -> throw AppException(ErrorCode.TGDB_QUOTA)
                    code == 401 || code == 403 || status == 401 || status == 403 ->
                        throw AppException(ErrorCode.TGDB_AUTH)
                    else -> throw AppException(ErrorCode.TGDB_UNAVAILABLE)
                }
            } finally {
                connection.disconnect()
            }
        }

    private fun hasNextPage(body: JSONObject): Boolean {
        val next = body.optJSONObject("pages")?.opt("next") ?: return false
        return next != JSONObject.NULL && next != false && next != ""
    }

    private fun normalizeGames(body: JSONObject, platformNames: Map<Int, String>?): List<TgdbGame> {
        val games = body.optJSONObject("data")?.optJSONArray("games") ?: return emptyList()
        val include = body.optJSONObject("include")
        val platformInclude = include?.optJSONObject("platform")
        val includedPlatforms = platformInclude?.optJSONObject("data") ?: platformInclude
        val boxart = include?.optJSONObject("boxart")

        val result = mutableListOf<TgdbGame>()
        for (index in 0 until games.length()) {
            val game = games.optJSONObject(index) ?: continue
            val id = (game.opt("id") as? Number)?.toLong() ?: continue
            val title = text(game, "game_title") ?: continue
            val platformId = (game.opt("platform") as? Number)?.toInt()
            val resolvedName = platformId?.let { platformNames?.get(it) }
                ?: platformId?.let { text(includedPlatforms?.optJSONObject(it.toString()), "name") }
            val genreIds = mutableListOf<Int>()
            val genreArray = game.optJSONArray("genres")
            if (genreArray != null) {
                for (i in 0 until genreArray.length()) (genreArray.opt(i) as? Number)?.let { genreIds += it.toInt() }
            }
            result += TgdbGame(
                id = id,
                title = title,
                releaseDate = cleanReleaseDate(text(game, "release_date")),
                platformId = platformId,
                platformName = resolvedName ?: "Platform ${platformId ?: "?"}",
                overview = text(game, "overview"),
                genreIds = genreIds,
                cover = pickCover(boxart, id),
            )
        }
        return result
    }

    private fun pickCover(boxart: JSONObject?, gameId: Long): Cover? {
        val images = boxart?.optJSONObject("data")?.optJSONArray(gameId.toString()) ?: return null
        val usable = mutableListOf<JSONObject>()
        for (index in 0 until images.length()) {
            val image = images.optJSONObject(index) ?: continue
            if (text(image, "filename") != null) usable += image
        }
        val image = usable.firstOrNull { it.optString("type") == "boxart" && it.optString("side") == "front" }
            ?: usable.firstOrNull { it.optString("type") == "boxart" }
            ?: usable.firstOrNull()
            ?: return null
        val filename = text(image, "filename") ?: return null
        val base = boxart.optJSONObject("base_url")

        fun build(sizes: List<String>): String {
            for (size in sizes) {
                val root = text(base, size) ?: continue
                val candidate = joinUrl(root, filename)
                if (isHttpUrl(candidate)) return candidate
            }
            return joinUrl(DEFAULT_IMAGE_BASE + sizes.first(), filename)
        }

        return Cover(
            small = build(listOf("small", "thumb", "medium", "original")),
            medium = build(listOf("medium", "large", "small", "original")),
        )
    }

    private fun text(obj: JSONObject?, key: String): String? {
        if (obj == null || obj.isNull(key)) return null
        val value = obj.opt(key) as? String ?: return null
        return value.trim().ifEmpty { null }
    }

    private fun joinUrl(base: String, filename: String): String =
        base.trimEnd('/') + "/" + filename.trimStart('/')

    private fun isHttpUrl(value: String): Boolean = try {
        val scheme = URI(value).scheme
        scheme == "https" || scheme == "http"
    } catch (_: Exception) {
        false
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private companion object {
        const val DEFAULT_IMAGE_BASE = "https://cdn.thegamesdb.net/images/"
        const val DICTIONARY_TTL_MS = 30L * 24 * 60 * 60 * 1000
        const val SEARCH_TTL_MS = 10L * 60 * 1000
        const val SEARCH_CACHE_LIMIT = 200
        const val MAX_DETAIL_PAGES = 3
        const val TIMEOUT_MS = 10_000
    }
}
