package com.example.raptor.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.lifecycle.AndroidViewModel
import com.example.raptor.AudioPlayer
import com.example.raptor.database.entities.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

// this has to use dependency injection
//TODO: since we now use dep injection we can stop passing application around and rely on
// localContext in each composable function
@HiltViewModel
class PlayerViewModel @Inject constructor (application: Application) : AndroidViewModel(application) {
    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext
    private val audioPlayer = AudioPlayer(context)

    private val tempSong = Song(
        songId = 0L,
        title = "Test123",
        albumId = null,
        fileUri = "content://com.android.externalstorage.documents/tree/14ED-2303%3AMusic/document/14ED-2303%3AMusic%2F06.%20Knife's%20Edge.flac"
    )

    val isPlaying: State<Boolean> = audioPlayer.isPlayingUI

    //TODO: https://gist.github.com/rubenquadros/f40488218b5fce8461b5f39905b3dae5
    val progressBarPosition: Flow<Float> = flow {
        while(true) {
            assert((audioPlayer.currentPosition / audioPlayer.currentDuration) <= 1)
            emit(audioPlayer.currentPosition.toFloat() / audioPlayer.currentDuration.toFloat())

            Log.d(PlayerViewModel::class.simpleName, "Current Seekbar position: ${(audioPlayer
                .currentPosition
                .toFloat() / audioPlayer.currentDuration.toFloat())} ")

            delay(200)
        }
    }

    fun onProgressBarMoved(tapPosition: Float) {
        assert(tapPosition * audioPlayer.currentDuration <= audioPlayer.currentDuration)
        audioPlayer.changeCurrentPosition((tapPosition * audioPlayer.currentDuration).toLong())
    }

    fun playPauseSong(song: Song) {
        if(!isPlaying.value) audioPlayer.playSong(tempSong) else audioPlayer.pause()
    }
}