package com.example.util

import java.io.File
import java.util.regex.Pattern

object DriveUrlHelper {
    private val DRIVE_FILE_ID_PATTERN = Pattern.compile("(?:/file/d/|id=|/open\\?id=|/d/)([a-zA-Z0-9_-]{25,})")

    /**
     * Ekstrak File ID unik dari URL Google Drive
     */
    fun extractDriveFileId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val matcher = DRIVE_FILE_ID_PATTERN.matcher(url)
        return if (matcher.find()) {
            matcher.group(1)
        } else null
    }

    /**
     * Konversi URL Google Drive ke URL direct image stream agar dapat ditampilkan
     * secara langsung oleh Coil AsyncImage atau diunduh tanpa terkena halaman HTML 'Akses Ditolak'.
     */
    fun toDirectImageUrl(urlOrPath: String?): String {
        if (urlOrPath.isNullOrBlank()) return ""
        val trimmed = urlOrPath.trim()

        // Saring pesan error upload lama
        if (trimmed.startsWith("Upload Error", ignoreCase = true) ||
            trimmed.contains("Akses ditolak", ignoreCase = true) ||
            trimmed.startsWith("[Izin", ignoreCase = true)
        ) {
            return ""
        }

        // Jika path lokal atau format file/content URI
        if (trimmed.startsWith("/") || trimmed.startsWith("file://") || trimmed.startsWith("content://")) {
            return trimmed
        }

        // Jika URL Google Drive
        if (trimmed.contains("drive.google.com") || trimmed.contains("googleusercontent.com")) {
            val fileId = extractDriveFileId(trimmed)
            if (!fileId.isNullOrBlank()) {
                // Gunakan CDN endpoint Google yang menyajikan stream gambar langsung
                return "https://lh3.googleusercontent.com/d/$fileId"
            }
        }

        return trimmed
    }

    /**
     * Menyediakan model yang tepat untuk Coil ImageRequest.
     * Mengembalikan objek java.io.File jika file lokal ada, atau URL stream langsung jika URL web.
     */
    fun resolveImageModel(urlOrPath: String?): Any {
        if (urlOrPath.isNullOrBlank()) return ""
        val trimmed = urlOrPath.trim()

        if (trimmed.startsWith("Upload Error", ignoreCase = true) ||
            trimmed.contains("Akses ditolak", ignoreCase = true)
        ) {
            return ""
        }

        // Cek file lokal
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            val file = File(trimmed)
            if (file.exists() && file.canRead()) {
                return file
            }
        }

        return toDirectImageUrl(trimmed)
    }

    /**
     * Menghasilkan URL direct download yang kompatibel untuk HttpURLConnection
     */
    fun toDownloadUrl(urlOrPath: String?): String {
        if (urlOrPath.isNullOrBlank()) return ""
        val trimmed = urlOrPath.trim()

        if (trimmed.contains("drive.google.com")) {
            val fileId = extractDriveFileId(trimmed)
            if (!fileId.isNullOrBlank()) {
                return "https://drive.google.com/uc?export=download&id=$fileId"
            }
        }
        return toDirectImageUrl(trimmed)
    }
}
