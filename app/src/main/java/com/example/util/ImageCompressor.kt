package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ImageCompressor {

    private const val TAG = "ImageCompressor"

    /**
     * Creates a temporary file in the cache directory and returns its content Uri via FileProvider.
     */
    fun createTempImageUri(context: Context): Pair<Uri, File>? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
            val storageDir = File(context.cacheDir, "camera_photos").apply {
                if (!exists()) mkdirs()
            }
            val file = File(storageDir, "IMG_AUDIT_${timeStamp}.jpg")
            if (!file.exists()) {
                file.createNewFile()
            }
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            Pair(uri, file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create temp image URI", e)
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqMaxDimension: Int = 1280): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqMaxDimension || width > reqMaxDimension) {
            val maxSide = maxOf(height, width)
            while ((maxSide / inSampleSize) > reqMaxDimension) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    /**
     * Compresses an image file from camera into an optimized local persistent image and returns its absolute path.
     * Resizes large dimensions to max 1280px and compresses JPEG quality to ~75% (< 150 KB).
     * Memory-safe for devices with 4GB RAM running Android 10.
     */
    suspend fun compressAndSaveImage(
        context: Context,
        sourceFile: File,
        auditPrefix: String = "audit_photo",
        watermarkText: String? = null,
        auditorName: String? = null,
        auditorNip: String? = null,
        watermarkTimestamp: String? = null,
        keterangan: String? = null,
        sample: String? = null,
        sampleTanggal: String? = null,
        paketService: String? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val destinationDir = File(context.filesDir, "audit_photos").apply { if (!exists()) mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
            val targetFile = File(destinationDir, "${auditPrefix}_${timeStamp}.jpg")

            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(sourceFile.absolutePath, boundsOptions)

            val maxDimension = 1280
            val sampleSize = calculateInSampleSize(boundsOptions, maxDimension)

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inMutable = true // Pre-allocate mutable bitmap directly, saving 12-24MB copy memory
            }
            var bitmap = try {
                BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions)
            } catch (oom: OutOfMemoryError) {
                System.gc()
                // Emergency low-memory fallback with higher sample size
                decodeOptions.inSampleSize = sampleSize * 2
                BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions)
            } ?: return@withContext sourceFile.absolutePath

            // Scale down to exact maxDimension if still larger
            val currentMaxSide = maxOf(bitmap.width, bitmap.height)
            if (currentMaxSide > maxDimension) {
                val scale = maxDimension.toFloat() / currentMaxSide.toFloat()
                val targetW = (bitmap.width * scale).toInt().coerceAtLeast(1)
                val targetH = (bitmap.height * scale).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                if (scaled != bitmap) {
                    bitmap.recycle()
                    bitmap = scaled
                }
            }

            // Check orientation from EXIF
            val exif = try {
                ExifInterface(sourceFile.absolutePath)
            } catch (e: Exception) {
                null
            }
            val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                ?: ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }
            if (!matrix.isIdentity) {
                val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotatedBitmap != bitmap) {
                    bitmap.recycle()
                    bitmap = rotatedBitmap
                }
            }

            // Stamped Watermark No Mobil, Auditor, Tanggal, Keterangan if provided (Permanen & Tidak dapat diedit manual)
            if (!watermarkText.isNullOrBlank()) {
                val mutableBitmap = if (bitmap.isMutable) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = android.graphics.Canvas(mutableBitmap)
                WatermarkHelper.drawVehicleWatermark(
                    canvas = canvas,
                    width = mutableBitmap.width.toFloat(),
                    height = mutableBitmap.height.toFloat(),
                    vehicleNumber = watermarkText,
                    auditorName = auditorName,
                    auditorNip = auditorNip,
                    timestampStr = watermarkTimestamp,
                    keterangan = keterangan,
                    sample = sample,
                    sampleTanggal = sampleTanggal,
                    paketService = paketService
                )
                if (mutableBitmap != bitmap) {
                    bitmap.recycle()
                    bitmap = mutableBitmap
                }
            }

            // Save compressed JPEG with 75% quality
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
                out.flush()
            }
            bitmap.recycle()

            // Delete original temporary camera file if different
            if (sourceFile.exists() && sourceFile.absolutePath != targetFile.absolutePath) {
                sourceFile.delete()
            }

            targetFile.absolutePath
        } catch (t: Throwable) {
            Log.e(TAG, "Error compressing image file", t)
            if (t is OutOfMemoryError) {
                System.gc()
            }
            sourceFile.absolutePath
        }
    }

    /**
     * Compresses an image directly from a content Uri (e.g. from gallery/picker) into an optimized persistent file.
     */
    suspend fun compressAndSaveFromUri(
        context: Context,
        sourceUri: Uri,
        auditPrefix: String = "audit_photo",
        watermarkText: String? = null,
        auditorName: String? = null,
        auditorNip: String? = null,
        watermarkTimestamp: String? = null,
        keterangan: String? = null,
        sample: String? = null,
        sampleTanggal: String? = null,
        paketService: String? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val destinationDir = File(context.filesDir, "audit_photos").apply { if (!exists()) mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
            val targetFile = File(destinationDir, "${auditPrefix}_${timeStamp}.jpg")

            // Read dimensions
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, boundsOptions)
            }

            val maxDimension = 1280
            val sampleSize = calculateInSampleSize(boundsOptions, maxDimension)

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inMutable = true
            }
            var bitmap = try {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, decodeOptions)
                }
            } catch (oom: OutOfMemoryError) {
                System.gc()
                decodeOptions.inSampleSize = sampleSize * 2
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, decodeOptions)
                }
            } ?: return@withContext ""

            // Scale down to exact maxDimension if needed
            val currentMaxSide = maxOf(bitmap.width, bitmap.height)
            if (currentMaxSide > maxDimension) {
                val scale = maxDimension.toFloat() / currentMaxSide.toFloat()
                val targetW = (bitmap.width * scale).toInt().coerceAtLeast(1)
                val targetH = (bitmap.height * scale).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                if (scaled != bitmap) {
                    bitmap.recycle()
                    bitmap = scaled
                }
            }

            // Check orientation
            val orientation = try {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    val exif = ExifInterface(input)
                    exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (e: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }

            var finalBitmap = bitmap
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }
            if (!matrix.isIdentity) {
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated != bitmap) {
                    bitmap.recycle()
                    finalBitmap = rotated
                }
            }

            // Stamped Watermark No Mobil, Auditor, Tanggal, Keterangan if provided (Permanen & Tidak dapat diedit manual)
            if (!watermarkText.isNullOrBlank()) {
                val mutableBitmap = if (finalBitmap.isMutable) finalBitmap else finalBitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = android.graphics.Canvas(mutableBitmap)
                WatermarkHelper.drawVehicleWatermark(
                    canvas = canvas,
                    width = mutableBitmap.width.toFloat(),
                    height = mutableBitmap.height.toFloat(),
                    vehicleNumber = watermarkText,
                    auditorName = auditorName,
                    auditorNip = auditorNip,
                    timestampStr = watermarkTimestamp,
                    keterangan = keterangan,
                    sample = sample,
                    sampleTanggal = sampleTanggal,
                    paketService = paketService
                )
                if (mutableBitmap != finalBitmap) {
                    finalBitmap.recycle()
                    finalBitmap = mutableBitmap
                }
            }

            FileOutputStream(targetFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
                out.flush()
            }
            finalBitmap.recycle()

            targetFile.absolutePath
        } catch (t: Throwable) {
            Log.e(TAG, "Error compressing image from Uri", t)
            if (t is OutOfMemoryError) {
                System.gc()
            }
            ""
        }
    }

    /**
     * Converts a local image path to Base64 string for direct upload to Google Apps Script / Drive.
     */
    suspend fun fileToBase64(filePath: String): String = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext ""
            val bytes = file.readBytes()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (t: Throwable) {
            Log.e(TAG, "Error encoding file to Base64", t)
            if (t is OutOfMemoryError) {
                System.gc()
            }
            ""
        }
    }
}

object DateUtils {
    private val indonesianLocale = Locale("id", "ID")
    val wibTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Jakarta")

    fun getCurrentDateFormatted(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", indonesianLocale)
        sdf.timeZone = wibTimeZone
        return sdf.format(Date())
    }

    fun getCurrentTimestampFormatted(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", indonesianLocale)
        sdf.timeZone = wibTimeZone
        return sdf.format(Date())
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", indonesianLocale)
        sdf.timeZone = wibTimeZone
        return sdf.format(Date(timestamp))
    }

    fun formatTimestampWithSeconds(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", indonesianLocale)
        sdf.timeZone = wibTimeZone
        return sdf.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", indonesianLocale)
        sdf.timeZone = wibTimeZone
        return sdf.format(Date(timestamp))
    }

    /**
     * Mengubah string tanggal sembarang (termasuk format ISO 8601 UTC seperti 2026-08-11T17:00:00)
     * menjadi tanggal bersih berformat dd/MM/yyyy dalam zona waktu Indonesia (WIB).
     */
    fun formatDisplayDate(rawStr: String?): String {
        if (rawStr.isNullOrBlank()) return "-"
        val clean = rawStr.trim()
        val cal = parseToCalendar(clean) ?: return clean
        val sdf = SimpleDateFormat("dd/MM/yyyy", indonesianLocale)
        sdf.timeZone = wibTimeZone
        return sdf.format(cal.time)
    }

    fun normalizeDateString(rawStr: String?): String {
        if (rawStr.isNullOrBlank()) return ""
        val clean = rawStr.trim()
        val cal = parseToCalendar(clean) ?: return clean
        val sdf = SimpleDateFormat("dd/MM/yyyy", indonesianLocale)
        sdf.timeZone = wibTimeZone
        return sdf.format(cal.time)
    }

    fun formatDisplayWaktuTimestamp(timestamp: Long, fallbackStr: String = ""): String {
        if (fallbackStr.isNotBlank() && (fallbackStr.contains(":") || fallbackStr.length > 10)) {
            return fallbackStr
        }
        if (timestamp > 0L) {
            return formatTimestampWithSeconds(timestamp)
        }
        return fallbackStr.ifBlank { "-" }
    }

    /**
     * Mem-parsing string waktu/timestamp sembarang (misal: dari Google Sheets "dd/MM/yyyy HH:mm:ss",
     * ISO 8601, numeric milliseconds, atau "HH:mm:ss" dengan tanggal fallback) menjadi milliseconds.
     */
    fun parseToMillis(rawStr: String?, fallbackDateStr: String = ""): Long? {
        if (rawStr.isNullOrBlank()) return null
        val clean = rawStr.trim()
        val num = clean.toLongOrNull()
        if (num != null && num > 1_000_000_000L) {
            return num
        }
        val cal = parseToCalendar(clean)
        if (cal != null) {
            return cal.timeInMillis
        }
        if (fallbackDateStr.isNotBlank() && clean.contains(":")) {
            val combined = "${fallbackDateStr.trim()} $clean"
            val calComb = parseToCalendar(combined)
            if (calComb != null) {
                return calComb.timeInMillis
            }
        }
        return null
    }

    /**
     * Mem-parsing string tanggal ke java.util.Calendar (dalam zona waktu WIB).
     * Mampu mendeteksi format ISO 8601 (misal 2026-08-11T17:00:00 atau dengan Z)
     * dan otomatis mengonversi jam UTC ke jam WIB (+7 jam).
     * Contoh: 2026-08-11T17:00:00 UTC = 2026-08-12 00:00:00 WIB (Tepat tanggal 12).
     */
    fun parseToCalendar(rawStr: String?): Calendar? {
        if (rawStr.isNullOrBlank()) return null
        val clean = rawStr.trim()

        val firstPart = clean.split(",", ";", "\n", "|").firstOrNull()?.trim() ?: clean

        // 1. Parsing format ISO 8601 dengan 'T' (misal: 2026-08-11T17:00:00 atau ...Z)
        if (firstPart.contains("T")) {
            val isoPatterns = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm"
            )
            for (p in isoPatterns) {
                try {
                    val sdf = SimpleDateFormat(p, Locale.US)
                    // Serialisasi ISO Google Apps Script defaultnya adalah UTC (17:00 UTC = 00:00 WIB hari berikutnya)
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    val date = sdf.parse(firstPart)
                    if (date != null) {
                        val cal = Calendar.getInstance(wibTimeZone)
                        cal.time = date
                        return cal
                    }
                } catch (_: Exception) {}
            }
        }

        // 2. Parsing format tanggal & waktu umum (Format dengan jam/menit/detik harus diutamakan sebelum format tanggal saja)
        val standardPatterns = listOf(
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy HH:mm",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "dd-MM-yyyy HH:mm:ss",
            "dd-MM-yyyy HH:mm",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd HH:mm",
            "MM/dd/yyyy HH:mm:ss",
            "MM/dd/yyyy HH:mm",
            "dd/MM/yyyy",
            "yyyy-MM-dd",
            "dd-MM-yyyy",
            "d/M/yyyy",
            "d-M-yyyy",
            "yyyy/MM/dd",
            "MM/dd/yyyy"
        )
        for (p in standardPatterns) {
            try {
                val sdf = SimpleDateFormat(p, Locale.US)
                sdf.timeZone = wibTimeZone
                val date = sdf.parse(firstPart)
                if (date != null) {
                    val cal = Calendar.getInstance(wibTimeZone)
                    cal.time = date
                    return cal
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback regex untuk mengekstrak angka tahun, bulan, hari
        val isoRegex = Regex("""(\d{4})[-/](\d{1,2})[-/](\d{1,2})""").find(firstPart)
        if (isoRegex != null) {
            try {
                val year = isoRegex.groupValues[1].toInt()
                val month = isoRegex.groupValues[2].toInt() - 1
                val day = isoRegex.groupValues[3].toInt()
                val cal = Calendar.getInstance(wibTimeZone)
                cal.set(year, month, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                return cal
            } catch (_: Exception) {}
        }

        val dmyRegex = Regex("""(\d{1,2})[-/](\d{1,2})[-/](\d{4})""").find(firstPart)
        if (dmyRegex != null) {
            try {
                val day = dmyRegex.groupValues[1].toInt()
                val month = dmyRegex.groupValues[2].toInt() - 1
                val year = dmyRegex.groupValues[3].toInt()
                val cal = Calendar.getInstance(wibTimeZone)
                cal.set(year, month, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                return cal
            } catch (_: Exception) {}
        }

        return null
    }

    /**
     * Memeriksa apakah tanggal penugasan auditor cocok dengan hari ini.
     * Mendukung:
     * - Tanggal lengkap (dd/MM/yyyy, yyyy-MM-dd)
     * - ISO 8601 UTC string (contoh: 2026-08-11T17:00:00 yang dalam WIB adalah 12/08/2026 atau 12/09/2026)
     * - Keselarasan hari (misal: akun aktif pada tanggal 12, dan hari ini tepat tanggal 12)
     * - Multi-tanggal dipisahkan koma atau baris baru
     */
    fun isDateMatchingToday(dateStr: String?): Boolean {
        if (dateStr.isNullOrBlank()) return false
        val clean = dateStr.trim()

        val parts = clean.split(",", ";", "\n", "|").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size > 1) {
            return parts.any { isSingleDateMatchingToday(it) }
        }
        return isSingleDateMatchingToday(clean)
    }

    private fun isSingleDateMatchingToday(clean: String): Boolean {
        val todayCal = Calendar.getInstance(wibTimeZone)
        val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)
        val todayMonth = todayCal.get(Calendar.MONTH)
        val todayYear = todayCal.get(Calendar.YEAR)

        // 1. Jika hanya diisi angka hari saja (misal: "12", "tgl 12", "tanggal 12")
        val pureDigits = clean.replace(Regex("""(?i)[^\d]"""), "").toIntOrNull()
        if (pureDigits != null && pureDigits in 1..31 && clean.length <= 4) {
            if (pureDigits == todayDay) return true
        }

        // 2. Parse dengan parseToCalendar (otomatis mengonversi jam UTC ke WIB jika ada 'T' & timezone)
        val targetCal = parseToCalendar(clean)
        if (targetCal != null) {
            val targetDay = targetCal.get(Calendar.DAY_OF_MONTH)
            val targetMonth = targetCal.get(Calendar.MONTH)
            val targetYear = targetCal.get(Calendar.YEAR)

            // Kasus A: Cocok persis Hari, Bulan, dan Tahun
            if (targetDay == todayDay && targetMonth == todayMonth && targetYear == todayYear) {
                return true
            }

            // Kasus B: Tanggal hari sama (misal tepat tanggal 12), tahun sama, selisih bulan <= 1
            // Menangani toleransi bulan 0-index JavaScript (di mana Sept = index 8 sehingga tersimpan 08)
            if (targetDay == todayDay && targetYear == todayYear && Math.abs(targetMonth - todayMonth) <= 1) {
                return true
            }

            // Kasus C: Tepat hari yang sama di bulan ini pada tahun berjalan (penugasan rutin per tanggal)
            if (targetDay == todayDay && targetYear == todayYear) {
                return true
            }

            // Kasus D: Tepat hari yang sama (misal jadwal audit tanggal 12)
            if (targetDay == todayDay) {
                return true
            }
        }

        // 3. Fallback pencocokan teks angka hari
        val todayDayStr = String.format(Locale.US, "%02d", todayDay)
        if (clean.contains(todayDayStr) || (pureDigits != null && pureDigits == todayDay)) {
            val hasWholeDay = Regex("""\b$todayDay\b|\b$todayDayStr\b""").containsMatchIn(clean)
            if (hasWholeDay) return true
        }

        return false
    }
}
