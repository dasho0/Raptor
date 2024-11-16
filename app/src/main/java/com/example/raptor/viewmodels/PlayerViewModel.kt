package com.example.raptor.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import com.example.raptor.AudioPlayer
import com.example.raptor.database.entities.Song

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext
    private val audioPlayer = AudioPlayer(context)

    private val temp_uri = "content://com.android.externalstorage.documents/tree/14ED-2303%3AMusic/document/14ED-2303%3AMusic%2F06.%20Knife's%20Edge.flac"

    fun playSong(song: Song) {
        // audioPlayer.playUri(song.fileUri?.toUri())
        audioPlayer.playUri(temp_uri.toUri())
    }
}