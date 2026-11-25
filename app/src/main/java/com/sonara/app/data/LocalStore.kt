package com.sonara.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

/**
 * Penyimpanan lokal: SharedPreferences biasa untuk settings /
 * liked / playlist (JSON), dan [SecurePrefs] untuk nilai sensitif
 * yang dienkripsi AES/GCM dengan kunci di Android Keystore.
 * Tidak ada kredensial yang disimpan polos.
 */
class LocalStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sonara", Context.MODE_PRIVATE)
    val secure = SecurePrefs(context)

    /**
     * Callback perubahan koleksi lokal (untuk sinkron cloud):
     * (jenis, id yang berubah, id yang dihapus). SyncManager
     * memasang ini; dibisukan saat SyncManager menerapkan data
     * dari cloud (flag applyingRemote di sisinya).
     */
    var onLibraryChanged: ((LibraryChangeKind, List<String>, List<String>) -> Unit)? = null

    // ---- settings ----
    var darkTheme: Boolean
        get() = prefs.getBoolean("dark_theme", true)
        set(v) = prefs.edit().putBoolean("dark_theme", v).apply()

    // ---- tema (v1.10.26) ----
    // Disimpan sebagai string ("arus"/"senja"/"blok"/"getar",
    // "light"/"dark"); nilai tak dikenal dipetakan ke default saat
    // dibaca (appThemeIdFromStorage/appThemeModeFromStorage). Kunci
    // "dark_theme" lama TIDAK dipakai lagi (tidak pernah dibaca).
    var themeId: String
        get() = prefs.getString("theme_id", "arus") ?: "arus"
        set(v) = prefs.edit().putString("theme_id", v).apply()
    var themeMode: String
        get() = prefs.getString("theme_mode", "light") ?: "light"
        set(v) = prefs.edit().putString("theme_mode", v).apply()

    // ---- audio ----
    var audioQuality: AudioQuality
        get() = try {
            AudioQuality.valueOf(
                prefs.getString("audio_quality", AudioQuality.BEST.name)
                    ?: AudioQuality.BEST.name,
            )
        } catch (_: Exception) {
            AudioQuality.BEST
        }
        set(v) = prefs.edit().putString("audio_quality", v.name).apply()

    /** Fade keluar di akhir lagu, detik (0 = mati). Bukan crossfade dua player. */
    var crossfadeSeconds: Int
        get() = prefs.getInt("crossfade_seconds", 0)
        set(v) = prefs.edit().putInt("crossfade_seconds", v.coerceIn(0, 12)).apply()

    /** Gapless: lagu berikut langsung jalan tanpa jeda/fade. */
    var gapless: Boolean
        get() = prefs.getBoolean("gapless", true)
        set(v) = prefs.edit().putBoolean("gapless", v).apply()

    /** Lewati bagian sponsor/intro/outro (SponsorBlock, v1.0.3; default nyala). */
    var skipSponsoredSegments: Boolean
        get() = prefs.getBoolean("skip_sponsored_segments", true)
        set(v) = prefs.edit().putBoolean("skip_sponsored_segments", v).apply()

    var followSystemTheme: Boolean
        get() = prefs.getBoolean("follow_system_theme", false)
        set(v) = prefs.edit().putBoolean("follow_system_theme", v).apply()

    var lastQueueIds: List<String>
        get() = prefs.getString("last_queue", null)?.let { jsonToStrings(it) } ?: emptyList()
        set(v) = prefs.edit().putString("last_queue", stringsToJson(v)).apply()

    // ---- terakhir diputar (v1.7) ----
    // Lagu yang benar-benar diputar di perangkat ini, terbaru dulu,
    // maks 20. Dipakai seksi "Terakhir diputar" di Koleksi — data
    // nyata, bukan karangan: sebelum ada lagu yang diputar, kosong.
    fun recentTracks(): List<Track> {
        val raw = prefs.getString("recent_tracks", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                runCatching { trackFromJson(arr.getJSONObject(i)) }.getOrNull()
                // v1.10.19: satu id satu baris (data tersimpan lama bisa
                // mengandung id ganda) — key LazyColumn harus unik.
            }.distinctBy { it.id }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addRecentTrack(track: Track) {
        val tracks = recentTracks().toMutableList()
        tracks.removeAll { it.id == track.id }
        tracks.add(0, track)
        val kept = tracks.take(20)
        val arr = JSONArray()
        kept.forEach { arr.put(trackToJson(it)) }
        prefs.edit().putString("recent_tracks", arr.toString()).apply()
        onLibraryChanged?.invoke(LibraryChangeKind.RECENT, listOf(track.id), emptyList())
    }

    /** Ganti seluruh riwayat (dipakai saat menerapkan data dari cloud). */
    fun setRecentTracks(tracks: List<Track>) {
        val kept = tracks.take(20)
        val arr = JSONArray()
        kept.forEach { arr.put(trackToJson(it)) }
        prefs.edit().putString("recent_tracks", arr.toString()).apply()
        onLibraryChanged?.invoke(
            LibraryChangeKind.RECENT,
            kept.map { it.id },
            emptyList(),
        )
    }

    // ---- cache durasi (v1.10.31, lokal saja, TIDAK disinkron) ----
    // videoId -> durationMs yang sudah diketahui lewat lookupVideo
    // (jalur feed tidak membawa durasi). Dipakai TrackDurations supaya
    // kunjungan ulang menampilkan durasi tanpa request ulang. Format:
    // array JSON pasangan [videoId, durationMs], urutan = tertua dulu,
    // maks 500 (tertua dibuang). Hanya nilai > 0 yang disimpan.
    fun durationCache(): Map<String, Long> {
        val raw = prefs.getString("duration_cache_v1", null) ?: return emptyMap()
        return try {
            val arr = JSONArray(raw)
            val out = LinkedHashMap<String, Long>()
            for (i in 0 until arr.length()) {
                val pair = arr.optJSONArray(i) ?: continue
                val id = pair.optString(0)
                val ms = pair.optLong(1, 0)
                if (id.isNotBlank() && ms > 0) out[id] = ms
            }
            out
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveDurationCache(map: Map<String, Long>) {
        val kept = map.entries.filter { it.key.isNotBlank() && it.value > 0 }.takeLast(500)
        val arr = JSONArray()
        kept.forEach { (k, v) ->
            arr.put(JSONArray().apply { put(k); put(v) })
        }
        prefs.edit().putString("duration_cache_v1", arr.toString()).apply()
    }

    // ---- hitungan putar (v1.10.16, lokal saja, TIDAK disinkron) ----
    // "Terakhir diputar" lama tidak bisa dipakai untuk frekuensi:
    // daftarnya dedup (satu lagu muncul sekali) dan tanpa timestamp.
    // Karena itu hitungan dicatat terpisah di sini, HANYA saat lagu
    // benar-benar mulai diputar (dipanggil berdampingan dengan
    // addRecentTrack dari PlayerManager). Sebelum ada pemutaran
    // yang tercatat, daftarnya kosong — UI menampilkan empty state
    // jujur, bukan angka karangan. Format: {"id": {"count": N,
    // "lastPlayedAt": ms, "track": {...}}}.
    fun recordPlay(track: Track) {
        val entries = readPlayLedger().toMutableList()
        val now = System.currentTimeMillis()
        val idx = entries.indexOfFirst { it.track.id == track.id }
        if (idx >= 0) {
            val old = entries[idx]
            entries[idx] = old.copy(count = old.count + 1, lastPlayedAtMs = now, track = track)
        } else {
            entries.add(PlayCount(track, 1, now))
        }
        // Batasi ukuran prefs: simpan 200 teratas (hitungan, lalu terbaru).
        val kept = entries
            .sortedWith(
                compareByDescending<PlayCount> { it.count }
                    .thenByDescending { it.lastPlayedAtMs },
            )
            .take(200)
        writePlayLedger(kept)
    }

    /** Seluruh hitungan putar yang tercatat (belum diurutkan khusus). */
    fun playCounts(): List<PlayCount> = readPlayLedger()

    private fun readPlayLedger(): List<PlayCount> {
        val raw = prefs.getString("play_counts_v1", null) ?: return emptyList()
        return try {
            val obj = JSONObject(raw)
            val out = mutableListOf<PlayCount>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val entry = obj.optJSONObject(keys.next()) ?: continue
                val trackObj = entry.optJSONObject("track") ?: continue
                val track = runCatching { trackFromJson(trackObj) }.getOrNull() ?: continue
                out.add(
                    PlayCount(
                        track = track,
                        count = entry.optInt("count", 0),
                        lastPlayedAtMs = entry.optLong("lastPlayedAt", 0L),
                    ),
                )
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writePlayLedger(entries: List<PlayCount>) {
        val obj = JSONObject()
        for (e in entries) {
            val entry = JSONObject()
            entry.put("count", e.count)
            entry.put("lastPlayedAt", e.lastPlayedAtMs)
            entry.put("track", trackToJson(e.track))
            obj.put(e.track.id, entry)
        }
        prefs.edit().putString("play_counts_v1", obj.toString()).apply()
    }

    // ---- riwayat pencarian (v1.10.16, lokal saja, TIDAK disinkron) ----
    // Hanya query yang benar-benar dieksekusi/diketuk user; query
    // pendek (< 2 karakter) tidak disimpan. Daftar dimanipulasi lewat
    // fungsi murni addRecentSearchQuery (PlayStats.kt) supaya bisa
    // diuji di JVM; di sini hanya persistensi JSON-nya.
    fun recentSearches(): List<String> {
        val raw = prefs.getString("recent_searches_v1", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }.filter { it.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addRecentSearch(query: String) {
        val updated = addRecentSearchQuery(recentSearches(), query)
        val arr = JSONArray()
        updated.forEach { arr.put(it) }
        prefs.edit().putString("recent_searches_v1", arr.toString()).apply()
    }

    fun removeRecentSearch(query: String) {
        val updated = recentSearches().filterNot { it == query }
        val arr = JSONArray()
        updated.forEach { arr.put(it) }
        prefs.edit().putString("recent_searches_v1", arr.toString()).apply()
    }

    fun clearRecentSearches() {
        prefs.edit().remove("recent_searches_v1").apply()
    }

    // ---- liked ----
    fun likedIds(): MutableSet<String> =
        prefs.getStringSet("liked_ids", emptySet())?.toMutableSet() ?: mutableSetOf()

    fun setLiked(id: String, liked: Boolean) {
        val set = likedIds()
        if (liked) set.add(id) else set.remove(id)
        prefs.edit().putStringSet("liked_ids", set).apply()
    }

    fun isLiked(id: String): Boolean = likedIds().contains(id)

    // ---- disliked (detail lagu, tombol Dislike) ----
    // Disimpan persisten supaya status Dislike tidak hilang saat ganti
    // lagu; tidak dipakai untuk menyaring rekomendasi (tidak ada
    // backend rekomendasi) — murni status lokal yang jujur.
    fun dislikedIds(): MutableSet<String> =
        prefs.getStringSet("disliked_ids", emptySet())?.toMutableSet() ?: mutableSetOf()

    fun setDisliked(id: String, disliked: Boolean) {
        val set = dislikedIds()
        if (disliked) set.add(id) else set.remove(id)
        prefs.edit().putStringSet("disliked_ids", set).apply()
    }

    fun isDisliked(id: String): Boolean = dislikedIds().contains(id)

    // ---- liked (track lengkap) ----
    // ID saja tidak cukup: tidak ada lagi sumber track yang bisa
    // di-resolve dari ID. Karena itu lagu yang disukai disimpan
    // LENGKAP (JSON) — "Disukai" tetap jalan untuk lagu
    // YouTube Music / lokal.
    fun likedTracks(): List<Track> {
        val raw = prefs.getString("liked_tracks", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                runCatching { trackFromJson(arr.getJSONObject(i)) }.getOrNull()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Suka / batal suka + simpan (atau hapus) track lengkapnya. */
    fun setLikedTrack(track: Track, liked: Boolean) {
        val set = likedIds()
        if (liked) set.add(track.id) else set.remove(track.id)
        prefs.edit().putStringSet("liked_ids", set).apply()
        val tracks = likedTracks().toMutableList()
        if (liked) {
            if (tracks.none { it.id == track.id }) tracks.add(track)
        } else {
            tracks.removeAll { it.id == track.id }
        }
        val arr = JSONArray()
        tracks.forEach { arr.put(trackToJson(it)) }
        prefs.edit().putString("liked_tracks", arr.toString()).apply()
        val removed = if (liked) emptyList<String>() else listOf(track.id)
        onLibraryChanged?.invoke(LibraryChangeKind.LIKED, listOf(track.id), removed)
    }

    /** Ganti seluruh lagu disukai sekaligus (dipakai saat pull dari cloud). */
    fun replaceLikedTracks(tracks: List<Track>) {
        val set = tracks.map { it.id }.toMutableSet()
        prefs.edit().putStringSet("liked_ids", set).apply()
        val arr = JSONArray()
        tracks.forEach { arr.put(trackToJson(it)) }
        prefs.edit().putString("liked_tracks", arr.toString()).apply()
        onLibraryChanged?.invoke(LibraryChangeKind.LIKED, tracks.map { it.id }, emptyList())
    }

    // ---- disliked (track lengkap, v1.0.3) ----
    // Cermin likedTracks, LOKAL SAJA (tidak disinkron ke cloud):
    // lagu tidak disukai disimpan lengkap supaya daftar "Disliked"
    // di Library tetap bisa ditampilkan + dipulihkan (un-dislike).
    // Terbaru dulu (setDislikedTrack menaruh track baru di depan).
    fun dislikedTracks(): List<Track> {
        val raw = prefs.getString("disliked_tracks", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                runCatching { trackFromJson(arr.getJSONObject(i)) }.getOrNull()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Tidak suka / batal tidak suka + simpan (atau hapus) track lengkapnya. */
    fun setDislikedTrack(track: Track, disliked: Boolean) {
        val set = dislikedIds()
        if (disliked) set.add(track.id) else set.remove(track.id)
        prefs.edit().putStringSet("disliked_ids", set).apply()
        val tracks = dislikedTracks().toMutableList()
        if (disliked) {
            tracks.removeAll { it.id == track.id }
            tracks.add(0, track)
        } else {
            tracks.removeAll { it.id == track.id }
        }
        val arr = JSONArray()
        tracks.forEach { arr.put(trackToJson(it)) }
        prefs.edit().putString("disliked_tracks", arr.toString()).apply()
        // Sengaja TIDAK memicu onLibraryChanged: daftar tidak disukai
        // lokal saja, tidak pernah disinkron ke cloud (SyncManager
        // tidak mengenal koleksi ini). UI membaca ulang dari store.
    }

    companion object {
        fun trackToJson(t: Track): JSONObject {
            val o = JSONObject()
            o.put("id", t.id)
            o.put("title", t.title)
            o.put("artist", t.artist)
            o.put("album", t.album)
            o.put("source", t.source.name)
            o.put("engine", t.engine.name)
            o.put("durationMs", t.durationMs)
            o.put("streamUrl", t.streamUrl ?: "")
            o.put("youtubeVideoId", t.youtubeVideoId ?: "")
            o.put("imageUrl", t.imageUrl ?: "")
            o.put("explicit", t.explicit)
            o.put("artistBrowseId", t.artistBrowseId ?: "")
            o.put("isPodcastEpisode", t.isPodcastEpisode)
            o.put("podcastShowBrowseId", t.podcastShowBrowseId ?: "")
            o.put("podcastShowTitle", t.podcastShowTitle ?: "")
            return o
        }

        fun trackFromJson(o: JSONObject): Track {
            // Data lama dari sumber yang sudah tidak ada dipetakan ke
            // YOUTUBE_MUSIC — track-nya tetap tampil, bukan crash.
            val source = try {
                Source.valueOf(o.optString("source", "YOUTUBE_MUSIC"))
            } catch (_: IllegalArgumentException) {
                Source.YOUTUBE_MUSIC
            }
            val engine = try {
                Engine.valueOf(o.optString("engine", "EXOPLAYER"))
            } catch (_: IllegalArgumentException) {
                Engine.EXOPLAYER
            }
            return Track(
                id = o.optString("id"),
                title = o.optString("title"),
                artist = o.optString("artist"),
                album = o.optString("album"),
                source = source,
                engine = engine,
                durationMs = o.optLong("durationMs", 0),
                streamUrl = o.optString("streamUrl").takeIf { it.isNotEmpty() },
                youtubeVideoId = o.optString("youtubeVideoId").takeIf { it.isNotEmpty() },
                imageUrl = o.optString("imageUrl").takeIf { it.isNotEmpty() },
                explicit = o.optBoolean("explicit", false),
                artistBrowseId = o.optString("artistBrowseId").takeIf { it.isNotEmpty() },
                isPodcastEpisode = o.optBoolean("isPodcastEpisode", false),
                podcastShowBrowseId = o.optString("podcastShowBrowseId").takeIf { it.isNotEmpty() },
                podcastShowTitle = o.optString("podcastShowTitle").takeIf { it.isNotEmpty() },
            )
        }
    }

    // ---- playlist lokal (disimpan sebagai JSON array) ----
    fun playlists(): List<Playlist> {
        val raw = prefs.getString("playlists", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Playlist(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    trackIds = jsonToStrings(o.optString("tracks", "[]")),
                    createdAt = o.optLong("createdAt", 0L),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun savePlaylists(list: List<Playlist>) {
        val old = playlists()
        val arr = JSONArray()
        for (p in list) {
            val o = JSONObject()
            o.put("id", p.id)
            o.put("name", p.name)
            o.put("tracks", stringsToJson(p.trackIds))
            o.put("createdAt", p.createdAt)
            arr.put(o)
        }
        prefs.edit().putString("playlists", arr.toString()).apply()
        // Diff untuk sinkron: id yang isinya berubah + id yang dihapus.
        val oldById = old.associateBy { it.id }
        val newById = list.associateBy { it.id }
        val changed = list.filter { oldById[it.id] != it }.map { it.id }
        val removed = old.filter { it.id !in newById }.map { it.id }
        if (changed.isNotEmpty() || removed.isNotEmpty()) {
            onLibraryChanged?.invoke(LibraryChangeKind.PLAYLIST, changed, removed)
        }
    }

    // ---- cache track yang dikenal (snapshot dari cloud) ----
    // Playlist dari perangkat lain menyimpan snapshot judul/artis;
    // cache ini membuat lagu-lagunya tampil + bisa diputar di
    // perangkat ini walau belum pernah diputar/disukai di sini.
    fun knownTracks(): List<Track> {
        val raw = prefs.getString("known_tracks", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                runCatching { trackFromJson(arr.getJSONObject(i)) }.getOrNull()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun cacheKnownTracks(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val merged = LinkedHashMap<String, Track>()
        for (t in tracks) merged[t.id] = t
        for (t in knownTracks()) merged.putIfAbsent(t.id, t)
        val kept = merged.values.take(500)
        val arr = JSONArray()
        kept.forEach { arr.put(trackToJson(it)) }
        prefs.edit().putString("known_tracks", arr.toString()).apply()
    }

    // ---- buku sinkron cloud (meta, tombstone, watermark) ----
    // updatedAt per dokumen: {"liked": {docId: ms}, "playlists": {...}, "history": {...}}.
    fun syncMeta(): JSONObject {
        return try {
            JSONObject(prefs.getString("sync_meta_v1", null) ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
    }

    fun saveSyncMeta(meta: JSONObject) {
        prefs.edit().putString("sync_meta_v1", meta.toString()).apply()
    }

    /** Tombstone hapus eksplisit: {"liked": {docId: deletedAtMs}, ...}. */
    fun syncTombstones(): JSONObject {
        return try {
            JSONObject(prefs.getString("sync_tombstones_v1", null) ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
    }

    fun saveSyncTombstones(tomb: JSONObject) {
        prefs.edit().putString("sync_tombstones_v1", tomb.toString()).apply()
    }

    /** Watermark sinkron terakhir yang sukses (0 = belum pernah). */
    fun lastSyncAtMs(): Long = prefs.getLong("sync_last_at_ms", 0L)

    fun setLastSyncAtMs(v: Long) {
        prefs.edit().putLong("sync_last_at_ms", v).apply()
    }

    /** UID akun yang datanya sedang disinkronkan (null = belum ada). */
    fun syncAccountUid(): String? = prefs.getString("sync_account_uid", null)

    fun setSyncAccountUid(uid: String?) {
        prefs.edit().putString("sync_account_uid", uid).apply()
    }

    private fun stringsToJson(list: List<String>): String {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr.toString()
    }

    private fun jsonToStrings(raw: String): List<String> {
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ---- artis yang diikuti (v1.10.21, LOKAL saja — tidak disinkron) ----
    fun followedArtists(): List<FollowedArtist> {
        val raw = prefs.getString("followed_artists_v1", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                runCatching {
                    val o = arr.getJSONObject(i)
                    FollowedArtist(
                        key = o.getString("key"),
                        name = o.getString("name"),
                        browseId = o.optString("browseId").takeIf { it.isNotEmpty() },
                        followedAtMs = o.optLong("followedAt", 0L),
                    )
                }.getOrNull()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun isFollowing(key: String): Boolean =
        followedArtists().any { it.key == key }

    /** Follow/unfollow satu artis; unfollow juga menghapus cache rilisannya. */
    fun setFollowedArtist(artist: FollowedArtist, followed: Boolean) {
        val current = followedArtists().toMutableList()
        current.removeAll { it.key == artist.key }
        if (followed) current.add(artist)
        val arr = JSONArray()
        for (a in current) {
            val o = JSONObject()
            o.put("key", a.key)
            o.put("name", a.name)
            o.put("browseId", a.browseId ?: "")
            o.put("followedAt", a.followedAtMs)
            arr.put(o)
        }
        prefs.edit().putString("followed_artists_v1", arr.toString()).apply()
        if (!followed) removeFollowedReleaseCache(artist.key)
    }

    // ---- cache rilisan artis diikuti (v1.10.21) ----
    /** {kunci artis: (fetchedAtMs, daftar track snapshot)}. */
    fun followedReleaseCache(): Map<String, Pair<Long, List<Track>>> {
        val raw = prefs.getString("followed_releases_v1", null) ?: return emptyMap()
        return try {
            val root = JSONObject(raw)
            val out = LinkedHashMap<String, Pair<Long, List<Track>>>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val o = root.optJSONObject(k) ?: continue
                val tracks = try {
                    val arr = o.optJSONArray("tracks") ?: JSONArray()
                    (0 until arr.length()).mapNotNull { i ->
                        runCatching { trackFromJson(arr.getJSONObject(i)) }.getOrNull()
                    }
                } catch (_: Exception) {
                    emptyList()
                }
                out[k] = o.optLong("fetchedAt", 0L) to tracks
            }
            out
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveFollowedReleaseCache(cache: Map<String, Pair<Long, List<Track>>>) {
        val root = JSONObject()
        for ((k, v) in cache) {
            val o = JSONObject()
            o.put("fetchedAt", v.first)
            val arr = JSONArray()
            v.second.take(FOLLOWED_SHELF_MAX_ITEMS).forEach { arr.put(trackToJson(it)) }
            o.put("tracks", arr)
            root.put(k, o)
        }
        prefs.edit().putString("followed_releases_v1", root.toString()).apply()
    }

    fun removeFollowedReleaseCache(key: String) {
        val cache = followedReleaseCache().toMutableMap()
        if (cache.remove(key) != null) saveFollowedReleaseCache(cache)
    }

    // ---- posisi lanjut konten panjang (v1.10.21, lokal saja) ----
    private fun resumePositionsRaw(): JSONObject {
        return try {
            JSONObject(prefs.getString("resume_positions_v1", null) ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
    }

    fun resumePosition(trackId: String): Long =
        resumePositionsRaw().optLong(trackId, 0L)

    /** Simpan posisi; cap 200 entri (yang paling lama dibuang). */
    fun saveResumePosition(trackId: String, ms: Long) {
        val obj = resumePositionsRaw()
        obj.remove(trackId)
        obj.put(trackId, ms)
        while (obj.length() > 200) {
            val eldest = obj.keys().next()
            obj.remove(eldest)
        }
        prefs.edit().putString("resume_positions_v1", obj.toString()).apply()
    }

    fun clearResumePosition(trackId: String) {
        val obj = resumePositionsRaw()
        if (obj.has(trackId)) {
            obj.remove(trackId)
            prefs.edit().putString("resume_positions_v1", obj.toString()).apply()
        }
        clearResumeSnapshot(trackId)
    }

    // ---- cuplikan posisi-lanjut untuk rak "Keep listening" (v1.0.5, lokal saja) ----
    // Peta {trackId: snapshot} — ditulis bersamaan dengan posisi (lihat
    // saveResumeSnapshot), supaya rak bisa dirender tanpa menebak metadata.
    private fun resumeSnapshotsRaw(): JSONObject {
        return try {
            JSONObject(prefs.getString("resume_snapshots_v1", null) ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
    }

    fun resumeSnapshots(): List<ResumeSnapshot> {
        val obj = resumeSnapshotsRaw()
        val out = ArrayList<ResumeSnapshot>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val o = obj.optJSONObject(k) ?: continue
            out += ResumeSnapshot(
                trackId = k,
                videoId = o.optString("videoId").takeIf { it.isNotEmpty() },
                title = o.optString("title"),
                artist = o.optString("artist"),
                showTitle = o.optString("showTitle").takeIf { it.isNotEmpty() },
                imageUrl = o.optString("imageUrl").takeIf { it.isNotEmpty() },
                durationMs = o.optLong("durationMs", 0L),
                savedAtMs = o.optLong("savedAt", 0L),
                positionMs = o.optLong("positionMs", 0L),
                showBrowseId = o.optString("showBrowseId"),
            )
        }
        return out
    }

    /** Simpan posisi + cuplikan; cap 20 entri (yang terlama dibuang). */
    fun saveResumeSnapshot(track: Track, positionMs: Long, savedAtMs: Long) {
        saveResumePositionOnly(track.id, positionMs)
        val obj = resumeSnapshotsRaw()
        obj.remove(track.id)
        val o = JSONObject()
        o.put("videoId", track.youtubeVideoId ?: "")
        o.put("title", track.title)
        o.put("artist", track.artist)
        o.put("showTitle", track.podcastShowTitle ?: "")
        o.put("showBrowseId", track.podcastShowBrowseId ?: "")
        o.put("imageUrl", track.imageUrl ?: "")
        o.put("durationMs", track.durationMs)
        o.put("savedAt", savedAtMs)
        o.put("positionMs", positionMs)
        obj.put(track.id, o)
        while (obj.length() > KeepListeningPolicy.MAX_SNAPSHOTS) {
            val eldest = obj.keys().next()
            obj.remove(eldest)
        }
        prefs.edit().putString("resume_snapshots_v1", obj.toString()).apply()
    }

    private fun saveResumePositionOnly(trackId: String, ms: Long) {
        val obj = resumePositionsRaw()
        obj.remove(trackId)
        obj.put(trackId, ms)
        while (obj.length() > 200) {
            val eldest = obj.keys().next()
            obj.remove(eldest)
        }
        prefs.edit().putString("resume_positions_v1", obj.toString()).apply()
    }

    fun clearResumeSnapshot(trackId: String) {
        val obj = resumeSnapshotsRaw()
        if (obj.has(trackId)) {
            obj.remove(trackId)
            prefs.edit().putString("resume_snapshots_v1", obj.toString()).apply()
        }
    }

    // ---- acara podcast yang diikuti (v1.0.5, LOKAL saja — tidak disinkron) ----
    fun followedShows(): List<FollowedShow> {
        val raw = prefs.getString("followed_shows_v1", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                runCatching {
                    val o = arr.getJSONObject(i)
                    FollowedShow(
                        key = o.getString("key"),
                        title = o.getString("title"),
                        browseId = o.getString("browseId"),
                        imageUrl = o.optString("imageUrl").takeIf { it.isNotEmpty() },
                        host = o.optString("host", ""),
                        followedAtMs = o.optLong("followedAt", 0L),
                    )
                }.getOrNull()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun isFollowingShow(browseId: String): Boolean =
        followedShows().any { it.browseId == browseId }

    /** Follow/unfollow satu acara; kunci = browseId MPSP…. */
    fun setFollowedShow(show: FollowedShow, followed: Boolean) {
        val current = followedShows().toMutableList()
        current.removeAll { it.key == show.key }
        if (followed) current.add(show)
        val arr = JSONArray()
        for (s in current) {
            val o = JSONObject()
            o.put("key", s.key)
            o.put("title", s.title)
            o.put("browseId", s.browseId)
            o.put("imageUrl", s.imageUrl ?: "")
            o.put("host", s.host)
            o.put("followedAt", s.followedAtMs)
            arr.put(o)
        }
        prefs.edit().putString("followed_shows_v1", arr.toString()).apply()
    }

    // ---- episode yang ditandai played (v1.0.5, lokal saja) ----
    fun playedEpisodeIds(): Set<String> {
        val raw = prefs.getString("played_episodes_v1", null) ?: return emptySet()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                runCatching { arr.getJSONObject(i).getString("id") }.getOrNull()
            }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun isEpisodePlayed(trackId: String): Boolean = trackId in playedEpisodeIds()

    /** Tandai played / batalkan. Mark juga menghapus posisi-lanjut episode itu. */
    fun setPlayedEpisode(trackId: String, played: Boolean) {
        val current = playedEpisodeIds().toMutableSet()
        if (played) current.add(trackId) else current.remove(trackId)
        val arr = JSONArray()
        for (id in current) {
            val o = JSONObject()
            o.put("id", id)
            arr.put(o)
        }
        prefs.edit().putString("played_episodes_v1", arr.toString()).apply()
        if (played) clearResumePosition(trackId)
    }

    // ---- kecepatan putar (v1.0.5, lokal saja) ----
    /** Rate default untuk lagu baru (persist, 1.0× bila belum diatur). */
    var defaultPlaybackRate: Float
        get() = prefs.getFloat("default_playback_rate", 1.0f)
            .takeIf { it in 0.5f..2.0f } ?: 1.0f
        set(v) = prefs.edit()
            .putFloat("default_playback_rate", v.coerceIn(0.5f, 2.0f)).apply()

    /** Rate per-episode tersimpan (hanya konten panjang); null = belum diatur. */
    fun episodeSpeed(trackId: String): Float? {
        return try {
            val obj = JSONObject(prefs.getString("episode_speeds_v1", null) ?: "{}")
            if (!obj.has(trackId)) null
            else obj.optDouble(trackId, Double.NaN).toFloat()
                .takeIf { it in 0.5f..2.0f }
        } catch (_: Exception) {
            null
        }
    }

    /** Simpan rate per-episode; cap 100 lagu (yang paling lama dibuang). */
    fun saveEpisodeSpeed(trackId: String, rate: Float) {
        val obj = try {
            JSONObject(prefs.getString("episode_speeds_v1", null) ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        obj.remove(trackId)
        obj.put(trackId, rate.coerceIn(0.5f, 2.0f).toDouble())
        while (obj.length() > 100) {
            val eldest = obj.keys().next()
            obj.remove(eldest)
        }
        prefs.edit().putString("episode_speeds_v1", obj.toString()).apply()
    }

    // ---- lirik: edit lokal + offset timing + preferensi sumber (v1.0.4, lokal saja) ----
    private fun lyricsEditsRaw(): JSONObject {
        return try {
            JSONObject(prefs.getString("lyrics_edits_v1", null) ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
    }

    /** Override edit lirik untuk satu lagu (null = belum pernah diedit). */
    fun lyricsEdit(trackId: String): LyricsEdit? {
        val raw = lyricsEditsRaw().optString(trackId)
        if (raw.isBlank()) return null
        return LyricsEditCodec.editFromJson(raw)
    }

    /** Simpan override edit; cap 100 lagu (yang paling lama dibuang, pola resume positions). */
    fun saveLyricsEdit(trackId: String, edit: LyricsEdit) {
        val obj = lyricsEditsRaw()
        obj.remove(trackId)
        obj.put(trackId, LyricsEditCodec.editToJson(edit))
        while (obj.length() > 100) {
            val eldest = obj.keys().next()
            obj.remove(eldest)
        }
        prefs.edit().putString("lyrics_edits_v1", obj.toString()).apply()
    }

    fun clearLyricsEdit(trackId: String) {
        val obj = lyricsEditsRaw()
        if (obj.has(trackId)) {
            obj.remove(trackId)
            prefs.edit().putString("lyrics_edits_v1", obj.toString()).apply()
        }
    }

    /** Offset timing per-lagu (ms, 0 = tidak ada). */
    fun lyricsOffsetMs(trackId: String): Long =
        LyricsOffsetsCodec.fromJson(prefs.getString("lyrics_offsets_v1", null))[trackId] ?: 0L

    /** Simpan offset per-lagu; 0 = hapus entrinya. */
    fun setLyricsOffsetMs(trackId: String, ms: Long) {
        val map = LyricsOffsetsCodec.fromJson(prefs.getString("lyrics_offsets_v1", null)).toMutableMap()
        val clamped = LyricsOffsetPolicy.clampOffset(ms)
        if (clamped == 0L) map.remove(trackId) else map[trackId] = clamped
        prefs.edit().putString("lyrics_offsets_v1", LyricsOffsetsCodec.toJson(map)).apply()
    }

    /** Offset timing global untuk semua lagu (ms, 0 = tidak ada). */
    var globalLyricsOffsetMs: Long
        get() = LyricsOffsetPolicy.clampOffset(prefs.getLong("lyrics_offset_global_ms", 0L))
        set(v) = prefs.edit().putLong(
            "lyrics_offset_global_ms", LyricsOffsetPolicy.clampOffset(v),
        ).apply()

    /** Preferensi sumber lirik: nama LyricsSource (AUTO default). */
    var lyricsSourcePref: String
        get() = prefs.getString("lyrics_source_pref", "AUTO") ?: "AUTO"
        set(v) = prefs.edit().putString("lyrics_source_pref", v).apply()

    // ---- organisasi Koleksi: pin/folder/tag/auto-download (v1.0.6, lokal saja) ----
    // TERPISAH dari model Playlist yang disinkron (pola cover v1.10.41):
    // mengubah Playlist akan mengubah diff sinkron. State di sini
    // tidak pernah disinkron ke cloud.
    fun playlistOrg(): LibraryOrg =
        LibraryOrgPolicy.orgFromJsonString(prefs.getString("playlist_org_v1", null))

    fun savePlaylistOrg(org: LibraryOrg) {
        prefs.edit().putString("playlist_org_v1", LibraryOrgPolicy.orgToJsonString(org)).apply()
    }

    /** Hapus state org milik playlist yang sudah tidak ada (dipanggil refresh Koleksi). */
    fun prunePlaylistOrg(liveIds: Collection<String>) {
        val org = playlistOrg()
        val live = liveIds.toSet()
        val pruned = org.copy(
            pinnedIds = org.pinnedIds.filter { it in live },
            playlistFolder = org.playlistFolder.filterKeys { it in live },
            tags = org.tags.filterKeys { it in live },
            autoDownloadIds = org.autoDownloadIds.filter { it in live }.toSet(),
        )
        if (pruned != org) savePlaylistOrg(pruned)
    }

    // ---- peristiwa putar untuk statistik "Your Year" (v1.0.6, lokal saja) ----
    // play_counts_v1 tidak punya timestamp per putar, jadi peristiwa
    // dicatat terpisah mulai versi ini. Statistik tahun hanya memakai
    // peristiwa tercatat (label jujur di UI).
    fun playEvents(): List<PlayEvent> =
        PlayEvents.eventsFromJson(prefs.getString("play_events_v1", null))

    fun recordPlayEvent(event: PlayEvent) {
        val updated = PlayEvents.recordPlayEvent(playEvents(), event)
        prefs.edit().putString(
            "play_events_v1",
            PlayEvents.eventsToJson(updated).toString(),
        ).apply()
    }

    // ---- tugas unduhan (v1.0.6, lokal saja, tidak disinkron) ----
    fun downloadTasks(): List<DownloadTasks.DownloadTask> =
        DownloadTasks.tasksFromJson(prefs.getString("download_tasks_v1", null))

    fun saveDownloadTasks(tasks: List<DownloadTasks.DownloadTask>) {
        prefs.edit().putString(
            "download_tasks_v1",
            DownloadTasks.tasksToJson(tasks).toString(),
        ).apply()
    }

    // ---- file lokal: override tag + folder SAF + sumber duplikat (v1.0.7, lokal saja) ----
    // Override tag hanya mengubah TAMPILAN; file aslinya tidak pernah
    // ditulis. Folder SAF = URI tree + izin persistable (diambil saat
    // memilih di pemilih dokumen sistem).
    fun localTagOverrides(): Map<String, LocalTagOverride> =
        LocalFilesPolicy.overridesFromJson(prefs.getString("local_tag_overrides_v1", null))

    fun saveLocalTagOverrides(overrides: Map<String, LocalTagOverride>) {
        prefs.edit().putString(
            "local_tag_overrides_v1",
            LocalFilesPolicy.overridesToJson(overrides),
        ).apply()
    }

    fun localPreferredSources(): Map<String, String> =
        LocalFilesPolicy.preferredSourcesFromJson(prefs.getString("local_preferred_sources_v1", null))

    fun saveLocalPreferredSources(preferred: Map<String, String>) {
        prefs.edit().putString(
            "local_preferred_sources_v1",
            LocalFilesPolicy.preferredSourcesToJson(preferred),
        ).apply()
    }

    fun localFolderUris(): List<String> {
        val raw = prefs.getString("local_folders_v1", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val uris = (0 until arr.length()).map { i -> arr.optString(i, "") }
            LocalFilesPolicy.normalizeFolderUris(uris)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveLocalFolderUris(uris: List<String>) {
        val arr = JSONArray()
        for (uri in LocalFilesPolicy.normalizeFolderUris(uris)) arr.put(uri)
        prefs.edit().putString("local_folders_v1", arr.toString()).apply()
    }

    /** Folder sebagai entri (nama diturunkan dari URI tree). */
    fun localFolders(): List<LocalFolderEntry> =
        localFolderUris().map { uri ->
            LocalFolderEntry(uriString = uri, displayName = LocalFilesPolicy.folderDisplayName(uri))
        }

    fun addLocalFolder(entry: LocalFolderEntry) {
        saveLocalFolderUris(localFolderUris() + entry.uriString)
    }

    fun removeLocalFolder(uriString: String) {
        saveLocalFolderUris(localFolderUris().filter { it != uriString })
    }

    fun saveLocalTagOverride(trackId: String, override: LocalTagOverride) {
        val updated = localTagOverrides().toMutableMap()
        updated[trackId] = override
        saveLocalTagOverrides(updated)
    }

    fun clearLocalTagOverride(trackId: String) {
        val updated = localTagOverrides().toMutableMap()
        updated.remove(trackId)
        saveLocalTagOverrides(updated)
    }

    fun setLocalPreferredSource(groupKey: String, trackId: String) {
        val updated = localPreferredSources().toMutableMap()
        updated[groupKey] = trackId
        saveLocalPreferredSources(updated)
    }

    /** Sembunyikan baris file yang hilang dari seksi musik lokal. */
    var hideMissingLocal: Boolean
        get() = prefs.getBoolean("hide_missing_local", false)
        set(v) = prefs.edit().putBoolean("hide_missing_local", v).apply()

    // ---- layout Beranda + Koleksi (v1.0.7, lokal saja) ----
    var homeLayout: String
        get() = prefs.getString("home_layout", "editorial") ?: "editorial"
        set(v) = prefs.edit().putString("home_layout", v).apply()

    // ---- tema kustom (v1.0.7, lokal saja; melapis tabel token) ----
    fun customThemeOverride(): CustomThemeOverride? =
        CustomThemePolicy.fromJson(prefs.getString("custom_theme_v1", null))

    fun saveCustomTheme(override: CustomThemeOverride) {
        prefs.edit().putString("custom_theme_v1", CustomThemePolicy.toJson(override)).apply()
    }

    fun clearCustomTheme() {
        prefs.edit().remove("custom_theme_v1").apply()
    }
}

/**
 * Token store terenkripsi: AES/GCM dengan kunci yang dibuat +
 * disimpan di Android Keystore (tidak pernah keluar dari perangkat).
 * Setara dengan yang dilakukan EncryptedSharedPreferences di
 * balik layar — ditulis langsung supaya tidak butuh library Tink
 * (tink-android tidak tersedia di cache / Maven).
 */
class SecurePrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sonara_secure", Context.MODE_PRIVATE)
    private val keyAlias = "sonara_secure_key"

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = ks.getKey(keyAlias, null) as? SecretKey
        if (existing != null) return existing
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        gen.init(spec)
        return gen.generateKey()
    }

    fun put(key: String, value: String?) {
        if (value == null) {
            prefs.edit().remove(key).remove("$key:iv").apply()
            return
        }
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cipher.iv
            val ct = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            prefs.edit()
                .putString(key, Base64.encodeToString(ct, Base64.NO_WRAP))
                .putString("$key:iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
        } catch (e: Exception) {
            // Keystore gagal (mis. perangkat tanpa Keystore): jangan
            // simpan polos — nilai dianggap tidak tersimpan.
            throw IllegalStateException("Keystore unavailable: ${e.message}", e)
        }
    }

    fun get(key: String): String? {
        val ctB64 = prefs.getString(key, null) ?: return null
        val ivB64 = prefs.getString("$key:iv", null) ?: return null
        return try {
            val ct = Base64.decode(ctB64, Base64.NO_WRAP)
            val iv = Base64.decode(ivB64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ct), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
