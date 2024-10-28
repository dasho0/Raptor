package com.example.raptor

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

object LibraryManager {
    private var picker = MusicFileLoader()
    private lateinit var tagExtractor: TagExtractor
    var isDataLoaded by mutableStateOf(false) // New loading state variable

    private fun obtainTags(fileList: List<MusicFileLoader.SongFile>, context: Context) {
        tagExtractor = TagExtractor(fileList)
        tagExtractor.extractTags(context)
        isDataLoaded = true // Set data as loaded once processing is complete
    }

    @Composable
    fun prepareFilePicker(context: Context) {
        picker.PrepareFilePicker(context)
    }

    fun pickFiles() {
        picker.launch()
    }

    suspend fun processFiles(context: Context) {
        val files = picker.getSongFiles()
        obtainTags(files, context)
    }

    fun getAlbums(): List<TagExtractor.SongTags> {
        return if (isDataLoaded) tagExtractor.getUniqueAlbums() else emptyList()
    }
    fun getAllTags(): List<TagExtractor.SongTags> {
        return if (isDataLoaded) tagExtractor.songTagsList else emptyList()
    }
}