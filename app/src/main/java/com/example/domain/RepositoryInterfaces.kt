package com.example.domain

import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    val scanHistory: Flow<List<ScanHistoryRecord>>
    val securityReports: Flow<List<SecurityReportRecord>>

    suspend fun saveScanSnapshot(
        networks: List<WifiNetwork>,
        connectedSsid: String?
    ): Long

    suspend fun saveSecurityReport(report: SecurityReportRecord): Long
    suspend fun deleteScanHistory(id: Long)
    suspend fun deleteSecurityReport(id: Long)
    suspend fun deleteAllData()
}
