package com.example.raptor

import androidx.compose.runtime.Composable

// This class is responsible - for now - for the initial loading and processing of music files
// into the library

object LibraryManager {
    private var picker = MusicFileLoader()

    @Composable fun prepareFilePicker() {
        picker.PrepareFilePicker()
    }

    fun pickFiles() {
        picker.launch()
    }

    // FIXME: temporary, delete later
    suspend fun getFileList(): List<MusicFileLoader.SongFile> {
        return picker.getSongFiles()
    }
}