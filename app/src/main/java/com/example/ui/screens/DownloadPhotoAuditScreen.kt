package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.DataAuditEntity
import com.example.data.local.entity.MasterPeriodeEntity
import com.example.data.local.entity.MasterPoolEntity
import com.example.data.local.entity.PengaturanNamaPhotoEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.components.PhotoDetailDialog
import com.example.ui.theme.Amber600
import com.example.ui.theme.Blue50
import com.example.ui.theme.Blue700
import com.example.ui.theme.HighDensityBlue
import com.example.ui.theme.HighDensityBlueBorder
import com.example.ui.theme.HighDensityBlueLight
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Teal50
import com.example.ui.theme.Teal600
import com.example.util.AuditPhotoItem
import com.example.util.PhotoDownloadHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun DownloadPhotoAuditScreen(
    audits: List<DataAuditEntity>,
    periodes: List<MasterPeriodeEntity>,
    pools: List<MasterPoolEntity>,
    photoTitles: List<PengaturanNamaPhotoEntity>,
    currentUser: UserEntity?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val userLevel = currentUser?.levelAkses?.trim()?.uppercase() ?: ""
    val isKepalaBengkel = userLevel == "KEPALA BENGKEL" || userLevel.contains("KABAG")
    val userPool = currentUser?.kodePool?.trim().orEmpty()
    val isPoolRestricted = userPool.isNotBlank() && !userPool.equals("SKT", ignoreCase = true) && !userPool.equals("ALL", ignoreCase = true) &&
        (isKepalaBengkel || userLevel == "AUDITOR" || (userLevel != "SUPER ADMIN" && userLevel != "ADMIN" && userLevel != "SKT" && userLevel != "MASTER DATA"))

    var searchQuery by remember { mutableStateOf("") }
    var selectedPeriodeFilter by remember { mutableStateOf("SEMUA") }
    var selectedPoolFilter by remember(currentUser) {
        mutableStateOf(if (isPoolRestricted) userPool else "SEMUA")
    }

    var expandedPeriodeDropdown by remember { mutableStateOf(false) }
    var expandedPoolDropdown by remember { mutableStateOf(false) }

    // Selected audit for detail preview modal
    var selectedAuditForDetail by remember { mutableStateOf<DataAuditEntity?>(null) }

    // Download progress dialog state
    var isDownloading by remember { mutableStateOf(false) }
    var downloadCurrentCount by remember { mutableIntStateOf(0) }
    var downloadTotalCount by remember { mutableIntStateOf(0) }
    var downloadStatusText by remember { mutableStateOf("") }
    var downloadTargetLabel by remember { mutableStateOf("") }

    // Pre-calculate photos for each audit to filter only audits with photos
    val auditsWithPhotos = remember(audits, photoTitles) {
        audits.map { audit ->
            val photos = PhotoDownloadHelper.extractPhotosFromAudit(audit, photoTitles)
            audit to photos
        }.filter { it.second.isNotEmpty() }
    }

    // Filter audits (Kabag strictly restricted to their led pool)
    val filteredAuditList = remember(
        auditsWithPhotos,
        searchQuery,
        selectedPeriodeFilter,
        selectedPoolFilter,
        isPoolRestricted,
        userPool
    ) {
        auditsWithPhotos.filter { (audit, _) ->
            if (isPoolRestricted && !audit.pool.equals(userPool, ignoreCase = true)) {
                return@filter false
            }

            val matchSearch = searchQuery.isBlank() ||
                audit.noMobil.contains(searchQuery, ignoreCase = true) ||
                audit.platNomer.contains(searchQuery, ignoreCase = true) ||
                audit.namaPetugas.contains(searchQuery, ignoreCase = true)

            val matchPeriode = selectedPeriodeFilter == "SEMUA" ||
                audit.periode.equals(selectedPeriodeFilter, ignoreCase = true)

            val matchPool = if (isPoolRestricted) {
                true
            } else {
                selectedPoolFilter == "SEMUA" ||
                    audit.pool.equals(selectedPoolFilter, ignoreCase = true)
            }

            matchSearch && matchPeriode && matchPool
        }
    }

    val totalAvailablePhotos = remember(filteredAuditList) {
        filteredAuditList.sumOf { it.second.size }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Top Header Card with User Role Info
        Surface(
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = HighDensityBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Download Foto Hasil Input",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = Slate900
                                )
                            )
                        }
                        Text(
                            text = "Unduh foto hasil audit kendaraan langsung ke Galeri perangkat (Pictures/AuditPhotos)",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.5.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Stat 1: Total Audit
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(HighDensityBlueLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = HighDensityBlue, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "${filteredAuditList.size}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                                Text(text = "Audit Tersedia", style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontSize = 10.sp))
                            }
                        }
                    }

                    // Stat 2: Total Foto
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFDCFCE7), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Teal600, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "$totalAvailablePhotos", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF15803D)))
                                Text(text = "Foto Tersimpan", style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontSize = 10.sp))
                            }
                        }
                    }
                }
            }
        }

        // Filters Section
        Surface(
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Slate200)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari No Mobil / Plat / Nama Auditor...", fontSize = 12.sp, color = Slate400) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate400, modifier = Modifier.size(16.dp))
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
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Dropdown Filters: Periode & Pool
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Periode Filter
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            onClick = { expandedPeriodeDropdown = !expandedPeriodeDropdown },
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Slate300),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Periode:", style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontSize = 9.sp))
                                    Text(
                                        text = selectedPeriodeFilter,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate800, fontSize = 11.5.sp),
                                        maxLines = 1
                                    )
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Slate600)
                            }
                        }

                        DropdownMenu(
                            expanded = expandedPeriodeDropdown,
                            onDismissRequest = { expandedPeriodeDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("SEMUA PERIODE", fontWeight = if (selectedPeriodeFilter == "SEMUA") FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedPeriodeFilter = "SEMUA"
                                    expandedPeriodeDropdown = false
                                }
                            )
                            periodes.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.periode, fontWeight = if (selectedPeriodeFilter == p.periode) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedPeriodeFilter = p.periode
                                        expandedPeriodeDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Pool Filter
                    Box(modifier = Modifier.weight(1f)) {
                        if (isPoolRestricted) {
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Slate300),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Pool Dipimpin:", style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontSize = 9.sp))
                                        Text(
                                            text = userPool,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate800, fontSize = 11.5.sp),
                                            maxLines = 1
                                        )
                                    }
                                    Icon(Icons.Default.Lock, contentDescription = "Terkunci", tint = Slate500, modifier = Modifier.size(15.dp))
                                }
                            }
                        } else {
                            Surface(
                                onClick = { expandedPoolDropdown = !expandedPoolDropdown },
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Slate300),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Pool:", style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontSize = 9.sp))
                                        Text(
                                            text = selectedPoolFilter,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate800, fontSize = 11.5.sp),
                                            maxLines = 1
                                        )
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Slate600)
                                }
                            }

                            DropdownMenu(
                                expanded = expandedPoolDropdown,
                                onDismissRequest = { expandedPoolDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("SEMUA POOL", fontWeight = if (selectedPoolFilter == "SEMUA") FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedPoolFilter = "SEMUA"
                                        expandedPoolDropdown = false
                                    }
                                )
                                pools.forEach { pl ->
                                    DropdownMenuItem(
                                        text = { Text("${pl.kodePool} - ${pl.namaPool}", fontWeight = if (selectedPoolFilter == pl.kodePool) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            selectedPoolFilter = pl.kodePool
                                            expandedPoolDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // List of Audits with Photos
        if (filteredAuditList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Slate300,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tidak Ada Foto Hasil Input",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate600)
                    )
                    Text(
                        text = "Belum ada audit yang memiliki foto terlampir sesuai kriteria filter.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate400),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredAuditList, key = { it.first.id }) { (audit, photos) ->
                    AuditDownloadCard(
                        audit = audit,
                        photos = photos,
                        onDownloadAll = {
                            isDownloading = true
                            downloadTargetLabel = "Mobil ${audit.noMobil} (${photos.size} Foto)"
                            downloadCurrentCount = 0
                            downloadTotalCount = photos.size
                            downloadStatusText = "Memulai pengunduhan..."

                            coroutineScope.launch {
                                val (success, fail, msg) = PhotoDownloadHelper.downloadAllAuditPhotos(
                                    context = context,
                                    audit = audit,
                                    photoTitles = photoTitles,
                                    onProgress = { cur, tot ->
                                        downloadCurrentCount = cur
                                        downloadTotalCount = tot
                                        downloadStatusText = "Menyimpan foto $cur dari $tot..."
                                    }
                                )
                                isDownloading = false
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        onOpenDetail = {
                            selectedAuditForDetail = audit
                        }
                    )
                }
            }
        }
    }

    // Detail Modal Dialog
    selectedAuditForDetail?.let { audit ->
        PhotoDetailDialog(
            audit = audit,
            photoTitles = photoTitles,
            currentUser = currentUser,
            onDismiss = { selectedAuditForDetail = null }
        )
    }

    // Download Progress Dialog
    if (isDownloading) {
        Dialog(onDismissRequest = { /* Non-dismissible while active */ }) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(HighDensityBlueLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DownloadForOffline,
                            contentDescription = null,
                            tint = HighDensityBlue,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Mengunduh Foto Hasil Input",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                    )
                    Text(
                        text = downloadTargetLabel,
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600, fontSize = 12.sp),
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val progressFraction = if (downloadTotalCount > 0) {
                        downloadCurrentCount.toFloat() / downloadTotalCount.toFloat()
                    } else 0f

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = HighDensityBlue,
                        trackColor = Color(0xFFE2E8F0)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = downloadStatusText,
                            style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontSize = 11.sp)
                        )
                        Text(
                            text = "$downloadCurrentCount / $downloadTotalCount",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = HighDensityBlue, fontSize = 11.sp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Foto disimpan ke Galeri HP: Pictures/AuditPhotos",
                        style = MaterialTheme.typography.labelSmall.copy(color = Slate400, fontSize = 10.sp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AuditDownloadCard(
    audit: DataAuditEntity,
    photos: List<AuditPhotoItem>,
    onDownloadAll: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Mobil & Action Download Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(HighDensityBlueLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = HighDensityBlue, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${audit.noMobil} • ${audit.platNomer}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate800,
                                fontSize = 15.sp
                            )
                        )
                        Text(
                            text = "${audit.tipe} • ${audit.warna} • Pool: ${audit.pool}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp)
                        )
                        Row(
                            modifier = Modifier.padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (audit.periode.isNotBlank()) {
                                Surface(
                                    color = Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = audit.periode,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = HighDensityBlue
                                        ),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${photos.size} Foto Terlampir",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309)
                                    ),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Primary Download Button
                Button(
                    onClick = onDownloadAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HighDensityBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download Semua",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Unduh Semua",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pratinjau Thumbnail Foto Hasil Input (Horizontal Scroll)
            Text(
                text = "Foto Hasil Input (${photos.size}):",
                style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                photos.forEach { photoItem ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Slate200),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier
                            .width(100.dp)
                            .clickable { onOpenDetail() }
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(width = 100.dp, height = 70.dp)
                                    .background(Color(0xFF0F172A))
                            ) {
                                val model = com.example.util.DriveUrlHelper.resolveImageModel(photoItem.photoPath)
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(model)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = photoItem.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(bottomEnd = 4.dp),
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    Text(
                                        text = "#${photoItem.slotIndex}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            Text(
                                text = photoItem.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Slate800
                                ),
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            // Footer info & "Lihat Detail" button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auditor: ${audit.namaPetugas} • ${audit.tanggal}",
                    style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontSize = 10.sp)
                )

                OutlinedButton(
                    onClick = onOpenDetail,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HighDensityBlue),
                    border = BorderStroke(1.dp, HighDensityBlueBorder),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lihat & Pilih Foto", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp))
                }
            }
        }
    }
}
