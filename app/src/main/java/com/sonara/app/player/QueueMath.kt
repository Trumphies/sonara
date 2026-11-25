package com.sonara.app.player

/**
 * Matematika indeks antrean MURNI (v1.10.16, tanpa Android — bisa
 * diuji di JVM). Dipakai [PlayerManager.removeFromQueue] dan
 * [PlayerManager.insertIntoQueue] (Undo swipe-hapus) supaya
 * penyesuaian indeks lagu berjalan teruji, bukan hanya dibaca.
 */

/**
 * Indeks lagu berjalan setelah satu lagu disisipkan di [insertAt]:
 * sisipan di depan (atau tepat di) lagu berjalan menggesernya +1;
 * sisipan di belakangnya atau antrean kosong (-1) tidak mengubah.
 */
fun indexAfterInsert(currentIndex: Int, insertAt: Int): Int =
    if (currentIndex >= 0 && insertAt <= currentIndex) currentIndex + 1 else currentIndex

/**
 * Indeks lagu berjalan setelah lagu di [removedIndex] dihapus
 * (bukan lagu berjalan — kasus itu ditangani pemutar sendiri
 * dengan playAt/stop): hapusan di depan menggesernya -1, sisanya
 * tidak mengubah.
 */
fun indexAfterRemove(currentIndex: Int, removedIndex: Int): Int =
    if (removedIndex < currentIndex) currentIndex - 1 else currentIndex

/**
 * Indeks berikutnya yang BOLEH diputar saat maju dari [fromIndex]
 * (v1.0.3): lewati semua indeks yang [isDisliked]. Mengembalikan -1
 * bila tidak ada lagi lagu yang boleh diputar sesudahnya. Advance
 * eksplisit (ketuk lagu) tidak memakai ini — niat user menang atas
 * daftar tidak suka; hanya perpindahan OTOMATIS/manual-next yang
 * melompati lagu yang tidak disukai.
 */
fun nextPlayableIndex(size: Int, fromIndex: Int, isDisliked: (Int) -> Boolean): Int {
    var i = fromIndex + 1
    while (i < size) {
        if (!isDisliked(i)) return i
        i++
    }
    return -1
}
