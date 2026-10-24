package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val nip: String = "",
    val kodeUnik: String = "",
    val namaPetugas: String = "",
    val username: String = "",
    val password: String = "",
    val levelAkses: String = "", // "SUPER ADMIN", "ADMIN", "AUDITOR"
    val kodePool: String = "",   // "SKT" or specific pool code like "POOL-01"
    val status: String = "AKTIF", // "AKTIF" or "BLOCKED"
    val tanggalAktif: String = "" // Khusus levelAkses AUDITOR (format dd/MM/yyyy)
)

@Entity(tableName = "master_pool")
data class MasterPoolEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val kodeUnik: String = "",
    val kodePool: String,
    val namaPool: String,
    val alamatPool: String = "",
    val statusArea: String = "JABODETABEK" // "JABODETABEK", "JABODETABEK SATELIT", "DAERAH", "DAERAH SATELIT"
)

@Entity(
    tableName = "master_kendaraan",
    indices = [
        Index(value = ["noMobil"]),
        Index(value = ["platNomer"]),
        Index(value = ["pool"])
    ]
)
data class MasterKendaraanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val kodeUnik: String = "",
    val noMobil: String,
    val platNomer: String,
    val tipe: String,
    val warna: String,
    val tahun: String = "",
    val tanggalService: String = "",
    val service: String = "",
    val pool: String
)

@Entity(tableName = "pengaturan_namaphoto")
data class PengaturanNamaPhotoEntity(
    @PrimaryKey
    val id: Int, // 1 to 20
    val nama: String, // "photo1" .. "photo20"
    val tampilan: String // Custom title
)

@Entity(tableName = "data_audit")
data class DataAuditEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cloudId: Long = 0L,
    val auditUuid: String = "",
    val kodeUnik: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val tanggal: String,
    val periode: String = "",
    val nip: String,
    val namaPetugas: String,
    val noMobil: String,
    val platNomer: String,
    val tipe: String,
    val warna: String,
    val tahun: String = "",
    val tanggalService: String = "",
    val service: String = "",
    val pool: String,
    val totalPoinTemuan: Int = 0,
    val jenisSample: String = "",
    val sampleTanggal: String = "",
    val paketService: String = "",
    val kpiScoresJson: String = "{}",
    val kpiPhotosJson: String = "{}",
    val kpiNotesJson: String = "{}",
    
    // 20 Photos and Notes (backward compatibility & Google Sheets)
    val photo1: String = "",
    val catatanPhoto1: String = "",
    val photo2: String = "",
    val catatanPhoto2: String = "",
    val photo3: String = "",
    val catatanPhoto3: String = "",
    val photo4: String = "",
    val catatanPhoto4: String = "",
    val photo5: String = "",
    val catatanPhoto5: String = "",
    val photo6: String = "",
    val catatanPhoto6: String = "",
    val photo7: String = "",
    val catatanPhoto7: String = "",
    val photo8: String = "",
    val catatanPhoto8: String = "",
    val photo9: String = "",
    val catatanPhoto9: String = "",
    val photo10: String = "",
    val catatanPhoto10: String = "",
    val photo11: String = "",
    val catatanPhoto11: String = "",
    val photo12: String = "",
    val catatanPhoto12: String = "",
    val photo13: String = "",
    val catatanPhoto13: String = "",
    val photo14: String = "",
    val catatanPhoto14: String = "",
    val photo15: String = "",
    val catatanPhoto15: String = "",
    val photo16: String = "",
    val catatanPhoto16: String = "",
    val photo17: String = "",
    val catatanPhoto17: String = "",
    val photo18: String = "",
    val catatanPhoto18: String = "",
    val photo19: String = "",
    val catatanPhoto19: String = "",
    val photo20: String = "",
    val catatanPhoto20: String = "",

    val isSynced: Boolean = false,
    val syncTimestamp: Long = 0L,
    val syncError: String? = null
)

@Entity(tableName = "master_periode")
data class MasterPeriodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val kodeUnik: String = "",
    val periode: String,
    val keterangan: String = ""
)
