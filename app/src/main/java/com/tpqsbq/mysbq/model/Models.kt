package com.tpqsbq.mysbq.model

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val nama: String = "",
    val role: String = "ustadz", // "admin" or "ustadz"
    val kelas: List<String> = emptyList(),
    val lembaga: String = ""
)

data class RiwayatItem(
    val kehadiran: String = "Hadir",
    val jilid: String = "-",
    val halaman: String = "-",
    val surah: String = "-",
    val ayat: String = "-",
    val nilaiHafalan: String = "-",
    val nilaiJilid: String = "-",
    val keterangan: String = "",
    val catatan: String = "",
    val timestamp: String = ""
)

data class PembayaranItem(
    val id: String = "",
    val jenis: String = "SPP", // "SPP", "Infaq", "Uang Gedung/Pendaftaran"
    val bulan: String = "-",   // "Januari" .. "Desember" or "-"
    val nominal: Long = 50000L,
    val tanggal: String = "",  // YYYY-MM-DD
    val catatan: String = "",
    val penerima: String = "",
    val timestamp: String = "",
    val updatedAt: String = ""
)

data class Santri(
    val id: String = "",
    val nama: String = "",
    val kelas: String = "",
    val kehadiran: String = "Hadir",
    val jilid: String = "-",
    val halaman: String = "-",
    val surah: String = "-",
    val ayat: String = "-",
    val nilaiHafalan: String = "Lancar",
    val nilaiJilid: String = "Lancar",
    val keterangan: String = "",
    val catatan: String = "",
    val timestamp: String = "",
    val riwayat: List<RiwayatItem> = emptyList(),
    val pembayaran: List<PembayaranItem> = emptyList()
)

data class Jurnal(
    val id: String = "",
    val kelas: String = "",
    val tanggal: String = "", // YYYY-MM-DD
    val tadarus: String = "",
    val murojaah: String = "",
    val materi: String = "",
    val timestamp: String = ""
)

data class ScanLogEntry(
    val nama: String,
    val studentId: String,
    val timeStr: String
)
