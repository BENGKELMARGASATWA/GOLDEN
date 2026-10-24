package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.DataAuditEntity
import com.example.data.model.KpiAuditSpec
import com.example.data.model.KpiItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ResumeExportMeta(
    val pool: String,
    val periode: String,
    val sample: String,
    val status: String,
    val totalUnit: Int,
    val totalStandar: Int,
    val totalTemuan: Int,
    val totalPoin: Int,
    val complianceRate: Int
)

sealed class ResumePdfRow {
    data class SectionHeader(val title: String) : ResumePdfRow()
    data class GroupHeader(val title: String) : ResumePdfRow()
    data class ItemRow(val item: KpiItem, val scores: List<Int>) : ResumePdfRow()
}

object ResumeExportHelper {

    fun parseKpiScores(json: String?): Map<String, Int> {
        if (json.isNullOrBlank() || json == "{}") return emptyMap()
        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, Int>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optInt(k, 0)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Generates CSV matching the on-screen Matrix Table format.
     */
    suspend fun generateResumeCsv(
        context: Context,
        audits: List<DataAuditEntity>,
        meta: ResumeExportMeta
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val file = File(dir, "Tabel_Resume_Audit_Armada_${dateStr}.csv")

            val parsedScores = audits.map { parseKpiScores(it.kpiScoresJson) }

            val csv = buildString {
                append('\uFEFF') // UTF-8 BOM for Microsoft Excel compatibility

                // Metadata Header block
                appendLine(csvRow("RESUME AUDIT KEINDAHAN ARMADA BLUEBIRD"))
                appendLine(csvRow("Waktu Cetak", SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())))
                appendLine(csvRow("Pool", meta.pool))
                appendLine(csvRow("Periode", meta.periode))
                appendLine(csvRow("Filter Jenis Sample", meta.sample))
                appendLine(csvRow("Filter Status", meta.status))
                appendLine(csvRow("Total Unit", meta.totalUnit.toString()))
                appendLine(csvRow("Lolos Standar (0 Poin)", "${meta.totalStandar} (${meta.complianceRate}%)"))
                appendLine(csvRow("Ada Temuan", meta.totalTemuan.toString()))
                appendLine(csvRow("Total Akumulasi Poin", meta.totalPoin.toString()))
                appendLine()

                // Matrix Column Headers: DATA CEK ALL vs Unit Mobil
                val unitHeaders = audits.mapIndexed { idx, _ -> "UNIT #${idx + 1}" }
                appendLine(csvRow(listOf("DATA CEK ALL") + unitHeaders))

                // Vehicle Info Rows (Susunan: [No MOBIL], [PLAT NOMER], [SAMPLE])
                appendLine(csvRow(listOf("NOMOR MOBIL") + audits.map { it.noMobil }))
                appendLine(csvRow(listOf("PLAT NOMOR") + audits.map { it.platNomer.ifBlank { "-" } }))
                appendLine(csvRow(listOf("SAMPLE") + audits.map { it.jenisSample }))
                appendLine(csvRow(listOf("TANGGAL AUDIT") + audits.map { it.tanggal }))
                appendLine(csvRow(listOf("PETUGAS AUDITOR") + audits.map { it.namaPetugas }))
                appendLine(csvRow(listOf("TOTAL POIN TEMUAN") + audits.map { "${it.totalPoinTemuan} Poin" }))
                appendLine()

                // Checklist Rows (Sections -> Groups -> Items)
                for (section in KpiAuditSpec.sections) {
                    appendLine(csvRow(listOf("=== ${section.title} ===")))
                    for (group in section.groups) {
                        appendLine(csvRow(listOf("• ${group.title}")))
                        for (item in group.items) {
                            val cells = mutableListOf<String>()
                            cells.add("${item.name} (Maks ${item.maxPoints} Poin)")
                            for (uIdx in audits.indices) {
                                val score = parsedScores[uIdx][item.id] ?: 0
                                cells.add(score.toString())
                            }
                            appendLine(csvRow(cells))
                        }
                    }
                    appendLine()
                }
            }

            file.writeText(csv, Charsets.UTF_8)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun csvRow(vararg cells: String): String = csvRow(cells.toList())

    private fun csvRow(cells: List<String>): String {
        return cells.joinToString(",") { cell ->
            val escaped = cell.replace("\"", "\"\"")
            if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
                "\"$escaped\""
            } else {
                escaped
            }
        }
    }

    /**
     * Generates a multi-page PDF document in A4 Landscape layout.
     * Contains standard Bluebird branding, vehicle headers on each page,
     * section dividers, and point values with color coding.
     */
    suspend fun generateResumePdf(
        context: Context,
        audits: List<DataAuditEntity>,
        meta: ResumeExportMeta
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val file = File(dir, "Tabel_Resume_Audit_Armada_${dateStr}.pdf")

            val parsedScores = audits.map { parseKpiScores(it.kpiScoresJson) }

            // Prepare flattened list of row items
            val allRows = mutableListOf<ResumePdfRow>()
            for (section in KpiAuditSpec.sections) {
                allRows.add(ResumePdfRow.SectionHeader(section.title))
                for (group in section.groups) {
                    allRows.add(ResumePdfRow.GroupHeader(group.title))
                    for (item in group.items) {
                        val scores = audits.indices.map { uIdx -> parsedScores[uIdx][item.id] ?: 0 }
                        allRows.add(ResumePdfRow.ItemRow(item, scores))
                    }
                }
            }

            val pageWidth = 842 // A4 Landscape width
            val pageHeight = 595 // A4 Landscape height
            val leftMargin = 20f
            val rightMargin = 822f
            val printWidth = rightMargin - leftMargin // 802f

            val col0Width = 230f
            val remainingWidth = printWidth - col0Width // 572f
            val carCount = maxOf(audits.size, 1)
            val carColWidth = remainingWidth / carCount

            val pdfDocument = PdfDocument()

            // Paint definitions
            val bgPaint = Paint().apply { isAntiAlias = true }
            val strokePaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 0.5f
                color = Color.parseColor("#CBD5E1")
            }
            val titlePaint = Paint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 12f
                color = Color.WHITE
            }
            val metaPaint = Paint().apply {
                isAntiAlias = true
                textSize = 7.5f
                color = Color.WHITE
            }
            val textDarkBold = Paint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 8f
                color = Color.parseColor("#0F172A")
            }
            val textDarkNormal = Paint().apply {
                isAntiAlias = true
                textSize = 7f
                color = Color.parseColor("#1E293B")
            }
            val textMuted = Paint().apply {
                isAntiAlias = true
                textSize = 6f
                color = Color.parseColor("#64748B")
            }
            val scoreZeroPaint = Paint().apply {
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                textSize = 7.5f
                color = Color.parseColor("#16A34A")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val scoreTemuanPaint = Paint().apply {
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                textSize = 7.5f
                color = Color.parseColor("#B45309")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val footerPaint = Paint().apply {
                isAntiAlias = true
                textSize = 7f
                color = Color.parseColor("#94A3B8")
            }

            fun getRowHeight(row: ResumePdfRow): Float = when (row) {
                is ResumePdfRow.SectionHeader -> 16f
                is ResumePdfRow.GroupHeader -> 13f
                is ResumePdfRow.ItemRow -> 12f
            }

            // Estimate total pages by simulation
            val maxY = 565f
            var simPageCount = 1
            var simY = 145f
            for (row in allRows) {
                val rh = getRowHeight(row)
                if (simY + rh > maxY) {
                    simPageCount++
                    simY = 125f // Next page starts lower due to repeat vehicle header
                }
                simY += rh
            }

            var currentPage = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPage).create()
            var currentPdfPage = pdfDocument.startPage(pageInfo)
            var canvas = currentPdfPage.canvas

            fun drawPageBanner(pageCanvas: Canvas, pageNum: Int) {
                // Main Header Banner (Corporate Bluebird Blue #005691)
                bgPaint.color = Color.parseColor("#005691")
                pageCanvas.drawRect(leftMargin, 12f, rightMargin, 46f, bgPaint)

                // Title
                titlePaint.textSize = 12f
                pageCanvas.drawText("RESUME AUDIT KEINDAHAN ARMADA — PT BLUE BIRD TBK", leftMargin + 10f, 28f, titlePaint)

                // Subtitle metadata strip
                val metaText = "Pool: ${meta.pool}  |  Periode: ${meta.periode}  |  Sample: ${meta.sample}  |  Unit: ${meta.totalUnit}  |  Lolos: ${meta.totalStandar} (${meta.complianceRate}%)  |  Temuan: ${meta.totalTemuan}  |  Total Poin: ${meta.totalPoin}"
                pageCanvas.drawText(metaText, leftMargin + 10f, 40f, metaPaint)

                val datePrint = "Dicetak: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}"
                val dateWidth = metaPaint.measureText(datePrint)
                pageCanvas.drawText(datePrint, rightMargin - dateWidth - 10f, 28f, metaPaint)

                val pageTag = "Halaman $pageNum dari $simPageCount"
                val tagWidth = metaPaint.measureText(pageTag)
                pageCanvas.drawText(pageTag, rightMargin - tagWidth - 10f, 40f, metaPaint)
            }

            fun drawTableHeader(pageCanvas: Canvas, startY: Float): Float {
                var y = startY
                val colHeight = 65f

                // Header Background (Dark Slate/Navy #1E293B)
                bgPaint.color = Color.parseColor("#1E293B")
                pageCanvas.drawRect(leftMargin, y, rightMargin, y + colHeight, bgPaint)

                // Column 0: DATA CEK ALL
                val c0Paint = Paint().apply {
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = 9.5f
                    color = Color.WHITE
                }
                pageCanvas.drawText("DATA CEK ALL", leftMargin + 10f, y + 22f, c0Paint)

                val c0SubPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 7f
                    color = Color.parseColor("#94A3B8")
                }
                pageCanvas.drawText("Pemeriksaan KPI Audit", leftMargin + 10f, y + 36f, c0SubPaint)
                pageCanvas.drawText("Sample", leftMargin + 10f, y + 48f, c0SubPaint)
                pageCanvas.drawText("Total Poin Temuan", leftMargin + 10f, y + 60f, c0SubPaint)

                // Vehicle Columns
                audits.forEachIndexed { idx, audit ->
                    val xStart = leftMargin + col0Width + (idx * carColWidth)
                    val xCenter = xStart + (carColWidth / 2f)

                    // Unit Tag
                    val uTagPaint = Paint().apply {
                        isAntiAlias = true
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textSize = 6.5f
                        color = Color.parseColor("#38BDF8")
                        textAlign = Paint.Align.CENTER
                    }
                    pageCanvas.drawText("UNIT #${idx + 1}", xCenter, y + 14f, uTagPaint)

                    // No Mobil
                    val noMobilPaint = Paint().apply {
                        isAntiAlias = true
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textSize = 9.5f
                        color = Color.WHITE
                        textAlign = Paint.Align.CENTER
                    }
                    pageCanvas.drawText(audit.noMobil, xCenter, y + 26f, noMobilPaint)

                    // Plat Nomer
                    val platPaint = Paint().apply {
                        isAntiAlias = true
                        textSize = 7f
                        color = Color.parseColor("#CBD5E1")
                        textAlign = Paint.Align.CENTER
                    }
                    pageCanvas.drawText(audit.platNomer.ifBlank { "-" }, xCenter, y + 36f, platPaint)

                    // Sample Badge (OPERASIONAL / SERVICE)
                    val sampleLabel = audit.jenisSample.ifBlank { "-" }.uppercase()
                    val isOp = sampleLabel == "OPERASIONAL"
                    val badgeW = (carColWidth - 8f).coerceAtLeast(20f)
                    val badgeH = 12f
                    val badgeRect = RectF(xCenter - (badgeW / 2f), y + 40f, xCenter + (badgeW / 2f), y + 40f + badgeH)
                    val badgeBg = Paint().apply {
                        isAntiAlias = true
                        color = if (isOp) Color.parseColor("#0284C7") else Color.parseColor("#0D9488")
                    }
                    pageCanvas.drawRoundRect(badgeRect, 3f, 3f, badgeBg)

                    val badgeTextPaint = Paint().apply {
                        isAntiAlias = true
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textSize = if (carColWidth < 50f) 5.5f else 6.5f
                        color = Color.WHITE
                        textAlign = Paint.Align.CENTER
                    }
                    pageCanvas.drawText(sampleLabel, xCenter, y + 48.5f, badgeTextPaint)

                    // Total Points Text
                    val isZero = audit.totalPoinTemuan == 0
                    val pointsPaint = Paint().apply {
                        isAntiAlias = true
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textSize = 7f
                        color = if (isZero) Color.parseColor("#4ADE80") else Color.parseColor("#FCA5A5")
                        textAlign = Paint.Align.CENTER
                    }
                    pageCanvas.drawText("${audit.totalPoinTemuan} Poin", xCenter, y + 60f, pointsPaint)

                    // Divider vertical between vehicle columns
                    pageCanvas.drawLine(xStart, y, xStart, y + colHeight, strokePaint)
                }

                // Vertical line between Col 0 and first vehicle column
                pageCanvas.drawLine(leftMargin + col0Width, y, leftMargin + col0Width, y + colHeight, strokePaint)

                // Border around entire table header
                pageCanvas.drawRect(leftMargin, y, rightMargin, y + colHeight, strokePaint)

                return y + colHeight
            }

            fun drawFooter(pageCanvas: Canvas) {
                strokePaint.color = Color.parseColor("#E2E8F0")
                pageCanvas.drawLine(leftMargin, 570f, rightMargin, 570f, strokePaint)

                footerPaint.textAlign = Paint.Align.LEFT
                pageCanvas.drawText("Sistem Audit Armada Bluebird • Dokumen Resmi Hasil Pemeriksaan Fisik Kendaraan", leftMargin, 582f, footerPaint)

                footerPaint.textAlign = Paint.Align.RIGHT
                pageCanvas.drawText("Halaman $currentPage dari $simPageCount", rightMargin, 582f, footerPaint)
            }

            // Draw first page
            drawPageBanner(canvas, currentPage)
            var currentY = drawTableHeader(canvas, 50f)

            for (rowIndex in allRows.indices) {
                val row = allRows[rowIndex]
                val rowH = getRowHeight(row)

                // Check pagination
                if (currentY + rowH > maxY) {
                    drawFooter(canvas)
                    pdfDocument.finishPage(currentPdfPage)

                    currentPage++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPage).create()
                    currentPdfPage = pdfDocument.startPage(pageInfo)
                    canvas = currentPdfPage.canvas

                    drawPageBanner(canvas, currentPage)
                    currentY = drawTableHeader(canvas, 50f)
                }

                when (row) {
                    is ResumePdfRow.SectionHeader -> {
                        // Section Header Row (#005691 Blue)
                        bgPaint.color = Color.parseColor("#0284C7")
                        canvas.drawRect(leftMargin, currentY, rightMargin, currentY + rowH, bgPaint)

                        val secPaint = Paint().apply {
                            isAntiAlias = true
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            textSize = 8f
                            color = Color.WHITE
                        }
                        canvas.drawText(row.title.uppercase(), leftMargin + 8f, currentY + 11.5f, secPaint)
                    }

                    is ResumePdfRow.GroupHeader -> {
                        // Group Subheader Row (#F1F5F9 Slate)
                        bgPaint.color = Color.parseColor("#F1F5F9")
                        canvas.drawRect(leftMargin, currentY, rightMargin, currentY + rowH, bgPaint)

                        val grpPaint = Paint().apply {
                            isAntiAlias = true
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            textSize = 7.5f
                            color = Color.parseColor("#334155")
                        }
                        canvas.drawText("• ${row.title}", leftMargin + 14f, currentY + 9.5f, grpPaint)
                    }

                    is ResumePdfRow.ItemRow -> {
                        val isEven = rowIndex % 2 == 0
                        bgPaint.color = if (isEven) Color.WHITE else Color.parseColor("#F8FAFC")
                        canvas.drawRect(leftMargin, currentY, rightMargin, currentY + rowH, bgPaint)

                        // Item name and max points
                        val itemName = row.item.name
                        canvas.drawText(itemName, leftMargin + 20f, currentY + 8.5f, textDarkNormal)
                        canvas.drawText("(${row.item.maxPoints} Pt)", leftMargin + 195f, currentY + 8.5f, textMuted)

                        // Scores for each vehicle
                        row.scores.forEachIndexed { uIdx, score ->
                            val xStart = leftMargin + col0Width + (uIdx * carColWidth)
                            val xCenter = xStart + (carColWidth / 2f)

                            if (score == 0) {
                                canvas.drawText("0", xCenter, currentY + 8.5f, scoreZeroPaint)
                            } else {
                                // Subtle highlight for temuan score
                                val scoreBg = RectF(xCenter - 10f, currentY + 1.5f, xCenter + 10f, currentY + rowH - 1.5f)
                                bgPaint.color = Color.parseColor("#FEF3C7")
                                canvas.drawRoundRect(scoreBg, 2f, 2f, bgPaint)

                                canvas.drawText(score.toString(), xCenter, currentY + 8.5f, scoreTemuanPaint)
                            }

                            // Vertical column separator
                            strokePaint.color = Color.parseColor("#E2E8F0")
                            canvas.drawLine(xStart, currentY, xStart, currentY + rowH, strokePaint)
                        }

                        // Vertical separator between Col 0 and car columns
                        strokePaint.color = Color.parseColor("#CBD5E1")
                        canvas.drawLine(leftMargin + col0Width, currentY, leftMargin + col0Width, currentY + rowH, strokePaint)

                        // Horizontal line below row
                        strokePaint.color = Color.parseColor("#F1F5F9")
                        canvas.drawLine(leftMargin, currentY + rowH, rightMargin, currentY + rowH, strokePaint)
                    }
                }

                currentY += rowH
            }

            // Outer border for the bottom table section
            strokePaint.color = Color.parseColor("#CBD5E1")
            canvas.drawRect(leftMargin, 50f, rightMargin, currentY, strokePaint)

            drawFooter(canvas)
            pdfDocument.finishPage(currentPdfPage)

            // Write PDF to cache file
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
     * Saves file into the Android device's public Downloads directory.
     */
    fun saveToPublicDownloads(
        context: Context,
        srcFile: File,
        mimeType: String,
        displayName: String
    ): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
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
                val targetFile = File(downloadsDir, displayName)
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

    /**
     * Opens or shares the downloaded file via Android native chooser.
     */
    fun shareOrOpenFile(
        context: Context,
        file: File,
        mimeType: String,
        chooserTitle: String
    ) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, chooserTitle))
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka aplikasi: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
