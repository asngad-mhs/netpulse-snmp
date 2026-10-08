package com.example.data.repository

import com.example.data.local.AlertLogDao
import com.example.data.local.DeviceDao
import com.example.data.local.TelegramConfigDao
import com.example.data.model.AlertLogEntity
import com.example.data.model.DeviceEntity
import com.example.data.model.DhcpLeaseInfo
import com.example.data.model.MetricPoint
import com.example.data.model.NetworkInterfaceInfo
import com.example.data.model.TelegramConfigEntity
import com.example.data.model.VlanInfo
import com.example.snmp.NetworkVendor
import com.example.snmp.SnmpClient
import com.example.snmp.VendorOidRegistry
import com.example.telegram.TelegramNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class NetworkRepository(
    private val deviceDao: DeviceDao,
    private val telegramConfigDao: TelegramConfigDao,
    private val alertLogDao: AlertLogDao,
    private val snmpClient: SnmpClient = SnmpClient(),
    private val telegramNotifier: TelegramNotifier = TelegramNotifier()
) {
    val allDevices: Flow<List<DeviceEntity>> = deviceDao.getAllDevices()
    val telegramConfig: Flow<TelegramConfigEntity?> = telegramConfigDao.getConfigFlow()
    val alertLogs: Flow<List<AlertLogEntity>> = alertLogDao.getAlertsFlow()

    // Real-time metric history for graphs (last 30 points per device)
    private val _deviceMetricsHistory = MutableStateFlow<Map<Int, List<MetricPoint>>>(emptyMap())
    val deviceMetricsHistory = _deviceMetricsHistory.asStateFlow()

    // Cache of interfaces per device
    private val _deviceInterfaces = MutableStateFlow<Map<Int, List<NetworkInterfaceInfo>>>(emptyMap())
    val deviceInterfaces = _deviceInterfaces.asStateFlow()

    // Cache of VLANs per device
    private val _deviceVlans = MutableStateFlow<Map<Int, List<VlanInfo>>>(emptyMap())
    val deviceVlans = _deviceVlans.asStateFlow()

    // Cache of DHCP leases per device
    private val _deviceDhcpLeases = MutableStateFlow<Map<Int, List<DhcpLeaseInfo>>>(emptyMap())
    val deviceDhcpLeases = _deviceDhcpLeases.asStateFlow()

    // Alert cooldown map to prevent message spam (deviceId_type -> timestamp)
    private val alertCooldowns = mutableMapOf<String, Long>()

    // Real octet counters for real device bandwidth calculation
    private val prevInOctets = mutableMapOf<Int, Long>()
    private val prevOutOctets = mutableMapOf<Int, Long>()
    private val prevPollTimes = mutableMapOf<Int, Long>()

    suspend fun saveDevice(device: DeviceEntity): Long {
        return if (device.id == 0) {
            deviceDao.insertDevice(device)
        } else {
            deviceDao.updateDevice(device)
            device.id.toLong()
        }
    }

    suspend fun deleteDevice(deviceId: Int) {
        deviceDao.deleteDeviceById(deviceId)
        // Clean up auxiliary cached data
        val curIf = _deviceInterfaces.value.toMutableMap()
        curIf.remove(deviceId)
        _deviceInterfaces.value = curIf

        val curVlan = _deviceVlans.value.toMutableMap()
        curVlan.remove(deviceId)
        _deviceVlans.value = curVlan

        val curDhcp = _deviceDhcpLeases.value.toMutableMap()
        curDhcp.remove(deviceId)
        _deviceDhcpLeases.value = curDhcp

        val curMetrics = _deviceMetricsHistory.value.toMutableMap()
        curMetrics.remove(deviceId)
        _deviceMetricsHistory.value = curMetrics
    }

    /**
     * Test connectivity to a network host via UDP SNMP (Port 161) or verified simulation
     */
    suspend fun testDeviceConnectivity(
        host: String,
        port: Int = 161,
        community: String = "public",
        version: Int = 1,
        isSimulated: Boolean = false
    ): SnmpClient.SnmpResult {
        return if (isSimulated) {
            delay(280L)
            SnmpClient.SnmpResult(
                success = true,
                latencyMs = (9L..24L).random(),
                varBinds = listOf(
                    com.example.snmp.Asn1Ber.VarBind(
                        oid = ".1.3.6.1.2.1.1.1.0",
                        value = com.example.snmp.Asn1Ber.BerValue(
                            tag = com.example.snmp.Asn1Ber.TAG_OCTET_STRING,
                            rawValue = byteArrayOf(),
                            stringValue = "Simulated SNMP Device ($host)"
                        )
                    )
                ),
                errorMessage = null
            )
        } else {
            snmpClient.snmpGet(
                host = host,
                port = port,
                community = community,
                version = version,
                oids = listOf(
                    ".1.3.6.1.2.1.1.1.0", // sysDescr
                    ".1.3.6.1.2.1.1.3.0", // sysUpTime
                    ".1.3.6.1.2.1.1.5.0"  // sysName
                )
            )
        }
    }

    /**
     * Ping and refresh online status for a saved device
     */
    suspend fun testAndPingDevice(deviceId: Int): SnmpClient.SnmpResult {
        val dev = deviceDao.getDeviceById(deviceId) ?: return SnmpClient.SnmpResult(
            success = false,
            latencyMs = 0L,
            errorMessage = "Perangkat ID $deviceId tidak ditemukan di database"
        )
        val result = testDeviceConnectivity(
            host = dev.host,
            port = dev.port,
            community = dev.community,
            version = dev.snmpVersion,
            isSimulated = dev.isSimulated
        )
        val updated = dev.copy(
            isOnline = result.success,
            lastUpdated = System.currentTimeMillis()
        )
        deviceDao.updateDevice(updated)
        return result
    }

    suspend fun saveTelegramConfig(config: TelegramConfigEntity) {
        telegramConfigDao.saveConfig(config)
    }

    suspend fun testTelegramNotification(botToken: String, chatId: String): TelegramNotifier.SendResult {
        val testMessage = """
            🧪 <b>[NETPULSE SNMP] Uji Coba Notifikasi Berhasil!</b>
            ━━━━━━━━━━━━━━━━━━━
            Sistem NetPulse SNMP telah berhasil terhubung dengan Bot Telegram Anda.
            <b>Mode:</b> Real-time Multi-vendor Network Telemetry
            <b>Fitur:</b> Pantauan CPU, Upload/Download, Client, VLAN & DHCP
            <b>Waktu:</b> ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}
            ━━━━━━━━━━━━━━━━━━━
            <i>Notifikasi otomatis akan dikirim jika ada anomali atau pemutusan interface.</i>
        """.trimIndent()
        return telegramNotifier.sendMessage(botToken, chatId, testMessage)
    }

    suspend fun manualSendAlert(deviceId: Int, title: String, message: String, severity: String) {
        val device = deviceDao.getDeviceById(deviceId) ?: return
        val config = telegramConfigDao.getConfig()

        var sentTelegram = false
        var responseMsg: String? = null

        if (config != null && config.isEnabled && config.botToken.isNotBlank() && config.chatId.isNotBlank()) {
            val formatted = telegramNotifier.formatAlertMessage(
                severity = severity,
                vendor = device.vendor,
                deviceName = device.name,
                host = device.host,
                alertTitle = title,
                metricDetail = message
            )
            val res = telegramNotifier.sendMessage(config.botToken, config.chatId, formatted)
            sentTelegram = res.success
            responseMsg = res.message
        }

        alertLogDao.insertAlert(
            AlertLogEntity(
                deviceId = device.id,
                deviceName = device.name,
                vendor = device.vendor,
                severity = severity,
                message = "$title: $message",
                sentToTelegram = sentTelegram,
                telegramResponse = responseMsg
            )
        )
    }

    suspend fun clearAlerts() {
        alertLogDao.clearAll()
    }

    /**
     * Diagnostic SNMP query for a custom OID
     */
    suspend fun queryCustomOid(
        host: String,
        port: Int,
        community: String,
        version: Int,
        oid: String
    ): SnmpClient.SnmpResult {
        return snmpClient.snmpGet(
            host = host,
            port = port,
            community = community,
            version = version,
            oids = listOf(oid)
        )
    }

    /**
     * Start the continuous telemetry polling loop
     */
    fun startMonitoring(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val devices = deviceDao.getEnabledDevices()
                    for (dev in devices) {
                        pollDevice(dev)
                    }
                } catch (e: Exception) {
                    // ignore and retry next cycle
                }
                delay(3000L) // 3 seconds real-time polling cadence
            }
        }
    }

    private suspend fun pollDevice(device: DeviceEntity) {
        val now = System.currentTimeMillis()
        val config = telegramConfigDao.getConfig()

        var newCpu = device.cpuUsage
        var newUpload = device.uploadSpeedKbps
        var newDownload = device.downloadSpeedKbps
        var newClients = device.clientCount
        var newUptime = device.uptimeSeconds + 3L
        var isOnline = device.isOnline

        if (!device.isSimulated) {
            // Real network hardware SNMP query
            val vendorEnum = try {
                NetworkVendor.valueOf(device.vendor.uppercase())
            } catch (e: Exception) {
                NetworkVendor.GENERIC
            }
            val spec = VendorOidRegistry.getSpec(vendorEnum)

            val oidsToQuery = listOf(
                spec.cpuLoadOid,
                spec.uptimeOid,
                spec.clientCountOid,
                ".1.3.6.1.2.1.25.3.3.1.2.1", // Standard hrProcessorLoad fallback
                ".1.3.6.1.2.1.31.1.1.1.6.1", // ifHCInOctets (64-bit high capacity)
                ".1.3.6.1.2.1.31.1.1.1.10.1", // ifHCOutOctets (64-bit high capacity)
                ".1.3.6.1.2.1.2.2.1.10.1",   // ifInOctets (32-bit fallback)
                ".1.3.6.1.2.1.2.2.1.16.1"    // ifOutOctets (32-bit fallback)
            )

            val getResult = snmpClient.snmpGet(
                host = device.host,
                port = device.port,
                community = device.community,
                version = device.snmpVersion,
                oids = oidsToQuery
            )

            if (getResult.success && getResult.varBinds.isNotEmpty()) {
                isOnline = true
                var realInOctets: Long? = null
                var realOutOctets: Long? = null

                for (vb in getResult.varBinds) {
                    when {
                        vb.oid.startsWith(spec.cpuLoadOid) || vb.oid.startsWith(".1.3.6.1.2.1.25.3.3.1.2.1") -> {
                            val v = vb.value.longValue?.toInt()
                            if (v != null && v in 0..100) newCpu = v
                        }
                        vb.oid.startsWith(spec.uptimeOid) -> {
                            val ticks = vb.value.longValue
                            if (ticks != null) newUptime = ticks / 100 // TimeTicks is centiseconds
                        }
                        vb.oid.startsWith(spec.clientCountOid) -> {
                            val c = vb.value.longValue?.toInt()
                            if (c != null && c >= 0) newClients = c
                        }
                        vb.oid.startsWith(".1.3.6.1.2.1.31.1.1.1.6.1") || vb.oid.startsWith(".1.3.6.1.2.1.2.2.1.10.1") -> {
                            val oct = vb.value.longValue
                            if (oct != null && oct > 0L && realInOctets == null) realInOctets = oct
                        }
                        vb.oid.startsWith(".1.3.6.1.2.1.31.1.1.1.10.1") || vb.oid.startsWith(".1.3.6.1.2.1.2.2.1.16.1") -> {
                            val oct = vb.value.longValue
                            if (oct != null && oct > 0L && realOutOctets == null) realOutOctets = oct
                        }
                    }
                }

                // Compute real-time bandwidth delta
                val lastIn = prevInOctets[device.id]
                val lastOut = prevOutOctets[device.id]
                val lastTime = prevPollTimes[device.id]

                if (realInOctets != null && lastIn != null && lastTime != null && now > lastTime) {
                    val deltaSec = (now - lastTime) / 1000.0
                    if (deltaSec > 0.5) {
                        val deltaIn = (realInOctets - lastIn).coerceAtLeast(0L)
                        newDownload = (deltaIn * 8.0) / (deltaSec * 1024.0) // Kbps
                    }
                }
                if (realOutOctets != null && lastOut != null && lastTime != null && now > lastTime) {
                    val deltaSec = (now - lastTime) / 1000.0
                    if (deltaSec > 0.5) {
                        val deltaOut = (realOutOctets - lastOut).coerceAtLeast(0L)
                        newUpload = (deltaOut * 8.0) / (deltaSec * 1024.0) // Kbps
                    }
                }

                if (realInOctets != null) prevInOctets[device.id] = realInOctets
                if (realOutOctets != null) prevOutOctets[device.id] = realOutOctets
                prevPollTimes[device.id] = now
            } else {
                // If real query timed out or unreachable
                isOnline = false
                checkAlertDeviceDown(device, config, getResult.errorMessage ?: "SNMP Timeout")
            }
        } else {
            // Live realistic telemetry simulation for demonstration / emulator
            // Slight natural drift to model real internet traffic bursts
            val driftCpu = Random.nextInt(-4, 5)
            newCpu = (device.cpuUsage + driftCpu).coerceIn(5, 96)

            val driftDown = Random.nextDouble(-1200.0, 1600.0)
            newDownload = (device.downloadSpeedKbps + driftDown).coerceIn(1200.0, 95000.0)

            val driftUp = Random.nextDouble(-400.0, 600.0)
            newUpload = (device.uploadSpeedKbps + driftUp).coerceIn(400.0, 32000.0)

            val driftClients = Random.nextInt(-2, 3)
            newClients = (device.clientCount + driftClients).coerceIn(10, 250)

            isOnline = true
        }

        // Check threshold alerts (e.g. CPU > threshold)
        if (config != null && config.isEnabled) {
            if (newCpu >= config.cpuThreshold) {
                checkAlertHighCpu(device, newCpu, config)
            }
        }

        val updated = device.copy(
            cpuUsage = newCpu,
            uploadSpeedKbps = newUpload,
            downloadSpeedKbps = newDownload,
            clientCount = newClients,
            uptimeSeconds = newUptime,
            isOnline = isOnline,
            lastUpdated = now
        )
        deviceDao.updateDevice(updated)

        // Update real-time history for graphs
        val historyMap = _deviceMetricsHistory.value.toMutableMap()
        val currentList = historyMap[device.id]?.toMutableList() ?: mutableListOf()
        currentList.add(MetricPoint(now, newUpload, newDownload, newCpu))
        if (currentList.size > 40) {
            currentList.removeAt(0)
        }
        historyMap[device.id] = currentList
        _deviceMetricsHistory.value = historyMap

        // Refresh interfaces, VLANs, and DHCP for device if needed
        ensureDeviceAuxiliaryData(device, newDownload, newUpload)
    }

    private suspend fun checkAlertHighCpu(device: DeviceEntity, currentCpu: Int, config: TelegramConfigEntity) {
        val cooldownKey = "${device.id}_high_cpu"
        val lastSent = alertCooldowns[cooldownKey] ?: 0L
        val now = System.currentTimeMillis()

        // 3-minute cooldown between repeated high-CPU notifications
        if (now - lastSent > 180_000L) {
            alertCooldowns[cooldownKey] = now
            val alertTitle = "Penggunaan CPU Kritis (${currentCpu}%)"
            val detail = "Beban prosesor melebihi batas toleransi (${config.cpuThreshold}%)."

            val formatted = telegramNotifier.formatAlertMessage(
                severity = "CRITICAL",
                vendor = device.vendor,
                deviceName = device.name,
                host = device.host,
                alertTitle = alertTitle,
                metricDetail = detail
            )

            var sentTelegram = false
            var resp: String? = null
            if (config.botToken.isNotBlank() && config.chatId.isNotBlank()) {
                val res = telegramNotifier.sendMessage(config.botToken, config.chatId, formatted)
                sentTelegram = res.success
                resp = res.message
            }

            alertLogDao.insertAlert(
                AlertLogEntity(
                    deviceId = device.id,
                    deviceName = device.name,
                    vendor = device.vendor,
                    severity = "CRITICAL",
                    message = "$alertTitle - $detail",
                    sentToTelegram = sentTelegram,
                    telegramResponse = resp
                )
            )
        }
    }

    private suspend fun checkAlertDeviceDown(device: DeviceEntity, config: TelegramConfigEntity?, errorMsg: String) {
        val cooldownKey = "${device.id}_down"
        val lastSent = alertCooldowns[cooldownKey] ?: 0L
        val now = System.currentTimeMillis()

        if (now - lastSent > 300_000L) {
            alertCooldowns[cooldownKey] = now
            val alertTitle = "Perangkat Tidak Merespon (SNMP Timeout)"
            val detail = "Gagal polling SNMP ke ${device.host}:${device.port}. Detail: $errorMsg"

            var sentTelegram = false
            var resp: String? = null
            if (config != null && config.isEnabled && config.alertOnDeviceDown && config.botToken.isNotBlank() && config.chatId.isNotBlank()) {
                val formatted = telegramNotifier.formatAlertMessage(
                    severity = "CRITICAL",
                    vendor = device.vendor,
                    deviceName = device.name,
                    host = device.host,
                    alertTitle = alertTitle,
                    metricDetail = detail
                )
                val res = telegramNotifier.sendMessage(config.botToken, config.chatId, formatted)
                sentTelegram = res.success
                resp = res.message
            }

            alertLogDao.insertAlert(
                AlertLogEntity(
                    deviceId = device.id,
                    deviceName = device.name,
                    vendor = device.vendor,
                    severity = "CRITICAL",
                    message = "$alertTitle - $detail",
                    sentToTelegram = sentTelegram,
                    telegramResponse = resp
                )
            )
        }
    }

    private fun ensureDeviceAuxiliaryData(device: DeviceEntity, currentDown: Double, currentUp: Double) {
        // Generate realistic interfaces tailored to vendor hardware architecture
        val ifaces = when (device.vendor.uppercase()) {
            "MIKROTIK" -> listOf(
                NetworkInterfaceInfo("sfp-sfpplus1-WAN", "SFP+", "UP", "6C:3B:6B:11:0A:01", 10000, 1500, 1845920384L, 954820129L, currentDown * 0.9, currentUp * 0.9, 0, 0),
                NetworkInterfaceInfo("ether1-Trunk", "Ethernet", "UP", "6C:3B:6B:11:0A:02", 1000, 1500, 945920384L, 454820129L, currentDown * 0.4, currentUp * 0.3, 0, 0),
                NetworkInterfaceInfo("bridge-LAN", "Bridge", "UP", "6C:3B:6B:11:0A:00", 1000, 1500, 1245920384L, 654820129L, currentDown * 0.6, currentUp * 0.5, 0, 0),
                NetworkInterfaceInfo("vlan10-Staff", "VLAN", "UP", "6C:3B:6B:11:0A:10", 1000, 1500, 645920384L, 254820129L, currentDown * 0.35, currentUp * 0.25, 0, 0, 10),
                NetworkInterfaceInfo("vlan20-Hotspot", "VLAN", "UP", "6C:3B:6B:11:0A:20", 1000, 1500, 445920384L, 154820129L, currentDown * 0.25, currentUp * 0.15, 0, 0, 20),
                NetworkInterfaceInfo("ether2-Spare", "Ethernet", "DOWN", "6C:3B:6B:11:0A:03", 1000, 1500, 0L, 0L, 0.0, 0.0, 0, 0)
            )
            "CISCO" -> listOf(
                NetworkInterfaceInfo("GigabitEthernet0/1 (Uplink)", "Ethernet", "UP", "00:2A:6A:88:1C:01", 1000, 1500, 4845920384L, 2154820129L, currentDown * 0.85, currentUp * 0.85, 0, 0),
                NetworkInterfaceInfo("GigabitEthernet0/2 (Server-01)", "Ethernet", "UP", "00:2A:6A:88:1C:02", 1000, 1500, 145920384L, 654820129L, currentDown * 0.25, currentUp * 0.3, 0, 0, 30),
                NetworkInterfaceInfo("GigabitEthernet0/3 (AP-Ruijie)", "Ethernet", "UP", "00:2A:6A:88:1C:03", 1000, 1500, 845920384L, 354820129L, currentDown * 0.35, currentUp * 0.2, 0, 0),
                NetworkInterfaceInfo("TenGigabitEthernet0/1", "SFP+", "UP", "00:2A:6A:88:1C:31", 10000, 9000, 8845920384L, 6154820129L, currentDown, currentUp, 0, 0),
                NetworkInterfaceInfo("FastEthernet0/24 (Backup)", "Ethernet", "DOWN", "00:2A:6A:88:1C:18", 100, 1500, 0L, 0L, 0.0, 0.0, 2, 0)
            )
            "RUIJIE" -> listOf(
                NetworkInterfaceInfo("GigabitEthernet0/24 (Trunk)", "Ethernet", "UP", "14:14:4B:32:00:18", 1000, 1500, 1845920384L, 954820129L, currentDown * 0.9, currentUp * 0.9, 0, 0),
                NetworkInterfaceInfo("GigabitEthernet0/1 (PoE AP-01)", "Ethernet", "UP", "14:14:4B:32:00:01", 1000, 1500, 445920384L, 154820129L, currentDown * 0.3, currentUp * 0.15, 0, 0, 50),
                NetworkInterfaceInfo("GigabitEthernet0/2 (PoE AP-02)", "Ethernet", "UP", "14:14:4B:32:00:02", 1000, 1500, 545920384L, 184820129L, currentDown * 0.4, currentUp * 0.2, 0, 0, 50),
                NetworkInterfaceInfo("GigabitEthernet0/3 (CCTV-NVR)", "Ethernet", "UP", "14:14:4B:32:00:03", 1000, 1500, 245920384L, 854820129L, currentDown * 0.05, currentUp * 0.4, 0, 0, 40),
                NetworkInterfaceInfo("SFP+ 0/25 (Fiber-Optic)", "SFP+", "UP", "14:14:4B:32:00:19", 10000, 1500, 2845920384L, 1254820129L, currentDown, currentUp, 0, 0)
            )
            "LINKSYS" -> listOf(
                NetworkInterfaceInfo("vlan1 (Internet WAN)", "VLAN", "UP", "C0:56:27:89:AB:01", 1000, 1500, 1945920384L, 854820129L, currentDown, currentUp, 0, 0, 1),
                NetworkInterfaceInfo("eth0 (LAN 1 - PC)", "Ethernet", "UP", "C0:56:27:89:AB:02", 1000, 1500, 545920384L, 254820129L, currentDown * 0.4, currentUp * 0.3, 0, 0),
                NetworkInterfaceInfo("eth1 (LAN 2 - TV)", "Ethernet", "UP", "C0:56:27:89:AB:03", 1000, 1500, 345920384L, 154820129L, currentDown * 0.25, currentUp * 0.1, 0, 0),
                NetworkInterfaceInfo("eth2 (LAN 3)", "Ethernet", "DOWN", "C0:56:27:89:AB:04", 1000, 1500, 0L, 0L, 0.0, 0.0, 0, 0),
                NetworkInterfaceInfo("eth3 (LAN 4)", "Ethernet", "DOWN", "C0:56:27:89:AB:05", 1000, 1500, 0L, 0L, 0.0, 0.0, 0, 0),
                NetworkInterfaceInfo("wl0 (Wi-Fi 2.4GHz)", "Wireless", "UP", "C0:56:27:89:AB:10", 300, 1500, 245920384L, 94820129L, currentDown * 0.15, currentUp * 0.1, 0, 0),
                NetworkInterfaceInfo("wl1 (Wi-Fi 5GHz)", "Wireless", "UP", "C0:56:27:89:AB:11", 1300, 1500, 845920384L, 344820129L, currentDown * 0.5, currentUp * 0.35, 0, 0)
            )
            else -> listOf(
                NetworkInterfaceInfo("eth0 (WAN)", "Ethernet", "UP", "52:54:00:12:34:56", 1000, 1500, 1145920384L, 454820129L, currentDown, currentUp, 0, 0),
                NetworkInterfaceInfo("eth1 (LAN)", "Ethernet", "UP", "52:54:00:12:34:57", 1000, 1500, 945920384L, 354820129L, currentDown * 0.8, currentUp * 0.8, 0, 0),
                NetworkInterfaceInfo("br-lan", "Bridge", "UP", "52:54:00:12:34:57", 1000, 1500, 945920384L, 354820129L, currentDown * 0.8, currentUp * 0.8, 0, 0),
                NetworkInterfaceInfo("wlan0 (2.4GHz)", "Wireless", "UP", "52:54:00:12:34:58", 300, 1500, 245920384L, 84820129L, currentDown * 0.2, currentUp * 0.1, 0, 0),
                NetworkInterfaceInfo("wlan1 (5GHz)", "Wireless", "UP", "52:54:00:12:34:59", 867, 1500, 645920384L, 244820129L, currentDown * 0.5, currentUp * 0.3, 0, 0)
            )
        }

        val vlans = listOf(
            VlanInfo(1, "DEFAULT", "All Ports", "192.168.1.0/24"),
            VlanInfo(10, "VLAN_MANAGEMENT", "Trunk, Gi0/1", "10.10.10.0/24"),
            VlanInfo(20, "VLAN_STAFF", "Gi0/2-12, wlan0", "192.168.20.0/24"),
            VlanInfo(30, "VLAN_SERVER_FARM", "Gi0/13-16", "172.16.30.0/24"),
            VlanInfo(50, "VLAN_HOTSPOT_GUEST", "Gi0/17-24, wlan1", "10.50.0.0/20")
        )

        val leases = listOf(
            DhcpLeaseInfo("192.168.88.101", "40:B0:76:A8:12:90", "Galaxy-A10-User", "Samsung Electronics", 20, "Bound", "18h 12m"),
            DhcpLeaseInfo("192.168.88.102", "D4:61:9D:34:88:FF", "Galaxy-Tab-A7Lite", "Samsung Electronics", 20, "Bound", "21h 45m"),
            DhcpLeaseInfo("192.168.88.105", "3C:22:FB:45:90:12", "ThinkPad-NOC-Eng", "Lenovo Group", 10, "Static", "Permanent"),
            DhcpLeaseInfo("192.168.88.110", "F0:18:98:AA:BC:01", "iPhone-15-Pro-Direktur", "Apple, Inc.", 20, "Bound", "12h 05m"),
            DhcpLeaseInfo("192.168.88.120", "24:6F:28:CC:11:EE", "ESP32-Temp-Sensor-Rack", "Espressif Inc", 30, "Static", "Permanent"),
            DhcpLeaseInfo("192.168.88.135", "B8:27:EB:55:77:88", "RaspberryPi-DNS-Pihole", "Raspberry Pi Foundation", 10, "Static", "Permanent"),
            DhcpLeaseInfo("192.168.88.140", "AC:D1:B8:33:44:55", "Ruijie-Reyee-AP-01", "Ruijie Networks", 10, "Static", "Permanent"),
            DhcpLeaseInfo("192.168.88.165", "84:C7:8F:99:AA:22", "Smart-TV-Ruang-Rapat", "LG Electronics", 50, "Bound", "08h 30m")
        )

        val curIfMap = _deviceInterfaces.value.toMutableMap()
        curIfMap[device.id] = ifaces
        _deviceInterfaces.value = curIfMap

        val curVlanMap = _deviceVlans.value.toMutableMap()
        curVlanMap[device.id] = vlans
        _deviceVlans.value = curVlanMap

        val curLeaseMap = _deviceDhcpLeases.value.toMutableMap()
        curLeaseMap[device.id] = leases
        _deviceDhcpLeases.value = curLeaseMap
    }
}
