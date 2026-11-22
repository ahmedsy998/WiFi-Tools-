package com.example.security

import com.example.domain.PasswordAnalysisResult
import com.example.domain.RiskLevel
import java.security.SecureRandom
import kotlin.math.log2
import kotlin.math.roundToInt

/**
 * 100% on-device local passphrase strength and entropy analyzer for authorized network owners.
 * Passwords never leave local memory and are never transmitted or persisted.
 */
class LocalPasswordValidator {

    private val commonWeakPatterns = setOf(
        "password", "12345678", "123456789", "1234567890", "qwertyui", "qwerty123",
        "admin123", "welcome1", "letmein1", "iloveyou", "changeme", "wifi1234",
        "wireless", "internet", "default1", "abcd1234", "00000000", "11111111",
        "88888888", "99999999", "linksys1", "netgear1", "tplink12"
    )

    private val keyboardWalks = listOf(
        "qwerty", "asdfgh", "zxcvbn", "123456", "234567", "345678", "456789", "098765", "abcdef"
    )

    fun analyze(password: String): PasswordAnalysisResult {
        val length = password.length
        val hasLower = password.any { it in 'a'..'z' }
        val hasUpper = password.any { it in 'A'..'Z' }
        val hasDigits = password.any { it in '0'..'9' }
        val hasSymbols = password.any { !it.isLetterOrDigit() }
        val wpaLengthCompliant = length in 8..63

        var poolSize = 0
        if (hasLower) poolSize += 26
        if (hasUpper) poolSize += 26
        if (hasDigits) poolSize += 10
        if (hasSymbols) poolSize += 33

        // Raw alphabet entropy reduced by Shannon uniqueness ratio
        val uniqueChars = password.toSet().size
        val diversityRatio = if (length > 0) (uniqueChars.toDouble() / length.toDouble()).coerceIn(0.25, 1.0) else 0.0
        var entropyBits = if (length > 0 && poolSize > 0) {
            length * log2(poolSize.toDouble()) * (0.65 + 0.35 * diversityRatio)
        } else {
            0.0
        }

        val warnings = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        val lowered = password.lowercase()

        if (length == 0) {
            return PasswordAnalysisResult(
                length = 0,
                entropyBits = 0.0,
                wpaLengthCompliant = false,
                score = 0,
                strengthLabel = "EMPTY",
                riskLevel = RiskLevel.CRITICAL,
                hasLowercase = false,
                hasUppercase = false,
                hasDigits = false,
                hasSymbols = false,
                estimatedCrackResistance = "Enter a passphrase to evaluate locally",
                warnings = listOf("No passphrase entered."),
                recommendations = listOf("Use 16+ random characters combining letters, numbers, and symbols.")
            )
        }

        if (length < 8) {
            warnings += "Too short for WPA2/WPA3-Personal (IEEE 802.11i requires 8–63 ASCII characters)."
            recommendations += "Increase length to at least 16 characters."
        } else if (length > 63) {
            warnings += "Exceeds the 63-character WPA2/WPA3 ASCII passphrase limit."
            recommendations += "Keep passphrase between 16 and 63 characters."
        } else if (length < 12) {
            warnings += "Length under 12 characters is vulnerable to GPU-accelerated WPA2 PMKID/handshake cracking."
            recommendations += "Expand to 16+ characters or 4+ random dictionary words."
        }

        if (commonWeakPatterns.any { lowered.contains(it) }) {
            warnings += "Contains a widely known dictionary or default router passphrase pattern."
            entropyBits *= 0.45
        }

        if (keyboardWalks.any { lowered.contains(it) }) {
            warnings += "Contains predictable sequential or keyboard-walk characters."
            entropyBits *= 0.72
        }

        if (Regex("(19|20)\\d{2}").containsMatchIn(password)) {
            warnings += "Contains a year pattern (e.g., 19xx/20xx), which attackers prioritize in rule-based wordlists."
            entropyBits *= 0.85
        }

        if (Regex("(.)\\1{2,}").containsMatchIn(password)) {
            warnings += "Contains repeated consecutive characters."
            entropyBits *= 0.82
        }

        if (!hasUpper) recommendations += "Add uppercase characters."
        if (!hasLower) recommendations += "Add lowercase characters."
        if (!hasDigits) recommendations += "Include digits."
        if (!hasSymbols) recommendations += "Add special symbols (e.g. !@#\$%^&*)."

        val roundedEntropy = (entropyBits * 10.0).roundToInt() / 10.0

        val score = when {
            !wpaLengthCompliant -> (roundedEntropy * 0.6).toInt().coerceIn(0, 35)
            else -> ((roundedEntropy / 95.0) * 100.0).toInt().coerceIn(5, 100)
        }

        val (strengthLabel, riskLevel, resistance) = when {
            !wpaLengthCompliant -> Triple("NON-COMPLIANT", RiskLevel.CRITICAL, "Rejected by WPA2/WPA3 spec")
            score < 35 -> Triple("CRITICAL / WEAK", RiskLevel.CRITICAL, "Minutes to hours on modern GPU rigs")
            score < 55 -> Triple("MODERATE", RiskLevel.HIGH, "Days to weeks against targeted WPA2 cracking")
            score < 78 -> Triple("STRONG", RiskLevel.LOW, "Years to centuries against offline PBKDF2-HMAC-SHA1")
            else -> Triple("FORTRESS", RiskLevel.SECURE, "Computationally infeasible with current hardware")
        }

        if (recommendations.isEmpty() && warnings.isEmpty()) {
            recommendations += "Excellent passphrase hygiene. Rotate only if shared with untrusted guests."
        }

        return PasswordAnalysisResult(
            length = length,
            entropyBits = roundedEntropy,
            wpaLengthCompliant = wpaLengthCompliant,
            score = score,
            strengthLabel = strengthLabel,
            riskLevel = riskLevel,
            hasLowercase = hasLower,
            hasUppercase = hasUpper,
            hasDigits = hasDigits,
            hasSymbols = hasSymbols,
            estimatedCrackResistance = resistance,
            warnings = warnings,
            recommendations = recommendations
        )
    }

    fun generateSecurePassphrase(length: Int = 20): String {
        val safeLength = length.coerceIn(12, 63)
        val upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        val lower = "abcdefghijkmnopqrstuvwxyz"
        val digits = "23456789"
        val symbols = "!@#\$%^&*-_=+?"
        val allChars = upper + lower + digits + symbols
        val random = SecureRandom()

        val result = mutableListOf(
            upper[random.nextInt(upper.length)],
            lower[random.nextInt(lower.length)],
            digits[random.nextInt(digits.length)],
            symbols[random.nextInt(symbols.length)]
        )
        while (result.size < safeLength) {
            result += allChars[random.nextInt(allChars.length)]
        }
        result.shuffle(random)
        return result.joinToString("")
    }
}
