package com.example.raptor.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
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
import androidx.lifecycle.ViewModel
import com.example.raptor.database.entities.Album
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val picker: MusicFileLoader,
    private val databaseManager: DatabaseManager,
    private val tagExtractor: TagExtractor,
): ViewModel() {

    private val _folderSelected = mutableStateOf(false)
    val folderSelected: State<Boolean> get() = _folderSelected

    val authors = databaseManager.fetchAuthorsFlow()

    fun getAlbumsByAuthor(author: String)= databaseManager.fetchAlbumsByAuthorFlow(author)

    fun getSongsByAlbum(albumId: Long) = databaseManager.fetchSongsByAlbumFlow(albumId)


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