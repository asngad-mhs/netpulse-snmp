package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AlertLogEntity
import com.example.data.model.DeviceEntity
import com.example.data.model.TelegramConfigEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [DeviceEntity::class, TelegramConfigEntity::class, AlertLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun telegramConfigDao(): TelegramConfigDao
    abstract fun alertLogDao(): AlertLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "netpulse_snmp_database.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            suspend fun populateInitialData(database: AppDatabase) {
                val deviceDao = database.deviceDao()
                val configDao = database.telegramConfigDao()
                val alertDao = database.alertLogDao()

                // Insert the 4 vendor devices requested by the user
                val initialDevices = listOf(
                    DeviceEntity(
                        id = 1,
                        name = "MikroTik CCR2004-1G-12S+2XS",
                        vendor = "MikroTik",
                        host = "192.168.88.1",
                        port = 161,
                        community = "public",
                        snmpVersion = 1,
                        pollingIntervalSeconds = 3,
                        isEnabled = true,
                        isOnline = true,
                        isSimulated = true,
                        cpuUsage = 24,
                        memoryUsage = 38,
                        uploadSpeedKbps = 2450.0,
                        downloadSpeedKbps = 18600.0,
                        clientCount = 86,
                        uptimeSeconds = 345600L, // 4 days
                        sysDescr = "RouterOS v7.14.3 (tile) CCR2004-1G-12S+2XS #1",
                        sysLocation = "Server Rack A01 - Ruang NOC"
                    ),
                    DeviceEntity(
                        id = 2,
                        name = "Cisco Catalyst 2960-X 48-Port",
                        vendor = "Cisco",
                        host = "10.10.1.2",
                        port = 161,
                        community = "cisco_public",
                        snmpVersion = 1,
                        pollingIntervalSeconds = 3,
                        isEnabled = true,
                        isOnline = true,
                        isSimulated = true,
                        cpuUsage = 15,
                        memoryUsage = 42,
                        uploadSpeedKbps = 4120.0,
                        downloadSpeedKbps = 32800.0,
                        clientCount = 142,
                        uptimeSeconds = 1209600L, // 14 days
                        sysDescr = "Cisco IOS Software, C2960X Software (C2960X-UNIVERSALK9-M), Version 15.2(7)E3",
                        sysLocation = "MDF Lantai 1 Gedung Rektorat"
                    ),
                    DeviceEntity(
                        id = 3,
                        name = "Ruijie Reyee RG-NBS3100-24GT",
                        vendor = "Ruijie",
                        host = "192.168.10.254",
                        port = 161,
                        community = "ruijie_snmp",
                        snmpVersion = 1,
                        pollingIntervalSeconds = 3,
                        isEnabled = true,
                        isOnline = true,
                        isSimulated = true,
                        cpuUsage = 32,
                        memoryUsage = 45,
                        uploadSpeedKbps = 1850.0,
                        downloadSpeedKbps = 14200.0,
                        clientCount = 54,
                        uptimeSeconds = 864000L, // 10 days
                        sysDescr = "Ruijie RGOS 11.4(1)B75P1 RG-NBS3100-24GT4SFP Managed Switch",
                        sysLocation = "IDF Lantai 2 Sayap Barat"
                    ),
                    DeviceEntity(
                        id = 4,
                        name = "OpenWrt 23.05 x86 Gateway",
                        vendor = "OpenWrt",
                        host = "192.168.1.1",
                        port = 161,
                        community = "public",
                        snmpVersion = 1,
                        pollingIntervalSeconds = 3,
                        isEnabled = true,
                        isOnline = true,
                        isSimulated = true,
                        cpuUsage = 9,
                        memoryUsage = 21,
                        uploadSpeedKbps = 980.0,
                        downloadSpeedKbps = 6300.0,
                        clientCount = 27,
                        uptimeSeconds = 2592000L, // 30 days
                        sysDescr = "Linux OpenWrt 5.15.150 #0 SMP x86_64 GNU/Linux Net-SNMP 5.9.1",
                        sysLocation = "Lab Jaringan & Komputer Kampus"
                    ),
                    DeviceEntity(
                        id = 5,
                        name = "Linksys WRT3200ACM Smart Wi-Fi",
                        vendor = "Linksys",
                        host = "192.168.1.1",
                        port = 161,
                        community = "public",
                        snmpVersion = 1,
                        pollingIntervalSeconds = 3,
                        isEnabled = true,
                        isOnline = true,
                        isSimulated = true,
                        cpuUsage = 21,
                        memoryUsage = 36,
                        uploadSpeedKbps = 1640.0,
                        downloadSpeedKbps = 11200.0,
                        clientCount = 34,
                        uptimeSeconds = 432000L, // 5 days
                        sysDescr = "Linksys WRT3200ACM Dual-Band Wi-Fi Router Firmware 1.0.8",
                        sysLocation = "Ruang Laboratorium Jaringan Multimedia"
                    )
                )

                deviceDao.insertAll(initialDevices)

                // Default Telegram setup guide
                configDao.saveConfig(
                    TelegramConfigEntity(
                        id = 1,
                        botToken = "",
                        chatId = "",
                        isEnabled = false,
                        cpuThreshold = 85,
                        bandwidthThresholdMbps = 100.0,
                        alertOnInterfaceDown = true,
                        alertOnDeviceDown = true
                    )
                )

                // Welcome alert
                alertDao.insertAlert(
                    AlertLogEntity(
                        timestamp = System.currentTimeMillis() - 3600000L,
                        deviceId = 1,
                        deviceName = "MikroTik CCR2004",
                        vendor = "MikroTik",
                        severity = "INFO",
                        message = "Sistem NetPulse SNMP berhasil diinisialisasi untuk pemantauan multi-vendor.",
                        sentToTelegram = false
                    )
                )
            }
        }
    }
}
