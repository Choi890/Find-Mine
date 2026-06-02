package com.findmine.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MemoryRecord::class, MemoryRecordFts::class],
    version = 2,
    exportSchema = false,
)
abstract class FindMineDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var instance: FindMineDatabase? = null

        private val Migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE VIRTUAL TABLE IF NOT EXISTS `memory_records_fts`
                    USING FTS4(
                        `itemName` TEXT NOT NULL,
                        `location` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `tags` TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO `memory_records_fts`(
                        rowid,
                        itemName,
                        location,
                        note,
                        tags
                    )
                    SELECT id, itemName, location, note, tags FROM `memory_records`
                    """.trimIndent(),
                )
            }
        }

        fun get(context: Context): FindMineDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FindMineDatabase::class.java,
                    "find_mine.db",
                )
                    .addMigrations(Migration1To2)
                    .build()
                    .also { instance = it }
            }
    }
}
