package com.example.raptor

import android.content.Context
import androidx.media3.common.AudioAttributes
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.raptor.database.entities.Song

class AudioPlayer(val context: Context) {
    private val player = ExoPlayer.Builder(context).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
    }

    private var currentSong: Song? = null

    private fun updateListeners(song: Song) {
        if(currentSong == song) {
            return
        }

        currentSong = song

        player.addListener(
            object: Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    Log.d(AudioPlayer::class.simpleName, if (isPlaying) "player is playing $song"
                    else "player is not playing")
                    isPlayingUI.value = isPlaying
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    Log.d(AudioPlayer::class.simpleName, "Playback State: ${playbackState}")
                }
            }
        )
    }

    private val isPlayingInternal get() = player.isPlaying

    val isPlayingUI: MutableState<Boolean> = mutableStateOf(false)
    val currentPosition get() = player.currentPosition
    val currentDuration get() = player.duration

    fun playSong(song: Song) {
        assert(isPlayingInternal == false)
        assert(!player.isPlaying)
        assert(isPlayingUI.value == isPlayingInternal)
        Log.d(javaClass.simpleName, "calling playSong")

        updateListeners(song)

        if(player.currentMediaItem.hashCode() != MediaItem.fromUri(song.fileUri!!.toUri())
                .hashCode()) {
            player.apply {
                setMediaItem(MediaItem.fromUri(song.fileUri!!.toUri())) //FIXME: we ball
                prepare()
            }

            Log.d(javaClass.simpleName, "playSong(), songs don't match")
        }

        player.play()

    }

    fun pause() {
        assert(isPlayingUI.value == true)
        assert(isPlayingInternal)
        assert(isPlayingUI.value == isPlayingInternal)

        Log.d(javaClass.simpleName, "calling pause")
        player.pause()

    }

    fun changeCurrentPosition(time: Long) {
        player.seekTo(time)
    }
}