package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagnosticDao {
    @Query("SELECT * FROM diagnostics ORDER BY dateMillis DESC")
    fun getAllDiagnostics(): Flow<List<DiagnosticEntity>>

    @Query("SELECT * FROM diagnostics WHERE id = :id LIMIT 1")
    suspend fun getDiagnosticById(id: String): DiagnosticEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiagnostic(diagnostic: DiagnosticEntity)

    @Update
    suspend fun updateDiagnostic(diagnostic: DiagnosticEntity)

    @Query("UPDATE diagnostics SET statusResolved = :status WHERE id = :id")
    suspend fun updateDiagnosticStatus(id: String, status: Boolean)

    @Query("DELETE FROM diagnostics WHERE id = :id")
    suspend fun deleteDiagnosticById(id: String)

    @Query("DELETE FROM diagnostics")
    suspend fun clearAllDiagnostics()
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY id DESC")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity)

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun deleteDeviceById(id: Int)
}
