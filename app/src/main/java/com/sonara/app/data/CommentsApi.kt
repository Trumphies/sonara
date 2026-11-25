package com.sonara.app.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Komentar YouTube Music — read-only (v1.10.19).
 *
 * Komentar diambil dari endpoint `next` dengan klien WEB (bukan
 * WEB_REMIX — `next` WEB_REMIX tidak mengembalikan panel komentar),
 * signed-out, tanpa cookies/visitorData. Terverifikasi live dari VM
 * 22 Nov 2026: 20 komentar/halaman + pagination + balasan + deteksi
 * komentar-nonaktif (tidak ada panel komentar sama sekali).
 *
 * Bentuk respons KRITIS: `commentThreadRenderer` hampir tidak membawa
 * data tampil — hanya kunci. Semua data tampil ada di
 * `frameworkUpdates.entityBatchUpdate.mutations[].payload` yang
 * diindeks kunci tersebut (commentEntityPayload /
 * commentSurfaceEntityPayload / engagementToolbarStateEntityPayload).
 * Bentuk entity-payload ini AKAN berubah suatu saat: parser di sini
 * defensif — semua field opsional, thread yang malformed di-skip,
 * tidak pernah crash.
 *
 * TIDAK PERNAH ditampilkan: total count ("X comments" — data itu
 * tidak disediakan respons), display name tebakan (hanya @handle),
 * angka like yang diperluas (hanya string singkatan verbatim seperti
 * "10K"), badge pinned yang tidak terpetakan. Posting/like/balas
 * butuh login → DI LUAR SCOPE; panel ini read-only, tanpa composer.
 *
 * Fungsi parse di sini MURNI (menerima JSONObject) supaya bisa diuji
 * dari fixture JSON nyata tanpa jaringan. Fungsi fetch bersifat
 * blocking — pemanggil WAJIB memanggilnya dari Dispatchers.IO,
 * seperti pemanggilan YoutubeMusicApi lainnya.
 */
object CommentsApi {

    /**
     * ViewModel komentar dari satu item. Dua bentuk yang ditemui di
     * respons nyata: ganda di thread renderer
     * (`commentViewModel.commentViewModel`) dan TUNGGAL di respons
     * balasan (`commentViewModel` langsung berisi commentKey).
     */
    private fun viewModelOf(item: JSONObject): JSONObject? {
        val thread = item.optJSONObject("commentThreadRenderer")
        val holder = thread?.optJSONObject("commentViewModel")
            ?: item.optJSONObject("commentViewModel")
            ?: return null
        holder.optJSONObject("commentViewModel")?.let { return it }
        return holder.takeIf { it.has("commentKey") }
    }

    /** Satu komentar, apa adanya dari API. */
    data class Comment(
        val commentId: String,
        /** @handle verbatim (authorButtonA11y) — TIDAK ADA display name terpisah. */
        val authorHandle: String,
        val avatarUrl: String?,
        val channelId: String?,
        /** Waktu verbatim dari API, mis. "1 year ago (edited)". */
        val publishedTimeText: String,
        /** Teks polos verbatim (content.content). */
        val text: String,
        /** String singkatan verbatim, mis. "10K"; null bila tidak ada. */
        val likeCountText: String?,
        /** String verbatim, mis. "277"; null bila tidak ada. */
        val replyCountText: String?,
        val hearted: Boolean,
        /** Teks verbatim "Pinned by @…" dari API; null bila tidak di-pin. */
        val pinnedText: String?,
        /** Token untuk mengambil balasan thread ini; null bila tidak ada. */
        val replyContinuationToken: String?,
    )

    data class CommentsPage(
        val comments: List<Comment>,
        val nextPageToken: String?,
        /** True = video ini komentarnya nonaktif (tidak ada panel komentar). */
        val commentsOff: Boolean,
    )

    // ---------- request ----------

    /**
     * Context klien WEB — bentuk PERSIS seperti probe yang terverifikasi
     * live (clientName WEB, versi 2.20240111.09.00, hl en, gl US).
     * Sengaja tidak memakai clientContext() jalur stream: konteks itu
     * menambah userAgent/timeZone/utcOffsetMinutes yang tidak ada di
     * probe, dan bentuk probe-lah yang terbukti bekerja untuk /next.
     */
    private fun webContext(): JSONObject =
        JSONObject().apply {
            put(
                "client",
                JSONObject().apply {
                    put("clientName", "WEB")
                    put("clientVersion", "2.20240111.09.00")
                    put("hl", "en")
                    put("gl", "US")
                },
            )
        }

    private fun nextBody(videoId: String): JSONObject =
        JSONObject().apply {
            put("context", webContext())
            put("videoId", videoId)
        }

    private fun continuationBody(token: String): JSONObject =
        JSONObject().apply {
            put("context", webContext())
            put("continuation", token)
        }

    /**
     * Ambil token halaman pertama dari panel komentar di respons
     * /next awal. Null bila tidak ada panel komentar (komentar
     * nonaktif) atau panelnya tidak membawa token.
     */
    fun commentsPanelToken(nextRoot: JSONObject): String? {
        val panels = nextRoot.optJSONArray("engagementPanels") ?: return null
        for (i in 0 until panels.length()) {
            val panel = panels.optJSONObject(i)
                ?.optJSONObject("engagementPanelSectionListRenderer")
                ?: continue
            if (panel.optString("panelIdentifier") !=
                "engagement-panel-comments-section"
            ) {
                continue
            }
            // Token pertama di bawah continuationItemRenderer; bila
            // tidak ada, token pertama mana pun di dalam panel.
            findTokenUnderContinuationItem(panel)?.let { return it }
            return findTokenAnywhere(panel)
        }
        return null
    }

    private fun findTokenUnderContinuationItem(node: Any?): String? {
        when (node) {
            is JSONObject -> {
                if (node.has("continuationItemRenderer")) {
                    findTokenAnywhere(node.optJSONObject("continuationItemRenderer"))
                        ?.let { return it }
                }
                val keys = node.keys()
                while (keys.hasNext()) {
                    findTokenUnderContinuationItem(node.get(keys.next()))
                        ?.let { return it }
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    findTokenUnderContinuationItem(node.get(i))?.let { return it }
                }
            }
        }
        return null
    }

    private fun findTokenAnywhere(node: Any?): String? {
        when (node) {
            is JSONObject -> {
                val cmd = node.optJSONObject("continuationEndpoint")
                    ?.optJSONObject("continuationCommand")
                val token = cmd?.optString("token", null)?.takeIf { it.isNotBlank() }
                if (token != null) return token
                val keys = node.keys()
                while (keys.hasNext()) {
                    findTokenAnywhere(node.get(keys.next()))?.let { return it }
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    findTokenAnywhere(node.get(i))?.let { return it }
                }
            }
        }
        return null
    }

    // ---------- fetch (blocking; panggil dari Dispatchers.IO) ----------

    /**
     * Halaman pertama komentar sebuah video. Komentar nonaktif →
     * CommentsPage kosong dengan commentsOff = true (bukan error).
     * Satu retry untuk kegagalan sesaat, seperti searchRaw().
     */
    fun fetchComments(api: YoutubeMusicApi, videoId: String): Result<CommentsPage> {
        val first = fetchCommentsOnce(api, videoId)
        if (first.isSuccess) return first
        return fetchCommentsOnce(api, videoId)
    }

    private fun fetchCommentsOnce(
        api: YoutubeMusicApi,
        videoId: String,
    ): Result<CommentsPage> {
        val nextRoot = api.nextRaw(nextBody(videoId)).getOrElse { return Result.failure(it) }
        val token = commentsPanelToken(nextRoot)
            ?: return Result.success(CommentsPage(emptyList(), null, commentsOff = true))
        val pageRoot = api.nextRaw(continuationBody(token)).getOrElse { return Result.failure(it) }
        return runCatching { parseCommentsPage(pageRoot) }
            .map { it.copy(commentsOff = false) }
    }

    /** Halaman berikutnya (token dari [CommentsPage.nextPageToken]). */
    fun fetchMore(api: YoutubeMusicApi, continuationToken: String): Result<CommentsPage> {
        val root = api.nextRaw(continuationBody(continuationToken))
            .getOrElse { return Result.failure(it) }
        return runCatching { parseCommentsPage(root) }
    }

    /** Balasan satu thread — jalur parse SAMA (item commentViewModel langsung). */
    fun fetchReplies(api: YoutubeMusicApi, threadToken: String): Result<CommentsPage> {
        val root = api.nextRaw(continuationBody(threadToken))
            .getOrElse { return Result.failure(it) }
        return runCatching { parseCommentsPage(root) }
    }

    // ---------- parse murni ----------

    /** Item continuationItems dari KEDUA perintah (reload + append). */
    private fun continuationItems(root: JSONObject): List<JSONObject> {
        val out = ArrayList<JSONObject>()
        val endpoints = root.optJSONArray("onResponseReceivedEndpoints") ?: return out
        for (i in 0 until endpoints.length()) {
            val ep = endpoints.optJSONObject(i) ?: continue
            for (key in arrayOf("reloadContinuationItemsCommand", "appendContinuationItemsAction")) {
                val items = ep.optJSONObject(key)?.optJSONArray("continuationItems")
                    ?: continue
                for (j in 0 until items.length()) {
                    items.optJSONObject(j)?.let { out.add(it) }
                }
            }
        }
        return out
    }

    /** Gabungkan runs[].text; null bila kosong. */
    private fun runsText(textObj: JSONObject?): String? {
        if (textObj == null) return null
        val runs = textObj.optJSONArray("runs")
            ?: return textObj.optString("simpleText", null)?.takeIf { it.isNotBlank() }
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            sb.append(runs.optJSONObject(i)?.optString("text", "") ?: "")
        }
        return sb.toString().takeIf { it.isNotBlank() }
    }

    /** Teks komentar: content.content polos; fallback gabungan runs. */
    private fun commentText(contentObj: JSONObject?): String? {
        if (contentObj == null) return null
        contentObj.optString("content", null)?.takeIf { it.isNotBlank() }?.let { return it }
        return runsText(contentObj)
    }

    /**
     * Parse satu respons halaman komentar (halaman 1, halaman
     * berikutnya, atau balasan). Thread yang malformed (tanpa entity,
     * tanpa @handle, tanpa teks) di-skip — tidak pernah crash.
     */
    fun parseCommentsPage(root: JSONObject): CommentsPage {
        // --- indeks entity ---
        val entities = HashMap<String, JSONObject>()
        val surfaces = HashMap<String, JSONObject>()
        val toolbarStates = HashMap<String, JSONObject>()
        val mutations = root.optJSONObject("frameworkUpdates")
            ?.optJSONObject("entityBatchUpdate")
            ?.optJSONArray("mutations")
        if (mutations != null) {
            for (i in 0 until mutations.length()) {
                val payload = mutations.optJSONObject(i)?.optJSONObject("payload")
                    ?: continue
                payload.optJSONObject("commentEntityPayload")?.let { entity ->
                    entity.optString("key", null)?.takeIf { it.isNotBlank() }?.let { key ->
                        entities[key] = entity
                    }
                }
                payload.optJSONObject("commentSurfaceEntityPayload")?.let { surface ->
                    surface.optString("key", null)?.takeIf { it.isNotBlank() }?.let { key ->
                        surfaces[key] = surface
                    }
                }
                payload.optJSONObject("engagementToolbarStateEntityPayload")?.let { state ->
                    state.optString("key", null)?.takeIf { it.isNotBlank() }?.let { key ->
                        toolbarStates[key] = state
                    }
                }
            }
        }

        val comments = ArrayList<Comment>()
        var nextPageToken: String? = null
        val items = continuationItems(root)
        for (item in items) {
            // Token halaman berikut = continuationItemRenderer TERAKHIR.
            val trailing = item.optJSONObject("continuationItemRenderer")
            if (trailing != null) {
                findTokenAnywhere(trailing)?.let { nextPageToken = it }
                continue
            }
            // Header (countText dsb.) sengaja diabaikan: total count
            // TIDAK PERNAH ditampilkan — respons tidak menyediakannya.
            if (item.has("commentsHeaderRenderer")) continue

            // Dua bentuk item: thread renderer, atau commentViewModel
            // langsung (balasan).
            val thread = item.optJSONObject("commentThreadRenderer")
            val viewModel = viewModelOf(item) ?: continue
            val commentKey = viewModel.optString("commentKey", null)
                ?.takeIf { it.isNotBlank() } ?: continue
            val entity = entities[commentKey] ?: continue
            val props = entity.optJSONObject("properties") ?: continue
            val text = commentText(props.optJSONObject("content")) ?: continue
            val surface = surfaces[viewModel.optString("commentSurfaceKey", null)]
            val avatarVm = surface?.optJSONObject("avatar")?.optJSONObject("avatarViewModel")
            val authorHandle = props.optString("authorButtonA11y", null)
                ?.takeIf { it.isNotBlank() }
                ?: avatarVm?.optString("accessibilityText", null)?.takeIf { it.isNotBlank() }
                ?: continue
            val toolbar = entity.optJSONObject("toolbar")
            val likeCount = toolbar?.optString("likeCountNotliked", null)
                ?.takeIf { it.isNotBlank() }
                ?: toolbar?.optString("likeCountLiked", null)?.takeIf { it.isNotBlank() }
            val replyCount = toolbar?.optString("replyCount", null)
                ?.takeIf { it.isNotBlank() }
            val stateKey = props.optString("toolbarStateKey", null)
                ?.takeIf { it.isNotBlank() }
                ?: viewModel.optString("toolbarStateKey", null)?.takeIf { it.isNotBlank() }
            val heartState = stateKey?.let { toolbarStates[it]?.optString("heartState", "") } ?: ""
            val hearted = heartState.contains("HEARTED") && !heartState.contains("UNHEARTED")
            val sources = avatarVm?.optJSONObject("image")?.optJSONArray("sources")
            val avatarUrl = if (sources != null && sources.length() > 0) {
                sources.optJSONObject(sources.length() - 1)?.optString("url", null)
                    ?.takeIf { it.isNotBlank() }
            } else {
                null
            }
            val channelId = avatarVm?.optJSONObject("endpoint")
                ?.optJSONObject("innertubeCommand")
                ?.optJSONObject("browseEndpoint")
                ?.optString("browseId", null)?.takeIf { it.isNotBlank() }
            var replyToken: String? = null
            val replyContents = thread?.optJSONObject("replies")
                ?.optJSONObject("commentRepliesRenderer")
                ?.optJSONArray("contents")
            if (replyContents != null) {
                for (i in 0 until replyContents.length()) {
                    val tok = findTokenAnywhere(replyContents.optJSONObject(i))
                    if (tok != null) {
                        replyToken = tok
                        break
                    }
                }
            }
            comments.add(
                Comment(
                    commentId = props.optString("commentId", null)
                        ?.takeIf { it.isNotBlank() }
                        ?: viewModel.optString("commentId", null)
                        ?: commentKey,
                    authorHandle = authorHandle,
                    avatarUrl = avatarUrl,
                    channelId = channelId,
                    publishedTimeText = props.optString("publishedTime", ""),
                    text = text,
                    likeCountText = likeCount,
                    replyCountText = replyCount,
                    hearted = hearted,
                    pinnedText = viewModel.optString("pinnedText", null)
                        ?.takeIf { it.isNotBlank() },
                    replyContinuationToken = replyToken,
                ),
            )
        }
        return CommentsPage(comments, nextPageToken, commentsOff = false)
    }
}
