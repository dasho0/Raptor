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
import com.example.raptor.database.entities.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.log
import kotlin.reflect.typeOf

/**
 * This class handles metadata extraction from a collection of `SongFile`. It will also copy all
 * album covers to app storage
 */

class TagExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageManager: ImageManager
) {
    /**
     * Dataclass representing the tags a song has.
     *
     * @param artists A list of artist names that authored the song
     * @param album A list of artist names that authored the album of the song
     * @param title The title of the song
     * @param releaseDate A string representing the release date of the song
     * @param albumArtists The title of the album of the song
     * @param fileUri `Uri` of the song
     * @param coverUri `Uri` of the song cover art, in app storage
     */
    data class SongInfo(
        val artists: List<String>?,
        val albumArtists: List<String>,
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
                fun handleMissingTags(map: MutableMap<String?, Any?>) {
                    if(map["ALBUMARTIST"] == null) map["ALBUMARTIST"] = List<String>(1,{"Unknown"} )
                }

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

                handleMissingTags(entryMap)

                val coverUri = imageManager.extractAlbumimage(
                    uri,
                    entryMap["ALBUMARTIST"] as List<String>,
                    entryMap["ALBUM"] as String
                )

                return SongInfo(
                    artists = entryMap["ARTIST"] as? List<String>?,
                    albumArtists = entryMap["ALBUMARTIST"] as List<String>,
                    title = entryMap["TITLE"] as? String?,
                    album = entryMap["ALBUM"] as String?,
                    releaseDate = entryMap["DATE"] as? String?,
                    fileUri = uri,
                    coverUri = coverUri,
                )
            }

            is Id3Frame -> {
                fun handleMissingTags(map: MutableMap<String, List<String>>) {
                    if(map["TPE2"] == null) map["TPE2"] = List<String>(1,{"Unknown"} )
                }

                val entryMap: MutableMap<String, List<String>> = mutableMapOf()
                metadataList.fastForEach {
                    val entry = it as Id3Frame
                    Log.d(javaClass.simpleName, "Id3 metadata: $entry")

                    when(entry) {
                        is TextInformationFrame -> {
                            entryMap[entry.id] = entry.values
                        }

                        else -> {
                            Log.w(javaClass.simpleName, "Unimplemented id3 frame: $entry")
                        }
                    }
                }

                handleMissingTags(entryMap)

                val coverUri = imageManager.extractAlbumimage(
                    uri,
                   entryMap["TPE2"]?: emptyList(),
                    entryMap["TALB"]?.get(0).toString()

                )

                return SongInfo(
                    artists = entryMap["TPE1"],
                    albumArtists = entryMap["TPE2"] as List<String>,
                    title = entryMap["TIT2"]?.get(0),
                    releaseDate = entryMap["TDA"]?.get(0),
                    album = entryMap["TALB"]?.get(0),
                    fileUri = uri,
                    coverUri = coverUri
                )
            }

            else -> {
                metadataList.fastForEach {
                    Log.w(
                        javaClass.simpleName,
                        "Unhendled tag format: ${it::class.simpleName}, metadata: $it"
                    )
                }

                return SongInfo(
                    null, mutableListOf("Unknown"), null, null, null, null, null
                )
            }
        }
    }

    /**
     * Extracts the tags and album covers to app storage of a list of `SongFile`
     *
     * @param fileList List of `SongFile`, each element should have it's data extracted
     *
     * @return List of `SongInfo` corresponding to each input song file
     */
    @OptIn(UnstableApi::class)
    fun extractTags(fileList: List<MusicFileLoader.SongFile>): List<SongInfo> {
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