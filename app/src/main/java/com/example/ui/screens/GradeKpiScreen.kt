package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MasterGradeKpiEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.theme.Amber50
import com.example.ui.theme.Amber600
import com.example.ui.theme.Blue50
import com.example.ui.theme.Blue700
import com.example.ui.theme.HighDensityBg
import com.example.ui.theme.HighDensityBlue
import com.example.ui.theme.HighDensityBlueDark
import com.example.ui.theme.Rose50
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Teal50
import com.example.ui.theme.Teal600
import java.util.Locale

/**
 * Helper untuk mendapatkan warna khas tiap Kategori Grade (A, B, C, D, E)
 */
private fun getGradeColors(kategori: String): Pair<Color, Color> {
    return when (kategori.trim().uppercase()) {
        "A" -> Pair(Teal600, Teal50)
        "B" -> Pair(Blue700, Blue50)
        "C" -> Pair(Amber600, Amber50)
        "D" -> Pair(Color(0xFFEA580C), Color(0xFFFFF7ED)) // Orange
        "E" -> Pair(Rose600, Rose50)
        else -> Pair(Slate700, Slate100)
    }
}

/**
 * Format desimal ke format tampilan 2 digit di belakang koma (misal: "0,00" atau "80,00")
 */
private fun formatDecimal(value: Double): String {
    return String.format(Locale("id", "ID"), "%.2f", value)
}

/**
 * Super Admin Screen: GRADE KPI
 * Menampilkan tabel Grade KPI dengan:
 * - Kolom 1: KATEGORY (berisi A, B, C, D, E)
 * - Kolom 2: Rentang Nilai Desimal (0,00 sampai 80,00)
 * Digunakan untuk menentukan nilai Grade A, B, C, D, E.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeKpiScreen(
    gradeList: List<MasterGradeKpiEntity>,
    currentUser: UserEntity?,
    onSaveGrade: (MasterGradeKpiEntity) -> Unit,
    onResetDefault: () -> Unit,
    onSyncToCloud: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isSuperAdmin = currentUser?.levelAkses?.trim()?.uppercase() == "SUPER ADMIN"

    // Access guard: Hanya Super Admin yang berhak mengakses menu ini
    if (!isSuperAdmin) {
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
                            .background(Color(0xFFFEE2E2), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Terkunci",
                            tint = Rose600,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Akses Khusus Super Admin",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Slate900,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Menu GRADE KPI hanya dapat diakses dan dikonfigurasi oleh SUPER ADMINISTRATOR.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate600,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    // Standard categories list A, B, C, D, E
    val targetCategories = listOf("A", "B", "C", "D", "E")

    // Merge loaded data with standard categories to guarantee all 5 categories are displayed
    val displayList = remember(gradeList) {
        val map = gradeList.associateBy { it.kategori.trim().uppercase() }
        targetCategories.map { cat ->
            map[cat] ?: when (cat) {
                "A" -> MasterGradeKpiEntity("A", 70.00, 80.00, "Sangat Baik (Grade A)")
                "B" -> MasterGradeKpiEntity("B", 60.00, 69.99, "Baik (Grade B)")
                "C" -> MasterGradeKpiEntity("C", 50.00, 59.99, "Cukup (Grade C)")
                "D" -> MasterGradeKpiEntity("D", 30.00, 49.99, "Kurang (Grade D)")
                else -> MasterGradeKpiEntity("E", 0.00, 29.99, "Kritis / Perlu Perbaikan (Grade E)")
            }
        }
    }

    var selectedGradeForEdit by remember { mutableStateOf<MasterGradeKpiEntity?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HighDensityBg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(HighDensityBlue.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Grade,
                                        contentDescription = "Grade KPI",
                                        tint = HighDensityBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "GRADE KPI",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = (-0.5).sp
                                            ),
                                            color = Slate900
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = HighDensityBlue
                                        ) {
                                            Text(
                                                text = "SUPER ADMIN",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Tabel Konfigurasi Rentang Nilai Desimal (0,00 s/d 80,00)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate600
                                    )
                                }
                            }

                            // Action Buttons
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Tombol Sinkron ke Cloud
                                OutlinedButton(
                                    onClick = { onSyncToCloud() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HighDensityBlue),
                                    border = BorderStroke(1.dp, HighDensityBlue.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("sync_grade_kpi_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Sinkron",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Sync Cloud",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                // Tombol Reset ke Standar Bawaan
                                OutlinedButton(
                                    onClick = { showResetDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700),
                                    border = BorderStroke(1.dp, Slate300),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("reset_grade_kpi_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Reset Default",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Main Table Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // TABEL HEADER (Sesuai Permintaan User: Col 1 KATEGORY, Col 2 RANGE DESIMAL 0,00 - 80,00)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(HighDensityBlue)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Coloum 1: KATEGORY (berisi A, B, C, D, E)
                            Text(
                                text = "KATEGORY",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color.White,
                                modifier = Modifier.weight(1.2f)
                            )

                            // Coloum 2: Nilai range desimal 0,00 sampai 80,00
                            Text(
                                text = "NILAI RANGE DESIMAL (0,00 - 80,00)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color.White,
                                modifier = Modifier.weight(2.6f)
                            )

                            // Coloum 3: AKSI
                            Text(
                                text = "AKSI",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1.0f)
                            )
                        }

                        // ROWS: A, B, C, D, E
                        displayList.forEachIndexed { index, grade ->
                            val (badgeColor, badgeBg) = getGradeColors(grade.kategori)
                            val isEvenRow = index % 2 == 0
                            val rowBg = if (isEvenRow) Color.White else Slate50

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowBg)
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Kolom 1: KATEGORY (A, B, C, D, E)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(badgeColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = grade.kategori,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 18.sp
                                            ),
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Grade ${grade.kategori}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Slate900
                                        )
                                        if (grade.keterangan.isNotBlank()) {
                                            Text(
                                                text = grade.keterangan,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = Slate500,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                // Kolom 2: NILAI RANGE DESIMAL (0,00 sampai 80,00)
                                Column(
                                    modifier = Modifier.weight(2.6f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = badgeBg,
                                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "RENTANG NILAI",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = badgeColor
                                                )
                                                Text(
                                                    text = "${formatDecimal(grade.minScore)}  s/d  ${formatDecimal(grade.maxScore)}",
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 14.sp
                                                    ),
                                                    color = Slate900
                                                )
                                            }

                                            // Mini visual indicator
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = badgeColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "Maks 80,00",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    color = badgeColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Kolom 3: AKSI
                                Box(
                                    modifier = Modifier.weight(1.0f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Button(
                                        onClick = { selectedGradeForEdit = grade },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = HighDensityBlue,
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("edit_grade_${grade.kategori}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Ubah",
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Ubah",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }

                            if (index < displayList.size - 1) {
                                HorizontalDivider(color = Slate200)
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG EDIT RANGE GRADE KPI
    if (selectedGradeForEdit != null) {
        val grade = selectedGradeForEdit!!
        var minStr by remember(grade) { mutableStateOf(formatDecimal(grade.minScore).replace(",", ".")) }
        var maxStr by remember(grade) { mutableStateOf(formatDecimal(grade.maxScore).replace(",", ".")) }
        var noteStr by remember(grade) { mutableStateOf(grade.keterangan) }
        var errorMessage by remember { mutableStateOf("") }

        val (diagColor, diagBg) = getGradeColors(grade.kategori)

        AlertDialog(
            onDismissRequest = { selectedGradeForEdit = null },
            shape = RoundedCornerShape(16.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(diagColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = grade.kategori,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Edit Range Grade ${grade.kategori}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Slate900
                        )
                        Text(
                            text = "Kategori ${grade.kategori} (Batas 0,00 s/d 80,00)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Atur batas minimal dan maksimal nilai desimal untuk Grade ${grade.kategori}:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Input Min Score
                        OutlinedTextField(
                            value = minStr,
                            onValueChange = {
                                minStr = it
                                errorMessage = ""
                            },
                            label = { Text("Nilai Min (0,00)") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_min_score")
                        )

                        // Input Max Score
                        OutlinedTextField(
                            value = maxStr,
                            onValueChange = {
                                maxStr = it
                                errorMessage = ""
                            },
                            label = { Text("Nilai Max (80,00)") },
                            placeholder = { Text("80.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_max_score")
                        )
                    }

                    // Keterangan / Label Grade
                    OutlinedTextField(
                        value = noteStr,
                        onValueChange = { noteStr = it },
                        label = { Text("Keterangan Grade") },
                        placeholder = { Text("Misal: Sangat Baik / Memuaskan") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_keterangan_grade")
                    )

                    if (errorMessage.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Rose50,
                            border = BorderStroke(1.dp, Rose600.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage,
                                color = Rose600,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedMin = minStr.replace(",", ".").trim().toDoubleOrNull()
                        val parsedMax = maxStr.replace(",", ".").trim().toDoubleOrNull()

                        if (parsedMin == null || parsedMax == null) {
                            errorMessage = "Nilai minimal dan maksimal harus berupa angka desimal valid."
                            return@Button
                        }

                        if (parsedMin < 0.0 || parsedMax > 80.0) {
                            errorMessage = "Nilai harus berada dalam rentang 0,00 sampai 80,00."
                            return@Button
                        }

                        if (parsedMin > parsedMax) {
                            errorMessage = "Nilai minimal tidak boleh lebih besar dari nilai maksimal."
                            return@Button
                        }

                        onSaveGrade(
                            grade.copy(
                                minScore = parsedMin,
                                maxScore = parsedMax,
                                keterangan = noteStr.trim(),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        selectedGradeForEdit = null
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityBlue),
                    modifier = Modifier.testTag("save_grade_dialog_button")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simpan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedGradeForEdit = null },
                    modifier = Modifier.testTag("cancel_grade_dialog_button")
                ) {
                    Text("Batal", color = Slate600)
                }
            }
        )
    }

    // DIALOG KONFIRMASI RESET DEFAULT
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Reset Grade KPI ke Default?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Slate900
                )
            },
            text = {
                Text(
                    text = "Semua rentang nilai untuk Grade A, B, C, D, dan E akan dikembalikan ke konfigurasi standar:\n\n" +
                            "• Grade A : 70,00 - 80,00 (Sangat Baik)\n" +
                            "• Grade B : 60,00 - 69,99 (Baik)\n" +
                            "• Grade C : 50,00 - 59,99 (Cukup)\n" +
                            "• Grade D : 30,00 - 49,99 (Kurang)\n" +
                            "• Grade E : 0,00 - 29,99 (Kritis / Perlu Perbaikan)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate700
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetDefault()
                        showResetDialog = false
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                    modifier = Modifier.testTag("confirm_reset_grade_button")
                ) {
                    Text("Ya, Reset Default", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Batal", color = Slate600)
                }
            }
        )
    }
}
