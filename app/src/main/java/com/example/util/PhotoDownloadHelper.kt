package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.local.entity.DataAuditEntity
import com.example.data.local.entity.PengaturanNamaPhotoEntity
import com.example.data.model.KpiAuditSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AuditPhotoItem(
    val slotIndex: Int,
    val photoPath: String,
    val title: String,
    val note: String,
    val itemKey: String
)

object PhotoDownloadHelper {

    /**
     * Extracts all valid photo items from a DataAuditEntity.
     */
    fun extractPhotosFromAudit(
        audit: DataAuditEntity,
        photoTitles: List<PengaturanNamaPhotoEntity> = emptyList()
    ): List<AuditPhotoItem> {
        val result = mutableListOf<AuditPhotoItem>()
        val allKpiItems = KpiAuditSpec.getAllItems()

        val kpiPhotos = try {
            val obj = JSONObject(audit.kpiPhotosJson)
            val map = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = obj.optString(k, "")
                if (v.isNotBlank()) map[k] = v
            }
            map
        } catch (e: Exception) {
            emptyMap<String, String>()
        }

        val kpiNotes = try {
            val obj = JSONObject(audit.kpiNotesJson)
            val map = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optString(k, "")
            }
            map
        } catch (e: Exception) {
            emptyMap<String, String>()
        }

        val kpiScores = try {
            val obj = JSONObject(audit.kpiScoresJson)
            val map = mutableMapOf<String, Int>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optInt(k, 0)
            }
            map
        } catch (e: Exception) {
            emptyMap<String, Int>()
        }

        if (kpiPhotos.isNotEmpty()) {
            var counter = 1
            for (kpiItem in allKpiItems) {
                val score = kpiScores[kpiItem.id] ?: 0
                val maxSlots = if (score > 0) score else kpiItem.maxPoints
                for (pIdx in 0 until maxSlots) {
                    val key = "${kpiItem.id}_$pIdx"
                    val path = kpiPhotos[key]
                    if (!path.isNullOrBlank() && com.example.data.model.AuditDatabaseSchema.isValidPhotoUrl(path)) {
                        val note = kpiNotes[key].orEmpty().trim()
                        val title = kpiItem.getLabelForPhoto(pIdx)
                        result.add(
                            AuditPhotoItem(
                                slotIndex = counter++,
                                photoPath = path,
                                title = title,
                                note = note,
                                itemKey = key
                            )
                        )
                    }
                }
            }

            // Ekstraksi 7 Foto Kendaraan & Interior
            val vehiclePhotoConfigs = listOf(
                "kendaraan_depan" to "Foto Kendaraan - Depan",
                "kendaraan_samping_kiri" to "Foto Kendaraan - Samping Kiri",
                "kendaraan_interior_depan_kiri" to "Foto Kendaraan - Interior Depan Kiri",
                "kendaraan_interior_belakang_kiri" to "Foto Kendaraan - Interior Belakang Kiri",
                "kendaraan_belakang" to "Foto Kendaraan - Belakang",
                "kendaraan_interior_belakang" to "Foto Kendaraan - Interior Belakang",
                "kendaraan_samping_kanan" to "Foto Kendaraan - Samping Kanan"
            )
            for ((vKey, vTitle) in vehiclePhotoConfigs) {
                val vPath = kpiPhotos[vKey]
                if (!vPath.isNullOrBlank() && com.example.data.model.AuditDatabaseSchema.isValidPhotoUrl(vPath)) {
                    result.add(
                        AuditPhotoItem(
                            slotIndex = counter++,
                            photoPath = vPath,
                            title = vTitle,
                            note = "Foto Kendaraan",
                            itemKey = vKey
                        )
                    )
                }
            }
        }

        // Fallback to legacy fields
        if (result.isEmpty()) {
            val legacy = listOf(
                audit.photo1 to audit.catatanPhoto1,
                audit.photo2 to audit.catatanPhoto2,
                audit.photo3 to audit.catatanPhoto3,
                audit.photo4 to audit.catatanPhoto4,
                audit.photo5 to audit.catatanPhoto5,
                audit.photo6 to audit.catatanPhoto6,
                audit.photo7 to audit.catatanPhoto7,
                audit.photo8 to audit.catatanPhoto8,
                audit.photo9 to audit.catatanPhoto9,
                audit.photo10 to audit.catatanPhoto10,
                audit.photo11 to audit.catatanPhoto11,
                audit.photo12 to audit.catatanPhoto12,
                audit.photo13 to audit.catatanPhoto13,
                audit.photo14 to audit.catatanPhoto14,
                audit.photo15 to audit.catatanPhoto15,
                audit.photo16 to audit.catatanPhoto16,
                audit.photo17 to audit.catatanPhoto17,
                audit.photo18 to audit.catatanPhoto18,
                audit.photo19 to audit.catatanPhoto19,
                audit.photo20 to audit.catatanPhoto20
            )
            val defaultVehicleKeys = listOf(
                "kendaraan_depan" to "Foto Kendaraan - Depan",
                "kendaraan_samping_kiri" to "Foto Kendaraan - Samping Kiri",
                "kendaraan_interior_depan_kiri" to "Foto Kendaraan - Interior Depan Kiri",
                "kendaraan_interior_belakang_kiri" to "Foto Kendaraan - Interior Belakang Kiri",
                "kendaraan_belakang" to "Foto Kendaraan - Belakang",
                "kendaraan_interior_belakang" to "Foto Kendaraan - Interior Belakang",
                "kendaraan_samping_kanan" to "Foto Kendaraan - Samping Kanan"
            )
            var counter = 1
            legacy.forEachIndexed { idx, (path, rawNote) ->
                if (path.isNotBlank() && com.example.data.model.AuditDatabaseSchema.isValidPhotoUrl(path)) {
                    val trimmed = rawNote.trim()
                    val vehicleDef = defaultVehicleKeys.getOrNull(idx)
                    val title = photoTitles.getOrNull(idx)?.tampilan?.ifBlank { null }
                        ?: if (idx < 7 && trimmed.contains("Kendaraan", ignoreCase = true)) trimmed
                        else vehicleDef?.second ?: "Foto ${idx + 1}"
                    val itemKey = if (idx < 7) (vehicleDef?.first ?: "legacy_$idx") else "legacy_$idx"
                    result.add(
                        AuditPhotoItem(
                            slotIndex = counter++,
                            photoPath = path,
                            title = title,
                            note = trimmed,
                            itemKey = itemKey
                        )
                    )
                }
            }
        }

        return result
    }

    /**
     * Downloads or saves a single photo directly into the Android Pictures/AuditPhotos gallery folder.
     */
    suspend fun savePhotoToGallery(
        context: Context,
        photoPath: String,
        customFileName: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val cleanName = customFileName.replace("[^a-zA-Z0-9_\\-]".toRegex(), "_")
            val fileName = if (cleanName.endsWith(".jpg", ignoreCase = true)) cleanName else "$cleanName.jpg"

            var inputStream: InputStream? = null
            try {
                if (photoPath.startsWith("http://", ignoreCase = true) || photoPath.startsWith("https://", ignoreCase = true)) {
                    var currentUrl = DriveUrlHelper.toDownloadUrl(photoPath)
                    var connection: HttpURLConnection
                    var redirectCount = 0
                    while (true) {
                        connection = URL(currentUrl).openConnection() as HttpURLConnection
                        connection.connectTimeout = 15000
                        connection.readTimeout = 20000
                        connection.instanceFollowRedirects = true
                        connection.doInput = true
                        connection.connect()
                        val code = connection.responseCode
                        if (code in 300..399 && redirectCount < 5) {
                            val newLoc = connection.getHeaderField("Location")
                            if (!newLoc.isNullOrBlank()) {
                                currentUrl = newLoc
                                redirectCount++
                                continue
                            }
                        }
                        if (code in 200..299) {
                            inputStream = connection.inputStream
                            break
                        } else {
                            return@withContext Result.failure(Exception("Gagal mengambil foto dari server (HTTP $code)"))
                        }
                    }
                } else {
                    val localFile = File(photoPath)
                    if (localFile.exists() && localFile.canRead()) {
                        inputStream = FileInputStream(localFile)
                    } else {
                        return@withContext Result.failure(Exception("File foto lokal tidak ditemukan: $photoPath"))
                    }
                }

                if (inputStream == null) {
                    return@withContext Result.failure(Exception("Gagal membuka stream foto"))
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/AuditPhotos")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }

                    val resolver = context.contentResolver
                    val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        ?: return@withContext Result.failure(Exception("Gagal membuat entri MediaStore"))

                    resolver.openOutputStream(imageUri)?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                    } ?: return@withContext Result.failure(Exception("Gagal menulis ke MediaStore stream"))

                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)

                    Result.success(imageUri)
                } else {
                    // Fallback for older Android versions
                    val picturesDir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "AuditPhotos"
                    ).apply { if (!exists()) mkdirs() }

                    val targetFile = File(picturesDir, fileName)
                    FileOutputStream(targetFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }

                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(targetFile.absolutePath),
                        arrayOf("image/jpeg"),
                        null
                    )

                    Result.success(Uri.fromFile(targetFile))
                }
            } finally {
                inputStream?.close()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads a specific list of photos with progress callback.
     * Returns Triple(successCount, failCount, message)
     */
    suspend fun downloadPhotosList(
        context: Context,
        photos: List<AuditPhotoItem>,
        noMobil: String,
        periode: String,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Triple<Int, Int, String> = withContext(Dispatchers.IO) {
        if (photos.isEmpty()) {
            return@withContext Triple(0, 0, "Tidak ada foto untuk diunduh pada mobil $noMobil")
        }

        var successCount = 0
        var failCount = 0
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanNoMobil = noMobil.replace("[^a-zA-Z0-9]".toRegex(), "")
        val cleanPeriode = periode.replace("[^a-zA-Z0-9]".toRegex(), "")

        photos.forEachIndexed { index, photoItem ->
            val cleanTitle = photoItem.title.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val fileName = "AUDIT_${cleanNoMobil}_${cleanPeriode}_${index + 1}_${cleanTitle}_$timestamp.jpg"

            val result = savePhotoToGallery(context, photoItem.photoPath, fileName)
            if (result.isSuccess) {
                successCount++
            } else {
                failCount++
            }
            onProgress(index + 1, photos.size)
        }

        val message = if (failCount == 0) {
            "Berhasil mengunduh $successCount foto ke folder Galeri (Pictures/AuditPhotos)"
        } else {
            "Selesai: $successCount foto berhasil diunduh, $failCount foto gagal."
        }

        Triple(successCount, failCount, message)
    }

    /**
     * Downloads all photos from an audit with progress callback.
     * Returns Triple(successCount, failCount, message)
     */
    suspend fun downloadAllAuditPhotos(
        context: Context,
        audit: DataAuditEntity,
        photoTitles: List<PengaturanNamaPhotoEntity> = emptyList(),
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Triple<Int, Int, String> = withContext(Dispatchers.IO) {
        val photos = extractPhotosFromAudit(audit, photoTitles)
        downloadPhotosList(
            context = context,
            photos = photos,
            noMobil = audit.noMobil,
            periode = audit.periode,
            onProgress = onProgress
        )
    }
}
