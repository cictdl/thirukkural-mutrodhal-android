package com.cict.tirukkural.ui

import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.cict.tirukkural.data.Chapter
import com.cict.tirukkural.data.Tirukkural
import com.cict.tirukkural.playback.OfflineManager
import com.cict.tirukkural.playback.OfflineState
import com.cict.tirukkural.playback.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val connected: Boolean = false,
    val chapterNumber: Int = 1,
    val isPlaying: Boolean = false,
    val playWhenReady: Boolean = false,
    val isBuffering: Boolean = false,
    val durationMs: Long = Tirukkural.chapter(1).durationSec * 1000L,
    val speed: Float = 1f,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffle: Boolean = false,
    val hasError: Boolean = false,
    val sleepAtWallMs: Long = 0L,
    val sleepAtEndOfChapter: Boolean = false,
) {
    val chapter: Chapter get() = Tirukkural.chapter(chapterNumber)
    val sleepTimerActive: Boolean get() = sleepAtEndOfChapter || sleepAtWallMs > 0L
}

data class PlaybackPosition(val positionMs: Long = 0L, val bufferedMs: Long = 0L)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val offlineManager = OfflineManager.get(application)
    val offline: StateFlow<OfflineState> = offlineManager.state
    val downloadFailures: SharedFlow<Int> = offlineManager.failures

    private val _ui = MutableStateFlow(PlayerUiState())
    val ui: StateFlow<PlayerUiState> = _ui.asStateFlow()

    private val _position = MutableStateFlow(PlaybackPosition())
    val position: StateFlow<PlaybackPosition> = _position.asStateFlow()

    private val controllerFuture: ListenableFuture<MediaController>
    private var controller: MediaController? = null
    private var ticker: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            sync()
            if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_PLAYBACK_STATE_CHANGED)) {
                // A chapter that just finished streaming is now fully cached.
                offlineManager.refresh()
            }
        }
    }

    private val controllerListener = object : MediaController.Listener {
        override fun onExtrasChanged(controller: MediaController, extras: Bundle) = readExtras(extras)
    }

    init {
        val token = SessionToken(application, ComponentName(application, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(application, token)
            .setListener(controllerListener)
            .buildAsync()
        viewModelScope.launch {
            val connected = controllerFuture.await()
            controller = connected
            connected.addListener(playerListener)
            readExtras(connected.sessionExtras)
            sync()
        }
    }

    override fun onCleared() {
        controller?.removeListener(playerListener)
        MediaController.releaseFuture(controllerFuture)
        controller = null
    }

    private fun sync() {
        val c = controller ?: return
        val number = Chapter.numberFromMediaId(c.currentMediaItem?.mediaId) ?: _ui.value.chapterNumber
        val duration = c.duration.takeIf { it != C.TIME_UNSET && it > 0 }
            ?: (Tirukkural.chapter(number).durationSec * 1000L)
        _ui.update {
            it.copy(
                connected = true,
                chapterNumber = number,
                isPlaying = c.isPlaying,
                playWhenReady = c.playWhenReady,
                isBuffering = c.playWhenReady && c.playbackState == Player.STATE_BUFFERING,
                durationMs = duration,
                speed = c.playbackParameters.speed,
                repeatMode = c.repeatMode,
                shuffle = c.shuffleModeEnabled,
                hasError = c.playerError != null,
            )
        }
        updatePosition()
        if (c.isPlaying) startTicker() else stopTicker()
    }

    private fun updatePosition() {
        val c = controller ?: return
        _position.value = PlaybackPosition(
            positionMs = c.currentPosition.coerceAtLeast(0L),
            bufferedMs = c.bufferedPosition.coerceAtLeast(0L),
        )
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = viewModelScope.launch {
            while (isActive) {
                updatePosition()
                delay(250)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun readExtras(extras: Bundle) {
        _ui.update {
            it.copy(
                sleepAtWallMs = extras.getLong(PlaybackService.EXTRA_SLEEP_AT_WALL_MS, 0L),
                sleepAtEndOfChapter = extras.getBoolean(PlaybackService.EXTRA_SLEEP_END_OF_CHAPTER, false),
            )
        }
    }

    private fun MediaController.ensureReady() {
        if (mediaItemCount != Tirukkural.CHAPTER_COUNT) {
            // The session fills in URIs and metadata from the media id.
            setMediaItems(Tirukkural.chapters.map { MediaItem.Builder().setMediaId(it.mediaId).build() })
        }
        if (playbackState == Player.STATE_IDLE) prepare()
    }

    // region Player actions

    fun playChapter(number: Int) {
        val c = controller ?: return
        c.ensureReady()
        c.seekTo(number - 1, 0L)
        c.play()
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying || (c.playWhenReady && c.playbackState == Player.STATE_BUFFERING)) {
            c.pause()
        } else {
            c.ensureReady()
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(c.currentMediaItemIndex, 0L)
            c.play()
        }
    }

    fun retry() {
        val c = controller ?: return
        c.ensureReady()
        c.play()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _position.update { it.copy(positionMs = positionMs) }
    }

    fun seekBack() = controller?.seekBack()

    fun seekForward() = controller?.seekForward()

    fun next() = controller?.seekToNextMediaItem()

    fun previous() = controller?.seekToPrevious()

    fun setSpeed(speed: Float) = controller?.setPlaybackSpeed(speed)

    fun cycleRepeatMode() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun setSleepTimer(minutes: Int, endOfChapter: Boolean = false) {
        controller?.sendCustomCommand(
            SessionCommand(PlaybackService.COMMAND_SLEEP_TIMER, Bundle.EMPTY),
            Bundle().apply {
                putInt(PlaybackService.ARG_MINUTES, minutes)
                putBoolean(PlaybackService.ARG_END_OF_CHAPTER, endOfChapter)
            },
        )
    }

    // endregion

    // region Offline actions

    fun download(number: Int) = offlineManager.download(number)

    fun cancelDownload(number: Int) = offlineManager.cancel(number)

    fun removeDownload(number: Int) = offlineManager.delete(number)

    fun downloadAll() = offlineManager.downloadAll()

    fun removeAllDownloads() = offlineManager.deleteAll()

    fun refreshOffline() = offlineManager.refresh()

    // endregion
}
