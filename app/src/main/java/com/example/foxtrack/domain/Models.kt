package com.example.foxtrack.domain

data class Cover(val small: String, val medium: String)

data class TgdbGame(
    val id: Long,
    val title: String,
    val releaseDate: String?,
    val platformId: Int?,
    val platformName: String,
    val overview: String?,
    val genreIds: List<Int>,
    val cover: Cover?,
    val genres: List<String> = emptyList(),
)

data class PlatformEntry(
    val gameId: Long,
    val name: String,
    val year: Int?,
)

data class SearchGroup(
    val key: String,
    val title: String,
    val year: Int?,
    val cover: String?,
    val platforms: List<PlatformEntry>,
    val libraryGameId: Long? = null,
)

enum class GameStatus(val dbValue: String) {
    WISH("Хочу пройти"),
    PLAYING("Играю"),
    DONE("Пройдена");

    companion object {
        fun fromDb(value: String): GameStatus = entries.firstOrNull { it.dbValue == value } ?: WISH
    }
}

data class User(
    val id: Long,
    val username: String,
    val email: String,
    val avatar: String?,
)

data class LibraryCard(
    val id: Long,
    val title: String,
    val coverUrl: String?,
    val selectedPlatforms: List<String>,
    val status: GameStatus,
    val rating: Int?,
)

data class LibraryListing(
    val items: List<LibraryCard>,
    val total: Int,
    val platforms: List<String>,
    val counts: Map<GameStatus, Int>,
    val allCount: Int,
)

data class GameDetail(
    val id: Long,
    val tgdbId: Long,
    val title: String,
    val coverUrl: String?,
    val description: String?,
    val releaseDate: String?,
    val genres: List<String>,
    val platforms: List<String>,
    val selectedPlatforms: List<String>,
    val status: GameStatus,
    val rating: Int?,
    val review: String?,
    val addedAt: String,
)

data class SearchPage(
    val query: String,
    val page: Int,
    val hasMore: Boolean,
    val results: List<SearchGroup>,
)
