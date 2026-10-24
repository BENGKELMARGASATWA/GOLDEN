package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableChart
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.local.entity.DataAuditEntity
import com.example.data.local.entity.MasterBobotEntity
import com.example.data.local.entity.MasterGradeKpiEntity
import com.example.data.local.entity.MasterPeriodeEntity
import com.example.data.local.entity.MasterPoolEntity
import com.example.data.local.entity.MasterSampleEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.KpiAuditSpec
import com.example.data.model.KpiItem
import com.example.util.AuditFilterUtils
import com.example.ui.theme.HighDensityBlue
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data model for individual item calculation inside a Sub Group.
 */
data class SubGroupDetailItem(
    val item: KpiItem,
    val bobot: Double,
    val pointMax: Int,
    val totalPointMax: Int,
    val jumlahPoint: Int,
    val totalPointTemuan: Int,
    val totalBobotTemuan: Double,
    val nilaiPersenItem: Double
)

/**
 * Data model for Sub Group summary.
 */
data class SubGroupGroupSummary(
    val sectionId: String,
    val sectionTitle: String,
    val groupId: String,
    val groupTitle: String,
    val items: List<SubGroupDetailItem>,
    val totalBobotTemuan: Double,
    val nilaiPersen: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RekapNilaiSubGroupScreen(
    audits: List<DataAuditEntity>,
    bobotList: List<MasterBobotEntity>,
    periodes: List<MasterPeriodeEntity>,
    pools: List<MasterPoolEntity>,
    currentUser: UserEntity?,
    sampleList: List<MasterSampleEntity>,
    gradeList: List<MasterGradeKpiEntity> = emptyList()
) {
    val context = LocalContext.current

    val registeredPool = currentUser?.kodePool?.trim()?.uppercase() ?: ""
    val userLevel = currentUser?.levelAkses?.trim()?.uppercase() ?: ""
    val isRestrictedPool = registeredPool.isNotBlank() && !registeredPool.equals("SKT", ignoreCase = true) && !registeredPool.equals("ALL", ignoreCase = true) &&
        (userLevel == "KEPALA BENGKEL" || userLevel.contains("KABAG") || userLevel == "AUDITOR" || (userLevel != "SUPER ADMIN" && userLevel != "ADMIN" && userLevel != "SKT" && userLevel != "MASTER DATA"))

    val userPoolList = remember(registeredPool) {
        registeredPool.split(",", ";").map { it.trim().uppercase() }.filter { it.isNotBlank() }
    }

    val defaultPool = remember(currentUser, isRestrictedPool, userPoolList) {
        if (isRestrictedPool && userPoolList.isNotEmpty()) {
            if (userPoolList.size == 1) userPoolList.first() else "ALL"
        } else {
            "ALL"
        }
    }

    // Determine default periode
    var selectedPeriode by remember { mutableStateOf("ALL") }
    var selectedPool by remember(currentUser) {
        mutableStateOf(defaultPool)
    }

    val effectivePool = selectedPool

    // Dropdown expansion states
    var periodeExpanded by remember { mutableStateOf(false) }
    var poolExpanded by remember { mutableStateOf(false) }

    // Map bobot by itemId for instant O(1) lookup
    val bobotMap = remember(bobotList) {
        bobotList.associate { it.itemId to it.bobot }
    }

    // Filter audits based on selected Periode and Pool
    // Sesuai aturan: Pada menu REKAP NILAI AUDIT, jumlah sample adalah data yang di tampilkan pada Riwayat Audit
    // (jika terdapat no mobil yang sama pada satu periode, maka hanya di tampilkan pada input terakhir).
    val filteredAudits = remember(audits, selectedPeriode, selectedPool, isRestrictedPool, userPoolList) {
        val poolAndPeriodFiltered = audits.filter { audit ->
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
            matchPeriode && matchPool
        }
        val deduped = AuditFilterUtils.filterUniqueAudits(poolAndPeriodFiltered)
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

    // Sum of points earned per item
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

    // Determine Ketentuan Sample
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

    // Calculate Sub Group summaries
    val subGroupSummaries = remember(bobotMap, findingsMap, sampleCount, ketentuanSample) {
        val list = mutableListOf<SubGroupGroupSummary>()
        for (section in KpiAuditSpec.sections) {
            for (group in section.groups) {
                val detailItems = group.items.map { item ->
                    val b = bobotMap[item.id] ?: item.defaultBobot
                    val pointMax = item.maxPoints
                    val totalPointMax = pointMax * sampleCount
                    val jumlahPoint = findingsMap[item.id] ?: 0
                    val totalPointTemuan = totalPointMax - jumlahPoint
                    val totalBobotTemuan = totalPointTemuan * b
                    val nilaiPersenItem = if (ketentuanSample > 0) totalBobotTemuan / ketentuanSample else 0.0
                    SubGroupDetailItem(
                        item = item,
                        bobot = b,
                        pointMax = pointMax,
                        totalPointMax = totalPointMax,
                        jumlahPoint = jumlahPoint,
                        totalPointTemuan = totalPointTemuan,
                        totalBobotTemuan = totalBobotTemuan,
                        nilaiPersenItem = nilaiPersenItem
                    )
                }

                val totalBobotGroup = detailItems.sumOf { it.totalBobotTemuan }
                val nilaiPersenGroup = if (ketentuanSample > 0) totalBobotGroup / ketentuanSample else 0.0

                list.add(
                    SubGroupGroupSummary(
                        sectionId = section.id,
                        sectionTitle = section.title,
                        groupId = group.id,
                        groupTitle = group.title,
                        items = detailItems,
                        totalBobotTemuan = totalBobotGroup,
                        nilaiPersen = nilaiPersenGroup
                    )
                )
            }
        }
        list
    }

    // Total % of all sub groups
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

    // NILAI KPI: (( % SEKSI EXTERIOR + % SEKSI INTERIOR)/2)-20
    // Ketentuan: Nilai minimal 0 dan maksimal 80
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

    // GRADE KPI: Nilai diambil dari Nilai KPI yang dicocokkan dengan Master Grade KPI
    val matchedGrade = remember(nilaiKpiPersen, gradeList) {
        if (gradeList.isNotEmpty()) {
            val rounded = kotlin.math.round(nilaiKpiPersen * 100.0) / 100.0
            val found = gradeList.firstOrNull { 
                rounded >= it.minScore - 0.005 && rounded <= it.maxScore + 0.005 
            } ?: gradeList.firstOrNull { nilaiKpiPersen in it.minScore..it.maxScore }

            if (found != null) {
                found
            } else if (rounded >= 70.00) {
                gradeList.find { it.kategori.equals("A", ignoreCase = true) }
            } else if (rounded <= 29.99) {
                gradeList.find { it.kategori.equals("E", ignoreCase = true) }
            } else {
                gradeList.minByOrNull { kotlin.math.abs(it.minScore - rounded) }
            }
        } else {
            // Default rentang Grade jika database belum termuat
            when {
                nilaiKpiPersen >= 70.00 -> MasterGradeKpiEntity("A", 70.00, 80.00, "Sangat Baik (Grade A)")
                nilaiKpiPersen >= 60.00 -> MasterGradeKpiEntity("B", 60.00, 69.99, "Baik (Grade B)")
                nilaiKpiPersen >= 50.00 -> MasterGradeKpiEntity("C", 50.00, 59.99, "Cukup (Grade C)")
                nilaiKpiPersen >= 30.00 -> MasterGradeKpiEntity("D", 30.00, 49.99, "Kurang (Grade D)")
                else -> MasterGradeKpiEntity("E", 0.00, 29.99, "Kritis / Perlu Perbaikan (Grade E)")
            }
        }
    }

    val gradeKategori = matchedGrade?.kategori ?: "-"
    val gradeKeterangan = matchedGrade?.keterangan.orEmpty().ifBlank { "Kategori $gradeKategori" }
    val gradeColor = when (gradeKategori.trim().uppercase()) {
        "A" -> Color(0xFF0D9488) // Teal
        "B" -> Color(0xFF2563EB) // Blue
        "C" -> Color(0xFFD97706) // Amber
        "D" -> Color(0xFFEA580C) // Orange
        "E" -> Color(0xFFDC2626) // Red
        else -> Color(0xFF4F46E5) // Indigo
    }

    // Expanded rows map for drilling down into item details
    val expandedRows = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. KPI SUMMARY METRIC CARDS
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Baris 1: Sample, NILAI KPI, KPI CUCIAN
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
                        modifier = Modifier.weight(1f)
                    )

                    // Card 2: NILAI KPI (Persen dihilangkan, desimal 2 angka di belakang koma, Min 0 - Maks 80)
                    SummaryMetricCard(
                        title = "NILAI KPI",
                        value = formattedNilaiKpi,
                        subtitle = "Min: 0 | Maks: 80",
                        icon = Icons.Default.Assessment,
                        color = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f)
                    )

                    // Card 3: KPI CUCIAN (Cucian dikurangi 20 dalam desimal 2 digit)
                    SummaryMetricCard(
                        title = "KPI CUCIAN",
                        value = formattedKpiCucian,
                        subtitle = "Cucian - 20",
                        icon = Icons.Default.LocalCarWash,
                        color = Color(0xFF7C3AED),
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("card_kpi_cucian")
                    )
                }

                // Baris 2: KOTAK DARI SEKSI (Seperti Kotak JUMLAH SAMPLE)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card 4: EXTERIOR (dibagi 5)
                    SummaryMetricCard(
                        title = "EXTERIOR",
                        value = "${Math.round(persenExterior)}",
                        subtitle = "Hasil Bagi 5",
                        icon = Icons.Default.DirectionsCar,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f)
                    )

                    // Card 5: INTERIOR (dibagi 5)
                    SummaryMetricCard(
                        title = "INTERIOR",
                        value = "${Math.round(persenInterior)}",
                        subtitle = "Hasil Bagi 5",
                        icon = Icons.Default.Assessment,
                        color = Color(0xFF0D9488),
                        modifier = Modifier.weight(1f)
                    )

                    // Card 6: CUCIAN (dibagi 3)
                    SummaryMetricCard(
                        title = "CUCIAN",
                        value = "${Math.round(persenCucian)}",
                        subtitle = "Hasil Bagi 3",
                        icon = Icons.Default.TableChart,
                        color = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. FILTERS (Periode & Pool) - Ramping & Otomatis Mengikuti Ukuran Huruf
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_filters_rekap_nilai")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Label Filter Ramping
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = HighDensityBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "FILTER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate700,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }

                    // Periode Dropdown Box (Ramping & Otomatis Mengikuti Ukuran Huruf)
                    val periodeOptions = remember(periodes) {
                        listOf("ALL") + periodes.map { it.periode }.distinct()
                    }
                    Box(modifier = Modifier.wrapContentSize()) {
                        Surface(
                            onClick = { periodeExpanded = true },
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, if (periodeExpanded) HighDensityBlue else Slate300),
                            color = Slate50,
                            modifier = Modifier
                                .wrapContentWidth()
                                .testTag("dropdown_periode_rekap_nilai")
                        ) {
                            Row(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "Periode:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Slate500
                                    )
                                )
                                Text(
                                    text = selectedPeriode,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                )
                                Icon(
                                    imageVector = if (periodeExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Slate600,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = periodeExpanded,
                            onDismissRequest = { periodeExpanded = false }
                        ) {
                            periodeOptions.forEach { p ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (p == "ALL") "Semua Periode (ALL)" else p,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (p == selectedPeriode) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedPeriode = p
                                        periodeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Pool Dropdown Box (Ramping & Otomatis Mengikuti Ukuran Huruf)
                    val poolOptions = remember(pools, isRestrictedPool, userPoolList) {
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
                    Box(modifier = Modifier.wrapContentSize()) {
                        val poolDisplayText = if (selectedPool == "ALL") {
                            if (isRestrictedPool && userPoolList.size > 1) "Semua Pool Saya" else "Semua Pool (ALL)"
                        } else {
                            selectedPool
                        }
                        Surface(
                            onClick = { poolExpanded = true },
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, if (poolExpanded) HighDensityBlue else Slate300),
                            color = Slate50,
                            modifier = Modifier
                                .wrapContentWidth()
                                .testTag("dropdown_pool_rekap_nilai")
                        ) {
                            Row(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "Pool:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Slate500
                                    )
                                )
                                Text(
                                    text = poolDisplayText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                )
                                Icon(
                                    imageVector = if (poolExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Slate600,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = poolExpanded,
                            onDismissRequest = { poolExpanded = false }
                        ) {
                            poolOptions.forEach { p ->
                                val itemText = if (p == "ALL") {
                                    if (isRestrictedPool && userPoolList.size > 1) "Semua Pool Saya" else "Semua Pool (ALL)"
                                } else {
                                    p
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = itemText,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (p == selectedPool) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedPool = p
                                        poolExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Reset Filter button jika bukan default
                    if (selectedPeriode != "ALL" || selectedPool != defaultPool) {
                        Surface(
                            onClick = {
                                selectedPeriode = "ALL"
                                selectedPool = defaultPool
                            },
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset Filter",
                                    tint = HighDensityBlue,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Reset",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HighDensityBlue
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. CORE TABLE: REKAP NILAI % SUB GROUP
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_rekap_nilai_table")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Table Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "REKAP NILAI AUDIT KEINDAHAN",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        letterSpacing = 0.4.sp
                                    )
                                )
                                Text(
                                    text = "Periode: $selectedPeriode | Pool: $effectivePool",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        // Action Buttons: Copy Summary & Export CSV
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val summaryText = buildString {
                                        appendLine("=== REKAP NILAI AUDIT KEINDAHAN ===")
                                        appendLine("Periode: $selectedPeriode")
                                        appendLine("Pool: $effectivePool")
                                        appendLine("Jumlah Sample: $sampleCount / $ketentuanSample Unit (Belum Cek: $belumCek)")
                                        appendLine("--------------------------------")
                                        appendLine("RINGKASAN SEKSI:")
                                        appendLine("• EXTERIOR (/5): ${Math.round(persenExterior)}")
                                        appendLine("• INTERIOR (/5): ${Math.round(persenInterior)}")
                                        appendLine("• CUCIAN (/3): ${Math.round(persenCucian)}")
                                        appendLine("--------------------------------")
                                        appendLine("NILAI KPI: $formattedNilaiKpi")
                                        appendLine("KPI CUCIAN: $formattedKpiCucian")
                                        appendLine("GRADE KPI: Grade $gradeKategori ($gradeKeterangan)")
                                        appendLine("--------------------------------")
                                        subGroupSummaries.forEach { sg ->
                                            appendLine("${sg.groupTitle}: ${Math.round(sg.nilaiPersen)}")
                                        }
                                        appendLine("--------------------------------")
                                        appendLine("TOTAL: ${Math.round(totalPersenSemuaGroup)}")
                                    }
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Rekap Nilai Sub Group", summaryText))
                                    Toast.makeText(context, "Ringkasan disalin ke clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Salin Ringkasan",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            IconButton(
                                onClick = {
                                    exportRekapNilaiCsv(
                                        context = context,
                                        subGroupSummaries = subGroupSummaries,
                                        totalPersenSemuaGroup = totalPersenSemuaGroup,
                                        sampleCount = sampleCount,
                                        ketentuanSample = ketentuanSample,
                                        nilaiKpiPersen = nilaiKpiPersen,
                                        persenExterior = persenExterior,
                                        persenInterior = persenInterior,
                                        persenCucian = persenCucian,
                                        selectedPeriode = selectedPeriode,
                                        selectedPool = effectivePool,
                                        gradeKategori = gradeKategori,
                                        gradeKeterangan = gradeKeterangan
                                    )
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Ekspor CSV",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Table Column Headers: Kolom 1 = GROUP | Kolom 2 = NILAI
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(HighDensityBlue)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GROUP",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 11.5.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "NILAI",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 11.5.sp
                            ),
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(110.dp)
                        )
                    }

                    // Table Body with Section Headers
                    var currentSection = ""
                    subGroupSummaries.forEachIndexed { index, subGroup ->
                        if (subGroup.sectionTitle != currentSection) {
                            currentSection = subGroup.sectionTitle
                            val (displaySecTitle, secPersen) = when {
                                subGroup.sectionTitle.contains("EXTERIOR", ignoreCase = true) ->
                                    "A. REKAP TEMUAN KPI AUDIT EXTERIOR" to persenExterior
                                subGroup.sectionTitle.contains("INTERIOR", ignoreCase = true) ->
                                    "B. REKAP TEMUAN KPI INTERIOR" to persenInterior
                                subGroup.sectionTitle.contains("CUCIAN", ignoreCase = true) ->
                                    "C. CUCIAN" to persenCucian
                                else ->
                                    subGroup.sectionTitle to subGroupSummaries.filter { it.sectionTitle == subGroup.sectionTitle }.sumOf { it.nilaiPersen }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE2E8F0))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = displaySecTitle,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Slate800,
                                        fontSize = 10.5.sp
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${Math.round(secPersen)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Slate800,
                                        fontSize = 11.sp
                                    ),
                                    textAlign = TextAlign.End
                                )
                            }
                            HorizontalDivider(color = Slate300, thickness = 0.5.dp)
                        }

                        val isRowExpanded = expandedRows[subGroup.groupId] ?: false
                        val rowBg = if (index % 2 == 0) Color.White else Color(0xFFF8FAFC)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowBg)
                                    .clickable {
                                        expandedRows[subGroup.groupId] = !isRowExpanded
                                    }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isRowExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Slate600,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = subGroup.groupTitle,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = Slate900,
                                            fontSize = 12.sp
                                        )
                                    )
                                }

                                Text(
                                    text = "${Math.round(subGroup.nilaiPersen)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = if (subGroup.nilaiPersen > 0) Color(0xFF0284C7) else Slate600,
                                        fontSize = 12.sp
                                    ),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(110.dp)
                                )
                            }

                            // Optional Detail Drill-down for the items in this Sub Group
                            if (isRowExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF1F5F9))
                                        .padding(start = 28.dp, end = 12.dp, top = 4.dp, bottom = 6.dp)
                                ) {
                                    subGroup.items.forEach { detail ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "• ${detail.item.name}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Slate700,
                                                    fontSize = 11.sp
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "Bobot: ${detail.bobot} | Temuan: ${detail.totalPointTemuan} (${Math.round(detail.nilaiPersenItem)})",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (detail.totalPointTemuan > 0) Color(0xFFDC2626) else Color(0xFF16A34A),
                                                    fontSize = 10.5.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                        }
                    }

                    // TOTAL ROW
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Slate900,
                                fontSize = 12.5.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${Math.round(totalPersenSemuaGroup)}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0284C7),
                                fontSize = 13.sp
                            ),
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(110.dp)
                        )
                    }
                }
            }
        }



        // 4. ACTION BAR (Export CSV Button)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        exportRekapNilaiCsv(
                            context = context,
                            subGroupSummaries = subGroupSummaries,
                            totalPersenSemuaGroup = totalPersenSemuaGroup,
                            sampleCount = sampleCount,
                            ketentuanSample = ketentuanSample,
                            nilaiKpiPersen = nilaiKpiPersen,
                            persenExterior = persenExterior,
                            persenInterior = persenInterior,
                            persenCucian = persenCucian,
                            selectedPeriode = selectedPeriode,
                            selectedPool = effectivePool,
                            gradeKategori = gradeKategori,
                            gradeKeterangan = gradeKeterangan
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityBlue),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_export_rekap_nilai_csv")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EKSPOR LAPORAN REKAP KE CSV",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Metric summary card component.
 */
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate600,
                        fontSize = 8.5.sp
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
                    color = Slate600,
                    fontSize = 8.5.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Helper to export the Rekap Nilai Audit Keindahan to CSV.
 */
private fun exportRekapNilaiCsv(
    context: Context,
    subGroupSummaries: List<SubGroupGroupSummary>,
    totalPersenSemuaGroup: Double,
    sampleCount: Int,
    ketentuanSample: Int,
    nilaiKpiPersen: Double,
    persenExterior: Double,
    persenInterior: Double,
    persenCucian: Double,
    selectedPeriode: String,
    selectedPool: String,
    gradeKategori: String = "-",
    gradeKeterangan: String = ""
) {
    try {
        val fileName = "REKAP_NILAI_AUDIT_KEINDAHAN_${selectedPool}_${selectedPeriode}_${System.currentTimeMillis()}.csv"
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, fileName)

        FileWriter(file).use { writer ->
            writer.appendLine("REKAP NILAI AUDIT KEINDAHAN")
            writer.appendLine("Periode:,$selectedPeriode")
            writer.appendLine("Pool:,$selectedPool")
            writer.appendLine("Jumlah Sample:,$sampleCount Mobil")
            writer.appendLine("Ketentuan Sample:,$ketentuanSample Mobil")
            writer.appendLine("Belum Cek:,${(ketentuanSample - sampleCount).coerceAtLeast(0)} Mobil")
            writer.appendLine("--------------------------------")
            writer.appendLine("EXTERIOR (/5):,${Math.round(persenExterior)}")
            writer.appendLine("INTERIOR (/5):,${Math.round(persenInterior)}")
            writer.appendLine("CUCIAN (/3):,${Math.round(persenCucian)}")
            val formattedKpi = String.format(Locale.US, "%.2f", nilaiKpiPersen)
            val kpiCucianVal = (persenCucian - 20.0).coerceIn(0.0, 80.0)
            val formattedCucian = String.format(Locale.US, "%.2f", kpiCucianVal)
            writer.appendLine("NILAI KPI:,$formattedKpi")
            writer.appendLine("KPI CUCIAN:,$formattedCucian")
            writer.appendLine("GRADE KPI:,Grade $gradeKategori ($gradeKeterangan)")
            writer.appendLine("Total Semua Group:,${Math.round(totalPersenSemuaGroup)}")
            writer.appendLine("Tanggal Ekspor:,${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            writer.appendLine()

            // Main Table: GROUP | NILAI
            writer.appendLine("SEKSI,GROUP,TOTAL BOBOT TEMUAN,NILAI")
            subGroupSummaries.forEach { sg ->
                val escSec = "\"${sg.sectionTitle.replace("\"", "\"\"")}\""
                val escGroup = "\"${sg.groupTitle.replace("\"", "\"\"")}\""
                val bbt = String.format(Locale.US, "%.1f", sg.totalBobotTemuan)
                val nl = "${Math.round(sg.nilaiPersen)}"
                writer.appendLine("$escSec,$escGroup,$bbt,$nl")
            }
            writer.appendLine("\"-\",\"TOTAL\",\"-\",${Math.round(totalPersenSemuaGroup)}")
            writer.appendLine()

            // Detail per KPI Item
            writer.appendLine("RINCIAN ITEM PER SUB GROUP")
            writer.appendLine("SEKSI,SUB GROUP,ID ITEM,NAMA ITEM,BOBOT,POINT MAKS,TOTAL POINT MAKS,JUMLAH POINT,TOTAL POINT TEMUAN,TOTAL BOBOT TEMUAN,KONTRIBUSI NILAI")
            subGroupSummaries.forEach { sg ->
                sg.items.forEach { detail ->
                    val escSec = "\"${sg.sectionTitle.replace("\"", "\"\"")}\""
                    val escGroup = "\"${sg.groupTitle.replace("\"", "\"\"")}\""
                    val escName = "\"${detail.item.name.replace("\"", "\"\"")}\""
                    writer.appendLine(
                        "$escSec,$escGroup,\"${detail.item.id}\",$escName,${detail.bobot},${detail.pointMax},${detail.totalPointMax},${detail.jumlahPoint},${detail.totalPointTemuan},${String.format(Locale.US, "%.1f", detail.totalBobotTemuan)},${Math.round(detail.nilaiPersenItem)}"
                    )
                }
            }
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Rekap Nilai Audit Keindahan - $selectedPool")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Rekap Nilai CSV"))
        Toast.makeText(context, "File CSV berhasil dibuat", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal mengekspor CSV: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
