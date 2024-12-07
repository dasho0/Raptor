package com.example.raptor

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.Metadata
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.ui.util.fastForEach
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.extractor.metadata.flac.PictureFrame
import androidx.media3.extractor.metadata.id3.Id3Decoder
import androidx.media3.extractor.metadata.id3.Id3Frame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.log
import kotlin.reflect.typeOf

//this class handles metadata extraction from a list of music files

class TagExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageManager: ImageManager
) {
    data class SongInfo(
        val artists: List<String>?,
        val albumArtists: List<String>?,
        val title: String?,
        val releaseDate: String?,
        val album: String?,
        val fileUri: Uri?,
        val coverUri: Uri?,
    )

    @OptIn(UnstableApi::class)
    private fun buildSongInfo(metadata: Metadata, uri: Uri?): SongInfo {
        val metadataList = mutableListOf<Metadata.Entry>()
        for(i in 0 until metadata.length()) {
            metadataList.add(metadata.get(i))
        }
        Log.d("${javaClass.simpleName}", "Metadata list: $metadataList")

        when(metadataList[0]) {
            is VorbisComment -> {
                val entryMap: MutableMap<String?, Any?> = mutableMapOf()

                // the last element `picture` screws up the logic and it only has a mimetype value
                // which i think is useless
                metadataList.take(metadataList.size - 1).fastForEach {
                    val entry = it as VorbisComment

                    val key = entry.key
                    val value = entry.value
                    when(key) {
                        "ALBUMARTIST", "ARTIST" -> {
                            if(!entryMap.containsKey(key)) {
                                entryMap[key] = mutableListOf<String?>(value)
                            } else {
                                (entryMap[key] as? MutableList<String?>)?.add(value)
                            }
                        }

                        else -> {
                            // assert(!entryMap.containsKey(key))
                            if(entryMap.containsKey(key)) {
                                Log.w(javaClass.simpleName, "Unhandled duplicate key: $key")
                                return@fastForEach
                            }
                            entryMap[key] = value
                        }
                    }
                }

                val coverUri = imageManager.extractAlbumimage(
                    uri,
                    entryMap["ALBUMARTIST"] as List<String>,
                    entryMap["ALBUM"] as String
                )

                return SongInfo(
                    artists = entryMap["ARTIST"] as? List<String>?,
                    albumArtists = entryMap["ALBUMARTIST"] as? List<String>?,
                    title = entryMap["TITLE"] as? String?,
                    album = entryMap["ALBUM"] as String?,
                    releaseDate = entryMap["DATE"] as? String?,
                    fileUri = uri,
                    coverUri = coverUri,
                )
            }

            is Id3Frame -> {
                metadataList.fastForEach {
                    val entry = it as Id3Frame
                    Log.d(javaClass.simpleName, "Id3 metadata: $entry")

                }

                return TODO("Implement id3 return")
            }

            else -> {
                metadataList.fastForEach {
                    Log.w(
                        javaClass.simpleName,
                        "Unhendled tag format: ${it::class.simpleName}, metadata: $it"
                    )
                }

                return SongInfo(
                    null, null, null, null, null, null, null
                )
            }
        }
    }


    @OptIn(UnstableApi::class)
    fun extractTags(fileList: List<MusicFileLoader.SongFile>): List<Any> {

        val tagsList = mutableListOf<SongInfo>()

        for(file in fileList) {
            val mediaItem = MediaItem.fromUri("${file.uri}")

// Retrieve metadata asynchronously
            //FIXME: probably shouldnt block the thread with get() every time a song is scanned
            // but whatever, im not syncing this thing manually
            val trackGroups = MetadataRetriever.retrieveMetadata(context, mediaItem).get()

            if(trackGroups != null) {
                // Parse and handle metadata
                assert(trackGroups.length == 1)

                val tags = trackGroups[0]
                    .getFormat(0)
                    .metadata
                    .let {
                        buildSongInfo(
                            it!!,
                            file.uri
                        )
                    } // assuming that a file without metadata
                // is invalid, so forcing a crash here is not a bad idea

                tagsList.add(tags)
            }
        }
        return tagsList
    }
}