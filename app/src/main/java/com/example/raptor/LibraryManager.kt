package com.example.raptor

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

object LibraryManager {
    private var picker = MusicFileLoader()
    private var tagExtractor = TagExtractor()

    private fun obtainTags(fileList: List<MusicFileLoader.SongFile>, context: Context) {
        tagExtractor.extractTags(fileList, context)
    }

    @Composable
    fun PrepareFilePicker(context: Context) {
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
        return tagExtractor.getUniqueAlbums()
    }
    fun getAllTags(): List<TagExtractor.SongTags> {
        return tagExtractor.songTagsList
    }
}