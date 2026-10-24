package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MasterKendaraanEntity
import com.example.data.local.entity.MasterPoolEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.theme.Blue50
import com.example.ui.theme.Blue700
import com.example.ui.theme.Blue900
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Teal700

@Composable
fun MasterKendaraanScreen(
    kendaraanList: List<MasterKendaraanEntity>,
    pools: List<MasterPoolEntity>,
    currentUser: UserEntity? = null,
    currentUserPool: String = "",
    isSyncing: Boolean = false,
    syncProgress: String = "",
    onSyncKendaraan: (poolFilter: String) -> Unit = {},
    onSaveKendaraan: (MasterKendaraanEntity) -> Unit,
    onDeleteKendaraan: (Int) -> Unit,
    onClearDemoKendaraan: () -> Unit = {}
) {
    val userRole = currentUser?.levelAkses?.trim()?.uppercase() ?: ""
    val canManageKendaraan = userRole == "SUPER ADMIN" || userRole == "ADMIN"

    // MASTER kendaraan secara otomatis download semua data saat screen dibuka
    LaunchedEffect(Unit) {
        if (!isSyncing) {
            onSyncKendaraan("")
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedPoolFilter by remember(currentUserPool) {
        mutableStateOf(currentUserPool.ifBlank { "SEMUA" })
    }
    var selectedTipeFilter by remember { mutableStateOf("SEMUA") }
    var showDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var editingKendaraan by remember { mutableStateOf<MasterKendaraanEntity?>(null) }

    // Dialog state
    var noMobil by remember { mutableStateOf("") }
    var platNomer by remember { mutableStateOf("") }
    var tipe by remember { mutableStateOf("") }
    var warna by remember { mutableStateOf("") }
    var tahun by remember { mutableStateOf("") }
    var pool by remember { mutableStateOf("SKT") }
    var poolExpanded by remember { mutableStateOf(false) }

    val availablePools = remember(pools) {
        val list = pools.map { it.kodePool.trim() }.filter { it.isNotBlank() }.distinct()
        if (list.isEmpty()) listOf("SKT", "POOL-01", "POOL-02", "POOL-03") else list
    }

    val totalCountInPool = remember(kendaraanList, selectedPoolFilter, selectedTipeFilter) {
        kendaraanList.count { item ->
            val matchesPool = selectedPoolFilter == "SEMUA" || item.pool.trim().equals(selectedPoolFilter.trim(), ignoreCase = true)
            val matchesTipe = selectedTipeFilter == "SEMUA" || item.tipe.trim().equals(selectedTipeFilter.trim(), ignoreCase = true)
            matchesPool && matchesTipe
        }
    }

    // Optimasi performa untuk 80.000 dataset: menggunakan asSequence() dan batasi render tampilan ke 200 data
    val filteredList = remember(kendaraanList, searchQuery, selectedPoolFilter, selectedTipeFilter) {
        val q = searchQuery.trim().lowercase()
        val filterPool = selectedPoolFilter != "SEMUA"
        val targetPool = selectedPoolFilter.trim()
        val filterTipe = selectedTipeFilter != "SEMUA"
        val targetTipe = selectedTipeFilter.trim()

        kendaraanList.asSequence().filter { item ->
            if (filterPool && !item.pool.trim().equals(targetPool, ignoreCase = true)) {
                return@filter false
            }
            if (filterTipe && !item.tipe.trim().equals(targetTipe, ignoreCase = true)) {
                return@filter false
            }
            if (q.isNotEmpty()) {
                val matches = item.noMobil.lowercase().contains(q) ||
                        item.platNomer.lowercase().contains(q) ||
                        item.tipe.lowercase().contains(q) ||
                        item.warna.lowercase().contains(q) ||
                        item.tahun.lowercase().contains(q) ||
                        item.pool.lowercase().contains(q)
                if (!matches) return@filter false
            }
            true
        }.take(200).toList()
    }

    val targetSearchWidth = when {
        searchQuery.isEmpty() -> 135.dp
        else -> (135.dp + (searchQuery.length * 10).dp).coerceIn(135.dp, 350.dp)
    }
    val animatedSearchWidth by animateDpAsState(
        targetValue = targetSearchWidth,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "search_width"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with Tombol Input & Sync di Atas
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
                    // Tombol Input & Tombol Sync Cepat
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (canManageKendaraan) {
                            Button(
                                onClick = {
                                    editingKendaraan = null
                                    noMobil = ""
                                    platNomer = ""
                                    tipe = ""
                                    warna = ""
                                    tahun = ""
                                    pool = pools.firstOrNull()?.kodePool ?: "SKT"
                                    showDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Blue700),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("TAMBAH", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        } else {
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = Slate600,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Mode Lihat",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = Slate600
                                        )
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (isSyncing) return@Button
                                showSyncDialog = true
                            },
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Teal700,
                                disabledContainerColor = Teal700.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync", modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isSyncing) "SINKRON..." else "SYNC SHEETS",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Surface(
                        color = Blue50,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Total: ${kendaraanList.size} Unit",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Blue700),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fitur Filter Dropdown (Pool & Tipe)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Dropdown Filter Pool
                    var poolFilterDropdownExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.wrapContentWidth()) {
                        Surface(
                            color = if (selectedPoolFilter != "SEMUA" && selectedPoolFilter.isNotBlank()) Blue50 else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (selectedPoolFilter != "SEMUA" && selectedPoolFilter.isNotBlank()) Blue700 else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .wrapContentWidth()
                                .clickable { poolFilterDropdownExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Blue700, modifier = Modifier.size(15.dp))
                                Text(
                                    text = if (selectedPoolFilter == "SEMUA" || selectedPoolFilter.isBlank()) "Semua Pool" else "Pool $selectedPoolFilter",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedPoolFilter != "SEMUA" && selectedPoolFilter.isNotBlank()) Blue700 else Slate800,
                                        fontSize = 11.5.sp
                                    )
                                )
                                Icon(
                                    imageVector = if (poolFilterDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Slate600,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = poolFilterDropdownExpanded,
                            onDismissRequest = { poolFilterDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "SEMUA POOL",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (selectedPoolFilter == "SEMUA") FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedPoolFilter == "SEMUA") Blue700 else Slate800
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = "(${kendaraanList.size})",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                                        )
                                    }
                                },
                                onClick = {
                                    selectedPoolFilter = "SEMUA"
                                    poolFilterDropdownExpanded = false
                                }
                            )
                            availablePools.forEach { p ->
                                val countInPool = kendaraanList.count { it.pool.trim().equals(p.trim(), ignoreCase = true) }
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Pool $p",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = if (selectedPoolFilter.equals(p, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (selectedPoolFilter.equals(p, ignoreCase = true)) Blue700 else Slate800
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(
                                                text = "($countInPool)",
                                                style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedPoolFilter = p
                                        poolFilterDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. Dropdown Filter Tipe Kendaraan
                    val distinctTipe = remember(kendaraanList) {
                        kendaraanList.map { it.tipe.trim() }.filter { it.isNotBlank() }.distinct().sorted()
                    }
                    if (distinctTipe.size > 1) {
                        var tipeFilterDropdownExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.wrapContentWidth()) {
                            Surface(
                                color = if (selectedTipeFilter != "SEMUA") Blue50 else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (selectedTipeFilter != "SEMUA") Blue700 else Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .clickable { tipeFilterDropdownExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Blue700, modifier = Modifier.size(15.dp))
                                    Text(
                                        text = if (selectedTipeFilter == "SEMUA") "Semua Tipe" else selectedTipeFilter,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedTipeFilter != "SEMUA") Blue700 else Slate800,
                                            fontSize = 11.5.sp
                                        )
                                    )
                                    Icon(
                                        imageVector = if (tipeFilterDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Slate600,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = tipeFilterDropdownExpanded,
                                onDismissRequest = { tipeFilterDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "SEMUA TIPE",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (selectedTipeFilter == "SEMUA") FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedTipeFilter == "SEMUA") Blue700 else Slate800
                                            )
                                        )
                                    },
                                    onClick = {
                                        selectedTipeFilter = "SEMUA"
                                        tipeFilterDropdownExpanded = false
                                    }
                                )
                                distinctTipe.forEach { t ->
                                    val countTipe = kendaraanList.count {
                                        it.tipe.trim().equals(t, ignoreCase = true) &&
                                        (selectedPoolFilter == "SEMUA" || it.pool.trim().equals(selectedPoolFilter, ignoreCase = true))
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = t,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = if (selectedTipeFilter.equals(t, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (selectedTipeFilter.equals(t, ignoreCase = true)) Blue700 else Slate800
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text(
                                                    text = "($countTipe)",
                                                    style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedTipeFilter = t
                                            tipeFilterDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Tombol Reset Filter jika sedang aktif
                    if (selectedPoolFilter != "SEMUA" || selectedTipeFilter != "SEMUA") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF1F2),
                            border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                            modifier = Modifier.clickable {
                                selectedPoolFilter = "SEMUA"
                                selectedTipeFilter = "SEMUA"
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Reset Filter", tint = Rose600, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "Reset",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Rose600,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fitur Pencarian Lebih Kecil & Otomatis Melebar Mengikuti Tulisan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, if (searchQuery.isNotBlank()) Blue700 else Color(0xFFCBD5E1)),
                        modifier = Modifier.width(animatedSearchWidth)
                    ) {
                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cari",
                                tint = if (searchQuery.isNotBlank()) Blue700 else Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 12.sp,
                                    color = Slate800,
                                    fontWeight = FontWeight.Medium
                                ),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Cari armada...",
                                                fontSize = 11.5.sp,
                                                color = Slate400
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus",
                                        tint = Slate400,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (searchQuery.isNotEmpty() || selectedPoolFilter != "SEMUA" || selectedTipeFilter != "SEMUA" || totalCountInPool > 200) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (totalCountInPool > 200) "Tampil: ${filteredList.size} dari $totalCountInPool unit" else "Tampil: ${filteredList.size} unit",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Blue700,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Banner Notifikasi Proses Sinkronisasi Berjalan
        if (isSyncing) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Blue50),
                border = BorderStroke(1.dp, Blue700.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Teal700,
                            strokeWidth = 2.5.dp
                        )
                        Column {
                            Text(
                                "Sinkronisasi Google Sheets Berjalan...",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Blue900
                                )
                            )
                            Text(
                                text = if (syncProgress.isNotBlank()) syncProgress else "Mengunduh dan memperbarui database armada...",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Slate700,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // Kendaraan Table / Cards
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Tidak ada data kendaraan.", style = MaterialTheme.typography.bodyMedium.copy(color = Slate600))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { "v_${it.id}_${it.noMobil}_${it.platNomer}" }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Blue50,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "No: ${item.noMobil}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Blue700
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.platNomer,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Blue900)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = if (item.tahun.isNotBlank()) "${item.tipe} • ${item.warna} • Th. ${item.tahun}" else "${item.tipe} • ${item.warna}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                                )

                                Text(
                                    text = "Lokasi Pool: ${item.pool}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate800)
                                )
                            }

                            if (canManageKendaraan) {
                                Row {
                                    IconButton(
                                        onClick = {
                                            editingKendaraan = item
                                            noMobil = item.noMobil
                                            platNomer = item.platNomer
                                            tipe = item.tipe
                                            warna = item.warna
                                            tahun = item.tahun
                                            pool = item.pool
                                            showDialog = true
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Blue700)
                                    }

                                    IconButton(
                                        onClick = { onDeleteKendaraan(item.id) }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Rose600)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Add/Edit Kendaraan
    if (showDialog && canManageKendaraan) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = if (editingKendaraan == null) "Tambah Master Kendaraan" else "Edit Master Kendaraan",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = noMobil,
                        onValueChange = { noMobil = it },
                        label = { Text("No Mobil") },
                        placeholder = { Text("Contoh: 101") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = platNomer,
                        onValueChange = { platNomer = it },
                        label = { Text("Plat Nomer") },
                        placeholder = { Text("Contoh: B 1024 SKT") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tipe,
                        onValueChange = { tipe = it },
                        label = { Text("Tipe Kendaraan") },
                        placeholder = { Text("Contoh: Toyota Avanza 1.5 G") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = warna,
                        onValueChange = { warna = it },
                        label = { Text("Warna Kendaraan") },
                        placeholder = { Text("Contoh: Hitam Metalik") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tahun,
                        onValueChange = { tahun = it },
                        label = { Text("Tahun Kendaraan") },
                        placeholder = { Text("Contoh: 2023") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dropdown Kode Pool (Ambil dari Master Pool)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = pool,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kode Pool (Master Pool)") },
                            trailingIcon = {
                                IconButton(onClick = { poolExpanded = !poolExpanded }) {
                                    Icon(
                                        imageVector = if (poolExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Pilih Pool",
                                        tint = Blue700
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { poolExpanded = true }
                        )
                        DropdownMenu(
                            expanded = poolExpanded,
                            onDismissRequest = { poolExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.75f)
                        ) {
                            availablePools.forEach { p ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = p,
                                            fontWeight = if (pool == p) FontWeight.Bold else FontWeight.Normal,
                                            color = if (pool == p) Blue700 else Slate800
                                        )
                                    },
                                    onClick = {
                                        pool = p
                                        poolExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noMobil.isNotBlank()) {
                            onSaveKendaraan(
                                MasterKendaraanEntity(
                                    id = editingKendaraan?.id ?: 0,
                                    noMobil = noMobil.trim(),
                                    platNomer = platNomer.trim(),
                                    tipe = tipe.trim(),
                                    warna = warna.trim(),
                                    tahun = tahun.trim(),
                                    pool = pool.trim()
                                )
                            )
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue700)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showSyncDialog) {
        val currentTargetPool = if (selectedPoolFilter != "SEMUA") selectedPoolFilter else (availablePools.firstOrNull() ?: "SKT")
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Teal700)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sinkronisasi Google Sheets", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Google Sheets memiliki data armada hingga 80.000 data. Pilih metode sinkronisasi:",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate700)
                    )

                    // Pilihan 1: Sync Pool Terpilih (Sangat Cepat < 1 Detik)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Blue50),
                        border = BorderStroke(1.dp, Blue700.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSyncDialog = false
                                onSyncKendaraan(currentTargetPool)
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Blue700,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("⚡", fontSize = 12.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Sinkron Pool $currentTargetPool Saja",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Blue900)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Super Cepat (< 1 Detik). Hanya mengunduh khusus armada Pool $currentTargetPool saja sehingga tidak perlu mengunduh 80.000 data.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Slate600)
                            )
                        }
                    }

                    // Pilihan 2: Sync Semua Pool (80.000 Unit secara bertahap / chunk)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate100),
                        border = BorderStroke(1.dp, Slate300),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSyncDialog = false
                                onSyncKendaraan("SEMUA")
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Slate700,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("🌐", fontSize = 12.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Sinkron Semua Pool (80.000 Unit)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate800)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Mengunduh seluruh database sheet 80.000 baris secara bertahap (batch 20.000 baris) dengan kompresi matriks hemat kuota.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Slate600)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSyncDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
