package com.tpqsbq.mysbq.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.tpqsbq.mysbq.model.Jurnal
import com.tpqsbq.mysbq.model.PembayaranItem
import com.tpqsbq.mysbq.model.RiwayatItem
import com.tpqsbq.mysbq.model.Santri
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object AppUtils {

    val NAMA_BULAN = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    fun currentMonthIndex(): Int {
        return Calendar.getInstance().get(Calendar.MONTH)
    }

    fun currentYear(): Int {
        return Calendar.getInstance().get(Calendar.YEAR)
    }

    fun getBulanIniNama(): String {
        return NAMA_BULAN[currentMonthIndex()]
    }

    fun isSppPaidThisMonth(pembayaran: List<PembayaranItem>): Boolean {
        val bulanIni = getBulanIniNama()
        return pembayaran.any { it.jenis == "SPP" && it.bulan == bulanIni }
    }

    fun formatRupiah(nominal: Long): String {
        val nf = NumberFormat.getInstance(Locale("id", "ID"))
        return "Rp ${nf.format(nominal)}"
    }

    fun nowIsoString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    fun todayDateStr(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun daysAgoDateStr(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(cal.time)
    }

    fun currentTimeStr(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
        return sdf.format(Date())
    }

    fun isWeekendToday(): Boolean {
        val cal = Calendar.getInstance()
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        return dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
    }

    fun parseIsoDate(iso: String): Date? {
        if (iso.isBlank()) return null
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (p in patterns) {
            try {
                val sdf = SimpleDateFormat(p, Locale.US)
                if (p.endsWith("'Z'")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                return sdf.parse(iso)
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun formatDateTimeShort(iso: String): String {
        val dt = parseIsoDate(iso) ?: return iso.take(16)
        val sdf = SimpleDateFormat("d/M/yyyy HH:mm", Locale.getDefault())
        return sdf.format(dt)
    }

    fun formatDateSlashShort(iso: String): String {
        val dt = parseIsoDate(iso) ?: return iso.take(10)
        val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
        return sdf.format(dt)
    }

    fun formatDateFullSlash(iso: String): String {
        val dt = parseIsoDate(iso) ?: return iso.take(10)
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(dt)
    }

    data class StarStatus(
        val stars: Int,
        val isBurned: Boolean
    )

    fun calculateWeeklyStars(riwayat: List<RiwayatItem>): StarStatus {
        if (riwayat.isEmpty()) return StarStatus(0, false)
        val cal = Calendar.getInstance()
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val diffToMonday = if (dow == Calendar.SUNDAY) -6 else (Calendar.MONDAY - dow)
        cal.add(Calendar.DAY_OF_MONTH, diffToMonday)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val mondayTime = cal.timeInMillis

        var stars = 0
        var adaAbsen = false

        for (r in riwayat) {
            val dt = parseIsoDate(r.timestamp) ?: continue
            if (dt.time >= mondayTime) {
                val rCal = Calendar.getInstance().apply { time = dt }
                val rDow = rCal.get(Calendar.DAY_OF_WEEK)
                if (rDow in Calendar.MONDAY..Calendar.FRIDAY) {
                    if (r.kehadiran == "Hadir") {
                        stars++
                    } else {
                        adaAbsen = true
                    }
                }
            }
        }
        return StarStatus(stars = stars.coerceAtMost(5), isBurned = adaAbsen)
    }

    fun resolveSemesterDateRange(
        chosenSemester: String,
        defaultMulai: String,
        defaultAkhir: String
    ): Triple<String, String, String> {
        val yr = currentYear()
        return when (chosenSemester) {
            "ganjil" -> Triple(
                "$yr-07-01",
                "$yr-12-31",
                "Semester Ganjil $yr (1 Jul s/d 31 Des)"
            )
            "genap" -> Triple(
                "$yr-01-01",
                "$yr-06-30",
                "Semester Genap $yr (1 Jan s/d 30 Jun)"
            )
            "ganjil_prev" -> Triple(
                "${yr - 1}-07-01",
                "${yr - 1}-12-31",
                "Semester Ganjil ${yr - 1} (Tahun Lalu)"
            )
            "semua" -> Triple(
                "2000-01-01",
                "2099-12-31",
                "Semua Waktu (Total Keseluruhan)"
            )
            else -> Triple(
                defaultMulai,
                defaultAkhir,
                "Sesuai Rentang Tanggal ($defaultMulai s/d $defaultAkhir)"
            )
        }
    }

    fun generateBarcodeBitmap(
        content: String,
        width: Int = 600,
        height: Int = 200
    ): Bitmap {
        val safeContent = content.ifBlank { "ST000000" }
        val bitMatrix = MultiFormatWriter().encode(
            safeContent,
            BarcodeFormat.CODE_128,
            width,
            height - 44
        )
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val barHeight = height - 44
        for (x in 0 until width) {
            for (y in 0 until barHeight) {
                if (bitMatrix.get(x, y)) {
                    bmp.setPixel(x, y, Color.BLACK)
                }
            }
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 28f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE
        }
        canvas.drawText(safeContent, width / 2f, height - 10f, textPaint)
        return bmp
    }

    // --- PDF EXPORTERS ---

    fun exportKuitansiSppPdf(
        context: Context,
        lembagaName: String,
        santri: Santri,
        payment: PembayaranItem
    ) {
        try {
            val pdf = PdfDocument()
            // Landscape A6-ish receipt (420 x 298 pt)
            val pageWidth = 420
            val pageHeight = 298
            val page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
            val canvas = page.canvas

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.rgb(217, 119, 6) // Amber-600
                strokeWidth = 2.5f
            }
            val dividerPaint = Paint().apply {
                color = Color.rgb(200, 200, 200)
                strokeWidth = 1f
            }
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.DKGRAY
                textSize = 9f
                textAlign = Paint.Align.CENTER
            }
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 9.5f
            }
            val boldValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            canvas.drawRect(14f, 14f, pageWidth - 14f, pageHeight - 14f, borderPaint)
            canvas.drawText(lembagaName.uppercase(Locale.getDefault()), pageWidth / 2f, 40f, titlePaint)
            canvas.drawText("BUKTI PEMBAYARAN / KUITANSI SAH", pageWidth / 2f, 55f, subPaint)
            canvas.drawLine(28f, 64f, pageWidth - 28f, 64f, dividerPaint)

            val leftCol = 38f
            val valCol = 145f
            var curY = 84f

            canvas.drawText("No. Kuitansi", leftCol, curY, labelPaint)
            canvas.drawText(": ${payment.id}", valCol, curY, labelPaint)
            curY += 18f

            canvas.drawText("Telah Terima Dari", leftCol, curY, labelPaint)
            canvas.drawText(": ${santri.nama} (${santri.kelas})", valCol, curY, boldValPaint)
            curY += 18f

            val peruntukan = if (payment.jenis == "SPP") "SPP Bulan ${payment.bulan}" else payment.jenis
            canvas.drawText("Untuk Pembayaran", leftCol, curY, labelPaint)
            canvas.drawText(": $peruntukan", valCol, curY, labelPaint)
            curY += 18f

            canvas.drawText("Tanggal Bayar", leftCol, curY, labelPaint)
            canvas.drawText(": ${payment.tanggal}", valCol, curY, labelPaint)
            curY += 18f

            if (payment.catatan.isNotBlank()) {
                canvas.drawText("Catatan", leftCol, curY, labelPaint)
                canvas.drawText(": ${payment.catatan}", valCol, curY, labelPaint)
                curY += 18f
            }

            // Nominal Highlight Box
            val boxFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(254, 243, 199)
                style = Paint.Style.FILL
            }
            val nomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(180, 83, 9)
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawRoundRect(RectF(leftCol, curY + 6f, leftCol + 175f, curY + 40f), 6f, 6f, boxFill)
            canvas.drawText(formatRupiah(payment.nominal), leftCol + 12f, curY + 28f, nomPaint)

            // Signature area
            val sigX = 290f
            val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 8.5f
            }
            val smallBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("Diterima oleh,", sigX, curY + 8f, smallPaint)
            canvas.drawText("Ustadz / Pengurus", sigX, curY + 20f, smallPaint)
            canvas.drawText(payment.penerima.ifBlank { "Bendahara" }, sigX, curY + 62f, smallBoldPaint)

            pdf.finishPage(page)
            val safeName = santri.nama.replace("\\s+".toRegex(), "_")
            sharePdfDocument(context, pdf, "Kuitansi_${safeName}_${payment.id}.pdf")
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal mencetak kuitansi: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportSingleBarcodePdf(context: Context, santri: Santri) {
        try {
            val pdf = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.BLACK
                strokeWidth = 2f
            }
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.DKGRAY
                textSize = 12f
                textAlign = Paint.Align.CENTER
            }

            val cardRect = RectF(120f, 120f, 475f, 340f)
            canvas.drawRoundRect(cardRect, 12f, 12f, borderPaint)
            canvas.drawText(santri.nama, cardRect.centerX(), 160f, titlePaint)
            canvas.drawText(santri.kelas.ifBlank { "-" }, cardRect.centerX(), 180f, subPaint)

            val barcodeBmp = generateBarcodeBitmap(santri.id, 480, 160)
            val destRect = RectF(155f, 195f, 440f, 295f)
            canvas.drawBitmap(barcodeBmp, null, destRect, null)

            canvas.drawText("Gunakan barcode ini untuk Absensi Scan", cardRect.centerX(), 320f, subPaint)

            pdf.finishPage(page)
            sharePdfDocument(context, pdf, "IDCard_${santri.nama.replace("\\s+".toRegex(), "_")}.pdf")
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuat PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportClassBarcodesPdf(context: Context, kelas: String, santriList: List<Santri>) {
        try {
            val filtered = santriList.filter { it.kelas == kelas }
            if (filtered.isEmpty()) {
                Toast.makeText(context, "Tidak ada santri di kelas $kelas", Toast.LENGTH_SHORT).show()
                return
            }

            val pdf = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            var pageNumber = 1
            var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var canvas = page.canvas

            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.LTGRAY
                strokeWidth = 1f
            }

            canvas.drawText("ID Card Barcode - $kelas", pageWidth / 2f, 40f, headerPaint)

            var y = 70f
            var col = 0

            for (s in filtered) {
                val x = if (col == 0) 45f else 315f
                val cardW = 235f
                val cardH = 105f

                if (y + cardH > pageHeight - 40) {
                    pdf.finishPage(page)
                    pageNumber++
                    page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    canvas = page.canvas
                    y = 50f
                }

                canvas.drawRoundRect(RectF(x, y, x + cardW, y + cardH), 8f, 8f, boxPaint)
                canvas.drawText(s.nama.take(24), x + cardW / 2f, y + 20f, namePaint)

                val bmp = generateBarcodeBitmap(s.id, 360, 120)
                canvas.drawBitmap(bmp, null, RectF(x + 20f, y + 28f, x + cardW - 20f, y + cardH - 8f), null)

                col++
                if (col >= 2) {
                    col = 0
                    y += 120f
                }
            }

            pdf.finishPage(page)
            sharePdfDocument(context, pdf, "Barcode_${kelas.replace("\\s+".toRegex(), "_")}.pdf")
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuat PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportRaporPdf(context: Context, santri: Santri) {
        try {
            val pdf = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            var pageNumber = 1
            var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var canvas = page.canvas

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.DKGRAY
                textSize = 12f
                textAlign = Paint.Align.CENTER
            }
            val headBgPaint = Paint().apply {
                color = Color.rgb(79, 70, 229)
                style = Paint.Style.FILL
            }
            val headTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val cellTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 9f
            }
            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
            }

            canvas.drawText("Rapor Ngaji: ${santri.nama}", pageWidth / 2f, 45f, titlePaint)
            canvas.drawText("Kelas: ${santri.kelas.ifBlank { "-" }}", pageWidth / 2f, 65f, subPaint)

            var y = 90f
            val cols = floatArrayOf(30f, 100f, 160f, 250f, 360f, 445f, 565f)
            val headers = arrayOf("Tanggal", "Kehadiran", "Jilid/Hal", "Hafalan", "Nilai", "Catatan")

            fun drawHeader(c: Canvas, topY: Float) {
                c.drawRect(cols[0], topY, cols.last(), topY + 24f, headBgPaint)
                for (i in headers.indices) {
                    c.drawText(headers[i], cols[i] + 4f, topY + 16f, headTextPaint)
                }
            }

            drawHeader(canvas, y)
            y += 24f

            for (r in santri.riwayat) {
                if (y > pageHeight - 50) {
                    pdf.finishPage(page)
                    pageNumber++
                    page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    canvas = page.canvas
                    y = 40f
                    drawHeader(canvas, y)
                    y += 24f
                }

                val tgl = formatDateFullSlash(r.timestamp)
                val jilidHal = "${r.jilid} - ${r.halaman}"
                val hafalan = "${r.surah} (${r.ayat})"
                val nilaiH = r.nilaiHafalan.ifBlank { r.keterangan.ifBlank { "-" } }
                val nilaiJ = r.nilaiJilid.ifBlank { r.keterangan.ifBlank { "-" } }
                val catatan = r.catatan.ifBlank { "-" }

                canvas.drawText(tgl, cols[0] + 4f, y + 14f, cellTextPaint)
                canvas.drawText(r.kehadiran, cols[1] + 4f, y + 14f, cellTextPaint)
                canvas.drawText(jilidHal.take(15), cols[2] + 4f, y + 14f, cellTextPaint)
                canvas.drawText(hafalan.take(18), cols[3] + 4f, y + 14f, cellTextPaint)
                canvas.drawText("H: $nilaiH", cols[4] + 4f, y + 12f, cellTextPaint)
                canvas.drawText("J: $nilaiJ", cols[4] + 4f, y + 23f, cellTextPaint)
                canvas.drawText(catatan.take(22), cols[5] + 4f, y + 14f, cellTextPaint)

                y += 28f
                canvas.drawLine(cols[0], y, cols.last(), y, linePaint)
            }

            pdf.finishPage(page)
            sharePdfDocument(context, pdf, "Rapor_${santri.nama.replace("\\s+".toRegex(), "_")}.pdf")
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuat PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportKelasSummaryPdf(context: Context, kelas: String, waliKelas: String, santriList: List<Santri>) {
        try {
            val murid = santriList.filter { it.kelas == kelas }
            val pdf = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            var pageNumber = 1
            var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var canvas = page.canvas

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.DKGRAY
                textSize = 12f
                textAlign = Paint.Align.CENTER
            }
            val headBgPaint = Paint().apply {
                color = Color.rgb(79, 70, 229)
                style = Paint.Style.FILL
            }
            val headTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val cellTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 10f
            }
            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
            }

            canvas.drawText("Daftar Santri - $kelas", pageWidth / 2f, 45f, titlePaint)
            canvas.drawText("Wali Kelas: $waliKelas | Total: ${murid.size} Santri", pageWidth / 2f, 65f, subPaint)

            var y = 90f
            val cols = floatArrayOf(35f, 70f, 230f, 305f, 420f, 560f)
            val headers = arrayOf("No", "Nama Santri", "Status", "Jilid / Hal", "Hafalan")

            canvas.drawRect(cols[0], y, cols.last(), y + 24f, headBgPaint)
            for (i in headers.indices) {
                canvas.drawText(headers[i], cols[i] + 4f, y + 16f, headTextPaint)
            }
            y += 24f

            murid.forEachIndexed { index, s ->
                if (y > pageHeight - 50) {
                    pdf.finishPage(page)
                    pageNumber++
                    page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    canvas = page.canvas
                    y = 40f
                }
                canvas.drawText("${index + 1}", cols[0] + 4f, y + 16f, cellTextPaint)
                canvas.drawText(s.nama.take(25), cols[1] + 4f, y + 16f, cellTextPaint)
                canvas.drawText(s.kehadiran, cols[2] + 4f, y + 16f, cellTextPaint)
                canvas.drawText("${s.jilid} (Hal ${s.halaman})".take(18), cols[3] + 4f, y + 16f, cellTextPaint)
                canvas.drawText("${s.surah} (${s.ayat})".take(22), cols[4] + 4f, y + 16f, cellTextPaint)
                y += 24f
                canvas.drawLine(cols[0], y, cols.last(), y, linePaint)
            }

            pdf.finishPage(page)
            sharePdfDocument(context, pdf, "Kelas_${kelas.replace("\\s+".toRegex(), "_")}.pdf")
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuat PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportGlobalRekapPdf(
        context: Context,
        tglMulai: String,
        tglAkhir: String,
        kelasList: List<String>,
        pengajarMap: Map<String, String>,
        semesterFilterMap: Map<String, String>,
        santriList: List<Santri>,
        jurnalList: List<Jurnal>
    ) {
        try {
            val pdf = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            var pageNumber = 1
            var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var canvas = page.canvas

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.DKGRAY
                textSize = 10f
                textAlign = Paint.Align.CENTER
            }
            val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(67, 56, 202)
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val boldSmallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val textSmallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 8.5f
            }
            val indigoHeadPaint = Paint().apply {
                color = Color.rgb(79, 70, 229)
                style = Paint.Style.FILL
            }
            val redHeadPaint = Paint().apply {
                color = Color.rgb(220, 38, 38)
                style = Paint.Style.FILL
            }
            val whiteHeadText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 0.8f
            }

            fun checkNewPage(neededHeight: Float) {
                if (yPos + neededHeight > pageHeight - 40) {
                    pdf.finishPage(page)
                    pageNumber++
                    page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    canvas = page.canvas
                    yPos = 40f
                }
            }

            canvas.drawText("Laporan Progres & Kehadiran Santri", pageWidth / 2f, 40f, titlePaint)
            canvas.drawText("Periode Laporan: $tglMulai s/d $tglAkhir", pageWidth / 2f, 56f, subPaint)
            yPos = 76f

            for (kls in kelasList) {
                checkNewPage(60f)
                val pengajar = pengajarMap[kls] ?: "Belum diatur"
                canvas.drawText("Kelas: $kls | Pengajar: $pengajar", 35f, yPos, sectionPaint)
                yPos += 16f

                val chosenSem = semesterFilterMap[kls] ?: "rentang"
                val (siaMulai, siaAkhir, labelSia) = resolveSemesterDateRange(chosenSem, tglMulai, tglAkhir)

                data class SiaRow(val nama: String, val h: Int, val s: Int, val i: Int, val a: Int)
                data class DetailRow(
                    val tgl: String,
                    val nama: String,
                    val hadir: String,
                    val jilid: String,
                    val surah: String,
                    val nilai: String,
                    val catatan: String
                )

                val siaRows = mutableListOf<SiaRow>()
                val detailRows = mutableListOf<DetailRow>()

                santriList.filter { it.kelas == kls }.forEach { s ->
                    val riwayatSemester = s.riwayat.filter { r ->
                        val t = r.timestamp.take(10)
                        t in siaMulai..siaAkhir
                    }
                    var cH = 0
                    var cS = 0
                    var cI = 0
                    var cA = 0
                    riwayatSemester.forEach { r ->
                        when (r.kehadiran) {
                            "Hadir" -> cH++
                            "Sakit" -> cS++
                            "Izin" -> cI++
                            "Alpa" -> cA++
                        }
                    }
                    siaRows.add(SiaRow(s.nama, cH, cS, cI, cA))

                    val riwayatRange = s.riwayat.filter { r ->
                        val t = r.timestamp.take(10)
                        t in tglMulai..tglAkhir
                    }
                    riwayatRange.forEach { r ->
                        val nH = r.nilaiHafalan.ifBlank { r.keterangan.ifBlank { "-" } }
                        val nJ = r.nilaiJilid.ifBlank { r.keterangan.ifBlank { "-" } }
                        detailRows.add(
                            DetailRow(
                                tgl = formatDateSlashShort(r.timestamp),
                                nama = s.nama,
                                hadir = r.kehadiran,
                                jilid = "${r.jilid} (${r.halaman})",
                                surah = "${r.surah} (${r.ayat})",
                                nilai = "H:$nH / J:$nJ",
                                catatan = r.catatan.ifBlank { "-" }
                            )
                        )
                    }
                }

                // 1. SIA Summary Table
                if (siaRows.isNotEmpty()) {
                    checkNewPage(40f)
                    canvas.drawText("Rekapitulasi Kehadiran (H/S/I/A) - $labelSia", 35f, yPos, boldSmallPaint)
                    yPos += 6f
                    val siaCols = floatArrayOf(35f, 260f, 340f, 420f, 495f, 560f)
                    val siaHeaders = arrayOf("Nama Santri", "Hadir", "Sakit", "Izin", "Alpa")
                    canvas.drawRect(siaCols[0], yPos, siaCols.last(), yPos + 18f, indigoHeadPaint)
                    for (idx in siaHeaders.indices) {
                        canvas.drawText(siaHeaders[idx], siaCols[idx] + 6f, yPos + 13f, whiteHeadText)
                    }
                    yPos += 18f
                    for (row in siaRows) {
                        checkNewPage(18f)
                        canvas.drawText(row.nama.take(30), siaCols[0] + 6f, yPos + 13f, textSmallPaint)
                        canvas.drawText(row.h.toString(), siaCols[1] + 14f, yPos + 13f, textSmallPaint)
                        canvas.drawText(row.s.toString(), siaCols[2] + 14f, yPos + 13f, textSmallPaint)
                        canvas.drawText(row.i.toString(), siaCols[3] + 14f, yPos + 13f, textSmallPaint)
                        canvas.drawText(row.a.toString(), siaCols[4] + 14f, yPos + 13f, textSmallPaint)
                        yPos += 16f
                        canvas.drawLine(siaCols[0], yPos, siaCols.last(), yPos, linePaint)
                    }
                    yPos += 12f
                }

                // 2. Jurnal Kelas
                val jurnalKelas = jurnalList
                    .filter { it.kelas == kls && it.tanggal in tglMulai..tglAkhir }
                    .sortedBy { it.tanggal }

                if (jurnalKelas.isNotEmpty()) {
                    checkNewPage(30f)
                    canvas.drawText("Jurnal Kelas Harian:", 35f, yPos, boldSmallPaint)
                    yPos += 12f
                    for (j in jurnalKelas) {
                        checkNewPage(28f)
                        canvas.drawText(
                            "[${j.tanggal}] Tadarus: ${j.tadarus.ifBlank { "-" }} | Murojaah: ${j.murojaah.ifBlank { "-" }}",
                            35f,
                            yPos,
                            textSmallPaint
                        )
                        yPos += 11f
                        canvas.drawText("Materi: ${j.materi.ifBlank { "-" }}", 35f, yPos, textSmallPaint)
                        yPos += 14f
                    }
                    yPos += 4f
                }

                // 3. Detail Table
                if (detailRows.isNotEmpty()) {
                    checkNewPage(40f)
                    val dCols = floatArrayOf(35f, 72f, 175f, 222f, 300f, 385f, 470f, 560f)
                    val dHeaders = arrayOf("Tgl", "Nama", "Hadir", "Jilid", "Hafalan", "Nilai", "Catatan")
                    canvas.drawRect(dCols[0], yPos, dCols.last(), yPos + 18f, redHeadPaint)
                    for (idx in dHeaders.indices) {
                        canvas.drawText(dHeaders[idx], dCols[idx] + 4f, yPos + 13f, whiteHeadText)
                    }
                    yPos += 18f
                    for (d in detailRows) {
                        checkNewPage(18f)
                        canvas.drawText(d.tgl, dCols[0] + 3f, yPos + 12f, textSmallPaint)
                        canvas.drawText(d.nama.take(16), dCols[1] + 3f, yPos + 12f, textSmallPaint)
                        canvas.drawText(d.hadir, dCols[2] + 3f, yPos + 12f, textSmallPaint)
                        canvas.drawText(d.jilid.take(13), dCols[3] + 3f, yPos + 12f, textSmallPaint)
                        canvas.drawText(d.surah.take(14), dCols[4] + 3f, yPos + 12f, textSmallPaint)
                        canvas.drawText(d.nilai.take(15), dCols[5] + 3f, yPos + 12f, textSmallPaint)
                        canvas.drawText(d.catatan.take(16), dCols[6] + 3f, yPos + 12f, textSmallPaint)
                        yPos += 16f
                        canvas.drawLine(dCols[0], yPos, dCols.last(), yPos, linePaint)
                    }
                    yPos += 20f
                } else {
                    checkNewPage(20f)
                    canvas.drawText("Tidak ada data harian ngaji.", 35f, yPos, textSmallPaint)
                    yPos += 20f
                }
            }

            pdf.finishPage(page)
            sharePdfDocument(context, pdf, "Rekap_${tglMulai}_sd_${tglAkhir}.pdf")
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuat PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private var yPos = 0f

    private fun sharePdfDocument(context: Context, pdfDocument: PdfDocument, fileName: String) {
        val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(Intent.createChooser(viewIntent, "Buka / Cetak PDF ($fileName)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Bagikan PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}
