package com.example

import com.example.domain.RiskLevel
import com.example.domain.SecurityProtocol
import com.example.domain.WifiBand
import com.example.domain.WifiNetwork
import com.example.security.SecurityAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityAnalyzerTest {

    private val analyzer = SecurityAnalyzer()

    @Test
    fun `parses WPA3 SAE capabilities correctly`() {
        val protocol = analyzer.parseSecurityProtocol("[WPA3-SAE-CCMP][ESS]")
        assertEquals(SecurityProtocol.WPA3_SAE, protocol)
    }

    @Test
    fun `parses WPA2 WPA3 transition capabilities correctly`() {
        val protocol = analyzer.parseSecurityProtocol("[WPA2-PSK+SAE-CCMP][RSN-PSK+SAE-CCMP][ESS]")
        assertEquals(SecurityProtocol.WPA2_WPA3_TRANSITION, protocol)
    }

    @Test
    fun `parses Open and WEP and legacy WPA capabilities correctly`() {
        assertEquals(SecurityProtocol.OPEN, analyzer.parseSecurityProtocol("[ESS]"))
        assertEquals(SecurityProtocol.WEP_LEGACY, analyzer.parseSecurityProtocol("[WEP][ESS]"))
        assertEquals(SecurityProtocol.WPA_LEGACY, analyzer.parseSecurityProtocol("[WPA-PSK-TKIP][ESS]"))
    }

    @Test
    fun `detects WPS and penalizes security score`() {
        val caps = "[WPA2-PSK-CCMP][WPS][ESS]"
        assertTrue(analyzer.hasWps(caps))
        assertFalse(analyzer.hasTkip(caps))

        val scoreWithoutWps = analyzer.calculateScore(SecurityProtocol.WPA2_PSK, hasWps = false, hasTkip = false, isHidden = false)
        val scoreWithWps = analyzer.calculateScore(SecurityProtocol.WPA2_PSK, hasWps = true, hasTkip = false, isHidden = false)
        assertTrue(scoreWithWps < scoreWithoutWps)
    }

    @Test
    fun `assessNetwork flags open network as critical with hardening steps`() {
        val openNet = WifiNetwork(
            ssid = "GuestOpen",
            bssid = "AA:BB:CC:DD:EE:FF",
            frequencyMhz = 2437,
            channel = 6,
            band = WifiBand.BAND_2_4_GHZ,
            rssi = -55,
            signalPercent = 90,
            capabilities = "[ESS]",
            securityProtocol = SecurityProtocol.OPEN,
            channelWidth = "20 MHz",
            wifiStandard = "Wi-Fi 5 (802.11ac)",
            hasWps = false,
            hasTkip = false,
            isHidden = false,
            securityScore = 5,
            riskLevel = RiskLevel.CRITICAL,
            timestampMs = 1L
        )
        val assessment = analyzer.assessNetwork(openNet)
        assertEquals(RiskLevel.CRITICAL, assessment.riskLevel)
        assertTrue(assessment.findings.any { it.title.contains("Open", ignoreCase = true) })
        assertTrue(assessment.hardeningSteps.isNotEmpty())
    }
}
