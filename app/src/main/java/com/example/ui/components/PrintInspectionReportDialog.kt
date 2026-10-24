package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.DataAuditEntity
import com.example.data.local.entity.PengaturanNamaPhotoEntity
import com.example.ui.theme.Blue700
import com.example.ui.theme.Blue900
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.util.InspectionPdfReportHelper
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun PrintInspectionReportDialog(
    audit: DataAuditEntity,
    photoTitles: List<PengaturanNamaPhotoEntity> = emptyList(),
    onDismiss: () -> Unit
) {
    DownloadInspectionReportDialog(
        audit = audit,
        photoTitles = photoTitles,
        onDismiss = onDismiss
    )
}

@Composable
fun DownloadInspectionReportDialog(
    audit: DataAuditEntity,
    photoTitles: List<PengaturanNamaPhotoEntity> = emptyList(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isGenerating by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var savedDownloadUri by remember { mutableStateOf<Uri?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isDownloadSuccess by remember { mutableStateOf(false) }

    val reportData = remember(audit) {
        InspectionPdfReportHelper.prepareReportData(audit, photoTitles)
    }

    // Fungsi untuk mengunduh PDF secara otomatis / manual
    fun startDownload() {
        coroutineScope.launch {
            isGenerating = true
            errorMessage = null
            isDownloadSuccess = false
            try {
                val targetFile = generatedPdfFile ?: run {
                    val res = InspectionPdfReportHelper.generatePdf(context, reportData)
                    res.getOrThrow().also { generatedPdfFile = it }
                }

                val savedUri = InspectionPdfReportHelper.saveToPublicDownloads(
                    context = context,
                    srcFile = targetFile,
                    displayName = targetFile.name
                )

                if (savedUri != null) {
                    savedDownloadUri = savedUri
                    isDownloadSuccess = true
                    Toast.makeText(
                        context,
                        "Laporan PDF berhasil diunduh ke folder Downloads!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // Jika saveToPublicDownloads null (misal permission), file tetap ada di cache
                    isDownloadSuccess = true
                    Toast.makeText(
                        context,
                        "Laporan PDF berhasil dibuat!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Gagal membuat dan mengunduh laporan PDF"
            } finally {
                isGenerating = false
            }
        }
    }

    // Unduh otomatis begitu dialog dibuka
    LaunchedEffect(audit.id) {
        startDownload()
    }

    Dialog(
        onDismissRequest = {
            if (!isGenerating) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = Blue700,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Download Laporan PDF",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900,
                                    fontSize = 17.sp
                                )
                            )
                            Text(
                                text = "Laporan Inspeksi Fisik Armada",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Slate600,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isGenerating,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Slate400
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Ringkasan Data Kendaraan & Temuan
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Mobil: ${audit.noMobil}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Blue900,
                                        fontSize = 15.sp
                                    )
                                )
                                Text(
                                    text = "Plat: ${audit.platNomer}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate700,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            // Badge Temuan
                            val hasTemuan = audit.totalPoinTemuan > 0
                            Surface(
                                color = if (hasTemuan) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (hasTemuan) Color(0xFFFECACA) else Color(0xFFBBF7D0))
                            ) {
                                Text(
                                    text = if (hasTemuan) "${audit.totalPoinTemuan} Poin Temuan" else "Lolos Standar (0 Pt)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasTemuan) Color(0xFFDC2626) else Color(0xFF15803D),
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pool: ${audit.pool} • ${audit.periode}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Slate600,
                                    fontSize = 11.5.sp
                                )
                            )
                            Text(
                                text = "Auditor: ${audit.namaPetugas}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Slate600,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Konten Status Download
                if (isGenerating) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Blue700,
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.5.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Menyiapkan & Mengunduh File PDF...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Slate900,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Menyusun catatan temuan & foto inspeksi armada",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Slate600,
                                fontSize = 11.5.sp
                            )
                        )
                    }
                } else if (isDownloadSuccess) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0FDF4), RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Color(0xFFBBF7D0)), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Laporan PDF Berhasil Diunduh!",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF15803D),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tersimpan di folder Unduhan (Downloads)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF166534),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                        generatedPdfFile?.let { file ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF15803D),
                                    fontSize = 10.5.sp
                                )
                            )
                        }
                    }
                } else if (errorMessage != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Color(0xFFFECACA)), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gagal Mengunduh Laporan",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFFB91C1C),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF991B1B),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Tombol Aksi
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isDownloadSuccess) {
                        // 1. Tombol BUKA FILE PDF
                        Button(
                            onClick = {
                                generatedPdfFile?.let { file ->
                                    InspectionPdfReportHelper.shareOrOpenPdf(
                                        context = context,
                                        pdfFile = file,
                                        isShareOnly = false
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.HighDensityBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BUKA FILE PDF",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            )
                        }

                        // 2. Tombol BAGIKAN PDF
                        OutlinedButton(
                            onClick = {
                                generatedPdfFile?.let { file ->
                                    InspectionPdfReportHelper.shareOrOpenPdf(
                                        context = context,
                                        pdfFile = file,
                                        isShareOnly = true
                                    )
                                }
                            },
                            border = BorderStroke(1.dp, Blue700),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Blue700,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Bagikan PDF (WhatsApp / Email)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Blue700,
                                    fontSize = 12.5.sp
                                )
                            )
                        }

                        // 3. Tombol Selesai / Tutup
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text(
                                text = "Selesai",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Slate600,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    } else if (errorMessage != null) {
                        // Tombol Coba Unduh Lagi
                        Button(
                            onClick = { startDownload() },
                            colors = ButtonDefaults.buttonColors(containerColor = Blue700),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "COBA UNDUH LAGI",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            )
                        }

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text(
                                text = "Tutup",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Slate600
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
