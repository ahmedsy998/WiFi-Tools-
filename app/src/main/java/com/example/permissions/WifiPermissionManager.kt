package com.example.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

data class WifiPermissionSnapshot(
    val hasFineLocation: Boolean,
    val hasCoarseLocation: Boolean,
    val hasNearbyWifiDevices: Boolean,
    val isLocationServiceEnabled: Boolean,
    val isWifiEnabled: Boolean,
    val androidSdkInt: Int = Build.VERSION.SDK_INT
) {
    val canReadScanResults: Boolean
        get() = hasFineLocation && isLocationServiceEnabled

    val canTriggerWifiScan: Boolean
        get() = hasFineLocation && isLocationServiceEnabled && isWifiEnabled &&
            (androidSdkInt < Build.VERSION_CODES.TIRAMISU || hasNearbyWifiDevices)

    val missingPermissions: List<String>
        get() = buildList {
            if (!hasFineLocation) add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (!hasCoarseLocation) add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (androidSdkInt >= Build.VERSION_CODES.TIRAMISU && !hasNearbyWifiDevices) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }

    val statusSummary: String
        get() = when {
            !hasFineLocation -> "Precise Location permission is required by Android to inspect Wi-Fi SSIDs, BSSIDs, and scan results."
            androidSdkInt >= Build.VERSION_CODES.TIRAMISU && !hasNearbyWifiDevices ->
                "Nearby Wi-Fi Devices permission (Android 13+) is recommended for full Wi-Fi inspection."
            !isLocationServiceEnabled ->
                "System Location Services are disabled. Android hides Wi-Fi scan results while Location is off."
            !isWifiEnabled ->
                "Wi-Fi radio is currently disabled. Enable Wi-Fi to discover networks and inspect active links."
            else -> "All required Android Wi-Fi permissions and radios are active."
        }
}

class WifiPermissionManager(private val context: Context) {

    fun requiredRuntimePermissions(): Array<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
    }.toTypedArray()

    fun getPermissionSnapshot(): WifiPermissionSnapshot {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasNearby = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val isLocationEnabled = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                locationManager?.isLocationEnabled == true
            } else {
                @Suppress("DEPRECATION")
                (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                    locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true)
            }
        }.getOrDefault(false)

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val isWifiEnabled = runCatching { wifiManager?.isWifiEnabled == true }.getOrDefault(false)

        return WifiPermissionSnapshot(
            hasFineLocation = hasFine,
            hasCoarseLocation = hasCoarse,
            hasNearbyWifiDevices = hasNearby,
            isLocationServiceEnabled = isLocationEnabled,
            isWifiEnabled = isWifiEnabled
        )
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openWifiSettings() {
        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Settings.Panel.ACTION_WIFI
        } else {
            Settings.ACTION_WIFI_SETTINGS
        }
        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure {
                val fallback = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(fallback) }
            }
    }
}
