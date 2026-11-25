package com.sonara.app.data

/**
 * Kebijakan output audio v1.0.7 — murni Kotlin, tanpa Android.
 *
 * Modelnya cerminan [AudioDeviceInfo] / [MediaRouter.RouteInfo]
 * framework (nilai [type] = konstanta AudioDeviceInfo; pemetaan
 * dari objek framework hidup di dialog, bukan di sini) supaya
 * logika label/dedupe/pencocokan bisa diuji mandiri.
 *
 * Kejujuran: SONARA hanya bisa MEMINTA sistem berpindah rute
 * (MediaRouter.selectRoute) dan menampilkan perangkat nyata dari
 * AudioManager. Perangkat tanpa rute sistem yang cocok tidak bisa
 * dipaksa berpindah dari app — dialog mengatakannya, bukan
 * berpura-pura berhasil.
 */
data class AudioDevice(
    val id: Int,
    /** Konstanta AudioDeviceInfo.TYPE_* (2 = speaker, 8 = A2DP, dst). */
    val type: Int,
    val productName: String,
    val isSink: Boolean,
)

data class AudioRoute(
    val id: String,
    val name: String,
    val selected: Boolean,
)

object AudioOutputPolicy {
    // Cerminan AudioDeviceInfo.TYPE_* (API 35, javap -constants).
    const val TYPE_BUILTIN_EARPIECE = 1
    const val TYPE_BUILTIN_SPEAKER = 2
    const val TYPE_WIRED_HEADSET = 3
    const val TYPE_WIRED_HEADPHONES = 4
    const val TYPE_BLUETOOTH_SCO = 7
    const val TYPE_BLUETOOTH_A2DP = 8
    const val TYPE_HDMI = 9
    const val TYPE_USB_DEVICE = 11
    const val TYPE_USB_HEADSET = 22
    const val TYPE_BUILTIN_SPEAKER_SAFE = 24
    const val TYPE_BLE_HEADSET = 26
    const val TYPE_BLE_SPEAKER = 27

    /** Label tampil per perangkat; nama produk dipakai bila ada. */
    fun deviceLabel(device: AudioDevice): String {
        val name = device.productName.trim()
        return when (device.type) {
            TYPE_BUILTIN_SPEAKER, TYPE_BUILTIN_SPEAKER_SAFE -> "Phone speaker"
            TYPE_BUILTIN_EARPIECE -> "Earpiece"
            TYPE_WIRED_HEADPHONES, TYPE_WIRED_HEADSET -> "Wired headphones"
            // Bluetooth klasik: nama produknya sering alamat/MAC —
            // selalu label generik. BLE memakai nama produk bila ada.
            TYPE_BLUETOOTH_A2DP, TYPE_BLUETOOTH_SCO -> "Bluetooth"
            TYPE_BLE_HEADSET, TYPE_BLE_SPEAKER ->
                if (name.isNotEmpty()) name else "Bluetooth"
            TYPE_HDMI -> if (name.isNotEmpty()) name else "HDMI"
            TYPE_USB_DEVICE, TYPE_USB_HEADSET -> if (name.isNotEmpty()) name else "USB audio"
            else -> if (name.isNotEmpty()) name else "Audio device"
        }
    }

    /** Hanya sink, dedupe berdasarkan (tipe, nama) — yang pertama menang. */
    fun listDevices(devices: List<AudioDevice>): List<AudioDevice> {
        val seen = LinkedHashSet<Pair<Int, String>>()
        val out = ArrayList<AudioDevice>()
        for (device in devices) {
            if (!device.isSink) continue
            val key = device.type to device.productName.trim().lowercase()
            if (seen.add(key)) out.add(device)
        }
        return out
    }

    /**
     * Rute sistem yang namanya sama dengan NAMA PRODUK perangkat
     * (normalisasi). Label generik ("Phone speaker") sengaja TIDAK
     * dipakai mencocokkan — label itu milik banyak perangkat dan
     * akan salah pasang dengan rute bernama sama.
     */
    fun matchRoute(device: AudioDevice, routes: List<AudioRoute>): AudioRoute? {
        val wanted = device.productName.trim().lowercase()
        if (wanted.isEmpty()) return null
        return routes.firstOrNull { it.name.trim().lowercase() == wanted }
    }

    fun selectedRoute(routes: List<AudioRoute>): AudioRoute? =
        routes.firstOrNull { it.selected }
}
