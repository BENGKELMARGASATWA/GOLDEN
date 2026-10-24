package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InsertPhoto
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.local.entity.UserEntity
import com.example.ui.theme.Blue50
import com.example.ui.theme.Blue700
import com.example.ui.theme.Blue900
import com.example.ui.theme.HighDensityBlue
import com.example.ui.theme.HighDensityBlueDark
import com.example.ui.theme.HighDensityBlueLight
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate800
import com.example.ui.viewmodel.AppScreen

@Composable
fun AppDrawerContent(
    currentScreen: AppScreen,
    currentUser: UserEntity?,
    logoUrl: String,
    remainingHours: Long,
    onNavigate: (AppScreen) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val level = currentUser?.levelAkses?.trim()?.uppercase() ?: ""
    val isSuperAdmin = level == "SUPER ADMIN"
    val isAdmin = level == "ADMIN"
    val isKepalaBengkel = level == "KEPALA BENGKEL"
    val isMasterData = level == "MASTER DATA"
    val isSkt = level == "SKT"
    val isAuditor = level == "AUDITOR"

    val showInputAudit = isSuperAdmin || isAdmin || isSkt || isAuditor
    val showRiwayatAudit = true
    val showMonitoring = isSuperAdmin || isAdmin || isSkt
    val showPeringkatKpi = isSuperAdmin || isAdmin
    val showMasterData = isSuperAdmin || isAdmin || isMasterData
    val showGoogleScript = isSuperAdmin // HANYA SUPER ADMIN (ADMIN hilang Google Script & Sync)
    val showDownloadPhoto = isSuperAdmin || isAdmin || isMasterData
    val showMonitoringPc = isSuperAdmin || isAdmin || isSkt

    Surface(
        color = Color.White,
        modifier = modifier
            .fillMaxHeight()
            .width(285.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            // Sidebar Header (Sky Blue #87CEEB Theme)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF785900), Color(0xFF9E7700), HighDensityBlue)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (logoUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(logoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.goldenbird_logo_official),
                                    contentDescription = "Logo Golden Bird",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "AUDIT GOLDEN BIRD",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    letterSpacing = (-0.3).sp
                                )
                            )
                            Text(
                                text = "Sistem Audit Keindahan PT Blue bIrd Group",
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            )
                        }
                    }

                    if (currentUser != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(Color.White.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = currentUser.namaPetugas,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${currentUser.levelAkses} • Pool: ${currentUser.kodePool}",
                                        color = Color.White.copy(alpha = 0.8f),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Menu Items List
            Column(modifier = Modifier.padding(vertical = 12.dp)) {

                // KEPALA BENGKEL ONLY MENU
                if (isKepalaBengkel) {
                    MenuSectionHeader(title = "MENU KEPALA BENGKEL")

                    DrawerMenuItem(
                        icon = Icons.Default.History,
                        label = "RIWAYAT AUDIT (${currentUser?.kodePool ?: "POOL"})",
                        isSelected = currentScreen == AppScreen.RIWAYAT_SERVICE,
                        onClick = { onNavigate(AppScreen.RIWAYAT_SERVICE) }
                    )

                    DrawerMenuItem(
                        icon = Icons.Default.Summarize,
                        label = "RESUME",
                        isSelected = currentScreen == AppScreen.RESUME_AUDIT,
                        onClick = { onNavigate(AppScreen.RESUME_AUDIT) }
                    )

                    DrawerMenuItem(
                        icon = Icons.Default.Tune,
                        label = "BOBOT RESUME",
                        isSelected = currentScreen == AppScreen.BOBOT_RESUME,
                        onClick = { onNavigate(AppScreen.BOBOT_RESUME) }
                    )

                    if (isSuperAdmin) {
                        DrawerMenuItem(
                            icon = Icons.Default.TableChart,
                            label = "REKAP NILAI",
                            isSelected = currentScreen == AppScreen.REKAP_NILAI,
                            onClick = { onNavigate(AppScreen.REKAP_NILAI) }
                        )
                    }

                    DrawerMenuItem(
                        icon = Icons.Default.Assessment,
                        label = "REKAP TEMUAN",
                        isSelected = currentScreen == AppScreen.REKAP_TEMUAN,
                        onClick = { onNavigate(AppScreen.REKAP_TEMUAN) }
                    )
                } else {
                    // AUDITOR / OPERATIONAL SECTION
                    if (showInputAudit || (showRiwayatAudit && !isMasterData)) {
                        MenuSectionHeader(title = if (isSkt) "MENU AUDIT SKT" else "MENU AUDIT")

                        if (showInputAudit) {
                            DrawerMenuItem(
                                icon = Icons.Default.EditNote,
                                label = "INPUT AUDIT",
                                laysOutIndented = false,
                                isSelected = currentScreen == AppScreen.INPUT_AUDIT,
                                onClick = { onNavigate(AppScreen.INPUT_AUDIT) }
                            )
                        }

                        if (showRiwayatAudit) {
                            DrawerMenuItem(
                                icon = Icons.Default.History,
                                label = "RIWAYAT AUDIT",
                                laysOutIndented = false,
                                isSelected = currentScreen == AppScreen.RIWAYAT_SERVICE,
                                onClick = { onNavigate(AppScreen.RIWAYAT_SERVICE) }
                            )

                            DrawerMenuItem(
                                icon = Icons.Default.Summarize,
                                label = "RESUME",
                                laysOutIndented = false,
                                isSelected = currentScreen == AppScreen.RESUME_AUDIT,
                                onClick = { onNavigate(AppScreen.RESUME_AUDIT) }
                            )

                            DrawerMenuItem(
                                icon = Icons.Default.Tune,
                                label = "BOBOT RESUME",
                                laysOutIndented = false,
                                isSelected = currentScreen == AppScreen.BOBOT_RESUME,
                                onClick = { onNavigate(AppScreen.BOBOT_RESUME) }
                            )

                            if (isSuperAdmin) {
                                DrawerMenuItem(
                                    icon = Icons.Default.TableChart,
                                    label = "REKAP NILAI",
                                    laysOutIndented = false,
                                    isSelected = currentScreen == AppScreen.REKAP_NILAI,
                                    onClick = { onNavigate(AppScreen.REKAP_NILAI) }
                                )
                            }

                            DrawerMenuItem(
                                icon = Icons.Default.Assessment,
                                label = "REKAP TEMUAN",
                                laysOutIndented = false,
                                isSelected = currentScreen == AppScreen.REKAP_TEMUAN,
                                onClick = { onNavigate(AppScreen.REKAP_TEMUAN) }
                            )

                            if (showPeringkatKpi) {
                                DrawerMenuItem(
                                    icon = Icons.Default.Leaderboard,
                                    label = "PERINGKAT KPI",
                                    laysOutIndented = false,
                                    isSelected = currentScreen == AppScreen.PERINGKAT_KPI,
                                    onClick = { onNavigate(AppScreen.PERINGKAT_KPI) }
                                )
                            }
                        }
                    }

                    // MONITORING ALL POOL SECTION (SUPER ADMIN, ADMIN, SKT)
                    if (showMonitoring) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), color = Color(0xFFE2E8F0))
                        MenuSectionHeader(title = "MONITORING POOL")

                        DrawerMenuItem(
                            icon = Icons.Default.Speed,
                            label = "MONITORING ALL POOL",
                            laysOutIndented = false,
                            isSelected = currentScreen == AppScreen.MONITORING_ALL_POOL,
                            onClick = { onNavigate(AppScreen.MONITORING_ALL_POOL) }
                        )

                        DrawerMenuItem(
                            icon = Icons.Default.FilterList,
                            label = "MONITORING AUDIT",
                            laysOutIndented = false,
                            isSelected = currentScreen == AppScreen.MONITORING_AUDIT,
                            onClick = { onNavigate(AppScreen.MONITORING_AUDIT) }
                        )

                        DrawerMenuItem(
                            icon = Icons.Default.BarChart,
                            label = "HASIL AUDIT ALL POOL",
                            laysOutIndented = false,
                            isSelected = currentScreen == AppScreen.HASIL_AUDIT_ALL_POOL,
                            onClick = { onNavigate(AppScreen.HASIL_AUDIT_ALL_POOL) }
                        )
                    }

                    // MASTER SETTINGS SECTION (MASTER KENDARAAN: Semua level dapat melihat)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), color = Color(0xFFE2E8F0))
                    MenuSectionHeader(title = "PENGATURAN MASTER")

                    DrawerMenuItem(
                        icon = Icons.Default.DirectionsCar,
                        label = "MASTER KENDARAAN",
                        laysOutIndented = false,
                        isSelected = currentScreen == AppScreen.MASTER_KENDARAAN,
                        onClick = { onNavigate(AppScreen.MASTER_KENDARAAN) }
                    )

                    if (showMasterData) {
                        DrawerMenuItem(
                            icon = Icons.Default.LocationOn,
                            label = "MASTER POOL",
                            laysOutIndented = false,
                            isSelected = currentScreen == AppScreen.MASTER_POOL,
                            onClick = { onNavigate(AppScreen.MASTER_POOL) }
                        )

                        DrawerMenuItem(
                            icon = Icons.Default.ManageAccounts,
                            label = "MASTER USER",
                            laysOutIndented = false,
                            isSelected = currentScreen == AppScreen.MASTER_USER,
                            onClick = { onNavigate(AppScreen.MASTER_USER) }
                        )

                        DrawerMenuItem(
                            icon = Icons.Default.CalendarMonth,
                            label = "PERIODE",
                            laysOutIndented = false,
                            isSelected = currentScreen == AppScreen.MASTER_PERIODE,
                            onClick = { onNavigate(AppScreen.MASTER_PERIODE) }
                        )

                        // BOBOT: SUPER ADMIN & ADMIN
                        if (isSuperAdmin || isAdmin) {
                            DrawerMenuItem(
                                icon = Icons.Default.Tune,
                                label = "BOBOT",
                                laysOutIndented = false,
                                isSelected = currentScreen == AppScreen.SETTING_BOBOT,
                                onClick = { onNavigate(AppScreen.SETTING_BOBOT) }
                            )

                            // SAMPLE: SUPER ADMIN & ADMIN
                            DrawerMenuItem(
                                icon = Icons.Default.Pin,
                                label = "SAMPLE",
                                laysOutIndented = false,
                                isSelected = currentScreen == AppScreen.SAMPLE,
                                onClick = { onNavigate(AppScreen.SAMPLE) }
                            )
                        }

                        // GRADE KPI: KHUSUS SUPER ADMIN
                        if (isSuperAdmin) {
                            DrawerMenuItem(
                                icon = Icons.Default.Grade,
                                label = "GRADE KPI",
                                laysOutIndented = false,
                                isSelected = currentScreen == AppScreen.GRADE_KPI,
                                onClick = { onNavigate(AppScreen.GRADE_KPI) }
                            )
                        }

                        // GOOGLE SCRIPT & SYNC: ONLY SUPER ADMIN
                        if (showGoogleScript) {
                            DrawerMenuItem(
                                icon = Icons.Default.CloudSync,
                                label = "GOOGLE SCRIPT & SYNC",
                                laysOutIndented = false,
                                isSelected = currentScreen == AppScreen.GOOGLE_SCRIPT_CONFIG,
                                onClick = { onNavigate(AppScreen.GOOGLE_SCRIPT_CONFIG) }
                            )
                        }

                        if (isMasterData) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), color = Color(0xFFE2E8F0))
                            MenuSectionHeader(title = "LOGS")

                            DrawerMenuItem(
                                icon = Icons.Default.History,
                                label = "RIWAYAT AUDIT",
                                laysOutIndented = false,
                                isSelected = currentScreen == AppScreen.RIWAYAT_SERVICE,
                                onClick = { onNavigate(AppScreen.RIWAYAT_SERVICE) }
                            )

                            DrawerMenuItem(
                                icon = Icons.Default.Summarize,
                                label = "RESUME",
                                laysOutIndented = false,
                                isSelected = currentScreen == AppScreen.RESUME_AUDIT,
                                onClick = { onNavigate(AppScreen.RESUME_AUDIT) }
                            )

                            DrawerMenuItem(
                                icon = Icons.Default.Tune,
                                label = "BOBOT RESUME",
                                laysOutIndented = false,
                                isSelected = currentScreen == AppScreen.BOBOT_RESUME,
                                onClick = { onNavigate(AppScreen.BOBOT_RESUME) }
                            )

                            if (isSuperAdmin) {
                                DrawerMenuItem(
                                    icon = Icons.Default.TableChart,
                                    label = "REKAP NILAI",
                                    laysOutIndented = false,
                                    isSelected = currentScreen == AppScreen.REKAP_NILAI,
                                    onClick = { onNavigate(AppScreen.REKAP_NILAI) }
                                )
                            }

                            DrawerMenuItem(
                                icon = Icons.Default.Assessment,
                                label = "REKAP TEMUAN",
                                laysOutIndented = false,
                                isSelected = currentScreen == AppScreen.REKAP_TEMUAN,
                                onClick = { onNavigate(AppScreen.REKAP_TEMUAN) }
                            )
                        }
                    }
                }

                // DOWNLOAD FOTO AUDIT (MASTER DATA, ADMIN, SUPER ADMIN)
                if (showDownloadPhoto) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), color = Color(0xFFE2E8F0))
                    MenuSectionHeader(title = "DOKUMENTASI FOTO")

                    DrawerMenuItem(
                        icon = Icons.Default.CloudDownload,
                        label = "DOWNLOAD FOTO AUDIT",
                        laysOutIndented = false,
                        isSelected = currentScreen == AppScreen.DOWNLOAD_PHOTO_AUDIT,
                        onClick = { onNavigate(AppScreen.DOWNLOAD_PHOTO_AUDIT) }
                    )
                }

                // WEB PORTAL PC REALTIME (SUPER ADMIN, ADMIN, KEPALA BENGKEL, MASTER DATA, SKT)
                if (showMonitoringPc) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), color = Color(0xFFE2E8F0))
                    MenuSectionHeader(title = "MONITORING PC")

                    DrawerMenuItem(
                        icon = Icons.Default.DesktopWindows,
                        label = "WEB PC REALTIME",
                        laysOutIndented = false,
                        isSelected = currentScreen == AppScreen.WEB_SERVER_PC,
                        onClick = { onNavigate(AppScreen.WEB_SERVER_PC) }
                    )
                }

                // Tombol Keluar Aplikasi langsung di bawah menu paling bawah
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp), color = Color(0xFFE2E8F0))

                Surface(
                    color = Color(0xFFFFF1F2),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { onLogout() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Keluar",
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Keluar Aplikasi",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE11D48)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "V1.01 2026 Persembahan SKT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = Slate400,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun MenuSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = Slate400,
            fontSize = 11.sp
        ),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    laysOutIndented: Boolean = false,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) HighDensityBlueLight else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) HighDensityBlueDark else Slate600,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) HighDensityBlueDark else Slate800,
                    fontSize = 13.sp
                )
            )
        }
    }
}
