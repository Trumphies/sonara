package com.sonara.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom

/**
 * Model + logika MURNI Jamming Together (v1.10.6) — tanpa dependensi
 * Android, supaya protokolnya bisa diuji di JVM biasa melawan stub
 * RTDB (lihat BUILD-v1.10.6.md).
 *
 * Node sesi di Realtime Database: /jam/$code
 *   host:    {uid, name}
 *   state:   {videoId, title, artist, artworkUrl, durationMs,
 *             positionMs, playing, updatedAtMs}
 *   queue:   {pushKey: {videoId, title, artist, artworkUrl,
 *             addedBy, addedByName, addedAtMs}}
 *   members: {uid: {name, photoUrl, lastSeenMs}}
 *   reactions: {pushKey: {reactionId, senderUid, senderName,
 *             sentAtMs}} — v1.10.17, ephemeral (hanya < 60 detik
 *             yang dibaca; tidak ada riwayat reaksi)
 *   votes:    {queueKey: {uid: true}} — v1.10.42, satu vote per
 *             anggota per item antrean; jumlah = jumlah uid.
 *   messages: {pushKey: {senderUid, senderName, text, sentAtMs}} —
 *             v1.10.42, chat sesi (verbatim, maks 50 terakhir).
 *   sharedControl: bool — v1.10.42, host saja yang menulis;
 *             tidak ada = false (tamu terkunci, perilaku lama).
 *   controlRequests: {pushKey: {action, positionMs, senderUid,
 *             senderName, sentAtMs}} — v1.10.42, pola jumpRequests:
 *             tamu meminta kontrol, hanya host yang menerapkan.
 *   ended:   bool
 *   createdAtMs: number (server timestamp)
 *
 * `updatedAtMs` / `lastSeenMs` / `createdAtMs` ditulis dengan
 * sentinel server ".sv": "timestamp" — server menggantinya dengan
 * jam server. Semua pembacaan di sini memperlakukannya sebagai
 * angka epoch-ms biasa.
 */

/** Alfabet kode sesi: tanpa 0/O/1/I supaya tidak ambigu saat dibaca. */
private const val JAM_CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
const val JAM_CODE_LENGTH = 6

/** Kode sesi 6 karakter acak (tidak ambigu). */
fun generateJamCode(random: SecureRandom = SecureRandom()): String {
    val sb = StringBuilder(JAM_CODE_LENGTH)
    repeat(JAM_CODE_LENGTH) {
        sb.append(JAM_CODE_ALPHABET[random.nextInt(JAM_CODE_ALPHABET.length)])
    }
    return sb.toString()
}

/** Normalisasi input user: huruf besar, buang spasi/strip. */
fun normalizeJamCode(raw: String): String =
    raw.uppercase().filter { it.isLetterOrDigit() && it != ' ' && it != '-' }

/** True bila kode 6 karakter dari alfabet yang sah. */
fun isValidJamCode(code: String): Boolean =
    code.length == JAM_CODE_LENGTH && code.all { it in JAM_CODE_ALPHABET }

// ---- Deep link Jam (v1.10.11 custom scheme; v1.10.18 App Link) --------
// Dua bentuk diterima (keduanya mengalir ke pendingJamCode yang sama):
//  1) sonara://jam/<KODE> — custom scheme, tanpa hosting/domain.
//     Link hanya bekerja di HP yang sudah terinstall SONARA.
//     Skema lama mixtune:// tetap diterima sebagai fallback (rename
//     v1.0: MIXTUNE -> SONARA; link lama yang sudah dishare tetap jalan).
//  2) https://moloqo.github.io/mixtune/jam/<KODE> — App Link yang bisa
//     diklik di WhatsApp (bridge GitHub Pages; autoVerify di manifest;
//     URL bridge TIDAK berubah saat rename v1.0).
//     Bila App Link belum terverifikasi, bridge browser membuka app
//     lewat tombol; bila app belum terinstall, bridge menampilkan kode.
// Kode polos selalu dicantumkan bersama link sebagai cadangan (entri
// manual / HP tanpa app). Fungsi murni (tanpa android.net.Uri) supaya
// bisa diuji di JVM — lihat harness111 (custom) + harness118b (https).

/** Skema + host deep link Jam (custom scheme). */
const val JAM_DEEP_LINK_SCHEME = "sonara"

/** Skema lama (rename v1.0: MIXTUNE -> SONARA) — fallback, tetap diterima. */
const val JAM_DEEP_LINK_SCHEME_LEGACY = "mixtune"
const val JAM_DEEP_LINK_HOST = "jam"

/** Host + prefix path bridge App Link Jam (GitHub Pages; URL tidak berubah saat rename). */
const val JAM_BRIDGE_HOST = "moloqo.github.io"
const val JAM_BRIDGE_PATH_PREFIX = "/mixtune/jam/"

/** Link sesi bentuk custom scheme: sonara://jam/<KODE> (kode dinormalisasi). */
fun jamDeepLinkUrl(code: String): String =
    "$JAM_DEEP_LINK_SCHEME://$JAM_DEEP_LINK_HOST/${normalizeJamCode(code)}"

/** Link share sesi (v1.10.18): URL https bridge yang bisa diklik di WhatsApp. */
fun jamShareUrl(code: String): String =
    "https://$JAM_BRIDGE_HOST$JAM_BRIDGE_PATH_PREFIX${normalizeJamCode(code)}"

/** True bila string tampak seperti URI bridge https Jam (host + prefix path cocok, kode belum tentu sah). */
fun isJamBridgeUri(raw: String?): Boolean {
    if (raw.isNullOrBlank()) return false
    val lower = raw.trim().lowercase()
    if (!lower.startsWith("https://")) return false
    val afterScheme = lower.substringAfter("://", "")
    val authority = afterScheme.substringBefore('/', "")
    if (authority != JAM_BRIDGE_HOST) return false
    val path = "/" + afterScheme.substringAfter('/', "")
        .substringBefore('?')
        .substringBefore('#')
    return path == "/mixtune/jam" || path.startsWith(JAM_BRIDGE_PATH_PREFIX)
}

/** True bila string tampak seperti deep link Jam (bentuk custom ATAU https; kode belum tentu sah). */
fun isJamDeepLinkUri(raw: String?): Boolean {
    if (raw.isNullOrBlank()) return false
    val lower = raw.trim().lowercase()
    val custom = lower == "$JAM_DEEP_LINK_SCHEME://$JAM_DEEP_LINK_HOST" ||
        lower.startsWith("$JAM_DEEP_LINK_SCHEME://$JAM_DEEP_LINK_HOST/") ||
        lower == "$JAM_DEEP_LINK_SCHEME_LEGACY://$JAM_DEEP_LINK_HOST" ||
        lower.startsWith("$JAM_DEEP_LINK_SCHEME_LEGACY://$JAM_DEEP_LINK_HOST/")
    return custom || isJamBridgeUri(raw)
}

/**
 * Parse deep link Jam → kode sesi yang sah, atau null bila URI
 * malformed / kode kosong / kode tidak sah. Menerima skema+host
 * huruf apa pun (Android mencocokkan intent-filter tanpa peduli
 * kapital), satu segmen path kode, garis miring akhir, dan query/
 * fragment (diabaikan). Segmen path tambahan setelah kode ditolak.
 * Bentuk https: path HARUS tepat /mixtune/jam/<KODE> (segmen
 * "mixtune"/"jam" boleh huruf apa pun; kode dinormalisasi; path
 * bridge tidak berubah saat rename v1.0).
 */
fun parseJamDeepLink(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val trimmed = raw.trim()
    if (!isJamDeepLinkUri(trimmed)) return null
    val lower = trimmed.lowercase()
    if (lower.startsWith("https://")) {
        // "https://moloqo.github.io/mixtune/jam/<kode>[/?query#fragment]"
        val afterScheme = trimmed.substringAfter("://", "")
        val path = afterScheme.substringAfter('/', "")
            .substringBefore('?')
            .substringBefore('#')
        val segments = path.split('/').filter { it.isNotEmpty() }
        if (segments.size != 3) return null
        if (!segments[0].equals("mixtune", ignoreCase = true)) return null
        if (!segments[1].equals("jam", ignoreCase = true)) return null
        val code = normalizeJamCode(segments[2])
        return if (isValidJamCode(code)) code else null
    }
    // "sonara://jam/<kode>[/?query#fragment]" (atau skema legacy
    // "mixtune://jam/<kode>") → path setelah host.
    val afterScheme = trimmed.substringAfter("://", "")
    val path = afterScheme.substringAfter('/', "")
        .substringBefore('?')
        .substringBefore('#')
    val segments = path.split('/').filter { it.isNotEmpty() }
    if (segments.size != 1) return null
    val code = normalizeJamCode(segments[0])
    return if (isValidJamCode(code)) code else null
}

/** State pemutaran host pada satu titik waktu server. */
data class JamState(
    val videoId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val durationMs: Long,
    val positionMs: Long,
    val playing: Boolean,
    /** Epoch-ms jam SERVER saat state ini ditulis. */
    val updatedAtMs: Long,
    /**
     * Push key antrean sesi dari lagu yang SEDANG diputar host
     * (v1.10.18, "" bila lagu di luar antrean). Bersama
     * [consumedThroughKey], inilah yang membuat derivasi Up Next
     * sama persis di host dan tamu — bukan tebakan dari videoId.
     */
    val queueKey: String = "",
    /**
     * High-water mark konsumsi antrean (v1.10.18): push key
     * TERAKHIR yang sudah diputar host. Tidak pernah mundur
     * (host memutar ulang lagu lama tidak menghidupkan kembali
     * lagu yang sudah lewat). "" = belum ada lagu antrean yang
     * diputar (fallback derivasi videoId lama berlaku).
     */
    val consumedThroughKey: String = "",
) {
    /** Kunci versi: berubah tiap ada state baru dari host. */
    val versionKey: String get() = "$videoId@$updatedAtMs"
}

data class JamQueueItem(
    val key: String,
    val videoId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val addedBy: String,
    val addedByName: String,
    val addedAtMs: Long,
)

data class JamMember(
    val uid: String,
    val name: String,
    val photoUrl: String,
    val lastSeenMs: Long,
)

// ---- Permintaan lompat (jump request) di sesi Jam (v1.10.34) --------
// Tamu tidak boleh memutar lokal (akan desync dari host), tapi boleh
// MEMINTA host melompat ke lagu Up Next. Permintaan ditulis anggota
// mana pun ke node baru `jumpRequests` di bawah sesi yang sama
// (rules auth-only yang sama, tidak diubah; dibaca semua anggota lewat
// stream/polling yang sama, tidak ada koneksi kedua). HANYA host yang
// menerapkannya (lalu menghapus child-nya supaya tidak diterapkan dua
// kali); tamu tidak pernah menerapkan permintaan — termasuk
// permintaannya sendiri. Pola yang sama dengan `reactions` (v1.10.17):
// pesan ephemeral antar-anggota, bukan state pemutaran.

/** Satu permintaan lompat ke lagu antrean sesi. `key` = push key RTDB. */
data class JamJumpRequest(
    val key: String,
    /** Push key antrean sesi lagu tujuan (identitas utama). */
    val queueKey: String,
    /** videoId lagu tujuan (fallback bila kunci tidak ditemukan). */
    val videoId: String,
    val senderUid: String,
    val senderName: String,
    /** Epoch-ms jam SERVER saat ditulis (sentinel .sv). 0 = belum terurai. */
    val sentAtMs: Long,
)

/** Permintaan lompat hanya dibaca selama masih segar — node tidak pernah dibersihkan server. */
const val JAM_JUMP_REQUEST_TTL_MS = 30_000L

/** True bila permintaan masih segar (umur < TTL; tanpa stempel dianggap segar — baru ditulis). */
fun isJumpRequestFresh(request: JamJumpRequest, nowMs: Long): Boolean =
    request.sentAtMs <= 0L || nowMs - request.sentAtMs < JAM_JUMP_REQUEST_TTL_MS

/**
 * Cari item tujuan lompat di antrean sesi: cocok per push key dulu
 * (identitas salinan — dua salinan videoId sama dibedakan), fallback
 * kemunculan pertama videoId. Null = tujuan sudah tidak ada di antrean.
 */
fun resolveJumpTarget(
    queue: List<JamQueueItem>,
    queueKey: String,
    videoId: String,
): JamQueueItem? {
    if (queueKey.isNotEmpty()) {
        queue.firstOrNull { it.key == queueKey }?.let { return it }
    }
    if (videoId.isNotEmpty()) {
        queue.firstOrNull { it.videoId == videoId }?.let { return it }
    }
    return null
}

// ---- Urutan antrean sesi (queueOrder) + permintaan reorder (v1.10.36) --
// Antrean RTDB terurut push key (kronologis) — urutan itu TIDAK BISA
// diubah tanpa mengganti kunci, dan mengganti kunci akan merusak
// pelacakan konsumsi per kunci (10.18). Urutan tampil kini dibawa node
// terpisah `queueOrder`: daftar push key dalam urutan tampil yang
// diinginkan host. Parse: kunci yang disebut di queueOrder dulu
// (dalam urutan itu), lalu kunci lain (item baru) di belakang dalam
// urutan push key. Sesi lama tanpa node ini berperilaku persis seperti
// dulu (urutan push key). Tamu TIDAK PERNAH menulis queueOrder
// langsung — tamu menulis permintaan ke node `reorderRequests` (pola
// jumpRequests 10.34); host memvalidasi + menerapkan + menghapus
// child-nya. Semua fungsi di sini MURNI (diuji harness136).

/** Satu permintaan susun-ulang antrean sesi. `key` = push key RTDB. */
data class JamReorderRequest(
    val key: String,
    /** Urutan BARU kunci-kunci upcoming (permutasi penuh upcoming saat itu). */
    val orderedKeys: List<String>,
    val senderUid: String,
    val senderName: String,
    /** Epoch-ms jam SERVER saat ditulis (sentinel .sv). 0 = belum terurai. */
    val sentAtMs: Long,
)

/** Permintaan reorder hanya dibaca selama masih segar — node tidak pernah dibersihkan server. */
const val JAM_REORDER_REQUEST_TTL_MS = 30_000L

/** True bila permintaan masih segar (umur < TTL; tanpa stempel dianggap segar — baru ditulis). */
fun isReorderRequestFresh(request: JamReorderRequest, nowMs: Long): Boolean =
    request.sentAtMs <= 0L || nowMs - request.sentAtMs < JAM_REORDER_REQUEST_TTL_MS

/**
 * Baca daftar kunci terurut dari satu field objek JSON: bentuk ARRAY
 * (JSONArray, urutan apa adanya) atau bentuk OBJEK berkunci numerik
 * ("0","1","2",... — diurutkan numerik, karena urutan keys() JSONObject
 * tidak dijamin). Entri bukan-string / kosong dilewati. Field hilang
 * atau bertipe lain = daftar kosong (defensif: sesi lama / malformed).
 */
fun parseOrderedKeys(obj: JSONObject?, field: String): List<String> {
    if (obj == null) return emptyList()
    val raw = obj.opt(field) ?: return emptyList()
    val out = mutableListOf<String>()
    when (raw) {
        is JSONArray -> {
            for (i in 0 until raw.length()) {
                val v = raw.opt(i)
                if (v is String && v.isNotEmpty()) out.add(v)
            }
        }
        is JSONObject -> {
            val numericKeys = raw.keys().asSequence().toList()
                .mapNotNull { it.toIntOrNull()?.let { n -> n to it } }
                .sortedBy { it.first }
            for ((_, k) in numericKeys) {
                val v = raw.opt(k)
                if (v is String && v.isNotEmpty()) out.add(v)
            }
        }
        else -> Unit
    }
    return out
}

/** Daftar kunci urutan tampil sesi dari node root (`queueOrder`). */
fun parseQueueOrder(json: JSONObject?): List<String> =
    parseOrderedKeys(json, "queueOrder")

/**
 * Terapkan urutan tampil ke antrean: kunci yang disebut di [queueOrder]
 * dulu (dalam urutan itu), lalu kunci lain yang belum disebut (item
 * baru) dalam urutan masuk. Kunci basi di order (item sudah dihapus)
 * diabaikan.
 */
fun applyQueueOrder(queue: List<JamQueueItem>, queueOrder: List<String>): List<JamQueueItem> {
    if (queueOrder.isEmpty()) return queue
    val byKey = queue.associateBy { it.key }
    val seen = LinkedHashSet<String>()
    val out = ArrayList<JamQueueItem>(queue.size)
    for (k in queueOrder) {
        val item = byKey[k] ?: continue
        if (seen.add(k)) out.add(item)
    }
    for (item in queue) {
        if (seen.add(item.key)) out.add(item)
    }
    return out
}

/**
 * True bila [requestedKeys] adalah permutasi PERSIS [currentUpcomingKeys]:
 * himpunan sama, tanpa duplikat, tanpa kunci asing (termasuk kunci lagu
 * yang sedang diputar — lagu berjalan tidak ada di upcoming, jadi
 * permintaan yang menyebutnya otomatis tidak valid).
 */
fun validateReorderKeys(currentUpcomingKeys: List<String>, requestedKeys: List<String>): Boolean {
    if (requestedKeys.size != currentUpcomingKeys.size) return false
    return requestedKeys.toSet() == currentUpcomingKeys.toSet()
}

/**
 * Susun daftar urutan tampil PENUH (semua kunci antrean) dari urutan
 * upcoming yang baru: prefix yang sudah terkonsumsi (kunci di luar
 * upcoming, dalam urutan antrean saat ini) dipertahankan apa adanya,
 * lalu upcoming dalam urutan baru. Null bila urutan baru bukan
 * permutasi upcoming saat ini.
 */
fun buildReorderedQueueOrder(
    fullQueue: List<JamQueueItem>,
    currentUpcomingKeys: List<String>,
    newUpcomingKeys: List<String>,
): List<String>? {
    if (!validateReorderKeys(currentUpcomingKeys, newUpcomingKeys)) return null
    val upcomingSet = currentUpcomingKeys.toSet()
    val prefix = fullQueue.map { it.key }.filter { it !in upcomingSet }
    return prefix + newUpcomingKeys
}

/**
 * Rencana langkah moveQueue (from, to) yang mengubah [current] menjadi
 * [target]. Prasyarat: kedua daftar memuat himpunan kunci yang sama
 * (pemanggil memvalidasi dulu). Serakah per posisi: tiap langkah
 * memindahkan elemen yang benar ke posisinya. Dipakai host untuk
 * menyelaraskan antrean lokalnya dengan urutan sesi yang baru tanpa
 * menyentuh salinan yang tidak terpetakan.
 */
fun reorderPlan(current: List<String>, target: List<String>): List<Pair<Int, Int>> {
    val model = current.toMutableList()
    val plan = mutableListOf<Pair<Int, Int>>()
    for (i in model.indices) {
        if (i >= target.size) break
        if (model[i] == target[i]) continue
        var j = -1
        for (k in i + 1 until model.size) {
            if (model[k] == target[i]) {
                j = k
                break
            }
        }
        if (j < 0) continue
        val x = model.removeAt(j)
        model.add(i, x)
        plan.add(j to i)
    }
    return plan
}

// ---- Autoplay fallback saat antrean sesi kosong (v1.10.34) -----------
// Fungsi MURNI pemilih lagu: kandidat = lagu NYATA dari feed (hasil
// search YTM live, diteruskan pemanggil — TIDAK ADA fetch di sini, dan
// TIDAK ADA lagu karangan). Aturan binding (lihat BUILD-v1.10.34.md):
// antrean tak kosong = pemanggil tidak boleh memanggil ini sama sekali
// (antrean selalu menang); tanpa pengulangan langsung (lewati videoId
// yang sudah dimainkan sesi ini + yang masih di antrean + lagu yang
// baru berakhir); tanpa videoId = dilewati; habis kandidat = null
// (pemanggil menampilkan status jujur, bukan lagu palsu).

/** Jumlah maksimum kandidat yang dicoba resolve-nya (pemanggil). */
const val JAM_AUTOPLAY_MAX_RESOLVE_TRIES = 5

/**
 * Pilih lagu autoplay pertama yang layak dari kandidat feed, sesuai
 * urutan feed. Null bila tidak ada kandidat yang layak.
 */
fun pickAutoplayTrack(
    candidates: List<Track>,
    playedVideoIds: Set<String>,
    queuedVideoIds: Set<String>,
    currentVideoId: String?,
): Track? {
    for (track in candidates) {
        val videoId = track.youtubeVideoId.orEmpty()
        if (videoId.isBlank()) continue
        if (videoId == currentVideoId) continue
        if (videoId in playedVideoIds) continue
        if (videoId in queuedVideoIds) continue
        return track
    }
    return null
}

// ---- Reaksi cepat di sesi Jam (v1.10.17) ------------------------------
// Reaksi adalah ISI pesan antar-anggota sesi (bukan ikon UI): emoji
// dipakai sebagai konten reaksi yang dikirim, dan hanya 4 jenis yang
// sah. Tidak ada aset baru; tidak ada riwayat permanen — pembaca
// hanya memakai reaksi yang lebih muda dari TTL di bawah.

/** ID reaksi yang sah. */
const val JAM_REACTION_HEART = "heart"
const val JAM_REACTION_FIRE = "fire"
const val JAM_REACTION_CLAP = "clap"
const val JAM_REACTION_WOW = "wow"

/** Urutan tombol reaksi di kartu Jam (sama untuk host + tamu). */
val JAM_REACTION_IDS = listOf(
    JAM_REACTION_HEART,
    JAM_REACTION_FIRE,
    JAM_REACTION_CLAP,
    JAM_REACTION_WOW,
)

/** True bila ID reaksi dikenal. */
fun isValidReactionId(id: String): Boolean = id in JAM_REACTION_IDS

/**
 * Glyph konten reaksi. String kosong = ID tidak dikenal.
 * (Glyph ditulis literal di source UTF-8; parser Kotlin
 * menerimanya sebagai string biasa.)
 */
fun reactionEmoji(id: String): String = when (id) {
    JAM_REACTION_HEART -> "❤️"
    JAM_REACTION_FIRE -> "🔥"
    JAM_REACTION_CLAP -> "👏"
    JAM_REACTION_WOW -> "😮"
    else -> ""
}

/** Satu reaksi yang dikirim anggota sesi. `key` = push key RTDB. */
data class JamReaction(
    val key: String,
    val reactionId: String,
    val senderUid: String,
    val senderName: String,
    /** Epoch-ms jam SERVER saat reaksi ditulis (sentinel .sv). */
    val sentAtMs: Long,
)

/**
 * Reaksi hanya dibaca selama masih segar — node `reactions` di
 * server tidak pernah dihapus (sesi bersifat ephemeral), jadi
 * pembaca membuang yang lebih tua dari TTL ini. Reaksi tanpa
 * stempel waktu yang sah dibuang (sentinel .sv selalu terurai
 * menjadi angka saat dibaca kembali).
 */
const val JAM_REACTION_TTL_MS = 60_000L

/** Reaksi yang masih segar (umur < TTL), urutan push key dipertahankan. */
fun pruneReactions(reactions: List<JamReaction>, nowMs: Long): List<JamReaction> =
    reactions.filter { it.sentAtMs > 0L && nowMs - it.sentAtMs < JAM_REACTION_TTL_MS }

/** Jeda minimum antar-reaksi dari satu HP (rate limit lokal). */
const val JAM_REACTION_COOLDOWN_MS = 2_000L

/** True bila reaksi boleh dikirim sekarang (cooldown sudah lewat). */
fun canSendReaction(lastSentAtMs: Long, nowMs: Long, cooldownMs: Long = JAM_REACTION_COOLDOWN_MS): Boolean =
    nowMs - lastSentAtMs >= cooldownMs

/** Satu pesan chat sesi Jam (v1.10.42). `key` = push key RTDB. */
data class JamChatMessage(
    val key: String,
    val senderUid: String,
    val senderName: String,
    /** Teks verbatim (sudah dibersihkan pengirim: trim + cap 500). */
    val text: String,
    /** Epoch-ms jam SERVER saat ditulis (sentinel .sv). 0 = belum terurai. */
    val sentAtMs: Long,
)

// ---- Kontrol bersama (shared control) + permintaan kontrol (v1.10.42) --
// Toggle sesi "Everyone can control playback": default MATI (tamu
// terkunci seperti dulu — aturan yang dikonfirmasi user). Saat
// MENYALA, tamu boleh MEMINTA kontrol putar (play/pause, next,
// previous, seek) lewat node `controlRequests` — pola yang sama
// dengan `jumpRequests` (10.34): tamu menulis permintaan, HANYA
// host yang menerapkan + menghapus child-nya; tamu tidak pernah
// memutar lokal. Lompat-ke-lagu + hapus antrean TIDAK ikut toggle
// ini (tetap pola jump request / host-only seperti dulu).

/** Aksi kontrol putar yang bisa diminta tamu. */
enum class JamControlAction(val wireName: String) {
    TOGGLE("toggle"),
    NEXT("next"),
    PREVIOUS("previous"),
    SEEK("seek"),
}

/** Nama wire -> aksi, atau null bila tidak dikenal (entri malformed dilewati). */
fun controlActionFromName(name: String): JamControlAction? =
    JamControlAction.entries.firstOrNull { it.wireName == name }

/** Satu permintaan kontrol putar dari tamu. `key` = push key RTDB. */
data class JamControlRequest(
    val key: String,
    val action: JamControlAction,
    /** Posisi target untuk SEEK (ms); diabaikan aksi lain. */
    val positionMs: Long,
    val senderUid: String,
    val senderName: String,
    /** Epoch-ms jam SERVER saat ditulis (sentinel .sv). 0 = belum terurai. */
    val sentAtMs: Long,
)

/** Seluruh isi node sesi, hasil parse JSON RTDB. */
data class JamSnapshot(
    val hostUid: String = "",
    val hostName: String = "",
    val state: JamState? = null,
    /**
     * Urutan tampil: node `queueOrder` bila ada (v1.10.36), bila tidak
     * urutan push key = urutan penambahan (push ID terurut waktu).
     */
    val queue: List<JamQueueItem> = emptyList(),
    val members: List<JamMember> = emptyList(),
    /** Urutan push key = urutan pengiriman. */
    val reactions: List<JamReaction> = emptyList(),
    /** Permintaan lompat yang belum diterapkan (v1.10.34; urutan push key). */
    val jumpRequests: List<JamJumpRequest> = emptyList(),
    /** Permintaan reorder yang belum diterapkan (v1.10.36; urutan push key). */
    val reorderRequests: List<JamReorderRequest> = emptyList(),
    /** Peta vote: push key antrean -> himpunan uid voter (v1.10.42). */
    val voteMap: Map<String, Set<String>> = emptyMap(),
    /** Pesan chat sesi, urutan push key = urutan kirim (v1.10.42). */
    val messages: List<JamChatMessage> = emptyList(),
    /** True bila tamu boleh meminta kontrol putar (v1.10.42; host saja yang menulis). */
    val sharedControl: Boolean = false,
    /** Permintaan kontrol yang belum diterapkan (v1.10.42; urutan push key). */
    val controlRequests: List<JamControlRequest> = emptyList(),
    val ended: Boolean = false,
    val createdAtMs: Long = 0L,
) {
    val host: JamMember? get() = members.firstOrNull { it.uid == hostUid }
}

/** Parse satu node sesi RTDB (JSONObject) menjadi snapshot. Defensif: field hilang = default. */
fun parseJamSnapshot(json: JSONObject?): JamSnapshot? {
    if (json == null) return null
    val hostObj = json.optJSONObject("host")
    val stateObj = json.optJSONObject("state")
    val state = stateObj?.let {
        JamState(
            videoId = it.optString("videoId"),
            title = it.optString("title"),
            artist = it.optString("artist"),
            artworkUrl = it.optString("artworkUrl"),
            durationMs = it.optLong("durationMs", 0L),
            positionMs = it.optLong("positionMs", 0L),
            playing = it.optBoolean("playing", false),
            updatedAtMs = it.optLong("updatedAtMs", 0L),
            // v1.10.18: marker konsumsi — state lama (sebelum
            // 10.18) tidak membawanya → default "" = fallback
            // derivasi videoId seperti dulu.
            queueKey = it.optString("queueKey", ""),
            consumedThroughKey = it.optString("consumedThroughKey", ""),
        )
    }
    val queue = mutableListOf<JamQueueItem>()
    val queueObj = json.optJSONObject("queue")
    if (queueObj != null) {
        val keys = queueObj.keys().asSequence().toList().sorted()
        for (key in keys) {
            val item = queueObj.optJSONObject(key) ?: continue
            queue.add(
                JamQueueItem(
                    key = key,
                    videoId = item.optString("videoId"),
                    title = item.optString("title"),
                    artist = item.optString("artist"),
                    artworkUrl = item.optString("artworkUrl"),
                    addedBy = item.optString("addedBy"),
                    addedByName = item.optString("addedByName"),
                    addedAtMs = item.optLong("addedAtMs", 0L),
                ),
            )
        }
    }
    // v1.10.36: urutan tampil = node queueOrder bila ada (sesi lama
    // tanpa node ini tetap urutan push key — lihat applyQueueOrder).
    val orderedQueue = applyQueueOrder(queue, parseQueueOrder(json))
    val members = mutableListOf<JamMember>()
    val membersObj = json.optJSONObject("members")
    if (membersObj != null) {
        for (uid in membersObj.keys()) {
            val m = membersObj.optJSONObject(uid) ?: continue
            members.add(
                JamMember(
                    uid = uid,
                    name = m.optString("name"),
                    photoUrl = m.optString("photoUrl"),
                    lastSeenMs = m.optLong("lastSeenMs", 0L),
                ),
            )
        }
    }
    // v1.10.17: reaksi — urutan push key = urutan pengiriman.
    // Entri malformed (ID tidak dikenal / tanpa pengirim) dilewati;
    // pemangkasan umur (< 60 detik) dilakukan pembaca, bukan di sini,
    // supaya parse ini tetap murni dan bisa diuji langsung.
    val reactions = mutableListOf<JamReaction>()
    val reactionsObj = json.optJSONObject("reactions")
    if (reactionsObj != null) {
        val keys = reactionsObj.keys().asSequence().toList().sorted()
        for (key in keys) {
            val r = reactionsObj.optJSONObject(key) ?: continue
            val reactionId = r.optString("reactionId")
            if (!isValidReactionId(reactionId)) continue
            reactions.add(
                JamReaction(
                    key = key,
                    reactionId = reactionId,
                    senderUid = r.optString("senderUid"),
                    senderName = r.optString("senderName"),
                    sentAtMs = r.optLong("sentAtMs", 0L),
                ),
            )
        }
    }
    // v1.10.34: permintaan lompat — parse defensif seperti reaksi
    // (entri malformed dilewati; pemangkasan umur dilakukan pembaca
    // host, bukan di sini, supaya parse ini tetap murni dan teruji).
    val jumpRequests = mutableListOf<JamJumpRequest>()
    val jumpRequestsObj = json.optJSONObject("jumpRequests")
    if (jumpRequestsObj != null) {
        val keys = jumpRequestsObj.keys().asSequence().toList().sorted()
        for (key in keys) {
            val r = jumpRequestsObj.optJSONObject(key) ?: continue
            jumpRequests.add(
                JamJumpRequest(
                    key = key,
                    queueKey = r.optString("queueKey"),
                    videoId = r.optString("videoId"),
                    senderUid = r.optString("senderUid"),
                    senderName = r.optString("senderName"),
                    sentAtMs = r.optLong("sentAtMs", 0L),
                ),
            )
        }
    }
    // v1.10.36: permintaan reorder — parse defensif seperti jumpRequests
    // (entri tanpa orderedKeys yang valid dilewati; pemangkasan umur
    // dilakukan pembaca host, bukan di sini, supaya parse ini tetap
    // murni dan teruji).
    val reorderRequests = mutableListOf<JamReorderRequest>()
    val reorderRequestsObj = json.optJSONObject("reorderRequests")
    if (reorderRequestsObj != null) {
        val keys = reorderRequestsObj.keys().asSequence().toList().sorted()
        for (key in keys) {
            val r = reorderRequestsObj.optJSONObject(key) ?: continue
            val orderedKeys = parseOrderedKeys(r, "orderedKeys")
            if (orderedKeys.isEmpty()) continue
            reorderRequests.add(
                JamReorderRequest(
                    key = key,
                    orderedKeys = orderedKeys,
                    senderUid = r.optString("senderUid"),
                    senderName = r.optString("senderName"),
                    sentAtMs = r.optLong("sentAtMs", 0L),
                ),
            )
        }
    }
    return JamSnapshot(
        hostUid = hostObj?.optString("uid").orEmpty(),
        hostName = hostObj?.optString("name").orEmpty(),
        state = state,
        queue = orderedQueue,
        members = members,
        reactions = reactions,
        jumpRequests = jumpRequests,
        reorderRequests = reorderRequests,
        voteMap = parseVoteMap(json),
        messages = parseChatMessages(json),
        sharedControl = json.optBoolean("sharedControl", false),
        controlRequests = parseControlRequests(json),
        ended = json.optBoolean("ended", false),
        createdAtMs = json.optLong("createdAtMs", 0L),
    )
}

/**
 * Parse node `votes`: {queueKey: {uid: true}}. Hanya entri objek
 * yang dibaca; nilai selain boolean-true bukan vote (entri
 * malformed dilewati, seperti reaksi). Sesi lama tanpa node ini =
 * peta kosong (tidak ada vote).
 */
private fun parseVoteMap(json: JSONObject): Map<String, Set<String>> {
    val votesObj = json.optJSONObject("votes") ?: return emptyMap()
    val out = LinkedHashMap<String, Set<String>>()
    for (key in votesObj.keys()) {
        val votersObj = votesObj.optJSONObject(key) ?: continue
        val voters = LinkedHashSet<String>()
        for (uid in votersObj.keys()) {
            if (votersObj.optBoolean(uid, false)) voters.add(uid)
        }
        // Entri tanpa satu pun vote true BUKAN entri vote (mis. nilai
        // malformed "yes") — jangan buat kunci kosong di peta, supaya
        // hitungan + P2/P4 (dan tampilan jumlah) tidak memuat nol.
        if (voters.isNotEmpty()) out[key] = voters
    }
    return out
}

/**
 * Parse node `messages`: urutan push key = urutan kirim. Entri
 * tanpa teks / tanpa pengirim dilewati (malformed); pemangkasan
 * 50 terakhir dilakukan penulis, bukan di sini, supaya parse ini
 * tetap murni dan bisa diuji langsung.
 */
private fun parseChatMessages(json: JSONObject): List<JamChatMessage> {
    val messagesObj = json.optJSONObject("messages") ?: return emptyList()
    val keys = messagesObj.keys().asSequence().toList().sorted()
    val out = ArrayList<JamChatMessage>(keys.size)
    for (key in keys) {
        val m = messagesObj.optJSONObject(key) ?: continue
        val text = m.optString("text")
        if (text.isEmpty()) continue
        out.add(
            JamChatMessage(
                key = key,
                senderUid = m.optString("senderUid"),
                senderName = m.optString("senderName"),
                text = text,
                sentAtMs = m.optLong("sentAtMs", 0L),
            ),
        )
    }
    return out
}

/**
 * Parse node `controlRequests`: pola jumpRequests (entri malformed
 * — aksi tidak dikenal — dilewati; pemangkasan umur dilakukan
 * pembaca host, bukan di sini).
 */
private fun parseControlRequests(json: JSONObject): List<JamControlRequest> {
    val reqObj = json.optJSONObject("controlRequests") ?: return emptyList()
    val keys = reqObj.keys().asSequence().toList().sorted()
    val out = ArrayList<JamControlRequest>(keys.size)
    for (key in keys) {
        val r = reqObj.optJSONObject(key) ?: continue
        val action = controlActionFromName(r.optString("action")) ?: continue
        out.add(
            JamControlRequest(
                key = key,
                action = action,
                positionMs = r.optLong("positionMs", 0L),
                senderUid = r.optString("senderUid"),
                senderName = r.optString("senderName"),
                sentAtMs = r.optLong("sentAtMs", 0L),
            ),
        )
    }
    return out
}

/**
 * Posisi yang SEHARUSNYA sedang diputar tamu, dari state host.
 *
 * v1.10.12: TANPA jam absolut. Sebelumnya posisi dihitung dari
 * `updatedAtMs` jam server + offset jam (yang selalu 0 — endpoint
 * .info ditolak REST), sehingga skew jam 1–3 detik antar-HP
 * langsung menjadi offset sinkron, dan tamu bisa mendahului host
 * sebesar waktu muat host (state ditulis saat host masih
 * buffering). Kini yang dipakai adalah waktu TERIMA lokal:
 * posisi saat ditulis + waktu yang berlalu SEJAK SNAPSHOT TIBA di
 * HP tamu (`receivedAtMs`, dicatat JamSessionManager saat versi
 * state berubah). Sisa error = latensi pengiriman (0,1–0,5 dtk),
 * bukan skew jam. Bila host pause, posisi = posisi yang ditulis
 * (tidak berjalan). Hasil di-cap di `durationMs` bila diketahui —
 * ekstrapolasi tidak boleh melewati akhir lagu.
 *
 * `updatedAtMs` tetap dipakai untuk `versionKey` + freshness, bukan
 * untuk posisi.
 */
fun expectedPositionMs(state: JamState, nowMs: Long, receivedAtMs: Long): Long {
    if (!state.playing) return state.positionMs.coerceAtLeast(0L).capAtDuration(state.durationMs)
    val elapsed = (nowMs - receivedAtMs).coerceAtLeast(0L)
    return (state.positionMs + elapsed).capAtDuration(state.durationMs)
}

private fun Long.capAtDuration(durationMs: Long): Long =
    if (durationMs > 0L) coerceAtMost(durationMs) else this

/** True bila posisi lokal meleset dari posisi host melebihi toleransi (v1.10.12: 750ms, dari 2000ms). */
fun needsSeek(localMs: Long, expectedMs: Long, thresholdMs: Long = 750L): Boolean {
    val diff = localMs - expectedMs
    return diff > thresholdMs || diff < -thresholdMs
}

// ---- Koreksi drift halus via playback rate (v1.10.15) ----------------
// Lompat-seek 750ms terdengar sebagai "loncat" saat kedua HP berbunyi
// berdekatan. Selisih MENENGAH dibetulkan pelan-pelan dengan kecepatan
// putar (tamu mengejar/menahan tanpa lompat); seek hanya untuk selisih
// BESAR. Keputusan MURNI supaya bisa diuji di JVM (lihat harness115).

/** Kecepatan pengejaran: tamu tertinggal → sedikit lebih cepat. */
const val DRIFT_RATE_SPEED_UP = 1.02f

/** Kecepatan penahanan: tamu mendahului → sedikit lebih lambat. */
const val DRIFT_RATE_SLOW_DOWN = 0.98f

/** Di bawah selisih ini koreksi rate dianggap selesai → kembali 1.0×. */
const val DRIFT_RATE_SETTLED_BELOW_MS = 250L

/** Di atas selisih ini rate tidak cukup → seek seperti biasa. */
const val DRIFT_RATE_SEEK_ABOVE_MS = 2000L

/** Hasil keputusan koreksi drift untuk satu evaluasi. */
enum class DriftCorrection {
    /** Tidak ada tindakan (dalam toleransi, atau rate sedang bekerja). */
    NONE,
    /** Tamu tertinggal → mainkan 1.02×. */
    SPEED_UP,
    /** Tamu mendahului → mainkan 0.98×. */
    SLOW_DOWN,
    /** Selisih < 250ms sementara rate aktif → kembalikan 1.0×. */
    RESET_RATE,
    /** Selisih > 2000ms → lompat-seek seperti biasa. */
    SEEK,
}

/**
 * Keputusan koreksi drift dari selisih posisi tamu vs host.
 *
 * `diff = local - expected`: positif = tamu di depan, negatif = tamu
 * tertinggal. Batas mengikuti [needsSeek] (750ms) dan aturan seek
 * besar (2000ms): selisih di antaranya → rate; di atasnya → seek;
 * di bawah 250ms dengan rate aktif → reset. Selisih 250–750ms dengan
 * rate AKTIF dipertahankan (NONE = jangan ubah rate; koreksi sedang
 * berjalan menuju < 250ms). Tepat di batas tidak memicu tindakan
 * (konsisten dengan `needsSeek`: hanya `>` yang meleset).
 */
fun driftCorrection(localMs: Long, expectedMs: Long, currentRate: Float): DriftCorrection {
    val diff = localMs - expectedMs
    if (diff > DRIFT_RATE_SEEK_ABOVE_MS || diff < -DRIFT_RATE_SEEK_ABOVE_MS) return DriftCorrection.SEEK
    if (diff > 750L) return DriftCorrection.SLOW_DOWN
    if (diff < -750L) return DriftCorrection.SPEED_UP
    if (currentRate != 1.0f && (diff < DRIFT_RATE_SETTLED_BELOW_MS && diff > -DRIFT_RATE_SETTLED_BELOW_MS)) {
        return DriftCorrection.RESET_RATE
    }
    return DriftCorrection.NONE
}

/**
 * Antrean sesi yang MASIH AKAN DIPUTAR, relatif terhadap lagu yang
 * sedang diputar host (v1.10.10; derivasi per kunci v1.10.18).
 *
 * Antrean RTDB bersifat append-only (item tidak pernah dihapus saat
 * dimainkan), jadi daftar mentahnya mencakup lagu yang sudah lewat.
 * Derivasi memakai marker konsumsi per PUSH KEY dari state host
 * (bukan kecocokan videoId semata), supaya:
 *  - lagu yang sudah diputar TIDAK PERNAH muncul lagi, termasuk
 *    saat host lalu memutar lagu dari LUAR antrean (state lama:
 *    "current tidak dikenal → seluruh antrean" membangkitkan
 *    kembali 10 lagu yang sudah diputar — bug laporan 21:06);
 *  - videoId yang sama 2x di antrean dibedakan per salinan
 *    (salinan yang sedang diputar tidak tampil di Up Next);
 *  - host dan tamu menghitung dari state yang SAMA → daftar sama.
 *
 * Urutan aturan (pertama yang cocok menang):
 *  1. consumedThroughKey (high-water mark): buang sampai kunci
 *     itu; bila kuncinya sudah dihapus dari antrean, buang semua
 *     kunci yang <= marker (urutan push key = kronologis).
 *  2. queueKey lagu berjalan (bila marker belum ada): buang
 *     sampai kunci itu.
 *  3. Fallback lama: kemunculan PERTAMA videoId berjalan; tidak
 *     dikenal → seluruh antrean (benar hanya bila antrean memang
 *     belum pernah diputar sama sekali).
 *
 * Satu-satunya sumber kebenaran daftar "akan datang" — dipakai
 * dialog Jam DAN tab Up Next, sehingga kedua daftar selalu sama
 * (lagu sama, urutan sama) dan maju bersama saat lagu berganti.
 */
fun upcomingJamQueue(queue: List<JamQueueItem>, currentVideoId: String?): List<JamQueueItem> =
    upcomingJamQueue(queue, null, currentVideoId)

fun upcomingJamQueue(queue: List<JamQueueItem>, state: JamState?): List<JamQueueItem> =
    upcomingJamQueue(queue, state, state?.videoId)

private fun upcomingJamQueue(
    queue: List<JamQueueItem>,
    state: JamState?,
    currentVideoId: String?,
): List<JamQueueItem> {
    val marker = state?.consumedThroughKey.orEmpty()
    if (marker.isNotEmpty()) {
        val idx = queue.indexOfFirst { it.key == marker }
        if (idx >= 0) return queue.drop(idx + 1)
        // Marker terhapus (item terputar dihapus host): semua kunci
        // yang <= marker sudah lewat (push key terurut kronologis).
        return queue.filter { it.key > marker }
    }
    val currentKey = state?.queueKey.orEmpty()
    if (currentKey.isNotEmpty()) {
        val idx = queue.indexOfFirst { it.key == currentKey }
        if (idx >= 0) return queue.drop(idx + 1)
    }
    if (currentVideoId.isNullOrBlank()) return queue
    val idx = queue.indexOfFirst { it.videoId == currentVideoId }
    if (idx < 0) return queue
    return queue.drop(idx + 1)
}

/**
 * High-water mark konsumsi: yang LEBIH JAUH di antrean menang,
 * tidak pernah mundur. Kunci yang hilang dari antrean (dihapus)
 * dibandingkan leksikografis (urutan push key = kronologis).
 * Fungsi murni — dipakai host saat mempublish state.
 */
fun advanceConsumedThrough(
    queue: List<JamQueueItem>,
    currentMarker: String,
    candidateKey: String,
): String {
    if (candidateKey.isEmpty()) return currentMarker
    if (currentMarker.isEmpty()) return candidateKey
    if (currentMarker == candidateKey) return currentMarker
    val curIdx = queue.indexOfFirst { it.key == currentMarker }
    val candIdx = queue.indexOfFirst { it.key == candidateKey }
    return if (curIdx >= 0 && candIdx >= 0) {
        if (candIdx > curIdx) candidateKey else currentMarker
    } else {
        // Satu/both hilang (dihapus): urutan leksikografis push key.
        if (candidateKey > currentMarker) candidateKey else currentMarker
    }
}

// ---- Target prefetch tamu (v1.10.35) ---------------------------------
// v1.10.15 hanya mem-prefetch SATU lagu (item pertama antrean yang
// akan datang) — lompat host ke lagu ke-2/ke-3 antrean, atau lagu
// yang antreannya berubah sebelum sempat di-prefetch, selalu jatuh
// ke cold resolve di momen ganti lagu. Kini tamu mem-prefetch
// beberapa lagu ke depan (lihat PREFETCH_TARGET_COUNT). Fungsi
// MURNI supaya daftarnya bisa diuji di JVM (lihat harness135).

/** Jumlah lagu ke depan yang di-prefetch tamu (v1.10.35, dari 1). */
const val PREFETCH_TARGET_COUNT = 3

/** Cooldown antar-percobaan prefetch untuk SATU videoId (v1.10.35, dari 30 detik global). */
const val PREFETCH_COOLDOWN_MS = 10_000L

/**
 * videoId lagu-lagu ke depan yang layak di-prefetch: [maxTargets]
 * pertama dari [upcomingJamQueue] (sudah membuang lagu yang sedang
 * diputar), tanpa duplikat, tanpa id kosong, tanpa lagu yang sedang
 * diputar tamu ([currentVideoId]). Urutan antrean dipertahankan.
 */
fun prefetchTargetVideoIds(
    queue: List<JamQueueItem>,
    state: JamState?,
    currentVideoId: String?,
    maxTargets: Int = PREFETCH_TARGET_COUNT,
): List<String> {
    if (maxTargets <= 0) return emptyList()
    val seen = LinkedHashSet<String>()
    for (item in upcomingJamQueue(queue, state)) {
        val vid = item.videoId
        if (vid.isBlank() || vid == currentVideoId || !seen.add(vid)) continue
        if (seen.size >= maxTargets) break
    }
    return seen.toList()
}

/** JamQueueItem → Track (satu konstruksi untuk append host + refleksi Up Next). */
fun JamQueueItem.toTrack(): Track = Track(
    id = "ytm:$videoId",
    title = title,
    artist = artist,
    source = Source.YOUTUBE_MUSIC,
    engine = Engine.YOUTUBE,
    youtubeVideoId = videoId,
    imageUrl = artworkUrl.takeIf { it.isNotEmpty() },
)

/** True bila anggota masih dianggap hadir (heartbeat < 90 detik). */fun isMemberFresh(lastSeenMs: Long, nowMs: Long, clockOffsetMs: Long, timeoutMs: Long = 90_000L): Boolean {
    if (lastSeenMs <= 0L) return false
    return (nowMs + clockOffsetMs) - lastSeenMs <= timeoutMs
}

/** True bila host masih dianggap hadir (batas lebih ketat: 60 detik). */
fun isHostFresh(lastSeenMs: Long, nowMs: Long, clockOffsetMs: Long): Boolean =
    isMemberFresh(lastSeenMs, nowMs, clockOffsetMs, 60_000L)

// ---- Penggabungan event SSE ke node JSON ----------------------------
// Event SSE RTDB: put/patch + path + data (JSON apa pun, termasuk
// skalar/null). Di sini node sesi dipertahankan sebagai JSONObject
// dan setiap event digabungkan ke dalamnya, lalu di-parse ulang
// menjadi JamSnapshot. Fungsi murni — diuji di harness JVM.

/** Gabungkan satu event SSE ke node root. Mengembalikan node BARU (deep copy). */
fun mergeSseEvent(root: JSONObject, path: String, data: Any?, isPatch: Boolean): JSONObject {
    val copy = JSONObject(root.toString())
    val cleanPath = path.trim('/').takeIf { it.isNotEmpty() }
    if (cleanPath == null) {
        // Path "/" — put mengganti seluruh node, patch menggabung
        // anak-anak tingkat atas.
        if (isPatch && data is JSONObject) {
            for (key in data.keys()) copy.put(key, data.get(key))
        } else if (data is JSONObject) {
            for (key in copy.keys().asSequence().toList()) copy.remove(key)
            for (key in data.keys()) copy.put(key, data.get(key))
        } else if (data == null || data == JSONObject.NULL) {
            for (key in copy.keys().asSequence().toList()) copy.remove(key)
        }
        return copy
    }
    val segments = cleanPath.split("/")
    if (isPatch && data is JSONObject) {
        // Patch: gabung dangkal di node tujuan.
        val target = navigateTo(copy, segments, create = true) ?: return copy
        for (key in data.keys()) target.put(key, data.get(key))
    } else {
        setNested(copy, segments, data)
    }
    return copy
}

private fun navigateTo(root: JSONObject, segments: List<String>, create: Boolean): JSONObject? {
    var current = root
    for (segment in segments) {
        val next = current.optJSONObject(segment)
        if (next != null) {
            current = next
        } else if (create) {
            val created = JSONObject()
            current.put(segment, created)
            current = created
        } else {
            return null
        }
    }
    return current
}

private fun setNested(root: JSONObject, segments: List<String>, data: Any?) {
    if (segments.isEmpty()) return
    val parent = navigateTo(root, segments.dropLast(1), create = true) ?: return
    val last = segments.last()
    if (data == null || data == JSONObject.NULL) {
        parent.remove(last)
    } else {
        parent.put(last, data)
    }
}

/** Encode JamState menjadi JSONObject untuk PUT/PATCH (tanpa updatedAtMs — server yang mengisi). */
fun jamStateToJson(state: JamState, includeServerTimestamp: Boolean = true): JSONObject {
    val o = JSONObject()
    o.put("videoId", state.videoId)
    o.put("title", state.title)
    o.put("artist", state.artist)
    o.put("artworkUrl", state.artworkUrl)
    o.put("durationMs", state.durationMs)
    o.put("positionMs", state.positionMs)
    o.put("playing", state.playing)
    // v1.10.18: marker konsumsi (selalu ditulis, kosong pun, supaya
    // state lama yang basi tidak disangka punya marker).
    o.put("queueKey", state.queueKey)
    o.put("consumedThroughKey", state.consumedThroughKey)
    if (includeServerTimestamp) {
        o.put("updatedAtMs", JSONObject().put(".sv", "timestamp"))
    } else {
        o.put("updatedAtMs", state.updatedAtMs)
    }
    return o
}

/** Sentinel server timestamp RTDB untuk field waktu lain (presence, createdAt). */
fun serverTimestampJson(): JSONObject = JSONObject().put(".sv", "timestamp")
