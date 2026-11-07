package com.tpqsbq.mysbq.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tpqsbq.mysbq.model.Jurnal
import com.tpqsbq.mysbq.model.PembayaranItem
import com.tpqsbq.mysbq.model.Santri
import com.tpqsbq.mysbq.model.ScanLogEntry
import com.tpqsbq.mysbq.util.AppUtils
import java.util.Calendar
import java.util.Locale

@Composable
fun SantriFormDialog(
    editingSantri: Santri?,
    availableClasses: List<String>,
    defaultClass: String,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        nama: String,
        kelas: String,
        kehadiran: String,
        jilid: String,
        halaman: String,
        surah: String,
        ayat: String,
        nilaiHafalan: String,
        nilaiJilid: String,
        catatan: String
    ) -> Unit
) {
    val isEdit = editingSantri != null
    var nama by remember { mutableStateOf(editingSantri?.nama ?: "") }
    var kelas by remember {
        mutableStateOf(
            editingSantri?.kelas?.ifBlank { defaultClass } ?: defaultClass
        )
    }
    var isCustomKelas by remember { mutableStateOf(availableClasses.isEmpty()) }
    var kehadiran by remember { mutableStateOf(editingSantri?.kehadiran ?: "Hadir") }
    var jilid by remember { mutableStateOf(editingSantri?.jilid ?: "") }
    var halaman by remember { mutableStateOf(editingSantri?.halaman ?: "-") }
    var surah by remember { mutableStateOf(editingSantri?.surah ?: "-") }
    var ayat by remember { mutableStateOf(editingSantri?.ayat ?: "-") }
    var nilaiHafalan by remember {
        mutableStateOf(
            editingSantri?.nilaiHafalan?.ifBlank { editingSantri.keterangan.ifBlank { "Lancar" } } ?: "Lancar"
        )
    }
    var nilaiJilid by remember {
        mutableStateOf(
            editingSantri?.nilaiJilid?.ifBlank { editingSantri.keterangan.ifBlank { "Lancar" } } ?: "Lancar"
        )
    }
    var catatan by remember { mutableStateOf(editingSantri?.catatan ?: "") }

    val nilaiOptions = listOf("Lancar", "Kurang Lancar", "Mengulang", "-")
    val kehadiranOptions = listOf("Hadir", "Izin", "Sakit", "Alpa")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IndigoPrimary)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEdit) "Update Progres Santri" else "Tambah Santri Baru",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                // Body
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = nama,
                        onValueChange = { nama = it },
                        label = { Text("Nama Santri") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Kelas selector + option to type new class
                    if (!isCustomKelas && availableClasses.isNotEmpty()) {
                        DropdownSelector(
                            label = "Kelas / Rombel (Bisa Pindah Kelas)",
                            options = availableClasses,
                            selectedOption = kelas,
                            onOptionSelected = { kelas = it }
                        )
                        Text(
                            text = "+ Ketik Nama Kelas Baru",
                            color = IndigoPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { isCustomKelas = true }
                        )
                    } else {
                        OutlinedTextField(
                            value = kelas,
                            onValueChange = { kelas = it },
                            label = { Text("Nama Kelas / Rombel") },
                            placeholder = { Text("Cth: Kelas Madinah") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (availableClasses.isNotEmpty()) {
                            Text(
                                text = "Pilih dari Daftar Kelas yang Ada",
                                color = IndigoPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    isCustomKelas = false
                                    if (kelas.isBlank()) kelas = availableClasses.first()
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DropdownSelector(
                            label = "Kehadiran",
                            options = kehadiranOptions,
                            selectedOption = kehadiran,
                            onOptionSelected = { kehadiran = it },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = jilid,
                            onValueChange = { jilid = it },
                            label = { Text("Posisi Jilid") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = halaman,
                            onValueChange = { halaman = it },
                            label = { Text("Halaman (Ngaji)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = surah,
                            onValueChange = { surah = it },
                            label = { Text("Surah Hafalan") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = ayat,
                            onValueChange = { ayat = it },
                            label = { Text("Ayat Hafalan Terakhir") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        DropdownSelector(
                            label = "Nilai Hafalan",
                            options = nilaiOptions,
                            selectedOption = nilaiHafalan,
                            onOptionSelected = { nilaiHafalan = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    DropdownSelector(
                        label = "Nilai Jilid / Ngaji",
                        options = nilaiOptions,
                        selectedOption = nilaiJilid,
                        onOptionSelected = { nilaiJilid = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = catatan,
                        onValueChange = { catatan = it },
                        label = { Text("Catatan Tambahan (Opsional)") },
                        placeholder = { Text("Cth: Tajwid kurang, Sering lupa huruf Dlod") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Batal", color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onSave(
                                    editingSantri?.id,
                                    nama,
                                    kelas,
                                    kehadiran,
                                    jilid,
                                    halaman,
                                    surah,
                                    ayat,
                                    nilaiHafalan,
                                    nilaiJilid,
                                    catatan
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simpan Data", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SppDialog(
    santri: Santri,
    canManage: Boolean,
    onDismiss: () -> Unit,
    onSavePayment: (
        editPaymentId: String?,
        jenis: String,
        bulan: String,
        nominal: Long,
        tanggal: String,
        catatan: String
    ) -> Unit,
    onDeletePayment: (String) -> Unit,
    onPrintKuitansi: (PembayaranItem) -> Unit
) {
    val context = LocalContext.current
    val currMonthIdx = AppUtils.currentMonthIndex()
    val currYear = AppUtils.currentYear()

    val paidMonths = remember(santri.pembayaran) {
        santri.pembayaran.filter { it.jenis == "SPP" }.map { it.bulan }.toSet()
    }

    val oldestUnpaidMonth = remember(paidMonths, currMonthIdx) {
        var found: String? = null
        for (idx in 0..currMonthIdx) {
            val mName = AppUtils.NAMA_BULAN[idx]
            if (!paidMonths.contains(mName)) {
                found = mName
                break
            }
        }
        found ?: AppUtils.NAMA_BULAN[currMonthIdx]
    }

    val countNunggak = remember(paidMonths, currMonthIdx) {
        (0..currMonthIdx).count { !paidMonths.contains(AppUtils.NAMA_BULAN[it]) }
    }

    var editPaymentId by remember { mutableStateOf<String?>(null) }
    var jenis by remember { mutableStateOf("SPP") }
    var bulan by remember(oldestUnpaidMonth) { mutableStateOf(oldestUnpaidMonth) }
    var nominalStr by remember { mutableStateOf("50000") }
    var tanggal by remember { mutableStateOf(AppUtils.todayDateStr()) }
    var catatan by remember { mutableStateOf("") }
    var deleteConfirmPayment by remember { mutableStateOf<PembayaranItem?>(null) }

    val jenisOptions = listOf("SPP", "Infaq", "Uang Gedung")

    fun pickDate() {
        val parts = tanggal.split("-")
        val cal = Calendar.getInstance()
        val yr = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
        val mo = (parts.getOrNull(1)?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
        val dy = parts.getOrNull(2)?.toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(context, { _, y, m, d ->
            tanggal = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
        }, yr, mo, dy).show()
    }

    val totalNominal = remember(santri.pembayaran) {
        santri.pembayaran.sumOf { it.nominal }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Amber
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AmberPrimary)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SPP & Infaq: ${santri.nama} (${santri.kelas})",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Kartu Kontrol SPP 12 Bulan
                    Surface(
                        color = Color(0xFFF9FAFB),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Kartu Kontrol SPP ($currYear)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF374151)
                                )
                                if (countNunggak > 0) {
                                    Surface(
                                        color = Color(0xFFFEE2E2),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                                    ) {
                                        Text(
                                            text = "Nunggak: $oldestUnpaidMonth ($countNunggak Bln)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB91C1C),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        color = Color(0xFFD1FAE5),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFF6EE7B7))
                                    ) {
                                        Text(
                                            text = "Tertib / Lunas Bulan Ini",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF047857),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 3 rows x 4 columns for 12 months
                            for (rowIdx in 0 until 3) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = if (rowIdx < 2) 6.dp else 0.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (colIdx in 0 until 4) {
                                        val mIdx = rowIdx * 4 + colIdx
                                        val mName = AppUtils.NAMA_BULAN[mIdx]
                                        val shortName = mName.take(3)
                                        val isPaid = paidMonths.contains(mName)
                                        val isPastOrCurr = mIdx <= currMonthIdx

                                        val bgColor = when {
                                            isPaid -> Color(0xFFECFDF5)
                                            isPastOrCurr -> Color(0xFFFEF2F2)
                                            else -> Color.White
                                        }
                                        val borderColor = when {
                                            isPaid -> Color(0xFF6EE7B7)
                                            isPastOrCurr -> Color(0xFFFCA5A5)
                                            else -> Color(0xFFE5E7EB)
                                        }
                                        val txtColor = when {
                                            isPaid -> Color(0xFF047857)
                                            isPastOrCurr -> Color(0xFFDC2626)
                                            else -> Color(0xFF9CA3AF)
                                        }
                                        val subLabel = when {
                                            isPaid -> "Lunas"
                                            isPastOrCurr -> "Nunggak"
                                            else -> "Belum"
                                        }

                                        Surface(
                                            color = bgColor,
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, borderColor),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable(enabled = !isPaid && canManage) {
                                                    jenis = "SPP"
                                                    bulan = mName
                                                }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = shortName,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = txtColor
                                                )
                                                Text(
                                                    text = subLabel,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = txtColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Form Input / Edit Pembayaran (Jika ustadz kelas ybs)
                    if (canManage) {
                        val isEditing = editPaymentId != null
                        Surface(
                            color = if (isEditing) Color(0xFFEEF2FF) else AmberLight,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isEditing) Color(0xFFC7D2FE) else Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (isEditing) "Edit Catatan Pembayaran" else "Catat Pembayaran Baru",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEditing) IndigoDark else Color(0xFF92400E)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DropdownSelector(
                                        label = "Jenis Pembayaran",
                                        options = jenisOptions,
                                        selectedOption = jenis,
                                        onOptionSelected = { jenis = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (jenis == "SPP") {
                                        DropdownSelector(
                                            label = "Untuk Bulan",
                                            options = AppUtils.NAMA_BULAN,
                                            selectedOption = bulan,
                                            onOptionSelected = { bulan = it },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = nominalStr,
                                        onValueChange = { nominalStr = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Nominal (Rp)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White
                                        )
                                    )

                                    OutlinedCard(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { pickDate() },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Tanggal Bayar", fontSize = 10.sp, color = Color.Gray)
                                                Text(tanggal, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = AmberPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = catatan,
                                    onValueChange = { catatan = it },
                                    label = { Text("Catatan (Opsional)") },
                                    placeholder = { Text("Cth: Lunas via transfer / Titip ustadz") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val nom = nominalStr.toLongOrNull() ?: 0L
                                            if (nom > 0L) {
                                                onSavePayment(editPaymentId, jenis, bulan, nom, tanggal, catatan)
                                                editPaymentId = null
                                                nominalStr = "50000"
                                                catatan = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isEditing) IndigoPrimary else AmberPrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = if (isEditing) "Perbarui Transaksi" else "Simpan Pembayaran",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (isEditing) {
                                        OutlinedButton(
                                            onClick = {
                                                editPaymentId = null
                                                jenis = "SPP"
                                                nominalStr = "50000"
                                                tanggal = AppUtils.todayDateStr()
                                                catatan = ""
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Batal", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Riwayat Transaksi
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Riwayat Transaksi",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF374151)
                            )
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                            ) {
                                Text(
                                    text = "Total: ${AppUtils.formatRupiah(totalNominal)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (santri.pembayaran.isEmpty()) {
                            Surface(
                                color = Color(0xFFF9FAFB),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Belum ada riwayat pembayaran.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontStyle = FontStyle.Italic,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(18.dp)
                                )
                            }
                        } else {
                            santri.pembayaran.forEach { p ->
                                val ket = if (p.jenis == "SPP") "SPP (${p.bulan})" else p.jenis
                                Surface(
                                    color = Color(0xFFF9FAFB),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
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
                                                    text = ket,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1F2937)
                                                )
                                            }
                                            Text(
                                                text = AppUtils.formatRupiah(p.nominal),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = GreenSuccess
                                            )
                                            Text(
                                                text = "Penerima: ${p.penerima}",
                                                fontSize = 10.sp,
                                                color = Color(0xFF4B5563)
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

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            // Cetak Kuitansi PDF
                                            IconButton(
                                                onClick = { onPrintKuitansi(p) },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(Color(0xFFDBEAFE), RoundedCornerShape(6.dp))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Receipt,
                                                    contentDescription = "Cetak Kuitansi PDF",
                                                    tint = Color(0xFF2563EB),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            if (canManage) {
                                                // Edit
                                                IconButton(
                                                    onClick = {
                                                        editPaymentId = p.id
                                                        jenis = p.jenis
                                                        if (p.jenis == "SPP" && p.bulan != "-") {
                                                            bulan = p.bulan
                                                        }
                                                        nominalStr = p.nominal.toString()
                                                        tanggal = p.tanggal
                                                        catatan = p.catatan
                                                    },
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp))
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit Transaksi",
                                                        tint = Color(0xFFD97706),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                // Hapus
                                                IconButton(
                                                    onClick = { deleteConfirmPayment = p },
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(Color(0xFFFEE2E2), RoundedCornerShape(6.dp))
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Hapus Transaksi",
                                                        tint = RedDanger,
                                                        modifier = Modifier.size(16.dp)
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
        }
    }

    deleteConfirmPayment?.let { p ->
        AlertDialog(
            onDismissRequest = { deleteConfirmPayment = null },
            title = { Text("Hapus Transaksi Pembayaran", fontWeight = FontWeight.Bold) },
            text = {
                val ket = if (p.jenis == "SPP") "SPP (${p.bulan})" else p.jenis
                Text("Yakin ingin menghapus catatan pembayaran $ket sebesar ${AppUtils.formatRupiah(p.nominal)}?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editPaymentId == p.id) {
                            editPaymentId = null
                        }
                        onDeletePayment(p.id)
                        deleteConfirmPayment = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                ) {
                    Text("Hapus", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmPayment = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun HistoryDialog(
    santri: Santri,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 620.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IndigoPrimary)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Riwayat Ngaji: ${santri.nama}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                if (santri.riwayat.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Belum ada riwayat ngaji", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(santri.riwayat) { r ->
                            Surface(
                                color = Color(0xFFF9FAFB),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = AppUtils.formatDateTimeShort(r.timestamp),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4B5563)
                                        )
                                        StatusBadge(status = r.kehadiran)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Hafalan: ${r.surah} (${r.ayat})", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("H: ", fontSize = 10.sp, color = Color.Gray)
                                                NilaiBadge(ket = r.nilaiHafalan.ifBlank { r.keterangan })
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Jilid: ${r.jilid} (Hal ${r.halaman})", fontSize = 12.sp)
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
fun BarcodeDialog(
    santri: Santri,
    onDismiss: () -> Unit,
    onPrintPdf: () -> Unit
) {
    val barcodeBmp = remember(santri.id) {
        AppUtils.generateBarcodeBitmap(santri.id, 600, 200)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IndigoPrimary)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ID Card Santri",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = santri.nama,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Image(
                        bitmap = barcodeBmp.asImageBitmap(),
                        contentDescription = "Barcode ${santri.id}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Gunakan barcode ini untuk Absensi Scan",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Surface(
                    color = Color(0xFFF9FAFB),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onPrintPdf,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cetak Barcode (PDF)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditLembagaDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IndigoPrimary)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Nama Lembaga",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                Column(modifier = Modifier.padding(18.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Lembaga (Dashboard)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Batal", color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onSave(name) },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simpan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JurnalDialog(
    kelas: String,
    initialDate: String,
    jurnalList: List<Jurnal>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var tanggal by remember { mutableStateOf(initialDate) }

    val existing = remember(tanggal, kelas, jurnalList) {
        jurnalList.find { it.id == "${kelas}_${tanggal}" }
    }

    var tadarus by remember(existing) { mutableStateOf(existing?.tadarus ?: "") }
    var murojaah by remember(existing) { mutableStateOf(existing?.murojaah ?: "") }
    var materi by remember(existing) { mutableStateOf(existing?.materi ?: "") }

    fun pickDate() {
        val parts = tanggal.split("-")
        val cal = Calendar.getInstance()
        val yr = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
        val mo = (parts.getOrNull(1)?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
        val dy = parts.getOrNull(2)?.toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(context, { _, y, m, d ->
            tanggal = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
        }, yr, mo, dy).show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IndigoPrimary)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Jurnal Kelas: $kelas",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { pickDate() },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Tanggal Jurnal", fontSize = 11.sp, color = Color.Gray)
                                Text(tanggal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = IndigoPrimary)
                        }
                    }

                    OutlinedTextField(
                        value = tadarus,
                        onValueChange = { tadarus = it },
                        label = { Text("Tadarus Surah") },
                        placeholder = { Text("Cth: Al-Baqarah (1-10)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = murojaah,
                        onValueChange = { murojaah = it },
                        label = { Text("Murojaah (Surah & Ayat)") },
                        placeholder = { Text("Cth: An-Naba s/d An-Naziat") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = materi,
                        onValueChange = { materi = it },
                        label = { Text("Materi Pembelajaran") },
                        placeholder = { Text("Cth: Tajwid Hukum Nun Mati") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Batal", color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onSave(kelas, tanggal, tadarus, murojaah, materi) },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simpan Jurnal", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScannerDialog(
    scanLogs: List<ScanLogEntry>,
    onStartCameraScan: () -> Unit,
    onManualSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var manualId by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GreenSuccess)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scan Barcode / QR Absensi",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onStartCameraScan,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Buka Kamera Scanner Sekarang",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Input Manual Backup (Jika kartu lecek)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualId,
                            onValueChange = { manualId = it },
                            label = { Text("Atau Ketik ID Santri (Cth: ST123456)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (manualId.isNotBlank()) {
                                    onManualSubmit(manualId.trim())
                                    manualId = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Absen", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Rapid-Scan Log History
                    Surface(
                        color = Color(0xFFF9FAFB),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Log Scan Terakhir (Rapid-Scan):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            if (scanLogs.isEmpty()) {
                                Text(
                                    text = "Belum ada santri yang di-scan pada sesi ini.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontStyle = FontStyle.Italic
                                )
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(scanLogs) { log ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = GreenSuccess,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${log.nama} - Masuk (${log.timeStr})",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF1F2937)
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
}
