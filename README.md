# Aplikasi Android Native: My SBQ (TPQ Salaamah Bisyafa'atil Qur'an)

Proyek ini adalah aplikasi **Full Android Native (Kotlin & Jetpack Compose)** — **bukan WebView** — yang dibuat khusus untuk **TPQ Salaamah Bisyafa'atil Qur'an** dan terhubung langsung secara *real-time* ke database **Firebase (`project-311a138f-ccc5-491b-b79`)** milik web `officialsaprudin/tpq-sbq`.

---

## ✨ Fitur Lengkap (100% Sama dengan Versi Web)

1. **Autentikasi & Multi-Role (`users`)**
   - Masuk (Login) & Daftar Akun Baru (lengkap dengan Nama/Gelar, Kelas yang diajar, dan tombol Lihat/Sembunyikan Password).
   - **Role Admin** (`admin@ngaji.com`, `saprudin@ngaji.com`): Memantau seluruh kelas secara *Read-Only* dan mengubah Nama Lembaga di Dashboard.
   - **Role Ustadz / Wali Kelas**: Akses penuh untuk mengelola data santri di kelasnya, scan absen, dan mengisi jurnal harian.

2. **Dashboard & Kartu Bintang Mingguan**
   - Statistik *real-time*: Total Santri, Hadir, Izin/Sakit, dan Alpa (dengan filter per kelas).
   - **Kartu Progres Santri**: Menghitung Bintang Mingguan (Senin–Jumat, maksimal 5 `⭐` atau status *Hangus* jika ada absen), posisi Jilid/Halaman, dan Surah/Ayat hafalan.
   - **Mesin Auto-Alpa Hari Kerja**: Otomatis menandai Alpa jika belum scan absen di hari Senin–Jumat, dan otomatis nonaktif di hari libur (Sabtu & Minggu).

3. **Manajemen Data Santri (`santri`) & Barcode / QR**
   - Tambah Santri, Update Progres (Kehadiran, Posisi Jilid, Halaman, Surah, Ayat, Nilai Hafalan & Jilid: *Lancar / Kurang Lancar / Mengulang*, Catatan), Pindah Kelas, dan Hapus Santri.
   - **Riwayat Ngaji** lengkap per santri.
   - **ID Card Barcode (CODE128)**: Tampilkan & cetak PDF kartu barcode per santri atau cetak **PDF Barcode Massal** satu kelas penuh.
   - **Rapid-Scan Kamera Absensi**: Scan barcode kartu santri menggunakan kamera Android Native (Google Code Scanner / ML Kit) dengan sistem **Smart-Cooldown 2 menit per anak** + Log Scan Terakhir.

4. **Daftar Kelas / Rombel**
   - Menampilkan daftar kelas beserta Wali Kelas, jumlah santri, daftar status kehadiran, dan tombol **Cetak PDF Kelas**.

5. **Laporan Rekap, Jurnal Harian (`jurnal`), & Export PDF Native**
   - Input & Edit Jurnal Kelas Harian (Tadarus, Murojaah, Materi Pembelajaran).
   - Tabel Rekapitulasi Absensi (`H / S / I / A`) dan rincian progres berdasarkan filter rentang tanggal.
   - **Export PDF Native (`PdfDocument`)**: Cetak Rapor Santri, Rekap Global PDF, Daftar Kelas PDF, dan ID Card Barcode PDF langsung dari HP Android.

---

## 🚀 Cara Mendapatkan File `.APK` Tanpa Instal Aplikasi Berat di Laptop (Via GitHub)

Karena proyek ini sudah dilengkapi dengan **GitHub Actions** ([`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml)), server GitHub akan merakitkan file `.apk` untuk Anda secara otomatis dan gratis:

1. **Buat Repository Baru di GitHub** (misalnya diberi nama `my-sbq-android`).
2. **Upload Semua Isi Folder `My SBQ` Ini ke Repository Tersebut**:
   - Pastikan folder `.github`, `app`, serta file `build.gradle.kts`, `settings.gradle.kts`, dan `gradle.properties` ikut ter-upload.
3. **Buka Tab `Actions` di GitHub Anda**:
   - Klik menu **Actions** di bagian atas repository GitHub Anda.
   - Anda akan melihat proses **"Build APK My SBQ"** sedang berjalan (tunggu sekitar 2–3 menit hingga muncul tanda centang hijau ✅).
4. **Download File APK**:
   - Klik proses build yang sudah selesai (centang hijau ✅) tersebut.
   - Gulir ke bagian paling bawah (**Artifacts**), lalu klik **`Aplikasi-Android-My-SBQ`** untuk mengunduh file ZIP yang berisi **`My-SBQ-Release.apk`**.
   - Kirim file **`My-SBQ-Release.apk`** ke HP Android Anda dan langsung instal!
