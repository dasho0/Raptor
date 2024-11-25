package com.example.raptor.viewmodels

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.raptor.AudioPlayer
import com.example.raptor.database.DatabaseManager
import com.example.raptor.database.entities.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val audioPlayer: AudioPlayer,
    private val databaseManager: DatabaseManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
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

    private val songFetcher = databaseManager.collectSong(savedStateHandle["songId"]!!) // FIXME:we ball
    private var currentSong: MutableStateFlow<Song?> = MutableStateFlow(null)

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

    fun playPauseRestartCurrentSong() {
        if(
            audioPlayer.playbackState.value == AudioPlayer.PlaybackStates.STATE_IDLE ||
            audioPlayer.playbackState.value == AudioPlayer.PlaybackStates.STATE_PAUSED ||
            audioPlayer.playbackState.value == AudioPlayer.PlaybackStates.STATE_READY
        ) {
            Log.d(javaClass.simpleName, "Playing: $currentSong")
            currentSong.value?.let {
                audioPlayer.playSong(it)
            }
        }
        else if(audioPlayer.playbackState.value == AudioPlayer.PlaybackStates.STATE_PLAYING)
            audioPlayer.pause()
        else
            audioPlayer.restartCurrentPlayback()
    }

    override fun onCleared() {
        super.onCleared()

        audioPlayer.releasePlayer()
    }

    init {
        viewModelScope.launch {
            databaseManager.collectSong(savedStateHandle["songId"]!!).collect {
                currentSong.value = it
            }  // FIXME:we ball
        }

    }
}