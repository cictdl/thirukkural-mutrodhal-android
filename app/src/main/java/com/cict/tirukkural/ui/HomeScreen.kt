package com.cict.tirukkural.ui

import android.text.format.Formatter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cict.tirukkural.BuildConfig
import com.cict.tirukkural.R
import com.cict.tirukkural.data.Chapter
import com.cict.tirukkural.data.Iyal
import com.cict.tirukkural.data.Paal
import com.cict.tirukkural.data.TOTAL_AUDIO_MB
import com.cict.tirukkural.data.Tirukkural
import com.cict.tirukkural.playback.OfflineState
import com.cict.tirukkural.ui.theme.BrandNight
import com.cict.tirukkural.ui.theme.BrandNightDeep
import com.cict.tirukkural.ui.theme.brush

/** Keeps the list readable on tablets and landscape instead of stretching edge to edge. */
private val ContentMaxWidth = 840.dp

private fun Chapter.matches(query: String): Boolean {
    if (query.isEmpty()) return true
    query.toIntOrNull()?.let { n -> return number == n || n in firstKural..lastKural }
    val latin = query.lowercase()
    return title.contains(query) ||
        transliteration.lowercase().contains(latin) ||
        iyal.tamil.contains(query) ||
        paal.tamil.contains(query) ||
        paal.english.lowercase().contains(latin)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    vm: PlayerViewModel,
    ui: PlayerUiState,
    position: PlaybackPosition,
    offline: OfflineState,
    listState: LazyListState,
    bottomPadding: Dp,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var paalFilter by rememberSaveable { mutableStateOf<Paal?>(null) }
    var dialog by rememberSaveable { mutableStateOf(HomeDialog.None) }

    val sections = remember(query, paalFilter) {
        val q = query.trim()
        Tirukkural.chapters
            .filter { (paalFilter == null || it.paal == paalFilter) && it.matches(q) }
            .groupBy { it.iyal }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "hero") {
            Hero(
                offline = offline,
                onDownloadAll = { dialog = HomeDialog.DownloadAll },
                onRemoveAll = { dialog = HomeDialog.RemoveAll },
                onAbout = { dialog = HomeDialog.About },
            )
        }
        item(key = "continue") {
            ContinueCard(
                ui = ui,
                position = position,
                onPlayPause = vm::togglePlayPause,
                onOpen = onOpenPlayer,
            )
        }
        item(key = "search") {
            SearchField(query = query, onQueryChange = { query = it })
        }
        item(key = "filters") {
            PaalFilters(selected = paalFilter, onSelect = { paalFilter = it })
        }
        if (sections.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.no_results),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .widthIn(max = ContentMaxWidth)
                        .fillMaxWidth()
                        .padding(32.dp),
                )
            }
        }
        sections.forEach { (iyal, chapters) ->
            stickyHeader(key = "iyal-${iyal.tamil}") {
                IyalHeader(iyal = iyal, count = chapters.size)
            }
            items(chapters, key = { it.number }) { chapter ->
                val isCurrent = chapter.number == ui.chapterNumber
                ChapterRow(
                    chapter = chapter,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && ui.isPlaying,
                    offline = offline,
                    onClick = {
                        if (isCurrent && ui.playWhenReady) onOpenPlayer() else vm.playChapter(chapter.number)
                    },
                    onDownload = { vm.download(chapter.number) },
                    onCancel = { vm.cancelDownload(chapter.number) },
                    onRemove = { vm.removeDownload(chapter.number) },
                )
            }
        }
    }

    when (dialog) {
        HomeDialog.DownloadAll -> ConfirmDialog(
            icon = Icons.Rounded.DownloadForOffline,
            title = stringResource(R.string.download_all),
            text = stringResource(R.string.download_all_message, TOTAL_AUDIO_MB),
            onConfirm = vm::downloadAll,
            onDismiss = { dialog = HomeDialog.None },
        )

        HomeDialog.RemoveAll -> ConfirmDialog(
            icon = Icons.Rounded.DeleteSweep,
            title = stringResource(R.string.remove_all_downloads),
            text = stringResource(
                R.string.remove_all_message,
                Formatter.formatShortFileSize(LocalContext.current, offline.usedBytes),
            ),
            onConfirm = vm::removeAllDownloads,
            onDismiss = { dialog = HomeDialog.None },
        )

        HomeDialog.About -> AboutDialog(
            onDismiss = { dialog = HomeDialog.None },
            onPrivacyPolicy = { dialog = HomeDialog.PrivacyPolicy },
        )
        HomeDialog.PrivacyPolicy -> PrivacyPolicyDialog(onDismiss = { dialog = HomeDialog.About })
        HomeDialog.None -> Unit
    }
}

private enum class HomeDialog { None, DownloadAll, RemoveAll, About, PrivacyPolicy }

@Composable
private fun Hero(
    offline: OfflineState,
    onDownloadAll: () -> Unit,
    onRemoveAll: () -> Unit,
    onAbout: () -> Unit,
) {
    val hours = Tirukkural.totalDurationSec / 3600
    val minutes = (Tirukkural.totalDurationSec % 3600) / 60
    // Shorter banner on landscape tablets and other short windows so the list stays in view.
    val heroHeight = if (LocalConfiguration.current.screenHeightDp < 700) 216.dp else 272.dp
    Box(
        Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .background(BrandNight),
    ) {
        // Thiruvalluvar artwork on the right, its own lettering faded into the navy.
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .aspectRatio(1f),
        ) {
            Image(
                painter = painterResource(R.drawable.artwork),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Canvas(Modifier.fillMaxSize()) {
                // Cover the artwork's built-in title lines (measured on the 512 px source) so only
                // the portrait shows beside the app's own heading.
                val unit = size.width / 512f
                drawRect(BrandNight, topLeft = Offset(0f, 36f * unit), size = Size(290f * unit, 94f * unit))
                drawRect(BrandNight, topLeft = Offset(0f, 138f * unit), size = Size(260f * unit, 86f * unit))
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to BrandNight,
                            0.42f to BrandNight,
                            0.54f to BrandNight.copy(alpha = 0.45f),
                            0.66f to Color.Transparent,
                        ),
                    ),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to BrandNightDeep)),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                .fillMaxWidth(),
        ) {
            Column(Modifier.fillMaxWidth(0.7f)) {
                Text("திருக்குறள்", style = MaterialTheme.typography.displaySmall, color = Color.White)
                Text("முற்றோதல்", style = MaterialTheme.typography.displaySmall, color = Color(0xFFD9C4FF))
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(R.string.app_name_english),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.72f),
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                StatChip(stringResource(R.string.stat_kurals))
                StatChip(stringResource(R.string.stat_chapters))
                StatChip(stringResource(R.string.stat_hours, hours, minutes))
            }
        }
        HeroMenu(
            offline = offline,
            onDownloadAll = onDownloadAll,
            onRemoveAll = onRemoveAll,
            onAbout = onAbout,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp),
        )
    }
}

@Composable
private fun StatChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(BrandNight.copy(alpha = 0.55f))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun HeroMenu(
    offline: OfflineState,
    onDownloadAll: () -> Unit,
    onRemoveAll: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Box(modifier) {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more), tint = Color.White)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            R.string.offline_summary,
                            offline.downloaded.size,
                            Formatter.formatShortFileSize(context, offline.usedBytes),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                leadingIcon = { Icon(Icons.Rounded.WifiOff, contentDescription = null) },
                enabled = false,
                onClick = {},
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.download_all)) },
                leadingIcon = { Icon(Icons.Rounded.DownloadForOffline, contentDescription = null) },
                enabled = offline.downloaded.size < Tirukkural.CHAPTER_COUNT,
                onClick = {
                    open = false
                    onDownloadAll()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.remove_all_downloads)) },
                leadingIcon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null) },
                enabled = offline.usedBytes > 0,
                onClick = {
                    open = false
                    onRemoveAll()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.about)) },
                leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                onClick = {
                    open = false
                    onAbout()
                },
            )
        }
    }
}

@Composable
private fun ContinueCard(
    ui: PlayerUiState,
    position: PlaybackPosition,
    onPlayPause: () -> Unit,
    onOpen: () -> Unit,
) {
    val chapter = ui.chapter
    val started = ui.chapterNumber > 1 || position.positionMs > 0 || ui.playWhenReady
    val fraction = if (ui.durationMs > 0) position.positionMs.toFloat() / ui.durationMs else 0f
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .widthIn(max = ContentMaxWidth)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .fillMaxWidth(),
        onClick = onOpen,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ChapterBadge(chapter, isCurrent = ui.playWhenReady, isPlaying = ui.isPlaying, size = 56.dp)
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
            ) {
                Text(
                    stringResource(if (started) R.string.continue_listening else R.string.start_recitation),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    chapter.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (started) {
                        "${stringResource(R.string.chapter_number, chapter.number)} • " +
                            "${formatTime(position.positionMs)} / ${formatTime(ui.durationMs)}"
                    } else {
                        stringResource(R.string.start_recitation_hint)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
                if (started) {
                    Spacer(Modifier.height(8.dp))
                    ProgressLine(
                        fraction = fraction,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
            PlayPauseButton(
                isPlaying = ui.isPlaying,
                isBuffering = ui.isBuffering,
                onClick = onPlayPause,
                size = 60.dp,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val focus = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .widthIn(max = ContentMaxWidth)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cancel))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    )
}

@Composable
private fun PaalFilters(selected: Paal?, onSelect: (Paal?) -> Unit) {
    Row(
        Modifier
            .widthIn(max = ContentMaxWidth)
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.filter_all)) },
            shape = RoundedCornerShape(50),
        )
        Paal.entries.forEach { paal ->
            FilterChip(
                selected = selected == paal,
                onClick = { onSelect(if (selected == paal) null else paal) },
                label = { Text(paal.tamil) },
                leadingIcon = {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(paal.brush),
                    )
                },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun IyalHeader(iyal: Iyal, count: Int) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(width = 4.dp, height = 18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(iyal.paal.brush),
            )
            Spacer(Modifier.width(10.dp))
            Text(iyal.tamil, style = MaterialTheme.typography.titleSmall)
            Text(
                "  •  ${iyal.paal.tamil}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Text(
                pluralStringResource(R.plurals.chapters_count, count, count),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ChapterRow(
    chapter: Chapter,
    isCurrent: Boolean,
    isPlaying: Boolean,
    offline: OfflineState,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .widthIn(max = ContentMaxWidth)
            .fillMaxWidth()
            .background(if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChapterBadge(chapter, isCurrent = isCurrent, isPlaying = isPlaying)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Text(
                chapter.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${stringResource(R.string.kural_range, chapter.kuralRange)}  •  " +
                    "${formatTime(chapter.durationSec * 1000L)}  •  ${chapter.transliteration}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DownloadButton(
            number = chapter.number,
            offline = offline,
            onDownload = onDownload,
            onCancel = onCancel,
            onRemove = onRemove,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ConfirmDialog(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon, contentDescription = null) },
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit, onPrivacyPolicy: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Image(
                painter = painterResource(R.drawable.artwork),
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp)),
            )
        },
        title = { Text(stringResource(R.string.app_name)) },
        text = {
            Column {
                Text(stringResource(R.string.about_body))
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.publisher),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onPrivacyPolicy,
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    Icon(Icons.Rounded.PrivacyTip, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.privacy_policy))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}
