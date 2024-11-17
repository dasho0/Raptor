package com.example.raptor

import android.content.Context
import androidx.media3.common.AudioAttributes
import android.net.Uri
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

    private fun updateListeners(song: Song) {
        player.addListener(
            object: Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    Log.d(javaClass.simpleName, if (isPlaying) "player is playing $song" else "player is not playing")
                    playerState.isPlaying.value = isPlaying
                }
            }
        )
    }

    data class PlayerState(
        var isPlaying: MutableState<Boolean> = mutableStateOf(false)
    )

    val playerState = PlayerState()

    fun playSong(song: Song) {
        updateListeners(song)
        player.apply {
            setMediaItem(MediaItem.fromUri(song.fileUri!!.toUri())) //FIXME: we ball
            prepare()
            play()
        }
    }
}