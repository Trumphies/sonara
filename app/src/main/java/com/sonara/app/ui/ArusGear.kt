package com.sonara.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Ikon GIR KUSTOM SONARA (v1.10.6) — digambar dari nol untuk app
 * ini, BUKAN ikon template/library (bukan Icons.Default.Settings).
 *
 * Konstruksi: 8 gigi membulat (garis luar polar: akar r=7, ujung
 * r=10, viewport 24) + lubang tengah, semuanya STROKE dengan ujung
 * dan sambungan membulat — bobot garis selaras ikon header lainnya,
 * tetap terbaca sebagai gir pada ukuran kecil. Titik-titik path
 * di bawah dihasilkan dari konstruksi polar tersebut (bukan salinan
 * ikon aplikasi lain). Warna mengikuti `tint` pemanggil (Icon
 * memakai warna dari konteks header: InkBlack di Beranda).
 */
val ArusGearIcon: ImageVector
    get() = ImageVector.Builder(
        name = "ArusGear",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(18.81f, 10.37f)
            lineTo(21.90f, 10.61f)
            lineTo(21.98f, 11.30f)
            lineTo(22.00f, 12.00f)
           
            lineTo(21.98f, 12.70f)
            lineTo(21.90f, 13.39f)
            lineTo(18.81f, 13.63f)
            lineTo(18.60f, 14.34f)
           
            lineTo(18.32f, 15.01f)
            lineTo(17.97f, 15.66f)
            lineTo(19.99f, 18.02f)
            lineTo(19.55f, 18.56f)
           
            lineTo(19.07f, 19.07f)
            lineTo(18.56f, 19.55f)
            lineTo(18.02f, 19.99f)
            lineTo(15.66f, 17.97f)
           
            lineTo(15.01f, 18.32f)
            lineTo(14.34f, 18.60f)
            lineTo(13.63f, 18.81f)
            lineTo(13.39f, 21.90f)
           
            lineTo(12.70f, 21.98f)
            lineTo(12.00f, 22.00f)
            lineTo(11.30f, 21.98f)
            lineTo(10.61f, 21.90f)
           
            lineTo(10.37f, 18.81f)
            lineTo(9.66f, 18.60f)
            lineTo(8.99f, 18.32f)
            lineTo(8.34f, 17.97f)
           
            lineTo(5.98f, 19.99f)
            lineTo(5.44f, 19.55f)
            lineTo(4.93f, 19.07f)
            lineTo(4.45f, 18.56f)
           
            lineTo(4.01f, 18.02f)
            lineTo(6.03f, 15.66f)
            lineTo(5.68f, 15.01f)
            lineTo(5.40f, 14.34f)
           
            lineTo(5.19f, 13.63f)
            lineTo(2.10f, 13.39f)
            lineTo(2.02f, 12.70f)
            lineTo(2.00f, 12.00f)
           
            lineTo(2.02f, 11.30f)
            lineTo(2.10f, 10.61f)
            lineTo(5.19f, 10.37f)
            lineTo(5.40f, 9.66f)
           
            lineTo(5.68f, 8.99f)
            lineTo(6.03f, 8.34f)
            lineTo(4.01f, 5.98f)
            lineTo(4.45f, 5.44f)
           
            lineTo(4.93f, 4.93f)
            lineTo(5.44f, 4.45f)
            lineTo(5.98f, 4.01f)
            lineTo(8.34f, 6.03f)
           
            lineTo(8.99f, 5.68f)
            lineTo(9.66f, 5.40f)
            lineTo(10.37f, 5.19f)
            lineTo(10.61f, 2.10f)
           
            lineTo(11.30f, 2.02f)
            lineTo(12.00f, 2.00f)
            lineTo(12.70f, 2.02f)
            lineTo(13.39f, 2.10f)
           
            lineTo(13.63f, 5.19f)
            lineTo(14.34f, 5.40f)
            lineTo(15.01f, 5.68f)
            lineTo(15.66f, 6.03f)
           
            lineTo(18.02f, 4.01f)
            lineTo(18.56f, 4.45f)
            lineTo(19.07f, 4.93f)
            lineTo(19.55f, 5.44f)
           
            lineTo(19.99f, 5.98f)
            lineTo(17.97f, 8.34f)
            lineTo(18.32f, 8.99f)
            lineTo(18.60f, 9.66f)
            close()
        }
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(15.10f, 12.00f)
            lineTo(14.68f, 13.55f)
            lineTo(13.55f, 14.68f)
            lineTo(12.00f, 15.10f)
           
            lineTo(10.45f, 14.68f)
            lineTo(9.32f, 13.55f)
            lineTo(8.90f, 12.00f)
            lineTo(9.32f, 10.45f)
           
            lineTo(10.45f, 9.32f)
            lineTo(12.00f, 8.90f)
            lineTo(13.55f, 9.32f)
            lineTo(14.68f, 10.45f)
            close()
        }
    }.build()
