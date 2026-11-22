package com.example.network

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import com.example.domain.WifiNetwork
import com.example.permissions.WifiPermissionManager
import com.example.security.SecurityAnalyzer
import com.example.utils.AppLogger
import com.example.utils.SignalQualityUtils
import com.example.utils.WifiChannelUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class WifiScanEvent(
    val networks: List<WifiNetwork>,
    val scanUpdated: Boolean,
    val timestampMs: Long,
    val statusNote: String
)

class WifiScannerService(
    private val context: Context,
    private val permissionManager: WifiPermissionManager,
    private val securityAnalyzer: SecurityAnalyzer
) {
    private val wifiManager: WifiManager? =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    fun observeScanBroadcasts(): Flow<WifiScanEvent> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    val updated = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                    val results = readCachedScanResults()
                    val note = if (updated) {
                        "Live RF scan completed (${results.size} access points)."
                    } else {
                        "Android OS scan throttle active (max 4 scans / 2 min). Showing latest cached OS results (${results.size} APs)."
                    }
                    trySend(
                        WifiScanEvent(
                            networks = results,
                            scanUpdated = updated,
                            timestampMs = System.currentTimeMillis(),
                            statusNote = note
                        )
                    )
                }
            }
        }

        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }

        // Emit current OS cache immediately if permitted
        val initial = readCachedScanResults()
        if (initial.isNotEmpty()) {
            trySend(
                WifiScanEvent(
                    networks = initial,
                    scanUpdated = false,
                    timestampMs = System.currentTimeMillis(),
                    statusNote = "Loaded ${initial.size} access points from Android Wi-Fi scan cache."
                )
            )
        }

        awaitClose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    @Suppress("DEPRECATION")
    fun triggerActiveScan(): Pair<Boolean, String> {
        val permissions = permissionManager.getPermissionSnapshot()
        if (!permissions.hasFineLocation) {
            return false to "Precise Location permission is required before Android permits Wi-Fi scanning."
        }
        if (!permissions.isLocationServiceEnabled) {
            return false to "System Location Services are disabled. Turn on Location to receive Wi-Fi scan results."
        }
        if (!permissions.isWifiEnabled) {
            return false to "Wi-Fi is turned off. Enable Wi-Fi to scan nearby networks."
        }
        val manager = wifiManager ?: return false to "WifiManager service unavailable on this device."

        return try {
            val started = manager.startScan()
            if (started) {
                true to "Active Wi-Fi scan requested via WifiManager…"
            } else {
                false to "Scan throttled by Android OS (foreground limit: 4 scans per 2 minutes). Displaying cached scan results."
            }
        } catch (se: SecurityException) {
            AppLogger.w("WifiScanner", "SecurityException starting scan", se)
            false to "Android denied Wi-Fi scan request due to missing runtime permission."
        } catch (e: Exception) {
            AppLogger.e("WifiScanner", "Unexpected error starting scan", e)
            false to "Unable to initiate Wi-Fi scan: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    @SuppressLint("MissingPermission")
    fun readCachedScanResults(): List<WifiNetwork> {
        val permissions = permissionManager.getPermissionSnapshot()
        if (!permissions.hasFineLocation) return emptyList()
        val manager = wifiManager ?: return emptyList()

        return try {
            val rawResults: List<ScanResult> = manager.scanResults ?: emptyList()
            val now = System.currentTimeMillis()
            rawResults
                .map { sr -> mapScanResult(sr, now) }
                .distinctBy { it.bssid }
                .sortedByDescending { it.rssi }
        } catch (se: SecurityException) {
            AppLogger.w("WifiScanner", "SecurityException reading scan results", se)
            emptyList()
        } catch (e: Exception) {
            AppLogger.e("WifiScanner", "Error reading scan results", e)
            emptyList()
        }
    }

    private fun mapScanResult(sr: ScanResult, nowMs: Long): WifiNetwork {
        val rawSsid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            sr.wifiSsid?.toString()?.removeSurrounding("\"") ?: ""
        } else {
            @Suppress("DEPRECATION")
            sr.SSID ?: ""
        }.trim()

        val isHidden = rawSsid.isBlank()
        val displaySsid = if (isHidden) "<Hidden Network>" else rawSsid
        val bssid = sr.BSSID?.uppercase() ?: "00:00:00:00:00:00"
        val freq = sr.frequency
        val band = WifiChannelUtils.frequencyToBand(freq)
        val channel = WifiChannelUtils.frequencyToChannel(freq)
        val capabilities = sr.capabilities ?: ""
        val protocol = securityAnalyzer.parseSecurityProtocol(capabilities)
        val hasWps = securityAnalyzer.hasWps(capabilities)
        val hasTkip = securityAnalyzer.hasTkip(capabilities)
        val score = securityAnalyzer.calculateScore(protocol, hasWps, hasTkip, isHidden)
        val risk = securityAnalyzer.scoreToRiskLevel(score)

        val standard = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WifiChannelUtils.wifiStandardLabel(sr.wifiStandard)
        } else {
            "802.11"
        }

        val width = WifiChannelUtils.channelWidthLabel(sr.channelWidth)

        return WifiNetwork(
            ssid = displaySsid,
            bssid = bssid,
            frequencyMhz = freq,
            channel = channel,
            band = band,
            rssi = sr.level,
            signalPercent = SignalQualityUtils.rssiToPercent(sr.level),
            capabilities = capabilities,
            securityProtocol = protocol,
            channelWidth = width,
            wifiStandard = standard,
            hasWps = hasWps,
            hasTkip = hasTkip,
            isHidden = isHidden,
            securityScore = score,
            riskLevel = risk,
            timestampMs = nowMs
        )
    }
}
