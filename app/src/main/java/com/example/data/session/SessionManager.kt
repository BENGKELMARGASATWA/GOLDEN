package com.example.data.session

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.entity.UserEntity

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("audit_session_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_LOGIN_TIMESTAMP = "login_timestamp"
        private const val KEY_NIP = "nip"
        private const val KEY_NAMA_PETUGAS = "nama_petugas"
        private const val KEY_USERNAME = "username"
        private const val KEY_LEVEL_AKSES = "level_akses"
        private const val KEY_KODE_POOL = "kode_pool"
        private const val KEY_STATUS = "status_user"
        private const val KEY_TANGGAL_AKTIF = "tanggal_aktif"

        // 48 Hours in milliseconds for standard roles, 12 Hours for AUDITOR
        private const val SESSION_DURATION_MS = 48L * 60L * 60L * 1000L
        private const val AUDITOR_SESSION_DURATION_MS = 12L * 60L * 60L * 1000L
        private const val KEY_LAST_ACTIVITY_TIMESTAMP = "last_activity_timestamp"

        // App Settings keys
        private const val KEY_LOGO_URL = "app_logo_url"
        private const val KEY_SHEET_ID = "sheet_id"
        private const val KEY_DRIVE_FOLDER_ID = "drive_folder_id"
        private const val KEY_SCRIPT_URL = "script_url"
        private const val KEY_BIGQUERY_DATASET = "bigquery_dataset"
        private const val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_AUTO_SYNC_INTERVAL_MINUTES = "auto_sync_interval_minutes"
        private const val KEY_LAST_SYNC_SUMMARY = "last_sync_summary"
        private const val KEY_DEMO_DATA_CLEARED = "demo_data_cleared"

        // Default Integrations (Google Apps Script, Google Sheets, Google Drive)
        const val DEFAULT_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbwa7leAgPhl8-Yk3rRmly1jzTWhYzTYDaDMCa7KhHpVDnP7HWC_i8xFjs96nLLssp81/exec"
        const val DEFAULT_SHEET_ID = "1gkKZMTwOPgEpwmyLPJykDpaqWFIn1FgjDvYMa6wn81M"
        const val DEFAULT_FOLDER_ID = "165IPc5ldYJlkahW30yVBCeKamIxpwwj0"
    }

    fun saveSession(user: UserEntity) {
        val now = System.currentTimeMillis()
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_LOGIN_TIMESTAMP, now)
            putLong(KEY_LAST_ACTIVITY_TIMESTAMP, now)
            putString(KEY_NIP, user.nip)
            putString(KEY_NAMA_PETUGAS, user.namaPetugas)
            putString(KEY_USERNAME, user.username)
            putString(KEY_LEVEL_AKSES, user.levelAkses)
            putString(KEY_KODE_POOL, user.kodePool)
            putString(KEY_STATUS, user.status)
            putString(KEY_TANGGAL_AKTIF, user.tanggalAktif)
            apply()
        }
    }

    fun recordUserActivity() {
        if (prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            prefs.edit().putLong(KEY_LAST_ACTIVITY_TIMESTAMP, System.currentTimeMillis()).apply()
        }
    }

    fun getLastActivityTimestamp(): Long {
        val last = prefs.getLong(KEY_LAST_ACTIVITY_TIMESTAMP, 0L)
        return if (last > 0L) last else prefs.getLong(KEY_LOGIN_TIMESTAMP, 0L)
    }

    fun clearSession() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_LOGIN_TIMESTAMP)
            remove(KEY_LAST_ACTIVITY_TIMESTAMP)
            remove(KEY_NIP)
            remove(KEY_NAMA_PETUGAS)
            remove(KEY_USERNAME)
            remove(KEY_LEVEL_AKSES)
            remove(KEY_KODE_POOL)
            remove(KEY_STATUS)
            remove(KEY_TANGGAL_AKTIF)
            apply()
        }
    }

    fun isSessionValid(): Boolean {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) return false
        val level = prefs.getString(KEY_LEVEL_AKSES, "AUDITOR") ?: "AUDITOR"
        val currentTime = System.currentTimeMillis()
        return if (level.trim().equals("AUDITOR", ignoreCase = true)) {
            val lastActivity = getLastActivityTimestamp()
            val elapsedInactivity = currentTime - lastActivity
            elapsedInactivity < AUDITOR_SESSION_DURATION_MS
        } else {
            val loginTime = prefs.getLong(KEY_LOGIN_TIMESTAMP, 0L)
            val elapsed = currentTime - loginTime
            elapsed < SESSION_DURATION_MS
        }
    }

    fun getCurrentUser(): UserEntity? {
        if (!isSessionValid()) return null
        val nip = prefs.getString(KEY_NIP, null) ?: return null
        val nama = prefs.getString(KEY_NAMA_PETUGAS, "") ?: ""
        val username = prefs.getString(KEY_USERNAME, "") ?: ""
        val level = prefs.getString(KEY_LEVEL_AKSES, "AUDITOR") ?: "AUDITOR"
        val pool = prefs.getString(KEY_KODE_POOL, "SKT") ?: "SKT"
        val status = prefs.getString(KEY_STATUS, "AKTIF") ?: "AKTIF"
        val tanggalAktif = prefs.getString(KEY_TANGGAL_AKTIF, "") ?: ""

        return UserEntity(
            nip = nip,
            namaPetugas = nama,
            username = username,
            password = "",
            levelAkses = level,
            kodePool = pool,
            status = status,
            tanggalAktif = tanggalAktif
        )
    }

    fun getRemainingSessionHours(): Long {
        val level = prefs.getString(KEY_LEVEL_AKSES, "AUDITOR") ?: "AUDITOR"
        val currentTime = System.currentTimeMillis()
        val remaining = if (level.trim().equals("AUDITOR", ignoreCase = true)) {
            val lastActivity = getLastActivityTimestamp()
            val elapsed = currentTime - lastActivity
            AUDITOR_SESSION_DURATION_MS - elapsed
        } else {
            val loginTime = prefs.getLong(KEY_LOGIN_TIMESTAMP, 0L)
            val elapsed = currentTime - loginTime
            SESSION_DURATION_MS - elapsed
        }
        return if (remaining > 0) remaining / (1000 * 60 * 60) else 0L
    }

    // App Configurations
    var logoUrl: String
        get() = prefs.getString(KEY_LOGO_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LOGO_URL, value).apply()

    var sheetId: String
        get() {
            val saved = prefs.getString(KEY_SHEET_ID, DEFAULT_SHEET_ID) ?: DEFAULT_SHEET_ID
            return if (saved.isBlank() || saved == "1PrIhg6n52IA2jRAvC-24sZMi6jLutIwhUUFkytArTJ8") DEFAULT_SHEET_ID else saved
        }
        set(value) = prefs.edit().putString(KEY_SHEET_ID, value).apply()

    var driveFolderId: String
        get() = prefs.getString(KEY_DRIVE_FOLDER_ID, DEFAULT_FOLDER_ID) ?: DEFAULT_FOLDER_ID
        set(value) = prefs.edit().putString(KEY_DRIVE_FOLDER_ID, value).apply()

    var scriptUrl: String
        get() {
            val saved = prefs.getString(KEY_SCRIPT_URL, "")
            return if (saved.isNullOrBlank() || saved.contains("AKfycbxAIvoQDWxtUENwNofeUNnfJ-qKoMVT") || saved.contains("AKfycbzqQcDHQTDvk7BPpMNMki")) DEFAULT_SCRIPT_URL else saved
        }
        set(value) = prefs.edit().putString(KEY_SCRIPT_URL, value).apply()

    var bigQueryDataset: String
        get() {
            val saved = prefs.getString(KEY_BIGQUERY_DATASET, "") ?: ""
            return if (saved == "fleet_maintenance_audit") "" else saved
        }
        set(value) = prefs.edit().putString(KEY_BIGQUERY_DATASET, value).apply()

    var isAutoSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SYNC_ENABLED, value).apply()

    var lastSyncTime: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TIME, value).apply()

    var autoSyncIntervalMinutes: Int
        get() = prefs.getInt(KEY_AUTO_SYNC_INTERVAL_MINUTES, 30)
        set(value) = prefs.edit().putInt(KEY_AUTO_SYNC_INTERVAL_MINUTES, value).apply()

    var lastSyncSummary: String
        get() = prefs.getString(KEY_LAST_SYNC_SUMMARY, "Belum pernah disinkronkan") ?: "Belum pernah disinkronkan"
        set(value) = prefs.edit().putString(KEY_LAST_SYNC_SUMMARY, value).apply()

    var isDemoDataCleared: Boolean
        get() = prefs.getBoolean(KEY_DEMO_DATA_CLEARED, false)
        set(value) = prefs.edit().putBoolean(KEY_DEMO_DATA_CLEARED, value).apply()

    var auditFormDraftJson: String
        get() = prefs.getString("audit_form_draft_json", "") ?: ""
        set(value) = prefs.edit().putString("audit_form_draft_json", value).apply()

    fun clearAuditFormDraft() {
        prefs.edit().remove("audit_form_draft_json").apply()
    }
}
