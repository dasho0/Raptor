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
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.*
import kotlin.math.sqrt

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

    val progressBarPosition: Flow<Float> = flow {
        while(true) {
            val duration = audioPlayer.currentDuration
            val pos = audioPlayer.currentPosition
            val fraction = if (duration > 0) (pos.toFloat() / duration.toFloat()) else 0f
            emit(fraction.coerceIn(0f, 1f))
            delay(33)
        }
    }

    val currentIconImage = audioPlayer.playbackState
        .map {
            iconFromState.getFrom(it)
        }

    val currentSongTitle = currentSong.map { it?.title }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSongArtists = currentSong.flatMapMerge { databaseManager.collectAuthorsOfSong(it) }
        .map {
            it?.map { a -> a.name }?.fastJoinToString(", ")
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSongAlbum = currentSong.flatMapMerge {
        databaseManager.collectAlbum(it?.albumId)
    }

    val currentCover = currentSongAlbum.map {
        Log.d(javaClass.simpleName, "Collecting bitmap with album: $it")
        if(it != null) {
            imageManager.collectBitmapFromUri(it.coverUri?.toUri())
        } else {
            ImageBitmap(1,1)
        }
    }

    private val _currentWaveform = MutableStateFlow<List<Float>>(emptyList())
    val currentWaveform: StateFlow<List<Float>> = _currentWaveform

    fun onProgressBarMoved(tapPosition: Float) {
        val duration = audioPlayer.currentDuration
        audioPlayer.changeCurrentPosition((tapPosition * duration).toLong())
    }

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

    override fun onCleared() {
        super.onCleared()
        audioPlayer.releasePlayer()
    }

    private suspend fun extractWaveformDataFromUri(uri: Uri): List<Float> {
        return withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            val result = mutableListOf<Float>()
            try {
                extractor.setDataSource(context, uri, null)
                var audioTrackIndex = -1
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME)
                    if (mime?.startsWith("audio/") == true) {
                        audioTrackIndex = i
                        break
                    }
                }
                if (audioTrackIndex < 0) {
                    Log.e("PlayerViewModel", "No audio track found in file.")
                    return@withContext emptyList<Float>()
                }

                extractor.selectTrack(audioTrackIndex)
                val format = extractor.getTrackFormat(audioTrackIndex)
                val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

                val mime = format.getString(MediaFormat.KEY_MIME)!!
                val decoder = MediaCodec.createDecoderByType(mime)
                decoder.configure(format, null, null, 0)
                decoder.start()

                val inputBuffers = decoder.inputBuffers
                val outputBuffers = decoder.outputBuffers

                val targetFrameCount = 500
                val samplesPerFrame = 1024
                val bufferInfo = MediaCodec.BufferInfo()
                var done = false
                var framesDecoded = 0

                while (!done && framesDecoded < targetFrameCount) {
                    val inputBufferIndex = decoder.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = inputBuffers[inputBufferIndex]
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            done = true
                        } else {
                            val presentationTimeUs = extractor.sampleTime
                            decoder.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                sampleSize,
                                presentationTimeUs,
                                0
                            )
                            extractor.advance()
                        }
                    }

                    val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
                    if (outputBufferIndex >= 0) {
                        val outputBuffer = decoder.getOutputBuffer(outputBufferIndex) ?: continue
                        val outData = ByteArray(bufferInfo.size)
                        outputBuffer.get(outData)
                        outputBuffer.clear()

                        val shorts = ShortArray(outData.size / 2)
                        for (i in shorts.indices) {
                            shorts[i] = ((outData[i*2+1].toInt() shl 8) or
                                    (outData[i*2].toInt() and 0xFF)).toShort()
                        }

                        var sum = 0.0
                        for (sample in shorts) {
                            sum += (sample * sample).toDouble()
                        }
                        val rms = sqrt(sum / shorts.size)
                        val amplitude = (rms / 32767.0).toFloat().coerceIn(0f,1f)

                        result.add(amplitude)
                        framesDecoded++

                        decoder.releaseOutputBuffer(outputBufferIndex, false)
                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    }
                }
                decoder.stop()
                decoder.release()
                extractor.release()

                if (result.size < targetFrameCount) {
                    while (result.size < targetFrameCount) {
                        result.add(0f)
                    }
                }

                return@withContext result
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error extracting waveform: ${e.message}")
                extractor.release()
                return@withContext emptyList<Float>()
            }
        }
    }

    private fun loadSongWaveform(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val waveform = extractWaveformDataFromUri(uri)
            withContext(Dispatchers.Main) {
                _currentWaveform.value = waveform
            }
        }
    }

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