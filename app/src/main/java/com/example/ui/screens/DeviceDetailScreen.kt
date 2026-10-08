package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DhcpLeaseInfo
import com.example.data.model.NetworkInterfaceInfo
import com.example.data.model.VlanInfo
import com.example.snmp.VendorOidRegistry
import com.example.ui.components.CpuGaugeMeter
import com.example.ui.components.OnlineStatusBadge
import com.example.ui.components.RealtimeBandwidthGraph
import com.example.ui.components.VendorBadge
import com.example.ui.components.formatSpeed
import com.example.ui.components.formatUptime
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseError
import com.example.viewmodel.NetworkViewModel

@Composable
fun DeviceDetailScreen(
    deviceId: Int,
    viewModel: NetworkViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allDevices by viewModel.allDevices.collectAsStateWithLifecycle()
    val device = allDevices.find { it.id == deviceId } ?: allDevices.firstOrNull()

    val ifacesMap by viewModel.deviceInterfaces.collectAsStateWithLifecycle()
    val vlanMap by viewModel.deviceVlans.collectAsStateWithLifecycle()
    val dhcpMap by viewModel.deviceDhcpLeases.collectAsStateWithLifecycle()
    val metricsMap by viewModel.metricsHistory.collectAsStateWithLifecycle()

    val ifaces = device?.let { ifacesMap[it.id] } ?: emptyList()
    val vlans = device?.let { vlanMap[it.id] } ?: emptyList()
    val dhcpLeases = device?.let { dhcpMap[it.id] } ?: emptyList()
    val metrics = device?.let { metricsMap[it.id] } ?: emptyList()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val connectionTestState by viewModel.connectionTestState.collectAsStateWithLifecycle()

    val tabTitles = listOf("Ringkasan", "Interface (${ifaces.size})", "VLAN (${vlans.size})", "DHCP (${dhcpLeases.size})", "SNMP Diagnostic")

    if (device == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Perangkat tidak ditemukan")
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("device_detail_screen")
    ) {
        // Top Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("btn_detail_back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }

                Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = device.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        VendorBadge(device.vendor)
                    }
                    Text(
                        text = "${device.host}:${device.port} • SNMP v${if (device.snmpVersion == 0) "1" else "2c"}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Status Badge & Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    OnlineStatusBadge(device.isOnline, device.isSimulated)

                    // Test Connect Button
                    IconButton(
                        onClick = { viewModel.testDeviceConnection(device) },
                        modifier = Modifier.testTag("btn_detail_test_connect")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Uji Koneksi",
                            tint = CyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Edit Device Button
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.testTag("btn_detail_edit_device")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Perangkat",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Delete Device Button
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.testTag("btn_detail_delete_device")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Perangkat",
                            tint = RoseError,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Live test connection result feedback banner
        if (connectionTestState != null && connectionTestState?.deviceId == device.id) {
            TestConnectionFeedbackBanner(
                testResult = connectionTestState,
                onDismiss = { viewModel.clearConnectionTestState() }
            )
        }

        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabTitles.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTabIndex == idx,
                    onClick = { selectedTabIndex = idx },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == idx) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.testTag("tab_$idx")
                )
            }
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTabIndex) {
                0 -> OverviewTabContent(device, metrics, viewModel)
                1 -> InterfacesTabContent(ifaces)
                2 -> VlansTabContent(vlans)
                3 -> DhcpTabContent(dhcpLeases)
                4 -> SnmpDiagnosticTabContent(device, viewModel)
            }
        }
    }

    if (showEditDialog) {
        DeviceFormDialog(
            deviceToEdit = device,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                viewModel.saveDevice(updated)
                showEditDialog = false
            },
            onTestConnection = { h, p, c, v, sim ->
                viewModel.testArbitraryConnection(h, p, c, v, sim)
            }
        )
    }

    if (showDeleteDialog) {
        DeleteDeviceConfirmDialog(
            device = device,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.deleteDevice(device.id)
                showDeleteDialog = false
                onBack()
            }
        )
    }
}

@Composable
private fun OverviewTabContent(
    device: com.example.data.model.DeviceEntity,
    metrics: List<com.example.data.model.MetricPoint>,
    viewModel: NetworkViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            RealtimeBandwidthGraph(
                metrics = metrics,
                currentDownKbps = device.downloadSpeedKbps,
                currentUpKbps = device.uploadSpeedKbps
            )
        }

        // CPU & Hardware Health Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CpuGaugeMeter(cpuPercent = device.cpuUsage, sizeDp = 100)

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "STATUS PERANGKAT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("RAM Memory:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${device.memoryUsage}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Klien Terhubung:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${device.clientCount} Device", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Waktu Aktif (Uptime):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatUptime(device.uptimeSeconds), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyanNeon, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Hardware Specification & SNMP MIB Details
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "IDENTITAS SNMP HARDWARE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(10.dp)) {
                        Text("sysDescr (Deskripsi Sistem):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = device.sysDescr.ifBlank { "SNMP Agent v2c standard compliant device." },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Lokasi Fisik (sysLocation):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(device.sysLocation, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Community String:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(device.community, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Polling Interval:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${device.pollingIntervalSeconds} detik", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mode Simulasi:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = {
                                viewModel.saveDevice(device.copy(isSimulated = !device.isSimulated))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (device.isSimulated) EmeraldGreen else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (device.isSimulated) "Simulasi AKTIF" else "Mode REAL SNMP",
                                fontSize = 11.sp,
                                color = if (device.isSimulated) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InterfacesTabContent(interfaces: List<NetworkInterfaceInfo>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(interfaces) { iface ->
            val isUp = iface.status.equals("UP", ignoreCase = true)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (isUp) EmeraldGreen.copy(alpha = 0.4f) else RoseError.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(if (isUp) EmeraldGreen else RoseError, CircleShape)
                            )
                            Text(
                                text = iface.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = iface.type,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Speed and MAC
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("MAC: ${iface.macAddress}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${iface.speedMbps} Mbps • MTU ${iface.mtu}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Real-time In/Out rates
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(13.dp))
                            Text(
                                text = "Rx: ${formatSpeed(iface.inRateKbps)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanNeon,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(13.dp))
                            Text(
                                text = "Tx: ${formatSpeed(iface.outRateKbps)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (iface.vlanId != null) {
                            Text(
                                text = "VLAN ${iface.vlanId}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VlansTabContent(vlans: List<VlanInfo>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(vlans) { vlan ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "VLAN ${vlan.vlanId}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = vlan.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = EmeraldGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = vlan.status,
                                color = EmeraldGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subnet Jaringan:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(vlan.subnet, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Port Anggota (Trunk/Access):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(vlan.ports, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun DhcpTabContent(leases: List<DhcpLeaseInfo>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(leases) { lease ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = lease.hostname,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = lease.ipAddress,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanNeon,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Surface(
                            color = if (lease.status == "Static") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = lease.status.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (lease.status == "Static") MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("MAC Hardware:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(lease.macAddress, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vendor Klien:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(lease.clientVendor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("VLAN & Sisa Sewa:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("VLAN ${lease.vlanId} • ${lease.leaseDuration}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun SnmpDiagnosticTabContent(
    device: com.example.data.model.DeviceEntity,
    viewModel: NetworkViewModel
) {
    var queryOid by remember { mutableStateOf(".1.3.6.1.2.1.1.1.0") } // sysDescr
    val diagnosticResult by viewModel.snmpDiagnosticResult.collectAsStateWithLifecycle()
    val isQuerying by viewModel.isPerformingSnmpQuery.collectAsStateWithLifecycle()

    val presets = listOf(
        "sysDescr" to ".1.3.6.1.2.1.1.1.0",
        "sysUpTime" to ".1.3.6.1.2.1.1.3.0",
        "sysName" to ".1.3.6.1.2.1.1.5.0",
        "CPU Load" to VendorOidRegistry.getSpec(
            try { com.example.snmp.NetworkVendor.valueOf(device.vendor.uppercase()) } catch (e: Exception) { com.example.snmp.NetworkVendor.GENERIC }
        ).cpuLoadOid,
        "ifNumber" to ".1.3.6.1.2.1.2.1.0"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = CyanNeon)
                        Text("SNMP Diagnostic MIB Query", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "Kirim paket UDP SNMP GET langsung ke ${device.host}:${device.port} untuk membaca ASN.1 BER OID apapun.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = queryOid,
                        onValueChange = { queryOid = it },
                        label = { Text("Object Identifier (OID)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_snmp_oid"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        singleLine = true
                    )

                    // Quick presets
                    Text("Preset OID Standar & Vendor:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { (label, oid) ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { queryOid = oid }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            viewModel.runSnmpDiagnostic(
                                host = device.host,
                                port = device.port,
                                community = device.community,
                                version = device.snmpVersion,
                                oid = queryOid
                            )
                        },
                        enabled = !isQuerying && queryOid.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_run_snmp_query")
                    ) {
                        if (isQuerying) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mengirim Query UDP...", color = Color.Black)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Jalankan SNMP GET", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Result view
        if (diagnosticResult != null) {
            val res = diagnosticResult!!
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, if (res.success) EmeraldGreen else RoseError),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (res.success) "RESPON BERHASIL" else "QUERY GAGAL / TIMEOUT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (res.success) EmeraldGreen else RoseError
                            )
                            Text(
                                text = "Latency: ${res.latencyMs} ms",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (res.success && res.varBinds.isNotEmpty()) {
                            res.varBinds.forEach { vb ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("OID: ${vb.oid}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Nilai: ${vb.value.stringValue}", fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = CyanNeon)
                                        Text("ASN.1 Tag: 0x${Integer.toHexString(vb.value.tag).uppercase()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        } else if (res.errorMessage != null) {
                            Text(
                                text = "Pesan Error: ${res.errorMessage}\n(Catatan: Jika di emulator dan IP lokal tidak dapat dijangkau dari luar, aktifkan mode simulasi atau gunakan IP publik/port forward)",
                                fontSize = 12.sp,
                                color = RoseError
                            )
                        }
                    }
                }
            }
        }
    }
}
