package com.example.raptor.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.util.fastJoinToString
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.*

/**
 * Viewmodel for the player screen
 */
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
            return when(state) {
                AudioPlayer.PlaybackStates.STATE_BUFFERING,
                AudioPlayer.PlaybackStates.STATE_READY -> {
                    lastIconState
                }
                AudioPlayer.PlaybackStates.STATE_ENDED -> {
                    Icons.Filled.Replay.also {
                        lastIconState = it
                    }
                }
                AudioPlayer.PlaybackStates.STATE_IDLE -> {
                    Icons.Filled.PlayArrow.also {
                        lastIconState = it
                    }
                }
                AudioPlayer.PlaybackStates.STATE_PLAYING -> {
                    Icons.Filled.PauseCircleFilled.also {
                        lastIconState = it
                    }
                }
                AudioPlayer.PlaybackStates.STATE_PAUSED -> {
                    Icons.Filled.PlayArrow.also {
                        lastIconState = it
                    }
                }
            }
        }
    }

    private val currentSong: MutableStateFlow<Song?> = MutableStateFlow(null)

    /**
     * Flow that represents the position of the playback progress bar
     */
    val progressBarPosition: Flow<Float> = flow {
        while(true) {
            val duration = audioPlayer.currentDuration
            val pos = audioPlayer.currentPosition
            val fraction = if (duration > 0) (pos.toFloat() / duration.toFloat()) else 0f
            emit(fraction.coerceIn(0f, 1f))
            delay(33)
        }
    }

    /**
     * Album icon of the currently playing song
     */
    val currentIconImage = audioPlayer.playbackState
        .map {
            iconFromState.getFrom(it)
        }

    /**
     * Flow with the title of the currently playing song
     */
    val currentSongTitle = currentSong.map { it?.title }

    /**
     * Flow with the artists of the current song joined into a string
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSongArtists = currentSong.flatMapMerge { databaseManager.collectAuthorsOfSong(it) }
        .map {
            it?.map { a -> a.name }?.fastJoinToString(", ")
        }

    /**
     * Flow with the `Album` of the current song
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSongAlbum = currentSong.flatMapMerge {
        databaseManager.collectAlbum(it?.albumId)
    }

    /**
     * Flow with an `ImageBitmap` of the cover of the current song
     */
    val currentCover = currentSongAlbum.map {
        Log.d(javaClass.simpleName, "Collecting bitmap with album: $it")
        if(it != null) {
            imageManager.getBitmapFromAppStorage(it.coverUri?.toUri())
        } else {
            ImageBitmap(1,1)
        }
    }

    /**
     * Observable list of points corresponding to the height of each point on the waveform
     */
    private val _currentWaveform = MutableStateFlow<List<Float>>(emptyList())
    val currentWaveform: StateFlow<List<Float>> = _currentWaveform

    /**
     * Handle the user tapping on the progress bar and change the playback position
     */
    fun onProgressBarMoved(tapPosition: Float) {
        val duration = audioPlayer.currentDuration
        audioPlayer.changeCurrentPosition((tapPosition * duration).toLong())
    }

    /**
     * Handle the user tapping the center button. Depending on the playback state the behaviour
     * will be different
     *
     * If the playback is playing, the button will pause
     * If the playback is paused, the playback will resume
     * If the playback is stopped, the playback will restart
     */
    fun playPauseRestartCurrentSong() {
        when (audioPlayer.playbackState.value) {
            AudioPlayer.PlaybackStates.STATE_IDLE,
            AudioPlayer.PlaybackStates.STATE_PAUSED,
            AudioPlayer.PlaybackStates.STATE_READY -> {
                currentSong.value?.let {
                    audioPlayer.playSong(it)
                }
            }
            AudioPlayer.PlaybackStates.STATE_PLAYING -> {
                audioPlayer.pause()
            }
            AudioPlayer.PlaybackStates.STATE_ENDED -> {
                audioPlayer.restartCurrentPlayback()
            }
            else -> {}
        }
    }

    fun skipTrack(isForward: Boolean) {
        currentSong.value?.let {song ->
            if(isForward) {

            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.releasePlayer()
    }


    private fun loadSongWaveform(uri: Uri) {
        viewModelScope.launch {
            val waveform = extractWaveformDataFromUri(context, uri)
            _currentWaveform.value = waveform
        }
    }

    /**
     * Initialize the `currentSong` Flow
     */
    init {
        viewModelScope.launch {
            databaseManager.collectSong(savedStateHandle["songId"]!!).collect {
                currentSong.value = it
                it?.fileUri?.let { fileStr ->
                    val fileUri = fileStr.toUri()
                    loadSongWaveform(fileUri)
                }
                playPauseRestartCurrentSong()
            }
        }
    }
}