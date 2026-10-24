package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DataAuditEntity
import com.example.data.local.entity.MasterBobotEntity
import com.example.data.local.entity.MasterGradeKpiEntity
import com.example.data.local.entity.MasterPeriodeEntity
import com.example.data.local.entity.MasterPoolEntity
import com.example.data.local.entity.MasterSampleEntity
import com.example.data.model.KpiAuditSpec
import com.example.ui.theme.Amber50
import com.example.ui.theme.Amber600
import com.example.ui.theme.Blue50
import com.example.ui.theme.Blue700
import com.example.ui.theme.HighDensityBlue
import com.example.ui.theme.HighDensityBlueBorder
import com.example.ui.theme.HighDensityBlueLight
import com.example.ui.theme.Rose50
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Teal50
import com.example.ui.theme.Teal600
import com.example.util.AuditFilterUtils
import com.example.util.ResumeExportHelper
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Emerald50 = Color(0xFFECFDF5)
private val Emerald600 = Color(0xFF059669)

/**
 * Model data baris Peringkat KPI
 */
data class PeringkatKpiItem(
    val rank: Int,
    val kodePool: String,
    val namaPool: String,
    val statusArea: String,
    val periode: String,
    val nilaiKpi: Double,
    val formattedNilaiKpi: String,
    val grade: String,
    val sampleCount: Int,
    val ketentuanSample: Int,
    val persenExterior: Double,
    val persenInterior: Double,
    val persenCucian: Double,
    val kpiCucian: Double = if (persenCucian > 0) (persenCucian - 20.0).coerceIn(0.0, 80.0) else 0.0,
    val formattedKpiCucian: String = String.format(Locale.US, "%.2f", if (persenCucian > 0) (persenCucian - 20.0).coerceIn(0.0, 80.0) else 0.0),
    val hasData: Boolean
)

enum class PeringkatSortOption(val label: String) {
    NILAI_TERTINGGI("Nilai KPI Tertinggi (#1 s/d Terakhir)"),
    NILAI_TERENDAH("Nilai KPI Terendah"),
    KPI_CUCIAN_TERTINGGI("Nilai KPI Cucian Tertinggi"),
    KODE_POOL("Kode Pool (A - Z)")
}

/**
 * Menu Khusus Level Admin: PERINGKAT KPI
 * Menyajikan tabel peringkat KPI dengan struktur kolom:
 * - Kolom 1: Kode Pool
 * - Kolom 2: STATUS AREA
 * - Kolom 3: Periode
 * - Kolom 4: Nilai KPI
 * - Kolom 5: KPI CUCIAN (sebelah kanan NILAI KPI dan sebelah kiri Aksi)
 * - Kolom 6: AKSI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeringkatKpiScreen(
    pools: List<MasterPoolEntity>,
    audits: List<DataAuditEntity>,
    periodes: List<MasterPeriodeEntity>,
    bobotList: List<MasterBobotEntity>,
    sampleList: List<MasterSampleEntity>,
    gradeList: List<MasterGradeKpiEntity>,
    onNavigateToRekapNilai: (poolCode: String, periode: String) -> Unit = { _, _ -> },
    onNavigateToHasilAudit: (poolCode: String) -> Unit = {}
) {
    val context = LocalContext.current

    // Periode list (Gabungan dari master_periode dan periode yang ada di data audit)
    val allPeriodeOptions = remember(periodes, audits) {
        val list = mutableListOf<String>()
        periodes.forEach { if (it.periode.isNotBlank()) list.add(it.periode.trim()) }
        audits.forEach { if (it.periode.isNotBlank()) list.add(it.periode.trim()) }
        list.distinct().sorted()
    }

    // Default periode: Pilih periode terbaru atau "SEMUA PERIODE" jika kosong
    var selectedPeriode by remember(allPeriodeOptions) {
        mutableStateOf(allPeriodeOptions.lastOrNull() ?: "SEMUA PERIODE")
    }

    // Status Area filter
    var selectedAreaFilter by remember { mutableStateOf("SEMUA AREA") }
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(PeringkatSortOption.NILAI_TERTINGGI) }

    // Dropdown expansion states
    var periodeDropdownExpanded by remember { mutableStateOf(false) }
    var areaDropdownExpanded by remember { mutableStateOf(false) }
    var sortDropdownExpanded by remember { mutableStateOf(false) }

    // Detail dialog state
    var selectedItemForDetail by remember { mutableStateOf<PeringkatKpiItem?>(null) }

    // Bobot Map
    val bobotMap = remember(bobotList) {
        bobotList.associate { it.itemId to it.bobot }
    }

    // Hitung seluruh item data Peringkat KPI
    val rawPeringkatList = remember(pools, audits, selectedPeriode, bobotMap, sampleList, gradeList) {
        val result = mutableListOf<PeringkatKpiItem>()

        if (selectedPeriode == "SEMUA PERIODE") {
            // Evaluasi seluruh kombinasi (Pool, Periode) yang memiliki audit
            val distinctPairs = audits
                .filter { it.pool.isNotBlank() && it.periode.isNotBlank() }
                .map { it.pool.trim().uppercase() to it.periode.trim().uppercase() }
                .distinct()

            if (distinctPairs.isNotEmpty()) {
                distinctPairs.forEach { (pCode, per) ->
                    val poolObj = pools.find { it.kodePool.equals(pCode, ignoreCase = true) }
                        ?: MasterPoolEntity(kodePool = pCode, namaPool = "Pool $pCode", statusArea = "JABODETABEK")
                    val item = calculateKpiForPoolAndPeriode(
                        pool = poolObj,
                        periode = per,
                        audits = audits,
                        bobotMap = bobotMap,
                        sampleList = sampleList,
                        gradeList = gradeList
                    )
                    result.add(item)
                }
            } else {
                // Jika belum ada audit, tampilkan pool terdaftar
                pools.forEach { pool ->
                    result.add(
                        calculateKpiForPoolAndPeriode(
                            pool = pool,
                            periode = "SEMUA PERIODE",
                            audits = audits,
                            bobotMap = bobotMap,
                            sampleList = sampleList,
                            gradeList = gradeList
                        )
                    )
                }
            }
        } else {
            // Periode spesifik dipilih: Evaluasi semua pool untuk periode ini
            pools.forEach { pool ->
                val item = calculateKpiForPoolAndPeriode(
                    pool = pool,
                    periode = selectedPeriode,
                    audits = audits,
                    bobotMap = bobotMap,
                    sampleList = sampleList,
                    gradeList = gradeList
                )
                result.add(item)
            }
        }

        result
    }

    // Filter berdasarkan Status Area dan Pencarian
    val filteredList = remember(rawPeringkatList, selectedAreaFilter, searchQuery) {
        rawPeringkatList.filter { item ->
            val matchArea = if (selectedAreaFilter == "SEMUA AREA") {
                true
            } else {
                item.statusArea.trim().equals(selectedAreaFilter.trim(), ignoreCase = true)
            }

            val matchSearch = if (searchQuery.isBlank()) {
                true
            } else {
                item.kodePool.contains(searchQuery, ignoreCase = true) ||
                        item.namaPool.contains(searchQuery, ignoreCase = true)
            }

            matchArea && matchSearch
        }
    }

    // Urutkan dan Berikan Nomor Peringkat (#1, #2, ...)
    val sortedAndRankedList = remember(filteredList, sortOption) {
        val sorted = when (sortOption) {
            PeringkatSortOption.NILAI_TERTINGGI -> {
                // Dahulukan yang memiliki sample data, lalu nilai KPI tertinggi
                filteredList.sortedWith(
                    compareByDescending<PeringkatKpiItem> { it.hasData }
                        .thenByDescending { it.nilaiKpi }
                        .thenByDescending { it.sampleCount }
                        .thenBy { it.kodePool }
                )
            }
            PeringkatSortOption.NILAI_TERENDAH -> {
                filteredList.sortedWith(
                    compareBy<PeringkatKpiItem> { it.nilaiKpi }
                        .thenBy { it.sampleCount }
                        .thenBy { it.kodePool }
                )
            }
            PeringkatSortOption.KPI_CUCIAN_TERTINGGI -> {
                filteredList.sortedWith(
                    compareByDescending<PeringkatKpiItem> { it.hasData }
                        .thenByDescending { it.kpiCucian }
                        .thenByDescending { it.nilaiKpi }
                        .thenBy { it.kodePool }
                )
            }
            PeringkatSortOption.KODE_POOL -> {
                filteredList.sortedBy { it.kodePool }
            }
        }

        // Tentukan nomor peringkat (rank 1, 2, 3...)
        sorted.mapIndexed { index, item ->
            item.copy(rank = index + 1)
        }
    }

    // Metrik ringkasan untuk header
    val totalPoolEvaluasi = sortedAndRankedList.size
    val poolWithDataCount = sortedAndRankedList.count { it.hasData }
    val rataRataKpi = remember(sortedAndRankedList) {
        val withData = sortedAndRankedList.filter { it.hasData }
        if (withData.isNotEmpty()) {
            withData.map { it.nilaiKpi }.average()
        } else {
            0.0
        }
    }
    val rataRataKpiCucian = remember(sortedAndRankedList) {
        val withData = sortedAndRankedList.filter { it.hasData }
        if (withData.isNotEmpty()) {
            withData.map { it.kpiCucian }.average()
        } else {
            0.0
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // TOP CARD: Header, Badge Menu Admin, dan 4 Kartu KPI Ringkasan
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
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(HighDensityBlueLight, CircleShape)
                                    .border(1.dp, HighDensityBlueBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Leaderboard,
                                    contentDescription = null,
                                    tint = HighDensityBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "PERINGKAT KPI",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = HighDensityBlue,
                                            fontSize = 16.sp,
                                            letterSpacing = (-0.2).sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Amber50,
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(0.6.dp, Color(0xFFFDE68A))
                                    ) {
                                        Text(
                                            text = "KHUSUS ADMIN",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Amber600,
                                                fontSize = 9.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Nilai KPI Audit Keindahan",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Slate600,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        // Tombol Bagikan / Ekspor CSV
                        OutlinedButton(
                            onClick = {
                                exportPeringkatKpiCsv(
                                    context = context,
                                    items = sortedAndRankedList,
                                    selectedPeriode = selectedPeriode
                                )
                            },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, HighDensityBlueBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = HighDensityBlue),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bagikan CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Slate200, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // KOTAK RINGKASAN METRIK
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryMetricBox(
                            title = "Total Pool",
                            value = "$totalPoolEvaluasi Pool",
                            subText = "$poolWithDataCount telah diaudit",
                            bgColor = HighDensityBlueLight,
                            borderColor = HighDensityBlueBorder,
                            textColor = HighDensityBlue,
                            modifier = Modifier.weight(1f)
                        )

                        SummaryMetricBox(
                            title = "Rata-Rata KPI",
                            value = if (poolWithDataCount > 0) String.format(Locale.US, "%.2f", rataRataKpi) else "-",
                            subText = "KPI Cucian: ${if (poolWithDataCount > 0) String.format(Locale.US, "%.2f", rataRataKpiCucian) else "-"}",
                            bgColor = Teal50,
                            borderColor = Color(0xFF99F6E4),
                            textColor = Teal600,
                            modifier = Modifier.weight(1f)
                        )

                        SummaryMetricBox(
                            title = "Periode Aktif",
                            value = if (selectedPeriode == "SEMUA PERIODE") "SEMUA" else selectedPeriode,
                            subText = "Filter Data Evaluasi",
                            bgColor = Color(0xFFF1F5F9),
                            borderColor = Slate200,
                            textColor = Slate800,
                            modifier = Modifier.weight(1.1f)
                        )
                    }
                }
            }
        }

        // CARD FILTER: Dropdown Periode, Dropdown Status Area, Search, dan Urutan
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "FILTER & PENGURUTAN TABEL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = Slate800,
                            fontSize = 11.sp,
                            letterSpacing = 0.3.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 1: Dropdown Periode & Dropdown Status Area
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Dropdown Periode
                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                onClick = { periodeDropdownExpanded = !periodeDropdownExpanded },
                                color = if (selectedPeriode != "SEMUA PERIODE") Blue50 else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (selectedPeriode != "SEMUA PERIODE") Blue700 else Slate200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "PERIODE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                color = Slate500,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = selectedPeriode,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedPeriode != "SEMUA PERIODE") Blue700 else Slate800
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Slate600)
                                }
                            }

                            DropdownMenu(
                                expanded = periodeDropdownExpanded,
                                onDismissRequest = { periodeDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("SEMUA PERIODE", fontWeight = if (selectedPeriode == "SEMUA PERIODE") FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedPeriode = "SEMUA PERIODE"
                                        periodeDropdownExpanded = false
                                    }
                                )
                                allPeriodeOptions.forEach { prd ->
                                    DropdownMenuItem(
                                        text = { Text(prd, fontWeight = if (selectedPeriode == prd) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            selectedPeriode = prd
                                            periodeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Dropdown Status Area
                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                onClick = { areaDropdownExpanded = !areaDropdownExpanded },
                                color = if (selectedAreaFilter != "SEMUA AREA") Emerald50 else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (selectedAreaFilter != "SEMUA AREA") Emerald600 else Slate200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "STATUS AREA",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                color = Slate500,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = selectedAreaFilter,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedAreaFilter != "SEMUA AREA") Emerald600 else Slate800
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Slate600)
                                }
                            }

                            DropdownMenu(
                                expanded = areaDropdownExpanded,
                                onDismissRequest = { areaDropdownExpanded = false }
                            ) {
                                listOf("SEMUA AREA", "JABODETABEK", "JABODETABEK SATELIT", "DAERAH", "DAERAH SATELIT").forEach { area ->
                                    DropdownMenuItem(
                                        text = { Text(area, fontWeight = if (selectedAreaFilter == area) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            selectedAreaFilter = area
                                            areaDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: Pencarian Kode Pool & Dropdown Urutan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Cari Kode / Nama Pool...", fontSize = 11.sp, color = Slate400) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate500, modifier = Modifier.size(14.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Blue700,
                                unfocusedBorderColor = Slate200,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        )

                        // Tombol Urutan
                        Box {
                            Surface(
                                onClick = { sortDropdownExpanded = !sortDropdownExpanded },
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Sort, contentDescription = null, tint = Slate700, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Urutkan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate800)
                                }
                            }

                            DropdownMenu(
                                expanded = sortDropdownExpanded,
                                onDismissRequest = { sortDropdownExpanded = false }
                            ) {
                                PeringkatSortOption.values().forEach { opt ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = opt.label,
                                                fontWeight = if (sortOption == opt) FontWeight.Bold else FontWeight.Normal,
                                                color = if (sortOption == opt) Blue700 else Slate800
                                            )
                                        },
                                        onClick = {
                                            sortOption = opt
                                            sortDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Title: TABEL PERINGKAT KPI
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TABEL PERINGKAT KPI (${sortedAndRankedList.size} Data)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = Slate800,
                        fontSize = 12.5.sp,
                        letterSpacing = 0.2.sp
                    )
                )
                Text(
                    text = "Klik baris untuk detail perhitungan",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Slate500,
                        fontSize = 10.sp
                    )
                )
            }
        }

        // TABEL PERINGKAT KPI DENGAN 5 KOLOM DATA & AKSI
        // Kolom 1: Kode Pool | Kolom 2: STATUS AREA | Kolom 3: Periode | Kolom 4: Nilai KPI | Kolom 5: KPI CUCIAN | Kolom 6: AKSI
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val horizontalScrollState = rememberScrollState()

                Column(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                    // HEADER TABEL
                    Row(
                        modifier = Modifier
                            .background(HighDensityBlue)
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NO",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(44.dp)
                        )
                        Text(
                            text = "KODE POOL",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            modifier = Modifier.width(115.dp)
                        )
                        Text(
                            text = "STATUS AREA",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            modifier = Modifier.width(145.dp)
                        )
                        Text(
                            text = "PERIODE",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            modifier = Modifier.width(125.dp)
                        )
                        Text(
                            text = "NILAI KPI",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(110.dp)
                        )
                        Text(
                            text = "KPI CUCIAN",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(110.dp)
                        )
                        Text(
                            text = "AKSI",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(55.dp)
                        )
                    }

                    HorizontalDivider(color = HighDensityBlueBorder, thickness = 1.dp)

                    if (sortedAndRankedList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .width(704.dp)
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.FilterList, contentDescription = null, tint = Slate400, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Tidak ada data ditemukan sesuai filter.", fontSize = 12.sp, color = Slate600)
                            }
                        }
                    } else {
                        sortedAndRankedList.forEachIndexed { index, item ->
                            val isTop1 = item.rank == 1 && item.hasData
                            val isTop2 = item.rank == 2 && item.hasData
                            val isTop3 = item.rank == 3 && item.hasData

                            val rowBg = when {
                                isTop1 -> Color(0xFFFEF9C3).copy(alpha = 0.4f)
                                isTop2 -> Color(0xFFF1F5F9).copy(alpha = 0.5f)
                                isTop3 -> Color(0xFFFFEDD5).copy(alpha = 0.3f)
                                index % 2 == 1 -> Color(0xFFF8FAFC)
                                else -> Color.White
                            }

                            Row(
                                modifier = Modifier
                                    .background(rowBg)
                                    .clickable { selectedItemForDetail = item }
                                    .padding(vertical = 9.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Kolom No / Rank
                                Box(
                                    modifier = Modifier.width(44.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        isTop1 -> {
                                            Surface(
                                                color = Color(0xFFFACC15),
                                                shape = CircleShape,
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("#1", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF713F12))
                                                }
                                            }
                                        }
                                        isTop2 -> {
                                            Surface(
                                                color = Color(0xFFCBD5E1),
                                                shape = CircleShape,
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("#2", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Slate800)
                                                }
                                            }
                                        }
                                        isTop3 -> {
                                            Surface(
                                                color = Color(0xFFFDBA74),
                                                shape = CircleShape,
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("#3", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF7C2D12))
                                                }
                                            }
                                        }
                                        else -> {
                                            Text(
                                                text = "#${item.rank}",
                                                fontSize = 11.sp,
                                                color = Slate600,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                // Kolom 1: KODE POOL
                                Column(
                                    modifier = Modifier.width(115.dp)
                                ) {
                                    Text(
                                        text = item.kodePool,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = HighDensityBlue,
                                            fontSize = 12.5.sp
                                        )
                                    )
                                    Text(
                                        text = item.namaPool,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Slate600,
                                            fontSize = 9.5.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Kolom 2: STATUS AREA
                                Box(
                                    modifier = Modifier.width(145.dp)
                                ) {
                                    StatusAreaBadge(statusArea = item.statusArea)
                                }

                                // Kolom 3: PERIODE
                                Column(
                                    modifier = Modifier.width(125.dp)
                                ) {
                                    Text(
                                        text = item.periode,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Slate800,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (item.hasData) "${item.sampleCount}/${item.ketentuanSample} Smp" else "0 Smp Diaudit",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (item.hasData) Teal600 else Slate400,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }

                                // Kolom 4: NILAI KPI
                                Column(
                                    modifier = Modifier.width(110.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (item.hasData) {
                                        Text(
                                            text = item.formattedNilaiKpi,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                color = if (item.nilaiKpi >= 60.0) Teal600 else if (item.nilaiKpi >= 50.0) Amber600 else Rose600,
                                                fontSize = 13.sp
                                            )
                                        )
                                        GradeBadge(grade = item.grade)
                                    } else {
                                        Text(
                                            text = "0.00",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = Slate400,
                                                fontSize = 11.sp
                                            )
                                        )
                                        Text(
                                            text = "Belum Audit",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Slate400,
                                                fontSize = 9.sp
                                            )
                                        )
                                    }
                                }

                                // Kolom 5: KPI CUCIAN (sebelah kanan NILAI KPI dan sebelah kiri Aksi)
                                Column(
                                    modifier = Modifier.width(110.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (item.hasData) {
                                        Text(
                                            text = item.formattedKpiCucian,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                color = if (item.kpiCucian >= 60.0) Teal600 else if (item.kpiCucian >= 40.0) Amber600 else Rose600,
                                                fontSize = 13.sp
                                            )
                                        )
                                        Surface(
                                            color = Color(0xFFF3E8FF),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(0.5.dp, Color(0xFFD8B4FE))
                                        ) {
                                            Text(
                                                text = "${String.format(Locale.US, "%.1f", item.persenCucian)}%",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF7E22CE),
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "0.00",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = Slate400,
                                                fontSize = 11.sp
                                            )
                                        )
                                        Text(
                                            text = "Belum Audit",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Slate400,
                                                fontSize = 9.sp
                                            )
                                        )
                                    }
                                }

                                // Kolom Aksi: Tombol Info Detail
                                Box(
                                    modifier = Modifier.width(55.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(
                                        onClick = { selectedItemForDetail = item },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Detail",
                                            tint = HighDensityBlue,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = Slate100, thickness = 0.8.dp)
                        }
                    }
                }
            }
        }
    }

    // POPUP DIALOG: DETAIL PERHITUNGAN NILAI KPI POOL
    if (selectedItemForDetail != null) {
        val item = selectedItemForDetail!!
        AlertDialog(
            onDismissRequest = { selectedItemForDetail = null },
            confirmButton = {
                Button(
                    onClick = {
                        onNavigateToHasilAudit(item.kodePool)
                        selectedItemForDetail = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityBlue),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Lihat Hasil Audit Pool", fontSize = 11.5.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedItemForDetail = null }) {
                    Text("Tutup", color = Slate600, fontSize = 11.5.sp)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Amber600, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Rincian KPI ${item.kodePool}",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = HighDensityBlue
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${item.kodePool} - ${item.namaPool}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate800,
                            fontSize = 13.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Status Area: ${item.statusArea}", fontSize = 11.sp, color = Slate600)
                        Text("Periode: ${item.periode}", fontSize = 11.sp, color = Slate600, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Slate200, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // CARD SKOR NILAI KPI & GRADE
                    Surface(
                        color = HighDensityBlueLight,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, HighDensityBlueBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("NILAI KPI AUDIT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HighDensityBlue)
                                Text(
                                    text = item.formattedNilaiKpi,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = HighDensityBlue
                                )
                                Text("Rumus: ((% Exterior + % Interior) / 2) - 20", fontSize = 9.sp, color = Slate600)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("GRADE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate600)
                                Surface(
                                    color = if (item.grade == "A") Color(0xFFDCFCE7) else Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, if (item.grade == "A") Color(0xFF86EFAC) else Blue700.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "Grade ${item.grade}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (item.grade == "A") Color(0xFF166534) else Blue700,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // CARD SKOR NILAI KPI CUCIAN
                    Surface(
                        color = Color(0xFFFAF5FF),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("NILAI KPI CUCIAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7E22CE))
                                Text(
                                    text = item.formattedKpiCucian,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF7E22CE)
                                )
                                Text("Rumus: % Seksi Cucian - 20 (Min 0, Max 80)", fontSize = 9.sp, color = Slate600)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("CAPAIAN CUCIAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate600)
                                Surface(
                                    color = Color(0xFFF3E8FF),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color(0xFFD8B4FE))
                                ) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", item.persenCucian)}%",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF7E22CE),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // BREAKDOWN SEKSI
                    Text("Rincian Capaian Seksi:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate800)
                    Spacer(modifier = Modifier.height(6.dp))

                    KpiBreakdownRow(
                        label = "Seksi Exterior (/5)",
                        value = "${String.format(Locale.US, "%.1f", item.persenExterior)}%",
                        progress = (item.persenExterior / 100.0).toFloat().coerceIn(0f, 1f),
                        color = HighDensityBlue
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    KpiBreakdownRow(
                        label = "Seksi Interior (/5)",
                        value = "${String.format(Locale.US, "%.1f", item.persenInterior)}%",
                        progress = (item.persenInterior / 100.0).toFloat().coerceIn(0f, 1f),
                        color = Teal600
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    KpiBreakdownRow(
                        label = "Seksi Cucian (/3)",
                        value = "${String.format(Locale.US, "%.1f", item.persenCucian)}%",
                        progress = (item.persenCucian / 100.0).toFloat().coerceIn(0f, 1f),
                        color = Amber600
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Slate200, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sample Terlaksana:", fontSize = 11.sp, color = Slate600)
                        Text(
                            text = "${item.sampleCount} dari ${item.ketentuanSample} Unit Ketentuan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.sampleCount >= item.ketentuanSample) Teal600 else Amber600
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun StatusAreaBadge(statusArea: String) {
    val upper = statusArea.trim().uppercase()
    val (bgColor, borderColor, textColor) = when {
        upper == "JABODETABEK" -> Triple(Color(0xFFEFF6FF), Color(0xFFBFDBFE), Color(0xFF1D4ED8))
        upper == "JABODETABEK SATELIT" -> Triple(Color(0xFFFAF5FF), Color(0xFFE9D5FF), Color(0xFF7E22CE))
        upper == "DAERAH" -> Triple(Color(0xFFECFDF5), Color(0xFFA7F3D0), Color(0xFF047857))
        upper == "DAERAH SATELIT" -> Triple(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFFB45309))
        else -> Triple(Color(0xFFF1F5F9), Slate200, Slate700)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.7.dp, borderColor)
    ) {
        Text(
            text = upper,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 9.5.sp
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GradeBadge(grade: String) {
    val upper = grade.trim().uppercase()
    val (bgColor, textColor) = when (upper) {
        "A" -> Pair(Color(0xFFDCFCE7), Color(0xFF15803D))
        "B" -> Pair(Color(0xFFDBEAFE), Color(0xFF1D4ED8))
        "C" -> Pair(Color(0xFFFEF3C7), Color(0xFFB45309))
        "D" -> Pair(Color(0xFFFFEDD5), Color(0xFFC2410C))
        "E" -> Pair(Color(0xFFFEE2E2), Color(0xFFB91C1C))
        else -> Pair(Slate100, Slate600)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = "Grade $upper",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                color = textColor,
                fontSize = 9.sp
            ),
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun SummaryMetricBox(
    title: String,
    value: String,
    subText: String,
    bgColor: Color,
    borderColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Slate600,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    fontSize = 14.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subText,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Slate500,
                    fontSize = 9.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun KpiBreakdownRow(
    label: String,
    value: String,
    progress: Float,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 10.sp, color = Slate700)
            Text(value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Slate200
        )
    }
}

/**
 * Logika perhitungan Nilai KPI per Pool dan Periode sesuai rumus resmi:
 * ((% SEKSI EXTERIOR + % SEKSI INTERIOR) / 2) - 20
 */
private fun calculateKpiForPoolAndPeriode(
    pool: MasterPoolEntity,
    periode: String,
    audits: List<DataAuditEntity>,
    bobotMap: Map<String, Double>,
    sampleList: List<MasterSampleEntity>,
    gradeList: List<MasterGradeKpiEntity>
): PeringkatKpiItem {
    // Filter audits untuk pool ini dan periode terpilih
    val poolAudits = audits.filter {
        it.pool.equals(pool.kodePool, ignoreCase = true) &&
                (periode.isBlank() || periode.equals("SEMUA PERIODE", ignoreCase = true) || it.periode.equals(periode, ignoreCase = true))
    }

    val deduped = AuditFilterUtils.filterUniqueAudits(poolAudits)
    val latestAudits = AuditFilterUtils.filterLatestPerCarAndPeriod(deduped)
    val sampleCount = latestAudits.size

    // Ketentuan Sample untuk Status Area pool ini
    val statusArea = pool.statusArea.trim().uppercase()
    val sampleEntity = sampleList.find { it.statusArea.trim().equals(statusArea, ignoreCase = true) }
    val ketentuanSample = sampleEntity?.jumlahSample ?: 10

    if (sampleCount == 0 || ketentuanSample <= 0) {
        val grade = if (gradeList.isNotEmpty()) {
            gradeList.find { it.kategori.equals("E", ignoreCase = true) }?.kategori ?: "E"
        } else "E"

        return PeringkatKpiItem(
            rank = 0,
            kodePool = pool.kodePool,
            namaPool = pool.namaPool,
            statusArea = pool.statusArea,
            periode = if (periode.isBlank() || periode == "SEMUA PERIODE") "SEMUA PERIODE" else periode,
            nilaiKpi = 0.0,
            formattedNilaiKpi = "0.00",
            grade = grade,
            sampleCount = sampleCount,
            ketentuanSample = ketentuanSample,
            persenExterior = 0.0,
            persenInterior = 0.0,
            persenCucian = 0.0,
            kpiCucian = 0.0,
            formattedKpiCucian = "0.00",
            hasData = false
        )
    }

    // Parsing score temuan dari masing-masing audit
    val parsedScores = latestAudits.map { ResumeExportHelper.parseKpiScores(it.kpiScoresJson) }
    val findingsMap = mutableMapOf<String, Int>()
    for (scores in parsedScores) {
        for ((itemId, score) in scores) {
            findingsMap[itemId] = (findingsMap[itemId] ?: 0) + score
        }
    }

    // Hitung total bobot dan % sub group per seksi
    var sumExteriorPersen = 0.0
    var sumInteriorPersen = 0.0
    var sumCucianPersen = 0.0

    for (section in KpiAuditSpec.sections) {
        val isExterior = section.id.equals("EXTERIOR", ignoreCase = true) || section.title.contains("EXTERIOR", ignoreCase = true)
        val isInterior = section.id.equals("INTERIOR", ignoreCase = true) || section.title.contains("INTERIOR", ignoreCase = true)
        val isCucian = section.id.equals("CUCIAN", ignoreCase = true) || section.title.contains("CUCIAN", ignoreCase = true)

        for (group in section.groups) {
            var totalBobotGroup = 0.0
            for (item in group.items) {
                val b = bobotMap[item.id] ?: item.defaultBobot
                val pointMax = item.maxPoints
                val totalPointMax = pointMax * sampleCount
                val jumlahPoint = findingsMap[item.id] ?: 0
                val totalPointTemuan = totalPointMax - jumlahPoint
                val totalBobotTemuan = totalPointTemuan * b
                totalBobotGroup += totalBobotTemuan
            }
            val nilaiPersenGroup = totalBobotGroup / ketentuanSample.toDouble()
            if (isExterior) {
                sumExteriorPersen += nilaiPersenGroup
            } else if (isInterior) {
                sumInteriorPersen += nilaiPersenGroup
            } else if (isCucian) {
                sumCucianPersen += nilaiPersenGroup
            }
        }
    }

    val persenExterior = sumExteriorPersen / 5.0
    val persenInterior = sumInteriorPersen / 5.0
    val persenCucian = sumCucianPersen / 3.0

    // NILAI KPI: ((% SEKSI EXTERIOR + % SEKSI INTERIOR) / 2) - 20 (Min 0, Max 80)
    val calculated = ((persenExterior + persenInterior) / 2.0) - 20.0
    val nilaiKpi = calculated.coerceIn(0.0, 80.0)
    val formattedNilaiKpi = String.format(Locale.US, "%.2f", nilaiKpi)

    // KPI CUCIAN: % Seksi Cucian dikurangi 20 (Min 0, Max 80)
    val calculatedCucian = persenCucian - 20.0
    val kpiCucian = calculatedCucian.coerceIn(0.0, 80.0)
    val formattedKpiCucian = String.format(Locale.US, "%.2f", kpiCucian)

    // Grade KPI
    val grade = if (gradeList.isNotEmpty()) {
        val rounded = kotlin.math.round(nilaiKpi * 100.0) / 100.0
        val found = gradeList.firstOrNull {
            rounded >= it.minScore - 0.005 && rounded <= it.maxScore + 0.005
        } ?: gradeList.firstOrNull { nilaiKpi in it.minScore..it.maxScore }

        found?.kategori ?: when {
            rounded >= 70.00 -> gradeList.find { it.kategori.equals("A", ignoreCase = true) }?.kategori ?: "A"
            rounded <= 29.99 -> gradeList.find { it.kategori.equals("E", ignoreCase = true) }?.kategori ?: "E"
            else -> "-"
        }
    } else {
        when {
            nilaiKpi >= 70.00 -> "A"
            nilaiKpi >= 60.00 -> "B"
            nilaiKpi >= 50.00 -> "C"
            nilaiKpi >= 30.00 -> "D"
            else -> "E"
        }
    }

    return PeringkatKpiItem(
        rank = 0,
        kodePool = pool.kodePool,
        namaPool = pool.namaPool,
        statusArea = pool.statusArea,
        periode = if (periode.isBlank() || periode == "SEMUA PERIODE") "SEMUA PERIODE" else periode,
        nilaiKpi = nilaiKpi,
        formattedNilaiKpi = formattedNilaiKpi,
        grade = grade,
        sampleCount = sampleCount,
        ketentuanSample = ketentuanSample,
        persenExterior = persenExterior,
        persenInterior = persenInterior,
        persenCucian = persenCucian,
        kpiCucian = kpiCucian,
        formattedKpiCucian = formattedKpiCucian,
        hasData = true
    )
}

/**
 * Ekspor Tabel Peringkat KPI ke format CSV
 */
private fun exportPeringkatKpiCsv(
    context: Context,
    items: List<PeringkatKpiItem>,
    selectedPeriode: String
) {
    try {
        val safePeriode = selectedPeriode.replace(" ", "_").replace("/", "-")
        val fileName = "PERINGKAT_KPI_${safePeriode}_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        val csvBuilder = StringBuilder()
        csvBuilder.appendLine("PERINGKAT KPI AUDIT KEINDAHAN")
        csvBuilder.appendLine("Periode:,$selectedPeriode")
        csvBuilder.appendLine("Tanggal Ekspor:,${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US).format(Date())}")
        csvBuilder.appendLine()
        csvBuilder.appendLine("Peringkat,Kode Pool,Nama Pool,STATUS AREA,Periode,Nilai KPI,KPI Cucian")

        items.forEach { item ->
            val cleanPoolName = item.namaPool.replace(",", " ")
            csvBuilder.appendLine(
                "${item.rank},${item.kodePool},\"$cleanPoolName\",${item.statusArea},${item.periode},${item.formattedNilaiKpi},${item.formattedKpiCucian}"
            )
        }

        FileOutputStream(file).use { os ->
            os.write(csvBuilder.toString().toByteArray())
        }

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Peringkat KPI Audit Keindahan - $selectedPeriode")
            putExtra(Intent.EXTRA_TEXT, "Berikut adalah laporan Peringkat KPI Audit Keindahan untuk Periode $selectedPeriode.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Bagikan Tabel Peringkat KPI"))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal mengekspor CSV: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
