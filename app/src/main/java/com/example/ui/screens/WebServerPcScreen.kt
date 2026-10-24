package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.theme.Blue50
import com.example.ui.theme.HighDensityBlue
import com.example.ui.theme.HighDensityBlueDark
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.AuditViewModel
import java.io.File

@Composable
fun WebServerPcScreen(
    viewModel: AuditViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isServerRunning by viewModel.isWebServerRunning.collectAsState()
    val serverUrl by viewModel.webServerUrl.collectAsState()
    val totalAudits by viewModel.allAudits.collectAsState()
    val availableIps by viewModel.availableIps.collectAsState()
    val isEmulator by viewModel.isEmulatorEnvironment.collectAsState()
    val currentPort by viewModel.currentServerPort.collectAsState()

    var isExportingHtml by remember { mutableStateOf(false) }
    var isExportingExcel by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = HighDensityBlue),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DesktopWindows,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WEB DASHBOARD PC REALTIME",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 17.sp,
                            letterSpacing = (-0.3).sp
                        )
                    )
                    Text(
                        text = "Input data audit langsung dari PC, pantau hasil monitoring, dan tabel resume matriks di browser PC/Laptop secara realtime",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    )
                }
            }
        }

        // EMULATOR / CLOUD WARNING BANNER (JIKA TERDETEKSI 10.0.2.15)
        if (isEmulator || serverUrl.contains("10.0.2.15") || serverUrl.contains("127.0.0.1")) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFDE68A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PENTING: TERDETEKSI EMULATOR CLOUD (IP 10.0.2.15)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF92400E),
                                    fontSize = 13.sp
                                )
                            )
                            Text(
                                text = "Alamat 10.0.2.15 adalah IP internal emulator cloud di server Google, sehingga browser PC fisik Anda tidak dapat menjangkaunya secara langsung via kabel/WiFi.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF78350F),
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFFDE68A))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "SOLUSI AGAR DAPAT DIBUKA DI PC ANDA:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. Gunakan tombol 'Ekspor & Bagikan File Web HTML' di bawah untuk membuka dashboard langsung di PC Anda tanpa perlu server IP.\n2. Jika nanti di-install di HP fisik Android, gunakan Hotspot HP atau pastikan HP dan PC berada di Wi-Fi yang sama.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF78350F),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    )
                }
            }
        }

        // FITUR UTAMA: EKSPOR FILE WEB HTML STANDALONE (PASTI BISA DIBUKA DI PC MANAPUN!)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFBFDBFE)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Blue50),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = HighDensityBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "METODE 1: FILE WEB DASHBOARD (.HTML)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = Slate900,
                                fontSize = 14.sp
                            )
                        )
                        Text(
                            text = "Paling Mudah & Pasti Berhasil • Buka di PC tanpa kendala IP/Wi-Fi",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF16A34A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Aplikasi akan membuat satu file web mandiri (.html) yang sudah berisi seluruh data audit, tabel resume 10 mobil, dan foto inspeksi. Kirim file ini ke PC (via WhatsApp Web, Email, atau Drive) lalu klik dua kali untuk membuka di browser PC Anda.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Slate600,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        isExportingHtml = true
                        viewModel.exportStandaloneHtmlReport(
                            onSuccess = { file ->
                                isExportingHtml = false
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/html"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "Dashboard Audit Armada Bluebird")
                                        putExtra(Intent.EXTRA_TEXT, "Berikut file Dashboard Laporan Audit Armada untuk dibuka di PC.")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Bagikan File Dashboard HTML ke PC"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Gagal membagikan file: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            onError = { err ->
                                isExportingHtml = false
                                Toast.makeText(context, "Gagal membuat file: $err", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_export_html_pc"),
                    enabled = !isExportingHtml
                ) {
                    Icon(
                        imageVector = if (isExportingHtml) Icons.Default.Refresh else Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isExportingHtml) "Sedang Membuat File Web..." else "Ekspor & Bagikan File Web HTML ke PC",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        isExportingExcel = true
                        viewModel.exportStandaloneExcelReport(
                            onSuccess = { file ->
                                isExportingExcel = false
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/vnd.ms-excel"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "Laporan Audit Armada Bluebird (Excel)")
                                        putExtra(Intent.EXTRA_TEXT, "Berikut file Laporan Audit Armada format Excel (.xls).")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Bagikan / Buka File Excel di PC"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Gagal membagikan file: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            onError = { err ->
                                isExportingExcel = false
                                Toast.makeText(context, "Gagal membuat file Excel: $err", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_export_excel_pc"),
                    enabled = !isExportingExcel
                ) {
                    Icon(
                        imageVector = if (isExportingExcel) Icons.Default.Refresh else Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isExportingExcel) "Sedang Membuat File Excel..." else "Download & Bagikan File Excel (.XLS) ke PC",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }

        // METODE 2: SERVER LOKAL WI-FI / HOTSPOT
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "METODE 2: SERVER LOKAL WI-FI / HOTSPOT (REALTIME)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate700,
                        letterSpacing = 0.5.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isServerRunning) Color(0xFF16A34A) else Color(0xFF94A3B8))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isServerRunning) "SERVER AKTIF (ONLINE)" else "SERVER NONAKTIF (OFFLINE)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isServerRunning) Color(0xFF16A34A) else Slate600
                                )
                            )
                        }
                        Text(
                            text = if (isServerRunning) "Menyiarkan data audit ke browser PC" else "Tekan tombol switch untuk menyalakan",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate400, fontSize = 11.5.sp)
                        )
                    }

                    Switch(
                        checked = isServerRunning,
                        onCheckedChange = { start ->
                            if (start) {
                                viewModel.startWebServer()
                            } else {
                                viewModel.stopWebServer()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = HighDensityBlue
                        ),
                        modifier = Modifier.testTag("switch_web_server")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(16.dp))

                // URL Display Box
                Text(
                    text = "ALAMAT URL UNTUK DIBUKA DI BROWSER PC:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Blue50,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isServerRunning) serverUrl else "http://[IP_HP_ANDA]:$currentPort",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = if (isServerRunning) HighDensityBlueDark else Slate400
                                )
                            )
                            Text(
                                text = "Port: $currentPort • ${if (isServerRunning) "Siap diakses" else "Server nonaktif"}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = Slate600)
                            )
                        }

                        if (isServerRunning) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("URL Web Report", serverUrl)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "URL berhasil disalin: $serverUrl", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HighDensityBlue),
                                modifier = Modifier.testTag("btn_copy_url")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Salin", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }

                // Action buttons: Open in browser
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (isServerRunning) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(serverUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Gagal membuka browser: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Nyalakan server terlebih dahulu", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_open_browser"),
                        shape = RoundedCornerShape(8.dp),
                        enabled = isServerRunning
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buka di Browser", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.refreshWebServerAddress()
                            Toast.makeText(context, "Alamat IP diperbarui", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_refresh_ip"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Perbarui IP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // IP Interface Selector (jika terdeteksi lebih dari 1 IP)
                if (availableIps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "PILIH ANTARMUKA IP JARINGAN:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        availableIps.forEach { ipInfo ->
                            val isSelected = serverUrl.contains(ipInfo.ip)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Blue50 else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) HighDensityBlue else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectServerIp(ipInfo.ip)
                                        Toast.makeText(context, "IP diubah ke ${ipInfo.ip}", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ipInfo.label,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) HighDensityBlueDark else Slate800,
                                                fontSize = 12.sp
                                            )
                                        )
                                        Text(
                                            text = "http://${ipInfo.ip}:$currentPort",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                color = Slate400,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = HighDensityBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Port Selector (8080, 8888, 5000)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "PILIHAN PORT SERVER:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(8080, 8888, 5000).forEach { port ->
                            FilterChip(
                                selected = currentPort == port,
                                onClick = {
                                    if (currentPort != port) {
                                        viewModel.changeServerPort(port)
                                    }
                                },
                                label = { Text(port.toString(), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HighDensityBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // TROUBLESHOOTING CARD: KENAPA WEB TIDAK BISA DIBUKA DI PC?
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = HighDensityBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "KENAPA WEB TIDAK BISA DIBUKA DI PC? (SOLUSI)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = Slate900,
                            letterSpacing = (-0.2).sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                InstructionStepItem(
                    stepNumber = "★",
                    title = "SOLUSI 1 (100% Berhasil): Gunakan Hotspot HP",
                    desc = "Nyalakan 'Hotspot Pribadi / Tethering' di HP Android Anda. Sambungkan Wi-Fi laptop/PC ke Hotspot HP tersebut. Buka alamat URL yang tertera (biasanya http://192.168.43.1:8080). Cara ini bebas dari firewall router kantor dan pasti terhubung langsung!"
                )

                Spacer(modifier = Modifier.height(12.dp))

                InstructionStepItem(
                    stepNumber = "2",
                    title = "Pastikan Bukan Menggunakan Paket Data Seluler di HP",
                    desc = "Jika HP menggunakan data seluler 4G/5G dan PC memakai Wi-Fi rumah, HP dan PC tidak saling terhubung. HP wajib terhubung ke Wi-Fi yang sama persis dengan PC."
                )

                Spacer(modifier = Modifier.height(12.dp))

                InstructionStepItem(
                    stepNumber = "3",
                    title = "Periksa Firewall Windows di PC",
                    desc = "Windows Firewall terkadang memblokir koneksi masuk ke port 8080. Jika gagal, coba ganti pilihan port di atas ke 8888 atau 5000."
                )

                Spacer(modifier = Modifier.height(12.dp))

                InstructionStepItem(
                    stepNumber = "4",
                    title = "Gunakan Metode 1 (Ekspor File Web HTML)",
                    desc = "Jika jaringan kantor memblokir komunikasi antar perangkat (Client Isolation), gunakan tombol 'Ekspor & Bagikan File Web HTML' di atas. File web ini bisa dibuka di PC kapan pun tanpa perlu koneksi jaringan lokal!"
                )
            }
        }

        // Stats Box
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF1F5F9),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Data Audit Siap Ditampilkan:",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate700, fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "${totalAudits.size} Laporan Armada",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = HighDensityBlue,
                        fontWeight = FontWeight.Black
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun InstructionStepItem(
    stepNumber: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Blue50)
                .border(1.5.dp, Color(0xFFBFDBFE), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = HighDensityBlue,
                    fontSize = 12.sp
                )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Slate800,
                    fontSize = 13.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Slate600,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
            )
        }
    }
}

@Composable
private fun FeatureCheckItem(
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF16A34A),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Slate800,
                    fontSize = 12.5.sp
                )
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Slate600,
                    fontSize = 11.5.sp
                )
            )
        }
    }
}
