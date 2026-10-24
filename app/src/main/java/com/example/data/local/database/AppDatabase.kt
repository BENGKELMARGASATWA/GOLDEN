package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.DataAuditDao
import com.example.data.local.dao.MasterBobotDao
import com.example.data.local.dao.MasterGradeKpiDao
import com.example.data.local.dao.MasterKendaraanDao
import com.example.data.local.dao.MasterPeriodeDao
import com.example.data.local.dao.MasterPoolDao
import com.example.data.local.dao.MasterSampleDao
import com.example.data.local.dao.PengaturanNamaPhotoDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.DataAuditEntity
import com.example.data.local.entity.MasterBobotEntity
import com.example.data.local.entity.MasterGradeKpiEntity
import com.example.data.local.entity.MasterKendaraanEntity
import com.example.data.local.entity.MasterPeriodeEntity
import com.example.data.local.entity.MasterPoolEntity
import com.example.data.local.entity.MasterSampleEntity
import com.example.data.local.entity.PengaturanNamaPhotoEntity
import com.example.data.local.entity.UserEntity
import com.example.util.UniqueCodeGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE data_audit ADD COLUMN jenisSample TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE data_audit ADD COLUMN sampleTanggal TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE data_audit ADD COLUMN paketService TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `master_periode` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `periode` TEXT NOT NULL,
                `keterangan` TEXT NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `users` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'AKTIF'")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `master_bobot` (
                `itemId` TEXT NOT NULL,
                `itemName` TEXT NOT NULL,
                `sectionTitle` TEXT NOT NULL,
                `groupTitle` TEXT NOT NULL,
                `pointMax` INTEGER NOT NULL,
                `bobot` REAL NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`itemId`)
            )
        """.trimIndent())
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `users` ADD COLUMN `tanggalAktif` TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `master_pool` ADD COLUMN `statusArea` TEXT NOT NULL DEFAULT 'JABODETABEK'")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `master_sample` (
                `statusArea` TEXT NOT NULL,
                `jumlahSample` INTEGER NOT NULL,
                `keterangan` TEXT NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`statusArea`)
            )
        """.trimIndent())
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `data_audit` ADD COLUMN `cloudId` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `data_audit` ADD COLUMN `auditUuid` TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `master_grade_kpi` (
                `kategori` TEXT NOT NULL,
                `minScore` REAL NOT NULL,
                `maxScore` REAL NOT NULL,
                `keterangan` TEXT NOT NULL DEFAULT '',
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`kategori`)
            )
        """.trimIndent())
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE `users` ADD COLUMN `kodeUnik` TEXT NOT NULL DEFAULT ''")
        } catch (e: Exception) {}
        try {
            db.execSQL("ALTER TABLE `master_pool` ADD COLUMN `kodeUnik` TEXT NOT NULL DEFAULT ''")
        } catch (e: Exception) {}
        try {
            db.execSQL("ALTER TABLE `master_kendaraan` ADD COLUMN `kodeUnik` TEXT NOT NULL DEFAULT ''")
        } catch (e: Exception) {}
        try {
            db.execSQL("ALTER TABLE `data_audit` ADD COLUMN `kodeUnik` TEXT NOT NULL DEFAULT ''")
        } catch (e: Exception) {}
        try {
            db.execSQL("ALTER TABLE `master_periode` ADD COLUMN `kodeUnik` TEXT NOT NULL DEFAULT ''")
        } catch (e: Exception) {}
        try {
            db.execSQL("ALTER TABLE `master_bobot` ADD COLUMN `kodeUnik` TEXT NOT NULL DEFAULT ''")
        } catch (e: Exception) {}
        try {
            db.execSQL("ALTER TABLE `master_sample` ADD COLUMN `kodeUnik` TEXT NOT NULL DEFAULT ''")
        } catch (e: Exception) {}
        try {
            db.execSQL("ALTER TABLE `master_grade_kpi` ADD COLUMN `kodeUnik` TEXT NOT NULL DEFAULT ''")
        } catch (e: Exception) {}
    }
}

@Database(
    entities = [
        UserEntity::class,
        MasterPoolEntity::class,
        MasterKendaraanEntity::class,
        DataAuditEntity::class,
        PengaturanNamaPhotoEntity::class,
        MasterPeriodeEntity::class,
        MasterBobotEntity::class,
        MasterSampleEntity::class,
        MasterGradeKpiEntity::class
    ],
    version = 14,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun masterPoolDao(): MasterPoolDao
    abstract fun masterKendaraanDao(): MasterKendaraanDao
    abstract fun dataAuditDao(): DataAuditDao
    abstract fun pengaturanNamaPhotoDao(): PengaturanNamaPhotoDao
    abstract fun masterPeriodeDao(): MasterPeriodeDao
    abstract fun masterBobotDao(): MasterBobotDao
    abstract fun masterSampleDao(): MasterSampleDao
    abstract fun masterGradeKpiDao(): MasterGradeKpiDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "audit_perawatan_mobil.db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDatabase(database)
                    }
                }
            }
        }

        suspend fun seedDatabase(database: AppDatabase) {
            // Hapus semua username lokal default (superadmin, admin, kabeng, masterdata, skt, auditor)
            try {
                val targetNips = listOf("1001", "1002", "1003", "1004", "3001", "2001")
                val targetUsernames = listOf("superadmin", "admin", "kabeng", "masterdata", "skt", "auditor")
                database.userDao().deleteUsersByNipOrUsername(targetNips, targetUsernames)
            } catch (e: Exception) {
                // Ignore migration differences
            }

            // Bersihkan data demo lama (audit dummy & kendaraan dummy) agar data bersih saat loading lambat
            try {
                database.dataAuditDao().deleteDemoAudits()
                database.masterKendaraanDao().deleteDemoKendaraan()
            } catch (e: Exception) {
                // Ignore migration differences
            }

            // Seed 30 Master Pools
            if (database.masterPoolDao().getPoolCount() == 0) {
                val pools = mutableListOf<MasterPoolEntity>()
                pools.add(
                    MasterPoolEntity(
                        id = 1,
                        kodeUnik = UniqueCodeGenerator.generatePoolCode(),
                        kodePool = "SKT",
                        namaPool = "POOL SKT PUSAT",
                        alamatPool = "Jl. Raya Industri Utama No. 1, Jakarta",
                        statusArea = "JABODETABEK"
                    )
                )
                for (i in 1..30) {
                    val code = String.format("POOL-%02d", i)
                    val cities = listOf(
                        "Jakarta Barat", "Jakarta Timur", "Jakarta Selatan", "Jakarta Utara", "Bekasi",
                        "Tangerang", "Bogor", "Depok", "Bandung", "Cirebon",
                        "Semarang", "Solo", "Yogyakarta", "Surabaya", "Malang",
                        "Denpasar", "Mataram", "Medan", "Palembang", "Pekanbaru",
                        "Padang", "Bandar Lampung", "Batam", "Pontianak", "Banjarmasin",
                        "Balikpapan", "Samarinda", "Makassar", "Manado", "Ambon"
                    )
                    val cityName = cities[(i - 1) % cities.size]
                    val area = when {
                        cityName.startsWith("Jakarta") -> "JABODETABEK"
                        listOf("Bekasi", "Tangerang", "Bogor", "Depok").contains(cityName) -> "JABODETABEK SATELIT"
                        listOf("Bandung", "Semarang", "Solo", "Yogyakarta", "Surabaya", "Medan", "Makassar", "Palembang").contains(cityName) -> "DAERAH"
                        else -> "DAERAH SATELIT"
                    }
                    pools.add(
                        MasterPoolEntity(
                            id = i + 1,
                            kodeUnik = UniqueCodeGenerator.generatePoolCode(),
                            kodePool = code,
                            namaPool = "POOL $code (${cityName.uppercase()})",
                            alamatPool = "Kawasan Operasional Armada $cityName No. $i",
                            statusArea = area
                        )
                    )
                }
                database.masterPoolDao().insertPools(pools)
            }

            // Seed 20 Photo Titles (PENGATURAN_NAMAPHOTO)
            if (database.pengaturanNamaPhotoDao().getCount() == 0) {
                val photoTitles = listOf(
                    "Tampak Depan Penuh & Grill",
                    "Lampu Utama & Foglamp Depan",
                    "Bumper Depan & Plat Nomer Depan",
                    "Kap Mesin & Ruang Mesin Bersih",
                    "Kondisi Kaca Depan & Wiper",
                    "Ban & Velg Depan Kanan (Cek Tekanan)",
                    "Bodi, Pintu & Spion Kanan",
                    "Ban & Velg Belakang Kanan",
                    "Tampak Belakang & Plat Nomer Belakang",
                    "Lampu Belakang & Reflektor",
                    "Pintu Bagasi & Ruang Kargo",
                    "Ban & Velg Belakang Kiri",
                    "Bodi, Pintu & Spion Kiri",
                    "Ban & Velg Depan Kiri",
                    "Atap Kendaraan & Antena",
                    "Interior Dashboard & Odometer/Speedometer",
                    "Kemudi / Setir & Tuas Transmisi",
                    "Jok Baris Depan & Belakang + Seatbelt",
                    "Kolong Mobil, Knalpot & Suspensi",
                    "Kondisi Ban Cadangan / Toolkit / Apar"
                )
                val photoEntities = photoTitles.mapIndexed { index, title ->
                    PengaturanNamaPhotoEntity(
                        id = index + 1,
                        nama = "photo${index + 1}",
                        tampilan = "${index + 1}. $title"
                    )
                }
                database.pengaturanNamaPhotoDao().insertAll(photoEntities)
            }

            if (database.masterPeriodeDao().getCount() == 0) {
                val initialPeriodes = listOf(
                    MasterPeriodeEntity(kodeUnik = UniqueCodeGenerator.generatePeriodeCode(), periode = "Periode 1", keterangan = "Audit Periode 1 (Minggu 1)"),
                    MasterPeriodeEntity(kodeUnik = UniqueCodeGenerator.generatePeriodeCode(), periode = "Periode 2", keterangan = "Audit Periode 2 (Minggu 2)"),
                    MasterPeriodeEntity(kodeUnik = UniqueCodeGenerator.generatePeriodeCode(), periode = "Periode 3", keterangan = "Audit Periode 3 (Minggu 3)"),
                    MasterPeriodeEntity(kodeUnik = UniqueCodeGenerator.generatePeriodeCode(), periode = "Periode 4", keterangan = "Audit Periode 4 (Minggu 4)"),
                    MasterPeriodeEntity(kodeUnik = UniqueCodeGenerator.generatePeriodeCode(), periode = "Mingguan", keterangan = "Audit Rutin Mingguan"),
                    MasterPeriodeEntity(kodeUnik = UniqueCodeGenerator.generatePeriodeCode(), periode = "Bulanan", keterangan = "Audit Rutin Bulanan")
                )
                database.masterPeriodeDao().insertAll(initialPeriodes)
            }

            // Seed Master Bobot from KpiAuditSpec (Default bobot 1.0)
            if (database.masterBobotDao().getCount() == 0) {
                val bobotList = mutableListOf<MasterBobotEntity>()
                com.example.data.model.KpiAuditSpec.sections.forEach { section ->
                    section.groups.forEach { group ->
                        group.items.forEach { item ->
                            bobotList.add(
                                MasterBobotEntity(
                                    itemId = item.id,
                                    kodeUnik = UniqueCodeGenerator.generateBobotCode(),
                                    itemName = item.name,
                                    sectionTitle = section.title,
                                    groupTitle = group.title,
                                    pointMax = item.maxPoints,
                                    bobot = item.defaultBobot
                                )
                            )
                        }
                    }
                }
                database.masterBobotDao().insertAll(bobotList)
            }

            // Seed Master Sample based on Status Area (Default sample counts for Super Admin)
            if (database.masterSampleDao().getCount() == 0) {
                val defaultSamples = listOf(
                    MasterSampleEntity(
                        statusArea = "JABODETABEK",
                        kodeUnik = UniqueCodeGenerator.generateSampleCode(),
                        jumlahSample = 20,
                        keterangan = "Standar sample armada area Jabodetabek"
                    ),
                    MasterSampleEntity(
                        statusArea = "JABODETABEK SATELIT",
                        kodeUnik = UniqueCodeGenerator.generateSampleCode(),
                        jumlahSample = 15,
                        keterangan = "Standar sample armada area Jabodetabek Satelit"
                    ),
                    MasterSampleEntity(
                        statusArea = "DAERAH",
                        kodeUnik = UniqueCodeGenerator.generateSampleCode(),
                        jumlahSample = 10,
                        keterangan = "Standar sample armada area Daerah"
                    ),
                    MasterSampleEntity(
                        statusArea = "DAERAH SATELIT",
                        kodeUnik = UniqueCodeGenerator.generateSampleCode(),
                        jumlahSample = 5,
                        keterangan = "Standar sample armada area Daerah Satelit"
                    )
                )
                database.masterSampleDao().insertAll(defaultSamples)
            }

            // Seed Master Grade KPI (Default struktur: A=70-80, B=60-69.99, C=50-59.99, D=30-49.99, E=0-29.99)
            if (database.masterGradeKpiDao().getCount() == 0) {
                val defaultGrades = listOf(
                    MasterGradeKpiEntity(
                        kategori = "A",
                        kodeUnik = UniqueCodeGenerator.generateGradeCode(),
                        minScore = 70.00,
                        maxScore = 80.00,
                        keterangan = "Sangat Baik (Grade A)"
                    ),
                    MasterGradeKpiEntity(
                        kategori = "B",
                        kodeUnik = UniqueCodeGenerator.generateGradeCode(),
                        minScore = 60.00,
                        maxScore = 69.99,
                        keterangan = "Baik (Grade B)"
                    ),
                    MasterGradeKpiEntity(
                        kategori = "C",
                        kodeUnik = UniqueCodeGenerator.generateGradeCode(),
                        minScore = 50.00,
                        maxScore = 59.99,
                        keterangan = "Cukup (Grade C)"
                    ),
                    MasterGradeKpiEntity(
                        kategori = "D",
                        kodeUnik = UniqueCodeGenerator.generateGradeCode(),
                        minScore = 30.00,
                        maxScore = 49.99,
                        keterangan = "Kurang (Grade D)"
                    ),
                    MasterGradeKpiEntity(
                        kategori = "E",
                        kodeUnik = UniqueCodeGenerator.generateGradeCode(),
                        minScore = 0.00,
                        maxScore = 29.99,
                        keterangan = "Kritis / Perlu Perbaikan (Grade E)"
                    )
                )
                database.masterGradeKpiDao().insertAll(defaultGrades)
            }
        }
    }
}
