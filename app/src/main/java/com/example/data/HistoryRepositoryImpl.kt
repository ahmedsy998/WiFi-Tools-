package com.example.data

import com.example.domain.HistoryRepository
import com.example.domain.ScanHistoryRecord
import com.example.domain.SecurityProtocol
import com.example.domain.SecurityReportRecord
import com.example.domain.WifiNetwork
import com.example.reports.ReportGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepositoryImpl(
    private val scanHistoryDao: ScanHistoryDao,
    private val securityReportDao: SecurityReportDao,
    private val reportGenerator: ReportGenerator
) : HistoryRepository {

    override val scanHistory: Flow<List<ScanHistoryRecord>> =
        scanHistoryDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override val securityReports: Flow<List<SecurityReportRecord>> =
        securityReportDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveScanSnapshot(
        networks: List<WifiNetwork>,
        connectedSsid: String?
    ): Long {
        val strongest = networks.maxByOrNull { it.rssi }
        val openOrWeak = networks.count {
            it.securityProtocol == SecurityProtocol.OPEN ||
                it.securityProtocol == SecurityProtocol.WEP_LEGACY ||
                it.securityProtocol == SecurityProtocol.WPA_LEGACY ||
                it.hasWps
        }
        val wpa3Count = networks.count {
            it.securityProtocol == SecurityProtocol.WPA3_SAE ||
                it.securityProtocol == SecurityProtocol.WPA3_ENTERPRISE ||
                it.securityProtocol == SecurityProtocol.WPA2_WPA3_TRANSITION
        }

        val entity = ScanHistoryEntity(
            timestampMs = System.currentTimeMillis(),
            totalNetworks = networks.size,
            openOrWeakCount = openOrWeak,
            wpa3Count = wpa3Count,
            strongestSsid = strongest?.ssid ?: "None",
            strongestRssi = strongest?.rssi ?: -100,
            connectedSsid = connectedSsid,
            detailsJson = reportGenerator.serializeNetworksToJson(networks)
        )
        return scanHistoryDao.insert(entity)
    }

    override suspend fun saveSecurityReport(report: SecurityReportRecord): Long {
        return securityReportDao.insert(report.toEntity())
    }

    override suspend fun deleteScanHistory(id: Long) {
        scanHistoryDao.deleteById(id)
    }

    override suspend fun deleteSecurityReport(id: Long) {
        securityReportDao.deleteById(id)
    }

    override suspend fun deleteAllData() {
        scanHistoryDao.deleteAll()
        securityReportDao.deleteAll()
    }
}
