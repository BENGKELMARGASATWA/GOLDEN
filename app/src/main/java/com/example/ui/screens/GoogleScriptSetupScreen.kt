package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.example.util.DateUtils
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.GoogleAppsScriptHub
import com.example.ui.theme.Blue50
import com.example.ui.theme.Blue700
import com.example.ui.theme.Blue900
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate800
import com.example.ui.theme.Teal50
import com.example.ui.theme.Teal600

@Composable
fun GoogleScriptSetupScreen(
    currentScriptUrl: String,
    currentLogoUrl: String,
    currentSheetId: String,
    currentFolderId: String,
    currentBigQuery: String,
    autoSyncEnabled: Boolean = true,
    autoSyncIntervalMinutes: Int = 30,
    lastReconcileTime: Long = 0L,
    lastReconcileSummary: String = "",
    isReconciling: Boolean = false,
    onToggleAutoSync: (Boolean) -> Unit = {},
    onSetAutoSyncInterval: (Int) -> Unit = {},
    onReconcileNow: () -> Unit = {},
    onSaveSettings: (String, String, String, String, String) -> Unit,
    onDownloadMasterData: () -> Unit,
    onSyncBobotToCloud: () -> Unit = {},
    onClearDemoData: () -> Unit = {},
    onClearAllAudits: () -> Unit = {},
    onClearAllKendaraan: () -> Unit = {},
    onInitInputAudit: ((Boolean, String) -> Unit) -> Unit = {},
    onInitGradeKpi: ((Boolean, String) -> Unit) -> Unit = {},
    onInitAllSheets: ((Boolean, String) -> Unit) -> Unit = {}
) {
    val context = LocalContext.current

    var scriptUrl by remember { mutableStateOf(currentScriptUrl) }
    var logoUrl by remember { mutableStateOf(currentLogoUrl) }
    var sheetId by remember { mutableStateOf(currentSheetId) }
    var folderId by remember { mutableStateOf(currentFolderId) }
    var bigQueryDataset by remember { mutableStateOf(currentBigQuery) }

    var selectedCodeTab by remember { mutableStateOf(0) } // 0: Code.gs, 1: index.html, 2: Init INPUT_AUDIT, 3: Init GRADE_KPI, 4: Setup Sheets

    var isInitializingAudit by remember { mutableStateOf(false) }
    var isInitializingAllSheets by remember { mutableStateOf(false) }
    var initStatusMessage by remember { mutableStateOf("") }

    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label disalin ke clipboard!", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card Configuration
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "KONFIGURASI INTEGRASI & LOGO",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Blue900
                    )
                )
                Text(
                    text = "Atur Google Apps Script Web App URL, ID Spreadsheet, Drive Folder, BigQuery, dan Link Logo.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Script Web App URL
                OutlinedTextField(
                    value = scriptUrl,
                    onValueChange = { scriptUrl = it },
                    label = { Text("Google Apps Script Web App URL (Exec)") },
                    placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = Blue700) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue700,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Logo URL
                OutlinedTextField(
                    value = logoUrl,
                    onValueChange = { logoUrl = it },
                    label = { Text("URL Link Logo Perusahaan (Opsional)") },
                    placeholder = { Text("https://contoh.com/logo-perusahaan.png") },
                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = Blue700) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue700,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sheet ID
                OutlinedTextField(
                    value = sheetId,
                    onValueChange = { sheetId = it },
                    label = { Text("Google Spreadsheet ID") },
                    leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null, tint = Blue700) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Drive Folder ID
                OutlinedTextField(
                    value = folderId,
                    onValueChange = { folderId = it },
                    label = { Text("Google Drive Folder ID (Penyimpanan Foto)") },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, tint = Blue700) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // BigQuery
                OutlinedTextField(
                    value = bigQueryDataset,
                    onValueChange = { bigQueryDataset = it },
                    label = { Text("Google BigQuery Dataset Name") },
                    placeholder = { Text("Kosongkan jika tidak digunakan") },
                    leadingIcon = { Icon(Icons.Default.Dataset, contentDescription = null, tint = Blue700) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onSaveSettings(
                                scriptUrl.trim(),
                                logoUrl.trim(),
                                sheetId.trim(),
                                folderId.trim(),
                                bigQueryDataset.trim()
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Blue700),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SIMPAN PENGATURAN")
                    }

                    OutlinedButton(
                        onClick = onDownloadMasterData,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TARIK MASTER DATA")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onSyncBobotToCloud,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SINKRONKAN / INISIALISASI MASTER BOBOT KE SHEETS")
                }
            }
        }

        // Auto Reconcile (Setiap 30 Menit Sekali) Card
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = Blue700,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "PENCOCOKAN OTOMATIS DATA",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Blue900
                                )
                            )
                            Text(
                                text = "Pengecekan berkala setiap 30 menit sekali",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Slate600,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    Switch(
                        checked = autoSyncEnabled,
                        onCheckedChange = onToggleAutoSync,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Blue700
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Slate100,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = Slate600, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Frekuensi Pengecekan:",
                                    style = MaterialTheme.typography.labelMedium.copy(color = Slate800, fontWeight = FontWeight.SemiBold)
                                )
                            }
                            Text(
                                text = "$autoSyncIntervalMinutes Menit Sekali",
                                style = MaterialTheme.typography.bodySmall.copy(color = Blue700, fontWeight = FontWeight.Bold)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = if (lastReconcileTime > 0) Teal600 else Slate600, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Terakhir Dicocokkan:",
                                    style = MaterialTheme.typography.labelMedium.copy(color = Slate800, fontWeight = FontWeight.SemiBold)
                                )
                            }
                            Text(
                                text = if (lastReconcileTime > 0) DateUtils.formatTimestampWithSeconds(lastReconcileTime) else "Belum pernah",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate800, fontWeight = FontWeight.Medium)
                            )
                        }

                        if (lastReconcileSummary.isNotBlank()) {
                            Text(
                                text = "Status: $lastReconcileSummary",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (lastReconcileSummary.contains("gagal", ignoreCase = true)) Color(0xFFDC2626) else Teal600,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onReconcileNow,
                    enabled = !isReconciling,
                    colors = ButtonDefaults.buttonColors(containerColor = Blue700),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isReconciling) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SEDANG MENCOCOKKAN DATA DENGAN SHEETS...")
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("COCOKKAN DATA SEKARANG")
                    }
                }
            }
        }

        // Inisialisasi Spreadsheet (INPUT_AUDIT & Master) Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            tint = Blue700,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "INISIALISASI SPREADSHEET (INPUT_AUDIT)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Blue900
                            )
                        )
                        Text(
                            text = "Siapkan 366 kolom standar (A1 s/d NB1) & format header otomatis",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Slate600,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Tombol di bawah akan menghubungi Google Apps Script untuk otomatis membuat/memperbarui sheet 'INPUT_AUDIT' dengan 366 kolom lengkap (A1 s/d NB1), styling header Navy Slate (#1E293B), membekukan baris 1 & kolom 1..7 (ID s/d NO_KENDARAAN), serta menata lebar kolom.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate800, fontSize = 12.sp)
                )

                if (initStatusMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = if (initStatusMessage.contains("gagal", ignoreCase = true) || initStatusMessage.contains("error", ignoreCase = true)) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = initStatusMessage,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (initStatusMessage.contains("gagal", ignoreCase = true) || initStatusMessage.contains("error", ignoreCase = true)) Color(0xFFDC2626) else Teal600,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        isInitializingAudit = true
                        initStatusMessage = "Sedang menyiapkan 366 kolom INPUT_AUDIT di Google Sheets..."
                        onInitInputAudit { success, msg ->
                            isInitializingAudit = false
                            initStatusMessage = msg
                        }
                    },
                    enabled = !isInitializingAudit && !isInitializingAllSheets,
                    colors = ButtonDefaults.buttonColors(containerColor = Blue700),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isInitializingAudit) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SEDANG MEMBUAT TABEL INPUT AUDIT...")
                    } else {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("INISIALISASI TABEL INPUT AUDIT (366 KOLOM)")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        isInitializingAllSheets = true
                        initStatusMessage = "Sedang menginisialisasi semua tabel (USER, POOL, KENDARAAN, BOBOT, SAMPLE, PERIODE, GRADE_KPI, INPUT_AUDIT)..."
                        onInitAllSheets { success, msg ->
                            isInitializingAllSheets = false
                            initStatusMessage = msg
                        }
                    },
                    enabled = !isInitializingAudit && !isInitializingAllSheets,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isInitializingAllSheets) {
                        CircularProgressIndicator(
                            color = Blue700,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Menyiapkan Semua Sheet...", style = MaterialTheme.typography.labelSmall)
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp), tint = Blue700)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Inisialisasi Semua Tabel (8 Sheet Lengkap)", style = MaterialTheme.typography.labelSmall.copy(color = Blue700))
                    }
                }
            }
        }

        // Code Copy-Paste Ready Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "KODE GOOGLE APPS SCRIPT (FULL)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Blue900
                            )
                        )

                        Button(
                            onClick = {
                                val (codeToCopy, label) = when (selectedCodeTab) {
                                    0 -> Pair(GoogleAppsScriptHub.getGoogleScriptCode(sheetId, folderId), "Code.gs")
                                    1 -> Pair(GoogleAppsScriptHub.getIndexHtmlCode(), "index.html")
                                    2 -> Pair(GoogleAppsScriptHub.getInputAuditInitScript(sheetId), "initInputAudit.gs")
                                    3 -> Pair(GoogleAppsScriptHub.getGradeKpiInitScript(sheetId), "initGradeKpi.gs")
                                    else -> Pair(
"""STRUKTUR GOOGLE SHEETS TERPUSAT (8 SHEET):

1. USER
   Kolom: NIP | NAMA_PETUGAS | USERNAMA | PASSWORD | LEVELAKSES | KODEPOOL | STATUS | TANGGAL_AKTIF
   Contoh: 1001 | Super Administrator | admin | 123 | SUPER ADMIN | SKT | AKTIF | 2026-01-01

2. MASTER_POOL
   Kolom: ID | KODE_POOL | NAMA_POOL | ALAMAT_POOL | STATUS_AREA
   Contoh: 1 | SKT | Pool SKT Pusat | Jl. Industri Raya No. 1 | JABODETABEK

3. MASTER_KENDARAAN
   Kolom: ID | NO_MOBIL | PLAT_NOMER | TIPE | WARNA | POOL | TAHUN | TANGGAL_SERVICE | SERVICE
   Contoh: 1 | 101 | B 1024 SKT | Toyota Avanza 1.5 G | Hitam Metalik | SKT | 2022 | 01/09/2026 | Berkala 40.000 KM

4. MASTER_BOBOT
   Kolom: ITEM_ID | ITEM_NAME | SECTION_TITLE | GROUP_TITLE | POINT_MAX | BOBOT
   Contoh: body_keropos | Keropos | A. REKAP TEMUAN KPI AUDIT EXTERIOR | 1. BODY | 1 | 30.0

5. MASTER_SAMPLE
   Kolom: STATUS_AREA | JUMLAH_SAMPLE | KETERANGAN
   Contoh: JABODETABEK | 20 | Standar sample armada area Jabodetabek

6. MASTER_PERIODE
   Kolom: ID | PERIODE | KETERANGAN
   Contoh: 1 | Periode 1 | Audit Periode 1 (Minggu 1)

7. MASTER_GRADE_KPI
   Kolom: KATEGORY | RANGE_NILAI_DESIMAL | KETERANGAN | MIN_SCORE | MAX_SCORE
   Contoh: A | 0,00 - 10,00 | Sangat Baik (Grade A) | 0.00 | 10.00
   Contoh: B | 10,01 - 25,00 | Baik (Grade B) | 10.01 | 25.00
   Contoh: C | 25,01 - 45,00 | Cukup (Grade C) | 25.01 | 45.00
   Contoh: D | 45,01 - 65,00 | Kurang (Grade D) | 45.01 | 65.00
   Contoh: E | 65,01 - 80,00 | Kritis / Perlu Perbaikan (Grade E) | 65.01 | 80.00

8. INPUT_AUDIT
   Kolom: 366 Kolom (A1 s/d NB1), meliputi ID, TANGGAL, TIMESTAMP, NIP, NAMA_PETUGAS, PERIODE, NO_KENDARAAN, PLAT_NOMER, TIPE, WARNA, TAHUN, POOL, 7 Foto Kendaraan, JENIS_SAMPLE, TANGGAL_SERVICE, PAKET_SERVICE, TOTAL_POIN_TEMUAN, serta 49 Item Temuan KPI (Point, Foto 1-3, Catatan 1-3).
""".trimIndent(), "Struktur_Sheets.txt")
                                }
                                copyToClipboard(codeToCopy, label)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Teal600),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salin Kode", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Fitur Edit In-Place Aktif: Saat mengedit audit atau master data, baris yang ada di Google Sheet akan diperbarui (update), tidak menambah baris baru lagi.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Teal600, fontWeight = FontWeight.Medium)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCodeTab == 0,
                        onClick = { selectedCodeTab = 0 },
                        label = { Text("Code.gs (Apps Script)") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Blue700, selectedLabelColor = Color.White)
                    )

                    FilterChip(
                        selected = selectedCodeTab == 1,
                        onClick = { selectedCodeTab = 1 },
                        label = { Text("index.html (Web Portal)") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Blue700, selectedLabelColor = Color.White)
                    )

                    FilterChip(
                        selected = selectedCodeTab == 2,
                        onClick = { selectedCodeTab = 2 },
                        label = { Text("Inisialisasi INPUT_AUDIT") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Blue700, selectedLabelColor = Color.White)
                    )

                    FilterChip(
                        selected = selectedCodeTab == 3,
                        onClick = { selectedCodeTab = 3 },
                        label = { Text("Inisialisasi GRADE_KPI") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Blue700, selectedLabelColor = Color.White)
                    )

                    FilterChip(
                        selected = selectedCodeTab == 4,
                        onClick = { selectedCodeTab = 4 },
                        label = { Text("Struktur 8 Sheet") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Blue700, selectedLabelColor = Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val displayedCode = remember(selectedCodeTab, sheetId, folderId) {
                    when (selectedCodeTab) {
                        0 -> GoogleAppsScriptHub.getGoogleScriptCode(sheetId, folderId)
                        1 -> GoogleAppsScriptHub.getIndexHtmlCode()
                        2 -> GoogleAppsScriptHub.getInputAuditInitScript(sheetId)
                        3 -> GoogleAppsScriptHub.getGradeKpiInitScript(sheetId)
                        else -> """
===========================================================
DATABASE GOOGLE SHEETS TERPUSAT (8 TAB / SHEETS LENGKAP)
===========================================================
Semua data aplikasi dapat langsung diedit di Google Sheets:

1. Tab "USER"
   -> NIP, NAMA_PETUGAS, USERNAMA, PASSWORD, LEVELAKSES, KODEPOOL, STATUS, TANGGAL_AKTIF
   -> Mengatur login admin dan auditor.

2. Tab "MASTER_POOL"
   -> ID, KODE_POOL, NAMA_POOL, ALAMAT_POOL, STATUS_AREA
   -> Status Area: JABODETABEK, JABODETABEK SATELIT, DAERAH, DAERAH SATELIT.

3. Tab "MASTER_KENDARAAN"
   -> ID, NO_MOBIL, PLAT_NOMER, TIPE, WARNA, POOL, TAHUN, TANGGAL_SERVICE, SERVICE
   -> Daftar kendaraan per pool armada.

4. Tab "MASTER_BOBOT"
   -> ITEM_ID, ITEM_NAME, SECTION_TITLE, GROUP_TITLE, POINT_MAX, BOBOT
   -> Menampung bobot KPI (Exterior, Interior, Cuci/Finishing).
   -> Bisa diedit langsung angkanya di Google Sheet!

5. Tab "MASTER_SAMPLE"
   -> STATUS_AREA, JUMLAH_SAMPLE, KETERANGAN
   -> Standar target kuota sample unit per area (20, 15, 10, 5).

6. Tab "MASTER_PERIODE"
   -> ID, PERIODE, KETERANGAN
   -> Daftar opsi periode audit (Periode 1, Periode 2, Mingguan, Bulanan, dll).

7. Tab "MASTER_GRADE_KPI"
   -> KATEGORI, MIN_SCORE, MAX_SCORE, KETERANGAN
   -> Konfigurasi Default:
      * Grade A: 70,00 - 80,00 (Sangat Baik)
      * Grade B: 60,00 - 69,99 (Baik)
      * Grade C: 50,00 - 59,99 (Cukup)
      * Grade D: 30,00 - 49,99 (Kurang)
      * Grade E: 0,00 - 29,99 (Kritis / Perlu Perbaikan)
   -> Dapat disetting manual langsung melalui Google Sheets atau melalui menu GRADE KPI di aplikasi.

8. Tab "INPUT_AUDIT"
   -> 366 Kolom Lengkap (A1 s/d NB1):
      - ID, TANGGAL, TIMESTAMP, NIP, NAMA_PETUGAS, PERIODE, NO_KENDARAAN, PLAT_NOMER, TIPE, WARNA, TAHUN, POOL
      - 7 Foto Kendaraan (Depan, Samping Kanan, Belakang, Samping Kiri, Interior Depan, Interior Belakang, Bagasi)
      - JENIS_SAMPLE, TANGGAL_SERVICE, PAKET_SERVICE, TOTAL_POIN_TEMUAN
      - 49 Item Temuan KPI (Point, Foto 1-3, Catatan 1-3 per item temuan)
   -> Rekap hasil audit lapangan beserta link foto Drive.

* Tips: Inisialisasi seluruh tabel Google Sheets dapat dijalankan sekaligus melalui tombol "Inisialisasi Semua Tabel" di atas.
""".trimIndent()
                    }
                }

                Surface(
                    color = Slate100,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = displayedCode,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Slate800,
                        modifier = Modifier
                            .padding(12.dp)
                            .height(260.dp)
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}
