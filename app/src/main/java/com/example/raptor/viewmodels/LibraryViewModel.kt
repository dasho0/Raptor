package com.example.raptor.viewmodels

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.example.raptor.MusicFileLoader
import com.example.raptor.TagExtractor
import com.example.raptor.database.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.example.raptor.ImageManager
import com.example.raptor.database.entities.Album
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val picker: MusicFileLoader,
    private val databaseManager: DatabaseManager,
    private val tagExtractor: TagExtractor,
    private val imageManager: ImageManager
): ViewModel() {

    private val _folderSelected = mutableStateOf(false)
    val folderSelected: State<Boolean> get() = _folderSelected

    val authors = databaseManager.collectAuthorsFlow()

    fun collectAlbumsByAuthor(author: String)= databaseManager.collectAlbumsByAuthorFlow(author)

    fun collectSongsByAlbum(albumId: Long) = databaseManager.collectSongsByAlbumFlow(albumId)

    fun collectAlbumCover(album: Album) = flow<ImageBitmap> {
        imageManager.collectBitmapFromUri(album.coverUri?.toUri())
    }

    private val fileProcessingFlow = picker.songFileList
        .map { fileList -> tagExtractor.extractTags(fileList) }
        .onEach {
            databaseManager.populateDatabase(it as List<TagExtractor.SongInfo>)
        }
        .flowOn(Dispatchers.IO)

    init {
        viewModelScope.launch {
            fileProcessingFlow.collect {
                Log.d("${javaClass.simpleName}", "Collecting flow from file picker")
            }
        }
    }

    @Composable
    fun PrepareFilePicker() {
        picker.PrepareFilePicker()
    }

    fun pickFiles() {
        picker.launch()
        _folderSelected.value = true // Persist folder selection
    }
}