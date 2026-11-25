package com.sonara.app.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Klien REST Realtime Database — TANPA Firebase SDK (pola yang sama
 * dengan FirebaseRest v1.10). Hanya OkHttp + org.json, tanpa import
 * Android apa pun, supaya bentuk request + parsing SSE-nya bisa
 * diuji melawan stub HTTP lokal di JVM biasa (lihat BUILD-v1.10.6.md).
 *
 * Aturan keamanan server (sudah published di console, JANGAN diubah):
 * /jam/$code boleh dibaca/ditulis hanya bila auth != null. Karena
 * itu SETIAP request membawa `?auth=<Firebase ID token>`. Token
 * TIDAK PERNAH di-log.
 *
 * Endpoint REST RTDB yang dipakai:
 * - GET/PUT/PATCH/POST/DELETE <base>/jam/$code[...].json?auth=...
 *   (POST ke koleksi mengembalikan {"name": pushKey}).
 * - GET <base>/.info/serverTimeOffset.json?auth=... — DITOLAK server
 *   REST (HTTP 400 "Invalid token in path", terverifikasi dari VM
 *   22 Nov 2026, dengan maupun tanpa auth). getServerTimeOffset()
 *   selalu mengembalikan 0 bila endpoint ini gagal — offset 0
 *   adalah fallback yang didokumentasikan, bukan error.
 * - Streaming: GET <base>/jam/$code.json?auth=... dengan header
 *   `Accept: text/event-stream` (event put/patch/keep-alive/cancel).
 */
class JamRest(
    private val http: OkHttpClient,
    private val sseHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // v1.10.12: readTimeout 45 detik, BUKAN 0 (tak terbatas).
        // Sebelumnya soket half-open (NAT timeout, ganti WiFi→
        // seluler, Doze) menggantung selamanya di readUtf8Line()
        // tanpa pernah dihitung sebagai failure — polling fallback
        // tidak pernah terpicu dan status tetap LIVE. 45 detik
        // memberi ruang untuk keep-alive RTDB (~30 detik) tanpa
        // false positive, tapi stall terdeteksi dalam ≤45 detik.
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build(),
    private val baseUrl: String = DEFAULT_BASE_URL,
) {
    class JamRestException(message: String, val code: Int = -1) : Exception(message)

    /**
     * Stream SSE tidak menerima byte apa pun dalam batas readTimeout
     * (v1.10.12). Berbeda dari JamRestException: pemanggil
     * (streamLoop) harus LANGSUNG masuk jalur polling, bukan
     * menghitungnya sebagai satu failure biasa dari tiga.
     */
    class SseStalledException : Exception("SSE stream stalled (no data within read timeout)")

    /** Event SSE yang sudah diurai. `data` = nilai JSON apa pun (objek/skalar/null). */
    data class SseEvent(val kind: String, val path: String, val data: Any?)

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private fun authSuffix(idToken: String): String =
        "?auth=" + java.net.URLEncoder.encode(idToken, Charsets.UTF_8.name())

    private fun sessionUrl(code: String, idToken: String, suffix: String = ""): String =
        "$baseUrl/jam/$code$suffix.json" + authSuffix(idToken)

    private fun nodeUrl(path: String, idToken: String): String =
        "$baseUrl/$path.json" + authSuffix(idToken)

    private fun executeText(request: Request): Pair<Int, String> {
        val resp = http.newCall(request).execute()
        val text = resp.body?.string().orEmpty()
        resp.close()
        return resp.code to text
    }

    /** Baca satu node sesi. Null = node tidak ada (body "null" / 404). */
    fun getSessionJson(code: String, idToken: String): JSONObject? {
        val (status, text) = executeText(
            Request.Builder().url(sessionUrl(code, idToken)).get().build(),
        )
        if (status == 401 || status == 403) throw JamRestException("Unauthorized", status)
        if (!isOk(status)) throw JamRestException("HTTP $status while reading the session", status)
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed == "null") return null
        return JSONObject(trimmed)
    }

    /** Tulis SELURUH node sesi (dipakai saat create). */
    fun putSession(code: String, body: JSONObject, idToken: String) {
        val (status, text) = executeText(
            Request.Builder().url(sessionUrl(code, idToken))
                .put(body.toString().toRequestBody(jsonMedia)).build(),
        )
        if (status == 401 || status == 403) throw JamRestException("Unauthorized", status)
        if (!isOk(status)) throw JamRestException("HTTP $status while creating the session", status)
    }

    /** PATCH dangkal pada satu path (mis. "jam/$code/state"). */
    fun patchNode(path: String, body: JSONObject, idToken: String) {
        val (status, _) = executeText(
            Request.Builder().url(nodeUrl(path, idToken))
                .patch(body.toString().toRequestBody(jsonMedia)).build(),
        )
        if (status == 401 || status == 403) throw JamRestException("Unauthorized", status)
        if (!isOk(status)) throw JamRestException("HTTP $status while writing $path", status)
    }

    /** Hapus satu path (presence saat leave). 404 = sudah tidak ada. */
    fun deleteNode(path: String, idToken: String) {
        val (status, _) = executeText(
            Request.Builder().url(nodeUrl(path, idToken)).delete().build(),
        )
        if (status == 401 || status == 403) throw JamRestException("Unauthorized", status)
        if (!isOk(status) && status != 404) {
            throw JamRestException("HTTP $status while deleting $path", status)
        }
    }

    /**
     * Append satu item antrean (POST → push key server).
     * Mengembalikan push key, atau "" bila server tidak mengembalikannya.
     */
    fun postQueueItem(code: String, item: JSONObject, idToken: String): String {
        val (status, text) = executeText(
            Request.Builder().url(sessionUrl(code, idToken, "/queue"))
                .post(item.toString().toRequestBody(jsonMedia)).build(),
        )
        if (status == 401 || status == 403) throw JamRestException("Unauthorized", status)
        if (!isOk(status)) throw JamRestException("HTTP $status while adding to the queue", status)
        return runCatching { JSONObject(text).optString("name") }.getOrDefault("")
    }

    /**
     * Append satu item ke koleksi anak sesi (POST → push key server).
     * Dipakai reaksi (v1.10.17, child "reactions") — jalur yang sama
     * dengan antrean, di bawah rules /jam/$code yang sama (auth-only,
     * tidak ada perubahan rules). Mengembalikan push key, atau ""
     * bila server tidak mengembalikannya.
     */
    fun postChildItem(code: String, child: String, item: JSONObject, idToken: String): String {
        val (status, text) = executeText(
            Request.Builder().url(sessionUrl(code, idToken, "/$child"))
                .post(item.toString().toRequestBody(jsonMedia)).build(),
        )
        if (status == 401 || status == 403) throw JamRestException("Unauthorized", status)
        if (!isOk(status)) throw JamRestException("HTTP $status while writing $child", status)
        return runCatching { JSONObject(text).optString("name") }.getOrDefault("")
    }

    /**
     * Offset jam server (ms). Endpoint .info DITOLAK REST API
     * (terverifikasi 400) — kembalikan 0 untuk SEMUA kegagalan.
     * Pemanggil TIDAK BOLEH menganggap 0 sebagai error.
     */
    fun getServerTimeOffset(idToken: String): Long {
        return try {
            val (status, text) = executeText(
                Request.Builder()
                    .url("$baseUrl/.info/serverTimeOffset.json" + authSuffix(idToken))
                    .get().build(),
            )
            if (!isOk(status)) return 0L
            val trimmed = text.trim()
            trimmed.toLongOrNull()
                ?: runCatching { JSONObject(trimmed).optLong("serverTimeOffset", 0L) }
                    .getOrDefault(0L)
        } catch (_: Exception) {
            0L
        }
    }

    // ---- Streaming (SSE) ------------------------------------------------

    @Volatile
    private var activeSseCall: okhttp3.Call? = null

    /** Batalkan stream SSE yang sedang berjalan (dipanggil saat leave). */
    fun cancelStream() {
        try {
            activeSseCall?.cancel()
        } catch (_: Exception) {
        }
        activeSseCall = null
    }

    /**
     * Buka stream SSE untuk satu node sesi dan teruskan event ke
     * [onEvent]. FUNGSI INI MEMBLOKIR sampai stream berakhir (server
     * menutup / koneksi putus / [cancelStream] dipanggil) atau
     * terjadi error HTTP — pemanggil bertanggung jawab atas retry.
     * [onConnected] dipanggil begitu respons HTTP sukses diterima
     * (sebelum event pertama) supaya pemanggil bisa melaporkan
     * status LIVE yang jujur. Event keep-alive diabaikan; event
     * cancel/auth_revoked melempar JamRestException. Bila tidak ada
     * byte selama readTimeout (45 detik), melempar
     * [SseStalledException] — pemanggil harus fallback ke polling.
     *
     * RTDB mengirim event awal `put` path "/" berisi seluruh node,
     * lalu `put`/`patch` per perubahan.
     */
    fun streamSession(
        code: String,
        idToken: String,
        onConnected: () -> Unit = {},
        onEvent: (SseEvent) -> Unit,
    ) {
        val request = Request.Builder()
            .url(sessionUrl(code, idToken))
            .header("Accept", "text/event-stream")
            .get()
            .build()
        val call = sseHttp.newCall(request)
        activeSseCall = call
        val resp = try {
            call.execute()
        } catch (e: IOException) {
            if (call.isCanceled()) return
            throw JamRestException("Stream connection failed: ${e.message}")
        }
        if (!resp.isSuccessful) {
            val text = runCatching { resp.body?.string().orEmpty() }.getOrDefault("")
            resp.close()
            if (resp.code == 401 || resp.code == 403) {
                throw JamRestException("Unauthorized", resp.code)
            }
            throw JamRestException("HTTP ${resp.code} while opening the stream", resp.code)
        }
        onConnected()
        val source = resp.body?.source()
            ?: run { resp.close(); throw JamRestException("Empty stream body") }
        try {
            var eventKind = ""
            val dataLines = StringBuilder()
            while (true) {
                val line = try {
                    source.readUtf8Line()
                } catch (e: java.net.SocketTimeoutException) {
                    // v1.10.12: tidak ada byte dalam readTimeout —
                    // soket stall. Bukan error biasa: lempar khusus
                    // supaya streamLoop langsung fallback ke polling.
                    if (call.isCanceled()) return
                    throw SseStalledException()
                } catch (e: IOException) {
                    if (call.isCanceled()) return
                    throw JamRestException("Stream read failed: ${e.message}")
                } ?: break // EOF — server menutup stream
                if (line.isEmpty()) {
                    // Baris kosong = akhir satu event.
                    if (eventKind.isNotEmpty() || dataLines.isNotEmpty()) {
                        dispatchSse(eventKind, dataLines.toString(), onEvent)
                    }
                    eventKind = ""
                    dataLines.clear()
                    continue
                }
                when {
                    line.startsWith("event:") -> eventKind = line.removePrefix("event:").trim()
                    line.startsWith("data:") -> {
                        if (dataLines.isNotEmpty()) dataLines.append('\n')
                        dataLines.append(line.removePrefix("data:").trim())
                    }
                    line.startsWith(":") -> Unit // komentar SSE, abaikan
                    else -> Unit
                }
            }
        } finally {
            runCatching { resp.close() }
            if (activeSseCall === call) activeSseCall = null
        }
    }

    private fun dispatchSse(kind: String, dataText: String, onEvent: (SseEvent) -> Unit) {
        val event = parseSseEvent(kind, dataText) ?: return
        onEvent(event)
    }

    /**
     * Parse SATU event SSE RTDB menjadi [SseEvent] (murni, tanpa I/O —
     * bisa diuji di JVM biasa). keep-alive / jenis tak dikenal /
     * kosong / JSON rusak -> null. cancel / auth_revoked -> throw
     * [JamRestException] seperti perilaku stream lama.
     */
    internal fun parseSseEvent(kind: String, dataText: String): SseEvent? {
        return when (kind) {
            "keep-alive" -> null
            "cancel" -> throw JamRestException("Stream cancelled by the server")
            "auth_revoked" -> throw JamRestException("Unauthorized", 401)
            "put", "patch" -> {
                if (dataText.isBlank()) return null
                val parsed: Any? = try {
                    JSONTokener(dataText).nextValue()
                } catch (_: Exception) {
                    return null
                }
                if (parsed !is JSONObject) return null
                val path = parsed.optString("path", "/")
                val data = if (parsed.isNull("data")) null else parsed.opt("data")
                SseEvent(kind, path, data)
            }
            else -> null
        }
    }

    private fun isOk(status: Int): Boolean = status in 200..299

    companion object {
        const val DEFAULT_BASE_URL: String =
            "https://mixtune-9f676-default-rtdb.asia-southeast1.firebasedatabase.app"
    }
}
