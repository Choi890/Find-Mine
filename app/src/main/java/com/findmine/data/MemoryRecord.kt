package com.findmine.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memory_records",
    indices = [
        Index(value = ["itemName"]),
        Index(value = ["createdAt"]),
        Index(value = ["favorite"]),
    ],
)
data class MemoryRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemName: String,
    val location: String,
    val note: String = "",
    val tags: String = "",
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val confidence: Float = 1f,
    val favorite: Boolean = false,
    val searchCount: Int = 0,
)
