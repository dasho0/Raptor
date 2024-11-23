package com.example.raptor.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.raptor.AudioPlayer
import com.example.raptor.database.entities.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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

    val progressBarPosition: Flow<Float> = flow {
        while(true) {
            assert((audioPlayer.currentPosition / audioPlayer.currentDuration) <= 1)
            emit(audioPlayer.currentPosition.toFloat() / audioPlayer.currentDuration.toFloat())
            delay(200)
        }
    }

    val buttonText = audioPlayer.playbackState
        .map { state -> String
            when(state) {
                AudioPlayer.PlaybackStates.STATE_BUFFERING -> {
                    String()
                }
                AudioPlayer.PlaybackStates.STATE_ENDED -> {
                    "Powtórz piosenkę"
                }
                AudioPlayer.PlaybackStates.STATE_IDLE -> {
                    "Zagraj piosenkę"
                }
                AudioPlayer.PlaybackStates.STATE_READY -> {
                    String()
                }
                AudioPlayer.PlaybackStates.STATE_PLAYING -> {
                    "Zapauzuj piosenkę"
                }
                AudioPlayer.PlaybackStates.STATE_PAUSED -> {
                    "Odpauzuj piosenkę"
                }
            }
        }

    fun onProgressBarMoved(tapPosition: Float) {
        if(audioPlayer.playbackState.value == AudioPlayer.PlaybackStates.STATE_IDLE) {
            throw(NotImplementedError())
        }

        assert(tapPosition * audioPlayer.currentDuration <= audioPlayer.currentDuration)

        audioPlayer.changeCurrentPosition((tapPosition * audioPlayer.currentDuration).toLong())
    }

    fun playPauseRestartSong(song: Song) {
        if(
            audioPlayer.playbackState.value == AudioPlayer.PlaybackStates.STATE_IDLE ||
            audioPlayer.playbackState.value == AudioPlayer.PlaybackStates.STATE_PAUSED
        )
            audioPlayer.playSong(tempSong)
        else if(audioPlayer.playbackState.value == AudioPlayer.PlaybackStates.STATE_PLAYING)
            audioPlayer.pause()
        else
            audioPlayer.restartCurrentPlayback()
    }
}