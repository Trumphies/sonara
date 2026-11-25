package com.sonara.app.data

/**
 * Logika MURNI Jam v1.10.42 (vote antrean, chat sesi, permintaan
 * kontrol tamu, presence orbit) — tanpa dependensi Android, supaya
 * bisa diuji di JVM biasa (lihat harness142).
 *
 * Node sesi baru di Realtime Database, di bawah /jam/$code yang
 * sama (rules auth-only, tidak diubah):
 *   votes:    {queueKey: {uid: true}} — satu vote per anggota per
 *             item; toggle = tulis/hapus child uid-nya sendiri
 *             (tanpa read-modify-write, tanpa host perantara).
 *             Jumlah vote = jumlah uid; urutan tampil = vote
 *             terbanyak dulu, tiebreak = urutan queueOrder.
 *   messages: {pushKey: {senderUid, senderName, text, sentAtMs}} —
 *             chat sesi, verbatim, tanpa moderasi (sesi berkode
 *             privat); dibatasi 50 terakhir (prune oleh pengirim).
 *   sharedControl: bool — host saja yang menulis; default false
 *             (tidak ada = perilaku lama: tamu terkunci).
 *   controlRequests: {pushKey: {action, positionMs, senderUid,
 *             senderName, sentAtMs}} — pola jumpRequests (10.34):
 *             tamu meminta, HANYA host yang menerapkan + menghapus
 *             child-nya; tamu tidak pernah memutar lokal.
 * Presence memakai node `members` yang sudah ada (10.6): tiap
 * anggota PATCH lastSeenMs ~10 detik; orbit hanya menampilkan
 * anggota yang heartbeat-nya < 30 detik (lihat
 * [isOrbitMemberFresh]) — daftar "listening" umum tetap memakai
 * batas 90 detik lama (isMemberFresh), supaya fluktuasi jaringan
 * sesaat tidak mengubah hitungan pendengar.
 */

/** Satu vote dihitung dari kehadiran uid di himpunan voter item itu. */
fun voteCounts(voteMap: Map<String, Set<String>>): Map<String, Int> =
    voteMap.mapValues { it.value.size }

/**
 * Urutan tampil antrean upcoming: vote terbanyak dulu; seri
 * mempertahankan urutan masuk ([upcoming] sudah dalam urutan
 * queueOrder — tiebreak stabil, tanpa mengubah queueOrder).
 * Lagu yang sedang diputar tidak ada di [upcoming] (derivasi
 * 10.18), jadi tidak pernah ikut diurutkan.
 */
fun sortUpcomingByVotes(
    upcoming: List<JamQueueItem>,
    counts: Map<String, Int>,
): List<JamQueueItem> {
    if (counts.isEmpty()) return upcoming
    return upcoming.sortedByDescending { counts[it.key] ?: 0 }
}

/** Keputusan tulis toggle vote: sudah vote -> hapus, belum -> tambah. */
enum class VoteToggle { ADD, REMOVE }

fun toggleVoteAction(votedByMe: Boolean): VoteToggle =
    if (votedByMe) VoteToggle.REMOVE else VoteToggle.ADD

// ---- Chat sesi ---------------------------------------------------------

/** Panjang maksimum satu pesan chat (karakter). */
const val JAM_CHAT_MAX_LENGTH = 500

/** Jeda minimum antar-pesan dari satu HP (rate limit lokal). */
const val JAM_CHAT_COOLDOWN_MS = 1_000L

/** Jumlah pesan yang dipertahankan di node `messages`. */
const val JAM_CHAT_KEEP_COUNT = 50

/** Ambang prune: di atas jumlah ini pengirim membuang yang terlama. */
const val JAM_CHAT_PRUNE_THRESHOLD = 60

/**
 * Bersihkan teks chat: pangkas spasi tepi, batasi panjang. Null =
 * kosong (tidak dikirim). Isi TIDAK diubah selain itu — verbatim,
 * tanpa sensor (sesi berkode privat).
 */
fun sanitizeChatText(raw: String, maxLength: Int = JAM_CHAT_MAX_LENGTH): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    return trimmed.take(maxLength)
}

/** True bila pesan boleh dikirim sekarang (cooldown sudah lewat). */
fun canSendChat(lastSentAtMs: Long, nowMs: Long, cooldownMs: Long = JAM_CHAT_COOLDOWN_MS): Boolean =
    nowMs - lastSentAtMs >= cooldownMs

/**
 * Kunci pesan TERLAMA yang harus dihapus supaya node `messages`
 * kembali ke [keep] entri. Kosong bila jumlah masih di bawah
 * [threshold] (histeresis 10: prune tidak jalan tiap kirim).
 * Urutan = urutan push key = urutan kirim ([messages] sudah terurut).
 */
fun chatKeysToPrune(
    messages: List<JamChatMessage>,
    keep: Int = JAM_CHAT_KEEP_COUNT,
    threshold: Int = JAM_CHAT_PRUNE_THRESHOLD,
): List<String> {
    if (messages.size <= threshold) return emptyList()
    return messages.take(messages.size - keep).map { it.key }
}

// ---- Permintaan kontrol tamu (shared control) --------------------------

/** Permintaan kontrol hanya dibaca selama masih segar. */
const val JAM_CONTROL_REQUEST_TTL_MS = 30_000L

/** True bila permintaan masih segar (umur < TTL; tanpa stempel dianggap segar — baru ditulis). */
fun isControlRequestFresh(request: JamControlRequest, nowMs: Long): Boolean =
    request.sentAtMs <= 0L || nowMs - request.sentAtMs < JAM_CONTROL_REQUEST_TTL_MS

/**
 * True bila host BOLEH menerapkan permintaan ini: shared control
 * sedang AKTIF (snapshot terbaru) DAN permintaan masih segar.
 * Mematikan toggle di tengah jalan membatalkan permintaan yang
 * belum diterapkan (host menghapus child-nya tanpa menerapkan).
 */
fun shouldApplyControlRequest(
    sharedControl: Boolean,
    request: JamControlRequest,
    nowMs: Long,
): Boolean = sharedControl && isControlRequestFresh(request, nowMs)

// ---- Presence orbit ----------------------------------------------------

/** Anggota tampil di orbit bila heartbeat-nya lebih segar dari ini. */
const val JAM_ORBIT_MEMBER_TIMEOUT_MS = 30_000L

/**
 * True bila anggota masih dianggap hadir DI ORBIT (batas 30 detik,
 * lebih ketat dari hitungan pendengar 90 detik). Jam server dipakai
 * lewat [clockOffsetMs] seperti isMemberFresh — tanpa jam absolut
 * untuk posisi, hanya untuk freshness presence.
 */
fun isOrbitMemberFresh(
    lastSeenMs: Long,
    nowMs: Long,
    clockOffsetMs: Long,
    timeoutMs: Long = JAM_ORBIT_MEMBER_TIMEOUT_MS,
): Boolean {
    if (lastSeenMs <= 0L) return false
    return (nowMs + clockOffsetMs) - lastSeenMs <= timeoutMs
}

/**
 * Hue avatar deterministik dari uid (0..359) — tanpa data profil
 * baru; anggota tanpa photoUrl digambar sebagai cakram inisial
 * dengan hue ini. Sama uid = sama hue di kedua HP.
 */
fun avatarHueFor(uid: String): Int {
    var h = 0
    for (ch in uid) h = (h * 31 + ch.code) % 360
    return if (h < 0) h + 360 else h
}

/**
 * Sudut anggota di orbit (derajat, 0 = kanan, -90 = atas): anggota
 * pertama di atas, sisanya searah jarum jam merata. Kosong bila
 * tidak ada anggota.
 */
fun orbitAngles(count: Int): List<Float> {
    if (count <= 0) return emptyList()
    return List(count) { i -> -90f + i * 360f / count }
}

/**
 * Kunci vote yang sudah basi: vote untuk item yang tidak lagi
 * upcoming DAN bukan lagu yang sedang diputar (kunci basi menumpuk
 * di node `votes` karena antrean bersifat append-only). Host
 * menghapusnya oportunistik; tamu tidak pernah menghapus vote
 * orang lain.
 */
fun staleVoteKeys(
    voteMap: Map<String, Set<String>>,
    upcomingKeys: Set<String>,
    currentKey: String,
): Set<String> =
    voteMap.keys.filter { it !in upcomingKeys && it != currentKey }.toSet()
