package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MasterPoolEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.theme.Amber50
import com.example.ui.theme.Amber600
import com.example.ui.theme.Blue50
import com.example.ui.theme.Blue700
import com.example.ui.theme.Blue900
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate800
import com.example.ui.theme.Teal50
import com.example.ui.theme.Teal600
import com.example.util.DateUtils
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MasterUserScreen(
    users: List<UserEntity>,
    pools: List<MasterPoolEntity>,
    currentUser: UserEntity? = null,
    onSaveUser: (UserEntity) -> Unit,
    onDeleteUser: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<UserEntity?>(null) }

    // Form states - default blank
    var nip by remember { mutableStateOf("") }
    var namaPetugas by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var levelAkses by remember { mutableStateOf("") }
    var tanggalAktif by remember { mutableStateOf("") }
    var kodePool by remember { mutableStateOf("") }
    var statusUser by remember { mutableStateOf("AKTIF") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var dialogErrorMessage by remember { mutableStateOf<String?>(null) }

    var levelAksesExpanded by remember { mutableStateOf(false) }
    var kodePoolExpanded by remember { mutableStateOf(false) }

    val isCurrentSuperAdmin = currentUser?.levelAkses.equals("SUPER ADMIN", ignoreCase = true)

    // Role options: SUPER ADMIN can create any role; ADMIN/others CANNOT create SUPER ADMIN
    val availableRoles = remember(isCurrentSuperAdmin) {
        if (isCurrentSuperAdmin) {
            listOf("SUPER ADMIN", "ADMIN", "KEPALA BENGKEL", "MASTER DATA", "SKT", "AUDITOR")
        } else {
            listOf("ADMIN", "KEPALA BENGKEL", "MASTER DATA", "SKT", "AUDITOR")
        }
    }

    val availablePools = remember(pools) {
        val list = pools.map { it.kodePool.trim() }.filter { it.isNotBlank() }.distinct()
        if (list.isEmpty()) listOf("SKT", "POOL-01", "POOL-02", "POOL-03") else list
    }

    val activeCount = remember(users) {
        users.count { !it.status.equals("BLOCKED", ignoreCase = true) && !it.status.equals("BLOKED", ignoreCase = true) }
    }
    val blockedCount = remember(users) {
        users.count { it.status.equals("BLOCKED", ignoreCase = true) || it.status.equals("BLOKED", ignoreCase = true) }
    }

    val filteredUsers = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users else {
            val q = searchQuery.trim().lowercase()
            users.filter {
                it.nip.lowercase().contains(q) ||
                        it.namaPetugas.lowercase().contains(q) ||
                        it.username.lowercase().contains(q) ||
                        it.levelAkses.lowercase().contains(q) ||
                        it.kodePool.lowercase().contains(q) ||
                        it.status.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Control Card: Tambah User, Total Stats, Search & Table Format Indicator
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
                    // Tombol Tambah User Baru (Form Blank)
                    Button(
                        onClick = {
                            editingUser = null
                            nip = ""
                            namaPetugas = ""
                            username = ""
                            password = ""
                            levelAkses = ""
                            tanggalAktif = ""
                            kodePool = ""
                            statusUser = "AKTIF"
                            dialogErrorMessage = null
                            isPasswordVisible = false
                            showDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Blue700),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_tambah_user")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TAMBAH USER", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Aktif: $activeCount",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF15803D)),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                        if (blockedCount > 0) {
                            Surface(
                                color = Color(0xFFFEE2E2),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Blocked: $blockedCount",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Rose600),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari NIP, Nama Petugas, UserName Login, Level Akses, Pool...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Blue700)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue700,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Tidak ada data pengguna terdaftar.", style = MaterialTheme.typography.bodyMedium.copy(color = Slate600))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredUsers, key = { it.nip.ifBlank { it.username } }) { user ->
                    val userRole = user.levelAkses.trim().uppercase()
                    val isTargetSuperAdmin = userRole == "SUPER ADMIN"
                    val canModify = isCurrentSuperAdmin || !isTargetSuperAdmin
                    val isBlocked = user.status.equals("BLOCKED", ignoreCase = true) || user.status.equals("BLOKED", ignoreCase = true)

                    val (badgeBg, badgeText) = when (userRole) {
                        "SUPER ADMIN" -> Color(0xFFEEF2FF) to Color(0xFF4F46E5)
                        "ADMIN" -> Blue50 to Blue700
                        "KEPALA BENGKEL" -> Amber50 to Amber600
                        "MASTER DATA" -> Teal50 to Teal600
                        "SKT" -> Color(0xFFF5F3FF) to Color(0xFF7C3AED)
                        else -> Color(0xFFF1F5F9) to Color(0xFF475569)
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isBlocked) Color(0xFFFFF1F2) else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isBlocked) Color(0xFFFECDD3) else Color(0xFFE2E8F0)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            // 1. NIP & Status Pengguna Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.5.dp, Color(0xFFBFDBFE))
                                ) {
                                    Text(
                                        text = "NIP: ${user.nip.ifBlank { "-" }}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Blue900,
                                            fontSize = 11.5.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                // Status Badge (AKTIF vs BLOCKED)
                                Surface(
                                    color = if (isBlocked) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isBlocked) Color(0xFFFCA5A5) else Color(0xFF86EFAC)
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isBlocked) Icons.Default.Block else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (isBlocked) Rose600 else Color(0xFF15803D),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isBlocked) "BLOCKED" else "AKTIF",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isBlocked) Rose600 else Color(0xFF15803D),
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 2. Nama Petugas (Huruf Besar)
                            Text(
                                text = user.namaPetugas.uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (isBlocked) Rose600 else Blue900
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 3. UserName Login & Password (Masked ******)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "UserName Login",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontSize = 10.sp)
                                    )
                                    Text(
                                        text = user.username,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Slate800)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Password",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontSize = 10.sp)
                                    )
                                    // Password strictly masked as ******
                                    Text(
                                        text = "******",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = Slate800,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 4. Level Akses
                            Surface(
                                color = badgeBg,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = user.levelAkses,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = badgeText
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            // Row 4: Tanggal Aktif (Khusus AUDITOR)
                            if (user.levelAkses.trim().equals("AUDITOR", ignoreCase = true)) {
                                Spacer(modifier = Modifier.height(6.dp))
                                val isAuditorActiveToday = remember(user.tanggalAktif) {
                                    DateUtils.isDateMatchingToday(user.tanggalAktif)
                                }
                                Surface(
                                    color = if (isAuditorActiveToday) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(
                                        0.5.dp,
                                        if (isAuditorActiveToday) Color(0xFF86EFAC) else Color(0xFFFDE68A)
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = if (isAuditorActiveToday) Color(0xFF15803D) else Color(0xFFB45309),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (user.tanggalAktif.isNotBlank()) "Tgl: ${DateUtils.formatDisplayDate(user.tanggalAktif)} (${if (isAuditorActiveToday) "Aktif Hari Ini" else "Tidak Aktif"})" else "Tgl: Belum di-set",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isAuditorActiveToday) Color(0xFF15803D) else Color(0xFFB45309),
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Row 5: Pool & Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Kode Pool Tugas
                                val isUserSkt = user.levelAkses.trim().equals("SKT", ignoreCase = true)
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.5.dp, Color(0xFFCBD5E1))
                                ) {
                                    Text(
                                        text = if (isUserSkt) "Pool: SKT (Pusat)" else "Pool: ${user.kodePool}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Slate800
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                // Actions: Edit, Quick Block/Activate, Delete
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (canModify) {
                                        // Quick Toggle Status Button (Aktifkan / Blokir)
                                        OutlinedButton(
                                            onClick = {
                                                val newStatus = if (isBlocked) "AKTIF" else "BLOCKED"
                                                onSaveUser(user.copy(status = newStatus))
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isBlocked) Color(0xFF16A34A) else Rose600
                                            ),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = if (isBlocked) Color(0xFF15803D) else Rose600
                                            ),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(
                                                text = if (isBlocked) "Aktifkan" else "Blokir",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        IconButton(
                                            onClick = {
                                                editingUser = user
                                                nip = user.nip
                                                namaPetugas = user.namaPetugas.uppercase()
                                                username = user.username
                                                password = user.password
                                                levelAkses = user.levelAkses
                                                tanggalAktif = user.tanggalAktif
                                                kodePool = if (user.levelAkses.trim().equals("SKT", ignoreCase = true)) "SKT" else user.kodePool.uppercase()
                                                statusUser = if (isBlocked) "BLOCKED" else "AKTIF"
                                                dialogErrorMessage = null
                                                isPasswordVisible = false
                                                showDialog = true
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Blue700, modifier = Modifier.size(18.dp))
                                        }

                                        IconButton(
                                            onClick = { onDeleteUser(user.nip.ifBlank { user.username }) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Rose600, modifier = Modifier.size(18.dp))
                                        }
                                    } else {
                                        Surface(
                                            color = Color(0xFFF1F5F9),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "Terkunci",
                                                style = MaterialTheme.typography.labelSmall.copy(color = Slate400, fontSize = 10.sp),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog Tambah / Edit User
    if (showDialog) {
        val isEditMode = editingUser != null
        val cleanCurrentNip = nip.trim()
        val cleanCurrentUsername = username.trim()
        val cleanCurrentPool = kodePool.trim().uppercase()
        val isLevelSkt = levelAkses.trim().equals("SKT", ignoreCase = true)

        // Validation for Pool in Master Pool (hanya jika bukan SKT)
        val isPoolInvalid = remember(cleanCurrentPool, pools, availablePools, isLevelSkt) {
            if (isLevelSkt) {
                false
            } else if (cleanCurrentPool.isNotBlank()) {
                val poolFound = pools.any { it.kodePool.trim().equals(cleanCurrentPool, ignoreCase = true) } ||
                        (pools.isEmpty() && availablePools.any { it.equals(cleanCurrentPool, ignoreCase = true) })
                !poolFound
            } else {
                false
            }
        }

        // Validation for duplicate NIP
        val isDuplicateNip = remember(cleanCurrentNip, editingUser, users) {
            if (cleanCurrentNip.isBlank()) false else {
                users.any {
                    it.nip.trim().equals(cleanCurrentNip, ignoreCase = true) &&
                            (editingUser == null || !it.nip.trim().equals(editingUser?.nip?.trim(), ignoreCase = true))
                }
            }
        }

        // Validation for duplicate username
        val isDuplicateUsername = remember(cleanCurrentUsername, editingUser, users) {
            if (cleanCurrentUsername.isBlank()) false else {
                users.any {
                    it.username.trim().equals(cleanCurrentUsername, ignoreCase = true) &&
                            (editingUser == null || (!it.nip.equals(editingUser?.nip, ignoreCase = true) && !it.username.equals(editingUser?.username, ignoreCase = true)))
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = if (isEditMode) "Edit Pengguna (${editingUser?.nip?.ifBlank { editingUser?.username }})" else "Tambah User Baru (Form Blank)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Blue900)
                )
            },
            text = {
                val dialogScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(dialogScrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (dialogErrorMessage != null) {
                        Surface(
                            color = Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = dialogErrorMessage ?: "",
                                color = Rose600,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    // 1. NIP (Nomor Induk Pegawai - Wajib Isi & Unik)
                    OutlinedTextField(
                        value = nip,
                        onValueChange = {
                            nip = it
                            dialogErrorMessage = null
                        },
                        label = { Text("NIP *") },
                        placeholder = { Text("Contoh: 1001 atau 20240101") },
                        singleLine = true,
                        isError = isDuplicateNip,
                        supportingText = if (isDuplicateNip) {
                            {
                                Text(
                                    text = "NIP '$cleanCurrentNip' sudah digunakan! NIP tidak boleh sama.",
                                    color = Rose600,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_nip")
                    )

                    // 2. Nama Petugas (SELALU HURUF BESAR)
                    OutlinedTextField(
                        value = namaPetugas,
                        onValueChange = {
                            namaPetugas = it.uppercase()
                            dialogErrorMessage = null
                        },
                        label = { Text("Nama Petugas (Selalu Huruf Besar) *") },
                        placeholder = { Text("CONTOH: BUDI SANTOSO") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_nama_petugas")
                    )

                    // 3. UserName Login (UNIK - TIDAK BOLEH SAMA)
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            dialogErrorMessage = null
                        },
                        label = { Text("UserName Login (Harus Unik) *") },
                        placeholder = { Text("Contoh: budi_skt") },
                        singleLine = true,
                        isError = isDuplicateUsername,
                        supportingText = if (isDuplicateUsername) {
                            {
                                Text(
                                    text = "Username '$cleanCurrentUsername' sudah digunakan! Username tidak boleh sama.",
                                    color = Rose600,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_username")
                    )

                    // 3. Password (TIDAK TERLIHAT HANYA KODE ******)
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            dialogErrorMessage = null
                        },
                        label = { Text("Password *") },
                        placeholder = { Text("Masukkan password") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation('*'),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isPasswordVisible) "Sembunyikan Password" else "Lihat Password",
                                    tint = Slate600
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_password")
                    )

                    // 4. Level Akses (Dropdown)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = levelAkses,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Level Akses *") },
                            placeholder = { Text("Pilih Level Akses") },
                            trailingIcon = {
                                IconButton(onClick = { levelAksesExpanded = !levelAksesExpanded }) {
                                    Icon(
                                        imageVector = if (levelAksesExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Pilih Level Akses",
                                        tint = Blue700
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { levelAksesExpanded = true }
                        )
                        DropdownMenu(
                            expanded = levelAksesExpanded,
                            onDismissRequest = { levelAksesExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.75f)
                        ) {
                            availableRoles.forEach { role ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = role,
                                            fontWeight = if (levelAkses == role) FontWeight.Bold else FontWeight.Normal,
                                            color = if (levelAkses == role) Blue700 else Slate800
                                        )
                                    },
                                    onClick = {
                                        levelAkses = role
                                        if (role.equals("SKT", ignoreCase = true)) {
                                            kodePool = "SKT"
                                        } else if (kodePool.equals("SKT", ignoreCase = true)) {
                                            kodePool = ""
                                        }
                                        levelAksesExpanded = false
                                        dialogErrorMessage = null
                                    }
                                )
                            }
                        }
                    }

                    // 5. Tanggal Aktif (HANYA AKAN TAMPIL JIKA LEVEL AKSES AUDITOR)
                    val isAuditorSelected = levelAkses.trim().equals("AUDITOR", ignoreCase = true)
                    if (isAuditorSelected) {
                        val context = LocalContext.current
                        val todayFormatted = remember { DateUtils.getCurrentDateFormatted() }

                        fun openCalendarPicker() {
                            val calendar = Calendar.getInstance()
                            if (tanggalAktif.isNotBlank()) {
                                try {
                                    val parsedCal = DateUtils.parseToCalendar(tanggalAktif)
                                    if (parsedCal != null) {
                                        calendar.time = parsedCal.time
                                    } else {
                                        val parts = tanggalAktif.split("/", "-")
                                        if (parts.size == 3) {
                                            if (parts[0].length == 4) {
                                                calendar.set(Calendar.YEAR, parts[0].toInt())
                                                calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
                                                calendar.set(Calendar.DAY_OF_MONTH, parts[2].substringBefore("T").toInt())
                                            } else {
                                                calendar.set(Calendar.DAY_OF_MONTH, parts[0].toInt())
                                                calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
                                                calendar.set(Calendar.YEAR, parts[2].substringBefore("T").toInt())
                                            }
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    tanggalAktif = String.format(Locale.US, "%02d/%02d/%04d", dayOfMonth, month + 1, year)
                                    dialogErrorMessage = null
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }

                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Tanggal Aktif (Khusus AUDITOR) *",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Blue900,
                                            fontSize = 12.sp
                                        )
                                    )

                                    // Shortcut Cepat: Hari Ini & Besok
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            color = Blue700,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.clickable {
                                                tanggalAktif = todayFormatted
                                                dialogErrorMessage = null
                                            }
                                        ) {
                                            Text(
                                                text = "Hari Ini",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }

                                        Surface(
                                            color = Color(0xFFDBEAFE),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.clickable {
                                                val cal = Calendar.getInstance()
                                                cal.add(Calendar.DAY_OF_YEAR, 1)
                                                tanggalAktif = String.format(
                                                    Locale.US,
                                                    "%02d/%02d/%04d",
                                                    cal.get(Calendar.DAY_OF_MONTH),
                                                    cal.get(Calendar.MONTH) + 1,
                                                    cal.get(Calendar.YEAR)
                                                )
                                                dialogErrorMessage = null
                                            }
                                        ) {
                                            Text(
                                                text = "Besok",
                                                color = Blue900,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = tanggalAktif,
                                    onValueChange = {
                                        tanggalAktif = it
                                        dialogErrorMessage = null
                                    },
                                    placeholder = { Text("DD/MM/YYYY (contoh: $todayFormatted)") },
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = { openCalendarPicker() }) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = "Pilih Tanggal dari Kalender",
                                                tint = Blue700
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_tanggal_aktif")
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "User AUDITOR hanya akan aktif dan dapat login sesuai dengan tanggal yang di-set di atas.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = Blue900,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }

                    // 6. Kode Pool Tugas (HANYA DITAMPILKAN JIKA BUKAN LEVEL SKT - DROPDOWN PILIHAN 1 POOL)
                    if (!isLevelSkt) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = if (cleanCurrentPool.isNotBlank()) {
                                        val matched = pools.find { it.kodePool.trim().equals(cleanCurrentPool, ignoreCase = true) }
                                        if (matched != null && matched.namaPool.isNotBlank()) {
                                            "$cleanCurrentPool - ${matched.namaPool}"
                                        } else {
                                            cleanCurrentPool
                                        }
                                    } else {
                                        ""
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Kode Pool Tugas *") },
                                    placeholder = { Text("Pilih Kode Pool dari Master Pool") },
                                    singleLine = true,
                                    isError = isPoolInvalid,
                                    supportingText = {
                                        if (cleanCurrentPool.isBlank()) {
                                            Text("Wajib dipilih dari Master Pool", color = Slate600)
                                        } else if (isPoolInvalid) {
                                            Text(
                                                text = "Kode Pool '$cleanCurrentPool' tidak ditemukan di Master Pool! Silakan pilih ulang.",
                                                color = Rose600,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            val matched = pools.find { it.kodePool.trim().equals(cleanCurrentPool, ignoreCase = true) }
                                            Text(
                                                text = "✓ Terdaftar: ${matched?.namaPool ?: cleanCurrentPool}",
                                                color = Color(0xFF15803D),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { kodePoolExpanded = !kodePoolExpanded }) {
                                            Icon(
                                                imageVector = if (kodePoolExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                contentDescription = "Pilih Pool dari Master Pool",
                                                tint = Blue700
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { kodePoolExpanded = true }
                                        .testTag("input_kode_pool")
                                )
                                DropdownMenu(
                                    expanded = kodePoolExpanded,
                                    onDismissRequest = { kodePoolExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    if (availablePools.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Tidak ada pool tersedia", color = Slate400) },
                                            onClick = { kodePoolExpanded = false }
                                        )
                                    } else {
                                        availablePools.forEach { p ->
                                            val poolDetail = pools.find { it.kodePool.trim().equals(p, ignoreCase = true) }
                                            val isSelected = cleanCurrentPool.equals(p, ignoreCase = true)
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(
                                                            text = p,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) Blue700 else Slate800
                                                        )
                                                        if (poolDetail != null && poolDetail.namaPool.isNotBlank()) {
                                                            Text(
                                                                text = poolDetail.namaPool,
                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = Slate400)
                                                            )
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    kodePool = p.uppercase()
                                                    kodePoolExpanded = false
                                                    dialogErrorMessage = null
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. Status (Blocked, Aktif)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Status Pengguna:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate800)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Option: AKTIF
                            Surface(
                                color = if (statusUser == "AKTIF") Color(0xFFDCFCE7) else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    width = if (statusUser == "AKTIF") 2.dp else 1.dp,
                                    color = if (statusUser == "AKTIF") Color(0xFF16A34A) else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { statusUser = "AKTIF" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (statusUser == "AKTIF") Color(0xFF15803D) else Slate400,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "AKTIF",
                                        fontWeight = FontWeight.Bold,
                                        color = if (statusUser == "AKTIF") Color(0xFF15803D) else Slate600,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Option: BLOCKED
                            Surface(
                                color = if (statusUser == "BLOCKED") Color(0xFFFEE2E2) else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    width = if (statusUser == "BLOCKED") 2.dp else 1.dp,
                                    color = if (statusUser == "BLOCKED") Rose600 else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { statusUser = "BLOCKED" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Block,
                                        contentDescription = null,
                                        tint = if (statusUser == "BLOCKED") Rose600 else Slate400,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "BLOCKED",
                                        fontWeight = FontWeight.Bold,
                                        color = if (statusUser == "BLOCKED") Rose600 else Slate600,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (statusUser == "BLOCKED") "User berstatus BLOCKED tidak akan dapat login ke sistem." else "User berstatus AKTIF dapat login normal.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = if (statusUser == "BLOCKED") Rose600 else Slate600
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanNip = nip.trim()
                        val cleanNama = namaPetugas.trim().uppercase()
                        val cleanUser = username.trim()
                        val cleanPass = password.trim()
                        val cleanRole = levelAkses.trim()
                        val cleanPool = kodePool.trim()

                        if (cleanNip.isBlank()) {
                            dialogErrorMessage = "NIP wajib diisi!"
                            return@Button
                        }
                        if (isDuplicateNip) {
                            dialogErrorMessage = "NIP '$cleanNip' sudah digunakan oleh user lain! NIP tidak boleh sama."
                            return@Button
                        }
                        if (cleanNama.isBlank()) {
                            dialogErrorMessage = "Nama Petugas wajib diisi!"
                            return@Button
                        }
                        if (cleanUser.isBlank()) {
                            dialogErrorMessage = "UserName Login wajib diisi!"
                            return@Button
                        }
                        if (isDuplicateUsername) {
                            dialogErrorMessage = "Username '$cleanUser' sudah digunakan oleh user lain! Username tidak boleh sama."
                            return@Button
                        }
                        if (cleanPass.isBlank()) {
                            dialogErrorMessage = "Password wajib diisi!"
                            return@Button
                        }
                        if (cleanRole.isBlank()) {
                            dialogErrorMessage = "Silakan pilih Level Akses!"
                            return@Button
                        }
                        if (cleanPool.isBlank()) {
                            dialogErrorMessage = "Silakan masukkan Kode Pool Tugas!"
                            return@Button
                        }

                        // Validasi: Kode Pool Tugas (Hanya untuk non-SKT, dan hanya pilihan 1 pool)
                        val isFinalRoleSkt = cleanRole.equals("SKT", ignoreCase = true)
                        val formattedPool = if (isFinalRoleSkt) {
                            "SKT"
                        } else {
                            if (cleanPool.isBlank()) {
                                dialogErrorMessage = "Kode Pool wajib dipilih!"
                                return@Button
                            }
                            val poolFound = pools.any { it.kodePool.trim().equals(cleanPool, ignoreCase = true) } ||
                                    (pools.isEmpty() && availablePools.any { it.equals(cleanPool, ignoreCase = true) })
                            if (!poolFound) {
                                dialogErrorMessage = "Kode Pool '$cleanPool' tidak ditemukan di Master Pool! Data ditolak."
                                return@Button
                            }
                            cleanPool.uppercase()
                        }

                        // Ensure non-superadmin cannot escalate to SUPER ADMIN
                        val finalRole = if (!isCurrentSuperAdmin && cleanRole.equals("SUPER ADMIN", ignoreCase = true)) {
                            "ADMIN"
                        } else {
                            cleanRole.uppercase()
                        }

                        // Validate Tanggal Aktif specifically for AUDITOR
                        val isAuditor = finalRole.equals("AUDITOR", ignoreCase = true)
                        val cleanTanggalAktif = if (isAuditor) tanggalAktif.trim() else ""
                        if (isAuditor && cleanTanggalAktif.isBlank()) {
                            dialogErrorMessage = "Tanggal Aktif wajib diisi untuk Level Akses AUDITOR!"
                            return@Button
                        }

                        // If editing and NIP was changed, delete old record
                        if (editingUser != null && editingUser!!.nip.isNotBlank() && editingUser!!.nip != cleanNip) {
                            onDeleteUser(editingUser!!.nip)
                        }

                        onSaveUser(
                            UserEntity(
                                nip = cleanNip,
                                namaPetugas = cleanNama,
                                username = cleanUser,
                                password = cleanPass,
                                levelAkses = finalRole,
                                kodePool = formattedPool,
                                status = statusUser.uppercase(),
                                tanggalAktif = cleanTanggalAktif
                            )
                        )
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue700),
                    modifier = Modifier.testTag("btn_simpan_user")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
