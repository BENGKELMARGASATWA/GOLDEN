package com.example.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility helper to render an inspection watermark on photos.
 * Stamping features (Permanen & Tidak dapat diedit manual):
 * - Pojok kanan atas (Top-Right corner)
 * - No Mobil yang di-input
 * - Nama Auditor
 * - Waktu inspeksi (Timestamp)
 * - High-contrast pill badge with vibrant border for 100% visibility on any background
 */
object WatermarkHelper {

    fun drawVehicleWatermark(
        canvas: Canvas,
        width: Float,
        height: Float,
        vehicleNumber: String,
        auditorName: String? = null,
        auditorNip: String? = null,
        timestampStr: String? = null,
        keterangan: String? = null,
        sample: String? = null,
        sampleTanggal: String? = null,
        paketService: String? = null
    ) {
        val minDim = minOf(width, height)
        // Scaled typography relative to canvas dimensions (dibuat 75% dari ukuran lama agar lebih ringkas)
        val titleFontSize = (minDim * 0.0225f).coerceIn(10f, 27f)
        val auditorFontSize = (titleFontSize * 0.90f).coerceIn(9f, 24f)
        val sampleFontSize = (titleFontSize * 0.88f).coerceIn(8.5f, 23f)
        val dateFontSize = (titleFontSize * 0.82f).coerceIn(8f, 21f)
        val ketFontSize = (titleFontSize * 0.82f).coerceIn(8f, 21f)

        // Susunan Watermark Mark Foto:
        // 1. No Mobil
        // 2. Auditor
        // 3. Sample: [Jenis sample (OPERASIONAL / SERVICE)] - [tanggal Service] - [jenis service]
        // 4. Tanggal
        // 5. Keterangan
        val vNum = vehicleNumber.trim().ifBlank { "UNIT MOBIL" }
        val titleText = "No Mobil: ${vNum.uppercase()}"
        val cleanNip = auditorNip?.trim().orEmpty()
        val cleanName = auditorName?.trim().orEmpty()
        val formattedAuditor = when {
            cleanNip.isNotBlank() && cleanName.isNotBlank() -> {
                if (cleanName.startsWith("$cleanNip/") || cleanName.contains("/")) cleanName
                else "$cleanNip/$cleanName"
            }
            cleanNip.isNotBlank() -> cleanNip
            cleanName.isNotBlank() -> cleanName
            else -> "AUDITOR"
        }
        val auditorText = "Auditor: ${formattedAuditor.uppercase()}"

        val rawSample = sample?.trim()?.ifBlank { null } ?: "OPERASIONAL"
        val sampleText = buildString {
            append("Sample: ")
            val upperSample = rawSample.uppercase()
            append(upperSample)
            if (upperSample.contains("SERVICE")) {
                val tgl = sampleTanggal?.trim().orEmpty()
                val paket = paketService?.trim().orEmpty()
                if (tgl.isNotBlank()) {
                    append(" - [$tgl]")
                }
                if (paket.isNotBlank()) {
                    append(" - [${paket.uppercase()}]")
                }
            } else if (rawSample.contains("-") && (sampleTanggal == null && paketService == null)) {
                // If sample string already was pre-formatted with details
                // e.g. "SERVICE - [12/10/2026] - [BERKALA]"
                clear()
                append("Sample: $rawSample")
            }
        }

        val dateValue = timestampStr?.ifBlank { null } ?: SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val dateText = "Tanggal: $dateValue"
        
        val cleanKeterangan = keterangan?.trim()?.ifBlank { null } ?: "Temuan Audit"
        val ketFullText = "Keterangan: $cleanKeterangan"

        val titlePaint = Paint().apply {
            color = Color.parseColor("#FACC15") // Bright Yellow
            textSize = titleFontSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val auditorPaint = Paint().apply {
            color = Color.parseColor("#FFFFFF") // Crisp White
            textSize = auditorFontSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val samplePaint = Paint().apply {
            color = Color.parseColor("#4ADE80") // Mint Green / Fresh Emerald
            textSize = sampleFontSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val datePaint = Paint().apply {
            color = Color.parseColor("#E2E8F0") // Slate-200 / Light White
            textSize = dateFontSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val ketPaint = Paint().apply {
            color = Color.parseColor("#38BDF8") // Vibrant Sky Blue / Cyan
            textSize = ketFontSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val titleBounds = Rect()
        titlePaint.getTextBounds(titleText, 0, titleText.length, titleBounds)

        val auditorBounds = Rect()
        auditorPaint.getTextBounds(auditorText, 0, auditorText.length, auditorBounds)

        val sampleBounds = Rect()
        samplePaint.getTextBounds(sampleText, 0, sampleText.length, sampleBounds)

        val dateBounds = Rect()
        datePaint.getTextBounds(dateText, 0, dateText.length, dateBounds)

        // 75% padding & margin for compact table
        val padX = titleFontSize * 0.35f
        val padY = titleFontSize * 0.30f
        val marginX = (minDim * 0.020f).coerceIn(8f, 22f)
        val marginY = (minDim * 0.020f).coerceIn(8f, 22f)
        val maxAvailableTextWidth = (width - (marginX * 2) - (padX * 2)).coerceAtLeast(140f)

        // Split or scale keterangan if exceeds available width
        val ketLines = mutableListOf<String>()
        val testKetBounds = Rect()
        ketPaint.getTextBounds(ketFullText, 0, ketFullText.length, testKetBounds)
        if (testKetBounds.width() <= maxAvailableTextWidth) {
            ketLines.add(ketFullText)
        } else {
            // Check if contains " - " separator to split logically
            val parts = ketFullText.split(" - ")
            if (parts.size >= 2) {
                ketLines.add(parts[0]) // "Keterangan: Judul Input"
                ketLines.add(parts.subList(1, parts.size).joinToString(" - ")) // "Keterangan Photo"
            } else {
                // Wrap text into 2 lines
                val mid = ketFullText.length / 2
                val spaceIdx = ketFullText.indexOf(' ', mid).takeIf { it != -1 } ?: mid
                ketLines.add(ketFullText.substring(0, spaceIdx).trim())
                ketLines.add(ketFullText.substring(spaceIdx).trim())
            }
        }

        val ketLineBounds = ketLines.map { line ->
            val b = Rect()
            ketPaint.getTextBounds(line, 0, line.length, b)
            b
        }

        val maxTextWidth = maxOf(
            titleBounds.width(),
            auditorBounds.width(),
            sampleBounds.width(),
            dateBounds.width(),
            ketLineBounds.maxOfOrNull { it.width() } ?: 0
        ).toFloat()

        val lineSpacing = titleFontSize * 0.22f
        val totalKetHeight = ketLineBounds.indices.sumOf { idx ->
            (ketLineBounds[idx].height() + (if (idx > 0) lineSpacing * 0.5f else 0f)).toDouble()
        }.toFloat()

        val totalTextHeight = titleBounds.height() + lineSpacing +
                auditorBounds.height() + lineSpacing +
                sampleBounds.height() + lineSpacing +
                dateBounds.height() + lineSpacing +
                totalKetHeight

        val badgeWidth = minOf(maxTextWidth + (padX * 2), width - (marginX * 2))
        val badgeHeight = totalTextHeight + (padY * 2)

        // Position: Top-Right corner (Pojok Kanan Atas)
        val right = width - marginX
        val left = maxOf(marginX, right - badgeWidth)
        val top = marginY
        val bottom = top + badgeHeight

        val badgeRect = RectF(left, top, right, bottom)
        val cornerRadius = (titleFontSize * 0.25f).coerceIn(4f, 12f)

        // Solid dark pill background: 92% opacity
        val bgPaint = Paint().apply {
            color = Color.argb(235, 15, 23, 42)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Pill border: vibrant Cyan
        val borderPaint = Paint().apply {
            color = Color.argb(230, 56, 189, 248)
            style = Paint.Style.STROKE
            strokeWidth = (minDim * 0.0022f).coerceIn(1.0f, 2.8f)
            isAntiAlias = true
        }

        canvas.drawRoundRect(badgeRect, cornerRadius, cornerRadius, bgPaint)
        canvas.drawRoundRect(badgeRect, cornerRadius, cornerRadius, borderPaint)

        // Susunan teks mark foto: No Mobil, Auditor, Sample, Tanggal, Keterangan
        val textX = left + padX
        var currentY = top + padY + titleBounds.height()
        canvas.drawText(titleText, textX, currentY, titlePaint)

        currentY += lineSpacing + auditorBounds.height()
        canvas.drawText(auditorText, textX, currentY, auditorPaint)

        currentY += lineSpacing + sampleBounds.height()
        canvas.drawText(sampleText, textX, currentY, samplePaint)

        currentY += lineSpacing + dateBounds.height()
        canvas.drawText(dateText, textX, currentY, datePaint)

        ketLines.forEachIndexed { idx, line ->
            val b = ketLineBounds[idx]
            currentY += (if (idx == 0) lineSpacing else lineSpacing * 0.6f) + b.height()
            canvas.drawText(line, textX, currentY, ketPaint)
        }
    }
}
