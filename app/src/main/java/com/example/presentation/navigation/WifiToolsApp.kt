package com.example.presentation.navigation

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.domain.SecurityReportRecord
import com.example.presentation.components.PulsingLiveDot
import com.example.presentation.screens.AnalyzerScreen
import com.example.presentation.screens.DashboardScreen
import com.example.presentation.screens.ReportsScreen
import com.example.presentation.screens.RouterScreen
import com.example.presentation.screens.ScannerScreen
import com.example.presentation.screens.SecurityScreen
import com.example.presentation.viewmodel.WifiToolsViewModel
import com.example.ui.theme.CrimsonBorder
import com.example.ui.theme.CrimsonPanel
import com.example.ui.theme.CrimsonPanelHigh
import com.example.ui.theme.CrimsonTextMuted
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

enum class WifiToolsDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("dashboard", "Command", Icons.Rounded.Dashboard, Icons.Outlined.Dashboard),
    SCANNER("scanner", "Scanner", Icons.Rounded.Radar, Icons.Outlined.Radar),
    ANALYZER("analyzer", "Analyzer", Icons.Rounded.Speed, Icons.Outlined.Speed),
    SECURITY("security", "Security", Icons.Rounded.Security, Icons.Outlined.Security),
    ROUTER("router", "Router", Icons.Rounded.Router, Icons.Outlined.Router),
    REPORTS("reports", "Reports", Icons.Rounded.Assessment, Icons.Outlined.Assessment)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiToolsApp(
    viewModel: WifiToolsViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: WifiToolsDestination.DASHBOARD.route
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh permission and radio status whenever user returns from Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionsAndConnection()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Modern runtime permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshPermissionsAndConnection()
    }

    // Zero-permission SAF report export
    var pendingExportContent by remember { mutableStateOf<String?>(null) }
    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val content = pendingExportContent
        pendingExportContent = null
        if (uri != null && content != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                }
            }
        }
    }

    fun launchShareIntent(report: SecurityReportRecord) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, report.title)
            putExtra(Intent.EXTRA_TEXT, report.fullMarkdown)
        }
        runCatching {
            context.startActivity(Intent.createChooser(shareIntent, "Share Security Assessment"))
        }
    }

    fun navigateToRoute(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeToast()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        val useNavRail = maxWidth >= 700.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = PureBlack,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = "WiFi Tools Emblem",
                                    tint = NeonRed,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "WIFI TOOLS",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = PureWhite
                                )
                            }
                        },
                        actions = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 16.dp)
                            ) {
                                PulsingLiveDot(
                                    active = uiState.activeConnection.isConnectedToWifi,
                                    color = if (uiState.activeConnection.isConnectedToWifi) CyberGreen else NeonRed
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.activeConnection.isConnectedToWifi) {
                                        "${uiState.activeConnection.band.label}"
                                    } else {
                                        "NO WIFI"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CrimsonTextMuted
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = PureBlack,
                            titleContentColor = PureWhite
                        )
                    )
                    HorizontalDivider(color = CrimsonBorder, thickness = 1.dp)
                }
            },
            bottomBar = {
                if (!useNavRail) {
                    Column {
                        HorizontalDivider(color = CrimsonBorder, thickness = 1.dp)
                        NavigationBar(
                            containerColor = PureBlack,
                            contentColor = PureWhite,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            WifiToolsDestination.entries.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToRoute(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PureWhite,
                                        selectedTextColor = NeonRed,
                                        indicatorColor = CrimsonPanelHigh,
                                        unselectedIconColor = CrimsonTextMuted,
                                        unselectedTextColor = CrimsonTextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_${dest.route}")
                                )
                            }
                        }
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = CrimsonPanel,
                        contentColor = PureWhite,
                        actionColor = NeonRed
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = PureBlack,
                        contentColor = PureWhite,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        WifiToolsDestination.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToRoute(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = PureWhite,
                                    selectedTextColor = NeonRed,
                                    indicatorColor = CrimsonPanelHigh,
                                    unselectedIconColor = CrimsonTextMuted,
                                    unselectedTextColor = CrimsonTextMuted
                                ),
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PureBlack),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = WifiToolsDestination.DASHBOARD.route,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 860.dp)
                    ) {
                        composable(WifiToolsDestination.DASHBOARD.route) {
                            DashboardScreen(
                                state = uiState,
                                onTriggerScan = { viewModel.startWifiScan() },
                                onRequestPermissions = {
                                    permissionLauncher.launch(viewModel.requiredRuntimePermissions())
                                },
                                onOpenLocationSettings = { viewModel.openLocationSettings() },
                                onOpenWifiSettings = { viewModel.openWifiSettings() },
                                onOpenAppSettings = { viewModel.openAppSettings() },
                                onSaveSnapshot = { viewModel.saveScanSnapshotToHistory() },
                                onNavigateToTab = { route -> navigateToRoute(route) }
                            )
                        }
                        composable(WifiToolsDestination.SCANNER.route) {
                            ScannerScreen(
                                state = uiState,
                                onTriggerScan = { viewModel.startWifiScan() },
                                onSearchChange = { viewModel.updateSearchQuery(it) },
                                onBandFilterSelect = { viewModel.selectBandFilter(it) },
                                onSecurityFilterSelect = { viewModel.selectSecurityFilter(it) },
                                onSortOptionSelect = { viewModel.selectSortOption(it) },
                                onInspectNetwork = { viewModel.inspectNetwork(it) },
                                onSaveSnapshot = { viewModel.saveScanSnapshotToHistory() },
                                onRequestPermissions = {
                                    permissionLauncher.launch(viewModel.requiredRuntimePermissions())
                                },
                                onOpenLocationSettings = { viewModel.openLocationSettings() },
                                onOpenWifiSettings = { viewModel.openWifiSettings() },
                                onOpenAppSettings = { viewModel.openAppSettings() }
                            )
                        }
                        composable(WifiToolsDestination.ANALYZER.route) {
                            AnalyzerScreen(
                                state = uiState,
                                onSelectCongestionBand = { viewModel.selectCongestionBand(it) },
                                onRunConnectivityCheck = { viewModel.runConnectivityDiagnostics() },
                                onRefreshConnection = { viewModel.refreshPermissionsAndConnection() }
                            )
                        }
                        composable(WifiToolsDestination.SECURITY.route) {
                            SecurityScreen(
                                state = uiState,
                                onAuthorizationToggle = { viewModel.setAuthorizationConfirmed(it) },
                                onRunAuthorizedAudit = { viewModel.runAuthorizedConfigurationAudit() },
                                onToggleHardeningItem = { viewModel.toggleHardeningChecklistItem(it) },
                                onPasswordChange = { viewModel.evaluatePassword(it) },
                                onGeneratePassphrase = { viewModel.generateLocalPassphrase() },
                                onGenerateReport = { viewModel.generateAndSaveAssessmentReport() }
                            )
                        }
                        composable(WifiToolsDestination.ROUTER.route) {
                            RouterScreen(
                                state = uiState,
                                onHostChange = { viewModel.updateTargetHost(it) },
                                onPortChange = { viewModel.updateTargetPort(it) },
                                onProbeCountChange = { viewModel.updateProbeCount(it) },
                                onRunLatencyTest = { viewModel.runRouterLatencyTest() },
                                onSaveDiagnosticReport = { viewModel.generateAndSaveAssessmentReport() }
                            )
                        }
                        composable(WifiToolsDestination.REPORTS.route) {
                            ReportsScreen(
                                state = uiState,
                                onGenerateReport = { viewModel.generateAndSaveAssessmentReport() },
                                onPreviewReport = { viewModel.previewReport(it) },
                                onShareReport = { report -> launchShareIntent(report) },
                                onExportReportMarkdown = { report ->
                                    pendingExportContent = report.fullMarkdown
                                    exportDocumentLauncher.launch("wifi_tools_report_${report.timestampMs}.md")
                                },
                                onExportReportJson = { report ->
                                    pendingExportContent = report.fullJson
                                    exportDocumentLauncher.launch("wifi_tools_report_${report.timestampMs}.json")
                                },
                                onDeleteReport = { viewModel.deleteSecurityReport(it) },
                                onDeleteScanHistory = { viewModel.deleteScanHistory(it) },
                                onPurgeAllData = { viewModel.purgeAllLocalData() }
                            )
                        }
                    }
                }
            }
        }
    }
}
