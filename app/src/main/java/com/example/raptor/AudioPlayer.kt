package com.example.raptor

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri

class AudioPlayer(val context: Context) {
    private val player = MediaPlayer().apply {
        AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()
    }

    fun playUri(uri: Uri?) {
        player.apply {
            setDataSource(context, uri!!) // FIXME: we ball
            prepare()
            start()
        }
    }
}