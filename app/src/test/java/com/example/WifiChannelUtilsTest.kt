package com.example

import com.example.domain.RiskLevel
import com.example.domain.SecurityProtocol
import com.example.domain.WifiBand
import com.example.domain.WifiNetwork
import com.example.utils.SignalQualityUtils
import com.example.utils.WifiChannelUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiChannelUtilsTest {

    @Test
    fun `maps 2_4 GHz, 5 GHz, and 6 GHz frequencies accurately`() {
        assertEquals(WifiBand.BAND_2_4_GHZ, WifiChannelUtils.frequencyToBand(2412))
        assertEquals(1, WifiChannelUtils.frequencyToChannel(2412))
        assertEquals(6, WifiChannelUtils.frequencyToChannel(2437))
        assertEquals(11, WifiChannelUtils.frequencyToChannel(2462))

        assertEquals(WifiBand.BAND_5_GHZ, WifiChannelUtils.frequencyToBand(5180))
        assertEquals(36, WifiChannelUtils.frequencyToChannel(5180))
        assertEquals(149, WifiChannelUtils.frequencyToChannel(5745))

        assertEquals(WifiBand.BAND_6_GHZ, WifiChannelUtils.frequencyToBand(5955))
        assertEquals(1, WifiChannelUtils.frequencyToChannel(5955))
    }

    @Test
    fun `recommends least congested non-overlapping 2_4 GHz channel`() {
        val nets = listOf(
            sampleNet("A", 2412, 1, -45),
            sampleNet("B", 2412, 1, -50),
            sampleNet("C", 2437, 6, -48)
        )
        val congestion = WifiChannelUtils.analyzeChannelCongestion(nets, WifiBand.BAND_2_4_GHZ)
        val recommended = WifiChannelUtils.recommendBestChannel(congestion, WifiBand.BAND_2_4_GHZ)
        assertEquals(11, recommended)
    }

    @Test
    fun `rssi conversion clamps cleanly between 0 and 100`() {
        assertEquals(100, SignalQualityUtils.rssiToPercent(-40))
        assertEquals(100, SignalQualityUtils.rssiToPercent(-50))
        assertEquals(50, SignalQualityUtils.rssiToPercent(-75))
        assertEquals(0, SignalQualityUtils.rssiToPercent(-100))
        assertEquals(0, SignalQualityUtils.rssiToPercent(-120))
        assertTrue(SignalQualityUtils.rssiToBars(-52) == 4)
    }

    private fun sampleNet(ssid: String, freq: Int, ch: Int, rssi: Int) = WifiNetwork(
        ssid = ssid,
        bssid = "00:11:22:33:44:0$ch",
        frequencyMhz = freq,
        channel = ch,
        band = WifiBand.BAND_2_4_GHZ,
        rssi = rssi,
        signalPercent = SignalQualityUtils.rssiToPercent(rssi),
        capabilities = "[WPA2-PSK-CCMP]",
        securityProtocol = SecurityProtocol.WPA2_PSK,
        channelWidth = "20 MHz",
        wifiStandard = "Wi-Fi 5",
        hasWps = false,
        hasTkip = false,
        isHidden = false,
        securityScore = 80,
        riskLevel = RiskLevel.LOW,
        timestampMs = 1L
    )
}
