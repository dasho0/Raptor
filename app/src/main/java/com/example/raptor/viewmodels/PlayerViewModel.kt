package com.example.raptor.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.ui.graphics.vector.ImageVector
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

    private val iconFromState = object {
        private var lastIconState = Icons.Filled.PlayArrow

        // TODO: should probably just turn this into a map
        fun getFrom(state: AudioPlayer.PlaybackStates): ImageVector  {
            when(state) {
                AudioPlayer.PlaybackStates.STATE_BUFFERING,
                AudioPlayer.PlaybackStates.STATE_READY, -> {
                    return lastIconState
                }
                AudioPlayer.PlaybackStates.STATE_ENDED -> {
                    return Icons.Filled.Replay.also {
                        lastIconState = it
                    }
                }
                AudioPlayer.PlaybackStates.STATE_IDLE -> {
                    return Icons.Filled.PlayArrow.also {
                        lastIconState = it
                    }
                }
                AudioPlayer.PlaybackStates.STATE_PLAYING -> {
                    return Icons.Filled.PauseCircleFilled.also {
                        lastIconState = it
                    }
                }
                AudioPlayer.PlaybackStates.STATE_PAUSED -> {
                    return Icons.Filled.PlayArrow.also {
                        lastIconState = it
                    }
                }
            }
        }
    }

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
            delay(33)
        }
    }

    val currentIconImage = audioPlayer.playbackState
        .map {
            iconFromState.getFrom(it)
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
            audioPlayer.playbackState.value == AudioPlayer.PlaybackStates.STATE_PAUSED ||
            audioPlayer.playbackState.value == AudioPlayer.PlaybackStates.STATE_READY
        )
            audioPlayer.playSong(tempSong)
        else if(audioPlayer.playbackState.value == AudioPlayer.PlaybackStates.STATE_PLAYING)
            audioPlayer.pause()
        else
            audioPlayer.restartCurrentPlayback()
    }

    override fun onCleared() {
        super.onCleared()

        audioPlayer.releasePlayer()
    }
}