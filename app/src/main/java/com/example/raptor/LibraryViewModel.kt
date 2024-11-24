package com.example.raptor

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.raptor.database.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.raptor.database.entities.Album
import kotlinx.coroutines.flow.Flow

class LibraryViewModel(application: Application): AndroidViewModel(application) {
    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext //this is bad practice
    // i think
    private val picker = MusicFileLoader(context)
    private val tagExtractor = TagExtractor()
    private val databaseManager = DatabaseManager(context)

    private val _folderSelected = mutableStateOf(false)
    val folderSelected: State<Boolean> get() = _folderSelected

    val authors = databaseManager.fetchAuthors()

    fun getAlbumsByAuthor(author: String): Flow<List<Album>> {
        return databaseManager.fetchAlbumsByAuthor(author)
    }

    fun getSongsByAlbum(album: String): Flow<List<com.example.raptor.database.entities.Song>> {
        return databaseManager.fetchSongsByAlbum(album)
    }

    val libraryState = databaseManager.fetchAllSongs()

    init {
        viewModelScope.launch {
            picker.songFileList.collect { fileList ->
                val tags = tagExtractor.extractTags(fileList, context)
                databaseManager.populateDatabase(tags as List<TagExtractor.SongTags>)
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