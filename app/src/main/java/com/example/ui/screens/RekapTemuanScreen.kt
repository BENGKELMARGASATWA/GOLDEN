package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.local.entity.DataAuditEntity
import com.example.data.local.entity.MasterPeriodeEntity
import com.example.data.local.entity.MasterPoolEntity
import com.example.data.local.entity.MasterSampleEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.KpiAuditSpec
import com.example.data.model.KpiGroup
import com.example.data.model.KpiItem
import com.example.data.model.KpiSection
import com.example.util.AuditFilterUtils
import com.example.ui.theme.*
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data audit per Pool untuk tabel multi-pool
 */
data class PoolAuditData(
    val poolCode: String,
    val sampleOp: Int,
    val sampleSvc: Int,
    val totalSample: Int,
    val parsedOp: List<Pair<DataAuditEntity, Map<String, Int>>>,
    val parsedSvc: List<Pair<DataAuditEntity, Map<String, Int>>>,
    val totalUnitOp: Int,
    val totalUnitSvc: Int,
    val totalPoinOp: Int,
    val totalPoinSvc: Int,
    val pctOp: Double,
    val pctSvc: Double,
    val totalUnitTot: Int = totalUnitOp + totalUnitSvc,
    val totalPoinTot: Int = totalPoinOp + totalPoinSvc,
    val pctTot: Double = if (totalSample > 0) ((totalUnitOp + totalUnitSvc).toDouble() / totalSample.toDouble()) * 100.0 else 0.0
)

/**
 * Screen REKAP TEMUAN
 *
 * Menampilkan tabel matriks temuan audit dengan 3 kolom per Pool:
 * - Kolom 1: DATA INPUT (Daftar item / titik pemeriksaan audit keindahan)
 * - Kolom 2: OPERASIONAL (Total Unit Temuan Operasional / Total Sample Operasional dalam %)
 * - Kolom 3: SERVICE (Total Unit Temuan Service / Total Sample Service dalam %)
 * - Kolom 4: TOTAL (Total Unit Temuan Gabungan Op + Svc / Total Sample Gabungan dalam %)
 *
 * Aturan Khusus:
 * - Operasional: total unit temuan dibagi total sample operasional dalam %
 * - Service: total unit temuan service dibagi total sample service dalam %
 * - Total: total unit temuan (Op + Svc) dibagi total sample (Op + Svc) dalam %
 * - Nilai persentase berwarna merah jika di atas 50%
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RekapTemuanScreen(
    audits: List<DataAuditEntity>,
    periodes: List<MasterPeriodeEntity>,
    pools: List<MasterPoolEntity>,
    sampleList: List<MasterSampleEntity> = emptyList(),
    currentUser: UserEntity?
) {
    val context = LocalContext.current

    // Cek pembatasan pool pengguna
    val registeredPool = currentUser?.kodePool?.trim()?.uppercase() ?: ""
    val userLevel = currentUser?.levelAkses?.trim()?.uppercase() ?: ""
    val isRestrictedPool = registeredPool.isNotBlank() &&
            !registeredPool.equals("SKT", ignoreCase = true) &&
            !registeredPool.equals("ALL", ignoreCase = true) &&
            (userLevel == "KEPALA BENGKEL" || userLevel.contains("KABAG") || userLevel == "AUDITOR" ||
                    (userLevel != "SUPER ADMIN" && userLevel != "ADMIN" && userLevel != "SKT" && userLevel != "MASTER DATA"))

    val isSuperAdminOrAdminOrSkt = userLevel == "SUPER ADMIN" || userLevel == "ADMIN" || userLevel == "SKT" ||
            userLevel.contains("ADMIN") || userLevel.contains("SKT")

    val userPoolList = remember(registeredPool) {
        registeredPool.split(",", ";").map { it.trim().uppercase() }.filter { it.isNotBlank() }
    }

    val defaultPoolForUser = remember(currentUser, isSuperAdminOrAdminOrSkt, isRestrictedPool, userPoolList) {
        if (isSuperAdminOrAdminOrSkt) {
            "SKT"
        } else if (isRestrictedPool && userPoolList.isNotEmpty()) {
            if (userPoolList.size == 1) userPoolList.first() else "ALL"
        } else {
            if (userPoolList.isNotEmpty()) userPoolList.first() else "ALL"
        }
    }

    // Map pool to Status Area (default "JABODETABEK" jika tidak ditentukan)
    val poolToAreaMap = remember(pools) {
        pools.associate { it.kodePool.trim().uppercase() to (it.statusArea.ifBlank { "JABODETABEK" }.trim().uppercase()) }
    }

    // Standard & dynamic Status Area options
    val statusAreaOptions = remember(pools, sampleList) {
        val standard = listOf("SEMUA AREA", "JABODETABEK", "JABODETABEK SATELIT", "DAERAH", "DAERAH SATELIT")
        val fromPools = pools.map { it.statusArea.trim().uppercase() }.filter { it.isNotBlank() }
        val fromSamples = sampleList.map { it.statusArea.trim().uppercase() }.filter { it.isNotBlank() }
        (standard + fromPools + fromSamples).distinct()
    }

    // State filter
    var selectedPeriode by remember { mutableStateOf("ALL") }
    var selectedStatusArea by remember { mutableStateOf("SEMUA AREA") }
    var selectedPool by remember(currentUser) {
        mutableStateOf(defaultPoolForUser)
    }
    val effectivePool = selectedPool

    LaunchedEffect(currentUser) {
        if (isSuperAdminOrAdminOrSkt && (selectedPool == "ALL" || selectedPool.isBlank())) {
            selectedPool = "SKT"
        }
    }

    // Jika Status Area berubah dan pool yang dipilih bukan ALL serta tidak berada di area terpilih, reset ke ALL
    LaunchedEffect(selectedStatusArea) {
        if (selectedStatusArea != "SEMUA AREA" && selectedPool != "ALL") {
            val poolArea = poolToAreaMap[selectedPool.trim().uppercase()] ?: "JABODETABEK"
            if (!poolArea.equals(selectedStatusArea, ignoreCase = true)) {
                selectedPool = "ALL"
            }
        }
    }

    // Dropdown visibility
    var periodeExpanded by remember { mutableStateOf(false) }
    var statusAreaExpanded by remember { mutableStateOf(false) }
    var poolExpanded by remember { mutableStateOf(false) }

    // Modal detail temuan per item
    var selectedItemDetail by remember { mutableStateOf<Pair<KpiItem, List<FindingDetail>>?>(null) }

    // Fitur Layar Penuh (Fullscreen) untuk tabel matriks temuan
    var isFullScreenTable by rememberSaveable { mutableStateOf(false) }

    // Filter audits berdasarkan Periode, Status Area & Pool terpilih
    // Sesuai aturan: Pada menu REKAP TEMUAN, data diambil dari data Riwayat Audit
    // (jika terdapat no mobil yang sama pada satu periode, maka hanya ditampilkan pada input terakhir).
    val baseAudits = remember(audits, selectedPeriode, selectedStatusArea, selectedPool, poolToAreaMap, isRestrictedPool, userPoolList) {
        val poolAndPeriodFiltered = audits.filter { audit ->
            val matchPeriode = if (selectedPeriode == "ALL") true else audit.periode.equals(selectedPeriode, ignoreCase = true)
            
            val matchStatusArea = if (selectedStatusArea == "SEMUA AREA") {
                true
            } else {
                val pArea = poolToAreaMap[audit.pool.trim().uppercase()] ?: "JABODETABEK"
                pArea.equals(selectedStatusArea, ignoreCase = true)
            }

            val matchPool = if (isRestrictedPool) {
                if (selectedPool == "ALL") {
                    userPoolList.any { it.equals(audit.pool.trim(), ignoreCase = true) }
                } else {
                    audit.pool.equals(selectedPool, ignoreCase = true)
                }
            } else {
                if (selectedPool == "ALL") true else audit.pool.equals(selectedPool, ignoreCase = true)
            }
            matchPeriode && matchStatusArea && matchPool
        }
        val deduped = AuditFilterUtils.filterUniqueAudits(poolAndPeriodFiltered)
        AuditFilterUtils.filterLatestPerCarAndPeriod(deduped)
    }

    // Daftar pool yang akan ditampilkan sebagai kolom ke kanan di tabel:
    // Jika selectedPool != "ALL", tampilkan pool tersebut saja.
    // Jika "ALL":
    //   - Jika isRestrictedPool, tampilkan pool-pool milik user (userPoolList) yang sesuai Status Area.
    //   - Jika bebas, tampilkan pool yang ada di database atau terdapat auditnya sesuai Status Area, urut alfabet.
    val displayedPoolList = remember(selectedPool, selectedStatusArea, poolToAreaMap, isRestrictedPool, userPoolList, pools, baseAudits) {
        if (selectedPool != "ALL") {
            listOf(selectedPool)
        } else {
            if (isRestrictedPool && userPoolList.isNotEmpty()) {
                if (selectedStatusArea == "SEMUA AREA") {
                    userPoolList
                } else {
                    val filtered = userPoolList.filter { p ->
                        val area = poolToAreaMap[p] ?: "JABODETABEK"
                        area.equals(selectedStatusArea, ignoreCase = true)
                    }
                    if (filtered.isEmpty()) listOf("ALL") else filtered
                }
            } else {
                val poolsFromAudits = baseAudits.map { it.pool.trim().uppercase() }.filter { it.isNotBlank() }.distinct()
                val masterPoolCodes = if (selectedStatusArea == "SEMUA AREA") {
                    pools.map { it.kodePool.trim().uppercase() }.filter { it.isNotBlank() }.distinct()
                } else {
                    pools.filter { it.statusArea.equals(selectedStatusArea, ignoreCase = true) }
                        .map { it.kodePool.trim().uppercase() }
                        .filter { it.isNotBlank() }
                        .distinct()
                }
                val combined = (poolsFromAudits + masterPoolCodes).distinct().sorted()
                if (combined.isEmpty()) listOf("ALL") else combined
            }
        }
    }

    // Hitung data audit per Pool untuk kolom-kolom tabel
    val poolDataMap = remember(displayedPoolList, baseAudits) {
        displayedPoolList.associateWith { pCode ->
            val pAudits = if (pCode == "ALL") baseAudits else baseAudits.filter { it.pool.equals(pCode, ignoreCase = true) }
            val pOp = pAudits.filter { it.jenisSample.equals("OPERASIONAL", ignoreCase = true) }
            val pSvc = pAudits.filter { it.jenisSample.equals("SERVICE", ignoreCase = true) }
            val sOp = pOp.size
            val sSvc = pSvc.size
            val totS = sOp + sSvc
            val parsedOp = pOp.map { it to parseItemScores(it.kpiScoresJson) }
            val parsedSvc = pSvc.map { it to parseItemScores(it.kpiScoresJson) }
            val uOp = parsedOp.sumOf { pair -> pair.second.values.count { it > 0 } }
            val uSvc = parsedSvc.sumOf { pair -> pair.second.values.count { it > 0 } }
            val ptOp = parsedOp.sumOf { pair -> pair.second.values.sum() }
            val ptSvc = parsedSvc.sumOf { pair -> pair.second.values.sum() }
            val pctOp = if (sOp > 0) (uOp.toDouble() / sOp.toDouble()) * 100.0 else 0.0
            val pctSvc = if (sSvc > 0) (uSvc.toDouble() / sSvc.toDouble()) * 100.0 else 0.0

            val uTot = uOp + uSvc
            val ptTot = ptOp + ptSvc
            val pctTot = if (totS > 0) (uTot.toDouble() / totS.toDouble()) * 100.0 else 0.0

            PoolAuditData(
                poolCode = pCode,
                sampleOp = sOp,
                sampleSvc = sSvc,
                totalSample = totS,
                parsedOp = parsedOp,
                parsedSvc = parsedSvc,
                totalUnitOp = uOp,
                totalUnitSvc = uSvc,
                totalPoinOp = ptOp,
                totalPoinSvc = ptSvc,
                pctOp = pctOp,
                pctSvc = pctSvc,
                totalUnitTot = uTot,
                totalPoinTot = ptTot,
                pctTot = pctTot
            )
        }
    }

    // Pisahkan seluruh baseAudits menjadi OPERASIONAL dan SERVICE untuk KPI Header keseluruhan
    val operasionalAudits = remember(baseAudits) {
        baseAudits.filter { it.jenisSample.equals("OPERASIONAL", ignoreCase = true) }
    }
    val serviceAudits = remember(baseAudits) {
        baseAudits.filter { it.jenisSample.equals("SERVICE", ignoreCase = true) }
    }

    val sampleCountOp = operasionalAudits.size
    val sampleCountSvc = serviceAudits.size
    val totalSampleAll = sampleCountOp + sampleCountSvc

    // Parse skor dari seluruh audit agar cepat
    val parsedOpScores = remember(operasionalAudits) {
        operasionalAudits.map { audit -> audit to parseItemScores(audit.kpiScoresJson) }
    }
    val parsedSvcScores = remember(serviceAudits) {
        serviceAudits.map { audit -> audit to parseItemScores(audit.kpiScoresJson) }
    }

    // Ambil struktur seksi dan grup dari KpiAuditSpec (seluruh seksi ditampilkan)
    val sections = KpiAuditSpec.sections
    val displayedSections = sections

    // Hitung total item yang melewati batas 50% untuk kartu ringkasan
    val criticalCount = remember(parsedOpScores, parsedSvcScores, sampleCountOp, sampleCountSvc, totalSampleAll) {
        val allItems = KpiAuditSpec.getAllItems()
        var count = 0
        allItems.forEach { item ->
            val unitOp = parsedOpScores.count { (it.second[item.id] ?: 0) > 0 }
            val pctOp = if (sampleCountOp > 0) (unitOp.toDouble() / sampleCountOp.toDouble()) * 100.0 else 0.0

            val unitSvc = parsedSvcScores.count { (it.second[item.id] ?: 0) > 0 }
            val pctSvc = if (sampleCountSvc > 0) (unitSvc.toDouble() / sampleCountSvc.toDouble()) * 100.0 else 0.0

            val unitTot = unitOp + unitSvc
            val pctTot = if (totalSampleAll > 0) (unitTot.toDouble() / totalSampleAll.toDouble()) * 100.0 else 0.0

            if (pctOp > 50.0 || pctSvc > 50.0 || pctTot > 50.0) {
                count++
            }
        }
        count
    }

    // Hitung total unit mobil dengan temuan & poin keseluruhan
    val totalUnitOp = remember(parsedOpScores) {
        parsedOpScores.sumOf { pair -> pair.second.values.count { it > 0 } }
    }
    val totalUnitSvc = remember(parsedSvcScores) {
        parsedSvcScores.sumOf { pair -> pair.second.values.count { it > 0 } }
    }
    val totalPoinSemuaOp = remember(parsedOpScores) {
        parsedOpScores.sumOf { pair -> pair.second.values.sum() }
    }
    val totalPoinSemuaSvc = remember(parsedSvcScores) {
        parsedSvcScores.sumOf { pair -> pair.second.values.sum() }
    }

    val overallPctOp = if (sampleCountOp > 0) (totalUnitOp.toDouble() / sampleCountOp.toDouble()) * 100.0 else 0.0
    val overallPctSvc = if (sampleCountSvc > 0) (totalUnitSvc.toDouble() / sampleCountSvc.toDouble()) * 100.0 else 0.0

    Scaffold(
        containerColor = HighDensityBg,
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, Slate200)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(HighDensityBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Assessment,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "REKAP TEMUAN",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = Slate900
                                    )
                                )
                            }
                        }

                        // Action Buttons: Salin Ringkasan & Ekspor CSV
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val summaryText = buildRekapTemuanSummary(
                                        periode = selectedPeriode,
                                        statusArea = selectedStatusArea,
                                        pool = effectivePool,
                                        sampleOp = sampleCountOp,
                                        sampleSvc = sampleCountSvc,
                                        totalSample = totalSampleAll,
                                        criticalCount = criticalCount,
                                        sections = displayedSections,
                                        parsedOp = parsedOpScores,
                                        parsedSvc = parsedSvcScores
                                    )
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Rekap Temuan", summaryText))
                                    Toast.makeText(context, "Ringkasan Rekap Temuan berhasil disalin!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Salin Ringkasan",
                                    tint = Slate600,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            IconButton(
                                onClick = {
                                    exportRekapTemuanCsv(
                                        context = context,
                                        selectedPeriode = selectedPeriode,
                                        selectedStatusArea = selectedStatusArea,
                                        selectedPool = effectivePool,
                                        displayedPoolList = displayedPoolList,
                                        poolDataMap = poolDataMap,
                                        poolToAreaMap = poolToAreaMap,
                                        sampleCountOp = sampleCountOp,
                                        sampleCountSvc = sampleCountSvc,
                                        totalSampleAll = totalSampleAll,
                                        sections = displayedSections
                                    )
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Ekspor CSV",
                                    tint = HighDensityBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Dropdown Filter Bar (Periode, Status Area, & Pool) - Ramping & Otomatis Mengikuti Ukuran Huruf
                    val periodeOptions = remember(periodes) {
                        listOf("ALL") + periodes.map { it.periode }.distinct()
                    }
                    val poolOptions = remember(pools, isRestrictedPool, userPoolList, selectedStatusArea, poolToAreaMap) {
                        val baseList = if (isRestrictedPool && userPoolList.isNotEmpty()) {
                            val list = mutableListOf<String>()
                            if (userPoolList.size > 1) {
                                list.add("ALL")
                            }
                            list.addAll(userPoolList)
                            list.distinct()
                        } else {
                            val list = mutableListOf<String>()
                            list.add("SKT")
                            list.add("ALL")
                            val poolCodes = pools.map { it.kodePool.trim().uppercase() }.filter { it.isNotBlank() }.distinct()
                            list.addAll(poolCodes)
                            list.distinct()
                        }

                        if (selectedStatusArea == "SEMUA AREA") {
                            baseList
                        } else {
                            baseList.filter { p ->
                                if (p == "ALL") true
                                else {
                                    val area = poolToAreaMap[p] ?: "JABODETABEK"
                                    area.equals(selectedStatusArea, ignoreCase = true)
                                }
                            }
                        }
                    }

                    // Baris 1: Filter Periode & Reset
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
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

                        // Filter Periode Dropdown Box (Ramping & Otomatis Mengikuti Ukuran Huruf)
                        Box(modifier = Modifier.wrapContentSize()) {
                            Surface(
                                onClick = { periodeExpanded = true },
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, if (periodeExpanded) HighDensityBlue else Slate300),
                                color = Slate50,
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .testTag("dropdown_periode_rekap_temuan")
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

                        // Reset Filter button jika bukan default
                        if (selectedPeriode != "ALL" || selectedStatusArea != "SEMUA AREA" || selectedPool != defaultPoolForUser) {
                            Surface(
                                onClick = {
                                    selectedPeriode = "ALL"
                                    selectedStatusArea = "SEMUA AREA"
                                    selectedPool = defaultPoolForUser
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

                    Spacer(modifier = Modifier.height(6.dp))

                    // Baris 2: Filter Area dan Filter Pool (di bawah filter Periode)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Filter Status Area Dropdown Box (Ramping & Otomatis Mengikuti Ukuran Huruf)
                        Box(modifier = Modifier.wrapContentSize()) {
                            Surface(
                                onClick = { statusAreaExpanded = true },
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, if (statusAreaExpanded) HighDensityBlue else if (selectedStatusArea != "SEMUA AREA") Color(0xFF16A34A) else Slate300),
                                color = if (selectedStatusArea != "SEMUA AREA") Color(0xFFF0FDF4) else Slate50,
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .testTag("dropdown_status_area_rekap_temuan")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .wrapContentWidth()
                                        .padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "Area:",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = if (selectedStatusArea != "SEMUA AREA") Color(0xFF15803D) else Slate500
                                        )
                                    )
                                    Text(
                                        text = selectedStatusArea,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedStatusArea != "SEMUA AREA") Color(0xFF166534) else Slate900
                                        )
                                    )
                                    Icon(
                                        imageVector = if (statusAreaExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = if (selectedStatusArea != "SEMUA AREA") Color(0xFF16A34A) else Slate600,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = statusAreaExpanded,
                                onDismissRequest = { statusAreaExpanded = false }
                            ) {
                                statusAreaOptions.forEach { area ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = area,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (area == selectedStatusArea) FontWeight.Bold else FontWeight.Normal,
                                                color = if (area == selectedStatusArea) Color(0xFF16A34A) else Color.Unspecified
                                            )
                                        },
                                        onClick = {
                                            selectedStatusArea = area
                                            statusAreaExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Filter Pool Dropdown Box (Ramping & Otomatis Mengikuti Ukuran Huruf)
                        val poolDisplayText = if (selectedPool == "ALL") {
                            if (isRestrictedPool && userPoolList.size > 1) "Semua Pool Saya" else "Semua Pool (ALL)"
                        } else {
                            selectedPool
                        }
                        Box(modifier = Modifier.wrapContentSize()) {
                            Surface(
                                onClick = {
                                    poolExpanded = true
                                },
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, if (poolExpanded) HighDensityBlue else Slate300),
                                color = Slate50,
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .testTag("dropdown_pool_rekap_temuan")
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
                                    val labelText = if (p == "ALL") {
                                        if (isRestrictedPool && userPoolList.size > 1) "Semua Pool Saya" else "Semua Pool (ALL)"
                                    } else {
                                        p
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = labelText,
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
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. KPI Metric Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card 1: Sample Operasional
                    KpiMetricCard(
                        title = "SAMPLE OPERASIONAL",
                        value = "$sampleCountOp Mobil",
                        subtitle = "Temuan: $totalUnitOp Unit (${String.format(Locale.US, "%.1f%%", overallPctOp)})",
                        icon = Icons.Default.DirectionsCar,
                        color = HighDensityBlue,
                        modifier = Modifier.weight(1f)
                    )

                    // Card 2: Sample Service
                    KpiMetricCard(
                        title = "SAMPLE SERVICE",
                        value = "$sampleCountSvc Mobil",
                        subtitle = "Temuan: $totalUnitSvc Unit (${String.format(Locale.US, "%.1f%%", overallPctSvc)})",
                        icon = Icons.Default.Build,
                        color = Color(0xFF0D9488), // Teal
                        modifier = Modifier.weight(1f)
                    )

                    // Card 3: Total Sample (OP+SVC) & Temuan Kritis (> 50%)
                    val totalUnitSemua = totalUnitOp + totalUnitSvc
                    val overallPctSemua = if (totalSampleAll > 0) (totalUnitSemua.toDouble() / totalSampleAll.toDouble()) * 100.0 else 0.0
                    KpiMetricCard(
                        title = "SAMPLE OP + SVC",
                        value = "$totalSampleAll Mobil",
                        subtitle = "Total Temuan: $totalUnitSemua (${String.format(Locale.US, "%.1f%%", overallPctSemua)})",
                        icon = Icons.Default.Warning,
                        color = if (criticalCount > 0) Color(0xFFDC2626) else Color(0xFF16A34A),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. Info Bar Pool & Total Sample
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Pool: ${if (effectivePool == "ALL") "SEMUA" else effectivePool}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700,
                                    fontSize = 11.sp
                                )
                            )
                            if (selectedStatusArea != "SEMUA AREA") {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(0.5.dp, Color(0xFF86EFAC))
                                ) {
                                    Text(
                                        text = selectedStatusArea,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF166534),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Total Sample: $totalSampleAll Mobil",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate600,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // 3. TABEL MULTI-POOL (FREEZE HEADER EXCEL & KLIK FULLSCREEN)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header Card: Title, Badge Freeze, dan Tombol Klik Full
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = HighDensityBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "MATRIKS TEMUAN AUDIT",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Slate900,
                                        fontSize = 12.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(0.5.dp, Color(0xFF93C5FD))
                                ) {
                                    Text(
                                        text = "FREEZE EXCEL",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = HighDensityBlue,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.5.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Tombol Klik Full
                            FilledTonalButton(
                                onClick = { isFullScreenTable = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFEFF6FF),
                                    contentColor = HighDensityBlue
                                ),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("btn_open_table_fullscreen")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Buka Fullscreen",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Klik Full",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        // Banner Interaktif: Klik untuk Fullscreen
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF0FDF4))
                                .clickable { isFullScreenTable = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.OpenInFull,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Header terkunci di atas. Ketuk di sini untuk Buka Layar Penuh (Full Screen)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF15803D),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Hint Scroll jika lebih dari 1 pool
                        if (displayedPoolList.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFEFF6FF))
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = HighDensityBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Geser tabel ke kanan untuk melihat perbandingan antar Pool (${displayedPoolList.size} Pool)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = HighDensityBlue,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }

                        HorizontalDivider(color = Slate200, thickness = 1.dp)

                        // Render Tabel Matriks Temuan dengan Freeze Header Excel & Interaktivitas Penuh
                        RekapTemuanMatrixTable(
                            displayedPoolList = displayedPoolList,
                            poolDataMap = poolDataMap,
                            displayedSections = displayedSections,
                            poolToAreaMap = poolToAreaMap,
                            selectedStatusArea = selectedStatusArea,
                            isFullScreen = false,
                            onToggleFullScreen = { isFullScreenTable = true },
                            onItemClick = { item, findings ->
                                selectedItemDetail = item to findings
                            }
                        )
                    }
                }
            }

            // 4. Tombol Ekspor CSV di Bawah
            item {
                Button(
                    onClick = {
                        exportRekapTemuanCsv(
                            context = context,
                            selectedPeriode = selectedPeriode,
                            selectedStatusArea = selectedStatusArea,
                            selectedPool = effectivePool,
                            displayedPoolList = displayedPoolList,
                            poolDataMap = poolDataMap,
                            poolToAreaMap = poolToAreaMap,
                            sampleCountOp = sampleCountOp,
                            sampleCountSvc = sampleCountSvc,
                            totalSampleAll = totalSampleAll,
                            sections = displayedSections
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_export_rekap_temuan_csv")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EKSPOR DATA KE CSV (EXCEL)",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Modal Dialog Detail Temuan Item
    selectedItemDetail?.let { (item, findings) ->
        ItemFindingsDetailDialog(
            item = item,
            findings = findings,
            onDismiss = { selectedItemDetail = null }
        )
    }

    // Modal Dialog Layar Penuh (Fullscreen) untuk Tabel Rekap Temuan
    if (isFullScreenTable) {
        Dialog(
            onDismissRequest = { isFullScreenTable = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = HighDensityBg
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Bar Fullscreen
                    Surface(
                        color = Color.White,
                        shadowElevation = 2.dp,
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { isFullScreenTable = false },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FullscreenExit,
                                            contentDescription = "Tutup Layar Penuh",
                                            tint = Slate700
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "REKAP TEMUAN",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 15.sp,
                                                    color = Slate900
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = HighDensityBlue,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "FULL SCREEN",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 8.5.sp
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Periode: $selectedPeriode • Area: $selectedStatusArea • Pool: $effectivePool • Sample: $totalSampleAll Smp",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Slate500,
                                                fontSize = 9.5.sp
                                            )
                                        )
                                    }
                                }

                                Button(
                                    onClick = { isFullScreenTable = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tutup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Tabel Matriks Fullscreen (mengisi seluruh sisa layar)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.White)
                    ) {
                        RekapTemuanMatrixTable(
                            displayedPoolList = displayedPoolList,
                            poolDataMap = poolDataMap,
                            displayedSections = displayedSections,
                            poolToAreaMap = poolToAreaMap,
                            selectedStatusArea = selectedStatusArea,
                            isFullScreen = true,
                            onToggleFullScreen = { isFullScreenTable = false },
                            onItemClick = { item, findings ->
                                selectedItemDetail = item to findings
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tabel Matriks Rekap Temuan dengan fitur Freeze Header ala Excel:
 * - Super Header Pool & Sub-Header (OPERASIONAL | SERVICE | TOTAL) terkunci di atas (Freeze).
 * - Tepat di bawah OPERASIONAL | SERVICE | TOTAL terdapat garis pemisah tegas dan bayangan freeze.
 * - Baris data dapat di-scroll vertikal secara bebas di bawah freeze header.
 * - Pinned Total Temuan terkunci di bagian bawah.
 * - Seluruh baris dapat diklik (Full Clickable) untuk memunculkan rincian kendaraan.
 */
@Composable
private fun RekapTemuanMatrixTable(
    displayedPoolList: List<String>,
    poolDataMap: Map<String, PoolAuditData>,
    displayedSections: List<KpiSection>,
    poolToAreaMap: Map<String, String> = emptyMap(),
    selectedStatusArea: String = "SEMUA AREA",
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    onItemClick: (KpiItem, List<FindingDetail>) -> Unit,
    modifier: Modifier = Modifier
) {
    val tableHorizontalScroll = rememberScrollState()
    val tableVerticalScroll = rememberScrollState()

    val colDataInputWidth = 220.dp
    val colPoolSubWidth = 92.dp
    val dividerSubWidth = 1.5.dp // Garis jika masih 1 pool (antar Operasional, Service & Total)
    val dividerPoolWidth = 2.dp  // Garis jika ke pool lain (antar Pool)

    val dividerSubColor = Color(0xFF2563EB)        // Biru jika masih 1 pool
    val dividerPoolColor = Color(0xFFDC2626)       // Merah jika ke pool lain

    val dividerSubColorHeader = Color(0xFF93C5FD)  // Biru cerah di sub-header
    val dividerPoolColorHeader = Color(0xFFEF4444) // Merah cerah di header

    val colPoolTotalWidth = (colPoolSubWidth * 3) + (dividerSubWidth * 2) // Lebar 1 blok Pool termasuk sub OPERASIONAL, SERVICE & TOTAL
    val totalTableWidth = colDataInputWidth + ((colPoolTotalWidth + dividerPoolWidth) * displayedPoolList.size)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(tableHorizontalScroll)
    ) {
        Column(
            modifier = Modifier
                .width(totalTableWidth)
                .then(
                    if (isFullScreen) Modifier.fillMaxHeight()
                    else Modifier.wrapContentHeight()
                )
        ) {
            // =========================================================================
            // 1. FREEZE HEADER TINGKAT 1: SUPER HEADER POOL (Terkunci / Freeze di atas)
            // =========================================================================
            Row(
                modifier = Modifier
                    .width(totalTableWidth)
                    .background(HighDensityBlue)
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kolom Kiri: DATA INPUT (Freeze Header)
                Box(
                    modifier = Modifier
                        .width(colDataInputWidth)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "DATA INPUT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                // Kolom-kolom Pool ke kanan
                displayedPoolList.forEach { poolCode ->
                    val pData = poolDataMap[poolCode]
                    val pTotalSample = pData?.totalSample ?: 0
                    val pSampleOp = pData?.sampleOp ?: 0
                    val pSampleSvc = pData?.sampleSvc ?: 0

                    // Garis Merah ke pool lain
                    Box(
                        modifier = Modifier
                            .width(dividerPoolWidth)
                            .fillMaxHeight()
                            .background(dividerPoolColorHeader)
                    )

                    Box(
                        modifier = Modifier
                            .width(colPoolTotalWidth)
                            .padding(horizontal = 4.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (poolCode == "ALL") "SEMUA POOL" else "POOL $poolCode",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val pArea = if (poolCode == "ALL") {
                                if (selectedStatusArea != "SEMUA AREA") selectedStatusArea else "SEMUA AREA"
                            } else {
                                poolToAreaMap[poolCode] ?: "JABODETABEK"
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(
                                    text = pArea,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp)
                                )
                            }
                            Text(
                                text = "Total $pTotalSample Smp ($pSampleOp Op + $pSampleSvc Svc)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.5.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 2. FREEZE HEADER TINGKAT 2: SUB-HEADER (OPERASIONAL | SERVICE | TOTAL)
            // =========================================================================
            Row(
                modifier = Modifier
                    .width(totalTableWidth)
                    .background(Color(0xFF1D4ED8)) // Biru gelap
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Label Kolom Kiri: KATEGORI / ITEM TEMUAN
                Box(
                    modifier = Modifier
                        .width(colDataInputWidth)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "KATEGORI / ITEM TEMUAN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 9.5.sp
                        )
                    )
                }

                // Sub-kolom OPERASIONAL, SERVICE & TOTAL untuk tiap Pool
                displayedPoolList.forEach { poolCode ->
                    val pData = poolDataMap[poolCode]
                    val pTotalSample = pData?.totalSample ?: 0
                    val pSampleOp = pData?.sampleOp ?: 0
                    val pSampleSvc = pData?.sampleSvc ?: 0

                    // Garis Merah ke pool lain
                    Box(
                        modifier = Modifier
                            .width(dividerPoolWidth)
                            .fillMaxHeight()
                            .background(dividerPoolColorHeader)
                    )

                    // 1. Sub-kolom OPERASIONAL
                    Box(
                        modifier = Modifier
                            .width(colPoolSubWidth)
                            .padding(horizontal = 2.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "OPERASIONAL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontSize = 9.5.sp
                                )
                            )
                            Text(
                                text = "($pSampleOp Smp)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 8.sp
                                )
                            )
                        }
                    }

                    // Garis Biru jika masih 1 pool
                    Box(
                        modifier = Modifier
                            .width(dividerSubWidth)
                            .fillMaxHeight()
                            .background(dividerSubColorHeader)
                    )

                    // 2. Sub-kolom SERVICE
                    Box(
                        modifier = Modifier
                            .width(colPoolSubWidth)
                            .padding(horizontal = 2.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "SERVICE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontSize = 9.5.sp
                                )
                            )
                            Text(
                                text = "($pSampleSvc Smp)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 8.sp
                                )
                            )
                        }
                    }

                    // Garis Biru jika masih 1 pool (antar SERVICE dan TOTAL)
                    Box(
                        modifier = Modifier
                            .width(dividerSubWidth)
                            .fillMaxHeight()
                            .background(dividerSubColorHeader)
                    )

                    // 3. Sub-kolom TOTAL (OPERASIONAL + SERVICE)
                    Box(
                        modifier = Modifier
                            .width(colPoolSubWidth)
                            .padding(horizontal = 2.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "TOTAL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFDE047), // Kuning aksen
                                    fontSize = 9.5.sp
                                )
                            )
                            Text(
                                text = "($pTotalSample Smp)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontSize = 8.sp
                                )
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 3. GARIS FREEZE EXCEL (TEPAT DI BAWAH OPERASIONAL | SERVICE | TOTAL)
            // =========================================================================
            Box(
                modifier = Modifier
                    .width(totalTableWidth)
                    .height(2.5.dp)
                    .background(Color(0xFF172554)) // Border navy tegas batas freeze
            )
            Box(
                modifier = Modifier
                    .width(totalTableWidth)
                    .height(4.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color(0x40000000),
                                Color(0x15000000),
                                Color.Transparent
                            )
                        )
                    )
            )

            // =========================================================================
            // 4. DAFTAR BARIS ITEM TEMUAN (SCROLLABLE VERTICALLY DI BAWAH FREEZE HEADER)
            // =========================================================================
            Box(
                modifier = Modifier
                    .width(totalTableWidth)
                    .then(
                        if (isFullScreen) Modifier.weight(1f)
                        else Modifier.heightIn(min = 280.dp, max = 460.dp)
                    )
                    .verticalScroll(tableVerticalScroll)
            ) {
                Column(modifier = Modifier.width(totalTableWidth)) {
                    if (displayedSections.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .width(totalTableWidth)
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada item temuan yang cocok",
                                color = Slate500,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        displayedSections.forEach { section ->
                            // Section Banner
                            Row(
                                modifier = Modifier
                                    .width(totalTableWidth)
                                    .background(Color(0xFFE2E8F0))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
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

                            HorizontalDivider(color = Slate300, thickness = 0.5.dp)

                            section.groups.forEach { group ->
                                // Group Banner
                                Row(
                                    modifier = Modifier
                                        .width(totalTableWidth)
                                        .background(Color(0xFFF8FAFC))
                                        .padding(horizontal = 14.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
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

                                HorizontalDivider(color = Slate200, thickness = 0.5.dp)

                                // Items Rows
                                group.items.forEachIndexed { itemIdx, item ->
                                    val isEven = itemIdx % 2 == 0

                                    // Cek apakah ada pool yang kritis pada item ini
                                    val hasCriticalInAnyPool = displayedPoolList.any { pCode ->
                                        val pData = poolDataMap[pCode]
                                        if (pData != null && pData.totalSample > 0) {
                                            val uOp = pData.parsedOp.count { (it.second[item.id] ?: 0) > 0 }
                                            val uSvc = pData.parsedSvc.count { (it.second[item.id] ?: 0) > 0 }
                                            val uTot = uOp + uSvc
                                            val pOp = if (pData.sampleOp > 0) (uOp.toDouble() / pData.sampleOp.toDouble()) * 100.0 else 0.0
                                            val pSvc = if (pData.sampleSvc > 0) (uSvc.toDouble() / pData.sampleSvc.toDouble()) * 100.0 else 0.0
                                            val pTot = if (pData.totalSample > 0) (uTot.toDouble() / pData.totalSample.toDouble()) * 100.0 else 0.0
                                            pOp > 50.0 || pSvc > 50.0 || pTot > 50.0
                                        } else false
                                    }

                                    Row(
                                        modifier = Modifier
                                            .width(totalTableWidth)
                                            .height(IntrinsicSize.Min)
                                            .background(
                                                if (hasCriticalInAnyPool) Color(0xFFFFF7ED)
                                                else if (isEven) Color.White
                                                else Color(0xFFFBFDFF)
                                            )
                                            .clickable {
                                                val allOpFindings = displayedPoolList.flatMap { pCode ->
                                                    val pData = poolDataMap[pCode]
                                                    pData?.parsedOp?.mapNotNull { (audit, map) ->
                                                        val score = map[item.id] ?: 0
                                                        if (score > 0) FindingDetail(audit.noMobil, audit.platNomer, "${audit.pool} - OP", score) else null
                                                    } ?: emptyList()
                                                }
                                                val allSvcFindings = displayedPoolList.flatMap { pCode ->
                                                    val pData = poolDataMap[pCode]
                                                    pData?.parsedSvc?.mapNotNull { (audit, map) ->
                                                        val score = map[item.id] ?: 0
                                                        if (score > 0) FindingDetail(audit.noMobil, audit.platNomer, "${audit.pool} - SVC", score) else null
                                                    } ?: emptyList()
                                                }
                                                onItemClick(item, allOpFindings + allSvcFindings)
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // KOLOM 1: DATA INPUT
                                        Box(
                                            modifier = Modifier
                                                .width(colDataInputWidth)
                                                .padding(horizontal = 10.dp, vertical = 7.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    text = item.name,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Slate900,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                                Text(
                                                    text = "Maks ${item.maxPoints} Poin",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Slate400,
                                                        fontSize = 8.5.sp
                                                    )
                                                )
                                            }
                                        }

                                        // KOLOM-KOLOM POOL KE KANAN
                                        displayedPoolList.forEach { poolCode ->
                                            val pData = poolDataMap[poolCode]
                                            val pTotSample = pData?.totalSample ?: 0
                                            val pSampleOp = pData?.sampleOp ?: 0
                                            val pSampleSvc = pData?.sampleSvc ?: 0

                                            val unitOp = pData?.parsedOp?.count { (it.second[item.id] ?: 0) > 0 } ?: 0
                                            val ptOp = pData?.parsedOp?.sumOf { it.second[item.id] ?: 0 } ?: 0
                                            val pctOp = if (pSampleOp > 0) (unitOp.toDouble() / pSampleOp.toDouble()) * 100.0 else 0.0
                                            val isOpCritical = pctOp > 50.0

                                            val unitSvc = pData?.parsedSvc?.count { (it.second[item.id] ?: 0) > 0 } ?: 0
                                            val ptSvc = pData?.parsedSvc?.sumOf { it.second[item.id] ?: 0 } ?: 0
                                            val pctSvc = if (pSampleSvc > 0) (unitSvc.toDouble() / pSampleSvc.toDouble()) * 100.0 else 0.0
                                            val isSvcCritical = pctSvc > 50.0

                                            val unitTot = unitOp + unitSvc
                                            val ptTot = ptOp + ptSvc
                                            val pctTot = if (pTotSample > 0) (unitTot.toDouble() / pTotSample.toDouble()) * 100.0 else 0.0
                                            val isTotCritical = pctTot > 50.0

                                            // Garis Merah ke pool lain
                                            Box(
                                                modifier = Modifier
                                                    .width(dividerPoolWidth)
                                                    .fillMaxHeight()
                                                    .background(dividerPoolColor)
                                            )

                                            // 1. Sub-kolom OPERASIONAL
                                            Box(
                                                modifier = Modifier
                                                    .width(colPoolSubWidth)
                                                    .padding(horizontal = 2.dp, vertical = 5.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                RatioPill(
                                                    units = unitOp,
                                                    points = ptOp,
                                                    sampleCount = pSampleOp,
                                                    percentage = pctOp,
                                                    isCritical = isOpCritical
                                                )
                                            }

                                            // Garis Biru jika masih 1 pool
                                            Box(
                                                modifier = Modifier
                                                    .width(dividerSubWidth)
                                                    .fillMaxHeight()
                                                    .background(dividerSubColor)
                                            )

                                            // 2. Sub-kolom SERVICE
                                            Box(
                                                modifier = Modifier
                                                    .width(colPoolSubWidth)
                                                    .padding(horizontal = 2.dp, vertical = 5.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                RatioPill(
                                                    units = unitSvc,
                                                    points = ptSvc,
                                                    sampleCount = pSampleSvc,
                                                    percentage = pctSvc,
                                                    isCritical = isSvcCritical
                                                )
                                            }

                                            // Garis Biru jika masih 1 pool (antar SERVICE dan TOTAL)
                                            Box(
                                                modifier = Modifier
                                                    .width(dividerSubWidth)
                                                    .fillMaxHeight()
                                                    .background(dividerSubColor)
                                            )

                                            // 3. Sub-kolom TOTAL (OPERASIONAL + SERVICE)
                                            Box(
                                                modifier = Modifier
                                                    .width(colPoolSubWidth)
                                                    .padding(horizontal = 2.dp, vertical = 5.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                RatioPill(
                                                    units = unitTot,
                                                    points = ptTot,
                                                    sampleCount = pTotSample,
                                                    percentage = pctTot,
                                                    isCritical = isTotCritical
                                                )
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

            // =========================================================================
            // 5. GARIS FREEZE BAWAH (DI ATAS TOTAL TEMUAN)
            // =========================================================================
            Box(
                modifier = Modifier
                    .width(totalTableWidth)
                    .height(3.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x15000000),
                                Color(0x35000000)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .width(totalTableWidth)
                    .height(2.dp)
                    .background(Color(0xFFCBD5E1))
            )

            // =========================================================================
            // 6. BARIS PINNED TOTAL TEMUAN (FROZEN DI BAWAH)
            // =========================================================================
            Row(
                modifier = Modifier
                    .width(totalTableWidth)
                    .background(Color(0xFFF1F5F9))
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(colDataInputWidth)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = "TOTAL TEMUAN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = Slate900,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = "Rasio & Total Poin (Op, Svc, Tot)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Slate500,
                                fontSize = 8.5.sp
                            )
                        )
                    }
                }

                // Total per Pool (Operasional, Service & Total)
                displayedPoolList.forEach { poolCode ->
                    val pData = poolDataMap[poolCode]
                    val pTotSample = pData?.totalSample ?: 0
                    val pSampleOp = pData?.sampleOp ?: 0
                    val pSampleSvc = pData?.sampleSvc ?: 0
                    val pTotUnitOp = pData?.totalUnitOp ?: 0
                    val pTotUnitSvc = pData?.totalUnitSvc ?: 0
                    val pTotPoinOp = pData?.totalPoinOp ?: 0
                    val pTotPoinSvc = pData?.totalPoinSvc ?: 0
                    val pPctOp = pData?.pctOp ?: 0.0
                    val pPctSvc = pData?.pctSvc ?: 0.0

                    val pTotUnitTot = pData?.totalUnitTot ?: (pTotUnitOp + pTotUnitSvc)
                    val pTotPoinTot = pData?.totalPoinTot ?: (pTotPoinOp + pTotPoinSvc)
                    val pPctTot = pData?.pctTot ?: (if (pTotSample > 0) (pTotUnitTot.toDouble() / pTotSample.toDouble()) * 100.0 else 0.0)

                    // Garis Merah ke pool lain
                    Box(
                        modifier = Modifier
                            .width(dividerPoolWidth)
                            .fillMaxHeight()
                            .background(dividerPoolColor)
                    )

                    // Total Operasional Pool
                    Box(
                        modifier = Modifier
                            .width(colPoolSubWidth)
                            .padding(horizontal = 2.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        RatioPill(
                            units = pTotUnitOp,
                            points = pTotPoinOp,
                            sampleCount = pSampleOp,
                            percentage = pPctOp,
                            isCritical = pPctOp > 50.0
                        )
                    }

                    // Garis Biru jika masih 1 pool (antar OPERASIONAL dan SERVICE)
                    Box(
                        modifier = Modifier
                            .width(dividerSubWidth)
                            .fillMaxHeight()
                            .background(dividerSubColor)
                    )

                    // Total Service Pool
                    Box(
                        modifier = Modifier
                            .width(colPoolSubWidth)
                            .padding(horizontal = 2.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        RatioPill(
                            units = pTotUnitSvc,
                            points = pTotPoinSvc,
                            sampleCount = pSampleSvc,
                            percentage = pPctSvc,
                            isCritical = pPctSvc > 50.0
                        )
                    }

                    // Garis Biru jika masih 1 pool (antar SERVICE dan TOTAL)
                    Box(
                        modifier = Modifier
                            .width(dividerSubWidth)
                            .fillMaxHeight()
                            .background(dividerSubColor)
                    )

                    // Total Gabungan (OPERASIONAL + SERVICE) Pool
                    Box(
                        modifier = Modifier
                            .width(colPoolSubWidth)
                            .padding(horizontal = 2.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        RatioPill(
                            units = pTotUnitTot,
                            points = pTotPoinTot,
                            sampleCount = pTotSample,
                            percentage = pPctTot,
                            isCritical = pPctTot > 50.0
                        )
                    }
                }
            }
        }
    }
}

/**
 * Komponen Pil / Badge untuk menampilkan nilai rasio hasil temuan (Jumlah Unit / Jumlah Sample)
 * Jika rasio > 50% maka akan berwarna merah tebal dengan background lembut.
 */
@Composable
private fun RatioPill(
    units: Int,
    points: Int = units,
    sampleCount: Int,
    percentage: Double,
    isCritical: Boolean
) {
    if (sampleCount == 0) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "-",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Slate400,
                    fontSize = 11.sp
                )
            )
        }
        return
    }

    val isZero = units == 0

    Surface(
        color = when {
            isCritical -> Color(0xFFFEE2E2) // Soft Red Pill
            isZero -> Color(0xFFDCFCE7)    // Soft Green
            else -> Color(0xFFF1F5F9)      // Slate Neutral
        },
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(
            1.dp,
            when {
                isCritical -> Color(0xFFFCA5A5) // Red border
                isZero -> Color(0xFF86EFAC)     // Green border
                else -> Color(0xFFCBD5E1)       // Slate border
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = if (units > 0) 3.dp else 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format(Locale.US, "%.1f%%", percentage),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = when {
                        isCritical -> Color(0xFFDC2626) // Red 600
                        isZero -> Color(0xFF15803D)     // Green 700
                        else -> Slate800
                    },
                    fontSize = 11.5.sp
                )
            )
            if (units > 0 && sampleCount > 0) {
                Text(
                    text = "$units unit / $sampleCount sample",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = when {
                            isCritical -> Color(0xFFB91C1C)
                            else -> Slate600
                        },
                        fontSize = 8.5.sp
                    )
                )
            }
        }
    }
}

/**
 * Kartu Metrik KPI di bagian atas
 */
@Composable
private fun KpiMetricCard(
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
                    fontSize = 14.sp
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
 * Dialog rincian kendaraan yang memiliki temuan pada item tertentu
 */
private data class FindingDetail(
    val noMobil: String,
    val platNomer: String,
    val jenisSample: String,
    val score: Int
)

@Composable
private fun ItemFindingsDetailDialog(
    item: KpiItem,
    findings: List<FindingDetail>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate300),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "RINCIAN TEMUAN",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = Slate900
                            )
                        )
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = HighDensityBlue,
                                fontSize = 13.sp
                            )
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate500)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Slate200)
                Spacer(modifier = Modifier.height(10.dp))

                if (findings.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada unit mobil yang ditemukan kendala pada item ini (Standar / 0 Poin).",
                            color = Slate500,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = "Ditemukan pada ${findings.size} unit mobil:",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(findings) { f ->
                            Surface(
                                color = Slate50,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Slate200)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "${f.noMobil} (${f.platNomer.ifBlank { "-" }})",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Slate900,
                                                fontSize = 12.sp
                                            )
                                        )
                                        Text(
                                            text = "Sample: ${f.jenisSample}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Slate500,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }

                                    Surface(
                                        color = Color(0xFFFEE2E2),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(0.5.dp, Color(0xFFFCA5A5))
                                    ) {
                                        Text(
                                            text = "${f.score} Poin",
                                            color = Color(0xFFDC2626),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tutup", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Parsing JSON skor temuan
 */
private fun parseItemScores(jsonStr: String): Map<String, Int> {
    if (jsonStr.isBlank()) return emptyMap()
    return try {
        val json = JSONObject(jsonStr)
        val map = mutableMapOf<String, Int>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            map[k] = json.optInt(k, 0)
        }
        map
    } catch (e: Exception) {
        emptyMap()
    }
}

/**
 * Membangun teks ringkasan untuk clipboard
 */
private fun buildRekapTemuanSummary(
    periode: String,
    statusArea: String = "SEMUA AREA",
    pool: String,
    sampleOp: Int,
    sampleSvc: Int,
    totalSample: Int,
    criticalCount: Int,
    sections: List<KpiSection>,
    parsedOp: List<Pair<DataAuditEntity, Map<String, Int>>>,
    parsedSvc: List<Pair<DataAuditEntity, Map<String, Int>>>
): String {
    val sb = StringBuilder()
    sb.appendLine("=== REKAP TEMUAN AUDIT KEINDAHAN ===")
    sb.appendLine("Periode: $periode | Status Area: $statusArea | Pool: $pool")
    sb.appendLine("Sample Realisasi: Operasional: $sampleOp Mobil | Service: $sampleSvc Mobil")
    sb.appendLine("Total Sample (Op + Svc): $totalSample Mobil")
    sb.appendLine("Formula Hasil Temuan:")
    sb.appendLine("- Operasional: Unit Temuan / Sample Operasional ($sampleOp Smp)")
    sb.appendLine("- Service: Unit Temuan / Sample Service ($sampleSvc Smp)")
    sb.appendLine("- Total: (Unit Temuan Op + Svc) / Total Sample ($totalSample Smp)")
    sb.appendLine("Temuan Kritis (> 50%): $criticalCount Titik Pemeriksaan")
    sb.appendLine("----------------------------------------------------------------------")
    sb.appendLine(String.format("%-25s | %-16s | %-16s | %-16s", "DATA INPUT", "OPERASIONAL", "SERVICE", "TOTAL TEMUAN"))
    sb.appendLine("----------------------------------------------------------------------")

    sections.forEach { sec ->
        sb.appendLine("[${sec.title}]")
        sec.groups.forEach { grp ->
            grp.items.forEach { item ->
                val unitOp = parsedOp.count { (it.second[item.id] ?: 0) > 0 }
                val pctOp = if (sampleOp > 0) (unitOp.toDouble() / sampleOp.toDouble()) * 100.0 else 0.0
                val tagOp = if (pctOp > 50.0) "(KRITIS)" else ""

                val unitSvc = parsedSvc.count { (it.second[item.id] ?: 0) > 0 }
                val pctSvc = if (sampleSvc > 0) (unitSvc.toDouble() / sampleSvc.toDouble()) * 100.0 else 0.0
                val tagSvc = if (pctSvc > 50.0) "(KRITIS)" else ""

                val unitTot = unitOp + unitSvc
                val pctTot = if (totalSample > 0) (unitTot.toDouble() / totalSample.toDouble()) * 100.0 else 0.0
                val tagTot = if (pctTot > 50.0) "(KRITIS)" else ""

                val opStr = String.format(Locale.US, "%d unit (%.1f%%) %s", unitOp, pctOp, tagOp).trim()
                val svcStr = String.format(Locale.US, "%d unit (%.1f%%) %s", unitSvc, pctSvc, tagSvc).trim()
                val totStr = String.format(Locale.US, "%d unit (%.1f%%) %s", unitTot, pctTot, tagTot).trim()

                sb.appendLine(String.format("%-25s | %-16s | %-16s | %-16s", item.name.take(25), opStr, svcStr, totStr))
            }
        }
    }
    sb.appendLine("----------------------------------------------------------------------")
    sb.appendLine("Catatan: Rasio > 50% berwarna merah / terindikasi kritis.")
    return sb.toString()
}

/**
 * Ekspor data Rekap Temuan ke CSV (Mendukung Multi-Pool ke Kanan)
 */
private fun exportRekapTemuanCsv(
    context: Context,
    selectedPeriode: String,
    selectedStatusArea: String = "SEMUA AREA",
    selectedPool: String,
    displayedPoolList: List<String>,
    poolDataMap: Map<String, PoolAuditData>,
    poolToAreaMap: Map<String, String> = emptyMap(),
    sampleCountOp: Int,
    sampleCountSvc: Int,
    totalSampleAll: Int,
    sections: List<KpiSection>
) {
    try {
        val areaTag = if (selectedStatusArea == "SEMUA AREA") "ALL_AREA" else selectedStatusArea.replace(" ", "_")
        val fileName = "REKAP_TEMUAN_${areaTag}_${selectedPool}_${selectedPeriode}_${System.currentTimeMillis()}.csv"
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, fileName)

        FileWriter(file).use { writer ->
            writer.appendLine("REKAP TEMUAN AUDIT KEINDAHAN")
            writer.appendLine("Periode:,$selectedPeriode")
            writer.appendLine("Status Area:,$selectedStatusArea")
            writer.appendLine("Pool Filter:,$selectedPool")
            writer.appendLine("Total Sample Pembagi (Op + Svc):,$totalSampleAll Mobil ($sampleCountOp Op + $sampleCountSvc Svc)")
            writer.appendLine("Formula:,\"Operasional = Unit Temuan / Sample Op, Service = Unit Temuan / Sample Svc, Total = Total Unit Temuan / Total Sample. Rasio > 50% tergolong temuan kritis\"")
            writer.appendLine("Tanggal Ekspor:,${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            writer.appendLine()

            // Header Tabel Tingkat 1 (POOL)
            val header1 = StringBuilder("SEKSI,GROUP,DATA INPUT")
            displayedPoolList.forEach { poolCode ->
                val pData = poolDataMap[poolCode]
                val pTot = pData?.totalSample ?: 0
                val pOp = pData?.sampleOp ?: 0
                val pSvc = pData?.sampleSvc ?: 0
                val pArea = if (poolCode == "ALL") {
                    if (selectedStatusArea != "SEMUA AREA") selectedStatusArea else "SEMUA AREA"
                } else {
                    poolToAreaMap[poolCode] ?: "JABODETABEK"
                }
                val pLabel = if (poolCode == "ALL") "SEMUA POOL ($pArea)" else "POOL $poolCode ($pArea)"
                header1.append(",\"$pLabel (Total: $pTot Smp | $pOp Op + $pSvc Svc)\",\"\",\"\",\"\",\"\",\"\"")
            }
            writer.appendLine(header1.toString())

            // Header Tabel Tingkat 2 (OPERASIONAL, SERVICE & TOTAL)
            val header2 = StringBuilder(",,")
            displayedPoolList.forEach { _ ->
                header2.append(",OPERASIONAL (UNIT),OPERASIONAL (%),SERVICE (UNIT),SERVICE (%),TOTAL (UNIT),TOTAL (%)")
            }
            writer.appendLine(header2.toString())

            sections.forEach { sec ->
                sec.groups.forEach { grp ->
                    grp.items.forEach { item ->
                        val escSec = "\"${sec.title.replace("\"", "\"\"")}\""
                        val escGrp = "\"${grp.title.replace("\"", "\"\"")}\""
                        val escItem = "\"${item.name.replace("\"", "\"\"")}\""

                        val row = StringBuilder("$escSec,$escGrp,$escItem")

                        displayedPoolList.forEach { poolCode ->
                            val pData = poolDataMap[poolCode]
                            val pTotSample = pData?.totalSample ?: 0
                            val pSampleOp = pData?.sampleOp ?: 0
                            val pSampleSvc = pData?.sampleSvc ?: 0

                            val unitOp = pData?.parsedOp?.count { (it.second[item.id] ?: 0) > 0 } ?: 0
                            val pctOp = if (pSampleOp > 0) (unitOp.toDouble() / pSampleOp.toDouble()) * 100.0 else 0.0

                            val unitSvc = pData?.parsedSvc?.count { (it.second[item.id] ?: 0) > 0 } ?: 0
                            val pctSvc = if (pSampleSvc > 0) (unitSvc.toDouble() / pSampleSvc.toDouble()) * 100.0 else 0.0

                            val unitTot = unitOp + unitSvc
                            val pctTot = if (pTotSample > 0) (unitTot.toDouble() / pTotSample.toDouble()) * 100.0 else 0.0

                            val pctOpStr = String.format(Locale.US, "%.1f%%", pctOp)
                            val pctSvcStr = String.format(Locale.US, "%.1f%%", pctSvc)
                            val pctTotStr = String.format(Locale.US, "%.1f%%", pctTot)

                            row.append(",$unitOp,$pctOpStr,$unitSvc,$pctSvcStr,$unitTot,$pctTotStr")
                        }

                        writer.appendLine(row.toString())
                    }
                }
            }
        }

        // Buka chooser untuk share / save
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Ekspor Rekap Temuan Audit ($selectedPool - $selectedPeriode)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Simpan / Kirim Rekap Temuan CSV"))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal mengekspor CSV: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
