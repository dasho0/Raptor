package com.example.raptor

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LibraryViewModel(application: Application): AndroidViewModel(application) {
    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext //this is bad practice
    // i think
    private var picker = MusicFileLoader(context)
    private var tagExtractor = TagExtractor()

    val libraryState = picker.songFileList.map { file ->
        tagExtractor.extractTags(file, context)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList<TagExtractor.SongTags>()
    )

    @Composable
    fun PrepareFilePicker(context: Context) {
        picker.PrepareFilePicker()
    }

    fun pickFiles() {
        picker.launch()
    }
}