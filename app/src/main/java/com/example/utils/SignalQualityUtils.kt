package com.example.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SignalQualityUtils {

    fun rssiToPercent(rssiDbm: Int): Int {
        return when {
            rssiDbm >= -50 -> 100
            rssiDbm <= -100 -> 0
            else -> ((rssiDbm + 100) * 2).coerceIn(0, 100)
        }
    }

    fun rssiToBars(rssiDbm: Int): Int = when {
        rssiDbm >= -55 -> 4
        rssiDbm >= -67 -> 3
        rssiDbm >= -78 -> 2
        rssiDbm >= -88 -> 1
        else -> 0
    }

    fun rssiQualityLabel(rssiDbm: Int): String = when {
        rssiDbm >= -55 -> "EXCELLENT"
        rssiDbm >= -67 -> "GOOD"
        rssiDbm >= -75 -> "FAIR"
        rssiDbm >= -85 -> "WEAK"
        else -> "CRITICAL"
    }

    fun formatTimestamp(epochMillis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return formatter.format(Date(epochMillis))
    }

    fun formatShortTime(epochMillis: Long): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.US)
        return formatter.format(Date(epochMillis))
    }

    fun formatIpv4FromInt(ipInt: Int): String {
        if (ipInt == 0) return "Unavailable"
        return String.format(
            Locale.US,
            "%d.%d.%d.%d",
            ipInt and 0xFF,
            (ipInt shr 8) and 0xFF,
            (ipInt shr 16) and 0xFF,
            (ipInt shr 24) and 0xFF
        )
    }
}
