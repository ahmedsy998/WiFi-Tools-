package com.example.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.example.domain.ActiveConnectionInfo
import com.example.domain.SecurityProtocol
import com.example.permissions.WifiPermissionManager
import com.example.utils.AppLogger
import com.example.utils.SignalQualityUtils
import com.example.utils.WifiChannelUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.net.Inet4Address

class ActiveConnectionMonitor(
    private val context: Context,
    private val permissionManager: WifiPermissionManager
) {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    fun observeActiveConnection(): Flow<ActiveConnectionInfo> = callbackFlow {
        val cm = connectivityManager
        if (cm == null) {
            trySend(ActiveConnectionInfo(limitationNote = "ConnectivityManager unavailable."))
            close()
            return@callbackFlow
        }

        // Send initial state immediately
        trySend(readCurrentConnectionSnapshot())

        val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                override fun onAvailable(network: Network) {
                    trySend(readCurrentConnectionSnapshot(network))
                }

                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    trySend(readCurrentConnectionSnapshot(network, caps))
                }

                override fun onLinkPropertiesChanged(network: Network, linkProps: LinkProperties) {
                    trySend(readCurrentConnectionSnapshot(network, linkPropertiesOverride = linkProps))
                }

                override fun onLost(network: Network) {
                    trySend(readCurrentConnectionSnapshot())
                }
            }
        } else {
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    trySend(readCurrentConnectionSnapshot(network))
                }

                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    trySend(readCurrentConnectionSnapshot(network, caps))
                }

                override fun onLinkPropertiesChanged(network: Network, linkProps: LinkProperties) {
                    trySend(readCurrentConnectionSnapshot(network, linkPropertiesOverride = linkProps))
                }

                override fun onLost(network: Network) {
                    trySend(readCurrentConnectionSnapshot())
                }
            }
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        runCatching { cm.registerNetworkCallback(request, callback) }
            .onFailure { AppLogger.e("ConnectionMonitor", "Failed to register NetworkCallback", it) }

        awaitClose {
            runCatching { cm.unregisterNetworkCallback(callback) }
        }
    }

    @SuppressLint("MissingPermission")
    fun readCurrentConnectionSnapshot(
        targetNetwork: Network? = null,
        capsOverride: NetworkCapabilities? = null,
        linkPropertiesOverride: LinkProperties? = null
    ): ActiveConnectionInfo {
        val cm = connectivityManager
        val perms = permissionManager.getPermissionSnapshot()
        val isWifiEnabled = perms.isWifiEnabled

        val activeNetwork = targetNetwork ?: cm?.activeNetwork
        val caps = capsOverride ?: activeNetwork?.let { cm?.getNetworkCapabilities(it) }
        val linkProps = linkPropertiesOverride ?: activeNetwork?.let { cm?.getLinkProperties(it) }

        val isWifiTransport = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isVpnActive = cm?.activeNetwork?.let {
            cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        } == true

        if (!isWifiTransport) {
            return ActiveConnectionInfo(
                isConnectedToWifi = false,
                isWifiEnabled = isWifiEnabled,
                isVpnActive = isVpnActive,
                limitationNote = if (!isWifiEnabled) "Wi-Fi radio is disabled." else "Not currently associated with a Wi-Fi access point."
            )
        }

        val wifiInfo: WifiInfo? = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && caps?.transportInfo is WifiInfo -> {
                caps.transportInfo as WifiInfo
            }
            else -> {
                @Suppress("DEPRECATION")
                runCatching { wifiManager?.connectionInfo }.getOrNull()
            }
        }

        val rawSsid = wifiInfo?.ssid?.removeSurrounding("\"") ?: "<unknown ssid>"
        val ssidRestricted = rawSsid.equals("<unknown ssid>", ignoreCase = true) || rawSsid.isBlank()
        val displaySsid = when {
            !ssidRestricted -> rawSsid
            !perms.hasFineLocation -> "Hidden (Grant Location Permission)"
            !perms.isLocationServiceEnabled -> "Hidden (Enable System Location)"
            else -> "Connected Wi-Fi (SSID Restricted by OS)"
        }

        val rawBssid = wifiInfo?.bssid?.uppercase()
        val displayBssid = if (rawBssid.isNullOrBlank() || rawBssid == "02:00:00:00:00:00") {
            "Masked by Android OS"
        } else {
            rawBssid
        }

        val rssi = wifiInfo?.rssi?.takeIf { it in -127..0 } ?: -100
        val freq = wifiInfo?.frequency ?: 0
        val band = WifiChannelUtils.frequencyToBand(freq)
        val channel = WifiChannelUtils.frequencyToChannel(freq)
        val linkSpeed = wifiInfo?.linkSpeed ?: 0

        val txSpeed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            wifiInfo?.txLinkSpeedMbps?.takeIf { it > 0 }
        } else null

        val rxSpeed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            wifiInfo?.rxLinkSpeedMbps?.takeIf { it > 0 }
        } else null

        val standard = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wifiInfo != null) {
            WifiChannelUtils.wifiStandardLabel(wifiInfo.wifiStandard)
        } else {
            "802.11"
        }

        val protocol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && wifiInfo != null) {
            mapWifiInfoSecurityType(wifiInfo.currentSecurityType)
        } else {
            SecurityProtocol.UNKNOWN
        }

        // Extract IP, Subnet, Gateway, DNS from LinkProperties (modern API 26+)
        val ipAddresses = linkProps?.linkAddresses
            ?.mapNotNull { it.address?.hostAddress }
            ?.distinct()
            ?: emptyList()

        val ipv4Prefix = linkProps?.linkAddresses
            ?.firstOrNull { it.address is Inet4Address }
            ?.let { "/${it.prefixLength}" }

        var gatewayIp = linkProps?.routes
            ?.firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }
            ?.gateway?.hostAddress
            ?: linkProps?.routes?.firstOrNull { it.isDefaultRoute && it.gateway != null }?.gateway?.hostAddress

        if (gatewayIp.isNullOrBlank()) {
            @Suppress("DEPRECATION")
            val dhcpGateway = runCatching { wifiManager?.dhcpInfo?.gateway ?: 0 }.getOrDefault(0)
            if (dhcpGateway != 0) {
                gatewayIp = SignalQualityUtils.formatIpv4FromInt(dhcpGateway)
            }
        }

        val dnsServers = linkProps?.dnsServers
            ?.mapNotNull { it.hostAddress }
            ?.distinct()
            ?: emptyList()

        val isPrivateDns = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            linkProps?.isPrivateDnsActive == true
        } else false

        val privateDnsName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            linkProps?.privateDnsServerName
        } else null

        val mtu = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            linkProps?.mtu ?: 0
        } else 0

        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        val isCaptive = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) == true
        val isMetered = !(caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ?: true)

        val limitationNote = if (ssidRestricted) {
            "Android restricts SSID/BSSID visibility unless Precise Location permission and System Location are enabled."
        } else null

        return ActiveConnectionInfo(
            isConnectedToWifi = true,
            isWifiEnabled = isWifiEnabled,
            ssid = displaySsid,
            bssid = displayBssid,
            rssiDbm = rssi,
            signalPercent = SignalQualityUtils.rssiToPercent(rssi),
            frequencyMhz = freq,
            channel = channel,
            band = band,
            linkSpeedMbps = linkSpeed,
            txLinkSpeedMbps = txSpeed,
            rxLinkSpeedMbps = rxSpeed,
            wifiStandard = standard,
            localIpAddresses = ipAddresses,
            gatewayIp = gatewayIp,
            dnsServers = dnsServers,
            subnetMaskOrPrefix = ipv4Prefix,
            interfaceName = linkProps?.interfaceName,
            isPrivateDnsActive = isPrivateDns,
            privateDnsServerName = privateDnsName,
            hasValidatedInternet = hasInternet,
            isCaptivePortal = isCaptive,
            isMetered = isMetered,
            isVpnActive = isVpnActive,
            mtu = mtu,
            securityProtocol = protocol,
            limitationNote = limitationNote
        )
    }

    private fun mapWifiInfoSecurityType(type: Int): SecurityProtocol = when (type) {
        WifiInfo.SECURITY_TYPE_OPEN -> SecurityProtocol.OPEN
        WifiInfo.SECURITY_TYPE_WEP -> SecurityProtocol.WEP_LEGACY
        WifiInfo.SECURITY_TYPE_PSK -> SecurityProtocol.WPA2_PSK
        WifiInfo.SECURITY_TYPE_EAP -> SecurityProtocol.WPA2_ENTERPRISE
        WifiInfo.SECURITY_TYPE_SAE -> SecurityProtocol.WPA3_SAE
        WifiInfo.SECURITY_TYPE_OWE -> SecurityProtocol.OWE
        WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE,
        WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT -> SecurityProtocol.WPA3_ENTERPRISE
        else -> SecurityProtocol.UNKNOWN
    }
}
