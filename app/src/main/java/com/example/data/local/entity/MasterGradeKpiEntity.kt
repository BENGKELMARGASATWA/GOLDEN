package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity untuk menyimpan konfigurasi Grade KPI di database lokal Room.
 * Tabel ini berisi Kategori (A, B, C, D, E) dan rentang nilai desimal (0,00 s/d 80,00).
 */
@Entity(tableName = "master_grade_kpi")
data class MasterGradeKpiEntity(
    @PrimaryKey
    val kategori: String, // "A", "B", "C", "D", "E"
    val minScore: Double, // 0.00 s/d 80.00
    val maxScore: Double, // 0.00 s/d 80.00
    val keterangan: String = "",
    val kodeUnik: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
