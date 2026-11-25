package com.sonara.app.data

/**
 * Kebijakan murni (tanpa Android) untuk emoji reaksi Jam yang terbang
 * melintasi layar (v1.10.43, perilaku Pulse — BUKAN salinan aset/lagu
 * Pulse). Satu floater per event reaksi, didedupe berdasarkan kunci
 * node reaksi; tidak ada node RTDB baru, tidak ada perubahan payload.
 *
 * Pola Pulse yang ditiru: emoji muncul di dekat baris reaksi, naik
 * melintasi layar sambil sedikit bergeser horizontal + berotasi,
 * lalu memudar (~3 detik, sama dengan tampilan mengambang 10.17),
 * dan tidak pernah menghalangi sentuhan (overlay pass-through —
 * ditegakkan di lapisan UI, bukan di sini).
 */

/** Satu emoji yang sedang terbang. [seed] in [0,1], deterministik dari kunci. */
data class FloatingReaction(
    val key: String,
    val reactionId: String,
    val senderName: String,
    val bornAtMs: Long,
    val seed: Float,
)

/** Batas floaters di layar — yang tertua dibuang dulu. */
const val FLOATING_REACTION_MAX = 8

/** Masa hidup satu floater, ms — sama dengan tampilan mengambang 10.17. */
const val FLOATING_REACTION_LIFETIME_MS = 3000L

/** Benih deterministik dari kunci reaksi, supaya posisi/rotasi stabil antar-render. */
fun floatingSeedFor(key: String): Float {
    val h = key.hashCode() and 0xFFFF
    return h / 65535f
}

/** Geser horizontal maksimum, dp — simetris, arah dari [seed]. */
fun floatingDriftDp(seed: Float): Float = (seed * 2f - 1f) * 48f

/** Rotasi akhir, derajat — simetris, arah dari [seed] (Pulse: 15 derajat). */
fun floatingRotationDeg(seed: Float): Float = (seed * 2f - 1f) * 15f

/** Fraksi lebar layar tempat floater muncul (Pulse: 15–85% lebar). */
fun floatingStartFraction(seed: Float): Float = 0.18f + seed * 0.64f

/** Progres animasi 0..1 dari waktu lahir; dijepit di kedua ujung. */
fun floatingReactionProgress(nowMs: Long, bornAtMs: Long): Float {
    val elapsed = nowMs - bornAtMs
    if (elapsed <= 0L) return 0f
    if (elapsed >= FLOATING_REACTION_LIFETIME_MS) return 1f
    return elapsed.toFloat() / FLOATING_REACTION_LIFETIME_MS.toFloat()
}

/** Buang floaters yang masa hidupnya habis (batas = persis lifetime). */
fun pruneFloatingReactions(
    current: List<FloatingReaction>,
    nowMs: Long,
): List<FloatingReaction> =
    current.filter { nowMs - it.bornAtMs < FLOATING_REACTION_LIFETIME_MS }

/**
 * Lahirkan floaters dari reaksi segar. Reaksi yang kuncinya sudah ada
 * di [current] dilewati (pengiriman ulang snapshot TIDAK boleh
 * memicu animasi kedua); kedaluwarsa dipangkas dulu; kelebihan dari
 * [max] membuang yang tertua. Bila [animate] false (reduced-motion),
 * daftar dikembalikan apa adanya — penelepon memakai badge statis.
 */
fun spawnFloatingReactions(
    current: List<FloatingReaction>,
    fresh: List<JamReaction>,
    nowMs: Long,
    max: Int = FLOATING_REACTION_MAX,
    animate: Boolean = true,
): List<FloatingReaction> {
    if (!animate || fresh.isEmpty()) return current
    val existingKeys = current.map { it.key }.toSet()
    val pruned = pruneFloatingReactions(current, nowMs)
    val spawned = fresh
        .filter { reaction -> reaction.key !in existingKeys }
        .map { reaction ->
            FloatingReaction(
                key = reaction.key,
                reactionId = reaction.reactionId,
                senderName = reaction.senderName,
                bornAtMs = nowMs,
                seed = floatingSeedFor(reaction.key),
            )
        }
    if (spawned.isEmpty()) return pruned
    return (pruned + spawned).takeLast(max)
}
