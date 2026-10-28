package com.cict.tirukkural.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.core.content.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.cict.tirukkural.MainActivity
import com.cict.tirukkural.R
import com.cict.tirukkural.data.Chapter
import com.cict.tirukkural.data.Tirukkural
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Hosts the player in a foreground media service so recitation continues with the screen off,
 * from the notification, lock screen, Bluetooth and headset buttons. It also owns the sleep timer
 * and remembers where the listener stopped.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val handler = Handler(Looper.getMainLooper())

    private var sleepAtElapsed = 0L
    private var sleepAtEndOfChapter = false

    private val sleepRunnable = Runnable {
        player.pause()
        clearSleepTimer()
    }

    private val saveRunnable = object : Runnable {
        override fun run() {
            saveState()
            if (player.isPlaying) handler.postDelayed(this, SAVE_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(AudioCache.dataSourceFactory(this)))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setSeekBackIncrementMs(SEEK_INCREMENT_MS)
            .setSeekForwardIncrementMs(SEEK_INCREMENT_MS)
            .build()
        player.addListener(playerListener)
        restoreState()

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(MainActivity.EXTRA_OPEN_PLAYER, true),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .setCallback(SessionCallback())
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().apply {
                setSmallIcon(R.drawable.ic_stat_recitation)
            },
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        saveState()
        handler.removeCallbacksAndMessages(null)
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_PLAYBACK_PARAMETERS_CHANGED,
                    Player.EVENT_REPEAT_MODE_CHANGED,
                    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                )
            ) {
                saveState()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            handler.removeCallbacks(saveRunnable)
            if (isPlaying) handler.postDelayed(saveRunnable, SAVE_INTERVAL_MS) else saveState()
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM && sleepAtEndOfChapter) {
                clearSleepTimer()
            }
        }
    }

    // region Sleep timer

    private fun setSleepTimer(minutes: Int, endOfChapter: Boolean) {
        handler.removeCallbacks(sleepRunnable)
        player.pauseAtEndOfMediaItems = endOfChapter
        sleepAtEndOfChapter = endOfChapter
        sleepAtElapsed = 0L
        if (!endOfChapter && minutes > 0) {
            val delay = minutes * 60_000L
            sleepAtElapsed = SystemClock.elapsedRealtime() + delay
            handler.postDelayed(sleepRunnable, delay)
        }
        publishSessionExtras()
    }

    private fun clearSleepTimer() = setSleepTimer(0, endOfChapter = false)

    private fun publishSessionExtras() {
        val remaining = if (sleepAtElapsed > 0) sleepAtElapsed - SystemClock.elapsedRealtime() else 0L
        session?.setSessionExtras(
            Bundle().apply {
                // Wall-clock time so controllers can show a countdown without sharing a clock base.
                putLong(EXTRA_SLEEP_AT_WALL_MS, if (remaining > 0) System.currentTimeMillis() + remaining else 0L)
                putBoolean(EXTRA_SLEEP_END_OF_CHAPTER, sleepAtEndOfChapter)
            },
        )
    }

    // endregion

    // region Persistence

    private val prefs by lazy { getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    private fun saveState() {
        if (player.mediaItemCount == 0) return
        val number = Chapter.numberFromMediaId(player.currentMediaItem?.mediaId) ?: return
        prefs.edit {
            putInt(KEY_CHAPTER, number)
            putLong(KEY_POSITION, player.currentPosition.coerceAtLeast(0L))
            putFloat(KEY_SPEED, player.playbackParameters.speed)
            putInt(KEY_REPEAT, player.repeatMode)
            putBoolean(KEY_SHUFFLE, player.shuffleModeEnabled)
        }
    }

    private fun savedStart(): MediaItemsWithStartPosition {
        val number = prefs.getInt(KEY_CHAPTER, 1).coerceIn(1, Tirukkural.CHAPTER_COUNT)
        val position = prefs.getLong(KEY_POSITION, 0L).coerceAtLeast(0L)
        return MediaItemsWithStartPosition(MediaLibrary.allItems(this), number - 1, position)
    }

    /** Loads the whole recitation queue at the last position, without starting playback. */
    private fun restoreState() {
        val start = savedStart()
        player.setMediaItems(start.mediaItems, start.startIndex, start.startPositionMs)
        player.setPlaybackSpeed(prefs.getFloat(KEY_SPEED, 1f))
        player.repeatMode = prefs.getInt(KEY_REPEAT, Player.REPEAT_MODE_OFF)
        player.shuffleModeEnabled = prefs.getBoolean(KEY_SHUFFLE, false)
    }

    // endregion

    private inner class SessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val result = MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
            // Only this app's own UI may set the sleep timer.
            if (controller.packageName == packageName) {
                result.setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                        .add(SessionCommand(COMMAND_SLEEP_TIMER, Bundle.EMPTY))
                        .build(),
                )
            }
            return result.build()
        }

        override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
            publishSessionExtras()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == COMMAND_SLEEP_TIMER) {
                setSleepTimer(
                    minutes = args.getInt(ARG_MINUTES, 0),
                    endOfChapter = args.getBoolean(ARG_END_OF_CHAPTER, false),
                )
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> =
            Futures.immediateFuture(
                mediaItems.map { MediaLibrary.resolve(this@PlaybackService, it) }.toMutableList(),
            )

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean,
        ): ListenableFuture<MediaItemsWithStartPosition> = Futures.immediateFuture(savedStart())
    }

    companion object {
        const val COMMAND_SLEEP_TIMER = "com.cict.tirukkural.SLEEP_TIMER"
        const val ARG_MINUTES = "minutes"
        const val ARG_END_OF_CHAPTER = "end_of_chapter"
        const val EXTRA_SLEEP_AT_WALL_MS = "sleep_at_wall_ms"
        const val EXTRA_SLEEP_END_OF_CHAPTER = "sleep_end_of_chapter"

        const val SEEK_INCREMENT_MS = 10_000L
        private const val SAVE_INTERVAL_MS = 5_000L

        private const val PREFS = "playback"
        private const val KEY_CHAPTER = "chapter"
        private const val KEY_POSITION = "position"
        private const val KEY_SPEED = "speed"
        private const val KEY_REPEAT = "repeat"
        private const val KEY_SHUFFLE = "shuffle"
    }
}
