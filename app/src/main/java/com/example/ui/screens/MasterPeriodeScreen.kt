package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MasterPeriodeEntity
import com.example.ui.theme.HighDensityBlue
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate800

@Composable
fun MasterPeriodeScreen(
    periodeList: List<MasterPeriodeEntity>,
    onSavePeriode: (MasterPeriodeEntity) -> Unit,
    onDeletePeriode: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var editingPeriode by remember { mutableStateOf<MasterPeriodeEntity?>(null) }

    var periodeInput by remember { mutableStateOf("") }
    var keteranganInput by remember { mutableStateOf("") }
    var deleteCandidate by remember { mutableStateOf<MasterPeriodeEntity?>(null) }

    val filteredList = remember(periodeList, searchQuery) {
        if (searchQuery.isBlank()) periodeList else {
            val q = searchQuery.trim().lowercase()
            periodeList.filter {
                it.periode.lowercase().contains(q) ||
                        it.keterangan.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
                    Button(
                        onClick = {
                            editingPeriode = null
                            periodeInput = ""
                            keteranganInput = ""
                            showDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_tambah_periode")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ TAMBAH PERIODE", fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "Total: ${periodeList.size} Periode",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Slate600
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari periode atau keterangan...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Slate500)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HighDensityBlue,
                        unfocusedBorderColor = Slate200
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_periode")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Table Header
        Surface(
            color = Slate800,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PERIODE",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    text = "KETERANGAN",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.weight(2f)
                )
                Text(
                    text = "AKSI",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.width(80.dp)
                )
            }
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "Belum ada data periode. Klik tombol + TAMBAH PERIODE." else "Tidak ada periode yang cocok dengan pencarian.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("periode_item_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Column 1: PERIODE
                            Row(
                                modifier = Modifier.weight(1.2f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = HighDensityBlue.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = HighDensityBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = item.periode,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            ),
                                            color = HighDensityBlue
                                        )
                                    }
                                }
                            }

                            // Column 2: KETERANGAN
                            Text(
                                text = item.keterangan.ifBlank { "-" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate800,
                                modifier = Modifier.weight(2f)
                            )

                            // Column 3: AKSI
                            Row(
                                modifier = Modifier.width(80.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = {
                                        editingPeriode = item
                                        periodeInput = item.periode
                                        keteranganInput = item.keterangan
                                        showDialog = true
                                    },
                                    modifier = Modifier.size(36.dp).testTag("btn_edit_periode_${item.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Periode",
                                        tint = HighDensityBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { deleteCandidate = item },
                                    modifier = Modifier.size(36.dp).testTag("btn_delete_periode_${item.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Hapus Periode",
                                        tint = Rose600,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = if (editingPeriode == null) "Tambah Periode Baru" else "Edit Data Periode",
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = periodeInput,
                        onValueChange = { periodeInput = it },
                        label = { Text("PERIODE (Wajib)") },
                        placeholder = { Text("Contoh: Periode 1, Mingguan, Bulanan") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HighDensityBlue,
                            unfocusedBorderColor = Slate200
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_input_periode")
                    )

                    OutlinedTextField(
                        value = keteranganInput,
                        onValueChange = { keteranganInput = it },
                        label = { Text("KETERANGAN") },
                        placeholder = { Text("Contoh: Audit Rutin Minggu Pertama") },
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HighDensityBlue,
                            unfocusedBorderColor = Slate200
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_input_keterangan")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanPeriode = periodeInput.trim()
                        if (cleanPeriode.isNotBlank()) {
                            val entity = MasterPeriodeEntity(
                                id = editingPeriode?.id ?: 0,
                                periode = cleanPeriode,
                                keterangan = keteranganInput.trim()
                            )
                            onSavePeriode(entity)
                            showDialog = false
                        }
                    },
                    enabled = periodeInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("dialog_btn_save_periode")
                ) {
                    Text("SIMPAN", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false },
                    modifier = Modifier.testTag("dialog_btn_cancel_periode")
                ) {
                    Text("BATAL", color = Slate600)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    deleteCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = {
                Text("Hapus Periode?", fontWeight = FontWeight.Bold, color = Slate800)
            },
            text = {
                Text("Apakah Anda yakin ingin menghapus periode '${candidate.periode}' (${candidate.keterangan})? Data yang sudah tersimpan dengan periode ini tidak akan terhapus.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePeriode(candidate.id)
                        deleteCandidate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_btn_delete_periode")
                ) {
                    Text("HAPUS", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deleteCandidate = null },
                    modifier = Modifier.testTag("cancel_btn_delete_periode")
                ) {
                    Text("BATAL", color = Slate600)
                }
            }
        )
    }
}
