package com.tpqsbq.mysbq.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tpqsbq.mysbq.data.FirebaseService
import com.tpqsbq.mysbq.model.Jurnal
import com.tpqsbq.mysbq.model.PembayaranItem
import com.tpqsbq.mysbq.model.RiwayatItem
import com.tpqsbq.mysbq.model.Santri
import com.tpqsbq.mysbq.model.ScanLogEntry
import com.tpqsbq.mysbq.model.UserProfile
import com.tpqsbq.mysbq.util.AppUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

data class AppUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val authError: String? = null,
    val toastMessage: String? = null,
    val isToastError: Boolean = false,

    val currentUser: UserProfile = UserProfile(),
    val currentLembagaName: String = "Aplikasi Progres Santri",
    val usersList: List<UserProfile> = emptyList(),
    val pengajarMap: Map<String, String> = emptyMap(),

    val santriList: List<Santri> = emptyList(),
    val jurnalList: List<Jurnal> = emptyList(),
    val availableClasses: List<String> = emptyList(),

    // Filters
    val dashKelasFilter: String = "Semua",
    val tabelKelasFilter: String = "Semua",
    val rekapKelasFilter: String = "Semua",
    val rekapTglMulai: String = AppUtils.daysAgoDateStr(6),
    val rekapTglAkhir: String = AppUtils.todayDateStr(),
    val semesterFilterMap: Map<String, String> = emptyMap(), // per-class SIA semester filter

    // Rapid Scan Log
    val scanLogs: List<ScanLogEntry> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseService = FirebaseService(application.applicationContext)

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var syncJob: Job? = null
    private var autoAlpaChecked = false
    private var initialDefaultFilterApplied = false

    // Smart-Cooldown 2 menit per ID Santri (120_000 ms)
    private val scanHistoryMap = mutableMapOf<String, Long>()

    init {
        if (firebaseService.isLoggedIn()) {
            _uiState.update { it.copy(isLoggedIn = true, isLoading = true) }
            viewModelScope.launch {
                firebaseService.refreshAuthToken()
                loadInitialProfileAndStartSync()
            }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun showToast(msg: String, isError: Boolean = false) {
        _uiState.update { it.copy(toastMessage = msg, isToastError = isError) }
    }

    fun canManageClass(kelas: String): Boolean {
        val user = _uiState.value.currentUser
        if (user.role != "ustadz") return false
        return user.kelas.contains("Semua") || user.kelas.contains(kelas)
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(authError = "Email dan Password wajib diisi") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, authError = null) }
            val res = firebaseService.signIn(email, password)
            res.onSuccess {
                autoAlpaChecked = false
                initialDefaultFilterApplied = false
                _uiState.update { state -> state.copy(isLoggedIn = true, authError = null) }
                loadInitialProfileAndStartSync()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        authError = "Gagal masuk: ${err.message ?: "Periksa email/password"}"
                    )
                }
            }
        }
    }

    fun register(email: String, password: String, nama: String, kelasInput: String) {
        if (email.isBlank() || password.isBlank() || nama.isBlank()) {
            _uiState.update { it.copy(authError = "Semua kolom pendaftaran wajib diisi") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, authError = null) }
            val res = firebaseService.signUp(email, password, nama, kelasInput)
            res.onSuccess { profile ->
                autoAlpaChecked = false
                initialDefaultFilterApplied = false
                _uiState.update {
                    it.copy(
                        isLoggedIn = true,
                        currentUser = profile,
                        authError = null
                    )
                }
                loadInitialProfileAndStartSync()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        authError = "Gagal daftar: ${err.message}"
                    )
                }
            }
        }
    }

    fun logout() {
        syncJob?.cancel()
        firebaseService.clearSession()
        autoAlpaChecked = false
        initialDefaultFilterApplied = false
        _uiState.value = AppUiState(isLoggedIn = false)
    }

    private suspend fun loadInitialProfileAndStartSync() {
        val uid = firebaseService.currentUid ?: return
        val email = firebaseService.currentEmail ?: ""
        val fetchedProfile = firebaseService.getUserProfile(uid)
        val profile = if (fetchedProfile != null) {
            val isRealAdmin = FirebaseService.ADMIN_EMAILS.contains(email.lowercase())
            val effectiveRole = if (!isRealAdmin && fetchedProfile.role == "admin") "ustadz" else fetchedProfile.role
            fetchedProfile.copy(
                role = effectiveRole,
                kelas = fetchedProfile.kelas.ifEmpty { listOf("Kelas Madinah") }
            )
        } else {
            val role = if (FirebaseService.ADMIN_EMAILS.contains(email.lowercase())) "admin" else "ustadz"
            val defaultKls = if (role == "admin") listOf("Semua") else listOf("Kelas Madinah", "Semua")
            UserProfile(uid = uid, email = email, role = role, kelas = defaultKls)
        }
        _uiState.update { it.copy(currentUser = profile) }

        syncAllData()

        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            while (isActive) {
                delay(10_000L)
                syncAllData(silent = true)
            }
        }
    }

    fun syncAllData(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _uiState.update { it.copy(isLoading = true) }
            try {
                val users = firebaseService.fetchAllUsers()
                val santri = firebaseService.fetchAllSantri()
                val jurnal = firebaseService.fetchAllJurnal()

                val pMap = mutableMapOf<String, String>()
                var lembagaName = _uiState.value.currentLembagaName
                for (u in users) {
                    if (u.role == "admin" && u.lembaga.isNotBlank()) {
                        lembagaName = u.lembaga
                    }
                    if (u.nama.isNotBlank()) {
                        for (k in u.kelas) {
                            if (k.isNotBlank() && k != "Semua") {
                                pMap[k] = u.nama
                            }
                        }
                    }
                }

                val currentProfile = users.find { it.uid == firebaseService.currentUid }
                    ?: _uiState.value.currentUser

                val clsSet = sortedSetOf<String>()
                santri.forEach { if (it.kelas.isNotBlank()) clsSet.add(it.kelas) }
                if (currentProfile.role == "admin") {
                    pMap.keys.forEach { clsSet.add(it) }
                } else {
                    currentProfile.kelas.filter { it.isNotBlank() && it != "Semua" }.forEach { clsSet.add(it) }
                }
                val classesList = clsSet.toList()

                val firstSpecificClass = currentProfile.kelas.firstOrNull { it != "Semua" }
                val defaultKelas = if (currentProfile.role == "ustadz" && !firstSpecificClass.isNullOrBlank()) {
                    firstSpecificClass
                } else {
                    "Semua"
                }

                _uiState.update { old ->
                    val dashF = if (!initialDefaultFilterApplied) defaultKelas else if (old.dashKelasFilter == "Semua" || classesList.contains(old.dashKelasFilter)) old.dashKelasFilter else defaultKelas
                    val tabF = if (!initialDefaultFilterApplied) defaultKelas else if (old.tabelKelasFilter == "Semua" || classesList.contains(old.tabelKelasFilter)) old.tabelKelasFilter else defaultKelas
                    val rekF = if (!initialDefaultFilterApplied) defaultKelas else if (old.rekapKelasFilter == "Semua" || classesList.contains(old.rekapKelasFilter)) old.rekapKelasFilter else defaultKelas

                    old.copy(
                        isLoading = false,
                        currentUser = currentProfile,
                        currentLembagaName = lembagaName,
                        usersList = users,
                        pengajarMap = pMap,
                        santriList = santri,
                        jurnalList = jurnal,
                        availableClasses = classesList,
                        dashKelasFilter = dashF,
                        tabelKelasFilter = tabF,
                        rekapKelasFilter = rekF
                    )
                }
                initialDefaultFilterApplied = true

                if (!autoAlpaChecked) {
                    autoAlpaChecked = true
                    runAutoAlpaIfNeeded(santri, currentProfile)
                }
            } catch (e: Exception) {
                if (!silent) {
                    _uiState.update { it.copy(isLoading = false) }
                    showToast("Gagal sinkronisasi: ${e.message}", isError = true)
                }
            }
        }
    }

    private suspend fun runAutoAlpaIfNeeded(santriList: List<Santri>, user: UserProfile) {
        if (AppUtils.isWeekendToday()) return
        if (user.role != "ustadz") return

        val todayStr = AppUtils.todayDateStr()
        val nowIso = AppUtils.nowIsoString()
        var anyUpdated = false

        for (s in santriList) {
            val canManage = user.kelas.contains("Semua") || user.kelas.contains(s.kelas)
            if (!canManage) continue
            val hasToday = s.riwayat.any { it.timestamp.take(10) == todayStr }
            if (!hasToday) {
                val newEntry = RiwayatItem(
                    kehadiran = "Alpa",
                    jilid = s.jilid.ifBlank { "-" },
                    halaman = s.halaman.ifBlank { "-" },
                    surah = s.surah.ifBlank { "-" },
                    ayat = s.ayat.ifBlank { "-" },
                    nilaiHafalan = "-",
                    nilaiJilid = "-",
                    catatan = "Belum Scan Absen",
                    timestamp = nowIso
                )
                val updatedRiwayat = listOf(newEntry) + s.riwayat
                val updatedSantri = s.copy(
                    kehadiran = "Alpa",
                    riwayat = updatedRiwayat
                )
                try {
                    firebaseService.saveSantri(updatedSantri)
                    anyUpdated = true
                } catch (_: Exception) {
                }
            }
        }
        if (anyUpdated) {
            syncAllData(silent = true)
        }
    }

    // --- FILTERS & PRESETS ---

    fun setDashKelasFilter(kelas: String) {
        _uiState.update { it.copy(dashKelasFilter = kelas) }
    }

    fun setTabelKelasFilter(kelas: String) {
        _uiState.update { it.copy(tabelKelasFilter = kelas) }
    }

    fun setRekapKelasFilter(kelas: String) {
        _uiState.update { it.copy(rekapKelasFilter = kelas) }
    }

    fun setRekapDateRange(mulai: String, akhir: String) {
        _uiState.update { it.copy(rekapTglMulai = mulai, rekapTglAkhir = akhir) }
    }

    /**
     * Ported from `applyTopSemesterPreset(val)` in index.html
     */
    fun applyTopSemesterPreset(preset: String) {
        val currYear = AppUtils.currentYear()
        when (preset) {
            "ganjil" -> setRekapDateRange("$currYear-07-01", "$currYear-12-31")
            "genap" -> setRekapDateRange("$currYear-01-01", "$currYear-06-30")
            "bulan_ini" -> {
                val cal = Calendar.getInstance()
                val y = cal.get(Calendar.YEAR)
                val m = cal.get(Calendar.MONTH) + 1
                val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val mStr = String.format(Locale.US, "%02d", m)
                setRekapDateRange("$y-$mStr-01", "$y-$mStr-$lastDay")
            }
        }
    }

    /**
     * Ported from `setSemesterSIA(kls, val)` in index.html
     */
    fun setSemesterSiaFilter(kelas: String, semesterKey: String) {
        _uiState.update { old ->
            val newMap = old.semesterFilterMap.toMutableMap()
            newMap[kelas] = semesterKey
            old.copy(semesterFilterMap = newMap)
        }
    }

    // --- CRUD SANTRI ---

    fun saveSantri(
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
    ) {
        val state = _uiState.value
        if (state.currentUser.role == "admin") return
        if (nama.isBlank() || kelas.isBlank()) {
            showToast("Nama Santri dan Kelas wajib diisi!", isError = true)
            return
        }

        val id = if (existingId.isNullOrBlank()) {
            "ST" + Random.nextInt(100000, 999999)
        } else {
            existingId
        }
        val nowIso = AppUtils.nowIsoString()
        val todayStr = nowIso.take(10)

        val oldDoc = state.santriList.find { it.id == id }
        val riwayatMut = oldDoc?.riwayat?.toMutableList() ?: mutableListOf()
        val pembayaranExisting = oldDoc?.pembayaran ?: emptyList()

        val newEntry = RiwayatItem(
            kehadiran = kehadiran,
            jilid = jilid.ifBlank { "-" },
            halaman = halaman.ifBlank { "-" },
            surah = surah.ifBlank { "-" },
            ayat = ayat.ifBlank { "-" },
            nilaiHafalan = nilaiHafalan,
            nilaiJilid = nilaiJilid,
            catatan = catatan,
            timestamp = nowIso
        )

        val existingIdx = riwayatMut.indexOfFirst { it.timestamp.take(10) == todayStr }
        if (existingIdx != -1) {
            riwayatMut[existingIdx] = newEntry
        } else {
            riwayatMut.add(0, newEntry)
        }

        val newSantri = Santri(
            id = id,
            nama = nama.trim(),
            kelas = kelas.trim(),
            kehadiran = kehadiran,
            jilid = jilid.ifBlank { "-" },
            halaman = halaman.ifBlank { "-" },
            surah = surah.ifBlank { "-" },
            ayat = ayat.ifBlank { "-" },
            nilaiHafalan = nilaiHafalan,
            nilaiJilid = nilaiJilid,
            catatan = catatan.trim(),
            timestamp = nowIso,
            riwayat = riwayatMut,
            pembayaran = pembayaranExisting
        )

        viewModelScope.launch {
            try {
                firebaseService.saveSantri(newSantri)
                showToast("Berhasil disimpan!")
                syncAllData(silent = true)
            } catch (e: Exception) {
                showToast("Gagal menyimpan: ${e.message}", isError = true)
            }
        }
    }

    fun deleteSantri(santri: Santri) {
        if (!canManageClass(santri.kelas)) {
            showToast("Akses ditolak: Bukan santri kelas Anda", isError = true)
            return
        }
        viewModelScope.launch {
            try {
                firebaseService.deleteSantri(santri.id)
                showToast("Data santri dihapus")
                syncAllData(silent = true)
            } catch (e: Exception) {
                showToast("Gagal menghapus: ${e.message}", isError = true)
            }
        }
    }

    // --- SPP & INFAQ CRUD ---

    fun saveSppPayment(
        santriId: String,
        editPaymentId: String?,
        jenis: String,
        bulan: String,
        nominal: Long,
        tanggal: String,
        catatan: String
    ) {
        val state = _uiState.value
        val santri = state.santriList.find { it.id == santriId } ?: return
        if (!canManageClass(santri.kelas)) {
            showToast("Akses Ditolak", isError = true)
            return
        }

        val penerima = state.pengajarMap[santri.kelas]
            ?: if (state.currentUser.role == "ustadz") state.currentUser.nama.ifBlank { "Ustadz Kelas" } else "Bendahara"

        val updatedList = if (!editPaymentId.isNullOrBlank()) {
            santri.pembayaran.map { p ->
                if (p.id == editPaymentId) {
                    p.copy(
                        jenis = jenis,
                        bulan = if (jenis == "SPP") bulan else "-",
                        nominal = nominal,
                        tanggal = tanggal,
                        catatan = catatan.trim(),
                        updatedAt = AppUtils.nowIsoString()
                    )
                } else {
                    p
                }
            }
        } else {
            val newPayment = PembayaranItem(
                id = "PAY_${System.currentTimeMillis()}",
                jenis = jenis,
                bulan = if (jenis == "SPP") bulan else "-",
                nominal = nominal,
                tanggal = tanggal,
                catatan = catatan.trim(),
                penerima = penerima,
                timestamp = AppUtils.nowIsoString()
            )
            listOf(newPayment) + santri.pembayaran
        }

        // Optimistic UI update
        _uiState.update { old ->
            old.copy(
                santriList = old.santriList.map {
                    if (it.id == santriId) it.copy(pembayaran = updatedList) else it
                }
            )
        }

        viewModelScope.launch {
            try {
                firebaseService.updateSantriPembayaran(santriId, updatedList)
                val msg = if (!editPaymentId.isNullOrBlank()) {
                    "Perubahan pembayaran berhasil disimpan!"
                } else {
                    "Pembayaran berhasil disimpan ke database!"
                }
                showToast(msg)
                syncAllData(silent = true)
            } catch (e: Exception) {
                showToast("Gagal menyimpan pembayaran: ${e.message}", isError = true)
            }
        }
    }

    fun deleteSppPayment(santriId: String, paymentId: String) {
        val state = _uiState.value
        val santri = state.santriList.find { it.id == santriId } ?: return
        if (!canManageClass(santri.kelas)) {
            showToast("Akses Ditolak", isError = true)
            return
        }

        val updatedList = santri.pembayaran.filter { it.id != paymentId }

        _uiState.update { old ->
            old.copy(
                santriList = old.santriList.map {
                    if (it.id == santriId) it.copy(pembayaran = updatedList) else it
                }
            )
        }

        viewModelScope.launch {
            try {
                firebaseService.updateSantriPembayaran(santriId, updatedList)
                showToast("Catatan pembayaran dihapus")
                syncAllData(silent = true)
            } catch (e: Exception) {
                showToast("Gagal menghapus pembayaran: ${e.message}", isError = true)
            }
        }
    }

    // --- BARCODE SCANNER HANDLER ---

    fun onBarcodeScanned(rawValue: String) {
        val studentId = rawValue.trim()
        val now = System.currentTimeMillis()

        val lastScan = scanHistoryMap[studentId]
        if (lastScan != null && (now - lastScan) < 120_000L) {
            showToast("Santri ini baru saja di-scan (< 2 menit lalu)")
            return
        }

        val state = _uiState.value
        val santri = state.santriList.find { it.id == studentId }
        if (santri == null) {
            showToast("Santri ($studentId) tidak ditemukan!", isError = true)
            return
        }

        if (!canManageClass(santri.kelas)) {
            showToast("Bukan santri kelas Anda.", isError = true)
            return
        }

        scanHistoryMap[studentId] = now
        val nowIso = AppUtils.nowIsoString()
        val todayStr = nowIso.take(10)
        val timeStr = AppUtils.currentTimeStr()

        val riwayatMut = santri.riwayat.toMutableList()
        val existingIdx = riwayatMut.indexOfFirst { it.timestamp.take(10) == todayStr }

        if (existingIdx != -1) {
            val oldItem = riwayatMut[existingIdx]
            riwayatMut[existingIdx] = oldItem.copy(
                kehadiran = "Hadir",
                timestamp = nowIso,
                catatan = "Hadir via Scan Barcode"
            )
        } else {
            riwayatMut.add(
                0,
                RiwayatItem(
                    kehadiran = "Hadir",
                    jilid = santri.jilid.ifBlank { "-" },
                    halaman = santri.halaman.ifBlank { "-" },
                    surah = santri.surah.ifBlank { "-" },
                    ayat = santri.ayat.ifBlank { "-" },
                    nilaiHafalan = santri.nilaiHafalan.ifBlank { "-" },
                    nilaiJilid = santri.nilaiJilid.ifBlank { "-" },
                    catatan = "Hadir via Scan Barcode",
                    timestamp = nowIso
                )
            )
        }

        val updatedSantri = santri.copy(
            kehadiran = "Hadir",
            riwayat = riwayatMut
        )

        val newLog = ScanLogEntry(
            nama = santri.nama,
            studentId = santri.id,
            timeStr = timeStr
        )
        _uiState.update { it.copy(scanLogs = listOf(newLog) + it.scanLogs) }

        viewModelScope.launch {
            try {
                firebaseService.saveSantri(updatedSantri)
                showToast("${santri.nama} - Hadir ($timeStr)")
                syncAllData(silent = true)
            } catch (e: Exception) {
                showToast("Gagal update absen: ${e.message}", isError = true)
            }
        }
    }

    // --- LEMBAGA & JURNAL ---

    fun updateLembagaName(newName: String) {
        val state = _uiState.value
        if (state.currentUser.role != "admin") return
        if (newName.isBlank()) return

        viewModelScope.launch {
            try {
                firebaseService.updateAdminLembagaName(newName.trim(), state.usersList)
                _uiState.update { it.copy(currentLembagaName = newName.trim()) }
                showToast("Nama lembaga diperbarui!")
                syncAllData(silent = true)
            } catch (e: Exception) {
                showToast("Gagal mengubah nama lembaga: ${e.message}", isError = true)
            }
        }
    }

    fun saveJurnal(
        kelas: String,
        tanggal: String,
        tadarus: String,
        murojaah: String,
        materi: String
    ) {
        if (kelas.isBlank() || kelas == "Semua") {
            showToast("Pilih satu kelas spesifik terlebih dahulu!", isError = true)
            return
        }
        if (!canManageClass(kelas)) {
            showToast("Akses Ditolak.", isError = true)
            return
        }

        val jId = "${kelas}_${tanggal}"
        val jurnal = Jurnal(
            id = jId,
            kelas = kelas,
            tanggal = tanggal,
            tadarus = tadarus.trim(),
            murojaah = murojaah.trim(),
            materi = materi.trim(),
            timestamp = AppUtils.nowIsoString()
        )

        viewModelScope.launch {
            try {
                firebaseService.saveJurnal(jurnal)
                showToast("Jurnal berhasil disimpan!")
                syncAllData(silent = true)
            } catch (e: Exception) {
                showToast("Gagal menyimpan jurnal: ${e.message}", isError = true)
            }
        }
    }
}
