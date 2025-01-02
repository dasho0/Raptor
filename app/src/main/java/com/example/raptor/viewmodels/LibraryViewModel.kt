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
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Viewmodel presenting the starting, author screen. Also handles user interaction with the file
 * picker
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val picker: MusicFileLoader,
    private val databaseManager: DatabaseManager,
    private val tagExtractor: TagExtractor,
) : ViewModel() {

    private val _folderSelected = mutableStateOf(false)

    /**
     * Folder selected by the user with the file picker
     */
    val folderSelected: State<Boolean> get() = _folderSelected

    /**
     * All authors in the database. Used for displaying author tiles
     */
    val authors = databaseManager.collectAuthorsFlow()

    /**
     * Gets a list of songs in a given album
     *
     * @param albumId Id of the album
     * @return Flow of a list of `Song`
     */
    fun getSongsByAlbum(albumId: Long) = databaseManager.collectSongsByAlbumFlow(albumId)

    private val fileProcessingFlow = picker.songFileList
        .map { fileList -> tagExtractor.extractTags(fileList) }
        .onEach {
            databaseManager.populateDatabase(it as List<TagExtractor.SongInfo>)
        }
        .flowOn(Dispatchers.IO)

    /**
     * Initialize the flow used by the file picker
     */
    init {
        viewModelScope.launch {
            fileProcessingFlow.collect {
                Log.d("${javaClass.simpleName}", "Collecting flow from file picker")
            }
        }
    }

    /**
     * @see  MusicFileLoader.PrepareFilePicker
     */
    @Composable
    fun PrepareFilePicker() {
        picker.PrepareFilePicker()
    }

    /**
     * Launch the file picker
     */
    fun pickFiles() {
        picker.launch()
        _folderSelected.value = true // Persist folder selection
    }
}