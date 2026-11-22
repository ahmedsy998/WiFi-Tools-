package com.example

import com.example.domain.RiskLevel
import com.example.security.LocalPasswordValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalPasswordValidatorTest {

    private val validator = LocalPasswordValidator()

    @Test
    fun `short password fails WPA length compliance`() {
        val result = validator.analyze("abc123")
        assertFalse(result.wpaLengthCompliant)
        assertEquals(RiskLevel.CRITICAL, result.riskLevel)
    }

    @Test
    fun `common dictionary pattern triggers warning`() {
        val result = validator.analyze("password2026")
        assertTrue(result.wpaLengthCompliant)
        assertTrue(result.warnings.isNotEmpty())
        assertTrue(result.score < 60)
    }

    @Test
    fun `generated passphrase achieves high entropy and fortress strength`() {
        val generated = validator.generateSecurePassphrase(20)
        assertEquals(20, generated.length)
        val result = validator.analyze(generated)
        assertTrue(result.wpaLengthCompliant)
        assertTrue(result.hasLowercase && result.hasUppercase && result.hasDigits && result.hasSymbols)
        assertTrue(result.entropyBits >= 80.0)
        assertEquals(RiskLevel.SECURE, result.riskLevel)
    }
}
