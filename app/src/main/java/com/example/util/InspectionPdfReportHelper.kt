package com.example.util

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Base64
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.DataAuditEntity
import com.example.data.local.entity.PengaturanNamaPhotoEntity
import com.example.data.model.KpiAuditSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Item data untuk rincian temuan sekaligus foto pada laporan inspeksi
 */
data class InspectionFindingItem(
    val index: Int,
    val title: String,
    val category: String, // "EXTERIOR", "INTERIOR", "CUCIAN", atau lainnya
    val score: Int,
    val note: String,
    val photoPath: String?
)

/**
 * Data terstruktur lengkap untuk dokumen Laporan Hasil Inspeksi Kendaraan
 */
data class InspectionReportData(
    val audit: DataAuditEntity,
    val exteriorPoints: Int,
    val interiorPoints: Int,
    val cucianPoints: Int,
    val findings: List<InspectionFindingItem>
)

object InspectionPdfReportHelper {

    /**
     * Mempersiapkan data laporan inspeksi dari DataAuditEntity
     */
    fun prepareReportData(
        audit: DataAuditEntity,
        photoTitles: List<PengaturanNamaPhotoEntity> = emptyList()
    ): InspectionReportData {
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

        val exteriorPoints = KpiAuditSpec.sections.find { it.id == "EXTERIOR" }
            ?.groups?.flatMap { it.items }?.sumOf { kpiScores[it.id] ?: 0 } ?: 0
        val interiorPoints = KpiAuditSpec.sections.find { it.id == "INTERIOR" }
            ?.groups?.flatMap { it.items }?.sumOf { kpiScores[it.id] ?: 0 } ?: 0
        val cucianPoints = KpiAuditSpec.sections.find { it.id == "CUCIAN" }
            ?.groups?.flatMap { it.items }?.sumOf { kpiScores[it.id] ?: 0 } ?: 0

        // Ekstrak semua foto menggunakan PhotoDownloadHelper
        val allPhotos = PhotoDownloadHelper.extractPhotosFromAudit(audit, photoTitles)
        // Ambil foto hasil inspeksi (bukan foto umum kendaraan)
        val inspectionPhotos = allPhotos.filter { item ->
            val trimmedNote = item.note.trim()
            !trimmedNote.contains("Foto Kendaraan", ignoreCase = true) &&
                    !item.itemKey.startsWith("kendaraan_")
        }

        val findingsList = mutableListOf<InspectionFindingItem>()
        var counter = 1

        // 1. Kumpulkan semua KPI item yang memiliki score > 0 (Titik Temuan)
        val allKpiItems = KpiAuditSpec.getAllItems()
        val processedPhotoKeys = mutableSetOf<String>()

        for (item in allKpiItems) {
            val score = kpiScores[item.id] ?: 0
            if (score > 0) {
                val section = KpiAuditSpec.sections.find { sec ->
                    sec.groups.any { grp -> grp.items.any { it.id == item.id } }
                }
                val categoryName = section?.title ?: "TEMUAN"

                // Cari foto terkait titik temuan ini
                val matchingPhoto = inspectionPhotos.find { photo ->
                    photo.itemKey.startsWith("${item.id}_") || photo.itemKey == item.id
                }

                if (matchingPhoto != null) {
                    processedPhotoKeys.add(matchingPhoto.itemKey)
                }

                // Ambil catatan dari foto atau dari kpiNotes
                val note = when {
                    !matchingPhoto?.note.isNullOrBlank() -> matchingPhoto!!.note.trim()
                    !kpiNotes[item.id].isNullOrBlank() -> kpiNotes[item.id]!!.trim()
                    !kpiNotes["${item.id}_0"].isNullOrBlank() -> kpiNotes["${item.id}_0"]!!.trim()
                    else -> "Kondisi tidak memenuhi standar operasional."
                }

                findingsList.add(
                    InspectionFindingItem(
                        index = counter++,
                        title = item.name,
                        category = categoryName,
                        score = score,
                        note = note,
                        photoPath = matchingPhoto?.photoPath
                    )
                )
            }
        }

        // 2. Tambahkan foto pengecekan lain yang belum masuk (misalnya foto temuan tambahan atau dokumentasi pengecekan)
        for (photo in inspectionPhotos) {
            if (!processedPhotoKeys.contains(photo.itemKey)) {
                val kpiItemId = if (photo.itemKey.contains("_")) photo.itemKey.substringBeforeLast("_") else photo.itemKey
                val score = kpiScores[kpiItemId] ?: 0
                val kpiItem = allKpiItems.find { it.id == kpiItemId }
                val section = KpiAuditSpec.sections.find { sec ->
                    sec.groups.any { grp -> grp.items.any { it.id == kpiItemId } }
                }

                val title = kpiItem?.name ?: photo.title
                val category = section?.title ?: "PENGECEKAN"
                val note = photo.note.ifBlank {
                    kpiNotes[kpiItemId] ?: (if (score > 0) "Kondisi temuan terlampir pada foto." else "Pemeriksaan titik normal.")
                }

                findingsList.add(
                    InspectionFindingItem(
                        index = counter++,
                        title = title,
                        category = category,
                        score = score,
                        note = note,
                        photoPath = photo.photoPath
                    )
                )
            }
        }

        // 3. Jika unit normal (0 temuan) dan tidak ada foto sama sekali, sertakan 1 entry status lolos standar
        if (findingsList.isEmpty()) {
            findingsList.add(
                InspectionFindingItem(
                    index = 1,
                    title = "Kondisi Kendaraan Memenuhi Standar Operasional",
                    category = "KESELURUHAN",
                    score = 0,
                    note = "Semua titik pemeriksaan (Exterior, Interior, Cucian) telah diperiksa dan dinyatakan lolos standar (0 Poin Temuan).",
                    photoPath = null
                )
            )
        }

        return InspectionReportData(
            audit = audit,
            exteriorPoints = exteriorPoints,
            interiorPoints = interiorPoints,
            cucianPoints = cucianPoints,
            findings = findingsList
        )
    }

    /**
     * Membuat dokumen PDF Laporan Hasil Inspeksi Kendaraan (A4 Portrait).
     * Susunan:
     * - Pojok kiri atas: Informasi Kendaraan
     * - Pojok kanan atas: Auditor sampai Point
     * - Bagian bawah mulai dari kiri: Catatan temuan sekaligus foto
     */
    suspend fun generatePdf(
        context: Context,
        reportData: InspectionReportData
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val audit = reportData.audit
            val safeMobil = audit.noMobil.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val safePlat = audit.platNomer.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Laporan_Inspeksi_${safeMobil}_${safePlat}_$dateStamp.pdf"
            val file = File(context.cacheDir, fileName)

            // Dimensi A4 Portrait (Points: 1/72 inch)
            val pageWidth = 595
            val pageHeight = 842
            val leftMargin = 28f
            val rightMargin = 567f
            val printableWidth = rightMargin - leftMargin // 539f
            val topMargin = 28f
            val bottomLimit = 780f // batas aman sebelum footer

            val pdfDocument = PdfDocument()

            // Paint Styles
            val bgPaint = Paint().apply { isAntiAlias = true }
            val strokePaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
            }
            val titlePaint = Paint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 13f
                color = Color.WHITE
            }
            val subtitlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 7.5f
                color = Color.parseColor("#E0E7FF")
            }
            val sectionHeaderPaint = Paint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 9f
                color = Color.WHITE
            }
            val labelPaint = Paint().apply {
                isAntiAlias = true
                textSize = 7.5f
                color = Color.parseColor("#64748B")
            }
            val valuePaint = Paint().apply {
                isAntiAlias = true
                textSize = 8f
                color = Color.parseColor("#0F172A")
            }
            val valueBoldPaint = Paint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 8.5f
                color = Color.parseColor("#0F172A")
            }
            val scoreBadgePaint = Paint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 9.5f
                textAlign = Paint.Align.CENTER
            }
            val footerPaint = Paint().apply {
                isAntiAlias = true
                textSize = 7f
                color = Color.parseColor("#94A3B8")
            }

            // Pre-load foto temuan secara aman, cepat, dan paralel
            val loadedBitmaps = mutableMapOf<String, Bitmap?>()
            val distinctPaths = reportData.findings.mapNotNull { it.photoPath?.takeIf { p -> p.isNotBlank() } }.distinct()

            coroutineScope {
                val deferredList = distinctPaths.map { path ->
                    async(Dispatchers.IO) {
                        val bmp = try {
                            withTimeoutOrNull(2500L) {
                                loadSampledBitmap(context, path, targetWidth = 320, targetHeight = 240)
                            }
                        } catch (e: Exception) {
                            null
                        }
                        path to bmp
                    }
                }
                deferredList.awaitAll().forEach { (path, bmp) ->
                    loadedBitmaps[path] = bmp
                }
            }

            var currentPageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            var currentPdfPage = pdfDocument.startPage(pageInfo)
            var canvas = currentPdfPage.canvas

            // Helper untuk menggambar Footer
            fun drawFooter(c: Canvas, pageNum: Int) {
                strokePaint.strokeWidth = 0.5f
                strokePaint.color = Color.parseColor("#E2E8F0")
                c.drawLine(leftMargin, 810f, rightMargin, 810f, strokePaint)

                val footerText = "Sistem Audit Armada Bluebird • Dokumen Resmi Hasil Pemeriksaan Fisik Kendaraan"
                c.drawText(footerText, leftMargin, 822f, footerPaint)

                val pageStr = "Halaman $pageNum"
                val pageW = footerPaint.measureText(pageStr)
                c.drawText(pageStr, rightMargin - pageW, 822f, footerPaint)
            }

            // 1. BANNER HEADER UTAMA (PT BLUE BIRD TBK)
            val bannerH = 50f
            bgPaint.color = Color.parseColor("#1D4ED8") // Blue 700
            canvas.drawRoundRect(RectF(leftMargin, topMargin, rightMargin, topMargin + bannerH), 6f, 6f, bgPaint)

            canvas.drawText("PT BLUE BIRD TBK • SISTEM AUDIT KI SKT V15 ID", leftMargin + 14f, topMargin + 16f, subtitlePaint)
            canvas.drawText("LAPORAN HASIL INSPEKSI KENDARAAN", leftMargin + 14f, topMargin + 34f, titlePaint)

            val displayKode = when {
                audit.kodeUnik.isNotBlank() -> audit.kodeUnik
                audit.auditUuid.isNotBlank() && audit.auditUuid.startsWith("AUD-") -> audit.auditUuid
                audit.cloudId > 0L -> "#${audit.cloudId}"
                else -> "#${audit.id}"
            }
            val printDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date())
            val infoRight = "Dokumen ID: $displayKode\nDicetak: $printDate WIB"
            val textPaintRight = TextPaint().apply {
                isAntiAlias = true
                textSize = 7.5f
                color = Color.WHITE
            }
            val rightLayout = StaticLayout.Builder.obtain(infoRight, 0, infoRight.length, textPaintRight, 180)
                .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
                .build()
            canvas.save()
            canvas.translate(rightMargin - 190f, topMargin + 12f)
            rightLayout.draw(canvas)
            canvas.restore()

            var currentY = topMargin + bannerH + 10f

            // 2. DUA KARTU POJOK:
            // POJOK KIRI ATAS: Informasi Kendaraan
            // POJOK KANAN ATAS: Auditor Sampai Point
            val colGap = 10f
            val colWidth = (printableWidth - colGap) / 2f // ~264.5f
            val topBoxHeight = 150f

            val leftBoxX = leftMargin
            val rightBoxX = leftMargin + colWidth + colGap

            // Background & Border Kiri Atas (Informasi Kendaraan)
            bgPaint.color = Color.parseColor("#F8FAFC")
            canvas.drawRoundRect(RectF(leftBoxX, currentY, leftBoxX + colWidth, currentY + topBoxHeight), 6f, 6f, bgPaint)
            strokePaint.strokeWidth = 1f
            strokePaint.color = Color.parseColor("#CBD5E1")
            canvas.drawRoundRect(RectF(leftBoxX, currentY, leftBoxX + colWidth, currentY + topBoxHeight), 6f, 6f, strokePaint)

            // Header Kartu Kiri Atas
            bgPaint.color = Color.parseColor("#0284C7") // Sky 600
            canvas.drawRoundRect(RectF(leftBoxX, currentY, leftBoxX + colWidth, currentY + 20f), 6f, 6f, bgPaint)
            // Hilangkan lengkungan bawah header kartu
            canvas.drawRect(RectF(leftBoxX, currentY + 12f, leftBoxX + colWidth, currentY + 20f), bgPaint)
            canvas.drawText("INFORMASI KENDARAAN", leftBoxX + 10f, currentY + 13.5f, sectionHeaderPaint)

            // Isi Kartu Kiri Atas
            var rowY = currentY + 34f
            val lblW = 75f

            fun drawInfoRow(c: Canvas, x: Float, y: Float, label: String, value: String, isBold: Boolean = false) {
                c.drawText(label, x + 10f, y, labelPaint)
                c.drawText(":", x + 10f + lblW - 6f, y, labelPaint)
                val p = if (isBold) valueBoldPaint else valuePaint
                c.drawText(value, x + 10f + lblW, y, p)
            }

            drawInfoRow(canvas, leftBoxX, rowY, "No Mobil", audit.noMobil, isBold = true)
            rowY += 13f
            drawInfoRow(canvas, leftBoxX, rowY, "Plat Nomer", audit.platNomer, isBold = true)
            rowY += 13f
            drawInfoRow(canvas, leftBoxX, rowY, "Tipe Armada", audit.tipe.ifBlank { "-" })
            rowY += 13f
            drawInfoRow(canvas, leftBoxX, rowY, "Warna / Tahun", "${audit.warna.ifBlank { "-" }} / ${audit.tahun.ifBlank { "-" }}")
            rowY += 13f
            drawInfoRow(canvas, leftBoxX, rowY, "Pool Armada", audit.pool.ifBlank { "-" }, isBold = true)
            rowY += 13f
            drawInfoRow(canvas, leftBoxX, rowY, "Periode Audit", audit.periode.ifBlank { "-" })
            rowY += 13f
            drawInfoRow(canvas, leftBoxX, rowY, "Jenis Sample", audit.jenisSample.ifBlank { "-" }, isBold = true)

            val isService = audit.jenisSample.equals("SERVICE", ignoreCase = true)
            if (isService) {
                rowY += 13f
                val tgl = if (audit.sampleTanggal.isNotBlank()) audit.sampleTanggal else audit.tanggalService.ifBlank { "-" }
                val pkt = if (audit.paketService.isNotBlank()) audit.paketService else audit.service.ifBlank { "-" }
                drawInfoRow(canvas, leftBoxX, rowY, "Service", "$tgl ($pkt)")
            }

            // Background & Border Kanan Atas (Auditor sampai Point)
            bgPaint.color = Color.parseColor("#F8FAFC")
            canvas.drawRoundRect(RectF(rightBoxX, currentY, rightBoxX + colWidth, currentY + topBoxHeight), 6f, 6f, bgPaint)
            strokePaint.strokeWidth = 1f
            strokePaint.color = Color.parseColor("#CBD5E1")
            canvas.drawRoundRect(RectF(rightBoxX, currentY, rightBoxX + colWidth, currentY + topBoxHeight), 6f, 6f, strokePaint)

            // Header Kartu Kanan Atas
            bgPaint.color = Color.parseColor("#1E40AF") // Blue 800
            canvas.drawRoundRect(RectF(rightBoxX, currentY, rightBoxX + colWidth, currentY + 20f), 6f, 6f, bgPaint)
            canvas.drawRect(RectF(rightBoxX, currentY + 12f, rightBoxX + colWidth, currentY + 20f), bgPaint)
            canvas.drawText("AUDITOR & EVALUASI POIN", rightBoxX + 10f, currentY + 13.5f, sectionHeaderPaint)

            // Isi Kartu Kanan Atas
            rowY = currentY + 34f
            val auditorDisplay = when {
                audit.nip.isNotBlank() && audit.namaPetugas.isNotBlank() -> "${audit.namaPetugas} (${audit.nip})"
                audit.namaPetugas.isNotBlank() -> audit.namaPetugas
                audit.nip.isNotBlank() -> audit.nip
                else -> "-"
            }
            drawInfoRow(canvas, rightBoxX, rowY, "Auditor / Petugas", auditorDisplay, isBold = true)
            rowY += 13f
            val waktuTimestamp = DateUtils.formatDisplayWaktuTimestamp(audit.timestamp, audit.tanggal)
            drawInfoRow(canvas, rightBoxX, rowY, "Waktu Inspeksi", waktuTimestamp)
            rowY += 13f
            val statusAudit = if (audit.totalPoinTemuan > 0) "ADA TEMUAN FISIK" else "LOLOS STANDAR OPERASIONAL"
            drawInfoRow(canvas, rightBoxX, rowY, "Status Hasil", statusAudit, isBold = true)

            // Badge Total Point Temuan
            rowY += 8f
            val scoreBoxH = 34f
            val hasTemuan = audit.totalPoinTemuan > 0
            val scoreBoxColor = if (hasTemuan) Color.parseColor("#FEF2F2") else Color.parseColor("#F0FDF4")
            val scoreBorderColor = if (hasTemuan) Color.parseColor("#FECACA") else Color.parseColor("#BBF7D0")
            val scoreTextColor = if (hasTemuan) Color.parseColor("#DC2626") else Color.parseColor("#15803D")

            val scoreRect = RectF(rightBoxX + 10f, rowY, rightBoxX + colWidth - 10f, rowY + scoreBoxH)
            bgPaint.color = scoreBoxColor
            canvas.drawRoundRect(scoreRect, 5f, 5f, bgPaint)
            strokePaint.strokeWidth = 1f
            strokePaint.color = scoreBorderColor
            canvas.drawRoundRect(scoreRect, 5f, 5f, strokePaint)

            scoreBadgePaint.color = scoreTextColor
            val scoreLabel = if (hasTemuan) {
                "TOTAL: ${audit.totalPoinTemuan} POIN TEMUAN"
            } else {
                "TOTAL: 0 POIN (STANDAR NORMAL)"
            }
            canvas.drawText(scoreLabel, scoreRect.centerX(), scoreRect.centerY() + 3.5f, scoreBadgePaint)

            // Rincian Breakdown Poin Kategori (Exterior | Interior | Cucian)
            rowY += scoreBoxH + 11f
            val breakdownStr = "Exterior: ${reportData.exteriorPoints} Poin  •  Interior: ${reportData.interiorPoints} Poin  •  Cucian: ${reportData.cucianPoints} Poin"
            val breakdownPaint = Paint().apply {
                isAntiAlias = true
                textSize = 7f
                textAlign = Paint.Align.CENTER
                color = Color.parseColor("#475569")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(breakdownStr, rightBoxX + (colWidth / 2f), rowY, breakdownPaint)

            currentY += topBoxHeight + 12f

            // 3. BAGIAN BAWAH MULAI DARI KIRI: CATATAN TEMUAN SEKALIGUS FOTO
            val subheaderH = 20f
            bgPaint.color = Color.parseColor("#0F172A") // Slate 900
            canvas.drawRoundRect(RectF(leftMargin, currentY, rightMargin, currentY + subheaderH), 4f, 4f, bgPaint)

            val findingsHeaderTitle = "CATATAN TEMUAN SEKALIGUS BUKTI FOTO HASIL INSPEKSI (${reportData.findings.size} ITEM)"
            canvas.drawText(findingsHeaderTitle, leftMargin + 10f, currentY + 13.5f, sectionHeaderPaint)

            currentY += subheaderH + 8f

            // Loop untuk setiap item temuan & foto
            val cardH = 115f
            val photoW = 145f
            val photoH = 95f

            for (finding in reportData.findings) {
                // Periksa apakah masih cukup tempat di halaman saat ini
                if (currentY + cardH > bottomLimit) {
                    drawFooter(canvas, currentPageNumber)
                    pdfDocument.finishPage(currentPdfPage)

                    currentPageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                    currentPdfPage = pdfDocument.startPage(pageInfo)
                    canvas = currentPdfPage.canvas

                    // Running Header Halaman Lanjutan
                    bgPaint.color = Color.parseColor("#F1F5F9")
                    canvas.drawRect(RectF(leftMargin, topMargin, rightMargin, topMargin + 18f), bgPaint)
                    val runTitle = "LAPORAN HASIL INSPEKSI KENDARAAN • Mobil ${audit.noMobil} (${audit.platNomer})"
                    val runPaint = Paint().apply {
                        isAntiAlias = true
                        textSize = 8f
                        color = Color.parseColor("#475569")
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    canvas.drawText(runTitle, leftMargin + 8f, topMargin + 12f, runPaint)
                    val halStr = "Halaman $currentPageNumber"
                    canvas.drawText(halStr, rightMargin - runPaint.measureText(halStr) - 8f, topMargin + 12f, runPaint)

                    currentY = topMargin + 26f
                }

                // Render Card Temuan Sekaligus Foto
                val cardRect = RectF(leftMargin, currentY, rightMargin, currentY + cardH)
                bgPaint.color = Color.WHITE
                canvas.drawRoundRect(cardRect, 6f, 6f, bgPaint)

                val itemHasPoin = finding.score > 0
                strokePaint.strokeWidth = 1f
                strokePaint.color = if (itemHasPoin) Color.parseColor("#FECACA") else Color.parseColor("#CBD5E1")
                canvas.drawRoundRect(cardRect, 6f, 6f, strokePaint)

                // SISI KIRI: Foto Bukti Temuan
                val photoLeft = leftMargin + 10f
                val photoTop = currentY + 10f
                val photoRect = RectF(photoLeft, photoTop, photoLeft + photoW, photoTop + photoH)

                val photoBitmap = finding.photoPath?.let { loadedBitmaps[it] }

                if (photoBitmap != null) {
                    // Gambar thumbnail foto dengan scaling proporsional
                    drawScaledBitmap(canvas, photoBitmap, photoRect)
                    strokePaint.strokeWidth = 0.8f
                    strokePaint.color = Color.parseColor("#94A3B8")
                    canvas.drawRoundRect(photoRect, 4f, 4f, strokePaint)
                } else {
                    // Placeholder jika tidak ada foto
                    bgPaint.color = Color.parseColor("#F1F5F9")
                    canvas.drawRoundRect(photoRect, 4f, 4f, bgPaint)
                    strokePaint.strokeWidth = 0.8f
                    strokePaint.color = Color.parseColor("#CBD5E1")
                    canvas.drawRoundRect(photoRect, 4f, 4f, strokePaint)

                    val noPhotoPaint = Paint().apply {
                        isAntiAlias = true
                        textSize = 8f
                        textAlign = Paint.Align.CENTER
                        color = Color.parseColor("#94A3B8")
                    }
                    val placeholderText = if (finding.score == 0) "Tidak Diperlukan Foto\n(Kondisi Normal)" else "Foto Belum Terlampir"
                    val pLayout = StaticLayout.Builder.obtain(placeholderText, 0, placeholderText.length, TextPaint(noPhotoPaint), photoW.toInt())
                        .setAlignment(Layout.Alignment.ALIGN_CENTER)
                        .build()
                    canvas.save()
                    canvas.translate(photoLeft, photoTop + (photoH / 2f) - 10f)
                    pLayout.draw(canvas)
                    canvas.restore()
                }

                // SISI KANAN (Mulai dari x = photoLeft + photoW + 12f): Catatan Temuan & Rincian Titik
                val textLeft = photoLeft + photoW + 12f
                val textWidth = rightMargin - textLeft - 10f

                var cY = photoTop + 3f

                // Header Baris 1: Nomor & Judul Item
                val itemTitleText = "${finding.index}. [${finding.category}] ${finding.title}"
                val itemTitlePaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 9.5f
                    color = Color.parseColor("#0F172A")
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText(itemTitleText, textLeft, cY + 8f, itemTitlePaint)

                // Header Baris 2: Badge Skor / Poin
                cY += 16f
                val badgeScoreText = if (finding.score > 0) "${finding.score} POIN TEMUAN" else "NORMAL (0 POIN)"
                val badgeBgColor = if (finding.score > 0) Color.parseColor("#FEF2F2") else Color.parseColor("#F0FDF4")
                val badgeBorderColor = if (finding.score > 0) Color.parseColor("#F87171") else Color.parseColor("#86EFAC")
                val badgeTextColor = if (finding.score > 0) Color.parseColor("#DC2626") else Color.parseColor("#16A34A")

                val badgePaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 7.5f
                    color = badgeTextColor
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val badgeTextW = badgePaint.measureText(badgeScoreText)
                val badgeRect = RectF(textLeft, cY, textLeft + badgeTextW + 12f, cY + 15f)

                bgPaint.color = badgeBgColor
                canvas.drawRoundRect(badgeRect, 3f, 3f, bgPaint)
                strokePaint.strokeWidth = 0.5f
                strokePaint.color = badgeBorderColor
                canvas.drawRoundRect(badgeRect, 3f, 3f, strokePaint)
                canvas.drawText(badgeScoreText, textLeft + 6f, cY + 10.5f, badgePaint)

                // Header Baris 3: Kotak Catatan Temuan
                cY += 20f
                val noteLabelPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 7.5f
                    color = Color.parseColor("#475569")
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("Catatan Temuan / Keterangan Auditor:", textLeft, cY + 7f, noteLabelPaint)

                cY += 11f
                val noteBoxH = 40f
                val noteRect = RectF(textLeft, cY, textLeft + textWidth, cY + noteBoxH)
                bgPaint.color = Color.parseColor("#FFFBEB") // Amber 50
                canvas.drawRoundRect(noteRect, 4f, 4f, bgPaint)
                strokePaint.strokeWidth = 0.5f
                strokePaint.color = Color.parseColor("#FDE68A") // Amber 200
                canvas.drawRoundRect(noteRect, 4f, 4f, strokePaint)

                // Teks Catatan Temuan yang ter-wrap rapi
                val noteContent = finding.note.ifBlank { "Tidak ada catatan khusus yang dicantumkan oleh auditor." }
                val noteTextPaint = TextPaint().apply {
                    isAntiAlias = true
                    textSize = 7.5f
                    color = Color.parseColor("#78350F") // Amber 900
                }
                val noteLayout = StaticLayout.Builder.obtain(noteContent, 0, noteContent.length, noteTextPaint, (textWidth - 12f).toInt())
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setMaxLines(3)
                    .build()

                canvas.save()
                canvas.translate(textLeft + 6f, cY + 4f)
                noteLayout.draw(canvas)
                canvas.restore()

                currentY += cardH + 8f
            }

            // 4. LEMBAR PENGESAHAN / TANDA TANGAN (Di halaman terakhir)
            val signH = 65f
            if (currentY + signH > bottomLimit) {
                drawFooter(canvas, currentPageNumber)
                pdfDocument.finishPage(currentPdfPage)

                currentPageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                currentPdfPage = pdfDocument.startPage(pageInfo)
                canvas = currentPdfPage.canvas
                currentY = topMargin + 20f
            }

            val signY = currentY + 10f
            val signW = (printableWidth - 40f) / 2f

            val signTitlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 8f
                textAlign = Paint.Align.CENTER
                color = Color.parseColor("#334155")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val signSubPaint = Paint().apply {
                isAntiAlias = true
                textSize = 7.5f
                textAlign = Paint.Align.CENTER
                color = Color.parseColor("#64748B")
            }

            // Kolom Kiri: Kepala Bengkel / Pool
            val leftSignX = leftMargin + (signW / 2f)
            canvas.drawText("Mengetahui / Menerima,", leftSignX, signY, signTitlePaint)
            canvas.drawText("Kepala Bengkel / Pool ${audit.pool}", leftSignX, signY + 11f, signSubPaint)

            strokePaint.strokeWidth = 0.5f
            strokePaint.color = Color.parseColor("#94A3B8")
            canvas.drawLine(leftMargin + 20f, signY + 52f, leftMargin + signW - 20f, signY + 52f, strokePaint)
            canvas.drawText("( Tanda Tangan & Cap Pool )", leftSignX, signY + 62f, signSubPaint)

            // Kolom Kanan: Auditor Petugas Pemeriksa
            val rightSignX = rightMargin - (signW / 2f)
            canvas.drawText("Auditor / Petugas Pemeriksa,", rightSignX, signY, signTitlePaint)
            canvas.drawText("Bluebird KI SKT", rightSignX, signY + 11f, signSubPaint)

            canvas.drawLine(rightMargin - signW + 20f, signY + 52f, rightMargin - 20f, signY + 52f, strokePaint)
            val audName = audit.namaPetugas.ifBlank { "Auditor" }
            val audNip = if (audit.nip.isNotBlank()) " (NIP: ${audit.nip})" else ""
            canvas.drawText("( $audName$audNip )", rightSignX, signY + 62f, signTitlePaint)

            drawFooter(canvas, currentPageNumber)
            pdfDocument.finishPage(currentPdfPage)

            // Tulis dokumen PDF ke file cache
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Menggambar bitmap pada area RectF dengan menjaga aspect ratio dan pemotongan rapi (center crop).
     */
    private fun drawScaledBitmap(canvas: Canvas, bitmap: Bitmap, destRect: RectF) {
        val srcW = bitmap.width.toFloat()
        val srcH = bitmap.height.toFloat()
        val destW = destRect.width()
        val destH = destRect.height()

        val scale = maxOf(destW / srcW, destH / srcH)
        val scaledW = srcW * scale
        val scaledH = srcH * scale

        val cropLeft = (scaledW - destW) / (2f * scale)
        val cropTop = (scaledH - destH) / (2f * scale)
        val cropRight = cropLeft + (destW / scale)
        val cropBottom = cropTop + (destH / scale)

        val srcRect = Rect(
            cropLeft.toInt().coerceAtLeast(0),
            cropTop.toInt().coerceAtLeast(0),
            cropRight.toInt().coerceAtMost(bitmap.width),
            cropBottom.toInt().coerceAtMost(bitmap.height)
        )

        canvas.save()
        canvas.clipRect(destRect)
        canvas.drawBitmap(bitmap, srcRect, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
    }

    /**
     * Memuat Bitmap dari jalur lokal, URI, Base64, atau URL secara aman dengan inSampleSize untuk mencegah OOM.
     */
    private fun loadSampledBitmap(
        context: Context,
        photoPath: String,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        return try {
            val trimmed = photoPath.trim()
            if (trimmed.isBlank()) return null

            // 1. Base64
            if (trimmed.startsWith("data:image", ignoreCase = true)) {
                val base64Data = trimmed.substringAfter("base64,")
                val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
                options.inJustDecodeBounds = false
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }

            // 2. File lokal
            if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
                val cleanPath = trimmed.removePrefix("file://")
                val file = File(cleanPath)
                if (file.exists() && file.canRead()) {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, options)
                    options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
                    options.inJustDecodeBounds = false
                    return BitmapFactory.decodeFile(file.absolutePath, options)
                }

                // Cek Content URI
                if (trimmed.startsWith("content://")) {
                    val uri = Uri.parse(trimmed)
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null && bytes.isNotEmpty()) {
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                        options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
                        options.inJustDecodeBounds = false
                        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    }
                }
            }

            // 3. URL Web / Google Drive (Dengan timeout cepat agar non-blocking)
            val directUrl = DriveUrlHelper.toDownloadUrl(trimmed)
            if (directUrl.startsWith("http://", ignoreCase = true) || directUrl.startsWith("https://", ignoreCase = true)) {
                var conn: HttpURLConnection = URL(directUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 2000
                conn.readTimeout = 2500
                conn.instanceFollowRedirects = true
                conn.connect()

                var responseCode = conn.responseCode
                var redirectCount = 0
                while (responseCode in 300..399 && redirectCount < 2) {
                    val newLoc = conn.getHeaderField("Location") ?: break
                    conn.disconnect()
                    conn = URL(newLoc).openConnection() as HttpURLConnection
                    conn.connectTimeout = 2000
                    conn.readTimeout = 2500
                    conn.connect()
                    responseCode = conn.responseCode
                    redirectCount++
                }

                if (responseCode in 200..299) {
                    val bytes = conn.inputStream.use { it.readBytes() }
                    if (bytes.isNotEmpty()) {
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                        options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
                        options.inJustDecodeBounds = false
                        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    }
                }
            }

            null
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Memanggil dialog Print sistem Android secara langsung (Dapat mencetak via WiFi, Bluetooth, atau Simpan PDF).
     */
    fun printPdf(context: Context, pdfFile: File, documentName: String) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null) {
                val printAdapter = object : PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val pdi = PrintDocumentInfo.Builder(pdfFile.name)
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                            .build()
                        callback?.onLayoutFinished(pdi, true)
                    }

                    override fun onWrite(
                        pages: Array<out PageRange>?,
                        destination: ParcelFileDescriptor?,
                        cancellationSignal: CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        var input: FileInputStream? = null
                        var output: FileOutputStream? = null
                        try {
                            input = FileInputStream(pdfFile)
                            output = FileOutputStream(destination?.fileDescriptor)
                            val buf = ByteArray(16384)
                            var bytesRead: Int
                            while (input.read(buf).also { bytesRead = it } >= 0) {
                                if (cancellationSignal?.isCanceled == true) {
                                    callback?.onWriteCancelled()
                                    return
                                }
                                output.write(buf, 0, bytesRead)
                            }
                            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.message)
                        } finally {
                            try { input?.close() } catch (_: Exception) {}
                            try { output?.close() } catch (_: Exception) {}
                        }
                    }
                }

                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                    .build()

                printManager.print(documentName, printAdapter, printAttributes)
            } else {
                // Jika PrintManager tidak tersedia, fallback buka via chooser
                Toast.makeText(context, "Membuka dokumen PDF untuk dicetak...", Toast.LENGTH_SHORT).show()
                shareOrOpenPdf(context, pdfFile, isShareOnly = false)
            }
        } catch (e: Exception) {
            // Jika PrintManager gagal/crash di lingkungan emulator, fallback ke dialog sistem chooser
            Toast.makeText(context, "Membuka opsi cetak dokumen PDF...", Toast.LENGTH_SHORT).show()
            shareOrOpenPdf(context, pdfFile, isShareOnly = false)
        }
    }

    /**
     * Membagikan atau membuka file PDF melalui aplikasi eksternal (Google Drive PDF, Adobe Reader, WhatsApp, dll.)
     */
    fun shareOrOpenPdf(
        context: Context,
        pdfFile: File,
        isShareOnly: Boolean = false
    ) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            if (!isShareOnly) {
                // Coba buka dengan PDF Viewer default sistem
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(viewIntent, "Buka atau Cetak Dokumen PDF").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(chooser)
                    return
                } catch (ignored: Exception) {
                    // Fallback ke ACTION_SEND jika tidak ada activity ACTION_VIEW terdaftar
                }
            }

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, pdfFile.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(sendIntent, if (isShareOnly) "Bagikan Laporan PDF" else "Buka / Cetak Laporan PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka aplikasi: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Menyimpan file PDF ke folder Unduhan (Downloads) publik perangkat
     */
    fun saveToPublicDownloads(
        context: Context,
        srcFile: File,
        displayName: String
    ): Uri? {
        return try {
            val fileName = if (displayName.endsWith(".pdf", ignoreCase = true)) displayName else "$displayName.pdf"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
                resolver.openOutputStream(uri)?.use { out ->
                    srcFile.inputStream().use { input -> input.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val targetFile = File(downloadsDir, fileName)
                srcFile.inputStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Uri.fromFile(targetFile)
            }
        } catch (e: Exception) {
            null
        }
    }
}
