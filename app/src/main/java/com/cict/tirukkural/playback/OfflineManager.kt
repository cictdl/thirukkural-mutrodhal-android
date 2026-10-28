package com.cict.tirukkural.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.ContentMetadata
import com.cict.tirukkural.data.Chapter
import com.cict.tirukkural.data.Tirukkural
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.ConcurrentHashMap

data class OfflineState(
    /** Chapters whose full recording is in the cache. */
    val downloaded: Set<Int> = emptySet(),
    /** Chapters being saved: 0..1 while transferring, [QUEUED] while waiting for a slot. */
    val progress: Map<Int, Float> = emptyMap(),
    val usedBytes: Long = 0L,
) {
    companion object {
        const val QUEUED = -1f
    }
}

/** Saves recitations into [AudioCache] so they can be played without a connection. */
@OptIn(UnstableApi::class)
class OfflineManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache get() = AudioCache.get(context)
    private val slots = Semaphore(PARALLEL_DOWNLOADS)
    private val jobs = ConcurrentHashMap<Int, Job>()
    private val writers = ConcurrentHashMap<Int, CacheWriter>()

    private val _state = MutableStateFlow(OfflineState())
    val state: StateFlow<OfflineState> = _state.asStateFlow()

    private val _failures = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    /** Emits the chapter number whenever a download fails. */
    val failures: SharedFlow<Int> = _failures.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        scope.launch { recompute() }
    }

    private fun recompute() {
        val done = Tirukkural.chapters.filter(::isFullyCached).mapTo(HashSet()) { it.number }
        _state.update { it.copy(downloaded = done, usedBytes = cache.cacheSpace) }
    }

    private fun isFullyCached(chapter: Chapter): Boolean {
        val key = chapter.mediaId
        val length = ContentMetadata.getContentLength(cache.getContentMetadata(key))
        return length > 0 && cache.isCached(key, 0, length)
    }

    fun isDownloading(number: Int) = jobs.containsKey(number)

    fun download(number: Int) {
        if (jobs.containsKey(number) || number in _state.value.downloaded) return
        setProgress(number, OfflineState.QUEUED)
        val job = scope.launch {
            slots.withPermit {
                val chapter = Tirukkural.chapter(number)
                val spec = DataSpec.Builder()
                    .setUri(chapter.audioUrl)
                    .setKey(chapter.mediaId)
                    .build()
                val writer = CacheWriter(
                    AudioCache.dataSourceFactory(context).createDataSource(),
                    spec,
                    null,
                ) { requestLength, bytesCached, _ ->
                    if (requestLength > 0) setProgress(number, bytesCached.toFloat() / requestLength)
                }
                writers[number] = writer
                setProgress(number, 0f)
                try {
                    writer.cache()
                } catch (_: InterruptedIOException) {
                    // Cancelled by the user.
                } catch (_: IOException) {
                    _failures.tryEmit(number)
                } finally {
                    writers.remove(number)
                }
            }
        }
        jobs[number] = job
        job.invokeOnCompletion {
            jobs.remove(number)
            _state.update { it.copy(progress = it.progress - number) }
            recompute()
        }
    }

    fun downloadAll() {
        val done = _state.value.downloaded
        Tirukkural.chapters.forEach { if (it.number !in done) download(it.number) }
    }

    fun cancel(number: Int) {
        writers[number]?.cancel()
        jobs[number]?.cancel()
    }

    fun cancelAll() {
        jobs.keys.toList().forEach(::cancel)
    }

    fun delete(number: Int) {
        cancel(number)
        scope.launch {
            cache.removeResource(Chapter.mediaIdFor(number))
            recompute()
        }
    }

    fun deleteAll() {
        cancelAll()
        scope.launch {
            cache.keys.toList().forEach { cache.removeResource(it) }
            recompute()
        }
    }

    private fun setProgress(number: Int, value: Float) {
        _state.update { it.copy(progress = it.progress + (number to value)) }
    }

    companion object {
        private const val PARALLEL_DOWNLOADS = 2

        @Volatile
        private var instance: OfflineManager? = null

        fun get(context: Context): OfflineManager =
            instance ?: synchronized(this) {
                instance ?: OfflineManager(context.applicationContext).also { instance = it }
            }
    }
}
