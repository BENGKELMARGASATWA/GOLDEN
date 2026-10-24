package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.local.entity.DataAuditEntity
import com.example.data.local.entity.MasterBobotEntity
import com.example.data.local.entity.MasterPeriodeEntity
import com.example.data.local.entity.MasterPoolEntity
import com.example.data.local.entity.MasterSampleEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.KpiAuditSpec
import com.example.data.model.KpiItem
import com.example.util.AuditFilterUtils
import com.example.ui.theme.Amber600
import com.example.ui.theme.HighDensityBg
import com.example.ui.theme.HighDensityBlue
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Model helper for rendering checklist items with hierarchy in BOBOT RESUME.
 */
data class BobotRowItem(
    val item: KpiItem,
    val sectionTitle: String,
    val groupTitle: String
)

/**
 * Model helper for sub group percentage summary table.
 */
data class SubGroupSummaryItem(
    val sectionId: String,
    val sectionTitle: String,
    val groupId: String,
    val groupTitle: String,
    val totalBobotTemuan: Double,
    val nilaiPersen: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BobotResumeScreen(
    audits: List<DataAuditEntity>,
    bobotList: List<MasterBobotEntity>,
    periodes: List<MasterPeriodeEntity>,
    pools: List<MasterPoolEntity>,
    currentUser: UserEntity?,
    sampleList: List<MasterSampleEntity> = emptyList(),
    onUpdateBobot: ((String, Double) -> Unit)? = null,
    onResetAllBobot: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userRole = currentUser?.levelAkses?.trim()?.uppercase() ?: ""
    val isSuperAdmin = userRole == "SUPER ADMIN"
    val registeredPool = currentUser?.kodePool?.trim().orEmpty()
    val isRestrictedPool = registeredPool.isNotBlank() && !registeredPool.equals("SKT", ignoreCase = true) && !registeredPool.equals("ALL", ignoreCase = true) &&
        (userRole == "KEPALA BENGKEL" || userRole.contains("KABAG") || userRole == "AUDITOR" || (userRole != "SUPER ADMIN" && userRole != "ADMIN" && userRole != "SKT" && userRole != "MASTER DATA"))

    val userPoolList = remember(registeredPool) {
        registeredPool.split(",", ";").map { it.trim().uppercase() }.filter { it.isNotBlank() }
    }

    val availablePoolOptions = remember(pools, isRestrictedPool, userPoolList) {
        if (isRestrictedPool && userPoolList.isNotEmpty()) {
            val list = mutableListOf<String>()
            if (userPoolList.size > 1) {
                list.add("ALL")
            }
            list.addAll(userPoolList)
            list.distinct()
        } else {
            val list = mutableListOf<String>()
            list.add("ALL")
            val pList = pools.map { it.kodePool.trim().uppercase() }.filter { it.isNotBlank() }.distinct()
            list.addAll(pList)
            list.distinct()
        }
    }

    val defaultPool = remember(currentUser, isRestrictedPool, userPoolList) {
        if (isRestrictedPool && userPoolList.isNotEmpty()) {
            if (userPoolList.size == 1) userPoolList.first() else "ALL"
        } else {
            "ALL"
        }
    }

    // State Filters
    var selectedPeriode by remember { mutableStateOf("ALL") }
    var selectedPool by remember(currentUser) {
        mutableStateOf(defaultPool)
    }
    var selectedSample by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    // Dropdown expanded states
    var periodeExpanded by remember { mutableStateOf(false) }
    var poolExpanded by remember { mutableStateOf(false) }
    var sampleExpanded by remember { mutableStateOf(false) }

    // Map bobot by itemId for instant O(1) lookup
    val bobotMap = remember(bobotList) {
        bobotList.associate { it.itemId to it.bobot }
    }

    // Flatten all KPI items with their section and group titles
    val allKpiRows = remember {
        KpiAuditSpec.sections.flatMap { section ->
            section.groups.flatMap { group ->
                group.items.map { item ->
                    BobotRowItem(
                        item = item,
                        sectionTitle = section.title,
                        groupTitle = group.title
                    )
                }
            }
        }
    }

    // Filter audits based on selected Periode, Pool, and Sample
    val filteredAudits = remember(audits, selectedPeriode, selectedPool, selectedSample, isRestrictedPool, userPoolList) {
        val filtered = audits.filter { audit ->
            val matchPeriode = if (selectedPeriode == "ALL") true else audit.periode.equals(selectedPeriode, ignoreCase = true)
            val matchPool = if (isRestrictedPool) {
                if (selectedPool == "ALL") {
                    userPoolList.any { it.equals(audit.pool.trim(), ignoreCase = true) }
                } else {
                    audit.pool.equals(selectedPool, ignoreCase = true)
                }
            } else {
                if (selectedPool == "ALL") true else audit.pool.equals(selectedPool, ignoreCase = true)
            }
            val matchSample = if (selectedSample == "ALL") true else audit.jenisSample.equals(selectedSample, ignoreCase = true)
            matchPeriode && matchPool && matchSample
        }
        val deduped = AuditFilterUtils.filterUniqueAudits(filtered)
        AuditFilterUtils.filterLatestPerCarAndPeriod(deduped)
    }

    // Parse all scores from filtered audits
    val parsedScoresList = remember(filteredAudits) {
        filteredAudits.map { audit ->
            try {
                val json = JSONObject(audit.kpiScoresJson)
                val map = mutableMapOf<String, Int>()
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = json.optInt(key, 0)
                }
                map
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    // Calculate finding sum per item for selected pool in one period
    val findingsMap = remember(parsedScoresList) {
        val map = mutableMapOf<String, Int>()
        for (scores in parsedScoresList) {
            for ((itemId, score) in scores) {
                map[itemId] = (map[itemId] ?: 0) + score
            }
        }
        map
    }

    val sampleCount = filteredAudits.size

    // Filter items by search query
    val displayedItems = remember(allKpiRows, searchQuery) {
        if (searchQuery.isBlank()) {
            allKpiRows
        } else {
            val q = searchQuery.trim().lowercase()
            allKpiRows.filter {
                it.item.name.lowercase().contains(q) ||
                it.item.id.lowercase().contains(q) ||
                it.sectionTitle.lowercase().contains(q) ||
                it.groupTitle.lowercase().contains(q)
            }
        }
    }

    val effectivePool = selectedPool

    // Ketentuan Sample lookup based on Status Area of selected pool (or pools)
    val ketentuanSample = remember(effectivePool, pools, sampleList, filteredAudits) {
        if (effectivePool != "ALL") {
            val poolObj = pools.find { it.kodePool.equals(effectivePool, ignoreCase = true) }
            val statusArea = poolObj?.statusArea?.trim()?.uppercase() ?: "JABODETABEK"
            val sampleEntity = sampleList.find { it.statusArea.trim().equals(statusArea, ignoreCase = true) }
            sampleEntity?.jumlahSample ?: 10
        } else {
            val distinctPools = filteredAudits.map { it.pool.trim().uppercase() }.distinct().filter { it.isNotBlank() }
            if (distinctPools.isNotEmpty()) {
                distinctPools.sumOf { pCode ->
                    val poolObj = pools.find { it.kodePool.equals(pCode, ignoreCase = true) }
                    val statusArea = poolObj?.statusArea?.trim()?.uppercase() ?: "JABODETABEK"
                    val sampleEntity = sampleList.find { it.statusArea.trim().equals(statusArea, ignoreCase = true) }
                    sampleEntity?.jumlahSample ?: 10
                }
            } else {
                val firstPool = pools.firstOrNull()
                val statusArea = firstPool?.statusArea?.trim()?.uppercase() ?: "JABODETABEK"
                val sampleEntity = sampleList.find { it.statusArea.trim().equals(statusArea, ignoreCase = true) }
                sampleEntity?.jumlahSample ?: 10
            }
        }
    }

    val belumCek = (ketentuanSample - sampleCount).coerceAtLeast(0)

    // Summary calculations across all KPI items
    val grandTotalPointMax = remember(allKpiRows) {
        allKpiRows.sumOf { it.item.maxPoints }
    }
    val grandTotalJumlahBobot = remember(allKpiRows, bobotMap) {
        allKpiRows.sumOf { row ->
            val b = bobotMap[row.item.id] ?: row.item.defaultBobot
            b * row.item.maxPoints
        }
    }
    val grandTotalJumlahPoint = remember(allKpiRows, findingsMap) {
        allKpiRows.sumOf { row ->
            findingsMap[row.item.id] ?: 0
        }
    }
    val grandTotalPointTemuan = remember(allKpiRows, findingsMap, sampleCount) {
        allKpiRows.sumOf { row ->
            val pointMax = row.item.maxPoints
            val jumlahPoint = findingsMap[row.item.id] ?: 0
            (pointMax * sampleCount) - jumlahPoint
        }
    }
    val grandTotalBobotTemuan = remember(allKpiRows, bobotMap, findingsMap, sampleCount) {
        allKpiRows.sumOf { row ->
            val b = bobotMap[row.item.id] ?: row.item.defaultBobot
            val pointMax = row.item.maxPoints
            val jumlahPoint = findingsMap[row.item.id] ?: 0
            val totalPointTemuan = (pointMax * sampleCount) - jumlahPoint
            totalPointTemuan * b
        }
    }
    val grandNilaiTotal = remember(grandTotalBobotTemuan, ketentuanSample) {
        if (ketentuanSample > 0) grandTotalBobotTemuan / ketentuanSample else 0.0
    }

    // All Sub Groups Summary (% dari setiap SUB GROUP)
    val subGroupSummaries = remember(allKpiRows, bobotMap, findingsMap, sampleCount, ketentuanSample) {
        val list = mutableListOf<SubGroupSummaryItem>()
        for (section in KpiAuditSpec.sections) {
            for (group in section.groups) {
                val totalBbt = group.items.sumOf { item ->
                    val b = bobotMap[item.id] ?: item.defaultBobot
                    val pointMax = item.maxPoints
                    val jumlahPoint = findingsMap[item.id] ?: 0
                    val totalPointTemuan = (pointMax * sampleCount) - jumlahPoint
                    totalPointTemuan * b
                }
                val nilaiPersen = if (ketentuanSample > 0) totalBbt / ketentuanSample else 0.0
                list.add(
                    SubGroupSummaryItem(
                        sectionId = section.id,
                        sectionTitle = section.title,
                        groupId = group.id,
                        groupTitle = group.title,
                        totalBobotTemuan = totalBbt,
                        nilaiPersen = nilaiPersen
                    )
                )
            }
        }
        list
    }

    val totalPersenSemuaGroup = remember(subGroupSummaries) {
        subGroupSummaries.sumOf { it.nilaiPersen }
    }

    // Calculate Section percentages (% SEKSI) divided by specific divisors:
    // Exterior / 5, Interior / 5, Cucian / 3
    val persenExterior = remember(subGroupSummaries) {
        val sum = subGroupSummaries.filter {
            it.sectionId.equals("EXTERIOR", ignoreCase = true) ||
            it.sectionTitle.contains("EXTERIOR", ignoreCase = true)
        }.sumOf { it.nilaiPersen }
        sum / 5.0
    }
    val persenInterior = remember(subGroupSummaries) {
        val sum = subGroupSummaries.filter {
            it.sectionId.equals("INTERIOR", ignoreCase = true) ||
            it.sectionTitle.contains("INTERIOR", ignoreCase = true)
        }.sumOf { it.nilaiPersen }
        sum / 5.0
    }
    val persenCucian = remember(subGroupSummaries) {
        val sum = subGroupSummaries.filter {
            it.sectionId.equals("CUCIAN", ignoreCase = true) ||
            it.sectionTitle.contains("CUCIAN", ignoreCase = true)
        }.sumOf { it.nilaiPersen }
        sum / 3.0
    }

    // NILAI KPI: (( % SEKSI EXTERIOR + % SEKSI INTERIOR)/2)-20 (Min: 0, Maks: 80)
    val nilaiKpiPersen = remember(persenExterior, persenInterior) {
        val calculated = ((persenExterior + persenInterior) / 2.0) - 20.0
        calculated.coerceIn(0.0, 80.0)
    }

    // Format desimal 2 angka di belakang koma (tanpa persen)
    val formattedNilaiKpi = remember(nilaiKpiPersen) {
        String.format(Locale.US, "%.2f", nilaiKpiPersen)
    }

    // KPI CUCIAN: Cucian dikurangi 20 dalam desimal 2 digit (Min: 0, Maks: 80)
    val kpiCucianPersen = remember(persenCucian) {
        val calculated = persenCucian - 20.0
        calculated.coerceIn(0.0, 80.0)
    }

    val formattedKpiCucian = remember(kpiCucianPersen) {
        String.format(Locale.US, "%.2f", kpiCucianPersen)
    }

    val horizontalScrollState = rememberScrollState()

    // Table Column Widths (11 columns as requested)
    val col1Width = 230.dp // TABEL INPUT
    val col2Width = 70.dp  // BOBOT
    val col3Width = 65.dp  // POINT
    val col4Width = 95.dp  // JUMLAH BOBOT ([BOBOT]*[POINT])
    val col5Width = 90.dp  // JUMLAH POINT
    val col6Width = 90.dp  // JUMLAH SAMPLE
    val col7Width = 105.dp // KETENTUAN SAMPLE
    val col8Width = 80.dp  // BELUM CEK
    val col9Width = 120.dp // TOTAL POINT TEMUAN
    val col10Width = 120.dp // TOTAL BOBOT TEMUAN
    val col11Width = 95.dp // NILAI TOTAL

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HighDensityBg)
    ) {
        // TOP HEADER BAR
        Surface(
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "BOBOT RESUME",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Slate900,
                                fontSize = 18.sp,
                                letterSpacing = (-0.3).sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = if (isSuperAdmin) HighDensityBlue.copy(alpha = 0.1f) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = currentUser?.levelAkses ?: "ALL USER",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (isSuperAdmin) HighDensityBlue else Slate600,
                                    fontSize = 9.5.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Tampilan Pembobotan Nilai Poin Temuan Audit",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate500,
                            fontSize = 11.5.sp
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            exportBobotResumeCsv(
                                context = context,
                                rows = allKpiRows,
                                bobotMap = bobotMap,
                                findingsMap = findingsMap,
                                sampleCount = sampleCount,
                                ketentuanSample = ketentuanSample,
                                nilaiKpiPersen = nilaiKpiPersen,
                                kpiCucianPersen = kpiCucianPersen,
                                subGroupSummaries = subGroupSummaries,
                                totalPersenSemuaGroup = totalPersenSemuaGroup,
                                selectedPeriode = selectedPeriode,
                                selectedPool = effectivePool
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, HighDensityBlue),
                        modifier = Modifier.testTag("btn_export_bobot_csv")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export CSV",
                            tint = HighDensityBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "CSV",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = HighDensityBlue
                            )
                        )
                    }
                }
            }
        }

        // SCROLLABLE BODY
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. KPI SUMMARY CARDS: JUMLAH SAMPLE | NILAI KPI | KPI CUCIAN
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card 1: JUMLAH SAMPLE
                    SummaryMetricCard(
                        title = "JUMLAH SAMPLE",
                        value = "$sampleCount / $ketentuanSample Unit",
                        subtitle = "Belum Cek: $belumCek Unit",
                        icon = Icons.Default.DirectionsCar,
                        color = if (belumCek == 0 && sampleCount > 0) Color(0xFF16A34A) else Color(0xFFD97706),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_jumlah_sample_bobot")
                    )

                    // Card 2: NILAI KPI (Min 0 - Maks 80)
                    SummaryMetricCard(
                        title = "NILAI KPI",
                        value = formattedNilaiKpi,
                        subtitle = "Min: 0 | Maks: 80",
                        icon = Icons.Default.Assessment,
                        color = Color(0xFF0284C7),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_nilai_kpi_bobot")
                    )

                    // Card 3: KPI CUCIAN (Cucian dikurangi 20 dalam desimal 2 digit)
                    SummaryMetricCard(
                        title = "KPI CUCIAN",
                        value = formattedKpiCucian,
                        subtitle = "Cucian - 20",
                        icon = Icons.Default.LocalCarWash,
                        color = Color(0xFF7C3AED),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_kpi_cucian_bobot")
                    )
                }
            }

            // 2. FILTERS CARD
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Filter Periode
                            ExposedDropdownMenuBox(
                                expanded = periodeExpanded,
                                onExpandedChange = { periodeExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = if (selectedPeriode == "ALL") "Semua Periode" else selectedPeriode,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Periode", fontSize = 10.sp) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodeExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = HighDensityBlue,
                                        unfocusedBorderColor = Slate300
                                    ),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                )
                                ExposedDropdownMenu(
                                    expanded = periodeExpanded,
                                    onDismissRequest = { periodeExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Semua Periode", fontSize = 12.sp) },
                                        onClick = {
                                            selectedPeriode = "ALL"
                                            periodeExpanded = false
                                        }
                                    )
                                    periodes.forEach { p ->
                                        DropdownMenuItem(
                                            text = { Text(p.periode, fontSize = 12.sp) },
                                            onClick = {
                                                selectedPeriode = p.periode
                                                periodeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Filter Pool
                            ExposedDropdownMenuBox(
                                expanded = poolExpanded,
                                onExpandedChange = { poolExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                val poolDisplayText = if (selectedPool == "ALL") {
                                    if (isRestrictedPool && userPoolList.size > 1) "Semua Pool Saya" else "Semua Pool"
                                } else {
                                    selectedPool
                                }

                                OutlinedTextField(
                                    value = poolDisplayText,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Pool", fontSize = 10.sp) },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = poolExpanded)
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
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                )
                                ExposedDropdownMenu(
                                    expanded = poolExpanded,
                                    onDismissRequest = { poolExpanded = false }
                                ) {
                                    availablePoolOptions.forEach { pl ->
                                        val labelText = if (pl == "ALL") {
                                            if (isRestrictedPool && userPoolList.size > 1) "Semua Pool Saya" else "Semua Pool"
                                        } else {
                                            pl
                                        }
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = labelText,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (pl == selectedPool) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                selectedPool = pl
                                                poolExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Filter Sample
                            ExposedDropdownMenuBox(
                                expanded = sampleExpanded,
                                onExpandedChange = { sampleExpanded = it },
                                modifier = Modifier.weight(0.9f)
                            ) {
                                OutlinedTextField(
                                    value = if (selectedSample == "ALL") "Semua Sample" else selectedSample,
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
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                )
                                ExposedDropdownMenu(
                                    expanded = sampleExpanded,
                                    onDismissRequest = { sampleExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Semua Sample", fontSize = 12.sp) },
                                        onClick = {
                                            selectedSample = "ALL"
                                            sampleExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("OPERASIONAL", fontSize = 12.sp) },
                                        onClick = {
                                            selectedSample = "OPERASIONAL"
                                            sampleExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("SERVICE", fontSize = 12.sp) },
                                        onClick = {
                                            selectedSample = "SERVICE"
                                            sampleExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Search Input
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Cari item input / checklist...", fontSize = 11.5.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate400, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HighDensityBlue,
                                unfocusedBorderColor = Slate300
                            ),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            singleLine = true
                        )
                    }
                }
            }

            // 3. TABLE CARD
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        HorizontalDivider(color = Slate200, thickness = 0.5.dp)

                        // HORIZONTALLY SCROLLABLE TABLE
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            Column {
                                // TABLE HEADER (11 Columns as requested)
                                Row(
                                    modifier = Modifier
                                        .background(HighDensityBlue)
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Col 1: TABEL INPUT
                                    Box(
                                        modifier = Modifier
                                            .width(col1Width)
                                            .padding(horizontal = 10.dp)
                                    ) {
                                        Text(
                                            text = "TABEL INPUT",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }

                                    // Col 2: BOBOT
                                    Box(
                                        modifier = Modifier
                                            .width(col2Width)
                                            .padding(horizontal = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "BOBOT",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                fontSize = 10.5.sp
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Col 3: POINT
                                    Box(
                                        modifier = Modifier
                                            .width(col3Width)
                                            .padding(horizontal = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "POINT",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                fontSize = 10.5.sp
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Col 4: JUMLAH BOBOT ([BOBOT]*[POINT])
                                    Box(
                                        modifier = Modifier
                                            .width(col4Width)
                                            .padding(horizontal = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "JUMLAH BOBOT",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White,
                                                    fontSize = 9.5.sp
                                                ),
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = "[BOBOT]*[POINT]",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Normal,
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    fontSize = 7.5.sp
                                                ),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    // Col 5: JUMLAH POINT
                                    Box(
                                        modifier = Modifier
                                            .width(col5Width)
                                            .padding(horizontal = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "JUMLAH POINT",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                fontSize = 9.5.sp
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Col 6: JUMLAH SAMPLE
                                    Box(
                                        modifier = Modifier
                                            .width(col6Width)
                                            .padding(horizontal = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "JUMLAH\nSAMPLE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                fontSize = 9.5.sp
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Col 7: KETENTUAN SAMPLE
                                    Box(
                                        modifier = Modifier
                                            .width(col7Width)
                                            .padding(horizontal = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "KETENTUAN\nSAMPLE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                fontSize = 9.5.sp
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Col 8: BELUM CEK
                                    Box(
                                        modifier = Modifier
                                            .width(col8Width)
                                            .padding(horizontal = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "BELUM CEK",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                fontSize = 9.5.sp
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Col 9: TOTAL POINT TEMUAN
                                    Box(
                                        modifier = Modifier
                                            .width(col9Width)
                                            .padding(horizontal = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "TOTAL POINT\nTEMUAN",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                fontSize = 9.sp
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Col 10: TOTAL BOBOT TEMUAN
                                    Box(
                                        modifier = Modifier
                                            .width(col10Width)
                                            .padding(horizontal = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "TOTAL BOBOT\nTEMUAN",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                fontSize = 9.sp
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Col 11: NILAI TOTAL
                                    Box(
                                        modifier = Modifier
                                            .width(col11Width)
                                            .padding(horizontal = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "NILAI TOTAL",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                fontSize = 10.sp
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                HorizontalDivider(color = Slate300, thickness = 1.dp)

                                // TABLE ROWS
                                if (displayedItems.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Tidak ada item checklist yang sesuai pencarian.",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                                        )
                                    }
                                } else {
                                    var lastSection = ""
                                    var lastGroup = ""

                                    displayedItems.forEachIndexed { index, rowItem ->
                                        // Section Header Row
                                        if (rowItem.sectionTitle != lastSection) {
                                            lastSection = rowItem.sectionTitle
                                            lastGroup = ""
                                            Row(
                                                modifier = Modifier
                                                    .background(Color(0xFFE2E8F0))
                                                    .padding(vertical = 5.dp, horizontal = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = lastSection.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Black,
                                                        color = Slate900,
                                                        fontSize = 10.5.sp
                                                    )
                                                )
                                            }
                                            HorizontalDivider(color = Slate300, thickness = 0.5.dp)
                                        }

                                        // Group Header Row
                                        if (rowItem.groupTitle != lastGroup) {
                                            lastGroup = rowItem.groupTitle
                                            Row(
                                                modifier = Modifier
                                                    .background(Color(0xFFF1F5F9))
                                                    .padding(vertical = 4.dp, horizontal = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "• $lastGroup",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = Slate700,
                                                        fontSize = 10.sp
                                                    )
                                                )
                                            }
                                            HorizontalDivider(color = Slate200, thickness = 0.5.dp)
                                        }

                                        val isEven = index % 2 == 0
                                        // 1. BOBOT
                                        val bobot = bobotMap[rowItem.item.id] ?: rowItem.item.defaultBobot
                                        // 2. POINT (max point)
                                        val pointMax = rowItem.item.maxPoints
                                        // 3. JUMLAH BOBOT = [BOBOT] * [POINT]
                                        val jumlahBobot = bobot * pointMax
                                        // 4. JUMLAH POINT = seluruh point yang diinput
                                        val jumlahPoint = findingsMap[rowItem.item.id] ?: 0
                                        // 5. JUMLAH SAMPLE = seluruh yang diaudit
                                        val jumlahSample = sampleCount
                                        // 6. KETENTUAN SAMPLE = target sample pool
                                        // 7. BELUM CEK = [KETENTUAN SAMPLE] - [JUMLAH SAMPLE]
                                        val belumCekItem = (ketentuanSample - jumlahSample).coerceAtLeast(0)
                                        // 8. TOTAL POINT TEMUAN = ([POINT] * [JUMLAH SAMPLE]) - [JUMLAH POINT]
                                        val totalPointTemuan = (pointMax * jumlahSample) - jumlahPoint
                                        // 9. TOTAL BOBOT TEMUAN = [TOTAL POINT TEMUAN] * [BOBOT]
                                        val totalBobotTemuan = totalPointTemuan * bobot
                                        // 10. NILAI TOTAL = [TOTAL BOBOT TEMUAN] / [KETENTUAN SAMPLE]
                                        val nilaiTotal = if (ketentuanSample > 0) totalBobotTemuan / ketentuanSample else 0.0

                                        Row(
                                            modifier = Modifier
                                                .background(if (isEven) Color.White else Color(0xFFFBFDFF))
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Col 1: TABEL INPUT (Item name & ID)
                                            Box(
                                                modifier = Modifier
                                                    .width(col1Width)
                                                    .padding(start = 14.dp, end = 8.dp)
                                            ) {
                                                Column {
                                                    Text(
                                                        text = rowItem.item.name,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontSize = 11.5.sp,
                                                            color = Slate900,
                                                            fontWeight = FontWeight.Medium
                                                        ),
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = rowItem.item.id,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 9.sp,
                                                            color = Slate400
                                                        )
                                                    )
                                                }
                                            }

                                            // Col 2: BOBOT
                                            Box(
                                                modifier = Modifier
                                                    .width(col2Width)
                                                    .padding(horizontal = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = String.format(Locale.US, "%.1f", bobot),
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = Slate800,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }

                                            // Col 3: POINT (Max point)
                                            Box(
                                                modifier = Modifier
                                                    .width(col3Width)
                                                    .padding(horizontal = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$pointMax",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = Slate800,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }

                                            // Col 4: JUMLAH BOBOT ([BOBOT]*[POINT])
                                            Box(
                                                modifier = Modifier
                                                    .width(col4Width)
                                                    .padding(horizontal = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Surface(
                                                    color = Color(0xFFF1F5F9),
                                                    shape = RoundedCornerShape(3.dp)
                                                ) {
                                                    Text(
                                                        text = String.format(Locale.US, "%.1f", jumlahBobot),
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontWeight = FontWeight.Black,
                                                            color = Slate800,
                                                            fontSize = 11.sp
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }

                                            // Col 5: JUMLAH POINT (Poin yang diinput)
                                            Box(
                                                modifier = Modifier
                                                    .width(col5Width)
                                                    .padding(horizontal = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (jumlahPoint == 0) {
                                                    Text(
                                                        text = "0",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontWeight = FontWeight.Medium,
                                                            color = Color(0xFF16A34A),
                                                            fontSize = 11.sp
                                                        )
                                                    )
                                                } else {
                                                    Surface(
                                                        color = Color(0xFFFEF3C7),
                                                        shape = RoundedCornerShape(3.dp),
                                                        border = BorderStroke(0.5.dp, Amber600.copy(alpha = 0.6f))
                                                    ) {
                                                        Text(
                                                            text = "$jumlahPoint",
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

                                            // Col 6: JUMLAH SAMPLE
                                            Box(
                                                modifier = Modifier
                                                    .width(col6Width)
                                                    .padding(horizontal = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$jumlahSample",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Slate700,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }

                                            // Col 7: KETENTUAN SAMPLE
                                            Box(
                                                modifier = Modifier
                                                    .width(col7Width)
                                                    .padding(horizontal = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$ketentuanSample",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = HighDensityBlue,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }

                                            // Col 8: BELUM CEK
                                            Box(
                                                modifier = Modifier
                                                    .width(col8Width)
                                                    .padding(horizontal = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (belumCekItem == 0) {
                                                    Text(
                                                        text = "0",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFF16A34A),
                                                            fontSize = 11.sp
                                                        )
                                                    )
                                                } else {
                                                    Surface(
                                                        color = Color(0xFFFEE2E2),
                                                        shape = RoundedCornerShape(3.dp)
                                                    ) {
                                                        Text(
                                                            text = "$belumCekItem",
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                fontWeight = FontWeight.Black,
                                                                color = Color(0xFFDC2626),
                                                                fontSize = 10.5.sp
                                                            ),
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Col 9: TOTAL POINT TEMUAN
                                            Box(
                                                modifier = Modifier
                                                    .width(col9Width)
                                                    .padding(horizontal = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Surface(
                                                    color = Color(0xFFF1F5F9),
                                                    shape = RoundedCornerShape(3.dp)
                                                ) {
                                                    Text(
                                                        text = "$totalPointTemuan",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = Slate800,
                                                            fontSize = 11.sp
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }

                                            // Col 10: TOTAL BOBOT TEMUAN
                                            Box(
                                                modifier = Modifier
                                                    .width(col10Width)
                                                    .padding(horizontal = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Surface(
                                                    color = Color(0xFFFEF9C3),
                                                    shape = RoundedCornerShape(3.dp)
                                                ) {
                                                    Text(
                                                        text = String.format(Locale.US, "%.1f", totalBobotTemuan),
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontWeight = FontWeight.Black,
                                                            color = Color(0xFF854D0E),
                                                            fontSize = 11.sp
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }

                                            // Col 11: NILAI TOTAL
                                            Box(
                                                modifier = Modifier
                                                    .width(col11Width)
                                                    .padding(horizontal = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Surface(
                                                    color = Color(0xFFEFF6FF),
                                                    shape = RoundedCornerShape(3.dp)
                                                ) {
                                                    Text(
                                                        text = String.format(Locale.US, "%.2f", nilaiTotal),
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontWeight = FontWeight.Black,
                                                            color = HighDensityBlue,
                                                            fontSize = 11.sp
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = Slate200, thickness = 0.5.dp)
                                    }

                                    // TOTAL KESELURUHAN SUMMARY ROW
                                    Row(
                                        modifier = Modifier
                                            .background(Color(0xFF0F172A))
                                            .padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Col 1: TOTAL KESELURUHAN
                                        Box(
                                            modifier = Modifier
                                                .width(col1Width)
                                                .padding(start = 14.dp, end = 8.dp)
                                        ) {
                                            Text(
                                                text = "TOTAL KESELURUHAN",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }

                                        // Col 2: BOBOT
                                        Box(modifier = Modifier.width(col2Width), contentAlignment = Alignment.Center) {
                                            Text("-", color = Slate400, fontSize = 11.sp)
                                        }

                                        // Col 3: POINT
                                        Box(modifier = Modifier.width(col3Width), contentAlignment = Alignment.Center) {
                                            Text("$grandTotalPointMax", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }

                                        // Col 4: JUMLAH BOBOT
                                        Box(modifier = Modifier.width(col4Width), contentAlignment = Alignment.Center) {
                                            Text(String.format(Locale.US, "%.1f", grandTotalJumlahBobot), color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                        }

                                        // Col 5: JUMLAH POINT
                                        Box(modifier = Modifier.width(col5Width), contentAlignment = Alignment.Center) {
                                            Text("$grandTotalJumlahPoint", color = Color(0xFFFCD34D), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                        }

                                        // Col 6: JUMLAH SAMPLE
                                        Box(modifier = Modifier.width(col6Width), contentAlignment = Alignment.Center) {
                                            Text("$sampleCount", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }

                                        // Col 7: KETENTUAN SAMPLE
                                        Box(modifier = Modifier.width(col7Width), contentAlignment = Alignment.Center) {
                                            Text("$ketentuanSample", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }

                                        // Col 8: BELUM CEK
                                        Box(modifier = Modifier.width(col8Width), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "$belumCek",
                                                color = if (belumCek > 0) Color(0xFFF87171) else Color(0xFF4ADE80),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp
                                            )
                                        }

                                        // Col 9: TOTAL POINT TEMUAN
                                        Box(modifier = Modifier.width(col9Width), contentAlignment = Alignment.Center) {
                                            Text("$grandTotalPointTemuan", color = Color(0xFFFCA5A5), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                        }

                                        // Col 10: TOTAL BOBOT TEMUAN
                                        Box(modifier = Modifier.width(col10Width), contentAlignment = Alignment.Center) {
                                            Text(String.format(Locale.US, "%.1f", grandTotalBobotTemuan), color = Color(0xFFFDE047), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                        }

                                        // Col 11: NILAI TOTAL
                                        Box(modifier = Modifier.width(col11Width), contentAlignment = Alignment.Center) {
                                            Text(String.format(Locale.US, "%.2f", grandNilaiTotal), color = Color(0xFF38BDF8), fontWeight = FontWeight.Black, fontSize = 11.5.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SummaryMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate600
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = color,
                    fontSize = 15.sp
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.5.sp,
                    color = Slate500
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun exportBobotResumeCsv(
    context: Context,
    rows: List<BobotRowItem>,
    bobotMap: Map<String, Double>,
    findingsMap: Map<String, Int>,
    sampleCount: Int,
    ketentuanSample: Int,
    nilaiKpiPersen: Double = 0.0,
    kpiCucianPersen: Double = 0.0,
    subGroupSummaries: List<SubGroupSummaryItem> = emptyList(),
    totalPersenSemuaGroup: Double = 0.0,
    selectedPeriode: String,
    selectedPool: String
) {
    try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Bobot_Resume_${selectedPool}_${selectedPeriode}_$timeStamp.csv"
        val file = File(context.cacheDir, fileName)

        val writer = FileOutputStream(file).bufferedWriter()
        writer.appendLine("LAPORAN BOBOT RESUME AUDIT ARMADA")
        writer.appendLine("Filter Pool:,$selectedPool")
        writer.appendLine("Filter Periode:,$selectedPeriode")
        writer.appendLine("Jumlah Sample:,$sampleCount Mobil")
        writer.appendLine("Ketentuan Sample:,$ketentuanSample Mobil")
        writer.appendLine("Belum Cek:,${(ketentuanSample - sampleCount).coerceAtLeast(0)} Mobil")
        writer.appendLine("Nilai KPI:,${String.format(Locale.US, "%.2f", nilaiKpiPersen)}")
        writer.appendLine("KPI Cucian:,${String.format(Locale.US, "%.2f", kpiCucianPersen)}")
        writer.appendLine("Tanggal Ekspor:,${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
        writer.appendLine()

        if (subGroupSummaries.isNotEmpty()) {
            writer.appendLine("RINGKASAN BOBOT SUB GROUP")
            writer.appendLine("GROUP,SEKSI,NILAI")
            subGroupSummaries.forEach { sg ->
                val escGroup = "\"${sg.groupTitle.replace("\"", "\"\"")}\""
                val escSec = "\"${sg.sectionTitle.replace("\"", "\"\"")}\""
                writer.appendLine("$escGroup,$escSec,${String.format(Locale.US, "%.2f%%", sg.nilaiPersen)}")
            }
            writer.appendLine("\"TOTAL\",\"-\",${String.format(Locale.US, "%.2f%%", totalPersenSemuaGroup)}")
            writer.appendLine()
        }

        // CSV Header with 11 columns matching table:
        writer.appendLine("TABEL INPUT,SEKSI,SUB GRUP,BOBOT,POINT,JUMLAH BOBOT,JUMLAH POINT,JUMLAH SAMPLE,KETENTUAN SAMPLE,BELUM CEK,TOTAL POINT TEMUAN,TOTAL BOBOT TEMUAN,NILAI TOTAL")

        var totalPtMax = 0
        var totalJmlBobot = 0.0
        var totalJmlPoint = 0
        var totalPtTemuan = 0
        var totalBbtTemuan = 0.0

        rows.forEach { row ->
            val bobot = bobotMap[row.item.id] ?: row.item.defaultBobot
            val pointMax = row.item.maxPoints
            val jumlahBobot = bobot * pointMax
            val jumlahPoint = findingsMap[row.item.id] ?: 0
            val belumCekItem = (ketentuanSample - sampleCount).coerceAtLeast(0)
            val totalPointTemuan = (pointMax * sampleCount) - jumlahPoint
            val totalBobotTemuan = totalPointTemuan * bobot
            val nilaiTotal = if (ketentuanSample > 0) totalBobotTemuan / ketentuanSample else 0.0

            totalPtMax += pointMax
            totalJmlBobot += jumlahBobot
            totalJmlPoint += jumlahPoint
            totalPtTemuan += totalPointTemuan
            totalBbtTemuan += totalBobotTemuan

            val escapedName = "\"${row.item.name.replace("\"", "\"\"")}\""
            val escapedSection = "\"${row.sectionTitle.replace("\"", "\"\"")}\""
            val escapedGroup = "\"${row.groupTitle.replace("\"", "\"\"")}\""

            writer.appendLine("$escapedName,$escapedSection,$escapedGroup,$bobot,$pointMax,${String.format(Locale.US, "%.1f", jumlahBobot)},$jumlahPoint,$sampleCount,$ketentuanSample,$belumCekItem,$totalPointTemuan,${String.format(Locale.US, "%.1f", totalBobotTemuan)},${String.format(Locale.US, "%.2f", nilaiTotal)}")
        }

        val grandNilaiTotal = if (ketentuanSample > 0) totalBbtTemuan / ketentuanSample else 0.0
        val belumCekTotal = (ketentuanSample - sampleCount).coerceAtLeast(0)
        writer.appendLine("\"TOTAL KESELURUHAN\",\"-\",\"-\",\"-\",$totalPtMax,${String.format(Locale.US, "%.1f", totalJmlBobot)},$totalJmlPoint,$sampleCount,$ketentuanSample,$belumCekTotal,$totalPtTemuan,${String.format(Locale.US, "%.1f", totalBbtTemuan)},${String.format(Locale.US, "%.2f", grandNilaiTotal)}")

        writer.flush()
        writer.close()

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Data Bobot Resume (CSV)"))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal mengekspor CSV: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
