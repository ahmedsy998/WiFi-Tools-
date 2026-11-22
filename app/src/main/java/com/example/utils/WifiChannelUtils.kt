package com.example.utils

import com.example.domain.ChannelCongestion
import com.example.domain.WifiBand
import com.example.domain.WifiNetwork
import kotlin.math.abs

object WifiChannelUtils {

    fun frequencyToBand(frequencyMhz: Int): WifiBand = when (frequencyMhz) {
        in 2400..2500 -> WifiBand.BAND_2_4_GHZ
        in 5150..5925 -> WifiBand.BAND_5_GHZ
        in 5926..7125 -> WifiBand.BAND_6_GHZ
        else -> WifiBand.UNKNOWN
    }

    fun frequencyToChannel(frequencyMhz: Int): Int = when {
        frequencyMhz == 2484 -> 14
        frequencyMhz in 2412..2472 -> (frequencyMhz - 2407) / 5
        frequencyMhz in 5170..5885 -> (frequencyMhz - 5000) / 5
        frequencyMhz == 5935 -> 2
        frequencyMhz in 5955..7115 -> (frequencyMhz - 5950) / 5
        else -> -1
    }

    fun channelWidthLabel(channelWidthCode: Int): String = when (channelWidthCode) {
        0 -> "20 MHz"
        1 -> "40 MHz"
        2 -> "80 MHz"
        3 -> "160 MHz"
        4 -> "80+80 MHz"
        5 -> "320 MHz"
        else -> "20 MHz"
    }

    fun wifiStandardLabel(standardCode: Int): String = when (standardCode) {
        1 -> "Wi-Fi 1/2/3 (802.11a/b/g)"
        4 -> "Wi-Fi 4 (802.11n)"
        5 -> "Wi-Fi 5 (802.11ac)"
        6 -> "Wi-Fi 6 (802.11ax)"
        7 -> "Wi-Fi 60GHz (802.11ad)"
        8 -> "Wi-Fi 7 (802.11be)"
        else -> "802.11"
    }

    /**
     * Computes channel congestion across 2.4 GHz (channels 1..13) or 5 GHz channels
     * based on genuine discovered networks, factoring co-channel and adjacent-channel overlap.
     */
    fun analyzeChannelCongestion(
        networks: List<WifiNetwork>,
        band: WifiBand
    ): List<ChannelCongestion> {
        val bandNetworks = networks.filter { it.band == band && it.channel > 0 }
        val targetChannels = when (band) {
            WifiBand.BAND_2_4_GHZ -> (1..13).toList()
            WifiBand.BAND_5_GHZ -> listOf(36, 40, 44, 48, 52, 56, 60, 64, 100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 144, 149, 153, 157, 161, 165)
            WifiBand.BAND_6_GHZ -> bandNetworks.map { it.channel }.distinct().sorted().ifEmpty { listOf(1, 5, 9, 13, 17, 21, 37) }
            WifiBand.UNKNOWN -> emptyList()
        }

        return targetChannels.map { channel ->
            val directNets = bandNetworks.filter { it.channel == channel }
            val overlappingNets = if (band == WifiBand.BAND_2_4_GHZ) {
                bandNetworks.filter { it.channel != channel && abs(it.channel - channel) <= 2 }
            } else {
                emptyList()
            }

            // Weight stronger signals more heavily in congestion score (0..100)
            val directWeight = directNets.sumOf { SignalQualityUtils.rssiToPercent(it.rssi) * 0.35 + 12.0 }
            val overlapWeight = overlappingNets.sumOf {
                val distanceFactor = if (abs(it.channel - channel) == 1) 0.5 else 0.25
                (SignalQualityUtils.rssiToPercent(it.rssi) * 0.25 + 6.0) * distanceFactor
            }
            val score = (directWeight + overlapWeight).toInt().coerceIn(0, 100)
            val isRecommended = when (band) {
                WifiBand.BAND_2_4_GHZ -> channel in listOf(1, 6, 11) && score <= 30
                else -> score <= 25
            }

            ChannelCongestion(
                channel = channel,
                band = band,
                networkCount = directNets.size,
                overlappingCount = overlappingNets.size,
                congestionScore = score,
                strongestRssi = directNets.maxOfOrNull { it.rssi },
                isNonOverlappingStandard = band != WifiBand.BAND_2_4_GHZ || channel in listOf(1, 6, 11),
                isRecommended = isRecommended
            )
        }
    }

    fun recommendBestChannel(congestionList: List<ChannelCongestion>, band: WifiBand): Int? {
        if (congestionList.isEmpty()) return null
        val candidates = if (band == WifiBand.BAND_2_4_GHZ) {
            congestionList.filter { it.channel in listOf(1, 6, 11) }
        } else {
            congestionList
        }
        return candidates.minWithOrNull(
            compareBy<ChannelCongestion> { it.congestionScore }
                .thenBy { it.networkCount }
                .thenBy { it.channel }
        )?.channel
    }
}
