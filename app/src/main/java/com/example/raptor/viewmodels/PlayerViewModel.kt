package com.example.raptor.viewmodels

import android.content.Context
import android.graphics.BitmapFactory
import android.media.effect.EffectContext
import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.util.fastJoinToString
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.util.joinIntoString
import com.example.raptor.AudioPlayer
import com.example.raptor.ImageManager
import com.example.raptor.database.DatabaseManager
import com.example.raptor.database.entities.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val audioPlayer: AudioPlayer,
    private val databaseManager: DatabaseManager,
    private val imageManager: ImageManager,
    @ApplicationContext private val context: Context,
    // TODO: it would be better to get rid of the handle and use assisted DI instead - would fix
    //  some dumb flow collection errors too
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

    private val currentSong: MutableStateFlow<Song?> = MutableStateFlow(null)

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

    val currentSongTitle = currentSong.map { it?.title }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSongArtists = currentSong.flatMapMerge() {
        databaseManager.collectAuthorsOfSong(it)
    }.map {
        it?.map {
            it.name
        }?.fastJoinToString(", ")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSongAlbum = currentSong.flatMapMerge() {
        databaseManager.collectAlbum(it?.albumId)
    }

    // FIXME: this is gigascuffed but touching it breaks everything, no you can't change this if
    // to a let, trust me
    val currentCover = currentSongAlbum.map {
        Log.d(javaClass.simpleName, "Collecting bitmap with album: $it")
        if(it != null) {
            imageManager.collectBitmapFromUri(it.coverUri?.toUri())
        } else {
            ImageBitmap(1,1)
        }
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
                assert(currentSong.value != null)
                playPauseRestartCurrentSong()
            }  // FIXME:we ball
        }
    }
}