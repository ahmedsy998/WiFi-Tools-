package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestampMs DESC")
    fun observeAll(): Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScanHistoryEntity): Long

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scan_history")
    suspend fun deleteAll()
}

@Dao
interface SecurityReportDao {
    @Query("SELECT * FROM security_reports ORDER BY timestampMs DESC")
    fun observeAll(): Flow<List<SecurityReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SecurityReportEntity): Long

    @Query("DELETE FROM security_reports WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM security_reports")
    suspend fun deleteAll()
}
