package com.example.foxtrack.domain

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

private val editionWords = listOf(
    "edition", "deluxe", "ultra", "ultimate", "super", "mega", "special", "limited",
    "collector'?s?", "platinum", "digital", "standard", "complete", "definitive",
    "remaster(?:ed)?", "goty", "anniversary", "extended", "director'?s? cut", "gold",
    "essentials?", "classics?", "hd", "remake", "enhanced", "premium", "exclusive",
    "bundle", "hits", "value", "budget", "player'?s? choice", "award", "critics'? award",
    "game of the year", "player of the year",
)

private val editionTail = Regex(
    "(?:[\\s\\p{Z}:!,-]+(?:" + editionWords.joinToString("|") + "))+[\\s\\p{Z}]*$",
    RegexOption.IGNORE_CASE,
)
private val bracketNoise = Regex("\\[[^\\]]*\\]")
private val parenNoise = Regex("\\([^)]*\\)")
private val trailingSeparators = Regex("[\\s\\p{Z}:,-]+$")
private val combiningMarks = Regex("\\p{M}+")
private val apostrophes = Regex("['’‘`´]")
private val nonAlphanumeric = Regex("[^\\p{L}\\p{N}]+")
private val yearPrefix = Regex("^(\\d{4})")

private val collator: Collator = Collator.getInstance(Locale.ENGLISH)

fun localeCompare(a: String, b: String): Int = collator.compare(a, b)

fun stripEditionNoise(title: String): String {
    var value = title.replace(bracketNoise, " ").replace(parenNoise, " ").trim()
    for (guard in 0 until 8) {
        val next = value.replace(editionTail, "").trim()
        if (next == value) break
        value = next
    }
    return value.replace(trailingSeparators, "").trim().ifEmpty { title.trim() }
}

fun titleKey(title: String): String {
    val normalized = Normalizer.normalize(stripEditionNoise(title), Normalizer.Form.NFKD)
    return normalized
        .replace(combiningMarks, "")
        .lowercase(Locale.ROOT)
        .replace(apostrophes, "")
        .replace("&", " and ")
        .replace(nonAlphanumeric, " ")
        .trim()
}

fun yearOf(date: String?): Int? {
    val match = yearPrefix.find(date ?: "") ?: return null
    return match.groupValues[1].toInt()
}

fun compareReleases(a: TgdbGame, b: TgdbGame): Int {
    val aHas = !a.releaseDate.isNullOrEmpty()
    val bHas = !b.releaseDate.isNullOrEmpty()
    if (aHas != bHas) return if (aHas) -1 else 1
    if (aHas && bHas && a.releaseDate != b.releaseDate) {
        return if (a.releaseDate!! < b.releaseDate!!) -1 else 1
    }
    return 0
}

private val byReleaseThenId = Comparator<TgdbGame> { a, b ->
    val release = compareReleases(a, b)
    if (release != 0) release else a.id.compareTo(b.id)
}

private val byCleanestTitle = Comparator<TgdbGame> { a, b ->
    val length = a.title.length - b.title.length
    if (length != 0) length else byReleaseThenId.compare(a, b)
}

fun buildGroup(key: String, entries: List<TgdbGame>): SearchGroup {
    val ordered = entries.sortedWith(byReleaseThenId)
    val perPlatform = LinkedHashMap<String, TgdbGame>()
    for (entry in ordered) {
        if (!perPlatform.containsKey(entry.platformName)) perPlatform[entry.platformName] = entry
    }
    val platforms = perPlatform.values
        .sortedWith { a, b ->
            val release = compareReleases(a, b)
            if (release != 0) release else localeCompare(a.platformName, b.platformName)
        }
        .map { PlatformEntry(gameId = it.id, name = it.platformName, year = yearOf(it.releaseDate)) }
    val primary = entries.sortedWith(byCleanestTitle).first()
    val withCover = ordered.firstOrNull { !it.cover?.small.isNullOrEmpty() }
    return SearchGroup(
        key = key,
        title = primary.title,
        year = yearOf(ordered.first().releaseDate),
        cover = withCover?.cover?.small,
        platforms = platforms,
    )
}

fun groupGames(games: List<TgdbGame>): List<SearchGroup> {
    val buckets = LinkedHashMap<String, MutableList<TgdbGame>>()
    for (game in games) {
        val key = titleKey(game.title)
        if (key.isEmpty()) continue
        buckets.getOrPut(key) { mutableListOf() }.add(game)
    }
    return buckets.map { (key, entries) -> buildGroup(key, entries) }
}

fun compareReleasesThenId(a: TgdbGame, b: TgdbGame): Int = byReleaseThenId.compare(a, b)

private fun byRelease(a: PlatformEntry, b: PlatformEntry): Int {
    val year = (a.year ?: 9999) - (b.year ?: 9999)
    return if (year != 0) year else localeCompare(a.name, b.name)
}

fun mergeGroups(current: List<SearchGroup>, incoming: List<SearchGroup>): List<SearchGroup> {
    val merged = LinkedHashMap<String, SearchGroup>()
    for (group in current) merged[group.key] = group

    for (group in incoming) {
        val existing = merged[group.key]
        if (existing == null) {
            merged[group.key] = group
            continue
        }
        val platforms = existing.platforms.toMutableList()
        for (platform in group.platforms) {
            val index = platforms.indexOfFirst { it.name == platform.name }
            if (index == -1) platforms.add(platform)
            else if (platform.gameId < platforms[index].gameId) platforms[index] = platform
        }
        platforms.sortWith { a, b -> byRelease(a, b) }
        val year = minOf(existing.year ?: 9999, group.year ?: 9999).let { if (it == 9999) null else it }
        merged[group.key] = existing.copy(
            platforms = platforms,
            year = year,
            cover = existing.cover ?: group.cover,
            libraryGameId = existing.libraryGameId ?: group.libraryGameId,
        )
    }
    return merged.values.toList()
}
