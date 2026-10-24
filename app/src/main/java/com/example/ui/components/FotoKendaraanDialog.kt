package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.theme.*
import com.example.util.DateUtils
import com.example.util.PhotoDownloadHelper
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FotoKendaraanDialog(
    audit: DataAuditEntity,
    photoTitles: List<PengaturanNamaPhotoEntity>,
    currentUser: UserEntity?,
    onDismiss: () -> Unit,
    onEditAudit: ((DataAuditEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isDownloadingAll by remember { mutableStateOf(false) }

    var fullScreenPhotoUrl by remember { mutableStateOf<Pair<String, String>?>(null) }

    val isSuperAdmin = currentUser?.levelAkses?.trim()?.uppercase() == "SUPER ADMIN"
    val isAdmin = currentUser?.levelAkses?.trim()?.uppercase() == "ADMIN"
    val isMasterData = currentUser?.levelAkses?.trim()?.uppercase() == "MASTER DATA"
    val isSkt = currentUser?.levelAkses?.trim()?.uppercase() == "SKT"
    val canDownload = isSuperAdmin || isAdmin || isMasterData || isSkt

    // Filter khusus: Pada Tampilan FOTO KENDARAAN hanya menampilkan foto dengan label Dokumentasi Armada
    val vehiclePhotoItems = remember(audit, photoTitles) {
        val allPhotos = PhotoDownloadHelper.extractPhotosFromAudit(audit, photoTitles)
        allPhotos
            .filter { item ->
                item.itemKey.startsWith("kendaraan_") ||
                item.note.trim().equals("Foto Kendaraan", ignoreCase = true) ||
                item.note.trim().contains("Foto Kendaraan", ignoreCase = true) ||
                item.title.contains("Foto Kendaraan", ignoreCase = true)
            }
            .mapIndexed { idx, item ->
                item.copy(slotIndex = idx + 1)
            }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Dialog: FOTO KENDARAAN
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Blue700)
                        .padding(16.dp)
                ) {
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
                                    .size(38.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "FOTO KENDARAAN",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        letterSpacing = (-0.2).sp
                                    )
                                )
                                Text(
                                    text = "${audit.noMobil} • ${audit.platNomer} (${audit.tipe}) • Pool: ${audit.pool}",
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp)
                                )
                            }
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

                // Summary Metadata Bar
                Surface(
                    color = Blue50,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            val auditorText = if (audit.nip.isNotBlank()) "${audit.namaPetugas.ifBlank { audit.nip }}" else audit.namaPetugas
                            Text(
                                text = "Auditor: $auditorText",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Blue900,
                                    fontSize = 11.5.sp
                                )
                            )
                            val waktuTimestampAudit = DateUtils.formatDisplayWaktuTimestamp(audit.timestamp, audit.tanggal)
                            Text(
                                text = "Waktu: $waktuTimestampAudit • Periode: ${audit.periode.ifBlank { "-" }}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate600, fontSize = 11.sp)
                            )
                        }

                        Surface(
                            color = HighDensityBlue,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${vehiclePhotoItems.size} Foto Terlampir",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Actions Header (Download all photos if authorized)
                if (canDownload && vehiclePhotoItems.isNotEmpty()) {
                    Surface(
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daftar Foto Dokumentasi Fisik Armada",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate700,
                                    fontSize = 11.5.sp
                                )
                            )

                            Surface(
                                color = HighDensityBlue,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.clickable(enabled = !isDownloadingAll) {
                                    isDownloadingAll = true
                                    coroutineScope.launch {
                                        val (_, _, msg) = PhotoDownloadHelper.downloadPhotosList(
                                            context = context,
                                            photos = vehiclePhotoItems,
                                            noMobil = audit.noMobil,
                                            periode = audit.periode
                                        )
                                        isDownloadingAll = false
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isDownloadingAll) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Download Semua",
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isDownloadingAll) "Mengunduh..." else "Download Foto Armada",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                }

                // TABLE FOTO KENDARAAN CONTENT
                if (vehiclePhotoItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ImageNotSupported,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Tidak Ada Foto Kendaraan",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Slate800
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Belum ada foto dokumentasi armada yang diunggah untuk No Mobil ${audit.noMobil}.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Slate500),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // TABEL FOTO KENDARAAN (Headers & Rows)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(12.dp)
                    ) {
                        // Table Header Row
                        Surface(
                            color = Blue800,
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "NO",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp),
                                    modifier = Modifier.width(32.dp),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "FOTO KENDARAAN",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp),
                                    modifier = Modifier.width(100.dp),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "TITIK DOKUMENTASI",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp),
                                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                                )
                                Text(
                                    text = "AKSI",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp),
                                    modifier = Modifier.width(50.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Table Body (LazyColumn of rows)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .border(BorderStroke(1.dp, Color(0xFFCBD5E1)), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                        ) {
                            itemsIndexed(vehiclePhotoItems) { index, photoItem ->
                                val slotNum = photoItem.slotIndex
                                val photoPath = photoItem.photoPath
                                val displayTitle = photoItem.title
                                val note = photoItem.note

                                val isEven = index % 2 == 0

                                Surface(
                                    color = if (isEven) Color.White else Color(0xFFF8FAFC),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Column 1: NO
                                        Box(
                                            modifier = Modifier.width(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Surface(
                                                color = HighDensityBlueLight,
                                                shape = CircleShape,
                                                modifier = Modifier.size(22.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "$slotNum",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Black,
                                                            color = HighDensityBlue,
                                                            fontSize = 10.sp
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        // Column 2: FOTO PREVIEW (Clickable for full screen zoom)
                                        Box(
                                            modifier = Modifier
                                                .width(100.dp)
                                                .height(75.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
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

                                            // Small Zoom Icon Badge
                                            Surface(
                                                color = Color.Black.copy(alpha = 0.55f),
                                                shape = RoundedCornerShape(topStart = 4.dp),
                                                modifier = Modifier.align(Alignment.BottomEnd)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ZoomIn,
                                                    contentDescription = "Perbesar",
                                                    tint = Color.White,
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .padding(2.dp)
                                                )
                                            }
                                        }

                                        // Column 3: TITIK DOKUMENTASI (Hanya label Dokumentasi Armada)
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = displayTitle,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate900,
                                                    fontSize = 11.5.sp
                                                ),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            // Label Tetap: Dokumentasi Armada
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
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            val customNote = if (note.isNotBlank() && !note.equals("Foto Kendaraan", ignoreCase = true)) note else ""
                                            if (customNote.isNotBlank()) {
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
                                                            text = customNote,
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                color = Slate800,
                                                                fontSize = 10.5.sp
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Column 4: AKSI (Hanya Zoom, Fitur Edit Dihilangkan)
                                        Box(
                                            modifier = Modifier.width(50.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    fullScreenPhotoUrl = Pair(photoPath, displayTitle)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ZoomIn,
                                                    contentDescription = "Zoom",
                                                    tint = HighDensityBlue,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }

                // Footer Close Button Bar
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Surface(
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Blue700,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
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
                    .background(Color.Black)
            ) {
                // Header Viewer
                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${audit.noMobil} • Dokumentasi Armada",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                            )
                        }

                        IconButton(
                            onClick = { fullScreenPhotoUrl = null },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Interactive Photo Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale > 1f) {
                                    val maxOffset = (scale - 1f) * 600f
                                    offset = Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxOffset, maxOffset),
                                        y = (offset.y + pan.y).coerceIn(-maxOffset, maxOffset)
                                    )
                                } else {
                                    offset = Offset.Zero
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(com.example.util.DriveUrlHelper.resolveImageModel(url))
                            .crossfade(true)
                            .build(),
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                }

                // Footer Zoom Controls
                Surface(
                    color = Color(0xFF1E293B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Zoom: ${(scale * 100).toInt()}%",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    scale = 1f
                                    offset = Offset.Zero
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                            ) {
                                Text("Reset Zoom", fontSize = 11.sp)
                            }
                            Button(
                                onClick = { fullScreenPhotoUrl = null },
                                colors = ButtonDefaults.buttonColors(containerColor = Blue700)
                            ) {
                                Text("Kembali", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
