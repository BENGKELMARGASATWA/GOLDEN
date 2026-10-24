package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DataAuditEntity
import com.example.data.local.entity.MasterKendaraanEntity
import com.example.data.local.entity.MasterPoolEntity
import com.example.ui.theme.Amber50
import com.example.ui.theme.Amber600
import com.example.ui.theme.Blue50
import com.example.ui.theme.Blue700
import com.example.ui.theme.Blue900
import com.example.ui.theme.HighDensityBlue
import com.example.ui.theme.HighDensityBlueBorder
import com.example.ui.theme.HighDensityBlueLight
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate800
import com.example.ui.theme.Teal50
import com.example.ui.theme.Teal600

@Composable
fun MonitoringPoolScreen(
    pools: List<MasterPoolEntity>,
    vehicles: List<MasterKendaraanEntity>,
    audits: List<DataAuditEntity>,
    onNavigateToHasilAudit: (String) -> Unit,
    onSyncAll: () -> Unit
) {
    val totalAudits = audits.size
    val unsyncedAudits = audits.count { !it.isSynced }
    val activePoolsCount = pools.size

    val poolStats = remember(pools, vehicles, audits) {
        pools.map { pool ->
            val poolVehicles = vehicles.filter { it.pool.equals(pool.kodePool, ignoreCase = true) }
            val poolAudits = audits.filter { it.pool.equals(pool.kodePool, ignoreCase = true) }
            Triple(pool, poolVehicles.size, poolAudits.size)
        }.sortedWith(
            compareByDescending<Triple<MasterPoolEntity, Int, Int>> { it.third } // Urutkan jumlah sample terbanyak ke sedikit
                .thenByDescending { it.second } // Jika jumlah sample sama, urutkan jumlah armada terbanyak
                .thenBy { it.first.kodePool }   // Lalu berdasarkan kode pool
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Header Card (Ramping & Streamlined)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MONITORING AUDIT ALL POOL",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = HighDensityBlue,
                                fontSize = 15.sp,
                                letterSpacing = (-0.2).sp
                            )
                        )

                        Button(
                            onClick = onSyncAll,
                            colors = ButtonDefaults.buttonColors(containerColor = Blue700),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sinkronkan Cloud", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Slate200, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // 4 KPI Summary Cards (Ramping)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KpiSummaryBox(
                            title = "Total Pool",
                            value = "$activePoolsCount Pool",
                            bgColor = HighDensityBlueLight,
                            borderColor = HighDensityBlueBorder,
                            textColor = HighDensityBlue,
                            modifier = Modifier.weight(1f)
                        )

                        KpiSummaryBox(
                            title = "Total Armada",
                            value = "${vehicles.size} Unit",
                            bgColor = HighDensityBlueLight,
                            borderColor = HighDensityBlueBorder,
                            textColor = HighDensityBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KpiSummaryBox(
                            title = "Hasil Audit",
                            value = "$totalAudits Selesai",
                            bgColor = Teal50,
                            borderColor = Color(0xFF99F6E4),
                            textColor = Teal600,
                            modifier = Modifier.weight(1f)
                        )

                        KpiSummaryBox(
                            title = "Pending Sync",
                            value = "$unsyncedAudits Offline",
                            bgColor = if (unsyncedAudits == 0) Teal50 else Amber50,
                            borderColor = if (unsyncedAudits == 0) Color(0xFF99F6E4) else Color(0xFFFDE68A),
                            textColor = if (unsyncedAudits == 0) Teal600 else Amber600,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Section Title
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Statistik Per Pool ($activePoolsCount Pool)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate800,
                        fontSize = 13.sp
                    )
                )
                Text(
                    text = "Urutan: Sample Terbanyak",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Slate600,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // Pool Stats List (Ramping & Sleek, diurutkan dari sample terbanyak ke sedikit)
        itemsIndexed(poolStats, key = { _, item -> item.first.id }) { index, (pool, vehicleCount, auditCount) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToHasilAudit(pool.kodePool) }
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
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
                                    .size(34.dp)
                                    .background(if (index < 3 && auditCount > 0) Color(0xFFEFF6FF) else HighDensityBlueLight, CircleShape)
                                    .border(1.dp, if (index < 3 && auditCount > 0) Blue700 else HighDensityBlueBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#${index + 1}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = if (index < 3 && auditCount > 0) Blue700 else HighDensityBlue,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${pool.kodePool} - ${pool.namaPool}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Slate800,
                                        fontSize = 13.sp
                                    ),
                                    maxLines = 1
                                )
                                Text(
                                    text = pool.alamatPool.ifBlank { "Kawasan Operasional" },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Slate600,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Lihat Detail",
                            tint = Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Slate100, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Armada: $vehicleCount Unit • Sample: $auditCount",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Slate600,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )

                        Surface(
                            color = if (auditCount > 0) Teal50 else Slate100,
                            shape = RoundedCornerShape(4.dp),
                            border = if (auditCount > 0) BorderStroke(0.6.dp, Color(0xFF99F6E4)) else null
                        ) {
                            Text(
                                text = if (auditCount > 0) "$auditCount Sample" else "0 Sample",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (auditCount > 0) Teal600 else Slate600,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiSummaryBox(
    title: String,
    value: String,
    bgColor: Color,
    borderColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Slate600,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    fontSize = 15.sp
                )
            )
        }
    }
}
