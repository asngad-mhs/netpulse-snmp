package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DeviceEntity
import com.example.ui.components.OnlineStatusBadge
import com.example.ui.components.VendorBadge
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseError
import com.example.viewmodel.NetworkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NetworkViewModel,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val allDevices by viewModel.allDevices.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var deviceToEdit by remember { mutableStateOf<DeviceEntity?>(null) }
    var deviceToDelete by remember { mutableStateOf<DeviceEntity?>(null) }
    val connectionTestState by viewModel.connectionTestState.collectAsStateWithLifecycle()
    var showChangePassDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
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
                        showLogoutConfirmDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_settings_logout")
                ) {
                    Text("Ya, Keluar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showChangePassDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePassDialog = false },
            onSubmit = { oldP, newP ->
                viewModel.changePassword(oldP, newP)
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Operator & Session Profile
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth().testTag("card_user_session_profile")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CyanNeon.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = CyanNeon,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = currentUser.ifBlank { "Administrator NOC" },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(EmeraldGreen, CircleShape)
                                    )
                                    Text(
                                        text = "Level Akses: SuperAdmin NOC • Aktif",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showChangePassDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("btn_change_password")
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ubah Sandi", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { showLogoutConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("btn_settings_logout")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Keluar",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Theme / Dark Mode Section
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.DarkMode, contentDescription = null, tint = CyanNeon)
                        Text(
                            text = "TEMA TAMPILAN & MODE GELAP (DARK MODE)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Text(
                        text = "Mode Gelap NOC Cyberpunk dirancang dengan kontras ramah mata untuk pengawasan jaringan intensif sepanjang hari.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Options: Dark, Light, System
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ThemeOptionRow(
                            title = "Mode Gelap (NOC Dark) - Disarankan",
                            subtitle = "Tema latar hitam pekat dengan aksen cyan neon dan emerald",
                            icon = Icons.Default.DarkMode,
                            selected = themeMode == "DARK",
                            onClick = { viewModel.setThemeMode("DARK") },
                            testTag = "theme_option_dark"
                        )

                        ThemeOptionRow(
                            title = "Mode Terang (Light Mode)",
                            subtitle = "Tema putih bersih untuk lingkungan kantor berpenerangan tinggi",
                            icon = Icons.Default.LightMode,
                            selected = themeMode == "LIGHT",
                            onClick = { viewModel.setThemeMode("LIGHT") },
                            testTag = "theme_option_light"
                        )

                        ThemeOptionRow(
                            title = "Ikuti Pengaturan Sistem Android",
                            subtitle = "Otomatis berganti mengikuti mode perangkat HP/Tablet",
                            icon = Icons.Default.SettingsBrightness,
                            selected = themeMode == "SYSTEM",
                            onClick = { viewModel.setThemeMode("SYSTEM") },
                            testTag = "theme_option_system"
                        )
                    }
                }
            }
        }

        // Section: Device Management
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manajemen Perangkat (${allDevices.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("btn_open_add_device_dialog")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah Node", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(allDevices) { dev ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(dev.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                VendorBadge(dev.vendor)
                            }
                            Text(
                                text = "${dev.host}:${dev.port} • Community: ${dev.community}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (dev.isSimulated) "Mode: Simulasi Cerdas (Emulator)" else "Mode: Real Hardware SNMP UDP 161",
                                fontSize = 11.sp,
                                color = if (dev.isSimulated) EmeraldGreen else CyanNeon
                            )
                        }

                        OnlineStatusBadge(dev.isOnline, dev.isSimulated)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action buttons row: Uji Koneksi, Edit, Hapus
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { viewModel.testDeviceConnection(dev) },
                            modifier = Modifier.testTag("btn_settings_test_${dev.id}")
                        ) {
                            Icon(Icons.Default.Sensors, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Uji Koneksi", fontSize = 11.sp, color = CyanNeon, fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = { deviceToEdit = dev },
                            modifier = Modifier.testTag("btn_settings_edit_${dev.id}")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(
                            onClick = { deviceToDelete = dev },
                            modifier = Modifier.testTag("btn_settings_delete_${dev.id}")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = RoseError, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // App Information & Compatibility Notice
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Devices, contentDescription = null, tint = CyanNeon)
                        Text("KOMPATIBILITAS MULTI-DEVICE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "• Responsif untuk HP layar ringkas (Samsung Galaxy A10) & Tablet layar besar (Samsung Galaxy Tab A7 Lite / Foldable).\n• Didukung engine SNMP ASN.1 BER UDP independen (v1/v2c).\n• Vendor didukung penuh: MikroTik RouterOS, Cisco IOS/Catalyst, Ruijie Reyee RGOS, OpenWrt Net-SNMP, Linksys EA/WRT Series, dan Generic SNMP.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showAddDialog) {
        DeviceFormDialog(
            deviceToEdit = null,
            onDismiss = { showAddDialog = false },
            onSave = { newDev ->
                viewModel.saveDevice(newDev)
                showAddDialog = false
            },
            onTestConnection = { h, p, c, v, sim ->
                viewModel.testArbitraryConnection(h, p, c, v, sim)
            }
        )
    }

    if (deviceToEdit != null) {
        DeviceFormDialog(
            deviceToEdit = deviceToEdit,
            onDismiss = { deviceToEdit = null },
            onSave = { updated ->
                viewModel.saveDevice(updated)
                deviceToEdit = null
            },
            onTestConnection = { h, p, c, v, sim ->
                viewModel.testArbitraryConnection(h, p, c, v, sim)
            }
        )
    }

    if (deviceToDelete != null) {
        DeleteDeviceConfirmDialog(
            device = deviceToDelete!!,
            onDismiss = { deviceToDelete = null },
            onConfirm = {
                viewModel.deleteDevice(deviceToDelete!!.id)
                deviceToDelete = null
            }
        )
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Column {
                Text(title, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AddDeviceDialog(
    onDismiss: () -> Unit,
    onSave: (DeviceEntity) -> Unit
) {
    DeviceFormDialog(
        deviceToEdit = null,
        onDismiss = onDismiss,
        onSave = onSave,
        onTestConnection = { _, _, _, _, _ ->
            com.example.snmp.SnmpClient.SnmpResult(true, 12L)
        }
    )
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSubmit: (oldPass: String, newPass: String) -> Boolean
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Ubah Kata Sandi Admin", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMsg != null) {
                    Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                if (successMsg != null) {
                    Text(successMsg ?: "", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it; errorMsg = null },
                    label = { Text("Kata Sandi Lama") },
                    placeholder = { Text("default: admin123") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_old_password")
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; errorMsg = null },
                    label = { Text("Kata Sandi Baru") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_new_password")
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMsg = null },
                    label = { Text("Konfirmasi Kata Sandi Baru") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_confirm_password")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPassword.isBlank()) {
                        errorMsg = "Kata sandi baru tidak boleh kosong"
                        return@Button
                    }
                    if (newPassword != confirmPassword) {
                        errorMsg = "Konfirmasi kata sandi tidak cocok"
                        return@Button
                    }
                    val changed = onSubmit(oldPassword, newPassword)
                    if (changed) {
                        successMsg = "Kata sandi berhasil diperbarui!"
                    } else {
                        errorMsg = "Kata sandi lama salah!"
                    }
                },
                modifier = Modifier.testTag("btn_save_new_password")
            ) {
                Text("Simpan Sandi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
