package com.sonara.app.data

/**
 * Keputusan tampilan identitas header Beranda (v1.10.4) — fungsi
 * MURNI tanpa dependensi Android supaya bisa diuji di JVM.
 *
 * Aturan (sesuai permintaan user: header = identitas akun Google):
 * - Belum login, atau nama akun kosong → tampilan lama persis
 *   seperti sebelum v1.10.4 ("tuan 👋" + avatar "T"); tidak ada
 *   foto, tidak ada sisa identitas akun yang sudah logout.
 * - Login + nama akun ada → nama akun apa adanya + 👋, inisial =
 *   huruf pertama nama (fallback bila foto gagal dimuat/offline),
 *   foto = photoUrl bila tidak kosong.
 */
data class HeaderIdentity(
    val displayText: String,
    val initial: String?,
    val photoUrl: String?,
)

fun resolveHeaderIdentity(
    signedIn: Boolean,
    displayName: String,
    photoUrl: String,
): HeaderIdentity {
    val name = displayName.trim()
    if (!signedIn || name.isEmpty()) {
        return HeaderIdentity(displayText = "friend 👋", initial = null, photoUrl = null)
    }
    return HeaderIdentity(
        displayText = "$name 👋",
        initial = name.first().uppercaseChar().toString(),
        photoUrl = photoUrl.takeIf { it.isNotBlank() },
    )
}
