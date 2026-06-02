package com.findmine.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.findmine.data.FindMineDatabase
import com.findmine.data.CarryContext
import com.findmine.data.ImageTextRecognizer
import com.findmine.data.ImageLabelCandidate
import com.findmine.data.LocalTextEmbedding
import com.findmine.data.MemoryRecord
import com.findmine.data.MemoryRepository
import com.findmine.data.MemoryTextParser
import com.findmine.data.OcrAnalysis
import com.findmine.data.PhotoMemoryLink
import com.findmine.data.SmartMemoryEngine
import com.findmine.data.SmartSuggestion
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale
import kotlin.math.max

enum class AppSection {
    Search,
    Add,
    Alerts,
}

data class MemoryDraft(
    val itemName: String = "",
    val location: String = "",
    val note: String = "",
    val tags: String = "",
    val imageUri: String? = null,
    val favorite: Boolean = false,
    val confidence: Float = 1f,
) {
    val canSave: Boolean
        get() = itemName.isNotBlank() && location.isNotBlank()
}

data class ScoredMemory(
    val record: MemoryRecord,
    val score: Int,
)

data class OcrUiState(
    val running: Boolean = false,
    val text: String = "",
    val labels: List<ImageLabelCandidate> = emptyList(),
    val links: List<PhotoMemoryLink> = emptyList(),
    val message: String = "",
    val error: String = "",
)

class FindMineViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = MemoryRepository(
        FindMineDatabase.get(application).memoryDao(),
    )
    private val imageTextRecognizer = ImageTextRecognizer(application.applicationContext)

    private val collator = Collator.getInstance(Locale.KOREAN)
    private var ftsSearchJob: Job? = null

    private val _section = MutableStateFlow(AppSection.Search)
    val section: StateFlow<AppSection> = _section

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _ftsIds = MutableStateFlow<List<Long>>(emptyList())

    private val _draft = MutableStateFlow(MemoryDraft())
    val draft: StateFlow<MemoryDraft> = _draft

    private val _ocrState = MutableStateFlow(OcrUiState())
    val ocrState: StateFlow<OcrUiState> = _ocrState

    private val _carryContext = MutableStateFlow(CarryContext())
    val carryContext: StateFlow<CarryContext> = _carryContext

    private val _lastSavedItem = MutableStateFlow<String?>(null)
    val lastSavedItem: StateFlow<String?> = _lastSavedItem

    val records: StateFlow<List<MemoryRecord>> =
        repository.records.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    val searchResults: StateFlow<List<ScoredMemory>> =
        combine(records, query, _ftsIds) { items, currentQuery, ftsIds ->
            scoreResults(items, currentQuery, ftsIds)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    val watchList: StateFlow<List<MemoryRecord>> =
        records.map { items ->
            items
                .filter { it.favorite || it.searchCount >= 2 }
                .sortedWith(
                    compareByDescending<MemoryRecord> { it.favorite }
                        .thenByDescending { it.searchCount }
                        .thenByDescending { it.updatedAt },
                )
                .take(8)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    val lastSeenRecords: StateFlow<List<MemoryRecord>> =
        records.map { items ->
            SmartMemoryEngine.lastSeen(items)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    val smartSuggestions: StateFlow<List<SmartSuggestion>> =
        combine(records, carryContext) { items, context ->
            SmartMemoryEngine.carrySuggestions(items, context)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    val smartBrief: StateFlow<String> =
        combine(smartSuggestions, carryContext) { suggestions, context ->
            SmartMemoryEngine.smartBrief(suggestions, context)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            "",
        )

    val answer: StateFlow<String> =
        combine(searchResults, query) { results, currentQuery ->
            buildAnswer(results.firstOrNull()?.record, currentQuery)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            "",
        )

    init {
        viewModelScope.launch {
            repository.seedIfEmpty()
        }
        viewModelScope.launch {
            var rebuilt = false
            records.collect { items ->
                if (!rebuilt && items.isNotEmpty()) {
                    repository.rebuildSearchIndex(items)
                    rebuilt = true
                    refreshFts(query.value)
                }
            }
        }
    }

    fun setSection(section: AppSection) {
        _section.value = section
    }

    fun setQuery(query: String) {
        _query.value = query
        refreshFts(query)
    }

    fun submitSearch() {
        val ids = searchResults.value
            .take(3)
            .map { it.record.id }
        viewModelScope.launch {
            repository.bumpSearchCounts(ids)
        }
    }

    fun applyVoiceToSearch(transcript: String) {
        setSection(AppSection.Search)
        setQuery(MemoryTextParser.extractSearchTerm(transcript))
        submitSearch()
    }

    fun updateDraft(transform: (MemoryDraft) -> MemoryDraft) {
        _draft.update(transform)
    }

    fun updateCarryContext(transform: (CarryContext) -> CarryContext) {
        _carryContext.update(transform)
    }

    fun applyVoiceToDraft(transcript: String) {
        val parsed = MemoryTextParser.parseRecordText(transcript)
        _draft.update { current ->
            current.copy(
                itemName = parsed.itemName.ifBlank { current.itemName },
                location = parsed.location.ifBlank { current.location },
                note = appendNotes(current.note, parsed.note),
                tags = mergeTags(current.tags, parsed.tags),
                confidence = parsed.confidence,
            )
        }
        setSection(AppSection.Add)
    }

    fun analyzeDraftImage() {
        val imageUri = draft.value.imageUri ?: return

        _ocrState.value = OcrUiState(running = true, message = "사진에서 물건과 텍스트를 분석하는 중")

        viewModelScope.launch {
            runCatching {
                imageTextRecognizer.recognize(imageUri)
            }.onSuccess { analysis ->
                applyOcrAnalysis(analysis)
            }.onFailure { error ->
                _ocrState.value = OcrUiState(
                    error = error.message ?: "OCR 분석에 실패했습니다.",
                )
            }
        }
    }

    fun clearOcrState() {
        _ocrState.value = OcrUiState()
    }

    fun applyPhotoLink(record: MemoryRecord) {
        _draft.update { current ->
            current.copy(
                itemName = current.itemName.ifBlank { record.itemName },
                location = current.location.ifBlank { record.location },
                note = appendNotes(
                    current.note,
                    "과거 기록 연결: ${record.itemName} · ${record.location}",
                ),
                tags = mergeTags(current.tags, record.tags, record.itemName, record.location),
                confidence = maxOf(current.confidence, 0.82f),
            )
        }
    }

    fun saveDraft() {
        val current = draft.value
        if (!current.canSave) return

        val now = System.currentTimeMillis()
        val tags = mergeTags(
            current.tags,
            MemoryTextParser.buildTags(
                current.itemName,
                current.location,
                current.note,
            ),
        )

        viewModelScope.launch {
            repository.insert(
                MemoryRecord(
                    itemName = current.itemName.trim(),
                    location = current.location.trim(),
                    note = current.note.trim(),
                    tags = tags,
                    imageUri = current.imageUri,
                    createdAt = now,
                    updatedAt = now,
                    confidence = current.confidence.coerceIn(0f, 1f),
                    favorite = current.favorite,
                ),
            )
            _lastSavedItem.value = current.itemName.trim()
            _draft.value = MemoryDraft()
            _ocrState.value = OcrUiState()
            setQuery(current.itemName.trim())
            _section.value = AppSection.Search
        }
    }

    fun clearLastSavedItem() {
        _lastSavedItem.value = null
    }

    fun delete(record: MemoryRecord) {
        viewModelScope.launch {
            repository.delete(record)
        }
    }

    fun toggleFavorite(record: MemoryRecord) {
        viewModelScope.launch {
            repository.setFavorite(record, !record.favorite)
        }
    }

    fun historyFor(record: MemoryRecord): List<MemoryRecord> {
        val target = MemoryTextParser.normalize(record.itemName)
        return records.value
            .filter { MemoryTextParser.normalize(it.itemName) == target }
            .sortedByDescending { it.createdAt }
    }

    private fun refreshFts(query: String) {
        ftsSearchJob?.cancel()
        if (query.isBlank()) {
            _ftsIds.value = emptyList()
            return
        }

        ftsSearchJob = viewModelScope.launch {
            _ftsIds.value = repository.searchFts(query).map { it.id }
        }
    }

    private fun scoreResults(
        items: List<MemoryRecord>,
        currentQuery: String,
        ftsIds: List<Long>,
    ): List<ScoredMemory> {
        val queryText = currentQuery.trim()
        if (queryText.isBlank()) {
            return items
                .sortedByDescending { it.createdAt }
                .map { ScoredMemory(it, 0) }
        }

        val normalizedQuery = MemoryTextParser.normalize(queryText)
        val tokens = MemoryTextParser.expandQueryTerms(queryText)
            .map { MemoryTextParser.normalize(it) }
            .filter { it.isNotBlank() }

        return items
            .mapNotNull { record ->
                val ftsRank = ftsIds.indexOf(record.id)
                val score = scoreRecord(record, queryText, normalizedQuery, tokens, ftsRank)
                if (score > 0) ScoredMemory(record, score) else null
            }
            .sortedWith(
                compareByDescending<ScoredMemory> { it.score }
                    .thenByDescending { it.record.createdAt }
                    .thenComparator { a, b -> collator.compare(a.record.itemName, b.record.itemName) },
            )
    }

    private fun scoreRecord(
        record: MemoryRecord,
        rawQuery: String,
        normalizedQuery: String,
        tokens: List<String>,
        ftsRank: Int,
    ): Int {
        val item = MemoryTextParser.normalize(record.itemName)
        val location = MemoryTextParser.normalize(record.location)
        val note = MemoryTextParser.normalize(record.note)
        val tags = MemoryTextParser.normalize(record.tags)
        val haystack = "$item $location $note $tags"

        var score = if (ftsRank >= 0) 78 - minOf(ftsRank, 24) else 0
        if (item == normalizedQuery) score += 120
        if (item.contains(normalizedQuery)) score += 80
        if (tags.contains(normalizedQuery)) score += 55
        if (location.contains(normalizedQuery)) score += 40
        if (note.contains(normalizedQuery)) score += 25

        val embeddingScore = (LocalTextEmbedding.similarity(rawQuery, record) * 52).toInt()
        if (embeddingScore >= 8) score += embeddingScore

        tokens.forEach { token ->
            if (token.isBlank()) return@forEach
            if (item.contains(token)) score += 30
            if (tags.contains(token)) score += 18
            if (location.contains(token)) score += 12
            if (note.contains(token)) score += 8
            if (haystack.contains(token)) score = max(score, 10)
        }

        if (record.favorite) score += 7
        score += minOf(record.searchCount, 6)

        return score
    }

    private fun buildAnswer(record: MemoryRecord?, currentQuery: String): String {
        if (currentQuery.isBlank()) return ""
        if (record == null) return "'$currentQuery' 기록을 찾지 못했습니다."

        return "${record.itemName}은 ${record.location}에 있습니다."
    }

    private fun applyOcrAnalysis(analysis: OcrAnalysis) {
        val parsed = analysis.parsed
        _draft.update { current ->
            current.copy(
                itemName = parsed.itemName.ifBlank { current.itemName },
                location = parsed.location.ifBlank { current.location },
                note = appendNotes(current.note, parsed.note),
                tags = mergeTags(current.tags, parsed.tags),
                confidence = maxOf(current.confidence, parsed.confidence),
            )
        }

        val message = when {
            parsed.itemName.isNotBlank() && parsed.location.isNotBlank() -> "물건명과 위치 후보를 채웠습니다."
            parsed.itemName.isNotBlank() -> "물건명 후보를 채웠습니다."
            analysis.labels.isNotEmpty() -> "사진 라벨을 과거 기록과 연결했습니다."
            analysis.text.isNotBlank() -> "OCR 텍스트를 메모에 추가했습니다."
            else -> "사진에서 연결할 단서를 찾지 못했습니다."
        }

        _ocrState.value = OcrUiState(
            text = analysis.text,
            labels = analysis.labels,
            links = SmartMemoryEngine.linkPhotoToRecords(analysis, records.value),
            message = message,
        )
    }

    private fun appendNotes(vararg notes: String): String =
        notes
            .flatMap { it.split("\n\n") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("\n\n")

    private fun mergeTags(vararg groups: String): String =
        groups
            .flatMap { it.split(",", " ", "\n") }
            .map { it.trim() }
            .filter { it.length >= 2 }
            .distinctBy { MemoryTextParser.normalize(it) }
            .take(14)
            .joinToString(",")
}
