package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnostics")
data class DiagnosticEntity(
    @PrimaryKey val id: String,
    val dateMillis: Long,
    val deviceName: String,
    val problemDetected: String,
    val category: String,
    val difficulty: String,
    val confidenceProbability: Int,
    val stepsJson: String, // JSON array of step instructions
    val techDetailsJson: String?, // JSON string with error code, commands, etc.
    val statusResolved: Boolean? // true = Résolu, false = Toujours en panne, null = Pending
)

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, // e.g. "Mon PC Gaming"
    val brand: String, // e.g. "ASUS"
    val model: String, // e.g. "ROG Strix G15"
    val os: String, // e.g. "Windows 11 64-bit"
    val ram: String, // e.g. "16 GB DDR4"
    val storage: String, // e.g. "1TB NVMe SSD"
    val cpu: String, // e.g. "Intel Core i7-12700H"
    val gpu: String // e.g. "NVIDIA RTX 3060"
)
