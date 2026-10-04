package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val vendor: String, // MikroTik, Cisco, Ruijie, OpenWrt, Generic
    val host: String,
    val port: Int = 161,
    val community: String = "public",
    val snmpVersion: Int = 1, // 0 = v1, 1 = v2c
    val pollingIntervalSeconds: Int = 3,
    val isEnabled: Boolean = true,
    val isOnline: Boolean = true,
    val isSimulated: Boolean = true, // default true so emulator gets live interactive telemetry right away
    val cpuUsage: Int = 18,
    val memoryUsage: Int = 34,
    val uploadSpeedKbps: Double = 1240.0,
    val downloadSpeedKbps: Double = 8450.0,
    val clientCount: Int = 38,
    val uptimeSeconds: Long = 184520L,
    val sysDescr: String = "",
    val sysLocation: String = "Data Center Rack A-02",
    val lastUpdated: Long = System.currentTimeMillis()
)

data class NetworkInterfaceInfo(
    val name: String,
    val type: String, // Ethernet, SFP+, Wireless, Bridge, VLAN
    val status: String, // UP, DOWN, DORMANT
    val macAddress: String,
    val speedMbps: Long,
    val mtu: Int,
    val inOctets: Long,
    val outOctets: Long,
    val inRateKbps: Double,
    val outRateKbps: Double,
    val inErrors: Long = 0,
    val outErrors: Long = 0,
    val vlanId: Int? = null
)

data class VlanInfo(
    val vlanId: Int,
    val name: String,
    val ports: String,
    val subnet: String,
    val status: String = "ACTIVE"
)

data class DhcpLeaseInfo(
    val ipAddress: String,
    val macAddress: String,
    val hostname: String,
    val clientVendor: String,
    val vlanId: Int,
    val status: String = "Bound",
    val leaseDuration: String = "23h 45m"
)

data class MetricPoint(
    val timestamp: Long,
    val uploadKbps: Double,
    val downloadKbps: Double,
    val cpuPercent: Int
)

@Entity(tableName = "telegram_config")
data class TelegramConfigEntity(
    @PrimaryKey val id: Int = 1,
    val botToken: String = "",
    val chatId: String = "",
    val isEnabled: Boolean = false,
    val cpuThreshold: Int = 85,
    val bandwidthThresholdMbps: Double = 100.0,
    val alertOnInterfaceDown: Boolean = true,
    val alertOnDeviceDown: Boolean = true
)

@Entity(tableName = "alert_logs")
data class AlertLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: Int,
    val deviceName: String,
    val vendor: String,
    val severity: String, // INFO, WARNING, CRITICAL
    val message: String,
    val sentToTelegram: Boolean = false,
    val telegramResponse: String? = null
)
