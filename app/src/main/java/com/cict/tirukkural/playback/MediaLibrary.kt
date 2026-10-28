package com.cict.tirukkural.playback

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import com.cict.tirukkural.R
import com.cict.tirukkural.data.Chapter
import com.cict.tirukkural.data.Tirukkural

object MediaLibrary {

    fun artworkUri(context: Context): Uri {
        val res = context.resources
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.packageName)
            .appendPath(res.getResourceTypeName(R.drawable.artwork))
            .appendPath(res.getResourceEntryName(R.drawable.artwork))
            .build()
    }

    fun mediaItem(context: Context, chapter: Chapter): MediaItem =
        MediaItem.Builder()
            .setMediaId(chapter.mediaId)
            .setUri(chapter.audioUrl)
            .setCustomCacheKey(chapter.mediaId)
            .setMimeType(MimeTypes.AUDIO_MPEG)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(chapter.title)
                    .setDisplayTitle(chapter.title)
                    .setArtist("அதிகாரம் ${chapter.number} • ${chapter.paal.tamil}")
                    .setAlbumTitle("திருக்குறள் முற்றோதல்")
                    .setAlbumArtist("திருவள்ளுவர்")
                    .setSubtitle("${chapter.iyal.tamil} • குறள் ${chapter.kuralRange}")
                    .setTrackNumber(chapter.number)
                    .setTotalTrackCount(Tirukkural.CHAPTER_COUNT)
                    .setArtworkUri(artworkUri(context))
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER)
                    .build(),
            )
            .build()

    fun allItems(context: Context): List<MediaItem> =
        Tirukkural.chapters.map { mediaItem(context, it) }

    /** Rebuilds a playable item from a bare media id sent by a controller. */
    fun resolve(context: Context, item: MediaItem): MediaItem {
        val number = Chapter.numberFromMediaId(item.mediaId) ?: return item
        return mediaItem(context, Tirukkural.chapter(number))
    }
}
