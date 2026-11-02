package com.example.foxtrack.domain

enum class PlatformIconKind { MONITOR, SMARTPHONE, JOYSTICK, GAMEPAD }

private class Family(val id: String, val pattern: Regex, val color: Long)

private val families = listOf(
    Family("playstation", Regex("playstation|\\bpsp\\b|\\bvita\\b|\\bps[1-5]\\b", RegexOption.IGNORE_CASE), 0xFF2C6FD1),
    Family("xbox", Regex("\\bxbox\\b", RegexOption.IGNORE_CASE), 0xFF5B8F3B),
    Family(
        "nintendo",
        Regex(
            "nintendo|\\bwii\\b|\\bswitch\\b|game ?boy|gamecube|\\bnes\\b|\\bsnes\\b|\\b3ds\\b|\\bds\\b|virtual boy",
            RegexOption.IGNORE_CASE,
        ),
        0xFFD1364A,
    ),
    Family("sega", Regex("sega|dreamcast|\\bsaturn\\b|genesis|game gear|master system", RegexOption.IGNORE_CASE), 0xFF3C53C7),
    Family("mobile", Regex("mobile|android|\\bios\\b|iphone|ipad", RegexOption.IGNORE_CASE), 0xFF8B5CF6),
    Family("arcade", Regex("arcade", RegexOption.IGNORE_CASE), 0xFFC9822A),
    Family("pc", Regex("\\bpc\\b|windows|linux|\\bmac\\b|steam", RegexOption.IGNORE_CASE), 0xFF6B7280),
)

private fun familyOf(name: String?): Family? = families.firstOrNull { it.pattern.containsMatchIn(name ?: "") }

fun platformFamily(name: String?): String = familyOf(name)?.id ?: "other"

fun platformColor(name: String?): Long = familyOf(name)?.color ?: 0xFF8A7A5C

fun platformIconKind(name: String?): PlatformIconKind = when (platformFamily(name)) {
    "pc" -> PlatformIconKind.MONITOR
    "mobile" -> PlatformIconKind.SMARTPHONE
    "arcade" -> PlatformIconKind.JOYSTICK
    else -> PlatformIconKind.GAMEPAD
}

private val genreLabelsRu = mapOf(
    "Action" to "Экшен",
    "Adventure" to "Приключения",
    "Construction and Management Simulation" to "Строительство и управление",
    "Role-Playing" to "Ролевая игра",
    "Puzzle" to "Головоломка",
    "Strategy" to "Стратегия",
    "Racing" to "Гонки",
    "Shooter" to "Шутер",
    "Life Simulation" to "Симулятор жизни",
    "Fighting" to "Файтинг",
    "Sports" to "Спорт",
    "Sandbox" to "Песочница",
    "Flight Simulator" to "Авиасимулятор",
    "Platform" to "Платформер",
    "Stealth" to "Стелс",
    "Music" to "Музыкальная",
    "Horror" to "Хоррор",
    "Vehicle Simulation" to "Симулятор транспорта",
    "Board" to "Настольная",
    "Education" to "Обучающая",
    "Family" to "Семейная",
    "Party" to "Вечеринка",
    "Quiz" to "Викторина",
)

fun genreLabel(name: String, language: String): String =
    if (language == "en") name else genreLabelsRu[name] ?: name
