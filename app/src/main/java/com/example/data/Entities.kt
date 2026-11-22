package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.RiskLevel
import com.example.domain.ScanHistoryRecord
import com.example.domain.SecurityReportRecord

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val totalNetworks: Int,
    val openOrWeakCount: Int,
    val wpa3Count: Int,
    val strongestSsid: String,
    val strongestRssi: Int,
    val connectedSsid: String?,
    val detailsJson: String
)

@Entity(tableName = "security_reports")
data class SecurityReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val title: String,
    val reportType: String,
    val targetName: String,
    val overallScore: Int,
    val riskLevel: String,
    val summary: String,
    val fullMarkdown: String,
    val fullJson: String
)

fun ScanHistoryEntity.toDomain(): ScanHistoryRecord = ScanHistoryRecord(
    id = id,
    timestampMs = timestampMs,
    totalNetworks = totalNetworks,
    openOrWeakCount = openOrWeakCount,
    wpa3Count = wpa3Count,
    strongestSsid = strongestSsid,
    strongestRssi = strongestRssi,
    connectedSsid = connectedSsid,
    detailsJson = detailsJson
)

fun SecurityReportEntity.toDomain(): SecurityReportRecord = SecurityReportRecord(
    id = id,
    timestampMs = timestampMs,
    title = title,
    reportType = reportType,
    targetName = targetName,
    overallScore = overallScore,
    riskLevel = runCatching { RiskLevel.valueOf(riskLevel) }.getOrDefault(RiskLevel.MODERATE),
    summary = summary,
    fullMarkdown = fullMarkdown,
    fullJson = fullJson
)

fun SecurityReportRecord.toEntity(): SecurityReportEntity = SecurityReportEntity(
    id = id,
    timestampMs = timestampMs,
    title = title,
    reportType = reportType,
    targetName = targetName,
    overallScore = overallScore,
    riskLevel = riskLevel.name,
    summary = summary,
    fullMarkdown = fullMarkdown,
    fullJson = fullJson
)
