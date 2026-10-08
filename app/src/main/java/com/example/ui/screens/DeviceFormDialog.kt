package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceEntity
import com.example.snmp.SnmpClient
import com.example.ui.components.VendorBadge
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseError
import com.example.viewmodel.NetworkViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceFormDialog(
    deviceToEdit: DeviceEntity? = null,
    onDismiss: () -> Unit,
    onSave: (DeviceEntity) -> Unit,
    onTestConnection: suspend (host: String, port: Int, community: String, version: Int, isSimulated: Boolean) -> SnmpClient.SnmpResult
) {
    val isEditMode = deviceToEdit != null
    var name by remember { mutableStateOf(deviceToEdit?.name ?: "") }
    var vendor by remember { mutableStateOf(deviceToEdit?.vendor ?: "MikroTik") }
    var host by remember { mutableStateOf(deviceToEdit?.host ?: "192.168.88.1") }
    var port by remember { mutableStateOf(deviceToEdit?.port?.toString() ?: "161") }
    var community by remember { mutableStateOf(deviceToEdit?.community ?: "public") }
    var snmpVersion by remember { mutableIntStateOf(deviceToEdit?.snmpVersion ?: 1) } // 0 = v1, 1 = v2c
    var isSimulated by remember { mutableStateOf(deviceToEdit?.isSimulated ?: true) }
    var isEnabled by remember { mutableStateOf(deviceToEdit?.isEnabled ?: true) }

    val vendors = listOf("MikroTik", "Linksys", "Ruijie", "Cisco", "OpenWrt", "Generic")
    var expandedVendor by remember { mutableStateOf(false) }

    // Connectivity Test state
    val scope = rememberCoroutineScope()
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<SnmpClient.SnmpResult?>(null) }
    var testStatusMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isEditMode) Icons.Default.Edit else Icons.Default.Router,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = if (isEditMode) "Edit Perangkat Jaringan" else "Tambah Perangkat Baru",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Device Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Perangkat") },
                    placeholder = { Text("Contoh: Core Gateway MikroTik") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_form_device_name"),
                    singleLine = true
                )

                // Vendor Hardware Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedVendor,
                    onExpandedChange = { expandedVendor = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = vendor,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vendor Hardware") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVendor) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("dropdown_form_vendor")
                    )
                    ExposedDropdownMenu(
                        expanded = expandedVendor,
                        onDismissRequest = { expandedVendor = false }
                    ) {
                        vendors.forEach { v ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        VendorBadge(v)
                                        Text(v, fontSize = 13.sp)
                                    }
                                },
                                onClick = {
                                    vendor = v
                                    expandedVendor = false
                                    testResult = null
                                    testStatusMessage = null
                                    // Automatic smart presets when vendor is picked (only if creating new)
                                    if (!isEditMode) {
                                        when (v) {
                                            "MikroTik" -> {
                                                if (host == "192.168.1.1" || host.isBlank()) host = "192.168.88.1"
                                                if (name.isBlank() || name.contains("Router") || name.contains("Gateway")) name = "MikroTik CCR2004 RouterOS"
                                                community = "public"
                                            }
                                            "Linksys" -> {
                                                host = "192.168.1.1"
                                                if (name.isBlank() || name.contains("Router") || name.contains("Gateway")) name = "Linksys WRT3200ACM Smart Wi-Fi"
                                                community = "public"
                                            }
                                            "Ruijie" -> {
                                                host = "192.168.10.254"
                                                if (name.isBlank() || name.contains("Router") || name.contains("Gateway")) name = "Ruijie Reyee RG-NBS3100 Switch"
                                                community = "ruijie_snmp"
                                            }
                                            "Cisco" -> {
                                                host = "10.10.1.2"
                                                if (name.isBlank() || name.contains("Router") || name.contains("Gateway")) name = "Cisco Catalyst 2960-X"
                                                community = "cisco_public"
                                            }
                                            "OpenWrt" -> {
                                                host = "192.168.1.1"
                                                if (name.isBlank() || name.contains("Router") || name.contains("Gateway")) name = "OpenWrt 23.05 Linux Gateway"
                                                community = "public"
                                            }
                                            else -> {
                                                if (name.isBlank()) name = "Generic SNMP Device"
                                                community = "public"
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // Host IP and Port
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it; testResult = null; testStatusMessage = null },
                        label = { Text("IP Host / Domain") },
                        placeholder = { Text("192.168.1.1") },
                        modifier = Modifier
                            .weight(1.6f)
                            .testTag("input_form_device_host"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it; testResult = null; testStatusMessage = null },
                        label = { Text("Port UDP") },
                        placeholder = { Text("161") },
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("input_form_device_port"),
                        singleLine = true
                    )
                }

                // Community String
                OutlinedTextField(
                    value = community,
                    onValueChange = { community = it; testResult = null; testStatusMessage = null },
                    label = { Text("SNMP Community String") },
                    placeholder = { Text("public") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_form_device_community"),
                    singleLine = true
                )

                // SNMP Version selector
                Column {
                    Text(
                        text = "Versi Protokol SNMP:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { snmpVersion = 0 }
                        ) {
                            RadioButton(
                                selected = snmpVersion == 0,
                                onClick = { snmpVersion = 0 },
                                modifier = Modifier.testTag("radio_snmp_v1")
                            )
                            Text("SNMP v1", fontSize = 12.sp)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { snmpVersion = 1 }
                        ) {
                            RadioButton(
                                selected = snmpVersion == 1,
                                onClick = { snmpVersion = 1 },
                                modifier = Modifier.testTag("radio_snmp_v2c")
                            )
                            Text("SNMP v2c (Rekomendasi)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Mode Simulasi Switch
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mode Simulasi Telemetri",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isSimulated) "Aktif (Uji coba offline / emulator)" else "Nonaktif (Koneksi hardware riil via Wi-Fi/LAN)",
                                fontSize = 10.sp,
                                color = if (isSimulated) CyanNeon else EmeraldGreen
                            )
                        }
                        Switch(
                            checked = isSimulated,
                            onCheckedChange = { isSimulated = it; testResult = null; testStatusMessage = null },
                            modifier = Modifier.testTag("switch_form_simulated")
                        )
                    }
                }

                // ==========================================
                // BUTTON: UJI KONEKTIVITAS / TEST CONNECT
                // ==========================================
                OutlinedButton(
                    onClick = {
                        val p = port.toIntOrNull() ?: 161
                        scope.launch {
                            isTestingConnection = true
                            testResult = null
                            testStatusMessage = "Menguji paket UDP SNMP ke ${host.trim()}:$p..."
                            val res = onTestConnection(
                                host.trim(),
                                p,
                                community.trim().ifBlank { "public" },
                                snmpVersion,
                                isSimulated
                            )
                            testResult = res
                            isTestingConnection = false
                            testStatusMessage = if (res.success) {
                                "✅ Terhubung! Latensi: ${res.latencyMs} ms (${vendor} merespons)"
                            } else {
                                "❌ Gagal: ${res.errorMessage ?: "SNMP Timeout (Port 161)"}"
                            }
                        }
                    },
                    enabled = host.isNotBlank() && !isTestingConnection,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CyanNeon
                    ),
                    border = BorderStroke(1.dp, CyanNeon),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_form_test_connect")
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = CyanNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Menguji Koneksi...", fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🔌 Uji Koneksi / Test Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Result feedback box
                AnimatedVisibility(visible = testStatusMessage != null) {
                    val isSuccess = testResult?.success == true
                    Surface(
                        color = if (isSuccess) EmeraldGreen.copy(alpha = 0.15f) else RoseError.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isSuccess) EmeraldGreen else RoseError),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isSuccess) EmeraldGreen else RoseError,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = testStatusMessage ?: "",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSuccess) EmeraldGreen else RoseError,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && host.isNotBlank()) {
                        val p = port.toIntOrNull() ?: 161
                        val updatedDevice = if (isEditMode) {
                            deviceToEdit!!.copy(
                                name = name.trim(),
                                vendor = vendor,
                                host = host.trim(),
                                port = p,
                                community = community.trim().ifBlank { "public" },
                                snmpVersion = snmpVersion,
                                isSimulated = isSimulated,
                                isEnabled = isEnabled,
                                isOnline = testResult?.success ?: deviceToEdit.isOnline,
                                lastUpdated = System.currentTimeMillis()
                            )
                        } else {
                            DeviceEntity(
                                name = name.trim(),
                                vendor = vendor,
                                host = host.trim(),
                                port = p,
                                community = community.trim().ifBlank { "public" },
                                snmpVersion = snmpVersion,
                                isSimulated = isSimulated,
                                isEnabled = isEnabled,
                                isOnline = testResult?.success ?: true,
                                lastUpdated = System.currentTimeMillis()
                            )
                        }
                        onSave(updatedDevice)
                    }
                },
                enabled = name.isNotBlank() && host.isNotBlank() && !isTestingConnection,
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF090D16)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_form_save_device")
            ) {
                Text(
                    text = if (isEditMode) "Perbarui Perangkat" else "Simpan & Daftarkan",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_form_cancel")
            ) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun DeleteDeviceConfirmDialog(
    device: DeviceEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(RoseError.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = RoseError,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = "Hapus Perangkat Jaringan?",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Apakah Anda yakin ingin menghapus perangkat berikut dari sistem NetPulse SNMP?",
                    fontSize = 13.sp
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = device.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${device.vendor} • ${device.host}:${device.port}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Text(
                    text = "⚠️ Catatan: Riwayat telemetri perangkat ini akan dihapus dari basis data lokal.",
                    fontSize = 11.sp,
                    color = RoseError
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_confirm_delete_device")
            ) {
                Text("Ya, Hapus Perangkat", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_delete_device")
            ) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun TestConnectionFeedbackBanner(
    testResult: NetworkViewModel.ConnectionTestResult?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (testResult == null) return

    Surface(
        color = if (testResult.success) EmeraldGreen.copy(alpha = 0.95f) else RoseError.copy(alpha = 0.95f),
        contentColor = Color.White,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("test_connection_feedback_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (testResult.success) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = if (testResult.success) "Koneksi SNMP Terverifikasi" else "Koneksi Gagal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = testResult.message,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
