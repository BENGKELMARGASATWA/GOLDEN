package com.example.ui.screens

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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import com.example.ui.components.DownloadInspectionReportDialog
import com.example.ui.components.PrintInspectionReportDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DataAuditEntity
import com.example.data.local.entity.MasterPoolEntity
import com.example.data.local.entity.PengaturanNamaPhotoEntity
import com.example.ui.components.FotoKendaraanDialog
import com.example.ui.components.PhotoDetailDialog
import com.example.ui.theme.Amber50
import com.example.ui.theme.Amber600
import com.example.ui.theme.Blue100
import com.example.ui.theme.Blue50
import com.example.ui.theme.Blue700
import com.example.ui.theme.Blue900
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Teal50
import com.example.ui.theme.Teal600

import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.Shield
import com.example.data.local.entity.UserEntity
import com.example.util.DateUtils
import com.example.util.AuditFilterUtils

@Composable
fun HasilAuditScreen(
    audits: List<DataAuditEntity>,
    pools: List<MasterPoolEntity>,
    photoTitles: List<PengaturanNamaPhotoEntity>,
    selectedPoolFilter: String,
    currentUser: UserEntity? = null,
    onPoolFilterChanged: (String) -> Unit,
    onDeleteAudit: (Long) -> Unit,
    onEditAudit: (DataAuditEntity) -> Unit = {},
    onDeletePhoto: ((Long, String, String) -> Unit)? = null,
    onSyncAll: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedAuditForModal by remember { mutableStateOf<DataAuditEntity?>(null) }
    var selectedAuditForFotoKendaraan by remember { mutableStateOf<DataAuditEntity?>(null) }
    var selectedAuditForPdfDownload by remember { mutableStateOf<DataAuditEntity?>(null) }

    var poolDropdownExpanded by remember { mutableStateOf(false) }
    var periodeDropdownExpanded by remember { mutableStateOf(false) }
    var selectedPeriode by remember { mutableStateOf("ALL") }
    var sampleDropdownExpanded by remember { mutableStateOf(false) }
    var selectedSampleFilter by remember { mutableStateOf("ALL") }

    val userLevel = currentUser?.levelAkses?.trim()?.uppercase() ?: ""
    val userPool = currentUser?.kodePool?.trim().orEmpty()
    val isPoolRestricted = userPool.isNotBlank() && !userPool.equals("SKT", ignoreCase = true) && !userPool.equals("ALL", ignoreCase = true) &&
        (userLevel == "KEPALA BENGKEL" || userLevel.contains("KABAG") || userLevel == "AUDITOR" || (userLevel != "SUPER ADMIN" && userLevel != "ADMIN" && userLevel != "SKT" && userLevel != "MASTER DATA"))
    val userPoolList = remember(userPool) {
        userPool.split(",", ";").map { it.trim().uppercase() }.filter { it.isNotBlank() }
    }

    val effectivePoolFilter = if (isPoolRestricted) {
        if (userPoolList.size > 1) selectedPoolFilter else userPool
    } else {
        selectedPoolFilter
    }

    // Sesuai aturan: "Data hasil INPUT AUDIT tidak akan di tampilkan jika di temukan ID yang sama"
    val uniqueAudits = remember(audits) {
        AuditFilterUtils.filterUniqueAudits(audits)
    }

    val filteredAudits = remember(uniqueAudits, effectivePoolFilter, selectedPeriode, selectedSampleFilter, searchQuery, isPoolRestricted, userPool, userPoolList) {
        uniqueAudits.filter { audit ->
            val matchPool = if (isPoolRestricted) {
                if (userPoolList.size > 1) {
                    if (effectivePoolFilter == "ALL" || effectivePoolFilter.isBlank()) {
                        userPoolList.any { it.equals(audit.pool.trim(), ignoreCase = true) }
                    } else {
                        audit.pool.equals(effectivePoolFilter, ignoreCase = true)
                    }
                } else {
                    audit.pool.equals(userPool, ignoreCase = true)
                }
            } else if (effectivePoolFilter == "ALL" || effectivePoolFilter.isBlank()) {
                true
            } else {
                audit.pool.equals(effectivePoolFilter, ignoreCase = true)
            }
            val matchPeriode = if (selectedPeriode == "ALL" || selectedPeriode.isBlank()) {
                true
            } else {
                audit.periode.equals(selectedPeriode, ignoreCase = true)
            }
            val matchSample = if (selectedSampleFilter == "ALL" || selectedSampleFilter.isBlank()) {
                true
            } else {
                audit.jenisSample.equals(selectedSampleFilter, ignoreCase = true)
            }
            val matchSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                audit.noMobil.lowercase().contains(q) ||
                        audit.platNomer.lowercase().contains(q) ||
                        audit.namaPetugas.lowercase().contains(q) ||
                        audit.nip.lowercase().contains(q) ||
                        audit.pool.lowercase().contains(q)
            }
            matchPool && matchPeriode && matchSample && matchSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Filter & Search Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "HASIL AUDIT ALL POOL",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Blue900
                            )
                        )
                        Text(
                            text = "Menampilkan ${filteredAudits.size} Laporan",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                        )
                    }

                    Button(
                        onClick = onSyncAll,
                        colors = ButtonDefaults.buttonColors(containerColor = Blue700),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync All", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari No Mobil, Plat, NIP Auditor...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Blue700)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus", tint = Slate400, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue700,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Menu Filter Dropdown dengan Lebar Kolom Otomatis Mengikuti Tulisan
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Dropdown Filter Pool
                    Box(modifier = Modifier.wrapContentWidth()) {
                        Surface(
                            color = if (effectivePoolFilter != "ALL" && effectivePoolFilter.isNotBlank()) Blue50 else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (effectivePoolFilter != "ALL" && effectivePoolFilter.isNotBlank()) Blue700 else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .wrapContentWidth()
                                .clickable { poolDropdownExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Blue700, modifier = Modifier.size(15.dp))
                                Text(
                                    text = if (effectivePoolFilter == "ALL" || effectivePoolFilter.isBlank()) "Semua Pool" else "Pool $effectivePoolFilter",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (effectivePoolFilter != "ALL" && effectivePoolFilter.isNotBlank()) Blue700 else Slate800,
                                        fontSize = 11.5.sp
                                    )
                                )
                                Icon(
                                    imageVector = if (poolDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Slate600,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = poolDropdownExpanded,
                            onDismissRequest = { poolDropdownExpanded = false }
                        ) {
                            if (!isPoolRestricted) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "SEMUA POOL (${audits.size})",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (effectivePoolFilter == "ALL" || effectivePoolFilter.isBlank()) FontWeight.Bold else FontWeight.Normal,
                                                color = if (effectivePoolFilter == "ALL" || effectivePoolFilter.isBlank()) Blue700 else Slate800
                                            )
                                        )
                                    },
                                    onClick = {
                                        onPoolFilterChanged("ALL")
                                        poolDropdownExpanded = false
                                    }
                                )
                            }
                            val displayPools = if (isPoolRestricted) {
                                if (userPoolList.size > 1) {
                                    pools.filter { p -> userPoolList.any { it.equals(p.kodePool, ignoreCase = true) } }.ifEmpty {
                                        userPoolList.map { MasterPoolEntity(kodePool = it, namaPool = "Pool $it") }
                                    }
                                } else {
                                    pools.filter { it.kodePool.equals(userPool, ignoreCase = true) }.ifEmpty {
                                        listOf(MasterPoolEntity(kodePool = userPool, namaPool = "Pool $userPool"))
                                    }
                                }
                            } else {
                                pools
                            }
                            displayPools.forEach { pool ->
                                val countInPool = audits.count { it.pool.equals(pool.kodePool, ignoreCase = true) }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${pool.kodePool} ($countInPool)",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (effectivePoolFilter.equals(pool.kodePool, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal,
                                                color = if (effectivePoolFilter.equals(pool.kodePool, ignoreCase = true)) Blue700 else Slate800
                                            )
                                        )
                                    },
                                    onClick = {
                                        if (!isPoolRestricted) onPoolFilterChanged(pool.kodePool)
                                        poolDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. Dropdown Filter Periode
                    val distinctPeriodes = remember(audits) {
                        audits.map { it.periode.trim() }.filter { it.isNotBlank() }.distinct()
                    }
                    if (distinctPeriodes.isNotEmpty()) {
                        Box(modifier = Modifier.wrapContentWidth()) {
                            Surface(
                                color = if (selectedPeriode != "ALL") Blue50 else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (selectedPeriode != "ALL") Blue700 else Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .clickable { periodeDropdownExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Blue700, modifier = Modifier.size(15.dp))
                                    Text(
                                        text = if (selectedPeriode == "ALL") "Semua Periode" else selectedPeriode,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedPeriode != "ALL") Blue700 else Slate800,
                                            fontSize = 11.5.sp
                                        )
                                    )
                                    Icon(
                                        imageVector = if (periodeDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Slate600,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = periodeDropdownExpanded,
                                onDismissRequest = { periodeDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "SEMUA PERIODE",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (selectedPeriode == "ALL") FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedPeriode == "ALL") Blue700 else Slate800
                                            )
                                        )
                                    },
                                    onClick = {
                                        selectedPeriode = "ALL"
                                        periodeDropdownExpanded = false
                                    }
                                )
                                distinctPeriodes.forEach { per ->
                                    val count = audits.count { it.periode.equals(per, ignoreCase = true) }
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "$per ($count)",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = if (selectedPeriode.equals(per, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (selectedPeriode.equals(per, ignoreCase = true)) Blue700 else Slate800
                                                )
                                            )
                                        },
                                        onClick = {
                                            selectedPeriode = per
                                            periodeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Dropdown Filter Sample (OPERASIONAL / SERVICE)
                    Box(modifier = Modifier.wrapContentWidth()) {
                        Surface(
                            color = if (selectedSampleFilter != "ALL") Blue50 else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (selectedSampleFilter != "ALL") Blue700 else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .wrapContentWidth()
                                .clickable { sampleDropdownExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.FilterList, contentDescription = null, tint = Blue700, modifier = Modifier.size(15.dp))
                                Text(
                                    text = if (selectedSampleFilter == "ALL") "Semua Sample" else selectedSampleFilter,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedSampleFilter != "ALL") Blue700 else Slate800,
                                        fontSize = 11.5.sp
                                    )
                                )
                                Icon(
                                    imageVector = if (sampleDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Slate600,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = sampleDropdownExpanded,
                            onDismissRequest = { sampleDropdownExpanded = false }
                        ) {
                            listOf("ALL" to "SEMUA SAMPLE", "OPERASIONAL" to "OPERASIONAL", "SERVICE" to "SERVICE").forEach { (code, label) ->
                                val count = if (code == "ALL") audits.size else audits.count { it.jenisSample.equals(code, ignoreCase = true) }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "$label ($count)",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (selectedSampleFilter == code) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedSampleFilter == code) Blue700 else Slate800
                                            )
                                        )
                                    },
                                    onClick = {
                                        selectedSampleFilter = code
                                        sampleDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 4. Tombol Reset jika ada filter aktif
                    if ((effectivePoolFilter != "ALL" && !isPoolRestricted) || selectedPeriode != "ALL" || selectedSampleFilter != "ALL") {
                        Surface(
                            color = Rose600.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Rose600.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .wrapContentWidth()
                                .clickable {
                                    if (!isPoolRestricted) onPoolFilterChanged("ALL")
                                    selectedPeriode = "ALL"
                                    selectedSampleFilter = "ALL"
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Reset", tint = Rose600, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Reset",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Rose600, fontSize = 11.sp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Results List
        if (filteredAudits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Slate400, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tidak ada laporan audit yang ditemukan.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Slate600)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredAudits, key = { it.id }) { audit ->
                    val photoCount = listOf(
                        audit.photo1, audit.photo2, audit.photo3, audit.photo4, audit.photo5,
                        audit.photo6, audit.photo7, audit.photo8, audit.photo9, audit.photo10,
                        audit.photo11, audit.photo12, audit.photo13, audit.photo14, audit.photo15,
                        audit.photo16, audit.photo17, audit.photo18, audit.photo19, audit.photo20
                    ).count { it.isNotBlank() }

                    val notesList = listOf(
                        audit.catatanPhoto1, audit.catatanPhoto2, audit.catatanPhoto3, audit.catatanPhoto4, audit.catatanPhoto5,
                        audit.catatanPhoto6, audit.catatanPhoto7, audit.catatanPhoto8, audit.catatanPhoto9, audit.catatanPhoto10,
                        audit.catatanPhoto11, audit.catatanPhoto12, audit.catatanPhoto13, audit.catatanPhoto14, audit.catatanPhoto15,
                        audit.catatanPhoto16, audit.catatanPhoto17, audit.catatanPhoto18, audit.catatanPhoto19, audit.catatanPhoto20
                    ).filter { it.isNotBlank() }

                    val primaryCatatan = if (notesList.isNotEmpty()) {
                        notesList.joinToString("\n")
                    } else {
                        "Kondisi Fisik Kendaraan Terawat & Siap Operasional"
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Row: Header & Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = Blue50,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = audit.noMobil,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                color = Blue900,
                                                fontSize = 15.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = audit.platNomer,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Slate600,
                                            fontSize = 13.sp
                                        )
                                    )
                                    val displayKode = when {
                                        audit.kodeUnik.isNotBlank() -> audit.kodeUnik
                                        audit.auditUuid.isNotBlank() && audit.auditUuid.startsWith("AUD-") -> audit.auditUuid
                                        audit.cloudId > 0L -> "#${audit.cloudId}"
                                        else -> "#${audit.id}"
                                    }
                                    Surface(
                                        color = Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                    ) {
                                        Text(
                                            text = "ID: $displayKode",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Slate800,
                                                fontSize = 10.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Surface(
                                    color = if (audit.isSynced) Teal50 else Amber50,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (audit.isSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                            contentDescription = null,
                                            tint = if (audit.isSynced) Teal600 else Amber600,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (audit.isSynced) "Sinkron" else "Offline",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (audit.isSynced) Teal600 else Amber600,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Hasil Inspeksi 2 Kolom Sesuai Format (Lebar Kolom Otomatis Mengikuti Tulisan)
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Kolom 1 (Lebar otomatis mengikuti tulisan)
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        AuditDataRowAuto(label = "No Mobil", value = audit.noMobil, isBold = true)
                                        AuditDataRowAuto(label = "Plat Nomer", value = audit.platNomer, isSemiBold = true)
                                        AuditDataRowAuto(label = "Tipe", value = audit.tipe.ifBlank { "-" })
                                        AuditDataRowAuto(label = "Warna", value = audit.warna.ifBlank { "-" })
                                        AuditDataRowAuto(label = "Tahun", value = audit.tahun.ifBlank { "-" })
                                        AuditDataRowAuto(label = "Pool Armada", value = audit.pool.ifBlank { "-" }, isSemiBold = true)
                                    }

                                    // Kolom 2 (Lebar otomatis mengikuti tulisan)
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val auditorDisplayText = when {
                                            audit.nip.isNotBlank() && audit.namaPetugas.isNotBlank() -> "${audit.nip}/${audit.namaPetugas}"
                                            audit.nip.isNotBlank() -> audit.nip
                                            audit.namaPetugas.isNotBlank() -> audit.namaPetugas
                                            else -> "-"
                                        }
                                        val waktuTimestampAudit = DateUtils.formatDisplayWaktuTimestamp(audit.timestamp, audit.tanggal)
                                        val isService = audit.jenisSample.equals("SERVICE", ignoreCase = true)
                                        val tgl = if (isService) {
                                            if (audit.sampleTanggal.isNotBlank()) audit.sampleTanggal else audit.tanggalService.ifBlank { "-" }
                                        } else {
                                            "-"
                                        }
                                        val pkt = if (isService) {
                                            if (audit.paketService.isNotBlank()) audit.paketService else audit.service.ifBlank { "-" }
                                        } else {
                                            "-"
                                        }

                                        AuditDataRowAuto(label = "Auditor", value = auditorDisplayText, isSemiBold = true)
                                        AuditDataRowAuto(label = "Tanggal", value = waktuTimestampAudit)
                                        AuditDataRowAuto(label = "Periode", value = audit.periode.ifBlank { "-" })
                                        AuditDataRowAuto(label = "Sample", value = audit.jenisSample.ifBlank { "-" })
                                        AuditDataRowAuto(label = "Tgl Service", value = tgl)
                                        AuditDataRowAuto(label = "Pkt Service", value = pkt)
                                        AuditDataRowAuto(
                                            label = "Temuan",
                                            value = "${audit.totalPoinTemuan} Poin",
                                            valueColor = if (audit.totalPoinTemuan > 0) Rose600 else Teal600,
                                            isBold = true
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Bottom Area: Auditor Info, Edit Button & Delete Button
                            val isSuperAdmin = currentUser?.levelAkses?.equals("SUPER ADMIN", ignoreCase = true) == true
                            val isAdmin = currentUser?.levelAkses?.equals("ADMIN", ignoreCase = true) == true
                            val isSkt = currentUser?.levelAkses?.equals("SKT", ignoreCase = true) == true
                            val isAuditor = currentUser?.levelAkses?.equals("AUDITOR", ignoreCase = true) == true
                            val isCreator = currentUser != null && (
                                (audit.nip.isNotBlank() && audit.nip.trim().equals(currentUser.nip.trim(), ignoreCase = true)) ||
                                audit.namaPetugas.trim().equals(currentUser.namaPetugas.trim(), ignoreCase = true) ||
                                audit.namaPetugas.trim().equals(currentUser.username.trim(), ignoreCase = true)
                            )
                            val auditTime = if (audit.timestamp > 0L) audit.timestamp else (DateUtils.parseToMillis(audit.tanggal, "") ?: audit.timestamp)
                            val elapsedMs = System.currentTimeMillis() - auditTime
                            val maxEditHoursMs = if (isAuditor) 6L * 60L * 60L * 1000L else 12L * 60L * 60L * 1000L
                            val max48HoursMs = 48L * 60L * 60L * 1000L
                            val isWithinEditHours = elapsedMs <= maxEditHoursMs
                            val isWithin48Hours = elapsedMs <= max48HoursMs
                            val canEdit = isSuperAdmin || (isCreator && isWithinEditHours)
                            val canDelete = (isSuperAdmin || ((isAdmin || isSkt) && isWithin48Hours)) && !isAuditor

                            // Baris Informasi Waktu & Sisa Edit
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tanggal: ${DateUtils.formatDisplayWaktuTimestamp(audit.timestamp, audit.tanggal)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Slate600,
                                        fontSize = 11.sp
                                    )
                                )
                                if (canEdit && !isSuperAdmin) {
                                    val rem = maxEditHoursMs - elapsedMs
                                    val h = rem / (1000 * 60 * 60)
                                    val m = (rem % (1000 * 60 * 60)) / (1000 * 60)
                                    Text(
                                        text = "Sisa edit: ${h}j ${m}m",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Amber600,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Baris Tombol Aksi Lengkap & Responsif
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Tombol LIHAT (Detail Foto Temuan)
                                Surface(
                                    onClick = { selectedAuditForModal = audit },
                                    color = Blue50,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Blue700.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = "Lihat Foto",
                                            tint = Blue700,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "LIHAT",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Blue700,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))

                                // 2. Tombol DOWNLOAD PDF
                                Surface(
                                    onClick = { selectedAuditForPdfDownload = audit },
                                    color = Color(0xFF1D4ED8),
                                    shape = RoundedCornerShape(8.dp),
                                    shadowElevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Download PDF",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "DOWNLOAD PDF",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))

                                // 3. Tombol FOTO Kendaraan
                                Surface(
                                    onClick = { selectedAuditForFotoKendaraan = audit },
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsCar,
                                            contentDescription = "Foto Kendaraan",
                                            tint = Slate700,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "FOTO",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Slate700,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))

                                // 4. Tombol EDIT
                                if (canEdit) {
                                    Surface(
                                        onClick = { onEditAudit(audit) },
                                        color = Amber50,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Amber600.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = Amber600,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "EDIT",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Amber600,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                // 5. Tombol DELETE
                                if (canDelete) {
                                    var showConfirmDelete by remember { mutableStateOf(false) }
                                    IconButton(
                                        onClick = { showConfirmDelete = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = Rose600,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    if (showConfirmDelete) {
                                        val remDel = max48HoursMs - elapsedMs
                                        val hDel = (remDel / (1000 * 60 * 60)).coerceAtLeast(0)
                                        val mDel = ((remDel % (1000 * 60 * 60)) / (1000 * 60)).coerceAtLeast(0)
                                        val timeInfo = if (isSuperAdmin) {
                                            "Level Akses: Super Admin (Akses Hapus Tanpa Batas)"
                                        } else {
                                            "Mode Hapus Admin Aktif: Sisa batas 48 jam (${hDel} jam ${mDel} menit)"
                                        }

                                        AlertDialog(
                                            onDismissRequest = { showConfirmDelete = false },
                                            title = { Text("Konfirmasi Hapus Data Audit") },
                                            text = {
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text("Apakah Anda yakin ingin menghapus data audit armada ${audit.noMobil} (${audit.tanggal})?")
                                                    Text(
                                                        text = timeInfo,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = if (isSuperAdmin) Slate600 else Rose600,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    )
                                                }
                                            },
                                            confirmButton = {
                                                TextButton(
                                                    onClick = {
                                                        showConfirmDelete = false
                                                        onDeleteAudit(audit.id)
                                                    }
                                                ) {
                                                    Text("Hapus", color = Rose600, fontWeight = FontWeight.Bold)
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showConfirmDelete = false }) {
                                                    Text("Batal")
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Photo Viewer Dialog
    selectedAuditForModal?.let { audit ->
        PhotoDetailDialog(
            audit = audit,
            photoTitles = photoTitles,
            currentUser = currentUser,
            onEditAudit = onEditAudit,
            onDeleteAudit = onDeleteAudit,
            onDeletePhoto = { itemKey, path ->
                onDeletePhoto?.invoke(audit.id, itemKey, path)
            },
            onDismiss = { selectedAuditForModal = null }
        )
    }

    // Modal Tabel Foto Kendaraan Dialog
    selectedAuditForFotoKendaraan?.let { audit ->
        FotoKendaraanDialog(
            audit = audit,
            photoTitles = photoTitles,
            currentUser = currentUser,
            onDismiss = { selectedAuditForFotoKendaraan = null },
            onEditAudit = onEditAudit
        )
    }

    // Modal Dialog Download Laporan PDF Hasil Inspeksi
    selectedAuditForPdfDownload?.let { audit ->
        DownloadInspectionReportDialog(
            audit = audit,
            photoTitles = photoTitles,
            onDismiss = { selectedAuditForPdfDownload = null }
        )
    }
}

@Composable
private fun AuditDataRowAuto(
    label: String,
    value: String,
    valueColor: Color = Slate800,
    isBold: Boolean = false,
    isSemiBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Slate600,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            modifier = Modifier.wrapContentWidth()
        )
        Text(
            text = ": ",
            color = Slate400,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
        )
        Text(
            text = value,
            color = valueColor,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isBold) FontWeight.Bold else if (isSemiBold) FontWeight.SemiBold else FontWeight.Normal
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
