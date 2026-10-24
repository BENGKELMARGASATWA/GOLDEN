package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MasterPoolEntity
import com.example.data.local.entity.MasterSampleEntity
import com.example.ui.theme.Blue50
import com.example.ui.theme.Blue700
import com.example.ui.theme.Blue900
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate800

val STATUS_AREA_OPTIONS = listOf(
    "JABODETABEK",
    "JABODETABEK SATELIT",
    "DAERAH",
    "DAERAH SATELIT"
)

private fun getStatusAreaColors(statusArea: String): Pair<Color, Color> {
    val clean = statusArea.trim().uppercase()
    return when {
        clean == "JABODETABEK" -> Pair(Color(0xFFDCFCE7), Color(0xFF15803D)) // Emerald/Green
        clean == "JABODETABEK SATELIT" -> Pair(Color(0xFFE0F2FE), Color(0xFF0369A1)) // Sky Blue
        clean == "DAERAH" -> Pair(Color(0xFFFEF3C7), Color(0xFFB45309)) // Amber/Orange
        clean == "DAERAH SATELIT" -> Pair(Color(0xFFF3E8FF), Color(0xFF6B21A8)) // Purple
        clean.contains("SATELIT") -> Pair(Color(0xFFE0E7FF), Color(0xFF4338CA)) // Indigo
        clean.contains("DAERAH") -> Pair(Color(0xFFFEF3C7), Color(0xFFB45309))
        clean.contains("JABO") -> Pair(Color(0xFFDCFCE7), Color(0xFF15803D))
        else -> {
            val palette = listOf(
                Pair(Color(0xFFDCFCE7), Color(0xFF15803D)),
                Pair(Color(0xFFE0F2FE), Color(0xFF0369A1)),
                Pair(Color(0xFFFEF3C7), Color(0xFFB45309)),
                Pair(Color(0xFFF3E8FF), Color(0xFF6B21A8)),
                Pair(Color(0xFFFFEDD5), Color(0xFFC2410C)),
                Pair(Color(0xFFCCFBF1), Color(0xFF0F766E))
            )
            val index = Math.abs(clean.hashCode()) % palette.size
            palette[index]
        }
    }
}

@Composable
fun MasterPoolScreen(
    pools: List<MasterPoolEntity>,
    sampleList: List<MasterSampleEntity> = emptyList(),
    onSavePool: (MasterPoolEntity) -> Unit,
    onDeletePool: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var editingPool by remember { mutableStateOf<MasterPoolEntity?>(null) }

    // Opsi Status Area dinamis mengikuti dari tabel SAMPLE (MasterSampleEntity)
    val statusAreaOptions = remember(sampleList) {
        val fromSamples = sampleList
            .map { it.statusArea.trim().uppercase() }
            .filter { it.isNotBlank() }
            .distinct()
        if (fromSamples.isNotEmpty()) fromSamples else STATUS_AREA_OPTIONS
    }

    val sampleInfoMap = remember(sampleList) {
        sampleList.associateBy { it.statusArea.trim().uppercase() }
    }

    val effectiveStatusAreaOptions = remember(statusAreaOptions, editingPool) {
        val currentArea = editingPool?.statusArea?.trim()?.uppercase().orEmpty()
        if (currentArea.isNotBlank() && !statusAreaOptions.contains(currentArea)) {
            listOf(currentArea) + statusAreaOptions
        } else {
            statusAreaOptions
        }
    }

    var kodePool by remember { mutableStateOf("") }
    var namaPool by remember { mutableStateOf("") }
    var alamatPool by remember { mutableStateOf("") }
    var statusArea by remember(effectiveStatusAreaOptions) {
        mutableStateOf(effectiveStatusAreaOptions.firstOrNull() ?: "JABODETABEK")
    }
    var expandedStatusArea by remember { mutableStateOf(false) }
    var dialogErrorMessage by remember { mutableStateOf<String?>(null) }

    val filteredPools = remember(pools, searchQuery) {
        if (searchQuery.isBlank()) pools else {
            val q = searchQuery.trim().lowercase()
            pools.filter {
                it.kodePool.lowercase().contains(q) ||
                        it.namaPool.lowercase().contains(q) ||
                        it.alamatPool.lowercase().contains(q) ||
                        it.statusArea.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
                    // Tombol Input Di Kiri Atas
                    Button(
                        onClick = {
                            editingPool = null
                            kodePool = ""
                            namaPool = ""
                            alamatPool = ""
                            statusArea = effectiveStatusAreaOptions.firstOrNull() ?: "JABODETABEK"
                            expandedStatusArea = false
                            dialogErrorMessage = null
                            showDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Blue700),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_tambah_pool")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TAMBAH POOL", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Surface(
                        color = Blue50,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Total: ${pools.size} Pool",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Blue700),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari Kode Pool, Nama Pool, Area, Alamat...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Blue700)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue700,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredPools.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Tidak ada data pool yang sesuai.", style = MaterialTheme.typography.bodyMedium.copy(color = Slate600))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredPools, key = { it.id }) { item ->
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
                                // Badges: Kode Pool & Status Area
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = Blue50,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(0.5.dp, Color(0xFFBFDBFE))
                                    ) {
                                        Text(
                                            text = item.kodePool,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Blue700
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }

                                    val (areaBg, areaText) = getStatusAreaColors(item.statusArea)
                                    Surface(
                                        color = areaBg,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(0.5.dp, areaText.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Public,
                                                contentDescription = null,
                                                tint = areaText,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = item.statusArea.ifBlank { "JABODETABEK" },
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = areaText,
                                                    fontSize = 10.5.sp
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Nama Pool (Huruf Kapital)
                                Text(
                                    text = item.namaPool,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Blue900,
                                        fontSize = 14.sp
                                    )
                                )

                                // Alamat Pool
                                if (item.alamatPool.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Slate400,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = item.alamatPool,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Slate600,
                                                fontSize = 11.5.sp
                                            ),
                                            maxLines = 2
                                        )
                                    }
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        editingPool = item
                                        kodePool = item.kodePool
                                        namaPool = item.namaPool
                                        alamatPool = item.alamatPool
                                        statusArea = item.statusArea.ifBlank { effectiveStatusAreaOptions.firstOrNull() ?: "JABODETABEK" }
                                        expandedStatusArea = false
                                        dialogErrorMessage = null
                                        showDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Blue700)
                                }

                                IconButton(
                                    onClick = { onDeletePool(item.id) }
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

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = if (editingPool == null) "Tambah Master Pool" else "Edit Master Pool",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Blue900)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Error Banner
                    if (dialogErrorMessage != null) {
                        Surface(
                            color = Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Rose600,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = dialogErrorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Rose600,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }

                    // 1. Kode Pool (huruf kapital wajib isi)
                    OutlinedTextField(
                        value = kodePool,
                        onValueChange = {
                            kodePool = it.uppercase()
                            dialogErrorMessage = null
                        },
                        label = { Text("Kode Pool * (Wajib Kapital)") },
                        placeholder = { Text("Contoh: POOL-01 atau SKT") },
                        singleLine = true,
                        isError = dialogErrorMessage != null && kodePool.isBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_kode_pool")
                    )

                    // 2. Nama Pool (Huruf kapital wajib isi)
                    OutlinedTextField(
                        value = namaPool,
                        onValueChange = {
                            namaPool = it.uppercase()
                            dialogErrorMessage = null
                        },
                        label = { Text("Nama Pool * (Wajib Kapital)") },
                        placeholder = { Text("Contoh: POOL JAKARTA BARAT") },
                        singleLine = true,
                        isError = dialogErrorMessage != null && namaPool.isBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_nama_pool")
                    )

                    // 3. Alamat Pool
                    OutlinedTextField(
                        value = alamatPool,
                        onValueChange = {
                            alamatPool = it
                            dialogErrorMessage = null
                        },
                        label = { Text("Alamat Pool") },
                        placeholder = { Text("Contoh: Jl. Daan Mogot KM 12") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_alamat_pool")
                    )

                    // 4. Status Area (Dropdown dinamis mengikuti dari tabel SAMPLE)
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Status Area * (Dari Tabel SAMPLE)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800,
                                    fontSize = 12.sp
                                )
                            )
                            if (sampleList.isNotEmpty()) {
                                Surface(
                                    color = Blue50,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${sampleList.size} Area SAMPLE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Blue700,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        // Dropdown Selector (Read-only TextField with DropdownMenu)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = statusArea,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { expandedStatusArea = !expandedStatusArea }) {
                                        Icon(
                                            imageVector = if (expandedStatusArea) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = "Pilih Status Area",
                                            tint = Blue700
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dropdown_status_area")
                            )

                            // Clickable overlay to open dropdown
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { expandedStatusArea = !expandedStatusArea }
                            )

                            DropdownMenu(
                                expanded = expandedStatusArea,
                                onDismissRequest = { expandedStatusArea = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (effectiveStatusAreaOptions.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Tidak ada Status Area di tabel SAMPLE", color = Slate400) },
                                        onClick = { expandedStatusArea = false }
                                    )
                                } else {
                                    effectiveStatusAreaOptions.forEach { option ->
                                        val isSelected = statusArea.trim().equals(option, ignoreCase = true)
                                        val (optBg, optText) = getStatusAreaColors(option)
                                        val sampleItem = sampleInfoMap[option]

                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Surface(
                                                            color = optBg,
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = option,
                                                                color = optText,
                                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                                ),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        if (sampleItem != null) {
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = "(${sampleItem.jumlahSample} Sample)",
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    color = Slate500,
                                                                    fontSize = 11.sp
                                                                )
                                                            )
                                                        }
                                                    }
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Blue700,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                statusArea = option
                                                expandedStatusArea = false
                                                dialogErrorMessage = null
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick Select Chips (Dinamis dari Status Area tabel SAMPLE)
                        if (effectiveStatusAreaOptions.isNotEmpty()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                effectiveStatusAreaOptions.chunked(2).forEach { rowOptions ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        rowOptions.forEach { option ->
                                            val isSelected = statusArea.trim().equals(option, ignoreCase = true)
                                            val (optBg, optText) = getStatusAreaColors(option)
                                            val sampleItem = sampleInfoMap[option]
                                            Surface(
                                                color = if (isSelected) optBg else Color(0xFFF8FAFC),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(
                                                    if (isSelected) 1.5.dp else 1.dp,
                                                    if (isSelected) optText else Color(0xFFE2E8F0)
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        statusArea = option
                                                        dialogErrorMessage = null
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = optText,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                    }
                                                    Text(
                                                        text = if (sampleItem != null) "$option (${sampleItem.jumlahSample})" else option,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) optText else Slate600,
                                                            fontSize = 10.sp
                                                        ),
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                        if (rowOptions.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanKode = kodePool.trim().uppercase()
                        val cleanNama = namaPool.trim().uppercase()
                        val cleanAlamat = alamatPool.trim()
                        val cleanStatusArea = statusArea.trim().uppercase()

                        if (cleanKode.isBlank()) {
                            dialogErrorMessage = "Kode Pool wajib diisi (Huruf Kapital)!"
                            return@Button
                        }
                        if (cleanNama.isBlank()) {
                            dialogErrorMessage = "Nama Pool wajib diisi (Huruf Kapital)!"
                            return@Button
                        }
                        if (cleanStatusArea.isBlank() || !effectiveStatusAreaOptions.any { it.equals(cleanStatusArea, ignoreCase = true) }) {
                            dialogErrorMessage = "Status Area wajib dipilih dari daftar tabel SAMPLE!"
                            return@Button
                        }

                        onSavePool(
                            MasterPoolEntity(
                                id = editingPool?.id ?: 0,
                                kodePool = cleanKode,
                                namaPool = cleanNama,
                                alamatPool = cleanAlamat,
                                statusArea = cleanStatusArea
                            )
                        )
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue700),
                    modifier = Modifier.testTag("btn_simpan_pool")
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
}

