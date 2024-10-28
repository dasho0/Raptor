package com.example.raptor

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// This class is responsible - for now - for the initial loading and processing of music files
// into the library

object LibraryManager {
    private var picker = MusicFileLoader()
    lateinit var tagExtractor: TagExtractor //FIXME: this leaks memory lmao

    private fun obtainTags(fileList: List<MusicFileLoader.SongFile>, context: Context) {
        tagExtractor = TagExtractor(fileList, context)
    }

    @Composable fun prepareFilePicker(context: Context) { //TODO: refer to MusicFileLoader
        // .PrepareFilePicker()
        picker.PrepareFilePicker(context)
    }

    fun pickFiles() {
        picker.launch()
    }

    suspend fun processFiles(context: Context) {
        val files = picker.getSongFiles()
        obtainTags(files, context)
    }

    // FIXME: temporary, delete later
    suspend fun getFileList(): List<MusicFileLoader.SongFile> {
        return picker.getSongFiles()
    }
}