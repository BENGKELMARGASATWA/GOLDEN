package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing Sample Count configuration based on Status Area (Master Pool).
 * Status Area values: "JABODETABEK", "JABODETABEK SATELIT", "DAERAH", "DAERAH SATELIT"
 * Used exclusively by Super Admin to configure sample counts for Bobot calculations.
 */
@Entity(tableName = "master_sample")
data class MasterSampleEntity(
    @PrimaryKey
    val statusArea: String,
    val kodeUnik: String = "",
    val jumlahSample: Int = 10,
    val keterangan: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
