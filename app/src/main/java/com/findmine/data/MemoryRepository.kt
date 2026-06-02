package com.findmine.data

import kotlinx.coroutines.flow.Flow

class MemoryRepository(
    private val dao: MemoryDao,
) {
    val records: Flow<List<MemoryRecord>> = dao.observeAll()

    suspend fun seedIfEmpty() {
        if (dao.count() > 0) return

        val now = System.currentTimeMillis()
        val samples = listOf(
            MemoryRecord(
                itemName = "여권",
                location = "검은 가방 안쪽 포켓",
                note = "여행 준비하면서 넣어둠",
                tags = "여권,가방,여행,신분증",
                createdAt = now - 86_400_000L,
                updatedAt = now - 86_400_000L,
                confidence = 0.96f,
                favorite = true,
                searchCount = 3,
            ),
            MemoryRecord(
                itemName = "무선 이어폰",
                location = "책상 왼쪽 서랍",
                note = "케이스째로 보관",
                tags = "이어폰,케이스,책상,서랍",
                createdAt = now - 9_600_000L,
                updatedAt = now - 9_600_000L,
                confidence = 0.93f,
            ),
            MemoryRecord(
                itemName = "충전기",
                location = "침대 옆 멀티탭",
                note = "USB-C 케이블과 같이 있음",
                tags = "충전기,케이블,침대,멀티탭",
                createdAt = now - 3_600_000L,
                updatedAt = now - 3_600_000L,
                confidence = 0.9f,
            ),
        )

        samples.forEach { insert(it) }
    }

    suspend fun insert(record: MemoryRecord): Long {
        val id = dao.insert(record)
        dao.upsertFts(record.copy(id = id).toFts())
        return id
    }

    suspend fun delete(record: MemoryRecord) {
        dao.deleteFts(record.id)
        dao.delete(record)
    }

    suspend fun setFavorite(record: MemoryRecord, favorite: Boolean) {
        dao.setFavorite(record.id, favorite, System.currentTimeMillis())
    }

    suspend fun bumpSearchCounts(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            dao.bumpSearchCounts(ids, System.currentTimeMillis())
        }
    }

    suspend fun rebuildSearchIndex(items: List<MemoryRecord>) {
        dao.clearFts()
        items.forEach { dao.upsertFts(it.toFts()) }
    }

    suspend fun searchFts(query: String, limit: Int = 24): List<MemoryRecord> =
        runCatching {
            val ftsQuery = MemoryTextParser.toFtsQuery(query)
            if (ftsQuery.isBlank()) emptyList() else dao.searchFts(ftsQuery, limit)
        }.getOrDefault(emptyList())

    private fun MemoryRecord.toFts(): MemoryRecordFts =
        MemoryRecordFts(
            rowId = id,
            itemName = itemName,
            location = location,
            note = note,
            tags = tags,
        )
}
