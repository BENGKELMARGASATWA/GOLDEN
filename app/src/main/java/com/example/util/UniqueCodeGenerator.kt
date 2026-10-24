package com.example.util

import java.security.SecureRandom
import java.util.Locale

/**
 * Generator kode unik dengan kombinasi unik (alphanumeric acak + komponen waktu)
 * untuk seluruh tabel database (Audit, Kendaraan, Pool, Periode, Bobot, Sample, Grade, User).
 * Menghasilkan kombinasi unik yang selalu berubah pada setiap entitas baru.
 */
object UniqueCodeGenerator {
    // Karakter bersih yang mudah dibaca dan tidak membingungkan (tanpa 0/O, 1/I)
    private const val CHAR_POOL = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    private val secureRandom = SecureRandom()

    /**
     * Menghasilkan kode unik kombinasi dengan format: [PREFIX]-[KOMBINASI_WAKTU][KOMBINASI_ACAK]
     * Contoh hasil: AUD-8K3M9P, KND-7B4F2L, POL-9D1X5C
     */
    fun generate(prefix: String, randomLength: Int = 4): String {
        val cleanPrefix = prefix.trim().uppercase(Locale.US)
        val timeBase = (System.currentTimeMillis() % 10000000L).toString(36).uppercase(Locale.US)
        val randomPart = StringBuilder(randomLength).apply {
            for (i in 0 until randomLength) {
                append(CHAR_POOL[secureRandom.nextInt(CHAR_POOL.length)])
            }
        }.toString()

        val combination = "$timeBase$randomPart"
        return if (cleanPrefix.isNotBlank()) "$cleanPrefix-$combination" else combination
    }

    fun generateAuditCode(): String = generate("AUD", 4)
    fun generateKendaraanCode(): String = generate("KND", 4)
    fun generatePoolCode(): String = generate("POL", 4)
    fun generatePeriodeCode(): String = generate("PRD", 4)
    fun generateBobotCode(): String = generate("BBT", 4)
    fun generateSampleCode(): String = generate("SMP", 4)
    fun generateGradeCode(): String = generate("GRD", 4)
    fun generateUserCode(): String = generate("USR", 4)
}
