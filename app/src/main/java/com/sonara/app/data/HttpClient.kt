package com.sonara.app.data

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** HTTP client bersama untuk API YouTube Music, lirik, dan download. */
object SonaraHttp {
    fun defaultHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
}
