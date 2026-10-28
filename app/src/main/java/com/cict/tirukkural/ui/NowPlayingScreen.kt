package com.cict.tirukkural.ui

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.cict.tirukkural.R
import com.cict.tirukkural.data.Paal
import com.cict.tirukkural.data.Tirukkural
import com.cict.tirukkural.playback.OfflineState
import com.cict.tirukkural.ui.theme.BrandNight
import com.cict.tirukkural.ui.theme.BrandNightDeep
import com.cict.tirukkural.ui.theme.accent
import com.cict.tirukkural.ui.theme.gradientColors
import kotlinx.coroutines.delay

private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
private val SLEEP_MINUTES = listOf(15, 30, 45, 60, 90)
private val White70 = Color.White.copy(alpha = 0.7f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    vm: PlayerViewModel,
    ui: PlayerUiState,
    position: PlaybackPosition,
    offline: OfflineState,
    /** Folds the player away; null when it is docked beside the list on large screens. */
    onCollapse: (() -> Unit)?,
) {
    var queueOpen by rememberSaveable { mutableStateOf(false) }
    val dismissDistance = with(LocalDensity.current) { 140.dp.toPx() }
    var dragTotal by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            // The backdrop glow is drawn larger than the screen; keep it inside the player pane.
            .clipToBounds()
            .background(BrandNightDeep)
            .then(
                if (onCollapse == null) {
                    Modifier
                } else {
                    Modifier.pointerInput(Unit) {
                        // Swipe down anywhere outside the controls to fold the player away.
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragTotal > dismissDistance) onCollapse()
                                dragTotal = 0f
                            },
                            onDragCancel = { dragTotal = 0f },
                        ) { _, dragAmount -> dragTotal += dragAmount }
                    }
                },
            ),
    ) {
        PlayerBackdrop(paal = ui.chapter.paal, playing = ui.isPlaying)
        val wide = maxWidth > maxHeight && maxWidth >= 600.dp
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            TopBar(chapterNumber = ui.chapterNumber, onCollapse = onCollapse, onQueue = { queueOpen = true })
            if (wide) {
                Row(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 32.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Artwork(isPlaying = ui.isPlaying, paal = ui.chapter.paal, modifier = Modifier.aspectRatio(1f))
                    }
                    Column(
                        Modifier
                            .weight(1.2f)
                            .padding(start = 32.dp),
                    ) {
                        PlayerDetails(vm, ui, position, offline)
                    }
                }
            } else {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Artwork(
                        isPlaying = ui.isPlaying,
                        paal = ui.chapter.paal,
                        modifier = Modifier
                            .widthIn(max = 460.dp)
                            .aspectRatio(1f),
                    )
                }
                Column(
                    Modifier
                        .widthIn(max = 640.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 24.dp),
                ) {
                    PlayerDetails(vm, ui, position, offline)
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }

    if (queueOpen) {
        ModalBottomSheet(onDismissRequest = { queueOpen = false }) {
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = (ui.chapterNumber - 3).coerceAtLeast(0))
            Text(
                stringResource(R.string.queue),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            LazyColumn(state = listState) {
                items(Tirukkural.chapters, key = { it.number }) { chapter ->
                    val isCurrent = chapter.number == ui.chapterNumber
                    ChapterRow(
                        chapter = chapter,
                        isCurrent = isCurrent,
                        isPlaying = isCurrent && ui.isPlaying,
                        offline = offline,
                        onClick = {
                            vm.playChapter(chapter.number)
                            queueOpen = false
                        },
                        onDownload = { vm.download(chapter.number) },
                        onCancel = { vm.cancelDownload(chapter.number) },
                        onRemove = { vm.removeDownload(chapter.number) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(chapterNumber: Int, onCollapse: (() -> Unit)?, onQueue: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onCollapse != null) {
            IconButton(onClick = onCollapse) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.collapse),
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        } else {
            Spacer(Modifier.size(48.dp))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.now_playing), style = MaterialTheme.typography.labelMedium, color = White70)
            Text(
                stringResource(R.string.chapter_of_total, chapterNumber),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }
        IconButton(onClick = onQueue) {
            Icon(
                Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = stringResource(R.string.queue),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun ColumnScope.PlayerDetails(
    vm: PlayerViewModel,
    ui: PlayerUiState,
    position: PlaybackPosition,
    offline: OfflineState,
) {
    val chapter = ui.chapter
    var scrubMs by remember { mutableStateOf<Long?>(null) }
    val shownPosition = scrubMs ?: position.positionMs

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                "${chapter.paal.tamil}  •  ${chapter.iyal.tamil}",
                style = MaterialTheme.typography.labelLarge,
                color = chapter.paal.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                chapter.title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${stringResource(R.string.kural_range, chapter.kuralRange)}  •  ${chapter.transliteration}",
                style = MaterialTheme.typography.bodyMedium,
                color = White70,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DownloadButton(
            number = chapter.number,
            offline = offline,
            onDownload = { vm.download(chapter.number) },
            onCancel = { vm.cancelDownload(chapter.number) },
            onRemove = { vm.removeDownload(chapter.number) },
            tint = Color.White,
            doneTint = chapter.paal.accent,
        )
    }

    Spacer(Modifier.height(18.dp))
    WaveformSeekBar(
        seed = chapter.number,
        positionMs = position.positionMs,
        bufferedMs = position.bufferedMs,
        durationMs = ui.durationMs,
        onSeek = vm::seekTo,
        onScrub = { scrubMs = it },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    )
    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(formatTime(shownPosition), style = MaterialTheme.typography.labelMedium, color = White70)
        Spacer(Modifier.weight(1f))
        Text(
            "-" + formatTime(ui.durationMs - shownPosition),
            style = MaterialTheme.typography.labelMedium,
            color = White70,
        )
    }

    if (ui.hasError) {
        Spacer(Modifier.height(10.dp))
        ErrorBanner(onRetry = vm::retry)
    }

    Spacer(Modifier.height(14.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToggleIcon(
            onClick = vm::toggleShuffle,
            active = ui.shuffle,
            contentDescription = stringResource(R.string.shuffle),
        ) { Icons.Rounded.Shuffle }
        IconButton(onClick = { vm.previous() }, modifier = Modifier.size(56.dp)) {
            Icon(
                Icons.Rounded.SkipPrevious,
                contentDescription = stringResource(R.string.previous),
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
        }
        PlayPauseButton(
            isPlaying = ui.isPlaying,
            isBuffering = ui.isBuffering,
            onClick = vm::togglePlayPause,
            size = 84.dp,
        )
        IconButton(onClick = { vm.next() }, modifier = Modifier.size(56.dp)) {
            Icon(
                Icons.Rounded.SkipNext,
                contentDescription = stringResource(R.string.next),
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
        }
        ToggleIcon(
            onClick = vm::cycleRepeatMode,
            active = ui.repeatMode != Player.REPEAT_MODE_OFF,
            contentDescription = stringResource(R.string.repeat),
        ) { if (ui.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat }
    }

    Spacer(Modifier.height(16.dp))
    SecondaryControls(vm, ui)
}

@Composable
private fun SecondaryControls(vm: PlayerViewModel, ui: PlayerUiState) {
    var speedMenu by remember { mutableStateOf(false) }
    var sleepMenu by remember { mutableStateOf(false) }
    val now by produceState(System.currentTimeMillis(), ui.sleepAtWallMs) {
        while (ui.sleepAtWallMs > 0L) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val remaining = ui.sleepAtWallMs - now
    val sleepLabel = when {
        ui.sleepAtEndOfChapter -> stringResource(R.string.sleep_end_short)
        ui.sleepAtWallMs > 0L && remaining > 0 -> formatTime(remaining)
        else -> null
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        GlassPill(
            icon = Icons.Rounded.Replay10,
            label = null,
            contentDescription = stringResource(R.string.rewind_10),
            onClick = { vm.seekBack() },
        )
        Box {
            GlassPill(
                icon = Icons.Rounded.Speed,
                label = formatSpeed(ui.speed),
                contentDescription = stringResource(R.string.speed),
                onClick = { speedMenu = true },
                highlighted = ui.speed != 1f,
            )
            DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                SPEEDS.forEach { speed ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                formatSpeed(speed),
                                fontWeight = if (speed == ui.speed) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            vm.setSpeed(speed)
                            speedMenu = false
                        },
                    )
                }
            }
        }
        Box {
            GlassPill(
                icon = Icons.Rounded.Bedtime,
                label = sleepLabel,
                contentDescription = stringResource(R.string.sleep_timer),
                onClick = { sleepMenu = true },
                highlighted = ui.sleepTimerActive,
            )
            DropdownMenu(expanded = sleepMenu, onDismissRequest = { sleepMenu = false }) {
                Text(
                    stringResource(R.string.sleep_timer),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                if (ui.sleepTimerActive) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sleep_off)) },
                        onClick = {
                            vm.setSleepTimer(0)
                            sleepMenu = false
                        },
                    )
                }
                SLEEP_MINUTES.forEach { minutes ->
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sleep_minutes, minutes)) },
                        onClick = {
                            vm.setSleepTimer(minutes)
                            sleepMenu = false
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sleep_end_of_chapter)) },
                    onClick = {
                        vm.setSleepTimer(0, endOfChapter = true)
                        sleepMenu = false
                    },
                )
            }
        }
        GlassPill(
            icon = Icons.Rounded.Forward10,
            label = null,
            contentDescription = stringResource(R.string.forward_10),
            onClick = { vm.seekForward() },
        )
    }
}

@Composable
private fun ToggleIcon(
    onClick: () -> Unit,
    active: Boolean,
    contentDescription: String,
    icon: () -> androidx.compose.ui.graphics.vector.ImageVector,
) {
    IconButton(onClick = onClick) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon(),
                contentDescription = contentDescription,
                tint = if (active) Color.White else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(26.dp),
            )
            if (active) {
                Box(
                    Modifier
                        .padding(top = 34.dp)
                        .size(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White),
                )
            }
        }
    }
}

@Composable
private fun ErrorBanner(onRetry: () -> Unit) {
    Surface(
        color = Color(0x33FF6B6B),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WifiOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text(
                stringResource(R.string.error_network),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.retry), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Artwork that breathes while the recitation plays and settles back when paused. */
@Composable
private fun Artwork(isPlaying: Boolean, paal: Paal, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.86f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 260f),
        label = "artScale",
    )
    val glow by animateColorAsState(paal.accent, tween(600), label = "glow")
    val transition = rememberInfiniteTransition(label = "art")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.62f * (if (isPlaying) pulse else 0.9f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glow.copy(alpha = if (isPlaying) 0.55f else 0.25f), Color.Transparent),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
            )
        }
        Image(
            painter = painterResource(R.drawable.artwork),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize(0.9f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(28.dp, RoundedCornerShape(28.dp), ambientColor = glow, spotColor = glow)
                .clip(RoundedCornerShape(28.dp)),
        )
    }
}

/** Blurred artwork with slowly drifting light in the colours of the current பால். */
@Composable
private fun PlayerBackdrop(paal: Paal, playing: Boolean) {
    val art = ImageBitmap.imageResource(R.drawable.artwork)
    val tiny = remember(art) {
        Bitmap.createScaledBitmap(art.asAndroidBitmap(), 12, 12, true).asImageBitmap()
    }
    val first by animateColorAsState(paal.gradientColors[0], tween(800), label = "aurora1")
    val second by animateColorAsState(paal.gradientColors[1], tween(800), label = "aurora2")
    val transition = rememberInfiniteTransition(label = "backdrop")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )
    val intensity by animateFloatAsState(if (playing) 1f else 0.6f, tween(700), label = "intensity")

    Box(Modifier.fillMaxSize()) {
        Image(
            bitmap = tiny,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.High,
            modifier = Modifier
                .fillMaxSize()
                .then(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(48.dp) else Modifier)
                .graphicsLayer { alpha = 0.75f },
        )
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(first.copy(alpha = 0.55f * intensity), Color.Transparent),
                    center = Offset(w * (0.15f + 0.3f * drift), h * 0.22f),
                    radius = w * 0.9f,
                ),
                radius = w * 0.9f,
                center = Offset(w * (0.15f + 0.3f * drift), h * 0.22f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(second.copy(alpha = 0.4f * intensity), Color.Transparent),
                    center = Offset(w * (0.9f - 0.3f * drift), h * 0.62f),
                    radius = w * 0.8f,
                ),
                radius = w * 0.8f,
                center = Offset(w * (0.9f - 0.3f * drift), h * 0.62f),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.25f),
                        0.45f to BrandNight.copy(alpha = 0.35f),
                        1f to BrandNightDeep.copy(alpha = 0.92f),
                    ),
                ),
        )
    }
}

/** Compact player pinned above the list; tap to open the full player. */
@Composable
fun MiniPlayer(
    ui: PlayerUiState,
    position: PlaybackPosition,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chapter = ui.chapter
    val fraction = if (ui.durationMs > 0) position.positionMs.toFloat() / ui.durationMs else 0f
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(22.dp),
        color = BrandNight,
        shadowElevation = 12.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            Modifier.background(
                Brush.horizontalGradient(
                    listOf(chapter.paal.gradientColors[0].copy(alpha = 0.45f), Color.Transparent),
                ),
            ),
        ) {
            Column {
                Row(
                    Modifier.padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.drawable.artwork),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                    ) {
                        Text(
                            chapter.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${stringResource(R.string.chapter_number, chapter.number)}  •  ${chapter.paal.tamil}",
                            style = MaterialTheme.typography.bodySmall,
                            color = White70,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    PlayPauseButton(
                        isPlaying = ui.isPlaying,
                        isBuffering = ui.isBuffering,
                        onClick = onPlayPause,
                        size = 52.dp,
                    )
                    IconButton(onClick = onNext) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(R.string.next), tint = Color.White)
                    }
                }
                ProgressLine(
                    fraction = fraction,
                    color = chapter.paal.accent,
                    trackColor = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
                )
            }
        }
    }
}
