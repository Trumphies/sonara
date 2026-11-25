package com.sonara.app.data

/**
 * Kebijakan murni tumpukan avatar anggota Jam (v1.10.44) — tanpa
 * import Android, dikompilasi standalone oleh harness144. Sumber
 * datanya HANYA [JamUiState.members] yang sudah ada (heartbeat
 * segar < 90 detik); tidak ada fetch / node RTDB baru.
 */

/** Jumlah avatar yang ditampilkan di nav kapsul + mark header. */
const val JAM_STACK_MAX_DEFAULT: Int = 3

/** Jumlah avatar di nav teks SENYAP (ruang lebih sempit). */
const val JAM_STACK_MAX_SENYAP: Int = 2

/** Hasil pemotongan daftar anggota untuk tumpukan avatar. */
data class JamAvatarStack(
    /** Anggota yang avatarnya digambar, urutan sama dengan [JamUiState.members]. */
    val shown: List<JamMember>,
    /** Jumlah anggota di luar [shown] — digambar sebagai chip "+N". */
    val extra: Int,
)

/**
 * Potong [members] ke [maxShown] avatar pertama; sisanya jadi
 * [JamAvatarStack.extra]. maxShown <= 0 berarti tidak ada yang
 * ditampilkan (semua jadi extra).
 */
fun jamAvatarStack(members: List<JamMember>, maxShown: Int): JamAvatarStack {
    val cap = maxShown.coerceAtLeast(0)
    val shown = members.take(cap)
    return JamAvatarStack(shown = shown, extra = (members.size - shown.size).coerceAtLeast(0))
}

/** Tumpukan hanya tampil saat sesi hidup DAN ada anggota segar. */
fun jamAvatarStackVisible(active: Boolean, members: List<JamMember>): Boolean =
    active && members.isNotEmpty()
