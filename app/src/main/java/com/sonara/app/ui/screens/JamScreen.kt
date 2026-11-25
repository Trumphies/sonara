package com.sonara.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonara.app.data.FLOATING_REACTION_LIFETIME_MS
import com.sonara.app.data.FloatingReaction
import com.sonara.app.data.JAM_REACTION_IDS
import com.sonara.app.data.floatingDriftDp
import com.sonara.app.data.floatingRotationDeg
import com.sonara.app.data.floatingStartFraction
import com.sonara.app.data.spawnFloatingReactions
import com.sonara.app.data.JamConnection
import com.sonara.app.data.JamMember
import com.sonara.app.data.JamReaction
import com.sonara.app.data.JamRole
import com.sonara.app.data.JamUiState
import com.sonara.app.data.Track
import com.sonara.app.data.avatarHueFor
import com.sonara.app.data.displayArtist
import com.sonara.app.data.isValidJamCode
import com.sonara.app.data.jamShareUrl
import com.sonara.app.data.orbitAngles
import com.sonara.app.data.reactionEmoji
import com.sonara.app.data.toTrack
import com.sonara.app.ui.ArusBackButton
import com.sonara.app.ui.CoverArt
import com.sonara.app.ui.DragHandleDots
import com.sonara.app.ui.SonaraState
import com.sonara.app.ui.PrimaryButton
import com.sonara.app.ui.ReorderableColumn
import com.sonara.app.ui.SecondaryButton
import com.sonara.app.ui.sonaraTextButtonColors
import com.sonara.app.ui.sonaraTextFieldColors
import com.sonara.app.ui.rememberReducedMotion
import com.sonara.app.ui.showToast
import com.sonara.app.ui.theme.AccentFill
import com.sonara.app.ui.theme.BurntOrange
import com.sonara.app.ui.theme.Corners
import com.sonara.app.ui.theme.CurrentTokens
import com.sonara.app.ui.theme.InkBlack
import com.sonara.app.ui.theme.InkMuted
import com.sonara.app.ui.theme.OnAccent
import com.sonara.app.ui.theme.PaperSurface
import com.sonara.app.ui.theme.ThemeError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Layar Jam full-screen (v1.10.42, layout + perilaku Pulse):
 * SATU permukaan sesi yang sama dengan isi sheet overlay Jam
 * ([JamSessionBody] dipakai keduanya) — dibuka dari tab Jam,
 * mark Jam di header, dan (sebagai sheet) dari chip ramping.
 * Urutan isi dari atas (Pulse): judul + badge, orbit, kotak kode,
 * toggle kontrol bersama, reaksi, Now playing, antrean + vote,
 * tambah lagu, chat, Share/End. Tema, klaster bawah, detail,
 * lirik, komentar, feed, dan sinkron TIDAK berubah.
 *
 * Lagu + artwork = data YTM NYATA (bukan lagu/cover karangan
 * Pulse); anggota orbit = anggota sesi NYATA (tanpa member palsu).
 */
@Composable
fun JamScreen(
    state: SonaraState,
    onBack: () -> Unit,
    onOpenDetail: (Track) -> Unit,
    /** Kode dari deep link Jam yang diketuk; null = dibuka biasa. */
    initialJoinCode: String? = null,
    /** Dipanggil sekali saat kode deep link mulai ditangani. */
    onInitialJoinCodeConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val jamState by state.jam.uiState.collectAsState()
    val reducedMotion = rememberReducedMotion()
    // Floaters overlay (v1.10.43): direset tiap sesi baru; spawn dari
    // callback reaksi segar di JamSessionBody (dedupe di kebijakan murni).
    var floaters by remember(jamState.code) { mutableStateOf<List<FloatingReaction>>(emptyList()) }
    Column(modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            ArusBackButton(onClick = onBack)
            Spacer(Modifier.width(4.dp))
            Text(
                "Jamming Together",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = InkBlack,
                modifier = Modifier.weight(1f),
            )
        }
        if (!jamState.active) {
            JamEmptyState(
                state = state,
                initialJoinCode = initialJoinCode,
                onInitialJoinCodeConsumed = onInitialJoinCodeConsumed,
                modifier = Modifier.weight(1f),
            )
        } else {
            Box(Modifier.weight(1f)) {
                JamSessionBody(
                    state = state,
                    onOpenDetail = onOpenDetail,
                    modifier = Modifier.fillMaxSize(),
                    onFreshReactions = { fresh ->
                        if (!reducedMotion) {
                            floaters = spawnFloatingReactions(
                                floaters, fresh, System.currentTimeMillis(),
                            )
                        }
                    },
                )
                if (!reducedMotion && floaters.isNotEmpty()) {
                    FloatingReactionOverlay(
                        floaters = floaters,
                        onExpired = { key ->
                            floaters = floaters.filter { it.key != key }
                        },
                    )
                }
            }
        }
    }
}

/**
 * Mark broadcast Jam (logo) yang digambar Canvas — titik + dua busur
 * kiri/kanan — supaya warnanya selalu token tema (tidak seperti
 * drawable paten yang membalik di varian gelap/terang). Dipakai
 * tab Jam di navigasi (terpilih = [OnChrome], tidak = [InkMuted],
 * dot LIVE aksen terpisah). Bentuk mengikuti mark logo 10.18
 * (dot + busur ±50°), diskalakan ke ukuran ikon.
 */
@Composable
fun JamMarkIcon(
    color: Color,
    markSize: Dp = 24.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(markSize)) {
        val min = size.minDimension
        val cx = size.width / 2f
        val cy = size.height / 2f
        val stroke = min * 0.075f
        drawCircle(color = color, radius = min * 0.10f, center = Offset(cx, cy))
        for (r in listOf(min * 0.30f, min * 0.44f)) {
            drawArc(
                color = color,
                startAngle = 140f,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = -40f,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

/** Avatar anggota orbit: foto profil bila ada, kalau tidak cakram inisial hue-dari-uid. */
/** v1.10.44: internal (bukan private) — dipakai ulang tumpukan avatar Jam (JamUi.kt). */
@Composable
internal fun JamMemberAvatar(
    member: JamMember,
    size: Dp,
    modifier: Modifier = Modifier,
    // v1.10.44: cincin pemisah opsional (tumpukan avatar) — null = tanpa cincin (orbit).
    ringColor: Color? = null,
) {
    val hue = avatarHueFor(member.uid)
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.hsl(hue.toFloat(), 0.65f, 0.55f))
            .then(if (ringColor != null) Modifier.border(1.5.dp, ringColor, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            member.name.firstOrNull()?.uppercase() ?: "?",
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
        )
        if (member.photoUrl.isNotBlank()) {
            AsyncImage(
                model = member.photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().clip(CircleShape),
            )
        }
    }
}

/**
 * Orbit sesi (Pulse): artwork lagu berjalan di tengah (bundar),
 * cincin denyut aksen, avatar anggota melingkar dengan label nama.
 * Statis saat reduced-motion (cincin tunggal, tanpa denyut).
 * Maks 8 avatar di orbit; sisanya jadi chip "+N".
 */
@Composable
fun JamOrbit(
    members: List<JamMember>,
    centerTrack: Track?,
    modifier: Modifier = Modifier,
) {
    val tokens = CurrentTokens
    val reducedMotion = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "jamOrbit")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "jamOrbitPulse",
    )
    val pulse2 = if (reducedMotion) 0f else (pulse + 0.5f) % 1f
    val p1 = if (reducedMotion) 0f else pulse
    val orbitSize = 296.dp
    val centerSize = 104.dp
    val avatarSize = 48.dp
    val density = LocalDensity.current
    val radiusPx = with(density) { (orbitSize / 2f - avatarSize / 2f - 4.dp).toPx() }
    val shown = members.take(8)
    val extra = (members.size - shown.size).coerceAtLeast(0)
    val angles = orbitAngles(shown.size)
    Box(
        modifier
            .fillMaxWidth()
            .height(orbitSize),
        contentAlignment = Alignment.Center,
    ) {
        // Cincin denyut (Pulse .rg): dua cincin mengembang + memudar.
        for ((p, _) in listOf(p1 to 0f, pulse2 to 0f)) {
            val scale = 0.8f + p * 1.2f
            val alpha = (1f - p) * 0.7f
            Box(
                Modifier
                    .size(150.dp * scale)
                    .clip(CircleShape)
                    .border(2.dp, tokens.accent.copy(alpha = alpha), CircleShape),
            )
        }
        // Artwork tengah (NYATA — dari player/sesi, bukan generatif).
        if (centerTrack != null) {
            CoverArt(
                track = centerTrack,
                size = centerSize,
                corner = 999.dp,
            )
        } else {
            Box(
                Modifier
                    .size(centerSize)
                    .clip(CircleShape)
                    .background(tokens.accent.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                JamMarkIcon(color = tokens.accent, markSize = 40.dp)
            }
        }
        // Avatar anggota di lingkaran.
        for ((i, member) in shown.withIndex()) {
            val angleRad = Math.toRadians(angles[i].toDouble())
            val dx = (cos(angleRad) * radiusPx).roundToInt()
            val dy = (sin(angleRad) * radiusPx).roundToInt()
            Column(
                Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(dx, dy) }
                    .offset(x = (-avatarSize / 2), y = (-avatarSize / 2)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                JamMemberAvatar(member = member, size = avatarSize)
                Spacer(Modifier.height(3.dp))
                Text(
                    member.name.ifEmpty { "Someone" },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Corners.small))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 7.dp, vertical = 1.dp),
                )
            }
        }
        if (extra > 0) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .clip(CircleShape)
                    .background(tokens.accent)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("+$extra", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnAccent)
            }
        }
    }
}

private enum class JamEmptyPendingAction { CREATE, JOIN }

/**
 * Status kosong sesi (Pulse): orbit + Start a Jam + kolom kode
 * inline + Join. Logika deep link + login dipindah VERBATIM dari
 * JamStartSheet lama (6 kotak kode diganti kolom tunggal Pulse;
 * kode deep link tetap terisi + auto-join).
 */
@Composable
private fun JamEmptyState(
    state: SonaraState,
    initialJoinCode: String?,
    onInitialJoinCodeConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val jamState by state.jam.uiState.collectAsState()
    val syncState by state.sync.uiState.collectAsState()
    val currentTrack by state.player.currentTrack.collectAsState()

    var codeInput by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingAction by remember { mutableStateOf<JamEmptyPendingAction?>(null) }

    fun toast(msg: String) = showToast(context, msg)

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        state.sync.handleGoogleSignInResult(result.data, ::toast)
    }

    fun launchSignIn() {
        val intent = state.sync.googleSignInIntent()
        if (intent != null) signInLauncher.launch(intent)
        else toast("Google sign-in isn't available on this device.")
    }

    fun doCreate() {
        if (!syncState.signedIn) {
            pendingAction = JamEmptyPendingAction.CREATE
            launchSignIn()
            return
        }
        busy = true
        error = null
        state.jam.createSession { result ->
            busy = false
            result.onFailure { e ->
                error = e.message ?: "Couldn't start a Jam."
            }
        }
    }

    fun doJoin(code: String) {
        if (!syncState.signedIn) {
            pendingAction = JamEmptyPendingAction.JOIN
            launchSignIn()
            return
        }
        busy = true
        error = null
        state.jam.joinSession(code) { result ->
            busy = false
            result.onFailure { e ->
                error = e.message ?: "Couldn't join the Jam."
            }
        }
    }

    LaunchedEffect(syncState.signedIn) {
        if (!syncState.signedIn) return@LaunchedEffect
        when (pendingAction) {
            JamEmptyPendingAction.CREATE -> {
                pendingAction = null
                doCreate()
            }
            JamEmptyPendingAction.JOIN -> {
                pendingAction = null
                if (codeInput.isNotBlank()) doJoin(codeInput)
            }
            null -> Unit
        }
    }

    var handledLinkCode by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(initialJoinCode) {
        val code = initialJoinCode ?: return@LaunchedEffect
        if (handledLinkCode == code) return@LaunchedEffect
        handledLinkCode = code
        onInitialJoinCodeConsumed()
        codeInput = code
        error = null
        val sessionAlive = jamState.active && jamState.code.isNotEmpty()
        if (sessionAlive && jamState.code == code) {
            // Sudah di sesi yang sama — tidak ada yang dilakukan
            // (layar akan menampilkan sesi begitu aktif).
        } else {
            if (sessionAlive) {
                error = "You're already in another Jam. Joining will leave it."
            }
            doJoin(code)
        }
    }

    // Diri sendiri di orbit (jujur: hanya "You", tanpa member palsu).
    val selfMember = if (syncState.signedIn) {
        listOf(
            JamMember(
                uid = syncState.email.ifEmpty { "self" },
                name = syncState.displayName.ifEmpty { "You" },
                photoUrl = syncState.photoUrl,
                lastSeenMs = 0L,
            ),
        )
    } else {
        emptyList()
    }

    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        item {
            Text(
                "Jam together",
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                color = InkBlack,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "One queue, everyone's taste, perfectly in sync.",
                fontSize = 14.sp,
                color = InkMuted,
            )
            Spacer(Modifier.height(6.dp))
        }
        item {
            JamOrbit(members = selfMember, centerTrack = currentTrack)
        }
        item {
            if (!syncState.signedIn) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Sign in with Google to start or join a Jam.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkBlack,
                )
                Spacer(Modifier.height(12.dp))
                PrimaryButton(
                    text = "Sign in with Google",
                    onClick = ::launchSignIn,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Spacer(Modifier.height(6.dp))
                PrimaryButton(
                    text = if (busy) "Starting…" else "Start a Jam",
                    onClick = ::doCreate,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                )
                Spacer(Modifier.height(16.dp))
                Text("or join with a code", fontSize = 13.sp, color = InkMuted)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { raw ->
                        codeInput = raw.uppercase()
                            .filter { it in 'A'..'Z' || it in '0'..'9' }
                            .take(6)
                        error = null
                    },
                    label = { Text("Jam code") },
                    placeholder = { Text("K7X2QP") },
                    colors = sonaraTextFieldColors(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (isValidJamCode(codeInput)) doJoin(codeInput) },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                PrimaryButton(
                    text = if (busy) "Joining…" else "Join",
                    onClick = { doJoin(codeInput) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy && isValidJamCode(codeInput),
                )
            }
            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, fontSize = 14.sp, color = ThemeError)
            }
            Spacer(Modifier.height(20.dp))
            for ((title, sub) in listOf(
                "Share a code" to "Friends join from any phone, no extra app.",
                "Everyone adds songs" to "Add from search, mixes or your library.",
                "Vote the queue" to "The most upvoted song plays next.",
                "React and chat live" to "Reactions float, chat stays in the session.",
            )) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 5.dp)) {
                    Box(
                        Modifier.size(8.dp).clip(CircleShape).background(AccentFill),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = InkBlack)
                        Text(sub, fontSize = 12.sp, color = InkMuted)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Isi sesi Jam dalam urutan Pulse — dipakai layar full-screen DAN
 * sheet overlay Jam ([JamSessionSheetContent]): satu implementasi,
 * dua wadah. Menutup wadah TIDAK keluar dari sesi.
 */
@Composable
fun JamSessionBody(
    state: SonaraState,
    onOpenDetail: (Track) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Dipanggil sekali per batch reaksi segar (lokal maupun remote —
     * sumbernya snapshot yang sama). Layar Jam memakainya untuk
     * overlay emoji terbang; sheet overlay tidak mengopernya, jadi
     * perilaku sheet TIDAK berubah.
     */
    onFreshReactions: (List<JamReaction>) -> Unit = {},
) {
    val jamState by state.jam.uiState.collectAsState()
    val context = LocalContext.current
    val currentTrack by state.player.currentTrack.collectAsState()
    val listState = rememberLazyListState()

    // ---- Reaksi cepat (v1.10.17): pola yang sama dengan sheet lama —
    // reaksi BARU mengambang ~3 detik di atas tombol, lalu hilang.
    val reactionScope = rememberCoroutineScope()
    var floatingReactions by remember(jamState.code) { mutableStateOf<List<JamReaction>>(emptyList()) }
    var seenReactionKeys by remember(jamState.code) { mutableStateOf<Set<String>>(emptySet()) }
    var latestReaction by remember(jamState.code) { mutableStateOf<JamReaction?>(null) }
    var reactionCoolingDown by remember(jamState.code) { mutableStateOf(false) }
    LaunchedEffect(jamState.recentReactions) {
        val fresh = jamState.recentReactions.filter { it.key !in seenReactionKeys }
        if (fresh.isNotEmpty()) {
            onFreshReactions(fresh)
            seenReactionKeys = seenReactionKeys + fresh.map { it.key }
            floatingReactions = (floatingReactions + fresh).takeLast(4)
            val last = fresh.last()
            latestReaction = last
            val keys = fresh.map { it.key }.toSet()
            reactionScope.launch {
                delay(3000L)
                floatingReactions = floatingReactions.filter { it.key !in keys }
            }
            reactionScope.launch {
                delay(5000L)
                if (latestReaction?.key == last.key) latestReaction = null
            }
        }
    }

    // ---- Cari + tambah lagu (logika yang sama dengan sheet lama):
    // debounce 600ms, max 6 hasil.
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Track>>(emptyList()) }
    var searchBusy by remember { mutableStateOf(false) }
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            return@LaunchedEffect
        }
        delay(600L)
        searchBusy = true
        val res = withContext(Dispatchers.IO) {
            state.ytmApi.search(searchQuery)
        }
        searchResults = res.getOrDefault(emptyList()).take(6)
        searchBusy = false
    }

    // ---- Chat: kirim lalu gulir ke pesan terbaru.
    var chatInput by remember { mutableStateOf("") }
    var scrollToChat by remember { mutableStateOf(false) }
    val messageCount = jamState.messages.size
    LaunchedEffect(messageCount) {
        if (scrollToChat && messageCount > 0) {
            scrollToChat = false
            // Indeks item terakhir = tombol Share/End; pesan terakhir
            // ada tepat sebelumnya — gulir ke akhir daftar.
            runCatching { listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1) }
        }
    }
    fun sendChat() {
        val text = chatInput.trim()
        if (text.isEmpty()) return
        chatInput = ""
        scrollToChat = true
        state.jam.sendChatMessage(text) { msg ->
            showToast(context, msg)
        }
    }

    val clipboard = LocalClipboardManager.current
    val centerTrack = currentTrack ?: jamState.nowTitle.takeIf { it.isNotEmpty() }?.let {
        Track(
            id = "jam:now",
            title = it,
            artist = jamState.nowArtist,
            source = com.sonara.app.data.Source.YOUTUBE_MUSIC,
            engine = com.sonara.app.data.Engine.YOUTUBE,
        )
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        item {
            // 1. Judul + badge (Pulse: nama sesi + "N listening · host").
            Text(
                "Jam session",
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                color = InkBlack,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(8.dp).clip(CircleShape)
                        .background(if (jamState.connection == JamConnection.LIVE) AccentFill else InkMuted),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "${jamState.orbitMembers.size} listening · Host ${jamState.hostName.ifEmpty { "Someone" }}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkMuted,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                jamStatusText(jamState),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = InkMuted,
            )
            if (jamState.hostAway) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "The host seems away — playback continues locally and will re-sync when the host is back.",
                    fontSize = 13.sp,
                    color = ThemeError,
                )
            }
            val followProblem = jamState.followProblem
            if (followProblem != null) {
                Spacer(Modifier.height(8.dp))
                Text(followProblem, fontSize = 13.sp, color = ThemeError)
            }
        }
        item {
            // 2. Orbit.
            Spacer(Modifier.height(6.dp))
            JamOrbit(members = jamState.orbitMembers, centerTrack = centerTrack)
        }
        item {
            // 3. Kotak kode invite + Copy (link bridge https yang sudah ada).
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Corners.card))
                    .background(PaperSurface)
                    .padding(start = 18.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Invite code", fontSize = 13.sp, color = InkMuted)
                    Text(
                        jamState.code,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        color = InkBlack,
                    )
                }
                SecondaryButton(
                    text = "Copy",
                    onClick = {
                        clipboard.setText(AnnotatedString(jamShareUrl(jamState.code)))
                        showToast(context, "Invite link copied.")
                    },
                )
            }
        }
        item {
            // 4. Toggle kontrol bersama (host saja yang mengubah).
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Everyone can control playback",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkBlack,
                    )
                    Text(
                        if (jamState.sharedControl) "Everyone can pause, skip, and seek."
                        else "Off: only the host can pause or skip.",
                        fontSize = 12.sp,
                        color = InkMuted,
                    )
                }
                Switch(
                    checked = jamState.sharedControl,
                    onCheckedChange = { enabled ->
                        state.jam.setSharedControl(enabled) { msg ->
                            if (msg.isNotEmpty()) showToast(context, msg)
                        }
                    },
                    enabled = jamState.role == JamRole.HOST,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OnAccent,
                        checkedTrackColor = BurntOrange,
                        checkedBorderColor = Color.Transparent,
                    ),
                )
            }
        }
        item {
            // 5. Reaksi (sistem 10.17 — tombol + mengambang).
            Spacer(Modifier.height(10.dp))
            if (floatingReactions.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    floatingReactions.forEach { reaction ->
                        Text(
                            "${reactionEmoji(reaction.reactionId)} ${reaction.senderName}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Text("Reactions", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = InkBlack)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                JAM_REACTION_IDS.forEach { reactionId ->
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                CircleShape,
                            )
                            .background(
                                if (reactionCoolingDown) MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                else PaperSurface,
                            )
                            .clickable(
                                enabled = !reactionCoolingDown,
                                onClickLabel = "Send $reactionId reaction",
                                onClick = {
                                    reactionCoolingDown = true
                                    reactionScope.launch {
                                        delay(2000L)
                                        reactionCoolingDown = false
                                    }
                                    state.jam.sendReaction(reactionId) { msg ->
                                        showToast(context, msg)
                                    }
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(reactionEmoji(reactionId), fontSize = 22.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        item {
            // 6. Now playing.
            Text("Now playing", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = InkBlack)
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Corners.small))
                    .clickable(
                        enabled = currentTrack != null,
                        onClickLabel = "Open now playing",
                        onClick = { currentTrack?.let(onOpenDetail) },
                    ),
            ) {
                val artTrack = currentTrack
                if (artTrack != null) {
                    CoverArt(
                        track = artTrack,
                        size = 48.dp,
                        corner = Corners.small,
                        letterSize = 18.sp,
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    when {
                        jamState.waitingForHost -> Text(
                            "Waiting for the host to play something…",
                            fontSize = 14.sp,
                            color = InkMuted,
                        )
                        jamState.hostLocalOnly -> Text(
                            "${jamState.nowTitle} — ${jamState.nowArtist}\nThe host is playing local music, which can't be shared in a Jam.",
                            fontSize = 14.sp,
                            color = InkMuted,
                        )
                        jamState.nowTitle.isNotEmpty() -> {
                            Text(
                                jamState.nowTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InkBlack,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                jamState.nowArtist,
                                fontSize = 12.sp,
                                color = InkMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        artTrack != null -> {
                            Text(
                                artTrack.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InkBlack,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                displayArtist(artTrack.artist),
                                fontSize = 12.sp,
                                color = InkMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        else -> Text("Nothing playing yet.", fontSize = 14.sp, color = InkMuted)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        item {
            // 7. Antrean Jam + vote + reorder + hapus host-only.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Up next (${jamState.upcomingQueue.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkBlack,
                    modifier = Modifier.weight(1f),
                )
                val adding = currentTrack
                if (adding?.youtubeVideoId != null) {
                    TextButton(
                        onClick = {
                            state.jam.addTrackToQueue(adding) { msg ->
                                showToast(context, msg)
                            }
                        },
                        modifier = Modifier.height(48.dp),
                        colors = sonaraTextButtonColors(),
                    ) {
                        Text("Add current song", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (jamState.upcomingQueue.isEmpty()) {
            item {
                Text(
                    "No songs in the queue yet — add one below.",
                    fontSize = 14.sp,
                    color = InkMuted,
                )
                Spacer(Modifier.height(8.dp))
            }
        } else {
            item {
                ReorderableColumn(
                    items = jamState.upcomingQueue,
                    keyOf = { it.key },
                    dragEnabled = true,
                    onMove = { from, to ->
                        val moved = jamState.upcomingQueue.toMutableList().apply {
                            add(to, removeAt(from))
                        }
                        state.jam.reorderSessionQueue(moved) { msg ->
                            showToast(context, msg)
                        }
                    },
                    rowContent = { item, _, handleModifier ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(
                                    onClickLabel = "Play ${item.title} in the Jam",
                                    onClick = {
                                        if (jamState.role == JamRole.HOST) {
                                            state.jam.jumpToQueueItem(item) { msg ->
                                                showToast(context, msg)
                                            }
                                        } else {
                                            state.jam.requestJumpToQueueItem(item) { msg ->
                                                showToast(context, msg)
                                            }
                                        }
                                    },
                                )
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Tombol vote (Pulse): chevron + jumlah.
                            val voted = item.key in jamState.votedKeys
                            val count = jamState.voteCounts[item.key] ?: 0
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(Corners.small))
                                    .clickable(
                                        onClickLabel = if (voted) "Remove vote for ${item.title}" else "Vote for ${item.title}",
                                        onClick = {
                                            state.jam.toggleVote(item) { msg ->
                                                if (msg.isNotEmpty()) showToast(context, msg)
                                            }
                                        },
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                            ) {
                                Icon(
                                    Icons.Filled.KeyboardArrowUp,
                                    contentDescription = null,
                                    tint = if (voted) AccentFill else InkMuted,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    "$count",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (voted) AccentFill else InkMuted,
                                )
                            }
                            val itemTrack = item.toTrack()
                            CoverArt(
                                track = itemTrack,
                                size = 48.dp,
                                corner = Corners.small,
                                letterSize = 18.sp,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = InkBlack,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    run { val a = displayArtist(item.artist); if (a.isNotBlank()) "$a • Added by ${item.addedByName}" else "Added by ${item.addedByName}" },
                                    fontSize = 12.sp,
                                    color = InkMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Box(
                                modifier = handleModifier.size(40.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                DragHandleDots(tint = InkMuted)
                            }
                            if (jamState.role == JamRole.HOST) {
                                IconButton(
                                    onClick = {
                                        state.jam.removeQueueItem(item) { msg ->
                                            showToast(
                                                context,
                                                if (msg == "Removed from the Jam queue.") {
                                                    "Removed \"${item.title}\" from the Jam queue."
                                                } else {
                                                    msg
                                                },
                                            )
                                        }
                                    },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Remove ${item.title} from the Jam queue",
                                        tint = InkMuted,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    },
                )
            }
        }
        item {
            // 8. Tambah lagu (kolom cari inline, pola sheet lama).
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Add a song to the queue") },
                colors = sonaraTextFieldColors(),
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = InkMuted)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (searchBusy) {
                Spacer(Modifier.height(8.dp))
                Text("Searching…", fontSize = 13.sp, color = InkMuted)
            }
            Spacer(Modifier.height(8.dp))
        }
        itemsIndexed(searchResults, key = { i, t -> "jamsearch:" + t.id + "#" + i }) { _, track ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        displayArtist(track.artist),
                        fontSize = 12.sp,
                        color = InkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = {
                        state.jam.addTrackToQueue(track) { msg ->
                            showToast(context, msg)
                        }
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add to Jam queue", tint = InkBlack)
                }
            }
        }
        item {
            // 9. Chat.
            Spacer(Modifier.height(16.dp))
            Text("Chat", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = InkBlack)
            Spacer(Modifier.height(8.dp))
            if (jamState.messages.isEmpty()) {
                Text(
                    "No messages yet — say hi to the session.",
                    fontSize = 14.sp,
                    color = InkMuted,
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        items(jamState.messages, key = { it.key }) { message ->
            val own = message.senderUid.isNotEmpty() &&
                message.senderUid == state.jam.currentUidForChatUi()
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = if (own) Arrangement.End else Arrangement.Start,
            ) {
                Column(
                    Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 14.dp,
                                topEnd = if (own) 6.dp else 14.dp,
                                bottomStart = 14.dp,
                                bottomEnd = if (own) 14.dp else 6.dp,
                            ),
                        )
                        .background(
                            if (own) MaterialTheme.colorScheme.surfaceVariant
                            else PaperSurface,
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            RoundedCornerShape(
                                topStart = 14.dp,
                                topEnd = if (own) 6.dp else 14.dp,
                                bottomStart = 14.dp,
                                bottomEnd = if (own) 14.dp else 6.dp,
                            ),
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    if (!own) {
                        Text(
                            message.senderName.ifEmpty { "Someone" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentFill,
                        )
                    }
                    Text(
                        message.text,
                        fontSize = 14.sp,
                        color = InkBlack,
                    )
                }
            }
        }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) {
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { chatInput = it },
                    placeholder = { Text("Say something") },
                    colors = sonaraTextFieldColors(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendChat() }),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = { sendChat() },
                    enabled = chatInput.isNotBlank(),
                    colors = sonaraTextButtonColors(),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Send", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row {
                SecondaryButton(
                    text = "Share link",
                    onClick = { shareJamCode(context, state, jamState.code) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                SecondaryButton(
                    text = if (jamState.role == JamRole.HOST) "End session" else "Leave",
                    onClick = {
                        if (jamState.role == JamRole.HOST) state.jam.endSession()
                        else state.jam.leaveSession()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

// jamStatusText + shareJamCode live in JamUi.kt (internal) — reused here.

/**
 * Overlay emoji reaksi terbang (v1.10.43, perilaku Pulse — BUKAN
 * salinan aset/lagu Pulse): emoji muncul di sepertiga bawah layar,
 * naik melintasi layar sambil bergeser horizontal + berotasi, lalu
 * memudar selama [FLOATING_REACTION_LIFETIME_MS]. Overlay ini
 * SENGAJA tanpa modifier sentuhan apa pun (tanpa clickable /
 * pointerInput), jadi sentuhan jatuh tembus ke LazyColumn di
 * bawahnya dan tidak ada layout yang bergeser. Hanya di layar Jam
 * layar-penuh — sheet overlay tidak memakai overlay ini (perilaku
 * sheet tetap baris statis 10.17). Saat reduced-motion, overlay
 * tidak dipasang sama sekali; badge statis di atas tombol reaksi
 * (yang sudah ada) menjadi fallback.
 */
@Composable
private fun FloatingReactionOverlay(
    floaters: List<FloatingReaction>,
    onExpired: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val width = maxWidth
        val height = maxHeight
        floaters.forEach { floater ->
            key(floater.key) {
                FloatingReactionFloater(
                    floater = floater,
                    containerWidth = width,
                    containerHeight = height,
                    onExpired = onExpired,
                )
            }
        }
    }
}

/** Satu emoji terbang: satu Animatable per item, cakupan recomposition sekecil mungkin. */
@Composable
private fun FloatingReactionFloater(
    floater: FloatingReaction,
    containerWidth: Dp,
    containerHeight: Dp,
    onExpired: (String) -> Unit,
) {
    val progress = remember(floater.key) { Animatable(0f) }
    LaunchedEffect(floater.key) {
        progress.animateTo(
            1f,
            animationSpec = tween(
                durationMillis = FLOATING_REACTION_LIFETIME_MS.toInt(),
                easing = LinearEasing,
            ),
        )
        onExpired(floater.key)
    }
    val density = LocalDensity.current
    val p = progress.value
    // Spawn: sepertiga bawah layar (pola Pulse: titik spawn tetap di
    // bawah, tidak mengikuti baris reaksi yang bisa ter-gulir),
    // x tersebar 18–82% lebar dari seed kunci; naik ~46% tinggi layar.
    val startXFrac = floatingStartFraction(floater.seed)
    val risePx = with(density) { (containerHeight * 0.46f).toPx() }
    val startYPx = with(density) { (containerHeight * 0.78f).toPx() }
    val halfGlyphPx = with(density) { 17.dp.toPx() }
    val xPx = with(density) { (containerWidth * startXFrac).toPx() } +
        with(density) { floatingDriftDp(floater.seed).dp.toPx() } * p - halfGlyphPx
    val yPx = startYPx - risePx * p - halfGlyphPx
    val floaterAlpha = when {
        p < 0.1f -> p / 0.1f
        p > 0.65f -> (1f - p) / 0.35f
        else -> 1f
    }
    val scale = 0.85f + 0.25f * p
    Text(
        text = reactionEmoji(floater.reactionId),
        fontSize = 34.sp,
        style = TextStyle(
            shadow = Shadow(
                color = InkBlack.copy(alpha = 0.25f),
                blurRadius = 8f,
            ),
        ),
        modifier = Modifier.graphicsLayer {
            translationX = xPx
            translationY = yPx
            rotationZ = floatingRotationDeg(floater.seed) * p
            scaleX = scale
            scaleY = scale
            alpha = floaterAlpha
        },
    )
}
