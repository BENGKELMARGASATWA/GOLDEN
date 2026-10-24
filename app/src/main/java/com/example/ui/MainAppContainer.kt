package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.DataAuditEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.components.AppDrawerContent
import com.example.ui.components.AppTopBar
import com.example.ui.screens.AuditInputScreen
import com.example.ui.screens.BobotResumeScreen
import com.example.ui.screens.DownloadPhotoAuditScreen
import com.example.ui.screens.GoogleScriptSetupScreen
import com.example.ui.screens.GradeKpiScreen
import com.example.ui.screens.HasilAuditScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MasterBobotScreen
import com.example.ui.screens.MasterKendaraanScreen
import com.example.ui.screens.MasterPeriodeScreen
import com.example.ui.screens.MasterPoolScreen
import com.example.ui.screens.MasterUserScreen
import com.example.ui.screens.MonitoringAuditScreen
import com.example.ui.screens.MonitoringPoolScreen
import com.example.ui.screens.PengaturanFotoScreen
import com.example.ui.screens.RekapNilaiSubGroupScreen
import com.example.ui.screens.RekapTemuanScreen
import com.example.ui.screens.RiwayatServiceScreen
import com.example.util.AuditFilterUtils
import com.example.ui.screens.SampleScreen
import com.example.ui.screens.WebServerPcScreen
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AuditViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun MainAppContainer(
    viewModel: AuditViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val formState by viewModel.auditForm.collectAsState()

    val pools by viewModel.allPools.collectAsState()
    val kendaraanList by viewModel.allKendaraan.collectAsState()
    val users by viewModel.allUsers.collectAsState()
    val audits by viewModel.allAudits.collectAsState()
    val photoTitles by viewModel.photoSettings.collectAsState()
    val periodes by viewModel.allPeriode.collectAsState()
    val bobotList by viewModel.allBobot.collectAsState()
    val sampleList by viewModel.allSamples.collectAsState()
    val gradeList by viewModel.allGradeKpi.collectAsState()

    val isReconciling by viewModel.isReconciling.collectAsState()
    val lastReconcileTime by viewModel.lastReconcileTime.collectAsState()
    val lastReconcileSummary by viewModel.lastReconcileSummary.collectAsState()
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsState()
    val autoSyncIntervalMinutes by viewModel.autoSyncIntervalMinutes.collectAsState()
    val isSyncingKendaraan by viewModel.isSyncingKendaraan.collectAsState()
    val syncKendaraanProgress by viewModel.syncKendaraanProgress.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val sessionManager = viewModel.repository.sessionManager
    val remainingHours = sessionManager.getRemainingSessionHours()
    val logoUrl = sessionManager.logoUrl

    // Listen to ViewModel toast/snackbar events
    LaunchedEffect(Unit) {
        viewModel.eventMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (currentScreen == AppScreen.LOGIN || currentUser == null) {
        LoginScreen(
            logoUrl = logoUrl,
            onLogin = { user, pass, callback ->
                viewModel.login(user, pass, callback)
            },
            onRefreshUsersInBackground = {
                viewModel.downloadAllUsersInBackground()
            }
        )
    } else {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent()
                            viewModel.recordActivity()
                        }
                    }
                }
        ) {
            val isExpandedScreen = maxWidth >= 720.dp

            val title = when (currentScreen) {
                AppScreen.INPUT_AUDIT -> if (formState.isEditMode) "Edit Data Audit" else "Input Audit Keindahan"
                AppScreen.RIWAYAT_SERVICE -> if (currentUser?.levelAkses.equals("KEPALA BENGKEL", ignoreCase = true)) "Riwayat Audit Pool ${currentUser?.kodePool}" else "Riwayat Audit Harian"
                AppScreen.RESUME_AUDIT -> if (currentUser?.levelAkses.equals("KEPALA BENGKEL", ignoreCase = true)) "Resume Audit Pool ${currentUser?.kodePool}" else "Resume Audit Keindahan"
                AppScreen.MONITORING_ALL_POOL -> "Monitoring ALL POOL"
                AppScreen.MONITORING_AUDIT -> "Monitoring Audit"
                AppScreen.HASIL_AUDIT_ALL_POOL -> "Hasil Audit ALL POOL"
                AppScreen.MASTER_KENDARAAN -> "Master Kendaraan"
                AppScreen.MASTER_POOL -> "Master Pool"
                AppScreen.MASTER_USER -> "Master User"
                AppScreen.MASTER_PERIODE -> "Master Periode"
                AppScreen.SETTING_BOBOT -> "BOBOT"
                AppScreen.SAMPLE -> "SAMPLE"
                AppScreen.GRADE_KPI -> "GRADE KPI"
                AppScreen.PENGATURAN_NAMAPHOTO -> "Pengaturan Nama Foto"
                AppScreen.DOWNLOAD_PHOTO_AUDIT -> "Download Foto Hasil Input"
                AppScreen.BOBOT_RESUME -> "BOBOT RESUME"
                AppScreen.REKAP_NILAI -> "REKAP NILAI AUDIT KEINDAHAN"
                AppScreen.REKAP_TEMUAN -> "REKAP TEMUAN"
                AppScreen.PERINGKAT_KPI -> "PERINGKAT KPI"
                AppScreen.GOOGLE_SCRIPT_CONFIG -> "Integrasi Google Script"
                AppScreen.WEB_SERVER_PC -> "Web PC Realtime"
                else -> "AUDIT GOLDEN BIRD"
            }

            if (isExpandedScreen) {
                // PC / Tablet Layout (Permanent white sidebar at left)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(com.example.ui.theme.HighDensityBg)
                ) {
                    AppDrawerContent(
                        currentScreen = currentScreen,
                        currentUser = currentUser,
                        logoUrl = logoUrl,
                        remainingHours = remainingHours,
                        onNavigate = { screen -> viewModel.navigateTo(screen) },
                        onLogout = { viewModel.logout() }
                    )

                    Scaffold(
                        topBar = {
                            AppTopBar(
                                title = title,
                                currentUser = currentUser,
                                logoUrl = logoUrl,
                                onMenuClick = { /* No-op in PC mode */ },
                                onSyncClick = { viewModel.reconcileDataWithSheets() },
                                isReconciling = isReconciling,
                                lastReconcileTime = lastReconcileTime,
                                autoSyncEnabled = autoSyncEnabled
                            )
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        containerColor = com.example.ui.theme.HighDensityBg
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            ScreenRouter(
                                currentScreen = currentScreen,
                                viewModel = viewModel,
                                currentUser = currentUser,
                                formState = formState,
                                pools = pools,
                                kendaraanList = kendaraanList,
                                users = users,
                                audits = audits,
                                photoTitles = photoTitles,
                                periodes = periodes,
                                bobotList = bobotList,
                                sampleList = sampleList,
                                gradeList = gradeList,
                                isSyncingKendaraan = isSyncingKendaraan,
                                syncKendaraanProgress = syncKendaraanProgress
                            )
                        }
                    }
                }
            } else {
                // Mobile Layout (Swipeable Modal Navigation Drawer with white background)
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        AppDrawerContent(
                            currentScreen = currentScreen,
                            currentUser = currentUser,
                            logoUrl = logoUrl,
                            remainingHours = remainingHours,
                            onNavigate = { screen ->
                                viewModel.navigateTo(screen)
                                coroutineScope.launch { drawerState.close() }
                            },
                            onLogout = {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.logout()
                            }
                        )
                    }
                ) {
                    Scaffold(
                        topBar = {
                            AppTopBar(
                                title = title,
                                currentUser = currentUser,
                                logoUrl = logoUrl,
                                onMenuClick = {
                                    coroutineScope.launch { drawerState.open() }
                                },
                                onSyncClick = { viewModel.reconcileDataWithSheets() },
                                isReconciling = isReconciling,
                                lastReconcileTime = lastReconcileTime,
                                autoSyncEnabled = autoSyncEnabled
                            )
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        containerColor = com.example.ui.theme.HighDensityBg
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            ScreenRouter(
                                currentScreen = currentScreen,
                                viewModel = viewModel,
                                currentUser = currentUser,
                                formState = formState,
                                pools = pools,
                                kendaraanList = kendaraanList,
                                users = users,
                                audits = audits,
                                photoTitles = photoTitles,
                                periodes = periodes,
                                bobotList = bobotList,
                                sampleList = sampleList,
                                gradeList = gradeList,
                                isSyncingKendaraan = isSyncingKendaraan,
                                syncKendaraanProgress = syncKendaraanProgress
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenRouter(
    currentScreen: AppScreen,
    viewModel: AuditViewModel,
    currentUser: com.example.data.local.entity.UserEntity?,
    formState: com.example.ui.viewmodel.AuditFormState,
    pools: List<com.example.data.local.entity.MasterPoolEntity>,
    kendaraanList: List<com.example.data.local.entity.MasterKendaraanEntity>,
    users: List<com.example.data.local.entity.UserEntity>,
    audits: List<com.example.data.local.entity.DataAuditEntity>,
    photoTitles: List<com.example.data.local.entity.PengaturanNamaPhotoEntity>,
    periodes: List<com.example.data.local.entity.MasterPeriodeEntity>,
    bobotList: List<com.example.data.local.entity.MasterBobotEntity>,
    sampleList: List<com.example.data.local.entity.MasterSampleEntity> = emptyList(),
    gradeList: List<com.example.data.local.entity.MasterGradeKpiEntity> = emptyList(),
    isSyncingKendaraan: Boolean = false,
    syncKendaraanProgress: String = ""
) {
    val isSuperAdmin = currentUser?.levelAkses?.trim()?.equals("SUPER ADMIN", ignoreCase = true) == true
    when (currentScreen) {
        AppScreen.INPUT_AUDIT -> {
            val userAudits = remember(audits, currentUser) {
                filterAuditsForUser(audits, currentUser)
            }
            AuditInputScreen(
                formState = formState,
                currentUser = currentUser,
                photoTitles = photoTitles,
                totalAuditsCount = userAudits.size,
                periodeList = periodes,
                pools = pools,
                onNoMobilChanged = { viewModel.onNoMobilChanged(it) },
                onPeriodeChanged = { viewModel.onPeriodeChanged(it) },
                onPlatNomerChanged = { viewModel.onPlatNomerChanged(it) },
                onTipeChanged = { viewModel.onTipeChanged(it) },
                onWarnaChanged = { viewModel.onWarnaChanged(it) },
                onTahunChanged = { viewModel.onTahunChanged(it) },
                onTanggalServiceChanged = { viewModel.onTanggalServiceChanged(it) },
                onServiceChanged = { viewModel.onServiceChanged(it) },
                onPoolChanged = { viewModel.onPoolChanged(it) },
                onJenisSampleChanged = { viewModel.onJenisSampleChanged(it) },
                onSampleTanggalChanged = { viewModel.onSampleTanggalChanged(it) },
                onPaketServiceChanged = { viewModel.onPaketServiceChanged(it) },
                onKpiScoreChanged = { itemId, score -> viewModel.onKpiScoreChanged(itemId, score) },
                onKpiPhotoCaptured = { key, path -> viewModel.onKpiPhotoCaptured(key, path) },
                onKpiRemovePhoto = { key -> viewModel.onKpiRemovePhoto(key) },
                onKpiNoteChanged = { key, note -> viewModel.onKpiNoteChanged(key, note) },
                onResetAllNormal = { viewModel.onResetAllNormal() },
                onPhotoCaptured = { slot, path -> viewModel.onPhotoCaptured(slot, path) },
                onRemovePhoto = { slot -> viewModel.onRemovePhoto(slot) },
                onNoteChanged = { slot, note -> viewModel.onNoteChanged(slot, note) },
                onCancelEdit = { viewModel.cancelEditAudit() },
                onRegenerateId = { viewModel.regenerateAuditCode() },
                onSubmit = {
                    viewModel.submitAudit {
                        viewModel.navigateTo(AppScreen.RIWAYAT_SERVICE)
                    }
                }
            )
        }

        AppScreen.RIWAYAT_SERVICE -> {
            val userAudits = remember(audits, currentUser) {
                filterAuditsForUser(audits, currentUser, AppScreen.RIWAYAT_SERVICE)
            }
            val userPools = remember(pools, currentUser) {
                filterPoolsForUser(pools, currentUser)
            }

            RiwayatServiceScreen(
                audits = userAudits,
                pools = userPools,
                currentUser = currentUser,
                photoTitles = photoTitles,
                onSelectAudit = { viewModel.selectAuditForDetail(it) },
                onDeleteAudit = { viewModel.deleteAudit(it) },
                onEditAudit = { viewModel.startEditAudit(it) },
                onDeletePhoto = { auditId, itemKey, path ->
                    viewModel.deleteSingleAuditPhoto(auditId, itemKey, path)
                },
                onSyncAll = { viewModel.reconcileDataWithSheets() }
            )
        }

        AppScreen.RESUME_AUDIT -> {
            val userAudits = remember(audits, currentUser) {
                filterAuditsForUser(audits, currentUser, AppScreen.RESUME_AUDIT)
            }
            val userPools = remember(pools, currentUser) {
                filterPoolsForUser(pools, currentUser)
            }

            com.example.ui.screens.ResumeAuditScreen(
                audits = userAudits,
                periodes = periodes,
                pools = userPools,
                photoTitles = photoTitles,
                currentUser = currentUser,
                onDeleteAudit = { viewModel.deleteAudit(it) },
                onEditAudit = { viewModel.startEditAudit(it) }
            )
        }

        AppScreen.MONITORING_ALL_POOL -> {
            val userAudits = remember(audits, currentUser) {
                filterAuditsForUser(audits, currentUser, AppScreen.MONITORING_ALL_POOL)
            }
            val userPools = remember(pools, currentUser) {
                filterPoolsForUser(pools, currentUser)
            }
            val userVehicles = remember(kendaraanList, currentUser) {
                filterVehiclesForUser(kendaraanList, currentUser)
            }
            MonitoringPoolScreen(
                pools = userPools,
                vehicles = userVehicles,
                audits = userAudits,
                onNavigateToHasilAudit = { poolCode ->
                    viewModel.poolFilterMonitoring.value = poolCode
                    viewModel.navigateTo(AppScreen.HASIL_AUDIT_ALL_POOL)
                },
                onSyncAll = { viewModel.reconcileDataWithSheets() }
            )
        }

        AppScreen.MONITORING_AUDIT -> {
            val userAudits = remember(audits, currentUser) {
                filterAuditsForUser(audits, currentUser, AppScreen.MONITORING_AUDIT)
            }
            val userPools = remember(pools, currentUser) {
                filterPoolsForUser(pools, currentUser)
            }
            val userVehicles = remember(kendaraanList, currentUser) {
                filterVehiclesForUser(kendaraanList, currentUser)
            }
            MonitoringAuditScreen(
                pools = userPools,
                vehicles = userVehicles,
                audits = userAudits,
                onNavigateToHasilAudit = { poolCode ->
                    viewModel.poolFilterMonitoring.value = poolCode
                    viewModel.navigateTo(AppScreen.HASIL_AUDIT_ALL_POOL)
                },
                onSyncAll = { viewModel.reconcileDataWithSheets() }
            )
        }

        AppScreen.HASIL_AUDIT_ALL_POOL -> {
            val userAudits = remember(audits, currentUser) {
                filterAuditsForUser(audits, currentUser, AppScreen.HASIL_AUDIT_ALL_POOL)
            }
            val userPools = remember(pools, currentUser) {
                filterPoolsForUser(pools, currentUser)
            }
            val selectedPool by viewModel.poolFilterMonitoring.collectAsState()
            val effectiveSelectedPool = if (isUserRestrictedToPool(currentUser)) {
                val userPoolsList = currentUser?.kodePool?.split(",", ";")?.map { it.trim().uppercase() }?.filter { it.isNotBlank() }.orEmpty()
                if (userPoolsList.size == 1) userPoolsList.first() else selectedPool
            } else {
                selectedPool
            }
            HasilAuditScreen(
                audits = userAudits,
                pools = userPools,
                photoTitles = photoTitles,
                selectedPoolFilter = effectiveSelectedPool,
                currentUser = currentUser,
                onPoolFilterChanged = { viewModel.poolFilterMonitoring.value = it },
                onDeleteAudit = { viewModel.deleteAudit(it) },
                onEditAudit = { viewModel.startEditAudit(it) },
                onDeletePhoto = { auditId, itemKey, path ->
                    viewModel.deleteSingleAuditPhoto(auditId, itemKey, path)
                },
                onSyncAll = { viewModel.reconcileDataWithSheets() }
            )
        }

        AppScreen.MASTER_KENDARAAN -> {
            val userLevel = currentUser?.levelAkses?.trim()?.uppercase() ?: ""
            if (userLevel == "KEPALA BENGKEL") {
                viewModel.navigateTo(AppScreen.RIWAYAT_SERVICE)
            } else {
                val userPool = currentUser?.let { viewModel.resolveTargetPool(it) }.orEmpty()
                MasterKendaraanScreen(
                    kendaraanList = kendaraanList,
                    pools = pools,
                    currentUser = currentUser,
                    currentUserPool = userPool,
                    isSyncing = isSyncingKendaraan,
                    syncProgress = syncKendaraanProgress,
                    onSyncKendaraan = { poolFilter -> viewModel.syncKendaraan(poolFilter) },
                    onSaveKendaraan = { viewModel.addOrUpdateKendaraan(it) },
                    onDeleteKendaraan = { viewModel.deleteKendaraan(it) },
                    onClearDemoKendaraan = { viewModel.clearDemoData() }
                )
            }
        }

        AppScreen.MASTER_POOL -> {
            MasterPoolScreen(
                pools = pools,
                sampleList = sampleList,
                onSavePool = { viewModel.addOrUpdatePool(it) },
                onDeletePool = { viewModel.deletePool(it) }
            )
        }

        AppScreen.MASTER_USER -> {
            MasterUserScreen(
                users = users,
                pools = pools,
                currentUser = currentUser,
                onSaveUser = { viewModel.addOrUpdateUser(it) },
                onDeleteUser = { viewModel.deleteUser(it) }
            )
        }

        AppScreen.PENGATURAN_NAMAPHOTO -> {
            PengaturanFotoScreen(
                photoSettings = photoTitles,
                onUpdateSetting = { viewModel.updatePhotoSetting(it) }
            )
        }

        AppScreen.MASTER_PERIODE -> {
            MasterPeriodeScreen(
                periodeList = periodes,
                onSavePeriode = { viewModel.addOrUpdatePeriode(it) },
                onDeletePeriode = { viewModel.deletePeriode(it) }
            )
        }

        AppScreen.SETTING_BOBOT -> {
            MasterBobotScreen(
                bobotList = bobotList,
                currentUser = currentUser,
                onUpdateBobot = { itemId, newBobot ->
                    viewModel.updateBobot(itemId, newBobot)
                },
                onResetAllBobot = {
                    viewModel.resetAllBobotToDefault()
                }
            )
        }

        AppScreen.SAMPLE -> {
            val isSyncingSample by viewModel.isSyncingSample.collectAsState()
            LaunchedEffect(Unit) {
                if (viewModel.repository.sessionManager.scriptUrl.isNotBlank()) {
                    viewModel.syncSampleWithGoogleSheet()
                }
            }
            SampleScreen(
                sampleList = sampleList,
                poolList = pools,
                currentUser = currentUser,
                onSaveSample = { sample ->
                    viewModel.addOrUpdateSample(sample)
                },
                onDeleteSample = { statusArea ->
                    viewModel.deleteSample(statusArea)
                },
                onSyncGoogleSheet = {
                    viewModel.syncSampleWithGoogleSheet()
                },
                isSyncing = isSyncingSample
            )
        }

        AppScreen.GRADE_KPI -> {
            GradeKpiScreen(
                gradeList = gradeList,
                currentUser = currentUser,
                onSaveGrade = { grade ->
                    viewModel.saveGradeKpi(grade)
                },
                onResetDefault = {
                    viewModel.resetDefaultGradeKpi()
                },
                onSyncToCloud = {
                    viewModel.syncAllGradeKpiToCloud()
                }
            )
        }

        AppScreen.DOWNLOAD_PHOTO_AUDIT -> {
            val userAudits = remember(audits, currentUser) {
                filterAuditsForUser(audits, currentUser, AppScreen.DOWNLOAD_PHOTO_AUDIT)
            }
            val userPools = remember(pools, currentUser) {
                filterPoolsForUser(pools, currentUser)
            }

            DownloadPhotoAuditScreen(
                audits = userAudits,
                periodes = periodes,
                pools = userPools,
                photoTitles = photoTitles,
                currentUser = currentUser
            )
        }

        AppScreen.BOBOT_RESUME -> {
            val userAudits = remember(audits, currentUser) {
                filterAuditsForUser(audits, currentUser, AppScreen.BOBOT_RESUME)
            }
            val userPools = remember(pools, currentUser) {
                filterPoolsForUser(pools, currentUser)
            }
            BobotResumeScreen(
                audits = userAudits,
                bobotList = bobotList,
                periodes = periodes,
                pools = userPools,
                currentUser = currentUser,
                sampleList = sampleList,
                onUpdateBobot = { itemId, newBobot ->
                    viewModel.updateBobot(itemId, newBobot)
                },
                onResetAllBobot = {
                    viewModel.resetAllBobotToDefault()
                }
            )
        }

        AppScreen.REKAP_NILAI -> {
            if (!isSuperAdmin) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Akses Terbatas",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "Akses Dibatasi: Menu Rekap Nilai hanya dapat diakses oleh level Super Admin.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { viewModel.navigateTo(AppScreen.INPUT_AUDIT) }) {
                            Text("Kembali ke Menu Utama")
                        }
                    }
                }
            } else {
                val userAudits = remember(audits, currentUser) {
                    filterAuditsForUser(audits, currentUser, AppScreen.REKAP_NILAI)
                }
                val userPools = remember(pools, currentUser) {
                    filterPoolsForUser(pools, currentUser)
                }
                RekapNilaiSubGroupScreen(
                    audits = userAudits,
                    bobotList = bobotList,
                    periodes = periodes,
                    pools = userPools,
                    currentUser = currentUser,
                    sampleList = sampleList,
                    gradeList = gradeList
                )
            }
        }

        AppScreen.REKAP_TEMUAN -> {
            val userAudits = remember(audits, currentUser) {
                filterAuditsForUser(audits, currentUser, AppScreen.REKAP_TEMUAN)
            }
            val userPools = remember(pools, currentUser) {
                filterPoolsForUser(pools, currentUser)
            }
            RekapTemuanScreen(
                audits = userAudits,
                periodes = periodes,
                pools = userPools,
                sampleList = sampleList,
                currentUser = currentUser
            )
        }

        AppScreen.PERINGKAT_KPI -> {
            val userAudits = remember(audits, currentUser) {
                filterAuditsForUser(audits, currentUser, AppScreen.PERINGKAT_KPI)
            }
            val userPools = remember(pools, currentUser) {
                filterPoolsForUser(pools, currentUser)
            }
            com.example.ui.screens.PeringkatKpiScreen(
                pools = userPools,
                audits = userAudits,
                periodes = periodes,
                bobotList = bobotList,
                sampleList = sampleList,
                gradeList = gradeList,
                onNavigateToRekapNilai = { _, _ ->
                    if (isSuperAdmin) {
                        viewModel.navigateTo(AppScreen.REKAP_NILAI)
                    }
                },
                onNavigateToHasilAudit = { poolCode ->
                    viewModel.poolFilterMonitoring.value = poolCode
                    viewModel.navigateTo(AppScreen.HASIL_AUDIT_ALL_POOL)
                }
            )
        }

        AppScreen.GOOGLE_SCRIPT_CONFIG -> {
            val sessionManager = viewModel.repository.sessionManager
            val isReconciling by viewModel.isReconciling.collectAsState()
            val lastReconcileTime by viewModel.lastReconcileTime.collectAsState()
            val lastReconcileSummary by viewModel.lastReconcileSummary.collectAsState()
            val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsState()
            val autoSyncIntervalMinutes by viewModel.autoSyncIntervalMinutes.collectAsState()

            GoogleScriptSetupScreen(
                currentScriptUrl = sessionManager.scriptUrl,
                currentLogoUrl = sessionManager.logoUrl,
                currentSheetId = sessionManager.sheetId,
                currentFolderId = sessionManager.driveFolderId,
                currentBigQuery = sessionManager.bigQueryDataset,
                autoSyncEnabled = autoSyncEnabled,
                autoSyncIntervalMinutes = autoSyncIntervalMinutes,
                lastReconcileTime = lastReconcileTime,
                lastReconcileSummary = lastReconcileSummary,
                isReconciling = isReconciling,
                onToggleAutoSync = { viewModel.toggleAutoSync(it) },
                onSetAutoSyncInterval = { viewModel.setAutoSyncInterval(it) },
                onReconcileNow = { viewModel.reconcileDataWithSheets() },
                onSaveSettings = { script, logo, sheet, folder, bq ->
                    viewModel.saveSettings(script, logo, sheet, folder, bq)
                },
                onDownloadMasterData = {
                    viewModel.downloadMasterData()
                },
                onSyncBobotToCloud = {
                    viewModel.syncAllBobotToCloud()
                },
                onClearDemoData = {
                    viewModel.clearDemoData()
                },
                onClearAllAudits = {
                    viewModel.deleteAllAudits()
                },
                onClearAllKendaraan = {
                    viewModel.deleteAllKendaraan()
                },
                onInitInputAudit = { onResult ->
                    viewModel.initInputAuditSheet(onResult)
                },
                onInitGradeKpi = { onResult ->
                    viewModel.initGradeKpiSheet(onResult)
                },
                onInitAllSheets = { onResult ->
                    viewModel.setupSpreadsheet(onResult)
                }
            )
        }

        AppScreen.WEB_SERVER_PC -> {
            val userLevel = currentUser?.levelAkses?.trim()?.uppercase() ?: ""
            val canAccess = userLevel in listOf("SUPER ADMIN", "ADMIN", "SKT")
            if (canAccess) {
                WebServerPcScreen(viewModel = viewModel)
            } else {
                viewModel.navigateTo(AppScreen.RIWAYAT_SERVICE)
            }
        }

        else -> {}
    }
}

/**
 * Cek apakah user dibatasi hanya untuk pool tertentu.
 * Aturan: Pengguna yang terdaftar di pool operasional (bukan SKT pusat / ALL)
 * tidak boleh melihat data atau armada dari pool lain.
 */
fun isUserRestrictedToPool(currentUser: UserEntity?): Boolean {
    if (currentUser == null) return false
    val role = currentUser.levelAkses.trim().uppercase()
    val pool = currentUser.kodePool.trim().uppercase()
    if (pool.isBlank() || pool == "SKT" || pool == "ALL") return false
    // Super admin, admin, skt, dan master data pusat memiliki akses multi-pool
    if (role == "SUPER ADMIN" || role == "ADMIN" || role == "SKT" || role == "MASTER DATA") {
        return false
    }
    return true
}

/**
 * Filter daftar pool untuk dropdown dan statistik sesuai penugasan user:
 * Jika user terikat dengan pool A, hanya tampilkan Pool A.
 */
private fun filterPoolsForUser(
    pools: List<com.example.data.local.entity.MasterPoolEntity>,
    currentUser: UserEntity?
): List<com.example.data.local.entity.MasterPoolEntity> {
    if (currentUser == null) return pools
    val pool = currentUser.kodePool.trim()
    if (isUserRestrictedToPool(currentUser) && pool.isNotBlank()) {
        val userPoolList = pool.split(",", ";").map { it.trim().uppercase() }.filter { it.isNotBlank() }
        val matched = pools.filter { p -> userPoolList.any { it.equals(p.kodePool.trim(), ignoreCase = true) } }
        return if (matched.isNotEmpty()) matched else userPoolList.map { com.example.data.local.entity.MasterPoolEntity(kodePool = it, namaPool = "Pool $it") }
    }
    return pools
}

/**
 * Filter daftar kendaraan:
 * Jika user terikat dengan pool A, hanya tampilkan kendaraan dari Pool A.
 */
private fun filterVehiclesForUser(
    vehicles: List<com.example.data.local.entity.MasterKendaraanEntity>,
    currentUser: UserEntity?
): List<com.example.data.local.entity.MasterKendaraanEntity> {
    if (currentUser == null) return vehicles
    val pool = currentUser.kodePool.trim()
    if (isUserRestrictedToPool(currentUser) && pool.isNotBlank()) {
        val userPoolList = pool.split(",", ";").map { it.trim().uppercase() }.filter { it.isNotBlank() }
        return vehicles.filter { v -> userPoolList.any { it.equals(v.pool.trim(), ignoreCase = true) } }
    }
    return vehicles
}

/**
 * Filter data audit sesuai aturan ketat isolasi data:
 * 1. "filter auditor A tidak dapat melihat auditor B padahal dalam 1 pool":
 *    - Auditor (atau staf lapangan) HANYA dapat melihat data audit yang diinput oleh dirinya sendiri
 *      (berdasarkan kecocokan NIP, username, atau nama petugas).
 *    - Auditor A sama sekali tidak dapat melihat data audit yang diinput oleh Auditor B walaupun dalam 1 pool.
 * 2. "user pool A tidak bisa lihat data dari pool lain":
 *    - Pengguna yang terikat pool (misal Kepala Bengkel Pool A, Auditor Pool A)
 *      HANYA dapat melihat data dari pool yang terdaftar (Pool A).
 *    - Sama sekali tidak dapat melihat data dari pool lain (Pool B, Pool C, dll).
 * 3. KEPALA BENGKEL / KABAG:
 *    - Dapat melihat seluruh data audit dalam pool-nya sendiri untuk keperluan monitoring dan evaluasi.
 * 4. SUPER ADMIN, ADMIN, SKT, dan MASTER DATA (Pusat):
 *    - Memiliki akses monitoring menyeluruh lintas pool dan auditor.
 */
private fun filterAuditsForUser(
    audits: List<DataAuditEntity>,
    currentUser: UserEntity?,
    screen: AppScreen? = null
): List<DataAuditEntity> {
    if (currentUser == null) return emptyList()

    // Aturan: Data hasil INPUT AUDIT tidak akan di tampilkan jika di temukan ID yang sama
    val deduplicatedAudits = AuditFilterUtils.filterUniqueAudits(audits)

    val role = currentUser.levelAkses.trim().uppercase()
    val userPool = currentUser.kodePool.trim()
    val poolRestricted = isUserRestrictedToPool(currentUser)

    // SUPER ADMIN, ADMIN, SKT, dan MASTER DATA (Pusat): Akses global ke semua pool & user
    if (!poolRestricted && (role == "SUPER ADMIN" || role == "ADMIN" || role == "SKT" || role == "MASTER DATA")) {
        return if (screen == AppScreen.RIWAYAT_SERVICE || screen == AppScreen.RESUME_AUDIT || screen == AppScreen.REKAP_NILAI || screen == AppScreen.BOBOT_RESUME || screen == AppScreen.REKAP_TEMUAN || screen == AppScreen.PERINGKAT_KPI) {
            AuditFilterUtils.filterLatestPerCarAndPeriod(deduplicatedAudits)
        } else {
            deduplicatedAudits
        }
    }

    // Terapkan isolasi pool: "user pool A tidak bisa lihat data dari pool lain"
    val userPoolList = userPool.split(",", ";").map { it.trim().uppercase() }.filter { it.isNotBlank() }
    val poolAudits = if (userPoolList.isNotEmpty() && !userPoolList.contains("SKT") && !userPoolList.contains("ALL")) {
        deduplicatedAudits.filter { a -> userPoolList.any { it.equals(a.pool.trim(), ignoreCase = true) } }
    } else {
        deduplicatedAudits
    }

    // KEPALA BENGKEL / KABAG:
    // Dapat melihat seluruh audit pada pool-nya sendiri (Pool A), tetapi tidak bisa melihat pool lain
    if (role == "KEPALA BENGKEL" || role.contains("KABAG")) {
        return if (screen == AppScreen.RIWAYAT_SERVICE || screen == AppScreen.RESUME_AUDIT || screen == AppScreen.REKAP_NILAI || screen == AppScreen.BOBOT_RESUME || screen == AppScreen.REKAP_TEMUAN) {
            AuditFilterUtils.filterLatestPerCarAndPeriod(poolAudits)
        } else {
            poolAudits
        }
    }

    // AUDITOR & Staf Lapangan / Pembuat input audit:
    // Aturan ketat: "auditor A tidak dapat melihat auditor B padahal dalam 1 pool"
    // HANYA menampilkan audit yang dibuat oleh auditor saat ini (cocok NIP / nama petugas / username).
    val ownAudits = poolAudits.filter { audit ->
        val nipMatch = currentUser.nip.isNotBlank() && audit.nip.isNotBlank() &&
            audit.nip.trim().equals(currentUser.nip.trim(), ignoreCase = true)
        val userMatch = currentUser.username.isNotBlank() && (
            audit.namaPetugas.trim().equals(currentUser.username.trim(), ignoreCase = true) ||
            audit.nip.trim().equals(currentUser.username.trim(), ignoreCase = true)
        )
        val nameMatch = currentUser.namaPetugas.isNotBlank() &&
            audit.namaPetugas.trim().equals(currentUser.namaPetugas.trim(), ignoreCase = true)

        nipMatch || userMatch || nameMatch
    }

    return if (screen == AppScreen.RIWAYAT_SERVICE || screen == AppScreen.RESUME_AUDIT || screen == AppScreen.REKAP_NILAI || screen == AppScreen.BOBOT_RESUME || screen == AppScreen.REKAP_TEMUAN) {
        AuditFilterUtils.filterLatestPerCarAndPeriod(ownAudits)
    } else {
        ownAudits
    }
}
