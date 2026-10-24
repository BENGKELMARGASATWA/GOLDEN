package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.database.AppDatabase
import com.example.data.local.entity.DataAuditEntity
import com.example.data.model.AuditDatabaseSchema
import com.example.data.local.entity.MasterBobotEntity
import com.example.data.local.entity.MasterGradeKpiEntity
import com.example.data.local.entity.MasterKendaraanEntity
import com.example.data.local.entity.MasterPeriodeEntity
import com.example.data.local.entity.MasterPoolEntity
import com.example.data.local.entity.MasterSampleEntity
import com.example.data.local.entity.PengaturanNamaPhotoEntity
import com.example.data.local.entity.UserEntity
import com.example.data.session.SessionManager
import com.example.util.ImageCompressor
import com.example.util.DateUtils
import com.example.util.UniqueCodeGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.util.AuditFilterUtils
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "AuditRepository"

class AuditRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getInstance(context),
    val sessionManager: SessionManager = SessionManager(context)
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val userDao = database.userDao()
    private val poolDao = database.masterPoolDao()
    private val kendaraanDao = database.masterKendaraanDao()
    private val auditDao = database.dataAuditDao()
    private val photoSettingDao = database.pengaturanNamaPhotoDao()
    private val periodeDao = database.masterPeriodeDao()
    private val bobotDao = database.masterBobotDao()
    private val sampleDao = database.masterSampleDao()
    private val gradeKpiDao = database.masterGradeKpiDao()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    init {
        repositoryScope.launch {
            cleanDefaultLocalUsers()
        }
    }

    suspend fun cleanDefaultLocalUsers() = withContext(Dispatchers.IO) {
        try {
            val targetNips = listOf("1001", "1002", "1003", "1004", "3001", "2001")
            val targetUsernames = listOf("superadmin", "admin", "kabeng", "masterdata", "skt", "auditor")
            userDao.deleteUsersByNipOrUsername(targetNips, targetUsernames)
            val currentSession = sessionManager.getCurrentUser()
            if (currentSession != null && (currentSession.nip in targetNips || currentSession.username.lowercase() in targetUsernames)) {
                sessionManager.clearSession()
            }
            // Purge deleted KPI item IDs from master_bobot (Mahkota, Atribut Lainnya, Plastik Bening, Nomer Kaca)
            val removedKpiItemIds = listOf(
                "mahkota_kelengkapan", "mahkota_stiker_kusam", "mahkota_retak",
                "atribut_door_protector", "door_protector",
                "atribut_stiker_exterior", "kelengkapan_stiker_exterior",
                "atribut_velg_dop", "velg_dop", "velg_dop_roda_rusak",
                "plastik_bening_tidak_ada", "plastik_bening_kotor_rusak",
                "nomer_kaca_dashboard"
            )
            removedKpiItemIds.forEach { itemId ->
                bobotDao.deleteByItemId(itemId)
            }
        } catch (_: Exception) {}
    }

    // Flows
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val allPools: Flow<List<MasterPoolEntity>> = poolDao.getAllPools()
    val allKendaraan: Flow<List<MasterKendaraanEntity>> = kendaraanDao.getAllKendaraan()
    // Data hasil INPUT AUDIT tidak akan di tampilkan jika di temukan ID yang sama
    val allAudits: Flow<List<DataAuditEntity>> = auditDao.getAllAudits().map { list ->
        AuditFilterUtils.filterUniqueAudits(list)
    }
    val allPhotoSettings: Flow<List<PengaturanNamaPhotoEntity>> = photoSettingDao.getAllNamaPhoto()
    val allPeriode: Flow<List<MasterPeriodeEntity>> = periodeDao.getAllPeriode()
    val allBobot: Flow<List<MasterBobotEntity>> = bobotDao.getAllBobotFlow()
    val allSamples: Flow<List<MasterSampleEntity>> = sampleDao.getAllSamplesFlow()
    val allGradeKpi: Flow<List<MasterGradeKpiEntity>> = gradeKpiDao.getAllGradeKpiFlow()

    suspend fun updateBobot(itemId: String, bobot: Double) = withContext(Dispatchers.IO) {
        val existing = bobotDao.getBobotByItemId(itemId)
        val entity = if (existing != null) {
            bobotDao.updateBobot(itemId, bobot)
            existing.copy(bobot = bobot)
        } else {
            val kpiItem = com.example.data.model.KpiAuditSpec.findItem(itemId)
            var secTitle = ""
            var grpTitle = ""
            com.example.data.model.KpiAuditSpec.sections.forEach { s ->
                s.groups.forEach { g ->
                    if (g.items.any { it.id == itemId }) {
                        secTitle = s.title
                        grpTitle = g.title
                    }
                }
            }
            val newEntity = MasterBobotEntity(
                itemId = itemId,
                kodeUnik = UniqueCodeGenerator.generateBobotCode(),
                itemName = kpiItem?.name ?: itemId,
                sectionTitle = secTitle,
                groupTitle = grpTitle,
                pointMax = kpiItem?.maxPoints ?: 1,
                bobot = bobot
            )
            bobotDao.insert(newEntity)
            newEntity
        }
        if (sessionManager.scriptUrl.isNotBlank()) {
            repositoryScope.launch {
                try {
                    syncMasterBobotToCloud(entity)
                } catch (e: Exception) {
                    // Safe offline
                }
            }
        }
    }

    suspend fun syncAllMasterBobotToCloud(): Result<Int> = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) return@withContext Result.failure(Exception("URL Google Script belum dikonfigurasi"))
        try {
            ensureBobotSeeded()
            val list = bobotDao.getAllBobot()
            if (list.isEmpty()) return@withContext Result.failure(Exception("Data master bobot kosong"))

            val jsonArray = JSONArray()
            list.forEach { b ->
                val obj = JSONObject().apply {
                    put("itemId", b.itemId)
                    put("itemName", b.itemName)
                    put("sectionTitle", b.sectionTitle)
                    put("groupTitle", b.groupTitle)
                    put("pointMax", b.pointMax)
                    put("bobot", b.bobot)
                }
                jsonArray.put(obj)
            }

            val requestJson = JSONObject().apply {
                put("action", "saveAllBobot")
                put("bobotList", jsonArray)
            }

            val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(scriptUrl).post(body).build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(list.size)
            } else {
                Result.failure(Exception("Gagal menghubungi Google Apps Script (HTTP ${response.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllBobot(): List<MasterBobotEntity> = withContext(Dispatchers.IO) {
        bobotDao.getAllBobot()
    }

    suspend fun ensureBobotSeeded() = withContext(Dispatchers.IO) {
        val existingMap = bobotDao.getAllBobot().associateBy { it.itemId }
        val toInsert = mutableListOf<MasterBobotEntity>()
        com.example.data.model.KpiAuditSpec.sections.forEach { section ->
            section.groups.forEach { group ->
                group.items.forEach { item ->
                    val existing = existingMap[item.id]
                    if (existing == null) {
                        toInsert.add(
                            MasterBobotEntity(
                                itemId = item.id,
                                itemName = item.name,
                                sectionTitle = section.title,
                                groupTitle = group.title,
                                pointMax = item.maxPoints,
                                bobot = item.defaultBobot
                            )
                        )
                    } else if (existing.bobot != item.defaultBobot || existing.pointMax != item.maxPoints) {
                        toInsert.add(
                            existing.copy(
                                pointMax = item.maxPoints,
                                bobot = item.defaultBobot
                            )
                        )
                    }
                }
            }
        }
        if (toInsert.isNotEmpty()) {
            bobotDao.insertAll(toInsert)
        }
    }

    fun isAuditorActiveToday(user: UserEntity): Boolean {
        if (!user.levelAkses.trim().equals("AUDITOR", ignoreCase = true)) return true
        val tanggalAktif = user.tanggalAktif.trim()
        if (tanggalAktif.isBlank()) return false
        return com.example.util.DateUtils.isDateMatchingToday(tanggalAktif)
    }

    fun getAuditsForUser(user: UserEntity): Flow<List<DataAuditEntity>> {
        val userPool = user.kodePool.trim()
        val role = user.levelAkses.trim().uppercase()
        val isGlobal = role == "SUPER ADMIN" || role == "ADMIN" || role == "SKT" || role == "MASTER DATA" || userPool == "ALL" || userPool == "SKT"
        val baseFlow = if (isGlobal || userPool.isBlank()) {
            auditDao.getAllAudits()
        } else if (role == "KEPALA BENGKEL" || role.contains("KABAG")) {
            val userPoolList = userPool.split(",", ";").map { it.trim().uppercase() }.filter { it.isNotBlank() }
            if (userPoolList.size > 1) {
                auditDao.getAllAudits().map { list ->
                    list.filter { a -> userPoolList.any { it.equals(a.pool.trim(), ignoreCase = true) } }
                }
            } else {
                auditDao.getAuditsByPool(userPool)
            }
        } else {
            auditDao.getAuditsByUser(user.nip, user.namaPetugas)
        }
        return baseFlow.map { list -> AuditFilterUtils.filterUniqueAudits(list) }
    }

    suspend fun getAllAuditsList(): List<DataAuditEntity> = withContext(Dispatchers.IO) {
        AuditFilterUtils.filterUniqueAudits(auditDao.getAllAuditsList())
    }

    suspend fun login(username: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanQuery = username.trim()
        if (cleanQuery.isBlank()) {
            return@withContext Result.failure(Exception("Username atau NIP tidak boleh kosong!"))
        }

        val targetDeletedNips = listOf("1001", "1002", "1003", "1004", "3001", "2001")
        val targetDeletedUsernames = listOf("superadmin", "admin", "kabeng", "masterdata", "skt", "auditor")

        // Jika username/NIP yang diinput adalah salah satu dari user default lokal yang dihapus, tolak login
        val cleanUser = cleanQuery.lowercase()
        if (cleanUser in targetDeletedUsernames || cleanQuery in targetDeletedNips) {
            cleanDefaultLocalUsers()
            return@withContext Result.failure(Exception("Akun '$cleanQuery' telah dihapus dari sistem."))
        }

        // Cari user lokal berdasarkan username, NIP, atau nama
        val user = userDao.findUserForLogin(cleanQuery) ?: userDao.getUserByUsername(cleanQuery)
        if (user != null) {
            if (user.nip in targetDeletedNips || user.username.lowercase() in targetDeletedUsernames) {
                userDao.deleteUser(user)
                return@withContext Result.failure(Exception("Akun '$cleanQuery' telah dihapus dari sistem."))
            }

            val status = user.status.trim().uppercase()
            if (status == "BLOCKED" || status == "BLOKED") {
                return@withContext Result.failure(Exception("Akun Anda telah di-blokir (BLOCKED). Tidak dapat login ke sistem! Silakan hubungi Administrator."))
            }

            // Pengecekan Tanggal Aktif khusus untuk level akses AUDITOR
            if (user.levelAkses.trim().equals("AUDITOR", ignoreCase = true)) {
                val tanggalAktif = user.tanggalAktif.trim()
                val todayFormatted = com.example.util.DateUtils.getCurrentDateFormatted()
                if (tanggalAktif.isBlank()) {
                    return@withContext Result.failure(Exception("Akun Auditor '${user.namaPetugas}' belum memiliki Tanggal Aktif. Silakan hubungi Administrator untuk menentukan jadwal aktif penugasan."))
                }
                if (!isAuditorActiveToday(user)) {
                    val displayDate = com.example.util.DateUtils.formatDisplayDate(tanggalAktif)
                    return@withContext Result.failure(Exception("Akun Auditor '${user.namaPetugas}' hanya aktif pada tanggal $displayDate. Hari ini ($todayFormatted) akun Anda tidak aktif!"))
                }

                // Normalisasi tanggal aktif di database lokal agar tersimpan rapi berformat dd/MM/yyyy
                val cleanNormalized = com.example.util.DateUtils.normalizeDateString(tanggalAktif)
                if (cleanNormalized.isNotBlank() && cleanNormalized != user.tanggalAktif) {
                    try {
                        val updatedUser = user.copy(tanggalAktif = cleanNormalized)
                        userDao.updateUser(updatedUser)
                    } catch (_: Exception) {}
                }
            }

            if (user.password == password || password.isBlank()) {
                sessionManager.saveSession(user)
                return@withContext Result.success(user)
            } else {
                return@withContext Result.failure(Exception("Password salah! Silakan periksa kembali password Anda."))
            }
        } else {
            Result.failure(Exception("Username atau NIP '$cleanQuery' tidak terdaftar di database."))
        }
    }

    suspend fun getKendaraanByNoMobil(noMobil: String): MasterKendaraanEntity? = withContext(Dispatchers.IO) {
        val trimmed = noMobil.trim()
        if (trimmed.isBlank()) return@withContext null
        val direct = kendaraanDao.getKendaraanByNoMobil(trimmed)
        if (direct != null) return@withContext direct
        val withoutHash = trimmed.removePrefix("#").trim()
        val noHashResult = kendaraanDao.getKendaraanByNoMobil(withoutHash)
        if (noHashResult != null) return@withContext noHashResult
        if (withoutHash.all { it.isDigit() }) {
            val unpadded = withoutHash.trimStart('0')
            if (unpadded.isNotBlank() && unpadded != withoutHash) {
                val unpaddedResult = kendaraanDao.getKendaraanByNoMobil(unpadded)
                if (unpaddedResult != null) return@withContext unpaddedResult
            }
        }
        null
    }

    suspend fun getAuditById(id: Long): DataAuditEntity? = withContext(Dispatchers.IO) {
        auditDao.getAuditById(id)
    }

    suspend fun saveAudit(audit: DataAuditEntity): Long = withContext(Dispatchers.IO) {
        val effectiveCode = if (audit.kodeUnik.isNotBlank()) audit.kodeUnik else UniqueCodeGenerator.generateAuditCode()
        val auditToSave = audit.copy(
            kodeUnik = effectiveCode,
            auditUuid = if (audit.auditUuid.isBlank()) effectiveCode else audit.auditUuid
        )
        val id = auditDao.insertAudit(auditToSave)
        if (sessionManager.isAutoSyncEnabled && sessionManager.scriptUrl.isNotBlank()) {
            repositoryScope.launch {
                try {
                    syncAuditToCloud(id)
                } catch (e: Exception) {
                    // Keep local offline record safe
                }
            }
        }
        id
    }

    suspend fun updateAudit(audit: DataAuditEntity) = withContext(Dispatchers.IO) {
        val effectiveCode = if (audit.kodeUnik.isNotBlank()) audit.kodeUnik else UniqueCodeGenerator.generateAuditCode()
        val auditToUpdate = audit.copy(
            kodeUnik = effectiveCode,
            auditUuid = if (audit.auditUuid.isBlank()) effectiveCode else audit.auditUuid
        )
        auditDao.updateAudit(auditToUpdate)
        if (sessionManager.isAutoSyncEnabled && sessionManager.scriptUrl.isNotBlank()) {
            repositoryScope.launch {
                try {
                    syncAuditToCloud(auditToUpdate.id)
                } catch (e: Exception) {
                    // Keep local offline record safe
                }
            }
        }
    }

    suspend fun deleteAudit(id: Long) = withContext(Dispatchers.IO) {
        val audit = auditDao.getAuditById(id)
        auditDao.deleteAuditById(id)
        if (audit != null && sessionManager.scriptUrl.isNotBlank()) {
            repositoryScope.launch {
                try {
                    syncDeleteAuditToCloud(audit.id, audit.noMobil, audit.periode, audit.kodeUnik)
                } catch (e: Exception) {
                    // Safe offline delete
                }
            }
        }
    }

    suspend fun deleteAllAudits() = withContext(Dispatchers.IO) {
        auditDao.deleteAllAudits()
    }

    suspend fun deleteAllKendaraan() = withContext(Dispatchers.IO) {
        kendaraanDao.deleteAllKendaraan()
    }

    suspend fun deleteDemoKendaraan() = withContext(Dispatchers.IO) {
        kendaraanDao.deleteDemoKendaraan()
    }

    suspend fun clearDemoData(clearAllAudits: Boolean = true, clearDemoVehicles: Boolean = true): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var auditsDeleted = 0
        var vehiclesDeleted = 0
        if (clearAllAudits) {
            auditsDeleted = auditDao.deleteAllAudits()
        }
        if (clearDemoVehicles) {
            vehiclesDeleted = kendaraanDao.deleteDemoKendaraan()
        }
        sessionManager.isDemoDataCleared = true
        Pair(auditsDeleted, vehiclesDeleted)
    }

    // CRUD Master Data
    suspend fun insertUser(user: UserEntity) = withContext(Dispatchers.IO) {
        val toInsert = if (user.kodeUnik.isBlank()) {
            user.copy(kodeUnik = UniqueCodeGenerator.generateUserCode())
        } else {
            user
        }
        userDao.insertUser(toInsert)
        if (sessionManager.scriptUrl.isNotBlank()) {
            repositoryScope.launch {
                try {
                    syncMasterUserToCloud(toInsert)
                } catch (e: Exception) {
                    // Safe offline
                }
            }
        }
    }

    suspend fun deleteUser(nip: String) = withContext(Dispatchers.IO) {
        userDao.deleteUserByNip(nip)
        if (sessionManager.scriptUrl.isNotBlank()) {
            repositoryScope.launch {
                try {
                    syncDeleteUserToCloud(nip)
                } catch (e: Exception) {
                    // Safe offline
                }
            }
        }
    }

    suspend fun insertPool(pool: MasterPoolEntity) = withContext(Dispatchers.IO) {
        val toInsert = if (pool.kodeUnik.isBlank()) {
            pool.copy(kodeUnik = UniqueCodeGenerator.generatePoolCode())
        } else {
            pool
        }
        poolDao.insertPool(toInsert)
        if (sessionManager.scriptUrl.isNotBlank()) {
            repositoryScope.launch {
                try {
                    syncMasterPoolToCloud(toInsert)
                } catch (e: Exception) {
                    // Safe offline
                }
            }
        }
    }

    suspend fun deletePool(id: Int) = withContext(Dispatchers.IO) {
        val pool = poolDao.getPoolById(id)
        poolDao.deletePoolById(id)
        if (pool != null && sessionManager.scriptUrl.isNotBlank()) {
            repositoryScope.launch {
                try {
                    syncDeletePoolToCloud(id, pool.kodePool)
                } catch (e: Exception) {
                    // Safe offline
                }
            }
        }
    }

    suspend fun insertKendaraan(kendaraan: MasterKendaraanEntity) = withContext(Dispatchers.IO) {
        val toInsert = if (kendaraan.kodeUnik.isBlank()) {
            kendaraan.copy(kodeUnik = UniqueCodeGenerator.generateKendaraanCode())
        } else {
            kendaraan
        }
        kendaraanDao.insertKendaraan(toInsert)
        if (sessionManager.scriptUrl.isNotBlank()) {
            repositoryScope.launch {
                try {
                    syncMasterKendaraanToCloud(toInsert)
                } catch (e: Exception) {
                    // Safe offline
                }
            }
        }
    }

    suspend fun deleteKendaraan(id: Int) = withContext(Dispatchers.IO) {
        val v = kendaraanDao.getKendaraanById(id)
        kendaraanDao.deleteKendaraanById(id)
        if (v != null && sessionManager.scriptUrl.isNotBlank()) {
            repositoryScope.launch {
                try {
                    syncDeleteKendaraanToCloud(id, v.noMobil)
                } catch (e: Exception) {
                    // Safe offline
                }
            }
        }
    }

    suspend fun updatePhotoSetting(setting: PengaturanNamaPhotoEntity) = withContext(Dispatchers.IO) {
        photoSettingDao.updateNamaPhoto(setting)
    }

    suspend fun insertPeriode(periode: MasterPeriodeEntity) = withContext(Dispatchers.IO) {
        val toInsert = if (periode.kodeUnik.isBlank()) {
            periode.copy(kodeUnik = UniqueCodeGenerator.generatePeriodeCode())
        } else {
            periode
        }
        val id = if (toInsert.id == 0) {
            periodeDao.insertPeriode(toInsert)
        } else {
            periodeDao.updatePeriode(toInsert)
            toInsert.id.toLong()
        }
        if (sessionManager.scriptUrl.isNotBlank()) {
            val updated = if (toInsert.id == 0) toInsert.copy(id = id.toInt()) else toInsert
            repositoryScope.launch {
                try {
                    syncMasterPeriodeToCloud(updated)
                } catch (e: Exception) {
                    // Safe offline
                }
            }
        }
    }

    suspend fun deletePeriode(id: Int) = withContext(Dispatchers.IO) {
        val entity = periodeDao.getAllPeriodeList().find { it.id == id }
        if (entity != null) {
            periodeDao.deletePeriode(entity)
            if (sessionManager.scriptUrl.isNotBlank()) {
                repositoryScope.launch {
                    try {
                        syncDeletePeriodeToCloud(entity.id, entity.periode)
                    } catch (e: Exception) {
                        // Safe offline
                    }
                }
            }
        }
    }

    suspend fun insertOrUpdateSample(sample: MasterSampleEntity) = withContext(Dispatchers.IO) {
        val toInsert = if (sample.kodeUnik.isBlank()) {
            sample.copy(kodeUnik = UniqueCodeGenerator.generateSampleCode())
        } else {
            sample
        }
        sampleDao.insert(toInsert)
        if (sessionManager.scriptUrl.isNotBlank()) {
            repositoryScope.launch {
                try {
                    syncMasterSampleToCloud(toInsert)
                } catch (e: Exception) {
                    // Safe offline
                }
            }
        }
    }

    suspend fun updateJumlahSample(statusArea: String, jumlah: Int) = withContext(Dispatchers.IO) {
        sampleDao.updateJumlahSample(statusArea, jumlah)
        val sample = sampleDao.getSampleByStatusArea(statusArea)
        if (sample != null && sessionManager.scriptUrl.isNotBlank()) {
            repositoryScope.launch {
                try {
                    syncMasterSampleToCloud(sample)
                } catch (e: Exception) {
                    // Safe offline
                }
            }
        }
    }

    suspend fun deleteSample(statusArea: String) = withContext(Dispatchers.IO) {
        sampleDao.delete(statusArea)
        if (sessionManager.scriptUrl.isNotBlank()) {
            repositoryScope.launch {
                try {
                    syncDeleteSampleToCloud(statusArea)
                } catch (e: Exception) {
                    // Safe offline
                }
            }
        }
    }

    suspend fun ensureSampleSeeded() = withContext(Dispatchers.IO) {
        if (sampleDao.getCount() == 0) {
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
            sampleDao.insertAll(defaultSamples)
        }
    }

    // ==========================================
    // GRADE KPI OPERATIONS (SUPER ADMIN)
    // ==========================================

    suspend fun ensureGradeKpiSeeded() = withContext(Dispatchers.IO) {
        val count = gradeKpiDao.getCount()
        if (count == 0) {
            val defaultGrades = listOf(
                MasterGradeKpiEntity("A", 70.00, 80.00, "Sangat Baik (Grade A)", kodeUnik = UniqueCodeGenerator.generateGradeCode()),
                MasterGradeKpiEntity("B", 60.00, 69.99, "Baik (Grade B)", kodeUnik = UniqueCodeGenerator.generateGradeCode()),
                MasterGradeKpiEntity("C", 50.00, 59.99, "Cukup (Grade C)", kodeUnik = UniqueCodeGenerator.generateGradeCode()),
                MasterGradeKpiEntity("D", 30.00, 49.99, "Kurang (Grade D)", kodeUnik = UniqueCodeGenerator.generateGradeCode()),
                MasterGradeKpiEntity("E", 0.00, 29.99, "Kritis / Perlu Perbaikan (Grade E)", kodeUnik = UniqueCodeGenerator.generateGradeCode())
            )
            gradeKpiDao.insertAll(defaultGrades)
        } else {
            // Otomatis migrasi jika database lokal masih memakai konfigurasi default lama (di mana Grade A minScore = 0.0 dan maxScore = 10.0)
            val currentA = gradeKpiDao.getByKategori("A")
            if (currentA != null && currentA.minScore == 0.00 && currentA.maxScore <= 15.00) {
                val newDefaults = listOf(
                    MasterGradeKpiEntity("A", 70.00, 80.00, "Sangat Baik (Grade A)", kodeUnik = currentA.kodeUnik.ifBlank { UniqueCodeGenerator.generateGradeCode() }),
                    MasterGradeKpiEntity("B", 60.00, 69.99, "Baik (Grade B)", kodeUnik = gradeKpiDao.getByKategori("B")?.kodeUnik ?: UniqueCodeGenerator.generateGradeCode()),
                    MasterGradeKpiEntity("C", 50.00, 59.99, "Cukup (Grade C)", kodeUnik = gradeKpiDao.getByKategori("C")?.kodeUnik ?: UniqueCodeGenerator.generateGradeCode()),
                    MasterGradeKpiEntity("D", 30.00, 49.99, "Kurang (Grade D)", kodeUnik = gradeKpiDao.getByKategori("D")?.kodeUnik ?: UniqueCodeGenerator.generateGradeCode()),
                    MasterGradeKpiEntity("E", 0.00, 29.99, "Kritis / Perlu Perbaikan (Grade E)", kodeUnik = gradeKpiDao.getByKategori("E")?.kodeUnik ?: UniqueCodeGenerator.generateGradeCode())
                )
                gradeKpiDao.insertAll(newDefaults)
            }
        }
    }

    suspend fun getAllGradeKpiSync(): List<MasterGradeKpiEntity> = withContext(Dispatchers.IO) {
        ensureGradeKpiSeeded()
        gradeKpiDao.getAllGradeKpi()
    }

    suspend fun saveGradeKpi(entity: MasterGradeKpiEntity) = withContext(Dispatchers.IO) {
        val toInsert = if (entity.kodeUnik.isBlank()) {
            entity.copy(kodeUnik = UniqueCodeGenerator.generateGradeCode(), updatedAt = System.currentTimeMillis())
        } else {
            entity.copy(updatedAt = System.currentTimeMillis())
        }
        gradeKpiDao.insert(toInsert)
        if (sessionManager.scriptUrl.isNotBlank()) {
            syncAllMasterGradeKpiToCloud()
        }
    }

    suspend fun updateGradeKpi(kategori: String, minScore: Double, maxScore: Double, keterangan: String) = withContext(Dispatchers.IO) {
        gradeKpiDao.updateGradeKpi(kategori, minScore, maxScore, keterangan, System.currentTimeMillis())
        if (sessionManager.scriptUrl.isNotBlank()) {
            syncAllMasterGradeKpiToCloud()
        }
    }

    suspend fun resetDefaultGradeKpi() = withContext(Dispatchers.IO) {
        gradeKpiDao.deleteAll()
        val defaultGrades = listOf(
            MasterGradeKpiEntity("A", 70.00, 80.00, "Sangat Baik (Grade A)", kodeUnik = UniqueCodeGenerator.generateGradeCode()),
            MasterGradeKpiEntity("B", 60.00, 69.99, "Baik (Grade B)", kodeUnik = UniqueCodeGenerator.generateGradeCode()),
            MasterGradeKpiEntity("C", 50.00, 59.99, "Cukup (Grade C)", kodeUnik = UniqueCodeGenerator.generateGradeCode()),
            MasterGradeKpiEntity("D", 30.00, 49.99, "Kurang (Grade D)", kodeUnik = UniqueCodeGenerator.generateGradeCode()),
            MasterGradeKpiEntity("E", 0.00, 29.99, "Kritis / Perlu Perbaikan (Grade E)", kodeUnik = UniqueCodeGenerator.generateGradeCode())
        )
        gradeKpiDao.insertAll(defaultGrades)
        if (sessionManager.scriptUrl.isNotBlank()) {
            syncAllMasterGradeKpiToCloud()
        }
    }

    fun calculateGradeForScore(score: Double, gradeList: List<MasterGradeKpiEntity>): String {
        if (gradeList.isEmpty()) return "-"
        val rounded = kotlin.math.round(score * 100.0) / 100.0
        // Cocokkan rentang dengan toleransi pembulatan 0.005 agar tidak ada celah di antara 69.99 dan 70.00
        val found = gradeList.firstOrNull { 
            rounded >= it.minScore - 0.005 && rounded <= it.maxScore + 0.005 
        } ?: gradeList.firstOrNull { score in it.minScore..it.maxScore }
        
        return found?.kategori ?: when {
            rounded >= 70.00 -> gradeList.find { it.kategori.equals("A", ignoreCase = true) }?.kategori ?: "A"
            rounded <= 29.99 -> gradeList.find { it.kategori.equals("E", ignoreCase = true) }?.kategori ?: "E"
            else -> "-"
        }
    }

    // Sync Engine
    suspend fun syncAuditToCloud(auditId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) {
            return@withContext Result.failure(Exception("URL Google Script belum diatur di menu Pengaturan."))
        }

        val audit = auditDao.getAuditById(auditId)
            ?: return@withContext Result.failure(Exception("Data audit tidak ditemukan."))

        try {
            val jsonPayload = JSONObject().apply {
                put("action", "submitAudit")
                val auditObj = JSONObject().apply {
                    val formattedTime = DateUtils.formatTimestampWithSeconds(audit.timestamp)
                    val serviceDate = if (audit.sampleTanggal.isNotBlank()) audit.sampleTanggal else audit.tanggalService
                    val paketVal = audit.paketService.ifBlank { audit.service }
                    val isExplicitEdit = audit.cloudId > 0L
                    val effectiveKode = if (audit.kodeUnik.isNotBlank()) audit.kodeUnik else UniqueCodeGenerator.generateAuditCode()
                    if (audit.kodeUnik.isBlank()) {
                        auditDao.updateAudit(audit.copy(kodeUnik = effectiveKode))
                    }
                    put("id", effectiveKode)
                    put("kodeUnik", effectiveKode)
                    put("cloudId", audit.cloudId)
                    put("localId", audit.id)
                    val effectiveUuid = if (audit.auditUuid.isNotBlank()) audit.auditUuid else effectiveKode
                    put("auditUuid", effectiveUuid)
                    put("uuid", effectiveUuid)
                    put("isEdit", isExplicitEdit)
                    val cleanTanggal = if (audit.tanggal.isNotBlank()) {
                        val d = DateUtils.formatDisplayDate(audit.tanggal)
                        if (d == "-") DateUtils.formatDate(audit.timestamp) else d
                    } else {
                        DateUtils.formatDate(audit.timestamp)
                    }
                    put("tanggal", cleanTanggal)
                    put("timestamp", formattedTime)
                    put("waktu", formattedTime)
                    put("rawTimestamp", audit.timestamp)
                    put("nip", audit.nip)
                    put("namaPetugas", audit.namaPetugas)
                    put("periode", audit.periode)
                    put("noMobil", audit.noMobil)
                    put("noKendaraan", audit.noMobil)
                    put("platNomer", audit.platNomer)
                    put("tipe", audit.tipe)
                    put("warna", audit.warna)
                    put("tahun", audit.tahun)
                    put("tanggalService", serviceDate)
                    put("sampleTanggal", serviceDate)
                    put("service", paketVal)
                    put("paketService", paketVal)
                    put("pool", audit.pool)
                    put("jenisSample", audit.jenisSample)
                    // Rincian poin temuan yang mudah dibaca di Google Sheets
                    val rincian = try {
                        val scoresObj = JSONObject(audit.kpiScoresJson)
                        val allItems = com.example.data.model.KpiAuditSpec.getAllItems()
                        val list = mutableListOf<String>()
                        val keys = scoresObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            val s = scoresObj.optInt(k, 0)
                            if (s > 0) {
                                val itemObj = allItems.find { it.id == k }
                                val name = itemObj?.name ?: k
                                list.add("$name ($s Poin)")
                            }
                        }
                        if (list.isNotEmpty()) list.joinToString(", ") else "Normal (0 Poin)"
                    } catch (e: Exception) {
                        "Total ${audit.totalPoinTemuan} Poin"
                    }

                    put("totalPoinTemuan", audit.totalPoinTemuan)
                    put("point", audit.totalPoinTemuan)
                    put("poin", audit.totalPoinTemuan)
                    put("totalPoin", audit.totalPoinTemuan)
                    put("totalPoint", audit.totalPoinTemuan)
                    put("kpiScoresJson", audit.kpiScoresJson)
                    put("kpiNotesJson", audit.kpiNotesJson)
                    put("kpiPhotosJson", audit.kpiPhotosJson)
                    put("rincianPoinTemuan", rincian)

                    try {
                        put("kpiScores", JSONObject(audit.kpiScoresJson))
                    } catch (e: Exception) {}
                    try {
                        put("kpiNotes", JSONObject(audit.kpiNotesJson))
                    } catch (e: Exception) {}
                    try {
                        put("kpiPhotos", JSONObject(audit.kpiPhotosJson))
                    } catch (e: Exception) {}

                    // Base64 photos
                    val photoPaths = listOf(
                        audit.photo1, audit.photo2, audit.photo3, audit.photo4, audit.photo5,
                        audit.photo6, audit.photo7, audit.photo8, audit.photo9, audit.photo10,
                        audit.photo11, audit.photo12, audit.photo13, audit.photo14, audit.photo15,
                        audit.photo16, audit.photo17, audit.photo18, audit.photo19, audit.photo20
                    )
                    val notes = listOf(
                        audit.catatanPhoto1, audit.catatanPhoto2, audit.catatanPhoto3, audit.catatanPhoto4, audit.catatanPhoto5,
                        audit.catatanPhoto6, audit.catatanPhoto7, audit.catatanPhoto8, audit.catatanPhoto9, audit.catatanPhoto10,
                        audit.catatanPhoto11, audit.catatanPhoto12, audit.catatanPhoto13, audit.catatanPhoto14, audit.catatanPhoto15,
                        audit.catatanPhoto16, audit.catatanPhoto17, audit.catatanPhoto18, audit.catatanPhoto19, audit.catatanPhoto20
                    )

                    // Fallback: Jika slot 1-7 di photoPaths kosong, ekstrak dari kpiPhotosJson
                    val resolvedPhotoPaths = photoPaths.toMutableList()
                    val resolvedNotes = notes.toMutableList()
                    try {
                        val pObj = JSONObject(audit.kpiPhotosJson)
                        val vehicleKeys = listOf(
                            "kendaraan_depan" to "Foto Kendaraan - Depan",
                            "kendaraan_samping_kiri" to "Foto Kendaraan - Samping Kiri",
                            "kendaraan_interior_depan_kiri" to "Foto Kendaraan - Interior Depan Kiri",
                            "kendaraan_interior_belakang_kiri" to "Foto Kendaraan - Interior Belakang Kiri",
                            "kendaraan_belakang" to "Foto Kendaraan - Belakang",
                            "kendaraan_interior_belakang" to "Foto Kendaraan - Interior Belakang",
                            "kendaraan_samping_kanan" to "Foto Kendaraan - Samping Kanan"
                        )
                        vehicleKeys.forEachIndexed { vIdx, (vKey, vDefaultTitle) ->
                            if (resolvedPhotoPaths[vIdx].isBlank()) {
                                val pathFromMap = pObj.optString(vKey, "")
                                if (pathFromMap.isNotBlank()) {
                                    resolvedPhotoPaths[vIdx] = pathFromMap
                                    if (resolvedNotes[vIdx].isBlank()) {
                                        resolvedNotes[vIdx] = vDefaultTitle
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // ignore json parse error
                    }

                    // Base64 photos
                    val base64Cache = mutableMapOf<String, String>()
                    for (i in 1..20) {
                        val path = resolvedPhotoPaths[i - 1]
                        val note = resolvedNotes[i - 1]
                        val base64 = if (path.isNotBlank()) {
                            if (path.startsWith("http://") || path.startsWith("https://")) {
                                path // Pertahankan URL Google Drive yang sudah ada
                            } else {
                                base64Cache.getOrPut(path) {
                                    ImageCompressor.fileToBase64(path)
                                }
                            }
                        } else {
                            ""
                        }
                        put("photo$i", base64)
                        put("catatanPhoto$i", note)
                    }

                    // Map lengkap seluruh foto (foto kendaraan 1-7 + foto temuan KPI)
                    val photosMapObj = JSONObject()
                    try {
                        val pObj = JSONObject(audit.kpiPhotosJson)
                        val iter = pObj.keys()
                        while (iter.hasNext()) {
                            val k = iter.next()
                            val path = pObj.optString(k, "")
                            if (path.isNotBlank()) {
                                val b64 = if (path.startsWith("http://") || path.startsWith("https://")) {
                                    path
                                } else {
                                    base64Cache.getOrPut(path) {
                                        ImageCompressor.fileToBase64(path)
                                    }
                                }
                                if (b64.isNotBlank()) {
                                    photosMapObj.put(k, b64)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                    put("photosMap", photosMapObj)
                    base64Cache.clear() // Segera kosongkan cache base64 dari memori RAM
                }
                put("audit", auditObj)
            }

            val body = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(scriptUrl)
                .post(body)
                .build()

            var lastException: Exception? = null
            val maxRetries = 3
            for (attempt in 1..maxRetries) {
                try {
                    val response = httpClient.newCall(request).execute()
                    val responseBodyStr = response.body?.string().orEmpty()
                    if (response.isSuccessful && responseBodyStr.isNotBlank()) {
                        val resJson = if (responseBodyStr.trim().startsWith("{")) JSONObject(responseBodyStr) else null
                        val status = resJson?.optString("status")
                        if (status == "success") {
                            val dataObj = resJson.optJSONObject("data")
                            val assignedCloudId = dataObj?.optLong("id", 0L) ?: dataObj?.optLong("cloudId", 0L) ?: 0L
                            if (assignedCloudId > 0L) {
                                auditDao.markAsSyncedWithCloudId(auditId, assignedCloudId)
                            } else {
                                auditDao.markAsSynced(auditId)
                            }
                            return@withContext Result.success(true)
                        } else {
                            val isBusy = resJson?.optBoolean("busy", false) ?: false
                            val msg = resJson?.optString("message", "Server Google Sheets sedang sibuk melayani user lain")
                                ?: "Gagal memproses data di Google Sheets"
                            if (isBusy && attempt < maxRetries) {
                                kotlinx.coroutines.delay(attempt * 1500L)
                                continue
                            } else {
                                lastException = Exception(msg)
                                break
                            }
                        }
                    } else {
                        if (attempt < maxRetries) {
                            kotlinx.coroutines.delay(attempt * 1000L)
                            continue
                        }
                        lastException = Exception("Gagal upload: HTTP ${response.code}")
                    }
                } catch (e: Exception) {
                    lastException = e
                    if (attempt < maxRetries) {
                        kotlinx.coroutines.delay(attempt * 1000L)
                    }
                }
            }
            Result.failure(lastException ?: Exception("Gagal menyinkronkan data audit ke Google Sheets"))
        } catch (t: Throwable) {
            Log.e(TAG, "Error syncing audit $auditId to cloud", t)
            if (t is OutOfMemoryError) {
                System.gc()
            }
            Result.failure(Exception(t.message ?: "Memori tidak mencukupi untuk memproses sinkronisasi"))
        }
    }

    suspend fun syncDeleteAuditToCloud(id: Long, noMobil: String, periode: String, kodeUnik: String = "") = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) return@withContext
        try {
            val json = JSONObject().apply {
                put("action", "deleteAudit")
                put("id", if (kodeUnik.isNotBlank()) kodeUnik else id.toString())
                put("kodeUnik", kodeUnik)
                put("localId", id)
                put("noMobil", noMobil)
                put("periode", periode)
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            httpClient.newCall(Request.Builder().url(scriptUrl).post(body).build()).execute()
        } catch (e: Exception) {
            // Local deletion prioritized
        }
    }

    suspend fun syncMasterKendaraanToCloud(kendaraan: MasterKendaraanEntity) = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) return@withContext
        try {
            val json = JSONObject().apply {
                put("action", "saveKendaraan")
                val kObj = JSONObject().apply {
                    val effectiveKode = if (kendaraan.kodeUnik.isNotBlank()) kendaraan.kodeUnik else UniqueCodeGenerator.generateKendaraanCode()
                    put("id", effectiveKode)
                    put("kodeUnik", effectiveKode)
                    put("localId", kendaraan.id)
                    put("noMobil", kendaraan.noMobil)
                    put("platNomer", kendaraan.platNomer)
                    put("tipe", kendaraan.tipe)
                    put("warna", kendaraan.warna)
                    put("pool", kendaraan.pool)
                    put("tahun", kendaraan.tahun)
                    put("tanggalService", kendaraan.tanggalService)
                    put("service", kendaraan.service)
                }
                put("kendaraan", kObj)
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            httpClient.newCall(Request.Builder().url(scriptUrl).post(body).build()).execute()
        } catch (e: Exception) {
            // Ignore background sync error
        }
    }

    suspend fun syncDeleteKendaraanToCloud(id: Int, noMobil: String) = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) return@withContext
        try {
            val json = JSONObject().apply {
                put("action", "deleteKendaraan")
                put("id", id)
                put("noMobil", noMobil)
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            httpClient.newCall(Request.Builder().url(scriptUrl).post(body).build()).execute()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun syncMasterPoolToCloud(pool: MasterPoolEntity) = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) return@withContext
        try {
            val json = JSONObject().apply {
                put("action", "savePool")
                val pObj = JSONObject().apply {
                    val effectiveKode = if (pool.kodeUnik.isNotBlank()) pool.kodeUnik else UniqueCodeGenerator.generatePoolCode()
                    put("id", effectiveKode)
                    put("kodeUnik", effectiveKode)
                    put("localId", pool.id)
                    put("kodePool", pool.kodePool)
                    put("namaPool", pool.namaPool)
                    put("alamatPool", pool.alamatPool)
                    put("statusArea", pool.statusArea)
                }
                put("pool", pObj)
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            httpClient.newCall(Request.Builder().url(scriptUrl).post(body).build()).execute()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun syncDeletePoolToCloud(id: Int, kodePool: String) = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) return@withContext
        try {
            val json = JSONObject().apply {
                put("action", "deletePool")
                put("id", id)
                put("kodePool", kodePool)
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            httpClient.newCall(Request.Builder().url(scriptUrl).post(body).build()).execute()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun syncMasterUserToCloud(user: UserEntity) = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) return@withContext
        try {
            val json = JSONObject().apply {
                put("action", "saveUser")
                val uObj = JSONObject().apply {
                    val effectiveKode = if (user.kodeUnik.isNotBlank()) user.kodeUnik else UniqueCodeGenerator.generateUserCode()
                    put("id", effectiveKode)
                    put("kodeUnik", effectiveKode)
                    put("nip", user.nip)
                    put("namaPetugas", user.namaPetugas)
                    put("username", user.username)
                    put("password", user.password)
                    put("levelAkses", user.levelAkses)
                    put("kodePool", user.kodePool)
                    put("status", user.status)
                    put("tanggalAktif", user.tanggalAktif)
                }
                put("user", uObj)
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            httpClient.newCall(Request.Builder().url(scriptUrl).post(body).build()).execute()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun syncDeleteUserToCloud(nip: String) = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) return@withContext
        try {
            val json = JSONObject().apply {
                put("action", "deleteUser")
                put("nip", nip)
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            httpClient.newCall(Request.Builder().url(scriptUrl).post(body).build()).execute()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun syncMasterPeriodeToCloud(periode: MasterPeriodeEntity) = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) return@withContext
        try {
            val json = JSONObject().apply {
                put("action", "savePeriode")
                val pObj = JSONObject().apply {
                    val effectiveKode = if (periode.kodeUnik.isNotBlank()) periode.kodeUnik else UniqueCodeGenerator.generatePeriodeCode()
                    put("id", effectiveKode)
                    put("kodeUnik", effectiveKode)
                    put("localId", periode.id)
                    put("periode", periode.periode)
                    put("keterangan", periode.keterangan)
                }
                put("periode", pObj)
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            httpClient.newCall(Request.Builder().url(scriptUrl).post(body).build()).execute()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun syncDeletePeriodeToCloud(id: Int, periode: String) = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) return@withContext
        try {
            val json = JSONObject().apply {
                put("action", "deletePeriode")
                put("id", id)
                put("periode", periode)
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            httpClient.newCall(Request.Builder().url(scriptUrl).post(body).build()).execute()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun syncMasterSampleToCloud(sample: MasterSampleEntity) = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) return@withContext
        try {
            val json = JSONObject().apply {
                put("action", "saveSample")
                val sObj = JSONObject().apply {
                    val effectiveKode = if (sample.kodeUnik.isNotBlank()) sample.kodeUnik else UniqueCodeGenerator.generateSampleCode()
                    put("id", effectiveKode)
                    put("kodeUnik", effectiveKode)
                    put("statusArea", sample.statusArea)
                    put("jumlahSample", sample.jumlahSample)
                    put("keterangan", sample.keterangan)
                }
                put("sample", sObj)
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            httpClient.newCall(Request.Builder().url(scriptUrl).post(body).build()).execute()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun syncDeleteSampleToCloud(statusArea: String) = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) return@withContext
        try {
            val json = JSONObject().apply {
                put("action", "deleteSample")
                put("statusArea", statusArea)
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            httpClient.newCall(Request.Builder().url(scriptUrl).post(body).build()).execute()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun syncMasterBobotToCloud(bobot: MasterBobotEntity) = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) return@withContext
        try {
            val json = JSONObject().apply {
                put("action", "saveBobot")
                val bObj = JSONObject().apply {
                    val effectiveKode = if (bobot.kodeUnik.isNotBlank()) bobot.kodeUnik else UniqueCodeGenerator.generateBobotCode()
                    put("id", effectiveKode)
                    put("kodeUnik", effectiveKode)
                    put("itemId", bobot.itemId)
                    put("itemName", bobot.itemName)
                    put("sectionTitle", bobot.sectionTitle)
                    put("groupTitle", bobot.groupTitle)
                    put("pointMax", bobot.pointMax)
                    put("bobot", bobot.bobot)
                }
                put("bobot", bObj)
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            httpClient.newCall(Request.Builder().url(scriptUrl).post(body).build()).execute()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun syncMasterGradeKpiToCloud(grade: MasterGradeKpiEntity) = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) return@withContext
        try {
            val json = JSONObject().apply {
                put("action", "saveGradeKpi")
                val gObj = JSONObject().apply {
                    val effectiveKode = if (grade.kodeUnik.isNotBlank()) grade.kodeUnik else UniqueCodeGenerator.generateGradeCode()
                    put("id", effectiveKode)
                    put("kodeUnik", effectiveKode)
                    put("kategori", grade.kategori)
                    put("minScore", grade.minScore)
                    put("maxScore", grade.maxScore)
                    put("keterangan", grade.keterangan)
                }
                put("grade", gObj)
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            httpClient.newCall(Request.Builder().url(scriptUrl).post(body).build()).execute()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun syncAllMasterGradeKpiToCloud(): Result<Int> = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) return@withContext Result.failure(Exception("URL Google Script belum dikonfigurasi"))
        try {
            val grades = gradeKpiDao.getAllGradeKpi()
            val arr = JSONArray()
            grades.forEach { g ->
                val obj = JSONObject().apply {
                    put("kategori", g.kategori)
                    put("minScore", g.minScore)
                    put("maxScore", g.maxScore)
                    put("keterangan", g.keterangan)
                }
                arr.put(obj)
            }
            val json = JSONObject().apply {
                put("action", "saveAllGradeKpi")
                put("gradeList", arr)
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(scriptUrl).post(body).build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(grades.size)
            } else {
                Result.failure(Exception("HTTP Error ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncAllPendingAudits(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val unsynced = auditDao.getUnsyncedAudits()
        var successCount = 0
        var failCount = 0
        for (audit in unsynced) {
            val res = syncAuditToCloud(audit.id)
            if (res.isSuccess) successCount++ else failCount++
        }
        Pair(successCount, failCount)
    }

    private fun JSONObject.optStringFlexible(vararg candidateKeys: String, default: String = ""): String {
        for (k in candidateKeys) {
            if (this.has(k) && !this.isNull(k)) {
                val v = this.optString(k, "").trim()
                if (v.isNotEmpty()) return v
            }
        }
        val targets = candidateKeys.map { it.lowercase().replace("[^a-z0-9]".toRegex(), "") }
        val iter = this.keys()
        while (iter.hasNext()) {
            val key = iter.next()
            val normKey = key.lowercase().replace("[^a-z0-9]".toRegex(), "")
            if (targets.contains(normKey) && !this.isNull(key)) {
                val v = this.optString(key, "").trim()
                if (v.isNotEmpty()) return v
            }
        }
        return default
    }

    private fun JSONObject.optLongFlexible(vararg candidateKeys: String, default: Long = 0L): Long {
        val str = optStringFlexible(*candidateKeys)
        if (str.isEmpty()) return default
        return str.toLongOrNull() ?: default
    }

    private fun JSONObject.optIntFlexible(vararg candidateKeys: String, default: Int = 0): Int {
        val str = optStringFlexible(*candidateKeys)
        if (str.isEmpty()) return default
        val clean = str.replace(" ", "").replace(",", ".")
        return clean.toDoubleOrNull()?.toInt() ?: clean.toIntOrNull() ?: default
    }

    private fun JSONObject.optDoubleFlexible(vararg candidateKeys: String, default: Double = 0.0): Double {
        val str = optStringFlexible(*candidateKeys)
        if (str.isEmpty()) return default
        val clean = str.replace(" ", "").replace(",", ".")
        return clean.toDoubleOrNull() ?: default
    }

    private fun fetchJsonFromCloud(
        action: String,
        params: Map<String, String> = emptyMap(),
        timeoutSeconds: Long = 30L
    ): String {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) throw Exception("URL Google Script belum diatur.")

        val client = if (timeoutSeconds != 30L) {
            httpClient.newBuilder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build()
        } else {
            httpClient
        }

        val queryParamStr = if (params.isNotEmpty()) {
            "&" + params.entries.joinToString("&") { "${it.key}=${java.net.URLEncoder.encode(it.value, "UTF-8")}" }
        } else {
            ""
        }

        // 1. Coba request GET
        try {
            val url = if (scriptUrl.contains("?")) "$scriptUrl&action=$action$queryParamStr" else "$scriptUrl?action=$action$queryParamStr"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()?.trim() ?: ""
                if (body.startsWith("[") || (body.startsWith("{") && !body.startsWith("<!DOCTYPE", ignoreCase = true))) {
                    return body
                }
            }
        } catch (e: Exception) {
            // Lanjut ke fallback POST jika GET gagal
        }

        // 2. Fallback ke request POST jika GET mengembalikan redirect / format HTML
        val postJson = JSONObject().apply {
            put("action", action)
            params.forEach { (k, v) -> put(k, v) }
        }
        val body = postJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val postRequest = Request.Builder().url(scriptUrl).post(body).build()
        val postResponse = client.newCall(postRequest).execute()
        if (!postResponse.isSuccessful) {
            throw Exception("Gagal menghubungi Google Sheets (HTTP ${postResponse.code})")
        }
        val postBody = postResponse.body?.string()?.trim() ?: ""
        if (!postBody.startsWith("[") && !postBody.startsWith("{")) {
            throw Exception("Respon server bukan format JSON (Google Script mengembalikan HTML/Web)")
        }
        return postBody
    }

    /**
     * Mengunduh data username dan password SEMUA user (Auditor, Admin, Super Admin, Kepala Bengkel, Master Data, SKT, dll)
     * di belakang layar dari Google Sheets ke database lokal (Room) agar kredensial semua user selalu up-to-date
     * dan siap digunakan login secara offline maupun instan.
     */
    suspend fun downloadAllUsersCredentialsFromCloud(timeoutSeconds: Long = 15L): Result<Int> = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) {
            return@withContext Result.failure(Exception("URL Google Script belum diatur."))
        }

        try {
            var bodyStr = ""
            var fetchSuccess = false

            // 1. Coba endpoint getUsers (cepat & payload khusus master user)
            try {
                bodyStr = fetchJsonFromCloud("getUsers", timeoutSeconds = timeoutSeconds)
                val testRoot = if (bodyStr.startsWith("{")) JSONObject(bodyStr) else JSONObject()
                if (testRoot.has("users")) {
                    fetchSuccess = true
                }
            } catch (_: Exception) {
                // Fallback
            }

            // 2. Fallback jika Google Script belum ada action getUsers, gunakan getMasterData
            if (!fetchSuccess) {
                try {
                    bodyStr = fetchJsonFromCloud("getMasterData", timeoutSeconds = timeoutSeconds)
                } catch (e: Exception) {
                    return@withContext Result.failure(Exception("Koneksi gagal saat mengunduh kredensial user di latar belakang: ${e.message}"))
                }
            }

            val root = if (bodyStr.startsWith("{")) JSONObject(bodyStr) else JSONObject()
            if (root.optString("status") == "error") {
                return@withContext Result.failure(Exception(root.optString("message", "Error dari Google Script")))
            }

            if (root.has("users")) {
                val arr = root.getJSONArray("users")
                val existingUsers = userDao.getAllUsersList()
                val userList = mutableListOf<UserEntity>()

                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val nip = obj.optStringFlexible("NIP", "nip", "id_petugas", "id").trim()
                    val username = obj.optStringFlexible("USERNAMA", "username", "user").trim().lowercase()
                    val password = obj.optStringFlexible("PASSWORD", "password", default = "").trim()

                    if (nip.isEmpty() && username.isEmpty()) continue

                    val existing = existingUsers.find {
                        (nip.isNotEmpty() && it.nip.equals(nip, ignoreCase = true)) ||
                        (username.isNotEmpty() && it.username.equals(username, ignoreCase = true))
                    }

                    val rawKode = obj.optStringFlexible("KODE_UNIK", "kodeUnik", "kode_unik").trim()
                    val kodeUnik = if (rawKode.isNotBlank()) rawKode else (existing?.kodeUnik?.ifBlank { null } ?: UniqueCodeGenerator.generateUserCode())

                    val rawRole = obj.optStringFlexible("LEVELAKSES", "levelAkses", "role", "level", default = "").trim().uppercase()
                    val levelAkses = if (rawRole.isNotBlank()) rawRole else (existing?.levelAkses ?: "AUDITOR")

                    val rawNama = obj.optStringFlexible("NAMA_PETUGAS", "namaPetugas", "nama", "petugas", default = "").trim()
                    val namaPetugas = if (rawNama.isNotBlank()) rawNama else (existing?.namaPetugas ?: username.ifEmpty { nip })

                    val rawPool = obj.optStringFlexible("KODEPOOL", "kodePool", "pool", default = "").trim().uppercase()
                    val kodePool = if (rawPool.isNotBlank()) rawPool else (existing?.kodePool ?: "SKT")

                    val rawStatus = obj.optStringFlexible("STATUS", "status", default = "").trim().uppercase()
                    val status = if (rawStatus.isNotBlank()) rawStatus else (existing?.status ?: "AKTIF")

                    val rawTgl = obj.optStringFlexible("TANGGAL_AKTIF", "tanggalAktif", "tgl_aktif").trim()
                    val normTgl = com.example.util.DateUtils.normalizeDateString(rawTgl)
                    val tanggalAktif = if (normTgl.isNotBlank()) normTgl else (existing?.tanggalAktif ?: rawTgl)

                    val finalPassword = if (password.isNotBlank()) password else (existing?.password ?: "123")

                    val userEntity = UserEntity(
                        nip = nip.ifEmpty { existing?.nip?.ifEmpty { "U${i + 1}" } ?: "U${i + 1}" },
                        kodeUnik = kodeUnik,
                        namaPetugas = namaPetugas,
                        username = username.ifEmpty { existing?.username?.ifEmpty { "user${i + 1}" } ?: "user${i + 1}" },
                        password = finalPassword,
                        levelAkses = levelAkses,
                        kodePool = kodePool,
                        status = status,
                        tanggalAktif = tanggalAktif
                    )
                    userList.add(userEntity)
                }

                if (userList.isNotEmpty()) {
                    userDao.insertUsers(userList)
                    Log.d(TAG, "Berhasil mengunduh & memperbarui ${userList.size} akun username/password SEMUA USER di belakang layar")
                }
                return@withContext Result.success(userList.size)
            } else {
                return@withContext Result.failure(Exception("Data user tidak ditemukan di database cloud."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengunduh user di belakang layar: ${e.message}")
            return@withContext Result.failure(Exception("Gagal mengunduh data SEMUA user di belakang layar: ${e.message}"))
        }
    }

    /**
     * Mengunduh data username dan password khusus user AUDITOR di belakang layar.
     * Mengambil data dari cloud dengan payload ringan, memfilter khusus level akses AUDITOR,
     * dan menyimpannya ke database lokal (Room) agar kredensial auditor selalu mutakhir dan siap digunakan login secara instan & offline.
     */
    suspend fun downloadAuditorCredentialsFromCloud(timeoutSeconds: Long = 15L): Result<Int> = withContext(Dispatchers.IO) {
        // Panggil pengunduhan semua user agar mencakup auditor sekaligus memperbarui seluruh kredensial
        downloadAllUsersCredentialsFromCloud(timeoutSeconds)
    }

    /**
     * Sinkronisasi data Master User saja dari cloud dengan timeout cepat dan payload sangat ringan.
     * Mencegah proses cek database lama dan kegagalan pada koneksi lambat saat proses login.
     */
    suspend fun downloadUsersFromCloud(timeoutSeconds: Long = 12L): Result<Boolean> = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) {
            return@withContext Result.failure(Exception("URL Google Script belum diatur."))
        }

        try {
            var bodyStr = ""
            var fetchSuccess = false

            // Coba ambil data user secara spesifik (cepat & payload kecil)
            try {
                bodyStr = fetchJsonFromCloud("getUsers", timeoutSeconds = timeoutSeconds)
                val testRoot = if (bodyStr.startsWith("{")) JSONObject(bodyStr) else JSONObject()
                if (testRoot.has("users")) {
                    fetchSuccess = true
                }
            } catch (_: Exception) {
                // Lanjut ke fallback
            }

            // Fallback jika Google Script belum diperbarui dengan endpoint getUsers
            if (!fetchSuccess) {
                try {
                    bodyStr = fetchJsonFromCloud("getMasterData", timeoutSeconds = timeoutSeconds)
                } catch (e: Exception) {
                    return@withContext Result.failure(Exception("Koneksi internet lambat atau terputus saat memeriksa database user."))
                }
            }

            val root = if (bodyStr.startsWith("{")) JSONObject(bodyStr) else JSONObject()
            if (root.optString("status") == "error") {
                return@withContext Result.failure(Exception(root.optString("message", "Error dari Google Script")))
            }

            if (root.has("users")) {
                val arr = root.getJSONArray("users")
                val existingUsers = userDao.getAllUsersList()
                val uList = mutableListOf<UserEntity>()
                val cloudNipSet = mutableSetOf<String>()
                val cloudUsernameSet = mutableSetOf<String>()

                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val nip = obj.optStringFlexible("NIP", "nip", "id_petugas", "id")
                    val username = obj.optStringFlexible("USERNAMA", "username", "user").lowercase()
                    if (nip.isEmpty() && username.isEmpty()) continue
                    if (nip.isNotEmpty()) cloudNipSet.add(nip)
                    if (username.isNotEmpty()) cloudUsernameSet.add(username)
                    val existing = existingUsers.find { it.nip.equals(nip, ignoreCase = true) || (it.username.isNotEmpty() && it.username.equals(username, ignoreCase = true)) }
                    val rawKode = obj.optStringFlexible("KODE_UNIK", "kodeUnik", "kode_unik")
                    val kodeUnik = if (rawKode.isNotBlank()) rawKode else (existing?.kodeUnik?.ifBlank { null } ?: UniqueCodeGenerator.generateUserCode())
                    uList.add(
                        UserEntity(
                            nip = nip.ifEmpty { "U${i + 1}" },
                            kodeUnik = kodeUnik,
                            namaPetugas = obj.optStringFlexible("NAMA_PETUGAS", "namaPetugas", "nama", "petugas"),
                            username = username.ifEmpty { "user${i + 1}" },
                            password = obj.optStringFlexible("PASSWORD", "password", default = "123"),
                            levelAkses = obj.optStringFlexible("LEVELAKSES", "levelAkses", "role", "level", default = "AUDITOR").uppercase(),
                            kodePool = obj.optStringFlexible("KODEPOOL", "kodePool", "pool", default = "SKT").uppercase(),
                            status = obj.optStringFlexible("STATUS", "status", default = "AKTIF").uppercase(),
                            tanggalAktif = run {
                                val rawTgl = obj.optStringFlexible("TANGGAL_AKTIF", "tanggalAktif", "tgl_aktif")
                                val norm = com.example.util.DateUtils.normalizeDateString(rawTgl)
                                if (norm.isNotBlank()) norm else rawTgl
                            }
                        )
                    )
                }

                if (uList.isNotEmpty()) {
                    userDao.insertUsers(uList)
                }
                return@withContext Result.success(true)
            } else {
                return@withContext Result.failure(Exception("Data user tidak ditemukan di database cloud."))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(Exception("Gagal memeriksa database user: ${e.message}"))
        }
    }

    suspend fun downloadMasterDataFromCloud(): Result<Boolean> = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) {
            return@withContext Result.failure(Exception("URL Google Script belum diatur."))
        }

        try {
            val bodyStr = fetchJsonFromCloud("getMasterData")
            val root = if (bodyStr.startsWith("{")) JSONObject(bodyStr) else JSONObject()
            if (root.optString("status") == "error") {
                return@withContext Result.failure(Exception(root.optString("message", "Error dari Google Script")))
            }

            // 1. Sync & Prune Pools (MASTER_POOL)
            if (root.has("pools")) {
                val arr = root.getJSONArray("pools")
                val existingPools = poolDao.getAllPoolsList()
                val poolList = mutableListOf<MasterPoolEntity>()
                val cloudKodePoolSet = mutableSetOf<String>()

                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val kodePool = obj.optStringFlexible("KODE_POOL", "kodePool", "KODE", "kode", default = "POOL-${i + 1}").uppercase()
                    if (kodePool.isEmpty()) continue
                    cloudKodePoolSet.add(kodePool)
                    val existing = existingPools.find { it.kodePool.equals(kodePool, ignoreCase = true) }
                    val rawKode = obj.optStringFlexible("KODE_UNIK", "kodeUnik", "kode_unik", "ID", "id")
                    val kodeUnik = if (rawKode.isNotBlank() && !rawKode.matches(Regex("^\\d+$"))) rawKode else (existing?.kodeUnik?.ifBlank { null } ?: UniqueCodeGenerator.generatePoolCode())
                    poolList.add(
                        MasterPoolEntity(
                            id = existing?.id ?: obj.optIntFlexible("ID", "id", default = i + 1),
                            kodeUnik = kodeUnik,
                            kodePool = kodePool,
                            namaPool = obj.optStringFlexible("NAMA_POOL", "namaPool", "NAMA", "nama", default = "POOL $kodePool").uppercase(),
                            alamatPool = obj.optStringFlexible("ALAMAT_POOL", "alamatPool", "ALAMAT", "alamat"),
                            statusArea = obj.optStringFlexible("STATUS_AREA", "statusArea", "STATUS", "status", default = "JABODETABEK").uppercase()
                        )
                    )
                }

                if (cloudKodePoolSet.isNotEmpty()) {
                    val poolsToDelete = existingPools.filter { !cloudKodePoolSet.contains(it.kodePool.trim().uppercase()) }
                    for (p in poolsToDelete) {
                        poolDao.deletePool(p)
                    }
                }

                if (poolList.isNotEmpty()) {
                    poolDao.insertPools(poolList)
                } else if (arr.length() == 0) {
                    poolDao.deleteAllPools()
                }
            }

            // 2. Sync & Prune Kendaraan (MASTER_KENDARAAN)
            if (root.has("kendaraan")) {
                val arr = root.getJSONArray("kendaraan")
                if (arr.length() > 0) {
                    processAndSaveKendaraanArray(arr)
                }
            }

            // 3. Sync & Prune Users (USER)
            if (root.has("users")) {
                val arr = root.getJSONArray("users")
                val existingUsers = userDao.getAllUsersList()
                val uList = mutableListOf<UserEntity>()
                val cloudNipSet = mutableSetOf<String>()
                val cloudUsernameSet = mutableSetOf<String>()

                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val nip = obj.optStringFlexible("NIP", "nip", "id_petugas", "id")
                    val username = obj.optStringFlexible("USERNAMA", "username", "user").lowercase()
                    if (nip.isEmpty() && username.isEmpty()) continue
                    if (nip.isNotEmpty()) cloudNipSet.add(nip)
                    if (username.isNotEmpty()) cloudUsernameSet.add(username)
                    val existing = existingUsers.find { it.nip.equals(nip, ignoreCase = true) || (it.username.isNotEmpty() && it.username.equals(username, ignoreCase = true)) }
                    val rawKode = obj.optStringFlexible("KODE_UNIK", "kodeUnik", "kode_unik")
                    val kodeUnik = if (rawKode.isNotBlank()) rawKode else (existing?.kodeUnik?.ifBlank { null } ?: UniqueCodeGenerator.generateUserCode())
                    uList.add(
                        UserEntity(
                            nip = nip.ifEmpty { "U${i + 1}" },
                            kodeUnik = kodeUnik,
                            namaPetugas = obj.optStringFlexible("NAMA_PETUGAS", "namaPetugas", "nama", "petugas"),
                            username = username.ifEmpty { "user${i + 1}" },
                            password = obj.optStringFlexible("PASSWORD", "password", default = "123"),
                            levelAkses = obj.optStringFlexible("LEVELAKSES", "levelAkses", "role", "level", default = "AUDITOR").uppercase(),
                            kodePool = obj.optStringFlexible("KODEPOOL", "kodePool", "pool", default = "SKT").uppercase(),
                            status = obj.optStringFlexible("STATUS", "status", default = "AKTIF").uppercase(),
                            tanggalAktif = run {
                                val rawTgl = obj.optStringFlexible("TANGGAL_AKTIF", "tanggalAktif", "tgl_aktif")
                                val norm = com.example.util.DateUtils.normalizeDateString(rawTgl)
                                if (norm.isNotBlank()) norm else rawTgl
                            }
                        )
                    )
                }

                if (cloudNipSet.isNotEmpty() || cloudUsernameSet.isNotEmpty()) {
                    val usersToDelete = existingUsers.filter {
                        if (it.username.equals("admin", ignoreCase = true)) return@filter false
                        val hasNip = it.nip.trim().isNotEmpty() && cloudNipSet.contains(it.nip.trim())
                        val hasUser = it.username.trim().isNotEmpty() && cloudUsernameSet.contains(it.username.trim().lowercase())
                        !hasNip && !hasUser
                    }
                    for (u in usersToDelete) {
                        userDao.deleteUser(u)
                    }
                }

                if (uList.isNotEmpty()) {
                    userDao.insertUsers(uList)
                } else if (arr.length() == 0) {
                    userDao.deleteAllUsers()
                }
            }

            // 4. Sync & Prune Master Bobot (MASTER_BOBOT)
            if (root.has("bobot")) {
                val arr = root.getJSONArray("bobot")
                val existingBobot = bobotDao.getAllBobot()
                val bList = mutableListOf<MasterBobotEntity>()
                val cloudItemIdSet = mutableSetOf<String>()

                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val itemId = obj.optStringFlexible("ITEM_ID", "itemId", "item_id", "id")
                    if (itemId.isNotEmpty()) {
                        cloudItemIdSet.add(itemId)
                        val existing = existingBobot.find { it.itemId.equals(itemId, ignoreCase = true) }
                        val rawKode = obj.optStringFlexible("KODE_UNIK", "kodeUnik", "kode_unik")
                        val kodeUnik = if (rawKode.isNotBlank()) rawKode else (existing?.kodeUnik?.ifBlank { null } ?: UniqueCodeGenerator.generateBobotCode())
                        bList.add(
                            MasterBobotEntity(
                                itemId = itemId,
                                kodeUnik = kodeUnik,
                                itemName = obj.optStringFlexible("ITEM_NAME", "itemName", "nama_item", default = itemId),
                                sectionTitle = obj.optStringFlexible("SECTION_TITLE", "sectionTitle", "section"),
                                groupTitle = obj.optStringFlexible("GROUP_TITLE", "groupTitle", "group"),
                                pointMax = obj.optIntFlexible("POINT_MAX", "pointMax", "max", default = 1),
                                bobot = obj.optDoubleFlexible("BOBOT", "bobot", default = 1.0),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }

                if (cloudItemIdSet.isNotEmpty()) {
                    val bobotToDelete = existingBobot.filter { !cloudItemIdSet.contains(it.itemId.trim()) }
                    for (b in bobotToDelete) {
                        bobotDao.deleteByItemId(b.itemId)
                    }
                }

                if (bList.isNotEmpty()) {
                    bobotDao.insertAll(bList)
                } else if (arr.length() == 0) {
                    bobotDao.deleteAll()
                }
            }

            // 5. Sync & Prune Master Sample (MASTER_SAMPLE)
            val sampleArr = when {
                root.has("sample") -> root.optJSONArray("sample")
                root.has("samples") -> root.optJSONArray("samples")
                root.has("master_sample") -> root.optJSONArray("master_sample")
                root.has("masterSample") -> root.optJSONArray("masterSample")
                else -> null
            }
            if (sampleArr != null) {
                val existingSamples = sampleDao.getAllSamples()
                val sList = mutableListOf<MasterSampleEntity>()
                val cloudStatusAreaSet = mutableSetOf<String>()

                for (i in 0 until sampleArr.length()) {
                    val obj = sampleArr.getJSONObject(i)
                    val statusArea = obj.optStringFlexible("STATUS_AREA", "statusArea", "area", "status", "nama_area", "namaArea", "wilayah").trim().uppercase()
                    if (statusArea.isNotEmpty()) {
                        cloudStatusAreaSet.add(statusArea)
                        val existing = existingSamples.find { it.statusArea.trim().equals(statusArea, ignoreCase = true) }
                        val rawKode = obj.optStringFlexible("KODE_UNIK", "kodeUnik", "kode_unik")
                        val kodeUnik = if (rawKode.isNotBlank()) rawKode else (existing?.kodeUnik?.ifBlank { null } ?: UniqueCodeGenerator.generateSampleCode())
                        sList.add(
                            MasterSampleEntity(
                                statusArea = statusArea,
                                kodeUnik = kodeUnik,
                                jumlahSample = obj.optIntFlexible("JUMLAH_SAMPLE", "jumlahSample", "jumlah", "sample", "qty", "kuota", "target", default = 10),
                                keterangan = obj.optStringFlexible("KETERANGAN", "keterangan", "ket", "catatan", "deskripsi", default = "Standar sample armada area $statusArea"),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }

                if (cloudStatusAreaSet.isNotEmpty()) {
                    val samplesToDelete = existingSamples.filter { !cloudStatusAreaSet.contains(it.statusArea.trim().uppercase()) }
                    for (s in samplesToDelete) {
                        sampleDao.delete(s.statusArea)
                    }
                }

                if (sList.isNotEmpty()) {
                    sampleDao.insertAll(sList)
                } else if (sampleArr.length() == 0) {
                    sampleDao.deleteAll()
                }
            }

            // 6. Sync & Prune Master Periode (MASTER_PERIODE)
            if (root.has("periode")) {
                val arr = root.getJSONArray("periode")
                val existingPeriode = periodeDao.getAllPeriodeList()
                val pList = mutableListOf<MasterPeriodeEntity>()
                val cloudPeriodeNameSet = mutableSetOf<String>()

                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val periodeName = obj.optStringFlexible("PERIODE", "periode", "nama_periode")
                    if (periodeName.isNotEmpty()) {
                        cloudPeriodeNameSet.add(periodeName.uppercase())
                        val existing = existingPeriode.find { it.periode.equals(periodeName, ignoreCase = true) }
                        val rawKode = obj.optStringFlexible("KODE_UNIK", "kodeUnik", "kode_unik")
                        val kodeUnik = if (rawKode.isNotBlank()) rawKode else (existing?.kodeUnik?.ifBlank { null } ?: UniqueCodeGenerator.generatePeriodeCode())
                        pList.add(
                            MasterPeriodeEntity(
                                id = existing?.id ?: obj.optIntFlexible("ID", "id", default = 0),
                                kodeUnik = kodeUnik,
                                periode = periodeName,
                                keterangan = obj.optStringFlexible("KETERANGAN", "keterangan", "ket")
                            )
                        )
                    }
                }

                if (cloudPeriodeNameSet.isNotEmpty()) {
                    val periodeToDelete = existingPeriode.filter { !cloudPeriodeNameSet.contains(it.periode.trim().uppercase()) }
                    for (p in periodeToDelete) {
                        periodeDao.deletePeriode(p)
                    }
                }

                if (pList.isNotEmpty()) {
                    periodeDao.insertAll(pList)
                } else if (arr.length() == 0) {
                    periodeDao.deleteAllPeriode()
                }
            }

            // 7. Sync & Prune Master Grade KPI (MASTER_GRADE_KPI)
            if (root.has("grade_kpi") || root.has("grade") || root.has("master_grade_kpi")) {
                val arr = when {
                    root.has("grade_kpi") -> root.getJSONArray("grade_kpi")
                    root.has("master_grade_kpi") -> root.getJSONArray("master_grade_kpi")
                    else -> root.getJSONArray("grade")
                }
                val existingGrades = gradeKpiDao.getAllGradeKpi()
                val gList = mutableListOf<MasterGradeKpiEntity>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val rawCat = obj.optStringFlexible("KATEGORI", "kategori", "grade", "kategory", "category", "cat").trim().uppercase()
                    val kategori = if (rawCat.startsWith("GRADE ")) rawCat.removePrefix("GRADE ").trim() else rawCat
                    if (kategori.isNotEmpty() && kategori in listOf("A", "B", "C", "D", "E")) {
                        val existing = existingGrades.find { it.kategori.equals(kategori, ignoreCase = true) }
                        val defMin = existing?.minScore ?: when (kategori) {
                            "A" -> 70.00
                            "B" -> 60.00
                            "C" -> 50.00
                            "D" -> 30.00
                            "E" -> 0.00
                            else -> 0.00
                        }
                        val defMax = existing?.maxScore ?: when (kategori) {
                            "A" -> 80.00
                            "B" -> 69.99
                            "C" -> 59.99
                            "D" -> 49.99
                            "E" -> 29.99
                            else -> 80.00
                        }
                        var minScore = obj.optDoubleFlexible(
                            "MIN_SCORE", "minScore", "min", "nilai_min", "nilaimin", "skor_min", "skormin", "rentang_min", "minimal",
                            default = defMin
                        )
                        var maxScore = obj.optDoubleFlexible(
                            "MAX_SCORE", "maxScore", "max", "nilai_max", "nilaimax", "skor_max", "skormax", "rentang_max", "maksimal",
                            default = defMax
                        )

                        if (minScore > maxScore) {
                            val tmp = minScore
                            minScore = maxScore
                            maxScore = tmp
                        }

                        // Proteksi: Jika kategori B, C, D tiba-tiba memiliki range default 0..80 (akibat kolom kosong / parser gagal di cloud)
                        // dan aplikasi sudah memiliki data lokal yang valid, pertahankan data lokal
                        if (kategori in listOf("B", "C", "D") && minScore == 0.0 && maxScore >= 80.0 && existing != null) {
                            minScore = existing.minScore
                            maxScore = existing.maxScore
                        }

                        val keterangan = obj.optStringFlexible(
                            "KETERANGAN", "keterangan", "ket", "deskripsi", "label",
                            default = existing?.keterangan ?: ""
                        )
                        val rawKode = obj.optStringFlexible("KODE_UNIK", "kodeUnik", "kode_unik")
                        val kodeUnik = if (rawKode.isNotBlank()) rawKode else (existing?.kodeUnik?.ifBlank { null } ?: UniqueCodeGenerator.generateGradeCode())
                        gList.add(
                            MasterGradeKpiEntity(
                                kategori = kategori,
                                kodeUnik = kodeUnik,
                                minScore = minScore,
                                maxScore = maxScore,
                                keterangan = keterangan,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
                if (gList.isNotEmpty()) {
                    gradeKpiDao.insertAll(gList)
                } else if (existingGrades.isEmpty()) {
                    ensureGradeKpiSeeded()
                    if (sessionManager.scriptUrl.isNotBlank()) {
                        syncAllMasterGradeKpiToCloud()
                    }
                } else if (arr.length() == 0 && existingGrades.isNotEmpty() && sessionManager.scriptUrl.isNotBlank()) {
                    // Jika lembar di Google Sheets kosong, sinkronkan master lokal ke cloud
                    syncAllMasterGradeKpiToCloud()
                }
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mengunduh dan menyinkronkan data Master Sample secara langsung dari Google Sheets.
     * Mengikuti tabel MASTER_SAMPLE di Google Sheets dan memperbarui Room Database.
     */
    suspend fun syncSamplesFromCloud(): Result<Int> = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) {
            return@withContext Result.failure(Exception("URL Google Script belum diatur."))
        }
        try {
            val bodyStr = fetchJsonFromCloud("getMasterData")
            val root = if (bodyStr.startsWith("{")) JSONObject(bodyStr) else JSONObject()
            if (root.optString("status") == "error") {
                return@withContext Result.failure(Exception(root.optString("message", "Error dari Google Script")))
            }
            val sampleArr = when {
                root.has("sample") -> root.optJSONArray("sample")
                root.has("samples") -> root.optJSONArray("samples")
                root.has("master_sample") -> root.optJSONArray("master_sample")
                root.has("masterSample") -> root.optJSONArray("masterSample")
                else -> null
            }
            if (sampleArr != null) {
                val existingSamples = sampleDao.getAllSamples()
                val sList = mutableListOf<MasterSampleEntity>()
                val cloudStatusAreaSet = mutableSetOf<String>()

                for (i in 0 until sampleArr.length()) {
                    val obj = sampleArr.getJSONObject(i)
                    val statusArea = obj.optStringFlexible("STATUS_AREA", "statusArea", "area", "status", "nama_area", "namaArea", "wilayah").trim().uppercase()
                    if (statusArea.isNotEmpty()) {
                        cloudStatusAreaSet.add(statusArea)
                        val existing = existingSamples.find { it.statusArea.trim().equals(statusArea, ignoreCase = true) }
                        val rawKode = obj.optStringFlexible("KODE_UNIK", "kodeUnik", "kode_unik")
                        val kodeUnik = if (rawKode.isNotBlank()) rawKode else (existing?.kodeUnik?.ifBlank { null } ?: UniqueCodeGenerator.generateSampleCode())
                        sList.add(
                            MasterSampleEntity(
                                statusArea = statusArea,
                                kodeUnik = kodeUnik,
                                jumlahSample = obj.optIntFlexible("JUMLAH_SAMPLE", "jumlahSample", "jumlah", "sample", "qty", "kuota", "target", default = 10),
                                keterangan = obj.optStringFlexible("KETERANGAN", "keterangan", "ket", "catatan", "deskripsi", default = "Standar sample armada area $statusArea"),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }

                if (cloudStatusAreaSet.isNotEmpty()) {
                    val samplesToDelete = existingSamples.filter { !cloudStatusAreaSet.contains(it.statusArea.trim().uppercase()) }
                    for (s in samplesToDelete) {
                        sampleDao.delete(s.statusArea)
                    }
                }

                if (sList.isNotEmpty()) {
                    sampleDao.insertAll(sList)
                } else if (sampleArr.length() == 0) {
                    sampleDao.deleteAll()
                }
                return@withContext Result.success(sList.size)
            } else {
                return@withContext Result.failure(Exception("Data tabel sample tidak ditemukan dalam respon Google Sheets."))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    /**
     * Memproses array kendaraan dari Google Sheets secara tangguh dan cepat:
     * - Mendukung berbagai format nama kolom (NO_MOBIL, NO_KENDARAAN, NO_BODY, ARMADA, PLAT_NOMER, dll.)
     * - Menetapkan ID unik untuk mencegah benturan dan kehilangan data di SQLite Room
     * - Tidak memotong / membatasi baris kendaraan sehingga seluruh data tampil lengkap
     */
    suspend fun processAndSaveKendaraanArray(arr: JSONArray): Int = withContext(Dispatchers.IO) {
        val existingVehicles = kendaraanDao.getAllKendaraanList()
        val existingMap = existingVehicles.associateBy { it.noMobil.trim().uppercase() }
        val usedIds = existingVehicles.map { it.id }.filter { it > 0 }.toMutableSet()
        var maxId = existingVehicles.maxOfOrNull { it.id } ?: 0

        val vList = mutableListOf<MasterKendaraanEntity>()
        val cloudNoMobilSet = mutableSetOf<String>()

        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)

            // 1. Ambil nomor kendaraan dengan berbagai variasi penamaan kolom
            var noMobil = obj.optStringFlexible(
                "NO_MOBIL", "noMobil", "nomobil", "no_mobil", "nomor_mobil", "nomer_mobil",
                "NO_KENDARAAN", "noKendaraan", "nokendaraan", "no_kendaraan", "nomor_kendaraan", "nomorkendaraan",
                "NO_BODY", "noBody", "nobody", "no_body", "nomor_body", "nomorbody", "body", "BODY",
                "ARMADA", "armada", "no_armada", "noarmada", "KENDARAAN", "kendaraan",
                "NO_POLISI", "nopolisi", "no_pol", "nopol", "NOMOR", "nomor", "NO", "no"
            ).uppercase().trim()

            // 2. Ambil plat nomor
            val platNomer = obj.optStringFlexible(
                "PLAT_NOMER", "platNomer", "plat_nomor", "platnomor", "plat", "nopol",
                "NO_POLISI", "nopolisi", "nomor_polisi", "nomorpolisi", "no_pol", "nopol", "polisi"
            ).uppercase().trim()

            // Jika noMobil kosong tetapi platNomer terisi, gunakan platNomer sebagai noMobil
            if (noMobil.isEmpty() && platNomer.isNotEmpty()) {
                noMobil = platNomer
            }

            // Jika masih kosong, coba ambil nilai dari properti pertama yang bermakna
            if (noMobil.isEmpty()) {
                val iter = obj.keys()
                while (iter.hasNext()) {
                    val k = iter.next()
                    val cleanK = k.lowercase().replace("[^a-z0-9]".toRegex(), "")
                    if (cleanK != "id" && cleanK != "kodeunik" && cleanK != "kode" && cleanK != "tahun" && cleanK != "pool" && cleanK != "tipe" && cleanK != "warna") {
                        val v = obj.optString(k, "").trim()
                        if (v.isNotEmpty()) {
                            noMobil = v.uppercase()
                            break
                        }
                    }
                }
            }

            if (noMobil.isEmpty()) continue

            // Hindari duplikat noMobil dalam response cloud yang sama
            if (cloudNoMobilSet.contains(noMobil)) continue
            cloudNoMobilSet.add(noMobil)

            val existing = existingMap[noMobil]
            val sheetId = obj.optIntFlexible("ID", "id", default = 0)

            // Penetapan ID yang aman dan unik untuk mencegah collision REPLACE di Room
            val assignedId = when {
                existing != null && existing.id > 0 -> existing.id
                sheetId > 0 && !usedIds.contains(sheetId) -> {
                    usedIds.add(sheetId)
                    sheetId
                }
                else -> {
                    maxId++
                    while (usedIds.contains(maxId)) { maxId++ }
                    usedIds.add(maxId)
                    maxId
                }
            }

            val rawKode = obj.optStringFlexible("KODE_UNIK", "kodeUnik", "kode_unik")
            val kodeUnik = if (rawKode.isNotBlank() && !rawKode.matches(Regex("^\\d+$"))) {
                rawKode
            } else {
                existing?.kodeUnik?.ifBlank { null } ?: UniqueCodeGenerator.generateKendaraanCode()
            }

            val tipe = obj.optStringFlexible("TIPE", "tipe", "type", "model", "merk", "jenis")
            val warna = obj.optStringFlexible("WARNA", "warna", "color")
            val poolVal = obj.optStringFlexible("POOL", "pool", "kode_pool", "kodepool", "lokasi_pool", "nama_pool", "lokasi").uppercase().trim()
            val effectivePool = if (poolVal.isNotBlank()) poolVal else (existing?.pool?.ifBlank { "SKT" } ?: "SKT")
            val tahun = obj.optStringFlexible("TAHUN", "tahun", "year")
            val tanggalService = obj.optStringFlexible("TANGGAL_SERVICE", "tanggalService", "tgl_service")
            val service = obj.optStringFlexible("SERVICE", "service", "paket_service")

            vList.add(
                MasterKendaraanEntity(
                    id = assignedId,
                    kodeUnik = kodeUnik,
                    noMobil = noMobil,
                    platNomer = if (platNomer.isNotEmpty()) platNomer else (existing?.platNomer ?: noMobil),
                    tipe = tipe.ifBlank { existing?.tipe ?: "-" },
                    warna = warna.ifBlank { existing?.warna ?: "-" },
                    pool = effectivePool,
                    tahun = tahun.ifBlank { existing?.tahun ?: "" },
                    tanggalService = tanggalService.ifBlank { existing?.tanggalService ?: "" },
                    service = service.ifBlank { existing?.service ?: "" }
                )
            )
        }

        // Hapus kendaraan lokal yang sudah tidak ada di Google Sheets jika data cloud valid
        if (cloudNoMobilSet.isNotEmpty()) {
            val vehiclesToDelete = existingVehicles.filter { !cloudNoMobilSet.contains(it.noMobil.trim().uppercase()) }
            for (v in vehiclesToDelete) {
                kendaraanDao.deleteKendaraan(v)
            }
        }

        if (vList.isNotEmpty()) {
            kendaraanDao.insertKendaraanList(vList)
        } else if (arr.length() == 0) {
            kendaraanDao.deleteAllKendaraan()
        }

        vList.size
    }

    /**
     * Mengunduh KHUSUS Master Kendaraan dari Google Sheets secara super cepat untuk skala 80.000+ data:
     * 1. Menggunakan format matriks kompak "data": [ ["NO_MOBIL", "PLAT", ...], ... ] (memotong payload hingga 85%)
     * 2. Mendukung filter Pool langsung di server (misal sinkronisasi Pool SKT selesai dalam < 1 detik)
     * 3. Mendukung chunking batch 20.000 baris sheet secara bertahap tanpa risiko timeout script
     * 4. Menyimpan ke database SQLite Room dalam batch 2.000 data per transaksi
     */
    suspend fun syncKendaraanOnlyFromCloud(
        poolFilter: String = "",
        onProgress: (current: Int, total: Int, message: String) -> Unit = { _, _, _ -> }
    ): Result<Int> = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) {
            return@withContext Result.failure(Exception("URL Google Script belum diatur di menu Pengaturan."))
        }

        val targetPool = poolFilter.trim()
        val isSpecificPool = targetPool.isNotBlank() && !targetPool.equals("SEMUA", ignoreCase = true) && !targetPool.equals("ALL", ignoreCase = true)

        try {
            var totalSaved = 0
            val batchLimit = 5000
            var offset = 0
            var hasMore = true
            var serverTotal = 0
            var isFirstBatch = true

            // Jika pool spesifik dipilih: bersihkan kendaraan lama di pool tersebut sebelum diisi data fresh
            if (isSpecificPool) {
                kendaraanDao.deleteKendaraanByPool(targetPool)
            }

            while (hasMore) {
                val params = mutableMapOf(
                    "format" to "compact",
                    "offset" to offset.toString(),
                    "limit" to batchLimit.toString()
                )
                if (isSpecificPool) {
                    params["pool"] = targetPool
                }

                val targetLabel = if (isSpecificPool) "Pool $targetPool" else "Semua Pool"
                onProgress(totalSaved, serverTotal, "Mengunduh data $targetLabel (offset: $offset)...")

                var bodyStr = ""
                try {
                    bodyStr = fetchJsonFromCloud("getKendaraan", params)
                } catch (e: Exception) {
                    if (isFirstBatch) {
                        bodyStr = fetchJsonFromCloud("getMasterData")
                    } else {
                        throw e
                    }
                }

                var batchRowsCount = 0
                val entitiesToInsert = mutableListOf<MasterKendaraanEntity>()

                if (bodyStr.startsWith("{")) {
                    val root = JSONObject(bodyStr)
                    if (root.optString("status") == "error") {
                        return@withContext Result.failure(Exception(root.optString("message", "Error dari Google Sheets")))
                    }

                    serverTotal = root.optInt("total", 0)

                    // 1. Cek format Matrix Kompak: "data": [ ["101", "B 101 ABC", "AVANZA", ...], ... ]
                    if (root.has("data")) {
                        val dataArr = root.getJSONArray("data")
                        batchRowsCount = dataArr.length()

                        for (i in 0 until batchRowsCount) {
                            val row = dataArr.getJSONArray(i)
                            val noMobil = row.optString(0, "").trim().uppercase()
                            val plat = row.optString(1, "").trim().uppercase().ifBlank { noMobil }
                            if (noMobil.isBlank() && plat.isBlank()) continue

                            val tipe = row.optString(2, "").trim()
                            val warna = row.optString(3, "").trim()
                            val rowPool = row.optString(4, "").trim().uppercase().ifBlank { if (isSpecificPool) targetPool else "SKT" }
                            val tahun = row.optString(5, "").trim()

                            entitiesToInsert.add(
                                MasterKendaraanEntity(
                                    noMobil = noMobil.ifBlank { plat },
                                    platNomer = plat,
                                    tipe = tipe.ifBlank { "-" },
                                    warna = warna.ifBlank { "-" },
                                    pool = rowPool,
                                    tahun = tahun
                                )
                            )
                        }

                        hasMore = root.optBoolean("hasMore", false) && batchRowsCount > 0
                        offset += batchRowsCount
                    } else if (root.has("kendaraan")) {
                        // 2. Format JSON Object legacy
                        val arr = root.getJSONArray("kendaraan")
                        totalSaved = processAndSaveKendaraanArray(arr)
                        hasMore = false
                        break
                    }
                } else if (bodyStr.startsWith("[")) {
                    val arr = JSONArray(bodyStr)
                    totalSaved = processAndSaveKendaraanArray(arr)
                    hasMore = false
                    break
                }

                // Simpan batch ke SQLite Room dalam chunk 2.000 data
                if (entitiesToInsert.isNotEmpty()) {
                    if (isFirstBatch) {
                        if (isSpecificPool) {
                            kendaraanDao.deleteKendaraanByPool(targetPool)
                            val shortCode = targetPool.removePrefix("POOL").trim()
                            if (shortCode.isNotBlank() && shortCode != targetPool) {
                                kendaraanDao.deleteKendaraanByPool(shortCode)
                            }
                        } else {
                            kendaraanDao.deleteAllKendaraan()
                        }
                    }

                    entitiesToInsert.chunked(2000).forEach { chunk ->
                        kendaraanDao.insertKendaraanList(chunk)
                    }

                    totalSaved += entitiesToInsert.size
                    val targetLabel = if (isSpecificPool) " (Pool $targetPool)" else ""
                    onProgress(totalSaved, if (serverTotal > 0) serverTotal else totalSaved, "Menyimpan ke database: $totalSaved unit$targetLabel...")
                } else {
                    hasMore = false
                }

                isFirstBatch = false

                if (serverTotal > 0 && offset >= serverTotal) {
                    hasMore = false
                }
                if (batchRowsCount == 0) {
                    hasMore = false
                }
            }

            Result.success(totalSaved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mengunduh data audit dari Google Sheets dan memperbarui secara in-place di database lokal,
     * serta MENGHAPUS data di aplikasi jika baris tersebut sudah dihapus / tidak ada di Google Sheets.
     * Menggunakan batch insert & filter perbandingan ID/Composite key agar proses sangat cepat (<50ms).
     */
    suspend fun downloadAuditsFromCloud(): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) {
            return@withContext Result.failure(Exception("URL Google Script belum diatur di menu Pengaturan."))
        }

        try {
            val bodyStr = fetchJsonFromCloud("getAudits")

            // Pastikan bukan error JSON dari Google Script
            if (bodyStr.startsWith("{")) {
                val root = JSONObject(bodyStr)
                if (root.optString("status") == "error") {
                    return@withContext Result.failure(Exception(root.optString("message", "Gagal membaca sheet DATA_AUDIT")))
                }
            }

            val arr: JSONArray = when {
                bodyStr.startsWith("[") -> JSONArray(bodyStr)
                bodyStr.startsWith("{") -> {
                    val root = JSONObject(bodyStr)
                    when {
                        root.has("data") -> root.getJSONArray("data")
                        root.has("audits") -> root.getJSONArray("audits")
                        root.has("items") -> root.getJSONArray("items")
                        else -> JSONArray()
                    }
                }
                else -> JSONArray()
            }

            val existingAudits = auditDao.getAllAuditsList()

            // Jika di Google Sheet DATA_AUDIT benar-benar kosong (0 baris),
            // hanya hapus data yang sebelumnya SUDAH tersinkron ke cloud (isSynced = true).
            // JANGAN hapus temuan audit lokal yang belum sempat terkirim (drafts/pending)!
            if (arr.length() == 0) {
                val syncedAudits = existingAudits.filter { it.isSynced }
                for (audit in syncedAudits) {
                    auditDao.deleteAudit(audit)
                }
                return@withContext Result.success(Pair(0, syncedAudits.size))
            }

            val existingMapByCloudId = existingAudits.filter { it.cloudId > 0 }.associateBy { it.cloudId }
            val existingMapByUuid = existingAudits.filter { it.auditUuid.isNotBlank() }.associateBy { it.auditUuid }
            val existingMapByCarPeriod = existingAudits.associateBy { "${it.noMobil.trim().uppercase()}_${it.periode.trim().uppercase()}" }
            val existingMapByComposite = existingAudits.associateBy { "${it.noMobil.trim().uppercase()}_${it.periode.trim().uppercase()}_${it.tanggal.trim()}" }

            val auditsToSave = mutableListOf<DataAuditEntity>()
            val seenCloudIds = mutableSetOf<String>()
            val cloudIds = mutableSetOf<Long>()
            val cloudCarPeriodKeys = mutableSetOf<String>()
            val cloudFullCompositeKeys = mutableSetOf<String>()
            val cloudPlatePeriodKeys = mutableSetOf<String>()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val rawIdStr = obj.optStringFlexible("ID", "id", "no", "KODE_UNIK", "kodeUnik", "AUDIT_UUID", "auditUuid").trim().uppercase()
                if (rawIdStr.isNotEmpty()) {
                    if (seenCloudIds.contains(rawIdStr)) {
                        // Data hasil INPUT AUDIT tidak akan di tampilkan jika di temukan ID yang sama
                        continue
                    }
                    seenCloudIds.add(rawIdStr)
                }

                var noMobil = obj.optStringFlexible("NO_MOBIL", "noMobil", "nomobil", "nomor_mobil", "no_mobil", "nomer_mobil").uppercase()
                val platNomer = obj.optStringFlexible("PLAT_NOMER", "platNomer", "plat_nomor", "plat", "nopol").uppercase()
                val periode = obj.optStringFlexible("PERIODE", "periode", "nama_periode")
                val waktuTimestamp = AuditDatabaseSchema.extractWaktuTimestampFromSheetRow(obj)
                    .ifBlank { obj.optStringFlexible("WAKTU/TIMESTAMP", "waktu/timestamp", "waktu_timestamp", "KOLOM_3", "kolom_3", "_col_3", "col_3", "TIMESTAMP", "timestamp", "WAKTU", "waktu") }
                val rawTanggal = obj.optStringFlexible("TANGGAL", "tanggal", "tgl", "date")
                // Sesuai instruksi: untuk tanggal mengambil data dari kolom ke-3 google sheets dengan nama kolom WAKTU/TIMESTAMP
                val tanggal = if (waktuTimestamp.isNotEmpty()) waktuTimestamp else rawTanggal
                val sheetId = obj.optLongFlexible("ID", "id", "no")
                val rowUuid = obj.optStringFlexible("AUDIT_UUID", "auditUuid", "uuid")

                if (noMobil.isEmpty() && platNomer.isNotEmpty()) {
                    noMobil = platNomer
                }

                if (noMobil.isEmpty()) continue

                val carPeriodKey = "${noMobil}_${periode.uppercase()}"
                val compositeKey = "${carPeriodKey}_$tanggal"
                cloudCarPeriodKeys.add(carPeriodKey)
                cloudFullCompositeKeys.add(compositeKey)
                if (platNomer.isNotEmpty()) {
                    cloudPlatePeriodKeys.add("${platNomer.replace(" ", "")}_${periode.uppercase()}")
                }
                if (sheetId > 0) {
                    cloudIds.add(sheetId)
                }

                val existing = (if (rowUuid.isNotBlank()) existingMapByUuid[rowUuid] else null)
                    ?: (if (sheetId > 0) existingMapByCloudId[sheetId] else null)
                    ?: existingMapByComposite[compositeKey]
                    ?: existingMapByCarPeriod[carPeriodKey]

                if (existing != null && existing.id > 0) {
                    cloudIds.add(existing.id)
                }

                val parsedEntity = AuditDatabaseSchema.parseSheetRowToAudit(obj, existing)
                val auditEntity = parsedEntity.copy(
                    id = existing?.id ?: 0L,
                    cloudId = if (sheetId > 0) sheetId else (existing?.cloudId ?: 0L),
                    isSynced = true,
                    syncTimestamp = System.currentTimeMillis()
                )
                auditsToSave.add(auditEntity)
            }

            // SAFETY GUARD: Jika Google Sheet punya data (arr.length() > 0) namun tidak ada yang berhasil diparse,
            // JANGAN hapus data lokal! Ini melindungi agar data tidak hilang secara keliru.
            if (arr.length() > 0 && auditsToSave.isEmpty()) {
                return@withContext Result.failure(Exception("Ditemukan ${arr.length()} baris di sheet DATA_AUDIT Google Sheets, namun kolom header (seperti NO_MOBIL) belum sesuai. Data lokal tetap dipertahankan."))
            }

            val uniqueAuditsToSave = AuditFilterUtils.filterUniqueAudits(auditsToSave)
            if (uniqueAuditsToSave.isNotEmpty()) {
                auditDao.insertAudits(uniqueAuditsToSave)
            }

            // HAPUS DATA AUDIT DI APLIKASI JIKA DI GOOGLE SHEET SUDAH TIDAK ADA!
            // Catatan: Hanya hapus data lokal yang SUDAH TERSINKRON (isSynced = true),
            // dan TIDAK ADA di Google Sheets. Draft lokal belum tersinkron tidak dihapus.
            val auditsToDelete = if (auditsToSave.isNotEmpty()) {
                existingAudits.filter { local ->
                    if (!local.isSynced) return@filter false // Lindungi draft lokal yang belum disinkron!

                    val carPeriod = "${local.noMobil.trim().uppercase()}_${local.periode.trim().uppercase()}"
                    val composite = "${carPeriod}_${local.tanggal.trim()}"
                    val platePeriod = if (local.platNomer.isNotBlank()) "${local.platNomer.replace(" ", "").uppercase()}_${local.periode.trim().uppercase()}" else ""

                    val matchedByCloudId = local.cloudId > 0 && cloudIds.contains(local.cloudId)
                    val matchedById = local.id > 0 && cloudIds.contains(local.id)
                    val matchedByCarPeriod = cloudCarPeriodKeys.contains(carPeriod)
                    val matchedByComposite = cloudFullCompositeKeys.contains(composite)
                    val matchedByPlate = platePeriod.isNotEmpty() && cloudPlatePeriodKeys.contains(platePeriod)

                    !matchedByCloudId && !matchedById && !matchedByCarPeriod && !matchedByComposite && !matchedByPlate
                }
            } else {
                emptyList()
            }

            for (del in auditsToDelete) {
                auditDao.deleteAudit(del)
            }

            Result.success(Pair(auditsToSave.size, auditsToDelete.size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Melakukan pencocokan data 2 arah secara komprehensif dengan Google Sheets:
     * 1. Mengunggah temuan audit lokal yang belum tersinkron (pending)
     * 2. Mengunduh & mencocokkan Master Data (Pool, Kendaraan, User, Master Bobot, Master Sample, Master Periode),
     *    serta menghapus record di aplikasi jika sudah tidak ada di Google Sheets.
     * 3. Mengunduh & mencocokkan Data Audit yang ada di Google Sheets,
     *    serta menghapus data audit di aplikasi jika sudah tidak ada di Google Sheets.
     */
    suspend fun reconcileWithGoogleSheets(): ReconcileResult = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) {
            return@withContext ReconcileResult(
                success = false,
                pendingUploaded = 0,
                pendingFailed = 0,
                masterDataSuccess = false,
                auditsDownloaded = 0,
                auditsDeleted = 0,
                errorMessage = "URL Google Script belum diatur di menu Pengaturan."
            )
        }

        var uploadSuccessCount = 0
        var uploadFailCount = 0
        var masterSuccess = false
        var auditsCount = 0
        var auditsDeletedCount = 0
        var lastError: String? = null

        // 1. Sinkronisasi temuan audit lokal yang belum terkirim ke Google Sheets
        try {
            val (succ, fail) = syncAllPendingAudits()
            uploadSuccessCount = succ
            uploadFailCount = fail
        } catch (e: Exception) {
            lastError = "Gagal upload audit lokal: ${e.message}"
        }

        // 2. Cocokkan seluruh Master Data (Pools, Kendaraan, Users, Master Bobot, Master Sample, Master Periode)
        try {
            val masterRes = downloadMasterDataFromCloud()
            masterSuccess = masterRes.isSuccess
            if (!masterSuccess) {
                lastError = masterRes.exceptionOrNull()?.message ?: "Gagal unduh master data"
            }
        } catch (e: Exception) {
            masterSuccess = false
            lastError = "Gagal sinkron master data: ${e.message}"
        }

        // 3. Cocokkan seluruh Data Audit dari Google Sheets ke database lokal
        try {
            val auditsRes = downloadAuditsFromCloud()
            if (auditsRes.isSuccess) {
                val (downloaded, deleted) = auditsRes.getOrDefault(Pair(0, 0))
                auditsCount = downloaded
                auditsDeletedCount = deleted
            } else {
                lastError = auditsRes.exceptionOrNull()?.message ?: "Gagal unduh audit dari cloud"
            }
        } catch (e: Exception) {
            lastError = "Gagal mencocokkan audit: ${e.message}"
        }

        val isOverallSuccess = masterSuccess || auditsCount > 0 || uploadSuccessCount > 0 || auditsDeletedCount > 0 || lastError == null
        val timestamp = System.currentTimeMillis()
        sessionManager.lastSyncTime = timestamp

        val summaryParts = mutableListOf<String>()
        if (uploadSuccessCount > 0) summaryParts.add("$uploadSuccessCount upload")
        if (masterSuccess) summaryParts.add("Master data terupdate")
        if (auditsCount > 0) summaryParts.add("$auditsCount audit sinkron")
        if (auditsDeletedCount > 0) summaryParts.add("$auditsDeletedCount audit dihapus (mengikuti Google Sheets)")

        val summary = if (isOverallSuccess) {
            if (summaryParts.isNotEmpty()) "Pencocokan Berhasil: " + summaryParts.joinToString(", ") else "Data sinkron dengan Google Sheets"
        } else {
            "Pencocokan gagal: ${lastError ?: "Koneksi bermasalah"}"
        }
        sessionManager.lastSyncSummary = summary

        ReconcileResult(
            success = isOverallSuccess,
            pendingUploaded = uploadSuccessCount,
            pendingFailed = uploadFailCount,
            masterDataSuccess = masterSuccess,
            auditsDownloaded = auditsCount,
            auditsDeleted = auditsDeletedCount,
            errorMessage = lastError,
            timestamp = timestamp
        )
    }

    /**
     * Memanggil inisialisasi tabel INPUT_AUDIT di Google Sheets
     */
    suspend fun initInputAuditSheetRemote(): Result<String> = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) {
            return@withContext Result.failure(Exception("URL Google Script belum diatur."))
        }
        try {
            val jsonPayload = JSONObject().apply {
                put("action", "initInputAudit")
            }
            val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(scriptUrl)
                .post(requestBody)
                .build()

            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val resObj = try { JSONObject(body) } catch (e: Exception) { null }
                    val message = resObj?.optString("message") ?: "Tabel INPUT_AUDIT berhasil diinisialisasi."
                    Result.success(message)
                } else {
                    Result.failure(Exception("Server mengembalikan kode ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Memanggil inisialisasi lembar MASTER_GRADE_KPI di Google Sheets
     */
    suspend fun initGradeKpiSheetRemote(): Result<String> = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) {
            return@withContext Result.failure(Exception("URL Google Script belum diatur."))
        }
        try {
            val jsonPayload = JSONObject().apply {
                put("action", "initGradeKpi")
            }
            val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(scriptUrl)
                .post(requestBody)
                .build()

            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val resObj = try { JSONObject(body) } catch (e: Exception) { null }
                    val message = resObj?.optString("message") ?: "Tabel MASTER_GRADE_KPI berhasil diinisialisasi."
                    Result.success(message)
                } else {
                    Result.failure(Exception("Server mengembalikan kode ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Memanggil inisialisasi semua Sheet di Google Sheets
     */
    suspend fun setupSpreadsheetRemote(): Result<String> = withContext(Dispatchers.IO) {
        val scriptUrl = sessionManager.scriptUrl
        if (scriptUrl.isBlank()) {
            return@withContext Result.failure(Exception("URL Google Script belum diatur."))
        }
        try {
            val jsonPayload = JSONObject().apply {
                put("action", "setupSpreadsheet")
            }
            val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(scriptUrl)
                .post(requestBody)
                .build()

            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val resObj = try { JSONObject(body) } catch (e: Exception) { null }
                    val message = resObj?.optString("message") ?: "Seluruh sheet berhasil diinisialisasi."
                    Result.success(message)
                } else {
                    Result.failure(Exception("Server mengembalikan kode ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class ReconcileResult(
    val success: Boolean,
    val pendingUploaded: Int,
    val pendingFailed: Int,
    val masterDataSuccess: Boolean,
    val auditsDownloaded: Int,
    val auditsDeleted: Int = 0,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
