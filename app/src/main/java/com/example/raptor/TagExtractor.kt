package com.example.raptor

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log

//this class handles metadata extraction from a list of music files

class TagExtractor(private val fileList: List<MusicFileLoader.SongFile>, private val context:
Context) {
    data class SongTags(
        val artist: String?, // will have to handle multiple artist on a single song somewhere
        val title: String?,
        val releaseYear: String?, //TODO: seems like you can't obtain the release date with MediaMetadataExtractor, only the modified date of the file wtf
        val album: String?,
    )

    private val extractor: MediaMetadataRetriever = MediaMetadataRetriever()
    var songTagsList = mutableListOf<SongTags>()
        private set

    init {
        Log.d("TagExtractor", "-TEST-")
        for(file in fileList) {
            if(file.mimeType == "vnd.android.document/directory") {
                Log.d("${TagExtractor::class.simpleName}", "Directory handling not implemented " +
                        "yet") //TODO: change all logging to be like that
                continue
            }

            extractor.setDataSource(context.contentResolver.openFileDescriptor(file.uri, "r")?.fileDescriptor)
            songTagsList.add(SongTags(
                artist = extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                title = extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                releaseYear = extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR),
                album = extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
            ))

            Log.d("TagExtractor", "Extracting from ${file.filename}: ${songTagsList.last()}")
        }

        extractor.release()
    }
}