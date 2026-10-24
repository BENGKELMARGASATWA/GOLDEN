package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.MasterPeriodeEntity
import com.example.data.local.entity.MasterPoolEntity
import com.example.data.local.entity.PengaturanNamaPhotoEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.KpiAuditSpec
import com.example.data.model.KpiGroup
import com.example.data.model.KpiItem
import com.example.data.model.KpiSection
import com.example.ui.components.PhotoMarkupEditorDialog
import com.example.ui.theme.Amber50
import com.example.ui.theme.Amber600
import com.example.ui.theme.Blue100
import com.example.ui.theme.Blue200
import com.example.ui.theme.Blue600
import com.example.ui.theme.Blue700
import com.example.ui.theme.Blue800
import com.example.ui.theme.Blue900
import com.example.ui.theme.HighDensityBg
import com.example.ui.theme.HighDensityBlue
import com.example.ui.theme.HighDensityBlueBorder
import com.example.ui.theme.HighDensityBlueLight
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
import com.example.ui.theme.Teal50
import com.example.ui.theme.Teal200
import com.example.ui.theme.Teal600
import com.example.ui.theme.Teal700
import com.example.ui.viewmodel.AuditFormState
import com.example.util.DateUtils
import com.example.util.ImageCompressor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditInputScreen(
    formState: AuditFormState,
    currentUser: UserEntity?,
    photoTitles: List<PengaturanNamaPhotoEntity>,
    totalAuditsCount: Int,
    periodeList: List<MasterPeriodeEntity> = emptyList(),
    pools: List<MasterPoolEntity> = emptyList(),
    onNoMobilChanged: (String) -> Unit,
    onPeriodeChanged: (String) -> Unit = {},
    onPlatNomerChanged: (String) -> Unit = {},
    onTipeChanged: (String) -> Unit = {},
    onWarnaChanged: (String) -> Unit = {},
    onTahunChanged: (String) -> Unit = {},
    onTanggalServiceChanged: (String) -> Unit = {},
    onServiceChanged: (String) -> Unit = {},
    onPoolChanged: (String) -> Unit = {},
    onJenisSampleChanged: (String) -> Unit = {},
    onSampleTanggalChanged: (String) -> Unit = {},
    onPaketServiceChanged: (String) -> Unit = {},
    onKpiScoreChanged: (String, Int) -> Unit = { _, _ -> },
    onKpiPhotoCaptured: (String, String) -> Unit = { _, _ -> },
    onKpiRemovePhoto: (String) -> Unit = {},
    onKpiNoteChanged: (String, String) -> Unit = { _, _ -> },
    onResetAllNormal: () -> Unit = {},
    onPhotoCaptured: (Int, String) -> Unit = { _, _ -> },
    onRemovePhoto: (Int) -> Unit = {},
    onNoteChanged: (Int, String) -> Unit = { _, _ -> },
    onCancelEdit: () -> Unit = {},
    onRegenerateId: () -> Unit = {},
    onSubmit: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeCapturingKey by rememberSaveable { mutableStateOf<String?>(null) }
    var activeCapturingLabel by rememberSaveable { mutableStateOf<String?>(null) }
    var activeCapturingItemTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var tempCameraFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var showPhotoPickerSheet by remember { mutableStateOf(false) }
    var activeMarkupTarget by remember { mutableStateOf<Triple<String, String, String>?>(null) } // key, path, label
    var previewPhotoTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // path, label
    val sheetState = rememberModalBottomSheetState()

    var selectedSectionFilter by remember(formState.initialTimestamp, formState.isEditMode) { mutableStateOf("ALL") } // "ALL", "EXTERIOR", "INTERIOR", "CUCIAN"
    var itemSearchQuery by rememberSaveable { mutableStateOf("") }

    // Waktu dan timestamp: Auto-update mengikuti jam HP saat input baru, atau mengikuti data tersimpan saat mode edit
    var currentDeviceTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(formState.isEditMode) {
        if (!formState.isEditMode) {
            while (isActive) {
                currentDeviceTimeMillis = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    val displayDateText = if (formState.isEditMode) {
        val raw = formState.originalTanggal?.ifBlank { null }
        if (!raw.isNullOrBlank()) {
            val formatted = DateUtils.formatDisplayDate(raw)
            if (formatted != "-") formatted else raw
        } else {
            val ts = formState.editingAuditTimestamp ?: formState.initialTimestamp
            DateUtils.formatDate(ts)
        }
    } else {
        DateUtils.formatDate(currentDeviceTimeMillis)
    }

    val displayTimeText = if (formState.isEditMode) {
        val ts = formState.editingAuditTimestamp ?: formState.initialTimestamp
        if (ts > 0L) {
            DateUtils.formatTimestampWithSeconds(ts)
        } else if (!formState.originalTanggal.isNullOrBlank() && formState.originalTanggal.contains(":")) {
            formState.originalTanggal
        } else {
            DateUtils.formatTimestampWithSeconds(formState.initialTimestamp)
        }
    } else {
        DateUtils.formatTimestampWithSeconds(currentDeviceTimeMillis)
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val key = activeCapturingKey
        val file = tempCameraFilePath?.let { File(it) }
        val itemTitle = activeCapturingItemTitle.orEmpty().trim()
        val photoLabel = activeCapturingLabel.orEmpty().trim()
        val photoNote = (key?.let { formState.kpiNotes[it] }).orEmpty().trim()
        val capturedVehicle = formState.noMobil.ifBlank { formState.platNomer }.trim().ifBlank { "UNIT MOBIL" }
        val capturedAuditorNip = if (formState.isEditMode) {
            formState.originalAuditorNip?.ifBlank { null } ?: currentUser?.nip.orEmpty().trim()
        } else {
            currentUser?.nip.orEmpty().trim()
        }
        val capturedAuditorName = if (formState.isEditMode) {
            (formState.originalAuditorName?.ifBlank { null } ?: currentUser?.namaPetugas?.ifBlank { currentUser.username }).orEmpty().trim()
        } else {
            (currentUser?.namaPetugas?.ifBlank { currentUser.username }).orEmpty().trim()
        }
        val capturedAuditor = when {
            capturedAuditorNip.isNotBlank() && capturedAuditorName.isNotBlank() ->
                "$capturedAuditorNip/${capturedAuditorName.uppercase()}"
            capturedAuditorNip.isNotBlank() -> capturedAuditorNip
            capturedAuditorName.isNotBlank() -> capturedAuditorName.uppercase()
            else -> "AUDITOR"
        }
        val capturedTime = if (formState.isEditMode) {
            val ts = formState.editingAuditTimestamp ?: formState.initialTimestamp
            if (ts > 0L) DateUtils.formatTimestampWithSeconds(ts) else DateUtils.formatTimestampWithSeconds(System.currentTimeMillis())
        } else {
            DateUtils.formatTimestampWithSeconds(System.currentTimeMillis())
        }

        activeCapturingKey = null
        activeCapturingLabel = null
        activeCapturingItemTitle = null
        tempCameraFilePath = null

        if (success && key != null && file != null && file.exists() && file.length() > 0) {
            coroutineScope.launch {
                try {
                    // Gabungkan data dari judul input dan keterangan dari photo
                    val watermarkKeterangan = buildString {
                        if (itemTitle.isNotBlank()) {
                            append(itemTitle)
                        }
                        if (photoLabel.isNotBlank()) {
                            if (isNotEmpty()) append(" - ")
                            append(photoLabel)
                        }
                        if (photoNote.isNotBlank()) {
                            if (isNotEmpty()) append(" ($photoNote)")
                        }
                    }.ifBlank { "Temuan Audit" }

                    val compressedPath = ImageCompressor.compressAndSaveImage(
                        context = context,
                        sourceFile = file,
                        auditPrefix = "audit_${capturedVehicle}_$key",
                        watermarkText = capturedVehicle,
                        auditorName = capturedAuditor,
                        auditorNip = capturedAuditorNip,
                        watermarkTimestamp = capturedTime,
                        keterangan = watermarkKeterangan,
                        sample = formState.jenisSample.ifBlank { "OPERASIONAL" },
                        sampleTanggal = formState.sampleTanggal.ifBlank { null },
                        paketService = formState.paketService.ifBlank { null }
                    )
                    onKpiPhotoCaptured(key, compressedPath)
                    Toast.makeText(context, "Foto berhasil disimpan!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal memproses foto: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val key = activeCapturingKey
        val itemTitle = activeCapturingItemTitle.orEmpty().trim()
        val photoLabel = activeCapturingLabel.orEmpty().trim()
        val photoNote = (key?.let { formState.kpiNotes[it] }).orEmpty().trim()
        val capturedVehicle = formState.noMobil.ifBlank { formState.platNomer }.trim().ifBlank { "UNIT MOBIL" }
        val capturedAuditorNip = if (formState.isEditMode) {
            formState.originalAuditorNip?.ifBlank { null } ?: currentUser?.nip.orEmpty().trim()
        } else {
            currentUser?.nip.orEmpty().trim()
        }
        val capturedAuditorName = if (formState.isEditMode) {
            (formState.originalAuditorName?.ifBlank { null } ?: currentUser?.namaPetugas?.ifBlank { currentUser.username }).orEmpty().trim()
        } else {
            (currentUser?.namaPetugas?.ifBlank { currentUser.username }).orEmpty().trim()
        }
        val capturedAuditor = when {
            capturedAuditorNip.isNotBlank() && capturedAuditorName.isNotBlank() ->
                "$capturedAuditorNip/${capturedAuditorName.uppercase()}"
            capturedAuditorNip.isNotBlank() -> capturedAuditorNip
            capturedAuditorName.isNotBlank() -> capturedAuditorName.uppercase()
            else -> "AUDITOR"
        }
        val capturedTime = DateUtils.formatTimestampWithSeconds(System.currentTimeMillis())

        activeCapturingKey = null
        activeCapturingLabel = null
        activeCapturingItemTitle = null

        if (uri != null && key != null) {
            coroutineScope.launch {
                try {
                    val input = context.contentResolver.openInputStream(uri)
                    val cacheFile = File(context.cacheDir, "gallery_import_${System.currentTimeMillis()}.jpg")
                    input?.use { inStream ->
                        cacheFile.outputStream().use { outStream ->
                            inStream.copyTo(outStream)
                        }
                    }

                    // Gabungkan data dari judul input dan keterangan dari photo
                    val watermarkKeterangan = buildString {
                        if (itemTitle.isNotBlank()) {
                            append(itemTitle)
                        }
                        if (photoLabel.isNotBlank()) {
                            if (isNotEmpty()) append(" - ")
                            append(photoLabel)
                        }
                        if (photoNote.isNotBlank()) {
                            if (isNotEmpty()) append(" ($photoNote)")
                        }
                    }.ifBlank { "Temuan Audit" }

                    val compressedPath = ImageCompressor.compressAndSaveImage(
                        context = context,
                        sourceFile = cacheFile,
                        auditPrefix = "audit_${capturedVehicle}_$key",
                        watermarkText = capturedVehicle,
                        auditorName = capturedAuditor,
                        auditorNip = capturedAuditorNip,
                        watermarkTimestamp = capturedTime,
                        keterangan = watermarkKeterangan,
                        sample = formState.jenisSample.ifBlank { "OPERASIONAL" },
                        sampleTanggal = formState.sampleTanggal.ifBlank { null },
                        paketService = formState.paketService.ifBlank { null }
                    )
                    onKpiPhotoCaptured(key, compressedPath)
                    Toast.makeText(context, "Foto galeri berhasil disimpan!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal memproses gambar galeri: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uriFilePair = ImageCompressor.createTempImageUri(context)
            if (uriFilePair != null) {
                tempCameraFilePath = uriFilePair.second.absolutePath
                cameraLauncher.launch(uriFilePair.first)
            } else {
                Toast.makeText(context, "Gagal membuat berkas sementara untuk kamera", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Izin kamera diperlukan untuk mengambil foto", Toast.LENGTH_SHORT).show()
        }
    }

    fun startCapture(key: String, label: String, itemTitle: String) {
        activeCapturingKey = key
        activeCapturingLabel = label
        activeCapturingItemTitle = itemTitle
        showPhotoPickerSheet = true
    }

    fun startCaptureDirectCamera(key: String, label: String, itemTitle: String) {
        activeCapturingKey = key
        activeCapturingLabel = label
        activeCapturingItemTitle = itemTitle
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            val uriFilePair = ImageCompressor.createTempImageUri(context)
            if (uriFilePair != null) {
                tempCameraFilePath = uriFilePair.second.absolutePath
                cameraLauncher.launch(uriFilePair.first)
            } else {
                Toast.makeText(context, "Gagal membuat berkas sementara kamera", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(permission)
        }
    }

    val findingCount = remember(formState.kpiScores) {
        formState.kpiScores.values.count { it > 0 }
    }

    val filteredSections = remember(selectedSectionFilter, itemSearchQuery, formState.kpiScores, formState.jenisSample) {
        val query = itemSearchQuery.trim().lowercase()
        val isOperasional = formState.jenisSample.equals("OPERASIONAL", ignoreCase = true)
        val baseSections = when (selectedSectionFilter) {
            "EXTERIOR" -> KpiAuditSpec.sections.filter { it.id == "EXTERIOR" }
            "INTERIOR" -> KpiAuditSpec.sections.filter { it.id == "INTERIOR" }
            "CUCIAN" -> KpiAuditSpec.sections.filter { it.id == "CUCIAN" }
            else -> KpiAuditSpec.sections
        }

        baseSections.mapNotNull { section ->
            val matchingGroups = section.groups.filterNot { group ->
                isOperasional && (
                    group.id == "INT_EXS_SERVICE" ||
                    group.id == "CUC_LENGKAP" ||
                    group.title.contains("EXS SERVICE", ignoreCase = true) ||
                    group.title.contains("CUCI LENGKAP", ignoreCase = true)
                )
            }.mapNotNull { group ->
                val matchingItems = group.items.filter { item ->
                    if (query.isNotEmpty()) {
                        val matchesName = item.name.lowercase().contains(query)
                        val matchesGroup = group.title.lowercase().contains(query)
                        val matchesSection = section.title.lowercase().contains(query)
                        val matchesId = item.id.lowercase().contains(query)
                        val matchesLabels = item.photoLabels.any { it.lowercase().contains(query) }
                        matchesName || matchesGroup || matchesSection || matchesId || matchesLabels
                    } else {
                        true
                    }
                }

                if (matchingItems.isNotEmpty()) {
                    group to matchingItems
                } else {
                    null
                }
            }

            if (matchingGroups.isNotEmpty()) {
                section to matchingGroups
            } else {
                null
            }
        }
    }

    val totalMatchingItems = remember(filteredSections) {
        filteredSections.sumOf { (_, groups) -> groups.sumOf { (_, items) -> items.size } }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(HighDensityBg)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Spacer for top insets
        item { Spacer(modifier = Modifier.height(6.dp)) }

        // 1. HEADER CARD: FORM AUDIT HARIAN
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_form_audit_harian"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Slate200)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = HighDensityBlue,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FORM AUDIT HARIAN",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Slate800
                            )
                            Text(
                                text = if (formState.isEditMode) "MODE EDIT DATA AUDIT" else "Pemeriksaan Kualitas & Kondisi Armada",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (formState.isEditMode) Rose600 else Slate500,
                                fontWeight = if (formState.isEditMode) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Slate200)

                    // Susunan FORM AUDIT HARIAN:
                    // 1. ID AUDIT / KODE UNIK (Otomatis & Unik untuk Google Sheets)
                    AutoInfoField(
                        label = "ID",
                        value = formState.kodeUnik.ifBlank { "AUD-GENERATING" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_id"),
                        trailingAction = if (!formState.isEditMode) {
                            {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("ID Audit", formState.kodeUnik))
                                            Toast.makeText(context, "ID ${formState.kodeUnik} disalin", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Salin ID",
                                            tint = Slate500,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = onRegenerateId,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = "Regenerate ID",
                                            tint = HighDensityBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        } else null
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. TANGGAL
                    AutoInfoField(
                        label = "TANGGAL",
                        value = displayDateText,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. WAKTU/TIMESTAMP
                    AutoInfoField(
                        label = "WAKTU/TIMESTAMP",
                        value = displayTimeText,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. NIP AUDITOR
                    val nipUser = if (formState.isEditMode) {
                        formState.originalAuditorNip?.ifBlank { null } ?: "-"
                    } else {
                        currentUser?.nip ?: "-"
                    }
                    val namaUser = if (formState.isEditMode) {
                        formState.originalAuditorName?.ifBlank { null } ?: "-"
                    } else {
                        currentUser?.namaPetugas?.ifBlank { currentUser.username } ?: "-"
                    }
                    AutoInfoField(
                        label = "NIP AUDITOR",
                        value = nipUser,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4. NAMA AUDITOR
                    AutoInfoField(
                        label = "NAMA AUDITOR",
                        value = namaUser,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 5. PERIODE (Model Dropdown - Tidak dapat di-input text)
                    var showPeriodeDropdown by remember { mutableStateOf(false) }
                    val availablePeriodes = remember(periodeList) {
                        if (periodeList.isNotEmpty()) {
                            periodeList.map { it.periode }
                        } else {
                            listOf("Periode 1", "Periode 2", "Periode 3", "Periode 4", "Mingguan", "Bulanan")
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = formState.periode,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("PERIODE (Wajib)") },
                            placeholder = { Text("-- Pilih Periode Audit --") },
                            trailingIcon = {
                                IconButton(onClick = { showPeriodeDropdown = !showPeriodeDropdown }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Pilih Periode")
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HighDensityBlue,
                                unfocusedBorderColor = if (formState.periode.isBlank()) Amber600 else Slate300
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_periode")
                        )
                        // Invisible click overlay so entire field opens dropdown
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showPeriodeDropdown = true }
                        )
                        DropdownMenu(
                            expanded = showPeriodeDropdown,
                            onDismissRequest = { showPeriodeDropdown = false }
                        ) {
                            availablePeriodes.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p) },
                                    onClick = {
                                        onPeriodeChanged(p)
                                        showPeriodeDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. CARD DATA KENDARAAN & DETAIL KENDARAAN
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_data_kendaraan"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Slate200)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "DATA KENDARAAN",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Slate800,
                            modifier = Modifier.weight(1f)
                        )
                        if (formState.isEditMode) {
                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Color(0xFF1D4ED8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Mode Edit",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF1D4ED8),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else if (formState.isVehicleFound) {
                            Surface(
                                color = Teal50,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Teal200)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Teal700,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Armada Terverifikasi",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Teal700,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else if (formState.validationError != null) {
                            Surface(
                                color = Color(0xFFFFF1F2),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFFECDD3))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = null,
                                        tint = Rose600,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "TIDAK TERDAFTAR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Rose600,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input NO MOBIL
                    OutlinedTextField(
                        value = formState.noMobil,
                        onValueChange = {
                            if (!formState.isEditMode) {
                                onNoMobilChanged(it.uppercase())
                            }
                        },
                        readOnly = formState.isEditMode,
                        label = {
                            Text(if (formState.isEditMode) "NO MOBIL" else "NO MOBIL (Wajib)")
                        },
                        placeholder = { Text("Masukkan nomor mobil...") },
                        leadingIcon = {
                            Icon(
                                if (formState.isEditMode) Icons.Default.Lock else Icons.Default.DirectionsCar,
                                contentDescription = if (formState.isEditMode) "Terkunci" else "No Mobil",
                                tint = if (formState.isEditMode) Slate600 else HighDensityBlue
                            )
                        },
                        trailingIcon = {
                            if (formState.isEditMode) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Terkunci",
                                    tint = Slate400,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else if (formState.noMobil.isNotBlank()) {
                                IconButton(onClick = { onNoMobilChanged("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        isError = !formState.isEditMode && formState.validationError != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (formState.isEditMode) Slate400 else if (formState.validationError != null) Rose600 else HighDensityBlue,
                            unfocusedBorderColor = if (formState.isEditMode) Slate300 else if (formState.validationError != null) Rose600 else if (formState.noMobil.isBlank()) Amber600 else Slate300,
                            focusedContainerColor = if (formState.isEditMode) Slate50 else Color.Transparent,
                            unfocusedContainerColor = if (formState.isEditMode) Slate50 else Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_no_mobil")
                    )

                    // Peringatan jika No Mobil Tidak Ditemukan atau ditolak pool (hanya saat form baru, bukan mode edit)
                    if (!formState.isEditMode && formState.validationError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFFFFF1F2),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("warning_no_mobil_not_found")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Rose600, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = formState.validationError,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Rose600
                                )
                            }
                        }
                    }

                    // Detail kendaraan: plat mengikuti no mobil, tipe/warna/tahun input manual, pool dari master pool
                    if (formState.noMobil.isNotBlank() || formState.isVehicleFound || formState.isEditMode) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "DETAIL KENDARAAN",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = HighDensityBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Baris 1: Plat Nomer (Otomatis mengikuti No Mobil) & Tipe (Input Manual)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = formState.platNomer.ifBlank { formState.noMobil.ifBlank { "-" } },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("PLAT NOMER") },
                                supportingText = { Text("Mengikuti No Mobil", fontSize = 10.sp, color = Slate500) },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Slate50,
                                    unfocusedContainerColor = Slate50,
                                    focusedBorderColor = Slate300,
                                    unfocusedBorderColor = Slate200
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("info_plat_nomer")
                            )
                            OutlinedTextField(
                                value = formState.tipe,
                                onValueChange = { onTipeChanged(it.uppercase()) },
                                label = { Text("TIPE") },
                                placeholder = { Text("Input manual") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_tipe")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Baris 2: Warna (Input Manual) & Tahun (Input Manual)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = formState.warna,
                                onValueChange = { onWarnaChanged(it.uppercase()) },
                                label = { Text("WARNA") },
                                placeholder = { Text("Input manual") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_warna")
                            )
                            OutlinedTextField(
                                value = formState.tahun,
                                onValueChange = onTahunChanged,
                                label = { Text("TAHUN") },
                                placeholder = { Text("Contoh: 2023") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_tahun")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Baris 3: Pool (Diambil dari kode pool master pool)
                        var poolDropdownExpanded by remember { mutableStateOf(false) }
                        val availablePoolCodes = remember(pools, currentUser) {
                            val fromMaster = pools.map { it.kodePool.trim().uppercase() }.filter { it.isNotBlank() }.distinct()
                            if (fromMaster.isNotEmpty()) {
                                fromMaster
                            } else {
                                val userPool = currentUser?.kodePool?.trim()?.uppercase().orEmpty()
                                if (userPool.isNotBlank()) listOf(userPool) else listOf("SKT")
                            }
                        }
                        val currentPoolValue = formState.pool.ifBlank {
                            currentUser?.kodePool?.trim()?.uppercase()?.takeIf { it.isNotBlank() } ?: availablePoolCodes.firstOrNull() ?: "SKT"
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = currentPoolValue,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("POOL (Master Pool)") },
                                trailingIcon = {
                                    IconButton(onClick = { poolDropdownExpanded = !poolDropdownExpanded }) {
                                        Icon(
                                            imageVector = if (poolDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = "Pilih Kode Pool",
                                            tint = Blue700
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("select_pool")
                                    .clickable { poolDropdownExpanded = true }
                            )

                            DropdownMenu(
                                expanded = poolDropdownExpanded,
                                onDismissRequest = { poolDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                availablePoolCodes.forEach { pCode ->
                                    val poolDetail = pools.firstOrNull { it.kodePool.trim().equals(pCode, ignoreCase = true) }
                                    val isSelected = currentPoolValue.equals(pCode, ignoreCase = true)
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = pCode,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = if (isSelected) Blue700 else Slate800
                                                )
                                                if (poolDetail != null && poolDetail.namaPool.isNotBlank()) {
                                                    Text(
                                                        text = poolDetail.namaPool,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                        color = Slate500
                                                    )
                                                }
                                            }
                                        },
                                        trailingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, tint = Blue700) }
                                        } else null,
                                        onClick = {
                                            onPoolChanged(pCode)
                                            poolDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = Slate100,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ketik No Mobil di atas untuk melengkapi detail plat, tipe, warna, tahun, dan pool.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Slate600
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. CARD SAMPLE (Setelah DATA KENDARAAN dan sebelum FOTO KENDARAAN)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_sample"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Slate200)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = HighDensityBlue,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SAMPLE",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Slate800
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Slate200)

                    // 1. JENIS SAMPLE (OPERASIONAL, SERVICE Dropdown) - Wajib diisi
                    var showJenisDropdown by remember { mutableStateOf(false) }
                    val jenisSampleOptions = listOf("OPERASIONAL", "SERVICE")

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = formState.jenisSample,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("JENIS SAMPLE (Wajib)") },
                            placeholder = { Text("Pilih Jenis Sample...") },
                            trailingIcon = {
                                IconButton(onClick = { showJenisDropdown = !showJenisDropdown }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Pilih Jenis Sample")
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HighDensityBlue,
                                unfocusedBorderColor = if (formState.jenisSample.isBlank()) Amber600 else Slate300
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_jenis_sample")
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showJenisDropdown = !showJenisDropdown }
                        )
                        DropdownMenu(
                            expanded = showJenisDropdown,
                            onDismissRequest = { showJenisDropdown = false }
                        ) {
                            jenisSampleOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            opt,
                                            fontWeight = if (opt == formState.jenisSample) FontWeight.Bold else FontWeight.Normal,
                                            color = if (opt == formState.jenisSample) HighDensityBlue else Slate800
                                        )
                                    },
                                    onClick = {
                                        onJenisSampleChanged(opt)
                                        showJenisDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Conditional fields for SERVICE
                    if (formState.jenisSample.equals("SERVICE", ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. TANGGAL SERVICE (Wajib) (jika di klik akan muncul kalender)
                        val calendar = remember { java.util.Calendar.getInstance() }
                        val datePickerDialog = remember {
                            android.app.DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val formatted = String.format(java.util.Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y)
                                    onSampleTanggalChanged(formatted)
                                },
                                calendar.get(java.util.Calendar.YEAR),
                                calendar.get(java.util.Calendar.MONTH),
                                calendar.get(java.util.Calendar.DAY_OF_MONTH)
                            )
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = formState.sampleTanggal,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("TANGGAL SERVICE (Wajib)") },
                                placeholder = { Text("Pilih Tanggal Service...") },
                                trailingIcon = {
                                    IconButton(onClick = { datePickerDialog.show() }) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = "Buka Kalender", tint = HighDensityBlue)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = HighDensityBlue,
                                    unfocusedBorderColor = if (formState.sampleTanggal.isBlank()) Amber600 else Slate300
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_sample_tanggal")
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { datePickerDialog.show() }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3. PAKET SERVICE (Wajib) (teks otomatis huruf kapital)
                        OutlinedTextField(
                            value = formState.paketService,
                            onValueChange = { onPaketServiceChanged(it.uppercase()) },
                            label = { Text("PAKET SERVICE (Wajib)") },
                            placeholder = { Text("Ketik Paket Service...") },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HighDensityBlue,
                                unfocusedBorderColor = if (formState.paketService.isBlank()) Amber600 else Slate300
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_paket_service")
                        )
                    }
                }
            }
        }

        // 4. CARD FOTO KENDARAAN (Setelah SAMPLE)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_foto_kendaraan"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Slate200)
            ) {
                val vehicleSlots = listOf(
                    Triple("kendaraan_depan", "DEPAN", "Foto Sisi Depan Kendaraan"),
                    Triple("kendaraan_samping_kiri", "SAMPING KIRI", "Foto Sisi Samping Kiri Kendaraan"),
                    Triple("kendaraan_interior_depan_kiri", "INTERIOR DEPAN KIRI", "Foto Interior Depan Kiri"),
                    Triple("kendaraan_interior_belakang_kiri", "INTERIOR BELAKANG KIRI", "Foto Interior Belakang Kiri"),
                    Triple("kendaraan_belakang", "BELAKANG", "Foto Sisi Belakang Kendaraan"),
                    Triple("kendaraan_interior_belakang", "INTERIOR BELAKANG", "Foto Interior Belakang"),
                    Triple("kendaraan_samping_kanan", "SAMPING KANAN", "Foto Sisi Samping Kanan Kendaraan")
                )
                val filledVehicleCount = vehicleSlots.count { formState.kpiPhotos[it.first]?.isNotBlank() == true }

                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = HighDensityBlue,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "FOTO KENDARAAN",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Slate800
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = if (filledVehicleCount == vehicleSlots.size) Color(0xFFDCFCE7) else Color(0xFFFFF1F2),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(0.5.dp, if (filledVehicleCount == vehicleSlots.size) Color(0xFF86EFAC) else Color(0xFFFECDD3))
                                ) {
                                    Text(
                                        text = if (filledVehicleCount == vehicleSlots.size) "✓ LENGKAP" else "* WAJIB DIISI",
                                        color = if (filledVehicleCount == vehicleSlots.size) Color(0xFF15803D) else Rose600,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Progres: $filledVehicleCount dari ${vehicleSlots.size} foto terisi",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = if (filledVehicleCount == vehicleSlots.size) Color(0xFF15803D) else Slate600
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Slate200)

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        vehicleSlots.forEach { (vKey, vLabel, vDesc) ->
                            val photoPath = formState.kpiPhotos[vKey].orEmpty()
                            VehiclePhotoSlotRow(
                                photoKey = vKey,
                                label = vLabel,
                                description = vDesc,
                                photoPath = photoPath,
                                onCaptureCamera = {
                                    startCaptureDirectCamera(vKey, vLabel, "FOTO KENDARAAN")
                                },
                                onRemove = {
                                    onKpiRemovePhoto(vKey)
                                },
                                onPreview = {
                                    if (photoPath.isNotBlank()) {
                                        previewPhotoTarget = Pair(photoPath, "FOTO KENDARAAN - $vLabel")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. KPI AUDIT SUMMARY & SECTION FILTER
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Slate200)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "RINGKASAN HASIL TEMUAN",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Slate500
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${formState.totalPoinTemuan}",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (formState.totalPoinTemuan > 0) HighDensityBlue else Teal700
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Total Poin Temuan",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Slate600
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                color = if (formState.totalPoinTemuan > 0) HighDensityBlueLight else Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (formState.totalPoinTemuan > 0) Blue200 else Color(0xFF22C55E))
                            ) {
                                Text(
                                    text = if (formState.totalPoinTemuan > 0) "$findingCount Item Temuan (${formState.totalPoinTemuan} Poin)" else "Semua Item 0 (Normal)",
                                    color = if (formState.totalPoinTemuan > 0) HighDensityBlue else Color(0xFF15803D),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Kotak Input Pencarian Temuan
                    OutlinedTextField(
                        value = itemSearchQuery,
                        onValueChange = { itemSearchQuery = it },
                        placeholder = {
                            Text("Cari item temuan (contoh: baret, kaca, jok, body)...", fontSize = 12.sp, color = Slate400)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Cari Temuan", tint = HighDensityBlue, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (itemSearchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { itemSearchQuery = "" },
                                    modifier = Modifier.testTag("button_clear_search_temuan")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Hapus Pencarian", tint = Slate400, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedBorderColor = HighDensityBlue,
                            unfocusedBorderColor = Slate300
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_search_item_temuan")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedSectionFilter == "ALL",
                            onClick = { selectedSectionFilter = "ALL" },
                            label = { Text("ALL", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HighDensityBlue,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("filter_chip_all")
                        )
                        FilterChip(
                            selected = selectedSectionFilter == "EXTERIOR",
                            onClick = { selectedSectionFilter = "EXTERIOR" },
                            label = { Text("A. Exterior") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HighDensityBlue,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("filter_chip_exterior")
                        )
                        FilterChip(
                            selected = selectedSectionFilter == "INTERIOR",
                            onClick = { selectedSectionFilter = "INTERIOR" },
                            label = { Text("B. Interior") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HighDensityBlue,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("filter_chip_interior")
                        )
                        FilterChip(
                            selected = selectedSectionFilter == "CUCIAN",
                            onClick = { selectedSectionFilter = "CUCIAN" },
                            label = { Text("C. Cucian") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HighDensityBlue,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("filter_chip_cucian")
                        )
                    }

                    // Status Hasil Pencarian / Filter jika aktif
                    if (itemSearchQuery.isNotBlank() || selectedSectionFilter != "ALL") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Menampilkan $totalMatchingItems item temuan",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = HighDensityBlue,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = "Reset Filter & Cari",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFE11D48),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier
                                    .clickable {
                                        itemSearchQuery = ""
                                        selectedSectionFilter = "ALL"
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. SECTIONS & GROUPS & ITEMS
        if (filteredSections.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (itemSearchQuery.isNotBlank()) {
                                "Tidak ada item temuan yang cocok dengan \"$itemSearchQuery\""
                            } else {
                                "Tidak ada item temuan pada kategori ini"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                itemSearchQuery = ""
                                selectedSectionFilter = "ALL"
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = HighDensityBlue),
                            border = BorderStroke(1.dp, HighDensityBlue)
                        ) {
                            Text("Tampilkan Semua Item Temuan", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        } else {
            filteredSections.forEach { (section, matchingGroups) ->
                item {
                    SectionHeaderBanner(section = section)
                }

                matchingGroups.forEach { (group, matchingItems) ->
                    item {
                        GroupHeaderCard(group = group)
                    }

                    matchingItems.forEach { item ->
                        item {
                            KpiItemCard(
                                item = item,
                                score = formState.kpiScores[item.id],
                                kpiPhotos = formState.kpiPhotos,
                                kpiNotes = formState.kpiNotes,
                                onScoreChanged = { newScore -> onKpiScoreChanged(item.id, newScore) },
                                onStartCapture = { key, label, itemTitle -> startCapture(key, label, itemTitle) },
                                onRemovePhoto = { key -> onKpiRemovePhoto(key) },
                                onNoteChanged = { key, note -> onKpiNoteChanged(key, note) },
                                onOpenMarkup = { key, path, label ->
                                    activeMarkupTarget = Triple(key, path, label)
                                },
                                onPreviewPhoto = { path, label ->
                                    previewPhotoTarget = Pair(path, label)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 5. SUBMIT & ACTION BUTTONS
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Slate200)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isOperasional = formState.jenisSample.equals("OPERASIONAL", ignoreCase = true)
                    val allItemsList = remember(isOperasional) {
                        if (isOperasional) {
                            KpiAuditSpec.sections.flatMap { it.groups }
                                .filterNot {
                                    it.id == "INT_EXS_SERVICE" ||
                                    it.id == "CUC_LENGKAP" ||
                                    it.title.contains("EXS SERVICE", ignoreCase = true) ||
                                    it.title.contains("CUCI LENGKAP", ignoreCase = true)
                                }
                                .flatMap { it.items }
                        } else {
                            KpiAuditSpec.getAllItems()
                        }
                    }
                    val unratedCount = remember(formState.kpiScores, allItemsList) {
                        allItemsList.count { !formState.kpiScores.containsKey(it.id) }
                    }
                    val areAllPointsFilled = unratedCount == 0

                    val requiredVehicleSlots = remember {
                        listOf(
                            "kendaraan_depan" to "DEPAN",
                            "kendaraan_samping_kiri" to "SAMPING KIRI",
                            "kendaraan_interior_depan_kiri" to "INTERIOR DEPAN KIRI",
                            "kendaraan_interior_belakang_kiri" to "INTERIOR BELAKANG KIRI",
                            "kendaraan_belakang" to "BELAKANG",
                            "kendaraan_interior_belakang" to "INTERIOR BELAKANG",
                            "kendaraan_samping_kanan" to "SAMPING KANAN"
                        )
                    }
                    val missingVehicleSlots = remember(formState.kpiPhotos) {
                        requiredVehicleSlots.filter { formState.kpiPhotos[it.first].isNullOrBlank() }
                    }
                    val areVehiclePhotosComplete = missingVehicleSlots.isEmpty()

                    val isService = formState.jenisSample.equals("SERVICE", ignoreCase = true)
                    val isServiceComplete = !isService || (formState.sampleTanggal.isNotBlank() && formState.paketService.isNotBlank())
                    val isVehicleValid = formState.noMobil.isNotBlank()
                    val canSubmit = isVehicleValid &&
                        formState.periode.isNotBlank() &&
                        formState.jenisSample.isNotBlank() &&
                        isServiceComplete &&
                        areAllPointsFilled &&
                        areVehiclePhotosComplete

                    if (!canSubmit) {
                        val missing = buildList {
                            if (formState.noMobil.isBlank()) {
                                add("No Mobil")
                            }
                            if (formState.periode.isBlank()) add("Periode")
                            if (formState.jenisSample.isBlank()) add("Jenis Sample")
                            if (isService) {
                                if (formState.sampleTanggal.isBlank()) add("Tanggal Service")
                                if (formState.paketService.isBlank()) add("Paket Service")
                            }
                            if (!areVehiclePhotosComplete) {
                                add("Foto Kendaraan (${7 - missingVehicleSlots.size}/7 terisi, kurang: ${missingVehicleSlots.joinToString { it.second }})")
                            }
                            if (!areAllPointsFilled) add("$unratedCount poin penilaian belum dipilih (*)")
                        }.joinToString(", ")
                        Text(
                            text = if (!formState.isEditMode && formState.validationError != null) "* ${formState.validationError}" else "* Harap lengkapi $missing untuk menyimpan audit",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = if (!formState.isEditMode && (formState.validationError != null || (!formState.isVehicleFound && formState.noMobil.isNotBlank()))) Rose600 else Amber600,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Button(
                        onClick = onSubmit,
                        enabled = !formState.isSubmitting && canSubmit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HighDensityBlue,
                            disabledContainerColor = Slate300
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_submit_audit")
                    ) {
                        if (formState.isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Menyimpan & Mengunggah Data...", style = MaterialTheme.typography.titleSmall)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (formState.isEditMode) "PERBARUI HASIL AUDIT" else "SIMPAN & KIRIM AUDIT",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    if (formState.isEditMode) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onCancelEdit,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose600),
                            border = BorderStroke(1.dp, Rose600),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_cancel_edit_audit")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Batal Edit & Kembali", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    // Modal Bottom Sheet for Photo Capture (Camera vs Gallery)
    if (showPhotoPickerSheet && activeCapturingKey != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showPhotoPickerSheet = false
                activeCapturingKey = null
                activeCapturingLabel = null
                activeCapturingItemTitle = null
            },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Lampirkan Foto Pemeriksaan",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Slate800
                )
                Text(
                    text = activeCapturingLabel.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Camera Option
                Surface(
                    onClick = {
                        showPhotoPickerSheet = false
                        val permission = Manifest.permission.CAMERA
                        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                            val uriFilePair = ImageCompressor.createTempImageUri(context)
                            if (uriFilePair != null) {
                                tempCameraFilePath = uriFilePair.second.absolutePath
                                cameraLauncher.launch(uriFilePair.first)
                            } else {
                                Toast.makeText(context, "Gagal membuat berkas sementara kamera", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            cameraPermissionLauncher.launch(permission)
                        }
                    },
                    color = Blue100,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = HighDensityBlue,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.padding(10.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Ambil Foto dari Kamera", fontWeight = FontWeight.Bold, color = Slate800)
                            Text("Dilengkapi otomatis watermark No Mobil, Auditor, Sample, Tanggal, Keterangan", style = MaterialTheme.typography.bodySmall, color = Slate600)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Gallery Option
                Surface(
                    onClick = {
                        showPhotoPickerSheet = false
                        galleryLauncher.launch("image/*")
                    },
                    color = Slate100,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Slate600,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.padding(10.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Pilih dari Galeri HP", fontWeight = FontWeight.Bold, color = Slate800)
                            Text("Pilih foto dokumentasi yang sudah tersimpan di HP", style = MaterialTheme.typography.bodySmall, color = Slate600)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Photo Markup Dialog (Editor Pena & Teks)
    activeMarkupTarget?.let { target ->
        PhotoMarkupEditorDialog(
            photoPath = target.second,
            title = target.third,
            vehicleNumber = formState.noMobil.ifBlank { formState.platNomer },
            auditorName = when {
                currentUser != null && currentUser.nip.isNotBlank() && currentUser.namaPetugas.isNotBlank() ->
                    "${currentUser.nip}/${currentUser.namaPetugas.uppercase()}"
                currentUser != null && currentUser.nip.isNotBlank() ->
                    currentUser.nip
                currentUser != null && currentUser.namaPetugas.isNotBlank() ->
                    currentUser.namaPetugas.uppercase()
                else -> currentUser?.username?.ifBlank { "AUDITOR" } ?: "AUDITOR"
            },
            sample = formState.jenisSample.ifBlank { "OPERASIONAL" },
            sampleTanggal = formState.sampleTanggal.ifBlank { null },
            paketService = formState.paketService.ifBlank { null },
            onSave = { markedPath ->
                onKpiPhotoCaptured(target.first, markedPath)
                activeMarkupTarget = null
            },
            onDismiss = { activeMarkupTarget = null }
        )
    }

    // Photo Preview Fullscreen Dialog
    previewPhotoTarget?.let { preview ->
        Dialog(onDismissRequest = { previewPhotoTarget = null }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = preview.second,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { previewPhotoTarget = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(com.example.util.DriveUrlHelper.resolveImageModel(preview.first))
                            .crossfade(true)
                            .build(),
                        contentDescription = preview.second,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoInfoField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Surface(
        color = Slate100,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Slate200),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Slate400, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Slate500,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = if (label == "ID") 0.5.sp else 0.sp
                    ),
                    color = if (label == "ID") HighDensityBlue else Slate800
                )
            }
            if (trailingAction != null) {
                trailingAction()
            }
        }
    }
}

@Composable
private fun DetailTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    testTag: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        placeholder = if (placeholder.isNotBlank()) { { Text(placeholder) } } else null,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = keyboardType,
            imeAction = ImeAction.Next
        ),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = HighDensityBlue,
            unfocusedBorderColor = Slate300
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.testTag(testTag)
    )
}

@Composable
private fun SectionHeaderBanner(section: KpiSection) {
    Surface(
        color = HighDensityBlue,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = Color.White
            )
        }
    }
}

@Composable
private fun GroupHeaderCard(group: KpiGroup) {
    Surface(
        color = Blue100,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Blue200),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = group.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = HighDensityBlue
            )
        }
    }
}

@Composable
private fun KpiItemCard(
    item: KpiItem,
    score: Int?,
    kpiPhotos: Map<String, String>,
    kpiNotes: Map<String, String>,
    onScoreChanged: (Int) -> Unit,
    onStartCapture: (String, String, String) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onNoteChanged: (String, String) -> Unit,
    onOpenMarkup: (String, String, String) -> Unit,
    onPreviewPhoto: (String, String) -> Unit
) {
    var expandedDropdown by remember { mutableStateOf(false) }

    val safeScore = score ?: 0
    val isFinding = safeScore > 0

    // Item card merepresentasikan Nilai/Point Temuan:
    // Bingkai tetap oranye (Amber600), font tulisan/text point menjadi biru (HighDensityBlue)
    val cardBorderColor = if (isFinding) Amber600 else Color(0xFFBBF7D0)
    val cardBgColor = if (isFinding) Color(0xFFFFFBEB) else Color(0xFFF0FDF4)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("kpi_item_${item.id}"),
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (isFinding) 1.5.dp else 1.dp,
            color = cardBorderColor
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Item Header with Name and Dropdown Score Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isFinding) Color(0xFFB45309) else Slate800
                        )
                        if (isFinding) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, Amber600)
                            ) {
                                Text(
                                    text = "TEMUAN",
                                    color = HighDensityBlue,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Nilai Maks: ${item.maxPoints} Poin",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500
                    )
                }

                // Dropdown Point Picker (Bingkai tetap oranye saat ada temuan poin; font jika tidak diklik berwarna oranye, jika diklik/dipilih berubah menjadi biru)
                Box {
                    val isDropdownOpen = expandedDropdown
                    Surface(
                        onClick = { expandedDropdown = !expandedDropdown },
                        color = if (isDropdownOpen) HighDensityBlueLight else if (isFinding) Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isFinding) Amber600 else if (isDropdownOpen) HighDensityBlue else Color(0xFF22C55E)
                        ),
                        modifier = Modifier.testTag("dropdown_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (safeScore == 0) "0 (Normal)" else "$safeScore Poin",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isDropdownOpen) HighDensityBlue else if (isFinding) Color(0xFFB45309) else Color(0xFF15803D)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = if (isDropdownOpen) HighDensityBlue else if (isFinding) Color(0xFFB45309) else Slate600
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        (0..item.maxPoints).forEach { pt ->
                            val isSelected = (pt == safeScore)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (pt == 0) "0 - Normal (Tidak Ada Temuan)" else "$pt Poin Temuan",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) HighDensityBlue else if (pt > 0) Color(0xFFB45309) else Color(0xFF15803D)
                                    )
                                },
                                onClick = {
                                    onScoreChanged(pt)
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // If score > 0, show the photo slots (Kotak Foto)
            AnimatedVisibility(visible = isFinding) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(
                        color = Color(0xFFFED7AA),
                        thickness = 1.dp
                    )

                    Text(
                        text = "Lampiran Foto Temuan (${safeScore} Slot Foto):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFB45309)
                    )

                    (0 until safeScore).forEach { slotIndex ->
                        val photoKey = "${item.id}_$slotIndex"
                        val label = item.getLabelForPhoto(slotIndex)
                        val photoPath = kpiPhotos[photoKey].orEmpty()
                        val noteText = kpiNotes[photoKey].orEmpty()

                        PhotoSlotRow(
                            photoKey = photoKey,
                            label = label,
                            photoPath = photoPath,
                            noteText = noteText,
                            onNoteChanged = { newNote -> onNoteChanged(photoKey, newNote) },
                            onCapture = { onStartCapture(photoKey, label, item.name) },
                            onRemove = { onRemovePhoto(photoKey) },
                            onOpenMarkup = { onOpenMarkup(photoKey, photoPath, label) },
                            onPreview = { onPreviewPhoto(photoPath, label) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoSlotRow(
    photoKey: String,
    label: String,
    photoPath: String,
    noteText: String,
    onNoteChanged: (String) -> Unit,
    onCapture: () -> Unit,
    onRemove: () -> Unit,
    onOpenMarkup: () -> Unit,
    onPreview: () -> Unit
) {
    val context = LocalContext.current
    val hasPhoto = photoPath.isNotBlank()

    // Garis berubah menjadi hijau yang sebelumnya oranye untuk membedakan mana yang sudah diupload atau belum
    val slotBorderColor = if (hasPhoto) Color(0xFF16A34A) else Amber600
    val slotBgColor = if (hasPhoto) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)

    Surface(
        color = slotBgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.5.dp,
            color = slotBorderColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("slot_$photoKey")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Label & Indikator Status Upload
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasPhoto) Icons.Default.CheckCircle else Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = if (hasPhoto) Color(0xFF16A34A) else Amber600,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (hasPhoto) Color(0xFF15803D) else Color(0xFFB45309)
                    )
                }

                Surface(
                    color = if (hasPhoto) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, if (hasPhoto) Color(0xFF86EFAC) else Color(0xFFFCD34D))
                ) {
                    Text(
                        text = if (hasPhoto) "✓ SUDAH DIUPLOAD" else "⚠ BELUM DIUPLOAD",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = if (hasPhoto) Color(0xFF15803D) else Color(0xFFB45309),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (!hasPhoto) {
                // Button to capture / upload foto (Garis dan aksen oranye sebelum diupload)
                OutlinedButton(
                    onClick = onCapture,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFFFF7ED),
                        contentColor = Color(0xFFC2410C)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFEA580C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFEA580C))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ambil / Upload Foto Temuan",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFC2410C)
                    )
                }
            } else {
                // Photo preview and controls (Garis hijau saat foto sudah diupload)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Image thumbnail dengan border hijau
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.5.dp, Color(0xFF16A34A), RoundedCornerShape(8.dp))
                            .clickable { onPreview() }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(com.example.util.DriveUrlHelper.resolveImageModel(photoPath))
                                .crossfade(true)
                                .build(),
                            contentDescription = label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Buttons: Zoom, Markup, Delete
                    Column(modifier = Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = onOpenMarkup,
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = ButtonDefaults.TextButtonContentPadding,
                                border = BorderStroke(1.dp, Color(0xFF16A34A)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF15803D)),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF16A34A))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pena / Tanda", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }

                            IconButton(
                                onClick = onPreview,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.ZoomIn, contentDescription = "Perbesar", tint = Color(0xFF16A34A))
                            }

                            IconButton(
                                onClick = onRemove,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFEF4444))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFFBBF7D0), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(6.dp))

                // Input Keterangan Foto
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Keterangan Foto Temuan (Opsional):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF15803D)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { onNoteChanged(it.uppercase()) },
                        placeholder = {
                            Text("TULIS KETERANGAN JIKA ADA (OPSIONAL)", fontSize = 11.5.sp, color = Slate400)
                        },
                        singleLine = false,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_input_$photoKey"),
                        shape = RoundedCornerShape(6.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF16A34A),
                            unfocusedBorderColor = Color(0xFF86EFAC)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun VehiclePhotoSlotRow(
    photoKey: String,
    label: String,
    description: String,
    photoPath: String,
    onCaptureCamera: () -> Unit,
    onRemove: () -> Unit,
    onPreview: () -> Unit
) {
    val hasPhoto = photoPath.isNotBlank()

    Surface(
        color = if (hasPhoto) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (hasPhoto) Color(0xFF86EFAC) else Slate300
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vehicle_photo_$photoKey")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (hasPhoto) Color(0xFFDCFCE7) else Slate200,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (hasPhoto) Icons.Default.CheckCircle else Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = if (hasPhoto) Color(0xFF16A34A) else Slate700,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Slate800
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (hasPhoto) Color(0xFFDCFCE7) else Color(0xFFFFF1F2),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, if (hasPhoto) Color(0xFF86EFAC) else Color(0xFFFECDD3))
                            ) {
                                Text(
                                    text = if (hasPhoto) "✓ Terisi" else "* Wajib",
                                    color = if (hasPhoto) Color(0xFF15803D) else Rose600,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Slate500
                        )
                    }
                }

                if (hasPhoto) {
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "✓ Terambil",
                            color = Color(0xFF15803D),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!hasPhoto) {
                // Button to capture directly from camera
                Button(
                    onClick = onCaptureCamera,
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("btn_capture_$photoKey")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ambil Foto $label",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                // Photo already taken: show thumbnail and actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onPreview() }
                            .weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Slate300, RoundedCornerShape(8.dp))
                                .background(Slate100)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(com.example.util.DriveUrlHelper.resolveImageModel(photoPath))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Foto $label",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Foto berhasil disimpan",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = Slate700
                            )
                            Text(
                                text = "Ketuk gambar untuk memperbesar",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = HighDensityBlue
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onPreview,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Perbesar", tint = HighDensityBlue)
                        }
                        IconButton(
                            onClick = onCaptureCamera,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Ambil Ulang", tint = Slate700)
                        }
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus Foto", tint = Color(0xFFEF4444))
                        }
                    }
                }
            }
        }
    }
}
