package com.sonara.app.data

import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Klien REST Firebase — TANPA Firebase SDK (build manual tanpa
 * Gradle). Hanya OkHttp + org.json, tanpa import Android apa pun,
 * supaya bentuk request-nya bisa diuji melawan stub HTTP lokal
 * di JVM biasa (lihat BUILD-v1.10.md).
 *
 * Endpoint yang dipakai (REST publik Firebase, stabil):
 * - Auth: Identity Toolkit signInWithIdp (tukar Google ID token
 *   → Firebase ID token + refresh token) dan Secure Token
 *   refresh (perbarui Firebase ID token).
 * - Data: Firestore REST v1 (list / patch upsert / delete /
 *   batchWrite), otorisasi Bearer Firebase ID token.
 */
class FirebaseRest(
    private val http: OkHttpClient,
    private val apiKey: String,
    private val projectId: String,
    private val identityBaseUrl: String = "https://identitytoolkit.googleapis.com",
    private val secureTokenBaseUrl: String = "https://securetoken.googleapis.com",
    private val firestoreBaseUrl: String = "https://firestore.googleapis.com",
) {
    class FirebaseException(message: String) : Exception(message)

    /**
     * Refresh token DITOLAK definitif oleh server (HTTP 400/401 dari
     * endpoint Secure Token) — sesi tidak bisa dipulihkan; pemanggil
     * BOLEH menghapus sesi tersimpan dan meminta login ulang.
     * (v1.10.7: sebelumnya SEMUA kegagalan refresh diperlakukan
     * sebagai sesi kedaluwarsa dan sesi dihapus — termasuk gagal
     * jaringan sesaat — sehingga user harus login ulang terus.)
     */
    class RefreshTokenInvalid(message: String) : Exception(message)

    /**
     * Kegagalan TRANSIEN saat refresh (jaringan putus, timeout,
     * HTTP 5xx) — sesi TETAP tersimpan; pemanggil DILARANG
     * menghapus sesi atau menampilkan "Session expired".
     */
    class RefreshTransient(message: String, cause: Throwable? = null) : Exception(message, cause)

    data class FirebaseSession(
        val uid: String,
        val email: String,
        val displayName: String,
        val photoUrl: String,
        val idToken: String,
        val refreshToken: String,
        /** Masa berlaku ID token dalam detik (dari server). */
        val expiresInSec: Long,
    )

    /** Dokumen Firestore mentah: path penuh + field (format Firestore). */
    data class RawDocument(val name: String, val fields: JSONObject) {
        /** ID dokumen = segmen terakhir path. */
        val docId: String get() = name.substringAfterLast("/")
    }

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private fun postJson(url: String, body: JSONObject, bearer: String? = null): JSONObject {
        val reqBuilder = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody(jsonMedia))
        if (bearer != null) reqBuilder.header("Authorization", "Bearer $bearer")
        val resp = http.newCall(reqBuilder.build()).execute()
        val text = resp.body?.string().orEmpty()
        resp.close()
        if (!resp.isSuccessful) throw FirebaseException("HTTP ${resp.code}: ${text.take(200)}")
        return JSONObject(text)
    }

    private fun patchJson(url: String, body: JSONObject, bearer: String? = null): JSONObject {
        // v1.10.2: upsert dokumen Firestore HARUS PATCH. v1.10 memakai
        // POST (via postJson) ke URL dokumen + updateMask.fieldPaths —
        // server menolaknya dengan HTTP 400 "Unknown name
        // updateMask.fieldPaths" karena field itu hanya ada di
        // UpdateDocumentRequest (PATCH), bukan CreateDocumentRequest.
        val reqBuilder = Request.Builder()
            .url(url)
            .patch(body.toString().toRequestBody(jsonMedia))
        if (bearer != null) reqBuilder.header("Authorization", "Bearer $bearer")
        val resp = http.newCall(reqBuilder.build()).execute()
        val text = resp.body?.string().orEmpty()
        resp.close()
        if (!resp.isSuccessful) throw FirebaseException("HTTP ${resp.code}: ${text.take(200)}")
        return JSONObject(text)
    }

    private fun postForm(url: String, form: FormBody): JSONObject {
        val resp = http.newCall(Request.Builder().url(url).post(form).build()).execute()
        val text = resp.body?.string().orEmpty()
        resp.close()
        if (!resp.isSuccessful) throw FirebaseException("HTTP ${resp.code}: ${text.take(200)}")
        return JSONObject(text)
    }

    private fun getJson(url: String, bearer: String): JSONObject {
        val resp = http.newCall(
            Request.Builder().url(url).header("Authorization", "Bearer $bearer").build(),
        ).execute()
        val text = resp.body?.string().orEmpty()
        resp.close()
        if (!resp.isSuccessful) throw FirebaseException("HTTP ${resp.code}: ${text.take(200)}")
        return if (text.isBlank()) JSONObject() else JSONObject(text)
    }

    private fun delete(url: String, bearer: String) {
        val resp = http.newCall(
            Request.Builder().url(url).delete()
                .header("Authorization", "Bearer $bearer").build(),
        ).execute()
        resp.close()
        // 404 = dokumen sudah tidak ada — dianggap sukses (idempotent).
        if (!resp.isSuccessful && resp.code != 404) {
            throw FirebaseException("HTTP ${resp.code} while deleting the document")
        }
    }

    // ---- Auth -------------------------------------------------------

    /** Tukar Google ID token → sesi Firebase (ID token + refresh token). */
    fun signInWithGoogleIdToken(googleIdToken: String): FirebaseSession {
        val body = JSONObject()
        body.put("postBody", "id_token=$googleIdToken&providerId=google.com")
        body.put("requestUri", "http://localhost")
        body.put("returnIdToken", true)
        body.put("returnSecureToken", true)
        val json = postJson(
            "$identityBaseUrl/v1/accounts:signInWithIdp?key=$apiKey",
            body,
        )
        return FirebaseSession(
            uid = json.optString("localId"),
            email = json.optString("email"),
            displayName = json.optString("displayName"),
            photoUrl = json.optString("photoUrl"),
            idToken = json.optString("idToken"),
            refreshToken = json.optString("refreshToken"),
            expiresInSec = json.optString("expiresIn", "3600").toLongOrNull() ?: 3600L,
        )
    }

    /**
     * Perbarui Firebase ID token memakai refresh token.
     *
     * Kontrak endpoint Secure Token (POST /v1/token?key=API_KEY,
     * form-urlencoded): respons sukses membawa ID token baru di
     * field `access_token` (field `id_token` HANYA fallback bila
     * ada — respons resmi tidak menjaminnya; v1.10.7 membaca
     * `id_token` saja sehingga SETIAP refresh mengembalikan token
     * kosong dan Jam-start yang selalu force-refresh selalu gagal).
     * Field `refresh_token` bisa berotasi; pemanggil memakai yang
     * baru bila ada, atau mempertahankan yang lama bila kosong.
     *
     * Klasifikasi error (v1.10.7): HTTP 400/401 dari endpoint
     * Secure Token = refresh token ditolak definitif
     * ([RefreshTokenInvalid]); IOException / HTTP lain = transien
     * ([RefreshTransient]). Pemanggil membedakan keduanya: hanya
     * penolakan definitif yang boleh menghapus sesi tersimpan.
     */
    fun refreshIdToken(refreshToken: String): FirebaseSession {
        val form = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .build()
        val json = try {
            postForm("$secureTokenBaseUrl/v1/token?key=$apiKey", form)
        } catch (e: FirebaseException) {
            val code = httpCodeOf(e)
            if (code == 400 || code == 401) {
                throw RefreshTokenInvalid("Refresh token rejected (HTTP $code)")
            }
            throw RefreshTransient("Token refresh failed (HTTP $code)", e)
        } catch (e: java.io.IOException) {
            throw RefreshTransient("Token refresh failed: no connection", e)
        }
        // v1.10.8: ID token baru ada di `access_token`; `id_token`
        // hanya fallback. Membaca `id_token` saja membuat refresh
        // SELALU menghasilkan token kosong.
        val newIdToken = json.optString("access_token")
            .ifEmpty { json.optString("id_token") }
        return FirebaseSession(
            uid = json.optString("user_id"),
            email = "",
            displayName = "",
            photoUrl = "",
            idToken = newIdToken,
            refreshToken = json.optString("refresh_token"),
            expiresInSec = json.optString("expires_in", "3600").toLongOrNull() ?: 3600L,
        )
    }

    // ---- Firestore --------------------------------------------------

    private fun documentsBase(): String =
        "$firestoreBaseUrl/v1/projects/$projectId/databases/(default)/documents"

    /** Encode satu nilai query parameter (fieldPaths, pageToken). */
    private fun urlEncode(value: String): String =
        java.net.URLEncoder.encode(value, Charsets.UTF_8.name())

    /** Daftar semua dokumen di koleksi `users/{uid}/{collection}`. */
    fun listDocuments(uid: String, collection: String, idToken: String): List<RawDocument> {
        val out = mutableListOf<RawDocument>()
        var pageToken: String? = null
        do {
            var url = "${documentsBase()}/users/$uid/$collection?pageSize=100"
            if (pageToken != null) url += "&pageToken=" + urlEncode(pageToken)
            val json = getJson(url, idToken)
            val docs = json.optJSONArray("documents") ?: JSONArray()
            for (i in 0 until docs.length()) {
                val d = docs.getJSONObject(i)
                out.add(RawDocument(d.optString("name"), d.optJSONObject("fields") ?: JSONObject()))
            }
            pageToken = json.optString("nextPageToken").takeIf { it.isNotEmpty() }
        } while (pageToken != null)
        return out
    }

    /**
     * Upsert dokumen (PATCH membuat dokumen bila belum ada).
     * [fields] dalam format nilai Firestore ({stringValue...}).
     *
     * v1.10.2: method HARUS PATCH — updateMask.fieldPaths hanya
     * dikenal UpdateDocumentRequest. POST ke URL dokumen ditolak
     * server dengan HTTP 400 (laporan HP v1.10.1).
     */
    fun upsertDocument(
        uid: String,
        collection: String,
        docId: String,
        fields: JSONObject,
        idToken: String,
    ) {
        val mask = fields.keys().asSequence().toList()
            .joinToString("&") { "updateMask.fieldPaths=" + urlEncode(it) }
        val body = JSONObject().put("fields", fields)
        patchJson(
            "${documentsBase()}/users/$uid/$collection/$docId?$mask",
            body,
            bearer = idToken,
        )
    }

    fun deleteDocument(uid: String, collection: String, docId: String, idToken: String) {
        delete("${documentsBase()}/users/$uid/$collection/$docId", idToken)
    }

    /** Hapus banyak dokumen sekaligus (maks 500 per request; di-chunk 100). */
    fun deleteDocuments(uid: String, collection: String, docIds: List<String>, idToken: String) {
        for (chunk in docIds.chunked(100)) {
            val writes = JSONArray()
            for (docId in chunk) {
                writes.put(
                    JSONObject().put(
                        "delete",
                        "${documentsBase()}/users/$uid/$collection/$docId",
                    ),
                )
            }
            postJson(
                "${documentsBase()}:batchWrite",
                JSONObject().put("writes", writes),
                bearer = idToken,
            )
        }
    }

    companion object {
        /**
         * Kode HTTP dari [FirebaseException] ("HTTP <kode>: ..."),
         * atau null bila pesannya tidak membawa kode. Murni —
         * dipakai pemanggil untuk membedakan 401 (token tidak
         * berlaku → refresh + ulangi sekali) dari error lain.
         */
        fun httpCodeOf(e: Exception): Int? {
            val m = Regex("HTTP (\\d+)").find(e.message.orEmpty()) ?: return null
            return m.groupValues.getOrNull(1)?.toIntOrNull()
        }

        /** True bila [e] adalah FirebaseException dengan kode HTTP [code]. */
        fun isHttpCode(e: Exception, code: Int): Boolean = httpCodeOf(e) == code

        // ---- Codec nilai Firestore ----

        fun stringField(v: String): JSONObject = JSONObject().put("stringValue", v)

        fun intField(v: Long): JSONObject = JSONObject().put("integerValue", v.toString())

        fun boolField(v: Boolean): JSONObject = JSONObject().put("booleanValue", v)

        fun timestampField(epochMs: Long): JSONObject =
            JSONObject().put("timestampValue", formatRfc3339(epochMs))

        fun arrayField(values: List<JSONObject>): JSONObject {
            val arr = JSONArray()
            values.forEach { arr.put(it) }
            return JSONObject().put("arrayValue", JSONObject().put("values", arr))
        }

        fun mapField(fields: JSONObject): JSONObject =
            JSONObject().put("mapValue", JSONObject().put("fields", fields))

        fun formatRfc3339(epochMs: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(java.util.Date(epochMs))
        }

        fun parseRfc3339(v: String): Long {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                sdf.parse(v)?.time ?: 0L
            } catch (_: Exception) {
                0L
            }
        }

        // ---- Pembaca field (defensif: field hilang = default) ----

        fun fieldString(fields: JSONObject, name: String): String =
            fields.optJSONObject(name)?.optString("stringValue").orEmpty()

        fun fieldLong(fields: JSONObject, name: String): Long =
            fields.optJSONObject(name)?.optString("integerValue")?.toLongOrNull() ?: 0L

        fun fieldBool(fields: JSONObject, name: String): Boolean =
            fields.optJSONObject(name)?.optBoolean("booleanValue") ?: false

        fun fieldTimestampMs(fields: JSONObject, name: String): Long {
            val raw = fields.optJSONObject(name)?.optString("timestampValue").orEmpty()
            return if (raw.isEmpty()) 0L else parseRfc3339(raw)
        }

        /** Array of mapValue → daftar fields tiap elemen. */
        fun fieldMapList(fields: JSONObject, name: String): List<JSONObject> {
            val out = mutableListOf<JSONObject>()
            val values = fields.optJSONObject(name)
                ?.optJSONObject("arrayValue")?.optJSONArray("values") ?: return out
            for (i in 0 until values.length()) {
                val item = values.optJSONObject(i) ?: continue
                out.add(item.optJSONObject("mapValue")?.optJSONObject("fields") ?: JSONObject())
            }
            return out
        }
    }
}
