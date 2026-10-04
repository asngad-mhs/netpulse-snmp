package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AlertLogEntity
import com.example.data.model.DeviceEntity
import com.example.data.model.TelegramConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY id ASC")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE id = :id LIMIT 1")
    suspend fun getDeviceById(id: Int): DeviceEntity?

    @Query("SELECT * FROM devices WHERE isEnabled = 1")
    suspend fun getEnabledDevices(): List<DeviceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(devices: List<DeviceEntity>)

    @Update
    suspend fun updateDevice(device: DeviceEntity)

    @Delete
    suspend fun deleteDevice(device: DeviceEntity)

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun deleteDeviceById(id: Int)
}

@Dao
interface TelegramConfigDao {
    @Query("SELECT * FROM telegram_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<TelegramConfigEntity?>

    @Query("SELECT * FROM telegram_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): TelegramConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: TelegramConfigEntity)
}

@Dao
interface AlertLogDao {
    @Query("SELECT * FROM alert_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAlertsFlow(): Flow<List<AlertLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertLogEntity): Long

    @Query("DELETE FROM alert_logs")
    suspend fun clearAll()
}
