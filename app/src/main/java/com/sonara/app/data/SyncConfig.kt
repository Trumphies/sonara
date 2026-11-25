package com.sonara.app.data

/**
 * Konfigurasi sinkron cloud (Firebase Spark, gratis tanpa kartu).
 *
 * Nilai di bawah di-wire dari file google-services.json ASLI yang
 * diunduh dari Firebase console (project mixtune-9f676) — setup
 * console sudah dilakukan, TIDAK ada langkah user. JANGAN menulis
 * ulang nilai-nilai ini ke laporan/log mana pun; BUILD-v1.10.md
 * hanya menyebut project_id + sumber file-nya.
 *
 * Bila nilai di bawah dikosongkan kembali ke placeholder, app jalan
 * normal 100% dalam mode signed-out dan tombol login menampilkan
 * status jujur "sinkron cloud belum dikonfigurasi" — tidak ada
 * error, tidak ada crash.
 */
object SyncConfig {
    /** Web API Key project Firebase (Project Settings → General). */
    const val FIREBASE_API_KEY: String = "AIzaSyC3oRh5Mu8IB9HBEn_pMRQXNwVRfXBX8Pw"

    /** Project ID Firebase. */
    const val FIREBASE_PROJECT_ID: String = "mixtune-9f676"

    /**
     * OAuth client ID tipe WEB ("Web client (auto created by Google
     * Service)") dari project yang sama — dipakai requestIdToken
     * Google Sign-In. BUKAN Android client ID.
     */
    const val GOOGLE_WEB_CLIENT_ID: String = "77242001591-9m242rjalpgpmbl4crk723qiota2pn5s.apps.googleusercontent.com"

    /** True hanya bila ketiga nilai sudah diganti dari placeholder. */
    val isConfigured: Boolean
        get() = FIREBASE_API_KEY != "FIREBASE_API_KEY_HERE" &&
            FIREBASE_API_KEY.isNotBlank() &&
            FIREBASE_PROJECT_ID != "FIREBASE_PROJECT_ID_HERE" &&
            FIREBASE_PROJECT_ID.isNotBlank() &&
            GOOGLE_WEB_CLIENT_ID != "GOOGLE_WEB_CLIENT_ID_HERE" &&
            GOOGLE_WEB_CLIENT_ID.isNotBlank()
}
