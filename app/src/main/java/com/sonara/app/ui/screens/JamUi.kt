package com.sonara.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonara.app.R
import com.sonara.app.data.JAM_STACK_MAX_DEFAULT
import com.sonara.app.data.JamConnection
import com.sonara.app.data.JamMember
import com.sonara.app.data.JamUiState
import com.sonara.app.data.jamAvatarStack
import com.sonara.app.ui.SonaraState
import com.sonara.app.ui.showToast
import com.sonara.app.ui.theme.AccentFill
import com.sonara.app.ui.theme.BottomCluster
import com.sonara.app.ui.theme.OnAccent
import com.sonara.app.ui.theme.StageBg
import com.sonara.app.ui.theme.Corners
import com.sonara.app.ui.theme.CurrentTokens
import com.sonara.app.ui.theme.InkBlack
import com.sonara.app.ui.theme.InkMuted

/**
 * UI Jamming Together (v1.10.14, arah C — "Kartu nempel, tanpa dialog").
 * ARUS, semua string bahasa Inggris. TIDAK ada dialog besar lagi:
 *
 * - JamEntryPill: mark broadcast logo-saja di baris header Beranda
 *   (v1.10.40, dulu pill "Jam"/kode). Bila sesi aktif, tap mark
 *   membuka layar Jam penuh; bila tidak, membuka layar Jam
 *   (status kosong: Start/Join) — sheet Start/Join 10.14 DIHAPUS
 *   di v1.10.42, logikanya pindah ke JamScreen (JamEmptyState).
 * - JamPinnedCard: SATU kartu tepat di atas mini player + nav (slot yang
 *   sama dengan JamSessionBar lama), hanya tampil saat sesi hidup.
 *   Ciut = satu baris; mekar = isi sesi Pulse (orbit + kode + reaksi +
 *   Now playing + Up next + vote + chat + End/Leave, lihat
 *   JamSessionBody di JamScreen.kt). Menutup kartu
 *   TIDAK keluar dari sesi — hanya End/Leave yang mengakhiri.
 * - JamLivePill: penanda LIVE di detail lagu; tap-nya kembali ke layar
 *   utama (tempat kartu nempel berada).
 * - Kontrol putar tamu dikunci di layar pemanggil (SonaraApp +
 *   NowPlayingScreen) lewat state.jam.guestLocked — lihat
 *   rememberGuestLocked() di bawah.
 * - Perilaku Jam (sinkron, antrean, deep link, lockout) TIDAK berubah:
 *   JamSessionManager/JamRest/JamModel tidak disentuh.
 */

/** True bila client ini tamu di sesi aktif — kontrol putar lokal harus dikunci. */
@Composable
fun rememberGuestLocked(state: SonaraState): Boolean {
    val locked by state.jam.guestLocked.collectAsState()
    return locked
}

/** Toast penolakan kontrol tamu (satu-satunya teks penolakan Jam). */
fun jamHostOnlyToast(context: Context) {
    showToast(context, "Only the host can control playback during a Jam.")
}

/**
 * Tumpukan avatar anggota sesi Jam yang tumpang-tindih (v1.10.44):
 * avatar pertama penuh, sisanya bergeser setengah lebar ke kanan;
 * anggota di luar [maxShown] jadi chip "+N". Sumber data = anggota
 * sesi yang sudah ada (kebijakan [jamAvatarStack]); pemanggil yang
 * memutuskan kapan tampil (hanya saat sesi hidup). Avatar memakai
 * [JamMemberAvatar] yang sama dengan orbit layar Jam — foto profil
 * bila ada, kalau tidak cakram inisial. Bukan tombol: tidak ada
 * modifier sentuhan, tap jatuh ke elemen di bawahnya.
 */
@Composable
fun JamMemberStack(
    members: List<JamMember>,
    maxShown: Int = JAM_STACK_MAX_DEFAULT,
    avatarSize: Dp = 18.dp,
    borderColor: Color,
    extraBackground: Color = InkBlack,
    extraTextColor: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    val stack = jamAvatarStack(members, maxShown)
    if (stack.shown.isEmpty() && stack.extra == 0) return
    val step = avatarSize * 0.55f
    val slots = stack.shown.size + if (stack.extra > 0) 1 else 0
    val totalWidth = avatarSize + step * (slots - 1).coerceAtLeast(0)
    Box(modifier.size(totalWidth, avatarSize)) {
        stack.shown.forEachIndexed { index, member ->
            JamMemberAvatar(
                member = member,
                size = avatarSize,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = step * index),
                ringColor = borderColor,
            )
        }
        if (stack.extra > 0) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = step * stack.shown.size)
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(extraBackground)
                    .border(1.5.dp, borderColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "+${stack.extra}",
                    fontSize = (avatarSize.value * 0.42f).sp,
                    fontWeight = FontWeight.Black,
                    color = extraTextColor,
                )
            }
        }
    }
}

/**
 * Mark broadcast Jam di baris header Beranda — LOGO SAJA (v1.10.40):
 * tanpa teks "Jam", tanpa wadah pill. Bila sesi aktif, tap mark
 * melebarkan kartu; bila tidak, membuka sheet kecil Start/Join
 * (perilaku tap persis seperti pill sebelumnya). Sesi aktif tetap
 * kentara: mark duduk di atas cakram [AccentFill] 40dp (sejajar
 * avatar + gir 40dp); idle mark telanjang di atas latar header.
 * Varian mark dipilih dari luminans latarnya (harness140e): idle =
 * varian gelap di latar gelap / terang di latar terang; aktif =
 * varian terang bila cakram AccentFill terang, varian on-orange
 * bila cakram gelap — busur luar mark >= 3.12:1 di semua varian.
 */
@Composable
fun JamEntryPill(
    state: SonaraState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val jamState by state.jam.uiState.collectAsState()
    val active = jamState.active
    // v1.10.44: anggota sesi segar (sumber yang sama dengan orbit
    // layar Jam) — tumpukan avatar hanya saat sesi hidup.
    val members = jamState.members
    // Latar header = background tema; cakram aktif = AccentFill.
    val darkGround = CurrentTokens.background.luminance() < 0.5f
    val lightDisc = AccentFill.luminance() >= 0.35f
    Box(
        modifier
            // v1.10.40: area sentuh 48dp (dulu pill 40dp) — mark
            // visual 28dp idle / cakram 40dp aktif, tetap sejajar
            // avatar + gir di baris header.
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClickLabel = if (active) "Open Jam session" else "Open Jam", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (active) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AccentFill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(
                        id = if (lightDisc) R.drawable.ic_jam_broadcast_light
                        else R.drawable.ic_jam_broadcast_on_orange,
                    ),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(22.dp),
                )
            }
        } else {
            Icon(
                painter = painterResource(
                    id = if (darkGround) R.drawable.ic_jam_broadcast_dark
                    else R.drawable.ic_jam_broadcast_light,
                ),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(28.dp),
            )
        }
        // v1.10.44: tumpukan avatar anggota di sudut kanan bawah
        // mark — hanya saat sesi hidup dan ada anggota segar; idle
        // tetap logo-saja. Bukan tombol (tap = tap mark).
        if (active && members.isNotEmpty()) {
            JamMemberStack(
                members = members,
                avatarSize = 18.dp,
                borderColor = CurrentTokens.background,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

/** Satu-satunya teks status koneksi Jam — dipakai baris ciut dan kartu mekar. */
internal fun jamStatusText(jamState: JamUiState): String {
    val base = when (jamState.connection) {
        JamConnection.CONNECTING -> "Connecting…"
        JamConnection.LIVE ->
            if (jamState.transport == "Polling") "Live • polling" else "Live • in sync"
        JamConnection.RECONNECTING -> "Connection lost — retrying…"
        JamConnection.OFFLINE -> "Offline"
        JamConnection.IDLE -> "Live"
    }
    return base + if (jamState.hostAway) " • Host away" else ""
}

internal fun shareJamCode(context: Context, state: SonaraState, code: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, state.jam.shareText(code))
    }
    context.startActivity(Intent.createChooser(send, "Share Jam code"))
}

/**
 * UI Jam — bottom chrome baru (v1.10.18, menggantikan kartu nempel
 * 10.14 arah C). Tiga elemen:
 *
 * - JamStatusChip: SATU baris ramping (~30dp) di atas mini player,
 *   hanya saat sesi hidup — mark broadcast + "LIVE • KODE •
 *   N listening • judul berjalan" (ellipsis). Ketuk → sheet.
 * - JamSheetOverlay: isi kartu mekar VERBATIM sebagai bottom sheet
 *   overlay (~75% layar + scrim); tutup via chevron bawah / scrim /
 *   geser ke bawah / Back sistem (Back menutup sheet dulu).
 * - JamSessionSheetContent: header sheet (kode + tombol ciutkan) +
 *   [JamSessionBody] — isi sesi Pulse yang SAMA dengan layar Jam
 *   penuh. Menutup sheet tidak mengubah state layar di bawahnya.
 * Perilaku sesi (sinkron, antrean, deep link, lockout) TIDAK berubah.
 * Panah: tidak ada lagi baris ciut ber-chevron; chevron di header
 * sheet menunjuk BAWAH (menutup ke bawah) — konsisten dengan
 * perbaikan arah panah 10.18 (ciut=atas, mekar=bawah).
 */

/** Chip status Jam ramping — satu baris di atas mini player. */
@Composable
fun JamStatusChip(
    state: SonaraState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val jamState by state.jam.uiState.collectAsState()
    if (!jamState.active) return
    val currentTrack by state.player.currentTrack.collectAsState()
    val nowTitle = jamState.nowTitle.ifNotEmpty { currentTrack?.title ?: "" }
    val text = buildString {
        append("LIVE • ")
        append(jamState.code)
        append(" • ")
        append(jamState.members.size)
        append(" listening")
        if (nowTitle.isNotEmpty()) {
            append(" • ")
            append(nowTitle)
        }
    }
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(BottomCluster.jamChipHeight)
            .clip(RoundedCornerShape(Corners.miniPlayer))
            .background(StageBg)
            .clickable(onClickLabel = "Open Jam session", onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_jam_broadcast_dark),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(BottomCluster.jamChipIcon),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Bottom sheet overlay sesi Jam (scrim + geser-ke-bawah + Back). */
@Composable
fun JamSheetOverlay(
    state: SonaraState,
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    // Back sistem menutup sheet DULU sebelum navigasi (handler ini
    // terdaftar setelah handler layar, jadi diprioritaskan).
    BackHandler(enabled = true, onBack = onDismiss)
    var dragY by remember { mutableStateOf(0f) }
    Box(Modifier.fillMaxSize()) {
        // v1.10.40: scrim HARUS buram penuh (opaque). Dengan alpha 0.5,
        // teks layar di baliknya tembus lewat 25% atas layar yang tidak
        // tertutup sheet — pola yang sama dengan ArusBottomSheet.
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClickLabel = "Close Jam session", onClick = onDismiss),
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .clip(RoundedCornerShape(topStart = Corners.sheet, topEnd = Corners.sheet))
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(onDismiss) {
                    detectVerticalDragGestures(
                        onDragStart = { dragY = 0f },
                        onVerticalDrag = { _, dy -> dragY += dy },
                        onDragEnd = {
                            if (dragY > 100f) onDismiss()
                            dragY = 0f
                        },
                    )
                },
        ) {
            // Gagang geser di bibir atas sheet.
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(InkMuted.copy(alpha = 0.5f)),
                )
            }
            JamSessionSheetContent(
                state = state,
                onClose = onDismiss,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Isi sheet sesi Jam (v1.10.42): header ringkas (kode + tombol
 * ciutkan) + isi sesi Pulse yang SAMA dengan layar Jam penuh
 * ([JamSessionBody]). Menutup sheet TIDAK keluar dari sesi.
 * Baris Now playing di sheet tidak membuka detail (perilaku lama:
 * barisnya memang tidak bisa diklik) — detail dibuka dari layar Jam.
 */
@Composable
fun JamSessionSheetContent(
    state: SonaraState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val jamState by state.jam.uiState.collectAsState()
    Column(modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        ) {
            Text(
                jamState.code,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                color = InkBlack,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                // Panah BAWAH: sheet menciut ke bawah (lihat komentar
                // panah di baris ciut).
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = "Collapse Jam session",
                    tint = InkBlack,
                )
            }
        }
        JamSessionBody(
            state = state,
            onOpenDetail = {},
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun JamLivePill(
    state: SonaraState,
    modifier: Modifier = Modifier,
    /** Tap kembali ke layar utama (tempat kartu nempel berada). Null = indikator saja. */
    onClick: (() -> Unit)? = null,
) {
    val jamState by state.jam.uiState.collectAsState()
    if (!jamState.active) return
    val count = jamState.members.size
    val boxModifier = if (onClick != null) {
        modifier.clickable(onClickLabel = "Open Jam session", onClick = onClick)
    } else {
        modifier
    }
    Box(
        boxModifier
            .clip(RoundedCornerShape(Corners.pill))
            .background(AccentFill)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // v1.10.18: mark broadcast (varian pill, di atas oranye)
            // mendampingi teks LIVE yang sudah ada.
            Icon(
                painter = painterResource(id = R.drawable.ic_jam_broadcast_on_orange),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (count > 0) "LIVE • $count listening" else "LIVE",
                color = OnAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private enum class PendingJamAction { CREATE, JOIN }


/** ifNotEmpty untuk String — dipakai baris ciut (judul dari sesi, fallback lagu lokal). */
private fun String.ifNotEmpty(fallback: () -> String): String =
    if (isNotEmpty()) this else fallback()
