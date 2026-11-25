package com.sonara.app.data

/**
 * Keputusan merge saat pull sinkron (v1.10.4) — fungsi MURNI tanpa
 * dependensi Android supaya bisa diuji di JVM.
 *
 * BUG v1.10.3 ke bawah: item lokal dianggap "dihapus di perangkat
 * lain" bila `metaTs <= lastSync`. Untuk item yang BELUM PERNAH
 * berhasil di-push, metaTs = 0, dan 0 <= lastSync SELALU benar —
 * jadi playlist/liked lokal yang baru dibuat (atau yang push-nya
 * pernah gagal, mis. era HTTP 400) DIHAPUS dari perangkat pada
 * pull berikutnya, SEBELUM sempat di-push. Cloud kosong menimpa
 * data lokal. Itulah kenapa playlist buatan user "tidak kesimpen".
 *
 * Aturan yang benar (local-first): item lokal HANYA boleh dibuang
 * bila ia PERNAH berhasil di-push (metaTs > 0) dan tidak lebih baru
 * dari sinkron terakhir, tidak ada di cloud, dan bukan tombstone.
 * Item yang belum pernah di-push SELALU dipertahankan — pull tidak
 * boleh menghapus data lokal yang belum sempat naik ke cloud.
 */
fun shouldDropOnPull(
    metaTs: Long,
    lastSyncMs: Long,
    presentInCloud: Boolean,
    tombstoned: Boolean,
): Boolean {
    if (lastSyncMs <= 0) return false
    if (presentInCloud || tombstoned) return false
    if (metaTs <= 0) return false
    return metaTs <= lastSyncMs
}
