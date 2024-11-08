package com.example.raptor

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class LibraryViewModel(application: Application): AndroidViewModel(application) {
    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext //this is bad practice
    // i think
    private val picker = MusicFileLoader(context)
    private val tagExtractor = TagExtractor()
    private val databaseManager = DatabaseManager(context)

    private val fileProcessingFlow = picker.songFileList
        .map { fileList -> tagExtractor.extractTags(fileList, context) }
        .onEach {
            databaseManager.populateDatabase(it)
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
    }
}