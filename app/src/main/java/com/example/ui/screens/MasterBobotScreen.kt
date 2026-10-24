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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MasterBobotEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.KpiAuditSpec
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

/**
 * Data class representing a display row for Data Input in Master Bobot Table.
 */
data class MasterBobotTableRow(
    val itemId: String,
    val itemName: String,
    val sectionTitle: String,
    val groupTitle: String,
    val pointMax: Int,
    val bobot: Double
)

@Composable
fun MasterBobotScreen(
    bobotList: List<MasterBobotEntity>,
    currentUser: UserEntity?,
    onUpdateBobot: (String, Double) -> Unit,
    onResetAllBobot: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isSuperAdmin = currentUser?.levelAkses?.trim()?.uppercase() == "SUPER ADMIN"
    val isAdmin = currentUser?.levelAkses?.trim()?.uppercase() == "ADMIN"
    val canAccess = isSuperAdmin || isAdmin

    // Access guard: Super Admin & Admin can open and modify this screen
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
                            contentDescription = "Akses Dibatasi",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Akses Dibatasi",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Menu Pengaturan Master BOBOT hanya dapat dibuka dan dimodifikasi oleh SUPER ADMIN atau ADMIN.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate600,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    // State for filtering & searching
    var searchQuery by remember { mutableStateOf("") }
    var selectedSectionFilter by remember { mutableStateOf("SEMUA") }

    // Dialog states
    var editingRow by remember { mutableStateOf<MasterBobotTableRow?>(null) }

    // Prepare all rows mapped from KpiAuditSpec (data input source) merged with current bobot in Room DB
    val allRows = remember(bobotList) {
        val bobotMap = bobotList.associateBy { it.itemId }
        val rows = mutableListOf<MasterBobotTableRow>()

        KpiAuditSpec.sections.forEach { section ->
            section.groups.forEach { group ->
                group.items.forEach { item ->
                    val entity = bobotMap[item.id]
                    rows.add(
                        MasterBobotTableRow(
                            itemId = item.id,
                            itemName = entity?.itemName?.ifBlank { item.name } ?: item.name,
                            sectionTitle = entity?.sectionTitle?.ifBlank { section.title } ?: section.title,
                            groupTitle = entity?.groupTitle?.ifBlank { group.title } ?: group.title,
                            pointMax = entity?.pointMax ?: item.maxPoints,
                            bobot = entity?.bobot ?: item.defaultBobot
                        )
                    )
                }
            }
        }
        rows
    }

    // Default bobot map lookup
    val defaultBobotMap = remember { KpiAuditSpec.getAllItems().associate { it.id to it.defaultBobot } }

    // Filter rows based on search and section filter
    val filteredRows by remember(allRows, searchQuery, selectedSectionFilter) {
        derivedStateOf {
            allRows.filter { row ->
                val matchesSearch = searchQuery.isBlank() ||
                        row.itemName.contains(searchQuery, ignoreCase = true) ||
                        row.groupTitle.contains(searchQuery, ignoreCase = true) ||
                        row.sectionTitle.contains(searchQuery, ignoreCase = true)

                val matchesSection = when (selectedSectionFilter) {
                    "A. EXTERIOR" -> row.sectionTitle.contains("EXTERIOR", ignoreCase = true)
                    "B. INTERIOR" -> row.sectionTitle.contains("INTERIOR", ignoreCase = true)
                    "C. KEBERSIHAN" -> row.sectionTitle.contains("KEBERSIHAN", ignoreCase = true) || row.sectionTitle.contains("CUCI", ignoreCase = true)
                    else -> true
                }

                matchesSearch && matchesSection
            }
        }
    }

    // Statistics
    val totalItems = allRows.size
    val customBobotCount = allRows.count { it.bobot != (defaultBobotMap[it.itemId] ?: 1.0) }
    val avgBobot = if (totalItems > 0) allRows.sumOf { it.bobot } / totalItems else 0.0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(HighDensityBg)
            .testTag("master_bobot_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header & Security Banner
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
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
                                    imageVector = Icons.Default.Scale,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "PENGATURAN MASTER BOBOT",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp,
                                        color = Slate900
                                    )
                                )
                                Text(
                                    text = "Tabel Bobot Penilaian untuk Item Data Input Audit",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Slate600,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Slate200)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Summary Quick Stats Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            label = "Total Data Input",
                            value = "$totalItems Item",
                            modifier = Modifier.weight(1f),
                            badgeColor = HighDensityBlue
                        )
                        StatCard(
                            label = "Rata-rata Bobot",
                            value = "%.2fx".format(avgBobot),
                            modifier = Modifier.weight(1f),
                            badgeColor = Color(0xFF0D9488)
                        )
                        StatCard(
                            label = "Bobot Disesuaikan",
                            value = "$customBobotCount Item",
                            modifier = Modifier.weight(1f),
                            badgeColor = if (customBobotCount > 0) Color(0xFFD97706) else Slate500
                        )
                    }
                }
            }
        }

        // 2. Filter, Search, and Reset Controls
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Slate200)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari item data input...", fontSize = 13.sp, color = Slate400) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Slate400,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = Slate400,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HighDensityBlue,
                            unfocusedBorderColor = Slate300
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("search_bobot_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter Chips for Sections
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SEMUA", "A. EXTERIOR", "B. INTERIOR", "C. KEBERSIHAN").forEach { filterName ->
                            val isSelected = selectedSectionFilter == filterName
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSectionFilter = filterName },
                                label = {
                                    Text(
                                        text = filterName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HighDensityBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFF1F5F9),
                                    labelColor = Slate700
                                ),
                                border = null,
                                modifier = Modifier.testTag("filter_chip_$filterName")
                            )
                        }
                    }
                }
            }
        }

        // 3. TABLE OF DATA INPUT WITH COLUMN 2 TITLED "BOBOT"
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Slate200)
            ) {
                Column {
                    // TABLE HEADER
                    // Column 1: "DATA INPUT"
                    // Column 2: "BOBOT"
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
                            // Column 1 Header: DATA INPUT
                            Row(
                                modifier = Modifier.weight(0.68f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DATA INPUT",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        letterSpacing = 0.5.sp,
                                        color = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${filteredRows.size})",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                )
                            }

                            // Column 2 Header: BOBOT
                            Text(
                                text = "BOBOT",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.5.sp,
                                    color = Color.White
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.32f)
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
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tidak ada item data input yang cocok",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Slate500
                                )
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
                                    .clickable { editingRow = row }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("table_row_${row.itemId}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // COLUMN 1: DATA INPUT CONTENT
                                Row(
                                    modifier = Modifier.weight(0.68f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Row index badge
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Slate200),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate700
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = row.itemName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Slate900
                                            ),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${row.groupTitle} • Maks ${row.pointMax} poin",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 11.sp,
                                                    color = Slate500
                                                )
                                            )
                                        }
                                    }
                                }

                                // COLUMN 2: BOBOT CONTENT
                                Row(
                                    modifier = Modifier.weight(0.32f),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val defBobot = defaultBobotMap[row.itemId] ?: 1.0
                                    val isCustom = row.bobot != defBobot
                                    val displayVal = if (row.bobot % 1.0 == 0.0) row.bobot.toInt().toString() else "%.1f".format(row.bobot)
                                    Surface(
                                        color = if (isCustom) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isCustom) HighDensityBlue else Slate300
                                        ),
                                        modifier = Modifier.clickable { editingRow = row }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = displayVal,
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 13.sp,
                                                    color = if (isCustom) HighDensityBlue else Slate800
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Bobot",
                                                tint = if (isCustom) HighDensityBlue else Slate400,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (index < filteredRows.lastIndex) {
                                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }

        // Info footer
        item {
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = Slate500,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Petunjuk: Klik pada nilai di kolom BOBOT untuk mengubah nilai pembobotan parameter data input. Perubahan langsung tersimpan otomatis di database sistem.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    )
                }
            }
        }
    }

    // DIALOG: EDIT BOBOT DATA INPUT
    if (editingRow != null) {
        val target = editingRow!!
        val defBobot = defaultBobotMap[target.itemId] ?: 1.0
        val defDisplay = if (defBobot % 1.0 == 0.0) defBobot.toInt().toString() else defBobot.toString()
        val initialInput = if (target.bobot % 1.0 == 0.0) target.bobot.toInt().toString() else target.bobot.toString()
        var bobotInput by remember { mutableStateOf(initialInput) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editingRow = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Scale,
                        contentDescription = null,
                        tint = HighDensityBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ubah BOBOT Data Input",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Item details card
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = target.itemName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${target.sectionTitle} • ${target.groupTitle}",
                                style = MaterialTheme.typography.labelSmall.copy(color = Slate600)
                            )
                            Text(
                                text = "Poin Temuan Maks: ${target.pointMax}",
                                style = MaterialTheme.typography.labelSmall.copy(color = Slate600)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Nilai Bobot Baru (Coloum BOBOT):",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Slate700
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = bobotInput,
                        onValueChange = {
                            bobotInput = it
                            errorMessage = null
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = errorMessage != null,
                        supportingText = {
                            if (errorMessage != null) {
                                Text(text = errorMessage!!, color = Color.Red, fontSize = 11.sp)
                            } else {
                                Text(text = "Nilai standar: $defDisplay", fontSize = 11.sp, color = Slate500)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HighDensityBlue,
                            unfocusedBorderColor = Slate300
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_bobot_textfield")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick presets chips
                    Text(
                        text = "Preset Cepat:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Slate600
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = if (bobotInput == defDisplay) HighDensityBlue else Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable {
                                bobotInput = defDisplay
                                errorMessage = null
                            }
                        ) {
                            Text(
                                text = "Standar ($defDisplay)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (bobotInput == defDisplay) Color.White else Slate700,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = bobotInput.replace(',', '.').toDoubleOrNull()
                        if (parsed == null || parsed <= 0.0) {
                            errorMessage = "Masukkan angka bobot valid lebih dari 0!"
                        } else {
                            onUpdateBobot(target.itemId, parsed)
                            editingRow = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("save_bobot_button")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simpan Bobot")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingRow = null }) {
                    Text("Batal", color = Slate600)
                }
            }
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Slate200),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = Slate500,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = badgeColor
                )
            )
        }
    }
}
