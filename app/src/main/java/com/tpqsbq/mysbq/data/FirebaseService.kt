package com.tpqsbq.mysbq.data

import android.content.Context
import android.content.SharedPreferences
import com.tpqsbq.mysbq.model.Jurnal
import com.tpqsbq.mysbq.model.PembayaranItem
import com.tpqsbq.mysbq.model.RiwayatItem
import com.tpqsbq.mysbq.model.Santri
import com.tpqsbq.mysbq.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class FirebaseService(context: Context) {

    companion object {
        const val API_KEY = "AIzaSyCfapSUazt_uCVVI5uttatd2iGR7DyLk54"
        const val PROJECT_ID = "project-311a138f-ccc5-491b-b79"
        // HANYA EMAIL ADMIN UTAMA (sesuai index.html terbaru)
        val ADMIN_EMAILS = listOf("admin@ngaji.com")

        private const val AUTH_BASE = "https://identitytoolkit.googleapis.com/v1"
        private const val TOKEN_BASE = "https://securetoken.googleapis.com/v1"
        private const val FIRESTORE_BASE =
            "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("mysbq_auth_prefs", Context.MODE_PRIVATE)

    var idToken: String?
        get() = prefs.getString("idToken", null)
        private set(value) = prefs.edit().putString("idToken", value).apply()

    var refreshToken: String?
        get() = prefs.getString("refreshToken", null)
        private set(value) = prefs.edit().putString("refreshToken", value).apply()

    var currentUid: String?
        get() = prefs.getString("uid", null)
        private set(value) = prefs.edit().putString("uid", value).apply()

    var currentEmail: String?
        get() = prefs.getString("email", null)
        private set(value) = prefs.edit().putString("email", value).apply()

    fun isLoggedIn(): Boolean = !refreshToken.isNullOrBlank() && !currentUid.isNullOrBlank()

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    suspend fun signIn(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "$AUTH_BASE/accounts:signInWithPassword?key=$API_KEY"
            val body = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
                put("returnSecureToken", true)
            }
            val res = httpRequest("POST", url, body.toString(), useAuth = false)
            val json = JSONObject(res)
            idToken = json.getString("idToken")
            refreshToken = json.getString("refreshToken")
            currentUid = json.getString("localId")
            currentEmail = json.optString("email", email.trim())
            Result.success(currentUid!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(
        email: String,
        password: String,
        nama: String,
        kelasInput: String
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val url = "$AUTH_BASE/accounts:signUp?key=$API_KEY"
            val emLower = email.trim().lowercase()
            val body = JSONObject().apply {
                put("email", emLower)
                put("password", password)
                put("returnSecureToken", true)
            }
            val res = httpRequest("POST", url, body.toString(), useAuth = false)
            val json = JSONObject(res)
            val uid = json.getString("localId")
            idToken = json.getString("idToken")
            refreshToken = json.getString("refreshToken")
            currentUid = uid
            currentEmail = emLower

            val role = if (ADMIN_EMAILS.contains(emLower)) "admin" else "ustadz"
            val kelasList = if (role == "admin") {
                listOf("Semua")
            } else {
                kelasInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }
            val lembaga = if (role == "admin") "Nama Lembaga Baru (Edit di Dashboard)" else ""

            val profile = UserProfile(
                uid = uid,
                email = emLower,
                nama = nama.trim(),
                role = role,
                kelas = kelasList,
                lembaga = lembaga
            )
            saveUserProfile(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshAuthToken(): Boolean = withContext(Dispatchers.IO) {
        val rt = refreshToken ?: return@withContext false
        try {
            val url = "$TOKEN_BASE/token?key=$API_KEY"
            val payload = "grant_type=refresh_token&refresh_token=${URLEncoder.encode(rt, "UTF-8")}"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
            }
            OutputStreamWriter(conn.outputStream).use { it.write(payload) }
            if (conn.responseCode in 200..299) {
                val resp = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val json = JSONObject(resp)
                idToken = json.getString("id_token")
                refreshToken = json.getString("refresh_token")
                currentUid = json.getString("user_id")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    // --- FIRESTORE OPERATIONS ---

    suspend fun saveUserProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        val fields = JSONObject().apply {
            put("email", fsString(profile.email))
            put("nama", fsString(profile.nama))
            put("role", fsString(profile.role))
            put("kelas", fsStringArray(profile.kelas))
            if (profile.lembaga.isNotEmpty()) {
                put("lembaga", fsString(profile.lembaga))
            }
        }
        val docBody = JSONObject().apply { put("fields", fields) }
        val encodedId = encodeDocId(profile.uid)
        httpRequest("PATCH", "$FIRESTORE_BASE/users/$encodedId", docBody.toString(), useAuth = true)
    }

    suspend fun getUserProfile(uid: String): UserProfile? = withContext(Dispatchers.IO) {
        try {
            val encodedId = encodeDocId(uid)
            val res = httpRequest("GET", "$FIRESTORE_BASE/users/$encodedId", null, useAuth = true)
            parseUserDoc(JSONObject(res))
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchAllUsers(): List<UserProfile> = withContext(Dispatchers.IO) {
        val list = mutableListOf<UserProfile>()
        var pageToken: String? = null
        do {
            val url = buildString {
                append("$FIRESTORE_BASE/users?pageSize=300")
                if (!pageToken.isNullOrEmpty()) append("&pageToken=$pageToken")
            }
            val res = httpRequest("GET", url, null, useAuth = true)
            val json = JSONObject(res)
            val docs = json.optJSONArray("documents")
            if (docs != null) {
                for (i in 0 until docs.length()) {
                    parseUserDoc(docs.getJSONObject(i))?.let { list.add(it) }
                }
            }
            pageToken = json.optString("nextPageToken", "").takeIf { it.isNotEmpty() }
        } while (pageToken != null)
        list
    }

    suspend fun updateAdminLembagaName(newName: String, users: List<UserProfile>) = withContext(Dispatchers.IO) {
        val admins = users.filter { it.role == "admin" }
        for (adm in admins) {
            val encodedId = encodeDocId(adm.uid)
            val url = "$FIRESTORE_BASE/users/$encodedId?updateMask.fieldPaths=lembaga"
            val fields = JSONObject().apply {
                put("lembaga", fsString(newName))
            }
            val body = JSONObject().apply { put("fields", fields) }
            httpRequest("PATCH", url, body.toString(), useAuth = true)
        }
    }

    suspend fun fetchAllSantri(): List<Santri> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Santri>()
        var pageToken: String? = null
        do {
            val url = buildString {
                append("$FIRESTORE_BASE/santri?pageSize=300")
                if (!pageToken.isNullOrEmpty()) append("&pageToken=$pageToken")
            }
            val res = httpRequest("GET", url, null, useAuth = true)
            val json = JSONObject(res)
            val docs = json.optJSONArray("documents")
            if (docs != null) {
                for (i in 0 until docs.length()) {
                    parseSantriDoc(docs.getJSONObject(i))?.let { list.add(it) }
                }
            }
            pageToken = json.optString("nextPageToken", "").takeIf { it.isNotEmpty() }
        } while (pageToken != null)
        list
    }

    suspend fun saveSantri(santri: Santri) = withContext(Dispatchers.IO) {
        val riwayatValues = JSONArray()
        santri.riwayat.forEach { r ->
            val rFields = JSONObject().apply {
                put("kehadiran", fsString(r.kehadiran))
                put("jilid", fsString(r.jilid))
                put("halaman", fsString(r.halaman))
                put("surah", fsString(r.surah))
                put("ayat", fsString(r.ayat))
                put("nilaiHafalan", fsString(r.nilaiHafalan))
                put("nilaiJilid", fsString(r.nilaiJilid))
                if (r.keterangan.isNotEmpty()) put("keterangan", fsString(r.keterangan))
                put("catatan", fsString(r.catatan))
                put("timestamp", fsString(r.timestamp))
            }
            val mapObj = JSONObject().apply {
                put("mapValue", JSONObject().apply { put("fields", rFields) })
            }
            riwayatValues.put(mapObj)
        }

        val pembayaranValues = encodePembayaranArray(santri.pembayaran)

        val fields = JSONObject().apply {
            put("id", fsString(santri.id))
            put("nama", fsString(santri.nama))
            put("kelas", fsString(santri.kelas))
            put("kehadiran", fsString(santri.kehadiran))
            put("jilid", fsString(santri.jilid))
            put("halaman", fsString(santri.halaman))
            put("surah", fsString(santri.surah))
            put("ayat", fsString(santri.ayat))
            put("nilaiHafalan", fsString(santri.nilaiHafalan))
            put("nilaiJilid", fsString(santri.nilaiJilid))
            put("catatan", fsString(santri.catatan))
            put("timestamp", fsString(santri.timestamp))
            put("riwayat", JSONObject().apply {
                put("arrayValue", JSONObject().apply { put("values", riwayatValues) })
            })
            put("pembayaran", JSONObject().apply {
                put("arrayValue", JSONObject().apply { put("values", pembayaranValues) })
            })
        }

        val body = JSONObject().apply { put("fields", fields) }
        val encodedId = encodeDocId(santri.id)
        httpRequest("PATCH", "$FIRESTORE_BASE/santri/$encodedId", body.toString(), useAuth = true)
    }

    suspend fun updateSantriPembayaran(santriId: String, pembayaranList: List<PembayaranItem>) = withContext(Dispatchers.IO) {
        val pembayaranValues = encodePembayaranArray(pembayaranList)
        val fields = JSONObject().apply {
            put("pembayaran", JSONObject().apply {
                put("arrayValue", JSONObject().apply { put("values", pembayaranValues) })
            })
        }
        val body = JSONObject().apply { put("fields", fields) }
        val encodedId = encodeDocId(santriId)
        val url = "$FIRESTORE_BASE/santri/$encodedId?updateMask.fieldPaths=pembayaran"
        httpRequest("PATCH", url, body.toString(), useAuth = true)
    }

    private fun encodePembayaranArray(list: List<PembayaranItem>): JSONArray {
        val arr = JSONArray()
        list.forEach { p ->
            val pFields = JSONObject().apply {
                put("id", fsString(p.id))
                put("jenis", fsString(p.jenis))
                put("bulan", fsString(p.bulan))
                put("nominal", fsInteger(p.nominal))
                put("tanggal", fsString(p.tanggal))
                put("catatan", fsString(p.catatan))
                put("penerima", fsString(p.penerima))
                put("timestamp", fsString(p.timestamp))
                if (p.updatedAt.isNotEmpty()) put("updatedAt", fsString(p.updatedAt))
            }
            val mapObj = JSONObject().apply {
                put("mapValue", JSONObject().apply { put("fields", pFields) })
            }
            arr.put(mapObj)
        }
        return arr
    }

    suspend fun deleteSantri(id: String) = withContext(Dispatchers.IO) {
        val encodedId = encodeDocId(id)
        httpRequest("DELETE", "$FIRESTORE_BASE/santri/$encodedId", null, useAuth = true)
    }

    suspend fun fetchAllJurnal(): List<Jurnal> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Jurnal>()
        var pageToken: String? = null
        do {
            val url = buildString {
                append("$FIRESTORE_BASE/jurnal?pageSize=300")
                if (!pageToken.isNullOrEmpty()) append("&pageToken=$pageToken")
            }
            val res = httpRequest("GET", url, null, useAuth = true)
            val json = JSONObject(res)
            val docs = json.optJSONArray("documents")
            if (docs != null) {
                for (i in 0 until docs.length()) {
                    parseJurnalDoc(docs.getJSONObject(i))?.let { list.add(it) }
                }
            }
            pageToken = json.optString("nextPageToken", "").takeIf { it.isNotEmpty() }
        } while (pageToken != null)
        list
    }

    suspend fun saveJurnal(jurnal: Jurnal) = withContext(Dispatchers.IO) {
        val fields = JSONObject().apply {
            put("id", fsString(jurnal.id))
            put("kelas", fsString(jurnal.kelas))
            put("tanggal", fsString(jurnal.tanggal))
            put("tadarus", fsString(jurnal.tadarus))
            put("murojaah", fsString(jurnal.murojaah))
            put("materi", fsString(jurnal.materi))
            put("timestamp", fsString(jurnal.timestamp))
        }
        val body = JSONObject().apply { put("fields", fields) }
        val encodedId = encodeDocId(jurnal.id)
        httpRequest("PATCH", "$FIRESTORE_BASE/jurnal/$encodedId", body.toString(), useAuth = true)
    }

    // --- PARSERS & HELPERS ---

    private fun parseUserDoc(doc: JSONObject): UserProfile? {
        val namePath = doc.optString("name", "")
        val docId = namePath.substringAfterLast("/")
        val fields = doc.optJSONObject("fields") ?: return null
        val kelasField = fields.optJSONObject("kelas")
        val kelasList = when {
            kelasField?.has("arrayValue") == true -> {
                val arr = kelasField.optJSONObject("arrayValue")?.optJSONArray("values")
                val list = mutableListOf<String>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val v = arr.getJSONObject(i).optString("stringValue", "")
                        if (v.isNotEmpty()) list.add(v)
                    }
                }
                list
            }
            kelasField?.has("stringValue") == true -> {
                val s = kelasField.optString("stringValue", "")
                if (s.isNotEmpty()) listOf(s) else emptyList()
            }
            else -> emptyList()
        }

        val email = readFsString(fields, "email")
        val rawRole = readFsString(fields, "role", "ustadz")
        // Enforce admin rule from index.html: if email is not in ADMIN_EMAILS and role == 'admin', demote to 'ustadz'
        val effectiveRole = if (email.isNotEmpty() && !ADMIN_EMAILS.contains(email.lowercase()) && rawRole == "admin") {
            "ustadz"
        } else {
            rawRole
        }

        return UserProfile(
            uid = docId,
            email = email,
            nama = readFsString(fields, "nama"),
            role = effectiveRole,
            kelas = kelasList,
            lembaga = readFsString(fields, "lembaga")
        )
    }

    private fun parseSantriDoc(doc: JSONObject): Santri? {
        val namePath = doc.optString("name", "")
        val docId = namePath.substringAfterLast("/")
        val fields = doc.optJSONObject("fields") ?: return null

        val riwayatList = mutableListOf<RiwayatItem>()
        val riwayatArr = fields.optJSONObject("riwayat")
            ?.optJSONObject("arrayValue")
            ?.optJSONArray("values")

        if (riwayatArr != null) {
            for (i in 0 until riwayatArr.length()) {
                val itemFields = riwayatArr.optJSONObject(i)
                    ?.optJSONObject("mapValue")
                    ?.optJSONObject("fields") ?: continue
                riwayatList.add(
                    RiwayatItem(
                        kehadiran = readFsString(itemFields, "kehadiran", "Hadir"),
                        jilid = readFsString(itemFields, "jilid", "-"),
                        halaman = readFsString(itemFields, "halaman", "-"),
                        surah = readFsString(itemFields, "surah", "-"),
                        ayat = readFsString(itemFields, "ayat", "-"),
                        nilaiHafalan = readFsString(itemFields, "nilaiHafalan", "-"),
                        nilaiJilid = readFsString(itemFields, "nilaiJilid", "-"),
                        keterangan = readFsString(itemFields, "keterangan", ""),
                        catatan = readFsString(itemFields, "catatan", ""),
                        timestamp = readFsString(itemFields, "timestamp", "")
                    )
                )
            }
        }

        val pembayaranList = mutableListOf<PembayaranItem>()
        val pembayaranArr = fields.optJSONObject("pembayaran")
            ?.optJSONObject("arrayValue")
            ?.optJSONArray("values")

        if (pembayaranArr != null) {
            for (i in 0 until pembayaranArr.length()) {
                val pFields = pembayaranArr.optJSONObject(i)
                    ?.optJSONObject("mapValue")
                    ?.optJSONObject("fields") ?: continue
                pembayaranList.add(
                    PembayaranItem(
                        id = readFsString(pFields, "id", "PAY_$i"),
                        jenis = readFsString(pFields, "jenis", "SPP"),
                        bulan = readFsString(pFields, "bulan", "-"),
                        nominal = readFsLong(pFields, "nominal", 0L),
                        tanggal = readFsString(pFields, "tanggal", ""),
                        catatan = readFsString(pFields, "catatan", ""),
                        penerima = readFsString(pFields, "penerima", ""),
                        timestamp = readFsString(pFields, "timestamp", ""),
                        updatedAt = readFsString(pFields, "updatedAt", "")
                    )
                )
            }
        }

        val idVal = readFsString(fields, "id").ifEmpty { docId }
        val ket = readFsString(fields, "keterangan", "")
        return Santri(
            id = idVal,
            nama = readFsString(fields, "nama", ""),
            kelas = readFsString(fields, "kelas", ""),
            kehadiran = readFsString(fields, "kehadiran", "Hadir"),
            jilid = readFsString(fields, "jilid", "-"),
            halaman = readFsString(fields, "halaman", "-"),
            surah = readFsString(fields, "surah", "-"),
            ayat = readFsString(fields, "ayat", "-"),
            nilaiHafalan = readFsString(fields, "nilaiHafalan", ket.ifEmpty { "Lancar" }),
            nilaiJilid = readFsString(fields, "nilaiJilid", ket.ifEmpty { "Lancar" }),
            keterangan = ket,
            catatan = readFsString(fields, "catatan", ""),
            timestamp = readFsString(fields, "timestamp", ""),
            riwayat = riwayatList,
            pembayaran = pembayaranList
        )
    }

    private fun parseJurnalDoc(doc: JSONObject): Jurnal? {
        val namePath = doc.optString("name", "")
        val docId = namePath.substringAfterLast("/")
        val fields = doc.optJSONObject("fields") ?: return null
        val idVal = readFsString(fields, "id").ifEmpty { docId }
        return Jurnal(
            id = idVal,
            kelas = readFsString(fields, "kelas", ""),
            tanggal = readFsString(fields, "tanggal", ""),
            tadarus = readFsString(fields, "tadarus", ""),
            murojaah = readFsString(fields, "murojaah", ""),
            materi = readFsString(fields, "materi", ""),
            timestamp = readFsString(fields, "timestamp", "")
        )
    }

    private fun fsString(value: String): JSONObject =
        JSONObject().apply { put("stringValue", value) }

    private fun fsInteger(value: Long): JSONObject =
        JSONObject().apply { put("integerValue", value.toString()) }

    private fun fsStringArray(values: List<String>): JSONObject {
        val arr = JSONArray()
        values.forEach { arr.put(fsString(it)) }
        return JSONObject().apply {
            put("arrayValue", JSONObject().apply { put("values", arr) })
        }
    }

    private fun readFsString(fields: JSONObject, key: String, defaultVal: String = ""): String {
        val obj = fields.optJSONObject(key) ?: return defaultVal
        return when {
            obj.has("stringValue") -> obj.optString("stringValue", defaultVal)
            obj.has("integerValue") -> obj.optString("integerValue", defaultVal)
            obj.has("doubleValue") -> obj.optDouble("doubleValue", 0.0).toLong().toString()
            else -> defaultVal
        }
    }

    private fun readFsLong(fields: JSONObject, key: String, defaultVal: Long = 0L): Long {
        val obj = fields.optJSONObject(key) ?: return defaultVal
        return when {
            obj.has("integerValue") -> obj.optString("integerValue", defaultVal.toString()).toLongOrNull() ?: defaultVal
            obj.has("doubleValue") -> obj.optDouble("doubleValue", defaultVal.toDouble()).toLong()
            obj.has("stringValue") -> obj.optString("stringValue", defaultVal.toString()).toLongOrNull() ?: defaultVal
            else -> defaultVal
        }
    }

    private fun encodeDocId(id: String): String =
        URLEncoder.encode(id, "UTF-8").replace("+", "%20")

    private suspend fun httpRequest(
        method: String,
        urlStr: String,
        body: String?,
        useAuth: Boolean,
        retried: Boolean = false
    ): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            if (method == "PATCH") {
                requestMethod = "POST"
                setRequestProperty("X-HTTP-Method-Override", "PATCH")
            } else {
                requestMethod = method
            }
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            if (useAuth && !idToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $idToken")
            }
            connectTimeout = 15000
            readTimeout = 15000
            if (body != null) {
                doOutput = true
            }
        }

        if (body != null) {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
        }

        val code = conn.responseCode
        if (code == 401 && useAuth && !retried) {
            conn.disconnect()
            if (refreshAuthToken()) {
                return httpRequest(method, urlStr, body, useAuth, retried = true)
            }
        }

        if (code in 200..299) {
            return BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
        } else {
            val errText = try {
                BufferedReader(InputStreamReader(conn.errorStream, Charsets.UTF_8)).use { it.readText() }
            } catch (e: Exception) {
                "HTTP Error $code"
            }
            val cleanMsg = try {
                JSONObject(errText).optJSONObject("error")?.optString("message") ?: errText
            } catch (e: Exception) {
                errText
            }
            throw Exception(cleanMsg)
        }
    }
}
