package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "master_bobot")
data class MasterBobotEntity(
    @PrimaryKey
    val itemId: String,
    val kodeUnik: String = "",
    val itemName: String,
    val sectionTitle: String,
    val groupTitle: String,
    val pointMax: Int,
    val bobot: Double = 1.0,
    val updatedAt: Long = System.currentTimeMillis()
)
