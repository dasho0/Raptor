package com.example.raptor

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow

//this class handles metadata extraction from a list of music files

class TagExtractor() {
    data class SongTags(
        val artist: String?, // will have to handle multiple artist on a single song somewhere
        val title: String?,
        val releaseYear: String?, //TODO: seems like you can't obtain the release date with MediaMetadataExtractor, only the modified date of the file wtf
        val album: String?,
    )

    private val extractor: MediaMetadataRetriever = MediaMetadataRetriever()

    fun extractTags(fileList: List<MusicFileLoader.SongFile>, context: Context): List<SongTags> {
        Log.d("TagExtractor", "-TEST-")

        var tagsList = mutableListOf<SongTags>()
        for(file in fileList) {
            try {
                extractor.setDataSource(context.contentResolver.openFileDescriptor(file.uri, "r")?.fileDescriptor)
                tagsList.add(SongTags(
                    artist = extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                    title = extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                    releaseYear = extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR),
                    album = extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                ))

                Log.d("TagExtractor", "Extracting from ${file.filename}: ${tagsList.last()}")
            } catch(exception: Exception) {
                Log.e("${TagExtractor::class.simpleName}", "Can't extract tags from file: " +
                        "${file.filename}, type: ${file.mimeType}, uri: ${file.uri}", exception)
            }
        }

        extractor.release()

        return tagsList
    }
}