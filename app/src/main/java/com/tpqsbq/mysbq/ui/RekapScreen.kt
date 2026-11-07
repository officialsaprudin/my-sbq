package com.tpqsbq.mysbq.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpqsbq.mysbq.model.Jurnal
import com.tpqsbq.mysbq.model.PembayaranItem
import com.tpqsbq.mysbq.model.RiwayatItem
import com.tpqsbq.mysbq.util.AppUtils
import com.tpqsbq.mysbq.viewmodel.AppUiState
import java.util.Calendar
import java.util.Locale

@Composable
fun RekapScreen(
    state: AppUiState,
    onKelasFilterChanged: (String) -> Unit,
    onDateRangeChanged: (String, String) -> Unit,
    onApplyPreset: (String) -> Unit,
    onSetSemesterSiaFilter: (String, String) -> Unit,
    onOpenJurnalModal: (String, String?) -> Unit,
    onExportGlobalPdf: () -> Unit
) {
    val context = LocalContext.current
    val filterOptions = remember(state.availableClasses) {
        listOf("Semua") + state.availableClasses
    }

    val kelasRenderList = remember(state.rekapKelasFilter, state.availableClasses) {
        if (state.rekapKelasFilter == "Semua") state.availableClasses else listOf(state.rekapKelasFilter)
    }

    fun showDatePicker(currentVal: String, onPicked: (String) -> Unit) {
        val parts = currentVal.split("-")
        val cal = Calendar.getInstance()
        val yr = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
        val mo = (parts.getOrNull(1)?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
        val dy = parts.getOrNull(2)?.toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(context, { _, y, m, d ->
            val formatted = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
            onPicked(formatted)
        }, yr, mo, dy).show()
    }

    var presetDropdownExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header & Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Laporan Progres & Keuangan Santri",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1F2937),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onExportGlobalPdf,
                        colors = ButtonDefaults.buttonColors(containerColor = RedDanger),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export Global PDF",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Filter Kelas + Set Periode Cepat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DropdownSelector(
                        label = "Filter Kelas",
                        options = filterOptions,
                        selectedOption = state.rekapKelasFilter,
                        onOptionSelected = onKelasFilterChanged,
                        modifier = Modifier.weight(1f)
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { presetDropdownExpanded = true },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                            colors = CardDefaults.outlinedCardColors(containerColor = IndigoLight)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = IndigoDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Set Periode Cepat",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IndigoDark
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = presetDropdownExpanded,
                            onDismissRequest = { presetDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("🍂 Semester Ganjil (Jul - Des)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    presetDropdownExpanded = false
                                    onApplyPreset("ganjil")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🌱 Semester Genap (Jan - Jun)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    presetDropdownExpanded = false
                                    onApplyPreset("genap")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📅 Bulan Berjalan", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    presetDropdownExpanded = false
                                    onApplyPreset("bulan_ini")
                                }
                            )
                        }
                    }
                }

                // Date Range Pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DateSelectCard(
                        label = "Dari Tanggal",
                        dateStr = state.rekapTglMulai,
                        onClick = {
                            showDatePicker(state.rekapTglMulai) { picked ->
                                onDateRangeChanged(picked, state.rekapTglAkhir)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    DateSelectCard(
                        label = "Sampai Tanggal",
                        dateStr = state.rekapTglAkhir,
                        onClick = {
                            showDatePicker(state.rekapTglAkhir) { picked ->
                                onDateRangeChanged(state.rekapTglMulai, picked)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (kelasRenderList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Tidak ada kelas untuk ditampilkan.",
                        color = Color.Gray,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        } else {
            items(kelasRenderList, key = { it }) { kls ->
                RekapKelasSection(
                    kelas = kls,
                    state = state,
                    onSetSemesterSiaFilter = { semKey -> onSetSemesterSiaFilter(kls, semKey) },
                    onOpenJurnalModal = onOpenJurnalModal
                )
            }
        }
    }
}

@Composable
private fun DateSelectCard(
    label: String,
    dateStr: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text(dateStr, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
            }
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = IndigoPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun RekapKelasSection(
    kelas: String,
    state: AppUiState,
    onSetSemesterSiaFilter: (String) -> Unit,
    onOpenJurnalModal: (String, String?) -> Unit
) {
    val pengajar = state.pengajarMap[kelas] ?: "Belum diatur"
    val canEditJurnal = state.currentUser.role == "ustadz" &&
        (state.currentUser.kelas.contains("Semua") || state.currentUser.kelas.contains(kelas))

    val jurnalKelasIni = remember(state.jurnalList, kelas, state.rekapTglMulai, state.rekapTglAkhir) {
        state.jurnalList
            .filter { it.kelas == kelas && it.tanggal >= state.rekapTglMulai && it.tanggal <= state.rekapTglAkhir }
            .sortedByDescending { it.tanggal }
    }

    val chosenSemester = state.semesterFilterMap[kelas] ?: "rentang"
    val (siaTglMulai, siaTglAkhir, semesterLabel) = remember(chosenSemester, state.rekapTglMulai, state.rekapTglAkhir) {
        AppUtils.resolveSemesterDateRange(chosenSemester, state.rekapTglMulai, state.rekapTglAkhir)
    }

    data class SiaItem(val nama: String, val h: Int, val s: Int, val i: Int, val a: Int)
    data class ActivityItem(val nama: String, val riwayat: RiwayatItem)
    data class KeuanganRow(val namaSantri: String, val payment: PembayaranItem)

    val siaList = mutableListOf<SiaItem>()
    val activityList = mutableListOf<ActivityItem>()
    val keuanganList = mutableListOf<KeuanganRow>()
    var totalUangMasuk = 0L

    val santriKelas = state.santriList.filter { it.kelas == kelas }

    santriKelas.forEach { s ->
        // 1. Rekapitulasi Absensi (H/S/I/A) berdasarkan Filter Semester
        val rSemester = s.riwayat.filter { r ->
            val t = r.timestamp.take(10)
            t >= siaTglMulai && t <= siaTglAkhir
        }
        var cH = 0
        var cS = 0
        var cI = 0
        var cA = 0
        rSemester.forEach { r ->
            when (r.kehadiran) {
                "Hadir" -> cH++
                "Sakit" -> cS++
                "Izin" -> cI++
                "Alpa" -> cA++
            }
        }
        siaList.add(SiaItem(s.nama, cH, cS, cI, cA))

        // 2. Rincian Progres Ngaji Harian (Rentang Tanggal Atas)
        val rRange = s.riwayat.filter { r ->
            val t = r.timestamp.take(10)
            t >= state.rekapTglMulai && t <= state.rekapTglAkhir
        }
        rRange.forEach { r ->
            activityList.add(ActivityItem(s.nama, r))
        }

        // 3. Rekap Keuangan Kelas (SPP & Infaq) (Rentang Tanggal Atas)
        val pRange = s.pembayaran.filter { p ->
            p.tanggal >= state.rekapTglMulai && p.tanggal <= state.rekapTglAkhir
        }
        pRange.forEach { p ->
            totalUangMasuk += p.nominal
            keuanganList.add(KeuanganRow(s.nama, p))
        }
    }

    val currYear = AppUtils.currentYear()
    var semMenuExpanded by remember { mutableStateOf(false) }

    val semDisplayShort = when (chosenSemester) {
        "ganjil" -> "🍂 Sem. Ganjil ($currYear)"
        "genap" -> "🌱 Sem. Genap ($currYear)"
        "ganjil_prev" -> "🍂 Sem. Ganjil (${currYear - 1})"
        "semua" -> "📚 Semua Waktu"
        else -> "Rentang Tanggal"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Class Header
        Column {
            Text(
                text = kelas,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1F2937)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Pengajar: $pengajar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = IndigoPrimary
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 6.dp),
                thickness = 2.dp,
                color = Color(0xFFC7D2FE)
            )
        }

        // Rekap Keuangan (SPP & Infaq) Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Rekap Keuangan (SPP & Infaq)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF374151)
                        )
                    }
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                    ) {
                        Text(
                            text = "Total Kas: ${AppUtils.formatRupiah(totalUangMasuk)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (keuanganList.isEmpty()) {
                    Text(
                        text = "Tidak ada pemasukan SPP/Infaq di rentang tanggal ini.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontStyle = FontStyle.Italic
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        keuanganList.forEach { kRow ->
                            val p = kRow.payment
                            val ket = if (p.jenis == "SPP") "SPP (${p.bulan})" else p.jenis
                            Surface(
                                color = Color(0xFFFFFBEB),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = p.tanggal,
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = kRow.namaSantri,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1F2937)
                                            )
                                        }
                                        Text(
                                            text = "$ket • Penerima: ${p.penerima}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF92400E),
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (p.catatan.isNotBlank() && p.catatan != "-") {
                                            Text(
                                                text = "\"${p.catatan}\"",
                                                fontSize = 10.sp,
                                                fontStyle = FontStyle.Italic,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    Text(
                                        text = AppUtils.formatRupiah(p.nominal),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GreenSuccess
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Jurnal Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = IndigoLight)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kumpulan Jurnal",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF374151)
                    )
                    if (canEditJurnal) {
                        Button(
                            onClick = { onOpenJurnalModal(kelas, null) },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Jurnal Hari Ini", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (jurnalKelasIni.isEmpty()) {
                    Text(
                        text = "Belum ada jurnal untuk rentang tanggal ini.",
                        fontSize = 13.sp,
                        color = IndigoPrimary,
                        fontStyle = FontStyle.Italic
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        jurnalKelasIni.forEach { j ->
                            JurnalItemCard(
                                jurnal = j,
                                canEdit = canEditJurnal,
                                onEdit = { onOpenJurnalModal(kelas, j.tanggal) }
                            )
                        }
                    }
                }
            }
        }

        // SIA Summary Card with Per-Class Semester Filter
        if (siaList.isNotEmpty()) {
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Rekapitulasi Absensi",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF374151)
                            )
                            Text(
                                text = semesterLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IndigoPrimary
                            )
                        }

                        Box {
                            Surface(
                                color = IndigoLight,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                                modifier = Modifier.clickable { semMenuExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = null,
                                        tint = IndigoPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = semDisplayShort,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IndigoDark
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = semMenuExpanded,
                                onDismissRequest = { semMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Rentang Tanggal (Filter Atas)", fontSize = 12.sp) },
                                    onClick = {
                                        semMenuExpanded = false
                                        onSetSemesterSiaFilter("rentang")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🍂 Semester Ganjil ($currYear)", fontSize = 12.sp) },
                                    onClick = {
                                        semMenuExpanded = false
                                        onSetSemesterSiaFilter("ganjil")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🌱 Semester Genap ($currYear)", fontSize = 12.sp) },
                                    onClick = {
                                        semMenuExpanded = false
                                        onSetSemesterSiaFilter("genap")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🍂 Semester Ganjil (${currYear - 1})", fontSize = 12.sp) },
                                    onClick = {
                                        semMenuExpanded = false
                                        onSetSemesterSiaFilter("ganjil_prev")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📚 Semua Waktu (Akumulasi)", fontSize = 12.sp) },
                                    onClick = {
                                        semMenuExpanded = false
                                        onSetSemesterSiaFilter("semua")
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                            .padding(vertical = 8.dp, horizontal = 8.dp)
                    ) {
                        Text("Nama Santri", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                        Text("H", fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.6f))
                        Text("S", fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.6f))
                        Text("I", fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.6f))
                        Text("A", fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.6f))
                    }

                    siaList.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.nama,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937),
                                modifier = Modifier.weight(2f)
                            )
                            Text(
                                text = item.h.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenSuccess,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.6f)
                            )
                            Text(
                                text = item.s.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangeSick,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.6f)
                            )
                            Text(
                                text = item.i.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = YellowWarn,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.6f)
                            )
                            Text(
                                text = item.a.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedDanger,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.6f)
                            )
                        }
                        HorizontalDivider(color = Color(0xFFF3F4F6))
                    }
                }
            }
        }

        // Detailed Activity Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Rincian Progres Ngaji",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF374151)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (activityList.isEmpty()) {
                    Text(
                        text = "Tidak ada data ngaji di rentang tanggal ini.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        activityList.forEach { act ->
                            val r = act.riwayat
                            val tglCetak = AppUtils.formatDateSlashShort(r.timestamp)
                            Surface(
                                color = Color(0xFFF9FAFB),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = Color(0xFFE0E7FF),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = tglCetak,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = IndigoPrimary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = act.nama,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1F2937)
                                            )
                                        }
                                        StatusBadge(status = r.kehadiran)
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Hafalan: ${r.surah} (${r.ayat})",
                                                fontSize = 12.sp,
                                                color = Color(0xFF374151)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("H: ", fontSize = 10.sp, color = Color.Gray)
                                                NilaiBadge(ket = r.nilaiHafalan.ifBlank { r.keterangan })
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Jilid: ${r.jilid} (Hal ${r.halaman})",
                                                fontSize = 12.sp,
                                                color = Color(0xFF374151)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("J: ", fontSize = 10.sp, color = Color.Gray)
                                                NilaiBadge(ket = r.nilaiJilid.ifBlank { r.keterangan })
                                            }
                                        }
                                    }

                                    if (r.catatan.isNotBlank() && r.catatan != "-") {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Catatan: ${r.catatan}",
                                            fontSize = 11.sp,
                                            fontStyle = FontStyle.Italic,
                                            color = Color.Gray
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

@Composable
private fun JurnalItemCard(
    jurnal: Jurnal,
    canEdit: Boolean,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Jurnal Tgl: ${jurnal.tanggal}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = IndigoDark
                )
                if (canEdit) {
                    Surface(
                        color = IndigoPrimary,
                        shape = RoundedCornerShape(4.dp),
                        onClick = onEdit
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Isi/Edit Jurnal",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color(0xFFF3F4F6)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                JurnalFieldBox("Tadarus", jurnal.tadarus, Modifier.weight(1f))
                JurnalFieldBox("Murojaah", jurnal.murojaah, Modifier.weight(1f))
                JurnalFieldBox("Materi", jurnal.materi, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun JurnalFieldBox(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFFF9FAFB),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = IndigoPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value.ifBlank { "-" },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2937)
            )
        }
    }
}
