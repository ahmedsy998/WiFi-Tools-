package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ScanHistoryEntity::class, SecurityReportEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WifiToolsDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun securityReportDao(): SecurityReportDao

    companion object {
        @Volatile
        private var INSTANCE: WifiToolsDatabase? = null

        fun getInstance(context: Context): WifiToolsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WifiToolsDatabase::class.java,
                    "wifi_tools_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
