package com.example.raptor

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.log
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

//this class handles metadata extraction from a list of music files

class TagExtractor {
    data class SongTags(
        val artist: String?,
        val title: String?,
        val releaseYear: String?,
        val album: String?
    )

    private val extractor = MediaMetadataRetriever()
    private val _songTagsList = MutableStateFlow<List<SongTags>>(emptyList())
    val songTagsList: StateFlow<List<SongTags>> = _songTagsList.asStateFlow()

    fun extractTags(fileList: List<MusicFileLoader.SongFile>, context: Context) {
        val tagsList = fileList.mapNotNull { file ->
            val extractor = MediaMetadataRetriever() // Utworzenie nowej instancji dla każdego pliku
            try {
                val fileDescriptor = context.contentResolver.openFileDescriptor(file.uri, "r")?.fileDescriptor
                if (fileDescriptor != null) {
                    extractor.setDataSource(fileDescriptor)
                    // Pobieranie metadanych
                    SongTags(
                        artist = extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                        title = extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                        releaseYear = extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR),
                        album = extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    )
                } else {
                    Log.e("TagExtractor", "FileDescriptor is null for URI: ${file.uri}")
                    null
                }
            } catch (e: Exception) {
                Log.e("TagExtractor", "Error extracting tags for ${file.filename}", e)
                null
            } finally {
                extractor.release() // Zwalniamy zasoby dla każdej instancji po jej użyciu
            }
        }
        _songTagsList.update { tagsList }
    }
}