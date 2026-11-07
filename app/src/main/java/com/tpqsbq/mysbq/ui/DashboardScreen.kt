package com.tpqsbq.mysbq.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpqsbq.mysbq.model.Santri
import com.tpqsbq.mysbq.util.AppUtils
import com.tpqsbq.mysbq.viewmodel.AppUiState

@Composable
fun DashboardScreen(
    state: AppUiState,
    onFilterChanged: (String) -> Unit,
    onEditLembagaClick: () -> Unit,
    onOpenSppModal: (Santri) -> Unit
) {
    val filterOptions = remember(state.availableClasses) {
        listOf("Semua") + state.availableClasses
    }
    val filteredSantri = remember(state.santriList, state.dashKelasFilter) {
        state.santriList.filter {
            state.dashKelasFilter == "Semua" || it.kelas == state.dashKelasFilter
        }
    }

    val totalCount = filteredSantri.size
    val hadirCount = filteredSantri.count { it.kehadiran == "Hadir" }
    val izinCount = filteredSantri.count { it.kehadiran == "Izin" || it.kehadiran == "Sakit" }
    val alpaCount = filteredSantri.count { it.kehadiran == "Alpa" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.currentLembagaName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1F2937),
                        modifier = Modifier.weight(1f)
                    )
                    if (state.currentUser.role == "admin") {
                        FilledTonalButton(
                            onClick = onEditLembagaClick,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Lembaga",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                DropdownSelector(
                    label = "Filter Kelas (Dashboard)",
                    options = filterOptions,
                    selectedOption = state.dashKelasFilter,
                    onOptionSelected = onFilterChanged
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryStatCard(
                        title = "Total Santri",
                        value = totalCount.toString(),
                        accentColor = IndigoPrimary,
                        valueColor = Color(0xFF1F2937),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatCard(
                        title = "Hadir",
                        value = hadirCount.toString(),
                        accentColor = GreenSuccess,
                        valueColor = GreenSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryStatCard(
                        title = "Izin/Sakit",
                        value = izinCount.toString(),
                        accentColor = YellowWarn,
                        valueColor = YellowWarn,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatCard(
                        title = "Alpa",
                        value = alpaCount.toString(),
                        accentColor = RedDanger,
                        valueColor = RedDanger,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Kartu Progres Santri (Bintang Mingguan)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151)
            )
        }

        if (filteredSantri.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Belum ada data santri untuk kelas ini.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        } else {
            items(filteredSantri, key = { it.id }) { santri ->
                SantriDashboardCard(
                    santri = santri,
                    onSppBadgeClick = { onOpenSppModal(santri) }
                )
            }
        }
    }
}

@Composable
private fun SummaryStatCard(
    title: String,
    value: String,
    accentColor: Color,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )
            Column(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = valueColor
                )
            }
        }
    }
}

@Composable
private fun SantriDashboardCard(
    santri: Santri,
    onSppBadgeClick: () -> Unit
) {
    val borderColor = when (santri.kehadiran) {
        "Hadir" -> GreenSuccess
        "Alpa" -> RedDanger
        else -> YellowWarn
    }
    val starStatus = remember(santri.riwayat) {
        AppUtils.calculateWeeklyStars(santri.riwayat)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(borderColor)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = santri.nama,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        Text(
                            text = santri.kelas.ifBlank { "-" },
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    StatusBadge(status = santri.kehadiran)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Bintang Sen-Jum: ",
                        fontSize = 13.sp,
                        color = Color(0xFF374151),
                        fontWeight = FontWeight.Medium
                    )
                    when {
                        starStatus.isBurned -> {
                            Surface(
                                color = Color(0xFFFEE2E2),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Hangus (Ada Absen)",
                                    color = Color(0xFFEF4444),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        starStatus.stars > 0 -> {
                            Text(
                                text = "⭐".repeat(starStatus.stars),
                                fontSize = 14.sp
                            )
                        }
                        else -> {
                            Text(
                                text = "Belum ada bintang",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color(0xFF818CF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${santri.jilid} - Hal ${santri.halaman}",
                        fontSize = 13.sp,
                        color = Color(0xFF4B5563)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = Color(0xFF818CF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${santri.surah} (Ayat ${santri.ayat})",
                        fontSize = 13.sp,
                        color = Color(0xFF4B5563)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // SPP Badge Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = AmberPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    SppStatusBadge(
                        pembayaranList = santri.pembayaran,
                        onClick = onSppBadgeClick
                    )
                }
            }
        }
    }
}
