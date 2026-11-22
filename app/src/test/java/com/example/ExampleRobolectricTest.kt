package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.HistoryRepositoryImpl
import com.example.data.WifiToolsDatabase
import com.example.domain.ActiveConnectionInfo
import com.example.domain.RiskLevel
import com.example.domain.SecurityProtocol
import com.example.domain.WifiBand
import com.example.domain.WifiNetwork
import com.example.reports.ReportGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun `app name matches WiFi Tools`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("WiFi Tools", context.getString(R.string.app_name))
    }

    @Test
    fun `room database saves and purges scan snapshots and reports`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WifiToolsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val generator = ReportGenerator()
            val repo = HistoryRepositoryImpl(db.scanHistoryDao(), db.securityReportDao(), generator)

            val network = WifiNetwork(
                ssid = "LabNet-5G",
                bssid = "AA:BB:CC:11:22:33",
                frequencyMhz = 5180,
                channel = 36,
                band = WifiBand.BAND_5_GHZ,
                rssi = -58,
                signalPercent = 84,
                capabilities = "[WPA3-SAE-CCMP][ESS]",
                securityProtocol = SecurityProtocol.WPA3_SAE,
                channelWidth = "80 MHz",
                wifiStandard = "Wi-Fi 6 (802.11ax)",
                hasWps = false,
                hasTkip = false,
                isHidden = false,
                securityScore = 98,
                riskLevel = RiskLevel.SECURE,
                timestampMs = System.currentTimeMillis()
            )

            repo.saveScanSnapshot(listOf(network), "LabNet-5G")
            val history = repo.scanHistory.first()
            assertEquals(1, history.size)
            assertEquals("LabNet-5G", history.first().strongestSsid)
            assertEquals(1, history.first().wpa3Count)

            val report = generator.buildComprehensiveReport(
                connection = ActiveConnectionInfo(
                    isConnectedToWifi = true,
                    isWifiEnabled = true,
                    ssid = "LabNet-5G",
                    gatewayIp = "192.168.1.1"
                ),
                networks = listOf(network),
                auditReport = null,
                latencyResult = null
            )
            assertTrue(report.fullMarkdown.contains("LabNet-5G"))
            assertTrue(report.fullJson.contains("192.168.1.1"))

            repo.saveSecurityReport(report)
            assertEquals(1, repo.securityReports.first().size)

            repo.deleteAllData()
            assertTrue(repo.scanHistory.first().isEmpty())
            assertTrue(repo.securityReports.first().isEmpty())
        } finally {
            db.close()
        }
    }
}
