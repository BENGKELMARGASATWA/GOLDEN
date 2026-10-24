package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DataAuditEntity
import com.example.data.local.entity.MasterKendaraanEntity
import com.example.data.local.entity.MasterPoolEntity
import com.example.ui.theme.Amber50
import com.example.ui.theme.Amber600
import com.example.ui.theme.Blue50
import com.example.ui.theme.Blue700
import com.example.ui.theme.HighDensityBlue
import com.example.ui.theme.HighDensityBlueBorder
import com.example.ui.theme.HighDensityBlueLight
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate800
import com.example.ui.theme.Teal50
import com.example.ui.theme.Teal600

/**
 * Menu MONITORING AUDIT untuk level SKT & ADMIN.
 * Menampilkan data seperti Monitoring All Pool, dengan keunggulan dapat memilih
 * beberapa pool sekaligus (multi-select) melalui dropdown berisi checklist.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonitoringAuditScreen(
    pools: List<MasterPoolEntity>,
    vehicles: List<MasterKendaraanEntity>,
    audits: List<DataAuditEntity>,
    onNavigateToHasilAudit: (String) -> Unit,
    onSyncAll: () -> Unit
) {
    // State checklist pool yang dipilih (default: semua pool tercentang)
    var selectedPoolCodes by remember(pools) {
        mutableStateOf(pools.map { it.kodePool }.toSet())
    }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Hitung data yang terfilter berdasarkan pool yang tercentang
    val selectedPools = remember(pools, selectedPoolCodes) {
        pools.filter { selectedPoolCodes.contains(it.kodePool) }
    }
    val filteredVehicles = remember(vehicles, selectedPoolCodes) {
        vehicles.filter { selectedPoolCodes.contains(it.pool) }
    }
    val filteredAudits = remember(audits, selectedPoolCodes) {
        audits.filter { selectedPoolCodes.contains(it.pool) }
    }

    val totalAudits = filteredAudits.size
    val unsyncedAudits = filteredAudits.count { !it.isSynced }
    val activePoolsCount = selectedPools.size

    val poolStats = remember(selectedPools, filteredVehicles, filteredAudits) {
        selectedPools.map { pool ->
            val poolVehicles = filteredVehicles.filter { it.pool.equals(pool.kodePool, ignoreCase = true) }
            val poolAudits = filteredAudits.filter { it.pool.equals(pool.kodePool, ignoreCase = true) }
            Triple(pool, poolVehicles.size, poolAudits.size)
        }.sortedWith(
            compareByDescending<Triple<MasterPoolEntity, Int, Int>> { it.third } // Urutkan sample terbanyak ke sedikit
                .thenByDescending { it.second } // Jika sama, urutkan armada terbanyak
                .thenBy { it.first.kodePool }   // Urutkan kode pool
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // TOP CARD: Header, Sinkronisasi Cloud, dan Dropdown Checklist Pemilihan Pool
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Row Judul & Tombol Sinkronkan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MONITORING AUDIT",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = HighDensityBlue,
                                    fontSize = 15.sp,
                                    letterSpacing = (-0.2).sp
                                )
                            )
                            Text(
                                text = "Pilih satu atau beberapa pool untuk dipantau",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Slate600,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Button(
                            onClick = onSyncAll,
                            colors = ButtonDefaults.buttonColors(containerColor = Blue700),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sinkronkan Cloud", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Slate200, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // DROPDOWN DENGAN CHECKLIST PILIHAN POOL
                    Text(
                        text = "PILIH POOL YANG DITAMPILKAN (DROPDOWN CHECKLIST)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = Slate800,
                            fontSize = 10.5.sp,
                            letterSpacing = 0.3.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Dropdown Trigger Field
                        Surface(
                            onClick = { dropdownExpanded = !dropdownExpanded },
                            color = if (selectedPoolCodes.isNotEmpty()) Color(0xFFF0F7FF) else Color(0xFFFFFBEB),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (selectedPoolCodes.isNotEmpty()) Blue700.copy(alpha = 0.6f) else Amber600.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterAlt,
                                        contentDescription = null,
                                        tint = if (selectedPoolCodes.isNotEmpty()) Blue700 else Amber600,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        val statusText = when {
                                            selectedPoolCodes.isEmpty() -> "Belum ada pool dipilih (Centang pool di dropdown)"
                                            selectedPoolCodes.size == pools.size -> "Semua Pool Terpilih (${pools.size} Pool)"
                                            else -> "${selectedPoolCodes.size} dari ${pools.size} Pool Terpilih (${selectedPoolCodes.joinToString(", ")})"
                                        }
                                        Text(
                                            text = statusText,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedPoolCodes.isNotEmpty()) HighDensityBlue else Slate800,
                                                fontSize = 12.sp
                                            ),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "Klik di sini untuk membuka checklist daftar pool",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Slate600,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = if (dropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = if (selectedPoolCodes.isNotEmpty()) Blue700 else Slate600,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Isi Dropdown Menu (Berisi checklist untuk masing-masing pool)
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .heightIn(max = 380.dp)
                                .background(Color.White)
                                .border(1.dp, Slate200, RoundedCornerShape(8.dp))
                        ) {
                            // Header Dropdown: Aksi Cepat Pilih Semua / Hapus Pilihan
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            selectedPoolCodes = pools.map { it.kodePool }.toSet()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SelectAll,
                                        contentDescription = null,
                                        tint = Blue700,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Pilih Semua",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Blue700,
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            selectedPoolCodes = emptySet()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ClearAll,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Kosongkan",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626),
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            HorizontalDivider(color = Slate200, thickness = 0.8.dp)

                            // Daftar Pool dengan Checkbox
                            pools.forEach { pool ->
                                val isChecked = selectedPoolCodes.contains(pool.kodePool)
                                val poolAuditCount = audits.count { it.pool.equals(pool.kodePool, ignoreCase = true) }
                                val poolCarCount = vehicles.count { it.pool.equals(pool.kodePool, ignoreCase = true) }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedPoolCodes = if (isChecked) {
                                                selectedPoolCodes - pool.kodePool
                                            } else {
                                                selectedPoolCodes + pool.kodePool
                                            }
                                        }
                                        .background(if (isChecked) Color(0xFFF8FAFC) else Color.White)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedPoolCodes = if (checked) {
                                                selectedPoolCodes + pool.kodePool
                                            } else {
                                                selectedPoolCodes - pool.kodePool
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Blue700,
                                            uncheckedColor = Slate400
                                        ),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${pool.kodePool} - ${pool.namaPool}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isChecked) Slate800 else Slate600,
                                                fontSize = 12.sp
                                            )
                                        )
                                        Text(
                                            text = "Armada: $poolCarCount • Sample Audit: $poolAuditCount",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Slate600,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }

                                    if (isChecked) {
                                        Surface(
                                            color = Blue50,
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(0.6.dp, Blue700.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = "AKTIF",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Blue700,
                                                    fontSize = 9.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Slate200, thickness = 0.8.dp)

                            // Tombol Tutup / Terapkan di bagian bawah dropdown
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Button(
                                    onClick = { dropdownExpanded = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Blue700),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tutup (${selectedPoolCodes.size} Pool)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                                }
                            }
                        }
                    }

                    // Baris Chips Pool Terpilih (Memudahkan melihat dan menghapus pool secara langsung)
                    if (selectedPoolCodes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Aktif:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate600,
                                    fontSize = 10.sp
                                )
                            )
                            selectedPoolCodes.forEach { code ->
                                Surface(
                                    color = Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(0.8.dp, Blue700.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 7.dp, end = 3.dp, top = 2.dp, bottom = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = code,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Blue700,
                                                fontSize = 10.5.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Hapus $code",
                                            tint = Blue700,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    selectedPoolCodes = selectedPoolCodes - code
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Slate200, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // 4 KPI Summary Cards (Berdasarkan Pool Terpilih)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KpiSummaryBox(
                            title = "Pool Terpilih",
                            value = "$activePoolsCount Pool",
                            bgColor = HighDensityBlueLight,
                            borderColor = HighDensityBlueBorder,
                            textColor = HighDensityBlue,
                            modifier = Modifier.weight(1f)
                        )

                        KpiSummaryBox(
                            title = "Total Armada",
                            value = "${filteredVehicles.size} Unit",
                            bgColor = HighDensityBlueLight,
                            borderColor = HighDensityBlueBorder,
                            textColor = HighDensityBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KpiSummaryBox(
                            title = "Hasil Audit",
                            value = "$totalAudits Selesai",
                            bgColor = Teal50,
                            borderColor = Color(0xFF99F6E4),
                            textColor = Teal600,
                            modifier = Modifier.weight(1f)
                        )

                        KpiSummaryBox(
                            title = "Pending Sync",
                            value = "$unsyncedAudits Offline",
                            bgColor = if (unsyncedAudits == 0) Teal50 else Amber50,
                            borderColor = if (unsyncedAudits == 0) Color(0xFF99F6E4) else Color(0xFFFDE68A),
                            textColor = if (unsyncedAudits == 0) Teal600 else Amber600,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Section Title
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Statistik Pool Terpilih ($activePoolsCount Pool)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate800,
                        fontSize = 13.sp
                    )
                )
                Text(
                    text = "Urutan: Sample Terbanyak",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Slate600,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // Jika tidak ada pool yang dicentang
        if (selectedPoolCodes.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tidak Ada Pool Yang Dipilih",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate800,
                                fontSize = 14.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Silakan klik dropdown di atas dan centang pool yang ingin Anda pantau.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Slate600,
                                fontSize = 12.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { selectedPoolCodes = pools.map { it.kodePool }.toSet() },
                            colors = ButtonDefaults.buttonColors(containerColor = Blue700),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Pilih Semua Pool", fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            // Daftar Kartu Statistik Pool Terpilih (Urutan sample terbanyak)
            itemsIndexed(poolStats, key = { _, item -> item.first.id }) { index, (pool, vehicleCount, auditCount) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToHasilAudit(pool.kodePool) }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(if (index < 3 && auditCount > 0) Color(0xFFEFF6FF) else HighDensityBlueLight, CircleShape)
                                        .border(1.dp, if (index < 3 && auditCount > 0) Blue700 else HighDensityBlueBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = if (index < 3 && auditCount > 0) Blue700 else HighDensityBlue,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${pool.kodePool} - ${pool.namaPool}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Slate800,
                                            fontSize = 13.sp
                                        ),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = pool.alamatPool.ifBlank { "Kawasan Operasional" },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Slate600,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Lihat Detail",
                                tint = Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Slate100, thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Armada: $vehicleCount Unit • Sample: $auditCount",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Slate600,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )

                            Surface(
                                color = if (auditCount > 0) Teal50 else Slate100,
                                shape = RoundedCornerShape(4.dp),
                                border = if (auditCount > 0) BorderStroke(0.6.dp, Color(0xFF99F6E4)) else null
                            ) {
                                Text(
                                    text = if (auditCount > 0) "$auditCount Sample" else "0 Sample",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (auditCount > 0) Teal600 else Slate600,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiSummaryBox(
    title: String,
    value: String,
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
                    fontSize = 15.sp
                )
            )
        }
    }
}
