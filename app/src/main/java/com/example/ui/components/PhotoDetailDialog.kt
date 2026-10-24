package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.DataAuditEntity
import com.example.data.local.entity.PengaturanNamaPhotoEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.theme.Amber50
import com.example.ui.theme.Amber600
import com.example.ui.theme.Blue50
import com.example.ui.theme.Blue700
import com.example.ui.theme.Blue900
import com.example.ui.theme.HighDensityBlue
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Teal50
import com.example.ui.theme.Teal600
import com.example.util.DateUtils
import com.example.util.PhotoDownloadHelper
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoDetailDialog(
    audit: DataAuditEntity,
    photoTitles: List<PengaturanNamaPhotoEntity>,
    currentUser: UserEntity? = null,
    onEditAudit: ((DataAuditEntity) -> Unit)? = null,
    onDeleteAudit: ((Long) -> Unit)? = null,
    onDeletePhoto: ((itemKey: String, photoPath: String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var fullScreenPhotoUrl by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showFotoKendaraanDialog by remember { mutableStateOf(false) }
    var photoToDelete by remember { mutableStateOf<Pair<String, String>?>(null) }

    val userLevel = currentUser?.levelAkses?.trim()?.uppercase() ?: ""
    val isKepalaBengkel = userLevel == "KEPALA BENGKEL" || userLevel.contains("KABAG")
    val userPool = currentUser?.kodePool?.trim().orEmpty()
    val canDownload = when {
        isKepalaBengkel -> userPool.isNotBlank() && audit.pool.equals(userPool, ignoreCase = true)
        else -> userLevel in listOf("MASTER DATA", "ADMIN", "SUPER ADMIN", "SKT")
    }
    val isSuperAdmin = userLevel == "SUPER ADMIN"
    val isAdmin = userLevel == "ADMIN"
    val isSkt = userLevel == "SKT"
    val isAuditor = userLevel == "AUDITOR"
    val isAuditorSamePool = isAuditor && currentUser != null && audit.pool.isNotBlank() && audit.pool.trim().equals(currentUser.kodePool.trim(), ignoreCase = true)
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
    val canEdit = isSuperAdmin || ((isCreator || isAuditorSamePool) && isWithinEditHours)
    val canDelete = (isSuperAdmin || ((isAdmin || isSkt) && isWithin48Hours)) && !isAuditor
    val canDeletePhoto = canEdit || canDelete

    val remainingHours = if (!isSuperAdmin && (isCreator || isAuditorSamePool) && isWithinEditHours) {
        val rem = maxEditHoursMs - elapsedMs
        val h = rem / (1000 * 60 * 60)
        val m = (rem % (1000 * 60 * 60)) / (1000 * 60)
        "${h}j ${m}m"
    } else null

    var showPrintReportDialog by remember { mutableStateOf(false) }

    val photoList = remember(audit) {
        listOf(
            Pair(audit.photo1, audit.catatanPhoto1),
            Pair(audit.photo2, audit.catatanPhoto2),
            Pair(audit.photo3, audit.catatanPhoto3),
            Pair(audit.photo4, audit.catatanPhoto4),
            Pair(audit.photo5, audit.catatanPhoto5),
            Pair(audit.photo6, audit.catatanPhoto6),
            Pair(audit.photo7, audit.catatanPhoto7),
            Pair(audit.photo8, audit.catatanPhoto8),
            Pair(audit.photo9, audit.catatanPhoto9),
            Pair(audit.photo10, audit.catatanPhoto10),
            Pair(audit.photo11, audit.catatanPhoto11),
            Pair(audit.photo12, audit.catatanPhoto12),
            Pair(audit.photo13, audit.catatanPhoto13),
            Pair(audit.photo14, audit.catatanPhoto14),
            Pair(audit.photo15, audit.catatanPhoto15),
            Pair(audit.photo16, audit.catatanPhoto16),
            Pair(audit.photo17, audit.catatanPhoto17),
            Pair(audit.photo18, audit.catatanPhoto18),
            Pair(audit.photo19, audit.catatanPhoto19),
            Pair(audit.photo20, audit.catatanPhoto20)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Dialog
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Blue700)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hasil Inspeksi",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    letterSpacing = (-0.2).sp
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text("No Mobil", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), modifier = Modifier.width(85.dp))
                                        Text(audit.noMobil, color = Color.White, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text("Plat Nomer", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), modifier = Modifier.width(85.dp))
                                        Text(audit.platNomer, color = Color.White, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text("Tipe", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), modifier = Modifier.width(85.dp))
                                        Text(audit.tipe.ifBlank { "-" }, color = Color.White, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text("Warna", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), modifier = Modifier.width(85.dp))
                                        Text(audit.warna.ifBlank { "-" }, color = Color.White, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text("Tahun", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), modifier = Modifier.width(85.dp))
                                        Text(audit.tahun.ifBlank { "-" }, color = Color.White, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text("pool Armada", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), modifier = Modifier.width(85.dp))
                                        Text(audit.pool.ifBlank { "-" }, color = Color.White, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
                                    }
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (canEdit && onEditAudit != null) {
                                Surface(
                                    color = Amber50,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Amber600.copy(alpha = 0.6f)),
                                    modifier = Modifier.clickable {
                                        onEditAudit(audit)
                                        onDismiss()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Amber600, modifier = Modifier.size(13.dp))
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

                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Summary Metadata Bar (di bawah No kendaraan sampai pool armada di atas KETERANGAN INSPEKSI)
                Surface(
                    color = Blue50,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Auditor:", style = MaterialTheme.typography.bodySmall.copy(color = Slate600, fontSize = 11.5.sp), modifier = Modifier.width(115.dp))
                            val auditorText = when {
                                audit.nip.isNotBlank() && audit.namaPetugas.isNotBlank() -> "${audit.nip}/${audit.namaPetugas}"
                                audit.nip.isNotBlank() -> audit.nip
                                audit.namaPetugas.isNotBlank() -> audit.namaPetugas
                                else -> "-"
                            }
                            Text(
                                auditorText,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Blue900, fontSize = 11.5.sp)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Tanggal:", style = MaterialTheme.typography.bodySmall.copy(color = Slate600, fontSize = 11.5.sp), modifier = Modifier.width(115.dp))
                            val waktuTimestampAudit = DateUtils.formatDisplayWaktuTimestamp(audit.timestamp, audit.tanggal)
                            Text(
                                waktuTimestampAudit,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = Slate900, fontSize = 11.5.sp)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Periode:", style = MaterialTheme.typography.bodySmall.copy(color = Slate600, fontSize = 11.5.sp), modifier = Modifier.width(115.dp))
                            Text(
                                audit.periode.ifBlank { "-" },
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = Slate900, fontSize = 11.5.sp)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Sample:", style = MaterialTheme.typography.bodySmall.copy(color = Slate600, fontSize = 11.5.sp), modifier = Modifier.width(115.dp))
                            Text(
                                audit.jenisSample.ifBlank { "-" },
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = Slate900, fontSize = 11.5.sp)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Tanggal Service:", style = MaterialTheme.typography.bodySmall.copy(color = Slate600, fontSize = 11.5.sp), modifier = Modifier.width(115.dp))
                            val isService = audit.jenisSample.equals("SERVICE", ignoreCase = true)
                            val tgl = if (isService) {
                                if (audit.sampleTanggal.isNotBlank()) audit.sampleTanggal else audit.tanggalService.ifBlank { "-" }
                            } else {
                                "-"
                            }
                            Text(
                                tgl,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = Slate900, fontSize = 11.5.sp)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Paket Service:", style = MaterialTheme.typography.bodySmall.copy(color = Slate600, fontSize = 11.5.sp), modifier = Modifier.width(115.dp))
                            val isService = audit.jenisSample.equals("SERVICE", ignoreCase = true)
                            val pkt = if (isService) {
                                if (audit.paketService.isNotBlank()) audit.paketService else audit.service.ifBlank { "-" }
                            } else {
                                "-"
                            }
                            Text(
                                pkt,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = Slate900, fontSize = 11.5.sp)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Temuan:", style = MaterialTheme.typography.bodySmall.copy(color = Slate600, fontSize = 11.5.sp), modifier = Modifier.width(115.dp))
                            Text(
                                "${audit.totalPoinTemuan} Poin",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (audit.totalPoinTemuan > 0) Color(0xFFE11D48) else Teal600,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Parse KPI Scores & calculate categories
                val kpiScores = remember(audit.kpiScoresJson) {
                    try {
                        val obj = org.json.JSONObject(audit.kpiScoresJson)
                        val map = mutableMapOf<String, Int>()
                        val keys = obj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            map[k] = obj.optInt(k, 0)
                        }
                        map
                    } catch (e: Exception) {
                        emptyMap<String, Int>()
                    }
                }

                val exteriorPoints = remember(kpiScores) {
                    com.example.data.model.KpiAuditSpec.sections.find { it.id == "EXTERIOR" }?.groups?.flatMap { it.items }?.sumOf { kpiScores[it.id] ?: 0 } ?: 0
                }
                val interiorPoints = remember(kpiScores) {
                    com.example.data.model.KpiAuditSpec.sections.find { it.id == "INTERIOR" }?.groups?.flatMap { it.items }?.sumOf { kpiScores[it.id] ?: 0 } ?: 0
                }
                val cucianPoints = remember(kpiScores) {
                    com.example.data.model.KpiAuditSpec.sections.find { it.id == "CUCIAN" }?.groups?.flatMap { it.items }?.sumOf { kpiScores[it.id] ?: 0 } ?: 0
                }
                val findingsItems = remember(kpiScores) {
                    com.example.data.model.KpiAuditSpec.getAllItems().filter { (kpiScores[it.id] ?: 0) > 0 }.map { item ->
                        val score = kpiScores[item.id] ?: 0
                        val section = com.example.data.model.KpiAuditSpec.sections.find { sec -> sec.groups.any { grp -> grp.items.any { it.id == item.id } } }
                        Triple(item, score, section?.title ?: "")
                    }
                }

                // Build photo items using PhotoDownloadHelper
                val filledPhotoItems = remember(audit, photoTitles) {
                    PhotoDownloadHelper.extractPhotosFromAudit(audit, photoTitles)
                }

                // Foto Hasil Pengecekan: Foto dengan catatan "Foto Kendaraan" (termasuk foto dokumentasi kendaraan) jangan ditampilkan
                val inspectionPhotoItems = remember(filledPhotoItems) {
                    filledPhotoItems.filter { item ->
                        val trimmedNote = item.note.trim()
                        !trimmedNote.equals("Foto Kendaraan", ignoreCase = true) &&
                        !trimmedNote.contains("Foto Kendaraan", ignoreCase = true) &&
                        !item.itemKey.startsWith("kendaraan_")
                    }
                }

                // Main Content (Hasil Pengecekan Inspeksi & Foto Hasil Pengecekan)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. HASIL PENGECEKAN INSPEKSI (Skor & Status Temuan)
                    item {
                        val hasTemuan = audit.totalPoinTemuan > 0
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (hasTemuan) Color(0xFFFFFBEB) else Color(0xFFF0FDF4)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (hasTemuan) Color(0xFFFDE68A) else Color(0xFFBBF7D0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(
                                                    if (hasTemuan) Color(0xFFD97706) else Color(0xFF16A34A),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (hasTemuan) Icons.Default.Warning else Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "HASIL PENGECEKAN INSPEKSI",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (hasTemuan) Color(0xFFB45309) else Color(0xFF15803D),
                                                fontSize = 13.sp,
                                                letterSpacing = 0.5.sp
                                            )
                                        )
                                    }
                                    Surface(
                                        color = if (hasTemuan) Color(0xFFDC2626) else Color(0xFF16A34A),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${audit.totalPoinTemuan} POIN TEMUAN",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // 3 Category Breakdown Pills
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Exterior
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "A. EXTERIOR",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate600,
                                                    fontSize = 9.5.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "$exteriorPoints Poin",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Black,
                                                    color = if (exteriorPoints > 0) Color(0xFFDC2626) else Color(0xFF16A34A),
                                                    fontSize = 13.sp
                                                )
                                            )
                                        }
                                    }
                                    // Interior
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "B. INTERIOR",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate600,
                                                    fontSize = 9.5.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "$interiorPoints Poin",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Black,
                                                    color = if (interiorPoints > 0) Color(0xFFDC2626) else Color(0xFF16A34A),
                                                    fontSize = 13.sp
                                                )
                                            )
                                        }
                                    }
                                    // Cucian
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "C. CUCIAN",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate600,
                                                    fontSize = 9.5.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "$cucianPoints Poin",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Black,
                                                    color = if (cucianPoints > 0) Color(0xFFDC2626) else Color(0xFF16A34A),
                                                    fontSize = 13.sp
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Detail Temuan or Normal Message
                                if (findingsItems.isNotEmpty()) {
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Rincian Titik Temuan (${findingsItems.size} Item):",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309),
                                                    fontSize = 11.sp
                                                )
                                            )
                                            findingsItems.forEach { (item, score, sectionTitle) ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        color = Color(0xFFFEF2F2),
                                                        shape = RoundedCornerShape(4.dp),
                                                        border = BorderStroke(0.5.dp, Color(0xFFFECACA))
                                                    ) {
                                                        Text(
                                                            text = "$score Poin",
                                                            color = Color(0xFFDC2626),
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.Black,
                                                                fontSize = 10.sp
                                                            ),
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "${if (sectionTitle.isNotBlank()) "[$sectionTitle] " else ""}${item.name}",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = Slate800,
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 11.5.sp
                                                        ),
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF16A34A),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Kondisi Normal (0 Poin) - Semua titik pemeriksaan memenuhi standar operasional.",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = Color(0xFF15803D),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. FOTO HASIL PENGECEKAN (Tepat di bawah Hasil PENGECEKAN INSPEKSI)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(Blue700, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoLibrary,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "FOTO HASIL PENGECEKAN",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Slate800,
                                                fontSize = 13.sp,
                                                letterSpacing = 0.5.sp
                                            )
                                        )
                                    }
                                    Surface(
                                        color = Color(0xFFDBEAFE),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${inspectionPhotoItems.size} FOTO",
                                            color = Blue900,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (inspectionPhotoItems.isEmpty()) {
                        item {
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ImageNotSupported,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Tidak ada foto pengecekan yang diunggah untuk inspeksi ini.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Slate600,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(inspectionPhotoItems) { displayIndex, photoItem ->
                            val slotNum = displayIndex + 1
                            val photoPath = photoItem.photoPath
                            val displayTitle = photoItem.title
                            val note = photoItem.note
                            val itemKey = photoItem.itemKey
                            val isGeneralVehicle = itemKey.startsWith("kendaraan_")
                            val kpiItemId = if (itemKey.contains("_")) itemKey.substringBeforeLast("_") else itemKey
                            val score = kpiScores[kpiItemId] ?: 0

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (score > 0) Color(0xFFFECACA) else Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Thumbnail with Zoom Overlay
                                    Box(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(75.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                            .clickable {
                                                fullScreenPhotoUrl = Pair(photoPath, displayTitle)
                                            }
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(com.example.util.DriveUrlHelper.resolveImageModel(photoPath))
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = displayTitle,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        Surface(
                                            color = Color.Black.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(topStart = 4.dp),
                                            modifier = Modifier.align(Alignment.BottomEnd)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ZoomIn,
                                                contentDescription = "Perbesar",
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .padding(2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Title, Status badge & Note
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                color = Blue50,
                                                shape = CircleShape,
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "$slotNum",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Black,
                                                            color = Blue700,
                                                            fontSize = 9.5.sp
                                                        )
                                                    )
                                                }
                                            }
                                            Text(
                                                text = displayTitle,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate900,
                                                    fontSize = 12.sp
                                                ),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Badge Status
                                        if (isGeneralVehicle) {
                                            Surface(
                                                color = Color(0xFFEFF6FF),
                                                shape = RoundedCornerShape(4.dp),
                                                border = BorderStroke(0.5.dp, Color(0xFFBFDBFE))
                                            ) {
                                                Text(
                                                    text = "Dokumentasi Armada",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = HighDensityBlue,
                                                        fontSize = 9.5.sp
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else if (score > 0) {
                                            Surface(
                                                color = Color(0xFFFEF2F2),
                                                shape = RoundedCornerShape(4.dp),
                                                border = BorderStroke(0.5.dp, Color(0xFFFECACA))
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .background(Color(0xFFDC2626), CircleShape)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Hasil: Temuan ($score Poin)",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFDC2626),
                                                            fontSize = 9.5.sp
                                                        )
                                                    )
                                                }
                                            }
                                        } else {
                                            Surface(
                                                color = Color(0xFFF0FDF4),
                                                shape = RoundedCornerShape(4.dp),
                                                border = BorderStroke(0.5.dp, Color(0xFFBBF7D0))
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .background(Color(0xFF16A34A), CircleShape)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Hasil: Normal (0 Poin)",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF16A34A),
                                                            fontSize = 9.5.sp
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        if (note.isNotBlank()) {
                                            Surface(
                                                color = Color(0xFFFFFBEB),
                                                shape = RoundedCornerShape(4.dp),
                                                border = BorderStroke(0.5.dp, Color(0xFFFDE68A))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Text(
                                                        text = "Catatan: ",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFB45309),
                                                            fontSize = 10.sp
                                                        )
                                                    )
                                                    Text(
                                                        text = note,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = Slate800,
                                                            fontSize = 10.5.sp
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Zoom action button
                                    IconButton(
                                        onClick = {
                                            fullScreenPhotoUrl = Pair(photoPath, displayTitle)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ZoomIn,
                                            contentDescription = "Zoom",
                                            tint = HighDensityBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    if (canDeletePhoto && onDeletePhoto != null) {
                                        IconButton(
                                            onClick = {
                                                photoToDelete = Pair(itemKey, photoPath)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus Foto",
                                                tint = Rose600,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Action Bar (Pojok Kanan Bawah: HAPUS, DOWNLOAD PDF, & TUTUP)
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Surface(
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = if (canDelete && onDeleteAudit != null) Arrangement.SpaceBetween else Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (canDelete && onDeleteAudit != null) {
                            var showConfirmDelete by remember { mutableStateOf(false) }
                            OutlinedButton(
                                onClick = { showConfirmDelete = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose600),
                                border = BorderStroke(1.dp, Rose600),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus Data",
                                    tint = Rose600,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "HAPUS",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Rose600,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                            if (showConfirmDelete) {
                                AlertDialog(
                                    onDismissRequest = { showConfirmDelete = false },
                                    title = { Text("Konfirmasi Hapus Data") },
                                    text = { Text("Apakah Anda yakin ingin menghapus data audit armada ${audit.noMobil} (${audit.tanggal})?") },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                showConfirmDelete = false
                                                onDeleteAudit(audit.id)
                                                onDismiss()
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

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Tombol Download PDF (di samping tombol tutup)
                            Button(
                                onClick = { showPrintReportDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = com.example.ui.theme.HighDensityBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_download_pdf_hasil_inspeksi")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download PDF",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DOWNLOAD PDF",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Blue700,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_tutup_hasil_inspeksi")
                            ) {
                                Text(
                                    text = "TUTUP",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Full screen image viewer with Interactive Pinch-to-zoom, Pan, and Zoom Controls
    fullScreenPhotoUrl?.let { (url, title) ->
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        Dialog(
            onDismissRequest = { fullScreenPhotoUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
            ) {
                // 1. KOLOM 1: KOLOM JUDUL & TOMBOL SILANG
                Surface(
                    color = Color(0xFF1E293B),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TITIK PEMERIKSAAN",
                                color = Color(0xFF38BDF8),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (canDownload) {
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val cleanTitle = title.replace("[^a-zA-Z0-9]".toRegex(), "_")
                                            val fileName = "FOTO_${audit.noMobil}_${cleanTitle}_${System.currentTimeMillis()}.jpg"
                                            val res = PhotoDownloadHelper.savePhotoToGallery(context, url, fileName)
                                            if (res.isSuccess) {
                                                Toast.makeText(context, "Foto berhasil diunduh ke Galeri (Pictures/AuditPhotos)", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Gagal mengunduh foto: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(HighDensityBlue, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Download Foto",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            // Tombol Silang Berada di Kolom Judul (Tidak Menutupi Foto)
                            IconButton(
                                onClick = { fullScreenPhotoUrl = null },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)

                // 2. JARAK ANTARA KOLOM JUDUL DENGAN KOLOM FOTO (Foto dibuat lebih ke bawah)
                Spacer(modifier = Modifier.height(24.dp))

                val imageModel = com.example.util.DriveUrlHelper.resolveImageModel(url)

                // 3. KOLOM 2: KOLOM FOTO
                // Wadah foto yang berada di bawah kolom judul & tombol silang, mengikuti lebar HP
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    // Zoomable and Pannable Image View
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 6f)
                                    if (scale > 1f) {
                                        val maxOffset = (scale - 1f) * 600f
                                        offset = Offset(
                                            x = (offset.x + pan.x * scale).coerceIn(-maxOffset, maxOffset),
                                            y = (offset.y + pan.y * scale).coerceIn(-maxOffset, maxOffset)
                                        )
                                    } else {
                                        offset = Offset.Zero
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (scale > 1.2f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            scale = 2.5f
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageModel)
                                .crossfade(true)
                                .build(),
                            contentDescription = title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                ),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Bottom Floating Controls Bar: Zoom In, Zoom Out, Reset, and Zoom % Indicator
                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp)
                    ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Zoom Out Button (-)
                        IconButton(
                            onClick = {
                                scale = (scale - 0.5f).coerceAtLeast(1f)
                                if (scale == 1f) offset = Offset.Zero
                            },
                            enabled = scale > 1f,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Zoom Out",
                                tint = if (scale > 1f) Color.White else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Zoom Percentage Indicator
                        Surface(
                            color = HighDensityBlue,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${(scale * 100).toInt()}%",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        // Zoom In Button (+)
                        IconButton(
                            onClick = {
                                scale = (scale + 0.5f).coerceAtMost(6f)
                            },
                            enabled = scale < 6f,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Zoom In",
                                tint = if (scale < 6f) Color.White else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Reset 1.0x Button
                        if (scale > 1f || offset != Offset.Zero) {
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.clickable {
                                    scale = 1f
                                    offset = Offset.Zero
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset Zoom",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "1.0x",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (photoToDelete != null) {
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = {
                Text(
                    text = "Hapus Foto Lampiran",
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin menghapus foto lampiran ini?",
                    color = Slate800,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = photoToDelete
                        photoToDelete = null
                        if (target != null && onDeletePhoto != null) {
                            onDeletePhoto(target.first, target.second)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600)
                ) {
                    Text("Hapus Foto", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { photoToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showFotoKendaraanDialog) {
        FotoKendaraanDialog(
            audit = audit,
            photoTitles = photoTitles,
            currentUser = currentUser,
            onDismiss = { showFotoKendaraanDialog = false },
            onEditAudit = onEditAudit
        )
    }

    if (showPrintReportDialog) {
        PrintInspectionReportDialog(
            audit = audit,
            photoTitles = photoTitles,
            onDismiss = { showPrintReportDialog = false }
        )
    }
}
}
