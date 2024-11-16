package com.example.raptor

import android.content.Context
import android.net.Uri
import androidx.media3.common.Metadata
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.extractor.metadata.vorbis.VorbisComment

//this class handles metadata extraction from a list of music files

class TagExtractor() {
    data class SongInfo(
        val artists: List<String>?,
        val albumArtists: List<String>?,
        val title: String?,
        val releaseDate: String?,
        val album: String?,
        val fileUri: Uri?
)

    @OptIn(UnstableApi::class)
    private fun buildSongInfo(metadata: Metadata, uri: Uri?): SongInfo {
        return metadata.let {
            val metadataList = mutableListOf<Metadata.Entry>()
            for(i in 0 until it.length()) {
                metadataList.add(it.get(i))
            }
            Log.d("${javaClass.simpleName}", "Metadata list: $metadataList")

            // the last element `picture` screws up the logic and it only has a mimetype value
            // which i think is useless
            val entryMap: MutableMap<String?, Any?> = mutableMapOf()

            for(_entry in metadataList.take(metadataList.size - 1)) {
                val entry = _entry as? VorbisComment
                val key = entry?.key
                val value = entry?.value

                when(key) {
                    "ALBUMARTIST", "ARTIST" -> {
                        if(!entryMap.containsKey(key)) {
                            entryMap[key] = mutableListOf<String?>(value)
                        } else {
                            (entryMap[key] as? MutableList<String?>)?.add(value)
                        }
                    }

                    else -> {
                        assert(!entryMap.containsKey(key))
                        entryMap[key] = value
                    }
                }
            }

            SongInfo(
                artists = entryMap["ARTIST"] as? List<String>?,
                albumArtists = entryMap["ALBUMARTIST"] as? List<String>?,
                title = entryMap["TITLE"] as? String?,
                album = entryMap["ALBUM"] as String?,
                releaseDate = entryMap["DATE"] as? String?,
                fileUri = uri
            )
        }
    }

    @OptIn(UnstableApi::class)
    fun extractTags(fileList: List<MusicFileLoader.SongFile>, context: Context): List<Any> {

        val tagsList = mutableListOf<SongInfo>()

        for(file in fileList) {
            val mediaItem = MediaItem.fromUri("${file.uri}")

// Retrieve metadata asynchronously
            //FIXME: probably shouldnt block the thread with get() every time a song is scanned
            // but whatever, im not syncing this thing manually
            val trackGroups = MetadataRetriever.retrieveMetadata(context, mediaItem).get()

            if (trackGroups != null) {
                // Parse and handle metadata
                assert(trackGroups.length == 1)

                val tags = trackGroups[0]
                    .getFormat(0)
                    .metadata
                    .let { buildSongInfo(it!!, file.uri) } // assuming that a file without metadata
                // is invalid, so forcing a crash here is not a bad idea

                tagsList.add(tags)
            }
        }
        return tagsList
    }
}