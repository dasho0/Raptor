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

    fun getAlbumsByAuthor(author: String) = databaseManager.fetchAlbumsByAuthor(author)

    fun getSongsByAlbum(album: String) = databaseManager.fetchSongsByAlbum(album)


    private val fileProcessingFlow = picker.songFileList
        .map { fileList -> tagExtractor.extractTags(fileList, context) }
        .onEach {
            databaseManager.populateDatabase(it as List<TagExtractor.SongTags>)
        }
        .flowOn(Dispatchers.IO)

    val libraryState = databaseManager.fetchAllSongs()

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