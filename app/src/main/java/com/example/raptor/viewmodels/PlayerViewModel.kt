package com.example.raptor.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.example.raptor.AudioPlayer
import com.example.raptor.database.entities.Song

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext
    private val audioPlayer = AudioPlayer(context)

    private val tempSong = Song(
        songId = 0L,
        title = "Test123",
        albumId = null,
        fileUri = "content://com.android.externalstorage.documents/tree/14ED-2303%3AMusic/document/14ED-2303%3AMusic%2F06.%20Knife's%20Edge.flac"
    )

    private val isPlaying get() = audioPlayer.isPlayingInternal
    val isPlayingUI: State<Boolean> = audioPlayer.uiState.isPlaying

    fun playPauseSong(song: Song) {
        assert(isPlaying == audioPlayer.isPlayingInternal)
        if(!isPlaying) audioPlayer.playSong(tempSong) else audioPlayer.pause()
    }
}