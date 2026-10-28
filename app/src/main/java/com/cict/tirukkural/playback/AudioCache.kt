package com.cict.tirukkural.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.cict.tirukkural.BuildConfig
import java.io.File

/**
 * One process-wide cache for the recitations. Everything that is streamed is written here, so a
 * chapter heard once (or downloaded explicitly) plays again without a network connection.
 * The whole collection is ~536 MB, so the cache is bounded without an evictor; users can clear it
 * from the menu. It lives in noBackupFilesDir so it never counts towards Auto Backup.
 */
@OptIn(UnstableApi::class)
object AudioCache {
    @Volatile
    private var cache: SimpleCache? = null

    fun get(context: Context): SimpleCache =
        cache ?: synchronized(this) {
            cache ?: SimpleCache(
                File(context.noBackupFilesDir, "recitations"),
                NoOpCacheEvictor(),
                StandaloneDatabaseProvider(context.applicationContext),
            ).also { cache = it }
        }

    private fun httpDataSourceFactory(): DefaultHttpDataSource.Factory =
        DefaultHttpDataSource.Factory()
            .setUserAgent("TirukkuralMutrodhal/${BuildConfig.VERSION_NAME} (Android)")
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)

    fun dataSourceFactory(context: Context): CacheDataSource.Factory =
        CacheDataSource.Factory()
            .setCache(get(context))
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context, httpDataSourceFactory()))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
}
