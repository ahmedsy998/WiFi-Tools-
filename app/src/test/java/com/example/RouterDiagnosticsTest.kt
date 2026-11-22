package com.example

import com.example.diagnostics.RouterDiagnosticsEngine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouterDiagnosticsTest {

    private val engine = RouterDiagnosticsEngine()

    @Test
    fun `validates authorized IPv4, IPv6, and hostname syntax safely`() {
        assertTrue(engine.isValidTargetHost("192.168.1.1"))
        assertTrue(engine.isValidTargetHost("1.1.1.1"))
        assertTrue(engine.isValidTargetHost("dns.google"))
        assertTrue(engine.isValidTargetHost("2606:4700:4700::1111"))

        assertFalse(engine.isValidTargetHost(""))
        assertFalse(engine.isValidTargetHost("   "))
        assertFalse(engine.isValidTargetHost("1.1.1.1; rm -rf /"))
        assertFalse(engine.isValidTargetHost("http://192.168.1.1"))
    }
}
