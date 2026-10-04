package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AddDeviceDialog
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeviceDetailScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppNavDestination
import com.example.viewmodel.NetworkViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val netViewModel: NetworkViewModel = viewModel()
            val themeMode by netViewModel.themeMode.collectAsStateWithLifecycle()
            val isLoggedIn by netViewModel.isLoggedIn.collectAsStateWithLifecycle()
            val savedUsername by netViewModel.savedUsername.collectAsStateWithLifecycle()
            val rememberMe by netViewModel.rememberMe.collectAsStateWithLifecycle()

            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                if (!isLoggedIn) {
                    LoginScreen(
                        initialUsername = savedUsername,
                        initialRememberMe = rememberMe,
                        onLoginSuccess = { user, pass, remember ->
                            netViewModel.login(user, pass, remember)
                        }
                    )
                } else {
                    NetPulseApp(viewModel = netViewModel)
                }
            }
        }
    }
}

data class NavItemSpec(
    val destination: AppNavDestination,
    val icon: ImageVector,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetPulseApp(viewModel: NetworkViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    var detailDeviceId by remember { mutableStateOf<Int?>(null) }
    var showAddDeviceModal by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val navItems = listOf(
        NavItemSpec(AppNavDestination.DASHBOARD, Icons.Default.Dashboard, "Dashboard"),
        NavItemSpec(AppNavDestination.INTERFACES, Icons.Default.Lan, "Interface"),
        NavItemSpec(AppNavDestination.VLAN_DHCP, Icons.Default.Dns, "VLAN & DHCP"),
        NavItemSpec(AppNavDestination.ALERTS, Icons.Default.NotificationsActive, "Telegram"),
        NavItemSpec(AppNavDestination.SETTINGS, Icons.Default.Settings, "Pengaturan")
    )

    // Back button handling
    BackHandler(enabled = detailDeviceId != null || currentScreen != AppNavDestination.DASHBOARD) {
        if (detailDeviceId != null) {
            detailDeviceId = null
        } else {
            viewModel.navigateTo(AppNavDestination.DASHBOARD)
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Konfirmasi Keluar", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Apakah Anda yakin ingin mengakhiri sesi monitoring dan keluar dari konsol NOC?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_logout")
                ) {
                    Text("Ya, Keluar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp // Tablet (Tab A7 Lite) or Desktop layout

        if (isWideScreen) {
            // Tablet & Desktop Responsive Layout with Navigation Rail
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    header = {
                        Box(modifier = Modifier.padding(vertical = 16.dp)) {
                            Icon(
                                Icons.Default.Router,
                                contentDescription = "Logo",
                                tint = CyanNeon,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    },
                    modifier = Modifier.testTag("tablet_nav_rail")
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    navItems.forEach { item ->
                        val selected = detailDeviceId == null && currentScreen == item.destination
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                detailDeviceId = null
                                viewModel.navigateTo(item.destination)
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, fontSize = 11.sp) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = CyanNeon,
                                selectedTextColor = CyanNeon,
                                indicatorColor = CyanNeon.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_rail_${item.destination.name}")
                        )
                    }
                }

                // Main Content Pane
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = if (detailDeviceId != null) "Telemetri Detail Perangkat" else currentScreen.label,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = { showLogoutDialog = true },
                                    modifier = Modifier.testTag("btn_tablet_logout")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Keluar",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { innerPadding ->
                    AppContentRouter(
                        currentScreen = currentScreen,
                        detailDeviceId = detailDeviceId,
                        viewModel = viewModel,
                        onNavigateToDetail = { id -> detailDeviceId = id },
                        onBackFromDetail = { detailDeviceId = null },
                        onNavigateToAddDevice = { showAddDeviceModal = true },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        } else {
            // Mobile Phone Responsive Layout (Galaxy A10, Compact screen)
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (detailDeviceId != null) "Detail Telemetri" else "NetPulse SNMP",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = { showLogoutDialog = true },
                                modifier = Modifier.testTag("btn_mobile_logout")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Keluar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    if (detailDeviceId == null) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .navigationBarsPadding()
                                .testTag("mobile_bottom_nav")
                        ) {
                            navItems.forEach { item ->
                                val selected = currentScreen == item.destination
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.navigateTo(item.destination) },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label, fontSize = 10.sp, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = CyanNeon,
                                        selectedTextColor = CyanNeon,
                                        indicatorColor = CyanNeon.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("nav_bottom_${item.destination.name}")
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                AppContentRouter(
                    currentScreen = currentScreen,
                    detailDeviceId = detailDeviceId,
                    viewModel = viewModel,
                    onNavigateToDetail = { id -> detailDeviceId = id },
                    onBackFromDetail = { detailDeviceId = null },
                    onNavigateToAddDevice = { showAddDeviceModal = true },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    if (showAddDeviceModal) {
        AddDeviceDialog(
            onDismiss = { showAddDeviceModal = false },
            onSave = { newDev ->
                viewModel.saveDevice(newDev)
                showAddDeviceModal = false
            }
        )
    }
}

@Composable
fun AppContentRouter(
    currentScreen: AppNavDestination,
    detailDeviceId: Int?,
    viewModel: NetworkViewModel,
    onNavigateToDetail: (Int) -> Unit,
    onBackFromDetail: () -> Unit,
    onNavigateToAddDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeDev by viewModel.activeDevice.collectAsStateWithLifecycle()

    if (detailDeviceId != null) {
        DeviceDetailScreen(
            deviceId = detailDeviceId,
            viewModel = viewModel,
            onBack = onBackFromDetail,
            modifier = modifier
        )
    } else {
        when (currentScreen) {
            AppNavDestination.DASHBOARD -> {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = onNavigateToDetail,
                    onNavigateToAddDevice = onNavigateToAddDevice,
                    modifier = modifier
                )
            }
            AppNavDestination.INTERFACES -> {
                // Direct deep interface inspection
                DeviceDetailScreen(
                    deviceId = activeDev?.id ?: 1,
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(AppNavDestination.DASHBOARD) },
                    modifier = modifier
                )
            }
            AppNavDestination.VLAN_DHCP -> {
                // Direct VLAN & DHCP inspection
                DeviceDetailScreen(
                    deviceId = activeDev?.id ?: 1,
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(AppNavDestination.DASHBOARD) },
                    modifier = modifier
                )
            }
            AppNavDestination.ALERTS -> {
                AlertsScreen(
                    viewModel = viewModel,
                    modifier = modifier
                )
            }
            AppNavDestination.SETTINGS -> {
                SettingsScreen(
                    viewModel = viewModel,
                    modifier = modifier
                )
            }
            else -> {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = onNavigateToDetail,
                    onNavigateToAddDevice = onNavigateToAddDevice,
                    modifier = modifier
                )
            }
        }
    }
}
