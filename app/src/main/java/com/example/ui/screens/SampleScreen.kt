package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MasterPoolEntity
import com.example.data.local.entity.MasterSampleEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.theme.HighDensityBg
import com.example.ui.theme.HighDensityBlue
import com.example.ui.theme.HighDensityBlueDark
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

/**
 * Super Admin Screen: SAMPLE
 * Displays a table where:
 * - Column 1: Status Area pada master pool
 * - Column 2: Jumlah Sample (digunakan untuk menentukan jumlah sample pada bobot)
 * Accessible exclusively by Super Admin.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleScreen(
    sampleList: List<MasterSampleEntity>,
    poolList: List<MasterPoolEntity>,
    currentUser: UserEntity?,
    onSaveSample: (MasterSampleEntity) -> Unit,
    onDeleteSample: (String) -> Unit,
    onSyncGoogleSheet: (() -> Unit)? = null,
    isSyncing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isSuperAdmin = currentUser?.levelAkses?.trim()?.uppercase() == "SUPER ADMIN"
    val isAdmin = currentUser?.levelAkses?.trim()?.uppercase() == "ADMIN"
    val canAccess = isSuperAdmin || isAdmin

    // Access guard: Super Admin & Admin can access and edit this screen
    if (!canAccess) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(HighDensityBg)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Rose600,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "AKSES TERBATAS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = Slate900
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Menu SAMPLE ini dikhususkan untuk level akses SUPER ADMIN dan ADMIN untuk mengelola konfigurasi jumlah sample berdasarkan Status Area Master Pool.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Slate600,
                            textAlign = TextAlign.Center
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Level Akses Anda: ${currentUser?.levelAkses ?: "TIDAK DIKETAHUI"}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    var editingSample by remember { mutableStateOf<MasterSampleEntity?>(null) }
    var isAddingNewSample by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<String?>(null) }

    // Data pada menu SAMPLE murni mengikuti data yang terdapat di tabel MASTER_SAMPLE Google Sheets (sampleList)
    val allRows = remember(sampleList) {
        sampleList.sortedBy { it.statusArea }
    }

    // Mendeteksi Status Area dari Master Pool yang belum terdaftar di tabel Google Sheets
    val unconfiguredPoolAreas = remember(poolList, sampleList) {
        poolList.map { it.statusArea.trim().uppercase() }
            .filter { it.isNotBlank() && sampleList.none { s -> s.statusArea.trim().equals(it, ignoreCase = true) } }
            .distinct()
    }

    val filteredRows by remember(allRows, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                allRows
            } else {
                val q = searchQuery.trim().lowercase()
                allRows.filter {
                    it.statusArea.lowercase().contains(q) ||
                    it.keterangan.lowercase().contains(q)
                }
            }
        }
    }

    val totalAreaCount = allRows.size
    val totalSampleAccumulated = allRows.sumOf { it.jumlahSample }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(HighDensityBg)
                .padding(innerPadding)
                .testTag("sample_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header Card with Super Admin Badge & Google Sheets Sync
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(HighDensityBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Pin,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "KONFIGURASI SAMPLE AREA",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 17.sp,
                                            color = Slate900
                                        )
                                    )
                                    Text(
                                        text = "Data mengikuti tabel MASTER_SAMPLE di Google Sheets",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Slate500,
                                            fontSize = 11.5.sp
                                        )
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (onSyncGoogleSheet != null) {
                                    Button(
                                        onClick = { onSyncGoogleSheet() },
                                        enabled = !isSyncing,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        if (isSyncing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("TARIK DARI SHEETS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                }

                                Button(
                                    onClick = { isAddingNewSample = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("TAMBAH AREA", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Slate200, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Slate200)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "TOTAL AREA",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Slate500,
                                            fontSize = 9.5.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$totalAreaCount Status Area",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Slate800,
                                            fontSize = 14.sp
                                        )
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "TOTAL SAMPLE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensityBlue,
                                            fontSize = 9.5.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$totalSampleAccumulated Unit Armada",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = HighDensityBlueDark,
                                            fontSize = 14.sp
                                        )
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "SUMBER DATA",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF047857),
                                            fontSize = 9.5.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Google Sheets",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF065F46),
                                            fontSize = 14.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Informasi Area dari Master Pool yang belum didaftarkan di Google Sheets
            if (unconfiguredPoolAreas.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Area di Master Pool Belum Ada di Google Sheets",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E)
                                    )
                                )
                                Text(
                                    text = "Status Area berikut ada pada Master Pool namun belum tercatat di Google Sheets: ${unconfiguredPoolAreas.joinToString(", ")}. Klik 'TAMBAH AREA' untuk mendaftarkannya.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF78350F),
                                        fontSize = 11.5.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 2. Search Box
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                            .testTag("search_status_area"),
                        placeholder = {
                            Text(
                                text = "Cari status area...",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate400)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = Slate400,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HighDensityBlue,
                            unfocusedBorderColor = Slate200
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // 3. TABLE OF STATUS AREA & JUMLAH SAMPLE
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column {
                        // TABLE HEADER
                        // Column 1: Status Area pada master pool
                        // Column 2: Jumlah Sample
                        Surface(
                            color = HighDensityBlue,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Kolom 1: Status Area
                                Text(
                                    text = "STATUS AREA",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.5.sp,
                                        color = Color.White
                                    ),
                                    modifier = Modifier.weight(0.58f)
                                )

                                // Kolom 2: Jumlah Sample
                                Text(
                                    text = "JUMLAH SAMPLE",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.5.sp,
                                        color = Color.White
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(0.28f)
                                )

                                // Action Column
                                Text(
                                    text = "AKSI",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.5.sp,
                                        color = Color.White
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(0.14f)
                                )
                            }
                        }

                        // TABLE ROWS
                        if (filteredRows.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = if (sampleList.isEmpty()) Icons.Default.CloudDownload else Icons.Default.Pin,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (sampleList.isEmpty()) "Belum ada data di tabel MASTER_SAMPLE Google Sheets." else "Tidak ada Status Area yang cocok dengan pencarian.",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = Slate600,
                                        textAlign = TextAlign.Center
                                    )
                                    if (sampleList.isEmpty() && onSyncGoogleSheet != null) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = { onSyncGoogleSheet() },
                                            enabled = !isSyncing,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            if (isSyncing) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    color = Color.White,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Tarik Data dari Google Sheets", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            filteredRows.forEachIndexed { index, row ->
                                val isEven = index % 2 == 0
                                val rowBg = if (isEven) Color.White else Color(0xFFF8FAFC)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(rowBg)
                                        .clickable { editingSample = row }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                        .testTag("row_sample_${row.statusArea}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Column 1: Status Area
                                    Column(modifier = Modifier.weight(0.58f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(HighDensityBlue)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = row.statusArea,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 13.5.sp,
                                                    color = Slate900
                                                )
                                            )
                                        }
                                        if (row.keterangan.isNotBlank()) {
                                            Text(
                                                text = row.keterangan,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.sp,
                                                    color = Slate500
                                                ),
                                                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                                            )
                                        }
                                    }

                                    // Column 2: Jumlah Sample
                                    Box(
                                        modifier = Modifier.weight(0.28f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            color = HighDensityBlue.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, HighDensityBlue.copy(alpha = 0.3f)),
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = "${row.jumlahSample}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 15.sp,
                                                        color = HighDensityBlue
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "unit",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Slate600
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // Action buttons
                                    Row(
                                        modifier = Modifier.weight(0.14f),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { editingSample = row },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = HighDensityBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                if (index < filteredRows.lastIndex) {
                                    HorizontalDivider(color = Slate200, thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // EDIT SAMPLE DIALOG
    editingSample?.let { sample ->
        EditSampleDialog(
            sample = sample,
            onDismiss = { editingSample = null },
            onConfirm = { updatedSample ->
                onSaveSample(updatedSample)
                editingSample = null
            }
        )
    }

    // TAMBAH SAMPLE AREA DIALOG
    if (isAddingNewSample) {
        AddSampleDialog(
            existingAreas = sampleList.map { it.statusArea.trim().uppercase() },
            onDismiss = { isAddingNewSample = false },
            onConfirm = { newSample ->
                onSaveSample(newSample)
                isAddingNewSample = false
            }
        )
    }
}

/**
 * Dialog to edit Jumlah Sample for a specific Status Area
 */
@Composable
private fun EditSampleDialog(
    sample: MasterSampleEntity,
    onDismiss: () -> Unit,
    onConfirm: (MasterSampleEntity) -> Unit
) {
    var sampleCountText by remember { mutableStateOf(sample.jumlahSample.toString()) }
    var keteranganText by remember { mutableStateOf(sample.keterangan) }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(HighDensityBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pin,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Edit Jumlah Sample",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Slate900
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Area: ${sample.statusArea}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = HighDensityBlue,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = sampleCountText,
                    onValueChange = {
                        sampleCountText = it
                        errorMessage = ""
                    },
                    label = { Text("Jumlah Sample (Unit)") },
                    placeholder = { Text("Masukkan jumlah sample, cth: 10") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorMessage.isNotBlank(),
                    supportingText = if (errorMessage.isNotBlank()) {
                        { Text(text = errorMessage, color = Rose600) }
                    } else {
                        { Text("Nilai ini menentukan kuota sample armada saat pembobotan.") }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_jumlah_sample"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HighDensityBlue,
                        unfocusedBorderColor = Slate300
                    )
                )

                // Quick presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 15, 20, 25).forEach { preset ->
                        Surface(
                            onClick = { sampleCountText = preset.toString() },
                            color = if (sampleCountText == preset.toString()) HighDensityBlue else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "$preset",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (sampleCountText == preset.toString()) Color.White else Slate700
                                ),
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = keteranganText,
                    onValueChange = { keteranganText = it },
                    label = { Text("Keterangan (Opsional)") },
                    placeholder = { Text("Cth: Standar sample area ...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HighDensityBlue,
                        unfocusedBorderColor = Slate300
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = sampleCountText.trim().toIntOrNull()
                    if (count == null || count <= 0) {
                        errorMessage = "Jumlah sample harus berupa angka bulat positif lebih dari 0"
                        return@Button
                    }
                    onConfirm(
                        sample.copy(
                            jumlahSample = count,
                            keterangan = keteranganText.trim(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = HighDensityBlue),
                modifier = Modifier.testTag("btn_save_sample")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simpan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Slate600)
            }
        }
    )
}

/**
 * Dialog to add a new Status Area and its Jumlah Sample to tabel SAMPLE
 */
@Composable
private fun AddSampleDialog(
    existingAreas: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (MasterSampleEntity) -> Unit
) {
    var statusAreaText by remember { mutableStateOf("") }
    var sampleCountText by remember { mutableStateOf("10") }
    var keteranganText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HighDensityBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Tambah Status Area Sample",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Slate900
                    )
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = statusAreaText,
                    onValueChange = {
                        statusAreaText = it.uppercase()
                        errorMessage = ""
                    },
                    label = { Text("Nama Status Area *") },
                    placeholder = { Text("Cth: JAWA TIMUR, SUMATERA UTARA") },
                    isError = errorMessage.isNotBlank(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HighDensityBlue,
                        unfocusedBorderColor = Slate300
                    )
                )

                OutlinedTextField(
                    value = sampleCountText,
                    onValueChange = {
                        sampleCountText = it
                        errorMessage = ""
                    },
                    label = { Text("Jumlah Sample (Unit) *") },
                    placeholder = { Text("Masukkan kuota sample, cth: 15") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorMessage.isNotBlank(),
                    supportingText = if (errorMessage.isNotBlank()) {
                        { Text(text = errorMessage, color = Rose600) }
                    } else {
                        { Text("Area ini akan otomatis muncul pada dropdown Status Area Master Pool.") }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HighDensityBlue,
                        unfocusedBorderColor = Slate300
                    )
                )

                // Quick presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 15, 20, 25).forEach { preset ->
                        Surface(
                            onClick = { sampleCountText = preset.toString() },
                            color = if (sampleCountText == preset.toString()) HighDensityBlue else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "$preset",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (sampleCountText == preset.toString()) Color.White else Slate700
                                ),
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = keteranganText,
                    onValueChange = { keteranganText = it },
                    label = { Text("Keterangan (Opsional)") },
                    placeholder = { Text("Cth: Standar armada area ...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HighDensityBlue,
                        unfocusedBorderColor = Slate300
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanArea = statusAreaText.trim().uppercase()
                    if (cleanArea.isBlank()) {
                        errorMessage = "Nama Status Area wajib diisi!"
                        return@Button
                    }
                    if (existingAreas.any { it.trim().equals(cleanArea, ignoreCase = true) }) {
                        errorMessage = "Status Area '$cleanArea' sudah ada dalam daftar!"
                        return@Button
                    }
                    val count = sampleCountText.trim().toIntOrNull()
                    if (count == null || count <= 0) {
                        errorMessage = "Jumlah sample harus berupa angka bulat positif lebih dari 0"
                        return@Button
                    }
                    onConfirm(
                        MasterSampleEntity(
                            statusArea = cleanArea,
                            jumlahSample = count,
                            keterangan = keteranganText.trim().ifBlank { "Standar sample armada area $cleanArea" },
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = HighDensityBlue)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simpan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Slate600)
            }
        }
    )
}

