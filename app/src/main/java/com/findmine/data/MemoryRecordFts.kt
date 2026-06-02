package com.findmine.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4
@Entity(tableName = "memory_records_fts")
data class MemoryRecordFts(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Long,
    val itemName: String,
    val location: String,
    val note: String,
    val tags: String,
)
