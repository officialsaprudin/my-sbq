package com.tpqsbq.mysbq.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpqsbq.mysbq.model.Santri
import com.tpqsbq.mysbq.util.AppUtils
import com.tpqsbq.mysbq.viewmodel.AppUiState

@Composable
fun DataSantriScreen(
    state: AppUiState,
    onFilterChanged: (String) -> Unit,
    onTambahClick: () -> Unit,
    onScanClick: () -> Unit,
    onMassBarcodeClick: (String) -> Unit,
    onOpenSppModal: (Santri) -> Unit,
    onShowBarcode: (Santri) -> Unit,
    onShowHistory: (Santri) -> Unit,
    onExportRapor: (Santri) -> Unit,
    onEditSantri: (Santri) -> Unit,
    onDeleteSantri: (Santri) -> Unit
) {
    val filterOptions = remember(state.availableClasses) {
        listOf("Semua") + state.availableClasses
    }
    val filteredSantri = remember(state.santriList, state.tabelKelasFilter) {
        state.santriList.filter {
            state.tabelKelasFilter == "Semua" || it.kelas == state.tabelKelasFilter
        }
    }
    val isWeekend = remember { AppUtils.isWeekendToday() }
    val isAdmin = state.currentUser.role == "admin"

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
                Text(
                    text = "Manajemen Data Santri",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1F2937)
                )

                DropdownSelector(
                    label = "Filter Kelas",
                    options = filterOptions,
                    selectedOption = state.tabelKelasFilter,
                    onOptionSelected = onFilterChanged
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.tabelKelasFilter != "Semua") {
                        Button(
                            onClick = { onMassBarcodeClick(state.tabelKelasFilter) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Barcode", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!isAdmin) {
                        Button(
                            onClick = onScanClick,
                            enabled = !isWeekend,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GreenSuccess,
                                disabledContainerColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isWeekend) Icons.Default.Lock else Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isWeekend) "Libur (Akhir Pekan)" else "Scan Absen",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onTambahClick,
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tambah", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (filteredSantri.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Belum ada data santri.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        } else {
            items(filteredSantri, key = { it.id }) { santri ->
                val waliKelas = state.pengajarMap[santri.kelas] ?: "-"
                val canModify = state.currentUser.role == "ustadz" &&
                    (state.currentUser.kelas.contains("Semua") || state.currentUser.kelas.contains(santri.kelas))

                SantriManagementCard(
                    santri = santri,
                    waliKelas = waliKelas,
                    canModify = canModify,
                    onOpenSpp = { onOpenSppModal(santri) },
                    onShowBarcode = { onShowBarcode(santri) },
                    onShowHistory = { onShowHistory(santri) },
                    onExportRapor = { onExportRapor(santri) },
                    onEdit = { onEditSantri(santri) },
                    onDelete = { onDeleteSantri(santri) }
                )
            }
        }
    }
}

@Composable
private fun SantriManagementCard(
    santri: Santri,
    waliKelas: String,
    canModify: Boolean,
    onOpenSpp: () -> Unit,
    onShowBarcode: () -> Unit,
    onShowHistory: () -> Unit,
    onExportRapor: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                        text = "ID: ${santri.id}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SppStatusBadge(
                        pembayaranList = santri.pembayaran,
                        onClick = onOpenSpp
                    )
                    StatusBadge(status = santri.kehadiran)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = santri.kelas.ifBlank { "-" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                )
                Text(
                    text = " • ",
                    color = Color.Gray
                )
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = waliKelas,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = IndigoPrimary
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color(0xFFF3F4F6)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hafalan", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = "${santri.surah} (Ayat ${santri.ayat})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Nilai: ", fontSize = 11.sp, color = Color.Gray)
                        NilaiBadge(ket = santri.nilaiHafalan.ifBlank { santri.keterangan })
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Jilid / Ngaji", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = "${santri.jilid} (Hal ${santri.halaman})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Nilai: ", fontSize = 11.sp, color = Color.Gray)
                        NilaiBadge(ket = santri.nilaiJilid.ifBlank { santri.keterangan })
                    }
                }
            }

            if (santri.catatan.isNotBlank() && santri.catatan != "-") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Catatan: ${santri.catatan}",
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF4B5563)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                color = Color(0xFFF3F4F6)
            )

            // Action Buttons Row (Including Wallet / SPP Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenSpp) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "SPP & Infaq",
                        tint = AmberPrimary
                    )
                }
                IconButton(onClick = onShowBarcode) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "Lihat Barcode",
                        tint = Color(0xFF374151)
                    )
                }
                IconButton(onClick = onShowHistory) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Riwayat Ngaji",
                        tint = GreenSuccess
                    )
                }
                IconButton(onClick = onExportRapor) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "Cetak Rapor PDF",
                        tint = RedDanger
                    )
                }
                if (canModify) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Progres",
                            tint = Color(0xFF2563EB)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Santri",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}
