package com.example.raptor

import android.content.Context
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.exoplayer.source.TrackGroupArray
import kotlinx.coroutines.flow.MutableStateFlow
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.FutureCallback
import java.util.concurrent.Executors

//this class handles metadata extraction from a list of music files

class TagExtractor() {
    data class SongTags(
        val artist: String?, // will have to handle multiple artist on a single song somewhere
        val title: String?,
        val releaseYear: String?, //TODO: seems like you can't obtain the release date with MediaMetadataExtractor, only the modified date of the file wtf
        val album: String?,
)

    private val extractor: MediaMetadataRetriever = MediaMetadataRetriever()

    @OptIn(UnstableApi::class)
    fun extractTags(fileList: List<MusicFileLoader.SongFile>, context: Context): List<SongTags> {
        Log.d("${javaClass.simpleName}", "-TEST-")

        for(file in fileList) {
            val mediaItem = MediaItem.fromUri("${file.uri}")
            val executor = Executors.newSingleThreadExecutor()

// Retrieve metadata asynchronously
            //FIXME: probably shouldnt block the thread with get() every time a song is scanned
            // but whatever, im not syncing this thing manually
            val trackGroups = MetadataRetriever.retrieveMetadata(context, mediaItem).get()

            if (trackGroups != null) {
                // Parse and handle metadata
                Log.d("${javaClass.simpleName}", "handling $trackGroups")
                assert(trackGroups.length == 1)

                val metadata = trackGroups[0]
                    .getFormat(0)
                    .metadata

                metadata?.let {
                    val metadataList = mutableListOf<String>()
                    for(i in 0 until it.length()) {
                        metadataList.add(it.get(i).toString())
                    }

                    Log.d("${javaClass.simpleName}", "Metadata list: $metadataList")
                }
            }
        }
        // return tagsList
        return emptyList()
    }
}