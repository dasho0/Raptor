package com.example.raptor

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel

class LibraryViewModel(application: Application): AndroidViewModel(application) {
    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext //this is bad practice
    // i think
    private var picker = MusicFileLoader(context)
    private var tagExtractor = TagExtractor()

    private fun obtainTags(fileList: List<MusicFileLoader.SongFile>, context: Context) {
        tagExtractor.extractTags(fileList, context)
    }

    @Composable
    fun PrepareFilePicker(context: Context) {
        picker.PrepareFilePicker()
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
    fun getAllTags(): List<TagExtractor.SongTags> { //TODO: This is also getting removed
        return tagExtractor.songTagsList
    }
}