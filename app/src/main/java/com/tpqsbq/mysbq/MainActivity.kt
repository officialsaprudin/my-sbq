package com.tpqsbq.mysbq

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.tpqsbq.mysbq.model.Santri
import com.tpqsbq.mysbq.ui.*
import com.tpqsbq.mysbq.util.AppUtils
import com.tpqsbq.mysbq.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = IndigoPrimary,
                    secondary = IndigoDark,
                    background = Color(0xFFF9FAFB)
                )
            ) {
                val state by viewModel.uiState.collectAsState()
                val context = LocalContext.current

                LaunchedEffect(state.toastMessage) {
                    state.toastMessage?.let { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        viewModel.clearToast()
                    }
                }

                if (!state.isLoggedIn) {
                    AuthScreen(
                        isLoading = state.isLoading,
                        authError = state.authError,
                        onLogin = { em, pw -> viewModel.login(em, pw) },
                        onRegister = { em, pw, nm, kl -> viewModel.register(em, pw, nm, kl) }
                    )
                } else {
                    MainAppContent(
                        viewModel = viewModel,
                        onLaunchCameraScanner = { launchNativeBarcodeScanner() }
                    )
                }
            }
        }
    }

    private fun launchNativeBarcodeScanner() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_ALL_FORMATS
            )
            .enableAutoZoom()
            .build()

        val scanner = GmsBarcodeScanning.getClient(this, options)
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue
                if (!rawValue.isNullOrBlank()) {
                    viewModel.onBarcodeScanned(rawValue)
                }
            }
            .addOnFailureListener { e ->
                viewModel.showToast(
                    "Kamera scanner belum siap (${e.localizedMessage ?: "Gunakan input ID manual"})",
                    isError = true
                )
            }
    }
}

enum class MainTab(val title: String) {
    DASHBOARD("Dashboard"),
    DATA_SANTRI("Data Santri"),
    KELAS("Kelas"),
    REKAP("Rekap")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    viewModel: MainViewModel,
    onLaunchCameraScanner: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var currentTab by remember { mutableStateOf(MainTab.DASHBOARD) }

    // Modal states
    var showSantriForm by remember { mutableStateOf(false) }
    var editingSantri by remember { mutableStateOf<Santri?>(null) }
    var historySantri by remember { mutableStateOf<Santri?>(null) }
    var barcodeSantri by remember { mutableStateOf<Santri?>(null) }
    var deleteConfirmSantri by remember { mutableStateOf<Santri?>(null) }
    var sppSantriId by remember { mutableStateOf<String?>(null) }
    var showEditLembagaModal by remember { mutableStateOf(false) }
    var showScannerModal by remember { mutableStateOf(false) }
    var jurnalModalState by remember { mutableStateOf<Pair<String, String?>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_sbq),
                            contentDescription = "Logo",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "TPQ Salaamah Bisyafa'atil Qur'an",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (state.currentUser.role == "admin") {
                                    "Mode Admin (Semua Kelas)"
                                } else {
                                    "${state.currentUser.nama.ifBlank { "Ustadz" }} • ${state.currentUser.kelas.joinToString(", ")}"
                                },
                                fontSize = 11.sp,
                                color = Color(0xFFC7D2FE),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.syncAllData(silent = false) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sinkronisasi Data",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Keluar",
                            tint = Color(0xFFFCA5A5)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IndigoPrimary)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = currentTab == MainTab.DASHBOARD,
                    onClick = { currentTab = MainTab.DASHBOARD },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Dashboard", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.DATA_SANTRI,
                    onClick = { currentTab = MainTab.DATA_SANTRI },
                    icon = { Icon(Icons.Default.Groups, contentDescription = null) },
                    label = { Text("Data Santri", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.KELAS,
                    onClick = { currentTab = MainTab.KELAS },
                    icon = { Icon(Icons.Default.Class, contentDescription = null) },
                    label = { Text("Kelas", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.REKAP,
                    onClick = { currentTab = MainTab.REKAP },
                    icon = { Icon(Icons.Default.Assignment, contentDescription = null) },
                    label = { Text("Rekap", fontSize = 11.sp) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.DASHBOARD -> {
                    DashboardScreen(
                        state = state,
                        onFilterChanged = { viewModel.setDashKelasFilter(it) },
                        onEditLembagaClick = { showEditLembagaModal = true },
                        onOpenSppModal = { s -> sppSantriId = s.id }
                    )
                }
                MainTab.DATA_SANTRI -> {
                    DataSantriScreen(
                        state = state,
                        onFilterChanged = { viewModel.setTabelKelasFilter(it) },
                        onTambahClick = {
                            editingSantri = null
                            showSantriForm = true
                        },
                        onScanClick = {
                            showScannerModal = true
                        },
                        onMassBarcodeClick = { kls ->
                            AppUtils.exportClassBarcodesPdf(context, kls, state.santriList)
                        },
                        onOpenSppModal = { s -> sppSantriId = s.id },
                        onShowBarcode = { s -> barcodeSantri = s },
                        onShowHistory = { s -> historySantri = s },
                        onExportRapor = { s -> AppUtils.exportRaporPdf(context, s) },
                        onEditSantri = { s ->
                            editingSantri = s
                            showSantriForm = true
                        },
                        onDeleteSantri = { s -> deleteConfirmSantri = s }
                    )
                }
                MainTab.KELAS -> {
                    KelasScreen(
                        state = state,
                        onExportKelasPdf = { kls ->
                            val wali = state.pengajarMap[kls] ?: "-"
                            AppUtils.exportKelasSummaryPdf(context, kls, wali, state.santriList)
                        }
                    )
                }
                MainTab.REKAP -> {
                    RekapScreen(
                        state = state,
                        onKelasFilterChanged = { viewModel.setRekapKelasFilter(it) },
                        onDateRangeChanged = { m, a -> viewModel.setRekapDateRange(m, a) },
                        onApplyPreset = { preset -> viewModel.applyTopSemesterPreset(preset) },
                        onSetSemesterSiaFilter = { kls, semKey -> viewModel.setSemesterSiaFilter(kls, semKey) },
                        onOpenJurnalModal = { kls, tgl ->
                            jurnalModalState = kls to tgl
                        },
                        onExportGlobalPdf = {
                            val klsList = if (state.rekapKelasFilter == "Semua") {
                                state.availableClasses
                            } else {
                                listOf(state.rekapKelasFilter)
                            }
                            AppUtils.exportGlobalRekapPdf(
                                context = context,
                                tglMulai = state.rekapTglMulai,
                                tglAkhir = state.rekapTglAkhir,
                                kelasList = klsList,
                                pengajarMap = state.pengajarMap,
                                santriList = state.santriList,
                                jurnalList = state.jurnalList,
                                semesterFilterMap = state.semesterFilterMap
                            )
                        }
                    )
                }
            }

            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = Color(0xFFFACC15)
                )
            }
        }
    }

    // --- DIALOGS ---

    if (showSantriForm) {
        val defaultCls = state.currentUser.kelas.firstOrNull { it != "Semua" }
            ?: state.availableClasses.firstOrNull()
            ?: "Kelas Madinah"
        SantriFormDialog(
            editingSantri = editingSantri,
            availableClasses = state.availableClasses,
            defaultClass = defaultCls,
            onDismiss = { showSantriForm = false },
            onSave = { id, nm, kls, khd, jld, hlm, srh, ayt, nh, nj, cat ->
                viewModel.saveSantri(id, nm, kls, khd, jld, hlm, srh, ayt, nh, nj, cat)
                showSantriForm = false
            }
        )
    }

    sppSantriId?.let { activeId ->
        val liveSantri = state.santriList.find { it.id == activeId }
        if (liveSantri != null) {
            SppDialog(
                santri = liveSantri,
                canManage = viewModel.canManageClass(liveSantri.kelas),
                onDismiss = { sppSantriId = null },
                onSavePayment = { editId, jenis, bulan, nom, tgl, cat ->
                    viewModel.saveSppPayment(liveSantri.id, editId, jenis, bulan, nom, tgl, cat)
                },
                onDeletePayment = { payId ->
                    viewModel.deleteSppPayment(liveSantri.id, payId)
                },
                onPrintKuitansi = { payItem ->
                    AppUtils.exportKuitansiSppPdf(
                        context = context,
                        lembagaName = state.currentLembagaName,
                        santri = liveSantri,
                        payment = payItem
                    )
                }
            )
        }
    }

    historySantri?.let { s ->
        HistoryDialog(
            santri = s,
            onDismiss = { historySantri = null }
        )
    }

    barcodeSantri?.let { s ->
        BarcodeDialog(
            santri = s,
            onDismiss = { barcodeSantri = null },
            onPrintPdf = {
                AppUtils.exportSingleBarcodePdf(context, s)
            }
        )
    }

    deleteConfirmSantri?.let { s ->
        AlertDialog(
            onDismissRequest = { deleteConfirmSantri = null },
            title = { Text("Hapus Data Santri", fontWeight = FontWeight.Bold) },
            text = { Text("Yakin ingin menghapus data santri \"${s.nama}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSantri(s)
                        deleteConfirmSantri = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                ) {
                    Text("Hapus", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmSantri = null }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showEditLembagaModal) {
        EditLembagaDialog(
            currentName = state.currentLembagaName,
            onDismiss = { showEditLembagaModal = false },
            onSave = { newName ->
                viewModel.updateLembagaName(newName)
                showEditLembagaModal = false
            }
        )
    }

    if (showScannerModal) {
        ScannerDialog(
            scanLogs = state.scanLogs,
            onStartCameraScan = onLaunchCameraScanner,
            onManualSubmit = { id -> viewModel.onBarcodeScanned(id) },
            onDismiss = { showScannerModal = false }
        )
    }

    jurnalModalState?.let { (kls, tgl) ->
        JurnalDialog(
            kelas = kls,
            initialDate = tgl ?: AppUtils.todayDateStr(),
            jurnalList = state.jurnalList,
            onDismiss = { jurnalModalState = null },
            onSave = { k, t, tad, mur, mat ->
                viewModel.saveJurnal(k, t, tad, mur, mat)
                jurnalModalState = null
            }
        )
    }
}
