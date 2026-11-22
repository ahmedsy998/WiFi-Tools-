package com.example.security

import com.example.domain.EncryptionStandardInfo
import com.example.domain.HardeningItem
import com.example.domain.RiskLevel

object EncryptionKnowledgeBase {

    val standards: List<EncryptionStandardInfo> = listOf(
        EncryptionStandardInfo(
            name = "WPA3-Personal (SAE)",
            statusBadge = "RECOMMENDED",
            riskLevel = RiskLevel.SECURE,
            cipherSuite = "SAE (Dragonfly) + AES-CCMP / GCMP-256 + Mandatory PMF",
            summary = "Current Wi-Fi Alliance security standard replacing the 4-way PSK handshake with Simultaneous Authentication of Equals (SAE).",
            vulnerabilities = "Resists offline dictionary attacks and provides forward secrecy. Transition mode may expose legacy WPA2 downgrade vectors if passphrases are weak.",
            guidance = "Enable WPA3-SAE or WPA2/WPA3 Transition with a 16+ character random passphrase and updated router firmware."
        ),
        EncryptionStandardInfo(
            name = "WPA2-Personal (AES-CCMP)",
            statusBadge = "ACCEPTABLE",
            riskLevel = RiskLevel.LOW,
            cipherSuite = "4-Way PSK Handshake + AES-CCMP (128-bit)",
            summary = "Widely supported standard offering strong frame confidentiality when configured strictly with AES-CCMP.",
            vulnerabilities = "Captured 4-way handshakes or PMKIDs can be tested offline against weak passphrases. Vulnerable to deauthentication frames without 802.11w PMF.",
            guidance = "Never enable TKIP mixed mode; pair with a 16+ character high-entropy passphrase and enable Protected Management Frames (PMF) if available."
        ),
        EncryptionStandardInfo(
            name = "OWE (Enhanced Open)",
            statusBadge = "GUEST SAFE",
            riskLevel = RiskLevel.MODERATE,
            cipherSuite = "Diffie-Hellman Key Exchange + AES-CCMP",
            summary = "Provides individualized over-the-air encryption on open networks without requiring a shared password.",
            vulnerabilities = "Protects against passive wireless eavesdropping, but does not verify AP identity against evil-twin hotspots.",
            guidance = "Ideal for public cafés or guest lobbies; always layer HTTPS/TLS and VPN on untrusted networks."
        ),
        EncryptionStandardInfo(
            name = "WPA1 (TKIP Legacy)",
            statusBadge = "WEAK",
            riskLevel = RiskLevel.HIGH,
            cipherSuite = "TKIP (RC4-based Temporal Key Integrity Protocol)",
            summary = "Transitional 2003 standard created for legacy WEP hardware before AES acceleration became standard.",
            vulnerabilities = "Susceptible to Beck-Tews and Ohigashi-Morii keystream recovery attacks; throttles modern Wi-Fi link rates to 54 Mbps.",
            guidance = "Disable WPA1 and TKIP entirely on all access points."
        ),
        EncryptionStandardInfo(
            name = "WEP & Open Unencrypted",
            statusBadge = "CRITICAL",
            riskLevel = RiskLevel.CRITICAL,
            cipherSuite = "RC4 (24-bit IV) or None",
            summary = "Obsolete 1997 WEP encryption or completely unencrypted wireless broadcasting.",
            vulnerabilities = "WEP keys can be recovered statistically within minutes; open networks expose unencrypted frames to anyone within radio range.",
            guidance = "Decommission WEP hardware immediately and avoid transmitting sensitive credentials over open Wi-Fi without an encrypted VPN tunnel."
        )
    )

    val routerHardeningChecklist: List<HardeningItem> = listOf(
        HardeningItem(
            id = "enforce_wpa3",
            title = "Enforce WPA3-SAE or WPA2-AES Only",
            category = "Encryption",
            detail = "Disable WEP, WPA1, and TKIP mixed ciphers. Enable Protected Management Frames (802.11w).",
            priority = RiskLevel.CRITICAL
        ),
        HardeningItem(
            id = "disable_wps",
            title = "Disable WPS (Wi-Fi Protected Setup)",
            category = "Authentication",
            detail = "Turn off both PIN and Push-Button WPS to prevent offline/online PIN brute-force attacks.",
            priority = RiskLevel.HIGH
        ),
        HardeningItem(
            id = "admin_password",
            title = "Replace Default Router Admin Credentials",
            category = "Management",
            detail = "Set a unique 16+ character admin password different from your Wi-Fi passphrase and disable remote WAN admin.",
            priority = RiskLevel.CRITICAL
        ),
        HardeningItem(
            id = "firmware_updates",
            title = "Enable Automatic Router Firmware Patches",
            category = "Maintenance",
            detail = "Verify router firmware is current to patch KRACK, FragAttacks, and UPnP/web-panel CVEs.",
            priority = RiskLevel.HIGH
        ),
        HardeningItem(
            id = "guest_isolation",
            title = "Isolate Guest & IoT Devices on Separate VLAN/SSID",
            category = "Segmentation",
            detail = "Enable AP/Client Isolation on guest networks so smart devices cannot probe LAN workstations.",
            priority = RiskLevel.MODERATE
        ),
        HardeningItem(
            id = "encrypted_dns",
            title = "Configure Encrypted DNS (DoT / DoH)",
            category = "Privacy",
            detail = "Enable Private DNS on Android and DNS-over-TLS/HTTPS on the router to prevent ISP/LAN DNS spoofing.",
            priority = RiskLevel.MODERATE
        ),
        HardeningItem(
            id = "disable_upnp",
            title = "Disable UPnP & Unused Port Forwards",
            category = "Perimeter",
            detail = "Prevent LAN devices or malware from silently opening external firewall ports on your gateway.",
            priority = RiskLevel.MODERATE
        )
    )
}
