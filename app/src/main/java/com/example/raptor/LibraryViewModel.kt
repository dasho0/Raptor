package com.example.raptor

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val picker = MusicFileLoader(application.applicationContext)
    private val tagExtractor = TagExtractor()

    val songTags = tagExtractor.songTagsList

    init {
        viewModelScope.launch {
            picker.songFileList.collectLatest { files ->
                tagExtractor.extractTags(files, application.applicationContext)
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