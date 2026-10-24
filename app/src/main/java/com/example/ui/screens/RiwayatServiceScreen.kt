package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import com.example.ui.components.PrintInspectionReportDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DataAuditEntity
import com.example.data.local.entity.MasterPoolEntity
import com.example.data.local.entity.PengaturanNamaPhotoEntity
import com.example.data.local.entity.UserEntity
import com.example.util.DateUtils
import com.example.ui.components.FotoKendaraanDialog
import com.example.ui.components.PhotoDetailDialog
import com.example.ui.theme.Amber50
import com.example.ui.theme.Amber600
import com.example.ui.theme.Blue50
import com.example.ui.theme.Blue700
import com.example.ui.theme.HighDensityBg
import com.example.ui.theme.HighDensityBlue
import com.example.ui.theme.HighDensityBlueBorder
import com.example.ui.theme.HighDensityBlueLight
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Teal50
import com.example.ui.theme.Teal600

import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.Shield
import com.example.util.AuditFilterUtils

@Composable
fun RiwayatServiceScreen(
    audits: List<DataAuditEntity>,
    currentUser: UserEntity?,
    photoTitles: List<PengaturanNamaPhotoEntity>,
    pools: List<MasterPoolEntity> = emptyList(),
    onSelectAudit: (DataAuditEntity) -> Unit,
    onDeleteAudit: (Long) -> Unit,
    onEditAudit: (DataAuditEntity) -> Unit = {},
    onDeletePhoto: ((Long, String, String) -> Unit)? = null,
    onSyncAll: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPoolFilter by remember { mutableStateOf("ALL") }
    var poolFilterExpanded by remember { mutableStateOf(false) }

    val registeredPool = currentUser?.kodePool?.trim()?.uppercase().orEmpty()
    val userRole = currentUser?.levelAkses?.trim()?.uppercase().orEmpty()
    val isRestrictedPool = registeredPool.isNotBlank() &&
        !registeredPool.equals("SKT", ignoreCase = true) &&
        !registeredPool.equals("ALL", ignoreCase = true) &&
        (userRole == "KEPALA BENGKEL" || userRole.contains("KABAG") || userRole == "AUDITOR" ||
            (userRole != "SUPER ADMIN" && userRole != "ADMIN" && userRole != "SKT" && userRole != "MASTER DATA"))

    val userPoolList = remember(registeredPool) {
        registeredPool.split(",", ";").map { it.trim().uppercase() }.filter { it.isNotBlank() }
    }

    val availablePoolOptions = remember(pools, audits, isRestrictedPool, userPoolList) {
        val list = mutableListOf<String>()
        list.add("ALL")
        if (isRestrictedPool && userPoolList.isNotEmpty()) {
            list.addAll(userPoolList)
        } else {
            val fromMaster = pools.map { it.kodePool.trim().uppercase() }.filter { it.isNotBlank() }
            val fromAudits = audits.map { it.pool.trim().uppercase() }.filter { it.isNotBlank() }
            list.addAll((fromMaster + fromAudits).distinct().sorted())
        }
        list.distinct()
    }

    var selectedAuditForModal by remember { mutableStateOf<DataAuditEntity?>(null) }
    var selectedAuditForFotoKendaraan by remember { mutableStateOf<DataAuditEntity?>(null) }
    var selectedAuditForPdfPrint by remember { mutableStateOf<DataAuditEntity?>(null) }

    // Sesuai aturan:
    // 1. Data hasil INPUT AUDIT tidak akan di tampilkan jika di temukan ID yang sama
    // 2. Pada menu RIWAYAT AUDIT jika terdapat no mobil yang sama pada satu periode maka hanya di tampilkan pada input terakhir
    val uniqueAudits = remember(audits) {
        val deduped = AuditFilterUtils.filterUniqueAudits(audits)
        AuditFilterUtils.filterLatestPerCarAndPeriod(deduped)
    }

    val filteredAudits = remember(uniqueAudits, searchQuery, selectedPoolFilter) {
        val poolFiltered = if (selectedPoolFilter == "ALL" || selectedPoolFilter.isBlank()) {
            uniqueAudits
        } else {
            uniqueAudits.filter { it.pool.equals(selectedPoolFilter, ignoreCase = true) }
        }

        if (searchQuery.isBlank()) {
            poolFiltered
        } else {
            val q = searchQuery.trim().lowercase()
            poolFiltered.filter {
                it.noMobil.lowercase().contains(q) ||
                        it.platNomer.lowercase().contains(q) ||
                        it.namaPetugas.lowercase().contains(q) ||
                        it.nip.lowercase().contains(q) ||
                        it.tanggal.lowercase().contains(q) ||
                        it.pool.lowercase().contains(q) ||
                        it.tipe.lowercase().contains(q) ||
                        it.jenisSample.lowercase().contains(q) ||
                        it.paketService.lowercase().contains(q) ||
                        it.sampleTanggal.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HighDensityBg)
            .padding(14.dp)
    ) {
        // Search Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        val isKepalaBengkel = currentUser?.levelAkses.equals("KEPALA BENGKEL", ignoreCase = true)
                        Text(
                            text = if (isKepalaBengkel) "RIWAYAT AUDIT POOL ${currentUser?.kodePool}" else "RIWAYAT AUDIT KENDARAAN",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = HighDensityBlue,
                                fontSize = 16.sp
                            )
                        )
                        if (!isKepalaBengkel) {
                            Text(
                                text = "Total ${filteredAudits.size} Pemeriksaan Tercatat" + if (selectedPoolFilter != "ALL") " (Pool $selectedPoolFilter)" else "",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onSyncAll,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                            .border(1.dp, HighDensityBlueBorder, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync Cloud",
                            tint = HighDensityBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search & Filter Row: Kolom pencarian dan tombol filter POOL berdampingan
                var isSearchFocused by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search Input Box
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(
                                width = 1.dp,
                                color = if (isSearchFocused || searchQuery.isNotEmpty()) HighDensityBlue else Slate300,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = if (isSearchFocused || searchQuery.isNotEmpty()) HighDensityBlue else Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Cari Data",
                                    fontSize = 12.sp,
                                    color = Slate400,
                                    maxLines = 1
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = Slate800,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(HighDensityBlue),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { isSearchFocused = it.isFocused }
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus",
                                    tint = Slate400,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    // Tombol Filter POOL di samping pencarian
                    Box(modifier = Modifier.wrapContentSize()) {
                        val isPoolFiltered = selectedPoolFilter != "ALL" && selectedPoolFilter.isNotBlank()
                        val poolButtonText = if (isPoolFiltered) selectedPoolFilter else "Pool"

                        Surface(
                            onClick = { poolFilterExpanded = true },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isPoolFiltered) HighDensityBlue else if (poolFilterExpanded) HighDensityBlue else Slate300
                            ),
                            color = if (isPoolFiltered) Color(0xFFEFF6FF) else Color.White,
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("btn_filter_pool_riwayat")
                        ) {
                            Row(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .fillMaxHeight()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filter Pool",
                                    tint = if (isPoolFiltered) HighDensityBlue else Slate600,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = poolButtonText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (isPoolFiltered) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isPoolFiltered) HighDensityBlue else Slate700
                                    )
                                )
                                if (isPoolFiltered) {
                                    IconButton(
                                        onClick = { selectedPoolFilter = "ALL" },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Reset Pool",
                                            tint = HighDensityBlue,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (poolFilterExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Slate500,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = poolFilterExpanded,
                            onDismissRequest = { poolFilterExpanded = false }
                        ) {
                            availablePoolOptions.forEach { poolCode ->
                                val count = if (poolCode == "ALL") {
                                    uniqueAudits.size
                                } else {
                                    uniqueAudits.count { it.pool.equals(poolCode, ignoreCase = true) }
                                }
                                val labelText = if (poolCode == "ALL") {
                                    if (isRestrictedPool && userPoolList.size > 1) "Semua Pool Saya" else "Semua Pool"
                                } else {
                                    poolCode
                                }

                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = labelText,
                                                fontSize = 12.sp,
                                                fontWeight = if (poolCode == selectedPoolFilter) FontWeight.Bold else FontWeight.Normal,
                                                color = if (poolCode == selectedPoolFilter) HighDensityBlue else Slate800
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "$count unit",
                                                fontSize = 10.5.sp,
                                                color = Slate400
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedPoolFilter = poolCode
                                        poolFilterExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Audits List
        if (filteredAudits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (searchQuery.isBlank() && selectedPoolFilter == "ALL") "Belum ada riwayat audit tersimpan." else "Tidak ada audit yang cocok dengan filter / pencarian.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Slate600)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredAudits, key = { it.id }) { audit ->
                    SleekAuditCard(
                        audit = audit,
                        currentUser = currentUser,
                        onShowFotoKendaraan = { selectedAuditForFotoKendaraan = it },
                        onShowHasil = { selectedAuditForModal = it },
                        onEditAudit = onEditAudit,
                        onDeleteAudit = onDeleteAudit,
                        onPrintPdf = { selectedAuditForPdfPrint = it }
                    )
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

    // Modal Cetak / Unduh Laporan PDF Dialog
    selectedAuditForPdfPrint?.let { audit ->
        PrintInspectionReportDialog(
            audit = audit,
            photoTitles = photoTitles,
            onDismiss = { selectedAuditForPdfPrint = null }
        )
    }
}

@Composable
private fun SleekAuditCard(
    audit: DataAuditEntity,
    currentUser: UserEntity?,
    onShowFotoKendaraan: (DataAuditEntity) -> Unit,
    onShowHasil: (DataAuditEntity) -> Unit,
    onEditAudit: (DataAuditEntity) -> Unit,
    onDeleteAudit: (Long) -> Unit,
    onPrintPdf: (DataAuditEntity) -> Unit
) {
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

    val sampleVal = audit.jenisSample.ifBlank { "OPERASIONAL" }
    val tglServiceVal = audit.sampleTanggal.ifBlank { audit.tanggalService.ifBlank { "-" } }
    val pktServiceVal = audit.paketService.ifBlank { audit.service.ifBlank { "-" } }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
            // Baris 1: Header Info Kendaraan & Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Info Kendaraan
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(28.dp)
                            .background(HighDensityBlueLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = HighDensityBlue,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = audit.noMobil.ifBlank { "-" },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate800,
                                fontSize = 13.5.sp
                            )
                        )
                        if (audit.tipe.isNotBlank()) {
                            Text(
                                text = audit.tipe,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Slate600,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (audit.pool.isNotBlank()) {
                            Text(
                                text = audit.pool,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Slate500,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Tombol Aksi Kanan
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Status Sinkronisasi
                    Surface(
                        color = if (audit.isSynced) Teal50 else Amber50,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Icon(
                            imageVector = if (audit.isSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = if (audit.isSynced) "Tersinkron" else "Offline",
                            tint = if (audit.isSynced) Teal600 else Amber600,
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 3.dp)
                                .size(13.dp)
                        )
                    }

                    // Tombol FOTO
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(1.dp, HighDensityBlue.copy(alpha = 0.35f)),
                        modifier = Modifier.clickable { onShowFotoKendaraan(audit) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "Foto",
                                tint = HighDensityBlue,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "FOTO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityBlue,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    // Tombol HASIL
                    Surface(
                        color = HighDensityBlueLight,
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(1.dp, HighDensityBlueBorder),
                        modifier = Modifier.clickable { onShowHasil(audit) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Hasil",
                                tint = HighDensityBlue,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "HASIL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityBlue,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(5.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(5.dp))

            // Baris 2: Data Ramping: Sample | tgl Service | pkt Service | Temuan (jumlah point)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Sample
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sample",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            color = Slate500,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = sampleVal,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 2. tgl Service
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "tgl Service",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            color = Slate500,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = tglServiceVal,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate700
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 3. pkt Service
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "pkt Service",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            color = Slate500,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = pktServiceVal,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate700
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 4. Temuan (jumlah point)
                Column(
                    modifier = Modifier.weight(1.1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Temuan",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            color = Slate500,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Surface(
                        color = if (audit.totalPoinTemuan > 0) Color(0xFFFFF1F2) else Teal50,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.6.dp, if (audit.totalPoinTemuan > 0) Color(0xFFFECDD3) else Color(0xFFA7F3D0))
                    ) {
                        Text(
                            text = "${audit.totalPoinTemuan} Poin",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (audit.totalPoinTemuan > 0) Rose600 else Teal600
                            ),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Baris 3: Waktu & Auditor + Tombol Edit (Pojok Kanan Bawah)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = DateUtils.formatDisplayWaktuTimestamp(audit.timestamp, audit.tanggal),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Slate600,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall.copy(color = Slate400, fontSize = 9.sp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Auditor: ${audit.namaPetugas}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Slate500,
                            fontSize = 9.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Action Buttons di Pojok Kanan Bawah (CETAK PDF, EDIT, & HAPUS)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Tombol CETAK PDF (di sebelah tombol EDIT)
                    Surface(
                        onClick = { onPrintPdf(audit) },
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(1.dp, Blue700.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Cetak PDF",
                                tint = Blue700,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "CETAK PDF",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Blue700,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    if (canEdit) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            onClick = { onEditAudit(audit) },
                            color = Amber50,
                            shape = RoundedCornerShape(5.dp),
                            border = BorderStroke(1.dp, Amber600.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = Amber600,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "EDIT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Amber600,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }

                    if (canDelete) {
                        Spacer(modifier = Modifier.width(6.dp))
                        var showConfirmDelete by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showConfirmDelete = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus",
                                tint = Rose600,
                                modifier = Modifier.size(15.dp)
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
