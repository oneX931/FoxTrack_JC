package com.example.foxtrack.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val placeholderDates = setOf("1970-01-01", "0000-00-00", "0001-01-01")
private val isoDate = Regex("^\\d{4}-\\d{2}-\\d{2}$")

fun cleanReleaseDate(value: String?): String? {
    if (value == null) return null
    val text = value.trim()
    if (text.isEmpty() || text in placeholderDates) return null
    return text
}

private fun longFormatter(language: String): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.forLanguageTag(language))

fun formatReleaseDate(value: String?, language: String): String? {
    if (value.isNullOrEmpty()) return null
    if (isoDate.matches(value)) {
        return try {
            LocalDate.parse(value).format(longFormatter(language))
        } catch (_: Exception) {
            value
        }
    }
    return value
}

fun formatAddedDate(value: String?, language: String): String? {
    if (value.isNullOrEmpty()) return null
    return try {
        Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDate().format(longFormatter(language))
    } catch (_: Exception) {
        null
    }
}
