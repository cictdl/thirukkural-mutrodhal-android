package com.cict.tirukkural.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cict.tirukkural.R
import com.cict.tirukkural.data.Chapter
import com.cict.tirukkural.playback.OfflineState
import com.cict.tirukkural.ui.theme.NumberStyle
import com.cict.tirukkural.ui.theme.brush
import java.util.Locale
import kotlin.random.Random

fun formatTime(ms: Long): String {
    val totalSec = (ms.coerceAtLeast(0L) / 1000).toInt()
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.ROOT, "%d:%02d", m, s)
}

fun formatSpeed(speed: Float): String =
    if (speed % 1f == 0f) String.format(Locale.ROOT, "%.1f×", speed)
    else String.format(Locale.ROOT, "%s×", speed.toString().trimEnd('0'))

/** Rounded square showing the அதிகாரம் number, or a live equalizer while it is being recited. */
@Composable
fun ChapterBadge(
    chapter: Chapter,
    isCurrent: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(chapter.paal.brush),
        contentAlignment = Alignment.Center,
    ) {
        if (isCurrent) {
            Equalizer(playing = isPlaying, color = Color.White, modifier = Modifier.size(size * 0.42f))
        } else {
            Text(
                text = chapter.number.toString(),
                style = NumberStyle,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun Equalizer(playing: Boolean, color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "equalizer")
    val durations = intArrayOf(520, 380, 640)
    val heights = durations.mapIndexed { i, duration ->
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, delayMillis = i * 90, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "bar$i",
        )
    }
    Canvas(modifier) {
        val gap = size.width * 0.18f
        val barWidth = (size.width - gap * 2) / 3
        heights.forEachIndexed { i, anim ->
            val fraction = if (playing) anim.value else if (i == 1) 0.55f else 0.35f
            val barHeight = size.height * fraction
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (barWidth + gap), size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2),
            )
        }
    }
}

/**
 * Seek bar drawn as a waveform. Bar heights are a stable pattern per chapter (not real audio
 * analysis, which would need extra permissions) so each அதிகாரம் has its own recognisable shape.
 */
@Composable
fun WaveformSeekBar(
    seed: Int,
    positionMs: Long,
    bufferedMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    playedColor: Color = Color.White,
    bufferedColor: Color = Color.White.copy(alpha = 0.42f),
    trackColor: Color = Color.White.copy(alpha = 0.18f),
    onScrub: (Long?) -> Unit = {},
) {
    val amplitudes = remember(seed) {
        val random = Random(seed * 7919)
        var previous = 0.5f
        FloatArray(96) {
            val target = 0.18f + random.nextFloat() * 0.82f
            previous = previous * 0.45f + target * 0.55f
            previous
        }
    }
    var scrubFraction by remember { mutableFloatStateOf(-1f) }
    var widthPx by remember { mutableFloatStateOf(1f) }
    val duration = durationMs.coerceAtLeast(1L)
    val played = if (scrubFraction >= 0f) scrubFraction else (positionMs.toFloat() / duration).coerceIn(0f, 1f)
    val buffered = (bufferedMs.toFloat() / duration).coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .semantics { contentDescription = "${formatTime(positionMs)} / ${formatTime(durationMs)}" }
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    onSeek((offset.x / size.width).coerceIn(0f, 1f).times(duration).toLong())
                }
            }
            .pointerInput(duration) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        scrubFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onScrub((scrubFraction * duration).toLong())
                    },
                    onDragEnd = {
                        onSeek((scrubFraction * duration).toLong())
                        scrubFraction = -1f
                        onScrub(null)
                    },
                    onDragCancel = {
                        scrubFraction = -1f
                        onScrub(null)
                    },
                ) { change, _ ->
                    change.consume()
                    scrubFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                    onScrub((scrubFraction * duration).toLong())
                }
            },
    ) {
        widthPx = size.width
        val step = size.width / amplitudes.size
        val barWidth = step * 0.58f
        amplitudes.forEachIndexed { i, amplitude ->
            val x = i * step + (step - barWidth) / 2
            val fraction = (i + 0.5f) / amplitudes.size
            val color = when {
                fraction <= played -> playedColor
                fraction <= buffered -> bufferedColor
                else -> trackColor
            }
            val barHeight = size.height * amplitude
            drawLine(
                color = color,
                start = Offset(x + barWidth / 2, (size.height - barHeight) / 2),
                end = Offset(x + barWidth / 2, (size.height + barHeight) / 2),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }
        // Playhead
        val headX = played * size.width
        drawLine(
            color = playedColor,
            start = Offset(headX, 0f),
            end = Offset(headX, size.height),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

/** Large round play/pause button with a spinner ring while the recitation buffers. */
@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    containerColor: Color = Color.White,
    contentColor: Color = Color(0xFF14172E),
) {
    val label = stringResource(if (isPlaying) R.string.pause else R.string.play)
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                color = containerColor,
                strokeWidth = 3.dp,
                strokeCap = StrokeCap.Round,
            )
        }
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(size - 10.dp)
                .clip(CircleShape)
                .background(containerColor),
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (scaleIn(tween(180)) + fadeIn(tween(180))) togetherWith (scaleOut(tween(120)) + fadeOut(tween(120)))
                },
                label = "playPause",
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(size * 0.48f),
                )
            }
        }
    }
}

/** Offline status for one chapter: save, progress (tap to cancel), or saved (tap for remove). */
@Composable
fun DownloadButton(
    number: Int,
    offline: OfflineState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    doneTint: Color = MaterialTheme.colorScheme.primary,
) {
    val progress = offline.progress[number]
    var menuOpen by remember { mutableStateOf(false) }
    Box(modifier) {
        when {
            progress != null -> IconButton(onClick = onCancel) {
                Box(contentAlignment = Alignment.Center) {
                    if (progress < 0f) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            color = tint,
                            strokeWidth = 2.5.dp,
                            strokeCap = StrokeCap.Round,
                        )
                    } else {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(26.dp),
                            color = tint,
                            trackColor = tint.copy(alpha = 0.2f),
                            strokeWidth = 2.5.dp,
                            strokeCap = StrokeCap.Round,
                        )
                    }
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cancel_download),
                        tint = tint,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            number in offline.downloaded -> IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Rounded.DownloadDone, contentDescription = stringResource(R.string.downloaded), tint = doneTint)
            }

            else -> IconButton(onClick = onDownload) {
                Icon(
                    Icons.Rounded.DownloadForOffline,
                    contentDescription = stringResource(R.string.download),
                    tint = tint.copy(alpha = 0.75f),
                )
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.remove_download)) },
                leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    onRemove()
                },
            )
        }
    }
}

/** Small translucent pill used for the secondary player controls. */
@Composable
fun GlassPill(
    icon: ImageVector,
    label: String?,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    val background = if (highlighted) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.12f)
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.width(12.dp))
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        if (label != null) {
            Box(Modifier.width(6.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
        Box(Modifier.width(12.dp))
    }
}

/** Thin progress line used under the mini player and the continue card. */
@Composable
fun ProgressLine(fraction: Float, color: Color, trackColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(trackColor),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
    }
}
