package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.AuthManager
import com.example.data.model.AlertLogEntity
import com.example.data.model.DeviceEntity
import com.example.data.model.DhcpLeaseInfo
import com.example.data.model.MetricPoint
import com.example.data.model.NetworkInterfaceInfo
import com.example.data.model.TelegramConfigEntity
import com.example.data.model.VlanInfo
import com.example.data.repository.NetworkRepository
import com.example.snmp.SnmpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppNavDestination(val label: String) {
    DASHBOARD("Dashboard"),
    DEVICES("Perangkat"),
    INTERFACES("Interface"),
    VLAN_DHCP("VLAN & DHCP"),
    ALERTS("Notifikasi & Log"),
    SETTINGS("Pengaturan")
}

class NetworkViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = NetworkRepository(
        deviceDao = database.deviceDao(),
        telegramConfigDao = database.telegramConfigDao(),
        alertLogDao = database.alertLogDao()
    )

    init {
        repository.startMonitoring(viewModelScope)
    }

    val allDevices: StateFlow<List<DeviceEntity>> = repository.allDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val telegramConfig: StateFlow<TelegramConfigEntity?> = repository.telegramConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val alertLogs: StateFlow<List<AlertLogEntity>> = repository.alertLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val metricsHistory: StateFlow<Map<Int, List<MetricPoint>>> = repository.deviceMetricsHistory
    val deviceInterfaces: StateFlow<Map<Int, List<NetworkInterfaceInfo>>> = repository.deviceInterfaces
    val deviceVlans: StateFlow<Map<Int, List<VlanInfo>>> = repository.deviceVlans
    val deviceDhcpLeases: StateFlow<Map<Int, List<DhcpLeaseInfo>>> = repository.deviceDhcpLeases

    // Auth & Session Management
    private val authManager = AuthManager(application)
    private val _isLoggedIn = MutableStateFlow(authManager.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow(authManager.getCurrentUser())
    val currentUser: StateFlow<String> = _currentUser.asStateFlow()

    private val _rememberMe = MutableStateFlow(authManager.isRememberMe())
    val rememberMe: StateFlow<Boolean> = _rememberMe.asStateFlow()

    private val _savedUsername = MutableStateFlow(authManager.getSavedUsername())
    val savedUsername: StateFlow<String> = _savedUsername.asStateFlow()

    fun login(username: String, pass: String, remember: Boolean): Boolean {
        val success = authManager.authenticate(username, pass, remember)
        if (success) {
            _isLoggedIn.value = true
            _currentUser.value = authManager.getCurrentUser()
            _rememberMe.value = remember
            _savedUsername.value = username
        }
        return success
    }

    fun logout() {
        authManager.logout()
        _isLoggedIn.value = false
        _currentScreen.value = AppNavDestination.DASHBOARD
    }

    fun changePassword(oldPass: String, newPass: String): Boolean {
        return authManager.changePassword(oldPass, newPass)
    }

    // Navigation and filters
    private val _currentScreen = MutableStateFlow(AppNavDestination.DASHBOARD)
    val currentScreen = _currentScreen.asStateFlow()

    private val _selectedVendorFilter = MutableStateFlow("ALL")
    val selectedVendorFilter = _selectedVendorFilter.asStateFlow()

    private val _selectedDeviceId = MutableStateFlow<Int?>(null)
    val selectedDeviceId = _selectedDeviceId.asStateFlow()

    // Dark mode setting: "DARK", "LIGHT", "SYSTEM"
    private val _themeMode = MutableStateFlow("DARK")
    val themeMode = _themeMode.asStateFlow()

    // Filtered devices flow
    val filteredDevices: StateFlow<List<DeviceEntity>> = combine(allDevices, _selectedVendorFilter) { list, vendor ->
        if (vendor == "ALL") list else list.filter { it.vendor.equals(vendor, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected device
    val activeDevice: StateFlow<DeviceEntity?> = combine(allDevices, _selectedDeviceId) { list, id ->
        if (id == null) list.firstOrNull() else list.find { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Telegram test state
    private val _telegramTestStatus = MutableStateFlow<String?>(null)
    val telegramTestStatus = _telegramTestStatus.asStateFlow()

    private val _isTestingTelegram = MutableStateFlow(false)
    val isTestingTelegram = _isTestingTelegram.asStateFlow()

    // SNMP Diagnostic Walk tool state
    private val _snmpDiagnosticResult = MutableStateFlow<SnmpClient.SnmpResult?>(null)
    val snmpDiagnosticResult = _snmpDiagnosticResult.asStateFlow()

    private val _isPerformingSnmpQuery = MutableStateFlow(false)
    val isPerformingSnmpQuery = _isPerformingSnmpQuery.asStateFlow()

    fun navigateTo(destination: AppNavDestination) {
        _currentScreen.value = destination
    }

    fun setVendorFilter(vendor: String) {
        _selectedVendorFilter.value = vendor
    }

    fun selectDevice(id: Int) {
        _selectedDeviceId.value = id
    }

    data class ConnectionTestResult(
        val deviceId: Int?,
        val host: String,
        val port: Int,
        val success: Boolean,
        val latencyMs: Long,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _connectionTestState = MutableStateFlow<ConnectionTestResult?>(null)
    val connectionTestState = _connectionTestState.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection = _isTestingConnection.asStateFlow()

    fun testDeviceConnection(device: DeviceEntity, onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _isTestingConnection.value = true
            val res = repository.testAndPingDevice(device.id)
            val msg = if (res.success) {
                "✅ Terhubung! Latensi: ${res.latencyMs} ms (${device.vendor} @ ${device.host}:${device.port})"
            } else {
                "❌ Gagal Terhubung: ${res.errorMessage ?: "SNMP Timeout / Host Unreachable"}"
            }
            _connectionTestState.value = ConnectionTestResult(
                deviceId = device.id,
                host = device.host,
                port = device.port,
                success = res.success,
                latencyMs = res.latencyMs,
                message = msg
            )
            _isTestingConnection.value = false
            onComplete?.invoke(res.success, msg)
        }
    }

    suspend fun testArbitraryConnection(
        host: String,
        port: Int,
        community: String,
        version: Int,
        isSimulated: Boolean
    ): SnmpClient.SnmpResult {
        return repository.testDeviceConnectivity(host, port, community, version, isSimulated)
    }

    fun clearConnectionTestState() {
        _connectionTestState.value = null
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
    }

    fun saveDevice(device: DeviceEntity) {
        viewModelScope.launch {
            val savedId = repository.saveDevice(device)
            if (_selectedDeviceId.value == null || device.id == 0) {
                _selectedDeviceId.value = if (device.id != 0) device.id else savedId.toInt()
            }
            // Trigger an immediate ping check to establish initial status
            repository.testAndPingDevice(if (device.id != 0) device.id else savedId.toInt())
        }
    }

    fun deleteDevice(deviceId: Int) {
        viewModelScope.launch {
            repository.deleteDevice(deviceId)
            if (_selectedDeviceId.value == deviceId) {
                _selectedDeviceId.value = null
            }
        }
    }

    fun saveTelegramConfig(config: TelegramConfigEntity) {
        viewModelScope.launch {
            repository.saveTelegramConfig(config)
        }
    }

    fun testTelegram(botToken: String, chatId: String) {
        viewModelScope.launch {
            _isTestingTelegram.value = true
            _telegramTestStatus.value = "Mengirim uji notifikasi ke Telegram..."
            val result = repository.testTelegramNotification(botToken, chatId)
            _isTestingTelegram.value = false
            _telegramTestStatus.value = if (result.success) {
                "✅ Berhasil! Silakan periksa pesan masuk di bot Telegram Anda."
            } else {
                "❌ Gagal: ${result.message}"
            }
        }
    }

    fun clearTelegramStatus() {
        _telegramTestStatus.value = null
    }

    fun clearAlertLogs() {
        viewModelScope.launch {
            repository.clearAlerts()
        }
    }

    fun runSnmpDiagnostic(host: String, port: Int, community: String, version: Int, oid: String) {
        viewModelScope.launch {
            _isPerformingSnmpQuery.value = true
            _snmpDiagnosticResult.value = null
            val res = repository.queryCustomOid(host, port, community, version, oid)
            _snmpDiagnosticResult.value = res
            _isPerformingSnmpQuery.value = false
        }
    }

    fun sendManualAlert(deviceId: Int, title: String, message: String, severity: String) {
        viewModelScope.launch {
            repository.manualSendAlert(deviceId, title, message, severity)
        }
    }
}
