package com.findmine.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_records ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MemoryRecord>>

    @Query("SELECT COUNT(*) FROM memory_records")
    suspend fun count(): Int

    @Insert
    suspend fun insert(record: MemoryRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFts(record: MemoryRecordFts)

    @Update
    suspend fun update(record: MemoryRecord)

    @Delete
    suspend fun delete(record: MemoryRecord)

    @Query("UPDATE memory_records SET favorite = :favorite, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean, now: Long)

    @Query("UPDATE memory_records SET searchCount = searchCount + 1, updatedAt = :now WHERE id IN (:ids)")
    suspend fun bumpSearchCounts(ids: List<Long>, now: Long)

    @Query("DELETE FROM memory_records_fts WHERE rowid = :id")
    suspend fun deleteFts(id: Long)

    @Query("DELETE FROM memory_records_fts")
    suspend fun clearFts()

    @Query(
        """
        SELECT memory_records.*
        FROM memory_records
        JOIN memory_records_fts ON memory_records.id = memory_records_fts.rowid
        WHERE memory_records_fts MATCH :query
        ORDER BY memory_records.createdAt DESC
        LIMIT :limit
        """,
    )
    suspend fun searchFts(query: String, limit: Int): List<MemoryRecord>
}
