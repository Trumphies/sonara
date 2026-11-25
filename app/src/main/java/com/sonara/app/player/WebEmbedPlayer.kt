package com.sonara.app.player

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject

/**
 * Player embed resmi YouTube (IFrame API) — HANYA fallback kalau
 * stream langsung YTM (player API visionOS + ExoPlayer) gagal.
 * WebView ini hanya mesin audio cadangan, dibuat 1x1 dp dan
 * disembunyikan (audio saja, tanpa video). BUKAN pembungkus web app:
 * seluruh UI SONARA native Compose.
 *
 * YouTube IFrame API: https://www.youtube.com/iframe_api
 */
@SuppressLint("SetJavaScriptEnabled")
class WebEmbedPlayer(context: Context) {
    enum class Kind { NONE, YOUTUBE }

    data class WebState(
        val playing: Boolean = false,
        val positionMs: Long = 0,
        val durationMs: Long = 0,
        val ended: Boolean = false,
    )

    /**
     * WebView dimiliki Application (v1.0.2) — dibuat SEKALI dari
     * application context dan hidup di luar composition/activity.
     * Sebelumnya WebView dibuat di composition (activity context) dan
     * dipertahankan selamanya oleh attach(): begitu activity
     * dihancurkan (swipe dari recents), WebView-nya mati dan lagu
     * jalur embed berhenti — dan setelah activity dibuat ulang,
     * WebView basi dari activity yang sudah mati itu tetap dipakai.
     * Alasan lama ("dibuat di Application context bisa merusak
     * dialog/input") tidak berlaku di sini: WebView ini audio-only
     * (IFrame, tanpa dialog/input/file chooser), dan tema application
     * context sudah AppTheme (android:theme di <application>), jadi
     * pembuatan dengan application context aman.
     */
    private val appContext: Context = context.applicationContext
    private var ownedView: WebView? = null

    /**
     * WebView milik Application — dibuat malas di thread Main saat
     * pertama dibutuhkan (semua pemanggil berjalan di Main).
     * Dilepas dari parent lamanya (bila ada) supaya bisa di-host
     * ulang oleh composition baru setelah activity dibuat ulang.
     */
    fun ownedView(): WebView {
        ownedView?.let { existing ->
            (existing.parent as? ViewGroup)?.removeView(existing)
            return existing
        }
        val view = WebView(appContext)
        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = true
        view.settings.mediaPlaybackRequiresUserGesture = false
        view.settings.cacheMode = WebSettings.LOAD_DEFAULT
        view.webChromeClient = WebChromeClient()
        view.webViewClient = WebViewClient()
        ownedView = view
        return view
    }

    /**
     * Kompatibilitas: pemanggil lama memasang WebView buatannya
     * sendiri. Sejak v1.0.2 view asing (activity context) sengaja
     * DIABAIKAN — kepemilikan ada di [ownedView]. Memanggil attach()
     * hanya memastikan owned view ada.
     */
    fun attach(view: WebView) {
        ownedView()
    }

    var kind: Kind = Kind.NONE
        private set
    var ready: Boolean = false
        private set

    private val mainHandler = Handler(Looper.getMainLooper())
    private var pollRunnable: Runnable? = null
    var onState: ((WebState) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    fun loadYouTube(videoId: String) {
        kind = Kind.YOUTUBE
        ready = false
        val html = """
            <!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1">
            <style>html,body{margin:0;padding:0;background:#000;overflow:hidden}</style></head>
            <body><div id="p"></div>
            <script src="https://www.youtube.com/iframe_api"></script>
            <script>
              var player=null; window.__ready=false; window.__state=-1;
              function onYouTubeIframeAPIReady(){
                player=new YT.Player('p',{width:'100%',height:'100%',videoId:'$videoId',
                  playerVars:{autoplay:1,controls:0,disablekb:1,fs:0,modestbranding:1,rel:0,playsinline:1},
                  events:{onReady:function(e){window.__ready=true;e.target.setVolume(100);e.target.playVideo();},
                    onStateChange:function(e){window.__state=e.data;},
                    onError:function(e){window.__err='YT:'+e.data;}}});
              }
              window.sonaraPlay=function(){if(player)player.playVideo();};
              window.sonaraPause=function(){if(player)player.pauseVideo();};
              window.sonaraSeek=function(s){if(player)player.seekTo(s,true);};
              window.sonaraVolume=function(v){if(player)player.setVolume(v);};
              window.sonaraState=function(){
                if(!player||!window.__ready)return JSON.stringify({playing:false,position:0,duration:0,ended:false});
                return JSON.stringify({playing:window.__state===1,
                  position:Math.floor(player.getCurrentTime()*1000),
                  duration:Math.floor(player.getDuration()*1000),
                  ended:window.__state===0});
              };
            </script></body></html>
        """.trimIndent()
        mainHandler.post {
            ownedView?.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
        }
        startPolling()
    }

    fun play() = eval("window.sonaraPlay&&window.sonaraPlay();")
    fun pause() = eval("window.sonaraPause&&window.sonaraPause();")

    /** Seek dalam detik untuk YouTube. */
    fun seekSeconds(seconds: Double) = eval("window.sonaraSeek&&window.sonaraSeek($seconds);")
    fun setVolume(percent: Int) = eval("window.sonaraVolume&&window.sonaraVolume($percent);")

    fun stop() {
        stopPolling()
        kind = Kind.NONE
        mainHandler.post { ownedView?.loadUrl("about:blank") }
    }

    fun destroy() {
        stopPolling()
        val view = ownedView ?: return
        ownedView = null
        try {
            view.destroy()
        } catch (_: Exception) {
        }
    }

    private fun eval(js: String) {
        val view = ownedView ?: return
        mainHandler.post {
            try {
                view.evaluateJavascript(js, null)
            } catch (_: Exception) {
            }
        }
    }

    private fun startPolling() {
        stopPolling()
        val r = object : Runnable {
            override fun run() {
                val view = ownedView
                if (kind == Kind.NONE || view == null) return
                try {
                    view.evaluateJavascript("window.sonaraState?window.sonaraState():'{}'") { value ->
                        try {
                            val clean = value?.trim('"')?.replace("\\\"", "\"") ?: "{}"
                            val json = JSONObject(clean)
                            ready = true
                            onState?.invoke(
                                WebState(
                                    playing = json.optBoolean("playing", false),
                                    positionMs = json.optLong("position", 0),
                                    durationMs = json.optLong("duration", 0),
                                    ended = json.optBoolean("ended", false),
                                ),
                            )
                        } catch (_: Exception) {
                        }
                    }
                    view.evaluateJavascript("window.__err||''") { value ->
                        val err = value?.trim('"').orEmpty()
                        if (err.isNotEmpty() && err != "null") {
                            onError?.invoke(err)
                        }
                    }
                } catch (_: Exception) {
                }
                // v1.10.39: 250ms (dulu 500ms) — pergantian baris lirik
                // pada jalur fallback embed tertinggal sampai ~0,5 dtk
                // + latensi bridge JS; 250ms tetap murah (satu eval JS).
                mainHandler.postDelayed(this, 250)
            }
        }
        pollRunnable = r
        mainHandler.postDelayed(r, 800)
    }

    private fun stopPolling() {
        pollRunnable?.let { mainHandler.removeCallbacks(it) }
        pollRunnable = null
    }
}
