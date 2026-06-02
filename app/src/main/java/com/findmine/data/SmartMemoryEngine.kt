package com.findmine.data

data class CarryContext(
    val goingOut: Boolean = true,
    val raining: Boolean = false,
    val scheduleText: String = "",
)

data class SmartSuggestion(
    val record: MemoryRecord,
    val priority: Int,
    val reason: String,
)

data class PhotoMemoryLink(
    val record: MemoryRecord,
    val score: Int,
    val reason: String,
)

object SmartMemoryEngine {
    private val schoolItems = listOf("노트북", "충전기", "이어폰", "지갑", "카드", "학생증", "노트", "필통", "태블릿")
    private val goingOutItems = listOf("지갑", "열쇠", "키", "카드", "이어폰", "우산", "마스크")
    private val rainItems = listOf("우산", "우비", "레인코트")
    private val travelItems = listOf("여권", "지갑", "카드", "충전기", "보조배터리", "이어폰")
    private val workItems = listOf("노트북", "충전기", "사원증", "이어폰", "지갑")

    fun lastSeen(records: List<MemoryRecord>, limit: Int = 8): List<MemoryRecord> =
        records
            .groupBy { MemoryTextParser.normalize(it.itemName) }
            .values
            .mapNotNull { group -> group.maxByOrNull { it.createdAt } }
            .sortedByDescending { it.createdAt }
            .take(limit)

    fun carrySuggestions(
        records: List<MemoryRecord>,
        context: CarryContext,
        now: Long = System.currentTimeMillis(),
        limit: Int = 8,
    ): List<SmartSuggestion> {
        // Score recent items with both user history and current context so reminders stay practical.
        val latest = lastSeen(records, limit = records.size.coerceAtLeast(1))
        val historyCounts = records.groupingBy { MemoryTextParser.normalize(it.itemName) }.eachCount()
        val contextText = MemoryTextParser.normalizeForEmbedding(context.scheduleText)
        val school = contextText.contains("학교") || contextText.contains("수업") || contextText.contains("강의")
        val travel = contextText.contains("여행") || contextText.contains("공항") || contextText.contains("출장")
        val work = contextText.contains("출근") || contextText.contains("회의") || contextText.contains("회사")

        return latest
            .mapNotNull { record ->
                val normalized = MemoryTextParser.normalize(
                    listOf(record.itemName, record.tags, record.note).joinToString(" "),
                )
                val reasons = mutableListOf<String>()
                var score = 0

                if (record.favorite) {
                    score += 24
                    reasons += "자주 잃어버림"
                }

                if (record.searchCount > 0) {
                    score += minOf(record.searchCount * 7, 35)
                    reasons += "최근 ${record.searchCount}회 검색"
                }

                val historyCount = historyCounts[MemoryTextParser.normalize(record.itemName)] ?: 0
                if (historyCount >= 2) {
                    score += minOf(historyCount * 5, 20)
                    reasons += "반복 기록 ${historyCount}회"
                }

                if (context.goingOut && matches(normalized, goingOutItems)) {
                    score += 28
                    reasons += "외출 전 확인"
                }
                if (context.raining && matches(normalized, rainItems)) {
                    score += 72
                    reasons += "비 오는 날 필요"
                }
                if (school && matches(normalized, schoolItems)) {
                    score += 42
                    reasons += "학교 일정"
                }
                if (travel && matches(normalized, travelItems)) {
                    score += 48
                    reasons += "여행/출장 일정"
                }
                if (work && matches(normalized, workItems)) {
                    score += 36
                    reasons += "출근/회의 일정"
                }

                score += recencyScore(record.createdAt, now)

                if (score < 20) return@mapNotNull null

                SmartSuggestion(
                    record = record,
                    priority = score,
                    reason = reasons.distinct().take(3).joinToString(" · ").ifBlank { "최근 위치 기록" },
                )
            }
            .sortedWith(
                compareByDescending<SmartSuggestion> { it.priority }
                    .thenByDescending { it.record.updatedAt },
            )
            .take(limit)
    }

    fun smartBrief(suggestions: List<SmartSuggestion>, context: CarryContext): String {
        val top = suggestions.firstOrNull() ?: return "기록이 쌓이면 외출 전 빠뜨릴 가능성이 높은 물건을 추천합니다."
        val schedule = context.scheduleText.trim()
        val prefix = buildString {
            if (schedule.isNotBlank()) append("오늘 $schedule 일정이 있습니다. ")
            if (context.raining) append("비 컨텍스트가 켜져 있습니다. ")
            if (context.goingOut) append("외출 전 확인 기준으로 ")
        }
        return "${prefix}최근 ${top.record.itemName}은 ${top.record.location}에 기록되어 있습니다."
    }

    fun linkPhotoToRecords(
        analysis: OcrAnalysis,
        records: List<MemoryRecord>,
        limit: Int = 5,
    ): List<PhotoMemoryLink> {
        // Combine OCR text, parsed fields, and labels before matching photos against saved memories.
        val labelText = analysis.labels.joinToString(" ") { it.text }
        val query = listOf(
            analysis.text,
            analysis.parsed.itemName,
            analysis.parsed.location,
            analysis.parsed.tags,
            labelText,
            translateLabels(analysis.labels.map { it.text }).joinToString(" "),
        ).joinToString(" ")

        if (query.isBlank()) return emptyList()

        val queryTerms = MemoryTextParser.expandQueryTerms(query)
            .map { MemoryTextParser.normalize(it) }
            .filter { it.isNotBlank() }

        return records
            .mapNotNull { record ->
                val normalizedRecord = MemoryTextParser.normalize(
                    listOf(record.itemName, record.location, record.note, record.tags).joinToString(" "),
                )
                var score = (LocalTextEmbedding.similarity(query, record) * 70).toInt()
                queryTerms.forEach { term ->
                    if (normalizedRecord.contains(term)) score += 18
                }
                if (analysis.parsed.itemName.isNotBlank() &&
                    MemoryTextParser.normalize(record.itemName).contains(MemoryTextParser.normalize(analysis.parsed.itemName))
                ) {
                    score += 45
                }

                if (score < 18) return@mapNotNull null

                val reason = when {
                    analysis.parsed.itemName.isNotBlank() &&
                        MemoryTextParser.normalize(record.itemName).contains(MemoryTextParser.normalize(analysis.parsed.itemName)) ->
                        "사진에서 추출한 물건명과 연결"
                    queryTerms.any { MemoryTextParser.normalize(record.tags).contains(it) } ->
                        "사진 라벨/태그가 과거 기록과 유사"
                    else -> "사진 내용과 기록 텍스트가 유사"
                }

                PhotoMemoryLink(record = record, score = score, reason = reason)
            }
            .sortedByDescending { it.score }
            .take(limit)
    }

    private fun recencyScore(createdAt: Long, now: Long): Int {
        val days = ((now - createdAt).coerceAtLeast(0L) / 86_400_000L).toInt()
        return when {
            days <= 1 -> 14
            days <= 7 -> 10
            days <= 30 -> 6
            else -> 2
        }
    }

    private fun matches(normalizedRecord: String, candidates: List<String>): Boolean =
        candidates.any { normalizedRecord.contains(MemoryTextParser.normalize(it)) }

    private fun translateLabels(labels: List<String>): List<String> =
        labels.flatMap { label ->
            when (label.lowercase()) {
                "bag", "backpack", "luggage" -> listOf("가방", "여행")
                "electronic device", "gadget" -> listOf("전자기기", "충전기", "이어폰")
                "mobile phone", "telephone" -> listOf("휴대폰", "충전기")
                "computer", "laptop" -> listOf("노트북", "충전기")
                "wallet", "money" -> listOf("지갑", "카드")
                "book", "notebook" -> listOf("노트", "학교")
                "key" -> listOf("열쇠", "키")
                "umbrella" -> listOf("우산", "비")
                else -> listOf(label)
            }
        }
}
