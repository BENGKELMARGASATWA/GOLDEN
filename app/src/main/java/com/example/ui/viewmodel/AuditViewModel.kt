package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.database.AppDatabase
import com.example.data.local.entity.DataAuditEntity
import com.example.data.local.entity.MasterBobotEntity
import com.example.data.local.entity.MasterGradeKpiEntity
import com.example.data.local.entity.MasterKendaraanEntity
import com.example.data.local.entity.MasterPeriodeEntity
import com.example.data.local.entity.MasterPoolEntity
import com.example.data.local.entity.MasterSampleEntity
import com.example.data.local.entity.PengaturanNamaPhotoEntity
import com.example.data.local.entity.UserEntity
import com.example.data.repository.AuditRepository
import com.example.data.session.SessionManager
import com.example.util.DateUtils
import com.example.util.UniqueCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    LOGIN,
    INPUT_AUDIT,
    RIWAYAT_SERVICE,
    RESUME_AUDIT,
    BOBOT_RESUME,
    REKAP_NILAI,
    REKAP_TEMUAN,
    PERINGKAT_KPI,
    SAMPLE,
    GRADE_KPI,
    MONITORING_ALL_POOL,
    MONITORING_AUDIT,
    HASIL_AUDIT_ALL_POOL,
    DOWNLOAD_PHOTO_AUDIT,
    MASTER_KENDARAAN,
    MASTER_POOL,
    MASTER_USER,
    MASTER_PERIODE,
    SETTING_BOBOT,
    PENGATURAN_NAMAPHOTO,
    GOOGLE_SCRIPT_CONFIG,
    WEB_SERVER_PC
}

fun defaultKpiScores(): Map<String, Int> {
    return com.example.data.model.KpiAuditSpec.getAllItems().associate { it.id to 0 }
}

data class AuditFormState(
    val editingAuditId: Long? = null,
    val editingAuditTimestamp: Long? = null,
    val originalTanggal: String? = null,
    val originalAuditorNip: String? = null,
    val originalAuditorName: String? = null,
    val initialTimestamp: Long = System.currentTimeMillis(), // Timestamp terkunci di awal saat form dibuka
    val kodeUnik: String = UniqueCodeGenerator.generateAuditCode(), // ID unik otomatis untuk setiap data audit baru
    
    // Header & Periode
    val periode: String = "",
    
    // Data Kendaraan
    val noMobil: String = "",
    val platNomer: String = "",
    val tipe: String = "",
    val warna: String = "",
    val tahun: String = "",
    val tanggalService: String = "",
    val service: String = "",
    val pool: String = "",
    val isVehicleFound: Boolean = false,
    
    // Sample
    val jenisSample: String = "",
    val sampleTanggal: String = "",
    val paketService: String = "",
    
    // KPI Scores (itemId -> Int score 0..maxPoints)
    // Default disetel 0 (Normal) sesuai instruksi, bukan lagi blank/wajib
    val kpiScores: Map<String, Int> = defaultKpiScores(),
    // KPI Photos (photoKey e.g. "body_dekok_0" -> localPath)
    val kpiPhotos: Map<String, String> = emptyMap(),
    // KPI Notes (photoKey -> noteText)
    val kpiNotes: Map<String, String> = emptyMap(),

    // Legacy photo list for backward compatibility
    val photos: List<String> = List(20) { "" },
    val notes: List<String> = List(20) { "" },
    val currentStepIndex: Int = 0,
    val isSubmitting: Boolean = false,
    val validationError: String? = null
) {
    val totalPoinTemuan: Int
        get() = kpiScores.values.sum()

    val filledPhotoCount: Int
        get() {
            val kpiCount = kpiPhotos.values.count { it.isNotBlank() }
            val legacyCount = photos.count { it.isNotBlank() }
            return maxOf(kpiCount, legacyCount)
        }

    val isEditMode: Boolean
        get() = editingAuditId != null

    val hasMissingNotesForPhotos: Boolean
        get() = false
}

class AuditViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    val repository = AuditRepository(application, database)

    // Current User & Auth
    private val _currentUser = MutableStateFlow<UserEntity?>(run {
        val saved = repository.sessionManager.getCurrentUser()
        val targetDeletedNips = setOf("1001", "1002", "1003", "1004", "3001", "2001")
        val targetDeletedUsernames = setOf("superadmin", "admin", "kabeng", "masterdata", "skt", "auditor")
        if (saved != null && (saved.nip in targetDeletedNips || saved.username.lowercase() in targetDeletedUsernames)) {
            repository.sessionManager.clearSession()
            null
        } else {
            saved
        }
    })
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Navigation
    private val _currentScreen = MutableStateFlow(
        if (repository.sessionManager.isSessionValid() && _currentUser.value != null) {
            val user = _currentUser.value
            if (user?.levelAkses?.trim()?.equals("AUDITOR", ignoreCase = true) == true && !repository.isAuditorActiveToday(user)) {
                repository.sessionManager.clearSession()
                _currentUser.value = null
                AppScreen.LOGIN
            } else {
                when (_currentUser.value?.levelAkses?.trim()?.uppercase()) {
                    "KEPALA BENGKEL" -> AppScreen.RIWAYAT_SERVICE
                    "AUDITOR" -> AppScreen.INPUT_AUDIT
                    "MASTER DATA" -> AppScreen.MASTER_KENDARAAN
                    else -> AppScreen.MONITORING_ALL_POOL
                }
            }
        } else {
            AppScreen.LOGIN
        }
    )
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Data Streams from Repository
    val allPools: StateFlow<List<MasterPoolEntity>> = repository.allPools
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allKendaraan: StateFlow<List<MasterKendaraanEntity>> = repository.allKendaraan
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allAudits: StateFlow<List<DataAuditEntity>> = repository.allAudits
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val photoSettings: StateFlow<List<PengaturanNamaPhotoEntity>> = repository.allPhotoSettings
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allPeriode: StateFlow<List<MasterPeriodeEntity>> = repository.allPeriode
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allBobot: StateFlow<List<MasterBobotEntity>> = repository.allBobot
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allSamples: StateFlow<List<MasterSampleEntity>> = repository.allSamples
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allGradeKpi: StateFlow<List<MasterGradeKpiEntity>> = repository.allGradeKpi
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Audit Form Draft Persistence (Auto-saved to prevent data loss on 4GB RAM devices if killed by OS)
    private fun formStateToJson(state: AuditFormState): String {
        return try {
            val obj = org.json.JSONObject()
            state.editingAuditId?.let { obj.put("editingAuditId", it) }
            state.editingAuditTimestamp?.let { obj.put("editingAuditTimestamp", it) }
            state.originalTanggal?.let { obj.put("originalTanggal", it) }
            state.originalAuditorNip?.let { obj.put("originalAuditorNip", it) }
            state.originalAuditorName?.let { obj.put("originalAuditorName", it) }
            obj.put("initialTimestamp", state.initialTimestamp)
            obj.put("kodeUnik", state.kodeUnik)
            obj.put("periode", state.periode)
            obj.put("noMobil", state.noMobil)
            obj.put("platNomer", state.platNomer)
            obj.put("tipe", state.tipe)
            obj.put("warna", state.warna)
            obj.put("tahun", state.tahun)
            obj.put("tanggalService", state.tanggalService)
            obj.put("service", state.service)
            obj.put("pool", state.pool)
            obj.put("isVehicleFound", state.isVehicleFound)
            obj.put("jenisSample", state.jenisSample)
            obj.put("sampleTanggal", state.sampleTanggal)
            obj.put("paketService", state.paketService)

            val kpiScoresObj = org.json.JSONObject()
            state.kpiScores.forEach { (k, v) -> kpiScoresObj.put(k, v) }
            obj.put("kpiScores", kpiScoresObj)

            val kpiPhotosObj = org.json.JSONObject()
            state.kpiPhotos.forEach { (k, v) -> kpiPhotosObj.put(k, v) }
            obj.put("kpiPhotos", kpiPhotosObj)

            val kpiNotesObj = org.json.JSONObject()
            state.kpiNotes.forEach { (k, v) -> kpiNotesObj.put(k, v) }
            obj.put("kpiNotes", kpiNotesObj)

            val photosArr = org.json.JSONArray()
            state.photos.forEach { photosArr.put(it) }
            obj.put("photos", photosArr)

            val notesArr = org.json.JSONArray()
            state.notes.forEach { notesArr.put(it) }
            obj.put("notes", notesArr)

            obj.put("currentStepIndex", state.currentStepIndex)
            obj.toString()
        } catch (_: Exception) {
            ""
        }
    }

    private fun jsonToFormState(json: String): AuditFormState? {
        if (json.isBlank() || json == "{}") return null
        return try {
            val obj = org.json.JSONObject(json)
            val kpiScoresMap = mutableMapOf<String, Int>()
            val sObj = obj.optJSONObject("kpiScores")
            sObj?.keys()?.forEach { k -> kpiScoresMap[k] = sObj.optInt(k) }

            val kpiPhotosMap = mutableMapOf<String, String>()
            val pObj = obj.optJSONObject("kpiPhotos")
            pObj?.keys()?.forEach { k -> kpiPhotosMap[k] = pObj.optString(k) }

            val kpiNotesMap = mutableMapOf<String, String>()
            val nObj = obj.optJSONObject("kpiNotes")
            nObj?.keys()?.forEach { k -> kpiNotesMap[k] = nObj.optString(k) }

            val pList = mutableListOf<String>()
            val pArr = obj.optJSONArray("photos")
            if (pArr != null) {
                for (i in 0 until pArr.length()) pList.add(pArr.optString(i, ""))
            }
            while (pList.size < 20) pList.add("")

            val nList = mutableListOf<String>()
            val nArr = obj.optJSONArray("notes")
            if (nArr != null) {
                for (i in 0 until nArr.length()) nList.add(nArr.optString(i, ""))
            }
            while (nList.size < 20) nList.add("")

            AuditFormState(
                editingAuditId = if (obj.has("editingAuditId")) obj.getLong("editingAuditId") else null,
                editingAuditTimestamp = if (obj.has("editingAuditTimestamp")) obj.getLong("editingAuditTimestamp") else null,
                originalTanggal = if (obj.has("originalTanggal")) obj.getString("originalTanggal") else null,
                originalAuditorNip = if (obj.has("originalAuditorNip")) obj.getString("originalAuditorNip") else null,
                originalAuditorName = if (obj.has("originalAuditorName")) obj.getString("originalAuditorName") else null,
                initialTimestamp = obj.optLong("initialTimestamp", System.currentTimeMillis()),
                kodeUnik = obj.optString("kodeUnik", UniqueCodeGenerator.generateAuditCode()),
                periode = obj.optString("periode", ""),
                noMobil = obj.optString("noMobil", ""),
                platNomer = obj.optString("platNomer", ""),
                tipe = obj.optString("tipe", ""),
                warna = obj.optString("warna", ""),
                tahun = obj.optString("tahun", ""),
                tanggalService = obj.optString("tanggalService", ""),
                service = obj.optString("service", ""),
                pool = obj.optString("pool", ""),
                isVehicleFound = if (obj.has("editingAuditId")) true else obj.optBoolean("isVehicleFound", false),
                jenisSample = obj.optString("jenisSample", ""),
                sampleTanggal = obj.optString("sampleTanggal", ""),
                paketService = obj.optString("paketService", ""),
                kpiScores = if (kpiScoresMap.isNotEmpty()) kpiScoresMap else defaultKpiScores(),
                kpiPhotos = kpiPhotosMap,
                kpiNotes = kpiNotesMap,
                photos = pList,
                notes = nList,
                currentStepIndex = obj.optInt("currentStepIndex", 0)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun loadAuditDraft(): AuditFormState? {
        val draftJson = repository.sessionManager.auditFormDraftJson
        if (draftJson.isBlank()) return null
        return jsonToFormState(draftJson)
    }

    private fun persistFormDraft(state: AuditFormState) {
        try {
            if (state.noMobil.isNotBlank() || state.filledPhotoCount > 0 || state.kpiScores.values.any { it > 0 }) {
                val json = formStateToJson(state)
                if (json.isNotBlank()) {
                    repository.sessionManager.auditFormDraftJson = json
                }
            } else {
                repository.sessionManager.clearAuditFormDraft()
            }
        } catch (_: Exception) {}
    }

    private fun clearFormDraft() {
        try {
            repository.sessionManager.clearAuditFormDraft()
        } catch (_: Exception) {}
    }

    // Audit Form State
    private val _auditForm = MutableStateFlow(loadAuditDraft() ?: AuditFormState())
    val auditForm: StateFlow<AuditFormState> = _auditForm.asStateFlow()

    // Selected Audit for Modal Viewing
    private val _selectedAuditDetail = MutableStateFlow<DataAuditEntity?>(null)
    val selectedAuditDetail: StateFlow<DataAuditEntity?> = _selectedAuditDetail.asStateFlow()

    // Toast/Snackbar events
    private val _eventMessage = MutableSharedFlow<String>()
    val eventMessage = _eventMessage.asSharedFlow()

    // Filter & Search states
    val searchQueryRiwayat = MutableStateFlow("")
    val poolFilterMonitoring = MutableStateFlow("ALL")
    val searchQueryKendaraan = MutableStateFlow("")
    val searchQueryUser = MutableStateFlow("")

    // Web Server PC Realtime
    private val webReportServer = com.example.server.WebReportServer(application)
    private val _isWebServerRunning = MutableStateFlow(false)
    val isWebServerRunning: StateFlow<Boolean> = _isWebServerRunning.asStateFlow()
    private val _webServerUrl = MutableStateFlow(webReportServer.getServerUrl())
    val webServerUrl: StateFlow<String> = _webServerUrl.asStateFlow()

    private val _availableIps = MutableStateFlow<List<com.example.server.IpInterfaceInfo>>(emptyList())
    val availableIps: StateFlow<List<com.example.server.IpInterfaceInfo>> = _availableIps.asStateFlow()

    private val _isEmulatorEnvironment = MutableStateFlow(false)
    val isEmulatorEnvironment: StateFlow<Boolean> = _isEmulatorEnvironment.asStateFlow()

    private val _currentServerPort = MutableStateFlow(8080)
    val currentServerPort: StateFlow<Int> = _currentServerPort.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isReconciling = MutableStateFlow(false)
    val isReconciling: StateFlow<Boolean> = _isReconciling.asStateFlow()

    private val _lastReconcileTime = MutableStateFlow(repository.sessionManager.lastSyncTime)
    val lastReconcileTime: StateFlow<Long> = _lastReconcileTime.asStateFlow()

    private val _lastReconcileSummary = MutableStateFlow(repository.sessionManager.lastSyncSummary)
    val lastReconcileSummary: StateFlow<String> = _lastReconcileSummary.asStateFlow()

    private val _autoSyncEnabled = MutableStateFlow(repository.sessionManager.isAutoSyncEnabled)
    val autoSyncEnabled: StateFlow<Boolean> = _autoSyncEnabled.asStateFlow()

    private val _autoSyncIntervalMinutes = MutableStateFlow(repository.sessionManager.autoSyncIntervalMinutes)
    val autoSyncIntervalMinutes: StateFlow<Int> = _autoSyncIntervalMinutes.asStateFlow()

    private var periodicReconcileJob: Job? = null
    private var userBackgroundSyncJob: Job? = null

    init {
        // Apply user-provided Google Script, Sheet, and Drive Folder settings
        repository.sessionManager.scriptUrl = SessionManager.DEFAULT_SCRIPT_URL
        repository.sessionManager.sheetId = SessionManager.DEFAULT_SHEET_ID
        repository.sessionManager.driveFolderId = SessionManager.DEFAULT_FOLDER_ID

        viewModelScope.launch {
            AppDatabase.seedDatabase(database)
            try {
                database.dataAuditDao().deleteDemoAudits()
                database.masterKendaraanDao().deleteDemoKendaraan()
            } catch (e: Exception) {
                // Ignore
            }
            repository.ensureBobotSeeded()
            repository.ensureSampleSeeded()
            repository.ensureGradeKpiSeeded()
            repository.sessionManager.isDemoDataCleared = true
        }
        refreshWebServerAddress()
        // Web Server dimulai secara manual saat user membuka layar Web Server PC
        // untuk menghemat daya baterai dan memori RAM pada HP RAM 4GB / Android 10.

        // Mengunduh data username dan password SEMUA user di belakang layar secara berkala
        startAllUsersBackgroundSync()
        viewModelScope.launch(Dispatchers.IO) {
            delay(600L)
            if (repository.sessionManager.scriptUrl.isNotBlank()) {
                repository.downloadAllUsersCredentialsFromCloud()
            }
        }

        // Background auto check hanya dimulai jika user sudah login sebelumnya
        if (_currentUser.value != null) {
            startPeriodicReconcile()
        }

        // Amati perubahan form dan simpan draft secara otomatis agar tidak hilang jika aplikasi ditutup oleh sistem saat membuka kamera
        viewModelScope.launch {
            _auditForm.collect { form ->
                persistFormDraft(form)
            }
        }

        // Jika user telah login sebelumnya, langsung sinkronkan dengan database yang di-set
        val savedUser = _currentUser.value
        if (savedUser != null && repository.sessionManager.scriptUrl.isNotBlank()) {
            viewModelScope.launch {
                delay(1200L)
                reconcileDataWithSheets(isBackground = true)
                // MASTER kendaraan secara otomatis download semua data
                syncKendaraan(poolFilter = "")
            }
        }
    }

    /**
     * Mendeteksi kode Pool spesifik dari akun user (berdasarkan username, kodePool, atau profil petugas)
     * Contoh: username "pool a" -> "POOL A", kodePool "POOL-01" -> "POOL-01"
     */
    fun resolveTargetPool(user: UserEntity?): String {
        if (user == null) return ""
        val poolCode = user.kodePool.trim()
        val username = user.username.trim()

        // 1. Jika username spesifik mengandung indikasi pool (misal: "pool a", "pool_a", "pool-01", dll.)
        if (username.contains("pool", ignoreCase = true)) {
            val normalized = username.replace("_", " ").replace("-", " ").trim()
            val matched = allPools.value.firstOrNull { pool ->
                pool.kodePool.equals(normalized, ignoreCase = true) ||
                pool.kodePool.equals(username, ignoreCase = true) ||
                pool.namaPool.contains(normalized, ignoreCase = true)
            }
            if (matched != null) {
                return matched.kodePool.trim().uppercase()
            }
            val parts = normalized.split("\\s+".toRegex())
            if (parts.size >= 2 && parts[0].equals("pool", ignoreCase = true)) {
                return "POOL ${parts.drop(1).joinToString(" ").uppercase()}"
            }
            return normalized.uppercase()
        }

        // 2. Jika kodePool terisi dan bukan nilai umum "ALL" atau "SEMUA"
        if (poolCode.isNotBlank() && !poolCode.equals("ALL", ignoreCase = true) && !poolCode.equals("SEMUA", ignoreCase = true)) {
            return poolCode.uppercase()
        }

        // 3. Cek jika namaPetugas mengandung kata Pool
        val namaPetugas = user.namaPetugas.trim()
        if (namaPetugas.contains("pool", ignoreCase = true)) {
            val parts = namaPetugas.split("\\s+".toRegex())
            val pIdx = parts.indexOfFirst { it.equals("pool", ignoreCase = true) }
            if (pIdx >= 0 && pIdx + 1 < parts.size) {
                return "POOL ${parts[pIdx + 1].uppercase()}"
            }
        }

        return ""
    }

    /**
     * Membandingkan dua kode pool secara toleran terhadap perbedaan format penulisan
     * (misal: "POOL A" dengan "A", "POOL-01" dengan "01", atau "ALL"/"SEMUA")
     */
    fun isPoolMatching(pool1: String, pool2: String): Boolean {
        val p1 = pool1.trim().uppercase().replace("-", " ").replace("_", " ")
        val p2 = pool2.trim().uppercase().replace("-", " ").replace("_", " ")
        if (p1.isEmpty() || p2.isEmpty() || p1 == "ALL" || p2 == "ALL" || p1 == "SEMUA" || p2 == "SEMUA") return true
        if (p1 == p2) return true
        val c1 = p1.removePrefix("POOL").trim()
        val c2 = p2.removePrefix("POOL").trim()
        if (c1.isNotBlank() && c1 == c2) return true
        if (c1.isNotBlank() && (p2 == c1 || p2 == "POOL $c1")) return true
        if (c2.isNotBlank() && (p1 == c2 || p1 == "POOL $c2")) return true
        return false
    }

    /**
     * Memulai background coroutine yang secara otomatis mengecek & mencocokkan data
     * dengan Google Sheets setiap 30 menit (atau sesuai interval yang diatur)
     * serta mengecek timeout ketidakaktifan 12 jam khusus untuk role AUDITOR.
     */
    fun startPeriodicReconcile() {
        periodicReconcileJob?.cancel()
        periodicReconcileJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val isEnabled = repository.sessionManager.isAutoSyncEnabled
                    val scriptUrl = repository.sessionManager.scriptUrl
                    val activeUser = _currentUser.value

                    // Cek masa aktif sesi / batas ketidakaktifan (khususnya 12 jam untuk AUDITOR)
                    if (activeUser != null) {
                        if (!repository.sessionManager.isSessionValid()) {
                            val isAuditor = activeUser.levelAkses.trim().equals("AUDITOR", ignoreCase = true)
                            val reason = if (isAuditor) {
                                "Sesi Auditor telah berakhir otomatis karena tidak ada aktifitas selama 12 jam."
                            } else {
                                "Sesi telah berakhir (melewati batas waktu maksimal). Silakan login kembali."
                            }
                            logout(reason = reason)
                            break
                        }
                    }

                    // HANYA jalankan background reconcile jika user telah login!
                    // Jangan membebani koneksi saat user masih di layar login.
                    if (activeUser != null && isEnabled && scriptUrl.isNotBlank()) {
                        val lastSync = repository.sessionManager.lastSyncTime
                        val now = System.currentTimeMillis()
                        val intervalMs = repository.sessionManager.autoSyncIntervalMinutes * 60 * 1000L
                        // Jika belum pernah sync atau waktu interval (30 menit) telah lewat
                        if (lastSync == 0L || (now - lastSync) >= intervalMs) {
                            reconcileDataWithSheets(isBackground = true)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore exception in background loop
                }
                // Cek status setiap 30 detik agar responsif terhadap perubahan waktu & setting
                delay(30_000L)
            }
        }
    }

    /**
     * Mengunduh data username dan password SEMUA user di belakang layar secara berkala atau on-demand.
     */
    fun downloadAllUsersInBackground(onComplete: ((Boolean, Int) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val scriptUrl = repository.sessionManager.scriptUrl
            if (scriptUrl.isBlank()) {
                onComplete?.invoke(false, 0)
                return@launch
            }
            try {
                val result = repository.downloadAllUsersCredentialsFromCloud(timeoutSeconds = 15L)
                if (result.isSuccess) {
                    val count = result.getOrDefault(0)
                    Log.d("AuditViewModel", "Berhasil mengunduh $count akun SEMUA user di belakang layar")
                    onComplete?.invoke(true, count)
                } else {
                    onComplete?.invoke(false, 0)
                }
            } catch (e: Exception) {
                Log.e("AuditViewModel", "Gagal unduh kredensial semua user di belakang layar: ${e.message}")
                onComplete?.invoke(false, 0)
            }
        }
    }

    /**
     * Kompatibilitas mundur untuk fungsi unduh kredensial auditor di latar belakang.
     */
    fun downloadAuditorCredentialsInBackground(onComplete: ((Boolean, Int) -> Unit)? = null) {
        downloadAllUsersInBackground(onComplete)
    }

    /**
     * Memulai background coroutine yang secara otomatis mengunduh data username dan password
     * SEMUA user di belakang layar setiap 10 menit.
     */
    fun startAllUsersBackgroundSync() {
        userBackgroundSyncJob?.cancel()
        userBackgroundSyncJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    if (repository.sessionManager.scriptUrl.isNotBlank()) {
                        repository.downloadAllUsersCredentialsFromCloud()
                    }
                } catch (_: Exception) {}
                // Cek dan unduh pembaruan akun semua user setiap 10 menit di latar belakang
                delay(10 * 60 * 1000L)
            }
        }
    }

    fun startAuditorBackgroundSync() {
        startAllUsersBackgroundSync()
    }

    fun toggleAutoSync(enabled: Boolean) {
        repository.sessionManager.isAutoSyncEnabled = enabled
        _autoSyncEnabled.value = enabled
        if (enabled) {
            startPeriodicReconcile()
            viewModelScope.launch {
                _eventMessage.emit("Auto Sync tiap 30 menit diaktifkan.")
            }
        } else {
            periodicReconcileJob?.cancel()
            viewModelScope.launch {
                _eventMessage.emit("Auto Sync dinonaktifkan.")
            }
        }
    }

    fun setAutoSyncInterval(minutes: Int) {
        val safe = if (minutes < 5) 5 else minutes
        repository.sessionManager.autoSyncIntervalMinutes = safe
        _autoSyncIntervalMinutes.value = safe
        startPeriodicReconcile()
    }

    /**
     * Melakukan pencocokan data komprehensif dua arah dengan Google Sheets:
     * - Kirim temuan audit lokal yang pending
     * - Tarik & perbarui Master Data (Pool, Kendaraan, User, Bobot, Sample, Periode)
     * - Tarik & cocokkan Data Audit dari Google Sheets
     */
    fun reconcileDataWithSheets(
        isBackground: Boolean = false,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (_isReconciling.value) {
            onComplete(false, "Proses pencocokan sedang berlangsung")
            return
        }

        viewModelScope.launch {
            _isReconciling.value = true
            val result = repository.reconcileWithGoogleSheets()
            _lastReconcileTime.value = result.timestamp
            _lastReconcileSummary.value = repository.sessionManager.lastSyncSummary
            _isReconciling.value = false

            if (result.success) {
                val msg = if (isBackground) {
                    "Pengecekan Otomatis (30 Menit): Data berhasil dicocokkan dengan Google Sheets."
                } else {
                    "Pencocokan selesai! Data audit, master pool, kendaraan, user, bobot, & sample berhasil dicocokkan."
                }
                // Dihilangkan pop up pesan _eventMessage.emit(msg) agar tampilan bersih
                onComplete(true, msg)
            } else {
                val err = result.errorMessage ?: "Gagal mencocokkan data dengan Google Sheets"
                if (!isBackground) {
                    _eventMessage.emit("Pencocokan gagal: $err")
                }
                onComplete(false, err)
            }
        }
    }

    fun updateBobot(itemId: String, bobot: Double, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateBobot(itemId, bobot)
            _eventMessage.emit("Nilai bobot berhasil diperbarui & disinkronkan ke Cloud")
            onComplete()
        }
    }

    fun syncAllBobotToCloud(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isSyncing.value = true
            val res = repository.syncAllMasterBobotToCloud()
            _isSyncing.value = false
            res.onSuccess { count ->
                _eventMessage.emit("Berhasil menyinkronkan $count data Master Bobot ke Google Sheets!")
                onComplete(true, "Berhasil menyinkronkan $count data Master Bobot ke Google Sheets")
            }.onFailure { err ->
                _eventMessage.emit("Gagal sinkron bobot: ${err.message}")
                onComplete(false, err.message ?: "Gagal sinkronisasi")
            }
        }
    }

    fun resetAllBobotToDefault(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            allBobot.value.forEach { item ->
                val defBobot = com.example.data.model.KpiAuditSpec.findItem(item.itemId)?.defaultBobot ?: 1.0
                repository.updateBobot(item.itemId, defBobot)
            }
            _eventMessage.emit("Semua bobot telah di-reset ke nilai standar")
            onComplete()
        }
    }

    fun recordActivity() {
        repository.sessionManager.recordUserActivity()
    }

    fun navigateTo(screen: AppScreen) {
        recordActivity()
        _currentScreen.value = screen
    }

    fun login(username: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            var result = repository.login(username, password)

            val initialError = result.exceptionOrNull()?.message ?: ""
            val isUserNotFoundLocally = result.isFailure && initialError.contains("tidak terdaftar", ignoreCase = true)
            val isWrongPassword = result.isFailure && initialError.contains("Password salah", ignoreCase = true)

            // Jika akun belum ditemukan secara lokal atau password tidak cocok (kemungkinan baru dibuat/diubah di cloud),
            // unduh kredensial SEMUA user di belakang layar dan coba verifikasi ulang
            if ((isUserNotFoundLocally || isWrongPassword) && repository.sessionManager.scriptUrl.isNotBlank()) {
                try {
                    val syncResult = repository.downloadAllUsersCredentialsFromCloud(timeoutSeconds = 12L)
                    if (syncResult.isSuccess && syncResult.getOrDefault(0) > 0) {
                        result = repository.login(username, password)
                    }
                } catch (e: Exception) {
                    // Abaikan exception, lanjutkan ke penanganan error asli
                }
            }

            if (result.isSuccess) {
                val user = result.getOrNull()
                _currentUser.value = user
                when (user?.levelAkses?.trim()?.uppercase()) {
                    "KEPALA BENGKEL" -> _currentScreen.value = AppScreen.RIWAYAT_SERVICE
                    "AUDITOR" -> _currentScreen.value = AppScreen.INPUT_AUDIT
                    "MASTER DATA" -> _currentScreen.value = AppScreen.MASTER_KENDARAAN
                    else -> _currentScreen.value = AppScreen.MONITORING_ALL_POOL
                }
                _eventMessage.emit("Selamat datang, ${user?.namaPetugas}!")
                onComplete(true, null)

                // Aktifkan sinkronisasi berkala di background setelah user berhasil login
                startPeriodicReconcile()

                // LANGSUNG SINGKRON KE DATABASE YANG DI-SET SAAT LOGIN SECARA BACKGROUND
                if (repository.sessionManager.scriptUrl.isNotBlank()) {
                    reconcileDataWithSheets(isBackground = true)
                }

                // MASTER kendaraan secara otomatis download semua data
                if (user != null && repository.sessionManager.scriptUrl.isNotBlank()) {
                    viewModelScope.launch {
                        delay(600L) // Jeda singkat agar inisialisasi navigasi stabil
                        syncKendaraan(poolFilter = "")
                    }
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Login gagal"
                _eventMessage.emit(errorMsg)
                onComplete(false, errorMsg)
            }
        }
    }

    fun login(username: String, password: String, onComplete: (Boolean) -> Unit) {
        login(username, password) { success, _ -> onComplete(success) }
    }

    fun logout(reason: String? = null) {
        periodicReconcileJob?.cancel()
        clearFormDraft()
        repository.sessionManager.clearSession()
        _currentUser.value = null
        _auditForm.value = AuditFormState()
        searchQueryRiwayat.value = ""
        poolFilterMonitoring.value = "ALL"
        searchQueryKendaraan.value = ""
        searchQueryUser.value = ""
        _selectedAuditDetail.value = null
        _currentScreen.value = AppScreen.LOGIN
        viewModelScope.launch {
            _eventMessage.emit(reason ?: "Sesi telah keluar. Semua form input telah di-reset.")
        }
    }

    fun resetAuditForm() {
        _auditForm.value = AuditFormState()
    }

    fun regenerateAuditCode() {
        if (!_auditForm.value.isEditMode) {
            _auditForm.value = _auditForm.value.copy(kodeUnik = UniqueCodeGenerator.generateAuditCode())
        }
    }

    // Audit Form Actions
    fun onNoMobilChanged(noMobil: String) {
        if (_auditForm.value.isEditMode) {
            // Pada mode Edit Data Audit, nomor mobil dan data kendaraan langsung terkunci
            return
        }
        val upperNoMobil = noMobil.uppercase()
        // Plat Nomer otomatis mengikuti NO Mobil secara real-time
        _auditForm.value = _auditForm.value.copy(
            noMobil = upperNoMobil,
            platNomer = upperNoMobil
        )

        viewModelScope.launch {
            val trimmed = upperNoMobil.trim()
            val vehicle = if (trimmed.isNotBlank()) repository.getKendaraanByNoMobil(trimmed) else null
            val user = _currentUser.value

            // Only update vehicle fields if the input hasn't changed in the meantime
            if (_auditForm.value.noMobil == upperNoMobil) {
                if (vehicle != null) {
                    _auditForm.value = _auditForm.value.copy(
                        platNomer = upperNoMobil, // Plat Nomer mengikuti NO Mobil
                        tipe = if (_auditForm.value.tipe.isBlank()) vehicle.tipe else _auditForm.value.tipe,
                        warna = if (_auditForm.value.warna.isBlank()) vehicle.warna else _auditForm.value.warna,
                        tahun = if (_auditForm.value.tahun.isBlank()) vehicle.tahun else _auditForm.value.tahun,
                        tanggalService = vehicle.tanggalService,
                        service = vehicle.service,
                        pool = if (_auditForm.value.pool.isBlank()) vehicle.pool else _auditForm.value.pool,
                        isVehicleFound = true,
                        validationError = null
                    )
                } else {
                    if (trimmed.isBlank()) {
                        _auditForm.value = _auditForm.value.copy(
                            platNomer = "",
                            tanggalService = "",
                            service = "",
                            isVehicleFound = false,
                            validationError = null
                        )
                    } else {
                        // Jika belum ada di Master Kendaraan, plat nomer tetap mengikuti No Mobil
                        _auditForm.value = _auditForm.value.copy(
                            platNomer = upperNoMobil,
                            pool = if (_auditForm.value.pool.isBlank()) user?.kodePool ?: "SKT" else _auditForm.value.pool,
                            isVehicleFound = true,
                            validationError = null
                        )
                    }
                }
            }
        }
    }

    fun onPeriodeChanged(periode: String) {
        _auditForm.value = _auditForm.value.copy(periode = periode)
    }

    fun onPlatNomerChanged(plat: String) {
        _auditForm.value = _auditForm.value.copy(platNomer = plat.uppercase())
    }

    fun onTipeChanged(tipe: String) {
        _auditForm.value = _auditForm.value.copy(tipe = tipe)
    }

    fun onWarnaChanged(warna: String) {
        _auditForm.value = _auditForm.value.copy(warna = warna)
    }

    fun onTahunChanged(tahun: String) {
        _auditForm.value = _auditForm.value.copy(tahun = tahun)
    }

    fun onTanggalServiceChanged(tanggal: String) {
        _auditForm.value = _auditForm.value.copy(tanggalService = tanggal)
    }

    fun onServiceChanged(service: String) {
        _auditForm.value = _auditForm.value.copy(service = service)
    }

    fun onPoolChanged(pool: String) {
        _auditForm.value = _auditForm.value.copy(pool = pool)
    }

    fun onJenisSampleChanged(jenis: String) {
        val isOperasional = jenis.equals("OPERASIONAL", ignoreCase = true)
        val hiddenItemIds = setOf(
            "pelumas_engsel", "kondisi_ruang_mesin", "kondisi_kap_mesin_dalam", "kondisi_tutup_bbm",
            "cuc_ruang_mesin", "cuc_kebersihan_engsel"
        )
        val updatedScores = _auditForm.value.kpiScores.toMutableMap()
        val updatedPhotos = _auditForm.value.kpiPhotos.toMutableMap()
        val updatedNotes = _auditForm.value.kpiNotes.toMutableMap()

        if (isOperasional) {
            hiddenItemIds.forEach { itId ->
                updatedScores[itId] = 0
                val pKeys = updatedPhotos.keys.filter { it == itId || it.startsWith("${itId}_") }
                pKeys.forEach { updatedPhotos.remove(it) }
                val nKeys = updatedNotes.keys.filter { it == itId || it.startsWith("${itId}_") }
                nKeys.forEach { updatedNotes.remove(it) }
            }
            _auditForm.value = _auditForm.value.copy(
                jenisSample = jenis,
                sampleTanggal = "",
                paketService = "",
                kpiScores = updatedScores,
                kpiPhotos = updatedPhotos,
                kpiNotes = updatedNotes
            )
        } else {
            _auditForm.value = _auditForm.value.copy(jenisSample = jenis)
        }
    }

    fun onSampleTanggalChanged(tanggal: String) {
        _auditForm.value = _auditForm.value.copy(sampleTanggal = tanggal)
    }

    fun onPaketServiceChanged(paket: String) {
        _auditForm.value = _auditForm.value.copy(paketService = paket.uppercase())
    }

    fun onKpiScoreChanged(itemId: String, score: Int) {
        val currentScores = _auditForm.value.kpiScores.toMutableMap()
        currentScores[itemId] = score
        val currentPhotos = _auditForm.value.kpiPhotos.toMutableMap()
        val currentNotes = _auditForm.value.kpiNotes.toMutableMap()

        if (score == 0) {
            // Jika point pada input dinolkan, hapus semua data foto dan catatan untuk item tersebut
            val pKeys = currentPhotos.keys.filter { it == itemId || it.startsWith("${itemId}_") }
            pKeys.forEach { currentPhotos.remove(it) }
            val nKeys = currentNotes.keys.filter { it == itemId || it.startsWith("${itemId}_") }
            nKeys.forEach { currentNotes.remove(it) }
        } else {
            // Jika skor diturunkan, hapus slot foto dan catatan yang melebihi skor baru
            val pKeys = currentPhotos.keys.filter { key ->
                if (key.startsWith("${itemId}_")) {
                    val idx = key.removePrefix("${itemId}_").toIntOrNull()
                    idx != null && idx >= score
                } else false
            }
            pKeys.forEach { currentPhotos.remove(it) }
            val nKeys = currentNotes.keys.filter { key ->
                if (key.startsWith("${itemId}_")) {
                    val idx = key.removePrefix("${itemId}_").toIntOrNull()
                    idx != null && idx >= score
                } else false
            }
            nKeys.forEach { currentNotes.remove(it) }
        }

        _auditForm.value = _auditForm.value.copy(
            kpiScores = currentScores,
            kpiPhotos = currentPhotos,
            kpiNotes = currentNotes
        )
    }

    fun onKpiPhotoCaptured(photoKey: String, localPath: String) {
        val currentPhotos = _auditForm.value.kpiPhotos.toMutableMap()
        currentPhotos[photoKey] = localPath
        _auditForm.value = _auditForm.value.copy(kpiPhotos = currentPhotos)
    }

    fun onKpiRemovePhoto(photoKey: String) {
        val currentPhotos = _auditForm.value.kpiPhotos.toMutableMap()
        val currentNotes = _auditForm.value.kpiNotes.toMutableMap()

        val lastUnderscore = photoKey.lastIndexOf('_')
        val isKpiSlot = lastUnderscore > 0 &&
                !photoKey.startsWith("kendaraan_") &&
                photoKey.substring(lastUnderscore + 1).toIntOrNull() != null

        if (isKpiSlot) {
            val itemId = photoKey.substring(0, lastUnderscore)
            val targetSlot = photoKey.substring(lastUnderscore + 1).toInt()

            // Cari index slot tertinggi yang tercatat untuk item ini
            val relatedKeys = currentPhotos.keys.filter { it.startsWith("${itemId}_") } +
                              currentNotes.keys.filter { it.startsWith("${itemId}_") }
            val maxSlot = (relatedKeys.mapNotNull { it.substringAfterLast('_').toIntOrNull() } + listOf(targetSlot + 1)).maxOrNull() ?: targetSlot

            // Geser foto dan catatan dari slot (i + 1) ke slot i
            // Contoh: foto pada point 1, 2, 3 -> hapus photo 2 -> point 3 otomatis pindah ke point 2
            for (i in targetSlot until maxSlot) {
                val nextKey = "${itemId}_${i + 1}"
                val currKey = "${itemId}_$i"

                val nextPhoto = currentPhotos[nextKey]
                if (nextPhoto != null) {
                    currentPhotos[currKey] = nextPhoto
                } else {
                    currentPhotos.remove(currKey)
                }

                val nextNote = currentNotes[nextKey]
                if (nextNote != null) {
                    currentNotes[currKey] = nextNote
                } else {
                    currentNotes.remove(currKey)
                }
            }

            // Bersihkan slot paling akhir
            currentPhotos.remove("${itemId}_$maxSlot")
            currentNotes.remove("${itemId}_$maxSlot")
        } else {
            currentPhotos.remove(photoKey)
            currentNotes.remove(photoKey)
        }

        val legacyP = _auditForm.value.photos.toMutableList()
        val legacyN = _auditForm.value.notes.toMutableList()
        val vehicleKeys = listOf(
            "kendaraan_depan",
            "kendaraan_samping_kiri",
            "kendaraan_interior_depan_kiri",
            "kendaraan_interior_belakang_kiri",
            "kendaraan_belakang",
            "kendaraan_interior_belakang",
            "kendaraan_samping_kanan"
        )
        val vIdx = vehicleKeys.indexOf(photoKey)
        if (vIdx in 0..6 && vIdx < legacyP.size) {
            legacyP[vIdx] = ""
            if (vIdx < legacyN.size) legacyN[vIdx] = ""
        } else {
            val oldPath = _auditForm.value.kpiPhotos[photoKey]
            if (!oldPath.isNullOrBlank()) {
                val fIdx = legacyP.indexOf(oldPath)
                if (fIdx >= 0) {
                    legacyP[fIdx] = ""
                    if (fIdx < legacyN.size) legacyN[fIdx] = ""
                }
            }
        }

        _auditForm.value = _auditForm.value.copy(
            kpiPhotos = currentPhotos,
            kpiNotes = currentNotes,
            photos = legacyP,
            notes = legacyN
        )

        if (isKpiSlot) {
            viewModelScope.launch {
                _eventMessage.emit("Foto point berhasil dihapus. Keterangan dan foto berikutnya otomatis bergeser.")
            }
        }
    }

    fun onKpiNoteChanged(photoKey: String, note: String) {
        val currentNotes = _auditForm.value.kpiNotes.toMutableMap()
        currentNotes[photoKey] = note.uppercase()
        _auditForm.value = _auditForm.value.copy(kpiNotes = currentNotes)
    }

    fun onResetAllNormal() {
        val allItems = com.example.data.model.KpiAuditSpec.getAllItems()
        val resetScores = allItems.associate { it.id to 0 }
        // Pertahankan foto kendaraan (DEPAN, SAMPING KIRI, BELAKANG, SAMPING KANAN)
        val vehiclePhotos = _auditForm.value.kpiPhotos.filterKeys { it.startsWith("kendaraan_") }
        _auditForm.value = _auditForm.value.copy(
            kpiScores = resetScores,
            kpiPhotos = vehiclePhotos,
            kpiNotes = emptyMap()
        )
        viewModelScope.launch {
            _eventMessage.emit("Semua item KPI disetel Normal (0 Poin)")
        }
    }

    fun onPhotoCaptured(slotIndex: Int, localPath: String) {
        val updatedPhotos = _auditForm.value.photos.toMutableList()
        if (slotIndex in 0 until 20) {
            updatedPhotos[slotIndex] = localPath
            // Move to next unlocked step if available
            val nextStep = if (slotIndex + 1 < 20) slotIndex + 1 else slotIndex
            _auditForm.value = _auditForm.value.copy(
                photos = updatedPhotos,
                currentStepIndex = nextStep
            )
        }
    }

    fun onRemovePhoto(slotIndex: Int) {
        val updatedPhotos = _auditForm.value.photos.toMutableList()
        if (slotIndex in 0 until 20) {
            updatedPhotos[slotIndex] = ""
            _auditForm.value = _auditForm.value.copy(
                photos = updatedPhotos,
                currentStepIndex = slotIndex
            )
        }
    }

    fun onNoteChanged(slotIndex: Int, note: String) {
        val updatedNotes = _auditForm.value.notes.toMutableList()
        if (slotIndex in 0 until 20) {
            updatedNotes[slotIndex] = note.uppercase()
            _auditForm.value = _auditForm.value.copy(notes = updatedNotes)
        }
    }

    fun canEditAudit(audit: DataAuditEntity, user: UserEntity? = _currentUser.value): Boolean {
        if (user == null) return false
        val isSuperAdmin = user.levelAkses.equals("SUPER ADMIN", ignoreCase = true)
        if (isSuperAdmin) {
            return true
        }
        val isCreator = (audit.nip.isNotBlank() && audit.nip.trim().equals(user.nip.trim(), ignoreCase = true)) ||
                audit.namaPetugas.trim().equals(user.namaPetugas.trim(), ignoreCase = true) ||
                audit.namaPetugas.trim().equals(user.username.trim(), ignoreCase = true)
        if (!isCreator) {
            return false
        }
        val elapsedMs = System.currentTimeMillis() - audit.timestamp
        val isAuditor = user.levelAkses.trim().equals("AUDITOR", ignoreCase = true)
        val maxEditHoursMs = if (isAuditor) 6L * 60L * 60L * 1000L else 12L * 60L * 60L * 1000L
        return elapsedMs <= maxEditHoursMs
    }

    fun getEditRemainingText(audit: DataAuditEntity, user: UserEntity? = _currentUser.value): String {
        if (user == null) return ""
        val isSuperAdmin = user.levelAkses.equals("SUPER ADMIN", ignoreCase = true)
        if (isSuperAdmin) {
            return "Akses Super Admin (Bebas Edit Selamanya)"
        }
        val isCreator = (audit.nip.isNotBlank() && audit.nip.trim().equals(user.nip.trim(), ignoreCase = true)) ||
                audit.namaPetugas.trim().equals(user.namaPetugas.trim(), ignoreCase = true) ||
                audit.namaPetugas.trim().equals(user.username.trim(), ignoreCase = true)
        if (!isCreator) {
            return "Hanya dapat diedit oleh auditor penginput atau Super Admin"
        }
        val elapsedMs = System.currentTimeMillis() - audit.timestamp
        val isAuditor = user.levelAkses.trim().equals("AUDITOR", ignoreCase = true)
        val limitHours = if (isAuditor) 6L else 12L
        val maxEditHoursMs = limitHours * 60L * 60L * 1000L
        val remainingMs = maxEditHoursMs - elapsedMs
        return if (remainingMs > 0) {
            val hours = remainingMs / (1000 * 60 * 60)
            val minutes = (remainingMs % (1000 * 60 * 60)) / (1000 * 60)
            "Dapat Diedit (Sisa ${hours}j ${minutes}m)"
        } else {
            "Batas Edit ${limitHours} Jam Telah Berakhir"
        }
    }

    private var screenBeforeEdit: AppScreen = AppScreen.RIWAYAT_SERVICE

    fun startEditAudit(audit: DataAuditEntity) {
        val user = _currentUser.value
        if (user == null) {
            viewModelScope.launch {
                _eventMessage.emit("Harap login terlebih dahulu untuk mengedit audit.")
            }
            return
        }

        val isSuperAdmin = user.levelAkses.equals("SUPER ADMIN", ignoreCase = true)
        val isCreator = (audit.nip.isNotBlank() && audit.nip.trim().equals(user.nip.trim(), ignoreCase = true)) ||
                audit.namaPetugas.trim().equals(user.namaPetugas.trim(), ignoreCase = true) ||
                audit.namaPetugas.trim().equals(user.username.trim(), ignoreCase = true)
        val isAuditor = user.levelAkses.trim().equals("AUDITOR", ignoreCase = true)
        val isAuditorSamePool = isAuditor && audit.pool.isNotBlank() && audit.pool.trim().equals(user.kodePool.trim(), ignoreCase = true)

        if (!isSuperAdmin && !isCreator && !isAuditorSamePool) {
            viewModelScope.launch {
                _eventMessage.emit("Menu edit hanya berfungsi untuk auditor pembuat audit ini, auditor pool terkait, atau Super Admin.")
            }
            return
        }

        if (!isSuperAdmin) {
            val elapsedMs = System.currentTimeMillis() - audit.timestamp
            val limitHours = if (isAuditor) 6L else 12L
            val maxEditHoursMs = limitHours * 60L * 60L * 1000L
            if (elapsedMs > maxEditHoursMs) {
                viewModelScope.launch {
                    _eventMessage.emit("Batas waktu edit $limitHours jam untuk audit ini telah berakhir. Hubungi Super Admin.")
                }
                return
            }
        }

        screenBeforeEdit = _currentScreen.value
        
        val baseScores = defaultKpiScores().toMutableMap()
        baseScores.putAll(jsonToMapInt(audit.kpiScoresJson))
        val scores = baseScores
        val photosMap = jsonToMapString(audit.kpiPhotosJson).toMutableMap()
        val notesMap = jsonToMapString(audit.kpiNotesJson).toMutableMap()

        val legacyPhotos = listOf(
            audit.photo1, audit.photo2, audit.photo3, audit.photo4, audit.photo5,
            audit.photo6, audit.photo7, audit.photo8, audit.photo9, audit.photo10,
            audit.photo11, audit.photo12, audit.photo13, audit.photo14, audit.photo15,
            audit.photo16, audit.photo17, audit.photo18, audit.photo19, audit.photo20
        )
        val legacyNotes = listOf(
            audit.catatanPhoto1, audit.catatanPhoto2, audit.catatanPhoto3, audit.catatanPhoto4, audit.catatanPhoto5,
            audit.catatanPhoto6, audit.catatanPhoto7, audit.catatanPhoto8, audit.catatanPhoto9, audit.catatanPhoto10,
            audit.catatanPhoto11, audit.catatanPhoto12, audit.catatanPhoto13, audit.catatanPhoto14, audit.catatanPhoto15,
            audit.catatanPhoto16, audit.catatanPhoto17, audit.catatanPhoto18, audit.catatanPhoto19, audit.catatanPhoto20
        )

        // Pastikan 7 foto kendaraan wajib terisi kembali jika tersimpan di legacy photos
        val vehicleKeys = listOf(
            "kendaraan_depan",
            "kendaraan_samping_kiri",
            "kendaraan_interior_depan_kiri",
            "kendaraan_interior_belakang_kiri",
            "kendaraan_belakang",
            "kendaraan_interior_belakang",
            "kendaraan_samping_kanan"
        )
        vehicleKeys.forEachIndexed { idx, vKey ->
            if (photosMap[vKey].isNullOrBlank()) {
                val p = legacyPhotos.getOrNull(idx).orEmpty()
                if (p.isNotBlank()) {
                    photosMap[vKey] = p
                    val n = legacyNotes.getOrNull(idx).orEmpty()
                    if (n.isNotBlank()) notesMap[vKey] = n
                }
            }
        }

        // Pastikan foto temuan legacy terpetakan ke photosMap jika belum ada di kpiPhotos
        val allItems = com.example.data.model.KpiAuditSpec.getAllItems()
        val hasAnyFindingInMap = photosMap.keys.any { !it.startsWith("kendaraan_") }
        if (!hasAnyFindingInMap) {
            var fIdx = 7
            for (itm in allItems) {
                val itemScore = scores[itm.id] ?: 0
                if (itemScore > 0) {
                    for (pIdx in 0 until itemScore) {
                        val key = "${itm.id}_$pIdx"
                        val p = legacyPhotos.getOrNull(fIdx).orEmpty()
                        if (p.isNotBlank()) {
                            photosMap[key] = p
                            val n = legacyNotes.getOrNull(fIdx).orEmpty()
                            if (n.isNotBlank()) notesMap[key] = n
                        }
                        fIdx++
                    }
                }
            }
            if (fIdx == 7) {
                legacyPhotos.forEachIndexed { idx, p ->
                    if (p.isNotBlank() && idx >= 7 && (idx - 7) < allItems.size) {
                        val itm = allItems[idx - 7]
                        if ((scores[itm.id] ?: 0) == 0) {
                            scores[itm.id] = 1
                        }
                        photosMap["${itm.id}_0"] = p
                        notesMap["${itm.id}_0"] = legacyNotes.getOrNull(idx).orEmpty()
                    }
                }
            }
        }

        // Pastikan item dengan point 0 tidak memiliki sisa foto atau catatan temuan
        for ((itemId, pt) in scores) {
            if (pt == 0) {
                val pKeys = photosMap.keys.filter { it == itemId || it.startsWith("${itemId}_") }
                pKeys.forEach { photosMap.remove(it) }
                val nKeys = notesMap.keys.filter { it == itemId || it.startsWith("${itemId}_") }
                nKeys.forEach { notesMap.remove(it) }
            }
        }

        val effectiveKode = if (audit.kodeUnik.isNotBlank()) {
            audit.kodeUnik
        } else if (audit.auditUuid.isNotBlank() && audit.auditUuid.startsWith("AUD-")) {
            audit.auditUuid
        } else {
            UniqueCodeGenerator.generateAuditCode()
        }

        val auditTimestamp = if (audit.timestamp > 0L) {
            audit.timestamp
        } else {
            DateUtils.parseToMillis(audit.tanggal, "") ?: System.currentTimeMillis()
        }

        _auditForm.value = AuditFormState(
            editingAuditId = audit.id,
            editingAuditTimestamp = auditTimestamp,
            originalTanggal = audit.tanggal,
            originalAuditorNip = audit.nip,
            originalAuditorName = audit.namaPetugas,
            initialTimestamp = auditTimestamp, // Mengikuti data WAKTU/TIMESTAMP yang tersimpan di Google Sheets
            kodeUnik = effectiveKode,
            periode = audit.periode,
            noMobil = audit.noMobil,
            platNomer = audit.platNomer,
            tipe = audit.tipe,
            warna = audit.warna,
            tahun = audit.tahun,
            tanggalService = audit.tanggalService,
            service = audit.service,
            pool = audit.pool,
            isVehicleFound = true,
            jenisSample = audit.jenisSample.ifBlank { "OPERASIONAL" },
            sampleTanggal = audit.sampleTanggal.ifBlank { audit.tanggalService },
            paketService = audit.paketService.ifBlank { audit.service },
            kpiScores = scores,
            kpiPhotos = photosMap,
            kpiNotes = notesMap,
            photos = legacyPhotos,
            notes = legacyNotes,
            currentStepIndex = 0
        )
        _selectedAuditDetail.value = null
        _currentScreen.value = AppScreen.INPUT_AUDIT
        viewModelScope.launch {
            // Pada mode Edit Data Audit: Data kendaraan dan nomor mobil terkunci mengikuti data Google Sheets
            val v = repository.getKendaraanByNoMobil(audit.noMobil.trim())
            val currentForm = _auditForm.value
            _auditForm.value = currentForm.copy(
                isVehicleFound = true,
                validationError = null,
                noMobil = audit.noMobil, // Terkunci mengikuti data Google Sheets
                platNomer = currentForm.platNomer.ifBlank { v?.platNomer.orEmpty() },
                tipe = currentForm.tipe.ifBlank { v?.tipe.orEmpty() },
                warna = currentForm.warna.ifBlank { v?.warna.orEmpty() },
                tahun = currentForm.tahun.ifBlank { v?.tahun.orEmpty() },
                pool = currentForm.pool.ifBlank { v?.pool.orEmpty() }
            )
            _eventMessage.emit("Mode Edit: No Mobil #${audit.noMobil}")
        }
    }

    fun cancelEditAudit() {
        clearFormDraft()
        _auditForm.value = AuditFormState()
        val targetScreen = if (screenBeforeEdit == AppScreen.INPUT_AUDIT) AppScreen.RIWAYAT_SERVICE else screenBeforeEdit
        _currentScreen.value = targetScreen
        viewModelScope.launch {
            _eventMessage.emit("Edit audit dibatalkan. Kembali ke riwayat audit.")
        }
    }

    private fun mapToJson(map: Map<String, Any>): String {
        if (map.isEmpty()) return "{}"
        val json = org.json.JSONObject()
        map.forEach { (k, v) -> json.put(k, v) }
        return json.toString()
    }

    private fun jsonToMapInt(json: String?): Map<String, Int> {
        if (json.isNullOrBlank() || json == "{}") return emptyMap()
        val map = mutableMapOf<String, Int>()
        try {
            val obj = org.json.JSONObject(json)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = obj.optInt(key)
            }
        } catch (e: Exception) {
            // ignore
        }
        return map
    }

    private fun jsonToMapString(json: String?): Map<String, String> {
        if (json.isNullOrBlank() || json == "{}") return emptyMap()
        val map = mutableMapOf<String, String>()
        try {
            val obj = org.json.JSONObject(json)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = obj.optString(key)
            }
        } catch (e: Exception) {
            // ignore
        }
        return map
    }

    fun submitAudit(onSuccess: () -> Unit) {
        val form = _auditForm.value
        val user = _currentUser.value ?: return

        if (form.noMobil.isBlank()) {
            viewModelScope.launch { _eventMessage.emit("Harap masukkan No Mobil!") }
            return
        }

        if (form.periode.isBlank()) {
            viewModelScope.launch { _eventMessage.emit("Harap isi atau pilih Periode audit!") }
            return
        }

        if (form.jenisSample.isBlank()) {
            viewModelScope.launch { _eventMessage.emit("Harap pilih Jenis Sample (Wajib)!") }
            return
        }

        if (form.jenisSample.equals("SERVICE", ignoreCase = true)) {
            if (form.sampleTanggal.isBlank()) {
                viewModelScope.launch { _eventMessage.emit("Harap pilih Tanggal Service (Wajib untuk jenis Service)!") }
                return
            }
            if (form.paketService.isBlank()) {
                viewModelScope.launch { _eventMessage.emit("Harap isi Paket Service (Wajib untuk jenis Service)!") }
                return
            }
        }

        val isOperasional = form.jenisSample.equals("OPERASIONAL", ignoreCase = true)
        val hiddenItemIds = if (isOperasional) {
            setOf("pelumas_engsel", "kondisi_ruang_mesin", "kondisi_kap_mesin_dalam", "kondisi_tutup_bbm", "cuc_ruang_mesin", "cuc_kebersihan_engsel")
        } else emptySet()

        val allItems = com.example.data.model.KpiAuditSpec.getAllItems()
        val completeScores = allItems.associate { it.id to if (hiddenItemIds.contains(it.id)) 0 else (form.kpiScores[it.id] ?: 0) }

        val requiredVehicleSlots = listOf(
            "kendaraan_depan" to "DEPAN",
            "kendaraan_samping_kiri" to "SAMPING KIRI",
            "kendaraan_interior_depan_kiri" to "INTERIOR DEPAN KIRI",
            "kendaraan_interior_belakang_kiri" to "INTERIOR BELAKANG KIRI",
            "kendaraan_belakang" to "BELAKANG",
            "kendaraan_interior_belakang" to "INTERIOR BELAKANG",
            "kendaraan_samping_kanan" to "SAMPING KANAN"
        )
        val missingVehicle = requiredVehicleSlots.filter { form.kpiPhotos[it.first].isNullOrBlank() }
        if (missingVehicle.isNotEmpty()) {
            val missingNames = missingVehicle.joinToString(", ") { it.second }
            viewModelScope.launch {
                _eventMessage.emit("Form Foto Kendaraan wajib diisi lengkap! Kurang foto: $missingNames")
            }
            return
        }

        viewModelScope.launch {
            // Verifikasi No Mobil terhadap Master Kendaraan secara real-time
            val trimmedNoMobil = form.noMobil.trim()
            val vehicle = repository.getKendaraanByNoMobil(trimmedNoMobil)

            if (vehicle != null) {
                // Kendaraan terdaftar di Master Kendaraan
                _auditForm.value = _auditForm.value.copy(
                    platNomer = if (form.platNomer.isNotBlank()) form.platNomer else form.noMobil,
                    tipe = if (form.tipe.isNotBlank()) form.tipe else vehicle.tipe,
                    warna = if (form.warna.isNotBlank()) form.warna else vehicle.warna,
                    tahun = if (form.tahun.isNotBlank()) form.tahun else vehicle.tahun,
                    pool = if (form.pool.isNotBlank()) form.pool else vehicle.pool,
                    isVehicleFound = true,
                    validationError = null
                )
            } else {
                // Kendaraan input mandiri
                _auditForm.value = _auditForm.value.copy(
                    platNomer = if (form.platNomer.isNotBlank()) form.platNomer else form.noMobil,
                    pool = if (form.pool.isNotBlank()) form.pool else user.kodePool,
                    isVehicleFound = true,
                    validationError = null
                )
            }

            _auditForm.value = _auditForm.value.copy(isSubmitting = true)

            // Build flattened photos (up to 20) for legacy views & sheets
            val flatPhotos = MutableList(20) { "" }
            val flatNotes = MutableList(20) { "" }

            // 1. Slot 1 sampai 7 (indeks 0..6) DIALOKASIKAN KHUSUS UNTUK 7 FOTO KENDARAAN WAJIB
            val vehiclePhotoSlots = listOf(
                "kendaraan_depan" to "Foto Kendaraan - Depan",
                "kendaraan_samping_kiri" to "Foto Kendaraan - Samping Kiri",
                "kendaraan_interior_depan_kiri" to "Foto Kendaraan - Interior Depan Kiri",
                "kendaraan_interior_belakang_kiri" to "Foto Kendaraan - Interior Belakang Kiri",
                "kendaraan_belakang" to "Foto Kendaraan - Belakang",
                "kendaraan_interior_belakang" to "Foto Kendaraan - Interior Belakang",
                "kendaraan_samping_kanan" to "Foto Kendaraan - Samping Kanan"
            )

            vehiclePhotoSlots.forEachIndexed { vIdx, (vKey, vTitle) ->
                val vPath = form.kpiPhotos[vKey].orEmpty()
                if (vPath.isNotBlank()) {
                    flatPhotos[vIdx] = vPath
                    val userNote = form.kpiNotes[vKey].orEmpty().trim()
                    flatNotes[vIdx] = if (userNote.isNotBlank()) userNote else vTitle
                } else {
                    flatPhotos[vIdx] = ""
                    flatNotes[vIdx] = ""
                }
            }

            // 2. Bersihkan foto dan catatan temuan jika poin item dinolkan (0)
            val cleanKpiPhotos = form.kpiPhotos.toMutableMap()
            val cleanKpiNotes = form.kpiNotes.toMutableMap()
            for (item in allItems) {
                val score = completeScores[item.id] ?: 0
                if (score == 0) {
                    val pKeys = cleanKpiPhotos.keys.filter { it == item.id || it.startsWith("${item.id}_") }
                    pKeys.forEach { cleanKpiPhotos.remove(it) }
                    val nKeys = cleanKpiNotes.keys.filter { it == item.id || it.startsWith("${item.id}_") }
                    nKeys.forEach { cleanKpiNotes.remove(it) }
                } else {
                    val pKeys = cleanKpiPhotos.keys.filter { key ->
                        if (key.startsWith("${item.id}_")) {
                            val idx = key.removePrefix("${item.id}_").toIntOrNull()
                            idx != null && idx >= score
                        } else false
                    }
                    pKeys.forEach { cleanKpiPhotos.remove(it) }
                    val nKeys = cleanKpiNotes.keys.filter { key ->
                        if (key.startsWith("${item.id}_")) {
                            val idx = key.removePrefix("${item.id}_").toIntOrNull()
                            idx != null && idx >= score
                        } else false
                    }
                    nKeys.forEach { cleanKpiNotes.remove(it) }
                }
            }

            // Slot 8 sampai 20 (indeks 7..19) untuk foto temuan penilaian KPI (hanya jika skor > 0)
            var flatFindingIndex = 7
            for (item in allItems) {
                val score = completeScores[item.id] ?: 0
                if (score > 0) {
                    for (pIdx in 0 until score) {
                        val key = "${item.id}_$pIdx"
                        val path = cleanKpiPhotos[key]
                        if (!path.isNullOrBlank() && flatFindingIndex < 20) {
                            flatPhotos[flatFindingIndex] = path
                            val userNote = cleanKpiNotes[key].orEmpty().trim()
                            val note = if (userNote.isNotBlank()) userNote else item.getLabelForPhoto(pIdx)
                            flatNotes[flatFindingIndex] = note
                            flatFindingIndex++
                        }
                    }
                }
            }

            // Pastikan sisa slot temuan yang tidak ada foto dikosongkan
            for (i in flatFindingIndex until 20) {
                flatPhotos[i] = ""
                flatNotes[i] = ""
            }

            val calculatedTotalPoin = completeScores.values.sum()
            val scoresJson = mapToJson(completeScores)
            val photosJson = mapToJson(cleanKpiPhotos)
            val notesJson = mapToJson(cleanKpiNotes)

            val isService = form.jenisSample.equals("SERVICE", ignoreCase = true)
            val finalTanggalService = if (isService) {
                if (form.sampleTanggal.isNotBlank()) form.sampleTanggal else form.tanggalService.ifBlank { DateUtils.getCurrentDateFormatted() }
            } else ""
            val finalSampleTanggal = finalTanggalService
            val finalPaketService = if (isService) {
                if (form.paketService.isNotBlank()) form.paketService else form.service
            } else ""
            val finalService = finalPaketService

            if (form.isEditMode && form.editingAuditId != null) {
                val existingAudit = repository.getAuditById(form.editingAuditId)
                // WAKTU/TIMESTAMP mengikuti data yang tersimpan di Google Sheets
                val sheetTimestamp = if (form.editingAuditTimestamp != null && form.editingAuditTimestamp > 0L) {
                    form.editingAuditTimestamp
                } else if (existingAudit != null && existingAudit.timestamp > 0L) {
                    existingAudit.timestamp
                } else {
                    form.initialTimestamp
                }

                // TANGGAL mengikuti data Google Sheets
                val rawTanggal = form.originalTanggal?.ifBlank { null } ?: existingAudit?.tanggal
                val sheetTanggal = if (!rawTanggal.isNullOrBlank()) {
                    val formatted = DateUtils.formatDisplayDate(rawTanggal)
                    if (formatted != "-") formatted else rawTanggal
                } else {
                    DateUtils.formatDate(sheetTimestamp)
                }

                // NIP AUDITOR mengikuti data yang tersimpan di Google Sheets
                val sheetNip = form.originalAuditorNip?.ifBlank { null } ?: existingAudit?.nip?.ifBlank { null } ?: user.nip

                // NAMA AUDITOR mengikuti data yang tersimpan di Google Sheets
                val sheetNamaPetugas = form.originalAuditorName?.ifBlank { null } ?: existingAudit?.namaPetugas?.ifBlank { null } ?: user.namaPetugas

                // NO MOBIL terkunci mengikuti data Google Sheets
                val sheetNoMobil = if (existingAudit != null && existingAudit.noMobil.isNotBlank()) {
                    existingAudit.noMobil
                } else {
                    form.noMobil
                }

                val effectiveKode = if (form.kodeUnik.isNotBlank()) {
                    form.kodeUnik
                } else if (existingAudit?.kodeUnik?.isNotBlank() == true) {
                    existingAudit.kodeUnik
                } else {
                    UniqueCodeGenerator.generateAuditCode()
                }
                val updatedAudit = DataAuditEntity(
                    id = form.editingAuditId,
                    cloudId = existingAudit?.cloudId ?: 0L,
                    auditUuid = existingAudit?.auditUuid?.ifBlank { null } ?: effectiveKode,
                    kodeUnik = effectiveKode,
                    tanggal = sheetTanggal,
                    timestamp = sheetTimestamp,
                    periode = form.periode,
                    nip = sheetNip,
                    namaPetugas = sheetNamaPetugas,
                    noMobil = sheetNoMobil,
                    platNomer = form.platNomer.ifBlank { existingAudit?.platNomer.orEmpty().ifBlank { "B-${sheetNoMobil}-AUT" } },
                    tipe = form.tipe.ifBlank { existingAudit?.tipe.orEmpty().ifBlank { "Armada Standar" } },
                    warna = form.warna.ifBlank { existingAudit?.warna.orEmpty().ifBlank { "Standar" } },
                    tahun = form.tahun.ifBlank { existingAudit?.tahun.orEmpty() },
                    tanggalService = finalTanggalService,
                    service = finalService,
                    pool = form.pool.ifBlank { existingAudit?.pool.orEmpty().ifBlank { user.kodePool } },
                    totalPoinTemuan = calculatedTotalPoin,
                    jenisSample = form.jenisSample,
                    sampleTanggal = finalSampleTanggal,
                    paketService = finalPaketService,
                    kpiScoresJson = scoresJson,
                    kpiPhotosJson = photosJson,
                    kpiNotesJson = notesJson,

                    photo1 = flatPhotos[0], catatanPhoto1 = flatNotes[0],
                    photo2 = flatPhotos[1], catatanPhoto2 = flatNotes[1],
                    photo3 = flatPhotos[2], catatanPhoto3 = flatNotes[2],
                    photo4 = flatPhotos[3], catatanPhoto4 = flatNotes[3],
                    photo5 = flatPhotos[4], catatanPhoto5 = flatNotes[4],
                    photo6 = flatPhotos[5], catatanPhoto6 = flatNotes[5],
                    photo7 = flatPhotos[6], catatanPhoto7 = flatNotes[6],
                    photo8 = flatPhotos[7], catatanPhoto8 = flatNotes[7],
                    photo9 = flatPhotos[8], catatanPhoto9 = flatNotes[8],
                    photo10 = flatPhotos[9], catatanPhoto10 = flatNotes[9],
                    photo11 = flatPhotos[10], catatanPhoto11 = flatNotes[10],
                    photo12 = flatPhotos[11], catatanPhoto12 = flatNotes[11],
                    photo13 = flatPhotos[12], catatanPhoto13 = flatNotes[12],
                    photo14 = flatPhotos[13], catatanPhoto14 = flatNotes[13],
                    photo15 = flatPhotos[14], catatanPhoto15 = flatNotes[14],
                    photo16 = flatPhotos[15], catatanPhoto16 = flatNotes[15],
                    photo17 = flatPhotos[16], catatanPhoto17 = flatNotes[16],
                    photo18 = flatPhotos[17], catatanPhoto18 = flatNotes[17],
                    photo19 = flatPhotos[18], catatanPhoto19 = flatNotes[18],
                    photo20 = flatPhotos[19], catatanPhoto20 = flatNotes[19],

                    isSynced = false
                )

                repository.updateAudit(updatedAudit)
                clearFormDraft()
                _auditForm.value = AuditFormState() // Reset form
                _eventMessage.emit("Data Audit No Mobil ${updatedAudit.noMobil} berhasil diperbarui!")
                onSuccess()
            } else {
                // New Audit: Timestamp dari data input dibuat update
                val currentTimestamp = System.currentTimeMillis()
                val lockedTanggal = DateUtils.formatDate(currentTimestamp)
                var effectiveKode = if (form.kodeUnik.isNotBlank()) form.kodeUnik.trim() else UniqueCodeGenerator.generateAuditCode()
                val existingAudits = repository.getAllAuditsList()
                while (existingAudits.any { 
                    it.kodeUnik.trim().equals(effectiveKode, ignoreCase = true) || 
                    it.auditUuid.trim().equals(effectiveKode, ignoreCase = true) 
                }) {
                    effectiveKode = UniqueCodeGenerator.generateAuditCode()
                }
                val newAudit = DataAuditEntity(
                    cloudId = 0L,
                    auditUuid = effectiveKode,
                    kodeUnik = effectiveKode,
                    tanggal = lockedTanggal,
                    timestamp = currentTimestamp,
                    periode = form.periode,
                    nip = user.nip,
                    namaPetugas = user.namaPetugas,
                    noMobil = form.noMobil,
                    platNomer = form.platNomer.ifBlank { form.noMobil },
                    tipe = form.tipe.ifBlank { "-" },
                    warna = form.warna.ifBlank { "-" },
                    tahun = form.tahun.ifBlank { "-" },
                    tanggalService = finalTanggalService,
                    service = finalService,
                    pool = form.pool.ifBlank { user.kodePool },
                    totalPoinTemuan = calculatedTotalPoin,
                    jenisSample = form.jenisSample,
                    sampleTanggal = finalSampleTanggal,
                    paketService = finalPaketService,
                    kpiScoresJson = scoresJson,
                    kpiPhotosJson = photosJson,
                    kpiNotesJson = notesJson,

                    photo1 = flatPhotos[0], catatanPhoto1 = flatNotes[0],
                    photo2 = flatPhotos[1], catatanPhoto2 = flatNotes[1],
                    photo3 = flatPhotos[2], catatanPhoto3 = flatNotes[2],
                    photo4 = flatPhotos[3], catatanPhoto4 = flatNotes[3],
                    photo5 = flatPhotos[4], catatanPhoto5 = flatNotes[4],
                    photo6 = flatPhotos[5], catatanPhoto6 = flatNotes[5],
                    photo7 = flatPhotos[6], catatanPhoto7 = flatNotes[6],
                    photo8 = flatPhotos[7], catatanPhoto8 = flatNotes[7],
                    photo9 = flatPhotos[8], catatanPhoto9 = flatNotes[8],
                    photo10 = flatPhotos[9], catatanPhoto10 = flatNotes[9],
                    photo11 = flatPhotos[10], catatanPhoto11 = flatNotes[10],
                    photo12 = flatPhotos[11], catatanPhoto12 = flatNotes[11],
                    photo13 = flatPhotos[12], catatanPhoto13 = flatNotes[12],
                    photo14 = flatPhotos[13], catatanPhoto14 = flatNotes[13],
                    photo15 = flatPhotos[14], catatanPhoto15 = flatNotes[14],
                    photo16 = flatPhotos[15], catatanPhoto16 = flatNotes[15],
                    photo17 = flatPhotos[16], catatanPhoto17 = flatNotes[16],
                    photo18 = flatPhotos[17], catatanPhoto18 = flatNotes[17],
                    photo19 = flatPhotos[18], catatanPhoto19 = flatNotes[18],
                    photo20 = flatPhotos[19], catatanPhoto20 = flatNotes[19],

                    isSynced = false
                )

                val auditId = repository.saveAudit(newAudit)

                if (!form.isVehicleFound && form.noMobil.isNotBlank()) {
                    repository.insertKendaraan(
                        com.example.data.local.entity.MasterKendaraanEntity(
                            noMobil = newAudit.noMobil,
                            platNomer = newAudit.platNomer,
                            tipe = newAudit.tipe,
                            warna = newAudit.warna,
                            tahun = newAudit.tahun,
                            tanggalService = newAudit.tanggalService,
                            service = newAudit.service,
                            pool = newAudit.pool
                        )
                    )
                }

                clearFormDraft()
                _auditForm.value = AuditFormState() // Reset form
                _eventMessage.emit("Data Audit No Mobil ${newAudit.noMobil} berhasil disimpan secara offline dan siap disinkronkan!")
                onSuccess()
            }
        }
    }

    fun selectAuditForDetail(audit: DataAuditEntity?) {
        _selectedAuditDetail.value = audit
    }

    fun deleteAudit(id: Long) {
        val user = _currentUser.value
        if (user == null) {
            viewModelScope.launch {
                _eventMessage.emit("Harap login terlebih dahulu untuk menghapus data audit.")
            }
            return
        }
        val role = user.levelAkses.trim().uppercase()
        if (role == "AUDITOR") {
            viewModelScope.launch {
                _eventMessage.emit("Level Akses AUDITOR tidak memiliki izin untuk menghapus data audit.")
            }
            return
        }

        val isSuperAdmin = role == "SUPER ADMIN"
        val isAdmin = role == "ADMIN"
        val isSkt = role == "SKT"

        if (!isSuperAdmin && !isAdmin && !isSkt) {
            viewModelScope.launch {
                _eventMessage.emit("Hanya user level SKT, Admin, atau Super Admin yang dapat menghapus data ini.")
            }
            return
        }

        viewModelScope.launch {
            val audit = repository.getAuditById(id)
            if (audit == null) {
                _eventMessage.emit("Data audit tidak ditemukan.")
                return@launch
            }

            // Fitur hapus pada level Admin dan SKT hanya akan aktif selama 48 jam sejak audit dibuat
            if ((isAdmin || isSkt) && !isSuperAdmin) {
                val auditTime = if (audit.timestamp > 0L) {
                    audit.timestamp
                } else {
                    DateUtils.parseToMillis(audit.tanggal, "") ?: audit.timestamp
                }
                val elapsedMs = System.currentTimeMillis() - auditTime
                val max48HoursMs = 48L * 60L * 60L * 1000L
                if (elapsedMs > max48HoursMs) {
                    _eventMessage.emit("Fitur hapus untuk level Admin & SKT hanya aktif selama 48 jam sejak audit dibuat.")
                    return@launch
                }
            }

            repository.deleteAudit(id)
            _eventMessage.emit("Data audit berhasil dihapus.")
        }
    }

    fun deleteSingleAuditPhoto(auditId: Long, itemKey: String, photoPath: String, onComplete: () -> Unit = {}) {
        val user = _currentUser.value
        if (user == null) {
            viewModelScope.launch {
                _eventMessage.emit("Harap login terlebih dahulu untuk menghapus foto lampiran.")
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val audit = repository.getAuditById(auditId)
            if (audit == null) {
                withContext(Dispatchers.Main) {
                    _eventMessage.emit("Data audit tidak ditemukan.")
                }
                return@launch
            }

            val role = user.levelAkses.trim().uppercase()
            val isSuperAdmin = role == "SUPER ADMIN"
            val isAdmin = role == "ADMIN"
            val isSkt = role == "SKT"
            val isAuditor = role == "AUDITOR"
            val isCreator = (audit.nip.isNotBlank() && audit.nip.trim().equals(user.nip.trim(), ignoreCase = true)) ||
                    audit.namaPetugas.trim().equals(user.namaPetugas.trim(), ignoreCase = true) ||
                    audit.namaPetugas.trim().equals(user.username.trim(), ignoreCase = true)
            val isAuditorSamePool = isAuditor && audit.pool.isNotBlank() && audit.pool.trim().equals(user.kodePool.trim(), ignoreCase = true)

            if (!isSuperAdmin && !isAdmin && !isSkt && !isCreator && !isAuditorSamePool) {
                withContext(Dispatchers.Main) {
                    _eventMessage.emit("Anda tidak memiliki izin untuk menghapus foto lampiran audit ini.")
                }
                return@launch
            }

            val currentPhotosMap = jsonToMapString(audit.kpiPhotosJson).toMutableMap()
            val currentNotesMap = jsonToMapString(audit.kpiNotesJson).toMutableMap()

            val lastUnderscore = itemKey.lastIndexOf('_')
            val isKpiSlot = lastUnderscore > 0 &&
                    !itemKey.startsWith("kendaraan_") &&
                    itemKey.substring(lastUnderscore + 1).toIntOrNull() != null

            if (isKpiSlot) {
                val itemId = itemKey.substring(0, lastUnderscore)
                val targetSlot = itemKey.substring(lastUnderscore + 1).toInt()
                val relatedKeys = currentPhotosMap.keys.filter { it.startsWith("${itemId}_") } +
                                  currentNotesMap.keys.filter { it.startsWith("${itemId}_") }
                val maxSlot = (relatedKeys.mapNotNull { it.substringAfterLast('_').toIntOrNull() } + listOf(targetSlot + 1)).maxOrNull() ?: targetSlot

                for (i in targetSlot until maxSlot) {
                    val nextKey = "${itemId}_${i + 1}"
                    val currKey = "${itemId}_$i"
                    val nextPhoto = currentPhotosMap[nextKey]
                    if (nextPhoto != null) {
                        currentPhotosMap[currKey] = nextPhoto
                    } else {
                        currentPhotosMap.remove(currKey)
                    }
                    val nextNote = currentNotesMap[nextKey]
                    if (nextNote != null) {
                        currentNotesMap[currKey] = nextNote
                    } else {
                        currentNotesMap.remove(currKey)
                    }
                }
                currentPhotosMap.remove("${itemId}_$maxSlot")
                currentNotesMap.remove("${itemId}_$maxSlot")
            } else {
                currentPhotosMap.remove(itemKey)
                currentNotesMap.remove(itemKey)

                if (photoPath.isNotBlank()) {
                    val matchingKeys = currentPhotosMap.filterValues { it == photoPath }.keys.toList()
                    matchingKeys.forEach {
                        currentPhotosMap.remove(it)
                        currentNotesMap.remove(it)
                    }
                }
            }

            val vehicleKeys = listOf(
                "kendaraan_depan", "kendaraan_samping_kiri", "kendaraan_interior_depan_kiri",
                "kendaraan_interior_belakang_kiri", "kendaraan_belakang", "kendaraan_interior_belakang",
                "kendaraan_samping_kanan"
            )
            val legacyP = mutableListOf(
                audit.photo1, audit.photo2, audit.photo3, audit.photo4, audit.photo5,
                audit.photo6, audit.photo7, audit.photo8, audit.photo9, audit.photo10,
                audit.photo11, audit.photo12, audit.photo13, audit.photo14, audit.photo15,
                audit.photo16, audit.photo17, audit.photo18, audit.photo19, audit.photo20
            )
            val legacyN = mutableListOf(
                audit.catatanPhoto1, audit.catatanPhoto2, audit.catatanPhoto3, audit.catatanPhoto4, audit.catatanPhoto5,
                audit.catatanPhoto6, audit.catatanPhoto7, audit.catatanPhoto8, audit.catatanPhoto9, audit.catatanPhoto10,
                audit.catatanPhoto11, audit.catatanPhoto12, audit.catatanPhoto13, audit.catatanPhoto14, audit.catatanPhoto15,
                audit.catatanPhoto16, audit.catatanPhoto17, audit.catatanPhoto18, audit.catatanPhoto19, audit.catatanPhoto20
            )

            val vIdx = vehicleKeys.indexOf(itemKey)
            if (vIdx in 0..6 && vIdx < legacyP.size) {
                legacyP[vIdx] = ""
                legacyN[vIdx] = ""
            }
            if (photoPath.isNotBlank()) {
                for (i in 0 until legacyP.size) {
                    if (legacyP[i] == photoPath) {
                        legacyP[i] = ""
                        legacyN[i] = ""
                    }
                }
            }

            val updatedAudit = audit.copy(
                kpiPhotosJson = mapToJson(currentPhotosMap),
                kpiNotesJson = mapToJson(currentNotesMap),
                photo1 = legacyP[0], catatanPhoto1 = legacyN[0],
                photo2 = legacyP[1], catatanPhoto2 = legacyN[1],
                photo3 = legacyP[2], catatanPhoto3 = legacyN[2],
                photo4 = legacyP[3], catatanPhoto4 = legacyN[3],
                photo5 = legacyP[4], catatanPhoto5 = legacyN[4],
                photo6 = legacyP[5], catatanPhoto6 = legacyN[5],
                photo7 = legacyP[6], catatanPhoto7 = legacyN[6],
                photo8 = legacyP[7], catatanPhoto8 = legacyN[7],
                photo9 = legacyP[8], catatanPhoto9 = legacyN[8],
                photo10 = legacyP[9], catatanPhoto10 = legacyN[9],
                photo11 = legacyP[10], catatanPhoto11 = legacyN[10],
                photo12 = legacyP[11], catatanPhoto12 = legacyN[11],
                photo13 = legacyP[12], catatanPhoto13 = legacyN[12],
                photo14 = legacyP[13], catatanPhoto14 = legacyN[13],
                photo15 = legacyP[14], catatanPhoto15 = legacyN[14],
                photo16 = legacyP[15], catatanPhoto16 = legacyN[15],
                photo17 = legacyP[16], catatanPhoto17 = legacyN[16],
                photo18 = legacyP[17], catatanPhoto18 = legacyN[17],
                photo19 = legacyP[18], catatanPhoto19 = legacyN[18],
                photo20 = legacyP[19], catatanPhoto20 = legacyN[19]
            )

            repository.updateAudit(updatedAudit)
            withContext(Dispatchers.Main) {
                if (_selectedAuditDetail.value?.id == auditId) {
                    _selectedAuditDetail.value = updatedAudit
                }
                _eventMessage.emit("Foto lampiran berhasil dihapus.")
                onComplete()
            }
        }
    }

    // Sync Actions
    fun syncPendingAudits() {
        viewModelScope.launch {
            val (success, fail) = repository.syncAllPendingAudits()
            var downloadedCount = 0
            if (repository.sessionManager.scriptUrl.isNotBlank()) {
                try {
                    val downloadRes = repository.downloadAuditsFromCloud()
                    if (downloadRes.isSuccess) {
                        downloadedCount = downloadRes.getOrDefault(Pair(0, 0)).first
                    }
                } catch (e: Exception) {
                    // Ignore download error during sync
                }
            }
            if (success > 0 || downloadedCount > 0) {
                _eventMessage.emit("Sinkronisasi Selesai: $success terkirim, $downloadedCount data audit diperbarui.")
            } else if (fail > 0) {
                _eventMessage.emit("Sinkronisasi: $fail gagal terkirim.")
            } else {
                _eventMessage.emit("Semua data audit sudah tersinkronkan dengan cloud.")
            }
        }
    }

    fun downloadMasterData() {
        viewModelScope.launch {
            val res = repository.downloadMasterDataFromCloud()
            if (res.isSuccess) {
                _eventMessage.emit("Master data berhasil diperbarui dari Google Sheets!")
            } else {
                _eventMessage.emit("Gagal sync master data: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    private val _isSyncingKendaraan = MutableStateFlow(false)
    val isSyncingKendaraan = _isSyncingKendaraan.asStateFlow()

    private val _syncKendaraanProgress = MutableStateFlow("")
    val syncKendaraanProgress = _syncKendaraanProgress.asStateFlow()

    fun syncKendaraan(poolFilter: String = "", onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            _isSyncingKendaraan.value = true
            _syncKendaraanProgress.value = "Menghubungi server Google Sheets..."
            val res = repository.syncKendaraanOnlyFromCloud(
                poolFilter = poolFilter,
                onProgress = { current, total, msg ->
                    _syncKendaraanProgress.value = if (total > 0) {
                        val pct = ((current.toDouble() / total.toDouble()) * 100).toInt().coerceIn(0, 100)
                        "$msg ($pct%)"
                    } else {
                        msg
                    }
                }
            )
            _isSyncingKendaraan.value = false
            _syncKendaraanProgress.value = ""
            if (res.isSuccess) {
                val count = res.getOrDefault(0)
                onComplete(count)
            } else {
                _eventMessage.emit("Gagal sync kendaraan: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    // Master CRUD
    fun addOrUpdateKendaraan(kendaraan: MasterKendaraanEntity) {
        val role = _currentUser.value?.levelAkses?.trim()?.uppercase() ?: ""
        if (role != "SUPER ADMIN" && role != "ADMIN") {
            viewModelScope.launch {
                _eventMessage.emit("Akses ditolak: Hanya Super Admin dan Admin yang dapat menambah atau mengedit kendaraan.")
            }
            return
        }
        viewModelScope.launch {
            repository.insertKendaraan(kendaraan)
            _eventMessage.emit("Data kendaraan ${kendaraan.noMobil} berhasil disimpan.")
        }
    }

    fun deleteKendaraan(id: Int) {
        val role = _currentUser.value?.levelAkses?.trim()?.uppercase() ?: ""
        if (role != "SUPER ADMIN" && role != "ADMIN") {
            viewModelScope.launch {
                _eventMessage.emit("Akses ditolak: Hanya Super Admin dan Admin yang dapat menghapus kendaraan.")
            }
            return
        }
        viewModelScope.launch {
            repository.deleteKendaraan(id)
            _eventMessage.emit("Kendaraan berhasil dihapus.")
        }
    }

    fun clearDemoData(onComplete: (Int, Int) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val (auditsDeleted, vehiclesDeleted) = repository.clearDemoData(clearAllAudits = true, clearDemoVehicles = true)
            _eventMessage.emit("Data demo berhasil dibersihkan ($auditsDeleted audit, $vehiclesDeleted mobil dummy dihapus)")
            onComplete(auditsDeleted, vehiclesDeleted)
        }
    }

    fun deleteAllAudits(onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = repository.deleteAllAudits()
            _eventMessage.emit("Seluruh data audit ($count data) berhasil dibersihkan")
            onComplete(count)
        }
    }

    fun deleteAllKendaraan(onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = repository.deleteAllKendaraan()
            _eventMessage.emit("Seluruh master kendaraan ($count unit) berhasil dikosongkan")
            onComplete(count)
        }
    }

    fun addOrUpdatePool(pool: MasterPoolEntity) {
        viewModelScope.launch {
            repository.insertPool(pool)
            _eventMessage.emit("Master Pool ${pool.kodePool} berhasil disimpan.")
        }
    }

    fun deletePool(id: Int) {
        viewModelScope.launch {
            repository.deletePool(id)
            _eventMessage.emit("Pool berhasil dihapus.")
        }
    }

    fun addOrUpdateUser(user: UserEntity) {
        viewModelScope.launch {
            repository.insertUser(user)
            _eventMessage.emit("User ${user.namaPetugas} (${user.username}) berhasil disimpan.")
        }
    }

    fun deleteUser(nip: String) {
        viewModelScope.launch {
            repository.deleteUser(nip)
            _eventMessage.emit("User NIP $nip berhasil dihapus.")
        }
    }

    fun updatePhotoSetting(setting: PengaturanNamaPhotoEntity) {
        viewModelScope.launch {
            repository.updatePhotoSetting(setting)
            _eventMessage.emit("Nama tampilan foto ${setting.id} diperbarui.")
        }
    }

    fun addOrUpdatePeriode(periode: MasterPeriodeEntity) {
        viewModelScope.launch {
            repository.insertPeriode(periode)
            _eventMessage.emit("Data Periode '${periode.periode}' berhasil disimpan.")
        }
    }

    fun deletePeriode(id: Int) {
        viewModelScope.launch {
            repository.deletePeriode(id)
            _eventMessage.emit("Periode berhasil dihapus.")
        }
    }

    private val _isSyncingSample = MutableStateFlow(false)
    val isSyncingSample = _isSyncingSample.asStateFlow()

    fun syncSampleWithGoogleSheet(onComplete: (Boolean) -> Unit = {}) {
        val scriptUrl = repository.sessionManager.scriptUrl
        if (scriptUrl.isBlank()) {
            viewModelScope.launch {
                _eventMessage.emit("URL Google Script belum diatur. Silakan atur di menu Integrasi Google Script.")
                onComplete(false)
            }
            return
        }

        viewModelScope.launch {
            _isSyncingSample.value = true
            val res = repository.syncSamplesFromCloud()
            _isSyncingSample.value = false
            if (res.isSuccess) {
                val count = res.getOrDefault(0)
                _eventMessage.emit("Data sample berhasil diperbarui dari Google Sheets ($count area)!")
                onComplete(true)
            } else {
                _eventMessage.emit("Gagal mengambil data sample dari Google Sheets: ${res.exceptionOrNull()?.message}")
                onComplete(false)
            }
        }
    }

    fun addOrUpdateSample(sample: MasterSampleEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insertOrUpdateSample(sample)
            _eventMessage.emit("Jumlah sample area '${sample.statusArea}' berhasil disimpan & disinkronkan ke Google Sheets.")
            onComplete()
        }
    }

    fun updateJumlahSample(statusArea: String, jumlah: Int, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateJumlahSample(statusArea, jumlah)
            _eventMessage.emit("Jumlah sample area '$statusArea' berhasil diperbarui & disinkronkan ke Google Sheets.")
            onComplete()
        }
    }

    fun deleteSample(statusArea: String) {
        viewModelScope.launch {
            repository.deleteSample(statusArea)
            _eventMessage.emit("Data sample area '$statusArea' berhasil dihapus dari Google Sheets.")
        }
    }

    // GRADE KPI FUNCTIONS (SUPER ADMIN)
    fun saveGradeKpi(entity: MasterGradeKpiEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.saveGradeKpi(entity)
            _isSyncing.value = false
            _eventMessage.emit("Grade KPI '${entity.kategori}' (${String.format(java.util.Locale.US, "%.2f - %.2f", entity.minScore, entity.maxScore)}) berhasil disimpan & disinkronkan.")
            onComplete()
        }
    }

    fun updateGradeKpi(kategori: String, minScore: Double, maxScore: Double, keterangan: String = "", onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.updateGradeKpi(kategori, minScore, maxScore, keterangan)
            _isSyncing.value = false
            _eventMessage.emit("Range Grade '$kategori' berhasil diperbarui (${String.format(java.util.Locale.US, "%.2f - %.2f", minScore, maxScore)}) & disinkronkan.")
            onComplete()
        }
    }

    fun resetDefaultGradeKpi(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.resetDefaultGradeKpi()
            _isSyncing.value = false
            _eventMessage.emit("Grade KPI berhasil direset ke standar bawaan (A, B, C, D, E) & disinkronkan.")
            onComplete()
        }
    }

    fun syncAllGradeKpiToCloud(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isSyncing.value = true
            val res = repository.syncAllMasterGradeKpiToCloud()
            _isSyncing.value = false
            res.onSuccess { count ->
                _eventMessage.emit("Berhasil menyinkronkan $count data Grade KPI ke Google Sheets!")
                onComplete(true, "Berhasil menyinkronkan $count data Grade KPI ke Google Sheets")
            }.onFailure { err ->
                _eventMessage.emit("Gagal sinkron Grade KPI: ${err.message}")
                onComplete(false, err.message ?: "Gagal sinkronisasi")
            }
        }
    }

    fun initGradeKpiSheet(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.initGradeKpiSheetRemote()
            _isSyncing.value = false
            result.onSuccess { msg ->
                _eventMessage.emit("Inisialisasi Tabel GRADE KPI Berhasil!")
                onResult(true, msg)
            }.onFailure { err ->
                _eventMessage.emit("Inisialisasi Tabel GRADE KPI Gagal: ${err.message}")
                onResult(false, err.message ?: "Terjadi kesalahan saat inisialisasi tabel di Sheets.")
            }
        }
    }

    fun getGradeForScore(score: Double): String {
        return repository.calculateGradeForScore(score, allGradeKpi.value)
    }

    fun saveSettings(scriptUrl: String, logoUrl: String, sheetId: String, folderId: String, bigQuery: String) {
        repository.sessionManager.scriptUrl = scriptUrl
        repository.sessionManager.logoUrl = logoUrl
        repository.sessionManager.sheetId = sheetId
        repository.sessionManager.driveFolderId = folderId
        repository.sessionManager.bigQueryDataset = bigQuery
        viewModelScope.launch {
            _eventMessage.emit("Pengaturan sistem & Google Script berhasil disimpan.")
        }
    }

    fun downloadSinglePhoto(
        context: android.content.Context,
        photoPath: String,
        fileName: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = com.example.util.PhotoDownloadHelper.savePhotoToGallery(context, photoPath, fileName)
            if (result.isSuccess) {
                _eventMessage.emit("Foto berhasil diunduh ke folder Galeri (Pictures/AuditPhotos)")
                onComplete(true, "Foto tersimpan di Galeri")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Gagal mengunduh foto"
                _eventMessage.emit("Gagal: $err")
                onComplete(false, err)
            }
        }
    }

    fun downloadAllPhotosFromAudit(
        context: android.content.Context,
        audit: DataAuditEntity,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        onComplete: (Int, Int, String) -> Unit
    ) {
        viewModelScope.launch {
            val photoSettingsList = photoSettings.value
            val (success, fail, msg) = com.example.util.PhotoDownloadHelper.downloadAllAuditPhotos(
                context = context,
                audit = audit,
                photoTitles = photoSettingsList,
                onProgress = onProgress
            )
            _eventMessage.emit(msg)
            onComplete(success, fail, msg)
        }
    }

    // Web Server PC Realtime Controls
    fun startWebServer() {
        webReportServer.start(
            onStarted = { url ->
                _isWebServerRunning.value = true
                _webServerUrl.value = url
                refreshWebServerAddress()
            },
            onError = { err ->
                _isWebServerRunning.value = false
                viewModelScope.launch {
                    _eventMessage.emit("Web Server Error: $err")
                }
            }
        )
    }

    fun stopWebServer() {
        webReportServer.stop()
        _isWebServerRunning.value = false
    }

    fun refreshWebServerAddress() {
        _availableIps.value = webReportServer.getAllAvailableIps()
        _isEmulatorEnvironment.value = webReportServer.isCurrentIpEmulator()
        _currentServerPort.value = webReportServer.currentPort
        _webServerUrl.value = webReportServer.getServerUrl()
        _isWebServerRunning.value = webReportServer.isRunning
    }

    fun selectServerIp(ip: String) {
        webReportServer.selectedIpOverride = ip
        _webServerUrl.value = webReportServer.getServerUrl()
        _isEmulatorEnvironment.value = webReportServer.isCurrentIpEmulator()
    }

    fun changeServerPort(port: Int) {
        _currentServerPort.value = port
        webReportServer.changePort(
            newPort = port,
            onStarted = { url ->
                _isWebServerRunning.value = true
                _webServerUrl.value = url
                viewModelScope.launch {
                    _eventMessage.emit("Port Web Server berhasil diubah ke $port: $url")
                }
            },
            onError = { err ->
                viewModelScope.launch {
                    _eventMessage.emit("Gagal mengubah port: $err")
                }
            }
        )
    }

    fun exportStandaloneHtmlReport(onSuccess: (java.io.File) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val file = webReportServer.generateStandaloneHtmlFile()
                onSuccess(file)
            } catch (e: Exception) {
                onError(e.message ?: "Gagal mengekspor file Web HTML")
            }
        }
    }

    fun exportStandaloneExcelReport(onSuccess: (java.io.File) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val file = webReportServer.generateStandaloneExcelFile()
                onSuccess(file)
            } catch (e: Exception) {
                onError(e.message ?: "Gagal mengekspor file Excel")
            }
        }
    }

    fun initInputAuditSheet(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.initInputAuditSheetRemote()
            if (result.isSuccess) {
                val msg = result.getOrDefault("Tabel INPUT_AUDIT berhasil diinisialisasi!")
                _eventMessage.emit(msg)
                onResult(true, msg)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Gagal inisialisasi tabel INPUT_AUDIT"
                _eventMessage.emit(err)
                onResult(false, err)
            }
        }
    }

    fun setupSpreadsheet(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.setupSpreadsheetRemote()
            if (result.isSuccess) {
                val msg = result.getOrDefault("Seluruh sheet berhasil diinisialisasi!")
                _eventMessage.emit(msg)
                onResult(true, msg)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Gagal inisialisasi spreadsheet"
                _eventMessage.emit(err)
                onResult(false, err)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webReportServer.stop()
    }
}
