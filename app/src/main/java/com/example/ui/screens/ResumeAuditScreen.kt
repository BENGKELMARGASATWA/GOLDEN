package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DataAuditEntity
import com.example.data.local.entity.MasterPeriodeEntity
import com.example.data.local.entity.MasterPoolEntity
import com.example.data.local.entity.PengaturanNamaPhotoEntity
import com.example.data.local.entity.UserEntity
import com.example.util.AuditFilterUtils
import com.example.ui.components.PhotoDetailDialog
import com.example.ui.theme.Amber50
import com.example.ui.theme.Amber600
import com.example.ui.theme.HighDensityBg
import com.example.ui.theme.HighDensityBlue
import com.example.ui.theme.HighDensityBlueBorder
import com.example.ui.theme.HighDensityBlueLight
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Teal50
import com.example.ui.theme.Teal600
import com.example.util.ResumeExportHelper
import com.example.util.ResumeExportMeta
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeAuditScreen(
    audits: List<DataAuditEntity>,
    periodes: List<MasterPeriodeEntity>,
    pools: List<MasterPoolEntity>,
    photoTitles: List<PengaturanNamaPhotoEntity>,
    currentUser: UserEntity?,
    onDeleteAudit: (Long) -> Unit = {},
    onEditAudit: (DataAuditEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userLevel = currentUser?.levelAkses?.trim()?.uppercase() ?: ""
    val userPool = currentUser?.kodePool?.trim().orEmpty()
    val isPoolRestricted = userPool.isNotBlank() && !userPool.equals("SKT", ignoreCase = true) && !userPool.equals("ALL", ignoreCase = true) &&
        (userLevel == "KEPALA BENGKEL" || userLevel.contains("KABAG") || userLevel == "AUDITOR" || (userLevel != "SUPER ADMIN" && userLevel != "ADMIN" && userLevel != "SKT" && userLevel != "MASTER DATA"))

    val userPoolList = remember(userPool) {
        userPool.split(",", ";").map { it.trim().uppercase() }.filter { it.isNotBlank() }
    }
    val isSinglePoolLocked = isPoolRestricted && userPoolList.size == 1

    var selectedSampleFilter by remember { mutableStateOf("SEMUA") }
    var selectedPoolFilter by remember(currentUser) {
        mutableStateOf(if (isSinglePoolLocked) userPoolList.first() else "SEMUA")
    }
    var selectedPeriodeFilter by remember { mutableStateOf("SEMUA") }

    var sampleExpanded by remember { mutableStateOf(false) }
    var poolExpanded by remember { mutableStateOf(false) }
    var periodeExpanded by remember { mutableStateOf(false) }

    var selectedAuditForModal by remember { mutableStateOf<DataAuditEntity?>(null) }
    var isExportingPdf by remember { mutableStateOf(false) }
    var isExportingCsv by remember { mutableStateOf(false) }

    // Options for Dropdowns: Sample | POOL | PERIODE
    val sampleOptions = remember(audits) {
        val list = mutableListOf("SEMUA", "OPERASIONAL", "SERVICE")
        audits.forEach { a ->
            val s = a.jenisSample.trim().uppercase()
            if (s.isNotBlank() && !list.contains(s)) {
                list.add(s)
            }
        }
        list
    }

    val poolOptions = remember(pools, audits, isPoolRestricted, userPoolList) {
        if (isPoolRestricted && userPoolList.isNotEmpty()) {
            val list = mutableListOf<String>()
            if (userPoolList.size > 1) {
                list.add("SEMUA")
            }
            list.addAll(userPoolList)
            list.distinct()
        } else {
            val list = mutableListOf("SEMUA")
            pools.forEach { p ->
                val k = p.kodePool.trim().uppercase()
                if (k.isNotBlank() && !list.contains(k)) {
                    list.add(k)
                }
            }
            audits.forEach { a ->
                val k = a.pool.trim().uppercase()
                if (k.isNotBlank() && !list.contains(k)) {
                    list.add(k)
                }
            }
            list
        }
    }

    val periodeOptions = remember(periodes, audits) {
        val list = mutableListOf("SEMUA")
        periodes.forEach { prd ->
            val p = prd.periode.trim()
            if (p.isNotBlank() && !list.contains(p)) {
                list.add(p)
            }
        }
        audits.forEach { a ->
            val p = a.periode.trim()
            if (p.isNotBlank() && !list.contains(p)) {
                list.add(p)
            }
        }
        list
    }

    // Filter audits
    // Sesuai aturan: Pada menu RESUME AUDIT KEINDAHAN, data unit diambil dari data riwayat audit
    // (jika terdapat no mobil yang sama pada satu periode, maka hanya ditampilkan pada input terakhir).
    val filteredAudits = remember(audits, selectedPeriodeFilter, selectedPoolFilter, selectedSampleFilter, isPoolRestricted, userPool, userPoolList) {
        val filtered = audits.filter { audit ->
            val matchPool = if (isPoolRestricted && userPool.isNotBlank()) {
                if (userPoolList.size > 1) {
                    if (selectedPoolFilter == "SEMUA" || selectedPoolFilter.isBlank()) {
                        userPoolList.any { it.equals(audit.pool.trim(), ignoreCase = true) }
                    } else {
                        audit.pool.equals(selectedPoolFilter, ignoreCase = true)
                    }
                } else {
                    audit.pool.equals(userPool, ignoreCase = true)
                }
            } else if (selectedPoolFilter == "SEMUA" || selectedPoolFilter.isBlank()) {
                true
            } else {
                audit.pool.equals(selectedPoolFilter, ignoreCase = true)
            }

            val matchPeriode = if (selectedPeriodeFilter == "SEMUA" || selectedPeriodeFilter.isBlank()) {
                true
            } else {
                audit.periode.equals(selectedPeriodeFilter, ignoreCase = true)
            }

            val matchSample = when (selectedSampleFilter.uppercase()) {
                "OPERASIONAL" -> audit.jenisSample.equals("OPERASIONAL", ignoreCase = true)
                "SERVICE" -> audit.jenisSample.equals("SERVICE", ignoreCase = true)
                "SEMUA", "" -> true
                else -> audit.jenisSample.equals(selectedSampleFilter, ignoreCase = true)
            }

            matchPool && matchPeriode && matchSample
        }
        val deduped = AuditFilterUtils.filterUniqueAudits(filtered)
        AuditFilterUtils.filterLatestPerCarAndPeriod(deduped)
    }

    // Metrics calculations
    val totalUnit = filteredAudits.size
    val totalStandar = filteredAudits.count { it.totalPoinTemuan == 0 }
    val totalTemuanUnit = filteredAudits.count { it.totalPoinTemuan > 0 }
    val totalPoinAkumulasi = filteredAudits.sumOf { it.totalPoinTemuan }
    val complianceRate = if (totalUnit > 0) (totalStandar * 100) / totalUnit else 100
    val totalOperasional = filteredAudits.count { it.jenisSample.equals("OPERASIONAL", ignoreCase = true) }
    val totalService = filteredAudits.count { it.jenisSample.equals("SERVICE", ignoreCase = true) }

    val exportMeta = remember(
        selectedPoolFilter,
        selectedPeriodeFilter,
        selectedSampleFilter,
        totalUnit,
        totalStandar,
        totalTemuanUnit,
        totalPoinAkumulasi,
        complianceRate
    ) {
        ResumeExportMeta(
            pool = if (selectedPoolFilter == "SEMUA") "SEMUA POOL" else selectedPoolFilter,
            periode = if (selectedPeriodeFilter == "SEMUA") "SEMUA PERIODE" else selectedPeriodeFilter,
            sample = if (selectedSampleFilter == "SEMUA") "SEMUA SAMPLE" else selectedSampleFilter,
            status = "SEMUA",
            totalUnit = totalUnit,
            totalStandar = totalStandar,
            totalTemuan = totalTemuanUnit,
            totalPoin = totalPoinAkumulasi,
            complianceRate = complianceRate
        )
    }

    fun downloadTablePdf(auditsToExport: List<DataAuditEntity>) {
        if (auditsToExport.isEmpty()) {
            Toast.makeText(context, "Tidak ada data armada untuk diunduh", Toast.LENGTH_SHORT).show()
            return
        }
        coroutineScope.launch {
            isExportingPdf = true
            Toast.makeText(context, "Sedang memproses dokumen PDF...", Toast.LENGTH_SHORT).show()
            val result = ResumeExportHelper.generateResumePdf(context, auditsToExport, exportMeta)
            isExportingPdf = false
            result.onSuccess { file ->
                val savedUri = ResumeExportHelper.saveToPublicDownloads(
                    context = context,
                    srcFile = file,
                    mimeType = "application/pdf",
                    displayName = file.name
                )
                val notif = if (savedUri != null) {
                    "File PDF berhasil disimpan ke folder Downloads: ${file.name}"
                } else {
                    "File PDF berhasil dibuat: ${file.name}"
                }
                Toast.makeText(context, notif, Toast.LENGTH_LONG).show()
                ResumeExportHelper.shareOrOpenFile(context, file, "application/pdf", "Buka atau Cetak PDF Resume Audit")
            }.onFailure { err ->
                Toast.makeText(context, "Gagal membuat file PDF: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun downloadTableCsv(auditsToExport: List<DataAuditEntity>) {
        if (auditsToExport.isEmpty()) {
            Toast.makeText(context, "Tidak ada data armada untuk diunduh", Toast.LENGTH_SHORT).show()
            return
        }
        coroutineScope.launch {
            isExportingCsv = true
            Toast.makeText(context, "Sedang menyiapkan file CSV...", Toast.LENGTH_SHORT).show()
            val result = ResumeExportHelper.generateResumeCsv(context, auditsToExport, exportMeta)
            isExportingCsv = false
            result.onSuccess { file ->
                val savedUri = ResumeExportHelper.saveToPublicDownloads(
                    context = context,
                    srcFile = file,
                    mimeType = "text/csv",
                    displayName = file.name
                )
                val notif = if (savedUri != null) {
                    "File CSV berhasil disimpan ke folder Downloads: ${file.name}"
                } else {
                    "File CSV berhasil dibuat: ${file.name}"
                }
                Toast.makeText(context, notif, Toast.LENGTH_LONG).show()
                ResumeExportHelper.shareOrOpenFile(context, file, "text/csv", "Buka dengan Excel atau Bagikan CSV")
            }.onFailure { err ->
                Toast.makeText(context, "Gagal membuat file CSV: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun copySummaryText() {
        val summaryText = buildString {
            appendLine("=== RESUME AUDIT KEINDAHAN ARMADA ===")
            appendLine("Pool: ${if (selectedPoolFilter == "SEMUA") "SEMUA POOL" else selectedPoolFilter}")
            appendLine("Periode: $selectedPeriodeFilter")
            appendLine("Total Unit Diaudit: $totalUnit")
            appendLine("Lolos Standar (0 Poin): $totalStandar ($complianceRate%)")
            appendLine("Ada Temuan: $totalTemuanUnit")
            appendLine("Total Akumulasi Poin: $totalPoinAkumulasi Poin")
            appendLine("Sample Operasional: $totalOperasional | Sample Service: $totalService")
            appendLine("=====================================")
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Resume Audit", summaryText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Ringkasan resume berhasil disalin ke clipboard!", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HighDensityBg)
            .padding(14.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Title Card with Copy Action
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = HighDensityBlueLight,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Summarize,
                                            contentDescription = null,
                                            tint = HighDensityBlue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "RESUME AUDIT KEINDAHAN",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Slate900,
                                            letterSpacing = (-0.3).sp
                                        )
                                    )
                                    Text(
                                        text = if (isPoolRestricted) "Rekapitulasi Audit Pool $userPool" else "Rekapitulasi Keseluruhan Audit Armada",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600, fontSize = 11.sp)
                                    )
                                }
                            }

                            Button(
                                onClick = { copySummaryText() },
                                colors = ButtonDefaults.buttonColors(containerColor = HighDensityBlue),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Salin Resume", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Filter Dropdown: Sample | POOL | PERIODE
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                tint = HighDensityBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FILTER AUDIT",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Slate900,
                                    fontSize = 11.5.sp
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Filter Dropdown Sample
                            ExposedDropdownMenuBox(
                                expanded = sampleExpanded,
                                onExpandedChange = { sampleExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = if (selectedSampleFilter == "SEMUA") "Semua" else selectedSampleFilter,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Sample", fontSize = 10.sp) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sampleExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = HighDensityBlue,
                                        unfocusedBorderColor = Slate300
                                    ),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = sampleExpanded,
                                    onDismissRequest = { sampleExpanded = false }
                                ) {
                                    sampleOptions.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(if (opt == "SEMUA") "Semua Sample" else opt, fontSize = 12.sp) },
                                            onClick = {
                                                selectedSampleFilter = opt
                                                sampleExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // 2. Filter Dropdown POOL
                            ExposedDropdownMenuBox(
                                expanded = if (isSinglePoolLocked) false else poolExpanded,
                                onExpandedChange = { if (!isSinglePoolLocked) poolExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                val poolDisplayText = if (isSinglePoolLocked) {
                                    userPoolList.first()
                                } else if (selectedPoolFilter == "SEMUA") {
                                    "Semua"
                                } else {
                                    selectedPoolFilter
                                }
                                OutlinedTextField(
                                    value = poolDisplayText,
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = !isSinglePoolLocked,
                                    label = { Text("POOL", fontSize = 10.sp) },
                                    trailingIcon = {
                                        if (isSinglePoolLocked) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Terkunci pada Pool $userPool",
                                                tint = Slate500,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = poolExpanded)
                                        }
                                    },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = HighDensityBlue,
                                        unfocusedBorderColor = Slate300,
                                        disabledBorderColor = Slate300,
                                        disabledTextColor = Color(0xFF0F172A),
                                        disabledLabelColor = Slate500,
                                        disabledContainerColor = Color(0xFFF1F5F9)
                                    ),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                    singleLine = true
                                )
                                if (!isSinglePoolLocked) {
                                    ExposedDropdownMenu(
                                        expanded = poolExpanded,
                                        onDismissRequest = { poolExpanded = false }
                                    ) {
                                        poolOptions.forEach { pl ->
                                            val label = if (pl == "SEMUA") {
                                                if (isPoolRestricted && userPoolList.size > 1) "Semua Pool Saya" else "Semua Pool"
                                            } else pl
                                            DropdownMenuItem(
                                                text = { Text(label, fontSize = 12.sp) },
                                                onClick = {
                                                    selectedPoolFilter = pl
                                                    poolExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. Filter Dropdown PERIODE
                            ExposedDropdownMenuBox(
                                expanded = periodeExpanded,
                                onExpandedChange = { periodeExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = if (selectedPeriodeFilter == "SEMUA") "Semua" else selectedPeriodeFilter,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("PERIODE", fontSize = 10.sp) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodeExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = HighDensityBlue,
                                        unfocusedBorderColor = Slate300
                                    ),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = periodeExpanded,
                                    onDismissRequest = { periodeExpanded = false }
                                ) {
                                    periodeOptions.forEach { prd ->
                                        DropdownMenuItem(
                                            text = { Text(if (prd == "SEMUA") "Semua Periode" else prd, fontSize = 12.sp) },
                                            onClick = {
                                                selectedPeriodeFilter = prd
                                                periodeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Form Tabel Resume Matrix: Data Cek All di kiri & point setiap unit baru di kanan (Max 14 Mobil)
            item {
                val tableAudits = remember(filteredAudits) { filteredAudits.take(14) }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "TABEL RESUME AUDIT ARMADA",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Slate900,
                                        letterSpacing = (-0.2).sp
                                    )
                                )
                                Text(
                                    text = "Data Cek All di sebelah kiri, point unit di kanan (Maks 14 mobil)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Slate500)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                color = HighDensityBlueLight,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${tableAudits.size}/14 Unit",
                                    color = HighDensityBlue,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Download Action Buttons: Unduh Tabel PDF & Unduh Tabel CSV
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { downloadTablePdf(tableAudits) },
                                enabled = !isExportingPdf && tableAudits.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFDC2626),
                                    disabledContainerColor = Color(0xFFFCA5A5)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("btn_unduh_tabel_pdf")
                            ) {
                                if (isExportingPdf) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Membuat PDF...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Unduh PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { downloadTableCsv(tableAudits) },
                                enabled = !isExportingCsv && tableAudits.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF16A34A),
                                    disabledContainerColor = Color(0xFF86EFAC)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("btn_unduh_tabel_csv")
                            ) {
                                if (isExportingCsv) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Membuat CSV...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.TableChart,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Unduh CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Option when filtered audits exceed 14 cars
                        if (filteredAudits.size > 14) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Seluruh ${filteredAudits.size} Unit Terfilter:",
                                        fontSize = 10.5.sp,
                                        color = Slate600,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Color(0xFFDC2626)),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier
                                                .clickable(enabled = !isExportingPdf) { downloadTablePdf(filteredAudits) }
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text("Semua (${filteredAudits.size}) PDF", fontSize = 9.5.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                                        }

                                        Surface(
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Color(0xFF16A34A)),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier
                                                .clickable(enabled = !isExportingCsv) { downloadTableCsv(filteredAudits) }
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text("Semua (${filteredAudits.size}) CSV", fontSize = 9.5.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Slate200)
                        Spacer(modifier = Modifier.height(10.dp))

                        if (tableAudits.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Assessment, contentDescription = null, tint = Slate400, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Tidak ada data audit yang sesuai dengan filter",
                                    color = Slate600,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            // Container horizontal scroll untuk tabel matriks (dibuat lebih rapat & rapi)
                            val horizontalScroll = rememberScrollState()
                            val parsedScoresList = remember(tableAudits) {
                                tableAudits.map { parseKpiScores(it.kpiScoresJson) }
                            }

                            val colLeftWidth = 190.dp
                            val colUnitWidth = 84.dp

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Slate300, RoundedCornerShape(8.dp))
                                    .horizontalScroll(horizontalScroll)
                            ) {
                                Column {
                                    // 1. HEADER ROW: DATA CEK ALL vs UNIT MOBIL
                                    Row(
                                        modifier = Modifier
                                            .background(HighDensityBlue)
                                            .padding(vertical = 8.dp)
                                    ) {
                                        // Kolom Kiri Header
                                        Box(
                                            modifier = Modifier
                                                .width(colLeftWidth)
                                                .padding(horizontal = 10.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Text(
                                                text = "DATA CEK ALL",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }

                                        // Kolom Kanan Header (Setiap Unit Mobil)
                                        // Susunan: [No MOBIL], [PLAT NOMER], [SAMPLE]
                                        tableAudits.forEachIndexed { index, audit ->
                                            Box(
                                                modifier = Modifier
                                                    .width(colUnitWidth)
                                                    .padding(horizontal = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Surface(
                                                        color = Color.White.copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(3.dp)
                                                    ) {
                                                        Text(
                                                            text = "#${index + 1}",
                                                            color = Color.White,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontSize = 8.5.sp,
                                                                fontWeight = FontWeight.Bold
                                                            ),
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    // 1. No MOBIL
                                                    Text(
                                                        text = audit.noMobil,
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 13.sp
                                                        )
                                                    )
                                                    // 2. PLAT NOMER
                                                    Text(
                                                        text = audit.platNomer.ifBlank { "-" },
                                                        color = Color.White.copy(alpha = 0.9f),
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    )
                                                    // 3. SAMPLE
                                                    Text(
                                                        text = audit.jenisSample.ifBlank { "-" },
                                                        color = Color.White.copy(alpha = 0.85f),
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    )

                                                    Spacer(modifier = Modifier.height(4.dp))

                                                    // Tombol Lihat Foto / Rincian
                                                    Button(
                                                        onClick = { selectedAuditForModal = audit },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                                        shape = RoundedCornerShape(4.dp),
                                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                                                        modifier = Modifier.height(22.dp)
                                                    ) {
                                                        Icon(Icons.Default.Image, contentDescription = null, tint = HighDensityBlue, modifier = Modifier.size(10.dp))
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text("Foto", color = HighDensityBlue, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = Slate300, thickness = 1.dp)

                                    // 2. TOTAL POIN TEMUAN ROW
                                    Row(
                                        modifier = Modifier
                                            .background(Color(0xFFF1F5F9))
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(colLeftWidth)
                                                .padding(horizontal = 10.dp)
                                        ) {
                                            Text(
                                                text = "TOTAL POIN TEMUAN",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = Slate900, fontSize = 10.sp)
                                            )
                                        }

                                        tableAudits.forEach { audit ->
                                            val isZero = audit.totalPoinTemuan == 0
                                            Box(
                                                modifier = Modifier
                                                    .width(colUnitWidth)
                                                    .padding(horizontal = 3.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${audit.totalPoinTemuan}",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 11.5.sp,
                                                        color = if (isZero) Color(0xFF16A34A) else Color(0xFFDC2626)
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = Slate300, thickness = 1.dp)

                                    // 4. ROWS OF KPI SPECIFICATIONS (SECTIONS -> GROUPS -> ITEMS)
                                    val allSections = com.example.data.model.KpiAuditSpec.sections

                                    allSections.forEach { section ->
                                        // Section Header Row
                                        Row(
                                            modifier = Modifier
                                                .background(Color(0xFFE2E8F0))
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(colLeftWidth)
                                                    .padding(horizontal = 12.dp)
                                            ) {
                                                Text(
                                                    text = section.title.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Black,
                                                        color = Slate900,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }

                                            tableAudits.forEach { _ ->
                                                Box(modifier = Modifier.width(colUnitWidth))
                                            }
                                        }

                                        HorizontalDivider(color = Slate300, thickness = 0.5.dp)

                                        section.groups.forEach { group ->
                                            // Group Subheader Row
                                            Row(
                                                modifier = Modifier
                                                    .background(Color(0xFFF8FAFC))
                                                    .padding(vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(colLeftWidth)
                                                        .padding(start = 16.dp, end = 12.dp)
                                                ) {
                                                    Text(
                                                        text = "• ${group.title}",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = Slate700,
                                                            fontSize = 10.5.sp
                                                        )
                                                    )
                                                }

                                                tableAudits.forEach { _ ->
                                                    Box(modifier = Modifier.width(colUnitWidth))
                                                }
                                            }

                                            HorizontalDivider(color = Slate200, thickness = 0.5.dp)

                                            // Items in Group
                                            group.items.forEachIndexed { itemIdx, item ->
                                                val isEven = itemIdx % 2 == 0
                                                Row(
                                                    modifier = Modifier
                                                        .background(if (isEven) Color.White else Color(0xFFFBFDFF))
                                                        .padding(vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Kolom Kiri: Nama Item KPI
                                                    Box(
                                                        modifier = Modifier
                                                            .width(colLeftWidth)
                                                            .padding(start = 24.dp, end = 8.dp)
                                                    ) {
                                                        Column {
                                                            Text(
                                                                text = item.name,
                                                                style = MaterialTheme.typography.bodySmall.copy(
                                                                    fontSize = 11.sp,
                                                                    color = Slate800,
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                            )
                                                            Text(
                                                                text = "Maks ${item.maxPoints} Poin",
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontSize = 9.sp,
                                                                    color = Slate400
                                                                )
                                                            )
                                                        }
                                                    }

                                                    // Kolom Kanan: Point setiap unit
                                                    tableAudits.forEachIndexed { uIdx, _ ->
                                                        val score = parsedScoresList[uIdx][item.id] ?: 0
                                                        val isZero = score == 0

                                                        Box(
                                                            modifier = Modifier
                                                                .width(colUnitWidth)
                                                                .padding(horizontal = 2.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            if (isZero) {
                                                                Text(
                                                                    text = "0",
                                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                                        fontWeight = FontWeight.Medium,
                                                                        color = Color(0xFF16A34A),
                                                                        fontSize = 11.5.sp
                                                                    )
                                                                )
                                                            } else {
                                                                Surface(
                                                                    color = Color(0xFFFEF3C7),
                                                                    shape = RoundedCornerShape(3.dp),
                                                                    border = BorderStroke(0.5.dp, Amber600.copy(alpha = 0.6f))
                                                                ) {
                                                                    Text(
                                                                        text = "$score",
                                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                                            fontWeight = FontWeight.Black,
                                                                            color = Color(0xFFB45309),
                                                                            fontSize = 11.sp
                                                                        ),
                                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                HorizontalDivider(color = Slate200, thickness = 0.5.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Photo Detail
    selectedAuditForModal?.let { audit ->
        PhotoDetailDialog(
            audit = audit,
            photoTitles = photoTitles,
            currentUser = currentUser,
            onDismiss = { selectedAuditForModal = null },
            onEditAudit = { toEdit ->
                onEditAudit(toEdit)
                selectedAuditForModal = null
            },
            onDeleteAudit = { id ->
                onDeleteAudit(id)
                selectedAuditForModal = null
            }
        )
    }
}

private fun parseKpiScores(json: String?): Map<String, Int> {
    if (json.isNullOrBlank() || json == "{}") return emptyMap()
    return try {
        val obj = org.json.JSONObject(json)
        val map = mutableMapOf<String, Int>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = obj.optInt(key, 0)
        }
        map
    } catch (e: Exception) {
        emptyMap()
    }
}
