package com.example.util

import com.example.data.local.entity.DataAuditEntity
import java.util.Locale

/**
 * Utilitas untuk memfilter dan memastikan integritas data hasil INPUT AUDIT.
 * Sesuai aturan: "Data hasil INPUT AUDIT tidak akan di tampilkan jika di temukan ID yang sama"
 */
object AuditFilterUtils {

    /**
     * Mendapatkan ID identitas unik dari sebuah data audit.
     * Prioritas:
     * 1. kodeUnik (misal: "AUD-XXXXX-XXXX")
     * 2. auditUuid (misal: "AUD-..." atau UUID)
     * 3. cloudId jika > 0 (nomor ID baris dari cloud/sheet)
     * 4. local database id jika > 0
     */
    fun getAuditUniqueId(audit: DataAuditEntity): String {
        val kode = audit.kodeUnik.trim()
        if (kode.isNotBlank()) return kode.uppercase(Locale.US)

        val uuid = audit.auditUuid.trim()
        if (uuid.isNotBlank()) return uuid.uppercase(Locale.US)

        if (audit.cloudId > 0L) {
            return "CLOUD_${audit.cloudId}"
        }

        if (audit.id > 0L) {
            return "ID_${audit.id}"
        }

        return ""
    }

    /**
     * Memastikan data hasil INPUT AUDIT tidak akan ditampilkan jika ditemukan ID yang sama.
     * Jika ditemukan record dengan ID yang sama dengan record yang sudah diproses sebelumnya,
     * record duplikat tersebut TIDAK AKAN DITAMPILKAN.
     */
    fun filterUniqueAudits(audits: List<DataAuditEntity>): List<DataAuditEntity> {
        if (audits.isEmpty()) return emptyList()

        val seenIds = HashSet<String>()
        val result = ArrayList<DataAuditEntity>(audits.size)

        for (audit in audits) {
            val uniqueId = getAuditUniqueId(audit)
            if (uniqueId.isNotEmpty()) {
                if (seenIds.contains(uniqueId)) {
                    // ID yang sama ditemukan! Tidak akan ditampilkan.
                    continue
                }
                seenIds.add(uniqueId)
            }
            result.add(audit)
        }

        return result
    }

    /**
     * Mengambil nilai waktu kronologis dari data audit dalam milliseconds.
     * Prioritas:
     * 1. timestamp (> 0L)
     * 2. parsing string tanggal/waktu (misal: ISO, dd/MM/yyyy HH:mm:ss, dll.)
     * 3. fallback 0L
     */
    fun getEffectiveTimestamp(audit: DataAuditEntity): Long {
        if (audit.timestamp > 0L) return audit.timestamp
        val parsed = DateUtils.parseToMillis(audit.tanggal, "")
        if (parsed != null && parsed > 0L) return parsed
        return 0L
    }

    /**
     * Memeriksa apakah kandidat [candidate] merupakan input yang lebih baru (terakhir) dibanding [current].
     */
    fun isNewerInput(candidate: DataAuditEntity, current: DataAuditEntity): Boolean {
        val candidateTime = getEffectiveTimestamp(candidate)
        val currentTime = getEffectiveTimestamp(current)

        if (candidateTime != currentTime) {
            return candidateTime > currentTime
        }

        // Jika timestamp/waktu sama persis, gunakan database ID lokal terbesar (input belakangan)
        if (candidate.id > 0L && current.id > 0L && candidate.id != current.id) {
            return candidate.id > current.id
        }

        // Jika ID lokal sama atau 0, bandingkan nomor baris/cloudId Google Sheets
        if (candidate.cloudId > 0L && current.cloudId > 0L && candidate.cloudId != current.cloudId) {
            return candidate.cloudId > current.cloudId
        }

        return false
    }

    /**
     * Menyaring data audit: Jika terdapat no mobil yang sama pada satu periode,
     * maka hanya ditampilkan data pada input terakhir (input teranyar).
     *
     * Urutan tampilan asli (kronologis DESC) tetap dipertahankan.
     */
    fun filterLatestPerCarAndPeriod(audits: List<DataAuditEntity>): List<DataAuditEntity> {
        if (audits.isEmpty()) return emptyList()

        // Kelompokkan dan temukan input terakhir untuk setiap kombinasi (noMobil, periode)
        val latestByCarAndPeriod = HashMap<String, DataAuditEntity>()

        for (audit in audits) {
            val car = audit.noMobil.trim().uppercase(Locale.US).ifBlank {
                audit.platNomer.trim().uppercase(Locale.US)
            }
            if (car.isBlank()) continue

            val period = audit.periode.trim().uppercase(Locale.US)
            val key = "${car}_${period}"

            val existing = latestByCarAndPeriod[key]
            if (existing == null || isNewerInput(audit, existing)) {
                latestByCarAndPeriod[key] = audit
            }
        }

        // Simpan referensi audit yang merupakan input terakhir
        val latestAuditsSet = HashSet(latestByCarAndPeriod.values)
        val latestIds = HashSet<Long>()
        val latestUniqueIds = HashSet<String>()
        for (latestAudit in latestByCarAndPeriod.values) {
            if (latestAudit.id > 0L) {
                latestIds.add(latestAudit.id)
            }
            val uid = getAuditUniqueId(latestAudit)
            if (uid.isNotEmpty()) {
                latestUniqueIds.add(uid)
            }
        }

        // Filter list: jika audit memiliki car yang tercatat, hanya lolos jika merupakan input terakhir
        return audits.filter { audit ->
            val car = audit.noMobil.trim().uppercase(Locale.US).ifBlank {
                audit.platNomer.trim().uppercase(Locale.US)
            }
            if (car.isBlank()) {
                true
            } else {
                val uid = getAuditUniqueId(audit)
                audit in latestAuditsSet ||
                        (audit.id > 0L && latestIds.contains(audit.id)) ||
                        (uid.isNotEmpty() && latestUniqueIds.contains(uid))
            }
        }
    }
}
