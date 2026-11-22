package com.example.data

import android.content.Context
import com.example.diagnostics.RouterDiagnosticsEngine
import com.example.domain.HistoryRepository
import com.example.network.ActiveConnectionMonitor
import com.example.network.WifiScannerService
import com.example.permissions.WifiPermissionManager
import com.example.reports.ReportGenerator
import com.example.security.AuthorizedAuditEngine
import com.example.security.LocalPasswordValidator
import com.example.security.SecurityAnalyzer

interface AppContainer {
    val permissionManager: WifiPermissionManager
    val securityAnalyzer: SecurityAnalyzer
    val passwordValidator: LocalPasswordValidator
    val authorizedAuditEngine: AuthorizedAuditEngine
    val wifiScannerService: WifiScannerService
    val activeConnectionMonitor: ActiveConnectionMonitor
    val routerDiagnosticsEngine: RouterDiagnosticsEngine
    val reportGenerator: ReportGenerator
    val historyRepository: HistoryRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext

    override val permissionManager: WifiPermissionManager by lazy {
        WifiPermissionManager(appContext)
    }

    override val securityAnalyzer: SecurityAnalyzer by lazy {
        SecurityAnalyzer()
    }

    override val passwordValidator: LocalPasswordValidator by lazy {
        LocalPasswordValidator()
    }

    override val authorizedAuditEngine: AuthorizedAuditEngine by lazy {
        AuthorizedAuditEngine()
    }

    override val wifiScannerService: WifiScannerService by lazy {
        WifiScannerService(appContext, permissionManager, securityAnalyzer)
    }

    override val activeConnectionMonitor: ActiveConnectionMonitor by lazy {
        ActiveConnectionMonitor(appContext, permissionManager)
    }

    override val routerDiagnosticsEngine: RouterDiagnosticsEngine by lazy {
        RouterDiagnosticsEngine()
    }

    override val reportGenerator: ReportGenerator by lazy {
        ReportGenerator()
    }

    private val database: WifiToolsDatabase by lazy {
        WifiToolsDatabase.getInstance(appContext)
    }

    override val historyRepository: HistoryRepository by lazy {
        HistoryRepositoryImpl(
            scanHistoryDao = database.scanHistoryDao(),
            securityReportDao = database.securityReportDao(),
            reportGenerator = reportGenerator
        )
    }
}
