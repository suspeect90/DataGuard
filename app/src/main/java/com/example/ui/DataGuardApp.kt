package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.components.DataGuardBrandLogo
import com.example.ui.components.NetworkStatusBadge
import com.example.ui.screens.AppFirewallScreen
import com.example.ui.screens.DataAnalyticsScreen
import com.example.ui.screens.OfflineCacheScreen
import com.example.ui.screens.ShieldDashboardScreen
import com.example.ui.theme.DataGuardTheme
import com.example.ui.theme.LocalDataGuardColors

const val ROUTE_SHIELD = "shield_dashboard"
const val ROUTE_APPS = "app_firewall"
const val ROUTE_ANALYTICS = "data_analytics"
const val ROUTE_CACHE = "offline_cache"

private data class DataGuardNavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val NAV_DESTINATIONS = listOf(
    DataGuardNavDestination(
        route = ROUTE_SHIELD,
        labelRes = R.string.tab_shield,
        selectedIcon = Icons.Rounded.Shield,
        unselectedIcon = Icons.Outlined.Shield,
        testTag = "nav_tab_shield"
    ),
    DataGuardNavDestination(
        route = ROUTE_APPS,
        labelRes = R.string.tab_apps,
        selectedIcon = Icons.Rounded.Apps,
        unselectedIcon = Icons.Outlined.Apps,
        testTag = "nav_tab_apps"
    ),
    DataGuardNavDestination(
        route = ROUTE_ANALYTICS,
        labelRes = R.string.tab_analytics,
        selectedIcon = Icons.Rounded.Analytics,
        unselectedIcon = Icons.Outlined.Analytics,
        testTag = "nav_tab_analytics"
    ),
    DataGuardNavDestination(
        route = ROUTE_CACHE,
        labelRes = R.string.tab_cache,
        selectedIcon = Icons.Rounded.Storage,
        unselectedIcon = Icons.Outlined.Storage,
        testTag = "nav_tab_cache"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataGuardApp(
    viewModel: DataGuardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    var showHelpSheet by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshUsageNow()
    }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onVpnConsentResult(result.resultCode == Activity.RESULT_OK)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.onFirewallToggleRequested()
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DataGuardEffect.RequestVpnConsent -> {
                    vpnPermissionLauncher.launch(effect.intent)
                }
                is DataGuardEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    val requestFirewallToggle: () -> Unit = {
        if (!uiState.vpnState.isActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotifPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotifPerm) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onFirewallToggleRequested()
            }
        } else {
            viewModel.onFirewallToggleRequested()
        }
    }

    DataGuardTheme(darkTheme = uiState.darkThemeEnabled) {
        val dgColors = LocalDataGuardColors.current
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_SHIELD

        val navigateToTab: (String) -> Unit = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        val auroraColor by animateColorAsState(
            targetValue = if (uiState.vpnState.isActive) dgColors.shieldActive else dgColors.wifiAccent,
            animationSpec = tween(600),
            label = "aurora_glow"
        )
        val bgColor = MaterialTheme.colorScheme.background

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(bgColor)
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                auroraColor.copy(alpha = if (uiState.darkThemeEnabled) 0.16f else 0.10f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.08f),
                            radius = size.maxDimension * 0.68f
                        )
                    )
                }
        ) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    TopAppBar(
                        title = {
                            DataGuardBrandLogo(isVpnActive = uiState.vpnState.isActive)
                        },
                        actions = {
                            NetworkStatusBadge(
                                snapshot = uiState.networkSnapshot,
                                modifier = Modifier.padding(end = 2.dp)
                            )
                            IconButton(
                                onClick = { showHelpSheet = true },
                                modifier = Modifier.testTag("help_guide_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                                    contentDescription = "Comprendre l'application",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { viewModel.toggleDarkTheme(!uiState.darkThemeEnabled) },
                                modifier = Modifier.testTag("theme_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (uiState.darkThemeEnabled) {
                                        Icons.Rounded.LightMode
                                    } else {
                                        Icons.Rounded.DarkMode
                                    },
                                    contentDescription = "Changer le thème visuel",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                },
                bottomBar = {
                    if (!useNavigationRail) {
                        Surface(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            shape = RoundedCornerShape(26.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            border = BorderStroke(1.dp, dgColors.cardBorder),
                            shadowElevation = 10.dp,
                            tonalElevation = 4.dp
                        ) {
                            NavigationBar(
                                containerColor = Color.Transparent,
                                tonalElevation = 0.dp,
                                windowInsets = WindowInsets(0, 0, 0, 0)
                            ) {
                                NAV_DESTINATIONS.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    val label = stringResource(dest.labelRes)
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateToTab(dest.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = label
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        modifier = Modifier.testTag(dest.testTag),
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = dgColors.shieldActive,
                                            selectedTextColor = dgColors.shieldActive,
                                            indicatorColor = dgColors.shieldActive.copy(alpha = 0.18f),
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavigationRail) {
                        NavigationRail(
                            modifier = Modifier.fillMaxHeight(),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            NAV_DESTINATIONS.forEach { dest ->
                                val selected = currentRoute == dest.route
                                val label = stringResource(dest.labelRes)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag(dest.testTag),
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = dgColors.shieldActive,
                                        selectedTextColor = dgColors.shieldActive,
                                        indicatorColor = dgColors.shieldActive.copy(alpha = 0.16f)
                                    )
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_SHIELD,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable(ROUTE_SHIELD) {
                            ShieldDashboardScreen(
                                uiState = uiState,
                                onToggleFirewall = requestFirewallToggle,
                                onToggleMobileDataAllowed = viewModel::toggleMobileDataAllowed,
                                onNavigateToApps = { navigateToTab(ROUTE_APPS) },
                                onNavigateToCache = { navigateToTab(ROUTE_CACHE) },
                                onClearLogs = viewModel::clearPacketLogs,
                                onDismissGuide = viewModel::dismissGuideBanner
                            )
                        }
                        composable(ROUTE_APPS) {
                            AppFirewallScreen(
                                uiState = uiState,
                                onSearchQueryChange = viewModel::setSearchQuery,
                                onFilterSelect = viewModel::setFilter,
                                onToggleHideSystemApps = viewModel::setHideSystemApps,
                                onToggleCellularOnly = viewModel::setCellularOnlyFirewall,
                                onToggleMobileDataAllowed = viewModel::toggleMobileDataAllowed,
                                onSetAllInstalledAllowed = viewModel::setAllInstalledMobileDataAllowed,
                                onBlockAllSocial = viewModel::blockAllSocialPresets,
                                onRefreshApps = viewModel::refreshInstalledApps
                            )
                        }
                        composable(ROUTE_ANALYTICS) {
                            DataAnalyticsScreen(
                                uiState = uiState,
                                onSelectPeriod = viewModel::selectUsagePeriod,
                                onSelectChartMode = viewModel::selectUsageChartMode
                            )
                        }
                        composable(ROUTE_CACHE) {
                            OfflineCacheScreen(
                                uiState = uiState,
                                onToggleWifiOnly = viewModel::toggleWifiOnlyDownloads,
                                onDownloadPack = viewModel::downloadOfflinePack,
                                onDeletePack = viewModel::deletePack,
                                onClearAllCache = viewModel::clearAllCache
                            )
                        }
                    }
                }
            }

            if (showHelpSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showHelpSheet = false },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "À quoi sert DataGuard ?",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "DataGuard vous aide à ne plus gaspiller votre forfait mobile (4G/5G) sans bloquer tout votre téléphone.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HelpStepRow(
                            title = "1. Bouclier VPN local (sans serveur externe)",
                            body = "Quand vous activez la protection, seules les applications bloquées (ex : TikTok, Instagram, Facebook) sont coupées. WhatsApp, les appels et vos autres applis restent connectées.",
                            accent = dgColors.shieldActive
                        )
                        HelpStepRow(
                            title = "2. Contrôle par application (4G ON / OFF)",
                            body = "Dans l'onglet Applis, choisissez chaque appli installée et coupez son accès réseau en permanence ou uniquement lorsque vous êtes en 4G/5G.",
                            accent = dgColors.wifiAccent
                        )
                        HelpStepRow(
                            title = "3. Analyse temps réel & Coffre Wi-Fi 1 Go",
                            body = "Repérez les applis qui consomment le plus dans Analyse, et téléchargez vos contenus hors-ligne uniquement en Wi-Fi gratuit dans Cache.",
                            accent = dgColors.cellularWarning
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { showHelpSheet = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Fermer le guide")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpStepRow(
    title: String,
    body: String,
    accent: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = accent
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
