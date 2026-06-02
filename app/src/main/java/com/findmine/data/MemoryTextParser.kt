package com.findmine.data

import java.util.Locale

data class ParsedMemoryText(
    val itemName: String = "",
    val location: String = "",
    val note: String = "",
    val tags: String = "",
    val confidence: Float = 0.75f,
)

object MemoryTextParser {
    private val locationClues = listOf(
        "가방",
        "서랍",
        "책상",
        "침대",
        "현관",
        "신발장",
        "포켓",
        "주머니",
        "선반",
        "차",
        "멀티탭",
        "박스",
        "옷장",
    )

    private val itemHints = mapOf(
        "여권" to listOf("여권", "passport", "pass port"),
        "가방" to listOf("가방", "bag", "backpack", "luggage"),
        "이어폰" to listOf("이어폰", "무선이어폰", "에어팟", "airpods", "buds", "버즈"),
        "충전기" to listOf("충전기", "charger", "usb-c", "usb c", "케이블", "어댑터", "electronic device"),
        "지갑" to listOf("지갑", "wallet", "카드지갑"),
        "카드" to listOf("카드", "card", "신용카드", "체크카드"),
        "열쇠" to listOf("열쇠", "키", "key", "차키"),
        "우산" to listOf("우산", "umbrella"),
        "노트북" to listOf("노트북", "laptop", "macbook", "그램"),
        "노트" to listOf("노트", "notebook", "book"),
        "약" to listOf("약", "medicine", "비타민", "영양제"),
    )

    private val locationHints = listOf(
        "검은 가방 안쪽 포켓",
        "책상 왼쪽 서랍",
        "침대 옆 멀티탭",
        "현관 오른쪽 신발장",
        "옷장 안쪽 선반",
        "차 안 콘솔박스",
    )

    private val queryNoise = listOf(
        "내",
        "어디",
        "있어",
        "있지",
        "찾아",
        "찾아줘",
        "보여줘",
        "알려줘",
        "마지막",
        "위치",
        "물건",
        "는",
        "은",
        "이",
        "가",
        "을",
        "를",
        "?",
    )

    fun parseRecordText(raw: String): ParsedMemoryText {
        val cleaned = raw.cleanup()
        if (cleaned.isBlank()) return ParsedMemoryText()

        val words = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return ParsedMemoryText(note = raw)

        val itemWordCount = when {
            words.size >= 2 && words[0] in listOf("무선", "보조", "외장", "검은", "흰색", "작은") -> 2
            else -> 1
        }
        val itemName = words.take(itemWordCount).joinToString(" ")
        val remainder = words.drop(itemWordCount).joinToString(" ")
        val location = remainder
            .replace(Regex("(에|안에|안쪽에)?\\s*(넣음|넣었음|넣어둠|두었음|둠|보관|있음)$"), "")
            .trim()
            .ifBlank { inferLocation(cleaned, itemName) }

        return ParsedMemoryText(
            itemName = itemName,
            location = location,
            note = cleaned,
            tags = buildTags(itemName, location, cleaned),
            confidence = if (location.isNotBlank()) 0.86f else 0.62f,
        )
    }

    fun parseOcrText(raw: String): ParsedMemoryText {
        val cleaned = raw.cleanup()
        if (cleaned.isBlank()) return ParsedMemoryText()

        val lower = cleaned.lowercase(Locale.KOREAN)
        val itemName = itemHints.entries
            .firstOrNull { (_, hints) -> hints.any { lower.contains(it.lowercase(Locale.KOREAN)) } }
            ?.key
            .orEmpty()

        val location = locationHints
            .firstOrNull { hint -> hint.split(" ").any { cleaned.contains(it) } }
            ?: locationClues
                .firstOrNull { cleaned.contains(it) }
                ?.let { clue -> cleaned.windowAround(clue) }
                .orEmpty()

        return ParsedMemoryText(
            itemName = itemName,
            location = location,
            note = if (cleaned.isBlank()) "" else "OCR\n$cleaned",
            tags = buildTags(itemName, location, cleaned),
            confidence = when {
                itemName.isNotBlank() && location.isNotBlank() -> 0.78f
                itemName.isNotBlank() -> 0.68f
                else -> 0.5f
            },
        )
    }

    fun expandQueryTerms(raw: String): List<String> {
        val baseTerms = extractSearchTerm(raw)
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableList()

        val normalized = raw.lowercase(Locale.KOREAN)
        itemHints.forEach { (canonical, aliases) ->
            if (aliases.any { normalized.contains(it.lowercase(Locale.KOREAN)) } || normalized.contains(canonical)) {
                baseTerms += canonical
                baseTerms += aliases
            }
        }

        return baseTerms
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { normalize(it) }
    }

    fun toFtsQuery(raw: String): String =
        expandQueryTerms(raw)
            .map { term ->
                term.replace(Regex("[^0-9A-Za-z가-힣]"), "")
            }
            .filter { it.length >= 2 }
            .distinct()
            .take(8)
            .joinToString(" OR ") { "$it*" }

    fun extractSearchTerm(raw: String): String {
        var normalized = raw.cleanup()
        queryNoise.forEach { token ->
            normalized = normalized.replace(token, " ")
        }
        return normalized
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { raw.cleanup() }
    }

    fun buildTags(vararg parts: String): String {
        val tags = linkedSetOf<String>()
        parts.joinToString(" ")
            .split(Regex("[,\\s]+"))
            .map { it.trim() }
            .filter { it.length >= 2 }
            .forEach { tags += it }

        val joined = parts.joinToString(" ")
        locationClues.filter { joined.contains(it) }.forEach { tags += it }

        return tags.take(12).joinToString(",")
    }

    fun normalize(text: String): String =
        text.lowercase(Locale.KOREAN)
            .replace(Regex("[^0-9a-z가-힣]+"), "")

    fun normalizeForEmbedding(text: String): String =
        text.lowercase(Locale.KOREAN)
            .replace(Regex("[^0-9a-z가-힣\\s]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun inferLocation(cleaned: String, itemName: String): String {
        val withoutItem = cleaned.removePrefix(itemName).trim()
        return withoutItem
    }

    private fun String.windowAround(token: String): String {
        val index = indexOf(token)
        if (index < 0) return token
        val start = (index - 8).coerceAtLeast(0)
        val end = (index + token.length + 14).coerceAtMost(length)
        return substring(start, end)
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun String.cleanup(): String =
        trim()
            .replace("“", "")
            .replace("”", "")
            .replace("\"", "")
            .replace(".", " ")
            .replace(",", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
