package com.example.raptor

import android.content.Context
import androidx.media3.common.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

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

    fun playUri(uri: Uri?) {
        player.apply {
            setMediaItem(MediaItem.fromUri(uri!!)) // FIXME: we ball
            prepare()
            play()
        }
    }
}